package com.proyecto.ganapp.domain.usecase.animal

import com.proyecto.ganapp.domain.repository.AnimalScheduleDeletionRepository
import com.proyecto.ganapp.domain.repository.SessionRepository
import com.proyecto.ganapp.domain.service.ScheduleAlarmCancelResult
import com.proyecto.ganapp.domain.usecase.notificacion.ScheduleTestFixture
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class DeleteAnimalWithNotificationSchedulesUseCaseTest {
    private class Fixture(count: Int = 3) {
        val data = ScheduleTestFixture()
        var userId: Long? = 7L
        var sessionFailure: Exception? = null
        var persistenceFailure: Exception? = null
        val session = object : SessionRepository {
            override fun observeAuthenticatedUserId() = flow {
                data.events += "session"
                sessionFailure?.let { throw it }
                emit(userId)
            }
            override suspend fun saveAuthenticatedUserId(userId: Long) = error("Unexpected session write")
            override suspend fun clearAuthenticatedUserId() = error("Unexpected session clear")
        }
        val deletion = object : AnimalScheduleDeletionRepository {
            override suspend fun deleteAnimalAndSchedules(animalId: Long) {
                data.events += "persist:$animalId"
                persistenceFailure?.let { throw it }
                data.schedules.entries.removeAll { it.value.idAnimal == animalId }
                data.animals.remove(animalId)
            }
        }

        init {
            data.schedules.clear()
            repeat(count) { index ->
                val id = 101L + index
                data.schedules[id] = data.schedule(id)
                data.activeAlarms[id] = data.now
            }
        }

        suspend fun delete(id: Long = 9L) = DeleteAnimalWithNotificationSchedulesUseCase(
            session, data.animalRepository, data.repository, data.alarms, deletion,
        )(id)

        fun assertNoWrites() {
            assertTrue(data.events.none { it.startsWith("persist:") || it.startsWith("delete:") })
            assertTrue(data.deleted.isEmpty())
            assertTrue(data.scheduled.isEmpty())
        }

        fun assertTemplatesIntact() {
            assertEquals(1, data.templates.size)
            assertEquals("Vacuna", data.templates[5L]!!.titulo)
            assertTrue(data.events.none { it.startsWith("template:") || it.startsWith("listTemplate:") })
        }
    }

    @Test fun noSessionOrNonPositiveUserStopsBeforeAnimal() = runTest {
        for (userId in listOf(null, 0L, -1L)) {
            val f = Fixture()
            f.userId = userId
            assertEquals(DeleteAnimalWithNotificationSchedulesResult.NoAuthenticatedUser, f.delete())
            assertEquals(listOf("session"), f.data.events)
            f.assertNoWrites()
        }
    }

    @Test fun invalidAnimalIdAfterSession() = runTest {
        for (id in listOf(0L, -1L)) {
            val f = Fixture()
            assertEquals(DeleteAnimalWithNotificationSchedulesResult.InvalidAnimalId, f.delete(id))
            assertEquals(listOf("session"), f.data.events)
            f.assertNoWrites()
        }
    }

    @Test fun missingAnimalStopsBeforeSchedules() = runTest {
        val f = Fixture()
        f.data.animals.clear()
        assertEquals(DeleteAnimalWithNotificationSchedulesResult.AnimalNotFound, f.delete())
        assertEquals(listOf("session", "animal:9"), f.data.events)
        f.assertNoWrites()
    }

    @Test fun foreignAnimalStopsBeforeSchedulesOrCancel() = runTest {
        val f = Fixture()
        f.data.animals[9L] = f.data.animals.getValue(9L).copy(idUsuario = 88L)
        assertEquals(DeleteAnimalWithNotificationSchedulesResult.OwnershipMismatch, f.delete())
        assertEquals(listOf("session", "animal:9"), f.data.events)
        assertTrue(f.data.cancelled.isEmpty())
        f.assertNoWrites()
    }

    @Test fun noSchedulesDeletesThroughSingleAtomicPortWithoutCancel() = runTest {
        val f = Fixture(0)
        assertEquals(DeleteAnimalWithNotificationSchedulesResult.Success, f.delete())
        assertEquals(listOf("session", "animal:9", "listAnimal:9", "persist:9"), f.data.events)
        assertTrue(f.data.cancelled.isEmpty())
        assertTrue(f.data.animals.isEmpty())
        f.assertTemplatesIntact()
    }

    @Test fun oneScheduleCancelsBeforePersistence() = runTest {
        val f = Fixture(1)
        assertEquals(DeleteAnimalWithNotificationSchedulesResult.Success, f.delete())
        assertEquals(listOf("session", "animal:9", "listAnimal:9", "cancel:101", "persist:9"), f.data.events)
        assertTrue(f.data.schedules.isEmpty())
        assertTrue(f.data.animals.isEmpty())
        f.assertTemplatesIntact()
    }

    @Test fun multipleSameTemplateAnimalSchedulesAllCancelThenOnePersistenceCall() = runTest {
        val f = Fixture()
        val unrelated = f.data.schedule(900L, animalId = 90L)
        f.data.schedules[900L] = unrelated
        assertEquals(DeleteAnimalWithNotificationSchedulesResult.Success, f.delete())
        assertEquals(
            listOf("session", "animal:9", "listAnimal:9", "cancel:101", "cancel:102", "cancel:103", "persist:9"),
            f.data.events,
        )
        assertEquals(listOf(101L, 102L, 103L), f.data.cancelled)
        assertEquals(mapOf(900L to unrelated), f.data.schedules)
        assertTrue(f.data.animals.isEmpty())
        assertTrue(f.data.scheduled.isEmpty())
        assertTrue(f.data.deleted.isEmpty())
        f.assertTemplatesIntact()
    }

    @Test fun firstCancelFailureKeepsAllRowsAndStopsLoop() = runTest {
        val f = Fixture()
        val cause = IllegalStateException("cancel failed")
        f.data.cancelResult = ScheduleAlarmCancelResult.Failure(cause)
        val schedules = f.data.schedules.toMap()
        val animals = f.data.animals.toMap()
        assertEquals(DeleteAnimalWithNotificationSchedulesResult.AlarmCancellationFailed(101L, cause), f.delete())
        assertEquals(listOf("session", "animal:9", "listAnimal:9", "cancel:101"), f.data.events)
        assertEquals(schedules, f.data.schedules)
        assertEquals(animals, f.data.animals)
        f.assertNoWrites()
        f.assertTemplatesIntact()
    }

    @Test fun intermediateCancelExceptionKeepsAllRowsWithoutAlarmRollback() = runTest {
        val f = Fixture(4)
        val cause = IllegalStateException("third cancel")
        f.data.failures["cancel:103"] = cause
        val schedules = f.data.schedules.toMap()
        val animals = f.data.animals.toMap()
        assertEquals(DeleteAnimalWithNotificationSchedulesResult.AlarmCancellationFailed(103L, cause), f.delete())
        assertEquals(listOf("session", "animal:9", "listAnimal:9", "cancel:101", "cancel:102", "cancel:103"), f.data.events)
        assertEquals(schedules, f.data.schedules)
        assertEquals(animals, f.data.animals)
        assertEquals(setOf(103L, 104L), f.data.activeAlarms.keys)
        f.assertNoWrites()
        f.assertTemplatesIntact()
    }

    @Test fun invalidCancelIdResultStopsPersistence() = runTest {
        val f = Fixture()
        f.data.cancelResult = ScheduleAlarmCancelResult.InvalidScheduleId
        assertEquals(DeleteAnimalWithNotificationSchedulesResult.AlarmCancellationFailed(101L), f.delete())
        f.assertNoWrites()
    }

    @Test fun persistenceFailureKeepsAtomicStateAfterAllCancelsWithoutReschedule() = runTest {
        val f = Fixture()
        val cause = IllegalStateException("transaction failed")
        f.persistenceFailure = cause
        val schedules = f.data.schedules.toMap()
        val animals = f.data.animals.toMap()
        assertEquals(DeleteAnimalWithNotificationSchedulesResult.PersistenceFailure(cause), f.delete())
        assertEquals(schedules, f.data.schedules)
        assertEquals(animals, f.data.animals)
        assertTrue(f.data.activeAlarms.isEmpty())
        assertTrue(f.data.scheduled.isEmpty())
        assertEquals(listOf("session", "animal:9", "listAnimal:9", "cancel:101", "cancel:102", "cancel:103", "persist:9"), f.data.events)
        f.assertTemplatesIntact()
    }

    @Test fun readFailuresReturnFailureWithoutCancellationOrPersistence() = runTest {
        for (stage in listOf("session", "animal:9", "listAnimal:9")) {
            val f = Fixture()
            val cause = IllegalStateException(stage)
            if (stage == "session") f.sessionFailure = cause else f.data.failures[stage] = cause
            val result = f.delete() as DeleteAnimalWithNotificationSchedulesResult.Failure
            assertEquals(cause.message, result.cause.message)
            assertTrue(f.data.cancelled.isEmpty())
            f.assertNoWrites()
        }
    }

    @Test fun cancellationPropagatesFromEveryDependency() = runTest {
        for (stage in listOf("session", "animal:9", "listAnimal:9", "cancel:102", "persist:9")) {
            val f = Fixture()
            val cause = CancellationException(stage)
            when (stage) {
                "session" -> f.sessionFailure = cause
                "persist:9" -> f.persistenceFailure = cause
                else -> f.data.failures[stage] = cause
            }
            val schedules = f.data.schedules.toMap()
            val animals = f.data.animals.toMap()
            try {
                f.delete()
                fail("Expected CancellationException from $stage")
            } catch (expected: CancellationException) {
                assertEquals(stage, expected.message)
            }
            assertEquals(schedules, f.data.schedules)
            assertEquals(animals, f.data.animals)
            assertTrue(f.data.scheduled.isEmpty())
            if (stage != "persist:9") f.assertNoWrites()
            f.assertTemplatesIntact()
        }
    }
}
