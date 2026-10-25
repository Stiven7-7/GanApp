package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.Notificacion
import com.proyecto.ganapp.domain.model.NotificationAlarmRequest
import com.proyecto.ganapp.domain.model.NotificationCancelResult
import com.proyecto.ganapp.domain.model.NotificationScheduleResult
import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import com.proyecto.ganapp.domain.repository.NotificacionRepository
import com.proyecto.ganapp.domain.service.NotificationAlarmScheduler
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class HandleNotificationAlarmUseCaseTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private lateinit var repository: AlarmFireNotificacionRepository
    private lateinit var scheduler: AlarmFireNotificationScheduler
    private lateinit var useCase: HandleNotificationAlarmUseCase

    @Before
    fun setUp() {
        repository = AlarmFireNotificacionRepository()
        scheduler = AlarmFireNotificationScheduler()
        useCase = HandleNotificationAlarmUseCase(
            repository = repository,
            scheduleNotificationUseCase = ScheduleNotificationUseCase(scheduler),
        )
    }

    @Test
    fun idCero_noConsultaNiPrograma() = runTest {
        val result = useCase(notificationId = 0L, firedAtMillis = 1L, zoneId = zone)

        assertEquals(HandleNotificationAlarmResult.InvalidNotificationId, result)
        assertEquals(0, repository.repositoryCalls)
        assertEquals(0, scheduler.scheduleCalls)
        assertEquals(0, scheduler.cancelCalls)
    }

    @Test
    fun idNegativo_noConsultaNiPrograma() = runTest {
        val result = useCase(notificationId = -1L, firedAtMillis = 1L, zoneId = zone)

        assertEquals(HandleNotificationAlarmResult.InvalidNotificationId, result)
        assertEquals(0, repository.repositoryCalls)
        assertEquals(0, scheduler.scheduleCalls)
        assertEquals(0, scheduler.cancelCalls)
    }

    @Test
    fun roomNull_cancelaYRetornaNotificationNotFound() = runTest {
        repository.notification = null

        val result = useCase(notificationId = 42L, firedAtMillis = 1L, zoneId = zone)

        assertEquals(HandleNotificationAlarmResult.NotificationNotFound(42L), result)
        assertEquals(1, scheduler.cancelCalls)
        assertEquals(42L, scheduler.lastCancelledId)
        assertEquals(0, scheduler.scheduleCalls)
    }

    @Test
    fun roomNullYCancelFalla_retornaFailureConLaCausa() = runTest {
        val cause = IllegalStateException("cancel")
        repository.notification = null
        scheduler.cancelResult = NotificationCancelResult.Failure(42L, cause)

        val result = useCase(notificationId = 42L, firedAtMillis = 1L, zoneId = zone)

        assertEquals(HandleNotificationAlarmResult.Failure(42L, cause), result)
        assertSame(cause, (result as HandleNotificationAlarmResult.Failure).cause)
        assertEquals(0, scheduler.scheduleCalls)
    }

    @Test
    fun idInconsistente_noProgramaYNoReusaElIdDelObjeto() = runTest {
        repository.notification = notification(
            id = 43L,
            inicio = "2026-06-10",
            fin = "2026-06-20",
            seRepite = RecurrenciaNotificacion.DIARIA.toPersistentString(),
        )

        val result = useCase(
            notificationId = 42L,
            firedAtMillis = at("2026-06-10", "08:00"),
            zoneId = zone,
        )

        assertEquals(HandleNotificationAlarmResult.Failure(42L, null), result)
        assertEquals(0, scheduler.scheduleCalls)
        assertEquals(0, scheduler.cancelCalls)
        assertNull(scheduler.lastRequest)
    }

    @Test
    fun recurrenciaDesconocida_noPrograma() = runTest {
        repository.notification = notification(
            inicio = "2026-06-10",
            fin = "2026-06-20",
            seRepite = "DESCONOCIDA",
        )

        val result = useCase(
            notificationId = 42L,
            firedAtMillis = at("2026-06-10", "08:00"),
            zoneId = zone,
        )

        assertEquals(HandleNotificationAlarmResult.InvalidRecurrence(42L), result)
        assertEquals(0, scheduler.scheduleCalls)
        assertEquals(0, scheduler.cancelCalls)
    }

    @Test
    fun no_despuesDeLaUnicaOcurrencia_cancelaYCompleta() = runTest {
        repository.notification = notification(
            inicio = "2026-06-10",
            fin = "2026-06-10",
            seRepite = RecurrenciaNotificacion.NO.toPersistentString(),
        )

        val result = useCase(
            notificationId = 42L,
            firedAtMillis = at("2026-06-10", "08:00"),
            zoneId = zone,
        )

        assertEquals(HandleNotificationAlarmResult.Completed(42L), result)
        assertEquals(1, scheduler.cancelCalls)
        assertEquals(0, scheduler.scheduleCalls)
    }

    @Test
    fun diaria_reprogramaElDiaSiguienteALaMismaHora() = runTest {
        repository.notification = notification(
            inicio = "2026-06-10",
            fin = "2026-06-30",
            seRepite = RecurrenciaNotificacion.DIARIA.toPersistentString(),
        )
        val firedAt = at("2026-06-10", "08:00")

        val result = useCase(notificationId = 42L, firedAtMillis = firedAt, zoneId = zone)

        val expected = at("2026-06-11", "08:00")
        assertEquals(HandleNotificationAlarmResult.Rescheduled(42L, expected), result)
        assertEquals(1, scheduler.scheduleCalls)
        assertEquals(0, scheduler.cancelCalls)
        assertEquals(42L, scheduler.lastRequest?.notificationId)
        assertEquals(expected, scheduler.lastRequest?.triggerAtMillis)
        assertLocal("2026-06-11", "08:00", scheduler.lastRequest?.triggerAtMillis)
    }

    @Test
    fun diaria_firedAtAntesDeLaHora_usaEseInstanteYNoElReloj() = runTest {
        repository.notification = notification(
            inicio = "2026-06-10",
            fin = "2026-06-30",
            seRepite = RecurrenciaNotificacion.DIARIA.toPersistentString(),
        )

        val result = useCase(
            notificationId = 42L,
            firedAtMillis = at("2026-06-10", "07:00"),
            zoneId = zone,
        )

        assertEquals(
            HandleNotificationAlarmResult.Rescheduled(42L, at("2026-06-10", "08:00")),
            result,
        )
    }

    @Test
    fun semanal_avanzaSieteDiasDeCalendario() = runTest {
        repository.notification = notification(
            inicio = "2026-06-15",
            fin = "2026-06-30",
            seRepite = RecurrenciaNotificacion.SEMANAL.toPersistentString(),
        )

        val result = useCase(
            notificationId = 42L,
            firedAtMillis = at("2026-06-15", "08:00"),
            zoneId = zone,
        )

        val expected = at("2026-06-22", "08:00")
        assertEquals(HandleNotificationAlarmResult.Rescheduled(42L, expected), result)
        assertEquals(expected, scheduler.lastRequest?.triggerAtMillis)
    }

    @Test
    fun cada15Dias_de31EneroA15Febrero() = runTest {
        repository.notification = notification(
            inicio = "2026-01-01",
            fin = "2026-02-28",
            seRepite = RecurrenciaNotificacion.CADA_15_DIAS.toPersistentString(),
        )

        val result = useCase(
            notificationId = 42L,
            firedAtMillis = at("2026-01-31", "08:00"),
            zoneId = zone,
        )

        assertEquals(
            HandleNotificationAlarmResult.Rescheduled(42L, at("2026-02-15", "08:00")),
            result,
        )
    }

    @Test
    fun mensual_31EneroCaeEn28Febrero() = runTest {
        repository.notification = notification(
            inicio = "2026-01-31",
            fin = "2026-03-31",
            seRepite = RecurrenciaNotificacion.MENSUAL.toPersistentString(),
        )

        val result = useCase(
            notificationId = 42L,
            firedAtMillis = at("2026-01-31", "08:00"),
            zoneId = zone,
        )

        assertEquals(
            HandleNotificationAlarmResult.Rescheduled(42L, at("2026-02-28", "08:00")),
            result,
        )
    }

    @Test
    fun mensual_28FebreroRecupera31Marzo() = runTest {
        repository.notification = notification(
            inicio = "2026-01-31",
            fin = "2026-03-31",
            seRepite = RecurrenciaNotificacion.MENSUAL.toPersistentString(),
        )

        val result = useCase(
            notificationId = 42L,
            firedAtMillis = at("2026-02-28", "08:00"),
            zoneId = zone,
        )

        assertEquals(
            HandleNotificationAlarmResult.Rescheduled(42L, at("2026-03-31", "08:00")),
            result,
        )
    }

    @Test
    fun anual_29Febrero2024CaeEn28Febrero2025() = runTest {
        repository.notification = notification(
            inicio = "2024-02-29",
            fin = "2025-12-31",
            seRepite = RecurrenciaNotificacion.ANUAL.toPersistentString(),
        )

        val result = useCase(
            notificationId = 42L,
            firedAtMillis = at("2024-02-29", "08:00"),
            zoneId = zone,
        )

        assertEquals(
            HandleNotificationAlarmResult.Rescheduled(42L, at("2025-02-28", "08:00")),
            result,
        )
    }

    @Test
    fun fechaFinAgotada_cancelaYCompleta() = runTest {
        repository.notification = notification(
            inicio = "2026-06-10",
            fin = "2026-06-10",
            seRepite = RecurrenciaNotificacion.DIARIA.toPersistentString(),
        )

        val result = useCase(
            notificationId = 42L,
            firedAtMillis = at("2026-06-10", "08:00"),
            zoneId = zone,
        )

        assertEquals(HandleNotificationAlarmResult.Completed(42L), result)
        assertEquals(1, scheduler.cancelCalls)
        assertEquals(0, scheduler.scheduleCalls)
    }

    @Test
    fun scheduleFailure_conservaLaCausa() = runTest {
        val cause = IllegalStateException("schedule")
        repository.notification = diaria()
        scheduler.scheduleResult = NotificationScheduleResult.Failure(42L, cause)

        val result = useCase(
            notificationId = 42L,
            firedAtMillis = at("2026-06-10", "08:00"),
            zoneId = zone,
        )

        assertEquals(HandleNotificationAlarmResult.Failure(42L, cause), result)
        assertSame(cause, (result as HandleNotificationAlarmResult.Failure).cause)
    }

    @Test
    fun invalidTriggerTimeInesperado_esFailure() = runTest {
        repository.notification = diaria()
        scheduler.scheduleResult = NotificationScheduleResult.InvalidTriggerTime

        val result = useCase(
            notificationId = 42L,
            firedAtMillis = at("2026-06-10", "08:00"),
            zoneId = zone,
        )

        assertEquals(HandleNotificationAlarmResult.Failure(42L, null), result)
    }

    @Test
    fun invalidNotificationIdInesperado_esFailure() = runTest {
        repository.notification = diaria()
        scheduler.scheduleResult = NotificationScheduleResult.InvalidNotificationId

        val result = useCase(
            notificationId = 42L,
            firedAtMillis = at("2026-06-10", "08:00"),
            zoneId = zone,
        )

        assertEquals(HandleNotificationAlarmResult.Failure(42L, null), result)
    }

    @Test
    fun noNextOccurrenceInesperado_esFailure() = runTest {
        repository.notification = diaria()
        scheduler.scheduleResult = NotificationScheduleResult.NoNextOccurrence(42L)

        val result = useCase(
            notificationId = 42L,
            firedAtMillis = at("2026-06-10", "08:00"),
            zoneId = zone,
        )

        assertEquals(HandleNotificationAlarmResult.Failure(42L, null), result)
    }

    @Test
    fun cancelInvalidoInesperado_esFailure() = runTest {
        repository.notification = notification(
            inicio = "2026-06-10",
            fin = "2026-06-10",
            seRepite = RecurrenciaNotificacion.NO.toPersistentString(),
        )
        scheduler.cancelResult = NotificationCancelResult.InvalidNotificationId

        val result = useCase(
            notificationId = 42L,
            firedAtMillis = at("2026-06-10", "08:00"),
            zoneId = zone,
        )

        assertEquals(HandleNotificationAlarmResult.Failure(42L, null), result)
    }

    @Test
    fun payloadSeReconstruyeDesdeRoomYConservaElId() = runTest {
        repository.notification = notification(
            nombre = " Vacuna ",
            inicio = "2026-06-10",
            fin = "2026-06-30",
            seRepite = RecurrenciaNotificacion.DIARIA.toPersistentString(),
        )

        useCase(
            notificationId = 42L,
            firedAtMillis = at("2026-06-10", "08:00"),
            zoneId = zone,
        )

        assertEquals(42L, scheduler.lastRequest?.notificationId)
        assertEquals("Recordatorio GanApp", scheduler.lastRequest?.title)
        assertEquals("Recordatorio pendiente: Vacuna", scheduler.lastRequest?.message)
    }

    @Test
    fun zoneIdExplicito_mantieneLaHoraLocal() = runTest {
        val newYork = ZoneId.of("America/New_York")
        repository.notification = notification(
            inicio = "2026-06-10",
            fin = "2026-06-30",
            seRepite = RecurrenciaNotificacion.DIARIA.toPersistentString(),
            zoneId = newYork,
        )

        val result = useCase(
            notificationId = 42L,
            firedAtMillis = at("2026-06-10", "08:00", newYork),
            zoneId = newYork,
        )

        val expected = at("2026-06-11", "08:00", newYork)
        assertEquals(HandleNotificationAlarmResult.Rescheduled(42L, expected), result)
        assertLocal("2026-06-11", "08:00", expected, newYork)
    }

    @Test
    fun cancellationExceptionDelRepositorio_sePropaga() = runTest {
        val cancellation = CancellationException("cancelled")
        repository.error = cancellation

        try {
            useCase(notificationId = 42L, firedAtMillis = 1L, zoneId = zone)
            fail("La cancelación debió propagarse")
        } catch (exception: CancellationException) {
            assertSame(cancellation, exception)
        }
    }

    @Test
    fun cancellationExceptionDelScheduler_sePropaga() = runTest {
        val cancellation = CancellationException("cancelled")
        repository.notification = diaria()
        scheduler.scheduleError = cancellation

        try {
            useCase(
                notificationId = 42L,
                firedAtMillis = at("2026-06-10", "08:00"),
                zoneId = zone,
            )
            fail("La cancelación debió propagarse")
        } catch (exception: CancellationException) {
            assertSame(cancellation, exception)
        }
    }

    private fun diaria(): Notificacion {
        return notification(
            inicio = "2026-06-10",
            fin = "2026-06-30",
            seRepite = RecurrenciaNotificacion.DIARIA.toPersistentString(),
        )
    }

    private fun notification(
        id: Long = 42L,
        nombre: String = "Vacuna",
        inicio: String,
        fin: String,
        hora: String = "08:00",
        seRepite: String,
        zoneId: ZoneId = zone,
    ): Notificacion {
        return Notificacion(
            idNotificacion = id,
            nombre = nombre,
            tipo = "Vacunación",
            fechaInicio = day(inicio, zoneId),
            fechaFin = day(fin, zoneId),
            seRepite = seRepite,
            hora = hora,
            idUsuario = 1L,
        )
    }

    private fun at(date: String, time: String, zoneId: ZoneId = zone): Long {
        return LocalDate.parse(date)
            .atTime(LocalTime.parse(time))
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    private fun day(date: String, zoneId: ZoneId = zone): Long {
        return LocalDate.parse(date).atStartOfDay(zoneId).toInstant().toEpochMilli()
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
}

private class AlarmFireNotificacionRepository : NotificacionRepository {
    var repositoryCalls: Int = 0
    var notification: Notificacion? = null
    var error: Throwable? = null

    override fun getAllNotifications(userId: Long): Flow<List<Notificacion>> = emptyFlow()

    override suspend fun insertNotification(notificacion: Notificacion): Long = 0L

    override suspend fun updateNotification(notificacion: Notificacion) = Unit

    override suspend fun deleteNotification(id: Long) = Unit

    override suspend fun getNotificationById(id: Long): Notificacion? {
        repositoryCalls += 1
        error?.let { throw it }
        return notification
    }
}

private class AlarmFireNotificationScheduler : NotificationAlarmScheduler {
    var scheduleCalls: Int = 0
    var cancelCalls: Int = 0
    var lastRequest: NotificationAlarmRequest? = null
    var lastCancelledId: Long? = null
    var scheduleResult: NotificationScheduleResult = NotificationScheduleResult.Scheduled(42L, 1L)
    var cancelResult: NotificationCancelResult = NotificationCancelResult.Cancelled(42L)
    var scheduleError: Throwable? = null

    override fun scheduleNext(request: NotificationAlarmRequest): NotificationScheduleResult {
        scheduleCalls += 1
        lastRequest = request
        scheduleError?.let { throw it }
        return scheduleResult
    }

    override fun cancel(notificationId: Long): NotificationCancelResult {
        cancelCalls += 1
        lastCancelledId = notificationId
        return cancelResult
    }
}
