package com.optimizer.android.domain.repository

import com.optimizer.android.domain.model.BatteryStatus
import com.optimizer.android.domain.model.JunkFile
import com.optimizer.android.domain.model.RamStatus
import com.optimizer.android.domain.model.StorageStatus
import kotlinx.coroutines.flow.Flow

interface SystemRepository {
    fun getBatteryStatus(): BatteryStatus
    fun getStorageStatus(): StorageStatus
    fun getRamStatus(): RamStatus
    
    suspend fun scanJunk(): Flow<String>
    suspend fun getJunkFiles(): List<JunkFile>
    suspend fun deleteJunkFiles(files: List<JunkFile>): Flow<String>
    suspend fun killBackgroundProcesses(): Flow<String>
    suspend fun getVaultContent(): String
    fun triggerGarbageCollection()
    suspend fun deleteAppResiduals(packageName: String): Boolean
}
