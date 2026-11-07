package com.proyecto.ganapp.di

import android.content.Context
import androidx.room.Room
import com.proyecto.ganapp.data.local.db.AppDatabase
import com.proyecto.ganapp.data.local.db.MIGRATION_6_7
import com.proyecto.ganapp.data.local.db.MIGRATION_7_8
import com.proyecto.ganapp.data.local.db.MIGRATION_8_9
import com.proyecto.ganapp.data.local.db.MIGRATION_9_10
import com.proyecto.ganapp.data.local.db.MIGRATION_10_11
import com.proyecto.ganapp.data.local.dao.AnimalDao
import com.proyecto.ganapp.data.local.dao.PlantillaNotificacionDao
import com.proyecto.ganapp.data.local.dao.ProgramacionNotificacionDao
import com.proyecto.ganapp.data.local.dao.UsuarioDao
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
            .addMigrations(
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10,
                MIGRATION_10_11,
            )
            .build()
    }

    @Provides
    fun provideUsuarioDao(db: AppDatabase): UsuarioDao = db.usuarioDao()

    @Provides
    fun provideAnimalDao(db: AppDatabase): AnimalDao = db.animalDao()

    @Provides
    fun providePlantillaNotificacionDao(db: AppDatabase): PlantillaNotificacionDao =
        db.plantillaNotificacionDao()

    @Provides
    fun provideProgramacionNotificacionDao(db: AppDatabase): ProgramacionNotificacionDao =
        db.programacionNotificacionDao()
}
