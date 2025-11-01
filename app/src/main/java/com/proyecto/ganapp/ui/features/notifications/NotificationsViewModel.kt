package com.proyecto.ganapp.ui.features.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.ganapp.domain.model.Notificacion
import com.proyecto.ganapp.domain.usecase.notificacion.GetNotificationsUseCase
import com.proyecto.ganapp.domain.usecase.notificacion.ScheduleNotificationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val getNotificationsUseCase: GetNotificationsUseCase,
    private val scheduleNotificationUseCase: ScheduleNotificationUseCase
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<Notificacion>>(emptyList())
    val notifications: StateFlow<List<Notificacion>> = _notifications

    fun loadNotifications(userId: Long) {
        viewModelScope.launch {
            getNotificationsUseCase(userId).collect { list ->
                _notifications.value = list
            }
        }
    }

    fun schedule(notificacion: Notificacion) {
        scheduleNotificationUseCase(notificacion)
    }
}
