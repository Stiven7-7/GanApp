package com.proyecto.ganapp.domain.usecase.session

sealed interface SaveSessionResult {

    data object Success : SaveSessionResult

    data object InvalidUserId : SaveSessionResult

    data object UnexpectedError : SaveSessionResult
}
