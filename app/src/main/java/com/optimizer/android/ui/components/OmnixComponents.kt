package com.optimizer.android.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.optimizer.android.ui.theme.OmnixMotion
import com.optimizer.android.ui.theme.OmnixThemeColors
import com.optimizer.android.ui.theme.OmnixHaptics
import com.optimizer.android.ui.theme.OmnixType

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperpowerCard(title: String, icon: ImageVector, onClick: () -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(2.dp, CrispWhite),
        colors = CardDefaults.outlinedCardColors(containerColor = PitchBlack),
        onClick = onClick
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = CrispWhite)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CrispWhite)
        }
    }
}

// ---- DEPRECATED legacy color tokens (dihapus di Task 10) ----
val PitchBlack = Color(0xFF000000)
val CrispWhite = Color(0xFFFFFFFF)
val NeonGreen = Color(0xFF00FF00)

@Composable
fun NeoDialog(title: String, text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(0.dp),
        containerColor = PitchBlack,
        titleContentColor = CrispWhite,
        textContentColor = CrispWhite,
        title = { Text(title, fontWeight = FontWeight.Black) },
        text = { Text(text, fontFamily = FontFamily.Monospace) },
        confirmButton = {
            Button(
                onClick = onConfirm, 
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CrispWhite, contentColor = PitchBlack)
            ) {
                Text("GASKAN", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(0.dp),
                border = BorderStroke(2.dp, CrispWhite),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CrispWhite)
            ) {
                Text("BATAL", fontWeight = FontWeight.Bold)
            }
        }
    )
}
