package com.proyecto.ganapp.data.repository.impl

import androidx.room.withTransaction
import com.proyecto.ganapp.data.local.db.AppDatabase
import com.proyecto.ganapp.domain.repository.AnimalScheduleDeletionRepository
import javax.inject.Inject

class AnimalScheduleDeletionRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
) : AnimalScheduleDeletionRepository {
    override suspend fun deleteAnimalAndSchedules(animalId: Long) {
        database.withTransaction {
            database.programacionNotificacionDao().deleteByAnimal(animalId)
            database.animalDao().deleteById(animalId)
        }
    }
}
