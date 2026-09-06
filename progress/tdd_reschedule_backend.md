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
