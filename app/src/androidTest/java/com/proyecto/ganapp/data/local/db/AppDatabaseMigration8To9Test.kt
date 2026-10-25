package com.proyecto.ganapp.data.local.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import java.time.ZoneId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigration8To9Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
    )

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var dbName: String

    @Before
    fun setUp() {
        dbName = "migration_8_9_test_${System.nanoTime()}"
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun fechaFinNula_permaneceNulaYFechaInicioPasaAEpochDay() {
        val bogota = ZoneId.of("America/Bogota")
        val date = LocalDate.of(2026, 1, 31)
        val legacyMillis = date.atStartOfDay(bogota).toInstant().toEpochMilli()
        helper.createDatabase(dbName, 8).apply {
            val userId = insertUsuario()
            insertNotificacion(userId, legacyMillis, null)
            close()
        }

        val migrated = helper.runMigrationsAndValidate(dbName, 9, true, MIGRATION_8_9)
        val row = readDates(migrated)
        migrated.close()

        assertEquals(epochMillisToEpochDay(legacyMillis, ZoneId.systemDefault()), row.fechaInicio)
        assertNull(row.fechaFin)
        assertEquals("Vacuna", row.nombre)
        assertEquals("08:00", row.hora)
    }

    @Test
    fun fechaFinPresente_ambasFechasPasanAEpochDay() {
        val losAngeles = ZoneId.of("America/Los_Angeles")
        val start = LocalDate.of(2026, 6, 10)
        val end = LocalDate.of(2026, 6, 12)
        val startMillis = start.atStartOfDay(losAngeles).toInstant().toEpochMilli()
        val endMillis = end.atStartOfDay(losAngeles).toInstant().toEpochMilli()
        helper.createDatabase(dbName, 8).apply {
            val userId = insertUsuario()
            insertNotificacion(userId, startMillis, endMillis)
            close()
        }

        val migrated = helper.runMigrationsAndValidate(dbName, 9, true, MIGRATION_8_9)
        val row = readDates(migrated)
        migrated.close()

        val zone = ZoneId.systemDefault()
        assertEquals(epochMillisToEpochDay(startMillis, zone), row.fechaInicio)
        assertEquals(epochMillisToEpochDay(endMillis, zone), row.fechaFin)
        assertTrue(row.fechaInicio > 10_000L)
    }

    private fun SupportSQLiteDatabase.insertUsuario(): Long {
        execSQL(
            "INSERT INTO usuario (nombre, apellido, correo, contrasena, fechaRegistro, activo) VALUES (?, ?, ?, ?, ?, ?)",
            arrayOf("Ana", "Paz", "ana@example.com", "synthetic-hash-not-a-real-secret", "2026-01-01", 1),
        )
        return query("SELECT last_insert_rowid()").use { cursor ->
            cursor.moveToFirst()
            cursor.getLong(0)
        }
    }

    private fun SupportSQLiteDatabase.insertNotificacion(
        userId: Long,
        fechaInicio: Long,
        fechaFin: Long?,
    ) {
        execSQL(
            "INSERT INTO notificacion (nombre, tipo, descripcion, fechaInicio, fechaFin, seRepite, hora, dosisPorDia, intervaloHoras, estado, fechaCreacion, idUsuario) VALUES (?, ?, NULL, ?, ?, ?, ?, NULL, NULL, ?, ?, ?)",
            arrayOf(
                "Vacuna",
                "Vacunación",
                fechaInicio,
                fechaFin,
                "DIARIA",
                "08:00",
                "PENDIENTE",
                1_700_000_000_000L,
                userId,
            ),
        )
    }

    private fun readDates(db: SupportSQLiteDatabase): DateRow {
        return db.query(
            "SELECT nombre, hora, fechaInicio, fechaFin FROM notificacion",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            val finIndex = cursor.getColumnIndexOrThrow("fechaFin")
            DateRow(
                nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                hora = cursor.getString(cursor.getColumnIndexOrThrow("hora")),
                fechaInicio = cursor.getLong(cursor.getColumnIndexOrThrow("fechaInicio")),
                fechaFin = if (cursor.isNull(finIndex)) null else cursor.getLong(finIndex),
            )
        }
    }

    private data class DateRow(
        val nombre: String,
        val hora: String,
        val fechaInicio: Long,
        val fechaFin: Long?,
    )
}
