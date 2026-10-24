package com.proyecto.ganapp.domain.usecase.animal

import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.AnimalDefaults
import com.proyecto.ganapp.domain.model.EspecieAnimal
import com.proyecto.ganapp.domain.repository.AnimalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InsertAnimalUseCaseTest {

    private lateinit var repository: FakeAnimalRepository
    private lateinit var useCase: InsertAnimalUseCase

    @Before
    fun setUp() {
        repository = FakeAnimalRepository()
        useCase = InsertAnimalUseCase(repository)
    }

    @Test
    fun especiesValidas_retornanSuccess() = runTest {
        listOf(EspecieAnimal.VACA, EspecieAnimal.CERDO, EspecieAnimal.OVEJA).forEach { especie ->
            repository.reset()
            val result = useCase(request(especie = especie, nombre = "Lola"))

            assertEquals(RegisterAnimalResult.Success(42L), result)
            assertEquals(1, repository.insertCalls)
            assertEquals(especie, repository.inserted?.especie)
            assertEquals(AnimalDefaults.RAZA_SIN_ESPECIFICAR, repository.inserted?.raza)
        }
    }

    @Test
    fun especieNula_exigeEspecieYNoInserta() = runTest {
        val result = useCase(request(especie = null))

        assertEquals(
            RegisterAnimalResult.ValidationError(
                setOf(RegisterAnimalValidationError.SPECIES_REQUIRED),
            ),
            result,
        )
        assertEquals(0, repository.insertCalls)
    }

    @Test
    fun especieLegacy_noSePermiteEnRegistroNuevo() = runTest {
        val result = useCase(request(especie = EspecieAnimal.SIN_ESPECIFICAR))

        assertEquals(
            RegisterAnimalResult.ValidationError(
                setOf(RegisterAnimalValidationError.SPECIES_NOT_ALLOWED),
            ),
            result,
        )
        assertEquals(0, repository.insertCalls)
    }

    @Test
    fun nombre_validoSeConservaYSeRecortaSinCambiarMayusculas() = runTest {
        useCase(request(nombre = "Manchita"))
        assertEquals("Manchita", repository.inserted?.nombre)

        useCase(request(nombre = "  Manchita  "))
        assertEquals("Manchita", repository.inserted?.nombre)

        useCase(request(nombre = "  manCHIta  "))
        assertEquals("manCHIta", repository.inserted?.nombre)
    }

    @Test
    fun nombreVacioOBlank_exigeNombreYNoInserta() = runTest {
        val empty = useCase(request(nombre = ""))
        val blank = useCase(request(nombre = "   "))

        assertEquals(
            setOf(RegisterAnimalValidationError.NAME_REQUIRED),
            (empty as RegisterAnimalResult.ValidationError).errors,
        )
        assertEquals(
            setOf(RegisterAnimalValidationError.NAME_REQUIRED),
            (blank as RegisterAnimalResult.ValidationError).errors,
        )
        assertEquals(0, repository.insertCalls)
    }

    @Test
    fun registrosNuevos_usanRazaSinEspecificar() = runTest {
        listOf(EspecieAnimal.VACA, EspecieAnimal.CERDO, EspecieAnimal.OVEJA).forEach { especie ->
            repository.reset()
            useCase(request(especie = especie))
            assertEquals(AnimalDefaults.RAZA_SIN_ESPECIFICAR, repository.inserted?.raza)
        }
    }

    @Test
    fun edad_nullCeroYPositivaSonValidas() = runTest {
        useCase(request(edad = null))
        assertNull(repository.inserted?.edad)

        useCase(request(edad = 0))
        assertEquals(0, repository.inserted?.edad)

        useCase(request(edad = 4))
        assertEquals(4, repository.inserted?.edad)
        assertTrue(repository.insertCalls == 3)
    }

    @Test
    fun edadNegativa_esInvalidaYNoInserta() = runTest {
        val result = useCase(request(edad = -1))

        assertEquals(
            setOf(RegisterAnimalValidationError.INVALID_AGE),
            (result as RegisterAnimalResult.ValidationError).errors,
        )
        assertEquals(0, repository.insertCalls)
    }

    @Test
    fun pesoNuloYPositivoFinito_sonValidos() = runTest {
        useCase(request(peso = null))
        assertNull(repository.inserted?.peso)

        useCase(request(peso = 12.5f))
        assertEquals(12.5f, repository.inserted?.peso)
    }

    @Test
    fun pesoCeroNegativoNaNYInfinitos_sonInvalidos() = runTest {
        listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY).forEach { peso ->
            repository.reset()
            val result = useCase(request(peso = peso))

            assertEquals(
                setOf(RegisterAnimalValidationError.INVALID_WEIGHT),
                (result as RegisterAnimalResult.ValidationError).errors,
            )
            assertEquals(0, repository.insertCalls)
        }
    }

    @Test
    fun color_seNormalizaEnElCasoDeUso() = runTest {
        useCase(request(color = null))
        assertNull(repository.inserted?.color)

        useCase(request(color = "  Blanco  "))
        assertEquals("Blanco", repository.inserted?.color)

        useCase(request(color = "   "))
        assertNull(repository.inserted?.color)

        useCase(request(color = ""))
        assertNull(repository.inserted?.color)
    }

    @Test
    fun foto_sePreservaExacta() = runTest {
        useCase(request(fotoUri = null))
        assertNull(repository.inserted?.fotoUri)

        val uri = "content://ganapp/foto/1"
        useCase(request(fotoUri = uri))
        assertEquals(uri, repository.inserted?.fotoUri)
    }

    @Test
    fun usuarioPositivo_esValido() = runTest {
        val result = useCase(request(idUsuario = 7L))

        assertEquals(RegisterAnimalResult.Success(42L), result)
        assertEquals(7L, repository.inserted?.idUsuario)
    }

    @Test
    fun usuarioCeroONegativo_esInvalidoYNoInserta() = runTest {
        val zero = useCase(request(idUsuario = 0L))
        val negative = useCase(request(idUsuario = -5L))

        assertEquals(
            setOf(RegisterAnimalValidationError.INVALID_USER),
            (zero as RegisterAnimalResult.ValidationError).errors,
        )
        assertEquals(
            setOf(RegisterAnimalValidationError.INVALID_USER),
            (negative as RegisterAnimalResult.ValidationError).errors,
        )
        assertEquals(0, repository.insertCalls)
    }

    @Test
    fun erroresMultiples_seAcumulanYNoInsertan() = runTest {
        val result = useCase(
            request(
                especie = null,
                nombre = "   ",
                edad = -2,
                peso = Float.NaN,
                idUsuario = 0L,
            ),
        )

        assertEquals(
            setOf(
                RegisterAnimalValidationError.SPECIES_REQUIRED,
                RegisterAnimalValidationError.NAME_REQUIRED,
                RegisterAnimalValidationError.INVALID_AGE,
                RegisterAnimalValidationError.INVALID_WEIGHT,
                RegisterAnimalValidationError.INVALID_USER,
            ),
            (result as RegisterAnimalResult.ValidationError).errors,
        )
        assertEquals(0, repository.insertCalls)
    }

    @Test
    fun success_propagaElIdDelRepositorio() = runTest {
        repository.nextId = 42L

        val result = useCase(request())

        assertEquals(RegisterAnimalResult.Success(42L), result)
    }

    @Test
    fun falloDelRepositorio_devuelveFailure() = runTest {
        val exception = IllegalStateException("db")
        repository.insertException = exception

        val result = useCase(request())

        assertEquals(RegisterAnimalResult.Failure(exception), result)
        assertEquals(1, repository.insertCalls)
    }

    @Test
    fun objetoInsertado_coincideConElContrato() = runTest {
        useCase(
            RegisterAnimalRequest(
                especie = EspecieAnimal.VACA,
                nombre = "  Manchita  ",
                edad = null,
                peso = null,
                color = "   ",
                fotoUri = "content://ganapp/manchita",
                idUsuario = 7L,
            ),
        )

        assertEquals(
            Animal(
                idAnimal = 0L,
                especie = EspecieAnimal.VACA,
                nombre = "Manchita",
                raza = AnimalDefaults.RAZA_SIN_ESPECIFICAR,
                edad = null,
                peso = null,
                color = null,
                fotoUri = "content://ganapp/manchita",
                idUsuario = 7L,
            ),
            repository.inserted,
        )
    }

    private fun request(
        especie: EspecieAnimal? = EspecieAnimal.VACA,
        nombre: String = "Lola",
        edad: Int? = null,
        peso: Float? = null,
        color: String? = null,
        fotoUri: String? = null,
        idUsuario: Long = 7L,
    ): RegisterAnimalRequest {
        return RegisterAnimalRequest(
            especie = especie,
            nombre = nombre,
            edad = edad,
            peso = peso,
            color = color,
            fotoUri = fotoUri,
            idUsuario = idUsuario,
        )
    }

    private class FakeAnimalRepository : AnimalRepository {
        var inserted: Animal? = null
        var insertCalls = 0
        var nextId = 42L
        var insertException: Exception? = null

        fun reset() {
            inserted = null
            insertCalls = 0
            insertException = null
        }

        override fun getAllAnimals(userId: Long): Flow<List<Animal>> {
            error("Unexpected call to getAllAnimals")
        }

        override fun getAnimalsByUser(userId: Long): Flow<List<Animal>> {
            error("Unexpected call to getAnimalsByUser")
        }

        override suspend fun getAnimalById(id: Long): Animal? {
            error("Unexpected call to getAnimalById")
        }

        override suspend fun insertAnimal(animal: Animal): Long {
            insertCalls += 1
            insertException?.let { throw it }
            inserted = animal
            return nextId
        }

        override suspend fun updateAnimal(animal: Animal): Boolean {
            error("Unexpected call to updateAnimal")
        }

        override suspend fun deleteAnimal(id: Long) {
            error("Unexpected call to deleteAnimal")
        }
    }
}
