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
