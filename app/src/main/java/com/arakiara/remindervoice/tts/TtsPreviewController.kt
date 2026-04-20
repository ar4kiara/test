package com.arakiara.remindervoice.tts

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arakiara.remindervoice.model.TtsSpeechPlanner
import com.arakiara.remindervoice.model.TtsSpeechStep
import com.arakiara.remindervoice.model.TtsStyle
import com.arakiara.remindervoice.model.TtsVoiceOption
import java.util.Locale

class TtsPreviewController(
    context: Context,
) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private var textToSpeech: TextToSpeech? = TextToSpeech(appContext, this)

    var ready by mutableStateOf(false)
        private set
    var availableVoices by mutableStateOf<List<TtsVoiceOption>>(emptyList())
        private set
    var lastError by mutableStateOf<String?>(null)
        private set

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ready = true
            textToSpeech?.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            refreshVoices()
        } else {
            lastError = "Mesin TTS di HP belum siap."
        }
    }

    fun refreshVoices() {
        val tts = textToSpeech ?: return
        val options = tts.voices
            .orEmpty()
            .map { voice -> voice.toOption() }
            .sortedWith(
                compareByDescending<TtsVoiceOption> { it.isIndonesian }
                    .thenBy { it.isNetwork }
                    .thenBy { it.label.lowercase(Locale.ROOT) },
            )
        availableVoices = options
    }

    fun speak(text: String, voiceName: String?, style: TtsStyle) {
        val tts = textToSpeech ?: return
        if (!ready) {
            lastError = "Mesin TTS belum siap."
            return
        }
        val spokenText = text.trim().ifBlank { "Halo, ini tes suara reminder." }
        applyVoice(tts, voiceName)
        tts.setSpeechRate(style.speechRate)
        tts.setPitch(style.pitch)
        speakPlanned(tts, spokenText, "preview-${System.currentTimeMillis()}")
    }

    private fun speakPlanned(tts: TextToSpeech, text: String, baseUtteranceId: String) {
        val steps = TtsSpeechPlanner.plan(text)
        steps.forEachIndexed { index, step ->
            val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val utteranceId = "$baseUtteranceId-$index"
            when (step) {
                is TtsSpeechStep.Text -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        tts.speak(step.value, queueMode, null, utteranceId)
                    } else {
                        @Suppress("DEPRECATION")
                        tts.speak(step.value, queueMode, null)
                    }
                }

                is TtsSpeechStep.Silence -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        tts.playSilentUtterance(step.durationMs, queueMode, utteranceId)
                    }
                }
            }
        }
    }

    fun stop() {
        textToSpeech?.stop()
    }

    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        ready = false
    }

    private fun applyVoice(tts: TextToSpeech, voiceName: String?) {
        val voices = tts.voices.orEmpty()
        val selectedVoice = voiceName
            ?.let { name -> voices.firstOrNull { it.name == name } }
            ?: voices.firstOrNull { it.locale?.language == Locale("id", "ID").language }

        if (selectedVoice != null) {
            runCatching {
                tts.voice = selectedVoice
            }
            runCatching {
                tts.setLanguage(selectedVoice.locale)
            }
        } else {
            val result = tts.setLanguage(Locale("id", "ID"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.getDefault())
            }
        }
    }
}

private fun Voice.toOption(): TtsVoiceOption {
    val localeValue = locale ?: Locale.getDefault()
    val localeLabel = buildString {
        append(localeValue.displayLanguage.ifBlank { localeValue.language })
        if (localeValue.displayCountry.isNotBlank()) {
            append(" • ")
            append(localeValue.displayCountry)
        }
    }

    val flags = buildList {
        if (localeValue.language == Locale("id", "ID").language) add("Indonesia")
        if (isNetworkConnectionRequired) add("Online") else add("Offline")
    }.joinToString(" • ")

    val label = buildString {
        append(name)
        if (localeLabel.isNotBlank()) {
            append(" — ")
            append(localeLabel)
        }
        if (flags.isNotBlank()) {
            append(" (")
            append(flags)
            append(")")
        }
    }

    return TtsVoiceOption(
        name = name,
        label = label,
        localeTag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            localeValue.toLanguageTag()
        } else {
            "${localeValue.language}-${localeValue.country}"
        },
        isNetwork = isNetworkConnectionRequired,
        isIndonesian = localeValue.language == Locale("id", "ID").language,
    )
}
