# ADR-0006: Hilt como framework de inyección de dependencias

## Estado

Aceptada.

## Contexto

14 `ViewModelFactory` manuales repetían el mismo patrón: construir un `MedicationRepositoryImpl` a partir de los DAOs de `MediTrackDatabase.getInstance(context)`, instanciar `AlarmScheduler`/`FileStorageHelper`/`SessionManager`, y pasarlos al constructor del `ViewModel`. `SessionManager` en particular se reinstanciaba en cada factory, recreando la `MasterKey` de `EncryptedSharedPreferences` en cada pantalla en vez de una sola vez.

Las dos alternativas evaluadas fueron un `AppContainer` manual (un objeto singleton con todas las dependencias, sin generación de código) y Hilt (el estándar de Android para DI, con generación de código vía KSP). Se optó explícitamente por **Hilt**, pese a su complejidad extra, por ser el estándar de la industria — coherente con el propósito del proyecto de ser una referencia de buenas prácticas, no solo la solución más simple.

## Decisión

- `MediTrackApplication` (`@HiltAndroidApp` + `Configuration.Provider`) reemplaza la inicialización por defecto de `WorkManager` vía App Startup, para que el `HiltWorkerFactory` esté disponible antes de que corra cualquier `Worker`.
- `di/DatabaseModule` provee `MediTrackDatabase` y sus 5 DAOs; `di/RepositoryModule` liga (`@Binds`) las 3 interfaces de repositorio a sus implementaciones. Se escribieron antes de tocar ningún `ViewModel`, para que ninguna conversión quedara bloqueada a mitad de camino por un binding faltante.
- El resto (los 9 casos de uso, los repositorios impl, `AlarmScheduler`/`FileStorageHelper`/`NotificationHelper`, `SessionManager`) se resuelve directo vía `@Inject constructor`, sin módulo explícito. `SessionManager` se marcó `@Singleton`.
- Los 13 `ViewModel`s pasan a `@HiltViewModel` + `@Inject constructor`; los que leen argumentos de navegación usan `SavedStateHandle` (confirmado que lee `Fragment.getArguments()` automáticamente, sin necesitar Safe Args). Se borran las 14 `ViewModelFactory`.
- `MainActivity` + los Fragments pasan a `@AndroidEntryPoint`. `MedicationAlarmReceiver`, `MedicationActionReceiver` y `BootReceiver` también (soportado en `BroadcastReceiver` desde Hilt 2.28+, con inyección síncrona antes de que corra `onReceive()`, compatible con el `goAsync()` existente). `MissedDoseWorker` pasa a `@HiltWorker` + `@AssistedInject`.

Todo vía KSP (`ksp(...)`), no `kapt` — Hilt soporta KSP desde 2.48, y usar kapt hubiera significado correr dos procesadores de anotaciones en el mismo build (Room ya usa KSP).

## Consecuencias

**Positivas:** las 14 factories manuales desaparecen; un binding faltante falla en tiempo de compilación (KSP), no en runtime; `SessionManager` deja de recrear la `MasterKey` en cada pantalla.

**Negativas / gotchas encontrados:**

- **Classloader de Hilt+KSP** ([dagger/dagger#3965](https://github.com/google/dagger/issues/3965)): declarar el plugin de Hilt en el `build.gradle.kts` raíz (`apply false`) mientras KSP solo estaba declarado en el subproyecto `app` rompe la build con `"The KSP plugin was detected to be applied but its task class could not be found"`. Se corrigió declarando también `alias(libs.plugins.ksp) apply false` en el raíz, igual que el plugin de Android.
- **`TestListenableWorkerBuilder` + `@AssistedInject`**: `TestListenableWorkerBuilder.build()` reflexiona buscando un constructor `(Context, WorkerParameters)` de 2 argumentos cuando no se le pasa un `WorkerFactory` explícito — ese constructor dejó de existir al convertir `MissedDoseWorker` a `@AssistedInject` (ahora recibe también `EvaluateMissedDoseUseCase`). `MissedDoseWorkerTest` se adaptó pasándole un `WorkerFactory` de prueba que arma el caso de uso a mano desde los repositorios reales.
- **`domain/usecase/` importando `services/` directo** (sin interfaz): se aceptó como trade-off pragmático — introducir una interfaz para `AlarmScheduler`/`FileStorageHelper`/`NotificationHelper` solo para permitir un mock que ningún test de este proyecto usa (todos los tests son instrumentados contra la base y servicios reales) no pagaba su complejidad.
