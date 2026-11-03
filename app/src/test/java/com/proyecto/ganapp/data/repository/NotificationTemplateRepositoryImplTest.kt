package com.proyecto.ganapp.data.repository

import com.proyecto.ganapp.data.local.entity.PlantillaNotificacionEntity
import com.proyecto.ganapp.data.repository.impl.NotificationTemplateRepositoryImpl
import com.proyecto.ganapp.domain.model.PlantillaNotificacion
import com.proyecto.ganapp.domain.model.TipoNotificacion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class NotificationTemplateRepositoryImplTest {
    private val dao = TemplateDaoProbe()
    private val schedules = ScheduleDaoProbe()
    private val repository = NotificationTemplateRepositoryImpl(dao, schedules)
    private val domain = PlantillaNotificacion(5L, " Vacuna ", TipoNotificacion.VACUNACION, 8L)
    private val entity = PlantillaNotificacionEntity(5L, " Vacuna ", "Vacunación", 8L)

    @Test fun listEmpty() = runTest {
        assertEquals(emptyList<PlantillaNotificacion>(), repository.getTemplatesByUser(8L).first())
        assertEquals(listOf("user" to 8L), dao.calls)
    }

    @Test fun listMapsEveryEmissionPreservingOrderDuplicatesAndDaoOwnership() = runTest {
        val second = entity.copy(idPlantilla = 6L, idUsuario = 99L)
        dao.emissions = flowOf(emptyList(), listOf(second, entity, entity), listOf(entity))
        assertEquals(
            listOf(emptyList(), listOf(domain.copy(idPlantilla = 6L, idUsuario = 99L), domain, domain), listOf(domain)),
            repository.getTemplatesByUser(8L).toList(),
        )
        assertEquals(listOf("user" to 8L), dao.calls)
    }

    @Test fun getMissing() = runTest {
        assertNull(repository.getTemplateById(5L))
        assertEquals(listOf("get" to 5L), dao.calls)
    }

    @Test fun getMapsEntity() = runTest {
        dao.entity = entity
        assertEquals(domain, repository.getTemplateById(5L))
    }

    @Test fun insertMapsExactlyAndReturnsActualIdIncludingInvalidValues() = runTest {
        for (id in listOf(42L, 0L, -1L)) {
            dao.insertedId = id
            assertEquals(id, repository.insertTemplate(domain))
            assertEquals(entity, dao.written)
        }
        assertEquals(List(3) { "insert" to 5L }, dao.calls)
        assertTrue(schedules.calls.isEmpty())
    }

    @Test fun updateZeroRows() = runTest {
        dao.updatedRows = 0
        assertFalse(repository.updateTemplate(domain))
        assertEquals(Triple(5L, " Vacuna ", "Vacunación"), dao.updatedContent)
        assertNull(dao.written)
        assertEquals(listOf("updateContent" to 5L), dao.calls)
    }

    @Test fun updatePositiveRows() = runTest {
        for (rows in listOf(1, 3)) {
            dao.updatedRows = rows
            assertTrue(repository.updateTemplate(domain))
            assertEquals(Triple(5L, " Vacuna ", "Vacunación"), dao.updatedContent)
            assertNull(dao.written)
        }
    }

    @Test fun updateChangesOnlyTitleAndTypePreservingIdentityOwnerAndAllLegacy() = runTest {
        val original = entity.copy(
            legacyDescripcion = "Descripción original",
            legacyEstado = "PENDIENTE",
            legacyFechaCreacion = 123456789L,
            legacyDosisPorDia = 3,
            legacyIntervaloHoras = 8,
        )
        dao.entity = original
        val edited = domain.copy(
            titulo = " Título editado ",
            tipo = TipoNotificacion.ALIMENTACION,
            idUsuario = 999L,
        )

        assertTrue(repository.updateTemplate(edited))
        assertEquals(
            original.copy(titulo = " Título editado ", tipo = "Alimentación"),
            dao.entity,
        )
        assertEquals(listOf("updateContent" to 5L), dao.calls)
        assertTrue(schedules.calls.isEmpty())
        assertNull(dao.written)
    }

    @Test fun updateZeroRowsLeavesExistingLegacyRowUntouched() = runTest {
        val original = entity.copy(legacyDescripcion = "Conservar", legacyEstado = "ACTIVA")
        dao.entity = original
        dao.updatedRows = 0
        assertFalse(repository.updateTemplate(domain.copy(titulo = "Otro")))
        assertEquals(original, dao.entity)
    }
    @Test fun deleteDelegatesWithoutScheduleCheck() = runTest {
        repository.deleteTemplate(51L)
        assertEquals(listOf("delete" to 51L), dao.calls)
        assertTrue(schedules.calls.isEmpty())
    }

    @Test fun hasSchedulesUsesOnlyCountForZeroOneAndMany() = runTest {
        for (count in listOf(0L, 1L, 9L)) {
            schedules.count = count
            assertEquals(count > 0, repository.hasSchedules(51L))
        }
        assertEquals(List(3) { "count" to 51L }, schedules.calls)
        assertTrue(dao.calls.isEmpty())
    }

    @Test fun corruptGetFailsInsteadOfReturningNull() = runTest {
        dao.entity = entity.copy(tipo = "UNKNOWN")
        try {
            repository.getTemplateById(5L)
            fail("Expected mapping failure")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("UNKNOWN"))
        }
    }

    @Test fun corruptFlowEmissionPropagatesAfterValidEmission() = runTest {
        dao.emissions = flowOf(listOf(entity), listOf(entity.copy(tipo = "UNKNOWN")))
        val collected = mutableListOf<List<PlantillaNotificacion>>()
        try {
            repository.getTemplatesByUser(8L).collect { collected += it }
            fail("Expected downstream mapping failure")
        } catch (expected: IllegalArgumentException) {
            assertEquals(listOf(listOf(domain)), collected)
            assertTrue(expected.message!!.contains("UNKNOWN"))
        }
    }
}
