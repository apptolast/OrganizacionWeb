# edit_project — TDD backend

Inicio tras cierre local de read_projects e init confirmado por el coordinador. Contrato aprobado por autorización global. Ponytail full y Caveman lite activos, respetando arquitectura hexagonal y TDD. No se añaden dependencias.
1. GET detalle: ROJO ETag ausente; VERDE V4 version y ProjectSnapshot obtenido en el mismo SELECT que los siete campos. Controlador devuelve cuerpo y etiqueta de ese snapshot único. Regresión de aplicación de lectura conservada.
2. Aplicación: ROJO clases de edición ausentes; VERDE representación validada y evento Updated/version incrementada. 3. Concurrencia: ROJO excepción de conflicto ausente; VERDE compara id y versión esperados antes de decidir cambio. 4. No-op: ROJO generaba versión/evento; VERDE devuelve snapshot original sin evento, después de comprobar precondición.
5. PUT: ROJO endpoint ausente; VERDE SELECT FOR UPDATE y UPDATE condicionado por owner/id/version con outbox en la misma transacción. 6. Conflicto HTTP: ROJO 500; VERDE 412 y ningún evento adicional incluso en no-op antiguo. 7. If-Match ausente: ROJO 500; VERDE 428 mediante manejo local del controlador, sin alterar creación.
8. If-Match inválido: ROJO formatos débiles/comodín/repetidos/overflow no rechazados correctamente; VERDE formato fuerte estricto, una precondición, errores 400. 9. JSON PUT: ROJO 10 casos de campos/JSON ambiguos; VERDE lectura local estricta de dos campos string, duplicados y documentos consecutivos rechazados con VALIDATION_ERROR/body. Creación conserva MALFORMED_JSON.

10. Rollback real: el esquema parametrizado ejecutó fallos PostgreSQL en UPDATE e INSERT. El trigger que suprime UPDATE reprodujo un éxito falso; exigir una fila actualizada lo corrigió. Ampliación posterior a INSERT suprimido: RED (200), guard de una fila insertada, GREEN en los cuatro casos. Se conservan proyecto, versión y outbox originales.
11. Comparación fuerte de ETag: dos casos adicionales (UUID en mayúsculas y versión 00) reprodujeron 200. Formato canónico de la etiqueta, GREEN en nueve casos de precondición.
12. ProjectUpdated: la prueba del caso de uso quedó RED por UNSUPPORTED_EVENT. Se amplió únicamente la lista de tipos permitidos; GREEN. Los tipos y versiones desconocidos conservan UNSUPPORTED_EVENT, conforme a la aclaración compatible del contrato.
13. Ruta real RabbitMQ: RED porque no existía la cola Updated. Selección cerrada de dos rutas, GREEN con confirmación y recepción del JSON original, messageId, persistencia y contentType en organization.project-updated.v1.
14. Identificadores de PUT: valores no UUID y UUID abreviado reprodujeron 500 y 404. Validación canónica del formato antes del puerto de entrada, GREEN con 400 VALIDATION_ERROR y cero escrituras.

15. Regresión observable de no-op: respuesta y fila completas, timestamps, versión y ETag intactos; ningún evento. Verde inicialmente, sin cambio artificial de producción.
16. Regresión de privacidad: recurso ajeno e inexistente con revisión 999 producen el mismo 404 antes del conflicto, sin escrituras. Verde inicialmente.
17. Regresión de frontera: credenciales ausentes/incorrectas, Origin ajeno y contenido no JSON producen 401/401/403/415, siempre no-store y cero escrituras. Verde inicialmente.
18. Regresión de campos independientes: cambiar solo nombre o solo descripción incrementa versión y genera evento; evita confundir cambios parciales con no-op. Verde inicialmente.
19. Concurrencia real: una transacción PostgreSQL mantiene bloqueada una modificación; el PUT equivalente con revisión antigua no termina mientras existe el bloqueo y responde 412 después del commit del escritor. Esperas y recursos acotados. Verde inicialmente.
20. Error interno: excepción inyectada en el adaptador devuelve 500 INTERNAL_ERROR con correlationId, sin SQL, secreto, datos ajenos ni escrituras. Verde inicialmente.
21. Snapshot de lectura: después de leer la fila, una actualización SQL cambia nombre y versión antes de construir la respuesta HTTP. El cuerpo y ETag siguen siendo los del snapshot leído; no hay segunda consulta de versión. Verde inicialmente.

Las pruebas nuevas se ejecutaron individualmente o mediante su único esquema parametrizado en cada ciclo. Las regresiones inicialmente verdes se identifican expresamente. No se cambiaron tests históricos para alterar UNSUPPORTED_EVENT. Verificación focalizada final y PIT completados: 143 casos en ocho clases verdes, 125/125 mutantes eliminados y 150/150 líneas cubiertas. Gradle 99273, salida 0. El coordinador realizará la regresión global y el cierre.

## Correspondencia con el contrato

| Escenarios | Evidencia backend |
| --- | --- |
| s1, s7, s12 | EditProjectTest.s1_changePreservesIdentityAndCreatesVersionedEvent; EditProjectsApiTest.s1_s12_putPersistsProjectVersionAndSingleEvent. Identidad, normalización Unicode, literalidad, tiempos microsegundos, versión y evento de siete campos. |
| s1 | EditProjectsApiTest.s1_detailIncludesStrongVersionTagFromSameSnapshot y s1_detailTagCannotReadLaterVersionThanItsBody. |
| s2 | EditProjectTest.s2_conflictRejectsOldVersionOrDifferentProject; EditProjectsApiTest.s2_oldVersionCannotOverwriteOrPassAsNoop y s2_noopWaitsForConcurrentWriterAndRejectsItsObsoleteTag. |
| s3 | EditProjectTest.s3_equivalentChangeKeepsOriginalSnapshotWithoutEvent; EditProjectsApiTest.s3_noopPreservesEntireStoredSnapshotAndTag. |
| s4–s6 | EditProjectsApiTest.s4_missingPreconditionNeverWritesEvenNoop, s5_invalidPreconditionNeverWrites y s6_invalidBodyNeverWrites. |
| s8 | EditProjectsApiTest.s8_foreignAndAbsentProjectsAreIndistinguishableBeforeVersionCheck. |
| s9, s11 | EditProjectsApiTest.s9_s11_boundaryRejectsUntrustedRequestsWithoutWrites. |
| s10 | EditProjectsApiTest.s10_invalidIdentityIs400WithoutWrites. |
| s13 | EditProjectsApiTest.s13_anyFailedWriteRollsBackProjectVersionAndEvent: errores y escrituras suprimidas en ambos registros, con PostgreSQL real. |
| s14 | Smoke independiente de integración aprobado: publicador activo, RabbitMQ detenido, PUT 200, evento pendiente y publicación original después de recuperar RabbitMQ. El coordinador revisó fuente y resultado. |
| s15 | PublishOutboxTest.edit_s15_publishesUpdatedWithOriginalEnvelope; RabbitBrokerPublisherTest.edit_s16_routesUpdatedToDedicatedDurableQueue (el nombre histórico del método usa s16; su contenido cubre s15). |
| s16 | Regresión completa PublishOutboxTest y RabbitBrokerPublisherTest: Created conserva ruta, los tipos/versiones desconocidos se bloquean, el JSON incompatible conserva su clasificación. |
| s17–s20, s22–s23 | Interfaz y E2E a cargo de los agentes frontend e integración; fuera de la autoría backend. |
| s21 | EditProjectsApiTest verifica no-store en éxito, no-op y errores. Almacenamiento del navegador corresponde a frontend/E2E. |
| s24 | EditProjectsApiTest.s24_internalFailureHasSafeProblemAndCorrelationId. |

No se añaden dependencias ni se modifican las garantías históricas de creación, publicación o lectura. La migración V4 es aditiva. No se ha desplegado el servidor del usuario.
Cierre local autorizado: init raíz 8183 con 240 pruebas backend y 122 frontend verdes; juez conjunto APPROVED, 18/18 E2E y smoke con broker detenido correctos. Feature 4 done; CI pendiente del commit de entrega. Ponytail full y Caveman lite permanecen activos.
