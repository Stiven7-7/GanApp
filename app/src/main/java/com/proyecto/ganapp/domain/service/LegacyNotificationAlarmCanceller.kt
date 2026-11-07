package com.proyecto.ganapp.domain.service

/** Cancela la alarma legacy por su identidad original. No programa alarmas nuevas. */
fun interface LegacyNotificationAlarmCanceller {
    fun cancel(notificationId: Long): ScheduleAlarmCancelResult
}
