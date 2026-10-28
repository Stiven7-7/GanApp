package com.proyecto.ganapp.data.local.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigration9To10Test {

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
        dbName = "migration_9_10_test_${System.nanoTime()}"
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun baseVacia_validaSchemaYColumnaConDefaultCero() {
        helper.createDatabase(dbName, 9).close()

        val migrated = helper.runMigrationsAndValidate(dbName, 10, true, MIGRATION_9_10)
        val column = readFavoriteColumn(migrated)
        migrated.close()

        assertEquals("esFavorito", column.name)
        assertEquals("INTEGER", column.type)
        assertEquals(1, column.notNull)
        assertEquals("0", column.defaultValue)
    }

    @Test
    fun animalCompleto_conservaCamposYQuedaNoFavorito() {
        val userId = helper.createDatabase(dbName, 9).let { database ->
            val insertedUserId = database.insertUsuario("ana@example.com")
            database.insertAnimal(
                idAnimal = 42L,
                especie = "VACA",
                nombre = "Lola",
                raza = " Holstein ",
                edad = 4,
                peso = 420.5,
                color = "Negro",
                userId = insertedUserId,
                fotoUri = "content://ganapp/lola",
            )
            database.close()
            insertedUserId
        }

        val migrated = helper.runMigrationsAndValidate(dbName, 10, true, MIGRATION_9_10)
        val animal = readAnimal(migrated, 42L)
        migrated.close()

        assertEquals(42L, animal.idAnimal)
        assertEquals(userId, animal.idUsuario)
        assertEquals("VACA", animal.especie)
        assertEquals("Lola", animal.nombre)
        assertEquals(" Holstein ", animal.raza)
        assertEquals(4, animal.edad)
        assertEquals(420.5, animal.peso!!, 0.001)
        assertEquals("Negro", animal.color)
        assertEquals("content://ganapp/lola", animal.fotoUri)
        assertEquals(0, animal.esFavorito)
    }

    @Test
    fun nullablesYSentinel_seConservanSinInferirFavorito() {
        val userId = helper.createDatabase(dbName, 9).let { database ->
            val insertedUserId = database.insertUsuario("legacy@example.com")
            database.insertAnimal(
                idAnimal = 43L,
                especie = "SIN_ESPECIFICAR",
                nombre = "Legacy",
                raza = "SIN_ESPECIFICAR",
                edad = null,
                peso = null,
                color = null,
                userId = insertedUserId,
                fotoUri = null,
            )
            database.close()
            insertedUserId
        }

        val migrated = helper.runMigrationsAndValidate(dbName, 10, true, MIGRATION_9_10)
        val animal = readAnimal(migrated, 43L)
        migrated.close()

        assertEquals(43L, animal.idAnimal)
        assertEquals(userId, animal.idUsuario)
        assertEquals("SIN_ESPECIFICAR", animal.especie)
        assertEquals("Legacy", animal.nombre)
        assertEquals("SIN_ESPECIFICAR", animal.raza)
        assertNull(animal.edad)
        assertNull(animal.peso)
        assertNull(animal.color)
        assertNull(animal.fotoUri)
        assertEquals(0, animal.esFavorito)
    }

    @Test
    fun relacionesDeNotificacion_sobrevivenLaColumnaNueva() {
        helper.createDatabase(dbName, 9).apply {
            val userId = insertUsuario("avisos@example.com")
            insertAnimal(
                idAnimal = 44L,
                especie = "CERDO",
                nombre = "Pepa",
                raza = "Duroc",
                edad = 2,
                peso = 80.0,
                color = "Rosa",
                userId = userId,
                fotoUri = null,
            )
            val notificationId = insertNotificacion(userId)
            execSQL(
                "INSERT INTO notificacion_animal (idAnimal, idNotificacion) VALUES (?, ?)",
                arrayOf(44L, notificationId),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(dbName, 10, true, MIGRATION_9_10)
        val relation = readRelation(migrated)
        migrated.close()

        assertEquals(44L, relation.animalId)
        assertEquals("Pepa", relation.animalName)
        assertEquals(0, relation.esFavorito)
        assertEquals("Vacuna", relation.notificationName)
        assertEquals(relation.notificationId, relation.linkedNotificationId)
    }

    private fun SupportSQLiteDatabase.insertUsuario(correo: String): Long {
        execSQL(
            "INSERT INTO usuario (nombre, apellido, correo, contrasena, fechaRegistro, activo) VALUES (?, ?, ?, ?, ?, ?)",
            arrayOf("Ana", "Paz", correo, "synthetic-hash-not-a-real-secret", "2026-01-01", 1),
        )
        return query("SELECT last_insert_rowid()").use { cursor ->
            cursor.moveToFirst()
            cursor.getLong(0)
        }
    }

    private fun SupportSQLiteDatabase.insertAnimal(
        idAnimal: Long,
        especie: String,
        nombre: String,
        raza: String,
        edad: Int?,
        peso: Double?,
        color: String?,
        userId: Long,
        fotoUri: String?,
    ) {
        execSQL(
            "INSERT INTO animal (idAnimal, especie, nombre, raza, edad, peso, color, idUsuario, fotoUri) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf(idAnimal, especie, nombre, raza, edad, peso, color, userId, fotoUri),
        )
    }

    private fun SupportSQLiteDatabase.insertNotificacion(userId: Long): Long {
        execSQL(
            "INSERT INTO notificacion (nombre, tipo, descripcion, fechaInicio, fechaFin, seRepite, hora, dosisPorDia, intervaloHoras, estado, fechaCreacion, idUsuario) VALUES (?, ?, NULL, ?, NULL, ?, ?, NULL, NULL, ?, ?, ?)",
            arrayOf("Vacuna", "Salud", 20_000L, "NO", "08:00", "PENDIENTE", 1_700_000_000_000L, userId),
        )
        return query("SELECT last_insert_rowid()").use { cursor ->
            cursor.moveToFirst()
            cursor.getLong(0)
        }
    }

    private fun readFavoriteColumn(db: SupportSQLiteDatabase): FavoriteColumn {
        return db.query(
            "SELECT name, type, `notnull`, dflt_value FROM pragma_table_info('animal') WHERE name = 'esFavorito'",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            FavoriteColumn(
                name = cursor.getString(0),
                type = cursor.getString(1),
                notNull = cursor.getInt(2),
                defaultValue = cursor.getString(3),
            )
        }
    }

    private fun readAnimal(db: SupportSQLiteDatabase, idAnimal: Long): AnimalRow {
        return db.query(
            "SELECT idAnimal, idUsuario, especie, nombre, raza, edad, peso, color, fotoUri, esFavorito FROM animal WHERE idAnimal = ?",
            arrayOf(idAnimal),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            AnimalRow(
                idAnimal = cursor.getLong(0),
                idUsuario = cursor.getLong(1),
                especie = cursor.getString(2),
                nombre = cursor.getString(3),
                raza = cursor.getString(4),
                edad = if (cursor.isNull(5)) null else cursor.getInt(5),
                peso = if (cursor.isNull(6)) null else cursor.getDouble(6),
                color = if (cursor.isNull(7)) null else cursor.getString(7),
                fotoUri = if (cursor.isNull(8)) null else cursor.getString(8),
                esFavorito = cursor.getInt(9),
            )
        }
    }

    private fun readRelation(db: SupportSQLiteDatabase): RelationRow {
        return db.query(
            """
            SELECT a.idAnimal, a.nombre, a.esFavorito, n.idNotificacion, n.nombre, na.idNotificacion
            FROM notificacion_animal na
            JOIN animal a ON a.idAnimal = na.idAnimal
            JOIN notificacion n ON n.idNotificacion = na.idNotificacion
            """.trimIndent(),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            RelationRow(
                animalId = cursor.getLong(0),
                animalName = cursor.getString(1),
                esFavorito = cursor.getInt(2),
                notificationId = cursor.getLong(3),
                notificationName = cursor.getString(4),
                linkedNotificationId = cursor.getLong(5),
            )
        }
    }

    private data class FavoriteColumn(
        val name: String,
        val type: String,
        val notNull: Int,
        val defaultValue: String?,
    )

    private data class AnimalRow(
        val idAnimal: Long,
        val idUsuario: Long,
        val especie: String,
        val nombre: String,
        val raza: String,
        val edad: Int?,
        val peso: Double?,
        val color: String?,
        val fotoUri: String?,
        val esFavorito: Int,
    )

    private data class RelationRow(
        val animalId: Long,
        val animalName: String,
        val esFavorito: Int,
        val notificationId: Long,
        val notificationName: String,
        val linkedNotificationId: Long,
    )
}
