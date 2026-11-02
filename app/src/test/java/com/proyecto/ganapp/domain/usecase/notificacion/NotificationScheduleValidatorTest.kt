package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationScheduleValidatorTest {
    private val zone = ZoneId.of("America/Bogota")
    private val day = LocalDate.of(2026, 11, 15)
    private val now = at(day, LocalTime.of(8, 0))

    private fun create() = CreateAnimalNotificationScheduleRequest(
        templateId = 1L,
        animalId = 2L,
        fechaInicio = day,
        fechaFin = day,
        hora = LocalTime.of(9, 0),
        recurrencia = RecurrenciaNotificacion.NO,
    )

    private fun update() = UpdateNotificationScheduleRequest(
        scheduleId = 3L,
        fechaInicio = day,
        fechaFin = day,
        hora = LocalTime.of(9, 0),
        recurrencia = RecurrenciaNotificacion.NO,
    )

    private fun validate(request: CreateAnimalNotificationScheduleRequest) =
        NotificationScheduleValidator.validate(request, now, zone)

    private fun validate(request: UpdateNotificationScheduleRequest) =
        NotificationScheduleValidator.validate(request, now, zone)

    private fun invalid(error: ScheduleValidationError) = ScheduleValidationResult.Invalid(error)

    @Test
    fun createTemplateIdCeroONegativo_esInvalido() {
        listOf(0L, -1L).forEach {
            assertEquals(invalid(ScheduleValidationError.INVALID_TEMPLATE_ID), validate(create().copy(templateId = it)))
        }
    }

    @Test
    fun createAnimalIdCeroONegativo_esInvalido() {
        listOf(0L, -1L).forEach {
            assertEquals(invalid(ScheduleValidationError.INVALID_ANIMAL_ID), validate(create().copy(animalId = it)))
        }
    }

    @Test
    fun updateScheduleIdCeroONegativo_esInvalido() {
        listOf(0L, -1L).forEach {
            assertEquals(invalid(ScheduleValidationError.INVALID_SCHEDULE_ID), validate(update().copy(scheduleId = it)))
        }
    }

    @Test
    fun precisionDeMinuto_esValidaEnCreateYUpdate() {
        listOf("09:00", "09:37", "23:59").forEach {
            val time = LocalTime.parse(it)
            val expected = ScheduleValidationResult.Valid(at(day, time))
            assertEquals(expected, validate(create().copy(hora = time)))
            assertEquals(expected, validate(update().copy(hora = time)))
        }
    }

    @Test
    fun segundosONanos_sonInvalidosSinTruncarEnCreateYUpdate() {
        listOf("09:00:30", "09:37:01", "09:00:00.500000000").forEach {
            val time = LocalTime.parse(it)
            val expected = invalid(ScheduleValidationError.INVALID_TIME_PRECISION)
            assertEquals(expected, validate(create().copy(hora = time)))
            assertEquals(expected, validate(update().copy(hora = time)))
        }
    }

    @Test
    fun finAnteriorAlInicio_esInvalidoEnCreateYUpdate() {
        val expected = invalid(ScheduleValidationError.END_BEFORE_START)
        assertEquals(expected, validate(create().copy(fechaFin = day.minusDays(1))))
        assertEquals(expected, validate(update().copy(fechaFin = day.minusDays(1))))
    }

    @Test
    fun noConFechasDistintas_esInvalidoEnCreateYUpdate() {
        val expected = invalid(ScheduleValidationError.NO_REQUIRES_SAME_END_DATE)
        assertEquals(expected, validate(create().copy(fechaFin = day.plusDays(1))))
        assertEquals(expected, validate(update().copy(fechaFin = day.plusDays(1))))
    }

    @Test
    fun noConFechasIgualesYHoraFutura_esValidoEnCreateYUpdate() {
        val expected = ScheduleValidationResult.Valid(at(day, LocalTime.of(9, 0)))
        assertEquals(expected, validate(create()))
        assertEquals(expected, validate(update()))
    }

    @Test
    fun todasLasRecurrenciasExistentes_atraviesanElContrato() {
        RecurrenciaNotificacion.entries.forEach {
            val expected = ScheduleValidationResult.Valid(at(day, LocalTime.of(9, 0)))
            assertEquals(expected, validate(create().copy(recurrencia = it)))
            assertEquals(expected, validate(update().copy(recurrencia = it)))
        }
    }

    @Test
    fun updateInicioPasadoConProximaOcurrencia_esValido() {
        val candidate = update().copy(
            fechaInicio = day.withDayOfMonth(1),
            fechaFin = day.withDayOfMonth(30),
            recurrencia = RecurrenciaNotificacion.DIARIA,
        )
        assertEquals(ScheduleValidationResult.Valid(at(day, LocalTime.of(9, 0))), validate(candidate))
    }

    @Test
    fun createInicioPasadoConProximaOcurrencia_esValido() {
        val candidate = create().copy(
            fechaInicio = day.minusDays(2),
            fechaFin = day.plusDays(2),
            recurrencia = RecurrenciaNotificacion.DIARIA,
        )
        assertEquals(ScheduleValidationResult.Valid(at(day, LocalTime.of(9, 0))), validate(candidate))
    }

    @Test
    fun configuracionAgotada_esInvalidaEnCreateYUpdate() {
        val expected = invalid(ScheduleValidationError.NO_FUTURE_OCCURRENCE)
        assertEquals(expected, validate(create().copy(
            fechaInicio = day.minusDays(3), fechaFin = day.minusDays(1),
            recurrencia = RecurrenciaNotificacion.DIARIA,
        )))
        assertEquals(expected, validate(update().copy(
            fechaInicio = day.minusDays(3), fechaFin = day.minusDays(1),
            recurrencia = RecurrenciaNotificacion.DIARIA,
        )))
    }

    @Test
    fun updateAgotado_puedeReactivarseConNuevaConfiguracionFutura() {
        val exhausted = update().copy(fechaInicio = day.minusDays(1), fechaFin = day.minusDays(1))
        assertEquals(invalid(ScheduleValidationError.NO_FUTURE_OCCURRENCE), validate(exhausted))
        val reconfigured = exhausted.copy(fechaInicio = day.plusDays(1), fechaFin = day.plusDays(1))
        assertEquals(
            ScheduleValidationResult.Valid(at(day.plusDays(1), reconfigured.hora)),
            validate(reconfigured),
        )
    }

    @Test
    fun noPasadoOIgualANow_esInvalidoEnCreateYUpdate() {
        listOf(LocalTime.of(7, 59), LocalTime.of(8, 0)).forEach { time ->
            val expected = invalid(ScheduleValidationError.NO_FUTURE_OCCURRENCE)
            assertEquals(expected, validate(create().copy(hora = time)))
            assertEquals(expected, validate(update().copy(hora = time)))
        }
    }

    @Test
    fun prioridadCreate_idsLuegoPrecisionLuegoRangoLuegoNo() {
        val candidate = create().copy(
            templateId = 0L, animalId = 0L,
            hora = LocalTime.of(9, 0, 30), fechaFin = day.minusDays(1),
        )
        assertEquals(invalid(ScheduleValidationError.INVALID_TEMPLATE_ID), validate(candidate))
        val validTemplate = candidate.copy(templateId = 1L)
        assertEquals(invalid(ScheduleValidationError.INVALID_ANIMAL_ID), validate(validTemplate))
        val validIds = validTemplate.copy(animalId = 2L)
        assertEquals(invalid(ScheduleValidationError.INVALID_TIME_PRECISION), validate(validIds))
        val validPrecision = validIds.copy(hora = LocalTime.of(9, 0))
        assertEquals(invalid(ScheduleValidationError.END_BEFORE_START), validate(validPrecision))
        assertEquals(invalid(ScheduleValidationError.NO_REQUIRES_SAME_END_DATE),
            validate(validPrecision.copy(fechaFin = day.plusDays(1))))
    }

    @Test
    fun prioridadUpdate_idLuegoPrecisionLuegoRangoLuegoNo() {
        val candidate = update().copy(
            scheduleId = 0L, hora = LocalTime.of(9, 0, 30), fechaFin = day.minusDays(1),
        )
        assertEquals(invalid(ScheduleValidationError.INVALID_SCHEDULE_ID), validate(candidate))
        val validId = candidate.copy(scheduleId = 3L)
        assertEquals(invalid(ScheduleValidationError.INVALID_TIME_PRECISION), validate(validId))
        val validPrecision = validId.copy(hora = LocalTime.of(9, 0))
        assertEquals(invalid(ScheduleValidationError.END_BEFORE_START), validate(validPrecision))
        assertEquals(invalid(ScheduleValidationError.NO_REQUIRES_SAME_END_DATE),
            validate(validPrecision.copy(fechaFin = day.plusDays(1))))
    }

    @Test
    fun zonaExplicita_determinaElInstanteSinDependerDeLaZonaDelSistema() {
        val utc = ZoneId.of("UTC")
        val beforeBoth = at(day.minusDays(1), LocalTime.NOON)
        val bogotaResult = NotificationScheduleValidator.validate(create(), beforeBoth, zone)
        val utcResult = NotificationScheduleValidator.validate(create(), beforeBoth, utc)
        assertEquals(ScheduleValidationResult.Valid(at(day, create().hora)), bogotaResult)
        assertEquals(ScheduleValidationResult.Valid(
            day.atTime(create().hora).atZone(utc).toInstant().toEpochMilli(),
        ), utcResult)
        assertTrue(bogotaResult != utcResult)
    }

    private fun at(date: LocalDate, time: LocalTime): Long =
        date.atTime(time).atZone(zone).toInstant().toEpochMilli()
}
