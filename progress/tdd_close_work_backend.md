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

## Intención de replay y cierre único

@s16 progressNote diferente con key confirmada: RED 4c54d6, GREEN ab046c. Receipt conserva requireIntent15 y añade overload con notas16; ambos caminos de Store (lookup y colisión tras rollback) pasan notas antes de negocio. @s16 nextStep distinto inicialmente GREEN 309e9a. @s15 replay CLOSE con null/vacío normalizados después de iniciar B inicialmente GREEN 755681; comprueba recibo exacto, inicio original, B intacta, conteos y reloj no consultado. Este caso no sustituye todavía replays históricos PAUSE/RESUME/inicio.

V17 aditiva aprobada por root: @s23 segundo cierre durable de la misma sesión por SQL directo RED 5d872d, índice único parcial session_id/CLOSE, GREEN bea9c1. Sin modificar V14–V16, filas ni tablas nuevas. La escritura SQL del test es una entrada inválida deliberada para verificar terminalidad; no se presenta como transición de negocio válida. Upgrade14/15 sigue pendiente del siguiente caso.

## Upgrade y atomicidad heredada conectados a CLOSE

@s23 WorkSessionClosureMigrationTest.s23_upgradePreservesRunningPausedAndAllPublishedFacts inicialmente GREEN 134404: migración real target16→17 con dos propietarios, running y paused, recibo15/intervalo/outbox; compara filas completas antes/después y GET sin escrituras, luego cierra ambos con neto correcto y sin intervalo extra para paused.

Refactor del helper de supresión para aceptar la operación pública, cuatro casos15 GREEN 910706. Después se añadió individualmente cada conexión CLOSE @s21, todas inicialmente GREEN: estado449ed4, intervalo e00b34, recibo sin ganador1a67ea, outbox902507. Conservan filas, intervalos, recibos y outbox anteriores mediante el mismo fixture SQL real. Fallo diferido al commit CLOSE inicialmente GREEN564d4b, incluyendo active todavía ocupado. No se cambia producción para fabricar RED: reutilizan atomicidad y conteo de filas15, ahora ejecutando el nuevo comando16.

## Carreras sobre una sesión

Refactor mínimo racePause→raceTransitions permite operaciones públicas manteniendo la barrera PostgreSQL existente: ambas sesiones observadas en pg_stat_activity esperando Lock, ambos futuros pendientes antes de liberar; regresión15 de dos carreras GREEN57a256. Después cada fila16 @s18 se añadió por separado y resultó inicialmente GREEN: mismo CLOSE/key a21850 (un recibo, replay false/true); CLOSE keys distintas b522aa; CLOSE/PAUSE cff8db; CLOSE/RESUME desde paused6c54ab. Revisión, cambios y outbox acreditan una sola transición ganadora; no se fuerza preferencia del scheduler ni se convierten clics en dos revisiones consecutivas.

## Lecturas y colisión con estado alcanzable

Conexiones individuales inicialmente GREEN: @s26 propia abierta devuelve WorkSessionChangeNotFound d31973; @s27 cierre de transacción de lectura falla como5038f994b y falloSQL de recibos como5036866e4; @s24 estado closed con reloj anterior conserva neto final y fila d0262d. No se usa active=null como prueba de ausencia de cierre.

@s22 colisión entre sesiones alcanzables, e1ad7b inicialmente GREEN: A cerrada, B única abierta; se oculta sólo el primer lookup de key en el JdbcTemplate de prueba para alcanzar INSERT real owner/key, y se observan dos txid distintos al recuperar ganador después del rollback. Se comprueba409 por intención ajena y B/intervalos/eventos intactos. La colisión es inyectada en el lookup, no una carrera natural afirmada; no se fabrican dos sesiones abiertas. El replay200 de intención idéntica se acredita por vía normal y carrera misma sesión; el lock de esa sesión impide reproducir naturalmente esa misma intención por la rama de colisión tardía.

## Cierre de conexiones temporales y transaccionales

Refuerzos individuales inicialmente GREEN: @s8 rangoUTC completo con315537897599999999µs945095; @s9 día local Madrid distinto del díaUTC3d98c1. @s25 snapshot running fija su lectura, writer CLOSE confirma antes del reloj y posterior GET ve closed44eb01. @s19 nuevo inicio durante cierre aún sin commit rechaza mientras A ocupa plaza, luego inicio deliberado prospera b01a25; variante rollback d2588d conserva A. El fixture mantiene abierta la transacción exterior, libera latch en finally y distingue el resultado interno aún no confirmado del éxito del comando real.

@s20 cierre no espera locks de otro propietario ni de su proyecto/tarea e4d3b4. @s15 replays históricos de inicio, PAUSE y RESUME después de CLOSE y nueva B12fa9d conservan hechos/conteos, sin reloj ni catálogo. @s27 GETclosure demuestra SHOW read-only/RR y snapshot coherente incluso con CLOSE confirmado entre consulta de propiedad y recibo a69391. @s21 falloSQL real del INSERT de outbox después de escrituras anteriores revierte todas56abae. No se cambió producción durante estos refuerzos.

## Freeze A+B

Regresión focal final f5ba7c EXIT0, después de SpotlessApply real: 115 pruebas, ocho suites, cero fallos/errores/omitidos. XML preservados en progress/close_work_backend_xml y recuentos en close_work_backend_results.json (1c05b3). Manifest close_work_backend_hashes.json fija 19 archivos: 13 Java de producción, V17 y cinco Java de prueba. No más Gradle tras esta frontera, según coordinación root; la suite backend completa y el check global de formato pertenecen al gate integrado.

No se añade bean16: ChangeWorkSessionUseCase y ReadWorkSessionChangesUseCase ya están enlazados a las mismas implementaciones; se amplían métodos y DTO internos preservando consumidores15. HTTP/publicador de C se verifican en sus propios informes, no se infieren desde estas pruebas.

### Mapa contractual A+B

| Escenarios16 | Evidencia ejecutada o reutilizada |
| --- | --- |
| s1–s2 | CloseWorkSessionTest nominal running/paused; Store s1_closeCommitsLastIntervalReceiptAndIndependentEvent; upgrade cierra paused sin intervalo adicional. |
| s3 | Compuesto: aritmética16 running/paused y requireTime compartido; ChangeWorkSessionTest.s4_pauseInTheSameMicrosecondAddsZero / resumeInTheSameMicrosecondAddsZero. No caso CLOSE cero dedicado. |
| s4–s5 | Ocho casos de notas en CloseWorkSessionTest; tipos/raíz JSON pertenecen a HTTP de C. |
| s6 | Frontera HTTP de C; se conserva puerto con token completo y notas antes del callback. |
| s7–s10 | CloseWorkSessionTest guardas de tiempo, rango total, fallback histórico, fecha Madrid, límites locales; añoUTC0 se rechaza por ser anterior al changedAt válido, año10000 tiene caso directo. |
| s11 | Store s26_readsClosureBySessionAfterOutboxRemoval compara recibo durable completo; lectura no recibe catálogo/reloj ni reproyecta zona. No se simula instalar otro TZDB. |
| s12 | Store15 s12_foreignCommandPrecedesTokenAndReplay y tokenIdentityPrecedesExistingReplay recorren el mismo commit; CLOSE replay exacto y nota distinta tienen casos16. |
| s13–s14 | CloseWorkSessionTest revisión/closed/máximo y regresión ChangeWorkSessionTest de guardas compartidas; PAUSE/RESUME sobre closed se rechazan por la compatibilidad exacta del mismo WorkSessionState. |
| s15 | Store s15_replaysOriginalCloseAfterAnotherSessionStarts y s15_startPauseAndResumeReplaysSurviveCloseAndNewActiveSession. |
| s16–s17 | Store dos notas distintas; identity guard compartida y s22_crossSessionCollisionRollsBackBeforeFreshIntentLookup con A closed/B única abierta. Acción/revisión forman parte de requireIntent15 ya probado; no se redefine namespace. |
| s18 | Cuatro carreras CLOSE/keys/P/R, ambos workers observados esperando Lock antes de liberar. |
| s19 | Dos intercalados inicio/cierre con commit o rollback, más nuevo inicio después de cierre confirmado. |
| s20 | Store s20_otherOwnerAndContextLocksDoNotBlockMyClose; misma ruta de lock no lee elegibilidad. El caso15 completedProjectAndTaskStillAllowPauseAndResume acredita esa frontera común; no caso CLOSE completed separado. |
| s21 | Cuatro supresiones CLOSE, falloSQL tras escrituras y fallo diferido al commit, todos con rollback verificable. |
| s22 | Colisión UNIQUE real con lookup inicial controlado, txid diferentes; límites de evidencia descritos arriba. Cero sin ganador se cubre con supresión de recibo. |
| s23 | V17 índice único parcial y WorkSessionClosureMigrationTest real target16→17, sin reescritura. |
| s24–s25 | Closed neto fijo con reloj anterior, errores de rango y rama no-running de ReadWorkSessionState reutilizadas; snapshot concurrente CLOSE con writer terminado antes del reloj. |
| s26–s27 | Recuperación por sesión durable, propiedad/abierta, SQL/cierre503, SHOW read-only/RR y dos consultas en un único snapshot. |
| s28 | Query/seguridad/formato HTTP de C. |
| s29–s30 | Evento11 independiente en core/PG; validación y Rabbit reales pertenecen al paquete C. |
| s31–s41 | UI/cliente, integración y smoke coordinados por root; no acreditados por este paquete Java. |

Límites: este freeze no es aprobación global ni campaña de mutación. Los casos compuestos anteriores se declaran como reutilización, no como nuevas filas ejecutadas. La rama tardía de colisión con intención idéntica no se fuerza mediante un estado imposible: el replay normal y la carrera misma key sí acreditan200. La revisión independiente decidirá cualquier refuerzo diferenciable antes de gates; no se añaden variantes para perseguir un porcentaje.
