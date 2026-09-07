package com.optimizer.android.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object OmnixMotion {
    // Durasi (ms) — aturan anti-slop: semua animasi 90-350ms
    const val INSTANT = 90
    const val QUICK = 150
    const val STANDARD = 250
    const val EMPHASIS = 350
    const val STAGGER_MS = 40L

    val ambient: CubicBezierEasing = FastOutSlowInEasing
    val linear: LinearEasing = LinearEasing

    fun <T> quick(): TweenSpec<T> = tween(QUICK, easing = ambient)
    fun <T> standard(): TweenSpec<T> = tween(STANDARD, easing = ambient)
    fun <T> emphasis(): TweenSpec<T> = tween(EMPHASIS, easing = ambient)

    // "Menghentak" — snap balik dengan sedikit bouncy, ini rasa brutalism-nya
    val snapSpring: SpringSpec<Float> = spring(
        dampingRatio = 0.6f,
        stiffness = 500f
    )
    val dialogSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = 400f
    )

    const val pressedScale = 0.97f
}
