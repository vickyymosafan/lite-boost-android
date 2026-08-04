package com.optimizer.android.presentation

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import javax.inject.Inject

@HiltViewModel
class StudioViewModel @Inject constructor() : ViewModel() {

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
                    val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
                    val file = File(dir, "OMNIX_Audio_${System.currentTimeMillis()}.m4a")
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
}
