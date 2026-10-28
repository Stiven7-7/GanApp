package com.proyecto.ganapp.domain.usecase.animal

import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.repository.AnimalRepository
import com.proyecto.ganapp.domain.repository.SessionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class GetAnimalByIdUseCase @Inject constructor(
    private val repository: AnimalRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(animalId: Long): Animal? {
        val userId = authenticatedUserId() ?: return null
        val animal = repository.getAnimalById(animalId) ?: return null
        if (animal.idUsuario != userId) return null
        return animal
    }

    private suspend fun authenticatedUserId(): Long? {
        val userId = sessionRepository.observeAuthenticatedUserId().first()
        return userId?.takeIf { it > 0L }
    }
}
