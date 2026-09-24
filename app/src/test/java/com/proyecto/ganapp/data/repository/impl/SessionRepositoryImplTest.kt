package com.proyecto.ganapp.data.repository.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.UUID

class SessionRepositoryImplTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: SessionRepositoryImpl

    private val authenticatedUserIdKey = longPreferencesKey("authenticated_user_id")

    @Before
    fun setUp() {
        dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val file = File(temporaryFolder.newFolder(), "${UUID.randomUUID()}.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { file },
        )
        repository = SessionRepositoryImpl(dataStore)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun sr01_ausenciaDeClave_emiteNull() = runTest {
        assertNull(repository.observeAuthenticatedUserId().first())
    }

    @Test
    fun sr02_savePositivo_emiteId() = runTest {
        repository.saveAuthenticatedUserId(42L)

        assertEquals(42L, repository.observeAuthenticatedUserId().first())
    }

    @Test
    fun sr03_segundoSave_reemplazaId() = runTest {
        repository.saveAuthenticatedUserId(42L)
        repository.saveAuthenticatedUserId(84L)

        assertEquals(84L, repository.observeAuthenticatedUserId().first())
        val preferences = dataStore.data.first()
        assertEquals(1, preferences.asMap().size)
        assertEquals(84L, preferences[authenticatedUserIdKey])
    }

    @Test
    fun sr04_clear_eliminaClaveYNoEscribeSentinel() = runTest {
        repository.saveAuthenticatedUserId(42L)
        repository.clearAuthenticatedUserId()

        assertNull(repository.observeAuthenticatedUserId().first())
        val preferences = dataStore.data.first()
        assertFalse(preferences.contains(authenticatedUserIdKey))
        assertNull(preferences[authenticatedUserIdKey])
    }

    @Test
    fun sr05_valorInvalidoLeidoYSaveRechazado() = runTest {
        dataStore.edit { it[authenticatedUserIdKey] = 0L }
        assertNull(repository.observeAuthenticatedUserId().first())

        dataStore.edit { it[authenticatedUserIdKey] = -1L }
        assertNull(repository.observeAuthenticatedUserId().first())

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.saveAuthenticatedUserId(0L) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.saveAuthenticatedUserId(-1L) }
        }

        assertNull(repository.observeAuthenticatedUserId().first())
        val preferences = dataStore.data.first()
        val stored = preferences[authenticatedUserIdKey]
        assertFalse(stored != null && stored > 0L)
    }

    @Test
    fun sr06_unicaClavePersistida() = runTest {
        repository.saveAuthenticatedUserId(42L)

        val preferences = dataStore.data.first()
        assertEquals(1, preferences.asMap().size)
        assertEquals(true, preferences.contains(authenticatedUserIdKey))
        assertEquals(42L, preferences[authenticatedUserIdKey])
    }
}
