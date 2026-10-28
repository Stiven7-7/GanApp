package com.proyecto.ganapp.domain.usecase.animal

import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.repository.AnimalRepository
import com.proyecto.ganapp.domain.repository.SessionRepository
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class GetFavoriteAnimalsUseCase @Inject constructor(
    private val animalRepository: AnimalRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(): Flow<List<Animal>> {
        return sessionRepository
            .observeAuthenticatedUserId()
            .flatMapLatest { userId ->
                if (userId == null || userId <= 0L) {
                    flowOf(emptyList())
                } else {
                    animalRepository.getFavoriteAnimalsByUser(userId)
                }
            }
            .map(::orderedByName)
    }

    private fun orderedByName(animals: List<Animal>): List<Animal> {
        return animals.sortedWith(
            compareBy<Animal> { animal ->
                animal.nombre.lowercase(Locale.ROOT)
            }.thenBy { animal ->
                animal.idAnimal
            },
        )
    }
}
