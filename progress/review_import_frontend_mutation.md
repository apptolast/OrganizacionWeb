# Importación23: mutación original y refuerzo acotado

Campaña original por `node scripts/project.mjs mutate import_data-frontend`, ocho workers, perTest, umbral80 y tres módulos completos más trece nodos AST. El JSON/HTML/config/log originales están preservados en `progress/import_stryker_original`. SHA256 JSON: `58464b8eaa0996245f0a3c08f209c8cb57639ad69e613c8e2f80397beb7118dc`. Duración20m35s; EXIT1 por umbral. El mensaje posterior de pnpm «Command stryker not found» es el envoltorio del EXIT1: el motor sí ejecutó y produjo ambos informes completos.

| Estado original | Número |
| --- | ---: |
| Killed |617|
| Survived |176|
| NoCoverage |7|
| Timeout |0|
| RuntimeError |1|
| Total |801|

Score del motor77,13%; conservador617/801=77,028714%. No se aprueba ese resultado. Las181entradas antes/después son idénticas; `integrity.json` conserva cero diferencias. El error668 modifica el valor de un inputFile a texto no vacío y el runner informa `TypeError: Cannot convert object to primitive value` en `errorToString`; se mantiene RuntimeError, no Killed ni Timeout. Inventario y residuos derivados conservan todas las localizaciones/estados exactos. La carpeta scratch ya había sido retirada por el lifecycle del motor cuando se comprobó; no se borraron otras rutas.

## Prioridades revisadas

Root aprobó siete grupos de conducta pública, sólo archivos de tests nuevos y sin cambiar producto, fixtures originales, dependencias, scope o umbral. No se persigue100%. Mensajes internos vacíos, nav-dot decorativo y guardas redundantes no justifican por sí solos nuevas baterías. No se declara equivalencia general de los textos visibles: las etiquetas de colecciones son parte de la interpretación de cantidades.

| Grupo | Oráculo | IDs candidatos originales |
| --- | --- | --- |
| Resumen y confirmación |14colecciones con cantidades distintas, preview mixto válido, resultadoIMPORTED y retirada depreview, aviso RUNNINGnull |228,440,441,443–451,547,812,814,823,828|
| Validación cerrada |Recuentos negativos coherentes y longitud recuperada cero/fraccionaria/excesiva/tipo incorrecto |148,151,332–334,339,341|
| Recuperación incierta |HTTP503 con codeIMPORT_NOT_FOUND y404 de otro recurso no permiten reenviar/cambiarFile |521,710,712–715,728|
| Actualización acotada |NO_CHANGE no relee apariencia; cachés dePROJECT/TASK sin cambios y aislamiento de ambos sentidos |23,25,62,68,71,74,80,83,86,90,91,109,111|
| Almacenamiento |No se inicia otra intención si removeItem falla al retirar rechazo confirmado |674,676|
| Solapamiento |CancelarpreviewA, empezarB y resolverA no libera el busy deB |579,582,587 y guardas relacionadas|
| Identidad persistida |Hash con prefijo o array se retira sinGET |426,430|

Los IDs son hipótesis revisadas, no kills atribuidos. Algunas guardas pueden seguir siendo redundantes por otras validaciones del mismo recorrido; los resultados los determinará el motor. Los11supervivientes estáticos de etiquetas no se reevalúan por nuevos tests en el modo incremental de Stryker10 y conservarán su estado sin atribuirles kills nuevos.

## Ciclos ejecutados

Nuevos archivos: `frontend/src/import-data.mutation.test.tsx` y `frontend/src/import-refresh.mutation.test.tsx`. Logs `import_refinement_001_initial.log` a `016_initial.log`, un caso añadido y ejecutado cada vez. El primer caso integra el positivo mixto mediante el cliente real; no se duplicó otro test para el mismo1+2=3. El aislamiento usa el mismo cuerpo para NONE,PROJECT,TASK, incorporados uno a uno.

Todos los casos fueron inicialmenteGREEN salvo013: su primer instrumento pidió un único rolealert, pero existían legítimamente el rechazo previo y el nuevo aviso de storage; además la corrección fallida conserva la intención y no vuelve a habilitar File. Se ajustó el oráculo a ese comportamiento seguro y al aviso concreto, sin cambio productivo. `import_refinement_013_fixture_green.log` preserva10/10. No se atribuye ese fallo a producto.

Conjunto focal final:123/123 en seis suites, `import_refinement_focal.log`, EXIT0. TypeScript encontró sólo la opción `exact` no admitida por TestingLibrary ByRoleOptions; se retiró sin cambiar la búsqueda por nombre exacto que ya proporciona la cadena. `import_refinement_types_final.log`, EXIT0. Producto y tests originales coinciden byte a byte con el before de la campaña original. Únicamente los scripts operativos revisados por root evolucionaron después de su after; se inventariarán nuevamente.

La regresión global y el lint finales están en curso al escribir este corte. Propuesta incremental: añadir únicamente `incremental:true` y `incrementalFile` apuntando a una copia byte-exact del raw original, sin editar estados ni reducir mutate. La campaña siguiente requerirá review de config y manifest; su resultado seguirá incluyendo las801firmas y todos los estados. No se suma manualmente un replay reducido al score original.

### Cierre del refuerzo

`import_refined_global.log`: 2424/2424 en 62 suites, EXIT0. Las183entradas fueron idénticas durante ese pase (`import_refined_gate_integrity.json`). El primer lint encontró sólo formato de las dos cadenas ByRole tras quitar la opción de tipos; se conserva `import_refined_lint.log`, se aplicó Prettier al archivo nuevo y `import_refined_lint_final.log` pasó completo. No cambió ningún oráculo ni fuente productiva, por lo que no se repitió la regresión global por formato. Root revisó íntegros los dos archivos y aprobó la configuración con exactamente dos claves operativas añadidas. `seed-original.json` y `cache.json` empiezan con SHA58464…18dc idéntico al original; sólo Stryker actualizará cache. Los informes originales siguen preservados independientemente del directorio de salida reutilizado.

## Resultado incremental completo

Campaña autorizada por el mismo arnés y universo: EXIT0, 7 minutos y 48 segundos. El motor informó 0 fuentes mutadas cambiadas y dos archivos nuevos de pruebas (+16 casos), con 671 resultados reutilizados y 130 ejecutados. El dry-run pasó 1088 tests. No se alteró ningún estado manualmente ni se seleccionó un universo reducido.

| Estado final | Número |
| --- | ---: |
| Killed | 651 |
| Survived | 145 |
| NoCoverage | 4 |
| Timeout | 0 |
| RuntimeError | 1 |
| Total | 801 |

El score conservador es **651/801 = 81,2734082397 %**, incluido el error como no detectado. El motor muestra 81,38 % al excluir ese error. Las 801 firmas son idénticas al original; las 183 entradas efectivas permanecieron iguales durante la campaña. Los 617 Killed originales siguen Killed; 31 Survived y 3 NoCoverage pasan a Killed. Los once Survived estáticos de etiquetas permanecen Survived: no se atribuye su cobertura nueva como eliminación. El RuntimeError 668 se conserva sin reclasificación.

Evidencia completa en `progress/import_stryker_incremental`: before/after, configuración, semilla inmutable, cache del motor, run.log, exit.txt, JSON/HTML finales y summary/inventory/residues. SHA256 del JSON final: `81b74a80891c1a93609e749fb0215135a1819581de5201c07f7d76ece3819619`. Original y semilla conservan SHA256 `58464b8eaa0996245f0a3c08f209c8cb57639ad69e613c8e2f80397beb7118dc`.

Los residuos quedan declarados, no convertidos en equivalencias: incluyen etiquetas estáticas visibles, ramas defensivas o de refresco protegidas por otras condiciones, textos internos y decoración de navegación. Las cuatro ramas sin cobertura y el error del runner siguen siendo límites de esta campaña. Los refuerzos públicos verifican explícitamente resumen, rechazo de longitudes inválidas, recuperación incierta, privacidad, almacenamiento, solapamiento y refresco acotado. No se propone otra campaña por mejora numérica; el scope revisado supera el umbral conservador con producto intacto. Esto no sustituye el cierre de atomicidad backend ni la regresión E2E del candidato final.
