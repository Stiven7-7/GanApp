package com.proyecto.ganapp.domain.usecase.animal

import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.AnimalDefaults
import com.proyecto.ganapp.domain.model.EspecieAnimal
import com.proyecto.ganapp.domain.repository.AnimalRepository
import kotlin.coroutines.cancellation.CancellationException

class InsertAnimalUseCase(
    private val repository: AnimalRepository,
) {
    suspend operator fun invoke(
        request: RegisterAnimalRequest,
    ): RegisterAnimalResult {
        val nombreNormalizado = request.nombre.trim()
        val colorNormalizado = request.color?.trim()?.takeIf { it.isNotBlank() }
        val errors = linkedSetOf<RegisterAnimalValidationError>()

        val especieValidada: EspecieAnimal? = when (request.especie) {
            null -> {
                errors += RegisterAnimalValidationError.SPECIES_REQUIRED
                null
            }
            EspecieAnimal.SIN_ESPECIFICAR -> {
                errors += RegisterAnimalValidationError.SPECIES_NOT_ALLOWED
                null
            }
            EspecieAnimal.VACA,
            EspecieAnimal.CERDO,
            EspecieAnimal.OVEJA,
            -> request.especie
        }

        if (nombreNormalizado.isBlank()) {
            errors += RegisterAnimalValidationError.NAME_REQUIRED
        }

        val edad = request.edad
        if (edad != null && edad < 0) {
            errors += RegisterAnimalValidationError.INVALID_AGE
        }

        val peso = request.peso
        if (peso != null && (!peso.isFinite() || peso <= 0f)) {
            errors += RegisterAnimalValidationError.INVALID_WEIGHT
        }

        if (request.idUsuario <= 0L) {
            errors += RegisterAnimalValidationError.INVALID_USER
        }

        if (errors.isNotEmpty() || especieValidada == null) {
            return RegisterAnimalResult.ValidationError(errors)
        }

        return try {
            val animalId = repository.insertAnimal(
                Animal(
                    especie = especieValidada,
                    nombre = nombreNormalizado,
                    raza = AnimalDefaults.RAZA_SIN_ESPECIFICAR,
                    edad = request.edad,
                    peso = request.peso,
                    color = colorNormalizado,
                    fotoUri = request.fotoUri,
                    idUsuario = request.idUsuario,
                ),
            )
            RegisterAnimalResult.Success(animalId)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            RegisterAnimalResult.Failure(exception)
        }
    }
}
