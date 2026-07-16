# ADR-0003: Value objects para `Schedule.time`/`daysOfWeek`

## Estado

Aceptada.

## Contexto

`Schedule.time` y `Schedule.daysOfWeek` eran `String` crudos (`"08:00"`, `"MON,TUE,WED"`) hasta en el modelo de dominio. El parseo (`runCatching { LocalTime.parse(...) }`, `mapNotNull` sobre el CSV) se repetía en al menos 10 sitios distintos: `AlarmScheduler`, `MedFormFragment`, `MedFormViewModel`, y varios tests. Cada sitio podía fallar el parseo de forma distinta (o no fallarlo), y el dominio no tenía forma de expresar "un horario inválido" como tipo.

## Decisión

- `Schedule.time: String` → `Schedule.time: LocalTime` (`java.time.LocalTime` nativo — el `minSdk = 28` del proyecto ya lo soporta sin necesidad de core library desugaring ni un wrapper propio).
- `Schedule.daysOfWeek: String` → `Schedule.daysOfWeek: WeekDays`, un value object nuevo (`data class WeekDays(val days: Set<DayOfWeek>)`).
- El parseo/formato (`"HH:mm" ↔ LocalTime`, CSV ↔ `Set<DayOfWeek>`) se centralizó como extension functions junto al mapper de `MedicationRepositoryImpl` (`utils/DateUtils.kt`, `utils/DayOfWeekCodes.kt`), no dentro del modelo de dominio — el dominio no debe saber de formato de string, esa es responsabilidad de la capa de datos.
- De paso, `SeniorListViewModel.Companion.aggregateStatus(...)` (cómputo puro sin I/O que decidía qué estado "de hoy" mostrar) se movió a `MedicationLogStatus.aggregate(...)` en el dominio — no pertenecía a un ViewModel.

## Consecuencias

Un horario inválido ahora es irrepresentable en el dominio (no puede existir un `Schedule` con una hora mal formada); el parseo vive en un solo lugar. El costo fue actualizar cada sitio que construía un `Schedule`/`ScheduleEntity` o llamaba a `AlarmScheduler.nextTriggerMillis(...)` con los tipos nuevos, incluyendo eliminar un caso de test que ya no aplicaba ("formato de hora inválido") porque `LocalTime` ya no permite construir esa situación.
