# Replay focal de prioridades frontend13

Refuerzo aprobado por root; baseline init6673 vigente y foco203/203 GREEN8ff2d0. Ponytail full/Caveman lite. No nueva campaña completa ni cambios de fuentes/tests.

Selección72fd6a: trece firmas originales comprobadas por archivo, ubicación, mutador y reemplazo, con fuente embebida idéntica a la actual (normalizando sólo CRLF/LF para comparación; hashes conservan bytes). Manifest: `reschedule_frontend_priority_replay_selection.json`, incluye config original en base64 y hashes previos de siete fuentes, cinco tests, configuración y reportes baseline/benchmark.

Root autoriza reutilizar temporalmente configuración fija del destino existente. Sólo mutate y rutas JSON/HTML cambian; concurrency8, perTest, thresholds90/80/80 e ignorePatterns protegido se conservan. Restauración exacta al terminar. CLI local instalada confirma rangos `file:startLine:startColumn-endLine:endColumn`; no infraestructura nueva.

Ejecución: `node .harness/harness.mjs mutate reschedule-frontend`, sesión85007, lanzamiento431926. Log separado `progress/reschedule_frontend_priority_replay.log`; reporter `frontend/reports/mutation-reschedule/replay-priority.json` y `.html`. Instrumentación00ba20: cinco fuentes y16 mutantes. Resultado pendiente: los adicionales se distinguirán de los trece candidatos por firma; no se adjudica Killed por intención ni se reclasifican RuntimeError171/180 originales.

Incidencias de lectura previas: dos patrones rg con wildcard en argumento de ruta de PowerShell dieron error123; corregidos usando directorio y glob de rg. No rechazo de aprobación ni acceso a archivos protegidos. No afecta al runner de mutación.

## Incidencia de selección y corrección

Primer intento85007 EXIT0/36749e, 2m21s.16 mutantes:12 Killed,3 Survived,1 RuntimeError,0 Timeout/NoCoverage. Stryker80% excluye el error; bruto12/16=75%, no PASS bruto. Sólo tres firmas solicitadas estuvieron presentes:397 Killed,184 Killed,186 RuntimeError. Los otros13 resultados son adicionales; no acredita selección completa. Informe/log originales de este intento conservados.

Causa verificada bff8d4: `project-reader.js:196–205` interpreta columnas mutate sin restar1, mientras `object-utils.js:80–87` convierte posiciones internas a JSON sumando1. Mi primer manifest copió columnas JSON sin conversión. Config restaurado y todos hashes previos idénticos ffeae0.

Segundo intento96363/bff8d4: columnas start/end menos1, líneas originales; comprobación reversible de las trece ubicaciones antes de ejecutar. Manifest separado `reschedule_frontend_priority_replay_corrected_selection.json`; reporter `replay-priority-corrected.json/html`, log propio. Conserva políticas y ejecución por arnés. Al terminar se exigirá correspondencia exacta de las trece firmas, no sólo un conteo total.

El error186 reproduce fallo del adaptador Stryker/Vitest `TypeError: Cannot convert object to primitive value` tras dos reinicios internos. No se modifica dependencia ni se atribuye Killed; los RuntimeError171/180 históricos permanecen intactos.

## Resultado final observado

Segundo intento96363 finalizado2049a3 EXIT0, 2m7s. Dry run748 tests GREEN. Correspondencia378c09 exige exactamente una coincidencia por firma para los13 candidatos: **11 Killed,1 Survived,1 RuntimeError**. Los36 adicionales:33 Killed y3 Survived. Total49:44 Killed,4 Survived,1 RuntimeError; cero Timeout/NoCoverage. Score bruto44/49=89,80%, Stryker44/48=91,67%; ambos superan80, sin omitir el error del informe. Esto es PASS del umbral focal, no cierre de13 ni resolución de todos sus huecos.

| Original | Replay | Estado |
| --- | --- | --- |
|60|0|Killed|
|183|3|Killed|
|184|4|Killed|
|186|6|RuntimeError|
|258|9|Killed|
|394|28|Killed|
|396|30|Killed|
|397|31|Killed|
|417|34|Killed|
|703|35|Survived|
|1113|36|Killed|
|1145|42|Killed|
|1432|48|Killed|

703 sigue siendo trabajo pendiente: la mutación cambia la inicialización de consentimiento false a true; el nuevo caso mixto no la detecta. No se justifica como equivalente ni se modifica producción/test durante medición.186 conserva error del runner, no detección. Los tres supervivientes adicionales corresponden a originales61 (contador value-1),1115 (predicado de exceso true) y1117 (exceso >=0); quedan registrados sin ampliar autoría ni declarar equivalencias.

Manifest de resultados `reschedule_frontend_priority_replay_results.json`: firmas, IDs anteriores/nuevos, motivos, killedBy, todos los adicionales y hashes antes/después. Restauración exacta de config confirmada378c09, siete fuentes/cinco tests/reportes baseline y benchmark idénticos. Root guardó tests en checkpoint c524d26 durante medición sin alterar sus bytes. Los dos intentos y sus logs se conservan; no se sobrescribió ningún reporte previo ni se tocaron rutas protegidas. No commits/push por este agente.
