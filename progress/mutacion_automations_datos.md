# Carril «seguridad y datos» de la feature 30 — motivos M8 a M12

Origen: `progress/carriles/bloqueantes_30.md`, lente «seguridad y datos».
Rama `claude/auto-datos`, worktree `C:/Users/vhurt/ow-worktrees/auto-datos`.

Todas las cifras y todos los mensajes de fallo de este documento están copiados
de una ejecución real. Ninguna prueba nueva nació verde: cada una lleva su rojo
acreditado abajo, con el texto que soltó.

Resumen: **cinco motivos cerrados, ninguno abierto. Cuatro defectos de
producción arreglados** (M8, M10, M11, M12) y un quinto punto ciego de oráculo
tapado (M9). Cuatro preguntas para el propietario al final; ninguna bloquea.

---

## M8 — `queue()` confundía «el endpoint ya no está» con «otro worker se me adelantó» — CERRADO

### El agujero

`PostgresAutomationWork.queue()` lanzaba `AutomationClaimedException` cuando el
`INSERT … SELECT` de la entrega no afectaba a ninguna fila. Ese INSERT no lleva
`ON CONFLICT` y el id de la entrega se sortea una línea antes, así que
`affected == 0` no puede significar nunca un reclamo perdido: sólo puede
significar que el SELECT no casó, es decir que el endpoint ya no es un endpoint
activo de este propietario.

`ExecuteAutomations` leía esa excepción como «ya lo hizo otro» y contestaba
`return true` **sin** llamar a `record()`, con la transacción `confirming` ya
revertida. Se perdían de golpe: la ejecución de la regla de webhook, la de
cualquier otra regla que casara el mismo evento (un CREATE_TASK ya aplicado en
el mismo `apply()`), su tarea, su `TaskCreated.v1`, y hasta la línea de
bitácora, porque `log()` sólo se llama tras un commit bueno. Y como el commit
del evento siguiente hace UPSERT del cursor a su posición absoluta, el evento
saltado no se volvía a leer jamás. Pérdida permanente y en silencio.

No hacía falta un atacante: `PostgresWebhookWork.disable` desactiva endpoints
solo, en el mismo proceso, así que la ventana entre `endpoints.isActiveEndpointOf`
—que se consulta FUERA de la transacción— y el INSERT es una condición de
producto normal.

### El rojo acreditado

Dos, uno por cada mitad del defecto.

**Mitad del ejecutor.** `ExecuteAutomationsTest`
`s21_anEndpointGoneBetweenTheCheckAndTheWriteSettlesItsRuleAndSavesTheRest`.
Primero por no compilar, que es la forma que tiene un rojo de decir que el
vocabulario no existe:

```
ExecuteAutomationsTest.java:775: error: cannot find symbol
> Task :compileTestJava FAILED
```

Con la clase ya creada, el rojo de conducta:

```
java.lang.AssertionError: no run for rule 5f3ca7ae-4939-4d84-9d9a-c024813ac83a
  at ExecuteAutomationsTest.outcomeOf(ExecuteAutomationsTest.java:457)
```

«No hay ejecución para esta regla» es literalmente el hallazgo: la regla se
evapora sin dejar fila.

**Mitad del adaptador.** `AutomationWorkPersistenceTest`
`s21_anEndpointNoLongerActiveIsNotAClaimAnotherWorkerWon`, contra PostgreSQL:

```
java.lang.AssertionError: [«el endpoint ya no está activo» y «otro worker se me
adelantó» no son la misma cosa]
Expecting actual throwable to be an instance of:
  com.apptolast.organization.application.AutomationEndpointGoneException
but was:
  com.apptolast.organization.application.AutomationClaimedException:
  Otro worker ya registró esta ejecución.
```

### El arreglo

- Nueva `AutomationEndpointGoneException(UUID endpointId)` en la capa de
  aplicación, con su Javadoc explicando por qué no es una `Claimed`.
- `PostgresAutomationWork.queue()` (:189) la lanza en lugar de la de reclamo,
  con el comentario que explica por qué `affected == 0` no puede ser un reclamo.
- `ExecuteAutomations`: el commit se extrae a `confirm(owner, event, outcomes)`,
  que la captura ANTES que la de reclamo y vuelve a confirmar el mismo evento
  con esa regla resuelta en `failed` / `ENDPOINT_NOT_FOUND`, que es lo que piden
  las filas 4 y 5 de @s21. Nada de la confirmación anterior llegó a escribirse,
  así que reconfirmar es correcto e idempotente. Cada pasada resuelve al menos
  un endpoint, de modo que la recursión no puede girar en el sitio.

El oráculo mira las dos reglas del evento, el cursor y la bitácora, no sólo la
fila del webhook: lo que se perdía no era sólo eso.

### Lo que queda anotado, no abierto

No hay prueba de extremo a extremo de la carrera real (endpoint desactivado
entre la comprobación y la escritura) porque el bean `webhookEndpointLookup`
comprueba `id`, `owner_id` y `status = 'active'`, exactamente lo mismo que el
SQL: no existe ningún camino determinista que pase el primer filtro y falle el
segundo sin una costura de prueba. Meter esa costura obligaría a sustituir el
bean para toda la clase `AutomationExecutionTest`, y he preferido no tocar el
contexto que comparten @s19, @s23, @s25 y @s26. Las dos mitades quedan medidas
por separado, cada una contra su pieza real.

---

## M9 — los predicados de aislamiento del `AutomationWork`, sin oráculo — CERRADO

### El agujero

Cuatro literales de SQL que decidían aislamiento y estado y que ninguna prueba
podía distinguir, porque cada test de `AutomationExecutionTest` se inventaba un
propietario nuevo y único: sin un segundo propietario en la base, borrar el
`WHERE owner_id = ?` no rompe nada, y PIT no muta cadenas.

Estado real al empezar, tras releer el árbol:

| predicado | dónde | estado |
| --- | --- | --- |
| `claim()` `AND status = 'retry'` | :192 | ya cubierto por `s22_onlyARowStillInRetryCanBeRenewedByALaterAttempt` |
| `withTheirRuns` `WHERE owner_id = ?` | :252-256 | ya cubierto por `s22_eachCandidateCarriesItsOwnPreviousRunsAndNobodyElses`, que mete una ejecución de un extraño sobre el mismo evento |
| `window()` `WHERE owner_id = ?` | :271-274 | **descubierto** |
| `queue()` `e.owner_id = ?` y `e.status = 'active'` | :167 | **descubierto** |

Los dos primeros los había cerrado ya `AutomationWorkPersistenceTest` (commits
`684536dd` y `866426dc`). Los otros dos son los que traía este carril.

### El rojo acreditado

Aquí el rojo no puede venir de la conducta —el predicado está en su sitio y la
prueba nacería verde—, así que lo he acreditado **mutando la producción a mano**
y comprobando que las pruebas nuevas caen. Las dos mutaciones conservan el
número de parámetros (`(owner_id = ? OR TRUE)`) para que el fallo sea el de la
aserción y no un desajuste de argumentos.

`theWalkOfOneOwnerNeverReadsTheOutboxOfAnother`, con `window()` mutado:

```
org.opentest4j.AssertionFailedError: [la ventana de candidatos es la de un
propietario, no la de la instalación]
Expecting actual:
  [11111111-1111-4111-8111-111111111111,
   22222222-2222-4222-8222-222222222222,
   3ec6a30d-ddb5-404c-87f1-7088a9a865ae,
   5a996525-f5e7-4345-a522-d44b94b00bb7,
   … ]
```

El ciclo de un propietario leyendo la outbox entera de la instalación.

`queueNeverHandsTheEventOfOneOwnerToTheEndpointOfAnother`, con `e.owner_id = ?`
mutado:

```
java.lang.AssertionError:
Expecting code to raise a throwable.
  at AutomationWorkPersistenceTest.queueNeverHandsTheEventOfOneOwnerToTheEndpointOfAnother
```

No lanzó nada: encoló. El payload del propietario A en la cola de entregas de B,
que es exactamente lo que el motivo anunciaba.

Producción restaurada después (`git diff` limpio sobre esas dos líneas) y la
clase verde.

### El arreglo

No hay defecto de producción que arreglar aquí: los cuatro predicados estaban
bien escritos. Lo que faltaba era el oráculo, y son dos pruebas nuevas de
persistencia con **dos propietarios en la base**:

- `theWalkOfOneOwnerNeverReadsTheOutboxOfAnother` — el evento del extraño ocurre
  ANTES que el propio a propósito: sin el predicado sería el primero de la lista.
- `queueNeverHandsTheEventOfOneOwnerToTheEndpointOfAnother` — el endpoint del
  extraño está **activo** a propósito: con uno inactivo, el predicado de estado
  taparía al de propietario y la prueba volvería a no discriminar.

La segunda cubre a la vez el `e.status = 'active'`, que es el que M8 necesitaba
(el otro test, `s21_anEndpointNoLongerActiveIsNotAClaimAnotherWorkerWon`, lo
ejercita con un endpoint propio desactivado).

---

## M10 — un dato envenenado de un propietario dejaba sin automatizaciones a todos — CERRADO

### El agujero

`runCycle()` era `for (var owner : work.ownersWithRules()) walk(owner);` sin
envolver nada. `walk()` dejaba escapar lo que lanzaran `work.cursor`,
`startCursor` y `work.after`, y `process()` calculaba los resultados —con
`matcher.loopGuarded`, que pide `AutomationEvent.uuid("taskId")` y revienta con
`IllegalArgumentException` si el payload no lo trae— FUERA del try. Cualquier
fallo atribuible a un propietario abortaba el ciclo para todos los que
`SELECT DISTINCT owner_id` devolviera después de él, indefinidamente, y
`AutomationSchedule.tick` sólo dejaba una línea por segundo con el nombre de una
clase: ni propietario, ni evento, ni causa.

### El rojo acreditado

Dos ciclos, uno por cada mitad del recorrido.

`s21_aPoisonedEventOfOneOwnerNeverLeavesTheOtherAccountsWithoutAutomations`:

```
ExecuteAutomationsTest > s21_aPoisonedEventOfOneOwnerNeverLeavesTheOtherAccountsWithoutAutomations() FAILED
    java.lang.IllegalArgumentException at ExecuteAutomationsTest.java:322
```

El ciclo entero se cae; el segundo propietario no llega a ejecutar nada.

`s21_anOwnerWhoseCursorCannotBeReadDoesNotTakeTheOtherAccountsDownWithHim`:

```
ExecuteAutomationsTest > s21_anOwnerWhoseCursorCannotBeReadDoesNotTakeTheOtherAccountsDownWithHim() FAILED
    com.apptolast.organization.application.StorageUnavailableException at ExecuteAutomationsTest.java:366
        Caused by: java.lang.IllegalStateException at ExecuteAutomationsTest.java:366
```

### El arreglo

- `AutomationAudit` gana `cycleFailed(String ownerId, UUID eventId, String category)`.
  Sigue admitiendo **sólo identificadores**: ni mensaje, ni payload, ni título.
  `eventId` es null cuando el fallo no es de un evento concreto, y la línea lo
  dice en lugar de callarse. `Slf4jAutomationAudit` la escribe como `warn`.
  Se mantiene el puerto y su adaptador: el dominio y la aplicación siguen sin ver
  slf4j, y `ArchitectureTest` pasa.
- `ExecuteAutomations.attempt(owner, candidate)` (:86) envuelve cada candidato.
  Al fallar **detiene el recorrido de ese propietario en ese evento en lugar de
  saltárselo**: el cursor se queda donde estaba, así que nada se pasa por alto en
  silencio, y la línea nombra propietario, evento y clase del fallo. Un
  aislamiento mudo cambiaría una avería general por una invisible.
- `ExecuteAutomations.walkGuarded(owner)` (:65) envuelve la mitad exterior —leer
  el cursor, arrancarlo, leer la ventana—, donde todavía no hay evento que
  nombrar.

De paso queda cubierto el `rows.forEach(work::record)` del catch, que estaba sin
protección: ahora cae dentro de la frontera por candidato.

`AutomationSchedule.tick` se deja como está: con la frontera por propietario, su
`catch` pasa a ser el último recurso de verdad y no el sitio donde se entierra
el diagnóstico.

---

## M11 — la entrega nacía sellada con el instante del evento — CERRADO

### El agujero

`queue()` sellaba `next_attempt_at`, `created_at` y `updated_at` de la entrega
con `notify.event().occurredAt()`. En el propio @s15 —el worker apagado que se
enciende y camina el atraso— toda entrega de automatización nacía con las tres
marcas en el pasado: el `createdAt` que el propietario ve en el registro de la
feature 25 (@s29) dejaba de significar cuándo se creó la entrega, y el índice
`webhook_deliveries_due (next_attempt_at, id) WHERE status='pending'`
(V23__webhooks.sql:53-54) las ponía por delante de todas las entregas legítimas.

Contraste que lo confirma: `PostgresWebhookOutbox.enqueue`, la otra puerta de
entrada a esa misma tabla, sella con `now`. Este adaptador era el único que no.

### El rojo acreditado

`s26_aQueuedDeliveryIsStampedWhenItIsQueuedAndNotWhenTheEventHappened`, primero
por no compilar (el adaptador no tenía reloj que inyectar, que es la razón de
fondo por la que la conducta no se podía medir):

```
AutomationWorkPersistenceTest.java:52: error: constructor PostgresAutomationWork
in class PostgresAutomationWork cannot be applied to given types;
```

Con el reloj ya en el constructor pero todavía sin usar en el sello, el rojo de
conducta:

```
java.lang.AssertionError: [las tres marcas son las del encolado, no las del
evento que lo provocó]
Expecting map: {"attempt"=0, "body"={… "occurredAt": "2026-09-08T10:00:01Z" …}
```

### El arreglo

`PostgresAutomationWork` recibe un `Clock` (quinto parámetro; `ApplicationConfiguration`
le pasa el bean que ya usan los demás casos de uso) y `queue()` sella con
`clock.instant()` (:169). El oráculo de @s26 en la clase de persistencia afirma
ahora las tres marcas, que era la tercera conducta de este adaptador sin ningún
testigo.

---

## M12 — el error de un campo aterrizaba en el campo de al lado — CERRADO

### El agujero

Era peor de lo que decía el motivo. El editor no expone control para
`action.projectId` ni para `action.estimatedMinutes`: una regla nueva apunta
siempre a `projects[0]?.id ?? ""`. Si la lista de proyectos no ha llegado o está
vacía, Guardar manda `projectId: ""`, el servidor contesta 400 sobre
`action.projectId`, y `controlIdOf` caía en su `?? "automation-name"`.

Consecuencia doble: el foco saltaba a **Nombre** —un campo que el servidor no
nombró y que no tiene ningún error— y el mensaje del servidor **no lo pintaba
nadie**, porque ningún control lee esa clave (`fields.name`,
`fields["action.titleTemplate"]` y `fields["action.criterionTemplate"]` son los
únicos que se leen). El propietario veía un Guardar que no hacía nada y un foco
señalando el campo equivocado.

Y estaba congelado como conducta esperada: `FIELDS` en `automations.test.tsx`
tenía una sexta fila, `["accion.desconocida", "automation-name"]`, que sancionaba
la rama por defecto. La prueba que cubría @s38 sólo ejercitaba
`action.criterionTemplate`, que sí está en el mapa.

### El rojo acreditado

`@s38 never pins a server error to a field the server did not name`:

```
TestingLibraryElementError: Unable to find role="alert"
 ❯ src/automations.test.tsx:270:25
   expect(await screen.findByRole("alert")).toHaveTextContent(/proyecto/i);
```

No hay alerta ninguna: el 400 del servidor no deja rastro visible.

### El arreglo

- `controlIdOf` deja de mentir: devuelve el control del campo, o **nada** si el
  editor no muestra ese campo. Fuera el `?? "automation-name"`.
- Nuevo `pinToTheirFields(errors)`, compartido por `save()` y `simulate()`, que
  eran la misma línea duplicada: el foco va al primer campo **que tenga
  control**, y el error de un campo que el editor no muestra va a la página como
  `role="alert"`, con su nombre delante (`UNSHOWN_LABELS`), para que el mensaje
  genérico del servidor («Este campo es obligatorio.») signifique algo.
- La sexta fila de `FIELDS` se retira, con la nota de por qué: @s38 pide «a SU
  campo», y de un campo que el editor no muestra Nombre no es el suyo. Lo que el
  editor haga con esos campos lo mide ahora la prueba nueva, que sí puede fallar
  por ello.

Una sola mutación cubre las dos aserciones de la prueba: si `controlIdOf`
volviera a contestar `"automation-name"` para todo, `unshown` quedaría vacío, no
habría alerta, y además el foco volvería a Nombre.

---

## Verde de cierre

Ejecutado en este worktree, con `E2E_WEB_PORT=18102` reservado y sin tocar
ningún otro:

| qué | resultado |
| --- | --- |
| `gradlew test --tests "…ExecuteAutomationsTest"` | verde (22 pruebas) |
| `gradlew test --tests "…AutomationWorkPersistenceTest"` | verde (8 pruebas) |
| `gradlew test --tests "…AutomationExecutionTest"` | verde |
| `gradlew test --tests "…AutomationScheduleTest"` | verde |
| `gradlew test --tests "…ArchitectureTest"` | verde |
| `gradlew test --tests "…AutomationWiringTest"` | verde |
| `gradlew test --tests "…AutomationPersistenceTest"` | verde |
| `gradlew test --tests "…AutomationsApiTest"` | verde |
| `gradlew compileTestJava` (todo el conjunto de pruebas) | verde |
| `gradlew spotlessApply` | aplicado |
| `vitest run` (suite entera del frontend) | verde, **90 ficheros, 3131 pruebas** |
| `prettier --write`, `tsc --noEmit`, `eslint` sobre los dos ficheros tocados | limpios |

**No he lanzado la suite entera del backend** (cada clase levanta un PostgreSQL y
hay más carriles en marcha), ni campañas de PIT ni de Stryker: las mide el centro.

---

## Preguntas para el propietario (ninguna bloquea; no he enmendado el contrato)

1. **¿El editor debería dejar elegir el proyecto de destino y la duración
   estimada?** Hoy una regla nueva coge `projects[0]` y no hay control para
   ninguno de los dos. He arreglado la mala atribución del error, pero la causa
   de fondo —que el editor compone campos que no enseña— sigue ahí, y el
   contrato no la cubre. Si la respuesta es que sí, es una feature, no un
   arreglo.
2. **¿Y si el propietario no tiene ningún proyecto todavía?** Hoy Guardar manda
   `projectId: ""` y come un 400. Ahora al menos lo dice; lo suyo sería que
   «Nueva regla» no se ofreciera, o que lo explicara antes. El contrato no dice
   nada.
3. **@s21, ¿el `attempt` de una regla resuelta por endpoint desaparecido en la
   carrera?** El contrato dice «attempt 1» para el endpoint borrado o
   desactivado *antes* del ciclo. En la carrera que arregla M8, el intento es el
   que tocara (1 en la práctica, porque la consulta al endpoint precede a
   cualquier reintento). Lo he dejado tal cual, sin forzarlo a 1.
4. **M10: detener el recorrido del propietario envenenado significa que ese
   propietario se queda parado hasta que alguien arregle la fila.** Es lo
   contrario de saltársela, y creo que es lo correcto —saltar sería perder en
   silencio, que es justo el pecado de M8—, pero deja una cuenta detenida con
   una línea de warn por ciclo. La alternativa (saltar y registrar) cambiaría la
   semántica del cursor, y eso sí sería enmendar el contrato.

---

## Nota de proceso

No he hecho `fetch`, ni `reset`, ni cambio de rama, ni rebase. La rama
`claude/auto-datos` parte de `65dcd72e` y sólo tiene encima mis commits. No hay
credenciales en ninguna prueba: el `secret_ciphertext` de los endpoints de
prueba es un byte de relleno (`\x01`), como en el resto del árbol.
