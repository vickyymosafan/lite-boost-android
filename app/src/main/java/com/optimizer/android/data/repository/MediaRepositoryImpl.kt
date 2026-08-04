package com.optimizer.android.data.repository

import android.content.Context
import com.optimizer.android.domain.repository.MediaRepository
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : MediaRepository {

    override fun createPhotoFile(): File {
        val mediaDirs = context.externalMediaDirs
        val dir = mediaDirs.firstOrNull() ?: context.filesDir
        val timeStamp = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())
        return File(dir, "$timeStamp.jpg")
    }
}
