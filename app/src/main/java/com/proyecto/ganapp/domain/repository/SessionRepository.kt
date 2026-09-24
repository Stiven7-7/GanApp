package com.proyecto.ganapp.domain.repository

import kotlinx.coroutines.flow.Flow

interface SessionRepository {

    fun observeAuthenticatedUserId(): Flow<Long?>

    suspend fun saveAuthenticatedUserId(userId: Long)

    suspend fun clearAuthenticatedUserId()
}
