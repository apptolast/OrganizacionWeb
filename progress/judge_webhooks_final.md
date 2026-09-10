# Review — feature 25 webhooks (`features/webhooks.feature`)

**Veredicto: CHANGES_REQUESTED** — 4 bloqueantes, 4 no bloqueantes. 10 de septiembre de 2026.

Encargo: comprobar, no aprobar, los trece cierres declarados de `progress/cierre_25.md`.
Todo lo que sigue está recomputado o citado con fichero y línea. No he ejecutado
`bin/harness init` ni campaña alguna: el encargo lo prohíbe expresamente, y eso deja
una puerta sin medir que declaro abajo en vez de darla por buena.

---

## Lo que comprobé y sí se sostiene

**La puerta de backend, 92,81 %.** Recomputada por mí del XML:
`backend/build/reports/pitest-webhooks/mutations.xml` da `KILLED 400, SURVIVED 17,
NO_COVERAGE 14` → 400/431 = **92,81 %**, exactamente lo publicado. La campaña corrió
sobre `af581554` y `git diff af581554 HEAD -- backend/src/main` sale **vacío**: el
fuente medido es el de `main`. La corrección del SHA (condición 11) es correcta.
Los 31 sin matar están nominados uno a uno.

**La puerta de frontend, 94,57 %, es real.** Y costó encontrarlo, ver bloqueante 2.
Recomputado: `Killed 592, Survived 33, NoCoverage 1, RuntimeError 1` → 592/626 =
**94,57 %**. Comparé byte a byte el `source` que el informe guarda de cada fichero
contra `frontend/src/`: los dos coinciden. La cifra describe el árbol de `main`.

**Las cuatro enmiendas llevan su nota fechada en el `.feature`**, y las cuatro tienen
oráculo que muerde:

| Enmienda | Nota en el `.feature` | Oráculo |
|---|---|---|
| R4 `latencyMs` al cronómetro monótono | `:345-351`, que cubre `@s25` y, nombrándolo, `@s17:257` | `JdkWebhookSenderTest.s25_latencyMsComesFromTheInjectedTicker` |
| R5 el plazo de `@s32` a 2500 ms | `:439-446`, y el `When` de `:438` ya dice 2500 | `WebhookScheduleTest.s32_b9_...` (la anotación) y `DispatchWebhooksTest.s32_aCycleStopsAtTwentyDeliveriesAndLeavesTheRestForTheNextOne` (el 20+5) |
| R6 el catálogo a ocho | `:363-374`, y `project-spec.md:2038` enmendado en su sitio | `webhooks-client.test.ts:520` y `webhooks.test.tsx:1357`, celda a celda, y las dos se pusieron rojas al devolver el catálogo a siete |
| R10 el dato adicional ata propietario y endpoint | `:128-137`, con la fila nueva en `:127` | `AesGcmWebhookSecretsTest.s8_b6_theAssociatedDataBindsBothTheOwnerAndTheEndpoint:39-41`, las dos mitades |
| R11 la clave malformada impide arrancar | `:146-158`, y las seis filas del `Examples` dicen `ausente` | `AesGcmWebhookSecretsTest.s9_b5_aMalformedCurrentKeyFailsFastInsteadOfDegrading:87-102` |

**Condición 12 cerrada, y bien.** El rojo está acreditado con su texto y con el mutante
que lo demuestra: con los tres `LOG.*` comentados la prueba vieja fallaba en la 145
—el `assertEquals(3, ...)`— y los seis `assertFalse` pasaban. Ésa es la prueba de que
no dependían del sujeto. La sustituta envenena de verdad los cuatro parámetros de texto
libre y cuenta un eco por argumento.

**Condición 13 cerrada.** El contador del resolutor de `CreateWebhookTest` es de conducta,
y el mutante elegido —el «calentar el DNS» silencioso— es el correcto: no lo caza `s34`,
así que demuestra que el contador hacía falta. La mitad estructural lleva aserción de
control sobre `DispatchWebhooks`, que es justo lo que faltaba en la condición 12.

**Condición 7 cerrada.** `progress/judge_webhooks_cierre.md:86-87` y `:260` llevan
tachada la frase «la ÚNICA cláusula» con su corrección fechada, y su 6.3 corrige las tres
citas. Además su 6.2 exigía que el límite de `field-sizing` dejara de vivir en una
bitácora de carril: está en `project-spec.md:2050`, «Límites explícitos». Cumplido.

**Cobertura de escenarios: los 42 `@s` tienen al menos un test.** Recorrido a máquina
sobre `backend/src/test`, `frontend/src` y `e2e`: ninguno queda a cero, y el mapa
`@s → test` de `progress/tdd_webhooks.md:190-231` existe y casa.

**Arquitectura.** Las 45 clases de producción respetan el hexágono: `domain/` sin
dependencias, puertos en `application/` (`WebhookSender`, `WebhookSecrets`, `WebhookWork`,
`WebhookOutbox`, `WebhookAudit`), adaptadores en `adapter/{http,persistence,webhook,logging,config}`.
Ninguna clase de `application` importa Spring ni JDBC.

---

## Cobertura de escenarios (@s ↔ test)

- @s1..@s42: **[x]** todos cubiertos. Sin huecos de escenario.
- Dos cláusulas dentro de escenarios cubiertos siguen sin oráculo: ver no bloqueante 8.

## Disciplina TDD

- **¿Evidencia de Rojo→Verde→Refactor?** SÍ, y de la buena. `progress/cierre_25_carril.md`
  pega el texto del fallo, no lo resume: el `Confirmación incompatible` con su traza
  (`:47-53`), el `expected: <4> but was: <5>` del eco duplicado (`:167-171`), el
  `expected: <[]> but was: <[example.com x5]>` del resolutor (`:220-228`). Cada rojo
  nombra el mutante que lo produjo y se deshizo (`git diff backend/src/main` vacío tres veces).
- **¿Producción sin test que la pida?** SÍ, una:
  `backend/src/main/java/com/apptolast/organization/application/WebhookStatusSource.java`.
  Ver no bloqueante 7.

## Calidad

Sin hallazgos de artesanía que bloqueen. Funciones cortas, nombres reveladores, contrato
de errores correcto (`problem+json` con código estable, y la prioridad de
`project-spec.md:2019` sujeta por `CreateWebhookTest` y `WebhookApiTest`). Lo que sigue
es de **veracidad del registro**, que en este repositorio es donde está el daño.

## Checkpoints

- **C1** [ ] — los ficheros base y los docs están; `bin/harness init` **no verificado**
  (prohibido por el encargo, y sin registro sobre el árbol de cierre). Ver bloqueante 4.
- **C2** [x] — dos features en `in_progress` (25 y 28), permitido por
  `harness.config.json` → `one_feature_at_a_time: false`. `feature_list.json` tiene 27
  features. `progress/current.md` describe la sesión activa.
- **C3** [x] — con la salvedad del no bloqueante 7.
- **C4** [ ] — no verificado, misma razón que C1.
- **C5** [x] — árbol limpio (`git status --porcelain` vacío).
- **C6** [ ] — cada `@s` tiene test, pero el contrato de la sección 25 de
  `project-spec.md` se contradice a sí mismo en dos puntos. Ver bloqueante 1.
- **C7** [x] — las dos puertas por encima del 80 %, recomputadas por mí, con los
  supervivientes nominados. Con los defectos de registro de los bloqueantes 2 y 3.

---

# Cambios requeridos

## Bloqueantes

### 1. `project-spec.md:2026` sigue prometiendo las dos cosas que R10 y R11 acaban de enmendar

- **Bloqueante:** sí · **De:** orquestador · **Estimación:** 20 min

La línea 2026, dentro de la **sección 25**, dice literalmente:

> «...nonce aleatorio de 12 bytes y **AAD igual al `id` del endpoint**... **Clave ausente
> o mal formada: la aplicación arranca**, `audit.workerError("CONFIGURATION_ERROR")`, el
> worker es no-op y las operaciones que necesitan clave responden 503 `CONNECTORS_DISABLED`.»

Son **exactamente** las dos afirmaciones falsas que las condiciones 6 y 4 del veredicto
anterior mandaban corregir. Producción usa `ownerId + "|" + endpointId`
(`AesGcmWebhookSecrets.java:135-136`) y `AesGcmWebhookSecrets.from` **lanza** con clave
malformada (su test lo fija en `:87-102`). Las dos enmiendas se escribieron en el
`.feature` y **no** en `project-spec.md`, aunque R4 y R6 sí llegaron a él (`:2038`) y el
límite de `field-sizing` también (`:2050`). O sea: no fue por criterio, fue por olvido.

Que exista la nota general de `project-spec.md:2496` —«los datos autenticados asociados
atan el texto cifrado al propietario además del identificador del recurso... fallo rápido
al arrancar si la clave está mal formada»— **no lo salva**: un documento que dice A en la
2026 y no-A en la 2496 no está enmendado, está contradicho. Y el `.feature` manda leer
esa sección: su preámbulo, `features/webhooks.feature:5`, dice «Rutas, DTO cerrados,
códigos de error, firma, plazos y límites son los de la sección 25 de project-spec.md».

Peor: `features/webhooks.feature:130-133` justifica R10 escribiendo que «La politica ya
estaba ratificada en project-spec.md (los datos asociados atan propietario ademas de
recurso)». Es cierto en la 2496 y **falso en la sección 25**, que es donde el lector va
a mirar. La nota de enmienda se apoya en una cita que su propio documento desmiente.

**Trabajo:** enmendar `project-spec.md:2026` en su sitio, con la nota fechada al modo de
la de `:2038`, para que diga AAD = propietario + endpoint, y para que separe «clave
ausente» (arranca degradado) de «clave presente pero malformada» (no arranca). Citar
R10 y R11.

### 2. La puerta de frontend no es auditable desde `main`: el único informe del repositorio mide el árbol anterior al arreglo

- **Bloqueante:** sí · **De:** orquestador · **Estimación:** 15 min

`progress/mutation_webhooks_frontend.md` y `progress/cierre_25_carril.md:260-291`
publican el 94,57 % como medido «sobre un árbol que sí incluye la octava clase».
Comprobé el artefacto que hay en el repositorio,
`frontend/reports/mutation-webhooks/mutation.json`, y el `source` que guarda de
`src/webhooks-client.ts` **no tiene** `SECRET_UNREADABLE` **ni** `webhookErrorClasses`:
es la versión de siete clases, 265 líneas contra las 273 del disco. Ese informe recomputa
592/626 = 94,57 % **por coincidencia**, con la misma cifra global y el mismo desglose por
fichero. Un juez que haga en `main` la comprobación que la bitácora invita a hacer
—recomputar del `mutation.json`— obtiene el número correcto de un árbol equivocado. Es
la trampa exacta que este repositorio lleva días pagando.

La campaña buena **existe** y la verifiqué: vive fuera del repositorio, en
`C:/Users/vhurt/ow-worktrees/wh-bloqueantes/frontend/reports/mutation-webhooks/mutation.json`
(18:17). Comparé sus dos `source` byte a byte contra `frontend/src/webhooks.tsx` y
`frontend/src/webhooks-client.ts`: **coinciden los dos**, y recomputa 592/626 = 94,57 %.
**La puerta está pasada de verdad**; lo que falta es poder demostrarlo desde `main`.

**Trabajo:** traer al árbol el informe posterior al arreglo o, si `frontend/reports/`
está ignorado, registrar en la bitácora su ruta exacta, su marca de tiempo y el hash de
los dos `source` que mutó; y decir cuál de los dos informes es el canónico. Hoy el que
se encuentra es el caducado.

### 3. La bitácora canónica de frontend publica seis líneas de la campaña superada y apunta como «acta» a la campaña anterior

- **Bloqueante:** sí · **De:** orquestador · **Estimación:** 10 min

`progress/mutation_webhooks_frontend.md:47-52` sitúa los seis supervivientes de
`src/webhooks-client.ts` en las líneas **67, 72, 96, 96, 109, 113**. En la campaña que
mide el árbol de hoy están en **73, 78, 102, 102, 115, 119** — exactamente +6, que es lo
que crece el fichero con el bloque de `SECRET_UNREADABLE`. Son las líneas del informe
caducado. (Las 28 de `webhooks.tsx` sí son correctas: ese fichero no cambió.)

Y en `:8` nombra `progress/mutacion_webhooks_frontend_medida.md` como «el acta de la
campaña», cuando ese fichero documenta la campaña **anterior**, sobre el SHA `1239ad0e`,
y lista **35** sin matar, no 34. La bitácora dice existir para que «no haya dos verdades»
y publica las dos.

**Trabajo:** rehacer las seis filas con las líneas de la campaña vigente y marcar
`mutacion_webhooks_frontend_medida.md` como superado, o reescribirlo con la campaña buena.

### 4. La condición 9 —árbol en verde— es la única de las trece que nadie declara cerrada, y sigue abierta

- **Bloqueante:** sí · **De:** campaña / coordinador · **Estimación:** 60 min

El propio carril lo dice, `progress/cierre_25_carril.md:328`: «`bin/harness init` en
verde: no lo ejecuté; es la condición 9 y la corre el coordinador sobre el árbol
integrado». Y el árbol se movió después de todo verde registrado: `af581554..HEAD` toca
`backend/src/test` (`Slf4jWebhookAuditTest` +58, `CreateWebhookTest` +84,
`ManageWebhookTest` +29), `frontend/src/webhooks-client.ts`, el `.feature` y
`project-spec.md`. Lo que hay es evidencia **por clase** —77/77, 167/167, 7/7, 13/13,
6/6, `tsc` limpio—, que no es lo que pide la condición.

No lo ejecuté yo: el encargo prohíbe la suite entera. Lo dejo abierto en vez de
suponerlo verde.

**Trabajo:** `bin\harness.ps1 verify` sobre `main` y pegar el resultado. Nota de riesgo
menor que conviene cubrir de paso: la campaña de backend corrió **antes** de que el
carril reescribiera `Slf4jWebhookAuditTest`. La clase salía 3/3 al 100 %, así que aun
perdiendo esas tres muertes la puerta seguiría en 397/431 = 92,11 %. No hace falta
remedir; sí conviene que quede escrito.

## No bloqueantes

### 5. El javadoc de `WebhookScheduleTest` sigue declarando abierto lo que R5 cerró

- **Bloqueante:** no · **De:** carril · **Estimación:** 5 min

`backend/src/test/java/com/apptolast/organization/adapter/config/WebhookScheduleTest.java:224-230`
dice todavía: «el When de @s32 dice "transcurren **1500 ms** desde el arranque"... El
contrato leído al pie de la letra **no se cumple**, y enmendarlo es del propietario, no
de este carril. La pregunta exacta está escrita en `progress/decisiones_pendientes.md`;
hasta que se ratifique, la cláusula temporal de @s32 **sigue declarada abierta**».

R5 lo ratificó hoy, `features/webhooks.feature:438` dice 2500 ms, y
`progress/decisiones_pendientes.md` está vacío desde las 19:15. Es la misma falta que la
condición 12 del veredicto anterior —un javadoc que afirma lo que no es— y ha quedado
justo en el fichero del que iba la enmienda.

### 6. «6 mutantes vivos» en las guardas de aborto: son 12

- **Bloqueante:** no · **De:** orquestador · **Estimación:** 10 min

Respondo a la pregunta directa del encargo: **el condicionamiento se sostiene en el
fondo, y no en la aritmética.**

`progress/mutation_webhooks_frontend.md:39` dice «Hay **6 mutantes vivos** en esas
líneas» y monta el recuento del denominador 620 sobre ese seis. Contando los
supervivientes de **todas** las guardas de `aborted` de `webhooks.tsx` —líneas 119, 122,
125, 151, 158, 161, **216, 227, 256, 269, 281, 288**, todas ellas
`if (signal.aborted) return` o `if (!...aborted) ...`— hay **12** `ConditionalExpression`
supervivientes. La condición 10 original ya nombraba once líneas, no seis.

**La conclusión no cambia y el fallo del juez anterior queda firme:** descontando los
doce, 592/614 = **96,4 %**, dieciséis puntos por encima del umbral. Las guardas nunca
fueron el margen, contadas como seis o como doce. Pero el número publicado es la mitad
del real y es **el número sobre el que se condicionó el fallo**, así que hay que
corregirlo y rehacer la segunda tabla.

### 7. `WebhookStatusSource` es producción de la 25 que ningún `@s` de la 25 pide

- **Bloqueante:** no · **De:** orquestador (declararlo); la cirugía de `claude/retirar-29` ya lo ejecuta · **Estimación:** 5 min

`backend/src/main/java/com/apptolast/organization/application/WebhookStatusSource.java`
son 69 líneas cuyo javadoc empieza «La fila de los webhooks (**25**)» e implementa
`ConnectorStatusSource`: existe **sólo** para el catálogo de conectores de la feature 29,
retirada hoy. Ningún escenario de `features/webhooks.feature` lo menciona, y su único
oráculo, `ConnectorStatusSourcesTest`, es de la feature retirada y desaparece con ella.
`claude/retirar-29` ya lo borra: comprobado contra el merge-base `9484aa97`, de todo lo
que esa rama toca lo único de webhooks son este fichero y `NotifyWebhookAction`, que es
de la 30.

Sin consecuencia para la cifra: **no está en el ámbito PIT** —no aparece en las 41 clases
de la tabla del acta—, así que el 92,81 % no se mueve al borrarlo.

**Y respondo a lo que se me preguntó sobre el alcance:** ningún escenario del contrato
depende de la 30 para tener sentido. La fuente es la outbox (feature 23), que se queda;
el acoplamiento 25↔30 iba en el sentido contrario —la 30 consumía `WebhookEndpointLookup`
y `NotifyWebhookAction` de la 25—. Lo que la 25 pierde es su único productor interno de
tráfico, y eso ya está escrito en `ratificaciones.md` R9. El único apoyo de la 25 en
código que va a desaparecer es este `WebhookStatusSource`.

### 8. Dos cláusulas de escenarios cubiertos siguen sin oráculo, una de ellas declarada y no cerrada

- **Bloqueante:** no · **De:** carril · **Estimación:** 30 min

- `progress/literales_sin_oraculo_webhooks.md:149-153` deja escrito y **abierto**:
  `ORDER BY d.next_attempt_at, d.id` de `PostgresWebhookWork.java:67` sigue sin oráculo,
  «con `DESC` el worker serviría siempre lo más nuevo primero y las entregas viejas se
  quedarían sin enviar». Eso sostiene el «en ese orden» de `@s20:289` y el «y después los
  dos eventos posteriores, en orden» de `@s28:402`. PIT no lo puede ver: vive dentro de
  un literal de cadena. Está honestamente declarado; sigue sin cerrar.
- `features/webhooks.feature:155-157` declara un «hueco» **mayor del que hay**: el
  arranque que falla con clave malformada sí tiene oráculo en la suite de la propia 25,
  `AesGcmWebhookSecretsTest:87-102`, no sólo en la feature 27 retirada. Lo que falta de
  verdad es el nivel de cableado: para los secretos de webhook no existe nada equivalente
  a `ExternalCalendarWiringTest.s9_aMalformedKeyStopsTheStartupWithoutRevealingItsValue`.
  Conviene decirlo así: declarar un hueco más grande del que hay envejece igual de mal
  que ocultarlo.

---

## Resumen

La feature está **muy cerca**. Las dos puertas de mutación están pasadas de verdad, y lo
verifiqué recomputándolas yo, no leyéndolas. Las cuatro enmiendas del propietario tienen
su nota fechada y su oráculo, y ninguna fila enmendada quedó sin comprobar. Los cierres
del carril (3, 12 y 13) son los mejores del expediente: rojo pegado con su texto, mutante
nombrado, producción restaurada y verificada vacía.

Lo que impide el `done` no es el producto, es que **el contrato y el registro todavía no
dicen lo que el código hace**: `project-spec.md:2026` promete las dos cosas que se
enmendaron hoy, el informe de frontend que hay en el repositorio mide el árbol de antes
del arreglo, la bitácora canónica publica líneas de una campaña superada, y la única
condición que nadie reclamó —el árbol en verde— sigue sin correr.

Son cuatro correcciones de texto y una ejecución. Ninguna toca producción.

---

## Nota de árbol, al cerrar el veredicto

Al empezar la revisión `git status --porcelain` estaba **vacío**; al terminarla, no:
otro carril está trabajando sobre `main` en vivo y hay borrados y modificaciones sin
commitear en `frontend/stryker.{additional-connectors,automations,github-connector}.config.json`
(borrados) y en `frontend/stryker.{appearance,external-calendar,ics-calendar}.config.json`
y `scripts/project.test.mjs` (modificados) — la cirugía de retirada de 27/29/30.

**No afecta a este veredicto:** `frontend/stryker.webhooks.config.json` **no** está entre
ellos, ni ningún fuente ni prueba de la 25. Pero deja el C5 en el aire y refuerza el
bloqueante 4: el verde que cierre esta feature hay que correrlo sobre un árbol quieto,
no sobre éste. Rectifico en consecuencia: **C5 [ ]**, no [x].
