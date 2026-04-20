package com.arakiara.remindervoice.alarm

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.arakiara.remindervoice.model.ReminderExtras

class AlarmReceiver : BroadcastReceiver() {

    @SuppressLint("WakelockTimeout")
    override fun onReceive(context: Context, intent: Intent) {
        val payload = ReminderExtras.run { intent.toReminderPayload() } ?: return
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "com.arakiara.remindervoice:alarmWakeLock",
        )
        wakeLock.acquire(30_000L)
        try {
            ReminderNotifier.showReminderNotification(context, payload)

            val serviceIntent = Intent(context, AlarmPlaybackService::class.java)
                .setAction(AlarmPlaybackService.ACTION_START)
                .let { ReminderExtras.run { it.putReminder(payload) } }
            ContextCompat.startForegroundService(context, serviceIntent)

            if (intent.action == ACTION_FIRE_REMINDER) {
                AlarmScheduler(context).scheduleReminder(payload)
            }
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }

    companion object {
        const val ACTION_FIRE_REMINDER = "com.arakiara.remindervoice.action.FIRE_REMINDER"
        const val ACTION_FIRE_SNOOZE = "com.arakiara.remindervoice.action.FIRE_SNOOZE"
    }
}
