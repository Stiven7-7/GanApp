package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.Notificacion
import com.proyecto.ganapp.domain.repository.NotificacionRepository
import javax.inject.Inject

class CreateNotificacionUseCase @Inject constructor(
    private val repository: NotificacionRepository
) {
    suspend operator fun invoke(notificacion: Notificacion) {
        repository.insertNotification(notificacion)
    }
}
