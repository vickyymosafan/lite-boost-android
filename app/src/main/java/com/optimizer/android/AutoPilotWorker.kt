package com.optimizer.android

import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.optimizer.android.domain.repository.SystemRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.collect

@HiltWorker
class AutoPilotWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val systemRepository: SystemRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                return Result.failure()
            }
        }

        val foundFiles = systemRepository.getJunkFiles()
        
        if (foundFiles.isNotEmpty()) {
            systemRepository.deleteJunkFiles(foundFiles).collect()
        }

        return Result.success()
    }
}
