package com.proyecto.ganapp.data.local.db

import androidx.room.TypeConverter
import com.proyecto.ganapp.domain.model.EspecieAnimal

object EspecieAnimalConverters {
    @TypeConverter
    @JvmStatic
    fun fromEspecieAnimal(especie: EspecieAnimal): String = especie.name

    @TypeConverter
    @JvmStatic
    fun toEspecieAnimal(value: String): EspecieAnimal {
        return try {
            EspecieAnimal.valueOf(value)
        } catch (exception: IllegalArgumentException) {
            throw IllegalArgumentException("Valor de especie desconocido: $value", exception)
        }
    }
}
