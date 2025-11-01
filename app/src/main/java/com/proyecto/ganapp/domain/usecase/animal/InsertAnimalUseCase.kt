package com.proyecto.ganapp.domain.usecase.animal

import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.repository.AnimalRepository

class InsertAnimalUseCase(
    private val repository: AnimalRepository
) {
    suspend operator fun invoke(animal: Animal) {
        repository.insertAnimal(animal)
    }
}
