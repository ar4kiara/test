package com.arakiara.remindervoice.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.arakiara.remindervoice.model.ReminderDays
import com.arakiara.remindervoice.model.ReminderPayload
import com.arakiara.remindervoice.model.TtsStyle
import com.arakiara.remindervoice.model.VoiceMode

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val message: String,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true,
    val daysOfWeek: String = ReminderDays.encode(ReminderDays.everyDay),
    val voiceMode: String = VoiceMode.NOTIFICATION_ONLY.name,
    val ttsStyle: String = TtsStyle.TEGAS.name,
    val ttsVoiceName: String? = null,
    val customSoundUri: String? = null,
    val notificationSoundUri: String? = null,
) {
    fun toPayload(): ReminderPayload = ReminderPayload(
        id = id,
        title = title,
        message = message,
        hour = hour,
        minute = minute,
        daysOfWeek = ReminderDays.decode(daysOfWeek),
        voiceMode = runCatching { VoiceMode.valueOf(voiceMode) }.getOrElse { VoiceMode.NOTIFICATION_ONLY },
        ttsStyle = runCatching { TtsStyle.valueOf(ttsStyle) }.getOrElse { TtsStyle.TEGAS },
        ttsVoiceName = ttsVoiceName,
        customSoundUri = customSoundUri,
        notificationSoundUri = notificationSoundUri,
    )
}
