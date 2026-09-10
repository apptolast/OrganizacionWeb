# Mutación de la feature 27 — conector de GitHub

Condición 8 del veredicto vigente (`progress/judge_github_connector_cierre.md:281-282`,
sección «### Backend — PIT»): «Todo superviviente, documentado en
`progress/mutation_github_connector.md`: matado con test nuevo o justificado
como equivalente, uno por uno (C7)». Este fichero no existía. Aquí está.

Carril: `claude/gh-resto`, worktree `C:/Users/vhurt/ow-worktrees/gh-resto`,
partiendo de `65dcd72e`. Cierra los motivos de
`progress/carriles/bloqueantes_27.md` **excepto M1, M2 y M6**, que los lleva
otro carril.

**No he lanzado ninguna campaña de PIT ni de Stryker.** Las mide el
propietario. Todo lo que aquí se afirma sobre un mutante concreto se ha
comprobado aplicándolo **a mano** al fuente, corriendo su clase de prueba y
deshaciéndolo; cada caso lleva escrito el resultado observado.

---

## 1. El artefacto del que salen estas cifras

- Fichero: `backend/build/reports/pitest-github-connector/mutations.xml` del
  repositorio principal, escrito el **10/09/2026 a las 12:35:09**.
- Ya **no** está truncado (el panel de precierre lo leyó a media escritura):
  cierra con `</mutations>` y trae los 502 mutantes completos.
- **No dice contra qué commit se midió.** Es el mismo defecto de trazabilidad
  que el panel anota para el frontend (M20), y aquí también aplica: lo único
  reconstruible es que se escribió a las 12:35:09 y que el commit de `main`
  inmediatamente anterior es `684536dd` (12:34:37). Una corrida de 502
  mutantes empieza bastante antes de esa hora, así que el árbol medido es
  anterior y **no se puede nombrar**. Queda declarado, no maquillado.
- Recuento medido sobre el XML, no copiado de ninguna bitácora:

| estado | mutantes |
| --- | --- |
| KILLED | 465 |
| TIMED_OUT (cuentan como muertos) | 3 |
| SURVIVED | 24 |
| NO_COVERAGE | 10 |
| **total** | **502** |

**468/502 = 93,23 %.** La cifra que circulaba, 467/502 = 93,03 %, es de una
corrida anterior; la diferencia es un mutante.

### Desglose por clase (lo que el juez pidió expresamente)

| clase | mutantes | muertos | SURVIVED | NO_COVERAGE |
| --- | ---: | ---: | ---: | ---: |
| adapter.config.ConnectorConfiguration | 19 | 19 | 0 | 0 |
| adapter.connectors.AesGcmSecretCipher | 17 | 17 | 0 | 0 |
| adapter.connectors.BoundedResponse | 21 | 17 | 3 | 1 |
| adapter.connectors.ConnectorKeyRing | 12 | 11 | 1 | 0 |
| adapter.connectors.ConnectorKeyRing$ConnectorKey | 1 | 1 | 0 | 0 |
| adapter.connectors.GithubApiBase | 27 | 27 | 0 | 0 |
| **adapter.connectors.GitlabApiBase** | **27** | 27 | 0 | 0 |
| adapter.connectors.HttpGithubIssueSource | 48 | 45 | 2 | 1 |
| **adapter.connectors.HttpGitlabIssueSource** | **54** | 47 | 4 | 3 |
| adapter.http.GithubConnectorController | 49 | 45 | 3 | 1 |
| adapter.http.…$ConnectionResponse | 6 | 6 | 0 | 0 |
| adapter.http.…$ImportResponse | 15 | 13 | 2 | 0 |
| adapter.persistence.PostgresConnectorConnectionStore | 11 | 9 | 2 | 0 |
| adapter.persistence.PostgresImportedTaskCommit | 16 | 13 | 3 | 0 |
| adapter.persistence.PostgresIssueImportReceiptStore | 24 | 21 | 1 | 2 |
| application.ConnectGithub | 6 | 6 | 0 | 0 |
| application.ConnectionView | 6 | 6 | 0 | 0 |
| application.ConnectorFailures | 7 | 7 | 0 | 0 |
| application.DisconnectGithub | 1 | 1 | 0 | 0 |
| application.ImportGuard | 1 | 1 | 0 | 0 |
| application.ImportIssues | 30 | 30 | 0 | 0 |
| application.IssuePage | 7 | 7 | 0 | 0 |
| application.IssueSourceException | 10 | 10 | 0 | 0 |
| application.ReadGithubConnection | 2 | 2 | 0 | 0 |
| application.ReadIssueImport | 2 | 2 | 0 | 0 |
| application.StoredConnection | 8 | 8 | 0 | 0 |
| domain.ExternalIssue | 15 | 15 | 0 | 0 |
| domain.GithubRepository | 6 | 6 | 0 | 0 |
| domain.IssueImportReceipt | 37 | 34 | 1 | 2 |
| domain.PersonalAccessToken | 17 | 15 | 2 | 0 |

**Dos clases nombradas en el ámbito no aparecen en el informe:**
`GithubIssueConnections` (no estaba declarada cuando se midió; lo arregla
`7cf3f9c2`, verificado en §3) y `Slf4jConnectorAudit` (declarada y con **cero**
mutantes; §3, motivo M13, sigue **abierto**).

**En negrita, las dos clases de la feature 29** que el comodín del ámbito
arrastra: 81 de los 502 mutantes (16,1 %) son código GitLab. Descontadas,
la feature 27 mide **394/421 = 93,59 %**. Las dos cifras pasan el umbral de
0,80, pero la que se presenta como «la de la 27» no lo es.

---

## 2. Los 34 mutantes no muertos, uno por uno

Veredicto de cada uno: **MUERTO** (hay test nuevo en este carril y se ha
verificado a mano que el mutante falla), **EQUIVALENTE** (razonado) o
**ABIERTO** (vive y se declara vivo).

### 2.1 SURVIVED (24)

| # | clase | método:línea | mutador | veredicto |
| --- | --- | --- | --- | --- |
| S1 | PersonalAccessToken | `hint`:38 | ConditionalsBoundary | **MUERTO** — el ternario `value.length() <= HINT_LENGTH ? value : …` ya no existe: el dominio impone un mínimo (M10) y `hint()` es un `substring` sin rama. Mutante eliminado por construcción. |
| S2 | PersonalAccessToken | `lambda$new$0`:23 | ConditionalsBoundary | **MUERTO** — es el techo del alfabeto, `c < 0x7f` → `c <= 0x7f`. Aplicado a mano: las 12 pruebas del token seguían verdes. Test nuevo `s6_rejectsTheDeleteControlCharacterJustAboveThePrintableRange`; con el mutante puesto falla («Expected ValidationException to be thrown, but nothing was thrown»), sin él pasa. |
| S3 | GithubConnectorController | `delete`:89 | VoidMethodCall (`rejectQuery`) | **MUERTO** — test nuevo `s6_aQueryStringOnDisconnectingIsRejectedWithoutNamingAField`. Verificado quitando la llamada a mano: falla. |
| S4 | GithubConnectorController | `startImport`:102 | VoidMethodCall (`rejectQuery`) | **MUERTO** — test nuevo `s6_aQueryStringOnStartingAnImportIsRejectedWithoutNamingAField`. Verificado igual. |
| S5 | …$ImportResponse | `failed`:318 | PrimitiveReturns (int→0) | **MUERTO** — todos los recibos serializados en las pruebas traían `failed` 0. Test nuevo `s30_theReceiptSerialisesFailuresAndTruncationWithTheirRealValues` con `failed` 2. Verificado sobrescribiendo el accesor a `return 0`: falla. |
| S6 | …$ImportResponse | `truncated`:318 | BooleanFalseReturn | **MUERTO** — mismo test, `truncated` true. Verificado igual. |
| S7 | GithubConnectorController | `titleOf`:278 | EmptyObjectReturns (→ `""`) | **ABIERTO** — el título humano del problema JSON. Ninguna prueba afirma el `title` de ningún problema; sólo `code` y `status`. Matarlo pide decidir antes si el `title` es superficie de contrato, y el `.feature` no lo nombra en ninguna fila. No lo invento yo: **queda vivo y declarado**. |
| S8 | ConnectorKeyRing | `malformed`:55 | NegateConditionals | **ABIERTO** — rama de diagnóstico del arranque (@s4). Las cuatro filas de clave malformada mueren; esta negación cambia sólo por qué camino se construye el mensaje. No lo he tocado por falta de un Then que lo distinga. |
| S9 | IssueImportReceipt | `close`:85 | NegateConditionals | **ABIERTO** — está en el borde de M5, pero M5 lo cierra por el lado del SQL (`GREATEST`), que es donde vive la garantía. Declarado vivo. |
| S10 | BoundedResponse | `bounded`:82 | ConditionalsBoundary | **ABIERTO** — frontera del límite de octetos leídos. |
| S11 | BoundedResponse | `bounded`:90 | NegateConditionals | **ABIERTO** — ídem. |
| S12 | BoundedResponse | `expired`:98 | ConditionalsBoundary | **ABIERTO** — frontera del plazo. Los tres piden pruebas de socket con temporización real (`ImportSocketTest`), que es material del carril de red, no de éste. |
| S13 | HttpGithubIssueSource | `retryAfter`:158 | PrimitiveReturns (int→0) | **ABIERTO** — el `Retry-After` que sale en @s20/@s21; hay oráculo del valor 45 en el caso de uso pero no del cálculo del adaptador. |
| S14 | HttpGithubIssueSource | `atLeastOneSecond`:163 | ConditionalsBoundary | **ABIERTO** — el suelo de un segundo del mismo cálculo. |
| S15 | HttpGitlabIssueSource | `issueOf`:97 | NegateConditionals | **NO ES DE ESTA FEATURE** — código de la 29 dentro del ámbito por el comodín. Ver §3, M9/M18. |
| S16 | HttpGitlabIssueSource | `issueOf`:97 | NegateConditionals (2.º) | **NO ES DE ESTA FEATURE** — ídem. |
| S17 | HttpGitlabIssueSource | `retryAfter`:180 | PrimitiveReturns | **NO ES DE ESTA FEATURE** — ídem. |
| S18 | HttpGitlabIssueSource | `atLeastOneSecond`:185 | ConditionalsBoundary | **NO ES DE ESTA FEATURE** — ídem. |
| S19 | PostgresConnectorConnectionStore | `lambda$save$0`:52 | EmptyObjectReturns (Integer→0) | **EQUIVALENTE** — es el valor de retorno del lambda que envuelve `jdbc.update` dentro del `TransactionTemplate`; nadie lo lee. Cambiarlo por 0 no altera ninguna conducta observable. |
| S20 | PostgresConnectorConnectionStore | `lambda$invalidate$0`:82 | EmptyObjectReturns | **EQUIVALENTE** — ídem. |
| S21 | PostgresIssueImportReceiptStore | `lambda$progress$0`:115 | EmptyObjectReturns | **EQUIVALENTE** — ídem. |
| S22 | PostgresImportedTaskCommit | `insertTask`:88 | VoidMethodCall (`expectOneRow`) | **ABIERTO** — quitar la comprobación de «exactamente una fila». Matarlo pide fabricar un `UPDATE`/`INSERT` que afecte a cero filas contra el PostgreSQL real; es alcanzable, pero no me ha dado la noche. Declarado vivo. |
| S23 | PostgresImportedTaskCommit | `insertEvent`:104 | VoidMethodCall | **ABIERTO** — ídem. |
| S24 | PostgresImportedTaskCommit | `insertLink`:119 | VoidMethodCall | **ABIERTO** — ídem. |

### 2.2 NO_COVERAGE (10) — ramas que ninguna prueba ejecuta

| # | clase | método:línea | mutador | veredicto |
| --- | --- | --- | --- | --- |
| N1 | IssueImportReceipt | `isRunning`:52 | BooleanFalseReturn | **CÓDIGO MUERTO** — `grep -rn "isRunning" backend/src` da **una sola** línea: su propia declaración. Nadie lo llama, ni en producción ni en pruebas. Se propone borrarlo; **no lo borro yo** porque hay carriles en vuelo sobre estos ficheros y una supresión que se cruce con un uso nuevo rompe la rama ajena en silencio. Decisión para el propietario. |
| N2 | IssueImportReceipt | `isRunning`:52 | BooleanTrueReturn | **CÓDIGO MUERTO** — ídem. |
| N3 | PostgresIssueImportReceiptStore | `lambda$finish$1`:144 | NullReturns | **CUBIERTO AHORA** — es el `read(...)` de dentro de `finish`. La prueba nueva de M5, `s12_aClockThatWentBackwardsNeverClosesAReceiptBeforeItStarted`, lo ejerce contra el PostgreSQL real, igual que las demás de cierre de recibo. Que saliera sin cobertura apunta a que las pruebas de Testcontainers no participaron en aquella corrida; hay que confirmarlo en la próxima campaña. |
| N4 | PostgresIssueImportReceiptStore | `missing`:217 | NullReturns | **ABIERTO** — «el recibo se esfumó mientras se cerraba». Rama defensiva que exige borrar la fila entre el UPDATE y el SELECT dentro de la misma transacción. Declarada viva. |
| N5 | GithubConnectorController | `githubUnavailable`:214 | NullReturns | **ABIERTO** — el constructor del problema 503 GITHUB_UNAVAILABLE. @s28 lo ejerce de punta a punta, así que otra vez huele a corrida sin la clase de prueba que lo cubre. A confirmar midiendo. |
| N6 | HttpGithubIssueSource | `get`:113 | VoidMethodCall (`Thread::interrupt`) | **ABIERTO** — restablecer la bandera de interrupción tras una `InterruptedException`. Ninguna prueba interrumpe el hilo. Es higiene de concurrencia, no conducta de contrato. |
| N7 | BoundedResponse | `headers`:34 | NullReturns | **ABIERTO** — accesor del portador de datos; sólo lo usa `HttpCalendarFeed` (feature 28). |
| N8 | HttpGitlabIssueSource | `get`:139 | VoidMethodCall | **NO ES DE ESTA FEATURE** — código de la 29. |
| N9 | HttpGitlabIssueSource | `retryAfter`:181 | Math | **NO ES DE ESTA FEATURE** — ídem. |
| N10 | HttpGitlabIssueSource | `retryAfter`:181 | PrimitiveReturns | **NO ES DE ESTA FEATURE** — ídem. |

**Resumen del inventario:** 6 muertos con test nuevo, 3 equivalentes
razonados, 2 código muerto con propuesta, 1 cubierto ahora, 7 pertenecen a la
feature 29 y no deberían estar contados aquí, **15 declarados abiertos** con
su razón.

---

## 3. Un apartado por motivo

### M3 y M8 — `GithubIssueConnections` fuera de todo ámbito · **CERRADO (verificado)**

Los arregló el propietario en `7cf3f9c2`. Verificado que el patrón casa **y
da mutantes de verdad**, aplicándolos a mano sobre
`ImportGithubIssuesTest` (37 pruebas):

| mutante | resultado |
| --- | --- |
| `source()` → `return ""` (EmptyObjectReturns) | **27 de 37 fallan** |
| `invalidate()` sin la llamada a `connections.invalidate` (VoidMethodCall) | **1 falla** (`s22_aRejectedTokenHalfwayKeepsWhatWasCreatedAndTurnsTheConnectionInvalid`) |
| `find()` → `Optional.empty()` (EmptyObjectReturns) | **35 de 37 fallan** |

El glob `com.apptolast.organization.application.GithubIssueConnections*` casa
con la clase, y sus tres mutantes con sentido —el `source` que acaba en cada
recibo, el `invalidate` que marca la conexión inservible tras el 401 de @s22, y
la lectura de la fila— **mueren**. El arreglo es bueno.

De paso, `7cf3f9c2` muda `ImportGuard*` al ámbito de la 29, que es quien lo
usa. Eso cierra la segunda mitad de M18.

### M4 — cuatro cláusulas del Then sin oráculo · **CERRADO**

**(a) @s3 «el resto de rutas de proyectos y tareas responde con normalidad».**
Nuevo `backend/.../GithubConnectorIsolationTest.java`:
`s3_theGateThatAnswersServiceUnavailableNeverCoversProjectsOrTasks` (cinco
rutas) y `s3_projectAndTaskRoutesDoNotNeedTheConnectorKeyToAnswer` (ocho
clases de proyectos y tareas, regla ArchUnit).
*Rojo acreditado:* ensanchando `ConnectorsGate.PREFIX` de
`/api/v1/me/external-calendar` a `/api/v1`, las cinco filas fallan («el filtro
de conectores no puede alcanzar /api/v1/projects»). Deshecho.

**(b) @s29 «no se crea ningún recibo nuevo en ninguna fila y el servidor falso
recibe cero peticiones».** Las cuatro pruebas `s29_*` de
`ImportGithubIssuesTest` sólo afirmaban la excepción. Ahora capturan
`receipts.size()` y `source.calls().size()` antes y los comparan después.
*Rojo acreditado:* moviendo `receipts.begin(...)` por delante de las
comprobaciones de validez y de proyecto, tres de las cuatro fallan con «no se
crea ningún recibo nuevo ==> expected: <0> but was: <1>». Deshecho. (La
cuarta no falla y es correcto: sin conexión no se llega ni a `begin`.)

**(c) @s27 «no se relanza ninguna importación automáticamente».**
`s27_onlyTheHttpBoundaryAndItsWiringCanStartAnImport`: sólo los controladores,
`ConnectorConfiguration` e `ImportIssues` pueden depender de
`ImportIssuesUseCase`. Más `s27_nothingOfTheConnectorRunsOnStartup`
(`ApplicationRunner`/`CommandLineRunner`).
*Rojo acreditado:* creando en `adapter.config` un `ImportRelaunchSchedule`
que llama al caso de uso, la regla falla nombrando las tres violaciones.
Borrado.

**(d) @s41 fila 4 «la sesión vence en mitad de la importación».** Nuevo
`frontend/src/github-connector-session-expiry.test.tsx`, en fichero aparte
para no chocar con el carril que lleva M1/M6 sobre
`github-connector.test.tsx`. Dos pruebas: el 401 de sesión durante el POST de
importación avisa a quien gobierna el acceso, y no se reintenta nada.
*Rojo acreditado:* quitando `if (response.status === 401) onUnauthorized?.(401)`
de `api-client.ts` → «expected [] to deeply equal [ 401 ]»; añadiendo un
reintento en el `catch` de `startImport` → «expected […] to have a length of 1
but got 2». Deshechos los dos.

### M5 — el `GREATEST` del SQL sin oráculo · **CERRADO**

`GithubConnectorPersistenceTest.s12_aClockThatWentBackwardsNeverClosesAReceiptBeforeItStarted`:
se cierra un recibo con un instante **anterior** al de comienzo y se exige que
`finished_at` acabe valiendo `started_at`.
*Rojo acreditado:* quitando `GREATEST(?, started_at)` del UPDATE, la prueba
falla con `StorageUnavailableException` — que es exactamente el 500 que el
CHECK de la V25 (`finished_at IS NULL OR finished_at >= started_at`) produce
al otro lado. Restaurado y verde.

### M7 — la excepción de descifrado escapaba y dejaba el recibo `running` · **CERRADO (defecto de producción arreglado)**

*Rojo:* `ImportGithubIssuesTest.b_anUndecipherableTokenLeavesTheReceiptFailedAndNoImportInProgress`
falló con «el recibo no puede quedarse en running ==> expected: <failed> but
was: <running>».

*Verde mínimo:* `ImportIssues.run()` gana un
`catch (SecretUndecipherableException error)` que cierra el recibo como
`failed` con `errorCode` **CONNECTOR_KEY_MISMATCH** —el mismo código que el
adaptador HTTP ya daba como 503— y **relanza la excepción tal cual**, para que
la respuesta siga siendo 503 CONNECTOR_KEY_MISMATCH y `GithubConnectorApiTest`
no cambie.

Efecto: rotar `APP_CONNECTOR_KEY` sin conservar `key-previous` ya no deja una
importación fantasma que bloquee las siguientes con un 409 IMPORT_IN_PROGRESS
mentiroso ni esconda el botón de importar durante quince minutos.

### M9 y M18 — el comodín arrastra producción de la feature 29 · **DIAGNOSTICADO, no aplicado**

`backend/build.gradle.kts` lo lleva el propietario; no lo he tocado. Lo que
haría, exacto:

**Quitar** de `githubConnectorClasses` (hoy en `:137`) esta línea:

```
"com.apptolast.organization.adapter.connectors.*",
```

**Sustituirla** por estas cinco, que son las clases de ese paquete que sí son
de la 27 o compartidas con la 28, nunca de la 29:

```
"com.apptolast.organization.adapter.connectors.GithubApiBase*",
"com.apptolast.organization.adapter.connectors.HttpGithubIssueSource*",
"com.apptolast.organization.adapter.connectors.AesGcmSecretCipher*",
"com.apptolast.organization.adapter.connectors.ConnectorKeyRing*",
"com.apptolast.organization.adapter.connectors.BoundedResponse*",
```

El paquete tiene exactamente siete clases; las dos que quedan fuera son
`GitlabApiBase` y `HttpGitlabIssueSource`, que ya están enumeradas una a una en
`additionalConnectorsClasses` (`:109-110`) y son las que producen el solapamiento
que el comentario de `:41-43` dice evitar.

Efecto medido sobre el XML: el ámbito de la 27 pasa de 502 a **421** mutantes y
la puntuación de 93,23 % a **93,59 %** (394/421). Sigue muy por encima del
umbral, y por fin es la de esta feature.

`ImportGuard*` ya está mudado por `7cf3f9c2`, así que esa mitad está hecha.

### M10 — `hint()` devolvía el token entero · **CERRADO (defecto de producción arreglado)**

*Rojo:* `PersonalAccessTokenTest.b_rejectsTokensSoShortThatTheHintWouldBeTheWholeSecret`
con `"a"`, `"ab"`, `"abc"` y `"abcd"` — «Expected ValidationException to be
thrown, but nothing was thrown» en las cuatro.

*Verde mínimo:* `PersonalAccessToken` impone `MIN_LENGTH = HINT_LENGTH + 1`
(cinco caracteres) con código **`TOO_SHORT`**, y `hint()` pierde el ternario:
es siempre `substring(length - 4)`, un sufijo estricto. Aplica también al PAT
de GitLab, porque `upTo(...)` comparte el constructor: la misma fuga en
`PostgresGitlabConnectionStore.token_hint` queda cerrada de paso.

Segundo test: `b_theHintIsAStrictSuffixOfEveryTokenTheDomainAccepts`.

**Lo que hay que preguntarle al propietario.** La tabla de @s6
(`features/github_connector.feature:94-105`) enumera los rechazos del token y
**no** tiene fila para «token demasiado corto». Ninguna fila existente se
rompe —ninguna conecta con un token de cuatro caracteres o menos— pero esto
añade un rechazo que el contrato no nombra, y el proyecto está rechazando
cierres esta noche justo por enmendar contratos sin contrafirma (M16). **No he
tocado el `.feature`.** La fila que habría que ratificar sería:

```
| token de 4 caracteres | 400 | VALIDATION_ERROR | TOO_SHORT |
```

Nota colateral: la cota inferior del CHECK de la V30 (29 octetos) se calculó
para un token de un carácter. Con el mínimo de cinco, el texto cifrado más
corto posible pasa a 33 octetos. La cota sigue siendo válida, sólo más
holgada de lo necesario; **no toco migraciones**.

### M11 — `ReadIssueImport` no filtraba por `source` · **CERRADO (defecto de producción arreglado)**

*Rojo:* `GithubConnectorQueriesTest.b_aReceiptOfAnotherConnectorIsNotVisibleThroughTheGithubRoute`
— «Expected IssueImportNotFoundException to be thrown, but nothing was
thrown»: `GET /api/v1/me/connectors/github/imports/{id}` devolvía 200 con un
recibo de GitLab del mismo propietario.

*Verde mínimo:* el puerto pasa a `find(ownerId, source, importId)`;
`PostgresIssueImportReceiptStore.find` filtra en el SQL
(`WHERE id=? AND owner_id=? AND source=?`), igual que `latest(ownerId, source)`
ya hacía desde la V29; `ReadIssueImportUseCase.execute` recibe el origen y cada
controlador pasa el suyo (`GithubIssueConnections.SOURCE` /
`GitlabIssueConnections.SOURCE`). El `read(ownerId, importId)` privado se
conserva sin filtro para el relectura interna de `finish`, que ya sabe de qué
fila habla.

Alcance: toca `GitlabConnectorController` (un argumento) y sus dos mocks en
`GitlabConnectorApiTest`. La incoherencia era simétrica: la ruta de GitLab
devolvía recibos de GitHub exactamente igual.

### M12 — el `full_name` de GitHub se guardaba sin revalidar · **CERRADO (defecto de producción arreglado)**

*Rojo:* `ConnectGithubTest.b_aFullNameThatIsNotACanonicalRepositoryIsAnUnusableAnswer`
con `"octocat/Hello World"`, `"octocat/repo?x=1"`, `"octocat/repo#frag"`,
`"octocat"` y `""` — las cinco filas: «Expected GithubUnavailableException to
be thrown, but nothing was thrown».

*Verde mínimo:* `ConnectGithub` pasa `identity.fullName()` por
`new GithubRepository(...)` y traduce el `ValidationException` a
`GithubUnavailableException` (503 GITHUB_UNAVAILABLE). Es el mismo trato que el
adaptador ya daba a un `full_name` ausente o no textual
(`HttpGithubIssueSource.text` → `IssueSourceException.unavailable()`), así que
la superficie queda coherente: respuesta inservible del proveedor, 503, y la
fila anterior intacta. Antes, un `full_name` con espacio o `#` llegaba hasta
`URI.create` y reventaba con `IllegalArgumentException` no capturada, por el
mismo agujero que M7.

### M13 — `Slf4jConnectorAudit` con cero mutantes · **ABIERTO**

Verificado que sigue igual: `grep -c Slf4jConnectorAudit mutations.xml` = **0**,
y la clase está nombrada en el ámbito (`build.gradle.kts:142`). Causa raíz
confirmada: el bloque `pitest` fija `excludedMethods` pero no fija `mutators`
ni **`avoidCallsTo`**, así que rige el `avoidCallsTo` por defecto de PIT, que
suprime las llamadas a `org.slf4j` — y esta clase es puro logging.

No lo arreglo porque **`backend/build.gradle.kts` lo lleva el propietario**. Lo
que hace falta es fijar `avoidCallsTo` explícitamente sin `org.slf4j` dentro,
en el mismo sitio donde ya está `excludedMethods` (`:740`), y volver a medir.
Es el mismo defecto que `progress/auditoria_condiciones_cierre.md` ya declara
bloqueante para `Slf4jWebhookAudit` en la feature 25. **Sigue siendo causal de
rechazo por la condición 7 del veredicto.**

### M14 — el mutante de frontera del cifrador · **CERRADO (verificado dos veces)**

Primero, **medido**: en el XML del 12:35, `AesGcmSecretCipher` tiene **17
mutantes, 17 muertos, 0 supervivientes**. El superviviente que el panel leyó
en el HTML de las 11:49 ya no está: la corrida posterior lo mata.

Segundo, **a mano**, sobre `AesGcmSecretCipherTest` (19 pruebas):

| mutante en `:60` | resultado |
| --- | --- |
| `ciphertext.length <= SHORTEST` → `< SHORTEST` (ConditionalsBoundary) | **falla 1**: `b5_aValidCiphertextOfExactlyTheShortestLengthIsStillRejected` |
| `<=` → `>` (NegateConditionals) | **fallan 5** |

El test nuevo hace lo correcto: cifra la cadena vacía para producir un texto
cifrado **válido** de exactamente 28 octetos, que es el único caso donde `<=`
y `<` se distinguen (con un array de ceros los dos devuelven vacío, uno por la
guarda y otro porque falla la etiqueta GCM). Los dos mutadores que el juez
exigió KILLED lo están.

**Corrección de la cifra:** el commit `e5e0d563` afirma «el cifrador genero 26
mutantes». El artefacto dice **17** (17 muertos). El suelo de 12 se cumple de
sobra, pero la cifra publicada estaba mal por nueve y hay que decirlo.

### M15 — no existía `progress/mutation_github_connector.md` · **CERRADO**

Es este fichero. Los 34 no muertos están en §2, uno por uno y con nombre.

### M16 — el contrato se enmendó sin contrafirma · **DOCUMENTADO (para el propietario)**

No he enmendado ni contrafirmado nada. Esto es lo que pasó, exacto:

- **Commit:** `a1b0d20b`, 10/09/2026 **00:19:48**, autor
  `PabloHurtadoGonzalo <pablofullstackdevelopernrby@gmail.com>`, asunto «fix(github_connector):
  el recibo de la 29 llega con source y projectPath, y el cliente lo decodifica».
- **Lo que cambió en `features/github_connector.feature`** (dos hunks, tres
  líneas):
  1. **`:146`** (@s10, fila «conexión invalid con tres recibos»):
     «…con sus **once** campos» → «…con sus **doce** campos».
  2. **`:165`** (@s12, primer Then): la lista de campos del recibo pasa de
     `id, projectId, repository, status, created, skipped, failed, truncated,
     errorCode, startedAt y finishedAt` (once) a
     `id, source, projectId, projectPath, status, created, skipped, failed,
     truncated, errorCode, startedAt y finishedAt` (doce). O sea: **desaparece
     `repository` y aparecen `source` y `projectPath`**.
  3. **`:166`** (@s12, segundo Then): se añaden dos aserciones nuevas al
     principio de la frase, «source es "github", projectPath es
     "octocat/Hello-World"».
- **Lo que NO lleva:** ni comentario de enmienda ni contrafirma. Las dos
  enmiendas anteriores del mismo fichero (`:55` y `:397`) sí dicen «Enmienda
  del 9 de septiembre de 2026, ratificada por el propietario». `grep ratificad`
  sobre el fichero sigue devolviendo sólo esas dos.
- **Motivo alegado en el cuerpo del commit:** que manda el contrato de la 29
  (`additional_connectors.feature:242`, @s20: los dos recibos tienen las mismas
  claves) y que las dos filas de la 27 estaban caducadas desde `a347936`.

**Lo que hay que preguntarle al propietario, en tres preguntas concretas:**

1. ¿Ratifica que el recibo de la 27 pase de `repository` a `source` +
   `projectPath`, es decir, de once a doce campos, porque lo manda el @s20 de
   la 29? (Es un cambio de superficie HTTP pública, no una errata.)
2. Si la respuesta es sí: **queda una tercera fila caducada que la enmienda no
   tocó.** `@s30`, fila «el recibo propio», sigue diciendo «exactamente los
   **once** campos del recibo». Con @s10 y @s12 ya en doce, el contrato se
   contradice consigo mismo. ¿Se corrige también a doce?
3. ¿Con qué fórmula? Las dos enmiendas anteriores usan «Enmienda del … ,
   ratificada por el propietario» como comentario sobre la tabla. Estas tres
   líneas no tienen ninguno.

Mientras no haya respuesta, **la 27 no puede marcarse `done`**: sería
incoherente con el dictamen que sigue abierto sobre la 29 por una desviación
menor que ésta.

### M17 — el veredicto vigente se emitió sobre otro árbol · **DOCUMENTADO (para el propietario)**

- El veredicto (`progress/judge_github_connector_cierre.md:7`) se emitió sobre
  **`HEAD = cc76ec5`**, y su apartado de disciplina TDD se sostiene en una
  frase literal: «el diff **NO** toca `src/` en absoluto… Cero producción
  nueva».
- **Esa frase dejó de ser cierta el mismo día.** Después del veredicto:
  - `a1b0d20b` (00:19) toca `frontend/src/github-connector-client.ts`:
    `RECEIPT_FIELDS` pasa a doce claves, el tipo `GithubImportReceipt` cambia, y
    `decodeReceipt` pasa de `typeof value.repository !== "string"` a
    `value.source !== GITHUB || !nonEmpty(value.projectPath)`.
  - `44011086` (10:45) vuelve a tocar el mismo fichero exportando dos
    constantes.
  - Y ahora, **este carril**, que toca producción de verdad y a conciencia:
    `ImportIssues`, `ConnectGithub`, `PersonalAccessToken`, `ReadIssueImport`,
    `ReadIssueImportUseCase`, `IssueImportReceiptStore`,
    `PostgresIssueImportReceiptStore`, `GithubConnectorController` y
    `GitlabConnectorController` — cuatro defectos de producto (M7, M10, M11,
    M12), cada uno con su rojo acreditado.
- **Consecuencia:** el APPROVED que se invoque para cerrar la 27 **no ha visto
  la superficie que se va a cerrar**. Cerrar con él es exactamente el «una
  condición declarada cerrada tampoco es evidencia» del encargo.

**Lo que hay que pedirle al propietario:** un veredicto nuevo sobre el árbol
final, o al menos un addendum del juez que cubra el diff de producción posterior
a `cc76ec5`. No es una objeción al contenido de los cambios —todos llevan rojo
acreditado, y están en §3— es que nadie con el sombrero de juez los ha leído.

### M19 — dos aserciones que no podían fallar · **CERRADO**

Las dos pruebas de `GithubConnectorPersistenceTest` que justifican la V30
construían el texto cifrado a mano
(`new byte[NONCE_BYTES + token.length() + TAG_BYTES]`) y luego afirmaban su
tamaño. **Roto para demostrarlo:** con el cifrador real mutado para volver a
escribir un byte de versión al principio —el formato que la V30 dice
impedir—, **la clase entera siguió verde: 28 pruebas, 0 fallos**. La mitad
que dice atar el rango al formato en reposo no ataba nada.

**Con dientes:** las dos llaman ahora a `AesGcmSecretCipher` de verdad, con
una clave de pruebas, y comparan lo que la columna guarda contra lo que el
cifrador produce y contra las cotas de la V30 (29 y 283).
*Rojo acreditado:* con el mismo mutante del byte de versión puesto, fallan las
dos, y por los dos motivos distintos que había que ver:

- la del token más corto, «Expected size: 33 but was: 34»;
- la del token más largo, **`StorageUnavailableException`** — porque 284
  octetos violan el CHECK de la V30. Ése es literalmente el 500 que estas dos
  pruebas se escribieron para impedir que volviera.

Restaurado el cifrador, las 29 pruebas pasan.

(De paso: «el token más corto que el dominio admite» ya no es `"x"` sino cinco
caracteres, por M10.)

### M20 — la campaña de frontend no dice contra qué commit se midió · **ABIERTO**

`progress/mutacion_github_connector_frontend_cierre.md:3` sigue diciendo sólo
«sobre `main`». El informe se escribió a las 10:55:31 y `44011086` toca
`frontend/src/github-connector-client.ts` y su prueba a las 10:45:01, diez
minutos antes; una corrida de 589 mutantes con `concurrency: 8` empieza
bastante antes de las 10:45.

**No lo maquillo escribiendo un commit a posteriori.** La trazabilidad se
perdió cuando no se anotó, y ponerle ahora un sha sería inventarla. Lo único
honesto es volver a medir el frontend contra el árbol final y anotar el commit
en la primera línea del informe. Además, este carril **añade** un fichero de
pruebas de frontend (`github-connector-session-expiry.test.tsx`), así que la
campaña anterior está caducada de todas formas.

Y lo mismo, exactamente, le pasa al backend: el `mutations.xml` del 12:35:09
tampoco nombra su commit (§1).

---

## 4. Qué queda abierto, en una lista

1. **M13** — `Slf4jConnectorAudit` sigue a cero mutantes. Condición 7 del
   veredicto (`judge_github_connector_cierre.md:276-280`) **incumplida**. Arreglo en `build.gradle.kts` (fijar
   `avoidCallsTo` sin `org.slf4j`), que lleva el propietario.
2. **M9/M18** — el ámbito sigue midiendo 81 mutantes de la feature 29. Los
   cinco patrones de sustitución están en §3; los aplica el propietario.
3. **M16** — el contrato tiene una enmienda sin contrafirma y una tercera fila
   (`@s30`, «once campos») que se quedó descolgada. Tres preguntas al
   propietario en §3.
4. **M17** — hace falta un veredicto sobre el árbol final: este carril mete
   producción nueva en nueve ficheros.
5. **M20** — las dos campañas (backend y frontend) están caducadas y sin
   commit anotado. Hay que volver a medirlas contra el árbol final.
6. **15 mutantes declarados vivos** en §2, con su razón cada uno; de ellos, 7
   son de la feature 29 y desaparecen del recuento en cuanto se aplique §3
   M9/M18.
7. **`IssueImportReceipt.isRunning()` es código muerto** (N1, N2). Propuesto su
   borrado; no lo hago yo por los carriles en vuelo.

## 5. Lo que se ha ejecutado en verde

Por clase, como manda la casa (nunca la suite entera):

- `PersonalAccessTokenTest`, `GitlabTokenTest`, `GithubRepositoryTest`,
  `ExternalIssueTest`, `ExternalIssueCriterionTest` y el resto de
  `domain.*` — verdes.
- `ImportGithubIssuesTest` (37), `ImportGitlabIssuesTest`, `ConnectGithubTest`
  (21), `GithubConnectorQueriesTest`, `ConnectorStatusSourcesTest`,
  `GitlabConnectionUseCasesTest` y el resto de `application.*` — verdes.
- `GithubConnectorApiTest` (50), `GitlabConnectorApiTest`,
  `GithubConnectorWiringTest`, `GitlabConnectorWiringTest`,
  `ConnectorCatalogApiTest` — verdes.
- `AesGcmSecretCipherTest` (19), `GithubApiBaseTest`,
  `HttpGithubIssueSourceTest`, `ConnectorAuditTest` — verdes.
- `GithubConnectorPersistenceTest` (29, Testcontainers),
  `GitlabConnectorPersistenceTest` — verdes.
- `ArchitectureTest`, `ExternalCalendarIsolationTest` y el nuevo
  `GithubConnectorIsolationTest` (16) — verdes.
- Frontend: `github-connector.test.tsx`, `github-connector-client.test.ts`,
  `github-connector-routing.test.tsx`, `api-client.test.ts` y el nuevo
  `github-connector-session-expiry.test.tsx` — **129 pruebas, 129 verdes**.
  `tsc --noEmit` limpio y `prettier --check` conforme.
- `spotlessCheck` conforme.
