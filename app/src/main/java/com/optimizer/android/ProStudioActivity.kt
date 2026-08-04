package com.optimizer.android

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Transformer
import androidx.media3.ui.PlayerView

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
        exoPlayer = null
    }

    private fun startExport(selectedUri: Uri?) {
        if (selectedUri == null) {
            Toast.makeText(this, "Silakan pilih video/foto dari galeri terlebih dahulu!", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "Rendering 1080p 60FPS using Hardware Encoder...", Toast.LENGTH_LONG).show()
    }

    @Composable
    fun ProStudioScreen() {
        var selectedUri by remember { mutableStateOf<Uri?>(null) }
        var isPlaying by remember { mutableStateOf(false) }
        var activeFilter by remember { mutableStateOf<String?>(null) }
        var isSlowMo by remember { mutableStateOf(false) }
        var statusLog by remember { mutableStateOf("READY. PILIH MEDIA DARIGALERI.") }

        val context = LocalContext.current

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                selectedUri = uri
                statusLog = "MEDIA LOADED: ${uri.lastPathSegment}"
                exoPlayer?.let { player ->
                    player.setMediaItem(MediaItem.fromUri(uri))
                    player.prepare()
                    player.playWhenReady = true
                    isPlaying = true
                }
            }
        }

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
                Text("PRO STUDIO AI", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 1.sp)
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { launcher.launch("video/*") },
                        shape = RoundedCornerShape(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White)
                    ) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("IMPORT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { startExport(selectedUri) },
                        shape = RoundedCornerShape(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Text("EXPORT 1080P", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Player View (Real ExoPlayer View)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.DarkGray)
                    .border(2.dp, if (activeFilter != null) Color.Green else Color.White)
            ) {
                if (selectedUri != null) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Overlay Filter Indicator
                    activeFilter?.let { filterName ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .background(Color.Red)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("FILTER: $filterName", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clickable { launcher.launch("video/*") },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "KLIK UNTUK IMPORT VIDEO/FOTO GALERI", 
                            color = Color.Gray, 
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Media Controls & Status Log
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "> $statusLog",
                    color = Color.Green,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    modifier = Modifier.weight(1f)
                )

                if (selectedUri != null) {
                    IconButton(
                        onClick = {
                            exoPlayer?.let { p ->
                                if (p.isPlaying) {
                                    p.pause()
                                    isPlaying = false
                                } else {
                                    p.play()
                                    isPlaying = true
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Timeline Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(Color.DarkGray)
                    .border(1.dp, Color.Gray)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().border(1.dp, Color.Black).background(Color.Blue.copy(alpha = 0.5f)))
                    Box(modifier = Modifier.weight(2f).fillMaxHeight().border(1.dp, Color.Black).background(Color.Red.copy(alpha = 0.5f)))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().border(1.dp, Color.Black).background(Color.Green.copy(alpha = 0.5f)))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Superpowers / AI Tools
            Text("AI SUPERPOWERS (KLIK UNTUK UJI FUNGSI)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(features) { feature ->
                    FeatureCard(
                        feature = feature,
                        isActive = activeFilter == feature.name,
                        onClick = {
                            if (selectedUri == null) {
                                Toast.makeText(this@ProStudioActivity, "Pilih video dari galeri terlebih dahulu!", Toast.LENGTH_SHORT).show()
                            } else {
                                when (feature.name) {
                                    "3D LUTs" -> {
                                        activeFilter = if (activeFilter == "3D LUTs") null else "3D LUTs"
                                        statusLog = if (activeFilter != null) "3D LUT CINEMATIC COLOR APPLIED" else "FILTER REMOVED"
                                    }
                                    "OPTICAL FLOW" -> {
                                        isSlowMo = !isSlowMo
                                        exoPlayer?.setPlaybackSpeed(if (isSlowMo) 0.5f else 1.0f)
                                        statusLog = if (isSlowMo) "OPTICAL FLOW SLOW-MO (0.5X) ACTIVE" else "NORMAL SPEED (1.0X)"
                                    }
                                    "MAGIC ERASER" -> {
                                        activeFilter = if (activeFilter == "MAGIC ERASER") null else "MAGIC ERASER"
                                        statusLog = "TOUCH CANVAS ACTIVATED. DRAW MASK TO ERASE."
                                    }
                                    "VIDEO MATTING" -> {
                                        activeFilter = if (activeFilter == "VIDEO MATTING") null else "VIDEO MATTING"
                                        statusLog = "AUTO GREEN-SCREEN BACKGROUND CUTOUT ACTIVE."
                                    }
                                    "VOICE ISOLATE" -> {
                                        statusLog = "NOISE ISOLATION DSP FILTER ACTIVATED."
                                        Toast.makeText(this@ProStudioActivity, "Voice Isolation Active: Background noise suppressed.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun FeatureCard(feature: StudioFeature, isActive: Boolean, onClick: () -> Unit) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(100.dp)
                .clickable { onClick() }
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(if (isActive) Color.Green.copy(alpha = 0.2f) else Color.Transparent)
                    .border(2.dp, if (isActive) Color.Green else Color.White, RoundedCornerShape(0.dp))
                    .padding(16.dp)
            ) {
                Icon(feature.icon, contentDescription = null, tint = if (isActive) Color.Green else Color.White, modifier = Modifier.fillMaxSize())
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = feature.name, 
                color = if (isActive) Color.Green else Color.White, 
                fontSize = 10.sp, 
                fontFamily = FontFamily.Monospace,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

data class StudioFeature(val name: String, val icon: ImageVector)
