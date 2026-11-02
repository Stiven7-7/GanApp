package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.PlantillaNotificacion
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GetNotificationTemplatesUseCaseTest {
    @Test
    fun sinSesionOCeroONegativo_noExponeDatosNiConsultaRepository() = runTest {
        listOf(null, 0L, -1L).forEach { user ->
            val repository = TemplateRepositoryProbe().apply { templates = listOf(templateFixture()) }
            val results = mutableListOf<List<PlantillaNotificacion>>()
            val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                GetNotificationTemplatesUseCase(repository, TemplateSessionProbe(user))().toList(results)
            }
            runCurrent()
            assertEquals(listOf(emptyList<PlantillaNotificacion>()), results)
            assertTrue(repository.calls.isEmpty())
            job.cancel()
        }
    }

    @Test
    fun listaVaciaEsValidaYConsultaOwnerDeSesion() = runTest {
        val repository = TemplateRepositoryProbe()
        val results = mutableListOf<List<PlantillaNotificacion>>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            GetNotificationTemplatesUseCase(repository, TemplateSessionProbe(7L))().toList(results)
        }
        runCurrent()
        assertEquals(listOf(7L), repository.queriedUsers)
        assertEquals(listOf(emptyList<PlantillaNotificacion>()), results)
        job.cancel()
    }

    @Test
    fun preservaPlantillasPropiasYFiltraDatosAjenosDelRepository() = runTest {
        val own = listOf(templateFixture(), templateFixture(id = 12L))
        val repository = TemplateRepositoryProbe().apply {
            templates = own + templateFixture(owner = 8L, id = 13L)
        }
        val results = mutableListOf<List<PlantillaNotificacion>>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            GetNotificationTemplatesUseCase(repository, TemplateSessionProbe())().toList(results)
        }
        runCurrent()
        assertEquals(own, results.last())
        assertEquals(listOf(7L), repository.queriedUsers)
        job.cancel()
    }

    @Test
    fun cambioDeUsuarioYLogoutRetiranDatosYCancelanLaConsultaAnterior() = runTest {
        val first = listOf(templateFixture())
        val second = listOf(templateFixture(owner = 8L, id = 20L))
        val firstSource = MutableStateFlow(first)
        val secondSource = MutableStateFlow(second)
        val repository = TemplateRepositoryProbe().apply {
            observed[7L] = firstSource
            observed[8L] = secondSource
        }
        val session = TemplateSessionProbe()
        val results = mutableListOf<List<PlantillaNotificacion>>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            GetNotificationTemplatesUseCase(repository, session)().toList(results)
        }
        runCurrent()
        assertEquals(first, results.last())
        session.user.value = 8L
        runCurrent()
        assertEquals(second, results.last())
        assertEquals(listOf(first, emptyList(), second), results.takeLast(3))
        val countAfterSwitch = results.size
        firstSource.value = listOf(templateFixture(id = 99L))
        runCurrent()
        assertEquals(countAfterSwitch, results.size)
        session.user.value = null
        runCurrent()
        assertTrue(results.last().isEmpty())
        val countAfterLogout = results.size
        secondSource.value = listOf(templateFixture(owner = 8L, id = 21L))
        runCurrent()
        assertEquals(countAfterLogout, results.size)
        assertEquals(listOf(7L, 8L), repository.queriedUsers)
        job.cancel()
    }

    @Test
    fun emisionesPosterioresDePlantillasPropiasSePropagan() = runTest {
        val source = MutableStateFlow<List<PlantillaNotificacion>>(emptyList())
        val repository = TemplateRepositoryProbe().apply { observed[7L] = source }
        val results = mutableListOf<List<PlantillaNotificacion>>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            GetNotificationTemplatesUseCase(repository, TemplateSessionProbe())().toList(results)
        }
        runCurrent()
        source.value = listOf(templateFixture())
        runCurrent()
        assertEquals(listOf(templateFixture()), results.last())
        job.cancel()
    }

    @Test
    fun erroresDeSesionYRepositorySePropaganComoErroresDeFlow() = runTest {
        val error = IllegalStateException("Session failure")
        val session = TemplateSessionProbe().apply { failure = error }
        try {
            GetNotificationTemplatesUseCase(TemplateRepositoryProbe(), session)().collect()
            fail("Expected session failure")
        } catch (actual: IllegalStateException) {
            assertPropagated(error, actual)
        }
        val repository = TemplateRepositoryProbe().apply { failingMethod = "list" }
        try {
            GetNotificationTemplatesUseCase(repository, TemplateSessionProbe())().collect()
            fail("Expected repository failure")
        } catch (actual: IllegalStateException) {
            assertPropagated(repository.failure, actual)
        }
    }

    @Test
    fun cancelacionDeSesionSePropaga() = runTest {
        val cancellation = CancellationException("cancel")
        val session = TemplateSessionProbe().apply { failure = cancellation }
        try {
            GetNotificationTemplatesUseCase(TemplateRepositoryProbe(), session)().collect()
            fail("Expected cancellation")
        } catch (actual: CancellationException) {
            assertPropagated(cancellation, actual)
        }
    }

    @Test
    fun cancelarElCollectorCancelaLasSuscripcionesDeSesionYRepository() = runTest {
        val source = MutableStateFlow(listOf(templateFixture()))
        val repository = TemplateRepositoryProbe().apply { observed[7L] = source }
        val session = TemplateSessionProbe()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            GetNotificationTemplatesUseCase(repository, session)().collect()
        }
        runCurrent()
        assertEquals(1, session.user.subscriptionCount.value)
        assertEquals(1, source.subscriptionCount.value)
        job.cancel(CancellationException("collector cancelled"))
        runCurrent()
        assertTrue(job.isCancelled)
        assertEquals(0, session.user.subscriptionCount.value)
        assertEquals(0, source.subscriptionCount.value)
    }
    private fun assertPropagated(expected: Throwable, actual: Throwable) {
        assertEquals(expected.javaClass, actual.javaClass)
        assertEquals(expected.message, actual.message)
        // La recuperación de stacktrace de coroutines puede copiar la excepción.
        assertTrue(actual === expected || actual.cause === expected)
    }
}
