package com.proyecto.ganapp.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.proyecto.ganapp.domain.usecase.notificacion.HandleNotificationAlarmUseCase
import dagger.hilt.android.AndroidEntryPoint
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var handleNotificationAlarmUseCase: HandleNotificationAlarmUseCase

    override fun onReceive(context: Context, intent: Intent) {
        val alarm = readAlarm(intent)
        NotificationHelper.show(context, alarm.title, alarm.message)
        if (alarm.notificationId <= 0L) {
            return
        }

        val firedAtMillis = System.currentTimeMillis()
        val zoneId = ZoneId.systemDefault()
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                handleNotificationAlarmUseCase(
                    notificationId = alarm.notificationId,
                    firedAtMillis = firedAtMillis,
                    zoneId = zoneId,
                )
            } finally {
                pendingResult.finish()
            }
        }
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
