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

class GetAnimalNotificationSchedulesUseCaseTest {
    @Test
    fun idInvalido_noConsulta() = runTest {
        listOf(0L, -1L).forEach { id ->
            val f = ScheduleTestFixture()
            assertEquals(GetAnimalNotificationSchedulesResult.InvalidAnimalId, f.list(id))
            assertTrue(f.events.isEmpty())
            assertEquals(0, f.session.observationCalls)
        }
    }

    @Test
    fun sinSesionOCeroONegativo_noExponeNiConsulta() = runTest {
        listOf(null, 0L, -1L).forEach { user ->
            val f = ScheduleTestFixture()
            f.session.user.value = user
            assertEquals(GetAnimalNotificationSchedulesResult.NoAuthenticatedUser, f.list())
            assertTrue(f.events.isEmpty())
        }
    }

    @Test
    fun animalAusenteOAjeno_noConsultaSchedules() = runTest {
        val absent = ScheduleTestFixture()
        absent.animals.clear()
        assertEquals(GetAnimalNotificationSchedulesResult.AnimalNotFound, absent.list())
        assertEquals(listOf("animal:9"), absent.events)
        val foreign = ScheduleTestFixture()
        foreign.animals[9L] = foreign.animals.getValue(9L).copy(idUsuario = 8L)
        assertEquals(GetAnimalNotificationSchedulesResult.OwnershipMismatch, foreign.list())
        assertEquals(listOf("animal:9"), foreign.events)
    }

    @Test
    fun animalSinSchedules_devuelveListaVaciaValida() = runTest {
        val f = ScheduleTestFixture()
        f.schedules.clear()
        assertEquals(GetAnimalNotificationSchedulesResult.Success(emptyList()), f.list())
        assertEquals(listOf("animal:9", "listAnimal:9"), f.events)
    }

    @Test
    fun dosSchedulesDeMismaTemplate_noSeDeduplicanYResuelvenTemplateActual() = runTest {
        val f = ScheduleTestFixture()
        f.schedules[78L] = f.schedule(id = 78L).copy(hora = LocalTime.of(18, 0))
        val before = (f.list() as GetAnimalNotificationSchedulesResult.Success).schedules
        assertEquals(2, before.size)
        assertEquals("Vacuna", before[0].plantilla.titulo)
        val current = f.templates.getValue(5L).copy(
            titulo = "Desparasitación", tipo = TipoNotificacion.CICLO_REPRODUCTIVO,
        )
        f.templates[5L] = current
        val after = (f.list() as GetAnimalNotificationSchedulesResult.Success).schedules
        assertEquals(listOf(77L, 78L), after.map { it.programacion.idProgramacion })
        assertTrue(after.all { it.plantilla == current })
        assertEquals(f.schedules[77L], after[0].programacion)
        assertEquals(f.schedules[78L], after[1].programacion)
    }

    @Test
    fun filasLegacyDeOtroAnimalSinTemplateOAjenas_noSeExponen() = runTest {
        val f = ScheduleTestFixture()
        f.templates[6L] = f.templates.getValue(5L).copy(idPlantilla = 6L, idUsuario = 8L)
        f.listOverride = listOf(
            f.schedule(), f.schedule(id = 78L, animalId = null),
            f.schedule(id = 79L, animalId = 99L),
            f.schedule(id = 80L, templateId = 6L),
            f.schedule(id = 81L, templateId = 404L),
        )
        val result = f.list() as GetAnimalNotificationSchedulesResult.Success
        assertEquals(listOf(ProgramacionNotificacionDetalle(f.schedule(), f.templates.getValue(5L))), result.schedules)
        assertEquals(listOf("animal:9", "listAnimal:9", "template:5", "template:6", "template:404"), f.events)
    }

    @Test
    fun erroresDeSesionLookupsYListadoSonFailure() = runTest {
        listOf("animal:9", "listAnimal:9", "template:5").forEach { step ->
            val f = ScheduleTestFixture()
            val error = IllegalStateException(step)
            f.failures[step] = error
            assertSame(error, (f.list() as GetAnimalNotificationSchedulesResult.Failure).cause)
        }
        val f = ScheduleTestFixture()
        val error = IllegalStateException("session")
        f.session.failure = error
        assertSame(error, (f.list() as GetAnimalNotificationSchedulesResult.Failure).cause)
    }

    @Test
    fun cancelacionDeSesionAnimalListadoYTemplateSePropaga() = runTest {
        val cancellation = CancellationException("cancel")
        val f = ScheduleTestFixture()
        f.session.failure = cancellation
        assertTemplateCancellation(cancellation) { f.list() }
        listOf("animal:9", "listAnimal:9", "template:5").forEach { step ->
            val candidate = ScheduleTestFixture()
            candidate.failures[step] = cancellation
            assertTemplateCancellation(cancellation) { candidate.list() }
        }
    }
}
