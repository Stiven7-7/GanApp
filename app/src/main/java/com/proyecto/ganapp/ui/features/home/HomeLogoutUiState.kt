package com.proyecto.ganapp.ui.features.home

enum class HomeLogoutError {
    UNEXPECTED
}

data class HomeLogoutUiState(
    val isLoading: Boolean = false,
    val error: HomeLogoutError? = null,
)
