# TDD historial de replanificación

Ownership exclusivo frontend/src/reschedule-history.tsx y su test; API/shared inmutables. Contrato13 aprobado. Ponytail full/Caveman lite. Focales Vitest sólo este archivo; no backend, suites globales, configuración ni commits.

## Ciclo1 — @s16/@s39, carga y vacío independientes

RED12361a import ausente. Componente carga historial por contexto al montarse, mantiene loading hasta respuesta válida y representa vacío. GREEN659bb2:1test; fetch real del cliente, no mocks de API ni datos activos necesarios.

## Ciclos2/3 — @s16/@s36/@s39, hechos y paginación

Presentación con movimiento+cancelación reales para cliente: RED05421c lista ausente, GREEN126af5:2tests; before/after/duración/fechaUTC y etiqueta histórica, un GET total. Navegación: RED58a964 botón ausente, GREEN68efb0:3tests; cursor, sustitución de página y regreso a recientes, sin concatenar filas obsoletas.

## Ciclos4/5 — @s18/@s25/@s37/@s39, errores y generación

Error503: REDc7f7c7 alert ausente y rechazo no manejado; catch/reintento explícito, GREEN7d0923:4tests. Refresh/ruta3variantes: RED8b39a8 (refresh no abortaba y rutas reutilizaban cursor ajeno), wrapper con key por proyecto/tarea/refreshToken reinicia instancia; GREENd65159:7tests.401 antiguo tras reemplazo no alcanza observer ni callback, nueva lectura empieza sin cursor.

## Ciclos6–8 — @s18/@s36/@s39, acceso y estado vigente

Clasificación historial401/RESOURCE404 vs recibo404:2RED21d45b,1variante inicialmenteGREEN; guarda tras readChangeError y callback específico, GREEN44ab4b:10tests. Estado explícito: RED52363f control ausente, GREENd8f3ca:11tests; estado cancelled no sustituye el movimiento histórico ni genera GETporfila. Error de estado después de consulta correcta: RED501c50 alert ausente/rechazo no manejado; retirada del dato vigente+error separado/reintento, GREENc16600:12tests.

## Ciclos9–11 — @s18/@s38, consulta y foco

Acceso en estado explícito:2REDdb01f5 y ausencia bloque inicialmenteGREEN; callback con clasificación y guardas, GREENe2a203:15tests. Doble activación pendiente: RED7e798a generaba3GET en vez de2; guarda síncrona del controlador y aria-disabled conserva foco, GREEN4b6f0b:16tests. Paginación/foco2variantes: RED221034 dejaba BODY, control externo inicialmenteGREEN; recuperación al encabezado sólo sin otro foco visible, GREEN9d2190:18tests.

## Ciclos12–14 — @s37, cobertura inicialmente verde de respuestas obsoletas

Paginación con401 de consulta de fila entregado en el mismo asyncact: inicialmenteGREEN3b845f, cleanup existente evita observer; no se atribuye RED ni se añade código por ese caso. Clasificación de404 con cuerpo ReadableStream pendiente tras refresh:2 inicialmenteGREEN7fb461 (lista/estado), sin callback ni alert obsoletos. Éxito tardío tras cambio de tarea:2 inicialmenteGREEN4afedd (lista/estado), no restaura objetivo ni estado previo. Las pruebas usan fetch/Response reales del cliente y controles públicos, sin modificar API ni refs privadas.

## Corte para integración y revisión

Firma exportada BlockChangeHistory({projectId,taskId,onAccessFailure,refreshToken?}); el padre monta al abrir «Cambios de bloques». refreshToken reinicia en página reciente. Se reutilizan BlockDetails/BlockTime, task-history y task-list. No GETporfila: cada recibo ya es completo; sólo consultar explícitamente el estado actual genera GET adicional. No POST ni datos de trabajo realizado. Recibo histórico permanece visible ante fallo de estado.401/RESOURCE404 se notifican al padre; otros errores de lectura permanecen locales.

Prettier focal835c38, regresión completa del archivo23/23GREEN40bba6, diff-check4132b9. ESLint focal pendiente de cerrar sesión31054 al escribir este corte; no se ejecutó tsc global, suite global, mutación ni E2E. Evidencia responsive/axe/motores/zoom pertenece a integración UX global pendiente, no queda certificada por estos tests DOM.

Mapa: @s16 paginación/vacío/presentación; @s18 errores de lista/estado y propiedad; @s25 recuperación de hechos persistidos representados por API (reinicio real queda E2E); @s36 recibo histórico/estado vigente/error separado; @s37 refresh/ruta/aborto/401/clasificación/respuesta tardía; @s38 foco de consulta y paginación; @s39 historial independiente de activos y reintento. @s17 parsing de cursor y DTOs lo verifica cliente aprobado, no se replica su parser. @s40 sólo controles semánticos/reutilizaciónCSS aquí; revisión UX completa pendiente.

FREEZE final: ESLint focal cfc683 EXIT0, sesión31054 cerrada. Sólo dos archivos nuevos de frontend y esta bitácora cambiados por esta subtarea; no más ediciones.23testsGREEN40bba6 y formato835c38 vigentes. Entregado a autor de TaskBlocks y coordinador para integración/revisión independiente; no done ni commits.

## Ciclo15 — corrección de revisión independiente: foco durante loading

Reviewer detectó que el guard(page||failure) aplazaba recuperar foco hasta respuesta, dejando BODY mientras GET seguía pendiente. Nuevo test parametrizado@s38 comprueba Más cambios anteriores y Reintentar cambios con GET diferido, foco en encabezado inmediatamente después del click y conservación de otro control elegido durante espera.2RED949ac9: BODY en ambos casos. Se retiró únicamente ese guard del useLayoutEffect; permanecen interacted y comprobación de focoBODY. GREEN44d57d:25/25, incluidos23previos. Prettier focal9ddcfe, ESLint focal25fd8d y diff-check663646 EXIT0. La salidaRED incluye mensaje secundario de pnpm tras Vitest completo; no oculta el resultado real de2asserts fallidos.

FREEZE renovado tras este único fix. No API, TaskBlocks, SCSS ni otros archivos de producto tocados. Firma intacta; se solicita re-review independiente del delta. No nuevas funcionalidades ni suites globales.
