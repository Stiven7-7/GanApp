package com.proyecto.ganapp.domain.usecase.animal

import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.EspecieAnimal
import com.proyecto.ganapp.domain.repository.AnimalRepository
import com.proyecto.ganapp.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class UpdateAnimalUseCaseTest {

    private lateinit var repository: FakeAnimalRepository
    private lateinit var session: FakeSessionRepository
    private lateinit var useCase: UpdateAnimalUseCase

    @Before
    fun setUp() {
        repository = FakeAnimalRepository()
        session = FakeSessionRepository(userId = 7L)
        useCase = UpdateAnimalUseCase(repository, session)
    }

    @Test
    fun especiesReales_actualizanYPreservanIdentidad() = runTest {
        listOf(EspecieAnimal.VACA, EspecieAnimal.CERDO, EspecieAnimal.OVEJA).forEach { especie ->
            repository.reset(original(especie = EspecieAnimal.OVEJA, raza = " Holstein "))
            val result = useCase(request(especie = especie, nombre = "  Nube  "))

            val success = result as UpdateAnimalResult.Success
            assertEquals(especie, success.animal.especie)
            assertEquals("Nube", success.animal.nombre)
            assertEquals(4L, success.animal.idAnimal)
            assertEquals(7L, success.animal.idUsuario)
            assertEquals(" Holstein ", success.animal.raza)
            assertEquals(success.animal, repository.updated)
        }
    }

    @Test
    fun especieNulaYNombreVacio_acumulanErroresSinActualizar() = runTest {
        repository.reset(original())
        val result = useCase(request(especie = null, nombre = "   ", edad = -1, peso = 0f))

        val error = result as UpdateAnimalResult.ValidationError
        assertEquals(
            setOf(
                UpdateAnimalValidationError.SPECIES_REQUIRED,
                UpdateAnimalValidationError.NAME_REQUIRED,
                UpdateAnimalValidationError.INVALID_AGE,
                UpdateAnimalValidationError.INVALID_WEIGHT,
            ),
            error.errors,
        )
        assertEquals(0, repository.updateCalls)
    }

    @Test
    fun sinEspecificarComoDestino_esInvalido() = runTest {
        repository.reset(original(especie = EspecieAnimal.VACA))
        val result = useCase(request(especie = EspecieAnimal.SIN_ESPECIFICAR))

        val error = result as UpdateAnimalResult.ValidationError
        assertTrue(error.errors.contains(UpdateAnimalValidationError.SPECIES_NOT_ALLOWED))
        assertEquals(0, repository.updateCalls)
    }

    @Test
    fun historicoSinEspecificar_puedePasarAEspecieReal() = runTest {
        listOf(EspecieAnimal.VACA, EspecieAnimal.CERDO, EspecieAnimal.OVEJA).forEach { especie ->
            repository.reset(original(especie = EspecieAnimal.SIN_ESPECIFICAR, raza = "SIN_ESPECIFICAR"))
            val result = useCase(request(especie = especie, nombre = "Histórico"))

            val success = result as UpdateAnimalResult.Success
            assertEquals(especie, success.animal.especie)
            assertEquals("SIN_ESPECIFICAR", success.animal.raza)
        }
    }

    @Test
    fun edadNullCeroYPositiva_sonValidas() = runTest {
        listOf(null, 0, 4).forEach { edad ->
            repository.reset(original())
            val result = useCase(request(edad = edad))
            assertEquals(edad, (result as UpdateAnimalResult.Success).animal.edad)
        }
    }

    @Test
    fun pesoNullYPositivo_sonValidos_ceroNuevoNo() = runTest {
        repository.reset(original(peso = 80f))
        assertNull((useCase(request(peso = null)) as UpdateAnimalResult.Success).animal.peso)

        repository.reset(original(peso = 80f))
        assertEquals(12.5f, (useCase(request(peso = 12.5f)) as UpdateAnimalResult.Success).animal.peso)

        repository.reset(original(peso = 80f))
        val invalid = useCase(request(peso = 0f)) as UpdateAnimalResult.ValidationError
        assertTrue(invalid.errors.contains(UpdateAnimalValidationError.INVALID_WEIGHT))
        assertEquals(0, repository.updateCalls)
    }

    @Test
    fun pesoNoFinitoONegativo_esInvalido() = runTest {
        listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -3f).forEach { peso ->
            repository.reset(original(peso = 80f))
            val result = useCase(request(peso = peso)) as UpdateAnimalResult.ValidationError
            assertTrue(result.errors.contains(UpdateAnimalValidationError.INVALID_WEIGHT))
            assertEquals(0, repository.updateCalls)
        }
    }

    @Test
    fun pesoHistoricoCero_permiteEditarOtroCampo() = runTest {
        repository.reset(original(peso = 0f, nombre = "Viejo", raza = "Holstein"))
        val result = useCase(request(nombre = "Nuevo", peso = 0f))

        val success = result as UpdateAnimalResult.Success
        assertEquals("Nuevo", success.animal.nombre)
        assertEquals(0f, success.animal.peso)
        assertEquals("Holstein", success.animal.raza)
        assertEquals(1, repository.updateCalls)
    }

    @Test
    fun colorSeRecortaYElBlancoPasaANull() = runTest {
        repository.reset(original())
        assertEquals("Negro", (useCase(request(color = "  Negro  ")) as UpdateAnimalResult.Success).animal.color)

        repository.reset(original(color = "Negro"))
        assertNull((useCase(request(color = "   ")) as UpdateAnimalResult.Success).animal.color)
    }

    @Test
    fun fotoSeConservaCambiaOSeElimina() = runTest {
        repository.reset(original(fotoUri = "content://vieja"))
        assertEquals(
            "content://vieja",
            (useCase(request(fotoUri = "content://vieja")) as UpdateAnimalResult.Success).animal.fotoUri,
        )

        repository.reset(original(fotoUri = "content://vieja"))
        assertEquals(
            "content://nueva",
            (useCase(request(fotoUri = "content://nueva")) as UpdateAnimalResult.Success).animal.fotoUri,
        )

        repository.reset(original(fotoUri = "content://vieja"))
        assertNull((useCase(request(fotoUri = null)) as UpdateAnimalResult.Success).animal.fotoUri)
    }

    @Test
    fun usuarioDistinto_esNotFoundSinActualizar() = runTest {
        repository.reset(original())
        session.userId = 99L

        assertEquals(
            UpdateAnimalResult.NotFound,
            useCase(request(nombre = "Ajeno", esFavorito = true)),
        )
        assertEquals(1, repository.getCalls)
        assertEquals(0, repository.updateCalls)
        assertNull(repository.updated)
    }

    @Test
    fun sesionAusente_esNotFoundSinActualizar() = runTest {
        repository.reset(original())
        listOf<Long?>(null, 0L, -1L).forEach { userId ->
            repository.reset(original())
            session.userId = userId

            assertEquals(UpdateAnimalResult.NotFound, useCase(request(esFavorito = true)))
            assertEquals(0, repository.getCalls)
            assertEquals(0, repository.updateCalls)
        }
    }

    @Test
    fun idInvalidoOInexistente_esNotFoundSinActualizar() = runTest {
        repository.reset(original())
        assertEquals(UpdateAnimalResult.NotFound, useCase(request(animalId = 0L)))
        assertEquals(UpdateAnimalResult.NotFound, useCase(request(animalId = -5L)))
        assertEquals(0, repository.getCalls)
        assertEquals(0, repository.updateCalls)

        repository.reset(null)
        assertEquals(UpdateAnimalResult.NotFound, useCase(request(animalId = 9L)))
        assertEquals(0, repository.updateCalls)
    }

    @Test
    fun updateSinFilas_esNotFound() = runTest {
        repository.reset(original())
        repository.updateResult = false
        assertEquals(UpdateAnimalResult.NotFound, useCase(request()))
        assertEquals(1, repository.updateCalls)
    }

    @Test
    fun excepcionDelRepositorio_esFailure() = runTest {
        val failure = IllegalStateException("db")
        repository.reset(original())
        repository.updateError = failure

        val result = useCase(request()) as UpdateAnimalResult.Failure
        assertSame(failure, result.cause)
    }

    @Test
    fun cancelacion_sePropaga() = runTest {
        val cancellation = CancellationException("cancel")
        repository.reset(original())
        repository.getError = cancellation
        var thrown: Throwable? = null
        try {
            useCase(request())
        } catch (exception: CancellationException) {
            thrown = exception
        }
        assertSame(cancellation, thrown)
        assertEquals(0, repository.updateCalls)
    }

    @Test
    fun cancelacionDeSesion_sePropagaSinActualizar() = runTest {
        val cancellation = CancellationException("sesion")
        repository.reset(original())
        session.error = cancellation
        var thrown: Throwable? = null

        try {
            useCase(request())
        } catch (exception: CancellationException) {
            thrown = exception
        }

        assertSame(cancellation, thrown)
        assertEquals(0, repository.getCalls)
        assertEquals(0, repository.updateCalls)
    }

    @Test
    fun favorito_falseATrue_conservaIdentidad() = runTest {
        repository.reset(original(esFavorito = false, raza = " Holstein "))
        val result = useCase(request(esFavorito = true)) as UpdateAnimalResult.Success

        assertEquals(true, result.animal.esFavorito)
        assertEquals(true, repository.updated?.esFavorito)
        assertEquals(4L, result.animal.idAnimal)
        assertEquals(7L, result.animal.idUsuario)
        assertEquals(" Holstein ", result.animal.raza)
    }

    @Test
    fun favorito_trueAFalse() = runTest {
        repository.reset(original(esFavorito = true))
        val result = useCase(request(esFavorito = false)) as UpdateAnimalResult.Success

        assertEquals(false, result.animal.esFavorito)
        assertEquals(false, repository.updated?.esFavorito)
    }

    @Test
    fun favorito_trueSeMantiene() = runTest {
        repository.reset(original(esFavorito = true))
        val result = useCase(request(esFavorito = true)) as UpdateAnimalResult.Success

        assertEquals(true, result.animal.esFavorito)
        assertEquals(true, repository.updated?.esFavorito)
    }

    @Test
    fun favorito_falseSeMantiene() = runTest {
        repository.reset(original(esFavorito = false))
        val result = useCase(request(esFavorito = false)) as UpdateAnimalResult.Success

        assertEquals(false, result.animal.esFavorito)
        assertEquals(false, repository.updated?.esFavorito)
    }

    private fun original(
        especie: EspecieAnimal = EspecieAnimal.VACA,
        nombre: String = "Lola",
        raza: String = "Holstein",
        edad: Int? = 2,
        peso: Float? = 80f,
        color: String? = "Blanco",
        fotoUri: String? = "content://ganapp/lola",
        esFavorito: Boolean = false,
    ) = Animal(
        idAnimal = 4L,
        especie = especie,
        nombre = nombre,
        raza = raza,
        edad = edad,
        peso = peso,
        color = color,
        fotoUri = fotoUri,
        idUsuario = 7L,
        esFavorito = esFavorito,
    )

    private fun request(
        animalId: Long = 4L,
        especie: EspecieAnimal? = EspecieAnimal.VACA,
        nombre: String = "Lola",
        edad: Int? = 2,
        peso: Float? = 80f,
        color: String? = "Blanco",
        fotoUri: String? = "content://ganapp/lola",
        esFavorito: Boolean = false,
    ) = UpdateAnimalRequest(
        animalId = animalId,
        especie = especie,
        nombre = nombre,
        edad = edad,
        peso = peso,
        color = color,
        fotoUri = fotoUri,
        esFavorito = esFavorito,
    )

    private class FakeAnimalRepository : AnimalRepository {
        var stored: Animal? = null
        var updateResult = true
        var updateError: Throwable? = null
        var getError: Throwable? = null
        var updateCalls = 0
        var getCalls = 0
        var updated: Animal? = null

        fun reset(animal: Animal?) {
            stored = animal
            updateResult = true
            updateError = null
            getError = null
            updateCalls = 0
            getCalls = 0
            updated = null
        }

        override fun getAllAnimals(userId: Long): Flow<List<Animal>> = flowOf(emptyList())

        override fun getAnimalsByUser(userId: Long): Flow<List<Animal>> = flowOf(emptyList())

        override suspend fun getAnimalById(id: Long): Animal? {
            getCalls += 1
            getError?.let { throw it }
            return stored?.takeIf { it.idAnimal == id }
        }

        override suspend fun insertAnimal(animal: Animal): Long = error("unused")

        override suspend fun updateAnimal(animal: Animal): Boolean {
            updateCalls += 1
            updated = animal
            updateError?.let { throw it }
            return updateResult
        }

        override suspend fun deleteAnimal(id: Long) = error("unused")
    }

    private class FakeSessionRepository(
        userId: Long?,
    ) : SessionRepository {
        var userId: Long? = userId
        var error: Throwable? = null

        override fun observeAuthenticatedUserId(): Flow<Long?> {
            val failure = error
            return if (failure != null) flow { throw failure } else flowOf(userId)
        }

        override suspend fun saveAuthenticatedUserId(userId: Long) = error("unused")

        override suspend fun clearAuthenticatedUserId() = error("unused")
    }
}
