# Review independiente — read_projects, backend

**Veredicto de código y cobertura backend: APPROVED.** Revisor: integration_craftsman, no autor de producción backend ni de sus tests unitarios/HTTP. Este informe no sustituye la revisión de frontend ni la revisión independiente de mis E2E, que corresponde al coordinador.

## Fuentes y alcance revisados

Contrato read_projects, project-spec, arquitectura, workflow, TDD, convenciones y CHECKPOINTS; ReadProjects/puertos, ProjectPage/ProjectPosition/ProjectSummary, ProjectReadController, PostgresProjectQueries, wiring, ApiErrors e índice V3. Se leyeron ReadProjectsApiTest y ReadProjectsTest, junto con la bitácora de ciclos del autor.

## Cobertura backend

| Escenario | Evidencia concreta |
| --- | --- |
| s1 | s1_readsOnlyOwnerAndOnlySummaryFields; s1_s5_lastPagePreservesDisplayValuesAndHasNoContinuation |
| s2 | s2_emptyListIsPrivateAndHasNoCursor; s2_emptyOwnCollectionHasNoContinuation |
| s3 | s3_unauthenticatedReadsArePrivate, ambas rutas |
| s4 | s4_ordersAndReturnsTwentyWithCursor; s4_pageLimitAndCursorUseLastVisibleItem |
| s5, s6 | s5_s6_continuesAfterCursorWithoutRepeatingOrInsertingNewerRows |
| s7 | s7_invalidPaginationReturnsFieldError; s7_rejectsCursorDateOutsidePostgresStorageRange; s7_acceptsFinitePostgresCursorBoundaries |
| s8 | s8_foreignCursorOnlyPositionsOwnCollection |
| s9 | s9_detailPreservesOriginalOwnedValues; s9_detailReturnsOriginalOwnedProject |
| s10 | s10_foreignAndMissingDetailsAreIndistinguishable; s10_missingOwnedDetailHasUniformDomainError |
| s11 | s11_invalidDetailIdentifierHasValidationError |
| s12 | s12_storageReadFailureIsUnavailableNotEmpty, fallos SQL reales en ambas rutas |
| s13 | s13_readsLeaveProjectAndOutboxRowsUnchanged; consultas no tienen dependencia del publicador |
| s28 | s28_unexpectedReadFailureHasOnlySafeProblem, ambas rutas |
| s29 HTTP | Cabecera no-store comprobada en lista/detalle y errores; almacenamiento cliente revisado fuera de esta frontera |

s14–s27, s30–s32 y parte cliente de s29 corresponden a frontend/navegador. No se declaran cubiertos por backend.

## Hallazgos

Sin bloqueos de corrección o privacidad en la fuente final. Cada consulta SQL vincula ownerId del Principal mediante parámetros. El cursor posiciona dentro de esa colección; no determina identidad ni autorización. Orden created_at DESC/id DESC y comparación estricta por tupla evitan repetición ante nuevas inserciones; la aplicación solicita 21 filas y publica 20 con cursor del último visible. ProjectPage copia la colección para impedir mutación posterior.

La decodificación exige base64url canónico sin padding, dos campos JSON de tipo correcto, sin claves repetidas, instante UTC a precisión microsegundo dentro del rango finito PostgreSQL y UUID completo. Las consultas inválidas se rechazan antes de JDBC. UUID ajeno/inexistente comparte 404 público; errores SQL se traducen a 503 y errores inesperados a 500 seguro. No se introducen endpoints de escritura ni llamadas a broker en el camino de lectura.

El índice V3 es aditivo y corresponde al filtro/orden real. No se acopla el contrato a un plan EXPLAIN determinado. Dominio/aplicación permanecen sin frameworks y el acceso JDBC/serialización/HTTP queda en adaptadores.

## TDD y verificación

La bitácora distingue rojos funcionales de regresiones ya verdes y correcciones de infraestructura; no se observa producción de feature4 mezclada. Se verificaron directamente los XML: ReadProjectsApiTest 36 casos, cero fallos/errores; ReadProjectsTest 7 casos, cero fallos. PIT contiene 103 registros, todos KILLED, sin atribuir ese porcentaje a HTTP/JDBC (su alcance es dominio/aplicación). Los adaptadores disponen de pruebas PostgreSQL reales.

Init final y cierre global los coordina la raíz; este informe aprueba la frontera backend sobre la fuente congelada y evidencia indicada. Ninguna aprobación de despliegue ni cambio de feature_list/current se realiza aquí. C1/C2/C5 globales quedan ligados al cierre del coordinador; C3 arquitectura y C4 evidencia real conformes; C6 contrato y trazabilidad de esta frontera conformes; C7 mutación de dominio/aplicación verificada directamente (103/103).
