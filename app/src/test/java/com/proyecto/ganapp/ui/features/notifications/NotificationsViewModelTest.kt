package com.proyecto.ganapp.ui.features.notifications

import com.proyecto.ganapp.domain.model.Notificacion
import com.proyecto.ganapp.domain.model.NotificationAlarmRequest
import com.proyecto.ganapp.domain.model.NotificationCancelResult
import com.proyecto.ganapp.domain.model.NotificationScheduleResult
import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import com.proyecto.ganapp.domain.model.TipoNotificacion
import com.proyecto.ganapp.domain.repository.NotificacionRepository
import com.proyecto.ganapp.domain.service.NotificationAlarmScheduler
import com.proyecto.ganapp.domain.usecase.notificacion.CreateAndScheduleNotificationUseCase
import com.proyecto.ganapp.domain.usecase.notificacion.CreateNotificacionUseCase
import com.proyecto.ganapp.domain.usecase.notificacion.CreateNotificationValidationError
import com.proyecto.ganapp.domain.usecase.notificacion.GetNotificationsUseCase
import com.proyecto.ganapp.domain.usecase.notificacion.ScheduleNotificationUseCase
import com.proyecto.ganapp.testutil.MainDispatcherRule
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fecha = LocalDate.of(2027, 6, 15).toEpochDay()

    private lateinit var repository: NotificationCreateProbeRepository
    private lateinit var scheduler: NotificationCreateProbeScheduler
    private lateinit var viewModel: NotificationsViewModel

    @Before
    fun setUp() {
        repository = NotificationCreateProbeRepository()
        scheduler = NotificationCreateProbeScheduler()
        viewModel = NotificationsViewModel(
            getNotificationsUseCase = GetNotificationsUseCase(repository),
            createAndScheduleNotificationUseCase = CreateAndScheduleNotificationUseCase(
                createNotificacionUseCase = CreateNotificacionUseCase(repository),
                scheduleNotificationUseCase = ScheduleNotificationUseCase(scheduler),
            ),
        )
    }

    @Test
    fun guardadoValido_llamaAlPipelineUnaVezConElRequestExacto() =
        runTest(mainDispatcherRule.testDispatcher) {
            val events = collectEvents()
            fillForm(tipo = "Vacunación", recurrencia = "NO")

            viewModel.onSave(7L)
            advanceUntilIdle()

            val stored = repository.inserted.single()
            assertEquals(1, repository.insertCalls)
            assertEquals(1, scheduler.scheduleCalls)
            assertEquals("Vacuna aftosa", stored.nombre)
            assertEquals(TipoNotificacion.VACUNACION, TipoNotificacion.fromPersistentString(stored.tipo))
            assertEquals(fecha, stored.fechaInicio)
            assertEquals(fecha, stored.fechaFin)
            assertEquals("08:30", stored.hora)
            assertEquals(RecurrenciaNotificacion.NO, RecurrenciaNotificacion.fromPersistentString(stored.seRepite))
            assertEquals(7L, stored.idUsuario)
            assertEquals(listOf(NotificationCreateEvent.Success), events)
        }

    @Test
    fun labelsDeTipo_lleganAlEnum() = runTest(mainDispatcherRule.testDispatcher) {
        collectEvents()
        val cases = listOf(
            "Vacunación" to TipoNotificacion.VACUNACION,
            "Alimentación" to TipoNotificacion.ALIMENTACION,
            "Ciclo Reproductivo" to TipoNotificacion.CICLO_REPRODUCTIVO,
        )

        cases.forEach { (label, expected) ->
            fillForm(tipo = label, recurrencia = "NO")
            viewModel.onSave(7L)
            advanceUntilIdle()
            assertEquals(
                expected,
                TipoNotificacion.fromPersistentString(repository.inserted.last().tipo),
            )
        }
        assertEquals(3, repository.insertCalls)
    }

    @Test
    fun labelsDeRecurrencia_lleganAlEnum() = runTest(mainDispatcherRule.testDispatcher) {
        collectEvents()
        val cases = listOf(
            "NO" to RecurrenciaNotificacion.NO,
            "DIARIA" to RecurrenciaNotificacion.DIARIA,
            "SEMANAL" to RecurrenciaNotificacion.SEMANAL,
            "CADA 15 DÍAS" to RecurrenciaNotificacion.CADA_15_DIAS,
            "MENSUAL" to RecurrenciaNotificacion.MENSUAL,
            "ANUAL" to RecurrenciaNotificacion.ANUAL,
        )

        cases.forEach { (label, expected) ->
            fillForm(tipo = "Vacunación", recurrencia = label)
            viewModel.onSave(7L)
            advanceUntilIdle()
            assertEquals(
                expected,
                RecurrenciaNotificacion.fromPersistentString(repository.inserted.last().seRepite),
            )
        }
        assertEquals(6, repository.insertCalls)
    }

    @Test
    fun validationFailure_conservaFormularioYNoPersiste() =
        runTest(mainDispatcherRule.testDispatcher) {
            val events = collectEvents()
            fillForm(tipo = "Vacunación", recurrencia = "NO")
            viewModel.nombre.value = "   "

            viewModel.onSave(7L)
            advanceUntilIdle()

            assertEquals(0, repository.insertCalls)
            assertEquals(0, scheduler.scheduleCalls)
            assertEquals("   ", viewModel.nombre.value)
            assertEquals(fecha, viewModel.fechaInicio.value)
            val event = events.single() as NotificationCreateEvent.ValidationFailure
            assertTrue(CreateNotificationValidationError.BLANK_NAME in event.errors)
            assertFalse(viewModel.isSaving.value)
        }

    @Test
    fun persistenceFailure_conservaFormulario() = runTest(mainDispatcherRule.testDispatcher) {
        val events = collectEvents()
        repository.insertError = IllegalStateException("db")
        fillForm(tipo = "Alimentación", recurrencia = "DIARIA")

        viewModel.onSave(7L)
        advanceUntilIdle()

        assertEquals(listOf(NotificationCreateEvent.PersistenceFailure), events)
        assertEquals("Vacuna aftosa", viewModel.nombre.value)
        assertEquals("Alimentación", viewModel.tipo.value)
        assertEquals(0, scheduler.scheduleCalls)
        assertFalse(viewModel.isSaving.value)
    }

    @Test
    fun schedulingFailure_conservaIdYNoReinsertaNiResetea() =
        runTest(mainDispatcherRule.testDispatcher) {
            val events = collectEvents()
            repository.insertId = 55L
            scheduler.scheduleResult = NotificationScheduleResult.Failure(
                notificationId = 55L,
                cause = IllegalStateException("alarm"),
            )
            fillForm(tipo = "Ciclo Reproductivo", recurrencia = "SEMANAL")

            viewModel.onSave(9L)
            advanceUntilIdle()

            assertEquals(1, repository.insertCalls)
            assertEquals(1, scheduler.scheduleCalls)
            assertEquals(
                listOf(NotificationCreateEvent.SchedulingFailure(55L)),
                events,
            )
            assertEquals("Vacuna aftosa", viewModel.nombre.value)
            assertEquals("Ciclo Reproductivo", viewModel.tipo.value)
            assertEquals("SEMANAL", viewModel.seRepite.value)
            assertFalse(viewModel.isSaving.value)
        }

    @Test
    fun success_reseteaFormulario() = runTest(mainDispatcherRule.testDispatcher) {
        collectEvents()
        fillForm(tipo = "Vacunación", recurrencia = "MENSUAL")

        viewModel.onSave(7L)
        advanceUntilIdle()

        assertEquals("", viewModel.nombre.value)
        assertEquals("", viewModel.tipo.value)
        assertEquals(0L, viewModel.fechaInicio.value)
        assertEquals(0L, viewModel.fechaFin.value)
        assertEquals("NO", viewModel.seRepite.value)
        assertEquals("08:00", viewModel.hora.value)
        assertFalse(viewModel.isSaving.value)
    }

    @Test
    fun dobleTapDuranteSaving_creaUnaSolaVez() = runTest(mainDispatcherRule.testDispatcher) {
        collectEvents()
        repository.insertGate = CompletableDeferred()
        fillForm(tipo = "Vacunación", recurrencia = "NO")

        viewModel.onSave(7L)
        assertTrue(viewModel.isSaving.value)
        viewModel.onSave(7L)
        advanceUntilIdle()

        assertEquals(1, repository.insertCalls)

        repository.insertGate!!.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, repository.insertCalls)
        assertEquals(1, scheduler.scheduleCalls)
        assertFalse(viewModel.isSaving.value)
    }

    private fun fillForm(tipo: String, recurrencia: String) {
        viewModel.nombre.value = "Vacuna aftosa"
        viewModel.tipo.value = tipo
        viewModel.fechaInicio.value = fecha
        viewModel.fechaFin.value = fecha
        viewModel.hora.value = "08:30"
        viewModel.seRepite.value = recurrencia
    }

    private fun kotlinx.coroutines.test.TestScope.collectEvents(): MutableList<NotificationCreateEvent> {
        val events = mutableListOf<NotificationCreateEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events += it }
        }
        return events
    }
}

private class NotificationCreateProbeRepository : NotificacionRepository {
    val inserted = mutableListOf<Notificacion>()
    var insertCalls: Int = 0
    var insertId: Long = 77L
    var insertError: Throwable? = null
    var insertGate: CompletableDeferred<Unit>? = null

    override fun getAllNotifications(userId: Long): Flow<List<Notificacion>> = flowOf(emptyList())

    override suspend fun insertNotification(notificacion: Notificacion): Long {
        insertCalls += 1
        inserted += notificacion
        insertGate?.await()
        insertError?.let { throw it }
        return insertId
    }

    override suspend fun updateNotification(notificacion: Notificacion) = Unit

    override suspend fun deleteNotification(id: Long) = Unit

    override suspend fun getNotificationById(id: Long): Notificacion? = null
}

private class NotificationCreateProbeScheduler : NotificationAlarmScheduler {
    var scheduleCalls: Int = 0
    var scheduleResult: NotificationScheduleResult? = null

    override fun scheduleNext(request: NotificationAlarmRequest): NotificationScheduleResult {
        scheduleCalls += 1
        return scheduleResult ?: NotificationScheduleResult.Scheduled(
            notificationId = request.notificationId,
            triggerAtMillis = request.triggerAtMillis,
        )
    }

    override fun cancel(notificationId: Long): NotificationCancelResult {
        return NotificationCancelResult.Cancelled(notificationId)
    }
}
