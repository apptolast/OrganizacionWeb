# Revisión independiente de mutantes UI — complete_reopen_task

## Resolución del coordinador

Revisadas las 37 identidades contra fuente/composición y los refuerzos finales. Se aceptan las doce equivalencias y ocho variantes de foco/carga. Los IDs 67 y 212 se reclasifican como **brechas de presentación**: concatenan palabras o zona con fecha. El mismo defecto de separación fue detectado en captura y corregido en producción; se exige criterio coherente y aserciones textuales de legibilidad para ambas eliminaciones. Resultado de revisión UI: 17 brechas, 12 equivalentes y 8 variantes permitidas. La tabla siguiente conserva el dictamen original del revisor y esta resolución prevalece sobre sus dos filas de espacios. El replay debe incluir las 17 brechas UI junto a las ocho API; resultados aún pendientes.

Fuente original: `frontend/reports/mutation-complete-reopen-task/mutation-original.json`, SHA-256 `b12dd721b7382f2887bb3059217576e6b3f7308317dffa5fc20150758355bc53`. Sólo TaskState/TaskHistory (37 Survived, ningún NoCoverage); API propia excluida y revisada por el coordinador. Se compararon identidad, posición, mutador y reemplazo del informe con su source embebido y la composición actual. No se ejecuta replay ni se modifican pruebas/producción en esta revisión. Ponytail full/Caveman lite.

15 huecos observables requieren refuerzo; 12 equivalencias fundadas en composición actual; 10 variantes que difieren pero no incumplen necesariamente el contrato. Las variantes no se suman como equivalencia semántica. El autor refuerza los huecos; el resultado de replay deberá registrarse aparte. No se concede aprobación global por el porcentaje original.

| ID | Archivo y posición | Mutador → reemplazo | Clasificación y fundamento |
| --- | --- | --- | --- |
| 16 | src/task-history.tsx:21:8–21:23 | ConditionalExpression → `true` | Variante permitida; no equivalente estricta. Permite foco al empezar carga, en vez de esperar página/error; contrato no fija ese instante. |
| 15 | src/task-history.tsx:20:7–21:24 | LogicalOperator → `interacted.current \|\| page \|\| failure` | Variante permitida; no equivalente estricta. Amplía momento de enfocar encabezado cuando body tiene foco; no roba otro elemento y mantiene destino visible. |
| 20 | src/task-history.tsx:24:7–24:29 | OptionalChaining → `heading.current.focus` | Equivalente en composición actual. El h2 está montado cuando corre useLayoutEffect; no existe rama sin el ref. |
| 26 | src/task-history.tsx:30:13–30:39 | ConditionalExpression → `true` | Hueco observable. Historia: una lectura antigua de StrictMode puede reemplazar la segunda respuesta en el mismo montaje. |
| 31 | src/task-history.tsx:33:13–33:38 | ConditionalExpression → `false` | Hueco observable. Historia: un rechazo tardío tras remount por historyRevision puede retirar acceso del TaskReader todavía vivo. |
| 48 | src/task-history.tsx:53:36–53:40 | BooleanLiteral → `false` | Hueco observable. Primer retry de historia pierde el destino de foco cuando desaparece su botón. |
| 51 | src/task-history.tsx:55:15–55:34 | CallExpression → `;` | Equivalente en composición actual. El retry aparece tras fallo de una carga cuyo inicio ya retiró page; vuelve a asignar undefined. |
| 53 | src/task-history.tsx:56:27–56:47 | ArrowFunction → `() => undefined` | Hueco observable. La primera revisión pasa de 0 a undefined; la segunda permanece undefined y ya no consulta. |
| 54 | src/task-history.tsx:56:38–56:47 | ArithmeticOperator → `value - 1` | Equivalente en composición actual. El contador sólo invalida el efecto: decrecer genera un valor diferente igual que crecer. |
| 67 | src/task-history.tsx:78:60–78:63 | StringLiteral → `""` | Variante permitida; no equivalente estricta. Eliminar espacio antes de UTC concatena texto pero mantiene zona, fecha y datetime. Diferencia visual real menor; no se llama equivalencia. |
| 74 | src/task-history.tsx:92:17–92:36 | CallExpression → `;` | Hueco observable. Paginación conserva filas y acción anteriores mientras la nueva página espera. |
| 78 | src/task-history.tsx:102:8–102:35 | ConditionalExpression → `true` | Hueco observable. Aparece Volver al historial reciente incluso en la primera página. |
| 80 | src/task-history.tsx:102:19–102:34 | ConditionalExpression → `true` | Variante permitida; no equivalente estricta. Permite volver a recientes durante carga de página antigua; sigue siendo navegación deliberada con cancelación. |
| 79 | src/task-history.tsx:102:8–102:35 | LogicalOperator → `cursor \|\| page \|\| failure` | Hueco observable. Aparece Volver al historial reciente sin cursor cuando existe página o error. |
| 83 | src/task-history.tsx:105:34–105:38 | BooleanLiteral → `false` | Equivalente en composición actual. Para llegar a esta acción ya se pulsó Más antiguas, que dejó interacted=true. |
| 87 | src/task-history.tsx:108:13–108:32 | CallExpression → `;` | Hueco observable. Volver a recientes conserva página anterior durante la consulta. |
| 90 | src/task-state.tsx:20:46–20:51 | BooleanLiteral → `true` | Hueco observable. Una lectura inicial muestra éxito de actualización sin haber escrito. |
| 91 | src/task-state.tsx:26:29–26:34 | BooleanLiteral → `true` | Variante permitida; no equivalente estricta. Puede enfocar encabezado en lectura inicial si body conserva foco; no implica escritura ni robo de foco elegido. |
| 98 | src/task-state.tsx:29:7–31:28 | LogicalOperator → `interacted.current && !saving \|\| snapshot \|\| failure` | Variante permitida; no equivalente estricta. Amplía los momentos de restauración sobre body; conserva la condición de no robar otro foco. |
| 99 | src/task-state.tsx:29:7–30:14 | ConditionalExpression → `true` | Variante permitida; no equivalente estricta. Puede enfocar al empezar espera; destino visible permitido sin exigir timing exacto. |
| 100 | src/task-state.tsx:29:7–30:14 | LogicalOperator → `interacted.current \|\| !saving` | Variante permitida; no equivalente estricta. Amplía momentos de foco sobre body, sin saltarse la comprobación de foco activo. |
| 102 | src/task-state.tsx:31:8–31:27 | ConditionalExpression → `true` | Variante permitida; no equivalente estricta. Permite foco durante lectura pendiente; no afecta respuesta ni acción habilitada. |
| 106 | src/task-state.tsx:34:7–34:29 | OptionalChaining → `heading.current.focus` | Equivalente en composición actual. El h2 está siempre montado durante useLayoutEffect. |
| 112 | src/task-state.tsx:36:49–36:51 | ArrayDeclaration → `["Stryker was here"]` | Equivalente en composición actual. Array de una cadena constante conserva ejecución al montar/limpieza al desmontar igual que []. |
| 115 | src/task-state.tsx:38:9–38:28 | ConditionalExpression → `false` | Equivalente en composición actual. Único caller público: botón sólo existe con snapshot y queda disabled durante saving. No se justifica invocar el handler internamente. |
| 116 | src/task-state.tsx:38:9–38:28 | LogicalOperator → `!snapshot && saving` | Equivalente en composición actual. Misma frontera del botón que115; la guarda no cambia una acción alcanzable del usuario. |
| 118 | src/task-state.tsx:39:26–39:30 | BooleanLiteral → `false` | Hueco observable. La primera transición pierde la restauración de foco al desaparecer/deshabilitar el botón. |
| 131 | src/task-state.tsx:52:11–52:36 | ConditionalExpression → `false` | Equivalente en composición actual. El controlador de escritura sólo se aborta al desmontar TaskState. La rama de éxito contiene exclusivamente setters de ese montaje retirado. |
| 136 | src/task-state.tsx:55:26–55:46 | ArrowFunction → `() => undefined` | Hueco observable. Sólo el primer PUT cambia key; el segundo no refresca la historia. |
| 137 | src/task-state.tsx:55:37–55:46 | ArithmeticOperator → `value - 1` | Equivalente en composición actual. historyRevision sólo es key; valores negativos distintos conservan cada remount. |
| 140 | src/task-state.tsx:57:11–57:36 | ConditionalExpression → `false` | Hueco observable. El rechazo tardío de un PUT puede llamar onAccessFailure cuando TaskReader sigue vivo tras retirar/remontar TaskState. |
| 155 | src/task-state.tsx:65:11–65:37 | ConditionalExpression → `true` | Equivalente en composición actual. El setter de saving tardío se dirige al TaskState desmontado; no tiene callback global. |
| 170 | src/task-state.tsx:74:46–74:66 | ArrowFunction → `() => undefined` | Hueco observable. Sólo la primera consulta deliberada cambia key; la siguiente conserva historia anterior. |
| 171 | src/task-state.tsx:74:57–74:66 | ArithmeticOperator → `value - 1` | Equivalente en composición actual. historyRevision sólo es key; decrecer mantiene cada remount. |
| 174 | src/task-state.tsx:77:13–77:38 | ConditionalExpression → `false` | Hueco observable. Un GET viejo rechazado en StrictMode puede propagar pérdida de acceso pese al GET vigente. |
| 193 | src/task-state.tsx:103:17–103:40 | CallExpression → `;` | Hueco observable. Durante consulta deliberada quedan snapshot y botón anteriores disponibles, en vez del estado de carga. |
| 212 | src/task-state.tsx:119:31–119:34 | StringLiteral → `""` | Variante permitida; no equivalente estricta. Eliminar espacio después de Finalizada el concatena texto; fecha semántica permanece. Diferencia visual menor, no equivalencia. |

Las guardas de abort no se justifican con setters ignorados tras unmount: 31/140/174 sí pueden invocar el callback de pérdida de acceso en un TaskReader vivo. 26 puede alterar una lectura vigente en el doble efecto de StrictMode. En cambio 131/155 no ejecutan ese callback. Para focos se comprueba destino visible y respeto al foco elegido; no se fuerza una prueba espejo del instante exacto. La forma del contador se justifica por su uso como revisión/key, no por el score.

Corrección visual de producción separada: TaskState añadió espacio explícito antes de UTC después del informe original. Esa línea no es el mutante212 (espacio tras Finalizada el), ni el67 (TaskHistory). No se cruzan identidades.
