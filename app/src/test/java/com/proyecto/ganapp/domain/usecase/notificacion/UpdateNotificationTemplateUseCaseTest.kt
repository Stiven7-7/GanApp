package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.TipoNotificacion
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class UpdateNotificationTemplateUseCaseTest {
    private fun request(id: Long = 11L, title: String = "  Alimentación Mixta  ") =
        UpdateNotificationTemplateRequest(id, title, TipoNotificacion.ALIMENTACION)

    @Test
    fun requestInvalidoNoConsultaSesionNiRepository() = runTest {
        listOf(request(id = 0L), request(id = -1L), request(title = ""), request(title = "   "))
            .forEach { request ->
                val session = TemplateSessionProbe()
                val repository = TemplateRepositoryProbe()
                val result = UpdateNotificationTemplateUseCase(repository, session)(request)
                val error = if (request.templateId <= 0L) TemplateValidationError.INVALID_TEMPLATE_ID
                    else TemplateValidationError.INVALID_TITLE
                assertEquals(UpdateNotificationTemplateResult.InvalidInput(error), result)
                assertEquals(0, session.observationCalls)
                assertTrue(repository.calls.isEmpty())
            }
    }

    @Test
    fun sinSesionOCeroONegativo_noActualiza() = runTest {
        listOf(null, 0L, -1L).forEach { user ->
            val repository = TemplateRepositoryProbe()
            assertEquals(UpdateNotificationTemplateResult.NoAuthenticatedUser,
                UpdateNotificationTemplateUseCase(repository, TemplateSessionProbe(user))(request()))
            assertTrue(repository.calls.isEmpty())
        }
    }

    @Test
    fun ausenteNoActualiza() = runTest {
        val repository = TemplateRepositoryProbe().apply { current = null }
        assertEquals(UpdateNotificationTemplateResult.TemplateNotFound,
            UpdateNotificationTemplateUseCase(repository, TemplateSessionProbe())(request()))
        assertEquals(listOf("get:11"), repository.calls)
        assertTrue(repository.updated.isEmpty())
    }

    @Test
    fun ownerAjenoNoActualiza() = runTest {
        val repository = TemplateRepositoryProbe().apply { current = templateFixture(owner = 9L) }
        assertEquals(UpdateNotificationTemplateResult.OwnershipMismatch,
            UpdateNotificationTemplateUseCase(repository, TemplateSessionProbe())(request()))
        assertEquals(listOf("get:11"), repository.calls)
        assertTrue(repository.updated.isEmpty())
    }

    @Test
    fun successConservaIdentidadYOwnerCambiaSoloTituloNormalizadoYTipo() = runTest {
        val repository = TemplateRepositoryProbe()
        assertEquals(UpdateNotificationTemplateResult.Success,
            UpdateNotificationTemplateUseCase(repository, TemplateSessionProbe())(request()))
        assertEquals(templateFixture().copy(
            titulo = "Alimentación Mixta", tipo = TipoNotificacion.ALIMENTACION,
        ), repository.updated.single())
        assertEquals(listOf("get:11", "update"), repository.calls)
    }

    @Test
    fun updateFalseEsFailure() = runTest {
        val repository = TemplateRepositoryProbe().apply { updateResult = false }
        assertTrue(UpdateNotificationTemplateUseCase(repository, TemplateSessionProbe())(request())
            is UpdateNotificationTemplateResult.Failure)
    }

    @Test
    fun erroresEnLecturaYUpdateConservanCausa() = runTest {
        listOf("get:11", "update").forEach { method ->
            val repository = TemplateRepositoryProbe().apply { failingMethod = method }
            val result = UpdateNotificationTemplateUseCase(repository, TemplateSessionProbe())(request())
            assertSame(repository.failure, (result as UpdateNotificationTemplateResult.Failure).cause)
        }
    }

    @Test
    fun errorDeSesionEsFailureSinRepository() = runTest {
        val error = IllegalStateException("Session failure")
        val repository = TemplateRepositoryProbe()
        val session = TemplateSessionProbe().apply { failure = error }
        val result = UpdateNotificationTemplateUseCase(repository, session)(request())
        assertSame(error, (result as UpdateNotificationTemplateResult.Failure).cause)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun cancelacionEnSesionLecturaYUpdateSePropaga() = runTest {
        val cancellation = CancellationException("cancel")
        val session = TemplateSessionProbe().apply { failure = cancellation }
        assertTemplateCancellation(cancellation) {
            UpdateNotificationTemplateUseCase(TemplateRepositoryProbe(), session)(request())
        }
        listOf("get:11", "update").forEach { method ->
            val repository = TemplateRepositoryProbe().apply {
                failingMethod = method; failure = cancellation
            }
            assertTemplateCancellation(cancellation) {
                UpdateNotificationTemplateUseCase(repository, TemplateSessionProbe())(request())
            }
        }
    }
}
