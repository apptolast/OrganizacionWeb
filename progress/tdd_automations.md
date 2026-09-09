# TDD — Feature 30 `automations` (fase 1)

Worktree `C:/Users/vhurt/ow-worktrees/automations`, rama `claude/automations`. Contrato: `features/automations.feature` (@s1–@s43). Puerto E2E 18095. Skills Ponytail full y Caveman lite aplicadas. `.memoria-cache/patterns/` no contiene patrones de testing ni arquitectura en este worktree: se diseña desde las plantillas del repositorio (feature 24, custom_views_fields, export_data, CreateTask/TaskCommit).

Feature en curso: 30 — automations. Escenarios a recorrer en fase 1: @s1–@s14, @s18, @s19 (parcial), @s21 (parcial), @s27 (parcial), @s28–@s43. Diferidos a fase 2 (dependen del worker compartido y de la feature 25): @s15, @s16 (ciclo del worker), @s17, @s20, @s22, @s23, @s24, @s25, @s26 y las filas NOTIFY_WEBHOOK reales de @s5, @s12, @s21, @s32 y @s33.

## Bitácora de ciclos

### Ciclo 1 — @s1, @s3, @s4, @s5 (validación de referencias al crear)

- **Rojo**: `CreateAutomationTest` (3 tests) no compilaba: faltaban `AutomationRule`,
  `AutomationRuleStore`, `AutomationTargets`, `WebhookEndpointLookup` y las cuatro
  excepciones. 29 errores de compilación en `compileTestJava`.
- **Verde**: `AutomationRule` (id, draft, version, createdAt, updatedAt),
  el puerto `AutomationRuleStore`, los puertos de referencias y `CreateAutomation`,
  que valida referencias, captura el instante con `CustomizationTime` (micros) y
  guarda versión 1. `WebhookEndpointLookup` es el puerto de la fase 2: en fase 1 se
  usa un doble que responde «no encontrado».
- **Refactor**: la comprobación de referencias vive en `AutomationReferences`,
  colaborador de paquete, para que la reutilicen reemplazo y simulación.
- Focal verde: `--tests "…application.CreateAutomationTest" --tests "…domain.Automation*"`.

### Ciclo 2 — @s9, @s11, @s12, @s13, @s14 (leer, reemplazar y borrar como casos de uso puros)

- **Rojo visto fallar**: el commit de resguardo `a6f2125` no compilaba.
  `./gradlew.bat test --tests "…application.AutomationRulesTest" …` →
  `Task :compileJava FAILED`, 2 errores: `ReplaceAutomation` implementaba
  `ReplaceAutomationUseCase`, que no existía, y `AutomationRulesTest` usaba un
  `DeleteAutomation` inexistente.
- **Verde mínimo**: los puertos de entrada `ReplaceAutomationUseCase` y
  `DeleteAutomationUseCase`, y el caso de uso `DeleteAutomation`, que delega el
  borrado con `If-Match` en el almacén. Nada más: `ReadAutomations` y
  `ReplaceAutomation` ya estaban escritos por el ciclo anterior.
- **Refactor**: ninguno necesario; el orden estable ya vivía en la constante
  `STABLE_ORDER` de `ReadAutomations` y la precondición en el almacén.
- Focal verde: 29 tests (`AutomationRulesTest` 6, `CreateAutomationTest` 3,
  `AutomationDraftTest` 4, `AutomationEventTypeTest` 1, `AutomationTemplateTest` 15),
  0 fallos.

### Ciclo 3 — @s27, @s28, @s29 (de qué proyecto habla un evento y cuándo se consulta la guarda)

- **Rojo visto fallar**: `AutomationEventTest` (4 tests) no compilaba;
  `compileTestJava FAILED` con `cannot find symbol: class AutomationEvent` y
  `package EventProject does not exist`.
- **Verde mínimo**: `AutomationEvent` (eventId, ownerId, eventType, aggregateId,
  occurredAt, payload) con `projectSource()` y `loopGuardTaskId()`, y el sellado
  `EventProject` con `Known`, `OfTask` y `OfWorkSession`. La *decisión* de dónde
  buscar el proyecto es regla de negocio y vive en dominio; la *búsqueda* será un
  puerto, para que la tabla de @s27 se pruebe sin base de datos.
- **Refactor**: ninguno; el `switch` sobre el tipo ya es la tabla del contrato.
- Focal verde: `AutomationEventTest` 4/4.

### Ciclo 4 — @s27, @s24 (parcial), @s28, @s29 (el evaluador)

- **Rojo visto fallar**: `AutomationMatcherTest` (5 tests), `compileTestJava FAILED`
  por `AutomationEventProjects`, `AutomationLoopGuard` y `AutomationMatcher`
  inexistentes.
- **Verde mínimo**: los dos puertos y `AutomationMatcher`, con `matches` (regla
  activa, mismo propietario, mismo tipo y condición sobre el proyecto resuelto) y
  `loopGuarded` separado de `matches`: la ejecución salta el evento, pero la
  simulación necesita evaluar la coincidencia *e* informar `loopGuarded`, así que
  no podían ser la misma decisión.
- **Refactor**: `projectOf` queda público porque la simulación lo reutiliza para
  la vista previa; `spotlessApply` sobre todo lo tocado.
- Focal verde: 38 tests en las 7 clases `Automation*`, 0 fallos.

### Ciclo 5 — @s6, @s18 (de qué tarea habla un evento)

- **Rojo visto fallar**: dos tests nuevos en `AutomationEventTest`;
  `package EventTask does not exist` y `cannot find symbol: method taskSource()`.
- **Verde mínimo**: `EventTask` (`Known`, `OfWorkSession`) y `AutomationEvent.taskSource()`.
  Hace falta porque @s6 admite `{{task.title}}` en disparadores de sesión, y en esos
  eventos la tarea sólo se alcanza por la sesión.
- **Refactor**: ninguno.
- Focal verde: `AutomationEventTest` 6/6.

### Ciclo 6 — @s30, @s31, @s32, @s33 (simulación en seco)

- **Rojo visto fallar**: `SimulateAutomationTest` (11 tests), `compileTestJava FAILED`
  por `AutomationEventTail`, `AutomationFacts`, `ActionPreview`, `AutomationMatch` y
  `SimulateAutomation` inexistentes.
- **Verde mínimo**: los puertos `AutomationEventTail` (los N no bloqueados más
  recientes) y `AutomationFacts` (nombre de proyecto, título de tarea y si el
  proyecto está completed, siempre vigentes); los DTO `ActionPreview.Task`,
  `ActionPreview.Webhook`, `AutomationMatch` y `AutomationSimulation`; y
  `SimulateAutomation`, que valida referencias con el mismo `AutomationReferences`
  que crear, pide `WINDOW = 100`, filtra por el evaluador y ordena
  `occurredAt, eventId` descendente.
- **Refactor**: `AutomationEventProjects.projectOfWorkSession` pasa a
  `taskOfWorkSession`, porque la tarea de la sesión hacía falta igualmente para
  `{{task.title}}` y el proyecto sale de ella; el doble de `AutomationMatcherTest`
  se reescribió en verde sin cambiar ninguna aserción. `CreateTaskAction.resolvedFailure`
  lleva el veredicto de longitud resuelta al dominio, junto a los límites.
  `AutomationRendering` aísla el render para que lo reutilice la ejecución.
- Focal verde: `SimulateAutomationTest` 11/11, `AutomationMatcherTest` 5/5.

### Ciclo 7 — @s34, @s35 (auditoría por regla)

- **Rojo visto fallar**: `ReadAutomationRunsTest` (5 tests), `compileTestJava FAILED`
  por `AutomationRun`, `AutomationRunStore` y `AutomationRunCursor` inexistentes.
- **Verde mínimo**: `AutomationRun` en dominio (con `ruleId` anulable, que es lo que
  sostiene la guarda de bucles tras borrar la regla), el puerto `AutomationRunStore`,
  el cursor `AutomationRunCursor` vinculado a su regla y `ReadAutomationRuns`, que
  pide `PAGE_SIZE + 1` para saber si hay más y devuelve `nextCursor` null al agotar.
  Un cursor de otra regla es `VALIDATION_ERROR` sobre `cursor`; una regla ajena o
  inexistente es el mismo 404 que en @s11.
- **Refactor**: ninguno.
- Focal verde: `ReadAutomationRunsTest` 5/5.

### Ciclo 8 — @s1–@s14, @s30, @s33, @s34, @s35, @s36 (la API HTTP)

- **Rojo visto fallar**: `AutomationsApiTest` (84 tests), `compileTestJava FAILED`
  con `cannot find symbol: class AutomationController`.
- **Verde mínimo**: `AutomationController` con las siete rutas, `AutomationBody`
  (lectura estricta del cuerpo cerrado), `AutomationView` (formas de salida),
  `AutomationRunCursorCodec` (cursor opaco base64url) y seis manejadores nuevos en
  `ApiErrors`: `UNKNOWN_EVENT_TYPE`, `INVALID_TEMPLATE`, `TARGET_NOT_FOUND`,
  `ENDPOINT_NOT_FOUND`, `RULE_LIMIT` y `AUTOMATION_CONFLICT`.
- **Dos rojos legítimos encontrados por el test, no por inspección**:
  1. PATCH devolvía 500 porque el `@ExceptionHandler(Exception.class)` compartido
     capturaba `HttpRequestMethodNotSupportedException`. Un `@ExceptionHandler`
     local no sirve: la excepción nace en el `DispatcherServlet`, antes del
     controlador. Se resolvió como en `ApiCredentialController` y
     `ExportDataController`: un mapeo explícito de PATCH que responde 405 con
     `Allow`. **No se tocó el manejador global**, para no cambiar el 500 de las
     demás features desde este carril.
  2. Las formas de salida no podían usar `@JsonInclude(NON_NULL)`: @s1, @s30 y @s34
     exigen `condition: null`, `criterionTemplate: null`, `wouldFail: null`,
     `deliveryId: null` y `nextCursor: null` **presentes**. Cada variante es ahora
     su propio record (`TaskAction`/`WebhookAction`, `TaskPreview`/`WebhookPreview`)
     para escribir siempre todas sus claves sin filtrar las de la otra.
- **Decisión de campos de error anotada**: en `action`, un juego de claves
  equivocado señala `action` (así lo fija @s8 para `{ type: CREATE_TASK }` sin
  projectId); en el cuerpo, una propiedad desconocida o duplicada señala `body` y
  una clave obligatoria ausente señala esa clave (@s8, fila `name ausente`); en
  `condition`, ambos casos señalan `condition.projectId` (@s3).
- Los instantes viajan como texto ISO-8601 propio para que la resolución de
  microsegundos no dependa de un ajuste del serializador.
- Focal verde: `AutomationsApiTest` 84/84 y `ArchitectureTest` en verde.

### Ciclo 9 — @s1, @s9, @s10, @s11, @s12, @s13, @s14 (persistencia de reglas, V28)

- **Rojo visto fallar**: `AutomationPersistenceTest` (6 tests) con
  `cannot find symbol: class PostgresAutomationStore`.
- **Verde mínimo**: la migración reservada `V28__automations.sql` con
  `automation_rules`, `automation_runs` (`rule_id ... ON DELETE SET NULL`, único
  `(rule_id, event_id)`) y `automation_cursors`, más `PostgresAutomationStore` y
  `AutomationActionJson` para la columna JSONB.
- **Decisión de concurrencia (@s10)**: el cupo se serializa con
  `pg_advisory_xact_lock(hashtext(owner_id))`. Un `SELECT count(*) ... FOR UPDATE`
  no sirve: no se pueden bloquear filas que todavía no existen. El test lanza dos
  hilos reales contra la última plaza y comprueba que hay exactamente un aceptado,
  un rechazado y veinte reglas.
- **Sobre el cursor**: la PREGUNTA ABIERTA de la propuesta se resuelve creando
  `automation_cursors` en V28, porque la feature 25 no ha entregado V23 ni tabla de
  cursores por consumidor. Queda anotado por si 25 la introduce después.
- Sólo se toca V28. No se ha modificado ninguna migración existente.
- Focal verde: `AutomationPersistenceTest` 6/6 (un único contenedor para todo el carril).

### Ciclo 10 — @s14, @s18, @s27, @s31, @s34 (historial, cola de eventos y hechos vigentes)

- **Rojo visto fallar**: seis tests nuevos en la **misma** clase
  `AutomationPersistenceTest` (para no levantar un segundo contenedor), con
  `cannot find symbol: PostgresAutomationRuns` y `PostgresAutomationEvents`.
- **Verde mínimo**: `PostgresAutomationRuns` (página por regla, `executed_at, id`
  descendente, cursor estricto) y `PostgresAutomationEvents`, que implementa de una
  vez `AutomationEventTail`, `AutomationEventProjects`, `AutomationFacts` y
  `AutomationLoopGuard`: las cuatro leen la misma parcela de datos del propietario.
- **@s14 comprobado de verdad**: tras borrar la regla, su historial deja de ser
  consultable pero la fila sobrevive con `rule_id` nulo y su `created_task_id`, así
  que la guarda de bucles sigue respondiendo true. Es el `ON DELETE SET NULL` de V28
  haciendo su trabajo, verificado contando filas en SQL.
- Todas las consultas van unidas a `projects.owner_id` o a `work_sessions.owner_id`:
  una tarea o sesión ajena no resuelve, que es lo que pide la última fila de @s27.
- Focal verde: `AutomationPersistenceTest` 12/12, un solo contenedor.

### Ciclo 11 — wiring real y punto de extensión de la fase 2 (@s1, @s4, @s5, @s11–@s14, @s33)

- **Rojo visto fallar**: `AutomationWiringTest` (3 tests) con
  `NoSuchBeanDefinitionException`: ningún caso de uso estaba publicado.
- **Verde mínimo**: doce beans en `ApplicationConfiguration` (tres adaptadores,
  `AutomationTargets`, `WebhookEndpointLookup`, el evaluador y los seis casos de uso).
  `AutomationTargets` acepta un proyecto propio **sea cual sea su estado**, porque
  @s4 guarda una regla hacia un proyecto completed y es la ejecución la que falla
  con `PROJECT_COMPLETED` (@s21), no el guardado.
- **Punto de extensión de la fase 2 con su test de contrato**: el bean
  `webhookEndpointLookup` responde false para todo endpoint mientras la feature 25
  no exista, y el test comprueba que por eso ninguna regla NOTIFY_WEBHOOK puede
  guardarse. Sustituir ese único bean por el adaptador real de 25 es todo el cambio
  de la fase 2; ningún escenario de webhook se declara verde por vacuidad.
- `ApplicationWiringTest` sigue en verde tras tocar la configuración compartida.
- Focal verde: `AutomationWiringTest` 3/3, `ApplicationWiringTest` sin regresión.

### Ciclos 12, 13, 14 — la interfaz (@s37, @s38, @s39, @s40, @s41, @s43)

- **Ciclo 12 — cliente de la API**. Rojo: `automations-api.test.ts` (12 tests),
  `Failed to resolve import "./automations-api"`. Verde: el módulo, que valida las
  formas cerradas antes de creérselas, manda `If-Match`, y separa
  `AutomationFieldErrors` (errors[] por campo) de `AutomationConflict` (412).
  Focal verde 12/12, `tsc` limpio.
- **Ciclo 13 — la página**. Rojo: `automations.test.tsx` (15 tests), import sin
  resolver. Verde: `automations.tsx`. **Tres rojos legítimos** salieron del test:
  el estado se pintaba dos veces (un `<span>` y el interruptor) y rompía la
  lectura; «Cargar versión actual» cerraba el editor en vez de recargar la regla
  dentro de él, que es lo que pide @s40; y la carga inicial llamaba a `setState`
  de forma síncrona dentro de un efecto (`react-hooks/set-state-in-effect`), así
  que se reescribió con el mismo patrón `.then()` que ya usa la lectura de
  proyectos. Focal verde 15/15, `tsc` y ESLint limpios.
- **Ciclo 14 — ruta y navegación**. Rojo: `automations-route.test.tsx` (3 tests),
  sin encabezado ni enlace. Verde: rama de ruta en `App.tsx`, miembro de la unión
  `section` y `RouteLink` en `workspace.tsx`, **tras** «API para integraciones»
  como fija la propuesta, con «Hoy» intacto en cabeza.
- **Toque entre carriles que el coordinador debe conocer**: tres tests hermanos
  (`App.test.tsx`, `export-data.test.tsx`, `appearance.test.tsx`) anclaban su
  aserción al **último** enlace del menú. Al añadir una entrada se desplazaron: se
  corrigió el índice en uno, conservando exactamente su intención (el orden de esa
  cola) y añadiendo la nueva entrada al final de cada cadena. Ninguna aserción se
  debilitó. Verificado sin regresión en **los 21 ficheros de test que renderizan
  `App` o `Workspace`**: 793 tests en verde.

### Ciclo 15 — puertas de mutación y evidencia E2E

- Ámbito PIT `automations` en `backend/build.gradle.kts` (dominio, casos de uso,
  controlador, adaptadores y wiring), `frontend/stryker.automations.config.json`
  con umbral 80 y los objetivos `automations-backend` y `automations-frontend` en
  `scripts/project.mjs`, con tres tests nuevos en `scripts/project.test.mjs` que
  fijan el ámbito, la configuración y los ficheros mutados. Los tres pasan.
- En este ciclo no se ejecutó E2E; se hizo después con turno del coordinador (ver
  ciclo 18). **Ninguna mutación se ha lanzado**: esa puerta la abre el coordinador.
- **Tres fallos preexistentes en `scripts/project.test.mjs`** (targets
  `integration_api-backend` / `integration_api-frontend` ausentes de la lista
  blanca y rangos `línea:columna` de `stryker.appearance.config.json` ya
  desalineados con `App.tsx`). Comprobado con `git stash` que fallaban **antes** de
  tocar nada: 69 pasaban y 3 fallaban antes, 72 pasan y fallan los mismos 3 después.
  No son míos, pero **mi inserción en `App.tsx` desplaza aún más esos rangos
  fijados por línea** en las configuraciones Stryker de apariencia, exportación,
  importación, historial e integraciones. Hay que volver a fijarlos en la
  integración; no lo hago desde este carril porque son de sus dueños.

### Ciclo 16 — reasentamiento sobre `origin/main` (7ea682d)

- `git rebase origin/main` intentaba **118 comits**: la rama salió de `01f80ab`
  (`codex/integration-api`), no de main, y main recibió la 24 por *squash*, así que
  git no reconoce esa historia como común. Se abortó y se replayaron **sólo los 20
  comits del carril**: `git rebase --onto origin/main 01f80ab claude/automations`.
- Conflictos resueltos: `project-spec.md` (se conservan las enmiendas de seguridad
  de main, que mi comit de sincronización precedía), `feature_list.json` (feature
  30 vuelve a `in_progress`; es la mía), `progress/gherkin_automations.md` (se
  conservan las decisiones del coordinador del 8/9 añadidas en main) y
  `progress/current.md` en los comits intermedios, reescrito al final.
- **`App.tsx` NO dio conflicto**: ningún otro carril ha aterrizado todavía su
  entrada de navegación en main. La mía es la única añadida; cuando lleguen
  calendario, webhooks y conectores habrá que conservarlas todas.
- **Regresión encontrada y corregida**: mi viejo comit de sincronización borraba de
  `scripts/project.mjs` los tres objetivos `integration_api-*` que main ya tiene.
  Restaurados. Eran la causa de dos de los tres fallos «preexistentes» que había
  reportado: los había provocado mi propio carril, no main. Queda dicho.

### Ciclo 17 — re-fijado de los rangos línea:columna de Stryker

`App.tsx` creció (feature 24 más mi rama de ruta) y `workspace.tsx` ganó entradas,
así que los rangos fijados por línea de cinco campañas apuntaban a código
equivocado. Se re-derivaron leyendo el **fragmento que cada rango seleccionaba en
el comit de referencia de su configuración** y localizándolo en el fichero actual;
cada rango nuevo se valida con el mismo oráculo que usa
`scripts/project.test.mjs` (empieza por el token esperado y contiene la cadena
distintiva). Los 17 rangos validan.

| Configuración | Antes | Después |
| --- | --- | --- |
| appearance | `App.tsx:32:8-32:44` | `App.tsx:33:8-33:44` |
| appearance | `App.tsx:49:16-61:32` | `App.tsx:53:18-65:34` |
| appearance | `App.tsx:78:10-119:7` | `App.tsx:84:10-125:7` |
| appearance | `workspace.tsx:77:10-82:22` | `workspace.tsx:78:10-83:22` |
| appearance | `session-gate.tsx:32:2-52:6` | sin cambio |
| appearance | `use-session.ts:208:0-229:1` | sin cambio |
| export-data | `App.tsx:29:8-29:45` | `App.tsx:34:8-34:45` |
| export-data | `App.tsx:37:8-51:28` | `App.tsx:51:16-65:34` |
| export-data | `App.tsx:54:7-97:7` | `App.tsx:82:10-125:7` |
| export-data | `workspace.tsx:81:10-86:22` | `workspace.tsx:84:10-89:22` |
| import-data | `App.tsx:28:8-28:41` | `App.tsx:29:8-29:41` |
| import-data | `App.tsx:34:8-34:45` | `App.tsx:35:8-35:45` |
| import-data | `App.tsx:45:12-61:32` | `App.tsx:49:14-65:34` |
| import-data | `App.tsx:66:10-119:7` | `App.tsx:72:10-125:7` |
| import-data | `workspace.tsx:89:10-97:22` | `workspace.tsx:90:10-98:22` |
| history | `App.tsx:14:8-14:57` | `App.tsx:31:8-31:57` |
| history | `App.tsx:25:12-31:22` | `App.tsx:59:24-65:34` |
| history | `App.tsx:38:10-62:7` | `App.tsx:92:10-125:7` |
| history | `workspace.tsx:55:10-60:22` | `workspace.tsx:66:10-71:22` |
| integration-api | `App.tsx:35:8-35:55` | `App.tsx:36:8-36:55` |
| integration-api | `App.tsx:43:8-61:32` | `App.tsx:47:12-65:34` |
| integration-api | `App.tsx:64:7-119:7` | `App.tsx:70:10-125:7` |
| integration-api | `workspace.tsx:98:10-105:22` | `workspace.tsx:99:10-106:22` |

Se actualizaron también los tres tests que fijaban esas listas
(`scripts/project.test.mjs`) y el fichero de evidencia
`progress/export_frontend_scope_nodes.json`, del que el test de exportación deriva
su expectativa. `scripts/project.test.mjs`: **75 pasan, 0 fallan** (antes 72/3).

**Fuera de mi alcance, avisado**: los rangos de `use-session.ts`,
`session-gate.tsx`, `appearance-state.tsx` y `customization-state.ts` de las
campañas de importación e integraciones no los toqué; si han derivado por el
trabajo de otros carriles en main, es deriva ajena y sus dueños deben revisarla.
Los replay históricos (`*-replay`, `*.replay`) se dejan intactos a propósito:
describen un estado pasado del código y re-fijarlos falsearía su evidencia.

### Ciclo 18 — @s42 demostrado de verdad sobre la pila real

`E2E_WEB_PORT=18093 pnpm test:e2e e2e/automations.spec.mjs`, una sola pila,
retirada al terminar (contenedores, red y volumen comprobados como eliminados).

- **Primera ejecución: 6 de 7.** Falló el caso del texto al 200 % porque usaba
  `page.addStyleTag`, y la CSP `style-src 'self'` que introdujo la feature 24 la
  bloquea. **El fallo era de mi guion, no de la página**: la CSP hizo exactamente
  su trabajo. Corregido con la técnica que ya usan el resto de auditorías del
  repositorio (`history-ux.spec.mjs`): doblar el tamaño calculado de cada elemento
  mediante su atributo `style` desde `page.evaluate`, que es zoom de texto y no de
  disposición.
- El guion también necesitaba **crear un proyecto** antes de abrir el editor: la
  fixture autenticada limpia las filas del propietario antes de cada caso, así que
  sin proyecto la acción `CREATE_TASK` no tenía destino válido.
- **Segunda ejecución: 7 de 7.** @s37, @s39 y @s40 sobre la pila real; @s42 a 320,
  768, 1280 y 1440 píxeles CSS y con texto al 200 %: sin desplazamiento
  horizontal, controles interactivos de 44 × 44 como mínimo y **cero violaciones
  axe serias o críticas** con `wcag2a`, `wcag2aa`, `wcag21aa`, `wcag22aa` y
  `best-practice`.

Con esto **@s42 pasa a estar cubierto**; la tabla de trazabilidad se actualiza.

## Discrepancia de contrato pendiente de dictamen (@s30 vs @s32)

@s30 dice que cada coincidencia contiene «exactamente eventId, eventType, occurredAt
y preview» (cuatro campos), pero @s32 exige leer `loopGuarded true` y `loopGuarded false`,
y el @s32 de NOTIFY_WEBHOOK fija la preview como «exactamente { type, endpointId, eventId }»,
sin `loopGuarded` dentro. Los tres enunciados no son satisfacibles a la vez.

Resuelto a favor de **cinco campos en la coincidencia**, con `loopGuarded` al lado de
`preview`, porque la cabecera del `.feature` declara normativo a
`progress/proposal_automations.md` para los DTO cerrados y esa propuesta dice
literalmente «informa `loopGuarded: true` en la coincidencia». Queda anotado para que
el juez confirme o pida corregir @s30; no se ha inventado comportamiento nuevo.

## Estado en curso (nota para el coordinador)

Se ejecutan **sólo pruebas focales con filtro** por la contención de Testcontainers
en la máquina; la suite completa queda para el cierre, con turno del coordinador.

## Trazabilidad @s → test (fase 1)

| @s | Tests que lo cubren |
| --- | --- |
| @s1 | `CreateAutomationTest.s1_storesVersionOneWithServerIdAndMicrosecondTimestamps`, `AutomationDraftTest.s1_trimsUnicodeWhiteSpaceAroundTheName`, `AutomationsApiTest.s1_createsTheRuleAndAnswersWithTheClosedRepresentation`, `s1_theOwnerNeverComesFromTheBody`, `AutomationPersistenceTest.s1_s11_storesEveryFieldOfBothActionShapesAndReadsThemBack`, `AutomationWiringTest.s1_s11_s12_s14_theRealBeansCreateReadReplaceAndDeleteAgainstPostgres` |
| @s2 | `AutomationEventTypeTest.s2_acceptsExactlyTheTwelvePublishedTypes`, `AutomationDraftTest.s2_rejectsUnknownEventTypesBeforeTemplates`, `AutomationsApiTest.s2_acceptsEveryPublishedTrigger` (12 filas), `s2_rejectsAnyOtherTriggerWithoutWriting` (4 filas), `s2_theTriggerObjectIsClosedToo` |
| @s3 | `CreateAutomationTest.s3_s4_rejectsForeignOrMissingProjectsWithoutWriting`, `AutomationRulesTest.s12_s3_replacingValidatesReferencesLikeCreating`, `AutomationsApiTest.s3_acceptsAConditionOverAnOwnProject`, `s3_rejectsAMalformedCondition` (2 filas) |
| @s4 | `CreateAutomationTest.s3_s4_…`, `AutomationsApiTest.s4_aForeignOrMissingProjectIsUnprocessable`, `AutomationWiringTest.s4_s33_theRealBeansRefuseAForeignProjectAndSimulateWithoutWriting` |
| @s5 | `CreateAutomationTest.s5_requiresAnActiveOwnEndpointForWebhookActions`, `AutomationsApiTest.s5_aForeignInactiveOrMissingEndpointIsUnprocessable`, `s5_acceptsAWebhookActionAndEchoesItClosed`, `s5_theWebhookActionIsClosedToo`, `AutomationWiringTest.s5_theWebhookExtensionPointRejectsEveryEndpointUntilFeature25Exists` |
| @s6 | `AutomationTemplateTest.s7_reportsTheFirstTemplateDefect` (filas válidas), `AutomationsApiTest.s6_keepsTemplatesByteForByte`, `AutomationEventTest.s6_s18_theTaskOfAnEventIsNamedDirectlyOrReachedThroughItsSession` |
| @s7 | `AutomationTemplateTest.s7_…` (14 filas), `AutomationDraftTest.s7_collectsOneTemplateErrorPerField`, `AutomationsApiTest.s7_anInvalidTemplateNamesItsField` (6 filas), `s7_reportsOneDefectPerFieldAtOnce`, `AutomationEventTest.s7_projectEventsHaveNoTaskAtAll` |
| @s8 | `AutomationDraftTest.s8_rejectsEachInvalidFieldByCodePoints`, `AutomationsApiTest.s8_rejectsAMalformedBodyNamingTheField` (7 filas), `s8_rejectsOutOfRangeActionFields` (4 filas), `s8_aBodyThatIsNotAnObjectIsRejected`, `s8_aTooLongNameIsRejectedByCodePoints` |
| @s9 | `AutomationRulesTest.s9_s14_theQuotaIsTwentyAndDeletingFreesASlot`, `AutomationPersistenceTest.s9_theQuotaIsTwentyPerOwnerCountingDisabledRules`, `AutomationsApiTest.s9_theTwentyFirstRuleIsAConflict` |
| @s10 | `AutomationPersistenceTest.s10_twoConcurrentCreationsForTheLastSlotLeaveExactlyTwentyRules` (dos hilos reales) |
| @s11 | `AutomationRulesTest.s11_listsOnlyOwnRulesOrderedByCreationThenId`, `s11_readingAForeignOrUnknownRuleIsTheSameNotFound`, `AutomationPersistenceTest.s11_ordersByCreationThenIdAndNeverLeaksAnotherOwner`, `AutomationsApiTest.s11_*` (6) |
| @s12 | `AutomationRulesTest.s12_replacingAlwaysBumpsTheVersionAndKeepsCreatedAt`, `s12_s3_…`, `AutomationPersistenceTest.s12_replacingBumpsTheVersionKeepsCreatedAtAndDemandsTheExpectedOne`, `AutomationsApiTest.s12_replacingBumpsTheVersionAndTheEtag`, `s12_replacingValidatesTheBodyBeforeTouchingTheUseCase` |
| @s13 | `AutomationRulesTest.s13_staleOrForeignPreconditionsLeaveTheRuleUntouched`, `AutomationPersistenceTest.s13_s14_deletingDemandsTheExpectedVersionAndFreesASlot`, `AutomationsApiTest.s13_aMalformedPreconditionIsAValidationError` (4 filas), `s13_aMissingPreconditionIsRequired` (2 filas), `s13_aStalePreconditionIsAnAutomationConflict`, `s13_aTriggerChangeStillNeedsThePrecondition` |
| @s14 | `AutomationRulesTest.s9_s14_…`, `AutomationPersistenceTest.s14_deletingARuleHidesItsRunsButKeepsTheLoopGuard`, `AutomationsApiTest.s14_deletingAnswersWithoutABody`, `s14_deletingAnUnknownRuleIsNotFound`, `automations-api.test.ts` «@s14 deletes with the version» |
| @s18 (render) | `AutomationTemplateTest.s18_rendersPlaceholdersWithTheCurrentValues`, `AutomationEventTest.s6_s18_…`, `AutomationPersistenceTest.s18_readsThePayloadSoTemplatesCanBeResolved`, `s18_s21_readsTheLiveNamesAndWhetherTheProjectIsCompleted`, `SimulateAutomationTest.s30_previewsEveryMatchNewestFirstWithoutTouchingTheRules` |
| @s21 (anticipación) | `SimulateAutomationTest.s32_anticipatesACompletedProjectWithoutRunningAnything`, `s32_anticipatesATooLongTitleAndResolvesItWithoutTruncating`, `s32_anticipatesATooLongCriterion`, `AutomationPersistenceTest.s18_s21_…` |
| @s24 (regla desactivada) | `AutomationMatcherTest.s24_aDisabledRuleNeverMatches` |
| @s27 | `AutomationEventTest.s27_*` (3), `AutomationMatcherTest.s27_*` (3), `AutomationPersistenceTest.s27_resolvesTasksAndSessionsOnlyInsideTheOwnersData` |
| @s28, @s29 | `AutomationEventTest.s29_onlyTaskCreationEventsConsultTheLoopGuard`, `AutomationMatcherTest.s28_s29_theGuardOnlyBlocksCreationEventsOfAutomatedTasks`, `AutomationPersistenceTest.s14_…` (la guarda sobrevive al borrado) |
| @s30 | `SimulateAutomationTest.s30_*` (2), `AutomationsApiTest.s30_simulatesWithoutSavingAnything` |
| @s31 | `SimulateAutomationTest.s31_countsWhatTheTailReturnsAndMayFindNothing`, `AutomationPersistenceTest.s31_readsTheMostRecentUnblockedEventsNewestFirstUpToTheLimit` |
| @s32 | `SimulateAutomationTest.s32_*` (6) |
| @s33 | `SimulateAutomationTest.s33_rejectsForeignReferencesWithTheSameErrorsAsCreating`, `s33_simulatingNeverConsumesAQuotaSlot`, `AutomationsApiTest.s33_*` (3), `AutomationWiringTest.s4_s33_…` |
| @s34 | `ReadAutomationRunsTest.s34_pagesTwentyAtATimeNewestFirstAndClosesWithANullCursor`, `s34_anExhaustedPageEndsWithoutACursor`, `AutomationPersistenceTest.s34_pagesTheHistoryNewestFirstAndOnlyForTheAskedRule`, `AutomationsApiTest.s34_*` (2) |
| @s35 | `ReadAutomationRunsTest.s35_*` (3), `AutomationsApiTest.s35_*` (3) |
| @s36 | `AutomationsApiTest.s36_*` (6: 401, 403 CSRF, 403 origen, 415, 405, 503) |
| @s37 | `automations.test.tsx` @s37 (4), `automations-route.test.tsx` (3), `automations-api.test.ts` @s37 (2) |
| @s38 | `automations.test.tsx` @s38 (3), `automations-api.test.ts` @s38 (2) |
| @s39 | `automations.test.tsx` @s39, `automations-api.test.ts` @s39 (2) |
| @s40 | `automations.test.tsx` @s40 (4), `automations-api.test.ts` @s40 (2) |
| @s41 | `automations.test.tsx` @s41, `automations-api.test.ts` @s41 (2) |
| @s42 | `e2e/automations.spec.mjs` — **ejecutado, 7/7 sobre la pila real**: 320/768/1280/1440 px y texto al 200 %, sin desplazamiento horizontal, 44 × 44 px y cero violaciones axe serias o críticas |
| @s43 | `automations.test.tsx` @s43 (2), `automations-api.test.ts` @s43 |

Diferidos a la fase 2 (worker compartido y feature 25), **sin cobertura y sin
declarar verdes**: @s15, @s16, @s17, @s19, @s20, @s22, @s23, @s25, @s26 y las
filas NOTIFY_WEBHOOK reales de @s5, @s12, @s21, @s32 y @s33.

## Comandos y números

- Backend, siempre filtrado y por clases:
  `backend/gradlew.bat test --no-daemon --tests "com.apptolast.organization.<Clase>"`.
  155 tests propios en verde: `AutomationEventTypeTest` 1, `AutomationTemplateTest` 15,
  `AutomationDraftTest` 4, `AutomationEventTest` 6, `CreateAutomationTest` 3,
  `AutomationRulesTest` 6, `AutomationMatcherTest` 5, `SimulateAutomationTest` 11,
  `ReadAutomationRunsTest` 5, `AutomationsApiTest` 84, `AutomationPersistenceTest` 12,
  `AutomationWiringTest` 3. `ArchitectureTest` y `ApplicationWiringTest` sin regresión.
  `spotlessCheck` limpio.
- Frontend, por fichero: `pnpm vitest run <ruta>`. 30 tests propios en verde
  (`automations-api.test.ts` 12, `automations.test.tsx` 15,
  `automations-route.test.tsx` 3), más 3 en `scripts/project.test.mjs`.
  `tsc --noEmit` y ESLint limpios sobre lo nuevo.
- Regresión de la navegación comprobada en los 21 ficheros que renderizan `App` o
  `Workspace`: 793 tests en verde.
- **Nunca** se lanzó la suite completa, ni `pitest`, ni Stryker, ni E2E.

## Decisiones y límites

- El cupo de veinte se serializa con `pg_advisory_xact_lock(hashtext(owner_id))`;
  contar filas no se puede bloquear de otro modo. Una colisión de `hashtext` sólo
  hace que dos propietarios se turnen.
- `AutomationTargets` acepta el proyecto propio **en cualquier estado**: apuntar a
  un proyecto completed es válido al guardar y falla al ejecutar (@s4 y @s21).
- La guarda de bucles se apoya en `automation_runs.created_task_id` con
  `rule_id ON DELETE SET NULL`: borrar la regla oculta su historial pero conserva
  la guarda, comprobado contra PostgreSQL real.
- V28 crea `automation_cursors` porque la feature 25 no ha entregado tabla de
  cursores por consumidor. Si 25 la introduce, hay que reconciliar.
- La acción NOTIFY_WEBHOOK queda **detrás del punto de extensión**
  `WebhookEndpointLookup`, cuyo bean responde false para todo. Sustituirlo por el
  adaptador real de 25 es el único cambio de la fase 2 en la parte de guardado.
- **Lo que NO está demostrado**: los escenarios de ejecución del worker (fase 2).
  La fase 1 no ejecuta ninguna regla: sólo las declara, las simula y expone su
  auditoría. @s42 sí quedó demostrado en el ciclo 18.
