package com.proyecto.ganapp.domain.service

import com.proyecto.ganapp.domain.model.NotificationAlarmRequest
import com.proyecto.ganapp.domain.model.NotificationCancelResult
import com.proyecto.ganapp.domain.model.NotificationScheduleResult

interface NotificationAlarmScheduler {
    fun scheduleNext(request: NotificationAlarmRequest): NotificationScheduleResult

    fun cancel(notificationId: Long): NotificationCancelResult
}
