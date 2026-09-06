# Mutación — feature11 frontend

**Veredicto:** PASS (umbral numérico; clasificación y refuerzos pendientes de coordinador)
**Umbral:** 80% (harness0.8/Strykerbreak80).

Rol mutation_tester leído; Ponytail full/Caveman lite. Judge completo APPROVED en progress/judge_schedule_block.md y init34832 EXIT0 verificados por coordinador. Ejecución autorizada vía arnés: `node .harness/harness.mjs mutate schedule_block-frontend`.

Scope cerrado aprobado: src/schedule-block-api.ts, src/task-blocks.tsx, src/task-reader.tsx y src/task-state.tsx completos. ignorePatterns protector activo y comprobado sintéticamente previamente. No se editan fuentes/tests, no se reducen umbrales, no se limpian destinos protegidos ni se repiten acciones rechazadas.

Ejecución sesión13670 iniciada11:17:07 local, procesoStryker5568. ProjectReader seleccionó4de135archivos; instrumentación1561mutantes (1d60c4). Baseline/score/resultados aún pendientes. PITbackend corre en paralelo por otro autor; cualquier Timeout se desglosará y no se presentará como killed por aserción.
Baseline Stryker PASS11:18:18 (15be56):709tests en1min1s, net32234,95ms y overhead29077,05ms. Este conteo es el reportado por el mutador y no sustituye las1121pruebas del initglobal. Mutantes aún en ejecución.
Coordinador creó/subió checkpoint3671b94 durante la ejecución, confirmando mismo código medido. Fuentes/tests se mantienen congelados. Dos workers activos40252/58828; reporter clear-text no publica conteos intermedios. JSON preexistente fechado00:12 no se usa como evidencia de esta ejecución; se espera su reemplazo final.
Muestras12:12:53→12:13:42 (48,57s), elapsed3400,4s/56min40s desde inicio. Worker42348 CPU1360,34→1417,27 (+56,92s),58828 CPU3512,59→3567,86 (+55,27s), memoria360/788MB al final. Parent CPU17,406→17,422. Cómputo activo confirmado, no porcentaje de avance; actividadCPU no descarta bucle activo. Único avance observable desde baseline es reemplazo de worker40252 por42348 a11:51:33; causa no publicada. Sin informe nuevo. Muestras en .e2e-work/mutation-frontend-worker-samples.jsonl. No se cancela ni reinicia.


## Resultado final de la ejecución original

PASS numérico, sesión13670 EXIT0 (93196a), 11:17:07–12:20:08 local: 63 min 1 s. Checkpoint3671b94, sin cambios de fuentes/tests durante ejecución.

1332 Killed / 1560 válidos = **85,38%**; umbral80%. Denominador conservador con RuntimeError: 1332/1561 = **85,33%**, también supera80%. **225 Survived, 3 NoCoverage, 1 RuntimeError, 0 Timeout**. Ningún Timeout se presenta como detección. No se excluyen equivalencias para elevar el score.

| Archivo | Killed | Survived | NoCoverage | RuntimeError | Score Stryker |
| --- | ---: | ---: | ---: | ---: | ---: |
| schedule-block-api.ts | 631 | 85 | 0 | 0 | 88,13% |
| task-blocks.tsx | 470 | 114 | 3 | 1 | 80,07% |
| task-reader.tsx | 112 | 9 | 0 | 0 | 92,56% |
| task-state.tsx | 119 | 17 | 0 | 0 | 87,50% |

Informe nuevo de12:20 conservado antes de cualquier replay: `.e2e-work/schedule-block-mutation/3671b94/mutation.json` y `mutation.html`. SHA256 JSON: `4c95f22fef3d0040e52303c96427351a11fefaa4358f9d8f070944b1ec003aec`. Original publicado por Stryker: `frontend/reports/mutation/mutation.json` y `.html`. El informe antiguo00:12 no se usa. Inventario global mecánico: `.e2e-work/schedule-block-mutation/3671b94/inventory.json` (229 identidades).

NE significa diferencia observable/oráculo faltante, no defecto demostrado en producción original. EQ conserva flujo público bajo invariantes descritas. VP cambia conducta sin incumplimiento demostrado, y no se suma como equivalencia. Los casos pendientes requieren decisión del coordinador antes de pruebas/replay. API85 pertenece al revisor independiente; referencias en `progress/review_schedule_block_frontend_mutation.md`. No se duplica su análisis aquí.

## Clasificación UI por comportamiento

- **NE — 726, 728, 759, 770, 776, 823**: Listado: comprobar ausencia de filas, error y mensaje vacío obsoletos durante retry/paginación y volver a recientes tras guardar desde página antigua. Eliminar setPage en reload requiere partir de una página existente (confirmación), no sólo un primer GET fallido.

- **EQ — 731, 1108**: El contador sólo invalida una dependencia del efecto. +1 y -1 generan un valor distinto; no se expone ni compara su orden.

- **NE — 736**: Resolver primera lectura cancelada de StrictMode después de la vigente; mantener exclusivamente listado vigente.

- **NE — 744, 746**: Listado cancelado antes/después de readBlockError: entregar RESOURCE_NOT_FOUND tarde mientras TaskReader sigue vivo tras retirar/remontar hijos; no retirar contexto vigente.

- **NE — 750**: Rechazo no Response devuelve null al clasificar: mostrar error de listado recuperable sin rechazo asíncrono no controlado.

- **NE — 757**: Comprobar tabIndex=-1 y recorrido secuencial; +1 altera el orden del teclado.

- **EQ — 803, 875, 879**: Refs: sección/h2 y form se renderizan incondicionalmente durante estas llamadas montadas. El fallback se invoca antes de onConfirmed; useLayoutEffect no corre después del desmontaje. No se generaliza a refs de TaskReader.

- **NE — 814, 816**: Abrir editor con proyecto activo y confirmar proyecto completed antes del preview/guardado nuevo: no permitir operación nueva. Mantener comprobación/reenvío de identidad retenida.

- **NE — 822**: Tras creación confirmada debe desaparecer editor y conservarse bloque confirmado; no dejar un borrador aún operativo.

- **NE — 826, 827, 828, 829, 830**: Abrir formulario antes de resolver configuración: objetivo/fechas vacíos, ninguna zona inventada. Fechas inválidas se normalizan a vacío por input datetime-local: contrastar payload de envío sin rellenarlas antes de llamar equivalente.

- **NE — 831, 833, 834, 838**: Primera apertura y preview con exceso: sin errores ficticios y consentimiento desmarcado. Comprobar que Guardar sigue deshabilitado hasta aceptación explícita.

- **VP pendiente — 886, 887**: Amplía restauración al elemento origin mientras permanece guarda :disabled. Examinar cancelación/desconexión real: no forzar foco a nodo retirado ni fabricar origin interno. No se declara equivalente ni hueco confirmado sin ese recorrido.

- **NE — 894, 897, 898**: Confirmación asíncrona: conservar foco elegido en otro control y restaurar destino visible si body conserva foco. Abarcar tanto Guardar como Comprobar.

- **EQ — 918, 1306**: Dependencias constantes [] y [cadena constante] conservan montaje/cleanup: React compara elementos primitivos, no identidad del array.

- **EQ — 921, 922, 923, 924, 960**: Guardas internas de check/save: botones públicos sólo existen con identidad/review y quedan disabled durante saving. Save nuevo además exige elegibilidad; reenvío conserva request. No hay caller externo de estas funciones privadas.

- **NE — 928**: Comprobación diferida: botón Comprobar y Reenviar deben quedar deshabilitados; evitar consultas/confirmaciones paralelas.

- **NE — 931**: Cerrar editor con check pendiente, abrir otro y entregar éxito antiguo: no confirmar bloque ni cerrar borrador vigente mediante callback del editor desmontado.

- **EQ — 936, 938, 981, 983, 1052, 1054**: Estas guardas separadas sólo evitan setters locales después de cancelación; classify contiene su propia guarda antes de onAccessFailure. Guardas hermanas anteriores/posteriores siguen activas. En inspect un rechazo antiguo previo a classify aún queda frenado después de await; no contar eliminar ambas a la vez.

- **NE — 942**: Tras check que falla por red o respuesta desconocida, no habilitar reenvío; sólo BLOCK_NOT_FOUND autoriza ausencia confirmada.

- **EQ — 949, 1023**: Finally de check/save: cancelación sólo al desmontar editor; setSaving tardío sólo apunta al montaje retirado, sin callback.

- **NE — 969, 996, 1016**: Estado de recuperación: comprobar ausencia de error de guardado al reintentar, CSRF definitivo sin mensaje de incertidumbre y rechazo de negocio sin acción Reenviar retenida. Para 1016 la UI exterior depende uncertain/csrfRejected ya falsos: puede ser estado latente sin efecto; contrastar ciclo posterior antes de exigir prueba.

- **NE — 1036, 1037**: Formulario permite envío por Enter además del botón. Con elegibilidad retirada o revisión pendiente, submit no debe emitir un nuevo preview. No basta que botón esté disabled.

- **NE — 1041**: Después de fallo de preview, reintentar y mantener respuesta pendiente: retirar error anterior mientras revisa.

- **NE — 1070**: Preview antiguo cancelado por edición: su finally no debe retirar Revisando del preview nuevo pendiente.

- **NE — 1079, 1087**: Configuración: StrictMode o reintento con lectura anterior diferida; no reemplazar zona/catálogo ni mostrar error obsoleto sobre respuesta vigente. Verificar entrelazado alcanzable.

- **NE — 1084, 1091, 1105, 1107**: Configuración: éxito sin alerta falsa; retry retira error; dos fallos y tercer intento realmente consulta; cerrar/navegar aborta llamada para que 401 tardío no retire SessionGate vigente.

- **NE — 1094**: Submit nativo conserva ruta y borrador, sin navegación/recarga por acción default. Test de evento cancelado o navegador con Enter.

- **NE — 1124, 1140, 1143, 1154, 1156, 1157, 1158, 1186**: DST: elegir ocurrencias, cambiar fecha/zona/ocurrencia; invalidar revisión y consentimiento, enviar offsets null cuando cambió extremo/zona. Cambiar sólo fin conserva elección de inicio (1143).

- **NE — 1177, 1181, 1183, 1184**: Errores de ocurrencias asociados aria-invalid y selección controlada distinta de inicio/fin. Comprobar placeholder vacío antes de selección y cambio de cada extremo por separado.

- **NE — 1196, 1197, 1198, 1199, 1200**: Mostrar desfase real en opciones con offsets diferentes y caso Z como UTC+00:00. validOffsets acepta Z; NoCoverage1200 muestra falta de ejercicio de esa rama, no imposibilidad.

- **NE — 1219, 1246, 1248, 1250**: Preview de varios días, sólo uno con exceso: exige consentimiento. Con todos los excesos cero no mostrar aceptación; some/every y >/>= son distinguibles por esos dos casos.

- **NE — 1234, 1237, 1273, 1286, 1289**: Comprobar mensajes/acciones sólo en estado correspondiente: sin falsos errores en apertura/éxito; instrucciones disponibilidad sólo por error aplicable; incertidumbre incluye explicación y conservación de identidad.

- **NE — 1241, 1268, 1269**: Legibilidad de textos completos: espacio entre Inicio y fecha, reservado y cantidad, solicitado y segundos. No declarar equivalente por tratarse de cadenas.

- **NE — 1267**: BUDGET_EXCEEDED muestra cada día con presupuesto/reservado/solicitado/exceso y zona; mensaje genérico no basta.

- **NE — 1301, 1310, 1312, 1331, 1332, 1333, 1336**: Consulta del conflicto: sin error inicial, carga visible y botón disabled durante respuesta diferida; retry retira error; éxito no conserva alerta.

- **NE — 1303, 1304**: Cerrar/remontar conflicto con GET pendiente y entregar 401 tarde: cleanup debe abortar petición que atraviesa observador global de acceso.

- **EQ — 1315, 1320, 1326**: Dentro de conflicto, request sólo aborta al desmontar. Resultado/error/finally sólo contienen setters del componente retirado. No callbacks externos después de await. Cleanup eliminado sí es diferente por apiRequest global (1303/1304).

- **NE — 1342, 1344, 1345, 1346, 1347, 1348, 1349, 1350**: Fecha visible en zona original distinta de UTC, con fecha/hora/segundos/desfase y localización española. Opciones inválidas llevan al fallback UTC, cambio observable aunque datetime siga correcto; locale vacío puede cambiar idioma.

- **NE — 963**: NoCoverage: recuperación con request retenida y elegibilidad retirada. Reenvío de identidad idéntica sigue permitido tras comprobar BLOCK_NOT_FOUND; mutación request por !request bloquea ese flujo.

- **NE — 1082**: NoCoverage: disponibilidad no configurada debe dejar selector sin zona seleccionada y ofrecer configuración; no usar valor ficticio. Comparar valor visible y payload de preview antes de asignar zona.

- **RuntimeError — 945**: Optional chaining problem?.code eliminado tras check desconocido. Runner falla en errorToString: Cannot convert object to primitive value, tras dos reinicios. No CompileError, Timeout ni Killed. Requiere replay acotado separado antes de concluir conducta del mutante.

- **VP histórica — 1490, 1497, 1498, 1499, 1501, 1368**: Variantes temporales de foco ya aceptadas: destino visible cuando body mantiene foco, sin robar un control elegido. No equivalencia semántica estricta. Correspondencia exacta abajo.

- **EQ histórica — 1505, 1511, 1514, 1515, 1537, 1556, 1573**: TaskState: h2 siempre montado; dependencia constante; guarda redundante con botón público; historyRevision sólo key; finally sólo setter local tras desmontaje. Flujo correspondiente se mantiene salvo caso1530 separado.

- **EQ histórica — 1380, 1455**: TaskReader revision/projectRevision sólo invalidan efecto, sin semántica ordinal pública.

- **Reabierta, NE potencial — 1401, 1403, 1412, 1427**: La lectura de detalle sólo devuelve ProjectSnapshot o lanza, pero key=route no basta: fallo privado de otro hijo permite Reintentar tarea dentro de TaskReader vivo. Ese retry cancela GET proyecto pendiente. Una respuesta/rechazo antiguo podría reemplazar snapshot, retirar contexto o finalizar carga nueva. Reproducir este recorrido antes de reutilizar equivalencia histórica; no se afirma fallo en producción original.

- **NE — 1530**: La equivalencia histórica131 ya NO se sostiene: success añade onSnapshot(result), callback del TaskReader vivo. Tras desmontar TaskState por fallo de otro hijo, reintentar tarea y resolver PUT viejo; no habilitar Planificar ni reemplazar estado vigente.

- **Alcance pendiente — 1532, 1541, 1566, 1577**: onSnapshot es prop opcional exportada; único caller de aplicación TaskReader la pasa. Sin callback, quitar ?. provoca fallo observable. Coordinador debe decidir prueba de contrato opcional del componente o equivalencia restringida a composición actual; no inventar caller interno ni descartar silenciosamente.

- **NE — 1465**: Contexto proyecto con snapshot previo pero reconsulta pendiente/error: no habilitar Planificar hasta estado confirmado. OR debilita las dos guardas.

- **EQ — 1468**: Rama !projectLoading && !projectFailure requiere GET de detalle confirmado para snapshot; inicial/retry mantienen loading=true, error pone failure=true. readProjects detalle no devuelve colección. No acceso a snapshot undefined alcanzable en composición actual.

## Correspondencia histórica exacta

Comparación de expresión original normalizada, mutador y reemplazo; números de línea no identifican una mutación entre ejecuciones. La igualdad sintáctica no basta cuando cambia el flujo posterior (1530).

| ID actual | Informe histórico / ID | Expresión + mutador + reemplazo iguales | Resolución |
| --- | --- | --- | --- |
| 1490 | complete-reopen-task / 91 | Sí | VP histórica |
| 1497 | complete-reopen-task / 98 | Sí | VP histórica |
| 1498 | complete-reopen-task / 99 | Sí | VP histórica |
| 1499 | complete-reopen-task / 100 | Sí | VP histórica |
| 1501 | complete-reopen-task / 102 | Sí | VP histórica |
| 1505 | complete-reopen-task / 106 | Sí | EQ histórica |
| 1511 | complete-reopen-task / 112 | Sí | EQ histórica |
| 1514 | complete-reopen-task / 115 | Sí | EQ histórica |
| 1515 | complete-reopen-task / 116 | Sí | EQ histórica |
| 1530 | complete-reopen-task / 131 | Sí | Reabierta: callback onSnapshot nuevo |
| 1537 | complete-reopen-task / 137 | Sí | EQ histórica |
| 1556 | complete-reopen-task / 155 | Sí | EQ histórica |
| 1573 | complete-reopen-task / 171 | Sí | EQ histórica |
| 1368 | split-task / 205 | Sí | VP histórica |
| 1380 | split-task / 217 | Sí | EQ histórica |
| 1401 | split-task / 238 | Sí | Reabierta, NE potencial |
| 1403 | split-task / 240 | Sí | Reabierta, NE potencial |
| 1412 | split-task / 249 | Sí | Reabierta, NE potencial |
| 1427 | split-task / 264 | Sí | Reabierta, NE potencial |
| 1455 | split-task / 292 | Sí | EQ histórica |

Fundamentos históricos: `progress/review_complete_reopen_task_frontend_mutants.md` y `progress/review_split_task_frontend_mutants.md`. Sin replay histórico repetido.

## Inventario individual UI/reader/state

Cada fila enlaza por ID al comportamiento/oráculo anterior; fuentes y reemplazos salen del JSON preservado, no de inferir una línea actual.

| ID | Archivo:línea | Estado | Mutador | Original | Reemplazo | Clasificación |
| --- | --- | --- | --- | --- | --- | --- |
| 963 | src/task-blocks.tsx:265 | NoCoverage | BooleanLiteral | `!request` | `request` | NE |
| 1082 | src/task-blocks.tsx:383 | NoCoverage | StringLiteral | `""` | `"Stryker was here!"` | NE |
| 1200 | src/task-blocks.tsx:553 | NoCoverage | StringLiteral | `"+00:00"` | `""` | NE |
| 726 | src/task-blocks.tsx:38 | Survived | CallExpression | `setPage(undefined);` | `;` | NE |
| 728 | src/task-blocks.tsx:39 | Survived | BooleanLiteral | `false` | `true` | NE |
| 731 | src/task-blocks.tsx:40 | Survived | ArithmeticOperator | `value + 1` | `value - 1` | EQ |
| 736 | src/task-blocks.tsx:46 | Survived | ConditionalExpression | `!controller.signal.aborted` | `true` | NE |
| 744 | src/task-blocks.tsx:52 | Survived | ConditionalExpression | `controller.signal.aborted` | `false` | NE |
| 746 | src/task-blocks.tsx:54 | Survived | ConditionalExpression | `controller.signal.aborted` | `false` | NE |
| 750 | src/task-blocks.tsx:55 | Survived | OptionalChaining | `problem?.code` | `problem.code` | NE |
| 757 | src/task-blocks.tsx:62 | Survived | UnaryOperator | `-1` | `+1` | NE |
| 759 | src/task-blocks.tsx:73 | Survived | ConditionalExpression | `page.items.length === 0` | `true` | NE |
| 770 | src/task-blocks.tsx:89 | Survived | CallExpression | `setPage(undefined);` | `;` | NE |
| 776 | src/task-blocks.tsx:99 | Survived | CallExpression | `setPage(undefined);` | `;` | NE |
| 803 | src/task-blocks.tsx:124 | Survived | OptionalChaining | `heading.current?.focus` | `heading.current.focus` | EQ |
| 814 | src/task-blocks.tsx:128 | Survived | ConditionalExpression | `projectStatus !== "completed"` | `true` | NE |
| 816 | src/task-blocks.tsx:128 | Survived | StringLiteral | `"completed"` | `""` | NE |
| 822 | src/task-blocks.tsx:136 | Survived | BooleanLiteral | `false` | `true` | NE |
| 823 | src/task-blocks.tsx:137 | Survived | CallExpression | `setCursor(undefined);` | `;` | NE |
| 827 | src/task-blocks.tsx:163 | Survived | StringLiteral | `""` | `"Stryker was here!"` | NE |
| 826 | src/task-blocks.tsx:162 | Survived | StringLiteral | `""` | `"Stryker was here!"` | NE |
| 828 | src/task-blocks.tsx:164 | Survived | StringLiteral | `""` | `"Stryker was here!"` | NE |
| 829 | src/task-blocks.tsx:170 | Survived | StringLiteral | `""` | `"Stryker was here!"` | NE |
| 830 | src/task-blocks.tsx:171 | Survived | ArrayDeclaration | `[]` | `["Stryker was here"]` | NE |
| 831 | src/task-blocks.tsx:172 | Survived | BooleanLiteral | `false` | `true` | NE |
| 833 | src/task-blocks.tsx:176 | Survived | BooleanLiteral | `false` | `true` | NE |
| 834 | src/task-blocks.tsx:177 | Survived | BooleanLiteral | `false` | `true` | NE |
| 838 | src/task-blocks.tsx:183 | Survived | BooleanLiteral | `false` | `true` | NE |
| 875 | src/task-blocks.tsx:205 | Survived | OptionalChaining | `form.current?.elements` | `form.current.elements` | EQ |
| 879 | src/task-blocks.tsx:207 | Survived | OptionalChaining | `form.current?.elements` | `form.current.elements` | EQ |
| 886 | src/task-blocks.tsx:210 | Survived | ConditionalExpression | `previous instanceof HTMLElement &&       previous.isConnected` | `true` | VP pendiente |
| 887 | src/task-blocks.tsx:210 | Survived | LogicalOperator | `previous instanceof HTMLElement &&       previous.isConnected` | `previous instanceof HTMLElement \|\| previous.isConnected` | VP pendiente |
| 894 | src/task-blocks.tsx:220 | Survived | ConditionalExpression | `document.activeElement === document.body \|\|       document.activeElement === origin.current` | `true` | NE |
| 897 | src/task-blocks.tsx:220 | Survived | ConditionalExpression | `document.activeElement === document.body` | `false` | NE |
| 898 | src/task-blocks.tsx:220 | Survived | EqualityOperator | `document.activeElement === document.body` | `document.activeElement !== document.body` | NE |
| 918 | src/task-blocks.tsx:237 | Survived | ArrayDeclaration | `[]` | `["Stryker was here"]` | EQ |
| 921 | src/task-blocks.tsx:240 | Survived | ConditionalExpression | `!request \|\| !review \|\| saving` | `false` | EQ |
| 922 | src/task-blocks.tsx:240 | Survived | LogicalOperator | `!request \|\| !review \|\| saving` | `(!request \|\| !review) && saving` | EQ |
| 923 | src/task-blocks.tsx:240 | Survived | ConditionalExpression | `!request \|\| !review` | `false` | EQ |
| 924 | src/task-blocks.tsx:240 | Survived | LogicalOperator | `!request \|\| !review` | `!request && !review` | EQ |
| 928 | src/task-blocks.tsx:242 | Survived | BooleanLiteral | `true` | `false` | NE |
| 931 | src/task-blocks.tsx:253 | Survived | ConditionalExpression | `!controller.signal.aborted` | `true` | NE |
| 936 | src/task-blocks.tsx:255 | Survived | ConditionalExpression | `controller.signal.aborted` | `false` | EQ |
| 938 | src/task-blocks.tsx:257 | Survived | ConditionalExpression | `controller.signal.aborted` | `false` | EQ |
| 942 | src/task-blocks.tsx:259 | Survived | ConditionalExpression | `problem?.code === "BLOCK_NOT_FOUND"` | `true` | NE |
| 945 | src/task-blocks.tsx:259 | RuntimeError | OptionalChaining | `problem?.code` | `problem.code` | RuntimeError |
| 949 | src/task-blocks.tsx:261 | Survived | ConditionalExpression | `!controller.signal.aborted` | `true` | EQ |
| 960 | src/task-blocks.tsx:265 | Survived | ConditionalExpression | `!eligible && !request` | `false` | EQ |
| 969 | src/task-blocks.tsx:281 | Survived | BooleanLiteral | `false` | `true` | NE |
| 981 | src/task-blocks.tsx:296 | Survived | ConditionalExpression | `controller.signal.aborted` | `false` | EQ |
| 983 | src/task-blocks.tsx:298 | Survived | ConditionalExpression | `controller.signal.aborted` | `false` | EQ |
| 996 | src/task-blocks.tsx:303 | Survived | BooleanLiteral | `false` | `true` | NE |
| 1016 | src/task-blocks.tsx:321 | Survived | BooleanLiteral | `false` | `true` | NE |
| 1023 | src/task-blocks.tsx:325 | Survived | ConditionalExpression | `!controller.signal.aborted` | `true` | EQ |
| 1036 | src/task-blocks.tsx:336 | Survived | ConditionalExpression | `!eligible \|\| reviewing` | `false` | NE |
| 1037 | src/task-blocks.tsx:336 | Survived | LogicalOperator | `!eligible \|\| reviewing` | `!eligible && reviewing` | NE |
| 1041 | src/task-blocks.tsx:340 | Survived | BooleanLiteral | `false` | `true` | NE |
| 1052 | src/task-blocks.tsx:360 | Survived | ConditionalExpression | `controller.signal.aborted` | `false` | EQ |
| 1054 | src/task-blocks.tsx:362 | Survived | ConditionalExpression | `controller.signal.aborted` | `false` | EQ |
| 1070 | src/task-blocks.tsx:371 | Survived | ConditionalExpression | `!controller.signal.aborted` | `true` | NE |
| 1079 | src/task-blocks.tsx:381 | Survived | ConditionalExpression | `controller.signal.aborted` | `false` | NE |
| 1084 | src/task-blocks.tsx:384 | Survived | BooleanLiteral | `false` | `true` | NE |
| 1087 | src/task-blocks.tsx:387 | Survived | ConditionalExpression | `!controller.signal.aborted` | `true` | NE |
| 1091 | src/task-blocks.tsx:389 | Survived | ArrowFunction | `() => controller.abort()` | `() => undefined` | NE |
| 1094 | src/task-blocks.tsx:397 | Survived | CallExpression | `event.preventDefault();` | `;` | NE |
| 1105 | src/task-blocks.tsx:410 | Survived | BooleanLiteral | `false` | `true` | NE |
| 1107 | src/task-blocks.tsx:411 | Survived | ArrowFunction | `(value) => value + 1` | `() => undefined` | NE |
| 1108 | src/task-blocks.tsx:411 | Survived | ArithmeticOperator | `value + 1` | `value - 1` | EQ |
| 1124 | src/task-blocks.tsx:454 | Survived | CallExpression | `setStartOffset(null);` | `;` | NE |
| 1140 | src/task-blocks.tsx:482 | Survived | CallExpression | `setEndOffset(null);` | `;` | NE |
| 1143 | src/task-blocks.tsx:483 | Survived | ObjectLiteral | `{                 ...current,                 endOffset: undefined,               }` | `{}` | NE |
| 1154 | src/task-blocks.tsx:506 | Survived | CallExpression | `invalidate();` | `;` | NE |
| 1156 | src/task-blocks.tsx:508 | Survived | CallExpression | `setStartOffset(null);` | `;` | NE |
| 1157 | src/task-blocks.tsx:509 | Survived | CallExpression | `setEndOffset(null);` | `;` | NE |
| 1158 | src/task-blocks.tsx:510 | Survived | CallExpression | `setOffsetChoices({});` | `;` | NE |
| 1177 | src/task-blocks.tsx:536 | Survived | BooleanLiteral | `true` | `false` | NE |
| 1181 | src/task-blocks.tsx:541 | Survived | ConditionalExpression | `field === "startOffset"` | `false` | NE |
| 1183 | src/task-blocks.tsx:541 | Survived | StringLiteral | `"startOffset"` | `""` | NE |
| 1184 | src/task-blocks.tsx:541 | Survived | StringLiteral | `""` | `"Stryker was here!"` | NE |
| 1186 | src/task-blocks.tsx:544 | Survived | CallExpression | `invalidate();` | `;` | NE |
| 1196 | src/task-blocks.tsx:553 | Survived | ConditionalExpression | `offset === "Z"` | `true` | NE |
| 1197 | src/task-blocks.tsx:553 | Survived | ConditionalExpression | `offset === "Z"` | `false` | NE |
| 1198 | src/task-blocks.tsx:553 | Survived | EqualityOperator | `offset === "Z"` | `offset !== "Z"` | NE |
| 1199 | src/task-blocks.tsx:553 | Survived | StringLiteral | `"Z"` | `""` | NE |
| 1219 | src/task-blocks.tsx:574 | Survived | MethodExpression | `review.days.some((day) => day.excessSeconds > 0)` | `review.days.every(day => day.excessSeconds > 0)` | NE |
| 1234 | src/task-blocks.tsx:582 | Survived | LogicalOperator | `reviewFailure && (           <p role="alert">             No se pudo revisar el bloque. Conservamos tus datos.           </p>         )` | `reviewFailure \|\| <p role="alert">             No se pudo revisar el bloque. Conservamos tus datos.           </p>` | NE |
| 1237 | src/task-blocks.tsx:587 | Survived | LogicalOperator | `saveFailure && (           <p role="alert">             No se guardó el bloque. Revisa los datos antes de volver a guardar.           </p>         )` | `saveFailure \|\| <p role="alert">             No se guardó el bloque. Revisa los datos antes de volver a guardar.           </p>` | NE |
| 1241 | src/task-blocks.tsx:600 | Survived | StringLiteral | `" "` | `""` | NE |
| 1246 | src/task-blocks.tsx:617 | Survived | MethodExpression | `review.days.some((day) => day.excessSeconds > 0)` | `review.days.every(day => day.excessSeconds > 0)` | NE |
| 1248 | src/task-blocks.tsx:617 | Survived | ConditionalExpression | `day.excessSeconds > 0` | `true` | NE |
| 1250 | src/task-blocks.tsx:617 | Survived | EqualityOperator | `day.excessSeconds > 0` | `day.excessSeconds >= 0` | NE |
| 1267 | src/task-blocks.tsx:641 | Survived | ArrowFunction | `(day) => (             <p key={day.date}>               {day.date}: presupuesto {day.budgetMinutes} minutos, reservado{" "}               {day.plannedSeconds} segundos, solicitado {day.requestedSeconds}{" "}               segundos, exceso {day.excessSeconds} segundos.             </p>           )` | `() => undefined` | NE |
| 1268 | src/task-blocks.tsx:643 | Survived | StringLiteral | `" "` | `""` | NE |
| 1269 | src/task-blocks.tsx:644 | Survived | StringLiteral | `" "` | `""` | NE |
| 1273 | src/task-blocks.tsx:650 | Survived | ConditionalExpression | `issue?.code === "AVAILABILITY_REQUIRED" \|\|         issue?.code === "AVAILABILITY_ZONE_UNAVAILABLE"` | `true` | NE |
| 1286 | src/task-blocks.tsx:667 | Survived | ConditionalExpression | `uncertain \|\| csrfRejected` | `true` | NE |
| 1289 | src/task-blocks.tsx:672 | Survived | StringLiteral | `"No podemos confirmar el guardado. Conservamos este bloque y su identificación para comprobarlo sin duplicarlo."` | `""` | NE |
| 1301 | src/task-blocks.tsx:714 | Survived | BooleanLiteral | `false` | `true` | NE |
| 1303 | src/task-blocks.tsx:716 | Survived | ArrowFunction | `() => () => request.current?.abort()` | `() => undefined` | NE |
| 1304 | src/task-blocks.tsx:716 | Survived | ArrowFunction | `() => request.current?.abort()` | `() => undefined` | NE |
| 1306 | src/task-blocks.tsx:716 | Survived | ArrayDeclaration | `[]` | `["Stryker was here"]` | EQ |
| 1310 | src/task-blocks.tsx:721 | Survived | BooleanLiteral | `true` | `false` | NE |
| 1312 | src/task-blocks.tsx:722 | Survived | BooleanLiteral | `false` | `true` | NE |
| 1315 | src/task-blocks.tsx:730 | Survived | ConditionalExpression | `!controller.signal.aborted` | `true` | EQ |
| 1320 | src/task-blocks.tsx:732 | Survived | ConditionalExpression | `!controller.signal.aborted` | `true` | EQ |
| 1326 | src/task-blocks.tsx:734 | Survived | ConditionalExpression | `!controller.signal.aborted` | `true` | EQ |
| 1331 | src/task-blocks.tsx:746 | Survived | ConditionalExpression | `loading && <p role="status">Consultando bloque en conflicto</p>` | `true` | NE |
| 1332 | src/task-blocks.tsx:746 | Survived | ConditionalExpression | `loading && <p role="status">Consultando bloque en conflicto</p>` | `false` | NE |
| 1333 | src/task-blocks.tsx:746 | Survived | LogicalOperator | `loading && <p role="status">Consultando bloque en conflicto</p>` | `loading \|\| <p role="status">Consultando bloque en conflicto</p>` | NE |
| 1336 | src/task-blocks.tsx:747 | Survived | LogicalOperator | `failed && (         <p role="alert">           No se pudo consultar el bloque en conflicto. Puedes volver a           intentarlo.         </p>       )` | `failed \|\| <p role="alert">           No se pudo consultar el bloque en conflicto. Puedes volver a           intentarlo.         </p>` | NE |
| 1342 | src/task-blocks.tsx:765 | Survived | StringLiteral | `"es"` | `""` | NE |
| 1344 | src/task-blocks.tsx:766 | Survived | StringLiteral | `"numeric"` | `""` | NE |
| 1345 | src/task-blocks.tsx:767 | Survived | StringLiteral | `"short"` | `""` | NE |
| 1346 | src/task-blocks.tsx:768 | Survived | StringLiteral | `"numeric"` | `""` | NE |
| 1347 | src/task-blocks.tsx:769 | Survived | StringLiteral | `"2-digit"` | `""` | NE |
| 1348 | src/task-blocks.tsx:770 | Survived | StringLiteral | `"2-digit"` | `""` | NE |
| 1349 | src/task-blocks.tsx:771 | Survived | StringLiteral | `"2-digit"` | `""` | NE |
| 1350 | src/task-blocks.tsx:773 | Survived | StringLiteral | `"longOffset"` | `""` | NE |
| 1368 | src/task-reader.tsx:31 | Survived | BooleanLiteral | `!projectLoading` | `projectLoading` | VP histórica |
| 1380 | src/task-reader.tsx:37 | Survived | ArithmeticOperator | `value + 1` | `value - 1` | EQ histórica |
| 1401 | src/task-reader.tsx:57 | Survived | ConditionalExpression | `!controller.signal.aborted && "project" in result` | `true` | Reabierta, NE potencial |
| 1403 | src/task-reader.tsx:57 | Survived | LogicalOperator | `!controller.signal.aborted && "project" in result` | `!controller.signal.aborted \|\| "project" in result` | Reabierta, NE potencial |
| 1412 | src/task-reader.tsx:63 | Survived | ConditionalExpression | `controller.signal.aborted` | `false` | Reabierta, NE potencial |
| 1427 | src/task-reader.tsx:72 | Survived | ConditionalExpression | `!controller.signal.aborted` | `true` | Reabierta, NE potencial |
| 1455 | src/task-reader.tsx:98 | Survived | ArithmeticOperator | `value + 1` | `value - 1` | EQ histórica |
| 1465 | src/task-reader.tsx:128 | Survived | LogicalOperator | `!projectLoading && !projectFailure` | `!projectLoading \|\| !projectFailure` | NE |
| 1468 | src/task-reader.tsx:129 | Survived | OptionalChaining | `snapshot?.project` | `snapshot.project` | EQ |
| 1490 | src/task-state.tsx:28 | Survived | BooleanLiteral | `false` | `true` | VP histórica |
| 1497 | src/task-state.tsx:31 | Survived | LogicalOperator | `interacted.current &&       !saving &&       (snapshot \|\| failure)` | `interacted.current && !saving \|\| snapshot \|\| failure` | VP histórica |
| 1498 | src/task-state.tsx:31 | Survived | ConditionalExpression | `interacted.current &&       !saving` | `true` | VP histórica |
| 1499 | src/task-state.tsx:31 | Survived | LogicalOperator | `interacted.current &&       !saving` | `interacted.current \|\| !saving` | VP histórica |
| 1501 | src/task-state.tsx:33 | Survived | ConditionalExpression | `snapshot \|\| failure` | `true` | VP histórica |
| 1505 | src/task-state.tsx:36 | Survived | OptionalChaining | `heading.current?.focus` | `heading.current.focus` | EQ histórica |
| 1511 | src/task-state.tsx:38 | Survived | ArrayDeclaration | `[]` | `["Stryker was here"]` | EQ histórica |
| 1514 | src/task-state.tsx:40 | Survived | ConditionalExpression | `!snapshot \|\| saving` | `false` | EQ histórica |
| 1515 | src/task-state.tsx:40 | Survived | LogicalOperator | `!snapshot \|\| saving` | `!snapshot && saving` | EQ histórica |
| 1530 | src/task-state.tsx:54 | Survived | ConditionalExpression | `controller.signal.aborted` | `false` | NE |
| 1532 | src/task-state.tsx:56 | Survived | OptionalChaining | `onSnapshot?.(result)` | `onSnapshot(result)` | Alcance pendiente |
| 1537 | src/task-state.tsx:58 | Survived | ArithmeticOperator | `value + 1` | `value - 1` | EQ histórica |
| 1541 | src/task-state.tsx:61 | Survived | OptionalChaining | `onSnapshot?.(undefined)` | `onSnapshot(undefined)` | Alcance pendiente |
| 1556 | src/task-state.tsx:69 | Survived | ConditionalExpression | `!controller.signal.aborted` | `true` | EQ histórica |
| 1566 | src/task-state.tsx:78 | Survived | OptionalChaining | `onSnapshot?.(result)` | `onSnapshot(result)` | Alcance pendiente |
| 1573 | src/task-state.tsx:79 | Survived | ArithmeticOperator | `value + 1` | `value - 1` | EQ histórica |
| 1577 | src/task-state.tsx:83 | Survived | OptionalChaining | `onSnapshot?.(undefined)` | `onSnapshot(undefined)` | Alcance pendiente |

## Límites y entrega

No se han editado producción ni pruebas, ni se ha ejecutado replay. RuntimeError945 queda sin clasificación como killed: el runner registra dos reinicios fallidos y errorToString en util/dist/src/errors.js22, invocado desde VitestTestRunner.run182/mutantRun129; el fallo es de ejecución del runner, no compilación. El JSON original conserva statusReason completo.

Este PASS acredita el umbral, no cierra automáticamente los huecos observables ni declara feature done. Root asignará refuerzos acotados a un autor y revisará equivalencias/variantes. Se conservan fuentes congeladas, límites protegidos, informes y denominador original.

API85: revisión independiente final disponible en `progress/review_schedule_block_frontend_mutation.md`; autor asignado por root para sus pruebas, sin interferencia con este inventario. Corrección documental: columnas Stryker son base1; expresiones originales regeneradas con ese convenio. Las correspondencias históricas conservan mutador/reemplazo/expresión coincidentes. Reader1401/1403/1412/1427 se reabren explícitamente por retry dentro del padre vivo.

Resolución root durante revisión:886 EQ por origin de acciones HTML y guardas connected/disabled intactas;887 NE por desaparición del botón Reenviar tras rechazo negocio;1016 NE por segunda intención incierta después de rechazo negocio.1532/1541/1566/1577 EQ restringida al caller real TaskReader, que siempre pasa onSnapshot; no se añade caller artificial para matar optional chaining. Reader1401/1403/1412/1427 tienen ya recorridos públicos de retry probados en refuerzos GREEN iniciales, por lo que no se reutiliza EQ histórica. Detalles/evidencia en progress/tdd_schedule_block_frontend.md; replay sigue pendiente.
