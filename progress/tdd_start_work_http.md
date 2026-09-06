# TDD HTTP14

## Entrega del adaptador aislado

49 pruebas MockMvc, 0 fallos, errores u omitidos: ejecución final a47c27 y XML ef9bff. Producción y tests congelados para revisión independiente. No equivale a una aprobación global de feature14.

Se entrega únicamente WorkSessionController.java, WorkSessionApiTest.java y esta bitácora. Los cinco snapshots de interfaz/excepciones permanecen como dependencias ajenas, excluidos de integración. ApplicationConfiguration y la regresión con PostgreSQL se completarán en el árbol común con el núcleo real, según la coordinación de root.

Mapa del alcance:
- @s6–9: límites 1/1440, tipos y rango, JSON estricto, campos extra, query/IDs/key y precedencia de filtros/415. Los rechazos de entrada comprueban que ningún puerto se invoca.
- @s10–15: adaptación cerrada de contexto ausente, estados completados, activa, error temporal, idempotencia y replay 200. El orden transaccional interno corresponde a las pruebas del núcleo; los mocks no lo acreditan.
- @s21–24: GET por ID/key, DTO y ausencia sin Location, active con envoltorio explícito y 503 sin falsa ausencia. La privacidad entre propietarios y la persistencia corresponden al corte integrado; aquí se comprueba el principal remitido al puerto y la respuesta HTTP.
- Se conserva el consejo de errores existente y sus títulos heredados. Sólo los errores específicos de inicio tienen handlers locales.

Cierre del ciclo 24: GREEN e240b2. Refuerzo de la forma cerrada de validación, inicialmente GREEN cb190a: cinco campos y un único error de tres campos. Los casos adicionales consignados abajo fueron añadidos y ejecutados individualmente.

Formato real Spotless 7.2.1 / google-java-format 1.31.0: eb14ac y 95e084. La implementación instalada de IdeHook confirma que recibe una ruta absoluta y escribe sólo ese archivo (59817f). Segunda pasada focal: IS CLEAN 3afdd8 y 7bf5ed; el task Check aparece SKIPPED por el modo hook, por lo que la evidencia precisa es IS CLEAN, no un chequeo global. No se ha formateado el resto del árbol.

Comando de pruebas final: gradlew.bat test --tests '*WorkSessionApiTest' --daemon --max-workers=2 con JVM propia -Xmx768m y -XX:MaxMetaspaceSize=384m. Este perfil evita la eliminación del daemon idle compatible del otro árbol; root confirmó reutilización del PID 62156. Se conserva el aviso de API deprecada de content().json en una aserción de ausencia; no es un fallo.

SHA256 del corte:
- WorkSessionController.java: 20C0CA344C54C8367451B2AA472627F22BAA2639C8270973E7BAA3CF10196EF1.
- WorkSessionApiTest.java: 384B2E5242D245A71AFA519CD36B8E110C531CFF560B37C1FD452DAB6AD4C141.

No se ejecutaron E2E, PIT, smoke ni init global en esta pista.

Árbol aislado OrganizacionWeb-session-publisher, rama autorizada codex/work-session-http, base b4f425a. Sólo controller HTTP nuevo, suite MockMvc y esta bitácora. Los cinco snapshots de interfaces/excepciones son dependencias de compilación ajenas: no editarlos ni entregarlos. Sin PG, core, publicador, wiring o Git. Ponytail full y Caveman lite.

Baseline reutilizado por autorización root; no init completo. Cada caso se añade y ejecuta antes del siguiente. Comando focal: `.\gradlew.bat test --tests '*WorkSessionApiTest' --daemon --max-workers=2` desde backend.

La suite usa WebMvcTest con SecurityConfiguration real y mocks de los puertos. Acredita adaptación HTTP/seguridad y ausencia de invocación al puerto en entradas rechazadas, no commit/rollback PostgreSQL.

1. POST nominal @s1: RED de compilación eb2d5e por controller ausente; mínimo endpoint. GREEN e13331, 1/1. Siete campos, Location, no-store y contexto del principal.

2. Replay200: RED eb3bc4, GREEN f465dd (2).
3. Query antes de JSON: RED6343f8. Incidente de fixture c20ec2: csrf() añadía parámetro_form _csrf; evidenciaee3079. Corregido a csrf().asHeader(), igual que el cliente real. GREEN3f7931 (3).
4. Key ausente antes de JSON: REDf39420; headers completos, REQUIRED y validación UUID después de IDs.

4. GREENcb2954 (4).
5. Query ante body vacío: REDd574c4; RequestBody opcional para preservar orden propio, GREEN413a2a (5).
6. Body vacío válido resto: REDc094ee; JsonParseException tratada por advice compartido, GREENcf65c8 (6).
7. JSON duplicado ante replay: RED85fee6; lector estricto heredado para duplicados/trailingtokens.

7. GREEN5b408e (7).
8. Raíz array: REDc1353a, GREEN4925a6 (8).
9. Extras ordenlexical antes de REQUIRED: RED394699, GREEN02e188 (9).
10. Duración ausente: RED682f2a; guarda REQUIRED para ausencia/null.

10. GREEN69721c (10).
11. Tipo string sincoerción: RED95ed8f, GREEN6f0adf (11). Frontera segura cedida a root para integrar hotfix8f2cd75 de fixtures heredados; no se toca WIP HTTP ni snapshot5.

12. Cero antes de puerto: RED13f4d3, GREEN14a889 (12); rango1–1440 con canConvertToInt.
13. GET active nominal: RED288166; envoltorio de un campo y principal al puerto real de lectura (mock en esta suite).

13. GREENbc7f42 (13).
14. GET ID nominal: RED798326, GREEN6f6a74 (14); UUID y owner al puerto, sinLocation.
15. GET key nominal: REDd06423; ruta literal y key UUID independiente de contexto.

15. GREEN9e5443 (15).
16. Query GET ID antes de UUID: REDaadd62, GREEN7f93bd (16).
17. Query repetida GET active: REDcabc1e. Rootdiagnosticó daemonsidlecompartidos; perfilJVMlocal768MiB/384MiB mantiene aislamiento sin modificarconfig.

17. GREENf9377f (17).
18. Query GET key antesUUID: RED07a16a, GREENfd6450 (18).
19. Sesión ausente: REDb68890, problema404 cerrado específico; advice local no cambia otros títulos.

19. GREEN6fbe61 (19).
20. Activa409 cinco campos: RED7d7989, GREENfbfdb1 (20).
21. Idempotencia409 cerrado: REDce1f19, GREENbf2daa (21).
22. Error temporal409 título normado: RED22476d; handlerlocal sin fieldErrors.

22. GREEN27da76 (22).
23. Proyecto completado: REDc9d7b1 por título heredado de tareas; handlerlocal para inicio, GREENd39031 (23).
24. Tarea completada: RED9871b9 por título de planificación; handlerlocal, sin tocar ApiErrors.

## Casos adicionales individuales sobre comportamiento heredado
- s6_durationAboveMaximumIsRejected: inicialmente GREEN 8dd64f, sin cambio de producción.
- s6_largeIntegerCannotWrapIntoValidMinutes: inicialmente GREEN e85c3b, sin cambio de producción.
- s7_trailingJsonIsMalformed: inicialmente GREEN 8713c0, sin cambio de producción.
- s7_truncatedJsonIsMalformed: inicialmente GREEN 569007, sin cambio de producción.
- s9_repeatedKeyRejectedBeforeBody: inicialmente GREEN 3486ec, sin cambio de producción.
- s9_badKeyRejectedBeforeBody: inicialmente GREEN ba800e, sin cambio de producción.
- s9_queryPrecedesInvalidPathAndMissingKey: inicialmente GREEN 6c6b51, sin cambio de producción.
- s9_pathIdPrecedesMissingKey: inicialmente GREEN c79270, sin cambio de producción.
- s23_confirmedAbsenceUsesExplicitNull: inicialmente GREEN 3db561, sin cambio de producción.
- s22_byRequestUsesSameClosedMissingProblem: inicialmente GREEN dae182, sin cambio de producción.
- s10_contextNotFoundDoesNotExposeSession: inicialmente GREEN d942ba, sin cambio de producción.

- s6_fractionalDurationIsInvalidType: inicialmente GREEN 8ee79e, sin cambio de producción.

- s6_booleanDurationIsInvalidType: inicialmente GREEN b7c5d5, sin cambio de producción.

- s6_nullDurationIsRequired: inicialmente GREEN e602ee, sin cambio de producción.

- s24_activeStorageFailureIsNotAbsence: inicialmente GREEN 2f6462, sin cambios de producción.
- s24_detailStorageFailureIsNotMissing: inicialmente GREEN f5d922, sin cambios de producción.
- s24_keyStorageFailureIsNotMissing: inicialmente GREEN 217e50, sin cambios de producción.
- s20_writeStorageFailureIsClosed: inicialmente GREEN 683e2c, sin cambios de producción.
- s8_anonymousReadCannotReachValidation: inicialmente GREEN 4c737c, sin cambios de producción.
- s8_missingCsrfPrecedesInvalidBody: inicialmente GREEN 8d97a0, sin cambios de producción.
- s8_untrustedOriginPrecedesInvalidBody: inicialmente GREEN 661ac3, sin cambios de producción.
- s6_oneMinuteReachesUseCase: inicialmente GREEN 47b4e1, sin cambios de producción.
- s6_maximumMinutesReachUseCase: inicialmente GREEN c844af, sin cambios de producción.
- s8_mediaTypePrecedesQueryValidation: inicialmente GREEN 0b204b, sin cambios de producción.
- s9_taskIdPrecedesMissingKey: inicialmente GREEN 67f75e, sin cambios de producción.
