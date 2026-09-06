# UX — disponibilidad

## Evidencia del recorrido

Primera suite Chromium: 48/48 en 3,7 minutos (sesión 71715), con cinco recorridos nuevos. El formulario pasó 28 anchos CSS: 320, 359, 360, 361, 390, 419, 421, 480, 599, 600, 601, 699, 701, 759, 761, 768, 820, 999, 1001, 1024, 1099, 1101, 1280, 1440, 1599, 1601, 1920 y 2560. A 768 se usa altura 400. Se midieron controles del main de al menos 44 × 44, ausencia de overflow de página, teclado real y axe WCAG A/AA. Ese alcance inicial no detectaba texto de navegación fuera de sus enlaces.

La inspección adicional encontró solape real a 701–760 px; la corrección SCSS alineó la navegación horizontal con el breakpoint de 700. La nueva aserción focal reprodujo RED (232 px de contenido en 169 px), y se incorporó a la matriz. La verificación final separada pasó matriz y navegación 2/2 en 26,9 segundos sobre CSS Codz1mIb; no se agrega como una nueva suite global al 48/48 original.

| Principio | Aplicación concreta | Evidencia y límites |
| --- | --- | --- |
| Atención selectiva | Zona y semana aparecen en una tarjeta con Guardar como acción principal. | Jerarquía en DOM y texto; percepción humana pendiente de evaluación de uso. |
| Carga cognitiva | Siete presupuestos por día, sin pedir ventanas ni horas de inicio. | Recorrido completo en una vista; no requiere recordar otra pantalla. |
| Estética-usabilidad | SCSS y jerarquía coherentes con la aplicación. | Escritorio, móvil, zoom a 320 y navegación a 720 inspeccionados: legibles y sin el solape corregido. |
| Posición en serie | Zona precede días; total y guardado cierran el formulario. | Orden de teclado verificado con Tab/Enter, sin focus() que sustituya el recorrido. |
| Tendencia a la meta | El total expresa capacidad prevista. | Progreso medido y objetivos realizados no aplican aquí; no se inventa avance. |
| Von Restorff | Confirmación y error tienen textos distintos. | Ningún éxito antes de respuesta válida; estado no depende sólo del color. |
| Zeigarnik | Preferencias guardadas reaparecen tras recarga y reinicio. | Borrador sin guardar no es persistencia: aviso permanente y Cancelar explícito. |
| Fluir | El usuario decide su presupuesto y puede descansar. | Inicio/pausa/cierre de sesión con hora de fin sigue pendiente de planificación, no se da por cumplido. |
| Fragmentación | Presupuesto separado por día de la semana. | Siete grupos semánticos fijos, sin crear tareas ni bloques desde disponibilidad. |
| Memoria de trabajo | Error conserva borrador y explica recuperación. | Conflicto y confirmación contradictoria reales en navegador; recarga válida reemplaza de forma deliberada. |
| Navaja de Occam | Selector y números nativos. | Sin autosave, nueva librería de calendario ni guardia global de navegación. |
| Conectividad uniforme | Cada etiqueta enlaza su presupuesto. | Labels y errores asociados; mapa cerrado validado en API. |
| Fitts | Controles y enlaces del formulario alcanzan 44 × 44 CSS. | Mediciones en 28 anchos y zoom nativo al 200 % con ancho interior 320; mínimo de 44 × 44 conservado. |
| Hick | Guardar, recuperar cuando procede y cancelar. | Sin modal de fusión ni acciones temporales ajenas al contrato. |
| Jakob | Selector de zona y entradas numéricas nativas. | Teclado real, botón para escribir y enlace para volver a Proyectos. |
| Semejanza | Siete días comparten estructura y mensajes. | Misma representación para días laborales y descanso. |
| Miller | Días agrupados por significado de semana. | Siete es el modelo del dominio; no se presenta como límite psicológico demostrado. |
| Parkinson | Los presupuestos no se amplían automáticamente. | Bloques con inicio/fin y respeto de cierre no existen en este recorrido; siguen pendientes. |
| Postel | Catálogo exacto y respuesta confirmada, sin coerción de datos inválidos. | API acepta aliases del catálogo e impide que 1e se convierta en cero. |
| Proximidad | Error de presupuesto junto al campo. | aria-invalid, foco visible y ausencia de PUT ante 1e verificados. |
| Prägnanz | Ausencia, borrador y confirmación son estados distintos. | Sin configurar no se interpreta como fila guardada ni fallo de lectura. |
| Región común | Tarjeta agrupa la disponibilidad personal. | No se mezcla con estado del proyecto ni tiempo trabajado. |
| Tesler | Servidor resuelve versión y concurrencia. | Carreras reales 200/412; revisión opaca y recuperación sin exponer SQL. |
| Modelo mental | Disponibilidad prevista es diferente de trabajo realizado. | Aviso explícito junto al total; no se crea evento ni sesión de trabajo. |
| Usuario activo | Primer guardado disponible desde ausencia. | Recorrido real guarda zona y siete valores sin proyecto previo obligatorio. |
| Pareto | Edición de una semana y zona en un formulario. | Prioridad de producto; no se afirma un porcentaje de uso o productividad. |
| Fin de pico | Confirmación cierta y salida explícita. | No-op estable y Cancelar descarta únicamente borrador; guardado contradictorio no muestra éxito. |
| Sesgo cognitivo | Cero permite descansar sin penalización. | Se guardan ceros y total derivado; no hay racha, presión ni tiempo inventado. |
| Sobrecarga de opciones | Catálogo nativo y siete presupuestos, sin ventanas arbitrarias. | Búsqueda nativa del selector, sin duplicar la autoridad del catálogo mediante Intl. |
| Doherty | Guardando disponibilidad aparece antes de finalizar el PUT. | MutationObserver verificó feedback menor de 400 ms con respuesta retenida; red/servidor no tienen garantía de ese plazo. |

## Límites

Axe no certifica toda WCAG ni demuestra facilidad de uso. Viewports, teclado y motores son pruebas locales; no se han probado móvil/tablet físicos, teclado virtual ni lector de pantalla real. No se heredan resultados de otras funciones para completar esta matriz. Los errores de respuesta se inyectan, mientras guardado, concurrencia, sesión y persistencia usan API y PostgreSQL reales. No se repiten las seis rutas del publicador porque disponibilidad no las modifica ni genera eventos personales.

## Motores, zoom e inspección visual final

Sesión 50590 terminó con código 0. Firefox 155 y WebKit 26.6, mediante Playwright 1.63.0, completaron el recorrido de persistencia de disponibilidad: 2/2 en 22,4 segundos. La matriz de 28 anchos no se multiplicó por esos motores. No se atribuye nueva garantía SameSite a WebKit Windows a partir de este recorrido.

Zoom real de Chromium mediante extensión propia y `chrome.tabs.setZoom(2)`: ancho interior 1426→713, DPR 1,5→3; ventana de 654 produce ancho interior 320, documento 312/312 sin desplazamiento horizontal. Se verificó PUT real desde la UI y lectura posterior del presupuesto confirmado. No es CSS zoom ni reducción artificial del viewport presentada como zoom.

El agente de integración inspeccionó las capturas finales de escritorio, móvil, zoom a 320 y navegación a 720: controles, etiquetas, total y descanso legibles; texto de navegación sin solape. El coordinador recibe las mismas imágenes para revisión adicional. Archivos en outputs/availability-{desktop,mobile,real-zoom-320}.png, availability-navigation-720.png y availability-real-zoom.json. El foco visible del encabezado en la vista recién abierta se conserva; no se oculta para embellecer la captura.

La corrección posterior de comportamiento 400 se verificó de forma focal, según los resultados siguientes; la revisión visual no sustituye esa evidencia.

La corrección de error 400 quedó verificada después en el bundle CIX_-ttO, con el mismo CSS Codz1mIb: un recorrido con dos mensajes inválidos pasó 1/1 (3,5 segundos), conserva borrador, exige recarga deliberada y permite el posterior guardado válido sin navegación de formulario. No cambió el diseño ni se repitieron capturas/matriz por esa guarda lógica. Detalle de la inyección y límites en tdd_availability_integration.md.

Foco desde presupuesto: una comprobación adicional detectó pérdida de foco al enviar con Enter y deshabilitar el input, tras éxito y 503. La evidencia original se conserva. La corrección revisada se verificó en CpU8JHCd: regresión permanente de Enter con éxito/503/400 y movimiento deliberado a un enlace externo, junto a la regresión 400 previa, **2/2 verdes en 4,2 segundos**. El foco regresa al input salvo cuando la persona elige otro destino, que se conserva. CSS Codz1mIb sin cambios; esta corrección no invalida las mediciones visuales previas ni justifica atribuir otra matriz no ejecutada.

Verificación final de foco en motores adicionales, bundle CpU8JHCd sin reconstrucción: Firefox 1/1 (2,6 segundos), WebKit 1/1 (2,7 segundos), sesión 84087 EXIT 0. Los cuatro resultados de la regresión Enter pasan en ambos motores. El destino externo se elige mediante Shift+Tab hacia Cerrar sesión y no se activa; conserva foco después del PUT. Tab hacia delante no había alcanzado de forma fiable los destinos elegidos en esos motores, por lo que se documenta el ajuste del recorrido, sin atribuirlo a un cambio productivo ni afirmar que toda navegación por teclado queda certificada. La matriz de 28 anchos y el zoom siguen siendo evidencia Chromium.
