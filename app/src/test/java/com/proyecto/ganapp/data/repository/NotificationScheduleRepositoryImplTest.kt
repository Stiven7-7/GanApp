package com.proyecto.ganapp.data.repository

import com.proyecto.ganapp.data.local.entity.ProgramacionNotificacionEntity
import com.proyecto.ganapp.data.repository.impl.NotificationScheduleRepositoryImpl
import com.proyecto.ganapp.domain.model.ProgramacionNotificacion
import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class NotificationScheduleRepositoryImplTest {
    private val dao = ScheduleDaoProbe()
    private val repository = NotificationScheduleRepositoryImpl(dao)
    private val domain = ProgramacionNotificacion(
        101L, 5L, 9L, LocalDate.of(2026, 11, 2), LocalDate.of(2027, 1, 3),
        LocalTime.of(6, 5), RecurrenciaNotificacion.CADA_15_DIAS,
    )
    private val entity = ProgramacionNotificacionEntity(
        101L, 5L, 9L, LocalDate.of(2026, 11, 2).toEpochDay(),
        LocalDate.of(2027, 1, 3).toEpochDay(), "06:05", "CADA 15 DÍAS",
    )

    @Test fun insertMapsAllFieldsAndReturnsActualId() = runTest {
        for (id in listOf(77L, 0L, -1L)) {
            dao.insertedId = id
            assertEquals(id, repository.insertSchedule(domain))
            assertEquals(entity, dao.written)
        }
        assertEquals(List(3) { "insert" to 101L }, dao.calls)
    }

    @Test fun getMissing() = runTest {
        assertNull(repository.getScheduleById(101L))
        assertEquals(listOf("get" to 101L), dao.calls)
    }

    @Test fun getMapsAllFields() = runTest {
        dao.entity = entity
        assertEquals(domain, repository.getScheduleById(101L))
    }

    @Test fun updateZeroRowsPreservesAssociations() = runTest {
        dao.updatedRows = 0
        assertFalse(repository.updateSchedule(domain))
        assertEquals(entity, dao.written)
        assertEquals(listOf("update" to 101L), dao.calls)
    }

    @Test fun updatePositiveRowsPreservesAssociations() = runTest {
        for (rows in listOf(1, 3)) {
            dao.updatedRows = rows
            assertTrue(repository.updateSchedule(domain))
            assertEquals(entity, dao.written)
        }
    }

    @Test fun deleteDelegatesExactIdOnly() = runTest {
        repository.deleteSchedule(303L)
        assertEquals(listOf("delete" to 303L), dao.calls)
    }

    @Test fun byAnimalPreservesSameTemplateAnimalMultipleRowsAndOrder() = runTest {
        dao.entities = listOf(entity.copy(idProgramacion = 102L), entity)
        assertEquals(listOf(domain.copy(idProgramacion = 102L), domain), repository.getSchedulesByAnimal(9L))
        assertEquals(listOf("animal" to 9L), dao.calls)
    }

    @Test fun byTemplatePreservesOrderCountIdsAndLegacy() = runTest {
        dao.entities = listOf(entity.copy(idProgramacion = 102L), entity.copy(idAnimal = null), entity)
        assertEquals(
            listOf(domain.copy(idProgramacion = 102L), domain.copy(idAnimal = null), domain),
            repository.getSchedulesByTemplate(5L),
        )
        assertEquals(listOf("template" to 5L), dao.calls)
    }

    @Test fun listsEmpty() = runTest {
        assertTrue(repository.getSchedulesByAnimal(9L).isEmpty())
        assertTrue(repository.getSchedulesByTemplate(5L).isEmpty())
    }

    @Test fun legacyNullSurvivesReadInsertAndUpdate() = runTest {
        dao.entity = entity.copy(idAnimal = null)
        val legacy = domain.copy(idAnimal = null)
        assertEquals(legacy, repository.getScheduleById(101L))
        assertEquals(77L, repository.insertSchedule(legacy))
        assertEquals(entity.copy(idAnimal = null), dao.written)
        assertTrue(repository.updateSchedule(legacy))
        assertEquals(entity.copy(idAnimal = null), dao.written)
    }

    @Test fun corruptTimeAndRecurrenceFailAcrossReadPaths() = runTest {
        for (corrupt in listOf(entity.copy(hora = "25:00"), entity.copy(recurrencia = "UNKNOWN"))) {
            dao.entity = corrupt
            dao.entities = listOf(entity, corrupt)
            for (read in listOf<suspend () -> Any?>(
                { repository.getScheduleById(101L) },
                { repository.getSchedulesByAnimal(9L) },
                { repository.getSchedulesByTemplate(5L) },
            )) {
                try {
                    read()
                    fail("Expected mapping failure")
                } catch (expected: IllegalArgumentException) {
                    assertNotNull(expected.message)
                }
            }
        }
    }

    @Test fun subMinutePrecisionFailsBeforeInsertOrUpdate() = runTest {
        for (time in listOf(LocalTime.of(6, 5, 1), LocalTime.of(6, 5, 0, 1))) {
            val invalid = domain.copy(hora = time)
            for (write in listOf<suspend () -> Any>(
                { repository.insertSchedule(invalid) }, { repository.updateSchedule(invalid) },
            )) {
                try {
                    write()
                    fail("Expected mapping failure")
                } catch (expected: IllegalArgumentException) {
                    assertTrue(expected.message!!.contains("minute precision"))
                }
            }
        }
        assertTrue(dao.calls.isEmpty())
        assertNull(dao.written)
    }
}
