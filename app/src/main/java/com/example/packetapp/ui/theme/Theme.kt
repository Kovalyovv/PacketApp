package com.example.packetapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2196F3), // Голубой для кнопок и акцентов
    onPrimary = Color.White,
    background = Color(0xFFE3F2FD), // Светло-голубой фон
    surface = Color.White,
    onSurface = Color.Black,
    secondary = Color(0xFF90CAF9) // Более светлый голубой
)

@Composable
fun ShoppingListAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}