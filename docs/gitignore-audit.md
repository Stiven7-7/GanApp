# Auditoría de versionado e ignorados de GANAPP

**Fecha:** 2026-07-30  
**Rama:** `master`  
**Raíz:** `C:/Users/Administrator/StudioProjects/GanApp`  
**Alcance:** Solo inspección y documentación. No se modificó `.gitignore`, ni el índice de forma intencional (add/rm/cached), ni archivos fuente.

### Estado Git al inicio / verificación de esta auditoría

Salida exacta de `git status --short` (inicio y cierre equivalentes en archivos relevantes):

```
 M .idea/deploymentTargetSelector.xml
 M .idea/misc.xml
?? .idea/appInsightsSettings.xml
?? docs/
```

También registrado:

- `git status --branch` → `On branch master` / `Your branch is up to date with 'origin/master'.`
- `git rev-parse --show-toplevel` → `C:/Users/Administrator/StudioProjects/GanApp`
- `git branch --show-current` → `master`

---

## 1. Estado actual de .gitignore

### 1.1 Contenido completo actual (raíz)

Archivo: `.gitignore`

```
*.iml
.gradle
/local.properties
/.idea/caches
/.idea/libraries
/.idea/modules.xml
/.idea/workspace.xml
/.idea/navEditor.xml
/.idea/assetWizardSettings.xml
.DS_Store
/build
/captures
.externalNativeBuild
.cxx
local.properties
```

Adicionalmente:

- `app/.gitignore` contiene solo `/build`
- `.idea/.gitignore` contiene `/shelf/` y `/workspace.xml`

### 1.2 Análisis

| Aspecto | Evaluación |
|---|---|
| Reglas existentes | Plantilla Android Studio típica: `*.iml`, `.gradle`, `local.properties` (duplicada con y sin `/`), subset de `.idea`, `build` raíz, `captures`, NDK/CMake |
| Categorías cubiertas | IDE parcial, Gradle cache, SDK local, build raíz, OS macOS (`.DS_Store`) |
| Categorías faltantes | APK/AAB, keystores/firma, secretos (`.env`, `*.properties` de signing), logs, Windows (`Thumbs.db`, `desktop.ini`), más archivos `.idea` locales (p. ej. `deploymentTargetSelector.xml`, `appInsightsSettings.xml`, `deviceManager.xml`, estados Copilot/StudioBot), `*.log` |
| Reglas demasiado amplias | No hay `*.xml` / `*.json` / `docs/` / `app/schemas/` globales. `/build` en raíz no ignora `app/build` (cubierto por `app/.gitignore`) |
| Riesgo de ocultar necesarios | Bajo con el `.gitignore` actual: `docs/` y `app/schemas/` **no** están ignorados |
| ¿`app/schemas` podría quedar ignorado? | **No** con reglas actuales (`git check-ignore` sin match en `1.json`/`6.json`) |
| ¿`docs/` podría quedar ignorado? | **No** con reglas actuales |

Nota: `local.properties` aparece dos veces (líneas 3 y 15); redundante pero inofensivo.

---

## 2. Archivos .idea rastreados

Salida completa de `git ls-files .idea`:

```
.idea/.gitignore
.idea/AndroidProjectSystem.xml
.idea/codeStyles/Project.xml
.idea/codeStyles/codeStyleConfig.xml
.idea/compiler.xml
.idea/copilot.data.migration.agent.xml
.idea/copilot.data.migration.ask.xml
.idea/copilot.data.migration.ask2agent.xml
.idea/copilot.data.migration.edit.xml
.idea/deploymentTargetSelector.xml
.idea/deviceManager.xml
.idea/gradle.xml
.idea/inspectionProfiles/Project_Default.xml
.idea/migrations.xml
.idea/misc.xml
.idea/runConfigurations.xml
.idea/studiobot.xml
.idea/vcs.xml
```

### Clasificación por archivo rastreado

| Archivo | Categoría |
|---|---|
| `.idea/.gitignore` | Configuración compartida útil |
| `.idea/AndroidProjectSystem.xml` | Configuración compartida útil (Gradle project system) |
| `.idea/codeStyles/Project.xml` | Configuración compartida útil |
| `.idea/codeStyles/codeStyleConfig.xml` | Configuración compartida útil |
| `.idea/compiler.xml` | Configuración compartida útil (bytecode 21) |
| `.idea/copilot.data.migration.*.xml` (4) | Caché o estado regenerable (migración Copilot) |
| `.idea/deploymentTargetSelector.xml` | Configuración de dispositivo o emulador |
| `.idea/deviceManager.xml` | Configuración de dispositivo o emulador / preferencia UI local |
| `.idea/gradle.xml` | Configuración compartida útil (módulos + JVM Gradle) |
| `.idea/inspectionProfiles/Project_Default.xml` | Configuración compartida útil |
| `.idea/migrations.xml` | Configuración compartida útil / estado de migración IDE |
| `.idea/misc.xml` | Configuración compartida útil (JDK del proyecto) |
| `.idea/runConfigurations.xml` | Configuración compartida útil (productores ignorados) |
| `.idea/studiobot.xml` | Configuración local del IDE (preferencia Studio Bot) |
| `.idea/vcs.xml` | Configuración compartida útil (mapeo Git) |

`git check-ignore -v` sobre los tres archivos detectados en status:

| Archivo | Resultado |
|---|---|
| `.idea/deploymentTargetSelector.xml` | **No está ignorado** (sin salida) |
| `.idea/appInsightsSettings.xml` | **No está ignorado** (sin salida) |
| `.idea/misc.xml` | **No está ignorado** (sin salida) |

---

## 3. Análisis individual de los tres archivos detectados

### 3.1 `.idea/deploymentTargetSelector.xml`

| Campo | Valor |
|---|---|
| Tracked | Sí |
| Ignored | No |
| Estado Git | ` M` (modificado, no staged) |
| Contenido | Selección de target de despliegue para run config `app` |
| Rutas absolutas | No |
| Identificadores de dispositivo | Sí: `DeviceId` con `pluginId="FirebaseDirectAccess"`, `identifier="model_id=e1q/36"` |
| Información de máquina | Timestamp local de selección (`DropdownSelection timestamp=...`) |
| ¿Aporta config compartida necesaria? | No para compilar; solo preferencia de dispositivo del desarrollador |
| Riesgo | Bajo–medio: ensucia el working tree; puede filtrar preferencias de dispositivo/cloud de un entorno concreto |
| Recomendación | **Dejar de versionar posteriormente** + ignorar; **revertir posteriormente** el cambio local tras decidir (o conservar solo si el equipo quiere compartir targets — no es el caso típico) |

### 3.2 `.idea/misc.xml`

| Campo | Valor |
|---|---|
| Tracked | Sí |
| Ignored | No |
| Estado Git | ` M` (marcado modificado) |
| Diff de contenido | `git diff` **vacío** de hunks; `git hash-object` del working copy **coincide** con blob de `HEAD` |
| Naturaleza del cambio | Ruido de working tree / EOL / metadatos de índice (aviso `LF will be replaced by CRLF`). Contenido sustancial idéntico a HEAD |
| Rutas | Solo `$PROJECT_DIR$/build/classes` (portable) |
| Identificadores de dispositivo | No |
| Información de máquina | `project-jdk-name="jbr-21"` (nombre de JDK del IDE; típico Android Studio, no ruta absoluta) |
| ¿Aporta config compartida? | Sí: languageLevel JDK_21 + tipo Android |
| Riesgo | Bajo (falso positivo de dirty). Revertir limpia el status sin perder contenido |
| Recomendación | **Conservar versionado** del archivo; el cambio local **requiere decisión manual** (probablemente **revertir posteriormente** solo para limpiar el status, tras confirmar que el contenido sigue siendo el deseado) |

### 3.3 `.idea/appInsightsSettings.xml`

| Campo | Valor |
|---|---|
| Tracked | No (`??`) |
| Ignored | No |
| Estado Git | Untracked |
| Contenido | Preferencias UI de App Insights / Firebase Crashlytics en el IDE |
| Rutas absolutas | No |
| Identificadores de equipo/usuario | No detectados |
| Credenciales / tokens / secretos | No. `appId` = `PLACEHOLDER`; `mobileSdkAppId`, `projectId`, `projectNumber` vacíos |
| Configuración de cuenta | Estructura de conexión Firebase, sin datos reales de cuenta |
| Preferencias locales | Sí (filtros de señales, intervalo 30 días, visibilidad) |
| ¿Necesario para compilar/ejecutar GANAPP? | **No** |
| Riesgo | Bajo hoy; medio si en el futuro se rellena con IDs reales de Firebase |
| Recomendación | **Ignorar** (no versionar). No añadirlo al índice |

---

## 4. Archivos generados o sensibles encontrados

| Ruta / patrón | En disco | Tracked | Ignored | Sensible | Generado | ¿Versionado por error? |
|---|---|---|---|---|---|---|
| `local.properties` | Sí | No | Sí (`.gitignore:15`) | Sí (ruta SDK local) | Local IDE/Gradle | No (correctamente ignorado) |
| `*.jks` / `*.keystore` | No encontrados | No | No hay regla explícita | Sería alto | N/A | N/A |
| `keystore.properties` / `signing.properties` / `secrets.properties` | No | No | No hay regla | Sería alto | N/A | N/A |
| `.env` / `.env.*` | No | No | No hay regla | Sería alto | N/A | N/A |
| `app/build/outputs/apk/debug/app-debug.apk` | Sí | No | Sí (`app/.gitignore:/build`) | Bajo–medio (binario de build) | Sí | No |
| Otros `*.apk` / `*.ap_` bajo `app/build/` | Sí | No | Sí | Generados | Sí | No |
| `*.aab` | No | No | No regla explícita | — | — | — |
| `build/` (raíz) | No existe | — | Sí (`/build`) | — | — | — |
| `app/build/` | Sí | No | Sí (`app/.gitignore`) | Generado | Sí | No |
| `.gradle/` | Sí | No | Sí | Cache | Sí | No |
| `*.iml` | No encontrados en FS | No | Sí (`*.iml`) | — | — | — |
| `*.log` | No encontrados | No | **Falta regla** | Posible | Posible | — |
| `.DS_Store` | — | No | Sí | OS | Sí | — |
| `Thumbs.db` / `desktop.ini` | No | No | **Falta regla** | OS | Sí | — |

**Conclusión:** no hay keystores ni secretos rastreados. `local.properties` y builds están correctamente fuera de Git. Faltan reglas preventivas para firma, APK/AAB fuera de `build/`, logs y archivos Windows.

---

## 5. Estado de los esquemas Room

| Campo | Valor |
|---|---|
| Ruta exacta | `app/schemas/com.proyecto.ganapp.data.local.db.AppDatabase/` |
| Archivos | `1.json`, `2.json`, `3.json`, `4.json`, `5.json`, `6.json` |
| Tracked | **Los 6** aparecen en `git ls-files app/schemas` |
| Ignored | **No** (`check-ignore` sin match en `1.json` y `6.json`) |
| Regla `.gitignore` que los excluya | Ninguna actual |

**Política confirmada por evidencia:** los esquemas exportados **permanecen versionados** y deben seguir así.

---

## 6. Revisión de seguridad de docs/

Archivos revisados (sin modificar):

- `docs/diagnostico-tecnico-inicial.md`
- `docs/baseline-inicial.md`

| Tipo de dato | ¿Presente? | Clasificación |
|---|---|---|
| Contraseñas en claro | No | — |
| Hashes bcrypt / password hashes | No | — |
| Tokens / API keys / keystores / secretos | No | — |
| Credenciales en URLs | No | — |
| Correos personales | Solo email noreply de GitHub del autor del commit (`…@users.noreply.github.com`) en baseline | Técnica aceptable (metadato Git público) |
| Rutas locales de usuario | Sí en `baseline-inicial.md`: `C:/Users/Administrator/StudioProjects/GanApp` y mención de JBR bajo `C:\Program Files\Android\Android Studio\jbr` | **Información local innecesaria** al versionar (no secreto, pero conviene valorar redacción a ruta relativa del repo) |
| URL remota del repo | `https://github.com/Stiven7-7/GanApp.git` | Técnica aceptable (ya pública si el repo lo es) |
| Mención de BCrypt / PasswordUtils / campo `contrasena` | Sí, como arquitectura | Técnica aceptable (sin valores) |
| Hash de commit Git | Sí | Técnica aceptable |

**Recomendación documental (no aplicada):** al versionar `docs/`, considerar redactar rutas absolutas con usuario Windows (`Administrator`) sustituyéndolas por `<repo-root>` o rutas relativas. No se detectaron secretos que bloqueen el versionado.

---

## 7. Política recomendada

| # | Categoría | Política GANAPP |
|---|---|---|
| 1 | Código fuente | Versionar siempre (`app/src/**`) |
| 2 | Archivos Gradle | Versionar (`*.gradle.kts`, `gradle/libs.versions.toml`, `settings.gradle.kts`, `gradle.properties`) |
| 3 | Gradle Wrapper | Versionar (`gradle/wrapper/**` incluyendo jar y properties) |
| 4 | `local.properties` | Ignorar siempre (ya cubierto) |
| 5 | Carpetas `build` | Ignorar (`/build`, `app/build` vía `app/.gitignore`; no versionar) |
| 6 | Carpeta `.gradle` | Ignorar (ya cubierto) |
| 7 | `.idea` | **No ignorar toda la carpeta**. Ignorar locales (targets, insights, device UI, workspace ya cubierto, caches). Mantener codeStyle, inspectionProfiles, vcs, compiler, gradle, AndroidProjectSystem, runConfigurations compartidas cuando aporten. Dejar de rastrear estados Copilot/StudioBot y selectores de dispositivo |
| 8 | `*.iml` | Ignorar (ya cubierto) |
| 9 | Esquemas Room | **Versionar siempre** `app/schemas/` |
| 10 | `docs/` | **Versionar** documentación técnica; revisar redacción de rutas locales antes del primer commit |
| 11 | APK / AAB | Ignorar (`*.apk`, `*.aab`, `*.ap_`) aunque salgan fuera de `build/` |
| 12 | Keystores / firma / secretos | Ignorar (`*.jks`, `*.keystore`, `keystore.properties`, `signing.properties`, `secrets.properties`, `.env*`) |
| 13 | Logs / temporales | Ignorar (`*.log`, `captures` ya, posibles dumps) |
| 14 | Archivos SO | Ignorar (`.DS_Store` ya; añadir `Thumbs.db`, `desktop.ini`) |
| 15 | Recursos y configs compartidas | Versionar `app/src/main/res/**`, `AndroidManifest.xml`, pruebas `test`/`androidTest`, proguard rules |

### Distinción `.idea` (detalle)

| Acción | Archivos |
|---|---|
| Deben ignorarse (locales) | `deploymentTargetSelector.xml`, `appInsightsSettings.xml`, preferiblemente `deviceManager.xml`, estados Copilot (`copilot.data.migration.*.xml`), `studiobot.xml`; mantener reglas existentes de workspace/caches/libraries/modules/navEditor/assetWizard |
| Pueden mantenerse versionados | `codeStyles/**`, `inspectionProfiles/**`, `vcs.xml`, `compiler.xml`, `gradle.xml`, `AndroidProjectSystem.xml`, `runConfigurations.xml`, `migrations.xml`, `.idea/.gitignore`, `misc.xml` (con cuidado) |
| Requieren decisión según contenido | `misc.xml` (mantener, limpiar dirty), `runConfigurations.xml` (si en el futuro incluyen paths locales), cualquier nuevo XML de plugins |

---

## 8. Bloque de .gitignore propuesto

**No aplicado.** Solo propuesta para una fase posterior. No ignora `docs/`, `app/schemas/`, wrapper ni recursos.

```gitignore
# --- OS ---
.DS_Store
Thumbs.db
desktop.ini

# --- Gradle / build locales ---
.gradle/
**/build/
!gradle/wrapper/gradle-wrapper.jar
/captures/
.externalNativeBuild/
.cxx/

# --- SDK / máquina ---
local.properties

# --- IDE: IntelliJ / Android Studio (selectivo; NO ignorar toda .idea/) ---
*.iml
.idea/caches/
.idea/libraries/
.idea/modules.xml
.idea/workspace.xml
.idea/navEditor.xml
.idea/assetWizardSettings.xml
.idea/shelf/
.idea/deploymentTargetSelector.xml
.idea/appInsightsSettings.xml
.idea/deviceManager.xml
.idea/studiobot.xml
.idea/copilot.data.migration.*.xml

# --- Artefactos de empaquetado (por si aparecen fuera de build/) ---
*.apk
*.aab
*.ap_

# --- Firma y secretos ---
*.jks
*.keystore
keystore.properties
signing.properties
secrets.properties
.env
.env.*

# --- Logs ---
*.log

# NOTA DE POLÍTICA (comentarios; no son reglas):
# - Versionar siempre: app/schemas/, docs/, gradle/wrapper/, *.gradle.kts, libs.versions.toml
# - No añadir reglas globales *.xml ni *.json
```

Aviso: agregar reglas **no** deja de rastrear archivos ya tracked; hace falta `git rm --cached` en fase posterior.

---

## 9. Archivos que deberían dejar de rastrearse

| Archivo | Estado actual | Motivo | Recomendación | Riesgo | Comando futuro sugerido |
|---|---|---|---|---|---|
| `.idea/deploymentTargetSelector.xml` | Tracked + modificado | Preferencia de dispositivo local (`FirebaseDirectAccess` / `model_id=e1q/36`) | Dejar de rastrear + ignorar; luego decidir revert/working tree | Bajo | `git rm --cached -- .idea/deploymentTargetSelector.xml` |
| `.idea/deviceManager.xml` | Tracked | UI de Device Manager local | Dejar de rastrear + ignorar | Bajo | `git rm --cached -- .idea/deviceManager.xml` |
| `.idea/copilot.data.migration.agent.xml` | Tracked | Estado regenerable Copilot | Dejar de rastrear + ignorar | Bajo | `git rm --cached -- .idea/copilot.data.migration.agent.xml` |
| `.idea/copilot.data.migration.ask.xml` | Tracked | Idem | Idem | Bajo | `git rm --cached -- .idea/copilot.data.migration.ask.xml` |
| `.idea/copilot.data.migration.ask2agent.xml` | Tracked | Idem | Idem | Bajo | `git rm --cached -- .idea/copilot.data.migration.ask2agent.xml` |
| `.idea/copilot.data.migration.edit.xml` | Tracked | Idem | Idem | Bajo | `git rm --cached -- .idea/copilot.data.migration.edit.xml` |
| `.idea/studiobot.xml` | Tracked | Preferencia local Studio Bot (`shareContext`) | Dejar de rastrear + ignorar | Bajo | `git rm --cached -- .idea/studiobot.xml` |

**No** se recomienda `git rm --cached` sobre toda `.idea/`.

Para `.idea/misc.xml`: **no** dejar de rastrear; solo limpiar el dirty state posteriormente si se confirma identidad de contenido.

Para `.idea/appInsightsSettings.xml`: **nunca trackear**; solo añadir a `.gitignore`.

---

## 10. Archivos que deben conservarse versionados

| Ruta o patrón | Motivo | Evidencia actual | Riesgo de ignorarlo |
|---|---|---|---|
| `app/src/main/java/**` | Código fuente | Tracked en el repo | Pierde la aplicación |
| `app/src/main/res/**` | Recursos Android | Tracked | UI/assets rotos |
| `app/src/main/AndroidManifest.xml` | Manifest | Tracked | App no instala/configura |
| `app/src/test/**`, `app/src/androidTest/**` | Pruebas | Tracked (plantillas) | Pierde tests |
| `*.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`, `settings.gradle.kts` | Build reproducible | Tracked | Build irreproducible |
| `gradle/wrapper/**` | Wrapper | `gradle-wrapper.jar` + `.properties` tracked | Builds inconsistentes |
| `app/schemas/**` (1–6.json) | Historia Room | 6 archivos tracked, no ignorados | Pierde baseline de migraciones futuras |
| `docs/**` | Documentación técnica | Hoy untracked; política: versionar | Pierde línea base/diagnóstico |
| `app/proguard-rules.pro` | Reglas release | Existe en módulo | Riesgo en minify futuro |
| `.idea/codeStyles/**`, `.idea/inspectionProfiles/**` | Estilo/inspecciones de equipo | Tracked | Inconsistencia de formato entre devs |
| `.idea/vcs.xml`, `.idea/compiler.xml`, `.idea/gradle.xml`, `.idea/AndroidProjectSystem.xml` | IDE compartido mínimo | Tracked | Setup IDE más frágil |
| `.idea/misc.xml` | JDK/language level compartido | Tracked | Confusión de JDK entre máquinas |

---

## 11. Comandos propuestos para una fase posterior

**Solo documentación. No ejecutados en esta auditoría.**

### 11.1 Añadir reglas (modifica `.gitignore` — no destructivo del historial)

```bat
REM Editar .gitignore manualmente con el bloque propuesto de la sección 8
git add -- .gitignore
```

### 11.2 Dejar de rastrear archivos (modifica el índice; **no** borra el working tree por defecto)

```bat
git rm --cached -- .idea/deploymentTargetSelector.xml
git rm --cached -- .idea/deviceManager.xml
git rm --cached -- .idea/studiobot.xml
git rm --cached -- .idea/copilot.data.migration.agent.xml
git rm --cached -- .idea/copilot.data.migration.ask.xml
git rm --cached -- .idea/copilot.data.migration.ask2agent.xml
git rm --cached -- .idea/copilot.data.migration.edit.xml
```

**Advertencia:** `git rm --cached` modifica el staging/índice. No ejecutar sobre toda `.idea/`.

### 11.3 Revertir cambios locales (destructivo del working tree del archivo)

```bat
REM Solo tras respaldo/decisión. Descarta cambios locales del archivo:
git restore -- .idea/deploymentTargetSelector.xml
git restore -- .idea/misc.xml
```

**Advertencia:** `git restore` / `git checkout -- <file>` **destruye** cambios locales no commitados de ese archivo.

### 11.4 Verificación (solo lectura)

```bat
git status --short
git check-ignore -v .idea/deploymentTargetSelector.xml .idea/appInsightsSettings.xml
git ls-files app/schemas
git ls-files .idea
git check-ignore -v docs/baseline-inicial.md
```

### Orden seguro recomendado (futuro)

1. Respaldo manual de `docs/` y de XML `.idea` si hay duda.  
2. Actualizar `.gitignore` (sin commit aún o en commit documental dedicado).  
3. `git rm --cached` archivo por archivo de la sección 9.  
4. Decidir `git restore` de `deploymentTargetSelector.xml` / `misc.xml` si se quiere working tree limpio.  
5. `git status` y verificar que `app/schemas` y `docs` siguen visibles.  
6. Solo entonces commit(s) documentales / de ignore — **fuera de esta auditoría**.

---

## 12. Riesgos

| Riesgo | Detalle |
|---|---|
| Archivos locales rastreados | `deploymentTargetSelector.xml`, Copilot migrations, `studiobot.xml`, `deviceManager.xml` |
| Secretos potenciales | Ninguno tracked hoy; `appInsightsSettings` vacío/PLACEHOLDER; falta prevención de keystores/`.env` |
| Pérdida de configuración compartida | Ignorar toda `.idea/` borraría codeStyles/inspections útiles |
| Reglas demasiado amplias | Evitar `*.xml`/`*.json`/ignorar `docs/` o `app/schemas/` |
| Efecto limitado de `.gitignore` | No desfija tracked files; requiere `rm --cached` |
| `git rm --cached` indiscriminado | Puede quitar del índice configs compartidas necesarias |
| Rutas locales en docs | `Administrator` path en baseline al versionar docs |
| Dirty `misc.xml` engañoso | Diff vacío; riesgo de “arreglar” de más |

---

## 13. Recomendación final

### Por archivo (resumen)

| Archivo | Acción futura |
|---|---|
| `.idea/deploymentTargetSelector.xml` | Ignorar + `rm --cached` + restore local |
| `.idea/misc.xml` | Mantener versionado; restore para limpiar dirty si se confirma |
| `.idea/appInsightsSettings.xml` | Ignorar; no `git add` |
| `.idea/deviceManager.xml` | Ignorar + `rm --cached` |
| `.idea/copilot.data.migration.*.xml` | Ignorar + `rm --cached` |
| `.idea/studiobot.xml` | Ignorar + `rm --cached` |
| `docs/*` | Versionar (tras valorar redacción de rutas) |
| `app/schemas/*` | Mantener versionados |
| `local.properties` | Mantener ignorado |

### Orden seguro posterior

1. Decidir texto de `.gitignore` (sección 8).  
2. `rm --cached` selectivo (sección 9).  
3. Limpiar working tree de targets/`misc` con `restore` solo si procede.  
4. Añadir `docs/` en un commit documental consciente.  
5. Verificar schemas + status.  
6. No crear rama ni Fase 1 hasta que el working tree quede en el estado acordado.

### Confirmación de no ejecución en esta auditoría

No se ejecutó: `git add`, `git rm`, `git rm --cached`, `git restore`, `git checkout` de archivos, `git reset`, `git clean`, commit, push, pull, fetch, merge, rebase, stash, ni creación de rama.  
No se modificó `.gitignore` ni archivos bajo `.idea`.  
Único archivo nuevo autorizado creado: `docs/gitignore-audit.md`.

---

*Fin de la auditoría de versionado e ignorados.*
