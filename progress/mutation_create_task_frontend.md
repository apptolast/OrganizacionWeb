# Mutación frontend: crear tareas

Estado final: **APPROVED** por revisión independiente. Campaña actual completa: **480/505 = 95,05 %**, EXIT 0. Replay focal separado: **16/16**, EXIT 0. Quedan 19 equivalentes justificados y 2 variantes de foco permitidas por contrato; no se calcula un porcentaje combinado.

Configuración: `frontend/stryker.create-task.config.json`, Stryker 10 con runner Vitest, cobertura por prueba y dos workers. Umbral requerido: 80; no se rebaja.

Alcance completo de cuatro archivos:

- `src/tasks-api.ts`: transporte GET/POST y validación de respuestas públicas.
- `src/task-validation.ts`: límites Unicode y estimación entera.
- `src/use-project-tasks.ts`: borrador, confirmación, errores, cancelación, cursores y recuperación explícita del proyecto.
- `src/project-tasks.tsx`: composición visible, estado completed, campos, controles y foco.

No se vuelve a mutar lógica histórica sin cambios. Los cuatro archivos también forman parte de `stryker.config.json` para las verificaciones futuras. Las cinco suites históricas aíslan únicamente el componente nuevo; las 71 pruebas nuevas del primer corte y los E2E de integración cubren su composición real; el cierre amplía la suite a 111 pruebas nuevas.

Base previa: lint y build verdes; suite completa de 331 pruebas. Resultado, supervivientes y cualquier replay se documentarán por separado, conservando el denominador de la campaña original.


Informe original conservado como mutation-original.json y mutation-original.html dentro de reports/mutation-create-task. La campaña duró 15 minutos y 53 segundos. Dry run: 292 tests según el runner; suite normal previa: 331 pruebas. No se equiparan ambas cifras ni se suma puntuación de replays.

## Inventario de los 102 supervivientes originales

Clasificación final por identidad de archivo, localización completa, mutador y reemplazo. De los 102 originales, 77 fueron eliminados en la segunda campaña, 3 adicionales en replay, 1 fue sustituido por corrección real y 21 conservan justificación. Las guardas de producción se mantienen.

| ID original | Ubicación | Clasificación | Justificación / evidencia |
| --- | --- | --- | --- |
| 8 | src/project-tasks.tsx:39 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 9 | src/project-tasks.tsx:39 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 10 | src/project-tasks.tsx:39 | Variante permitida por contrato | Variante temporal observable: si un retry GET termina mientras POST sigue pendiente, puede restaurar el encabezado antes. Ambas variantes conservan la guarda de body, no desplazan foco elegido y ofrecen contexto significativo; el contrato no fija ese orden entre operaciones independientes. |
| 11 | src/project-tasks.tsx:39 | Variante permitida por contrato | Variante temporal observable: si un retry GET termina mientras POST sigue pendiente, puede restaurar el encabezado antes. Ambas variantes conservan la guarda de body, no desplazan foco elegido y ofrecen contexto significativo; el contrato no fija ese orden entre operaciones independientes. |
| 14 | src/project-tasks.tsx:41 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 19 | src/project-tasks.tsx:45 | Equivalente justificado | El encabezado está montado cuando se ejecuta el efecto; el efecto no corre después del desmontaje. |
| 22 | src/project-tasks.tsx:56 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 24 | src/project-tasks.tsx:60 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 31 | src/project-tasks.tsx:81 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 33 | src/project-tasks.tsx:82 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 48 | src/project-tasks.tsx:107 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 53 | src/project-tasks.tsx:109 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 52 | src/project-tasks.tsx:109 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 59 | src/project-tasks.tsx:125 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 63 | src/project-tasks.tsx:127 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 64 | src/project-tasks.tsx:128 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 70 | src/project-tasks.tsx:148 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 74 | src/project-tasks.tsx:150 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 75 | src/project-tasks.tsx:151 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 95 | src/project-tasks.tsx:178 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 96 | src/project-tasks.tsx:178 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 97 | src/project-tasks.tsx:178 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 107 | src/project-tasks.tsx:182 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 109 | src/project-tasks.tsx:182 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 110 | src/project-tasks.tsx:182 | Eliminado en replay focal | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 111 | src/project-tasks.tsx:182 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 112 | src/project-tasks.tsx:182 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 113 | src/project-tasks.tsx:188 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 114 | src/project-tasks.tsx:188 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 115 | src/project-tasks.tsx:188 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 116 | src/project-tasks.tsx:189 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 117 | src/project-tasks.tsx:190 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 125 | src/task-validation.ts:13 | Eliminado en segunda campaña | Ejemplo solicitado por revisión independiente; tasks-api.test.ts o task-validation.test.ts. |
| 126 | src/task-validation.ts:13 | Eliminado en segunda campaña | Ejemplo solicitado por revisión independiente; tasks-api.test.ts o task-validation.test.ts. |
| 128 | src/task-validation.ts:13 | Eliminado en segunda campaña | Ejemplo solicitado por revisión independiente; tasks-api.test.ts o task-validation.test.ts. |
| 129 | src/task-validation.ts:13 | Eliminado en segunda campaña | Ejemplo solicitado por revisión independiente; tasks-api.test.ts o task-validation.test.ts. |
| 136 | src/task-validation.ts:15 | Eliminado en segunda campaña | Ejemplo solicitado por revisión independiente; tasks-api.test.ts o task-validation.test.ts. |
| 145 | src/task-validation.ts:16 | Eliminado en segunda campaña | Ejemplo solicitado por revisión independiente; tasks-api.test.ts o task-validation.test.ts. |
| 162 | src/task-validation.ts:20 | Eliminado en segunda campaña | Ejemplo solicitado por revisión independiente; tasks-api.test.ts o task-validation.test.ts. |
| 164 | src/task-validation.ts:21 | Eliminado en segunda campaña | Ejemplo solicitado por revisión independiente; tasks-api.test.ts o task-validation.test.ts. |
| 165 | src/task-validation.ts:21 | Eliminado en segunda campaña | Ejemplo solicitado por revisión independiente; tasks-api.test.ts o task-validation.test.ts. |
| 175 | src/tasks-api.ts:15 | Equivalente justificado | Equivalencia de validaciones redundantes revisada independientemente en review_task_validation_mutants.md. |
| 210 | src/tasks-api.ts:22 | Eliminado en segunda campaña | Ejemplo solicitado por revisión independiente; tasks-api.test.ts o task-validation.test.ts. |
| 213 | src/tasks-api.ts:23 | Eliminado en segunda campaña | Ejemplo solicitado por revisión independiente; tasks-api.test.ts o task-validation.test.ts. |
| 214 | src/tasks-api.ts:23 | Eliminado en segunda campaña | Ejemplo solicitado por revisión independiente; tasks-api.test.ts o task-validation.test.ts. |
| 260 | src/tasks-api.ts:33 | Equivalente justificado | Equivalencia de validaciones redundantes revisada independientemente en review_task_validation_mutants.md. |
| 272 | src/tasks-api.ts:38 | Eliminado en segunda campaña | Ejemplo solicitado por revisión independiente; tasks-api.test.ts o task-validation.test.ts. |
| 275 | src/tasks-api.ts:40 | Eliminado en segunda campaña | Ejemplo solicitado por revisión independiente; tasks-api.test.ts o task-validation.test.ts. |
| 283 | src/tasks-api.ts:57 | Eliminado en segunda campaña | Ejemplo solicitado por revisión independiente; tasks-api.test.ts o task-validation.test.ts. |
| 284 | src/tasks-api.ts:57 | Eliminado en segunda campaña | Ejemplo solicitado por revisión independiente; tasks-api.test.ts o task-validation.test.ts. |
| 304 | src/tasks-api.ts:80 | Eliminado en replay focal | Ejemplo solicitado por revisión independiente; tasks-api.test.ts o task-validation.test.ts. |
| 323 | src/tasks-api.ts:83 | Eliminado en segunda campaña | La clasificación preliminar confundió esta guarda typeof data con 304 (status HTTP). El ejemplo JSON primitivo la elimina; no es equivalente. |
| 349 | src/tasks-api.ts:93 | Eliminado en segunda campaña | Ejemplo solicitado por revisión independiente; tasks-api.test.ts o task-validation.test.ts. |
| 358 | src/use-project-tasks.ts:9 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 359 | src/use-project-tasks.ts:10 | Equivalente justificado | El elemento centinela no coincide con ningún TaskField; las tres consultas includes siguen falsas y cada submit reemplaza el array. |
| 360 | src/use-project-tasks.ts:11 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 371 | src/use-project-tasks.ts:29 | Equivalente justificado | Una dependencia literal constante produce los mismos montaje y cleanup que el array vacío. |
| 376 | src/use-project-tasks.ts:35 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 381 | src/use-project-tasks.ts:38 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 388 | src/use-project-tasks.ts:43 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 397 | src/use-project-tasks.ts:48 | Equivalente justificado | El formulario renderizado contiene siempre los controles de los nombres permitidos; el elemento existe incluso en la referencia DOM capturada si se desmonta. |
| 400 | src/use-project-tasks.ts:51 | Equivalente justificado | Sólo puede duplicar estimatedMinutes en el array de errores; la vista consulta includes y no itera ese array. |
| 407 | src/use-project-tasks.ts:56 | Equivalente justificado | El formulario renderizado contiene siempre los controles de los nombres permitidos; el elemento existe incluso en la referencia DOM capturada si se desmonta. |
| 411 | src/use-project-tasks.ts:62 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 412 | src/use-project-tasks.ts:63 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 420 | src/use-project-tasks.ts:76 | Equivalente justificado | La cancelación de esta escritura sólo ocurre al desmontar el componente. Estos bloques únicamente actualizan estado local; React lo descarta y no monta efectos ni consultas nuevas. apiRequest mantiene su propia guarda global de sesión. |
| 423 | src/use-project-tasks.ts:79 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 427 | src/use-project-tasks.ts:81 | Identidad sustituida por corrección | La expresión capturada revision + 1 se sustituyó por actualización funcional tras RED/GREEN de carrera real. El mutante original deja de pertenecer al código actual; no se declara eliminado ni se suma a kills. |
| 430 | src/use-project-tasks.ts:83 | Equivalente justificado | La cancelación de esta escritura sólo ocurre al desmontar el componente. Estos bloques únicamente actualizan estado local; React lo descarta y no monta efectos ni consultas nuevas. apiRequest mantiene su propia guarda global de sesión. |
| 431 | src/use-project-tasks.ts:89 | Equivalente justificado | undefined y null son equivalentes para las consultas opcionales code/errors posteriores. |
| 433 | src/use-project-tasks.ts:91 | Equivalente justificado | La cancelación de esta escritura sólo ocurre al desmontar el componente. Estos bloques únicamente actualizan estado local; React lo descarta y no monta efectos ni consultas nuevas. apiRequest mantiene su propia guarda global de sesión. |
| 434 | src/use-project-tasks.ts:93 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 436 | src/use-project-tasks.ts:93 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 437 | src/use-project-tasks.ts:93 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 438 | src/use-project-tasks.ts:93 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 439 | src/use-project-tasks.ts:94 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 441 | src/use-project-tasks.ts:95 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 443 | src/use-project-tasks.ts:95 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 450 | src/use-project-tasks.ts:99 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 451 | src/use-project-tasks.ts:99 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 452 | src/use-project-tasks.ts:100 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 454 | src/use-project-tasks.ts:101 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 458 | src/use-project-tasks.ts:105 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 463 | src/use-project-tasks.ts:107 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 464 | src/use-project-tasks.ts:107 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 465 | src/use-project-tasks.ts:107 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 466 | src/use-project-tasks.ts:107 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 467 | src/use-project-tasks.ts:107 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 470 | src/use-project-tasks.ts:108 | Eliminado en segunda campaña | La clasificación preliminar identificó mal la línea: elimina entry !== null, no la comprobación de field. La prueba con entradas nulas detecta el fallo. |
| 476 | src/use-project-tasks.ts:114 | Equivalente justificado | Con array vacío la consulta del control llamado undefined no encuentra elemento y su encadenamiento opcional no cambia el foco. |
| 478 | src/use-project-tasks.ts:115 | Equivalente justificado | El formulario renderizado contiene siempre los controles de los nombres permitidos; el elemento existe incluso en la referencia DOM capturada si se desmonta. |
| 485 | src/use-project-tasks.ts:123 | Equivalente justificado | La cancelación de esta escritura sólo ocurre al desmontar el componente. Estos bloques únicamente actualizan estado local; React lo descarta y no monta efectos ni consultas nuevas. apiRequest mantiene su propia guarda global de sesión. |
| 491 | src/use-project-tasks.ts:127 | Equivalente justificado | La única entrada pública es el botón deshabilitado durante reviewing; la función no está expuesta a otros consumidores. |
| 493 | src/use-project-tasks.ts:130 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 497 | src/use-project-tasks.ts:136 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 501 | src/use-project-tasks.ts:139 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 504 | src/use-project-tasks.ts:141 | Equivalente justificado | Después de desmontar, esta rama de revisión sólo cambia estado local descartado; no invoca onProjectConfirmed. La guarda del callback de éxito (497) sí es observable y tiene prueba específica. |
| 510 | src/use-project-tasks.ts:146 | Equivalente justificado | Después de desmontar, esta rama de revisión sólo cambia estado local descartado; no invoca onProjectConfirmed. La guarda del callback de éxito (497) sí es observable y tiene prueba específica. |
| 515 | src/use-project-tasks.ts:150 | Equivalente justificado | El botón Reintentar sólo se muestra por fallo de una lectura iniciada sin page; cada navegación/creación ya retira la página antes de iniciar GET. |
| 519 | src/use-project-tasks.ts:152 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |
| 520 | src/use-project-tasks.ts:152 | Eliminado en replay focal | El replay detecta la cancelación de invalidación con confirmación y clic público antes del commit React. Es una comprobación unitaria de lotes, no una reproducción de un defecto de producción en navegador. |
| 522 | src/use-project-tasks.ts:155 | Eliminado en segunda campaña | Recorridos ampliados de campos, foco, estado pendiente, errores o recuperación en create-task.test.tsx. |


## Segunda campaña actual

La ejecución de stryker.create-task.incremental.config.json leyó el informe original y produjo 505 mutantes actuales. Stryker informa explícitamente **0 de 505 resultados reutilizados**: un archivo de producción cambió (+5/-4 identidades) y tres archivos de pruebas cambiaron (+105/-69 IDs). Por tanto, esta invocación incremental ejecutó de nuevo el alcance completo. Dry run verde: 328 tests según el runner; suite normal previa a esta segunda campaña: 367 pruebas y lint verdes. No se suman campañas ni se presenta reutilización inexistente.

La segunda campaña terminó en 10 minutos y 23 segundos: **480 Killed / 505**, 25 Survived, cero Timeout, NoCoverage o errores; **95,05 %**, EXIT 0. Los artefactos se llaman `mutation-incremental.json` y `mutation-incremental.html`; el nombre refleja el modo invocado, pero no hubo reutilización de resultados.

## Replay y cierre

`stryker.create-task.replay.config.json` muta únicamente project-tasks.tsx:182, tasks-api.ts:80 y use-project-tasks.ts:81/152. Resultado observado: **16 Killed / 16**, sin otros estados, EXIT 0 en 52 segundos. La comparación exacta de archivo, localización, mutador y reemplazo empareja los supervivientes actuales 110, 304, 428 y 521 con los IDs 6, 10, 14 y 17 del replay, todos eliminados. No se modifica el 480/505 observado en la campaña completa.

El 110 se detecta mostrando la tarjeta confirmada al visitar una página antigua que no contiene esa tarea. El 304 se detecta con un GET que devuelve HTTP 201/503 y un cuerpo de colección válido: el status sigue siendo un error. Los contadores se detectan agrupando una confirmación y un clic sobre el botón todavía conectado antes del commit React, mediante DOM/fetch públicos, sin invocar el hook ni alterar estado interno. La revisión acepta esta comprobación unitaria; no se presenta como una reproducción adicional en navegador.

Los 21 restantes de la campaña completa no afectados por el replay están identificados en la tabla: 19 equivalentes y 2 variantes temporales permitidas de foco. La revisión independiente aceptó sus justificaciones. La clasificación provisional de 323 y 470 se corrigió al contrastar localización y fuente embebida; ambos quedaron eliminados, como refleja el inventario final.

Validación final del autor: **371 pruebas en 12 archivos y lint verdes**. Producción no cambió después de la actualización funcional revisada y construida. Init independiente 486 backend / 366 frontend, suite posterior del autor 367 y suite final 371 son evidencias distintas. E2E y revisión visual se conservan en los informes de integración. Dictamen final: `progress/judge_create_task.md`, APPROVED. No se ejecutan más campañas globales ni se rebaja el umbral.
