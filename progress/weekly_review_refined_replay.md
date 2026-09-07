# Refuerzo dirigido de revisión semanal

La campaña original se conserva: 615 Killed, 144 Survived, 2 NoCoverage, 3 Timeout y 2 RuntimeError. No se ha repetido ni reclasificado. Los seis refuerzos autorizados ejercitan protección existente; ninguno exigió cambiar TS productivo.

| Fila | Oráculo | Primera ejecución |
| --- | --- | --- |
| 1 | Catálogo pendiente, desmontaje, HTTP 401 entregado antes del observador: no revoca acceso | GREEN 100b3a, `weekly_review_refine_1.log` |
| 2 | Primer reintento vuelve a 503; segundo manual consulta la misma selección y obtiene semana | GREEN 4ac38e, `weekly_review_refine_2.log` |
| 3 | Foco voluntario en fecha y después body: la respuesta no lo lleva al encabezado | GREEN e761cc, `weekly_review_refine_3.log` |
| 4 | Semana martes–lunes completamente coherente, salvo requisito de lunes | GREEN 0e544c, `weekly_review_refine_4.log` |
| 5 | Un presupuesto diario null y seis ceros con total conocido: rechaza | GREEN 2ec9e8, `weekly_review_refine_5.log` |
| 6 | Siete presupuestos positivos de 1 con suma 7: acepta | GREEN 8fa0fa, `weekly_review_refine_6.log` |

Root detectó que el refuerzo de foco había sustituido el resultado final del caso anterior. Se conserva ahora el mismo cuerpo parametrizado con dos filas: permanece en el control / pasa a body. Ambas pasan inicialmente, d60265 (`weekly_review_refine_3_preserved.log`). No se duplicó el fixture ni se retiró el destino final original.

Foco final: 69/69 (39 API y 30 UI), `weekly_review_refined_final_focal.log`. Global anterior al añadido de la fila conservada: 1967/1967 en 42 archivos, EXIT 0 6e2f8c, `weekly_review_refined_global.log`; no se atribuye ese global al último delta de una fila. Lint/build previos EXIT 0 950f99, `weekly_review_refined_lint.log` y `weekly_review_outline_build.log`. Lint final en `weekly_review_refined_final_lint.log`.

## Propuesta, todavía sin ejecutar

`weekly_review_refined_replay_proposal.json` contiene siete hashes, siete nodos AST completos mínimos y 19 firmas originales identificadas por archivo, ubicación, mutador y replacement. El generador documental `weekly_review_refined_replay_proposal.mjs` verifica nodos exactos con TypeScript. Las columnas de la ubicación original del reporte son de base uno; se convierten a columnas de base cero del scope, con líneas de base uno. Validación 9159a0. No hay config de replay creada.

Se proponen los siete nodos, heredando configuración original (8 workers, perTest, todas las suites, umbral 80 y mutadores/ignores intactos). Los mutantes extra que se generen se informarán aparte. No se promete que las 19 firmas terminen Killed: algunas variantes de foco podrían conservar observables; se juzgará el resultado sin nuevos refuerzos automáticos.

El ID 157 (sustituir sólo `monday === null` por false) se retira de la lista de objetivos diferenciables del caso martes: ese caso mantiene `monday` no nulo. El nodo completo lo puede generar como extra. No se le atribuye cobertura nueva ni equivalencia universal. Los límites P2, los dos RuntimeError y los tres Timeout originales permanecen como documentados en `mutation_weekly_review_frontend.md`.

Versionar selectivamente los dos tests y documentos/JSON de propuesta. El generador es evidencia reproducible de los rangos, no parte del arnés productivo. Raw original intacto. No nuevos targets, campañas ni cambios de producción TS.
