# Secuencia: confirmar una dosis

Hay dos puntos de entrada equivalentes — desde la acción de la notificación (sin abrir la app) y desde la pantalla de alerta dentro de la app — y ambos delegan en el mismo `ConfirmDoseUseCase`. El diagrama muestra el camino de la notificación, que es el más largo.

```mermaid
sequenceDiagram
    actor U as Usuario
    participant Notif as Notificación (Android)
    participant Rcv as MedicationActionReceiver
    participant UC as ConfirmDoseUseCase
    participant Repo as MedicationRepository
    participant DB as Room
    participant Alarm as AlarmScheduler
    participant WM as WorkManager

    U->>Notif: toca "Confirmar"
    Notif->>Rcv: onReceive(ACTION_CONFIRM, logId, scheduleId)
    Note over Rcv: Hilt ya inyectó confirmDoseUseCase\nvia @AndroidEntryPoint
    Rcv->>Rcv: goAsync() + CoroutineScope(IO).launch
    Rcv->>UC: invoke(logId, scheduleId)
    UC->>Repo: getLogById(logId)
    Repo->>DB: SELECT medication_logs WHERE id
    DB-->>Repo: MedicationLogEntity
    Repo-->>UC: MedicationLog?
    UC->>Repo: updateLog(log.copy(status=CONFIRMED, confirmedAt=now))
    Repo->>DB: UPDATE medication_logs
    UC->>Repo: getScheduleById(scheduleId)
    Repo-->>UC: Schedule
    UC->>Alarm: schedule(scheduleId, medicationId, time, daysOfWeek)
    Alarm->>Alarm: setExactAndAllowWhileIdle(nextTriggerMillis)
    Alarm->>WM: enqueueUniqueWork(missedDoseWorkName, REPLACE)
    Note over WM: reemplaza el MissedDoseWorker\nque estaba pendiente para esta dosis
    Rcv->>Notif: NotificationManagerCompat.cancel(logId)
```

Cualquier pantalla observando `Flow<List<MedicationLog>>` (p. ej. el historial de hoy en `MedDetailViewModel`) recibe la actualización automáticamente en cuanto Room commitea el `UPDATE`, sin necesidad de refrescar manualmente.
