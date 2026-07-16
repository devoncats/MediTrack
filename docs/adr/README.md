# Architecture Decision Records

Registro de las decisiones de arquitectura relevantes del proyecto, en el formato estándar (contexto → decisión → consecuencias). La mayoría corresponde al refactor estructural de Clean Architecture + DDD + Hilt, hecho en 5 fases incrementales sobre la base de una revisión de arquitectura previa.

| ADR | Título | Estado |
|---|---|---|
| [0001](0001-clean-architecture-por-capas.md) | Adoptar Clean Architecture por capas | Aceptada |
| [0002](0002-username-no-email.md) | Renombrar `User.email` a `username` | Aceptada |
| [0003](0003-value-objects-schedule.md) | Value objects para `Schedule.time`/`daysOfWeek` | Aceptada |
| [0004](0004-flow-sobre-livedata.md) | `Flow` en vez de `LiveData` en el dominio | Aceptada |
| [0005](0005-capa-de-casos-de-uso.md) | Introducir una capa de casos de uso | Aceptada |
| [0006](0006-hilt-como-di.md) | Hilt como framework de inyección de dependencias | Aceptada |
| [0007](0007-encryptedsharedpreferences.md) | Mantener `EncryptedSharedPreferences` para la sesión | Aceptada |
| [0008](0008-r8-deshabilitado.md) | Dejar R8/minificación deshabilitado en release | Aceptada |
