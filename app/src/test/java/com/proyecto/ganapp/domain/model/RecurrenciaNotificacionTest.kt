package com.proyecto.ganapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecurrenciaNotificacionTest {

    @Test
    fun mapeoPersistente_deCadaRecurrencia() {
        assertEquals("NO", RecurrenciaNotificacion.NO.toPersistentString())
        assertEquals("DIARIA", RecurrenciaNotificacion.DIARIA.toPersistentString())
        assertEquals("SEMANAL", RecurrenciaNotificacion.SEMANAL.toPersistentString())
        assertEquals("CADA 15 DÍAS", RecurrenciaNotificacion.CADA_15_DIAS.toPersistentString())
        assertEquals("MENSUAL", RecurrenciaNotificacion.MENSUAL.toPersistentString())
        assertEquals("ANUAL", RecurrenciaNotificacion.ANUAL.toPersistentString())
    }

    @Test
    fun parsingInverso_deRepresentacionesValidas() {
        assertEquals(RecurrenciaNotificacion.NO, RecurrenciaNotificacion.fromPersistentString("NO"))
        assertEquals(RecurrenciaNotificacion.DIARIA, RecurrenciaNotificacion.fromPersistentString("DIARIA"))
        assertEquals(RecurrenciaNotificacion.SEMANAL, RecurrenciaNotificacion.fromPersistentString("SEMANAL"))
        assertEquals(RecurrenciaNotificacion.MENSUAL, RecurrenciaNotificacion.fromPersistentString("MENSUAL"))
        assertEquals(RecurrenciaNotificacion.ANUAL, RecurrenciaNotificacion.fromPersistentString("ANUAL"))
    }

    @Test
    fun parsing_cada15Dias_aceptaFormaConAcentoYSinAcento() {
        assertEquals(
            RecurrenciaNotificacion.CADA_15_DIAS,
            RecurrenciaNotificacion.fromPersistentString("CADA 15 DÍAS"),
        )
        assertEquals(
            RecurrenciaNotificacion.CADA_15_DIAS,
            RecurrenciaNotificacion.fromPersistentString("CADA 15 DIAS"),
        )
    }

    @Test
    fun parsing_deStringDesconocido_retornaNull() {
        assertNull(RecurrenciaNotificacion.fromPersistentString("QUINCENAL"))
        assertNull(RecurrenciaNotificacion.fromPersistentString("CADA_15_DIAS"))
        assertNull(RecurrenciaNotificacion.fromPersistentString(""))
    }
}
