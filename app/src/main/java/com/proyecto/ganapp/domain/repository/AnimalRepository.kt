package com.proyecto.ganapp.domain.repository

import com.proyecto.ganapp.domain.model.Animal
import kotlinx.coroutines.flow.Flow

interface AnimalRepository {
    fun getAllAnimals(userId: Long): Flow<List<Animal>>
    fun getAnimalsByUser(userId: Long): Flow<List<Animal>>
    suspend fun getAnimalById(id: Long): Animal?
    suspend fun insertAnimal(animal: Animal)
    suspend fun updateAnimal(animal: Animal)
    suspend fun deleteAnimal(id: Long)
}
