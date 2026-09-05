# read_projects — TDD backend

Autorización persistente del usuario registrada por el coordinador. Init 37769 verde antes de producción. Un solo test nuevo por ciclo; puerto de entrada y dominio/aplicación sin frameworks.

1. @s2 ROJO clases de lectura ausentes; VERDE consulta al puerto propio y página vacía sin continuación. Test ReadProjectsTest ejecutado antes/después.
2. @s4 ROJO página de 21 sin cursor; VERDE devuelve20 y posición del último visible. 3. @s9 ROJO método de detalle ausente; VERDE consulta al puerto con owner/id y devuelve original. 4. @s10 ROJO excepción de recurso ausente; VERDE ProjectNotFoundException con mensaje uniforme. Cuatro tests de aplicación verdes.
5. @s2 HTTP ROJO endpoint ausente; VERDE wiring y vacío con no-store de Spring Security. 6. @s1 ROJO colección propia vacía pese a filas; VERDE lectura JDBC filtrada por propietario y solo cinco campos públicos del resumen.
7. @s9 HTTP ROJO detalle sin endpoint; VERDE detalle SQL por owner/id y representación exacta. 8. @s10 ROJO ajeno/inexistente500; VERDE404 uniforme, sin revelar existencia, no-store también en error. Mensaje de log genérico actualizado a operación de proyecto.
9. @s4 ROJO sin orden/cursor; VERDE orden DESC por fecha/UUID, SQL LIMIT y cursor basado en último visible. 10. @s5/@s6 ROJO continuación repetía página e incluía nuevo; VERDE comparación keyset estricta, propietario siempre filtrado, siguiente página estable tras inserción más reciente.
11. @s7 ROJO 12 variantes inválidas aceptadas o500; VERDE validación de consulta/cursor con errores de campo. Sin padding ni nanos no persistibles según formato propuesto; no se usa cursor como autorización. 12. @s11 ROJO UUID malformado500 o abreviado404; VERDE400 id. 13. @s12 ROJO fallos SQL reales500; VERDE traducción503 en lista y detalle, sin falso vacío.
14. Snapshot: ROJO lista de aplicación afectada por mutación del adaptador; VERDE copia inmutable en ProjectPage. 15. Índice V3 aditivo owner_id/created_at/id: EXPLAIN puntual con 10000 propietarios pasó de Seq Scan+Sort a Index Scan con Index Cond, sin Sort. El coordinador pidió retirar esa aserción de la suite permanente para no acoplarla al planificador; se conserva como evidencia de optimización, no como requisito de contrato.
16. Regresiones posteriores de seguridad/lectura: autenticación 401 y no-store en ambas rutas, error 500 seguro con correlationId, cursor de otro propietario sin fuga y snapshots PostgreSQL de proyectos/outbox idénticos después de consultar. Pasaron inicialmente; no se inventa un rojo ni se cambia producción para estas aserciones.
17. Verificación focalizada final 63963: exit 0. Spotless aplicado, 29 casos API con PostgreSQL real, 7 casos de aplicación y 1 regla de arquitectura verdes. PIT 103/103 (13 nuevos y 90 previos), 121/121 líneas, sin supervivientes ni NO_COVERAGE. No se ejecuta verify global adicional por esta frontera; lo coordina la raíz.

## Trazabilidad backend

| Escenario | Test ejecutado |
| --- | --- |
| s1 | ReadProjectsApiTest.s1_readsOnlyOwnerAndOnlySummaryFields; ReadProjectsTest.s1_s5_lastPagePreservesDisplayValuesAndHasNoContinuation |
| s2 | ReadProjectsApiTest.s2_emptyListIsPrivateAndHasNoCursor; ReadProjectsTest.s2_emptyOwnCollectionHasNoContinuation |
| s3 | ReadProjectsApiTest.s3_unauthenticatedReadsArePrivate, lista y detalle |
| s4 | ReadProjectsApiTest.s4_ordersAndReturnsTwentyWithCursor; ReadProjectsTest.s4_pageLimitAndCursorUseLastVisibleItem |
| s5 / s6 | ReadProjectsApiTest.s5_s6_continuesAfterCursorWithoutRepeatingOrInsertingNewerRows |
| s7 | ReadProjectsApiTest.s7_invalidPaginationReturnsFieldError, 12 defectos |
| s8 | ReadProjectsApiTest.s8_foreignCursorOnlyPositionsOwnCollection |
| s9 | ReadProjectsApiTest.s9_detailPreservesOriginalOwnedValues; ReadProjectsTest.s9_detailReturnsOriginalOwnedProject |
| s10 | ReadProjectsApiTest.s10_foreignAndMissingDetailsAreIndistinguishable, dos casos; ReadProjectsTest.s10_missingOwnedDetailHasUniformDomainError |
| s11 | ReadProjectsApiTest.s11_invalidDetailIdentifierHasValidationError, dos casos |
| s12 | ReadProjectsApiTest.s12_storageReadFailureIsUnavailableNotEmpty, fallos PostgreSQL reales de ambas lecturas |
| s13 | ReadProjectsApiTest.s13_readsLeaveProjectAndOutboxRowsUnchanged |
| s28 | ReadProjectsApiTest.s28_unexpectedReadFailureHasOnlySafeProblem, ambas rutas |
| s29 (HTTP) | s2/s3/s9/s10/s12 comprueban no-store; ausencia de persistencia cliente pertenece a frontend/E2E |

Los escenarios s14–s27, s30–s32 y la parte cliente de s29 pertenecen a frontend/E2E y no se declaran cubiertos por estas pruebas backend. El índice V3 se comprobó puntualmente con EXPLAIN; la suite no depende del plan elegido por PostgreSQL. La validación del cursor aplica el formato contractual base64url sin padding, exactamente dos propiedades, UTC a precisión microsegundo y UUID; no añade identidad confiable ni permisos al cursor.

PIT conserva el alcance dominio/aplicación y exclusiones previas (equals/hashCode/toString generados; FRECORD desactivado). Los adaptadores HTTP/JDBC se comprueban por integración real y no se les atribuye cobertura de mutación del 100 %. Producción congelada para revisión; sin commits ni cambio de estado done por este agente.
18. Revisión del coordinador detectó fechas de cursor parseables fuera del almacenamiento. ROJO real: tres extremos devolvían respuestas distintas de 400. VERDE: se valida el rango finito exacto de PostgreSQL antes de crear parámetros JDBC; no se restringe arbitrariamente a cuatro dígitos de año. Fuente primaria: [PostgreSQL 17 timestamp.h](https://github.com/postgres/postgres/blob/REL_17_STABLE/src/include/datatype/timestamp.h), mínimo inclusivo -4713-11-24T00:00:00Z y máximo exclusivo +294277-01-01T00:00:00Z. Dos fronteras válidas pasan HTTP 200 contra PostgreSQL real. Se añaden también los dos extremos inmediatamente fuera del rango. Cambio solo en adaptador HTTP, fuera del alcance PIT ya verificado; no se repite mutación innecesariamente.
