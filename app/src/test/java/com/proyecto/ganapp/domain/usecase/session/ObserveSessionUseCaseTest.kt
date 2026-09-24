package com.proyecto.ganapp.domain.usecase.session

import com.proyecto.ganapp.domain.model.Usuario
import com.proyecto.ganapp.domain.repository.RegisterUserRepositoryResult
import com.proyecto.ganapp.domain.repository.SessionRepository
import com.proyecto.ganapp.domain.repository.UsuarioRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveSessionUseCaseTest {

    private lateinit var sessionRepository: FakeSessionRepository
    private lateinit var usuarioRepository: FakeUsuarioRepository
    private lateinit var useCase: ObserveSessionUseCase

    @Before
    fun setUp() {
        sessionRepository = FakeSessionRepository()
        usuarioRepository = FakeUsuarioRepository()
        useCase = ObserveSessionUseCase(
            sessionRepository = sessionRepository,
            usuarioRepository = usuarioRepository,
        )
    }

    @Test
    fun os01_sinId_devuelveUnauthenticatedYNoConsultaRoom() = runTest {
        val results = mutableListOf<ObserveSessionResult>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase().collect { results += it }
        }
        advanceUntilIdle()

        assertEquals(listOf(ObserveSessionResult.Unauthenticated), results)
        assertEquals(0, usuarioRepository.getUserByIdCallCount)
        assertEquals(0, sessionRepository.clearCallCount)
    }

    @Test
    fun os02_idValidoUsuarioExiste_devuelveAuthenticated() = runTest {
        usuarioRepository.usersById[42L] = usuario(id = 42L)
        sessionRepository.emitUserId(42L)

        val results = mutableListOf<ObserveSessionResult>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase().collect { results += it }
        }
        advanceUntilIdle()

        assertEquals(listOf(ObserveSessionResult.Authenticated(userId = 42L)), results)
        assertEquals(1, usuarioRepository.getUserByIdCallCount)
        assertEquals(listOf(42L), usuarioRepository.requestedUserIds)
        assertEquals(0, sessionRepository.clearCallCount)
    }

    @Test
    fun os03_idValidoUsuarioNoExiste_limpiaYDevuelveUnauthenticated() = runTest {
        sessionRepository.emitUserId(42L)

        val results = mutableListOf<ObserveSessionResult>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase().collect { results += it }
        }
        advanceUntilIdle()

        assertEquals(listOf(ObserveSessionResult.Unauthenticated), results)
        assertEquals(1, usuarioRepository.getUserByIdCallCount)
        assertEquals(1, sessionRepository.clearCallCount)
        assertNull(sessionRepository.currentUserId())
    }

    @Test
    fun os04_huerfanoClearFalla_devuelveUnauthenticatedSinLoop() = runTest {
        sessionRepository.emitUserId(42L)
        sessionRepository.clearException = IllegalStateException("clear failed")

        val results = mutableListOf<ObserveSessionResult>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase().collect { results += it }
        }
        advanceUntilIdle()

        assertEquals(listOf(ObserveSessionResult.Unauthenticated), results)
        assertEquals(1, sessionRepository.clearCallCount)
    }

    @Test
    fun os05_errorDataStore_devuelveUnauthenticated() = runTest {
        sessionRepository.observeException = IllegalStateException("datastore failed")

        val results = mutableListOf<ObserveSessionResult>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase().collect { results += it }
        }
        advanceUntilIdle()

        assertEquals(listOf(ObserveSessionResult.Unauthenticated), results)
        assertEquals(0, usuarioRepository.getUserByIdCallCount)
        assertEquals(0, sessionRepository.clearCallCount)
    }

    @Test
    fun os06_errorRoom_devuelveUnauthenticatedSinClear() = runTest {
        sessionRepository.emitUserId(42L)
        usuarioRepository.getUserByIdException = IllegalStateException("room failed")

        val results = mutableListOf<ObserveSessionResult>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase().collect { results += it }
        }
        advanceUntilIdle()

        assertEquals(listOf(ObserveSessionResult.Unauthenticated), results)
        assertEquals(1, usuarioRepository.getUserByIdCallCount)
        assertEquals(0, sessionRepository.clearCallCount)
    }

    @Test
    fun os07_transicionNullAIdValido_esReactiva() = runTest {
        usuarioRepository.usersById[42L] = usuario(id = 42L)

        val results = mutableListOf<ObserveSessionResult>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase().collect { results += it }
        }
        advanceUntilIdle()
        assertEquals(listOf(ObserveSessionResult.Unauthenticated), results)
        assertEquals(0, usuarioRepository.getUserByIdCallCount)

        sessionRepository.emitUserId(42L)
        advanceUntilIdle()

        assertEquals(
            listOf(
                ObserveSessionResult.Unauthenticated,
                ObserveSessionResult.Authenticated(userId = 42L),
            ),
            results,
        )
        assertEquals(1, usuarioRepository.getUserByIdCallCount)
        assertEquals(listOf(42L), usuarioRepository.requestedUserIds)
    }

    @Test
    fun os08_transicionIdAIdB_esReactiva() = runTest {
        usuarioRepository.usersById[10L] = usuario(id = 10L)
        usuarioRepository.usersById[20L] = usuario(id = 20L)
        sessionRepository.emitUserId(10L)

        val results = mutableListOf<ObserveSessionResult>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase().collect { results += it }
        }
        advanceUntilIdle()
        assertEquals(listOf(ObserveSessionResult.Authenticated(userId = 10L)), results)

        sessionRepository.emitUserId(20L)
        advanceUntilIdle()

        assertEquals(
            listOf(
                ObserveSessionResult.Authenticated(userId = 10L),
                ObserveSessionResult.Authenticated(userId = 20L),
            ),
            results,
        )
        assertEquals(listOf(10L, 20L), usuarioRepository.requestedUserIds)
        assertEquals(0, sessionRepository.clearCallCount)
    }

    @Test
    fun os09_idNoPositivo_devuelveUnauthenticatedSinConsultar() = runTest {
        sessionRepository.emitUserId(0L)
        val zeroResults = mutableListOf<ObserveSessionResult>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase().collect { zeroResults += it }
        }
        advanceUntilIdle()
        assertEquals(listOf(ObserveSessionResult.Unauthenticated), zeroResults)
        assertEquals(0, usuarioRepository.getUserByIdCallCount)
        assertEquals(0, sessionRepository.clearCallCount)

        sessionRepository = FakeSessionRepository()
        usuarioRepository = FakeUsuarioRepository()
        useCase = ObserveSessionUseCase(sessionRepository, usuarioRepository)
        sessionRepository.emitUserId(-1L)

        val negativeResults = mutableListOf<ObserveSessionResult>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase().collect { negativeResults += it }
        }
        advanceUntilIdle()
        assertEquals(listOf(ObserveSessionResult.Unauthenticated), negativeResults)
        assertEquals(0, usuarioRepository.getUserByIdCallCount)
        assertEquals(0, sessionRepository.clearCallCount)
    }

    private fun usuario(id: Long): Usuario {
        return Usuario(
            idUsuario = id,
            nombre = "Ana",
            apellido = "Perez",
            correo = "user@example.com",
            contrasena = "unused",
        )
    }

    private class FakeSessionRepository : SessionRepository {
        private val sessionFlow = MutableStateFlow<Long?>(null)
        var observeException: Exception? = null
        var clearException: Exception? = null
        var clearCallCount = 0

        fun currentUserId(): Long? = sessionFlow.value

        suspend fun emitUserId(userId: Long?) {
            sessionFlow.value = userId
        }

        override fun observeAuthenticatedUserId(): Flow<Long?> {
            val exception = observeException
            return if (exception != null) {
                flow { throw exception }
            } else {
                sessionFlow
            }
        }

        override suspend fun saveAuthenticatedUserId(userId: Long) {
            sessionFlow.value = userId
        }

        override suspend fun clearAuthenticatedUserId() {
            clearCallCount += 1
            clearException?.let { throw it }
            sessionFlow.value = null
        }
    }

    private class FakeUsuarioRepository : UsuarioRepository {
        val usersById = mutableMapOf<Long, Usuario>()
        var getUserByIdCallCount = 0
        val requestedUserIds = mutableListOf<Long>()
        var getUserByIdException: Exception? = null

        override suspend fun getUserById(id: Long): Usuario? {
            getUserByIdCallCount += 1
            requestedUserIds += id
            getUserByIdException?.let { throw it }
            return usersById[id]
        }

        override suspend fun register(usuario: Usuario): RegisterUserRepositoryResult {
            error("Unexpected call to register")
        }

        override suspend fun login(correo: String, contrasena: String): Usuario? {
            error("Unexpected call to login")
        }

        override suspend fun getUsuarioByCorreo(correo: String): Usuario? {
            error("Unexpected call to getUsuarioByCorreo")
        }
    }
}
