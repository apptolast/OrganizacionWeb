# Original Stryker frontend 21

La campaña completa terminó con EXIT 1 por umbral: 76,6391% (1391/1815 sin los nueve errores), frente al 80% exigido. El cálculo conservador killed/total es 76,2610% (1391/1824). No se considera cerrado este gate.

Comando: node scripts/project.mjs mutate custom_views_fields-frontend. Duración 44 min 8 s; ocho workers, perTest, cinco módulos completos y once nodos AST de integración. El dry-run ejecutó 922 pruebas relacionadas correctamente; no representa la regresión global de 2209 pruebas.

| Archivo | Killed | Survived | NoCoverage | RuntimeError | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| src/custom-fields-api.ts | 291 | 72 | 1 | 0 | 364 |
| src/customization-api.ts | 408 | 92 | 2 | 0 | 502 |
| src/customization-state.ts | 257 | 88 | 3 | 4 | 352 |
| src/customization.tsx | 192 | 66 | 5 | 1 | 264 |
| src/custom-fields.tsx | 192 | 69 | 0 | 4 | 265 |
| src/project-reader.tsx | 15 | 10 | 0 | 0 | 25 |
| src/project-tasks.tsx | 33 | 16 | 0 | 0 | 49 |
| src/task-reader.tsx | 3 | 0 | 0 | 0 | 3 |
| Total | 1391 | 413 | 11 | 9 | 1824 |

Los nodos seleccionados de App.tsx no generaron mutantes. Hay cero Timeout y cero CompileError. Los nueve RuntimeError permanecen como errores: src/customization-state.ts #1410, src/customization-state.ts #1413, src/customization-state.ts #1416, src/customization-state.ts #1420, src/customization.tsx #1672, src/custom-fields.tsx #481, src/custom-fields.tsx #583, src/custom-fields.tsx #592, src/custom-fields.tsx #597. Su statusReason conserva el fallo del runner errorToString: TypeError Cannot convert object to primitive value. No se reclasifican como detecciones y no se cambian dependencias por esta evidencia.

## Evidencia e integridad

Originales preservados en customization_stryker_original/: mutation.json, mutation.html y run.log. SHA-256 del JSON: 3DC6D9B548B532389660A53B9915B620683C552032FEF8E3510A10AFAE997942. inventory.json contiene todos los estados, firmas y fragmentos; residues.json conserva los 433 no Killed, incluidos los errores. Las columnas del JSON Stryker son 1-based y el extremo final es exclusivo.

before.json y after.json registran 163 entradas: 160 iguales y tres deltas autorizados documentados en authorized_deltas.json. Cambió styles.scss por integración CSS revisada de C después de crear el sandbox; también cambiaron las configuraciones no seleccionadas default y Appearance por reparación autorizada de coordenadas históricas. Las copias previas y los nodos exactos están en nonselected_before/ y nonselected_config_delta.json. Ningún JS, prueba Vitest ni configuración seleccionada cambió. La configuración seleccionada conserva SHA-256 6F21EC1DCC8B7B1EB4AE33EA99FA13028C042701E7CE2C032E01E23A635D6F34.

## Siguiente revisión

A revisa clientes y estado; C revisa UI, integración y errores. Se priorizan privacidad, incertidumbre, recuperación, foco y fidelidad de datos. No se declaran equivalentes por ausencia de cobertura ni se añaden matrices para perseguir 100%. No se ejecutó replay ni refinamiento automático. Una futura campaña completa debe acreditar el umbral, sin sumar manualmente un resultado dirigido al original.
