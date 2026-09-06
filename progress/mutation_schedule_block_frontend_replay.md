# Mutación — schedule_block frontend replay

**Estado:** CERRADO. PASS de umbral: Stryker89,83 %; bruto362/404 =89,60 %. Un RuntimeError separado.

Campaña independiente sobre checkpoint `56ced31`, aprobada en `progress/judge_schedule_block.md`. Init previo: backend1365/frontend1198/arnés9 verdes; E2E7 reales72ed46 y build391add verdes. Fuentes, pruebas y configuración congeladas.

Comando exacto por arnés: `node .harness/harness.mjs mutate schedule_block-frontend-replay`. Inicio Stryker 2026-09-06 12:52:51 Europe/Madrid, sesión94100, PID46508, salida inicial5d4b2b. Configuración: umbral80, concurrencia2, cobertura perTest. Instrumentación: cuatro archivos,404 mutantes. Los141 rangos incluyen167 identidades originales seleccionadas y una línea nueva; los mutantes adicionales generados por dichos rangos pertenecen al denominador propio de este replay.

Destinos: `frontend/reports/mutation-schedule-block/replay.json` y `replay.html`. Informe original preservado en `frontend/reports/mutation/mutation.json`. No se mezclan denominadores ni se atribuye Killed al RuntimeError945 sin evidencia. Al cerrar se cotejarán archivo, expresión fuente, operador y replacement con las ubicaciones remapeadas del manifest, conservando mayúsculas y regex.

Preparación: lectura del rol mutation_tester, política y puerta final. Una lectura intentó un nombre de configuración inexistente con guion; se localizó el nombre real con puntos sin modificar ningún archivo ni ejecutar otra mutación. No afecta al comando del arnés.

Baseline GREEN, salida512f49: a las12:54:23 finaliza ejecución inicial con786 pruebas,1m16s (net40,850s, overhead36,056s). No representa la regresión global1198 ni se mezcla con ella. Empieza medición de mutantes; score pendiente.


## Resultado final del replay

**Veredicto de umbral: PASS.** Salida finalca2c38, EXIT0, cierre2026-09-06 13:11:21 Europe/Madrid. Duración18m30s.

404 mutantes generados: **362 Killed,41 Survived,1 RuntimeError,0 Timeout,0 NoCoverage**. Stryker publica **89,83 % =362/403**, excluyendo su RuntimeError del denominador evaluable. El cociente bruto incluyendo los404 generados es **89,60 % =362/404**; ambos superan80. No se excluyeron equivalencias ni se convirtió el error en kill. El PASS del umbral no resuelve los huecos de cobertura ni el error del runner.

Por archivo: API234K/6S; TaskBlocks112K/33S/1RuntimeError; TaskReader14K/2S; TaskState2K. No se presenta como nueva puntuación global de la campaña original.

Cotejo independiente155a9f: **167/167 identidades seleccionadas emparejadas unívocamente**, sin duplicados, usando archivo, ubicación del rango remapeado, expresión fuente exacta, mutatorName y replacement exacto con igualdad sensible a mayúsculas (también regex). Transiciones:130 Survived a Killed,3 NoCoverage a Killed,33 Survived a Survived y1 RuntimeError a RuntimeError. La selección obtiene133K/33S/1error; no es el denominador de404 del replay. El mutante nuevo396, retirada de setTaskState(undefined) en TaskReader94, es Killed.

Los ocho supervivientes fuera de la selección167 fueron generados por los rangos: originales516/552/553/575/576/716 (API),886 y1108 (UI). Conservan su estado real. El mapa completo y el error literal están en `progress/mutation_schedule_block_frontend_replay_mapping.json`; incluye las167 identidades y todos los resultados no Killed.

SHA-256 original `4C95F22FEF3D0040E52303C96427351A11FEFAA4358F9D8F070944B1EC003AEC`, idéntico al manifest. SHA-256 replay `7C868917AC645A03BE78893CDF575EA7FF3ABAA8F3BD021FA2A3FBBD62B21059` (7284f9). Informes JSON/HTML finales conservados en reports/mutation-schedule-block, sin sobrescribir el original.

## Supervivientes: inventario y revisión necesaria

Las propuestas siguientes son oráculos para un autor posterior, no bugs de producción demostrados ni tests ya ejecutados. Se mantienen las equivalencias aprobadas en review_schedule_block_ui_mutation.md y review_schedule_block_frontend_mutation.md;744 se resolvió en review_schedule_block_replay_followup.md. No se reduce el denominador.

| Original / replay | Archivo:línea | Mutador y reemplazo exacto | Clasificación o comprobación pendiente |
| --- | --- | --- | --- |
| 516 / 130 | src/schedule-block-api.ts:385 | ConditionalExpression: <code>true</code> | Equivalencia contextual API ya revisada por root; conservar resultado S y denominador. |
| 552 / 138 | src/schedule-block-api.ts:397 | Regex: <code>/(?!0000)\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/</code> | Equivalencia contextual API ya revisada por root; conservar resultado S y denominador. |
| 553 / 139 | src/schedule-block-api.ts:397 | Regex: <code>/^(?!0000)\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/</code> | Equivalencia contextual API ya revisada por root; conservar resultado S y denominador. |
| 575 / 153 | src/schedule-block-api.ts:406 | ConditionalExpression: <code>true</code> | Equivalencia contextual API ya revisada por root; conservar resultado S y denominador. |
| 576 / 154 | src/schedule-block-api.ts:406 | EqualityOperator: <code>value.length &gt;= 0</code> | Equivalencia contextual API ya revisada por root; conservar resultado S y denominador. |
| 716 / 234 | src/schedule-block-api.ts:463 | Regex: <code>/\.\d*[1-9]\d*Z/</code> | Equivalencia contextual API ya revisada por root; conservar resultado S y denominador. |
| 744 / 246 | src/task-blocks.tsx:52 | ConditionalExpression: <code>false</code> | Equivalencia individual aceptada por root: parser puro y guarda746 posterior a await; no descontar. |
| 746 / 248 | src/task-blocks.tsx:54 | ConditionalExpression: <code>false</code> | Pendiente: diferir JSON de RESOURCE_NOT_FOUND, retirar hijos y reintentar dentro del mismo TaskReader; resolver clasificación antigua sin retirar contexto vigente. |
| 750 / 249 | src/task-blocks.tsx:55 | OptionalChaining: <code>problem.code</code> | Pendiente: error de listado no clasificable (problem null), comprobar recuperación visible sin rechazo asincrónico no manejado. |
| 757 / 250 | src/task-blocks.tsx:62 | UnaryOperator: <code>+1</code> | Pendiente: verificar orden de teclado del encabezado; +1 no debe añadir foco secuencial prioritario. |
| 814 / 256 | src/task-blocks.tsx:128 | ConditionalExpression: <code>true</code> | Pendiente: tarea pending y proyecto completed confirmado desde carga inicial, sin Planificar disponible. |
| 816 / 258 | src/task-blocks.tsx:128 | StringLiteral: <code>""</code> | Pendiente: tarea pending y proyecto completed confirmado desde carga inicial, sin Planificar disponible. |
| 829 / 264 | src/task-blocks.tsx:170 | StringLiteral: <code>"Stryker was here!"</code> | Revisar observabilidad del valor inicial de zona antes de configuración; DOM/payload vacío, sin inferir equivalencia por supervivencia. |
| 834 / 268 | src/task-blocks.tsx:177 | BooleanLiteral: <code>true</code> | Pendiente: configuración inicial retenida, sin alerta de fallo antes de resolver; sincronizar montaje vigente. |
| 886 / 270 | src/task-blocks.tsx:210 | ConditionalExpression: <code>true</code> | Equivalencia contextual aceptada en revisión UI; no descontar. |
| 887 / 271 | src/task-blocks.tsx:210 | LogicalOperator: <code>previous instanceof HTMLElement \|\| previous.isConnected</code> | Pendiente: fallback real cuando origen retirado ya no está conectado; el test nuevo no eliminó esta variante. |
| 894 / 272 | src/task-blocks.tsx:220 | ConditionalExpression: <code>true</code> | Pendiente: confirmación con foco deliberado fuera del editor y con body activo; observar destino correcto después de respuesta final. |
| 897 / 275 | src/task-blocks.tsx:220 | ConditionalExpression: <code>false</code> | Pendiente: confirmación con foco deliberado fuera del editor y con body activo; observar destino correcto después de respuesta final. |
| 898 / 276 | src/task-blocks.tsx:220 | EqualityOperator: <code>document.activeElement !== document.body</code> | Pendiente: confirmación con foco deliberado fuera del editor y con body activo; observar destino correcto después de respuesta final. |
| 931 / 281 | src/task-blocks.tsx:253 | ConditionalExpression: <code>true</code> | Pendiente: check con JSON de éxito diferido, cerrar/reabrir editor en mismo TaskBlocks y conservar nueva intención al resolver el check antiguo. |
| 996 / 290 | src/task-blocks.tsx:303 | BooleanLiteral: <code>true</code> | Pendiente: rechazo CSRF seguido de reenvío incierto; no conservar mensaje/estado CSRF de la petición anterior. |
| 1070 / 298 | src/task-blocks.tsx:371 | ConditionalExpression: <code>true</code> | Pendiente: previewA abortado, previewB pendiente; resolver A y comprobar que no termina espera ni permite previewC. |
| 1087 / 305 | src/task-blocks.tsx:387 | ConditionalExpression: <code>true</code> | Pendiente: fallo de configuración antiguo tras limpieza del efecto; distinguir cambios visibles del componente vigente o justificar setter local. |
| 1108 / 311 | src/task-blocks.tsx:411 | ArithmeticOperator: <code>value - 1</code> | Contador de invalidación, equivalencia contextual aceptada; no descontar. |
| 1124 / 312 | src/task-blocks.tsx:454 | CallExpression: <code>;</code> | Pendiente: elegir startOffset, cambiar startLocal y observar startOffset null en siguiente payload. |
| 1143 / 314 | src/task-blocks.tsx:483 | ObjectLiteral: <code>{}</code> | Pendiente: cambiar fin conservando selector de inicio conectado al DOM; no basta consultar referencia de elemento retirado. |
| 1154 / 315 | src/task-blocks.tsx:506 | CallExpression: <code>;</code> | Pendiente: cambiar zona/ocurrencia después de revisión y aceptación; no guardar revisión anterior ni conservar consentimiento. |
| 1157 / 317 | src/task-blocks.tsx:509 | CallExpression: <code>;</code> | Pendiente: elegir endOffset no nulo, cambiar zona y comprobar null en payload siguiente. |
| 1184 / 324 | src/task-blocks.tsx:541 | StringLiteral: <code>"Stryker was here!"</code> | Revisar selección vacía de ocurrencia, opción/payload y diferencias observables del fallback; no aceptar equivalencia automáticamente. |
| 1186 / 325 | src/task-blocks.tsx:544 | CallExpression: <code>;</code> | Pendiente: cambiar zona/ocurrencia después de revisión y aceptación; no guardar revisión anterior ni conservar consentimiento. |
| 1241 / 343 | src/task-blocks.tsx:600 | StringLiteral: <code>""</code> | Pendiente: texto accesible completo con separación entre etiqueta/cantidad/unidad; los matchers normalizados pueden ocultar pérdida del espacio. |
| 1248 / 346 | src/task-blocks.tsx:617 | ConditionalExpression: <code>true</code> | Pendiente: esperar revisión sin exceso definitivamente resuelta antes de comprobar ausencia de checkbox, incluida frontera cero. |
| 1267 / 350 | src/task-blocks.tsx:641 | ArrowFunction: <code>() =&gt; undefined</code> | Pendiente: BUDGET_EXCEEDED muestra todas las cifras de días, no sólo el aviso general. |
| 1250 / 348 | src/task-blocks.tsx:617 | EqualityOperator: <code>day.excessSeconds &gt;= 0</code> | Pendiente: esperar revisión sin exceso definitivamente resuelta antes de comprobar ausencia de checkbox, incluida frontera cero. |
| 1268 / 351 | src/task-blocks.tsx:643 | StringLiteral: <code>""</code> | Pendiente: texto accesible completo con separación entre etiqueta/cantidad/unidad; los matchers normalizados pueden ocultar pérdida del espacio. |
| 1269 / 352 | src/task-blocks.tsx:644 | StringLiteral: <code>""</code> | Pendiente: texto accesible completo con separación entre etiqueta/cantidad/unidad; los matchers normalizados pueden ocultar pérdida del espacio. |
| 1273 / 353 | src/task-blocks.tsx:650 | ConditionalExpression: <code>true</code> | Pendiente: revisión/errores ajenos a disponibilidad no muestran enlace ni advertencia de configurar. |
| 1303 / 367 | src/task-blocks.tsx:716 | ArrowFunction: <code>() =&gt; undefined</code> | Pendiente: consulta de conflicto diferida, cerrar/reabrir editor, respuesta401 antigua no revoca acceso ni dispara nueva sesión. |
| 1304 / 368 | src/task-blocks.tsx:716 | ArrowFunction: <code>() =&gt; undefined</code> | Pendiente: consulta de conflicto diferida, cerrar/reabrir editor, respuesta401 antigua no revoca acceso ni dispara nueva sesión. |
| 1401 / 386 | src/task-reader.tsx:57 | ConditionalExpression: <code>true</code> | Pendiente: respuesta de proyecto antigua tras retry alcanza callback del padre vivo; observar snapshot/estado actual después de liberar JSON, no sólo fetch. |
| 1403 / 388 | src/task-reader.tsx:57 | LogicalOperator: <code>!controller.signal.aborted \|\| "project" in result</code> | Pendiente: respuesta de proyecto antigua tras retry alcanza callback del padre vivo; observar snapshot/estado actual después de liberar JSON, no sólo fetch. |

## Error conservado separado

Original945 / replay286, src/task-blocks.tsx:259, OptionalChaining: `problem?.code` sustituido por `problem.code`. **RuntimeError**, no Killed. El runner informó dos intentos internos de reinicio; este agente no lanzó un retry manual ni intervino en el runtime. Texto literal de statusReason:

```text
Test runner crashed. Tried twice to restart it without any luck. Last time the error message was: Error: TypeError: Cannot convert object to primitive value
TypeError: Cannot convert object to primitive value
    at String (<anonymous>)
    at errorToString (file:///C:/Users/vhurt/Documents/Codex/2026-09-05/a-ver-necesito-o-me-gustar/work/OrganizacionWeb/frontend/node_modules/.pnpm/@stryker-mutator+util@10.0.0/node_modules/@stryker-mutator/util/dist/src/errors.js:22:12)
    at Array.map (<anonymous>)
    at VitestTestRunner.run (file:///C:/Users/vhurt/Documents/Codex/2026-09-05/a-ver-necesito-o-me-gustar/work/OrganizacionWeb/frontend/node_modules/.pnpm/@stryker-mutator+vitest-run_ad682ca84ee1a65181c9af1140987023/node_modules/@stryker-mutator/vitest-runner/dist/src/vitest-test-runner.js:182:18)
    at VitestTestRunner.mutantRun (file:///C:/Users/vhurt/Documents/Codex/2026-09-05/a-ver-necesito-o-me-gustar/work/OrganizacionWeb/frontend/node_modules/.pnpm/@stryker-mutator+vitest-run_ad682ca84ee1a65181c9af1140987023/node_modules/@stryker-mutator/vitest-runner/dist/src/vitest-test-runner.js:129:30)
    at ChildProcessTestRunnerWorker.mutantRun (file:///C:/Users/vhurt/Documents/Codex/2026-09-05/a-ver-necesito-o-me-gustar/work/OrganizacionWeb/frontend/node_modules/.pnpm/@stryker-mutator+core@10.0.0_@types+node@26.4.1/node_modules/@stryker-mutator/core/dist/src/test-runner/child-process-test-runner-worker.js:38:24)
    at ChildProcessProxyWorker.handleCall (file:///C:/Users/vhurt/Documents/Codex/2026-09-05/a-ver-necesito-o-me-gustar/work/OrganizacionWeb/frontend/node_modules/.pnpm/@stryker-mutator+core@10.0.0_@types+node@26.4.1/node_modules/@stryker-mutator/core/dist/src/child-proxy/child-process-proxy-worker.js:87:28)
Error: TypeError: Cannot convert object to primitive value
TypeError: Cannot convert object to primitive value
    at String (<anonymous>)
    at errorToString (file:///C:/Users/vhurt/Documents/Codex/2026-09-05/a-ver-necesito-o-me-gustar/work/OrganizacionWeb/frontend/node_modules/.pnpm/@stryker-mutator+util@10.0.0/node_modules/@stryker-mutator/util/dist/src/errors.js:22:12)
    at Array.map (<anonymous>)
    at VitestTestRunner.run (file:///C:/Users/vhurt/Documents/Codex/2026-09-05/a-ver-necesito-o-me-gustar/work/OrganizacionWeb/frontend/node_modules/.pnpm/@stryker-mutator+vitest-run_ad682ca84ee1a65181c9af1140987023/node_modules/@stryker-mutator/vitest-runner/dist/src/vitest-test-runner.js:182:18)
    at VitestTestRunner.mutantRun (file:///C:/Users/vhurt/Documents/Codex/2026-09-05/a-ver-necesito-o-me-gustar/work/OrganizacionWeb/frontend/node_modules/.pnpm/@stryker-mutator+vitest-run_ad682ca84ee1a65181c9af1140987023/node_modules/@stryker-mutator/vitest-runner/dist/src/vitest-test-runner.js:129:30)
    at ChildProcessTestRunnerWorker.mutantRun (file:///C:/Users/vhurt/Documents/Codex/2026-09-05/a-ver-necesito-o-me-gustar/work/OrganizacionWeb/frontend/node_modules/.pnpm/@stryker-mutator+core@10.0.0_@types+node@26.4.1/node_modules/@stryker-mutator/core/dist/src/test-runner/child-process-test-runner-worker.js:38:24)
    at ChildProcessProxyWorker.handleCall (file:///C:/Users/vhurt/Documents/Codex/2026-09-05/a-ver-necesito-o-me-gustar/work/OrganizacionWeb/frontend/node_modules/.pnpm/@stryker-mutator+core@10.0.0_@types+node@26.4.1/node_modules/@stryker-mutator/core/dist/src/child-proxy/child-process-proxy-worker.js:87:28)
    at ChildProcess.<anonymous> (file:///C:/Users/vhurt/Documents/Codex/2026-09-05/a-ver-necesito-o-me-gustar/work/OrganizacionWeb/frontend/node_modules/.pnpm/@stryker-mutator+core@10.0.0_@types+node@26.4.1/node_modules/@stryker-mutator/core/dist/src/child-proxy/child-process-proxy.js:123:33)
    at ChildProcess.emit (node:events:518:28)
    at emit (node:internal/child_process:949:14)
    at process.processTicksAndRejections (node:internal/process/task_queues:91:21)
```

No se ejecutaron pruebas adicionales, cambios productivos, cambios de tests/configuración ni limpieza tras medir. Feature continúa in_progress. Los refuerzos inicialmente verdes no garantizan por sí solos eliminar los mutantes: este replay demuestra133 resultados Killed seleccionados y conserva los33 supervivientes seleccionados para revisión posterior.

Verificación final readonly cb1c09: los cuatro hashes fuente coinciden con el manifest y las cuatro fuentes embebidas del reporte coinciden exactamente con los archivos medidos. Sin cambios durante la campaña. diff --check de ambos artefactos de evidencia limpio205cd2.

Verificación del coordinador067ac3: recuento independiente del JSON362 Killed/41 Survived/1 RuntimeError y167 correspondencias únicas por archivo, id de replay, operador, replacement sensible a mayúsculas, ubicación y estado. Continúa revisión conductual; este PASS no se presenta como cierre de feature.

## Dictamen readonly sobre RuntimeError945 / replay286

El error no invalida el cálculo del umbral: incluso contando945 como no eliminado,362/404 =89,60 % supera80. Sin embargo, no acredita que945 sea Killed ni equivalente. Su resultado sigue siendo fallo del adaptador al serializar un error (`errorToString`, `Cannot convert object to primitive value`); el informe no permite inferir el resultado que habría emitido un runner funcional. La sustitución de `problem?.code` por `problem.code` cambia el comportamiento ante null y no tiene aquí una justificación de equivalencia.

Evidencia funcional existente: `frontend/src/task-blocks.test.tsx:2323`, test @s48 «comprobar guardado bloquea acciones durante espera y fallo desconocido no autoriza reenvío», rechaza la consulta con TypeError. Comprueba espera bloqueada, comprobación disponible después, ausencia de reenvío, mensaje de incertidumbre y objetivo todavía bloqueado. Pasó inicialmente en c2583f y forma parte del corte180/180 y del init1198 previo. Acredita esa conducta de producción; no compara de nuevo todos los campos ni la key después de ese rechazo y no ejecuta una segunda consulta en esa misma secuencia.

La matriz @s45/@s46 «conserva intención incierta %s y confirma sólo consulta válida» (`frontend/src/task-blocks.test.tsx:1113`) complementa con valor exacto del objetivo, consulta por la misma key y un solo POST para503/network/respuesta inválida de creación. Son secuencias distintas: no deben presentarse como una demostración de identidad completa después del fallo de comprobación del test @s48, ni como prueba del kill945.

`docs/mutation-testing.md` exige umbral y separación entre medir y corregir; `docs/verification.md` exige justificar o eliminar supervivientes y no cerrar con supervivientes sin justificar. Ninguno define expresamente una política de cero RuntimeError ni autoriza convertirlos en éxitos. La documentación honesta para una eventual decisión final del juez es: **umbral superado con un caso de mutación no evaluado por fallo del adaptador, conservado como no eliminado en el score bruto, conducta de producción cubierta hasta los límites anteriores**. Esto puede registrarse como limitación técnica explícita; no es una autorización automática de done ni elimina la obligación de resolver los restantes supervivientes. El juez debe distinguir esa aceptación de limitación del cierre de las demás puertas. No se ejecutó mutación, prueba manual del mutante ni workaround; no se modificaron runtime, dependencias, configuración, fuentes o tests en esta revisión.
