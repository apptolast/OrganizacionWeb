# Alcance pendiente de PIT para Replanificar

Revisión de preparación, no configuración aplicada ni campaña ejecutada. Lectura c71e1f/04b533/76fcf3: el Gradle actual no tiene mutationScope=reschedule y el dispatcher del árbol backend aún no contiene reschedule-backend. No usar el target genérico como si acreditase todos los nuevos adaptadores13: su unión de clases puede omitir controladores y consultas nuevas.

Antes de ejecutar, el autor de mutación debe definir un target explícito, después del freeze integrado. Mantener umbral80, mutadores/filtros heredados y reportes separados. Concurrencia PIT actual4; la propuesta12 no está validada y no se aplica por extrapolar el resultado de Stryker.

## Inventario para contrastar contra el diff final

- Nuevos casos de uso y cambios de dominio: MoveBlock, CancelBlock, ReadBlockChanges, BlockChanged y su Interval, BlockMutation, BlockChangeReceipt/Confirmation, BlockMoveRequest, BlockState y cualquier validación añadida en BlockRequest.
- Reutilización modificada: PlanBlock, ReadBlocks, OutboxMessage. Incluir sus cambios efectivos aunque el nombre no contenga Reschedule.
- Fronteras: RescheduleController y records internos; controlador de recibos/historia con su nombre final; BlockController y ApiErrors al compartir handlers; PostgresBlockStore, PostgresBlockChangeQueries, PostgresTodayQueries, RabbitBrokerPublisher y ApplicationConfiguration.
- Contrastar tipos con bytecode ejecutable: interfaces o records generados sin comportamiento no requieren inventar mutantes; conservar el filtro heredado y documentar el alcance real observado.

Seleccionar todas las pruebas pertinentes de contrato, dominio, aplicación, HTTP, PostgreSQL, wiring y broker. La suite global como conjunto de candidatos evita omitir regresiones de clases compartidas; PIT elegirá por cobertura. Si se acota la lista de tests, justificar cada omisión frente a las clases y comportamientos modificados. No excluir clases compartidas ni sus mutantes difíciles sólo para obtener un porcentaje mayor.

La migración SQL V13 no queda mutada por PIT de bytecode. Su evidencia independiente es lectura de constraints, upgrade y rollback reales con Flyway/PostgreSQL. No denominar esa evidencia score SQL.

Salida requerida: hashes del corte, comando y estado final, inventario real, Killed/Survived/NoCoverage/Timeout/errores separados y límites de herramienta. No contar timeouts o errores como prueba de detección funcional. Analizar supervivientes relevantes antes del dictamen, sin exigir una cifra100 artificial ni reclasificaciones indiscriminadas.
