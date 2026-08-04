package com.optimizer.android.domain.model

data class CapturedNotification(
    val packageName: String,
    val title: String,
    val text: String,
    val timestamp: Long
)
