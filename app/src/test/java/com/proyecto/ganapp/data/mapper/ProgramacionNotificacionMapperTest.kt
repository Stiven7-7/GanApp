package com.proyecto.ganapp.data.mapper

import com.proyecto.ganapp.data.local.entity.ProgramacionNotificacionEntity
import com.proyecto.ganapp.domain.model.ProgramacionNotificacion
import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ProgramacionNotificacionMapperTest {
    private val day = LocalDate.of(2026, 11, 15)

    private fun domain() = ProgramacionNotificacion(
        idProgramacion = 77L, idPlantilla = 10L, idAnimal = 9L,
        fechaInicio = day, fechaFin = day.plusDays(10), hora = LocalTime.of(9, 37),
        recurrencia = RecurrenciaNotificacion.DIARIA,
    )

    @Test
    fun fechasNormalesFinDeMesYBisiesto_roundTripEpochDaySinMillis() {
        listOf(day, LocalDate.of(2026, 1, 31), LocalDate.of(2028, 2, 29)).forEach { date ->
            val original = domain().copy(fechaInicio = date, fechaFin = date.plusDays(1))
            val entity = original.toEntity()
            assertEquals(date.toEpochDay(), entity.fechaInicio)
            assertEquals(date.plusDays(1).toEpochDay(), entity.fechaFin)
            assertEquals(original, entity.toDomain())
        }
    }

    @Test
    fun horasValidas_roundTripExactoHHmm() {
        listOf("00:00", "09:37", "23:59").forEach { text ->
            val original = domain().copy(hora = LocalTime.parse(text))
            val entity = original.toEntity()
            assertEquals(text, entity.hora)
            assertEquals(original, entity.toDomain())
        }
    }

    @Test
    fun segundosONanosEnDomain_seRechazanSinTruncar() {
        listOf("09:00:30", "09:00:00.500").forEach { text ->
            assertThrows(IllegalArgumentException::class.java) {
                domain().copy(hora = LocalTime.parse(text)).toEntity()
            }
        }
    }

    @Test
    fun textoHoraInvalido_seRechazaSinCorregir() {
        listOf("25:00", "09:99", "abc", "9:00", "09:00:00", " 09:00 ", "24:00", "").forEach { text ->
            val entity = domain().toEntity().copy(hora = text)
            assertThrows(IllegalArgumentException::class.java) { entity.toDomain() }
        }
    }

    @Test
    fun recurrenciasReales_roundTripConTextoCanonico() {
        val expected = mapOf(
            RecurrenciaNotificacion.NO to "NO",
            RecurrenciaNotificacion.DIARIA to "DIARIA",
            RecurrenciaNotificacion.SEMANAL to "SEMANAL",
            RecurrenciaNotificacion.CADA_15_DIAS to "CADA 15 DÍAS",
            RecurrenciaNotificacion.MENSUAL to "MENSUAL",
            RecurrenciaNotificacion.ANUAL to "ANUAL",
        )
        expected.forEach { (recurrence, text) ->
            val original = domain().copy(recurrencia = recurrence)
            assertEquals(text, original.toEntity().recurrencia)
            assertEquals(original, original.toEntity().toDomain())
        }
    }

    @Test
    fun aliasLegacySinTilde_seLeeYSeSerializaCanonico() {
        val entity = domain().toEntity().copy(recurrencia = "CADA 15 DIAS")
        val restored = entity.toDomain()
        assertEquals(RecurrenciaNotificacion.CADA_15_DIAS, restored.recurrencia)
        assertEquals("CADA 15 DÍAS", restored.toEntity().recurrencia)
    }

    @Test
    fun recurrenciaDesconocida_fallaSinFallback() {
        val entity = domain().toEntity().copy(recurrencia = "OTRO")
        assertThrows(IllegalArgumentException::class.java) { entity.toDomain() }
    }

    @Test
    fun animalNull_seConservaEnAmbasDirecciones() {
        val original = domain().copy(idAnimal = null)
        val entity = original.toEntity()
        assertNull(entity.idAnimal)
        assertNull(entity.toDomain().idAnimal)
        assertEquals(original, entity.toDomain())
        assertEquals(entity, entity.toDomain().toEntity())
    }

    @Test
    fun epochDayIrrepresentable_fallaSinInventarFecha() {
        listOf(Long.MIN_VALUE, Long.MAX_VALUE).forEach { value ->
            assertThrows(DateTimeException::class.java) { domain().toEntity().copy(fechaInicio = value).toDomain() }
            assertThrows(DateTimeException::class.java) { domain().toEntity().copy(fechaFin = value).toDomain() }
        }
    }

    @Test
    fun formatoHoraEsIndependienteDelLocale() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"))
            val original = domain()
            assertEquals("09:37", original.toEntity().hora)
            assertEquals(original, original.toEntity().toDomain())
        } finally {
            Locale.setDefault(previous)
        }
    }
}
