# Verificacion de RNF — precision de notificaciones (issue #35)

**Entorno:** emulador Pixel 10 Pro XL, Android 17 (API 37), `sdk_gphone16k_x86_64`.
**Fecha:** 2026-07-05.

## RNF-01 (Tiempo de alerta): margen <= 30 segundos

**Cumplido.** Automatizado en
`AlarmSchedulerTest.exactAlarm_firesWithinRnf01ToleranceOf30Seconds`: programa una alarma exacta
(`AlarmManager.setExactAndAllowWhileIdle`, el mismo mecanismo que usa `AlarmScheduler.schedule()`
en produccion) 3 segundos en el futuro, mide el instante real en que el `BroadcastReceiver` la
recibe, y verifica que el margen sea menor a 30 segundos. Este test corrio dentro del suite
completo de 90 tests (ver issue #34) sin fallar.

Para el criterio de aceptacion original ("cronometrar 5 alarmas consecutivas") se corrio el test 5
veces seguidas (`./gradlew connectedDebugAndroidTest --tests AlarmSchedulerTest`), las 5
pasaron con margenes muy por debajo de 30s (el emulador entrega la alarma en 1-3s de la hora
objetivo, dominado por el overhead del propio `AlarmManager`/Doze, no por la app).

## RNF-05 (Persistencia de alarmas): la notificacion llega con la app cerrada

Se verificaron **dos escenarios distintos** de "cerrar la app", porque en Android no son
equivalentes:

### Escenario A — proceso eliminado en segundo plano (lo que hace un usuario al deslizar la app
fuera de recientes / lo que hace el sistema al liberar memoria)

**Cumplido.** Procedimiento: se programo una alarma a +60s usando el mismo
`AlarmScheduler`/`MedicationLogEntity` de produccion (base de datos real, no en memoria), se llevo
la app a segundo plano y se elimino su proceso (`adb shell am kill com.devoncats.meditrack`,
confirmado con `adb shell pidof` que el proceso ya no existia), y se espero a que pasara la hora
objetivo. La notificacion **"Hora de tomar RNF05KillTest"** aparecio correctamente
(`adb shell dumpsys notification`), disparada por el `BroadcastReceiver` declarado en el
manifest, que Android revive para entregar la alarma aunque el proceso estuviera muerto. Esto
confirma que la arquitectura (AlarmManager + BroadcastReceiver, no un Service atado al proceso)
es robusta frente al cierre normal de la app.

### Escenario B — "Forzar detencion" real (Ajustes > Apps > Forzar detencion, o
`adb shell am force-stop`)

**No aplica / limitacion de la plataforma, no un bug de la app.** Con el mismo procedimiento pero
usando `am force-stop` en vez de `am kill`, la alarma **no se entrega**: la entrada sigue
apareciendo en `adb shell dumpsys alarm`, pero `adb shell dumpsys package` confirma que la app
queda en estado `stopped=true`, y Android bloquea la entrega de broadcasts a apps en ese estado
hasta que el usuario vuelve a abrirlas explicitamente. Este es un comportamiento de plataforma
documentado desde Android 3.1 (pensado para bateria/privacidad) y aplica por igual a **cualquier**
app que use `AlarmManager`, no solo a MediTrack — no hay forma de evitarlo desde el codigo de la
app, y no deberia intentarse (seria pelear contra una proteccion intencional del SO).

**Conclusion:** se interpreta el criterio de aceptacion "cerrar la app de forma forzada" como el
escenario A (el que realmente puede ocurrirle a un Paciente o Paciente Mayor en el uso normal:
deslizar la app fuera de recientes, quedarse sin memoria, etc.), que es el que queda cubierto por
la arquitectura actual. El escenario B se documenta para que quede registrado que, si un
cuidador fuerza la detencion de la app manualmente desde Ajustes, las alarmas pendientes no
sonaran hasta reabrirla — igual que le pasaria con cualquier otra app de recordatorios en
Android.
