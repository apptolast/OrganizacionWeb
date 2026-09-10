# Cierre de tres condiciones del dictamen de la feature 29

Veredicto que se cierra: `progress/judge_additional_connectors.md`, condiciones
**C3** (techo de tamaño en los dos orígenes HTTP), **C4** (la prueba de contrato
apuntada al conector que se rompió) y **C6** (el oráculo que no puede fallar).

Carril `additional-connectors`, rama `claude/additional-connectors`, worktree
`C:/Users/vhurt/ow-worktrees/additional-connectors`, `E2E_WEB_PORT=18098`.
Base: `origin/main` en `5309e40` (avance por fast-forward desde `ba5e740`, que
era antecesor: el árbol estaba limpio y no se descartó nada).

Sin campañas de Stryker ni de PIT, según el aviso de recursos. Backend siempre
por clase concreta.

---

## C3 — techo de tamaño en la lectura del cuerpo — **CERRADA**

### El agujero

La fila `| 200 con cuerpo JSON de más de 5 MiB |` del `Scenario Outline` `@s25`
(`features/additional_connectors.feature:314`) no tenía ni prueba ni mecanismo.
`HttpGitlabIssueSource.java:128` y `HttpGithubIssueSource.java:101` leían el
cuerpo con `HttpResponse.BodyHandlers.ofString()`, sin techo ninguno. Con un
`api-base` autoalojado —que el contrato permite expresamente— eso agota la
memoria del proceso.

### El mecanismo

Nueva clase `backend/src/main/java/com/apptolast/organization/adapter/connectors/BoundedResponse.java`,
copiando el enfoque ya resuelto en `HttpCalendarFeed.java:205`
(`FeedFetch.failed(FeedError.FEED_TOO_LARGE)`):

- `BodyHandlers.ofString()` → `BodyHandlers.ofInputStream()`.
- Lectura en trozos de 16 KiB con `LIMIT = 5 * 1024 * 1024`. Se lee **hasta un
  byte más** del techo: ese byte de más es la prueba de que el cuerpo no cabe.
- Al pasarse, `IssueSourceException.unavailable()`, que es el camino que ya
  desemboca en 503 `GITLAB_UNAVAILABLE` / `GITHUB_UNAVAILABLE`.
- `try (var stream = response.body())`: cerrar el cuerpo es lo que **aborta la
  descarga** del proveedor que sigue emitiendo. Sin eso el techo ahorraría
  memoria pero no ancho de banda ni tiempo.

Es una sola clase compartida por los dos adaptadores a propósito: el peligro es
el mismo en los dos y tenerlo escrito dos veces es exactamente lo que deja que
uno se arregle y el otro no (que es el defecto que denuncia F1 del dictamen).

`BoundedResponse` es un `record (int status, HttpHeaders headers, String body)`,
así que los dos adaptadores dejan de manejar `HttpResponse<String>` y pasan a
manejar una respuesta **ya acotada**. El cambio de tipo es el mecanismo, no un
refactor de paso: mientras el tipo fuera `HttpResponse<String>` el cuerpo ya
estaba entero en memoria antes de que nadie pudiera mirarlo.

### El desenlace que exige el escenario, y de dónde sale cada mitad

`@s25` pide cinco cosas. La cadena queda así, y conviene que esté escrita porque
**ninguna prueba sola las cubre todas**:

| Lo que exige `@s25` | Quién lo afirma |
|---|---|
| el cuerpo de más de 5 MiB es un proveedor indisponible | `HttpGitlabIssueSourceTest.s25_abodyOverFiveMebibytesIsUnavailableInsteadOfEatingTheMemory` (nueva) |
| HTTP 503 con `GITLAB_UNAVAILABLE`, conexión intacta, sin tarea ni enlace, recibo `failed` | `ImportGitlabIssuesTest.s25_anUnavailableProviderAnnotatesTheFailureButLeavesTheConnectionConnected` (ya existía, para cualquier `IssueSourceException.unavailable()`) |
| no se sigue la redirección | `HttpGitlabIssueSourceTest:211` (ya existía) |

La prueba nueva es el eslabón que faltaba: acredita que **este** cuerpo produce
**esa** excepción. El resto de la cadena ya estaba probado y no había que
duplicarlo.

### Rojos acreditados

**Rojo 1 — el techo (GitLab).** Prueba escrita antes que el mecanismo, contra el
`ofString()` sin techo:

```
HttpGitlabIssueSourceTest > s25_abodyOverFiveMebibytesIsUnavailableInsteadOfEatingTheMemory() FAILED
    java.lang.NullPointerException at HttpGitlabIssueSourceTest.java:281
23 tests completed, 1 failed
```

El `NullPointerException` está en el ayudante `reasonOf`: `catchThrowableOfType`
devolvió `null` porque **no se lanzó ninguna excepción**. Es decir, el cuerpo de
5 MiB + 1 se importó tan campante después de cargarse entero en memoria. El
motivo del rojo es el correcto.

**Rojo 2 — el techo (GitHub).** Mismo ciclo sobre el adaptador gemelo:

```
HttpGithubIssueSourceTest > s28_abodyOverFiveMebibytesIsUnavailableInsteadOfEatingTheMemory() FAILED
    org.opentest4j.AssertionFailedError at HttpGithubIssueSourceTest.java:58
34 tests completed, 1 failed
```

La línea 58 es el `assertThrows` de `listFailure()`: se esperaba una
`IssueSourceException` y no se lanzó nada. Mismo motivo.

**Rojo 3 — la frontera.** Con el mecanismo puesto, mutante `>` → `>=` en
`BoundedResponse:85`:

```
HttpGitlabIssueSourceTest > s25_abodyOfExactlyFiveMebibytesStillImports() FAILED
    com.apptolast.organization.application.IssueSourceException at HttpGitlabIssueSourceTest.java:267
```

Muerto. Las dos mitades del techo tienen dientes: la de arriba y la de abajo.
Restaurado tras la comprobación.

Las dos pruebas de frontera declaran los 5 MiB con una constante **propia del
test** (`FIVE_MEBIBYTES`), no importada de producción. Si alguien sube el techo
del adaptador, las pruebas se enteran en vez de seguirlo.

### La trampa del plazo de lectura: **estaba, y no tenía oráculo**

El aviso era: «el corte por tamaño solo rescata al proveedor que sigue
emitiendo; uno que abre el cuerpo y no lo cierra necesita además un plazo de
lectura». Lo comprobé **ejecutando**, no razonando, y el resultado es peor de lo
que esperaba.

Con `ofString()` el plazo venía gratis: `HttpRequest.timeout` cubría el
intercambio entero. Con `ofInputStream()` **no**: el temporizador del cliente se
cancela en cuanto llegan las cabeceras y las lecturas del cuerpo quedan fuera de
él. O sea que el techo de tamaño, puesto solo, habría **abierto** un agujero
mientras cerraba otro. Así que el plazo va en `BoundedResponse` desde el primer
momento, cerrado por los dos lados como en `HttpCalendarFeed`: el bucle mira el
instante límite en cada trozo, y además se programa el cierre del cuerpo en ese
instante, porque un proveedor mudo no llega nunca a la comprobación.

Y aquí está el hallazgo: **el oráculo que había para eso no muerde**. Rompí el
plazo entero (las tres comprobaciones `expired(deadline)` a `false` y la
guillotina a un no-op) y corrí la clase:

```
BUILD SUCCESSFUL in 21s   (24 tests, 0 failed)
```

`s25_aprovidearThatNeverFinishesAnsweringIsUnavailable` seguía verde. Pasa **por
suerte**: el servidor falso acaba cerrando a los 6 s, el cuerpo llega vacío, un
cuerpo vacío no es un array y de ahí sale `UNAVAILABLE` igual, sólo que seis
segundos más tarde en vez de cuatro. El escenario dice «sin respuesta dentro del
tiempo de lectura» y esa prueba no mide ningún tiempo.

Oráculo nuevo, `HttpGitlabIssueSourceTest.s25_aproviderThatOpensTheBodyAndGoesSilentIsCutByTheReadDeadline`,
con un servidor que manda las cabeceras y **no emite ni cierra nunca**
(`FakeIssueServer.holdBody()`, con el cerrojo soltado en `close()` para no dejar
hilos colgados). El oráculo es un `@Timeout` en hilo aparte: sin plazo la
llamada no termina jamás, y así falla por vencimiento en vez de colgar la suite.

**Rojo 4 — el plazo.** Con el plazo roto igual que antes:

```
HttpGitlabIssueSourceTest > s25_aproviderThatOpensTheBodyAndGoesSilentIsCutByTheReadDeadline() FAILED
    java.util.concurrent.TimeoutException at ArrayList.java:1604
        Caused by: org.junit.jupiter.api.AssertTimeoutPreemptively$ExecutionTimeoutException
24 tests completed, 1 failed
```

Restaurado y verde.

En GitHub no hizo falta oráculo nuevo: `s28_aServerThatNeverSendsTheBodyGivesUpWellUnderFiveSeconds`
ya afirma sobre el **tiempo transcurrido** (`< 4900 ms` con el servidor
retrasando 20 s), así que mata tanto el mutante de la guillotina como el de la
comprobación en el bucle. Es lo que la prueba hermana de GitLab debería haber
hecho desde el principio.

### Ficheros

- `backend/src/main/java/com/apptolast/organization/adapter/connectors/BoundedResponse.java` (nuevo)
- `backend/src/main/java/com/apptolast/organization/adapter/connectors/HttpGitlabIssueSource.java`
- `backend/src/main/java/com/apptolast/organization/adapter/connectors/HttpGithubIssueSource.java`
- `backend/src/test/java/com/apptolast/organization/adapter/connectors/FakeIssueServer.java`
- `backend/src/test/java/com/apptolast/organization/adapter/connectors/HttpGitlabIssueSourceTest.java`
- `backend/src/test/java/com/apptolast/organization/adapter/connectors/HttpGithubIssueSourceTest.java`

Los dos ficheros de GitHub son del backend y no están en la tabla de dueños de
`REPARTO_NOCHE.md` (que reserva para el carril 27 `e2e/github-connector*.mjs` y
`frontend/src/github-connector*`). La condición C3 del dictamen dice literalmente
«Aplica igual a `HttpGithubIssueSource.java:101`».

### Para el orquestador: ámbito de mutación de `BoundedResponse`

`BoundedResponse` cae bajo `com.apptolast.organization.adapter.connectors.*`, que
`githubConnectorClasses` (`backend/build.gradle.kts:130`) ya declara entero: la
campaña de la 27 le dará mutantes. **No** entra en `additionalConnectorsClasses`,
que enumera clases una a una y no incluye el comodín del paquete. Es la misma
convención que el comentario de `:81-83` explica para las clases compartidas —un
mutante contado dos veces no informa—, así que lo dejo tal cual y lo anoto en vez
de tocar `backend/build.gradle.kts`, que no es mío.

Mutantes que espero que **mueran** en `BoundedResponse`:

- `buffer.size() > LIMIT` → `>=`, `<`, `true`, `false`: los matan las dos pruebas
  de frontera de cada adaptador (cuatro en total).
- la guillotina y las tres comprobaciones `expired(deadline)`: las matan
  `s25_aproviderThatOpensTheBodyAndGoesSilentIsCutByTheReadDeadline` y
  `s28_aServerThatNeverSendsTheBodyGivesUpWellUnderFiveSeconds`.

Mutantes que espero que **sobrevivan**, y por qué:

- `expired()` con `>=` → `>`: la diferencia es un nanosegundo exacto.
- `Math.max(0, deadline - System.nanoTime())` → quitar el `max`: `schedule` con
  un retardo negativo ya dispara inmediatamente.
- `guillotine.cancel(false)` → `cancel(true)`: no cambia nada observable.
- el tamaño de `CHUNK`: cualquier valor razonable da el mismo desenlace.

---

## C6 — los dos oráculos flojos — **CERRADA**

### H1 — el oráculo que no podía fallar

`ConnectorCatalogApiTest.s5_thewholeBodyCarriesNoSecretNoUrlAndNoProjectPath`
afirmaba que el cuerpo no contenía `glpat`, `invalid_token`, `WXYZ`,
`grupo/proyecto` ni `http`. El doble devolvía seis `ConnectorRow` cuyos únicos
datos eran `"gitlab"`, `"CONNECTION_INVALID"` y un `Instant`: **ninguna de las
cinco cadenas entraba jamás en el caso de prueba**. El controlador podía
serializar todo lo que recibiera y la prueba seguía verde.

Ahora la fila la deriva el `GitlabStatusSource` **de verdad**, a partir de un
`GitlabConnectionView` con los ocho campos poblados, incluidos los cinco que
`@s5` prohíbe publicar: base de la API, ruta del proyecto, identificador del
proyecto, pista del token y versión. La prueba pasa a cubrir **derivación y
serialización de punta a punta**, que es lo que pide «el cuerpo completo de la
respuesta» y lo que ninguna de las dos pruebas que sí mordían cubría entera
(`ConnectorStatusSourcesTest:378-395` mira el `toString()` de la fila, no el
JSON; `HttpGitlabIssueSourceTest:93-99` mira el mensaje de la excepción).

Y lleva la construcción que hace no-vacua una prueba de ausencia, la misma que
el dictamen elogia en `gitlab-connector.test.tsx:241`: **primero se afirma que lo
prohibido está en la entrada**, y sólo entonces significa algo que no esté en la
salida. Si alguien vuelve a vaciar el fixture, esa mitad se cae.

**Rojo acreditado.** Con la prueba nueva verde, inyecté en
`GitlabStatusSource.read` una fuga por un campo legítimo —el código del error
pasa a ser `view.projectPath()`, que es exactamente la clase de «mejora» que
provoca este fallo—:

```
FAILED: s5_thewholeBodyCarriesNoTokenHintNoApiBaseAndNoProjectPath()
org.opentest4j.AssertionFailedError: {"connectors":[ ... ,
  {"id":"gitlab","status":"error","lastActivityAt":"2026-09-10T08:30:00Z",
   "lastError":{"code":"grupo/proyecto","at":"2026-09-10T08:30:00Z"}}]}
==> expected: <false> but was: <true>
```

El mensaje de fallo lleva el cuerpo entero, así que la fuga se lee de un vistazo.
Producción restaurada y verde.

**Lo que este oráculo sigue sin poder decir, y queda escrito en su javadoc:** el
token entero nunca llega al catálogo, porque `GitlabConnectionView` no tiene
hueco para él —está bien diseñado—, y el texto libre del proveedor tampoco. Por
eso las dos aserciones sobre `glpat` e `invalid_token` **se retiran** de esta
prueba en vez de dejarse como decoración: donde muerden es en
`HttpGitlabIssueSourceTest:93-99`, que sí tiene el cuerpo real
`"invalid_token: glpat-abcdef1234"` en su fixture. Prefiero cinco aserciones que
pueden fallar a siete de las que dos no pueden.

### H2 — la asimetría del `@s31`

`s31_withoutASessionTheCatalogAnswersNothing` afirmaba `isUnauthorized()` y
`verifyNoInteractions`, pero no el código `UNAUTHENTICATED` que fija la fila del
escenario; su hermana de Bearer sí afirma `$.code`. Añadido.

**Rojo acreditado.** Mutante en `SecurityConfiguration.java:123`,
`"UNAUTHENTICATED"` → `"UNAUTHORIZED"`:

```
FAILED: s31_withoutASessionTheCatalogAnswersNothing()
java.lang.AssertionError: JSON path "$.code" expected:<UNAUTHENTICATED> but was:<UNAUTHORIZED>
```

Producción restaurada y verde. Diez pruebas de la clase en verde.

### Ficheros

- `backend/src/test/java/com/apptolast/organization/adapter/ConnectorCatalogApiTest.java`

Sin cambios de producción: los dos hallazgos eran de oráculo, no de conducta.
