package com.proyecto.ganapp.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.Locale

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val rows = mutableListOf<Pair<Long, String>>()
        db.query("SELECT idUsuario, correo FROM usuario").use { cursor ->
            while (cursor.moveToNext()) {
                rows += cursor.getLong(0) to cursor.getString(1)
            }
        }

        val normalizedById = rows.map { (id, correo) ->
            id to correo.trim().lowercase(Locale.ROOT)
        }

        val hasCollision = normalizedById
            .groupBy { it.second }
            .any { it.value.size > 1 }

        if (hasCollision) {
            error("Migration 6→7 aborted: duplicate normalized user emails detected")
        }

        for ((id, normalized) in normalizedById) {
            val original = rows.first { it.first == id }.second
            if (original != normalized) {
                db.execSQL(
                    "UPDATE usuario SET correo = ? WHERE idUsuario = ?",
                    arrayOf(normalized, id),
                )
            }
        }

        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_usuario_correo` ON `usuario` (`correo`)",
        )
    }
}
