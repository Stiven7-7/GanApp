package com.proyecto.ganapp.ui.features.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.ganapp.domain.model.Notificacion
import com.proyecto.ganapp.domain.usecase.notificacion.AssignNotificationToAnimalResult
import com.proyecto.ganapp.domain.usecase.notificacion.AssignNotificationToAnimalUseCase
import com.proyecto.ganapp.domain.usecase.notificacion.GetNotificationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AssignNotificationViewModel @Inject constructor(
    private val getNotificationsUseCase: GetNotificationsUseCase,
    private val assignNotificationToAnimalUseCase: AssignNotificationToAnimalUseCase,
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<Notificacion>>(emptyList())
    val notifications: StateFlow<List<Notificacion>> = _notifications

    private val _isAssigning = MutableStateFlow(false)
    val isAssigning: StateFlow<Boolean> = _isAssigning.asStateFlow()

    private val _event = MutableSharedFlow<AssignNotificationEvent>()
    val event = _event.asSharedFlow()

    fun loadNotifications(userId: Long) {
        viewModelScope.launch {
            getNotificationsUseCase(userId).collect { list ->
                _notifications.value = list
            }
        }
    }

    fun assign(
        userId: Long,
        animalId: Long,
        notificationId: Long,
    ) {
        if (_isAssigning.value) return
        _isAssigning.value = true
        viewModelScope.launch {
            try {
                val result = assignNotificationToAnimalUseCase(
                    userId = userId,
                    animalId = animalId,
                    notificationId = notificationId,
                )
                _event.emit(result.toEvent())
            } finally {
                _isAssigning.value = false
            }
        }
    }

    private fun AssignNotificationToAnimalResult.toEvent(): AssignNotificationEvent {
        return when (this) {
            AssignNotificationToAnimalResult.Success ->
                AssignNotificationEvent.Success
            AssignNotificationToAnimalResult.InvalidInput ->
                AssignNotificationEvent.InvalidInput
            AssignNotificationToAnimalResult.AnimalNotFound ->
                AssignNotificationEvent.AnimalNotFound
            AssignNotificationToAnimalResult.NotificationNotFound ->
                AssignNotificationEvent.NotificationNotFound
            AssignNotificationToAnimalResult.OwnershipMismatch ->
                AssignNotificationEvent.OwnershipMismatch
            is AssignNotificationToAnimalResult.Failure ->
                AssignNotificationEvent.Failure
        }
    }
}
