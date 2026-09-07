# TDD frontend18 — Historial

Alcance autorizado: cliente y página, integración con navegación/detalles existentes. Contrato aprobado 768AA48A…B72C13, rama común gestionada por root. Ponytail full/Caveman lite: reutilización de validadores y controles; sin dependencias ni router nuevos. El init previo es reutilizado por instrucción expresa del coordinador. No se ejecutan gates globales en estos ciclos.

Comando focal: `pnpm --dir frontend exec vitest run src/history-api.test.ts`. Los logs `progress/history_client_NN_{red,green}.log` conservan el resultado Vitest. En los fallos pnpm añade un diagnóstico secundario engañoso «Command vitest not found» después del resultado; Vitest sí ejecutó y el fallo real se identifica dentro del log.

| Ciclo | Oráculo individual | RED real | GREEN | Cambio mínimo |
| --- | --- | --- | --- | --- |
| 1 | @s6 vacío con petición privada y signal | b31ba0, módulo inexistente | e509fa, 1/1 | GET con apiRequest |
| 2 | @s35 conservar filtros/cursor solicitados | f7049c, URL sin query | 6aa946, 2/2 | serializar URLSearchParams |
| 3 | @s23 conservar Response503 | 6f8db6, JSON vacío en vez de Response | 1a5ac3, 3/3 | rechazar status distinto de200 |
| 4 | @s26 envoltorio con campo extra | a4da08, acepta total | log04green, 4/4 | exact heredado para envoltorio |

Corte todavía incompleto: no afirmar decoder ni UI terminados. Las familias, validación de filas y filtros se añaden en los siguientes ciclos individuales.

| 5 | @s26 items no es array | 5b74c5 | d74006, 5/5 | comprobar array |
| 6 | @s1 inicio original sin GET adicional | inicialmente GREEN 0a7516, 6/6 | mismo corte | no producción |
| 7 | @s26 plannedEndAt desviado un µs | 86e99b | 57d14f, 7/7 | reutilizar isSessionStart |
| 8 | @s26 familia desconocida con details válido | 5a3282 | log08green, 8/8 | discriminante SESSION_STARTED |

Los casos nominales inicialmente verdes se conservan como oráculos positivos; no se fabricó RED. El decoder sigue intencionadamente parcial durante los ciclos.

| 9 | @s1 tarea completada | aa62be | aed360, 119/119 con heredados | exportar isHistoryEntry sin alterar cuerpo |
| 10 | @s1 reserva original | a0a7fb | 97ecad, 10/10 | isBlock existente |
| 11 | @s1 recibo CANCELLED | a8d04d | a374e3, 32/32 con heredados | exportar isChange de reschedule sin alterar cuerpo |
| 12 | @s1 recibo PAUSE | c6a7eb | log12green con heredados | exportar isChange de state API sin alterar cuerpo |

Las tres exportaciones mínimas evitan copiar validaciones de detalles históricos. No cambian la lógica ni los oráculos heredados. El próximo tramo cierra identidad exterior, orden/filtros y las variantes discriminadas antes del freeze del cliente.

| 13 | @s26 falta taskTitle | b55ffe | 660a80, 13/13 | entrada exacta8 |
| 14 | @s26 projectName vacío | a3abf0 | fde493, 14/14 | text heredado |
| 15 | @s26 taskTitle null | 4cdfe3 | 64f262, 15/15 | text heredado |
| 16 | @s26 ID exterior distinto de SessionStart | ad5c11 | log16green, 16/16 | comparación de identidad |

| 17 | @s26 instante exterior desviado un µs | 27491c | bf99c1, 17/17 | comparar microseconds |
| 18 | @s26 inicio de otro proyecto exterior | 15912c | aed84c, 18/18 | comparar projectId |
| 19 | @s26 inicio de otra tarea exterior | 3eb1cb | 6444d1, 19/19 | comparar taskId |
| 20 | @s26 reserva fechada por startAt | 217a04 | 7262f1, 20/20 | comparar createdAt |
| 21 | @s26 pausa de otra tarea exterior | 2529b1 | 1c74bd, 21/21 | comparar contexto before.session |
| 22 | @s26 pausa de otro proyecto exterior | 0959ff | log22green, 22/22 | completar contexto before.session |

| 23 | @s26 ID exterior de reserva | 144764 | 21e4d8, 23/23 | comparar ID |
| 24 | @s26 ID exterior de pausa | 5a0f8f | 7bcfcf, 24/24 | comparar ID |
| 25 | @s26 instante exterior de pausa | 2abf0d | 844dd6, 25/25 | comparar µs |
| 26 | @s26 ID exterior de tarea | 8886a8 | log26green, 26/26 | comparar ID |

| 27 | @s26 instante exterior de tarea | 54ec2c | 5f0ac0, 27/27 | comparar µs |
| 28 | @s26 ID exterior de cambio de bloque | e0295f | f7d522, 28/28 | comparar ID |
| 29 | @s26 instante exterior de cambio de bloque | 185c8b | cccc83, 29/29 | comparar µs |
| 30 | @s26 projectId inválido sin contexto en details4 | a65760 | 99cd49, 30/30 | uuid exterior |
| 31 | @s26 taskId inválido sin contexto en details4 | 9f1e73 | log31green, 31/31 | uuid exterior |

| 32 | @s26 veintiuna entradas | e7c3fc | a24af5, 32/32 | límite20 |
| 33 | @s26 cursor no textual | 9a1450 | 1cb16c, 33/33 | tipo cursor |
| 34 | @s26 cursor vacío | 55b00c | 45ba9c, 34/34 | cursor textual no vacío |

Refactor en verde 7124fd: unión discriminada HistoryEntry/HistoryPage reutilizando los cinco tipos existentes. 34/34 y TypeScript sin errores. No modifica comportamiento.

| 35 | @s27 orden ascendente por un µs en año1600 | 77751c | 07b974, 35/35 | comparación BigInt |
| 36 | @s27 familia en orden inverso con empate temporal | 5203ed | bf3770, 36/36 | rango fijo del contrato |
| 37 | @s27 UUID ascendente con empate | 9ea80e | 4e202a, 37/37 | comparación lexicográfica |
| 38 | @s27 duplicado(type,id) con distintos instantes | a0c94e | log38green, 38/38 | Set de identidad compuesta |

Ciclo35 tuvo arranque Vitest más lento (28 segundos); resultado RED real por el oráculo, sin timeout ni cambios del entorno. La comparación usa microseconds heredado y BigInt, no Date.parse/Number para el orden.

| 39 | @s27 fila de otro filtro projectId | b7460b | 3be147, 39/39 | sameId contra filtro |
| 40 | @s27 fila de otro filtro taskId | 58fea2 | b8b295, 40/40 | sameId contra filtro |
| 41 | @s27 familia incompatible con categoría | 5e90bd | be1176, 41/41 | mapa de cinco familias |
| 42 | @s27 hecho anterior a fromUTC | efd251 | ffd7d7, 42/42 | fechaUTC inclusiva |
| 43 | @s27 hecho posterior a toUTC | 022afa | log43green, 43/43 | fechaUTC inclusiva |

| 44 | @s26 año0000 en TaskHistory4 heredado | 81b321 | 089e8b, 44/44 | exigir µs normativo no null en exterior |
| 45 | @s26 ID de hecho en mayúsculas | 0608c9 | d09b86, 45/45 | UUID canónico exterior |
| 46 | @s26 projectId en mayúsculas | d3cc02 | e8d20c, 46/46 | UUID canónico exterior |
| 47 | @s26 taskId en mayúsculas | 0f4e37 | log47green, 47/47 | UUID canónico exterior |

La restricción0001–9999 pertenece al envoltorio18: TaskHistory9 permite más años, así que no se modifica ese productor/validador heredado. Los filtros UUID de entrada sí se comparan normalizados mediante sameId; las respuestas18 exigen IDs canónicos.

| 48 | @s36 borrador cambia mientras H espera | 39e26a | ae6a8d, 48/48 | copiar URLSearchParams antes de await |
| 49 | @s6 veinte entradas, cursor y filtros inclusivos | inicialmente GREEN 30e98d | 49/49 | no producción |
| 50 | @s13 seis hechos empatados, revisión no causal y UUID entre familias | inicialmente GREEN 2c999f | 50/50 | no producción |
| 51 | @s25 CLOSE con neto9007199254740992 y notas/atribución histórica | inicialmente GREEN 1bd868 | 51/51 | no producción |
| 52 | @s1 EXTEND conserva trabajado | inicialmente GREEN 909929 | 52/52 | no producción |
| 53 | @s1 RESCHEDULED conserva ambos intervalos | inicialmente GREEN fba265 | 53/53 | no producción |

## Primer freeze del cliente

Cincuenta y tres oráculos nuevos. Regresión focal de seis suites: **528/528 GREEN**, 55369e, `progress/history_client_freeze_tests.log`. Los otros475 son heredados, no nuevos ejemplos ejecutados de18. No se equiparan estos conteos con las142 filas Gherkin.

Fuentes nuevas: `frontend/src/history-api.ts` y su test. Cambios compartidos: solamente export de tres funciones ya existentes en `task-status-api.ts`, `reschedule-api.ts` y `work-session-state-api.ts`; cuerpos y pruebas heredadas intactos. Se usan también isBlock, isSessionStart, microseconds, exact, text, uuid, sameId y apiRequest existentes. No nueva librería, petición por fila ni decodificación del cursor en cliente.

Cobertura de este corte: transporte GET privado/signal, Response no200; envoltorio y entradas cerrados, cinco familias y variantes discriminadas reutilizadas, correspondencia de identidad/contexto/instante, UTC0001–9999 y microsegundos exactos, etiquetas actuales no vacías, veinte entradas, cursor opaco textual, orden por tiempo/rango/UUID y unicidad(type,id), coincidencia de filtros, captura de consulta previa al await. Los positivos cubren empates, frontera20, filtros inclusivos, CLOSE de época1600/neto grande, RESUME, EXTEND y RESCHEDULED. Las matrices internas de validadores de detalles se reutilizan; no se duplican aquí.

Pendiente: página Historial, navegación y enlaces de detalles, formulario y URL, estados/carga/errores, post-await/privacidad del montaje, foco/teclado/responsive y evidencia real UX/E2E. El cliente no añade validación de sintaxis del cursor ni sustituye la precedencia HTTP del servidor. La prueba E2E inicial de C sigue RED por enlace ausente; no indica fallo de este decoder.
