# TDD del conector de GitHub (feature 27)

Rama `claude/github-connector` (worktree `C:/Users/vhurt/ow-worktrees/github-connector`), partiendo de
`codex/integration-api`. Contrato: `features/github_connector.feature`, 42 escenarios @s1–@s42.
Puerto E2E reservado: 18093.

## Estado

Feature en curso: 27 — `github_connector`. Escenarios a recorrer: @s1…@s42.
No se marca `done`: falta `judge` y `mutation_tester`.

## Decisión de diseño para la feature 29

El listado de issues sale por el puerto `IssueSource` (`application/IssueSource.java`), que devuelve
`ExternalIssue` de dominio y `IssuePage`. GitLab (feature 29) implementa el mismo puerto sin tocar el
caso de uso: el caso de uso no conoce GitHub, sólo `IssueSource`, `ExternalIssue` y los códigos de
error de `IssueSourceException`.

## Ciclos rojo → verde → refactor

### Ciclo 1 — @s5 `owner/repo` recortado y validado

- ROJO `GithubRepositoryTest`: recorte Unicode White_Space, expresión del propietario (1–39, sin
  guion inicial ni final), del nombre (1–100), `REQUIRED` cuando falta.
- VERDE `domain/GithubRepository`: record con `Pattern` y `parse` que recorta.
- REFACTOR: la aserción del caso inválido comprueba `field` y `code` en lugar de reinyectar el
  mensaje real en el esperado (era una tautología).

### Ciclo 2 — @s6 el PAT nunca se imprime

- ROJO `PersonalAccessTokenTest`: 1–255 ASCII imprimibles sin espacios, `REQUIRED`, `TOO_LONG`,
  `INVALID_FORMAT`, y `toString` sin el valor.
- VERDE `domain/PersonalAccessToken`.
- REFACTOR: misma corrección de tautología en la aserción.

### Ciclo 3 — @s14 título de la issue recortado a 160 puntos de código

- ROJO `ExternalIssueTest` (no compilaba: `ExternalIssue` no existía).
- VERDE `domain/ExternalIssue.taskTitle()`: recorte Unicode y, si supera 160 puntos de código,
  159 puntos más `…`. Se cuenta por puntos de código, así que `🚀` no se parte.
