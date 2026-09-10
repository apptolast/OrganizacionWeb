# Carril de bloqueantes — feature 29 (conectores adicionales / GitLab)

Alcance recibido: **B4, B5, B6, B7, B8, B9, B10 y B11** de
`progress/carriles/bloqueantes_29.md`, mas los dos hallazgos sueltos que el panel
dejo anotados y los dos defectos de produccion que me paso el orquestador a
mitad de carril.

Fuera de alcance por instruccion: **B1** (mutacion de frontend), **B2** (campaña
de mutacion de backend) y **B3** (`@s38` en navegador). No los he tocado.

Rama `claude/gitlab-bloqueantes`, worktree `C:/Users/vhurt/ow-worktrees/gitlab-bloqueantes`,
partiendo de `9a43fba7`. **Sin rebase, sin fetch, sin cambio de rama.** El arbol de
`main` ha avanzado durante el carril (el orquestador cita 179 pruebas nuevas en
`gitlab-connector.test.tsx` y un arreglo en `external-calendar.tsx`): mi
`gitlab-connector.test.tsx` y mi `gitlab-connector.tsx` son los de mi base, asi que
la integracion de este carril con el de mutacion de frontend **tendra conflicto en
esos dos ficheros** y hay que resolverlo a mano. Queda anotado, no rebasado.

## Como se ha medido

No se ha lanzado ninguna campaña de PIT ni de Stryker (prohibido por carga).
Cada mutante nombrado se ha **aplicado a mano al fuente, corrido contra su clase
concreta y deshecho**, verificando ademas que el fuente vuelve a quedar identico
a HEAD. Cuando un mutante muere, la bitacora nombra **qué prueba lo mata**.

Aviso que conviene conservar: mis veredictos de «SOBREVIVE» se han medido contra
**la clase o clases nombradas**, no contra la suite entera. Un «MUERE» es
concluyente; un «SOBREVIVE» mio significa «estas pruebas no lo matan», no
«ninguna prueba del repositorio lo mata».

## Resumen

| | Bloqueante | Desenlace |
|---|---|---|
| B4 | `SecretUndecipherableException` sin manejador + recibo huerfano | **CERRADO** — dos defectos de produccion arreglados |
| B5 | Instancia autoalojada que produccion rechaza (@s11/@s15) | **ABIERTO** — pregunta al propietario redactada |
| B6 | La frontera del recibo no afirma seis de siete valores | **CERRADO** — los 10 mutantes muertos, uno a uno |
| B7 | `decodeFailure` con cero cobertura | **CERRADO** — 6 mutantes muertos + pantalla |
| B8 | `@s31` sin `$.code` en tres filas CSRF y dos sin sesion | **CERRADO** |
| B9 | Borrado de conexion sin dos propietarios en la base | **CERRADO** — 2 mutantes de literal SQL muertos |
| B10 | `@s32` pide un identificador de correlacion que no existe | **ABIERTO** — es hueco de implementacion, no de prueba |
| B11 | Supervivientes en `HttpGitlabIssueSource` y `BoundedResponse` | **PARCIAL** — 5 muertos, 3 justificados equivalentes, 1 abierto |
| H-a | `ImportIssues:196` fija `"TaskCreated.v1"` sin oraculo | **CERRADO** |
| H-b | `HttpGitlabIssueSourceTest:232` no discriminante | **CERRADO** — se le han puesto dientes |
| D-1 | `pending.current = controller` sin abortar el anterior | **CERRADO** — defecto de produccion arreglado |
| D-2 | `readGitlabImport` exportada y sin uso; recibo `running` sin presentacion | **ABIERTO** — el contrato no fija la conducta |

**Defectos de produccion arreglados: 3.** (B4 ×2, D-1.)

---

## B4 — `SecretUndecipherableException`: 500 mudo y recibo huerfano · CERRADO

Dos defectos distintos con la misma causa. Los dos arreglados, cada uno con su
rojo.

### B4.1 — La ruta de GitLab devolvia 500 sin codigo

**Rojo** (`GitlabConnectorApiTest.s12_anUndecipherableStoredTokenIsAnExplicitProblemAndNotAGenericFailure`):

```
java.lang.AssertionError: Status expected:<503> but was:<500>
```

**Verde**: `GitlabConnectorController` gana el decimo `@ExceptionHandler`, gemelo
del de `GithubConnectorController:183-190`, que devuelve
`503 CONNECTOR_KEY_MISMATCH`. La prueba afirma ademas que el cuerpo no contiene
el token ni la cadena `glpat`.

### B4.2 — El recibo quedaba `running` con `finished_at` NULL

`ImportIssues.execute` inserta el recibo con `receipts.begin` **antes** de
descifrar, y el `try` de `run` solo capturaba `IssueSourceException`,
`ProjectCompletedException` y `StorageUnavailableException`. La excepcion se
escapaba sin pasar por `receipts.finish`, de modo que una clave rotada dejaba al
propietario con un `409 IMPORT_IN_PROGRESS` en toda escritura durante quince
minutos, por un fallo de configuracion del servidor.

**Rojo** (`ImportGitlabIssuesTest.s27_atokenNoKeyCanOpenClosesTheReceiptInsteadOfLeavingItRunning`):

```
org.opentest4j.AssertionFailedError: expected: <failed> but was: <running>
```

**Verde**: `ImportIssues.run` gana un `catch (SecretUndecipherableException)` que
cierra el recibo con `errorCode = CONNECTOR_KEY_MISMATCH` via el camino ya
existente `failed(...)` —que ademas deja la traza de auditoria— y **repropaga la
excepcion original**, para que el adaptador la siga traduciendo a 503 y no a un
`IssueImportFailedException` con otro codigo.

La prueba afirma las cuatro cosas: `status` failed, `errorCode`
CONNECTOR_KEY_MISMATCH, `finishedAt` no nulo, `importing(OWNER, …)` false, y que
el proveedor no recibio ni una peticion.

**Nota de alcance**: el caso de uso es el compartido con la 27, asi que este
arreglo cierra tambien **M7 de `progress/carriles/bloqueantes_27.md`**, que
describe exactamente el mismo defecto. No he escrito el espejo en
`ImportGithubIssuesTest`: el mecanismo esta medido sobre la clase compartida y el
carril de la 27 decidira si quiere su propia fila.

---

## B5 — El contrato fija una instancia autoalojada que produccion rechaza · ABIERTO

**No he enmendado el `.feature`.** La decision no es mia y el procedimiento del
repositorio exige contrafirma del propietario.

Lo comprobado, que confirma el motivo del panel:

- `features/additional_connectors.feature:132` y `:136` (@s11) fijan
  `app.gitlab.api-base = https://gitlab.example.com/api/v4` y exigen «HTTP 200
  con apiBase "https://gitlab.example.com/api/v4"».
- `GitlabApiBase.of` (`adapter/connectors/GitlabApiBase.java:36`) solo admite
  `gitlab.com` oficial o bucle local.
- `GitlabApiBaseTest.java:31-49`, `s11_refusesAnythingElseWhenTheBeanIsBuilt`,
  lleva la etiqueta **@s11** e incluye ese valor exacto entre los **rechazados**.
  La prueba afirma lo contrario de su escenario.
- El unico sitio donde produccion compone el `externalId` de @s15 es
  `HttpGitlabIssueSource:105-107`, y con la unica configuracion aceptable en
  pruebas devuelve `127.0.0.1:9001`, no `gitlab.example.com:9001`.
  `HttpGitlabIssueSourceTest:165` afirma `"127.0.0.1:9001"`.

**Pregunta exacta para el propietario, para copiar y contestar:**

> El conector de GitLab, ¿debe admitir instancias autoalojadas?
>
> (a) **Si, por lista blanca configurable.** Entonces `GitlabApiBase` deja de
> llevar los hosts dentro y pasa a leer una propiedad del servidor
> (`app.gitlab.allowed-hosts`, por ejemplo), las pruebas de @s11 se reescriben
> contra esa lista, y el `.feature` se queda como esta. Coste: hay que decidir
> ademas si la lista admite HTTP plano o solo HTTPS, y quien la escribe.
>
> (b) **No, solo `gitlab.com` y bucle local.** Entonces el `.feature` esta mal en
> tres sitios y hay que enmendarlo con contrafirma: `:132` y `:136` (la base y el
> apiBase que @s11 declara aceptados) y `:187` (el `externalId`
> `gitlab.example.com:9001` de @s15, que pasa a ser el host que produzca la base
> realmente admitida). Ademas hay que revisar
> `progress/proposal_additional_connectors.md:15` y `:81`, que venden GitLab por
> «cubrir autoalojado», y `BoundedResponse`, cuyo javadoc justifica el techo de
> 5 MiB diciendo «admite instancias autoalojadas» —el techo sigue estando bien,
> pero la razon escrita cambia.

**Lo que si he arreglado sin depender de la respuesta:** nada de @s11/@s15
depende de ella, asi que **no he tocado ninguna de las dos**. El resto del carril
es independiente.

**Cuanto falta para cerrarlo:** la respuesta del propietario. Si es (b), la
enmienda del `.feature` con contrafirma y ajustar dos literales de prueba:
media hora. Si es (a), hay implementacion de produccion nueva (propiedad,
validacion, y las pruebas de la lista): medio dia.

---

## B6 — La frontera del recibo no afirmaba seis de los siete valores · CERRADO

`GitlabConnectorApiTest` afirmaba `$.source`, `$.projectPath`, `$.created` y el
**conjunto de claves**; una clave con valor `null` sigue estando en el conjunto.

Ampliado:

- `s15_afinishedImportAnswersCreatedWithItsLocationAndTwelveFields` afirma ahora
  los doce valores de la fila 185 del contrato, incluido `errorCode: null` por
  comparacion de JSON (`content().json`), que `jsonPath(...).value(null)` no
  distingue bien.
- `s30_areceiptOfTheOwnerComesBackWholeThroughItsIdentifier` afirma `id`,
  `projectId`, `status`, `created`, `startedAt` y `finishedAt`.
- Nueva `s16_s17_s21_s26_everyCounterAndFlagOfTheReceiptTravelsWithItsValue`,
  parametrizada con cuatro filas tomadas del contrato: @s16 fila 4
  (`created 200, truncated true`), @s17 (`created 2, skipped 1, failed 0`), @s21
  fila 4 (`created 0, failed 1`) y @s26 (`failed, created 100,
  errorCode GITLAB_UNAVAILABLE`). Es lo que aporta los contadores **no nulos**,
  sin los cuales los mutantes «reemplaza el int por 0» son inmatables.

**Los diez mutantes de `ImportResponse`, aplicados a mano uno a uno:**

| Mutante | Veredicto | Quien lo mata |
|---|---|---|
| `id()` → null | MUERTO | `s15_afinishedImport…`, `s30_areceiptOfTheOwner…` |
| `projectId()` → null | MUERTO | `s15_afinishedImport…`, `s30_areceiptOfTheOwner…` |
| `status()` → `""` | MUERTO | las cuatro filas de `s16_s17_s21_s26_…` |
| `skipped()` → 0 | MUERTO | fila 2 (@s17, `skipped 1`) |
| `failed()` → 0 | MUERTO | fila 3 (@s21, `failed 1`) |
| `truncated()` → true | MUERTO | filas 2, 3 y 4 |
| `truncated()` → false | MUERTO | fila 1 (@s16, `truncated true`) |
| `errorCode()` → `""` | MUERTO | las cuatro filas |
| `startedAt()` → null | MUERTO | `s15_afinishedImport…`, `s30_areceiptOfTheOwner…` |
| `finishedAt()` → null | MUERTO | `s15_afinishedImport…`, `s30_areceiptOfTheOwner…` |

Diez de diez. El fuente quedo identico a HEAD tras la barrida (verificado con
`git diff`).

**Los tres manejadores sin cobertura**, tambien cubiertos:

- `malformed` → nueva `s12_abodyThatIsNotReadableJsonIsAMalformedBodyAndNotAValidationError`,
  parametrizada con `{`, texto que no es JSON y cuerpo vacio; afirma
  `400 MALFORMED_JSON`, el tipo `application/problem+json` y que el caso de uso
  no se toca.
- `rateLimitedProblem` **con recibo** (la rama `if (receipt != null)`) → nueva
  `s24_animportKilledByTheQuotaCarriesRetryAfterAndItsPartialCounters`, que
  afirma codigo, `retryAfterSeconds`, la cabecera `Retry-After` y los cuatro
  contadores parciales que viajan con el problema.
- `importFailed` ya lo ejercia `s23_afailedImportCarriesItsReceiptIdentifierWithTheProblem`;
  el NO_COVERAGE del XML citado por el panel es de un arbol anterior.

---

## B7 — `decodeFailure` con cero cobertura · CERRADO

Todos los fixtures del carril traian `lastError: null`, asi que la guarda entera
(`!exact(value, ERROR_FIELDS) || !nonEmpty(value.code) || !instant(value.at)`) y
su `throw` no se ejecutaban nunca. La prueba de contrato que C4 añadio compara el
**texto fuente** del `record` Java contra `ERROR_FIELDS`; el decodificador que
rompe en ejecucion seguia sin ejecutarse.

Añadido en `gitlab-connector-client.test.ts` (9 pruebas nuevas):

- una conexion en `error` con `lastError` valido que **decodifica** y llega
  entera;
- siete formas invalidas, todas afirmando `INCOMPATIBLE`: campo de mas (que es
  por donde se colaria el mensaje del proveedor), campo de menos, campo
  renombrado (`errorCode` por `code`), `code` vacio, `at` que no es instante,
  `at` con desplazamiento en vez de `Z`, y un `lastError` que ni siquiera es un
  objeto;
- el mismo decodificador por la ruta de `connectGitlab`, no solo por la de
  lectura.

**Los seis mutantes de la guarda, aplicados a mano:**

| Mutante | Veredicto | Pruebas que caen |
|---|---|---|
| condicion entera → `false` | MUERTO | 8 |
| quitar `!exact(value, ERROR_FIELDS)` | MUERTO | 1 |
| quitar `!nonEmpty(value.code)` | MUERTO | 1 |
| quitar `!instant(value.at)` | MUERTO | 3 |
| `value === null` → `value !== null` | MUERTO | 38 |
| quitar el `throw` entero | MUERTO | 8 |

Seis de seis. Fuente identico a HEAD tras la barrida.

En `gitlab-connector.test.tsx`, dos pruebas mas para que el `lastError` llegue a
la **pantalla desde el servidor** y no fabricado por el propio componente: una
que acepta el `lastError` valido y muestra «Error», y otra que rechaza el
`lastError` con un campo de mas y comprueba que ni `glpat` ni `invalid_token`
llegan al DOM.

Ojo, dato de producto que sale de aqui: **la pantalla del conector no pinta el
codigo de `lastError` en ningun sitio**, solo el texto «Error». Eso es correcto
segun el contrato —quien pinta el ultimo error es la fila del catalogo, @s33
linea 425— pero conviene que conste, porque el nombre de @s5 sugiere lo
contrario.

---

## B8 — `@s31` sin `$.code` en cinco peticiones · CERRADO

`s31_withoutACsrfTokenNothingIsWritten` cubria las tres filas de `CSRF_INVALID`
con solo `status().isForbidden()`, y `UNTRUSTED_ORIGIN` devuelve **el mismo 403**:
el estado por si solo no discrimina entre las dos filas. Igual
`s31_withoutASessionNoGitlabRouteAnswersAnything` con `isUnauthorized()`.

Como PIT no muta literales de cadena, ninguna campaña lo delatara nunca; el
oraculo es la unica via. Añadidos los cinco `$.code`, y acreditado el rojo
perturbando el literal de produccion a mano:

`SessionAccessDeniedHandler:35`, `"CSRF_INVALID"` → `"UNTRUSTED_ORIGIN"`:

```
java.lang.AssertionError: JSON path "$.code" expected:<CSRF_INVALID> but was:<UNTRUSTED_ORIGIN>
```

`SecurityConfiguration:123`, `"UNAUTHENTICATED"` → `"SESSION_REQUIRED"`:

```
java.lang.AssertionError: JSON path "$.code" expected:<UNAUTHENTICATED> but was:<SESSION_REQUIRED>
```

Los dos literales devueltos a su valor original y verificado `git diff` vacio en
ambos ficheros. Con esto la asimetria que C6 dejo a medias —arreglada en
`ConnectorCatalogApiTest` y viva en el fichero hermano— queda cerrada.

---

## B9 — El borrado de la conexion sin dos propietarios en la base · CERRADO

`PostgresGitlabConnectionStore:86-89` hace
`DELETE FROM gitlab_connections WHERE owner_id=?`, y `GitlabConnectorPersistenceTest`
nunca tenia dos filas de conexion a la vez: `s14` borraba con una sola fila
presente. PIT no muta literales SQL, asi que quitar el `WHERE` no rompia nada, y
lo que hay en esa tabla son los PAT cifrados de todos los inquilinos.

- `s14_deletingIsIdempotentAndLeavesTasksLinksAndReceiptsUntouched` guarda ahora
  conexion de `OWNER` **y** de `OTHER`, y afirma que tras
  `connections.delete(OWNER)` la de `OTHER` sigue presente, con su `ciphertext`
  intacto y su `version` sin tocar, y que la tabla conserva una fila.
- Nueva `s13_savingAgainReplacesOnlyTheRowOfItsOwnerAndNotTheOneNextToIt` para el
  caso simetrico del `ON CONFLICT (owner_id)`.

**Los dos mutantes, aplicados a mano:**

| Mutante | Veredicto | Texto del fallo |
|---|---|---|
| `DELETE FROM gitlab_connections` (sin `WHERE owner_id=?`) | MUERTO | `s14…`: `Expecting value to be true but was false` |
| `save` como «borra la tabla e inserta» (implementacion ingenua de reemplazo) | MUERTO | `s13…`: `java.util.NoSuchElementException: No value present`; `s14…`: `Expecting value to be true but was false` |

El primero es especialmente elocuente: borrar todas las filas afecta a dos, y el
`== 1` del `delete` pasa a devolver `false`, o sea que el borrado destructivo se
declara ademas fallido.

Fuente devuelto a HEAD y verificado con `git diff` vacio.

---

## B10 — `@s32` pide un identificador de correlacion que no existe · ABIERTO

Confirmado punto por punto, y **es hueco de implementacion, no de prueba**: no
hay nada que medir todavia.

- `features/additional_connectors.feature:413` (@s32) exige «los logs del fallo
  contienen el codigo CONNECTION_INVALID **y el identificador de correlacion de
  la peticion**».
- `application/ConnectorAudit.java` no admite ningun parametro de correlacion en
  ninguno de sus cuatro metodos (`connected`, `connectionRefused`,
  `importFinished`, `importFailed`).
- El unico `correlationId` del repositorio lo acuña `adapter/http/ApiErrors.java:127`
  dentro del manejador de 500, que es otro camino y no pasa por la auditoria.
- El oraculo que se cita como cierre, `GitlabTokenConfinementTest:131-135`,
  afirma `contains("CONNECTION_INVALID")` y `contains("owner=owner-1")`: el
  **propietario**, que es el mismo en todas las peticiones de esa persona, no la
  peticion. Es sustituir el valor que el contrato pide por otro que casualmente
  esta en la misma linea.

**No lo he implementado.** La razon: propagar una correlacion desde el filtro
HTTP hasta `Slf4jConnectorAudit` toca la firma de un puerto de aplicacion que
comparten la 27 y la 29 —los cuatro metodos, sus dos implementaciones y todos sus
dobles de prueba— y es exactamente el tipo de cambio transversal que no cabe en
un carril que tiene que aterrizar. Hacerlo a medias es peor que declararlo.

**Pregunta exacta para el propietario:**

> @s32 pide el identificador de correlacion de la peticion en los logs del fallo,
> y hoy el sistema registra el propietario. ¿Cual de las dos?
>
> (a) **Se implementa la correlacion.** Hace falta: acuñar el identificador una
> sola vez por peticion en un filtro HTTP (hoy solo se acuña dentro del manejador
> de 500, en `ApiErrors:127`), publicarlo por MDC o por parametro explicito,
> añadirlo a los cuatro metodos de `ConnectorAudit` y a `Slf4jConnectorAudit`, y
> afirmarlo en `GitlabTokenConfinementTest`. Toca a la 27 tambien, porque el
> puerto es compartido. **Estimacion: un dia**, mas la coordinacion con el carril
> de la 27.
>
> (b) **Se enmienda @s32 con contrafirma** para que la clausula pida el
> propietario y el instante, que es lo que el sistema registra de verdad, y la
> correlacion se apunta como trabajo posterior. **Estimacion: media hora.**

Mi lectura, para que sea util y no neutra: la razon de ser de @s32 es diagnosticar
un fallo de credencial **sin volcar el token**, y para eso el propietario no basta
—no distingue dos intentos de la misma persona—. Recomiendo (a), pero como
trabajo propio con su cabecera, no colado en un carril de bloqueantes.

**Cuanto falta:** la decision, y despues un dia de trabajo si es (a).

---

## B11 — Supervivientes en `HttpGitlabIssueSource` y `BoundedResponse` · PARCIAL

Los siete supervivientes nombrados, mas el `headers()` sin cobertura, medidos uno
a uno **antes y despues** de escribir los oraculos.

### Muertos con oraculo nuevo (5)

| Superviviente | Antes | Ahora | Quien lo mata |
|---|---|---|---|
| `HttpGitlabIssueSource:97` negated conditional #1 (`description == null` → `!=`) | SOBREVIVE | **MUERTO** | `s21_anAbsentDescriptionFieldIsNoBodyEither`, `s21_thedescriptionOfTheIssueArrivesAsTheBodyOfTheExternalIssue` |
| `HttpGitlabIssueSource:97` negated conditional #2 (`isNull()` → `!isNull()`) | SOBREVIVE | **MUERTO** | `s21_anExplicitNullDescriptionIsNoBodyAtAllAndNotTheWordNull`, `s21_thedescriptionOfTheIssue…` |
| `BoundedResponse:82` boundary (`read >= 0` → `> 0`) | — | **MUERTO** | `s25_areadOfZeroBytesIsNotTheEndOfTheBody` |
| `BoundedResponse:90` negated conditional (catch `IOException`) | — | **MUERTO** | `s25_anetworkFailureWellWithinTheDeadlineTravelsAsItselfAndNotAsATimeout`, `s25_thesameFailureOnceTheDeadlineIsSpentIsAnUnavailableProvider` |
| `BoundedResponse:34` `headers()` → null (estaba en NO_COVERAGE) | — | **MUERTO** | `s16_theStatusAndTheHeadersOfTheProviderTravelUntouched` |

Para `:97` la causa del hueco estaba a la vista: **todas** las issues de las
pruebas del carril traen `"description":null`, asi que ninguna prueba distinguia
las tres formas con que GitLab entrega el cuerpo. Ahora hay una por forma —texto,
`null` explicito y campo ausente— y las dos mitades de la guarda quedan atadas.

Para `BoundedResponse` he escrito un carril propio,
`backend/src/test/java/com/apptolast/organization/adapter/connectors/BoundedResponseTest.java`.
Las pruebas del adaptador solo llegan a esa clase a traves de un servidor HTTP
real, que no sabe emitir lecturas de cero bytes ni fallos de red a voluntad;
entregarle el flujo directamente es lo unico que discrimina las decisiones del
bucle. Cinco pruebas: la lectura de cero bytes que **no** es fin de cuerpo, el
fallo de red con plazo de sobra que viaja como `IOException` y no como
vencimiento, el mismo fallo con el plazo ya gastado que si es vencimiento, el
techo de 5 MiB por los dos lados, y el paso integro de estado y cabeceras.

### Justificados como equivalentes (3)

Estos tres **no se pueden matar**, y la razon no es «no se me ocurre como»: esta
comprobada.

**`HttpGitlabIssueSource:180`, «replaced int return with 0» sobre
`return DEFAULT_RETRY_SECONDS`.** SOBREVIVE incluso con
`s24_aquotaAnnouncedWithoutRetryAfterFallsBackToAMinute`, que afirma exactamente
60. El motivo es que el valor **se vuelve a aplicar aguas abajo**:
`IssueSourceException.rateLimited` (`application/IssueSourceException.java:44-47`)
hace `retryAfterSeconds < 1 ? DEFAULT_RETRY_SECONDS : retryAfterSeconds`, y su
`DEFAULT_RETRY_SECONDS` **tambien vale 60**. Devolver 0 desde el adaptador produce
el mismo 60 en el puerto, asi que **no existe observacion posible** que los
distinga. El defecto de fondo no es la prueba, es que **el mismo valor por
defecto esta escrito en dos sitios**; el del adaptador es codigo muerto. Lo dejo
señalado y no lo toco: quitarlo es un cambio de produccion sin prueba roja que lo
pida, y ademas afecta a la 27.

**`HttpGitlabIssueSource:185`, boundary `seconds < MINIMUM_RETRY_SECONDS` → `<=`.**
`MINIMUM_RETRY_SECONDS` vale 1. Las dos versiones solo pueden diferir en
`seconds == 1`: la original da `(int) Math.min(1, MAX) = 1`, la mutada da
`MINIMUM_RETRY_SECONDS = 1`. **Son el mismo numero.** Mutante aritmeticamente
equivalente, y ademas enmascarado por el mismo `< 1` del parrafo anterior.

**`BoundedResponse:98`, boundary `System.nanoTime() - deadline >= 0` → `> 0`.**
Difieren en un unico instante: cuando el reloj monotono coincide exactamente con
el plazo, un nanosegundo entre 2^63. No es alcanzable de forma determinista, y
una prueba que lo intentase seria inestable. Equivalente a efectos practicos.

### Abierto (1)

**`HttpGitlabIssueSource:179-181`, la rama `ratelimit-reset`, en NO_COVERAGE.**

No la he cubierto **a proposito**, porque cubrirla seria inventar conducta.
@s24 (`feature:294-298`) tiene tres filas y **ninguna** menciona
`RateLimit-Reset`. Peor: su fila 2 dice «429 **sin** Retry-After → 60 segundos»,
y una respuesta 429 sin `Retry-After` **pero con** `RateLimit-Reset` es
literalmente esa fila, y produccion no devuelve 60, devuelve
`reset − ahora`. O sea que **produccion contradice una fila del contrato** en un
caso que el contrato si cubre.

Escribir una prueba que afirme lo que produccion hace hoy seria un oraculo que no
puede fallar, exactamente el vicio que este panel esta destapando. Y borrar la
rama es un cambio de conducta que no me corresponde decidir.

**Pregunta exacta para el propietario:**

> Un 429 sin `Retry-After` pero con `RateLimit-Reset`, ¿que debe esperar?
> (a) Los 60 segundos que fija la fila 2 de @s24 —y entonces sobra la rama
> `ratelimit-reset` de `HttpGitlabIssueSource:179-181` y hay que quitarla—, o
> (b) el tiempo que falta hasta el reset —y entonces @s24 necesita una cuarta
> fila, con contrafirma, que lo diga—.

**Cuanto falta:** la respuesta, y despues quince minutos (opcion a: borrar tres
lineas y ver que la suite sigue verde) o una hora (opcion b: enmienda con
contrafirma mas la prueba de la fila nueva).

### Contraste con la prevision de C3

`progress/cierre_condiciones_29.md:187-200` habia previsto **tres**
supervivientes y **ninguno coincide** con `:82` ni con `:90`. Queda dicho aqui,
que era lo que el dictamen pedia y nadie habia hecho: la prevision de C3 esta
**desmentida por la medicion**. Los de `BoundedResponse` que si existian estan
ahora muertos, asi que la correccion de la prevision es que **ya no hay
supervivientes en `BoundedResponse`** salvo `:98`, justificado arriba.

---

## H-a — `ImportIssues:196` fijaba `"TaskCreated.v1"` sin ningun oraculo · CERRADO

El nombre del evento es un contrato **entre features**: las automatizaciones de
la 30 se suscriben a ese literal exacto, y cambiarlo dejaria de dispararlas en
silencio.

`ConnectorFakes.FakeImportedTaskCommit` solo guardaba el `eventId` del evento;
ahora guarda el evento entero y expone `lastEvent()`.
`ImportGitlabIssuesTest.s15_importingOpenIssuesCreatesLinkedTasksAndAReceiptThatNamesItsSource`
afirma `type()` y `schemaVersion()`.

**Rojo acreditado** cambiando el literal a `"TaskCreated.v2"`:

```
org.opentest4j.AssertionFailedError: expected: <TaskCreated.v1> but was: <TaskCreated.v2>
```

Literal devuelto a `v1`.

## H-b — `HttpGitlabIssueSourceTest:232` no discriminante · CERRADO, con dientes

`s25_aprovidearThatNeverFinishesAnsweringIsUnavailable` estaba **documentada por
el propio fichero** como no discriminante: con `delayBody`, el proveedor acaba
cerrando y el cuerpo vacio ya no es un array, de modo que da `UNAVAILABLE` aunque
no hubiera plazo ninguno.

En vez de borrarla —es la unica que representa la fila «sin respuesta dentro del
tiempo de lectura» de @s25 por ese camino— le he puesto los mismos dientes que
tiene su gemela de GitHub (`HttpGithubIssueSourceTest:316`): ahora se llama
`s25_aproviderThatNeverFinishesAnsweringIsCutBeforeItDecidesToAnswer`, sirve un
cuerpo **valido** (para que el veredicto no pueda venir de la forma) y **mide el
reloj**, afirmando que el adaptador se rinde al menos medio segundo antes de que
el proveedor termine. Sin plazo de lectura, la llamada tardaria los seis segundos
enteros y la prueba se pone roja.

---

## D-1 — `pending.current = controller` sin abortar el anterior · CERRADO

Defecto de produccion que me paso el orquestador, encontrado por el carril de
mutacion de frontend. Confirmado y arreglado.

`frontend/src/gitlab-connector.tsx` asignaba `pending.current = controller` en
cuatro sitios sin abortar el anterior, asi que dos operaciones solapadas escribian
las dos y ganaba **la que respondia ultima, no la que el propietario pidio
ultima**.

**Rojo** (`gitlab-connector.test.tsx`,
`@s37 a slow import does not repaint its receipt over an already disconnected screen`):

```
AssertionError: expected false to be true // Object.is equality
- Expected  true
+ Received  false
  ❯ src/gitlab-connector.test.tsx:651:28   expect(inFlight.aborted).toBe(true);
```

Es decir: con la importacion en vuelo, el propietario desconecta y la peticion de
importacion **seguia viva**, lista para repintar «Creadas/Omitidas/Fallidas»
sobre una pantalla ya desconectada.

**Verde**, siguiendo el patron ya establecido en `external-calendar.tsx:109-114`
(feature 28), que el orquestador señalo:

- `relieve()` aborta la peticion en vuelo, crea el controlador nuevo y lo deja en
  `pending`. Los cuatro sitios (`loadConnection`, `submitConnection`,
  `startImport`, `confirmDisconnect`) pasan por el.
- `settled(controller)` sustituye a los `finally { if (live(controller)) … }`.
  **Este segundo cambio es imprescindible y no es cosmetico**: el indicador de
  «estoy trabajando» pertenece a la operacion que lo encendio, asi que tiene que
  apagarse **aunque a esa operacion la hayan relevado**. Sin esto, abortar la
  importacion dejaba `importing` en `true` para siempre y el aviso
  `role="status"` seguia anunciando «Importando issues…» despues de desconectar
  —una regresion que el arreglo ingenuo introduce—. Lo que **nunca** se aplica
  tras un relevo son los datos: eso lo sigue guardando `live(controller)` en el
  `try` y en el `catch`.

La prueba afirma las tres cosas: que la peticion queda abortada, que el aviso
aria-live vuelve a quedar vacio y que no aparece la region «Resultado de la
importacion» ni la palabra «Creadas».

Suite de frontend completa: **3143 pruebas en 90 ficheros, todas verdes**.
`tsc --noEmit` y `eslint` limpios; `prettier --write` aplicado.

## D-2 — `readGitlabImport` exportada y sin uso en produccion · ABIERTO

Confirmado: `readGitlabImport` solo la llama su prueba. Y la consecuencia que
señala el orquestador tambien: el componente `Receipt`
(`gitlab-connector.tsx:445-470`) pinta **cuatro cifras y la bandera de truncado**,
y **nunca el `status`**, asi que un recibo `running` se presenta identico a una
importacion terminada sin resultados. El propietario no puede distinguir «va por
la mitad» de «no encontro nada».

**No he inventado conducta.** He mirado el contrato y **no dice que ver mientras
una importacion esta `running`**:

- @s35 (`feature:446-447`) dice «pulsar Importar issues deshabilita el boton y
  anuncia por aria-live un progreso **sin porcentaje** hasta recibir el recibo» y
  «el recibo se presenta como cuatro cifras etiquetadas … con aviso visible si
  truncated es true». O sea: el contrato modela la importacion como **sincrona**
  —una sola peticion que devuelve el recibo terminado— y ahi el `running` no
  existe.
- Pero el servidor **si** puede devolver un recibo `running`: el tipo del cliente
  lo admite (`status: "running" | "completed" | "failed"`), `decodeReceipt` lo
  valida con su propia regla («solo uno en curso carece de final»), y
  `GET /imports/{id}` existe precisamente para eso.

Hay por tanto un hueco entre lo que el servidor puede decir y lo que la pantalla
sabe leer, y **el contrato no lo cubre**.

**Pregunta exacta para el propietario:**

> `POST /imports` puede devolver un recibo en `running` (el tipo lo admite y
> `GET /imports/{id}` existe para seguirlo), pero @s35 describe la importacion
> como si siempre volviera terminada, y la pantalla pinta cuatro cifras sin
> decir nunca el estado. ¿Que debe ver el propietario mientras una importacion
> esta `running`?
>
> (a) **Nada nuevo: el servidor nunca devuelve `running` en el POST.** Entonces
> sobra `readGitlabImport` en el cliente, hay que quitarla, y conviene que
> `decodeReceipt` **rechace** un `running` en la respuesta del POST en vez de
> aceptarlo. Media hora.
>
> (b) **La pantalla sigue la importacion.** Entonces @s35 necesita una clausula
> nueva con contrafirma que diga: cada cuanto se relee, que se muestra mientras
> tanto, que dice el aviso aria-live, y que pasa si la lectura falla o si el
> propietario se va. Es escenario nuevo, no ajuste. Un dia largo, y toca @s37.

**Lo unico que he hecho sin depender de la respuesta:** dejarlo escrito. No he
tocado `Receipt` ni he borrado `readGitlabImport`, porque las dos acciones
dependen de la respuesta y son opuestas entre si.

**Cuanto falta:** la decision. Media hora si es (a), un dia si es (b).

---

## Lo que queda abierto, en una lista

1. **B5** — instancia autoalojada: pregunta redactada arriba. Media hora si la
   respuesta es «no las admitimos»; medio dia si es «por lista blanca».
2. **B10** — correlacion de @s32: hueco de implementacion, no de prueba. Un dia
   si se implementa; media hora si se enmienda el escenario con contrafirma.
3. **B11 parcial** — la rama `ratelimit-reset` (`HttpGitlabIssueSource:179-181`),
   sin cubrir a proposito porque produccion contradice la fila 2 de @s24.
   Quince minutos o una hora segun la respuesta.
4. **D-2** — que ve el propietario con una importacion `running`. Media hora o un
   dia segun la respuesta.
5. **Deuda señalada, sin bloquear**: el valor por defecto de reintento esta
   escrito dos veces (`HttpGitlabIssueSource:35` y `IssueSourceException:16`), y
   el del adaptador es codigo muerto. Quien lo quite mata de paso el mutante
   `:180`. No lo he tocado porque afecta tambien a la 27.

## Lo que NO he tocado, y por que

- **`backend/build.gradle.kts`**: lo lleva el orquestador. Este carril **no
  necesita ningun cambio de ambito**: la clase nueva `BoundedResponseTest` es de
  prueba, y `BoundedResponse` ya esta en `additionalConnectorsClasses`. Lista de
  cambios pedidos: **ninguno**.
- **`features/additional_connectors.feature`**: ni una linea. Las dos enmiendas
  que hacen falta (B5 y, segun la respuesta, @s24) son del propietario.
- **B1, B2, B3**: fuera de alcance por instruccion.
- **Los 20 supervivientes declarados equivalentes** por el carril de mutacion de
  frontend: no los he vuelto a mirar, como se me pidio.

## Higiene

- Ningun contenedor propio vivo al terminar (`docker ps` vacio). Nunca levante
  pila E2E; el puerto `18109` que tenia asignado no llego a usarse.
- Ninguna campaña de PIT ni de Stryker lanzada.
- Ningun secreto en pruebas, aserciones ni mensajes: los tokens de los fixtures
  son literales inventados y las pruebas nuevas afirman precisamente que ni
  `glpat` ni `invalid_token` salen por el cuerpo ni por el DOM.
- Todos los mutantes aplicados a mano fueron deshechos y verificados con
  `git diff` vacio sobre su fichero antes de continuar.
