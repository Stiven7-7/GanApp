package com.proyecto.ganapp.domain.usecase.session

import com.proyecto.ganapp.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ClearSessionUseCaseTest {

    private lateinit var repository: FakeSessionRepository
    private lateinit var useCase: ClearSessionUseCase

    @Before
    fun setUp() {
        repository = FakeSessionRepository()
        useCase = ClearSessionUseCase(repository)
    }

    @Test
    fun cs01_repositorySuccess_devuelveSuccess() = runTest {
        val result = useCase()

        assertEquals(ClearSessionResult.Success, result)
        assertEquals(1, repository.clearCallCount)
    }

    @Test
    fun cs02_repositoryException_devuelveUnexpectedError() = runTest {
        repository.clearException = IllegalStateException("boom")

        val result = useCase()

        assertEquals(ClearSessionResult.UnexpectedError, result)
        assertEquals(1, repository.clearCallCount)
    }

    @Test
    fun cs03_unaInvocacion_llamaClearExactamenteUnaVez() = runTest {
        useCase()

        assertEquals(1, repository.clearCallCount)
    }

    private class FakeSessionRepository : SessionRepository {
        var clearCallCount = 0
        var clearException: Exception? = null

        override fun observeAuthenticatedUserId(): Flow<Long?> = flowOf(null)

        override suspend fun saveAuthenticatedUserId(userId: Long) {
            error("Unexpected call to saveAuthenticatedUserId")
        }

        override suspend fun clearAuthenticatedUserId() {
            clearCallCount += 1
            clearException?.let { throw it }
        }
    }
}
