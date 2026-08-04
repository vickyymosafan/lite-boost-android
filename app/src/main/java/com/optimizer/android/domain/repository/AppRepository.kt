package com.optimizer.android.domain.repository

import com.optimizer.android.domain.model.AppItem

interface AppRepository {
    suspend fun getInstalledApps(currentPackageName: String): List<AppItem>
}
