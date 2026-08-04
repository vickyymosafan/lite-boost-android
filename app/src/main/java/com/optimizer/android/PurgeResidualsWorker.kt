package com.optimizer.android

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import com.optimizer.android.domain.repository.SystemRepository

@HiltWorker
class PurgeResidualsWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val systemRepository: SystemRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_PACKAGE_NAME = "PACKAGE_NAME"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val pkg = inputData.getString(KEY_PACKAGE_NAME) ?: return@withContext Result.failure()
        
        val success = systemRepository.deleteAppResiduals(pkg)
        
        if (success) {
            Result.success()
        } else {
            Result.failure()
        }
    }
}
