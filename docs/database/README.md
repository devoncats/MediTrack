# Base de datos

MediTrack persiste todo en una única base **Room/SQLite** (`meditrack.db`), definida en `MediTrackDatabase` (versión de esquema actual: **3**, `exportSchema = true` — los JSON de esquema quedan en `app/schemas/`).

## Diagrama entidad-relación

Ver [`docs/diagrams/er-diagram.md`](../diagrams/er-diagram.md) para el diagrama Mermaid completo.

## Tablas

### `users`

| Columna | Tipo | Notas |
|---|---|---|
| `id` | `INTEGER` PK autogenerado | |
| `name` | `TEXT` | |
| `username` | `TEXT` | **único** (`index_users_username`). Para `PATIENT`/`CAREGIVER` es el email real; para `SENIOR_PATIENT` es un identificador sintético (`pm_<nombre>_<caregiverId>`) generado por `CreateSeniorPatientUseCase` — nunca fue un email real, de ahí el rename en la migración v2→v3 (ver [ADR-0002](../adr/0002-username-no-email.md)). |
| `passwordHash` | `TEXT` | `salt:hash` en Base64, SHA-256 con salt aleatorio (`PasswordHasher`). |
| `role` | `TEXT` | Enum `UserRole` (`PATIENT`, `CAREGIVER`, `SENIOR_PATIENT`) vía `Converters`. |
| `caregiverId` | `INTEGER?` | FK a `users.id`, `ON DELETE SET NULL`. Solo lo usan los `SENIOR_PATIENT` (apunta a su cuidador). Índice `index_users_caregiverId`. |

### `medications`

| Columna | Tipo | Notas |
|---|---|---|
| `id` | `INTEGER` PK autogenerado | |
| `name`, `dose`, `frequency` | `TEXT` | `frequency` es texto libre descriptivo (p. ej. "Cada 8 horas"), no controla la programación de alarmas — eso lo hace `schedules`. |
| `instructions` | `TEXT?` | |
| `ownerUserId` | `INTEGER` | FK a `users.id`, `ON DELETE CASCADE`. Índice `index_medications_ownerUserId`. |
| `photoUri` | `TEXT?` | Ruta relativa dentro del almacenamiento interno de la app (ver `FileStorageHelper`), no una URI de content provider persistida. |
| `createdAt` | `INTEGER` | epoch millis. |

### `schedules`

| Columna | Tipo | Notas |
|---|---|---|
| `id` | `INTEGER` PK autogenerado | |
| `medicationId` | `INTEGER` | FK a `medications.id`, `ON DELETE CASCADE`. Índice `index_schedules_medicationId`. |
| `time` | `TEXT` | `"HH:mm"` (p. ej. `"08:00"`). En dominio es `LocalTime` — la conversión vive en `MedicationRepositoryImpl` vía `LocalTime.toHHmm()`/`String.toLocalTime()` (`utils/DateUtils.kt`). |
| `daysOfWeek` | `TEXT` | CSV de códigos de 3 letras (`"MON,TUE,WED"`). En dominio es `WeekDays(Set<DayOfWeek>)` — la conversión vive en `WeekDays.toCsv()`/`String.toWeekDays()` (`utils/DayOfWeekCodes.kt`). |

Un medicamento puede tener **varios** horarios (p. ej. 08:00 y 20:00 para "cada 12 horas").

### `medication_logs`

| Columna | Tipo | Notas |
|---|---|---|
| `id` | `INTEGER` PK autogenerado | |
| `medicationId` | `INTEGER` | FK a `medications.id`, `ON DELETE CASCADE`. Índice `index_medication_logs_medicationId`. |
| `scheduleId` | `INTEGER?` | FK a `schedules.id`, `ON DELETE SET NULL`. Índice `index_medication_logs_scheduleId`. Nullable solo por compatibilidad con filas migradas de v1 (que no tenían esta columna) — toda fila nueva la trae. Sin este dato, un medicamento con dos horarios podía marcar como "perdida" la dosis equivocada. |
| `scheduledDatetime` | `INTEGER` | epoch millis. La hora **nominal** de la dosis (la que dictaba el horario), no necesariamente el instante real en que sonó la alarma (que puede demorarse por Doze). |
| `confirmedAt` | `INTEGER?` | epoch millis; `null` mientras esté `PENDING`/`MISSED`. |
| `status` | `TEXT` | Enum `MedicationLogStatus` (`PENDING`, `CONFIRMED`, `MISSED`) vía `Converters`. |

Cada disparo de alarma (o cada dosis pospuesta) genera/reutiliza una fila acá — es el registro histórico de tomas.

### `emergency_contacts`

| Columna | Tipo | Notas |
|---|---|---|
| `id` | `INTEGER` PK autogenerado | |
| `userId` | `INTEGER` | FK a `users.id`, `ON DELETE CASCADE`. Índice `index_emergency_contacts_userId`. |
| `name`, `phone` | `TEXT` | |

A lo sumo un contacto por usuario (en la práctica, solo se usa para `SENIOR_PATIENT`).

## Migraciones

| Versión | Migración | Motivo |
|---|---|---|
| 1 → 2 | `MIGRATION_1_2` | Agrega `medication_logs.scheduleId` (+ índice). Antes, un medicamento con más de un horario podía hacer que el `MissedDoseWorker` evaluara la dosis del horario equivocado. |
| 2 → 3 | `MIGRATION_2_3` | Renombra `users.email` → `users.username` (`ALTER TABLE ... RENAME COLUMN`) y recrea el índice único (`DROP INDEX index_users_email` + `CREATE UNIQUE INDEX index_users_username`). El rename de columna por sí solo no alcanza: Room deriva el nombre del índice a partir del nombre de columna y su validación de esquema falla si el índice no se recrea explícitamente bajo el nuevo nombre. Ver [ADR-0002](../adr/0002-username-no-email.md). |

Ambas migraciones están registradas en `MediTrackDatabase.getInstance()` vía `.addMigrations(MIGRATION_1_2, MIGRATION_2_3)` — no hay `fallbackToDestructiveMigration`, así que una migración faltante rompe el build de tests instrumentados en vez de perder datos silenciosamente.

## DAOs

Un DAO por entidad (`UserDao`, `MedicationDao`, `ScheduleDao`, `MedicationLogDao`, `EmergencyContactDao`), todos `interface` con `suspend fun` para escrituras/lecturas puntuales y `Flow<T>` para lecturas reactivas — Room genera la implementación y emite un nuevo valor en el `Flow` automáticamente cuando cambian las tablas involucradas en la consulta.

Dos consultas de `MedicationLogDao` hacen `JOIN` a través de tres tablas para servir directamente las pantallas del cuidador sin lógica adicional en el repositorio:

- `observeMissedDoseAlertsForCaregiver`: dosis `MISSED` de todos los seniors a cargo de un cuidador.
- `observeTodayLogStatusesForCaregiverSeniors`: estado de "hoy" de cada senior a cargo, para el listado del cuidador.

## `Converters`

Room no serializa enums nativamente; `Converters` los mapea a `String` vía `.name`/`.valueOf()` para `UserRole` y `MedicationLogStatus`. Los value objects del dominio (`LocalTime`, `WeekDays`) **no** pasan por `@TypeConverter` — se convierten explícitamente en el mapper de `MedicationRepositoryImpl` (`ScheduleEntity.toDomain()` / `Schedule.toEntity()`), a propósito: el dominio no debe saber de formato de string, y esa conversión es responsabilidad de la capa de datos, no de Room.
