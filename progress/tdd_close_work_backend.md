# TDD backend16 — núcleo y persistencia

## Primer corte nominal compilable

Gate root80c94b6, contrato16 SHA3E45F260…44285. Sin init redundante ni HTTP/publicador/frontend. @s1 CloseWorkSessionTest.s1_closesRunningWithExactWorkNotesAndIndependentEvent: REDa05bec por tipos/puerto ausentes; GREEN6317cf junto a16 pruebas de ChangeWorkSession15. Se preservan constructor6 de recibo y constructor2/accessor event de bundle15. El evento nuevo tiene11 campos y su oráculo comprueba valores independientes, no serializa el mismo record como expected.

Incidente2230cc: se usó ruta backend duplicada desde cwd backend; no se escribió test y el filtro quedó vacío. No es RED. Tras crear correctamente el único test se observó a05bec. La evolución del puerto añadió notas antes del callback;0c33c0 mostró cuatro fakes funcionales15 con aridad anterior. Se adaptó únicamente su parámetro, preservando aserciones.

Regresión pertinente JSONB15: WorkSessionTransitionStoreTest.s2_pauseCommitsExactStateIntervalReceiptAndIndependentEvent RED4371d7 por closure:null añadido al serializar el record evolucionado. El adaptador omite ese campo sólo cuando es null; no anotaciones Jackson en núcleo ni reescritura de filas históricas. GREENfa22fd incluye también s25_receiptDetailIsIndependentOfLaterStateAndOutbox, que lee desde PostgreSQL el recibo6 sin closure. Constructor15/deserialización siguen compatibles. No se modificaron sus expected ni oráculos para tolerar un campo nuevo.

Formato real dc1ebe y regresión posterior93a62a/spotlessCheck EXIT0:19 casos (1 cierre nominal,16 core15,2 PG15), sin fallos. Este corte NO acredita cierre paused, notas inválidas, precedencias, fallback ni persistencia close: siguen próximos ciclos. Store sólo adapta firma y serialización15; no ha implementado INSERT de cierre ni evento16. La consulta closure todavía no existe; no se precrea antes de su caso real.

Freeze temprano de tipos para C: WorkSessionCloseNotes, WorkSessionClosure, WorkSessionClosed, WorkSessionTransitionReceipt, WorkSessionTransition, WorkSessionChanging, ChangeWorkSessionUseCase; ChangeWorkSession contiene el nominal real. C puede comenzar publicación contra WorkSessionClosed y POST contra el puerto close, esperando el próximo corte para consulta closure. No usar una rama entera ni copiar implementaciones en vuelo. Las notas todavía son valor nominal sin validación completa; la firma permanece estable mientras siguientes casos añaden sus reglas.

## Cierre paused y guardas antes del reloj

Tras liberar checkpoint db8bb0e, @s2 `s2_closesPausedWithoutCountingTheRest`: RED 7f9a1a por runningSince null; mínimo condicional según estado, GREEN 5b7a04. @s13 revisión obsoleta sobre estado cerrado: RED 9ef153, GREEN 4b893b. @s13 cierre nuevo con revisión vigente sobre closed: RED 495632, GREEN 4e2a36. @s14 máximo BIGINT antes del reloj: RED 7a5911, GREEN 8643b0. Cada caso se añadió y ejecutó individualmente antes de la fuente correspondiente. Los tres rechazos verifican ausencia de llamadas al reloj.

Refactor posterior: WorkSessionState comparte orden revisión/compatibilidad/agotar revisión entre requireTransition15 y requireClose16. Regresión core 8d2b80 GREEN (5 Close + 16 Change). No modifica puertos ni eventos congelados para C. Persistencia CLOSE sigue pendiente.

## Tiempo y zona de cierre

@s7 reloj anterior: RED 54fb9f, reutilización requireTime15 y GREEN 441478. Año UTC10000 inicialmente GREEN e87b1c mediante la misma guarda. @s9 zona histórica irresoluble: RED 3c5bb6, fallback UTC sólo al resolver ZoneId y GREEN 7a4c29, conservando zona del inicio. @s10 desbordamiento local superior con UTC válido: RED 018084, validación del año local y GREEN 97ee61. La fila local año cero inicialmente GREEN 9f8c60. Ninguna consulta de preferencias ni catálogo cliente.

## Notas validadas para HTTP

@s4 null normalizado: RED 604ed4, GREEN ee7c4a. @s5 progressNote 2001 puntos: RED 6952b4, GREEN b0317e; nextStep 2001 emoji: RED b8d1ae, GREEN 0870f6. Refactor normalize compartido, GREEN e7b545. NUL decodificado: RED c118b6, GREEN 32adec. Surrogate alto aislado: RED d41e45, GREEN b3fa91; se reutiliza el recorrido codePoints de Java, que deja los pares válidos como un único punto fuera del rango surrogate. Bajo aislado inicialmente GREEN 331704. Frontera positiva 2000 emoji inicialmente GREEN 39bb41; espacios y saltos preservados inicialmente GREEN 096b49. Cada variante fue añadida después de cerrar la anterior, sin matrices RED simultáneas.

Formato Spotless real y regresión core 424a34 GREEN. WorkSessionCloseNotes queda estable para C: constructor normaliza sólo null, limita a 2000 puntos de código y rechaza NUL/surrogates aislados con ValidationException, campo correspondiente e INVALID_VALUE. No cambia shape ni firma pública; tipos JSON siguen siendo responsabilidad del adaptador HTTP. Consulta closure todavía pendiente del nominal PostgreSQL.

## Primer nominal PostgreSQL CLOSE y puerto de recuperación

@s1 `WorkSessionTransitionStoreTest.s1_closeCommitsLastIntervalReceiptAndIndependentEvent`: RED a19a45 (evento15 null), GREEN 7c779f tras guardar evento16 e intervalo final cuando before running. Oráculos JSON explícitos de recibo/evento independientes; evento de inicio previo permanece intacto. Sin migración todavía: tablas15 permiten nominal16; restricciones e integridad se evaluarán por casos propios sin reescribir V16.

@s26 `s26_readsClosureBySessionAfterOutboxRemoval`: RED de compilación 543fb9, método concreto Store GREEN 1ee54d, consulta durable por sesión en plantilla RR read-only reutilizada. @s26 forwarding de ReadWorkSessionChangesUseCase.closure: RED a3d58e, GREEN d9fbb3 al ampliar puertos con implementación real ya existente. Formato y regresión 3f2124 GREEN (5 lectura aplicación +1 PG). La distinción propiedad/open404 y capturas de fallo siguen pendientes; no se declara lectura completa todavía. La firma pública para C ya queda estable: WorkSessionTransitionReceipt closure(String owner, UUID session).

Corrección de fixture solicitada por root: @s10 año local cero ahora parte de pausa inmediata con acumulado0, coherente con startedAt==changedAt; inicialmente GREEN 5bf7a4. Sin cambio de producción por ese ajuste.

@s26 propiedad de sesión ajena: RED e1097a, GREEN bc6213 tras consultar propiedad dentro de la misma plantilla RR antes del recibo. Bundle concreto de lectura y dependencias compilables: ReadWorkSessionChangesUseCase, ReadWorkSessionChanges, WorkSessionTransitionQueries, PostgresWorkSessionStore. Para reproducir los nuevos PG nominales también ChangeWorkSession, WorkSessionState y WorkSessionCloseNotes (el último ya copiado a C). Receipt/Transition/evento pertenecen al checkpoint db8bb0e; no tienen delta posterior. Regresión focal y formato d8c267 GREEN. Manifest close_work_closure_bundle_hashes.json fija esos siete Java; es checkpoint parcial, aún sin declarar reproducción/atomicidad completa del cierre.
