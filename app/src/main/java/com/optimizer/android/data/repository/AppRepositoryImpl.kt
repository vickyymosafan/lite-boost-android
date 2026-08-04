package com.optimizer.android.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.optimizer.android.domain.model.AppItem
import com.optimizer.android.domain.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AppRepositoryImpl @Inject constructor(
    private val context: Context
) : AppRepository {
    override suspend fun getInstalledApps(currentPackageName: String): List<AppItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && it.packageName != currentPackageName }
            .map { AppItem(it.loadLabel(pm).toString(), it.packageName) }
            .sortedBy { it.name.lowercase() }
    }
}
