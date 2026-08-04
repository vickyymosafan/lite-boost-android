package com.optimizer.android.domain.repository

import android.service.notification.StatusBarNotification

interface NotificationRepository {
    suspend fun saveNotification(sbn: StatusBarNotification?)
}
