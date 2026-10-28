package com.proyecto.ganapp.data.local.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.proyecto.ganapp.data.local.db.AppDatabase
import com.proyecto.ganapp.data.local.entity.AnimalEntity
import com.proyecto.ganapp.data.local.entity.UsuarioEntity
import com.proyecto.ganapp.domain.model.EspecieAnimal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnimalDaoFavoriteQueryTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: AnimalDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.animalDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun favoritos_respetanUsuarioYEstado() = runBlocking {
        val userA = insertUser("ana@example.com")
        val userB = insertUser("bruno@example.com")
        val favoriteA = insertAnimal(userA, "Lola", favorite = true)
        val plainA = insertAnimal(userA, "Nube", favorite = false)
        val favoriteB = insertAnimal(userB, "Rosado", favorite = true)

        val ids = dao.getFavoriteAnimalsByUser(userA).awaitIds()

        assertEquals(setOf(favoriteA), ids)
        assertTrue(plainA !in ids)
        assertTrue(favoriteB !in ids)
    }

    @Test
    fun usuarioSinFavoritos_emiteListaVacia() = runBlocking {
        val userId = insertUser("vacio@example.com")
        insertAnimal(userId, "Lola", favorite = false)

        val ids = dao.getFavoriteAnimalsByUser(userId).awaitIds()

        assertEquals(emptySet<Long>(), ids)
    }

    @Test
    fun variosFavoritosDelMismoUsuario_aparecenTodos() = runBlocking {
        val userId = insertUser("varios@example.com")
        val ids = setOf(
            insertAnimal(userId, "Nube", favorite = true),
            insertAnimal(userId, "Lola", favorite = true),
            insertAnimal(userId, "Manolo", favorite = true),
            insertAnimal(userId, "Ana", favorite = true),
        )
        insertAnimal(userId, "Oculto", favorite = false)

        val favorites = dao.getFavoriteAnimalsByUser(userId).awaitIds()

        assertEquals(ids, favorites)
    }

    @Test
    fun falseATrue_emiteElAnimalEnLaMismaObservacion() = runBlocking {
        val userId = insertUser("alta@example.com")
        val animalId = insertAnimal(userId, "Lola", favorite = false)
        val observation = observe(dao.getFavoriteAnimalsByUser(userId))

        assertEquals(emptySet<Long>(), observation.next())

        val stored = dao.getById(animalId)!!
        dao.update(stored.copy(esFavorito = true))

        assertEquals(setOf(animalId), observation.nextMatching(setOf(animalId)))
        observation.cancel()
    }

    @Test
    fun trueAFalse_dejaDeEmitirElAnimal() = runBlocking {
        val userId = insertUser("baja@example.com")
        val animalId = insertAnimal(userId, "Lola", favorite = true)
        val observation = observe(dao.getFavoriteAnimalsByUser(userId))

        assertEquals(setOf(animalId), observation.next())

        val stored = dao.getById(animalId)!!
        dao.update(stored.copy(esFavorito = false))

        assertEquals(emptySet<Long>(), observation.nextMatching(emptySet()))
        observation.cancel()
    }

    private suspend fun insertUser(correo: String): Long {
        return database.usuarioDao().insert(
            UsuarioEntity(
                nombre = "Ana",
                apellido = "Perez",
                correo = correo,
                contrasena = "hash",
            ),
        )
    }

    private suspend fun insertAnimal(
        userId: Long,
        nombre: String,
        favorite: Boolean,
    ): Long {
        return dao.insert(
            AnimalEntity(
                especie = EspecieAnimal.VACA,
                nombre = nombre,
                raza = "Holstein",
                edad = 2,
                peso = 80f,
                color = "Blanco",
                idUsuario = userId,
                fotoUri = null,
                esFavorito = favorite,
            ),
        )
    }

    private suspend fun Flow<List<AnimalEntity>>.awaitIds(): Set<Long> {
        return withTimeout(5_000) {
            firstEmission().map { it.idAnimal }.toSet()
        }
    }

    private suspend fun Flow<List<AnimalEntity>>.firstEmission(): List<AnimalEntity> {
        return coroutineScope {
            val channel = Channel<List<AnimalEntity>>(capacity = Channel.UNLIMITED)
            val job = launch {
                collect { channel.send(it) }
            }
            try {
                channel.receive()
            } finally {
                job.cancel()
            }
        }
    }

    private fun observe(flow: Flow<List<AnimalEntity>>): FavoriteObservation {
        val channel = Channel<Set<Long>>(capacity = Channel.UNLIMITED)
        val job = CoroutineScope(Dispatchers.IO + Job()).launch {
            flow.collect { animals ->
                channel.send(animals.map { it.idAnimal }.toSet())
            }
        }
        return FavoriteObservation(channel, job)
    }

    private class FavoriteObservation(
        private val channel: Channel<Set<Long>>,
        private val job: Job,
    ) {
        suspend fun next(): Set<Long> = withTimeout(5_000) { channel.receive() }

        suspend fun nextMatching(expected: Set<Long>): Set<Long> {
            while (true) {
                val emission = withTimeout(5_000) { channel.receive() }
                if (emission == expected) return emission
            }
        }

        fun cancel() {
            job.cancel()
            channel.close()
        }
    }
}
