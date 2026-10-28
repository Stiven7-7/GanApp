package com.proyecto.ganapp.domain.usecase.animal

import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.EspecieAnimal
import com.proyecto.ganapp.domain.repository.AnimalRepository
import com.proyecto.ganapp.domain.repository.SessionRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first

class UpdateAnimalUseCase @Inject constructor(
    private val repository: AnimalRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(request: UpdateAnimalRequest): UpdateAnimalResult {
        val userId = try {
            authenticatedUserId()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            return UpdateAnimalResult.Failure(exception)
        } ?: return UpdateAnimalResult.NotFound

        if (request.animalId <= 0L) {
            return UpdateAnimalResult.NotFound
        }

        val original = try {
            repository.getAnimalById(request.animalId)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            return UpdateAnimalResult.Failure(exception)
        } ?: return UpdateAnimalResult.NotFound

        if (original.idUsuario != userId) {
            return UpdateAnimalResult.NotFound
        }

        val nombreNormalizado = request.nombre.trim()
        val colorNormalizado = request.color?.trim()?.takeIf { it.isNotBlank() }
        val errors = linkedSetOf<UpdateAnimalValidationError>()

        val especieValidada: EspecieAnimal? = when (request.especie) {
            null -> {
                errors += UpdateAnimalValidationError.SPECIES_REQUIRED
                null
            }
            EspecieAnimal.SIN_ESPECIFICAR -> {
                errors += UpdateAnimalValidationError.SPECIES_NOT_ALLOWED
                null
            }
            EspecieAnimal.VACA,
            EspecieAnimal.CERDO,
            EspecieAnimal.OVEJA,
            -> request.especie
        }

        if (nombreNormalizado.isBlank()) {
            errors += UpdateAnimalValidationError.NAME_REQUIRED
        }

        val edad = request.edad
        if (edad != null && edad < 0) {
            errors += UpdateAnimalValidationError.INVALID_AGE
        }

        if (!isWeightAllowed(original = original, requested = request.peso)) {
            errors += UpdateAnimalValidationError.INVALID_WEIGHT
        }

        if (errors.isNotEmpty() || especieValidada == null) {
            return UpdateAnimalResult.ValidationError(errors)
        }

        val updatedAnimal = original.copy(
            especie = especieValidada,
            nombre = nombreNormalizado,
            edad = request.edad,
            peso = request.peso,
            color = colorNormalizado,
            fotoUri = request.fotoUri,
        )

        return try {
            val updated = repository.updateAnimal(updatedAnimal)
            if (updated) {
                UpdateAnimalResult.Success(updatedAnimal)
            } else {
                UpdateAnimalResult.NotFound
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            UpdateAnimalResult.Failure(exception)
        }
    }

    private fun isWeightAllowed(original: Animal, requested: Float?): Boolean {
        if (requested == null) return true
        if (original.peso == 0f && requested == 0f) return true
        return requested.isFinite() && requested > 0f
    }

    private suspend fun authenticatedUserId(): Long? {
        val userId = sessionRepository.observeAuthenticatedUserId().first()
        return userId?.takeIf { it > 0L }
    }
}
