package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

object CreateNotificationValidator {

    private val strictTime = Regex("^([01]\\d|2[0-3]):([0-5]\\d)$")

    fun validate(
        request: CreateNotificationRequest,
        now: Long,
    ): Set<CreateNotificationValidationError> {
        val errors = linkedSetOf<CreateNotificationValidationError>()

        if (request.nombre.trim().isEmpty()) {
            errors += CreateNotificationValidationError.BLANK_NAME
        }
        if (request.idUsuario <= 0L) {
            errors += CreateNotificationValidationError.INVALID_USER_ID
        }
        val startDate = calendarDate(request.fechaInicio)
        val endDate = calendarDate(request.fechaFin)
        if (startDate == null) {
            errors += CreateNotificationValidationError.INVALID_START_DATE
        }
        if (endDate == null) {
            errors += CreateNotificationValidationError.INVALID_END_DATE
        }
        if (startDate != null && endDate != null) {
            if (endDate.isBefore(startDate)) {
                errors += CreateNotificationValidationError.END_BEFORE_START
            }
            if (
                request.recurrencia == RecurrenciaNotificacion.NO &&
                endDate != startDate
            ) {
                errors += CreateNotificationValidationError.NON_RECURRING_END_MUST_EQUAL_START
            }
        }

        val time = parseStrictTime(request.hora)
        if (time == null) {
            errors += CreateNotificationValidationError.INVALID_TIME
        } else if (startDate != null && occurrenceAt(startDate, time) <= now) {
            errors += CreateNotificationValidationError.FIRST_OCCURRENCE_NOT_FUTURE
        }

        return errors
    }

    fun firstOccurrence(request: CreateNotificationRequest): Long? {
        val startDate = calendarDate(request.fechaInicio) ?: return null
        val time = parseStrictTime(request.hora) ?: return null
        return occurrenceAt(startDate, time)
    }

    private fun occurrenceAt(startDate: LocalDate, time: LocalTime): Long {
        return startDate.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun calendarDate(epochDay: Long): LocalDate? {
        return try {
            LocalDate.ofEpochDay(epochDay)
        } catch (exception: DateTimeException) {
            null
        }
    }

    private fun parseStrictTime(hora: String): LocalTime? {
        val match = strictTime.matchEntire(hora) ?: return null
        return LocalTime.of(
            match.groupValues[1].toInt(),
            match.groupValues[2].toInt(),
        )
    }
}
