# UX — complete_reopen_task

Corte congelado de feature 9, web C1K_ahR7 y CSS DBt1XW6J. Suite Chromium 43/43 verde en 3,3 minutos, incluidos los cinco casos nuevos. La matriz verificó 22 anchos CSS (320, 359, 360, 361, 390, 480, 599, 600, 601, 699, 701, 768, 820, 1024, 1099, 1101, 1280, 1440, 1599, 1601, 1920 y 2560), con altura 400 en 768: sin desbordamiento horizontal, enlaces/controles del main de al menos 44 × 44 y axe WCAG A/AA sin violaciones detectadas. No se heredan resultados visuales de features anteriores. La corrección posterior añadió sólo un espacio antes de UTC; la nueva imagen web y capturas se verificaron en sesión 64507.

| Principio | Aplicación | Evidencia y límites |
| --- | --- | --- |
| Atención selectiva | Estado de la tarea e Historial son regiones distintas. | Semántica accesible; atención humana no medida. |
| Carga cognitiva | Una transición disponible según estado confirmado. | No se presentan combinaciones arbitrarias ni estados de proyecto como estado de tarea. |
| Estética-usabilidad | SCSS y jerarquía mantienen el estilo existente. | Escritorio/móvil/zoom inspeccionados: texto y controles legibles, sin recortes; preferencia humana no medida. |
| Posición en serie | Estado precede la historia duradera. | Orden DOM explícito y fechas por transición. |
| Tendencia a la meta | Completar registra un resultado logrado. | Sin porcentaje agregado ni inferencia sobre avance del proyecto. |
| Von Restorff | Completar/Reabrir nombran la única escritura del control. | No depende de un icono sin etiqueta. |
| Zeigarnik | Estado e historia se recuperan tras recargar. | Recorrido real conserva tres transiciones; publicación operativa no es historia. |
| Fluir | Completar permite continuar con otro resultado. | Sesiones de concentración y continuidad temporal no aplican todavía. |
| Fragmentación | Cada tarea cambia de estado por separado. | No se propaga a padres, hijos ni proyecto. |
| Memoria de trabajo | Conflicto conserva contexto y ofrece consultar estado vigente. | Recuperación deliberada sin repetir PUT. |
| Navaja de Occam | Botones nativos y snapshot único. | Sin nueva biblioteca de estados ni visualización de árbol. |
| Conectividad uniforme | Historia pertenece a la tarea abierta. | Cursor vinculado a proyecto/tarea; contexto visible. |
| Fitts | Controles y enlaces deben medir al menos 44 × 44 CSS. | Medición real verde en 22 anchos y zoom nativo 200 % con innerWidth 320. |
| Hick | Una transición y paginación explícita de historia. | Sin menú de estados arbitrarios. |
| Jakob | Botones escriben y enlaces navegan. | Teclado real, sin focus() que salte el recorrido. |
| Semejanza | Etiquetas de estado coinciden en lista/detalle/control. | El DTO previo conserva sus ocho campos. |
| Miller | Snapshot pequeño y página de veinte transiciones. | Veinte es tamaño técnico, no supuesto límite psicológico. |
| Parkinson | Completar/reabrir no modifica estimaciones ni extiende tiempo. | Bloques con inicio/fin siguen pendientes de planificación y sesiones. |
| Postel | API valida respuesta y revisión antes de permitir una escritura. | Sin coerción de estados ni sustitución de fecha. |
| Proximidad | Error y recuperación están junto al control afectado. | Fallo de historia no invalida transición confirmada. |
| Prägnanz | Pending/completed se representan mediante texto claro. | No se depende sólo del color. |
| Región común | Estado e historia se agrupan por tarea. | Regiones accesibles diferenciadas del estado del proyecto. |
| Tesler | Servidor resuelve concurrencia y orden duradero. | ETag opaco, versión interna y transacción PostgreSQL. |
| Modelo mental | Reabrir conserva haber completado anteriormente. | La fecha actual se limpia; el historial mantiene la transición previa. |
| Usuario activo | La acción está disponible desde el detalle. | Cambio real y recuperación desde el mismo contexto. |
| Pareto | Dos intenciones cubren el corte de resultado. | No se afirma proporción de productividad o uso. |
| Fin de pico | Confirmación sólo tras PUT válido. | Historial posterior y estado confirmado; no éxito optimista. |
| Sesgo cognitivo | Completar un padre no completa sus hijos. | Sin progreso ni tiempo trabajado inventados. |
| Sobrecarga de opciones | No hay edición del historial, borrado ni cambios masivos. | Alcance aprobado conservado. |
| Doherty | Cambiando estado de la tarea anuncia espera. | MutationObserver midió 4 ms desde click hasta el aviso con PUT retenido; no es una garantía universal de latencia. |

No se probarán dispositivos físicos ni lectores de pantalla reales en esta matriz. Axe no certifica WCAG completa; viewport/touch son emulación. Los errores de recuperación del navegador se inyectan; estado, historia, concurrencia, privacidad y sesiones usan API y PostgreSQL reales. El coordinador revisa el cliente API y la lectura del historial que escribió este agente; éste revisa independientemente las transiciones ajenas. Mutación y publicación tienen informes separados.


Zoom nativo chrome.tabs.setZoom(2) en Chromium con perfil aislado: innerWidth 1426→713 y DPR 1,5→3; ventana 654 produce innerWidth 320, documento 312/312 sin overflow. PUT real y tres transiciones visibles a ese zoom; controles/enlaces al menos 44 × 44. Capturas completas de escritorio, móvil y zoom en outputs/complete-reopen-task-{desktop,mobile,real-zoom-320}.png y mediciones JSON correspondiente. El espacio UTC corregido está visible en la captura móvil final. El contorno inicial del título indica foco visible.

Firefox 155 y WebKit 26.6 (Playwright 1.63.0) verificaron el recorrido real de estado e historia 2/2, 10,2 segundos. No se multiplicó toda la matriz por motor ni se atribuye nueva garantía SameSite a WebKit Windows. El publicador y el reinicio del backend conservaron historia/DTO/ETag, según bitácora separada.