# TDD backend 17: fin efectivo y ampliación

Contrato706f539, autorización root y estado ace3f9e; 44 tags/132 ejemplos, sin equivalencia con número de pruebas. Baseline16 init/build GREEN reutilizado por instrucción root; sin suite global nueva. Ponytail full/Caveman lite; arquitectura hexagonal existente, TDD individual, sin HTTP/publicador/frontend/harness ni Git propios.

## Primer checkpoint nominal

1. @s1 ExtendWorkSessionTest.s1_extendsTheEffectiveEndWithoutChangingStateOrWork: RED eaa14e por tipos ausentes; mínimo GREEN9cc5fe. Puerto de extensión recibe contexto coherente estado/fin/última decisión y produce recibo existente con extension más evento nuevo11. El único caso acredita ampliación anterior al vencimiento, microsegundo truncado una sola vez y State6 conservado salvo revisión; no acredita todavía guardas, fórmula tardía ni PostgreSQL EXTEND. Incidente anterior bcabd4: ruta test duplicaba backend y no escribió archivo, filtro sin pruebas; no se cuenta como RED.
2. @s13 ReadWorkSessionEndTest.s13_readsTheEndWithOneClockCaptureAfterTheStoredSnapshot: RED8c14b1 por puerto/caso ausentes, GREENb5fe16. Un snapshot paused con reloj consultado sólo dentro del callback después de las lecturas. No acredita transacción PostgreSQL ni guardas temporales todavía.
3. @s22 WorkSessionExtensionReceiptTest.s22_readsThePersistedPauseShapeWithoutExtension: inicialmente GREEN43397d, sin producción adicional. JSON literal independiente de recibo15 sin extension se deserializa con null interno y conserva datos. Es compatibilidad de decoder, no prueba de upgrade PostgreSQL.
4. Regresión pertinente @s22: test PostgreSQL EXISTENTE WorkSessionTransitionStoreTest.s1_closeCommitsLastIntervalReceiptAndIndependentEvent detectó extension:null añadido al JSONB CLOSE: RED863773. Mínimo Store.receiptJson retira extension cuando null, GREEN162278. No se modificó la aserción histórica ni se fabricó otro RED. Store sólo tiene esa línea de compatibilidad, no implementación parcial de extend.

Firmas reales nuevas: ExtendWorkSessionUseCase.extend(owner,session,key,WorkSessionRevision,int additionalMinutes) devuelve WorkSessionTransitionConfirmation. ReadWorkSessionEndUseCase.read(owner,session) devuelve WorkSessionEndSnapshot(state,serverNow,effectiveEndAt). WorkSessionEnd contiene state/effectiveEndAt/lastDecisionAt interno; WorkSessionExtending opera mediante callback y WorkSessionEndQueries hace lectura mediante callback. WorkSessionExtensionTransition agrupa recibo/eventoExtended, sin añadir más eventos null a WorkSessionTransition histórico. WorkSessionTransitionReceipt conserva constructores6/7 y añade extension8 interno; la representación HTTP sigue responsabilidad del adaptador, aún no implementado17.

Pendiente explícito después de este checkpoint: cantidad/precedencias, fórmula max(fin,reloj), closed/revisión/BIGINT/temporal, marca compartida de P/R/C, replay completo y guardas de E; Store17, migración aditiva, atomicidad, consultas/carreras/upgrade y wiring. No feature completa. C recibirá checkpoint fijo validado por root; no copiar archivos en vuelo ni crear stubs.

Checkpoint nominal FREEZE: SpotlessApply y regresión pertinente40/40 GREEN649354, XML a0e6c1 (36 core heredados,3 nuevos y1 PostgreSQL heredado), cero fallos/errores/omitidos. end_time_first_bundle_results.json conserva recuentos; end_time_first_bundle_hashes.json fija16archivos (13producción,3tests). No hay fakes históricos modificados ni cambios de State6. Fuente quieta para revisión/commit selectivo root y entrega a C; no se declara completa17.

## Ciclos posteriores al checkpoint909b0ca

5. @s2 paused y fin vencido: ExtendWorkSessionTest.s2_extendsAPausedExpiredSessionFromTheConfirmationClock RED2ee62d, GREEN123a2f; fórmula ahora parte de max(fin,now), conserva paused/trabajo, extremo1440 válido.
6. @s9 revisión obsoleta antes del reloj: s9_rejectsAStaleRevisionBeforeConsultingTheClock RED23fea8 (llamaba reloj), reutiliza requireClose para la misma compatibilidad abierta/revisión/máximo. Primer reintento cdf408 detectó error del propio assert que esperaba message en vez del accessor code; corregido sin cambiar excepción existente, GREEN56a81a. Guardas estado/máximo quedan por conectar explícitamente, no se declaran pruebas ejecutadas aún.
7. @s4 cero: s4_rejectsZeroMinutesBeforeEnteringTheStore REDca3380, GREEN6fd14d; ValidationException/FieldError existentes antes del puerto.
8. @s4 máximo excedido1441: s4_rejectsMoreThanOneDayBeforeEnteringTheStore REDcbbc8d, GREENa4f5a9; mínimo amplía límite superior. Tipos JSON/fracción corresponden al adaptador C, no se inventa int fraccionario en núcleo.
9. @s10 EXTEND anterior a última decisión: s10_rejectsAClockEarlierThanTheLastExtension REDb691fe→GREEN692919.
10. @s12 reloj año10000: s12_rejectsAClockInYearTenThousand RED79873a→GREENa4e57c; reutiliza validación temporal State6.
11. @s12 fin desbordado: s12_rejectsAnEndBeyondYearNineThousandNineHundredNinetyNine RED5f74cc→GREENabd539; verifica nuevo fin mediante la guarda existente, sin redondeo.
12. @s14 GET anterior a marca: ReadWorkSessionEndTest.s14_rejectsAReadClockBeforeTheLastConfirmedDecision REDcb2ce4→GREENec7a2d.
13. @s14 GET fuera de rango: s14_rejectsAReadClockOutsideTheUtcRange RED0a6946→GREEN0aa80c. Refactor en GREEN reúne la guarda de estado/marca en WorkSessionEnd.requireTime; ambos focos11casos GREEN045626. No hay segundo Clock ni cambio State6.
14. @s1 PG real: WorkSessionTransitionStoreTest.s1_extendCommitsEndRevisionReceiptAndIndependentEvent RED7b132a puerto no implementado→GREENd30c9e. V18 añade sólo effective_end_at/last_decision_at nullable, Store confirma proyección/recibo/evento11 con oráculo JSON independiente; no escribe intervalos ni altera evento de inicio. Constraints adicionales/upgrade siguen pendientes.
15. @s6 identidad token PG: s6_extendRequiresTheTokenSessionIdentityBeforeBusiness RED507b9a→GREEN18c7ac, propiedad primero y sin Clock. Incidente1106ea era helper de fixture inexistente, corregido antes de RED funcional.
16. @s7 replay tras cierre y nueva sesión: s7_replaysAnExtensionAfterClosureAndAnotherStartWithoutClock REDd3dcb4→GREEN3e5ec7. @s8 cantidad distinta: s8_rejectsAnotherQuantityForTheSameExtensionKey RED3fe324→GREEN8aae97; requireExtensionIntent reutiliza identidad/acción/revisión y añade cantidad.
17. @s10 guarda PG compartida PAUSE: s10_pauseCannotPrecedeTheLastExtension RED618299→GREEN108376; callback conserva su Clock único y Store valida receipt.occurredAt contra marca antes de writes. Incidente9203c8 por edición de saltos de línea dejó variable antigua; corregido sin cambiar contrato. RESUME s10_resumeCannotPrecedeTheLastExtension inicialmenteGREEN1606ca; CLOSE s10_closeCannotPrecedeTheLastExtension inicialmenteGREENfcd690, fixtures paused reales tras extensión.
18. @s10 actualización de marca: s10_resumeAdvancesTheLastDecisionWithItsAlreadyCapturedInstant RED7238de→GREENa6a853; UPDATE compartido P/R/C persiste el occurredAt ya capturado. No añade Clock ni cambia State6/puerto histórico.
19. @s13 PG E: s13_readsThePersistedEffectiveEndWithoutWriting RED5457b3→GREEN4e6079, reutiliza plantilla stateSnapshot read-only RR. Refactor tres mappers a endState compartido; regresión423661/e4543a encontró3oráculos nuevos con conteos globales que incluían fixtures anteriores (BeforeEach crea owner distinto, no borra todaBD). b898b2 confirmó causa. Se restringieron sólo esos conteos nuevos por session_id/aggregate_id, sin alterar producción ni tests anteriores.

Segundo checkpoint nominal PG y guardas:21/21GREEN540101, recuentos preservados7339a3 (8Extend,3ReadEnd,1JSONcompat,9PG). Spotless final7339a3. end_time_pg_checkpoint_hashes.json fija18archivos propios. Pendientes reales: colisión postrollback/concurrencia/atomicidad17, integridad/upgrad eV18, observaciónRR/fallosconsulta, wiring y regresión integrada. No se presenta la suite completa heredada como ejecutada en este corte; últimos36core+PGclose siguen evidencia del primercheckpoint antes de estos cambios compartidos. Se ejecutarán los pertinentes al cierre.

### 20. Wiring de P y E

Cada bean se exigió mediante una llamada que alcanza el Store real en contexto fresco. P: RED `fab1e1` (bean ausente) → GREEN `759b1c`; E: RED `c1410c` → GREEN `661543`. El intento previo `c97295` no encontró tests porque la inserción textual usaba finales CRLF frente a LF: incidente de edición, no RED. No se añadieron puertos simulados de producción ni consultas previas en HTTP.

### 21. Colisión y atomicidad de EXTEND

`end_s20_crossSessionCollisionRollsBackBeforeFreshIntentLookup`: RED `bbeb1c` → GREEN `26441f`. La primera consulta de key se oculta controladamente; PostgreSQL mantiene A cerrada y B como única abierta. La violación real de owner/key se recupera tras rollback en otra transacción (`txid_current` distintos), valida intención y devuelve conflicto sin residuos de B. No representa una carrera natural de dos sesiones abiertas.

La supresión de proyección (`ddebb3`), recibo sin ganador (`3f2a2a`), outbox (`756df8`), fallo diferido de COMMIT (`74e7e2`) y restricción distinta de owner/key (`3e84f3`) son cinco ejemplos individuales inicialmente GREEN. Se reutiliza fixture de triggers con restauración en finally; compara toda la fila de sesión (incluidos fin y marca) e intervalos/recibos/outbox del agregado. La supresión del recibo llega a recuperación sin ganador y conserva 503; no se confunde con conflicto.

Wiring selectivo: Spotless real y 2/2 GREEN `8be983`, hashes `end_time_wiring_checkpoint_hashes.json`. El adaptador es real, pero JDBC/manager son simulados; no se afirma integración HTTP ni PostgreSQL real.

### 22. Carreras con locks PostgreSQL observables

La misma key e intención produjo un solo recibo, revisión y evento, con replay false/true: inicialmente GREEN `0a2f75`. Dos EXTEND distintos: inicialmente GREEN `9deac4`; EXTEND/PAUSE: inicialmente GREEN `8d3183`. Se reutiliza la barrera de sesión que observa ambas conexiones esperando Lock y ambos futuros pendientes antes de liberar. El oráculo común contrasta estado final, fin, marca y número de intervalos según el único ganador; no impone quién gana.

### 23. Últimas garantías y compatibilidad

EXTEND/RESUME `c597ea` y EXTEND/CLOSE `51f20e` completan las cuatro carreras @s19, inicialmente GREEN. E read-only RR con writer independiente terminado antes del Clock: inicialmente GREEN `2d86bd`; fallo al finalizar lectura: `5bc417`.

La recuperación @s20 de intención idéntica usa una instantánea antigua controlada después de adquirir el lock real y oculta la primera consulta de key. PostgreSQL conserva un ganador real; la escritura colisiona realmente y la nueva lectura usa otro txid. Inicialmente GREEN `a5b62e`. Es fault injection de la frontera de recuperación, no afirmación de carrera natural bajo FOR UPDATE. El caso de ausencia se acredita con la supresión de recibo sin ganador del paso21.

@s21 inicialmente GREEN `1f4d9e`: proyecto, tarea, preferencia y otra sesión permanecen bloqueados mientras el worker confirma la ampliación propia en menos del timeout, sin modificar los recursos bloqueados. `c4ab47` fue un defecto de fixture (preferencia aún inexistente), corregido sembrando la fila necesaria; no RED de producto.

@s11 RESUME exactamente en T de una ampliación, preservando trabajo y fin: inicialmente GREEN `b3b4eb`. Refuerzo de acumulación de dos días sin límite acumulativo y mismo microsegundo: `8b01fe`. Contexto proyecto/tarea completed permite EXTEND: inicialmente GREEN `66f4b7`; `f37a8b` fue fixture inválida sin completed_at de tarea, corregida conforme al esquema histórico.

V18 rechaza effective_end_at anterior a planned_end_at: RED `254f78` → GREEN `3c647c`, con un CHECK aditivo y nullable para datos anteriores. No se alteran V14–V17 ni se materializa fallback por GET.

Upgrade explícito17→18: `WorkSessionEndMigrationTest` inicialmente GREEN `fe8463`, conserva filas de running/paused/closed, intervalos, keys y JSONB6 P/R y JSONB7 CLOSE, además de los eventos de inicio retenidos. La siembra SQL reproduce formas históricas, sin ejecutar Store17 contra esquema anterior. `713e1f` fue un error de tipo del fixture (LocalDate frente a String), no RED funcional. Lecturas E prueban fallback del fin/marca y ausencia de backfill.

Regresión heredada detectada y corregida sólo en fixture: `WorkSessionClosureMigrationTest` RED `0b397c` porque intentaba PAUSE actual sobre V16. Se conservó el propósito usando siembra explícita histórica State6/receipt6/evento12. La comparación comprueba la misma cantidad de filas y todos los valores previos, admitiendo únicamente las columnas aditivas posteriores. El test de migración15 ya era compatible y no se modificó. Ambas migraciones heredadas GREEN `1f2ab5`.

### 24. Freeze A+B backend17

Formato Spotless real y regresión focal final `dc5c90` EXIT0: **175 tests en13 suites**, sin fallos/errores/omitidos. XML preservados en `progress/end_time_backend_final_xml/`; inventario exacto con SHA en `end_time_backend_final_results.json` (lectura `6279c0`). Incluye8 Extend,3 ReadEnd,1 JSONB,94 Store transiciones,1 Store inicio,3 migraciones,36 core15/16,28 contextos y1 integración HTTP+PG de C. No son175 tests nuevos de17 ni una suite global. `git diff --check -- backend` GREEN `28d811` (aviso de normalización CRLF de V18, sin error).

Freeze de26 archivos relevantes en `progress/end_time_backend_final_hashes.json`; incluye dependencias compartidas ya versionadas y sus tests además del delta final. No se modificaron V14–V17. El status transitorio de V14 observado tras el test de inicio no presenta diff de contenido; la prueba heredada restaura su recurso. El único fixture histórico modificado por A es `WorkSessionClosureMigrationTest`, explicado arriba. La configuración/harness y HTTP/publicador fueron integrados por root desde C y no forman parte de esta autoría.

No hay Gradle activo ni campañas iniciadas. El paquete queda para revisión independiente; pruebas globales, PIT/Stryker, recorrido Rabbit/API reiniciado y E2E/UX17 son gates posteriores, no acreditados por esta regresión.

### Mapa final de backend y reutilización

Prefijos de suites: **Extend**=`ExtendWorkSessionTest`, **E**=`ReadWorkSessionEndTest`, **PG**=`WorkSessionTransitionStoreTest`, **Upgrade**=`WorkSessionEndMigrationTest`. Los nombres sin prefijo end_ pertenecen a los primeros ciclos17 o a contratos anteriores: el tag aislado nunca sustituye el nombre de método.

| Contrato17 | Evidencia concreta / frontera |
| --- | --- |
| @s1 | Extend.s1_extendsTheEffectiveEndWithoutChangingStateOrWork y PG.s1_extendCommitsEndRevisionReceiptAndIndependentEvent: fórmula anticipada, µs, estado sólo revisión y JSON/evento independientes. |
| @s2 | Extend.s2_extendsAPausedExpiredSessionFromTheConfirmationClock; PG.end_s2_completedProjectAndTaskDoNotPreventExtension. La fórmula max es común a ambos estados y no consulta contexto completado. |
| @s3 | PG.end_s11_sameMicrosecondAllowsAnotherExtensionWithoutACumulativeDayLimit confirma dos ampliaciones de1440, acumulado2880, sin trabajo nuevo. |
| @s4–5 | Extend cero/1441 antes del puerto. Tipos/queries/cabeceras/JSON son frontera HTTP de C; no se simula parsing en núcleo. |
| @s6 | PG.s6_extendRequiresTheTokenSessionIdentityBeforeBusiness y HTTP+PG EndTimeIntegrationTest para propiedad ajena. Orden concreto Store: SELECT owner/id → identidad → replay; la prueba integrada de propiedad no instrumenta el Clock ni se presenta como una prueba distinta de esa instrumentación. |
| @s7–8 | PG.s7_replaysAnExtensionAfterClosureAndAnotherStartWithoutClock y s8_rejectsAnotherQuantityForTheSameExtensionKey; PG.end_s20_crossSessionCollisionRollsBackBeforeFreshIntentLookup para sesión/acción ajenas. requireExtensionIntent llama a requireIntent(session, EXTEND, expected), cuya identidad/acción/revisión se conserva: Change.s17_receiptIntentIncludesSessionIdentity y PG.s17_keyCannotChangeExpectedRevision/s17_keyCannotChangeActionBeforeCheckingCurrentRevision, reejecutados en la regresión. |
| @s9 | Extend.s9_rejectsAStaleRevisionBeforeConsultingTheClock conecta la guarda única WorkSessionState.requireClose. Sus ramas closed y máximo están comprobadas sin Clock por Close.s13_closedSessionRejectsNewCloseBeforeClock y Close.s14_exhaustedRevisionPrecedesClock; stale antes de closed por Close.s13_staleRevisionPrecedesClosedStateAndClock. No se atribuyen tres tests nuevos a EXTEND. |
| @s10 | Extend.s10_rejectsAClockEarlierThanTheLastExtension; PG.s10_pauseCannotPrecedeTheLastExtension/s10_resumeCannotPrecedeTheLastExtension/s10_closeCannotPrecedeTheLastExtension. PG.s10_resumeAdvancesTheLastDecisionWithItsAlreadyCapturedInstant conecta UPDATE compartido de P/R/C con una sola captura. |
| @s11 | PG.end_s11_resumeAtTheExtensionMicrosecondPreservesTheExtendedEnd, exactamente paused→RESUME en T de EXTEND, sin sumar descanso. |
| @s12 | Extend.s12_rejectsAClockInYearTenThousand/s12_rejectsAnEndBeyondYearNineThousandNineHundredNinetyNine. Año0000 queda en la guarda compartida now<changedAt para todo estado válido de años1–9999; Close.s7_rejectsClockBeforeLastChange y Change.s7_rejectsBackwardClockInsteadOfClamping acreditan ese rechazo observable. No hay un test nuevo de EXTEND con literal0000. |
| @s13 | PG.s13_readsThePersistedEffectiveEndWithoutWriting; Upgrade.s22_upgradePreservesHistoricalStatesAndReceiptsWithoutGetBackfill y la integración HTTP+PG de C acreditan proyección anterior y cerrada. |
| @s14–15 | E.s14_rejectsAReadClockBeforeTheLastConfirmedDecision/s14_rejectsAReadClockOutsideTheUtcRange; PG.end_s13_snapshotExcludesAnExtensionCommittedBeforeClock verifica read-only RR y dos lecturas coherentes con writer terminado; PG.end_s11_resumeAtTheExtensionMicrosecondPreservesTheExtendedEnd lee E exactamente en la marca. |
| @s16 | PG.end_s16_readCompletionFailureIsNotAValidEndSnapshot conecta la plantilla de E con traducción del fallo al finalizar. El wrapper storage es el mismo que s24_stateSqlFailureIsNotAbsence y s24_keySqlFailureIsNotAbsence, reejecutados; C conecta E503 en HTTP. No se afirma una segunda prueba SQL específica de E inexistente. |
| @s17 | Cinco end_s17_* individuales: proyección, recibo sin ganador, outbox, COMMIT diferido y otra restricción. Comparación de la fila completa y efectos del agregado. |
| @s18–19 | end_s18_sameKeyRaceConfirmsExactlyOneExtension y cuatro end_s19_*: dos EXTEND, PAUSE, RESUME, CLOSE. Dos conexiones bloqueadas observadas, una revisión ganadora sin orden impuesto. |
| @s20 | end_s20_crossSessionCollisionRollsBackBeforeFreshIntentLookup (A cerrada/B abierta), end_s20_sameIntentRecoveryUsesTheDurableWinnerAfterRollback (snapshot/lookup controlados) y end_s17_suppressedReceiptWithoutWinnerRollsBackExtension. Son pruebas de recuperación posterior al rollback y UNIQUE real; el éxito fallback no se deduce de la carrera serializada @s18. |
| @s21 | end_s21_otherOwnerAndPlanningLocksDoNotBlockExtension mantiene los cuatro recursos bloqueados y sin alteraciones. |
| @s22 | Upgrade nuevo y las dos migraciones heredadas; JSONB P/R6/CLOSE7 y columnas anteriores preservados, nuevos NULL resueltos sin backfill. CHECK end>=plan RED→GREEN. |
| @s23 | EndTimeIntegrationTest.s1_s7_s13_s23_extensionRemainsDurableAndRecoverableAfterClose de C, incluido en175, recupera C/K después de cierre y retirada exclusiva outbox17. Reinicio real de proceso/API y ampliación posterior antes del cierre corresponden al smoke aún pendiente; instanciar Store nuevo no se presenta como reinicio de API. |
| @s24–44 | Publicador/HTTP/cliente/panel/E2E/UX son autorías y gates separados. Este freeze no atribuye su cobertura al núcleo. |

No hay defecto funcional conocido pendiente de A en este corte. La evidencia compuesta conserva los límites anteriores para que el juez evalúe reutilización sin inflar número de ejemplos ni fabricar carreras.
