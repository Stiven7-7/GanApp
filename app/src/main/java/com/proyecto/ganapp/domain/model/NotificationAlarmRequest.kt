package com.proyecto.ganapp.domain.model

data class NotificationAlarmRequest(
    val notificationId: Long,
    val triggerAtMillis: Long,
    val title: String,
    val message: String,
)
