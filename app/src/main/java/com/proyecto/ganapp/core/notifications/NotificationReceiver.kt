package com.proyecto.ganapp.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarm = readAlarm(intent)
        NotificationHelper.show(context, alarm.title, alarm.message)
    }

    private fun readAlarm(intent: Intent): ReceivedAlarm {
        return ReceivedAlarm(
            notificationId = intent.getLongExtra(
                NotificationAlarmIntentContract.EXTRA_NOTIFICATION_ID,
                NotificationAlarmIntentContract.MISSING_NOTIFICATION_ID,
            ),
            title = intent.getStringExtra(NotificationAlarmIntentContract.EXTRA_TITLE)
                ?: "GanApp",
            message = intent.getStringExtra(NotificationAlarmIntentContract.EXTRA_MESSAGE)
                ?: "Tienes un recordatorio pendiente en tu ganado.",
        )
    }

    private data class ReceivedAlarm(
        val notificationId: Long,
        val title: String,
        val message: String,
    )
}
