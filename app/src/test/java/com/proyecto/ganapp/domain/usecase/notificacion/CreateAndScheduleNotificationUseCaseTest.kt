package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.Notificacion
import com.proyecto.ganapp.domain.model.NotificationAlarmRequest
import com.proyecto.ganapp.domain.model.NotificationCancelResult
import com.proyecto.ganapp.domain.model.NotificationScheduleResult
import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import com.proyecto.ganapp.domain.model.TipoNotificacion
import com.proyecto.ganapp.domain.repository.NotificacionRepository
import com.proyecto.ganapp.domain.service.NotificationAlarmScheduler
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
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class CreateAndScheduleNotificationUseCaseTest {

    private val zone = ZoneId.systemDefault()
    private val day = LocalDate.of(2026, 6, 15)
    private val start = day.toEpochDay()
    private val now = day.minusDays(1).atTime(LocalTime.NOON).atZone(zone).toInstant().toEpochMilli()

    private lateinit var repository: RecordingNotificacionRepository
    private lateinit var alarmScheduler: RecordingNotificationAlarmScheduler
    private lateinit var useCase: CreateAndScheduleNotificationUseCase

    @Before
    fun setUp() {
        repository = RecordingNotificacionRepository()
        alarmScheduler = RecordingNotificationAlarmScheduler()
        useCase = CreateAndScheduleNotificationUseCase(
            createNotificacionUseCase = CreateNotificacionUseCase(repository),
            scheduleNotificationUseCase = ScheduleNotificationUseCase(alarmScheduler),
        )
    }

    @Test
    fun requestInvalido_noPrograma() = runTest {
        val result = useCase(validRequest(nombre = ""), now)

        assertEquals(
            CreateNotificationResult.ValidationFailure(
                setOf(CreateNotificationValidationError.BLANK_NAME),
            ),
            result,
        )
        assertEquals(0, alarmScheduler.scheduleCalls)
        assertEquals(0, repository.insertCalls)
        assertEquals(0, repository.deleteCalls)
    }

    @Test
    fun persistenceFailure_noPrograma() = runTest {
        val cause = IllegalStateException("db error")
        repository.insertError = cause

        val result = useCase(validRequest(), now)

        assertEquals(CreateNotificationResult.PersistenceFailure(cause), result)
        assertSame(cause, (result as CreateNotificationResult.PersistenceFailure).cause)
        assertEquals(0, alarmScheduler.scheduleCalls)
        assertEquals(0, repository.deleteCalls)
    }

    @Test
    fun idDePersistenciaCero_noPrograma() = runTest {
        repository.nextId = 0L

        val result = useCase(validRequest(), now)

        assertEquals(CreateNotificationResult.PersistenceFailure(), result)
        assertEquals(0, alarmScheduler.scheduleCalls)
        assertEquals(1, repository.insertCalls)
        assertEquals(0, repository.deleteCalls)
    }

    @Test
    fun persistido_programaLaPrimeraOcurrenciaYRetornaSuccess() = runTest {
        repository.nextId = 42L
        val request = validRequest(nombre = " Vacuna ")
        val firstOccurrence = CreateNotificationValidator.firstOccurrence(request)
        alarmScheduler.scheduleResult = NotificationScheduleResult.Scheduled(42L, firstOccurrence!!)

        val result = useCase(request, now)

        assertEquals(
            CreateNotificationResult.Success(
                notificationId = 42L,
                firstOccurrenceAtMillis = firstOccurrence,
            ),
            result,
        )
        assertEquals(1, alarmScheduler.scheduleCalls)
        assertEquals(1, repository.insertCalls)
        assertEquals(0, repository.deleteCalls)
        assertEquals(" Vacuna ", request.nombre)
    }

    @Test
    fun schedulerRecibeElIdRealPersistido() = runTest {
        repository.nextId = 42L
        val request = validRequest()
        val firstOccurrence = CreateNotificationValidator.firstOccurrence(request)!!
        alarmScheduler.scheduleResult = NotificationScheduleResult.Scheduled(42L, firstOccurrence)

        useCase(request, now)

        assertEquals(42L, alarmScheduler.lastRequest?.notificationId)
    }

    @Test
    fun schedulerRecibeLaPrimeraOcurrenciaDelValidador() = runTest {
        repository.nextId = 42L
        val request = validRequest()
        val firstOccurrence = CreateNotificationValidator.firstOccurrence(request)!!
        alarmScheduler.scheduleResult = NotificationScheduleResult.Scheduled(42L, firstOccurrence)

        useCase(request, now)

        assertEquals(firstOccurrence, alarmScheduler.lastRequest?.triggerAtMillis)
    }

    @Test
    fun now_seReenviaYNoUsaElRelojDelSistema() = runTest {
        repository.nextId = 42L
        val request = validRequest()
        val firstOccurrence = CreateNotificationValidator.firstOccurrence(request)!!
        val sameNow = firstOccurrence - 1L
        alarmScheduler.scheduleResult = NotificationScheduleResult.Scheduled(42L, firstOccurrence)

        val result = useCase(request, sameNow)

        assertEquals(
            CreateNotificationResult.Success(
                notificationId = 42L,
                firstOccurrenceAtMillis = firstOccurrence,
            ),
            result,
        )
        assertEquals(firstOccurrence, alarmScheduler.lastRequest?.triggerAtMillis)
        assertTrue(firstOccurrence > sameNow)
        assertEquals(1, alarmScheduler.scheduleCalls)
    }

    @Test
    fun payload_esNeutroYElNombreVaSinEspaciosExternos() = runTest {
        repository.nextId = 42L
        val request = validRequest(nombre = " Vacuna ")
        val firstOccurrence = CreateNotificationValidator.firstOccurrence(request)!!
        alarmScheduler.scheduleResult = NotificationScheduleResult.Scheduled(42L, firstOccurrence)

        useCase(request, now)

        assertEquals("Recordatorio GanApp", alarmScheduler.lastRequest?.title)
        assertEquals("Recordatorio pendiente: Vacuna", alarmScheduler.lastRequest?.message)
    }

    @Test
    fun falloDelScheduler_conservaLaFilaYLaCausa() = runTest {
        repository.nextId = 42L
        val cause = IllegalStateException("alarm")
        alarmScheduler.scheduleResult = NotificationScheduleResult.Failure(42L, cause)

        val result = useCase(validRequest(), now)

        assertEquals(CreateNotificationResult.SchedulingFailure(42L, cause), result)
        assertSame(cause, (result as CreateNotificationResult.SchedulingFailure).cause)
        assertEquals(1, repository.insertCalls)
        assertEquals(0, repository.deleteCalls)
        assertEquals(1, alarmScheduler.scheduleCalls)
    }

    @Test
    fun invalidTriggerTimeInesperado_esSchedulingFailure() = runTest {
        repository.nextId = 42L
        alarmScheduler.scheduleResult = NotificationScheduleResult.InvalidTriggerTime

        val result = useCase(validRequest(), now)

        assertEquals(CreateNotificationResult.SchedulingFailure(42L, null), result)
        assertEquals(0, repository.deleteCalls)
    }

    @Test
    fun invalidNotificationIdInesperado_esSchedulingFailure() = runTest {
        repository.nextId = 42L
        alarmScheduler.scheduleResult = NotificationScheduleResult.InvalidNotificationId

        val result = useCase(validRequest(), now)

        assertEquals(CreateNotificationResult.SchedulingFailure(42L, null), result)
        assertNull((result as CreateNotificationResult.SchedulingFailure).cause)
        assertEquals(0, repository.deleteCalls)
    }

    @Test
    fun noNextOccurrenceInesperado_noEsSuccess() = runTest {
        repository.nextId = 42L
        alarmScheduler.scheduleResult = NotificationScheduleResult.NoNextOccurrence(42L)

        val result = useCase(validRequest(), now)

        assertEquals(CreateNotificationResult.SchedulingFailure(42L, null), result)
        assertEquals(0, repository.deleteCalls)
    }

    @Test
    fun cancellationException_sePropaga() = runTest {
        val cancellation = CancellationException("cancelled")
        repository.insertError = cancellation

        try {
            useCase(validRequest(), now)
            fail("La cancelación debió propagarse")
        } catch (exception: CancellationException) {
            assertSame(cancellation, exception)
        }
        assertEquals(0, alarmScheduler.scheduleCalls)
        assertEquals(0, repository.deleteCalls)
    }

    private fun validRequest(
        nombre: String = "Vacuna",
    ): CreateNotificationRequest {
        return CreateNotificationRequest(
            nombre = nombre,
            tipo = TipoNotificacion.VACUNACION,
            fechaInicio = start,
            fechaFin = start,
            hora = "08:30",
            recurrencia = RecurrenciaNotificacion.NO,
            idUsuario = 7L,
        )
    }
}

private class RecordingNotificacionRepository : NotificacionRepository {
    var nextId: Long = 0L
    var insertError: Throwable? = null
    var insertCalls: Int = 0
    var deleteCalls: Int = 0

    override fun getAllNotifications(userId: Long): Flow<List<Notificacion>> = emptyFlow()

    override suspend fun insertNotification(notificacion: Notificacion): Long {
        insertCalls += 1
        insertError?.let { throw it }
        return nextId
    }

    override suspend fun updateNotification(notificacion: Notificacion) = Unit

    override suspend fun deleteNotification(id: Long) {
        deleteCalls += 1
    }

    override suspend fun getNotificationById(id: Long): Notificacion? = null
}

private class RecordingNotificationAlarmScheduler : NotificationAlarmScheduler {
    var scheduleCalls: Int = 0
    var lastRequest: NotificationAlarmRequest? = null
    var scheduleResult: NotificationScheduleResult = NotificationScheduleResult.Scheduled(1L, 1L)

    override fun scheduleNext(request: NotificationAlarmRequest): NotificationScheduleResult {
        scheduleCalls += 1
        lastRequest = request
        return scheduleResult
    }

    override fun cancel(notificationId: Long): NotificationCancelResult {
        return NotificationCancelResult.Cancelled(notificationId)
    }
}
