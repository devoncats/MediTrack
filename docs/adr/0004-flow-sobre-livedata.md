# ADR-0004: `Flow` en vez de `LiveData` en el dominio

## Estado

Aceptada.

## Contexto

Era la violación de Clean Architecture más citada en la revisión: `domain/repository/*.kt` dependía de `androidx.lifecycle.LiveData`, un tipo específico de Android, en la capa que se supone no debe conocer ningún framework.

## Decisión

- Los 5 DAOs cambian sus métodos `observe*` de `LiveData<T>` a `Flow<T>` — Room soporta `Flow` nativamente, sin conversión manual.
- Las 3 interfaces de repositorio replican el cambio; el dominio deja de importar `androidx.lifecycle`.
- Los `ViewModel`s siguen exponiendo `LiveData` a los `Fragment`s exactamente igual que antes, vía `.asLiveData()` en el borde ViewModel→View. Decisión deliberada: la violación reportada era específicamente "`LiveData` en el dominio", no "`LiveData` en la UI" — este último es un uso legítimo según la guía oficial de Android, y no se tocaron los ~20 Fragments para este cambio.
- `utils/LiveDataUtils.kt` (un helper `combineLatest` sobre `MediatorLiveData`, agregado en una limpieza de deuda técnica anterior) se eliminó: con `Flow`, combinar fuentes es `kotlinx.coroutines.flow.combine(...)` directo. El patrón de "refrescar en `onResume`" pasó de un campo mutable observado manualmente a `MutableStateFlow` + `flatMapLatest`.

## Consecuencias

`Flow.combine()` suspende la transformación y cancela automáticamente cualquier invocación en curso en cuanto llega una emisión más reciente — esto reemplazó, sin código adicional, el manejo manual de `Job` que existía en `SeniorMedListViewModel` para evitar que un cómputo lento y obsoleto pisara el resultado de una emisión más nueva. Fue la fase de mayor superficie de cambio en tests (toca cada pantalla con listas reactivas), verificada con la suite instrumentada completa.
