# Revisión del coordinador — integración y evidencia visual

El coordinador no escribió producción ni E2E de esta funcionalidad. Revisó `e2e/read-projects.spec.mjs`, la adaptación de argumentos de `scripts/e2e.mjs`, las consultas JDBC, la validación del cursor, los hooks de lectura y las capturas del navegador.

- Fixture exclusivo con credenciales sintéticas. SQL y limpieza se limitan al stack de pruebas; no se usa el servidor del usuario. La comparación de filas antes/después comprueba que leer no escribe proyectos ni eventos.
- Recorrido real: POST, lista, detalle, recarga y continuación por cursor. Las respuestas 503/401 interceptadas prueban presentación y recuperación; no se presentan como caídas reales del backend. La ausencia de credenciales también se comprueba con una solicitud real independiente del contexto autenticado de Playwright.
- Hallazgo corregido en la revisión: la comparación de mensajes 404 buscaba campos inexistentes. Se pidió comparar el cuerpo completo de las respuestas ajena/inexistente. El resultado del E2E final se registra por separado.
- Hallazgo del coordinador en producción backend: un Instant válido pero fuera del rango del almacenamiento podía convertirse en un error 500/503. El autor reprodujo y corrigió la validación del cursor con pruebas HTTP de las fronteras PostgreSQL.
- La matriz automatizada inspecciona lista/detalle, textos largos, controles táctiles, teclado y axe. No equivale a una auditoría completa de usabilidad ni a pruebas en dispositivos físicos.

## Zoom y capturas

El control nativo de Windows falló al validar la identidad de Chrome for Testing, incluso tras recuperar la ventana; se dejó de usar esa vía. El agente de integración comprobó zoom real mediante `chrome.tabs.setZoom(2)` y `getZoom()` en una extensión y perfil de pruebas aislados. La API oficial se documenta en [Chrome tabs](https://developer.chrome.com/docs/extensions/reference/api/tabs#method-setZoom).

Las mediciones muestran DPR 1,5 → 3 y ancho interior 1426 → 713 manteniendo el exterior en 1440. Una segunda comprobación conserva zoom 2 y reduce la ventana hasta 320 píxeles CSS. No se usa zoom CSS ni emulación de pinch. Perfil y extensión propios se eliminan al acabar.

Las primeras capturas estaban recortadas por la combinación de zoom y captura de Playwright; el coordinador rechazó esa evidencia visual. Las capturas corregidas usan la superficie del navegador con las dimensiones DIP correspondientes, sin transformar posteriormente la imagen. El coordinador inspeccionó lista y detalle completos a 320 píxeles CSS: texto legible, HTML literal, acciones accesibles y distribución sin solapes visibles. Los valores originales y el helper se registran en la bitácora de integración.

No hay bloqueos adicionales encontrados en la revisión de integración. La aceptación final depende de la ejecución conjunta de E2E, el arnés y la mutación de las fuentes nuevas; sus resultados no se anticipan en este informe.
