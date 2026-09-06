# Mutación frontend13

Corte de fuentes759271b, manifestbf99fa5 en reschedule_frontend_freeze.json. Revisión de composición APPROVED parcial y alcance revisado: siete archivos, TaskBlocks completo22:0–223:1; seis archivos completos. No exclusiones nuevas ni reducción de umbral80. Directorio protegido permanece en ignorePatterns.

Init integrado6673 EXIT0bc2678:1495frontend/28archivos,18scripts, lint y backendBUILD SUCCESSFUL6177b4. No se modificaron fuentes durante la ejecución; commits posteriores sólo de evidencia. No representa E2E13 ni cierre backend.

Se inicia la campaña mediante node .harness/harness.mjs mutate reschedule-frontend. Pendiente de resultado; no se declara score ni feature13done.

## Resultado del baseline con dos procesos

Ejecución18743 finalizada179d24 EXIT0,79min3s, el6 de septiembre a20:19:03. Reporte original conservado en frontend/reports/mutation-reschedule/mutation.json y HTML.1416 mutantes:1226 Killed,178 Survived,10 NoCoverage,2 RuntimeError y0 Timeout. Score Stryker86,70% sobre1414 evaluables; contando los dos errores como no detectados,1226/1416=86,58%. Se supera80, pero faltan revisión de huecos y cierre funcional.

Errores171 y180 en change-submit.tsx: mutaciones OptionalChaining de onRejected(problem) y problem.code. El adaptador Stryker/Vitest falla al representar una excepción con `TypeError: Cannot convert object to primitive value`, después de dos reinicios internos. Evidencia037af6: no son Killed, equivalentes ni errores de aplicación reparados. Conservar el informe y registrar el límite de herramienta durante revisión.

## Medición controlada de concurrencia

Existe autorización de rendimiento para comprobar ocho procesos, condicionada a cero Timeout y reproducción de la campaña controlada del mismo alcance. Ahora existe ese baseline. Se mantienen las fuentes/tests y los1416 candidatos; sólo se cambia temporalmente concurrency2→8 y el directorio de informes para no sobrescribir el original. El manifest original conserva los hashes de baseline; la excepción es esta configuración operativa declarada.

Se vuelve a usar el mismo comando del harness. La prueba no intenta reparar ni ocultar RuntimeError171/180; compara rendimiento y clasificaciones, conservando ambas ejecuciones separadas. No se adoptará la configuración por una puntuación mayor si cambian estados por contención o aparecen timeouts. Resultado pendiente; fuentes frontend siguen congeladas hasta terminar esta medición.
