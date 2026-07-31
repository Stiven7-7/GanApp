# Diagnóstico técnico de GANAPP

**Fecha de inspección:** 30 de julio de 2026  
**Alcance:** Solo lectura y validación Gradle (`assembleDebug`, `test`, `lint`). No se modificó código fuente existente.  
**Módulo analizado:** `:app` (único módulo de aplicación).

---

## 1. Resumen ejecutivo

GANAPP es una aplicación Android monomodular (`:app`) orientada a gestión ganadera local: autenticación de usuarios, registro de animales y notificaciones/recordatorios asociados. Implementa una **Clean Architecture parcial** (domain / data / ui) con **Jetpack Compose**, **Hilt**, **Room** y programación de alarmas vía **AlarmManager**.

El proyecto **compila correctamente** (`assembleDebug` OK), los tests unitarios de plantilla pasan y lint finaliza con **0 errores y 52 warnings**. La arquitectura está esbozada, pero presenta incumplimientos reales de separación de capas (ViewModels que inyectan repositorios directamente, casos de uso muertos o stub, dominio filtrado con entidades Room), dependencias Gradle declaradas y no usadas (Retrofit, WorkManager, Timber), tema Compose no aplicado, sin persistencia de sesión, migraciones Room destructivas y cobertura de pruebas nula sobre lógica de negocio.

---

## 2. Tecnologías y versiones detectadas

| Tecnología | Versión | Archivo de evidencia |
|---|---|---|
| Kotlin | 2.0.21 | `gradle/libs.versions.toml` (`kotlin`) |
| Android Gradle Plugin | 8.13.0 | `gradle/libs.versions.toml` (`agp`) |
| Gradle Wrapper | 8.13 | `gradle/wrapper/gradle-wrapper.properties` |
| compileSdk | 34 | `app/build.gradle.kts` |
| targetSdk | 34 | `app/build.gradle.kts` |
| minSdk | 26 | `app/build.gradle.kts` |
| Jetpack Compose BOM | 2024.09.00 | `gradle/libs.versions.toml` (`composeBom`) |
| Compose Compiler plugin | 2.0.21 (plugin `kotlin-compose`) | `gradle/libs.versions.toml`, `app/build.gradle.kts` |
| kotlinCompilerExtensionVersion (legacy) | 1.6.0 | `app/build.gradle.kts` (`composeOptions`) — coexistente con plugin Compose; no se verificó si se ignora en runtime del plugin |
| Hilt | 2.48 | `gradle/libs.versions.toml` (`hilt`) |
| Hilt Navigation Compose | 1.2.0 | `gradle/libs.versions.toml` |
| Room | 2.6.1 | `gradle/libs.versions.toml` (`room`) |
| Navigation Compose | 2.8.3 | `gradle/libs.versions.toml` (`navigation`) |
| Coroutines | 1.7.3 | `gradle/libs.versions.toml` (`coroutines`) |
| Lifecycle Runtime KTX | 2.6.1 | `gradle/libs.versions.toml` |
| Activity Compose | 1.8.0 | `gradle/libs.versions.toml` |
| Core KTX | 1.10.1 | `gradle/libs.versions.toml` |
| BCrypt (at.favre.lib) | 0.9.0 | `gradle/libs.versions.toml` |
| Coil (catalog) | 2.4.0 | `gradle/libs.versions.toml` |
| Coil (hardcoded duplicado) | 2.3.0 | `app/build.gradle.kts` línea 112 |
| Retrofit | 2.9.0 | Declarado en TOML/Gradle; **sin uso en código Kotlin** |
| OkHttp | 4.11.0 | Declarado; **sin uso en código** |
| Moshi | 1.15.0 | Declarado; **sin uso en código** |
| WorkManager | 2.9.0 | Declarado; **sin uso en código** |
| Timber | 5.0.1 | Declarado; **sin uso en código** |
| JUnit | 4.13.2 | `gradle/libs.versions.toml` |
| Espresso | 3.5.1 | `gradle/libs.versions.toml` |
| JVM target | 17 | `app/build.gradle.kts` |

---

## 3. Estructura del repositorio

Árbol resumido (relevante):

```
GanApp/
├── build.gradle.kts                 # plugins raíz (apply false)
├── settings.gradle.kts              # include(":app")
├── gradle.properties
├── gradlew / gradlew.bat
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/gradle-wrapper.properties
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   ├── schemas/com.proyecto.ganapp.data.local.db.AppDatabase/  # 1.json … 6.json
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/proyecto/ganapp/
│       │   │   ├── MainActivity.kt
│       │   │   ├── core/            # App, AppModule, notifications
│       │   │   ├── data/            # local (dao/db/entity), mapper, repository/impl
│       │   │   ├── di/              # Database, Repository, UseCase modules
│       │   │   ├── domain/          # model, repository, usecase
│       │   │   ├── ui/              # common/theme, features, navigation
│       │   │   └── util/
│       │   └── res/                 # drawable, mipmap-*, values, xml
│       ├── test/.../ExampleUnitTest.kt
│       └── androidTest/.../ExampleInstrumentedTest.kt
└── docs/                            # este informe
```

**Módulos Gradle:** únicamente `:app` (`settings.gradle.kts`).

---

## 4. Arquitectura actual

Patrón **realmente implementado**: Clean Architecture / capas **parcial**, con MVVM en la UI.

Capas presentes:

| Capa | Existe | Observación |
|---|---|---|
| UI (Compose + ViewModel) | Sí | Features: auth, home, animals, notifications |
| Domain (model + repository interfaces + use cases) | Sí | Incompleta / inconsistente |
| Data (Room + mappers + repo impl) | Sí | Persistencia 100 % local |
| DI (Hilt) | Sí | Mezcla `@Binds`, `@Provides` y `@Inject` |
| Red / sync remoto | No | Solo dependencias sin código |

Flujo típico observado:

1. Compose Screen → Hilt ViewModel  
2. ViewModel → UseCase **o** Repository (según feature; inconsistente)  
3. RepositoryImpl → DAO → Room (`ganapp_db`)  
4. Notificaciones de sistema: `NotificationsViewModel` → `NotificationScheduler` (AlarmManager) → `NotificationReceiver` → `NotificationHelper`

No hay capa de red, no hay DataStore/SharedPreferences de sesión, no hay WorkManager en runtime.

---

## 5. Inventario técnico

| Capa | Paquete | Archivo o clase | Responsabilidad | Dependencias |
|---|---|---|---|---|
| App | `com.proyecto.ganapp` | `MainActivity` | Entry point Compose; pide `POST_NOTIFICATIONS` | NavGraph, Hilt |
| Core | `core` | `App` | `@HiltAndroidApp` | Hilt |
| Core/DI | `core` | `AppModule` | Provide `NotificacionxAnimalRepository` | DAO, Impl |
| Core | `core.notifications` | `NotificationScheduler` | Programa AlarmManager | AlarmManager, Notificacion |
| Core | `core.notifications` | `NotificationReceiver` | Recibe broadcast y muestra notificación | NotificationHelper |
| Core | `core.notifications` | `NotificationHelper` | Canal + NotificationCompat | Context |
| Domain | `domain.model` | `Usuario`, `Animal`, `Notificacion` | Modelos de dominio | — |
| Domain | `domain.repository` | 4 interfaces | Contratos de acceso a datos | modelos / entity (NxA) |
| Domain | `domain.usecase.*` | 10 use cases | Orquestación de negocio (parcial) | repositorios |
| Data | `data.local.entity` | 4 entities | Tablas Room | Room |
| Data | `data.local.dao` | 4 DAOs | Queries CRUD / Flow | Room |
| Data | `data.local.db` | `AppDatabase`, `DateConverters` | BD v6 | Room |
| Data | `data.mapper` | 3 mappers | Entity ↔ Domain | — |
| Data | `data.repository.impl` | 4 impls | Implementan repos | DAOs, PasswordUtils |
| DI | `di` | `DatabaseModule` | Room + DAOs | Room |
| DI | `di` | `RepositoryModule` | Binds Animal/Usuario/Notificacion | Impls |
| DI | `di` | `UseCaseModule` | Provides varios use cases | Repos |
| UI | `ui.navigation` | `NavGraph`, `Screen` | Rutas Compose Navigation | Screens |
| UI | `ui.features.auth` | Login/Register + AuthViewModel | Auth | UsuarioRepository |
| UI | `ui.features.home` | HomeScreen + HomeViewModel | Menú principal | GetUsuarioByIdUseCase |
| UI | `ui.features.animals` | Animals/Register + AnimalsViewModel | CRUD parcial animales | AnimalRepository (+ use cases no usados) |
| UI | `ui.features.notifications` | 5 screens + 4 VMs | Listar/crear/asignar notificaciones | Use cases / repos / Scheduler |
| UI | `ui.common.theme` | Color, Theme, Type | Tema Material3 (plantilla) | Compose |
| Util | `util` | `PasswordUtils`, `DateUtils` | Hash BCrypt; fecha string | bcrypt |

---

## 6. Modelo de datos

### 6.1 Entidades de dominio

| Modelo | Archivo | Campos |
|---|---|---|
| `Usuario` | `domain/model/Usuario.kt` | idUsuario, nombre, apellido, correo, contrasena |
| `Animal` | `domain/model/Animal.kt` | idAnimal, nombre, edad, peso, color, idUsuario, fotoUri |
| `Notificacion` | `domain/model/Notificacion.kt` | idNotificacion, nombre, tipo, fechaInicio, fechaFin, seRepite, hora, dosisPorDia, intervaloHoras, idUsuario |

No existe modelo de dominio para la relación animal–notificación.

### 6.2 Entidades Room

| Entity | Tabla | PK | FKs / índices | Campos extra vs dominio |
|---|---|---|---|---|
| `UsuarioEntity` | `usuario` | idUsuario | — | fechaRegistro, activo |
| `AnimalEntity` | `animal` | idAnimal | FK idUsuario CASCADE; Index(idUsuario) | raza |
| `NotificacionEntity` | `notificacion` | idNotificacion | FK idUsuario CASCADE; Index(idUsuario) | descripcion, estado, fechaCreacion; fechaFin nullable |
| `NotificacionxAnimalEntity` | `notificacion_animal` | (idAnimal, idNotificacion) | FKs CASCADE + índices | Solo en data |

### 6.3 DAOs

- `UsuarioDao`: insert/update/delete/getById/getAll/getByCorreo  
- `AnimalDao`: insert/update/deleteById/getById/getByUser/getAnimalsByUser/getAll — **`getByUser` y `getAnimalsByUser` duplican la misma query**  
- `NotificacionDao`: insert/update/deleteById/getById/getAll/getActiveNotifications  
- `NotificacionxAnimalDao`: insert/delete/getNotificacionesByAnimal/getAnimalesByNotificacion  

### 6.4 Base de datos

- Clase: `AppDatabase` — versión **6**, `exportSchema = true`, nombre `ganapp_db`  
- Schemas exportados: `app/schemas/.../1.json` … `6.json`  
- Construcción: `DatabaseModule` con **`.fallbackToDestructiveMigration()`**  
- **No existen clases `Migration` en el código**  
- `DateConverters`: Long ↔ `java.util.Date` (registrado; entidades actuales no tipan campos como `Date`)

### 6.5 Relaciones conceptuales

```
Usuario 1 ─── * Animal
Usuario 1 ─── * Notificacion
Animal * ─── * Notificacion   (vía notificacion_animal)
```

---

## 7. Flujo de navegación

**Archivo:** `ui/navigation/NavGraph.kt`  
**startDestination:** `login`

| Ruta real | Argumentos | Pantalla |
|---|---|---|
| `login` | — | `LoginScreen` |
| `register` | — | `RegisterScreen` |
| `home/{idUsuario}` | idUsuario: Long | `HomeScreen` |
| `register_animal/{idUsuario}` | idUsuario | `RegisterAnimalScreen` |
| `animals/{userId}` | userId (**`!!`**) | `AnimalsScreen` |
| `assignNotification/{userId}/{animalId}` | userId, animalId | `AssignNotificationScreen` |
| `animalNotifications/{animalId}` | animalId (**`!!`**) | `AnimalNotificationsScreen` |
| `notifications/{userId}` | userId | `NotificationsScreen` |
| `register_notification/{userId}` | userId | `RegisterNotificationScreen` |

**Sealed class `Screen`:** solo define Login, Register, Home (`"home"` sin arg), Animals, Notifications. Varias rutas reales son strings literales no tipados. `Screen.Home.route` (`"home"`) **no coincide** con la ruta usada (`home/{idUsuario}`).

Flujo principal:

```
Login ──OK──► Home(idUsuario)
  │              ├── register_animal
  │              ├── animals ──► assignNotification / animalNotifications
  │              └── notifications ──► register_notification
  └── Register ──► Login
```

No hay deep links, ni grafo anidado, ni back stack tipado con type-safe navigation.

---

## 8. Manejo del estado

- **No se usa LiveData** en el código fuente inspeccionado.  
- Patrón dominante: `MutableStateFlow` / `StateFlow` en ViewModels + `collectAsState()` en Compose.  
- `NotificationsViewModel` también usa `MutableSharedFlow` para eventos (`SUCCESS`, `ERROR_NOMBRE`, `ERROR_FECHAS`).  
- Estado de formulario: a veces local (`remember { mutableStateOf }`) en Login/Register/RegisterAnimal; en notificaciones los campos viven como `MutableStateFlow` públicos en el ViewModel.  
- No hay UiState sellado unificado (Loading / Success / Error) por feature.  
- No hay manejo de loading visible en pantallas inspeccionadas.  
- Errores: login muestra mensaje si `loginResult == false`; registro no muestra error de persistencia; eventos `ERROR_*` de notificaciones **no se muestran** en UI (solo se observa `SUCCESS` en `RegisterNotificationScreen`).

---

## 9. Inyección de dependencias

- Application: `core/App.kt` (`@HiltAndroidApp`)  
- Activity: `MainActivity` (`@AndroidEntryPoint`)  
- ViewModels: `@HiltViewModel` + `@Inject constructor`  

Módulos:

| Módulo | Tipo | Provee |
|---|---|---|
| `di/DatabaseModule` | `@Provides` | AppDatabase, 4 DAOs |
| `di/RepositoryModule` | `@Binds` | Animal, Usuario, Notificacion repositories |
| `core/AppModule` | `@Provides` | NotificacionxAnimalRepository |
| `di/UseCaseModule` | `@Provides` | GetAnimals, InsertAnimal, Login, RegisterUser, GetNotifications, ScheduleNotification |

Use cases con constructor `@Inject` (sin entrada en UseCaseModule):  
`CreateNotificacionUseCase`, `AssignNotificationToAnimalUseCase`, `GetNotificationsOfAnimalUseCase`, `GetUsuarioByIdUseCase`.

**Inconsistencias:**

- `AuthViewModel` inyecta `UsuarioRepository` y **no** usa `LoginUseCase` / `RegisterUserUseCase` (sí providos en DI).  
- `AnimalsViewModel` inyecta use cases **y** repository; solo usa el repository.  
- `ScheduleNotificationUseCase` es stub vacío y se provee; el scheduling real está en `NotificationScheduler` llamado desde el ViewModel.  
- Hilt dependencies duplicadas en `app/build.gradle.kts` (líneas 68–69 y 76–77).

---

## 10. Interfaz y sistema visual

### Compose theme

- `ui/common/theme/Color.kt`: Purple/Pink plantilla Android Studio.  
- `ui/common/theme/Theme.kt`: `GanAppTheme` con dynamic color.  
- `ui/common/theme/Type.kt`: Typography mínima (`bodyLarge`).  
- **`GanAppTheme` no se aplica en `MainActivity`** (`setContent { NavGraph() }`).

### XML / recursos

- `themes.xml`: `Theme.GanApp` → `android:Theme.Material.Light.NoActionBar`  
- `colors.xml`: colores púrpura/teal de plantilla (lint: unused)  
- `strings.xml`: solo `app_name` = GanApp  
- Drawables PNG: `logo_app`, `logo_register`, `def_vaca/cerdo/gallina/oveja`  
- Iconos launcher mipmap + adaptive XML  
- Manifest icon: `@drawable/logo_app`

### UI real

Las pantallas usan colores hardcodeados (fondos verdes claros, acentos `#00E676` / `#00C853`), no el theme Compose ni `colors.xml`.  
No hay carpeta `ui/common` de componentes compartidos más allá de theme. Cards y selectores viven embebidos en cada feature.

### Componentes reutilizables

No hay librería de design system propia. Helpers locales: `AnimalCard`, `getAnimalImageRes`, `NotificationCard`, `TipoSelector`, `RepeatSelector`, `NotificationAssignCard`, `NotificationItem`.

---

## 11. Pruebas existentes

| Tipo | Archivo | Qué prueba |
|---|---|---|
| Unitaria | `app/src/test/.../ExampleUnitTest.kt` | `2 + 2 = 4` |
| Instrumentada | `app/src/androidTest/.../ExampleInstrumentedTest.kt` | packageName == `com.proyecto.ganapp` |

**No existen** pruebas de ViewModels, use cases, repositorios, Room, mappers, navegación ni UI Compose.

---

## 12. Resultado de compilación, pruebas y lint

Entorno de ejecución: `JAVA_HOME` no estaba definido en el shell; se usó el JBR de Android Studio (`C:\Program Files\Android\Android Studio\jbr`, OpenJDK 21.0.9) **solo para la sesión de comandos**, sin modificar archivos del proyecto.

### 12.1 `gradlew.bat assembleDebug`

| Campo | Valor |
|---|---|
| Comando | `.\gradlew.bat assembleDebug --no-daemon` |
| Resultado | **BUILD SUCCESSFUL** (exit 0), ~34 s |
| Error principal | Ninguno |
| Warnings relevantes en esta fase | Ninguno bloqueante |

### 12.2 `gradlew.bat test`

| Campo | Valor |
|---|---|
| Comando | `.\gradlew.bat test --no-daemon` |
| Resultado | **BUILD SUCCESSFUL** (exit 0), ~1 m 44 s |
| Tests | `testDebugUnitTest` y `testReleaseUnitTest` OK (plantilla) |
| Warning compilación | `HomeScreen.kt` líneas 78 y 93: `outlinedButtonBorder` deprecated |
| Warning kapt | Kapt no soporta language version 2.0+; fallback a 1.9 |

### 12.3 `gradlew.bat lint`

| Campo | Valor |
|---|---|
| Comando | `.\gradlew.bat lint --no-daemon` |
| Resultado | **BUILD SUCCESSFUL** (exit 0), ~2 m 25 s |
| Resumen | **0 errors, 52 warnings** |
| Informe | `app/build/reports/lint-results-debug.html` / `.txt` / `.xml` |

**Warnings lint destacados (no corregidos):**

| Severidad | Hallazgo | Evidencia |
|---|---|---|
| Warning | `targetSdk`/`compileSdk` 34 desactualizados | `app/build.gradle.kts` |
| Warning | Dependencias/AGP/Kotlin con versiones más nuevas disponibles | `libs.versions.toml`, `app/build.gradle.kts` |
| Warning | Coil hardcoded 2.3.0 + UseTomlInstead | `app/build.gradle.kts:111-112` |
| Warning | Room con kapt en vez de KSP | `app/build.gradle.kts:82` |
| Warning | DefaultLocale en format hora | `RegisterNotificationScreen.kt:68` |
| Warning | ObsoleteSdkInt (minSdk 26) | `NotificationHelper`, `NotificationScheduler` |
| Warning | UnusedResources (colores plantilla, ic_launcher mipmap) | `colors.xml`, mipmaps |
| Warning | IconLocation (PNG en drawable/) | logos y def_* |
| Warning | RedundantLabel en activity | `AndroidManifest.xml:21` |

### 12.4 Posibles errores de compilación por análisis estático

No se identificó un error de tipos/recursos que impida compilar: `assembleDebug` lo confirma. Riesgos son de **runtime/diseño**, no de fallo de build.

---

## 13. Problemas encontrados

| Prioridad | Problema | Evidencia | Impacto | Posible causa |
|---|---|---|---|---|
| Crítico | Migraciones Room destructivas: pérdida de datos al subir versión | `di/DatabaseModule.kt` (`fallbackToDestructiveMigration`); `AppDatabase` v6; sin clases Migration | Pérdida de usuarios/animales/notificaciones en dispositivos | Evolución de schema sin migraciones formales |
| Crítico | `idNotificacion` suele ser 0 al programar alarma tras insert | `CreateNotificacionUseCase` no retorna ID; `NotificationsViewModel.onSave` programa con el modelo original; `NotificationScheduler` usa `idNotificacion` o `currentTimeMillis` | requestCodes inestables, posibles colisiones/cancelaciones incorrectas | Insert no propaga el ID generado |
| Alto | Sin reprogramación de alarmas tras reinicio | Manifest sin `RECEIVE_BOOT_COMPLETED`; no hay BootReceiver | Recordatorios se pierden al reiniciar el dispositivo | Diseño AlarmManager incompleto |
| Alto | Navegación con `!!` en argumentos | `NavGraph.kt` rutas `animals/{userId}` y `animalNotifications/{animalId}` | Crash NPE si falta argumento | Parsing inseguro |
| Alto | Registro navega antes de confirmar persistencia | `RegisterScreen.kt`: llama `onRegisterSuccess()` tras `register()` y también `LaunchedEffect(usuario)` | Navegación prematura / doble navegación; usuario sin ID persistido en estado | Flujo async no esperado |
| Alto | Dominio acoplado a Room en relación NxA | `NotificacionxAnimalRepository` expone `NotificacionxAnimalEntity` | Rompe Clean Architecture; domain depende de data | Falta modelo de dominio / DTO |
| Alto | ViewModels saltan casos de uso / usan repos directos | `AuthViewModel`, `AnimalsViewModel`, `AssignNotificationViewModel` | Capas inconsistentes; lógica de negocio dispersa | Implementación incremental sin disciplina |
| Alto | `ScheduleNotificationUseCase` stub; scheduling en UI layer | `ScheduleNotificationUseCase.kt` vacío; `NotificationsViewModel` → `NotificationScheduler` | Lógica de sistema en presentación; use case muerto | Stub nunca completado |
| Alto | Sin sesión persistente; siempre start en login | `NavGraph` startDestination login; sin DataStore/Prefs | UX: re-login siempre; no hay logout real en UI | Feature no implementada |
| Medio | Desalineación dominio ↔ Room | mappers omiten raza, activo, descripcion, estado, etc. | Datos silenciosamente perdidos en round-trip | Modelos no sincronizados |
| Medio | Filtrado de notificaciones en memoria | `NotificacionRepositoryImpl.getAllNotifications` hace `getAll()` + filter | Ineficiencia / escala pobre | Query no filtrada por usuario en DAO |
| Medio | Validaciones incompletas / errores no mostrados | Login sin validar vacío; RegisterAnimal sin validar nombre; eventos ERROR_* ignorados en UI | Datos inválidos y mala UX | Validación parcial |
| Medio | Theme Compose no aplicado | `MainActivity` no usa `GanAppTheme` | Tema huérfano; UI inconsistente con Material3 theme | Olvido al cablear setContent |
| Medio | Dependencias muertas (Retrofit, WorkManager, Timber, Moshi, OkHttp) | `app/build.gradle.kts` + TOML vs ausencia en `.kt` | APK más pesado, ruido, confusión arquitectónica | Scaffold anticipado no usado |
| Medio | Coil duplicado (catalog 2.4.0 + hardcoded 2.3.0) | `app/build.gradle.kts` | Resolución ambigua / deuda Gradle | Copia residual |
| Medio | Opción UI “ANUAL” sin rama en scheduler | `RegisterNotificationScreen` RepeatSelector vs `NotificationScheduler.when` | Se trata como no-repetición | Falta case |
| Medio | `dosisPorDia` / `intervaloHoras` sin controles UI | Fields en VM; UI no los edita | Feature de dosis múltiples inaccesible | UI incompleta |
| Medio | `composeOptions.kotlinCompilerExtensionVersion = 1.6.0` con Kotlin 2.0 Compose plugin | `app/build.gradle.kts` | Configuración confusa / potencialmente obsoleta | Migración parcial a Compose Compiler plugin |
| Bajo | DAOs/métodos duplicados | `AnimalDao.getByUser` ≡ `getAnimalsByUser`; repo similar | Mantenimiento confuso | Refactor incompleto |
| Bajo | Imports no usados en NavGraph | `padding`, `Text`, `Modifier`, `dp` | Ruido | Limpieza pendiente |
| Bajo | Colores/recursos plantilla sin uso | lint UnusedResources | Basura en APK | Template AS |
| Bajo | Pruebas solo de ejemplo | ExampleUnit/InstrumentedTest | Sin red de seguridad | No se escribieron tests de negocio |
| Bajo | kapt + Kotlin 2.0 | log de `test`/`compile` | Builds más lentos; warning | No migrado a KSP |

---

## 14. Deuda técnica

1. **Arquitectura inconsistente:** mezcla de use cases reales, use cases stub, y acceso directo a repositorios desde ViewModels.  
2. **DI fragmentada:** `AppModule` en `core` vs módulos en `di`; `@Provides` y `@Inject` mezclados para use cases.  
3. **Schema Room v6 sin migraciones** pese a schemas exportados 1–6.  
4. **Catálogo de versiones vs dependencias hardcodeadas** (icons-extended, coil 2.3.0).  
5. **UI sin design system:** colores literales, sin strings externalizados, theme Compose muerto.  
6. **Navegación híbrida** (sealed class incompleta + strings).  
7. **Notificaciones de sistema frágiles:** sin boot, IDs inestables, `setRepeating` deprecated/inexacto.  
8. **Cobertura de tests ~0 %** sobre lógica real.  
9. **Dependencias de red/logging/work** sin código consumidor.  
10. **Seguridad/sesión:** BCrypt sí existe para passwords; no hay control de sesión, ni unicidad de correo enforced en UI (sí consulta por correo en DAO; no hay índice UNIQUE visible en entity — **no se verificó constraint UNIQUE en schemas JSON en detalle**).

---

## 15. Archivos aparentemente incompletos o sin uso

| Elemento | Evidencia de “sin uso / incompleto” | Nota |
|---|---|---|
| `ScheduleNotificationUseCase.kt` | Cuerpo vacío; no llamado desde VMs | Stub explícito |
| `LoginUseCase` / `RegisterUserUseCase` | Providos en `UseCaseModule`; Auth usa repository | Aparentemente no consumidos por UI |
| `GetAnimalsUseCase` / `InsertAnimalUseCase` | Inyectados en `AnimalsViewModel` pero no invocados | Código muerto en runtime del VM |
| `GanAppTheme` | No referenciado desde `MainActivity` | Tema huérfano |
| `AuthViewModel.logout` | Sin callers en pantallas | Método sin uso UI |
| `DateConverters` | Registrado; entidades usan Long/String | Posiblemente residual |
| `NotificacionDao.getActiveNotifications` | No se encontró uso en repos/VMs en inspección | Posible sin uso |
| Métodos update/delete animal/notificación en repos | Sin pantallas de edición/borrado | API data sin UI |
| Dependencias Retrofit/OkHttp/Moshi/WorkManager/Timber | Solo Gradle | Sin clases consumidoras |
| `Screen.Home` route `"home"` | No usada como ruta real | Desalineación |

*“Aparentemente sin uso” se basa en búsqueda estática en el código fuente; no se usó análisis de reachability de bytecode completo.*

---

## 16. Información que no pudo verificarse

- Comportamiento en dispositivo físico real (permisos denegados, Doze, OEM alarm limits).  
- Si `kotlinCompilerExtensionVersion = 1.6.0` es efectivamente ignorado por el Compose Compiler Gradle Plugin 2.0 (el build pasó, pero no se inspeccionó el classpath del compiler).  
- Contenido detallado de cada schema JSON 1–6 (diferencias campo a campo entre versiones).  
- Si existe índice UNIQUE en correo de usuario a nivel SQL (no declarado en la entity Kotlin; schemas no se auditan campo a campo aquí).  
- Pruebas instrumentadas en emulador/dispositivo (solo se ejecutó `test` unitario JVM; `lint` y `assembleDebug` sí). El comando `test` de Gradle **no** ejecuta `androidTest` por defecto.  
- Uso en runtime de recursos mipmap tras packaging (lint los marca unused; el roundIcon del manifest sí referencia `ic_launcher_round`).  
- Calidad visual/UX en distintas densidades.  
- Historial git / intención original del producto más allá del código.

---

## 17. Recomendaciones iniciales

Recomendaciones **generales** (sin implementar en esta fase):

1. Definir y documentar el contrato de capas (UI → UseCase → Repository → Data) y eliminar accesos directos a repos desde ViewModels donde ya existan use cases.  
2. Completar o eliminar stubs (`ScheduleNotificationUseCase`) y unificar el scheduling de notificaciones detrás de un caso de uso / servicio de dominio.  
3. Introducir migraciones Room reales y retirar `fallbackToDestructiveMigration` para builds de producción.  
4. Devolver IDs generados en inserts (especialmente notificaciones) antes de programar alarmas.  
5. Añadir `BootReceiver` (y permisos asociados) para reprogramar alarmas.  
6. Aplicar `GanAppTheme`, centralizar colores/tipografía y externalizar strings.  
7. Tipar navegación (routes + args seguros) y eliminar `!!`.  
8. Introducir UiState con loading/error por feature y validaciones homogéneas.  
9. Limpiar dependencias no usadas y unificar Coil/icons en el version catalog.  
10. Migrar kapt → KSP (Room/Hilt según compatibilidad) cuando se planifique mantenimiento de build.  
11. Añadir tests unitarios de use cases, repositorios (con Room in-memory) y ViewModels.  
12. Decidir si habrá backend: si no, retirar Retrofit/Moshi/OkHttp; si sí, diseñar capa remote.  
13. Persistir sesión de usuario (DataStore) y exponer logout en UI.  
14. Alinear modelos de dominio con campos Room que se quieran conservar (o eliminar campos muertos).

---

## 18. Lista completa de archivos inspeccionados

### Configuración / raíz

- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle.properties`
- `gradle/libs.versions.toml`
- `gradle/wrapper/gradle-wrapper.properties`
- `.gitignore`
- `app/build.gradle.kts`
- `app/proguard-rules.pro` (existencia; no auditoría línea a línea de reglas)
- `app/src/main/AndroidManifest.xml`

### Schemas Room

- `app/schemas/com.proyecto.ganapp.data.local.db.AppDatabase/1.json` … `6.json` (existencia y versiones; no diff campo a campo)

### Código Kotlin (`app/src/main/java`) — 64 archivos

- `MainActivity.kt`
- `core/App.kt`
- `core/AppModule.kt`
- `core/notifications/NotificationHelper.kt`
- `core/notifications/NotificationReceiver.kt`
- `core/notifications/NotificationScheduler.kt`
- `data/local/dao/AnimalDao.kt`
- `data/local/dao/NotificacionDao.kt`
- `data/local/dao/NotificacionxAnimalDao.kt`
- `data/local/dao/UsuarioDao.kt`
- `data/local/db/AppDatabase.kt`
- `data/local/db/DateConverters.kt`
- `data/local/entity/AnimalEntity.kt`
- `data/local/entity/NotificacionEntity.kt`
- `data/local/entity/NotificacionxAnimalEntity.kt`
- `data/local/entity/UsuarioEntity.kt`
- `data/mapper/AnimalMapper.kt`
- `data/mapper/NotificacionMapper.kt`
- `data/mapper/UsuarioMapper.kt`
- `data/repository/impl/AnimalRepositoryImpl.kt`
- `data/repository/impl/NotificacionRepositoryImpl.kt`
- `data/repository/impl/NotificacionxAnimalRepositoryImpl.kt`
- `data/repository/impl/UsuarioRepositoryImpl.kt`
- `di/DatabaseModule.kt`
- `di/RepositoryModule.kt`
- `di/UseCaseModule.kt`
- `domain/model/Animal.kt`
- `domain/model/Notificacion.kt`
- `domain/model/Usuario.kt`
- `domain/repository/AnimalRepository.kt`
- `domain/repository/NotificacionRepository.kt`
- `domain/repository/NotificacionxAnimalRepository.kt`
- `domain/repository/UsuarioRepository.kt`
- `domain/usecase/animal/GetAnimalsUseCase.kt`
- `domain/usecase/animal/InsertAnimalUseCase.kt`
- `domain/usecase/notificacion/AssignNotificationToAnimalUseCase.kt`
- `domain/usecase/notificacion/CreateNotificacionUseCase.kt`
- `domain/usecase/notificacion/GetNotificationsOfAnimalUseCase.kt`
- `domain/usecase/notificacion/GetNotificationsUseCase.kt`
- `domain/usecase/notificacion/ScheduleNotificationUseCase.kt`
- `domain/usecase/usuario/GetUsuarioByIdUseCase.kt`
- `domain/usecase/usuario/LoginUseCase.kt`
- `domain/usecase/usuario/RegisterUserUseCase.kt`
- `ui/common/theme/Color.kt`
- `ui/common/theme/Theme.kt`
- `ui/common/theme/Type.kt`
- `ui/features/animals/AnimalsScreen.kt`
- `ui/features/animals/AnimalsViewModel.kt`
- `ui/features/animals/RegisterAnimalScreen.kt`
- `ui/features/auth/AuthViewModel.kt`
- `ui/features/auth/LoginScreen.kt`
- `ui/features/auth/RegisterScreen.kt`
- `ui/features/home/HomeScreen.kt`
- `ui/features/home/HomeViewModel.kt`
- `ui/features/notifications/AnimalNotificationsScreen.kt`
- `ui/features/notifications/AnimalNotificationsViewModel.kt`
- `ui/features/notifications/AssignNotificationScreen.kt`
- `ui/features/notifications/AssignNotificationViewModel.kt`
- `ui/features/notifications/NotificationsScreen.kt`
- `ui/features/notifications/NotificationsViewModel.kt`
- `ui/features/notifications/RegisterNotificationScreen.kt`
- `ui/navigation/NavGraph.kt`
- `util/DateUtils.kt`
- `util/PasswordUtils.kt`

### Recursos

- `res/values/strings.xml`
- `res/values/colors.xml`
- `res/values/themes.xml`
- `res/xml/backup_rules.xml`
- `res/xml/data_extraction_rules.xml`
- `res/drawable/ic_launcher_background.xml`
- `res/drawable/ic_launcher_foreground.xml`
- `res/drawable/logo_app.png`
- `res/drawable/logo_register.png`
- `res/drawable/def_vaca.png`
- `res/drawable/def_cerdo.png`
- `res/drawable/def_gallina.png`
- `res/drawable/def_oveja.png`
- `res/mipmap-anydpi/ic_launcher.xml`
- `res/mipmap-anydpi/ic_launcher_round.xml`
- `res/mipmap-{h,m,x,xx,xxx}dpi/ic_launcher.webp` y `ic_launcher_round.webp`

### Pruebas

- `app/src/test/java/com/proyecto/ganapp/ExampleUnitTest.kt`
- `app/src/androidTest/java/com/proyecto/ganapp/ExampleInstrumentedTest.kt`

### Informes de validación generados por Gradle (lectura)

- `app/build/reports/lint-results-debug.txt`
- `app/build/reports/lint-results-debug.html` (existencia)
- `app/build/reports/lint-results-debug.xml` (existencia)

---

*Fin del diagnóstico técnico inicial. Ningún archivo fuente existente fue modificado en esta fase.*
