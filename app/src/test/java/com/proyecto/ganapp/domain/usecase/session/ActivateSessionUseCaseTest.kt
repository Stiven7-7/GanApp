package com.proyecto.ganapp.domain.usecase.session

import com.proyecto.ganapp.domain.model.Notificacion
import com.proyecto.ganapp.domain.model.NotificationAlarmRequest
import com.proyecto.ganapp.domain.model.NotificationCancelResult
import com.proyecto.ganapp.domain.model.NotificationScheduleResult
import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import com.proyecto.ganapp.domain.repository.NotificacionRepository
import com.proyecto.ganapp.domain.repository.SessionRepository
import com.proyecto.ganapp.domain.service.NotificationAlarmScheduler
import com.proyecto.ganapp.domain.service.NotificationRecurrenceEngine
import com.proyecto.ganapp.domain.usecase.notificacion.ReconcileNotificationAlarmsUseCase
import com.proyecto.ganapp.domain.usecase.notificacion.ScheduleNotificationUseCase
import java.time.LocalDate
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class ActivateSessionUseCaseTest {

    private val bogota: ZoneId = ZoneId.of("America/Bogota")
    private val now: Long = 123_456_789L

    private lateinit var session: ActivateProbeSessionRepository
    private lateinit var notifications: ActivateProbeNotificacionRepository
    private lateinit var scheduler: ActivateProbeNotificationScheduler
    private lateinit var useCase: ActivateSessionUseCase

    @Before
    fun setUp() {
        session = ActivateProbeSessionRepository()
        notifications = ActivateProbeNotificacionRepository()
        scheduler = ActivateProbeNotificationScheduler()
        useCase = ActivateSessionUseCase(
            saveSessionUseCase = SaveSessionUseCase(session),
            reconcileNotificationAlarmsUseCase = ReconcileNotificationAlarmsUseCase(
                sessionRepository = session,
                notificationRepository = notifications,
                scheduleNotificationUseCase = ScheduleNotificationUseCase(scheduler),
            ),
        )
    }

    @Test
    fun userIdInvalido_noReconcilia() = runTest {
        val result = useCase(userId = 0L, now = now, zoneId = bogota)

        assertEquals(ActivateSessionResult.InvalidUserId, result)
        assertEquals(0, session.observeCalls)
        assertEquals(0, notifications.calls)
    }

    @Test
    fun falloGuardandoSesion_noReconcilia() = runTest {
        session.saveError = IllegalStateException("save")

        val result = useCase(userId = 7L, now = now, zoneId = bogota)

        assertEquals(ActivateSessionResult.SessionSaveFailure, result)
        assertEquals(0, session.observeCalls)
        assertEquals(0, notifications.calls)
        assertEquals(0, session.clearCalls)
    }

    @Test
    fun saveExitoso_reconciliaUnaVezConNowYZoneIdRecibidos() = runTest {
        notifications.notifications = listOf(notification(id = 42L))

        val result = useCase(userId = 7L, now = now, zoneId = bogota)

        val expected = NotificationRecurrenceEngine.nextOccurrence(
            recurrence = RecurrenciaNotificacion.NO,
            fechaInicio = notification(id = 42L).fechaInicio,
            fechaFin = notification(id = 42L).fechaFin,
            hora = "08:00",
            afterMillis = now,
            zoneId = bogota,
        )
        val otherZone = NotificationRecurrenceEngine.nextOccurrence(
            recurrence = RecurrenciaNotificacion.NO,
            fechaInicio = notification(id = 42L).fechaInicio,
            fechaFin = notification(id = 42L).fechaFin,
            hora = "08:00",
            afterMillis = now,
            zoneId = ZoneId.of("Europe/Madrid"),
        )
        assertEquals(ActivateSessionResult.Success, result)
        assertEquals(1, session.observeCalls)
        assertEquals(1, notifications.calls)
        assertEquals(listOf(7L), notifications.requestedUserIds)
        assertEquals(expected, scheduler.requests.single().triggerAtMillis)
        assertNotEquals(otherZone, scheduler.requests.single().triggerAtMillis)
    }

    @Test
    fun reconciledSinFallosConSkipped_retornaSuccess() = runTest {
        notifications.notifications = listOf(notification(id = 0L))

        val result = useCase(userId = 7L, now = now, zoneId = bogota)

        assertEquals(ActivateSessionResult.Success, result)
        assertEquals(0, scheduler.scheduleCalls)
        assertEquals(0, session.clearCalls)
    }

    @Test
    fun reconciledConFallos_degradaSinRollback() = runTest {
        notifications.notifications = listOf(notification(id = 42L))
        scheduler.scheduleResult = NotificationScheduleResult.Failure(
            notificationId = 42L,
            cause = IllegalStateException("alarm"),
        )

        val result = useCase(userId = 7L, now = now, zoneId = bogota)

        assertEquals(
            ActivateSessionResult.SuccessWithNotificationReconciliationFailure,
            result,
        )
        assertEquals(1, session.saveCalls)
        assertEquals(0, session.clearCalls)
    }

    @Test
    fun falloGlobalDeReconciliacion_degradaSinRollback() = runTest {
        notifications.error = IllegalStateException("notifications")

        val result = useCase(userId = 7L, now = now, zoneId = bogota)

        assertEquals(
            ActivateSessionResult.SuccessWithNotificationReconciliationFailure,
            result,
        )
        assertEquals(1, session.saveCalls)
        assertEquals(0, scheduler.scheduleCalls)
        assertEquals(0, session.clearCalls)
    }

    @Test
    fun sinUsuarioDespuesDeGuardar_degradaSinRollback() = runTest {
        session.hideAuthenticatedUser = true

        val result = useCase(userId = 7L, now = now, zoneId = bogota)

        assertEquals(
            ActivateSessionResult.SuccessWithNotificationReconciliationFailure,
            result,
        )
        assertEquals(1, session.saveCalls)
        assertEquals(0, notifications.calls)
        assertEquals(0, session.clearCalls)
    }

    @Test
    fun cancellationExceptionDeReconciliacion_sePropaga() = runTest {
        val cancellation = CancellationException("reconcile")
        notifications.error = cancellation

        try {
            useCase(userId = 7L, now = now, zoneId = bogota)
            fail("La cancelación debió propagarse")
        } catch (exception: CancellationException) {
            assertSame(cancellation, exception)
        }
        assertEquals(1, session.saveCalls)
        assertEquals(0, session.clearCalls)
    }

    private fun notification(id: Long): Notificacion {
        val day = LocalDate.of(2026, 6, 15).toEpochDay()
        return Notificacion(
            idNotificacion = id,
            nombre = "Vacuna",
            tipo = "Vacunación",
            fechaInicio = day,
            fechaFin = day,
            seRepite = RecurrenciaNotificacion.NO.toPersistentString(),
            hora = "08:00",
            idUsuario = 7L,
        )
    }
}

private class ActivateProbeSessionRepository : SessionRepository {
    var savedUserId: Long? = null
    var saveCalls: Int = 0
    var clearCalls: Int = 0
    var observeCalls: Int = 0
    var saveError: Throwable? = null
    var hideAuthenticatedUser: Boolean = false

    override fun observeAuthenticatedUserId(): Flow<Long?> {
        observeCalls += 1
        val visibleUserId = if (hideAuthenticatedUser) null else savedUserId
        return flowOf(visibleUserId)
    }

    override suspend fun saveAuthenticatedUserId(userId: Long) {
        saveCalls += 1
        savedUserId = userId
        saveError?.let { throw it }
    }

    override suspend fun clearAuthenticatedUserId() {
        clearCalls += 1
    }
}

private class ActivateProbeNotificacionRepository : NotificacionRepository {
    var calls: Int = 0
    val requestedUserIds = mutableListOf<Long>()
    var notifications: List<Notificacion> = emptyList()
    var error: Throwable? = null

    override fun getAllNotifications(userId: Long): Flow<List<Notificacion>> {
        calls += 1
        requestedUserIds += userId
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

private class ActivateProbeNotificationScheduler : NotificationAlarmScheduler {
    val requests = mutableListOf<NotificationAlarmRequest>()
    var scheduleCalls: Int = 0
    var scheduleResult: NotificationScheduleResult? = null

    override fun scheduleNext(request: NotificationAlarmRequest): NotificationScheduleResult {
        scheduleCalls += 1
        requests += request
        return scheduleResult ?: NotificationScheduleResult.Scheduled(
            notificationId = request.notificationId,
            triggerAtMillis = request.triggerAtMillis,
        )
    }

    override fun cancel(notificationId: Long): NotificationCancelResult {
        return NotificationCancelResult.Cancelled(notificationId)
    }
}
