package com.proyecto.ganapp.domain.usecase.session

sealed interface ObserveSessionResult {

    data object Unauthenticated : ObserveSessionResult

    data class Authenticated(
        val userId: Long,
    ) : ObserveSessionResult
}
