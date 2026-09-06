# TDD frontend: planificar bloque

Contrato aprobado a84e42f, 62 escenarios/325 casos. Baseline compartido 96222 autorizado por raíz; no se repite init durante escrituras paralelas. Ponytail full y Caveman lite aplicadas. API propiedad separada de schedule_api. Sin mutación antes de revisión.

## Ciclos observados

1. s56, compartir elegibilidad confirmada: RED por ausencia de Planificar bloque. Callback estable de TaskState a un snapshot separado en TaskReader; GREEN al completar/reabrir, con exactamente una lectura del proyecto. No se modifica el objeto task ni su dependencia de lectura.
2. s56, consulta deliberada después de conflicto: RED porque la planificación seguía disponible con estado incierto. Retirar snapshot compartido al fallar estado; GREEN al recuperar pending. La sección permanece montada.

Primer tramo todavía mínimo; editor/lista y demás escenarios siguen pendientes. No declara interfaz terminada ni aprobación UX global.

3. s56, proyecto completed: RED al permitir creación; GREEN pasando elegibilidad confirmada del proyecto sin desmontar sección.
4. s38, editor inline nativo: RED por campos ausentes; GREEN con objetivo, dos datetime-local, zona guardada y cero POST al abrir/editar. Se corrigió la ruta del fixture de preferencias, sin cambiar API.
5. s38/s54, configuración fallida: RED por rechazo no manejado; GREEN con alerta y reintento que conserva objetivo.
6. s39, preview: RED sin petición/espera; GREEN con intención exacta y revisión que distingue zonas antes de crear.
7. s42/s53, editar invalida preview: RED porque Guardar seguía habilitado; GREEN con cancelación del preview e ignorado de respuesta tardía.
8. s40/s54, red/503/DTO incompatible: tres RED por falta de recuperación; GREEN conservando campos, retirando preview y bloqueando Guardar.
9. s39/s43, exceso: RED sin consentimiento; GREEN mostrando días/presupuesto/segundos y aceptación no preseleccionada, invalidada al editar.
10. s44/s46, creación: RED sin envío; GREEN con key nativa, revisión y offsets retenidos, un solo POST y confirmación 201 validada por cliente.
11. s45/s46, resultado incierto red/503/DTO distinto: tres RED sin consulta recuperable; GREEN con intención bloqueada y GET por la misma key, sin otra creación automática.

Comandos focales: pnpm exec vitest run src/task-blocks.test.tsx -t <nombre del comportamiento>. Conteo actual: 15 casos UI (último grupo 3/3 verde). Todavía faltan errores tipados, cancelación de escritura, lista/paginación, DST y matriz UX; código no congelado.

## Reanudación comprobada (2026-09-06)

El corte anterior quedó interrumpido con documentación parcial: al retomarlo existían 44 casos UI, no 15. Se ejecutaron task-blocks, task-state y split-task: 149/151 verdes. Los dos fallos heredados eran los casos de foco @s58 de preview y creación, sin foco externo; no se borraron ni se atribuyeron como ciclos nuevos. Los casos de error asociado de fecha/zona y DST ya estaban implementados y verdes. También estaban presentes revisión del presupuesto concurrente, navegación a disponibilidad, pérdida de elegibilidad de tarea, cancelación de escritura, lista/paginación y UTC alternativo. Esta comprobación acredita su estado actual, no reconstruye evidencia RED no observada de la sesión anterior.

12. @s58: los dos RED heredados fallaban con foco en body. GREEN del grupo (4/4): restaurar origen habilitado, encabezado si sigue deshabilitado y conservar foco externo elegido.
13. @s49: RED nuevo, BLOCK_OVERLAP mantenía incertidumbre y faltaba rechazo definitivo. GREEN: conserva campos, retira revisión, deshabilita Guardar y permite corregir sin comprobación de creación.
14. @s52: RED nuevo, RESOURCE_NOT_FOUND del listado mantenía tarea/borrador. GREEN: clasificar error reconocido y retirar contexto con onAccessFailure; comprobar cancelación antes y después del parser asíncrono.
15. @s49: RED nuevo, faltaba consulta del bloque conflictivo. GREEN: consulta explícita mediante readBlock aprobado, incluso en otro proyecto/tarea propios, muestra objetivo/instantes y conserva el borrador.

Comandos focales ejecutados para cada ciclo: `pnpm exec vitest run src/task-blocks.test.tsx -t <nombre>`. Suite UI tras estos ciclos: 47/47 verde. Formato aplicado sólo a archivos frontend asignados. Todavía pendientes foco de confirmación/recuperación, sesión real, transición de proyecto completed, estilos y matriz UX. Sin mutación, freeze ni done.

16. @s58: RED al desaparecer Guardar tras 201, quedaba body con foco. GREEN: enfocar encabezado antes de desmontar editor sólo si el usuario no eligió otro control.
17. @s58: RED equivalente en confirmación by-request porque check no retenía origen. GREEN: registrar el control que inicia la consulta y reutilizar confirmación/foco.
18. @s52/@s56: comprobaciones de integración de componentes con SessionGate y cambio real de estado mediante ProjectStatusControl. Los primeros fallos se debían a fixtures incompletos: sesión anónima sin username:null y endpoint/método de estado incorrectos. Corregidos fixtures conforme a clientes existentes; ambos casos verdes sin cambiar producción. No se presentan como RED de comportamiento. Se demuestra 401 durante recuperación, retirada de lista/intención privada y proyecto completed conservando recuperación con una sola lectura inicial del proyecto y una sola creación.
19. @s52: RED, reconsultar conflicto con RESOURCE_NOT_FOUND conservaba detalles privados anteriores. GREEN: retirar detalle al iniciar reconsulta; error visible y reintento explícito.
20. @s59: navegador Chromium con HTTP interceptado, script `.e2e-work/schedule-block-frontend-ui.mjs`: RED, selector de zona medía 146×19 px. Tras min-height, siguiente comprobación descubrió aceptación de 42 px de alto en ancho grande. GREEN con selector de 48 px y etiqueta de aceptación flex de al menos 44 px. Refactor visual en verde: reutilizar agrupación y estilos de tareas, fieldset sin borde/min-width intrínseco, tamaños nativos de 16 px, avisos y grupos de revisión. 23 anchos de 320 a 2560 px (incluidos ambos lados de 420/600/700/1000 existentes), controles ≥44×44, sin overflow y axe 0. Capturas de revisión 320/1440 y confirmación 320 inspeccionadas. Esta evidencia usa respuestas HTTP simuladas; no acredita backend ni persistencia.

Última regresión focal: task-blocks (52), task-state y split-task, 159/159 verdes. `pnpm exec tsc --noEmit` verde. Los targets schedule-block-api y task-blocks se incorporan a la configuración de Stryker, sin ejecutar mutación ni cambiar su umbral. Revisión formal, E2E con API/PG, más estados visuales y motores siguen pendientes.

Regresión frontend completa al estabilizar este corte: `pnpm exec vitest run`, 1117/1117, 21 archivos, EXIT 0 (09:48:48). ESLint focal y Prettier de archivos asignados verdes. Matriz preliminar de treinta filas guardada en `progress/ux_schedule_block_frontend.md`; límites explícitos. Coordinador revisó 52/52 y cerró los tres P2 en su informe incremental. No constituye freeze ni permiso de mutación.

21. @s50: comprobación de comportamiento existente, verde en primera ejecución sin cambio de producción. IDEMPOTENCY_CONFLICT conserva key/campos bloqueados, no ofrece reenvío ni genera otra intención y permite confirmar con GET por la misma key y una sola creación. Conteo actual de UI: 53; el resultado completo de 1117 pertenece al corte anterior de 52, no se extrapola.

## Mapa UI del corte

Todos los nombres siguientes pertenecen a `frontend/src/task-blocks.test.tsx`. Cada referencia acredita los casos ejecutados, no todos los Examples del contrato salvo que se indique.

- @s25/@s26: `consulta bloques persistidos y pagina sin tocar el borrador` (presentación/paginación cliente; orden y 21 filas reales pendientes E2E).
- @s38: `abre editor nativo con zona guardada sin enviar ni guardar automáticamente`; `recupera configuración fallida sin perder objetivo`.
- @s39: `revisa intención antes de habilitar creación y distingue ambas zonas` y `exceso muestra presupuesto y exige aceptación específica sin preselección`.
- @s40: `preview fallido %s conserva borrador y retira revisión anterior` (red, 503, DTO inválido).
- @s41: `las ocurrencias DST se eligen por extremo y se retiran al cambiar su fecha`.
- @s42: `editar invalida revisión vigente y respuesta pendiente`, más el caso DST; no se afirma cada fila del outline como caso independiente.
- @s43: `exceso muestra presupuesto y exige aceptación específica sin preselección`.
- @s44: `confirma sólo creación persistida con key y revisión, bloqueando doble envío`, más cancelación durante creación.
- @s45/@s46: `conserva intención incierta %s y confirma sólo consulta válida`, creación confirmada y reenvío/replay. Red, 503 y DTO distinto comprobados en UI; más variantes de DTO en cliente API aprobado.
- @s47/@s48: `404 por key conserva bloqueo y permite reenviar exactamente el mismo bloque`.
- @s49: rechazos definitivos tipados, errores de campo, presupuesto concurrente, solape definitivo y consulta de conflicto.
- @s50: `IDEMPOTENCY_CONFLICT conserva key e intención bloqueadas y sólo consulta su resultado`.
- @s51: `recuperar CSRF conserva petición y exige reenvío manual con token renovado`, usando SessionGate.
- @s52: pérdida de contexto preview/create/check, listado RESOURCE_NOT_FOUND, SessionGate 401 durante recuperación y reconsulta del conflicto.
- @s53: edición invalida preview pendiente; cerrar creación y reabrir ignora respuesta antigua. Además `listado tardío de otra tarea no reemplaza el contexto navegado`, `recuperación tardía no confirma ni roba foco tras navegar a otra tarea` y `renovación CSRF tardía no restaura contexto revocado durante recuperación`: JSON retenido hasta después del cambio y señal abortada comprobada. Casos específicos cerrados sin cambiar producción; bitácora del ciclo al final.
- @s54: preview fallido, configuración fallida y fallo de lista conservando editor/creación confirmada.
- @s55: configuración requerida o zona indisponible ofrece enlace con descarte explicado.
- @s56: compartir estado confirmado sin otra lectura; recuperar incertidumbre de tarea; tarea/proyecto completed conservan intención enviada y recuperación.
- @s57: `conserva datetime y muestra UTC explícito si Intl no reconoce la zona guardada`.
- @s58: origen/body/exterior durante preview y fallo create; confirmación y check restauran encabezado. Navegación real Shift+Tab de aceptación a Guardar comprobada en script Chromium.
- @s59: errores asociados en controles, teclado/axe en tres motores con HTTP simulado; siete recorridos reales en tres motores. Límites en `ux_schedule_block_frontend.md`.
- @s60: matriz interceptada tres motores, 141 combinaciones por motor y zoom nativo Chromium setZoom2 verificados. Siete E2E reales en Chromium y14 en Firefox/WebKit. Dispositivos físicos y teclado virtual permanecen sin comprobar.
- @s61: treinta filas en `ux_schedule_block_frontend.md`; feedback local medido por debajo de400ms en tres motores y zoom nativo. Evaluación humana de comprensión/atención sigue pendiente; automatización no la sustituye.

## E2E real en preparación

Cesión explícita del coordinador: `e2e/schedule-block.spec.mjs` y adaptación de TRUNCATEs E2E antiguos. Primer test de creación, recarga, replay, by-request, detalle y una fila/outbox preparado; `playwright --list` y `node --check` verdes, ejecución real todavía pendiente de endpoints backend estables. Los ocho TRUNCATEs existentes incluyen planned_blocks explícitamente para conservar su aislamiento con V11; no se añadió CASCADE. No se ejecuta Docker hasta coordinar con autor backend. No se modificó el runner ni archivos bloqueados.

## QA ampliado sin dependencia del backend

Script interceptado ampliado y ejecutado en Chromium153, Firefox155 y WebKit26.6: cada motor pasó 141 combinaciones (seis estados en23anchos y texto duplicado en3), siete auditorías axe sin infracciones, teclado con Shift+Tab y Enter, altura400 a ancho768 y ausencia de animación con movimiento reducido. Feedback local:4,5/3/3ms manteniendo la respuesta retenida. Zoom nativo setZoom2 en perfil aislado produjo DPR1,5→3, inner1426→713 y ventana654 con inner320/client312/scroll312: siete estados verdes, axe0 y9,5ms. Captura corregida mediante patrón CDP previo porque screenshot directo de Playwright recortaba a zoom nativo; evidencia final completa inspeccionada. Detalles y límites actualizados en `ux_schedule_block_frontend.md`. Esto resuelve las comprobaciones técnicas interceptadas de @s59/@s60/@s61 y no acredita API/PG, dispositivos físicos ni evaluación humana.

Coordinador ejecutó regresión independiente final: **1118/1118 PASS, EXIT0**, salidadbe46b,47,41s. Su lint inicial falló sólo por formato de src/split-task.test.tsx y src/task-state.test.tsx (72cb0c). Se aplicó Prettier exclusivamente a esos dos archivos (544fbe), después **pnpm lint PASS, EXIT0**, salidae4c6f0. Sin repetición de tests por cambio sólo de formato; no formato global ni destinos protegidos.

## Cierre puntual @s53 y estado vigente

Se añadieron una a una tres caracterizaciones sobre guardas existentes, sin tocar producción. `listado tardío de otra tarea no reemplaza el contexto navegado`: primera ejecución1PASS/53skipped,9e2170. `recuperación tardía no confirma ni roba foco tras navegar a otra tarea`: primera ejecución1PASS/54skipped,224ff5. Ambas retienen response.json hasta después de navegar a otra tarea; comprueban aborto y que datos/foco nuevos no cambian al resolver.

`renovación CSRF tardía no restaura contexto revocado durante recuperación`: primer intento de fixture falló1e8311 porque CSRF_INVALID ofrece reenvío pero no Comprobar guardado; no fue defecto de producción. Se corrigió el disparador de revocación a PUTestado401 con Completar tarea mientras GETsesión JSON seguía pendiente. Nueva ejecución1PASS/55skipped,5d7ff2: respuesta autenticada antigua no remonta la UI privada después de login requerido. Complementa los tests existentes de useSession cleanup y no añade variantes DTO redundantes.

Regresión focal final **319/319 PASS, EXIT0**,1e2072:56UI,73autenticación,190clienteAPI. Lint pasó27d65d. TypeScript detectó una opción exact de Playwright copiada erróneamente al matcher ByRoleOptions del test; se retiró (no necesaria para nombre string) y **pnpm build PASS, EXIT0**,b96beb, con tsc y Vite incluidos. Sin cambio de bundle respecto al corte previo. La regresión total1118 del coordinador precede estos tres tests; no se presenta como1121 ejecutados. Mapa@s53/59–61 actualizado para separar evidencia vigente de pendientes históricos. No mutación ni commits.

Corrección de orden de verificación final: el lint27d65d ocurrió antes de retirar exact del matcher; después sólo se ejecutó buildb96beb. Init global del coordinador3534 detectó por ello Prettier pendiente en task-blocks.test.tsx (bddd1f). Se aplicó formato exclusivamente a ese archivo (b7b291), sin cambios funcionales, y luego **pnpm lint PASS, EXIT0**,9c0a7e. No se repitieron suites ni se ejecutó mutación; coordinador mantiene init global en curso y repetirá su puerta tras este formato. Freeze restaurado al finalizar este ajuste.

Protección preventiva solicitada por coordinador antes de futura mutación: stryker.config.json añade sólo ignorePatterns [".stryker-tmp-availability-replay"], conservando targets/thresholds/tests. ProjectReader instalado incorpora ignorePatterns y filtra directorios antes de recursión (líneas218–260). Validación sintética con Minimatch instalado y las mismas opciones/predicado: PASS8e95f0; directorio protegido excluido y src incluido, sin enumerar directorios reales. Intentos previos de importar ProjectReader directamente fallaron por inicialización circular de la librería; resolución del symlink fue necesaria para cargar su dependencia con pnpm. No fueron ejecuciones Stryker ni acceso al destino protegido. Prettier específico config sin cambios adicionales447c40.

## Refuerzo posterior a mutación — autor UI/shared

Rol tdd_craftsman reactivado por root después de clasificación original. APItests tienen autor independiente. Sin Stryker/replay antes de nuevo judge.

- @s56 `reintentar tarea espera su estado nuevo antes de permitir planificar`: primer fallo90a2c5 fue selector inexistente Completar proyecto; corregido por encabezado Estado del proyecto, sin producción. RED real9d5579: después de listado RESOURCE_NOT_FOUND y retry, GET proyecto confirma antes que GET estado; aparece Planificar con estado antiguo. Corrección mínima TaskReader: setTaskState(undefined) junto a setTask(undefined) en retry. GREEN56ed8e,1/1 focal. Producción sólo tras RED real.
- @s53 `una transición vieja no cambia la elegibilidad tras reintentar tarea`: verde inicial e2ed4f,1/1. PUT viejo resuelve completed después de listado404, retry y confirmación nueva pending. Conserva Planificar/Completar tarea actuales. Refuerzo del hueco1530 por callback onSnapshot; no se declara killed sin replay posterior ni se fabricó RED de producción.

Quedan matrices de callbacks/recuperación/UX del inventario. No se declara cierre global por dos focales.

- @s53 proyecto anterior tras retry:118d33 GREEN inicial. Mantiene carga nueva y no incorpora proyecto completed viejo; cubre recorrido público de reader1401/1403/1427 reabiertos. Rechazo proyecto viejo404:6ce059 GREEN inicial, conserva tarea recuperada (1412). Sin producción extra ni replay.
- @s49/@s58 rechazo de reenvío y segunda intención:8874cc GREEN inicial. Foco vuelve a heading al desaparecer Reenviar; después de rechazo negocio, nueva intención incierta no permite reenvío sin404. Oráculos887/1016, sin declarar killed aún.
- @s41 varios días con sólo un exceso:9cb62e GREEN inicial. Consentimiento desmarcado, guardar bloqueado hasta aceptación; nueva revisión sin exceso retira checkbox y no inventa alertas. some/every y frontera cero.
- @s56 submit nativo: d177b7 GREEN inicial. Evento default cancelado, una sola preview durante espera, ninguna preview adicional tras completed y Guardar disabled. No se invocan handlers privados.

- @s56 reenvío retenido tras completed/404: fa3074 GREEN inicial, misma key/body y confirmación, cubre NoCoverage963.
- @s40 disponibilidad no configurada: fixture inicial incompleta produjo fallo cb895c, corregida con nulls+ETag contractual;65bab6 GREEN sin cambio de producción. Campos/payload vacíos y ninguna zona inventada; cubre NoCoverage1082.
- @s40 dos reintentos configuración:39d38f GREEN inicial, espera sin alerta y tercer GET confirmado; no se confunde primer retry con updater que queda undefined.
- @s42 ocurrencias:378e46 GREEN inicial, opciones UTC+01:00/UTC+00:00 (Z), aria-invalid, selección asimétrica, cambio de fin conserva inicio, cambio de zona limpia ambos offsets en payload. Fixture de respuesta estructural de UI; no se presenta como validación física del huso por backend. NoCoverage1200 cubierto sin llamarlo killed aún.
- @s39 fecha original Europe/Madrid: e3f808 GREEN inicial, fecha española/segundos/GMT+01:00 y datetime persistido; sin falso vacío/alerta.
- @s52 estados consulta conflicto:0c7734 GREEN inicial; espera, disabled, fallo y reintento sin alerta obsoleta, éxito.
- @s53 StrictMode:3b3e4a GREEN inicial; listado/configuración anteriores no reemplazan instancia vigente ni selección deliberada.
- @s48 comprobación desconocida: c2583f GREEN inicial; mientras espera disabled, después TypeError sigue incierto sin reenvío y conserva explicación/identidad. RuntimeError945 original sigue siendo error de runner hasta replay.
- @s56 reconsulta proyecto:69a33a GREEN inicial, snapshot previo no habilita Planificar durante nueva carga ni tras fallo. Refuerzo1465.
- @s54 paginación/confirmación desde página antigua: b1edc2 GREEN inicial; filas antiguas retiradas durante cada lectura, creación retorna a consulta reciente y cierra editor, sin falso error.

Formato específico a0d81c sólo task-blocks.test.tsx y task-reader.tsx (este último unchanged). Regresión final de tres archivos UI/shared: **180/180 PASS**, sesión12301,6ddfe4 EXIT0,15,99s. TaskBlocks73 (antes56, +17), resto estado/detalle107. ESLint focal16c42c EXIT0, tsc f37f93 EXIT0. API250 del otro autor no se mezcla con este conteo.

Fuentes/pruebas UI/shared congeladas después de estas verificaciones. Única producción nueva: task-reader.tsx94 setTaskState(undefined), respaldada RED9d5579→GREEN56ed8e. No más suites ni Stryker hasta revisión/orden del coordinador. Los refuerzos verdes iniciales son oráculos propuestos para replay selectivo; no acreditan por sí solos killed ni que todos los NE hayan desaparecido. Conservan inventario original y RuntimeError separado.

## Seguimiento acotado tras replay — cinco entrelazados públicos

Rol autor TDD, Ponytail full/Caveman lite; root libera freeze sólo para IDs746,931,1070,1303/1304,1401/1403. No se persiguen equivalencias ni100%. No se modifica producción, APItests, configuración, reports, harness ni rangos. Los cinco tests se añadieron y ejecutaron uno por uno; todos son **refuerzos inicialmente GREEN**, no RED de producción ni demostración de kills.

| IDs originales | Test en task-blocks.test.tsx | Primer resultado y oráculo |
| --- | --- | --- |
| 746 | @s53 clasificación de listado antiguo no retira TaskReader recuperado (2458) | GREEN8b8623. clone().json del404 de listado ya comenzó; history404 desmonta hijos, retry recupera mismo padre; liberar JSON antiguo no retira contexto/Planificar. |
| 931 | @s53 check antiguo no confirma ni cierra otro editor del mismo listado (2509) | GREEN8b25bf. JSON de check200 pendiente; cancelar y reabrir editor manteniendo TaskBlocks; liberar éxito antiguo conserva nueva intención y no confirma. |
| 1070 | @s53 finally de preview abortado no termina la revisión nueva pendiente (2549) | GREENde9079. JSON de A pendiente, edición aborta A, B ya espera; terminar A no retira Revisando ni habilita botón/submit C; B confirma120min. |
| 1303/1304 | @s53 conflicto retirado no revoca sesión al recibir401 tardío (2596) | GREEN98c4ca. SessionGate, GET conflicto pendiente, cancelar/reabrir;401 tardío no retira intención ni abre login ni consulta de nuevo sesión. Oráculo externo, no sólo signal.aborted. |
| 1401/1403 | @s53 JSON de proyecto antiguo no reemplaza proyecto ya confirmado tras retry (2639) | GREEN89d29c. JSON de proyecto anterior ya comenzó; listado404/retry, proyecto nuevo active ya confirmado; resolver completed viejo no sustituye snapshot ni deshabilita Planificar/Pausar. A diferencia del caso anterior, se observa después de ambas confirmaciones. |

Formato específico8680bc. Verificación final: **78/78 task-blocks PASS**, sesión54054 salida750008 EXIT0,15,15s; ESLint focal3a7fd0 EXIT0; tsc94a1ba EXIT0; git diff --check41e623 limpio. Diff de este tramo: un archivo de pruebas,221 inserciones. No se ejecutó mutación ni se reinterpretó RuntimeError945. Listo para revisión independiente; fuentes/pruebas vuelven a estar congeladas para ese corte.

## Último seguimiento agrupado de supervivientes observables

Autorización root: sólo task-blocks.test.tsx y bitácora; producción permanece56ced31. Se respetan EQ834/1087/1184 y no se añaden pruebas para ellas. Cada modificación de un flujo se ejecutó antes de continuar al siguiente; todos los refuerzos son **GREEN iniciales** y no se atribuyen kills antes de medición autorizada.

| Grupo / IDs | Refuerzo público y evidencia focal |
| --- | --- |
| Recuperación750/996 | Caso nuevo error desconocido del listado: alerta/retry y conservación editor, bc1cb8 GREEN. Flujo CSRF existente ahora exige ausencia de Comprobar guardado tras rechazo definitivo, cee8c6 GREEN; conserva reenvío manual/key/body anteriores. |
| Elegibilidad814/816/829 | Nuevo borrador revisado→proyecto completed confirma Revisar/Guardar disabled, submit sin nuevo preview y cero POST, a0e814 GREEN. Fixture existente de configuración pendiente rellena datos y revisa ANTES de resolver configuración: payload zoneId vacío aunque DOM muestre placeholder, b5d9a3 GREEN. |
| Teclado/foco757/887/894/897/898 | Matriz existente conserva preview/error y añade confirmación exitosa con body/foco externo,6/6 GREEN10ea17. Nuevo recorrido Tab mantiene skip-link primero y heading programático seguido de Planificar, a0057b GREEN. Flujo de reenvío existente usa ahora CSRF→recuperar acceso→reenviar→rechazo negocio:183da5 GREEN, foco heading y nueva intención incierta sin reenvío prematuro. |
| DST1124/1143/1154/1157/1186 | Fixture existente Madrid: cambiar inicio elegido envía startOffset:null, fe1b5d GREEN. Fixture de ocurrencias consulta selector de inicio desde DOM actual tras cambiar fin y vuelve a elegir endOffset=Z antes de cambiar zona:859a66 GREEN. Nuevo flujo con ambas ocurrencias y revisión aceptada: cambiar ocurrencia y luego zona retira review/consentimiento, exige aceptación nueva y envía offsets correctos/null:1ee5d6 GREEN. |
| Presentación1241/1248/1250/1267/1268/1269/1273 | Rechazo concurrente de presupuesto existente comprueba frase completa con todas cifras/espacios y ausencia de enlace de disponibilidad en problema ajeno; revisión comprueba raw textContent Inicio+espacio+fecha, e88af0 GREEN. Caso de varios días espera región final y ambas cifras Exceso0 antes de exigir ausencia de checkbox:bfba53 GREEN. Ya no observa intervalo transitorio sin review. |

### Recorrido887 confirmado, sin refs artificiales

No se acepta EQ: cuando uncertain=false y csrfRejected=true, el botón Reenviar está fuera del fieldset. Al reenviar, save pone csrfRejected=false y saving=true en el mismo evento. El wrapper de recuperación desaparece; el botón retirado no llega a actualizar disabled=true. El test comprueba que ese nodo de acción real queda desconectado y habilitado, sin modificar sus propiedades. Tras rechazo BUDGET_EXCEEDED, el foco debe ir al heading actual. La variante OR omitiría isConnected y puede intentar enfocar el nodo retirado. En el recorrido anterior procedente de incertidumbre el botón sí conservaba disabled=true; por eso no distinguía887.

### Corte final para juez

Formato específicoa4130d. **84/84 PASS**, sesión78601, salida fee3b5 EXIT0,14,27s. ESLint focalf0addb EXIT0; tsc69ba29 EXIT0; diff --checkd6ebde limpio. Conteo78→84: cuatro nuevos tests y dos filas nuevas en matriz existente, además de refuerzos en fixtures existentes. Se mantiene toda cobertura previa.

Los cinco grupos y sus22 IDs están atendidos por oráculos públicos; todavía no se afirma resultado de mutación. Sin bloqueos. No producción, APItests, config/harness, nuevos reports, E2E ni build repetidos. Sólo task-blocks.test.tsx y esta bitácora. Fuentes/pruebas congeladas otra vez para juez y puerta global del root.

Ajustes de revisión posteriores al84/84: root0ab894 detectó que el segundo payload de disponibilidad seguía tomando primer preview; se corrigió esperando exactamente2 peticiones y usando la última, conservando también la aserción previa a configuración. Focal e892db GREEN. Revisor10422a detectó que el cambio de1143 había reconsultado el selector antes de editar fin, pero aún conservaba referencia start después: la afirmación anterior de bitácora era prematura para ese punto. Ahora también después de editar fin se usa screen.getByLabelText del DOM vigente; focal4f8899 GREEN. Sin producción ni nuevos tests. Formato91cda6, ESLint54bccd y tscc13610 EXIT0 tras ambos ajustes. La regresión84/84 anterior se complementa con estos dos focales; root tomará init global del corte final. Freeze final para juez.

## Incidente init26470: aserción de cleanup en prueba antigua create-task

Root comunica init26470 rojo9e0c93:1208PASS/1FAIL, cancelación de la revisión GET en create-task.test.tsx. Focal original8d3143 PASS; no se demuestra bug productivo. Se inspeccionan useProjectTasks/useReadProjects/ProjectReader: revisar estado sólo activa reviewing; no retira por sí mismo la región Tareas. El índice3 correspondía al GET correcto, por lo que tampoco se atribuye a identidad de llamada equivocada.

Instrumentación temporal acotada con MutationObserver y listener de abort en el test observado: dos ejecuciones52083b/cb6262 pasaron pero el reporter no expuso console; se obtuvo el diagnóstico mediante fallo deliberado d1fcb9, NO un RED de producción. Secuencia observada: beforePause región=true y GETrevisión.signal=false; mutation región=true/signal=false; mutation región=false/signal=false; después evento abort. Esto demuestra ventana entre commit DOM y cleanup del useEffect, suficiente para que waitFor ausencia de región termine antes de la cancelación bajo carga global.

Instrumentación completamente retirada. Ajuste sólo de prueba: captura signal en mock de la revisión identificada por URL+GET; espera mensaje terminal404 y waitFor(signal.aborted===true), conservando retiro de región y comprobaciones posteriores a respuesta tardía (ni Tareas ni título privado reaparecen). No timeout aumentado ni aserción eliminada: la cancelación pasa a sincronizarse con su fase real. Producción intacta y resto de archivos congelados.

Focal ajustado364a22 PASS. Formato específico32bf5d. Suite create-task **55/55 PASS7c3c42**,5,19s; ESLint31b060 y tsc79b6ea EXIT0; diff --check9cb74d limpio. Corte congelado para revisor y nueva puerta global del root; no mutación ejecutada.
