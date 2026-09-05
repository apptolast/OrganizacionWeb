# Review independiente — project_states, backend

**Veredicto de código y cobertura backend: APPROVED.** Revisor: integration_craftsman, no autor de producción ni de tests backend de esta feature. El transporte Rabbit anterior fue escrito por este agente en feature 2; aquí se revisa únicamente la ampliación de esquema/ruta realizada por backend_craftsman. Mis E2E, smoke y cambios Compose necesitan la revisión independiente del coordinador.

## Alcance revisado

Contrato project_states, propuesta y sección del spec, arquitectura/workflow/TDD/convenciones y CHECKPOINTS. ProjectStates, ChangeProjectStatus y sus puertos/registros/evento; PostgresProjectStatusEditing; cambio de validación de Project/ProjectSummary; V5; controlador compartido de edición, ApiErrors y wiring/configuración; validación de OutboxMessage y ruta Rabbit StatusChanged. Leídos los tests nuevos y la bitácora del autor.

## Escenarios backend

| Escenario | Prueba concreta |
| --- | --- |
| s1 | ChangeProjectStatusTest.s1_allowedTransitionChangesOnlyStatusAndEmitsExactEvent; ProjectStatesApiTest.s1_s11_updatesStateAndPersistsOneExactEvent; s1_s14_transitionsRemainReadableAndTextEditingPreservesState |
| s2 | s2_invalidTransitionNeverProducesChange, cinco transiciones prohibidas; s2_invalidTransitionReturnsConflictWithoutWrites |
| s3 | s3_currentStateIsNoopEvenAboveCapacity, cuatro estados; s3_sameStatePreservesDatabaseAndTag |
| s4 | s4_onlyOneConcurrentActivationCanTakeLastSlot: dos peticiones HTTP simultáneas con PostgreSQL, HTTP 200/409 y un evento; s4_s8_capacityCountsCurrentOwnerAndRejectsAtOrAboveLimit |
| s5, s6 | s5_s6_pauseReleasesOnlyOwnersCapacityForAnotherActivation |
| s7 | ProjectStateConfigurationTest.s7_capacityIsValidatedAtStartup, siete valores y caso de uso Spring con límite observable. Configuración Compose validada por integración, revisión independiente a cargo del coordinador |
| s8 | s8_lowerLimitDoesNotPauseExistingProjects; s4_s8_capacityCountsCurrentOwnerAndRejectsAtOrAboveLimit |
| s9 | s9_textAndStateShareOneRevision, ambos órdenes y no-op obsoleto; s9_staleOrForeignRevisionFailsBeforeNoop |
| s10 | s10_boundaryFailuresCannotWriteOrExposePrivateState, 19 casos de autenticación/privacidad/precondición/JSON/origen/formato |
| s11 | s1_s11_updatesStateAndPersistsOneExactEvent; smoke con worker habilitado y RabbitMQ detenido confirma HTTP 200 y evento pendiente. El smoke pertenece a integración y se revisa por el coordinador |
| s12 | s12_failedOrSuppressedWriteRollsBackAllState: cuatro triggers reales de PostgreSQL, excepciones y filas suprimidas en proyecto/outbox |
| s13 | PublishOutboxTest.states_s13_publishesExactStatusEnvelope y states_s13_blocksIncompatibleStatusPayload; RabbitBrokerPublisherTest.states_s13_routesStatusChangedToDedicatedQueue; regresión de Created/Updated. Smoke recibe el mismo evento de ocho campos tras recuperación |
| s14 backend | s1_s14_transitionsRemainReadableAndTextEditingPreservesState; ProjectTest.states_s14_acceptsAllDefinedStoredStates |
| s17 | s17_internalFailureIsSafeAndPrivate; no-store en la matriz HTTP de fronteras |

La parte visual de s14 y s15–s16 corresponde a frontend/E2E y al informe UX; no se atribuye a backend.

## Hallazgos y límites técnicos

Sin bloqueos de corrección en la fuente final. Todas las mutaciones de estado adquieren primero el bloqueo asesor de PostgreSQL y luego la fila propia; el conteo vincula ownerId procedente del Principal. La comparación de revisión precede a no-op, transición y capacidad. La misma transacción aplica versión/estado/fecha y outbox, exigiendo una fila afectada por cada escritura. Editar texto comparte el bloqueo de fila y la versión, y crear siempre produce Idea: ninguna de esas rutas aumenta el conteo activo por fuera del protocolo.

La garantía de capacidad utiliza READ_COMMITTED, valor del despliegue documentado. Bajo ese aislamiento, el conteo posterior al bloqueo observa el commit del anterior titular. El bloqueo global serializa brevemente cambios de todos los propietarios; el comentario Ponytail documenta el límite y la eventual división por propietario ante contención medida. No espera al broker y no añade tablas de bloqueo ni un contador en memoria.

Cuatro estados cerrados, siete transiciones y no-op con revisión vigente; la reapertura queda pausada. El límite sólo se comprueba al entrar en Activo. Bajar la configuración no modifica proyectos existentes. El controlador reutiliza parsing estricto y ETag, sin aceptar campos adicionales ni datos de otro propietario. Estado desconocido es validación; transición imposible entre valores válidos es conflicto.

El evento StatusChanged contiene exactamente ocho campos y omite nombre/descripción. Valida transiciones del mismo mapa de dominio y selecciona una tercera ruta cerrada; no transforma el JSON original ni crea consumidores. Created/Updated conservan sus esquemas y rutas. No se introducen dependencias nuevas, personalización arbitraria, tareas ni autenticación de feature 6.

## TDD, XML y mutación

La bitácora separa rojos funcionales de regresiones inicialmente verdes. Incluye la primera respuesta 500 de transición/límite, configuración ausente, evento no admitido, payloads inválidos y ruta Rabbit desconocida. Las regresiones de privacidad, rollback, estados almacenados y concurrencia con texto se identifican expresamente; no se observa producción ajena al contrato.

XML inspeccionado directamente: ProjectStatesApiTest 43 casos, ChangeProjectStatusTest 21, ProjectStatesTest 4 y ProjectStateConfigurationTest 7; cero fallos/errores. PIT XML contiene 163 registros, todos KILLED. Ese porcentaje corresponde al alcance dominio/aplicación y no se extiende a HTTP/JDBC/Rabbit. Los adaptadores tienen pruebas PostgreSQL/Rabbit reales.

Init raíz 51375 comunicado por el coordinador: verde, 328 pruebas backend, 171 frontend y lint. Se usa esa ejecución conjunta para el checkpoint, sin repetir Gradle desde cada agente. La decisión global, frontend y revisión de mis herramientas corresponden al coordinador; este informe aprueba sólo la frontera backend descrita.
