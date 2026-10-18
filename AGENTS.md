# AGENTS.md — instrucciones para agentes de IA

> Punto de entrada para cualquier agente (Claude Code, Codex, Copilot, OpenCode, Gemini). Corto a propósito: lo detallado vive en `docs/`, léelo solo cuando lo necesites. Las cinco secciones son del proyecto; el bloque final «Arnés SSD» lo gestiona la plantilla (`scripts/harness-upgrade.mjs`) y no se edita aquí.

## 1. Resumen del proyecto

**OrganizationWeb**: organización personal por proyectos y sesiones de trabajo acotadas (decidir qué hacer, reservar tiempo y conservar un historial del avance). Monorepo: web React + TypeScript + SCSS (pnpm) y API Java + Spring Boot hexagonal (Gradle Kotlin DSL) con PostgreSQL y publicación de eventos por RabbitMQ (outbox). Estado y alcance del producto: `README.md`.

## 2. Ficheros clave

| Ruta | Qué contiene |
| --- | --- |
| `harness.config.json` | Los comandos del arnés; todos delegan en `scripts/project.mjs`, que coordina backend y frontend |
| `backend/` · `frontend/` | API (Gradle) y web (pnpm), cada una con sus fuentes, tests y build |
| `e2e/` · `playwright.config.mjs` | Recorridos de la web contra la API y PostgreSQL reales |
| `deploy/` · `docker-compose.yml` | Proxy, contenedor de la web y ejecución local |
| `scripts/` | `project.mjs` (install, lint, test, build, mutate) y los humos de e2e, publicador y DNS |
| `features/<capacidad>.feature` | Spec viva de cada capacidad (Given/When/Then); el PR que cambia la capacidad actualiza su `.feature` |
| `docs/` | `architecture.md`, `conventions.md`, `ux-requirements.md`, `agent-efficiency.md` y la doc del arnés (`tdd`, `verification`, `modelos`…) |
| `.github/workflows/` | `harness-ci.yml` («Application CI», en cada PR y push a `main`), `pr-body.yml` («Plantilla del PR», comprueba la descripción), `publish-images.yml` (publica las imágenes `:latest` tras la CI verde de `main`) y `harness-mutation.yml` (campaña de mutación, solo manual) |
| `feature_list.json` · `progress/` · `project-spec.md` | **Histórico** del pipeline anterior: el arnés ya no los usa ni los exige y no se actualizan; el seguimiento es el issue y el PR |
| `.claude/agents/` · `.github/agents/` · `.codex/agents/` · `.opencode/agents/` | Agentes de nivel (`worker_fast`, `worker_balanced`, `reviewer_frontier`) y especialistas de solo lectura; qué modelo es cada nivel: `docs/modelos.md` |

## 3. Desarrollo y validación

**Cómo se ejecuta.** Empieza siempre por el entorno: `node .harness/harness.mjs init` (en Windows también `bin\harness.ps1 init`). Hace lint y corre **toda** la suite, que levanta PostgreSQL en contenedores: necesita Docker y tarda. Si falla, **para** y resuelve el entorno antes de tocar código. Los comandos de tu stack viven en `harness.config.json` y se invocan a través del arnés.

| Comando | Para qué |
| --- | --- |
| `node .harness/harness.mjs init` | Verifica el entorno y el arnés: ficheros base, lint y toda la suite |
| `node scripts/project.mjs install` | `pnpm install --frozen-lockfile` en la raíz y en `frontend/` |
| `node scripts/project.mjs lint` · `test` · `build` | Sintaxis de los scripts y `spotlessCheck` + ESLint/Prettier · suites de scripts, backend y frontend · `bootJar` y build de la web |
| `pnpm test:e2e` · `pnpm test:e2e:browser` | Recorridos e2e (`scripts/e2e.mjs`) y Playwright |
| `pnpm test:publisher` · `pnpm test:web-dns` | Humos del publicador y de la recuperación DNS de la web (con Docker); los corre la CI |
| `node .harness/harness.mjs mutate <objetivo>` | Mutación (PIT en backend, Stryker en frontend); solo nivel crítico, sobre los módulos que fija el plan. Objetivos válidos: `scripts/project.mjs`. Sin objetivo corre la campaña completa (horas) |
| `node .harness/harness.mjs verify` | `init` + mutación completa; solo antes de una entrega (el cierre ordinario es `init`) |

**Al empezar una tarea.** Lee el issue (objetivo, criterios y plan) o lo que pida la persona, y la «Política de trabajo» del final de este fichero. Opcional y no bloqueante: sincroniza la memoria de la organización con `scripts/sync-memoria.sh` (`pwsh scripts/sync-memoria.ps1` en Windows) y revisa en `.memoria-cache/patterns/` los patrones de tu categoría antes de diseñar desde cero, respetando su «Cuándo NO aplica»; si falla, sigue sin ella y anótalo en el PR (`docs/memoria-organizacional.md`).

**Flujo.**
- Trabaja en una **rama nueva** desde `main` y usa **commits convencionales** (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`).
- Antes de pedir revisión, **revisa tu propio diff**: `/code-review` (Claude Code), `/review` (Codex y Copilot CLI) o el diff a `reviewer_frontier`.
- El PR sigue la plantilla de `.github/`; si el cambio es visual, adjunta imágenes o vídeo de antes y después.

## 4. Guardarraíles

- **No saltes la puerta humana:** plan aprobado antes de implementar y diff revisado en local antes de cualquier `git push`. La autorización global del 5 de septiembre de 2026 (`project-spec.md`) eximía de la puerta por feature del pipeline anterior, que ya no existe; no sustituye a estas dos.
- **Un test por criterio** del issue; TDD estricto (`docs/tdd.md`) y mutación solo en nivel crítico.
- **Un tema por tarea y por PR**, pequeño y fácil de revisar. **Una tarea, una sesión:** el traspaso es el issue y el PR, no el chat; si lo que te piden se sale del objetivo, avisa y propón una sesión nueva.
- **Interfaces:** antes de diseñar, implementar o revisar una, lee `docs/ux-requirements.md` (matriz de los 30 principios de Laws of UX, criterios responsive y evidencia requerida). No declares cumplimiento global por pasar solo axe o unas pocas resoluciones.
- **Eficiencia:** aplica [Ponytail](.agents/skills/ponytail/SKILL.md) (full) y [Caveman](.agents/skills/caveman/SKILL.md) (lite), leyéndolos una vez por contexto, como indica `docs/agent-efficiency.md`; los requisitos explícitos y la calidad mandan.
- **Un test roto o inestable sin cambio de producto** (el defecto está en el propio oráculo: una consulta ambigua, una carrera, una aserción que supone algo del entorno) se arregla directamente, como nivel trivial (plan de 1-2 líneas aprobado, sin subagentes ni mutación). Dos condiciones: el diff **no** toca `src/` de producción y la reparación deja el oráculo **igual de exigente o más**. Nunca se arregla un test debilitándolo: ni `getAllBy*` para tapar una consulta ambigua, ni un `waitFor` que engulle la carrera, ni borrando el escenario (regla ratificada por el propietario el 10 de septiembre de 2026, a raíz del run #259 de CI). Cambio posterior a esa ratificación: el push ya no es inmediato, espera a la revisión local de la política.
- **Si no sabes algo, busca en `docs/`** antes de inventarlo.
- **Si te bloqueas** (una herramienta no hace lo que esperas), no inventes un workaround: documenta el bloqueo en el issue o en el PR y para.

## 5. Definición de hecho

Una tarea está hecha cuando se cumple todo esto:

- `node .harness/harness.mjs init` en verde; en nivel crítico, además, la mutación sobre los módulos del plan por encima de `mutation.threshold` (0,8).
- Cada criterio del issue tiene su test; si cambió una capacidad, su `features/<capacidad>.feature` está actualizado en el mismo diff.
- Si hay interfaz, la evidencia que pide `docs/ux-requirements.md` y los recorridos e2e relevantes en verde.
- El diff está revisado (por ti y, en local, por la persona), el PR rellena sus bloques con la casilla de revisión local marcada y `Application CI` pasa.
- El issue tiene las casillas del plan marcadas y «Resultados de la ejecución» rellena.
- El repo queda limpio: sin logs de debug, sin ficheros temporales, sin TODOs sin contexto.

<!-- harness:begin — bloque gestionado por la plantilla: no lo edites aquí; lo reemplaza scripts/harness-upgrade.mjs al aplicar -->
## Arnés SSD (gestionado por la plantilla)

- Entorno y arnés: `bin/harness init` · tests: `bin/harness test` · mutación: `bin/harness mutate [target]`.
- ¿Al día con la plantilla? `node scripts/harness-upgrade.mjs --check` (solo lectura); `--diff` enseña los cambios.
- Qué es de la plantilla y qué del proyecto: `harness.manifest.json`.

### Política de trabajo

**Niveles.** El planificador clasifica cada tarea por juicio (las pistas no son exhaustivas; ante la duda, el nivel superior) y escribe el nivel y su motivo en el plan; la persona lo corrige al aprobarlo.
- **trivial:** cambio local y evidente. Un solo contexto, sin subagentes obligatorios.
- **pequeño:** acotado a pocos ficheros y sin decisiones de diseño. Se planifica con `frontier` y se implementa con `balanced`, en la misma sesión.
- **estándar:** varias piezas o un comportamiento nuevo. Mismo mecanismo; el plan fija los artefactos que exige.
- **crítico:** seguridad, datos, dinero o un contrato público. El planificador sigue como orquestador y revisor: delega, puede reasignar y revisa. Exige además TDD estricto y mutación sobre los módulos que fija el plan (`docs/verification.md`); en los demás niveles no se corre mutación.

**Puerta humana (dos puntos, todos los niveles).** 1) La persona aprueba el plan antes de implementar (en trivial basta uno de 1-2 líneas con el nivel y el motivo). 2) La persona revisa el diff en local antes de cualquier `git push`; un agente interactivo no empuja sin esa revisión.

**Delegación.** Por capacidad, no por proveedor: `frontier` (planificar, revisar, lo crítico), `balanced` (implementar), `fast` (tareas mecánicas y exploración). Salvo en trivial, el plan lleva una tabla «Delegación»: ejecutor, nivel, esfuerzo, motivo, «escalar si…» y verificación automática. Qué modelo es cada nivel en cada herramienta: `docs/modelos.md`.

**Issues y PR.** Plantillas en `.github/`. Toda tarea tiene su issue (objetivo, nivel, criterios Given/When/Then, plan en casillas, tareas relacionadas, resultados); en trivial es mínimo: objetivo y plan de 1-2 líneas con el nivel y el motivo. El PR lo enlaza con `Closes #N`, lleva siempre cuatro bloques (qué, por qué, cómo se probó, pendiente), «Impacto en tareas anteriores» y la casilla de revisión local; el workflow «Plantilla del PR» comprueba esas secciones y la casilla (`node .harness/pr-body.mjs`).

**Bucle de aprendizaje (fase de cierre).** Una tarea nueva puede dejar obsoletas otras anteriores; el nivel trivial no ejecuta esta fase.
- Al planificar, anota en «Tareas relacionadas» del issue las anteriores que tocan lo mismo: `node .harness/impacto-tareas.mjs --files <rutas del plan>` da los candidatos.
- Al cerrar, `node .harness/impacto-tareas.mjs` sobre la rama lista, con tope fijo y sin LLM, los PR e issues previos que tocaron los mismos ficheros. Alimenta «Impacto en tareas anteriores»: cada candidato se marca «afecta» o «no afecta», con el motivo en una línea.
- Cada tarea afectada (su issue; si solo existe el PR, el PR) recibe la etiqueta `revisit` y un comentario con el motivo y el enlace al PR. Si el impacto exige cambiar algo, un agente en modo plan abre un issue `revisit` nuevo con el plan de enmienda ya redactado; se enlaza desde el comentario, desde «Resultados de la ejecución» y desde el bloque del PR, y la persona lo aprueba como cualquier plan. Comentar, etiquetar, crear la etiqueta si falta y abrir ese issue se hace con el OK de la persona; ningún issue se reabre por su cuenta.
- Lo aprendido que valga para tareas futuras va a `AGENTS.md` en el mismo PR. No hay bot retrospectivo.
- «Todo criterio de aceptación tiene un test» es la mitad determinista del bucle.
<!-- harness:end -->
