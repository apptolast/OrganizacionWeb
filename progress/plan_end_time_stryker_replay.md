# Propuesta de único replay dirigido17

Reutilizar el mecanismo aprobado de close16 attempt3 (15/15Killed, doce firmas presentes), sin copiar sus rangos ni reclasificar el original17 de85,22%. Esta preparación no crea configuración ni ejecuta suites.

1. Tras freeze y revisión de A/B, fijar las firmas exactas del JSON original17: archivo, ubicación, mutador y reemplazo; IDs sólo como referencia. Verificar que pertenecen a las siete fuentes de stryker.end-time-notification.config.json y que los hashes productivos siguen iguales.
2. Derivar una configuración local dedicada de esa base17, manteniendo Vitest/vite.config.ts completo, perTest,8workers, umbral80 e ignorePatterns protegido exacto. Sustituir únicamente mutate por rangos mínimos de las firmas dentro de esas siete fuentes, no siete archivos completos; ninguna nueva exclusión de ramas o tests.
3. Convertir columnas JSON base1 a selector base0 (restar1 en ambos extremos; mantener líneas). Implementación instalada verificada1244fc: filterMutatePattern resta1 sólo a líneas del selector, reportPositionToStrykerPosition resta1 a ambas coordenadas. Prevalidar cada firma contra el rango interpretado antes de lanzar.
4. Usar reports/mutation-end-time-notification-replay/ y .stryker-tmp-end-time-notification-replay, con log/raw/manifest propios. Conservar intactos JSON/HTML originales y registrar sus hashes; snapshot antes/después de fuentes, tests, config y entradas del runner.
5. Una ejecución directa autorizada tras review de configuración; exigir presencia de todas las firmas pedidas y enumerar también extras generados por sus rangos, todos los estados y EXIT real. No bajar umbral si el foco queda por debajo, no repetir automáticamente ni sumar denominadores al original. Ausencia de firma o error exige diagnóstico y decisión root.

Siguiente paso: recibir los dos paquetes de pruebas y sus firmas; revisar independientemente los oráculos de A. No se anticipa cuántos mutantes generará el rango ni su resultado.

## Configuración preparada para revisión

COMMON frontend/stryker.end-time-notification-replay.config.json y progress/end_time_stryker_replay_targets.json creados22cbc2, formato GREEN. Catorce firmas exactas del original:14/52/62/155/217/225/226/228/357 y1138/1142/1147/1227/1228. Nueve rangos mínimos;217 contiene225/226/228,1138 contiene1142,1227/1228 comparten ubicación. El manifiesto conserva cada firma individual y su rango seleccionado, sin confundir cobertura del rango con detección.

Prevalidación ejecutada contra ProjectReader.prototype.filterMutatePattern instalado: los catorce rangos interpretados coinciden con coordenadas0 derivadas del JSON. Fuentes de los cuatro archivos seleccionados comparadas byte textual normalizado con source del informe; todos pertenecen al allowlist original de siete. No se mutan los otros tres archivos completos ni se excluyen sus tests.

Incidente de preparación55b6cf: importar directamente project-reader.js expuso dependencia circular de MUTATION_RANGE_REGEX antes de inicialización. No creó config ni ejecutó campaña. Se cargó primero el entrypoint público core y después el parser interno (56d609); validación y generación22cbc2 terminaron EXIT0. No cambio a librería ni workaround del runner.

Comando propuesto, todavía NO ejecutado: pnpm --dir frontend exec stryker run stryker.end-time-notification-replay.config.json. Antes requiere aprobación root y freeze A/B; root preservará129inputs más configuración dirigida antes/después. No variar umbral ni reintentar automáticamente ante resultado insuficiente/error. Raw original SHA8E0BF31D8D2CC753700E8946E11516F929ED20A35C7D38D836EC9D8E8FE9EB4A intacto.
