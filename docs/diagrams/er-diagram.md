# Diagrama entidad-relación

Ver el detalle de cada columna en [`docs/database/README.md`](../database/README.md).

```mermaid
erDiagram
    USERS ||--o{ USERS : "caregiverId (SET NULL)"
    USERS ||--o{ MEDICATIONS : "ownerUserId (CASCADE)"
    USERS ||--o{ EMERGENCY_CONTACTS : "userId (CASCADE)"
    MEDICATIONS ||--o{ SCHEDULES : "medicationId (CASCADE)"
    MEDICATIONS ||--o{ MEDICATION_LOGS : "medicationId (CASCADE)"
    SCHEDULES |o--o{ MEDICATION_LOGS : "scheduleId (SET NULL)"

    USERS {
        long id PK
        string name
        string username UK "sintetico para SENIOR_PATIENT"
        string passwordHash "salt:hash SHA-256"
        string role "PATIENT | CAREGIVER | SENIOR_PATIENT"
        long caregiverId FK "nullable, solo SENIOR_PATIENT"
    }
    MEDICATIONS {
        long id PK
        string name
        string dose
        string frequency "texto libre"
        string instructions "nullable"
        long ownerUserId FK
        string photoUri "nullable"
        long createdAt "epoch millis"
    }
    SCHEDULES {
        long id PK
        long medicationId FK
        string time "HH:mm"
        string daysOfWeek "CSV: MON,TUE,..."
    }
    MEDICATION_LOGS {
        long id PK
        long medicationId FK
        long scheduleId FK "nullable"
        long scheduledDatetime "epoch millis, hora nominal"
        long confirmedAt "nullable, epoch millis"
        string status "PENDING | CONFIRMED | MISSED"
    }
    EMERGENCY_CONTACTS {
        long id PK
        long userId FK
        string name
        string phone
    }
```

**Notas de cardinalidad:**

- Un `medication` puede tener **varios** `schedules` (medicamentos con más de una toma diaria).
- Cada disparo de alarma (o cada postergación) genera o reutiliza una fila en `medication_logs` — es el historial de tomas.
- `emergency_contacts.userId` es 1:1 en la práctica (a lo sumo un contacto por usuario), aunque el esquema no lo fuerza con un índice único — solo se consulta con `LIMIT 1`.
- `users.caregiverId` es la única FK auto-referencial: un `SENIOR_PATIENT` apunta a su `CAREGIVER`.
