# Compatibilidad de fixtures E2E con V12

Propuesta aislada sobre `538d55e`, sin cambios de producto ni de oráculos. Baseline init comunicado por root: `7dcf39`, 1444 backend, 1453 frontend y 17 scripts GREEN. Ponytail full y Caveman lite; no se repite init mientras los autores backend trabajan en otro árbol.

## RED real

`node scripts/e2e.mjs e2e/project-states.spec.mjs:17` ejecutó API y PostgreSQL reales en el stack aislado `organizationweb-e2e-46292`. Resultado `1dbc0c`: un test fallido, EXIT 1, antes del recorrido por `cannot truncate a table referenced in a foreign key constraint`; detalle: `block_projections` referencia `planned_blocks`. El runner retiró su stack y volumen.

V12 añade referencias de `block_projections` y `block_changes` a `planned_blocks`, además de la FK compuesta de `block_changes` a `tasks`. Las once listas heredadas no incluían las tablas nuevas.

## Cambio mínimo

Añadidas explícitamente `block_changes, block_projections` a las once sentencias TRUNCATE existentes: authentication, availability, complete-reopen-task, create-task, edit-project, project-states, read-projects, schedule-block, split-task, today-native-zoom y today. Inventario `rg`: exactamente once coincidencias. No CASCADE, helper nuevo, cambios de aserciones, esperas, tamaños ni omisiones.

Diff inicial `aaea8b`: once sustituciones de una línea. Comprobación sintáctica Node de los once archivos y `git diff --check` pasan. Se conserva el formato circundante anterior.

## GREEN y entrega

`pnpm test:e2e`, sesión 59772, stack `organizationweb-e2e-16204`: **91/91 PASS, EXIT 0, 6.2 minutos**, evidencia final `2ed762`. Incluye el recorrido RED original ahora GREEN (`ffeebb`), todos los anchos de disponibilidad 320–2560, las cuatro pruebas de estados, recuperación de ACK perdido tras reinicio real, y zoom nativo Chromium al 200 % en Windows (test 87). No fallos ni omisiones reportados. El runner retiró su stack y volumen al terminar; puerto 18080 liberado.

Fuentes de producto, configuración, workflow y oráculos permanecen intactos. Entrega de las once listas y esta bitácora para revisión independiente y publicación por root; no se atribuye a esta ejecución validación Linux ni cierre de feature 13.
