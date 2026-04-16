# WalkRush — Guía para Agentes de Código

> Este documento está escrito para agentes de IA que trabajen en el proyecto. El lector se asume con cero contexto previo sobre la aplicación.

---

## 📋 Resumen del proyecto

**WalkRush** es una aplicación Android open source (licencia MIT) orientada a personas que entrenan en **caminadora (treadmill)**. Permite registrar sesiones de caminata/carrera indoor, sincronizar métricas de salud y gestionar un perfil de usuario.

- **Idioma principal del proyecto**: español (comentarios, documentación y strings de UI).
- **Estado**: en desarrollo temprano. Algunas pantallas están implementadas; otras son placeholders.
- **Package**: `dev.jefrien.walkrush`

---

## 🛠 Stack tecnológico

| Capa | Tecnología | Versión destacada |
|------|------------|-------------------|
| **Lenguaje** | Kotlin | 2.3.20 |
| **UI** | Jetpack Compose + Material 3 | BOM 2026.03.01 |
| **Navegación** | Navigation Compose | 2.9.7 |
| **DI** | Koin | 4.2.1 |
| **Backend / Auth** | Supabase Kotlin SDK | 3.5.0 |
| **Red** | Ktor Client | 3.4.2 |
| **Serialización** | Kotlinx Serialization | 1.11.0 |
| **Salud** | Health Connect | 1.1.0 |
| **Almacenamiento local** | multiplatform-settings (SharedPreferences) | 1.3.0 |
| **Build** | Gradle Kotlin DSL | AGP 9.1.1 |

- `minSdk = 26`, `compileSdk = 36`, `targetSdk = 36`
- Java 11 como target de compilación.

---

## 🏗 Arquitectura y organización del código

El proyecto sigue una arquitectura limpia con separación por capas y el patrón **MVVM** en la capa de presentación.

```
app/src/main/java/dev/jefrien/walkrush/
├── data/
│   ├── local/                    # Almacenamiento local
│   │   └── SupabaseSessionManager.kt   # SessionManager sobre SharedPreferences
│   └── remote/supabase/          # Implementaciones de repositorios
│       ├── SupabaseAuthRepository.kt
│       └── SupabaseUserProfileRepository.kt
├── domain/
│   ├── model/                    # Modelos de dominio serializables
│   │   ├── auth/                 # User, AuthResult, AuthException
│   │   └── userprofile/          # UserProfile, FitnessGoal, TreadmillCapabilities, enums
│   ├── repository/               # Interfaces de repositorios
│   │   ├── AuthRepository.kt
│   │   └── UserProfileRepository.kt
│   └── usecase/                  # Casos de uso
│       ├── auth/                 # SignInUseCase, SignUpUseCase, SignOutUseCase
│       └── userprofile/          # SaveUserProfileUseCase, GetUserProfileUseCase
├── presentation/
│   ├── auth/                     # AuthScreen, AuthViewModel, componentes
│   ├── onboarding/               # OnboardingScreen, OnboardingViewModel, steps y componentes
│   ├── navigation/               # Route, WalkRushNavHost, NavigationState
│   └── ... (Home, Profile, History, ActiveWorkout son placeholders)
├── di/                           # Módulos de Koin
│   ├── AppModule.kt              # Dispatchers y utilidades de app
│   ├── NetworkModule.kt          # SupabaseClient, Ktor HttpClient, Json
│   ├── DataModule.kt             # Binding de repositorios
│   ├── DomainModule.kt           # Binding de use cases
│   └── ViewModelModule.kt        # Binding de ViewModels
└── ui/theme/                     # Colores, tipografía y tema de Compose
    ├── Color.kt
    ├── Theme.kt
    └── Type.kt
```

### Convenciones arquitectónicas
- **Interfaces de repositorio** en `domain/repository/`; **implementaciones** en `data/remote/supabase/`.
- Los **UseCases** son clases con `operator fun invoke(...)`.
- Los **ViewModels** exponen:
  - `StateFlow<UiState>` para el estado reactivo de la pantalla.
  - `SharedFlow<Event>` para eventos de una sola vez (navegación, snackbars).
  - Propiedades mutables de Compose (`mutableStateOf`, `mutableIntStateOf`, etc.) para campos de formulario.
- La **navegación** usa `Route` (sealed class) con paths string; el `WalkRushNavHost` gestiona el grafo y los deeplinks.

---

## ⚙️ Configuración y build

### Credenciales obligatorias
La app requiere un proyecto de Supabase. Crea o edita `local.properties` en la raíz:

```properties
SUPABASE_URL=https://tu-proyecto.supabase.co
SUPABASE_ANON_KEY=tu-anon-key-publica
```

> **No subas `local.properties` al control de versiones.** Ya está en `.gitignore`.

El `build.gradle.kts` del módulo `app` inyecta estas variables como `BuildConfig.SUPABASE_URL` y `BuildConfig.SUPABASE_ANON_KEY`. También puede leer variables de entorno para CI/CD.

### Comandos de build

```bash
# Sincronizar / compilar
./gradlew :app:assembleDebug

# Instalar en dispositivo conectado
./gradlew :app:installDebug

# Ejecutar tests unitarios
./gradlew :app:testDebugUnitTest

# Ejecutar tests instrumentados (requiere emulador/dispositivo)
./gradlew :app:connectedDebugAndroidTest
```

### Opciones de Gradle activas
- `org.gradle.parallel=true`
- `org.gradle.caching=true`
- `android.useAndroidX=true`
- `android.nonTransitiveRClass=true`
- `ksp.incremental=true`

---

## 🧪 Estrategia de testing

**Estado actual**: hay solo tests de ejemplo (`ExampleUnitTest`, `ExampleInstrumentedTest`).

**Dependencias disponibles**:
- JUnit 4 para tests unitarios.
- Espresso para tests de UI instrumentados.
- Compose UI Test (`androidx.compose.ui:test-junit4`) para tests de Compose.

**Recomendaciones para nuevos tests**:
- Tests unitarios para UseCases y ViewModels (usando coroutines test).
- Tests de UI para flujos de Auth y Onboarding con Compose Testing.
- Mockear repositorios en tests de ViewModel; evitar llamadas reales a Supabase.

---

## 🎨 Estilo de código y convenciones

- **Idioma de comentarios y documentación**: español.
- **Nombres de clases/interfaces**: `PascalCase` en inglés (p. ej. `AuthRepository`, `UserProfile`).
- **Funciones y propiedades**: `camelCase`.
- **Packages**: todo en minúsculas y en inglés.
- **Imports**: sin wildcards; usa imports explícitos.
- **Coroutines**:
  - Usa `viewModelScope.launch` en ViewModels.
  - Inyecta dispatchers (`Dispatchers.IO`, `Dispatchers.Default`) desde `AppModule` para facilitar tests.
- **Compose**:
  - Pantallas en `presentation/<feature>/<Feature>Screen.kt`.
  - Componentes reutilizables en `presentation/<feature>/components/`.
  - Estados de carga y error manejados explícitamente en el `UiState`.
- **Strings de UI**: actualmente hardcodeados en español dentro de los Composables. En el futuro podrían migrarse a `res/values/strings.xml`.

---

## 🔐 Consideraciones de seguridad

- Las claves de Supabase (`SUPABASE_ANON_KEY`, `SUPABASE_SERVICE_ROLE_KEY`) residen únicamente en `local.properties`.
- El flujo OAuth usa **PKCE** (`FlowType.PKCE`) con deeplink `walkrush://callback`.
- La sesión de Supabase se persiste localmente mediante `SupabaseSessionManager`, que usa `SharedPreferences` a través de `multiplatform-settings`. No está cifrado actualmente; si la app maneja datos sensibles en el futuro, considera migrar a `EncryptedSharedPreferences`.
- Permisos de Health Connect declarados en `AndroidManifest.xml` (lectura/escritura de pasos, distancia, calorías, ritmo cardíaco, ejercicio, velocidad).

---

## 🚀 Flujo de navegación principal

1. `MainActivity` determina la ruta inicial consultando `AuthRepository`:
   - Si no está autenticado → `Route.Auth`
   - Si está autenticado pero sin perfil → `Route.Onboarding`
   - Si está autenticado y con perfil → `Route.Home`
2. **Auth**: login / registro con email y contraseña vía Supabase.
3. **Onboarding**: 5 pasos (bienvenida, info básica, objetivos, horario, capacidades de caminadora) que guardan el perfil en Supabase.
4. **Home / Profile / History / ActiveWorkout**: pantallas principales (algunas aún son placeholders).

---

## 📝 Notas para el desarrollo

- La app está en fase temprana; muchas pantallas principales (`Home`, `Profile`, `History`, `ActiveWorkout`) aún no están implementadas y solo existen placeholders en `WalkRushNavHost`.
- Health Connect está declarado como dependencia pero la integración funcional aún no está completada.
- Si agregas nuevas pantallas, sigue el patrón existente: `Screen.kt` + `ViewModel.kt` + registro en `ViewModelModule` + ruta en `Route` + composable en `WalkRushNavHost`.
- El tema soporta **Material You dynamic color** en Android 12+ y tiene esquemas claros y oscuros personalizados.
