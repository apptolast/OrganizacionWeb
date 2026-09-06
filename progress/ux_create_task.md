# UX — create_task

Revisión del corte congelado. Los 32 E2E conjuntos pasaron en 2,4 minutos. El caso de tareas recorre 22 anchos CSS: 320, 359, 360, 361, 390, 480, 599, 600, 601, 699, 701, 768, 820, 1024, 1099, 1101, 1280, 1440, 1599, 1601, 1920 y 2560. Incluye ambos lados de los breakpoints pertinentes y altura de 400 en 768. Sin desbordamiento de página; controles de al menos 44 por 44; axe no detectó violaciones en sus reglas WCAG A/AA.

El zoom nativo de Chromium se fijó mediante chrome.tabs.setZoom(2), en perfil aislado. Manteniendo la ventana de 1440, innerWidth pasó de 1426 a 713 y DPR de 1,5 a 3. Al ajustar la ventana a 654, innerWidth fue 320 y scrollWidth/clientWidth 312. Los controles midieron 52, 112, 52 y 45 CSS de alto. El formulario confirmó un POST real en ese estado, sin CSS zoom ni emulación de pinch. Capturas y JSON: outputs/create-task-desktop.png, create-task-mobile.png, create-task-real-zoom-320.png y create-task-real-zoom.json. Inspección de integración: contenido legible, filas diferenciadas y sin recortes. Datos sintéticos identificados como prueba.

Firefox 155 y WebKit 26.6 ejecutaron la creación real, contrato HTTP, evento, recarga y proyecto intacto: 2/2 en 10 segundos, con Playwright 1.63.0. La matriz de anchos y zoom pertenece a Chromium. No se atribuye a esta prueba cobertura nueva del enforcement SameSite de WebKit Windows; permanece la limitación documentada en authentication.

| Principio | Aplicación | Evidencia y límites |
| --- | --- | --- |
| Atención selectiva | Tareas tiene encabezado y región propios. | Capturas y árbol accesible; atención humana no medida. |
| Carga cognitiva | Tres campos, dos opcionales en el contrato. | Creación real sin configurar reglas previas. |
| Estética-usabilidad | Tarjetas y formulario conservan SCSS y espaciado de proyectos. | Inspección visual; preferencia humana pendiente. |
| Posición en serie | Lista, explicación y formulario mantienen orden estable. | DOM y capturas. |
| Tendencia a la meta | El criterio describe un resultado concreto. | No se inventan porcentajes de avance. |
| Von Restorff | Crear tarea es la acción principal del formulario. | Contraste comprobado por axe, etiquetas visibles. |
| Zeigarnik | Las tareas pendientes se conservan al terminar un proyecto. | Cierre y reapertura reales sin completar tareas. |
| Fluir | Crear una tarea no inicia trabajo ni cronómetro. | No aplicable al flujo temporal todavía. |
| Fragmentación | El resultado y la estimación se separan por campos. | Etiquetas y orden de teclado. |
| Memoria de trabajo | Los errores mantienen los tres valores del borrador. | HTTP 400, 503 y fallo de red sin reenvío automático. |
| Navaja de Occam | Controles nativos y lista simple. | Sin componentes o capas especulativas. |
| Conectividad uniforme | Todas las tareas pertenecen al proyecto abierto. | API vinculada al proyecto; no existe jerarquía en este corte. |
| Fitts | Campos y botones de al menos 44 CSS. | Medición en 22 anchos y zoom estrecho. |
| Hick | Una acción de creación y paginación cuando procede. | 21 tareas reales; recuperación sólo ante fallo. |
| Jakob | Input, textarea, número y botones nativos. | Tab y Enter reales; exponente incompleto rechazado. |
| Semejanza | Filas repiten título, criterio, Pendiente y estimación. | Capturas y lectura en tres motores. |
| Miller | Campos agrupados por tarea, sin límite psicológico arbitrario. | Estructura revisada; comprensión humana pendiente. |
| Parkinson | La estimación no es tiempo trabajado. | Texto explícito y ausencia de registro temporal generado. |
| Postel | Tipos y límites definidos, sin coerción silenciosa. | Backend y cliente; «1e» produce cero POST. |
| Proximidad | Errores se asocian al campo y al formulario. | aria-invalid y foco en primer campo inválido. |
| Prägnanz | Estado pendiente escrito, no sólo coloreado. | Axe, texto y capturas. |
| Región común | Región Tareas agrupa lista y formulario. | Semántica accesible y separación visual. |
| Tesler | El sistema resuelve identidad, persistencia y continuación. | Location, SQL, cursor y recarga reales. |
| Modelo mental | Terminar proyecto no completa sus tareas. | Explicación visible, formulario oculto y reapertura deliberada. |
| Usuario activo | Crear desde el detalle del proyecto. | Recorrido real sin pantalla de configuración. |
| Pareto | Título, criterio y estimación cubren este corte. | No se atribuyen porcentajes de uso o productividad. |
| Fin de pico | Confirmación sólo tras respuesta guardada. | POST retenido, botón deshabilitado y un único envío. |
| Sesgo cognitivo | Estimación opcional, sin evaluar productividad. | No hay rankings, rachas o tiempo ficticio. |
| Sobrecarga de opciones | No hay estados de tarea editables ni jerarquía aún. | Alcance contractual preservado. |
| Doherty | Guardando tarea anuncia la espera y bloquea duplicados. | Submit + MutationObserver: 2 ms, menor de 400, antes de liberar POST. Caso reforzado 1/1 en 15,5 s; no es latencia universal. |

No se probaron dispositivos físicos, teclado virtual ni lector de pantalla real. Viewport y tap son emulación. Axe no certifica WCAG completa ni sustituye la evaluación humana. Los fallos de red de recuperación se inyectan en el navegador; persistencia, privacidad, sesiones, paginación y guardado usan API y PostgreSQL reales. El broker se valida por separado; no se repite la matriz de crash sin cambios de transporte.

Regresión posterior sin cambio visual: un reintento de lista terminado durante un POST retenido no impide la lectura posterior al guardado. El nuevo caso E2E pasó 1/1 en 4,0 segundos sobre imagen corregida, con tres GET, un POST y una tarea persistida. Las capturas y matriz anteriores mantienen su alcance visual; no se presentan como una nueva corrida conjunta.
