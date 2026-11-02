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

class UpdateNotificationScheduleUseCaseTest {
    @Test
    fun configuracionAgotadaOPrecisionInvalida_noConsultaActualizaNiAgenda() = runTest {
        val f = ScheduleTestFixture()
        val past = f.updateRequest().copy(fechaInicio = f.day.minusDays(1), fechaFin = f.day.minusDays(1))
        assertEquals(UpdateNotificationScheduleResult.InvalidInput(ScheduleValidationError.NO_FUTURE_OCCURRENCE),
            f.update(past))
        assertEquals(UpdateNotificationScheduleResult.InvalidInput(ScheduleValidationError.INVALID_TIME_PRECISION),
            f.update(f.updateRequest().copy(hora = LocalTime.of(9, 0, 1))))
        assertTrue(f.events.isEmpty())
        assertEquals(0, f.session.observationCalls)
    }

    @Test
    fun sinSesionOCeroONegativo_noConsultaActualizaNiAgenda() = runTest {
        listOf(null, 0L, -1L).forEach { user ->
            val f = ScheduleTestFixture()
            f.session.user.value = user
            assertEquals(UpdateNotificationScheduleResult.NoAuthenticatedUser, f.update())
            assertTrue(f.events.isEmpty())
        }
    }

    @Test
    fun scheduleAusente_noActualizaNiAgenda() = runTest {
        val f = ScheduleTestFixture()
        f.schedules.clear()
        assertEquals(UpdateNotificationScheduleResult.ScheduleNotFound, f.update())
        assertEquals(listOf("get:77"), f.events)
    }

    @Test
    fun templateAusenteOAjena_noActualizaNiAgenda() = runTest {
        val absent = ScheduleTestFixture()
        absent.templates.clear()
        assertEquals(UpdateNotificationScheduleResult.TemplateNotFound, absent.update())
        assertEquals(listOf("get:77", "template:5"), absent.events)
        val foreign = ScheduleTestFixture()
        foreign.templates[5L] = foreign.templates.getValue(5L).copy(idUsuario = 8L)
        assertEquals(UpdateNotificationScheduleResult.OwnershipMismatch, foreign.update())
        assertEquals(listOf("get:77", "template:5"), foreign.events)
    }

    @Test
    fun animalAusenteOAjeno_noActualizaNiAgenda() = runTest {
        val absent = ScheduleTestFixture()
        absent.animals.clear()
        assertEquals(UpdateNotificationScheduleResult.AnimalNotFound, absent.update())
        assertTrue(absent.updated.isEmpty())
        assertTrue(absent.scheduled.isEmpty())
        val foreign = ScheduleTestFixture()
        foreign.animals[9L] = foreign.animals.getValue(9L).copy(idUsuario = 8L)
        assertEquals(UpdateNotificationScheduleResult.OwnershipMismatch, foreign.update())
        assertTrue(foreign.updated.isEmpty())
        assertTrue(foreign.scheduled.isEmpty())
    }

    @Test
    fun successSoloCambiaTemporalidadConservaIdentidadYReprogramaMismoId() = runTest {
        val f = ScheduleTestFixture()
        val original = f.schedules.getValue(77L)
        val request = f.updateRequest().copy(
            fechaInicio = f.day.plusDays(1), fechaFin = f.day.plusDays(10),
            hora = LocalTime.of(18, 0), recurrencia = RecurrenciaNotificacion.DIARIA,
        )
        assertEquals(UpdateNotificationScheduleResult.Success, f.update(request))
        val updated = f.updated.single()
        assertEquals(original.copy(
            fechaInicio = request.fechaInicio, fechaFin = request.fechaFin,
            hora = request.hora, recurrencia = request.recurrencia,
        ), updated)
        assertEquals(77L, updated.idProgramacion)
        assertEquals(5L, updated.idPlantilla)
        assertEquals(9L, updated.idAnimal)
        assertEquals(listOf(77L to f.at(request.fechaInicio, request.hora)), f.scheduled)
        assertEquals(listOf("get:77", "template:5", "animal:9", "update", "schedule:77"), f.events)
    }

    @Test
    fun inicioPasadoConProximaOcurrenciaFutura_permiteUpdate() = runTest {
        val f = ScheduleTestFixture()
        val request = f.updateRequest().copy(
            fechaInicio = f.day.withDayOfMonth(1), fechaFin = f.day.withDayOfMonth(30),
            recurrencia = RecurrenciaNotificacion.DIARIA,
        )
        assertEquals(UpdateNotificationScheduleResult.Success, f.update(request))
        assertEquals(request.fechaInicio, f.updated.single().fechaInicio)
        assertEquals(listOf(77L to f.at(f.day, request.hora)), f.scheduled)
    }

    @Test
    fun legacyConservaAnimalNullYReprogramaSinConsultarAnimal() = runTest {
        val f = ScheduleTestFixture()
        f.schedules[77L] = f.schedule(animalId = null)
        f.animals.clear()
        assertEquals(UpdateNotificationScheduleResult.Success, f.update())
        assertNull(f.updated.single().idAnimal)
        assertEquals(5L, f.updated.single().idPlantilla)
        assertEquals(77L, f.updated.single().idProgramacion)
        assertEquals(listOf("get:77", "template:5", "update", "schedule:77"), f.events)
    }

    @Test
    fun updateFalse_noAgendaNiDevuelveSuccess() = runTest {
        val f = ScheduleTestFixture()
        f.updateResult = false
        val original = f.schedules.getValue(77L)
        assertTrue(f.update() is UpdateNotificationScheduleResult.Failure)
        assertTrue(f.scheduled.isEmpty())
        assertEquals(original, f.schedules[77L])
    }

    @Test
    fun falloTipadoDeReprogramacionConservaUpdateSinRollback() = runTest {
        val f = ScheduleTestFixture()
        val error = IllegalStateException("alarm")
        f.alarmResult = ScheduleAlarmResult.Failure(error)
        val result = f.update() as UpdateNotificationScheduleResult.SchedulingFailure
        assertEquals(77L, result.scheduleId)
        assertSame(error, result.cause)
        assertEquals(LocalTime.of(9, 0), f.schedules.getValue(77L).hora)
        assertEquals(1, f.updated.size)
        assertTrue(f.deleted.isEmpty())
    }

    @Test
    fun excepcionDeAlarmaEsSchedulingFailureSinSegundoUpdate() = runTest {
        val f = ScheduleTestFixture()
        val error = IllegalStateException("alarm throws")
        f.failures["schedule:77"] = error
        val result = f.update() as UpdateNotificationScheduleResult.SchedulingFailure
        assertEquals(77L, result.scheduleId)
        assertSame(error, result.cause)
        assertEquals(1, f.updated.size)
        assertEquals(f.updated.single(), f.schedules[77L])
    }

    @Test
    fun resultadoInvalidoDeAlarmaEsSchedulingFailureSinRollback() = runTest {
        listOf(ScheduleAlarmResult.InvalidScheduleId, ScheduleAlarmResult.InvalidTriggerTime).forEach { outcome ->
            val f = ScheduleTestFixture()
            f.alarmResult = outcome
            assertEquals(UpdateNotificationScheduleResult.SchedulingFailure(77L), f.update())
            assertEquals(1, f.updated.size)
            assertEquals(f.updated.single(), f.schedules[77L])
        }
    }

    @Test
    fun erroresPreviosAReprogramacionSonFailureSinAlarma() = runTest {
        listOf("get:77", "template:5", "animal:9", "update").forEach { step ->
            val f = ScheduleTestFixture()
            val error = IllegalStateException(step)
            f.failures[step] = error
            assertSame(error, (f.update() as UpdateNotificationScheduleResult.Failure).cause)
            assertTrue(f.scheduled.isEmpty())
        }
        val f = ScheduleTestFixture()
        val error = IllegalStateException("session")
        f.session.failure = error
        assertSame(error, (f.update() as UpdateNotificationScheduleResult.Failure).cause)
        assertTrue(f.events.isEmpty())
    }

    @Test
    fun cancelacionDeSesionLookupsUpdateYAlarmaSePropaga() = runTest {
        val cancellation = CancellationException("cancel")
        val f = ScheduleTestFixture()
        f.session.failure = cancellation
        assertTemplateCancellation(cancellation) { f.update() }
        listOf("get:77", "template:5", "animal:9", "update", "schedule:77").forEach { step ->
            val candidate = ScheduleTestFixture()
            candidate.failures[step] = cancellation
            assertTemplateCancellation(cancellation) { candidate.update() }
            if (step == "schedule:77") assertEquals(candidate.updated.single(), candidate.schedules[77L])
        }
    }
}
