package com.proyecto.ganapp.ui.features.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.ganapp.domain.model.Notificacion
import com.proyecto.ganapp.domain.repository.NotificacionRepository
import com.proyecto.ganapp.domain.usecase.notificacion.AssignNotificationToAnimalUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class AssignNotificationViewModel @Inject constructor(
    private val notificacionRepository: NotificacionRepository,
    private val assignUseCase: AssignNotificationToAnimalUseCase
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<Notificacion>>(emptyList())
    val notifications: StateFlow<List<Notificacion>> = _notifications

    fun loadNotifications(userId: Long) {
        viewModelScope.launch {
            notificacionRepository.getAllNotifications(userId).collect { list ->
                _notifications.value = list
            }
        }
    }

    fun assign(animalId: Long, notificacionId: Long, onAssigned: () -> Unit) {
        viewModelScope.launch {
            assignUseCase(animalId, notificacionId)
            onAssigned()
        }
    }
}
