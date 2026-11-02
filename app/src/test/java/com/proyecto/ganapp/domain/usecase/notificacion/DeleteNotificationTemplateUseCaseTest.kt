package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.TipoNotificacion
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class DeleteNotificationTemplateUseCaseTest {
    @Test
    fun idInvalidoNoConsultaSesionNiRepository() = runTest {
        listOf(0L, -1L).forEach { id ->
            val session = TemplateSessionProbe()
            val repository = TemplateRepositoryProbe()
            assertEquals(DeleteNotificationTemplateResult.InvalidTemplateId,
                DeleteNotificationTemplateUseCase(repository, session)(id))
            assertEquals(0, session.observationCalls)
            assertTrue(repository.calls.isEmpty())
        }
    }

    @Test
    fun sinSesionOCeroONegativo_noElimina() = runTest {
        listOf(null, 0L, -1L).forEach { user ->
            val repository = TemplateRepositoryProbe()
            assertEquals(DeleteNotificationTemplateResult.NoAuthenticatedUser,
                DeleteNotificationTemplateUseCase(repository, TemplateSessionProbe(user))(11L))
            assertTrue(repository.calls.isEmpty())
        }
    }

    @Test
    fun ausenteNoConsultaProgramacionesNiElimina() = runTest {
        val repository = TemplateRepositoryProbe().apply { current = null }
        assertEquals(DeleteNotificationTemplateResult.TemplateNotFound,
            DeleteNotificationTemplateUseCase(repository, TemplateSessionProbe())(11L))
        assertEquals(listOf("get:11"), repository.calls)
    }

    @Test
    fun ownerAjenoNoConsultaProgramacionesNiElimina() = runTest {
        val repository = TemplateRepositoryProbe().apply { current = templateFixture(owner = 8L) }
        assertEquals(DeleteNotificationTemplateResult.OwnershipMismatch,
            DeleteNotificationTemplateUseCase(repository, TemplateSessionProbe())(11L))
        assertEquals(listOf("get:11"), repository.calls)
        assertTrue(repository.deleted.isEmpty())
    }

    @Test
    fun plantillaEnUsoEsInUseSinDelete() = runTest {
        val repository = TemplateRepositoryProbe().apply { inUse = true }
        assertEquals(DeleteNotificationTemplateResult.InUse,
            DeleteNotificationTemplateUseCase(repository, TemplateSessionProbe())(11L))
        assertEquals(listOf("get:11", "has:11"), repository.calls)
        assertTrue(repository.deleted.isEmpty())
    }

    @Test
    fun plantillaSinProgramacionesSeEliminaTrasComprobarOwnershipYUso() = runTest {
        val repository = TemplateRepositoryProbe()
        assertEquals(DeleteNotificationTemplateResult.Success,
            DeleteNotificationTemplateUseCase(repository, TemplateSessionProbe())(11L))
        assertEquals(listOf("get:11", "has:11", "delete:11"), repository.calls)
        assertEquals(listOf(11L), repository.deleted)
    }

    @Test
    fun erroresEnLecturaOHasSchedulesNoEjecutanDelete() = runTest {
        listOf("get:11", "has:11").forEach { method ->
            val repository = TemplateRepositoryProbe().apply { failingMethod = method }
            val result = DeleteNotificationTemplateUseCase(repository, TemplateSessionProbe())(11L)
            assertSame(repository.failure, (result as DeleteNotificationTemplateResult.Failure).cause)
            assertFalse(repository.calls.contains("delete:11"))
            assertTrue(repository.deleted.isEmpty())
        }
    }

    @Test
    fun errorEnDeleteEsFailureConCausa() = runTest {
        val repository = TemplateRepositoryProbe().apply { failingMethod = "delete:11" }
        val result = DeleteNotificationTemplateUseCase(repository, TemplateSessionProbe())(11L)
        assertSame(repository.failure, (result as DeleteNotificationTemplateResult.Failure).cause)
    }

    @Test
    fun errorDeSesionEsFailureSinRepository() = runTest {
        val error = IllegalStateException("Session failure")
        val session = TemplateSessionProbe().apply { failure = error }
        val repository = TemplateRepositoryProbe()
        val result = DeleteNotificationTemplateUseCase(repository, session)(11L)
        assertSame(error, (result as DeleteNotificationTemplateResult.Failure).cause)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun cancelacionEnSesionLecturaHasSchedulesYDeleteSePropaga() = runTest {
        val cancellation = CancellationException("cancel")
        val session = TemplateSessionProbe().apply { failure = cancellation }
        assertTemplateCancellation(cancellation) {
            DeleteNotificationTemplateUseCase(TemplateRepositoryProbe(), session)(11L)
        }
        listOf("get:11", "has:11", "delete:11").forEach { method ->
            val repository = TemplateRepositoryProbe().apply {
                failingMethod = method; failure = cancellation
            }
            assertTemplateCancellation(cancellation) {
                DeleteNotificationTemplateUseCase(repository, TemplateSessionProbe())(11L)
            }
        }
    }
}
