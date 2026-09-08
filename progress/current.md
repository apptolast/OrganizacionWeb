# Estado actual — cierre de 24 y arranque paralelo de 25–30 (8 de septiembre de 2026, sesión nocturna)

Instrucción del usuario (8 de septiembre, 19:31): sincronizar con el remoto, completar todas las features restantes hoy con el máximo de agentes en paralelo, y verificar el modo oscuro. Esta instrucción explícita autoriza trabajar 25–30 en paralelo en worktrees aislados (una rama por feature) sin repetir puertas humanas; se conservan spec, Gherkin, TDD, revisión independiente y mutación por feature. La autorización global del 5 de septiembre sigue vigente.

## Hecho en esta sesión

- Local sincronizado con `origin/main` 4c7d558 (44 commits nuevos). Borradores locales antiguos del backend de replanificación apartados fuera del repo (ya estaban integrados por PR 5/6).
- Arnés Windows: `scripts/project.mjs` resuelve `gradlew.bat` con ruta absoluta porque `NoDefaultCurrentDirectoryInExePath=1` impide a cmd buscar en el directorio actual; `scripts/e2e.mjs` acepta `E2E_WEB_PORT` para levantar varias pilas E2E en paralelo. 70/70 tests del arnés verdes.
- Feature 24: único fallo de CI (34253701367, @s41 texto 200 % a 320 px, marca de 345 px) corregido en `codex/integration-api` con 01f80ab (`.brand` deja de ser `nowrap`). Verificación local: Vitest 2507/2507, tsc, E2E focal 5/5 y 10/10 de reflow. CI 34264153095 en curso. Pendiente: CI verde → marcar PR 29 lista → merge → despliegue y cierre 24.
- Briefs de arquitectura para implementadores: `progress/brief_backend.md`, `progress/brief_frontend.md`.
- Contratos 25–30: propuestas normativas en `progress/proposal_<name>.md`, integradas en `project-spec.md` (secciones «Feature 25»–«Feature 30»). Migraciones reservadas: V23 webhooks, V24 calendar_feed_tokens, V25 github_connector, V26 external_calendar, V27 additional_connectors, V28 automations. Gherkin en curso por seis `gherkin_author`.
- Modo oscuro: auditoría Playwright en curso (`progress/darkmode_audit.md`). Análisis estático previo: `frontend/src/today.scss` usa colores claros fijos sin variante oscura; `frontend/index.html` `theme-color` solo claro.
- CI de main 34235036356 (commit de docs) falló por un E2E de exportación con `Protocol error (Network.getResponseBody)`: fallo de infraestructura de Playwright, no de producto; se vigila si se repite.

## Plan de carriles (worktrees en C:/Users/vhurt/ow-worktrees, ramas `claude/<feature>` desde `codex/integration-api`)

- A: 25 webhooks → después 30 automations fase 2 (worker compartido y NOTIFY_WEBHOOK).
- B: 26 ics_calendar.
- C: 27 github_connector → después 29 additional_connectors (depende de 25–28 integradas).
- D: 28 external_calendar.
- E: 30 automations fase 1 (reglas, plantillas, simulación, auditoría, UI) en paralelo; fase 2 tras 25.

Cada carril: tdd_craftsman → judge → mutation_tester → integración en main por PR con CI. Orden de integración previsto: 24, 25, 26, 27, 28, 30, 29.

## Límites operativos

COMMON/V14 protegido. Puertos 8080/18080/18081 reservados; los E2E paralelos usan `E2E_WEB_PORT` 18090+. Credenciales fuera de Git/logs/chat. No se declara `done` sin juez y mutación sobre el umbral.
