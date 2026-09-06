# TDD backend reschedule

Contrato13 aprobado d1ff609; base init78050 vigente sin cambios productivos posteriores. Ownership backend/src y esta bitácora; sin PIT, commits ni cambios de frontend/config. Ponytail full/Caveman lite y rol tdd_craftsman. Focales Gradle autorizados por coordinador, único ejecutor; init global al freeze.

## Ciclo1 — @s1, estado inicial puro

BlockStateTest.s1_originalCreationDefinesInitialStateWithoutNewFacts exige bloque original, revisión1, planned y updatedAt=createdAt. RED2136f0: símbolo BlockState ausente. Mínimo record y factoría inicial conservan la instancia original sin fecha/evento nuevos. GREEN e93865,1test. Aún no acredita HTTP/migración/persistencia.

## Ciclo2 — @s1, lectura HTTP/PG inicial

RescheduleApiTest.s1_readsOriginalStateWithoutMaterializingOrChangingFacts usa fila original real y compara respuesta cerrada/ETag/no-store y tablas antes/después. RED c96651/2839c6: ruta ausente respondió500 del fallback global, esperado200. RescheduleController reutiliza ReadBlocksUseCase y BlockState inicial. GREEN c9f904:2tests (1HTTP+1dominio). Sin migración ni proyección todavía; siguiente ciclo exige estado persistido.

## Ciclo3 — @s3/@s4, proyección cancelada y revisión grande

RescheduleApiTest.s3_s4_readsCancelledProjectionWithExactLargeRevisionAndLastBlock. RED7bd336: tabla block_projections ausente. Migración aditiva V12 con referencia a creación y estado/revisión/fecha; ReadBlocksUseCase/BlockQueries añaden lectura state reutilizando bean existente. GREENfcfead:3tests. Fixturesbackend TRUNCATE incluyen explícitamente la nueva tabla dependiente; noCASCADE ni cambios de migraciones anteriores. No hay escritor de13 aún.

## Ciclo4 — @s2/@s3, estado movido frente a recibo original

RescheduleApiTest.s2_s3_projectionDoesNotRewriteCreationReceipt. RED05a628: faltan columnas temporales en proyección. V12 añade sólo tiempos/zona/offsets/duración opcionales; lectura reconstruye campos vigentes y conserva identidad/createdAt del original. GREEN18eb2f:4tests; by-request continúa original y tabla planned_blocks exactamente intacta. Campos opcionales permiten cancelación inicial sin copiar intervalo histórico. Constraints adicionales y escritores se verificarán en sus ciclos.

## Ciclo5 — @s18, ausencia privada

RescheduleApiTest.s18_missingBlockHasSpecificProblemWithoutDisclosingOtherResources. RED1cf8c9: BlockNotFoundException no tenía handler fuera de BlockController. Handler compartido en ApiErrors; GREENcce65f:5tests, BLOCK_NOT_FOUND vs RESOURCE_NOT_FOUND. Se retirará duplicado local de11 en frontera de refactor con regresión11, sin cambiar DTO/error.

## Ciclos6/7 — @s6/@s12/@s13, cancelación pura y precedencia

BlockStateTest.s12_s13_cancellationPreservesLastBlockWhenClockMovesBack: RED169cad método ausente, GREENa0ff92; transición conserva Block9 y acepta reloj anterior sin mutar estado previo. Después s6_revisionStateAndExhaustionProtectCancellationInOrder:5RED301fda por ausencia de guardas, GREENc6e6cb:7dominio. Revisión, cancelled y máximo se evalúan en ese orden; sin wrap.

## Ciclo8 — @s12/@s13/@s26, primera cancelación durable

RescheduleApiTest.s12_s13_s26_cancelCommitsReceiptProjectionAndEventWithoutPreference. RED453416/f8708e: POST no implementado. CancelBlock/BlockEditing coordinan callback con Clock1 truncadoMICROS; BlockMutation/BlockChangeReceipt/BlockChanged separan estado, reciboHTTP7 y payload13 con intervalos4. V12 añade block_changes como historial/recibo, sin copiar creación. Adaptador bloquea contexto/preferencia existente/filaoriginal; escribe las tres partes. GREEN79e5b0:12tests (7dominio+5HTTP). Este corte todavía no implementa replay, parsing estricto, rowcounts ni lectura activa11/12; siguientes ciclos los exigirán. FixturesTRUNCATE incluyen ambas tablas nuevas explícitas.

## Ciclo9 — @s15, replay antes de revisión/reloj

RescheduleApiTest.s15_cancelReplayReturnsOriginalReceiptBeforeRevisionAndClock. RED38627b/51835d: segundoPOST500 en vez de200. Recibo persistido se reconstruye sin reloj ni catálogo; BlockChangeConfirmation separa201/200 conservando cuerpo/Location. GREENb94709:13tests. Todavía falta validar identidad/tipo de key y releer tras lock en carrera, cubiertos por próximos ciclos; no se declara idempotencia completa.

## Regresión de configuración tras ciclo9

Foco de contextos existentes REDafc3f1:18fallos/22 por bean BlockEditing ausente (causa confirmada9877e8). Dos fixtures añaden sólo puerto controlado; GREEN27cc46:22/22. No se debilitan aserciones operacionales ni capacidad. La nueva operación tendrá su caso de wiring propio antes de freeze.

## Ciclo10 — @s5/@s19, cabeceras de cancelación y precedencia

RescheduleApiTest.s5_s19_cancelHeadersAreValidatedInContractOrder,16variantes: query e IDs antes de If-Match, ausente428, débil/comodín/lista/repetición/otroID/cero/cero inicial/overflow400, key ausente/inválida/repetida400. RED689e98:16/16 fallan. Controller recibe headers sin conversión MVC prematura; reutiliza identifier/invalid de11 con visibilidad package-private. GREEN2fe2fe:29tests (22HTTP y7dominio), cero fallos/errores. Estado inicial, proyecciones, recibo original y cancelación/replay previo conservados en este foco.

## PAUSA DE AUTORÍA — reparto explícito del usuario, 2026-09-06

Root informó colisión remota de dos coordinadores y después autorización «Paralelamente tanto tu como Claude sin pisaros»: Codex frontend/pruebas, Claude Code backend. Terminado sólo el ciclo activo en GREEN. No iniciar otro ciclo, integrar remoto, hacer commit/push, formatear globalmente ni limpiar. Root preservará este WIP en rama de respaldo; no debe incorporarse a main como backend completo13. Última sesiónGradle96026 terminó EXIT0; ninguna sesión de pruebas pendiente. No PIT ni init completo de esta feature ejecutados.

### Entrega parcial exacta

- Nuevas fuentes: adapter/http/RescheduleController; application/BlockChanged,BlockEditing,BlockMutation,CancelBlock,CancelBlockUseCase; domain/BlockChangeConfirmation,BlockChangeReceipt,BlockState,BlockStateException.
- Migración nueva V12__block_changes.sql: block_projections y block_changes. No se tocaron migraciones previas ni hechos planned_blocks. No contiene todavía todos los constraints definitivos.
- Fuentes compartidas: ApplicationConfiguration, ApiErrors, BlockController (visibilidad de dos helpers), PostgresBlockStore, BlockQueries, ReadBlocks, ReadBlocksUseCase. No se tocó publicador ni Today productivo.
- Tests nuevos: RescheduleApiTest22 casos y BlockStateTest7. ApplicationWiringTest/ProjectStateConfigurationTest incorporan sólo BlockEditing a fixtures tras RED real;22contextosGREEN27cc46.
- Quince fixturesbackend históricos incorporan block_changes,block_projections al TRUNCATE explícito de planned_blocks; RescheduleApiTest usa la misma lista. NoCASCADE ni borrados de workspace.

### Límites que NO deben confundirse con implementación completa

Sólo lectura /state inicial/movida/cancelada, estado puro de cancelación, cancelación feliz sin preferencia y replay secuencial de cancelación, más cabeceras de ese POST están comprobados. Pendientes: JSONbody estricto de cancelación (aún no lo valida), validaciónquery/UUID de /state, erroresHTTP de guardas de estado, validación de identidad/tipo/cuerpo de replay, segundo replay tras espera y carrera41, rowcounts/rollback/fallosfintransacción, guards/constraints de datos reconstruidos, move/preview, consultas de recibos/historial, lectura vigente11 y Today12, presupuesto/solapes/mutexconcurrentes, octava ruta/validaciónoutbox, wiring operacional nuevo, refactor/formato/regresión completa. Handler BLOCK_NOT_FOUND local11 sigue duplicado con el compartido; refactor pendiente. No hay token/PIT scope nuevo preparado. No congelar este WIP como producto desplegable.

La trazabilidad anterior registra únicamente lo observado en cada ciclo; no equivale a cobertura completa de un escenario con varias ramas. El relevo puede reutilizar ideas/testoráculos revisados, pero debe comparar su implementación independiente y conservar su propia evidencia TDD.

## Reanudación Codex — worktree backend aislado

Usuario detuvo Claude Code y asignó todo el desarrollo a Codex. Base HEAD5f99b6a incorpora checkpoint y merge documentado por raíz. Se conserva esta evidencia parcial, sin presentar29pruebas como feature completa. Dependencias instaladas frozen mediante scripts/project.mjs install:638648 EXIT0. JDK25.0.1 disponible. Inventario004b3c: sóloV1–V12; V12 es la única migración de13 en este worktree y no hay otro backend publicado incorporado. Se usará exclusivamente BD efímera Testcontainers para verificarla, no producción.

Init39501 iniciado por arnés: lint5fc908 detectó formato Java pendiente del checkpoint; compilaciónmain/test completó. Suite sigue en ejecución al escribir esta frontera. No se inicia TDD nuevo antes de resolver baseline. Huecos pendientes permanecen los de la entrega parcial anterior.

Baseline completo del checkpoint: Gradle c8e348 BUILD SUCCESSFUL2m16; XML11615c registra1444tests0fallos/errores/skip en59clases. Frontendbaseline9a3da3:1373/1373en24archivos y17scripts verdes. Init9a3da3 EXIT1 exclusivamente por Spotless. Corregido formato estándar con94cae3spotlessApply EXIT0 (sin comportamiento nuevo). Se repite init antes del siguiente ciclo TDD.

Init repetido1991/d72c00 EXIT0: lint corregido, backend completo2m18 y frontend1373/1373,17scripts. A partir de aquí sólo focales del nuevo TDD hasta frontera global coordinada.

## Ciclo11 — @s19, estructura antes de replay

Test individual s19_cancelRejectsExtraBodyBeforeReturningExistingReceipt: primero confirma cancelación, después reusa key con objective extra y exige400UNKNOWN_FIELD sin nuevo recibo/proyección/evento. RED95bc18 devolvía replay en lugar de400. Controller recibe raw opcional para conservar precedencia y valida campos antes del puerto. No se atribuye aún validación completa de JSON; próximos oráculos cubrirán raíz/ilegibilidad.
GREEN ciclo11 f5b122 (1oráculo HTTP real).

## Ciclo12 — @s19, raíz JSON de cancelación

Test individual s19_cancelRejectsArrayRootWithoutWriting. REDcefe98:array[] provocaba cancelación201. Una guarda isObject antes de iterar campos produce VALIDATION_ERROR body/INVALID_TYPE y ninguna escritura. Foco conjunto de regresión13 en curso al escribir esta entrada.
GREEN ciclo12:2c1d10,31casos(24HTTP+7dominio). Ventana Gradle cedida al autor publicador, archivos de su propiedad congelados por este autor; no Spotless global concurrente.

## Ciclo13 — @s19, JSON ilegible

Test individual s19_cancelMalformedJsonUsesSharedProblemWithoutWriting exige cuerpo problem cerrado exacto y cero escrituras. RED9e4eb2/1a3272:respuesta500. ApiErrors reutiliza el handler malformed para JsonProcessingException de adaptadores. GREEN4db2671test. Pendientes cuerpo ausente/blanco y tokens sobrantes, no se presume su cobertura.

## Ciclo14 — @s19, cuerpo ausente

Test individual s19_cancelMissingBodyIsMalformedAfterValidHeaders: RED1af49e por500 ante raw ausente. Guarda raw null/blanco produce JsonParseException que reutiliza el problema compartido400. Foco GREEN pendiente al escribir; cuerpo blanco queda rama aún no probada específicamente.

Frontera de colaboración nueva: resume_frontend toma MoveBlock/UseCase/BlockMoving/MoveContext/BlockMoveRequest y tests directos, además PlanBlock.java y BlockRequest.java para reutilización de evaluación/estructura. Este autor no edita esos dos compartidos mientras dure cesión. Mantengo Store/controller/BlockState/migraciones/wiring. Publicador separado mantiene su lista de archivos. Gradle se coordina secuencialmente y sin Spotless global concurrente.
GREEN ciclo14:0b98cc1test. Publicador toma siguiente ventana; MoveBlock tendrá su primera ventana después. Últimas fuentes editadas de este autor: RescheduleController, ApiErrors y RescheduleApiTest, además formato del checkpoint; no migraciones editadas.

Notas de integración para próximos oráculos, todavía NO implementadas: al cambiar GETdetalle11 a proyección planned, cancel/replay y GETstate deben seguir buscando creación original para no convertir cancelaciones en404. ownerBlocks y Today deberán leer proyecciones activas mediante joins conservando original inmutable. La carrera41 sin preferencia requiere tratar colisión de task/key como409 con rollback; un INSERT ON CONFLICT(task_id,request_key) DO NOTHING seguido de comprobación de recibo existente puede distinguir colisión real de rowcount0 por fallo/supresión sin inspeccionar mensajes SQL. Estas son opciones de implementación sujetas al RED correspondiente, no evidencia de que ya funcionen.

## Ciclo15 — @s19, JSON sobrante

Oráculo individual s19_cancelRejectsTrailingJsonWithoutWriting inicialmente GREENc8e7b6. Investigación b50e07/733a40 confirma spring.jackson.deserialization.fail-on-trailing-tokens=true heredado. No producción adicional ni atribución de RED falso.

## Ciclo16 — @s18, query antes de IDs en state

Oráculo individual s18_stateRejectsUnknownQueryBeforeMalformedIds: REDcfec15 por conversión UUID de MVC antes del handler. Parámetros String y validadores compartidos conservan precedencia query→project→task→block antes del puerto. Foco conjunto en curso al escribir.

GREEN ciclo16: 060d45, foco conjunto RescheduleApiTest y BlockStateTest. El siguiente autor Move recibió Gradle para completar su puerto; no se ha ejecutado otra suite global. Los XML focales ya no permanecen tras su ejecución posterior, por lo que no se añade un conteo retrospectivo sin archivo disponible.

## Ciclo17 — @s6, revisión obsoleta HTTP

Test individual s6_cancelStaleRevisionReturnsConflictWithoutWriting usa proyección cancelada revisión2 e If-Match1: RED91ea11 (detalle a9b504, esperado412 frente500). Handler del adaptador traduce BlockStateException a problema412; GREEN2d5bff. Esta frontera sólo prueba BLOCK_CONFLICT; clasificación409 de cancelled/max sigue pendiente del próximo oráculo y no se declara cubierta.

## Ciclo18 — @s19, JSON ilegible antes del recibo histórico

Test individual s19_cancelTrailingJsonPrecedesHistoricalReplay confirma cancelación y repite key/headers con '{} null'. Inicialmente GREEN405f08 por configuración JSON heredada, sin nueva producción. Verifica400 MALFORMED_JSON, mismo estado/único recibo/único evento y ninguna lectura del reloj en rechazo. Se cede Gradle al publicador, sin formato global ni cambios de Java propios durante su ventana.

## Ciclo19 — @s6, cancelación ya efectiva

Oráculo individual s6_cancelCurrentCancelledBlockIsDefinitiveConflict: RED78addd porque devolvía412. Mínimo cambio del handler distingue BLOCK_CONFLICT412 del rechazo de estado409. GREEN921343 junto al caso de revisión anterior. Conserva proyección y cero recibos/eventos.

## Ciclo20 — @s6, BIGINT máximo sin preferencia

Oráculo individual s6_cancelExhaustedVersionWithoutPreferenceDoesNotOverflow inicialmente GREENe39870. Verifica409 BLOCK_VERSION_EXHAUSTED sobre9223372036854775807, sin overflow, preferencia ni escrituras. No nueva producción ni RED atribuido.

## Ciclo21 — @s15, identidad de bloque en replay

Oráculo individual s15_cancelKeyCannotRecoverReceiptForAnotherBlock confirma A y usa su key contra B de la misma tarea con intervalo posterior separado. RED7a4664/260e45: devolvía200 con recibo ajeno a B. Store compara blockId antes de replay y el adaptador traduce BlockIdempotencyConflictException a409 compartiendo la forma11. GREEN271025 con RescheduleApiTest y BlockStateTest completos. Aún pendiente comparar kind/intención de movimiento y reconsulta tras lock; no se declaran cubiertos por este caso.

XML f531b2:33 HTTP +7 BlockState,40 casos,0fallos/errores. Ventana cedida al publicador tras guardar evidencia.

## Ciclo22 — @s8, movimiento nominal HTTP y PostgreSQL

Oráculo individual s8_moveCommitsNewIntervalAndPreservesOriginalCreation: RED5baca7 por ruta ausente. Integra MoveBlock en configuración, helpers sintácticos11 con visibilidad package sin cambiar lógica, ruta de confirmación y callback BlockMoving en Store. Proyección almacena destino completo; persistChange reutiliza la escritura de recibo/outbox de cancelación. GREENa8b747 con regresión HTTP. Creación original intacta, revisión2, Location y único evento/recibo con reloj único verificados. Replay de movimiento, rowcounts, consultas activas y carreras siguen pendientes; esta evidencia es nominal.

## Ciclo23 — @s7, preview HTTP sin escrituras

Oráculo individual s7_movePreviewExcludesOwnIntervalAndDoesNotWrite: REDac4d27 por ruta ausente. Ruta usa puerto y DTO preview11, ETag de revisión comprobada y offsets resolubles null. Store toma disponibilidad SHARE y bloque SHARE mediante callbacks. GREENf8ec0a en RescheduleApiTest completo; verifica diez campos, objetivo original, cero reserva previa propia en plannedSeconds y ausencia de proyección/recibo/outbox. No se declara probado aún snapshot concurrente ni todos los errores HTTP de preview. Se cede Gradle al autor Move para cierre de sus oráculos. Contextos sintéticos necesitarán comprobar su nuevo puerto BlockMoving antes de la regresión final.

## Regresión de contexto tras bean MoveBlock

El autor Move ejecutó los dos contextos existentes junto su cierre:4a20fc/47f474 confirmó18 fallos por falta de BlockMoving (15 wiring y3 variantes válidas de ProjectState), mientras61 pruebas de núcleo compartido pasaban. Se añade únicamente ese puerto simulado a ambos fixtures, sin debilitar aserciones. GREENb9736d con ApplicationWiringTest y ProjectStateConfigurationTest completos; no se repite init global. Próxima prueba operativa específica del bean MoveBlock queda pendiente, aunque HTTP ya lo utiliza realmente.

## Ciclo24 — @s15, replay de movimiento antes del negocio

Oráculo individual s15_moveReplayPrecedesRevisionPreferenceAndClock: RED3bba47, la ruta volvía al chequeo de revisión en lugar de recuperar el hecho. Consulta durable antes de bloquear disponibilidad/ejecutar callback; GREEN588c4b. Recupera cuerpo y Location idénticos tras cambiar revisión/zona de disponibilidad, sin reloj ni otra proyección/evento/recibo. Comparación de kind y de intención distintas, y segunda consulta tras espera, permanecen pendientes de sus oráculos. Gradle cedido al autor de lecturas para su primer puerto/app.

## Ciclo25 — @s15, key de movimiento usada para cancelar

Oráculo individual s15_cancelRejectsKeyOfMovementOnSameBlock: RED608642/15b276, faltaba comparación de kind. Cancel replay exige CANCELLED además de identidad; GREEN023e09. Verifica409 IDEMPOTENCY_CONFLICT y conservación de proyección/único recibo/evento, sin reloj.

## Ciclo26 — @s15, comparación sintáctica estable de movimiento

Oráculo individual s15_moveReplayDoesNotAliasTextualZoneIntentions: REDae3704. Comparación del BlockRequest normalizado completo del after durable con la intención recibida, conservando objetivo inmutable; exige kind RESCHEDULED antes de acceder a after. No catálogo, DST ni reloj. GREEN527e75 en familia s15 completa. UTC y Etc/UTC siguen intenciones textuales distintas aun con mismos instantes. Reverse-kind y otras variaciones aún pueden añadirse inicialmente verdes; no se afirma que este único vector pruebe todas por separado. Gradle cedido al autor de lecturas para20/21.

## Ciclo27 — @s18, lectura pública read-only real

Oráculo individual s18_stateUsesAnActualReadOnlyDatabaseTransaction inspecciona SHOW transaction_read_only dentro de doBegin del gestor real PostgreSQL, no un getter del template. RED38cf30: estaba off. State público usa un template propio readOnly y REPEATABLE_READ (elección compatible para snapshot, no requisito nuevo), sin FOR SHARE; lookup interno de comandos mantiene locks y template original. No se cambia flag mutable compartido. GREEN392de4 en RescheduleApiTest completo, incluidos cancel/move/replay. El estado de conexión vuelve a off fuera de lectura. Fallo al cierre read-only aún queda para su oráculo propio.

## Ciclo28 — @s3, detalle vigente frente a hechos históricos

Oráculo individual s3_cancelledBlockHasNoCurrentDetailButRetainsStateAndCreationReceipt: RED63e066. GET detalle utiliza state público y excluye cancelled; comandos validan contra original interno, preservando replay después de cancelación. GREENdd3fb5 con40HTTP. GETstate200 y GETby-request200 original permanecen; ninguna escritura de lectura.

## Ciclo29 — @s3, retirar cancelados del listado vigente

Oráculo individual s3_cancelledBlockLeavesTheCurrentList: REDb2fd05. Consulta de listado filtra cancelados antes del límite21/cursor; no borra creación, recibo ni evento. GREEN00c37b en familia s3. Intervalos movidos en listado, paginación con cancelados y Today siguen siguientes oráculos, no cubiertos por este vacío.

## Ciclo30 — @s3, intervalo vigente en listado

Oráculo individual s3_movedBlockUsesCurrentIntervalInListAndDetail: REDd5c223 al comparar item con after confirmado. Se reutiliza selección SQL CURRENT_COLUMNS/coalesce para state y listado, manteniendo orden createdAt/id y límite21. GREEN0ec59b incluye API11 y API13 completas; detalle conserva el mismo DTO9 vigente sin añadir metadatos de estado.

## Ciclo31 — @s11, liberar intervalo cancelado para creación11

Oráculo individual s11_cancelledIntervalIsAvailableForANewCreation: REDb38b22 porque ownerBlocks seguía contando original cancelado. Selección de propietario reutiliza CURRENT_COLUMNS y filtra planned, conservando owner join y filas originales. GREEN170496 con RescheduleApiTest y ScheduleBlockPersistenceTest: la creación11 puede ocupar intervalo liberado, se mantiene el original y sólo existen un cambio y dos eventos (cancelación+creación). Today y escenarios de movimiento entre días siguen pendientes de evidencia propia.

## Ciclo32 — @s11, excluir cancelación de Today

Oráculo individual s11_todayExcludesCancelledReservations: RED443bbc. Today reutiliza CURRENT_COLUMNS y filtra planned e intersección mediante instantes proyectados, conservando template readOnly RR, owner join y nombres actuales. GREENe868b4 junto TodayApiTest completo. Cancelado deja plannedSeconds0/remaining7200 y ningún nextBlockId sin borrar original ni producir escrituras de lectura. Movimiento entre días queda aún para un oráculo específico, sin atribuirlo a este caso de cancelación.

## Ciclo33 — @s11, movimiento entre agendas de días distintos

Oráculo individual s11_todayMovesReservationBetweenDaysWithoutChangingCreation inicialmente GREENad8a90, sin modificación productiva. Tras mover al día siguiente, Today del día original queda vacío; al avanzar el Clock al destino devuelve exactamente after y plannedSeconds3600/remaining3600. Creación original y único recibo/evento permanecen. Esta evidencia complementa el RED de cancelación del ciclo32, no se presenta como otro RED.

## Ciclo34 — @s4, ETag canónico reutilizable desde UUID mayúscula

Oráculo individual s4_stateRevisionFromUppercasePathCanBeUsedForCancellation: RED5e963f/XMLd26a4b, el ETag copiaba mayúsculas del path y no cumplía la forma canónica requerida por If-Match. Cambio mínimo: serializar UUID ya validado, conservando aceptación heredada11 del path. GREEN469422 con RescheduleApiTest completo; comprueba respuesta canónica y cancelación posterior con ese mismo ETag. No se cambia parser ni contrato de UUID. Se cede Gradle al autor de recibos; rowcounts y carreras siguen pendientes.

## Ciclo35 — @s20, escritura de proyección suprimida

Oráculo individual s20_suppressedProjectionRollsBackTheWholeCancellation usa trigger PostgreSQL BEFORE INSERT que retorna NULL. REDfa6461/XMLae0466: la API respondía201 pese a cero filas de proyección. Comprobación mínima de rowcount exactamente1; GREENda52d9. Conserva original y ausencia de proyección, recibo, evento y preferencia. El trigger se retira en finally exclusivamente dentro de la base de prueba. No se atribuye aún cobertura al rowcount de recibo/outbox.

## Ciclo36 — @s20, escritura de recibo suprimida

Oráculo individual s20_suppressedReceiptRollsBackProjectionAndEvent usa trigger BEFORE INSERT en block_changes. REDde795c/XMLc0b01b: respondía201 sin recibo. Rowcount exactamente1 exige StorageUnavailable y rollback de la proyección ya escrita; GREEN1b008b con los dos s20 de supresión. La ausencia de recibo ganador sigue siendo503; la colisión concurrente real de key requiere todavía el ciclo s41 y no se confunde con esta supresión. Outbox y rechazo de commit pendientes. Se cede Gradle a lecturas ID/key.

## Ciclo37 — @s20, outbox suprimido durante movimiento

Tras integrar V13 (64d5174), los dos s20 existentes pasan GREEN6038d5 antes del nuevo test. Oráculo individual s20_suppressedOutboxRollsBackMovementAndReceipt: RED636d85/XML1edec7, se devolvía201 pese al trigger RETURN NULL. Comprobación de rowcount1 en outbox; GREENdd464c con los tres s20. Movimiento no deja proyección ni recibo parcial y conserva creación/disponibilidad. Todas las escrituras de persistChange ya cuentan filas; la colisión UNIQUE de key aún espera tratamiento y prueba s41.

## Ciclo38 — @s20, rechazo real del commit antes de confirmación

Oráculo individual s20_rejectedCommitRollsBackEveryMovementWrite inicialmente GREEN5309ee junto los cuatro s20, sin modificación productiva adicional. Un constraint trigger PostgreSQL DEFERRABLE INITIALLY DEFERRED rechaza el commit con texto privado. La API devuelve exactamente el problema503/no-store y las siete tablas observadas permanecen idénticas (proyecto, tarea, creación, preferencia, proyección, recibo, outbox). La captura existente de DataAccessException/TransactionException cubre el fallo real al finalizar; no se inventa otro RED. Gradle cedido a lecturas; siguiente propio carreras y recheck durable.

## Ciclo39 — @s21, replay tras esperar el bloqueo del mismo bloque

Oráculo individual s21_waitingCancellationWithSameKeyReplaysWinningReceipt sincroniza dos sesiones PostgreSQL realmente bloqueadas en SELECT del bloque antes de liberar el holder. REDe31446/b832f1 y XMLcd5dfd: respuestas412+201 en vez de200+201. Cancel consulta de nuevo el recibo después del lock y reutiliza la validación de identidad/kind. GREEN1cc5b7 con s21 y familia s15; cuerpo/Location idénticos, revisión2, un recibo/evento y una lectura de Clock. Movimiento aún requiere su recheck equivalente mediante otro oráculo; no se atribuye cobertura de esa ruta.

## Ciclo40 — @s41, colisión real de key entre bloques sin disponibilidad

Oráculo individual s41_distinctBlocksWithSameKeyCollideWithoutPartialChangesOrPreference retiene un advisory exclusivo sólo en fixture; el trigger BEFORE INSERT pide SHARE. Se observan explícitamente dos sesiones PostgreSQL bloqueadas en INSERT INTO block_changes antes de liberar, por lo que ambas ya pasaron los lookups/recheck. REDa1e60b/XML601b34:201+503 en lugar de201+409. INSERT usa ON CONFLICT(task_id,request_key) DO NOTHING y sólo clasifica409 si el rowcount0 tiene recibo ganador durable; sin ganador conserva503. GREEN02d443 con s41, s21 y cuatro s20. Sólo ganador cambia revisión/proyección; perdedor planned/revisión1, originales intactos, un recibo/evento y cero preferencia. Finally libera advisory antes de cerrar workers y retira trigger/función de fixture. No es un lock nuevo de producción. Se cede Gradle a lecturas; recheck de movimiento y otras carreras quedan siguientes.

## Integración @s16 — bean de lectura y contextos existentes

Autor de lecturas observó REDb1c35c/b0d069 del primer GET changes (literal interpretado como blockId sin ruta). Tras su controlador/adapter mínimos, se añade bean ReadBlockChangesUseCase con BlockChangeQueries. Foco14978/973e2c: HTTP pasa y los18 contextos sintéticos fallan por nuevo puerto ausente, causa fe19d3. Se añade sólo fixture BlockChangeQueries a las dos suites, sin cambiar aserciones; GREENd7fbec con23 casos (HTTP1 + wiring15 + configuración7). No se atribuyen aún pruebas de operación de factories nuevas: se agregarán individualmente antes de freeze final. Se congela Java para integración selectiva de handlers aprobados por root.

## Ciclo41 — @s21, replay de movimiento tras el mutex

Oráculo individual s21_waitingMovementWithSameKeyReplaysWinningReceipt observa dos sesiones bloqueadas en disponibilidad antes de liberar. REDfe0188/XMLfeadaa:201+412, faltaba recheck en move. Se reutiliza comparación completa de intención mediante movedReplay antes y después de los locks. GREEN29ad71 con s21/s15. Recibo y Location iguales, una revisión y evento, Clock leído una vez; no se ejecuta negocio para el segundo request.

## Ciclo42 — @s22, dos movimientos compiten por presupuesto entre proyectos

Oráculo individual s22_competingMovesAcrossProjectsRecalculateTheSharedBudget inicialmente GREENf6785b sin cambio productivo. Dos bloques de proyectos distintos parten de otros días; destinos distintos no solapados en lunes tienen capacidad para uno solo. Ambos requests se observan bloqueados en disponibilidad antes de soltar el mutex. Uno confirma201 y el otro409 BUDGET_EXCEEDED con planned/requested/excess3600, sin contar dos veces su intervalo anterior. Originales intactos, una proyección/recibo/evento. Esta prueba usa los handlers compartidos ya integrados y no atribuye aún cobertura a carrera creación/movimiento por solape. Gradle cedido a lecturas.

## Ciclo43 — @s24, Today no mezcla intervalo mientras confirma movimiento

Oráculo individual s24_todaySnapshotKeepsTheWholeIntervalBeforeConcurrentMovement usa TodayQueries real y su callback posterior a leer disponibilidad. Dentro confirma un POST real desde otro thread antes del SELECT de bloques. Incidente f8b78e: nombre local/lambda duplicado en test, corregido sin fuente productiva; no se cuenta RED funcional. Primera ejecución válida GREEN34d736: snapshot mantiene10–11 y nombres/3600 completos, siguiente lectura12–13; creación intacta y único recibo/evento. Executor exterior al read garantiza cierre después de liberar transacción si falla el timeout.

## Ciclo44 — @s24, Today conserva snapshot anterior a cancelación

Oráculo individual s24_todaySnapshotKeepsReservationUntilTheNextReadAfterCancellation inicialmente GREENeeef34 junto ambos s24, sin cambios productivos. Cancelación HTTP confirma entre disponibilidad y SELECT de bloques: consulta abierta mantiene reserva/nextBlock y3600; siguiente consulta queda vacía,0/7200 y sin próximo/cierre. Sólo un recibo/evento procede del writer. Se cede Gradle; preview concurrente y creación/movimiento por solape aún pendientes de sus oráculos.

## Ciclo45 — @s21, movimientos distintos con keys nuevas

Refactor previo de las dos barreras de replay a raceOnRow/assertReplayedOnce: GREENbb3406, sin nueva semántica ni cambio de producción. Oráculo individual s21_distinctMovementKeysCompeteForOneRevision inicialmente GREEN791854: dos destinos distintos y dos keys nuevas, ambos esperando el mutex, producen201+412 BLOCK_CONFLICT. Original intacto, revisión2 y una proyección/recibo/evento. Se acredita esta fila separadamente del replay.

## Ciclo46 — @s21, cancelaciones con keys nuevas sin preferencia

Oráculo individual s21_distinctCancellationKeysCompeteForOneRevisionWithoutPreference inicialmente GREENa599f8. Dos sesiones esperan el mismo bloque y usan keys distintas; responden201+412 BLOCK_CONFLICT, estado cancelled/revisión2, un cambio/evento y creación intacta, sin fabricar disponibilidad. No se confunde con dos cancelaciones de la misma key. Se cede Gradle; quedan ambos órdenes mover/cancelar y misma key entre kinds distintos.

## Ciclo47 — @s21, movimiento confirma antes de cancelación concurrente

Oráculo individual s21_movementWinsBeforeConcurrentCancellationWithNewKey inicialmente GREEN54b4c4. Holder externo del bloque; se observa al movimiento esperando ese bloque (ya retiene disponibilidad), luego se lanza cancelación y se observa su espera en disponibilidad. Al liberar, orden garantizado201 movimiento/412 cancelación, BLOCK_CONFLICT, planned/revisión2, original intacto y único cambio/evento. La barrera es común de test para el orden inverso, sin depender del scheduler ni alterar producción.

## Ciclo48 — @s21, cancelación confirma antes de movimiento concurrente

Oráculo individual s21_cancellationWinsBeforeConcurrentMovementWithNewKey inicialmente GREEN763516. La misma barrera observa cancelación reteniendo preferencia y esperando bloque, luego movimiento esperando preferencia. Respuestas ordenadas201/412 BLOCK_CONFLICT, estado cancelled/revisión2, original y un cambio/evento. No basta el caso inverso para afirmar este orden: queda registrado por separado. Gradle cedido; última fila s21 kind distinto/misma key pendiente.

## Ciclo49 — @s21, misma key entre intenciones concurrentes distintas

Oráculo individual s21_sameKeyForConcurrentCancellationAndMovementIsAnIntentionConflict inicialmente GREEN6134f7. Se fuerza cancelación primero reteniendo bloque y observando movimiento detrás del mutex. Responde201 cancelación/409 IDEMPOTENCY_CONFLICT movimiento; no412 ni replay de otro kind, Clock1 y único hecho cancelled/revisión2 con original intacto. La fila restante de s21 queda acreditada por este caso; el orden contrario de replay de kind ya se verifica en ciclo25.

## Ciclo50 — @s18, fallo al terminar GET state read-only

Oráculo individual s18_stateReadOnlyCompletionFailureReturnsStorageProblemWithoutWrites inicialmente GREENa062aa, sin cambios productivos. Spy del gestor real conserva commit de lectura y lanza TransactionSystemException al finalizar únicamente cuando la transacción observada era readOnly. GET responde problema503 cerrado/no-store, sin texto privado; original intacto y ninguna escritura auxiliar. Complementa el SHOW del ciclo27, que por sí solo no probaba este error. No se usa fallo posterior al commit para alegar rollback de escritura; ese rechazo PostgreSQL previo a confirmar está en ciclo38.

## Integración independiente @s23

Root copió únicamente RescheduleCoordinationTest aprobado (597e10, SHA F8895D…DB7082), sin producción. Foco integrado GREEN50fb09 en este árbol:6 casos, sin fallos, errores ni skips. Autoría y ciclos inicialmente verdes corresponden a progress/tdd_reschedule_coordination.md; no se reetiquetan como ciclos propios. La integración cubre los seis órdenes reservados con proyecto/tarea/preferencia y sus bloqueos reales. Se cede Gradle a lecturas.

## Ciclo51 — @s22, movimiento confirma antes de creación concurrente

Oráculo individual s22_movementWinsDestinationBeforeConcurrentCreation. Primera ejecución alcanzó201/409 pero falló por usar blockId en vez de id del DTO de conflicto heredado11 (XML e8da38); se corrige sólo esa expectativa, sin atribuir RED funcional. Foco GREEN68c2fc. La barrera observa movimiento reteniendo disponibilidad y esperando bloque, y creación esperando disponibilidad; al liberar, creación rechaza BLOCK_OVERLAP del bloque movido. Original intacto y una proyección/recibo/evento. No hubo cambio productivo.

## Ciclo52 — @s22, creación confirmada después del preview y antes de mover

Oráculo individual s22_creationAfterPreviewOccupiesTheDestinationBeforeMovement inicialmente GREEN90a35a. Preview200 del destino libre; creación11 confirma201 en ese destino; movimiento posterior rechaza409 BLOCK_OVERLAP con el id de esa nueva reserva. Creación original intacta, dos originales, ninguna proyección/cambio y sólo outbox de creación. Este orden es secuencial y acredita revalidación después del preview, no una segunda carrera. Se combina con la carrera real inversa del ciclo51 y presupuesto compartido del ciclo42. Producción intacta.

## Ciclo53 — @s24, preview y cancelación concurrente conservan presupuesto coherente

Oráculo individual s24_movePreviewKeepsItsBudgetWhileCancellationWaits. Primer intento5deeaf falló por la observación de estadísticas PostgreSQL dentro de la propia transacción: pg_stat_activity retenía el snapshot inicial y la barrera nunca veía al waiter (XML bac9da). Se limpia exclusivamente el snapshot de estadísticas en cada sondeo del helper de pruebas; no cambia aislamiento ni SQL productivo. Foco GREEN1f0014, sin RED funcional atribuido. MoveBlock y ambos adaptadores reales: tras leer reservas del propietario, cancelación HTTP queda observada esperando la preferencia; primer preview mantiene3600 planificados, libera locks y permite201, siguiente preview ve0. Originales intactos y ninguna proyección del bloque consultado; sólo el writer produce un cambio/evento. El executor exterior garantiza que cualquier fallo libera antes la transacción de preview. La regresión final volverá a ejecutar las barreras que comparten el helper.

## Ciclo54 — wiring operacional de cancelación

Antes de la indicación final de root de reutilizar HTTP nominales, se añadió un único caso operacional reschedule_s12_cancelBeanReachesItsPortInFreshContext: inicialmente GREENa27c6f. Resuelve el puerto de entrada real y comprueba que alcanza BlockEditing en contexto fresco. No se añaden los otros dos casos de wiring: Move/Read quedan acreditados por sus HTTP nominales y contexto integrado d7fbec.

## Ciclo55 — @s19, query de movimiento antes de revisión y replay

Oráculo individual s19_moveQueryPrecedesMissingRevisionAndHistoricalReplay inicialmente GREEN395916. Tras movimiento durable, query desconocida junto con If-Match ausente devuelve400/query; no replay ni nueva lectura de reloj/escritura. Es una conexión de precedencia HTTP, no repetición de la matriz de parseo11.

## Ciclo56 — @s19, Availability-Revision ausente antes de replay

Oráculo individual s19_missingAvailabilityRevisionPrecedesMoveReplay inicialmente GREENea3c77. Resto de headers/cuerpo válido, recibo previo durable:428 PRECONDITION_REQUIRED, sin lectura de reloj ni nuevas escrituras.

## Ciclo57 — @s19, documento JSON inválido antes de replay

Oráculo individual s19_malformedMovementDocumentPrecedesHistoricalReplay inicialmente GREENcf5547. Con headers válidos y recibo previo, dos raíces JSON producen400 MALFORMED_JSON, sin reloj ni cambios adicionales. Usa el contrato de problema existente11.

## Ciclo58 — @s5, revisión obligatoria del preview

Oráculo individual s5_previewRequiresBlockRevisionWithoutWriting inicialmente GREEN125aa1. Destino y disponibilidad válidos, sólo If-Match ausente:428 PRECONDITION_REQUIRED, sin reloj ni materialización/escrituras.

## Ciclo59 — @s2, replay de creación después de dos movimientos

Oráculo individual s2_creationReplayAfterTwoMovesReturnsOnlyTheOriginalFact inicialmente GREEN45175f. Dos movimientos confirmados dejan revisión3; POST11 con la key e intención originales devuelve200, Location original y mismo DTO histórico que by-request antes de mover. No altera la proyección ni lee reloj; un original, dos cambios y dos eventos. No se cambia la comparación de creación11.

## Entrega funcional congelada para judge — 2026-09-06

Formato focal de siete archivos propios GREEN5d8e3f. Regresión integrada GREEN7d770e (53s), XMLfb0109:447 pruebas,0 fallos/errores/skips. Incluye RescheduleApiTest70, RescheduleErrorsApiTest9, RescheduleCoordinationTest6, ScheduleBlockApiTest173, ScheduleBlockPersistenceTest38, TodayApiTest27, lecturas33, Move17, MoveRequest6, Plan24, BlockRequest14, BlockState7, ApplicationWiring16 y ProjectStateConfiguration7. git diff --check EXIT0. No init global ni PIT ejecutados en esta frontera; root integra el corte común antes de esas puertas. Fuentes y tests congelados, sin más cambios productivos desde los ciclos documentados; únicamente formato final.

### Mapa de escenarios y reutilización

Los nombres abreviados RA/ME/MB corresponden a RescheduleApiTest, RescheduleErrorsApiTest y MoveBlockTest. No se atribuye a una prueba una matriz HTTP que no ejecuta.

| Contrato13 | Evidencia concreta |
| --- | --- |
| @s1 | RA.s1_readsOriginalStateWithoutMaterializingOrChangingFacts; BlockStateTest.s1_originalCreationDefinesInitialStateWithoutNewFacts; upgrade V13 independiente aprobado |
| @s2 | RA.s2_creationReplayAfterTwoMovesReturnsOnlyTheOriginalFact; s3_cancelledBlockHasNoCurrentDetailButRetainsStateAndCreationReceipt |
| @s3–4 | RA.s3_movedBlockUsesCurrentIntervalInListAndDetail; s3_cancelledBlockLeavesTheCurrentList; s3_s4_readsCancelledProjectionWithExactLargeRevisionAndLastBlock; s4_stateRevisionFromUppercasePathCanBeUsedForCancellation |
| @s5–6 | RA.s5_s19_cancelHeadersAreValidatedInContractOrder; s5_previewRequiresBlockRevisionWithoutWriting; s6_cancelStaleRevisionReturnsConflictWithoutWriting; s6_cancelCurrentCancelledBlockIsDefinitiveConflict; s6_cancelExhaustedVersionWithoutPreferenceDoesNotOverflow; MB.s6_revisionStateAndExhaustionPrecedeMissingAvailability y s6_availabilityIdentityAndVersionPrecedeCompletedProject |
| @s7–10 | RA.s7_movePreviewExcludesOwnIntervalAndDoesNotWrite; MB.s7_previewExcludesOnlyMovedIdentityAndKeepsOtherTaskReservations, s8_confirmationRechecksClockAfterAValidPreview, s10_unchangedResolvedDestinationPrecedesOverlap, s10_changingOnlyZoneIsEffectiveEvenWhenInstantsStayEqual; ME.previewReportsBothOccurrencesOfAmbiguousStart, moveReportsValidOffsetForAnIncorrectEndOccurrence, previewPreservesValidationErrorForNonexistentLocalTime; BlockMoveRequestTest reutiliza validación de BlockRequest y PlanBlock/ResolvedBlockTime existentes |
| @s9–11 | RA.s8_moveCommitsNewIntervalAndPreservesOriginalCreation; s11_todayMovesReservationBetweenDaysWithoutChangingCreation; s11_todayExcludesCancelledReservations; s11_cancelledIntervalIsAvailableForANewCreation; MB.s11_moveCreatesOneReceiptAndEventWhilePreservingBlockIdentity |
| @s12–13 | RA.s12_s13_s26_cancelCommitsReceiptProjectionAndEventWithoutPreference; BlockStateTest.s12_s13_cancellationPreservesLastBlockWhenClockMovesBack; cancelación no consulta elegibilidad, disponibilidad ni catálogo en su callback |
| @s14–15 | RA.s15_moveReplayDoesNotAliasTextualZoneIntentions; s15_cancelRejectsKeyOfMovementOnSameBlock; s15_cancelKeyCannotRecoverReceiptForAnotherBlock; s15_moveReplayPrecedesRevisionPreferenceAndClock; s15_cancelReplayReturnsOriginalReceiptBeforeRevisionAndClock; MB.s15_confirmedReplayReturnsHistoricalReceiptWithoutClockOrCatalog. Unicidad contextual y separación de tabla de creación se verifican además en V13. No se presenta cada condición de la tabla como un caso HTTP independiente |
| @s16–17 | Entrega independiente lecturas: ReadBlockChangesTest5, BlockChangeQueriesPersistenceTest11, BlockChangesApiTest17; cursor11 compartido mantiene regresión ScheduleBlockApiTest173 |
| @s18 | RA.s18_missingBlockHasSpecificProblemWithoutDisclosingOtherResources; s18_stateUsesAnActualReadOnlyDatabaseTransaction; s18_stateReadOnlyCompletionFailureReturnsStorageProblemWithoutWrites; s18_stateRejectsUnknownQueryBeforeMalformedIds; negativos ID/key/owner/503 y cierre read-only del paquete de lecturas |
| @s19 | RA.s19_moveQueryPrecedesMissingRevisionAndHistoricalReplay; s19_missingAvailabilityRevisionPrecedesMoveReplay; s19_malformedMovementDocumentPrecedesHistoricalReplay y casos individuales de cuerpo cancelación; reutilización seguridad compartida ScheduleBlockApiTest.s19_allRoutesKeepSecurity y s23_replayStillRequiresSecurityAndStructure, JSON s62_unreadableJson/s62_creationUnreadableJson. Esas rutas11 acreditan el filtro común, no nuevas invocaciones explícitas de cada ruta13 |
| @s20 | RA.s20_suppressedProjectionRollsBackTheWholeCancellation; s20_suppressedReceiptRollsBackProjectionAndEvent; s20_suppressedOutboxRollsBackMovementAndReceipt; s20_rejectedCommitRollsBackEveryMovementWrite |
| @s21 | RA.s21_waitingMovementWithSameKeyReplaysWinningReceipt; s21_distinctMovementKeysCompeteForOneRevision; s21_movementWinsBeforeConcurrentCancellationWithNewKey; s21_cancellationWinsBeforeConcurrentMovementWithNewKey; s21_distinctCancellationKeysCompeteForOneRevisionWithoutPreference; s21_sameKeyForConcurrentCancellationAndMovementIsAnIntentionConflict. Cancelación misma key adicional separada |
| @s22 | RA.s22_competingMovesAcrossProjectsRecalculateTheSharedBudget; s22_movementWinsDestinationBeforeConcurrentCreation; s22_creationAfterPreviewOccupiesTheDestinationBeforeMovement (orden secuencial explícito); MB.s22_commitRequiresSpecificBudgetConsentWithoutCountingPriorInterval; ME.moveRejectsOverlapEvenWithBudgetConsent; exclusión/contigüidad y presupuesto core11 reutilizados |
| @s23 | RescheduleCoordinationTest6, integración propia50fb09 y regresión final447; autoría independiente, bitácora específica |
| @s24 | RA.s24_todaySnapshotKeepsTheWholeIntervalBeforeConcurrentMovement; s24_todaySnapshotKeepsReservationUntilTheNextReadAfterCancellation; s24_movePreviewKeepsItsBudgetWhileCancellationWaits |
| @s25 | Lectura durable by-key independiente de outbox y estado posterior en paquete de lecturas; reinicio/ACK perdido corresponde a E2E real del autor aislado, no se atribuye a la suite MockMvc |
| @s26–27 | Paquete publicador independiente205GREENd498ea: PublishOutboxTest, RabbitBrokerPublisherTest.reschedule_s26_routesOriginalThirteenFieldsToDurableQuorumQueue y recuperación heredada11. Mapa y límites exactos en tdd_reschedule_publisher.md |
| @s41 | RA.s41_distinctBlocksWithSameKeyCollideWithoutPartialChangesOrPreference: dos sesiones observadas en INSERT,201/409 y rollback del perdedor; no se confunde con supresión de INSERT sin ganador503 |

La revisión final debe combinar este mapa con los paquetes Move, lecturas, publicador, migración y E2E. Este informe no declara feature done ni mutación superada.

### Hashes del corte propio

| Archivo | SHA256 |
| --- | --- |
| backend/src/main/java/com/apptolast/organization/adapter/config/ApplicationConfiguration.java | F8C886DD468C581A6C3CFED1052A3311B5FC1FA1A7F26DF11EB6B785195349E9 |
| backend/src/main/java/com/apptolast/organization/adapter/http/RescheduleController.java | 2178AF54AA8FCC3F84DE1BA974B6C8D95C7BCCE38D848128058525E0A8DED3D5 |
| backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresBlockStore.java | A0AB035C9C568DE8156066BD10B541E7B619CB1738F24E6F1FA0EF53D7F3022C |
| backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresTodayQueries.java | 81ADDA7169B24B2239A686D4D50E74516F26A95D324083260ABF2C704FF32576 |
| backend/src/test/java/com/apptolast/organization/adapter/RescheduleApiTest.java | 4D270F68A5966BE21FC490F8EEC8DC9EC6733BD6B7F328A58FFB9211BCDE3AAD |
| backend/src/test/java/com/apptolast/organization/adapter/config/ApplicationWiringTest.java | 32284AC740AF88C4A29A436F3BFAE96590123F885BD83998FDBDE23051C3A324 |
| backend/src/test/java/com/apptolast/organization/adapter/config/ProjectStateConfigurationTest.java | FB94CCDAA8C229515895FC1FD5EDD45114B34A24C5A0B9C77B16A84022B14A96 |
