# UX de exportación 22

API simulada explícitamente; navegador y descarga son reales. Este informe no
acredita snapshot SQL, autenticación HTTP ni integración del backend 22. Esas
puertas siguen pendientes de A/C. Puerto local 18081, sin usar 8080 ni modificar
backend. Chromium, Firefox y WebKit de Playwright 1.63 instalado.

## Resultados y límites

Primer recorrido Chromium RED 8844ac: respuesta válida y feedback de 6 ms, pero BODY
conservaba foco tras deshabilitar el iniciador. Nueve fuentes iguales al before.
Corrección mínima aprobada por root: al terminar, recuperar control conectado
o encabezado si desapareció, sólo cuando permanece la intención y foco en BODY.
Mismo recorrido GREEN c5643f; DOM 22/22 y build 631d34. No RED JSDOM inventado.

WebKit rechazó correctamente la simulación que perdía charset (ec9006).
Playwright coreBundle.js:49263 separa mimeType por punto y coma al interceptar.
El fixture ahora entrega los mismos bytes mediante node:http local efímero;
no modifica ni relaja el cliente. Nominal WebKit GREEN afcb74. Cabeceras y bytes
de diagnóstico conservados en export-browser-webkit-diagnostic.

Teclado, preparación, dos descargas nativas exactas con un GET, Cancelar,
413 y movimiento voluntario (control o después BODY): 6/6, log
export_browser_three_final.log, herramienta 2b6618. Archivo personal de 886 bytes,
nombre seguro y contenido exacto; cuenta vacía simulada, no hechos reales.

Geometría:31 anchos de 320 a 2560, incluidos ambos lados de breakpoints 360/420/
600/700/1000/1100/1600. Cuatro estados (inicial, pendiente, error 503, preparado),
LIGHT/DARK mediante SYSTEM, texto 200%. 256 medidas y 8 axe por motor;768 medidas y
24 axe sin violaciones en total. Logs export_browser_geometry_initial.log y
export_browser_geometry_engines.log (a15e8a/28e58b). Capturas 320 y 1440 por estado
y tema, texto 200 a 320. Se midieron 44 px, límites horizontales y overflow.

SYSTEM cambia con el SO, forced-colors, reduced-motion y paisaje 768×360:
3/3, export_browser_modalities.log (56858a). Foco visible 3 px y centro no ocluido;
cero animaciones activas. Axe de contraste corresponde a temas normales; no se
extrapola a forced-colors, donde se verificó geometría/foco y colores nativos.

Zoom nativo Chromium 200% mediante chrome.tabs.setZoom: factor 2, DPR doble y
320 px CSS, sin overflow (d7f0d4, export_browser_native_zoom_short.log). Los dos
intentos previos con perfil/extensión bajo la ruta larga de output agotaron 30 s;
el segundo también agotó teardown. Se preservan logs. La ruta corta aislada
.e2e-work/export-zoom-PID permitió arrancar en 1.3 s; no se aumentaron timeouts.
Firefox/WebKit tienen reflow/texto 200, no se les atribuye ese zoom nativo.

Root observó jerarquía invertida en preparado. Se reutilizaron primary-link
para Descargar y secondary-link para Preparar de nuevo, conservando enlace
nativo y foco. Captura nueva 320 revisada: acciones completas y separadas.

Límites reales: no dispositivos físicos, teclado virtual, lector de pantalla
real ni evaluación humana de facilidad de uso. No cobertura universal. La
simulación no sustituye el E2E integrado. La campaña de mutación posterior se registra por separado en mutation_export_data_frontend.md.

## Matriz de los 30 principios

| Principio | Aplicación y evidencia | Resultado |
| --- | --- | --- |
| Atención selectiva | Descargar primario tras preparado; captura 320 revisada | Verificado visualmente |
| Carga cognitiva | Sin campos ni requisitos que recordar; ayuda contextual | Revisión de recorrido; evaluación humana pendiente |
| Estética-usabilidad | Dos temas, estados y24 axe; mensajes recuperables | Verificado en entorno descrito |
| Posición en serie | Hoy primero, Exportación al final; Tab coherente | DOM y navegador |
| Tendencia a la meta | No porcentaje ni avance ficticio | No aplica cálculo de progreso |
| Von Restorff | Enlace de descarga sólido y preparar de nuevo secundario | Capturas de preparado |
| Zeigarnik | Cancelar permite abandonar sin obligación; no toca borradores | DOM y navegador; no nueva persistencia |
| Fluir | Exportación no inicia ni extiende sesiones | No aplica temporizador |
| Fragmentación | Ayuda, preparación y archivo como etapas distintas | Capturas y semántica |
| Memoria de trabajo | Contexto del propietario visible; reintento manual | Verificado; no formulario nuevo |
| Navaja de Occam | Preparar y segundo gesto nativo, sin wizard | Revisión de controles |
| Conectividad uniforme | No gráficos ni relaciones añadidas | No aplicable |
| Fitts | Controles/nav 44 px medidos en31 anchos | Verificado; táctil físico pendiente |
| Hick | Preparar o Descargar según estado; Cancelar durante espera | Revisión visual y flujo |
| Jakob | Botones y enlace download nativos; teclado real | Tres motores y descarga exacta |
| Semejanza | Tokens y clases existentes, dos temas | Capturas y geometría |
| Miller | Agrupación por etapa sin imponer número de opciones | Revisión; comprensión humana pendiente |
| Parkinson | No modifica comienzo/fin de bloques | No aplicable |
| Postel | Transporte cerrado, Unicode/long preservados en cliente | 32 pruebas de cliente; no validación de registros |
| Proximidad | Ayuda antes de preparar, estado y enlace juntos | Capturas en error/éxito |
| Prägnanz | Estados textuales, sin iconos como única explicación | Nombres y roles accesibles |
| Región común | main único y área de preparación | Revisión semántica y capturas |
| Tesler | Bytes/cabeceras verificados antes del segundo gesto | Cliente y descarga nativa |
| Modelo mental | Copia JSON no equivale a importar/restaurar | Ayuda contractual visible |
| Usuario activo | Entrada sin consulta y botón reconocible | Navegación DOM; prueba humana pendiente |
| Pareto | Exportación completa con dos gestos, sin formatos extra | Revisión de alcance |
| Regla del pico y final | Archivo preparado no afirma guardado en disco | Estado y descarga exacta |
| Sesgo cognitivo | Sin métricas de productividad ni promesas de éxito falso | Revisión de textos |
| Sobrecarga de opciones | Un formato aprobado, sin menú innecesario | Revisión de controles |
| Doherty | Feedback de 6 ms con respuesta retenida; sin porcentaje | Chromium medido; resto dentro de 400 ms |

## Captura nativa y cierre de gates

La captura estándar `zoom200.png` quedó vacía incluso al traer la pestaña al frente y esperar dos frames. Se conserva y no acredita legibilidad. En la misma pestaña, zoom y tamaño, `Page.captureScreenshot` con `fromSurface: false` produjo `zoom200-compositor.png`, inspeccionada por B y root: texto legible, Descargar destacado y Preparar de nuevo secundario. Es la porción desplazada del recorrido, sin reescalar el PNG. Evidencia: `.e2e-work/export-browser-native-zoom-compositor`; log `export_browser_native_zoom_compositor.log` (ede775). Las métricas originales permanecen separadas.

Regresión final: 2295/2295 pruebas, 56 archivos (cbb659). El pase anterior 2294/2295 queda preservado: la expectativa heredada colocaba Apariencia al final; el contrato 22 añade Exportación después. Se corrigió sólo esa expectativa, manteniendo Hoy primero y los demás oráculos. Arnés de mutación: 63/63 (80810d), sin ejecutar campaña todavía.
