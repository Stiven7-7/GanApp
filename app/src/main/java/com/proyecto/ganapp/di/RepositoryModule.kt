package com.proyecto.ganapp.di

import com.proyecto.ganapp.data.repository.impl.AnimalRepositoryImpl
import com.proyecto.ganapp.data.repository.impl.UsuarioRepositoryImpl
import com.proyecto.ganapp.data.repository.impl.NotificacionRepositoryImpl
import com.proyecto.ganapp.domain.repository.AnimalRepository
import com.proyecto.ganapp.domain.repository.UsuarioRepository
import com.proyecto.ganapp.domain.repository.NotificacionRepository
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
    abstract fun bindNotificacionRepository(
        impl: NotificacionRepositoryImpl
    ): NotificacionRepository
}
