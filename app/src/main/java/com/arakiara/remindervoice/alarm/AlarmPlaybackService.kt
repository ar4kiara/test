package com.arakiara.remindervoice.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.arakiara.remindervoice.model.ReminderExtras
import com.arakiara.remindervoice.model.ReminderPayload
import com.arakiara.remindervoice.model.TtsSpeechPlanner
import com.arakiara.remindervoice.model.TtsSpeechStep
import com.arakiara.remindervoice.model.TtsStyle
import com.arakiara.remindervoice.model.VoiceMode
import com.arakiara.remindervoice.settings.AppSettings
import java.util.Locale

class AlarmPlaybackService : Service(), TextToSpeech.OnInitListener {

    private var mediaPlayer: MediaPlayer? = null
    private var textToSpeech: TextToSpeech? = null
    private var currentPayload: ReminderPayload? = null
    private var ttsReady = false
    private var shouldRepeat = false
    private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private var originalAlarmVolume: Int? = null
    private val handler = Handler(Looper.getMainLooper())
    private var repeatRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        ReminderNotifier.ensureChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopAllPlayback()
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_START, null -> {
                val payload = ReminderExtras.run { intent?.toReminderPayload() } ?: return START_NOT_STICKY
                currentPayload = payload
                shouldRepeat = true
                startForeground(
                    ReminderNotifier.NOTIFICATION_SERVICE,
                    ReminderNotifier.buildServiceNotification(this, payload),
                )
                playPayload(payload)
                return START_STICKY
            }

            else -> return START_NOT_STICKY
        }
    }

    private fun playPayload(payload: ReminderPayload) {
        cancelRepeatRunnable()
        stopCurrentPlayback(keepServiceAlive = true, keepVolumeBoost = true)
        boostAlarmVolume()
        when (payload.voiceMode) {
            VoiceMode.CUSTOM_SOUND -> playUriOnce(payload.customSoundUri?.let(Uri::parse) ?: defaultAlarmUri())
            VoiceMode.TTS -> speakPayload(payload)
            VoiceMode.NOTIFICATION_ONLY -> playUriOnce(payload.notificationSoundUri?.let(Uri::parse) ?: defaultAlarmUri())
        }
    }

    private fun playUriOnce(uri: Uri) {
        val player = MediaPlayer().apply {
            setAudioAttributes(alarmAudioAttributes(contentType = AudioAttributes.CONTENT_TYPE_MUSIC))
            setDataSource(this@AlarmPlaybackService, uri)
            isLooping = false
            setOnPreparedListener { it.start() }
            setOnCompletionListener {
                scheduleNextRepeat()
            }
            setOnErrorListener { _, _, _ ->
                scheduleNextRepeat()
                true
            }
            prepareAsync()
        }
        mediaPlayer = player
    }

    private fun speakPayload(payload: ReminderPayload) {
        if (textToSpeech == null) {
            textToSpeech = TextToSpeech(this, this)
        } else if (ttsReady) {
            speakNow(payload)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = true
            val tts = textToSpeech ?: return
            tts.setAudioAttributes(alarmAudioAttributes(contentType = AudioAttributes.CONTENT_TYPE_SPEECH))
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                override fun onDone(utteranceId: String?) {
                    if (utteranceId?.endsWith(FINAL_UTTERANCE_SUFFIX) == true) {
                        scheduleNextRepeat()
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (utteranceId?.endsWith(FINAL_UTTERANCE_SUFFIX) == true) {
                        scheduleNextRepeat()
                    }
                }
            })
            currentPayload?.let(::speakNow)
        } else {
            stopAllPlayback()
            stopSelf()
        }
    }

    private fun speakNow(payload: ReminderPayload) {
        val tts = textToSpeech ?: return
        applyTtsStyle(tts, payload.ttsStyle)
        applyTtsVoice(tts, payload.ttsVoiceName)
        val text = TtsSpeechPlanner.buildAlarmText(payload.title, payload.message)
        speakPlanned(tts, text, "reminder-${payload.id}-${System.currentTimeMillis()}")
    }

    private fun speakPlanned(tts: TextToSpeech, text: String, baseUtteranceId: String) {
        val steps = TtsSpeechPlanner.plan(text)
        steps.forEachIndexed { index, step ->
            val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val isLast = index == steps.lastIndex
            val utteranceId = "$baseUtteranceId-$index${if (isLast) FINAL_UTTERANCE_SUFFIX else ""}"
            when (step) {
                is TtsSpeechStep.Text -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        tts.speak(step.value, queueMode, null, utteranceId)
                    } else {
                        @Suppress("DEPRECATION")
                        tts.speak(step.value, queueMode, null)
                        if (isLast) scheduleNextRepeat()
                    }
                }

                is TtsSpeechStep.Silence -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        tts.playSilentUtterance(step.durationMs, queueMode, utteranceId)
                    } else if (isLast) {
                        scheduleNextRepeat()
                    }
                }
            }
        }
    }

    private fun applyTtsStyle(tts: TextToSpeech, style: TtsStyle) {
        tts.setSpeechRate(style.speechRate)
        tts.setPitch(style.pitch)
    }

    private fun applyTtsVoice(tts: TextToSpeech, voiceName: String?) {
        val selectedVoice = voiceName
            ?.let { name -> tts.voices.orEmpty().firstOrNull { it.name == name } }
            ?: tts.voices.orEmpty().firstOrNull { it.locale?.language == Locale("id", "ID").language }

        if (selectedVoice != null) {
            runCatching { tts.voice = selectedVoice }
            runCatching { tts.setLanguage(selectedVoice.locale) }
        } else {
            val result = tts.setLanguage(Locale("id", "ID"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.getDefault())
            }
        }
    }

    private fun scheduleNextRepeat() {
        if (!shouldRepeat) return
        cancelRepeatRunnable()
        val payload = currentPayload ?: return
        repeatRunnable = Runnable {
            if (shouldRepeat) {
                playPayload(payload)
            }
        }.also {
            handler.postDelayed(it, AppSettings.getRepeatDelayMillis(this))
        }
    }

    private fun cancelRepeatRunnable() {
        repeatRunnable?.let(handler::removeCallbacks)
        repeatRunnable = null
    }

    private fun boostAlarmVolume() {
        if (originalAlarmVolume == null) {
            originalAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        }
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val targetVolume = AppSettings.percentToVolumeIndex(
            maxVolume = maxVolume,
            percent = AppSettings.getForcedAlarmVolumePercent(this),
        )
        runCatching {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetVolume, 0)
        }
    }

    private fun restoreAlarmVolume() {
        val original = originalAlarmVolume ?: return
        runCatching {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, original, 0)
        }
        originalAlarmVolume = null
    }

    private fun alarmAudioAttributes(contentType: Int): AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(contentType)
            .build()

    private fun defaultAlarmUri(): Uri =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    private fun stopCurrentPlayback(
        keepServiceAlive: Boolean = false,
        keepVolumeBoost: Boolean = false,
    ) {
        mediaPlayer?.runCatching {
            stop()
        }
        mediaPlayer?.runCatching {
            release()
        }
        mediaPlayer = null

        textToSpeech?.stop()
        if (!keepServiceAlive) {
            textToSpeech?.shutdown()
            textToSpeech = null
            ttsReady = false
        }
        if (!keepVolumeBoost) {
            restoreAlarmVolume()
        }
    }

    private fun stopAllPlayback() {
        shouldRepeat = false
        cancelRepeatRunnable()
        stopCurrentPlayback(keepServiceAlive = false, keepVolumeBoost = false)
        ReminderNotifier.cancelServiceNotification(this)
    }

    override fun onDestroy() {
        stopAllPlayback()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.arakiara.remindervoice.action.START_PLAYBACK"
        const val ACTION_STOP = "com.arakiara.remindervoice.action.STOP_PLAYBACK"
        private const val FINAL_UTTERANCE_SUFFIX = "-final"
    }
}
