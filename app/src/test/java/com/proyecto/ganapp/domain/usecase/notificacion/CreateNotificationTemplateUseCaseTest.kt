package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.TipoNotificacion
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class CreateNotificationTemplateUseCaseTest {
    private fun request(title: String = "  Vacuna Mixta  ") =
        CreateNotificationTemplateRequest(title, TipoNotificacion.VACUNACION)

    @Test
    fun sinSesionOCeroONegativo_noConsultaNiInserta() = runTest {
        listOf(null, 0L, -1L).forEach { user ->
            val repository = TemplateRepositoryProbe()
            val result = CreateNotificationTemplateUseCase(repository, TemplateSessionProbe(user))(request())
            assertEquals(CreateNotificationTemplateResult.NoAuthenticatedUser, result)
            assertTrue(repository.calls.isEmpty())
        }
    }

    @Test
    fun tituloInvalido_noInserta() = runTest {
        listOf("", "   ").forEach { title ->
            val repository = TemplateRepositoryProbe()
            val result = CreateNotificationTemplateUseCase(repository, TemplateSessionProbe())(request(title))
            assertEquals(CreateNotificationTemplateResult.InvalidInput(TemplateValidationError.INVALID_TITLE), result)
            assertTrue(repository.calls.isEmpty())
        }
    }

    @Test
    fun createNormalizaTituloUsaSesionYExigeIdReal() = runTest {
        val repository = TemplateRepositoryProbe()
        val result = CreateNotificationTemplateUseCase(repository, TemplateSessionProbe(9L))(request())
        assertEquals(CreateNotificationTemplateResult.Success(21L), result)
        assertEquals(listOf("insert"), repository.calls)
        val inserted = repository.inserted.single()
        assertEquals(0L, inserted.idPlantilla)
        assertEquals(9L, inserted.idUsuario)
        assertEquals("Vacuna Mixta", inserted.titulo)
        assertEquals(TipoNotificacion.VACUNACION, inserted.tipo)
    }

    @Test
    fun duplicadosSeInsertanSinConsultarExistencia() = runTest {
        val repository = TemplateRepositoryProbe()
        val useCase = CreateNotificationTemplateUseCase(repository, TemplateSessionProbe())
        repeat(2) { assertEquals(CreateNotificationTemplateResult.Success(21L), useCase(request())) }
        assertEquals(listOf("insert", "insert"), repository.calls)
        assertEquals(2, repository.inserted.size)
        assertEquals(repository.inserted[0], repository.inserted[1])
    }

    @Test
    fun idInsertadoCeroONegativo_noEsSuccess() = runTest {
        listOf(0L, -1L).forEach { id ->
            val repository = TemplateRepositoryProbe().apply { insertId = id }
            assertTrue(CreateNotificationTemplateUseCase(repository, TemplateSessionProbe())(request())
                is CreateNotificationTemplateResult.Failure)
        }
    }

    @Test
    fun errorDeRepositoryConservaCausa() = runTest {
        val repository = TemplateRepositoryProbe().apply { failingMethod = "insert" }
        val result = CreateNotificationTemplateUseCase(repository, TemplateSessionProbe())(request())
        assertSame(repository.failure, (result as CreateNotificationTemplateResult.Failure).cause)
    }

    @Test
    fun errorDeSesionEsFailureSinRepository() = runTest {
        val error = IllegalStateException("Session failure")
        val session = TemplateSessionProbe().apply { failure = error }
        val repository = TemplateRepositoryProbe()
        val result = CreateNotificationTemplateUseCase(repository, session)(request())
        assertSame(error, (result as CreateNotificationTemplateResult.Failure).cause)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun cancelacionDeSesionOInsertSePropaga() = runTest {
        val cancellation = CancellationException("cancel")
        val session = TemplateSessionProbe().apply { failure = cancellation }
        val repository = TemplateRepositoryProbe()
        assertTemplateCancellation(cancellation) {
            CreateNotificationTemplateUseCase(repository, session)(request())
        }
        assertTrue(repository.calls.isEmpty())
        repository.failingMethod = "insert"
        repository.failure = cancellation
        assertTemplateCancellation(cancellation) {
            CreateNotificationTemplateUseCase(repository, TemplateSessionProbe())(request())
        }
    }
}
