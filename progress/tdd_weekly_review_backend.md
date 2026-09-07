# TDD backend19 — revisión semanal

Contrato34 escenarios/100 ejemplos, freeze root4fbc370. Autorización TDD previa
@s1–24 conservada; init62482 EXIT0 antes del ciclo. Sin suites globales nuevas.
Ponytail full/Caveman lite, docs/tdd leídos77edb8. A posee núcleo/PG/wiring; C
HTTP aislado y B frontend. No cambios a V14 ni rutas protegidas.

## Ciclo1: nominal vacío y frontera de captura temporal

ReadWeeklyReviewTest.s1_emptyWeekUsesOneClockOnlyAfterTheSnapshotCallback.
RED compilación real9361c1 por ausencia de tres tipos de aplicación, antes de
escribir producción. GREEN f3dff5; formato real y focal completo47cc69 EXIT0,
1/1, cero fallos. No se atribuye RED funcional adicional: falta de compilación
cuenta como primer RED según docs/tdd.

El doble de WeeklyReviewQueries establece una marca de snapshot y sólo después
invoca el callback; el Clock verifica esa marca y se llama exactamente una vez.
Se obtiene semanaUTC07–13septiembre, siete días continuos, tres duraciones
correctas con capacidadnull, contador0 e instante truncadoµs. Oráculo compara
las fronteras/fechas/valores públicos, no serialización del mismo record.

Límite: prueba de frontera de puerto y núcleo, no PostgreSQL, aislamiento real,
HTTP ni snapshotdurable. Primer bundle implementa sólo omisión de date/zone y
preferencia vacía; date explícita, catálogo/preferencia, datos no vacíos,
integridad, errores y persistencia son siguientes ciclos. Constructor conserva
la firma convenida con ZoneCatalog, todavía no consumido por este nominal;
no se afirma soportar los otros valores por compilar la firma. Ningún puerto
abstracto tiene stub/default; el doble sólo vive en la prueba.

WeeklyReviewWindow es detalle interno A, inicialmente sólo serverNow y
emptyReview; se ampliará por próximos tests. C consume ReadWeeklyReviewUseCase
y WeeklyReview con records Day/Totals, nunca la ventana interna.

## Freeze nominal

Cinco fuentes y un test en weekly_review_first_bundle.json, con hashes y XML
preservado. No wiring ni adaptadorPG en este corte; root puede revisar/versionar
este bundle para que C implemente el nominal HTTP contra tipos reales. No
feature terminada ni pruebas de todas las familias.
## Ciclos 2–5: selección temporal y capacidad

Cada caso se añadió individualmente y se ejecutó con `gradlew.bat test --daemon --tests '*ReadWeeklyReviewTest.<método>' --max-workers=4` antes de cambiar producción.

- `s2_explicitDateAndZoneSelectTheirCivilWeek`: RED bb6d46 (semana actual en vez de marzo); un reemplazo parcial mantuvo RED 6d8e9d, corregido sin cambiar oráculo. GREEN d1e74c, 2/2. Fecha explícita y semana DST de 167 horas.
- `s2_storedZoneUsesItsLocalWeekAndCurrentCapacity`: RED ce068c; GREEN 425c55, 3/3. También acredita @s3 y capacidad nominal @s15: lunes local cuando UTC sigue domingo, 120 minutos por día.
- `s2_unavailableStoredZoneFallsBackWithoutMovingItsBudget`: RED ff1ffe (ZoneRulesException); GREEN e0b211, 1/1. Fallback UTC, zona guardada visible y capacidad desconocida.
- `s15_explicitDifferentZoneKeepsCapacityUnknown`: RED 3d648f (presupuesto trasladado); GREEN 8354af, 1/1. EXPLICIT conserva la zona guardada, sin trasladar capacidad.

Las firmas públicas de WeeklyReview/get permanecen iguales. Sólo la ventana interna evoluciona. No se atribuye aún evidencia PostgreSQL.
## Ciclos 6–13: validación y nominal PostgreSQL/wiring

Comando focal idéntico al anterior con el método indicado, sin suite global.

- `s4_unknownExplicitZoneFailsBeforeQueriesOrClock`: RED ff926c; GREEN 751175. Error zoneId/INVALID_VALUE antes de consultar y capturar reloj.
- `s6_explicitWeekBeyondCivilRangeFailsBeforeSnapshot`: RED 9ca663; GREEN c56d89. Semana civil incompleta produce date/INVALID_VALUE.
- `s6_utcBoundaryBeforeYearOneIsATemporalConflict`: RED de compilación 9b5839; GREEN 251114. Exige WeeklyReviewTimeOutOfRangeException real.
- `s6_unrepresentableClockFailsEvenForAnExplicitSafeWeek`: RED ed8c79; GREEN 45cc5a.
- `s6_localClockBeyondYearRangeFailsEvenWithExplicitDate`: RED 1b2281; GREEN 6b8cd7.
- `s6_defaultWeekWhoseSundayExceedsYearRangeIsATemporalConflict`: RED 09ff9f; GREEN 02389a, regresión de los 11 casos.
- `WeeklyReviewPersistenceTest.s1_emptyWeekReadsPostgresInReadOnlyRepeatableRead`: RED de compilación f5f52a; GREEN 2fea66, PostgreSQL17.9 real migrado. El callback del Clock comprueba SHOW transaction_isolation=repeatable read y transaction_read_only=on. Availability.find se ejecuta antes del callback; el caso prueba nominal vacío, todavía no carrera con escritor ni agregación de hechos.
- `ApplicationWiringTest.weekly_s1_readBeanUsesTheRealQueries`: RED 07061d (bean ausente); GREEN c5a75d. Usa casos de uso y adaptador concretos con JdbcTemplate/transaction manager simulados; no se atribuye PostgreSQL ni HTTP a este oráculo.

ReadWeeklyReviewUseCase y WeeklyReview no cambian. La excepción temporal vive en domain, como las demás reglas puras. No se ha añadido migración ni endpoint. El adaptador nominal aún devuelve la semana vacía tras disponibilidad; agregación de hechos y errores se incorporan mediante siguientes casos.

Formato real y regresión de las tres clases: 5281e8 EXIT0, 35/35 (11 aplicación, 1 PostgreSQL y 23 wiring), cero fallos/errores. Manifiesto weekly_review_nominal_bundle.json y XML preservados en weekly_review_nominal_xml. Este corte permite integrar HTTP nominal; aún faltan agregaciones, corrupción y carreras reales.

## Ciclos 14–19: primeras sumas

- `WeeklyReviewWindowTest.s8_plannedIntervalsAreClippedToTheirCivilDays`: RED de compilación 4985a6; GREEN10c8b6. Intervalo mínimo interno, suma por intersección diaria.
- `WeeklyReviewPersistenceTest.s8_projectedPlanIsReadAndClippedEvenForCompletedContext`: RED5be3ac; GREENc3c488. Reservas reales y contexto completed, filas intactas; lectura OffsetDateTime.
- `s9_movedReservationUsesOnlyItsCurrentProjectedInterval`: incidente de fixture091476 (updated_at obligatorio omitido), corregido antes de RED funcional195eea; GREEN197b3c. Proyección vigente sustituye original, no suma ambos.
- `s9_cancelledReservationDoesNotContributeItsOriginalPlan`: REDa195de; GREEN25d883.
- `WeeklyReviewWindowTest.s13_workPreservesMicrosecondsAndZeroLengthIntervals`: RED de compilación470e92; GREEN2ba218, 2/2 dominio. Dos microsegundos y tramo de duración cero.
- `WeeklyReviewPersistenceTest.s10_pausedSessionCountsClosedIntervalsWithoutAddingItsAccumulatedTotal`: REDb12e71; GREEN96c6ab.
- `s10_runningSessionAddsOnlyItsLiveTailToClosedIntervals`: REDde02fa; GREEN0c951f. Veinte minutos cerrados más veinte abiertos, sin sumar descanso ni plannedMinutes.

No se acredita todavía integridad de los intervalos ni retroceso de reloj seleccionado. Las consultas evolucionan con los siguientes oráculos, sin N+1 ni tablas nuevas.

## Integridad y selección: ciclos siguientes

Los comandos siguen siendo el focal del método, uno a uno.

- `s22_pastIntervalStillChecksTheLiveSessionsLatestExtensionDecision`: incidente de sintaxis a4bb94 corregido; RED funcional d33ec3; GREEN 0e913f.
- `s19_selectedSessionsClosedSumMustMatchItsAccumulatedWork`: RED f22fd2; GREEN 49a4b2 con regresión PostgreSQL. Una consulta conjunta trae todas las filas de intervalos de cada sesión seleccionada; no N+1. Sumando sólo intersecciones no se podría validar el acumulado completo.
- `s19_overlappingIntervalsAreRejectedEvenWhenTheirSumMatches`: RED bd5e8a; GREEN 072a7b.
- `s19_intervalBeforeSessionStartCannotBeCredited`: RED d61856; GREEN b8cc2f.
- `s19_intervalAfterClosedSessionEndCannotBeCredited`: RED 895b09; GREEN 09d7ad.
- `s17_legacyClosedSessionsCountOnlyWhenTheirUnknownLifeStartsInTheWeek`: RED 77e492; GREEN c74645, filas sin cambios.
- `s19_missingEndWithAReceiptIsCorruptionNotUnknownLegacyDuration`: RED 30e731; GREEN 1b2394.
- `s18_runningLegacyWithoutRunningSinceUsesItsStartWithoutBackfill`: RED 798b12; GREEN 66a1cb.
- `s14_futureWeekDoesNotSelectALiveTailEntirelyBeforeIt`: RED d7edd1; GREEN 61ef4b. Refuerzo señalado también por root: no seleccionar una cola enteramente anterior al horizonte futuro.
- `s22_selectedUnknownLegacyStillChecksItsDurableDecision`: RED bd9d12; ejecución GREEN pendiente de registrar al recibir salida final.

## Cierre de integridad y evidencia compuesta

- @s22 legacy: GREEN final 99e633 (sustituye la anotación pendiente anterior).
- `WeeklyReviewWindowTest.s19_durationAccumulatorRejectsOverflowInsteadOfWrapping`: RED de compilación ca2618; GREEN 4f0d84. Oráculo acotado del acumulador con Long.MAX_VALUE+1, no millones de filas ficticias. Refactor a ese acumulador para días/totales y addExact para acumulado cerrado; regresión 8930e8 verde.
- `s23_factReadFailureIsStorageUnavailableWithoutPartialSummary`: RED 5e477e; GREEN cbf08c. Tabla aislada renombrada/restaurada en finally, SQL real.
- `s23_commitFailureCannotLeakTheCompletedSummary`: RED bcc7f9; GREEN 567c90. Commit real read-only seguido de fallo de confirmación inyectado en el manager; no se afirma fallo espontáneo del servidor PostgreSQL.
- `s19_arithmeticFailureInsideTheReadTransactionMapsToStorageUnavailable`: RED 7a16ab; GREEN 4b84e5. Inyección aritmética explícita en callback bajo transacción real; acredita traducción, no volumen masivo en PostgreSQL.
- `s19_runningTailCannotOverlapAlreadyClosedWork`: RED 2337a3; GREEN f68760.
- `s19_reversedIntervalCannotCancelPositiveWorkInTheIntegritySum`: RED a1e8e1; GREEN 5ba437.
- `s19_unknownSelectedSessionStatusIsNotTreatedAsPaused`: RED 180920; GREEN f0fccc.
- `s19_pausedSessionCannotRetainAnOpenTail`: RED a26a06; GREEN e93c3b.
- `s21_writerAfterSnapshotCannotMixNewIntervalsWithOldRunningTail`: inicialmente GREEN 412485. Escritor con conexión/transacción distinta confirma durante el callback del Clock, posterior a SELECT de disponibilidad. Cambia cuatro fuentes conjuntamente. Primer resultado conserva running/plan/capacidad originales; segundo ve cierre/intervalo/movimiento/presupuesto completos. No se fabrica RED ni se llama carrera de locks: es intercalación determinista de snapshot y commit reales.
- `s20_everySourceIsScopedToTheAuthenticatedOwner`: inicialmente GREEN fd96d8, dos propietarios con plan, trabajo, sesiones antiguas y presupuestos distintos.
- `s11_yearOneDatesUseTheProlepticCalendarInPostgres`: inicialmente GREEN 2d0806, año0001 leído mediante OffsetDateTime, sin calendario híbrido.
- `WeeklyReviewWindowTest.s7_skippedCivilDayHasZeroDurationsButKeepsItsChosenBudget`: inicialmente GREEN 5ad697, Apia2011 y capacidad actual sin ajuste.
- `s12_fallTransitionCountsThreeRealHoursInsideTheLongWeek`: inicialmente GREEN 1a11fe, 169horas/3horas reales.
- `s12_springTransitionCountsOneRealHourWithoutReducingCapacity`: inicialmente GREEN 94528d, 167horas/1hora real y presupuesto intacto; cubre@s16.
- `s19_unknownLegacyCannotHideAnUnexplainedAccumulatedDuration`: RED 34465c; GREEN 7d1bfe.
- `s11_closedWorkCrossingMondayCountsOnlyItsIntersection`: inicialmente GREEN 0edddc, corte lunes y fin exactamente en frontera excluido. No se utiliza workDate ni se lee el JSON del cierre para distribuir trabajo.
- `s19_liveProjectionCannotBeginBeforeTheOriginalSession`: RED e82de6; GREEN 7185af.

## Última revisión funcional

- `s22_confirmedIntervalAheadOfClockIsATemporalConflict`: inicialmente GREEN 419827, datos coherentes; ese caso por sí solo no separa el extremo del fallback de marca.
- `s15_zeroBudgetRemainsKnownZeroRatherThanUnknown`: inicialmente GREEN af08b2.
- Regresión 4027ee: 87/88 verdes, único fallo del doble JdbcTemplate de ApplicationWiringTest: el nuevo ResultSetExtractor devolvía null por respuesta por defecto del mock. El doble ahora ejecuta el extractor con ResultSet vacío; GREEN 0d3ada, sin cambio productivo ni tolerancia a null en el adaptador.
- Revisión root pidió separar changedAt/intervalo del coalesce de lastDecision. `s22_futureChangedAtCannotBeHiddenByAnOlderLastDecision`: RED 766295; GREEN bef0ed. `s22_futureIntervalHasTemporalPrecedenceOverInconsistentEarlierMetadata`: RED 476d15 (503 incidental), GREEN 6312b8 (409 temporal). Los extremos confirmados se verifican antes de la integridad de esa sesión; no se confía en una restricción V18 que no existe.

La fila imposible de@s6 se sustituyó únicamente tras aprobación root, como explica review_weekly_review_calendar_bound.md; no se añadieron ejemplos ni se cambió la normativa general.

## Mapa del corte funcional A

Los ejemplos Gherkin no son conteos de pruebas. Los nombres abreviados siguientes corresponden a los métodos completos registrados arriba; HTTP pertenece a C y su suite se incluye sólo como regresión integrada.

| Contrato | Evidencia existente | Alcance/límite |
| --- | --- | --- |
| @s1 | ReadWeeklyReviewTest.s1; WeeklyReviewPersistenceTest.s1; ApplicationWiringTest.weekly_s1; WeeklyReviewApiTest.s1 | Núcleo, PostgreSQL real, wiring y DTO HTTP separados. |
| @s2–3 | s2_explicitDateAndZoneSelectTheirCivilWeek, storedZoneUsesItsLocalWeekAndCurrentCapacity, unavailableStoredZoneFallsBackWithoutMovingItsBudget; s15_explicitDifferentZoneKeepsCapacityUnknown | Fuente de zona, fecha explícita y lunes local. |
| @s4–5 | s4_unknownExplicitZoneFailsBeforeQueriesOrClock; WeeklyReviewApiTest s4/s5 | Catálogo/normalización sólo en aplicación, forma query y seguridad en HTTP. |
| @s6 | Seis pruebas temporales de ReadWeeklyReviewTest y review_weekly_review_calendar_bound.md | 400 civil explícito separado de409 derivado; fila imposible sustituida tras aprobación. |
| @s7 | s2_explicitDateAndZoneSelectTheirCivilWeek; WindowTest s7/s12 | DST167/169 y Apia con día saltado. |
| @s8–9 | PG s8_projectedPlanIsReadAndClippedEvenForCompletedContext; s9_movedReservationUsesOnlyItsCurrentProjectedInterval; s9_cancelledReservationDoesNotContributeItsOriginalPlan; WindowTest s8 | Proyección vigente, corte diario/semanal y contexto completed. Movimiento fuera de semana usa el mismo filtro de intersección SQL, sin fixture repetido por cada reserva. |
| @s10 | PG s10_pausedSessionCountsClosedIntervalsWithoutAddingItsAccumulatedTotal, runningSessionAddsOnlyItsLiveTailToClosedIntervals; s17 legacy con sesión closed cuantificable | Running agrega cola; paused/closed sólo intervalos. No se afirma ejecutar individualmente cada historia P/R/C del esquema heredado. |
| @s11 | PG s11_closedWorkCrossingMondayCountsOnlyItsIntersection y yearOneDatesUseTheProlepticCalendarInPostgres | Corte semiabierto y calendario proléptico. La consulta sólo usa existencia de recibos para legacy, nunca workDate para sumar. |
| @s12–13 | WeeklyReviewWindowTest s12 primavera/otoño y s13 microsegundos | Aritmética de instantes, dosµs, cero válido, sin redondeo. |
| @s14 | PG runningSessionAddsOnlyItsLiveTailToClosedIntervals; futureWeekDoesNotSelectALiveTailEntirelyBeforeIt; s22 EXTEND | plannedEnd no limita cola; effectiveEnd no se agrega como plan/trabajo. |
| @s15–16 | Aplicación s2/s15, WindowTest s15_zeroBudget y s12_springTransition | Presupuesto actual, null/cero distintos, no conversión ni ajusteDST. |
| @s17–18 | PG legacyClosedSessionsCountOnlyWhenTheirUnknownLifeStartsInTheWeek, runningLegacyWithoutRunningSinceUsesItsStartWithoutBackfill | Compatibilidad explícita, filas intactas. |
| @s19 | PG s19 de acumulado, vida, solapes, reversión, cola, estado, legacy inconsistente y traducción aritmética; WindowTest.durationAccumulatorRejectsOverflowInsteadOfWrapping | Overflow es evidencia compuesta: acumulador puro + traducción bajo transacción real, no millones de filas materializadas. |
| @s20 | PG everySourceIsScopedToTheAuthenticatedOwner | Dos owners y todas las fuentes. |
| @s21 | PG writerAfterSnapshotCannotMixNewIntervalsWithOldRunningTail; s1 read-only/RR | Intercalación real mediante segundo DataSource y commit dentro del callback. Primera consulta fija snapshot antes del Clock único. |
| @s22 | PG EXTEND/legacy/futureChangedAt/futureInterval y confirmedIntervalAheadOfClock | Validaciones independientes, 409 precede inconsistencia incidental de esos hechos futuros. |
| @s23 | PG factReadFailure, unreadablePreference, commitFailure; HTTP storageFailure | Renombrado/restauración de tablas aisladas, commit controlado y ausencia de resultado parcial. |
| @s24 | Consulta no lee outbox ni escribe; snapshot y filas intactas comprobados en PG | Reinicio real de proceso API y retirada del transporte pendientes de integración/E2E; no se simula como simple recreación de un objeto. |
| @s25–34 | Fuera de propiedad A | Cliente/UI, E2E y UX a cargo de B/C/root; no atribuir desde tests Java. |

No migración nueva: se reutilizan tablas, claves e índices existentes, con tres consultas por lectura (preferencia, plan y sesiones/intervalos seleccionados), sin N+1 de red. Las consultas usan OffsetDateTime para los hechos temporales; V14–V18 no se modifican. No cache, Clock adicional, eventos ni consumidor.

Campos públicos y firma get conservados desde el primer bundle. Las utilidades/ventana internas evolucionaron por los casos activos. El paquete aún requiere revisión independiente, gates globales, mutación y evidencia E2E/UX de feature19; no equivale a feature terminada.

Última comprobación: s23_unreadablePreferenceDoesNotBecomeAnUnconfiguredWeek inicialmente GREEN16ea3a. Formato real y regresión final f86eaa EXIT0: 91/91 (33 PostgreSQL,11 aplicación,7 dominio,23 wiring,17 HTTP de C), cinco XML sin fallos/errores/omitidos. Manifiesto weekly_review_backend_final_bundle.json y raw en weekly_review_backend_final_xml. Fuentes/tests del manifiesto congelados para revisión; ningún Gradle activo.
