# Coordinación Codex / Claude Code — feature13

El usuario indicó a Codex el6 de septiembre de2026: «Paralelamente tanto tu como Claude sin pisaros». Se detectó trabajo simultáneo sobre reschedule al recibir706a0bc mientras Codex preparaba cf5e3e3. No se debe reemplazar la implementación del otro equipo ni forzar main.

## Reparto comunicado por Codex

- **Codex: frontend React/SCSS, cliente API y pruebas de frontend**, en rama `codex/reschedule-frontend`. No continuará implementando backend. El cliente ya tiene revisión independiente y269 pruebas focales verdes (19 nuevas+250 API11); commit cf5e3e3, pendiente de publicar su rama al escribir esta nota.
- **Claude Code: backend Java/PostgreSQL/HTTP/EDA y sus pruebas.** Este reparto se ha comunicado al usuario para trasladarlo a Claude Code; Codex no tiene canal directo y no atribuye un acuse de recibo todavía. Evitar una segunda implementación de frontend; si ya existe, comparar ambos cortes antes de integrar.
- **Contrato compartido:** d1ff609, features/reschedule.feature,41 escenarios/156 casos representativos. Cambios de contrato o formato HTTP deben comunicarse antes de implementar otra interpretación.
- **Integración:** revisar diferencias y pruebas de ambas ramas antes de fusionar. UI y cliente pueden desarrollarse con pruebas de componentes; el recorrido real requiere el backend acordado. No atribuir E2E, mutación ni cierre antes de ejecutarlos.

## Avance previo preservado

Codex estaba desarrollando backend antes de detectar el solape. Se detiene en una frontera acotada y se conservará en `codex/reschedule-backend-checkpoint`, exclusivamente como referencia/reutilización opcional. No es otra propuesta para fusionar automáticamente ni una funcionalidad terminada. Su migración V12 no debe aplicarse junto a una migración alternativa sin revisión.

El cambio remoto de CI706a0bc se conserva. Esta nota no cambia workflows, umbrales, paralelismo de pruebas ni decisiones de cierre. Los informes de cada equipo deben identificar su rama y ejecución; las pruebas de un árbol no acreditan automáticamente otro.

Publicación confirmada: frontend en `codex/reschedule-frontend` a74a812 (incluye cliente cf5e3e3 y primer panel parcial con87 pruebas focales verdes). Backend previo en `codex/reschedule-backend-checkpoint`03ad92e, detenido con29 pruebas focales verdes; contiene pendientes explícitos y no está listo para integrar. Codex reanuda sólo frontend. Ambos avances están respaldados; ninguna de estas ramas se ha fusionado a main.

## Límites compartidos

No acceder ni limpiar `.e2e-work/read-review*`, `frontend/.stryker-tmp-availability-replay` o `progress/proposal_schedule_block_time.md`, ni eliminar sus ascendientes. No force-push ni borrado de ramas/archivos del otro equipo. La autorización de features/commits sigue vigente; no se declara MVP, despliegue o feature13 terminada.
