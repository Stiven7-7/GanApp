package com.proyecto.ganapp.data.mapper

import com.proyecto.ganapp.data.local.entity.PlantillaNotificacionEntity
import com.proyecto.ganapp.domain.model.PlantillaNotificacion
import com.proyecto.ganapp.domain.model.TipoNotificacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class PlantillaNotificacionMapperTest {
    @Test
    fun domainToEntity_preservaIdentidadTituloTipoYOwnerConLegacyNull() {
        val domain = PlantillaNotificacion(10L, " Vacuna Mixta ", TipoNotificacion.VACUNACION, 7L)
        val entity = domain.toEntity()
        assertEquals(10L, entity.idPlantilla)
        assertEquals(" Vacuna Mixta ", entity.titulo)
        assertEquals("Vacunación", entity.tipo)
        assertEquals(7L, entity.idUsuario)
        assertNull(entity.legacyDescripcion)
        assertNull(entity.legacyEstado)
        assertNull(entity.legacyFechaCreacion)
        assertNull(entity.legacyDosisPorDia)
        assertNull(entity.legacyIntervaloHoras)
    }

    @Test
    fun entityToDomain_ignoraDeudaLegacySinNormalizarDatosDeNegocio() {
        val entity = PlantillaNotificacionEntity(
            idPlantilla = 42L, titulo = " Desparasitación ", tipo = "Ciclo Reproductivo", idUsuario = 9L,
            legacyDescripcion = "Descripción anterior", legacyEstado = "PENDIENTE",
            legacyFechaCreacion = 123456789L, legacyDosisPorDia = 2, legacyIntervaloHoras = 12,
        )
        assertEquals(
            PlantillaNotificacion(42L, " Desparasitación ", TipoNotificacion.CICLO_REPRODUCTIVO, 9L),
            entity.toDomain(),
        )
        // El dominio no contiene campos legacy; al crear otra Entity no se restauran.
        val recreated = entity.toDomain().toEntity()
        assertNull(recreated.legacyDescripcion)
        assertNull(recreated.legacyEstado)
        assertNull(recreated.legacyFechaCreacion)
        assertNull(recreated.legacyDosisPorDia)
        assertNull(recreated.legacyIntervaloHoras)
    }

    @Test
    fun tiposReales_roundTripConTextoCanonico() {
        val expected = mapOf(
            TipoNotificacion.VACUNACION to "Vacunación",
            TipoNotificacion.ALIMENTACION to "Alimentación",
            TipoNotificacion.CICLO_REPRODUCTIVO to "Ciclo Reproductivo",
        )
        expected.forEach { (type, text) ->
            val domain = PlantillaNotificacion(10L, "Título", type, 7L)
            assertEquals(text, domain.toEntity().tipo)
            assertEquals(domain, domain.toEntity().toDomain())
        }
    }

    @Test
    fun tipoDesconocido_fallaExplicitamenteSinDefault() {
        val entity = PlantillaNotificacionEntity(10L, "Título", "OTRO", 7L)
        assertThrows(IllegalArgumentException::class.java) { entity.toDomain() }
    }
}
