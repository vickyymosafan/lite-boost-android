package com.optimizer.android

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class BlackholeNotificationService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let {
            // Kita bisa menambahkan filter package name di sini.
            // Untuk demonstrasi, kita memblokir semua notifikasi (Blackhole).
            try {
                cancelNotification(it.key)
                Log.d("BlackholeService", "Notifikasi dari ${it.packageName} ditelan!")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
