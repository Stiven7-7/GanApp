package com.proyecto.ganapp.ui.features.notifications.animalschedules

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
import com.proyecto.ganapp.domain.usecase.animal.GetAnimalByIdUseCase
import com.proyecto.ganapp.domain.usecase.notificacion.GetAnimalNotificationSchedulesUseCase
import com.proyecto.ganapp.domain.usecase.notificacion.TemplateSessionProbe
import com.proyecto.ganapp.testutil.MainDispatcherRule
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnimalSchedulesViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val stores = mutableListOf<ViewModelStore>()

    @After fun clearViewModels() { stores.forEach { it.clear() } }

    private inner class Fixture(
        animalId: Long = 9L,
        animal: Animal? = animal(),
        schedules: List<ProgramacionNotificacion> = listOf(schedule(77L), schedule(78L, LocalTime.of(18, 30))),
        template: PlantillaNotificacion = template(),
        userId: Long? = 7L,
        failList: Boolean = false,
    ) {
        val session = TemplateSessionProbe(userId)
        val animals = AnimalReadProbe(animal)
        val listed = ScheduleReadProbe(schedules, failList)
        val templates = TemplateReadProbe(template)
        val vm = AnimalSchedulesViewModel(
            animalId = animalId,
            getSchedules = GetAnimalNotificationSchedulesUseCase(listed, templates, animals, session),
            getAnimal = GetAnimalByIdUseCase(animals, session),
        ).also { viewModel -> stores += ViewModelStore().apply { put("schedules", viewModel) } }
    }

    @Test fun loadStartsLoadingThenShowsEverySchedule() = runTest(main.testDispatcher) {
        val f = Fixture()
        assertEquals(AnimalSchedulesContent.Loading, f.vm.uiState.value)
        advanceUntilIdle()
        val success = f.vm.uiState.value as AnimalSchedulesContent.Success
        assertEquals("Mancha", success.animal.nombre)
        assertEquals(listOf(77L, 78L), success.schedules.map { it.programacion.idProgramacion })
        assertEquals(listOf(9L), f.listed.animalIds)
    }

    @Test fun oneScheduleStaysAsOneItem() = runTest(main.testDispatcher) {
        val only = schedule(77L)
        val f = Fixture(schedules = listOf(only))
        advanceUntilIdle()
        val success = f.vm.uiState.value as AnimalSchedulesContent.Success
        assertEquals(listOf(only), success.schedules.map { it.programacion })
        assertEquals(template(), success.schedules.single().plantilla)
    }

    @Test fun emptyListShowsEmpty() = runTest(main.testDispatcher) {
        val f = Fixture(schedules = emptyList())
        advanceUntilIdle()
        val empty = f.vm.uiState.value as AnimalSchedulesContent.Empty
        assertEquals("Mancha", empty.animal.nombre)
        assertEquals(AnimalSchedulesEmptyMessage, "Aún no hay programaciones.")
    }

    @Test fun loadFailureShowsErrorWithoutSchedules() = runTest(main.testDispatcher) {
        val f = Fixture(failList = true)
        advanceUntilIdle()
        assertEquals(AnimalSchedulesContent.Error(AnimalSchedulesErrorMessage), f.vm.uiState.value)
        assertEquals(1, f.animals.reads)
    }

    @Test fun sameTemplateStaysAsTwoItemsFromCurrentTemplateAndSchedule() = runTest(main.testDispatcher) {
        val current = template().copy(titulo = "Desparasitación", tipo = TipoNotificacion.CICLO_REPRODUCTIVO)
        val morning = schedule(77L)
        val evening = schedule(78L, LocalTime.of(18, 30), RecurrenciaNotificacion.SEMANAL)
        val f = Fixture(schedules = listOf(morning, evening), template = current)
        advanceUntilIdle()
        val success = f.vm.uiState.value as AnimalSchedulesContent.Success
        assertEquals(2, success.schedules.size)
        assertEquals(listOf(5L, 5L), success.schedules.map { it.plantilla.idPlantilla })
        assertTrue(success.schedules.all { it.plantilla == current })
        assertEquals(morning, success.schedules[0].programacion)
        assertEquals(evening, success.schedules[1].programacion)
        assertEquals("Desparasitación", success.schedules[0].plantilla.titulo)
        assertEquals(TipoNotificacion.CICLO_REPRODUCTIVO, success.schedules[1].plantilla.tipo)
        assertEquals(LocalTime.of(8, 0), success.schedules[0].programacion.hora)
        assertEquals(RecurrenciaNotificacion.SEMANAL, success.schedules[1].programacion.recurrencia)
    }

    @Test fun sessionOwnershipAndMissingAnimalExposeNoForeignData() = runTest(main.testDispatcher) {
        val foreignTemplate = template().copy(titulo = "Ajena", idUsuario = 8L)
        val noSession = Fixture(userId = null, template = foreignTemplate)
        advanceUntilIdle()
        assertEquals(AnimalSchedulesContent.Error("No hay una sesión activa."), noSession.vm.uiState.value)
        assertTrue(noSession.listed.animalIds.isEmpty())

        val missing = Fixture(animal = null, template = foreignTemplate)
        advanceUntilIdle()
        assertEquals(AnimalSchedulesContent.Error("No se encontró el animal."), missing.vm.uiState.value)
        assertTrue(missing.listed.animalIds.isEmpty())

        val foreignAnimal = animal().copy(idUsuario = 8L, nombre = "Ajeno")
        val mismatch = Fixture(animal = foreignAnimal, template = foreignTemplate)
        advanceUntilIdle()
        val error = mismatch.vm.uiState.value as AnimalSchedulesContent.Error
        assertEquals(AnimalSchedulesErrorMessage, error.message)
        assertFalse(error.message.contains("Ajena"))
        assertFalse(error.message.contains("Ajeno"))
        assertTrue(mismatch.listed.animalIds.isEmpty())
    }

    @Test fun backInvokesCallbackWithoutNavigationState() = runTest(main.testDispatcher) {
        var backed = 0
        leaveAnimalSchedules { backed++ }
        assertEquals(1, backed)
        val f = Fixture()
        advanceUntilIdle()
        assertTrue(f.vm.uiState.value is AnimalSchedulesContent.Success)
    }
}

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

private fun template() = PlantillaNotificacion(5L, "Vacuna aftosa", TipoNotificacion.VACUNACION, 7L)

private fun schedule(
    id: Long,
    hora: LocalTime = LocalTime.of(8, 0),
    recurrencia: RecurrenciaNotificacion = RecurrenciaNotificacion.DIARIA,
) = ProgramacionNotificacion(
    idProgramacion = id,
    idPlantilla = 5L,
    idAnimal = 9L,
    fechaInicio = LocalDate.of(2026, 11, 15),
    fechaFin = LocalDate.of(2026, 11, 30),
    hora = hora,
    recurrencia = recurrencia,
)

private class AnimalReadProbe(private val animal: Animal?) : AnimalRepository {
    var reads = 0
    override fun getAllAnimals(userId: Long): Flow<List<Animal>> = flowOf(emptyList())
    override fun getAnimalsByUser(userId: Long): Flow<List<Animal>> = flowOf(emptyList())
    override fun getFavoriteAnimalsByUser(userId: Long): Flow<List<Animal>> = flowOf(emptyList())
    override suspend fun getAnimalById(id: Long): Animal? {
        reads++
        return animal?.takeIf { it.idAnimal == id }
    }
    override suspend fun insertAnimal(animal: Animal): Long = 0L
    override suspend fun updateAnimal(animal: Animal): Boolean = false
    override suspend fun deleteAnimal(id: Long) = Unit
}

private class ScheduleReadProbe(
    private val schedules: List<ProgramacionNotificacion>,
    private val fail: Boolean,
) : NotificationScheduleRepository {
    val animalIds = mutableListOf<Long>()
    override suspend fun insertSchedule(schedule: ProgramacionNotificacion): Long = 0L
    override suspend fun getScheduleById(scheduleId: Long): ProgramacionNotificacion? = null
    override suspend fun updateSchedule(schedule: ProgramacionNotificacion): Boolean = false
    override suspend fun deleteSchedule(scheduleId: Long) = Unit
    override suspend fun getSchedulesByAnimal(animalId: Long): List<ProgramacionNotificacion> {
        animalIds += animalId
        if (fail) throw IllegalStateException("list")
        return schedules
    }
    override suspend fun getSchedulesByTemplate(templateId: Long): List<ProgramacionNotificacion> = emptyList()
}

private class TemplateReadProbe(private val template: PlantillaNotificacion) : NotificationTemplateRepository {
    override fun getTemplatesByUser(userId: Long): Flow<List<PlantillaNotificacion>> = flowOf(emptyList())
    override suspend fun getTemplateById(templateId: Long): PlantillaNotificacion? =
        template.takeIf { it.idPlantilla == templateId }
    override suspend fun insertTemplate(template: PlantillaNotificacion): Long = 0L
    override suspend fun updateTemplate(template: PlantillaNotificacion): Boolean = false
    override suspend fun deleteTemplate(templateId: Long) = Unit
    override suspend fun hasSchedules(templateId: Long): Boolean = false
}
