package com.optimizer.android.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.optimizer.android.ui.theme.OmnixMotion
import com.optimizer.android.ui.theme.OmnixThemeColors
import com.optimizer.android.ui.theme.OmnixHaptics
import com.optimizer.android.ui.theme.OmnixType
import kotlinx.coroutines.launch

// Staggered entrance — fade + slide-up, jeda 40ms antar item
@Composable
fun StaggerIn(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    // Di @Preview (inspection mode) langsung tampil, tidak menunggu animasi
    val inPreview = androidx.compose.ui.platform.LocalInspectionMode.current
    var entered by remember { mutableStateOf(inPreview) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * OmnixMotion.STAGGER_MS + 100L)
        entered = true
    }
    AnimatedVisibility(
        visible = entered,
        enter = fadeIn(OmnixMotion.standard()) + slideInVertically(OmnixMotion.standard()) { it / 3 },
        modifier = modifier
    ) { Box { content() } }
}

@Composable
fun StatusCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    alert: Boolean = false,
    index: Int = 0
) {
    val colors = OmnixThemeColors.colors
    val border = if (alert) colors.danger else colors.ink
    val alertPulse = rememberInfiniteTransition(label = "alertPulse")
    val pulseAlpha by alertPulse.animateFloat(
        initialValue = 1f, targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(2000, easing = OmnixMotion.ambient), RepeatMode.Reverse),
        label = "pulseAlpha"
    )
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(alert) { if (alert) OmnixHaptics.buzz(haptics) }

    StaggerIn(index, modifier) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape,
            border = BorderStroke(2.dp, border.copy(alpha = if (alert) pulseAlpha else 1f)),
            colors = CardDefaults.outlinedCardColors(containerColor = colors.base)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    title.uppercase(),
                    style = OmnixType.label,
                    color = if (alert) colors.danger else colors.grid
                )
                Spacer(Modifier.height(6.dp))
                AnimatedContent(
                    targetState = value,
                    transitionSpec = {
                        (slideInVertically(OmnixMotion.standard()) { it } + fadeIn(OmnixMotion.standard())) togetherWith
                            (slideOutVertically(OmnixMotion.standard()) { -it } + fadeOut(OmnixMotion.standard()))
                    },
                    label = "statusValue"
                ) { v ->
                    Text(v, style = OmnixType.valueBig, color = if (alert) colors.danger else colors.ink)
                }
            }
        }
    }
}

@Composable
fun SuperpowerCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color? = null,
    index: Int = 0
) {
    val colors = OmnixThemeColors.colors
    val rail = accent ?: colors.ink
    val haptics = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }
    var pressX by remember { mutableStateOf(0.5f) }

    val scale by animateFloatAsState(
        targetValue = if (pressed) OmnixMotion.pressedScale else 1f,
        animationSpec = OmnixMotion.snapSpring,
        label = "pressScale"
    )
    val shadowAlpha by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = OmnixMotion.quick(),
        label = "shadowAlpha"
    )

    StaggerIn(index, modifier.fillMaxWidth()) {
        Box(Modifier.padding(bottom = 8.dp)) {
            // Hard accent shadow (glow brutalism) — muncul saat pressed
            Box(
                Modifier
                    .matchParentSize()
                    .offset(x = 4.dp, y = 4.dp)
                    .background(rail)
                    .alpha(shadowAlpha)
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale; scaleY = scale
                        translationX = ((pressX - 0.5f) * 4f).dp.toPx()
                    }
                    .border(2.dp, colors.ink, RectangleShape)
                    .background(colors.base)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                pressed = true
                                pressX = (it.x / size.width.toFloat()).coerceIn(0f, 1f)
                                OmnixHaptics.tick(haptics)
                                tryAwaitRelease()
                                pressed = false
                            },
                            onTap = { onClick() }
                        )
                    }
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .border(2.dp, colors.ink, RectangleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = rail)
                    }
                    Spacer(Modifier.width(14.dp))
                    // Accent rail kiri ikon
                    Box(Modifier.width(4.dp).height(28.dp).background(rail))
                    Spacer(Modifier.width(12.dp))
                    Text(title, style = OmnixType.title, color = colors.ink)
                }
            }
        }
    }
}

// ---- DEPRECATED legacy color tokens (dihapus di Task 10) ----
val PitchBlack = Color(0xFF000000)
val CrispWhite = Color(0xFFFFFFFF)
val NeonGreen = Color(0xFF00FF00)

@Composable
fun NeoDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color? = null,
    confirmLabel: String = "GASKAN",
    content: (@Composable () -> Unit)? = null
) {
    val colors = OmnixThemeColors.colors
    val accentColor = accent ?: colors.ink
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val scrim by animateFloatAsState(
        targetValue = if (shown) 0.75f else 0f,
        animationSpec = OmnixMotion.quick(),
        label = "scrim"
    )
    val contentScale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.9f,
        animationSpec = OmnixMotion.dialogSpring,
        label = "dialogScale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.base.copy(alpha = scrim))
                .pointerInput(Unit) { detectTapGestures { onDismiss() } }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer { scaleX = contentScale; scaleY = contentScale }
                    .border(2.dp, colors.ink, RectangleShape)
                    .background(colors.base)
            ) {
                Box(Modifier.fillMaxWidth().height(6.dp).background(accentColor))
                Column(Modifier.padding(20.dp)) {
                    Text(title, style = OmnixType.title, color = colors.ink)
                    Spacer(Modifier.height(10.dp))
                    if (content != null) {
                        content()
                    } else {
                        Text(text, style = OmnixType.mono, color = colors.grid)
                    }
                    Spacer(Modifier.height(20.dp))
                    Row {
                        Button(
                            onClick = {
                                scope.launch { OmnixHaptics.doubleTick(haptics) }
                                onConfirm()
                            },
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = colors.base)
                        ) { Text(confirmLabel, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.width(10.dp))
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RectangleShape,
                            border = BorderStroke(2.dp, colors.ink),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.ink)
                        ) { Text("BATAL", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(number: String, title: String, modifier: Modifier = Modifier) {
    val colors = OmnixThemeColors.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text("$number /", style = OmnixType.label, color = colors.grid)
        Spacer(Modifier.width(8.dp))
        Text(title.uppercase(), style = OmnixType.title, color = colors.ink)
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f).height(2.dp).background(colors.ink))
    }
}

@Composable
fun BrutalBar(progress: Float, accent: Color, modifier: Modifier = Modifier) {
    val total = 16
    val filled = (progress.coerceIn(0f, 1f) * total).toInt()
    val bar = "█".repeat(filled) + "░".repeat(total - filled)
    Text(
        "$bar ${ (progress.coerceIn(0f, 1f) * 100).toInt() }%",
        style = OmnixType.mono,
        color = accent,
        modifier = modifier
    )
}
