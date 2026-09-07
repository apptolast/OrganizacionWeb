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

## Página e integración — ciclos individuales

| Ciclo UI | Oráculo | RED | GREEN | Cambio |
| --- | --- | --- | --- | --- |
| 1 | @s28 navegación existente, URL y vacío confirmado | d48d1b, enlace ausente | log UI01green_final | History, enlace Workspace y ruta App |

Primer intento del ciclo UI1 todavía rojo 89a795: sustitución textual no actualizó section de Workspace, por lo que faltaba aria-current. Corregido dentro del mismo ciclo; no se atribuye ese intento a GREEN.
| 2 | @s34 espera sin ausencia | cdd5ff | 05a035, 2/2 | status de carga |
| 3 | @s34 fallo503 sin ausencia/carga indefinida | 242c72, incluye rechazo no capturado | e9aa0c, 3/3 | catch y error visible |
| 4 | @s35 reintento GET con misma URL | 315d06 | log UI04green, 4/4 | refresh manual y espera anunciada |
| 5 | @s36 HTTP401 tardío tras desmontar | 824132 | 43ded2, 5/5 | AbortController y guardas tras await |
| 6 | @s32 inicio con enlaces de contexto/sesión | b347bf | 9d131b, 6/6 | lista semántica y SnapshotTime existente |
| 7 | @s33 lista mixta de cinco familias | 9a8b5b | log UI07green, 7/7 | etiquetas discriminadas y sessionId del recibo |
| 8 | @s34 URL nueva no muestra datos anteriores como vigentes | 586b4a | c337cd, 8/8 | resultado asociado a ruta y refresh |
| 9 | @s29 borrador explícito de categoría/fechas, reinicia cursor | 0f2060 | log UI09green_final, 9/9 | formulario nativo y URL existente |

Incidentes de edición dentro del ciclo: UI8 aebed7 no compilaba porque la sustitución de líneas conservó los estados anteriores; UI9 98c8b6 dejó el handler sin insertar. Se corrigieron las sustituciones antes de GREEN, sin relajar pruebas. Esos intentos no se cuentan como pases.
| 10 | @s34 vacío filtrado y limpiar filtros | 75f543 | 718aa3, 10/10 | mensaje propio y enlace nativo |
| 11 | @s29/@s31 refuerzo del caso existente con cursor vacío | 918712 | 0811e3, 10/10 | no afirmar ausencia global desde continuación |
| 12 | @s31 página reemplazada y volver a recientes | 2a7002 | log UI12green, 11/11 | enlaces URL con filtros conservados |

UI10 actualiza el texto esperado del caso UI4: su consulta ya tenía category=sessions, por lo que ahora corresponde vacío filtrado. Conserva los oráculos de reintento, URL, número de peticiones y anuncio de espera. UI11 fortalece un caso existente, no agrega otro test.
| 13 | @s37 HTTP401 vigente | f59234 | 3d5964, 12/12 | retirar controles y orientar autenticación |
| 14 | @s37 contexto404 y quitar sólo contexto/cursor | a05a85 | 9a1733, 13/13 | salida conservando categoría/fechas |
| 15 | @s33 cierre con notas/tiempo propio/atribución | 7fdf14 | log UI15green, 14/14 | details nativo, texto y SCSS mínimo pre-wrap |

El test UI15 verifica DOM, texto sin HTML y fallback; no acredita aún la geometría física del texto. La regla SCSS conserva espacios/saltos y permite envolver líneas; su reflow se medirá con UX real.
| 16 | @s33 EXTEND con fines y revisión local | 2b5e93 | 9bfc5c, 15/15 | datos propios sin trabajo inventado |
| 17 | @s33 inicio con tiempo previsto original | 154a78 | e2dc36, 16/16 | detalles históricos de inicio |
| 18 | @s33 pausa con acumulado, no total final | d51699 | log UI18green, 17/17 | etiqueta y seconds heredado |

Root comunicó aclaración de seguridad18 commit5e2ad1e: @s18 prueba GET autenticado sin CSRF/query inválida como400query, no un403 imposible. No cambia UI ni se modifica el contrato desde este paquete.
| 19 | @s33 reserva original con intervalo | 30947b | da279b, 18/18 | hechos de reserva, no trabajo |
| 20 | @s33 replanificación con before/after | 600b44 | 99a0c2, 19/19 | ReservationDetails y revisión local |
| 21 | @s33 reapertura visible | 6426c8 | log UI21green, 20/20 | transición histórica de tarea |

UI20 ajustó la consulta del oráculo UI7 a headings de nivel2: los nuevos detalles añaden headings de nivel3. Se conservan las cinco etiquetas, orden, destinos y una petición; no se selecciona un primer elemento arbitrario. Primer intento 7b57d3 reflejó ese desajuste de ámbito antes del verde final.

## Checkpoint nominal de montaje para revisión y primer E2E

**Cinco archivos congelados**: `history.tsx`, `history.test.tsx`, `history.scss`, `App.tsx`, `workspace.tsx`, bajo frontend/src. Dependencia: cliente aprobado99f2971. Manifiesto en `progress/history_ui_checkpoint.json`.

20/20 pruebas UI verdes después del formato, log `progress/history_ui_checkpoint_tests.log`, 39452c. ESLint y TypeScript EXIT0 d88e6f. Prettier requirió una segunda pasada del test (acbdc4): el check después de la primera escritura detectó aún formato pendiente, como ya ocurrió con cadenas de mocks en17. El check final de los cinco archivos está verde. No se declara estable la primera pasada ni se atribuye el aviso al producto.

Incluye: acceso mediante Workspace, URL/página única, filtros explícitos nativos, paginación y recientes, estados de espera/vacío/error, recuperación GET, aborto antes de401 tardío, retirada de datos al cambiar consulta, cinco familias y sus detalles históricos con enlaces de sesión correctos. Reservas, inicio previsto, pausa acumulada, cierre final y ampliación están diferenciados; texto de notas y atribución se conservan. No catálogos nuevos ni consultas por fila.

**Pendientes explícitos de este checkpoint**, que no es freeze final de18: enlaces desde detalles de proyecto/tarea y enlaces para filtrar contexto desde filas; etiqueta genérica de contexto vacío; regla de fechas from>to en formulario; foco/teclado al sustituir controles, descarte de JSON diferido tras cambiar consulta y regresiones de composición; explicación visible de empates/revisión local; SCSS responsive y evidencia completa30UX. La lectura de los details heredados y el cursor se reutilizan; no repetir matrices internas. El primer E2E vacío puede ejecutarse sobre este corte, sin presentarlo como cierre global.

## UI 22–23 — enlaces desde detalles existentes

UI22: el nuevo oráculo público del proyecto completado falló por ausencia del enlace (7f2fd9, history_ui_22_red.log). Se añadió únicamente el enlace dentro del detalle autorizado. ReadProjects completo: 36/36 GREEN a78cad, history_ui_22_green.log.

UI23: el detalle de tarea falló por ausencia de «Ver historial de esta tarea» (94a87f, history_ui_23_red.log). Se añadió el enlace con ambos identificadores conocidos, sin cambiar el enrutamiento del fixture ni sus oráculos de peticiones inesperadas. El primer intento de escritura falló por el parámetro PowerShell NoNewline; ese intento no cambió producción y su ejecución siguió roja (history_ui_23_green.log). Escritura corregida y suite completa 3/3 GREEN 1f6650, history_ui_23_green_final.log.

El checkpoint nominal fue aprobado e integrado por root en 9fa0e58; sus cinco archivos están liberados. Continúo sólo los pendientes contractuales.

## UI 24–27 — contexto y foco del reintento

UI24: contexto vacío sin nombre inventado ni consulta adicional, RED bd4ab0 → 21/21 GREEN cf7c90. UI25 refuerza el nominal existente con los dos enlaces contextuales de fila: RED 520dbc → 21/21 GREEN 55575f. Los enlaces filtran el contexto conocido, sin crear catálogos ni rutas nuevas.

UI26 refuerza el reintento existente con foco en encabezado al desaparecer su botón: RED ddb963 → 21/21 GREEN aa9c16. UI27 añade movimiento deliberado a categoría y posterior blur mientras espera: RED fff919 acreditó apropiación indebida del foco al terminar; el listener local invalida esa intención y 22/22 GREEN c8cacc. No bandera permanente de interacción. Logs history_ui_24–27_red/green.log conservados.

## UI 28–35 — navegación y retirada

UI28 y UI29 refuerzan respectivamente aplicar filtros y paginación existentes con foco en el encabezado: RED bc45dd/6f1945 → GREEN 184639/847bc3, 22/22. UI30 añade al caso de cinco familias la advertencia contractual de empate no causal: RED 300267 → GREEN b1b2ce, 22/22.

UI31 HTTP401 antiguo tras nueva página: inicialmente GREEN 438bc9. UI32 JSON200 diferido tras nueva página: inicialmente GREEN ec8eb7. UI33 problema503 desconocido antiguo: inicialmente GREEN 2aa904; esta UI clasifica por HTTP sin await del cuerpo del problema, por lo que no se inventa una etapa asíncrona de clasificación inexistente. Las tres guardas usan consultas reales del cliente y no alteraron producción.

UI34 y UI35 acreditan retirada de notas de CLOSE previamente visibles y enlaces contextuales ante401/404 vigentes. Inicialmente GREEN e3ed80/f68690. Se mantienen los oráculos iniciales de ausencia de acceso y limpieza de contexto. Logs individuales history_ui_28–35 preservados; 27 casos UI acumulados, no equivalencia con los142 ejemplos del contrato.

## UI 36–38 — URL completa

UI36 limpiar todos los filtros/contexto/cursor con GETglobal: inicialmente GREEN 10e5a8. UI37 refuerza navegación existente con Back real de history y recuperación de una sola página anterior: inicialmente GREEN 7c36d8. UI38 arranque nuevo directamente en URL antigua con cursor opaco y filtros: inicialmente GREEN; no necesita cadena en memoria. Esto acredita montaje desde URL en jsdom, no una recarga física de navegador. El E2E complementará ese límite. No cambios productivos en estos tres ciclos.

## Freeze funcional final — 7 de septiembre de 2026, 12:04 Europe/Madrid

UI38 inicialmente GREEN cc98d0. Tras formato focal, History 29 casos y regresión de ReadProjects, composición WorkSession y TaskBlocks: 154/154 en cuatro suites, EXIT0 512ba0 (history_ui_final_tests.log). Prettier check, ESLint focal y TypeScript sin emisión: EXIT0 ff1589; logs history_ui_final_format_check.log, history_ui_final_lint.log y history_ui_final_types.log. El check final de formato pasó después de una escritura, sin repetir el incidente del checkpoint anterior.

Manifest history_frontend_freeze.json congela14 archivos: nueve de montaje/evidencia UI y cinco de cliente/validadores ya aprobados. Delta desde checkpoint nominal: seis archivos, enlaces de los dos detalles, contexto de página/fila, aviso no causal, foco local y oráculos. No cambios a la lógica del cliente ni sus validadores. App, Workspace y SCSS permanecen iguales al checkpoint aprobado.

### Mapa frontend y límites

- @s25–27: 53 oráculos de cliente ya aprobados (528 con herencia), precisión BigInt, detalles cerrados, contextos, filtros, orden y rechazo íntegro. UI reutiliza SnapshotTime/seconds intactos; su oráculo heredado muestra 0,000001 s. CLOSE UI conserva notas, fecha atribuida y fallback UTC; la prueba de época1000/1600 y neto superior a Number pertenece al cliente, no se atribuye a una captura de navegador.
- @s28–32: navegación Principal y enlaces de detalle proyecto completado/tarea; contexto vacío genérico sin consulta de nombres; aplicar/limpiar; una página por URL, antiguos/recientes, Back y montaje nuevo con URL antigua; enlaces de fila a rutas existentes y filtros de contexto. Recarga física pendiente de E2E.
- @s33: cinco familias, detalles nativos sin GET por fila, notas plaintext y whitespace, intervalos originales/reservados, cierre/neto propio, extensión/fin, transición/reapertura y revisiones locales. Advertencia de empate sin causalidad; sin estadísticas ni escrituras.
- @s34–35: pendiente anunciado síncronamente, vacíos separados,503 conservando URL y GET de reintento. La prueba jsdom no acredita latencia física de400ms.
- @s36–37: HTTP401 antes del observador, JSON diferido y503 obsoletos;401/404 vigentes retiran notas y contexto previamente visibles. La clasificación de error no espera JSON; no se inventa cobertura de un await inexistente.
- @s38: foco al desaparecer iniciador en aplicar/paginación/reintento; intento cancelado al mover deliberadamente foco, incluso si luego queda en body. Semántica y foco comprobados en DOM, no lector físico.
- @s39: etiquetas, jerarquía nativa y notas con pre-wrap presentes; matriz31anchos,44px, tres motores, axe, texto y zoom siguen pendientes de UX real. SCSS sólo conserva notas y no se presenta como prueba de geometría. Ningún nuevo CSS especulativo durante esta entrega.

Backend, HTTP, cursor, snapshot, persistencia y publicación corresponden a los otros autores y a su evidencia; no se convierten142ejemplos contractuales en142tests frontend. E2E nominal corre sobre el snapshot previo aprobado de C, no se atribuye automáticamente a este delta. Sin Git ni campañas globales por B.

## UI39 — rango invertido y refreeze

Root señaló el rango invertido pendiente, no un fallo del contrato HTTP. Único recorrido añadido: fechas invertidas → error accesible enlazado a Hasta sin GET ni cambio de cursor → corregir fecha y aplicar GET válido. RED 58da38 (history_ui_39_red.log); mínimo local, 30/30 History GREEN d47b5d (history_ui_39_green.log). El error se retira al aplicar válido y no se arrastra a otra URL. No nueva matriz de fechas heredadas ni cambios al decoder.

Formato/check, ESLint y tipos focales terminados tras el ajuste; logs history_ui_39_format_check.log, history_ui_39_lint.log y history_ui_39_types.log. El pase previo154/154 sigue preservado para los otros tres archivos de pruebas sin cambios; sólo History pasó de29 a30, no se etiqueta155 como una ejecución conjunta nueva. Manifest anterior FD49C9…5B7D preservado en history_frontend_before_range_freeze.json; nuevo history_frontend_freeze.json cambia únicamente History y su test. Resto doce hashes idénticos. Freeze final de producto; CSS sólo si UX real demuestra necesidad y root coordina.
