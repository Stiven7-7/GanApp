package com.proyecto.ganapp.ui.features.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.ganapp.domain.model.Notificacion
import com.proyecto.ganapp.domain.usecase.notificacion.GetNotificationsOfAnimalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnimalNotificationsViewModel @Inject constructor(
    private val getNotifications: GetNotificationsOfAnimalUseCase
) : ViewModel() {

    private val _list = MutableStateFlow<List<Notificacion>>(emptyList())
    val list: StateFlow<List<Notificacion>> = _list

    fun load(animalId: Long) {
        viewModelScope.launch {
            _list.value = getNotifications(animalId)
        }
    }
}
