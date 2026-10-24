package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import java.time.Instant
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
        val startDateValid = request.fechaInicio > 0L
        val endDateValid = request.fechaFin > 0L
        if (!startDateValid) {
            errors += CreateNotificationValidationError.INVALID_START_DATE
        }
        if (!endDateValid) {
            errors += CreateNotificationValidationError.INVALID_END_DATE
        }
        if (startDateValid && endDateValid) {
            if (request.fechaFin < request.fechaInicio) {
                errors += CreateNotificationValidationError.END_BEFORE_START
            }
            if (
                request.recurrencia == RecurrenciaNotificacion.NO &&
                request.fechaFin != request.fechaInicio
            ) {
                errors += CreateNotificationValidationError.NON_RECURRING_END_MUST_EQUAL_START
            }
        }

        val time = parseStrictTime(request.hora)
        if (time == null) {
            errors += CreateNotificationValidationError.INVALID_TIME
        } else if (request.fechaInicio > 0L && occurrenceAt(request.fechaInicio, time) <= now) {
            errors += CreateNotificationValidationError.FIRST_OCCURRENCE_NOT_FUTURE
        }

        return errors
    }

    fun firstOccurrence(request: CreateNotificationRequest): Long? {
        if (request.fechaInicio <= 0L) return null
        val time = parseStrictTime(request.hora) ?: return null
        return occurrenceAt(request.fechaInicio, time)
    }

    private fun occurrenceAt(fechaInicio: Long, time: LocalTime): Long {
        val zone = ZoneId.systemDefault()
        val startDay = Instant.ofEpochMilli(fechaInicio).atZone(zone).toLocalDate()
        return startDay.atTime(time).atZone(zone).toInstant().toEpochMilli()
    }

    private fun parseStrictTime(hora: String): LocalTime? {
        val match = strictTime.matchEntire(hora) ?: return null
        return LocalTime.of(
            match.groupValues[1].toInt(),
            match.groupValues[2].toInt(),
        )
    }
}
