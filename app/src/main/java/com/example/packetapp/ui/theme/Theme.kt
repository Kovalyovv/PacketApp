package com.example.packetapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2196F3), // Синий
    primaryContainer = Color(0xFFE0E0E0), // Светло-серый для фона TopAppBar
    onPrimaryContainer = Color(0xFF424242), // Тёмно-серый для текста и иконок
    secondary = Color(0xFF03DAC5),
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color(0xFF424242), // Тёмно-серый для текста
    surfaceVariant = Color(0xFFF1EDFA) // Светло-серый для карточек
)

@Composable
fun PacketAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}