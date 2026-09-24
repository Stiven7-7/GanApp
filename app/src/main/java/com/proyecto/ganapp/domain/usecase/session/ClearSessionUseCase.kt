package com.proyecto.ganapp.domain.usecase.session

import com.proyecto.ganapp.domain.repository.SessionRepository
import kotlin.coroutines.cancellation.CancellationException

class ClearSessionUseCase(
    private val sessionRepository: SessionRepository,
) {

    suspend operator fun invoke(): ClearSessionResult {
        return try {
            sessionRepository.clearAuthenticatedUserId()
            ClearSessionResult.Success
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            ClearSessionResult.UnexpectedError
        }
    }
}
