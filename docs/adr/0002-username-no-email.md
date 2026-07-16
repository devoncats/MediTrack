# ADR-0002: Renombrar `User.email` a `username`

## Estado

Aceptada.

## Contexto

`User.email` (y `UserEntity.email`) almacenaba, para los usuarios `PATIENT`/`CAREGIVER`, un email real (lo que recoge la pantalla de registro). Pero para `SENIOR_PATIENT` almacenaba un identificador sintético generado por la app (`pm_<nombre_normalizado>_<caregiverId>`), nunca un email — el senior no se registra a sí mismo, su cuidador lo da de alta y recibe un usuario+PIN para que el senior inicie sesión. El nombre del campo mentía su semántica real para un tercio de los roles del sistema.

## Decisión

Renombrar el campo a `username` en el modelo de dominio, la entidad Room y el DAO (`findByEmail` → `findByUsername`). La UI de login/registro sigue mostrando "Correo electrónico" y validando formato de email para `PATIENT`/`CAREGIVER` — eso no cambia, solo el nombre interno del campo y lo que representa a nivel de dominio.

Migración Room v2→v3 (`MIGRATION_2_3`): `ALTER TABLE users RENAME COLUMN email TO username`, seguido de recrear el índice único bajo el nuevo nombre (`DROP INDEX index_users_email` + `CREATE UNIQUE INDEX index_users_username`) — el rename de columna por sí solo no alcanza porque Room deriva el nombre del índice del nombre de columna y su validación de esquema lo verifica.

## Consecuencias

Esta fue la primera fase del refactor y se ejecutó aislada, verificada con la suite instrumentada completa antes de seguir — es la fase de mayor riesgo real por tocar una migración sobre datos persistidos (a diferencia de las fases siguientes, que son reestructuración de código sin tocar el esquema). El nombre del campo ahora es honesto para los tres roles; el costo fue tocar todos los sitios que construían `User`/`UserEntity` con el nombre viejo (ViewModels, tests unitarios e instrumentados).
