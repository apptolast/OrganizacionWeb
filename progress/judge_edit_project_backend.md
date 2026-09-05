# Review independiente — edit_project, backend

**Veredicto de código y cobertura backend: APPROVED.** Revisor: integration_craftsman. No autor de producción de edición ni de sus pruebas backend. El adaptador Rabbit existente se escribió en la feature 2 por este agente; para la feature 4 sólo se revisó la ampliación de rutas realizada por backend_craftsman. Los E2E y la ampliación de publisher-smoke son de este revisor y requieren revisión independiente del coordinador, sin atribuir auto-revisión independiente.

## Alcance y fuentes

Contrato edit_project, project-spec, workflow/TDD, arquitectura, convenciones y CHECKPOINTS. Fuente congelada: EditProject y sus puertos, ProjectSnapshot/ProjectRevision/ProjectChange, evento ProjectUpdated, V4, PostgresProjectEditing/Queries, controladores de lectura y edición, ApiErrors, wiring, lista cerrada de OutboxMessage y rutas de RabbitBrokerPublisher. Leídas las pruebas EditProjectTest, EditProjectsApiTest y ampliaciones de PublishOutboxTest/RabbitBrokerPublisherTest, junto con la bitácora del autor.

## Cobertura del contrato backend

| Escenario | Evidencia concreta |
| --- | --- |
| s1 | s1_changePreservesIdentityAndCreatesVersionedEvent; s1_eachEditableFieldAloneProducesChange; s1_s12_putPersistsProjectVersionAndSingleEvent; s1_detailIncludesStrongVersionTagFromSameSnapshot; s1_detailTagCannotReadLaterVersionThanItsBody |
| s2 | s2_conflictRejectsOldVersionOrDifferentProject; s2_oldVersionCannotOverwriteOrPassAsNoop; s2_noopWaitsForConcurrentWriterAndRejectsItsObsoleteTag, con bloqueo PostgreSQL real |
| s3 | s3_equivalentChangeKeepsOriginalSnapshotWithoutEvent; s3_noopPreservesEntireStoredSnapshotAndTag |
| s4 | s4_missingPreconditionNeverWritesEvenNoop |
| s5 | s5_invalidPreconditionNeverWrites, nueve variantes |
| s6 | s6_invalidBodyNeverWrites, trece variantes de validación/JSON |
| s7 | Representación Unicode/HTML literal en s1_s12 y s1_change; validación reutilizada de Project; E2E real de texto literal |
| s8 | s8_foreignAndAbsentProjectsAreIndistinguishableBeforeVersionCheck, dos casos, más inspección del único error público sin parámetros privados |
| s9, s11 | s9_s11_boundaryRejectsUntrustedRequestsWithoutWrites: credenciales ausentes/incorrectas, Origin ajeno y contenido no JSON |
| s10 | s10_invalidIdentityIs400WithoutWrites, UUID inválido y abreviado |
| s12 | s1_s12 verifica siete campos exactos del evento y persistencia atómica |
| s13 | s13_anyFailedWriteRollsBackProjectVersionAndEvent: excepciones y supresión de filas mediante triggers reales en UPDATE e INSERT |
| s14 | Ampliación de pnpm test:publisher: worker habilitado, RabbitMQ detenido, PUT 200, Updated pendiente con BROKER_UNAVAILABLE; recuperación y recepción del mismo evento. Prueba escrita por integración, revisión independiente del script a cargo del coordinador |
| s15, s16 | edit_s15_publishesUpdatedWithOriginalEnvelope; edit_s16_routesUpdatedToDedicatedDurableQueue con RabbitMQ real; pruebas históricas de Created y bloqueo de tipos/versiones no admitidos; smoke recibe JSON Updated original y metadatos persistentes |
| s21 backend | no-store en éxito y errores HTTP; persistencia del navegador corresponde a E2E/frontend |
| s24 | s24_internalFailureHasSafeProblemAndCorrelationId y snapshot SQL sin efectos |

s17–s20, s22–s23 y la parte de almacenamiento de cliente de s21 corresponden a la revisión frontend/E2E, no se declaran cubiertos por los tests backend.

## Hallazgos y diseño

Sin bloqueos de corrección observados. El propietario procede del Principal y todas las consultas condicionan propietario/id. La fila se bloquea dentro de la misma transacción que compara versión, decide no-op y escribe proyecto/outbox. La lectura de detalle obtiene cuerpo y versión en un único snapshot; el controlador no consulta de nuevo una versión posterior. El no-op también comprueba precondición bajo el bloqueo. Se exige exactamente una fila afectada tanto en proyecto como en outbox; un trigger que suprime una escritura provoca rollback y 503.

ETag fuerte y canónico, versión no negativa dentro de long, un único valor If-Match. El PUT sólo admite dos strings y rechaza JSON ambiguo mediante el lector estricto local, conservando el contrato histórico de creación. No se amplían campos mutables, estados, tareas ni permisos. Identidad, propietario, creación y estado se conservan; updatedAt/evento comparten precisión microsegundo del servidor.

El publicador sigue siendo asíncrono y admite dos rutas cerradas. No se construye una ruta arbitraria con datos externos. ProjectUpdated conserva siete campos sin descripción, JSON original, persistencia y confirmación; tipos/versiones no admitidos mantienen UNSUPPORTED_EVENT. Se reutilizan transporte y puertos existentes, sin dependencias ni capas nuevas ajenas al requisito hexagonal.

## TDD y verificación

La bitácora del autor distingue ciclos rojos funcionales y regresiones inicialmente verdes, incluyendo los falsos 200 ante UPDATE/INSERT suprimidos. No se identificó producción sin un requisito o prueba que la exija. XML inspeccionado directamente: EditProjectsApiTest, 42 casos; EditProjectTest, 6 casos; cero fallos y errores. PIT XML inspeccionado: 125 mutantes, todos KILLED. Su alcance es dominio/aplicación, no un porcentaje global de JDBC/HTTP/Rabbit.

El coordinador ejecutó init raíz 8183: verde, 240 pruebas backend, 122 frontend y lint. Se reutiliza esa ejecución conjunta para el checkpoint, sin repetir la suite local de cada agente. pnpm test:publisher terminó con exit 0 y verificó el caso nuevo de broker caído/recuperación, además de las comprobaciones existentes del script. No se repitió aquí la matriz de muerte de procesos de feature 2. El cierre global y la revisión de mis herramientas/E2E pertenecen al coordinador.
