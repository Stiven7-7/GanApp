package com.proyecto.ganapp.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.ganapp.domain.usecase.session.ObserveSessionResult
import com.proyecto.ganapp.domain.usecase.session.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RootSessionViewModel @Inject constructor(
    observeSessionUseCase: ObserveSessionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<RootSessionState>(RootSessionState.Loading)
    val uiState: StateFlow<RootSessionState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSessionUseCase().collect { result ->
                _uiState.value = when (result) {
                    ObserveSessionResult.Unauthenticated -> RootSessionState.Unauthenticated
                    is ObserveSessionResult.Authenticated ->
                        RootSessionState.Authenticated(userId = result.userId)
                }
            }
        }
    }
}
