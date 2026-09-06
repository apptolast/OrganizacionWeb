# TDD frontend: completar y reabrir tareas

Contrato aprobado: `features/complete_reopen_task.feature`, sección 9 de `project-spec.md`. Se leyeron Ponytail full, Caveman lite, TDD y la matriz UX del repositorio. Baseline heredado: 622 pruebas backend y 475 frontend, con lint verde; no se repitió init.

Autor frontend: UI, estilos, composición y configuración de mutación. integration_craftsman implementó `task-status-api.ts`, ampliación de `tasks-api.ts` y pruebas API; su bitácora y dictamen son independientes. No se modificaron metadatos globales ni se hicieron commits.

## Diseño y ciclos observados

`TaskState` conserva un único snapshot confirmado con su ETag y sustituye el badge fijo del detalle. `TaskHistory` maneja lectura, cursor y recuperación independiente. Cada confirmación de PUT o consulta deliberada de estado remonta el historial desde su primera página; la consulta inicial no duplica peticiones. La lista utiliza el estado real de cada DTO. El formulario de subtareas y las acciones del proyecto conservan sus reglas previas.

Cada fila corresponde a un comportamiento RED observado antes de su implementación mínima y posterior GREEN.

| Ciclo | RED observado → GREEN |
| --- | --- |
| 1 | Sin consulta de revisión → GET real antes de habilitar la acción. |
| 2 | Botón no bloqueado → PUT con ETag, espera visible y confirmación validada. |
| 3 | PUT 412/503 sin recuperación → consulta deliberada, sin reenvío automático. |
| 4 | GET 503 sin recuperación → Reintentar estado. |
| 5 | GET 401/404 conservaba detalle → retirada completa. |
| 6 | PUT 401/404 conservaba borrador → retirada completa del contexto privado. |
| 7 | PUT sin AbortSignal → cancelación al navegar; 401 tardío no invalida SessionGate. |
| 8 | Historial ausente → carga y vacío confirmado distintos. |
| 9 | Transiciones no representadas → lista y paginación con cursor opaco. |
| 10 | PUT no recargaba historia → recarga independiente y reintento sin PUT. |
| 11 | History 401/404 conservaba datos → retirada y reintento sin snapshot antiguo. |
| 12 | Sin fecha de finalización → fecha confirmada y retirada al reabrir. |
| 13 | Paginación perdía foco → encabezado local enfocable. |
| 14 | Recuperación de estado perdía foco → destino local, condicionado a body. |
| 15 | Lista mostraba Pendiente fijo → estado de cada tarea confirmada. |
| 16 | Fechas usaban zona implícita → UTC explícito con datetime original. |
| 17 | Error de página antigua atrapaba el cursor → vuelta a historia reciente. |
| 18 | GET deliberado tras 412 dejaba historia vacía antigua → recarga de historia al confirmar consulta. |

Regresiones añadidas que fueron verdes con las guardas existentes: StrictMode con GET antiguo no cooperativo y PUT posterior, seis combinaciones de solicitudes y navegación/cierre de sesión, CSRF con recuperación sin reenvío, respuestas inválidas, foco elegido durante espera, independencia del proyecto terminado y creación de hijo con padre completado. No se describen como nuevos ciclos RED.

La suite histórica split-task conserva componentes y SessionGate reales. Sus helpers responden a los nuevos endpoints; una aserción de Pendiente ahora espera su GET independiente. La primera corrida final detectó esa espera ausente (624 correctas y 1 fallo); se corrigió sólo la prueba. No se debilitó la cobertura de privacidad ni se sustituyó el transporte de sesión.

## Mapa del contrato

| Escenarios | Evidencia frontend |
| --- | --- |
| s1–s3 | Consulta previa, completar, reabrir, ETag y fecha en `task-state.test.tsx`; validación API independiente. |
| s4–s7 | No-op, precondiciones y cuerpo cerrado: contrato servidor y pruebas API. UI sólo envía intención explícita con ETag confirmado. |
| s8–s9 | Proyecto terminado no bloquea transición; lista usa status real; DTO8 ampliado por pruebas API. |
| s10–s12 | Vacío confirmado, transiciones y páginas; conservación completa y orden ante cambios externos se verifican además en backend/E2E. |
| s13–s14 | Cursor opaco validado por API; 401/404 en GET, PUT e historia retiran contexto. |
| s15 | SessionGate real, CSRF de memoria y recuperación sin PUT automático ni pérdida de borrador. |
| s16–s17 | Atomicidad/concurrencia: backend. UI trata 412 sin atribuir éxito. |
| s18 | Error de lectura diferenciado, recuperación de páginas y estado; API rechaza respuestas inválidas. |
| s19–s20 | Outbox y broker: backend e integración, no simulados por UI. |
| s21–s23 | Espera/doble envío, completar/reabrir, todos los resultados inciertos y consulta deliberada. |
| s24 | GET status, GET history y PUT pendientes durante navegación a otra tarea o logout; señal abortada y 401 tardío ignorado con SessionGate real. |
| s25 | PUT confirmado permanece tras fallo de historia; reintento sólo GET. Historia antigua rechazada no oculta la nueva. |
| s26 | Foco local y foco elegido en tests; estilos SCSS con controles 44 px. Matriz, zoom y motores son evidencia de integración pendiente. |
| s27 | Crear subtarea conserva padre completado, sin PUT de estado. |
| s28 | Orden de bloqueos: backend; UI mantiene solicitudes independientes y reglas existentes. |
| s29–s31 | Página, identificadores y no-store: API y backend; cliente transmite cursor sin interpretarlo. |
| s32–s33 | Validación de eventos y orden temporal: backend. |
| s34 | GET inválido, sin ETag o ETag ajeno no habilita cambios; reintento disponible. |
| s35 | Montaje real StrictMode: A abortado, B confirmado, PUT confirmado y respuesta A tardía no reemplaza snapshot. |
| s36 | GET status 401/404 retira detalle y datos privados. |

## Validación previa a mutación

Suite final: **625/625 pruebas, 17 archivos**, incluida composición nueva (44 casos). Build TypeScript/Vite verde sobre la fuente final. Lint final verde. Fuente y pruebas congeladas para revisión. No se repitió backend.

Perfil preparado `frontend/stryker.complete-reopen-task.config.json`: completos `task-state.tsx`, `task-history.tsx`, `task-status-api.ts`; rangos de lógica modificada `tasks-api.ts:87` y `:115`, `project-tasks.tsx:103–105`, `task-reader.tsx:107–111`. Umbral 80 y concurrency 2, sin exclusiones de lógica nueva. Los tres módulos nuevos también se incorporaron al perfil global. La puerta se aprobó después mediante el dictamen independiente; los resultados de la ejecución autorizada se detallan abajo.


## Refuerzos tras mutación y verificación final

Campaña original: 415 Killed + 1 Timeout /465 = 89,46 %, con 49 supervivientes, sin NoCoverage ni errores. Se conserva íntegra y se clasifica en `progress/mutation_complete_reopen_task_frontend.md`; la revisión independiente encontró 25 brechas, 16 equivalencias y 8 variantes permitidas.

Se añadieron 11 casos UI y 10 API que fueron verdes sobre la producción correcta; su RED está en la detección de mutantes concretos, no en un supuesto fallo de producción. Incluyen reintentos repetidos, refrescos posteriores, carga sin snapshot antiguo, falsas confirmaciones, respuestas viejas contra un contexto aún vivo, tipos JSON adversos y años expandidos válidos. Las pruebas de foco usan la misma simulación pública de body que las suites previas para suplir el desenfoque de disabled que JSDOM no reproduce.

La única corrección de presentación adicional fue el espacio UTC del estado, observado como `3:39UTC` en DOM real por integración. Se reforzaron las aserciones textuales existentes, sin añadir comportamiento de producto. Build/lint focal verdes y capturas rehechas sobre la fuente corregida.

**Verificación final independiente de raíz: 646/646 pruebas, 17 archivos, 7,75 s, EXIT 0.** No se repitió backend por cambios sólo de tests o espacio. Integración comunicó 43/43 E2E, 2/2 motores y zoom real verdes. Los límites físicos y UX están en su informe.

Replay principal: 83/89 = 93,26 %, EXIT 0 en 3 min 7 s; 23 brechas originales detectadas, sin timeout. Replay pequeño: 9/10, separaciones detectadas y timeout original reclasificado como brecha real. Tras un caso DTO8 con status active (7/7 compatibilidad focal), replay final: 8/8 Killed, incluido el original475. La suite global observada permanece 646; el caso adicional sólo se verificó focalmente. Fuente y pruebas liberadas al coordinador. No se suman estos porcentajes a la campaña completa ni se declara una nueva ejecución global.
