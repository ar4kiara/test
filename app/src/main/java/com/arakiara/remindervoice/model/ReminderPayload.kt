package com.arakiara.remindervoice.model

import android.content.Intent

data class ReminderPayload(
    val id: Int,
    val title: String,
    val message: String,
    val hour: Int,
    val minute: Int,
    val daysOfWeek: Set<Int>,
    val voiceMode: VoiceMode,
    val ttsStyle: TtsStyle = TtsStyle.TEGAS,
    val ttsVoiceName: String? = null,
    val customSoundUri: String? = null,
    val notificationSoundUri: String? = null,
)

object ReminderExtras {
    private const val PREFIX = "com.arakiara.remindervoice.extra."
    const val EXTRA_ID = PREFIX + "ID"
    const val EXTRA_TITLE = PREFIX + "TITLE"
    const val EXTRA_MESSAGE = PREFIX + "MESSAGE"
    const val EXTRA_HOUR = PREFIX + "HOUR"
    const val EXTRA_MINUTE = PREFIX + "MINUTE"
    const val EXTRA_DAYS_OF_WEEK = PREFIX + "DAYS_OF_WEEK"
    const val EXTRA_VOICE_MODE = PREFIX + "VOICE_MODE"
    const val EXTRA_TTS_STYLE = PREFIX + "TTS_STYLE"
    const val EXTRA_TTS_VOICE_NAME = PREFIX + "TTS_VOICE_NAME"
    const val EXTRA_CUSTOM_SOUND_URI = PREFIX + "CUSTOM_SOUND_URI"
    const val EXTRA_NOTIFICATION_SOUND_URI = PREFIX + "NOTIFICATION_SOUND_URI"
    const val EXTRA_TEMP_ID = PREFIX + "TEMP_ID"

    fun Intent.putReminder(payload: ReminderPayload, tempId: Int? = null): Intent = apply {
        putExtra(EXTRA_ID, payload.id)
        putExtra(EXTRA_TITLE, payload.title)
        putExtra(EXTRA_MESSAGE, payload.message)
        putExtra(EXTRA_HOUR, payload.hour)
        putExtra(EXTRA_MINUTE, payload.minute)
        putExtra(EXTRA_DAYS_OF_WEEK, ReminderDays.encode(payload.daysOfWeek))
        putExtra(EXTRA_VOICE_MODE, payload.voiceMode.name)
        putExtra(EXTRA_TTS_STYLE, payload.ttsStyle.name)
        putExtra(EXTRA_TTS_VOICE_NAME, payload.ttsVoiceName)
        putExtra(EXTRA_CUSTOM_SOUND_URI, payload.customSoundUri)
        putExtra(EXTRA_NOTIFICATION_SOUND_URI, payload.notificationSoundUri)
        if (tempId != null) putExtra(EXTRA_TEMP_ID, tempId)
    }

    fun Intent.toReminderPayload(): ReminderPayload? {
        val id = getIntExtra(EXTRA_ID, Int.MIN_VALUE)
        if (id == Int.MIN_VALUE) return null
        val title = getStringExtra(EXTRA_TITLE).orEmpty()
        val message = getStringExtra(EXTRA_MESSAGE).orEmpty()
        val hour = getIntExtra(EXTRA_HOUR, 0)
        val minute = getIntExtra(EXTRA_MINUTE, 0)
        val daysOfWeek = ReminderDays.decode(getStringExtra(EXTRA_DAYS_OF_WEEK))
        val voiceMode = runCatching {
            VoiceMode.valueOf(getStringExtra(EXTRA_VOICE_MODE).orEmpty())
        }.getOrElse { VoiceMode.NOTIFICATION_ONLY }
        val ttsStyle = runCatching {
            TtsStyle.valueOf(getStringExtra(EXTRA_TTS_STYLE).orEmpty())
        }.getOrElse { TtsStyle.TEGAS }
        val ttsVoiceName = getStringExtra(EXTRA_TTS_VOICE_NAME)
        val customSoundUri = getStringExtra(EXTRA_CUSTOM_SOUND_URI)
        val notificationSoundUri = getStringExtra(EXTRA_NOTIFICATION_SOUND_URI)
        return ReminderPayload(
            id = id,
            title = title,
            message = message,
            hour = hour,
            minute = minute,
            daysOfWeek = daysOfWeek,
            voiceMode = voiceMode,
            ttsStyle = ttsStyle,
            ttsVoiceName = ttsVoiceName,
            customSoundUri = customSoundUri,
            notificationSoundUri = notificationSoundUri,
        )
    }
}
