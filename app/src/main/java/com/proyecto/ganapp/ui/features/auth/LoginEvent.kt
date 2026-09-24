package com.proyecto.ganapp.ui.features.auth

sealed interface LoginEvent {

    data class NavigateToHome(
        val userId: Long,
    ) : LoginEvent
}
