# Mutación Hoy: diagnóstico final de dos identidades

**Veredicto de esta ejecución: FAIL. Score bruto:2/3 =66,666667% (umbral80%). EXIT1.**

Autorizado por root tras checkpoint565c5be0b8391b47862fd2526d7c6542a1a39f44, init78050 EXIT0/0ba43b (1354frontend,17scripts,lint; backend1415 previo sin cambios) y judge337/412 APPROVED7ae526. Rol mutation_tester independiente de producción y pruebas unitarias; Ponytail full/Caveman lite.

Comando: node .harness/harness.mjs mutate today-frontend-final. Sesión30040/PID53548. Inicio16:06:52, fin16:07:58; duración informada1m6s. Instrumentación492bef:1fuente,3mutantes. Baseline604tests en51s, GREEN. Finalea3333:2Killed,1Survived,0NoCoverage,0Timeout,0RuntimeError,0CompileError. El fallo explícito es score66,67 menor que80. Pnpm añadió después el mensaje «Command stryker not found», aunque Stryker ejecutó y escribió ambos informes; no se interpreta como mutador ausente ni se reintenta.

## Identidades exactas

|Original|Replay previo|Final|Mutación|Resultado y oráculo|
|---|---|---|---|---|
|337|74|1|today.tsx61 force→false|Killed por test595: @s28 @s30 an obsolete401 delivered in the deadline turn cannot revoke current access. El observer recibió401 una vez cuando debía permanecer sin llamadas.|
|412|88|2|today.tsx111 segundo true→false|Killed por test593: @s28 a fractional visibility deadline retires the old day while its replacement is pending. El mutante conserva Proyecto personal enDOM.|
|336 incidental|73|0|today.tsx61 force→true|Survived. Equivalencia contextual previamente aprobada: sólo añade aborto de petición terminada en recorrido no forzado que pasa la guarda pending. Se conserva en denominador bruto.|

Mapping exacto en today_frontend_final_mapping.json: archivo, operador, sustitución y posiciones1-based del informe comparadas con selector0-based del manifest, exactamente una coincidencia para cada objetivo. Resultado de objetivos:2/2Killed, distinto del bruto2/3; no se presenta como score de campaña ni convierte su FAIL en PASS. No falta test observable para336 bajo el contexto aprobado; si cambia esa composición se reabre la equivalencia.

## Integridad y límites

71 hashes antes/después idénticos (167e69), registrados en today_frontend_final_hashes.json. Incluyen fuentes/tests, configuraciones/manifest del replay y final, JSON original y JSON/HTML del replay anterior. Freeze liberado inmediatamente después del resultado y comprobación de hashes, antes de esta clasificación. No hubo segunda ejecución ni modificaciones a fuentes/tests/config durante medición.

JSONfinal SHA256 4a2dfc0b2eb542b8c2410edbad1b7930191998499154750ff7a5db280f08b824. HTMLfinal SHA256 c5c8db5ac6362249971f4f0e699b32f1034c422754d5f4b939732074ca2cdde6. Informes: frontend/reports/mutation-today/final.json y final.html, separados de original y replay.

Se conservan sin ajuste las campañas original418/521=80,23% y replay107/113=94,69%, con sus propios cortes y denominadores. Este diagnóstico diminuto falla80% aunque cierra los dos objetivos restantes; el juez evalúa el conjunto. No se descuentan equivalentes, no se añaden líneas ajenas y no se reduce umbral. La limitación de generación del atributo aria-disabled continúa documentada en el replay. Ningún contenido de rutas protegidas fue leído ni limpiado.
