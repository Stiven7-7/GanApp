package com.proyecto.ganapp.data.local.db

import android.database.sqlite.SQLiteConstraintException
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Inspección SQLite del rehearsal; NO sustituye el schema autoritativo Room v11 de 4.11. */
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigration10To11PhysicalSchemaTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java, emptyList(),
    )
    private val f = Migration10To11RehearsalFixture(helper)

    @After
    fun cleanup() = f.close()

    @Test
    fun columnasTiposPKNullabilityYAutoincrementCoincidenConContrato() {
        val db = f.create()
        f.rehearse(db)
        // Triple: afinidad, NOT NULL, posición de PK.
        assertEquals(mapOf(
            "idPlantilla" to Triple("INTEGER", "1", "1"),
            "titulo" to Triple("TEXT", "1", "0"),
            "tipo" to Triple("TEXT", "1", "0"),
            "idUsuario" to Triple("INTEGER", "1", "0"),
            "legacyDescripcion" to Triple("TEXT", "0", "0"),
            "legacyEstado" to Triple("TEXT", "0", "0"),
            "legacyFechaCreacion" to Triple("INTEGER", "0", "0"),
            "legacyDosisPorDia" to Triple("INTEGER", "0", "0"),
            "legacyIntervaloHoras" to Triple("INTEGER", "0", "0"),
        ), columns(db, "plantilla_notificacion"))
        assertEquals(mapOf(
            "idProgramacion" to Triple("INTEGER", "1", "1"),
            "idPlantilla" to Triple("INTEGER", "1", "0"),
            "idAnimal" to Triple("INTEGER", "0", "0"),
            "fechaInicio" to Triple("INTEGER", "1", "0"),
            "fechaFin" to Triple("INTEGER", "1", "0"),
            "hora" to Triple("TEXT", "1", "0"),
            "recurrencia" to Triple("TEXT", "1", "0"),
        ), columns(db, "programacion_notificacion"))
        listOf("plantilla_notificacion", "programacion_notificacion").forEach { table ->
            val ddl = f.rows(db, "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = ?", table)
                .single().getValue("sql")!!
            assertTrue(ddl, ddl.contains("PRIMARY KEY AUTOINCREMENT NOT NULL"))
        }
    }

    @Test
    fun foreignKeysFisicasDeclaranCascadeYRestrictConNoActionEnUpdate() {
        val db = f.create()
        f.rehearse(db)
        assertEquals(setOf(listOf("usuario", "idUsuario", "idUsuario", "CASCADE", "NO ACTION")),
            foreignKeys(db, "plantilla_notificacion"))
        assertEquals(setOf(
            listOf("plantilla_notificacion", "idPlantilla", "idPlantilla", "RESTRICT", "NO ACTION"),
            listOf("animal", "idAnimal", "idAnimal", "RESTRICT", "NO ACTION"),
        ), foreignKeys(db, "programacion_notificacion"))
    }

    @Test
    fun indicesFisicosSonLosTresEsperadosYNingunoEsUnique() {
        val db = f.create()
        f.rehearse(db)
        val expected = mapOf(
            "plantilla_notificacion" to mapOf("index_plantilla_notificacion_idUsuario" to "idUsuario"),
            "programacion_notificacion" to mapOf(
                "index_programacion_notificacion_idPlantilla" to "idPlantilla",
                "index_programacion_notificacion_idAnimal" to "idAnimal",
            ),
        )
        expected.forEach { (table, expectedIndices) ->
            val indices = f.rows(db, "PRAGMA index_list('$table')")
            assertEquals(expectedIndices.keys, indices.map { it.getValue("name") }.toSet())
            assertTrue(indices.all { it.getValue("unique") == "0" })
            indices.forEach { index ->
                val name = index.getValue("name")!!
                assertEquals(listOf(expectedIndices.getValue(name)),
                    f.rows(db, "PRAGMA index_info('$name')").map { it.getValue("name") })
            }
        }
    }

    @Test
    fun constraintsRestrictYCascadeSonEfectivasEnSQLite() {
        val db = f.create()
        f.user(db)
        f.animal(db)
        f.notification(db)
        f.link(db)
        f.rehearse(db)
        assertThrows(SQLiteConstraintException::class.java) {
            db.execSQL("DELETE FROM plantilla_notificacion WHERE idPlantilla = 10")
        }
        assertThrows(SQLiteConstraintException::class.java) {
            db.execSQL("DELETE FROM animal WHERE idAnimal = 7")
        }
        assertEquals(1L, f.scalar(db, "SELECT COUNT(*) FROM programacion_notificacion"))
        assertEquals(1L, f.scalar(db, "SELECT COUNT(*) FROM plantilla_notificacion WHERE idPlantilla = 10"))
        assertEquals(1L, f.scalar(db, "SELECT COUNT(*) FROM animal WHERE idAnimal = 7"))
        // CASCADE del usuario se verifica con una plantilla sin programaciones dependientes.
        f.user(db, 2L)
        db.execSQL("INSERT INTO plantilla_notificacion (idPlantilla, titulo, tipo, idUsuario) VALUES (20, 'Otra', 'Vacunación', 2)")
        db.execSQL("DELETE FROM usuario WHERE idUsuario = 2")
        assertEquals(0L, f.scalar(db, "SELECT COUNT(*) FROM plantilla_notificacion WHERE idPlantilla = 20"))
        assertEquals(1L, f.scalar(db, "SELECT COUNT(*) FROM plantilla_notificacion WHERE idPlantilla = 10"))
    }

    private fun columns(db: SupportSQLiteDatabase, table: String): Map<String, Triple<String, String, String>> =
        f.rows(db, "PRAGMA table_info('$table')").associate { column ->
            column.getValue("name")!! to Triple(
                column.getValue("type")!!, column.getValue("notnull")!!, column.getValue("pk")!!,
            )
        }

    private fun foreignKeys(db: SupportSQLiteDatabase, table: String): Set<List<String?>> =
        f.rows(db, "PRAGMA foreign_key_list('$table')").map { fk ->
            listOf(fk.getValue("table"), fk.getValue("from"), fk.getValue("to"),
                fk.getValue("on_delete"), fk.getValue("on_update"))
        }.toSet()
}
