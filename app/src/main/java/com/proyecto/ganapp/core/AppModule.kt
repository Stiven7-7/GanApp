package com.proyecto.ganapp.core

import com.proyecto.ganapp.data.local.dao.NotificacionxAnimalDao
import com.proyecto.ganapp.data.repository.impl.NotificacionxAnimalRepositoryImpl
import com.proyecto.ganapp.domain.repository.NotificacionxAnimalRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideNotificacionxAnimalRepository(
        dao: NotificacionxAnimalDao
    ): NotificacionxAnimalRepository {
        return NotificacionxAnimalRepositoryImpl(dao)
    }
}
