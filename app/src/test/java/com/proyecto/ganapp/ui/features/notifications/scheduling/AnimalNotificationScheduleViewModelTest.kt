package com.proyecto.ganapp.ui.features.notifications.scheduling

import androidx.lifecycle.ViewModelStore
import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.EspecieAnimal
import com.proyecto.ganapp.domain.model.PlantillaNotificacion
import com.proyecto.ganapp.domain.model.ProgramacionNotificacion
import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import com.proyecto.ganapp.domain.model.TipoNotificacion
import com.proyecto.ganapp.domain.repository.AnimalRepository
import com.proyecto.ganapp.domain.repository.NotificationScheduleRepository
import com.proyecto.ganapp.domain.repository.NotificationTemplateRepository
import com.proyecto.ganapp.domain.service.NotificationScheduleAlarmService
import com.proyecto.ganapp.domain.service.ScheduleAlarmCancelResult
import com.proyecto.ganapp.domain.service.ScheduleAlarmResult
import com.proyecto.ganapp.domain.usecase.animal.GetAnimalByIdUseCase
import com.proyecto.ganapp.domain.usecase.notificacion.CreateAnimalNotificationScheduleUseCase
import com.proyecto.ganapp.domain.usecase.notificacion.GetNotificationTemplatesUseCase
import com.proyecto.ganapp.domain.usecase.notificacion.TemplateRepositoryProbe
import com.proyecto.ganapp.domain.usecase.notificacion.TemplateSessionProbe
import com.proyecto.ganapp.testutil.MainDispatcherRule
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnimalNotificationScheduleViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val stores = mutableListOf<ViewModelStore>()

    @After fun clearViewModels() { stores.forEach { it.clear() } }

    private inner class Fixture(
        templates: List<PlantillaNotificacion> = listOf(template(11L), template(12L, "Suplemento", TipoNotificacion.ALIMENTACION)),
        animal: Animal? = animal(),
        failList: Boolean = false,
    ) {
        val zone: ZoneId = ZoneId.of("America/Bogota")
        val now: Long = LocalDate.of(2026, 11, 6).atTime(8, 0).atZone(zone).toInstant().toEpochMilli()
        val rows = MutableStateFlow(templates)
        val session = TemplateSessionProbe()
        val templateProbe = TemplateRepositoryProbe().also {
            it.observed[7L] = rows
            if (failList) it.failingMethod = "list"
        }
        val templatesRepository = object : NotificationTemplateRepository by templateProbe {
            override suspend fun getTemplateById(templateId: Long): PlantillaNotificacion? =
                rows.value.firstOrNull { it.idPlantilla == templateId }
        }
        val animals = AnimalProbe(animal)
        val schedules = ScheduleInsertProbe()
        val alarms = AlarmProbe()
        val vm = AnimalNotificationScheduleViewModel(
            animalId = 9L,
            getAnimal = GetAnimalByIdUseCase(animals, session),
            getTemplates = GetNotificationTemplatesUseCase(templatesRepository, session),
            createSchedule = CreateAnimalNotificationScheduleUseCase(
                schedules, templatesRepository, animals, session, alarms,
            ),
            nowMillis = { now },
            zoneId = zone,
        ).also { viewModel -> stores += ViewModelStore().apply { put("schedule", viewModel) } }

        fun collect(scope: TestScope): MutableList<AnimalScheduleEvent> {
            val received = mutableListOf<AnimalScheduleEvent>()
            scope.backgroundScope.launch(UnconfinedTestDispatcher(scope.testScheduler)) {
                vm.events.collect { received += it }
            }
            return received
        }
    }

    @Test fun loadStartsLoadingThenShowsAnimalAndTemplates() = runTest(main.testDispatcher) {
        val f = Fixture()
        assertEquals(AnimalScheduleLoad.Loading, f.vm.uiState.value.load)
        assertFalse(f.vm.uiState.value.canContinue)
        advanceUntilIdle()
        val ready = f.vm.uiState.value.load as AnimalScheduleLoad.Ready
        assertEquals("Mancha", ready.animal.nombre)
        assertEquals(EspecieAnimal.VACA, ready.animal.especie)
        assertEquals(listOf(11L, 12L), ready.templates.map { it.idPlantilla })
    }

    @Test fun emptyTemplatesKeepAnimalAndDisableContinue() = runTest(main.testDispatcher) {
        val f = Fixture(templates = emptyList())
        advanceUntilIdle()
        val empty = f.vm.uiState.value.load as AnimalScheduleLoad.Empty
        assertEquals("Mancha", empty.animal.nombre)
        assertFalse(f.vm.uiState.value.canContinue)
        f.vm.continueToConfigure()
        assertEquals(AnimalScheduleStep.SelectTemplate, f.vm.uiState.value.step)
    }

    @Test fun loadFailureShowsError() = runTest(main.testDispatcher) {
        val f = Fixture(failList = true)
        advanceUntilIdle()
        assertTrue(f.vm.uiState.value.load is AnimalScheduleLoad.Error)
    }

    @Test fun selectionIsSingleAndEnablesContinue() = runTest(main.testDispatcher) {
        val f = Fixture()
        advanceUntilIdle()
        assertFalse(f.vm.uiState.value.canContinue)
        f.vm.selectTemplate(11L)
        assertTrue(f.vm.uiState.value.canContinue)
        f.vm.selectTemplate(12L)
        assertEquals(12L, f.vm.uiState.value.selectedTemplateId)
        f.vm.selectTemplate(99L)
        assertEquals(12L, f.vm.uiState.value.selectedTemplateId)
    }

    @Test fun configureShowsSelectedAnimalAndTemplate() = runTest(main.testDispatcher) {
        val f = Fixture()
        advanceUntilIdle()
        f.vm.selectTemplate(12L)
        f.vm.continueToConfigure()
        val state = f.vm.uiState.value
        assertEquals(AnimalScheduleStep.Configure, state.step)
        assertEquals(9L, state.animal?.idAnimal)
        assertEquals("Mancha", state.animal?.nombre)
        assertEquals(12L, state.selectedTemplate?.idPlantilla)
        assertEquals("Suplemento", state.selectedTemplate?.titulo)
        assertEquals(TipoNotificacion.ALIMENTACION, state.selectedTemplate?.tipo)
    }

    @Test fun missingFieldsStayOnTheForm() = runTest(main.testDispatcher) {
        val f = Fixture()
        advanceUntilIdle()
        f.vm.selectTemplate(11L)
        f.vm.continueToConfigure()
        f.vm.save()
        assertEquals(
            setOf(ScheduleField.FechaInicio, ScheduleField.FechaFin, ScheduleField.Hora),
            f.vm.uiState.value.draft.fieldErrors.keys,
        )
        assertEquals(0, f.schedules.attempts)
    }

    @Test fun endBeforeStartAndNoMismatchExposeFieldErrors() = runTest(main.testDispatcher) {
        val f = Fixture()
        advanceUntilIdle()
        f.vm.selectTemplate(11L)
        f.vm.continueToConfigure()
        f.vm.changeStart(LocalDate.of(2026, 11, 20))
        f.vm.changeEnd(LocalDate.of(2026, 11, 15))
        f.vm.changeTime(LocalTime.of(9, 0))
        f.vm.changeRecurrence(RecurrenciaNotificacion.DIARIA)
        f.vm.save()
        advanceUntilIdle()
        assertTrue(f.vm.uiState.value.draft.fieldErrors.containsKey(ScheduleField.FechaFin))
        assertEquals(0, f.schedules.attempts)

        f.vm.changeStart(LocalDate.of(2026, 11, 15))
        f.vm.changeEnd(LocalDate.of(2026, 11, 16))
        f.vm.changeRecurrence(RecurrenciaNotificacion.NO)
        f.vm.save()
        advanceUntilIdle()
        assertTrue(f.vm.uiState.value.draft.fieldErrors.containsKey(ScheduleField.FechaFin))
        assertEquals(0, f.schedules.inserted.size)
        assertTrue(f.schedules.updated.isEmpty())
    }

    @Test fun saveBuildsIdsWithoutSnapshotsAndStripsSeconds() = runTest(main.testDispatcher) {
        val f = Fixture()
        val events = f.collect(this)
        advanceUntilIdle()
        f.prepare(LocalTime.of(9, 15, 30, 400))
        f.vm.save()
        advanceUntilIdle()
        val saved = f.schedules.inserted.single()
        assertEquals(11L, saved.idPlantilla)
        assertEquals(9L, saved.idAnimal)
        assertEquals(LocalDate.of(2026, 11, 15), saved.fechaInicio)
        assertEquals(LocalDate.of(2026, 11, 20), saved.fechaFin)
        assertEquals(LocalTime.of(9, 15), saved.hora)
        assertEquals(0, saved.hora.second)
        assertEquals(0, saved.hora.nano)
        assertEquals(RecurrenciaNotificacion.DIARIA, saved.recurrencia)
        assertTrue(f.schedules.updated.isEmpty())
        assertEquals(listOf(AnimalScheduleEvent.Completed), events)
        assertFalse(f.vm.uiState.value.isSaving)
    }

    @Test fun persistenceFailureDoesNotReportSchedulingFailure() = runTest(main.testDispatcher) {
        val f = Fixture()
        val events = f.collect(this)
        advanceUntilIdle()
        f.prepare(LocalTime.of(9, 0))
        f.schedules.failure = IllegalStateException("db")
        f.vm.save()
        advanceUntilIdle()
        assertTrue(f.schedules.inserted.isEmpty())
        assertEquals(listOf(AnimalScheduleEvent.Message("No se pudo guardar la programación.")), events)
        assertFalse(f.vm.uiState.value.isSaving)
    }

    @Test fun schedulingFailureKeepsScheduleIdAndDoesNotComplete() = runTest(main.testDispatcher) {
        val f = Fixture()
        val events = f.collect(this)
        advanceUntilIdle()
        f.prepare(LocalTime.of(9, 0))
        f.alarms.result = ScheduleAlarmResult.Failure(IllegalStateException("alarm"))
        f.vm.save()
        advanceUntilIdle()
        assertEquals(9L, f.schedules.inserted.single().idAnimal)
        assertEquals(listOf(AnimalScheduleEvent.SchedulingFailed(101L)), events)
        assertFalse(f.vm.uiState.value.isSaving)
    }

    @Test fun secondSaveWhileSavingIsIgnored() = runTest(main.testDispatcher) {
        val f = Fixture()
        val events = f.collect(this)
        advanceUntilIdle()
        f.prepare(LocalTime.of(9, 0))
        f.schedules.gate = CompletableDeferred()
        f.vm.save()
        assertTrue(f.vm.uiState.value.isSaving)
        f.vm.save()
        advanceUntilIdle()
        assertEquals(1, f.schedules.attempts)
        f.schedules.gate?.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, f.schedules.inserted.size)
        assertEquals(listOf(AnimalScheduleEvent.Completed), events)
    }

    @Test fun sameTemplateAndAnimalCanBeScheduledTwice() = runTest(main.testDispatcher) {
        val f = Fixture()
        val events = f.collect(this)
        advanceUntilIdle()
        f.prepare(LocalTime.of(9, 0))
        f.vm.save()
        advanceUntilIdle()
        f.vm.changeTime(LocalTime.of(18, 30))
        f.vm.save()
        advanceUntilIdle()
        assertEquals(listOf(11L, 11L), f.schedules.inserted.map { it.idPlantilla })
        assertEquals(listOf(9L, 9L), f.schedules.inserted.map { it.idAnimal })
        assertEquals(LocalTime.of(18, 30), f.schedules.inserted[1].hora)
        assertEquals(
            listOf(AnimalScheduleEvent.Completed, AnimalScheduleEvent.Completed),
            events,
        )
    }

    private fun Fixture.prepare(time: LocalTime) {
        vm.selectTemplate(11L)
        vm.continueToConfigure()
        vm.changeStart(LocalDate.of(2026, 11, 15))
        vm.changeEnd(LocalDate.of(2026, 11, 20))
        vm.changeTime(time)
        vm.changeRecurrence(RecurrenciaNotificacion.DIARIA)
    }
}

private fun template(
    id: Long,
    titulo: String = "Vacuna aftosa",
    tipo: TipoNotificacion = TipoNotificacion.VACUNACION,
) = PlantillaNotificacion(id, titulo, tipo, 7L)

private fun animal() = Animal(
    idAnimal = 9L,
    especie = EspecieAnimal.VACA,
    nombre = "Mancha",
    raza = "Holstein",
    edad = null,
    peso = null,
    color = null,
    fotoUri = null,
    idUsuario = 7L,
)

private class AnimalProbe(private val animal: Animal?) : AnimalRepository {
    override fun getAllAnimals(userId: Long): Flow<List<Animal>> = flowOf(emptyList())
    override fun getAnimalsByUser(userId: Long): Flow<List<Animal>> = flowOf(emptyList())
    override fun getFavoriteAnimalsByUser(userId: Long): Flow<List<Animal>> = flowOf(emptyList())
    override suspend fun getAnimalById(id: Long): Animal? = animal?.takeIf { it.idAnimal == id }
    override suspend fun insertAnimal(animal: Animal): Long = 0L
    override suspend fun updateAnimal(animal: Animal): Boolean = false
    override suspend fun deleteAnimal(id: Long) = Unit
}

private class ScheduleInsertProbe : NotificationScheduleRepository {
    val inserted = mutableListOf<ProgramacionNotificacion>()
    val updated = mutableListOf<ProgramacionNotificacion>()
    var attempts = 0
    var gate: CompletableDeferred<Unit>? = null
    var failure: Exception? = null
    private var nextId = 101L

    override suspend fun insertSchedule(schedule: ProgramacionNotificacion): Long {
        attempts++
        gate?.await()
        failure?.let { throw it }
        inserted += schedule
        return nextId++
    }

    override suspend fun getScheduleById(scheduleId: Long): ProgramacionNotificacion? = null
    override suspend fun updateSchedule(schedule: ProgramacionNotificacion): Boolean {
        updated += schedule
        return true
    }
    override suspend fun deleteSchedule(scheduleId: Long) = Unit
    override suspend fun getSchedulesByAnimal(animalId: Long): List<ProgramacionNotificacion> = emptyList()
    override suspend fun getSchedulesByTemplate(templateId: Long): List<ProgramacionNotificacion> = emptyList()
}

private class AlarmProbe : NotificationScheduleAlarmService {
    var result: ScheduleAlarmResult = ScheduleAlarmResult.Scheduled
    override fun schedule(scheduleId: Long, triggerAtMillis: Long): ScheduleAlarmResult = result
    override fun cancel(scheduleId: Long): ScheduleAlarmCancelResult = ScheduleAlarmCancelResult.Cancelled
}
