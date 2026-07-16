# ADR-0007: Mantener `EncryptedSharedPreferences` para la sesión

## Estado

Aceptada.

## Contexto

`SessionManager` guarda la sesión activa (`userId`, `role`) en `EncryptedSharedPreferences` (androidx.security-crypto), una API marcada `@Deprecated` en versiones recientes de la librería (Google recomienda migrar a Jetpack DataStore + cifrado propio). Durante la limpieza de deuda técnica se evaluó migrar a esa alternativa más moderna.

## Decisión

Mantener `EncryptedSharedPreferences` tal cual, por decisión explícita: para los fines del proyecto (académico, con foco en demostrar Clean Architecture/DDD), el trade-off no vale la pena — es la opción de mayor comprensión académica (API sincrónica simple, sin la curva de `Flow`/serialización de DataStore) frente a un beneficio marginal (evitar un warning de deprecación) que no aporta al objetivo del proyecto.

## Consecuencias

`SessionManager.kt` mantiene `@file:Suppress("DEPRECATION")` de forma permanente y consciente, no como deuda pendiente. Si el proyecto evolucionara hacia una entrega productiva real, esta decisión debería revisarse.
