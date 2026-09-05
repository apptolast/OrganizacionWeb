# read_projects — navegador e integración

Contrato aprobado e init37769 verde confirmados por coordinador. Propiedad de e2e/ y adaptación mínima de scripts/e2e.mjs para reenviar argumentos Playwright: permite ejecutar sólo la feature nueva durante ciclos, conservando la suite conjunta por defecto.

Primer test antes de validación: creación real con HTML literal/Unicode, lista persistente, detalle por enlace y recarga, vuelta a primera página. No se simulan respuestas de API en ese recorrido. Backend/frontend trabajan sus propios ciclos; se ejecutará cuando el coordinador confirme disponibilidad para no confundir errores transitorios de compilación con defectos funcionales.

Plan acotado: paginación real21, límites de privacidad/errores, matriz12 anchos reutilizando proyectos reales, axe y teclado. Zoom CSS equivalente no se contará como zoom real; coordinador gestiona esa evidencia. Dispositivos físicos y teclado virtual se declararán pendientes si no se prueban.

## Ciclos ejecutados

- Creación → lista → detalle → recarga: primer intento funcional verde, 1 test en 8.4 segundos. La producción básica ya existía al ejecutarlo; no se atribuye un rojo inexistente.
- Paginación real: 21 proyectos guardados por POST, empate de createdAt controlado en PostgreSQL del fixture propio, primera página de 20 en orden UUID PostgreSQL, nueva creación intermedia, continuación de un elemento sin repetición, URL recargable, foco en h1 y vuelta al inicio. Snapshot exacto de proyectos/outbox demuestra que las lecturas no escriben. Primer intento verde, 1 test en 5.7 segundos.
- Privacidad/estado vacío: primer intento se detuvo durante build de frontend (exit 2) mientras el autor trabajaba. No se ejecutó el navegador y no cuenta como rojo de producto; se espera build verde del autor antes de repetir.

## Verificación final del navegador

El fixture propio se mantuvo entre comprobaciones para evitar reconstrucciones innecesarias. Su estado y helpers están bajo .e2e-work; sólo se modifican datos de su PostgreSQL. Se reconstruyeron backend/web una vez al congelar la fuente final.

- Chromium: 14/14 E2E verdes en 52.0 segundos (ocho históricos de creación y seis nuevos de lectura). Los selectores iniciales de dos tests confundían la flecha decorativa aria-hidden con parte del nombre; se corrigieron a nombres accesibles, sin modificar producción. El 404 ajeno/inexistente ahora compara objetos públicos completos tras revisión independiente del coordinador.
- 21 proyectos reales: orden PostgreSQL con empates, cursor estable tras nueva creación, página final y vuelta al inicio, foco y snapshot exacto sin escrituras.
- Autenticación real sin credenciales usa fetch de Node para evitar que Playwright herede sus credenciales globales. El primer intento de contexto anónimo heredó auth y se corrigió como defecto del fixture, no como fallo de autorización del servidor.
- Espera/error UI: respuestas 503 y401 interceptadas deliberadamente en navegador, sin presentarlas como caída real del servidor; Reintentar consulta API real. Última medición desde navegación hasta anuncio accesible: lista209 ms y detalle171 ms, por debajo de400 ms en este entorno local. No equivale a garantía de tiempo de red.
- Matriz Chromium: 320,360,390,480,600,768,820,1024,1280,1440,1920,2560 píxeles CSS, ambas vistas con nombre de120 y descripción de4000 puntos de código y palabras largas. Incluye altura400 en768, ausencia de overflow, controles principales44×44, axe WCAG2/2.1/2.2, navegación de teclado y foco. Un contexto táctil emulado verifica tap y maxTouchPoints; no es un teléfono físico.
- Smoke adicional Firefox/WebKit: dos recorridos reales creación/lista/detalle/recarga/HTML literal y axe, ambos verdes en9.9 segundos. Configuración optativa e2e/cross-browser.config.mjs; no multiplica la matriz ni sustituye Chromium canónico en CI.

## Zoom nativo y capturas

El intento de UI nativa del coordinador quedó bloqueado por identificación inconsistente de ventana en sky. Se utilizó una extensión MV3 exclusivamente dentro de un perfil Chromium temporal propio. [chrome.tabs.setZoom](https://developer.chrome.com/docs/extensions/reference/api/tabs#method-setZoom) fija2 y getZoom lo confirma; [chrome.windows.update](https://developer.chrome.com/docs/extensions/reference/api/windows#method-update) ajusta únicamente esa ventana. No se usa CSS zoom, pinch ni emulación de viewport para atribuir esta evidencia.

Baseline: outerWidth1440, innerWidth1426, DPR1.5. Zoom2: misma outerWidth, innerWidth713, DPR3. Ventana reducida: outerWidth654, innerWidth320, DPR3. Lista/detalle mantienen scrollWidth=clientWidth (705/705 y312/312). Navegación ida/vuelta operativa. JSON y capturas en outputs/read-projects-real-zoom*. El perfil y extensión temporales se eliminan al terminar.

Las primeras capturas de Playwright recortaban la derecha con zoom; no se aceptaron como evidencia visual. Se corrigió la captura directa CDP con clip en DIP equivalente a CSS×zoom, sin editar la imagen: PNG de320 mide936 píxeles (312 CSS×DPR3). Se espera h1 del proyecto confirmado antes del detalle. El coordinador inspeccionó las capturas corregidas y confirmó que son completas y legibles. Las capturas responsive normales también se copiaron a outputs/read-{list,detail}-{320,1440}.png.

Límites explícitos: no dispositivos físicos, teclado virtual ni todas las variantes de hardware/lectores de pantalla; los 30 principios UX incluyen valoración humana no sustituible por axe. Matriz del autor en progress/ux_read_projects.md y revisión global a cargo del coordinador.

Teardown final: proceso del fixture exit 0; Chromium propio cerrado, stack organizationweb-e2e-61160 y su volumen eliminados. Estado de entorno temporal borrado. No se detuvo ningún servicio ajeno ni se desplegó en el servidor.
