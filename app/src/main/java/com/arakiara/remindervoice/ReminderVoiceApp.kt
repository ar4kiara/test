package com.arakiara.remindervoice

import android.app.Application
import com.arakiara.remindervoice.alarm.AlarmScheduler
import com.arakiara.remindervoice.alarm.ReminderNotifier
import com.arakiara.remindervoice.data.ReminderDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderVoiceApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderNotifier.ensureChannels(this)
        CoroutineScope(Dispatchers.IO).launch {
            val dao = ReminderDatabase.get(this@ReminderVoiceApp).reminderDao()
            val scheduler = AlarmScheduler(this@ReminderVoiceApp)
            dao.getEnabled().forEach { scheduler.scheduleReminder(it.toPayload()) }
        }
    }
}
