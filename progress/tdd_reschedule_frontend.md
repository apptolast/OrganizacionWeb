# TDD frontend13: reschedule

Contrato d1ff6098a8117 aprobado; frontend único autor, backend independiente. Init78050 vigente según root, sin init concurrente. Ponytail full/Caveman lite. Ownership src frontend/tests y esta bitácora; no config/harness/E2E. Estado: en curso, no veredicto.

## Cliente: ciclos reales

1. @s1/@s4 leer state con ETag BIGINT exacto y petición privada única. REDd97118 import ausente, GREEN1f917e (1test). Mínimo cliente state.
2. @s31 matriz cerrada de estado/ETag incompatible. REDd09a84 aceptación de null, GREEN388061 (2tests). Reutiliza isBlock/exact/instant11 y revisión textual con límite BigInt. Matriz agrupada en un test, sin atribuir13ejecuciones individuales.

Siguientes: status HTTP/privacidad, preview, recibos y recuperación, historial; después UI/composición según design_reschedule_ui.md. Los escenarios de persistencia/eventos pertenecen al autor backend; todavía no hay cobertura completa ni UI13.
3. @s18 estados HTTP fallidos con cuerpo válido: RED12aa01, GREEN847012 (3tests).
4. @s7 preview de movimiento con If-Match y cinco campos: REDcf327a, GREENc7c156 (4tests).
5. @s31 rechazo de preview incompatible: REDfb8d39, GREEN7258a6 (5tests). Exporta isPreview11 sin cambiar su lógica.
6. @s4 ruta UUID mayúscula con respuesta canónica y BIGINT máximo: RED85edad, GREENb3aaf7 (6tests), más formato focal. Exporta/reutiliza sameId11; no relaja ETag canónico.
7. @s12/@s13 cancelación sin disponibilidad, reloj anterior permitido: REDbd2104, GREEN78eb4f (7tests).
8. @s32 esquema/identidad/revisión de recibo cancelado y Location POST: REDe41681, GREENcfa34f (8tests).
9. @s11/@s15 movimiento y replay POST200 con headers exactos: RED262b57, GREEN271638 (9tests).
10. @s29/@s32 preview no coincide con intención retenida: REDaebbdf, GREEN6a3a02 (10tests), rechaza antes de transmitir.
11. @s25/@s34 GET recibo por key sin exigir Location: RED21a34a, GREENeaf272 (11tests). Recuperación compara intención, conserva código HTTP; no consulta detalle ni estado actual en lugar del recibo.
12. @s37 copia estable de intención entre awaits POST/check: RED3ed624, GREEN4f5d2a (12tests); structuredClone de datos retenidos.
13. @s16/@s39 lectura paginada de cambios con cursor codificado: REDf8d590, GREENb1f08c (13tests).
14. @s16/@s31 historial y recibos incompatibles: RED00bcaf, GREEN10d2a6 (14tests); validación cerrada before/after e identidades invariables.
15. @s32 revisión respuestaMAX+1 tras lecturaMAX válida: RED466629, GREEN37188e (15tests). No se invalida lecturaMAX; recibo imposible se rechaza.
16. @s18 GET recibo por id sin Location: RED1b095d, GREENb7210c (16tests).
17. @s33/@s35 cinco errores nuevos con shape/status cerrado: RED517013, GREENfd62c3 (17tests).
18. @s35 errores heredados CSRF/presupuesto: REDf14219, GREEN393251 (18tests), composición readBlockError sin relajar lógica heredada.
19. @s16/@s18 HTTPfallido/duplicados/orden historial: RED5042a7, GREENb2d233 (19tests); comparación de instante conserva microsegundos y desempateUUID.

## Corte cliente para revisión independiente

Formato focal y eslint/tsc sin errores b139b1. Regresión cliente nuevo19 + cliente11 existente250:269/269 GREEN0f2e3a. Sólo exports sameId/isPreview en schedule-block-api.ts; su lógica no cambia. API13 queda estable para revisión mientras sigue TDD UI. No significa feature completa ni cobertura de persistencia/E2E. Las matrices internas son grupos de variantes de un test, no se cuentan como ejecuciones adicionales. Todavía faltan oráculos públicos de UI/guardas y posteriores límites que señale revisión.

## Primer corte UI y pausa coordinada

20. @s28 abrir un panel inline desde una consulta deliberada de state; valores locales de Madrid independientes del navegador y cerrar sin POST. RED2bfc4d, GREENf5ba46 (1testUI).
21. @s31 lectura de state fallida no habilita editor, recuperación explícita. REDac3270 (incluyó rechazo no capturado en montaje aún mínimo), GREEN0f0bc9 (2testsUI).
22. @s12/@s36/@s38 cancelar reserva de tarea/proyecto completed sin disponibilidad; retirar fila, confirmar hecho histórico y enfocar encabezado si desaparece origen. RED7d411c, GREEN7c3ed0 (3testsUI).

Refactor en verde: BlockDetails y BlockTime movidos sin cambiar lógica a block-details.tsx para reutilización. Primera regresión74e138 EXIT1:52fallos/35pass y47errores debidos al import BlockTime omitido en el editor11. Diagnóstico69b221 y corrección de importf7524c. Regresión nueva5a2120 EXIT0:87/87 (84UI11+3UI13),31,93s. No se oculta la ejecución fallida ni se modifica ningún test11 para hacer pasar ese refactor.

Static checks: tsc9c8ee9 EXIT0. ESLint8c56ad señaló sólo import act no usado en testnuevo; retirado, eslint y Prettier focalesf8bf9f EXIT0. No se repiten tests por eliminar import sin uso.

Pausa solicitada por root al detectar otro equipo trabajando backend13. El usuario autorizó paralelo sin pisarse; root prepara rama Codexfrontend. Corte local estable, sin procesos de pruebas vivos al entregarlo. No se han hecho commits/push/cleanup desde este autor. API aprobada queda intacta desde corte269GREEN; nueva UI aún parcial y NO aprobada globalmente.

Archivos UI del corte: src/task-blocks.tsx (acciones y confirmación), src/reschedule-block.tsx (panel inicial/mínima cancelación), src/reschedule-block.test.tsx (3tests), src/block-details.tsx (extracción). No hay SCSS nuevo todavía.

Pendientes reales tras reanudación: preview/formulario completo de movimiento y selector/zonas no reconocidas; recuperación/errores y CSRF de cancelación/movimiento; vigencia state después de recibo y confirmación histórica11; historial inline; elegibilidad/estado cancelled y guardas de generación/ruta/401; foco/teclado/UIUX. En particular cancelación mínima todavía no captura errores ni conserva key ante incertidumbre: no se presenta como final ni debe publicarse producto por este WIP. No queda testRED activo tras el corte; siguiente comportamiento necesita su propio RED.

## Reanudación en rama Codex frontend

23. @s33/@s34 cancelación con ACK perdido conserva intención; comprobar recibo ausente permite sólo reenvío manual con idénticos URL/body/headers/key. RED c66658: 3 PASS/1 FAIL y rechazo lost ACK sin capturar. Arranque lento del entorno (36,6 s más setup 10 s), sin bucle de render; sesión62834 terminada. Implementación intermedia6285a2 todavía roja porque la sustitución de JSX no se aplicó; corregido el markup, GREEN3a1dbc:4/4, sin errores no capturados. Formato focal b84147. No se modifica API aprobada.

Historial delegado por root a resume_backend, ownership exclusivo reschedule-history.tsx y su test. Integración posterior desde TaskBlocks con montaje deliberado y refreshToken; no se ejecutan suites globales durante autoría paralela.
24. @s31 estado actual cancelled no ofrece otra operación: RED f79fdb, GREEN d281d2 (5/5).
25. @s35 conflicto de revisión412 requiere consulta explícita de estado; no reenvía automáticamente: RED291b41, GREENdaf70e (6/6). La consulta retira el editor anterior antes de esperar el nuevo snapshot.
26. @s7/@s29 revisión antes/después sólo tras preview: RED8bd6d8, GREEN5cd0b9/219f04 (7/7); refactor de fixture compartida GREEN0e8f77. BlockDetails reutilizado sin cambiar validadores.
27. @s8 POST deliberado de movimiento con revisiones exactas y allowOverBudget=false: RED4ede86, GREEN56a8c3 (8/8). Corte de movimiento aún mínimo: faltan recuperación compartida, invalidar preview al editar, presupuesto/zonas/DST, guardas/foco; no se atribuye UI completa a este nominal.
28. @s29 editar fecha retira revisión y confirmación: RED2734b9, GREEN577f82 (9/9).
29. @s37 edición aborta preview pendiente y descarta éxito tardío: RED9bc31d, GREENc9358b (10/10).
30. @s37 cierre aborta preview antes de entregar401 antiguo al observer de sesión: RED8c7773, GREENcea3aa (11/11).
31. @s35 preview fallida conserva borrador y permite revisión deliberada: RED372bfa/b0b201, incluido rechazo offline no capturado; GREENf903f4 (12/12) sin errores.
32. @s34 movimiento con ACK perdido recupera recibo por la misma key sin otro POST: RED8997af (rechazo sin capturar), GREEN37daea (13/13). Se reutiliza el componente de envío/recuperación; renombrado ChangeSubmit en refactor GREEN694a04, formatoaa8695.
33. @s9 exceso muestra presupuesto/reservado/solicitado/exceso y exige consentimiento explícito con guarda real: REDbe95c4, GREENc13b0c (14/14). No se calcula presupuesto cliente ni se relaja el validador compartido.

Comprobación estática intermedia: invocación errónea d0039b apuntó a tsconfig.app.json inexistente; no es indisponibilidad de tsc. Comando correcto tsc --noEmit GREEN648a19. ESLint279755 señaló cuatro parámetros mock sin uso, sustituidos por firma genérica equivalente; eslint focal GREEN9e3b06, formato9ab81a.
34. @s39 integración de historial entregado por autor independiente: carga sólo al activar Ver cambios de bloques y refreshToken tras confirmación. RED7c1b2e, GREEN61ee1c (15/15). No se modifican reschedule-history.tsx/tests; su evidencia propia está en tdd_reschedule_history.md.
35. @s30 zona histórica no reconocida por Intl: REDab4391 (RangeError en render), GREEN49b1d6 (16/16). Conserva representación UTC con nombre original; nuevo destino empieza sin horas ni zona inferidas.
36. @s28/@s40 sugerencias nativas mediante datalist consultadas sólo al enfocar zona: RED83f685, GREENb4d798 (17/17). Reusa readAvailabilityZones; no consulta disponibilidad ni catálogo al abrir panel. Concreción UX autorizada por root.
37. @s40 fallo del catálogo conserva zona y ofrece reintento explícito: REDd083c2 (incluye rechazo sin capturar), GREEN1af785 (18/18).
38. @s37 cierre aborta catálogo pendiente antes de401 tardío: REDacdfcc, GREENf71d46 (19/19). Callback y catch ignoran señal abortada.
39. @s34 refuerzo de la secuencia existente: tras ACK perdido los campos quedan readonly y la guarda ignora edición sintética; RED15ed22, GREENc83713/10b5d1 (19/19). No se cuenta como test adicional.
40. @s36 movimiento sólo con tarea/proyecto elegibles confirmados; cancelación sigue visible con completed/desconocido: REDb6040d, GREEN62f9eb (20/20).
41. @s36 retirar revisión al quedar elegibilidad desconocida: RED652e55, GREENe4a41b (21/21). Refuerzo de restauración elegible no resucita preview: RED04e699, GREEN991dbf (21/21).
42. @s34/@s36 la secuencia de recuperación ya existente continúa aunque tarea/proyecto pasen a completed: RED9fee10; ver GREEN siguiente. No se cuenta como test adicional; mismo recibo/key sin otro POST.
Ciclo42 GREEN7db738:21/21 EXIT0.
43. @s7/@s29 dos ocurrencias DST elegidas explícitamente desde offsets del servidor: RED17b1dd, GREEN4c08fa (22/22). Refuerzo de mismo flujo al editar fin (conserva inicio) y zona (retira ambos): REDce4097, GREEN2472da (22/22).
44. @s40 Enter desde destino nativo revisa, no confirma: REDfb038b, GREEN486d77 (23/23). Formulario submit llama preview; confirmar mantiene type=button.
45. @s38 refuerzo del movimiento nominal: confirmar con foco en botón desaparecido lo devuelve al encabezado; RED6bebb6, GREENbeae39 (23/23).
46. @s29 refuerzo presupuesto: cambiar zona retira preview/consentimiento y nueva revisión exige nueva aceptación; RED286b4b, GREENe42e8b. Refactor invalidate común fecha/zona aborta preview y retira consentimiento; GREENbbb3e8 (23/23).
47. @s29 refuerzo DST: cambiar ocurrencia retira revisión mediante invalidate común; RED568f26, GREEN siguiente (23tests).
Ciclo47 GREENf9fe97:23/23 EXIT0.
48. @s35 hora local inexistente conserva borrador y asocia error al campo inicio: RED67448b, GREENafc72a (24/24).
49. @s35 movimiento412 consulta state real y siguiente preview usa revisión nueva sin perder fechas: RED2bf3c3, GREENd1d3c3 (25/25). Refuerzo del mismo flujo con lectura503 y reintento: RED7823a6 (incluido rechazo sin capturar), GREEN5d906d (25/25).
50. @s37 RESOURCE_NOT_FOUND de state comunica retirada del contexto al padre: RED668c85, GREENc41f6a (26/26).
51. @s33 refuerzo del flujo presupuesto: consentimiento queda disabled y su guarda ignora click durante envío: REDe4d47e, GREEN10b827 (26/26).
52. @s35 CSRF conocido conserva intención y permite reenvío deliberado con token renovado sin comprobar404: RED9b4281, GREEN072993 (27/27). Esta prueba controla setCsrfToken en frontera API; no atribuye nueva evidencia del flujo completo SessionGate. Limpieza de observer/token pasa al afterEach para aislar fallos.
53. @s38 integración BlockConfirmation13: refuerzo cancelación requiere lectura automática de estado actual separada del recibo histórico. REDa1f819, GREEN7f5800 (27/27). Componente del autor independiente no modificado.
54. Refactor autorizado en verde: ChangeSubmit extraído sin cambio semántico a change-submit.tsx. Formato3d712a, regresión panel GREENb8cd58 (27/27). Ownership del archivo nuevo y su futuro test pasa a resume_backend; este autor no lo edita desde la cesión. Panel conserva integración mediante props ya existentes.
55. Integración confirmación original11 en TaskBlocks conserva Bloque guardado/id/foco y añade separación histórica con lectura actual automática: RED9f0e4a, GREEN4d644d (1 focal PASS,83omitidos por filtro, no84ejecuciones). Se inicia regresión del archivo84 para comprobar efecto de integración compartida; componente BlockConfirmation sin editar.
Regresión84 del archivo11:80f21e EXIT1,83PASS/1FAIL. Historial nuevo había quedado antes de Planificar en orden DOM y alteraba el Tab desde encabezado (@s59). Se movió apertura/historial detrás del editor, conservando recorrido anterior y todos sus asserts. Focal afectado GREEN5aa8f7:1PASS/83filtrados; no se declara84PASS después de ese ajuste hasta gate posterior.
56. @s38 preview conserva foco y coalesce doble activación mientras espera: RED4037db, GREEN166222 (1focalPASS/27filtrados).
57. @s28 cancelación presenta intervalo del state actual, no fila antigua: RED523cd0, GREEN4849f0 (1focalPASS/28filtrados).
58. @s35 errores de fin y zona asociados a sus campos (matriz de2variantes,1test): RED24057d/b464dd, GREEN3843b9 (1PASS/29filtrados).
Refactor de presentación verde: wrappers .field con label/htmlFor reutilizan CSS existente para fechas/zona/ocurrencias; no estilos nuevos ni assertions className. Formato1aebfd, suite de panel GREEN041448:30/30. No se atribuyen píxeles/reflow desde JSDOM.
59. @s35 rechazo definitivo de presupuesto desbloquea borrador, retira revisión y exige nueva aceptación: RED4fe4f8, GREEN3eb4ed (1PASS/30filtrados).
60. @s38 abrir/cerrar por teclado mantiene foco en encabezados: RED983b2b/7a1e67, GREEN1b5c9b (1PASS/31filtrados).
61. @s37 RESOURCE_NOT_FOUND durante preview o envío comunica retirada del contexto al padre (2variantes,1test): RED829b4c, GREENc6f06c (1PASS/32filtrados). Integración de callbacks de ChangeSubmit; componente reservado sin editar.
62. @s36 preview pendiente al retirar elegibilidad: RED82d337, GREEN904b54 (1PASS/33filtrados). Aborta petición, retira consentimiento y no acepta resultado antiguo tras volver a pending.
63. @s35 refuerzo del flujo DST existente: errores asociados a ambos selectores y retirada del error al corregir ocurrencia. RED174489, GREENa83bce (1PASS/33filtrados); no test adicional.
64. @s29 iniciar nueva revisión retira la anterior mientras espera: RED03691a/c7d194, GREENbb5ba7 (1PASS/34filtrados).
65. @s35 consulta tras412 que descubre cancelación mantiene borrador pero impide revisar otro movimiento: RED9da90e, GREENfcd27d (1PASS/35filtrados).
66. @s36 composición App crear→mover mismo bloque reproducía dos confirmaciones/estados actuales: REDf188a2, GREEN8ffdba (1PASS/84filtrados). Se conserva sólo confirmación de operación más reciente al confirmar éxito; no se retira el recibo previo durante petición/fallo. API y BlockConfirmation intactos.
67. @s33 selectores de ocurrencia bloqueados y guarda de edición durante incertidumbre: primeros intentos c0afdf/b892ae detectaron fixture fuera de esquema y nombre de botón incorrecto; corregidos sin producción. RED real1c3964, GREENcb3f76 (1PASS/36filtrados).
68. @s35 rechazo BLOCK_UNCHANGED conserva mensaje junto al borrador desbloqueado: RED8d9e5e, GREEN255c0f (1PASS/37filtrados).
69. @s35 validación definitiva del envío se asocia al campo fin, permite corregirlo y exige nueva revisión: fixture inicial7048b8 usaba código inválido; corregido. RED real4624e5, GREENef9d35 (1PASS/38filtrados).
Corte de integración: formato14ceae y regresión panel39 + TaskBlocks85 GREEN5ecf30:124/124 EXIT0. ESLint313c6a detectó lectura de ref durante render y parámetro mock sin uso; refactor mueve sólo aborto a useLayoutEffect y conserva retirada síncrona del estado. Formato4d43fc, ESLint335e9e y tsc74969b EXIT0; focal guardia elegibilidad GREEN799cbf (1PASS/38filtrados). No repetir124 tras este refactor hasta gate global del coordinador; focal cubre la ruta modificada.
Re-review CHANGES_REQUESTED: R1–R6 en progress/review_reschedule_frontend.md. Antes de producción se repara únicamente formato block-confirmation.test.tsx autorizado por root tras init99190: 0546b0 Prettiercheck EXIT0, AST estructural TypeScript igual (kind, hijos y valores literales; sin posiciones/trivia/paréntesis redundantes/JSX vacío). SHA AST951fa150fa298aadebbf978ae035b2adda5865dd12655fe5111e1facadac641f; archivo antesc69c5c8c24dadeccd55ef1372d6c29e0fcf260945362905d029157c45694856f/después5a0287963d945a275ea9b997cb2a7fffedffc017a3a91dc8e819883120ca2c7c. Comparaciones iniciales567858/b4c5b6 no escribieron; printer sensible a líneas y serialización SourceFile.text corregidos antes de demostrar equivalencia. No suite repetida por formato.
70. R1 @s37 recuperación412→GET RESOURCE_NOT_FOUND en App real retira tarea/objetivo/borrador: RED2860d9, GREENe477f5 (1PASS/85filtrados). loadCurrent clasifica con guardas antes/después del await; no cambia API ni padre.
71. R2 @s35 preview412 ofrece consulta deliberada y siguiente preview usa revisión2 conservando destino, sin POST automático: RED421908, GREEN8fd496 (1PASS/39filtrados).
72. R3 @s38 foco durante GET de reintento inicial y recuperación412 (2variantes,1test), y no robar otro control tras respuesta: REDbdbc7c, GREEN4acb0f (1PASS/40filtrados). Encabezado existente como destino cuando BODY; callback local sólo durante recuperación.
73. R4 @s38 aviso visible de que cerrar no revoca operación transmitida, sin POST extra ni confirmación tardía: REDcd8ef1, GREEN8c8be5 (1PASS/41filtrados). Explicación incondicional precisa, sin modal ni máquina adicional.
74. R5 @s35 corregir inicio/zona retira errores afectados y permite revisión válida (2variantes,1test): RED73c1ae, GREEN2e0d89 (1PASS/42filtrados). Se mantienen errores de otros campos aún sin corregir.
75. R6 @s40 refuerzo de prueba diferida/coalescing existente: anuncio role=status Revisando movimiento durante preview y retirada al finalizar: RED435f57, GREEN1bde16 (1PASS/42filtrados). No atribuye medición de latencia real desde JSDOM.
Cierre correctivo R1–R6: formato2bf87c, regresión dos archivos panel43+TaskBlocks86 GREENa67b37 129/129 EXIT0; ESLint focal (incluye test BlockConfirmation formateado) d719f2 y TypeScript7e8d7c EXIT0. Fuentes/tests congelados para re-review del coordinador. No nuevas suites globales, E2E, mutación ni commits de este autor.
76. Re-review R6 de la misma espera de recuperación: refuerzo del test R3 exige status Consultando estado actual mientras GET espera y retirada al acabar. RED99bc0b, GREENb5c560 (1PASS/42filtrados); mínimo JSX sobre currentLoading existente, formatof99a41. Freeze renovado; sin repetir129 ni suites globales por esta línea.
