package com.arakiara.remindervoice.model

import java.util.Calendar

data class DayOption(
    val calendarValue: Int,
    val shortLabel: String,
    val fullLabel: String,
)

object ReminderDays {
    val orderedDays = listOf(
        DayOption(Calendar.MONDAY, "Sen", "Senin"),
        DayOption(Calendar.TUESDAY, "Sel", "Selasa"),
        DayOption(Calendar.WEDNESDAY, "Rab", "Rabu"),
        DayOption(Calendar.THURSDAY, "Kam", "Kamis"),
        DayOption(Calendar.FRIDAY, "Jum", "Jumat"),
        DayOption(Calendar.SATURDAY, "Sab", "Sabtu"),
        DayOption(Calendar.SUNDAY, "Min", "Minggu"),
    )

    val everyDay: Set<Int> = orderedDays.map { it.calendarValue }.toSet()

    fun encode(days: Set<Int>): String =
        orderedDays.mapNotNull { day ->
            if (day.calendarValue in days) day.calendarValue.toString() else null
        }.joinToString(",")

    fun decode(raw: String?): Set<Int> {
        val parsed = raw
            .orEmpty()
            .split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
        return if (parsed.isEmpty()) everyDay else parsed
    }

    fun describe(days: Set<Int>): String {
        if (days.size == orderedDays.size) return "Setiap hari"
        return orderedDays
            .filter { it.calendarValue in days }
            .joinToString(" • ") { it.shortLabel }
    }
}
