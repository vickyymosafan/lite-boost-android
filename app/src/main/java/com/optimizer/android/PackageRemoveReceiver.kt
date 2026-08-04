package com.optimizer.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class PackageRemoveReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_PACKAGE_REMOVED) {
            val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
            if (isReplacing) return

            val data = intent.data ?: return
            val packageName = data.schemeSpecificPart

            val workData = Data.Builder()
                .putString("PACKAGE_NAME", packageName)
                .build()

            val purgeRequest = OneTimeWorkRequestBuilder<PurgeResidualsWorker>()
                .setInputData(workData)
                .build()

            WorkManager.getInstance(context).enqueue(purgeRequest)
        }
    }
}
