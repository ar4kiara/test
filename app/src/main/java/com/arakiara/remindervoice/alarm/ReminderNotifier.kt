package com.arakiara.remindervoice.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.arakiara.remindervoice.MainActivity
import com.arakiara.remindervoice.R
import com.arakiara.remindervoice.model.ReminderExtras
import com.arakiara.remindervoice.model.ReminderPayload
import com.arakiara.remindervoice.model.VoiceMode
import kotlin.math.absoluteValue

object ReminderNotifier {
    private const val CHANNEL_DEFAULT_PREFIX = "alarm_default"
    const val CHANNEL_VOICE = "alarm_voice"
    const val CHANNEL_SERVICE = "playback_service"

    const val NOTIFICATION_REMINDER_BASE = 10_000
    const val NOTIFICATION_SERVICE = 99_999

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)

        val voiceChannel = NotificationChannel(
            CHANNEL_VOICE,
            context.getString(R.string.channel_alarm_voice),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_alarm_voice_desc)
            enableVibration(true)
            setSound(null, null)
        }

        val serviceChannel = NotificationChannel(
            CHANNEL_SERVICE,
            context.getString(R.string.channel_playback_service),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.channel_playback_service_desc)
            setSound(null, null)
        }

        manager.createNotificationChannels(listOf(voiceChannel, serviceChannel))
    }

    fun showReminderNotification(context: Context, payload: ReminderPayload) {
        ensureChannels(context)

        val contentIntent = PendingIntent.getActivity(
            context,
            payload.id,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = Intent(context, AlarmActionReceiver::class.java)
            .setAction(AlarmActionReceiver.ACTION_STOP)
            .let { ReminderExtras.run { it.putReminder(payload) } }

        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            payload.id + 500_000,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val snoozeIntent = Intent(context, AlarmActionReceiver::class.java)
            .setAction(AlarmActionReceiver.ACTION_SNOOZE)
            .let { ReminderExtras.run { it.putReminder(payload) } }

        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            payload.id + 600_000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val body = payload.message.ifBlank { payload.title }
        val channelId = CHANNEL_VOICE

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(payload.title.ifBlank { "Pengingat" })
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(0, context.getString(R.string.action_stop), stopPendingIntent)
            .addAction(0, context.getString(R.string.action_snooze), snoozePendingIntent)

        NotificationManagerCompat.from(context).notify(
            NOTIFICATION_REMINDER_BASE + payload.id,
            builder.build(),
        )
    }

    private fun ensureReminderSoundChannel(context: Context, payload: ReminderPayload): String {
        val channelId = buildReminderSoundChannelId(payload)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(channelId) == null) {
                val alarmAudioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val soundUri = payload.notificationSoundUri?.let(android.net.Uri::parse)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

                val channel = NotificationChannel(
                    channelId,
                    context.getString(R.string.channel_alarm_default),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = context.getString(R.string.channel_alarm_default_desc)
                    enableVibration(true)
                    setSound(soundUri, alarmAudioAttributes)
                }
                manager.createNotificationChannel(channel)
            }
        }
        return channelId
    }

    private fun buildReminderSoundChannelId(payload: ReminderPayload): String {
        val suffix = (payload.notificationSoundUri?.hashCode() ?: 0).absoluteValue
        return "${CHANNEL_DEFAULT_PREFIX}_${payload.id}_$suffix"
    }

    fun buildServiceNotification(context: Context, payload: ReminderPayload) =
        NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Alarm aktif")
            .setContentText(payload.message.ifBlank { payload.title })
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    fun cancelReminderNotification(context: Context, reminderId: Int) {
        NotificationManagerCompat.from(context)
            .cancel(NOTIFICATION_REMINDER_BASE + reminderId)
    }

    fun cancelServiceNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_SERVICE)
    }
}
