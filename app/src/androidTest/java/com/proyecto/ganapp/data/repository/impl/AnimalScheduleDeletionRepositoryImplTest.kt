package com.proyecto.ganapp.data.repository.impl

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.proyecto.ganapp.data.local.db.AppDatabase
import com.proyecto.ganapp.data.local.entity.AnimalEntity
import com.proyecto.ganapp.data.local.entity.PlantillaNotificacionEntity
import com.proyecto.ganapp.data.local.entity.ProgramacionNotificacionEntity
import com.proyecto.ganapp.data.local.entity.UsuarioEntity
import com.proyecto.ganapp.domain.model.EspecieAnimal
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnimalScheduleDeletionRepositoryImplTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun transaccionEliminaProgramacionesYAnimalYConservaPlantilla() = runBlocking {
        seed()
        AnimalScheduleDeletionRepositoryImpl(database).deleteAnimalAndSchedules(7L)

        assertNull(database.animalDao().getById(7L))
        assertEquals(emptyList<ProgramacionNotificacionEntity>(), database.programacionNotificacionDao().getByAnimal(7L))
        assertNotNull(database.plantillaNotificacionDao().getById(10L))
        assertNotNull(database.usuarioDao().getById(1L))
    }

    @Test
    fun borrarAnimalConProgramacionVigenteFallaPorRestrictYNoBorraNada() = runBlocking {
        seed()
        try {
            database.animalDao().deleteById(7L)
            fail("RESTRICT debió impedir el delete del animal")
        } catch (exception: SQLiteConstraintException) {
            assertNotNull(exception)
        }
        assertNotNull(database.animalDao().getById(7L))
        assertEquals(2, database.programacionNotificacionDao().getByAnimal(7L).size)
        assertNotNull(database.plantillaNotificacionDao().getById(10L))
    }

    private suspend fun seed() {
        database.usuarioDao().insert(
            UsuarioEntity(
                idUsuario = 1L,
                nombre = "Ana",
                apellido = "Paz",
                correo = "ana@example.test",
                contrasena = "synthetic",
                fechaRegistro = "2026-01-01",
                activo = true,
            ),
        )
        database.animalDao().insert(
            AnimalEntity(
                idAnimal = 7L,
                especie = EspecieAnimal.VACA,
                nombre = "Mancha",
                raza = "Holstein",
                edad = 4,
                peso = 420f,
                color = "Negro",
                idUsuario = 1L,
                fotoUri = null,
            ),
        )
        database.plantillaNotificacionDao().insert(
            PlantillaNotificacionEntity(
                idPlantilla = 10L,
                titulo = "Vacuna",
                tipo = "Vacunación",
                idUsuario = 1L,
            ),
        )
        listOf(1L, 2L).forEach { id ->
            database.programacionNotificacionDao().insert(
                ProgramacionNotificacionEntity(
                    idProgramacion = id,
                    idPlantilla = 10L,
                    idAnimal = 7L,
                    fechaInicio = 20_000L,
                    fechaFin = 20_010L,
                    hora = "09:00",
                    recurrencia = "DIARIA",
                ),
            )
        }
    }
}
