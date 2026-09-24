package com.proyecto.ganapp.ui.features.home

import com.proyecto.ganapp.domain.model.Usuario
import com.proyecto.ganapp.domain.repository.RegisterUserRepositoryResult
import com.proyecto.ganapp.domain.repository.SessionRepository
import com.proyecto.ganapp.domain.repository.UsuarioRepository
import com.proyecto.ganapp.domain.usecase.session.ClearSessionUseCase
import com.proyecto.ganapp.domain.usecase.usuario.GetUsuarioByIdUseCase
import com.proyecto.ganapp.testutil.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var usuarioRepository: FakeUsuarioRepository
    private lateinit var sessionRepository: FakeSessionRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        usuarioRepository = FakeUsuarioRepository()
        sessionRepository = FakeSessionRepository()
        viewModel = HomeViewModel(
            getUsuarioByIdUseCase = GetUsuarioByIdUseCase(usuarioRepository),
            clearSessionUseCase = ClearSessionUseCase(sessionRepository),
        )
    }

    @Test
    fun hvm01_estadoLogoutInicial_noLoadingSinError() {
        val state = viewModel.logoutUiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun hvm02_logoutSuccess_llamaClearUnaVezYFinalizaSinError() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel.logout()
            advanceUntilIdle()

            assertEquals(1, sessionRepository.clearCallCount)
            val state = viewModel.logoutUiState.value
            assertFalse(state.isLoading)
            assertNull(state.error)
        }

    @Test
    fun hvm03_logoutPendiente_mantieneLoading() =
        runTest(mainDispatcherRule.testDispatcher) {
            sessionRepository.clearGate = CompletableDeferred()

            viewModel.logout()
            advanceUntilIdle()

            assertEquals(1, sessionRepository.clearCallCount)
            val pending = viewModel.logoutUiState.value
            assertTrue(pending.isLoading)
            assertNull(pending.error)

            sessionRepository.clearGate!!.complete(Unit)
            advanceUntilIdle()
        }

    @Test
    fun hvm04_doubleTapDuranteClearPendiente_invocaUnaSolaVez() =
        runTest(mainDispatcherRule.testDispatcher) {
            sessionRepository.clearGate = CompletableDeferred()

            viewModel.logout()
            advanceUntilIdle()

            viewModel.logout()

            assertEquals(1, sessionRepository.clearCallCount)

            sessionRepository.clearGate!!.complete(Unit)
            advanceUntilIdle()

            val state = viewModel.logoutUiState.value
            assertFalse(state.isLoading)
            assertNull(state.error)
        }

    @Test
    fun hvm05_clearFailure_muestraUnexpectedYTerminaLoading() =
        runTest(mainDispatcherRule.testDispatcher) {
            sessionRepository.clearException = IllegalStateException("boom")

            viewModel.logout()
            advanceUntilIdle()

            assertEquals(1, sessionRepository.clearCallCount)
            val state = viewModel.logoutUiState.value
            assertFalse(state.isLoading)
            assertEquals(HomeLogoutError.UNEXPECTED, state.error)
        }

    @Test
    fun hvm06_retryDespuesDeFailure_segundoIntentoSuccessLimpiaError() =
        runTest(mainDispatcherRule.testDispatcher) {
            sessionRepository.clearException = IllegalStateException("boom")

            viewModel.logout()
            advanceUntilIdle()

            assertEquals(1, sessionRepository.clearCallCount)
            assertEquals(HomeLogoutError.UNEXPECTED, viewModel.logoutUiState.value.error)
            assertFalse(viewModel.logoutUiState.value.isLoading)

            sessionRepository.clearException = null
            viewModel.logout()

            val retrying = viewModel.logoutUiState.value
            assertTrue(retrying.isLoading)
            assertNull(retrying.error)

            advanceUntilIdle()

            assertEquals(2, sessionRepository.clearCallCount)
            val state = viewModel.logoutUiState.value
            assertFalse(state.isLoading)
            assertNull(state.error)
        }

    @Test
    fun hvm07_cargarUsuario_preservaComportamientoExistente() =
        runTest(mainDispatcherRule.testDispatcher) {
            usuarioRepository.usersById[42L] = usuario(id = 42L)

            viewModel.cargarUsuario(42L)
            advanceUntilIdle()

            val loaded = viewModel.usuario.value
            assertEquals(42L, loaded?.idUsuario)
            assertEquals(usuario(id = 42L), loaded)
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

    private class FakeUsuarioRepository : UsuarioRepository {
        val usersById = mutableMapOf<Long, Usuario>()

        override suspend fun getUserById(id: Long): Usuario? {
            return usersById[id]
        }

        override suspend fun login(correo: String, contrasena: String): Usuario? {
            error("Unexpected call to login")
        }

        override suspend fun register(usuario: Usuario): RegisterUserRepositoryResult {
            error("Unexpected call to register")
        }

        override suspend fun getUsuarioByCorreo(correo: String): Usuario? {
            error("Unexpected call to getUsuarioByCorreo")
        }
    }

    private class FakeSessionRepository : SessionRepository {
        var clearCallCount = 0
        var clearException: Exception? = null
        var clearGate: CompletableDeferred<Unit>? = null

        override fun observeAuthenticatedUserId(): Flow<Long?> = flowOf(null)

        override suspend fun saveAuthenticatedUserId(userId: Long) {
            error("Unexpected call to saveAuthenticatedUserId")
        }

        override suspend fun clearAuthenticatedUserId() {
            clearCallCount += 1
            clearGate?.await()
            clearException?.let { throw it }
        }
    }
}
