package com.proyecto.ganapp.domain.usecase.session

sealed interface ClearSessionResult {

    data object Success : ClearSessionResult

    data object UnexpectedError : ClearSessionResult
}
