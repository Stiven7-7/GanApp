package com.proyecto.ganapp.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.proyecto.ganapp.domain.usecase.notificacion.HandleNotificationScheduleAlarmResult
import com.proyecto.ganapp.domain.usecase.notificacion.HandleNotificationScheduleAlarmUseCase
import dagger.hilt.android.AndroidEntryPoint
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotificationScheduleAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var handleNotificationScheduleAlarmUseCase: HandleNotificationScheduleAlarmUseCase

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getLongExtra(
            NotificationScheduleAlarmIntentContract.EXTRA_SCHEDULE_ID,
            NotificationScheduleAlarmIntentContract.MISSING_SCHEDULE_ID,
        )
        if (scheduleId <= 0L) return
        val firedAtMillis = System.currentTimeMillis()
        val zoneId = ZoneId.systemDefault()
        val pendingResult: BroadcastReceiver.PendingResult? = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val prepared = handleNotificationScheduleAlarmUseCase.prepare(
                    scheduleId = scheduleId,
                    firedAtMillis = firedAtMillis,
                    zoneId = zoneId,
                )
                if (prepared is HandleNotificationScheduleAlarmResult.Display) {
                    NotificationHelper.show(
                        context = context,
                        title = prepared.title,
                        message = prepared.message,
                        notificationId = scheduleId.toInt(),
                    )
                }
                handleNotificationScheduleAlarmUseCase.armNext(
                    scheduleId = scheduleId,
                    firedAtMillis = firedAtMillis,
                    zoneId = zoneId,
                )
            } finally {
                pendingResult?.finish()
            }
        }
    }
}
