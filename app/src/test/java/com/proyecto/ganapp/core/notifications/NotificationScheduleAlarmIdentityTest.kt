package com.proyecto.ganapp.core.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NotificationScheduleAlarmIdentityTest {
    @Test
    fun mismoIdMantieneIdentidadDeterminista() {
        assertEquals(NotificationScheduleAlarmIdentity.of(101L), NotificationScheduleAlarmIdentity.of(101L))
    }

    @Test
    fun idsDistintosSeSeparanPorUriSinDependerDelRequestCode() {
        val first = NotificationScheduleAlarmIdentity.of(101L)
        val second = NotificationScheduleAlarmIdentity.of(102L)
        assertNotEquals(first.dataUri, second.dataUri)
        assertEquals(first.action, second.action)
        assertEquals(0, first.requestCode)
        assertEquals(first.requestCode, second.requestCode)
    }

    @Test
    fun idsLongNoSeReducenAIntParaGarantizarUnicidad() {
        val first = NotificationScheduleAlarmIdentity.of(1L)
        val narrowedCollision = NotificationScheduleAlarmIdentity.of(1L + (1L shl 32))
        assertNotEquals(first.dataUri, narrowedCollision.dataUri)
        assertEquals("ganapp://notification-schedule-alarm/${Long.MAX_VALUE}",
            NotificationScheduleAlarmIdentity.of(Long.MAX_VALUE).dataUri)
    }

    @Test
    fun namespaceNuevoNoColisionaConLegacyParaMismoId() {
        val legacy = NotificationAlarmIdentity.of(101L)
        val schedule = NotificationScheduleAlarmIdentity.of(101L)
        assertNotEquals(legacy.action, schedule.action)
        assertNotEquals(legacy.dataUri, schedule.dataUri)
    }

    @Test
    fun contratoDeIdentidadYPayloadEsEstable() {
        val identity = NotificationScheduleAlarmIdentity.of(101L)
        assertEquals("com.proyecto.ganapp.action.NOTIFICATION_SCHEDULE_ALARM", identity.action)
        assertEquals("ganapp://notification-schedule-alarm/101", identity.dataUri)
        assertEquals("scheduleId", NotificationScheduleAlarmIntentContract.EXTRA_SCHEDULE_ID)
        assertEquals(0L, NotificationScheduleAlarmIntentContract.MISSING_SCHEDULE_ID)
    }
}
