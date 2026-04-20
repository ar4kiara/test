package com.arakiara.remindervoice.model

import kotlin.math.roundToLong

sealed class TtsSpeechStep {
    data class Text(val value: String) : TtsSpeechStep()
    data class Silence(val durationMs: Long) : TtsSpeechStep()
}

object TtsSpeechPlanner {
    private val customPauseRegex = Regex("\\[jeda\\s*([0-9]+(?:[\\.,][0-9]+)?)?\\]", RegexOption.IGNORE_CASE)
    private val softBreakRegex = Regex("(\\n+|\\|+)")

    fun buildAlarmText(title: String, message: String): String = buildString {
        append(title.ifBlank { "Pengingat" })
        if (message.isNotBlank()) {
            append(". ")
            append(message)
        }
    }

    fun plan(rawText: String): List<TtsSpeechStep> {
        val source = rawText
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .trim()

        if (source.isBlank()) return listOf(TtsSpeechStep.Text("Halo, ini tes suara reminder."))

        val steps = mutableListOf<TtsSpeechStep>()
        var lastIndex = 0

        customPauseRegex.findAll(source).forEach { match ->
            addTextWithSoftBreaks(source.substring(lastIndex, match.range.first), steps)
            val seconds = match.groupValues.getOrNull(1)
                ?.replace(',', '.')
                ?.toDoubleOrNull()
                ?: 1.0
            steps.add(TtsSpeechStep.Silence((seconds * 1000.0).roundToLong().coerceIn(250L, 15_000L)))
            lastIndex = match.range.last + 1
        }
        addTextWithSoftBreaks(source.substring(lastIndex), steps)

        return cleanupSteps(steps).ifEmpty { listOf(TtsSpeechStep.Text("Halo, ini tes suara reminder.")) }
    }

    private fun addTextWithSoftBreaks(text: String, steps: MutableList<TtsSpeechStep>) {
        var lastIndex = 0
        softBreakRegex.findAll(text).forEach { match ->
            addText(text.substring(lastIndex, match.range.first), steps)
            val markerCount = match.value.count { it == '\n' || it == '|' }.coerceAtLeast(1)
            steps.add(TtsSpeechStep.Silence((700L * markerCount).coerceAtMost(4_000L)))
            lastIndex = match.range.last + 1
        }
        addText(text.substring(lastIndex), steps)
    }

    private fun addText(text: String, steps: MutableList<TtsSpeechStep>) {
        val cleaned = text
            .replace(Regex("[ \\t]+"), " ")
            .trim()
        if (cleaned.isNotBlank()) {
            steps.add(TtsSpeechStep.Text(cleaned))
        }
    }

    private fun cleanupSteps(rawSteps: List<TtsSpeechStep>): List<TtsSpeechStep> {
        val result = mutableListOf<TtsSpeechStep>()
        rawSteps.forEach { step ->
            when (step) {
                is TtsSpeechStep.Text -> result.add(step)
                is TtsSpeechStep.Silence -> {
                    val last = result.lastOrNull()
                    if (last is TtsSpeechStep.Silence) {
                        result[result.lastIndex] = TtsSpeechStep.Silence((last.durationMs + step.durationMs).coerceAtMost(15_000L))
                    } else if (result.isNotEmpty()) {
                        result.add(step)
                    }
                }
            }
        }
        while (result.lastOrNull() is TtsSpeechStep.Silence) {
            result.removeAt(result.lastIndex)
        }
        return result
    }
}
