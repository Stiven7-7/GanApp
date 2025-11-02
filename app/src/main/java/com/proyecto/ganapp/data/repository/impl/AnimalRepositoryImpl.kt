package com.proyecto.ganapp.data.repository.impl

import com.proyecto.ganapp.data.local.dao.AnimalDao
import com.proyecto.ganapp.data.mapper.toDomain
import com.proyecto.ganapp.data.mapper.toEntity
import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.repository.AnimalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AnimalRepositoryImpl @Inject constructor(
    private val dao: AnimalDao
) : AnimalRepository {

    override fun getAllAnimals(userId: Long): Flow<List<Animal>> {
        return dao.getByUser(userId).map { list -> list.map { it.toDomain() } }
    }

    override fun getAnimalsByUser(userId: Long): Flow<List<Animal>> {
        return dao.getByUser(userId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getAnimalById(id: Long): Animal? {
        return dao.getById(id)?.toDomain()
    }

    override suspend fun insertAnimal(animal: Animal) {
        dao.insert(animal.toEntity())
    }

    override suspend fun updateAnimal(animal: Animal) {
        dao.update(animal.toEntity())
    }

    override suspend fun deleteAnimal(id: Long) {
        dao.deleteById(id)
    }
}
