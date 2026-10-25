package com.proyecto.ganapp.ui.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.ganapp.domain.model.Usuario
import com.proyecto.ganapp.domain.usecase.session.LogoutResult
import com.proyecto.ganapp.domain.usecase.session.LogoutUseCase
import com.proyecto.ganapp.domain.usecase.usuario.GetUsuarioByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getUsuarioByIdUseCase: GetUsuarioByIdUseCase,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario

    private val _logoutUiState = MutableStateFlow(HomeLogoutUiState())
    val logoutUiState: StateFlow<HomeLogoutUiState> = _logoutUiState.asStateFlow()

    fun cargarUsuario(id: Long) {
        viewModelScope.launch {
            _usuario.value = getUsuarioByIdUseCase(id)
        }
    }

    fun logout() {
        if (_logoutUiState.value.isLoading) return

        _logoutUiState.update {
            it.copy(
                isLoading = true,
                error = null,
            )
        }

        viewModelScope.launch {
            when (logoutUseCase()) {
                LogoutResult.Success -> {
                    _logoutUiState.value = HomeLogoutUiState(
                        isLoading = false,
                        error = null,
                    )
                }
                LogoutResult.AlarmCancellationFailure,
                LogoutResult.SessionClearFailure,
                -> {
                    _logoutUiState.update {
                        it.copy(
                            isLoading = false,
                            error = HomeLogoutError.UNEXPECTED,
                        )
                    }
                }
            }
        }
    }
}
