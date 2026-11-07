package com.proyecto.ganapp.di

import com.proyecto.ganapp.core.notifications.AndroidLegacyNotificationAlarmCanceller
import com.proyecto.ganapp.core.notifications.AndroidNotificationScheduleAlarmService
import com.proyecto.ganapp.domain.service.LegacyNotificationAlarmCanceller
import com.proyecto.ganapp.domain.service.NotificationScheduleAlarmService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationSchedulerModule {

    @Binds
    @Singleton
    abstract fun bindNotificationScheduleAlarmService(
        impl: AndroidNotificationScheduleAlarmService,
    ): NotificationScheduleAlarmService

    @Binds
    @Singleton
    abstract fun bindLegacyNotificationAlarmCanceller(
        impl: AndroidLegacyNotificationAlarmCanceller,
    ): LegacyNotificationAlarmCanceller
}
