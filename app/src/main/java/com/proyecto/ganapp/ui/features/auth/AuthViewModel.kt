package com.proyecto.ganapp.ui.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.ganapp.domain.model.Usuario
import com.proyecto.ganapp.domain.repository.UsuarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val usuarioRepository: UsuarioRepository
) : ViewModel() {

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario

    private val _loginResult = MutableStateFlow<Boolean?>(null)
    val loginResult: StateFlow<Boolean?> = _loginResult

    fun register(usuario: Usuario) {
        viewModelScope.launch {
            usuarioRepository.register(usuario)
            _usuario.value = usuario
        }
    }

    fun login(correo: String, contrasena: String) {
        viewModelScope.launch {
            val user = usuarioRepository.getUsuarioByCorreo(correo)
            _loginResult.value = user?.contrasena == contrasena
        }
    }

    fun logout() {
        _usuario.value = null
        _loginResult.value = null
    }
}
