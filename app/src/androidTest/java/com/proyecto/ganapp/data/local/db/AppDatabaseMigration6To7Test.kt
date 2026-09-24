package com.proyecto.ganapp.data.local.db

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.proyecto.ganapp.data.local.entity.UsuarioEntity
import com.proyecto.ganapp.data.repository.impl.UsuarioRepositoryImpl
import com.proyecto.ganapp.domain.model.Usuario
import com.proyecto.ganapp.domain.repository.RegisterUserRepositoryResult
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigration6To7Test {

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
        dbName = "migration_6_7_test_${System.nanoTime()}"
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun m1_baseV6Vacia_migraYCreaIndiceUnique() {
        helper.createDatabase(dbName, 6).close()

        val migrated = helper.runMigrationsAndValidate(dbName, 7, true, MIGRATION_6_7)
        assertTrue(hasUniqueCorreoIndex(migrated))
        assertEquals(0, countUsuarios(migrated))
        migrated.close()
    }

    @Test
    fun m2_correoYaNormalizado_sePreserva() {
        helper.createDatabase(dbName, 6).apply {
            insertUsuario(
                nombre = "Ana",
                apellido = "Perez",
                correo = "user@example.com",
                passwordPlaceholder = SYNTHETIC_HASH,
                fechaRegistro = "2026-01-01",
                activo = 1,
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(dbName, 7, true, MIGRATION_6_7)
        val row = queryUsuario(migrated, 1L)
        assertEquals("user@example.com", row.correo)
        assertEquals("Ana", row.nombre)
        assertEquals("Perez", row.apellido)
        assertEquals(SYNTHETIC_HASH, row.contrasena)
        assertEquals("2026-01-01", row.fechaRegistro)
        assertEquals(1, row.activo)
        migrated.close()
    }

    @Test
    fun m3_mayusculasYEspacios_seNormalizan() {
        helper.createDatabase(dbName, 6).apply {
            insertUsuario(
                nombre = "Luis",
                apellido = "Gomez",
                correo = " User@Example.COM ",
                passwordPlaceholder = SYNTHETIC_HASH,
                fechaRegistro = "2026-02-02",
                activo = 1,
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(dbName, 7, true, MIGRATION_6_7)
        val row = queryUsuario(migrated, 1L)
        assertEquals("user@example.com", row.correo)
        assertEquals("Luis", row.nombre)
        assertEquals("Gomez", row.apellido)
        assertEquals(SYNTHETIC_HASH, row.contrasena)
        assertEquals("2026-02-02", row.fechaRegistro)
        migrated.close()
    }

    @Test
    fun m4_relacionesExistentes_sePreservanSinCascade() {
        helper.createDatabase(dbName, 6).apply {
            val userId = insertUsuario(
                nombre = "Marta",
                apellido = "Rios",
                correo = "marta@example.com",
                passwordPlaceholder = SYNTHETIC_HASH,
                fechaRegistro = "2026-03-03",
                activo = 1,
            )
            insertAnimal(userId)
            insertNotificacion(userId)
            close()
        }

        val migrated = helper.runMigrationsAndValidate(dbName, 7, true, MIGRATION_6_7)
        val usuario = queryUsuario(migrated, 1L)
        assertEquals("marta@example.com", usuario.correo)
        assertEquals(1, countByUsuario(migrated, "animal", 1L))
        assertEquals(1, countByUsuario(migrated, "notificacion", 1L))
        migrated.close()
    }

    @Test
    fun m5_duplicadoDespuesDeMigrar_abortaYNoReemplaza() {
        helper.createDatabase(dbName, 6).apply {
            insertUsuario(
                nombre = "Ana",
                apellido = "Perez",
                correo = "unique@example.com",
                passwordPlaceholder = SYNTHETIC_HASH,
                fechaRegistro = "2026-04-04",
                activo = 1,
            )
            close()
        }
        helper.runMigrationsAndValidate(dbName, 7, true, MIGRATION_6_7).close()

        val appDb = openAppDatabase()
        try {
            val dao = appDb.usuarioDao()
            runBlocking {
                val original = dao.getById(1L)
                requireNotNull(original)
                try {
                    dao.insert(
                        UsuarioEntity(
                            nombre = "Otra",
                            apellido = "Persona",
                            correo = "unique@example.com",
                            contrasena = "otro-hash",
                            fechaRegistro = "2026-04-05",
                            activo = true,
                        )
                    )
                    fail("Expected UNIQUE/ABORT on duplicate correo")
                } catch (_: SQLiteConstraintException) {
                    // expected
                }
                val after = dao.getById(1L)
                requireNotNull(after)
                assertEquals(original.nombre, after.nombre)
                assertEquals(original.correo, after.correo)
                assertEquals(original.contrasena, after.contrasena)
                assertEquals(1, dao.getAll().size)
            }
        } finally {
            appDb.close()
        }
    }

    @Test
    fun m5_repositorio_traduceConstraintADuplicateEmail() {
        helper.createDatabase(dbName, 6).close()
        helper.runMigrationsAndValidate(dbName, 7, true, MIGRATION_6_7).close()

        val appDb = openAppDatabase()
        try {
            val repository = UsuarioRepositoryImpl(appDb.usuarioDao())
            runBlocking {
                val first = repository.register(
                    Usuario(
                        nombre = "Ana",
                        apellido = "Perez",
                        correo = "repo@example.com",
                        contrasena = "password12",
                    )
                )
                assertTrue(first is RegisterUserRepositoryResult.Success)
                val firstId = (first as RegisterUserRepositoryResult.Success).userId
                assertTrue(firstId > 0)

                val second = repository.register(
                    Usuario(
                        nombre = "Otra",
                        apellido = "Persona",
                        correo = "repo@example.com",
                        contrasena = "password34",
                    )
                )
                assertEquals(RegisterUserRepositoryResult.DuplicateEmail, second)

                val stored = appDb.usuarioDao().getById(firstId)
                requireNotNull(stored)
                assertEquals("Ana", stored.nombre)
                assertEquals("repo@example.com", stored.correo)
                assertEquals(1, appDb.usuarioDao().getAll().size)
            }
        } finally {
            appDb.close()
        }
    }

    @Test
    fun m6_duplicadosNormalizadosEnV6_abortaSinEscribir() {
        helper.createDatabase(dbName, 6).apply {
            insertUsuario(
                nombre = "Uno",
                apellido = "A",
                correo = "User@Example.com",
                passwordPlaceholder = "hash-a",
                fechaRegistro = "2026-05-05",
                activo = 1,
            )
            insertUsuario(
                nombre = "Dos",
                apellido = "B",
                correo = " user@example.com ",
                passwordPlaceholder = "hash-b",
                fechaRegistro = "2026-05-06",
                activo = 1,
            )
            close()
        }

        try {
            helper.runMigrationsAndValidate(dbName, 7, true, MIGRATION_6_7)
            fail("Expected migration abort on normalized collisions")
        } catch (error: Throwable) {
            val messages = generateSequence(error) { it.cause }
                .mapNotNull { it.message }
                .joinToString(separator = " | ")
            assertTrue(messages.contains("duplicate normalized user emails"))
        }

        assertFalse(hasUniqueCorreoIndexOnFile())
        val sqlite = openRawDatabase()
        try {
            assertEquals(2, countUsuarios(sqlite))
            assertEquals("User@Example.com", queryCorreo(sqlite, 1L))
            assertEquals(" user@example.com ", queryCorreo(sqlite, 2L))
            assertEquals(6, sqlite.version)
        } finally {
            sqlite.close()
        }
    }

    private fun openAppDatabase(): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(MIGRATION_6_7)
            .build()
    }

    private fun openRawDatabase(): SQLiteDatabase {
        return SQLiteDatabase.openDatabase(
            context.getDatabasePath(dbName).path,
            null,
            SQLiteDatabase.OPEN_READONLY,
        )
    }

    private fun hasUniqueCorreoIndexOnFile(): Boolean {
        val sqlite = openRawDatabase()
        return try {
            sqlite.rawQuery("PRAGMA index_list(usuario)", null).use { cursor ->
                generateSequence { if (cursor.moveToNext()) cursor else null }
                    .any { row ->
                        row.getString(row.getColumnIndexOrThrow("name")) == "index_usuario_correo" &&
                            row.getInt(row.getColumnIndexOrThrow("unique")) == 1
                    }
            }
        } catch (_: Exception) {
            false
        } finally {
            sqlite.close()
        }
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

    private fun SupportSQLiteDatabase.insertAnimal(userId: Long) {
        execSQL(
            "INSERT INTO animal (nombre, edad, peso, color, raza, idUsuario, fotoUri) VALUES (?, ?, ?, ?, ?, ?, ?)",
            arrayOf("Vaca", 3, 350.5, "Blanco", "Holstein", userId, null),
        )
    }

    private fun SupportSQLiteDatabase.insertNotificacion(userId: Long) {
        execSQL(
            "INSERT INTO notificacion (nombre, tipo, descripcion, fechaInicio, fechaFin, seRepite, hora, dosisPorDia, intervaloHoras, estado, fechaCreacion, idUsuario) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf(
                "Vacuna",
                "SALUD",
                null,
                1_700_000_000_000L,
                null,
                "NO",
                "08:00",
                null,
                null,
                "PENDIENTE",
                1_700_000_000_000L,
                userId,
            ),
        )
    }

    private fun queryUsuario(db: SupportSQLiteDatabase, id: Long): UsuarioRow {
        return db.query(
            "SELECT idUsuario, nombre, apellido, correo, contrasena, fechaRegistro, activo FROM usuario WHERE idUsuario = ?",
            arrayOf(id),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            UsuarioRow(
                idUsuario = cursor.getLong(0),
                nombre = cursor.getString(1),
                apellido = cursor.getString(2),
                correo = cursor.getString(3),
                contrasena = cursor.getString(4),
                fechaRegistro = cursor.getString(5),
                activo = cursor.getInt(6),
            )
        }
    }

    private fun countUsuarios(db: SupportSQLiteDatabase): Int {
        return db.query("SELECT COUNT(*) FROM usuario").use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
    }

    private fun countUsuarios(db: SQLiteDatabase): Int {
        return db.rawQuery("SELECT COUNT(*) FROM usuario", null).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
    }

    private fun queryCorreo(db: SQLiteDatabase, id: Long): String {
        return db.rawQuery(
            "SELECT correo FROM usuario WHERE idUsuario = ?",
            arrayOf(id.toString()),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            cursor.getString(0)
        }
    }

    private fun countByUsuario(db: SupportSQLiteDatabase, table: String, userId: Long): Int {
        return db.query("SELECT COUNT(*) FROM $table WHERE idUsuario = ?", arrayOf(userId)).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
    }

    private fun hasUniqueCorreoIndex(db: SupportSQLiteDatabase): Boolean {
        return db.query("PRAGMA index_list(usuario)").use { cursor ->
            generateSequence { if (cursor.moveToNext()) cursor else null }
                .any { row ->
                    row.getString(row.getColumnIndexOrThrow("name")) == "index_usuario_correo" &&
                        row.getInt(row.getColumnIndexOrThrow("unique")) == 1
                }
        }
    }

    private data class UsuarioRow(
        val idUsuario: Long,
        val nombre: String,
        val apellido: String,
        val correo: String,
        val contrasena: String,
        val fechaRegistro: String,
        val activo: Int,
    )

    private companion object {
        const val SYNTHETIC_HASH = "synthetic-hash-not-a-real-secret"
    }
}
