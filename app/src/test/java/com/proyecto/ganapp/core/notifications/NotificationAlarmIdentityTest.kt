package com.proyecto.ganapp.core.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationAlarmIdentityTest {

    @Test
    fun mismoId_produceLaMismaIdentidad() {
        assertEquals(
            NotificationAlarmIdentity.of(42L),
            NotificationAlarmIdentity.of(42L),
        )
    }

    @Test
    fun idsDistintos_producenDataDistinta() {
        val first = NotificationAlarmIdentity.of(42L)
        val second = NotificationAlarmIdentity.of(43L)

        assertNotEquals(first.dataUri, second.dataUri)
        assertEquals(first.action, second.action)
        assertEquals(first.requestCode, second.requestCode)
    }

    @Test
    fun longMaxValue_conservaElIdCompletoEnLaUri() {
        val identity = NotificationAlarmIdentity.of(Long.MAX_VALUE)

        assertEquals(
            "ganapp://notification-alarm/${Long.MAX_VALUE}",
            identity.dataUri,
        )
        assertTrue(identity.dataUri.contains(Long.MAX_VALUE.toString()))
        assertEquals(0, identity.requestCode)
        assertEquals(
            NotificationAlarmIdentity.of(42L).requestCode,
            identity.requestCode,
        )
    }

    @Test
    fun actionYDataUri_sonEstables() {
        val identity = NotificationAlarmIdentity.of(42L)

        assertEquals(
            "com.proyecto.ganapp.action.NOTIFICATION_ALARM",
            identity.action,
        )
        assertEquals("ganapp://notification-alarm/42", identity.dataUri)
    }
}
