# UX — split_task

Corte congelado con web DDEzlNbV. La corrida Chromium ejecutó 38 casos: 37 pasaron y falló una búsqueda histórica de foco de read_projects. Sus cinco casos nuevos de subtareas pasaron. La espera del encabezado y Shift+Tab sustituyeron el bucle fijo de 12 Tab; el replay de ese caso pasó 1/1 en 17,5 segundos, sin modificar producción. No se presenta la combinación como una corrida 38/38.

La matriz de subtareas comprobó 22 anchos CSS: 320, 359, 360, 361, 390, 480, 599, 600, 601, 699, 701, 768, 820, 1024, 1099, 1101, 1280, 1440, 1599, 1601, 1920 y 2560; altura de 400 en 768. Sin overflow de página, controles y enlaces del main de al menos 44 × 44 CSS. Incluye enlace de hijo A, padre P, vuelta al proyecto y controles de estado del proyecto. No se midieron tarjetas en lugar de sus enlaces. Axe no detectó violaciones en sus reglas WCAG A/AA. El recorrido abrió UUID en mayúsculas y confirmó el contexto canónico.

Zoom nativo mediante chrome.tabs.setZoom(2), perfil Chromium aislado: innerWidth 1426 a 713, DPR 1,5 a 3; ventana ajustada a 654 produjo innerWidth 320 y scrollWidth/clientWidth 312. Enlaces y controles conservaron al menos 44 × 44. El formulario creó una subtarea mediante POST real a 200 % y 320 CSS. Capturas outputs/split-task-desktop.png, split-task-mobile.png, split-task-real-zoom-320.png y JSON split-task-real-zoom.json. Inspección de integración sobre escritorio/móvil: texto y relación legibles, enlace corto identificable, controles separados, sin recortes de página. El contorno del título es el foco visible.

Firefox 155 y WebKit 26.6, con Playwright 1.63.0, pasaron 2/2 en 13,1 segundos: creación por API real, jerarquía de varios niveles, contrato plano preservado, relación padre, navegación y recarga. No es una matriz completa en esos motores. Una primera selección de tests del helper falló con No tests found por argumentos con espacios; corregida la selección, ambos tests se ejecutaron. No se atribuye enforcement SameSite nuevo a WebKit Windows.

| Principio | Aplicación | Evidencia y límites |
| --- | --- | --- |
| Atención selectiva | Relación y Subtareas ocupan regiones distintas. | Capturas y semántica accesible; atención humana no medida. |
| Carga cognitiva | Cada vista muestra padre directo e hijos directos. | Nieto real no aparece como hijo del abuelo; no se carga un árbol completo. |
| Estética-usabilidad | SCSS mantiene espaciado y tarjetas de tareas. | Inspección visual; preferencia humana pendiente. |
| Posición en serie | Contexto precede lista y creación. | DOM y capturas mantienen el orden. |
| Tendencia a la meta | Cada hijo permite un criterio comprobable. | El progreso medido no aplica todavía; no se verifica avance porcentual ni agregación. |
| Von Restorff | Crear subtarea identifica la escritura principal. | Botón explícito; enlaces de navegación subrayados. |
| Zeigarnik | La jerarquía persiste al salir y recargar. | Tres niveles reales y navegación después de reload. |
| Fluir | La descomposición prepara resultados pequeños. | Sesiones de concentración y su continuidad no aplican todavía; pendiente del recorrido temporal futuro. |
| Fragmentación | La tarea puede dividirse en resultados pequeños. | Padre, hijo y nieto conservan identidades diferentes. |
| Memoria de trabajo | Los errores de guardado conservan tres campos. | HTTP 400/503/red inyectados; sólo reintento deliberado. |
| Navaja de Occam | Reutiliza campos, API común y controles nativos. | Sin editor de árbol ni paquetes adicionales. |
| Conectividad uniforme | El vínculo al padre indica la relación real. | GET parent y navegación; no se presenta raíz durante un error. |
| Fitts | Enlaces y controles miden al menos 44 CSS. | Medición real de enlaces A/P, contexto y formulario en 22 anchos y zoom. |
| Hick | Una creación por nivel y paginación explícita. | 21 hijos reales y regreso a recientes; sin opciones de árbol global. |
| Jakob | Enlaces navegan, botones escriben y campos son nativos. | Tab, Shift+Tab y Enter reales; entrada numérica incompleta rechazada. |
| Semejanza | Los hijos conservan la presentación de tarea. | DTO de ocho campos y filas con criterio, estado y estimación. |
| Miller | Tres campos agrupados por resultado. | No se atribuye un límite psicológico arbitrario a la cantidad de hijos. |
| Parkinson | No hay extensión automática de tiempo al crear hijos. | Bloques con inicio y fin quedan pendientes de planificación/sesiones; las estimaciones independientes no verifican ese recorrido. |
| Postel | Validación definida sin coerción silenciosa. | API revisada por el coordinador; campos estrictos, UUID semánticos y errores recuperables. |
| Proximidad | Error y formulario permanecen asociados. | aria-invalid, foco en título y conservación de borrador. |
| Prägnanz | Pendiente y padre se expresan con texto. | No dependen sólo de color o icono; axe y capturas. |
| Región común | Subtareas agrupa lista y formulario. | Región accesible diferenciada del estado del proyecto. |
| Tesler | Servidor resuelve identidad, relación y continuación. | FK, cursor con proyecto/padre y Location; datos reales en E2E. |
| Modelo mental | Un hijo sigue siendo tarea del mismo proyecto. | Lista plana conserva todos los niveles; GET hijos muestra sólo el nivel directo. |
| Usuario activo | Descomponer desde el detalle de una tarea. | Navegación real entre tres niveles y creación en zoom estrecho. |
| Pareto | Título, criterio y estimación cubren el corte. | No se afirma una proporción de productividad o uso. |
| Fin de pico | Confirmación sucede después del guardado. | POST retenido, un envío y lectura posterior muestran el hijo confirmado. |
| Sesgo cognitivo | No se sugiere que dividir equivalga a avanzar. | Sin progreso inventado ni suma temporal automática. |
| Sobrecarga de opciones | Sólo padre directo e hijos, sin movimiento ni borrado. | Alcance contractual preservado; completed y recuperación cubiertos en tests frontend/backend y regresión de proyecto. |
| Doherty | Guardando subtarea anuncia la espera. | MutationObserver midió 1 ms desde submit, antes de liberar POST; no es latencia universal. |

No se probaron dispositivos físicos, teclado virtual ni lector de pantalla real. Viewport/touch son emulación. Axe no certifica WCAG completa. Los errores de recuperación se inyectan en navegador; persistencia, sesiones, privacidad, relaciones y paginación usan API y PostgreSQL reales. La inspección de este agente no es independiente sobre tasks-api.ts y E2E, que escribió; el coordinador revisa esos archivos. La revisión backend sí es independiente. El smoke RabbitMQ se registra por separado, sin repetir la matriz histórica de crash.