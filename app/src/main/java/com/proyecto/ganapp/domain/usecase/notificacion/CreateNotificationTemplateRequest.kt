package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.TipoNotificacion

data class CreateNotificationTemplateRequest(
    val titulo: String,
    val tipo: TipoNotificacion,
)
