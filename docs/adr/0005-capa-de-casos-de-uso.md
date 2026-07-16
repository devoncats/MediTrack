# ADR-0005: Introducir una capa de casos de uso

## Estado

Aceptada.

## Contexto

La lógica de negocio vivía en los `ViewModel`s y se triplicaba: confirmar una dosis existía, casi idéntico, en `AlertViewModel`, `SeniorAlertViewModel` y `MedicationActionReceiver.confirmDose()`. Los `Receiver`s y el `Worker` de background accedían a los DAOs de Room directo (`MediTrackDatabase.getInstance(context).medicationDao()...`), saltándose por completo los repositorios de dominio.

## Decisión

Crear `domain/usecase/` con 9 casos de uso (`RegisterUserUseCase`, `LoginUseCase`, `SaveMedicationUseCase`, `DeleteMedicationUseCase`, `ConfirmDoseUseCase`, `PostponeDoseUseCase`, `EvaluateMissedDoseUseCase`, `RescheduleAllAlarmsUseCase`, `CreateSeniorPatientUseCase` + `DeleteSeniorUseCase`), cada uno dependiendo solo de repositorios/servicios, nunca de `Fragment`/`Activity`. Detalle completo en [`docs/api/`](../api/README.md).

Antes de borrar cualquier implementación duplicada, se diffearon línea por línea las 3 copias de confirmar dosis (dos ViewModels + el Receiver) por si ya habían divergido sutilmente — no fue el caso, pero de haberlo sido, unificarlas a ciegas habría introducido una regresión de comportamiento.

De paso, `MedicationAlarmReceiver` (que no estaba en el alcance original de esta fase) también dejó de tocar los DAOs directo: al convertirse a un punto de inyección de Hilt en la fase siguiente, pasó a depender de `MedicationRepository`/`UserRepository` inyectados en vez de instanciar la base de datos a mano — cerrando el último punto donde un `Receiver` saltaba la capa de repositorios.

## Consecuencias

La lógica de confirmar/posponer dosis vive en un solo lugar; un cambio de comportamiento futuro (p. ej. cambiar cuánto se pospone una dosis) se hace en un único archivo en vez de tres. El costo: los `Receiver`s y el `Worker` siguieron construyendo sus dependencias a mano (`MedicationRepositoryImpl(dao, dao, dao)`) hasta la fase de Hilt siguiente — un estado intermedio deliberadamente transitorio, no una arquitectura final.
