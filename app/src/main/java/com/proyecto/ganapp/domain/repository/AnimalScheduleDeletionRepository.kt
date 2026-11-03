package com.proyecto.ganapp.domain.repository

/** Frontera futura de persistencia; sin implementación ni binding productivo. */
interface AnimalScheduleDeletionRepository {
    /**
     * Elimina todas las programaciones del animal y después el animal en una única transacción.
     * Al retornar, la unidad completa está persistida; ante excepción no debe quedar ningún cambio.
     * Conserva las plantillas. No cancela alarmas ni valida sesión u ownership.
     */
    suspend fun deleteAnimalAndSchedules(animalId: Long)
}
