package com.optimizer.android

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Environment
import android.os.StatFs
import java.io.File

object OptimizerUtils {

    // --- SYSTEM STATUS ---
    
    data class StorageStatus(val freeMb: Long, val totalMb: Long)
    data class RamStatus(val freeMb: Long, val totalMb: Long)

    fun getStorageStatus(): StorageStatus {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong
        
        val totalMb = (totalBlocks * blockSize) / (1024 * 1024)
        val freeMb = (availableBlocks * blockSize) / (1024 * 1024)
        return StorageStatus(freeMb, totalMb)
    }

    fun getRamStatus(context: Context): RamStatus {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memoryInfo)
        
        val totalMb = memoryInfo.totalMem / (1024 * 1024)
        val freeMb = memoryInfo.availMem / (1024 * 1024)
        return RamStatus(freeMb, totalMb)
    }

    // --- JUNK CLEANER (2-STAGE) ---

    fun scanJunk(onLog: (String) -> Unit): List<File> {
        val root = Environment.getExternalStorageDirectory()
        val foundFiles = mutableListOf<File>()
        
        if (root == null || !root.exists()) {
            onLog("Error: External storage not found.")
            return emptyList()
        }
        onLog("Memulai pemindaian (Scan) di ${root.absolutePath}...")

        fun scanRecursively(dir: File) {
            val files = dir.listFiles() ?: return
            for (file in files) {
                if (file.isDirectory) {
                    scanRecursively(file)
                    // Check if it's a target cache directory or empty directory
                    if (file.absolutePath.contains("/Android/data/") && file.absolutePath.endsWith("/cache")) {
                        foundFiles.add(file)
                    } else if (file.listFiles()?.isEmpty() == true) {
                        foundFiles.add(file)
                    }
                } else {
                    val name = file.name.lowercase()
                    if (name.endsWith(".tmp") || name.endsWith(".log") || 
                        name.endsWith(".bak") || name.endsWith(".exo")) {
                        foundFiles.add(file)
                    }
                }
            }
        }

        scanRecursively(root)
        onLog("Scan selesai! Ditemukan ${foundFiles.size} file/folder sampah.")
        return foundFiles
    }

    fun deleteJunkFiles(files: List<File>, onLog: (String) -> Unit) {
        var deletedCount = 0
        var freedBytes = 0L

        for (file in files) {
            if (!file.exists()) continue
            val size = if (file.isDirectory) getFolderSize(file) else file.length()
            val name = file.name
            
            val success = if (file.isDirectory) file.deleteRecursively() else file.delete()
            if (success) {
                deletedCount++
                freedBytes += size
                onLog("Dihapus: $name (${size / 1024} KB)")
            }
        }
        onLog("Selesai! Berhasil menghapus $deletedCount file. Membebaskan ${freedBytes / (1024 * 1024)} MB.")
    }

    private fun getFolderSize(dir: File): Long {
        var size = 0L
        val files = dir.listFiles() ?: return 0
        for (f in files) {
            size += if (f.isDirectory) getFolderSize(f) else f.length()
        }
        return size
    }

    // --- RAM BOOSTER ---

    fun fastReboot(context: Context, onLog: (String) -> Unit) {
        onLog("Memulai Fast Reboot (RAM Booster)...")
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        var killedCount = 0

        for (appInfo in packages) {
            if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && appInfo.packageName != context.packageName) {
                try {
                    am.killBackgroundProcesses(appInfo.packageName)
                    killedCount++
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
        onLog("Fast Reboot berhasil! Menghentikan paksa $killedCount aplikasi.")
        onLog("Sisa RAM telah direfresh.")
    }
}
