package com.arakiara.remindervoice.settings

import android.content.Context
import kotlin.math.roundToInt

object AppSettings {
    private const val PREF_NAME = "reminder_voice_settings"
    private const val KEY_FORCED_ALARM_VOLUME_PERCENT = "forced_alarm_volume_percent"
    private const val KEY_REPEAT_DELAY_SECONDS = "repeat_delay_seconds"

    const val DEFAULT_VOLUME_PERCENT = 90
    const val DEFAULT_REPEAT_DELAY_SECONDS = 3

    fun getForcedAlarmVolumePercent(context: Context): Int = context.settingsPrefs()
        .getInt(KEY_FORCED_ALARM_VOLUME_PERCENT, DEFAULT_VOLUME_PERCENT)
        .coerceIn(10, 100)

    fun setForcedAlarmVolumePercent(context: Context, value: Int) {
        context.settingsPrefs()
            .edit()
            .putInt(KEY_FORCED_ALARM_VOLUME_PERCENT, value.coerceIn(10, 100))
            .apply()
    }

    fun getRepeatDelaySeconds(context: Context): Int = context.settingsPrefs()
        .getInt(KEY_REPEAT_DELAY_SECONDS, DEFAULT_REPEAT_DELAY_SECONDS)
        .coerceIn(1, 15)

    fun getRepeatDelayMillis(context: Context): Long = getRepeatDelaySeconds(context) * 1000L

    fun setRepeatDelaySeconds(context: Context, value: Int) {
        context.settingsPrefs()
            .edit()
            .putInt(KEY_REPEAT_DELAY_SECONDS, value.coerceIn(1, 15))
            .apply()
    }

    fun percentToVolumeIndex(maxVolume: Int, percent: Int): Int {
        if (maxVolume <= 0) return 0
        return (maxVolume * (percent.coerceIn(10, 100) / 100f)).roundToInt().coerceIn(1, maxVolume)
    }

    private fun Context.settingsPrefs() = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
}
