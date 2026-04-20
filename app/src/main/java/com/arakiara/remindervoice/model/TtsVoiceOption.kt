package com.arakiara.remindervoice.model

data class TtsVoiceOption(
    val name: String,
    val label: String,
    val localeTag: String,
    val isNetwork: Boolean,
    val isIndonesian: Boolean,
)
