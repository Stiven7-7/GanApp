package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.NotificationAlarmRequest
import com.proyecto.ganapp.domain.model.NotificationCancelResult
import com.proyecto.ganapp.domain.model.NotificationScheduleResult
import com.proyecto.ganapp.domain.service.NotificationAlarmScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

class ScheduleNotificationUseCaseTest {

    private lateinit var scheduler: FakeNotificationAlarmScheduler
    private lateinit var useCase: ScheduleNotificationUseCase

    @Before
    fun setUp() {
        scheduler = FakeNotificationAlarmScheduler()
        useCase = ScheduleNotificationUseCase(scheduler)
    }

    @Test
    fun idCero_noPrograma() {
        val result = useCase.scheduleNext(
            notificationId = 0L,
            nextOccurrenceAtMillis = 2_000L,
            title = "Vacuna",
            message = "Aplicar vacuna",
            now = 1_000L,
        )

        assertEquals(NotificationScheduleResult.InvalidNotificationId, result)
        assertEquals(0, scheduler.scheduleCalls)
        assertEquals(0, scheduler.cancelCalls)
    }

    @Test
    fun idNegativo_noPrograma() {
        val result = useCase.scheduleNext(
            notificationId = -1L,
            nextOccurrenceAtMillis = 2_000L,
            title = "Vacuna",
            message = "Aplicar vacuna",
            now = 1_000L,
        )

        assertEquals(NotificationScheduleResult.InvalidNotificationId, result)
        assertEquals(0, scheduler.scheduleCalls)
        assertEquals(0, scheduler.cancelCalls)
    }

    @Test
    fun triggerIgualANow_noPrograma() {
        val result = useCase.scheduleNext(
            notificationId = 42L,
            nextOccurrenceAtMillis = 1_000L,
            title = "Vacuna",
            message = "Aplicar vacuna",
            now = 1_000L,
        )

        assertEquals(NotificationScheduleResult.InvalidTriggerTime, result)
        assertEquals(0, scheduler.scheduleCalls)
        assertEquals(0, scheduler.cancelCalls)
    }

    @Test
    fun triggerPasado_noPrograma() {
        val result = useCase.scheduleNext(
            notificationId = 42L,
            nextOccurrenceAtMillis = 999L,
            title = "Vacuna",
            message = "Aplicar vacuna",
            now = 1_000L,
        )

        assertEquals(NotificationScheduleResult.InvalidTriggerTime, result)
        assertEquals(0, scheduler.scheduleCalls)
        assertEquals(0, scheduler.cancelCalls)
    }

    @Test
    fun triggerFuturo_programaUnaVez() {
        val trigger = 2_000L
        scheduler.scheduleResult = NotificationScheduleResult.Scheduled(42L, trigger)

        val result = useCase.scheduleNext(
            notificationId = 42L,
            nextOccurrenceAtMillis = trigger,
            title = "Vacuna",
            message = "Aplicar vacuna",
            now = 1_000L,
        )

        assertEquals(NotificationScheduleResult.Scheduled(42L, trigger), result)
        assertEquals(1, scheduler.scheduleCalls)
        assertEquals(0, scheduler.cancelCalls)
        assertEquals(
            NotificationAlarmRequest(
                notificationId = 42L,
                triggerAtMillis = trigger,
                title = "Vacuna",
                message = "Aplicar vacuna",
            ),
            scheduler.lastRequest,
        )
    }

    @Test
    fun falloDelScheduler_sePropaga() {
        val cause = IllegalStateException("alarm")
        scheduler.scheduleResult = NotificationScheduleResult.Failure(42L, cause)

        val result = useCase.scheduleNext(
            notificationId = 42L,
            nextOccurrenceAtMillis = 2_000L,
            title = "Vacuna",
            message = "Aplicar vacuna",
            now = 1_000L,
        )

        assertEquals(NotificationScheduleResult.Failure(42L, cause), result)
        assertSame(cause, (result as NotificationScheduleResult.Failure).cause)
        assertEquals(1, scheduler.scheduleCalls)
    }

    @Test
    fun sinProximaOcurrencia_cancelaYNoPrograma() {
        scheduler.cancelResult = NotificationCancelResult.Cancelled(42L)

        val result = useCase.scheduleNext(
            notificationId = 42L,
            nextOccurrenceAtMillis = null,
            title = "Vacuna",
            message = "Aplicar vacuna",
            now = 1_000L,
        )

        assertEquals(NotificationScheduleResult.NoNextOccurrence(42L), result)
        assertEquals(0, scheduler.scheduleCalls)
        assertEquals(1, scheduler.cancelCalls)
        assertEquals(42L, scheduler.lastCancelledId)
    }

    @Test
    fun sinProximaOcurrencia_siCancelarFalla_propagaElFallo() {
        val cause = IllegalStateException("cancel")
        scheduler.cancelResult = NotificationCancelResult.Failure(42L, cause)

        val result = useCase.scheduleNext(
            notificationId = 42L,
            nextOccurrenceAtMillis = null,
            title = "Vacuna",
            message = "Aplicar vacuna",
            now = 1_000L,
        )

        assertEquals(NotificationScheduleResult.Failure(42L, cause), result)
        assertSame(cause, (result as NotificationScheduleResult.Failure).cause)
        assertEquals(0, scheduler.scheduleCalls)
        assertEquals(1, scheduler.cancelCalls)
    }

    @Test
    fun cancelConIdCero_noTocaElScheduler() {
        val result = useCase.cancel(0L)

        assertEquals(NotificationCancelResult.InvalidNotificationId, result)
        assertEquals(0, scheduler.cancelCalls)
    }

    @Test
    fun cancelConIdNegativo_noTocaElScheduler() {
        val result = useCase.cancel(-1L)

        assertEquals(NotificationCancelResult.InvalidNotificationId, result)
        assertEquals(0, scheduler.cancelCalls)
    }

    @Test
    fun cancelConIdValido_propagaCancelled() {
        scheduler.cancelResult = NotificationCancelResult.Cancelled(42L)

        val result = useCase.cancel(42L)

        assertEquals(NotificationCancelResult.Cancelled(42L), result)
        assertEquals(1, scheduler.cancelCalls)
        assertEquals(42L, scheduler.lastCancelledId)
    }

    @Test
    fun cancelConIdValido_propagaFailure() {
        val cause = IllegalStateException("cancel")
        scheduler.cancelResult = NotificationCancelResult.Failure(42L, cause)

        val result = useCase.cancel(42L)

        assertEquals(NotificationCancelResult.Failure(42L, cause), result)
        assertSame(cause, (result as NotificationCancelResult.Failure).cause)
        assertEquals(1, scheduler.cancelCalls)
    }
}

private class FakeNotificationAlarmScheduler : NotificationAlarmScheduler {
    var scheduleCalls: Int = 0
    var cancelCalls: Int = 0
    var lastRequest: NotificationAlarmRequest? = null
    var lastCancelledId: Long? = null
    var scheduleResult: NotificationScheduleResult = NotificationScheduleResult.Scheduled(1L, 1L)
    var cancelResult: NotificationCancelResult = NotificationCancelResult.Cancelled(1L)

    override fun scheduleNext(request: NotificationAlarmRequest): NotificationScheduleResult {
        scheduleCalls += 1
        lastRequest = request
        return scheduleResult
    }

    override fun cancel(notificationId: Long): NotificationCancelResult {
        cancelCalls += 1
        lastCancelledId = notificationId
        return cancelResult
    }
}
