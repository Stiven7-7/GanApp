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

class CreateAnimalNotificationScheduleUseCaseTest {
    @Test
    fun sinSesionOCeroONegativo_noConsultaPersisteNiAgenda() = runTest {
        listOf(null, 0L, -1L).forEach { user ->
            val f = ScheduleTestFixture()
            f.session.user.value = user
            assertEquals(CreateAnimalNotificationScheduleResult.NoAuthenticatedUser, f.create())
            assertTrue(f.events.isEmpty())
        }
    }

    @Test
    fun temporalidadInvalida_noConsultaPersisteNiAgenda() = runTest {
        val f = ScheduleTestFixture()
        assertEquals(
            CreateAnimalNotificationScheduleResult.InvalidInput(ScheduleValidationError.INVALID_TIME_PRECISION),
            f.create(f.createRequest(LocalTime.of(9, 0, 30))),
        )
        assertTrue(f.events.isEmpty())
    }

    @Test
    fun templateAusente_noInsertaNiAgenda() = runTest {
        val f = ScheduleTestFixture()
        f.templates.clear()
        assertEquals(CreateAnimalNotificationScheduleResult.TemplateNotFound, f.create())
        assertEquals(listOf("template:5"), f.events)
    }

    @Test
    fun templateAjena_noInsertaNiConsultaAnimal() = runTest {
        val f = ScheduleTestFixture()
        f.templates[5L] = f.templates.getValue(5L).copy(idUsuario = 8L)
        assertEquals(CreateAnimalNotificationScheduleResult.OwnershipMismatch, f.create())
        assertEquals(listOf("template:5"), f.events)
    }

    @Test
    fun animalAusenteOAjeno_noInsertaNiAgenda() = runTest {
        val absent = ScheduleTestFixture()
        absent.animals.clear()
        assertEquals(CreateAnimalNotificationScheduleResult.AnimalNotFound, absent.create())
        assertEquals(listOf("template:5", "animal:9"), absent.events)
        val foreign = ScheduleTestFixture()
        foreign.animals[9L] = foreign.animals.getValue(9L).copy(idUsuario = 8L)
        assertEquals(CreateAnimalNotificationScheduleResult.OwnershipMismatch, foreign.create())
        assertEquals(listOf("template:5", "animal:9"), foreign.events)
    }

    @Test
    fun successPersisteContratoCompletoYAgendaConIdRealYProximaOcurrencia() = runTest {
        val f = ScheduleTestFixture()
        val request = f.createRequest()
        assertEquals(CreateAnimalNotificationScheduleResult.Success(101L), f.create(request))
        val persisted = f.inserted.single()
        assertEquals(0L, persisted.idProgramacion)
        assertEquals(request.templateId, persisted.idPlantilla)
        assertEquals(request.animalId, persisted.idAnimal)
        assertEquals(request.fechaInicio, persisted.fechaInicio)
        assertEquals(request.fechaFin, persisted.fechaFin)
        assertEquals(request.hora, persisted.hora)
        assertEquals(request.recurrencia, persisted.recurrencia)
        assertEquals(listOf(101L to f.at(f.day, request.hora)), f.scheduled)
        assertEquals(listOf("template:5", "animal:9", "insert", "schedule:101"), f.events)
    }

    @Test
    fun idInsertadoNoPositivo_noAgendaNiDevuelveSuccess() = runTest {
        listOf(0L, -1L).forEach { id ->
            val f = ScheduleTestFixture()
            f.nextInsertId = id
            assertTrue(f.create() is CreateAnimalNotificationScheduleResult.Failure)
            assertTrue(f.scheduled.isEmpty())
            assertFalse(f.events.any { it.startsWith("schedule:") })
        }
    }

    @Test
    fun falloTipadoDeAlarmaConservaIdYFilaSinRollback() = runTest {
        val error = IllegalStateException("alarm")
        val f = ScheduleTestFixture()
        f.nextInsertId = 42L
        f.alarmResult = ScheduleAlarmResult.Failure(error)
        val result = f.create() as CreateAnimalNotificationScheduleResult.SchedulingFailure
        assertEquals(42L, result.scheduleId)
        assertSame(error, result.cause)
        assertNotNull(f.schedules[42L])
        assertTrue(f.deleted.isEmpty())
        assertEquals(listOf(42L to f.at(f.day, LocalTime.of(9, 0))), f.scheduled)
    }

    @Test
    fun alarmaLanzaExcepcion_esSchedulingFailureConFilaPersistida() = runTest {
        val f = ScheduleTestFixture()
        val error = IllegalStateException("alarm throws")
        f.failures["schedule:101"] = error
        val result = f.create() as CreateAnimalNotificationScheduleResult.SchedulingFailure
        assertEquals(101L, result.scheduleId)
        assertSame(error, result.cause)
        assertNotNull(f.schedules[101L])
        assertTrue(f.deleted.isEmpty())
    }

    @Test
    fun resultadosInvalidosDeAlarma_noSonSuccessNiRollback() = runTest {
        listOf(ScheduleAlarmResult.InvalidScheduleId, ScheduleAlarmResult.InvalidTriggerTime).forEach { outcome ->
            val f = ScheduleTestFixture()
            f.alarmResult = outcome
            assertEquals(CreateAnimalNotificationScheduleResult.SchedulingFailure(101L), f.create())
            assertNotNull(f.schedules[101L])
            assertTrue(f.deleted.isEmpty())
        }
    }

    @Test
    fun mismaTemplateYAnimalAdmitenDosHorasConIdsDistintosSinConsultarDuplicados() = runTest {
        val f = ScheduleTestFixture()
        val tomorrow = f.day.plusDays(1)
        val morning = f.createRequest(LocalTime.of(8, 0)).copy(fechaInicio = tomorrow, fechaFin = tomorrow)
        val evening = morning.copy(hora = LocalTime.of(18, 0))
        assertEquals(CreateAnimalNotificationScheduleResult.Success(101L), f.create(morning))
        assertEquals(CreateAnimalNotificationScheduleResult.Success(102L), f.create(evening))
        assertEquals(listOf(101L to f.at(tomorrow, morning.hora), 102L to f.at(tomorrow, evening.hora)), f.scheduled)
        assertEquals(2, f.inserted.size)
        assertEquals(5L, f.schedules.getValue(101L).idPlantilla)
        assertEquals(9L, f.schedules.getValue(102L).idAnimal)
        assertFalse(f.events.any { it.startsWith("list") })
    }

    @Test
    fun erroresPreviosAlSchedulingSonFailureSinAlarma() = runTest {
        listOf("template:5", "animal:9", "insert").forEach { step ->
            val f = ScheduleTestFixture()
            val error = IllegalStateException(step)
            f.failures[step] = error
            assertSame(error, (f.create() as CreateAnimalNotificationScheduleResult.Failure).cause)
            assertTrue(f.scheduled.isEmpty())
        }
        val f = ScheduleTestFixture()
        val error = IllegalStateException("session")
        f.session.failure = error
        assertSame(error, (f.create() as CreateAnimalNotificationScheduleResult.Failure).cause)
        assertTrue(f.events.isEmpty())
    }

    @Test
    fun cancelacionDeSesionLookupsInsertYAlarmaSePropaga() = runTest {
        val cancellation = CancellationException("cancel")
        val withoutSession = ScheduleTestFixture()
        withoutSession.session.failure = cancellation
        assertTemplateCancellation(cancellation) { withoutSession.create() }
        listOf("template:5", "animal:9", "insert", "schedule:101").forEach { step ->
            val f = ScheduleTestFixture()
            f.failures[step] = cancellation
            assertTemplateCancellation(cancellation) { f.create() }
            assertTrue(f.deleted.isEmpty())
            if (step == "schedule:101") assertNotNull(f.schedules[101L])
        }
    }
}
