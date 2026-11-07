package com.proyecto.ganapp.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.proyecto.ganapp.data.local.dao.AnimalDao
import com.proyecto.ganapp.data.local.dao.PlantillaNotificacionDao
import com.proyecto.ganapp.data.local.dao.ProgramacionNotificacionDao
import com.proyecto.ganapp.data.local.dao.UsuarioDao
import com.proyecto.ganapp.data.local.entity.AnimalEntity
import com.proyecto.ganapp.data.local.entity.PlantillaNotificacionEntity
import com.proyecto.ganapp.data.local.entity.ProgramacionNotificacionEntity
import com.proyecto.ganapp.data.local.entity.UsuarioEntity

@Database(
    entities = [
        UsuarioEntity::class,
        AnimalEntity::class,
        PlantillaNotificacionEntity::class,
        ProgramacionNotificacionEntity::class,
    ],
    version = 11,
    exportSchema = true
)
@TypeConverters(value = [DateConverters::class, EspecieAnimalConverters::class])
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun animalDao(): AnimalDao
    abstract fun plantillaNotificacionDao(): PlantillaNotificacionDao
    abstract fun programacionNotificacionDao(): ProgramacionNotificacionDao

    companion object {
        const val DATABASE_NAME = "ganapp_db"
    }
}
