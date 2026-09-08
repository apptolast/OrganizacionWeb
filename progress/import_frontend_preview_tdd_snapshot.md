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
