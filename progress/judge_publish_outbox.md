# publish_outbox — revisión independiente

**Veredicto: APPROVED.** Revisión de producción y tests backend/broker del corte aprobado publish_outbox. El juez no ha escrito esa producción. El coordinador revisó por separado el smoke de stack en progress/judge_publish_outbox_tooling.md, cuya autoría pertenece a este agente; no se autorrevisa esa parte. La matriz UX no aplica: no cambia la interfaz. Este dictamen permite el cierre coordinado; no afirma todavía commit, push, CI remoto ni despliegue.

## Cobertura de escenarios

Se inspeccionaron los 23 escenarios/36 casos y los tests concretos del mapa canónico progress/tdd_publish_outbox.md, incluida cada fila de Examples.

| Escenario | Verificación inspeccionada |
| --- | --- |
| @s1 [x] | PublishOutboxTest.s1_recordsPublishedOnlyAfterAcceptedOriginalMessage; OutboxWorkTest.s1_migrationAndConfirmedPublicationPreserveOriginalRecord; RabbitBrokerPublisherTest.s1_confirmsPersistentOriginalJsonAndMetadataOnRealBroker |
| @s2 [x] | OutboxWorkTest.s2_uncommittedCreationIsInvisibleToPublisher |
| @s3 [x] | OutboxWorkTest.s3_emptyOutboxDoesNotSendOrCreateRecords |
| @s4 [x] | OutboxWorkTest.s4_s13_otherReplicaSkipsClaimedRowWhileConfirmationPending |
| @s5 [x] | PublishOutboxTest.s5_preservesFailedAttemptWithStableCode (4 resultados), s5_retryDelayUsesOneCompletionInstant, s5_neverRetriesSameEventWithinOneLongCycle; JDBC retry y RabbitBrokerFailuresTest |
| @s6 [x] | RabbitBrokerPublisherTest.s6_realMandatoryReturnWinsOverPositiveConfirm, con booleano ACK real comprobado; retorno más timeout cubierto aparte |
| @s7 [x] | PublishOutboxTest.s7_boundsExponentialRetryWithoutOverflow (5 filas) |
| @s8 [x] | OutboxWorkTest.s8_onlyClaimsDueEventsIncludingExactBoundary (2 filas) |
| @s9 [x] | Smoke etapa recuperación; revisión independiente del coordinador |
| @s10 [x] | OutboxWorkTest.s10_publishedRecordAndTimestampRemainUnchangedOnLaterCycle |
| @s11 [x] | OutboxRecoveryTest.s11_realProcessDeathReleasesClaimAndRetriesOriginalIdentity (BEFORE/AFTER) con PublisherCrashProcess y muerte OS real |
| @s12 [x] | OutboxRecoveryTest.s12_realAcceptanceSurvivesDatabaseRollbackAndRetryDuplicatesIdentity, complementado por JDBC rollback/audit |
| @s13 [x] | OutboxWorkTest.s4_s13_otherReplicaSkipsClaimedRowWhileConfirmationPending: dos conexiones y reclamaciones reales |
| @s14 [x] | Smoke reinicio con backend detenido; revisión independiente del coordinador |
| @s15 [x] | PublishOutboxTest.s15_blocksUnsupportedWithoutSendingAndContinues, s15_blocksIncompatiblePayloadWithoutSending, s15_blocksValidTimestampThatDiffersFromStoredColumn; OutboxWorkTest.s15_nonObjectPayloadIsBlockedWithoutStoppingValidWork |
| @s16 [x] | Smoke POST con broker detenido, respuesta acotada y evento conservado; revisión independiente del coordinador |
| @s17 [x] | PublishOutboxTest.s17_limitsCycleToTwentyDistinctRecords; OutboxWorkTest.s17_ordersAvailableEventsAndLeavesTwentyFirstPending |
| @s18 [x] | PublishOutboxTest.s18_storageFailureNeverSendsAndReportsWorkerError; OutboxWorkTest.s18_unavailablePostgresNeverInvokesBroker; PublisherConfigurationTest.s18_storageFailureSchedulesNextCycleAtLeastOneSecondLater |
| @s19 [x] | PublisherConfigurationTest.s19_disabledPublisherHasNoWorkerOrBrokerBeans; regresión E2E base con publisher false |
| @s20 [x] | Smoke reconexión conserva mensaje previo y permite publicación posterior; revisión independiente del coordinador |
| @s21 [x] | RabbitBrokerPublisherTest.s21_incompatibleQueueIsPreservedAndReported; PublishOutboxTest.s21_topologyMismatchAbortsClaimWithoutEventResult |
| @s22 [x] | PublisherConfigurationTest.s22_enabledMissingSecretIsReportedWithoutClaimingOrConnecting y s22_allInvalidSettingsStayInactive |
| @s23 [x] | PublicationAuditTest.s23_recordsOnlySafeOutcomeMetadata (3 outcomes); RabbitBrokerFailuresTest.s23_clientExceptionDoesNotExposeSensitiveDetails |

## Disciplina y calidad

Evidencia RED→GREEN incremental en bitácoras backend y broker; correcciones de fixture/readiness se distinguen de fallos de producción. Las pruebas OS y smoke adicionales pasaron sobre implementación ya guiada por tests: no se inventa un rojo funcional. No se encontró producción ajena al contrato ni consumidores/conectores adelantados. No hay cambios frontend.

Dominio y aplicación dependen solo de Java y puertos internos; JDBC, Jackson, Spring y Rabbit quedan en adaptadores, comprobado también por ArchUnit. V2 añade columnas sin reemplazar V1 ni borrar eventos. El payload bruto almacenado y su mapa validado provienen del mismo JSONB; siete campos exactos, tipos, identidad, fecha y nombre se validan antes de enviar. La transacción mantiene claim hasta confirmar resultado; audit published ocurre después de commit. Los fallos de commit conservan original y permiten duplicados de identidad estable. No se promete entrega exactamente una vez ni alta disponibilidad con un solo nodo.

Hallazgo corregido durante review: la prueba real UNROUTABLE no demostraba por sí sola ACK positivo porque también podía resolver desde timeout; ahora captura y exige true del waitForConfirms real. No quedan cambios de producción requeridos.

## Checkpoints y evidencia final

- C1 [x]: init independiente43031 exit0; Spotless y lint frontend verdes.
- C2 [x]: una única feature in_progress, contrato aprobado y estado coherente. Cierre lifecycle corresponde al coordinador después de este dictamen.
- C3 [x]: fronteras hexagonales, dependencias justificadas y sin hooks de fallo en producción.
- C4 [x]: informes XML del build normal inspeccionados:147 tests,12 suites,0 failures/errors; incluyen3 recuperaciones OS/Rabbit/PG. Init reutilizó backend UP-TO-DATE de la ejecución global74662 y reejecutó38 tests frontend verdes; no se presenta como segunda ejecución backend desde cero.
- C5 [x]: archivos nuevos corresponden a código/tests/documentación del corte; scratch y outputs ignorados, no temporales sospechosos para commit. Histórico y cambio a done se ejecutan en el cierre coordinado, no se anticipan aquí.
- C6 [x]:23 escenarios/36 casos cubiertos según tabla y contrato aprobado conservado.
- C7 [x]: XML PIT inspeccionado directamente:90/90 KILLED, ningún superviviente ni NO_COVERAGE. Scope exacto com.apptolast.organization.domain.* y com.apptolast.organization.application.*; incluye36 mutantes previos y54 nuevos (PublishOutbox18, OutboxMessage30, PublicationAttempt6). FRECORD desactivado para incluir constructores escritos; solo equals/hashCode/toString generados excluidos. Adaptadores no forman parte de ese porcentaje; cuatro mutantes semánticos Rabbit eliminados son evidencia adicional separada, documentada sin pretender cobertura exhaustiva. Informe canónico progress/mutation_publish_outbox.md.

Cambios requeridos: ninguno. Al emitir el dictamen están en curso la verificación final del coordinador6887 y la regresión E2E+smoke49506. Sus resultados, cierre de metadata y CI sobre el commit siguen su puerta independiente; no se anticipan como verdes aquí.

## Preflight técnico: RabbitMQ Java 5.25.0

- El [protocolo](https://www.rabbitmq.com/docs/confirms#when-publishes-are-confirmed) garantiza basic.return antes del ACK/NACK para mandatory sin ruta. [ChannelN](https://github.com/rabbitmq/rabbitmq-java-client/blob/v5.25.0/src/main/java/com/rabbitmq/client/impl/ChannelN.java) llama al listener sin delegarlo; después procesa confirm y notifica a waitForConfirms. Registrar la devolución inline mediante AtomicBoolean por intento; nunca delegar esa escritura a un executor. Devolución prevalece sobre ACK.
- Canal/conexión nuevos por intento aíslan confirms tardíos. Un timeout de waitForConfirms no permite reutilizar sin más el canal pendiente. waitForConfirmsOrDie añade cierre graceful al timeout; no confundir el plazo de confirmación con la duración total del intento.
- [ConnectionFactory](https://github.com/rabbitmq/rabbitmq-java-client/blob/v5.25.0/src/main/java/com/rabbitmq/client/ConnectionFactory.java): explicitar límites positivos de conexión, handshake y RPC; desactivar recuperación automática si la outbox controla los reintentos. Los defaults no constituyen un presupuesto adecuado para mantener una reclamación SQL.
- NIO ofrece escritura mediante cola: [BlockingQueueNioQueue](https://github.com/rabbitmq/rabbitmq-java-client/blob/v5.25.0/src/main/java/com/rabbitmq/client/impl/nio/BlockingQueueNioQueue.java) usa offer con timeout; [SocketChannelFrameHandlerState](https://github.com/rabbitmq/rabbitmq-java-client/blob/v5.25.0/src/main/java/com/rabbitmq/client/impl/nio/SocketChannelFrameHandlerState.java) falla con IOException si no puede encolar. useNio + NioParams.setWriteEnqueuingTimeoutInMs positivo evita depender de escrituras socket bloqueantes sin plazo.
- [AMQConnection](https://github.com/rabbitmq/rabbitmq-java-client/blob/v5.25.0/src/main/java/com/rabbitmq/client/impl/AMQConnection.java): close() usa espera indefinida por defecto. Preferir cleanup explícito abort(timeout positivo), con transporte de escritura acotado. Future.cancel no demuestra que una escritura bloqueante haya terminado.
- El contrato exige cinco segundos para confirmar, además de fases finitas de preparación y limpieza, no cinco segundos totales. El límite NIO es por frame: no garantiza duración total constante para cualquier payload. ownerId procede de configuración y no tiene límite contractual explícito; no inferir un máximo absoluto del límite del nombre ni inventar otra restricción de usuario.

## Evidencia a revisar al congelar

Considerados durante preflight: devolución real seguida de ACK real; confirm retenido y aislamiento de ACK tardío; peer TCP sin handshake, RPC sin respuesta y cola de escritura saturada; liberación de reclamaciones/recursos. No todos esos fallos se inyectaron en transporte real. Evidencia final disponible: broker real para metadatos, mandatory return más ACK positivo y topología incompatible; socket TCP real sin handshake. NACK, timeout de confirmación y aislamiento de canales se prueban con dobles deterministas; límites RPC/NIO mediante configuración explícita y código oficial5.25.0. No se afirma prueba de proxy reteniendo confirms, ausencia real de respuesta de topología ni saturación real NIO. El coordinador acepta este alcance del corte, sin introducir otro framework de fallos.

Revisión de fuentes congeladas completada: JSON original y validación pura estricta, copia defensiva del mapa, migración V2 aditiva, transacción por claim con SKIP LOCKED, commit previo al audit published, backoff desde un único instante de finalización con precisión PostgreSQL y control de interrupción. Casos de muerte OS BEFORE/AFTER y rollback SQL posterior a aceptación Rabbit inspeccionados en OutboxRecoveryTest/PublisherCrashProcess: no usan excepciones simuladas como sustituto de muerte real ni hooks en producción. Init independiente y mutación comprobados en el dictamen anterior.
