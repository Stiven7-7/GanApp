package com.proyecto.ganapp.di

import com.proyecto.ganapp.domain.repository.AnimalRepository
import com.proyecto.ganapp.domain.repository.UsuarioRepository
import com.proyecto.ganapp.domain.repository.NotificacionRepository
import com.proyecto.ganapp.domain.usecase.animal.GetAnimalsUseCase
import com.proyecto.ganapp.domain.usecase.animal.InsertAnimalUseCase
import com.proyecto.ganapp.domain.usecase.usuario.LoginUseCase
import com.proyecto.ganapp.domain.usecase.usuario.RegisterUserUseCase
import com.proyecto.ganapp.domain.usecase.notificacion.GetNotificationsUseCase
import com.proyecto.ganapp.domain.usecase.notificacion.ScheduleNotificationUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    // 🐮 Animal UseCases
    @Provides
    @Singleton
    fun provideGetAnimalsUseCase(repository: AnimalRepository): GetAnimalsUseCase {
        return GetAnimalsUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideInsertAnimalUseCase(repository: AnimalRepository): InsertAnimalUseCase {
        return InsertAnimalUseCase(repository)
    }

    // 👤 Usuario UseCases
    @Provides
    @Singleton
    fun provideLoginUseCase(repository: UsuarioRepository): LoginUseCase {
        return LoginUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideRegisterUserUseCase(repository: UsuarioRepository): RegisterUserUseCase {
        return RegisterUserUseCase(repository)
    }

    // 🔔 Notificación UseCases
    @Provides
    @Singleton
    fun provideGetNotificationsUseCase(repository: NotificacionRepository): GetNotificationsUseCase {
        return GetNotificationsUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideScheduleNotificationUseCase(): ScheduleNotificationUseCase {
        return ScheduleNotificationUseCase()
    }
}
