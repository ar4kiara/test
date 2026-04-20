package com.arakiara.remindervoice.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arakiara.remindervoice.alarm.AlarmScheduler
import com.arakiara.remindervoice.data.ReminderDatabase
import com.arakiara.remindervoice.data.ReminderEntity
import com.arakiara.remindervoice.model.ReminderDays
import com.arakiara.remindervoice.model.TtsStyle
import com.arakiara.remindervoice.model.VoiceMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReminderViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = ReminderDatabase.get(application).reminderDao()
    private val scheduler = AlarmScheduler(application)

    val reminders = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var statusMessage by mutableStateOf<String?>(null)
        private set

    fun clearStatusMessage() {
        statusMessage = null
    }

    fun addReminder(
        title: String,
        message: String,
        hour: Int,
        minute: Int,
        daysOfWeek: Set<Int>,
        voiceMode: VoiceMode,
        ttsStyle: TtsStyle,
        ttsVoiceName: String?,
        customSoundUri: String?,
        notificationSoundUri: String?,
    ) {
        val validated = validateReminder(
            title = title,
            hour = hour,
            minute = minute,
            daysOfWeek = daysOfWeek,
            voiceMode = voiceMode,
            customSoundUri = customSoundUri,
        ) ?: return

        viewModelScope.launch {
            val reminder = ReminderEntity(
                title = validated.first,
                message = message.trim(),
                hour = hour,
                minute = minute,
                enabled = true,
                daysOfWeek = ReminderDays.encode(daysOfWeek),
                voiceMode = voiceMode.name,
                ttsStyle = ttsStyle.name,
                ttsVoiceName = ttsVoiceName,
                customSoundUri = customSoundUri,
                notificationSoundUri = notificationSoundUri,
            )
            val newId = dao.insert(reminder).toInt()
            val saved = reminder.copy(id = newId)
            val scheduled = scheduler.scheduleReminder(saved.toPayload())
            statusMessage = if (scheduled) {
                "Pengingat disimpan."
            } else {
                "Pengingat tersimpan, tapi izin exact alarm belum aktif."
            }
        }
    }

    fun updateReminder(
        reminder: ReminderEntity,
        title: String,
        message: String,
        hour: Int,
        minute: Int,
        daysOfWeek: Set<Int>,
        voiceMode: VoiceMode,
        ttsStyle: TtsStyle,
        ttsVoiceName: String?,
        customSoundUri: String?,
        notificationSoundUri: String?,
    ) {
        val validated = validateReminder(
            title = title,
            hour = hour,
            minute = minute,
            daysOfWeek = daysOfWeek,
            voiceMode = voiceMode,
            customSoundUri = customSoundUri,
        ) ?: return

        viewModelScope.launch {
            scheduler.cancelReminder(reminder.id)
            val updated = reminder.copy(
                title = validated.first,
                message = message.trim(),
                hour = hour,
                minute = minute,
                daysOfWeek = ReminderDays.encode(daysOfWeek),
                voiceMode = voiceMode.name,
                ttsStyle = ttsStyle.name,
                ttsVoiceName = ttsVoiceName,
                customSoundUri = customSoundUri,
                notificationSoundUri = notificationSoundUri,
            )
            dao.update(updated)
            if (updated.enabled) {
                val scheduled = scheduler.scheduleReminder(updated.toPayload())
                statusMessage = if (scheduled) {
                    "Pengingat diperbarui."
                } else {
                    "Pengingat diperbarui, tapi izin exact alarm belum aktif."
                }
            } else {
                statusMessage = "Pengingat diperbarui."
            }
        }
    }

    fun toggleReminder(reminder: ReminderEntity, enabled: Boolean) {
        viewModelScope.launch {
            val updated = reminder.copy(enabled = enabled)
            dao.update(updated)
            if (enabled) {
                val scheduled = scheduler.scheduleReminder(updated.toPayload())
                statusMessage = if (scheduled) "Pengingat diaktifkan." else "Aktif, tapi exact alarm belum diizinkan."
            } else {
                scheduler.cancelReminder(reminder.id)
                statusMessage = "Pengingat dimatikan."
            }
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            scheduler.cancelReminder(reminder.id)
            dao.delete(reminder)
            statusMessage = "Pengingat dihapus."
        }
    }

    private fun validateReminder(
        title: String,
        hour: Int,
        minute: Int,
        daysOfWeek: Set<Int>,
        voiceMode: VoiceMode,
        customSoundUri: String?,
    ): Pair<String, Unit>? {
        val trimmedTitle = title.trim()

        if (trimmedTitle.isBlank()) {
            statusMessage = "Judul pengingat wajib diisi."
            return null
        }
        if (hour !in 0..23 || minute !in 0..59) {
            statusMessage = "Jam harus valid."
            return null
        }
        if (daysOfWeek.isEmpty()) {
            statusMessage = "Pilih minimal satu hari."
            return null
        }
        if (voiceMode == VoiceMode.CUSTOM_SOUND && customSoundUri.isNullOrBlank()) {
            statusMessage = "Pilih file audio dulu untuk mode custom."
            return null
        }
        return trimmedTitle to Unit
    }
}
