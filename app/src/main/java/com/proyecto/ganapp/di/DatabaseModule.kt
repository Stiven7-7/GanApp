package com.proyecto.ganapp.di

import android.content.Context
import androidx.room.Room
import com.proyecto.ganapp.data.local.db.AppDatabase
import com.proyecto.ganapp.data.local.dao.AnimalDao
import com.proyecto.ganapp.data.local.dao.UsuarioDao
import com.proyecto.ganapp.data.local.dao.NotificacionDao
import com.proyecto.ganapp.data.local.dao.NotificacionxAnimalDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "ganapp_db"
        )
            .fallbackToDestructiveMigration() // elimina la BD si cambia versión y no hay migración
            .build()
    }

    @Provides
    fun provideUsuarioDao(db: AppDatabase): UsuarioDao = db.usuarioDao()

    @Provides
    fun provideAnimalDao(db: AppDatabase): AnimalDao = db.animalDao()

    @Provides
    fun provideNotificacionDao(db: AppDatabase): NotificacionDao = db.notificacionDao()

    @Provides
    fun provideNotificacionxAnimalDao(db: AppDatabase): NotificacionxAnimalDao = db.notificacionxAnimalDao()
}
