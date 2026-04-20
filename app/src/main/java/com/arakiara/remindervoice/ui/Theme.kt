package com.arakiara.remindervoice.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2447F9),
    onPrimary = Color.White,
    secondary = Color(0xFF7C4DFF),
    tertiary = Color(0xFF00B8D9),
    background = Color(0xFFF5F7FF),
    surface = Color.White,
    surfaceVariant = Color(0xFFE9EEFF),
    onSurfaceVariant = Color(0xFF3A476F),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9FB1FF),
    onPrimary = Color(0xFF0E1B5E),
    secondary = Color(0xFFD0BCFF),
    tertiary = Color(0xFF73D7EC),
    background = Color(0xFF0B1020),
    surface = Color(0xFF10182C),
    surfaceVariant = Color(0xFF18233D),
    onSurfaceVariant = Color(0xFFD5DEFF),
)

@Composable
fun ReminderVoiceTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
