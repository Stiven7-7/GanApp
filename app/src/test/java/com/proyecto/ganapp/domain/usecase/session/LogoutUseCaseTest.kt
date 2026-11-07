package com.proyecto.ganapp.domain.usecase.session

import com.proyecto.ganapp.domain.model.PlantillaNotificacion
import com.proyecto.ganapp.domain.model.ProgramacionNotificacion
import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import com.proyecto.ganapp.domain.model.TipoNotificacion
import com.proyecto.ganapp.domain.repository.NotificationScheduleRepository
import com.proyecto.ganapp.domain.repository.NotificationTemplateRepository
import com.proyecto.ganapp.domain.repository.SessionRepository
import com.proyecto.ganapp.domain.service.LegacyNotificationAlarmCanceller
import com.proyecto.ganapp.domain.service.NotificationScheduleAlarmService
import com.proyecto.ganapp.domain.service.ScheduleAlarmCancelResult
import com.proyecto.ganapp.domain.service.ScheduleAlarmResult
import com.proyecto.ganapp.domain.usecase.notificacion.CancelAuthenticatedUserScheduleAlarmsUseCase
import java.time.LocalDate
import java.time.LocalTime
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
    private lateinit var templates: LogoutTemplateRepository
    private lateinit var schedules: LogoutScheduleRepository
    private lateinit var alarms: LogoutAlarmService
    private lateinit var legacy: LogoutLegacyCanceller
    private lateinit var useCase: LogoutUseCase

    @Before
    fun setUp() {
        session = LogoutSessionRepository()
        templates = LogoutTemplateRepository()
        schedules = LogoutScheduleRepository()
        alarms = LogoutAlarmService()
        legacy = LogoutLegacyCanceller()
        useCase = LogoutUseCase(
            cancelAuthenticatedUserScheduleAlarmsUseCase = CancelAuthenticatedUserScheduleAlarmsUseCase(
                sessionRepository = session,
                templateRepository = templates,
                scheduleRepository = schedules,
                alarmService = alarms,
                legacyCanceller = legacy,
            ),
            clearSessionUseCase = ClearSessionUseCase(session),
        )
    }

    @Test
    fun batchSinFallosYClearExitoso_retornaSuccess() = runTest {
        session.userId = 7L
        templates.templates = listOf(template(10L))
        schedules.byTemplate[10L] = listOf(schedule(4L, 10L))

        val result = useCase()

        assertEquals(LogoutResult.Success, result)
        assertEquals(listOf(10L), legacy.ids)
        assertEquals(listOf(4L), alarms.cancelled)
        assertEquals(1, session.clearCalls)
    }

    @Test
    fun skippedSinFallos_limpiaSesion() = runTest {
        session.userId = 7L
        templates.templates = listOf(template(10L), template(20L), template(0L))

        val result = useCase()

        assertEquals(LogoutResult.Success, result)
        assertEquals(listOf(10L, 20L), legacy.ids)
        assertEquals(1, session.clearCalls)
    }

    @Test
    fun falloRealDeCancelacion_noLimpiaSesion() = runTest {
        session.userId = 7L
        templates.templates = listOf(template(10L))
        legacy.result = ScheduleAlarmCancelResult.Failure(IllegalStateException("alarm"))

        val result = useCase()

        assertEquals(LogoutResult.AlarmCancellationFailure, result)
        assertEquals(0, session.clearCalls)
    }

    @Test
    fun falloGlobalDelBatch_noLimpiaSesion() = runTest {
        session.userId = 7L
        templates.error = IllegalStateException("templates")

        val result = useCase()

        assertEquals(LogoutResult.AlarmCancellationFailure, result)
        assertEquals(0, session.clearCalls)
        assertEquals(0, legacy.ids.size)
    }

    @Test
    fun clearInesperado_retornaSessionClearFailure() = runTest {
        session.userId = 7L
        templates.templates = listOf(template(10L))
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
        assertEquals(0, templates.calls)
        assertEquals(0, legacy.ids.size)
    }

    @Test
    fun cancellationExceptionAlCancelarAlarmas_sePropagaSinLimpiarSesion() = runTest {
        val cancellation = CancellationException("cancel")
        session.userId = 7L
        templates.templates = listOf(template(10L))
        legacy.error = cancellation

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

    private fun template(id: Long) = PlantillaNotificacion(id, "Vacuna", TipoNotificacion.VACUNACION, 7L)

    private fun schedule(id: Long, templateId: Long) = ProgramacionNotificacion(
        idProgramacion = id,
        idPlantilla = templateId,
        idAnimal = 1L,
        fechaInicio = LocalDate.of(2026, 6, 15),
        fechaFin = LocalDate.of(2026, 6, 15),
        hora = LocalTime.of(8, 0),
        recurrencia = RecurrenciaNotificacion.NO,
    )
}

private class LogoutSessionRepository : SessionRepository {
    var userId: Long? = null
    var clearCalls: Int = 0
    var clearError: Throwable? = null

    override fun observeAuthenticatedUserId(): Flow<Long?> = flowOf(userId)

    override suspend fun saveAuthenticatedUserId(userId: Long) = Unit

    override suspend fun clearAuthenticatedUserId() {
        clearCalls += 1
        clearError?.let { throw it }
    }
}

private class LogoutTemplateRepository : NotificationTemplateRepository {
    var templates: List<PlantillaNotificacion> = emptyList()
    var calls: Int = 0
    var error: Throwable? = null

    override fun getTemplatesByUser(userId: Long): Flow<List<PlantillaNotificacion>> {
        calls += 1
        val failure = error
        return if (failure != null) flow { throw failure } else flowOf(templates)
    }

    override suspend fun getTemplateById(templateId: Long): PlantillaNotificacion? = null
    override suspend fun insertTemplate(template: PlantillaNotificacion): Long = 0L
    override suspend fun updateTemplate(template: PlantillaNotificacion): Boolean = false
    override suspend fun deleteTemplate(templateId: Long) = Unit
    override suspend fun hasSchedules(templateId: Long): Boolean = false
}

private class LogoutScheduleRepository : NotificationScheduleRepository {
    val byTemplate = mutableMapOf<Long, List<ProgramacionNotificacion>>()

    override suspend fun getSchedulesByTemplate(templateId: Long): List<ProgramacionNotificacion> =
        byTemplate[templateId].orEmpty()

    override suspend fun insertSchedule(schedule: ProgramacionNotificacion): Long = 0L
    override suspend fun getScheduleById(scheduleId: Long): ProgramacionNotificacion? = null
    override suspend fun updateSchedule(schedule: ProgramacionNotificacion): Boolean = false
    override suspend fun deleteSchedule(scheduleId: Long) = Unit
    override suspend fun getSchedulesByAnimal(animalId: Long): List<ProgramacionNotificacion> = emptyList()
}

private class LogoutAlarmService : NotificationScheduleAlarmService {
    val cancelled = mutableListOf<Long>()

    override fun schedule(scheduleId: Long, triggerAtMillis: Long): ScheduleAlarmResult =
        ScheduleAlarmResult.Scheduled

    override fun cancel(scheduleId: Long): ScheduleAlarmCancelResult {
        cancelled += scheduleId
        return ScheduleAlarmCancelResult.Cancelled
    }
}

private class LogoutLegacyCanceller : LegacyNotificationAlarmCanceller {
    val ids = mutableListOf<Long>()
    var result: ScheduleAlarmCancelResult = ScheduleAlarmCancelResult.Cancelled
    var error: Throwable? = null

    override fun cancel(notificationId: Long): ScheduleAlarmCancelResult {
        error?.let { throw it }
        ids += notificationId
        return result
    }
}
