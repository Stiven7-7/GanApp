package com.proyecto.ganapp.data.local.db

import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

/** Validación autoritativa de Room v11. Reutiliza el sembrado de 4.5B. */
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigration10To11OfficialTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java, emptyList(),
    )
    private val f = Migration10To11RehearsalFixture(helper)

    @After
    fun cleanup() = f.close()

    @Test
    fun v10Vacia_validaEsquema11YEliminaSoloTablasLegacy() {
        val db = f.official(f.create())
        assertTrue(f.tableExists(db, "plantilla_notificacion"))
        assertTrue(f.tableExists(db, "programacion_notificacion"))
        assertTrue(f.tableExists(db, "usuario"))
        assertTrue(f.tableExists(db, "animal"))
        assertFalse(f.tableExists(db, "notificacion"))
        assertFalse(f.tableExists(db, "notificacion_animal"))
        assertEquals(0L, f.scalar(db, "SELECT COUNT(*) FROM plantilla_notificacion"))
        assertEquals(0L, f.scalar(db, "SELECT COUNT(*) FROM programacion_notificacion"))
        assertTrue(f.rows(db, "PRAGMA table_info(programacion_notificacion)").none {
            it.getValue("name") == "titulo" || it.getValue("name") == "tipo"
        })
        assertEquals(
            setOf(listOf("usuario", "idUsuario", "CASCADE", "NO ACTION")),
            foreignKeys(db, "plantilla_notificacion"),
        )
        assertEquals(
            setOf(
                listOf("plantilla_notificacion", "idPlantilla", "RESTRICT", "NO ACTION"),
                listOf("animal", "idAnimal", "RESTRICT", "NO ACTION"),
            ),
            foreignKeys(db, "programacion_notificacion"),
        )
        val indexes = f.rows(db, "SELECT name FROM sqlite_master WHERE type = 'index' AND name LIKE 'index_%'")
            .map { it.getValue("name") }.toSet()
        assertTrue(
            indexes.containsAll(
                setOf(
                    "index_plantilla_notificacion_idUsuario",
                    "index_programacion_notificacion_idPlantilla",
                    "index_programacion_notificacion_idAnimal",
                ),
            ),
        )
        assertTrue(f.rows(db, "PRAGMA foreign_key_check").isEmpty())
    }

    @Test
    fun ceroAnimales_conservaIdOwnershipYLegacyConAnimalNull() {
        val seeded = f.create()
        f.user(seeded)
        f.notification(seeded)
        val db = f.official(seeded)
        assertEquals(mapOf(
            "idPlantilla" to "10", "titulo" to " Vacuna ", "tipo" to "Vacunación", "idUsuario" to "1",
            "legacyDescripcion" to "Descripción anterior", "legacyEstado" to "COMPLETADA",
            "legacyFechaCreacion" to "1700000000123", "legacyDosisPorDia" to "2", "legacyIntervaloHoras" to "12",
        ), f.templates(db).single())
        f.assertSchedule(f.schedules(db).single(), animal = null)
    }

    @Test
    fun unAnimal_unaProgramacionSinDeduplicarTitulo() {
        val seeded = f.create()
        f.user(seeded)
        f.animal(seeded)
        f.notification(seeded, id = 10L, title = "Vacuna")
        f.notification(seeded, id = 42L, title = "Vacuna")
        f.link(seeded, notification = 10L, animal = 7L)
        val db = f.official(seeded)
        assertEquals(listOf("10", "42"), f.templates(db).map { it.getValue("idPlantilla") })
        f.assertSchedule(f.schedules(db).single { it.getValue("idPlantilla") == "10" })
        f.assertSchedule(f.schedules(db).single { it.getValue("idPlantilla") == "42" }, template = 42L, animal = null)
    }

    @Test
    fun variosAnimales_variasProgramacionesDeLaMismaPlantillaSinDeduplicar() {
        val seeded = f.create()
        f.user(seeded)
        f.notification(seeded)
        listOf(7L, 8L, 9L).forEach { id -> f.animal(seeded, id); f.link(seeded, animal = id) }
        val db = f.official(seeded)
        val schedules = f.schedules(db)
        assertEquals(1, f.templates(db).size)
        assertEquals(listOf("7", "8", "9"), schedules.map { it.getValue("idAnimal") })
        assertEquals(3, schedules.map { it.getValue("idProgramacion") }.toSet().size)
        schedules.forEach { f.assertSchedule(it, animal = it.getValue("idAnimal")!!.toLong()) }
    }

    @Test
    fun usuariosYAnimalesNoCambianYFechasHoraRecurrenciaSeNormalizan() {
        val seeded = f.create()
        f.user(seeded, 1L)
        f.user(seeded, 2L)
        f.animal(seeded, 7L, owner = 1L, favorite = 1)
        f.animal(seeded, 8L, owner = 2L, favorite = 0, nullables = true)
        f.notification(seeded, id = 10L, owner = 1L, recurrence = "CADA 15 DIAS")
        f.notification(seeded, id = 42L, owner = 2L, end = null)
        f.link(seeded, 10L, 7L)
        f.link(seeded, 42L, 8L)
        val users = f.rows(seeded, "SELECT * FROM usuario ORDER BY idUsuario")
        val animals = f.rows(seeded, "SELECT * FROM animal ORDER BY idAnimal")
        val db = f.official(seeded)
        assertEquals(users, f.rows(db, "SELECT * FROM usuario ORDER BY idUsuario"))
        assertEquals(animals, f.rows(db, "SELECT * FROM animal ORDER BY idAnimal"))
        f.assertSchedule(f.schedules(db).single { it.getValue("idPlantilla") == "10" }, recurrence = "CADA 15 DÍAS")
        f.assertSchedule(
            f.schedules(db).single { it.getValue("idPlantilla") == "42" },
            template = 42L, animal = 8L, end = 20_000L,
        )
        assertEquals(0L, f.scalar(db, """
            SELECT COUNT(*) FROM programacion_notificacion p
            JOIN plantilla_notificacion t ON t.idPlantilla = p.idPlantilla
            JOIN animal a ON a.idAnimal = p.idAnimal WHERE t.idUsuario != a.idUsuario
        """.trimIndent()))
    }

    @Test
    fun cadena9a10a11_validaEsquemaYConservaRelacion() {
        val seeded = f.create(9)
        f.user(seeded)
        f.animal(seeded)
        f.notification(seeded)
        f.link(seeded)
        val db = f.official(seeded, MIGRATION_9_10, MIGRATION_10_11)
        assertEquals("0", f.rows(db, "SELECT esFavorito FROM animal").single().getValue("esFavorito"))
        f.assertSchedule(f.schedules(db).single())
        assertFalse(f.tableExists(db, "notificacion"))
    }

    @Test
    fun cadena8a9a10a11_convierteEpochDayUnaSolaVez() {
        val seeded = f.create(8)
        f.user(seeded)
        f.animal(seeded)
        val start = LocalDate.of(2026, 1, 31)
        val end = start.plusDays(2)
        val zone = ZoneId.systemDefault()
        f.notification(
            seeded,
            start = start.atTime(12, 0).atZone(zone).toInstant().toEpochMilli(),
            end = end.atTime(12, 0).atZone(zone).toInstant().toEpochMilli(),
        )
        f.link(seeded)
        val db = f.official(seeded, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
        f.assertSchedule(
            f.schedules(db).single(),
            start = start.toEpochDay(),
            end = end.toEpochDay(),
        )
        assertEquals("0", f.rows(db, "SELECT esFavorito FROM animal").single().getValue("esFavorito"))
    }

    @Test
    fun insercionesPosterioresObtienenIdsValidosSinColision() {
        val seeded = f.create()
        f.user(seeded)
        f.animal(seeded)
        listOf(10L, 42L, 999L).forEach { f.notification(seeded, id = it) }
        f.link(seeded)
        val db = f.official(seeded)
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
    }

    @Test
    fun tipoCorrupto_runMigrationsAndValidateAbortaYConservaV10() {
        val seeded = f.create()
        f.user(seeded)
        f.animal(seeded)
        f.notification(seeded)
        f.link(seeded)
        seeded.execSQL("UPDATE notificacion SET tipo = 'OTRO' WHERE idNotificacion = 10")
        val name = f.databaseName(seeded)
        try {
            val failure = assertThrows(Exception::class.java) { f.official(seeded) }
            val text = generateSequence(failure as Throwable) { it.cause }.joinToString(" | ") { it.message.orEmpty() }
            assertTrue(text, text.contains("unknown notification type"))
            SQLiteDatabase.openDatabase(f.databaseFile(name).path, null, SQLiteDatabase.OPEN_READONLY).use { raw ->
                assertEquals(10, raw.version)
                assertEquals(1, countMaster(raw, "notificacion"))
                assertEquals(1, countMaster(raw, "notificacion_animal"))
                assertEquals(0, countMaster(raw, "plantilla_notificacion"))
                assertEquals(0, countMaster(raw, "programacion_notificacion"))
            }
        } finally {
            f.deleteDatabase(name)
        }
    }

    private fun foreignKeys(db: SupportSQLiteDatabase, table: String): Set<List<String>> =
        f.rows(db, "PRAGMA foreign_key_list($table)").map {
            listOf(it.getValue("table")!!, it.getValue("from")!!, it.getValue("on_delete")!!, it.getValue("on_update")!!)
        }.toSet()

    private fun countMaster(raw: SQLiteDatabase, table: String): Int =
        raw.rawQuery(
            "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(table),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            cursor.getInt(0)
        }
}
