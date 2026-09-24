package com.proyecto.ganapp.data.repository.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.proyecto.ganapp.di.SessionDataStore
import com.proyecto.ganapp.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepositoryImpl @Inject constructor(
    @SessionDataStore
    private val dataStore: DataStore<Preferences>,
) : SessionRepository {

    override fun observeAuthenticatedUserId(): Flow<Long?> {
        return dataStore.data.map { preferences ->
            preferences[AUTHENTICATED_USER_ID]?.takeIf { it > 0L }
        }
    }

    override suspend fun saveAuthenticatedUserId(userId: Long) {
        require(userId > 0L) { "authenticated_user_id must be greater than 0" }
        dataStore.edit { preferences ->
            preferences[AUTHENTICATED_USER_ID] = userId
        }
    }

    override suspend fun clearAuthenticatedUserId() {
        dataStore.edit { preferences ->
            preferences.remove(AUTHENTICATED_USER_ID)
        }
    }

    private companion object {
        val AUTHENTICATED_USER_ID = longPreferencesKey("authenticated_user_id")
    }
}
