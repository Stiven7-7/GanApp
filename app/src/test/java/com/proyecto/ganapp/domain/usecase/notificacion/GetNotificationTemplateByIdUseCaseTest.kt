package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.TipoNotificacion
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class GetNotificationTemplateByIdUseCaseTest {
    @Test
    fun idInvalidoNoConsultaSesionNiRepository() = runTest {
        listOf(0L, -1L).forEach { id ->
            val session = TemplateSessionProbe()
            val repository = TemplateRepositoryProbe()
            assertEquals(GetNotificationTemplateByIdResult.InvalidTemplateId,
                GetNotificationTemplateByIdUseCase(repository, session)(id))
            assertEquals(0, session.observationCalls)
            assertTrue(repository.calls.isEmpty())
        }
    }

    @Test
    fun sinSesionOCeroONegativo_noConsultaRepository() = runTest {
        listOf(null, 0L, -1L).forEach { user ->
            val repository = TemplateRepositoryProbe()
            assertEquals(GetNotificationTemplateByIdResult.NoAuthenticatedUser,
                GetNotificationTemplateByIdUseCase(repository, TemplateSessionProbe(user))(11L))
            assertTrue(repository.calls.isEmpty())
        }
    }

    @Test
    fun plantillaAusenteEsNotFound() = runTest {
        val repository = TemplateRepositoryProbe().apply { current = null }
        assertEquals(GetNotificationTemplateByIdResult.TemplateNotFound,
            GetNotificationTemplateByIdUseCase(repository, TemplateSessionProbe())(11L))
        assertEquals(listOf("get:11"), repository.calls)
    }

    @Test
    fun plantillaPropiaEsSuccess() = runTest {
        val repository = TemplateRepositoryProbe()
        assertEquals(GetNotificationTemplateByIdResult.Success(templateFixture()),
            GetNotificationTemplateByIdUseCase(repository, TemplateSessionProbe())(11L))
        assertEquals(listOf("get:11"), repository.calls)
    }

    @Test
    fun plantillaAjenaNoSeExpone() = runTest {
        val repository = TemplateRepositoryProbe().apply { current = templateFixture(owner = 8L) }
        assertEquals(GetNotificationTemplateByIdResult.OwnershipMismatch,
            GetNotificationTemplateByIdUseCase(repository, TemplateSessionProbe())(11L))
    }

    @Test
    fun erroresDeSesionYRepositorySonFailureConCausa() = runTest {
        val repository = TemplateRepositoryProbe().apply { failingMethod = "get:11" }
        val result = GetNotificationTemplateByIdUseCase(repository, TemplateSessionProbe())(11L)
        assertSame(repository.failure, (result as GetNotificationTemplateByIdResult.Failure).cause)
        val error = IllegalStateException("Session failure")
        val session = TemplateSessionProbe().apply { failure = error }
        assertSame(error, (GetNotificationTemplateByIdUseCase(TemplateRepositoryProbe(), session)(11L)
            as GetNotificationTemplateByIdResult.Failure).cause)
    }

    @Test
    fun cancelacionDeSesionYLecturaSePropaga() = runTest {
        val cancellation = CancellationException("cancel")
        val session = TemplateSessionProbe().apply { failure = cancellation }
        assertTemplateCancellation(cancellation) {
            GetNotificationTemplateByIdUseCase(TemplateRepositoryProbe(), session)(11L)
        }
        val repository = TemplateRepositoryProbe().apply {
            failingMethod = "get:11"; failure = cancellation
        }
        assertTemplateCancellation(cancellation) {
            GetNotificationTemplateByIdUseCase(repository, TemplateSessionProbe())(11L)
        }
    }
}
