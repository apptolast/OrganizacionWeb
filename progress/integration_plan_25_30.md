# Plan de integración de los carriles 25–30 y del modo oscuro

Escrito el 9 de septiembre de 2026 por el coordinador, tras probar en seco la fusión de los siete carriles contra `main` en un worktree desechable. Sustituye a cualquier suposición previa sobre conflictos.

## Punto de partida

`main` recibió la feature 24 aplastada en un solo commit (`0277c50`, squash de la PR 29). Los carriles 25–30 nacieron de `codex/integration-api`, cuyos commits originales no están en `main`. La base común de cualquier fusión es por tanto `4c7d558`, anterior a las dos historias.

Eso hacía temer una fusión sucia. No lo es: el contenido de la feature 24 es idéntico byte a byte en ambos lados, así que Git resuelve solo esas rutas. La prueba en seco del 9 de septiembre da este resultado.

| Carril | Archivos que aporta | Conflictos |
| --- | --- | --- |
| `claude/webhooks` | 24 | `feature_list.json`, `progress/current.md` |
| `claude/ics-calendar` | 7 | `feature_list.json`, `progress/current.md` |
| `claude/github-connector` | 2 | `feature_list.json`, `progress/current.md` |
| `claude/external-calendar` | 13 | `feature_list.json`, `progress/current.md` |
| `claude/automations` | 16 | `feature_list.json`, `progress/current.md`, `progress/gherkin_automations.md` |
| `claude/darkmode` | 8 | ninguno |
| `claude/integration-api-closure` | según avance | ninguno |

Las cifras son del momento de la prueba y crecerán conforme avancen los carriles. Lo que importa es que ningún conflicto cae sobre código de producto: son los tres archivos de contabilidad que el coordinador edita en `main`.

## Regla de resolución

1. `progress/current.md`: siempre la versión de `main` (`git checkout --ours`). El estado de sesión lo lleva el coordinador; la copia del carril está congelada en el momento de su creación.
2. `feature_list.json`: partir de la versión de `main` y cambiar únicamente el estado de la feature que se integra. Las demás filas las gobierna `main`.
3. `progress/gherkin_<name>.md`: quedarse con la versión de `main` cuando el conflicto sea la sección «Decisiones del coordinador», añadida después de sembrar el carril.

## Orden de integración

`24` (hecha) → `darkmode` → `25` → `26` → `27` → `28` → `30` → `29`.

El modo oscuro va primero porque no tiene conflictos y corrige un defecto visible en producción. La 29 va última porque reutiliza el puerto de listado de issues que construye la 27, y su contrato depende además de que existan 25, 26 y 28 para poblar el catálogo de conectores.

## Puerta por carril, antes de fusionar

1. Bitácora `progress/tdd_<name>.md` con el mapa completo `@s → test`.
2. Suites verdes en el propio carril: backend, frontend, comprobación de tipos, pruebas del arnés y su E2E.
3. `judge` con dictamen APPROVED en `progress/judge_<name>.md`.
4. `mutation_tester` por encima de 0,8 estricto en `progress/mutation_<name>.md`, con originales conservados.
5. Fusión a `main`, CI verde, y solo entonces `status: "done"` en `feature_list.json`.

No se declara `done` por tener la suite verde. El despliegue productivo es una puerta distinta y sigue bloqueada por acceso al servidor.

## Worktrees

Viven en `C:/Users/vhurt/ow-worktrees/`, fuera de OneDrive para que su sincronización no interfiera. `_mergetest` es desechable y solo sirve para ensayar fusiones. Al terminar cada carril, `git worktree remove` y borrado de la rama una vez fusionada.
