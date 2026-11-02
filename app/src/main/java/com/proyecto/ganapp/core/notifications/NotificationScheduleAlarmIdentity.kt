package com.proyecto.ganapp.core.notifications

data class NotificationScheduleAlarmIdentity(
    val action: String,
    val dataUri: String,
    val requestCode: Int,
) {
    companion object {
        const val REQUEST_CODE = 0

        fun of(scheduleId: Long): NotificationScheduleAlarmIdentity =
            NotificationScheduleAlarmIdentity(
                action = NotificationScheduleAlarmIntentContract.ACTION_NOTIFICATION_SCHEDULE_ALARM,
                dataUri = NotificationScheduleAlarmIntentContract.dataUri(scheduleId),
                requestCode = REQUEST_CODE,
            )
    }
}
