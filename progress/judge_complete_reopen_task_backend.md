# Revisión independiente de transiciones backend — complete_reopen_task

Revisor: agente de integración. Alcance independiente: ChangeTaskStatus, TaskSnapshot, TaskStatusController, PostgresTaskStatusStore, V9, extensión TaskStatusChanged de OutboxMessage/RabbitBrokerPublisher y sus pruebas escritas por el agente backend. **Excluidos del dictamen independiente**: cliente frontend y lectura backend del historial, que escribió este revisor y juzga el coordinador. Ponytail full/Caveman lite.

## Dictamen de fuente previo al cierre

Favorable, sin hallazgos bloqueantes en el corte leído. No es aprobación global: faltan suite conjunta congelada, mutación autorizada y ejecución de navegador/publicador.

- ETag y cuerpo salen del mismo TaskSnapshot; DTO de estado tiene exactamente tres campos y las lecturas anteriores conservan ocho. UUID de ruta admite caja distinta; la precondición exige representación canónica fuerte, identidad y BIGINT.
- El controlador respeta ruta, precondición y JSON estricto; los campos desconocidos preceden a status y se ordenan léxicamente. El servicio verifica la versión antes del no-op, dentro del bloqueo transaccional. Propiedad y existencia se resuelven antes de esa versión.
- FOR NO KEY UPDATE OF t limita el bloqueo a la tarea y permite la comprobación de FK al crear hijos. Las pruebas s16/s28 usan conexiones PostgreSQL reales y latches acotados; prueban 200/412 y ambos órdenes de creación/completado, con resultados persistidos.
- UPDATE, historia y outbox comparten TransactionTemplate. Cada escritura exige una fila; seis casos de trigger con excepción o supresión comprueban rollback íntegro. La historia tiene FK propia a tarea y unicidad tarea/versión, sin dependencia del outbox.
- El reloj se trunca a microsegundos y no reduce updatedAt. La reapertura limpia completedAt; completar lo iguala al instante confirmado. Ni proyecto ni descendientes se modifican; se prueban los cuatro estados de proyecto.
- La sexta ruta mantiene allowlist cerrada, nueve campos y transición real conocida; aggregateId es proyecto y taskId independiente. Se conserva JSON original y transporte persistente/quorum de las cinco rutas previas. La prueba real Rabbit del autor cubre el nuevo enrutamiento; el smoke integrado todavía está pendiente.

Evidencia leída: `progress/tdd_complete_reopen_task_backend.md`, TaskStatusApiTest (incluidas pruebas de atomicidad, privacidad y carreras), fuentes de aplicación/adaptadores y V9. El informe no suma sus resultados a la suite aislada de historia ni atribuye ejecuciones ajenas al revisor.

## Puerta de mutación del coordinador

**APPROVED para PIT complete_reopen_task**, 2026-09-06. Dictamen conjunto: revisión independiente de transiciones anterior y revisión independiente del historial en judge_complete_reopen_task_history.md. Init 58990 del coordinador: lint verde, 798 pruebas backend y 625 frontend, cero fallos. El posterior ajuste de cuatro fixtures históricas sólo conserva un endpoint PostgreSQL por JVM durante PIT, con limpieza Ryuk; no cambia producción ni aserciones. Diff leído por el coordinador y ejecución focal 94651 del autor: 163/163 verdes, registrada separadamente del init.

Perfil contrastado: incluye nuevos controladores y persistencia de estado/historial, aplicación y dominio con lógica, Task, queries compartidas, OutboxMessage, RabbitBrokerPublisher y ApiErrors. Los handlers históricos conservan sus suites, el perfil global incorpora los adaptadores nuevos y el umbral sigue en 80. Preservar el informe original, justificar supervivientes y separar replays. Esta puerta no declara cierre de la funcionalidad; faltan mutación e integración final.

Actualización de integración: suite final Chromium 43/43, smoke seis rutas y persistencia tras reinicio verdes; Firefox/WebKit 2/2. Estos resultados constan en tdd_complete_reopen_task_integration.md y no sustituyen la revisión independiente de la lectura propia por el coordinador. Se mantiene pendiente el resultado de mutación backend para cierre global; no hay nuevos hallazgos de transiciones en esta integración.
