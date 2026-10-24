package com.proyecto.ganapp.di

import com.proyecto.ganapp.core.notifications.AndroidNotificationAlarmScheduler
import com.proyecto.ganapp.domain.service.NotificationAlarmScheduler
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
    abstract fun bindNotificationAlarmScheduler(
        impl: AndroidNotificationAlarmScheduler,
    ): NotificationAlarmScheduler
}
