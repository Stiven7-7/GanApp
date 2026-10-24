package com.proyecto.ganapp.data.local.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.proyecto.ganapp.domain.model.EspecieAnimal
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigration7To8Test {

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
        dbName = "migration_7_8_test_${System.nanoTime()}"
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun dbVacia_migraYValidaSchema8() {
        helper.createDatabase(dbName, 7).close()

        val migrated = helper.runMigrationsAndValidate(dbName, 8, true, MIGRATION_7_8)
        assertTrue(hasIdUsuarioIndex(migrated))
        assertTrue(hasUsuarioCascade(migrated))
        assertEquals(0, countAnimals(migrated))
        migrated.close()
    }

    @Test
    fun historicos_especieLegacyYDatosPreservados() {
        helper.createDatabase(dbName, 7).apply {
            val userId = insertUsuario(
                nombre = "Marta",
                apellido = "Rios",
                correo = "marta@example.com",
                passwordPlaceholder = SYNTHETIC_HASH,
                fechaRegistro = "2026-03-03",
                activo = 1,
            )
            insertLegacyAnimal(
                idAnimal = 101L,
                nombre = "Lola",
                edad = 5,
                peso = 320.5,
                color = "Blanco",
                raza = "Holstein",
                userId = userId,
                fotoUri = "content://ganapp/lola",
            )
            insertLegacyAnimal(
                idAnimal = 102L,
                nombre = "Manchita",
                edad = 3,
                peso = 210.0,
                color = "Pinto",
                raza = null,
                userId = userId,
                fotoUri = null,
            )
            insertLegacyAnimal(
                idAnimal = 103L,
                nombre = "Copito",
                edad = 1,
                peso = 40.0,
                color = "Blanco",
                raza = "   ",
                userId = userId,
                fotoUri = null,
            )
            insertLegacyAnimal(
                idAnimal = 104L,
                nombre = "Cero",
                edad = 0,
                peso = 0.0,
                color = "",
                raza = "Brahman",
                userId = userId,
                fotoUri = "content://ganapp/cero",
            )
            insertLegacyAnimal(
                idAnimal = 105L,
                nombre = "Espaciada",
                edad = 2,
                peso = 15.0,
                color = "Gris",
                raza = " Holstein ",
                userId = userId,
                fotoUri = null,
            )
            val notificationId = insertNotificacion(userId)
            execSQL(
                "INSERT INTO notificacion_animal (idAnimal, idNotificacion) VALUES (?, ?)",
                arrayOf(101L, notificationId),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(dbName, 8, true, MIGRATION_7_8)
        assertEquals(5, countAnimals(migrated))
        assertTrue(hasIdUsuarioIndex(migrated))
        assertTrue(hasUsuarioCascade(migrated))

        val lola = queryAnimal(migrated, 101L)
        assertEquals("SIN_ESPECIFICAR", lola.especie)
        assertEquals("Lola", lola.nombre)
        assertEquals("Holstein", lola.raza)
        assertEquals(5, lola.edad)
        assertEquals(320.5f, lola.peso!!, 0.001f)
        assertEquals("Blanco", lola.color)
        assertEquals("content://ganapp/lola", lola.fotoUri)
        assertEquals(1L, lola.idUsuario)
        assertEquals(101L, lola.idAnimal)

        val manchita = queryAnimal(migrated, 102L)
        assertEquals("Manchita", manchita.nombre)
        assertEquals("SIN_ESPECIFICAR", manchita.especie)
        assertEquals("SIN_ESPECIFICAR", manchita.raza)
        assertEquals(3, manchita.edad)
        assertEquals(210f, manchita.peso!!, 0.001f)
        assertEquals("Pinto", manchita.color)
        assertNull(manchita.fotoUri)
        assertEquals(1L, manchita.idUsuario)
        assertEquals(102L, manchita.idAnimal)

        val copito = queryAnimal(migrated, 103L)
        assertEquals("SIN_ESPECIFICAR", copito.especie)
        assertEquals("SIN_ESPECIFICAR", copito.raza)

        val cero = queryAnimal(migrated, 104L)
        assertEquals("SIN_ESPECIFICAR", cero.especie)
        assertEquals("Brahman", cero.raza)
        assertNotNull(cero.edad)
        assertEquals(0, cero.edad)
        assertNotNull(cero.peso)
        assertEquals(0f, cero.peso!!, 0f)
        assertEquals("", cero.color)
        assertEquals("content://ganapp/cero", cero.fotoUri)
        assertEquals(104L, cero.idAnimal)
        assertEquals(1L, cero.idUsuario)

        val espaciada = queryAnimal(migrated, 105L)
        assertEquals("SIN_ESPECIFICAR", espaciada.especie)
        assertEquals(" Holstein ", espaciada.raza)

        assertEquals(1, countLinks(migrated, 101L))
        migrated.close()
    }

    @Test
    fun deleteUsuario_eliminaAnimalPorCascade() {
        helper.createDatabase(dbName, 7).apply {
            val userId = insertUsuario(
                nombre = "Ana",
                apellido = "Perez",
                correo = "ana@example.com",
                passwordPlaceholder = SYNTHETIC_HASH,
                fechaRegistro = "2026-04-04",
                activo = 1,
            )
            insertLegacyAnimal(
                idAnimal = 201L,
                nombre = "Manchita",
                edad = 4,
                peso = 300.0,
                color = "Pinto",
                raza = "Holstein",
                userId = userId,
                fotoUri = null,
            )
            close()
        }
        helper.runMigrationsAndValidate(dbName, 8, true, MIGRATION_7_8).close()

        val appDb = openAppDatabase()
        try {
            runBlocking {
                val before = appDb.animalDao().getById(201L)
                requireNotNull(before)
                assertEquals(EspecieAnimal.SIN_ESPECIFICAR, before.especie)
                assertEquals("Holstein", before.raza)
                assertEquals("Manchita", before.nombre)
                assertEquals(201L, before.idAnimal)

                appDb.openHelper.writableDatabase.execSQL(
                    "DELETE FROM usuario WHERE idUsuario = ?",
                    arrayOf(before.idUsuario),
                )

                assertNull(appDb.animalDao().getById(201L))
            }
        } finally {
            appDb.close()
        }
    }

    private fun openAppDatabase(): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(MIGRATION_6_7, MIGRATION_7_8)
            .build()
    }

    private fun SupportSQLiteDatabase.insertUsuario(
        nombre: String,
        apellido: String,
        correo: String,
        passwordPlaceholder: String,
        fechaRegistro: String,
        activo: Int,
    ): Long {
        execSQL(
            "INSERT INTO usuario (nombre, apellido, correo, contrasena, fechaRegistro, activo) VALUES (?, ?, ?, ?, ?, ?)",
            arrayOf(nombre, apellido, correo, passwordPlaceholder, fechaRegistro, activo),
        )
        return query("SELECT last_insert_rowid()").use { cursor ->
            cursor.moveToFirst()
            cursor.getLong(0)
        }
    }

    private fun SupportSQLiteDatabase.insertLegacyAnimal(
        idAnimal: Long,
        nombre: String,
        edad: Int,
        peso: Double,
        color: String,
        raza: String?,
        userId: Long,
        fotoUri: String?,
    ) {
        val razaSql = if (raza == null) "NULL" else "?"
        val fotoSql = if (fotoUri == null) "NULL" else "?"
        val args = mutableListOf<Any>(idAnimal, nombre, edad, peso, color)
        if (raza != null) {
            args += raza
        }
        args += userId
        if (fotoUri != null) {
            args += fotoUri
        }
        execSQL(
            "INSERT INTO animal (idAnimal, nombre, edad, peso, color, raza, idUsuario, fotoUri) " +
                "VALUES (?, ?, ?, ?, ?, $razaSql, ?, $fotoSql)",
            args.toTypedArray(),
        )
    }

    private fun SupportSQLiteDatabase.insertNotificacion(userId: Long): Long {
        execSQL(
            "INSERT INTO notificacion (nombre, tipo, descripcion, fechaInicio, fechaFin, seRepite, hora, dosisPorDia, intervaloHoras, estado, fechaCreacion, idUsuario) VALUES (?, ?, NULL, ?, NULL, ?, ?, NULL, NULL, ?, ?, ?)",
            arrayOf(
                "Vacuna",
                "SALUD",
                1_700_000_000_000L,
                "NO",
                "08:00",
                "PENDIENTE",
                1_700_000_000_000L,
                userId,
            ),
        )
        return query("SELECT last_insert_rowid()").use { cursor ->
            cursor.moveToFirst()
            cursor.getLong(0)
        }
    }

    private fun queryAnimal(db: SupportSQLiteDatabase, id: Long): AnimalRow {
        return db.query(
            "SELECT idAnimal, especie, nombre, raza, edad, peso, color, idUsuario, fotoUri FROM animal WHERE idAnimal = ?",
            arrayOf(id),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            AnimalRow(
                idAnimal = cursor.getLong(cursor.getColumnIndexOrThrow("idAnimal")),
                especie = cursor.getString(cursor.getColumnIndexOrThrow("especie")),
                nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                raza = cursor.getString(cursor.getColumnIndexOrThrow("raza")),
                edad = cursor.optionalInt("edad"),
                peso = cursor.optionalFloat("peso"),
                color = cursor.optionalString("color"),
                idUsuario = cursor.getLong(cursor.getColumnIndexOrThrow("idUsuario")),
                fotoUri = cursor.optionalString("fotoUri"),
            )
        }
    }

    private fun android.database.Cursor.optionalString(column: String): String? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getString(index)
    }

    private fun android.database.Cursor.optionalInt(column: String): Int? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getInt(index)
    }

    private fun android.database.Cursor.optionalFloat(column: String): Float? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getFloat(index)
    }

    private fun countAnimals(db: SupportSQLiteDatabase): Int {
        return db.query("SELECT COUNT(*) FROM animal").use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
    }

    private fun countLinks(db: SupportSQLiteDatabase, animalId: Long): Int {
        return db.query(
            "SELECT COUNT(*) FROM notificacion_animal WHERE idAnimal = ?",
            arrayOf(animalId),
        ).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
    }

    private fun hasIdUsuarioIndex(db: SupportSQLiteDatabase): Boolean {
        return db.query("PRAGMA index_list(animal)").use { cursor ->
            generateSequence { if (cursor.moveToNext()) cursor else null }
                .any { row ->
                    row.getString(row.getColumnIndexOrThrow("name")) == "index_animal_idUsuario" &&
                        row.getInt(row.getColumnIndexOrThrow("unique")) == 0
                }
        }
    }

    private fun hasUsuarioCascade(db: SupportSQLiteDatabase): Boolean {
        return db.query("PRAGMA foreign_key_list(animal)").use { cursor ->
            generateSequence { if (cursor.moveToNext()) cursor else null }
                .any { row ->
                    row.getString(row.getColumnIndexOrThrow("table")) == "usuario" &&
                        row.getString(row.getColumnIndexOrThrow("from")) == "idUsuario" &&
                        row.getString(row.getColumnIndexOrThrow("to")) == "idUsuario" &&
                        row.getString(row.getColumnIndexOrThrow("on_delete"))
                            .equals("CASCADE", ignoreCase = true)
                }
        }
    }

    private data class AnimalRow(
        val idAnimal: Long,
        val especie: String,
        val nombre: String,
        val raza: String,
        val edad: Int?,
        val peso: Float?,
        val color: String?,
        val idUsuario: Long,
        val fotoUri: String?,
    )

    private companion object {
        const val SYNTHETIC_HASH = "synthetic-hash-not-a-real-secret"
    }
}
