# Integración — complete_reopen_task

## Corte inicial: preparación anterior al freeze

Ponytail full/Caveman lite. Contrato aprobado de 36 escenarios y sección 9 leídos. El cliente API se entregó por TDD y fue revisado favorablemente por el coordinador; bitácora separada tdd_complete_reopen_task_api.md. Este documento no presupone pruebas de navegador ni servidor.

Cinco recorridos preparados en e2e/complete-reopen-task.spec.mjs: completar/reabrir/completar por UI y retener historia tras limpieza del outbox; 21 transiciones, paginación real y dos PUT concurrentes con un ganador; privacidad de estado/historia; conflicto y error de historial sin repetir escritura; matriz de 22 anchos, historial poblado, teclado y feedback medido antes de liberar PUT. Descubrimiento Playwright: cinco tests; no ejecutados funcionalmente todavía. Las matrices HTTP exhaustivas pertenecen al backend, no se duplican como navegación.

Los beforeEach históricos agregan task_status_history explícitamente al TRUNCATE del fixture porque la nueva FK impide truncar tasks sin incluir su historia. No se usa CASCADE ni se cambian datos fuera del proyecto aislado. El test de conservación elimina exclusivamente los eventos sintéticos de la tarea bajo prueba y comprueba que su historia sigue disponible.

publisher-smoke.mjs mantiene las cinco rutas previas y prepara una sexta: TaskStatusChanged.v1 con nueve campos. Durante la misma caída Rabbit existente se completa una subtarea, se consulta historia y se observa retry pendiente; después se comprueba JSON original, cola quorum/ruta y retención tras el mismo reinicio con worker detenido. Selección por proyecto y taskId, con una transición sintética de ese tipo en este fixture. Sólo pasó node --check; no se atribuye RED/GREEN funcional al smoke aún.

Se espera freeze conjunto antes de construir imágenes. No se leen ni alteran los dos temporales cuya eliminación fue bloqueada. Próximos fixtures tendrán nombres nuevos y limpieza acotada propia. Quedan ejecución sobre corte final, capturas/zoom nativo, matriz UX de treinta filas y revisión independiente backend. No se ejecuta mutación sin aprobación del coordinador.
Revisión del coordinador aplicada antes de ejecutar: fixture page declarado en paginación; tras reload se exigen tres transiciones, no sólo encabezado. El recorrido 412 espera historial vacío confirmado antes del cambio externo y una entrada tras Consultar estado vigente, de modo que detecta un historial obsoleto. Discovery mantiene cinco casos; todavía no se ejecutaron contra imagen de feature 9.

## Corte final en navegador

Sesión 83167: pnpm test:e2e terminó EXIT 0, 43/43 casos Chromium en 3,3 minutos. Incluye los cinco nuevos y toda regresión anterior, sin reintentos agregados de otros cortes. Imagen web C1K_ahR7 / CSS DBt1XW6J; imágenes organizationweb-e2e-2808. Matriz nueva de 22 anchos y axe verde; feedback de estado medido en 4 ms con PUT retenido. Fixture, red y volumen propios eliminados por finally.

El coordinador detectó antes de ejecutar un bloque de reinicio colocado por error dentro de cleanup del smoke. Se movió al try principal, después de verificar mensajes con worker parado y antes de dispose/down; contexto completo y sintaxis revisados. No se presenta como RED de producto. Se excluyen frontend/reports y frontend/.stryker-tmp existentes del contexto Docker tras acabar transferencia, sin necesidad de reconstruir producto por esa exclusión.

## Publicación y reinicio

Sesión 88526: pnpm test:publisher EXIT 0. Seis rutas reales verificadas. Con Rabbit detenido y worker habilitado, PUT de tarea confirmó 200 e historia duradera; el outbox reintentó pendiente y luego publicó TaskStatusChanged.v1 original de nueve campos. Se verificaron ruta task.status-changed.v1, cola quorum duradera, messageId y payload original; la misma parada/reanudación de Rabbit conservó el mensaje con backend detenido. Al arrancar después el backend, la misma sesión recuperó historia, DTO y ETag idénticos. No se repitió matriz de crash; limpieza del stack y volúmenes propios completada.

## Visual y motores adicionales

Sesión 94713 EXIT 0: Firefox 155 y WebKit 26.6 con Playwright 1.63.0 pasaron el recorrido real de completar/reabrir/historia/recarga, 2/2 en 10,2 segundos. Es un smoke acotado, no toda la matriz en esos motores; no se añade afirmación de enforcement SameSite a WebKit Windows.

La inspección detectó falta de espacio antes de UTC en TaskState. El autor corrigió únicamente ese espacio JSX. Un intento del helper no insertó el build por diferencia de salto de línea y repitió las capturas antiguas; no se usa como validación de la corrección. El helper corregido sí ejecutó build web y terminó EXIT 0, sesión 64507: captura móvil confirma el espacio. No se reconstruyó backend ni se repitió su suite/publicador.

Zoom nativo mediante extensión chrome.tabs.setZoom(2) en perfil aislado: innerWidth 1426→713, DPR 1,5→3; ventana ajustada a 654 da innerWidth 320 y scrollWidth/clientWidth 312. Controles y enlaces del main miden como mínimo 44 × 44. PUT real completó tarea y mostró tres transiciones a 200 % y 320 CSS. Capturas finales outputs/complete-reopen-task-desktop.png, mobile.png, real-zoom-320.png y complete-reopen-task-real-zoom.json. Inspección de integración: regiones, estado, UTC e historia legibles, sin recortes de página. Fixtures y perfiles propios cerrados/limpiados; no se tocaron temporales bloqueados.

Identidad final de la captura: index.html leído desde la imagen web confirma JavaScript CAKNgGJM (espacio UTC) y CSS DBt1XW6J; el corte previo de 43 E2E usaba C1K_ahR7. Se mantienen ambas evidencias separadas.
