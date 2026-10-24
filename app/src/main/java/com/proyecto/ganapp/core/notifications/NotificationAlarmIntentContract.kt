package com.proyecto.ganapp.core.notifications

object NotificationAlarmIntentContract {
    const val ACTION_NOTIFICATION_ALARM = "com.proyecto.ganapp.action.NOTIFICATION_ALARM"
    const val EXTRA_NOTIFICATION_ID = "notificationId"
    const val EXTRA_TITLE = "title"
    const val EXTRA_MESSAGE = "message"
    const val SCHEME = "ganapp"
    const val HOST = "notification-alarm"
    const val MISSING_NOTIFICATION_ID = 0L

    fun dataUri(notificationId: Long): String {
        return "$SCHEME://$HOST/$notificationId"
    }
}
