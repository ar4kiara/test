package com.arakiara.remindervoice.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.arakiara.remindervoice.model.ReminderExtras

class AlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val payload = ReminderExtras.run { intent.toReminderPayload() } ?: return
        when (intent.action) {
            ACTION_STOP -> {
                ReminderNotifier.cancelReminderNotification(context, payload.id)
                ReminderNotifier.cancelServiceNotification(context)
                context.stopService(Intent(context, AlarmPlaybackService::class.java))
            }

            ACTION_SNOOZE -> {
                ReminderNotifier.cancelReminderNotification(context, payload.id)
                ReminderNotifier.cancelServiceNotification(context)
                context.stopService(Intent(context, AlarmPlaybackService::class.java))
                AlarmScheduler(context).scheduleSnooze(payload, 5)
            }
        }
    }

    companion object {
        const val ACTION_STOP = "com.arakiara.remindervoice.action.STOP"
        const val ACTION_SNOOZE = "com.arakiara.remindervoice.action.SNOOZE"
    }
}
