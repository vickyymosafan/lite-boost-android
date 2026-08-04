package com.optimizer.android.domain.repository

import java.io.File

interface MediaRepository {
    fun createPhotoFile(): File
}
