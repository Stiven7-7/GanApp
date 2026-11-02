package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.ProgramacionNotificacionDetalle
import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import com.proyecto.ganapp.domain.model.TipoNotificacion
import com.proyecto.ganapp.domain.service.ScheduleAlarmCancelResult
import com.proyecto.ganapp.domain.service.ScheduleAlarmResult
import java.time.LocalTime
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GetNotificationScheduleByIdUseCaseTest {
    @Test
    fun idInvalido_noConsultaSesionNiRepositories() = runTest {
        listOf(0L, -1L).forEach { id ->
            val f = ScheduleTestFixture()
            assertEquals(GetNotificationScheduleByIdResult.InvalidScheduleId, f.read(id))
            assertEquals(0, f.session.observationCalls)
            assertTrue(f.events.isEmpty())
        }
    }

    @Test
    fun sinSesionOCeroONegativo_noConsulta() = runTest {
        listOf(null, 0L, -1L).forEach { user ->
            val f = ScheduleTestFixture()
            f.session.user.value = user
            assertEquals(GetNotificationScheduleByIdResult.NoAuthenticatedUser, f.read())
            assertTrue(f.events.isEmpty())
        }
    }

    @Test
    fun scheduleTemplateOAnimalAusentes_sonErroresTipados() = runTest {
        val scheduleMissing = ScheduleTestFixture()
        scheduleMissing.schedules.clear()
        assertEquals(GetNotificationScheduleByIdResult.ScheduleNotFound, scheduleMissing.read())
        val templateMissing = ScheduleTestFixture()
        templateMissing.templates.clear()
        assertEquals(GetNotificationScheduleByIdResult.TemplateNotFound, templateMissing.read())
        val animalMissing = ScheduleTestFixture()
        animalMissing.animals.clear()
        assertEquals(GetNotificationScheduleByIdResult.AnimalNotFound, animalMissing.read())
    }

    @Test
    fun normalPropia_devuelveDetalleConTemplateActual() = runTest {
        val f = ScheduleTestFixture()
        val current = f.templates.getValue(5L).copy(titulo = "Desparasitación", tipo = TipoNotificacion.ALIMENTACION)
        f.templates[5L] = current
        assertEquals(
            GetNotificationScheduleByIdResult.Success(ProgramacionNotificacionDetalle(f.schedule(), current)),
            f.read(),
        )
        assertEquals(listOf("get:77", "template:5", "animal:9"), f.events)
    }

    @Test
    fun legacyPropia_devuelveDetalleSinConsultarAnimal() = runTest {
        val f = ScheduleTestFixture()
        val legacy = f.schedule(animalId = null)
        f.schedules[77L] = legacy
        f.animals.clear()
        assertEquals(
            GetNotificationScheduleByIdResult.Success(ProgramacionNotificacionDetalle(legacy, f.templates.getValue(5L))),
            f.read(),
        )
        assertEquals(listOf("get:77", "template:5"), f.events)
    }

    @Test
    fun templateOAnimalAjenos_noExponenDetalle() = runTest {
        val foreignTemplate = ScheduleTestFixture()
        foreignTemplate.templates[5L] = foreignTemplate.templates.getValue(5L).copy(idUsuario = 8L)
        assertEquals(GetNotificationScheduleByIdResult.OwnershipMismatch, foreignTemplate.read())
        assertEquals(listOf("get:77", "template:5"), foreignTemplate.events)
        val foreignAnimal = ScheduleTestFixture()
        foreignAnimal.animals[9L] = foreignAnimal.animals.getValue(9L).copy(idUsuario = 8L)
        assertEquals(GetNotificationScheduleByIdResult.OwnershipMismatch, foreignAnimal.read())
    }

    @Test
    fun erroresDeSesionYLookupsSonFailure() = runTest {
        listOf("get:77", "template:5", "animal:9").forEach { step ->
            val f = ScheduleTestFixture()
            val error = IllegalStateException(step)
            f.failures[step] = error
            assertSame(error, (f.read() as GetNotificationScheduleByIdResult.Failure).cause)
        }
        val f = ScheduleTestFixture()
        val error = IllegalStateException("session")
        f.session.failure = error
        assertSame(error, (f.read() as GetNotificationScheduleByIdResult.Failure).cause)
    }

    @Test
    fun cancelacionDeSesionYLookupsSePropaga() = runTest {
        val cancellation = CancellationException("cancel")
        val f = ScheduleTestFixture()
        f.session.failure = cancellation
        assertTemplateCancellation(cancellation) { f.read() }
        listOf("get:77", "template:5", "animal:9").forEach { step ->
            val candidate = ScheduleTestFixture()
            candidate.failures[step] = cancellation
            assertTemplateCancellation(cancellation) { candidate.read() }
        }
    }
}
