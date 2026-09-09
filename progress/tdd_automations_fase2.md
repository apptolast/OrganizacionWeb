# Feature 30 — automatizaciones, segunda pasada (carril C)

Cierre de los hallazgos del dictamen `progress/carriles/dictamen_f30.md`.
Worktree `C:/Users/vhurt/ow-worktrees/automations`, rama `claude/automations`,
`E2E_WEB_PORT=18094`.

## Alcance acordado en esta sesión

El coordinador retiró el bloqueante 1 (ejecutor de reglas) del encargo por
plazo: es trabajo de horas y a medias no vale. En su lugar queda escrito el
inventario preciso de los nueve escenarios sin oráculo (sección final), para
que quien lo retome empiece con el mapa hecho.

---

## Hallazgo 2 [BLOQUEANTE] — el interruptor no ataba el If-Match vivo ni el instante del cambio

**Contrato:** `features/automations.feature:534` (@s40, fila del interruptor).

**Qué faltaba.** El doble de fetch de `frontend/src/automations.test.tsx:50`
sólo guardaba `{url, method}`: las cabeceras de la petición se tiraban. Y la
ruta PUT se declaraba sin `delay`, así que entre el clic y la respuesta no
existía ningún instante en el que afirmar nada.

**Ciclo.**

1. ROJO por mutación (no se puede escribir el test "antes" de un producto que
   ya existe, así que el rojo se acredita rompiendo la producción):
   - Mutante A — `frontend/src/automations.tsx:320`, `rule.version` sustituido
     por el literal `1`. Con las pruebas nuevas:
     `AssertionError: expected '"1"' to be '"2"'`, 2 tests fallan.
     Con las pruebas anteriores el mutante sobrevivía (era el superviviente que
     el dictamen predecía).
   - Mutante B — interruptor optimista: `setRules` marcando el cambio en el
     clic, antes del `await replaceAutomation`. Con las pruebas nuevas los dos
     tests del interruptor fallan con `Received element is not checked:`.
     Con las anteriores sobrevivía, porque sólo observaban el estado final.
2. VERDE: producción restaurada, 15/15 en `src/automations.test.tsx`.

**Cambios (sólo pruebas).**
- `calls` pasa a `{url, method, headers: Headers}[]` y el doble hace
  `new Headers(options.headers)`.
- `@s40 flips the switch…`: la ruta PUT lleva promesa retenida; antes de
  liberarla se afirma `toBeChecked()` y «Activa»; después, «Inactiva»; se
  afirma `If-Match: "2"` en el primer PUT y, tras un segundo accionamiento con
  su propia respuesta 200, `If-Match: "3"` en el segundo — la versión que el
  servidor acaba de devolver.
- `@s40 puts the switch back…`: misma promesa retenida, con la afirmación de
  que el interruptor sigue marcado durante el vuelo, de modo que el optimismo
  con revert también muere. Añade el `If-Match: "2"` de esa petición.

**Estado: cerrado.**

---

## Hallazgo 6 [ALTA] — el aislamiento por identidad (`key={owner}`) no tenía oráculo

**Contrato:** `features/automations.feature:561-571` (@s43, «no queda ningún
dato de reglas ni simulaciones en memoria de otra identidad»).

**Qué faltaba.** `frontend/src/automations.tsx:147-149` monta
`<AutomationsWorkspace key={owner} />`: el prop `owner` no se usa para nada
más, existe sólo para forzar el remontaje al cambiar de identidad. Ninguno de
los tests cambiaba nunca de identidad, así que borrar la `key` dejaba la suite
verde al 100 %. Es la única feature del proyecto sin el patrón
`view.rerender(<X owner="otro" />)` que ya usan github-connector,
integration-api, webhooks, import-data y calendar.

**Ciclo.**

1. ROJO por mutación: borrada la `key={owner}` de `automations.tsx:148`.
   `@s43 keeps no rule nor simulation of the identity that just left` falla con
   `TestingLibraryElementError: Unable to find role="switch" and name /pausada/i`
   — sin remontaje no se vuelve a pedir la lista y sigue en pantalla la de Ana.
   Antes del test, ese mutante sobrevivía entero.
2. VERDE: producción restaurada, 16/16.

**Cambio (sólo pruebas).** Test nuevo en `frontend/src/automations.test.tsx`:
monta con `owner="ana"` con la regla «Seguimiento», abre el editor, simula y
espera el `role="status"` de la simulación; entonces
`view.rerender(<Automations owner="bruno" />)` con una segunda respuesta de
`/api/v1/me/automations` que devuelve otra regla; afirma que ni la regla ni el
título de la coincidencia ni el editor de Ana siguen en pantalla, que la lista
se ha vuelto a pedir (2 GET) y que no queda nada en `localStorage` ni en
`sessionStorage`.

**Estado: cerrado.**

---

## Hallazgo 11 [MEDIA] — el ámbito de mutación dejaba fuera la integración con el armazón

**Qué faltaba.** `frontend/stryker.automations.config.json` mutaba sólo
`src/automations-api.ts` y `src/automations.tsx`. La feature también añadió
producción en `src/App.tsx` (predicado de ruta, rama del ternario `section`,
rama de render) y en `src/workspace.tsx` (el `RouteLink` con su
`aria-current`), y ninguna configuración invocable las mutaba.
`docs/mutation-testing.md` exige el umbral sobre las líneas nuevas o tocadas.

**Ciclo.**

1. ROJO: añadida al guardarraíl `automations Stryker configuration mutates only
   the feature files` de `scripts/project.test.mjs` la lista de seis entradas y
   la validación por contenido de los cuatro rangos.
   `node --test scripts/project.test.mjs` → `not ok 88 … Expected values to be
   strictly deep-equal`, con los cuatro rangos ausentes de la configuración.
2. VERDE: los cuatro rangos añadidos a la configuración. 94/94.

**Rangos, con la convención de `stryker.ics-calendar.config.json`** (columna
inicial 0-indexada, columna final excluyente, sin el punto y coma final):
`src/App.tsx:47:8-47:51`, `src/App.tsx:55:8-56:30`, `src/App.tsx:84:7-85:40`,
`src/workspace.tsx:128:10-133:22`. El guardarraíl los recorta del fichero real
y comprueba que empiezan por `automations = route`, `automations`,
`automations && username` y `<RouteLink`, y que contienen
`/automatizaciones`, `Automatizaciones`, `<Automations owner={username} />` y
`/automatizaciones`. Así un desplazamiento de `App.tsx` rompe la prueba en vez
de mutar en silencio otra pantalla.

**Fichero compartido tocado** (REGLAS.md §6): `scripts/project.test.mjs`, sólo
dentro del `test(...)` de automations. Punto de conflicto probable en la
integración.

**Deuda de lote anotada, fuera de mi ámbito:** `stryker.external-calendar`,
`stryker.github-connector` y la feature 25 (sin configuración ni destino de
mutación frontend) tienen la misma omisión.

**Estado: cerrado.** No se ejecuta la campaña: el coordinador lo prohibió por
plazo y carga de máquina.

---

## Hallazgo 5 [ALTA] — @s12: la conservación del historial tras el PUT no tenía oráculo

**Contrato:** `features/automations.feature:170-181`. El Given es «una regla
propia versión 1 enabled true **con 2 ejecuciones registradas**» y tres de las
cuatro filas exigen que «las 2 ejecuciones siguen consultables» / «se
conservan».

**Qué faltaba.** Ninguno de los cinco tests asignados a @s12 creaba ejecución
alguna. El único que ejercitaba un `replace` real contra Postgres,
`AutomationWiringTest.s1_s11_s12_s14`, leía el historial después y afirmaba
`isEmpty()` sobre una regla que nunca tuvo ejecuciones: un anti-oráculo que
pasa igual si el replace conserva, borra u orfana el historial.

**Ciclo.**

1. ROJO por mutación: insertada en `PostgresAutomationStore.replace`, justo
   antes del `UPDATE`, una sentencia que borra las filas de `automation_runs`
   de la regla — el modo de fallo que el dictamen describe (replace por borrado
   y reinserción, o una migración que cambiara el `ON DELETE SET NULL` de
   `V28__automations.sql:18`). Resultado:
   `s12_replacingARuleKeepsItsTwoRecordedRunsReadableAndUnchanged() FAILED`,
   `Expecting actual: [] to contain exactly in any order: [AutomationRun[...]]`.
   Producción restaurada, `BUILD SUCCESSFUL`.
2. VERDE: el test nuevo pasa contra el `UPDATE` real.

**Cambios (sólo pruebas), en
`backend/src/test/java/com/apptolast/organization/adapter/config/AutomationWiringTest.java`:**
- Helper `run(owner, rule)` que inserta una ejecución `succeeded` real.
- Test nuevo `s12_replacingARuleKeepsItsTwoRecordedRunsReadableAndUnchanged`:
  crea la regla, le inserta 2 ejecuciones, lee el historial, hace el `replace`
  con `enabled false` y `trigger TaskStatusChanged.v1` (dos de las cuatro filas
  del Examples a la vez), y afirma versión 2, `enabled false` en la lectura y
  que el historial devuelve **los mismos dos objetos íntegros**, no sólo dos
  filas.
- Retirado el `assertThat(runs.read(...)).isEmpty()` posterior al replace en
  `s1_s11_s12_s14`: era el anti-oráculo. Ese test conserva la comprobación de
  historial vacío en la regla recién creada, donde sí significa algo, y pasa a
  borrar con `If-Match "1"`.

**Fuera de alcance, verificado y anotado:** la fila 4 de @s12 (PUT a acción
`NOTIFY_WEBHOOK` hacia endpoint propio) sigue inalcanzable porque
`ApplicationConfiguration` devuelve el stub `(owner, endpointId) -> false` para
`WebhookEndpointLookup` pese a que la feature 25 ya está en `main`. Es cableado
entre las features 25 y 30, no un hueco de oráculo de @s12.

**Estado: cerrado** (salvo esa fila 4, que depende del cableado de 25).

---

# Hallazgo 1 [BLOQUEANTE] — el ejecutor de reglas: inventario para quien lo retome

**No implementado en esta sesión.** El coordinador lo retiró del encargo por
plazo (es trabajo de horas y a medias vale cero). Queda aquí el mapa preciso
para arrancar sin volver a investigar. **La feature 30 no puede cerrarse en
`done` con este hallazgo abierto**: o se construye el ejecutor, o se enmienda
formalmente `features/automations.feature` y `feature_list.json` por la puerta
humana para que 30 declare sólo lo entregado y los nueve escenarios pasen a una
feature nueva.

## Lo que existe hoy y lo que no

Existen y están probados: `CreateAutomation`, `ReadAutomations`,
`ReplaceAutomation`, `DeleteAutomation`, `SimulateAutomation`,
`ReadAutomationRuns`, `AutomationMatcher`, `AutomationRendering`,
`AutomationTemplate`, `AutomationEvent` (con `projectSource`, `taskSource` y
`loopGuardTaskId`) y los adaptadores `PostgresAutomationStore`,
`PostgresAutomationRuns` y `PostgresAutomationEvents`.

**No existe nada que ejecute.** Comprobado en el árbol:

- `AutomationRunStore` (`application/AutomationRunStore.java`) es un
  `@FunctionalInterface` con un único método de **lectura**,
  `page(owner, ruleId, after, limit)`. **No hay puerto de escritura de
  ejecuciones**: `automation_runs` sólo puede poblarse desde las pruebas.
- `grep -rn automation_cursors backend/src` devuelve una sola línea: el
  `CREATE TABLE` de `V28__automations.sql:35`. Ni adaptador, ni puerto, ni uso.
- El único `@Scheduled` de `backend/src/main/java` es
  `adapter/config/WebhookSchedule.java:27`. No hay planificador ni bean de
  worker de automatizaciones, ni la propiedad `app.automations.enabled` en
  ninguna parte de `backend/src/main/resources`.
- `AutomationRunCursor` es el cursor **opaco de paginación del historial**
  (`record ruleId/executedAt/id`). No confundirlo con `automation_cursors`,
  que es el cursor **de eventos** por propietario (`owner_id`, `occurred_at`,
  `event_id`).
- `WebhookEndpointLookup` sigue cableado en `ApplicationConfiguration` al stub
  `(owner, endpointId) -> false`, pese a que la feature 25 ya está en `main`.

## Los nueve escenarios sin ningún oráculo

`features/automations.feature`, sección «Ejecución».

| Esc. | Línea | Qué exige, en una frase | Qué haría falta |
| --- | --- | --- | --- |
| @s15 | 213 | Con `app.automations.enabled` ausente no se lee ni se escribe nada; al habilitarlo el ciclo procesa E1 y E2 desde el cursor en E0 y lo deja en E2 | La propiedad y su bean condicional; el puerto de cursor; el ciclo. Oráculo: cero filas y cursor intacto antes; 2 ejecuciones `succeeded`, 2 tareas y cursor en E2 después |
| @s16 | 222 | La primera regla inicializa el cursor **en el presente**: 40 eventos anteriores no se procesan, sólo E41 | Inicialización del cursor al crear la primera regla (o al primer ciclo sin cursor). Oráculo: exactamente 1 ejecución y 1 tarea, ninguna referencia a los 40, cursor en E41 |
| @s17 | 231 | Orden `(occurred_at, event_id)`; se omiten los `blocked` y los commits tardíos anteriores al cursor | Lectura por tupla con horizonte de gracia (el patrón de `EnqueueWebhookDeliveries.GRACE`, 5 s). Oráculo: `executedAt` no decreciente en E1, E2, E4; `createdAt` de las 3 tareas en ese orden; sin ejecución para E3 ni T; cursor en E4 |
| @s19 | 256 | `CREATE_TASK` crea la tarea **por el caso de uso existente**, con su `TaskCreated.v1` en la outbox, todo en **una sola transacción** | El caso de uso de ejecución más un puerto transaccional que confirme ejecución + tarea + evento + cursor juntos. Oráculo: 1 tarea raíz con `completionCriterion ""` y `estimatedMinutes null`, 1 `TaskCreated.v1` con `aggregateId P` y `payload.taskId`, la fila de ejecución exacta, cursor en el evento, y el log con `ruleId/eventId/outcome/attempt/code` **sin** título ni nombre de proyecto |
| @s20 | 268 | Un fallo de almacenamiento **revierte** ejecución, tarea y evento y registra `attempt 1 / retry / STORAGE_UNAVAILABLE` | Fallo inducido dentro de la transacción y una escritura de la fila `retry` **fuera** de ella. Oráculo: ni tarea ni evento nuevos; 1 ejecución `retry`; cursor sin avanzar |
| @s22 | 294 | Los `retry` se reintentan **antes** que los eventos nuevos y el tercer intento queda `failed` | Cola de reintentos por propietario, leída antes que la cola de eventos. Oráculo: E1 con `attempt 3 / failed / STORAGE_UNAVAILABLE`, E1 antes que E2 dentro del ciclo, E2 `succeeded attempt 1`, cursor en E2, y ningún cuarto intento |
| @s23 | 305 | Dos workers concurrentes ejecutan una regla **como máximo una vez** por evento | Se apoya en `UNIQUE (rule_id, event_id)` de `V28__automations.sql`. Oráculo con dos hilos reales contra Postgres, como `AutomationPersistenceTest.s10_…`: 1 ejecución, 1 tarea, 1 `TaskCreated.v1`, ningún error no controlado |
| @s25 | 326 | Una caída antes de confirmar deja el cursor atrás y la relectura **no duplica** | Simular la caída abortando la transacción tras crear la tarea. Oráculo: cursor previo en el evento anterior; tras el reinicio, exactamente 1 ejecución, 1 tarea y 1 evento para E |
| @s26 | 335 | `NOTIFY_WEBHOOK` encola el evento **original sin transformarlo** en las entregas de la feature 25, aunque el endpoint no esté suscrito a ese tipo | Puerto de escritura de entregas sobre la feature 25 (hoy sólo existe `WebhookEndpointLookup`, y devuelve `false`). Oráculo: 1 entrega pendiente con `eventId`, `eventType` y `body` byte a byte iguales a la outbox; ejecución `succeeded` con `deliveryId` y `createdTaskId null`; ningún evento nuevo; entrega y ejecución en la misma confirmación |

## Los tres escenarios cubiertos a medias

- **@s18** (línea 240): probada la plantilla (`AutomationTemplateTest`), no el
  Then «la tarea creada tiene título exactamente …». Falta la mitad de
  ejecución, incluida la fila del proyecto renombrado antes del ciclo.
- **@s21** (línea 280): probada la **anticipación** en la simulación
  (`SimulateAutomationTest.s32_*`), no el Then «la ejecución de R1 tiene
  `attempt 1`, `status failed`, `errorCode …`». Las cinco filas necesitan
  ejecutor, y dos de ellas (`ENDPOINT_NOT_FOUND`) además la feature 25.
- **@s24** (línea 317): un `Scenario Outline` de cuatro filas apoyado en un
  único test, `AutomationMatcherTest.s24_aDisabledRuleNeverMatches`. Las otras
  tres filas son sobre concurrencia con un PUT y sobre avance de cursor.

Y las filas **NOTIFY_WEBHOOK reales** de @s5, @s12, @s21, @s32 y @s33, hoy
inalcanzables por el stub de `WebhookEndpointLookup`.

## Diseño propuesto (para que la mayor parte se pruebe sin contenedor)

Un caso de uso puro `ExecuteAutomations implements ExecuteAutomationsUseCase`
con `void runCycle()`, y **un solo puerto** `AutomationWork` que concentre lo
transaccional, de modo que un doble en memoria cubra @s15, @s16, @s17, @s18,
@s20, @s21, @s22 y @s24 sin Postgres:

- `List<String> ownersWithRules()`
- `Optional<AutomationCursor> cursor(String owner)`
- `void startCursor(String owner, AutomationCursor present)` — @s16
- `List<AutomationEvent> after(String owner, AutomationCursor from, Instant horizon)` — @s17
- `List<AutomationRun> pendingRetries(String owner)` — @s22
- `void commit(AutomationOutcome outcome)` — efecto, fila de ejecución y cursor en una transacción
- `void skip(String owner, AutomationCursor reached)` — evento sin regla que dispare, @s24

`AutomationOutcome` sería `record(AutomationRun run, AutomationEffect effect,
AutomationCursor reached)` con `AutomationEffect` sellado
(`CreateTask | Notify | Nothing`). Los identificadores de tarea y de entrega los
genera el caso de uso (`UUID.randomUUID()`), como ya hace
`EnqueueWebhookDeliveries`, para que el adaptador quede tonto y el caso de uso
determinista.

Contra Postgres quedan sólo @s19 (atomicidad de la confirmación), @s23
(concurrencia sobre `UNIQUE (rule_id, event_id)`), @s25 (caída antes de
confirmar) y @s26 (entregas de la feature 25): una sola clase de test con
contenedor.

El planificador copia `WebhookSchedule`: un `@Scheduled` que no puede propagar
excepciones, condicionado a `app.automations.enabled`.
