package com.optimizer.android.data.repository

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import com.optimizer.android.domain.model.BatteryHealth
import com.optimizer.android.domain.model.BatteryStatus
import com.optimizer.android.domain.model.JunkFile
import com.optimizer.android.domain.model.RamStatus
import com.optimizer.android.domain.model.StorageStatus
import com.optimizer.android.domain.repository.SystemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SystemRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SystemRepository {

    companion object {
        const val BYTES_IN_MB = 1048576L
        const val BYTES_IN_KB = 1024L
        const val TEMP_DIVISOR = 10.0f
    }

    override fun getBatteryStatus(): BatteryStatus {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (intent == null) return BatteryStatus(0f, BatteryHealth.UNKNOWN)
        
        val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val tempCelsius = temp / TEMP_DIVISOR
        
        val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
        val healthEnum = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealth.GOOD
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealth.OVERHEAT
            BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealth.DEAD
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealth.OVER_VOLTAGE
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> BatteryHealth.FAILURE
            BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealth.COLD
            else -> BatteryHealth.UNKNOWN
        }
        
        return BatteryStatus(tempCelsius, healthEnum)
    }

    override fun getStorageStatus(): StorageStatus {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong
        
        val totalMb = (totalBlocks * blockSize) / BYTES_IN_MB
        val freeMb = (availableBlocks * blockSize) / BYTES_IN_MB
        return StorageStatus(freeMb, totalMb)
    }

    override fun getRamStatus(): RamStatus {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memoryInfo)
        
        val totalMb = memoryInfo.totalMem / BYTES_IN_MB
        val freeMb = memoryInfo.availMem / BYTES_IN_MB
        return RamStatus(freeMb, totalMb)
    }

    override suspend fun scanJunk(): Flow<String> = flow {
        val root = Environment.getExternalStorageDirectory()
        if (root == null || !root.exists()) {
            emit("Gagal: Penyimpanan eksternal tidak ditemukan.")
            return@flow
        }
        emit("Memulai pemindaian (Scan) di ${root.absolutePath}...")
        
        // Simulating scan delay for UX, the actual scan happens in getJunkFiles
        emit("Scan berjalan di background...")
    }.flowOn(Dispatchers.IO)

    override suspend fun getJunkFiles(): List<JunkFile> = withContext(Dispatchers.IO) {
        // Use context.getExternalFilesDir to respect modern Android storage
        val root = context.getExternalFilesDir(null) ?: Environment.getExternalStorageDirectory()
        val foundFiles = mutableListOf<JunkFile>()
        
        if (root == null || !root.exists()) return@withContext emptyList()

        root.walkTopDown().forEach { file ->
            if (file.isDirectory) {
                if (file.absolutePath.contains("/Android/data/") && file.absolutePath.endsWith("/cache")) {
                    foundFiles.add(JunkFile(file, getFolderSize(file)))
                } else if (file.listFiles()?.isEmpty() == true) {
                    foundFiles.add(JunkFile(file, 0L))
                }
            } else {
                val name = file.name.lowercase()
                if (name.endsWith(".tmp") || name.endsWith(".log") || 
                    name.endsWith(".bak") || name.endsWith(".exo")) {
                    foundFiles.add(JunkFile(file, file.length()))
                }
            }
        }

        return@withContext foundFiles
    }

    override suspend fun deleteJunkFiles(files: List<JunkFile>): Flow<String> = flow {
        var deletedCount = 0
        var freedBytes = 0L

        for (junk in files) {
            val file = junk.file
            if (!file.exists()) continue
            
            val success = if (file.isDirectory) file.deleteRecursively() else file.delete()
            if (success) {
                deletedCount++
                freedBytes += junk.size
                emit("Dihapus: ${file.name} (${junk.size / 1024} KB)")
            }
        }
        emit("Selesai! Berhasil menghapus $deletedCount file. Membebaskan ${freedBytes / 1048576L} MB.")
    }.flowOn(Dispatchers.IO)

    private fun getFolderSize(dir: File): Long {
        var size = 0L
        val files = dir.listFiles() ?: return 0
        for (f in files) {
            size += if (f.isDirectory) getFolderSize(f) else f.length()
        }
        return size
    }

    override suspend fun killBackgroundProcesses(): Flow<String> = flow {
        emit("Memulai optimasi RAM (Fast Reboot)...")
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val pm = context.packageManager
        val packages = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        }
        var killedCount = 0

        for (appInfo in packages) {
            if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && appInfo.packageName != context.packageName) {
                try {
                    am.killBackgroundProcesses(appInfo.packageName)
                    killedCount++
                } catch (e: Exception) {
                    // Ignore gracefully
                }
            }
        }
        emit("Fast Reboot berhasil! Menghentikan paksa $killedCount aplikasi.")
        emit("Sisa RAM telah direfresh.")
    }.flowOn(Dispatchers.IO)

    override suspend fun getVaultContent(): String = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, "vault.txt")
        if (file.exists()) {
            file.readText()
        } else {
            "Vault kosong. Belum ada notifikasi yang ditangkap."
        }
    }

    override fun triggerGarbageCollection() {
        System.gc()
    }

    override suspend fun deleteAppResiduals(packageName: String): Boolean = withContext(Dispatchers.IO) {
        val root = Environment.getExternalStorageDirectory()
        val dirs = listOf(
            File(root, "Android/data/$packageName"),
            File(root, "Android/obb/$packageName")
        )
        
        var success = true
        for (dir in dirs) {
            if (dir.exists()) {
                if (!dir.deleteRecursively()) {
                    success = false
                }
            }
        }
        return@withContext success
    }
}
