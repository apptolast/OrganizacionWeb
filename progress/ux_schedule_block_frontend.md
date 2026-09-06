# UX de bloques: evidencia inicial de interfaz

Contrato a84e42f, feature 11 todavía in_progress. Ponytail full y Caveman lite. No es aprobación global ni certificación de usabilidad. La revisión de treinta principios sigue docs/ux-requirements.md.

## Entorno y alcance

Pruebas de componentes: 52 casos de task-blocks, 159/159 con task-state y split-task. Vite local 127.0.0.1:5174, Chromium de Playwright y HTTP interceptado: `.e2e-work/schedule-block-frontend-ui.mjs`; datos ficticios de objetivo Unicode largo, revisión UTC con presupuesto Europe/Madrid y exceso explícito, creación confirmada. Estos datos simulados no acreditan API, PostgreSQL ni persistencia. Mediciones en `.e2e-work/schedule-block-ui/measurements.json`; capturas preview-320.png, preview-1440.png y confirmed-320.png en el mismo directorio, inspeccionadas visualmente.

Anchos CSS comprobados: 320, 360, 390, 419, 420, 421, 480, 599, 600, 601, 699, 700, 701, 768, 820, 999, 1000, 1001, 1024, 1280, 1440, 1920 y 2560. Revisión/aceptación sin overflow y controles principales ≥44×44 en todos; confirmación a 320×568 sin overflow. Axe: cero infracciones en la revisión capturada. El nuevo SCSS usa el breakpoint existente de 600 px; se comprobaron 599/600/601. No se confunde esta matriz de anchos con zoom nativo ni con cobertura completa de estados.

## Matriz de treinta principios

| Principio | Aplicación concreta | Evidencia y resultado |
| --- | --- | --- |
| Atención selectiva | Guardar disponible después de revisar; avisos separados. | Capturas de revisión y @s39/@s43. Verificado en este corte; jerarquía con personas pendiente. |
| Carga cognitiva | Objetivo, comienzo, fin y zona; ocurrencias sólo tras ambigüedad. | @s38/@s41. Comportamiento verificado; comprensión humana pendiente. |
| Estética-usabilidad | Agrupación y controles coherentes con tareas existentes. | Capturas 320/1440 inspeccionadas. Evaluación humana pendiente. |
| Posición en serie | Revisar, Guardar y Cancelar mantienen orden. | DOM y capturas. Recorrido completo de teclado por todos los estados pendiente. |
| Tendencia a la meta | No inventar progreso a partir del bloque planificado. | Texto explícito y ausencia de métricas de avance. Cálculo de progreso no aplicable a esta feature. |
| Von Restorff | Alertas con texto y borde, no sólo color. | @s49 y SCSS de alertas; contraste axe en revisión verde. Todos los errores visuales pendientes. |
| Zeigarnik | Se conserva el borrador ante errores; se explica descarte al salir. | @s44/@s53/@s54. Verificado por componentes. No se afirma persistencia del borrador tras cierre. |
| Fluir | Bloque muestra duración e instantes. | @s39, capturas. Empezar/pausar/cerrar sesiones no aplicable; no existe ejecución de sesión aquí. |
| Fragmentación | Tarea, bloques, revisión y subtareas están en secciones separadas. | Capturas y regiones con nombres accesibles. Verificado en estados capturados. |
| Memoria de trabajo | Revisión visible con ambas zonas y campos conservados. | @s39/@s45/@s49. Verificado por componentes. |
| Navaja de Occam | Controles nativos; consulta de conflicto explícita inline. | @s38/@s49; sin dependencia nueva ni pantalla extra. Revisión de código disponible. |
| Conectividad uniforme | Se muestra el bloque conflictivo devuelto para el contexto propio. | @s49 consulta incluso otro proyecto/tarea propio. Sin conexiones decorativas. |
| Fitts | Zona de 48 px y aceptación mínima de 44 px; botones separados. | RED visual real de 19/42 px corregido; 23 anchos verdes. Dispositivos táctiles físicos pendientes. |
| Hick | Revisión previa a guardar; reenvío sólo tras ausencia confirmada por key. | @s39/@s47/@s48. Flujo verificado; carga decisional humana pendiente. |
| Jakob | Formularios y controles nativos, foco conservado o devuelto a encabezado. | @s38/@s58. Confirmación y recuperación verificadas; teclado en motores alternativos pendiente. |
| Semejanza | Bloques reutilizan estilos de agrupación de tareas y controles secundarios. | SCSS compartido y capturas. Verificado en Chromium capturado. |
| Miller | Datos agrupados por bloque y por día, sin límites arbitrarios de opciones. | Revisión semántica y capturas. Comprensión humana pendiente. |
| Parkinson | Duración explícita entre dos extremos; ninguna extensión automática. | @s39/@s44; las sesiones en curso quedan fuera de este recorrido. |
| Postel | Error de campo seguro, fechas nativas y ocurrencia decidida por el usuario. | @s41/@s59; cliente API aprobado conserva normalización/contrato. Verificado por componentes. |
| Proximidad | Ayuda/error junto a cada campo y asociado por aria-describedby. | Fecha/zona/objetivo y ambos offsets comprobados. Verificado por componentes. |
| Prägnanz | Estados textualizados: revisando, guardando, incierto y guardado. | @s39/@s44/@s45. Verificado; lector de pantalla real pendiente. |
| Región común | Contenedores corresponden a lista, revisión y conflicto reales. | Regiones semánticas y capturas. Verificado en estados capturados. |
| Tesler | Backend resuelve tiempo; UI explica offsets, ambas zonas y exceso. | @s39/@s41/@s49/@s57. Integración con tiempo backend pendiente. |
| Modelo mental | Bloques son tiempo planificado, no trabajo realizado. | Texto visible y @s56: completar proyecto/tarea conserva recuperación. Verificado por componentes. |
| Usuario activo | Estado vacío orienta hacia Planificar bloque. | @s38 y estado inicial del script. Evaluación de primer uso humana pendiente. |
| Pareto | Creación frecuente accesible desde la propia tarea. | @s38 y captura. Prioridad de producto, sin porcentaje de uso supuesto. |
| Fin de pico | Confirmación sólo con DTO coincidente; fallo de lista no borra confirmación. | @s46/@s54; incertidumbre recuperable. Verificado por componentes, persistencia real pendiente. |
| Sesgo cognitivo | Consentimiento sin preselección y descanso de cero minutos explícito. | @s43 y script con presupuesto cero. Verificado en revisión simulada. |
| Sobrecarga de opciones | Configuración existente aporta zona; ocurrencias se revelan cuando hacen falta. | @s38/@s41/@s55. Personalización de temas/densidad no aplicable a este corte. |
| Doherty | Espera honesta visible antes de confirmar. | @s39/@s44 verifican estados pendientes; medición <400 ms pendiente. |

## Puertas pendientes

E2E con servidor y PostgreSQL reales; matriz de carga/error/recuperación completa; Firefox y WebKit; zoom nativo 200 %, ampliación de texto, orientación/altura reducida durante edición y teclado; feedback medido; anuncios con lector real; dispositivos físicos y teclado virtual; facilidad de uso humana. Los resultados anteriores no sustituyen revisión independiente ni mutación y no declaran done.

## Ampliación verificada posterior (HTTP simulado)

El alcance inicial de arriba se conserva como evidencia histórica. El mismo script ahora acepta `chromium`, `firefox`, `webkit` y `zoom`, y genera cada motor en `.e2e-work/schedule-block-ui/<motor>/measurements.json` con capturas por estado/ancho. No se ejecutó Docker ni se presentó esta ampliación como E2E de API/PG.

Chromium 153.0.8010.12, Firefox 155.0 y WebKit 26.6 completaron **141 combinaciones por motor**: vacío, error de validación asociado, revisión con exceso, guardando, resultado incierto y confirmado en los 23 anchos enumerados; además texto duplicado desde su tamaño calculado en 320/768/1440. En 768 se usa altura 400. Ninguna página desborda horizontalmente; los controles medidos siguen siendo ≥44×44. Los datetime-local conservan edición segmentada nativa: la ampliación de texto no se presenta como prueba de un teclado virtual ni de un dispositivo físico.

Axe analizó cada uno de esos siete estados, cero infracciones en los tres motores. Se comprobó Shift+Tab desde la aceptación hasta Guardar, su contorno de foco visible, Enter para guardar y Enter para recuperar. El guardado pendiente bloqueó campos, informó espera y tras el 503 simulado conservó la intención; la consulta simulada confirmó con una sola creación. Movimiento reducido activado y ninguna animación en la sección. Capturas de WebKit incierto a 320 y Firefox con texto ampliado a 320 inspeccionadas; también el diseño inicial en Chromium. No se afirma haber inspeccionado visualmente cada una de las 423 combinaciones.

Feedback con MutationObserver desde el click del botón hasta aparición de `Guardando bloque`, manteniendo la respuesta retenida: Chromium **4,5 ms**, Firefox **3 ms**, WebKit **3 ms**. Esto mide reacción local, no garantiza latencia de red/servidor ni todos los dispositivos.

Zoom **nativo** Chromium al 200 % mediante `chrome.tabs.setZoom(2)` en perfil aislado: ventana 1440 mantiene tamaño, innerWidth pasa de 1426 a 713 y DPR de 1,5 a 3. Ventana ajustada a 654 produce innerWidth 320 y clientWidth/scrollWidth 312. Los siete estados pasan medidas y axe; feedback **9,5 ms**. No se usó CSS zoom ni reducción de viewport como sustituto. La primera captura mediante screenshot de Playwright quedaba recortada por su interacción con el zoom; se sustituyó el mecanismo de captura por Page.captureScreenshot con el patrón CDP usado previamente en availability, manteniendo los mismos tamaños nativos y comprobaciones. La captura completa de error de campo a 320 se inspeccionó sin recorte de página y con foco visible. Las capturas finales y medidas nativas están en la subcarpeta zoom. El perfil propio de esta comprobación queda bajo esa carpeta, sin limpieza de ascendientes ni modificación de archivos bloqueados.

### Actualización de resultados de la matriz

Las treinta filas de arriba mantienen su aplicación. Se resuelven los pendientes técnicos citados allí para: orden de teclado de Revisar/Guardar/aceptación/recuperación (Posición en serie, Jakob); contraste automatizado y estados de error/espera/incertidumbre (Von Restorff, Estética-usabilidad, Fin de pico); matriz de motores (Semejanza); texto ampliado y altura reducida (Proximidad, Región común); y medición de feedback local (Doherty). Las filas que requieren comprensión/atención humana siguen pendientes de esa evaluación, sin convertir pruebas automáticas en evidencia humana.

Puertas restantes: integración con servidor y PostgreSQL reales, revisión independiente y mutación; dispositivos físicos, teclado virtual y lector de pantalla real; facilidad de uso por personas. La comparación temporal de DST con backend sigue pendiente del E2E real. No se declara done.

### Abrir y cancelar con teclado

Probe adicional en los tres motores con foco en Planificar y Enter: al reemplazarse el botón, activeElement pasa a body, pero Tab siguiente llega a textarea#block-objective. Con foco en Cancelar y Enter, activeElement vuelve a body y Tab siguiente llega a Planificar bloque. El navegador conserva el punto secuencial de navegación; no vuelve al inicio ni introduce trampa. Se conserva ese comportamiento nativo y no se añade autoenfoque por observar body aisladamente. El probe vive en `--focus` del mismo script y sus resultados se comunicaron al coordinador.

Las últimas reejecuciones para validar capturas y este probe produjeron feedback de 8,2/4/3 ms (Chromium/Firefox/WebKit), todos inferiores a 400 ms. Los valores iniciales de arriba se conservaron como resultados observados, no como constantes ni garantía de latencia. El JSON por motor contiene su última medición. Capturas no nativas también se regeneraron desde scrollY=0 para evitar el artefacto de posición de elementos fixed al capturar la página completa.

## Primera evidencia con API y PostgreSQL reales

Suite real `e2e/schedule-block.spec.mjs`: **3/3 PASS** sobre snapshot backend126HTTP, runner61176, fixture aislado organizationweb-e2e-61432, sin cambiar el runner. Incluye creación/replay/recarga y SQL de bloque/outbox; respuesta201 perdida después de commit, completar proyecto y recuperación by-request; y presupuesto cero que exige consentimiento nuevo tras editar. No acredita todavía solape concurrente, reparto histórico de presupuesto ni DST real.

Capturas reales de revisión, persistido y recuperado a320/1440 en `.e2e-work/schedule-block-real/chromium/`, con JSON de URL/fixture/fecha por estado. Inspeccionadas review-1440.png y recovered-320.png: sin recorte visible, datos/acciones legibles y confirmación verdadera. Estas capturas usan API/PG y datos sintéticos, a diferencia de las matrices interceptadas anteriores. Detalle de snapshots y alcance en `tdd_schedule_block_integration.md`.

Actualización real: cuarto caso DST Madrid pasó en Chromium con API y PG, **4/4 PASS, EXIT0** sobre snapshot138HTTP (fixture69344). Ambas ocurrencias son explícitas y cambiar el inicio invalida revisión; duración30→90 y UTC00:15–01:45 verificados. Primer smoke real también pasa Firefox/WebKit, **2/2 PASS**, fixture13460. Sus capturas review/persisted a320/1440 están en los directorios de cada motor. Recuperación, consentimiento y DST cruzados aún pendientes de la ejecución completa; el filtro inicial sólo seleccionaba el smoke.
Inspección adicional de capturas reales: webkit/persisted-320.png y firefox/review-1440.png, sin recorte visible; bloques, resumen temporal y acciones legibles. La altura completa refleja todo el detalle de tarea, no un modal.
Matriz real completa: Firefox/WebKit 8/8 PASS, EXIT0 (fixture11224, snapshot142HTTP + reservas Store). Recuperación tras ACK perdido, consentimiento renovado y DST también pasan en ambos motores. Capturas reales recovered a320/1440 disponibles por motor. Se mantienen pendientes dispositivos/lectores físicos, comprensión humana y puertas de revisión/mutación; integración feliz no acredita carreras ni rollback.
Integración incremental adicional Chromium: 6/6 PASS (fixture67288, snapshot147HTTP). Solape aparecido tras preview conserva borrador y permite detalle explícito; consumo de presupuesto posterior obliga a nueva revisión y aceptación. Ambos rechazos definitivos se verificaron sin nueva fila antes de corregir. Estos dos casos todavía no tienen ejecución Firefox/WebKit; los cuatro anteriores sí.
Estado vigente posterior: siete recorridos reales en cada motor (Chromium7/7, Firefox/WebKit14/14), incluidos solape, capacidad consumida y reinicio real de backend conservando PostgreSQL. Los pendientes cruzados anteriores son historia superada. Nuevas pruebas@s53 retienen JSON durante navegación/revocación y confirman ausencia de datos/foco obsoletos; mapa actual en tdd_schedule_block_frontend.md. Persisten límites de dispositivos físicos, lector real y evaluación humana.
