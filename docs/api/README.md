# API interna: casos de uso y repositorios

MediTrack no expone una API HTTP — es una app local con Room como única fuente de datos. Este documento describe el "API" interno del dominio: los **9 casos de uso** (la superficie que consume `presentation/`) y las **3 interfaces de repositorio** que ellos consumen (la superficie que consume `data/`).

Todos los casos de uso son `class Xxx @Inject constructor(...)` con `operator fun invoke(...)`, así que se llaman como `xxxUseCase(args)`.

## Casos de uso

### `RegisterUserUseCase`

`suspend operator fun invoke(name: String, email: String, password: String, role: UserRole): RegisterResult`

Registra un `PATIENT` o `CAREGIVER` nuevo. Valida formato de email (`android.util.Patterns`) y unicidad de `username` contra `UserRepository`. `RegisterResult` es `Success | InvalidEmailFormat | EmailAlreadyRegistered`.

### `LoginUseCase`

`suspend operator fun invoke(username: String, password: String): LoginResult`

Verifica credenciales (`PasswordHasher.verify`) y, si son válidas, persiste la sesión vía `SessionManager.saveSession()`. `LoginResult` es `Success(role: UserRole) | InvalidCredentials`.

### `SaveMedicationUseCase`

`suspend operator fun invoke(ownerUserId, existingMedicationId: Long?, name, dose, frequency, instructions, selectedDays, selectedTimes, capturedPhotoUri): SaveMedicationResult`

Crea o actualiza un medicamento y sus horarios. `existingMedicationId = null` significa alta; si no es null, es edición. Reemplaza todos los `Schedule` existentes por los nuevos (cancela las alarmas viejas antes de programar las nuevas), y gestiona la foto (guarda la nueva, borra la anterior si había). `SaveMedicationResult` es `Success(medicationId) | ValidationError` (nombre/dosis/frecuencia vacíos, o sin días/horarios seleccionados).

### `DeleteMedicationUseCase`

`suspend operator fun invoke(medicationId: Long): Boolean`

Cancela las alarmas de todos los horarios del medicamento, lo borra (Room hace CASCADE sobre `schedules` y `medication_logs`) y borra su foto si tenía. Devuelve `false` si el medicamento no existe.

### `ConfirmDoseUseCase`

`suspend operator fun invoke(logId: Long, scheduleId: Long): Boolean`

Marca un `MedicationLog` como `CONFIRMED` y reprograma la siguiente ocurrencia del horario (lo que también reemplaza el `MissedDoseWorker` pendiente para esa dosis). Punto único que reemplazó tres copias casi idénticas que existían en `AlertViewModel`, `SeniorAlertViewModel` y `MedicationActionReceiver` antes del refactor (ver [ADR-0005](../adr/0005-capa-de-casos-de-uso.md)).

### `PostponeDoseUseCase`

`operator fun invoke(scheduleId: Long, medicationId: Long, logId: Long)`

No es `suspend`: solo delega en `AlarmScheduler.postpone()`, que reprograma la alarma 15 minutos después (`AlarmScheduler.POSTPONE_MINUTES`) reutilizando el mismo `logId` (para no duplicar el registro de la dosis).

### `EvaluateMissedDoseUseCase`

`suspend operator fun invoke(medicationId: Long, scheduleId: Long)`

Corre 30 minutos después de cada alarma (encolado por `AlarmScheduler.enqueueMissedDoseCheck`). Si la dosis sigue `PENDING`, la marca `MISSED` y, si el dueño es `SENIOR_PATIENT`, notifica al cuidador (`NotificationHelper.showMissedDoseCaregiverNotification`). Siempre reprograma la siguiente ocurrencia al final, haya o no una dosis pendiente que evaluar.

### `RescheduleAllAlarmsUseCase`

`suspend operator fun invoke()`

Reprograma **todos** los horarios de la base (`MedicationRepository.getAllSchedules()`). Lo llama `BootReceiver` tras un reinicio del dispositivo, ya que `AlarmManager` no persiste las alarmas entre reinicios.

### `CreateSeniorPatientUseCase`

`suspend operator fun invoke(caregiverId: Long, name: String, contactName: String, contactPhone: String): CreateSeniorPatientResult`

Da de alta un `SENIOR_PATIENT`: genera un `username` sintético (`pm_<nombre_normalizado>_<caregiverId>`, con sufijo numérico si colisiona) y un PIN de 6 dígitos vía `SecureRandom` (nunca `kotlin.random.Random` — el PIN es una credencial de acceso). Opcionalmente crea su contacto de emergencia. `CreateSeniorPatientResult` es `Success(GeneratedCredentials(username, pin)) | ValidationError` (nombre vacío).

### `DeleteSeniorUseCase`

`suspend operator fun invoke(senior: User)`

Borra un senior y todo lo asociado. Reutiliza `DeleteMedicationUseCase` en un loop por cada medicamento del senior en vez de reimplementar la limpieza de alarmas/fotos — el borrado final del `User` deja que Room haga CASCADE sobre lo que ya no tiene medicamentos.

## Repositorios

### `UserRepository`

```kotlin
suspend fun insert(user: User): Long
suspend fun findByUsername(username: String): User?
suspend fun findById(id: Long): User?
suspend fun delete(user: User)
fun observeSeniorPatientsByCaregiver(caregiverId: Long): Flow<List<User>>
```

### `MedicationRepository`

Cubre tres entidades relacionadas (`Medication`, `Schedule`, `MedicationLog`) porque siempre se consultan juntas y separarlas en tres repositorios habría forzado a los casos de uso a coordinar transacciones a mano.

```kotlin
// Medication
suspend fun insertMedication(medication: Medication): Long
suspend fun updateMedication(medication: Medication)
suspend fun deleteMedication(medication: Medication)
suspend fun getMedicationById(id: Long): Medication?
suspend fun getMedicationsByOwner(ownerUserId: Long): List<Medication>
fun observeMedicationsByOwner(ownerUserId: Long): Flow<List<Medication>>
fun observeMedicationById(id: Long): Flow<Medication?>

// Schedule
suspend fun insertSchedule(schedule: Schedule): Long
suspend fun updateSchedule(schedule: Schedule)
suspend fun deleteSchedule(schedule: Schedule)
suspend fun getSchedulesByMedication(medicationId: Long): List<Schedule>
suspend fun getScheduleById(id: Long): Schedule?
suspend fun getAllSchedules(): List<Schedule>
fun observeSchedulesByMedication(medicationId: Long): Flow<List<Schedule>>

// MedicationLog
suspend fun insertLog(log: MedicationLog): Long
suspend fun updateLog(log: MedicationLog)
suspend fun deleteLog(log: MedicationLog)
suspend fun getLogById(id: Long): MedicationLog?
suspend fun getLatestPendingLogForSchedule(scheduleId: Long): MedicationLog?
fun observeLogsByMedication(medicationId: Long): Flow<List<MedicationLog>>
fun observeLogsByMedicationBetween(medicationId: Long, startInclusive: Long, endExclusive: Long): Flow<List<MedicationLog>>
fun observeLogsByOwnerBetween(ownerUserId: Long, startInclusive: Long, endExclusive: Long): Flow<List<MedicationLog>>
fun observeMissedDoseAlertsForCaregiver(caregiverId: Long): Flow<List<MissedDoseAlert>>
fun observeTodayLogStatusesForCaregiverSeniors(caregiverId: Long, startInclusive: Long, endExclusive: Long): Flow<List<SeniorDoseStatus>>
```

### `EmergencyContactRepository`

```kotlin
suspend fun insert(contact: EmergencyContact): Long
suspend fun update(contact: EmergencyContact)
suspend fun findByUserId(userId: Long): EmergencyContact?
```

Un senior tiene a lo sumo un contacto de emergencia (`findByUserId` no es una lista); no hay `delete` porque el contacto se borra por CASCADE cuando se borra el senior.
