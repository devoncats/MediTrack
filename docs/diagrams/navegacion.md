# Navegación

Un único `NavHostFragment` (`main_nav_graph`) incluye los 4 grafos por rol. `MainActivity` redirige al grafo correspondiente según el rol de la sesión activa (`SessionManager`) en el arranque en frío; dentro de cada grafo, la navegación es la estándar del Navigation Component (sin Safe Args — los argumentos van por `Bundle` con claves centralizadas en `NavArgKeys`, leídas en destino vía `SavedStateHandle`).

```mermaid
graph TD
    Start([App arranca]) --> Check{"¿Sesión activa?\n(SessionManager.isLoggedIn)"}
    Check -- no --> Auth
    Check -- sí --> RoleCheck{role}
    RoleCheck -- PATIENT --> Patient
    RoleCheck -- CAREGIVER --> Caregiver
    RoleCheck -- SENIOR_PATIENT --> Senior

    subgraph Auth["auth_graph"]
        Login[LoginFragment]
        Register[RegisterFragment]
        Login <--> Register
    end
    Login -- login exitoso, por rol --> RoleCheck

    subgraph Patient["patient_graph"]
        MedList[MedListFragment]
        MedForm[MedFormFragment]
        MedDetail[MedDetailFragment]
        Alert[AlertFragment]
        Camera[CameraFragment]
        MedList --> MedForm
        MedList --> MedDetail
        MedDetail --> MedForm
        MedForm --> Camera
        MedList -.notificación/deep link.-> Alert
    end

    subgraph Caregiver["caregiver_graph"]
        Dashboard[DashboardFragment]
        SeniorList[SeniorListFragment]
        SeniorDetail[SeniorDetailFragment]
        CreateSenior[CreateSeniorPatientFragment]
        MissedDoseAlert[MissedDoseAlertFragment]
        Dashboard --> SeniorList
        Dashboard --> MedDetail2[MedDetailFragment]
        Dashboard --> MedForm2[MedFormFragment]
        Dashboard -.notificación.-> MissedDoseAlert
        SeniorList --> SeniorDetail
        SeniorList --> CreateSenior
        SeniorDetail --> MedForm2
        SeniorDetail --> MedDetail2
    end

    subgraph Senior["senior_graph"]
        SeniorMedList[SeniorMedListFragment]
        SeniorAlert[SeniorAlertFragment]
        SeniorMedList -.notificación/deep link.-> SeniorAlert
    end
```

**Puntos clave:**

- `CameraFragment` es un destino más dentro de `patient_graph` (no un grafo incluido aparte); se comunica con `MedFormFragment` vía Fragment Result API, no vía argumentos de navegación.
- El logout (`Fragment.logout()` en `presentation/SessionNavigation.kt`, llamado desde cada pantalla raíz de rol) limpia la sesión (`SessionManager.clearSession()`) y navega de vuelta a `auth_graph` con `popUpTo` para vaciar el back stack del rol anterior.
- Las alertas de dosis (`AlertFragment` / `SeniorAlertFragment` / `MissedDoseAlertFragment`) también se alcanzan por **deep link** desde una notificación (`NotificationHelper` usa `NavDeepLinkBuilder`), no solo por navegación interna — por eso reciben su argumento (`scheduleId`/`logId`) tanto desde un `Bundle` de navegación normal como desde el intent del deep link, indistintamente (ambos terminan en `Fragment.arguments`, que es lo que lee `SavedStateHandle`).
- `SeniorDetailFragment` (dentro de `caregiver_graph`) reutiliza las mismas pantallas `MedListFragment`/`MedFormFragment`/`MedDetailFragment` de `patient_graph` — el cuidador ve/edita los medicamentos del senior con la misma UI, pasando `seniorUserId` como argumento en vez de operar sobre el usuario logueado.
