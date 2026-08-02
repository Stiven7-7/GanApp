# Inspección previa de la Subfase 1.2

**Fecha:** 2026-08-02  
**Alcance:** Solo lectura + builds Gradle. Único archivo creado: este informe.  
**Lint:** no ejecutado (build y tests OK; no fue necesario).

---

## 1. Estado Git

| Campo | Valor |
|---|---|
| Raíz | `C:/Users/Administrator/StudioProjects/GanApp` |
| Rama activa | `feature/auth-session-foundation` |
| HEAD completo | `67c27f6009b79ac56a68d46abd1498d3361c9870` |
| HEAD corto | `67c27f6` |
| Último commit | `chore: establish project baseline and repository hygiene` (AuthorDate Thu Jul 30 21:40:02 2026 -0500) |
| Upstream | `origin/feature/auth-session-foundation` |
| Ahead/behind | `0	0` (sin fetch) |
| Remoto | `origin` → `https://github.com/Stiven7-7/GanApp.git` |

### Condición de inicio — **NO CUMPLIDA**

`git status --short` al inicio:

```
 M .idea/misc.xml
?? docs/auth-current-state-audit.md
```

- No se limpiaron ni modificaron esos archivos.
- Se continuó solo con lectura, builds (artefactos `build/`) y la creación de este informe autorizado.
- **Hecho:** el HEAD inspeccionado sigue siendo el commit de higiene de Fase 0; no hay commits posteriores de Subfase 1.1 en esta rama (si 1.1 se aprobó en documento, no aparece como cambio de código en `HEAD`).

---

## 2. Build base

`JAVA_HOME` temporal de sesión: `C:\Program Files\Android\Android Studio\jbr` (OpenJDK 21.0.9). Sin cambios permanentes.

| Comando | Inicio | Fin | Duración | Exit | Resultado |
|---|---|---|---|---|---|
| `.\gradlew.bat assembleDebug --no-daemon` | 2026-08-02 18:35:26 -05:00 | 18:35:41 | **14.3 s** | 0 | **SUCCESS** (43 tasks UP-TO-DATE) |
| `.\gradlew.bat test --no-daemon` | 2026-08-02 18:35:41 -05:00 | 18:35:57 | **16.7 s** | 0 | **SUCCESS** (`testDebugUnitTest` / `testReleaseUnitTest` UP-TO-DATE) |

- Errores: ninguno.  
- Advertencias relevantes a auth: ninguna en estas corridas.  
- Reportes de test: `app/build/reports/tests/` (artefacto Gradle).  
- Lint: **no ejecutado**.

---

## 3. Firmas actuales de casos de uso

### `LoginUseCase`

| Campo | Valor |
|---|---|
| Ruta | `app/src/main/java/com/proyecto/ganapp/domain/usecase/usuario/LoginUseCase.kt` |
| Paquete | `com.proyecto.ganapp.domain.usecase.usuario` |
| Constructor | `LoginUseCase(private val repository: UsuarioRepository)` — **sin** `@Inject` |
| Firma pública | `suspend operator fun invoke(correo: String, contrasena: String): Usuario?` |
| Normalización / validación / try-catch | **Ninguna** — delega a `repository.login` |
| Hilt | `@Provides` + `@Singleton` en `di/UseCaseModule.kt` → `provideLoginUseCase` |
| Consumo real | **Ninguno.** `AuthViewModel` no lo inyecta ni llama. |
| Búsqueda `LoginUseCase` | Declaración + UseCaseModule. Sin inyección en ViewModels. |

### `RegisterUserUseCase`

| Campo | Valor |
|---|---|
| Ruta | `.../domain/usecase/usuario/RegisterUserUseCase.kt` |
| Constructor | `RegisterUserUseCase(private val repository: UsuarioRepository)` — **sin** `@Inject` |
| Firma pública | `suspend operator fun invoke(usuario: Usuario): Long` |
| Normalización / validación / try-catch | **Ninguna** — delega a `repository.register` |
| Hilt | `provideRegisterUserUseCase` en `UseCaseModule` |
| Consumo real | **Ninguno.** Auth usa el repositorio directo. |

**Riesgo de cambiar firmas:** bajo para consumidores actuales (cero), pero alto para el contrato futuro de 1.2: cambiar `Usuario?` / `Long` a resultados tipados obliga a adaptar AuthViewModel y, si se mantiene compatibilidad UI 1.2, a mapear tipados → `usuario` / `loginResult` existentes.

---

## 4. Estado actual de AuthViewModel

| Campo | Valor |
|---|---|
| Ruta | `app/src/main/java/com/proyecto/ganapp/ui/features/auth/AuthViewModel.kt` |
| DI | `@HiltViewModel` + `@Inject constructor(usuarioRepository: UsuarioRepository)` |
| Acceso directo a repo | **Sí** (`register` / `login`) |
| Estados | `_usuario` / `usuario: StateFlow<Usuario?>`; `_loginResult` / `loginResult: StateFlow<Boolean?>` |
| Métodos | `register(Usuario)`, `login(correo, contrasena)`, `logout()` |
| Scope | `viewModelScope.launch` — **sin** Dispatcher explícito |
| Excepciones / loading | **Ausentes** |
| Datos sensibles | `Usuario` con campo `contrasena` (hash tras login vía `toDomain`) en StateFlow |

### Conteos de consumidores (efectivos)

| Elemento | Consumidores | Detalle |
|---|---|---|
| `AuthViewModel.usuario` | **2** | `LoginScreen.kt` (`collectAsState` + `usuario?.idUsuario` en `LaunchedEffect`); `RegisterScreen.kt` (`collectAsState` + `LaunchedEffect` navega si no null) |
| `AuthViewModel.loginResult` | **1 archivo / 3 usos** | Solo `LoginScreen.kt`: observe, `LaunchedEffect(true)`, mensaje si `false` |
| `register()` | **1** | `RegisterScreen.kt` onClick |
| `login()` | **1** | `LoginScreen.kt` onClick |
| `logout()` | **0** | Solo declaración en ViewModel |

### Instancias

`LoginScreen` y `RegisterScreen` usan `hiltViewModel()` en **destinos NavHost distintos** → **instancias separadas** por `NavBackStackEntry` (no comparten estado entre pantallas).

### Riesgos observados (hechos)

- Registro: ignora `Long` del repo; asigna el `Usuario` de entrada (`idUsuario=0`).  
- Doble navegación potencial en Register (`onRegisterSuccess()` inmediato + `LaunchedEffect(usuario)`).  
- Sin prevención de solicitudes simultáneas / doble clic.  
- `loginResult` residual puede re-disparar efectos si el mismo ViewModel sobrevive.

---

## 5. Contrato actual de UsuarioRepository

**Interfaz:** `domain/repository/UsuarioRepository.kt`  
**Impl:** `data/repository/impl/UsuarioRepositoryImpl.kt` (`@Inject`)  
**Bind:** `di/RepositoryModule.kt` `@Binds` `@Singleton`

| Función | Firma | Impl / DAO | Consumidores app | Uso |
|---|---|---|---|---|
| `register` | `suspend (Usuario): Long` | Hash BCrypt → `UsuarioEntity` manual → `dao.insert` | AuthViewModel; RegisterUserUseCase (sin callers UI) | Auth |
| `login` | `suspend (correo, contrasena): Usuario?` | `getByCorreo` + `PasswordUtils.verify` + `toDomain` | AuthViewModel; LoginUseCase (sin callers UI) | Auth |
| `getUserById` | `suspend (Long): Usuario?` | `dao.getById` + `toDomain` | `GetUsuarioByIdUseCase` → **HomeViewModel** | Home |
| `getUsuarioByCorreo` | `suspend (String): Usuario?` | `dao.getByCorreo` + `toDomain` | **Ningún caller** fuera de interfaz/impl | Sin uso |

### Consumidores reales de `UsuarioRepository` (inyección / uso de la abstracción)

1. `AuthViewModel`  
2. `LoginUseCase`  
3. `RegisterUserUseCase`  
4. `GetUsuarioByIdUseCase`  

Más wiring: `RepositoryModule`, `UseCaseModule`, `UsuarioRepositoryImpl`.  
**Total consumidores de aplicación: 4 archivos.**

### Implicación 1.2

- **Conservar temporalmente** firmas `register` / `login` / `getUserById` si Home y use cases thin siguen usándolas.  
- Cambiar retorno de `login` a resultado tipado **rompe** LoginUseCase y AuthViewModel (aceptable dentro de 1.2 si se adaptan juntos).  
- `getUserById` → Home: **no cambiar** semántica ni quitar hash del modelo `Usuario` sin adaptar Home o introducir modelo sin hash solo en auth.  
- Animales / notificaciones: **0** consumidores de `UsuarioRepository` (solo `idUsuario` Long por navegación).

---

## 6. Referencias del modelo Usuario

**Definición:** `domain/model/Usuario.kt`

```
idUsuario: Long = 0
nombre: String
apellido: String
correo: String
contrasena: String   // sin default
```

- **Sin** campo `activo` en dominio (solo en entity).  
- **`Usuario.copy(...)`:** búsqueda sin resultados de uso sobre el data class (solo `Color.copy` / `BorderStroke.copy` en UI).  
- **Constructores `Usuario(`:** `Usuario.kt` (clase), `UsuarioMapper.toDomain`, `RegisterScreen` (nuevoUsuario).

### Llegada a presentación

| Destino | Cómo | Qué usa de Usuario |
|---|---|---|
| LoginScreen | `AuthViewModel.usuario` | `idUsuario` para navegar |
| RegisterScreen | `AuthViewModel.usuario` | solo null-check para navegar |
| HomeScreen | `HomeViewModel.usuario` vía `GetUsuarioByIdUseCase` | **solo `nombre`** |

Animales/notificaciones: **no** dependen del tipo `Usuario`.

### Nombre recomendado para modelo sin hash (delta mínimo)

Convención actual: español (`Usuario`, `Animal`, `Notificacion`).  
**Recomendación:** `UsuarioSesion` o, si se prefiere inglés alineado a DataStore futuro `authenticatedUserId`: **`AuthenticatedUser`**.  
`UserSummary` es menos claro. `SessionUser` es aceptable.  
**Propuesta mínima:** introducir `AuthenticatedUser` (id, nombre, apellido, correo) **solo** en capa auth/session; **mantener** `Usuario` temporalmente para Home/`getUserById` hasta una subfase que limpie exposición de hash — o hacer que `getUserById` mapee a modelo sin hash y adaptar Home (1 archivo) en 1.2 si se quiere cortar exposición ya.

---

## 7. Exposición actual de contraseña o hash

### Referencias a `Usuario.contrasena` (campo del modelo de dominio)

Conteo exacto de puntos que leen/escriben el **campo del data class `Usuario`**:

| # | Archivo:línea | Tipo | Categoría |
|---|---|---|---|
| 1 | `domain/model/Usuario.kt:8` | Declaración | Modelo |
| 2 | `ui/features/auth/RegisterScreen.kt:154` | Escritura en ctor | Construcción UI (plain) |
| 3 | `data/repository/impl/UsuarioRepositoryImpl.kt:18` | Lectura `usuario.contrasena` | Persistencia / hash |
| 4 | `data/mapper/UsuarioMapper.kt:11` | Escritura en `toDomain` | Mapeo (hash → dominio) |
| 5 | `data/mapper/UsuarioMapper.kt:19` | Lectura en `toEntity` | Mapeo (no usado por register actual) |

**Total referencias dominio `Usuario.contrasena`: 5** (1 declaración + 4 usos).

### Otros `contrasena` (no cuentan como `Usuario.contrasena` pero relevantes)

- Parámetros plain en `login` (VM, UseCase, Repo interface/impl).  
- `UsuarioEntity.contrasena` (almacena hash).  
- Estado local Compose `var contrasena` en Login/Register.

### Flujo de sensibilidad

1. Plain en UI (`remember`) → `Usuario.contrasena` en registro o parámetro login.  
2. Hash en `UsuarioRepositoryImpl.register` antes de Room.  
3. Tras login/getById, `toDomain` pone **hash** en `Usuario.contrasena` → StateFlows de Auth y Home.

**Hecho:** el hash llega a presentación vía StateFlow aunque Home solo muestra `nombre`.

---

## 8. Flujo actual de registro

```
RegisterScreen (remember fields)
  → isNotBlank ×4
  → Usuario(..., contrasena=plain, id=0)
  → AuthViewModel.register → repo.register (hash + insert) [Long descartado]
  → _usuario = input Usuario (id=0, plain aún en objeto de entrada)
  → onRegisterSuccess() inmediato → navigate("login")
  → LaunchedEffect(usuario!=null) → onRegisterSuccess() otra vez
```

Validación autoritativa en use case: **ausente**. Normalización: **ausente**.

---

## 9. Flujo actual de login

```
LoginScreen → AuthViewModel.login(plain)
  → dao.getByCorreo (igualdad exacta)
  → PasswordUtils.verify
  → toDomain() (Usuario con hash) o null
  → loginResult true/false + usuario
  → LaunchedEffect → navigate("home/$idUsuario")
```

Mensaje único si `loginResult == false`. Sin distinguir causas.

---

## 10. Configuración Hilt

| Binding | Mecanismo | Scope |
|---|---|---|
| `UsuarioRepository` ← `UsuarioRepositoryImpl` | `@Binds` RepositoryModule | Singleton |
| `LoginUseCase` | `@Provides` UseCaseModule | Singleton |
| `RegisterUserUseCase` | `@Provides` UseCaseModule | Singleton |
| `UsuarioDao` / `AppDatabase` | `@Provides` DatabaseModule | DB Singleton |
| `GetUsuarioByIdUseCase` | `@Inject constructor` | — |
| `AuthViewModel` | `@HiltViewModel` `@Inject` | ViewModel |

**Hechos:** use cases de auth se proveen pero no se inyectan en AuthViewModel → provisiones actualmente **efectivamente ociosas** para UI.  
**Cambio mínimo 1.2:** AuthViewModel pasa a depender de `LoginUseCase` + `RegisterUserUseCase` (y opcionalmente deja de inyectar el repo). Si se añade `@Inject constructor` a los use cases, **evitar duplicar** con `@Provides` (riesgo de binding duplicado). Preferible: mantener `@Provides` actuales o migrar a `@Inject` y eliminar provides — no ambos.

---

## 11. Integración con pantallas

### LoginScreen

- Firma: `(onLoginSuccess: (Long) -> Unit, onNavigateToRegister, viewModel = hiltViewModel())`  
- Observa `loginResult` y `usuario`.  
- `remember` para correo/contraseña — **no** `rememberSaveable` / SavedStateHandle.  
- Sin loading / anti doble-clic.  
- Impacto 1.2: bajo si se mantienen `loginResult: Boolean?` y navegación por `idUsuario`.  
- Posponer a 1.4: rediseño UI, confirmación password, UiState rico.

### RegisterScreen

- Observa `usuario`; valida `isNotBlank`; construye `Usuario` con plain.  
- Doble navegación potencial.  
- Impacto 1.2: si register deja de setear `usuario` o cambia semántica, hay que adaptar el `LaunchedEffect` **o** mantener adaptador temporal que setee un flag/`usuario` no nulo tras éxito tipado.  
- Posponer a 1.4: eliminar navegación prematura/doble de forma definitiva en UI.

---

## 12. Integración con navegación

Archivo: `ui/navigation/NavGraph.kt`

| Ruta | Args | Notas |
|---|---|---|
| `login` | — | `startDestination` |
| `register` | — | éxito → `navigate("login")` sin `popUpTo` / `launchSingleTop` |
| `home/{idUsuario}` | String→Long, default 0L | Login → `home/$id` sin clear stack |

**Preservar en 1.2:** rutas y paso de `Long idUsuario` a Home.  
**Posponer:** 1.4 eventos UI; 1.5 DataStore; 1.6 navegación raíz / restauración / logout definitivo.  
Animales/notificaciones: reciben `idUsuario` desde Home; **no tocar** en 1.2.

---

## 13. Persistencia de usuario inspeccionada

| Elemento | Estado confirmado |
|---|---|
| Entity campos | idUsuario PK autoGenerate, nombre, apellido, correo, contrasena, fechaRegistro, activo |
| Índice UNIQUE correo | **No** (entity + schema 6 `indices: []`) |
| Insert | `suspend fun insert(...): Long` + `OnConflictStrategy.REPLACE` (por PK) |
| getByCorreo | `WHERE correo = :correo LIMIT 1` — igualdad exacta (case-sensitive típico) |
| BCrypt | `PasswordUtils` cost **12**; hash en register; verify en login |
| Mapper | `toDomain` incluye hash; `toEntity` no usado por register actual |
| Room version / DB | **6** / `ganapp_db` |
| fallbackToDestructiveMigration | **Sí** en DatabaseModule |
| Schema 6 vs entity | Campos alineados; sin unique en correo |

**1.2 no debe** tocar Entity/DAO/AppDatabase/schemas/migraciones.

---

## 14. Pruebas y dependencias disponibles

| Ubicación | Contenido |
|---|---|
| `app/src/test/.../ExampleUnitTest.kt` | Plantilla `2+2` |
| `app/src/androidTest/.../ExampleInstrumentedTest.kt` | packageName |
| Tests auth | **Ninguno** |

| Dependencia | ¿Disponible? | Notas |
|---|---|---|
| JUnit 4.13.2 | Sí (`testImplementation`) | Suficiente para use cases con fakes |
| AndroidX JUnit / Espresso | Sí (androidTest) | No imprescindible 1.2 |
| Compose UI test (toml) | En catálogo, **no** en `dependencies` de app | Posponible |
| kotlinx-coroutines-test | **No** | Útil para ViewModel; no imprescindible si 1.2 prueba use cases síncronos/suspend con runBlocking |
| MockK / Mockito / Turbine / Truth | **No** | Alternativa: fakes manuales de `UsuarioRepository` |
| Room testing / Hilt testing | **No** | Posponible (fuera de use case puro) |

**Para primera implementación 1.2:** JUnit + fake de `UsuarioRepository` (**necesaria** en práctica; **sin** dependencia nueva).  
**Útil no imprescindible:** `coroutines-test`.  
**Posponible:** MockK, Turbine, Hilt/Room/Compose testing.

---

## 15. Riesgos de compatibilidad

| Riesgo | Severidad | Mitigación 1.2 |
|---|---|---|
| Working tree sucio previo | Proceso | No tocar `.idea` ni audit previo; no mezclar commits |
| AuthViewModel deja de exponer `Usuario`/`loginResult` | Alto UI | Adaptador temporal: mapear resultados tipados → estados actuales |
| `getUserById` sigue devolviendo hash en `Usuario` | Medio | No cambiar Home; o mapear a modelo sin hash solo en auth |
| Doble binding Hilt use cases | Medio | Un solo mecanismo Provide o Inject |
| Cambiar firma repo `login` | Medio | Adaptar LoginUseCase + AuthVM juntos; no tocar getUserById |
| Compilación Home | Bajo si no se toca `Usuario`/`GetUsuarioByIdUseCase` | No modificar Home |
| Animales/notificaciones | Nulo si no se tocan rutas | Fuera de alcance |
| Exposición passwordHash | Alto residual | Nuevo modelo auth sin hash; no pasar plain ni hash a StateFlow UI |

---

## 16. Delta mínimo recomendado

### Incluir en 1.2

1. Enriquecer `LoginUseCase` / `RegisterUserUseCase`: normalización (trim/lowercase correo), validaciones autoritativas, resultados tipados (éxito / error de validación / credenciales / etc.).  
2. Modelo sin hash para auth (p. ej. `AuthenticatedUser`) + mapeo desde entity/repo **sin** exponer hash a VM/UI.  
3. `AuthViewModel` depende de use cases (no del repo).  
4. try/catch controlado en use cases o VM; mapear a estados actuales `loginResult`/`usuario` **o** equivalentes mínimos compatibles.  
5. Tests unitarios de use cases con fake repo (JUnit).  
6. Propagar ID real de registro al estado de éxito (sin navegar a Home aún).

### Excluir explícitamente (confirmado)

Índice UNIQUE Room, migraciones, DataStore, Save/Observe session funcionales, navegación raíz, logout definitivo, rediseño Login/Register, confirmación visual password, animales, notificaciones.

### Adaptadores temporales

- Mantener `StateFlow<Boolean?>` loginResult y un estado “éxito registro” observable por RegisterScreen **sin** reescribir pantallas en profundidad.  
- Evitar romper `LaunchedEffect` de Register: o bien seguir emitiendo un usuario/sesión no nulo sin hash, o emitir un evento booleano que la UI actual aún no consume (entonces haría falta micro-ajuste en Register — preferible posponer a 1.4 y en 1.2 solo asegurar que `_usuario` post-register use modelo sin plain/hash y con id real si se mantiene el LaunchedEffect).

---

## 17. Archivos que se modificarían

| Archivo | Cambio esperado 1.2 |
|---|---|
| `domain/usecase/usuario/LoginUseCase.kt` | Validación, normalización, resultado tipado |
| `domain/usecase/usuario/RegisterUserUseCase.kt` | Idem |
| `ui/features/auth/AuthViewModel.kt` | Inyectar use cases; mapear resultados |
| `di/UseCaseModule.kt` | Solo si cambian constructores / se evita duplicar @Inject |
| Posible nuevo: `domain/model/AuthenticatedUser.kt` (o nombre elegido) | Modelo sin hash |
| Posible nuevo: resultados tipados (`LoginResult` / `RegisterResult`) | Contratos |
| Posible nuevo: tests bajo `app/src/test/.../usecase/usuario/` | Pruebas |
| Posible ajuste mínimo `UsuarioRepository` / Impl | Solo si hace falta API que no devuelva hash a auth (p. ej. método login tipado) — preferible encapsular en use case mapeando entity vía repo existente |

---

## 18. Archivos que no deben tocarse

- `UsuarioEntity.kt`, `UsuarioDao.kt`, `AppDatabase.kt`, `DatabaseModule.kt`, schemas `1.json`–`6.json`  
- `NavGraph.kt` (salvo fuerza mayor de compilación; objetivo: no)  
- `LoginScreen.kt` / `RegisterScreen.kt` en profundidad (solo si adaptador lo exige al mínimo)  
- Animales, notificaciones, sus ViewModels/repos  
- `HomeScreen` / `HomeViewModel` / `GetUsuarioByIdUseCase` si se mantiene `Usuario` para Home  
- Gradle / dependencias (salvo decisión explícita posterior fuera de esta inspección)

---

## 19. Orden de implementación sugerido

1. Definir contratos tipados + `AuthenticatedUser` (sin tocar Room).  
2. Compilar.  
3. Implementar lógica en `RegisterUserUseCase` + tests fake.  
4. Compilar + `test`.  
5. Implementar lógica en `LoginUseCase` + tests.  
6. Compilar + `test`.  
7. Adaptar `AuthViewModel` a use cases; mapear a estados UI actuales; dejar de poner plain/hash en StateFlow.  
8. Compilar + `test`.  
9. Verificar manualmente login/registro sin cambiar navegación.  
10. No abrir PR de Room/DataStore en este mismo paso.

Puntos intermedios de compilación: tras modelos/resultados; tras cada use case; tras ViewModel.

---

## 20. Información pendiente

- Contenido exacto de la Subfase 1.1 “aprobada” vs código en `HEAD` (no hay diff de auth respecto al baseline `67c27f6` en el árbol inspeccionado).  
- Decisión de producto sobre si Home debe dejar de recibir hash en 1.2 o en fase posterior.  
- Si RegisterScreen podrá tocarse mínimamente en 1.2 para eliminar doble navegación o queda 100 % en 1.4.  
- Resolución del working tree sucio (`.idea/misc.xml`, `docs/auth-current-state-audit.md`) antes del commit de implementación.  
- Comportamiento runtime de doble navegación (ya conocido por código; no re-ejecutado en esta inspección).

---

### Confirmaciones finales de esta inspección

- No se modificó código fuente, Gradle, Room, schemas, UI ni navegación.  
- No se ejecutó `git add`, commit ni push.  
- Lint no se ejecutó.  
- Único archivo intencional nuevo: `docs/auth-phase-1-2-inspection.md`.
