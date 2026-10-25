package com.proyecto.ganapp.ui.features.notifications

import com.proyecto.ganapp.domain.usecase.notificacion.CreateNotificationValidationError

sealed interface NotificationCreateEvent {

    data object Success : NotificationCreateEvent

    data class ValidationFailure(
        val errors: Set<CreateNotificationValidationError>,
    ) : NotificationCreateEvent

    data object PersistenceFailure : NotificationCreateEvent

    data class SchedulingFailure(
        val notificationId: Long,
    ) : NotificationCreateEvent
}
