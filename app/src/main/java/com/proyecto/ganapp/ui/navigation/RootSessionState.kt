package com.proyecto.ganapp.ui.navigation

sealed interface RootSessionState {

    data object Loading : RootSessionState

    data object Unauthenticated : RootSessionState

    data class Authenticated(
        val userId: Long,
    ) : RootSessionState
}
