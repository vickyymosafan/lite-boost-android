package com.optimizer.android.presentation

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.optimizer.android.DrawAction
import com.optimizer.android.DrawToolType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@HiltViewModel
class StudioViewModel @Inject constructor() : ViewModel() {

    /**
     * Renders and exports edited photo with ALL adjustments, filters, drawings, and text burned into it.
     * Also saves/indexes the exported image to the Public Device Gallery via MediaStore & MediaScanner.
     */
    fun exportPhoto(
        context: Context,
        sourceBitmap: Bitmap,
        rotation: Float,
        flipH: Boolean,
        flipV: Boolean,
        colorMatrix: ColorMatrix?,
        vignette: Float,
        grain: Float,
        drawActions: List<DrawAction>,
        previewW: Float,
        previewH: Float,
        textOverlay: String,
        textColorInt: Int,
        textSizePx: Float,
        textBold: Boolean,
        textHasBg: Boolean,
        textBgColorInt: Int,
        textHasStroke: Boolean,
        textStrokeColorInt: Int,
        textStrokeWidthPx: Float,
        textHasShadow: Boolean,
        onProgress: (String) -> Unit,
        onComplete: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                onProgress("RENDERING PHOTO EDITS...")
                val savedPath = withContext(Dispatchers.IO) {
                    // 1. Transform matrix for Rotation & Flip
                    val transformMatrix = Matrix().apply {
                        postRotate(rotation)
                        postScale(if (flipH) -1f else 1f, if (flipV) -1f else 1f, sourceBitmap.width / 2f, sourceBitmap.height / 2f)
                    }

                    // 2. Create transformed base bitmap
                    val transformedBmp = Bitmap.createBitmap(
                        sourceBitmap, 0, 0, sourceBitmap.width, sourceBitmap.height, transformMatrix, true
                    )

                    // 3. Create output canvas bitmap matching transformed dimensions
                    val outW = transformedBmp.width
                    val outH = transformedBmp.height
                    val resultBmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(resultBmp)

                    // 4. Draw base photo with Color Matrix (Color grading / preset filter)
                    val imagePaint = Paint().apply {
                        isAntiAlias = true
                        isFilterBitmap = true
                        if (colorMatrix != null) {
                            colorFilter = ColorMatrixColorFilter(colorMatrix.values)
                        }
                    }
                    canvas.drawBitmap(transformedBmp, 0f, 0f, imagePaint)

                    // 5. Render Vignette if active
                    if (vignette > 0f) {
                        val vigPaint = Paint().apply {
                            isAntiAlias = true
                            shader = android.graphics.RadialGradient(
                                outW / 2f, outH / 2f,
                                max(outW, outH) * 0.7f,
                                intArrayOf(android.graphics.Color.TRANSPARENT, android.graphics.Color.argb((vignette * 200).toInt().coerceIn(0, 255), 0, 0, 0)),
                                floatArrayOf(0.4f, 1.0f),
                                android.graphics.Shader.TileMode.CLAMP
                            )
                        }
                        canvas.drawRect(0f, 0f, outW.toFloat(), outH.toFloat(), vigPaint)
                    }

                    // 6. Render Drawings mapped to output bitmap dimensions
                    val scaleX = if (previewW > 0f) outW / previewW else 1f
                    val scaleY = if (previewH > 0f) outH / previewH else 1f
                    val strokeScale = (scaleX + scaleY) / 2f

                    val drawPaint = Paint().apply {
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                        isAntiAlias = true
                    }

                    for (action in drawActions) {
                        drawPaint.color = android.graphics.Color.argb(
                            255,
                            (action.color.red * 255).toInt(),
                            (action.color.green * 255).toInt(),
                            (action.color.blue * 255).toInt()
                        )
                        drawPaint.strokeWidth = action.width * strokeScale

                        when (action.tool) {
                            DrawToolType.PEN -> {
                                if (action.points.size > 1) {
                                    for (i in 0 until action.points.size - 1) {
                                        canvas.drawLine(
                                            action.points[i].x * scaleX, action.points[i].y * scaleY,
                                            action.points[i + 1].x * scaleX, action.points[i + 1].y * scaleY,
                                            drawPaint
                                        )
                                    }
                                }
                            }
                            DrawToolType.RECTANGLE -> {
                                if (action.points.size >= 2) {
                                    val s = action.points.first()
                                    val e = action.points.last()
                                    canvas.drawRect(
                                        min(s.x, e.x) * scaleX, min(s.y, e.y) * scaleY,
                                        max(s.x, e.x) * scaleX, max(s.y, e.y) * scaleY,
                                        drawPaint
                                    )
                                }
                            }
                            DrawToolType.CIRCLE -> {
                                if (action.points.size >= 2) {
                                    val s = action.points.first()
                                    val e = action.points.last()
                                    val cx = (s.x + e.x) / 2f * scaleX
                                    val cy = (s.y + e.y) / 2f * scaleY
                                    val r = sqrt((e.x - s.x) * (e.x - s.x) + (e.y - s.y) * (e.y - s.y)) / 2f * strokeScale
                                    canvas.drawCircle(cx, cy, r, drawPaint)
                                }
                            }
                            DrawToolType.LINE -> {
                                if (action.points.size >= 2) {
                                    canvas.drawLine(
                                        action.points.first().x * scaleX, action.points.first().y * scaleY,
                                        action.points.last().x * scaleX, action.points.last().y * scaleY,
                                        drawPaint
                                    )
                                }
                            }
                        }
                    }

                    // 7. Render Text Overlay centered / mapped to output bitmap
                    if (textOverlay.isNotEmpty()) {
                        val textPaint = Paint().apply {
                            color = textColorInt
                            textSize = textSizePx * strokeScale * 1.5f
                            isAntiAlias = true
                            isFakeBoldText = textBold
                            textAlign = Paint.Align.CENTER
                        }

                        val centerX = outW / 2f
                        val centerY = outH / 2f

                        // Background Box
                        if (textHasBg) {
                            val bgPaint = Paint().apply {
                                color = textBgColorInt
                                style = Paint.Style.FILL
                                isAntiAlias = true
                                alpha = 180
                            }
                            val bounds = Rect()
                            textPaint.getTextBounds(textOverlay, 0, textOverlay.length, bounds)
                            val padX = 40f * strokeScale
                            val padY = 20f * strokeScale
                            val rect = RectF(
                                centerX - bounds.width() / 2f - padX,
                                centerY - bounds.height() - padY,
                                centerX + bounds.width() / 2f + padX,
                                centerY + padY
                            )
                            canvas.drawRoundRect(rect, 16f, 16f, bgPaint)
                        }

                        // Stroke
                        if (textHasStroke) {
                            val strokePaint = Paint(textPaint).apply {
                                style = Paint.Style.STROKE
                                color = textStrokeColorInt
                                strokeWidth = textStrokeWidthPx * strokeScale * 2f
                            }
                            canvas.drawText(textOverlay, centerX, centerY, strokePaint)
                        }

                        // Shadow
                        if (textHasShadow) {
                            val shadowPaint = Paint(textPaint).apply {
                                color = android.graphics.Color.BLACK
                                alpha = 160
                            }
                            canvas.drawText(textOverlay, centerX + (4f * strokeScale), centerY + (4f * strokeScale), shadowPaint)
                        }

                        // Main Text
                        canvas.drawText(textOverlay, centerX, centerY, textPaint)
                    }

                    // 8. Save image file
                    val filename = "OMNIX_Photo_${System.currentTimeMillis()}.png"
                    val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val omnixFolder = File(picturesDir, "OMNIX")
                    if (!omnixFolder.exists()) omnixFolder.mkdirs()

                    val file = File(omnixFolder, filename)
                    FileOutputStream(file).use { out ->
                        resultBmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }

                    // Recycle temp bitmaps
                    if (transformedBmp != sourceBitmap) transformedBmp.recycle()
                    resultBmp.recycle()

                    // 9. Register with MediaStore / MediaScanner for Public Gallery view
                    scanAndSaveToGallery(context, file, "image/png")
                    file.absolutePath
                }

                onComplete(savedPath)
            } catch (e: Exception) {
                onError(e.message ?: "Photo Export Failed")
            }
        }
    }

    fun extractAudio(
        context: Context,
        sourceUri: Uri,
        onComplete: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val extractor = MediaExtractor()
                    extractor.setDataSource(context, sourceUri, null)
                    var audioIdx = -1
                    var audioFmt: MediaFormat? = null
                    for (i in 0 until extractor.trackCount) {
                        val fmt = extractor.getTrackFormat(i)
                        if (fmt.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                            audioIdx = i
                            audioFmt = fmt
                            break
                        }
                    }
                    if (audioIdx < 0 || audioFmt == null) {
                        throw Exception("No audio track found")
                    }
                    extractor.selectTrack(audioIdx)

                    val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                    val omnixFolder = File(musicDir, "OMNIX")
                    if (!omnixFolder.exists()) omnixFolder.mkdirs()

                    val file = File(omnixFolder, "OMNIX_Audio_${System.currentTimeMillis()}.m4a")
                    val muxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                    val muxTrack = muxer.addTrack(audioFmt)
                    muxer.start()
                    val buf = ByteBuffer.allocate(1024 * 512)
                    val info = MediaCodec.BufferInfo()
                    while (true) {
                        val sz = extractor.readSampleData(buf, 0)
                        if (sz < 0) break
                        info.offset = 0
                        info.size = sz
                        info.presentationTimeUs = extractor.sampleTime
                        info.flags = extractor.sampleFlags
                        muxer.writeSampleData(muxTrack, buf, info)
                        extractor.advance()
                    }
                    muxer.stop()
                    muxer.release()
                    extractor.release()

                    // Index to MediaStore
                    scanAndSaveToGallery(context, file, "audio/m4a")
                    file.absolutePath
                }
                onComplete(result)
            } catch (e: Exception) {
                onError(e.message ?: "Extraction Failed")
            }
        }
    }

    fun captureFrame(
        context: Context,
        uri: Uri,
        positionMs: Long,
        onComplete: (Bitmap) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val frame = withContext(Dispatchers.IO) {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, uri)
                    val bmp = retriever.getFrameAtTime(positionMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
                    retriever.release()
                    bmp
                }
                frame?.let { onComplete(it) }
            } catch (e: Exception) {
                // Ignore frame capture failure
            }
        }
    }

    /**
     * Registers and indexes exported media file with MediaStore & MediaScannerConnection
     * so that it instantly appears in the public Android Gallery / Photos / Music apps.
     */
    fun scanAndSaveToGallery(context: Context, file: File, mimeType: String) {
        try {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf(mimeType)
            ) { path, uri -> }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val relativeSubDir = when {
                    mimeType.startsWith("image/") -> "Pictures/OMNIX"
                    mimeType.startsWith("video/") -> "Movies/OMNIX"
                    mimeType.startsWith("audio/") -> "Music/OMNIX"
                    else -> Environment.DIRECTORY_DOWNLOADS
                }
                
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativeSubDir)
                }

                val contentUri = when {
                    mimeType.startsWith("image/") -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    mimeType.startsWith("video/") -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    mimeType.startsWith("audio/") -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    else -> MediaStore.Files.getContentUri("external")
                }

                context.contentResolver.insert(contentUri, contentValues)
            }
        } catch (e: Exception) {
            // Ignore background scan exception gracefully
        }
    }
}
