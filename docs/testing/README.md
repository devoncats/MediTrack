# Testing

## Estrategia

El proyecto **no usa dobles de prueba** (mocks/fakes) en ningún nivel — tanto los tests unitarios como los instrumentados corren contra implementaciones reales:

- Los **unitarios** (`app/src/test/`, JVM puro) cubren funciones y clases de dominio sin dependencias de Android: hashing de contraseñas, agregación de estado de dosis, cálculo del próximo disparo de una alarma, generación de usuario/PIN sintéticos.
- Los **instrumentados** (`app/src/androidTest/`, corren en un emulador/dispositivo real) cubren todo lo que toca Room, `AlarmManager`, `WorkManager`, notificaciones o navegación — que es la mayor parte del comportamiento de la app. Usan la base de datos real (`MediTrackDatabase.getInstance(context)`, sembrando y limpiando datos de prueba explícitamente en cada test) en vez de una base en memoria con dobles.

Esta decisión es deliberada: para una app cuyo comportamiento central son alarmas exactas, WorkManager y persistencia, un mock de `AlarmManager` o de un DAO habría dado falsa confianza — el objetivo es verificar el comportamiento real del sistema operativo y de Room, no una simulación de ellos.

## Cómo correr los tests

```bash
./gradlew testDebugUnitTest           # unitarios, no requiere emulador
./gradlew connectedDebugAndroidTest   # instrumentados, requiere emulador/dispositivo conectado
```

Los tests instrumentados no corren en CI (ver [`docs/development/`](../development/README.md)) — se corren localmente antes de cada PR relevante.

### Recomendación sobre el emulador

Bajo corridas repetidas y prolongadas de la suite instrumentada completa, el emulador puede degradarse (aparecen fallos intermitentes en tests sin relación entre sí, no reproducibles corriendo el test aislado). Si se observan fallos que no tienen que ver con el cambio que se está probando, un cold boot del emulador (`adb emu kill` + relanzar con `-no-snapshot-load -no-snapshot-save`) antes de la siguiente corrida suele eliminarlos. Esto se verificó empíricamente durante el desarrollo de este proyecto: corridas con 2-3 fallos "fantasma" volvían a 94/94 en verde tras un cold boot, sin ningún cambio de código de por medio.

## Inventario de tests

**Unitarios** (`app/src/test/`, 4 archivos):

| Archivo | Cubre |
|---|---|
| `PasswordHasherTest` | Hash/verify de contraseñas (salt aleatorio, SHA-256). |
| `MedicationLogStatusTest` | `MedicationLogStatus.aggregate()` (MISSED > PENDING > CONFIRMED). |
| `AlarmSchedulerNextTriggerTest` | `AlarmScheduler.nextTriggerMillis()` — cálculo del próximo disparo dado un `LocalTime`/`WeekDays`. |
| `CreateSeniorPatientUseCaseTest` | `buildUsername()`/`generatePin()` — normalización de nombre y aleatoriedad del PIN. |

**Instrumentados** (`app/src/androidTest/`, 30 archivos — DAOs, repositorio, servicios, receivers, worker, y un `*FragmentTest`/`*Test` por flujo de UI relevante en `auth/`, `patient/`, `caregiver/` y `senior/`), 94 tests en total. Áreas cubiertas:

- **DAOs y repositorio**: `UserDaoTest`, `MedicationDaoTest`, `ScheduleDaoTest`, `MedicationLogDaoTest`, `MedicationRepositoryTest`.
- **Servicios**: `AlarmSchedulerTest`, `NotificationHelperTest`, `FileStorageHelperTest`, `SessionManagerTest`.
- **Receivers y Worker**: `MedicationAlarmReceiverTest`, `MedicationActionReceiverTest`, `BootReceiverTest`, `MissedDoseWorkerTest`.
- **Casos de uso**: `RescheduleAllAlarmsUseCaseTest`.
- **Flujos de UI por rol**: `LoginFragmentTest`, `RegisterFragmentTest` (auth); `MedListFragmentTest`, `MedFormFragmentTest`, `MedFormEditFragmentTest`, `MedDetailFragmentTest`, `AlertFragmentTest`, `CameraFragmentTest` (patient); `DashboardFragmentTest`, `SeniorListFragmentTest`, `SeniorDetailFragmentTest`, `CreateSeniorPatientFragmentTest`, `MissedDoseAlertFragmentTest` (caregiver); `SeniorMedListFragmentTest`, `SeniorAlertFragmentTest` (senior); `MainActivitySessionTest` (redirección según sesión activa).

## Evidencia histórica de pruebas funcionales

[`docs/test-results/`](../test-results/) contiene la evidencia de pruebas funcionales de los 15 casos de uso y los requisitos no funcionales (notificaciones, accesibilidad/compatibilidad), fechada **2026-07-05** — **anterior** al refactor de Clean Architecture/DDD/Hilt y a varias correcciones de bugs documentadas en este mismo repositorio. En ese momento la suite tenía 90/90 tests en verde; a la fecha de esta documentación, tras el refactor completo, son **94/94**. El comportamiento funcional descrito ahí (los 15 CU) no cambió — los tests nuevos son cobertura adicional introducida junto con las correcciones de bugs y el refactor, no una re-verificación de lo ya documentado.
