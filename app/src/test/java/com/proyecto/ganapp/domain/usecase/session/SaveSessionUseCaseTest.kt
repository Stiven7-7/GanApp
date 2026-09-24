package com.proyecto.ganapp.domain.usecase.session

import com.proyecto.ganapp.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class SaveSessionUseCaseTest {

    private lateinit var repository: FakeSessionRepository
    private lateinit var useCase: SaveSessionUseCase

    @Before
    fun setUp() {
        repository = FakeSessionRepository()
        useCase = SaveSessionUseCase(repository)
    }

    @Test
    fun ss01_idValido_repositorySuccess_devuelveSuccess() = runTest {
        val result = useCase(42L)

        assertEquals(SaveSessionResult.Success, result)
        assertEquals(1, repository.saveCallCount)
        assertEquals(42L, repository.lastSavedUserId)
    }

    @Test
    fun ss02_idCero_devuelveInvalidUserIdYNoLlamaRepository() = runTest {
        val result = useCase(0L)

        assertEquals(SaveSessionResult.InvalidUserId, result)
        assertEquals(0, repository.saveCallCount)
        assertNull(repository.lastSavedUserId)
    }

    @Test
    fun ss03_idNegativo_devuelveInvalidUserIdYNoLlamaRepository() = runTest {
        val result = useCase(-1L)

        assertEquals(SaveSessionResult.InvalidUserId, result)
        assertEquals(0, repository.saveCallCount)
        assertNull(repository.lastSavedUserId)
    }

    @Test
    fun ss04_repositoryException_devuelveUnexpectedError() = runTest {
        repository.saveException = IllegalStateException("boom")

        val result = useCase(42L)

        assertEquals(SaveSessionResult.UnexpectedError, result)
        assertEquals(1, repository.saveCallCount)
        assertEquals(42L, repository.lastSavedUserId)
    }

    private class FakeSessionRepository : SessionRepository {
        var saveCallCount = 0
        var lastSavedUserId: Long? = null
        var saveException: Exception? = null

        override fun observeAuthenticatedUserId(): Flow<Long?> = flowOf(null)

        override suspend fun saveAuthenticatedUserId(userId: Long) {
            saveCallCount += 1
            lastSavedUserId = userId
            saveException?.let { throw it }
        }

        override suspend fun clearAuthenticatedUserId() = Unit
    }
}
