package com.proyecto.ganapp.data.local.db

import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue

/** Solo bases con nombre exclusivo de tests; nunca abre ganapp_db. */
internal class Migration10To11RehearsalFixture(private val helper: MigrationTestHelper) {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databases = linkedMapOf<String, SupportSQLiteDatabase>()

    fun create(version: Int = 10): SupportSQLiteDatabase {
        val name = "rehearsal_10_11_${System.nanoTime()}"
        val db = helper.createDatabase(name, version)
        databases[name] = db
        db.setForeignKeyConstraintsEnabled(true)
        assertEquals(version, db.version)
        return db
    }

    fun close() {
        databases.forEach { (name, db) ->
            db.close()
            context.deleteDatabase(name)
        }
        databases.clear()
    }

    /** Simula la transacción del upgrade. No sustituye `runMigrationsAndValidate`. */
    fun rehearse(db: SupportSQLiteDatabase, vararg migrations: Migration) {
        val path = if (migrations.isEmpty()) listOf(MIGRATION_10_11) else migrations.toList()
        db.beginTransaction()
        try {
            path.forEach { it.migrate(db) }
            db.version = 11
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        assertEquals(11, db.version)
        assertTrue(rows(db, "PRAGMA foreign_key_check").isEmpty())
    }

    /** Cierra la base sembrada y valida el resultado contra el esquema exportado v11. */
    fun official(db: SupportSQLiteDatabase, vararg migrations: Migration): SupportSQLiteDatabase {
        val name = databaseName(db)
        db.close()
        databases.remove(name)
        val steps = if (migrations.isEmpty()) arrayOf(MIGRATION_10_11) else migrations
        val validated = helper.runMigrationsAndValidate(name, 11, true, *steps)
        databases[name] = validated
        return validated
    }

    fun databaseName(db: SupportSQLiteDatabase): String =
        databases.entries.first { it.value === db }.key

    fun databaseFile(name: String) = context.getDatabasePath(name)

    fun deleteDatabase(name: String) {
        databases.remove(name)?.close()
        context.deleteDatabase(name)
    }

    fun corrupt(db: SupportSQLiteDatabase, seed: () -> Unit) {
        assertFalse(db.inTransaction())
        db.setForeignKeyConstraintsEnabled(false)
        try {
            seed()
        } finally {
            db.setForeignKeyConstraintsEnabled(true)
        }
    }

    fun assertAbortAndRollback(db: SupportSQLiteDatabase, message: String) {
        val before = snapshot(db)
        val failure = assertThrows(RuntimeException::class.java) { rehearse(db) }
        assertTrue(failure.toString(), failure.message.orEmpty().contains(message))
        assertEquals(before, snapshot(db))
        assertTrue(tableExists(db, "notificacion"))
        assertTrue(tableExists(db, "notificacion_animal"))
        assertFalse(tableExists(db, "plantilla_notificacion"))
        assertFalse(tableExists(db, "programacion_notificacion"))
        assertEquals(10, db.version)
    }

    fun user(db: SupportSQLiteDatabase, id: Long = 1L) {
        db.execSQL(
            "INSERT INTO usuario (idUsuario, nombre, apellido, correo, contrasena, fechaRegistro, activo) VALUES (?, ?, ?, ?, ?, ?, ?)",
            arrayOf(id, "Ana-$id", "Paz", "user$id@example.test", "synthetic-not-a-real-secret-$id", "2026-01-01", if (id % 2L == 0L) 0 else 1),
        )
    }

    fun animal(
        db: SupportSQLiteDatabase,
        id: Long = 7L,
        owner: Long = 1L,
        favorite: Int = 1,
        nullables: Boolean = false,
    ) {
        db.execSQL(
            "INSERT INTO animal (idAnimal, especie, nombre, raza, edad, peso, color, idUsuario, fotoUri) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(
                id, "VACA", "Mancha-$id", if (nullables) "SIN_ESPECIFICAR" else " Holstein ",
                if (nullables) null else 4, if (nullables) null else 420.5,
                if (nullables) null else "Negro", owner, if (nullables) null else "content://test/$id",
            ),
        )
        if (rows(db, "PRAGMA table_info('animal')").any { it.getValue("name") == "esFavorito" }) {
            db.execSQL("UPDATE animal SET esFavorito = ? WHERE idAnimal = ?", arrayOf(favorite, id))
        }
    }

    fun notification(
        db: SupportSQLiteDatabase,
        id: Long = 10L,
        owner: Long = 1L,
        title: String = " Vacuna ",
        type: String = "Vacunación",
        start: Long = 20_000L,
        end: Long? = 20_010L,
        recurrence: String = "DIARIA",
        time: String = "09:37",
        description: String? = "Descripción anterior",
        doses: Int? = 2,
        interval: Int? = 12,
    ) {
        db.execSQL(
            """
            INSERT INTO notificacion (
                idNotificacion, nombre, tipo, descripcion, fechaInicio, fechaFin, seRepite,
                hora, dosisPorDia, intervaloHoras, estado, fechaCreacion, idUsuario
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf(id, title, type, description, start, end, recurrence, time,
                doses, interval, "COMPLETADA", 1_700_000_000_123L, owner),
        )
    }

    fun link(db: SupportSQLiteDatabase, notification: Long = 10L, animal: Long = 7L) {
        db.execSQL("INSERT INTO notificacion_animal (idAnimal, idNotificacion) VALUES (?, ?)",
            arrayOf(animal, notification))
    }

    fun rows(
        db: SupportSQLiteDatabase,
        sql: String,
        vararg args: Any?,
    ): List<Map<String, String?>> = db.query(sql, args).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(cursor.columnNames.indices.associate { index ->
                    cursor.getColumnName(index) to if (cursor.isNull(index)) null else cursor.getString(index)
                })
            }
        }
    }

    fun scalar(db: SupportSQLiteDatabase, sql: String, vararg args: Any?): Long =
        db.query(sql, args).use { cursor ->
            assertTrue(cursor.moveToFirst())
            cursor.getLong(0)
        }

    fun tableExists(db: SupportSQLiteDatabase, table: String): Boolean =
        scalar(db, "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?", table) == 1L

    fun templates(db: SupportSQLiteDatabase) = rows(db, "SELECT * FROM plantilla_notificacion ORDER BY idPlantilla")
    fun schedules(db: SupportSQLiteDatabase) = rows(db, "SELECT * FROM programacion_notificacion ORDER BY idProgramacion")

    fun assertSchedule(
        row: Map<String, String?>,
        template: Long = 10L,
        animal: Long? = 7L,
        start: Long = 20_000L,
        end: Long = 20_010L,
        time: String = "09:37",
        recurrence: String = "DIARIA",
    ) {
        assertTrue(row.getValue("idProgramacion")!!.toLong() > 0L)
        assertEquals(template.toString(), row.getValue("idPlantilla"))
        assertEquals(animal?.toString(), row.getValue("idAnimal"))
        assertEquals(start.toString(), row.getValue("fechaInicio"))
        assertEquals(end.toString(), row.getValue("fechaFin"))
        assertEquals(time, row.getValue("hora"))
        assertEquals(recurrence, row.getValue("recurrencia"))
    }

    private fun snapshot(db: SupportSQLiteDatabase): Snapshot {
        val metadata = rows(db, "SELECT type, name, tbl_name, sql FROM sqlite_master ORDER BY type, name")
        val tables = metadata.filter { it.getValue("type") == "table" }.map { it.getValue("name")!! }
        val contents = tables.associateWith { table -> rows(db, "SELECT * FROM `$table` ORDER BY rowid") }
        return Snapshot(db.version, metadata, contents)
    }

    private data class Snapshot(
        val version: Int,
        val metadata: List<Map<String, String?>>,
        val contents: Map<String, List<Map<String, String?>>>,
    )
}
