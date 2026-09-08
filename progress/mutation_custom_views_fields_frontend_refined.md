# Campaña completa incremental frontend 21

EXIT 0 b0fbc5. Stryker 82,1330% (1494/1819); conservador killed/total 81,9079% (1494/1824), superior al umbral80 incluso contando cinco errores como no detectados. Duración21 min10 s, del02:31:58 al02:53:08. Cero Timeout y cero CompileError.

| Archivo | Killed | Survived | NoCoverage | RuntimeError | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| src/custom-fields-api.ts | 313 | 51 | 0 | 0 | 364 |
| src/custom-fields.tsx | 207 | 54 | 0 | 4 | 265 |
| src/customization-api.ts | 427 | 75 | 0 | 0 | 502 |
| src/customization-state.ts | 270 | 80 | 2 | 0 | 352 |
| src/customization.tsx | 206 | 52 | 5 | 1 | 264 |
| src/project-reader.tsx | 22 | 3 | 0 | 0 | 25 |
| src/project-tasks.tsx | 46 | 3 | 0 | 0 | 49 |
| src/task-reader.tsx | 3 | 0 | 0 | 0 | 3 |
| Total | 1494 | 318 | 7 | 5 | 1824 |

## Universo y reutilización

Mismo comando node scripts/project.mjs mutate custom_views_fields-frontend, cinco módulos completos y once nodos AST de integración, perTest, ocho workers y umbral80. App está seleccionado pero sus nodos no generan mutantes. mapping.json compara1824 firmas exactas por archivo, localización, mutador y replacement: no faltan ni sobran firmas respecto al original.

Stryker10 anuncia1484 resultados reutilizados y340 programados para ejecución. El dry-run pasa954 pruebas relacionadas; detecta cero archivos mutados cambiados y seis testfiles cambiados (+82/-50). Estos incluyen cinco archivos de refuerzos y appearance.test.tsx con dos esperas explícitas de pintura. No se afirma que toda la campaña sea ejecución nueva. El motor decide la reutilización a partir de cobertura y cambios de tests, sin selección reducida ni suma manual.

El raw no incluye bandera por mutante de reutilización: reuse.json conserva los conteos anunciados, no inventa un desglose por estado reutilizado. Los1391 Killed originales permanecen Killed;97 Survived,2 NoCoverage y4 RuntimeError pasan a Killed por el resultado del motor. Dos NoCoverage pasan a Survived; siete permanecen NoCoverage. Status idéntico no demuestra por sí solo que se reutilizara.

## Errores y límites

Permanecen cinco RuntimeError: src/custom-fields.tsx #481, src/custom-fields.tsx #583, src/custom-fields.tsx #592, src/custom-fields.tsx #597, src/customization.tsx #1672. Son estados reales del reporte, no equivalencias ni detecciones. Los cuatro errores del estado1410/1413/1416/1420 ahora constan Killed en la campaña; no se reclasificó el original. Quedan318 supervivientes y7NoCoverage identificables en residues.json; superar80 no equivale a cubrir todos los comportamientos ni a ausencia de límites. Se conservan las revisiones de riesgos de A/C y la evidencia UX separada. No se inició campaña posterior.

## Integridad y evidencia

Carpeta customization_stryker_refined: before.json, after.json, config.json, seed_manifest.json, authorized_deltas.json, mutation.json, mutation.html, run.log, inventory.json, residues.json, mapping.json y reuse.json. SHA256JSON D97A11B5EC5A5F6311BA6A1BDDF882AEE00477B4B0DB8A9683255FF77E8470C0. El raw original mantiene SHA3DC6D9B548B532389660A53B9915B620683C552032FEF8E3510A10AFAE997942 y su score76,6391% sin reescribirlo.

Before/after169:168 entradas idénticas y un delta autorizado, scripts/project.test.mjs, por expectativa histórica del directorio temporal incremental. Ese arnés Node es externo a Vitest y no altera fuentes, tests Vitest, configuración seleccionada ni sandbox. La configuración seleccionada conserva D5E38935564067252AD2900EB7B9ABBE239269BD3B744B513E6CB8D2F14413B0. No se atribuye integridad169/169.

Regresión final2241/54, apariencia50/50, lint/formato y tipos EXIT0; build del mismo producto EXIT0. Se conserva el fallo anterior de sincronización y su corrección de sólo dos asserts. La aceptación global/CI/despliegue corresponde a root; este informe cierra únicamente el gate de mutación frontend21.
