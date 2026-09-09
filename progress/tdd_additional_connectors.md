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
| `@s1` | catálogo de seis filas en orden fijo | abierto | — (sin producción: no existe caso de uso de catálogo; mitad B) |
| `@s2` | cada fila deriva su estado de su fuente | parcial | sólo las dos filas `gitlab`: `GitlabConnectionUseCasesTest:79,87,95`; las diez restantes, abierto (mitad B) |
| `@s3` | sin clave, las filas que cifran salen `disabled` | abierto | el equivalente por ruta de GitLab está en `@s29`; la fila del catálogo, no |
| `@s4` | el catálogo sólo ve al propietario autenticado | parcial | aislamiento de la fila GitLab en `GitlabConnectorPersistenceTest:168`; el catálogo, abierto |
| `@s5` | `lastError` sin secretos ni texto del proveedor | parcial | `GitlabConnectorApiTest:179` sobre `GET /connectors/gitlab`; sobre el catálogo, abierto |
| `@s6` | consultar el catálogo no sincroniza ni escribe | abierto | — (mitad B) |
| `@s7` | almacenamiento caído no da catálogo optimista | abierto | — (mitad B) |
| `@s8` | `not_connected` en vez de 404 | cerrado | `GitlabConnectionUseCasesTest:38,52,68`; `GitlabConnectorApiTest:132,157`; `GitlabConnectorWiringTest:48` |
| `@s9` | conectar valida el proyecto y cifra el token | cerrado | `GitlabConnectionUseCasesTest:105,127`; `HttpGitlabIssueSourceTest:57,75`; `GitlabConnectorPersistenceTest:138,154,162`; `GitlabConnectorApiTest:211`; `GitlabTokenTest:29` |
| `@s10` | cuerpos inválidos rechazados sin llamar a GitLab | cerrado | `GitlabProjectPathTest:17,32,37,43`; `GitlabTokenTest:17,24`; `GitlabConnectorApiTest:243,260` |
| `@s11` | ruta codificada y base de API sólo del servidor | cerrado | `GitlabApiBaseTest:24,45,52,59`; `GitlabConnectorWiringTest:26,41` |
| `@s12` | GitLab rechaza y no se guarda nada | cerrado | `GitlabConnectionUseCasesTest:172,184,194,207`; `HttpGitlabIssueSourceTest:90,103,111`; `GitlabConnectorApiTest:287,311` |
| `@s13` | sustituir token sube `version` y conserva enlaces | cerrado | `GitlabConnectionUseCasesTest:136`; `GitlabConnectorPersistenceTest:177` |
| `@s14` | desconectar idempotente conservando tareas | cerrado | `GitlabConnectionUseCasesTest:157`; `GitlabConnectorPersistenceTest:192`; `GitlabConnectorApiTest:449` |
| `@s15` | importar crea tareas enlazadas con recibo | cerrado | `ImportGitlabIssuesTest:71,255`; `HttpGitlabIssueSourceTest:151`; `GitlabConnectorApiTest:392`; `GitlabConnectorPersistenceTest:300`; y la última línea (`lastActivityAt` = `finishedAt`) en `GitlabConnectionUseCasesTest:79,87,95` |
| `@s16` | paginar por `X-Next-Page` y marcar `truncated` | parcial | el adaptador lee la cabecera y fija la consulta: `HttpGitlabIssueSourceTest:124,143`, `GitlabApiBaseTest:71`. El recuento por filas del Outline (0/37/140/200 y el tope de dos páginas) es heredado: `ImportGithubIssuesTest:138,158` |
| `@s17` | excluir incidentes, test cases, tasks y movidas | cerrado | `HttpGitlabIssueSourceTest:168`; `ImportGitlabIssuesTest:94` |
| `@s18` | repetir la importación es idempotente por enlace | cerrado | `ImportGitlabIssuesTest:108` |
| `@s19` | unicidad de enlaces por origen | cerrado | `ImportGitlabIssuesTest:130`; `GitlabConnectorPersistenceTest:224,264` |
| `@s20` | un solo caso de uso sirve a los dos gestores | cerrado | `ImportGitlabIssuesTest:144` |
| `@s21` | el mapeo issue → tarea es el de 27 | heredado | las cinco filas (recorte a 160/159 puntos de código, cuerpo a 2000, título en blanco como `failed`) sólo tienen oráculo con datos de GitHub: `ImportGithubIssuesTest:104,224`. **Deuda declarada**: falta la tabla equivalente con `web_url` de GitLab |
| `@s22` | precondiciones antes de contactar GitLab | parcial | dos filas cerradas en la frontera: `GitlabConnectorApiTest:327,345`. Las cinco restantes (proyecto inexistente, ajeno, `completed`, conexión en `error`, y el **orden** entre comprobaciones) son heredadas: `ImportGithubIssuesTest:340,351,362,433,441,449,457` |
| `@s23` | token rechazado marca la conexión y deja recibo | cerrado | `ImportGitlabIssuesTest:205`; `GitlabConnectorPersistenceTest:207`; `GitlabConnectorApiTest:359` |
| `@s24` | cuota con `Retry-After` | cerrado | `HttpGitlabIssueSourceTest:187,198`; `ImportGitlabIssuesTest:241`; `GitlabConnectorApiTest:311` |
| `@s25` | indisponible, sin seguir redirecciones | cerrado | `HttpGitlabIssueSourceTest:211,223,232`; `ImportGitlabIssuesTest:226` |
| `@s26` | fallo en la página 2 conserva lo confirmado | **cerrado en esta sesión** | `ImportGitlabIssuesTest:266,286` (ver bitácora) |
| `@s27` | una sola importación por propietario | cerrado | `GitlabConnectionUseCasesTest:219`; `GitlabConnectorPersistenceTest:273` |
| `@s28` | el `running` abandonado deja de bloquear a los 15 min | cerrado | `GitlabConnectionUseCasesTest:234,243`; `GitlabConnectorPersistenceTest:286` |
| `@s29` | sin clave, `CONNECTORS_DISABLED` sin tocar nada | cerrado | `GitlabConnectionUseCasesTest:253`; `GitlabConnectorApiTest:458`; `GitlabConnectorWiringTest:63` |
| `@s30` | el recibo ajeno equivale al inexistente | cerrado | `GitlabConnectorApiTest:421,431` |
| `@s31` | sesión, CSRF y origen en todas las rutas | parcial | las de GitLab: `GitlabConnectorApiTest:480,488,507,522`. Las dos filas de `GET /api/v1/me/connectors` dependen de la mitad B |
| `@s32` | el token nunca sale salvo en `PRIVATE-TOKEN` | parcial | cada ruta comprueba por su cuenta que no lo devuelve (`GitlabConnectorApiTest:211`, `HttpGitlabIssueSourceTest:57`, `GitlabConnectorPersistenceTest:138`). Falta el barrido único del escenario: logs de la aplicación en el mismo caso y `localStorage`/`sessionStorage`/cookies del navegador |
| `@s33` | pantalla `/conectores` | abierto | — (sin producción; mitad B) |
| `@s34` | `/conectores/gitlab` sin conexión | abierto | — (sin producción de frontend) |
| `@s35` | pantalla con conexión existente | abierto | — (sin producción de frontend) |
| `@s36` | errores recuperables con su acción | abierto | — (sin producción de frontend) |
| `@s37` | cancelación y cierre de sesión | abierto | — (sin producción de frontend) |
| `@s38` | responsive, texto ampliado, teclado y axe | abierto | — (sin producción de frontend) |

**Recuento: 20 escenarios cerrados de 38.** 6 parciales (`@s2`, `@s4`, `@s5`,
`@s16`, `@s22`, `@s31`), 2 heredados sin oráculo propio (`@s21`, y la parte de
`@s16`/`@s22` ya contada), 1 parcial de seguridad (`@s32`) y 9 abiertos sin
producción (`@s1`, `@s3`, `@s6`, `@s7`, `@s33`…`@s38`).

### Aviso sobre la numeración

`ConnectorAuditTest` lleva pruebas con prefijo `s34_`: son de la **feature 27**,
cuyo `@s34` es la auditoría del conector. No cubren el `@s34` de esta feature
(la pantalla `/conectores/gitlab`). Igual pasa con los prefijos de
`ImportGithubIssuesTest`: toda su numeración es la de 27. Al leer este mapa hay
que quedarse con la columna de la izquierda, no con el nombre del método.

### Lo que falta para poder cerrar la feature

Por orden de coste creciente:

1. `@s21` con datos de GitLab (aplicación, sin contenedor).
2. `@s32` como barrido único incluyendo logs (frontera HTTP).
3. `@s33`…`@s38`: la pantalla entera, que hoy no existe. Es la mitad del
   trabajo que queda.
4. `@s1`…`@s7` y `@s33`: el catálogo (mitad B), bloqueado hasta que 25, 26, 28
   y 30 estén integradas.

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
