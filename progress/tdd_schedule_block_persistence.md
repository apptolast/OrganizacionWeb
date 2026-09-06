# TDD persistencia de bloques — schedule_block

Contrato aprobado a84e42f, Ponytail full y Caveman lite. Cesión exclusiva de PostgresBlockStore.java, V11__planned_blocks.sql y ScheduleBlockPersistenceTest.java. La capa HTTP/puertos permanece con su autor. Baseline HTTP138 GREEN reportada por backend; núcleo/publicador congelados. Gradle y formato coordinados; sin PIT. Una prueba de comportamiento cada vez.

## Ciclos

1. @s12: fixture completado inicialmente infringe tasks_completion_consistent; se corrige completed_at antes de evaluar negocio. RED real 02babf: preview devuelve plannedSeconds=0 pese a reservas propias de otros proyectos completados. GREEN 4a2503: consulta de bloques por owner a través de projects, sin filtrar estados; otro owner excluido.
2. @s13/@s15: RED 2/2 (4cb018): creación ignora reservas de otro proyecto propio y admite solape/exceso. Se reutiliza ownerBlocks dentro del contexto transaccional de creación. Se conserva rechazo de solape incluso con consentimiento y ausencia de escrituras ante ambos conflictos.

3. @s29: RED 8f35d3: dos sesiones PostgreSQL verificadas esperando el mismo bloqueo de disponibilidad terminan con BlockOverlapException en la segunda. GREEN 76210a: helper replay conserva comparación exacta antes y después del bloqueo; resultados una creación y un replay idéntico, un bloque y una outbox.
4. @s30: tres carreras coordinadas por bloqueo real (solape entre proyectos, exceso combinado sin permiso, misma key/intenciones distintas) inicialmente verdes con las correcciones anteriores. Se exige una creación y el conflicto concreto, una fila de bloque y una outbox; no se atribuye implementación nueva a estos casos.

5. @s34: RED 5/5 (8cfc3a). Los triggers reales prueban fallo/supresión de INSERT en ambas tablas y rechazo diferido de COMMIT. GREEN 02aaee: exigir exactamente una fila en bloque/outbox y traducir DataAccessException/TransactionException fuera de transaction.execute, incluyendo fallo al confirmar. Se verifica rollback completo, by-request ausente y proyecto/tarea/disponibilidad intactos.
6. Invariantes DB: RED 10/11 (beb3e3); relación compuesta ya protegida. Se añaden UNIQUE(task_id,request_key), orden y duración real/1–1440, precisión de segundos UTC, años públicos UTC/locales y precisión local de minutos en V11. Los tests atacan SQL directamente y comprueban que fila válida y evento permanecen intactos. No se debilita validación por fixtures.

7. @s33, ausencia: RED 2/2 (920455). Un spy delimita el locking SELECT vacío y hace confirmar una inserción real desde otra conexión antes del siguiente SELECT. Preview y creación aceptaban una preferencia no bloqueada. GREEN 674623: conservar Optional.empty cuando el locking SELECT no obtuvo fila, sin reemplazarlo con find posterior; preferencia concurrente persiste, pero no se escribe bloque/outbox.
8. @s31: seis casos inicialmente GREEN (677064), con adaptadores reales ChangeTaskStatus/PostgresTaskStatusStore, ChangeProjectStatus/PostgresProjectStatusEditing y PostgresAvailabilityStore. Transacción exterior retiene el primer commit; pg_stat_activity verifica que el segundo espera el lock real antes de liberarlo. Ambos órdenes terminan; primero creación conserva bloque histórico, primero cambio produce TASK_COMPLETED/PROJECT_COMPLETED/AVAILABILITY_CONFLICT. Se comprueban eventos de bloque y eventos propios de estado, sin éxitos parciales. SHOW transaction_isolation acredita read committed en estas transacciones.

9. @s33, snapshot configurado: dos casos inicialmente GREEN (4b9d69). Preview retiene su contexto bajo SHARE, mientras writer real de preferencias o reserva queda bloqueado en PostgreSQL. Tras liberación se comprueba snapshot original entero y lectura nueva con revisión/zona/presupuesto o plannedSeconds actualizado. Sólo el writer produce sus escrituras.
10. @s32: inicialmente GREEN (ec1f41). Una creación retiene todos sus locks antes del commit; otra persona confirma el mismo intervalo mientras la primera continúa pendiente. Después ambas terminan con dos bloques y dos eventos, identidades distintas.
11. Lecturas con almacenamiento fallido: RED 4/4 (16d8d6) al renombrar temporalmente la tabla real, con restauración en finally. Se extrae execute común conservando SQL de consultas y captura de DataAccessException/TransactionException en preview/list/detail/by-request/commit. El fallo sigue siendo StorageUnavailableException; se comprueba recuperación posterior e identidad persistida intacta.

## Freeze para revisión independiente — 2026-09-06

Tramo funcional completo. Ejecución propia previa de toda ScheduleBlockPersistenceTest: **38 pruebas, 0 fallos/errores/omitidas**, EXIT 0 d9ef28 (26 segundos). Después, el autor HTTP ejecutó Spotless global y regresión conjunta en frontera de edición acordada: **170 HTTP + 38 persistencia = 208 pruebas verdes**, salidas reportadas 949b02/96219f. Este autor leyó los dos XML finales y confirmó conteos y cero fallos/errores/omitidas; no se atribuye una segunda ejecución independiente propia.

Archivos congelados y SHA256 después del formato:

- backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresBlockStore.java — 240FC482A73984FF004B35EDB6C6E3552E38F38E629769DB8723F1E5D94A500F
- backend/src/main/resources/db/migration/V11__planned_blocks.sql — D5E66E334813A0FB2BE844F9E1CDB92E79A844B2DDE44E54F43464138E25E986
- backend/src/test/java/com/apptolast/organization/adapter/persistence/ScheduleBlockPersistenceTest.java — 9F70F576EC22B28965977EC1AC20A949594F3964F5694EE55CCEA79CA1420F9A

### Mapa @s29–@s35

Todos los métodos siguientes están en ScheduleBlockPersistenceTest, paquete adapter.persistence:

- @s29 — s29_sameKeyWaitingForAvailabilityReplaysAfterWinningCommit (línea 168). Dos esperas reales de disponibilidad antes de liberar el bloqueo externo; una creación y un replay, bloque completo igual, una fila y una outbox.
- @s30 — s30_serializesConflictingReservationsAcrossProjects (línea 216). Tres carreras: solape, exceso combinado e intención distinta con misma key. Una creación y el conflicto concreto; una fila y una outbox.
- @s31 — s31_realStateAndPreferenceWritersCoordinateInBothCommitOrders (línea 473). Seis combinaciones usando escritores reales de tareas/proyectos/disponibilidad, espera de locks observada y ambos órdenes de commit. Acredita read committed en ejecución.
- @s32 — s32_otherOwnerCommitsWhileFirstOwnerStillHoldsAvailability (línea 610). Owner B confirma mientras owner A mantiene transacción y locks sin confirmar; después dos bloques/eventos.
- @s33 — s33_absentLockedPreferenceCannotBeReplacedByUnlockedConcurrentInsert (línea 356), preview/creación y fila inicialmente ausente; s33_previewHoldsOneCoherentSnapshotAgainstConcurrentWriters (línea 538), escritor de preferencias/reserva esperando al preview y comparaciones completas antes/después.
- @s34 — s34_rollsBackBothWritesAndClassifiesEveryPrecommitFailure (línea 268), cinco fallos reales; s24_s26_s34_readFailuresStayStorageErrorsWithoutPartialResults (línea 653), cuatro lecturas con SQL fallido y recuperación posterior.
- @s35 — reinicio real del proceso backend y recuperación por key pertenecen a la evidencia E2E del autor frontend. Este test de persistencia no simula un reinicio ni se atribuye esa evidencia.

Las carreras de esta suite invocan PlanBlock y adaptadores reales contra PostgreSQL; observan BlockCreation.replayed y excepciones concretas. No se presentan como peticiones MockMvc concurrentes: status HTTP/Location/DTO/códigos corresponden a ScheduleBlockApiTest y a E2E. Las restricciones DB adicionales se prueban por SQL directo en s2_s6_s10_s29_databaseRejectsInvalidStoredIntervalsAndDuplicateKeys (línea 324).

Límite deliberado documentado en código: se carga el historial del owner para calcular intersecciones; si el volumen medido lo exige, limitar la consulta a los días afectados. No hay bloqueo global nuevo, ventanas arbitrarias de días ni filtros por estado que liberen reservas. No se modificaron puertos, controlador, ApiErrors, núcleo/publicador congelados ni temporales bloqueados. Sin PIT ni aprobación final de feature desde este autor.
