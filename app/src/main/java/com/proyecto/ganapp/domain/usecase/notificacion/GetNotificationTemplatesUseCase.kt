package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.PlantillaNotificacion
import com.proyecto.ganapp.domain.repository.NotificationTemplateRepository
import com.proyecto.ganapp.domain.repository.SessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class GetNotificationTemplatesUseCase(
    private val repository: NotificationTemplateRepository,
    private val sessionRepository: SessionRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<PlantillaNotificacion>> = flow {
        emitAll(
            sessionRepository.observeAuthenticatedUserId()
                .distinctUntilChanged()
                .flatMapLatest { userId ->
                    if (userId == null || userId <= 0L) {
                        flowOf(emptyList())
                    } else {
                        repository.getTemplatesByUser(userId)
                            .map { templates -> templates.filter { it.idUsuario == userId } }
                            // Retira los datos del usuario anterior mientras llega la nueva consulta.
                            .onStart { emit(emptyList()) }
                    }
                }
                .distinctUntilChanged(),
        )
    }
}
