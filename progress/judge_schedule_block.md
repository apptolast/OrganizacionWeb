# Review — feature 11 schedule_block

**Veredicto: APPROVED para diseño, cobertura y verificación previa a mutación.** Fecha: 6 de septiembre de 2026. Juez: coordinador, sin autoría de producción ni pruebas. La feature sigue in_progress; este dictamen habilita PIT/Stryker, no el cierre.

## Cobertura de escenarios

El índice [tdd_schedule_block.md](tdd_schedule_block.md) reúne los ciclos y mapas concretos de autores. Se revisaron fuentes, pruebas y revisiones parciales, incluida la cobertura adicional posterior a los hallazgos iniciales.

| Escenario | Evidencia revisada |
| --- | --- |
| @s1 | ScheduleBlockApiTest.s1_previewReturnsExactSnapshotWithoutWriting |
| @s2 | ScheduleBlockApiTest.s2_createAtomic; SQL y evento; primer E2E real |
| @s3 | BlockRequestTest, normalización y límite de 500 puntos suplementarios |
| @s4 | s4_invalidStructure, s4_creationStructure, s4_rootObject, s4_creationRootMustBeObject |
| @s5 | s5_strictLocal |
| @s6 | ResolvedBlockTimeTest, duración exacta y límites |
| @s7 | PlanBlockTest.s7_rechecksServerClockAfterPreviewAndAcceptsExactEquality |
| @s8 | ResolvedBlockTimeTest, s8_closedOffsetOptions y E2E Madrid |
| @s9 | Resolución con catálogo controlado y s9_canonicalOffset |
| @s10 | Límites UTC/locales de ResolvedBlockTimeTest y restricciones SQL |
| @s11 | BlockBudgetTest: anclas consecutivas, Apia, Casey, medianoche y DST |
| @s12 | ScheduleBlockPersistenceTest.s12_previewCountsOwnReservationsAcrossCompletedProjectsOnly |
| @s13 | PlanBlockTest contigüidad y ScheduleBlockPersistenceTest.s13_s15_creationRechecksAllOwnReservationsBeforeWriting |
| @s14 | PlanBlockTest.s13_s14_rejectsOverlapUsingFirstStartThenUuid |
| @s15 | s15_budgetErrorHasCurrentDays; PlanBlockTest y E2E consentimiento |
| @s16 | Carreras de presupuesto en persistencia y E2E consumo posterior al preview |
| @s17 | s17_s18_businessPrecedence |
| @s18 | s18_headers y s19_emptyBodyPreservesPrecedence |
| @s19 | Seguridad, propiedad, query/IDs/headers antes del cuerpo en ScheduleBlockApiTest |
| @s20 | no-store en respuestas correctas y errores de las cinco rutas HTTP |
| @s21 | s21_s22_replayConfirmedIntention |
| @s22 | s22_keyBindsExactIntention y s22_sameKeyIsScopedToTask |
| @s23 | s23_replayStillRequiresSecurityAndStructure |
| @s24 | s24_s26_readSavedBlock y recuperación E2E por key |
| @s25 | s25_s26_listUsesStableCursor y s25_createdAtPrecedesUuidOrder |
| @s26 | s24_s26_readSavedBlock y s26_readStorageFailureIsSafe503 |
| @s27 | s19_s27_strictReadQueryAndCursor |
| @s28 | BlockBudgetTest.s11_s28_projectsMidnightAndHistoricalReservationsIntoCurrentBudgetZone |
| @s29 | Persistencia s29_sameKeyWaitingForAvailabilityReplaysAfterWinningCommit |
| @s30 | Persistencia s30_serializesConflictingReservationsAcrossProjects |
| @s31 | Persistencia s31_realStateAndPreferenceWritersCoordinateInBothCommitOrders |
| @s32 | Persistencia s32_otherOwnerCommitsWhileFirstOwnerStillHoldsAvailability |
| @s33 | Persistencia: ausencia de preferencia bajo lock y snapshot contra writers reales |
| @s34 | Persistencia: cinco triggers de fallo/supresión/COMMIT y fallos de lectura |
| @s35 | E2E lost acknowledgement recovers persisted block after real backend restart |
| @s36 | s2_createAtomic y PublishOutboxTest: payload cerrado BlockPlanned.v1 |
| @s37 | PublishOutboxTest, RabbitBrokerPublisherTest y OutboxRecoveryTest; rutas y reintentos reales |
| @s38 | UI abre editor nativo con zona guardada y recupera configuración fallida |
| @s39 | UI revisa intención, distingue zonas y confirma presupuesto; E2E real |
| @s40 | UI preview fallido conserva borrador; variantes estrictas del cliente API |
| @s41 | UI ocurrencias por extremo y E2E Madrid |
| @s42 | UI edición invalida revisión/respuesta pendiente; E2E renovación de consentimiento |
| @s43 | UI aceptación explícita no preseleccionada; E2E presupuesto cero y consumo concurrente |
| @s44 | UI una intención retenida y doble envío bloqueado; E2E key/revisión/201 |
| @s45 | Cliente API y UI incertidumbre; E2E abort después del commit real |
| @s46 | UI confirmación coherente y recuperación E2E |
| @s47 | UI 404 por key conserva intención bloqueada |
| @s48 | UI reenvía exactamente el mismo bloque de forma manual |
| @s49 | UI rechazos tipados, errores por campo y consulta explícita de conflicto; E2E solape/presupuesto |
| @s50 | UI IDEMPOTENCY_CONFLICT conserva key e intención y consulta su resultado |
| @s51 | UI recuperación CSRF conserva petición y exige reenvío manual |
| @s52 | UI retiro del contexto privado ante pérdida de acceso, incluida SessionGate |
| @s53 | UI JSON tardío en listado/check tras navegación y CSRF tras revocación; señales abortadas y foco conservado |
| @s54 | UI fallos de configuración/preview/lista sin perder confirmación o borrador |
| @s55 | UI enlace a disponibilidad con descarte explicado |
| @s56 | TaskState compartido y recuperación con proyecto/tarea completados; E2E proyecto completed |
| @s57 | UI muestra UTC explícito y zona original si Intl no puede resolverla |
| @s58 | UI foco origen/encabezado/exterior y teclado nativo en tres motores |
| @s59 | Matriz de controles, errores asociados, reflow y axe; estilos SCSS |
| @s60 | Chromium/Firefox/WebKit y zoom nativo Chromium al 200 %, documentados separadamente de viewport |
| @s61 | Matriz de treinta principios y medición de feedback; límites humanos explícitos |
| @s62 | s62_unreadableJson y s62_creationUnreadableJson |

Los tags agrupan comportamientos: no se identifica el número de tests con los 325 Examples ni se afirma que cada E2E cubra todas las variantes de sus etiquetas. Las pruebas y bitácoras detallan cada parámetro y distinguen los niveles HTTP, dominio, persistencia y UI.

## Disciplina y calidad

Ciclos RED/GREEN documentados para código nuevo. Cobertura adicional inicialmente verde identificada como tal. Hallazgos corregidos y revisados: anclas de días omitidos/invertidos, precisión temporal y límites, precedencia con cuerpo ausente, reservas globales, recheck de idempotencia, preferencia insertada después de observar ausencia, rowcounts y traducción de errores hasta COMMIT, foco y retirada de contexto privado. La regresión de configuración se corrigió añadiendo puertos al fixture, sin debilitar aserciones de capacidad.

Dominio/aplicación conservan fronteras hexagonales; PostgreSQL coordina locks y atomicidad, outbox entrega al menos una vez. No hay reenvío automático de creación ni confirmación ficticia. Frontend usa React/SCSS y controles nativos, sin nuevas dependencias. El límite conocido de cargar historia del propietario tiene comentario explícito; no se añade infraestructura especulativa.

Arnés: targets cerrados y argumentos constantes; default completo conservado. Siete pruebas stdlib comprueban selección, rechazo y conexión CLI/CI sin lanzar mutadores. PIT incluye ApplicationConfiguration y los adaptadores/modelos nuevos, mantiene umbral 80 y recibe classpath de recuperación por provider lazy comprobado sin ejecutar mutación. Stryker incluye API/UI y TaskReader/TaskState completos; mantiene umbral 80. Su exclusión de una carpeta protegida fue validada sintéticamente, sin leerla ni limpiarla.

## Verificación y checkpoints

- C1/C2: init 34832 EXIT 0 (1b3236), configuración válida y una sola feature activa.
- C3: arquitectura y dependencias revisadas; formato/lint verdes. Build frontend b96beb y builds reales Docker verdes.
- C4: 1338 backend sin fallos/errores/omitidos (XML 22cb2f), 1121 frontend y siete tests del arnés. PostgreSQL y RabbitMQ reales en sus suites.
- C5: estado activo documentado. Commit/cierre e historial final se completarán después de mutación; no se afirma limpieza de temporales protegidos.
- C6: contrato aprobado, mapas y ciclos enlazados; no se detecta comportamiento de producto ajeno al contrato.
- C7: pendiente, a cargo de PIT/Stryker después de este dictamen.

E2E: los siete recorridos de bloques pasan en cada motor; revisión independiente del E2E aprobada. Regresión completa del coordinador: 57/58 verdes (9109e0). El único fallo fue timeout histórico de disponibilidad; focal con traza pasó en 27 segundos, con 23,9 en la matriz. División revisada: conserva exactamente los 28 anchos, altura reducida, estado de error, bounds, navegación y axe; no cambia SCSS ni timeout. Grupo afectado 31/31 verde (aebd37), con funcional 1,4 segundos y anchos 1,5–2,5 cada uno. Son ejecuciones separadas y dos casos solapados, no una supuesta ejecución global nueva de 86/86. La incidencia queda resuelta mediante comprobación de la parte modificada y conservación de los otros 57 resultados.

No se atribuye evaluación humana, dispositivos físicos o lector real a las mediciones automáticas. No hay despliegue productivo ni cierre de MVP. No quedan cambios bloqueantes para iniciar mutación de esta feature.
