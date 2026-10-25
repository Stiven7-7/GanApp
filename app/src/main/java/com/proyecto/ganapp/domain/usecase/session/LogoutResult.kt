package com.proyecto.ganapp.domain.usecase.session

sealed interface LogoutResult {
    data object Success : LogoutResult
    data object AlarmCancellationFailure : LogoutResult
    data object SessionClearFailure : LogoutResult
}
