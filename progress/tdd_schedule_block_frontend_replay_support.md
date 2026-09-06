# Soporte de replay frontend schedule_block

2026-09-06. Autor resume_review, Ponytail full/Caveman lite. Root autoriza target cerrado adicional, configuración específica y trazabilidad; sin cambiar harness motor/config, target default ni medición original. No se ha ejecutado Stryker, Gradle ni PIT desde esta subtarea.

## TDD del soporte

1. Nuevo test con runner inyectado exige schedule_block-frontend-replay y llamada única a pnpm --dir frontend exec stryker run stryker.schedule-block.replay.config.json. RED7d6fe0: Invalid target. Implementación mínima añade valor a allowlist y rama fija con return. Regresión8/8GREENd6ecb1; ningún subprocess real se lanza por el runner inyectado.
2. Nuevo test de configuración exige política80/perTest/concurrency2, ignorePatterns protegido, reporters y destinos propios, rangos sólo de los cuatro archivos autorizados. RED7eaa95: config aún no existe. Config añadida desde original+inventario:9/9GREEN5edcf1.
3. Ampliada regresión de target desconocido/fuera de mutate y paso CLI con nombre nuevo. Inicialmente GREEN; no acción nueva de producción. Default sigue invocando ambas suites completas; scopes previos intactos.

CLI y esquema instalados comprobados por lectura: @stryker-mutator/core/dist/src/stryker-cli.js registra command('run').addArgument(configFileArgument) y pasa ese argumento a readStrykerOptions. schema/stryker-schema.json documenta htmlReporter.fileName/jsonReporter.fileName. ProjectReader.filterMutatePattern usa línea1-based/columna0-based; el reporte usa líneas/columnas1-based. No se lanzó CLI Stryker para verificar esta sintaxis.

## Selección provisional y protección del informe original

frontend/stryker.schedule-block.replay.config.json conserva configuración base (thresholds high90/low80/break80, cobertura perTest, concurrency2, plugins y vitest, ignorePatterns [.stryker-tmp-availability-replay]). Cambia mutate a rangos y asigna reports/mutation-schedule-block/replay.json y replay.html. No sobreescribe reports/mutation/mutation.json ni el HTML original. La selección no es un score global corregido: el replay tendrá su propio denominador, incluyendo mutantes incidentales de otros operadores en los mismos rangos.

progress/schedule_block_frontend_replay_selection.json traza167 IDs originales seleccionados:67 API H (85 menos18 equivalencias contextuales autorizadas),94 TaskBlocks (93NE incluyendo3NoCoverage, más RuntimeError945),5TaskReader y1TaskState. Cada entrada conserva status, operador, location original y rango actual. Se deduplican rangos idénticos:140 rangos de originales más1rango de producción nueva setTaskState(undefined), total141. No se excluyen operadores/equivalencias globalmente.

El mapa se construyó del JSON original y su source embebida, contrastando que todas sus líneas existen en orden dentro de la fuente actual (sólo inserciones permitidas). Se convierte columna restando1 y se remapea línea por la inserción de TaskReader. Si deja de ser una edición sólo de inserción, el generador puntual falla y requiere revisar el mapa; no adivina desplazamientos. Fuente original y actual tienen hashes registrados.

Estado provisional: autor UI sigue editando tests y debe confirmar freeze de producción. Nueva línea actual TaskReader94, rango94:14–94:38. Antes de judge se verifican de nuevo hashes/rangos frente al freeze y se actualiza el estado del manifiesto. No ejecutar Stryker hasta revisión/permiso de root.

Formato focal3d625d aplicado sólo scripts/project.mjs, scripts/project.test.mjs y configreplay; regresión node:test9/9GREENa62972 después del formato, node --check ambos scripts EXIT0fc35f5. No se ejecutó el target real. Freeze soporte provisional; resta confirmación fuenteUI para cerrar manifiesto.

Autor frontend confirma producción estable: sólo inserción TaskReader y ninguna edición adicional prevista sin RED nuevo; tests siguen en autoría. Validación final de soporte comprueba hashes actuales de los4fuentes,141rangos exactamente iguales a unión de manifiesto y producción nueva, límites de línea/columna y rangos no vacíos. Todo válido (salida de validación final); manifiesto marcado production-freeze-verified-awaiting-root-judge con timestampUTC. Config no requirió ajustar líneas respecto corte provisional. Tests UI pendientes de freeze del otro autor no alteran estos rangos. Entrega lista para juez, sin permiso implícito de ejecutar Stryker.

## Replay final acotado:29 observables restantes

Root autoriza adaptar el target cerrado existente después de revisar el primer replay; no cambia scripts/project.mjs, harness, default ni targets previos. La configuración fija apunta ahora a reports/mutation-schedule-block/final.json y final.html; replay.json/html y el manifiesto167 original se preservan.

Ciclo1: cambiar expectativa de destinos en test existente→REDd41c1a (seguía apuntando replay.json). Ajustar sólo reporterpaths→9/9GREEN3b0fc0. Ciclo2: nuevo test de selección trazable29IDs, rangos exactos y destinos separados→RED4e8a90 (manifiesto final inexistente). Crear manifiesto nuevo y reducir rangos→10/10GREENb2dae3. Los9tests anteriores permanecen; sólo literal de destino se actualiza al nuevo contrato. Todo runner inyectado, cero Stryker real.

Nuevo progress/schedule_block_frontend_final_selection.json conserva correspondencia originalId/replayId, statusSurvived, ubicación original/replay/actual, expresión, operador, replacement, hashfuente y rango. Selección29 (7yaasignados+22delremaining), deduplicada a26rangos de TaskBlocks/TaskReader. Verificación exige identidad unívoca y coincidencia exacta source actual vs embebido del replay (normalizando sóloCRLF). Registra SHA del manifiesto anterior y JSONreplay anterior, sin modificarlos. No selecciona945 para atribuirle kill ni equivalencias como objetivos; los rangos pueden generar mutantes incidentales con denominador propio. Política80/perTest/concurrency2 e ignorePatterns protegido intactos.

Formato focal del test/config y regresión posterior10/10GREEN completados; scripts/project.mjs permanece igual en este seguimiento. FuentesUI no editadas; otroautor termina su batch de tests. Config y manifiesto listos para revisiónroot, no ejecución de mutación autorizada implícitamente. Si producción cambia antes de medir, hay que validar de nuevo rangos/hash; los testsUI por sí solos no desplazan estas posiciones.
