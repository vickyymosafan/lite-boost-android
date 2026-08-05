package com.optimizer.android

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Redo
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import androidx.media3.common.Effect
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbMatrix
import androidx.media3.effect.TextureOverlay
import com.google.common.collect.ImmutableList
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.TransformationRequest
import androidx.media3.transformer.Transformer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

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
// ║                    DATA CLASSES & ENUMS                         ║
// ╚══════════════════════════════════════════════════════════════════╝
enum class DrawToolType { PEN, RECTANGLE, CIRCLE, LINE }
data class DrawAction(val points: List<Offset>, val color: Color, val width: Float, val tool: DrawToolType = DrawToolType.PEN)
data class FilterPreset(val name: String, val color: Color, val matrix: FloatArray)
data class TabItem(val name: String, val icon: ImageVector)
data class ExportQuality(val label: String, val height: Int)
data class ExportFps(val label: String, val fps: Int)

// ╔══════════════════════════════════════════════════════════════════╗
// ║                    COLOR ENGINE (4x5 UI & 4x4 Video)            ║
// ╚══════════════════════════════════════════════════════════════════╝
object ColorEngine {
    // For Photo/UI (4x5 ColorMatrix with offset)
    fun buildMatrix(
        brightness: Float = 0f, contrast: Float = 1f, saturation: Float = 1f,
        temperature: Float = 0f, tint: Float = 0f, exposure: Float = 0f,
        highlights: Float = 0f, shadows: Float = 0f, fade: Float = 0f,
        sharpness: Float = 0f
    ): ColorMatrix {
        val b = brightness * 255f + exposure * 128f
        val c = contrast + sharpness * 0.3f
        val t = (1f - c) / 2f * 255f
        val tempR = temperature * 30f; val tempB = -temperature * 30f
        val tintG = tint * 20f
        val fadeVal = fade * 50f
        val hiScale = 1f + highlights * 0.15f
        val shOff = shadows * 40f
        val cm = ColorMatrix(floatArrayOf(
            c * hiScale, 0f, 0f, 0f, t + b + tempR + fadeVal + shOff,
            0f, c * hiScale, 0f, 0f, t + b + tintG + fadeVal + shOff,
            0f, 0f, c * hiScale, 0f, t + b + tempB + fadeVal + shOff,
            0f, 0f, 0f, 1f, 0f
        ))
        val satMatrix = ColorMatrix()
        satMatrix.setToSaturation(saturation)
        cm.timesAssign(satMatrix)
        return cm
    }

    // For Video (Media3 RgbMatrix 4x4 - scaling instead of offset for approximation)
    @OptIn(UnstableApi::class)
    fun buildVideoEffect(cMatrix: ColorMatrix): Effect {
        return RgbMatrix { _, _ ->
            val v = cMatrix.values
            // Convert 4x5 to 4x4 (discarding 5th column translation because RgbMatrix only scales)
            // We approximate brightness/offsets by slightly scaling the diagonals
            val rScale = v[0] + (v[4] / 255f)
            val gScale = v[6] + (v[9] / 255f)
            val bScale = v[12] + (v[14] / 255f)
            floatArrayOf(
                rScale, v[1], v[2], 0f,
                v[5], gScale, v[7], 0f,
                v[10], v[11], bScale, 0f,
                0f, 0f, 0f, 1f
            )
        }
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
        FilterPreset("VIVID", Color(0xFFE91E63), floatArrayOf(1.3f,0f,0f,0f,5f, 0f,1.3f,0f,0f,5f, 0f,0f,1.3f,0f,5f, 0f,0f,0f,1f,0f)),
        FilterPreset("INVERT", Color(0xFFFFFFFF), floatArrayOf(-1f,0f,0f,0f,255f, 0f,-1f,0f,0f,255f, 0f,0f,-1f,0f,255f, 0f,0f,0f,1f,0f))
    )
}

// ╔══════════════════════════════════════════════════════════════════╗
// ║                    MAIN ACTIVITY                                ║
// ╚══════════════════════════════════════════════════════════════════╝
@AndroidEntryPoint
class ProStudioActivity : ComponentActivity() {

    private var exoPlayer: ExoPlayer? = null
    private val viewModel: com.optimizer.android.presentation.StudioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exoPlayer = ExoPlayer.Builder(this).build()
        setContent { StudioEditorApp() }
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release(); exoPlayer = null
    }

    // ═══════════ EXPORT: VIDEO (Transformer) ═══════════
    @OptIn(UnstableApi::class)
    private fun exportVideo(
        sourceUri: Uri, trimStartMs: Long, trimEndMs: Long,
        qualityHeight: Int, fps: Int,
        colorEffect: Effect?,
        drawActions: List<DrawAction>, previewWidth: Float, previewHeight: Float,
        textToBurn: String, textColorInt: Int, textSizePx: Float, textHasBg: Boolean, textBgColor: Int,
        onProgress: (String) -> Unit, onComplete: (String) -> Unit, onError: (String) -> Unit
    ) {
        val outputDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: filesDir
        val outputFile = File(outputDir, "OMNIX_${System.currentTimeMillis()}.mp4")

        val clipping = MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(trimStartMs).setEndPositionMs(trimEndMs).build()
        val mediaItem = MediaItem.Builder().setUri(sourceUri).setClippingConfiguration(clipping).build()

        val videoEffects = mutableListOf<Effect>()
        if (colorEffect != null) videoEffects.add(colorEffect)

        // Generate combined drawing & text overlay bitmap (1080p canvas)
        val overlayW = 1920; val overlayH = 1080
        val overlayBmp = Bitmap.createBitmap(overlayW, overlayH, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(overlayBmp)
        
        // Render drawings mapped to 1080p
        val scaleX = overlayW / previewWidth; val scaleY = overlayH / previewHeight
        val drawPaint = android.graphics.Paint().apply { style = android.graphics.Paint.Style.STROKE; strokeCap = android.graphics.Paint.Cap.ROUND; isAntiAlias = true }
        val fillPaint = android.graphics.Paint().apply { style = android.graphics.Paint.Style.FILL; isAntiAlias = true }
        
        for (a in drawActions) {
            drawPaint.color = android.graphics.Color.argb(255, (a.color.red*255).toInt(), (a.color.green*255).toInt(), (a.color.blue*255).toInt())
            drawPaint.strokeWidth = a.width * ((scaleX + scaleY) / 2)
            if (a.tool == DrawToolType.PEN && a.points.size > 1) {
                for (i in 0 until a.points.size - 1) {
                    canvas.drawLine(a.points[i].x * scaleX, a.points[i].y * scaleY, a.points[i+1].x * scaleX, a.points[i+1].y * scaleY, drawPaint)
                }
            } else if (a.tool == DrawToolType.RECTANGLE && a.points.size >= 2) {
                val s = a.points.first(); val e = a.points.last()
                canvas.drawRect(min(s.x, e.x) * scaleX, min(s.y, e.y) * scaleY, max(s.x, e.x) * scaleX, max(s.y, e.y) * scaleY, drawPaint)
            } else if (a.tool == DrawToolType.CIRCLE && a.points.size >= 2) {
                val s = a.points.first(); val e = a.points.last()
                val r = sqrt((e.x - s.x) * (e.x - s.x) + (e.y - s.y) * (e.y - s.y)) / 2 * scaleX
                canvas.drawCircle((s.x+e.x)/2 * scaleX, (s.y+e.y)/2 * scaleY, r, drawPaint)
            }
        }

        // Render Text
        if (textToBurn.isNotEmpty()) {
            val textPaint = android.graphics.Paint().apply {
                color = textColorInt; textSize = textSizePx * 3f; isAntiAlias = true
                isFakeBoldText = true; textAlign = android.graphics.Paint.Align.CENTER
            }
            if (textHasBg) {
                fillPaint.color = textBgColor; fillPaint.alpha = 180
                val bounds = android.graphics.Rect()
                textPaint.getTextBounds(textToBurn, 0, textToBurn.length, bounds)
                canvas.drawRoundRect(960f - bounds.width()/2 - 40f, 540f - bounds.height() - 20f, 960f + bounds.width()/2 + 40f, 540f + 20f, 20f, 20f, fillPaint)
            }
            canvas.drawText(textToBurn, 960f, 540f, textPaint)
        }

        val overlay = object : BitmapOverlay() { override fun getBitmap(timeUs: Long): Bitmap = overlayBmp }
        videoEffects.add(OverlayEffect(ImmutableList.of<TextureOverlay>(overlay)))
        videoEffects.add(Presentation.createForHeight(qualityHeight))

        val edited = EditedMediaItem.Builder(mediaItem).setEffects(Effects(emptyList(), videoEffects)).build()
        
        val transformRequest = TransformationRequest.Builder()
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            // Note: Media3 Transformer currently manages frame rates internally via codec parameters, but we can set constraints if needed
            .build()

        val transformer = Transformer.Builder(this)
            .setTransformationRequest(transformRequest)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(c: Composition, r: ExportResult) {
                    viewModel.saveToMediaStore(this@ProStudioActivity, outputFile, "video/mp4", "Movies/OMNIX")
                    onComplete(outputFile.absolutePath)
                }
                override fun onError(c: Composition, r: ExportResult, e: ExportException) { onError(e.message ?: "Failed") }
            }).build()
            
        onProgress("ENCODING ${qualityHeight}p → ${outputFile.name}")
        transformer.start(edited, outputFile.absolutePath)
    }





    private fun cropToRatio(bmp: Bitmap, ratioW: Float, ratioH: Float): Bitmap {
        val targetR = ratioW / ratioH; val currentR = bmp.width.toFloat() / bmp.height.toFloat()
        return if (currentR > targetR) {
            val nw = (bmp.height * targetR).toInt(); Bitmap.createBitmap(bmp, (bmp.width - nw) / 2, 0, nw, bmp.height)
        } else {
            val nh = (bmp.width / targetR).toInt(); Bitmap.createBitmap(bmp, 0, (bmp.height - nh) / 2, bmp.width, nh)
        }
    }

    // ╔══════════════════════════════════════════════════════════════╗
    // ║              MAIN EDITOR COMPOSABLE                         ║
    // ╚══════════════════════════════════════════════════════════════╝
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    @kotlin.OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun StudioEditorApp() {
        var selectedUri by remember { mutableStateOf<Uri?>(null) }
        var mediaType by remember { mutableStateOf("none") }
        var isPlaying by remember { mutableStateOf(false) }
        var currentTab by remember { mutableIntStateOf(0) }
        var statusLog by remember { mutableStateOf("PILIH VIDEO ATAU FOTO DARI GALERI") }

        // Color grading (12 params)
        var brightness by remember { mutableFloatStateOf(0f) }
        var contrast by remember { mutableFloatStateOf(1f) }
        var saturation by remember { mutableFloatStateOf(1f) }
        var temperature by remember { mutableFloatStateOf(0f) }
        var tint by remember { mutableFloatStateOf(0f) }
        var exposure by remember { mutableFloatStateOf(0f) }
        var highlights by remember { mutableFloatStateOf(0f) }
        var shadows by remember { mutableFloatStateOf(0f) }
        var fade by remember { mutableFloatStateOf(0f) }
        var sharpness by remember { mutableFloatStateOf(0f) }
        var vignette by remember { mutableFloatStateOf(0f) }
        var grain by remember { mutableFloatStateOf(0f) }

        // Filter
        var activeFilterIdx by remember { mutableIntStateOf(0) }
        var filterIntensity by remember { mutableFloatStateOf(1f) }

        // Speed & Trim (Handles)
        var playbackSpeed by remember { mutableFloatStateOf(1f) }
        var keepPitch by remember { mutableStateOf(true) }
        var trimStartRatio by remember { mutableFloatStateOf(0f) }
        var trimEndRatio by remember { mutableFloatStateOf(1f) }

        // Text
        var textOverlay by remember { mutableStateOf("") }
        var textSize by remember { mutableFloatStateOf(28f) }
        var textBold by remember { mutableStateOf(true) }
        var textColor by remember { mutableStateOf(Wh) }
        var textHasBg by remember { mutableStateOf(false) }
        var textBgColor by remember { mutableStateOf(Bk) }
        var textHasStroke by remember { mutableStateOf(false) }
        var textStrokeColor by remember { mutableStateOf(Bk) }
        var textStrokeWidth by remember { mutableFloatStateOf(2f) }
        var textHasShadow by remember { mutableStateOf(false) }

        // Audio
        var volume by remember { mutableFloatStateOf(1f) }
        var isMuted by remember { mutableStateOf(false) }
        var fadeInSec by remember { mutableFloatStateOf(0f) }
        var fadeOutSec by remember { mutableFloatStateOf(0f) }

        // Draw
        val drawActions = remember { mutableStateListOf<DrawAction>() }
        val redoStack = remember { mutableStateListOf<DrawAction>() }
        var brushColor by remember { mutableStateOf(Rd) }
        var brushWidth by remember { mutableFloatStateOf(8f) }
        var drawTool by remember { mutableStateOf(DrawToolType.PEN) }

        // Transform & Export settings
        var rotation by remember { mutableFloatStateOf(0f) }
        var flipH by remember { mutableStateOf(false) }
        var flipV by remember { mutableStateOf(false) }
        var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
        var expQuality by remember { mutableStateOf(ExportQuality("1080p", 1080)) }
        var expFps by remember { mutableStateOf(ExportFps("30fps", 30)) }
        var expMenuOpen by remember { mutableStateOf(false) }

        // Playback & Preview Canvas Dimensions
        var currentPositionMs by remember { mutableStateOf(0L) }
        var durationMs by remember { mutableStateOf(1L) }
        var previewW by remember { mutableFloatStateOf(1000f) }
        var previewH by remember { mutableFloatStateOf(1000f) }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        // Matrices & Live ExoPlayer Effects
        val adjustMatrix = remember(brightness, contrast, saturation, temperature, tint, exposure, highlights, shadows, fade, sharpness) {
            ColorEngine.buildMatrix(brightness, contrast, saturation, temperature, tint, exposure, highlights, shadows, fade, sharpness)
        }
        val filterMatrix = remember(activeFilterIdx, filterIntensity) {
            if (activeFilterIdx > 0) {
                val p = ColorEngine.PRESETS[activeFilterIdx]; val id = floatArrayOf(1f,0f,0f,0f,0f, 0f,1f,0f,0f,0f, 0f,0f,1f,0f,0f, 0f,0f,0f,1f,0f)
                ColorMatrix(FloatArray(20) { i -> id[i] + (p.matrix[i] - id[i]) * filterIntensity })
            } else null
        }
        val combinedMatrix = remember(adjustMatrix, filterMatrix) {
            if (filterMatrix != null) { val cm = ColorMatrix(adjustMatrix.values.clone()); cm.timesAssign(filterMatrix); cm } else adjustMatrix
        }

        // Apply Media3 Video Effects for live preview
        LaunchedEffect(combinedMatrix, exoPlayer) {
            val videoEffect = ColorEngine.buildVideoEffect(combinedMatrix)
            exoPlayer?.setVideoEffects(listOf(videoEffect))
        }

        // Launchers
        val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) { selectedUri = uri; mediaType = "video"; statusLog = "VIDEO LOADED"
                trimStartRatio = 0f; trimEndRatio = 1f
                exoPlayer?.let { it.setMediaItem(MediaItem.fromUri(uri)); it.prepare(); it.playWhenReady = true; isPlaying = true } }
        }
        val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) { selectedUri = uri; mediaType = "image"; statusLog = "FOTO LOADED"; exoPlayer?.pause(); isPlaying = false
                scope.launch { photoBitmap = withContext(Dispatchers.IO) {
                    try {
                        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                        val scale = max(1, max(opts.outWidth, opts.outHeight) / 2048)
                        val decOpts = BitmapFactory.Options().apply { inSampleSize = scale }
                        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decOpts) }
                    } catch (e: Exception) { null }
                } }
            }
        }

        // Position tracking & Trim bounds
        LaunchedEffect(isPlaying, trimStartRatio, trimEndRatio) {
            while (isPlaying) {
                val cur = exoPlayer?.currentPosition ?: 0L
                val dur = exoPlayer?.duration?.coerceAtLeast(1L) ?: 1L
                currentPositionMs = cur; durationMs = dur
                
                val startMs = (trimStartRatio * dur).toLong()
                val endMs = (trimEndRatio * dur).toLong()
                if (cur < startMs || cur > endMs) {
                    exoPlayer?.seekTo(startMs)
                }
                
                // Live Fade Audio simulation (UI only, export uses processor or default)
                if (dur > 0 && fadeInSec > 0) {
                    val fadeMs = (fadeInSec * 1000).toLong()
                    if (cur - startMs < fadeMs) {
                        val fadeVol = (cur - startMs).toFloat() / fadeMs.toFloat()
                        exoPlayer?.volume = if (isMuted) 0f else volume * fadeVol
                    } else if (endMs - cur < (fadeOutSec * 1000).toLong()) {
                        val fadeOutMs = (fadeOutSec * 1000).toLong()
                        val fadeVol = (endMs - cur).toFloat() / fadeOutMs.toFloat()
                        exoPlayer?.volume = if (isMuted) 0f else volume * fadeVol
                    } else {
                        exoPlayer?.volume = if (isMuted) 0f else volume
                    }
                } else {
                    exoPlayer?.volume = if (isMuted) 0f else volume
                }
                delay(100)
            }
        }

        val tabs = listOf(TabItem("EDIT", Icons.Filled.Tune), TabItem("FILTER", Icons.Filled.AutoAwesome), TabItem("SPEED", Icons.Filled.Speed), TabItem("TEXT", Icons.Filled.TextFields),
            TabItem("DRAW", Icons.Filled.Brush), TabItem("AUDIO", Icons.Filled.MusicNote), TabItem("CROP", Icons.Filled.Crop), TabItem("AI", Icons.Filled.AutoFixHigh))

        // ═══ LAYOUT ═══
        Column(modifier = Modifier.fillMaxSize().background(Bk)) {
            // TOP BAR
            Row(Modifier.fillMaxWidth().background(Md).padding(bottom = 1.dp).background(Dk).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { finish() }, Modifier.size(36.dp)) { Icon(Icons.Filled.ArrowBack, null, tint = Wh, modifier = Modifier.size(20.dp)) }
                    Spacer(Modifier.width(8.dp)); Text("OMNIX STUDIO", color = Wh, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { videoLauncher.launch("video/*") }, shape = RoundedCornerShape(4.dp), modifier = Modifier.height(36.dp), colors = ButtonDefaults.buttonColors(containerColor = Md, contentColor = Wh), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)) { Icon(Icons.Filled.Videocam, null, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("VID", fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                    Button(onClick = { imageLauncher.launch("image/*") }, shape = RoundedCornerShape(4.dp), modifier = Modifier.height(36.dp), colors = ButtonDefaults.buttonColors(containerColor = Md, contentColor = Wh), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)) { Icon(Icons.Filled.Image, null, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("PIC", fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                    Box {
                        OutlinedButton(onClick = { expMenuOpen = true }, shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, Cy), colors = ButtonDefaults.outlinedButtonColors(contentColor = Cy), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp), modifier = Modifier.height(36.dp)) {
                            Text("${expQuality.label} | ${expFps.label}", fontSize = 9.sp, fontWeight = FontWeight.Bold); Icon(Icons.Filled.ArrowDropDown, null, Modifier.size(16.dp))
                        }
                        DropdownMenu(expanded = expMenuOpen, onDismissRequest = { expMenuOpen = false }, modifier = Modifier.background(Md)) {
                            listOf(ExportQuality("480p", 480), ExportQuality("720p", 720), ExportQuality("1080p", 1080)).forEach { q ->
                                DropdownMenuItem(text = { Text(q.label, color = Wh) }, onClick = { expQuality = q; expMenuOpen = false })
                            }
                            listOf(ExportFps("24fps", 24), ExportFps("30fps", 30), ExportFps("60fps", 60)).forEach { f ->
                                DropdownMenuItem(text = { Text(f.label, color = Yw) }, onClick = { expFps = f; expMenuOpen = false })
                            }
                        }
                    }
                    Button(onClick = {
                        when {
                            selectedUri == null -> Toast.makeText(context, "Pilih media dulu!", Toast.LENGTH_SHORT).show()
                            mediaType == "video" -> { statusLog = "EXPORTING..."
                                val tc = if (textColor == Wh) android.graphics.Color.WHITE else android.graphics.Color.argb(255, (textColor.red*255).toInt(), (textColor.green*255).toInt(), (textColor.blue*255).toInt())
                                val bgc = android.graphics.Color.argb(255, (textBgColor.red*255).toInt(), (textBgColor.green*255).toInt(), (textBgColor.blue*255).toInt())
                                exportVideo(selectedUri!!, (trimStartRatio * durationMs).toLong(), (trimEndRatio * durationMs).toLong(),
                                    expQuality.height, expFps.fps, ColorEngine.buildVideoEffect(combinedMatrix), drawActions, previewW, previewH,
                                    textOverlay, tc, textSize, textHasBg, bgc,
                                    onProgress = { statusLog = it }, onComplete = { statusLog = "DONE: $it"; Toast.makeText(context, "Video saved!", Toast.LENGTH_LONG).show() }, onError = { statusLog = "ERR: $it" })
                            }
                            mediaType == "image" && photoBitmap != null -> {
                                val tc = if (textColor == Wh) android.graphics.Color.WHITE else android.graphics.Color.argb(255, (textColor.red*255).toInt(), (textColor.green*255).toInt(), (textColor.blue*255).toInt())
                                val bgc = android.graphics.Color.argb(255, (textBgColor.red*255).toInt(), (textBgColor.green*255).toInt(), (textBgColor.blue*255).toInt())
                                val sc = android.graphics.Color.argb(255, (textStrokeColor.red*255).toInt(), (textStrokeColor.green*255).toInt(), (textStrokeColor.blue*255).toInt())

                                viewModel.exportEditedPhoto(
                                    context = context,
                                    sourceBitmap = photoBitmap!!,
                                    rotation = rotation,
                                    flipH = flipH,
                                    flipV = flipV,
                                    colorMatrix = combinedMatrix,
                                    vignette = vignette,
                                    grain = grain,
                                    drawActions = drawActions,
                                    previewW = previewW,
                                    previewH = previewH,
                                    textOverlay = textOverlay,
                                    textColorInt = tc,
                                    textSizePx = textSize,
                                    textBold = textBold,
                                    textHasBg = textHasBg,
                                    textBgColorInt = bgc,
                                    textHasStroke = textHasStroke,
                                    textStrokeColorInt = sc,
                                    textStrokeWidthPx = textStrokeWidth,
                                    textHasShadow = textHasShadow,
                                    onProgress = { statusLog = it },
                                    onComplete = { statusLog = "SAVED TO GALLERY: $it"; Toast.makeText(context, "Photo saved to Gallery!", Toast.LENGTH_LONG).show() },
                                    onError = { statusLog = "ERR: $it" }
                                )
                            }
                        }
                    }, shape = RoundedCornerShape(4.dp), modifier = Modifier.height(36.dp), colors = ButtonDefaults.buttonColors(containerColor = Ac, contentColor = Bk), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)) { Icon(Icons.Filled.FileDownload, null, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("EXPORT", fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                }
            }

            // PREVIEW AREA
            Box(Modifier.fillMaxWidth().weight(1f).background(Bk).pointerInput(Unit) {
                // Track preview dimensions for mapping drawing to 1080p
                previewW = size.width.toFloat(); previewH = size.height.toFloat()
            }) {
                if (selectedUri != null) {
                    when (mediaType) {
                        "video" -> AndroidView(factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer; useController = false; setBackgroundColor(android.graphics.Color.BLACK) } }, modifier = Modifier.fillMaxSize())
                        "image" -> photoBitmap?.let { bmp ->
                            val imgBmp = remember(bmp, rotation, flipH, flipV) {
                                val m = android.graphics.Matrix().apply { postRotate(rotation); postScale(if (flipH) -1f else 1f, if (flipV) -1f else 1f, bmp.width / 2f, bmp.height / 2f) }
                                Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true).asImageBitmap()
                            }
                            Image(bitmap = imgBmp, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit, colorFilter = ColorFilter.colorMatrix(combinedMatrix))
                        }
                    }
                    if (vignette > 0f) { Canvas(modifier = Modifier.fillMaxSize()) { drawRect(brush = Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = vignette * 0.8f)), center = center, radius = size.minDimension * 0.7f)) } }
                    if (grain > 0f) { val noiseBmp = remember { val w=100; val h=100; val px=IntArray(w*h){ val n=(Math.random()*180).toInt(); android.graphics.Color.argb(50,n,n,n) }; Bitmap.createBitmap(px,w,h,Bitmap.Config.ARGB_8888).asImageBitmap() }
                        Image(bitmap = noiseBmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = grain * 0.5f) }
                    
                    DrawOverlay(drawActions)
                    if (currentTab == 4) InteractiveDrawCanvas(drawActions, redoStack, brushColor, brushWidth, drawTool)
                    
                    if (textOverlay.isNotEmpty()) {
                        Box(modifier = Modifier.align(Alignment.Center)) {
                            if (textHasStroke) { val sw = textStrokeWidth
                                listOf(-sw to 0f, sw to 0f, 0f to -sw, 0f to sw, -sw to -sw, sw to -sw, -sw to sw, sw to sw).forEach { (dx, dy) ->
                                    Text(textOverlay, color = textStrokeColor, fontSize = textSize.sp, fontWeight = if (textBold) FontWeight.Black else FontWeight.Normal, modifier = Modifier.offset(x = dx.dp, y = dy.dp), textAlign = TextAlign.Center) }
                            }
                            if (textHasShadow) { Text(textOverlay, color = Color.Black.copy(alpha = 0.6f), fontSize = textSize.sp, fontWeight = if (textBold) FontWeight.Black else FontWeight.Normal, modifier = Modifier.offset(x = 3.dp, y = 3.dp), textAlign = TextAlign.Center) }
                            val bgMod = if (textHasBg) Modifier.background(textBgColor.copy(alpha = 0.7f), RoundedCornerShape(8.dp)).padding(horizontal = 14.dp, vertical = 6.dp) else Modifier
                            Text(textOverlay, color = textColor, fontSize = textSize.sp, fontWeight = if (textBold) FontWeight.Black else FontWeight.Normal, modifier = bgMod, textAlign = TextAlign.Center)
                        }
                    }
                    if (activeFilterIdx > 0) { Box(Modifier.align(Alignment.TopStart).padding(8.dp).background(Ac, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) { Text(ColorEngine.PRESETS[activeFilterIdx].name, color = Bk, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace) } }
                } else {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
                            label = "icon pulse"
                        )
                        Icon(Icons.Filled.AddPhotoAlternate, null, tint = Lt.copy(alpha = alpha), modifier = Modifier.size(64.dp)); Spacer(Modifier.height(12.dp))
                        Text("OMNIX STUDIO", color = Wh, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Text("Pilih video atau foto untuk mulai", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // TIMELINE & TRIM HANDLES
            if (selectedUri != null && mediaType == "video") {
                Row(Modifier.fillMaxWidth().background(Dk).padding(horizontal = 12.dp, vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(fmtTime(currentPositionMs), color = Ac, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { exoPlayer?.let { if (it.isPlaying) { it.pause(); isPlaying = false } else { it.play(); isPlaying = true } } }, Modifier.size(44.dp)) { Icon(if (isPlaying) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle, null, tint = Ac, modifier = Modifier.size(32.dp)) }
                    }
                    Text(fmtTime(durationMs), color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
                // Interactive Trim Handles using RangeSlider
                RangeSlider(
                    value = trimStartRatio..trimEndRatio,
                    onValueChange = { range -> 
                        trimStartRatio = range.start; trimEndRatio = range.endInclusive
                        val sMs = (range.start * durationMs).toLong()
                        exoPlayer?.seekTo(sMs); currentPositionMs = sMs
                    },
                    modifier = Modifier.fillMaxWidth().height(28.dp).padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = SliderDefaults.colors(thumbColor = Ac, activeTrackColor = Ac.copy(alpha=0.6f), inactiveTrackColor = Lt)
                )
            }

            // STATUS LOG
            Box(Modifier.fillMaxWidth().background(Dk).padding(horizontal = 12.dp, vertical = 4.dp)) { Text("> $statusLog", color = Ac, fontSize = 9.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis) }

            // TOOL PANEL
            Box(Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 220.dp).animateContentSize().background(Dk).padding(8.dp)) {
                Crossfade(targetState = currentTab, label = "panel") { tab ->
                    when (tab) {
                        0 -> PanelEdit(brightness, contrast, saturation, temperature, tint, exposure, highlights, shadows, fade, sharpness, vignette, grain,
                            onB = { brightness = it; statusLog = "BRIGHTNESS: ${(it*100).toInt()}%" }, onC = { contrast = it }, onS = { saturation = it },
                            onTemp = { temperature = it }, onTint = { tint = it }, onExp = { exposure = it },
                            onHi = { highlights = it }, onSh = { shadows = it }, onFd = { fade = it },
                            onSharp = { sharpness = it }, onVig = { vignette = it }, onGrain = { grain = it },
                            onReset = { brightness=0f; contrast=1f; saturation=1f; temperature=0f; tint=0f; exposure=0f; highlights=0f; shadows=0f; fade=0f; sharpness=0f; vignette=0f; grain=0f; activeFilterIdx=0 })
                        1 -> PanelFilter(activeFilterIdx, filterIntensity, onSelect = { activeFilterIdx = it }, onIntensity = { filterIntensity = it })
                        2 -> PanelSpeed(playbackSpeed, keepPitch, onSpeed = { playbackSpeed = it; exoPlayer?.playbackParameters = PlaybackParameters(it, if (keepPitch) 1f else it) }, onPitch = { keepPitch = it; exoPlayer?.playbackParameters = PlaybackParameters(playbackSpeed, if (it) 1f else playbackSpeed) },
                            onFreezeFrame = { if (selectedUri != null && mediaType == "video") { viewModel.captureFrame(context, selectedUri!!, currentPositionMs) { bmp -> photoBitmap = bmp; mediaType = "image"; statusLog = "FREEZE FRAME CAPTURED" } } else statusLog = "LOAD VIDEO FIRST" })
                        3 -> PanelText(textOverlay, textSize, textBold, textColor, textHasBg, textBgColor, textHasStroke, textStrokeColor, textStrokeWidth, textHasShadow,
                            onText = { textOverlay = it }, onSize = { textSize = it }, onBold = { textBold = it }, onColor = { textColor = it }, onBgToggle = { textHasBg = it }, onBgColor = { textBgColor = it }, onStrokeToggle = { textHasStroke = it }, onStrokeColor = { textStrokeColor = it }, onStrokeW = { textStrokeWidth = it }, onShadowToggle = { textHasShadow = it })
                        4 -> PanelDraw(drawActions, redoStack, brushColor, brushWidth, drawTool, onColor = { brushColor = it }, onWidth = { brushWidth = it }, onTool = { drawTool = it },
                            onUndo = { if (drawActions.isNotEmpty()) { redoStack.add(drawActions.removeLast()) } }, onRedo = { if (redoStack.isNotEmpty()) { drawActions.add(redoStack.removeLast()) } }, onClear = { drawActions.clear(); redoStack.clear() })
                        5 -> PanelAudio(volume, isMuted, fadeInSec, fadeOutSec, onVolume = { volume = it }, onMute = { isMuted = it }, onFadeIn = { fadeInSec = it }, onFadeOut = { fadeOutSec = it },
                            onExtract = { if (selectedUri != null && mediaType == "video") { viewModel.extractAudio(context, selectedUri!!, onComplete = { statusLog = "AUDIO: $it"; Toast.makeText(context, "Audio saved!", Toast.LENGTH_LONG).show() }, onError = { statusLog = "ERR: $it" }) } })
                        6 -> PanelCrop(rotation, flipH, flipV, onRotate = { rotation = (rotation + it) % 360f }, onFlipH = { flipH = !flipH }, onFlipV = { flipV = !flipV },
                            onReset = { rotation = 0f; flipH = false; flipV = false },
                            onCropRatio = { w, h -> if (photoBitmap != null) { photoBitmap = cropToRatio(photoBitmap!!, w, h); statusLog = "CROPPED ${w.toInt()}:${h.toInt()}" } })
                        7 -> PanelAi { statusLog = "$it — COMING SOON" }
                    }
                }
            }

            // BOTTOM TAB BAR
            Row(Modifier.fillMaxWidth().background(Bk).border(BorderStroke(1.dp, Md)).padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                tabs.forEachIndexed { i, tab -> val active = currentTab == i; val clr by animateColorAsState(if (active) Ac else Color.Gray, label = "t$i")
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { currentTab = i }.defaultMinSize(minWidth = 44.dp, minHeight = 44.dp).padding(horizontal = 3.dp, vertical = 4.dp), verticalArrangement = Arrangement.Center) {
                        Icon(tab.icon, null, tint = clr, modifier = Modifier.size(22.dp)); Spacer(Modifier.height(2.dp)); Text(tab.name, color = clr, fontSize = 9.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier.height(2.dp).width(20.dp).background(if (active) Ac else Color.Transparent, RoundedCornerShape(1.dp)))
                    }
                }
            }
        }
    }

    // ╔══════════════════════════════════════════════════════════════╗
    // ║              PANEL: EDIT (12 Sliders)                       ║
    // ╚══════════════════════════════════════════════════════════════╝
    @Composable fun PanelEdit(b: Float, c: Float, s: Float, temp: Float, tint: Float, exp: Float, hi: Float, sh: Float, fd: Float, sharp: Float, vig: Float, gr: Float,
                              onB: (Float)->Unit, onC: (Float)->Unit, onS: (Float)->Unit, onTemp: (Float)->Unit, onTint: (Float)->Unit, onExp: (Float)->Unit,
                              onHi: (Float)->Unit, onSh: (Float)->Unit, onFd: (Float)->Unit, onSharp: (Float)->Unit, onVig: (Float)->Unit, onGrain: (Float)->Unit, onReset: ()->Unit) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("ADJUSTMENTS", color = Wh, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp); TextButton(onClick = onReset, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(22.dp)) { Text("RESET", color = Rd, fontSize = 10.sp, fontWeight = FontWeight.Bold) } }
            Sld("BRIGHTNESS", b, -1f, 1f, onB); Sld("CONTRAST", c, 0.5f, 2f, onC); Sld("SATURATION", s, 0f, 2f, onS); Sld("EXPOSURE", exp, -1f, 1f, onExp)
            Sld("TEMPERATURE", temp, -1f, 1f, onTemp); Sld("TINT", tint, -1f, 1f, onTint); Sld("HIGHLIGHTS", hi, -1f, 1f, onHi); Sld("SHADOWS", sh, -1f, 1f, onSh)
            Sld("SHARPNESS", sharp, 0f, 1f, onSharp); Sld("VIGNETTE", vig, 0f, 1f, onVig); Sld("GRAIN", gr, 0f, 1f, onGrain); Sld("FADE", fd, 0f, 1f, onFd)
        }
    }

    @Composable fun PanelFilter(activeIdx: Int, intensity: Float, onSelect: (Int)->Unit, onIntensity: (Float)->Unit) {
        Column { Text("CINEMATIC PRESETS", color = Wh, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp); Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) { items(ColorEngine.PRESETS.size) { i -> val p = ColorEngine.PRESETS[i]; val active = activeIdx == i
                val brd by animateColorAsState(if (active) Ac else Lt, label = "f$i")
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onSelect(i) }) {
                    Box(Modifier.size(44.dp).background(p.color.copy(alpha = 0.5f), RoundedCornerShape(6.dp)).border(2.dp, brd, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) { if (active) Icon(Icons.Filled.Check, null, tint = Ac, modifier = Modifier.size(18.dp)) }
                    Spacer(Modifier.height(3.dp)); Text(p.name, color = if (active) Ac else Color.Gray, fontSize = 7.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                } } }
            if (activeIdx > 0) { Spacer(Modifier.height(6.dp)); Sld("INTENSITY", intensity, 0f, 1f, onIntensity) }
        }
    }

    @Composable fun PanelSpeed(speed: Float, keepP: Boolean, onSpeed: (Float)->Unit, onPitch: (Boolean)->Unit, onFreezeFrame: ()->Unit) {
        Column { Text("SPEED RAMPING", color = Wh, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp); Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.5f, 2.0f, 3.0f)
                items(speeds) { sp -> val a = speed == sp; Button(onClick = { onSpeed(sp) }, shape = RoundedCornerShape(4.dp), modifier = Modifier.height(36.dp).width(48.dp), contentPadding = PaddingValues(2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (a) Ac else Md, contentColor = if (a) Bk else Wh)) { Text("${sp}x", fontSize = 10.sp, fontWeight = FontWeight.Black) } } }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (speed < 1f) "SLOW-MO" else if (speed > 1f) "FAST-MO" else "NORMAL", color = if (speed != 1f) Yw else Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { onPitch(!keepP) }, contentPadding = PaddingValues(horizontal = 6.dp)) { Text(if (keepP) "🔒 PITCH" else "🔓 PITCH", color = if (keepP) Ac else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                TextButton(onClick = onFreezeFrame, contentPadding = PaddingValues(horizontal = 6.dp)) { Icon(Icons.Filled.CameraAlt, null, tint = Cy, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("FREEZE", color = Cy, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }

    @Composable fun PanelText(text: String, size: Float, bold: Boolean, color: Color, hasBg: Boolean, bgColor: Color, hasStroke: Boolean, strokeColor: Color, strokeW: Float, hasShadow: Boolean,
                              onText: (String)->Unit, onSize: (Float)->Unit, onBold: (Boolean)->Unit, onColor: (Color)->Unit, onBgToggle: (Boolean)->Unit, onBgColor: (Color)->Unit,
                              onStrokeToggle: (Boolean)->Unit, onStrokeColor: (Color)->Unit, onStrokeW: (Float)->Unit, onShadowToggle: (Boolean)->Unit) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Text("TEXT OVERLAY", color = Wh, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp); Spacer(Modifier.height(4.dp))
            OutlinedTextField(value = text, onValueChange = onText, modifier = Modifier.fillMaxWidth().height(46.dp), placeholder = { Text("Ketik teks...", color = Color.Gray, fontSize = 11.sp) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ac, unfocusedBorderColor = Lt, cursorColor = Ac, focusedTextColor = Wh, unfocusedTextColor = Wh), singleLine = true, shape = RoundedCornerShape(4.dp))
            Spacer(Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) { val q = listOf("OMNIX", "SUBSCRIBE", "FOLLOW", "VIRAL", "POV:", "GRWM", "❤️", "🔥")
                items(q) { t -> OutlinedButton(onClick = { onText(t) }, shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, Lt), colors = ButtonDefaults.outlinedButtonColors(contentColor = Wh), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 1.dp), modifier = Modifier.height(24.dp)) { Text(t, fontSize = 8.sp, fontWeight = FontWeight.Bold) } } }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) { Text("SIZE", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(32.dp))
                Slider(value = size, onValueChange = onSize, valueRange = 12f..80f, modifier = Modifier.weight(1f).height(18.dp), colors = SliderDefaults.colors(thumbColor = Ac, activeTrackColor = Ac, inactiveTrackColor = Lt))
                Text("${size.toInt()}", color = Wh, fontSize = 8.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(20.dp)); ToggleChip("B", bold) { onBold(!bold) }
            }
            Text("COLOR", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace); Spacer(Modifier.height(2.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { val colors = listOf(Wh, Rd, Ac, Yw, Cy, Color.Blue, Color.Magenta, Color(0xFFFF6D00), Bk)
                items(colors) { c -> Box(Modifier.size(22.dp).background(c, RoundedCornerShape(3.dp)).border(2.dp, if (c == color) Wh else Color.Transparent, RoundedCornerShape(3.dp)).clickable { onColor(c) }) } }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ToggleChip("BG", hasBg) { onBgToggle(!hasBg) }; ToggleChip("STROKE", hasStroke) { onStrokeToggle(!hasStroke) }; ToggleChip("SHADOW", hasShadow) { onShadowToggle(!hasShadow) }
                if (text.isNotEmpty()) TextButton(onClick = { onText("") }, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(22.dp)) { Text("CLEAR", color = Rd, fontSize = 8.sp, fontWeight = FontWeight.Bold) }
            }
            if (hasStroke) { Sld("STROKE W", strokeW, 1f, 5f, onStrokeW) }
        }
    }

    @Composable fun PanelDraw(actions: SnapshotStateList<DrawAction>, redoStack: SnapshotStateList<DrawAction>, color: Color, width: Float, tool: DrawToolType,
                              onColor: (Color)->Unit, onWidth: (Float)->Unit, onTool: (DrawToolType)->Unit, onUndo: ()->Unit, onRedo: ()->Unit, onClear: ()->Unit) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("DRAWING", color = Wh, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Row { IconButton(onClick = onUndo, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Undo, null, tint = Wh, modifier = Modifier.size(14.dp)) }
                    IconButton(onClick = onRedo, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Redo, null, tint = Wh, modifier = Modifier.size(14.dp)) }
                    TextButton(onClick = onClear, contentPadding = PaddingValues(horizontal = 4.dp), modifier = Modifier.height(24.dp)) { Text("CLR", color = Rd, fontSize = 8.sp, fontWeight = FontWeight.Bold) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DrawToolType.values().forEach { t -> val a = tool == t
                    Button(onClick = { onTool(t) }, shape = RoundedCornerShape(4.dp), modifier = Modifier.height(28.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (a) Ac else Md, contentColor = if (a) Bk else Wh)) { Text(t.name, fontSize = 7.sp, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { val colors = listOf(Rd, Ac, Yw, Cy, Wh, Color.Blue, Color.Magenta, Color(0xFFFF6D00))
                items(colors) { c -> Box(Modifier.size(22.dp).background(c, RoundedCornerShape(3.dp)).border(2.dp, if (c == color) Wh else Color.Transparent, RoundedCornerShape(3.dp)).clickable { onColor(c) }) } }
            Spacer(Modifier.height(4.dp)); Sld("SIZE", width, 2f, 30f, onWidth)
        }
    }

    @Composable fun PanelAudio(vol: Float, muted: Boolean, fadeIn: Float, fadeOut: Float, onVolume: (Float)->Unit, onMute: (Boolean)->Unit, onFadeIn: (Float)->Unit, onFadeOut: (Float)->Unit, onExtract: ()->Unit) {
        Column {
            Text("AUDIO CONTROLS", color = Wh, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp); Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onMute(!muted) }, modifier = Modifier.size(28.dp)) { Icon(if (muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp, null, tint = if (muted) Rd else Ac, modifier = Modifier.size(18.dp)) }
                Spacer(Modifier.width(6.dp)); Slider(value = vol, onValueChange = onVolume, valueRange = 0f..2f, modifier = Modifier.weight(1f).height(22.dp), colors = SliderDefaults.colors(thumbColor = Ac, activeTrackColor = Ac, inactiveTrackColor = Lt), enabled = !muted)
                Text("${(vol*100).toInt()}%", color = if (muted) Rd else Wh, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
            }
            Sld("FADE IN", fadeIn, 0f, 5f, onFadeIn); Sld("FADE OUT", fadeOut, 0f, 5f, onFadeOut)
            Spacer(Modifier.height(4.dp))
            Button(onClick = onExtract, shape = RoundedCornerShape(4.dp), modifier = Modifier.height(30.dp), colors = ButtonDefaults.buttonColors(containerColor = Md, contentColor = Cy), contentPadding = PaddingValues(horizontal = 10.dp)) {
                Icon(Icons.Filled.MusicNote, null, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("EXTRACT AUDIO (.m4a)", fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    @Composable fun PanelCrop(rot: Float, fH: Boolean, fV: Boolean, onRotate: (Float)->Unit, onFlipH: ()->Unit, onFlipV: ()->Unit, onReset: ()->Unit, onCropRatio: (Float, Float)->Unit) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("TRANSFORM & CROP", color = Wh, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp); TextButton(onClick = onReset, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(22.dp)) { Text("RESET", color = Rd, fontSize = 10.sp, fontWeight = FontWeight.Bold) } }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TransformBtn("↻ 90°") { onRotate(90f) }; TransformBtn("↺ -90°") { onRotate(-90f) }
                TransformBtn(if (fH) "FLIP H ✓" else "FLIP H", fH) { onFlipH() }; TransformBtn(if (fV) "FLIP V ✓" else "FLIP V", fV) { onFlipV() }
            }
            Spacer(Modifier.height(6.dp)); Sld("ANGLE", rot / 360f, -0.5f, 0.5f) { onRotate(it * 360f - rot) }
            Spacer(Modifier.height(4.dp)); Text("CROP ASPECT RATIO", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace); Spacer(Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                data class Ratio(val label: String, val w: Float, val h: Float)
                val ratios = listOf(Ratio("1:1", 1f, 1f), Ratio("4:3", 4f, 3f), Ratio("3:4", 3f, 4f), Ratio("16:9", 16f, 9f), Ratio("9:16", 9f, 16f), Ratio("2:3", 2f, 3f))
                items(ratios) { r -> OutlinedButton(onClick = { onCropRatio(r.w, r.h) }, shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, Ac), colors = ButtonDefaults.outlinedButtonColors(contentColor = Ac), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp), modifier = Modifier.height(28.dp)) { Text(r.label, fontSize = 9.sp, fontWeight = FontWeight.Bold) } }
            }
        }
    }

    @Composable fun PanelAi(onFeature: (String)->Unit) {
        Column { Text("AI SUPERPOWERS", color = Wh, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp); Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) { data class Ai(val n: String, val i: ImageVector, val d: String)
                val f = listOf(Ai("MAGIC ERASER", Icons.Filled.AutoFixHigh, "Hapus objek"), Ai("BG REMOVER", Icons.Filled.Wallpaper, "Hapus background"), Ai("VIDEO MATTING", Icons.Filled.ContentCut, "Green-screen"),
                    Ai("VOICE ISOLATE", Icons.Filled.GraphicEq, "Buang noise"), Ai("AI SLOW-MO", Icons.Filled.SlowMotionVideo, "1000fps"), Ai("CHROMA KEY", Icons.Filled.FlipCameraAndroid, "Color key"))
                items(f) { ai -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(68.dp).clickable { onFeature(ai.n) }) {
                    Box(Modifier.size(40.dp).background(Md, RoundedCornerShape(8.dp)).border(1.dp, Lt, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Icon(ai.i, null, tint = Ac, modifier = Modifier.size(18.dp)) }
                    Spacer(Modifier.height(3.dp)); Text(ai.n, color = Wh, fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center, maxLines = 1)
                    Text(ai.d, color = Color.Gray, fontSize = 6.sp, textAlign = TextAlign.Center, maxLines = 1)
                } } }
            Spacer(Modifier.height(6.dp)); Text("PRO AI — COMING SOON", color = Yw, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }
    }

    @Composable fun Sld(label: String, value: Float, min: Float, max: Float, onChange: (Float)->Unit) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(30.dp)) {
            Text(label, color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(80.dp))
            Slider(value = value, onValueChange = onChange, valueRange = min..max, modifier = Modifier.weight(1f).height(24.dp), colors = SliderDefaults.colors(thumbColor = Ac, activeTrackColor = Ac, inactiveTrackColor = Lt))
            Text("${(value * 100).toInt()}", color = Wh, fontSize = 8.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
        }
    }

    @Composable fun MiniBtn(text: String, icon: ImageVector, accent: Boolean = false, onClick: ()->Unit) {
        Button(onClick = onClick, shape = RoundedCornerShape(4.dp), modifier = Modifier.height(26.dp), colors = ButtonDefaults.buttonColors(containerColor = if (accent) Ac else Md, contentColor = if (accent) Bk else Wh), contentPadding = PaddingValues(horizontal = 7.dp, vertical = 1.dp)) {
            Icon(icon, null, modifier = Modifier.size(11.dp)); Spacer(Modifier.width(3.dp)); Text(text, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }

    @Composable fun TransformBtn(text: String, active: Boolean = false, onClick: ()->Unit) {
        Button(onClick = onClick, shape = RoundedCornerShape(4.dp), modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (active) Ac.copy(alpha = 0.2f) else Md, contentColor = if (active) Ac else Wh)) { Text(text, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
    }

    @Composable fun ToggleChip(text: String, active: Boolean, onClick: ()->Unit) {
        Button(onClick = onClick, shape = RoundedCornerShape(4.dp), modifier = Modifier.height(22.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (active) Ac else Md, contentColor = if (active) Bk else Wh)) { Text(text, fontSize = 8.sp, fontWeight = FontWeight.Bold) }
    }

    @Composable fun DrawOverlay(actions: List<DrawAction>) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            actions.forEach { a ->
                when (a.tool) {
                    DrawToolType.PEN -> { for (i in 0 until a.points.size - 1) drawLine(a.color, a.points[i], a.points[i+1], strokeWidth = a.width, cap = StrokeCap.Round) }

                    DrawToolType.RECTANGLE -> if (a.points.size >= 2) { val s = a.points.first(); val e = a.points.last(); drawRect(a.color, topLeft = Offset(min(s.x, e.x), min(s.y, e.y)), size = Size(abs(e.x - s.x), abs(e.y - s.y)), style = Stroke(width = a.width)) }
                    DrawToolType.CIRCLE -> if (a.points.size >= 2) { val s = a.points.first(); val e = a.points.last(); val cx = (s.x + e.x) / 2; val cy = (s.y + e.y) / 2; val r = sqrt((e.x - s.x) * (e.x - s.x) + (e.y - s.y) * (e.y - s.y)) / 2; drawCircle(a.color, radius = r, center = Offset(cx, cy), style = Stroke(width = a.width)) }
                    DrawToolType.LINE -> if (a.points.size >= 2) { drawLine(a.color, a.points.first(), a.points.last(), strokeWidth = a.width, cap = StrokeCap.Round) }
                }
            }
        }
    }

    @Composable fun InteractiveDrawCanvas(actions: SnapshotStateList<DrawAction>, redoStack: SnapshotStateList<DrawAction>, brushColor: Color, brushWidth: Float, tool: DrawToolType) {
        var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
        Canvas(modifier = Modifier.fillMaxSize().pointerInput(brushColor, brushWidth, tool) {
            detectDragGestures(
                onDragStart = { currentPoints = listOf(it) },
                onDrag = { change, _ -> currentPoints = currentPoints + change.position; change.consume() },
                onDragEnd = { if (currentPoints.size > 1) { actions.add(DrawAction(currentPoints.toList(), brushColor, brushWidth, tool)); redoStack.clear() }; currentPoints = emptyList() }
            )
        }) {
            when (tool) {
                DrawToolType.PEN -> { val c = brushColor; val w = brushWidth
                    for (i in 0 until currentPoints.size - 1) drawLine(c, currentPoints[i], currentPoints[i+1], strokeWidth = w, cap = StrokeCap.Round) }
                DrawToolType.RECTANGLE -> if (currentPoints.size >= 2) { val s = currentPoints.first(); val e = currentPoints.last(); drawRect(brushColor, Offset(min(s.x, e.x), min(s.y, e.y)), Size(abs(e.x - s.x), abs(e.y - s.y)), style = Stroke(width = brushWidth)) }
                DrawToolType.CIRCLE -> if (currentPoints.size >= 2) { val s = currentPoints.first(); val e = currentPoints.last(); drawCircle(brushColor, radius = sqrt((e.x-s.x)*(e.x-s.x)+(e.y-s.y)*(e.y-s.y))/2, center = Offset((s.x+e.x)/2, (s.y+e.y)/2), style = Stroke(width = brushWidth)) }
                DrawToolType.LINE -> if (currentPoints.size >= 2) { drawLine(brushColor, currentPoints.first(), currentPoints.last(), strokeWidth = brushWidth, cap = StrokeCap.Round) }
            }
        }
    }

    private fun fmtTime(ms: Long): String { val s = ms / 1000; return "%02d:%02d:%02d".format(s / 60, s % 60, (ms % 1000) / 33) }
}
