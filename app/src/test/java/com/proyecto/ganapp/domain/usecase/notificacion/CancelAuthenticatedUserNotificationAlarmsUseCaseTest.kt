package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.Notificacion
import com.proyecto.ganapp.domain.model.NotificationAlarmRequest
import com.proyecto.ganapp.domain.model.NotificationCancelResult
import com.proyecto.ganapp.domain.model.NotificationScheduleResult
import com.proyecto.ganapp.domain.repository.NotificacionRepository
import com.proyecto.ganapp.domain.repository.SessionRepository
import com.proyecto.ganapp.domain.service.NotificationAlarmScheduler
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class CancelAuthenticatedUserNotificationAlarmsUseCaseTest {

    private lateinit var session: LogoutAlarmSessionRepository
    private lateinit var repository: LogoutAlarmNotificacionRepository
    private lateinit var scheduler: LogoutAlarmNotificationScheduler
    private lateinit var useCase: CancelAuthenticatedUserNotificationAlarmsUseCase

    @Before
    fun setUp() {
        session = LogoutAlarmSessionRepository()
        repository = LogoutAlarmNotificacionRepository()
        scheduler = LogoutAlarmNotificationScheduler()
        useCase = CancelAuthenticatedUserNotificationAlarmsUseCase(
            sessionRepository = session,
            notificationRepository = repository,
            scheduleNotificationUseCase = ScheduleNotificationUseCase(scheduler),
        )
    }

    @Test
    fun usuarioA_consultaSoloEseUsuario() = runTest {
        session.userId = 7L
        repository.notifications = listOf(notification(10L))

        useCase()

        assertEquals(listOf(7L), repository.requestedUserIds)
    }

    @Test
    fun usuarioA_noConsultaNiCancelaIdentidadesDeOtroUsuario() = runTest {
        session.userId = 7L
        repository.notifications = listOf(notification(10L), notification(20L))

        useCase()

        assertTrue(repository.requestedUserIds.none { it == 8L })
        assertEquals(listOf(10L, 20L), scheduler.cancelledIds)
    }

    @Test
    fun idsValidos_cancelaCadaUnoUnaVez() = runTest {
        session.userId = 7L
        repository.notifications = listOf(
            notification(10L),
            notification(20L),
            notification(30L),
        )

        val result = useCase()

        assertEquals(listOf(10L, 20L, 30L), scheduler.cancelledIds)
        assertEquals(cancelled(total = 3, cancelled = 3), result)
    }

    @Test
    fun idInvalido_noLlamaSchedulerYContinua() = runTest {
        session.userId = 7L
        repository.notifications = listOf(
            notification(10L),
            notification(0L),
            notification(30L),
        )

        val result = useCase()

        assertEquals(listOf(10L, 30L), scheduler.cancelledIds)
        assertEquals(
            cancelled(total = 3, cancelled = 2, skippedInvalid = 1),
            result,
        )
    }

    @Test
    fun falloDeCancelacion_continuaConElResto() = runTest {
        session.userId = 7L
        scheduler.results = mutableListOf(
            NotificationCancelResult.Cancelled(10L),
            NotificationCancelResult.Failure(20L, IllegalStateException("alarm")),
            NotificationCancelResult.Cancelled(30L),
        )
        repository.notifications = listOf(
            notification(10L),
            notification(20L),
            notification(30L),
        )

        val result = useCase()

        assertEquals(listOf(10L, 20L, 30L), scheduler.cancelledIds)
        assertEquals(cancelled(total = 3, cancelled = 2, failed = 1), result)
    }

    @Test
    fun invalidNotificationIdDelScheduler_cuentaComoFallo() = runTest {
        session.userId = 7L
        scheduler.result = NotificationCancelResult.InvalidNotificationId
        repository.notifications = listOf(notification(10L))

        val result = useCase()

        assertEquals(cancelled(total = 1, failed = 1), result)
    }

    @Test
    fun falloDelRepositorio_noCancela() = runTest {
        val cause = IllegalStateException("notifications")
        session.userId = 7L
        repository.error = cause

        val result = useCase()

        assertEquals(
            CancelAuthenticatedUserNotificationAlarmsResult.Failure(cause),
            result,
        )
        assertEquals(0, scheduler.cancelCalls)
    }

    @Test
    fun falloDeSesion_noConsultaNiCancela() = runTest {
        val cause = IllegalStateException("session")
        session.error = cause

        val result = useCase()

        assertEquals(
            CancelAuthenticatedUserNotificationAlarmsResult.Failure(cause),
            result,
        )
        assertEquals(0, repository.calls)
        assertEquals(0, scheduler.cancelCalls)
    }

    @Test
    fun sinUsuario_noConsultaNiCancela() = runTest {
        session.userId = null

        val result = useCase()

        assertEquals(
            CancelAuthenticatedUserNotificationAlarmsResult.NoAuthenticatedUser,
            result,
        )
        assertEquals(0, repository.calls)
        assertEquals(0, scheduler.cancelCalls)
    }

    @Test
    fun usuarioNoPositivo_noConsultaNiCancela() = runTest {
        session.userId = 0L

        val result = useCase()

        assertEquals(
            CancelAuthenticatedUserNotificationAlarmsResult.NoAuthenticatedUser,
            result,
        )
        assertEquals(0, repository.calls)
        assertEquals(0, scheduler.cancelCalls)
    }

    @Test
    fun cancellationExceptionDeSesion_sePropaga() = runTest {
        val cancellation = CancellationException("session")
        session.error = cancellation

        try {
            useCase()
            fail("La cancelación debió propagarse")
        } catch (exception: CancellationException) {
            assertSame(cancellation, exception)
        }
        assertEquals(0, repository.calls)
        assertEquals(0, scheduler.cancelCalls)
    }

    @Test
    fun cancellationExceptionDeNotificaciones_sePropaga() = runTest {
        val cancellation = CancellationException("notifications")
        session.userId = 7L
        repository.error = cancellation

        try {
            useCase()
            fail("La cancelación debió propagarse")
        } catch (exception: CancellationException) {
            assertSame(cancellation, exception)
        }
        assertEquals(0, scheduler.cancelCalls)
    }

    @Test
    fun cancellationExceptionDelScheduler_sePropagaYNoContinua() = runTest {
        val cancellation = CancellationException("cancel")
        session.userId = 7L
        scheduler.error = cancellation
        repository.notifications = listOf(notification(10L), notification(20L))

        try {
            useCase()
            fail("La cancelación debió propagarse")
        } catch (exception: CancellationException) {
            assertSame(cancellation, exception)
        }
        assertEquals(listOf(10L), scheduler.cancelledIds)
    }

    @Test
    fun batchMixto_respetaLaInvarianteDeContadores() = runTest {
        session.userId = 7L
        scheduler.results = mutableListOf(
            NotificationCancelResult.Cancelled(10L),
            NotificationCancelResult.Failure(30L, IllegalStateException("alarm")),
        )
        repository.notifications = listOf(
            notification(10L),
            notification(0L),
            notification(30L),
        )

        val result = useCase() as CancelAuthenticatedUserNotificationAlarmsResult.Cancelled

        assertEquals(7L, result.userId)
        assertEquals(3, result.total)
        assertEquals(1, result.cancelled)
        assertEquals(1, result.skippedInvalid)
        assertEquals(1, result.failed)
        assertEquals(result.total, result.cancelled + result.skippedInvalid + result.failed)
    }

    private fun cancelled(
        total: Int,
        cancelled: Int = 0,
        skippedInvalid: Int = 0,
        failed: Int = 0,
    ): CancelAuthenticatedUserNotificationAlarmsResult {
        return CancelAuthenticatedUserNotificationAlarmsResult.Cancelled(
            userId = 7L,
            total = total,
            cancelled = cancelled,
            skippedInvalid = skippedInvalid,
            failed = failed,
        )
    }

    private fun notification(id: Long): Notificacion {
        return Notificacion(
            idNotificacion = id,
            nombre = "Vacuna",
            tipo = "Vacunación",
            fechaInicio = 20_000L,
            fechaFin = 20_000L,
            seRepite = "NO",
            hora = "08:00",
            idUsuario = 7L,
        )
    }
}

private class LogoutAlarmSessionRepository : SessionRepository {
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

private class LogoutAlarmNotificacionRepository : NotificacionRepository {
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

private class LogoutAlarmNotificationScheduler : NotificationAlarmScheduler {
    val cancelledIds = mutableListOf<Long>()
    var cancelCalls: Int = 0
    var result: NotificationCancelResult = NotificationCancelResult.Cancelled(1L)
    var results: MutableList<NotificationCancelResult>? = null
    var error: Throwable? = null

    override fun scheduleNext(request: NotificationAlarmRequest): NotificationScheduleResult {
        error("Unexpected call to scheduleNext")
    }

    override fun cancel(notificationId: Long): NotificationCancelResult {
        cancelCalls += 1
        cancelledIds += notificationId
        error?.let { throw it }
        val queued = results
        if (queued != null && queued.isNotEmpty()) {
            return queued.removeAt(0)
        }
        return result
    }
}
