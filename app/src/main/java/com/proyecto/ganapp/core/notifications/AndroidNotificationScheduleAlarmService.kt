package com.proyecto.ganapp.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import com.proyecto.ganapp.domain.service.NotificationScheduleAlarmService
import com.proyecto.ganapp.domain.service.ScheduleAlarmCancelResult
import com.proyecto.ganapp.domain.service.ScheduleAlarmResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class AndroidNotificationScheduleAlarmService @Inject constructor(
    @ApplicationContext private val context: Context,
) : NotificationScheduleAlarmService {
    override fun schedule(scheduleId: Long, triggerAtMillis: Long): ScheduleAlarmResult {
        if (scheduleId <= 0L) return ScheduleAlarmResult.InvalidScheduleId
        if (triggerAtMillis <= 0L || triggerAtMillis <= System.currentTimeMillis()) {
            return ScheduleAlarmResult.InvalidTriggerTime
        }
        return try {
            val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val identity = NotificationScheduleAlarmIdentity.of(scheduleId)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                identity.requestCode,
                NotificationScheduleAlarmIntentContract.intent(context, scheduleId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            ScheduleAlarmResult.Scheduled
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            ScheduleAlarmResult.Failure(exception)
        }
    }

    override fun cancel(scheduleId: Long): ScheduleAlarmCancelResult {
        if (scheduleId <= 0L) return ScheduleAlarmCancelResult.InvalidScheduleId
        return try {
            val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val identity = NotificationScheduleAlarmIdentity.of(scheduleId)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                identity.requestCode,
                NotificationScheduleAlarmIntentContract.intent(context, scheduleId),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            if (pendingIntent != null) {
                manager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
            ScheduleAlarmCancelResult.Cancelled
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            ScheduleAlarmCancelResult.Failure(exception)
        }
    }
}
