package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import com.proyecto.ganapp.domain.service.NotificationRecurrenceEngine
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object NotificationScheduleValidator {
    private val minuteFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

    fun validate(
        request: CreateAnimalNotificationScheduleRequest,
        now: Long,
        zoneId: ZoneId,
    ): ScheduleValidationResult {
        if (request.templateId <= 0L) return invalid(ScheduleValidationError.INVALID_TEMPLATE_ID)
        if (request.animalId <= 0L) return invalid(ScheduleValidationError.INVALID_ANIMAL_ID)
        return validateConfiguration(
            request.fechaInicio, request.fechaFin, request.hora, request.recurrencia, now, zoneId,
        )
    }

    fun validate(
        request: UpdateNotificationScheduleRequest,
        now: Long,
        zoneId: ZoneId,
    ): ScheduleValidationResult {
        if (request.scheduleId <= 0L) return invalid(ScheduleValidationError.INVALID_SCHEDULE_ID)
        return validateConfiguration(
            request.fechaInicio, request.fechaFin, request.hora, request.recurrencia, now, zoneId,
        )
    }

    // Prioridad: IDs en cada entrada; precisión, rango, regla NO y finalmente motor.
    private fun validateConfiguration(
        fechaInicio: LocalDate,
        fechaFin: LocalDate,
        hora: LocalTime,
        recurrencia: RecurrenciaNotificacion,
        now: Long,
        zoneId: ZoneId,
    ): ScheduleValidationResult {
        if (hora.second != 0 || hora.nano != 0) {
            return invalid(ScheduleValidationError.INVALID_TIME_PRECISION)
        }
        if (fechaFin.isBefore(fechaInicio)) {
            return invalid(ScheduleValidationError.END_BEFORE_START)
        }
        if (recurrencia == RecurrenciaNotificacion.NO && fechaFin != fechaInicio) {
            return invalid(ScheduleValidationError.NO_REQUIRES_SAME_END_DATE)
        }
        // Adaptación solo en el borde del motor existente, sin truncar ni redondear.
        val next = NotificationRecurrenceEngine.nextOccurrence(
            recurrence = recurrencia,
            fechaInicio = fechaInicio.toEpochDay(),
            fechaFin = fechaFin.toEpochDay(),
            hora = hora.format(minuteFormat),
            afterMillis = now,
            zoneId = zoneId,
        )
        return if (next != null && next > now) {
            ScheduleValidationResult.Valid(next)
        } else {
            invalid(ScheduleValidationError.NO_FUTURE_OCCURRENCE)
        }
    }

    private fun invalid(error: ScheduleValidationError) = ScheduleValidationResult.Invalid(error)
}
