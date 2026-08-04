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

@HiltWorker
class PurgeResidualsWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val packageName = inputData.getString("PACKAGE_NAME") ?: return@withContext Result.failure()
        
        val root = Environment.getExternalStorageDirectory()
        val dataDir = File(root, "Android/data")
        val obbDir = File(root, "Android/obb")

        val targets = listOf(
            File(dataDir, packageName),
            File(obbDir, packageName),
            File(root, packageName)
        )

        var deletedCount = 0
        for (target in targets) {
            if (target.exists() && target.isDirectory) {
                if (target.deleteRecursively()) {
                    deletedCount++
                }
            }
        }
        
        return@withContext Result.success()
    }
}
