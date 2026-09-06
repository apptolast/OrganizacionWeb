# Mutación Hoy: replay acotado

Estado: ejecución en curso; todavía sin veredicto ni score.

Autorización: judge aprobado por root, checkpoint `1f7090eb6a114194622d45184a84be4904f302bb`, init14639 EXIT0/a6e120 (1353 frontend,15 scripts y lint; backend1415 previo sin cambios). Rol mutation_tester independiente de autores de producción y pruebas unitarias, con Ponytail full y Caveman lite.

Comando: `node .harness/harness.mjs mutate today-frontend-replay`. Sesión13403, PID Stryker68396. Inicio15:52:28, evidencia46008e: cuatro fuentes instrumentadas con113 mutantes. Creación de dos workers e inicio del baseline perTest15:52:40, salida6235aa. El alcance por posiciones puede generar mutantes adicionales a las63 identidades originales: se mapearán individualmente al terminar, sin equiparar ambos denominadores.

Hashes previos de67 archivos permitidos registrados en `today_frontend_replay_hashes.json`; incluye fuentes/pruebas frontend, soporte del replay, manifest y JSON original preservado. Freeze de fuentes/tests/config hasta resultado. No se ejecuta una segunda campaña automáticamente, ni se modifica el reporte original. La región nueva del botón124 no tiene identidad original de atributo que pueda reivindicarse retrospectivamente.

## Resultado final

**Veredicto: PASS por umbral. Score bruto:107/113 =94,690265% (umbral80%).** EXIT0, salida721bcf; final15:56:46, duración informada4m18s. Baseline691 tests en58s, salida0d61da. Estados:107Killed,6Survived,0NoCoverage,0Timeout,0RuntimeError,0CompileError. No se cuentan errores ni timeout como kills.

Freeze liberado al finalizar y comprobar hashes812b54, antes de clasificación. Los67 archivos tienen hashes idénticos antes/después; original preservado SHA2565cc335b97919aaa1bcd3cf4cf956af54ee55db3a02fdb23a060c77508f5c47a3. No hubo segunda ejecución.

Informes nuevos: frontend/reports/mutation-today/replay.json (SHA256 b5b152fb890f79083935c6110b87a82f24eb9ee2681f8f7199f833e33f395dd7) y replay.html (SHA256 426a8cf4ed9063f14da839689df0804bb7f42613a67b56aaa3db91fbf7772d1f). El resultado original418/521=80,23% sigue separado e inmutable.

### Identidades seleccionadas y generación adicional

Mapping76bb72 verifica exactamente una coincidencia por cada identidad seleccionada mediante archivo, operador, sustitución y ubicación convertida. Resultado de los63 originales:61Killed y2Survived (96,825397%, denominador separado, no score de campaña). Otros50 mutantes generados por los rangos:46Killed y4Survived. El detalle completo está en today_frontend_replay_mapping.json.

La región nueva del botón124 generó sólo replay89 ArrowFunction, sustitución del handler por () => undefined, Killed. No generó un mutante directo del atributo aria-disabled: esta campaña no acredita su sensibilidad por mutación. Su corrección mantiene la evidencia unitaria/E2E previa, sin inventar kill de atributo.

### Supervivientes y siguiente oráculo

| Original | Replay | Ubicación y mutación | Dictamen |
|---|---|---|---|
|337|74|today.tsx61: force → false|Pendiente de autor/juez. El cleanup del efecto también aborta al cambiar revision, por lo que comprobar signal después de act no distingue aborto inmediato. Revisar un entrelazado público con petición pendiente, refresh forzado y resolución anterior al cleanup; si la composición impide observarlo, demostrar redundancia antes de clasificar E. No se declara E aquí.|
|412|88|today.tsx111: segundo true de refresh(true,true) → false|Pendiente de refuerzo. El test fractional visibility nuevo no aparece entre tests ejecutados para este mutante: visible inicia refresh/revision y sustituye el timer. Proponer petición manual ya pendiente antes de hidden/visible para conservar timer de visibilidad, callback con demora fraccional truncada antes del deadline y aserción de retirada de agenda mientras reemplazo sigue pendiente.|
|215|47|today-api.ts118: comparación UUID → true|Equivalencia contextual ya aprobada, regenerada incidentalmente; mantiene rechazo de entradas iguales imposibles en agenda válida sin solapes.|
|216|48|today-api.ts118: >= → >|Equivalencia ya aprobada: UUID iguales rechazados por Set.|
|219|51|today-api.ts118: UUID derecho toLowerCase → toUpperCase|Equivalencia contextual aprobada: comparación hexadecimal ASCII sólo se endurece; no admite descendente antes rechazado.|
|336|73|today.tsx61: force → true|Equivalencia contextual aprobada: rama no forzada sólo continúa sin petición pendiente; aborto extra de petición terminada.|

Las cuatro equivalencias incidentales no se descuentan del score bruto. Las dos seleccionadas siguen pendientes de decisión y este PASS de umbral no certifica cierre de todos los huecos. No se cambian fuentes/tests desde el rol de medición.

### Tabla de las63 identidades exactas

| Original | Replay | Archivo | Estado |
|---|---|---|---|
|225|54|src/today-api.ts|Killed|
|127|14|src/today-api.ts|Killed|
|171|19|src/today-api.ts|Killed|
|172|20|src/today-api.ts|Killed|
|173|21|src/today-api.ts|Killed|
|174|22|src/today-api.ts|Killed|
|175|23|src/today-api.ts|Killed|
|176|24|src/today-api.ts|Killed|
|177|25|src/today-api.ts|Killed|
|178|26|src/today-api.ts|Killed|
|181|29|src/today-api.ts|Killed|
|183|31|src/today-api.ts|Killed|
|185|33|src/today-api.ts|Killed|
|192|40|src/today-api.ts|Killed|
|211|43|src/today-api.ts|Killed|
|212|44|src/today-api.ts|Killed|
|213|45|src/today-api.ts|Killed|
|214|46|src/today-api.ts|Killed|
|217|49|src/today-api.ts|Killed|
|218|50|src/today-api.ts|Killed|
|224|53|src/today-api.ts|Killed|
|226|55|src/today-api.ts|Killed|
|232|57|src/today-api.ts|Killed|
|233|58|src/today-api.ts|Killed|
|18|0|src/App.tsx|Killed|
|28|1|src/App.tsx|Killed|
|29|2|src/App.tsx|Killed|
|32|5|src/App.tsx|Killed|
|39|12|src/App.tsx|Killed|
|40|13|src/App.tsx|Killed|
|286|60|src/today.tsx|Killed|
|292|61|src/today.tsx|Killed|
|309|63|src/today.tsx|Killed|
|315|66|src/today.tsx|Killed|
|329|70|src/today.tsx|Killed|
|331|72|src/today.tsx|Killed|
|337|74|src/today.tsx|Survived|
|347|75|src/today.tsx|Killed|
|366|76|src/today.tsx|Killed|
|368|78|src/today.tsx|Killed|
|370|80|src/today.tsx|Killed|
|378|81|src/today.tsx|Killed|
|380|83|src/today.tsx|Killed|
|397|84|src/today.tsx|Killed|
|399|86|src/today.tsx|Killed|
|412|88|src/today.tsx|Survived|
|426|90|src/today.tsx|Killed|
|443|91|src/today.tsx|Killed|
|445|93|src/today.tsx|Killed|
|452|94|src/today.tsx|Killed|
|465|95|src/today.tsx|Killed|
|466|96|src/today.tsx|Killed|
|467|97|src/today.tsx|Killed|
|468|98|src/today.tsx|Killed|
|469|99|src/today.tsx|Killed|
|470|100|src/today.tsx|Killed|
|471|101|src/today.tsx|Killed|
|479|104|src/today.tsx|Killed|
|481|106|src/today.tsx|Killed|
|484|109|src/today.tsx|Killed|
|486|111|src/today.tsx|Killed|
|491|112|src/today.tsx|Killed|
|536|114|src/workspace.tsx|Killed|
