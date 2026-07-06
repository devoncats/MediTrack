# Pruebas funcionales de los 15 casos de uso (issue #34)

**Entorno:** emulador Pixel 10 Pro XL, Android 17 (API 37), `sdk_gphone16k_x86_64`.
**Fecha:** 2026-07-05.
**Metodo:** suite instrumentada (`./gradlew connectedDebugAndroidTest`, Espresso) mas una
verificacion manual en vivo sobre el emulador para el flujo Paciente -> Cuidador -> Paciente Mayor
(capturas en [screenshots/](screenshots/)).

## Resultado global

**90/90 tests instrumentados pasaron** (0 fallos, 0 omitidos) en la ultima ejecucion completa del
suite. **15/15 CU en estado Cumplido** (supera el criterio de aceptacion de al menos 13/15).

| CU | Descripcion | Resultado | Evidencia |
|---|---|---|---|
| CU-001 | Login por actor | Cumplido | `LoginFragmentTest` (3/3) + [captura](screenshots/cu001-login.png) |
| CU-002 | Registro Paciente/Cuidador | Cumplido | `RegisterFragmentTest` (5/5) + [captura](screenshots/cu002-registro-form.png) |
| CU-003 | Ver lista de medicamentos | Cumplido | `MedListFragmentTest` (4/4) + [captura](screenshots/cu003-lista-medicamentos-vacia.png) |
| CU-004 | Agregar medicamento | Cumplido | `MedFormFragmentTest` (3/3) + [form](screenshots/cu004-agregar-medicamento-form.png) / [guardado](screenshots/cu004-medicamento-guardado.png) |
| CU-005 | Editar medicamento | Cumplido | `MedFormEditFragmentTest` (4/4) |
| CU-006 | Eliminar medicamento | Cumplido | `MedDetailFragmentTest` (5/5) + [detalle](screenshots/cu005-cu006-detalle-medicamento.png) |
| CU-007 | Confirmar toma | Cumplido | `AlertFragmentTest.tappingPendingChip_opensAlertWithMedicationInfoAndConfirmingUpdatesListImmediately` |
| CU-008 | Posponer toma | Cumplido | `AlertFragmentTest.postponing_reschedulesAlarmAndLeavesLogUntouched` |
| CU-009 | Ver lista de PMs | Cumplido | `SeniorListFragmentTest` (3/3) + [captura](screenshots/cu009-lista-pm.png) |
| CU-010 | Crear PM con credenciales | Cumplido | `CreateSeniorPatientFragmentTest` (2/2) + [captura](screenshots/cu010-credenciales-pm.png) |
| CU-011 | Eliminar PM | Cumplido | `SeniorListFragmentTest.deleteSenior_withConfirmation_cascadesMedicationsSchedulesAndLogs` |
| CU-012 | Alerta de toma omitida | Cumplido | `MissedDoseAlertFragmentTest` (4/4) + `MissedDoseWorkerTest` (3/3) |
| CU-013 | Llamada de emergencia | Cumplido | `MissedDoseAlertFragmentTest.callButton_launchesActionCallWithRegisteredNumber` |
| CU-014 | PM ve sus medicamentos | Cumplido | `SeniorMedListFragmentTest` (5/5) + [captura](screenshots/cu014-pm-lista-medicamentos.png) |
| CU-015 | PM confirma toma | Cumplido | `SeniorAlertFragmentTest` (2/2) |

Capturas adicionales del recorrido manual: [dashboard del Cuidador](screenshots/cu-dashboard-cuidador.png).

## Bug encontrado y corregido durante esta verificacion

Al correr el suite completo (90 tests) de una sola vez -algo que nunca se habia hecho antes; no
hay CI configurado en el repo y cada dev corria sus propias clases de test- el proceso de la app
**crasheaba de forma intermitente** alrededor del test 10/90, deteniendo toda la ejecucion:

```
java.lang.RuntimeException: Exception while computing database live data.
	at androidx.room.RoomTrackingLiveData.refresh(...)
Caused by: kotlinx.coroutines.JobCancellationException: Job was cancelled
```

**Causa raiz:** el helper compartido `getOrAwaitValue()` (`LiveDataTestUtils.kt`, usado por casi
todos los DAO/repository tests) hace `observeForever` y retorna en cuanto llega la primera
emision, pero Room programa su siguiente refresco de invalidacion de forma asincrona justo
despues de esa emision. Si el `@After` del test cierra la base de datos en memoria antes de que
ese refresco en vuelo termine, el query se ejecuta contra una conexion ya cerrada en un hilo de
fondo sin manejo de excepciones, y crashea el proceso completo de instrumentacion (no solo el
test en curso).

**Fix:** se agrego `InstrumentationRegistry.getInstrumentation().waitForIdleSync()` en
`getOrAwaitValue()` despues del `latch.await(...)`, para drenar el hilo principal y dejar que
Room termine de asentar el invalidation tracker antes de que el test siguiente cierre su propia
base de datos. Tras el fix, **90/90 tests pasan de forma consistente** en corridas repetidas.
Es un cambio acotado a codigo de test (`app/src/androidTest`), no afecta produccion.

## Notas sobre el recorrido manual

El recorrido de captura de pantalla registro ademas datos reales generados en el proceso, utiles
como set de demo (ver issue #39):
- Paciente: `maria.gomez@meditrack.demo` / `Demo1234`
- Cuidador: `carlos.perez@meditrack.demo` / `Demo1234`
- Paciente Mayor generado por el Cuidador: usuario `pm_rosa_martinez_2`, PIN `525919`
