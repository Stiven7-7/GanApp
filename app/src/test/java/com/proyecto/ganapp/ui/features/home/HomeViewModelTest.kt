package com.proyecto.ganapp.ui.features.home

import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.EspecieAnimal
import com.proyecto.ganapp.domain.model.Notificacion
import com.proyecto.ganapp.domain.model.NotificationAlarmRequest
import com.proyecto.ganapp.domain.model.NotificationCancelResult
import com.proyecto.ganapp.domain.model.NotificationScheduleResult
import com.proyecto.ganapp.domain.model.Usuario
import com.proyecto.ganapp.domain.repository.AnimalRepository
import com.proyecto.ganapp.domain.repository.NotificacionRepository
import com.proyecto.ganapp.domain.repository.RegisterUserRepositoryResult
import com.proyecto.ganapp.domain.repository.SessionRepository
import com.proyecto.ganapp.domain.repository.UsuarioRepository
import com.proyecto.ganapp.domain.usecase.animal.GetFavoriteAnimalsUseCase
import com.proyecto.ganapp.domain.service.NotificationAlarmScheduler
import com.proyecto.ganapp.domain.usecase.notificacion.CancelAuthenticatedUserNotificationAlarmsUseCase
import com.proyecto.ganapp.domain.usecase.notificacion.ScheduleNotificationUseCase
import com.proyecto.ganapp.domain.usecase.session.ClearSessionUseCase
import com.proyecto.ganapp.domain.usecase.session.LogoutUseCase
import com.proyecto.ganapp.domain.usecase.usuario.GetUsuarioByIdUseCase
import com.proyecto.ganapp.testutil.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
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
    private lateinit var favoriteSession: FavoriteSessionRepository
    private lateinit var favoriteAnimals: FavoriteAnimalRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        usuarioRepository = FakeUsuarioRepository()
        sessionRepository = FakeSessionRepository()
        favoriteSession = FavoriteSessionRepository()
        favoriteAnimals = FavoriteAnimalRepository()
        viewModel = HomeViewModel(
            getUsuarioByIdUseCase = GetUsuarioByIdUseCase(usuarioRepository),
            logoutUseCase = LogoutUseCase(
                cancelAuthenticatedUserNotificationAlarmsUseCase =
                    CancelAuthenticatedUserNotificationAlarmsUseCase(
                        sessionRepository = sessionRepository,
                        notificationRepository = HomeAlarmNotificacionRepository(),
                        scheduleNotificationUseCase = ScheduleNotificationUseCase(
                            HomeAlarmNotificationScheduler(),
                        ),
                    ),
                clearSessionUseCase = ClearSessionUseCase(sessionRepository),
            ),
            getFavoriteAnimalsUseCase = GetFavoriteAnimalsUseCase(
                animalRepository = favoriteAnimals,
                sessionRepository = favoriteSession,
            ),
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

            assertEquals(1, sessionRepository.observeCalls)
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
    fun hvm08_falloCancelandoAlarmas_muestraUnexpectedSinLimpiarSesion() =
        runTest(mainDispatcherRule.testDispatcher) {
            sessionRepository.observeError = IllegalStateException("session")

            viewModel.logout()
            advanceUntilIdle()

            assertEquals(0, sessionRepository.clearCallCount)
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

    @Test
    fun favoritos_empiezanEnLoading() {
        assertEquals(HomeFavoritesUiState.Loading, viewModel.favoritesUiState.value)
    }

    @Test
    fun favoritosVacios_publicanSuccessVacio() = runTest(mainDispatcherRule.testDispatcher) {
        advanceUntilIdle()

        assertEquals(
            HomeFavoritesUiState.Success(emptyList()),
            viewModel.favoritesUiState.value,
        )
        assertTrue(favoriteAnimals.requestedUserIds.isEmpty())
    }

    @Test
    fun unFavorito_publicaSuccess() = runTest(mainDispatcherRule.testDispatcher) {
        val lola = favoriteAnimal(id = 4L, nombre = "Lola")
        favoriteSession.userIds.value = 7L
        favoriteAnimals.emit(7L, listOf(lola))

        advanceUntilIdle()

        assertEquals(HomeFavoritesUiState.Success(listOf(lola)), viewModel.favoritesUiState.value)
    }

    @Test
    fun variosFavoritos_recibeLaListaCompletaOrdenada() = runTest(mainDispatcherRule.testDispatcher) {
        favoriteSession.userIds.value = 7L
        favoriteAnimals.emit(
            7L,
            listOf(
                favoriteAnimal(id = 1L, nombre = "zeta"),
                favoriteAnimal(id = 2L, nombre = "Ana"),
                favoriteAnimal(id = 3L, nombre = "lola"),
                favoriteAnimal(id = 4L, nombre = "Manolo"),
                favoriteAnimal(id = 5L, nombre = "Nube"),
                favoriteAnimal(id = 6L, nombre = "Bruno"),
            ),
        )

        advanceUntilIdle()

        val state = viewModel.favoritesUiState.value as HomeFavoritesUiState.Success
        assertEquals(
            listOf("Ana", "Bruno", "lola", "Manolo", "Nube", "zeta"),
            state.animals.map { it.nombre },
        )
    }

    @Test
    fun nuevaEmision_actualizaFavoritosSinRecrearViewModel() = runTest(mainDispatcherRule.testDispatcher) {
        favoriteSession.userIds.value = 7L
        favoriteAnimals.emit(7L, listOf(favoriteAnimal(id = 1L, nombre = "Lola")))
        advanceUntilIdle()

        favoriteAnimals.emit(7L, listOf(favoriteAnimal(id = 2L, nombre = "Nube")))
        advanceUntilIdle()

        val state = viewModel.favoritesUiState.value as HomeFavoritesUiState.Success
        assertEquals(listOf("Nube"), state.animals.map { it.nombre })
    }

    @Test
    fun favoritoFalseATrue_yTrueAFalse() = runTest(mainDispatcherRule.testDispatcher) {
        favoriteSession.userIds.value = 7L
        favoriteAnimals.emit(7L, emptyList())
        advanceUntilIdle()
        assertEquals(HomeFavoritesUiState.Success(emptyList()), viewModel.favoritesUiState.value)

        val lola = favoriteAnimal(id = 4L, nombre = "Lola")
        favoriteAnimals.emit(7L, listOf(lola))
        advanceUntilIdle()
        assertEquals(HomeFavoritesUiState.Success(listOf(lola)), viewModel.favoritesUiState.value)

        favoriteAnimals.emit(7L, emptyList())
        advanceUntilIdle()
        assertEquals(HomeFavoritesUiState.Success(emptyList()), viewModel.favoritesUiState.value)
    }

    @Test
    fun errorDeFavoritos_noAfectaUsuarioNiLogout() = runTest(mainDispatcherRule.testDispatcher) {
        val failure = IllegalStateException("db")
        favoriteSession.userIds.value = 7L
        favoriteAnimals.failure = failure
        usuarioRepository.usersById[42L] = usuario(id = 42L)

        viewModel.cargarUsuario(42L)
        advanceUntilIdle()

        val favorites = viewModel.favoritesUiState.value as HomeFavoritesUiState.Error
        assertTrue(favorites.cause is IllegalStateException)
        assertEquals("db", favorites.cause?.message)
        assertEquals(usuario(id = 42L), viewModel.usuario.value)
        assertFalse(viewModel.logoutUiState.value.isLoading)
        assertNull(viewModel.logoutUiState.value.error)
    }

    @Test
    fun cambioDeSesion_publicaFavoritosDelUsuarioActivo() = runTest(mainDispatcherRule.testDispatcher) {
        favoriteSession.userIds.value = 7L
        favoriteAnimals.emit(7L, listOf(favoriteAnimal(id = 1L, nombre = "Lola", owner = 7L)))
        favoriteAnimals.emit(9L, listOf(favoriteAnimal(id = 2L, nombre = "Nube", owner = 9L)))
        advanceUntilIdle()

        favoriteSession.userIds.value = 9L
        advanceUntilIdle()

        val state = viewModel.favoritesUiState.value as HomeFavoritesUiState.Success
        assertEquals(listOf("Nube"), state.animals.map { it.nombre })
        assertEquals(listOf(7L, 9L), favoriteAnimals.requestedUserIds)
    }

    private fun favoriteAnimal(
        id: Long,
        nombre: String,
        owner: Long = 7L,
    ) = Animal(
        idAnimal = id,
        especie = EspecieAnimal.VACA,
        nombre = nombre,
        raza = "Holstein",
        edad = 2,
        peso = 80f,
        color = "Blanco",
        fotoUri = null,
        idUsuario = owner,
    )

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
        var observeCalls = 0
        var observeError: Throwable? = null

        override fun observeAuthenticatedUserId(): Flow<Long?> {
            observeCalls += 1
            val failure = observeError
            if (failure != null) {
                return flow { throw failure }
            }
            return flowOf(null)
        }

        override suspend fun saveAuthenticatedUserId(userId: Long) {
            error("Unexpected call to saveAuthenticatedUserId")
        }

        override suspend fun clearAuthenticatedUserId() {
            clearCallCount += 1
            clearGate?.await()
            clearException?.let { throw it }
        }
    }

    private class FavoriteSessionRepository : SessionRepository {
        val userIds = MutableStateFlow<Long?>(null)

        override fun observeAuthenticatedUserId(): Flow<Long?> = userIds

        override suspend fun saveAuthenticatedUserId(userId: Long) = error("unused")

        override suspend fun clearAuthenticatedUserId() = error("unused")
    }

    private class FavoriteAnimalRepository : AnimalRepository {
        val requestedUserIds = mutableListOf<Long>()
        val flows = mutableMapOf<Long, MutableStateFlow<List<Animal>>>()
        var failure: Throwable? = null

        fun emit(userId: Long, animals: List<Animal>) {
            flows.getOrPut(userId) { MutableStateFlow(emptyList()) }.value = animals
        }

        override fun getFavoriteAnimalsByUser(userId: Long): Flow<List<Animal>> {
            requestedUserIds += userId
            failure?.let { error -> return flow { throw error } }
            return flows.getOrPut(userId) { MutableStateFlow(emptyList()) }
        }

        override fun getAllAnimals(userId: Long): Flow<List<Animal>> = flowOf(emptyList())

        override fun getAnimalsByUser(userId: Long): Flow<List<Animal>> = flowOf(emptyList())

        override suspend fun getAnimalById(id: Long): Animal? = null

        override suspend fun insertAnimal(animal: Animal): Long = error("unused")

        override suspend fun updateAnimal(animal: Animal): Boolean = error("unused")

        override suspend fun deleteAnimal(id: Long) = error("unused")
    }
}

private class HomeAlarmNotificacionRepository : NotificacionRepository {
    override fun getAllNotifications(userId: Long): Flow<List<Notificacion>> = flowOf(emptyList())

    override suspend fun insertNotification(notificacion: Notificacion): Long = 0L

    override suspend fun updateNotification(notificacion: Notificacion) = Unit

    override suspend fun deleteNotification(id: Long) = Unit

    override suspend fun getNotificationById(id: Long): Notificacion? = null
}

private class HomeAlarmNotificationScheduler : NotificationAlarmScheduler {
    override fun scheduleNext(request: NotificationAlarmRequest): NotificationScheduleResult {
        error("Unexpected call to scheduleNext")
    }

    override fun cancel(notificationId: Long): NotificationCancelResult {
        return NotificationCancelResult.Cancelled(notificationId)
    }
}
