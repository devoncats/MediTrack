# Componentes de `presentation/` y `services/`

Un `Fragment` (`@AndroidEntryPoint`) + un `ViewModel` (`@HiltViewModel`) por pantalla, organizados por rol. Los `ViewModel`s que necesitan un argumento de navegación lo leen de `SavedStateHandle` (claves en `presentation/NavArgKeys.kt`); los que necesitan el usuario logueado usan `SessionManager` inyectado.

## `presentation/auth/`

| Pantalla | ViewModel | Depende de | Nav args |
|---|---|---|---|
| `LoginFragment` | `LoginViewModel` | `LoginUseCase` | — |
| `RegisterFragment` | `RegisterViewModel` | `RegisterUserUseCase` | — |

`LoginViewModel.login()` expone `LoginResult` (`Success(role) | InvalidCredentials`); el `Fragment` navega al grafo de rol correspondiente. `RegisterViewModel.register()` expone `RegisterResult` (`Success | InvalidEmailFormat | EmailAlreadyRegistered`).

## `presentation/patient/`

| Pantalla | ViewModel | Depende de | Nav args |
|---|---|---|---|
| `MedListFragment` | `MedListViewModel` | `MedicationRepository`, `SessionManager` | `SENIOR_USER_ID` (opcional) |
| `MedFormFragment` | `MedFormViewModel` | `MedicationRepository`, `SaveMedicationUseCase`, `SessionManager` | `MEDICATION_ID` (alta si falta), `SENIOR_USER_ID` (opcional) |
| `MedDetailFragment` | `MedDetailViewModel` | `MedicationRepository`, `DeleteMedicationUseCase` | `MEDICATION_ID` |
| `AlertFragment` | `AlertViewModel` | `MedicationRepository`, `ConfirmDoseUseCase`, `PostponeDoseUseCase` | `SCHEDULE_ID` |

`MedListViewModel` es el único `ViewModel` reutilizado por dos Fragments distintos (`MedListFragment` en `patient_graph` y `SeniorDetailFragment` en `caregiver_graph`): si `SENIOR_USER_ID` está presente en los argumentos de navegación, lista los medicamentos de ese senior (vista del cuidador); si no, cae a `SessionManager.getUserId()` (vista propia del paciente). El mismo patrón aplica a `MedFormViewModel`/`MedFormFragment`.

`AlertFragment` es la pantalla completa (confirmar/posponer/descartar), a diferencia de `SeniorAlertFragment` (ver más abajo), que solo permite confirmar.

## `presentation/caregiver/`

| Pantalla | ViewModel | Depende de | Nav args |
|---|---|---|---|
| `DashboardFragment` | `DashboardViewModel` + `MedListViewModel` | `MedicationRepository`, `SessionManager` | — |
| `SeniorListFragment` | `SeniorListViewModel` | `UserRepository`, `MedicationRepository`, `DeleteSeniorUseCase`, `SessionManager` | — |
| `SeniorDetailFragment` | `MedListViewModel` + `EmergencyContactViewModel` | (ver arriba) | `SENIOR_USER_ID`, `SENIOR_NAME` |
| `CreateSeniorPatientFragment` | `CreateSeniorPatientViewModel` | `CreateSeniorPatientUseCase`, `SessionManager` | — |
| `MissedDoseAlertFragment` | `MissedDoseAlertViewModel` | `MedicationRepository`, `UserRepository`, `EmergencyContactRepository` | `LOG_ID` |

`DashboardFragment` combina dos ViewModels: su propia lista de medicamentos (como cuidador, si también gestiona los suyos) y las alertas de dosis perdida de sus seniors a cargo. `SeniorDetailFragment` combina la lista de medicamentos del senior (reutilizando `MedListViewModel`) con su contacto de emergencia. `CreateSeniorPatientViewModel.result` expone `CreateSeniorPatientResult.Success(GeneratedCredentials)` con el usuario+PIN generados, mostrados en un diálogo no cancelable (el cuidador debe anotarlos antes de cerrar).

## `presentation/senior/`

| Pantalla | ViewModel | Depende de | Nav args |
|---|---|---|---|
| `SeniorMedListFragment` | `SeniorMedListViewModel` | `MedicationRepository`, `SessionManager` | — |
| `SeniorAlertFragment` | `SeniorAlertViewModel` | `MedicationRepository`, `ConfirmDoseUseCase` | `SCHEDULE_ID` |

Pantallas deliberadamente simplificadas: `SeniorAlertFragment` no tiene botón de posponer (para reducir la carga cognitiva de la interacción); `SeniorMedListViewModel` siempre opera sobre el usuario logueado (nunca recibe `SENIOR_USER_ID`, a diferencia de `MedListViewModel`).

## `presentation/camera/`

`CameraFragment` no tiene `ViewModel` propio — usa `CameraHelper` directo y se comunica con `MedFormFragment` vía `Fragment Result API` (`setFragmentResult`/`setFragmentResultListener`, clave `CameraFragment.RESULT_KEY`). Es el único Fragment del proyecto sin `@AndroidEntryPoint`, porque no inyecta nada.

## `services/`

| Clase | Rol | Inyección |
|---|---|---|
| `AlarmScheduler` | Programa/cancela alarmas exactas (`AlarmManager`) y encola la verificación de dosis perdida (`WorkManager`). | `@Inject constructor` |
| `NotificationHelper` | Arma y dispara notificaciones (alarma de medicación, dosis perdida). | `@Inject constructor` |
| `FileStorageHelper` | Guarda/lee/borra la foto del medicamento en almacenamiento interno. | `@Inject constructor` (también se instancia ad hoc en `MedFormFragment` para leer una foto sin pasar por el ViewModel) |
| `MedicationAlarmReceiver` | Recibe el disparo de una alarma, crea/reutiliza el `MedicationLog` y muestra la notificación. | `@AndroidEntryPoint`, campos `@Inject` |
| `MedicationActionReceiver` | Recibe las acciones "Confirmar"/"Posponer" de la notificación. | `@AndroidEntryPoint`, campos `@Inject` |
| `BootReceiver` | Reprograma todas las alarmas tras un reinicio del dispositivo (`AlarmManager` no persiste alarmas entre reinicios). | `@AndroidEntryPoint`, campo `@Inject` |
| `MissedDoseWorker` | `CoroutineWorker` que evalúa si una dosis quedó sin confirmar. | `@HiltWorker` + `@AssistedInject` |

Ver [`docs/diagrams/confirmar-dosis.md`](../diagrams/confirmar-dosis.md) y [`docs/diagrams/dosis-perdida.md`](../diagrams/dosis-perdida.md) para el flujo completo de estos servicios.
