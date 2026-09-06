# TDD backend — today

Contrato a127747, features/today.feature38 escenarios. Propiedad backend y esta bitácora; init global previo94736 aceptado por coordinador, no repetido durante TDD concurrente. Ponytail full/Caveman lite y arquitectura de dominio puro. Sin PIT, commits ni cambios frontend/harness.

## Ciclos

1. @s1 TodayWindowTest.s1_emptyDayKeepsKnownCapacityAndNoCandidates. Primer comando9f2cb6 no creó el archivo por prefijo backend duplicado en cwd; no fue RED de producto. Corregida ruta, RED compilación b9b236 por TodayWindow ausente. Implementación mínima de ventana UTC/configurada y resumen vacío; GREEN cff483,1 test. Cálculo de agenda no vacía y fallback aún pendientes; no se presentan como implementados.

## Trazabilidad en curso

@s1: núcleo del día vacío cubierto, HTTP y no escritura pendientes. @s2–15 pendientes. Próxima prueba: capacidad con bloques reales. Tipos actuales TodayWindow y TodayWindow.Agenda, susceptibles de refactor en frontera verde.

2. @s2 capacidad cuatro vectores: RED4c0952/b54444, 4 fallos por planificado0; suma de duraciones y restantes/exceso max0, GREEN783d36,5 casos acumulados.
3. @s3 fallback dos casos: RED6c5c8c NoSuchElement/ZoneRules; UTC con fuente y zona guardada, presupuesto/restante/exceso null conservando bloque; GREENd27c75,7 acumulados.
4. @s4 intersección cinco casos: REDb7163c,4fallos y caso interior ya verde; filtrar sólo intersección positiva y recortar únicamente suma, GREEN22561c,12 acumulados.
5. @s5 tres días Madrid23/25/24h y presupuesto Sunday45/Monday120: GREEN inicial69e7a8/bfc435 sobre atStartOfDay existente. Sin cambio productivo ni RED fabricado;15 casos acumulados.
6. @s6 cierre real de bloque medianoche,2variantes: prueba añadida, ejecuciónfb36aa/45922 pendiente al registrar este avance. No bloqueo; tests Gradle ~9–10s cada ciclo. HTTP aún no iniciado.

6 final: RED17f895 dos cierresnull; máximo fin real implementado, GREENe37cf6/09ccf8,17casosacumulados. @s7 siguiente: matriz5 extremos con entradasdesordenadas añadida, ejecución7c66a0.

7. @s7 cinco extremos y agenda desordenada: RED3b76fa,cincofallos; selección actual semiabierta, próximo inicioestricto y ordeninicio implementados. GREEN7e1b4f/75211,22casos acumulados. Próxima frontera: HTTP@s1 y puertos de lectura; nombres@8 y total21@9 pendientes integración.

8. @s1 HTTP PostgreSQLreal: TodayApiTest.s1_emptySnapshotHasExactClosedSchemaAndDoesNotWrite RED76fbb9 sinendpoint; se añaden ReadTodayUseCase/TodayQueries/ReadToday, adaptación inicial PGpreferencia y TodayController15 campos. ApplicationConfiguration conecta el servicio. EjecuciónGREEN4ad9bf/8241. Store aún entregaagenda vacía: siguiente prueba exige bloques+nombres; RR pendiente@s10, sin afirmaciónprematura.

9. @s8 cuatro estados entidad: primerRED ee50c8 tuvo dosfallosfixture completed_at!=updated_at; corregidos timestampsfixture,REDreal70daba cuatroitems vacíos. TodayItem añadebloque+nombres, SQLowner/intersección+join tasks/proyectos, reutiliza PostgresBlockStore.MAPPER (visibilidadpaquete) yBlockResponse.from. Adaptado núcleo aTodayItem preservandooráculos. GREENd118c3/32331,27casos (22dominio+5HTTP). RR aúnpendiente; no migración/índice añadido.

10. @s9 21reservasinsertadasalrevés: GREENinicialbc9bf8/4ef435, todoslositems+resúmen/candidatoscompletos; sinprodextra.
11. @s10 snapshot dosestados: RED7cd1dd agenda mezcladaconwriter PG confirmado despuésleerpreferencia; transacción localreadOnlyREPEATABLE_READ sinlocks ysinmodificartemplatecompartido, GREENe225bd/81694. Writer confirmaenotrothread mientrasreadabierta; nuevalecturavecambiocoherente.30casosacumulados.

12. @s11 reloj único+precisión heredada: REDa89d0c serverNow9decimales; ReadToday captura clock.instant una vez y truncaMICROS antesventana. GREEN5a8bfe/44374. Test fija valorposteriorotrodía y verificauna captura,fecha/bordes/candidatos/suma coherentes.31casosacumulados.

13. @s12 aislamientoowner conreservas/nombres/presupuesto privadoB: GREENinicial7918a2/d44329 víaSQLownerexistente.14. @s13 autenticaciónantesquery: GREENinicialc8ce88/3814cb víaSecurityexistente. No cambiosprodparaestoscasos.33acumulados.15. @s14 cincoparámetros: pruebaejecutándose6d053a/3440, todavíaendpointaceptaqueries.

15 final: query cinco RED ff83a2 → GREEN a947e6; 38 casos acumulados.16. @s15 tres fases storage: RED 527c64 bloque SQL500; fallo commit inicialmente era fixture amplio que también interceptaba Spring Session (17f6d7 SESSION_UNAVAILABLE), no RED válido de agenda. Catch DataAccess/Transaction alrededor de execute completo; consulta bloque GREEN854ba7. Fixture commit delimitado a transacción readOnly de agenda, tres casos GREEN27e022. Fallo al cierre observado sólo en fixture corregido ya verde; no se atribuye un RED inexistente a ese caso. 41 casos acumulados. Siguiente frontera contextos existentes y wiring Today.

17. Contextos heredados ApplicationWiringTest/ProjectStateConfigurationTest: RED d0e22e,17 fallos por TodayQueries ausente confirmado en XML031d22. Añadido únicamente puerto controlado a ambos fixtures;21GREEN e498c1 conservan oráculos operacionales. Búsqueda withUserConfiguration(ApplicationConfiguration) encontró sólo estos dos fixtures.
18. ApplicationWiringTest.today_s1_readingBeanReachesItsPortInFreshContext: GREEN inicial258706; resuelve ReadTodayUseCase, ejecuta get y obtiene señal tipada de TodayQueries. No producción adicional.
19. @s3 HTTP fallback dos estados: GREEN inicial574a7a. Preserva zona histórica del bloque aun retirada del catálogo, campos null explícitos y filas intactas.
20. @s10 configuración efectiva de PostgreSQL: GREEN inicial7c1ac8. SHOW transaction_read_only=on y transaction_isolation=repeatable read dentro del callback; fuera readOnly=off. Complementa writer concurrente confirmado sin bloqueo de los dos casos @s10.
21. @s4 filtro SQL cinco intervalos: GREEN iniciald32ab3. Observa filas devueltas por JDBC antes del filtro del dominio, por lo que cargar historial completo no puede ocultarse tras summarize; comprueba fronteras positivas y DTO de intervalo íntegro. Sin producción adicional.

Frontera funcional:49 casos nuevos TodayWindow/TodayApi por ciclos y un wiring nuevo, más21 contextos previos en verde. Pendiente formato, scope autorizado today y regresión conjunta; src/main/build.gradle congelados para COPY Docker E2E solicitado por coordinador. No mutación ejecutada.

## Mapa de aceptación backend

- @s1: TodayWindowTest.s1_emptyDayKeepsKnownCapacityAndNoCandidates + TodayApiTest.s1_emptySnapshotHasExactClosedSchemaAndDoesNotWrite; wiring operativo ApplicationWiringTest.today_s1_readingBeanReachesItsPortInFreshContext.
- @s2: TodayWindowTest.s2_capacityUsesReservedSeconds (4 vectores).
- @s3: TodayWindowTest.s3_fallbackRetainsBlocksWithoutInventingBudget y TodayApiTest.s3_httpFallbackPreservesHistoricalBlockAndUnknownCapacity (2 estados cada uno).
- @s4: TodayWindowTest.s4_countsOnlyPositiveIntersectionsWithoutClippingStoredBlock y TodayApiTest.s4_sqlLoadsOnlyPositiveIntersections (5 cada uno; segundo observa resultado SQL antes del resumen).
- @s5: TodayWindowTest.s5_usesRealLocalMidnightsAndWeekdayBudget (Madrid23/25/24h y presupuesto por weekday).
- @s6: TodayWindowTest.s6_midnightBlockKeepsItsActualClosingTime (ambos días, cierre real).
- @s7: TodayWindowTest.s7_currentAndNextUseSemiOpenBoundaries (5 extremos).
- @s8: TodayApiTest.s8_readsCurrentNamesAndUnmodifiedBlockForEveryEntityState (4 combinaciones válidas).
- @s9: TodayApiTest.s9_returnsAllTwentyOneSortedBlocksAndWholeDaySummary.
- @s10: TodayApiTest.s10_snapshotDoesNotMixConcurrentPreferenceNamesOrBlocks (ausencia/presencia; writer confirma mientras lectura abierta) y s10_postgresSnapshotIsActuallyReadOnlyRepeatableRead (propiedades PostgreSQL efectivas y sin contaminar conexión siguiente).
- @s11: TodayApiTest.s11_capturesClockOnceAndCanonicalizesMicrosBeforeChoosingDay.
- @s12: TodayApiTest.s12_neverIncludesOtherOwnersBlocksNamesOrCapacity.
- @s13: TodayApiTest.s13_authenticationPrecedesUnknownQueryValidation.
- @s14: TodayApiTest.s14_rejectsEveryClientParameter (5 querys).
- @s15: TodayApiTest.s15_storageFailuresNeverBecomeEmptySnapshots (preferencia, bloques, cierre).

## Diseño y alcance

Nuevos domain/TodayWindow.java y TodayItem.java; application/ReadTodayUseCase.java, TodayQueries.java y ReadToday.java; adapter/http/TodayController.java y adapter/persistence/PostgresTodayQueries.java. Dominio sólo java/domain. La frontera TodayQueries acepta cálculo puro de ventana después de leer preferencia en la misma transacción; Clock y ZoneCatalog se capturan desde aplicación. Store usa un TransactionTemplate local readOnly/RR, un SELECT preferencia y un SELECT de bloques con JOIN de nombres propios e intersección SQL. No infraestructura ni migración ni outbox nuevos. Índices existentes de planned_blocks y owner/proyecto se reutilizan.

Compartidos: ApplicationConfiguration conecta ReadToday; PostgresBlockStore.MAPPER pasa de private a package-private para reutilizar lectura DTO histórica sin duplicarla. Esta visibilidad no añade ramas mutables; el coordinador aprobó regresión de store en lugar de incluir toda su lógica histórica en campaña today. Contextos ProjectStateConfigurationTest/ApplicationWiringTest añaden TodayQueries controlado; aserciones previas conservadas.

## Entrega para judge — freeze backend

Tras COPY Docker confirmado0c132c, scope today aprobado configurado en backend/build.gradle.kts. Se incluyen completos TodayWindow*,TodayItem,ReadToday,TodayController,PostgresTodayQueries,ApplicationConfiguration y las cuatro suites TodayWindowTest/TodayApiTest/ApplicationWiringTest/ProjectStateConfigurationTest. Default añade los dos adaptadores nuevos y suites; core ya cubría dominio/aplicación. Destino separado reports/pitest-today. Umbral80 y exclusiones heredadas sin cambio; no PIT ejecutado.

SpotlessApply y regresión focal conjunta c94da8 EXIT0 en42s. XML8f8c35:283tests,0fallos,0errores,0skip: TodayWindow22,TodayApi27,ApplicationWiring15,ProjectStateConfiguration7,Architecture1,ScheduleBlockApi173,ScheduleBlockPersistence38. Esta última pareja comprueba regresión del MAPPER compartido. Diff --check backend sin errores1bb93a (sólo aviso normal CRLF/LF). Fuentes/tests/config congelados para revisión independiente; no commits/done ni puerta de mutación anticipada.
