# Mutación frontend de integración API: resultado final

Campaña correctiva oficial completa, `node scripts/project.mjs mutate integration_api-frontend`, HEAD inicial 4a943842216dc1c3ba535cbca63adf6ae910c346, EXIT 0. Misma configuración y universo original: tres módulos completos y siete nodos AST en seis archivos físicos; 997 mutantes, ocho workers, perTest, umbral 80. No incremental, selección reducida, reinterpretación de estados ni suma manual de campañas.

| Estado | Original | Correctiva |
| --- | ---: | ---: |
| Killed | 780 | 836 |
| Survived | 208 | 158 |
| NoCoverage | 8 | 2 |
| RuntimeError | 1 | 1 |
| Timeout | 0 | 0 |
| Total | 997 | 997 |

Resultado conservador final: 836/997 = 83,85155466399198 %, supera 80. El motor informa 83,94 % al excluir el error; ambas métricas se distinguen. Original conservador 78,23470411233701 %, EXIT 1, conservado íntegro.

Los 194 inputs before/after coinciden sin deltas. Las 997 firmas con ID, archivo, inicio/fin, mutador y replacement coinciden; se conserva la multiplicidad de mutantes con idéntica representación textual y distinto ID. Todas las fuentes de producto y configuración efectiva permanecen idénticas. Los commits de E2E y documentación durante la campaña están fuera de ese universo y no cambiaron Vitest.

Comparación de resultados reales: los 780 Killed originales siguen Killed, 50 Survived y 6 NoCoverage pasan a Killed. Permanecen 158 Survived, 2 NoCoverage y RuntimeError 712. Este último conserva el fallo errorToString del runner; no es un timeout ni una muerte inferida. Los residuos se entregan completos en inventory.json y no se declaran todos equivalentes. No se persigue 100 % ni se propone otra campaña sin un cambio pertinente.

La comprobación inicial de la correctiva ejecutó 1039 pruebas relacionadas, frente a 1024 de la original. Los 15 refuerzos fueron inicialmente GREEN y luego el foco conjunto pasó 83/83, con formato, tipos y ESLint. La regresión frontend anterior de 2492/2492 sigue siendo evidencia histórica previa a esos 15 refuerzos; no se presenta como global posterior. La CI oficial y la aceptación final son puertas separadas de esta campaña.

Raw JSON final SHA256 A8A2371419518AB4D869D8A16D38C31B12D59E272919F08E7FF3E880DCC5702F. Artefactos originales: progress/integration24_stryker_corrective_evidence.zip, que contiene {mutation.json,mutation.html,run.log,run.exit,config_original.json,head.txt,inputs_before.json,inputs_after.json,inventory.json,transitions.json,results.json} más manifest.json. El freeze hermano fija sus bytes; el ZIP portable incluye esos once archivos y manifiesto verificado contra originales. No se modificó el engine ni el denominador.