package com.proyecto.ganapp.ui.features.notifications

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.ganapp.core.notifications.NotificationScheduler
import com.proyecto.ganapp.domain.model.Notificacion
import com.proyecto.ganapp.domain.usecase.notificacion.CreateNotificacionUseCase
import com.proyecto.ganapp.domain.usecase.notificacion.GetNotificationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val getNotificationsUseCase: GetNotificationsUseCase,
    private val createNotificacionUseCase: CreateNotificacionUseCase,
    @ApplicationContext private val context: Context   // 👈 inyectamos contexto
) : ViewModel() {

    val nombre = MutableStateFlow("")
    val tipo = MutableStateFlow("")
    val fechaInicio = MutableStateFlow(0L)
    val fechaFin = MutableStateFlow(0L)
    val seRepite = MutableStateFlow("NO")

    val hora = MutableStateFlow("08:00")
    val dosisPorDia = MutableStateFlow<Int?>(null)
    val intervaloHoras = MutableStateFlow<Int?>(null)

    val idUsuario = MutableStateFlow(0L)

    private val _notifications = MutableStateFlow<List<Notificacion>>(emptyList())
    val notifications: StateFlow<List<Notificacion>> = _notifications

    private val _event = MutableSharedFlow<String>()
    val event = _event.asSharedFlow()

    fun loadNotifications(userId: Long) {
        viewModelScope.launch {
            getNotificationsUseCase(userId).collect { _notifications.value = it }
        }
    }

    fun onSave(notificacion: Notificacion) {
        viewModelScope.launch {

            if (notificacion.nombre.isBlank()) {
                _event.emit("ERROR_NOMBRE")
                return@launch
            }

            if (notificacion.fechaInicio <= 0 || notificacion.fechaFin <= 0) {
                _event.emit("ERROR_FECHAS")
                return@launch
            }

            // 1) Guardar en BD
            createNotificacionUseCase(notificacion)

            // 2) Programar la alarma de sistema
            NotificationScheduler.schedule(context, notificacion)

            // 3) Avisar a la UI
            _event.emit("SUCCESS")
        }
    }
}
