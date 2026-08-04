package com.optimizer.android

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Transformer
import androidx.media3.ui.PlayerView

// ===== DESIGN TOKENS =====
private val StudioBlack = Color(0xFF0A0A0A)
private val StudioDark = Color(0xFF1A1A1A)
private val StudioMedium = Color(0xFF2A2A2A)
private val StudioLight = Color(0xFF3A3A3A)
private val StudioWhite = Color(0xFFEEEEEE)
private val StudioAccent = Color(0xFF00E676) // Neon Green
private val StudioRed = Color(0xFFFF1744)
private val StudioYellow = Color(0xFFFFEA00)

class ProStudioActivity : ComponentActivity() {

    private var exoPlayer: ExoPlayer? = null
    private var transformer: Transformer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exoPlayer = ExoPlayer.Builder(this).build()
        transformer = Transformer.Builder(this).build()
        setContent { StudioEditorApp() }
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
    }

    // ===== MAIN COMPOSABLE =====
    @Composable
    fun StudioEditorApp() {
        var selectedUri by remember { mutableStateOf<Uri?>(null) }
        var isPlaying by remember { mutableStateOf(false) }
        var currentTab by remember { mutableIntStateOf(0) }
        var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
        var activeFilterName by remember { mutableStateOf<String?>(null) }
        var activeColorMatrix by remember { mutableStateOf<ColorMatrix?>(null) }
        var brightness by remember { mutableFloatStateOf(0f) }
        var contrast by remember { mutableFloatStateOf(1f) }
        var saturation by remember { mutableFloatStateOf(1f) }
        var textOverlay by remember { mutableStateOf("") }
        var showTextInput by remember { mutableStateOf(false) }
        var statusLog by remember { mutableStateOf("PILIH VIDEO ATAU FOTO DARI GALERI") }
        var mediaType by remember { mutableStateOf("video") }

        val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedUri = uri
                mediaType = "video"
                statusLog = "VIDEO LOADED"
                exoPlayer?.let { p ->
                    p.setMediaItem(MediaItem.fromUri(uri))
                    p.prepare()
                    p.playWhenReady = true
                    isPlaying = true
                }
            }
        }
        val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedUri = uri
                mediaType = "image"
                statusLog = "FOTO LOADED"
                exoPlayer?.pause()
                isPlaying = false
            }
        }

        val tabs = listOf(
            TabItem("EDIT", Icons.Filled.Tune),
            TabItem("FILTER", Icons.Filled.AutoAwesome),
            TabItem("SPEED", Icons.Filled.Speed),
            TabItem("TEXT", Icons.Filled.TextFields),
            TabItem("AI", Icons.Filled.AutoFixHigh)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(StudioBlack)
        ) {
            // ===== TOP BAR =====
            TopBar(
                onImportVideo = { videoLauncher.launch("video/*") },
                onImportImage = { imageLauncher.launch("image/*") },
                onExport = {
                    if (selectedUri == null) {
                        Toast.makeText(this@ProStudioActivity, "Pilih media terlebih dahulu!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ProStudioActivity, "Exporting 1080p 60FPS...", Toast.LENGTH_LONG).show()
                    }
                }
            )

            // ===== PREVIEW AREA =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(StudioBlack)
            ) {
                if (selectedUri != null) {
                    if (mediaType == "video") {
                        VideoPreview(activeColorMatrix)
                    } else {
                        ImagePreview(selectedUri!!, activeColorMatrix)
                    }

                    // Filter badge overlay
                    activeFilterName?.let { name ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                                .background(StudioAccent, RoundedCornerShape(4.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(name, color = StudioBlack, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                        }
                    }

                    // Text overlay
                    if (textOverlay.isNotEmpty()) {
                        Text(
                            text = textOverlay,
                            color = StudioWhite,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.align(Alignment.Center),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Speed indicator
                    if (playbackSpeed != 1.0f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .background(StudioYellow, RoundedCornerShape(4.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("${playbackSpeed}x", color = StudioBlack, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                        }
                    }
                } else {
                    // Empty State — Import Prompt
                    EmptyStatePrompt(
                        onImportVideo = { videoLauncher.launch("video/*") },
                        onImportImage = { imageLauncher.launch("image/*") }
                    )
                }
            }

            // ===== PLAYBACK CONTROLS =====
            if (selectedUri != null && mediaType == "video") {
                PlaybackControls(
                    isPlaying = isPlaying,
                    onPlayPause = {
                        exoPlayer?.let { p ->
                            if (p.isPlaying) { p.pause(); isPlaying = false } else { p.play(); isPlaying = true }
                        }
                    }
                )
            }

            // ===== STATUS LOG =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StudioDark)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text("> $statusLog", color = StudioAccent, fontSize = 10.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            // ===== TOOL PANELS (Based on Tab) =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(StudioDark)
                    .padding(12.dp)
            ) {
                when (currentTab) {
                    0 -> EditPanel(
                        brightness = brightness,
                        contrast = contrast,
                        saturation = saturation,
                        onBrightnessChange = { brightness = it; activeColorMatrix = buildAdjustMatrix(brightness, contrast, saturation); statusLog = "BRIGHTNESS: ${(brightness * 100).toInt()}%" },
                        onContrastChange = { contrast = it; activeColorMatrix = buildAdjustMatrix(brightness, contrast, saturation); statusLog = "CONTRAST: ${(contrast * 100).toInt()}%" },
                        onSaturationChange = { saturation = it; activeColorMatrix = buildAdjustMatrix(brightness, contrast, saturation); statusLog = "SATURATION: ${(saturation * 100).toInt()}%" },
                        onReset = { brightness = 0f; contrast = 1f; saturation = 1f; activeColorMatrix = null; activeFilterName = null; statusLog = "ADJUSTMENTS RESET" }
                    )
                    1 -> FilterPanel(
                        activeFilter = activeFilterName,
                        onFilterSelect = { name, matrix ->
                            activeFilterName = if (activeFilterName == name) null else name
                            activeColorMatrix = if (activeFilterName != null) matrix else null
                            statusLog = if (activeFilterName != null) "FILTER: $name APPLIED" else "FILTER REMOVED"
                        }
                    )
                    2 -> SpeedPanel(
                        currentSpeed = playbackSpeed,
                        onSpeedChange = { speed ->
                            playbackSpeed = speed
                            exoPlayer?.setPlaybackSpeed(speed)
                            statusLog = "PLAYBACK SPEED: ${speed}x"
                        }
                    )
                    3 -> TextPanel(
                        currentText = textOverlay,
                        onTextChange = { textOverlay = it; statusLog = if (it.isEmpty()) "TEXT REMOVED" else "TEXT: \"$it\"" }
                    )
                    4 -> AiPanel(
                        onFeatureClick = { feature ->
                            if (selectedUri == null) {
                                Toast.makeText(this@ProStudioActivity, "Pilih media dulu!", Toast.LENGTH_SHORT).show()
                            } else {
                                statusLog = "${feature.uppercase()} ENGINE ACTIVATED"
                                Toast.makeText(this@ProStudioActivity, "$feature engine processing...", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // ===== BOTTOM TAB BAR (CapCut-style) =====
            BottomTabBar(tabs = tabs, currentTab = currentTab, onTabSelect = { currentTab = it })
        }
    }

    // ===== COMPONENT: TOP BAR =====
    @Composable
    fun TopBar(onImportVideo: () -> Unit, onImportImage: () -> Unit, onExport: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioDark)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("OMNIX STUDIO", color = StudioWhite, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallButton(text = "VIDEO", icon = Icons.Filled.Videocam, onClick = onImportVideo)
                SmallButton(text = "FOTO", icon = Icons.Filled.Image, onClick = onImportImage)
                SmallButton(text = "EXPORT", icon = Icons.Filled.FileDownload, onClick = onExport, accent = true)
            }
        }
    }

    @Composable
    fun SmallButton(text: String, icon: ImageVector, onClick: () -> Unit, accent: Boolean = false) {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (accent) StudioAccent else StudioMedium,
                contentColor = if (accent) StudioBlack else StudioWhite
            ),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }

    // ===== COMPONENT: VIDEO PREVIEW =====
    @Composable
    fun VideoPreview(colorMatrix: ColorMatrix?) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    // ===== COMPONENT: IMAGE PREVIEW =====
    @Composable
    fun ImagePreview(uri: Uri, colorMatrix: ColorMatrix?) {
        val context = LocalContext.current
        val bitmap = remember(uri) {
            try {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            } catch (e: Exception) { null }
        }
        bitmap?.let { bmp ->
            val imgBitmap = remember(bmp) { androidx.compose.ui.graphics.asImageBitmap(bmp) }
            Image(
                bitmap = imgBitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                colorFilter = colorMatrix?.let { ColorFilter.colorMatrix(it) }
            )
        }
    }

    // ===== COMPONENT: EMPTY STATE =====
    @Composable
    fun EmptyStatePrompt(onImportVideo: () -> Unit, onImportImage: () -> Unit) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.MovieCreation, contentDescription = null, tint = StudioLight, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("OMNIX STUDIO", color = StudioWhite, fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Text("Pilih video atau foto untuk mulai mengedit", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onImportVideo,
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(2.dp, StudioAccent),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioAccent)
                ) {
                    Icon(Icons.Filled.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("IMPORT VIDEO", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onImportImage,
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(2.dp, StudioWhite),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioWhite)
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("IMPORT FOTO", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }

    // ===== COMPONENT: PLAYBACK CONTROLS =====
    @Composable
    fun PlaybackControls(isPlaying: Boolean, onPlayPause: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioDark)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { exoPlayer?.seekBack() }) {
                Icon(Icons.Filled.Replay10, contentDescription = null, tint = StudioWhite, modifier = Modifier.size(24.dp))
            }
            IconButton(onClick = onPlayPause, modifier = Modifier.size(48.dp)) {
                Icon(
                    if (isPlaying) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                    contentDescription = null,
                    tint = StudioAccent,
                    modifier = Modifier.size(40.dp)
                )
            }
            IconButton(onClick = { exoPlayer?.seekForward() }) {
                Icon(Icons.Filled.Forward10, contentDescription = null, tint = StudioWhite, modifier = Modifier.size(24.dp))
            }
        }
    }

    // ===== PANEL: EDIT (Brightness/Contrast/Saturation) =====
    @Composable
    fun EditPanel(brightness: Float, contrast: Float, saturation: Float, onBrightnessChange: (Float) -> Unit, onContrastChange: (Float) -> Unit, onSaturationChange: (Float) -> Unit, onReset: () -> Unit) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
            Text("ADJUSTMENTS", color = StudioWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            AdjustSlider("BRIGHTNESS", brightness, -1f, 1f, onBrightnessChange)
            AdjustSlider("CONTRAST", contrast, 0.5f, 2f, onContrastChange)
            AdjustSlider("SATURATION", saturation, 0f, 2f, onSaturationChange)
            TextButton(onClick = onReset) { Text("RESET ALL", color = StudioRed, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        }
    }

    @Composable
    fun AdjustSlider(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(90.dp))
            Slider(
                value = value, onValueChange = onChange, valueRange = min..max,
                modifier = Modifier.weight(1f).height(24.dp),
                colors = SliderDefaults.colors(thumbColor = StudioAccent, activeTrackColor = StudioAccent, inactiveTrackColor = StudioLight)
            )
            Text("${(value * 100).toInt()}", color = StudioWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
        }
    }

    // ===== PANEL: FILTERS (3D LUTs Presets) =====
    @Composable
    fun FilterPanel(activeFilter: String?, onFilterSelect: (String, ColorMatrix) -> Unit) {
        Column {
            Text("CINEMATIC PRESETS", color = StudioWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val presets = listOf(
                    FilterPreset("CYBERPUNK", Color(0xFF00FFFF), floatArrayOf(0.5f,0.5f,0f,0f,0f, 0f,1f,0.2f,0f,0f, 0f,0.2f,1.2f,0f,0f, 0f,0f,0f,1f,0f)),
                    FilterPreset("VINTAGE", Color(0xFFD4A574), floatArrayOf(1.2f,0.1f,0f,0f,20f, 0f,1f,0.1f,0f,10f, 0f,0f,0.8f,0f,0f, 0f,0f,0f,1f,0f)),
                    FilterPreset("MOODY", Color(0xFF4A6741), floatArrayOf(0.8f,0f,0f,0f,-20f, 0f,0.9f,0.1f,0f,-10f, 0f,0f,1.1f,0f,0f, 0f,0f,0f,1f,0f)),
                    FilterPreset("B&W", Color(0xFF888888), floatArrayOf(0.33f,0.33f,0.33f,0f,0f, 0.33f,0.33f,0.33f,0f,0f, 0.33f,0.33f,0.33f,0f,0f, 0f,0f,0f,1f,0f)),
                    FilterPreset("WARM", Color(0xFFFF8A65), floatArrayOf(1.3f,0f,0f,0f,10f, 0f,1.1f,0f,0f,5f, 0f,0f,0.9f,0f,-10f, 0f,0f,0f,1f,0f)),
                    FilterPreset("COLD", Color(0xFF42A5F5), floatArrayOf(0.9f,0f,0f,0f,-10f, 0f,1f,0f,0f,0f, 0f,0f,1.3f,0f,10f, 0f,0f,0f,1f,0f))
                )
                items(presets) { preset ->
                    FilterPresetCard(preset, isActive = activeFilter == preset.name) {
                        onFilterSelect(preset.name, ColorMatrix(preset.matrix))
                    }
                }
            }
        }
    }

    @Composable
    fun FilterPresetCard(preset: FilterPreset, isActive: Boolean, onClick: () -> Unit) {
        val borderColor by animateColorAsState(if (isActive) StudioAccent else StudioLight, label = "border")
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(preset.color.copy(alpha = 0.6f))
                    .border(2.dp, borderColor, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isActive) Icon(Icons.Filled.Check, contentDescription = null, tint = StudioAccent, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(preset.name, color = if (isActive) StudioAccent else Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
    }

    // ===== PANEL: SPEED =====
    @Composable
    fun SpeedPanel(currentSpeed: Float, onSpeedChange: (Float) -> Unit) {
        Column {
            Text("SPEED RAMPING", color = StudioWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(16.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.5f, 2.0f, 3.0f)
                items(speeds) { speed ->
                    val isActive = currentSpeed == speed
                    Button(
                        onClick = { onSpeedChange(speed) },
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isActive) StudioAccent else StudioMedium,
                            contentColor = if (isActive) StudioBlack else StudioWhite
                        ),
                        modifier = Modifier.height(48.dp).width(56.dp),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text("${speed}x", fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = when {
                    currentSpeed < 1f -> "SLOW-MO MODE (${currentSpeed}x)"
                    currentSpeed > 1f -> "FAST-MO MODE (${currentSpeed}x)"
                    else -> "NORMAL SPEED"
                },
                color = if (currentSpeed != 1.0f) StudioYellow else Color.Gray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }

    // ===== PANEL: TEXT =====
    @Composable
    fun TextPanel(currentText: String, onTextChange: (String) -> Unit) {
        Column {
            Text("TEXT OVERLAY", color = StudioWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = currentText,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                placeholder = { Text("Ketik teks di sini...", color = Color.Gray, fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StudioAccent,
                    unfocusedBorderColor = StudioLight,
                    cursorColor = StudioAccent,
                    focusedTextColor = StudioWhite,
                    unfocusedTextColor = StudioWhite
                ),
                singleLine = true,
                shape = RoundedCornerShape(4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val quickTexts = listOf("OMNIX", "SUBSCRIBE", "FOLLOW ME", "VIRAL", "POV:", "GRWM")
                items(quickTexts) { text ->
                    OutlinedButton(
                        onClick = { onTextChange(text) },
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, StudioLight),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioWhite),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (currentText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = { onTextChange("") }) { Text("HAPUS TEKS", color = StudioRed, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }

    // ===== PANEL: AI SUPERPOWERS =====
    @Composable
    fun AiPanel(onFeatureClick: (String) -> Unit) {
        Column {
            Text("AI SUPERPOWERS", color = StudioWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val aiFeatures = listOf(
                    AiFeature("MAGIC ERASER", Icons.Filled.AutoFixHigh, "Hapus objek dari foto"),
                    AiFeature("VIDEO MATTING", Icons.Filled.ContentCut, "Auto green-screen"),
                    AiFeature("VOICE ISOLATE", Icons.Filled.GraphicEq, "Buang noise audio"),
                    AiFeature("OPTICAL FLOW", Icons.Filled.SlowMotionVideo, "AI Slow-Mo 1000fps"),
                    AiFeature("BG REMOVER", Icons.Filled.Wallpaper, "Hapus background")
                )
                items(aiFeatures) { feature ->
                    AiFeatureCard(feature) { onFeatureClick(feature.name) }
                }
            }
        }
    }

    @Composable
    fun AiFeatureCard(feature: AiFeature, onClick: () -> Unit) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(80.dp).clickable { onClick() }
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(StudioMedium, RoundedCornerShape(8.dp))
                    .border(1.dp, StudioLight, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(feature.icon, contentDescription = null, tint = StudioAccent, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(feature.name, color = StudioWhite, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center, maxLines = 1)
            Text(feature.desc, color = Color.Gray, fontSize = 7.sp, textAlign = TextAlign.Center, maxLines = 1)
        }
    }

    // ===== COMPONENT: BOTTOM TAB BAR =====
    @Composable
    fun BottomTabBar(tabs: List<TabItem>, currentTab: Int, onTabSelect: (Int) -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioBlack)
                .border(BorderStroke(1.dp, StudioMedium))
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEachIndexed { index, tab ->
                val isActive = currentTab == index
                val iconColor by animateColorAsState(if (isActive) StudioAccent else Color.Gray, label = "tabColor")
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onTabSelect(index) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(tab.icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(tab.name, color = iconColor, fontSize = 9.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }

    // ===== UTILITY: Build Color Matrix =====
    private fun buildAdjustMatrix(brightness: Float, contrast: Float, saturation: Float): ColorMatrix {
        val cm = ColorMatrix()
        // Brightness
        val b = brightness * 255
        val brightnessMatrix = ColorMatrix(floatArrayOf(1f,0f,0f,0f,b, 0f,1f,0f,0f,b, 0f,0f,1f,0f,b, 0f,0f,0f,1f,0f))
        cm.timesAssign(brightnessMatrix)
        // Contrast
        val t = (1f - contrast) / 2f * 255f
        val contrastMatrix = ColorMatrix(floatArrayOf(contrast,0f,0f,0f,t, 0f,contrast,0f,0f,t, 0f,0f,contrast,0f,t, 0f,0f,0f,1f,0f))
        cm.timesAssign(contrastMatrix)
        // Saturation
        cm.setToSaturation(saturation)
        return cm
    }
}

// ===== DATA CLASSES =====
data class StudioFeature(val name: String, val icon: ImageVector)
data class FilterPreset(val name: String, val color: Color, val matrix: FloatArray)
data class TabItem(val name: String, val icon: ImageVector)
data class AiFeature(val name: String, val icon: ImageVector, val desc: String)
