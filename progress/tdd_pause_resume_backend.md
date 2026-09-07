# TDD pausa y reanudación — núcleo y PostgreSQL

Autorización: contrato a5c556f, estado in_progress 5367b35. Baseline anterior vigente; no init repetido en fase documental. Ownership A+B: dominio/aplicación y persistencia; HTTP/publicador se coordinan con otro autor. No implementación de cierre 16.

## Ciclo 1 — @s2 pausa nominal

Único test ChangeWorkSessionTest.s2_pauseRecordsExactIntervalReceiptAndEvent: RED de compilación 709c15 por tipos/puerto ausentes; mínimo núcleo y contratos GREEN cf51b2, 1 test. Comprueba 1000001 µs exactos, reloj truncado y capturado una vez, recibo antes/después e identidad/evento de doce campos con valores esperados independientes. ChronoUnit.MICROS opera directamente en microsegundos, sin conversión a nanosegundos ni redondeo por minuto.

Corte compilable parcial: ChangeWorkSessionUseCase.pause(owner, session, key, expected) y WorkSessionChanging.commit(owner, session, key, action, expected, callback). WorkSessionState en domain usa long para revisión/acumulado y SessionStart original; DTO HTTP deberá serializar ambos long como texto. WorkSessionTransitionReceipt evita colisión con WorkSessionChange de 14; WorkSessionStateChanged ya usa cadenas para los campos del evento. Pendientes resume, guardas, lectura, PostgreSQL y HTTP; nominal no acredita feature completa.

## Ciclos 2 y 3 — reanudación y token completo

@s3 único nominal resume: RED279ee7 por método ausente; GREEN3c1309, 2 casos tras generalización mínima del comando. La pausa no se suma y el fin original permanece en SessionStart.

@s12 identidad del token: observación root detectó que long aislado perdía el UUID antes de la comprobación transaccional de propiedad. Único test de forwarding REDd0aba4; mínimo WorkSessionRevision(UUID sessionId,long value) en caso de uso y puerto, GREEN4d8d07, 3 casos. Firma pause/resume(owner,session,key,WorkSessionRevision). No GET adicional en controller ni validación de propiedad simulada en núcleo; Store deberá comprobar propiedad, identidad y replay en ese orden. Primer nominal todavía no incluye esas guardas.

Ventana Java/Gradle cedida al publicador tras este GREEN; tipos/puertos compilables notificados al coordinador y autor C. No freeze de feature completa.

## Ciclos 4–7 — snapshot y precedencias

@s1 lectura nominal: REDbf65ad por puerto/caso de uso ausentes, GREEN3e56a4 (1 test). ReadWorkSessionStateUseCase.read(owner,id) devuelve WorkSessionSnapshot; el callback de WorkSessionStateQueries captura reloj sólo después de obtener estado propio. Formas HTTP siguen responsabilidad C.

@s12 revisión obsoleta antes de estado/reloj: REDc88c9e, GREEN885cf9. @s14 PAUSE sobre paused: REDf3d1e8, GREENf3fa65. @s15 máximo BIGINT antes de reloj: REDbeddd3, GREEN8534c8. @s7 retroceso de un microsegundo: REDee9391, GREEN07be8e. Cada uno fue un test individual y mínimo cambio; WorkSessionTransitionException(code) conserva códigos propios de 15 sin alterar la excepción temporal de inicio14. Domain WorkSessionState protege estas guardas; pendientes rangos UTC, otros netos, PG y HTTP.

## Límites temporales y neto de lectura

Cada prueba siguiente se escribió y ejecutó antes del mínimo cambio correspondiente: comando año 10000 REDafc8da/GREENf2a93b; lectura paused REDaa1268/GREENa82c0d; lectura reloj atrasado RED26b68f/GREEN4898a7; lectura año0 RED631876/GREENfcf9b7; lectura año10000 REDe81d67/GREEN15438f. El neto paused no crece; el aporte abierto nunca resta acumulado; el serverNow devuelto no se recorta. Las guardas temporales no cambian títulos ni lógica de inicio14.

La numeración anterior «ciclos 4–7» agrupa cinco ciclos (4–8); las identidades y RED/GREEN registrados son la evidencia, no un recuento inventado. Todos los casos fueron individuales, sin matrices anticipadas.

## PostgreSQL nominal — @s2

Único WorkSessionTransitionStoreTest.s2_pauseCommitsExactStateIntervalReceiptAndIndependentEvent: RED0640e5 porque Store no implementaba WorkSessionChanging; GREENd18f6e en PostgreSQL real, 1 caso. V16 aditiva conserva V14/V15 y añade estado derivable inicial, intervalos terminados y recibos separados. El intervalo abierto inicial se deriva de startedAt; el recibo14 permanece inmutable. Guarda pausa, intervalo terminado, recibo y evento en una transacción. Esperados JSON completos se construyen con valores explícitos, no serializando los records bajo prueba. El evento de inicio existente permanece idéntico.

Corte aún parcial: falta RESUME PG, replay/identidad, plaza paused, constraints y fallos/carreras. Se actualizarán fixtures legacy de TRUNCATE con los dos hijos explícitos antes de regresión global; no se ha ejecutado global durante esta frontera.

## Puertos de recuperación para HTTP

@s25 detail nominal REDf12daa/GREENce6b67; refactor del método de puerto a changeDetail GREEN24bd42 para coexistir con detail de inicio en Store único. @s25 byRequest nominal RED3b51ff/GREENf92faf. @s13 ausencia de recibo REDcd8b46/GREEN614a96; ausencia de key RED99fe8f/GREENd65329 (4 casos). Contratos compilables entregados a C: ReadWorkSessionChangesUseCase.detail/byRequest; WorkSessionTransitionQueries.changeDetail/changeByRequest; WorkSessionChangeNotFoundException. Son pruebas directas de forwarding/ausencia; no acreditan aún recuperación PG o reinicio.

## Persistencia de estado, plaza y replay

@s1 lectura inicial sin materialización RED4ed108/GREEN3a25e1; @s22 SHOW read-only/RR durante captura RED7e595d/GREEN72c215. Plantilla stateSnapshot propia, sin mutar flags compartidos con lecturas14.

@s3 reanudación PG REDefcff9/GREENf6ae2b; conserva intervalos terminados y runningSince inicia sólo el segmento nuevo. @s9 plaza pausada en aplicación RED3adb50/GREEN956704; unicidad real PostgreSQL running junto a paused REDe295c1/GREEN60991a. V16 reemplaza sólo el índice parcial anterior por uno que cubre ambos estados; V15 permanece intacta.

@s12 identidad token ajena tras ownership RED71eef7/GREEN0a6cfc. @s16 replay después de otra transición sin reloj REDcf7388/GREENbccff3. @s17 cambio de acción con misma key antes de revisión actual REDaa09fd/GREENbcd125. Todos fueron casos individuales en PostgreSQL real. Todavía pendientes revisión esperada distinta, carreras, supresiones/commit y restricciones completas; no freeze global.

## Recuperación y compatibilidad de fixtures

@s17 revisión esperada distinta RED356676/GREENe3a9dc. @s25 recibo PG por id independiente de estado/outbox RED534da9/GREEN31cec9; key por puerto de entrada RED795f30/GREENb076b6. El primer fixture borra outbox para demostrar independencia de almacenamiento; no afirma entrega Rabbit ni reinicio de proceso, pendientes del smoke real.

Compatibilidad V16: AuthenticationHttpTest.s1_anonymousSessionIsPublicExactAndPersisted reprodujo REDa46e61 por FK de work_session_intervals (XMLf04e52). Se añadieron work_session_intervals y work_session_changes explícitamente a 22 listas de limpieza existentes; ningún CASCADE nuevo ni FK retirada. Lista exacta en pause_resume_fixture_paths.txt. Mismo foco GREENbc7b2a tras a6567a, sin suite global ni prueba artificial del literal de limpieza. Formato y regresión afectada quedan para el cierre integrado.

## Fallos al finalizar lecturas

@s24 estado: RED93ff84/GREEN31215c; recibo por id: RED76013c/GREEN7fd31b; recibo por key: RED5c6c60/GREEN0e9d89. Cada caso usa gestor delegado que ejecuta la lectura real read-only y después falla al finalizar, verificando StorageUnavailableException fuera de la plantilla. La misma envoltura heredada traduce DataAccessException; sus conexiones SQL se comprobarán individualmente sin inventar nuevas reglas.

Frontera GREEN0e9d89 cedida a root para integrar sólo publicador C. No Gradle concurrente ni fuentes propias modificadas durante esa integración.

## Refuerzo de intención, atomicidad y privacidad

Comparación completa de intención del recibo: RED3ca91c/GREEN5654ed (núcleo + tres replay PG). Se comprueba sessionId contra una identidad solicitada distinta mediante recibo controlado del núcleo, sin fingir dos sesiones abiertas del mismo propietario en PostgreSQL. El helper requireIntent centraliza también acción/revisión existentes.

Casos individuales inicialmente verdes de @s21: supresión de estado a4b2ff, intervalo333a76, recibo942eff, outboxe898c4 y fallo real diferido al commit c0ba2c. Cada trigger se restaura en finally y se comprueban estado anterior y conteos de intervalos/recibos/eventos; no se fabricó RED ni se atribuyó conflicto a supresión sin ganador.

Privacidad inicialmente verde: estado ajeno780647; comando ajeno antes token/replay9f371c; recibo ajeno/ausentedbd7f2; key ajena/ausentef04b7b; identidad token antes replay6027c4. @s24 fallo SQL real de estado012dde. Cada prueba se añadió únicamente después del GREEN anterior; los grupos de llamadas sólo orquestaron esa secuencia, no escribieron matrices anticipadas.

## SQL y concurrencia observada

Fallos SQL de recibo/key inicialmente GREENd25963/ae25b5. @s19 primera barrera falló en fixture9bfd5e: pg_stat_activity observada dentro de una misma transacción conservaba snapshot estadístico inicial0 (6cea31). Se invalida únicamente ese snapshot de estadísticas mediante pg_stat_clear_snapshot por sondeo. Caso real same-key GREENfc56cb, inicialmente verde de producción; dos sesiones esperaron Lock y ambos futuros seguían pendientes antes de liberar la fila. Keys distintas, mismo montaje, inicialmente GREEN1f744f: un cambio y PRECONDITION_FAILED. No se cambió producción para maquillar el incidente de fixture.

@s20 inicialmente GREEN10fe24: otro propietario y proyecto/tarea propios permanecen bloqueados mientras la pausa propia confirma; no mutex global ni dependencia de esos locks. @s22 inicialmente GREEN950490: lectura RR fija estado running, writer de pausa confirma en otro hilo antes de capturar reloj; consulta adicional dentro del snapshot sigue running y una nueva lectura exterior ve paused. Verifica snapshot coherente y ausencia de bloqueo de escritura por GET; no se simula concurrencia con una respuesta ya calculada.

## Precisión temporal, upgrade y cableado

Casos individuales inicialmente GREEN: pausa en el mismo microsegundo a383fe; reanudación en el mismo microsegundo 873940; rango UTC completo y valor exacto 315537897599999999 µs (supera Number y nanosegundos Long) 80ed95; suma de fracciones sin redondeo 2a6b43; revisión penúltima que alcanza BIGINT máximo 299eda; reanudar running antes del reloj c893a2. No se fabricaron RED para conducta ya implementada.

Upgrade PostgreSQL V15→V16 inicialmente GREEN7e0c8b: conserva todos los campos del inicio, key y eventos anteriores; GET deriva estado inicial sin materializar proyección ni intervalos. Acumulado negativo rechazado por restricción nueva V16: RED0df816/GREENd56399. V14/V15 permanecen intactas.

Cableado de los tres casos de uso con operación contra Store real: comando RED85b817/GREENe2e0d5; estado RED4cb558/GREENe8d7cc; recibo REDb7140c/GREEN504bb4. El último ejecutó ApplicationWiringTest y ProjectStateConfigurationTest completos. La ausencia consultada produce la excepción contractual, no una mera afirmación de existencia de bean. Incidente d159c3: inserción textual duplicó el primer test y falló compilación; se retiraron las copias antes de observar el RED real de bean ausente. No cuenta como RED funcional.

## Colisión esperada y transacción nueva

@s17 REDd255cb (503 incorrecto) y GREENc478f7 (409). Fixture PostgreSQL válido con pausa durable; un JdbcTemplate controlado oculta únicamente la primera búsqueda de key y permite intentar RESUME con la key ya usada. El INSERT real alcanza UNIQUE(owner_id,request_key). ON CONFLICT específico evita absorber otras colisiones; su fila cero revierte la transacción y busca recibo durable en otra transacción (dos txid distintos observados). Compara intención y mantiene estado/recibo/eventos de la pausa. Es inyección de visibilidad de lectura para cubrir la frontera de colisión, no una segunda sesión abierta imposible ni una carrera adicional de negocio. Supresión sin ganador conserva 503 y las demás restricciones únicas conservan tratamiento de almacenamiento.

## Últimos oráculos y regresión integrada

@s8 proyecto/tarea completed: incidente de fixture78d3b9 (completed_at de tarea ausente; constraint correcta) corregido siguiendo el fixture14 existente. GREENb46e90 inicialmente verde de producción; pausa/reanudación conservan ambos estados y SessionStart. @s18 keys14/15 independientes inicialmente GREENeeb20f. @s6 comando atravesando DST, medianoche y fin fijo inicialmente GREEN5477a2.

Spotless real aplicado67ac8a. Regresión integrada bc1fa6 + spotlessCheck EXIT0: 230 casos en12 suites, cero fallos/errores. XML preservados en pause_resume_backend_checkpoint_xml y recuentos363ffa: Change16, ReadState5, ReadChanges4, Store15 36, Migration1, HTTP15 48, HTTP14 49, Integration14 3, Store14 41, Persistence14 1, Wiring19, ProjectConfig7. Incidente de invocación f8f7df: --tests quedó después de spotlessCheck y Gradle rechazó la opción antes de ejecutar; se corrigió el orden, sin falsear resultados.

GET @s6 exactos añadidos después de esa regresión: medianoche/fin vencido inicialmente GREEN0c4e5e; pliegue DST inicialmente GREEN6f4b5f. Se mejoró el fixture DST para crear el inicio mediante preferencia Europe/Madrid real, conservando coherencia del evento y recibo originales en vez de ajustar zona después; GREENfa613b. Ambos exigen ausencia de cambios/eventos y fila completa intacta.

## Mapa compacto de evidencia A+B

- @s1: WorkSessionTransitionMigrationTest.s1_upgradePreservesPublishedStartAndDerivesStateWithoutGetWrites; Store.s1_stateReadsInitialProjectionWithoutMaterializingIt. Upgrade real desde15, sin backfill por GET.
- @s2–3: ChangeWorkSessionTest nominales; Store nominales pausa/reanudar con JSON esperado independiente, intervalo y outbox reales. Wiring operativo de los tres puertos.
- @s4–5: ChangeWorkSessionTest mismo microsegundo PAUSE/RESUME y acumulación fraccionaria sin redondeo.
- @s6: Store.s6_midnightAndExpiredPlanDoNotCreateATransition y s6_dstFoldDoesNotChangeTheRunningSnapshotOrFixedEnd; refuerzo de comando en ChangeWorkSessionTest.
- @s7: ChangeWorkSessionTest reloj anterior y año10000; el límite inferior se compone con comparación contra changedAt válido y validación14. HTTP comprueba traducción temporal con título15.
- @s8: Store.s8_completedProjectAndTaskStillAllowPauseAndResume y s20_otherOwnerAndContextLocksDoNotBlockMyTransition; Store no consulta catálogo/disponibilidad en la ruta de transición. No se inventa constructor de catálogo15.
- @s9: Store.s9_pausedSessionStillOccupiesTheOwnersPlace y s9_databasePreventsRunningBesidePausedForTheSameOwner, con restricción PG real.
- @s10–12: HTTP del autor C; A+B agrega token completo, propiedad antes de identidad, identidad antes de replay, revisión antes de estado/reloj.
- @s13: Store consultas de estado/comando/recibo/key ajenos, mismas excepciones de ausencia; HTTP del autor C conecta status/cuerpo.
- @s14–15: ChangeWorkSessionTest pausa paused, resume running, revisión máxima sin reloj y penúltima válida.
- @s16–17: Store replay tras transición posterior sin reloj; cambios de acción/revisión; requireIntent de sesión controlada en núcleo; colisión esperada con nueva transacción y límite de fixture descrito arriba.
- @s18: Store.s18_startAndTransitionKeysHaveIndependentNamespaces demuestra14/15. Namespace13 se conserva por tabla independiente, sin repetir aquí una agenda artificial ni declarar una prueba13 nueva.
- @s19: Store dos carreras reales con ambas peticiones esperando Lock: same-key201/200 y distintas keys201/412.
- @s20: Store prueba otro propietario y locks de proyecto/tarea sin impedir transición propia.
- @s21: Store cuatro supresiones y fallo diferido al commit con rollback completo; colisión sin ganador sigue503.
- @s22: Store SHOW read-only/RR dentro de captura única y writer concurrente confirmado antes de Clock sin mezcla del snapshot.
- @s23: ReadWorkSessionStateTest running atrasado, paused, años0/10000; Store ausencia/ajeno antes de reloj. Son evidencias compuestas, no seis nuevas ejecuciones HTTP reales.
- @s24: Store errores SQL reales y fallos al finalizar de S/C/K; HTTP conecta los problemas cerrados.
- @s25: Store recupera recibos durables por id/key después de transición posterior y retirada de outbox. El borrado del fixture acredita independencia del almacenamiento, no publicación Rabbit ni reinicio de proceso: eso corresponde al smoke de C.
- @s26: paquete de publicador C integrado por root4b5a254; su bitácora contiene los oráculos propios. No se vuelve a atribuir como autoría A+B.
- @s27–39: cliente/React, UX y E2E de los otros autores; fuera de este paquete A+B. @s28 añade aquí rango UTC completo315537897599999999µs sin overflow nanos.

Límites: la unicidad owner/key entre sesiones diferentes se mantiene persistente, pero el fixture de dos sesiones no cerradas del mismo propietario sería inválido en15. No se habilita closed16 para probarlo. La rama defensiva de colisión se comprueba mediante ocultación controlada de la primera lectura, con SQL y rollback reales. Revisión, mutación, smoke y gate global integrados siguen bajo coordinación de root; esta bitácora no declara feature done.

## Freeze A+B para revisión

GET finales y formato focal del único test: primer hook18c6e5 recibió ruta relativa y no formateó (mensaje explícito, aunque EXIT0); ruta absoluta correcta dee387 aplicó formato. Regresión Store completo38/38 + spotlessCheck GREEN7134ad, sin cambios de producción. Evidencia final compuesta: 232 casos únicos, cero fallos/errores/skips94e212, en pause_resume_backend_final_xml (230 del corte bc1fa6 con Store sustituido por su ejecución final38). No se afirma una nueva ejecución conjunta232: se conservan también XML originales230 por separado.

Manifest de freeze: pause_resume_backend_freeze_hashes.json,313 archivos de backend/src/main, backend/src/test y build.gradle.kts. Src/main permanece idéntico durante el smoke C. C notificó smoke real15 EXIT0 0886c3 y283 hashes idénticos d1955e; su informe es la evidencia de publicación/recuperación/reinicio, pendiente de dictamen root.

Selector Gradle pause_resume_session: suma al alcance14 compartido todas las clases domain.WorkSession*, ChangeWorkSession*, ReadWorkSessionState*, ReadWorkSessionChanges* y WorkSessionStateController*. Esto incluye los tipos/eventos/recibos por el patrón application.WorkSession* existente, Store completo, OutboxMessage, PublishOutbox, RabbitBrokerPublisher y ApplicationConfiguration. Se conservan los candidatos JUnit completos, umbral80,4 threads, timeout/filtros heredados y report separado reports/pitest-pause-resume-session. El default global incorpora también las clases15. Dry-run016fd0 verificó compilación del selector sin ejecutar PIT; después se añadió el alcance15 al default y la configuración compiló de nuevo con la regresión7134ad. Ninguna campaña iniciada por A+B.

Sin nuevos estados16, timers, librerías, cambios publicadosV14/V15 ni correcciones frontend. Freeze de fuentes/tests/config para revisión cruzada; root conserva commits, gate integrado y autorización de mutación.
