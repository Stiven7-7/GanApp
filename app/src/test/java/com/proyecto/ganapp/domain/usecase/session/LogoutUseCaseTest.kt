package com.proyecto.ganapp.domain.usecase.session

import com.proyecto.ganapp.domain.model.Notificacion
import com.proyecto.ganapp.domain.model.NotificationAlarmRequest
import com.proyecto.ganapp.domain.model.NotificationCancelResult
import com.proyecto.ganapp.domain.model.NotificationScheduleResult
import com.proyecto.ganapp.domain.repository.NotificacionRepository
import com.proyecto.ganapp.domain.repository.SessionRepository
import com.proyecto.ganapp.domain.service.NotificationAlarmScheduler
import com.proyecto.ganapp.domain.usecase.notificacion.CancelAuthenticatedUserNotificationAlarmsUseCase
import com.proyecto.ganapp.domain.usecase.notificacion.ScheduleNotificationUseCase
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class LogoutUseCaseTest {

    private lateinit var session: LogoutSessionRepository
    private lateinit var repository: LogoutNotificacionRepository
    private lateinit var scheduler: LogoutNotificationScheduler
    private lateinit var useCase: LogoutUseCase

    @Before
    fun setUp() {
        session = LogoutSessionRepository()
        repository = LogoutNotificacionRepository()
        scheduler = LogoutNotificationScheduler()
        useCase = LogoutUseCase(
            cancelAuthenticatedUserNotificationAlarmsUseCase =
                CancelAuthenticatedUserNotificationAlarmsUseCase(
                    sessionRepository = session,
                    notificationRepository = repository,
                    scheduleNotificationUseCase = ScheduleNotificationUseCase(scheduler),
                ),
            clearSessionUseCase = ClearSessionUseCase(session),
        )
    }

    @Test
    fun batchSinFallosYClearExitoso_retornaSuccess() = runTest {
        session.userId = 7L
        repository.notifications = listOf(notification(10L))

        val result = useCase()

        assertEquals(LogoutResult.Success, result)
        assertEquals(1, session.clearCalls)
    }

    @Test
    fun skippedSinFallos_limpiaSesion() = runTest {
        session.userId = 7L
        repository.notifications = listOf(
            notification(10L),
            notification(20L),
            notification(0L),
        )

        val result = useCase()

        assertEquals(LogoutResult.Success, result)
        assertEquals(1, session.clearCalls)
    }

    @Test
    fun falloRealDeCancelacion_noLimpiaSesion() = runTest {
        session.userId = 7L
        scheduler.result = NotificationCancelResult.Failure(10L, IllegalStateException("alarm"))
        repository.notifications = listOf(notification(10L))

        val result = useCase()

        assertEquals(LogoutResult.AlarmCancellationFailure, result)
        assertEquals(0, session.clearCalls)
    }

    @Test
    fun falloGlobalDelBatch_noLimpiaSesion() = runTest {
        session.userId = 7L
        repository.error = IllegalStateException("notifications")

        val result = useCase()

        assertEquals(LogoutResult.AlarmCancellationFailure, result)
        assertEquals(0, session.clearCalls)
        assertEquals(0, scheduler.cancelCalls)
    }

    @Test
    fun clearInesperado_retornaSessionClearFailure() = runTest {
        session.userId = 7L
        repository.notifications = listOf(notification(10L))
        session.clearError = IllegalStateException("clear")

        val result = useCase()

        assertEquals(LogoutResult.SessionClearFailure, result)
        assertEquals(1, session.clearCalls)
    }

    @Test
    fun sinUsuario_logoutIdempotenteLimpiaSesion() = runTest {
        session.userId = null

        val result = useCase()

        assertEquals(LogoutResult.Success, result)
        assertEquals(1, session.clearCalls)
        assertEquals(0, repository.calls)
        assertEquals(0, scheduler.cancelCalls)
    }

    @Test
    fun cancellationExceptionAlCancelarAlarmas_sePropagaSinLimpiarSesion() = runTest {
        val cancellation = CancellationException("cancel")
        session.userId = 7L
        repository.notifications = listOf(notification(10L))
        scheduler.error = cancellation

        try {
            useCase()
            fail("La cancelación debió propagarse")
        } catch (exception: CancellationException) {
            assertSame(cancellation, exception)
        }
        assertEquals(0, session.clearCalls)
    }

    @Test
    fun cancellationExceptionAlLimpiarSesion_sePropaga() = runTest {
        val cancellation = CancellationException("clear")
        session.userId = null
        session.clearError = cancellation

        try {
            useCase()
            fail("La cancelación debió propagarse")
        } catch (exception: CancellationException) {
            assertSame(cancellation, exception)
        }
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

private class LogoutSessionRepository : SessionRepository {
    var userId: Long? = null
    var error: Throwable? = null
    var clearCalls: Int = 0
    var clearError: Throwable? = null

    override fun observeAuthenticatedUserId(): Flow<Long?> {
        val failure = error
        if (failure != null) {
            return flow { throw failure }
        }
        return flowOf(userId)
    }

    override suspend fun saveAuthenticatedUserId(userId: Long) = Unit

    override suspend fun clearAuthenticatedUserId() {
        clearCalls += 1
        clearError?.let { throw it }
    }
}

private class LogoutNotificacionRepository : NotificacionRepository {
    var calls: Int = 0
    var notifications: List<Notificacion> = emptyList()
    var error: Throwable? = null

    override fun getAllNotifications(userId: Long): Flow<List<Notificacion>> {
        calls += 1
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

private class LogoutNotificationScheduler : NotificationAlarmScheduler {
    var cancelCalls: Int = 0
    var result: NotificationCancelResult = NotificationCancelResult.Cancelled(1L)
    var error: Throwable? = null

    override fun scheduleNext(request: NotificationAlarmRequest): NotificationScheduleResult {
        error("Unexpected call to scheduleNext")
    }

    override fun cancel(notificationId: Long): NotificationCancelResult {
        cancelCalls += 1
        error?.let { throw it }
        return result
    }
}
