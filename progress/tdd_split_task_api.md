# split_task — cliente API frontend

El coordinador y el autor frontend delegaron exclusivamente `frontend/src/tasks-api.ts` y `frontend/src/split-task-api.test.ts` a integration_craftsman. UI/hooks y sus pruebas siguen con frontend_craftsman. Ponytail full y Caveman lite: se reutilizan apiRequest, DTO8, guardas y paginación; no se añade paquete ni capa de transporte.

## Firmas pactadas

- `readTask(projectId, id, signal)` conserva la lectura de detalle mínima que el autor ya había introducido. Se amplía su comprobación de identidad semántica y se verifica independientemente.
- `readTaskParent(projectId, id, signal?)` devuelve exactamente `{parent: Task | null}`.
- `createTask(projectId, draft, signal?, parentTaskId?)` y `readTasks(projectId, cursor?, signal?, parentTaskId?)` conservan llamadas históricas; el cuarto argumento selecciona la colección de hijos.

Una función local compone las dos rutas de colección. El helper sameId compara UUID sin distinguir caja y conserva los DTO canónicos del servidor; no convierte identificadores del contenido ni normaliza títulos. El coordinador pidió comprobar expresamente rutas UUID en mayúsculas.

## Ciclos observados

| Ciclo | RED observado | Cambio mínimo y GREEN |
| --- | --- | --- |
| 1 | Dos casos raíz/padre fallan: `readTaskParent is not a function`. | GET de relación con cookies, no-store y AbortSignal; 2/2 verdes. |
| 2 | 19 fallos de 21: status distinto de 200 y relación inválida se resolvían como éxito. | Guardas de HTTP, objeto cerrado, parent null o DTO8 propio y no autorrelación; 21/21 verdes. |
| 3 | Dos fallos de 23: creación y página llamaban todavía a la colección plana. | Cuarto argumento opcional y composición compartida de ruta; 23/23 verdes. JSON, cursor codificado y señal se conservan. |
| 4 | Cuatro fallos de 27: rutas UUID con mayúsculas rechazaban DTO canónico y una autorrelación con distinta caja pasaba. | Comparación semántica compartida de proyecto/id/padre; 27/27 verdes. |
| 5 | Dos fallos de 29: una creación podía confirmar el padre como hijo y una página podía incluir al propio padre. | Guardas de identidad hijo/padre en POST y colección, sin afectar raíces; 29/29 verdes. |

Cada RED se ejecutó con `pnpm test -- src/split-task-api.test.ts` antes del cambio correspondiente. No se presenta el readTask previo como una implementación nueva desarrollada desde RED por este agente.

## Regresión y fronteras adicionales

Se añadieron comprobaciones de comportamiento ya correcto reutilizado: detalle propio y su señal/ruta/cache; rechazo por HTTP aunque el cuerpo sea válido; detalle de otra identidad/proyecto; DTO extra o primitivo; hijos vacíos y página de veinte; 21 rechazados; cursor no string; error de red o JSON sin raíz ficticia ni reintento. Son verificación del código compartido, no ciclos RED inventados.

`pnpm test -- src/split-task-api.test.ts src/tasks-api.test.ts` terminó con **100 pruebas verdes** en dos archivos. ESLint de los dos archivos modificados y `tsc --noEmit` terminaron con salida 0. Prettier se aplicó sólo a esos archivos. No se ejecutó mutación antes de la puerta de revisión; el autor frontend integrará este módulo en su alcance final.

## Trazabilidad y límites

- s1, s2, s10, s34: rutas raíz/hijo compatibles, JSON sin parentId añadido y valores confirmados DTO8. Los límites completos heredados siguen verificados por tasks-api.test y por el endpoint backend correspondiente.
- s9, s19, s20, s23: detalle/relación estrictos, status y error preservados, no-store, cookies y cancelación propagada. El cliente no inventa una raíz ante fallo.
- s11, s12, s35: página cerrada, máximo veinte, cursor opaco codificado y colección del padre; no autorrelación.
- s6, s7, s26–s31: se reutiliza apiRequest y se conserva Response ante error; UI gestiona borradores, acceso y respuestas tardías. Estos tests del cliente no sustituyen las pruebas de sesión/CSRF ni las de hooks.

No hay implementación backend ni cambios de UI de este agente. La revisión de este módulo debe hacerla el coordinador o el autor frontend; integration_craftsman conserva únicamente la revisión independiente del backend. Las imágenes y E2E esperan freeze conjunto.
