package com.arakiara.remindervoice.model

enum class TtsStyle(
    val label: String,
    val speechRate: Float,
    val pitch: Float,
) {
    TEGAS("Indonesia tegas", 0.92f, 0.86f),
    NATURAL("Indonesia natural", 1.0f, 1.0f),
    HALUS("Indonesia halus", 0.95f, 1.08f),
}
