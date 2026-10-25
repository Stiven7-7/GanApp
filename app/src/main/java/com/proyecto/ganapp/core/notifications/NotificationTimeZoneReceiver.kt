package com.proyecto.ganapp.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.proyecto.ganapp.domain.usecase.notificacion.ReconcileNotificationAlarmsUseCase
import dagger.hilt.android.AndroidEntryPoint
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotificationTimeZoneReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reconcileNotificationAlarmsUseCase: ReconcileNotificationAlarmsUseCase

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_TIMEZONE_CHANGED) {
            return
        }

        val now = System.currentTimeMillis()
        val zoneId = ZoneId.systemDefault()
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                reconcileNotificationAlarmsUseCase(
                    now = now,
                    zoneId = zoneId,
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
