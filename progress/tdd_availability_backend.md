# TDD backend — disponibilidad

Ponytail full y Caveman lite activos. Contrato 3f9a293 leído: 47 escenarios, 237 casos. Inicio autorizado después de init compartido 27065, EXIT 0: 798 backend, 647 frontend y lint verdes. Feature 10 es la única activa. No se repite baseline ni se ejecuta PIT antes de revisión.

Alcance: preferencia propia, catálogo backend, ETag/snapshot, siete presupuestos y transacción concurrente. Sin evento personal, reserva temporal ni modificación de proyectos, tareas, historia u outbox.

## Ciclos 1 a 8: entidad y guardado puro
1. RED de Availability ausente; GREEN de los siete presupuestos y total derivado.
2. RED porque mutar el mapa de entrada alteraba el snapshot; GREEN con copia inmutable.
3. Trece reconstrucciones inválidas fallan antes de guardas de identidad, estructura diaria, rango, versión y fechas; después pasan. Una zona histórica textual no se resuelve contra java.time al reconstruir.
4. RED de puertos/caso de uso ausentes; GREEN del primer guardado propio con UUID, versión 0 e instante truncado a microsegundos.
5. RED porque una zona histórica fuera de catálogo llegaba al puerto de escritura; GREEN de VALIDATION_ERROR/zoneId/INVALID_VALUE antes del almacenamiento, también para no-op.
6. Tres relojes (avanza, igual, retrocede) fallan porque el guardado recreaba identidad; GREEN de actualización con mismo UUID/createdAt, versión siguiente y fecha no decreciente.
7. RED de no-op que cambiaba snapshot; GREEN devuelve el mismo valor confirmado.
8. RED de conflicto inexistente; GREEN compara revisión/identidad dentro del callback transaccional antes de no-op, incluidas ausencia y UUID distintos. Suite SaveAvailabilityTest focal verde; no se han creado adaptadores ni ejecutado PIT.

## Ciclos 9 a 17: lectura y persistencia HTTP
9. RED de ReadAvailability ausente; GREEN de ausencia y zona histórica sin consultar catálogo.
10. RED de consulta de catálogo ausente; GREEN de lista exacta y ordenada.
11. RED de adaptador java.time ausente; GREEN de conjunto de JVM más UTC, sin filtrar aliases.
12. GET de preferencia falla antes de ruta/V10; GREEN de ausencia explícita, cuatro campos, ETag inicial y cero filas.
13. GET zones falla sin ruta; GREEN del catálogo HTTP exacto.
14. Primer PUT falla antes de handler/commit; GREEN para Europe/Madrid y UTC, identidad propia, siete columnas, versión 0, fechas y mismo snapshot al leer.
15. Actualización falla con primer mínimo; GREEN de parsing de revisión configurada y UPDATE con identidad/createdAt conservados y versión siguiente.
16. No-op falla ante trigger real que rechaza cualquier UPDATE; GREEN al omitir la escritura si el callback conserva el mismo snapshot.
17. Trece precondiciones inválidas fallan antes de validación canónica; GREEN con prioridad sobre JSON truncado. Seis conflictos HTTP fallan con 500; GREEN al traducirlos a AVAILABILITY_CONFLICT 412, conservando filas propias y ajenas.

## Ciclos 18 a 20: documento y presupuestos estrictos
18. Las 13 formas de JSON mal formado o clave duplicada fallan inicialmente; GREEN tras parser estricto y error MALFORMED_JSON del controller. Se ejecuta la duplicación de cada uno de los siete días.
19. Las 26 formas/propiedades de s13 producen 20 fallos; seis zonas ya se rechazaban por el catálogo del caso de uso. GREEN tras validar raíz, extras, zoneId y objeto diario en orden. La pertenencia se comprueba antes de analizar presupuestos y también en el caso de uso.
20. Los 24 casos de s14/s15 fallan inicialmente, incluidos cada día ausente/negativo y tokens 1.0, fracción, null, texto, booleano y colección. GREEN de REQUIRED, INVALID_TYPE y OUT_OF_RANGE con nombres de campo exactos, sin conversión o truncado silencioso.

21. Las cuatro claves diarias adicionales fallan inicialmente; GREEN tras cerrar el conjunto de siete días sin traducción.
22. Las tres rutas aceptaban query desconocida; RED observado en GET, catálogo y PUT. GREEN de VALIDATION_ERROR/query antes de precondición y cuerpo.

23. Los seis fallos SQL de s25 producen inicialmente 500 o falso 200. GREEN con traducción segura, guardas de filas y rollback de INSERT/UPDATE y constraint diferida que rechaza COMMIT. No se simula pérdida de ACK posterior al commit.

24. Carrera inicial: dos INSERT esperan un bloqueo de tabla real tras observar ausencia; RED por 503 del perdedor. GREEN con ON CONFLICT por propietario y clasificación de cero filas: 412 si existe ganador, 503 si un trigger suprimió INSERT sin fila. Los seis fallos SQL siguen verdes.
25. Las carreras de actualización y no-op obsoleto esperando a un escritor pasan inicialmente por el bloqueo de fila existente. Se comprueba una sola revisión ganadora y el rechazo 412 tras commit; no se modificó producción para estos casos.

26. La seguridad heredada pasa inicialmente los seis casos de sesión ausente/expirada y tres fronteras CSRF/origen. Se añade nullDay a la matriz de reconstrucción y pasa con la guarda existente.
27. Privacidad entre dos propietarios, lectura de zona histórica y rechazo de su reenvío, alias CET y los 14 límites diarios pasan con el comportamiento ya implementado. Totales extremos conservan DTO de cuatro campos sin columna derivada.
28. Las once combinaciones de precedencia pasan inicialmente; se contrastan códigos y campo determinista, incluidas negociación, query, ETag y no-op obsoleto.
29. Primer guardado, cambio y no-op conservan snapshots completos de proyectos, tareas, historia y seis filas sintéticas de outbox. Es una prueba de ausencia de efectos, no de publicación de sus payloads.
30. La revisión de dominio aceptaba estados imposibles; tres casos RED y GREEN con revisión no negativa e identidad ausente sólo para versión cero. Fixture de configuración actualizado con los tres puertos nuevos; siete casos previos conservados y verdes.

31. Los tres relojes controlados pasan también contra PostgreSQL: el valor confirmado, la columna con precisión de microsegundos y el GET coinciden. No-op y reloj regresivo conservan la fecha previa.
32. Refactor de formato/imports y comentarios de READ_COMMITTED, sin nueva capa. Suite de alcance y arquitectura: sesión 49498 EXIT 0, 194 pruebas (151 HTTP, 16 dominio, 3 revisión, 12 guardado, 3 consulta, 1 catálogo JVM, 7 configuración y 1 arquitectura). Tras reforzar los códigos exactos de s24 y snapshots de las demás tablas de s25, sus nueve casos y lint pasan en ejecución separada. No se suman como casos adicionales.

## Trazabilidad del contrato

Las pruebas citadas sin prefijo están en AvailabilityApiTest. Un GREEN inicial se identifica en los ciclos anteriores; no se presenta como RED artificial.

| Escenarios | Evidencia backend |
| --- | --- |
| s1 | s1_readsUnconfiguredWithoutCreatingPreference; ReadAvailabilityTest |
| s2 | s2_exposesExactSortedBackendCatalog; JavaTimeZoneCatalogTest; ReadAvailabilityTest |
| s3 | s3_confirmsFirstPreferenceAndSameGetSnapshot, dos zonas; SaveAvailabilityTest |
| s4 | s4_retainsCatalogAliasExactly |
| s5 | s5_acceptsBothBoundsForEveryDay, 14 filas; AvailabilityTest |
| s6 | s6_persistsFullWeekWithoutDerivedColumn, dos extremos; total en AvailabilityTest |
| s7 | s7_updatesOnlyContentAndNextRevision |
| s8 | s8_noOpDoesNotIssueAnUpdate, trigger real; SaveAvailabilityTest |
| s9 | s9_confirmedClockValuesRoundTripPostgres y matriz de tres relojes de SaveAvailabilityTest |
| s10 | s10_rejectsAmbiguousPreconditionBeforeMalformedBody, 13 filas |
| s11 | s11_returnsConflictWithoutChangingOwnOrForeignRows, seis filas HTTP; en prueba pura foreign/missingId representan la misma frontera de identidad distinta |
| s12 | s12_rejectsMalformedAndDuplicateJson, 13 filas |
| s13 | s13_rejectsShapeAndPreferenceFields, 26 filas |
| s14, s15 | s14_s15_rejectsEachMissingDayAndInvalidBudget, 24 filas |
| s16 | s16_rejectsExtraDayKeysWithoutTranslation, cuatro filas |
| s17 | s17_errorPrecedenceIsStable, 11 filas |
| s18 | s18_personalPreferenceNeverChangesProjectHistoryOrOutbox, tres operaciones y snapshots completos; las seis filas sintéticas prueban ausencia de efectos, no validez/publicación de payloads |
| s19 | s19_firstCreationRaceHasOneWinnerWithoutOverwriting, dos conexiones HTTP bloqueadas antes de INSERT, un 200 y un 412 |
| s20 | s20_concurrentUpdatesShareOneRevision, bloqueo real y dos escritores HTTP |
| s21 | s21_waitingOldNoOpMustConflictAfterWriterCommits, escritor transaccional y lector/escritor HTTP esperando |
| s22 | s22_ownerComesOnlyFromSession, GET/PUT y fila ajena intacta |
| s23 | s23_requiresSessionIncludingPersistedExpiration, seis filas; expiración persistida en Spring Session JDBC |
| s24 | s24_rejectsCsrfOrForeignOrigin, tres códigos y cero escrituras |
| s25 | s25_storageFailureNeverConfirmsOrChangesPreference, seis fallos PostgreSQL reales; constraint diferida rechaza COMMIT antes de confirmación, sin simular pérdida de ACK |
| s26 | s26_rejectsQueryBeforeMissingPrecondition, tres rutas |
| s27 | Aserciones no-store en s1/s2/s3/s10/s11/s13/s17/s23/s24/s25/s26 cubren las 15 combinaciones de ruta/estado |
| s28 | Reinicio real asignado a integración, pendiente de ejecución sobre imagen congelada |
| s29–s44 | Interfaz y cliente API asignados a frontend/integración; sin afirmar cumplimiento desde estas pruebas backend |
| s45, s46 | s45_s46_historicalZoneCanBeReadButCannotBeSaved, lectura exacta y rechazo incluso sin cambios |
| s47 | Navegación asignada a frontend/integración |

## Corte y mutación propuesta

Fuentes backend congeladas para revisión independiente; Gradle liberado. Perfil `-PmutationScope=availability` preparado, sin ejecutar. Incluye Availability/AvailabilityRevision, ReadAvailability/SaveAvailability, controller y parsing, store JDBC, catálogo nativo y ApiErrors. Las pruebas históricas de handlers se incluyen sin atribuir esos handlers a funcionalidad nueva. El perfil global incorpora también los adaptadores y pruebas nuevos; no necesita cambio del script raíz. DTO de respuesta y puertos sin lógica no se presentan como lógica cubierta. Umbral 80 conservado. No se ha modificado outbox ni agregado dependencias. Ponytail full y Caveman lite siguen activos.

## Mutación tras revisión

Tras init conjunto 8318 y aprobación independiente, PIT focal 57648 termina EXIT 0: 130/130 KILLED en XML original, sin supervivientes, falta de cobertura ni timeouts. Informe separado en mutation_availability_backend.md; no replay ni nuevo cambio de producción/tests. Gradle liberado.
