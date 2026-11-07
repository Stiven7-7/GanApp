package com.proyecto.ganapp.ui.features.notifications.templates

import androidx.lifecycle.ViewModelStore
import com.proyecto.ganapp.domain.model.PlantillaNotificacion
import com.proyecto.ganapp.domain.model.TipoNotificacion
import com.proyecto.ganapp.domain.repository.NotificationTemplateRepository
import com.proyecto.ganapp.domain.usecase.notificacion.*
import com.proyecto.ganapp.testutil.MainDispatcherRule
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationTemplatesViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val stores = mutableListOf<ViewModelStore>()
    @After fun clearViewModels() { stores.forEach { it.clear() } }

    private inner class Fixture(initial: List<PlantillaNotificacion> = listOf(templateFixture())) {
        val probe = TemplateRepositoryProbe()
        val session = TemplateSessionProbe()
        val rows = MutableStateFlow(initial)
        var insertGate: CompletableDeferred<Unit>? = null
        var updateGate: CompletableDeferred<Unit>? = null
        var deleteGate: CompletableDeferred<Unit>? = null
        var insertAttempts = 0
        var updateAttempts = 0
        var deleteAttempts = 0
        val repository = object : NotificationTemplateRepository by probe {
            override suspend fun insertTemplate(template: PlantillaNotificacion): Long {
                insertAttempts++
                insertGate?.await()
                val id = probe.insertTemplate(template)
                rows.value += template.copy(idPlantilla = id)
                return id
            }
            override suspend fun updateTemplate(template: PlantillaNotificacion): Boolean {
                updateAttempts++
                updateGate?.await()
                val result = probe.updateTemplate(template)
                if (result) rows.value = rows.value.map { if (it.idPlantilla == template.idPlantilla) template else it }
                return result
            }
            override suspend fun deleteTemplate(templateId: Long) {
                deleteAttempts++
                deleteGate?.await()
                probe.deleteTemplate(templateId)
                rows.value = rows.value.filterNot { it.idPlantilla == templateId }
            }
        }
        init { probe.observed[7L] = rows }
        val vm = NotificationTemplatesViewModel(
            GetNotificationTemplatesUseCase(repository, session),
            CreateNotificationTemplateUseCase(repository, session),
            GetNotificationTemplateByIdUseCase(repository, session),
            UpdateNotificationTemplateUseCase(repository, session),
            DeleteNotificationTemplateUseCase(repository, session),
        ).also { vm -> stores += ViewModelStore().apply { put("templates", vm) } }
    }

    @Test fun loadStartsLoadingThenSuccessAndRemainsReactive() = runTest(main.testDispatcher) {
        val f = Fixture()
        assertEquals(TemplateListState.Loading, f.vm.uiState.value.content)
        advanceUntilIdle()
        assertEquals(TemplateListState.Success(listOf(templateFixture())), f.vm.uiState.value.content)
        f.rows.value = listOf(templateFixture(id = 22L))
        advanceUntilIdle()
        assertEquals(TemplateListState.Success(f.rows.value), f.vm.uiState.value.content)
    }

    @Test fun emptyLoad() = runTest(main.testDispatcher) {
        val f = Fixture(emptyList())
        advanceUntilIdle()
        assertEquals(TemplateListState.Empty, f.vm.uiState.value.content)
    }

    @Test fun loadFailureAndRetry() = runTest(main.testDispatcher) {
        val f = Fixture()
        f.probe.failingMethod = "list"
        advanceUntilIdle()
        assertTrue(f.vm.uiState.value.content is TemplateListState.Error)
        f.probe.failingMethod = null
        f.vm.load()
        assertEquals(TemplateListState.Loading, f.vm.uiState.value.content)
        advanceUntilIdle()
        assertTrue(f.vm.uiState.value.content is TemplateListState.Success)
    }

    @Test fun createSuccessUsesEnumAndClosesEditor() = runTest(main.testDispatcher) {
        val f = Fixture(emptyList())
        advanceUntilIdle()
        f.vm.openCreate()
        f.vm.changeTitle(" Nueva ")
        f.vm.changeType(TipoNotificacion.ALIMENTACION)
        f.vm.save()
        advanceUntilIdle()
        assertNull(f.vm.uiState.value.editor)
        assertFalse(f.vm.uiState.value.isWorking)
        assertEquals("Nueva", f.probe.inserted.single().titulo)
        assertEquals(TipoNotificacion.ALIMENTACION, f.probe.inserted.single().tipo)
        assertTrue(f.vm.uiState.value.content is TemplateListState.Success)
    }

    @Test fun createFailureKeepsFormAndEmitsMessage() = runTest(main.testDispatcher) {
        val f = Fixture()
        val messages = mutableListOf<TemplateEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { f.vm.events.collect { messages += it } }
        advanceUntilIdle()
        f.vm.openCreate()
        f.vm.save()
        advanceUntilIdle()
        assertNotNull(f.vm.uiState.value.editor)
        assertTrue(f.probe.inserted.isEmpty())
        assertEquals(listOf(TemplateEvent.Message("Escribe un título para la plantilla.")), messages)
    }

    @Test fun createPersistenceFailureKeepsEditor() = runTest(main.testDispatcher) {
        val f = Fixture()
        advanceUntilIdle()
        f.probe.failingMethod = "insert"
        f.vm.openCreate()
        f.vm.changeTitle("Título")
        f.vm.save()
        advanceUntilIdle()
        assertNotNull(f.vm.uiState.value.editor)
        assertFalse(f.vm.uiState.value.isWorking)
    }

    @Test fun updateSuccessLoadsExistingAndSavesChanges() = runTest(main.testDispatcher) {
        val f = Fixture()
        advanceUntilIdle()
        f.vm.openEdit(11L)
        advanceUntilIdle()
        assertEquals(TemplateEditor(11L, TemplateForm("Vacuna", TipoNotificacion.VACUNACION)), f.vm.uiState.value.editor)
        f.vm.changeTitle("Editada")
        f.vm.changeType(TipoNotificacion.CICLO_REPRODUCTIVO)
        f.vm.save()
        advanceUntilIdle()
        assertNull(f.vm.uiState.value.editor)
        assertEquals(templateFixture().copy(titulo = "Editada", tipo = TipoNotificacion.CICLO_REPRODUCTIVO), f.probe.updated.single())
    }

    @Test fun updateFailureKeepsEditorAndEmitsMessage() = runTest(main.testDispatcher) {
        val f = Fixture()
        val messages = mutableListOf<TemplateEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { f.vm.events.collect { messages += it } }
        advanceUntilIdle()
        f.vm.openEdit(11L)
        advanceUntilIdle()
        f.probe.updateResult = false
        f.vm.save()
        advanceUntilIdle()
        assertNotNull(f.vm.uiState.value.editor)
        assertEquals(listOf(TemplateEvent.Message("No se pudieron guardar los cambios.")), messages)
    }

    @Test fun failedOpenEditShowsNoEditor() = runTest(main.testDispatcher) {
        val f = Fixture()
        advanceUntilIdle()
        f.probe.current = null
        f.vm.openEdit(11L)
        advanceUntilIdle()
        assertNull(f.vm.uiState.value.editor)
        assertFalse(f.vm.uiState.value.isWorking)
    }

    @Test fun requestAndCancelNeverDelete() = runTest(main.testDispatcher) {
        val f = Fixture()
        advanceUntilIdle()
        f.vm.requestDelete(templateFixture())
        assertEquals(templateFixture(), f.vm.uiState.value.deleteTarget)
        assertTrue(f.probe.deleted.isEmpty())
        f.vm.dismissDelete()
        f.vm.confirmDelete()
        advanceUntilIdle()
        assertNull(f.vm.uiState.value.deleteTarget)
        assertTrue(f.probe.deleted.isEmpty())
        assertEquals(0, f.deleteAttempts)
    }

    @Test fun confirmedDeleteSuccessUpdatesList() = runTest(main.testDispatcher) {
        val f = Fixture()
        advanceUntilIdle()
        f.vm.requestDelete(templateFixture())
        f.vm.confirmDelete()
        advanceUntilIdle()
        assertEquals(listOf(11L), f.probe.deleted)
        assertNull(f.vm.uiState.value.deleteTarget)
        assertEquals(TemplateListState.Empty, f.vm.uiState.value.content)
    }

    @Test fun deleteInUseEmitsExactMessageAndKeepsTemplate() = runTest(main.testDispatcher) {
        val f = Fixture()
        val messages = mutableListOf<TemplateEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { f.vm.events.collect { messages += it } }
        advanceUntilIdle()
        f.probe.inUse = true
        f.vm.requestDelete(templateFixture())
        f.vm.confirmDelete()
        advanceUntilIdle()
        assertTrue(f.probe.deleted.isEmpty())
        assertEquals(TemplateListState.Success(listOf(templateFixture())), f.vm.uiState.value.content)
        assertEquals(listOf(TemplateEvent.Message("No se puede eliminar porque la plantilla está en uso.")), messages)
        assertFalse(f.vm.uiState.value.isWorking)
    }

    @Test fun deleteFailureKeepsConfirmationAndTemplate() = runTest(main.testDispatcher) {
        val f = Fixture()
        advanceUntilIdle()
        f.probe.failingMethod = "delete:11"
        f.vm.requestDelete(templateFixture())
        f.vm.confirmDelete()
        advanceUntilIdle()
        assertNotNull(f.vm.uiState.value.deleteTarget)
        assertTrue(f.probe.deleted.isEmpty())
    }

    @Test fun doubleCreateUpdateDeleteSubmitsAreBlocked() = runTest(main.testDispatcher) {
        for (mode in listOf("create", "update", "delete")) {
            val f = Fixture()
            advanceUntilIdle()
            val gate = CompletableDeferred<Unit>()
            when (mode) {
                "create" -> {
                    f.insertGate = gate
                    f.vm.openCreate()
                    f.vm.changeTitle("Nueva")
                    f.vm.save(); f.vm.save()
                }
                "update" -> {
                    f.vm.openEdit(11L)
                    advanceUntilIdle()
                    f.updateGate = gate
                    f.vm.save(); f.vm.save()
                }
                else -> {
                    f.deleteGate = gate
                    f.vm.requestDelete(templateFixture())
                    f.vm.confirmDelete(); f.vm.confirmDelete()
                }
            }
            advanceUntilIdle()
            assertTrue(f.vm.uiState.value.isWorking)
            assertEquals(1, f.insertAttempts + f.updateAttempts + f.deleteAttempts)
            gate.complete(Unit)
            advanceUntilIdle()
            assertFalse(f.vm.uiState.value.isWorking)
        }
    }

    @Test fun messageIsNotReplayedToNewCollector() = runTest(main.testDispatcher) {
        val f = Fixture()
        val first = mutableListOf<TemplateEvent>()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { f.vm.events.collect { first += it } }
        advanceUntilIdle()
        f.vm.openCreate()
        f.vm.save()
        advanceUntilIdle()
        assertEquals(1, first.size)
        collector.cancel()
        val second = mutableListOf<TemplateEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { f.vm.events.collect { second += it } }
        advanceUntilIdle()
        assertTrue(second.isEmpty())
    }

    @Test fun formHasOnlyTitleAndEnumTypeAndScreenHasNoSchedulingInputs() {
        assertEquals(setOf("titulo", "tipo"), TemplateForm::class.java.declaredFields.filterNot { java.lang.reflect.Modifier.isStatic(it.modifiers) }.map { it.name }.toSet())
        val relative = "src/main/java/com/proyecto/ganapp/ui/features/notifications/templates/NotificationTemplatesScreen.kt"
        val source = listOf(File(relative), File("app/$relative")).first { it.exists() }.readText()
        assertTrue(source.contains("Text(\"Título\")"))
        assertTrue(Regex("""Text\(\s*"Tipo"\s*[,)]""").containsMatchIn(source))
        assertEquals(1, Regex("OutlinedTextField\\(").findAll(source).count())
        for (forbidden in listOf("fecha", "hora", "recurrencia", "idAnimal", "DatePicker", "TimePicker")) {
            assertFalse("Unexpected scheduling input: $forbidden", source.contains(forbidden, ignoreCase = true))
        }
    }
}
