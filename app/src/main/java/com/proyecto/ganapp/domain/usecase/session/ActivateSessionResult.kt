package com.proyecto.ganapp.domain.usecase.session

sealed interface ActivateSessionResult {
    data object Success : ActivateSessionResult

    data object SuccessWithNotificationReconciliationFailure :
        ActivateSessionResult

    data object InvalidUserId : ActivateSessionResult

    data object SessionSaveFailure : ActivateSessionResult
}
