package com.optimizer.android.data.repository

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import android.util.Log
import com.optimizer.android.domain.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val context: Context
) : NotificationRepository {

    override suspend fun saveNotification(sbn: StatusBarNotification?) = withContext(Dispatchers.IO) {
        sbn?.let {
            val extras = it.notification.extras
            val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val pkg = it.packageName

            if (title.isNotEmpty() && text.isNotEmpty()) {
                val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                val logEntry = "[$time] $pkg | $title: $text\n"
                
                try {
                    val file = File(context.filesDir, "vault.txt")
                    FileOutputStream(file, true).use { fos ->
                        fos.write(logEntry.toByteArray())
                    }
                    Log.d("VaultService", "Saved to vault: $logEntry")
                } catch (e: Exception) {
                    Log.e("VaultService", "Failed to save to vault", e)
                }
            }
        }
    }
}
