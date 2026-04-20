package com.arakiara.remindervoice.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.arakiara.remindervoice.data.ReminderDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val dao = ReminderDatabase.get(context).reminderDao()
                val scheduler = AlarmScheduler(context)
                dao.getEnabled().forEach { scheduler.scheduleReminder(it.toPayload()) }
            }
            pendingResult.finish()
        }
    }
}
