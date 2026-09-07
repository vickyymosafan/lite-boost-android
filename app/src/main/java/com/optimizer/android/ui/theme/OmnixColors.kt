package com.optimizer.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class FeatureType { CLEANER, SHIELD, VAULT, CAMERA, STUDIO }

@Immutable
data class OmnixColorScheme(
    val base: Color,
    val ink: Color,
    val grid: Color,
    val danger: Color,
    private val accentMap: Map<FeatureType, Color>
) {
    fun accent(feature: FeatureType): Color = accentMap.getValue(feature)
}

fun omnixColorScheme(): OmnixColorScheme = OmnixColorScheme(
    base = Color(0xFF0A0A0A),   // Base  — hitam diangkat
    ink = Color(0xFFF2F2F2),    // Ink   — teks utama off-white
    grid = Color(0xFF7A7A7A),   // Grid  — teks sekunder / label mono
    danger = Color(0xFFFF3B30), // Danger — alert
    accentMap = mapOf(
        FeatureType.CLEANER to Color(0xFFC6FF00), // Volt Lime
        FeatureType.SHIELD  to Color(0xFF00E5FF), // Cyan
        FeatureType.VAULT   to Color(0xFFB388FF), // Violet
        FeatureType.CAMERA  to Color(0xFFFF4D9D), // Magenta
        FeatureType.STUDIO  to Color(0xFFFF8A00)  // Orange
    )
)

val LocalOmnixColors = staticCompositionLocalOf { omnixColorScheme() }
