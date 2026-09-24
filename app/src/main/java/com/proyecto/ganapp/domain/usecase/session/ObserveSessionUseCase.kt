package com.proyecto.ganapp.domain.usecase.session

import com.proyecto.ganapp.domain.repository.SessionRepository
import com.proyecto.ganapp.domain.repository.UsuarioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.coroutines.cancellation.CancellationException

class ObserveSessionUseCase(
    private val sessionRepository: SessionRepository,
    private val usuarioRepository: UsuarioRepository,
) {

    operator fun invoke(): Flow<ObserveSessionResult> {
        return sessionRepository.observeAuthenticatedUserId()
            .map { userId -> resolve(userId) }
            .catch { exception ->
                if (exception is CancellationException) throw exception
                emit(ObserveSessionResult.Unauthenticated)
            }
            .distinctUntilChanged()
    }

    private suspend fun resolve(userId: Long?): ObserveSessionResult {
        if (userId == null || userId <= 0L) {
            return ObserveSessionResult.Unauthenticated
        }
        val usuario = try {
            usuarioRepository.getUserById(userId)
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception
            return ObserveSessionResult.Unauthenticated
        }
        if (usuario != null) {
            return ObserveSessionResult.Authenticated(userId = userId)
        }
        try {
            sessionRepository.clearAuthenticatedUserId()
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception
        }
        return ObserveSessionResult.Unauthenticated
    }
}
