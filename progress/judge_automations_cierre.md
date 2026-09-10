# Review de cierre — feature 30 `automations` (el ejecutor)

**Veredicto: APPROVED CONDICIONADO A** las cuatro condiciones de la sección
final. La condición 1 es una prueba **roja hoy en `main`**: hasta que esté
verde, esto no es una aprobación, es una promesa de aprobación.

Alcance: la sesión del 10 de septiembre (`1acc2c4` … `9951147`, `60a3a4e`,
`d517a99`), contra `features/automations.feature` y
`progress/tdd_automations_fase2.md`. Solo lectura: no se ha ejecutado la suite
ni ninguna campaña. Se ha ejecutado **una** prueba suelta,
`node --test scripts/project.test.mjs -t "automations Stryker configuration"`,
para distinguir dos hipótesis sobre los rangos. Falló (ver H1).

---

## 1. ¿El ejecutor cumple de verdad los nueve escenarios?

Escenario a escenario, con el oráculo que lo sostiene y con lo que **no** mide.

| @s | Oráculo | Veredicto |
| --- | --- | --- |
| @s15 | `ExecuteAutomationsTest.java:119` (2 runs, 2 tareas, cursor en E2) + `AutomationScheduleTest.java:18` (el bean sólo con la propiedad) | **Cumplido**, con una aserción vacua (H5) |
| @s16 | `ExecuteAutomationsTest.java:135`: `work.started` = cursor en el `createdAt` de la regla más antigua; 40 eventos previos sin ejecución; E41 en el **mismo** ciclo | **Cumplido**. La lectura de «el presente» de `tdd_automations_fase2.md:257-261` es la única compatible con el Given, que pone E41 antes del ciclo |
| @s17 | `ExecuteAutomationsTest.java:154`: orden `(occurredAt, eventId)` con E1/E2 al mismo instante, la fila `blocked` que no produce y sí mueve el cursor, y el `containsExactly(tuple(e1,1),(e2,1),(blocked,0),(e4,1))` de `:176-179` | **Cumplido en lo esencial**, con dos huecos reales (H3, H4) |
| @s18 | `ExecuteAutomationsTest.java:187`, las dos filas, sobre el efecto real | **Cumplido**. Lo que ata el «valores vigentes» no es este test sino la fila `161 code point title` de @s21 (`:302-304`), que pone en el doble de `AutomationFacts` un título distinto del que lleva el payload y exige `TITLE_TOO_LONG`: sin eso, título de payload y título vigente son indistinguibles en @s18 |
| @s19 | `AutomationExecutionTest.java:46` contra Postgres: tarea raíz con `completion_criterion ""` y `estimated_minutes null`, `TaskCreated.v1` con `aggregate_id P` y `payload.taskId`, fila de ejecución completa, cursor. `queryForMap` da el «exactamente una» gratis. Bitácora en `ExecuteAutomationsTest.java:99` | **Cumplido**. La cláusula de atomicidad la sostiene @s25, no @s19 |
| @s20 | `ExecuteAutomationsTest.java:332`: `commits` vacío, una fila `retry/STORAGE_UNAVAILABLE` en `record`, cursor intacto en E0 y E2 **sin procesar** (`process` devuelve `false`, `ExecuteAutomations.java:100`) | **Cumplido** |
| @s22 | `ExecuteAutomationsTest.java:357` (orden `attempted == [E1,E2]`, `attempt 3/failed` conservando el id de la fila, E2 `succeeded`, cursor en E2) y `:391` | **Cumplido**, y bien cazado: en `:357` la cláusula «un ciclo posterior no crea un cuarto intento» es **vacua** porque el cursor ya pasó de E1; el artesano lo vio y escribió `:391`, donde el cursor sí relee la fila zanjada |
| @s23 | `AutomationExecutionTest.java:77`: dos hilos con `CyclicBarrier`, 1 run, 1 tarea, 1 evento, cero fallos. Lo sostiene el `ON CONFLICT (rule_id, event_id) DO NOTHING` de `PostgresAutomationWork.java:187` y el abandono por `AutomationClaimedException` de `:201` | **Cumplido**. El perdedor bloquea en el índice único hasta el commit del ganador y lee 0 filas: la carrera es determinista, no afortunada |
| @s25 | `AutomationExecutionTest.java:103` | **Cumplido, y el modelo es fiel**: verifiqué el orden de `PostgresAutomationWork.apply()` (`:132-150`), que crea el efecto **antes** de `claim()`. Por eso el `status "crashed"` que el CHECK rechaza revienta con la tarea ya escrita, y el `count(countAutomatedTasks()).isZero()` de `:114` discrimina de verdad. Si el `claim` fuera primero, esa aserción sería un placebo |
| @s26 | `AutomationExecutionTest.java:133`: entrega pendiente con `event_id`/`event_type`, `body` = `payload::text`, run `succeeded` con `delivery_id` y `created_task_id null`, outbox sin evento nuevo, endpoint suscrito **sólo** a `TaskCreated.v1` | **Cumplido**. El `o.payload::text` dentro del `INSERT ... SELECT` (`PostgresAutomationWork.java:164`) hace el «byte a byte» estructural; el oráculo discrimina contra una re-serialización en Java (jsonb imprime `": "`, Jackson no) |
| @s21 | `ExecuteAutomationsTest.java:267`, las cinco filas + R2 con su efecto + cursor | **Cumplido**, con una aserción vacua (H2) |
| @s24 | `ExecuteAutomationsTest.java:211` (cursor avanza, reactivar no resucita) y `:239` (`RacingRules`, `reads == 1`, plantilla íntegra, `hasSameSizeAs`) | **Cumplido**. El `reads == 1` es el oráculo bueno: mata el mutante «releer la regla para renderizar» |
| @s28 | `ExecuteAutomationsTest.java:439`, dos filas | **Cumplido** |
| @s29 | `ExecuteAutomationsTest.java:453`, con `guardConsultations` vacío | **Cumplido** |

**Refutación intentada y fallida** (esto es lo que me hace firmar): busqué los
modos de placebo que este proyecto ha pagado esta noche. Encontré cinco
aserciones que no pueden fallar (H2, H3, H5 y las dos de abajo), **ninguna de
ellas siendo el único oráculo de su cláusula**. En cada caso hay al lado una
aserción que sí discrimina. Es deuda de limpieza, no un contrato sin probar.

Las dos que no llegan a hallazgo numerado, por si se tocan:

- `ExecuteAutomationsTest.java:109`, `.doesNotContain("Redactar informe",
  "Marketing")`: es cierto **por construcción**, porque el puerto
  `AutomationAudit` no admite ni título ni nombre. Que la cláusula la garantice
  el tipo y no la prueba es una buena noticia; conviene que quede dicho.
- `AutomationExecutionTest.java:103`: la cláusula «las reglas y ejecuciones
  anteriores al reinicio se conservan sin cambios»
  (`features/automations.feature:333`) no tiene aserción porque no hay ninguna
  anterior. Es vacuidad del montaje, no del oráculo.

## 2. El rojo acreditado y los seis mutantes de control

Contrasté la narrativa contra el árbol. Los seis mutantes están **bien
elegidos**: cada uno ataca la propiedad que su escenario afirma, no una vecina.

- **A y B (@s18, `7187d92`).** A (efecto construido con la plantilla cruda) cae
  en las dos filas; B (nombre histórico fijo) cae **sólo en la fila 2**. Que un
  mutante mate una fila y no la otra es la firma de un mutante que discrimina la
  propiedad correcta: sin él, las dos filas de @s18 recorren el mismo camino con
  distinto literal.
- **C (@s24/@s17, `639d504`)**: saltarse el `commit` cuando ninguna regla
  dispara, la «optimización evidente». Su muerte cruzada en @s17 (fila bloqueada)
  y @s24 fila 1 prueba que el avance del cursor sobre omisiones deliberadas está
  atado en los dos sitios. Bien elegido.
- **D (@s24, `639d504`)**: releer la regla con `rules.find()` para renderizar.
  Muere contra `reads == 1` (`ExecuteAutomationsTest.java:249`). Es literalmente
  la mezcla de versiones que prohíbe `features/automations.feature:324`.
- **E (@s25/@s23, `9a1d1a0`)**: `PROPAGATION_NOT_SUPPORTED`. El
  `TransactionTemplate confirming` de `PostgresAutomationWork.java:58` es lo
  único que hace caer la tarea junto al run; el mutante ataca esa línea exacta.
- **F (@s23, `9a1d1a0`)**: `ON CONFLICT DO UPDATE` en el reclamo → dos tareas y
  dos `TaskCreated.v1`. **Es el mutante que más valía**, porque acredita que la
  carrera del test es real y no una serialización afortunada, que es el riesgo
  clásico de una prueba de concurrencia. Sin él la prueba no vale nada; con él,
  vale.

Los seis rojos «reales» (@s16, @s17, @s20, @s21, @s22, @s28) son coherentes con
el árbol: en @s21 el `switch` sellado de `ExecuteAutomations.java:151-154`
sustituye a un cast, que es lo que la bitácora dice que reventaba con
`ClassCastException` en las dos filas de webhook.

**Lo único que no se sostiene de la narrativa** está en H5: la bitácora
(`tdd_automations_fase2.md:247-248`) declara que @s15 «duerme 1,5 s con el
contexto deshabilitado y afirma cero ciclos». No es así.

## 3. @s28 y @s29: ¿era un agujero real?

**Medio sí, y la bitácora lo cuenta más grande de lo que fue.**

- Lo que **ya existía antes de esta noche**: `AutomationMatcherTest.java:164`
  (`loopGuarded` cierto para `TaskCreated.v1`/`SubtaskCreated.v1` de una tarea
  automatizada, falso para `TaskStatusChanged.v1` y `ProjectUpdated.v1`) y
  `AutomationEventTest.java:87` (`loopGuardTaskId` sólo en los dos tipos de
  creación). La **semántica** de la guarda estaba probada.
- Lo que **faltaba de verdad**: que alguien la **consultara**. Sin ejecutor no
  había quien lo hiciera, y `a164f2e` acredita que la primera versión del motor
  ignoraba `matcher.loopGuarded`. Las cláusulas «no existe ejecución de ninguna
  regla para ese evento» y «el cursor queda en ese evento»
  (`features/automations.feature:373-375`) no tenían oráculo. Ahora sí:
  `ExecuteAutomationsTest.java:439` y `:453`.

Conclusión: el agujero era real pero **estrecho** — la integración, no la
semántica. La bitácora (`tdd_automations_fase2.md:374-375`) debe decir eso.

## 4. Las cuatro dimensiones

**Contrato.** Los nueve del inventario, más @s18/@s21/@s24 y @s28/@s29:
cerrados. Ver el cuadro de la sección 1 y H2–H4.

**Oráculos.** Sólidos donde importa (`reads == 1`, el mutante F, el orden de
`apply()` que hace fiel el modelo de @s25). Cinco aserciones vacuas, ninguna
solitaria. Ver H2, H3, H5.

**Accesibilidad.** El hallazgo 9 del dictamen anterior está **cerrado de verdad**,
y bien: `e2e/automations-ux.spec.mjs:454-486` recorre con `Tab` real, compara
contra el orden del DOM **leído** y no contra una lista a mano (`:390-407`), mide
el anillo **en cada parada** y exige el `:focus-visible` del producto y no el del
agente de usuario (`:426-437`), y vuelve con `Shift+Tab` afirmando el inverso
**rotado una posición**, que es la forma correcta de matar una trampa de foco. El
oráculo de recorte de dos ejes con su mutación de control acreditada
(`tdd_automations_fase2.md:805-816`) es el que este repositorio ya validó en ics.
Sin objeciones. Nota menor, sin acción: `walk` (`:439`) hace `continue` sobre las
paradas ausentes de `expected`, así que una parada intrusa dentro de
`.automations` se toleraría.

**Seguridad — el ejecutor con lupa.** Es lo que más he mirado, por ser un
trabajador de fondo sobre datos de varios propietarios. El aislamiento está bien
construido, con cinturón y tirantes: `PostgresAutomationWork.java:273` acota la
ventana con `owner_id = ?`; `:252` acota las ejecuciones igual;
`AutomationMatcher.java:21` vuelve a comprobar `owner.equals(event.ownerId())`
aunque la consulta ya lo garantice; `AutomationRendering.java:38-46` pasa `owner`
a cada hecho; y el `webhookEndpointLookup` real
(`ApplicationConfiguration.java:687-694`) exige `owner_id` **y**
`status='active'`, con su oráculo de cuatro casos en
`AutomationWiringTest.java:141`. No he encontrado ninguna vía de fuga. Tres
endurecimientos que **no bloquean**: H6, H8, H9.

## 5. Las dos cosas que el artesano deja abiertas

**(a) Las filas `NOTIFY_WEBHOOK` de @s5, @s12, @s21, @s32 y @s33.**
La bitácora (`tdd_automations_fase2.md:473-474`) **se acusa de más de lo que
debe**. Comprobado una por una:

- @s5: **cerrado esta misma noche** en `AutomationWiringTest.java:141-156`
  (activo propio, desactivado propio, activo ajeno, desconocido, y la regla que
  sí se guarda).
- @s21 filas 4 y 5: **cerradas** en `ExecuteAutomationsTest.java:264-265`.
- @s32 y @s33: **ya estaban cerradas** con dobles —
  `SimulateAutomationTest.java:209` y `:222`, `AutomationsApiTest.java:711` y
  `:740`— y el encabezado del contrato (`features/automations.feature:7-8`)
  autoriza el doble explícitamente.
- @s12 **fila 4** (PUT que cambia la acción a `NOTIFY_WEBHOOK` conservando las 2
  ejecuciones): **es la única que sigue abierta**. Ningún test del árbol
  reemplaza una regla por una `NotifyWebhookAction`.

Veredicto: **deuda aceptable**, no bloqueante. Es **una fila** de un Outline
cuyo mecanismo ya está clavado por `AutomationWiringTest.java:98`, que afirma que
el `replace` devuelve **los mismos dos objetos íntegros** y no dos filas
cualesquiera. Pero la bitácora tiene que decir la verdad en las dos direcciones:
declarar abierto lo que se cerró es la misma infracción de `AGENTS.md:51` que
declarar cerrado lo que no se midió. → condición 3.

**(b) `app.automations.enabled` sin declarar en `application.properties`.**
Comprobado: no aparece en `backend/src/main/resources/application.properties`
(que sí declara `app.publisher.enabled` en `:12`), y `app.webhooks.enabled` de la
feature 25 está exactamente igual — sólo vive en `WebhookConfiguration.java:13`.
Además, el Given de @s15 (`features/automations.feature:215`) parte literalmente
de «ausente en configuración».

Veredicto: **deuda aceptable en el repositorio, bloqueante para el despliegue**.
No la convierto en condición de código porque el contrato la contempla y hay
precedente aprobado. Lo que sí es un límite que nadie debe descubrir en
producción, y que hoy no está escrito en ninguna parte: **el motor entero no se
ejerce nunca en la pila real**. Su evidencia es unitaria más una clase con
contenedor; ningún E2E lo enciende. → condición 4.

## 6. El cambio estructural: `AutomationAudit` y @s19

**Bien hecho, y el oráculo sigue midiendo lo que el contrato pide.** Verificado:

- `ExecuteAutomations.java` ya no importa `org.slf4j`; la guarda de
  `ArchitectureTest.java:23-29` («application sólo depende de java, domain y
  application») vuelve a ser cierta **sin relajarla**.
- La guarda importa con `DO_NOT_INCLUDE_TESTS` (`ArchitectureTest.java:13-14`),
  así que que `ExecuteAutomationsTest.java:77-78` instancie el adaptador **no**
  es una violación: es una decisión de prueba, y es la correcta.
- El oráculo mide la **línea real**: el test engancha un `ListAppender` al logger
  `organization.automations`, que es el nombre que fija
  `Slf4jAutomationAudit.java:18`, y afirma sobre `getFormattedMessage()`. Si el
  adaptador cambiara de logger o dejara de emitir, `logged()` sería vacío y el
  `.contains(...)` de `:107-109` caería. **No es un doble grabador**: eso sí
  habría vaciado el escenario, porque habría probado lo que el caso de uso pasa y
  no lo que se escribe. La elección está además justificada en el comentario
  `:75-76`, que es como se hacen las cosas.

Una sola objeción, y va a la condición 2: el adaptador nuevo **no está en el
ámbito de PIT** (H7).

---

## Hallazgos

### H1 [BLOQUEANTE] `bin/harness test` está ROJO hoy en `main`, en la guarda de esta feature

`scripts/project.test.mjs:2219` afirma `deepEqual` contra los rangos viejos
(`src/App.tsx:47:8-47:51`, `55:8-56:30`, `84:7-85:40`,
`src/workspace.tsx:128:10-133:22`), y
`frontend/stryker.automations.config.json:7-10` contiene hoy `49:8-49:51`,
`61:12-62:32`, `94:10-95:40` y `138:10-143:22`.

Ejecutado (la única prueba suelta de esta revisión):
`node --test -t "automations Stryker configuration" scripts/project.test.mjs`
→ `1..1 # fail 1`, `AssertionError … deepStrictEqual`, en `project.test.mjs:2219`.

`scripts/project.mjs:479-480` mete ese fichero en `bin/harness test`, así que la
suite del arnés está roja. Causa: `0ecc8d7` («feature 29: cableadas las rutas del
catálogo … la navegación sube a catorce») desplazó `App.tsx` y `workspace.tsx`,
actualizó el **fichero de configuración** y **no** la lista fijada en la guarda.
Es exactamente el fallo que anuncia `progress/hallazgo_rangos_stryker.md:11-14`,
con el matiz importante de que aquí **la guarda funcionó**: gritó. Lo que falta es
terminar de atenderla.

Dato para quien lo arregle, ya comprobado por mí imprimiendo el texto cubierto:
los cuatro rangos **nuevos** son correctos hoy (`automations = route ===
"/automatizaciones"`, `automations ? "Automatizaciones"`, `automations && username
? <Automations owner={username} />` y el `<RouteLink href="/automatizaciones">`).
Hay que mover la lista de la guarda, no la configuración. La comprobación por
contenido de `:2230-2248` pasaría; nunca llega a ejecutarse porque el `deepEqual`
revienta antes.

### H2 [MEDIA] @s21: «ningún ciclo posterior reintenta R1 para E» es una aserción vacua

`ExecuteAutomationsTest.java:292-294`. Tras el primer ciclo el cursor está en
`(T0+1, E1)`, así que el segundo `runCycle()` no vuelve a leer E1 y el
`hasSize(2)` es cierto pase lo que pase. Es la **misma** vacuidad que el artesano
detectó y corrigió para @s22 con `:391`; aquí quedó sin corregir.

Atenuante que evita subirlo de gravedad: la cláusula sí tiene oráculo, en ese
mismo `:391`, porque su `settledRun` (`:413-415`) es `status "failed"` —el caso
determinista de @s21— y ataca la misma guarda (`ExecuteAutomations.java:144`). No
es un agujero de contrato; es una aserción que promete lo que no comprueba.

### H3 [MEDIA] @s17: «executedAt no decreciente» no puede fallar, y el orden de los `createdAt` no se mide

Dos cosas sobre `features/automations.feature:237-238`:

1. `ExecuteAutomationsTest.java:171-174` afirma `isSorted()` sobre `executedAt`,
   pero el reloj es `Clock.fixed(...)` (`:79`): los tres valores son
   **idénticos**, y `isSorted` sobre iguales es cierto siempre. Vacua. El orden sí
   está probado, pero por la aserción de al lado (`:170`, `containsExactly(e1, e2,
   e4)`), que es la que trabaja.
2. «las 3 tareas creadas tienen `createdAt` en ese mismo orden» **no tiene oráculo
   en ninguna parte**: `AutomationEffect.CreateTask` no lleva instante y el
   unitario sólo afirma `hasSize(3)` (`:175`); @s17 no se replica contra Postgres.
   Es la única cláusula de los nueve escenarios que se queda sin nada.

### H4 [BAJA] @s17: el descarte del commit tardío lo hace el doble, no la producción

`FakeWork.after` (`ExecuteAutomationsTest.java:620-629`) filtra el evento `late`
él mismo, así que «no existe ejecución para T» es una propiedad del doble. Lo
salva a medias que el filtro delegue en `AutomationCursor.precedes`
(`AutomationCursor.java:18-24`), que **es producción de dominio** y que además
fija la comparación de UUID sin signo. Lo que no se prueba es su traducción a
SQL: el `(occurred_at, event_id) > (?, ?)` de `PostgresAutomationWork.java:273`.
Riesgo bajo, y la desviación de diseño que lo permite —retirar la ventana de
gracia, `tdd_automations_fase2.md:208-214`— está bien razonada contra el propio
Given, que acepta explícitamente perder el commit tardío.

### H5 [MEDIA] @s15: la evidencia de «no se lee ni se escribe nada» no es la que la bitácora dice

`AutomationScheduleTest.java:25-31`. `ApplicationContextRunner.run(...)` **crea y
cierra** el contexto dentro de la llamada, así que cuando llega el
`Thread.sleep(1500)` de `:29` no hay ningún contexto vivo y el
`assertEquals(List.of(), cycles)` de `:30` es cierto por construcción: 1,5
segundos de suite a cambio de nada. Quien mata el mutante del
`@ConditionalOnProperty` son los dos `assertFalse(containsBean(...))` de `:25` y
`:28`, que sí discriminan.

El defecto de verdad está en `tdd_automations_fase2.md:247-248`, que presenta ese
sueño como la evidencia de la primera mitad del escenario. Eso es declarar medido
lo que no se midió (`AGENTS.md:51`), en pequeño y en una bitácora que por lo demás
es honesta.

### H6 [BAJA, endurecimiento] `record()` puede pisar el `succeeded` de otro worker

`PostgresAutomationWork.java:204-212`: `upsert` hace `ON CONFLICT (rule_id,
event_id) DO UPDATE SET status = EXCLUDED.status, created_task_id = …`, sin guarda
de estado. El camino: si `apply()` revienta con un `DataAccessException`
**después** de que otro worker haya reclamado y confirmado esa `(regla, evento)`,
el perdedor escribe `retry/STORAGE_UNAVAILABLE` **encima** de la fila `succeeded`
del ganador y anula su `created_task_id`, dejando una tarea real sin ejecución que
la explique — justo el invariante que pinta `features/automations.feature:263`. El
camino normal está a salvo, porque el reclamo perdido lanza
`AutomationClaimedException` y `ExecuteAutomations.java:89` la captura **antes** de
la rama que graba. Es estrecho, pero es el único punto del ejecutor donde una
escritura no está acotada por el estado que espera encontrar.

### H7 [MEDIA] `Slf4jAutomationAudit` queda fuera del ámbito de PIT

`backend/build.gradle.kts:538-566`: `automationsClasses` no tiene ningún patrón
que alcance `com.apptolast.organization.adapter.logging.Slf4jAutomationAudit`. Las
**tres** clases hermanas sí están en el ámbito de su feature — `:129`
(`Slf4jConnectorAudit*`), `:451` (`Slf4jExternalCalendarAudit*`) y `:608`
(`Slf4jWebhookAudit*`)—, así que la convención existe y esta feature es la
excepción. La clase nació anoche en `d517a99` y es el único sitio donde vive la
línea que afirma @s19: la campaña la daría por cubierta sin generarle un mutante.
Es la cuarta vez que este proyecto tropieza con la misma piedra
(`AesGcmSecretCipher`, `ApiErrors`, y el propio `ExecuteAutomations` que el
artesano cazó en `60a3a4e`).

### H8 [BAJA, endurecimiento] Dos consultas del ejecutor sin predicado de propietario

Defensa en profundidad, no fuga: hoy los identificadores vienen siempre del
recorrido del propio propietario.

- `PostgresAutomationWork.java:166-167`: el `INSERT ... SELECT` de la entrega une
  `outbox_events o` por `o.event_id` **sin** `o.owner_id = ?`, aunque sí acota el
  endpoint.
- `:190-192`: el `UPDATE automation_runs` del reintento acota por
  `rule_id`/`event_id`/`status`, sin `owner_id`.

En un trabajador que recorre a todos los propietarios en el mismo proceso, añadir
el predicado cuesta dos palabras y quita una clase entera de error.

### H9 [BAJA] Un fallo determinista que llega tarde se etiqueta como transitorio

Si el proyecto pasa a `completed` **entre** el `preview` y el `commit`,
`CreateTask.java:23` lanza `ProjectCompletedException`, que no es
`DataAccessException` ni `TransactionException` y por tanto escapa del envoltorio
de `PostgresAutomationWork.java:118`; `ExecuteAutomations.java:92` la recoge como
`RuntimeException` y la sella como `STORAGE_UNAVAILABLE`. La consecuencia
observable es que ese `PROJECT_COMPLETED` acaba `failed` en el intento **2** y no
en el **1** que pide `features/automations.feature:282`. La decisión de capturar
`RuntimeException` está razonada y la comparto
(`tdd_automations_fase2.md:343-347`); lo que falta es la excepción a la excepción.

---

## Condiciones

**1 [BLOQUEANTE, minutos].** Dejar `bin/harness test` en verde: alinear
`scripts/project.test.mjs:2219-2226` con los cuatro rangos vigentes y **volver a
correr esa prueba**. No apruebo nada con la suite roja. Al hacerlo, sustituir el
`deepEqual` por algo que no vuelva a duplicar la constante —la lista literal y la
validación por contenido de `:2230-2248` dicen lo mismo dos veces y sólo una de
las dos se desplaza—; y llevar a la puerta humana la recomendación 2 de
`progress/hallazgo_rangos_stryker.md:74-78`, que quita la causa en vez del síntoma.

**2 [BLOQUEANTE] La puerta de mutación.** Exijo, por capa:

| Ámbito | Puntuación | Condición dura |
| --- | --- | --- |
| `automations-backend` (PIT) | **≥ 0,80** global, el umbral de `harness.config.json` | y **≥ 0,90 sobre `ExecuteAutomations`**: es el motor, se prueba con un doble en memoria y en milisegundos; ahí un superviviente no tiene coartada |
| `automations-frontend` (Stryker) | **≥ 0,80**, el `break` que ya declara la configuración | sobre `src/automations.tsx` y `src/automations-api.ts` |

**Clases que tienen que recibir mutantes sí o sí**, con el número de mutantes
generados **por clase** en el informe, porque el modo de fallo de este proyecto no
es la puntuación baja: es el **cero mutantes** que pasa por verde.

1. `application.ExecuteAutomations` — el motor. Entró en el ámbito en `60a3a4e` y
   **nunca** ha recibido una campaña.
2. `adapter.persistence.PostgresAutomationWork` — el reclamo, la transacción y el
   `INSERT ... SELECT` de la entrega. Es donde viven @s23, @s25 y @s26.
3. `adapter.config.AutomationSchedule` y `AutomationConfiguration` — @s15.
4. `domain.AutomationCursor` — el `precedes` con la comparación sin signo. Es la
   producción en la que se apoya el doble de @s17, así que es la única red que le
   queda a esa cláusula (H4).
5. `adapter.logging.Slf4jAutomationAudit` — **hay que meterlo antes en el ámbito**
   (H7), o @s19 se mide sobre código que nadie muta.
6. `adapter.config.ApplicationConfiguration` — ya está (`:565`), y es donde viven
   los tres beans nuevos y el `webhookEndpointLookup` real.

Y registrar en `progress/mutation_automations_*.md` la **lista nominal de
supervivientes** con un veredicto escrito para cada uno, no sólo el porcentaje.
Antes de lanzar el frontend, imprimir con `scratchpad/rangos.mjs` el texto que
cubre cada rango y pegarlo en el informe: una campaña sobre un rango desplazado
mide si el árbol compila (`progress/hallazgo_rangos_stryker.md:34-36`).

**3 [no bloqueante, minutos].** Corregir `progress/tdd_automations_fase2.md` en
los tres puntos donde no dice la verdad, en las dos direcciones: `:473-474` (de
las cinco filas `NOTIFY_WEBHOOK` sólo sigue abierta @s12 fila 4); `:247-248` (el
sueño de 1,5 s ocurre con los contextos ya cerrados, H5); `:374-375` (@s28/@s29 ya
tenían oráculo de semántica en `AutomationMatcherTest.java:164` y
`AutomationEventTest.java:87`; lo que faltaba era que el ejecutor consultara la
guarda).

**4 [no bloqueante, minutos].** Dejar escrito, en la bitácora y en la nota de
entrega al despliegue, el límite que hoy no está en ningún sitio: **el motor no
corre en ninguna pila real**. Ni la de E2E ni el despliegue lo encienden mientras
`app.automations.enabled` no se declare, y ningún E2E lo ejerce. Quien decida
encenderlo debe saber que ése será el primer ciclo del worker que corra fuera de
una prueba.

**Deuda registrada, sin condición:** H2, H3, H4, H6, H8, H9. H6, H8 y H9 piden una
decisión **escrita** —arreglar o justificar—, no silencio: es la regla que esta
misma feature aplicó bien al exceptuar los `INPUT` del oráculo de recorte
(`tdd_automations_fase2.md:795-803`).

---

## Cierre

El ejecutor está bien construido y bien probado. Lo que me convence no es el
recuento de escenarios sino tres detalles que sólo aparecen mirando de cerca: el
orden de `apply()` que hace **fiel** el modelo de caída de @s25; el mutante F, que
convierte una prueba de concurrencia en una prueba de concurrencia **de verdad**;
y que el propio artesano detectara y cerrara la vacuidad de @s22 escribiendo un
segundo test. Las dos desviaciones de diseño (`tdd_automations_fase2.md:206-230`)
están argumentadas contra el contrato y no contra la comodidad, y la retirada de
la ventana de gracia se apoya en la lectura correcta del Given de @s17.

Lo que impide firmar hoy no es el trabajo de la noche: es una prueba roja que otra
feature dejó atrás, y una campaña de mutación que aún no tiene cifra.
