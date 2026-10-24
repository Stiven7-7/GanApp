package com.proyecto.ganapp.core.notifications

data class NotificationAlarmIdentity(
    val action: String,
    val dataUri: String,
    val requestCode: Int,
) {
    companion object {
        const val REQUEST_CODE = 0

        fun of(notificationId: Long): NotificationAlarmIdentity {
            return NotificationAlarmIdentity(
                action = NotificationAlarmIntentContract.ACTION_NOTIFICATION_ALARM,
                dataUri = NotificationAlarmIntentContract.dataUri(notificationId),
                requestCode = REQUEST_CODE,
            )
        }
    }
}
