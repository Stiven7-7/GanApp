package com.proyecto.ganapp.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.proyecto.ganapp.data.local.dao.AnimalDao
import com.proyecto.ganapp.data.local.dao.NotificacionDao
import com.proyecto.ganapp.data.local.dao.NotificacionxAnimalDao
import com.proyecto.ganapp.data.local.dao.UsuarioDao
import com.proyecto.ganapp.data.local.entity.AnimalEntity
import com.proyecto.ganapp.data.local.entity.NotificacionEntity
import com.proyecto.ganapp.data.local.entity.NotificacionxAnimalEntity
import com.proyecto.ganapp.data.local.entity.UsuarioEntity

@Database(
    entities = [
        UsuarioEntity::class,
        AnimalEntity::class,
        NotificacionEntity::class,
        NotificacionxAnimalEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(value = [DateConverters::class])
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun animalDao(): AnimalDao
    abstract fun notificacionDao(): NotificacionDao
    abstract fun notificacionxAnimalDao(): NotificacionxAnimalDao

    companion object {
        const val DATABASE_NAME = "ganapp_db"
    }
}
