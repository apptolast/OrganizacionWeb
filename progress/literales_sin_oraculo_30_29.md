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
