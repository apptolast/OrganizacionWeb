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

## Trazabilidad

(mapa @s → test al cierre)

## Comandos y números

(al cierre)

## Decisiones y límites

(al cierre)
