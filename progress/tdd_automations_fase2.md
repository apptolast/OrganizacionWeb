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

# Hallazgo 1 [BLOQUEANTE] — el ejecutor de reglas: SESIÓN DEL 10 DE SEPTIEMBRE

> Lo que sigue **sustituye** al estado «abierto» del inventario que hay debajo.
> El inventario se conserva íntegro porque sigue siendo el mapa del escenario a
> escenario; esta sección dice qué se ha construido y qué queda.

## Lo construido

Producción nueva, toda en el carril de automatizaciones:

| Fichero | Qué es |
| --- | --- |
| `domain/AutomationCursor.java` | El cursor **de eventos** por propietario, tupla `(occurredAt, eventId)` con `precedes`. No confundir con `AutomationRunCursor`, que pagina el historial de una regla |
| `application/AutomationCandidate.java` | Una fila del outbox como la ve el worker: evento, `blocked` y **las ejecuciones ya registradas para ella** |
| `application/AutomationEffect.java` | Sellado: `None`, `CreateTask`, `Notify` |
| `application/AutomationOutcome.java` | `(AutomationRun run, AutomationEffect effect)` |
| `application/AutomationCommit.java` | `(owner, reached, outcomes)`: todo lo que produce un evento, para una sola confirmación |
| `application/AutomationWork.java` | El puerto único: `ownersWithRules`, `cursor`, `startCursor`, `after`, `commit`, `record` |
| `application/ExecuteAutomations.java` | El motor |
| `adapter/config/AutomationSchedule.java` + `AutomationConfiguration.java` | El `@Scheduled` condicionado a `app.automations.enabled` |

**Migraciones: ninguna.** Comprobado antes de empezar: `automation_cursors`
ya existe desde `V28__automations.sql:35` y `automation_runs` tiene el
`UNIQUE (rule_id, event_id)` que @s23 necesita. V37–V39 quedan libres.

## Dos desviaciones del diseño propuesto, razonadas

1. **Sin ventana de gracia y sin `horizon` en el puerto.** El diseño la copiaba
   de `EnqueueWebhookDeliveries.GRACE`. Al leer @s17 de cerca, su Given incluye
   «un evento T con `occurred_at` anterior a C que confirma después de leer el
   cursor» y su Then dice «no existe ejecución **para T**»: el contrato acepta
   explícitamente que un commit tardío por detrás del cursor se pierda, que es
   justo lo que la gracia existiría para evitar. Añadirla sería producción que
   ningún test rojo pide (Ley 1), así que el parámetro se retiró del puerto.
2. **Sin `pendingRetries`: una sola cola.** El diseño proponía una cola de
   reintentos por propietario, leída antes que la de eventos, para que @s22
   cumpliera «E1 se intentó antes que E2». Al escribirlo salió un agujero: con
   dos colas, el orden entre ellas es un convenio, y además @s20 deja el cursor
   **detrás** del evento fallido, de modo que el evento varado **sigue estando
   en la cola de eventos**. Se resolvió al revés: `AutomationCandidate` lleva
   las ejecuciones ya registradas para su evento, y el reintento ocurre donde el
   outbox ya lo ordena. «Antes que el nuevo» pasa a ser una propiedad de
   construcción, no un convenio. Un puerto menos y un orden menos que mantener.

   La consecuencia que hubo que decidir: un evento cuya confirmación falla
   **retiene el recorrido** sólo mientras le quede alguna fila en `retry`; si
   todas quedan `failed`, el recorrido sigue y el cursor lo alcanza en el ciclo
   siguiente, cuando lo relee y encuentra todas las reglas zanjadas. Eso es lo
   que hace compatibles el «el cursor no avanza» de @s20 con el «el cursor queda
   en E2» de @s22.

## Ciclos, uno por escenario y uno por commit

### @s15 — `1acc2c4`

Dos mitades. La de aplicación: un ciclo ejecuta E1 y E2 desde el cursor en E0,
2 ejecuciones `succeeded`, 2 tareas, cursor en E2. La de configuración:
`AutomationConfiguration` sólo publica el bean con `app.automations.enabled=true`.

**Rojo, en dos pasos.** (1) La prueba no compilaba: ninguno de los siete tipos
existía —`cannot find symbol: class AutomationWork`, y seis más—. (2) Con
`runCycle()` vacío, `AssertionFailedError` en la línea del `containsExactly`
sobre las ejecuciones. Verde con el motor mínimo.

Mitad de configuración, **rojo por mutación**: borrado el
`@ConditionalOnProperty`, `s15_withoutTheFlagThereIsNoWorkerAndNothingIsEverRead`
FAILED; restaurado, verde. La prueba además duerme 1,5 s con el contexto
deshabilitado y afirma cero ciclos: «no se lee ni se escribe nada».

### @s16 — `84dac60`

El cursor ausente ya no significa «no hacer nada»: se inicializa en el
`createdAt` de la regla **más antigua** del propietario y el mismo ciclo sigue
desde ahí. Los 40 eventos anteriores a la regla quedan fuera y E41, posterior,
se procesa sin esperar a un segundo ciclo.

Nota de lectura del contrato: «inicializa el cursor **en el presente**» no puede
significar «en el instante del primer ciclo», porque el Given pone E41
*antes* del ciclo y el Then exige que se procese. El presente que vale es el de
la creación de la regla. Se implementa en el worker y no en `CreateAutomation`
para no tocar un caso de uso de otro alcance.

**Rojo:** con la producción de `1acc2c4` el test falla por cero ejecuciones y
cursor nulo.

### @s17 — `23dfb92`

Recorrido en orden `(occurred_at, event_id)` con E1 y E2 al mismo instante y
`event_id` de E1 menor; la fila `blocked` no produce ejecución pero **sí** mueve
el cursor; el evento T con `occurredAt` anterior al cursor nunca se lee. El
doble ordena como PostgreSQL (instante, luego uuid **sin signo**, reusando
`WebhookCursor.compareUnsigned`).

**Rojo:** sin la rama de `blocked`, 4 ejecuciones y 4 tareas donde el contrato
exige 3.

### @s18 — `7187d92` (uno de los tres «medio cubiertos»)

Las dos filas del Examples, midiendo el efecto real y no la plantilla:
título exacto, `completionCriterion` exacto
`"TaskCreated.v1 a las 2026-09-08T10:15:30.123456Z"` y `estimatedMinutes` 30.

Pasó a la primera —el renderizado ya existía para la simulación—, así que el
rojo se acredita por mutación, con **dos** mutantes:

- **A**: el efecto se construye con `action.titleTemplate()` /
  `criterionTemplate()` en vez de con la vista previa resuelta. Caen **las dos
  filas**.
- **B**: `AutomationRendering` resuelve `project.name` con el nombre histórico
  (`"Marketing"` fijo) en vez del vigente. Cae **exactamente la fila 2**, la del
  proyecto renombrado antes del ciclo — que es literalmente la propiedad que el
  escenario afirma.

Restaurado, verde.

### @s24 — `639d504` (medio cubierto: sólo tenía `AutomationMatcherTest`)

Dos tests para las cuatro filas: la regla desactivada no ejecuta pero el cursor
avanza; reactivarla después **no** resucita el evento; y un PUT que corre con la
evaluación deja una versión entera, nunca una mezcla, y nunca una tarea sin
ejecución.

**Rojo por mutación:**

- **C**: `process()` se salta el `commit` cuando ninguna regla dispara —la
  «optimización» evidente—. Caen @s24 fila 1 (cursor sin avanzar y el ciclo
  siguiente resucitando E1) **y** @s17 (la fila bloqueada tampoco movía nada).
- **D**: `outcomeOf()` relee la regla con `rules.find()` para renderizar. Cae
  @s24 fila 4: dos lecturas del almacén y título de la versión nueva sobre una
  regla evaluada con la anterior. La mezcla de versiones que el contrato prohíbe.

La carrera se modela con `RacingRules`, un almacén cuya única regla cambia entre
la primera lectura y la segunda; el test afirma `reads == 1`.

### @s21 — `9623c4f` (medio cubierto: sólo la anticipación en la simulación)

Las cinco filas: `PROJECT_COMPLETED`, `TITLE_TOO_LONG`, `CRITERION_TOO_LONG` y
las dos de `ENDPOINT_NOT_FOUND`. Cada fila afirma además que R2 sigue
ejecutándose con su efecto, que el cursor queda en E y que un segundo ciclo no
crea reintento.

**Rojo real, no por mutación:** las cinco filas fallaban contra el ejecutor de
`23dfb92`, que sólo sabía construir ejecuciones `succeeded` y hacía un cast
crudo a `CreateTaskAction` (las dos filas de webhook reventaban con
`ClassCastException`). El cast se sustituyó por el `switch` sellado.

Decisión escrita: **endpoint borrado y endpoint desactivado son la misma cosa
desde el ejecutor**, porque `WebhookEndpointLookup` responde «no es un endpoint
activo de este propietario» en ambos casos — y el contrato les da el mismo
código. Las dos filas se conservan como filas distintas del Examples porque el
Given difiere, pero comparten oráculo.

### @s20 — `3833874`

Un fallo de almacenamiento revierte ejecución, tarea y evento; la única fila que
sobrevive es la de reintento, escrita **fuera** de la transacción; y el
recorrido del propietario se detiene en ese evento para que el cursor no lo
salte. El E2 posterior del test es lo que acredita esa última cláusula.

**Rojo:** con el ejecutor anterior la excepción escapaba del ciclo, no se
registraba ninguna fila y nada impedía saltar a E2.

Decisión escrita: se captura `RuntimeException`, no sólo
`StorageUnavailableException`, y se etiqueta `STORAGE_UNAVAILABLE`. Dejar que un
fallo inesperado deje varado a un propietario para siempre es peor que un código
de error grueso; el contrato no publica ningún otro código para una confirmación
revertida.

### @s22 — `eea71f4`

Dos tests. El primero: ejecución `retry` en `attempt 2` para E1, evento nuevo E2
posterior, y el almacenamiento fallando sólo para E1. Afirma el orden dentro del
ciclo (`attempted == [E1, E2]`), `attempt 3 / failed / STORAGE_UNAVAILABLE`
**conservando el id de la fila**, E2 `succeeded attempt 1`, cursor en E2 y que un
ciclo posterior no crea un cuarto intento. El segundo: una fila ya zanjada que
el cursor vuelve a leer no se intenta otra vez — el agujero que dejaba el
primero, porque allí el cuarto intento era inalcanzable por otra razón.

**Rojo:** los dos tests no compilaban (`AutomationCandidate` sin `runs`), y con
el ejecutor anterior E1 habría salido con `attempt 1` y E2 no se habría
procesado nunca.

### @s28 y @s29 — `a164f2e`

El ejecutor ignoraba `matcher.loopGuarded`: el `TaskCreated.v1` de una tarea
creada por automatización encadenaba. Ahora es una omisión deliberada, igual que
la fila bloqueada: no produce nada y el cursor avanza. Las dos filas de @s28 (R1
viva y R1 borrada) y el @s29 completo, que además cuenta las consultas al puerto
para afirmar que la guarda **no** se consulta para `TaskStatusChanged.v1`.

**Rojo:** las dos filas de @s28 fallaban contra el ejecutor anterior, que creaba
una tarea por cada regla activa sobre el evento de la tarea automatizada.

Nota: @s28 y @s29 no estaban en la lista de nueve del inventario, pero el
agujero era real y lo abría el propio ejecutor nuevo.

### @s19 (bitácora) — `e0765da`

La única cláusula de @s19 medible sin contenedor: el log lleva `ruleId`,
`eventId`, `outcome`, `attempt` y `code`, y **no** lleva el título renderizado ni
el nombre del proyecto. **Rojo:** el ejecutor no escribía ninguna línea.

### @s19, @s23 y @s25 contra Postgres — `9a1d1a0`

`PostgresAutomationWork`, el adaptador del puerto único, y los dos beans en
`ApplicationConfiguration`. `CreateTask` se une a la transacción de la
confirmación por `PROPAGATION_REQUIRED`, así que la tarea y su `TaskCreated.v1`
caen con todo lo demás; sólo `record()` escribe aparte (`REQUIRES_NEW`), que es
precisamente la fila que debe sobrevivir al rollback.

El reclamo de `(regla, evento)` se apoya en el `UNIQUE` de `V28`: primer intento
con `ON CONFLICT DO NOTHING`, reintento con `UPDATE ... WHERE status='retry'`.
Cero filas significa que otro worker ya la tiene: se abandona la confirmación
entera con `AutomationClaimedException` y el recorrido sigue, sin error no
controlado — que es la tercera cláusula de @s23.

**Rojo en dos pasos y dos mutantes:**

- Los tres tests fallaban antes del adaptador (no había bean de `AutomationWork`
  ni de `ExecuteAutomationsUseCase`).
- **E**: la confirmación deja de ser transacción
  (`PROPAGATION_NOT_SUPPORTED`). Caen @s25 —la tarea sobrevive a la caída— y
  @s23.
- **F**: el reclamo pasa a gana-el-último (`ON CONFLICT DO UPDATE`). Cae @s23
  con **dos tareas y dos `TaskCreated.v1` para un solo evento**. Acredita además
  que la carrera del test es real y no una serialización afortunada, que es el
  riesgo clásico de una prueba de concurrencia.

Modelo de la caída de @s25: la confirmación crea la tarea y su evento y luego
escribe una fila de ejecución con un `status` que el `CHECK` de `automation_runs`
rechaza. El rollback llega con la tarea y la fila de outbox ya escritas, que es
exactamente «cae después de crear la tarea y antes de confirmar».

### @s26 — `9951147`

Se retira el stub `(owner, endpointId) -> false` de `WebhookEndpointLookup` —la
feature 25 lleva en `main` desde hace tiempo— por una consulta real sobre
`webhook_endpoints`: sólo un endpoint **activo** del **propio** propietario
cuenta. Con eso, la rama `Notify` del adaptador inserta una entrega pendiente de
la feature 25 en la misma confirmación que la ejecución y el cursor.

El cuerpo lo copia la propia base desde `outbox_events` (`o.payload::text` en el
`SELECT` del `INSERT`), de modo que **ninguna re-serialización puede alterar un
byte**. La suscripción del endpoint no se consulta a propósito: la regla es la
suscripción, y por eso el test usa un endpoint suscrito sólo a `TaskCreated.v1`
para un evento `ProjectStatusChanged.v1`.

**Rojo:** el test no podía ni sembrar la regla —`create.create` lanzaba
`WebhookEndpointNotFoundException` con el stub— y después fallaba por ausencia
de entrega.

De paso, `s5` de `AutomationWiringTest` dejaba de decir la verdad («hasta que
exista la feature 25…»). Pasa a medir lo que ahora hay: endpoint activo propio,
desactivado propio, activo ajeno y desconocido, y una regla que **sí** se
guarda.

**Consecuencia:** quedan alcanzables las filas `NOTIFY_WEBHOOK` de @s5, @s12,
@s21, @s32 y @s33 que el inventario daba por bloqueadas. Sus oráculos concretos
no se han escrito en esta sesión: es trabajo de otro carril o de otra pasada, y
queda anotado aquí como lo único que la retirada del stub deja abierto.

### Ámbito de mutación — `60a3a4e`

`automationsClasses` de `backend/build.gradle.kts` sólo tenía comodines que
empiezan por `Automation`, así que **`ExecuteAutomations`, `AutomationSchedule` y
`AutomationConfiguration` —el motor entero— quedaban fuera**: la campaña habría
dado por cubierto código que nadie mutaba. Mismo criterio que el hallazgo 10 con
`ApiErrors`. Añadidos los tres patrones.

## Recuento del ejecutor

| Escenario | Dónde se prueba | Estado |
| --- | --- | --- |
| @s15 | `ExecuteAutomationsTest` + `AutomationScheduleTest` | **Cerrado** |
| @s16 | `ExecuteAutomationsTest` | **Cerrado** |
| @s17 | `ExecuteAutomationsTest` | **Cerrado** |
| @s19 | `AutomationExecutionTest` (Postgres) + bitácora en `ExecuteAutomationsTest` | **Cerrado** |
| @s20 | `ExecuteAutomationsTest` | **Cerrado** |
| @s22 | `ExecuteAutomationsTest`, dos tests | **Cerrado** |
| @s23 | `AutomationExecutionTest` (dos hilos reales) | **Cerrado** |
| @s25 | `AutomationExecutionTest` | **Cerrado** |
| @s26 | `AutomationExecutionTest` | **Cerrado** |
| @s18 (medio cubierto) | `ExecuteAutomationsTest`, las dos filas | **Cerrado** |
| @s21 (medio cubierto) | `ExecuteAutomationsTest`, las cinco filas | **Cerrado** |
| @s24 (medio cubierto) | `ExecuteAutomationsTest`, dos tests | **Cerrado** |
| @s28, @s29 (agujero abierto por el ejecutor) | `ExecuteAutomationsTest` | **Cerrado** |

**Los nueve del inventario: nueve cerrados.** Más los tres medio cubiertos y los
dos de la guarda contra bucles.

## Lo que sigue abierto, y no se silencia

1. **Las filas `NOTIFY_WEBHOOK` de @s5, @s12, @s21, @s32 y @s33.** Ya son
   alcanzables —el stub cayó en `9951147`— pero sus oráculos no están escritos.
2. **La puerta de mutación**, no ejecutada por instrucción del coordinador. El
   ámbito ya está corregido (`60a3a4e` en backend, hallazgo 11 en frontend).
   Umbral 0,80, y hay que registrar la **lista de supervivientes**.
3. **`app.automations.enabled` no se declara en `application.properties`**, igual
   que `app.webhooks.enabled`: el worker es opt-in por entorno. Si se quiere que
   el motor corra en la pila de E2E o en despliegue, hay que ponerlo allí — es
   fichero compartido y no se ha tocado.
4. **Hallazgo 9** (recorrido de teclado y foco visible) lo cerró otro carril en
   `e2e/automations-ux.spec.mjs`. No se ha tocado.

## Ficheros compartidos tocados en esta sesión (REGLAS.md §6)

- `backend/src/main/java/.../adapter/config/ApplicationConfiguration.java`: dos
  beans nuevos (`automationWork`, `executeAutomations`) y el
  `webhookEndpointLookup` que pasa de stub a consulta real.
- `backend/build.gradle.kts`: tres patrones dentro de `automationsClasses`.

Ambos son puntos de conflicto probables en la integración.

---

# Hallazgo 1 [BLOQUEANTE] — inventario original (mapa escenario a escenario)

**No implementado en la sesión anterior.** El coordinador lo retiró por
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

---

# Hallazgos que quedan ABIERTOS, y por qué

ACTUALIZACIÓN al final de la sesión: el **hallazgo 4 quedó cerrado** para
`e2e/automations.spec.mjs` (ver la sección siguiente, con un defecto real de
contraste encontrado y arreglado por el camino). Siguen abiertos 3, 7, 8 y 9, y
el 4 sólo en lo que toca a `e2e/automations-ux.spec.mjs`.

Los que faltan (3, 7, 8 y 9) viven todos en la misma auditoría E2E,
`e2e/automations-ux.spec.mjs`, y exigen levantar la pila real
(`E2E_WEB_PORT=18094`) para acreditar el rojo. No cabían en el plazo de la
sesión, y REGLAS.md §3 prohíbe cerrar un hallazgo con un oráculo que nunca se
ha visto fallar: un oráculo E2E escrito a ciegas es exactamente la prueba
placebo que el dictamen castiga. Se dejan abiertos y documentados, no
silenciados.

Lo que **sí** se ha hecho por ellos en esta sesión: corregir
`progress/ux_automations.md`, que declaraba verificado lo que nunca se midió.
Eso es la mitad del hallazgo 3, la mitad del 4 y la mitad del 9 —la infracción
de `AGENTS.md:51` («no declarar cumplimiento sin medirlo»)— y se cierra sin
necesidad de pila:

- Título de la sección: pasa de «Estados verificados (los siete…)» a
  «Modalidades verificadas», con un aviso que dice en qué consiste la confusión
  y qué dos de los siete estados de pantalla se midieron realmente.
- Fila 1 (anchos): ya no dice «lista y editor abiertos» —era falso, el
  `clearRules()` del `beforeEach` deja cero reglas—, y nombra explícitamente
  que el recorte de contenido no está medido.
- Fila 6 (`forced-colors`): ya no dice «foco alcanzable y visible». Dice lo que
  el test hace: `save.focus()` + `toBeFocused()` sobre un solo control, sin
  recorrido con Tab y sin `outlineWidth`.
- Fila «Posición en serie»: el orden del DOM está verificado por lectura; el
  orden de teclado **no está medido**.
- Fila «Von Restorff»: verificada por lectura y en unitario; en E2E ese
  `role="switch"` nunca se renderizó.
- Fila «Región común»: axe nunca vio el `<ul aria-label="Reglas">`, el
  `<ul aria-label="Coincidencias">` ni la sección de historial.

## Hallazgo 4 [BLOQUEANTE] — el Given de @s42 no se cumple en ningún test

`features/automations.feature:548` exige «lista, editor abierto y resultados de
simulación visibles», y ese Given rige las cinco filas del Examples. Las dos
suites que tocan `/automatizaciones` (`e2e/automations-ux.spec.mjs:18-25` y
`e2e/automations.spec.mjs:9-16`) hacen `clearRules()` en `beforeEach` y entran
por `goto` + «Nueva regla», de modo que `rules.length === 0` y `simulation`
sigue nulo.

**Remedio, listo para ejecutar.** En el `beforeEach` de
`e2e/automations-ux.spec.mjs`, tras `clearRules()`, sembrar por API dos reglas
—una activa con nombre largo y acción `CREATE_TASK`, otra inactiva— igual que
`e2e/automations.spec.mjs:49-53` hace al guardar; abrir el editor con «Editar
<la primera>» en vez de «Nueva regla»; pulsar «Simular» y esperar
`getByRole("status", { name: "Resultado de la simulación" })` antes de
`geometry()`. Con una coincidencia `wouldFail` para que el texto largo esté
presente. Rojo acreditable: con el `beforeEach` actual, la espera del
`role="status"` caduca.

## Hallazgo 3 [BLOQUEANTE] — la auditoría alcanza 2 de los 7 estados de pantalla

Faltan cinco: carga retenida, error 503 con «Reintentar», lista con dos reglas,
resultados de simulación e historial abierto con una fila con `createdTaskId`.
Cada uno necesita axe + geometría en los mismos anchos y temas. El remedio del
hallazgo 4 cubre dos de los cinco (lista y simulación); los otros tres exigen
rutas interceptadas (`page.route`) para retener la carga y forzar el 503, y
sembrar una ejecución para el historial.

## Hallazgos 7 y 8 [ALTA/MEDIA] — «ni contenido cortado» sin oráculo

El único oráculo geométrico es
`expect(observed.scroll).toBeLessThanOrEqual(observed.client)` sobre
`documentElement`: mide desbordamiento de página, nunca recorte. El repositorio
ya tiene el oráculo correcto y probado en
`e2e/ics-calendar-ux.spec.mjs:171-185` (campo `clipped`, recorriendo
`main, main *`, marcando cuando `overflowX !== "visible" && scrollWidth >
clientWidth + 1` o el equivalente vertical).

**Remedio.** Portar `clipped` a `geometry()` y asertar `toEqual([])` en
`assertUsable`, con lo que los cuatro tests (siete modalidades, zoom nativo
incluido) quedan cubiertos de golpe. **Caveat que hay que decidir por escrito**:
`.automations input, select` lleva `text-overflow: ellipsis`
(`styles.scss:502`) y Chromium aplica `overflow: clip` a los `<input>` de texto
en su hoja de agente; la primera ejecución los marcará. O se exceptúan los
controles de formulario nativos con justificación (su valor es alcanzable con
el cursor y está íntegro en el árbol de accesibilidad) o se arregla como en
ics; excluirlos en silencio vuelve a vaciar el oráculo. Y la mutación de
control obligatoria antes de cerrar: `.automations li { overflow: hidden;
max-height: 96px; }` debe hacer fallar la suite.

Además, `e2e/automations.spec.mjs:97` se titula «el texto al 200 % **no corta
contenido**» y sólo mide `scrollWidth`: o se renombra a «no desborda en
horizontal» o se le pone el oráculo que promete.

## Hallazgo 9 [ALTA] — ni recorrido de teclado ni foco visible

Lo único relacionado con teclado en toda la feature son tres líneas de
`.focus()` programático dentro del test de `forced-colors`. `outlineWidth` se
mide en al menos diez specs del repositorio y aquí en ninguno.

**Remedio.** Un test que recorra con `page.keyboard.press("Tab")` desde el
inicio del `<main>`, recoja rol y nombre accesible de cada parada, afirme la
secuencia esperada —incluido Guardar antes que Simular—, exija
`outlineWidth >= 2` (o `box-shadow` equivalente) en cada parada, y vuelva con
`Shift+Tab` comprobando que se sale por ambos extremos sin trampa de foco.
Repetirlo en `forced-colors`. La parte de matriz ya está corregida arriba.

## Puerta de mutación

**No ejecutada**, por instrucción explícita del coordinador (plazo y carga de
máquina). Pendientes, para quien la lance:

1. Añadir `"com.apptolast.organization.adapter.http.ApiErrors"` a
   `automationsClasses` en `backend/build.gradle.kts:492-511` — hallazgo 10,
   que el brief da por cerrado; conviene confirmarlo antes de la campaña.
2. `node scripts/project.mjs mutate automations-frontend` con el ámbito ya
   ampliado en esta sesión (hallazgo 11), y `automations-backend`.
3. Registrar en `progress/mutation_automations_*.md` la **lista de
   supervivientes**, no sólo el porcentaje. Umbral 0,80.

---

## Hallazgo 4 [BLOQUEANTE] — el Given de @s42, cumplido; y un defecto real que escondía

**Contrato:** `features/automations.feature:548`, «Given /automatizaciones con
lista, editor abierto y resultados de simulación visibles», que rige las cinco
filas del Examples de @s42.

**Ciclo, sobre la pila real (`E2E_WEB_PORT=18094`).**

1. ROJO medido, no argumentado. Se añadió un test de control temporal que
   reproduce el Given anterior (`goto` + «Nueva regla», sin sembrar) y afirma lo
   que el contrato exige. Falla:
   `CONTROL rojo: el Given anterior de @s42 no tenia lista ni simulacion` —
   ni `<ul aria-label="Reglas">` ni el `role="status"` de la simulación existen.
   Queda acreditado que las cinco filas se medían en el estado vacío. El control
   se borró tras registrarlo.
2. ROJO ADICIONAL, no previsto y **real**: al sembrar las reglas, los cuatro
   tests de @s42 pasaron a fallar por axe con una violación `color-contrast` de
   impacto **serious** (WCAG 1.4.3):
   `Element has insufficient color contrast of 1.01 (foreground color: #ffffff,
   background color: #fdfefb, font size: 9.0pt (12px))`, sobre el
   `role="switch"` de cada regla. No era un falso positivo: `.automations
   [role="switch"]` fijaba `background: var(--editable)` (casi blanco) pero
   heredaba el `color` blanco del botón de acción, así que el texto
   «Activa»/«Inactiva» era **invisible**. Justo el defecto que el dictamen
   predijo que la auditoría no podía ver, porque ese interruptor nunca se
   renderizó en ninguna corrida medida.
3. VERDE: una línea de producción, `color: var(--ink)` en
   `frontend/src/styles.scss`, `.automations [role="switch"]`. Los **7 tests en
   verde en 27,6 s**, incluidos los cuatro anchos con axe.

**Cambios.**
- Producción: `frontend/src/styles.scss`, la tinta del interruptor.
- `e2e/automations.spec.mjs`: helper `seedRule(...)` que crea reglas por la API
  real (`POST /api/v1/me/automations` con `csrfHeaders`) y `openDenseScreen(...)`
  que siembra dos reglas —una activa con nombre largo, una inactiva—, espera la
  lista y los dos interruptores, abre el editor con «Editar <la larga>», pulsa
  «Simular» y espera el `role="status"`. Los cuatro tests de anchos y el del
  texto al 200 % miden desde ahí.
- Renombrado `«el texto al 200 % no corta contenido a 1440 px»` a **«no desborda
  en horizontal»**: el título prometía un recorte que el oráculo no medía. Es el
  hallazgo adicional del verificador dentro del 8; el oráculo de recorte en sí
  sigue abierto y anotado.

**Nota de coordinación:** no se toca el zoom nativo al 200 %; otro carril lo
lleva en `e2e/automations-native-zoom.spec.mjs`.

**Estado: cerrado** para `e2e/automations.spec.mjs`. Sigue abierto el mismo
sembrado en `e2e/automations-ux.spec.mjs` (temas, `forced-colors`,
`reduced-motion`), que es el hallazgo 3.

---

## Hallazgos 3, 7 y 8 — pantalla densa en la auditoría UX y oráculo de recorte

**Contrato:** `features/automations.feature:548` (el Given de @s42) y `:550`
(«no hay desplazamiento horizontal **ni contenido cortado**»).

### Lo que se cambió

`e2e/automations-ux.spec.mjs`:

- `openEditor(page)` se sustituye por `openDenseScreen(page, request, project)`:
  limpia, siembra por API dos reglas —una activa con nombre largo, otra
  inactiva—, espera la lista y el interruptor de la inactiva, abre el editor con
  «Editar <la larga>», pulsa «Simular» y espera el
  `role="status"` de «Resultado de la simulación». Las cuatro pruebas de la
  auditoría miden desde ahí: cuatro anchos × dos temas, texto al 200 % en los
  mismos, y `forced-colors` + `reduced-motion`.
- `geometry()` gana el campo **`clipped`**: recorte **por elemento** y en **los
  dos ejes**, portado de `e2e/ics-calendar-ux.spec.mjs:171-185`. Recorre
  `main, main *` y marca cuando `overflowX !== "visible" && scrollWidth >
  clientWidth + 1` o el equivalente vertical, devolviendo etiqueta, `aria-label`,
  los primeros 60 caracteres de texto y las cuatro medidas, para que el fallo
  sea diagnosticable.
- `assertUsable()` afirma `expect(observed.clipped, "la pantalla recorta
  contenido (ancho o alto)").toEqual([])`. Como es el oráculo compartido de las
  cuatro pruebas, la cláusula queda cubierta en todas las modalidades de golpe.

### La excepción, decidida por escrito y no en silencio

`INPUT`, `SELECT` y `TEXTAREA` se exceptúan del oráculo de recorte. Razón:
Chromium aplica `overflow: clip` a los controles de texto en su hoja de agente
de usuario, de modo que **todo** `<input>` con un valor largo se marcaría; su
valor sigue siendo alcanzable con el cursor e íntegro en el árbol de
accesibilidad, así que no es un fallo de WCAG 1.4.4. Queda escrito en el
comentario del helper y en esta bitácora. Excluirlos sin decirlo volvería a
vaciar el oráculo, que es justo lo que el dictamen prohíbe.

### Rojo acreditado: la mutación de control que el dictamen exige

Añadido a `frontend/src/styles.scss`, dentro de `.automations`:
`li { overflow: hidden; max-height: 96px; }`. Resultado con la pila real:

- **3 de las 4 pruebas fallan** con `Error: la pantalla recorta contenido (ancho
  o alto)` y el detalle `"clientHeight": 94` frente a `"scrollHeight": 327`,
  `249`, `447` y `800` según el ancho y el tema.
- El oráculo anterior (`documentElement.scrollWidth <= clientWidth`) **no
  detectaba nada** de eso: el contenido queda recortado, no desbordado.

Producción restaurada, **4/4 en verde en 42,3 s**.

### Renombrado

`e2e/automations.spec.mjs:97` se titulaba «el texto al 200 % **no corta
contenido**» y sólo medía desbordamiento: renombrado a «no desborda en
horizontal» en el commit del hallazgo 4.

**Nota de coordinación:** el zoom nativo al 200 % lo lleva otro carril en
`e2e/automations-native-zoom.spec.mjs`; no se ha duplicado. La prueba de zoom
nativo que aún vive en `automations-ux.spec.mjs` no se ha tocado, aunque hereda
el `clipped` por compartir `assertUsable`.

**Estado: 3, 7 y 8 cerrados.**

---

## Recuento final de la sesión

| Hallazgo | Gravedad | Estado |
| --- | --- | --- |
| 1 — no existe el ejecutor de reglas | BLOQUEANTE | **CERRADO el 10 de septiembre**: los nueve escenarios del inventario, más @s18, @s21, @s24, @s28 y @s29. Ver la sección del ejecutor arriba |
| 2 — el interruptor no ataba el If-Match vivo ni el instante | BLOQUEANTE | Cerrado |
| 3 — la auditoría alcanzaba 2 de 7 estados | BLOQUEANTE | Cerrado (lista, editor y simulación en las cuatro pruebas) |
| 4 — el Given de @s42 no se cumplía | BLOQUEANTE | Cerrado; destapó un defecto real de contraste |
| 5 — @s12, historial tras el PUT | ALTA | Cerrado (salvo la fila `NOTIFY_WEBHOOK`, que depende del cableado de la 25) |
| 6 — aislamiento por identidad | ALTA | Cerrado |
| 7 — recorte de contenido sin oráculo | ALTA | Cerrado, con mutación de control acreditada |
| 8 — recorte sólo horizontal y a nivel de documento | MEDIA | Cerrado, mismo oráculo de dos ejes |
| 9 — recorrido de teclado y foco visible | ALTA | **ABIERTO**; la matriz UX ya no lo declara verificado |
| 10 — `ApiErrors` fuera del ámbito PIT | MEDIA | Ya venía cerrado del brief |
| 11 — ámbito Stryker sin la integración con el armazón | MEDIA | Cerrado |

**Cerrados: 8 (más el 10 que venía dado). Abiertos: 2 — el 1 y el 9.**

> ACTUALIZACIÓN 10 de septiembre: el **1 quedó cerrado** (el ejecutor, arriba) y
> el **9 lo cerró otro carril** en `e2e/automations-ux.spec.mjs`. No queda
> ninguno de los once abierto.

Puerta de mutación **no ejecutada**, por instrucción del coordinador.

Estados de pantalla que siguen sin auditar (resto del 3, ahora acotado): carga
retenida, error 503 con «Reintentar» e historial abierto. Exigen `page.route`
para retener y forzar el 503 y sembrar una ejecución; el resto del estado denso
ya está medido.
