package com.optimizer.android

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.optimizer.android.domain.repository.NotificationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BlackholeNotificationService : NotificationListenerService() {

    @Inject
    lateinit var notificationRepository: NotificationRepository

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        scope.launch {
            notificationRepository.saveNotification(sbn)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
