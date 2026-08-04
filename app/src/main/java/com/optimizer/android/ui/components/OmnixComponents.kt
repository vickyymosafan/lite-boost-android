package com.optimizer.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// GEN-Z NEO-BRUTALISM COLORS
val PitchBlack = Color(0xFF000000)
val CrispWhite = Color(0xFFFFFFFF)
val NeonGreen = Color(0xFF00FF00)

@Composable
fun StatusCard(modifier: Modifier = Modifier, title: String, value: String, alert: Boolean = false) {
    val color = if (alert) Color.Red else CrispWhite
    OutlinedCard(
        modifier = modifier,
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(2.dp, color),
        colors = CardDefaults.outlinedCardColors(containerColor = PitchBlack)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = color)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Black, fontSize = 20.sp, color = color)
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
