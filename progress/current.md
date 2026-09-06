# Sesión actual — Codex frontend

Feature13 reschedule in_progress. Features1–12 done;14–30 pending. Contrato compartido d1ff609:41 escenarios/156 casos representativos. Autorización global vigente; Ponytail full/Caveman lite. No se declara MVP, despliegue ni cierre13.

## Coordinación paralela

El usuario indicó «Paralelamente tanto tu como Claude sin pisaros» después de detectar706a0bc. Reparto comunicado: Codex frontend/pruebas; Claude Code backend. Rama activa Codex: codex/reschedule-frontend. No se fuerza main ni se continúa el backend paralelo de Codex. Ver [parallel-coordination.md](parallel-coordination.md), publicado en main a378925. Falta acuse directo de Claude Code; el usuario recibió la instrucción para trasladársela.

Las notas originales de la otra sesión se conservan íntegramente en [claude-session-706a0bc.md](claude-session-706a0bc.md), atribuidas a ese commit. No son ejecuciones realizadas por este coordinador. Los workflows de706a0bc se integran sin cambios; no se altera paralelismo ni umbral de pruebas en esta pista.

## Avance Codex

- Cliente API aprobado parcial, cf5e3e3:19 tests nuevos+250 API11=269GREEN0f2e3a; tipos/lint b139b1. Dictamen independiente [review_reschedule_frontend_api.md](review_reschedule_frontend_api.md), hashes verificados458b75. No acredita UI completa ni servidor.
- Primer panel UI parcial:3 tests nuevos+84 UI11=87GREEN5a2120; tsc9c8ee9, formato/lintf8bf9f. Refactor de presentación temporal reutilizada; todavía faltan movimiento completo, recuperación e historial. Detalle en [tdd_reschedule_frontend.md](tdd_reschedule_frontend.md).
- Backend previo de Codex conservado exclusivamente como referencia en rama codex/reschedule-backend-checkpoint, commit03ad92e, push ebda3a.29 tests focales verdes2fe2fe/06b600 y22 contextos27cc46. No desplegable: faltan reintentos, concurrencia, movimiento, lecturas vigentes, publicación y validación completa. No fusionar su V12 con una migración alternativa sin revisión.
- Baseline anterior de12: init78050,1354 frontend/17 scripts/lint y1415 backend vigentes. No acredita el código13 en desarrollo.

## Siguiente trabajo

Reanudar únicamente TDD frontend con resume_frontend después de guardar esta frontera y publicar rama. El coordinador y revisores no editan producción/tests. Pruebas de integración real requieren backend de Claude Code; revisión, mutación y UX siguen obligatorias antes de cerrar. Evitar suites simultáneas del mismo stack y no cambiar archivos compartidos sin coordinar.

## Límites

No acceder ni limpiar .e2e-work/read-review*, frontend/.stryker-tmp-availability-replay o progress/proposal_schedule_block_time.md, ni borrar sus ascendientes. No fuerza de push, despliegue o limpieza global. Las pruebas y notas históricas conservan sus resultados reales, incluidos fallos; ninguna ejecución remota se presupone verde.
