# Feature 11 — Cliente API, TDD en curso

Contrato: a84e42f, features/schedule_block.feature y Feature 11 de project-spec.md. Baseline init EXIT 0 ejecutado por coordinador; no se repite init concurrentemente. Ponytail full y Caveman lite leídos. Propiedad: schedule-block-api.ts y su test. Sin mutación ni declaración done.

## Evidencia real de ciclos

Comando de cada RED y GREEN: `pnpm exec vitest run src/schedule-block-api.test.ts` desde frontend. Cada fila corresponde a una declaración de test, parametrizada cuando representa Examples del mismo comportamiento. Salidas observadas el 6 de septiembre de 2026.

| Ciclo | Tags | RED observado | GREEN |
| --- | --- | --- | --- |
| Preview y envío exacto | @s1 @s3 | módulo inexistente, EXIT 1 | 1 test, EXIT 0 |
| Preview incompatible cerrado | @s40 | 36 casos resuelven indebidamente | 37 tests |
| HTTP preview | @s40 @s52 | 9 estados resuelven indebidamente | 46 tests |
| Creación retenida 200/201 | @s2 @s44 @s46 | createBlock no es función, 2 casos | 48 tests |
| Confirmación cerrada/coincidente | @s45 @s46 | 22 DTO resuelven indebidamente | 70 tests |
| Microsegundos no truncados | @s40 @s45 | 2 extremos aceptan .000001Z | 72 tests |
| Texto intrínsecamente inválido | @s40 | vacío, 501 puntos, zona vacía aceptados | 75 tests |
| HTTP creación | @s45 @s49 @s50 @s51 @s52 | 8 estados resuelven indebidamente | 83 tests |
| Intención distinta del preview | @s44 | 3 creaciones transmitidas indebidamente | 86 tests |
| Recuperación por key | @s46 | readBlockByRequest no es función | 87 tests |
| Fallo/ausencia/DTO de recuperación | @s47 | 5 resultados aceptados | 92 tests |
| Página y cursor opaco | @s25 @s26 | readBlocks no es función, 2 casos | 94 tests |
| Esquema de página | @s25 @s26 @s54 | 13 páginas aceptadas | 107 tests |
| HTTP de lista | @s25 @s26 @s52 @s54 | 4 estados aceptados | 111 tests |
| Detalle | @s26 | readBlock no es función | 112 tests |
| Identidad/errores de detalle | @s26 | 5 resultados aceptados | 117 tests |
| Errores reconocidos y cuerpo no consumido | @s47 @s49 @s50 @s51 @s52 @s62 | readBlockError no es función, 12 casos | 129 tests |

Prettier aplicado a ambos archivos después de detalle. La normalización usa Unicode White_Space. Instantes de extremos requieren segundos enteros; createdAt conserva admisión de hasta seis decimales. Comparación local menos offset, sin TZDB del navegador ni reloj del cliente. HTTP se preserva como Response; cuerpo inválido produce Error. Parser cerrado de errores implementado; lecturas y creación tienen validación.

## Trazabilidad y pendientes

Los nombres concretos de tests contienen los tags de la tabla. Este cliente cubre fronteras HTTP/DTO, no suplanta pruebas backend de persistencia, concurrencia, DST, publicación ni pruebas UI. Pendientes: revisión independiente y mutación posterior autorizada por juez.

## Segundo tramo: parser cerrado y comprobaciones de frontera

Mismo comando de tests. Ciclos posteriores observados:

| Comportamiento | RED | GREEN |
| --- | --- | --- |
| Errores desconocidos, mal formados o status contradictorio, @s45 | 8 fallos de 9 casos; null ya era aceptado | 138 |
| Validación reconocida, @s4 @s5 @s6 @s7 @s49 | 9 devolvían null | 147 |
| Opciones ambiguas de ambos extremos, @s8 @s41 | 2 devolvían null | 149 |
| INVALID_OFFSET con opciones canónicas, @s8 | 4 devolvían null tras corregir matriz de Vitest para no expandir argumentos | 153 |
| BLOCK_OVERLAP con identidad cerrada, @s14 @s49 | 1 devolvía null | 154 |
| BUDGET_EXCEEDED con días recalculados, @s15 @s49 | 1 devolvía null | 155 |
| Error ya consumido conserva incertidumbre, @s45 | Response.clone rechazaba | 156 |

Comprobaciones adicionales de comportamiento ya implementado (verdes inicialmente; no motivaron producción nueva): 16 errores de validación/opciones inválidas (172), reenvío exacto y CSRF (173), cinco ejemplos de resolución de instantes/offsets y años límite (178), tres retenciones durante await (181), nueve conflictos mal formados (190). Se registran como comprobaciones, no se inventa un RED.

## Mapa de pruebas de frontera

- @s1 @s3: `sends a preview without creating, retaining signal and exact input`.
- @s2 @s44 @s46: `sends retained creation and accepts confirmation HTTP`; `refuses mismatched retained request before creation`.
- @s4 @s5 @s6 @s7 @s49: `recognizes validation`.
- @s6 @s8 @s10 @s40: `accepts server-resolved instants without browser TZDB`; no acredita resolución TZDB en cliente.
- @s8 @s41: `parses closed occurrence options`; `reads invalid offset with canonical valid options`.
- @s14 @s15 @s49: `reads closed overlap identity`; `reads recalculated excess days`.
- @s25 @s26: `reads private blocks page and opaque cursor`; `rejects malformed list`; `does not invent empty list on HTTP`.
- @s26: `reads a specific block using signal`; `rejects wrong detail and preserves HTTP error`.
- @s40: `rejects incompatible preview`; `rejects intrinsically invalid matching text`; `preview preserves HTTP rejection`.
- @s40 @s45: `rejects fractional second mismatches`; `refuses malformed validation and occurrence options`.
- @s44 @s48 @s51: `transmits identical retained intent and precondition on manual resend with current CSRF`.
- @s44 @s46 @s53: `retains expected values while ... waits`; el descarte de contexto obsoleto pertenece a UI.
- @s45 @s46: `rejects incompatible creation DTO`; `treats already consumed error body as unknown`.
- @s45 @s49: `does not treat malformed conflict as definitive`; `rejects unknown malformed or status-mismatched error`.
- @s46 @s47: `recovers only matching retained confirmation with signal`; `rejects missing failed or foreign recovery`.
- @s47 @s49 @s50 @s51 @s52 @s62: `reads recognized ... without consuming response`; `preserves creation HTTP rejection`.

Corrección documental: nombres iniciales de lista/detalle usaban erróneamente @s31/@s32; corregidos a @s25/@s26. Ningún test de este archivo acredita concurrencia backend.

## Verificación del tramo entregable a revisión

- 08:45 (hora del runner): `pnpm exec vitest run src/schedule-block-api.test.ts src/availability-api.test.ts src/tasks-api.test.ts src/task-status-api.test.ts src/session-api.test.ts`: EXIT 0, cuatro archivos encontrados, 499 tests. `session-api.test.ts` no existe y no se cuenta como ejecutado. Cliente nuevo: 190 casos.
- `pnpm exec tsc --noEmit`: EXIT 0.
- `pnpm exec eslint src/schedule-block-api.ts src/schedule-block-api.test.ts`: EXIT 0.
- `pnpm exec prettier --check src/schedule-block-api.ts src/schedule-block-api.test.ts`: EXIT 0.
- No se ha lanzado init global, mutación, commit ni push. Estado de feature no modificado. Revisión pendiente.


