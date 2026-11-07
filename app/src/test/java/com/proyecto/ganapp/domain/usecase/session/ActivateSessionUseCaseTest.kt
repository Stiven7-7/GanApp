package com.proyecto.ganapp.domain.usecase.session

import com.proyecto.ganapp.domain.model.PlantillaNotificacion
import com.proyecto.ganapp.domain.model.ProgramacionNotificacion
import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import com.proyecto.ganapp.domain.model.TipoNotificacion
import com.proyecto.ganapp.domain.repository.NotificationScheduleRepository
import com.proyecto.ganapp.domain.repository.NotificationTemplateRepository
import com.proyecto.ganapp.domain.repository.SessionRepository
import com.proyecto.ganapp.domain.service.LegacyNotificationAlarmCanceller
import com.proyecto.ganapp.domain.service.NotificationRecurrenceEngine
import com.proyecto.ganapp.domain.service.NotificationScheduleAlarmService
import com.proyecto.ganapp.domain.service.ScheduleAlarmCancelResult
import com.proyecto.ganapp.domain.service.ScheduleAlarmResult
import com.proyecto.ganapp.domain.usecase.notificacion.ReconcileNotificationScheduleAlarmsUseCase
import java.time.LocalDate
import java.time.LocalTime
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
    private lateinit var templates: ActivateProbeTemplateRepository
    private lateinit var schedules: ActivateProbeScheduleRepository
    private lateinit var alarms: ActivateProbeAlarmService
    private lateinit var legacy: ActivateProbeLegacyCanceller
    private lateinit var useCase: ActivateSessionUseCase

    @Before
    fun setUp() {
        session = ActivateProbeSessionRepository()
        templates = ActivateProbeTemplateRepository()
        schedules = ActivateProbeScheduleRepository()
        alarms = ActivateProbeAlarmService()
        legacy = ActivateProbeLegacyCanceller()
        useCase = ActivateSessionUseCase(
            saveSessionUseCase = SaveSessionUseCase(session),
            reconcileNotificationScheduleAlarmsUseCase = ReconcileNotificationScheduleAlarmsUseCase(
                sessionRepository = session,
                templateRepository = templates,
                scheduleRepository = schedules,
                alarmService = alarms,
                legacyCanceller = legacy,
            ),
        )
    }

    @Test
    fun userIdInvalido_noReconcilia() = runTest {
        val result = useCase(userId = 0L, now = now, zoneId = bogota)

        assertEquals(ActivateSessionResult.InvalidUserId, result)
        assertEquals(0, session.observeCalls)
        assertEquals(0, templates.calls)
    }

    @Test
    fun falloGuardandoSesion_noReconcilia() = runTest {
        session.saveError = IllegalStateException("save")

        val result = useCase(userId = 7L, now = now, zoneId = bogota)

        assertEquals(ActivateSessionResult.SessionSaveFailure, result)
        assertEquals(0, session.observeCalls)
        assertEquals(0, templates.calls)
        assertEquals(0, session.clearCalls)
    }

    @Test
    fun saveExitoso_reconciliaUnaVezConNowYZoneIdRecibidos() = runTest {
        templates.templates = listOf(template(42L))
        schedules.byTemplate[42L] = listOf(schedule(id = 9L, templateId = 42L))

        val result = useCase(userId = 7L, now = now, zoneId = bogota)

        val expected = NotificationRecurrenceEngine.nextOccurrence(
            recurrence = RecurrenciaNotificacion.NO,
            fechaInicio = LocalDate.of(2026, 6, 15).toEpochDay(),
            fechaFin = LocalDate.of(2026, 6, 15).toEpochDay(),
            hora = "08:00",
            afterMillis = now,
            zoneId = bogota,
        )
        val otherZone = NotificationRecurrenceEngine.nextOccurrence(
            recurrence = RecurrenciaNotificacion.NO,
            fechaInicio = LocalDate.of(2026, 6, 15).toEpochDay(),
            fechaFin = LocalDate.of(2026, 6, 15).toEpochDay(),
            hora = "08:00",
            afterMillis = now,
            zoneId = ZoneId.of("Europe/Madrid"),
        )
        assertEquals(ActivateSessionResult.Success, result)
        assertEquals(1, session.observeCalls)
        assertEquals(listOf(42L), legacy.ids)
        assertEquals(9L, alarms.scheduled.single().first)
        assertEquals(expected, alarms.scheduled.single().second)
        assertNotEquals(otherZone, alarms.scheduled.single().second)
    }

    @Test
    fun reconciledSinFallosConSkipped_retornaSuccess() = runTest {
        templates.templates = listOf(template(0L))

        val result = useCase(userId = 7L, now = now, zoneId = bogota)

        assertEquals(ActivateSessionResult.Success, result)
        assertEquals(0, alarms.scheduled.size)
        assertEquals(0, session.clearCalls)
    }

    @Test
    fun reconciledConFallos_degradaSinRollback() = runTest {
        templates.templates = listOf(template(42L))
        schedules.byTemplate[42L] = listOf(schedule(9L, 42L))
        alarms.scheduleResult = ScheduleAlarmResult.Failure(IllegalStateException("alarm"))

        val result = useCase(userId = 7L, now = now, zoneId = bogota)

        assertEquals(ActivateSessionResult.SuccessWithNotificationReconciliationFailure, result)
        assertEquals(1, session.saveCalls)
        assertEquals(0, session.clearCalls)
    }

    @Test
    fun falloGlobalDeReconciliacion_degradaSinRollback() = runTest {
        templates.error = IllegalStateException("templates")

        val result = useCase(userId = 7L, now = now, zoneId = bogota)

        assertEquals(ActivateSessionResult.SuccessWithNotificationReconciliationFailure, result)
        assertEquals(1, session.saveCalls)
        assertEquals(0, alarms.scheduled.size)
        assertEquals(0, session.clearCalls)
    }

    @Test
    fun sinUsuarioDespuesDeGuardar_degradaSinRollback() = runTest {
        session.hideAuthenticatedUser = true

        val result = useCase(userId = 7L, now = now, zoneId = bogota)

        assertEquals(ActivateSessionResult.SuccessWithNotificationReconciliationFailure, result)
        assertEquals(1, session.saveCalls)
        assertEquals(0, templates.calls)
        assertEquals(0, session.clearCalls)
    }

    @Test
    fun cancellationExceptionDeReconciliacion_sePropaga() = runTest {
        val cancellation = CancellationException("reconcile")
        templates.error = cancellation

        try {
            useCase(userId = 7L, now = now, zoneId = bogota)
            fail("La cancelación debió propagarse")
        } catch (exception: CancellationException) {
            assertSame(cancellation, exception)
        }
        assertEquals(1, session.saveCalls)
        assertEquals(0, session.clearCalls)
    }

    private fun template(id: Long) = PlantillaNotificacion(
        idPlantilla = id,
        titulo = "Vacuna",
        tipo = TipoNotificacion.VACUNACION,
        idUsuario = 7L,
    )

    private fun schedule(id: Long, templateId: Long) = ProgramacionNotificacion(
        idProgramacion = id,
        idPlantilla = templateId,
        idAnimal = 3L,
        fechaInicio = LocalDate.of(2026, 6, 15),
        fechaFin = LocalDate.of(2026, 6, 15),
        hora = LocalTime.of(8, 0),
        recurrencia = RecurrenciaNotificacion.NO,
    )
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
        saveError?.let { throw it }
        savedUserId = userId
    }

    override suspend fun clearAuthenticatedUserId() {
        clearCalls += 1
        savedUserId = null
    }
}

private class ActivateProbeTemplateRepository : NotificationTemplateRepository {
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

private class ActivateProbeScheduleRepository : NotificationScheduleRepository {
    val byTemplate = mutableMapOf<Long, List<ProgramacionNotificacion>>()

    override suspend fun getSchedulesByTemplate(templateId: Long): List<ProgramacionNotificacion> =
        byTemplate[templateId].orEmpty()

    override suspend fun insertSchedule(schedule: ProgramacionNotificacion): Long = 0L
    override suspend fun getScheduleById(scheduleId: Long): ProgramacionNotificacion? = null
    override suspend fun updateSchedule(schedule: ProgramacionNotificacion): Boolean = false
    override suspend fun deleteSchedule(scheduleId: Long) = Unit
    override suspend fun getSchedulesByAnimal(animalId: Long): List<ProgramacionNotificacion> = emptyList()
}

private class ActivateProbeAlarmService : NotificationScheduleAlarmService {
    val scheduled = mutableListOf<Pair<Long, Long>>()
    var scheduleResult: ScheduleAlarmResult = ScheduleAlarmResult.Scheduled

    override fun schedule(scheduleId: Long, triggerAtMillis: Long): ScheduleAlarmResult {
        val result = scheduleResult
        if (result is ScheduleAlarmResult.Scheduled) scheduled += scheduleId to triggerAtMillis
        return result
    }

    override fun cancel(scheduleId: Long): ScheduleAlarmCancelResult = ScheduleAlarmCancelResult.Cancelled
}

private class ActivateProbeLegacyCanceller : LegacyNotificationAlarmCanceller {
    val ids = mutableListOf<Long>()

    override fun cancel(notificationId: Long): ScheduleAlarmCancelResult {
        ids += notificationId
        return ScheduleAlarmCancelResult.Cancelled
    }
}
