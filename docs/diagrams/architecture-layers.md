# Capas de arquitectura

Diagrama de componentes con las clases principales de cada capa. Ver [`docs/architecture/README.md`](../architecture/README.md) para la explicación en texto.

```mermaid
graph TD
    subgraph Presentation["presentation/ (Android)"]
        direction TB
        Frag["Fragments (@AndroidEntryPoint)"]
        VM["ViewModels (@HiltViewModel)\nusan SavedStateHandle para nav args"]
    end

    subgraph Domain["domain/ (Kotlin puro, sin deps de Android)"]
        direction TB
        UC["usecase/\nRegisterUser, Login, SaveMedication,\nDeleteMedication, ConfirmDose, PostponeDose,\nEvaluateMissedDose, RescheduleAllAlarms,\nCreateSeniorPatient, DeleteSenior"]
        Model["model/\nUser, Medication, Schedule, WeekDays,\nMedicationLog, MedicationLogStatus,\nEmergencyContact"]
        RepoI["repository/\nUserRepository, MedicationRepository,\nEmergencyContactRepository (interfaces)"]
    end

    subgraph DataServices["data/ + services/ (Android)"]
        direction TB
        RepoImpl["data/repository/\nUserRepositoryImpl, MedicationRepositoryImpl,\nEmergencyContactRepositoryImpl"]
        Room["data/local/\nMediTrackDatabase, DAOs, Entities,\nSessionManager"]
        Svc["services/\nAlarmScheduler, NotificationHelper,\nFileStorageHelper, MedicationAlarmReceiver,\nMedicationActionReceiver, BootReceiver,\nMissedDoseWorker"]
    end

    subgraph DI["di/ (composition root)"]
        DBMod["DatabaseModule (@Provides)"]
        RepoMod["RepositoryModule (@Binds)"]
    end

    Frag --> VM
    VM --> UC
    VM --> RepoI
    UC --> RepoI
    UC --> Svc
    RepoImpl -.implementa.-> RepoI
    RepoImpl --> Room
    Svc --> Room
    Svc --> RepoI

    DBMod -.provee.-> Room
    RepoMod -.liga.-> RepoI
    RepoMod -.a.-> RepoImpl

    style Domain fill:#e8f4ea,stroke:#2d6a3e
    style Presentation fill:#eaf0fb,stroke:#2d4a8a
    style DataServices fill:#fbeeea,stroke:#8a3d2d
    style DI fill:#f5f0fa,stroke:#5a2d8a
```

La regla de dependencia se cumple en un solo punto de fricción intencional: `domain/usecase/` importa `services/` (p. ej. `ConfirmDoseUseCase` usa `AlarmScheduler`) en vez de una interfaz. Es una desviación pragmática aceptada — ver [ADR-0006](../adr/0006-hilt-como-di.md) — porque introducir una interfaz solo para permitir un mock nunca usado en los tests reales (todos los tests de este proyecto son instrumentados contra la base real) no pagaba su complejidad.
