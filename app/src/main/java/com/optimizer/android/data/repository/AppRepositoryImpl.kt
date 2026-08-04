package com.optimizer.android.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.optimizer.android.domain.model.AppItem
import com.optimizer.android.domain.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AppRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppRepository {
    override suspend fun getInstalledApps(): List<AppItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val packages = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        }
        packages
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && it.packageName != context.packageName }
            .map { AppItem(it.loadLabel(pm).toString(), it.packageName) }
            .sortedBy { it.name.lowercase() }
    }
}
