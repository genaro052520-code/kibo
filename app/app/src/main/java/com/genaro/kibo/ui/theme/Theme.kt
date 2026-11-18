package com.genaro.kibo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EsquemaClaro = lightColorScheme(
    primary = KiboPurple,
    onPrimary = Color.White,
    secondary = KiboGreen,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = Color(0xFF1B1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1B1B1F),
    onSurfaceVariant = Color(0xFF4A4A4F),
    error = AvisoRojo
)

@Composable
fun KIBOTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaClaro,
        typography = Typography,
        content = content
    )
}