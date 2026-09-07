# UX de revisión semanal — evidencia final para revisión

Fuente de criterios: `docs/ux-requirements.md`. La evaluación semántica se refiere sólo a la revisión semanal; no certifica facilidad de uso psicológica universal. E2E nominal, selección y durabilidad pasan. Dos fallos físicos/funcionales observados fueron corregidos y revalidados. Inventario ejecutado: `weekly_review_e2e_ux_freeze.json`, 124 medidas y 25 axe sin infracciones, con tres motores y zoom nativo Chromium. Gates globales/mutación/CI se resuelven por separado.

| Principio | Aplicación y evidencia actual / límite |
| --- | --- |
| Atención selectiva | H1, formulario de dos campos, totales y días; captura inicial revisada. 26 anchos por motor, entre 320 y 2560, sin desbordamiento ni solapes medidos. |
| Carga cognitiva | Sólo fecha y zona; no selección de sesiones ni memoria de otra pantalla. Revisión semántica, sin estudio de usuarios. |
| Estética-usabilidad | Reutiliza estilos y controles existentes. Áreas pequeñas corregidas por SCSS local; mismos controles revalidados. |
| Posición en serie | Mostrar semana precede resultados y navegación semanal. Tab alcanza fecha; Enter activa actualización y recuperación en los tres motores. |
| Tendencia a la meta | E2E durable exige 1 h reservada y 30 min trabajados, sin porcentaje de logro. No muestra objetivos ni incentivos nuevos. |
| Von Restorff | Acción nativa Mostrar semana; errores se expresan con texto. Capturas de pendiente/error/recuperación en 320/768/1440 por motor. |
| Zeigarnik | No edita ni pierde notas/siguiente paso; remite a planificación. No agrega un recorrido de cierre. |
| Fluir | Lectura bajo demanda sin temporizador ni interrupciones de sesión. No modifica la hora de fin. |
| Fragmentación | Totales y siete unidades diarias semánticas. Datos reales positivos acreditados por @s24. |
| Memoria de trabajo | Fecha/zona aplicadas viven en URL; recarga real conserva selección. Back real y recuperación de error conservan selección. |
| Navaja de Occam | Una página y dos filtros nativos, sin gráfico ni calendario adicional. |
| Conectividad uniforme | No dibuja conexiones entre plan y ejecución ni atribuye causalidad por coincidencia. |
| Fitts | RED inicial selector 19 px y enlace 21 px; corrección scoped pasa 44 px, reflow y ausencia de solapes en nominal. |
| Hick | Mostrar semana es la decisión de consulta; cambiar un campo no consulta automáticamente. E2E de selección acredita que editar no consulta; Mostrar semana aplica. |
| Jakob | Enlaces conservan URL, formulario nativo, navegación semanal explícita. Nominal y recarga reales verdes. |
| Semejanza | Mismo Workspace y quinto enlace, sin nueva navegación paralela. Nominal, recuperación y texto200 pasan en Chromium/Firefox/WebKit. |
| Miller | Siete días por definición civil, no por regla cognitiva arbitraria. Agrupación diaria etiquetada. |
| Parkinson | No altera sesiones ni presupuesto y no extiende tiempo. La vista es sólo lectura. |
| Postel | Fechas/zonas conforme al contrato; decoder cerrado sin recomputar TZDB. 503 recuperado en navegador; campo400/409 conservan oráculos DOM, sin atribuirles captura física propia. |
| Proximidad | Labels y errores enlazados al campo en fuente/tests. Error503 medido y capturado; asociación específica de campos400 comprobada por DOM. |
| Prägnanz | Plan, trabajo y presupuesto se nombran por separado; sin iconos que sustituyan texto. |
| Región común | Formulario, resumen y lista diaria representan unidades reales. Captura inicial sin solapes a 320. |
| Tesler | Servidor calcula intersecciones temporales; la UI explica zona y presupuesto desconocido. No expone IDs/revisiones. |
| Modelo mental | @s24 demuestra reserva de 1 h y ejecución distinta de 30 min; texto aclara que no son tareas terminadas. |
| Usuario activo | Primer GET vacío útil con siete días y presupuesto desconocido, sin configurar antes. E2E nominal verde. |
| Pareto | Consulta de semana actual por defecto y selección histórica directa; no se inventan porcentajes de uso. |
| Fin de pico | Lectura confirmada distingue error y datos anteriores; recuperación real final200 después del503 controlado pasa en tres motores. |
| Sesgo cognitivo | Cero de capacidad es descanso planificado actual; no se infiere descanso real ni deuda. Siete días con capacidad cero capturados; tests y lenguaje distinguen descanso planificado de real. |
| Sobrecarga de opciones | Dos filtros y tres enlaces semanales; zonas cargadas al usar el selector. No nuevos catálogos. |
| Doherty | Estados de consulta y actualización existen; feedback medido antes de liberar503: Chromium4,6 ms, Firefox5 ms, WebKit3 ms; no equivale a tiempo total de red. |

Evidencia física: por motor, 26 anchos nominales (incluye mínimos 320–2560 y ambos lados de breakpoints relevantes, 768×400), cuatro estados × tres anchos, texto200 en tres anchos con factor2 verificado. Zoom nativo Chromium200: DPR1,5→3, 320×453 CSS. Texto200 amplía el contenido principal; el zoom nativo cubre toda la página. El título puede partir palabras al ampliar, conservando texto y reflow. No se atribuye un recorrido completo de teclado sólo por enfocar un control: se acredita Tab inicial, activación por Enter y foco recuperado; la navegación exhaustiva, dispositivos físicos, teclado virtual y lector de pantalla real no se ejecutaron. Tampoco se midió aprendizaje/carga cognitiva con personas. Error400/409, privacidad401 y respuestas obsoletas tienen oráculos DOM previos; no se confunden con captura browser específica. No se declara toda la matriz de estados ampliada al200: texto/nativo corresponden al resumen con capacidad cero. Axe no sustituye estos límites.
# Delta final: separación del contorno del encabezado

La revisión visual de root motivó una medición dentro del nominal existente. RED real afc04d (`weekly_review_outline_red.log`): a 320 px, el borde inferior del h1 y el comienzo de la etiqueta estaban ambos en y=343,46875; el outline de 3 px con offset de 4 px invadía 7 px la caja de la etiqueta. La captura y geometría previas se preservaron en `progress/weekly_review_ux_before_outline`.

Autorizado el cambio local `.weekly-review > h1 { margin-bottom: 16px; }`, el mismo nominal pasó, EXIT 0 6db083 (`weekly_review_outline_green.log`). Las 26 anchuras conservan controles y ausencia de overflow, junto al nuevo oráculo del contorno. A 320 y 1440 quedan 9 px libres después del contorno hasta la etiqueta (343,46875→359,46875 y 202,171875→218,171875 respectivamente). No se modificó TS ni estilos globales.

Firefox y WebKit: 3/3 casos de nominal, recuperación y texto ampliado, EXIT 0 b5c10d; logs `weekly_review_outline_firefox.log` y `weekly_review_outline_webkit.log`. Artefactos en las mismas rutas por motor; el snapshot previo permanece aparte. La ampliación nativa y texto de Chromium pasaron 2/2, EXIT 0 0348f9 (`weekly_review_outline_enlarged_final.log`). El primer intento no ejecutó casos: cmd interpretó el separador de la regex como pipe; se corrigió sólo el argumento a --grep 200. Zoom nuevo en `.e2e-work/weekly-review-native/organizationweb-e2e-11988/evidence/zoom.json`: factor 2, DPR 1,5 a 3 y viewport 320×453. No se repite ni se atribuye una nueva campaña de mutación por este cambio local de CSS.
