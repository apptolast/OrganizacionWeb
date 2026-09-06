# Hoy: soporte de selección de mutación

Contrato a127747. Soporte autorizado por root, sin ejecutar PIT ni Stryker. Ponytail full y Caveman lite. No modifica producción de aplicación ni configuración backend.

## Ciclos del arnés

1. Test «today backend scope runs only its fixed PIT target»: RED ad6183, `Invalid target: today-backend`. Se añadió el nombre al conjunto cerrado y la única llamada `pitest --no-daemon -PmutationScope=today`. GREEN07891c, 1/1.
2. Test «today frontend scope runs only its fixed Stryker configuration»: RED3ec039, `Invalid target: today-frontend`. Se añadió el nombre cerrado y ejecución fija de `stryker.today.config.json` mediante pnpm. GREEN5068ca, 1/1.
3. Refuerzo de matrices existentes para rechazo de targets con argumentos/operación incorrecta y paso intacto de ambos nombres por CLI. Inicialmente GREEN; no se atribuye RED a estas filas.

Regresión scripts/project.test.mjs: 12/12 GREEN62d123. El runner inyectado sólo captura argumentos: no lanza Gradle, Docker ni Stryker. Se mantienen los tres targets schedule_block previos, ausencia/target vacío con ambas suites completas, rechazo antes de subprocess y validación de argumentos adicionales. `node --check scripts/project.mjs` y diff --check focal EXIT0 en la misma salida.

## Pendiente antes de ejecutar

La ruta frontend ya se selecciona, pero su configuración todavía no existe: depende del corte estable de la UI. Debe incluir today-api.ts, today.tsx y únicamente regiones compartidas realmente modificadas; actualizar también el scope por defecto tras inspeccionar los desplazamientos, sin incorporar CreateProjectScreen intacto. Mantener umbral80, perTest, concurrency2, ignorePatterns exacto del destino protegido e informes separados en reports/mutation-today. Backend build.gradle pertenece al autor backend y espera sus nombres finales. Este soporte parcial no certifica que una campaña sea ejecutable ni autoriza mutación antes del juez.

## Configuración sobre primer corte estable de UI

La pendiente anterior se resolvió tras build c9040d del autor: test de configuración RED66b491 por archivo ausente, GREEN8d6cd4 tras crear configuración y manifest. Regresión final13/13 GREEN0560e0, formato focal7c935f y diff --check sin errores. Configuración today: API/UI completas, App10–49 (CreateProjectScreen50+ intacta), Workspace27–54 y85, href ProjectReader61/157, condiciones use-session29/189. Columnas finales comprobadas contra fuente ae141a; no se inventan longitudes. Default conserva targets previos y sustituye regiones desplazadas de App/Workspace, añade Today y href; use-session ya completo. Exports/formato de schedule-block-api no añaden comportamiento y su archivo completo permanece en default. Navigation sigue intacto.

`today_frontend_mutation_scope.json` registra hashes de bytes del corte y rangos, sin resultados de mutación. El posterior fix de frontera de medianoche anunciado por root requiere actualizar el hash de Today antes del juez; no se ejecuta una campaña con ese manifest desactualizado.

## Actualización posterior del corte

Se actualizó únicamente el hash de today.tsx tras los fixes temporales aprobados (13 scripts GREEN38e58c) y, tras conservar el informe original de mutación, tras el fix de foco aria-disabled9f4c89 (13 scripts GREENadd52f). Las regiones compartidas, límites, reporters y selección no cambiaron. El informe original queda intacto; el hash actual identifica la fuente corregida y no se atribuye retrospectivamente a aquella medición. No se ha preparado ni ejecutado selección de replay: espera inventario y revisión de root.

## Replay acotado tras revisión independiente

Sustituye la pendiente de replay anterior. Root aprobó 40 equivalencias contextuales individuales y mantiene 63 identidades observables del inventario original de 103. El nuevo target cerrado `today-frontend-replay` selecciona únicamente `stryker.today.replay.config.json`; no cambia targets previos ni ejecución completa por defecto.

Ciclo 1: test del target fijo RED777f25 (`Invalid target: today-frontend-replay`) → GREEN59c418. Ciclo 2: test de selección y manifest RED919c25 (manifest ausente) → GREENba447c. Las ampliaciones de matrices de argumentos inválidos y CLI fueron inicialmente GREEN, sin atribuirles un RED. Verificación final3925a8: 15/15 pruebas node:test, formato focal, node --check y git diff --check focal, EXIT0. Ningún subprocess de mutación fue ejecutado por estos tests.

`today_frontend_replay_selection.json` conserva las 63 identidades únicas con operador, estado original, sustitución, expresión y posición original; contrasta cada identidad con el inventario de 103 y registra las 40 exclusiones. Las posiciones originales usan columnas 1-based: el mapper convierte las columnas a 0-based antes de extraer expresiones y generar selectores. Comprueba correspondencia exacta con la fuente actual y hashes de los cuatro archivos seleccionados. No se omiten primeros caracteres ni negaciones.

Son 55 rangos deduplicados, incluyendo la región nueva `src/today.tsx:124:0-124:78` del botón Actualizar. Esa región tiene originalId null: el catálogo original no generó un mutante directo del atributo disabled y no se inventa una identidad ni un kill retrospectivo. La generación futura se reportará por separado, incluso si sólo produce una variante del handler ya conocido.

Se conservan threshold break80, coverageAnalysis perTest, concurrency2 y el ignorePatterns exacto del destino protegido. Los nuevos informes irán a `reports/mutation-today/replay.json` y `replay.html`. El JSON original preservado mantiene SHA256 `5cc335b97919aaa1bcd3cf4cf956af54ee55db3a02fdb23a060c77508f5c47a3`; el 80,23% bruto original no se ajusta por equivalencias. Referencias: fuente original f568f6e y fuente actual 5fe9afc, con refuerzos posteriores sólo en tests por sus autores.

Soporte congelado para revisión e init de root. No se editaron fuentes de aplicación ni pruebas unitarias frontend, ni se ejecutó Stryker/PIT. No se leyó ni limpió contenido de rutas protegidas.
