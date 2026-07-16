# Guía de desarrollo

## Requisitos

- **JDK 21**
- **Android SDK**: `compileSdk`/`targetSdk` 37, `minSdk` 28. El proyecto usa `java.time` (`LocalDateTime`, `LocalTime`, `ZoneId`, `DayOfWeek`) nativo, sin core library desugaring — por eso `minSdk` no puede bajar de 26 sin habilitar desugaring, y de hecho está fijado en 28 desde la configuración inicial del proyecto.
- Un emulador o dispositivo físico (API ≥ 28) para los tests instrumentados y para correr la app — la suite de tests instrumentados no corre en CI (ver más abajo).

## Levantar el proyecto

```bash
git clone <repo>
cd MediTrack
./gradlew assembleDebug     # compila el APK debug
./gradlew installDebug      # instala en un dispositivo/emulador conectado
```

O abrir el proyecto en Android Studio y ejecutar la configuración `app`.

`local.properties` (no versionado) debe apuntar al Android SDK local (`sdk.dir=...`) — Android Studio lo genera automáticamente al abrir el proyecto.

## Comandos frecuentes

```bash
./gradlew compileDebugKotlin compileDebugUnitTestKotlin compileDebugAndroidTestKotlin  # solo compilar (rápido, sin correr nada)
./gradlew testDebugUnitTest           # unitarios (JVM, sin emulador)
./gradlew connectedDebugAndroidTest   # instrumentados (requiere emulador/dispositivo)
```

Para desarrollo iterativo, correr los dos primeros comandos (compilación + unitarios) es mucho más rápido que la suite instrumentada completa; reservar `connectedDebugAndroidTest` para antes de un commit/PR.

## Convenciones del proyecto

- **Idioma**: código, nombres, comentarios y tests en **inglés**. Documentación (este directorio, mensajes de commit, PRs) en **español**.
- **Comentarios**: solo cuando explican un *por qué* no obvio (una invariante, un workaround, una decisión con trade-off) — no se documenta *qué* hace el código cuando el nombre ya lo dice.
- **Sin Safe Args**: los argumentos de navegación van por `Bundle`/`bundleOf`, con las claves centralizadas en `presentation/NavArgKeys.kt` — nunca strings sueltos en el call site.
- **Sin mocks en los tests**: toda la suite (unitaria e instrumentada) corre contra implementaciones reales (Room en memoria o real, servicios reales) en vez de dobles de prueba. Ver [`docs/testing/`](../testing/README.md).
- **Sin abstraer prematuramente**: `domain/usecase/` depende directo de clases concretas de `services/` (no hay interfaces para `AlarmScheduler`/`FileStorageHelper`/`NotificationHelper`) — ver [ADR-0006](../adr/0006-hilt-como-di.md) para el razonamiento.
- **Migraciones de Room son obligatorias**: no hay `fallbackToDestructiveMigration`; un cambio de esquema sin su migración correspondiente rompe el build de tests instrumentados.

## Git

- Rama principal: `main`.
- Convención de commits: `tipo(alcance): descripción` (`feat`, `fix`, `refactor`, `test`, `chore`), cuerpo explicando el *por qué* cuando no es obvio — ver el historial de commits del refactor de Clean Architecture para ejemplos extensos.
- Los ADRs ([`docs/adr/`](../adr/README.md)) documentan las decisiones de arquitectura relevantes; una decisión de diseño no trivial debería registrarse ahí, no solo en el mensaje de commit.

## CI

`.github/workflows/ci.yml` corre en cada push y PR contra `main`:

1. Checkout + JDK 21 (Temurin) + setup de Gradle.
2. `./gradlew assembleDebug` — compila el APK debug (falla si no compila cualquier módulo).
3. `./gradlew testDebugUnitTest` — corre los tests unitarios.
4. Sube el reporte de tests unitarios como artifact (`always()`, incluso si fallan).

**Los tests instrumentados no corren en CI** — requieren un emulador Android, que el runner de GitHub Actions (`ubuntu-latest`) no provee sin configuración adicional (p. ej. `reactivecircus/android-emulator-runner`). Se corren localmente antes de cada PR relevante. Esto es una limitación conocida del pipeline actual, no una omisión — de agregarse en el futuro, sería la extensión natural de `ci.yml`.

## Estructura del emulador de referencia usado en este proyecto

Los tests instrumentados y la evidencia en [`docs/test-results/`](../test-results/) se corrieron sobre `Pixel_10_Pro_XL`, Android 17 (API 37), `sdk_gphone16k_x86_64`. No es un requisito estricto — cualquier emulador/dispositivo con API ≥ 28 debería servir — pero es el entorno contra el que se validó el comportamiento de alarmas exactas y notificaciones.
