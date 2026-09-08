# Revisión UX de credenciales para integraciones (24)

Corte funcional revisado: freeze 33D925EBA26F6EE211A6BB91D377954A2F85A44DB7D5FF171D04992AFF45BB86. Commit frontend 82a45c2 incorpora además la guarda Unicode (ciclo 068), sin cambio de DOM/CSS para entradas válidas. Todas las solicitudes del navegador se simulan explícitamente; esto no sustituye E2E con API/PG reales.

## Evidencia y límites

- `integration24_browser_production.log`: 6/6, creación/cierre del secreto y recuperación manual de creación/revocación en Chromium, Firefox y WebKit. Teclado real, foco tras controles deshabilitados/desmontados y GET inicial único.
- `integration24_browser_modalities.log`: 3/3, texto 200 % verificado por tamaño computado, 320/768/1280, LIGHT/DARK mediante SYSTEM y preferencias del navegador, movimiento reducido y colores forzados. Nombres de 80 puntos de código y seis scopes. Sin overflow de página y objetivos de 44 px. Axe normal completo; sólo color-contrast se omite en colores nativos forzados, no en LIGHT/DARK.
- `integration24_browser_native_zoom.log`: 1/1 Chromium, zoom real mediante extensión 2, DPR duplicado y viewport 320 CSS. Otros motores tienen texto ampliado/reflow, no se afirma zoom nativo para ellos. La captura del compositor es visible y fue inspeccionada; muestra la porción desplazada Caducidad/Crear, no toda la página simultáneamente.
- `integration24_browser_feedback.log`: 3/3 con PUT retenido; feedback medido 2.4/1/4 ms según JSON de cada motor, inferior a 400 ms sin respuesta ni éxito anticipado. Son mediciones locales, no SLA de red.
- Unitarios: corte 66/66 más ciclo Unicode focal GREEN separado; privacidad, almacenamiento, identidad, deduplicación y errores con originales RED conservados.
- Primer ensayo de desarrollo conservado: `integration24_browser_recovery_initial.log`, 1/3 por dos GET iniciales observados bajo StrictMode en Chromium/WebKit. Los mismos oráculos pasan en producción. No se alteró el conteo para obtener verde.

Capturas representativas, bajo `.e2e-work/`:

- `integration-browser-production/integration-api-browser-in-24746-ive-readability-s33-s37-s41-chromium/light-prepared-320.png` y `light-prepared-1280.png`: revisadas por root.
- `integration-browser-modalities/integration-api-browser-in-add62-nd-existing-media-modes-s41-chromium/light-text200-320.png`: inspeccionada; los nombres artificiales sin espacios se parten para preservar el ancho y no ocultar texto.
- `integration-browser-native-zoom/integration-api-browser-in-ef1a3-ative-Chromium-zoom-200-s41-chromium/zoom200-compositor.png`: inspeccionada, sin reescalar el PNG.

## Matriz de los 30 criterios

«Verificado» se limita al comportamiento o medición descritos. Las valoraciones de comprensión/jerarquía son revisión heurística, no un estudio con usuarios ni una certificación por axe.

| Criterio | Aplicación y evidencia | Resultado / límite |
| --- | --- | --- |
| Atención selectiva | Crear principal inicialmente; secreto antes del formulario y Copiar principal al prepararlo. Capturas 320/1280 revisadas. | Verificado visualmente; revisión heurística. |
| Carga cognitiva | Nombre, permisos explícitos y una caducidad; ayuda local y secreto con selección. | Recorrido verificado; comprensión pendiente de uso real. |
| Estética-usabilidad | Tokens de temas existentes; errores explicados, fecha UTC legible; tres motores y axe. | Verificado visual y funcionalmente, sin inferir facilidad por estética. |
| Posición en serie | DOM y teclado mantienen el mismo orden; foco de retorno al H1 si desaparece el iniciador. | Verificado en navegador. |
| Tendencia a la meta | No existe objetivo/progreso cuantificado en esta gestión. | No aplicable; no se inventa progreso. |
| Von Restorff | Acción primaria diferenciada por posición, texto y superficie; secreto agrupado. | Verificado visualmente. |
| Zeigarnik | Intención owner/id conservada antes del PUT; incertidumbre recuperable manualmente. | Verificado unitario y navegador; no persistir formulario/secreto es requisito de privacidad. |
| Fluir | No se inicia ni extiende ninguna sesión de trabajo. | No aplicable a credenciales; no altera el recorrido de sesiones. |
| Fragmentación | Permisos agrupados por función y cada credencial en su región. | Verificado; no confunde proyectos/tareas con credenciales. |
| Memoria de trabajo | Borrador permanece ante rechazos en la vista; tras reload se explica reintroducción e identidad retenida. | Verificado; el secreto nunca se recupera de almacenamiento. |
| Navaja de Occam | Cada control crea, copia, cierra, consulta, pagina o revoca; sin OAuth ni formularios externos. | Revisión heurística del recorrido. |
| Conectividad uniforme | No hay diagramas ni conexiones decorativas. | No aplicable; no se sugieren relaciones inexistentes. |
| Fitts | Controles y labels de checkbox >=44 px, anchos dentro del viewport con texto normal/ampliado. | Verificado geométricamente y por teclado en tres motores. |
| Hick | Seis permisos independientes visibles, sin menús anidados; recuperación sólo cuando corresponde. | Revisión heurística, sin afirmar tiempos de decisión medidos. |
| Jakob | Inputs, checkbox, select, botones y enlace privado convencionales; ruta directa sobre login real. | Verificado unitario y navegador. |
| Semejanza | Temas, bordes, foco y acciones secundarias reutilizan estilos existentes. | Verificado visualmente en ambos temas. |
| Miller | Agrupación por permisos, caducidad y credencial; no se impone un número mágico de opciones. | Revisión heurística. |
| Parkinson | No hay duración de sesión ni extensión automática; caducidad 7/30/90 es explícita. | Regla de sesiones no aplicable; selector verificado. |
| Postel | White_Space Unicode, 1–80 puntos de código, emoji válido y surrogate aislado rechazado. | Verificado por ciclos focales; sin debilitar formato de credencial. |
| Proximidad | Nombre y error cercano, aria-invalid/aria-describedby retirados al corregir. | Verificado ciclo 065 y revisión DOM. |
| Prägnanz | Estados con texto (incierto, perdido, revocado, caducado), no iconos solos. | Verificado; vocabulario revisado heurísticamente. |
| Región común | Secreto, permisos, credencial y confirmación de revocación agrupados. | Verificado visualmente. |
| Tesler | Fecha/hora UTC legible, dateTime original; errores internos no se muestran. | Verificado ciclo 066 y capturas. |
| Modelo mental | Credencial autoriza solicitudes; no cambia estado de tarea/bloque/sesión al gestionarla. | Revisión de alcance; nominal real posterior separado en integration24-e2e-original-evidence.zip. |
| Usuario activo | Estado vacío, ayuda de permisos independientes y visualización única junto al formulario. | Recorrido nominal verificado; primera experiencia no medida con usuarios. |
| Pareto | Creación y gestión propia accesibles en una pantalla, sin eliminar paginación/recuperación. | Priorización de diseño, no porcentaje de uso observado. |
| Fin de pico | Confirmación cierta, secreto perdido explicado y revocación deliberada; fallos no dan falso éxito. | Verificado unitario y navegador. |
| Sesgo cognitivo | Caducidad modificable, scopes vacíos iniciales, sin selección implícita de lectura. | Verificado; no hay métricas de productividad ni presión. |
| Sobrecarga de opciones | Seis permisos acotados y tres caducidades, acciones de recuperación contextuales. | Verificado estructuralmente; comprensión pendiente de uso real. |
| Doherty | Indicador de trabajo antes de respuesta retenida, sin porcentaje inventado ni secreto anticipado. | Tres mediciones <400 ms; no garantiza latencia de servidor. |

Actualización de puertas: regresión frontend 2492/2492 y build/lint verdes; E2E nominal real 1/1 sobre 9a15cb5, con 577 entradas sin delta, conservado por separado en integration24-e2e-original-evidence.zip. Quedan mutación del universo revisado, regresión E2E completa y aceptación de despliegue. La matriz no declara la feature disponible en producción.

Ampliación solicitada por root: integration24_browser_wide.log, 3/3 inicialmente GREEN, comprueba sólo geometría LIGHT/DARK en 1920/2560 (12 medidas), sin repetir creación/recuperación. Capturas bajo .e2e-work/integration-browser-wide. El contenido conserva máximo 880 px y los controles 44 px, sin overflow de página. Con esta ampliación quedan medidos 320/768/1280/1920/2560 en tamaño normal; texto 200 % permanece en los tres anchos estrechos declarados.
