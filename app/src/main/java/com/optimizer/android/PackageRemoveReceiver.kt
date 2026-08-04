package com.optimizer.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import java.io.File

class PackageRemoveReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_PACKAGE_REMOVED) {
            // Check if it's an update. We only care about complete uninstalls.
            val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
            if (isReplacing) return

            val data = intent.data ?: return
            val packageName = data.schemeSpecificPart
            Log.d("OMNIX_ERASER", "Package removed: $packageName. Commencing Deep Purge.")

            // Execute cleanup on a background thread
            Thread {
                try {
                    purgeResiduals(packageName)
                } catch (e: Exception) {
                    Log.e("OMNIX_ERASER", "Purge failed: ${e.message}")
                }
            }.start()
        }
    }

    private fun purgeResiduals(packageName: String) {
        val root = Environment.getExternalStorageDirectory()
        val dataDir = File(root, "Android/data")
        val obbDir = File(root, "Android/obb")

        val targets = listOf(
            File(dataDir, packageName),
            File(obbDir, packageName),
            File(root, packageName) // Some poorly coded apps leave a root folder
        )

        var deletedCount = 0
        for (target in targets) {
            if (target.exists() && target.isDirectory) {
                if (target.deleteRecursively()) {
                    Log.d("OMNIX_ERASER", "NUKED: ${target.absolutePath}")
                    deletedCount++
                } else {
                    Log.e("OMNIX_ERASER", "FAILED TO NUKE: ${target.absolutePath}")
                }
            }
        }
        
        // As a fallback, search all folders in root for partial package matches if it's a known bad actor
        // Note: Full recursive scan is too slow, we just target the most common ones.
        if (deletedCount > 0) {
            Log.d("OMNIX_ERASER", "Deep Purge Complete. $deletedCount root directories annihilated.")
        } else {
            Log.d("OMNIX_ERASER", "No residual data found for $packageName.")
        }
    }
}
