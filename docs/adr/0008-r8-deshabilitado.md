# ADR-0008: Dejar R8/minificación deshabilitado en `release`

## Estado

Aceptada.

## Contexto

El build type `release` no minificaba ni ofuscaba código (`isMinifyEnabled` no configurado / R8 apagado). Se evaluó habilitarlo para acercar el build de `release` a un build de producción real.

## Decisión

Dejarlo deshabilitado por ahora. En AGP 9.2.1 (la versión que usa el proyecto), habilitar la optimización vía el nuevo DSL (`optimization { enable = true }`) requiere activar el flag incubante `android.r8.gradual.support` de Gradle, porque ese DSL todavía es una API en preview. Se documentó la razón directamente en `app/build.gradle.kts`:

```kotlin
release {
    // R8/minification stays off for now: enabling it (optimization.enable = true) on
    // AGP 9.2.1 requires opting into the incubating `android.r8.gradual.support` Gradle
    // flag, since this DSL is still a preview API. Revisit once it's stable, or once
    // the project is closer to an actual production release.
    optimization {
        enable = false
    }
}
```

## Consecuencias

El build `release` no está minificado/ofuscado. Aceptable para un proyecto académico que no se publica en producción; revisar cuando el DSL de `optimization` salga de preview en una versión estable de AGP, o si el proyecto se acerca a una entrega productiva real.
