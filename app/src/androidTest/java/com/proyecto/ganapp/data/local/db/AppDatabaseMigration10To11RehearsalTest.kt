package com.proyecto.ganapp.data.local.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigration10To11RehearsalTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java, emptyList(),
    )
    private val f = Migration10To11RehearsalFixture(helper)

    @After
    fun cleanup() = f.close()

    @Test
    fun baseV10Vacia_creaTablasNuevasYEliminaSoloLegacyNotifications() {
        val db = f.create()
        f.rehearse(db)
        assertTrue(f.tableExists(db, "plantilla_notificacion"))
        assertTrue(f.tableExists(db, "programacion_notificacion"))
        assertTrue(f.tableExists(db, "usuario"))
        assertTrue(f.tableExists(db, "animal"))
        assertFalse(f.tableExists(db, "notificacion"))
        assertFalse(f.tableExists(db, "notificacion_animal"))
        assertEquals(0L, f.scalar(db, "SELECT COUNT(*) FROM plantilla_notificacion"))
        assertEquals(0L, f.scalar(db, "SELECT COUNT(*) FROM programacion_notificacion"))
    }

    @Test
    fun ceroAnimales_preservaPlantillaLegacyCompletaYAnimalFisicamenteNull() {
        val db = f.create()
        f.user(db)
        f.notification(db)
        f.rehearse(db)
        val template = f.templates(db).single()
        assertEquals(mapOf(
            "idPlantilla" to "10", "titulo" to " Vacuna ", "tipo" to "Vacunación", "idUsuario" to "1",
            "legacyDescripcion" to "Descripción anterior", "legacyEstado" to "COMPLETADA",
            "legacyFechaCreacion" to "1700000000123", "legacyDosisPorDia" to "2", "legacyIntervaloHoras" to "12",
        ), template)
        f.assertSchedule(f.schedules(db).single(), animal = null)
        assertEquals(1L, f.scalar(db, "SELECT COUNT(*) FROM programacion_notificacion WHERE idAnimal IS NULL"))
        assertEquals(0L, f.scalar(db, "SELECT COUNT(*) FROM programacion_notificacion WHERE idAnimal IN (0, -1)"))
    }

    @Test
    fun unAnimal_generaUnaProgramacionConTemporalidadIntacta() {
        val db = f.create()
        seedLinked(db)
        f.rehearse(db)
        assertEquals(listOf("10"), f.templates(db).map { it.getValue("idPlantilla") })
        f.assertSchedule(f.schedules(db).single())
    }

    @Test
    fun variosAnimales_generaPKsPropiasUnicasSinCopiarSistematicamenteIdLegacy() {
        val db = f.create()
        f.user(db)
        f.notification(db)
        listOf(7L, 8L, 9L).forEach { id -> f.animal(db, id); f.link(db, animal = id) }
        f.rehearse(db)
        val schedules = f.schedules(db)
        assertEquals(1, f.templates(db).size)
        assertEquals(3, schedules.size)
        assertEquals(setOf("7", "8", "9"), schedules.map { it.getValue("idAnimal") }.toSet())
        val ids = schedules.map { it.getValue("idProgramacion")!!.toLong() }
        assertTrue(ids.all { it > 0L })
        assertEquals(3, ids.toSet().size)
        assertTrue(ids.any { it != 10L })
        schedules.forEach { f.assertSchedule(it, animal = it.getValue("idAnimal")!!.toLong()) }
    }

    @Test
    fun mezclaCeroUnoYVariosAnimales_respetaFormulaDeConteosPorNotificacion() {
        val db = f.create()
        f.user(db)
        listOf(10L, 42L, 999L).forEach { f.notification(db, id = it) }
        listOf(7L, 8L, 9L).forEach { f.animal(db, id = it) }
        f.link(db, notification = 42L, animal = 7L)
        listOf(7L, 8L, 9L).forEach { f.link(db, notification = 999L, animal = it) }
        f.rehearse(db)
        assertEquals(3, f.templates(db).size)
        assertEquals(5, f.schedules(db).size)
        val grouped = f.schedules(db).groupBy { it.getValue("idPlantilla") }
        assertEquals(1, grouped.getValue("10").size)
        assertEquals(null, grouped.getValue("10").single().getValue("idAnimal"))
        assertEquals(1, grouped.getValue("42").size)
        assertEquals(3, grouped.getValue("999").size)
    }

    @Test
    fun duplicadosDeTituloYTipo_conservanDosPlantillasConIdsLegacyDistintos() {
        val db = f.create()
        f.user(db)
        f.notification(db, id = 10L, title = "Vacuna")
        f.notification(db, id = 42L, title = "Vacuna")
        f.rehearse(db)
        assertEquals(listOf("10", "42"), f.templates(db).map { it.getValue("idPlantilla") })
        assertTrue(f.templates(db).all { it.getValue("titulo") == "Vacuna" && it.getValue("tipo") == "Vacunación" })
        assertEquals(2, f.schedules(db).size)
    }

    @Test
    fun multiplesUsuarios_preservaTodasLasColumnasUsuarioAnimalYFavoritosSinCruces() {
        val db = f.create()
        f.user(db, 1L)
        f.user(db, 2L)
        f.animal(db, 7L, owner = 1L, favorite = 1)
        f.animal(db, 8L, owner = 2L, favorite = 0, nullables = true)
        f.notification(db, 10L, owner = 1L)
        f.notification(db, 42L, owner = 2L)
        f.link(db, 10L, 7L)
        f.link(db, 42L, 8L)
        val usersBefore = f.rows(db, "SELECT * FROM usuario ORDER BY idUsuario")
        val animalsBefore = f.rows(db, "SELECT * FROM animal ORDER BY idAnimal")
        f.rehearse(db)
        assertEquals(usersBefore, f.rows(db, "SELECT * FROM usuario ORDER BY idUsuario"))
        assertEquals(animalsBefore, f.rows(db, "SELECT * FROM animal ORDER BY idAnimal"))
        assertEquals(listOf("1", "2"), f.templates(db).map { it.getValue("idUsuario") })
        assertEquals(listOf("1", "0"), f.rows(db, "SELECT esFavorito FROM animal ORDER BY idAnimal").map { it.getValue("esFavorito") })
        assertEquals(0L, f.scalar(db, """
            SELECT COUNT(*) FROM programacion_notificacion p
            JOIN plantilla_notificacion t ON t.idPlantilla = p.idPlantilla
            JOIN animal a ON a.idAnimal = p.idAnimal WHERE t.idUsuario != a.idUsuario
        """.trimIndent()))
    }

    @Test
    fun fechaFinNullSeNormalizaYSoloLegacyNullablePermaneceNull() {
        val db = f.create()
        f.user(db)
        f.notification(db, end = null, description = null, doses = null, interval = null)
        f.rehearse(db)
        f.assertSchedule(f.schedules(db).single(), animal = null, end = 20_000L)
        val template = f.templates(db).single()
        listOf("legacyDescripcion", "legacyDosisPorDia", "legacyIntervaloHoras").forEach {
            assertEquals(null, template.getValue(it))
        }
    }

    @Test
    fun tiposRecurrenciasYHorasValidas_sePreservanConUnicoAliasCanonizado() {
        val db = f.create()
        f.user(db)
        val recurrences = listOf("NO", "DIARIA", "SEMANAL", "CADA 15 DÍAS", "CADA 15 DIAS", "MENSUAL", "ANUAL")
        val types = listOf("Vacunación", "Alimentación", "Ciclo Reproductivo")
        val times = listOf("00:00", "09:37", "23:59")
        recurrences.forEachIndexed { index, recurrence ->
            f.notification(db, id = 10L + index, type = types[index % 3], time = times[index % 3],
                recurrence = recurrence, end = if (recurrence == "NO") 20_000L else 20_010L)
        }
        f.rehearse(db)
        recurrences.forEachIndexed { index, recurrence ->
            val id = (10L + index).toString()
            val template = f.templates(db).single { it.getValue("idPlantilla") == id }
            val schedule = f.schedules(db).single { it.getValue("idPlantilla") == id }
            assertEquals(types[index % 3], template.getValue("tipo"))
            assertEquals(times[index % 3], schedule.getValue("hora"))
            assertEquals(if (recurrence == "CADA 15 DIAS") "CADA 15 DÍAS" else recurrence, schedule.getValue("recurrencia"))
        }
    }

    @Test
    fun idsLegacyNoSeReenumeranYNuevasInsercionesAutomaticasNoColisionanNiImponenUnique() {
        val db = f.create()
        f.user(db)
        f.animal(db)
        listOf(10L, 42L, 999L).forEach { f.notification(db, id = it) }
        f.link(db)
        f.rehearse(db)
        assertEquals(listOf("10", "42", "999"), f.templates(db).map { it.getValue("idPlantilla") })
        db.execSQL("INSERT INTO plantilla_notificacion (titulo, tipo, idUsuario) VALUES ('Nueva', 'Vacunación', 1)")
        assertTrue(f.scalar(db, "SELECT last_insert_rowid()") > 999L)
        val previousMax = f.scalar(db, "SELECT MAX(idProgramacion) FROM programacion_notificacion")
        db.execSQL("""
            INSERT INTO programacion_notificacion (idPlantilla, idAnimal, fechaInicio, fechaFin, hora, recurrencia)
            VALUES (10, 7, 20000, 20010, '18:00', 'DIARIA')
        """.trimIndent())
        val generated = f.scalar(db, "SELECT last_insert_rowid()")
        assertTrue(generated > previousMax)
        assertEquals(2L, f.scalar(db, "SELECT COUNT(*) FROM programacion_notificacion WHERE idPlantilla = 10 AND idAnimal = 7"))
        assertEquals(1L, f.scalar(db, "SELECT COUNT(*) FROM programacion_notificacion WHERE idProgramacion = ?", generated))
    }

    @Test
    fun cadena9a10aRehearsal11_preservaDatosYDefaultFavorito() {
        val db = f.create(9)
        seedLinked(db)
        val users = f.rows(db, "SELECT * FROM usuario")
        f.rehearse(db, MIGRATION_9_10, MIGRATION_10_11)
        assertEquals(users, f.rows(db, "SELECT * FROM usuario"))
        assertEquals("0", f.rows(db, "SELECT esFavorito FROM animal").single().getValue("esFavorito"))
        f.assertSchedule(f.schedules(db).single())
    }

    @Test
    fun cadena8a9a10aRehearsal11_convierteMillisUnaSolaVezYConservaRelacion() {
        val db = f.create(8)
        f.user(db)
        f.animal(db)
        val start = LocalDate.of(2026, 1, 31)
        val end = start.plusDays(2)
        val zone = ZoneId.systemDefault()
        val startMillis = start.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val endMillis = end.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        f.notification(db, start = startMillis, end = endMillis)
        f.notification(db, id = 42L, start = startMillis, end = null)
        f.link(db)
        f.rehearse(db, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
        f.assertSchedule(f.schedules(db).single { it.getValue("idPlantilla") == "10" },
            start = start.toEpochDay(), end = end.toEpochDay())
        f.assertSchedule(f.schedules(db).single { it.getValue("idPlantilla") == "42" },
            template = 42L, animal = null, start = start.toEpochDay(), end = start.toEpochDay())
        assertEquals("0", f.rows(db, "SELECT esFavorito FROM animal").single().getValue("esFavorito"))
    }

    private fun seedLinked(db: SupportSQLiteDatabase) {
        f.user(db)
        f.animal(db)
        f.notification(db)
        f.link(db)
    }
}
