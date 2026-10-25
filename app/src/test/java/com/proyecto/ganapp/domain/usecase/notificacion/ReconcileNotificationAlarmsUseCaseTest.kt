package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.Notificacion
import com.proyecto.ganapp.domain.model.NotificationAlarmRequest
import com.proyecto.ganapp.domain.model.NotificationCancelResult
import com.proyecto.ganapp.domain.model.NotificationScheduleResult
import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import com.proyecto.ganapp.domain.repository.NotificacionRepository
import com.proyecto.ganapp.domain.repository.SessionRepository
import com.proyecto.ganapp.domain.service.NotificationAlarmScheduler
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class ReconcileNotificationAlarmsUseCaseTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private lateinit var session: ReconcileAlarmSessionRepository
    private lateinit var repository: ReconcileAlarmNotificacionRepository
    private lateinit var scheduler: ReconcileAlarmNotificationScheduler
    private lateinit var useCase: ReconcileNotificationAlarmsUseCase

    @Before
    fun setUp() {
        session = ReconcileAlarmSessionRepository()
        repository = ReconcileAlarmNotificacionRepository()
        scheduler = ReconcileAlarmNotificationScheduler()
        useCase = ReconcileNotificationAlarmsUseCase(
            sessionRepository = session,
            notificationRepository = repository,
            scheduleNotificationUseCase = ScheduleNotificationUseCase(scheduler),
        )
    }

    @Test
    fun sinSesion_noConsultaNiPrograma() = runTest {
        session.userId = null

        val result = useCase(now = 1L, zoneId = zone)

        assertEquals(ReconcileNotificationAlarmsResult.NoAuthenticatedUser, result)
        assertEquals(0, repository.notificationsCalls)
        assertEquals(0, scheduler.scheduleCalls)
        assertEquals(0, scheduler.cancelCalls)
    }

    @Test
    fun usuario7_consultaSoloEseUsuario() = runTest {
        session.userId = 7L
        repository.notifications = emptyList()

        useCase(now = 1L, zoneId = zone)

        assertEquals(1, repository.notificationsCalls)
        assertEquals(7L, repository.requestedUserId)
    }

    @Test
    fun listaVacia_reconciledEnCero() = runTest {
        session.userId = 7L
        repository.notifications = emptyList()

        val result = useCase(now = 1L, zoneId = zone)

        assertEquals(
            ReconcileNotificationAlarmsResult.Reconciled(
                userId = 7L,
                total = 0,
                scheduled = 0,
                completed = 0,
                skippedInvalid = 0,
                failed = 0,
            ),
            result,
        )
    }

    @Test
    fun diaria_programaLaProximaOcurrencia() = runTest {
        val now = at("2026-06-10", "07:00")
        session.userId = 7L
        repository.notifications = listOf(
            notification(
                inicio = "2026-06-10",
                fin = "2026-06-30",
                seRepite = RecurrenciaNotificacion.DIARIA.toPersistentString(),
            ),
        )

        val result = useCase(now = now, zoneId = zone)

        val expected = at("2026-06-10", "08:00")
        assertEquals(reconciled(total = 1, scheduled = 1), result)
        assertEquals(1, scheduler.scheduleCalls)
        assertEquals(expected, scheduler.requests.single().triggerAtMillis)
        assertEquals(42L, scheduler.requests.single().notificationId)
    }

    @Test
    fun semanal_programaElSiguienteLunes() = runTest {
        val now = at("2026-06-15", "08:00")
        session.userId = 7L
        repository.notifications = listOf(
            notification(
                inicio = "2026-06-15",
                fin = "2026-06-30",
                seRepite = RecurrenciaNotificacion.SEMANAL.toPersistentString(),
            ),
        )

        useCase(now = now, zoneId = zone)

        assertEquals(at("2026-06-22", "08:00"), scheduler.requests.single().triggerAtMillis)
    }

    @Test
    fun mensual_31EneroCaeEn28FebreroYLuegoRecupera31() = runTest {
        session.userId = 7L
        repository.notifications = listOf(
            notification(
                inicio = "2026-01-31",
                fin = "2026-03-31",
                seRepite = RecurrenciaNotificacion.MENSUAL.toPersistentString(),
            ),
        )

        useCase(now = at("2026-01-31", "08:00"), zoneId = zone)
        assertEquals(at("2026-02-28", "08:00"), scheduler.requests.single().triggerAtMillis)

        scheduler.requests.clear()
        useCase(now = at("2026-02-28", "08:00"), zoneId = zone)
        assertEquals(at("2026-03-31", "08:00"), scheduler.requests.single().triggerAtMillis)
    }

    @Test
    fun anual_29Febrero2024CaeEn28Febrero2025() = runTest {
        session.userId = 7L
        repository.notifications = listOf(
            notification(
                inicio = "2024-02-29",
                fin = "2025-12-31",
                seRepite = RecurrenciaNotificacion.ANUAL.toPersistentString(),
            ),
        )

        useCase(now = at("2024-02-29", "08:00"), zoneId = zone)

        assertEquals(at("2025-02-28", "08:00"), scheduler.requests.single().triggerAtMillis)
    }

    @Test
    fun noAgotada_cancelaYCompleta() = runTest {
        session.userId = 7L
        repository.notifications = listOf(
            notification(
                inicio = "2026-06-10",
                fin = "2026-06-10",
                seRepite = RecurrenciaNotificacion.NO.toPersistentString(),
            ),
        )

        val result = useCase(now = at("2026-06-10", "08:00"), zoneId = zone)

        assertEquals(reconciled(total = 1, completed = 1), result)
        assertEquals(1, scheduler.cancelCalls)
        assertEquals(0, scheduler.scheduleCalls)
    }

    @Test
    fun fechaFinAgotada_cancelaYCompleta() = runTest {
        session.userId = 7L
        repository.notifications = listOf(
            notification(
                inicio = "2026-06-10",
                fin = "2026-06-10",
                seRepite = RecurrenciaNotificacion.DIARIA.toPersistentString(),
            ),
        )

        val result = useCase(now = at("2026-06-10", "08:00"), zoneId = zone)

        assertEquals(reconciled(total = 1, completed = 1), result)
        assertEquals(1, scheduler.cancelCalls)
        assertEquals(42L, scheduler.lastCancelledId)
    }

    @Test
    fun recurrenciaDesconocida_seSaltaYProcesaLaSiguiente() = runTest {
        session.userId = 7L
        repository.notifications = listOf(
            notification(
                id = 41L,
                inicio = "2026-06-10",
                fin = "2026-06-30",
                seRepite = "DESCONOCIDA",
            ),
            notification(
                inicio = "2026-06-10",
                fin = "2026-06-30",
                seRepite = RecurrenciaNotificacion.DIARIA.toPersistentString(),
            ),
        )

        val result = useCase(now = at("2026-06-10", "07:00"), zoneId = zone)

        assertEquals(reconciled(total = 2, scheduled = 1, skippedInvalid = 1), result)
        assertEquals(1, scheduler.scheduleCalls)
        assertEquals(42L, scheduler.requests.single().notificationId)
        assertEquals(0, scheduler.cancelCalls)
    }

    @Test
    fun idInvalido_seSaltaSinScheduleNiCancel() = runTest {
        session.userId = 7L
        repository.notifications = listOf(
            notification(
                id = 0L,
                inicio = "2026-06-10",
                fin = "2026-06-30",
                seRepite = RecurrenciaNotificacion.DIARIA.toPersistentString(),
            ),
            notification(
                inicio = "2026-06-10",
                fin = "2026-06-30",
                seRepite = RecurrenciaNotificacion.DIARIA.toPersistentString(),
            ),
        )

        val result = useCase(now = at("2026-06-10", "07:00"), zoneId = zone)

        assertEquals(reconciled(total = 2, scheduled = 1, skippedInvalid = 1), result)
        assertEquals(1, scheduler.scheduleCalls)
        assertEquals(42L, scheduler.requests.single().notificationId)
        assertEquals(0, scheduler.cancelCalls)
    }

    @Test
    fun scheduleFailure_noAbortaElBatch() = runTest {
        val cause = IllegalStateException("schedule")
        session.userId = 7L
        scheduler.scheduleResults = mutableListOf(
            NotificationScheduleResult.Failure(41L, cause),
            NotificationScheduleResult.Scheduled(42L, 1L),
        )
        repository.notifications = listOf(
            notification(
                id = 41L,
                inicio = "2026-06-10",
                fin = "2026-06-30",
                seRepite = RecurrenciaNotificacion.DIARIA.toPersistentString(),
            ),
            notification(
                inicio = "2026-06-10",
                fin = "2026-06-30",
                seRepite = RecurrenciaNotificacion.DIARIA.toPersistentString(),
            ),
        )

        val result = useCase(now = at("2026-06-10", "07:00"), zoneId = zone)

        assertEquals(reconciled(total = 2, scheduled = 1, failed = 1), result)
        assertEquals(2, scheduler.scheduleCalls)
    }

    @Test
    fun cancelFailure_noAbortaElBatch() = runTest {
        session.userId = 7L
        scheduler.cancelResult = NotificationCancelResult.Failure(41L, IllegalStateException("cancel"))
        repository.notifications = listOf(
            notification(
                id = 41L,
                inicio = "2026-06-10",
                fin = "2026-06-10",
                seRepite = RecurrenciaNotificacion.NO.toPersistentString(),
            ),
            notification(
                inicio = "2026-06-10",
                fin = "2026-06-30",
                seRepite = RecurrenciaNotificacion.DIARIA.toPersistentString(),
            ),
        )

        val result = useCase(now = at("2026-06-10", "08:00"), zoneId = zone)

        assertEquals(reconciled(total = 2, scheduled = 1, failed = 1), result)
        assertEquals(1, scheduler.cancelCalls)
        assertEquals(1, scheduler.scheduleCalls)
    }

    @Test
    fun payloadSeReconstruyeDesdeRoom() = runTest {
        session.userId = 7L
        repository.notifications = listOf(
            notification(
                nombre = " Vacuna ",
                inicio = "2026-06-10",
                fin = "2026-06-30",
                seRepite = RecurrenciaNotificacion.DIARIA.toPersistentString(),
            ),
        )

        useCase(now = at("2026-06-10", "07:00"), zoneId = zone)

        assertEquals("Recordatorio GanApp", scheduler.requests.single().title)
        assertEquals("Recordatorio pendiente: Vacuna", scheduler.requests.single().message)
    }

    @Test
    fun mismoNow_seAplicaATodoElBatch() = runTest {
        val now = at("2026-06-10", "14:00")
        session.userId = 7L
        repository.notifications = listOf(
            notification(
                id = 41L,
                inicio = "2026-06-01",
                fin = "2026-06-30",
                hora = "08:00",
                seRepite = RecurrenciaNotificacion.DIARIA.toPersistentString(),
            ),
            notification(
                id = 42L,
                inicio = "2026-06-01",
                fin = "2026-06-30",
                hora = "18:00",
                seRepite = RecurrenciaNotificacion.DIARIA.toPersistentString(),
            ),
        )

        val result = useCase(now = now, zoneId = zone)

        assertEquals(reconciled(total = 2, scheduled = 2), result)
        assertEquals(at("2026-06-11", "08:00"), scheduler.requests[0].triggerAtMillis)
        assertEquals(at("2026-06-10", "18:00"), scheduler.requests[1].triggerAtMillis)
    }

    @Test
    fun zoneIdExplicito_seUsaEnTodoElBatch() = runTest {
        val newYork = ZoneId.of("America/New_York")
        val now = at("2026-06-10", "14:00", newYork)
        session.userId = 7L
        repository.notifications = listOf(
            notification(
                id = 41L,
                inicio = "2026-06-10",
                fin = "2026-06-30",
                seRepite = RecurrenciaNotificacion.DIARIA.toPersistentString(),
                zoneId = newYork,
            ),
            notification(
                id = 42L,
                inicio = "2026-06-10",
                fin = "2026-06-30",
                hora = "18:00",
                seRepite = RecurrenciaNotificacion.DIARIA.toPersistentString(),
                zoneId = newYork,
            ),
        )

        useCase(now = now, zoneId = newYork)

        assertLocal("2026-06-11", "08:00", scheduler.requests[0].triggerAtMillis, newYork)
        assertLocal("2026-06-10", "18:00", scheduler.requests[1].triggerAtMillis, newYork)
    }

    @Test
    fun noReplay_diariaSaltaAManana() = runTest {
        val now = at("2026-06-10", "14:00")
        session.userId = 7L
        repository.notifications = listOf(
            notification(
                inicio = "2026-06-01",
                fin = "2026-06-30",
                seRepite = RecurrenciaNotificacion.DIARIA.toPersistentString(),
            ),
        )

        useCase(now = now, zoneId = zone)

        assertEquals(1, scheduler.scheduleCalls)
        assertEquals(0, scheduler.cancelCalls)
        assertEquals(at("2026-06-11", "08:00"), scheduler.requests.single().triggerAtMillis)
    }

    @Test
    fun cancellationExceptionDeSesion_sePropaga() = runTest {
        val cancellation = CancellationException("session")
        session.error = cancellation

        try {
            useCase(now = 1L, zoneId = zone)
            fail("La cancelación debió propagarse")
        } catch (exception: CancellationException) {
            assertSame(cancellation, exception)
        }
        assertEquals(0, repository.notificationsCalls)
    }

    @Test
    fun cancellationExceptionDeNotificaciones_sePropaga() = runTest {
        val cancellation = CancellationException("notifications")
        session.userId = 7L
        repository.error = cancellation

        try {
            useCase(now = 1L, zoneId = zone)
            fail("La cancelación debió propagarse")
        } catch (exception: CancellationException) {
            assertSame(cancellation, exception)
        }
    }

    @Test
    fun cancellationExceptionDelScheduler_sePropagaYNoContinua() = runTest {
        val cancellation = CancellationException("schedule")
        session.userId = 7L
        scheduler.scheduleError = cancellation
        repository.notifications = listOf(
            notification(
                id = 41L,
                inicio = "2026-06-10",
                fin = "2026-06-30",
                seRepite = RecurrenciaNotificacion.DIARIA.toPersistentString(),
            ),
            notification(
                inicio = "2026-06-10",
                fin = "2026-06-30",
                seRepite = RecurrenciaNotificacion.DIARIA.toPersistentString(),
            ),
        )

        try {
            useCase(now = at("2026-06-10", "07:00"), zoneId = zone)
            fail("La cancelación debió propagarse")
        } catch (exception: CancellationException) {
            assertSame(cancellation, exception)
        }
        assertEquals(1, scheduler.scheduleCalls)
    }

    @Test
    fun batchMixto_respetaLaInvarianteDeContadores() = runTest {
        session.userId = 7L
        scheduler.scheduleResults = mutableListOf(
            NotificationScheduleResult.Scheduled(41L, 1L),
            NotificationScheduleResult.InvalidTriggerTime,
        )
        repository.notifications = listOf(
            notification(
                id = 41L,
                inicio = "2026-06-10",
                fin = "2026-06-30",
                seRepite = RecurrenciaNotificacion.DIARIA.toPersistentString(),
            ),
            notification(
                id = 42L,
                inicio = "2026-06-10",
                fin = "2026-06-10",
                seRepite = RecurrenciaNotificacion.NO.toPersistentString(),
            ),
            notification(
                id = 43L,
                inicio = "2026-06-10",
                fin = "2026-06-30",
                seRepite = "DESCONOCIDA",
            ),
            notification(
                id = 44L,
                inicio = "2026-06-10",
                fin = "2026-06-30",
                seRepite = RecurrenciaNotificacion.DIARIA.toPersistentString(),
            ),
        )

        val result = useCase(now = at("2026-06-10", "08:00"), zoneId = zone)

        val reconciled = result as ReconcileNotificationAlarmsResult.Reconciled
        assertEquals(7L, reconciled.userId)
        assertEquals(4, reconciled.total)
        assertEquals(1, reconciled.scheduled)
        assertEquals(1, reconciled.completed)
        assertEquals(1, reconciled.skippedInvalid)
        assertEquals(1, reconciled.failed)
        assertEquals(
            reconciled.total,
            reconciled.scheduled + reconciled.completed + reconciled.skippedInvalid + reconciled.failed,
        )
        assertNull(scheduler.requests.find { it.notificationId == 43L })
    }

    private fun reconciled(
        total: Int,
        scheduled: Int = 0,
        completed: Int = 0,
        skippedInvalid: Int = 0,
        failed: Int = 0,
    ): ReconcileNotificationAlarmsResult {
        return ReconcileNotificationAlarmsResult.Reconciled(
            userId = 7L,
            total = total,
            scheduled = scheduled,
            completed = completed,
            skippedInvalid = skippedInvalid,
            failed = failed,
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
            idUsuario = 7L,
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
        actual: Long,
        zoneId: ZoneId,
    ) {
        val zoned = Instant.ofEpochMilli(actual).atZone(zoneId)
        assertEquals(LocalDate.parse(date), zoned.toLocalDate())
        assertEquals(LocalTime.parse(time), zoned.toLocalTime())
    }
}

private class ReconcileAlarmSessionRepository : SessionRepository {
    var userId: Long? = null
    var error: Throwable? = null

    override fun observeAuthenticatedUserId(): Flow<Long?> {
        val failure = error
        if (failure != null) {
            return flow { throw failure }
        }
        return flowOf(userId)
    }

    override suspend fun saveAuthenticatedUserId(userId: Long) = Unit

    override suspend fun clearAuthenticatedUserId() = Unit
}

private class ReconcileAlarmNotificacionRepository : NotificacionRepository {
    var notificationsCalls: Int = 0
    var requestedUserId: Long? = null
    var notifications: List<Notificacion> = emptyList()
    var error: Throwable? = null

    override fun getAllNotifications(userId: Long): Flow<List<Notificacion>> {
        notificationsCalls += 1
        requestedUserId = userId
        val failure = error
        if (failure != null) {
            return flow { throw failure }
        }
        return flowOf(notifications)
    }

    override suspend fun insertNotification(notificacion: Notificacion): Long = 0L

    override suspend fun updateNotification(notificacion: Notificacion) = Unit

    override suspend fun deleteNotification(id: Long) = Unit

    override suspend fun getNotificationById(id: Long): Notificacion? = null
}

private class ReconcileAlarmNotificationScheduler : NotificationAlarmScheduler {
    val requests = mutableListOf<NotificationAlarmRequest>()
    var scheduleCalls: Int = 0
    var cancelCalls: Int = 0
    var lastCancelledId: Long? = null
    var scheduleResult: NotificationScheduleResult = NotificationScheduleResult.Scheduled(1L, 1L)
    var scheduleResults: MutableList<NotificationScheduleResult>? = null
    var cancelResult: NotificationCancelResult = NotificationCancelResult.Cancelled(1L)
    var scheduleError: Throwable? = null

    override fun scheduleNext(request: NotificationAlarmRequest): NotificationScheduleResult {
        scheduleCalls += 1
        requests += request
        scheduleError?.let { throw it }
        val queued = scheduleResults
        if (queued != null && queued.isNotEmpty()) {
            return queued.removeAt(0)
        }
        return scheduleResult
    }

    override fun cancel(notificationId: Long): NotificationCancelResult {
        cancelCalls += 1
        lastCancelledId = notificationId
        return cancelResult
    }
}
