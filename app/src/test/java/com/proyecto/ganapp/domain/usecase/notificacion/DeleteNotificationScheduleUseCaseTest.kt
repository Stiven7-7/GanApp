package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
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

class DeleteNotificationScheduleUseCaseTest {
    @Test
    fun idInvalido_noConsultaNiCancela() = runTest {
        listOf(0L, -1L).forEach { id ->
            val f = ScheduleTestFixture()
            assertEquals(DeleteNotificationScheduleResult.InvalidScheduleId, f.delete(id))
            assertTrue(f.events.isEmpty())
            assertEquals(0, f.session.observationCalls)
        }
    }

    @Test
    fun sinSesionOCeroONegativo_noConsultaNiCancela() = runTest {
        listOf(null, 0L, -1L).forEach { user ->
            val f = ScheduleTestFixture()
            f.session.user.value = user
            assertEquals(DeleteNotificationScheduleResult.NoAuthenticatedUser, f.delete())
            assertTrue(f.events.isEmpty())
        }
    }

    @Test
    fun scheduleTemplateOAnimalAusentes_noCancelaNiElimina() = runTest {
        val missingSchedule = ScheduleTestFixture()
        missingSchedule.schedules.clear()
        assertEquals(DeleteNotificationScheduleResult.ScheduleNotFound, missingSchedule.delete())
        assertEquals(listOf("get:77"), missingSchedule.events)
        val missingTemplate = ScheduleTestFixture()
        missingTemplate.templates.clear()
        assertEquals(DeleteNotificationScheduleResult.TemplateNotFound, missingTemplate.delete())
        assertEquals(listOf("get:77", "template:5"), missingTemplate.events)
        val missingAnimal = ScheduleTestFixture()
        missingAnimal.animals.clear()
        assertEquals(DeleteNotificationScheduleResult.AnimalNotFound, missingAnimal.delete())
        assertTrue(missingAnimal.cancelled.isEmpty())
        assertTrue(missingAnimal.deleted.isEmpty())
    }

    @Test
    fun templateOAnimalAjenos_noCancelaNiElimina() = runTest {
        val foreignTemplate = ScheduleTestFixture()
        foreignTemplate.templates[5L] = foreignTemplate.templates.getValue(5L).copy(idUsuario = 8L)
        assertEquals(DeleteNotificationScheduleResult.OwnershipMismatch, foreignTemplate.delete())
        assertEquals(listOf("get:77", "template:5"), foreignTemplate.events)
        val foreignAnimal = ScheduleTestFixture()
        foreignAnimal.animals[9L] = foreignAnimal.animals.getValue(9L).copy(idUsuario = 8L)
        assertEquals(DeleteNotificationScheduleResult.OwnershipMismatch, foreignAnimal.delete())
        assertTrue(foreignAnimal.cancelled.isEmpty())
        assertTrue(foreignAnimal.deleted.isEmpty())
    }

    @Test
    fun successCancelaAntesDeEliminarPorMismoId() = runTest {
        val f = ScheduleTestFixture()
        f.activeAlarms[77L] = f.now
        assertEquals(DeleteNotificationScheduleResult.Success, f.delete())
        assertEquals(listOf("get:77", "template:5", "animal:9", "cancel:77", "delete:77"), f.events)
        assertEquals(listOf(77L), f.cancelled)
        assertEquals(listOf(77L), f.deleted)
        assertNull(f.schedules[77L])
        assertNull(f.activeAlarms[77L])
    }

    @Test
    fun cancelFailureNoEliminaYConservaFila() = runTest {
        val f = ScheduleTestFixture()
        val error = IllegalStateException("cancel")
        f.cancelResult = ScheduleAlarmCancelResult.Failure(error)
        val result = f.delete() as DeleteNotificationScheduleResult.AlarmCancellationFailed
        assertSame(error, result.cause)
        assertEquals(listOf(77L), f.cancelled)
        assertTrue(f.deleted.isEmpty())
        assertFalse(f.events.contains("delete:77"))
        assertNotNull(f.schedules[77L])
    }

    @Test
    fun cancelLanzaExcepcionNoEliminaYConservaFila() = runTest {
        val f = ScheduleTestFixture()
        val error = IllegalStateException("cancel throws")
        f.failures["cancel:77"] = error
        assertSame(error, (f.delete() as DeleteNotificationScheduleResult.AlarmCancellationFailed).cause)
        assertFalse(f.events.contains("delete:77"))
        assertNotNull(f.schedules[77L])
    }

    @Test
    fun cancelIdInvalidoNoElimina() = runTest {
        val f = ScheduleTestFixture()
        f.cancelResult = ScheduleAlarmCancelResult.InvalidScheduleId
        assertEquals(DeleteNotificationScheduleResult.AlarmCancellationFailed(), f.delete())
        assertTrue(f.deleted.isEmpty())
        assertNotNull(f.schedules[77L])
    }

    @Test
    fun deleteFallaDespuesDeCancelar_devuelvePersistenceFailureSinReschedule() = runTest {
        val f = ScheduleTestFixture()
        f.activeAlarms[77L] = f.now
        val error = IllegalStateException("delete")
        f.failures["delete:77"] = error
        assertSame(error, (f.delete() as DeleteNotificationScheduleResult.PersistenceFailure).cause)
        assertEquals(listOf("get:77", "template:5", "animal:9", "cancel:77", "delete:77"), f.events)
        assertNotNull(f.schedules[77L])
        assertNull(f.activeAlarms[77L])
        assertTrue(f.scheduled.isEmpty())
    }

    @Test
    fun legacyPropiaPermiteCancelarYEliminarSinConsultarAnimal() = runTest {
        val f = ScheduleTestFixture()
        f.schedules[77L] = f.schedule(animalId = null)
        f.animals.clear()
        assertEquals(DeleteNotificationScheduleResult.Success, f.delete())
        assertEquals(listOf("get:77", "template:5", "cancel:77", "delete:77"), f.events)
    }

    @Test
    fun legacyConTemplateAjenaNoCancelaNiElimina() = runTest {
        val f = ScheduleTestFixture()
        f.schedules[77L] = f.schedule(animalId = null)
        f.templates[5L] = f.templates.getValue(5L).copy(idUsuario = 8L)
        assertEquals(DeleteNotificationScheduleResult.OwnershipMismatch, f.delete())
        assertTrue(f.cancelled.isEmpty())
        assertTrue(f.deleted.isEmpty())
    }

    @Test
    fun alarmaYaAusente_seConsideraCancelacionSatisfactoria() = runTest {
        val f = ScheduleTestFixture()
        assertTrue(f.activeAlarms.isEmpty())
        assertEquals(DeleteNotificationScheduleResult.Success, f.delete())
        assertEquals(listOf(77L), f.cancelled)
        assertEquals(listOf(77L), f.deleted)
    }

    @Test
    fun erroresDeSesionYLookupsSonFailureSinCancelNiDelete() = runTest {
        listOf("get:77", "template:5", "animal:9").forEach { step ->
            val f = ScheduleTestFixture()
            val error = IllegalStateException(step)
            f.failures[step] = error
            assertSame(error, (f.delete() as DeleteNotificationScheduleResult.Failure).cause)
            assertTrue(f.cancelled.isEmpty())
            assertTrue(f.deleted.isEmpty())
        }
        val f = ScheduleTestFixture()
        val error = IllegalStateException("session")
        f.session.failure = error
        assertSame(error, (f.delete() as DeleteNotificationScheduleResult.Failure).cause)
        assertTrue(f.events.isEmpty())
    }

    @Test
    fun cancelacionDeSesionLookupsCancelYDeleteSePropaga() = runTest {
        val cancellation = CancellationException("cancel")
        val f = ScheduleTestFixture()
        f.session.failure = cancellation
        assertTemplateCancellation(cancellation) { f.delete() }
        listOf("get:77", "template:5", "animal:9", "cancel:77", "delete:77").forEach { step ->
            val candidate = ScheduleTestFixture()
            candidate.failures[step] = cancellation
            assertTemplateCancellation(cancellation) { candidate.delete() }
            assertNotNull(candidate.schedules[77L])
            if (step == "cancel:77") assertFalse(candidate.events.contains("delete:77"))
            if (step == "delete:77") assertEquals(listOf(77L), candidate.cancelled)
        }
    }
}
