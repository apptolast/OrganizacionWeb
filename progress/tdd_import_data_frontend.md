# TDD frontend23

Contrato aprobado en5feebd7; sección23 SHA959BE37196F6E07FFAF969C1824BB33C86DCAE3E6DBD4872DF15437BEDF49369. Se aplica Ponytail full/Caveman lite, un test por ciclo. Sólo PRIMARY import-data; no COMMON/V14 ni8080. Baseline oficial suministrado por root; no se repite global por ciclo.

Cada ciclo ejecuta `pnpm --dir frontend test -- src/import-data-api.test.ts`. Logs propios `progress/import_frontend_NNN_red.log` y `_green.log` conservados. Todos los RED siguientes fueron EXIT1 y su GREEN EXIT0, sin timeout/retries ni tests anticipados.

| Ciclo | Oráculo público y causa RED | GREEN focal |
| --- | --- | --- |
| 001 | @s18/@s34 File original, POST y CSRF existentes, preview nominal14 vacío; módulo ausente | 1/1 |
| 002 | @s18 envelope con campo adicional antes aceptado | 2/2 |
| 003 | @s18 preview de otro owner antes aceptado | 3/3 |
| 004 | @s18 hash de otros bytes antes aceptado | 4/4 |
| 005 | @s17 File32 MiB+1 antes llegaba a fetch; ahora rechazo previo a envío/lectura | 5/5 |
| 006 | @s18 byteLength discordante antes aceptado | 6/6 |
| 007 | @s40 señal ya abortada antes enviaba | 7/7 |
| 008 | @s40 aborto durante fetch antes consumía JSON | 8/8 |
| 009 | @s40 aborto durante JSON antes leía File | 9/9 |
| 010 | @s40 aborto durante lectura File antes invocaba Web Crypto | 10/10 |
| 011 | @s40 digest terminado después de aborto antes aceptado | 11/11 |

Refactor002: fixture con hash real para que envelope inválido no quede encubierto cuando se añade guardia de hash; resultado2/2 EXIT0 en `_002_refactor.log`. Tags iniciales corregidos a escenario18 (preview cerrado), sin cambiar oráculos.

El cliente aún es parcial: transporte/identidad/bytes/abortos, no validación completa de counts, formato, timestamps, runningSessions ni status/errores. Confirmación, recibos, intención persistida, UI y actualización de snapshots siguen pendientes. No se atribuye cierre de cliente, UI o feature a estos once tests. No global, mutación ni Git propios.

## Corte temprano de preview

Ciclos012–026, uno cada vez: formato, versión tipada, timestamp civil inválido, counts incompletos, colección extra de insertCounts, fracción coherente, suma discordante, límite total100001, runningSessions noarray, dos sesiones, campoextra, UUID inválido, timestamp sin microsegundos, aviso sin inserción y response503 no consumida como preview. Cada `_red.log` EXIT1 y `_green.log` EXIT0 con conteo creciente de12 a26; no se añadieron casos por lote.

Refactor final conserva guardias y expone ImportPreview tipado. El primer tsc encontró incompatibilidad sólo de declaraciones NodeFile/DOMFile (BYOB stream): la prueba usa File nativo de Node para arrayBuffer, ausente en JSDOM, con adaptación del constructor únicamente en tests y comentario explícito. No se cambió producto para acomodar ese fixture. Tipos finales, ESLint y Prettier EXIT0 en import_frontend_preview_checks.log; focal26/26 EXIT0 en import_frontend_preview_freeze.log. No build/global ni campañas.

Freeze temprano: dos fuentes del cliente/fixture, bitácora y dos logs finales inventariados en import_frontend_preview_freeze.json. Cubre transporte y decoder de vista previa, no confirmación/recibo/errorUI ni flujo de importación completo. Las familias de errores se entregan como Response al futuro consumidor para clasificación segura. Faltan positivos de frontera/sesión con datos, implementación de confirmación y recuperación, intención persistida y UI. No se afirma que los26 tests validen todos los174 ejemplos backend.

## Intención mínima y positivos del cliente

Ciclos027–036: lectura nominal; retirada por propietario distinto sin tocar otras claves; campos privados extra; JSON malformado; key inválida; digest mayúsculo; guardado nominal; almacenamiento que descarta silenciosamente; omisión de datos incidentales del caller; limpieza acotada. Cada ciclo tuvo RED EXIT1 y GREEN EXIT0 en sus logs numerados. Todavía son pruebas del módulo, no acreditan integración logout ni UI.

037 acepta exactamente32 MiB de File y100000 proyectos en counts, archivo con registros generados y padding JSON válido;038 acepta warning con null legado;039 conserva timestamp histórico con6 microsegundos. Los tres fueron inicialmente GREEN, sin cambio productivo ni RED fabricado. Son validaciones del cliente con Response simulada; no acreditan importación backend de esos registros.

040: la hipótesis de que el regexp admitía LF final no se reprodujo: `_040_red.log` contiene realmente EXIT0/11 pruebas. Se conserva el nombre original del log y se clasifica como inicialmente GREEN, no RED. Se retiró la comprobación redundante de longitud añadida provisionalmente; sólo quedó refactor de retorno tipado de intent. Focal combinado40/40 EXIT0 en import_frontend_040_refactor.log. No hay un bug corregido atribuible a040.

La bitácora del freeze inicial se conserva exactamente en import_frontend_preview_tdd_snapshot.md; el manifiesto inicial mantiene su referencia histórica a la versión anterior de esta bitácora. Las dos fuentes liberadas por root se siguen desarrollando después de su revisión parcial, sin reescribir los resultados iniciales. Próximo: cliente de confirmación/recibo y después consumidores UI.

## Cliente de confirmación y consulta

041–054: confirmación nominal File/key/hash/CSRF; rechazo de bytes diferentes antes de POST; respuesta409 conservada; recibo con campo extra; key ajena; hash distinto; byteLength distinto; recordedAt inválido; counts incompletos; IMPORTED sin inserciones; suma100001; recuperación nominal sin File; longitud imposible en recuperación; key de ruta inválida. Cada ciclo tuvo RED EXIT1 y GREEN EXIT0, una prueba cada vez. Se extrajeron lectura/status/guardas y hash acotado comunes conservando los oráculos de preview;043_refactor EXIT0. Ningún cambio de api-client global.

055 (404 conservado) y056 (recuperación IMPORTED en fronteras32 MiB/100000) fueron inicialmente GREEN. Resultado56/56 en2 suites, EXIT0 en import_frontend_client_final.log. Tipos y ESLint EXIT0 antes del formato final; el primer check de Prettier falló únicamente en import-data-api.test.ts y se conserva en import_frontend_client_checks.log. Reaplicar Prettier a ese archivo y check de cuatro fuentes EXIT0, log import_frontend_client_format_fix.log. Cambio sólo de formato, no se repite la suite por ello.

Este corte expone ImportPreview, ImportReceipt e ImportIntent sin casts de datos recibidos ni parser de registros del archivo. Freeze: cuatro fuentes, tres logs finales y copia inmutable de bitácora, con import_frontend_client_freeze.json. No acredita integración global: errores de storage se entregan al futuro consumidor; sesión/ruta/UI, foco físico y refresh siguen en cola. No Java/proxy ni campañas propias.
