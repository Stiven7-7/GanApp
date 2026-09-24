package com.proyecto.ganapp.ui.navigation

import com.proyecto.ganapp.domain.model.Usuario
import com.proyecto.ganapp.domain.repository.RegisterUserRepositoryResult
import com.proyecto.ganapp.domain.repository.SessionRepository
import com.proyecto.ganapp.domain.repository.UsuarioRepository
import com.proyecto.ganapp.domain.usecase.session.ObserveSessionUseCase
import com.proyecto.ganapp.testutil.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RootSessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var sessionRepository: FakeSessionRepository
    private lateinit var usuarioRepository: FakeUsuarioRepository

    @Before
    fun setUp() {
        sessionRepository = FakeSessionRepository()
        usuarioRepository = FakeUsuarioRepository()
    }

    @Test
    fun rv01_estadoInicial_esLoading() {
        val viewModel = createViewModel()

        assertEquals(RootSessionState.Loading, viewModel.uiState.value)
    }

    @Test
    fun rv02_sinId_devuelveUnauthenticated() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(RootSessionState.Unauthenticated, viewModel.uiState.value)
    }

    @Test
    fun rv03_idValidoUsuarioExiste_devuelveAuthenticated() =
        runTest(mainDispatcherRule.testDispatcher) {
            usuarioRepository.usersById[42L] = usuario(id = 42L)
            sessionRepository.emitUserId(42L)

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(RootSessionState.Authenticated(userId = 42L), viewModel.uiState.value)
        }

    @Test
    fun rv04_loadingNoSeSaltaAntesDePrimeraEmision() =
        runTest(mainDispatcherRule.testDispatcher) {
            sessionRepository.observeGate = CompletableDeferred()

            val viewModel = createViewModel()
            advanceUntilIdle()
            assertEquals(RootSessionState.Loading, viewModel.uiState.value)

            sessionRepository.observeGate!!.complete(Unit)
            advanceUntilIdle()

            assertEquals(RootSessionState.Unauthenticated, viewModel.uiState.value)
        }

    @Test
    fun rv05_unauthenticatedAAuthenticated_mismaInstancia() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()
            assertEquals(RootSessionState.Unauthenticated, viewModel.uiState.value)

            usuarioRepository.usersById[42L] = usuario(id = 42L)
            sessionRepository.emitUserId(42L)
            advanceUntilIdle()

            assertEquals(RootSessionState.Authenticated(userId = 42L), viewModel.uiState.value)
        }

    @Test
    fun rv06_authenticatedAUnauthenticated_mismaInstancia() =
        runTest(mainDispatcherRule.testDispatcher) {
            usuarioRepository.usersById[42L] = usuario(id = 42L)
            sessionRepository.emitUserId(42L)

            val viewModel = createViewModel()
            advanceUntilIdle()
            assertEquals(RootSessionState.Authenticated(userId = 42L), viewModel.uiState.value)

            sessionRepository.emitUserId(null)
            advanceUntilIdle()

            assertEquals(RootSessionState.Unauthenticated, viewModel.uiState.value)
        }

    @Test
    fun rv07_usaIdExacto() = runTest(mainDispatcherRule.testDispatcher) {
        usuarioRepository.usersById[987654321L] = usuario(id = 987654321L)
        sessionRepository.emitUserId(987654321L)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(
            RootSessionState.Authenticated(userId = 987654321L),
            viewModel.uiState.value,
        )
    }

    private fun createViewModel(): RootSessionViewModel {
        return RootSessionViewModel(
            observeSessionUseCase = ObserveSessionUseCase(
                sessionRepository = sessionRepository,
                usuarioRepository = usuarioRepository,
            ),
        )
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
        var observeGate: CompletableDeferred<Unit>? = null

        fun emitUserId(userId: Long?) {
            sessionFlow.value = userId
        }

        override fun observeAuthenticatedUserId(): Flow<Long?> {
            val gate = observeGate
            return if (gate != null) {
                flow {
                    gate.await()
                    emitAll(sessionFlow)
                }
            } else {
                sessionFlow
            }
        }

        override suspend fun saveAuthenticatedUserId(userId: Long) {
            sessionFlow.value = userId
        }

        override suspend fun clearAuthenticatedUserId() {
            sessionFlow.value = null
        }
    }

    private class FakeUsuarioRepository : UsuarioRepository {
        val usersById = mutableMapOf<Long, Usuario>()

        override suspend fun getUserById(id: Long): Usuario? {
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
