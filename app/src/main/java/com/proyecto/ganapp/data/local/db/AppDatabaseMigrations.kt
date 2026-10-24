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

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `animal_new` (
                `idAnimal` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `especie` TEXT NOT NULL,
                `nombre` TEXT NOT NULL,
                `raza` TEXT NOT NULL,
                `edad` INTEGER,
                `peso` REAL,
                `color` TEXT,
                `idUsuario` INTEGER NOT NULL,
                `fotoUri` TEXT,
                FOREIGN KEY(`idUsuario`) REFERENCES `usuario`(`idUsuario`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO `animal_new` (
                `idAnimal`,
                `especie`,
                `nombre`,
                `raza`,
                `edad`,
                `peso`,
                `color`,
                `idUsuario`,
                `fotoUri`
            )
            SELECT
                `idAnimal`,
                'SIN_ESPECIFICAR',
                `nombre`,
                CASE
                    WHEN `raza` IS NULL OR TRIM(`raza`) = '' THEN 'SIN_ESPECIFICAR'
                    ELSE `raza`
                END,
                `edad`,
                `peso`,
                `color`,
                `idUsuario`,
                `fotoUri`
            FROM `animal`
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE `animal`")
        db.execSQL("ALTER TABLE `animal_new` RENAME TO `animal`")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_animal_idUsuario` ON `animal` (`idUsuario`)",
        )
        if (hasSqliteSequence(db)) {
            db.execSQL(
                "UPDATE sqlite_sequence SET name = 'animal' WHERE name = 'animal_new'",
            )
        }
    }

    private fun hasSqliteSequence(db: SupportSQLiteDatabase): Boolean {
        return db.query(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'sqlite_sequence'",
        ).use { cursor -> cursor.moveToFirst() }
    }
}
