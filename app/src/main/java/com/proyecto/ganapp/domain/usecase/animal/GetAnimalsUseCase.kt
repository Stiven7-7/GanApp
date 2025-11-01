package com.proyecto.ganapp.domain.usecase.animal

import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.repository.AnimalRepository
import kotlinx.coroutines.flow.Flow

class GetAnimalsUseCase(
    private val repository: AnimalRepository
) {
    operator fun invoke(userId: Long): Flow<List<Animal>> {
        return repository.getAllAnimals(userId)
    }
}
