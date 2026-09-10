# Literales sin oráculo de las features 30 y 29 — 10 de septiembre de 2026

Carril `claude/github-connector`, worktree `C:/Users/vhurt/ow-worktrees/github-connector`.
Encargo: los cuatro hallazgos de `PostgresAutomationWork` (feature 30) y el del
mapa `ERROR_TEXT` del catálogo (feature 29), de
`progress/punto_ciego_literales.md`.

PIT muta bytecode y no muta literales de cadena. Todo lo de abajo tenía
puntuación perfecta sin que nadie lo hubiera verificado. La regla de la noche:
**el hallazgo no se cierra hasta que la producción rota pone roja la prueba
nueva**, con el mensaje anotado. Un oráculo que no puede fallar no vale.

## Dónde vive el trabajo

- Backend: `backend/src/test/java/com/apptolast/organization/adapter/persistence/AutomationWorkPersistenceTest.java`
  (clase nueva). Comparte el contenedor de `AutomationPersistenceTest` a
  propósito: son de la misma familia y no hacen falta dos.

## Tabla de rojos acreditados

| # | Literal | Qué rompí en producción | Mensaje del rojo |
|---|---|---|---|
| 1 | `ORDER BY occurred_at, event_id` (`window()`) | quitar `, event_id` | `[«E1 y E2 con el mismo occurred_at y event_id de E1 menor», y E1 va primero] Expecting actual: [2222…, 1111…] to contain exactly (and in same order): [1111…, 2222…]` |
| 1 | idem | `ORDER BY occurred_at, event_id DESC` | el mismo |
| 1 | idem (la **pérdida**, con las dos primeras aserciones relajadas a propósito) | `event_id DESC` | `[el hermano queda por delante del cursor, no detrás: no se pierde] Expecting actual: [] to contain exactly: [2222…]` |
| 2 | `AND status = 'retry'` (`claim()`) | `AND status = 'failed'` | `AutomationClaimedException: Otro worker ya registró esta ejecución.` |
| 2 | idem | borrar la cláusula entera | `[una fila ya resuelta no la reescribe nadie] Expecting code to raise a throwable.` |
| 3 | `"blocked".equals(row.getString("status"))` (`event()`) | `"held".equals(…)` | `[una fila blocked está retenida: se salta, no dispara nada] Expecting value to be true but was false` |
| 3 | idem | `"pending".equals(…)` | `[una fila pending no está retenida y sus reglas deben dispararse] Expecting value to be false but was true` |
| 4 | la consulta de `withTheirRuns(...)`: el `event_id IN (ventana)` | `window("event_id")` → `window("aggregate_id")` | `[las ejecuciones del evento, todas las suyas y sólo las suyas] Expecting actual: [] to contain exactly in any order: [AutomationRun[…attempt=2, status=retry…], AutomationRun[…attempt=1, status=succeeded…]]` |
| 4 | idem: el `owner_id = ?` | `owner_id <> ?` | el mismo, pero con la ejecución **del otro propietario** sobre ese mismo evento dentro: `Expecting actual: [AutomationRun[…ownerId=work-cb29…]]` |
| 5 | `ERROR_TEXT` (`connectors-catalog.tsx:50-60`) | **nada**: los dos códigos de GitLab ya faltaban | `Unable to find an element with the text: El proveedor limitó las peticiones` y `…: GitLab no responde` |
| 5 | idem | borrar la entrada `FEED_MALFORMED` | `Unable to find an element with the text: El calendario no se pudo interpretar` |
| 5 | el respaldo de `describeError` (`:67`) | vaciar el texto genérico | `Unable to find an element with the text: Hay un problema con esta integración` |
| 5 | la lista de siete de la prueba | borrar `FEED_MALFORMED` de `FEED_TEXT` | `TS2741: Property 'FEED_MALFORMED' is missing … but required in type 'Record<FeedError, string>'` |

---

## Hallazgo 1 — `ORDER BY occurred_at, event_id LIMIT ?` (`PostgresAutomationWork.window()`, :274)

**Qué decide.** El orden en que el ejecutor recorre la outbox de cada
propietario. El cursor sólo avanza hacia delante: procesar en otro orden
equivale a saltarse eventos **para siempre**.

**Por qué no lo distinguía nadie.** El orden de tupla que exige @s17
(`features/automations.feature:232`) sólo se comprobaba contra
`FakeWork.after()`, que ordena en Java dentro del propio test
(`ExecuteAutomationsTest.java:620-628`): eso demuestra que el caso de uso
respeta el orden que le den, no que el adaptador lo produzca. Y
`AutomationExecutionTest`, la única prueba de esta clase contra PostgreSQL,
nunca tiene más de una fila candidata por llamada a `after()`.

**La prueba.** `s17_ofTwoEventsOfTheSameInstantTheSmallerEventIdGoesFirstAndTheOtherIsNotLost`.
Dos identificadores fijos (`1111…` y `2222…`) para que «menor» signifique lo
mismo en PostgreSQL —que ordena `uuid` como bytes sin signo— y en la lectura de
la prueba. El **mayor se inserta primero a propósito**: sin desempate, el orden
que devuelve la tabla es el de escritura, y entonces la primera aserción cae.

Tiene dos oráculos distintos, y hacen falta los dos:

1. el orden: `after()` devuelve `[SMALLER, GREATER]`;
2. la no pérdida: el cursor avanza **al candidato que el adaptador puso
   primero** —no a uno escrito a mano en la prueba— y una segunda llamada a
   `after()` desde ahí todavía ve al hermano.

El segundo es el que enseña el daño de verdad: con el desempate roto, el cursor
salta al de identificador mayor y el hermano desaparece de todos los ciclos
futuros. Acreditado relajando a propósito las dos aserciones previas: la
tercera dio `Expecting actual: [] to contain exactly: [2222…]`. Producción y
prueba restauradas después.

---

## Hallazgo 3 — `"blocked".equals(row.getString("status"))` (`PostgresAutomationWork.event()`, :299)

**Qué decide.** Si una fila de la outbox marcada como bloqueada se salta
—avanza el cursor sin producir nada— o dispara reglas. Es la otra mitad de
@s17: «E3 posterior en estado blocked … no existe ejecución para E3»
(`features/automations.feature:233` y `:238`).

**Por qué no lo distinguía nadie.** La bandera se fabricaba a mano en el doble
(`ExecuteAutomationsTest.java:532`, usada en `:164`). `AutomationPersistenceTest.java:315`
sí inserta una fila `blocked`, pero prueba `PostgresAutomationEvents.recent()`
y su `status <> blocked`, que es otra consulta distinta. Por este adaptador no
pasaba ninguna.

**La consecuencia.** Si el literal deja de coincidir, `candidate.blocked()` es
siempre `false` y las reglas se ejecutan sobre eventos que la outbox retuvo a
propósito: se crean tareas y se encolan webhooks a partir de eventos que el
sistema decidió no publicar.

**La prueba.** `s17_theBlockedFlagOfEachOutboxRowReachesTheWorker`. Afirma los
**dos** lados —la fila `pending` con `blocked() == false` y la `blocked` con
`true`— porque un literal invertido sólo se distingue mirando los dos, y así
quedó acreditado: romperlo hacia un valor que no existe pone roja la segunda
aserción, invertirlo a `"pending"` pone roja la primera.

---

## Hallazgo 2 — `AND status = 'retry'` en el UPDATE de reintento (`PostgresAutomationWork.claim()`, :192)

**Qué decide.** Si un segundo o tercer intento puede reclamar la fila abierta.
Es la escalera de reintentos completa (attempt 1 → 2 → 3 → failed) que exige
@s22 (`features/automations.feature:290-294`).

**Por qué no lo distinguía nadie.** Ningún test de integración dejaba nunca una
fila en `retry` antes de un ciclo: `AutomationExecutionTest` sólo crea filas de
primer intento, y el primer intento va por la otra rama de `claim()`, la del
`INSERT … ON CONFLICT DO NOTHING`. La escalera sólo se probaba con el doble en
memoria (`ExecuteAutomationsTest.java:350` y `:377`), que no ejecuta ese UPDATE.

**La consecuencia.** Cambiado a `'failed'` o borrado, el UPDATE afecta a 0
filas, `claim()` lanza `AutomationClaimedException`, `ExecuteAutomations` lo
interpreta como «otro worker ganó» (`ExecuteAutomations.java:89`) y el paseo
continúa: el reintento nunca aterriza y la ejecución se queda clavada en su
intento para siempre, sin error visible.

**La prueba.** `s22_onlyARowStillInRetryCanBeRenewedByALaterAttempt`, en tres
tramos, porque las dos mutaciones fallan por lados opuestos:

1. una fila en `retry` attempt 1 se renueva a attempt 2 `retry` con su nuevo
   `executed_at`;
2. y de ahí a attempt 3 `failed`, que es la escalera del contrato;
3. y esa fila ya resuelta **no** la renueva un cuarto intento: se rechaza con
   `AutomationClaimedException` y la fila no cambia.

Sin el tramo 3, borrar la cláusula sobrevive; sin los tramos 1 y 2, cambiarla
de estado sobrevive. Hacen falta los dos lados.

---

## Hallazgo 4 — la consulta de `withTheirRuns(...)` (`PostgresAutomationWork.java:250-257`)

**Qué decide.** Si el ejecutor ve las ejecuciones previas de cada evento. De
ahí salen la idempotencia —una regla ya resuelta no se reintenta— y el número
de intento (`attemptOf`, `ExecuteAutomations.java:143`).

**Por qué no lo distinguía nadie.** Ningún test de integración dejaba de forma
determinista una fila en `automation_runs` antes de que un ciclo la leyera: en
@s25 la caída hace rollback y la fila no llega a existir, y en @s19/@s26 el
ciclo es el primero. La propiedad «una fila resuelta no se reintenta» sólo se
probaba con el doble (`ExecuteAutomationsTest.java:399`).

**La consecuencia.** Si la consulta devolviera vacío siempre —columna mal
escrita, `IN` mal armado, filtro de propietario de más—, `attemptOf()`
construiría siempre attempt 1: la escalera se colapsa y las filas en `retry`
quedan huérfanas, todo ello **enmascarado** por el `ON CONFLICT DO NOTHING`. Y
si el filtro de propietario se cayera, un candidato arrastraría ejecuciones de
otra cuenta sobre su mismo evento.

**La prueba.** `s22_eachCandidateCarriesItsOwnPreviousRunsAndNobodyElses`. La
siembra tiene una fila por cada cosa que la consulta tiene que hacer bien: dos
ejecuciones propias del mismo evento —de dos reglas distintas, que es el caso
real de dos reglas sobre el mismo trigger—, una ejecución **ajena** sobre ese
mismo evento, y un segundo evento sin ninguna. Se afirma el `AutomationRun`
entero, campo a campo, así que `RUN_COLUMNS` queda atado de paso.

- Romper la columna de la ventana del `IN` deja la lista vacía → rojo.
- Invertir el filtro de propietario mete la ejecución del extraño → rojo.

---

## Hallazgo 5 — el mapa `ERROR_TEXT` del catálogo (`frontend/src/connectors-catalog.tsx:50-60`)

**Éste no era sólo un hueco de prueba: era un defecto vivo.** El informe lo
anunció —«hoy mismo, sin cambiar nada: después de un fallo de GitLab la fila
del catálogo dice “Conectado” y debajo “Hay un problema con esta integración”,
que no traduce nada»— y la prueba nueva lo confirmó en el primer rojo, antes de
tocar una sola línea de producción.

**El recuento.** Los códigos que un `ConnectorStatusSource` puede publicar de
verdad en `lastError.code` son once, y salen de cuatro sitios independientes:

| Emisor | Códigos |
|---|---|
| `GithubStatusSource.java:80` | `CONNECTION_INVALID` |
| `WebhookStatusSource.java:49` (`WebhookEndpoint.DELIVERY_EXHAUSTED`) | `DELIVERY_EXHAUSTED` |
| `ConnectorFailures.gitlabImportErrorCode` (:61-67), por `ImportIssues.giveUp` → `GitlabIssueConnections.recordFailure`/`invalidate` → `GitlabConnection.lastError()` | `CONNECTION_INVALID`, `RATE_LIMITED`, `GITLAB_UNAVAILABLE` |
| `ExternalCalendarStatusSource.java:46` (`FeedError.name()`) | los siete de `FeedError` |

El mapa tenía **nueve**. Faltaban `RATE_LIMITED` y `GITLAB_UNAVAILABLE`, que
son justo los dos que @s25 (`features/additional_connectors.feature:307`) deja
escritos en la conexión tras un 503 de GitLab. Y @s33 (`:424`) promete «el
último error como **código traducido**».

**Por qué no lo distinguía nadie.** Las tres pruebas del catálogo que tocaban
`lastError` (`connectors-catalog.test.tsx:94`, `:128`, `:158`) usaban siempre
`CONNECTION_INVALID`, el único código de fuera del calendario que sí estaba en
el mapa. El backend afirma sus códigos contra sus propios literales y el
frontend contra los suyos; nada ata las dos listas, no hay prueba de contrato y
no existe ningún e2e que abra `/conectores`.

**La prueba.** Una tabla con los once códigos, cada uno con la fila donde puede
aparecer, el emisor anotado al lado y el texto exacto que el propietario tiene
que leer. Por cada uno se exige tres cosas: que salga su texto, que **no** salga
el código crudo y que **no** salga el genérico. Y una prueba más para el
respaldo: un código que esta versión no conoce sí tiene que caer en el genérico,
así que esa rama también tiene oráculo ahora.

**Contra el óxido.** Los siete del calendario se declaran en la prueba como
`Record<FeedError, string>`, con `FeedError` importado del cliente de la
feature 26. No es decoración: quitar una entrada da `TS2741` en `tsc --noEmit`,
que es parte de `pnpm build`. Añadir un octavo error de feed rompe este fichero
en vez de degradar la pantalla en silencio.

**El cambio de producción.** Dos entradas nuevas en `ERROR_TEXT`:

- `RATE_LIMITED: "El proveedor limitó las peticiones"` — neutral a propósito:
  el mapa es por código, no por conector, y `ConnectorFailure` del catálogo sólo
  lleva `{ code, at }`, sin los segundos que sí pinta `gitlab-connector.tsx:53`.
- `GITLAB_UNAVAILABLE: "GitLab no responde"` — la versión corta de la de
  `gitlab-connector.tsx:31`, en el estilo escueto del catálogo.

Más un comentario que dice de dónde sale la lista y para qué está el respaldo.
Es la única producción que este carril toca, y la toca porque el contrato la
exigía y no la cumplía.

## Lo que queda abierto (fuera del alcance de este carril)

- **Nada ata todavía las dos listas de códigos.** Lo cerrado aquí es que el mapa
  cubre entero el conjunto de hoy, y que la lista de errores de feed no puede
  crecer sin romper la prueba. Pero si mañana `ConnectorFailures` añadiera un
  cuarto código de importación de GitLab, esta prueba seguiría verde. Cerrarlo
  de verdad pide una prueba de contrato o un e2e sobre `/conectores`, que hoy no
  existe (el único e2e que toca el catálogo es `github-connector.spec.mjs`, y
  sólo mira su propia pantalla). Lo dejo anotado, no lo invento.
- El informe trae para la 30 varios omitidos graves que **no** están en este
  encargo y que siguen sin oráculo, sobre todo `PostgresAutomationWork.upsert()`
  entero —el `ON CONFLICT … DO UPDATE` que es LA fila que tiene que sobrevivir
  al rollback de @s20/@s22— y el `AND o.event_id = ?` del `INSERT … SELECT` de
  `queue()`. Los menciono porque la clase nueva que dejo,
  `AutomationWorkPersistenceTest`, es el sitio natural para cerrarlos.

## Ficheros compartidos que he tocado

Ninguno de la lista de `REGLAS.md` §6 ni del reparto de `REPARTO_NOCHE.md` §3.
`frontend/src/connectors-catalog.tsx` y su prueba no tienen dueño asignado esta
noche; `frontend/src/external-calendar-api.ts` **no** se toca: sólo se le
importa el tipo `FeedError`. No se ha creado ninguna migración, así que no gasto
ninguno de los números reservados.

## Previsión de mutantes, para contrastar con la campaña

No he corrido ni Stryker ni PIT (REPARTO §4: las corre el orquestador, en
serie). Lo que espero:

**Frontend, `connectors-catalog.tsx`.** Aquí sí hay ganancia medible, porque
Stryker —al revés que PIT— **sí** muta literales de cadena y objetos:

- los **veintidós** mutantes de cadena de `ERROR_TEXT` (once claves y once
  valores; antes sólo estaban cubiertos los dos de `CONNECTION_INVALID`) pasan a
  morir: cada código se comprueba por su texto exacto y por su clave;
- el mutante que sustituye el respaldo `"Hay un problema con esta integración"`
  por `""`, y el que borra el `??` dejando `ERROR_TEXT[code]`, mueren con la
  prueba del código desconocido;
- el `BlockStatement` de `describeError` muere por cualquiera de las doce.

Dos entradas nuevas en el mapa añaden cuatro mutantes que antes no existían, y
los cuatro nacen cubiertos.

**Backend, `PostgresAutomationWork`.** Aquí la ganancia de puntuación será
**pequeña o nula, y eso es exactamente el punto del informe**: los cuatro
hallazgos son literales de cadena dentro de SQL, que PIT no muta. Lo que cambia
no es la cifra sino que la conducta pasa a estar ejercida contra PostgreSQL. Lo
poco que sí debería moverse:

- `event()` y `withTheirRuns()` dejan de tener líneas sin cobertura de
  integración, así que los mutantes de `removeCall`/`returnValue` sobre ellas
  pasan a tener quien los mate;
- `claim()` cubre por fin su rama de `attempt != 1`, con lo que el mutante de
  frontera sobre `run.attempt() == 1` (`>=`, `!=`) tiene ahora oráculo por los
  dos lados.

Si la campaña contradice esta previsión, gana la campaña.

## Base de la rama

Rama `claude/github-connector`, con base en `a39a5409` (`origin/main` en el
momento del arranque). Durante la sesión `origin/main` avanzó hasta `1bc1dca3`
por otro carril; **no he rebasado ni fusionado**, para no pisarle el trabajo a
nadie. Cinco commits, uno por hallazgo.
