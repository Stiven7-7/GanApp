package com.proyecto.ganapp.ui.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.ganapp.domain.model.Usuario
import com.proyecto.ganapp.domain.usecase.session.ClearSessionResult
import com.proyecto.ganapp.domain.usecase.session.ClearSessionUseCase
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
    private val clearSessionUseCase: ClearSessionUseCase,
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
            when (clearSessionUseCase()) {
                ClearSessionResult.Success -> {
                    _logoutUiState.value = HomeLogoutUiState(
                        isLoading = false,
                        error = null,
                    )
                }
                ClearSessionResult.UnexpectedError -> {
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
