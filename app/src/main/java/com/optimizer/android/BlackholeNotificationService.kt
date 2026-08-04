package com.optimizer.android

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BlackholeNotificationService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let {
            val extras = it.notification.extras
            val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val pkg = it.packageName

            if (title.isNotEmpty() && text.isNotEmpty()) {
                val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                val logEntry = "[$time] $pkg | $title: $text\n"
                
                try {
                    val file = File(applicationContext.filesDir, "vault.txt")
                    FileOutputStream(file, true).use { fos ->
                        fos.write(logEntry.toByteArray())
                    }
                    Log.d("VaultService", "Saved to vault: $logEntry")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Jika mode Blackhole aktif, kita bisa batalkan notifikasinya.
            // Untuk Anti-Delete Vault, kita simpan secara pasif saja.
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // Pesan dihapus oleh pengirim (misal WhatsApp 'Delete for everyone'). 
        // Tapi kita sudah menyimpannya di onNotificationPosted!
    }
}
