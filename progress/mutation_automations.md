# Mutación — feature 30 (automatizaciones)

Lista nominal de **cada** superviviente con veredicto escrito, que es la condición 2
del juez —BLOQUEANTE— y no existía en ninguna parte del repositorio (motivo M13).
Cubre los motivos **M13 a M22** de `progress/carriles/bloqueantes_30.md`.

**Cómo se ha trabajado.** No se ha lanzado ninguna campaña de PIT ni de Stryker: se
miden en el centro y compiten por memoria. Todo lo de aquí sale de dos sitios y de
ningún otro:

1. los informes ya medidos —`backend/build/reports/pitest-automations/mutations.xml`
   (04:26) y `frontend/reports/mutation-automations/mutation.json`—, leídos y
   contados con un script, no copiados de un `index.html`;
2. para cada mutante concreto, **aplicarlo a mano al fuente de producción, correr la
   clase de test concreta y deshacer el cambio**. Cada veredicto de «muerto» de más
   abajo lleva el nombre de la prueba que cae y se ha comprobado uno a uno, no en
   bloque.

El árbol de producción quedó intacto: `git status` sobre `backend/src/main` y
`frontend/src` está vacío. Los ficheros se restauran desde copia byte a byte, nunca
con `git checkout`.

---

## 1. La cifra (motivo M18)

| | |
|---|---|
| Mutantes generados | **607** |
| KILLED | **581** |
| SURVIVED | 14 |
| NO_COVERAGE | 12 |
| **Puntuación medida** | **95,72 %** (581 ÷ 607 = 95,7166 %) |

La cifra publicada de **«96,00 %» no estaba medida**: es el entero redondeado del
`index.html` de PIT, presentado con dos decimales que nadie calculó, en un documento
cuyo encabezado dice que todas las cifras están medidas. La buena, contada sobre el
XML, es **95,72 %**. Queda escrita aunque quede peor.

Dos avisos más sobre esa cifra, que valen para cualquiera que la cite:

- La campaña es de las **04:26** y el superviviente de `AutomationConfiguration` se
  mató a las **04:28** (`5309e409`). Desde ese minuto la cifra ya no describía el
  árbol.
- Después de este carril tampoco lo describe, y en el otro sentido. **17 mutantes
  más** de la lista quedan muertos con oráculo acreditado, y uno más lo estaba ya
  desde las 04:28. Aritmética sobre kills verificados uno a uno:
  (581 + 18) ÷ 607 = **98,68 %**.

  > Ese 98,68 % es una **proyección**, no una medida. Nadie ha vuelto a correr PIT.
  > La única cifra medida que existe hoy sigue siendo el **95,72 %** de las 04:26.
  > Quien vuelva a lanzar la campaña debe sustituir las dos por el número real.

---

## 2. Lista nominal de los 26 mutantes no muertos

Los 26 del informe (14 SURVIVED + 12 NO_COVERAGE), sin excepción, cada uno con su
veredicto. `MUERTO` significa que existe una prueba que **falla con el mutante
aplicado y pasa sin él**, comprobado en esta sesión.

### 2.1 Muertos en este carril (17)

| # | Clase : línea | Mutador | Veredicto — prueba que lo mata |
|---|---|---|---|
| 1 | `AutomationBody:48` `condition()` | NullReturnVals | **MUERTO** — `AutomationsApiTest.s3_acceptsAConditionOverAnOwnProject` (ver M14) |
| 2 | `AutomationBody:94` `exactly()` | VoidMethodCall (quita `onlyKnown`) | **MUERTO** — `AutomationsApiTest.s8_rejectsAMalformedBodyNamingTheField[8]`, fila nueva: cuenta de claves correcta con un nombre cambiado |
| 3 | `AutomationView$Rule:39` `enabled()` | BooleanTrueReturnVals | **MUERTO** — `AutomationsApiTest.s12_aPausedRuleIsReadAsPaused` (ver M17) |
| 4 | `AutomationView$Run:52` `deliveryId()` | NullReturnVals | **MUERTO** — `AutomationsApiTest.s34_aWebhookRunNamesTheDeliveryItProduced` |
| 5 | `AutomationView$TaskPreview:68` `completionCriterion()` | EmptyObjectReturnVals | **MUERTO** — `AutomationsApiTest.s30_simulatesWithoutSavingAnything` |
| 6 | `AutomationView$WebhookPreview:77` `eventId()` | NullReturnVals | **MUERTO** — `AutomationsApiTest.s33_simulatingReportsTheWebhookPreviewClosed` |
| 7 | `AutomationView$Match:79` `loopGuarded()` | BooleanFalseReturnVals | **MUERTO** — `AutomationsApiTest.s32_aGuardedMatchSaysSoAndStillResolvesItsPreview` |
| 8 | `AutomationRun:10` `deliveryId()` (dominio) | NullReturnVals | **MUERTO** — `AutomationsApiTest.s34_aWebhookRunNamesTheDeliveryItProduced` |
| 9 | `AutomationTemplate:36` `render()` | Math (`+` → `-`) | **MUERTO** — `AutomationTemplateTest.s18_rendersPlaceholdersWithTheCurrentValues` (ver 2.4) |
| 10 | `ExecuteAutomations:143` `lambda$attemptOf$0()` | BooleanTrueReturnVals | **MUERTO** — `ExecuteAutomationsTest.s22_eachRuleRetriesOnItsOwnPreviousRunAndNeverOnItsSiblings` (ver M15) |
| 11 | `ExecuteAutomations:97` `process()` | VoidMethodCall (quita `List::forEach`) | **MUERTO** — `ExecuteAutomationsTest.s20_aFailedConfirmationLeavesNothingBehindAndStrandsTheWalkOnTheEvent` (ver M20) |
| 12 | `ExecuteAutomations:97` `lambda$process$0()` | VoidMethodCall (quita `::log`) | **MUERTO** — la misma prueba de @s20 |
| 13 | `PostgresAutomationWork:85` `startCursor()` | VoidMethodCall (quita `::write`) | **MUERTO** — `AutomationWorkPersistenceTest.s16_theFirstCursorIsWrittenOnceAndALaterStartNeverDragsItBack` |
| 14 | `PostgresAutomationWork:341` `write()` | VoidMethodCall (quita `Runnable::run`) | **MUERTO** — la misma prueba de @s16 |
| 15 | `PostgresAutomationWork:126` `record()` | VoidMethodCall (quita `executeWithoutResult`) | **MUERTO** — `AutomationWorkPersistenceTest.s20_theRunOfARolledBackConfirmationIsWrittenApartAndTheRetryRenewsIt` |
| 16 | `PostgresAutomationWork:126` `lambda$record$0()` | VoidMethodCall (quita `::upsert`) | **MUERTO** — la misma prueba de @s20 |
| 17 | `PostgresAutomationWork:316` `runOf()` | NullReturnVals | **MUERTO** — `AutomationWorkPersistenceTest.s22_eachCandidateCarriesItsOwnPreviousRunsAndNobodyElses`, prueba de otro carril (`866426dc`, 12:35) posterior a la campaña. Acreditado aquí aplicando el mutante. |

### 2.2 Ya muerto antes de este carril (1)

| # | Clase : línea | Mutador | Veredicto |
|---|---|---|---|
| 18 | `AutomationConfiguration:16` `automationSchedule()` | NullReturnVals | **MUERTO** en `5309e409` (04:28), dos minutos después de la campaña. Acreditado aquí: con el mutante cae `AutomationScheduleTest.s15_withoutTheFlagThereIsNoWorkerAndNothingIsEverRead`. |

### 2.3 Justificados: equivalentes, con demostración (1)

| # | Clase : línea | Mutador | Veredicto |
|---|---|---|---|
| 19 | `AutomationCursor:20` `precedes()` | ConditionalsBoundary (`> 0` → `>= 0`) | **EQUIVALENTE, y se demuestra.** La línea es `if (byInstant != 0) return byInstant > 0;`. Dentro de esa rama `byInstant != 0` por construcción, así que `byInstant >= 0` y `byInstant > 0` valen lo mismo para **todo** valor posible. No es que falte oráculo: **ninguna prueba puede distinguirlos**. Comprobado además a mano: con el mutante aplicado, `ExecuteAutomationsTest` sigue en verde, como debe ser. No requiere acción. |

### 2.4 Justificados: accesores de `record` que nadie invoca (5)

| # | Clase : línea | Mutador |
|---|---|---|
| 20 | `AutomationEvent:9` `payload()` | EmptyObjectReturnVals |
| 21 | `TemplateValues:6` `eventType()` | EmptyObjectReturnVals |
| 22 | `TemplateValues:6` `occurredAt()` | NullReturnVals |
| 23 | `TemplateValues:6` `taskTitle()` | EmptyObjectReturnVals |
| 24 | `TemplateValues:6` `projectName()` | EmptyObjectReturnVals |

**Veredicto de los cinco: JUSTIFICADO, y la razón es concreta, no un encogimiento de
hombros.** Son accesores autogenerados de `record` que **ningún llamador ejecuta
dentro del ámbito**. El detalle importa: dentro del propio `record`, `TemplateValues.value(...)`
y `withoutTask()` leen los **campos** (`getfield`), no los accesores, y lo mismo hace
`AutomationEvent.uuid(...)` con `payload`. Fuera del `record`, nadie de la feature 30
llama a esos cinco métodos. Por eso salen NO_COVERAGE: no se ejecutan nunca, y PIT no
puede matar lo que no se ejecuta.

Matarlos exigiría escribir una prueba que llame al accesor **por llamarlo**, sin
ninguna cláusula del contrato detrás. Eso es exactamente la prueba placebo que el
motivo M14 condena en esta misma feature: un oráculo que comprueba su propio montaje.
No se hace. La alternativa limpia —quitar el componente del `record`— no cabe: el
componente es la forma del dato y otros consumidores lo construyen.

### 2.5 De otra feature, colateral en el ámbito (1)

| # | Clase : línea | Mutador | Veredicto |
|---|---|---|---|
| 25 | `ApplicationConfiguration:22` `lambda$enabledApiCredentialOwner$0()` | BooleanTrueReturnVals | **FUERA DE ALCANCE, con destinatario.** `ApplicationConfiguration` entra en la campaña de automatizaciones porque el ámbito la arrastra entera (113 mutantes, 112 muertos); esta lambda es del cupo de credenciales de la API, feature `integration_api`, no de la 30. No se juzga aquí ni se cierra aquí: **queda anotada para el carril de `integration_api`**, que es quien tiene el contrato para decidir. Ninguna cláusula de `features/automations.feature` la cubre. |

### 2.6 Hueco real de cobertura, declarado ABIERTO (1)

| # | Clase : línea | Mutador | Veredicto |
|---|---|---|---|
| 26 | `EventTask$OfWorkSession:11` `sessionId()` | NullReturnVals | **ABIERTO.** No es un accesor muerto: lo llama `AutomationMatcher:55`, en `taskOf(...)`. Sale NO_COVERAGE porque **ninguna prueba hace pasar por ahí un evento de sesión de trabajo**. `AutomationMatcherTest` sí ejercita la rama gemela de `projectOf` (`EventProject.OfWorkSession`, líneas 113-120), pero no la de la tarea. Lo que queda sin vigilar: una regla con trigger `WorkSessionClosed.v1` / `WorkSessionStateChanged.v1` / `WorkSessionExtended.v1` y `{{task.title}}` en la plantilla —combinación que el contrato admite y que `AutomationTemplateTest:25` ya da por válida—. **Oráculo que lo cierra:** en `AutomationMatcherTest`, un evento `WorkSessionClosed.v1` cuya `taskOf` resuelva a la tarea de la sesión, afirmando la tarea devuelta. No se escribe aquí porque el trabajo de este carril eran los supervivientes de M13-M22 y este mutante no aparece en ninguno de ellos; se declara para que no se pierda. |

---

## 3. Frontend: la cifra y el libro (motivos M18 y M21)

Contado sobre `frontend/reports/mutation-automations/mutation.json`, no sobre ninguna
bitácora:

| Fichero | Total | Killed | Timeout | Survived | NoCoverage |
|---|---|---|---|---|---|
| `src/automations.tsx` | 500 | 423 | 0 | 73 | 4 |
| `src/automations-api.ts` | 371 | 369 | 1 | 1 | 0 |
| `src/App.tsx` | 8 | 8 | 0 | 0 | 0 |
| `src/workspace.tsx` | 5 | 4 | 1 | 0 | 0 |
| **Total** | **884** | **804** | **2** | **74** | **4** |

**Puntuación medida: 91,18 %** ((804 + 2) ÷ 884 = 91,1765 %). El fichero grande,
`automations.tsx`, mide **84,60 %** (423 ÷ 500). Las dos coinciden con lo publicado:
aquí la aritmética estaba bien.

Lo que no estaba bien es **el rastro**, y era peor de lo que decía M13:

- `progress/verificacion_mutantes_automations.json`, que la bitácora declaraba como
  «el veredicto de cada uno», contenía **11 entradas**. El script lo sobrescribía
  entero en cada pasada, así que la última pasada con filtro (`R8`) borró todo lo
  anterior; el resto sólo existía en el historial de git de ese fichero.
- Reconstruido desde las **8 pasadas** que hay en el historial y fundido por
  `(fichero, nombre)`: **148 mutantes con veredicto** (147 MUERE, 1 SOBREVIVE). Ese
  libro completo queda ahora commiteado en el fichero.
- El script conserva **178 anclas**. De ellas, **121** tienen veredicto en el
  historial y **57 no lo tienen en ninguna parte** — nunca se juzgaron. Y hay **27**
  veredictos en el historial cuya ancla ya no está en el script (el racimo que
  desapareció). El fichero afirmaba cubrirlo todo y cubría el 8 %.

**Dos arreglos en `scripts/verificar-mutantes-automations.mjs`** (es un script, no
producción ni tests):

1. **El libro acumula.** Funde con lo que ya hay en lugar de sobrescribir, y al
   terminar imprime cuántas anclas siguen sin juzgar. Una pasada con filtro ya no
   borra el trabajo de las demás.
2. **Se acabaron los veredictos fantasma.** Hallazgo de esta sesión, y no es teórico:
   al correr el script en este worktree, `vitest` **no llegó a ejecutarse** (no había
   `node_modules`) y el script anotó tan tranquilo `SOBREVIVE`, porque «ninguna prueba
   falló». Confundía *nadie falla* con *nadie corrió*. Ahora exige ver la marca de una
   ejecución real de vitest en la salida y, si no la ve, anota `NO EJECUTADO`.
   Cualquier verdicto del libro anterior pudo nacer así.
3. Además, el script escribía sobre los fuentes de verdad y **si se interrumpía los
   dejaba mutados**. Ahora los restaura en `process.on("exit")` y en `SIGINT`/`SIGTERM`.

El único `SOBREVIVE` del libro se ha vuelto a medir **con vitest corriendo de verdad**
(suite de automatizaciones: 3 ficheros, 84 pruebas, verde) y sobrevive:

> `R8 223 limpieza de la lectura inicial -> undefined` — **EQUIVALENTE POR REDUNDANCIA,
> demostrado.** El efecto de la lectura inicial (`automations.tsx:217-230`) publica su
> controlador en `live.current` (`:219`), y la limpieza del efecto de montaje
> (`:186-191`) hace `live.current?.abort()` sobre **ese mismo controlador**. El
> `return () => controller.abort()` de `:229` aborta por segunda vez algo ya abortado.
> Por eso ninguna prueba lo nota, ni siquiera
> `@s43 cancels the reads still in the air when the page is left`, que sí afirma
> `signalOf("/api/v1/me/automations").aborted === true`. El contraste que lo confirma:
> el ancla gemela `R8 233` (lectura de proyectos) **sí muere**, porque ese controlador
> no se publica en ningún ref y su limpieza es la única que lo aborta.

---

## 4. Desenlace motivo a motivo

### M13 — la lista nominal con veredicto · **CERRADO**

Es este documento. Los 26 mutantes no muertos del informe tienen nombre, clase, línea,
mutador y veredicto escrito en la sección 2. Ninguno queda sin juzgar. Y el motivo
tenía razón en lo de fondo: leer la lista es lo que destapó los hallazgos — el
`AutomationBody:94` (nadie lo había mirado), la equivalencia de `AutomationCursor:20`,
el reventón de `AutomationTemplate:36` y el hueco de `EventTask$OfWorkSession`.

### M14 — la prueba placebo de @s3 · **CERRADO**

Primero se demostró que **no muerde**, que es lo que se pedía. Con el mutante real
aplicado a producción (`AutomationBody:48` devolviendo `null`), la clase entera
`AutomationsApiTest` pasó: **84 pruebas, 0 fallos**. La prueba comprobaba el eco de su
propio stub (`when(create.create(...)).thenReturn(rule(1, conditioned))` y luego
`$.condition.projectId` sobre la respuesta, que se construye a partir de ese mismo
`conditioned`).

Después se le pusieron dientes, con el mutante **todavía aplicado**, y quedó en rojo:

```
AutomationsApiTest > s3_acceptsAConditionOverAnOwnProject() FAILED
  org.mockito.exceptions.verification.opentest4j.ArgumentsAreDifferent
  Argument(s) are different! Wanted:
      AutomationDraft[... conditionProjectId=11111111-1111-4111-8111-111111111111, ...]
  Actual invocations have different arguments at position [1]:
      AutomationDraft[... conditionProjectId=null, ...]
```

Producción restaurada, verde. La aserción nueva es una sola línea —
`verify(create).create(eq("owner"), eq(conditioned))` — y mira el draft que sale del
parser, no el que entra por el stub.

### M15 — `ExecuteAutomations:143`, el filtro de la ejecución previa · **CERRADO**

Muerto con oráculo, no justificado. Prueba nueva
`s22_eachRuleRetriesOnItsOwnPreviousRunAndNeverOnItsSiblings`: un mismo evento con
ejecuciones previas de **dos** reglas —R1 `failed` (y primera en la lista, que es la
que `findFirst()` devolvería sin filtro) y R2 en `retry`—. Con el mutante `-> true`,
R2 hereda la fila de R1, la ve resuelta y se salta para siempre. Rojo acreditado:

```
ExecuteAutomationsTest > s22_eachRuleRetriesOnItsOwnPreviousRunAndNeverOnItsSiblings() FAILED
```

### M16 — los cinco NO_COVERAGE de `PostgresAutomationWork` · **CERRADO**, con H6 abierto

Los cinco muertos, cada uno acreditado por separado contra PostgreSQL real:

| Mutante | Prueba que lo mata |
|---|---|
| `:85` `startCursor` quita `::write` | `s16_theFirstCursorIsWrittenOnceAndALaterStartNeverDragsItBack` (nueva) |
| `:341` `write` quita `Runnable::run` | la misma |
| `:126` `record` quita `executeWithoutResult` | `s20_theRunOfARolledBackConfirmationIsWrittenApartAndTheRetryRenewsIt` (nueva) |
| `:126` `lambda$record$0` quita `::upsert` | la misma |
| `:316` `runOf` devuelve null | `s22_eachCandidateCarriesItsOwnPreviousRunsAndNobodyElses` (de otro carril, `866426dc`) |

**H6 sigue ABIERTO, y con reproducción exacta.** El juez pidió por escrito «arreglar o
justificar» el `ON CONFLICT (rule_id, event_id) DO UPDATE` de `upsert` (`:204-212`).
El riesgo es real y concreto: `upsert` **no lleva guarda de estado**, a diferencia de
`claim` (`:192`), que sí exige `AND status = 'retry'`. Camino: el worker A calcula su
resultado, el worker B confirma `succeeded` sobre la misma `(regla, evento)`, la
transacción de A revierte y A llama a `record()`, que **pisa el `succeeded` de B** con
un `retry`/`failed`. La prueba nueva de @s20 **no fija ese defecto como esperado** —eso
sería congelar el fallo, que es justo lo que M2 reprocha en otro sitio—: afirma sólo lo
que el contrato promete (la fila sobrevive a la reversión y el reintento renueva la
suya). Cerrarlo pide una decisión de contrato —añadir la guarda de estado a `upsert`, o
declarar que el último que escribe manda—, y eso no lo inventa un carril de mutación.

### M17 — `AutomationView$Rule::enabled` y su familia · **CERRADO**

Los seis accesores de la familia, muertos uno a uno. La causa era siempre la misma: el
fixture coincidía con el mutante. Todos los montajes tenían `enabled: true`,
`deliveryId: null`, `completionCriterion: ""` y `loopGuarded: false`, así que devolver
`true`/`null`/`""`/`false` era indistinguible del valor real.

Dos de los arreglos **no son cambios de gusto, son correcciones**: el contrato
(`features/automations.feature:399`) pide
`completionCriterion: "TaskCreated.v1 a las <occurredAt>"` y la prueba usaba `""`; y la
fila 4 de @s32 (`:428`) pide la preview del webhook con **tres** claves incluido
`eventId`, y la prueba contaba tres claves sin mirar nunca esa. Con el `eventId` a
null la cuenta seguía dando tres.

Escenarios nuevos cubiertos que antes no tenían oráculo en la capa HTTP: @s12 fila 1
(`:178`, regla pausada) y @s32 fila 3 (`:427`, `loopGuarded true`).

### M18 — la cifra no medida · **CERRADO**

Sección 1. **95,72 %**, calculado del XML. El 98,68 % proyectado va marcado como
proyección y con el aviso de que sólo el 95,72 % está medido.

### M19 — `Slf4jAutomationAudit` con cero mutantes · **CERRADO, con la causa nombrada**

Se descartan las dos sospechas del encargo, con evidencia:

- **El patrón no está muerto.** Barrido `patrones-muertos.mjs` sobre
  `backend/build.gradle.kts` contra los dos árboles de fuentes: **442 patrones, 0
  muertos**. El de la línea 577,
  `com.apptolast.organization.adapter.logging.Slf4jAutomationAudit*`, resuelve a la
  clase real.
- **El nombre es exacto.** La clase existe en
  `backend/src/main/java/com/apptolast/organization/adapter/logging/Slf4jAutomationAudit.java`.

**La causa es el interceptor `FLOGCALL` de PIT**, activo por defecto y no desactivado
en `build.gradle.kts` (allí sólo se apaga `FRECORD`). Descarta las mutaciones que caen
**dentro de una llamada a un framework de logging**. `runFinished` tiene un único
enunciado, `LOG.info(...)`, y sus cinco argumentos son parámetros pasados tal cual, sin
ninguna expresión que calcular: después del filtro no queda **nada** que mutar, y la
clase no aparece ni una vez en el XML.

Dos pruebas de que es eso y no otra cosa:

1. **Rastreo transversal.** En los seis informes de mutación del árbol —**3.819
   mutantes** entre `pitest-automations`, `-external-calendar`, `-github-connector`,
   `-integration-api-http`, `-noche-cinco` y `-webhooks`— hay **cero** apariciones de
   `removed call to org/slf4j/Logger::*`. Ni una.
2. **El contraste que lo clava.** `Slf4jExternalCalendarAudit` es de la misma forma
   —un `LOG.info(...)` y nada más— pero tiene un ternario entre los argumentos
   (`error == null ? "NONE" : error.name()`). Recibe **exactamente un mutante**:
   `NegateConditionals` en la línea 21, KILLED. Ni uno solo sobre la llamada. Es la
   expresión de los argumentos lo que se muta; la llamada nunca. Por eso la
   explicación anterior («ningún adaptador de bitácora recibe mutantes: es una
   propiedad de los mutadores») acertaba en el efecto y erraba en la causa: sí los
   reciben, si tienen algo que calcular.

**Y se cierra el hueco, no sólo se explica.** Como en la feature 25 con
`Slf4jWebhookAuditTest`, ahora existe `Slf4jAutomationAuditTest` (2 pruebas), que mide
el adaptador directamente y afirma las cinco claves que pide @s19 (`:266`). Acreditado
con los dos defectos que **PIT no puede generar nunca**:

```
borrar la llamada a LOG.info entera        -> FALLAN las 2 pruebas
intercambiar ruleId y eventId              -> FALLA s19_aFinishedRunIsNamedByItsFiveIdentifiers...
```

Esa línea del contrato ya no depende de una campaña que no la mira.

### M20 — `ExecuteAutomations:97`, la bitácora del camino de fallo · **CERRADO**

El motivo pedía «un veredicto escrito» y acepta que no es incumplimiento de contrato.
El veredicto es **muerto**, que es mejor que un veredicto. La prueba de @s20 ya existía
y no miraba el log; se le añadió la aserción de que el intento revertido se audita por
identificadores, igual que el bueno. Los **dos** mutantes de la línea mueren, cada uno
acreditado por separado:

```
quitar List::forEach          -> s20_aFailedConfirmationLeavesNothingBehind...() FAILED
quitar ::log dentro del lambda -> s20_aFailedConfirmationLeavesNothingBehind...() FAILED
```

Razón de fondo para no haberlo dejado en «justificado»: junto con la fila de `record`,
esa línea es **la única huella que sobrevive a la transacción revertida**. Sin ella, un
almacenamiento caído deja al propietario clavado en un evento sin una sola línea que
diga cuál ni por qué.

### M21 — la evidencia del frontend no reproducible · **CERRADO**

Sección 3: libro reconstruido a 148 entradas, script que acumula, guarda contra
veredictos fantasma y restauración de los fuentes ante interrupción. Con dos datos que
el motivo no tenía: **57 anclas sin veredicto en ninguna parte** y el fallo de
*«nadie falla» = «nadie corrió»*, que se manifestó en vivo.

### M22 — los diez supervivientes de `editingOf` · **CERRADO en veredicto, 2 kills ABIERTOS**

Los diez son exactamente los que dice el motivo (`automations.tsx` líneas 110-121,
verificados en el `mutation.json`). Pero no son diez cosas iguales, y agruparlos
escondía lo importante. La partición, con la razón de cada grupo:

| Mutantes | Grupo | Veredicto |
|---|---|---|
| `114:7` ConditionalExpression → true | **EQUIVALENTE** | La rama verdadera es `(rule.action.criterionTemplate ?? "")`. Para una acción `NOTIFY_WEBHOOK` esa propiedad no existe: vale `undefined`, y `?? ""` da `""` — **idéntico** a la rama falsa. Las dos ramas producen el mismo valor. Ninguna prueba puede matarlo. |
| `112:72` StringLiteral → marca<br>`116:11` StringLiteral → marca | **MATABLES, abiertos** | Son los únicos dos que llegan a la pantalla: `titleTemplate` y `criterionTemplate` **sí** tienen control (`automation-title`, `automation-criterion`). Con el mutante, abrir una regla de webhook mostraría `"Stryker was here!"` en esos campos. |
| `110:16`, `110:77` (projectId)<br>`118:7` ×2, `118:7` LogicalOperator, `119:7`, `121:11` (estimatedMinutes) | **INOBSERVABLES por construcción** | Siete mutantes sobre `projectId` y `estimatedMinutes`. Esos dos campos **no tienen ningún control en el editor** (los únicos cinco son `automation-name`, `-trigger`, `-condition`, `-title`, `-criterion`), así que no se pintan; y `actionOf` devuelve intacta la acción de una regla que no es `CREATE_TASK`, así que tampoco viajan en el cuerpo del PUT. No salen por ningún sitio observable. **No es un hueco de pruebas: es la consecuencia directa de M12.** Se cierran cuando se cierre M12 (dando control a esos campos) o cuando se borre la rama muerta. |

El motivo tenía razón en el diagnóstico: el arreglo de `actionOf` fue correcto y
mínimo, y **movió el síntoma fuera del alcance de las pruebas** en vez de cerrar la
rama. Siete de los diez lo demuestran.

**Por qué los dos matables quedan abiertos y no muertos.** El oráculo está escrito y
listo: abrir la regla de webhook en el editor y afirmar que **Título** y **Criterio**
aparecen vacíos (el andamiaje ya existe, `@s37 keeps the endpoint of a webhook rule
when only its name changes` abre justo ese editor). Lo que no existe es la **fila del
contrato**: ni @s37, ni @s38, ni @s40 dicen nada sobre qué debe mostrar el editor al
abrir una regla de webhook. Escribir la aserción sería fijar como esperado un
comportamiento que nadie ha aprobado, y la regla del carril es no inventar conducta.
**Se pide fila de contrato**, con este texto propuesto para los Examples de @s37:

> `| una regla NOTIFY_WEBHOOK | pulsa «Editar» | los campos de tarea del editor aparecen vacíos y el endpoint se conserva |`

Con esa fila aprobada, los dos mutantes caen en una sola prueba. Recordatorio de
proporción: M22 **no bloquea** —el fichero mide 84,60 % y el ámbito 91,18 %—.

---

## 5. Abiertos, en una lista

Todo lo que este carril **no** cierra, dicho como abierto y con destinatario:

1. **H6 · `PostgresAutomationWork.upsert` sin guarda de estado** (M16). Un `record()`
   tardío pisa el `succeeded` de otro worker. Reproducción en la sección M16. Pide
   decisión de contrato: guarda de estado o «el último que escribe manda». No se ha
   fijado en ninguna prueba, ni como bueno ni como malo.
2. **Los 2 mutantes matables de `editingOf`** (M22, `112:72` y `116:11`). Bloqueados
   por falta de fila de contrato; texto propuesto arriba.
3. **Los 7 inobservables de `editingOf`** (M22). Dependen de M12: campos sin control
   en el editor. Se cierran con M12, no antes.
4. **`EventTask$OfWorkSession::sessionId` sin cobertura** (mutante 26). Oráculo
   propuesto en la sección 2.6. Ningún evento de sesión de trabajo recorre `taskOf`.
5. **Las 57 anclas del script de frontend sin juzgar**, y las 27 cuyo ancla ya no
   existe. El script ahora las cuenta e imprime en cada pasada; juzgarlas es trabajo
   de otra sesión.
6. **`ApplicationConfiguration:22`** (mutante 25). Es de `integration_api`; para su
   carril.
7. **La campaña hay que volver a lanzarla.** Ninguna cifra de mutación del árbol
   describe hoy el árbol. La medida buena es 95,72 % (04:26); la proyección tras este
   carril es 98,68 %. Sólo el centro puede sustituirlas por una medida real.

---

## 6. Qué se tocó

**Nada de producción.** `git status` sobre `backend/src/main` y `frontend/src`, vacío.

| Fichero | Qué |
|---|---|
| `backend/.../adapter/AutomationsApiTest.java` | dientes a @s3; @s12 pausada; @s32 guardada; @s34 con deliveryId; @s30 y @s33 corregidos al contrato; fila nueva de @s8 |
| `backend/.../application/ExecuteAutomationsTest.java` | @s22 de dos reglas (M15); bitácora del fallo en @s20 (M20) |
| `backend/.../persistence/AutomationWorkPersistenceTest.java` | @s16 cursor inicial; @s20 `record` aparte (M16) |
| `backend/.../domain/AutomationTemplateTest.java` | marcadores pegados en @s18 |
| `backend/.../adapter/logging/Slf4jAutomationAuditTest.java` | **nuevo** (M19) |
| `scripts/verificar-mutantes-automations.mjs` | libro acumulativo, guarda anti-fantasma, restauración ante interrupción (M21) |
| `progress/verificacion_mutantes_automations.json` | reconstruido a 148 veredictos reales (M21) |

**Verde de las clases tocadas**, cada una por su nombre, nunca la suite entera:

```
AutomationsApiTest            88 pruebas   0 fallos
ExecuteAutomationsTest        20 pruebas   0 fallos
AutomationTemplateTest        15 pruebas   0 fallos
Slf4jAutomationAuditTest       2 pruebas   0 fallos
AutomationScheduleTest         3 pruebas   0 fallos
AutomationWorkPersistenceTest  6 pruebas   0 fallos   (Testcontainers)
frontend  src/automations      84 pruebas  0 fallos   (3 ficheros, vitest)
```
