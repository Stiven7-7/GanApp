package com.proyecto.ganapp.ui.features.auth

sealed interface RegisterEvent {

    data object NavigateToLogin : RegisterEvent
}
