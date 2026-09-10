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
