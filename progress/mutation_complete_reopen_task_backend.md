# Mutación backend: completar y reabrir tareas

Ponytail full y Caveman lite activos. Campaña autorizada por el judge actualizado después de init 58990 verde (798 backend, 625 frontend y lint) y verificación separada de las cuatro fixtures históricas (163 pruebas verdes). Producción congelada.

Comando: `./gradlew.bat pitest -PmutationScope=complete_reopen_task`, JAVA_HOME JDK 25. Sesión 11298. Informe esperado: `backend/build/reports/pitest-complete-reopen-task/mutations.xml`. Umbral 80, cuatro hilos, margen de inicio de contenedores de 15 segundos conservado desde campañas previas; ningún cambio para elevar score.

Alcance: Task, snapshots/versión e historia, ChangeTaskStatus, ReadTaskStatus, ReadTaskHistory, TaskStatusChanged y cambio, OutboxMessage, HTTP de estado/historia, ApiErrors completo, JDBC de estado/historia y queries compartidas, RabbitBrokerPublisher. Puertos y DTO sin ramas no se presentan como lógica mutada. ApiErrors conserva handlers históricos y sus suites para que el nuevo 412 no oculte código existente. El perfil CI predeterminado incluye los adaptadores propios nuevos.

Resultado original: sesión 11298 EXIT 0, 270/270 mutantes KILLED (100 %). XML comprobado mediante parser: no hay SURVIVED, TIMED_OUT, NO_COVERAGE, errores de memoria o ejecución. Cobertura de líneas del alcance mutado: 498/503. No hay supervivientes que justificar ni replays necesarios. El denominador incluye lógica nueva y compartida; no se atribuyen los 270 exclusivamente a líneas nuevas.

La campaña tardó 715 segundos, de los que 45 fueron cobertura y 669 análisis. Se ejecutaron 1228 tests de mutación. La repetición de contenedores RabbitMQ por iteración explicó la mayor duración; inspecciones de hilo y contenedores confirmaron nuevos arranques, y el resultado final tiene cero timeouts. No se modificó el margen de 15 segundos ni se convirtió ese tiempo en evidencia de detección.

Original conservado en build/reports/pitest-complete-reopen-task/mutations.xml y copia ignorada .e2e-work/pit-complete-reopen-task-original.xml. No se ejecutó una segunda campaña ni se mezclaron resultados. Se mantiene producción congelada; cierre global y metadatos quedan a la espera del juez y de los otros alcances.

## Desglose del XML original

| Clase | Mutantes KILLED |
| --- | ---: |
| ApiErrors | 13 |
| ChangeTaskStatus | 9 |
| OutboxMessage | 67 |
| PostgresTaskHistoryQueries | 5 |
| PostgresTaskQueries | 12 |
| PostgresTaskStatusStore | 10 |
| RabbitBrokerPublisher | 36 |
| ReadTaskHistory | 3 |
| ReadTaskStatus | 1 |
| Task | 28 |
| TaskHistoryController | 29 |
| TaskHistoryEntry | 5 |
| TaskHistoryPage | 2 |
| TaskHistoryPosition | 1 |
| TaskRevision | 2 |
| TaskSnapshot | 10 |
| TaskStatusChange | 2 |
| TaskStatusChanged | 9 |
| TaskStatusController | 26 |
