# ADR-0001: Adoptar Clean Architecture por capas

## Estado

Aceptada.

## Contexto

Una revisión de arquitectura del proyecto identificó varias violaciones a Clean Architecture: `LiveData` (un tipo de `androidx.lifecycle`) usado directo en las interfaces de repositorio del dominio; ausencia de una capa de casos de uso, con la lógica de negocio repartida y duplicada entre `ViewModel`s y `BroadcastReceiver`s; `Receiver`s y el `Worker` de background accediendo a los DAOs de Room directo, saltándose los repositorios; y 14 `ViewModelFactory` manuales repitiendo el mismo cableado de dependencias en cada pantalla.

El proyecto es una referencia académica de Clean Architecture/DDD en Android, así que estas violaciones no eran solo deuda técnica — contradecían el propósito mismo del proyecto.

## Decisión

Reestructurar el código en tres capas concéntricas (`domain/` en el centro, sin depender de Android; `data/`+`services/` y `presentation/` como capas externas que dependen de `domain/` pero no entre sí), ejecutado en 5 fases incrementales para minimizar el retrabajo:

1. Migración de esquema `email → username` (aislada primero por ser la de mayor riesgo, al tocar datos persistidos).
2. Value objects para reemplazar los strings sin validar del dominio.
3. `LiveData` → `Flow` en DAOs y repositorios.
4. Extracción de la capa de casos de uso (escrita ya contra el modelo/Flow final de las fases 2-3).
5. Hilt como composition root (último, para no recablear el resto del código dos veces).

Cada fase se compiló y se corrió `testDebugUnitTest` antes de pasar a la siguiente; la suite instrumentada completa (~94 tests) se corrió al final de las fases de mayor riesgo.

## Consecuencias

**Positivas:** el dominio es Kotlin puro testeable sin Android; la lógica de negocio (confirmar/posponer dosis, evaluar dosis perdida) vive en un único lugar en vez de triplicada; agregar una pantalla nueva ya no requiere escribir una `ViewModelFactory` a mano.

**Negativas / trade-offs aceptados:** el refactor tocó prácticamente todos los archivos de `presentation/` y `services/`, con el riesgo de romper comportamiento sutil (mitigado diffeando línea por línea las implementaciones duplicadas antes de unificarlas — ver [ADR-0005](0005-capa-de-casos-de-uso.md)). No hubo red de revisión humana automática en el repo durante este trabajo, así que cada fase se mantuvo sin push hasta estar completamente verificada en verde.
