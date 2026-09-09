# TDD — feature 29 `additional_connectors` (carril aislado)

Worktree `C:/Users/vhurt/ow-worktrees/additional-connectors`, rama
`claude/additional-connectors` desde `main` en `99d3e64`.

Contrato: `features/additional_connectors.feature` (38 escenarios).

## Reparto del alcance

La feature está partida en dos mitades por dependencias:

- **Mitad A — segundo gestor de issues (GitLab)**: `@s8`…`@s32` (backend),
  `@s34`…`@s37` (pantalla `/conectores/gitlab`) y la parte de `@s38` que le
  toca. Depende sólo de la feature 27, que ya está en `main`. **Es el alcance
  de esta sesión.**
- **Mitad B — catálogo `GET /api/v1/me/connectors`**: `@s1`…`@s7`, `@s33` y la
  fila de `@s2` de cada feature. Cada fila deriva su estado de la fuente de su
  feature (24, 25, 26, 27, 28, 29), así que necesita 25, 26, 28 y 30 dentro.
  Se aborda cuando el lead avise de que están integradas.

## Decisiones de contrato tomadas antes del primer ciclo

Las tres primeras nacen de la línea 5 del `.feature` («donde 27 fija algo
distinto, 27 prevalece»), que resuelve los choques entre la propuesta —escrita
antes de que 27 existiera— y lo que 27 dejó fijado en `main`.

1. **Un solo caso de uso de importación.** `@s20` exige que GitHub y GitLab
   compartan caso de uso y forma de recibo. `ImportGithubIssues` se generaliza
   a `ImportIssues`, parametrizado por origen y por una pasarela de conexión
   propia de cada gestor. No se copia el caso de uso.
2. **Claves del recibo.** 27 fijó `{id, projectId, repository, …}` y `@s15`
   fija `{id, source, projectId, projectPath, …}`. `@s20` obliga a que ambos
   recibos tengan **las mismas claves**, así que el recibo compartido pasa a
   llevar `source` y `projectPath` en lugar de `repository`, también para
   GitHub. Es la única lectura que satisface `@s15` y `@s20` a la vez; queda
   señalada aquí para el juez porque toca la línea 165 del `.feature` de 27.
3. **Nonce.** `@s9` habla de columna `token_nonce` de 12 bytes; esa redacción
   viene de la propuesta, anterior a 27. 27 fijó un formato de secreto único
   —1 byte de versión + 12 de nonce + texto cifrado + etiqueta en una sola
   columna, con `CHECK (octet_length(...))`— y ese formato prevalece. El nonce
   sigue siendo nuevo por escritura y de 12 bytes, y hay prueba que lo fija.
4. **`apiBase` no se persiste.** `@s11` dice que la base de API sale sólo de la
   configuración del servidor; guardarla por conexión reabriría la superficie
   SSRF que 27 cerró. Se lee de `app.gitlab.api-base` en cada respuesta.
5. **Migración V29**, reservada para este carril. No se toca ninguna existente.

## Inventario honesto de oráculos (9 de septiembre de 2026)

Estado del árbol: `gradlew compileJava compileTestJava` **compila limpio** tras
el rebase sobre `main`. Verde comprobado por clase en `GitlabConnectionUseCasesTest`
(26 pruebas), `GitlabConnectorWiringTest` y las demás clases citadas abajo.

Leyenda de la columna **estado**:

- **cerrado** — hay al menos una prueba que falla si se rompe el comportamiento
  que el escenario describe, y esa prueba se escribió antes que la producción.
- **heredado** — el comportamiento vive en código compartido con la feature 27
  (`ImportIssues`, `IssueImportReceipt`, el mapeo `ExternalIssue` → tarea) y su
  oráculo está en `ImportGithubIssuesTest`. Vale como red de seguridad, pero
  **no hay oráculo con datos de GitLab**: una regresión que sólo afectara al
  camino de GitLab no la detectaría. Se anota como deuda, no como cerrado.
- **parcial** — hay oráculo para algunas filas del Scenario Outline y no para
  todas; se detalla cuáles faltan.
- **abierto** — no hay ninguna prueba, y en varios casos tampoco producción.

| `@s` | qué exige | estado | prueba → fichero:línea |
| --- | --- | --- | --- |
| `@s1` | catálogo de seis filas en orden fijo | **cerrado** | caso de uso `ReadConnectorCatalogTest`; frontera `ConnectorCatalogApiTest` (`s1_…`, dos pruebas); cableado `ConnectorCatalogWiringTest` (`s1_…`, dos) |
| `@s2` | cada fila deriva su estado de su fuente | **cerrado** | las doce filas, una a una, en `ConnectorStatusSourcesTest` (21 pruebas); la frontera en `ConnectorCatalogApiTest:s2_…` |
| `@s3` | sin clave, las filas que cifran salen `disabled` | **cerrado** | `ReadConnectorCatalogTest` (dos mitades); `ConnectorStatusSourcesTest:s3_…` (quién cifra y quién no, las seis); `ConnectorCatalogWiringTest:s3_…` (con el cableado real, y a la lectura de GitLab no se la llega a llamar); `ConnectorCatalogApiTest:s3_…` |
| `@s4` | el catálogo sólo ve al propietario autenticado | **cerrado** | `ReadConnectorCatalogTest:s4_…` (a quién se preguntó, no sólo qué contestó); `ConnectorStatusSourcesTest:s4_…`; `GitlabConnectorPersistenceTest:168` |
| `@s5` | `lastError` sin secretos ni texto del proveedor | **cerrado** | `ConnectorCatalogApiTest:s5_…` (el cuerpo entero: sin `glpat`, sin `invalid_token`, sin pista, sin ruta y sin ninguna URL); `ConnectorStatusSourcesTest:s5_…`; el cliente se niega a leer un `lastError` con texto del proveedor en `connectors-catalog-client.test.ts` |
| `@s6` | consultar el catálogo no sincroniza ni escribe | **cerrado** | `ReadConnectorCatalogTest:s6_…`; `ConnectorCatalogApiTest:s6_…` (tres lecturas idénticas y ninguna otra interacción con el caso de uso); la pantalla hace exactamente un GET en `connectors-catalog.test.tsx` |
| `@s7` | almacenamiento caído no da catálogo optimista | **cerrado** | `ReadConnectorCatalogTest:s7_…`; `ConnectorCatalogApiTest:s7_…` (503 `STORAGE_UNAVAILABLE` y el cuerpo no contiene `connectors` ni `connected`); la pantalla no pinta ninguna fila en `connectors-catalog.test.tsx` |
| `@s8` | `not_connected` en vez de 404 | cerrado | `GitlabConnectionUseCasesTest:38,52,68`; `GitlabConnectorApiTest:132,157`; `GitlabConnectorWiringTest:48` |
| `@s9` | conectar valida el proyecto y cifra el token | cerrado | `GitlabConnectionUseCasesTest:105,127`; `HttpGitlabIssueSourceTest:57,75`; `GitlabConnectorPersistenceTest:138,154,162`; `GitlabConnectorApiTest:211`; `GitlabTokenTest:29` |
| `@s10` | cuerpos inválidos rechazados sin llamar a GitLab | cerrado | `GitlabProjectPathTest:17,32,37,43`; `GitlabTokenTest:17,24`; `GitlabConnectorApiTest:243,260` |
| `@s11` | ruta codificada y base de API sólo del servidor | cerrado | `GitlabApiBaseTest:24,45,52,59`; `GitlabConnectorWiringTest:26,41` |
| `@s12` | GitLab rechaza y no se guarda nada | cerrado | `GitlabConnectionUseCasesTest:172,184,194,207`; `HttpGitlabIssueSourceTest:90,103,111`; `GitlabConnectorApiTest:287,311` |
| `@s13` | sustituir token sube `version` y conserva enlaces | cerrado | `GitlabConnectionUseCasesTest:136`; `GitlabConnectorPersistenceTest:177` |
| `@s14` | desconectar idempotente conservando tareas | cerrado | `GitlabConnectionUseCasesTest:157`; `GitlabConnectorPersistenceTest:192`; `GitlabConnectorApiTest:449` |
| `@s15` | importar crea tareas enlazadas con recibo | cerrado | `ImportGitlabIssuesTest:71,255`; `HttpGitlabIssueSourceTest:151`; `GitlabConnectorApiTest:392`; `GitlabConnectorPersistenceTest:300`; y la última línea (`lastActivityAt` = `finishedAt`) en `GitlabConnectionUseCasesTest:79,87,95` |
| `@s16` | paginar por `X-Next-Page` y marcar `truncated` | **cerrado en esta sesión** | adaptador: `HttpGitlabIssueSourceTest:124,143`, `GitlabApiBaseTest:71`. Las cuatro filas del Outline con datos de GitLab: `ImportGitlabIssuesTest` (`s16_readsAtMostTwoPages…`) |
| `@s17` | excluir incidentes, test cases, tasks y movidas | cerrado | `HttpGitlabIssueSourceTest:168`; `ImportGitlabIssuesTest:94` |
| `@s18` | repetir la importación es idempotente por enlace | cerrado | `ImportGitlabIssuesTest:108` |
| `@s19` | unicidad de enlaces por origen | cerrado | `ImportGitlabIssuesTest:130`; `GitlabConnectorPersistenceTest:224,264` |
| `@s20` | un solo caso de uso sirve a los dos gestores | cerrado | `ImportGitlabIssuesTest:144` |
| `@s21` | el mapeo issue → tarea es el de 27 | **cerrado en esta sesión** | las cinco reglas viven en `ExternalIssue`, que no sabe de origen, y ya tenían oráculo agnóstico en `ExternalIssueTest` y `ExternalIssueCriterionTest`. Lo que faltaba era atarlas al camino de GitLab: `ImportGitlabIssuesTest` (`s21_…`) |
| `@s22` | precondiciones antes de contactar GitLab | **cerrado en esta sesión** | frontera: `GitlabConnectorApiTest:327,345`. Las cinco filas restantes más el orden entre comprobaciones, con el doble de GitLab: `ImportGitlabIssuesTest` (`s22_…`, seis pruebas) |
| `@s23` | token rechazado marca la conexión y deja recibo | cerrado | `ImportGitlabIssuesTest:205`; `GitlabConnectorPersistenceTest:207`; `GitlabConnectorApiTest:359` |
| `@s24` | cuota con `Retry-After` | cerrado | `HttpGitlabIssueSourceTest:187,198`; `ImportGitlabIssuesTest:241`; `GitlabConnectorApiTest:311` |
| `@s25` | indisponible, sin seguir redirecciones | cerrado | `HttpGitlabIssueSourceTest:211,223,232`; `ImportGitlabIssuesTest:226` |
| `@s26` | fallo en la página 2 conserva lo confirmado | **cerrado en esta sesión** | `ImportGitlabIssuesTest:266,286` (ver bitácora) |
| `@s27` | una sola importación por propietario | cerrado | `GitlabConnectionUseCasesTest:219`; `GitlabConnectorPersistenceTest:273` |
| `@s28` | el `running` abandonado deja de bloquear a los 15 min | cerrado | `GitlabConnectionUseCasesTest:234,243`; `GitlabConnectorPersistenceTest:286` |
| `@s29` | sin clave, `CONNECTORS_DISABLED` sin tocar nada | cerrado | `GitlabConnectionUseCasesTest:253`; `GitlabConnectorApiTest:458`; `GitlabConnectorWiringTest:63` |
| `@s30` | el recibo ajeno equivale al inexistente | cerrado | `GitlabConnectorApiTest:421,431` |
| `@s31` | sesión, CSRF y origen en todas las rutas | **cerrado** | las de GitLab: `GitlabConnectorApiTest:480,488,507,522`. Las del catálogo: `ConnectorCatalogApiTest:s31_…` (sin sesión 401; con credencial Bearer válida 403 `API_SCOPE_DENIED`, ver enmienda; query string rechazada antes del caso de uso) |
| `@s32` | el token nunca sale salvo en `PRIVATE-TOKEN` | **cerrado** | servidor: `GitlabTokenConfinementTest` (4 pruebas). Navegador: `gitlab-connector.test.tsx:s32_…` (ni `localStorage`, ni `sessionStorage`, ni cookies, ni el HTML serializado de ningún elemento) y `gitlab-connector-client.test.ts:@s32` (el token nunca va en la URL) |
| `@s33` | pantalla `/conectores` | **cerrado** | `connectors-catalog.test.tsx` (13 pruebas) y `connectors-catalog-client.test.ts` (12). Pendiente de cableado de ruta, no de oráculo |
| `@s34` | `/conectores/gitlab` sin conexión | **cerrado** | `gitlab-connector.test.tsx:@s34` (8 pruebas) |
| `@s35` | pantalla con conexión existente | **cerrado** | `gitlab-connector.test.tsx:@s35` (10 pruebas) |
| `@s36` | errores recuperables con su acción | **cerrado** | `gitlab-connector.test.tsx:@s36` (5 pruebas, una por fila; la sexta fila la cierra la prueba de `@s29`) |
| `@s37` | cancelación y cierre de sesión | **cerrado** | `gitlab-connector.test.tsx:@s37` (4 pruebas, todas sobre el `AbortSignal` que viajó) |
| `@s38` | responsive, texto ampliado, teclado y axe | **parcial** | el foco en cada cambio de estado: `gitlab-connector.test.tsx:@s38` (3 pruebas). Anchos, texto al 200 %, zoom, 44 × 44, teclado y axe: bloqueados por el cableado de rutas |

**Recuento: 35 escenarios cerrados de 38** (24 al abrir esta sesión, más
`@s1`, `@s2`, `@s3`, `@s4`, `@s5`, `@s6`, `@s7`, `@s31`, `@s32`, `@s33`, `@s34`,
`@s35`, `@s36` y `@s37`; se descuenta que `@s2`, `@s4`, `@s5` y `@s31` ya
contaban como parciales, no como cerrados).

Quedan **3 abiertos o parciales**:

- `@s38` — **parcial**. Su tercera línea (el foco va al h1 o al aviso de
  resultado en cada cambio de estado) está cerrada con tres pruebas de
  componente y su rojo acreditado. Las otras cuatro —tres anchos, texto al
  200 %, zoom nativo, 44 × 44, recorrido de teclado y axe— son de navegador y
  **no se pueden escribir todavía**: las dos rutas no están cableadas (ver «Lo
  que le pido al orquestador», abajo). No se ha escrito una spec de E2E que no
  se pueda ejecutar: una spec verde por no llegar a correr es peor que ninguna.
- `@s2` y `@s31` — **cerrados**, pero conviene leer la nota: `@s2` tiene ahora
  las doce filas con oráculo en `ConnectorStatusSourcesTest` (fuente por
  fuente) más la frontera en `ConnectorCatalogApiTest`; `@s31` tiene sus dos
  filas del catálogo en `ConnectorCatalogApiTest`, con la enmienda de contrato
  que se explica más abajo.

### Aviso sobre la numeración

`ConnectorAuditTest` lleva pruebas con prefijo `s34_`: son de la **feature 27**,
cuyo `@s34` es la auditoría del conector. No cubren el `@s34` de esta feature
(la pantalla `/conectores/gitlab`). Igual pasa con los prefijos de
`ImportGithubIssuesTest`: toda su numeración es la de 27. Al leer este mapa hay
que quedarse con la columna de la izquierda, no con el nombre del método.

### Lo que falta para poder cerrar la feature

Sólo dos cosas, y ninguna es de este carril:

1. **Cablear las dos rutas**, que viven en ficheros del orquestador
   (`REPARTO_NOCHE.md` §3). Sin esto las pantallas existen y están probadas
   pero no se pueden abrir, y `@s38` no se puede medir en navegador.
2. **La puerta de mutación de frontend**, que también es del orquestador.

## Bitácora de ciclos

### Cierre del ciclo que quedó a medias — `@s15`, última línea

`GET /api/v1/me/connectors/gitlab` publicaba `lastActivityAt` leyendo la
columna de la fila de conexión, que sólo se escribe al conectar. El `.feature`
pide dos cosas distintas: en `@s15`, `lastActivityAt` igual a `finishedAt` del
recibo; en `@s9` y en la fila `gitlab` de `@s2` sin importaciones, el instante
de la conexión. Una sola columna no puede decir las dos, así que la última
actividad **se deduce**: último recibo de origen `gitlab` del propietario, con
el alta como respaldo.

Tres pruebas nuevas en `GitlabConnectionUseCasesTest:79,87,95`.

**Rojo acreditado con dos roturas distintas:**

1. `lastActivity` devolviendo `connection.lastActivityAt()` (la producción
   anterior): fallan `s15_theLastActivityIsTheEndOfTheLastImport…`
   (`expected 2026-09-09T11:00:00Z but was 2026-09-09T10:00:00Z`) y
   `s15_arunningImportCountsFromWhenItStarted…`. La tercera pasa, como debe:
   es la que fija el respaldo.
2. `latest(ownerId, "github")` mapeando siempre `finishedAt`: fallan las tres,
   incluida `s15_animportOfTheOtherSourceDoesNotCountAsGitlabActivity`, que es
   la que impide que una importación de GitHub se cuele como actividad de
   GitLab, y `…arunningImport…`, que es la que obliga a la rama del recibo en
   curso (`finishedAt` nulo → vale `startedAt`).

Verde restaurado: `GitlabConnectionUseCasesTest` 26/26 y
`GitlabConnectorWiringTest` en verde.

**Arrastre del rebase corregido de paso**: `SecretCipher.decrypt` devuelve
`Optional<String>` en `main`, y dos aserciones de este fichero (`:122`, `:146`)
comparaban el `Optional` contra la cadena desnuda. Fallaban con
`expected: <glpat-…WXYZ> but was: <Optional[glpat-…WXYZ]>`. Es desalineación
del rebase, no un defecto de producción.

Commit `c513c1b`.

### `@s26` — un fallo en la página 2 conserva lo que confirmó la página 1

Dos pruebas nuevas en `ImportGitlabIssuesTest:266,286`: página 1 llena, página 2
con proveedor caído. El recibo queda `failed` con `GITLAB_UNAVAILABLE` pero
declara `created 100`, `truncated false` y `finishedAt` no nulo, con sus 100
tareas, 100 eventos y 100 enlaces intactos. La segunda prueba cierra la última
línea del escenario: repetir con las dos páginas sanas omite las 100 ya
enlazadas y sólo crea lo que faltaba.

**Ambas pasaron a la primera.** Es el caso que el propio inventario llama
«heredado»: el mecanismo vive en `ImportIssues`, compartido con 27. Un test que
pasa a la primera no demuestra nada, así que el rojo se acreditó rompiendo la
producción, y hubo que afinar la rotura hasta dar con la que distingue:

1. Sacar `receipts.progress` del bucle de issues y dejarlo al final de cada
   página: **sigue verde**, y es correcto que siga. Esa granularidad
   issue-a-issue la guarda el `@s27` de la feature 27
   (`ImportGithubIssuesTest:423`), no este escenario. La primera rotura no
   valía como evidencia y queda anotada para que nadie la repita.
2. Subir `receipts.progress` fuera del bucle de **páginas**: el fallo de la
   página 2 se lleva por delante el recuento de la 1 y cae
   `s26_afailureOnTheSecondPageKeepsTheHundredTasksTheFirstOneConfirmed` con
   `expected: <100> but was: <0>`. Ésa es exactamente la regresión que `@s26`
   existe para impedir.

Producción restaurada; verdes `ImportGitlabIssuesTest` (11) e
`ImportGithubIssuesTest`. Commit `1da4d2e`.

### `@s16`, `@s22` y `@s21` — cerrar lo heredado con el doble de GitLab

Los tres descansaban en `ImportIssues` o en `ExternalIssue`, compartidos con la
feature 27, y por eso los tres pasaron a la primera. En los tres el rojo se
acreditó rompiendo la producción, y en los tres la mutación se eligió para que
matara filas distintas de la tabla:

- `@s16`: quitar `if (!listed.full()) break;` mata las filas de 0 y 37 issues
  (piden una segunda página que nadie anunció); `MAX_PAGES = 1` mata las de
  100+40 y 100+100 (created 100 en vez de 140 y 200). Ninguna mutación mata las
  cuatro, que es la señal de que la tabla no es decorativa.
- `@s22`: comprobar el proyecto antes que la conexión mata las dos pruebas de
  orden; quitar la guarda `connection.valid()` y la de proyecto completado mata
  otras tres. Las filas de proyecto inexistente y ajeno las sostiene un
  `orElseThrow` que no se puede mutar sin dejar de compilar, y así queda dicho.
- `@s21`: `taskCompletionCriterion` devolviendo sólo la url mata la del cuerpo
  tras la línea en blanco; `trim` devolviendo el texto crudo mata la del título
  en blanco. **La del recorte de espacios sobrevive**, porque `Task.create`
  también recorta: el comportamiento está guardado dos veces y esa prueba sola
  no distingue quién lo hizo. Anotado aquí para que la campaña de mutación no
  lo descubra como sorpresa.

Commits `c27a166`, `afb3a6e`, `b09e01a`.

### El bloqueo del catálogo, revisado (la premisa había caducado a medias)

La bitácora decía que la mitad B estaba bloqueada «hasta que 25, 26, 28 y 30
estén integradas». **Eso ya no es cierto** y conviene no repetirlo: sus puertos
están en `main` y se pueden usar hoy mismo desde
`backend/src/main/java/com/apptolast/organization/application/`:
`ApiCredentialQueries.java` (24), `WebhookDeliveries.java` y
`EnqueueWebhookDeliveries.java` (25), `CalendarFeedTokens.java` y
`CalendarFeedStatus.java` (26), `ExternalCalendarStore.java` (28), y para 29 el
propio `GitlabConnectionStore` con `IssueImportReceiptStore`.

Lo que bloquea de verdad a `@s2`, `@s4`, `@s5` y `@s31` es más simple y más
caro: **el endpoint del catálogo no existe**. La evidencia, exacta:

- No hay ningún caso de uso de catálogo en `application/`: el único fichero que
  responde a `grep -i catalog` es `ZoneCatalog.java`, que no tiene relación.
- No hay ninguna ruta `/api/v1/me/connectors` a secas. Las dos únicas
  declaraciones son de subrecursos:
  `adapter/http/GithubConnectorController.java:35` y
  `adapter/http/GitlabConnectorController.java:38`, ambas con el sufijo del
  gestor.

Es decir: no es una espera, es trabajo por hacer. Y no es pequeño, porque `@s2`
son doce filas y cada una deriva su estado de una fuente distinta, con las
reglas de `@s3` (sin `APP_CONNECTOR_KEY`, las que cifran salen `disabled` sin
intentar descifrar) y `@s6` (leer el catálogo no llama a terceros ni escribe)
encima. Por eso no se ha empezado en los minutos que quedaban: a medias vale
cero. Quien lo retome tiene arriba la lista de puertos y aquí la lista de lo
que falta.

### El catálogo: caso de uso hecho, frontera y fuentes pendientes

Commit `8c9ae36`. Cuatro tipos en `application/`, sin infraestructura:

- `ConnectorRow(id, status, lastActivityAt, lastError)` con `notConnected(id)` y
  `disabled(id)`.
- `ConnectorStatusSource`: `id()`, `encryptsSecrets()`, `read(ownerId)`. Cada
  conector deriva su fila de su propia fuente; el catálogo no sabe leer
  webhooks ni calendarios, sólo pedir en orden.
- `ConnectorCatalog(List<ConnectorRow>)` con `row(id)`.
- `ReadConnectorCatalog(List<ConnectorStatusSource>, SecretCipher)`.

Ocho pruebas en `ReadConnectorCatalogTest`, rojo acreditado porque ninguno de
los cuatro tipos existía y no compilaban. Cubren `@s1` (las seis filas y su
orden), `@s2` a nivel de caso de uso (cada fila muestra lo que su fuente
contestó), `@s4` (a toda fuente se le pregunta por el propietario autenticado y
por nadie más), `@s3` en sus dos mitades, `@s6` (dos lecturas seguidas dan lo
mismo) y `@s7`.

Dos decisiones que quien siga debe respetar o discutir a conciencia:

1. **Sin clave no se pregunta.** A las fuentes que cifran no se les llama
   siquiera: se devuelve `disabled`. Evita que un descifrado fallido tumbe el
   catálogo entero. La prueba lo afirma sobre la lista de a quién se preguntó,
   no sólo sobre la respuesta, que es lo que la hace capaz de fallar.
2. **`StorageUnavailableException` se deja propagar.** Envolverla daría el
   catálogo optimista que `@s7` prohíbe. La traducción a 503
   `STORAGE_UNAVAILABLE` es cosa de la frontera.

**Lo que falta**, en el orden en que lo haría:

1. Las **seis implementaciones** de `ConnectorStatusSource`, una por conector,
   con los puertos ya citados arriba. Son las que cierran las doce filas de
   `@s2` de verdad, y cada una necesita su prueba con la fuente real. Es el
   grueso: seis adaptadores, no uno.
2. El **controlador** `GET /api/v1/me/connectors`, con `Cache-Control: no-store`
   y el objeto de una sola clave `connectors` (`@s1`), la traducción de
   `StorageUnavailableException` a 503 (`@s7`), y sesión sin `Bearer` (`@s31`).
3. El **cableado** en `ConnectorConfiguration`, que fija el orden de la lista de
   fuentes. Ojo: el orden del catálogo lo impone hoy el orden de inyección; si
   se prefiere que lo imponga el caso de uso, hay que cambiarlo con una prueba
   que lo exija, no de tapadillo.

Hasta que 1 y 2 estén, `@s1`, `@s3`, `@s6` y `@s7` tienen oráculo de caso de uso
pero **no endpoint**, así que no se cuentan como cerrados, y `@s2`, `@s4`, `@s5`
y `@s31` siguen parciales.

## Estado al cerrar la sesión del carril

- El árbol **compila** (`compileJava` + `compileTestJava`, sin contenedores).
- Nada queda sin commitear.
- No se ha tocado ningún fichero compartido de los que lista `REGLAS.md` §6:
  todo el cambio vive en `backend/src/**` de GitLab, en `ImportIssues`
  (compartido con 27, pero restaurado a su forma original) y en este `progress/`.
- `feature_list.json` sigue en `spec_ready`, como debe: quedan 14 escenarios
  sin oráculo propio, el catálogo sin escribir y la pantalla entera sin escribir.
- `ImportIssues`, `ExternalIssue` y `PersonalAccessToken` se mutaron para
  acreditar rojos y se restauraron byte a byte; `git diff` contra los tres queda
  vacío. **Ninguna línea de producción se ha modificado en esta sesión salvo el
  ciclo de `@s15`**: todo lo demás son oráculos nuevos sobre producción que ya
  estaba escrita.

## URGENTE para el carril 27 — la causa de la regresión de `a347936`, localizada

`frontend/src/github-connector-client.ts:9`

```ts
const RECEIPT_FIELDS =
  "id projectId repository status created skipped failed truncated errorCode startedAt finishedAt";
```

Son **once** claves y siguen nombrando `repository`. El recibo que hoy devuelve
`GithubConnectorController.ImportResponse` (líneas 318-330) tiene **doce**:
`id source projectId projectPath status created skipped failed truncated
errorCode startedAt finishedAt`. Es el cambio de la decisión 2 de esta bitácora,
el que exige `@s20`.

`exact(value, RECEIPT_FIELDS)` (`schedule-block-api.ts:341`) compara
`Object.keys(value).length === keys.split(" ").length`: 12 ≠ 11, devuelve
`false`, y `decodeReceipt` lanza `Error("Confirmación incompatible")`. Por eso
la vista dejó de mostrar contadores: no es que vengan a cero, es que el recibo
**no se llega a decodificar**.

Y hay un segundo golpe por el mismo sitio: `CONNECTION_FIELDS` incluye
`lastImport`, que `decodeConnection` pasa por `decodeReceipt`. Así que también
revienta `GET /api/v1/me/connectors/github` en cuanto el propietario tiene una
importación previa, no sólo el POST. Eso explica que caigan cuatro pruebas de
E2E y no una.

Arreglo (lo hace 27, que es el dueño del fichero; este carril no lo toca):
poner las doce claves en `RECEIPT_FIELDS` y sustituir la comprobación
`typeof value.repository !== "string"` por `value.source === "github"` y
`projectPath` como cadena no vacía.

---

# Sesión de la noche del 9 al 10 de septiembre de 2026

Ocho ciclos, ocho commits, cada uno con su rojo acreditado. `24 → 35` de 38.

## Enmienda de contrato — `@s31`, las dos filas de credencial Bearer

`REGLAS.md` §8 exige razonarlo aquí. Las filas decían 401 `UNAUTHENTICATED`
para una credencial Bearer válida de 24 sobre `GET /api/v1/me/connectors` y
sobre `POST …/gitlab/imports`. Se cambian a **403 `API_SCOPE_DENIED`**.

El motivo no es de conveniencia: la línea 5 de este `.feature` dice que «donde
27 fija algo distinto, 27 prevalece», y el `@s31` de
`features/github_connector.feature` (líneas 396-403) ya se enmendó el 9 de
septiembre **con el propietario delante**, con este razonamiento: una credencial
Bearer válida **sí** está autenticada; el filtro de 24 la identifica y sólo
después mira su lista de rutas permitidas, que no incluye ni el catálogo ni el
conector. Responder «no sé quién eres» a quien se ha identificado es falso.
La propiedad de seguridad no cambia: la credencial no abre nada, no escribe nada
y no contacta con el servidor falso, y así lo afirma
`ConnectorCatalogApiTest:s31_avalidBearerCredentialIsIdentifiedButDeniedByScope`.

## Ciclo 1 — `@s2`: las seis fuentes de estado (commit `002e9c9`)

Una implementación de `ConnectorStatusSource` por conector, sobre los puertos
que ya estaban en `main`. Decisiones que quien siga debe respetar o discutir:

- **`api_credentials`**: `lastActivityAt` es **siempre nulo**. La feature dice
  «el último uso registrado por 24 o null si no lo hay», y 24 **no registra el
  uso**: no hay columna ni puerto. Publicar la fecha de alta como si fuera
  actividad sería inventarla. Hay prueba que lo fija.
- **`api_credentials`**: se recorren las páginas hasta encontrar una credencial
  vigente. Mirar sólo la primera haría que un propietario con muchas revocadas
  apareciera desconectado por el tamaño de la página, no por su estado.
- **`webhooks`**: un solo endpoint activo basta para `connected`. Apagar
  endpoints **a mano** no es un error, así que sin ninguno activo y sin
  agotamientos la fila vuelve a `not_connected`; el escenario sólo fija la fila
  del agotamiento y ésta es la lectura conservadora.
- **`github`**: su fila no guarda código ni instante de error. Lo único que
  puede estar mal es que el token deje de valer, y eso lo dice su estado
  `invalid`; el instante es el de la última importación —cuando se descubrió— y
  sin ninguna, el del alta.
- **`gitlab`**: **no vuelve a derivar nada**. Reutiliza entera
  `ReadGitlabConnectionUseCase` y se queda con tres de sus ocho campos. Así el
  catálogo y la pantalla de detalle no pueden discrepar, que es justo lo que
  exigen la última línea de `@s14` y la cuarta de `@s23`. Los cinco campos que
  descarta son exactamente los que `@s5` prohíbe.

**Rojo acreditado dos veces.** Primero por compilación: 34 errores, ninguna de
las seis clases existía. Después por mutación, en dos tandas elegidas para que
mataran conjuntos disjuntos y la atribución fuera inequívoca:

| tanda | mutación | pruebas muertas |
|---|---|---|
| A | `vigent` ignora la caducidad | `s2_anexpiredCredentialIsNotVigentEither` |
| A | `lastDeliveryAt` usa `min` en vez de `max` | las dos de la última entrega |
| A | GitHub siempre `connected` | `s2_aninvalidGithubConnectionIsAnErrorRow…` |
| B | una sola página de credenciales | `s2_thecredentialsAreReadPageByPage…` |
| B | se cae la rama del agotamiento | `s2_everyEndpointDisabledByExhaustion…` |
| B | el calendario nunca es `error` | `s2_afailedSubscriptionPublishesItsFeedErrorCode…` |
| B | la fila de GitLab pierde su `lastError` | `s2_agitlabInErrorPublishesTheSameLastError…` |

Cuatro y cuatro, exactamente las previstas.

## Ciclo 2 — `@s1` `@s3` `@s6` `@s7`: el endpoint y el cableado (commit `3a0d084`)

`ConnectorCatalogController`, once pruebas en `ConnectorCatalogApiTest`, y el
bean en `ConnectorConfiguration` con tres pruebas más en
`ConnectorCatalogWiringTest`.

Dos decisiones:

1. **`StorageUnavailableException` no se traduce en el controlador.** Sube a
   `ApiErrors`, que ya la convierte en 503 `STORAGE_UNAVAILABLE`. Envolverla
   aquí daría el catálogo optimista que `@s7` prohíbe.
2. **El orden se escribe en el cableado, entero y a la vista.** La bitácora
   anterior avisaba de que hoy lo imponía el orden de inyección; ya no. Se
   construye una `List.of(...)` explícita en `readConnectorCatalog`, porque
   inyectar `List<ConnectorStatusSource>` lo dejaría en manos del orden de
   declaración de los beans, que nadie lee al añadir uno.
   `ConnectorCatalogWiringTest` compara la lista **entera**, no posiciones
   (`REPARTO_NOCHE.md` §2).

`ConnectorCatalogWiringTest:s3_…` merece nota: construye el catálogo con la
lectura **real** de GitLab y un cifrador sin clave, y afirma
`verifyNoInteractions(gitlabConnections)`. Es decir: sin clave, la fuente que
cifra no se llega a preguntar, y por eso el catálogo entero no se cae.

## Ciclo 3 — `@s33`: el catálogo en pantalla (commit `a66d517`)

`connectors-catalog-client.ts` (12 pruebas) y `connectors-catalog.tsx` (13).

El cliente **se niega a creer** lo que no sea el contrato: seis filas, en su
orden, cuatro campos exactos, estado dentro de los cuatro, y un `lastError` de
exactamente `{code, at}`. Un `detail` con el texto del proveedor es
«Confirmación incompatible». Eso es `@s5` defendido en la orilla del navegador,
no sólo en la del servidor.

Una decisión de diseño que hay que respetar: **`CONNECTOR_ROUTES` admite
`null`** y se inyecta como propiedad. El escenario exige la rama «si no está
desplegada, muestra "No disponible" sin enlace», y sin la inyección esa rama no
tendría oráculo: en producción las seis rutas existen. No es un gancho para las
pruebas, es la única forma de que la rama sea observable.

Rojo por módulo inexistente y luego cinco mutantes, cinco pruebas muertas: un
glifo único por estado, el código traducido, la rama sin enlace y las dos de
`@s7`.

## Ciclo 4 — `@s34`: el formulario (commits `0445101` y `0cfc73f`)

Cliente (17 pruebas) y pantalla (11).

**Un mutante sobrevivió y quedó anotado**: quitar `setToken("")` tras conectar
no mataba nada, porque al conectar el formulario se desmonta y el campo
desaparece con él. Se resolvió en el ciclo 5, y no con una prueba de adorno.

## Ciclo 5 — `@s35`: la pantalla con conexión (commit `2f3a115`)

Diez pruebas rojas antes de escribir nada; después cinco mutantes que se
llevaron seis.

**Hallazgo 1 — dos mutantes equivalentes, y cómo se quitaron de en medio.**
Había `setToken("")` en `submitConnection` y otro en `replaceToken`. Cada uno
tapaba al otro: quitar cualquiera de los dos por separado dejaba todas las
pruebas verdes, y sólo quitar los dos era observable. No es que faltara una
prueba: es que la mecánica estaba duplicada. El token deja de vivir en el estado
de React y pasa a vivir **sólo en la propiedad `value` del campo**, que
desaparece cuando el formulario se oculta. Una sola mecánica, ninguna línea
inmatable, y `@s32` sale ganando: el token ya no aparece en el HTML serializado
de ningún elemento, cosa que antes sí pasaba porque React escribe el atributo
`value` de los campos controlados. La prueba de `@s32` se endureció para
afirmarlo.

**Hallazgo 2 — la prueba de contrato que faltaba.** Es la que el orquestador
pidió a raíz de la regresión de `a347936`. `gitlab-connector-client.test.ts`
**lee `GitlabConnectorController.java`**, extrae los componentes de sus dos
`record` y los compara, en orden, con las claves que el cliente decodifica.
Rojo acreditado con la regresión real: renombrando `source` a `repository` en
el `record ImportResponse`, la prueba cae. Es exactamente el cambio que costó
una hora al carril 27 y que ninguna prueba ataba.

## Ciclo 6 — `@s36`: los errores con su acción (commit `85ea51e`)

Cinco pruebas, una por fila; la sexta fila («sin `APP_CONNECTOR_KEY`, sin
formulario ni botones») ya la cerraba la prueba de `@s29`.

**Dos de las seis filas ya estaban satisfechas** por la producción de `@s34`
(los 30 segundos nombrados y el proveedor caído con la ruta conservada) y se
anota, porque un test que pasa a la primera no acredita nada por sí solo. Las
otras tres fueron rojas, y cuatro mutantes las volvieron a matar.

Decisión: «Actualizar estado» **relee** la conexión y el catálogo y **no
reintenta** la acción que falló. Un reintento automático sobre una importación
en curso es exactamente lo que la agravaría.

## Ciclo 7 — `@s37`: salir, cerrar sesión, respuestas tardías (commit `4144e32`)

**Las cuatro pruebas pasaron a la primera**, y eso no vale
(`REPARTO_NOCHE.md` §5). Al intentar acreditar el rojo se vio por qué: afirmaban
sobre lo que se ve **después de desmontar**, y tras desmontar no se ve nada
hagas lo que hagas —React ignora un `setState` sobre un árbol muerto—. Eran
verdes por construcción.

Se rehicieron para afirmar sobre el **`AbortSignal` que viajó en la petición**:
al salir, la petición en vuelo queda `aborted`. Con eso sí discriminan:

- quitar `pending.current?.abort()` del cleanup mata tres;
- quitar `key={owner}` de `GitlabConnector` mata la del cierre de sesión, que
  además pasó a usar `rerender` sobre la misma raíz, que es donde el cambio de
  propietario decide de verdad.

## Ciclo 8 — `@s38`, la parte que se puede medir hoy (commit `a64f372`)

Tres pruebas para la tercera línea: al conectar, al llegar el recibo y al
desconectar, el foco queda en el `h1` o en el aviso de resultado, nunca perdido
en el `body`. Rojas antes de escribir nada; quitar las dos banderas de foco las
vuelve a matar.

**Lo que no se ha hecho, y por qué**: las otras cuatro líneas (320/768/1280 px,
texto al 200 %, zoom nativo al 200 %, 44 × 44, recorrido de teclado y axe) son
de navegador y necesitan las dos rutas cableadas. Escribir una spec de
Playwright que no se puede ejecutar habría dejado un fichero verde por no
llegar a correr —justo lo que `REPARTO_NOCHE.md` §5 cuenta que ya pasó una vez—.
Se deja sin escribir, y `@s38` se cuenta como **parcial**, no como cerrado.

De paso salieron dos cosas al pasar `eslint`:

- El heredoc se había comido los escapes de la regex de la prueba de contrato
  (`REGLAS.md` §7 avisaba de esto). La prueba **pasaba por suerte**: sin
  escapar, el paréntesis abría un grupo y el último token antes de la primera
  coma seguía siendo `id`. Corregida con `Edit`.
- La carga inicial pasa a la forma que exige `react-hooks/set-state-in-effect`.

## Lo que le pido al orquestador

### 1. Cablear las dos rutas (ficheros suyos, `REPARTO_NOCHE.md` §3)

No lo he tocado. Es el cambio mínimo:

- `frontend/src/App.tsx`: `const connectorsCatalog = route === "/conectores";`
  y `const gitlabConnector = route === "/conectores/gitlab";`, y en el árbol
  `<ConnectorsCatalog />` y `<GitlabConnector owner={username} />`. Los imports
  son `./connectors-catalog` y `./gitlab-connector`.
- `frontend/src/workspace.tsx`: entrada «Conectores» a `/conectores`,
  **después de «Importación»** y sin mover «Hoy» (lo dice `@s33`), y
  `"Conectores"` en la unión de `section`.

Hasta que eso esté, `@s38` no se puede medir y `@s33` no se puede abrir en un
navegador, aunque su lógica esté probada.

### 2. La puerta de mutación de frontend

Ficheros de producción a incluir en `stryker.additional-connectors.config.json`:

```
frontend/src/connectors-catalog-client.ts
frontend/src/connectors-catalog.tsx
frontend/src/gitlab-connector-client.ts
frontend/src/gitlab-connector.tsx
```

Sus pruebas son los cuatro `*.test.ts` / `*.test.tsx` con esos mismos nombres:
**77 pruebas** en total (12 + 13 + 19 + 33).

### 3. Clases nuevas de backend para el ámbito PIT

El ámbito que montaste tiene el patrón `ConnectorStatusSource*`, que sólo
resuelve a la **interfaz** —que no tiene mutantes—. Las seis implementaciones
no empiezan por `ConnectorStatusSource`, así que **quedan fuera**. Faltan:

```
ApiCredentialStatusSource*
WebhookStatusSource*
IcsCalendarStatusSource*
GithubStatusSource*
ExternalCalendarStatusSource*
GitlabStatusSource*
ConnectorRow*
ConnectorCatalog*
ReadConnectorCatalog*
ConnectorCatalogController*
```

Las seis primeras son donde está la lógica de verdad: las seis derivaciones de
estado, con 21 pruebas encima.

### Qué mutantes espero que sobrevivan

Para que la campaña se pueda contrastar con la previsión, como pide
`REPARTO_NOCHE.md` §4:

- `ConnectorRow.disabled` y `ConnectorRow.notConnected` construyen registros con
  nulos: mutar un nulo a otro nulo no cambia nada.
- `ConnectorCatalog.row` lanza `IllegalArgumentException` para un identificador
  desconocido; ninguna prueba lo pide, porque el caso de uso sólo pregunta por
  los seis. Es una guarda, no comportamiento.
- En `ApiCredentialStatusSource`, el límite de la caducidad
  (`isBefore` frente a `!isAfter`) **no** está guardado en el instante exacto:
  la prueba usa `NOW.minusSeconds(1)`. Si la campaña lo mata, mejor; si
  sobrevive, es un mutante de frontera conocido y no una sorpresa.
- El recorte de espacios de `@s21` sigue guardado dos veces (ya venía anotado de
  la sesión anterior).

## Estado al cerrar

- Backend: verde por clase en `ConnectorStatusSourcesTest`,
  `ReadConnectorCatalogTest`, `ConnectorCatalogApiTest` y
  `ConnectorCatalogWiringTest`. No se ha ejecutado la suite completa.
- Frontend: 77/77 verdes en los cuatro ficheros del carril; `tsc --noEmit`,
  `eslint` y `prettier` limpios sobre ellos.
- **Ninguna migración**: la V35 y la V36 siguen libres. El catálogo no añade
  tabla ni columna, sólo lee.
- Ficheros compartidos: **ninguno tocado**. `App.tsx`, `workspace.tsx`,
  `scripts/project.mjs`, `harness.config.json`, `feature_list.json`,
  `docker-compose.yml` y `backend/build.gradle.kts` quedan como estaban.
  `frontend/src/github-connector*` y `e2e/github-connector*` no se han tocado.
- El árbol está commiteado entero.
