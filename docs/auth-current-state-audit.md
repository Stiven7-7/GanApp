# Auditoría del estado actual de autenticación de GANAPP

## 1. Alcance y entorno

| Campo | Valor |
|---|---|
| Fecha | 2026-07-30 / 2026-07-31 (UTC-5) |
| Rama | `feature/auth-session-foundation` |
| Commit (HEAD) | `67c27f6009b79ac56a68d46abd1498d3361c9870` |
| Working tree al inicio | Limpio |
| Emulador / dispositivo A | Samsung remoto `SM-S921U` vía ADB `localhost:63626`, Android **14** — BD vacía confirmada (0 usuarios / 0 animales / 0 notificaciones); UI Login visible |
| Emulador B | AVD local `Medium_Phone_API_36.1` (`sdk_gphone64_x86_64`), Android **16** — APK debug instalado, datos limpiados con `pm clear`; UI Login visible |
| Estado inicial declarado | App inicia en Login; sin sesión; sin animales/notificaciones previos |
| Confirmación de no modificación | No se modificó código, Room, Gradle, navegación ni Git (salvo este informe nuevo) |

**Limitación de ejecución:** el dispositivo Samsung se desconectó tras la inspección inicial de BD/UI. En el AVD local, la automatización `adb input tap` **no activó de forma fiable** el `Modifier.clickable` de Compose hacia Registro (el enlace es visible y `clickable=true`, pero la pantalla no cambió). Por ello varios escenarios UI del PASO 19 quedan como **NO DEFINIDO** en ejecución, aunque el comportamiento queda **determinado por código**.

---

## 2. Archivos y componentes inspeccionados

| Archivo | Clase / función | Responsabilidad | Usado | Rol auth |
|---|---|---|---|---|
| `ui/features/auth/LoginScreen.kt` | `LoginScreen` | Formulario login + error UI | Sí | Login, navegación |
| `ui/features/auth/RegisterScreen.kt` | `RegisterScreen` | Formulario registro + validación parcial | Sí | Registro, navegación |
| `ui/features/auth/AuthViewModel.kt` | `AuthViewModel` | Estado auth en memoria; register/login/logout | Sí | Registro, login, logout (solo memoria) |
| `domain/model/Usuario.kt` | `Usuario` | Modelo de dominio | Sí | Persistencia / UI |
| `data/local/entity/UsuarioEntity.kt` | `UsuarioEntity` | Tabla Room `usuario` | Sí | Persistencia |
| `data/local/dao/UsuarioDao.kt` | `UsuarioDao` | CRUD / `getByCorreo` | Sí | Persistencia |
| `domain/repository/UsuarioRepository.kt` | interface | Contrato registro/login | Sí | Persistencia |
| `data/repository/impl/UsuarioRepositoryImpl.kt` | impl | Hash BCrypt + DAO | Sí | Registro, login |
| `data/mapper/UsuarioMapper.kt` | `toDomain` / `toEntity` | Mapeo (register no usa `toEntity`) | Parcial | Persistencia |
| `domain/usecase/usuario/LoginUseCase.kt` | `LoginUseCase` | Delega a repo | **No** (AuthVM salta al repo) | — |
| `domain/usecase/usuario/RegisterUserUseCase.kt` | `RegisterUserUseCase` | Delega a repo | **No** (AuthVM salta al repo) | — |
| `domain/usecase/usuario/GetUsuarioByIdUseCase.kt` | use case | Carga usuario en Home | Sí | Post-login |
| `util/PasswordUtils.kt` | `hash` / `verify` | BCrypt cost 12 | Sí | Seguridad |
| `ui/navigation/NavGraph.kt` | `NavGraph` / `Screen` | Rutas login/register/home/{id} | Sí | Navegación |
| `ui/features/home/HomeScreen.kt` | `HomeScreen` | Hub post-login | Sí | Usa `idUsuario` |
| `ui/features/home/HomeViewModel.kt` | `HomeViewModel` | Carga usuario por ID | Sí | Post-login |
| `di/DatabaseModule.kt` | provides DAO/DB | Room + fallback destructivo | Sí | Persistencia |
| `di/RepositoryModule.kt` | binds UsuarioRepository | Hilt | Sí | DI |
| `di/UseCaseModule.kt` | provides Login/Register use cases | Hilt | Use cases auth **no consumidos por AuthVM** | DI |
| `MainActivity.kt` | Activity | `NavGraph()`; permiso notificaciones | Sí | Entry |
| `data/local/db/AppDatabase.kt` | BD v6 | Incluye `UsuarioEntity` | Sí | Persistencia |
| Schemas `1.json`–`6.json` | schema Room | Índice UNIQUE correo: **ausente** en v6 | Sí | Persistencia |
| `app/src/test/.../ExampleUnitTest.kt` | plantilla | `2+2` | Sí (gradlew test) | No auth |
| `app/src/androidTest/.../ExampleInstrumentedTest.kt` | plantilla | packageName | No vía `gradlew test` | No auth |

---

## 3. Hechos confirmados por código

### Registro
- UI construye `Usuario(nombre, apellido, correo, contrasena)` con `idUsuario` default **0**.
- `AuthViewModel.register` llama `usuarioRepository.register(usuario)` en `viewModelScope.launch` y **descarta** el `Long` retornado; luego `_usuario.value = usuario` (**sigue con id 0**).
- Repo hashea con `PasswordUtils.hash` **antes** de insertar; construye `UsuarioEntity` manualmente (no usa `toEntity()`).
- DAO: `@Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(...): Long`.

### Validaciones
- Solo UI registro: `isNotBlank()` en nombre, apellido, correo, contraseña.
- Sin formato de correo, longitud mínima, trim, lowercase.
- Login: **sin** validación de vacíos en UI.

### Normalización
- **No** hay `trim` ni `lowercase` en registro ni login.
- `getByCorreo` usa `WHERE correo = :correo` (igualdad exacta SQLite, sensible a mayúsculas según collation por defecto BINARY).

### Correo único
- `UsuarioEntity`: **sin** `@Index(unique=true)`.
- Schema 6: `indices: []` en tabla `usuario`.
- `OnConflictStrategy.REPLACE` actúa por **PK**, no por correo → correos duplicados **pueden coexistir**.
- Repo **no** consulta existencia antes de insertar.
- UI **no** previene duplicados.

### ID
- Room `autoGenerate = true` en `idUsuario`.
- `insert` retorna `Long` (rowId).
- ViewModel **no** propaga el ID al estado.
- Navegación post-registro va a **Login**, no a Home; no usa ID de registro.
- Login exitoso navega con `usuario.idUsuario` del dominio mapeado desde entity (**ID real**).

### BCrypt
- Librería `at.favre.lib:bcrypt` **0.9.0** (`libs.versions.toml`).
- `hash(password, cost=12)` → `BCrypt.withDefaults().hashToString`.
- Salt automático (estándar BCrypt).
- `verify` → `BCrypt.verifyer().verify`.
- Hash en coroutine del ViewModel (`viewModelScope`), no en main thread de forma síncrona visible.
- Sin logs de contraseña/hash en el código inspeccionado.

### Login
- Busca por correo; si null → null; si hash no verifica → null.
- UI: un solo mensaje `"Correo o contraseña incorrectos"` si `loginResult == false`.
- No distingue usuario inexistente vs contraseña incorrecta.

### ViewModel
- Inyecta **repositorio**, no use cases.
- Estados: `_usuario`, `_loginResult` (StateFlow); **sin** SharedFlow, **sin** loading, **sin** try/catch.
- `logout()` limpia ambos a null; **ninguna UI lo invoca**.

### Navegación
- Registro: `onRegisterSuccess()` **inmediato** tras `viewModel.register` + `LaunchedEffect(usuario)` → **doble navegación posible** a `login`.
- Login: `LaunchedEffect(loginResult)` si true → `onLoginSuccess(idUsuario)` → `home/{idUsuario}`.
- Sin `popUpTo` / clear back stack.
- `startDestination = login`.

### Logout / sesión
- Logout solo en ViewModel; sin botón.
- Sin DataStore / SharedPreferences de sesión / SessionRepository (búsqueda en `app/src`).

### Pruebas
- Ninguna prueba real de auth; solo plantillas.

---

## 4. Hechos confirmados por ejecución

| Escenario | Resultado ejecución | Evidencia |
|---|---|---|
| Base vacía (Samsung) | **PASS** | `SELECT COUNT(*)` → usuarios 0, animales 0, notificaciones 0 |
| App inicia en Login (Samsung + AVD) | **PASS** | UI dump: “Iniciar Sesión”, campos correo/contraseña, enlace registro |
| A–L flujos registro/login completos en UI | **NO DEFINIDO** (mayoría) | ADB tap no navegó a Registro en AVD; Samsung desconectado |
| Ausencia de restauración de sesión | **PASS** (código + cold start) | `startDestination=login`; cold start AVD muestra Login; sin DataStore |
| Room file hasta primer DAO | Observado | Tras solo mostrar Login, carpeta `databases/` vacía en AVD (WorkManager sí crea `workdb`) |

---

## 5. Flujo actual de registro

```
RegisterScreen (estado local mutableStateOf)
  → validación UI isNotBlank (4 campos)
  → AuthViewModel.register(Usuario id=0)
       → viewModelScope.launch
       → UsuarioRepositoryImpl.register
            → PasswordUtils.hash(plain, cost=12)
            → UsuarioEntity(..., contrasena=hash)
            → UsuarioDao.insert (REPLACE por PK) → Long id  [descartado]
       → _usuario = usuario (id sigue 0)
  → onRegisterSuccess() INMEDIATO (sin await) → NavGraph navigate("login")
  → LaunchedEffect(usuario!=null) → onRegisterSuccess() OTRA VEZ → navigate("login")
```

Mapper `toEntity` **no** participa en register. Use case `RegisterUserUseCase` **no** participa.

---

## 6. Flujo actual de login

```
LoginScreen (correo/contraseña locales)
  → AuthViewModel.login(correo, contrasena)  [sin validar vacíos]
       → viewModelScope.launch
       → UsuarioRepositoryImpl.login
            → dao.getByCorreo(correo)  // igualdad exacta
            → si null → null
            → PasswordUtils.verify(plain, hash)
            → entity.toDomain() o null
       → si user!=null: _usuario=user; _loginResult=true
         else: _loginResult=false
  → LaunchedEffect(loginResult==true) → onLoginSuccess(idUsuario)
  → NavGraph navigate("home/$idUsuario")  // sin popUpTo
  → UI error si loginResult==false: "Correo o contraseña incorrectos"
```

---

## 7. Validaciones y normalización

| Campo / regla | Existente | Parcial | Ausente | Dónde |
|---|---|---|---|---|
| Nombre no vacío | Sí (`isNotBlank`) | | | UI Register |
| Apellido no vacío | Sí | | | UI Register |
| Correo no vacío | Sí | | | UI Register |
| Contraseña no vacía | Sí | | | UI Register |
| Formato correo | | | Sí | — |
| Longitud mínima password | | | Sí | — |
| Trim espacios | | | Sí | — |
| Lowercase correo | | | Sí | — |
| Mensaje si validación falla | | | Sí (botón no-op silencioso) | UI Register |
| Validación login vacíos | | | Sí | LoginScreen |
| Unique correo Room | | | Sí | Entity/schema |
| Pre-check duplicado repo | | | Sí | Repo |
| Consulta correo case-insensitive | | | Sí | DAO |

---

## 8. Persistencia e identificadores

| Aspecto | Detalle |
|---|---|
| Firma DAO | `@Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(usuario: UsuarioEntity): Long` |
| ID generado | Sí (`autoGenerate = true`) |
| Repo retorna ID | Sí (`register`: `Long`) |
| ViewModel recibe ID | **No** (ignorado) |
| Objeto en memoria post-registro | `Usuario` con **idUsuario=0** |
| Navegación post-registro | A `login` (no usa ID) |
| Navegación post-login | `home/{idUsuario}` con ID de entity |
| Riesgo ID 0 en Home tras registro | No aplica vía registro (va a Login); sí si alguien navegara con usuario de memoria post-register |

---

## 9. Seguridad de contraseña

| Aspecto | Estado |
|---|---|
| Librería | `at.favre.lib:bcrypt:0.9.0` |
| Cost | 12 |
| Salt | Automático (BCrypt) |
| Almacenamiento | Hash en columna `contrasena`; plain solo en memoria del formulario |
| Momento del hash | En repo, antes de `insert` |
| Hilo | Dentro de coroutine suspendida del ViewModel |
| Logs sensibles | No encontrados en código auth |
| Formato esperado | Prefijo típico `$2a$` / `$2b$` (no se copió hash completo; no se obtuvo fila en AVD por falta de registro UI) |
| Dominio post-login | `toDomain()` **incluye el hash** en `Usuario.contrasena` (queda en StateFlow) |

---

## 10. Estados y eventos de AuthViewModel

| Estado o evento | Tipo | Quién lo escribe | Quién lo observa | Riesgo |
|---|---|---|---|---|
| `usuario` | `StateFlow<Usuario?>` | `register`, `login`, `logout` | LoginScreen, RegisterScreen | ID 0 tras register; hash en memoria |
| `loginResult` | `StateFlow<Boolean?>` | `login`, `logout` | LoginScreen | null inicial; residual entre intentos |
| Loading | — | — | — | Sin feedback de espera (BCrypt cost 12 puede notarse) |
| Error tipado | — | — | — | Solo boolean genérico |
| SharedFlow eventos | — | — | — | Navegación acoplada a StateFlow + callbacks |
| `logout` | función | Nadie en UI | — | Código muerto funcional |

Dependencias: solo `UsuarioRepository`. Scope: `viewModelScope`. Sin try/catch → excepciones Room/BCrypt pueden crashar coroutine sin UI.

---

## 11. Navegación y doble navegación

**Código (registro):**

1. `Button.onClick` → `viewModel.register(...)` luego **`onRegisterSuccess()`** inmediato.  
2. `LaunchedEffect(usuario)` cuando `usuario != null` → **`onRegisterSuccess()`** otra vez.  
3. Destino: `navController.navigate(Screen.Login.route)` **sin** `popUpTo`.  
4. Atrás desde Login tras registro: puede volver a Register (back stack).  
5. Tras registro el usuario **no** queda “autenticado” en Home; vuelve a Login.

**Código (login):**

1. `LaunchedEffect(loginResult)` una vez true → `home/{id}`.  
2. Sin clear back stack → Atrás desde Home vuelve a Login (con posible `loginResult` residual true → **riesgo de re-navegación** si el efecto se re-dispara según ciclo de vida del ViewModel/composable).  
3. Rotación/recreación: sin sesión persistente; si proceso muere, vuelve a Login.

**Ejecución:** no se pudo contar eventos de navegación de registro en AVD (tap Compose no navegó). Cold start → Login confirmado.

---

## 12. Manejo de errores

| Error | Origen | Capturado | Visible UI | Mensaje | Riesgo |
|---|---|---|---|---|---|
| Campos registro vacíos | UI | Sí (no llama VM) | No | — | Silencioso |
| Correo inválido formato | — | No | No | — | Datos basura |
| Correo duplicado | Room no rechaza | No | No | — | Cuentas duplicadas |
| Login fallido (user/pass) | Repo null | Sí → `loginResult=false` | Sí | “Correo o contraseña incorrectos” | No distingue causa |
| Usuario inexistente | DAO null | Como login fallido | Mismo mensaje | Idem | — |
| Hash inválido / BCrypt exception | `verify` | No try/catch | No | — | Crash potencial |
| Room insert exception | DAO | No try/catch | No | — | Crash; navegación ya pudo ocurrir |
| Navegación prematura | UI register | N/A | N/A | — | Login antes de confirmar insert |

---

## 13. Relación con animales y notificaciones

```
Login éxito → home/{idUsuario}
  → Home pasa idUsuario a:
       register_animal/{idUsuario}
       animals/{userId}
       notifications/{userId}
  → AnimalsViewModel.insertAnimal(..., idUsuario)
  → AnimalsViewModel.loadAnimals(userId) → repo por usuario
  → Notifications filtradas por idUsuario (en memoria tras getAll)
```

- **No** hay fuente central de sesión autenticada; solo argumento de navegación (+ ViewModel Home puntual).
- Riesgo: manipular ruta/deep link interno con otro `idUsuario` consultaría datos de otro usuario (app offline local; amenaza limitada a quien controla el dispositivo).
- ID inválido / usuario inexistente: Home muestra sin “Bienvenido” si `getUsuarioById` retorna null; rutas hijas aún reciben el id numérico.

---

## 14. Logout y sesión

| Pregunta | Respuesta |
|---|---|
| ¿Existe `logout`? | Sí, en `AuthViewModel` |
| ¿UI? | **No** |
| ¿Qué limpia? | `_usuario`, `_loginResult` en memoria |
| ¿Back stack / Room? | No |
| ¿Sesión persistente? | **No** (sin DataStore/Prefs) |
| `startDestination` | `login` |
| Tras matar proceso | Vuelve a Login |

---

## 15. Pruebas existentes

| Archivo | Tipo | Valida | ¿`gradlew test`? | Cobertura auth |
|---|---|---|---|---|
| `ExampleUnitTest.kt` | unit | aritmética | Sí | Ninguna |
| `ExampleInstrumentedTest.kt` | androidTest | packageName | No (requiere dispositivo) | Ninguna |

**Vacíos:** PasswordUtils, UsuarioDao, UsuarioRepositoryImpl, AuthViewModel, pantallas, navegación, unicidad correo, normalización, BCrypt.

---

## 16. Problemas encontrados

### Críticos
| Archivo | Problema | Causa | Impacto | Evidencia |
|---|---|---|---|---|
| `RegisterScreen.kt` | Navegación antes de confirmar persistencia + doble `onRegisterSuccess` | Callback inmediato + `LaunchedEffect` | Usuario en Login sin garantía de insert; posibles navigates múltiples | Líneas onClick + LaunchedEffect |
| `UsuarioEntity` / schema 6 | Sin UNIQUE en correo | Diseño | Duplicados posibles | Entity + `6.json` indices `[]` |

### Altos
| Archivo | Problema | Causa | Impacto | Evidencia |
|---|---|---|---|---|
| `AuthViewModel.register` | Ignora ID retornado; estado con id 0 | Asigna el `Usuario` de entrada | Estado inconsistente | `register` VM |
| `RegisterScreen` / Login | Sin normalización correo | No trim/lowercase | Login falla por capitalización/espacios | Código |
| Auth | Sin sesión / sin logout UI | No implementado | Re-login siempre; sin cierre explícito | NavGraph + búsqueda sesión |
| `AuthViewModel` | Sin manejo de excepciones | Sin try/catch | Crash silencioso en UI | VM |

### Medios
| Archivo | Problema | Causa | Impacto | Evidencia |
|---|---|---|---|---|
| `LoginUseCase` / `RegisterUserUseCase` | No usados por AuthVM | DI provee; VM usa repo | Capas inconsistentes | UseCaseModule vs AuthVM |
| `UsuarioMapper.toDomain` | Expone hash como `contrasena` | Mapeo directo | Hash en StateFlow UI | Mapper |
| `NavGraph` login→home | Sin `popUpTo` | Diseño | Back stack confuso / re-nav | NavGraph |
| Validaciones | Débiles / silenciosas | Solo isNotBlank | UX pobre / datos inválidos | RegisterScreen |

### Bajos
| Archivo | Problema | Causa | Impacto | Evidencia |
|---|---|---|---|---|
| `AuthViewModel.logout` | Sin callers | Feature incompleta | Código muerto | Grep |
| Loading | Ausente | — | UI puede parecer congelada en hash | VM |

---

## 17. Riesgos

- **Seguridad:** hash en dominio/StateFlow; sin rate-limit; duplicados de cuenta.
- **Persistencia:** REPLACE por PK no protege correo; migraciones destructivas globales (fuera de auth pero afecta usuarios).
- **Navegación:** doble navigate; back stack; posible re-entrada a Home.
- **UX:** errores silenciosos en registro; mensaje único de login.
- **Regresión futura:** cualquier sesión real debe coordinar con `idUsuario` en todas las rutas de animales/notificaciones.

---

## 18. Información no verificable

- Resultado UI de registro válido / duplicado / capitalización / espacios (ADB Compose no navegó a Register en AVD; Samsung offline).
- Login correcto end-to-end con ID en Home en esta sesión.
- Contenido real de columna hash en Room tras registro (no se insertó usuario en AVD).
- Número exacto de eventos de navegación en dispositivo.
- Comportamiento exacto de BCrypt ante hash corrupto en runtime.
- Collation SQLite en todos los OEM (asumida BINARY por defecto).
- Rotación de pantalla durante login (no probada).

---

## 19. Recomendaciones sin implementación

Orden sugerido (sin código):

1. Unificar navegación de registro (un solo evento tras éxito confirmado de Room).  
2. Propagar y usar el ID real retornado por `insert`.  
3. Definir política de correo: unique + normalización (trim/lowercase) alineada login/registro.  
4. Manejo de errores tipados + mensajes UI (duplicado, validación, fallo hash).  
5. Validaciones de formato/longitud en un solo lugar (dominio o use case).  
6. Usar use cases desde AuthViewModel (o retirar los no usados).  
7. Definir contrato de sesión (futuro DataStore) y logout UI sin tocar aún animales/notificaciones más de lo necesario.  
8. Añadir pruebas unitarias de PasswordUtils, repo y ViewModel.  
9. Ajustar back stack login/home (`popUpTo`) en diseño de Fase 1.

---

## 20. Datos de prueba creados

| Origen | Datos | Persistidos |
|---|---|---|
| Samsung (inspección) | Ningún usuario creado en esta auditoría | BD vacía al inspeccionar |
| AVD local | `pm clear` al instalar; intentos de texto en Login (`nouser@example.com` / pass de prueba) **sin confirmación de insert** | Carpeta `databases/` sin `ganapp_db` al cerrar pruebas de UI (Room no abierto vía DAO exitoso) |
| Correos ficticios previstos (no confirmados en BD) | `prueba.auth.01@example.com`, `Prueba.Auth.01@Example.com` | No confirmados |
| Contraseñas | Siempre **[REDACTADA]** en este informe | — |

**No se limpió la base al final** (más allá del `pm clear` inicial del AVD para partir de instalación limpia, autorizado como entorno de prueba).

---

## 21. Verificación final de Git

Ver sección de entrega; esperado únicamente:

```
?? docs/auth-current-state-audit.md
```

(si `docs/` ya estaba tracked parcialmente, el archivo nuevo aparece como untracked).

---

*Fin de la auditoría del estado actual de autenticación. Sin implementación de sesión ni cambios de código.*
