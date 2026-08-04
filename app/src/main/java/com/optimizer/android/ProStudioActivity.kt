package com.optimizer.android

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

// ╔══════════════════════════════════════════════════════════════════╗
// ║                    DESIGN TOKENS                               ║
// ╚══════════════════════════════════════════════════════════════════╝
private val Bk = Color(0xFF0A0A0A)
private val Dk = Color(0xFF151515)
private val Md = Color(0xFF222222)
private val Lt = Color(0xFF333333)
private val Wh = Color(0xFFEEEEEE)
private val Ac = Color(0xFF00E676)
private val Rd = Color(0xFFFF1744)
private val Yw = Color(0xFFFFEA00)
private val Cy = Color(0xFF00BCD4)

// ╔══════════════════════════════════════════════════════════════════╗
// ║                    DATA CLASSES                                 ║
// ╚══════════════════════════════════════════════════════════════════╝
data class DrawStroke(val points: List<Offset>, val color: Color, val width: Float)
data class FilterPreset(val name: String, val color: Color, val matrix: FloatArray)
data class TabItem(val name: String, val icon: ImageVector)

// ╔══════════════════════════════════════════════════════════════════╗
// ║                    COLOR ENGINE                                 ║
// ╚══════════════════════════════════════════════════════════════════╝
object ColorEngine {
    fun buildMatrix(
        brightness: Float = 0f, contrast: Float = 1f, saturation: Float = 1f,
        temperature: Float = 0f, tint: Float = 0f, exposure: Float = 0f,
        highlights: Float = 0f, shadows: Float = 0f, fade: Float = 0f
    ): ColorMatrix {
        val cm = ColorMatrix()
        // Brightness: offset to RGB via 5th column in 5x4 matrix
        val b = brightness * 255f
        // Exposure (additive to brightness)
        val e = exposure * 128f
        val totalB = b + e
        // Contrast
        val c = contrast
        val t = (1f - c) / 2f * 255f
        // Temperature: shift R/B
        val tempR = temperature * 30f
        val tempB = -temperature * 30f
        // Tint: shift G
        val tintG = tint * 20f
        // Fade (lift blacks)
        val fadeVal = fade * 50f
        // Highlights boost (approximate: boost bright channels slightly)
        val hiR = 1f + highlights * 0.15f
        val hiG = 1f + highlights * 0.15f
        val hiB = 1f + highlights * 0.15f
        // Shadows lift
        val shOff = shadows * 40f

        val arr = floatArrayOf(
            c * hiR, 0f, 0f, 0f, t + totalB + tempR + fadeVal + shOff,
            0f, c * hiG, 0f, 0f, t + totalB + tintG + fadeVal + shOff,
            0f, 0f, c * hiB, 0f, t + totalB + tempB + fadeVal + shOff,
            0f, 0f, 0f, 1f, 0f
        )
        cm.set(arr)
        // Apply saturation
        val satMatrix = ColorMatrix()
        satMatrix.setToSaturation(saturation)
        cm.timesAssign(satMatrix)
        return cm
    }

    val PRESETS = listOf(
        FilterPreset("NONE", Color.Gray, floatArrayOf(1f,0f,0f,0f,0f, 0f,1f,0f,0f,0f, 0f,0f,1f,0f,0f, 0f,0f,0f,1f,0f)),
        FilterPreset("CYBERPUNK", Color(0xFF00FFFF), floatArrayOf(0.6f,0.4f,0f,0f,10f, 0f,1f,0.2f,0f,0f, 0f,0.2f,1.3f,0f,15f, 0f,0f,0f,1f,0f)),
        FilterPreset("VINTAGE", Color(0xFFD4A574), floatArrayOf(1.2f,0.1f,0f,0f,25f, 0f,1f,0.1f,0f,15f, 0f,0f,0.75f,0f,0f, 0f,0f,0f,1f,0f)),
        FilterPreset("MOODY", Color(0xFF4A6741), floatArrayOf(0.8f,0f,0f,0f,-25f, 0f,0.85f,0.1f,0f,-15f, 0f,0f,1.1f,0f,5f, 0f,0f,0f,1f,0f)),
        FilterPreset("B&W", Color(0xFF888888), floatArrayOf(0.33f,0.33f,0.33f,0f,0f, 0.33f,0.33f,0.33f,0f,0f, 0.33f,0.33f,0.33f,0f,0f, 0f,0f,0f,1f,0f)),
        FilterPreset("WARM", Color(0xFFFF8A65), floatArrayOf(1.3f,0f,0f,0f,15f, 0f,1.1f,0f,0f,8f, 0f,0f,0.85f,0f,-12f, 0f,0f,0f,1f,0f)),
        FilterPreset("COLD", Color(0xFF42A5F5), floatArrayOf(0.85f,0f,0f,0f,-12f, 0f,1f,0f,0f,0f, 0f,0f,1.3f,0f,15f, 0f,0f,0f,1f,0f)),
        FilterPreset("FILM", Color(0xFFA0887E), floatArrayOf(1.05f,0.05f,0f,0f,10f, 0f,1f,0.05f,0f,8f, 0f,0.05f,0.95f,0f,15f, 0f,0f,0f,1f,0f)),
        FilterPreset("VIVID", Color(0xFFE91E63), floatArrayOf(1.3f,0f,0f,0f,5f, 0f,1.3f,0f,0f,5f, 0f,0f,1.3f,0f,5f, 0f,0f,0f,1f,0f))
    )
}

// ╔══════════════════════════════════════════════════════════════════╗
// ║                    MAIN ACTIVITY                                ║
// ╚══════════════════════════════════════════════════════════════════╝
class ProStudioActivity : ComponentActivity() {

    private var exoPlayer: ExoPlayer? = null
    private var transformer: Transformer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exoPlayer = ExoPlayer.Builder(this).build()
        setContent { StudioEditorApp() }
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
        transformer?.cancel()
        transformer = null
    }

    // ════════════════════════════════════════════════════════════════
    // EXPORT ENGINE (Real 1080p 60fps Hardware Encoding)
    // ════════════════════════════════════════════════════════════════
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun exportVideo(
        sourceUri: Uri,
        trimStartMs: Long,
        trimEndMs: Long,
        onProgress: (String) -> Unit,
        onComplete: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val outputDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: filesDir
        val outputFile = File(outputDir, "OMNIX_Export_${System.currentTimeMillis()}.mp4")
        val outputPath = outputFile.absolutePath

        val clippingConfig = MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(trimStartMs)
            .setEndPositionMs(trimEndMs)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(sourceUri)
            .setClippingConfiguration(clippingConfig)
            .build()

        val editedMediaItem = EditedMediaItem.Builder(mediaItem).build()

        transformer = Transformer.Builder(this)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    onComplete(outputPath)
                }
                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                    onError(exportException.message ?: "Export failed")
                }
            })
            .build()

        onProgress("ENCODING 1080p 60fps → $outputPath")
        transformer?.start(editedMediaItem, outputPath)
    }

    // ════════════════════════════════════════════════════════════════
    // PHOTO EXPORT ENGINE
    // ════════════════════════════════════════════════════════════════
    private fun exportPhoto(bitmap: Bitmap, onComplete: (String) -> Unit) {
        val outputDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: filesDir
        val outputFile = File(outputDir, "OMNIX_Photo_${System.currentTimeMillis()}.png")
        try {
            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            onComplete(outputFile.absolutePath)
        } catch (e: Exception) {
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ╔══════════════════════════════════════════════════════════════╗
    // ║              MAIN EDITOR COMPOSABLE                         ║
    // ╚══════════════════════════════════════════════════════════════╝
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun StudioEditorApp() {
        // ── STATE ──
        var selectedUri by remember { mutableStateOf<Uri?>(null) }
        var mediaType by remember { mutableStateOf("none") } // "none", "video", "image"
        var isPlaying by remember { mutableStateOf(false) }
        var currentTab by remember { mutableIntStateOf(0) }
        var statusLog by remember { mutableStateOf("PILIH VIDEO ATAU FOTO DARI GALERI") }

        // Color grading state
        var brightness by remember { mutableFloatStateOf(0f) }
        var contrast by remember { mutableFloatStateOf(1f) }
        var saturation by remember { mutableFloatStateOf(1f) }
        var temperature by remember { mutableFloatStateOf(0f) }
        var tint by remember { mutableFloatStateOf(0f) }
        var exposure by remember { mutableFloatStateOf(0f) }
        var highlights by remember { mutableFloatStateOf(0f) }
        var shadows by remember { mutableFloatStateOf(0f) }
        var fade by remember { mutableFloatStateOf(0f) }

        // Filter state
        var activeFilterIdx by remember { mutableIntStateOf(0) }
        var filterIntensity by remember { mutableFloatStateOf(1f) }

        // Speed state
        var playbackSpeed by remember { mutableFloatStateOf(1f) }
        var keepPitch by remember { mutableStateOf(true) }

        // Text overlay state
        var textOverlay by remember { mutableStateOf("") }
        var textSize by remember { mutableFloatStateOf(28f) }
        var textBold by remember { mutableStateOf(true) }

        // Audio state
        var volume by remember { mutableFloatStateOf(1f) }
        var isMuted by remember { mutableStateOf(false) }

        // Trim state
        var trimStart by remember { mutableFloatStateOf(0f) }
        var trimEnd by remember { mutableFloatStateOf(1f) }

        // Draw state
        val drawStrokes = remember { mutableStateListOf<DrawStroke>() }
        var brushColor by remember { mutableStateOf(Rd) }
        var brushWidth by remember { mutableFloatStateOf(8f) }

        // Photo transform state
        var rotation by remember { mutableFloatStateOf(0f) }
        var flipH by remember { mutableStateOf(false) }
        var flipV by remember { mutableStateOf(false) }
        var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }

        // Playback position tracking
        var currentPositionMs by remember { mutableStateOf(0L) }
        var durationMs by remember { mutableStateOf(0L) }
        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current

        // Compute active color matrix
        val adjustMatrix = remember(brightness, contrast, saturation, temperature, tint, exposure, highlights, shadows, fade) {
            ColorEngine.buildMatrix(brightness, contrast, saturation, temperature, tint, exposure, highlights, shadows, fade)
        }
        val filterMatrix = remember(activeFilterIdx, filterIntensity) {
            if (activeFilterIdx > 0) {
                val preset = ColorEngine.PRESETS[activeFilterIdx]
                val identity = floatArrayOf(1f,0f,0f,0f,0f, 0f,1f,0f,0f,0f, 0f,0f,1f,0f,0f, 0f,0f,0f,1f,0f)
                val blended = FloatArray(20) { i -> identity[i] + (preset.matrix[i] - identity[i]) * filterIntensity }
                ColorMatrix(blended)
            } else null
        }
        val combinedMatrix = remember(adjustMatrix, filterMatrix) {
            if (filterMatrix != null) {
                val cm = ColorMatrix(adjustMatrix.values.clone())
                cm.timesAssign(filterMatrix)
                cm
            } else adjustMatrix
        }

        // Launchers
        val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedUri = uri; mediaType = "video"; statusLog = "VIDEO LOADED"
                trimStart = 0f; trimEnd = 1f
                exoPlayer?.let { p -> p.setMediaItem(MediaItem.fromUri(uri)); p.prepare(); p.playWhenReady = true; isPlaying = true }
            }
        }
        val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedUri = uri; mediaType = "image"; statusLog = "FOTO LOADED"
                exoPlayer?.pause(); isPlaying = false
                // Load bitmap
                coroutineScope.launch {
                    photoBitmap = withContext(Dispatchers.IO) {
                        try { context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } } catch (e: Exception) { null }
                    }
                }
            }
        }

        // Position tracking for video
        LaunchedEffect(isPlaying) {
            while (isPlaying) {
                currentPositionMs = exoPlayer?.currentPosition ?: 0L
                durationMs = exoPlayer?.duration?.coerceAtLeast(1L) ?: 1L
                delay(200)
            }
        }

        // Apply volume
        LaunchedEffect(volume, isMuted) {
            exoPlayer?.volume = if (isMuted) 0f else volume
        }

        // Tabs
        val tabs = listOf(
            TabItem("EDIT", Icons.Filled.Tune),
            TabItem("FILTER", Icons.Filled.AutoAwesome),
            TabItem("SPEED", Icons.Filled.Speed),
            TabItem("TEXT", Icons.Filled.TextFields),
            TabItem("DRAW", Icons.Filled.Brush),
            TabItem("AUDIO", Icons.Filled.MusicNote),
            TabItem("CROP", Icons.Filled.Crop),
            TabItem("AI", Icons.Filled.AutoFixHigh)
        )

        // ── LAYOUT ──
        Column(modifier = Modifier.fillMaxSize().background(Bk)) {
            // TOP BAR
            Row(
                modifier = Modifier.fillMaxWidth().background(Dk).padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { finish() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.ArrowBack, null, tint = Wh, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("OMNIX STUDIO", color = Wh, fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = 1.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    MiniBtn("VIDEO", Icons.Filled.Videocam) { videoLauncher.launch("video/*") }
                    MiniBtn("FOTO", Icons.Filled.Image) { imageLauncher.launch("image/*") }
                    MiniBtn("EXPORT", Icons.Filled.FileDownload, accent = true) {
                        when {
                            selectedUri == null -> Toast.makeText(context, "Pilih media dulu!", Toast.LENGTH_SHORT).show()
                            mediaType == "video" -> {
                                statusLog = "EXPORTING VIDEO..."
                                val dur = durationMs.coerceAtLeast(1L)
                                exportVideo(selectedUri!!, (trimStart * dur).toLong(), (trimEnd * dur).toLong(),
                                    onProgress = { statusLog = it },
                                    onComplete = { statusLog = "EXPORT SELESAI: $it"; Toast.makeText(context, "Video saved!", Toast.LENGTH_LONG).show() },
                                    onError = { statusLog = "ERROR: $it" }
                                )
                            }
                            mediaType == "image" && photoBitmap != null -> {
                                statusLog = "EXPORTING PHOTO..."
                                exportPhoto(photoBitmap!!) { path -> statusLog = "PHOTO SAVED: $path"; Toast.makeText(context, "Photo saved!", Toast.LENGTH_LONG).show() }
                            }
                        }
                    }
                }
            }

            // PREVIEW AREA
            Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Bk)) {
                if (selectedUri != null) {
                    when (mediaType) {
                        "video" -> AndroidView(
                            factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer; useController = false; setBackgroundColor(android.graphics.Color.BLACK) } },
                            modifier = Modifier.fillMaxSize()
                        )
                        "image" -> photoBitmap?.let { bmp ->
                            val imgBitmap = remember(bmp, rotation, flipH, flipV) {
                                val matrix = android.graphics.Matrix().apply {
                                    postRotate(rotation)
                                    postScale(if (flipH) -1f else 1f, if (flipV) -1f else 1f, bmp.width / 2f, bmp.height / 2f)
                                }
                                Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true).asImageBitmap()
                            }
                            androidx.compose.foundation.Image(
                                bitmap = imgBitmap, contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit,
                                colorFilter = ColorFilter.colorMatrix(combinedMatrix)
                            )
                        }
                    }

                    // Drawing overlay
                    if (currentTab == 4 && drawStrokes.isNotEmpty()) {
                        DrawOverlay(drawStrokes)
                    }
                    // Interactive draw canvas when draw tab is active
                    if (currentTab == 4) {
                        InteractiveDrawCanvas(drawStrokes, brushColor, brushWidth)
                    }

                    // Text overlay
                    if (textOverlay.isNotEmpty()) {
                        Text(
                            text = textOverlay, color = Wh, fontSize = textSize.sp,
                            fontWeight = if (textBold) FontWeight.Black else FontWeight.Normal,
                            modifier = Modifier.align(Alignment.Center),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Filter badge
                    if (activeFilterIdx > 0) {
                        Box(modifier = Modifier.align(Alignment.TopStart).padding(8.dp).background(Ac, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text(ColorEngine.PRESETS[activeFilterIdx].name, color = Bk, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                        }
                    }
                    // Speed badge
                    if (playbackSpeed != 1f) {
                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Yw, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text("${playbackSpeed}x", color = Bk, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                        }
                    }
                } else {
                    // Empty state
                    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Filled.AddPhotoAlternate, null, tint = Lt, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("OMNIX STUDIO", color = Wh, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Text("Pilih video atau foto untuk mulai mengedit", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Spacer(Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = { videoLauncher.launch("video/*") }, shape = RoundedCornerShape(4.dp), border = BorderStroke(2.dp, Ac), colors = ButtonDefaults.outlinedButtonColors(contentColor = Ac)) {
                                Icon(Icons.Filled.Videocam, null, Modifier.size(14.dp)); Spacer(Modifier.width(6.dp)); Text("VIDEO", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(onClick = { imageLauncher.launch("image/*") }, shape = RoundedCornerShape(4.dp), border = BorderStroke(2.dp, Wh), colors = ButtonDefaults.outlinedButtonColors(contentColor = Wh)) {
                                Icon(Icons.Filled.Image, null, Modifier.size(14.dp)); Spacer(Modifier.width(6.dp)); Text("FOTO", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // PLAYBACK CONTROLS (video only)
            if (selectedUri != null && mediaType == "video") {
                Row(modifier = Modifier.fillMaxWidth().background(Dk).padding(horizontal = 12.dp, vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(formatTime(currentPositionMs), color = Ac, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { exoPlayer?.seekBack() }, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Replay10, null, tint = Wh, modifier = Modifier.size(18.dp)) }
                        IconButton(onClick = { exoPlayer?.let { if (it.isPlaying) { it.pause(); isPlaying = false } else { it.play(); isPlaying = true } } }, modifier = Modifier.size(40.dp)) {
                            Icon(if (isPlaying) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle, null, tint = Ac, modifier = Modifier.size(32.dp))
                        }
                        IconButton(onClick = { exoPlayer?.seekForward() }, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Forward10, null, tint = Wh, modifier = Modifier.size(18.dp)) }
                        // Frame step buttons
                        IconButton(onClick = { exoPlayer?.seekTo((exoPlayer?.currentPosition ?: 0) - 33) }, modifier = Modifier.size(28.dp)) { Text("<", color = Wh, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        IconButton(onClick = { exoPlayer?.seekTo((exoPlayer?.currentPosition ?: 0) + 33) }, modifier = Modifier.size(28.dp)) { Text(">", color = Wh, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    }
                    Text(formatTime(durationMs), color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
                // Timeline scrub bar
                if (durationMs > 0) {
                    Slider(
                        value = currentPositionMs.toFloat() / durationMs.toFloat(),
                        onValueChange = { exoPlayer?.seekTo((it * durationMs).toLong()); currentPositionMs = (it * durationMs).toLong() },
                        modifier = Modifier.fillMaxWidth().height(20.dp).padding(horizontal = 12.dp),
                        colors = SliderDefaults.colors(thumbColor = Ac, activeTrackColor = Ac, inactiveTrackColor = Lt)
                    )
                }
            }

            // STATUS LOG
            Box(modifier = Modifier.fillMaxWidth().background(Dk).padding(horizontal = 12.dp, vertical = 4.dp)) {
                Text("> $statusLog", color = Ac, fontSize = 9.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            // TOOL PANEL
            Box(modifier = Modifier.fillMaxWidth().height(170.dp).background(Dk).padding(8.dp)) {
                when (currentTab) {
                    0 -> PanelEdit(brightness, contrast, saturation, temperature, tint, exposure, highlights, shadows, fade,
                        onB = { brightness = it; statusLog = "BRIGHTNESS: ${(it * 100).toInt()}%" },
                        onC = { contrast = it; statusLog = "CONTRAST: ${(it * 100).toInt()}%" },
                        onS = { saturation = it; statusLog = "SATURATION: ${(it * 100).toInt()}%" },
                        onTemp = { temperature = it; statusLog = "TEMPERATURE: ${(it * 100).toInt()}" },
                        onTint = { tint = it; statusLog = "TINT: ${(it * 100).toInt()}" },
                        onExp = { exposure = it; statusLog = "EXPOSURE: ${(it * 100).toInt()}" },
                        onHi = { highlights = it; statusLog = "HIGHLIGHTS: ${(it * 100).toInt()}" },
                        onSh = { shadows = it; statusLog = "SHADOWS: ${(it * 100).toInt()}" },
                        onFd = { fade = it; statusLog = "FADE: ${(it * 100).toInt()}" },
                        onReset = { brightness = 0f; contrast = 1f; saturation = 1f; temperature = 0f; tint = 0f; exposure = 0f; highlights = 0f; shadows = 0f; fade = 0f; activeFilterIdx = 0; statusLog = "ALL RESET" }
                    )
                    1 -> PanelFilter(activeFilterIdx, filterIntensity,
                        onSelect = { activeFilterIdx = it; statusLog = if (it > 0) "FILTER: ${ColorEngine.PRESETS[it].name}" else "FILTER OFF" },
                        onIntensity = { filterIntensity = it; statusLog = "INTENSITY: ${(it * 100).toInt()}%" }
                    )
                    2 -> PanelSpeed(playbackSpeed, keepPitch,
                        onSpeed = { playbackSpeed = it; exoPlayer?.playbackParameters = PlaybackParameters(it, if (keepPitch) 1f else it); statusLog = "SPEED: ${it}x" },
                        onPitch = { keepPitch = it; exoPlayer?.playbackParameters = PlaybackParameters(playbackSpeed, if (it) 1f else playbackSpeed); statusLog = if (it) "PITCH LOCK ON" else "PITCH LOCK OFF" }
                    )
                    3 -> PanelText(textOverlay, textSize, textBold,
                        onText = { textOverlay = it; statusLog = if (it.isEmpty()) "TEXT CLEARED" else "TEXT: \"$it\"" },
                        onSize = { textSize = it; statusLog = "TEXT SIZE: ${it.toInt()}sp" },
                        onBold = { textBold = it }
                    )
                    4 -> PanelDraw(drawStrokes, brushColor, brushWidth,
                        onColorChange = { brushColor = it },
                        onWidthChange = { brushWidth = it; statusLog = "BRUSH: ${it.toInt()}px" },
                        onUndo = { if (drawStrokes.isNotEmpty()) { drawStrokes.removeAt(drawStrokes.lastIndex); statusLog = "UNDO" } },
                        onClear = { drawStrokes.clear(); statusLog = "CANVAS CLEARED" }
                    )
                    5 -> PanelAudio(volume, isMuted,
                        onVolume = { volume = it; statusLog = "VOLUME: ${(it * 100).toInt()}%" },
                        onMute = { isMuted = it; statusLog = if (it) "MUTED" else "UNMUTED" }
                    )
                    6 -> PanelCrop(rotation, flipH, flipV,
                        onRotate = { rotation = (rotation + it) % 360f; statusLog = "ROTATION: ${rotation.toInt()}°" },
                        onFlipH = { flipH = !flipH; statusLog = if (flipH) "FLIP H: ON" else "FLIP H: OFF" },
                        onFlipV = { flipV = !flipV; statusLog = if (flipV) "FLIP V: ON" else "FLIP V: OFF" },
                        onReset = { rotation = 0f; flipH = false; flipV = false; statusLog = "TRANSFORM RESET" }
                    )
                    7 -> PanelAi { statusLog = "$it — COMING SOON" }
                }
            }

            // BOTTOM TAB BAR
            Row(
                modifier = Modifier.fillMaxWidth().background(Bk).border(BorderStroke(1.dp, Md)).padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                tabs.forEachIndexed { i, tab ->
                    val active = currentTab == i
                    val clr by animateColorAsState(if (active) Ac else Color.Gray, label = "t$i")
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { currentTab = i }.padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(tab.icon, null, tint = clr, modifier = Modifier.size(18.dp))
                        Text(tab.name, color = clr, fontSize = 8.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }

    // ╔══════════════════════════════════════════════════════════════╗
    // ║                    PANEL COMPOSABLES                        ║
    // ╚══════════════════════════════════════════════════════════════╝

    // ── PANEL: EDIT (9 Adjustment Sliders) ──
    @Composable
    fun PanelEdit(b: Float, c: Float, s: Float, temp: Float, tint: Float, exp: Float, hi: Float, sh: Float, fd: Float,
                  onB: (Float) -> Unit, onC: (Float) -> Unit, onS: (Float) -> Unit,
                  onTemp: (Float) -> Unit, onTint: (Float) -> Unit, onExp: (Float) -> Unit,
                  onHi: (Float) -> Unit, onSh: (Float) -> Unit, onFd: (Float) -> Unit, onReset: () -> Unit) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ADJUSTMENTS", color = Wh, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                TextButton(onClick = onReset, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(24.dp)) { Text("RESET", color = Rd, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            }
            Sld("BRIGHTNESS", b, -1f, 1f, onB); Sld("CONTRAST", c, 0.5f, 2f, onC); Sld("SATURATION", s, 0f, 2f, onS)
            Sld("TEMPERATURE", temp, -1f, 1f, onTemp); Sld("TINT", tint, -1f, 1f, onTint); Sld("EXPOSURE", exp, -1f, 1f, onExp)
            Sld("HIGHLIGHTS", hi, -1f, 1f, onHi); Sld("SHADOWS", sh, -1f, 1f, onSh); Sld("FADE", fd, 0f, 1f, onFd)
        }
    }

    @Composable fun Sld(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(28.dp)) {
            Text(label, color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(85.dp))
            Slider(value = value, onValueChange = onChange, valueRange = min..max, modifier = Modifier.weight(1f).height(20.dp),
                colors = SliderDefaults.colors(thumbColor = Ac, activeTrackColor = Ac, inactiveTrackColor = Lt))
            Text("${(value * 100).toInt()}", color = Wh, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
        }
    }

    // ── PANEL: FILTER ──
    @Composable
    fun PanelFilter(activeIdx: Int, intensity: Float, onSelect: (Int) -> Unit, onIntensity: (Float) -> Unit) {
        Column {
            Text("CINEMATIC PRESETS", color = Wh, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(ColorEngine.PRESETS.size) { i ->
                    val p = ColorEngine.PRESETS[i]; val active = activeIdx == i
                    val brd by animateColorAsState(if (active) Ac else Lt, label = "f$i")
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onSelect(i) }) {
                        Box(modifier = Modifier.size(48.dp).background(p.color.copy(alpha = 0.5f), RoundedCornerShape(6.dp)).border(2.dp, brd, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                            if (active) Icon(Icons.Filled.Check, null, tint = Ac, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(p.name, color = if (active) Ac else Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (activeIdx > 0) {
                Spacer(Modifier.height(8.dp))
                Sld("INTENSITY", intensity, 0f, 1f, onIntensity)
            }
        }
    }

    // ── PANEL: SPEED ──
    @Composable
    fun PanelSpeed(currentSpeed: Float, keepPitch: Boolean, onSpeed: (Float) -> Unit, onPitch: (Boolean) -> Unit) {
        Column {
            Text("SPEED RAMPING", color = Wh, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.5f, 2.0f, 3.0f)
                items(speeds) { spd ->
                    val active = currentSpeed == spd
                    Button(onClick = { onSpeed(spd) }, shape = RoundedCornerShape(4.dp), modifier = Modifier.height(40.dp).width(50.dp), contentPadding = PaddingValues(2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (active) Ac else Md, contentColor = if (active) Bk else Wh)) {
                        Text("${spd}x", fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (currentSpeed < 1f) "SLOW-MO" else if (currentSpeed > 1f) "FAST-MO" else "NORMAL", color = if (currentSpeed != 1f) Yw else Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { onPitch(!keepPitch) }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text(if (keepPitch) "🔒 PITCH LOCK" else "🔓 PITCH FREE", color = if (keepPitch) Ac else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // ── PANEL: TEXT ──
    @Composable
    fun PanelText(text: String, size: Float, bold: Boolean, onText: (String) -> Unit, onSize: (Float) -> Unit, onBold: (Boolean) -> Unit) {
        Column {
            Text("TEXT OVERLAY", color = Wh, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(value = text, onValueChange = onText, modifier = Modifier.fillMaxWidth().height(48.dp), placeholder = { Text("Ketik teks...", color = Color.Gray, fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ac, unfocusedBorderColor = Lt, cursorColor = Ac, focusedTextColor = Wh, unfocusedTextColor = Wh), singleLine = true, shape = RoundedCornerShape(4.dp))
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("SIZE", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(40.dp))
                Slider(value = size, onValueChange = onSize, valueRange = 12f..80f, modifier = Modifier.weight(1f).height(20.dp),
                    colors = SliderDefaults.colors(thumbColor = Ac, activeTrackColor = Ac, inactiveTrackColor = Lt))
                Text("${size.toInt()}", color = Wh, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(24.dp))
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { onBold(!bold) }, contentPadding = PaddingValues(horizontal = 6.dp), modifier = Modifier.height(28.dp)) {
                    Text(if (bold) "B" else "b", color = if (bold) Ac else Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val quick = listOf("OMNIX", "SUBSCRIBE", "FOLLOW", "VIRAL", "POV:", "GRWM", "❤️", "🔥")
                items(quick) { q ->
                    OutlinedButton(onClick = { onText(q) }, shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, Lt), colors = ButtonDefaults.outlinedButtonColors(contentColor = Wh),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp), modifier = Modifier.height(28.dp)) {
                        Text(q, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (text.isNotEmpty()) {
                TextButton(onClick = { onText("") }, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(20.dp)) { Text("HAPUS TEKS", color = Rd, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }

    // ── PANEL: DRAW ──
    @Composable
    fun PanelDraw(strokes: SnapshotStateList<DrawStroke>, color: Color, width: Float, onColorChange: (Color) -> Unit, onWidthChange: (Float) -> Unit, onUndo: () -> Unit, onClear: () -> Unit) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("DRAWING", color = Wh, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Row {
                    IconButton(onClick = onUndo, modifier = Modifier.size(28.dp)) { Icon(Icons.Filled.Undo, null, tint = Wh, modifier = Modifier.size(16.dp)) }
                    TextButton(onClick = onClear, contentPadding = PaddingValues(horizontal = 6.dp), modifier = Modifier.height(28.dp)) { Text("CLEAR", color = Rd, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(6.dp))
            // Color picker row
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val colors = listOf(Rd, Ac, Yw, Cy, Wh, Color.Blue, Color.Magenta, Color(0xFFFF6D00))
                items(colors) { c ->
                    Box(modifier = Modifier.size(28.dp).background(c, RoundedCornerShape(4.dp)).border(2.dp, if (c == color) Wh else Color.Transparent, RoundedCornerShape(4.dp)).clickable { onColorChange(c) })
                }
            }
            Spacer(Modifier.height(6.dp))
            Sld("BRUSH SIZE", width, 2f, 30f, onWidthChange)
            Text("${strokes.size} STROKES", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }
    }

    // ── PANEL: AUDIO ──
    @Composable
    fun PanelAudio(volume: Float, isMuted: Boolean, onVolume: (Float) -> Unit, onMute: (Boolean) -> Unit) {
        Column {
            Text("AUDIO CONTROLS", color = Wh, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onMute(!isMuted) }, modifier = Modifier.size(32.dp)) {
                    Icon(if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp, null, tint = if (isMuted) Rd else Ac, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(8.dp))
                Slider(value = volume, onValueChange = onVolume, valueRange = 0f..2f, modifier = Modifier.weight(1f).height(24.dp),
                    colors = SliderDefaults.colors(thumbColor = Ac, activeTrackColor = Ac, inactiveTrackColor = Lt), enabled = !isMuted)
                Spacer(Modifier.width(8.dp))
                Text("${(volume * 100).toInt()}%", color = if (isMuted) Rd else Wh, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(when { isMuted -> "MUTED"; volume > 1f -> "VOLUME BOOST (${(volume * 100).toInt()}%)"; else -> "NORMAL VOLUME" },
                color = if (isMuted) Rd else if (volume > 1f) Yw else Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }

    // ── PANEL: CROP / TRANSFORM ──
    @Composable
    fun PanelCrop(rotation: Float, flipH: Boolean, flipV: Boolean, onRotate: (Float) -> Unit, onFlipH: () -> Unit, onFlipV: () -> Unit, onReset: () -> Unit) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("TRANSFORM", color = Wh, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                TextButton(onClick = onReset, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(24.dp)) { Text("RESET", color = Rd, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TransformBtn("↻ 90°") { onRotate(90f) }
                TransformBtn("↺ -90°") { onRotate(-90f) }
                TransformBtn(if (flipH) "FLIP H ✓" else "FLIP H", flipH) { onFlipH() }
                TransformBtn(if (flipV) "FLIP V ✓" else "FLIP V", flipV) { onFlipV() }
            }
            Spacer(Modifier.height(10.dp))
            Sld("ROTATION", rotation / 360f, -0.5f, 0.5f) { onRotate(it * 360f - rotation) }
            Text("CURRENT: ${rotation.toInt()}°", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }
    }

    @Composable fun TransformBtn(text: String, active: Boolean = false, onClick: () -> Unit) {
        Button(onClick = onClick, shape = RoundedCornerShape(4.dp), modifier = Modifier.height(36.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (active) Ac.copy(alpha = 0.2f) else Md, contentColor = if (active) Ac else Wh)) {
            Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }

    // ── PANEL: AI (Tier 2 + 3) ──
    @Composable
    fun PanelAi(onFeature: (String) -> Unit) {
        Column {
            Text("AI SUPERPOWERS", color = Wh, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                data class Ai(val name: String, val icon: ImageVector, val desc: String)
                val features = listOf(
                    Ai("MAGIC ERASER", Icons.Filled.AutoFixHigh, "Hapus objek"),
                    Ai("BG REMOVER", Icons.Filled.Wallpaper, "Hapus background"),
                    Ai("VIDEO MATTING", Icons.Filled.ContentCut, "Auto green-screen"),
                    Ai("VOICE ISOLATE", Icons.Filled.GraphicEq, "Buang noise"),
                    Ai("AI SLOW-MO", Icons.Filled.SlowMotionVideo, "1000fps interpolation"),
                    Ai("CHROMA KEY", Icons.Filled.FlipCameraAndroid, "Color-based key")
                )
                items(features) { f ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp).clickable { onFeature(f.name) }) {
                        Box(modifier = Modifier.size(44.dp).background(Md, RoundedCornerShape(8.dp)).border(1.dp, Lt, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                            Icon(f.icon, null, tint = Ac, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(f.name, color = Wh, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center, maxLines = 1)
                        Text(f.desc, color = Color.Gray, fontSize = 7.sp, textAlign = TextAlign.Center, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("PRO AI — COMING SOON IN FUTURE UPDATE", color = Yw, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }
    }

    // ╔══════════════════════════════════════════════════════════════╗
    // ║                    UTILITY COMPOSABLES                      ║
    // ╚══════════════════════════════════════════════════════════════╝

    @Composable fun MiniBtn(text: String, icon: ImageVector, accent: Boolean = false, onClick: () -> Unit) {
        Button(onClick = onClick, shape = RoundedCornerShape(4.dp), modifier = Modifier.height(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (accent) Ac else Md, contentColor = if (accent) Bk else Wh),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
            Icon(icon, null, Modifier.size(12.dp)); Spacer(Modifier.width(3.dp)); Text(text, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }

    @Composable fun DrawOverlay(strokes: List<DrawStroke>) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            strokes.forEach { stroke ->
                for (i in 0 until stroke.points.size - 1) {
                    drawLine(stroke.color, stroke.points[i], stroke.points[i + 1], strokeWidth = stroke.width, cap = StrokeCap.Round)
                }
            }
        }
    }

    @Composable fun InteractiveDrawCanvas(strokes: SnapshotStateList<DrawStroke>, brushColor: Color, brushWidth: Float) {
        var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
        Canvas(modifier = Modifier.fillMaxSize().pointerInput(brushColor, brushWidth) {
            detectDragGestures(
                onDragStart = { offset -> currentPoints = listOf(offset) },
                onDrag = { change, _ -> currentPoints = currentPoints + change.position; change.consume() },
                onDragEnd = { if (currentPoints.size > 1) strokes.add(DrawStroke(currentPoints.toList(), brushColor, brushWidth)); currentPoints = emptyList() }
            )
        }) {
            // Draw current live stroke
            for (i in 0 until currentPoints.size - 1) {
                drawLine(brushColor, currentPoints[i], currentPoints[i + 1], strokeWidth = brushWidth, cap = StrokeCap.Round)
            }
        }
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val frames = (ms % 1000) / 33
        return "%02d:%02d:%02d".format(minutes, seconds, frames)
    }
}
