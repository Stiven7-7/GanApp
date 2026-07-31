# Línea base inicial de GANAPP

Documento de control de la **Fase 0 — Preparación y respaldo**.  
Único archivo de documentación autorizado a crear/modificar en esta fase (además de artefactos normales bajo `build/`).

---

## 1. Datos de la inspección

| Campo | Valor |
|---|---|
| Fecha | 2026-07-30 |
| Hora (inicio inspección Git) | 21:19:02 -05:00 |
| Hora (inicio assembleDebug) | 21:19:58 -05:00 |
| Hora (fin lint) | 21:20:49 -05:00 |
| Zona horaria | UTC-5 (America/Bogotá, observada en el sistema) |
| Sistema operativo | Microsoft Windows NT 10.0.26200.0 (win32) |
| Shell | PowerShell |
| Entorno | Workspace local Cursor / Android Studio project |
| Ruta del repositorio | `<repo-root>` |
| Java utilizado | OpenJDK 21.0.9 (`build 21.0.9+-14787801-b1163.94`), JBR de Android Studio |
| JAVA_HOME | Definido **temporalmente solo para la sesión** de comandos: `<JBR de Android Studio>`. No se modificó configuración permanente del sistema ni archivos del proyecto. |
| Gradle Wrapper | 8.13 (`gradle/wrapper/gradle-wrapper.properties`) |

---

## 2. Estado Git

### 2.1 Identidad del repositorio

| Campo | Valor |
|---|---|
| Raíz | `<repo-root>` |
| Rama activa | `master` |
| Upstream | `origin/master` |
| Relación con upstream (sin fetch) | `## master...origin/master` — **up to date** (`git rev-list --left-right --count origin/master...HEAD` → `0	0`) |
| Remoto | `origin` → `https://github.com/Stiven7-7/GanApp.git` (fetch/push) |

### 2.2 Último commit

| Campo | Valor |
|---|---|
| Hash completo | `f154ff1d268b50a24bbe436d92b32d4bda5a5a15` |
| Hash corto | `f154ff1` |
| Autor | Brayan Stiven Pinzon \<126515783+StivenPinzon@users.noreply.github.com\> |
| AuthorDate | Mon Mar 9 20:39:25 2026 -0500 |
| CommitDate | Mon Mar 9 20:39:25 2026 -0500 |
| Mensaje (primera línea) | `feat: implement user authentication with password hashing and add animal registration flow` |

### 2.3 Ramas

**Locales:**

- `* master`

**Remotas visibles (sin fetch):**

- `origin/HEAD -> origin/master`
- `origin/master`

### 2.4 Working tree

Estado al momento de la inspección (antes de crear este archivo):

```
 M .idea/deploymentTargetSelector.xml
 M .idea/misc.xml
?? .idea/appInsightsSettings.xml
?? docs/
```

Detalle:

| Categoría | Archivos |
|---|---|
| Modificados (tracked, no staged) | `.idea/deploymentTargetSelector.xml`, `.idea/misc.xml` |
| Staged | *(ninguno)* |
| No rastreados | `.idea/appInsightsSettings.xml`, `docs/` (contenido: `docs/diagnostico-tecnico-inicial.md`) |
| Commits locales pendientes de push | **Ninguno** detectable localmente (ahead=0; sin ejecutar fetch) |

### 2.5 Información pendiente de respaldo

Ver sección 2.6 y sección 10. Resumen: hay cambios IDE locales y documentación en `docs/` **sin commit**; no hay commits locales sin publicar.

### 2.6 Clasificación de información no respaldada

1. **Cambios rastreados sin commit:** `.idea/deploymentTargetSelector.xml`, `.idea/misc.xml`  
2. **Archivos staged sin commit:** ninguno  
3. **Archivos no rastreados:** `.idea/appInsightsSettings.xml`, `docs/diagnostico-tecnico-inicial.md` (y, tras esta fase, también `docs/baseline-inicial.md`)  
4. **Commits locales no publicados:** ninguno detectable (`0` ahead de `origin/master`, sin fetch)  
5. **Documentos técnicos fuera de Git:** `docs/diagnostico-tecnico-inicial.md` (untracked)  
6. **Archivos sensibles/locales que no deben versionarse:** `local.properties` (ignorado por `.gitignore`); artefactos bajo `app/build/` (ignorados); no se inspeccionó contenido de BD del dispositivo

**Consecuencia:** la rama de trabajo **no debe crearse todavía** mientras existan cambios locales / documentos no rastreados sin decisión de respaldo explícita.

---

## 3. Diagnósticos existentes

| Archivo | Ruta exacta | Estado en Git |
|---|---|---|
| Diagnóstico técnico inicial | `docs/diagnostico-tecnico-inicial.md` | **No rastreado** (`?? docs/`). No staged. No modificado (es untracked). No ignorado (`git check-ignore` no lo marca). Dentro del repositorio. |
| Línea base (este documento) | `docs/baseline-inicial.md` | Creado en Fase 0; quedará **no rastreado** hasta que se decida versionarlo en una fase posterior. |

No se encontraron otros archivos con nombres tipo `Diagnostico_Tecnico_Inicial.md`, `baseline*`, o informes técnicos adicionales bajo el workspace (búsqueda por glob).

No se copió ni movió ningún diagnóstico.

---

## 4. Estado de compilación

| Campo | Valor |
|---|---|
| Comando | `.\gradlew.bat assembleDebug --no-daemon` |
| Inicio | 2026-07-30 21:19:58 -05:00 |
| Fin | 2026-07-30 21:20:17 -05:00 |
| Duración aproximada | 19.2 s (Gradle reportó ~18 s) |
| Código de salida | 0 |
| Resultado | **SUCCESS** |
| Errores | Ninguno |
| Advertencias principales | Ninguna bloqueante en esta ejecución (mayoría UP-TO-DATE) |
| JAVA_HOME temporal | Sí (JBR Android Studio) |
| Java | OpenJDK 21.0.9 |
| Archivos fuente modificados | **No** (solo artefactos bajo `app/build/` como resultado normal de Gradle) |
| APK / salida | Tarea `:app:assembleDebug` completada |

---

## 5. Estado de pruebas

| Campo | Valor |
|---|---|
| Comando | `.\gradlew.bat test --no-daemon` |
| Inicio | 2026-07-30 21:20:18 -05:00 |
| Fin | 2026-07-30 21:20:33 -05:00 |
| Duración aproximada | 15.5 s (Gradle ~15 s) |
| Código de salida | 0 |
| Resultado | **SUCCESS** |
| Tareas | `:app:testDebugUnitTest`, `:app:testReleaseUnitTest`, `:app:test` |
| Reportes | `app/build/reports/tests/testDebugUnitTest/index.html`, `app/build/reports/tests/testReleaseUnitTest/index.html` |
| Pruebas ejecutadas (conocidas) | Plantillas `ExampleUnitTest` (assert 2+2=4) en variantes debug/release unit test |
| Limitaciones conocidas | `gradlew test` **no** ejecuta `androidTest`. No hay pruebas de negocio (ViewModels, Room, use cases, UI). |
| Errores | Ninguno |
| Advertencias | En esta corrida la mayoría de tareas estaban UP-TO-DATE; warnings históricos conocidos incluyen kapt + Kotlin 2.0 y deprecations en `HomeScreen` (documentados en diagnóstico previo) |

---

## 6. Estado de lint

| Campo | Valor |
|---|---|
| Comando | `.\gradlew.bat lint --no-daemon` |
| Inicio | 2026-07-30 21:20:33 -05:00 |
| Fin | 2026-07-30 21:20:49 -05:00 |
| Duración aproximada | 15.7 s (Gradle ~15 s) |
| Código de salida | 0 |
| Resultado | **SUCCESS** |
| Errores | **0** |
| Warnings | **52** (`0 errors, 52 warnings` en reporte) |
| Ubicación del reporte | `app/build/reports/lint-results-debug.html`, `.txt`, `.xml` |

### Advertencias principales (resumen, sin corregir)

- `OldTargetApi` / `GradleDependency` / `NewerVersionAvailable`: SDK 34 y versiones de librerías desactualizadas respecto a lo disponible.
- `UseTomlInstead` / Coil hardcoded `2.3.0` en `app/build.gradle.kts`.
- `KaptUsageInsteadOfKsp` para Room.
- `UnusedResources` (colores plantilla, algunos mipmaps).
- `IconLocation` (PNG en `drawable/`).
- `ObsoleteSdkInt` (checks API \< minSdk 26).
- `DefaultLocale` en `RegisterNotificationScreen.kt`.
- `RedundantLabel` en `AndroidManifest.xml`.

---

## 7. Versiones principales

| Tecnología | Versión | Archivo de evidencia |
|---|---|---|
| applicationId | `com.proyecto.ganapp` | `app/build.gradle.kts` |
| namespace | `com.proyecto.ganapp` | `app/build.gradle.kts` |
| minSdk | 26 | `app/build.gradle.kts` |
| targetSdk | 34 | `app/build.gradle.kts` |
| compileSdk | 34 | `app/build.gradle.kts` |
| Android Gradle Plugin | 8.13.0 | `gradle/libs.versions.toml` (`agp`) |
| Gradle Wrapper | 8.13 | `gradle/wrapper/gradle-wrapper.properties` |
| Kotlin | 2.0.21 | `gradle/libs.versions.toml` (`kotlin`) |
| Compose BOM | 2024.09.00 | `gradle/libs.versions.toml` (`composeBom`) |
| Compose Compiler plugin | 2.0.21 (`kotlin-compose`) | `gradle/libs.versions.toml` / `app/build.gradle.kts` |
| kotlinCompilerExtensionVersion (legacy) | 1.6.0 | `app/build.gradle.kts` (`composeOptions`) |
| Room | 2.6.1 | `gradle/libs.versions.toml` (`room`) |
| Hilt | 2.48 | `gradle/libs.versions.toml` (`hilt`) |
| Navigation Compose | 2.8.3 | `gradle/libs.versions.toml` |
| Coroutines | 1.7.3 | `gradle/libs.versions.toml` |
| JVM target | 17 | `app/build.gradle.kts` (`kotlinOptions.jvmTarget` / `compileOptions`) |

---

## 8. Configuración Room

| Campo | Valor | Evidencia |
|---|---|---|
| Nombre de base | `ganapp_db` | `AppDatabase.DATABASE_NAME`; `DatabaseModule` builder |
| Versión de esquema | **6** | `AppDatabase.kt` (`version = 6`) |
| exportSchema | `true` | `AppDatabase.kt` |
| Ubicación de esquemas | `app/schemas` (vía kapt `room.schemaLocation`) | `app/build.gradle.kts` |
| Ruta concreta exportada | `app/schemas/com.proyecto.ganapp.data.local.db.AppDatabase/` | directorio en disco |
| Esquemas disponibles | `1.json`, `2.json`, `3.json`, `4.json`, `5.json`, `6.json` | listado del directorio |
| fallbackToDestructiveMigration | **Sí**, activo | `di/DatabaseModule.kt` línea con `.fallbackToDestructiveMigration()` |
| Clases `Migration` | **No existen** en el código fuente inspeccionado | búsqueda de `Migration` en `app/src` solo encuentra el comentario del fallback |

**Estado confirmado (sin modificar):** migraciones destructivas habilitadas; schemas 1–6 preservados en el repo; no se eliminó fallback ni se crearon migraciones en esta fase.

---

## 9. Datos locales de desarrollo

### 9.1 Datos funcionales que podrían existir en el dispositivo/emulador

Persistidos en Room (`ganapp_db`), según el modelo actual:

- Usuarios (`usuario`)
- Animales (`animal`)
- Notificaciones (`notificacion`)
- Relaciones animal–notificación (`notificacion_animal`)
- Sesión: **no hay DataStore ni SharedPreferences de sesión en el código**; la “sesión” efectiva es solo el `idUsuario` pasado por navegación tras login. Tras reinicio de app, el flujo vuelve a `login`.

No se inspeccionó ni extrajo contenido de bases de datos locales ni contraseñas.

### 9.2 Autorización de descarte posterior

El coordinador del proyecto **autorizó** que los datos actuales de desarrollo puedan descartarse en una **fase posterior**.

Método futuro autorizado (únicamente):

- reinstalación de la aplicación; o
- borrado de datos de la aplicación.

### 9.3 Confirmación de esta fase

**En la Fase 0 no se limpió ningún dato** (ni reinstalación, ni clear data, ni borrado de BD, ni desinstalación).

---

## 10. Riesgos previos al inicio

| Riesgo | Estado |
|---|---|
| Cambios locales sin respaldo (IDE) | Sí: `.idea/deploymentTargetSelector.xml`, `.idea/misc.xml` modificados; `.idea/appInsightsSettings.xml` untracked |
| Documentos no rastreados | Sí: `docs/diagnostico-tecnico-inicial.md` y (tras Fase 0) `docs/baseline-inicial.md` |
| Commits no publicados | No detectados (ahead 0; sin fetch) |
| Fallas de assembleDebug / test / lint | **No** en esta corrida (los tres SUCCESS) |
| Migraciones destructivas Room | Confirmado: `fallbackToDestructiveMigration()` activo; sin clases `Migration` |
| Ausencia de pruebas reales de negocio | Confirmado: solo `ExampleUnitTest` / `ExampleInstrumentedTest` de plantilla |
| Working tree no limpio | Impide crear rama de trabajo de forma segura sin decisión de respaldo previa |
| Dependencia de JAVA_HOME | El shell no tenía Java en PATH; se usó JBR temporal — riesgo operativo en CI/otros shells sin JAVA_HOME |

---

## 11. Procedimiento de restauración

Objetivo: poder **volver a examinar** el estado de esta línea base sin destruir trabajo local no respaldado.

### 11.1 Puntos de anclaje

| Elemento | Valor |
|---|---|
| Rama documentada | `master` |
| Commit base | `f154ff1d268b50a24bbe436d92b32d4bda5a5a15` (`f154ff1`) |
| Remoto de referencia | `origin` → `https://github.com/Stiven7-7/GanApp.git` |
| Upstream | `origin/master` |

### 11.2 Verificar estado (solo lectura)

```bat
git rev-parse --show-toplevel
git branch --show-current
git status --short
git status --branch
git log -1 --format=fuller
git rev-parse HEAD
```

Comparar `HEAD` con `f154ff1d268b50a24bbe436d92b32d4bda5a5a15` y revisar que los únicos deltas esperados sean IDE + `docs/` (+ `build/` ignorado).

### 11.3 Archivos locales a preservar manualmente antes de cualquier operación que toque el working tree

- `docs/diagnostico-tecnico-inicial.md`
- `docs/baseline-inicial.md` (este archivo)
- Cualquier cambio intencional en `.idea/` que el equipo quiera conservar (hoy: modificaciones en `deploymentTargetSelector.xml`, `misc.xml`, y untracked `appInsightsSettings.xml`)

Copiar esos archivos a una ubicación externa si se planea limpiar el árbol.

### 11.4 Cómo examinar el commit base (no destructivo)

Comandos **seguros** sugeridos (documentados, **no ejecutados** en Fase 0):

```bat
git show f154ff1d268b50a24bbe436d92b32d4bda5a5a15
```

Solo lectura del árbol (alternativa no destructiva):

```bat
git archive --format=zip --output=<ruta-temporal>\ganapp-baseline-f154ff1.zip f154ff1
```

### 11.5 Advertencias — NO ejecutar sobre cambios sin respaldo

**No usar** (salvo instrucción explícita posterior y respaldo confirmado):

- `git reset --hard`
- `git clean -fd` / `git clean -fdx`
- `git checkout -- <file>` / `git restore` forzando sobre archivos con cambios no respaldados
- `git checkout` forzado que descarte working tree
- `git push --force` / rewrite de historial

### 11.6 Reportes técnicos relevantes

| Recurso | Ubicación |
|---|---|
| Diagnóstico previo | `docs/diagnostico-tecnico-inicial.md` |
| Esta línea base | `docs/baseline-inicial.md` |
| Lint | `app/build/reports/lint-results-debug.html` |
| Tests unitarios | `app/build/reports/tests/testDebugUnitTest/index.html` |

Nota: `app/build/` es generable y no debe versionarse.

### 11.7 Condiciones para considerar recuperable el estado

El estado de línea base se considera recuperable si:

1. El commit `f154ff1d268b50a24bbe436d92b32d4bda5a5a15` sigue alcanzable en el remoto o en el repo local.  
2. Los documentos `docs/*.md` se preservaron fuera de Git o se versionaron conscientemente.  
3. No se ejecutaron limpiezas destructivas del working tree sin respaldo.  
4. Los schemas Room `1.json`–`6.json` siguen presentes en `app/schemas/...`.

---

## 12. Recomendación de rama y commit base

| Campo | Valor |
|---|---|
| Nombre recomendado | `feature/auth-session-foundation` |
| Alternativa | No requerida: la convención observada en el último commit usa prefijo `feat:` en mensajes, pero no hay otras ramas locales para inferir naming; `feature/...` es coherente y preferido. |
| Commit base | `f154ff1d268b50a24bbe436d92b32d4bda5a5a15` |
| Condición antes de crear la rama | Working tree respaldado o limpio respecto a lo que el equipo quiera conservar: decidir qué hacer con cambios `.idea/` y con `docs/` (commitear, ignorar o archivar fuera). |
| ¿Se creó la rama en Fase 0? | **No.** Confirmado: no se ejecutó `git branch`/`git checkout -b` de creación. |

**Regla aplicada:** existen cambios locales y archivos no rastreados relevantes → **la rama no debe crearse todavía**.

---

## 13. Criterio de finalización de la Fase 0

| Criterio | Cumplido |
|---|---|
| Estado Git completamente documentado | Sí |
| Información no respaldada identificada | Sí |
| `assembleDebug` ejecutado y registrado | Sí (SUCCESS) |
| `test` ejecutado y registrado | Sí (SUCCESS) |
| `lint` ejecutado y registrado | Sí (SUCCESS, 0 errors / 52 warnings) |
| applicationId y configuración Room confirmados | Sí |
| Diagnósticos existentes clasificados respecto a Git | Sí |
| Procedimiento de restauración documentado | Sí |
| Solo se creó/modificó `docs/baseline-inicial.md` (+ artefactos `build/`) | Sí (verificación en sección de cierre) |
| No se creó ninguna rama | Sí |
| No se realizaron cambios funcionales, arquitectónicos, de dependencias o de Room | Sí |

### Veredicto de Fase 0

**FASE 0 COMPLETA** (documentación y validación).  

Pendiente operativo **fuera del alcance de creación de rama**: respaldar/decidir sobre `.idea/` y `docs/` antes de iniciar Fase 1 / crear `feature/auth-session-foundation`.

---

## 14. Acciones explícitamente NO realizadas (confirmación)

- No se modificó ningún archivo fuente existente.  
- No se formateó código.  
- No se corrigieron warnings.  
- No se actualizaron dependencias ni Gradle.  
- No se modificó Room, schemas, versión de BD, relaciones ni `fallbackToDestructiveMigration`.  
- No se crearon migraciones.  
- No se movieron/renombraron clases o paquetes.  
- No se implementó autenticación adicional ni DataStore.  
- No se refactorizaron animales/notificaciones.  
- No se limpiaron datos de la app / BD / instalación.  
- No se crearon commits, push, pull, merge, rebase, stash.  
- No se creó ninguna rama.  
- No se modificó `.gitignore`.  
- No se ejecutaron comandos destructivos de Git.

---

*Fin de la línea base inicial — Fase 0.*
