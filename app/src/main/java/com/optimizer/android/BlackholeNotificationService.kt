package com.optimizer.android

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.optimizer.android.domain.model.CapturedNotification
import com.optimizer.android.domain.repository.NotificationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

fun StatusBarNotification.toCapturedNotification(): CapturedNotification? {
    val extras = this.notification.extras
    val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
    val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
    
    if (title.isEmpty() || text.isEmpty()) return null
    return CapturedNotification(this.packageName, title, text, System.currentTimeMillis())
}

@AndroidEntryPoint
class BlackholeNotificationService : NotificationListenerService() {

    @Inject
    lateinit var notificationRepository: NotificationRepository

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.toCapturedNotification()?.let { model ->
            scope.launch {
                notificationRepository.saveNotification(model)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
