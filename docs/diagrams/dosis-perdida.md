# Secuencia: evaluación de dosis no confirmada

Cada vez que se programa una alarma (`AlarmScheduler.schedule()`), se encola además un `MissedDoseWorker` con un delay de `(tiempo hasta la alarma) + 30 minutos` (`MISSED_DOSE_CHECK_DELAY_MINUTES`). Si para entonces la dosis sigue `PENDING`, se marca `MISSED`.

```mermaid
sequenceDiagram
    participant Alarm as AlarmScheduler
    participant WM as WorkManager
    participant Worker as MissedDoseWorker
    participant UC as EvaluateMissedDoseUseCase
    participant Repo as MedicationRepository
    participant UserRepo as UserRepository
    participant DB as Room
    participant Notif as NotificationHelper

    Alarm->>WM: enqueueUniqueWork(missedDoseWorkName, REPLACE, delay)
    Note over WM: ~30 min después de la hora nominal de la dosis
    WM->>Worker: doWork() (HiltWorkerFactory ya inyectó\nel caso de uso via @AssistedInject)
    Worker->>UC: invoke(medicationId, scheduleId)
    UC->>Repo: getMedicationById / getScheduleById
    Repo->>DB: SELECT
    DB-->>Repo: Medication, Schedule
    UC->>Repo: getLatestPendingLogForSchedule(scheduleId)
    Repo->>DB: SELECT ... WHERE status='PENDING' ORDER BY scheduledDatetime DESC LIMIT 1
    alt hay una dosis PENDING
        DB-->>Repo: MedicationLogEntity
        UC->>Repo: updateLog(log.copy(status=MISSED))
        UC->>UserRepo: findById(medication.ownerUserId)
        UserRepo-->>UC: User (owner)
        alt owner.role == SENIOR_PATIENT
            UC->>Notif: showMissedDoseCaregiverNotification(...)
            Note over Notif: notificación local en el mismo dispositivo —\nno hay backend/FCM para avisar al cuidador real
        end
    else no hay dosis pendiente
        Note over UC: la dosis ya fue confirmada o pospuesta;\nno hay nada que marcar
    end
    UC->>Alarm: schedule(scheduleId, medicationId, time, daysOfWeek)
    Note over Alarm: siempre reprograma la siguiente ocurrencia,\nhaya o no marcado algo como MISSED
    Worker-->>WM: Result.success()
```
