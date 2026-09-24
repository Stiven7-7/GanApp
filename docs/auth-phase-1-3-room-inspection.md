# Inspección previa de la Subfase 1.3

Fecha de inspección: 2026-09-24.
Alcance: persistencia de `usuario`, unicidad de correo, migración Room 6→7 y traducción de `DuplicateEmail`.
Naturaleza: solo lectura, salvo este documento.

Leyenda de cada hallazgo:

- **Estado comprobado:** hecho observado en el repositorio o en comandos.
- **Evidencia:** ruta, comando o fragmento que lo demuestra.
- **Riesgo:** consecuencia si se implementa 1.3 sin atender el hecho.
- **Recomendación:** propuesta no implementada.
- **Decisión pendiente:** requiere aprobación antes de codificar.

---

## 1. Estado Git

**Estado comprobado**

- Rama activa: `feature/auth-session-foundation`.
- Working tree inicial: limpio.
- HEAD local: `df82fcda9d60c3077d20a0fdbfe9e9e9b7d2b1e2`.
- `origin/feature/auth-session-foundation`: `df82fcda9d60c3077d20a0fdbfe9e9e9b7d2b1e2`.
- Ahead/behind: `0 0`.
- Tracking: `up to date with 'origin/feature/auth-session-foundation'`.
- Remoto: `https://github.com/Stiven7-7/GanApp.git`.
- Último commit: `df82fcd fix: clear stale authentication errors on screen exit`.

**Evidencia:** `git branch --show-current`, `git status --short --untracked-files=all`, `git status --branch`, `git rev-parse HEAD`, `git rev-parse origin/feature/auth-session-foundation`, `git rev-list --left-right --count origin/feature/auth-session-foundation...HEAD`, `git remote -v`.

---

## 2. Build y tests base

**Estado comprobado**

El primer intento de `.\gradlew.bat test --no-daemon` falló porque `JAVA_HOME` no estaba definido en esta sesión. No se modificó el repositorio. Se reejecutó con el JBR de Android Studio (`C:\Program Files\Android\Android Studio\jbr`, OpenJDK 21.0.9) solo en el entorno del comando.

| Comando | Resultado | Duración Gradle | Notas |
|---|---|---|---|
| `.\gradlew.bat test --no-daemon` | SUCCESS | 14 s | 69 tareas UP-TO-DATE |
| `.\gradlew.bat assembleDebug --no-daemon` | SUCCESS | 1 m 10 s | Compiló debug |

Pruebas unitarias por variante (XML en `app/build/test-results`):

| Suite | Tests | Failures |
|---|---|---|
| `RegisterUserUseCaseTest` | 24 | 0 |
| `LoginUseCaseTest` | 27 | 0 |
| `ExampleUnitTest` | 1 | 0 |
| **Total por variante** | **52** | **0** |

No hay tests instrumentados ejecutados en este baseline (solo existe `ExampleInstrumentedTest`).

Gradle no generó cambios versionados. Tras ambos comandos: working tree limpio.

**Evidencia:** terminal Gradle; `app/build/test-results/testDebugUnitTest/*.xml`; `git status --short` posterior.

**Riesgo:** en máquinas nuevas el build falla si no hay `JAVA_HOME`. No es un defecto de 1.3.

---

## 3. UsuarioEntity actual

**Estado comprobado**

| Campo | Valor |
|---|---|
| Ruta | `app/src/main/java/com/proyecto/ganapp/data/local/entity/UsuarioEntity.kt` |
| Package | `com.proyecto.ganapp.data.local.entity` |
| Anotación | `@Entity(tableName = "usuario")` sin `indices`, sin `foreignKeys`, sin `unique` |
| Tabla | `usuario` |
| PK | `idUsuario` (`Long`), `@PrimaryKey(autoGenerate = true)`, default `0` |
| Campos | `idUsuario`, `nombre`, `apellido`, `correo`, `contrasena`, `fechaRegistro`, `activo` |

Columnas:

| Propiedad Kotlin | Columna SQLite | Tipo Kotlin | Afinidad schema 6 | Null | Default en entity |
|---|---|---|---|---|---|
| `idUsuario` | `idUsuario` | `Long` | INTEGER NOT NULL | no | `0` |
| `nombre` | `nombre` | `String` | TEXT NOT NULL | no | no |
| `apellido` | `apellido` | `String` | TEXT NOT NULL | no | no |
| `correo` | `correo` | `String` | TEXT NOT NULL | no | no |
| `contrasena` | `contrasena` (`@ColumnInfo(name = "contrasena")`) | `String` | TEXT NOT NULL | no | no |
| `fechaRegistro` | `fechaRegistro` | `String` | TEXT NOT NULL | no | `DateUtils.currentDate()` |
| `activo` | `activo` | `Boolean` | INTEGER NOT NULL | no | `true` |

- Índices actuales sobre `usuario`: ninguno.
- Foreign keys sobre `usuario`: ninguna.
- UNIQUE sobre `correo`: no existe.
- Índice simple sobre `correo`: no existe.
- Correo no admite null en entity ni en schema 6.

Relación con dominio:

- `Usuario` (`domain/model/Usuario.kt`) solo tiene `idUsuario`, `nombre`, `apellido`, `correo`, `contrasena`.
- No incluye `fechaRegistro` ni `activo`.
- Mapper `UsuarioEntity.toDomain()` copia el valor de `contrasena` al modelo (en persistencia real es el hash BCrypt).
- Mapper `Usuario.toEntity()` copia campos 1:1 y deja defaults de entity para `fechaRegistro` y `activo`.
- El mapper **no** normaliza correo. La normalización depende del llamador (`RegisterUserUseCase` / `LoginUseCase`).

**Evidencia:** `UsuarioEntity.kt`, `Usuario.kt`, `UsuarioMapper.kt`, schema `6.json`.

---

## 4. UsuarioDao actual

**Estado comprobado**

Ruta: `app/src/main/java/com/proyecto/ganapp/data/local/dao/UsuarioDao.kt`.

| Método | Anotación / SQL | Retorno |
|---|---|---|
| `insert(usuario)` | `@Insert(onConflict = OnConflictStrategy.REPLACE)` | `Long` |
| `update(usuario)` | `@Update` | `Unit` |
| `delete(usuario)` | `@Delete` | `Unit` |
| `getById(id)` | `SELECT * FROM usuario WHERE idUsuario = :id LIMIT 1` | `UsuarioEntity?` |
| `getAll()` | `SELECT * FROM usuario` | `List<UsuarioEntity>` |
| `getByCorreo(correo)` | `SELECT * FROM usuario WHERE correo = :correo LIMIT 1` | `UsuarioEntity?` |

- No hay `COLLATE NOCASE`, `trim`, `lower` ni equivalentes.
- Comparación de correo: igualdad exacta SQLite (sensible a mayúsculas y espacios).
- Si existieran varias filas con el mismo correo, `LIMIT 1` devolvería una fila no determinista.
- `Flow` está importado y no se usa.

Consumidores del DAO (main):

- `UsuarioRepositoryImpl.register` → `insert`.
- `UsuarioRepositoryImpl.login` → `getByCorreo`.
- `UsuarioRepositoryImpl.getUserById` → `getById`.
- `UsuarioRepositoryImpl.getUsuarioByCorreo` → `getByCorreo`.
- `update`, `delete` y `getAll` no tienen consumidores en main.

Login presupone unicidad funcional: toma la primera fila coincidente. Register depende únicamente del `Long` de `insert`; no preconsulta existencia.

**Evidencia:** `UsuarioDao.kt`; grep de `dao.` / `UsuarioDao` en `app/src/main`.

---

## 5. UsuarioRepository actual

**Estado comprobado**

Interfaz: `app/src/main/java/com/proyecto/ganapp/domain/repository/UsuarioRepository.kt`.

```
suspend fun register(usuario: Usuario): Long
suspend fun login(correo: String, contrasena: String): Usuario?
suspend fun getUserById(id: Long): Usuario?
suspend fun getUsuarioByCorreo(correo: String): Usuario?
```

Implementación: `UsuarioRepositoryImpl.kt`. Binding Hilt: `RepositoryModule.bindUsuarioRepository`.

| Método | Comportamiento comprobado | Consumidores main |
|---|---|---|
| `register` | Hashea con `PasswordUtils.hash` (coste por defecto **12**), construye `UsuarioEntity` **sin** `idUsuario` (autoGenerate), asigna `fechaRegistro` y `activo = true`, retorna `dao.insert` | `RegisterUserUseCase` |
| `login` | `getByCorreo` exacto; `PasswordUtils.verify`; si ok, `toDomain()` | `LoginUseCase` |
| `getUserById` | `getById` + `toDomain` | `GetUsuarioByIdUseCase` → `HomeViewModel` |
| `getUsuarioByCorreo` | `getByCorreo` + `toDomain` | ningún use case actual |

Normalización en repositorio: **ninguna**.

BCrypt:

- Generación: `UsuarioRepositoryImpl.register` vía `PasswordUtils.hash`.
- Verificación: `UsuarioRepositoryImpl.login` vía `PasswordUtils.verify`.
- Coste visible: `12` en `PasswordUtils.hash` (default del parámetro).

ID de insert: se retorna tal cual. Room con `autoGenerate` e `idUsuario = 0` genera un ID nuevo. Con `REPLACE` y conflicto de PK se reutilizaría el ID; hoy no hay UNIQUE de correo, así que un segundo registro con el mismo correo crea **otra fila** y un ID nuevo.

Excepciones: el repositorio no captura ninguna. Las excepciones de Room/SQLite se propagan.

`REPLACE` hoy no sustituye por correo (no hay unique). Si se añadiera UNIQUE en `correo` y se mantuviera `REPLACE`, Room podría borrar la fila previa y reinsertar, disparando `ON DELETE CASCADE` en `animal` y `notificacion`.

Home depende de `getUserById` y del ID de navegación. LoginUseCase/RegisterUserUseCase dependen de `login`/`register` actuales.

**Evidencia:** `UsuarioRepository.kt`, `UsuarioRepositoryImpl.kt`, `PasswordUtils.kt`, `RepositoryModule.kt`, `HomeViewModel.kt`.

---

## 6. Registro actual de usuarios

**Estado comprobado**

Flujo:

1. UI (`RegisterScreen`) envía campos separados; confirmación temporal = contraseña.
2. `AuthViewModel.register` llama `RegisterUserUseCase`.
3. Use case normaliza nombre/apellido (`trim`) y correo (`trim` + `lowercase(Locale.ROOT)`).
4. Valida y acumula errores; si hay errores no llama al repositorio.
5. Construye `Usuario(idUsuario = 0, …)` con correo ya normalizado y contraseña sin transformar.
6. `repository.register` hashea e inserta con `REPLACE`.
7. Si `userId > 0` → `Success`; si `<= 0` o excepción → `UnexpectedError`.

No existe `DuplicateEmail`. Un segundo registro con el mismo correo normalizado **se inserta** como otro usuario.

**Riesgo:** la base puede acumular filas con el mismo correo. Login tomará una sola fila (`LIMIT 1`). Home puede recibir un ID distinto al usuario “esperado”.

---

## 7. Login y consulta por correo

**Estado comprobado**

- `LoginUseCase` normaliza el correo igual que el registro (`trim` + `lowercase(Locale.ROOT)`) y lo envía a `repository.login`.
- El DAO compara `correo = :correo` sin `COLLATE NOCASE`.
- Usuarios insertados **después** de 1.2 quedan en minúsculas.
- Usuarios insertados **antes** de 1.2 pueden tener mayúsculas o espacios si la UI los guardó así. Esos logins fallarían tras la normalización del use case.

**Riesgo:** unicidad UNIQUE sobre el valor crudo no equivale a unicidad del correo normalizado. Migrar sin `trim`/`lower` de filas existentes dejaría duplicados lógicos (`Ana@X.com` vs `ana@x.com`) y logins rotos.

---

## 8. AppDatabase versión 6

**Estado comprobado**

| Campo | Valor |
|---|---|
| Ruta | `app/src/main/java/com/proyecto/ganapp/data/local/db/AppDatabase.kt` |
| Clase | `AppDatabase` |
| Versión | **6** |
| `exportSchema` | `true` |
| Entities | `UsuarioEntity`, `AnimalEntity`, `NotificacionEntity`, `NotificacionxAnimalEntity` |
| TypeConverters | `DateConverters` |
| DAOs | `usuarioDao`, `animalDao`, `notificacionDao`, `notificacionxAnimalDao` |
| Companion | `DATABASE_NAME = "ganapp_db"` |
| Migraciones declaradas en la clase | **ninguna** |
| Callbacks / prepopulate / seeds | **no** |
| Destructive desde la clase | no; está en `DatabaseModule` |

**Evidencia:** `AppDatabase.kt`.

---

## 9. DatabaseModule

**Estado comprobado**

Ruta: `app/src/main/java/com/proyecto/ganapp/di/DatabaseModule.kt`.

- Builder: `Room.databaseBuilder(context, AppDatabase::class.java, "ganapp_db")`.
- Context: `@ApplicationContext`.
- Scope de la base: `@Singleton`.
- `addMigrations`: **no se llama**.
- Migraciones registradas: **ninguna**.
- `fallbackToDestructiveMigration()`: **sí**, con comentario de que elimina la BD si cambia la versión y no hay migración.
- `fallbackToDestructiveMigrationFrom`: no existe.
- `UsuarioDao` se provee con `db.usuarioDao()` (sin `@Singleton` en el DAO).

**Riesgo si `MIGRATION_6_7` se define pero no se registra:** Room no la aplicará. Con fallback destructivo activo, al abrir versión 7 **se borra toda la base**.

**Riesgo de conservar fallback destructivo:** cualquier incremento de versión sin migración registrada destruye usuarios, animales y notificaciones.

**Evidencia:** `DatabaseModule.kt` líneas 23–31.

---

## 10. Migraciones existentes

**Estado comprobado**

No existen clases `Migration`, constantes `MIGRATION_*` ni `addMigrations` en Kotlin.

Búsqueda en `**/*.{kt,kts,java}`: único match `fallbackToDestructiveMigration` en `DatabaseModule`.

Schemas JSON 1–6 sí existen. Eso documenta el **resultado** de versiones, no migraciones SQL versionadas.

Historial inferido por schemas (no ejecutado):

| Versión | identityHash | Tabla `usuario` | Cambio notable vs previa |
|---|---|---|---|
| 1 | `985e6be455e218f363d9288409d294f4` | idéntica a 6 | animal: orden `fotoUri` antes de `idUsuario`; notificación sin `dosisPorDia`/`intervaloHoras`, `hora` nullable |
| 2–6 | `a7b985e44c105ffe85d81bfb0bfed8f1` | idéntica | mismo hash: schema de entidades estable desde v2 |

Saltos: no hay archivos 7+. Versiones 2–6 comparten identity hash; los incrementos 2→6 **no cambiaron** el schema de `usuario`.

Estrategia histórica real: **destrucción** vía fallback, no `ALTER`/`CREATE`/copia.

Pruebas de migración: **ninguna**.

**Recomendación:** tratar 6→7 como la primera migración SQL real del proyecto. No asumir que usuarios de v1–5 sobrevivieron (el fallback los habría recreado).

---

## 11. Schema Room 6

**Estado comprobado**

- Directorio: `app/schemas/com.proyecto.ganapp.data.local.db.AppDatabase/`.
- Archivos: `1.json` … `6.json`. No falta ninguna de 1 a 6. No existe `7.json`.

`usuario` en schema 6:

- Tabla: `usuario`.
- Columnas: `idUsuario`, `nombre`, `apellido`, `correo`, `contrasena`, `fechaRegistro`, `activo`.
- Tipos: INTEGER / TEXT / INTEGER (`activo`).
- Nullability: todas `notNull: true`.
- PK: `idUsuario` AUTOINCREMENT.
- Foreign keys: `[]`.
- Índices: `[]`.
- identityHash BD: `a7b985e44c105ffe85d81bfb0bfed8f1`.
- SetupQueries: `room_master_table` + insert del identity hash.

Coincide con `UsuarioEntity` actual.

**Hecho de proceso (no propuesta ejecutada):**

- Los JSON de schema **no deben editarse a mano**.
- El schema 7 lo debe emitir Room al **compilar** después de subir `version` y cambiar entities/índices.
- Tarea que lo genera: `kapt` durante `compileDebugKotlin` / `assembleDebug` (argumento `room.schemaLocation` = `$projectDir/schemas` en `app/build.gradle.kts`).
- Ruta esperada: `app/schemas/com.proyecto.ganapp.data.local.db.AppDatabase/7.json`.

---

## 12. Índices actuales

**Estado comprobado**

| Tabla | Índices |
|---|---|
| `usuario` | ninguno |
| `animal` | `index_animal_idUsuario` (no unique) |
| `notificacion` | `index_notificacion_idUsuario` (no unique) |
| `notificacion_animal` | `index_notificacion_animal_idAnimal`, `index_notificacion_animal_idNotificacion` (no unique) |

No hay UNIQUE en correo.

---

## 13. Configuración de fallback destructivo

**Estado comprobado**

`fallbackToDestructiveMigration()` está activo y **no** hay migraciones registradas.

Cualquier `version = 7` sin `addMigrations(MIGRATION_6_7)` y con este fallback **borra** `ganapp_db`.

**Recomendación:** en el bloque de migración, registrar `MIGRATION_6_7` **y** retirar el fallback destructivo (o restringirlo de forma explícita y aprobada). Conservar ambos es el mayor riesgo operativo de 1.3.

**Decisión pendiente:** si se elimina el fallback en el mismo commit que la migración o en un commit previo de endurecimiento.

---

## 14. Datos duplicados exactos

**Estado comprobado**

No hay emulador ni dispositivo (`adb devices` vacío). No se ejecutó ningún `SELECT` contra una base real. No se inventan conteos.

Consulta de solo lectura propuesta (no ejecutada):

```sql
-- A. Duplicados exactos (sin contraseña)
SELECT correo, COUNT(*) AS n, GROUP_CONCAT(idUsuario) AS ids
FROM usuario
GROUP BY correo
HAVING n > 1;
```

**Limitación:** cantidad de duplicados exactos = no determinada.

---

## 15. Duplicados tras normalización

Consultas propuestas (no ejecutadas):

```sql
-- B. Ignorando mayúsculas
SELECT lower(correo) AS k, COUNT(*) AS n, GROUP_CONCAT(idUsuario) AS ids
FROM usuario
GROUP BY k
HAVING n > 1;

-- C. Tras trim
SELECT trim(correo) AS k, COUNT(*) AS n, GROUP_CONCAT(idUsuario) AS ids
FROM usuario
GROUP BY k
HAVING n > 1;

-- D. trim + lowercase (alineado con RegisterUserUseCase / LoginUseCase)
SELECT lower(trim(correo)) AS k, COUNT(*) AS n, GROUP_CONCAT(idUsuario) AS ids
FROM usuario
GROUP BY k
HAVING n > 1;
```

**Limitación:** grupos duplicados normalizados = no determinados. **No** se puede afirmar hoy si un UNIQUE sobre `lower(trim(correo))` se podría crear de inmediato.

---

## 16. Correos no normalizados

Consultas propuestas (no ejecutadas):

```sql
-- E. Vacíos
SELECT COUNT(*) FROM usuario WHERE trim(correo) = '';

-- F. Null (schema no lo permite; verificación defensiva)
SELECT COUNT(*) FROM usuario WHERE correo IS NULL;

-- G. Espacios exteriores
SELECT COUNT(*) FROM usuario WHERE correo != trim(correo);

-- H. Cambian tras lower
SELECT COUNT(*) FROM usuario WHERE correo != lower(correo);

-- I. Total
SELECT COUNT(*) FROM usuario;
```

**Limitación:** totales y no normalizados = no determinados.

Si se reabre un AVD con datos de la Subfase 1.2, esas consultas bastan. No mostrar `contrasena`, nombres ni correos en claro; solo IDs y conteos.

---

## 17. Relaciones con animales y notificaciones

**Estado comprobado**

`AnimalEntity` (`animal`):

- FK `idUsuario` → `usuario.idUsuario`.
- `onDelete = CASCADE`, `onUpdate` implícito NO ACTION (schema: `ON UPDATE NO ACTION ON DELETE CASCADE`).
- Índice `index_animal_idUsuario`.

`NotificacionEntity` (`notificacion`):

- FK `idUsuario` → `usuario.idUsuario`.
- `onDelete = CASCADE`.
- Índice `index_notificacion_idUsuario`.

`NotificacionxAnimalEntity` (`notificacion_animal`):

- FK a `animal` y `notificacion`, ambas CASCADE.
- No referencia `idUsuario` de forma directa. Dependencia **transitiva**: borrar usuario → CASCADE animal/notificación → CASCADE cruce.

Room tiene `foreignKeys` activas (no solo lógicas).

Home, listados de animales y notificaciones navegan por `idUsuario` / `userId` en rutas Compose. No hay otras tablas con `idUsuario` en schema 6.

**Evidencia:** entities, schema 6, `NavGraph.kt`.

---

## 18. Riesgos de deduplicación

**Estado comprobado (campos reales)**

Disponibles en `usuario`:

- `idUsuario` (autoincrement: proxy de antigüedad).
- `fechaRegistro` (`dd-MM-yyyy`, no timestamp; empates el mismo día).
- `activo` (default `true`; no está en dominio ni se usa en registro/login).

No existen: fecha/hora de creación precisa, “usuario más reciente” fiable el mismo día, ni conteo persistido de relaciones (habría que calcularlo con JOIN).

**Riesgo**

- Borrar un usuario duplicado con CASCADE elimina sus animales, notificaciones y cruces.
- Reasignar FKs (`UPDATE animal SET idUsuario = :kept`) sería necesario **antes** de borrar si se quiere conservar dependencias.
- `REPLACE` + UNIQUE tiene el mismo efecto destructivo que borrar.
- `fechaRegistro` no ordena intra-día.
- `activo` no es criterio usable hoy (todos `true` al insertar).

**Recomendación académica (no ejecutada):**

1. Preferir **no borrar** si el diagnóstico (cuando exista emulador) muestra 0 duplicados normalizados: solo `UPDATE` de normalización + `CREATE UNIQUE INDEX`.
2. Si hay duplicados: conservar el `idUsuario` **menor** (más antiguo) **salvo** que otro ID tenga más filas hijas; reasignar FKs al conservado; luego borrar el resto.
3. Documentar la política en el commit de migración.

**Decisión pendiente:** criterio exacto de conservación y si se permite pérdida de filas huérfanas (no debería).

---

## 19. Alternativas para migración 6→7

Todas son **propuestas no ejecutadas**.

### A. Fallar si hay duplicados

- Preservación: máxima si aborta.
- Integridad: alta.
- Complejidad: baja (`SELECT` + abort).
- Pérdida: ninguna si falla; la app no abre en 7.
- Reversibilidad: sí (queda en 6).
- Pruebas: fáciles.
- Adecuación: buena como **guard** inicial; mala como única estrategia si hay datos de 1.2.

### B. Normalizar y conservar una fila por correo

- Preservación: alta si se reasignan FKs.
- Complejidad: media-alta.
- Pérdida: solo si se elige mal el ID.
- Adecuación: correcta si el diagnóstico muestra duplicados.

### C. Normalizar y renombrar temporalmente duplicados

- Preservación de filas: alta.
- Integridad de negocio: baja (correos artificiales).
- Adecuación académica: débil (ensucia datos).

### D. Tabla nueva, copiar válidos, reemplazar

- Preservación: controlada.
- Complejidad: alta (FKs de `animal`/`notificacion` apuntan a `usuario`).
- Requiere recrear FKs e índices hijos.
- Adecuación: excesiva si el cambio es solo UNIQUE + normalización.

---

## 20. Estrategia recomendada

**Recomendación (no implementada)**

Estrategia híbrida **A + normalización in-place + UNIQUE**, con **B solo si el diagnóstico previo encuentra duplicados**:

1. Quitar `fallbackToDestructiveMigration()` al registrar la migración.
2. `UPDATE usuario SET correo = lower(trim(correo));`
3. Detectar grupos `GROUP BY correo HAVING COUNT(*) > 1`.
   - Si 0: `CREATE UNIQUE INDEX index_usuario_correo ON usuario(correo)`.
   - Si > 0: política B (conservar un ID, reasignar FKs, borrar el resto) **o** abortar la migración (A) según aprobación.
4. Subir `AppDatabase.version` a 7 y dejar que Room genere `7.json`.

SQL ilustrativo (**no ejecutado**):

```sql
-- PROPUESTA NO EJECUTADA
UPDATE usuario SET correo = lower(trim(correo));
CREATE UNIQUE INDEX IF NOT EXISTS `index_usuario_correo` ON `usuario` (`correo`);
```

El índice Room equivalente sería `@Entity(indices = [Index(value = ["correo"], unique = true)])`.

**Riesgo principal:** fallback destructivo + UNIQUE mal aplicado + CASCADE.

---

## 21. Estrategia de inserción recomendada

| Opción | Duplicado | Reemplazo silencioso | ID | DuplicateEmail | Pruebas |
|---|---|---|---|---|---|
| A. Mantener `REPLACE` | borra fila previa + CASCADE | **sí, grave** | nuevo o reutilizado | difícil de distinguir | peligrosa |
| B. `ABORT` | lanza constraint | no | no inserta | sí, vía excepción | clara |
| C. `IGNORE` | no inserta | no | suele `-1` | se confunde con UnexpectedError (`<= 0`) | ambigua |
| D. `ABORT` + capturar constraint | igual que B | no | no inserta | sí | la mejor |

**Recomendación:** **D**. Evita reemplazo silencioso, conserva el ID original del usuario existente y permite mapear duplicado ≠ fallo técnico.

No cambiar el DAO en esta inspección.

---

## 22. Mapeo de DuplicateEmail

**Estado comprobado**

`RegisterUserResult` hoy: `Success` / `ValidationError` / `UnexpectedError`.
`RegisterUserUseCase` captura `Exception` y la convierte en `UnexpectedError`.
`AuthRegisterState` no tiene rama de duplicado. `RegisterScreen` trata solo validación e inesperado.
El fake de registro puede lanzar `registerException`.
El test `excepcionDelRepositorio_produceUnexpectedError` espera `UnexpectedError` para cualquier excepción.

**Recomendación**

- `DuplicateEmail` debe ser un **resultado adicional** de `RegisterUserResult`, no un `RegisterValidationError` (no es fallo de formato de entrada; es conflicto de persistencia).
- No debe ser solo “error técnico especializado” opaco: la UI necesita un mensaje específico.
- El repositorio **no** debería devolver `Usuario` ni hash. Puede:
  - lanzar una excepción de dominio JVM (`DuplicateEmailException`) tras capturar `SQLiteConstraintException`, **o**
  - devolver un resultado tipado de persistencia (`Long` vs duplicado).
- Un precheck `getUsuarioByCorreo` **solo** no basta: condición de carrera (menor en Room local, pero la UNIQUE es la garantía).
- Combinación recomendada: UNIQUE real + traducción de constraint (+ precheck opcional para UX).

Impacto UI (posponible al bloque de integración):

- `AuthRegisterState.DuplicateEmail`.
- Rama en `AuthViewModel.when`.
- Mensaje temporal en `RegisterScreen`.

Tests a adaptar/añadir (cuando se implemente):

- Fake: configurar “duplicado” distinto de excepción genérica.
- Nuevo test: correo ya existente → `DuplicateEmail`, repositorio invocado.
- Conservar el test de excepción genérica → `UnexpectedError`.

**Decisión pendiente:** excepción de dominio vs resultado tipado de repositorio (cambiar `register(): Long` rompe fakes e interfaz).

---

## 23. Excepciones Room disponibles

**Estado comprobado**

- Room catalogado: `androidx.room:room-runtime` / `room-ktx` / `room-compiler` **2.6.1**.
- No hay imports actuales de `SQLiteConstraintException` en el proyecto.
- En dispositivo/emulador, `OnConflictStrategy.ABORT` produce `android.database.sqlite.SQLiteConstraintException` (API Android). Esa clase **no** está disponible en unit tests JVM puros (sin Robolectric).
- `androidx.sqlite.SQLiteException` no está referenciada ni hay dependencia explícita `androidx.sqlite` más allá de la transitiva de Room.

**Recomendación:** no capturar `SQLiteConstraintException` dentro de `RegisterUserUseCase` (acoplaría dominio a Android y rompería tests JVM). Traducir en `UsuarioRepositoryImpl` (capa data) a un tipo de dominio.

Cadena propuesta (no implementada):

```
SQLiteConstraintException (data)
→ DuplicateEmailException o resultado de repositorio (domain)
→ RegisterUserResult.DuplicateEmail
→ AuthRegisterState.DuplicateEmail
→ mensaje temporal en RegisterScreen
```

---

## 24. Pruebas de migración disponibles

**Estado comprobado**

| Capacidad | ¿Disponible hoy? |
|---|---|
| Crear base v6 en instrumentación | no hay helper ni test Room |
| Usar schema 6 con `MigrationTestHelper` | schema 6 existe; **falta** `androidx.room:room-testing` |
| Migrar a 7 y verificar | no, hasta implementar migración + helper |
| Tests DAO / repositorio Room | no existen |
| Tests instrumentados reales | solo `ExampleInstrumentedTest` (package) |
| Tests JVM de use case | sí (fakes, `runBlocking`, JUnit) |

Se puede verificar `DuplicateEmail` en JVM con fake. No se puede verificar índice UNIQUE ni conservación de filas sin instrumentación o dependencia nueva.

---

## 25. Dependencias de testing actuales

**Estado comprobado** (`app/build.gradle.kts` + `libs.versions.toml`)

| Dependencia | Configuración | ¿Presente? |
|---|---|---|
| JUnit 4.13.2 | `testImplementation` | sí |
| AndroidX JUnit | `androidTestImplementation` | sí |
| Espresso core | `androidTestImplementation` | sí |
| `AndroidJUnitRunner` | `testInstrumentationRunner` | sí |
| `androidx.room:room-testing` | — | **no** |
| `MigrationTestHelper` | — | **no** |
| Hilt testing | — | **no** |
| `coroutines-test` | — | **no** |
| MockK / Mockito / Turbine / Truth | — | **no** |
| Robolectric | — | **no** |

**Dependencias nuevas que harían falta para pruebas de migración reales:** `androidx.room:room-testing` (y, en la práctica, ejecutar en `androidTest`). Eso **no está autorizado** en esta inspección.

Sin nuevas deps: tests JVM de contrato `DuplicateEmail`; validación de migración en emulador de forma manual.

---

## 26. Archivos que se modificarían

Delta mínimo **propuesto**, no aplicado.

**Probable modificación**

- `UsuarioEntity.kt` (índice unique `correo`).
- `UsuarioDao.kt` (`OnConflictStrategy.ABORT`).
- `UsuarioRepositoryImpl.kt` (traducción de constraint).
- `UsuarioRepository.kt` solo si se cambia el retorno de `register`.
- `RegisterUserResult.kt` (`DuplicateEmail`).
- `RegisterUserUseCase.kt` (mapeo del duplicado; no tragárselo como `UnexpectedError`).
- `AppDatabase.kt` (`version = 7`).
- `DatabaseModule.kt` (`addMigrations` + quitar fallback destructivo).
- Nueva clase `MIGRATION_6_7` (ubicación a definir, p. ej. `data/local/db`).
- Schema 7 generado por Room (no a mano).
- `RegisterUserUseCaseTest.kt` + `FakeUsuarioRepository.kt`.
- Más adelante: `AuthRegisterState.kt`, `AuthViewModel.kt`, `RegisterScreen.kt`.

**Archivos nuevos posibles**

- `DuplicateEmailException.kt` (o resultado de persistencia).
- `AppDatabaseMigrations.kt` / `MIGRATION_6_7`.
- Tests androidTest de migración **si** se aprueba `room-testing`.

**Solo inspección (no tocar en 1.3 salvo necesidad demostrada)**

- `UsuarioMapper.kt`, `PasswordUtils.kt`, `LoginUseCase.kt`, Home, animales, notificaciones, NavGraph, Gradle (salvo dep de test aprobada).

**Hilt:** `UseCaseModule` / `RepositoryModule` no requieren cambio si se conservan constructores. `DatabaseModule` sí.

---

## 27. Archivos que no deben tocarse

Durante la implementación futura de 1.3, salvo bloque de UI expresamente aprobado:

- Schemas 1–6 (no editar a mano).
- `LoginUseCase` / contratos de login ya cerrados (salvo bug de consulta por correo no normalizado en datos viejos, que se resuelve normalizando en migración).
- UI de Home, animales, notificaciones, navegación raíz.
- DataStore / sesión (fuera de 1.3).
- Gradle/Hilt salvo `DatabaseModule` y, si se aprueba, `room-testing`.
- `PasswordUtils` y coste 12.

---

## 28. División propuesta de la Subfase 1.3

Basada en dependencias reales (no hay migraciones ni room-testing).

| Bloque | Contenido | Depende de | Riesgo | Entorno |
|---|---|---|---|---|
| **1** | Contrato `DuplicateEmail` + política de repositorio/excepción de dominio + tests JVM del use case (fake) | nada | bajo | JVM |
| **2** | `Index(unique)` en entity + `ABORT` en DAO + traducción en `UsuarioRepositoryImpl` | 1 (para saber qué lanzar/devolver) | medio | compile |
| **3** | `MIGRATION_6_7` + `version = 7` + registrar migración + **quitar fallback** + schema 7 generado | 2 (el schema 7 debe incluir el unique) | **alto** | compile + emulador |
| **4** | Pruebas de unicidad JVM; migración instrumentada **solo si** se aprueba `room-testing` | 1–3 | medio | JVM / androidTest |
| **5** | `AuthRegisterState` / ViewModel / mensaje temporal en Registro | 1 | bajo | UI |
| **6** | Validación manual + diagnóstico SQL de solo lectura + cierre Git | 3 y 5 | operativo | emulador |

Orden: 1 → 2 → 3 → 4 → 5 → 6.

Commits separados recomendados (un commit por bloque).

Bloque de mayor riesgo: **3** (pérdida de datos por fallback o CASCADE).

Bloque que requiere emulador: **3** (verificación manual), **4** (si hay instrumentación), **6**.

---

## 29. Primer bloque recomendado

**Bloque 1 — Contrato de `DuplicateEmail` y política de repositorio.**

Motivo: no toca Room ni datos; deja el mapeo testeable en JVM; bloquea la decisión “resultado vs excepción de dominio” antes de cambiar el DAO.

No implementar UNIQUE ni migración en ese bloque.

---

## 30. Decisiones pendientes

1. ¿`DuplicateEmail` como `data object` en `RegisterUserResult` (recomendado) u otra forma?
2. ¿Se cambia `UsuarioRepository.register` a un resultado tipado, o se lanza excepción de dominio y se mantiene `Long`?
3. ¿Se elimina `fallbackToDestructiveMigration()` en el mismo commit que `MIGRATION_6_7`?
4. Política si el emulador muestra duplicados tras `lower(trim(correo))`: abortar vs conservar ID menor vs conservar el de más relaciones.
5. ¿Se aprueba añadir `androidx.room:room-testing` para `MigrationTestHelper`?
6. ¿El bloque 5 (UI) entra en 1.3 o se pospone a un ajuste posterior?
7. ¿El UNIQUE es sobre `correo` ya normalizado (tras UPDATE) o se intenta índice funcional? Room 2.6.1 indexa la columna tal cual; la normalización debe ocurrir **antes** del UNIQUE.

---

## Anexo — Archivos inspeccionados

- `UsuarioEntity.kt`, `UsuarioDao.kt`, `UsuarioMapper.kt`, `Usuario.kt`
- `UsuarioRepository.kt`, `UsuarioRepositoryImpl.kt`, `PasswordUtils.kt`, `DateUtils.kt`
- `AppDatabase.kt`, `DatabaseModule.kt`, `RepositoryModule.kt`, `UseCaseModule.kt`
- `RegisterUserUseCase.kt`, `RegisterUserResult.kt`, `RegisterValidationError.kt`
- `RegisterUserUseCaseTest.kt`, `FakeUsuarioRepository.kt`
- `LoginUseCase.kt`, `GetUsuarioByIdUseCase.kt`, `HomeViewModel.kt`
- `AuthViewModel.kt`, `AuthRegisterState.kt`, `RegisterScreen.kt`
- `AnimalEntity.kt`, `NotificacionEntity.kt`, `NotificacionxAnimalEntity.kt`
- Schemas `1.json`–`6.json`
- `app/build.gradle.kts`, `gradle/libs.versions.toml`
- `ExampleInstrumentedTest.kt`, `ExampleUnitTest.kt`
)
