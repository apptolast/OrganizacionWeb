# Soporte PIT13 integrado — TDD y gate pendiente

Tras MERGE DONE1332eb7, sin producción concurrente. Ownership limitado a backend/build.gradle.kts, scripts/project.mjs y scripts/project.test.mjs más esta bitácora. Ponytail full/Caveman lite; no Git ni campaña PIT.

1. Dispatch fijo: un test nuevo, RED7ab3eb «Invalid target: reschedule-backend». Whitelist y rama mínima conservando reschedule-frontend; GREENa9e4fb.
2. Sufijo shell: un test separado inicialmenteGREEN2813a6; no subproceso, no cambio productivo.
3. Flags Gradle adicionales: un test separado inicialmenteGREEN40abb8; no subproceso, no cambio productivo.
4. Target fuera de mutate: un test separado inicialmenteGREEN00f55f; no subproceso, no cambio productivo.

Aplicada la propuesta Gradle revisada por root56cdad/12ab2b (configuración, no código de negocio):23patrones, candidatos de toda la suite Java, umbral80, threads4, mutadores/filtros/JVM/timeout heredados, reporte pitest-reschedule separado. Se amplía también el scope de mutación por defecto para no omitir adaptadores13; scopes anteriores intactos. El workflow de push application-ci no ejecuta PIT. Sí existe harness-mutation.yml programado/manual que ejecuta harness verify y mutación indirecta; el scope por defecto ampliado se aplica también allí. No se atribuye un RED de Java a esta incorporación de configuración.

Prettier focal sobre los dos scripts y regresión node:test completa GREENe1e16a:22/22, cero omitidos. Se han conservado los18tests anteriores y añadido4, uno por ciclo; no se aplicó la matriz negativa propuesta de golpe.

Siguiente: un único init integrado autorizado por root. No ejecutar PIT hasta gateverde y aviso del coordinador.

Inventario final revisado16b8ba:23patrones/54fuentes en `reschedule_backend_mutation_scope_integrated.json`, con hashes del corte integrado. Relectura de clases compiladas a6c00a encuentra63archivos .class seleccionados, incluidas clases internas BlockChanged.Interval, BlockCursor.Position y PageResponse. Esto no predice número de mutantes: interfaces/records pueden no aportar bytecode mutable.

Hashes soporte: build `5905df8f40be87c144f117fb545d2adf718b3df597da1271267865abdade6735`; dispatcher `34d97dd58359f4e2564bd51e49bc8eb10226992b619740b1439f90f7cfc3959a`; tests `7484df42c746527b50b31daf89e8c71ccfb58f4f0048749e1ae8bc7ec52918fd`.
Init único sesión1806 comenzó con0eb2a6; lint verde912737/6bdc1e y22scripts verdes, suite backend en curso. No campaña PIT iniciada.

## Init integrado final y entrega

Único `node .harness/harness.mjs init`, sesión1806, EXIT0 `91757f`. Lint completo GREEN;22scripts; backend BUILD SUCCESSFUL `4fc543` en2m4s, XML `d2e592` confirma67suites/1617tests, cero fallos/errores/omitidos; frontend28archivos/1498tests GREEN91757f en49,71s. No se repitió init ni se ejecutó PIT.

Los54hashes de producción del inventario integrado se vuelven a verificar antes de entregar; los hashes del soporte al terminar se adjuntan abajo. Root revisó el diff95b173 sin hallazgos. Espera confirmación del coordinador para empezar campaña; fuentes/tests/config quedan quietos.
- `backend/build.gradle.kts`: `5905df8f40be87c144f117fb545d2adf718b3df597da1271267865abdade6735`
- `scripts/project.mjs`: `34d97dd58359f4e2564bd51e49bc8eb10226992b619740b1439f90f7cfc3959a`
- `scripts/project.test.mjs`: `7484df42c746527b50b31daf89e8c71ccfb58f4f0048749e1ae8bc7ec52918fd`
