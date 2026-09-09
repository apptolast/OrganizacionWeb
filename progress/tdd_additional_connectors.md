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

## Mapa `@s` → prueba

(se rellena al cerrar cada ciclo)

## Bitácora de ciclos

(se rellena al cerrar cada ciclo)
