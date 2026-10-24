package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import com.proyecto.ganapp.domain.model.TipoNotificacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CreateNotificationMapperTest {

    @Test
    fun nombre_seNormalizaConTrimSinMutarElRequest() {
        val request = request(nombre = " Vacuna ")

        val notificacion = CreateNotificationMapper.toNotificacion(request)

        assertEquals("Vacuna", notificacion.nombre)
        assertEquals(" Vacuna ", request.nombre)
    }

    @Test
    fun tipos_usanLaRepresentacionPersistida() {
        assertEquals(
            "Vacunación",
            CreateNotificationMapper.toNotificacion(request(tipo = TipoNotificacion.VACUNACION)).tipo,
        )
        assertEquals(
            "Alimentación",
            CreateNotificationMapper.toNotificacion(request(tipo = TipoNotificacion.ALIMENTACION)).tipo,
        )
        assertEquals(
            "Ciclo Reproductivo",
            CreateNotificationMapper.toNotificacion(
                request(tipo = TipoNotificacion.CICLO_REPRODUCTIVO),
            ).tipo,
        )
    }

    @Test
    fun recurrencias_usanLaRepresentacionPersistida() {
        val expected = mapOf(
            RecurrenciaNotificacion.NO to "NO",
            RecurrenciaNotificacion.DIARIA to "DIARIA",
            RecurrenciaNotificacion.SEMANAL to "SEMANAL",
            RecurrenciaNotificacion.CADA_15_DIAS to "CADA 15 DÍAS",
            RecurrenciaNotificacion.MENSUAL to "MENSUAL",
            RecurrenciaNotificacion.ANUAL to "ANUAL",
        )

        expected.forEach { (recurrencia, persisted) ->
            assertEquals(
                persisted,
                CreateNotificationMapper.toNotificacion(request(recurrencia = recurrencia)).seRepite,
            )
        }
    }

    @Test
    fun campos_seCopianYLosLegacyQuedanNulos() {
        val notificacion = CreateNotificationMapper.toNotificacion(
            request(
                fechaInicio = 1_700_000_000_000L,
                fechaFin = 1_700_086_400_000L,
                hora = "08:30",
                idUsuario = 7L,
            ),
        )

        assertEquals(1_700_000_000_000L, notificacion.fechaInicio)
        assertEquals(1_700_086_400_000L, notificacion.fechaFin)
        assertEquals("08:30", notificacion.hora)
        assertEquals(7L, notificacion.idUsuario)
        assertEquals(0L, notificacion.idNotificacion)
        assertNull(notificacion.dosisPorDia)
        assertNull(notificacion.intervaloHoras)
    }

    private fun request(
        nombre: String = "Vacuna",
        tipo: TipoNotificacion = TipoNotificacion.VACUNACION,
        fechaInicio: Long = 10L,
        fechaFin: Long = 10L,
        hora: String = "08:30",
        recurrencia: RecurrenciaNotificacion = RecurrenciaNotificacion.NO,
        idUsuario: Long = 1L,
    ): CreateNotificationRequest {
        return CreateNotificationRequest(
            nombre = nombre,
            tipo = tipo,
            fechaInicio = fechaInicio,
            fechaFin = fechaFin,
            hora = hora,
            recurrencia = recurrencia,
            idUsuario = idUsuario,
        )
    }
}
