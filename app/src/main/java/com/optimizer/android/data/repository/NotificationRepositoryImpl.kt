package com.optimizer.android.data.repository

import android.content.Context
import android.util.Log
import com.optimizer.android.domain.model.CapturedNotification
import com.optimizer.android.domain.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

class NotificationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationRepository {

    companion object {
        private const val VAULT_FILE = "vault.txt"
        private const val TAG = "NotificationRepository"
    }

    override suspend fun saveNotification(notification: CapturedNotification) = withContext(Dispatchers.IO) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(notification.timestamp))
        val logEntry = "[$time] ${notification.packageName} | ${notification.title}: ${notification.text}\n"
        
        try {
            val file = File(context.filesDir, VAULT_FILE)
            file.appendText(logEntry)
            Log.d(TAG, "Saved to vault: $logEntry")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save to vault", e)
        }
    }
}
