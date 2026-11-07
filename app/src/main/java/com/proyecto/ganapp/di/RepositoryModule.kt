package com.proyecto.ganapp.di

import com.proyecto.ganapp.data.repository.impl.AnimalRepositoryImpl
import com.proyecto.ganapp.data.repository.impl.AnimalScheduleDeletionRepositoryImpl
import com.proyecto.ganapp.data.repository.impl.NotificationScheduleRepositoryImpl
import com.proyecto.ganapp.data.repository.impl.NotificationTemplateRepositoryImpl
import com.proyecto.ganapp.data.repository.impl.SessionRepositoryImpl
import com.proyecto.ganapp.data.repository.impl.UsuarioRepositoryImpl
import com.proyecto.ganapp.domain.repository.AnimalRepository
import com.proyecto.ganapp.domain.repository.AnimalScheduleDeletionRepository
import com.proyecto.ganapp.domain.repository.NotificationScheduleRepository
import com.proyecto.ganapp.domain.repository.NotificationTemplateRepository
import com.proyecto.ganapp.domain.repository.SessionRepository
import com.proyecto.ganapp.domain.repository.UsuarioRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAnimalRepository(
        impl: AnimalRepositoryImpl
    ): AnimalRepository

    @Binds
    @Singleton
    abstract fun bindUsuarioRepository(
        impl: UsuarioRepositoryImpl
    ): UsuarioRepository

    @Binds
    @Singleton
    abstract fun bindNotificationTemplateRepository(
        impl: NotificationTemplateRepositoryImpl,
    ): NotificationTemplateRepository

    @Binds
    @Singleton
    abstract fun bindNotificationScheduleRepository(
        impl: NotificationScheduleRepositoryImpl,
    ): NotificationScheduleRepository

    @Binds
    @Singleton
    abstract fun bindAnimalScheduleDeletionRepository(
        impl: AnimalScheduleDeletionRepositoryImpl,
    ): AnimalScheduleDeletionRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(
        impl: SessionRepositoryImpl
    ): SessionRepository
}
