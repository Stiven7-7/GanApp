package com.proyecto.ganapp.domain.service

/** Puerto abstracto; la identidad es siempre idProgramacion, sin contenido de la plantilla. */
interface NotificationScheduleAlarmService {
    /** Programa o reemplaza la alarma de scheduleId; exige ID e instante positivos. */
    fun schedule(scheduleId: Long, triggerAtMillis: Long): ScheduleAlarmResult

    /** Idempotente: una alarma inexistente también produce Cancelled. */
    fun cancel(scheduleId: Long): ScheduleAlarmCancelResult
}

sealed interface ScheduleAlarmResult {
    data object Scheduled : ScheduleAlarmResult
    data object InvalidScheduleId : ScheduleAlarmResult
    data object InvalidTriggerTime : ScheduleAlarmResult
    data class Failure(val cause: Throwable? = null) : ScheduleAlarmResult
}

sealed interface ScheduleAlarmCancelResult {
    data object Cancelled : ScheduleAlarmCancelResult
    data object InvalidScheduleId : ScheduleAlarmCancelResult
    data class Failure(val cause: Throwable? = null) : ScheduleAlarmCancelResult
}
