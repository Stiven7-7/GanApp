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

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigration10To11CorruptionTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java, emptyList(),
    )
    private val f = Migration10To11RehearsalFixture(helper)

    @After
    fun cleanup() = f.close()

    @Test
    fun crossUser_abortaYConservaTodasLasTablasYDatosLegacy() {
        val db = valid()
        f.user(db, 2L)
        db.execSQL("UPDATE animal SET idUsuario = 2 WHERE idAnimal = 7")
        f.assertAbortAndRollback(db, "cross-user")
    }

    @Test
    fun animalInexistente_abortaYHaceRollback() {
        val db = valid(linked = false)
        f.corrupt(db) { f.link(db, animal = 404L) }
        f.assertAbortAndRollback(db, "legacy foreign key violation")
    }

    @Test
    fun notificacionInexistente_abortaYHaceRollback() {
        val db = valid(linked = false)
        f.corrupt(db) { f.link(db, notification = 404L) }
        f.assertAbortAndRollback(db, "legacy foreign key violation")
    }

    @Test
    fun usuarioInexistente_abortaYHaceRollback() {
        val db = valid()
        f.corrupt(db) { db.execSQL("UPDATE notificacion SET idUsuario = 404 WHERE idNotificacion = 10") }
        f.assertAbortAndRollback(db, "legacy foreign key violation")
    }

    @Test
    fun tipoDesconocido_abortaSinFallbackYHaceRollback() {
        val db = valid()
        db.execSQL("UPDATE notificacion SET tipo = 'OTRO' WHERE idNotificacion = 10")
        f.assertAbortAndRollback(db, "unknown notification type")
    }

    @Test
    fun recurrenciaDesconocida_abortaSinFallbackYHaceRollback() {
        val db = valid()
        db.execSQL("UPDATE notificacion SET seRepite = 'A VECES' WHERE idNotificacion = 10")
        f.assertAbortAndRollback(db, "unknown recurrence")
    }

    @Test
    fun horasInvalidasOSegundos_abortanSinCorreccionYConRollback() {
        listOf("25:00", "09:99", "abc", "09:00:30").forEach { time ->
            val db = valid()
            db.execSQL("UPDATE notificacion SET hora = ? WHERE idNotificacion = 10", arrayOf(time))
            f.assertAbortAndRollback(db, "Invalid notification schedule time")
        }
    }

    @Test
    fun falloSQLDespuesDeCrearTablas_revierteTambienDDLParcial() {
        val db = valid()
        // Inyección de fallo exclusiva de test: el último CREATE INDEX colisionará.
        db.execSQL("CREATE INDEX index_programacion_notificacion_idAnimal ON animal (idAnimal)")
        f.assertAbortAndRollback(db, "index_programacion_notificacion_idAnimal")
    }

    private fun valid(linked: Boolean = true): SupportSQLiteDatabase {
        val db = f.create()
        f.user(db)
        f.animal(db)
        f.notification(db)
        if (linked) f.link(db)
        return db
    }
}
