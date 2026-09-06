# Mutación frontend: dividir tareas

Campaña completa finalizada, EXIT 0: **558/601 detectados (92,85 %)**, 41 Survived y 2 NoCoverage, sin timeout ni errores. Duración: 15 minutos y 20 segundos. Supera el umbral de 80. El replay focal posterior es una ejecución separada y no modifica esa puntuación.

Precondiciones: inicio independiente del coordinador 9396, EXIT 0, con 622 pruebas backend y 462 frontend; lint global verde. Revisión de diseño y alcance aprobada antes de ejecutar. PIT backend terminó antes de iniciar Stryker. Concurrencia 2, umbrales originales conservados.

## Alcance exacto de la campaña completa

Perfil: `frontend/stryker.split-task.config.json`.

- `src/tasks-api.ts`: archivo completo; nuevas consultas de detalle/relación, contexto opcional de hijos y validación compartida.
- `src/task-reader.tsx`: archivo completo; ruta de tarea, carga, contexto del proyecto, recuperación y foco.
- `src/task-parent.tsx`: archivo completo; padre confirmado, raíz, error y cancelación.
- `src/project-tasks.tsx`: archivo completo; composición de formulario/lista, enlaces y presentación específica de subtareas.
- `src/use-project-tasks.ts:34:0-42:200`: consulta y dependencias que incorporan parentTaskId.
- `src/use-project-tasks.ts:64:0-78:200`: llamada de creación que incorpora parentTaskId.
- `src/App.tsx:10:0-14:200`: reconocimiento y selección de ruta.
- `src/use-session.ts:187:0-189:200`: ruta privada admitida después de comprobar sesión.

Son 7 archivos instrumentados. La lógica histórica no modificada del hook y la sesión queda fuera de esta campaña focal; permanece completa en el perfil global. La validación histórica de tareas no cambió. El perfil global añade los dos archivos nuevos y el rango de ruta de App, conservando todos sus alcances históricos. No se excluye lógica nueva ni se rebaja el umbral.

El transporte y sus 49 pruebas iniciales nuevas fueron desarrollados mediante delegación explícita por integration_craftsman; ver `progress/tdd_split_task_api.md`. Los tres ejemplos adicionales de errores controlados del replay los añadió frontend_craftsman. La revisión independiente del API corresponde al coordinador, no a su autor.

## Resultados observados

| Ejecución | Denominador | Killed | Survived | NoCoverage | Timeout/error | Resultado |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| Completa | 601 | 558 | 41 | 2 | 0 | 92,85 %, EXIT 0 |
| Replay focal | 58 | 56 | 2 | 0 | 0 | 96,55 %, EXIT 0 |

Original preservado en `frontend/reports/mutation-split-task/mutation-original.json`; copia de salida completa `mutation.json` y HTML `mutation.html`. Replay: `replay.json`, `replay.html`, perfil `frontend/stryker.split-task.replay.config.json`. Duración del replay: 2 minutos y 2 segundos. El dry run completo informó 418 pruebas y el replay 431; la suite normal completa observada antes y después de ampliar pruebas fue 462 y **475**, respectivamente. No se equiparan esos contadores del runner con la suite normal.

Tras el resultado completo se añadieron únicamente pruebas: tipos inválidos con error controlado, rutas parciales, estimación nula, foco durante carga, limpieza de errores y snapshot, repetición de reintento y cancelación con efecto global de sesión. No cambió producción ni se repitió una campaña global. La primera suite posterior encontró que un doble todavía no había registrado su función de resolución; se corrigió esperando explícitamente el registro de la petición. La siguiente suite completa pasó **475/475**, 14 archivos. Lint verde. El build y la integración del corte de producción siguen válidos porque sólo se añadieron pruebas y perfiles.

## Identidades originales detectadas en el replay

Se compararon archivo, fuente embebida completa, localización inicial/final, mutador y replacement. La fuente coincide exactamente en todos los emparejamientos. Los IDs son identificadores opacos, no posiciones dentro del denominador.

| Archivo | ID original → ID replay | Resultado |
| --- | --- | --- |
| App | 0→0, 1→1 | Killed |
| task-parent | 180→6, 186→8 | Killed |
| task-reader | 194→11, 195→12, 197→14, 208→23, 214→25 | Killed |
| task-reader | 251→27, 254→30, 255→31, 257→33, 259→35 | Killed |
| task-reader | 268→36, 272→38, 284→40, 286→42, 288→44 | Killed |
| task-reader | 293→45, 295→47, 297→49 | Killed |
| tasks-api | 321→53, 349→60 | Killed |

Son **24 identidades originales ahora detectadas**. Los NoCoverage originales eran task-reader **259, línea 64**, asignación de pérdida de acceso desde GET del proyecto, y **297, línea 109**, texto de estimación ausente; ambos quedaron Killed. El 268 se detecta por un 401 tardío real en el cliente compartido con SessionGate y borrador de otra tarea, no sólo inspeccionando AbortSignal. Los 321/349 requieren error exacto controlado ante projectId no textual y relación JSON de un carácter; no son equivalentes a lanzar TypeError.

También se emparejaron 187→9 y 205→20, que continúan Survived con las justificaciones siguientes. No se suman scores ni se presenta una nueva campaña completa inferida.

## Los 19 restantes: 12 equivalentes y 7 variantes permitidas

Revisión independiente: `progress/review_split_task_frontend_mutants.md`; el coordinador aceptó las variantes separadas de equivalencia. El 194 estaba inicialmente propuesto como variante de tiempo de foco, pero la prueba adicional de carga independiente lo detecta; no queda entre los restantes.

Equivalentes (12): **25, 163, 187, 217, 238, 240, 249, 264, 292, 391, 474, 601**. Las justificaciones de montaje, refs y contadores están en la [revisión independiente](review_split_task_frontend_mutants.md). En particular, las guardas de setters tras desmontaje no se confunden con cancelar la petición: retirar cleanup 268 sí permite un 401 global tardío y está detectado. Los contadores decrecientes siguen cambiando; 186, que deja undefined y bloquea el segundo retry, está detectado. Para API, 391 sigue rechazando primitivos JSON por typeof data.id y 474 es redundante con Number.isInteger, que exige number.

Variantes permitidas (7): **16, 17, 151, 158, 159, 188, 205**. Cambian el momento de enfocar un destino local cuando body está activo, o el espacio de presentación junto al enlace. Conservan un destino significativo, no roban foco elegido y no alteran los datos. Se aceptan por contrato; no se presentan como ausencia de diferencia observable ni como equivalentes. La revisión independiente y el dictamen del coordinador recogen esa distinción.

La clasificación cubre los 43 resultados originales no detectados: 24 detectados mediante replay, 12 equivalentes y 7 variantes contractualmente permitidas. No quedan huecos observables sin resolver en el alcance revisado. El coordinador verificó independientemente los JSON, los 24 emparejamientos y la suite normal de 475 pruebas, y emitió dictamen final APPROVED. Se liberan los archivos; este informe no modifica lifecycle.
