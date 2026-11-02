package com.proyecto.ganapp.core.notifications

import android.content.Context
import android.content.Intent
import android.net.Uri

object NotificationScheduleAlarmIntentContract {
    const val ACTION_NOTIFICATION_SCHEDULE_ALARM = "com.proyecto.ganapp.action.NOTIFICATION_SCHEDULE_ALARM"
    const val EXTRA_SCHEDULE_ID = "scheduleId"
    const val MISSING_SCHEDULE_ID = 0L
    const val SCHEME = "ganapp"
    const val HOST = "notification-schedule-alarm"

    fun dataUri(scheduleId: Long): String = "$SCHEME://$HOST/$scheduleId"

    fun intent(context: Context, scheduleId: Long): Intent {
        val identity = NotificationScheduleAlarmIdentity.of(scheduleId)
        return Intent(context, NotificationScheduleAlarmReceiver::class.java).apply {
            action = identity.action
            data = Uri.parse(identity.dataUri)
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
        }
    }
}
