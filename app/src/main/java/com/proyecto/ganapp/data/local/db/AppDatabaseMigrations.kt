package com.proyecto.ganapp.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.proyecto.ganapp.data.mapper.parseNotificationScheduleTime
import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import com.proyecto.ganapp.domain.model.TipoNotificacion
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

fun epochMillisToEpochDay(epochMillis: Long, zoneId: ZoneId): Long {
    return Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate().toEpochDay()
}

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

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val zoneId = ZoneId.systemDefault()
        val rows = mutableListOf<Triple<Long, Long, Long?>>()
        db.query("SELECT idNotificacion, fechaInicio, fechaFin FROM notificacion").use { cursor ->
            while (cursor.moveToNext()) {
                rows += Triple(
                    cursor.getLong(0),
                    cursor.getLong(1),
                    if (cursor.isNull(2)) null else cursor.getLong(2),
                )
            }
        }
        for ((id, fechaInicioMillis, fechaFinMillis) in rows) {
            val fechaInicio = epochMillisToEpochDay(fechaInicioMillis, zoneId)
            if (fechaFinMillis == null) {
                db.execSQL(
                    "UPDATE notificacion SET fechaInicio = ? WHERE idNotificacion = ?",
                    arrayOf(fechaInicio, id),
                )
            } else {
                db.execSQL(
                    "UPDATE notificacion SET fechaInicio = ?, fechaFin = ? WHERE idNotificacion = ?",
                    arrayOf(fechaInicio, epochMillisToEpochDay(fechaFinMillis, zoneId), id),
                )
            }
        }
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE animal ADD COLUMN esFavorito INTEGER NOT NULL DEFAULT 0",
        )
    }
}

/** Registrada en DatabaseModule. Conserva la lógica aprobada en 4.5A. */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Room ejecutará la migración en su transacción. Cualquier error debe abortarla.
        db.query("PRAGMA foreign_key_check").use { cursor ->
            check(!cursor.moveToFirst()) { "Migration 10→11 aborted: legacy foreign key violation" }
        }
        db.query(
            """
            SELECT 1
            FROM notificacion_animal na
            JOIN notificacion n ON n.idNotificacion = na.idNotificacion
            JOIN animal a ON a.idAnimal = na.idAnimal
            WHERE n.idUsuario != a.idUsuario
            LIMIT 1
            """.trimIndent(),
        ).use { cursor ->
            check(!cursor.moveToFirst()) { "Migration 10→11 aborted: cross-user notification association" }
        }
        db.query("SELECT idNotificacion, tipo, hora, seRepite FROM notificacion").use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                check(TipoNotificacion.fromPersistentString(cursor.getString(1)) != null) {
                    "Migration 10→11 aborted: unknown notification type at ID $id"
                }
                parseNotificationScheduleTime(cursor.getString(2))
                check(RecurrenciaNotificacion.fromPersistentString(cursor.getString(3)) != null) {
                    "Migration 10→11 aborted: unknown recurrence at ID $id"
                }
            }
        }
        val templateCount = db.query("SELECT COUNT(*) FROM notificacion").use { cursor ->
            check(cursor.moveToFirst())
            cursor.getLong(0)
        }
        val scheduleCount = db.query(
            """
            SELECT
                (SELECT COUNT(*) FROM notificacion_animal) +
                (SELECT COUNT(*) FROM notificacion n WHERE NOT EXISTS (
                    SELECT 1 FROM notificacion_animal na WHERE na.idNotificacion = n.idNotificacion
                ))
            """.trimIndent(),
        ).use { cursor ->
            check(cursor.moveToFirst())
            cursor.getLong(0)
        }

        db.execSQL(
            """
            CREATE TABLE `plantilla_notificacion` (
                `idPlantilla` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `titulo` TEXT NOT NULL,
                `tipo` TEXT NOT NULL,
                `idUsuario` INTEGER NOT NULL,
                `legacyDescripcion` TEXT,
                `legacyEstado` TEXT,
                `legacyFechaCreacion` INTEGER,
                `legacyDosisPorDia` INTEGER,
                `legacyIntervaloHoras` INTEGER,
                FOREIGN KEY(`idUsuario`) REFERENCES `usuario`(`idUsuario`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE `programacion_notificacion` (
                `idProgramacion` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `idPlantilla` INTEGER NOT NULL,
                `idAnimal` INTEGER,
                `fechaInicio` INTEGER NOT NULL,
                `fechaFin` INTEGER NOT NULL,
                `hora` TEXT NOT NULL,
                `recurrencia` TEXT NOT NULL,
                FOREIGN KEY(`idPlantilla`) REFERENCES `plantilla_notificacion`(`idPlantilla`)
                    ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(`idAnimal`) REFERENCES `animal`(`idAnimal`)
                    ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX `index_plantilla_notificacion_idUsuario` ON `plantilla_notificacion` (`idUsuario`)")
        db.execSQL("CREATE INDEX `index_programacion_notificacion_idPlantilla` ON `programacion_notificacion` (`idPlantilla`)")
        db.execSQL("CREATE INDEX `index_programacion_notificacion_idAnimal` ON `programacion_notificacion` (`idAnimal`)")

        db.execSQL(
            """
            INSERT INTO plantilla_notificacion (
                idPlantilla, titulo, tipo, idUsuario, legacyDescripcion, legacyEstado,
                legacyFechaCreacion, legacyDosisPorDia, legacyIntervaloHoras
            )
            SELECT idNotificacion, nombre, tipo, idUsuario, descripcion, estado,
                fechaCreacion, dosisPorDia, intervaloHoras
            FROM notificacion
            """.trimIndent(),
        )
        // Se omite idProgramacion: cada relación obtiene su propio ID autogenerado.
        db.execSQL(
            """
            INSERT INTO programacion_notificacion (
                idPlantilla, idAnimal, fechaInicio, fechaFin, hora, recurrencia
            )
            SELECT n.idNotificacion, na.idAnimal, n.fechaInicio, COALESCE(n.fechaFin, n.fechaInicio),
                n.hora, CASE WHEN n.seRepite = 'CADA 15 DIAS' THEN 'CADA 15 DÍAS' ELSE n.seRepite END
            FROM notificacion n
            JOIN notificacion_animal na ON na.idNotificacion = n.idNotificacion
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO programacion_notificacion (
                idPlantilla, idAnimal, fechaInicio, fechaFin, hora, recurrencia
            )
            SELECT n.idNotificacion, NULL, n.fechaInicio, COALESCE(n.fechaFin, n.fechaInicio),
                n.hora, CASE WHEN n.seRepite = 'CADA 15 DIAS' THEN 'CADA 15 DÍAS' ELSE n.seRepite END
            FROM notificacion n
            WHERE NOT EXISTS (
                SELECT 1 FROM notificacion_animal na WHERE na.idNotificacion = n.idNotificacion
            )
            """.trimIndent(),
        )

        db.query("SELECT COUNT(*) FROM plantilla_notificacion").use { cursor ->
            check(cursor.moveToFirst() && cursor.getLong(0) == templateCount) {
                "Migration 10→11 aborted: template count mismatch"
            }
        }
        db.query("SELECT COUNT(*) FROM programacion_notificacion").use { cursor ->
            check(cursor.moveToFirst() && cursor.getLong(0) == scheduleCount) {
                "Migration 10→11 aborted: schedule count mismatch"
            }
        }
        db.query("PRAGMA foreign_key_check").use { cursor ->
            check(!cursor.moveToFirst()) { "Migration 10→11 aborted: migrated foreign key violation" }
        }
        db.execSQL("DROP TABLE notificacion_animal")
        db.execSQL("DROP TABLE notificacion")
    }
}
