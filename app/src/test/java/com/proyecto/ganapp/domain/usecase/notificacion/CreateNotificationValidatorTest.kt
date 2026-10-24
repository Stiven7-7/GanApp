package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import com.proyecto.ganapp.domain.model.TipoNotificacion
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateNotificationValidatorTest {

    private val zone = ZoneId.systemDefault()
    private val day = LocalDate.of(2026, 6, 15)
    private val beforeDay = localMillis(day.minusDays(1), LocalTime.NOON)

    @Test
    fun nombreVacio_esInvalido() {
        val result = validate(request(nombre = ""))

        assertEquals(setOf(CreateNotificationValidationError.BLANK_NAME), result)
    }

    @Test
    fun nombreSoloEspacios_esInvalido() {
        val result = validate(request(nombre = "   "))

        assertEquals(setOf(CreateNotificationValidationError.BLANK_NAME), result)
    }

    @Test
    fun nombreConEspaciosExternos_esValido() {
        val result = validate(request(nombre = " Vacuna "))

        assertTrue(result.isEmpty())
    }

    @Test
    fun idUsuarioCero_esInvalido() {
        val result = validate(request(idUsuario = 0L))

        assertEquals(setOf(CreateNotificationValidationError.INVALID_USER_ID), result)
    }

    @Test
    fun idUsuarioNegativo_esInvalido() {
        val result = validate(request(idUsuario = -1L))

        assertEquals(setOf(CreateNotificationValidationError.INVALID_USER_ID), result)
    }

    @Test
    fun idUsuarioPositivo_esValido() {
        val result = validate(request(idUsuario = 1L))

        assertTrue(result.isEmpty())
    }

    @Test
    fun fechaInicio_noPositiva_esInvalida() {
        listOf(0L, -1L).forEach { fechaInicio ->
            val result = validate(
                request(
                    fechaInicio = fechaInicio,
                    fechaFin = startOfDay(),
                    recurrencia = RecurrenciaNotificacion.DIARIA,
                ),
            )

            assertEquals(setOf(CreateNotificationValidationError.INVALID_START_DATE), result)
        }
    }

    @Test
    fun fechaFin_noPositiva_esInvalida() {
        listOf(0L, -1L).forEach { fechaFin ->
            val result = validate(
                request(
                    fechaInicio = startOfDay(),
                    fechaFin = fechaFin,
                    recurrencia = RecurrenciaNotificacion.DIARIA,
                ),
            )

            assertEquals(setOf(CreateNotificationValidationError.INVALID_END_DATE), result)
        }
    }

    @Test
    fun fechaFin_anteriorAFechaInicio_esInvalida() {
        val result = validate(
            request(
                fechaInicio = startOfDay(day),
                fechaFin = startOfDay(day.minusDays(1)),
                recurrencia = RecurrenciaNotificacion.DIARIA,
            ),
        )

        assertEquals(setOf(CreateNotificationValidationError.END_BEFORE_START), result)
    }

    @Test
    fun fechaFin_igualAFechaInicio_esValidaCuandoLaRecurrenciaLoPermite() {
        val diaria = validate(
            request(
                fechaInicio = startOfDay(),
                fechaFin = startOfDay(),
                recurrencia = RecurrenciaNotificacion.DIARIA,
            ),
        )
        val noRecurrente = validate(
            request(
                fechaInicio = startOfDay(),
                fechaFin = startOfDay(),
                recurrencia = RecurrenciaNotificacion.NO,
            ),
        )

        assertTrue(diaria.isEmpty())
        assertTrue(noRecurrente.isEmpty())
    }

    @Test
    fun recurrenciaNo_conFechasDistintas_esInvalida() {
        val result = validate(
            request(
                fechaInicio = startOfDay(day),
                fechaFin = startOfDay(day.plusDays(1)),
                recurrencia = RecurrenciaNotificacion.NO,
            ),
        )

        assertEquals(
            setOf(CreateNotificationValidationError.NON_RECURRING_END_MUST_EQUAL_START),
            result,
        )
    }

    @Test
    fun recurrenciaNo_conFechaEstructuralmenteInvalida_noEvaluaLaRelacion() {
        val finInvalido = validate(
            request(
                fechaInicio = startOfDay(),
                fechaFin = 0L,
                recurrencia = RecurrenciaNotificacion.NO,
            ),
        )
        val inicioInvalido = validate(
            request(
                fechaInicio = 0L,
                fechaFin = startOfDay(),
                recurrencia = RecurrenciaNotificacion.NO,
            ),
        )

        assertEquals(setOf(CreateNotificationValidationError.INVALID_END_DATE), finInvalido)
        assertEquals(setOf(CreateNotificationValidationError.INVALID_START_DATE), inicioInvalido)
    }

    @Test
    fun recurrenciaNo_conFechasIguales_esValida() {
        val result = validate(
            request(
                fechaInicio = startOfDay(),
                fechaFin = startOfDay(),
                recurrencia = RecurrenciaNotificacion.NO,
            ),
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun horasValidas_sonAceptadas() {
        listOf("00:00", "08:30", "23:59").forEach { hora ->
            val result = validate(request(hora = hora))

            assertTrue(result.isEmpty())
        }
    }

    @Test
    fun horasInvalidas_producenErrorSinFallback() {
        val nowAntesDeLasOcho = localMillis(day, LocalTime.of(7, 0))

        listOf("24:00", "08:60", "8:30", "abc", "").forEach { hora ->
            val candidate = request(hora = hora)
            val result = CreateNotificationValidator.validate(candidate, nowAntesDeLasOcho)

            assertEquals(setOf(CreateNotificationValidationError.INVALID_TIME), result)
            assertNull(CreateNotificationValidator.firstOccurrence(candidate))
        }
    }

    @Test
    fun primeraOcurrencia_conFechaInicioNoPositiva_retornaNull() {
        listOf(0L, -1L).forEach { fechaInicio ->
            val candidate = request(
                fechaInicio = fechaInicio,
                hora = "08:30",
            )

            assertNull(CreateNotificationValidator.firstOccurrence(candidate))
        }
    }

    @Test
    fun primeraOcurrencia_enElPasado_esInvalida() {
        val now = localMillis(day, LocalTime.of(18, 0))
        val result = CreateNotificationValidator.validate(
            request(hora = "17:59"),
            now,
        )

        assertEquals(setOf(CreateNotificationValidationError.FIRST_OCCURRENCE_NOT_FUTURE), result)
    }

    @Test
    fun primeraOcurrencia_igualANow_esInvalida() {
        val now = localMillis(day, LocalTime.of(18, 0))
        val result = CreateNotificationValidator.validate(
            request(hora = "18:00"),
            now,
        )

        assertEquals(
            localMillis(day, LocalTime.of(18, 0)),
            CreateNotificationValidator.firstOccurrence(request(hora = "18:00")),
        )
        assertEquals(setOf(CreateNotificationValidationError.FIRST_OCCURRENCE_NOT_FUTURE), result)
    }

    @Test
    fun primeraOcurrencia_posteriorANow_esValida() {
        val now = localMillis(day, LocalTime.of(18, 0))
        val candidate = request(hora = "18:01")

        assertEquals(
            localMillis(day, LocalTime.of(18, 1)),
            CreateNotificationValidator.firstOccurrence(candidate),
        )
        assertTrue(CreateNotificationValidator.validate(candidate, now).isEmpty())
    }

    private fun validate(
        request: CreateNotificationRequest,
        now: Long = beforeDay,
    ): Set<CreateNotificationValidationError> {
        return CreateNotificationValidator.validate(request, now)
    }

    private fun request(
        nombre: String = "Vacuna",
        tipo: TipoNotificacion = TipoNotificacion.VACUNACION,
        fechaInicio: Long = startOfDay(),
        fechaFin: Long = startOfDay(),
        hora: String = "08:30",
        recurrencia: RecurrenciaNotificacion = RecurrenciaNotificacion.NO,
        idUsuario: Long = 1L,
    ): CreateNotificationRequest {
        return CreateNotificationRequest(
            nombre = nombre,
            tipo = tipo,
            fechaInicio = fechaInicio,
            fechaFin = fechaFin,
            hora = hora,
            recurrencia = recurrencia,
            idUsuario = idUsuario,
        )
    }

    private fun startOfDay(date: LocalDate = day): Long {
        return date.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    private fun localMillis(date: LocalDate, time: LocalTime): Long {
        return date.atTime(time).atZone(zone).toInstant().toEpochMilli()
    }
}
