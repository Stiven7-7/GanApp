package com.proyecto.ganapp.domain.usecase.session

import com.proyecto.ganapp.domain.repository.SessionRepository

class SaveSessionUseCase(
    private val sessionRepository: SessionRepository,
) {

    suspend operator fun invoke(userId: Long): SaveSessionResult {
        if (userId <= 0L) {
            return SaveSessionResult.InvalidUserId
        }
        return try {
            sessionRepository.saveAuthenticatedUserId(userId)
            SaveSessionResult.Success
        } catch (_: Exception) {
            SaveSessionResult.UnexpectedError
        }
    }
}
