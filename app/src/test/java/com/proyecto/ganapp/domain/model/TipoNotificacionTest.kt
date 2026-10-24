package com.proyecto.ganapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TipoNotificacionTest {

    @Test
    fun mapeoPersistente_deCadaTipo() {
        assertEquals("Vacunación", TipoNotificacion.VACUNACION.toPersistentString())
        assertEquals("Alimentación", TipoNotificacion.ALIMENTACION.toPersistentString())
        assertEquals("Ciclo Reproductivo", TipoNotificacion.CICLO_REPRODUCTIVO.toPersistentString())
    }

    @Test
    fun parsingInverso_deRepresentacionesValidas() {
        assertEquals(
            TipoNotificacion.VACUNACION,
            TipoNotificacion.fromPersistentString("Vacunación"),
        )
        assertEquals(
            TipoNotificacion.ALIMENTACION,
            TipoNotificacion.fromPersistentString("Alimentación"),
        )
        assertEquals(
            TipoNotificacion.CICLO_REPRODUCTIVO,
            TipoNotificacion.fromPersistentString("Ciclo Reproductivo"),
        )
    }

    @Test
    fun parsing_deStringDesconocido_retornaNull() {
        assertNull(TipoNotificacion.fromPersistentString("OTRO"))
        assertNull(TipoNotificacion.fromPersistentString("VACUNACION"))
        assertNull(TipoNotificacion.fromPersistentString(""))
    }

    @Test
    fun catalogo_noUsaElNombreDelEnumComoFormatoPersistido() {
        TipoNotificacion.entries.forEach { tipo ->
            assertEquals(tipo, TipoNotificacion.fromPersistentString(tipo.toPersistentString()))
        }
        assertNull(TipoNotificacion.fromPersistentString(TipoNotificacion.VACUNACION.name))
    }
}
