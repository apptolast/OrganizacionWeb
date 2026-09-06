# Sesión actual

## Feature activa

Feature13 `reschedule`, in_progress. Contrato aprobado features/reschedule.feature:41 escenarios/156 casos, commit d1ff609. Coordinación asumida por craftsman_lead de Claude Code tras la sesión anterior; sin pisar contratos ni progreso existentes.

## Arranque verificado de esta sesión

- Remoto sincronizado: local igualado a origin/main d1ff609 por fast-forward. Sin cambios locales pendientes.
- `node scripts/project.mjs install` ejecutado: `node_modules` faltaba en el árbol local y `init` no lo instala.
- `node .harness/harness.mjs init` en verde: lint sin errores, backend BUILD SUCCESSFUL,17 pruebas del arnés y1354 frontend.
- Límite de entorno de esta máquina: `NoDefaultCurrentDirectoryInExePath=1` impide que cmd.exe resuelva `gradlew.bat` desde su directorio. Los comandos del arnés se lanzan con el prefijo `env -u NoDefaultCurrentDirectoryInExePath`. No se modifica el repositorio por esta causa.
- Defecto detectado: `frontend/src/create-task.test.tsx` @s29 falló una vez bajo carga (`fetcher.mock.calls[1]` undefined) y pasa aislado y en repetición. Afirma la llamada de forma síncrona tras `findByRole`, sin esperar al efecto. Queda registrado para endurecer en esta sesión; no es cambio de producción.

## Plan de esta feature

Fases paralelas sin solape de archivos, luego secuenciales:
1. Backend Java completo (dominio, aplicación, PostgreSQL con migración aditiva, HTTP, evento BlockChanged.v1) → progress/tdd_reschedule_backend.md.
2. Cliente API de frontend aislado → progress/tdd_reschedule_frontend_api.md.
3. Interfaz de mover/cancelar/historial → progress/tdd_reschedule_frontend_ui.md.
4. Recorrido real E2E y matriz UX → progress/tdd_reschedule_e2e.md.
5. judge independiente, security_reviewer, a11y_seo_auditor, mutation_tester.

## Continuidad

Features1–12 done;13 in_progress;14–30 pending. Autorización global vigente; no repetir permiso por contrato. Ponytail full y Caveman lite en cada delegación.

## Límites vigentes

No se declara MVP completo, CI remoto ni despliegue. No leer ni limpiar proposal_schedule_block_time.md, .e2e-work/read-review* ni frontend/.stryker-tmp-availability-replay, ni eliminar sus ascendientes.

## Hallazgo de CI remota (6 de septiembre de 2026, coordinador)

La CI no completa desde a127747 (12:09 UTC, éxito). Ocho ejecuciones de `Application CI` quedaron simultáneas y en curso más de dos horas cada una: `feat: read the personal day agenda` (12:54 UTC) seguía en el paso `node .harness/harness.mjs verify` a las14:55 UTC. Ese paso ejecuta init más la campaña de mutación completa en un runner gratuito de dos núcleos, con `timeout-minutes:240` y sin grupo `concurrency`, así que cada push apila otra ejecución de horas en lugar de sustituir la anterior.

Consecuencia comprobable: feature12 `today` y el contrato13 están cerrados y verificados **en local**, no en CI remota. No se atribuye verde remoto a ningún commit posterior a a127747.

Acción tomada: cancelar las siete ejecuciones superadas y conservar sólo la de d1ff609, que es HEAD. Es lo que haría un grupo `concurrency` con `cancel-in-progress`.

Acción pendiente de decisión del usuario: `.github/workflows/` es ruta sensible según `.github/workflows/guard-sensitive-paths.yml` y `.github/AUTONOMOUS.md`. La propuesta es separar la puerta rápida por push (lint, tests, build, E2E y publisher) de la campaña de mutación, que pasaría a `workflow_dispatch` y ejecución nocturna, más el grupo `concurrency` con cancelación. La mutación seguiría siendo obligatoria para cerrar cada feature, ejecutada por `mutation_tester` en local. No se modifica el workflow sin esa confirmación.

## Hallazgo de rendimiento de la puerta de mutación (coordinador)

Esta máquina tiene24 núcleos lógicos y63,4 GB de RAM. La campaña de mutación usa una fracción mínima: `backend/build.gradle.kts` fija `threads.set(4)` para PIT y todas las configuraciones de Stryker de `frontend/` fijan `"concurrency": 2`. `tasks.test` no declara `maxParallelForks`. Eso explica que las campañas de mutación dominen el reloj de cada feature.

Propuesta pendiente de validación, no aplicada todavía: subir PIT a doce hilos y Stryker a ocho para las campañas acotadas por feature. Es un cambio de velocidad, no de qué se mide, pero tiene un riesgo real registrado en la memoria organizacional (`testing/informe-de-mutacion-con-timeouts-miente.md`): más contención de CPU produce más timeouts y Stryker cuenta Timeout como matado. Cualquier subida se acepta sólo si el informe conserva cero timeouts y el score reproduce el de una campaña controlada del mismo alcance.

No se aplica ahora porque `backend/build.gradle.kts` está dentro del alcance de escritura del agente de backend en curso. Se evalúa al cerrar esa pista.

## Memoria organizacional

`scripts/sync-memoria.sh` ejecutado con éxito:24 patrones en `.memoria-cache/patterns/`. Los patrones aplicables se comunicaron a las pistas de TDD en curso.

## Decisión del usuario sobre CI y paralelismo (6 de septiembre de 2026)

El usuario elige separar la puerta rápida de la campaña de mutación, y subir el paralelismo con validación. Ambas quedan registradas como instrucción vigente.

CI: `harness-ci.yml` ejecuta por push y PR init, build, E2E y publisher con `timeout-minutes:90` y grupo `concurrency` con cancelación. `harness-mutation.yml` ejecuta `harness verify` de noche y a petición, con grupo propio sin cancelación y `timeout-minutes:240`. La mutación sigue siendo obligatoria en local para cerrar cada feature según `docs/verification.md` y C7 de `CHECKPOINTS.md`; el workflow nocturno no la sustituye.

Paralelismo: subir PIT a doce hilos y Stryker a ocho, aceptado sólo si el informe conserva cero timeouts y reproduce el score de una campaña controlada del mismo alcance. Pendiente hasta que cierren las pistas de TDD que escriben en esos archivos.

## Reparto con Codex, confirmado el 6 de septiembre de 2026

El segundo equipo no era otra sesion de Claude Code: es Codex, la sesion de ChatGPT que llevaba el proyecto antes. El usuario confirmo el reparto. Claude Code toma backend Java, PostgreSQL, HTTP y EDA con sus pruebas. Codex toma frontend React/SCSS, cliente de API y pruebas de frontend, en `codex/reschedule-frontend`. Documento unico de coordinacion: [parallel-coordination.md](parallel-coordination.md), con el acuse de recibo publicado en edc7f97.

Acciones ejecutadas al aceptar el reparto:

- Detenida la pista propia de cliente de API de frontend, que duplicaba el carril de Codex. Estaba verde con 213 pruebas focales.
- Devuelto el carril de frontend limpio en `main`: `schedule-block-api.ts` restaurado y los archivos nuevos retirados del arbol de trabajo.
- Ese corte detenido queda publicado como referencia en `claude/reschedule-frontend-api-checkpoint` (d0e83bb), simetrico al checkpoint de backend de Codex. No se fusiona.
- Continua la pista de backend sobre el contrato d1ff609, sin interrupcion.
- Reservadas las versiones Flyway V12 a V19 para este backend. La V12 alternativa de `codex/reschedule-backend-checkpoint` no se aplica.

Aviso pendiente de arreglar, porque el usuario exige cero warnings: la CI emite la deprecacion de Node.js 20 en `gradle/actions/setup-gradle@v4` y `pnpm/action-setup@v4`. No se toca ahora para no encadenar cambios de workflow durante la coordinacion.

Nota de operacion: la ejecucion de CI de 706a0bc se cancelo sola al llegar a378925. Es el grupo `concurrency` funcionando como se diseno, no un fallo.

