package com.proyecto.ganapp.domain.usecase.animal

import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.repository.AnimalRepository
import javax.inject.Inject

class GetAnimalByIdUseCase @Inject constructor(
    private val repository: AnimalRepository,
) {
    suspend operator fun invoke(animalId: Long): Animal? {
        return repository.getAnimalById(animalId)
    }
}
