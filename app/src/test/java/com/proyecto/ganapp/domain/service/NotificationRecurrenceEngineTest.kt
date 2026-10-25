package com.proyecto.ganapp.domain.service

import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import com.proyecto.ganapp.domain.model.TipoNotificacion
import com.proyecto.ganapp.domain.usecase.notificacion.CreateNotificationRequest
import com.proyecto.ganapp.domain.usecase.notificacion.CreateNotificationValidator
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationRecurrenceEngineTest {

    private val zone: ZoneId = ZoneId.of("UTC")

    @Test
    fun no_primeraOcurrenciaFutura() {
        val first = at("2026-06-15", "08:00")

        val result = next(RecurrenciaNotificacion.NO, "2026-06-15", "2026-06-15", "08:00", first - 1L)

        assertEquals(first, result)
    }

    @Test
    fun no_igualdadConAfter_retornaNull() {
        val first = at("2026-06-15", "08:00")

        assertNull(next(RecurrenciaNotificacion.NO, "2026-06-15", "2026-06-15", "08:00", first))
    }

    @Test
    fun no_ocurrenciaPasada_retornaNull() {
        val first = at("2026-06-15", "08:00")

        assertNull(next(RecurrenciaNotificacion.NO, "2026-06-15", "2026-06-15", "08:00", first + 1L))
    }

    @Test
    fun diaria_mismaFechaConHoraFutura() {
        val result = next(
            RecurrenciaNotificacion.DIARIA,
            "2026-06-10",
            "2026-06-20",
            "08:00",
            at("2026-06-10", "07:00"),
        )

        assertLocal("2026-06-10", "08:00", result)
    }

    @Test
    fun diaria_horaYaPasada_avanzaAlDiaSiguiente() {
        val result = next(
            RecurrenciaNotificacion.DIARIA,
            "2026-06-10",
            "2026-06-20",
            "08:00",
            at("2026-06-10", "09:00"),
        )

        assertLocal("2026-06-11", "08:00", result)
    }

    @Test
    fun diaria_saltaOcurrenciasPerdidas() {
        val result = next(
            RecurrenciaNotificacion.DIARIA,
            "2026-06-01",
            "2026-06-30",
            "08:00",
            at("2026-06-10", "14:00"),
        )

        assertLocal("2026-06-11", "08:00", result)
    }

    @Test
    fun diaria_fechaFinInclusivaYNullDespues() {
        val last = at("2026-06-12", "08:00")

        assertLocal(
            "2026-06-10",
            "08:00",
            next(RecurrenciaNotificacion.DIARIA, "2026-06-10", "2026-06-12", "08:00", at("2026-06-09", "08:00")),
        )
        assertLocal(
            "2026-06-12",
            "08:00",
            next(RecurrenciaNotificacion.DIARIA, "2026-06-10", "2026-06-12", "08:00", at("2026-06-11", "08:00")),
        )
        assertEquals(last, next(RecurrenciaNotificacion.DIARIA, "2026-06-10", "2026-06-12", "08:00", last - 1L))
        assertNull(next(RecurrenciaNotificacion.DIARIA, "2026-06-10", "2026-06-12", "08:00", last))
    }

    @Test
    fun diaria_afterExactoDevuelveLaSiguiente() {
        val current = at("2026-06-10", "08:00")

        val result = next(
            RecurrenciaNotificacion.DIARIA,
            "2026-06-10",
            "2026-06-20",
            "08:00",
            current,
        )

        assertLocal("2026-06-11", "08:00", result)
    }

    @Test
    fun semanal_conservaElLunesYElMultiploDeSiete() {
        val start = LocalDate.parse("2026-06-15")
        assertEquals(DayOfWeek.MONDAY, start.dayOfWeek)

        val nextWeek = next(
            RecurrenciaNotificacion.SEMANAL,
            "2026-06-15",
            "2026-08-31",
            "08:00",
            at("2026-06-15", "08:00"),
        )
        assertLocal("2026-06-22", "08:00", nextWeek)

        val later = next(
            RecurrenciaNotificacion.SEMANAL,
            "2026-06-15",
            "2026-08-31",
            "08:00",
            at("2026-07-06", "09:00"),
        )
        assertLocal("2026-07-13", "08:00", later)
        assertEquals(DayOfWeek.MONDAY, localDate(later).dayOfWeek)
    }

    @Test
    fun cada15Dias_avanzaDesdeElAnclaOriginal() {
        val end = "2026-03-31"
        val first = next(RecurrenciaNotificacion.CADA_15_DIAS, "2026-01-01", end, "08:00", at("2025-12-31", "08:00"))
        val second = next(RecurrenciaNotificacion.CADA_15_DIAS, "2026-01-01", end, "08:00", first!!)
        val third = next(RecurrenciaNotificacion.CADA_15_DIAS, "2026-01-01", end, "08:00", second!!)
        val fourth = next(RecurrenciaNotificacion.CADA_15_DIAS, "2026-01-01", end, "08:00", third!!)

        assertLocal("2026-01-01", "08:00", first)
        assertLocal("2026-01-16", "08:00", second)
        assertLocal("2026-01-31", "08:00", third)
        assertLocal("2026-02-15", "08:00", fourth)
    }

    @Test
    fun mensual_31Enero2026_recuperaElAnclaEnMarzo() {
        val feb = next(RecurrenciaNotificacion.MENSUAL, "2026-01-31", "2026-12-31", "08:00", at("2026-01-31", "08:00"))
        val mar = next(RecurrenciaNotificacion.MENSUAL, "2026-01-31", "2026-12-31", "08:00", feb!!)

        assertLocal("2026-02-28", "08:00", feb)
        assertLocal("2026-03-31", "08:00", mar)
    }

    @Test
    fun mensual_31Enero2028_usa29FebreroYRecupera31() {
        val feb = next(RecurrenciaNotificacion.MENSUAL, "2028-01-31", "2028-12-31", "08:00", at("2028-01-31", "08:00"))
        val mar = next(RecurrenciaNotificacion.MENSUAL, "2028-01-31", "2028-12-31", "08:00", feb!!)

        assertLocal("2028-02-29", "08:00", feb)
        assertLocal("2028-03-31", "08:00", mar)
    }

    @Test
    fun mensual_30Enero_caeEnElUltimoDiaDeFebreroYVuelveA30() {
        val feb = next(RecurrenciaNotificacion.MENSUAL, "2026-01-30", "2026-12-31", "08:00", at("2026-01-30", "08:00"))
        val mar = next(RecurrenciaNotificacion.MENSUAL, "2026-01-30", "2026-12-31", "08:00", feb!!)

        assertLocal("2026-02-28", "08:00", feb)
        assertLocal("2026-03-30", "08:00", mar)
    }

    @Test
    fun mensual_15SeMantiene() {
        val feb = next(RecurrenciaNotificacion.MENSUAL, "2026-01-15", "2026-12-31", "08:00", at("2026-01-15", "08:00"))
        val mar = next(RecurrenciaNotificacion.MENSUAL, "2026-01-15", "2026-12-31", "08:00", feb!!)

        assertLocal("2026-02-15", "08:00", feb)
        assertLocal("2026-03-15", "08:00", mar)
    }

    @Test
    fun mensual_fechaFinInclusiva() {
        val feb = at("2026-02-28", "08:00")

        assertEquals(
            feb,
            next(RecurrenciaNotificacion.MENSUAL, "2026-01-31", "2026-02-28", "08:00", at("2026-01-31", "08:00")),
        )
        assertNull(
            next(RecurrenciaNotificacion.MENSUAL, "2026-01-31", "2026-02-28", "08:00", feb),
        )
    }

    @Test
    fun anual_15JunioAvanzaUnAnio() {
        val result = next(
            RecurrenciaNotificacion.ANUAL,
            "2026-06-15",
            "2030-06-15",
            "08:00",
            at("2026-06-15", "08:00"),
        )

        assertLocal("2027-06-15", "08:00", result)
    }

    @Test
    fun anual_29FebreroSeRecuperaEnElSiguienteBisiesto() {
        val end = "2030-12-31"
        val y2024 = next(RecurrenciaNotificacion.ANUAL, "2024-02-29", end, "08:00", at("2024-02-28", "08:00"))
        val y2025 = next(RecurrenciaNotificacion.ANUAL, "2024-02-29", end, "08:00", y2024!!)
        val y2026 = next(RecurrenciaNotificacion.ANUAL, "2024-02-29", end, "08:00", y2025!!)
        val y2027 = next(RecurrenciaNotificacion.ANUAL, "2024-02-29", end, "08:00", y2026!!)
        val y2028 = next(RecurrenciaNotificacion.ANUAL, "2024-02-29", end, "08:00", y2027!!)

        assertLocal("2024-02-29", "08:00", y2024)
        assertLocal("2025-02-28", "08:00", y2025)
        assertLocal("2026-02-28", "08:00", y2026)
        assertLocal("2027-02-28", "08:00", y2027)
        assertLocal("2028-02-29", "08:00", y2028)
    }

    @Test
    fun anual_fechaFinInclusiva() {
        val last = at("2027-06-15", "08:00")

        assertEquals(
            last,
            next(RecurrenciaNotificacion.ANUAL, "2026-06-15", "2027-06-15", "08:00", at("2026-06-15", "08:00")),
        )
        assertNull(next(RecurrenciaNotificacion.ANUAL, "2026-06-15", "2027-06-15", "08:00", last))
    }

    @Test
    fun diaria_mantiene08LocalAlCruzarDst() {
        val newYork = ZoneId.of("America/New_York")
        val start = day("2026-03-07", newYork)
        val end = day("2026-03-12", newYork)
        val march7 = at("2026-03-07", "08:00", newYork)
        val march8 = at("2026-03-08", "08:00", newYork)

        val onTransition = NotificationRecurrenceEngine.nextOccurrence(
            RecurrenciaNotificacion.DIARIA,
            start,
            end,
            "08:00",
            march7,
            newYork,
        )
        val afterTransition = NotificationRecurrenceEngine.nextOccurrence(
            RecurrenciaNotificacion.DIARIA,
            start,
            end,
            "08:00",
            march8,
            newYork,
        )

        assertLocal("2026-03-08", "08:00", onTransition, newYork)
        assertLocal("2026-03-09", "08:00", afterTransition, newYork)
    }

    @Test
    fun epochDay_mantieneLaFechaLocalEnBogotaLosAngelesYMadrid() {
        val epochDay = LocalDate.of(2026, 1, 31).toEpochDay()
        val bogota = ZoneId.of("America/Bogota")
        val losAngeles = ZoneId.of("America/Los_Angeles")
        val madrid = ZoneId.of("Europe/Madrid")

        val bogotaTrigger = triggerOn(epochDay, bogota)
        val losAngelesTrigger = triggerOn(epochDay, losAngeles)
        val madridTrigger = triggerOn(epochDay, madrid)

        assertLocal("2026-01-31", "08:00", bogotaTrigger, bogota)
        assertLocal("2026-01-31", "08:00", losAngelesTrigger, losAngeles)
        assertLocal("2026-01-31", "08:00", madridTrigger, madrid)
        assertNotEquals(bogotaTrigger, losAngelesTrigger)
        assertNotEquals(bogotaTrigger, madridTrigger)
    }

    @Test
    fun mensual_conservaElAnclaEnBogota() {
        val epochDay = LocalDate.of(2026, 1, 31).toEpochDay()
        val end = LocalDate.of(2026, 3, 31).toEpochDay()
        val bogota = ZoneId.of("America/Bogota")
        val february = NotificationRecurrenceEngine.nextOccurrence(
            RecurrenciaNotificacion.MENSUAL,
            epochDay,
            end,
            "08:00",
            at("2026-01-31", "08:00", bogota),
            bogota,
        )
        val march = NotificationRecurrenceEngine.nextOccurrence(
            RecurrenciaNotificacion.MENSUAL,
            epochDay,
            end,
            "08:00",
            february!!,
            bogota,
        )

        assertLocal("2026-02-28", "08:00", february, bogota)
        assertLocal("2026-03-31", "08:00", march, bogota)
    }

    @Test
    fun fechaInicioIrrepresentable_retornaNull() {
        assertNull(
            NotificationRecurrenceEngine.nextOccurrence(
                RecurrenciaNotificacion.DIARIA,
                Long.MIN_VALUE,
                day("2026-06-15"),
                "08:00",
                1L,
                zone,
            ),
        )
    }

    @Test
    fun fechaFinIrrepresentable_retornaNull() {
        assertNull(
            NotificationRecurrenceEngine.nextOccurrence(
                RecurrenciaNotificacion.DIARIA,
                day("2026-06-15"),
                Long.MIN_VALUE,
                "08:00",
                1L,
                zone,
            ),
        )
    }

    @Test
    fun fechaFinAnteriorAlInicio_retornaNull() {
        assertNull(next(RecurrenciaNotificacion.DIARIA, "2026-06-12", "2026-06-10", "08:00", 1L))
    }

    @Test
    fun horasInvalidas_retornanNull() {
        listOf("8:30", "24:00", "abc").forEach { hora ->
            assertNull(next(RecurrenciaNotificacion.DIARIA, "2026-06-10", "2026-06-12", hora, 1L))
        }
    }

    @Test
    fun primeraOcurrencia_coincideConElValidadorDe10A() {
        val validatorZone = ZoneId.systemDefault()
        val start = day("2026-06-15", validatorZone)
        val request = CreateNotificationRequest(
            nombre = "Vacuna",
            tipo = TipoNotificacion.VACUNACION,
            fechaInicio = start,
            fechaFin = start,
            hora = "08:30",
            recurrencia = RecurrenciaNotificacion.NO,
            idUsuario = 1L,
        )
        val first = CreateNotificationValidator.firstOccurrence(request)

        val result = NotificationRecurrenceEngine.nextOccurrence(
            RecurrenciaNotificacion.NO,
            start,
            start,
            "08:30",
            first!! - 1L,
            validatorZone,
        )

        assertEquals(first, result)
    }

    private fun triggerOn(epochDay: Long, zoneId: ZoneId): Long? {
        val after = LocalDate.of(2026, 1, 30)
            .atTime(12, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        return NotificationRecurrenceEngine.nextOccurrence(
            RecurrenciaNotificacion.NO,
            epochDay,
            epochDay,
            "08:00",
            after,
            zoneId,
        )
    }

    private fun next(
        recurrence: RecurrenciaNotificacion,
        start: String,
        end: String,
        hora: String,
        afterMillis: Long,
        zoneId: ZoneId = zone,
    ): Long? {
        return NotificationRecurrenceEngine.nextOccurrence(
            recurrence = recurrence,
            fechaInicio = day(start, zoneId),
            fechaFin = day(end, zoneId),
            hora = hora,
            afterMillis = afterMillis,
            zoneId = zoneId,
        )
    }

    private fun at(date: String, time: String, zoneId: ZoneId = zone): Long {
        return LocalDate.parse(date)
            .atTime(LocalTime.parse(time))
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    private fun day(date: String, @Suppress("UNUSED_PARAMETER") zoneId: ZoneId = zone): Long {
        return LocalDate.parse(date).toEpochDay()
    }

    private fun assertLocal(
        date: String,
        time: String,
        actual: Long?,
        zoneId: ZoneId = zone,
    ) {
        val zoned = Instant.ofEpochMilli(actual!!).atZone(zoneId)
        assertEquals(LocalDate.parse(date), zoned.toLocalDate())
        assertEquals(LocalTime.parse(time), zoned.toLocalTime())
    }

    private fun localDate(actual: Long?): LocalDate {
        return Instant.ofEpochMilli(actual!!).atZone(zone).toLocalDate()
    }
}
