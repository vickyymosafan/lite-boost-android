package com.optimizer.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

private val omnixMaterialScheme = darkColorScheme(
    background = Color(0xFF0A0A0A),
    surface = Color(0xFF0A0A0A),
    onBackground = Color(0xFFF2F2F2),
    onSurface = Color(0xFFF2F2F2),
    primary = Color(0xFFC6FF00),
    error = Color(0xFFFF3B30)
)

@Composable
fun OmnixTheme(content: @Composable () -> Unit) {
    val colors = remember { omnixColorScheme() }
    CompositionLocalProvider(LocalOmnixColors provides colors) {
        MaterialTheme(colorScheme = omnixMaterialScheme, content = content)
    }
}

object OmnixThemeColors {
    val colors: OmnixColorScheme
        @Composable get() = LocalOmnixColors.current
}
