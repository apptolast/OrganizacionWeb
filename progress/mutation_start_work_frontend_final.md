# Mutación frontend14 — campaña final

**Veredicto de umbral: PASS.** 483/539 = 89,6104 %, umbral80, EXIT0 eb4926. Duración reportada 10 minutos y 26 segundos. Quedan 56 Survived; no se reclasifican ni descuentan. Cero Timeout, NoCoverage, RuntimeError o CompileError.

Medición autorizada tras review 9e28ac/3665aa y hashes ebc4510. Fuente/test/config congelados. Rol mutation_tester: sólo medir y reportar; Ponytail full y Caveman lite.

Regresión frontend global 4393bc EXIT0: 1585 pruebas en 31 archivos. Build 4c4a3d EXIT0. No se ejecutó backend: PIT tiene otro autor.

Antes de ejecutar, copiados sin mover ni borrar los originales a frontend/reports/mutation-start-work-session-initial/. Evidencia 10d4ce confirma SHA256 exactos:
- mutation.json: E88C2A6C45207C40D68267162F320E84B9A769CD581F69B21896C8B1260764FF.
- mutation.html: 6986FF220F6144DF010D12D1924F9EE563A1AED25946C7ACE5E4B98241E152D5.

La configuración original mantiene sus rutas de salida, ignorePatterns protegido, perTest, concurrency8 y umbral80. El reporte previo está preservado; no se cambió la configuración para la copia.

Snapshot mutation_start_work_frontend_final_before.json: 88 archivos, incluidos todos los tests de la lista original y los dos reforzados (ya formaban parte de ella). Captura previa verificada 10d4ce. Log propio: mutation_start_work_frontend_final.log.

Comando: node .harness/harness.mjs mutate start_work_session-frontend. Arranque8f864c, sesión85286/PID54016. Baseline693 pruebas GREEN9fc7e8; instrumentación539 mutantes. El corte final incluye seis mutantes adicionales generados por la corrección de presentación. No hubo modificaciones durante la campaña.

## Resultado final y conservación del corte

| Archivo | Total | Killed | Survived | NoCoverage | Timeout | RuntimeError |
| --- | --- | --- | --- | --- | --- | --- |
| work-session-api.ts | 211 | 198 | 13 | 0 | 0 | 0 |
| work-session.tsx | 321 | 281 | 40 | 0 | 0 | 0 |
| task-reader.tsx | 7 | 4 | 3 | 0 | 0 | 0 |
| Total | 539 | 483 | 56 | 0 | 0 | 0 |

After f4ef2e confirma 88 archivos y cero diferencias respecto al before. Snapshot completo en mutation_start_work_frontend_final_after.json. Los originales copiados mantienen sus dos hashes exactos.

Reportes finales en frontend/reports/mutation-start-work-session/:
- JSON B4E68AAB2C60D9C6D1EAD58035AA66471E29BB41E5388B7A001998CF8DAFFB7F.
- HTML 24275EE64A93182DB9D11AAF0A4FE4E39AE880B33B2ADB2764541C235D82FF71.

## Correspondencias con los 81 residuos iniciales

mutation_start_work_frontend_final_inventory.json contiene las 81 correspondencias, ubicaciones inicial/final, mutador, texto original/reemplazo, SHA de fuente y ambos estados. Extracción por columnas1-based y coincidencia de firma más posición: prefijo/sufijo de fuente conservados permiten desplazar las líneas después del párrafo modificado. Cada correspondencia exige una coincidencia única; ninguna se resuelve sólo por ID numérico. Comprobación del algoritmo contra el original: dd8571. Mapeo final: 90082e, 81/81 únicas.

De esos 81: **25 Killed y 56 Survived**. IDs iniciales ahora Killed:
29,32,37,35,54,61,58,72,65,66,73,69,76,141,257,258,261,367,369,370,429,467,502,504,440.

Los 24 objetivos de los seis grupos prioritarios aparecen Killed; también467. Los seis mutantes adicionales están Killed. Ningún superviviente final procede de un Killed anterior o de un mutante nuevo. La diferencia de IDs después del cambio importa: por ejemplo, el antiguo502 de preventDefault no es el final502, que pertenece a la ayuda de inelegibilidad.

La revisión previa propone equivalencias contextuales para21 de los supervivientes; esta medición no las adopta como estados del reporter. Los restantes requieren el dictamen de root según review_start_work_mutation_residuals.md. No se interpreta PASS de umbral como cierre de feature14 ni como equivalencia universal.

## Residuos finales observados

| Inicial → final | Archivo:línea final | Mutador | Original → reemplazo |
| --- | --- | --- | --- |
| 14 → 14 | src/work-session-api.ts:29 | ArrowFunction | () => null → () => undefined |
| 175 → 175 | src/work-session-api.ts:159 | LogicalOperator | start !== null &&     end !== null → start !== null &#124;&#124; end !== null |
| 176 → 176 | src/work-session-api.ts:159 | ConditionalExpression | start !== null → true |
| 174 → 174 | src/work-session-api.ts:159 | ConditionalExpression | start !== null &&     end !== null → true |
| 178 → 178 | src/work-session-api.ts:160 | ConditionalExpression | end !== null → true |
| 188 → 188 | src/work-session-api.ts:167 | Regex | /(?:\.\d+)?Z$/ → /(?:\.\d+)?Z/ |
| 189 → 189 | src/work-session-api.ts:167 | Regex | /(?:\.\d+)?Z$/ → /(?:\.\d+)Z$/ |
| 190 → 190 | src/work-session-api.ts:167 | Regex | /(?:\.\d+)?Z$/ → /(?:\.\d)?Z$/ |
| 191 → 191 | src/work-session-api.ts:167 | Regex | /(?:\.\d+)?Z$/ → /(?:\.\D+)?Z$/ |
| 192 → 192 | src/work-session-api.ts:167 | StringLiteral | "Z" → "" |
| 195 → 195 | src/work-session-api.ts:168 | Regex | /\.(\d+)Z$/ → /\.(\d+)Z/ |
| 199 → 199 | src/work-session-api.ts:169 | ArithmeticOperator | BigInt(wholeMilliseconds) * 1000n + BigInt(fraction.padEnd(6, "0")) → BigInt(wholeMilliseconds) * 1000n - BigInt(fraction.padEnd(6, "0")) |
| 201 → 201 | src/work-session-api.ts:169 | StringLiteral | "0" → "" |
| 6 → 6 | src/task-reader.tsx:131 | OptionalChaining | snapshot?.project → snapshot.project |
| 3 → 3 | src/task-reader.tsx:130 | LogicalOperator | !projectLoading && !projectFailure → !projectLoading &#124;&#124; !projectFailure |
| 1 → 1 | src/task-reader.tsx:130 | ConditionalExpression | !projectLoading && !projectFailure → true |
| 233 → 233 | src/work-session.tsx:29 | OptionalChaining | heading.current?.focus → heading.current.focus |
| 236 → 236 | src/work-session.tsx:36 | StringLiteral | "" → "Stryker was here!" |
| 238 → 238 | src/work-session.tsx:38 | BooleanLiteral | false → true |
| 240 → 240 | src/work-session.tsx:41 | BooleanLiteral | false → true |
| 249 → 249 | src/work-session.tsx:48 | ConditionalExpression | active === null → true |
| 294 → 294 | src/work-session.tsx:62 | EqualityOperator | Number(minutes) < 1 → Number(minutes) <= 1 |
| 296 → 296 | src/work-session.tsx:63 | ConditionalExpression | Number(minutes) > 1440 → false |
| 297 → 297 | src/work-session.tsx:63 | EqualityOperator | Number(minutes) > 1440 → Number(minutes) >= 1440 |
| 305 → 305 | src/work-session.tsx:69 | StringLiteral | "" → "Stryker was here!" |
| 313 → 313 | src/work-session.tsx:83 | ConditionalExpression | controller.signal.aborted → false |
| 316 → 316 | src/work-session.tsx:85 | BooleanLiteral | false → true |
| 314 → 314 | src/work-session.tsx:84 | OptionalChaining | lookup.current?.abort → lookup.current.abort |
| 318 → 318 | src/work-session.tsx:86 | BooleanLiteral | false → true |
| 273 → 273 | src/work-session.tsx:54 | ArrayDeclaration | [] → ["Stryker was here"] |
| 322 → 322 | src/work-session.tsx:89 | BooleanLiteral | false → true |
| 375 → 375 | src/work-session.tsx:113 | BooleanLiteral | false → true |
| 384 → 384 | src/work-session.tsx:118 | OptionalChaining | problem.errors.find((error) => error.field === "plannedMinutes")             ?.message → problem.errors.find(error => error.field === "plannedMinutes").message |
| 386 → 386 | src/work-session.tsx:118 | ConditionalExpression | error.field === "plannedMinutes" → true |
| 393 → 393 | src/work-session.tsx:123 | BooleanLiteral | false → true |
| 411 → 411 | src/work-session.tsx:132 | ConditionalExpression | !controller.signal.aborted && command.current === controller → true |
| 413 → 413 | src/work-session.tsx:132 | LogicalOperator | !controller.signal.aborted && command.current === controller → !controller.signal.aborted &#124;&#124; command.current === controller |
| 415 → 415 | src/work-session.tsx:132 | ConditionalExpression | command.current === controller → true |
| 470 → 470 | src/work-session.tsx:183 | ArithmeticOperator | value + 1 → value - 1 |
| 469 → 469 | src/work-session.tsx:183 | ArrowFunction | (value) => value + 1 → () => undefined |
| 446 → 446 | src/work-session.tsx:159 | StringLiteral | &#96;${id}-heading&#96; → &#96;&#96; |
| 447 → 447 | src/work-session.tsx:161 | StringLiteral | &#96;${id}-heading&#96; → &#96;&#96; |
| 500 → 506 | src/work-session.tsx:203 | BooleanLiteral | !uncertain → uncertain |
| 494 → 500 | src/work-session.tsx:203 | ConditionalExpression | !eligible && !uncertain && (             <p>               Para iniciar trabajo necesitamos confirmar una tarea pendiente y               un proyecto no completado.             </p>           ) → true |
| 495 → 501 | src/work-session.tsx:203 | ConditionalExpression | !eligible && !uncertain && (             <p>               Para iniciar trabajo necesitamos confirmar una tarea pendiente y               un proyecto no completado.             </p>           ) → false |
| 496 → 502 | src/work-session.tsx:203 | LogicalOperator | !eligible && !uncertain && (             <p>               Para iniciar trabajo necesitamos confirmar una tarea pendiente y               un proyecto no completado.             </p>           ) → !eligible && !uncertain &#124;&#124; <p>               Para iniciar trabajo necesitamos confirmar una tarea pendiente y               un proyecto no completado.             </p> |
| 497 → 503 | src/work-session.tsx:203 | ConditionalExpression | !eligible && !uncertain → true |
| 499 → 505 | src/work-session.tsx:203 | BooleanLiteral | !eligible → eligible |
| 498 → 504 | src/work-session.tsx:203 | LogicalOperator | !eligible && !uncertain → !eligible &#124;&#124; !uncertain |
| 541 → 547 | src/work-session.tsx:299 | BooleanLiteral | false → true |
| 543 → 549 | src/work-session.tsx:302 | StringLiteral | "es-ES" → "" |
| 545 → 551 | src/work-session.tsx:304 | StringLiteral | "long" → "" |
| 546 → 552 | src/work-session.tsx:305 | StringLiteral | "long" → "" |
| 554 → 560 | src/work-session.tsx:318 | StringLiteral | " " → "" |
| 555 → 561 | src/work-session.tsx:325 | StringLiteral | " " → "" |
| 532 → 538 | src/work-session.tsx:271 | ConditionalExpression | busy &#124;&#124; uncertain → true |

No se ejecutaron nuevas pruebas ni cambios para estos residuos. Reporte original, reporte final e inventario permanecen separados. Freeze de medición liberado al coordinador tras verificar los hashes.
