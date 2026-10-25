package com.proyecto.ganapp.ui.features.notifications

sealed interface AssignNotificationEvent {

    data object Success : AssignNotificationEvent

    data object InvalidInput : AssignNotificationEvent

    data object AnimalNotFound : AssignNotificationEvent

    data object NotificationNotFound : AssignNotificationEvent

    data object OwnershipMismatch : AssignNotificationEvent

    data object Failure : AssignNotificationEvent
}
