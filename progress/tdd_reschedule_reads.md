# TDD lecturas de cambios — feature13

Ámbito autorizado tras aprobación de Move: @s16–18 y @s25, puerto/caso de uso, adaptador PostgreSQL y controlador de recibos por ID/key e historial. Move, frontend, Store/RescheduleController y wiring permanecen de sus autores. No Git, build, mutación ni nuevas dependencias. Ponytail full y Caveman lite; lecturas de instrucciones43dd60/36bbad, contrato y reutilización dac618/602b79.

Baseline coordinado vigente; no repetir init mientras publicador/core compilan. El autor de wiring añadirá bean y mock cuando el constructor compile. Un caso por RED/GREEN; no preparar matrices enteras. Una prueba inicialmente verde se registra como refuerzo, sin fabricar RED.

Reutilización: BlockChangeReceipt durable y RescheduleController.ReceiptResponse.from; BlockController.identifier/invalid tienen visibilidad de paquete. ReadTaskHistory/ReadBlocks ya usan lookahead21 y páginas de20; seguir ese patrón sin alterar sus contratos. Cursor heredado es privado y específico de collection/fecha: valorar extracción mínima compartida únicamente al llegar al test HTTP que la exija, coordinando propiedad antes de editar otro archivo.

Decisión de lectura acordada con root/core: TransactionTemplate propio readOnly, snapshot REPEATABLE_READ para contexto y recibos coherentes, sin cambiar template compartido ni SELECT FOR SHARE. Traducir almacenamiento y fallo al finalizar transacción a STORAGE_UNAVAILABLE503. Confirmar propietario/proyecto/tarea dentro del mismo snapshot antes de distinguir ausencia de recibo. GET no fabrica cambios ni depende del outbox.

Primer ciclo previsto: historial vacío conserva contexto/cursor y devuelve items vacío/next null. Todavía no hay prueba escrita ni ejecución: publicador termina regresión y core tiene dos ciclos coordinados antes de ceder Gradle.

## Ciclo1 — @s16 historial vacío

ReadBlockChangesTest.s16_emptyHistoryKeepsContextAndHasNoContinuation: un caso, contexto exacto delegado al puerto, cursor inicial null y página vacía sin continuación. REDd278b6 por clases inexistentes. Se añaden únicamente caso de uso/puerto de listado y valores página/posición, siguiendo el patrón ReadTaskHistory. El adaptador aún no existe y no se afirma privacidad PostgreSQL desde esta prueba. GREEN en ejecución al registrar (sesión44352); no se inicia otro caso antes del resultado.
GREEN5e915a EXIT0: primer caso ejecutado. Java quieto y Gradle devuelto a core para sus ciclos. No bean anticipado: el constructor ReadBlockChanges(BlockChangeQueries) compila, pero la implementación del puerto aún deberá nacer de un RED PostgreSQL. Próximos casos20 y21 separados, y sólo después adaptador/HTTP; no se declara paginación completa por el vacío.

## Ciclo2 — @s16 página terminal20

s16_exactlyTwentyReceiptsAreTerminalAndKeepTheirOrder se añadió solo y fue inicialmente GREENe5d3d2. Conserva los20 recibos en orden y next null; no necesitó cambio de producción. Los valores de recibo son opacos en esta prueba de paginación, no una validación del DTO HTTP.

## Ciclo3 — @s16 lookahead21

s16_twentyOneReceiptsUseTheLastServedPositionForContinuation añade un solo caso con empate de occurredAt, IDs descendentes y cursor de entrada concreto. RED602a4f: se devolvían21 elementos. Mínimo limita20 y, sólo si quedan más, crea continuación con occurredAt/id del último servido, nunca el elemento21. GREEN87cb7c EXIT0, tres pruebas del caso de uso. Ventana cedida a core para estado readOnly; todavía no implementación PostgreSQL ni HTTP.

## Ciclo4 — @s16/@s18 lectura durable PostgreSQL

BlockChangeQueriesPersistenceTest.s16_s18_readsDurableCancellationWithoutOutboxOrAvailability usa PostgreSQL17.9 real con Flyway y serializa BlockChangeReceipt/PlannedBlock completos, no DTO HTTP. Proyecto y tarea completed, intervalo pasado, sin disponibilidad/outbox/proyección. RED5e647d por adapter ausente. Mínimo lee JSON durable filtrado por propietario/proyecto/tarea.

8eb2d9 falló en preparación: tarea completed sin completed_at violaba tasks_completion_consistent (94b698). Se añadió la fecha requerida, sin cambiar el escenario a pending. GREENe98444 EXIT0, un caso PG. No se atribuye ese fallo de fixture al negocio. Se conserva ausencia de outbox/proyección después de leer.

Límite explícito de este corte: adapter todavía nominal, sin distinción de contexto ausente, transacción readOnly, orden/cursor/límite ni traducción503; esos comportamientos requieren siguientes casos individuales. El parámetro manager reservado por la firma acordada aún no se usa. No hay @Component, wiring o ruta HTTP publicada antes de completar esas guardas. Java quieto y ventana devuelta a core.

## Ciclo5 — @s18 transacción readOnly real

s18_readsInsideReadOnlyRepeatableReadTransaction observa SHOW transaction_read_only y SHOW transaction_isolation desde la conexión PostgreSQL vinculada a la consulta. RED3dca7d: estado off. Template propio configurado readOnly/RR alrededor de la lectura; GREENe812ca. No cambia manager/template ajeno ni incorpora locks. El oráculo confirma ejecución de la consulta, no sólo configuración en memoria.

## Ciclo6 — @s18 contexto ajeno

s18_foreignContextIsNotAnEmptyHistory, un caso: propietario distinto sobre IDs existentes debe recibir ResourceNotFoundException, no página vacía. RED28bf62; comprobación de pertenencia proyecto/tarea dentro del mismo snapshot antes de consultar recibos. GREENb6b652 EXIT0,3PG+3app. Java quieto y Gradle cedido a core. Restan orden/límite/cursor PG, recibos ID/key, fallos de almacenamiento/cierre y HTTP. No se ha conectado un endpoint incompleto.

## Ajuste de fixture en GREEN

La fixture inicialmente aislada conservaba recibo sin proyección. Tras revisión se extrajo cancellation(id,occurredAt) y ahora siembra un bloque distinto con proyección cancelled/revisión2 por recibo; no crea reservas planned superpuestas ni duplica versión del mismo bloque. Antes/after sigue PlannedBlock válido; sin outbox deliberadamente. Regresión de los3PG GREENa6dfac. El conteo esperado de proyecciones pasa de0 a1 por esa preparación explícita, no por escritura desde GET. No se atribuye atomicidad de comandos a una fixture SQL.

## Ciclo7 — @s16 orden y lookahead SQL

s16_limitsLookaheadToTwentyOneAndOrdersByTimeThenUuidDescending:22cancelaciones coherentes, fecha antigua y empates con UUID conocidos. REDdba3c6; ORDER BY occurred_at DESC,id DESC LIMIT21. GREEN969bee,4PG. El caso compara conteos de recibos/proyecciones/originales/outbox antes y después de la lectura.

## Ciclo8 — @s16 continuación estricta

s16_continuesStrictlyAfterTimeAndUuidWithoutRepeatingBoundary: cuatro recibos, cursor en mitad de empate; sólo devuelve el empate con UUID inferior y la fecha anterior incluso con UUID superior. RED607ff3; predicado de pareja (occurred_at,id)<(cursorAt,cursorId). GREENa93e8a EXIT0,5PG+3app. No se repite frontera y el cursor null conserva primera página. Java quieto y Gradle cedido al núcleo.

## Ciclo9 — @s18 fallo al terminar readOnly

s18_translatesFailureAfterReadOnlyTransactionCommit ejecuta lectura real y un manager delegado que, después del commit, lanza TransactionSystemException. RED0bbc83; captura fuera de TransactionTemplate.execute y traducción StorageUnavailableException conservando causa. GREEN591ea0,6PG. No se promete reproducir una avería física; el punto de fallo posterior a consulta/commit se inyecta explícitamente.

## Ciclo10 — @s18 fallo SQL

s18_translatesSqlFailureWithoutReturningAnEmptyHistory renombra block_changes únicamente en el PostgreSQL efímero de esta clase y lo restaura en finally. La consulta real produce DataAccessException. RED7944d6; ampliación mínima del catch para traducir esa familia sin capturar ResourceNotFoundException. GREEN2e1155 EXIT0,7PG+3app; duración55s. Ventana devuelta a core. No detalles HTTP probados todavía: STORAGE_UNAVAILABLE se verificó en la frontera del puerto, su503/no-store se comprobará con controlador.

## Refactor GREEN previo a recibos

Se extraen read(owner,project,task,Supplier) y mapper receipt para reutilizar contexto, snapshot y traducción al consultar recibos. GREEN9d925a,7PG+3app; no se añadió comportamiento nuevo durante extracción.

## Ciclo11 — @s18 recibo por ID

s18_readsReceiptByItsOwnIdWithoutChangingTheHistoricalSnapshot: recibo durable completo por ID con contexto completed, sin otro recibo/evento. REDe423fc/8dadcf por método ausente; mínimo detail reutiliza read/mapper. GREEN066d26 EXIT0,8PG. Ausencia de recibo y key siguen pendientes y no hay endpoint público; este corte sólo nominal.

Root solicita snapshot compilable para autor independiente de handlers: fuentes/pruebas propias quietas tras066d26, sin copiar ni tocar BlockController/ApiErrors. Esperar COPYDONE antes de escribir Java. El autor del núcleo retoma después dos ciclos; byKey se continuará al recuperar ventana.

## Ciclo12 — @s25 key histórica tras cambio posterior

s25_recoversOriginalMoveByKeyAfterLaterCancellationAndNewAdapter prepara historia coherente movimiento2/cancelación3 con proyección3 y recupera por key el recibo2 exacto. RED372bfc por byRequest ausente; consulta durable filtrada por contexto/key, GREENe71c48,9PG. Conteos de recibos/outbox permanecen iguales y la proyección sigue3. El adaptador es nuevo; **no se reinició un proceso backend**. La parte de reinicio real de @s25 corresponde a integración/E2E posterior. La fixture SQL no acredita atomicidad de comandos.

## Ciclo13 — @s18 ausencia por ID

s18_missingReceiptIdInOwnContextHasItsDistinctFailure: RED432305 por excepción ausente, mínimo BlockChangeNotFoundException y traducción de ausencia sólo en detail. GREENb85946 EXIT0,10PG+3app. Ausencia por key sigue siguiente caso; puerto de aplicación aún sólo listado y no hay controlador público.

Corte b85946 quieto y sin Gradle activo para integración de V13 solicitada por root. Después se cede ventana a core; no cambiar recursos Flyway ni Git desde este autor. BlockController/ApiErrors permanecen reservados al autor independiente de handlers.

## Baseline V13 y ciclo14 — ausencia por key

Tras integración64d5174, los10PG vigentes vuelven a pasar GREEN6c3e6a antes de añadir ningún caso. s18_missingRequestKeyInOwnContextHasItsDistinctFailure reproduce ausencia mal clasificada: RED1fb05a/9905bb; mínimo byRequest usa BlockChangeNotFoundException. GREEN84f1d6,11PG.

Refactor GREEN de pruebas app: tres fakes funcionales pasan a mocks para poder ampliar el puerto sin métodos default ficticios; conserva verificación de contexto exacto/cursor y orden/contenido de páginas. Primer comando02ecd0 tuvo error de sintaxis PowerShell antes de escribir; no cuenta como RED de producto. Refactor ejecutado GREEN3772dd,3app. Ventana cedida al núcleo para carrera; forwarding todavía no empezó.

## Ciclos15–16 — puerto de recibos

s18_forwardsReceiptIdAndExactContextWithoutChangingTheReceipt: RED816671, métodos detail mínimos en puertos y caso de uso; GREEN1709cb,4app. s25_forwardsOriginalKeyAndContextWithoutReadingCurrentState: REDcdf9f8, métodos byRequest; GREEN62adde,5app. El adaptador ya implementaba ambos concretamente, por lo que no se introducen stubs/defaults para hacer compilar. Se exige misma instancia de recibo, contexto/key exactos y ninguna consulta adicional.

## Ciclo17 — primer HTTP PostgreSQL real, pendiente GREEN

BlockChangesApiTest.s16_readsConfirmedEmptyBlockChangeHistory reutiliza montaje Spring/seguridad/PostgreSQL del historial de tareas, con fixture propia y un único caso. GET literal /blocks/changes devuelve400 blockId INVALID_FORMAT, REDb1c35c confirmado por XMLb0d069. Se añade BlockChangesController mínimo para listado vacío y @Component del adaptador. Root/core conserva ApplicationConfiguration y sus dos tests de contexto: cesión explícita para añadir bean ReadBlockChangesUseCase y observar/arreglar mocks necesarios. No Gradle propio activo al ceder; fuentes/pruebas quietas hasta su GREEN. El controlador todavía no implementa cursor/recibos/query y no se declara HTTP completo.
Ciclo17 cierre compartido: core añadió bean; su foco observó18fallos de mocks de contexto RED973e2c y añadió únicamente BlockChangeQueries a esos fixtures. GREENd7fbec/XMLa4a25b comunicado por core:23casos (HTTP nuevo1 + contextos22). No atribuir esta ejecución al autor de lecturas ni18defectos funcionales. Fuentes quietas hasta que root integre handlers92e83e6; BlockController aún no cedido.

## Ciclos18–19 — recibos HTTP reales

s18_readsClosedHistoricalReceiptById: RED64ca19 por ruta ausente; GET /{changeId} reutiliza ReceiptResponse.from. GREEN95dcd6,2HTTP. Oráculo cerrado de7campos, revisión textual, microsegundos, after null y before Block9 exacto. Fixture real serializa dominio y proyección cancelled coherente.

s18_recoversTheSameReceiptByRequestKeyWithoutLocationRequirement: RED3129b1; GET literal /by-request/{requestKey} delega puerto y reutiliza DTO. GREENb73a0b EXIT0,3HTTP. Compara contenido completo con recibo porID ya comprobado; conteos durable/outbox sin cambios. No se exige Location en GET y no se simula un POST/replay.

Se libera Gradle a core para carreras. Listado/ID/key nominales operativos; todavía faltan erroresHTTP/query/cursor y verificación final de formato/regresión. BlockController sólo cedido para extracción mínima del cursor compartido, handlers integrados permanecen intactos.

## Extracción GREEN del cursor heredado

BlockController fue cedido explícitamente después de INTEGRATED2b9537. Se mueve únicamente el decoder a BlockCursor.decode con collection/timestampField explícitos; createdAt/blocks, encoder y demás helpers/handlers11 permanecen. GREEN961758,173API11+3HTTP13. No se reintroducen handlers retirados ni se edita ApiErrors. Los controles de base64url canónico, JSON cerrado, duplicados/trailing, contexto, UUID y tiempo son lógica heredada verificada, no una matriz nueva adelantada.

## Ciclo20 — @s16 páginas HTTP20+1

s16_pagesTwentyOneReceiptsWithClosedScopedCursorAndNoDuplicates: RED743e0d por nextCursor null. Conexión del cursor BlockChanges/occurredAt al decoder compartido y encoder cerrado de5campos, manteniendo20elementos y página siguiente sólo el restante. GREEN881ea0,4HTTP. Verifica orden de UUID empatados, contexto del cursor, canonicidad base64url y ausencia de frontera duplicada.

## Ciclo21 — @s17 query antes de IDs

s17_rejectsUnknownHistoryQueryBeforeParsingContextIds: REDd4b9ec (field projectId en vez de query), guarda de claves permitidas antes de parsear IDs. GREEN326d1f,5HTTP. Todavía restan cursor repetido, erroresHTTP y representación terminal20. GJF focal de13archivos (12propios y BlockController cedido) y diffcheckc8e213 EXIT0; sin formato global ni archivos de otros autores. Ventana devuelta a core.

## Ciclos22–24 — límites y error HTTP

s16_exactlyTwentyReceiptsHaveNoContinuation: inicialmenteGREENa95057, un caso explícito solicitado por root para terminal20. No producción nueva.

s17_rejectsRepeatedOtherwiseValidCursor: RED1b0215 (aceptaba el primer valor), mínimo size1 antes de decodificar; GREEN08883d,7HTTP.

s18_missingReceiptIdReturnsItsPublic404WithoutStorageDetails: REDe5a8c5 por500, handler local BlockChangeNotFoundException devuelve404 problem+json/no-store con código propio y sin detalles internos. GREENb27df3,8HTTP. No modificación de ApiErrors compartido. Ventana cedida al núcleo; pendientes query en ID/key y comprobaciones de conexión/privacidad/503. No se replicará matriz de decoder ya cubierta por173API11.

### Ciclos 25–26: queries de recibos, un caso por ciclo
- ID: `s18_rejectsReceiptIdQueryBeforeParsingIds`, RED `1b2f77` (se informaba projectId antes de query); primer reemplazo textual no aplicó por finales de línea y repitió el mismo RED `02361d`, sin cambio productivo. Guarda mínima aplicada; GREEN `1abeaf`.
- Key: `s18_rejectsReceiptKeyQueryBeforeParsingIds`, RED `051282`; guarda de query vacía antes de identifier en la ruta by-request. Regresión HTTP completa ejecutada a continuación (sesión 70577), 10 casos. No cambios al decodificador ni al DTO.
- Resultado de la regresión del ciclo26: GREEN `b9478c`, EXIT0, 10 HTTP. La ejecución adicional `02361d` fue un fallo de edición textual sin modificación efectiva, no otro ciclo funcional.

### Mapa de reutilización para el cierre pendiente
- El decoder extraído conserva la cobertura de `ScheduleBlockApiTest.s19_s27_strictReadQueryAndCursor`: 21 variantes heredadas de formato, JSON cerrado, ámbito, fecha, UUID y query. Regresión real 173 API11 + 3 HTTP nuevos GREEN `961758`; no son 21 pruebas nuevas de feature13.
- La conexión específica de historial prueba formato cerrado `blockChanges/occurredAt`, paginación 21→20+1, terminal exacto20, query desconocida y cursor repetido. Las rutas ID/key prueban su propia precedencia query antes de IDs.
- Pendiente en esta frontera: ejemplos HTTP de privacidad, ausencia key y traducción503, más un cursor de colección ajena para demostrar el parámetro del decoder. Seguridad POST/CSRF sigue perteneciendo a las suites heredadas y al autor de comandos.
- `s25_recoversOriginalMoveByKeyAfterLaterCancellationAndNewAdapter` acredita persistencia y recuperación histórica con otro adaptador, no reinicio de proceso. El reinicio real de @s25 permanece para E2E coordinado.

### Ciclos 27–29: conexión HTTP de errores ya implementados
Cada test se añadió y ejecutó antes del siguiente; los tres fueron inicialmente GREEN y no exigieron producción nueva.
- `s18_missingReceiptKeyReturnsItsDistinct404`: GREEN `8f5af9`, ausencia key propia, código específico, problem+json y no-store.
- `s18_foreignHistoryIsNotAnEmptySuccessfulPage`: GREEN `88796b`, otro Principal frente a historial real; RESOURCE_NOT_FOUND sin recibo/objetivo y fila conservada.
- `s18_historyStorageFailureReturns503WithoutSqlOrReceipt`: GREEN `1e7661`, tabla de cambios renombrada temporalmente sólo en PostgreSQL efímero del test y restaurada en finally; 503 público sin SQL ni recibo, fila conservada. No equivale a provocar fallo de commit HTTP: ese borde lo acredita el test PostgreSQL de cierre de transacción ya documentado.

### Ciclos 30–33 y freeze del paquete
Los cuatro casos siguientes se añadieron y ejecutaron individualmente. Todos fueron inicialmente GREEN, sin cambio productivo:
- `s18_foreignKeyContextPrecedesReceiptAbsence`: `676027`, key real en proyecto ajeno; RESOURCE_NOT_FOUND sin identidad, key ni objetivo.
- `s18_receiptIdCannotCrossTaskWithinOwnProject`: `8d7f80`, contexto propio distinto de la tarea del recibo; BLOCK_CHANGE_NOT_FOUND sin recibo.
- `s17_historyRejectsAnotherCollectionWithItsOwnTimestampField`: `e87018`, cursor estructuralmente válido con occurredAt/contexto correctos y collection=blocks; demuestra conexión del parámetro blockChanges al decoder compartido.
- `s19_anonymousHistoryDoesNotReachQueryValidation`: `bbe1b1`, sesión ausente frente a query inválida; 401 UNAUTHENTICATED y no-store antes de validar query.

Formato Google Java Format 1.31 aplicado sólo a los13 Java de este paquete (incluido BlockController cedido para extracción), seguido de `./gradlew.bat test --tests '*ReadBlockChangesTest' --tests '*BlockChangeQueriesPersistenceTest' --tests '*BlockChangesApiTest' --console=plain`. GREEN `72171d`, EXIT0, 14s. XML `99762b`: 5 aplicación +11 PostgreSQL +17 HTTP =33, cero fallos/errores/omitidos. No es una suite global ni una campaña de mutación. La regresión API11 posterior a la extracción fue GREEN `961758` con173 casos; los cambios posteriores se limitan al nuevo controlador y sus tests, aparte de formato.

### Cobertura y límites finales para revisión
- @s16: aplicación `s16_emptyHistoryKeepsContextAndHasNoContinuation`, `s16_exactlyTwentyReceiptsAreTerminalAndKeepTheirOrder`, `s16_twentyOneReceiptsUseTheLastServedPositionForContinuation`; SQL orden/limit/cursor estricto; HTTP vacío, exacto20 y21 en dos páginas sin duplicados. Sin historial ficticio de creación.
- @s17: decoder único compartido con API11 y sus21 variantes estrictas (evidencia heredada, no matriz nueva). HTTP13 añade conexión collection/occurredAt, query desconocida y cursor repetido. No se ha repetido cada forma de JSON inválido en la ruta nueva.
- @s18: PostgreSQL real read-only/RR, contexto antes de buscar recibo, ausencia ID/key diferenciada, errores SQL y error tras commit traducidos. HTTP conecta DTO7/Block9, contexto ajeno, aislamiento entre tareas, ausencia ID/key, SQL503, problem+json/no-store. El fallo al finalizar la transacción se inyecta en el test del adaptador; no se afirma un segundo ensayo HTTP de ese mismo fallo.
- @s19 y seguridad heredada: conexión GET anónimo antes de query en HTTP13. Origen/CSRF/cuerpos y comandos pertenecen a API11 y a las suites de comandos13 del otro autor; no se atribuyen a estos17 HTTP.
- @s25: recuperación del recibo original por key tras cambio posterior y otro adaptador, con PostgreSQL conservado y sin nueva escritura. Reiniciar un adaptador NO reinicia el backend: reinicio real/pérdida de ACK queda para E2E.
- Fixtures persistentes: bloques diferentes con cancelaciónv2 y proyección metadata coherente; el caso histórico por key contiene movimientov2 y cancelaciónv3. No se siembran eventos irrelevantes; no se acredita atomicidad de comandos con estas lecturas. Conteos y read-only cubren ausencia de escritura de las lecturas ejercitadas.
- Puerto/caso de uso/DTO compartido no resuelven catálogo ni reloj al recuperar. ApplicationConfiguration y fixtures de contexto fueron integrados por core (23GREEN d7fbec), no editados por este autor.
- Sin fuentes frontend, cambios de configuración, Git, mutación ni limpieza de rutas protegidas. Move continúa congelado y aprobado por separado. Feature13 sigue in_progress; este freeze requiere judge.

### Identidad del corte congelado
- `backend/src/main/java/com/apptolast/organization/application/ReadBlockChanges.java` SHA256 `80d921ce987d0c1c54ae8deb471bde7e27435c6ab923a6eea83d5c8326f020ce`
- `backend/src/main/java/com/apptolast/organization/application/ReadBlockChangesUseCase.java` SHA256 `766c08d3fe8956cbaf7cfa1bebfdc293307ad2f2806b5745a4da905162a0bf71`
- `backend/src/main/java/com/apptolast/organization/application/BlockChangeQueries.java` SHA256 `f3114eec8a003c9d8afdbef1bea23587adb28895f7cbd60150c68dab9e238be3`
- `backend/src/main/java/com/apptolast/organization/application/BlockChangeNotFoundException.java` SHA256 `a783f260056a5faab51e2a1e41405f2169fac979feeb8076960bbeeb05253e61`
- `backend/src/main/java/com/apptolast/organization/domain/BlockChangePosition.java` SHA256 `df31d26a0af87a2d6a0bfc153eb830fbe9061e76bd5498eda1609a79646d9921`
- `backend/src/main/java/com/apptolast/organization/domain/BlockChangePage.java` SHA256 `16979fcac422eda01a9407dd80368bd0147c7ab7a32c8edcc34ac72b90419131`
- `backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresBlockChangeQueries.java` SHA256 `36b60d1fdccda6d020c74a8c6442a5bcf183aa6563d09b675ca78fa35b9ae257`
- `backend/src/main/java/com/apptolast/organization/adapter/http/BlockChangesController.java` SHA256 `f34effa01cd21af4de264b169faf30cacfb0dec8a30eb8380b7f6ce72ecef483`
- `backend/src/main/java/com/apptolast/organization/adapter/http/BlockCursor.java` SHA256 `8217beb6b26eb046d70cf66d5e142121e11f5673dc2defee7f7afa2c87e0e630`
- `backend/src/main/java/com/apptolast/organization/adapter/http/BlockController.java` SHA256 `05d02bd0118e23ab3cf34b3c07324622669725f32986dfd1685f1907a87dbbe7`
- `backend/src/test/java/com/apptolast/organization/application/ReadBlockChangesTest.java` SHA256 `0b25bea0303c53575c2226bce595b40c9c32369a6dff97dba7ed76be12be9c75`
- `backend/src/test/java/com/apptolast/organization/adapter/persistence/BlockChangeQueriesPersistenceTest.java` SHA256 `57673e691265ee3a2ce0555d51f8462bb0cc84a55cbc71bd2934483d75a64b5a`
- `backend/src/test/java/com/apptolast/organization/adapter/BlockChangesApiTest.java` SHA256 `bbd654d1e2c7eae0541c1726f35caa4fd34ae2ca8295f5708b1a8a8d7275db12`
