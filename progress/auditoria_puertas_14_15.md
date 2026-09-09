# Auditoría: ¿pasaron las features 14 y 15 sus puertas? — 9 de septiembre de 2026

Un barrido por nombre de fichero sobre `progress/` no encontraba ni informe de
juez ni de mutación para las features 14 `start_work_session` y 15
`pause_resume_session`, ambas marcadas `done` en `feature_list.json`. La
disciplina del repo (`CLAUDE.md`) prohíbe cerrar sin juez aprobado y sin
mutación por encima de 0,80 (`harness.config.json:22`).

**Veredicto: falso positivo. Las dos tienen las dos puertas, con evidencia
commiteada en `main`.** La causa es de nomenclatura, no de evidencia perdida.

`CLAUDE.md:64` fija el patrón `progress/judge_<name>.md` y
`progress/mutation_<name>.md`. Los agentes añadieron sufijos `_final`,
`_backend` y `_frontend` porque cada feature tuvo campañas separadas por capa
—PIT en el backend, Stryker en el frontend— y, en el caso de la 14, un juez
final distinto de un juez de UX intermedio.

Las seis rutas están versionadas (`git ls-files` las devuelve todas): no son
ficheros sueltos del árbol de trabajo ni quedaron solo en una rama.

## Feature 14 — `start_work_session`

**Juez:** `progress/judge_start_work_final.md:3` — «APPROVED para cerrar
feature 14». La línea 23 añade el replay del residuo de mutación sin reabrir el
cierre.

**Mutación, dos campañas, ambas contabilizadas por el propio juez en su línea
13-14:**

- Backend PIT: `progress/mutation_start_work_backend.md:3` — 340/347 =
  **97,98 %**, veredicto PASS. El desglose del XML está en la línea 24: 340
  KILLED, 5 SURVIVED, 1 NO_COVERAGE, 1 TIMED_OUT.
- Replay del residuo: `progress/mutation_start_work_backend_replay.md:3,15` —
  11/11 KILLED; los cinco supervivientes originales pasan a muertos. El juez
  deja escrito en `judge_start_work_final.md:19` que **no es una nueva puerta de
  cierre**, sino refuerzo de oráculo.
- Frontend Stryker: `progress/mutation_start_work_frontend_final.md:3` —
  483/539 = **89,61 %**, PASS, con tabla por fichero en las líneas 21-26.

**Puerta declarada en el arnés:** `frontend/stryker.start-work-session.config.json`
(umbrales 90/80/80 sobre `work-session-api.ts`, `work-session.tsx` y
`task-reader.tsx`), cableada en `scripts/project.mjs:349-356`
(`start_work_session-frontend`), `:345-346` (`pitest -PmutationScope=start_work_session`)
y `:341-342` para el replay.

## Feature 15 — `pause_resume_session`

**Juez:** `progress/judge_pause_resume_final.md:3` — «APPROVED para cerrar
feature 15». La línea 25 confirma la PR 14 fusionada en `main` (`b2ea1f2`).

**Mutación:**

- Backend PIT: `progress/mutation_pause_resume_backend.md:3` — 523/525 =
  **99,62 %**, 0 supervivientes, 2 sin cobertura. PASS.
- Frontend Stryker: `progress/mutation_pause_resume_frontend.md:9` — 741/861 =
  **86,06 %**. Después, `progress/review_pause_resume_mutation_frontend.md:21`
  descuenta de forma conservadora tres muertes atribuidas a un fixture inestable
  de `@s37`, dejando 738/861 = **85,71 %**, que sigue por encima de 80. El juez
  cita ese ajuste en `judge_pause_resume_final.md:23`.

**Puerta declarada en el arnés:** `frontend/stryker.pause-resume-session.config.json`
(umbrales 90/80/80 sobre `work-session-state-api.ts`, `work-session-state.tsx` y
`work-session.tsx`), cableada en `scripts/project.mjs:326-333` y `:337-338`.

## Recomendación

No hay que reabrir nada. Sí conviene **unificar los nombres de los informes** o
relajar el patrón de `CLAUDE.md`, porque el próximo barrido de coherencia
volverá a dar el mismo susto: hoy costó una auditoría entera comprobar que dos
features cerradas hace días estaban bien cerradas.
