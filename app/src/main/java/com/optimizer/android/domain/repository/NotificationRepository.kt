package com.optimizer.android.domain.repository

import com.optimizer.android.domain.model.CapturedNotification

interface NotificationRepository {
    suspend fun saveNotification(notification: CapturedNotification)
}
