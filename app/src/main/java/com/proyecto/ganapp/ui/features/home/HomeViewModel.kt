package com.proyecto.ganapp.ui.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.ganapp.domain.model.Usuario
import com.proyecto.ganapp.domain.usecase.usuario.GetUsuarioByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getUsuarioByIdUseCase: GetUsuarioByIdUseCase
) : ViewModel() {

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario

    fun cargarUsuario(id: Long) {
        viewModelScope.launch {
            _usuario.value = getUsuarioByIdUseCase(id)
        }
    }
}
