# Publicador outbox — trazabilidad de verificación

Contrato aprobado: `features/publish_outbox.feature`, 23 escenarios / 36 casos. Implementación completada: verificación local, juez APPROVED y CI remoto SUCCESS. Fuentes y tests de producción conservan el corte `create_project`.

| Escenario | Evidencia ejecutable |
| --- | --- |
| s1 original, metadatos y commit | `PublishOutboxTest.s1_*`, `OutboxWorkTest` publicación confirmada, `RabbitBrokerPublisherTest.s1_confirmsPersistentOriginalJsonAndMetadataOnRealBroker`, `scripts/publisher-smoke.mjs` etapa 1 |
| s2 creación sin commit | `OutboxWorkTest` transacción real no confirmada, otro hilo JDBC no puede reclamar |
| s3 vacío | `OutboxWorkTest.s3_emptyOutboxDoesNotSendOrCreateRecords` |
| s4 pendiente mientras espera | `OutboxWorkTest` concurrencia con latch y consulta desde segunda conexión |
| s5 cuatro fallos | `PublishOutboxTest.s5_preservesFailedAttemptWithStableCode` (4 casos), `OutboxWorkTest.s5_failedPublishPersistsRetryAndExcludesSameCycle`, `RabbitBrokerFailuresTest` NACK/timeout/desconexión/transportes acotados |
| s6 devolución antes de ACK | `RabbitBrokerPublisherTest.s6_realMandatoryReturnWinsOverPositiveConfirm` captura ACK verdadero y devolución real; resultado UNROUTABLE |
| s7 cinco intervalos | `PublishOutboxTest.s7_boundsExponentialRetryWithoutOverflow` (5 casos, incluido intento 101) |
| s8 frontera temporal | `OutboxWorkTest.s8_onlyClaimsDueEventsIncludingExactBoundary` (2 casos) |
| s9 recuperación | `scripts/publisher-smoke.mjs` parada/arranque real Rabbit, mismo eventId y payload |
| s10 publicado no seleccionado | `OutboxWorkTest.s10_publishedRecordAndTimestampRemainUnchangedOnLaterCycle` |
| s11 caída antes/después de aceptar | `OutboxRecoveryTest.s11_realProcessDeathReleasesClaimAndRetriesOriginalIdentity` (BEFORE/AFTER) / `PublisherCrashProcess`: proceso OS propio matado con claim JDBC abierto; cero/una copia previa comprobada en Rabbit real y una/dos copias originales después del reintento |
| s12 rollback después de aceptar | `OutboxWorkTest.s12_resultWriteFailureRollsBackWithoutPublishedAudit`; `OutboxRecoveryTest.s12_realAcceptanceSurvivesDatabaseRollbackAndRetryDuplicatesIdentity`: Rabbit acepta realmente, trigger PostgreSQL revierte UPDATE, fila intacta y segunda copia de identidad estable tras retry |
| s13 réplicas | `OutboxWorkTest` dos transacciones reales, SKIP LOCKED, fila distinta publicada, primera reclamación intacta |
| s14 persistencia broker | `scripts/publisher-smoke.mjs` reinicia Rabbit con volumen, backend detenido; mensajes y topología originales |
| s15 inválidos aislados | `PublishOutboxTest.s15_*`, `OutboxWorkTest` JSONB null/array/string/number, blocked conserva intentos previos y siguiente válido progresa |
| s16 API con broker caído | `scripts/publisher-smoke.mjs` HTTP 201 con plazo de 4.5 s, proyecto+outbox confirmados, publicación posterior |
| s17 máximo 20/orden | `PublishOutboxTest.s17_limitsCycleToTwentyDistinctRecords`, `OutboxWorkTest.s17_ordersAvailableEventsAndLeavesTwentyFirstPending` |
| s18 PostgreSQL indisponible | `PublishOutboxTest.s18_storageFailureNeverSendsAndReportsWorkerError`, `OutboxWorkTest.s18_unavailablePostgresNeverInvokesBroker`, `PublisherConfigurationTest.s18_storageFailureSchedulesNextCycleAtLeastOneSecondLater` |
| s19 deshabilitado | `PublisherConfigurationTest.s19_disabledPublisherHasNoWorkerOrBrokerBeans`, ocho E2E base con publisher explícitamente false y fila pending conservada tras reinicio |
| s20 topología compatible | `scripts/publisher-smoke.mjs` reconexión conserva mensajes previos y publica nuevos, verifica exchange/binding/quorum |
| s21 incompatibilidad | `RabbitBrokerPublisherTest.s21_incompatibleQueueIsPreservedAndReported` broker real, `PublishOutboxTest.s21_topologyMismatchAbortsClaimWithoutEventResult` worker_error y sin resultado de evento |
| s22 configuración incompleta | `PublisherConfigurationTest.s22_*`, incluidos valores vacíos y puerto no numérico; worker inactivo, contexto viable |
| s23 privacidad | `PublicationAuditTest` tres outcomes y worker_error, `RabbitBrokerFailuresTest.s23_clientExceptionDoesNotExposeSensitiveDetails` |

## Evidencia y alcance

- Historia RED/GREEN de núcleo, JDBC, configuración y auditoría: `progress/tdd_publish_outbox_backend.md`.
- Adaptador Rabbit y cuatro mutantes semánticos adicionales: `progress/tdd_publish_outbox_broker.md`.
- Compose, runner y revisión independiente: `progress/tdd_publish_outbox_integration.md`, `progress/judge_publish_outbox_tooling.md`.
- Smoke real completo verde: `progress/tdd_publish_outbox_smoke.md`.
- PIT dominio/aplicación: 90/90 mutantes eliminados, 105/105 líneas cubiertas, ningún superviviente ni NO_COVERAGE. `FRECORD` desactivado; solo equals/hashCode/toString generados se excluyen, igual que feature 1. Adaptadores fuera del alcance PIT: tests de infraestructura y mutantes semánticos Rabbit aportan evidencia separada.
- Recuperación OS y rollback con broker real: tres casos verdes, exit 0 en 35 s, registrados en `progress/tdd_publish_outbox_recovery.md`; juez inspeccionó los fixtures y las aserciones. Verificación final local del coordinador 6887 completada con exit 0 y dictamen del juez APPROVED. No se ha desplegado esta feature en servidor ni se afirma entrega exactamente una vez; un fallo entre aceptación Rabbit y commit PostgreSQL puede producir duplicados con la misma identidad.
- Verificación local final del coordinador: lint, 147 tests backend, 38 tests frontend, PIT 90/90 y Stryker 143/148 (96,62 %) verdes. E2E 49506: ocho pruebas base y las tres etapas de smoke del publicador verdes; builds correctos. Los cinco supervivientes Stryker pertenecen al baseline anterior, sin cambios de frontend.
- Código publicado en commit `1a3737758c655462fc3814f6af8d0f87138eb1a8`. Application CI run `33993262637` terminó SUCCESS, incluidos verify, build, E2E y publisher smoke. Feature 2 cerrada como done tras confirmación del coordinador.
