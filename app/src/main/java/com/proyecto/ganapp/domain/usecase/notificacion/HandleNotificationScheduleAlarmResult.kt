package com.proyecto.ganapp.domain.usecase.notificacion

sealed interface HandleNotificationScheduleAlarmResult {
    data class Display(
        val title: String,
        val message: String,
    ) : HandleNotificationScheduleAlarmResult

    data object Ignored : HandleNotificationScheduleAlarmResult
}
