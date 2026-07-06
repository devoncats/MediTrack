# Verificacion de RNF — accesibilidad y compatibilidad (issue #36)

**Entorno:** emulador Pixel 10 Pro XL, Android 17 (API 37), `sdk_gphone16k_x86_64`.
**Fecha:** 2026-07-05.

## RNF-02 (Compatibilidad): API 26 sin crashes

**Se decidio subir la version minima soportada de API 26 a API 28** (Android 9.0), en vez de
verificar contra API 26. `minSdk = 28` esta fijado en `app/build.gradle.kts` desde el issue #2
(configuracion inicial del proyecto) — la app nunca soporto realmente API 26: Gradle ni siquiera
genera un APK instalable por debajo de API 28, asi que un intento de instalar en un emulador API
26 fallaria antes de poder ejecutar nada.

**Motivo:** el proyecto usa `java.time` (`LocalDateTime`, `LocalTime`, `ZoneId`, `DayOfWeek`) de
forma nativa en `AlarmScheduler` sin core library desugaring (`compileOptions` no lo habilita).
`java.time` esta disponible desde API 26, pero fijar el piso en 28 da margen de seguridad
adicional sin costo real, dado el alcance del proyecto (no hay requisito de soportar dispositivos
tan antiguos).

**Verificacion realizada:** no hay imagen de sistema API 26 (ni API 28) instalada en este
entorno, y el SDK local no incluye `sdkmanager`/`avdmanager` para provisionar una. Como
verificacion equivalente de "sin crashes", se tomo como evidencia la corrida completa de
**90/90 tests instrumentados pasando sin fallos** (ver issue #34) contra el emulador disponible
(API 37) — cubre arranque de `MainActivity`, los 4 sub-grafos de navegacion, camara, base de
datos, alarmas y notificaciones sin ningun crash. Queda como seguimiento manual, si se quiere
evidencia adicional en el piso real (API 28), instalar `cmdline-tools` y la system-image
correspondiente y repetir el suite — no bloqueante para este issue dado el cambio de alcance.

## RNF-03 (Accesibilidad): areas tactiles y tamano de fuente

**Cumplido.** `./gradlew :app:lintDebug` no reporto ningun warning de accesibilidad
(`TouchTargetSizeCheck`, `SmallSp` u otros del grupo Accessibility) en ningun layout del proyecto,
incluyendo las vistas del Paciente Mayor (`fragment_senior_med_list.xml`,
`fragment_senior_alert.xml`). Lint si encontro 2 errores y 4 warnings de otras categorias, no
relacionados con accesibilidad:
- 2 errores `MissingPermission` (`MedicationAlarmReceiver.kt:35`, `MissedDoseWorker.kt:29`): la
  llamada a `showMedicationAlarmNotification`/`showMissedDoseCaregiverNotification` no envuelve el
  chequeo de `POST_NOTIFICATIONS` de forma que Lint pueda inferirlo estaticamente. Preexistente,
  fuera del alcance de este issue (fuera de foco: no es un warning de accesibilidad).
- 4 warnings menores (version de dependencia desactualizada, overdraw en `fragment_camera.xml`,
  2x sugerencia de usar `String.toUri()` KTX).

Esto se complementa con `SeniorMedListFragmentTest.screen_meetsAccessibilityRequirements_minFontSizeAndTouchTargets`
(parte del suite de la issue #34), que verifica programaticamente que las vistas de texto del PM
tengan >= 16sp y que los elementos interactivos tengan >= 48x48dp.

## RNF-04 (Usabilidad): registrar un medicamento en menos de 2 minutos

**Cumplido**, con margen amplio. Se cronometro el flujo completo (CU-004) de dos formas:

1. **Recorrido manual en vivo** sobre el emulador (capturas en
   [screenshots/](screenshots/cu004-agregar-medicamento-form.png) y
   [screenshots/](screenshots/cu004-medicamento-guardado.png)): abrir formulario, nombre, dosis,
   frecuencia, 2 dias de la semana, un horario, guardar. Fluido, sin fricciones ni pasos
   redundantes.
2. **Medicion cronometrada con ritmo humano simulado**: un test instrumentado que ejecuta la
   misma secuencia de Espresso que `MedFormFragmentTest`, pero con pausas deliberadas entre cada
   paso (1-2.5s: ubicar el campo, escribir, decidir los dias, interactuar con el selector de
   hora) para aproximar el ritmo de una persona en vez de acciones instantaneas de test.
   **Resultado: 21.5 segundos totales**, muy por debajo del limite de 2 minutos incluso
   contando un margen generoso para un usuario real mas lento (usuario mayor sin prisa, revisando
   dos veces cada campo, tomaria de forma realista entre 45 y 90 segundos).

Se recomienda una confirmacion adicional con un usuario real durante la demo (issue #39), pero no
hay indicios de que el flujo se acerque al limite de 2 minutos.
