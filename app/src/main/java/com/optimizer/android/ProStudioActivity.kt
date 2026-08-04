package com.optimizer.android

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Transformer

class ProStudioActivity : ComponentActivity() {

    private var exoPlayer: ExoPlayer? = null
    private var transformer: Transformer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Media3 components
        exoPlayer = ExoPlayer.Builder(this).build()
        transformer = Transformer.Builder(this).build()

        setContent {
            ProStudioScreen()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
    }

    private fun startExport() {
        Toast.makeText(this, "Rendering 1080p 60FPS using Hardware Encoder...", Toast.LENGTH_LONG).show()
        // In a real app, this would use transformer?.start() with an edited MediaItem
    }

    @Composable
    fun ProStudioScreen() {
        val features = listOf(
            StudioFeature("MAGIC ERASER", Icons.Filled.AutoFixHigh),
            StudioFeature("VIDEO MATTING", Icons.Filled.ContentCut),
            StudioFeature("3D LUTs", Icons.Filled.MovieFilter),
            StudioFeature("VOICE ISOLATE", Icons.Filled.GraphicEq),
            StudioFeature("OPTICAL FLOW", Icons.Filled.Speed)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("PRO STUDIO AI", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = 1.sp)
                Button(
                    onClick = { startExport() },
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Text("EXPORT 1080P", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Player View (Mockup)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f/9f)
                    .background(Color.DarkGray)
                    .border(2.dp, Color.White)
            ) {
                Text(
                    "VIDEO PREVIEW (EXOPLAYER)", 
                    color = Color.White, 
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Timeline (Mockup)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color.DarkGray)
                    .border(1.dp, Color.Gray)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().border(1.dp, Color.Black).background(Color.Blue.copy(alpha = 0.5f)))
                    Box(modifier = Modifier.weight(2f).fillMaxHeight().border(1.dp, Color.Black).background(Color.Red.copy(alpha = 0.5f)))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().border(1.dp, Color.Black).background(Color.Green.copy(alpha = 0.5f)))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Superpowers / AI Tools
            Text("AI SUPERPOWERS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(features) { feature ->
                    FeatureCard(feature)
                }
            }
        }
    }

    @Composable
    fun FeatureCard(feature: StudioFeature) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(100.dp)
                .clickable {
                    Toast.makeText(this@ProStudioActivity, "Activating ${feature.name}...", Toast.LENGTH_SHORT).show()
                }
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .border(2.dp, Color.White, RoundedCornerShape(0.dp))
                    .padding(16.dp)
            ) {
                Icon(feature.icon, contentDescription = null, tint = Color.White, modifier = Modifier.fillMaxSize())
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = feature.name, 
                color = Color.White, 
                fontSize = 10.sp, 
                fontFamily = FontFamily.Monospace,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

data class StudioFeature(val name: String, val icon: ImageVector)
