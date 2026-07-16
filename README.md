# MediTrack

Aplicación Android para la gestión de medicación de pacientes mayores, pensada para tres roles: el paciente que gestiona sus propios medicamentos, el cuidador que supervisa a uno o más pacientes mayores a su cargo, y el paciente mayor (senior) que solo confirma sus dosis desde una pantalla simplificada.

Proyecto académico construido como referencia de Clean Architecture + DDD en Android nativo (Kotlin, sin frameworks de UI declarativa).

## Índice

- [Funcionalidad](#funcionalidad)
- [Stack técnico](#stack-técnico)
- [Arquitectura](#arquitectura)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Requisitos](#requisitos)
- [Cómo correr el proyecto](#cómo-correr-el-proyecto)
- [Testing](#testing)
- [CI](#ci)
- [Documentación](#documentación)

## Funcionalidad

- **Paciente / Cuidador**: registro y login, alta de medicamentos con foto, dosis, frecuencia, horarios y días de la semana; historial de tomas del día; confirmar o posponer una dosis desde la alerta.
- **Cuidador**: alta de pacientes mayores a su cargo (genera usuario y PIN de acceso), gestión de su contacto de emergencia, panel con el estado de tomas del día de cada senior y alertas de dosis no confirmadas (con llamada directa al contacto de emergencia).
- **Paciente mayor (senior)**: pantalla de alerta simplificada (solo nombre del medicamento, dosis y botón de confirmar — sin posponer, para reducir la carga cognitiva de la interacción).
- **Alarmas y notificaciones**: alarmas exactas (`AlarmManager`) por cada horario configurado, con reprogramación automática tras reinicio del dispositivo (`BootReceiver`) y verificación de dosis no confirmadas 30 minutos después vía `WorkManager`.

## Stack técnico

| Área | Tecnología |
|---|---|
| Lenguaje | Kotlin |
| UI | Android Views + Navigation Component (sin Jetpack Compose) |
| Concurrencia | Coroutines + `Flow` |
| Persistencia | Room 2.8.4 (SQLite) |
| Inyección de dependencias | Hilt 2.60.1 |
| Alarmas / tareas en segundo plano | `AlarmManager` + WorkManager 2.11.2 (`HiltWorkerFactory`) |
| Cámara | CameraX 1.6.1 |
| Sesión / credenciales | `EncryptedSharedPreferences` (androidx.security-crypto) |
| CI | GitHub Actions |
| Testing | JUnit4 + Espresso (instrumentado) |

`minSdk = 28`, `targetSdk = compileSdk = 37`.

## Arquitectura

El proyecto sigue **Clean Architecture** con tres capas y **Hilt** como composition root:

```
presentation/  →  domain/  ←  data/
   (Android)      (Kotlin puro)   (Room, servicios)
```

- **`domain/`** — no depende de ningún framework de Android (ni `androidx.lifecycle`, ni `android.*`, salvo `android.net.Uri`/`android.util.Patterns` en un par de puntos pragmáticos). Contiene:
  - `model/`: entidades de dominio y value objects (`User`, `Medication`, `Schedule`, `WeekDays`, `MedicationLog`, `MedicationLogStatus`, `EmergencyContact`, `UserRole`, `MissedDoseAlert`, `SeniorDoseStatus`).
  - `repository/`: interfaces (`UserRepository`, `MedicationRepository`, `EmergencyContactRepository`), expuestas como `Flow` para los datos reactivos.
  - `usecase/`: 9 casos de uso, cada uno una única operación de negocio inyectable por Hilt (`RegisterUserUseCase`, `LoginUseCase`, `SaveMedicationUseCase`, `DeleteMedicationUseCase`, `ConfirmDoseUseCase`, `PostponeDoseUseCase`, `EvaluateMissedDoseUseCase`, `RescheduleAllAlarmsUseCase`, `CreateSeniorPatientUseCase` + `DeleteSeniorUseCase`).
- **`data/`** — implementaciones de los repositorios sobre Room (`data/local/dao`, `data/local/entity`) y la base de datos (`MediTrackDatabase`, con migraciones versionadas).
- **`presentation/`** — un `ViewModel` (`@HiltViewModel`) + `Fragment` (`@AndroidEntryPoint`) por pantalla, organizados por rol (`auth/`, `patient/`, `caregiver/`, `senior/`, `camera/`). Los ViewModels que dependen de argumentos de navegación los leen vía `SavedStateHandle` (sin Safe Args — las claves están centralizadas en `presentation/NavArgKeys.kt`).
- **`services/`** — integraciones con el SO: `AlarmScheduler`, `NotificationHelper`, `FileStorageHelper`, y los puntos de entrada de alarmas/background (`MedicationAlarmReceiver`, `MedicationActionReceiver`, `BootReceiver`, `MissedDoseWorker`), todos con inyección de dependencias vía Hilt.
- **`di/`** — módulos de Hilt (`DatabaseModule`, `RepositoryModule`); la mayoría de las clases de `data`/`services`/`domain.usecase` se inyectan directo vía `@Inject constructor` sin necesitar módulo explícito.

Ver [docs/architecture/](docs/architecture/) para el detalle completo, diagramas y las decisiones de diseño (ADRs).

## Estructura del proyecto

```
app/src/main/java/com/devoncats/meditrack/
├── data/            # Room (DAOs, entities, DB) + implementaciones de repositorio
├── di/               # Módulos de Hilt
├── domain/
│   ├── model/        # Entidades y value objects
│   ├── repository/    # Interfaces de repositorio
│   └── usecase/       # Casos de uso
├── presentation/
│   ├── auth/          # Login, registro
│   ├── patient/        # Lista/alta/detalle de medicamentos, alerta de dosis
│   ├── caregiver/      # Dashboard, lista de seniors, alerta de dosis perdida
│   ├── senior/         # Lista de medicamentos y alerta simplificadas
│   └── camera/         # Captura de foto de medicamento
├── services/          # AlarmManager, WorkManager, notificaciones, receivers
└── utils/             # Formateo de fechas/horas, hashing de contraseñas, etc.
```

## Requisitos

- JDK 21
- Android SDK (`compileSdk`/`targetSdk` 37, `minSdk` 28)
- Un emulador o dispositivo físico para los tests instrumentados

## Cómo correr el proyecto

```bash
./gradlew assembleDebug        # compila el APK debug
./gradlew installDebug         # instala en un dispositivo/emulador conectado
```

O abrir el proyecto en Android Studio y ejecutar la configuración `app` sobre un emulador/dispositivo.

## Testing

```bash
./gradlew testDebugUnitTest          # unitarios (JVM)
./gradlew connectedDebugAndroidTest  # instrumentados (requiere emulador/dispositivo)
```

La suite instrumentada cubre los 15 casos de uso funcionales y los requisitos no funcionales de notificaciones y accesibilidad/compatibilidad — ver [docs/testing/](docs/testing/).

## CI

`.github/workflows/ci.yml` corre en cada push/PR contra `main`: compila el APK debug y ejecuta los tests unitarios (los instrumentados no corren en CI por requerir un emulador). Ver [docs/development/](docs/development/) para el detalle del pipeline.

## Documentación

La documentación técnica completa vive en [`docs/`](docs/):

- [`docs/architecture/`](docs/architecture/) — Clean Architecture, capas, flujo de datos, diagramas.
- [`docs/api/`](docs/api/) — casos de uso y repositorios (contratos internos; la app no expone una API HTTP).
- [`docs/database/`](docs/database/) — esquema Room, migraciones, diagrama entidad-relación.
- [`docs/components/`](docs/components/) — ViewModels, Fragments y servicios por pantalla/rol.
- [`docs/development/`](docs/development/) — cómo levantar el entorno, convenciones, CI.
- [`docs/testing/`](docs/testing/) — estrategia de testing y resultados.
- [`docs/adr/`](docs/adr/) — decisiones de arquitectura relevantes (Architecture Decision Records).
- [`docs/diagrams/`](docs/diagrams/) — diagramas Mermaid fuente.
