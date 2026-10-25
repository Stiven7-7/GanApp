package com.proyecto.ganapp.ui.features.notifications

import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.EspecieAnimal
import com.proyecto.ganapp.domain.model.Notificacion
import com.proyecto.ganapp.domain.repository.AnimalRepository
import com.proyecto.ganapp.domain.repository.NotificacionRepository
import com.proyecto.ganapp.domain.repository.NotificacionxAnimalRepository
import com.proyecto.ganapp.domain.usecase.notificacion.AssignNotificationToAnimalUseCase
import com.proyecto.ganapp.domain.usecase.notificacion.GetNotificationsUseCase
import com.proyecto.ganapp.testutil.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AssignNotificationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var notifications: AssignUiNotificacionRepository
    private lateinit var animals: AssignUiAnimalRepository
    private lateinit var links: AssignUiLinkRepository
    private lateinit var viewModel: AssignNotificationViewModel

    @Before
    fun setUp() {
        notifications = AssignUiNotificacionRepository()
        animals = AssignUiAnimalRepository()
        links = AssignUiLinkRepository()
        viewModel = AssignNotificationViewModel(
            getNotificationsUseCase = GetNotificationsUseCase(notifications),
            assignNotificationToAnimalUseCase = AssignNotificationToAnimalUseCase(
                animalRepository = animals,
                notificationRepository = notifications,
                linkRepository = links,
            ),
        )
    }

    @Test
    fun loadNotifications_consultaGetNotificationsUseCaseDelUsuario() =
        runTest(mainDispatcherRule.testDispatcher) {
            notifications.list = listOf(notification(id = 9L, ownerId = 7L))

            viewModel.loadNotifications(7L)
            advanceUntilIdle()

            assertEquals(listOf(7L), notifications.requestedUserIds)
            assertEquals(listOf(notification(id = 9L, ownerId = 7L)), viewModel.notifications.value)
        }

    @Test
    fun success_emiteSuccess() = runTest(mainDispatcherRule.testDispatcher) {
        val events = collectEvents()
        animals.animal = animal(ownerId = 7L)
        notifications.byId[9L] = notification(id = 9L, ownerId = 7L)

        viewModel.assign(userId = 7L, animalId = 4L, notificationId = 9L)
        advanceUntilIdle()

        assertEquals(listOf(AssignNotificationEvent.Success), events)
        assertEquals(1, links.assignCalls)
        assertFalse(viewModel.isAssigning.value)
    }

    @Test
    fun invalidInput_noEmiteSuccess() = runTest(mainDispatcherRule.testDispatcher) {
        val events = collectEvents()

        viewModel.assign(userId = 0L, animalId = 4L, notificationId = 9L)
        advanceUntilIdle()

        assertEquals(listOf(AssignNotificationEvent.InvalidInput), events)
        assertEquals(0, links.assignCalls)
        assertFalse(viewModel.isAssigning.value)
    }

    @Test
    fun animalNotFound_noEmiteSuccess() = runTest(mainDispatcherRule.testDispatcher) {
        val events = collectEvents()
        animals.animal = null

        viewModel.assign(userId = 7L, animalId = 4L, notificationId = 9L)
        advanceUntilIdle()

        assertEquals(listOf(AssignNotificationEvent.AnimalNotFound), events)
        assertFalse(viewModel.isAssigning.value)
    }

    @Test
    fun notificationNotFound_noEmiteSuccess() = runTest(mainDispatcherRule.testDispatcher) {
        val events = collectEvents()
        animals.animal = animal(ownerId = 7L)

        viewModel.assign(userId = 7L, animalId = 4L, notificationId = 9L)
        advanceUntilIdle()

        assertEquals(listOf(AssignNotificationEvent.NotificationNotFound), events)
        assertFalse(viewModel.isAssigning.value)
    }

    @Test
    fun ownershipMismatch_noEmiteSuccess() = runTest(mainDispatcherRule.testDispatcher) {
        val events = collectEvents()
        animals.animal = animal(ownerId = 8L)
        notifications.byId[9L] = notification(id = 9L, ownerId = 8L)

        viewModel.assign(userId = 7L, animalId = 4L, notificationId = 9L)
        advanceUntilIdle()

        assertEquals(listOf(AssignNotificationEvent.OwnershipMismatch), events)
        assertEquals(0, links.assignCalls)
        assertFalse(viewModel.isAssigning.value)
    }

    @Test
    fun failure_noEmiteSuccess() = runTest(mainDispatcherRule.testDispatcher) {
        val events = collectEvents()
        animals.animal = animal(ownerId = 7L)
        notifications.byId[9L] = notification(id = 9L, ownerId = 7L)
        links.assignError = IllegalStateException("link")

        viewModel.assign(userId = 7L, animalId = 4L, notificationId = 9L)
        advanceUntilIdle()

        assertEquals(listOf(AssignNotificationEvent.Failure), events)
        assertFalse(viewModel.isAssigning.value)
    }

    @Test
    fun dobleTap_asignaUnaSolaVez() = runTest(mainDispatcherRule.testDispatcher) {
        collectEvents()
        animals.animal = animal(ownerId = 7L)
        notifications.byId[9L] = notification(id = 9L, ownerId = 7L)
        links.assignGate = CompletableDeferred()

        viewModel.assign(userId = 7L, animalId = 4L, notificationId = 9L)
        assertTrue(viewModel.isAssigning.value)
        viewModel.assign(userId = 7L, animalId = 4L, notificationId = 9L)
        advanceUntilIdle()

        assertEquals(1, links.assignCalls)

        links.assignGate!!.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, links.assignCalls)
        assertFalse(viewModel.isAssigning.value)
    }

    private fun kotlinx.coroutines.test.TestScope.collectEvents(): MutableList<AssignNotificationEvent> {
        val events = mutableListOf<AssignNotificationEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events += it }
        }
        return events
    }

    private fun animal(ownerId: Long): Animal {
        return Animal(
            idAnimal = 4L,
            especie = EspecieAnimal.VACA,
            nombre = "Lola",
            raza = "Holstein",
            edad = null,
            peso = null,
            color = null,
            fotoUri = null,
            idUsuario = ownerId,
        )
    }

    private fun notification(id: Long, ownerId: Long): Notificacion {
        return Notificacion(
            idNotificacion = id,
            nombre = "Vacuna",
            tipo = "Vacunación",
            fechaInicio = 20_000L,
            fechaFin = 20_000L,
            seRepite = "NO",
            hora = "08:00",
            idUsuario = ownerId,
        )
    }
}

private class AssignUiNotificacionRepository : NotificacionRepository {
    val requestedUserIds = mutableListOf<Long>()
    var list: List<Notificacion> = emptyList()
    val byId = mutableMapOf<Long, Notificacion>()

    override fun getAllNotifications(userId: Long): Flow<List<Notificacion>> {
        requestedUserIds += userId
        return flowOf(list)
    }

    override suspend fun getNotificationById(id: Long): Notificacion? = byId[id]

    override suspend fun insertNotification(notificacion: Notificacion): Long = 0L

    override suspend fun updateNotification(notificacion: Notificacion) = Unit

    override suspend fun deleteNotification(id: Long) = Unit
}

private class AssignUiAnimalRepository : AnimalRepository {
    var animal: Animal? = null

    override suspend fun getAnimalById(id: Long): Animal? = animal

    override fun getAllAnimals(userId: Long): Flow<List<Animal>> = error("Unexpected")

    override fun getAnimalsByUser(userId: Long): Flow<List<Animal>> = error("Unexpected")

    override suspend fun insertAnimal(animal: Animal): Long = error("Unexpected")

    override suspend fun updateAnimal(animal: Animal): Boolean = error("Unexpected")

    override suspend fun deleteAnimal(id: Long) = error("Unexpected")
}

private class AssignUiLinkRepository : NotificacionxAnimalRepository {
    var assignCalls: Int = 0
    var assignError: Throwable? = null
    var assignGate: CompletableDeferred<Unit>? = null

    override suspend fun assignNotificationToAnimal(idAnimal: Long, idNotificacion: Long) {
        assignCalls += 1
        assignGate?.await()
        assignError?.let { throw it }
    }

    override suspend fun removeAssignment(idAnimal: Long, idNotificacion: Long) = Unit

    override suspend fun getNotificationIdsOfAnimal(idAnimal: Long): List<Long> = error("Unexpected")

    override suspend fun getAnimalIdsWithNotification(idNotificacion: Long): List<Long> {
        error("Unexpected")
    }
}
