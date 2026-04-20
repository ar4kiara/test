package com.arakiara.remindervoice.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.arakiara.remindervoice.model.ReminderExtras
import com.arakiara.remindervoice.model.ReminderPayload
import java.util.Calendar
import kotlin.math.absoluteValue

class AlarmScheduler(
    private val context: Context,
) {
    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExact(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun scheduleReminder(payload: ReminderPayload): Boolean {
        if (!canScheduleExact()) return false

        val triggerAt = nextTriggerMillis(
            hour = payload.hour,
            minute = payload.minute,
            selectedDays = payload.daysOfWeek,
        )
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_FIRE_REMINDER)
            .let { ReminderExtras.run { it.putReminder(payload) } }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            payload.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent,
        )
        return true
    }

    fun cancelReminder(reminderId: Int) {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_FIRE_REMINDER)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pendingIntent)
    }

    fun scheduleSnooze(payload: ReminderPayload, minutesFromNow: Int = 5) {
        if (!canScheduleExact()) return

        val tempId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt().absoluteValue
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_FIRE_SNOOZE)
            .let { ReminderExtras.run { it.putReminder(payload, tempId) } }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            tempId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val triggerAt = System.currentTimeMillis() + minutesFromNow * 60_000L
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent,
        )
    }

    private fun nextTriggerMillis(
        hour: Int,
        minute: Int,
        selectedDays: Set<Int>,
    ): Long {
        val normalizedDays = if (selectedDays.isEmpty()) {
            setOf(
                Calendar.MONDAY,
                Calendar.TUESDAY,
                Calendar.WEDNESDAY,
                Calendar.THURSDAY,
                Calendar.FRIDAY,
                Calendar.SATURDAY,
                Calendar.SUNDAY,
            )
        } else {
            selectedDays
        }

        val now = Calendar.getInstance()
        repeat(8) { dayOffset ->
            val candidate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (candidate.get(Calendar.DAY_OF_WEEK) in normalizedDays && candidate.after(now)) {
                return candidate.timeInMillis
            }
        }

        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
