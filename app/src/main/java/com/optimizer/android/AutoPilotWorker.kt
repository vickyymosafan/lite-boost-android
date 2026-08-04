package com.optimizer.android

import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File

class AutoPilotWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Cek izin (Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                // Jika tidak ada izin, batalkan auto-pilot.
                return Result.failure()
            }
        }

        // Lakukan pembersihan diam-diam
        val foundFiles = OptimizerUtils.scanJunk { /* abaikan log */ }
        
        if (foundFiles.isNotEmpty()) {
            OptimizerUtils.deleteJunkFiles(foundFiles) { /* abaikan log */ }
        }

        return Result.success()
    }
}
