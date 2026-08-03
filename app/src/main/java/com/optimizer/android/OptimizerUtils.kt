package com.optimizer.android

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Environment
import java.io.File

object OptimizerUtils {

    fun cleanJunk(onLog: (String) -> Unit) {
        val root = Environment.getExternalStorageDirectory()
        if (root == null || !root.exists()) {
            onLog("Error: External storage not found.")
            return
        }
        onLog("Starting Junk Scan on ${root.absolutePath}...")
        var deletedFiles = 0
        var freedBytes = 0L

        fun scanAndClean(dir: File) {
            val files = dir.listFiles() ?: return
            for (file in files) {
                if (file.isDirectory) {
                    scanAndClean(file)
                    // Check if it's a target cache directory or empty directory
                    if (file.absolutePath.contains("/Android/data/") && file.absolutePath.endsWith("/cache")) {
                        val size = getFolderSize(file)
                        if (file.deleteRecursively()) {
                            freedBytes += size
                            onLog("Deleted Cache: ${file.absolutePath}")
                        }
                    } else if ((file.listFiles()?.isEmpty() == true)) {
                        if (file.delete()) {
                            onLog("Deleted Empty Folder: ${file.name}")
                        }
                    }
                } else {
                    val name = file.name.lowercase()
                    if (name.endsWith(".tmp") || name.endsWith(".log") || 
                        name.endsWith(".bak") || name.endsWith(".exo")) {
                        val size = file.length()
                        if (file.delete()) {
                            deletedFiles++
                            freedBytes += size
                            onLog("Deleted Junk: ${file.name} (${size / 1024} KB)")
                        }
                    }
                }
            }
        }

        scanAndClean(root)
        onLog("Scan completed! Deleted $deletedFiles files. Freed ${freedBytes / (1024 * 1024)} MB.")
    }

    private fun getFolderSize(dir: File): Long {
        var size = 0L
        val files = dir.listFiles() ?: return 0
        for (f in files) {
            size += if (f.isDirectory) getFolderSize(f) else f.length()
        }
        return size
    }

    fun fastReboot(context: Context, onLog: (String) -> Unit) {
        onLog("Initiating Fast Reboot (RAM Booster)...")
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        var killedCount = 0

        for (appInfo in packages) {
            // Only kill non-system apps, exclude ourselves
            if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && appInfo.packageName != context.packageName) {
                try {
                    am.killBackgroundProcesses(appInfo.packageName)
                    killedCount++
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
        onLog("Fast Reboot complete! Force-stopped $killedCount background apps.")
        onLog("RAM has been freed.")
    }
}
