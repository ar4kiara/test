package com.arakiara.remindervoice.model

enum class VoiceMode(val label: String) {
    NOTIFICATION_ONLY("Notif biasa"),
    TTS("Suara ngomong"),
    CUSTOM_SOUND("File audio custom")
}
