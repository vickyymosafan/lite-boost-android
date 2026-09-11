package com.optimizer.android.data.filter

import android.content.Context
import com.optimizer.android.domain.model.FilterListId
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilterListStore @Inject constructor(@ApplicationContext private val context: Context) {

    fun readBundled(id: FilterListId): String = try {
        context.assets.open("filters/${id.assetFile}").bufferedReader().use { it.readText() }
    } catch (e: Exception) { "" }

    fun readUpdated(id: FilterListId): String? = try {
        val f = File(context.filesDir, "filters/${id.assetFile}")
        if (f.exists() && f.length() > 10_240) f.readText() else null
    } catch (e: Exception) { null }

    fun writeUpdated(id: FilterListId, content: String): Boolean = try {
        val dir = File(context.filesDir, "filters")
        if (!dir.exists()) dir.mkdirs()
        File(dir, id.assetFile).writeText(content)
        true
    } catch (e: Exception) { false }
}