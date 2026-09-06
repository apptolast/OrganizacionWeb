# TDD cliente API — availability

Contrato aprobado, sección 10 y firmas acordadas con frontend leídos. Ponytail full/Caveman lite. Init compartido 27065: 798 backend, 647 frontend y lint verdes. Ownership exclusivo availability-api.ts y availability-api.test.ts; coordinador revisa esta API. Sin backend ni UI ajenos, sin mutación antes de aprobación.

1. GET ausencia: RED por módulo ausente, después GREEN 1/1 con lectura mínima y ETag. Test exige ruta exacta, cookies, no-store y signal. Tipos discriminados y claves diarias acordados; el contenido mínimo todavía no valida respuestas adversas.

2. Respuesta de ausencia y HTTP: RED 27/28. GREEN 28/28 al exigir HTTP 200, cuatro campos exactos, false/tres null y tag de ausencia. Cada error conserva Response original; objetos/escalares mal formados producen Error controlado, no TypeError.

3. Configuración existente: RED 3 casos válidos, GREEN 31/31 con lectura mínima discriminada. Conserva zona histórica textual sin consulta de catálogo; claves diarias pactadas.

4. Validación configurada: RED 48/83; los otros casos permanecieron verdes y no se atribuyen como RED nuevos. GREEN 83/83 con cuatro campos, mapa cerrado de siete enteros en rango, fecha UTC real con microsegundos y ETag configurado fuerte dentro de BIGINT. Fecha/tag válidos se conservan exactamente, sin interpretar su versión en la UI.

5. Catálogo: RED por export ausente, GREEN 84/84 con endpoint mínimo. Catálogo inválido y HTTP inesperados: RED 18/102; GREEN 102/102. Se exige cuerpo items cerrado, strings no vacíos, UTC, orden estricto y ausencia de duplicados; no se filtran aliases ni se normalizan con Intl. Cookies, no-store y signal se mantienen.

6. PUT mínimo: RED por export ausente, GREEN 103/103 con payload raíz cerrado, CSRF, If-Match, cookies y signal. Confirmaciones incompatibles: RED 10/113, GREEN 113/113 al exigir configured true y comparar zona/siete números con copia de la intención enviada. Incluye mutación del objeto del caller durante espera para no comparar con valores que nunca se enviaron.

7. Regresión del comportamiento reutilizado: 146/146 verdes a la primera, sin fabricar RED. Incluye fechas tipo objeto/array y años expandidos válidos, fecha fuera del rango Date, siete ceros y máximos, no-op confirmado, todos los errores PUT, JSON truncado, red sin retry, cancelación 401 tardía y notificación de pérdida de acceso vigente para los tres endpoints. ESLint de ambos archivos y TypeScript global terminaron EXIT 0. Se añadió además UUID mayúsculo manteniendo prefijo availability minúsculo para no esconder esa frontera detrás de un prefijo inválido: 147/147 verde.

## Entrega para revisión

API y test exclusivos liberados, sin cambios en módulos compartidos. Exports pactados: DAY_KEYS/DayKey/DailyMinutes, AvailabilityInput, AvailabilitySnapshot discriminado, readAvailability(signal?), readAvailabilityZones(signal?) y saveAvailability(input,etag,signal?). El snapshot configurado no verifica pertenencia al catálogo al leer; la UI contrasta el catálogo antes de guardar y el servidor mantiene su validación. PUT devuelve tipo configurado sólo después de verificar cuerpo/tag e intención copiada antes del await. Sin reintentos, nuevas dependencias ni almacenamiento persistente en navegador.

La revisión independiente corresponde al coordinador; no se ha ejecutado mutación de esta API. E2E/UX esperan el freeze conjunto de backend/UI.

## Refuerzo del mutante 31 — discriminante de snapshot

La revisión independiente del coordinador identificó en el original `mutation-availability/mutation-original.json` el mutante 31, `ConditionalExpression`, `src/availability-api.ts:40:5–40:29`, reemplazo `true` de `data.configured === true`. Aceptaba configured false cuando el resto de los cuatro campos y el ETag eran configurados válidos.

Se añadió una prueba pública de ese cuerpo inconsistente. El código original rechaza la respuesta y la suite API pasa **148/148**, EXIT 0 (salida 79638a). Es un refuerzo de evidencia sobre producción correcta, no un RED ficticio de implementación.

Se prepara replay Stryker con el mecanismo existente, restringido a esa expresión; configuración temporal bajo `.e2e-work`, sandbox separado `.stryker-tmp-api-review`, reporte `frontend/reports/mutation-availability/api-replay.json`. No se cambia el archivo productivo. SHA256 previo: `24e01d1ec98516b36ccf468c95ad4c4f50f2b2dab4e6629aa1eefb1b2ed3c6cd`. Resultado de replay pendiente en este momento.

Replay terminado en sesión 78110: **3/3 Killed, 0 Survived, 0 NoCoverage, 0 timeouts y 0 errores**, EXIT 0, 50 segundos. El mutante original 31 corresponde al ID 0 del replay por identidad exacta de archivo, ubicación y reemplazo: ConditionalExpression, línea 40 columnas 5–29, `true`. Fue eliminado únicamente por el nuevo test 569 (`@s41 rejects false discriminant despite otherwise valid configured snapshot`); el reporte registra «promise resolved … instead of rejecting», prueba del falso éxito que detecta la nueva aserción.

SHA256 productivo posterior idéntico al previo: `24e01d1ec98516b36ccf468c95ad4c4f50f2b2dab4e6629aa1eefb1b2ed3c6cd`. Stryker mutó exclusivamente su copia aislada; no hubo producción temporal que restaurar ni cambios de lógica. ESLint del test terminó con código 0. Este 100 % se refiere sólo a las tres variantes de la expresión seleccionada; no reemplaza el score original ni convierte las otras equivalencias revisadas por el coordinador en mutantes detectados.
