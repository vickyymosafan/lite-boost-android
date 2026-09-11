package com.optimizer.android.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.optimizer.android.domain.repository.DnsShieldRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class FilterListUpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val dnsShieldRepository: DnsShieldRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return if (dnsShieldRepository.updateLists()) Result.success() else Result.retry()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<FilterListUpdateWorker>(7, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "filter_list_update", ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }
}
