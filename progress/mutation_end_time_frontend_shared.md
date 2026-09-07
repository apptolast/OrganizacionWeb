# Mutación frontend17 — archivos compartidos

Revisión read-only de Reader, StatePanel, WorkSession y StateAPI. No es el dictamen de los tres archivos nuevos ni una aprobación completa de 17. Campaña original root90337 EXIT0 e73c7b, 25m42; raw `end_time_stryker_original.json`, SHA256 `8E0BF31D8D2CC753700E8946E11516F929ED20A35C7D38D836EC9D8E8FE9EB4A` verificado 1ac5b5. Root25fae9 acredita 129 inputs idénticos. No se ejecutaron pruebas ni campañas durante esta lectura.

Original global: 1980 = 1684 Killed + 289 Survived + 3 NoCoverage + 4 RuntimeError. Stryker excluye RuntimeError del denominador: 1684/1976 = 85,222672%; estricto sobre generados 1684/1980 = 85,050505%. Ambos superan 80; ningún error es Killed. Este informe sólo examina 210 supervivientes, tres NC y cuatro errores de las cuatro fuentes asignadas.

| Archivo | K | S | NC | RuntimeError | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Reader | 303 | 54 | 1 | 1 | 359 |
| StatePanel | 241 | 37 | 1 | 1 | 280 |
| WorkSession | 299 | 45 | 1 | 0 | 345 |
| StateAPI | 486 | 74 | 0 | 2 | 562 |

## Comparación con 16 por contexto

Se comparó línea de fuente, mutador y sustitución, sin usar IDs entre campañas: 45/54 S de Reader, 30/37 de State y 55/74 de StateAPI tienen contexto coincidente en el raw original16. Es una ayuda para localizar herencia, no equivalencia ni identidad unívoca de expresiones repetidas. WorkSession no formó parte del scope16 y no se inventa ese antecedente.

Las firmas reforzadas en replay16 siguen detectadas: asociaciones aria de ambos campos en Reader315–329, aborto de OpenSessionAfterClosure407 y guardia/carga del retry433–434 son Killed en17 (7887c0). `setLoading(true)` también existe en Reader246 y sobrevive allí: pertenece a otro botón, no demuestra que el refuerzo16 se perdiera. El replay16 15/15 permanece separado; no se trasladan sus IDs ni se suman sus kills. La primera consulta comparativa tuvo un error de sintaxis PowerShell4e5cfe y se corrigió sólo la lectura, sin cambios de archivos.

## Prioridades y oráculos observables

**P1 de oráculo, no bug de producto demostrado: validación nueva EXTEND.** StateAPI187–198 conserva supervivientes al omitir igualdad de status, changedAt o runningSince. El caso `rejects an EXTEND that advances a valid running interval` cambia a la vez changedAt/runningSince/worked; otras guardas siguen rechazando y ocultan el check mutado. Propuesta mínima, si root autoriza: un recibo EXTEND con before paused y after closed coherente, manteniendo trabajo/changedAt y runningSince=null, para aislar status; y un before/after paused cuyo changedAt avance con trabajo idéntico para aislar la invariancia temporal. Para runningSince no proponer un snapshot inválido: isState ya exige la relación propia de running; estudiar alcanzabilidad antes de añadir un caso que sólo falle en otra guarda. La fuente vigente sí contiene las comparaciones.

**P1 de oráculo: forma cerrada de extension.** StateAPI230 sobrevive al retirar exact y231 al retirar typeof. Un EXTEND válido con una clave extra únicamente dentro de extension distingue el contrato cerrado sin otro error; un boolean true como additionalMinutes seguirá rechazado por Number.isInteger, por lo que no sirve para atribuir kill a typeof. La comparación previous>occurred cambiada a >= (246) elige valores iguales en igualdad; explicación local matemática, sin excluir el mutante ni extrapolar equivalencia al resto.

**P1 de privacidad a priorizar si se refuerza el montaje compartido:** StatePanel141–143, cleanup completo o controller.abort retirado, sobreviven. Reader407 sí está Killed y no cubre este cleanup. Un GET S pendiente, navegación/desmontaje de State y HTTP401 antiguo antes del observador discrimina el efecto global, aunque React descarte setters del componente viejo. Hay abortos adicionales del coordinador en la composición: el oráculo debe ejercer una frontera pública alcanzable y comprobar observer, no invocar funciones privadas ni limitarse a signal. Fuente actual aborta; no defecto actual demostrado.

**P2, herencia observable:** Reader246/248,351,437 y State160–164/187/222 tienen carga/reintento o incremento sustituido por undefined. El primer refresh puede cambiar 0 a undefined y aún ejecutar efecto; una segunda consulta manual después de otro error diferencia la ausencia de incremento. Los decrementos siguen cambiando el valor, pero se mantienen como supervivientes, no se excluyen. Foco/tabIndex y anuncios requieren oráculos DOM públicos; no ampliar ahora una matriz UX ya medida.

**P2, intención CLOSE heredada:** StateAPI329 comparación sólo progressNote sigue S, como el contexto documentado16. Un recibo con únicamente progressNote distinto y nextStep igual aislaría el contrato; no atribuir rechazo de ambas notas cambiadas a ambas guardas. No es nuevo bug17 ni autorización para reabrir toda16.

**P2, composición Task:** WorkSession341 local.settle eliminado puede quedar compensado por onConfirmed y remount tras A; no concluir equivalencia sin comprobar identidad y generación. El NC336 es la rama acquire rechazado: los botones hermanos/guardas suelen impedir esa llamada. Evaluar el flujo público antes de proponer test; no invocar directamente callbacks internos por cobertura. Límites1/1440, mensajes/aria de inicio y formato local son heredados14, diferenciables pero fuera de una ampliación automática de alcance17.

## Agrupación del resto

- Reader: bloqueo local, carga/feedback/foco, retirada de borrador/recibo y guardas tras await, actualización manual, presentación y consulta A posterior a CLOSE. La retirada de UI puede esconder diferencias en variables internas; no certifica equivalencia general. Guardas post-await con S/F y cierre conservan riesgo observable sólo si una respuesta tardía puede afectar un componente aún vigente; los nuevos tests de retiro ya detectan varios restauradores, no necesariamente cada guarda redundante.
- StatePanel: guardas de envío/acción, carga/limpieza, refresh/foco y formato de fecha. La acción pasada a acquire (61) no es el action del body (68): cambiarla puede no alterar el POST pero sí coordinación; no tratar un POST correcto como oráculo de toda la adquisición.
- WorkSession: validación e incertidumbre14, recuperación y GET A padre, notificación settle, semántica de encabezados y fechas. Los nuevos abortos padre están probados por respuestas401, pero los otros residuos no heredan sus kills.
- StateAPI: gramática/tipos de revision y decimal, estados/intervalos, unión de recibos, EXTEND, intención y clasificación de problemas. Hay guardas superpuestas: distinguir errores coherentes que atraviesen las anteriores antes de solicitar otra prueba. Regex sin ancla y tipos eliminados permanecen visibles en inventario; no se consideran seguros sólo por score.

## Tres NoCoverage y cuatro RuntimeError

Reader86 y State57 afectan `!snapshot && !retained.current`, defensa del envío cuando no hay snapshot/intención. Los controles públicos normalmente requieren uno; conserva NC, sin fabricar invocación privada. WorkSession336 afecta el rechazo de acquire del coordinador, explicado arriba. No se declaran las tres rutas universalmente inalcanzables.

Reader139 y State110 eliminan optional chaining de problem?.code: mismos contextos de errores16, ahora otras líneas. StateAPI343 tiene dos mutaciones de la guarda Response/bodyUsed. Los cuatro informes contienen `Test runner crashed`, dos reinicios fallidos y `TypeError: Cannot convert object to primitive value` en util/errorToString y VitestTestRunner.run. La desreferencia/operación sobre error no Response alterada puede producir la excepción que el runner no consigue representar; el raw no prueba kill. Fuente actual conserva guardas. Son errores de medición residuales, no cuatro bugs del producto ni equivalencias.

## Conclusión acotada

No se demostró defecto funcional actual mediante esta lectura. Sí existen huecos diferenciables, principalmente la invariancia/forma de EXTEND y el aborto público de State; root decide si reforzar esos pocos casos. No se propone campaña completa, cambio de producción ni un test por cada superviviente. Los gates globalE2E/CI permanecen separados. El anexo conserva todos los residuos asignados con identidad del snapshot: archivo, rango, mutador, sustitución y línea de contexto; ID sólo facilita localizar el raw original17.

## Inventario íntegro de residuos asignados

### src/work-session-reader.tsx

```text
476 | Survived | 37:45-37:71 | ConditionalExpression | true | const blocked = Boolean(decision.owner && decision.owner !== "CLOSE");
478 | Survived | 37:64-37:71 | StringLiteral | "" | const blocked = Boolean(decision.owner && decision.owner !== "CLOSE");
479 | Survived | 42:42-42:46 | BooleanLiteral | false | const [loading, setLoading] = useState(true);
480 | Survived | 44:29-44:34 | BooleanLiteral | true | const interacted = useRef(false);
489 | Survived | 47:28-47:33 | BooleanLiteral | true | interacted.current = false;
490 | Survived | 48:7-48:29 | OptionalChaining | heading.current.focus | heading.current?.focus();
494 | Survived | 58:44-58:49 | BooleanLiteral | true | const [checking, setChecking] = useState(false);
499 | Survived | 61:51-61:53 | ArrayDeclaration | ["Stryker was here"] | useEffect(() => () => command.current?.abort(), []);
505 | Survived | 69:5-69:26 | OptionalChaining | lookup.current.abort | lookup.current?.abort();
508 | Survived | 72:13-72:18 | BooleanLiteral | true | setBusy(false);
513 | Survived | 76:5-76:27 | CallExpression | ; | setClosure(undefined);
515 | Survived | 77:21-77:23 | StringLiteral | "Stryker was here!" | setProgressNote("");
517 | Survived | 78:17-78:19 | StringLiteral | "Stryker was here!" | setNextStep("");
522 | Survived | 82:6-82:8 | ArrayDeclaration | ["Stryker was here"] | }, []);
534 | Survived | 86:8-86:38 | ConditionalExpression | false | (!snapshot && !retained.current) ||
537 | NoCoverage | 86:21-86:38 | BooleanLiteral | retained.current | (!snapshot && !retained.current) ||
538 | Survived | 88:8-88:41 | ConditionalExpression | false | (uncertain && !check && !mayResend)
561 | Survived | 101:26-101:66 | ConditionalExpression | true | interacted.current = document.activeElement !== document.body;
565 | Survived | 102:18-102:23 | BooleanLiteral | true | setMayResend(false);
572 | Survived | 116:11-116:36 | ConditionalExpression | true | if (result.action === "CLOSE") {
586 | Survived | 125:11-125:36 | ConditionalExpression | false | if (controller.signal.aborted) return;
588 | Survived | 127:11-127:36 | ConditionalExpression | false | if (controller.signal.aborted) return;
596 | Survived | 132:9-132:32 | CallExpression | ; | setSnapshot(undefined);
612 | RuntimeError | 139:21-139:34 | OptionalChaining | problem.code | (check && problem?.code === "WORK_SESSION_CHANGE_NOT_FOUND"),
618 | Survived | 143:11-143:37 | ConditionalExpression | true | if (!controller.signal.aborted) {
648 | Survived | 173:13-173:38 | ConditionalExpression | false | if (controller.signal.aborted) return;
659 | Survived | 184:13-184:38 | ConditionalExpression | false | if (controller.signal.aborted) return;
661 | Survived | 186:13-186:38 | ConditionalExpression | false | if (controller.signal.aborted) return;
672 | Survived | 194:9-194:19 | CallExpression | ; | untrack();
674 | Survived | 195:13-195:39 | ConditionalExpression | true | if (!controller.signal.aborted) setLoading(false);
679 | Survived | 198:7-198:17 | CallExpression | ; | untrack();
682 | Survived | 214:17-214:19 | UnaryOperator | +1 | tabIndex={-1}
686 | Survived | 223:35-223:37 | UnaryOperator | +1 | <h1 ref={heading} tabIndex={-1}>
706 | Survived | 234:9-234:26 | ConditionalExpression | true | {(busy || uncertain) && (
712 | Survived | 245:36-245:76 | ConditionalExpression | true | interacted.current = document.activeElement !== document.body;
713 | Survived | 245:36-245:76 | ConditionalExpression | false | interacted.current = document.activeElement !== document.body;
714 | Survived | 245:36-245:76 | EqualityOperator | document.activeElement === document.body | interacted.current = document.activeElement !== document.body;
716 | Survived | 246:26-246:30 | BooleanLiteral | false | setLoading(true);
717 | Survived | 247:15-247:38 | CallExpression | ; | setConflict(undefined);
719 | Survived | 248:26-248:46 | ArrowFunction | () => undefined | setRefresh((value) => value + 1);
720 | Survived | 248:37-248:46 | ArithmeticOperator | value - 1 | setRefresh((value) => value + 1);
752 | Survived | 281:13-281:36 | CallExpression | ; | event.preventDefault();
753 | Survived | 291:21-291:24 | StringLiteral | "" | Inicio:{" "}
754 | Survived | 298:27-298:30 | StringLiteral | "" | Fin previsto:{" "}
785 | Survived | 349:36-349:76 | ConditionalExpression | true | interacted.current = document.activeElement !== document.body;
791 | Survived | 351:26-351:46 | ArrowFunction | () => undefined | setRefresh((value) => value + 1);
792 | Survived | 351:37-351:46 | ArithmeticOperator | value - 1 | setRefresh((value) => value + 1);
797 | Survived | 367:22-367:25 | StringLiteral | "" | Cerrada:{" "}
816 | Survived | 399:13-399:39 | ConditionalExpression | true | if (!controller.signal.aborted) setActive(value);
821 | Survived | 402:13-402:39 | ConditionalExpression | true | if (!controller.signal.aborted) setFailed(true);
827 | Survived | 405:13-405:39 | ConditionalExpression | true | if (!controller.signal.aborted) setLoading(false);
841 | Survived | 420:8-420:27 | ConditionalExpression | true | {!loading && !failed && active && (
842 | Survived | 420:8-420:27 | LogicalOperator | !loading || !failed | {!loading && !failed && active && (
853 | Survived | 436:11-436:32 | CallExpression | ; | setActive(undefined);
855 | Survived | 437:22-437:42 | ArrowFunction | () => undefined | setRefresh((value) => value + 1);
856 | Survived | 437:33-437:42 | ArithmeticOperator | value - 1 | setRefresh((value) => value + 1);
```

### src/work-session-state.tsx

```text
1440 | Survived | 40:7-40:29 | OptionalChaining | heading.current.focus | heading.current?.focus();
1441 | Survived | 44:42-44:46 | BooleanLiteral | false | const [loading, setLoading] = useState(true);
1448 | Survived | 50:51-50:53 | ArrayDeclaration | ["Stryker was here"] | useEffect(() => () => command.current?.abort(), []);
1450 | Survived | 54:46-54:51 | BooleanLiteral | true | const [mayResend, setMayResend] = useState(false);
1460 | Survived | 57:18-57:48 | ConditionalExpression | false | if (busy || (!snapshot && !retained.current)) return;
1463 | NoCoverage | 57:31-57:48 | BooleanLiteral | retained.current | if (busy || (!snapshot && !retained.current)) return;
1469 | Survived | 61:12-61:48 | ConditionalExpression | true | (snapshot!.state.status === "running" ? "PAUSE" : "RESUME"),
1474 | Survived | 61:61-61:69 | StringLiteral | "" | (snapshot!.state.status === "running" ? "PAUSE" : "RESUME"),
1483 | Survived | 73:5-73:26 | OptionalChaining | lookup.current.abort | lookup.current?.abort();
1490 | Survived | 77:18-77:23 | BooleanLiteral | true | setMayResend(false);
1493 | Survived | 82:11-82:36 | ConditionalExpression | false | if (controller.signal.aborted) return;
1498 | Survived | 87:7-87:30 | CallExpression | ; | setSnapshot(undefined);
1504 | Survived | 90:18-90:38 | ArrowFunction | () => undefined | setRefresh((value) => value + 1);
1505 | Survived | 90:29-90:38 | ArithmeticOperator | value - 1 | setRefresh((value) => value + 1);
1517 | Survived | 98:11-98:36 | ConditionalExpression | false | if (controller.signal.aborted) return;
1528 | Survived | 105:9-105:32 | CallExpression | ; | setSnapshot(undefined);
1541 | RuntimeError | 110:21-110:34 | OptionalChaining | problem.code | (check && problem?.code === "WORK_SESSION_CHANGE_NOT_FOUND"),
1547 | Survived | 114:11-114:37 | ConditionalExpression | true | if (!controller.signal.aborted) setBusy(false);
1575 | Survived | 138:9-138:19 | CallExpression | ; | untrack();
1577 | Survived | 139:13-139:39 | ConditionalExpression | true | if (!controller.signal.aborted) setLoading(false);
1581 | Survived | 141:18-144:6 | BlockStatement | {} | return () => {
1582 | Survived | 142:7-142:17 | CallExpression | ; | untrack();
1583 | Survived | 143:7-143:26 | CallExpression | ; | controller.abort();
1585 | Survived | 148:35-148:37 | UnaryOperator | +1 | <h3 ref={heading} tabIndex={-1}>
1602 | Survived | 158:15-158:30 | LogicalOperator | loading && busy | if (loading || busy) return;
1601 | Survived | 158:15-158:30 | ConditionalExpression | false | if (loading || busy) return;
1608 | Survived | 161:22-161:42 | ArrowFunction | () => undefined | setRefresh((value) => value + 1);
1609 | Survived | 161:33-161:42 | ArithmeticOperator | value - 1 | setRefresh((value) => value + 1);
1616 | Survived | 167:9-167:26 | ConditionalExpression | true | {(busy || uncertain) && (
1630 | Survived | 187:26-187:46 | ArrowFunction | () => undefined | setRefresh((value) => value + 1);
1631 | Survived | 187:37-187:46 | ArithmeticOperator | value - 1 | setRefresh((value) => value + 1);
1645 | Survived | 222:26-222:46 | ArrowFunction | () => undefined | setRefresh((value) => value + 1);
1646 | Survived | 222:37-222:46 | ArithmeticOperator | value - 1 | setRefresh((value) => value + 1);
1660 | Survived | 244:26-244:29 | StringLiteral | "" | Actualizado:{" "}
1667 | Survived | 253:30-253:54 | ConditionalExpression | true | aria-disabled={busy || awaitingSnapshot}
1700 | Survived | 292:41-292:48 | StringLiteral | "" | formatter = new Intl.DateTimeFormat("es-ES", {
1702 | Survived | 294:18-294:24 | StringLiteral | "" | dateStyle: "long",
1703 | Survived | 295:18-295:24 | StringLiteral | "" | timeStyle: "long",
1712 | Survived | 307:77-307:80 | StringLiteral | "" | <time dateTime={instant}>{formatter.format(new Date(instant))}</time>{" "}
```

### src/work-session.tsx

```text
1740 | Survived | 32:7-32:29 | OptionalChaining | heading.current.focus | heading.current?.focus();
1743 | Survived | 39:42-39:44 | StringLiteral | "Stryker was here!" | const [minutes, setMinutes] = useState("");
1745 | Survived | 41:44-41:49 | BooleanLiteral | true | const [checking, setChecking] = useState(false);
1747 | Survived | 44:46-44:51 | BooleanLiteral | true | const [mayResend, setMayResend] = useState(false);
1756 | Survived | 51:24-51:39 | ConditionalExpression | true | const absenceKnown = active === null && !loading && !lookupFailed;
1780 | Survived | 57:51-57:53 | ArrayDeclaration | ["Stryker was here"] | useEffect(() => () => command.current?.abort(), []);
1801 | Survived | 65:9-65:28 | EqualityOperator | Number(minutes) <= 1 | Number(minutes) < 1 ||
1804 | Survived | 66:9-66:31 | EqualityOperator | Number(minutes) >= 1440 | Number(minutes) > 1440)
1803 | Survived | 66:9-66:31 | ConditionalExpression | false | Number(minutes) > 1440)
1820 | Survived | 86:11-86:36 | ConditionalExpression | false | if (controller.signal.aborted) return;
1821 | Survived | 87:7-87:28 | OptionalChaining | lookup.current.abort | lookup.current?.abort();
1823 | Survived | 88:18-88:23 | BooleanLiteral | true | setLoading(false);
1825 | Survived | 89:23-89:28 | BooleanLiteral | true | setLookupFailed(false);
1882 | Survived | 116:22-116:27 | BooleanLiteral | true | setMayResend(false);
1891 | Survived | 121:11-122:22 | OptionalChaining | problem.errors.find(error => error.field === "plannedMinutes").message | problem.errors.find((error) => error.field === "plannedMinutes")
1893 | Survived | 121:42-121:74 | ConditionalExpression | true | problem.errors.find((error) => error.field === "plannedMinutes")
1900 | Survived | 126:22-126:27 | BooleanLiteral | true | setMayResend(false);
1920 | Survived | 135:11-135:71 | LogicalOperator | !controller.signal.aborted || command.current === controller | if (!controller.signal.aborted && command.current === controller) {
1918 | Survived | 135:11-135:71 | ConditionalExpression | true | if (!controller.signal.aborted && command.current === controller) {
1922 | Survived | 135:41-135:71 | ConditionalExpression | true | if (!controller.signal.aborted && command.current === controller) {
1953 | Survived | 162:24-162:39 | StringLiteral | `` | aria-labelledby={`${id}-heading`}
1954 | Survived | 164:15-164:30 | StringLiteral | `` | <h2 id={`${id}-heading`} ref={heading} tabIndex={-1}>
1976 | Survived | 186:22-186:42 | ArrowFunction | () => undefined | setRefresh((value) => value + 1);
1977 | Survived | 186:33-186:42 | ArithmeticOperator | value - 1 | setRefresh((value) => value + 1);
1993 | Survived | 208:15-208:36 | OptionalChaining | lookup.current.abort | lookup.current?.abort();
1995 | Survived | 209:26-209:31 | BooleanLiteral | true | setLoading(false);
1997 | Survived | 212:15-212:36 | OptionalChaining | lookup.current.abort | lookup.current?.abort();
1999 | Survived | 213:26-213:30 | BooleanLiteral | false | setLoading(true);
2001 | Survived | 214:31-214:36 | BooleanLiteral | true | setLookupFailed(false);
2003 | Survived | 215:26-215:46 | ArrowFunction | () => undefined | setRefresh((value) => value + 1);
2004 | Survived | 215:37-215:46 | ArithmeticOperator | value - 1 | setRefresh((value) => value + 1);
2027 | Survived | 226:12-226:35 | LogicalOperator | !eligible || !uncertain | {!eligible && !uncertain && (
2028 | Survived | 226:12-226:21 | BooleanLiteral | eligible | {!eligible && !uncertain && (
2024 | Survived | 226:12-231:12 | ConditionalExpression | false | {!eligible && !uncertain && (
2025 | Survived | 226:12-231:12 | LogicalOperator | !eligible && !uncertain || <p>               Para iniciar trabajo necesitamos confirmar una tarea pendiente y               un proyecto no completado.             </p> | {!eligible && !uncertain && (
2026 | Survived | 226:12-226:35 | ConditionalExpression | true | {!eligible && !uncertain && (
2029 | Survived | 226:25-226:35 | BooleanLiteral | uncertain | {!eligible && !uncertain && (
2061 | Survived | 294:17-294:34 | ConditionalExpression | true | {(busy || uncertain) && (
2075 | NoCoverage | 336:42-336:47 | BooleanLiteral | true | if (!local.acquire(action)) return false;
2079 | Survived | 341:7-341:22 | CallExpression | ; | local.settle();
2082 | Survived | 362:18-362:23 | BooleanLiteral | true | let fallback = false;
2084 | Survived | 365:41-365:48 | StringLiteral | "" | formatter = new Intl.DateTimeFormat("es-ES", {
2086 | Survived | 367:18-367:24 | StringLiteral | "" | dateStyle: "long",
2087 | Survived | 368:18-368:24 | StringLiteral | "" | timeStyle: "long",
2095 | Survived | 381:17-381:20 | StringLiteral | "" | Inicio:{" "}
2096 | Survived | 388:23-388:26 | StringLiteral | "" | Fin previsto:{" "}
```

### src/work-session-state-api.ts

```text
879 | Survived | 43:5-44:36 | ConditionalExpression | false | now === null ||
881 | Survived | 43:5-43:17 | ConditionalExpression | false | now === null ||
880 | Survived | 43:5-44:36 | LogicalOperator | now === null && !decimal(value.netMicroseconds) | now === null ||
893 | Survived | 47:28-47:39 | EqualityOperator | now <= since | (since === null || now < since ? 0n : now - since)
921 | Survived | 64:6-66:32 | ConditionalExpression | true | (value.status === "running" ||
934 | Survived | 68:5-68:39 | ConditionalExpression | true | typeof value.revision === "string" &&
937 | Survived | 69:5-69:20 | Regex | /[1-9][0-9]*$/ | /^[1-9][0-9]*$/.test(value.revision) &&
938 | Survived | 69:5-69:20 | Regex | /^[1-9][0-9]*/ | /^[1-9][0-9]*$/.test(value.revision) &&
943 | Survived | 70:5-70:51 | EqualityOperator | BigInt(value.revision) < 9223372036854775807n | BigInt(value.revision) <= 9223372036854775807n
957 | Survived | 76:5-76:21 | ConditionalExpression | true | changed !== null &&
956 | Survived | 76:5-77:21 | LogicalOperator | changed !== null || started !== null | changed !== null &&
955 | Survived | 76:5-77:21 | ConditionalExpression | true | changed !== null &&
951 | Survived | 76:5-79:38 | ConditionalExpression | true | changed !== null &&
953 | Survived | 76:5-78:23 | ConditionalExpression | true | changed !== null &&
952 | Survived | 76:5-79:38 | LogicalOperator | changed !== null && started !== null && changed >= started || decimal(value.workedMicroseconds) | changed !== null &&
954 | Survived | 76:5-78:23 | LogicalOperator | changed !== null && started !== null || changed >= started | changed !== null &&
959 | Survived | 77:5-77:21 | ConditionalExpression | true | started !== null &&
961 | Survived | 78:5-78:23 | ConditionalExpression | true | changed >= started &&
982 | Survived | 88:10-88:35 | ConditionalExpression | true | return typeof value === "string" && /^(0|[1-9][0-9]*)$/.test(value);
979 | Survived | 88:10-88:70 | ConditionalExpression | true | return typeof value === "string" && /^(0|[1-9][0-9]*)$/.test(value);
985 | Survived | 88:39-88:58 | Regex | /(0|[1-9][0-9]*)$/ | return typeof value === "string" && /^(0|[1-9][0-9]*)$/.test(value);
986 | Survived | 88:39-88:58 | Regex | /^(0|[1-9][0-9]*)/ | return typeof value === "string" && /^(0|[1-9][0-9]*)$/.test(value);
1002 | Survived | 143:13-143:39 | ConditionalExpression | true | : intent.action === "EXTEND"
1082 | Survived | 170:7-171:31 | LogicalOperator | exact(value, "id sessionId action occurredAt before after closure") || value.action === "CLOSE" | ((exact(value, "id sessionId action occurredAt before after closure") &&
1081 | Survived | 170:7-171:31 | ConditionalExpression | true | ((exact(value, "id sessionId action occurredAt before after closure") &&
1080 | Survived | 170:7-172:73 | LogicalOperator | exact(value, "id sessionId action occurredAt before after closure") && value.action === "CLOSE" || exact(value.closure, "progressNote nextStep workDate closeZoneId") | ((exact(value, "id sessionId action occurredAt before after closure") &&
1084 | Survived | 171:7-171:31 | ConditionalExpression | true | value.action === "CLOSE" &&
1091 | Survived | 176:7-176:49 | ConditionalExpression | true | typeof value.closure.workDate === "string" &&
1095 | Survived | 177:7-177:28 | Regex | /^\d{4}-\d{2}-\d{2}/ | /^\d{4}-\d{2}-\d{2}$/.test(value.closure.workDate) &&
1094 | Survived | 177:7-177:28 | Regex | /\d{4}-\d{2}-\d{2}$/ | /^\d{4}-\d{2}-\d{2}$/.test(value.closure.workDate) &&
1106 | Survived | 179:8-180:34 | LogicalOperator | exact(value, "id sessionId action occurredAt before after extension") || value.action === "EXTEND" | (exact(value, "id sessionId action occurredAt before after extension") &&
1108 | Survived | 180:9-180:34 | ConditionalExpression | true | value.action === "EXTEND") ||
1137 | Survived | 187:9-188:51 | ConditionalExpression | true | ? value.before.status !== "closed" &&
1138 | Survived | 187:9-188:51 | LogicalOperator | value.before.status !== "closed" || value.after.status === value.before.status | ? value.before.status !== "closed" &&
1139 | Survived | 187:9-187:41 | ConditionalExpression | true | ? value.before.status !== "closed" &&
1141 | Survived | 187:33-187:41 | StringLiteral | "" | ? value.before.status !== "closed" &&
1142 | Survived | 188:9-188:51 | ConditionalExpression | true | value.after.status === value.before.status &&
1147 | Survived | 196:9-196:57 | ConditionalExpression | true | value.after.changedAt === value.before.changedAt &&
1149 | Survived | 197:9-197:63 | ConditionalExpression | true | value.after.runningSince === value.before.runningSince &&
1151 | Survived | 198:9-198:75 | ConditionalExpression | true | value.after.workedMicroseconds === value.before.workedMicroseconds
1157 | Survived | 200:11-200:79 | ConditionalExpression | true | ? value.before.status === "running" && value.after.status === "paused"
1159 | Survived | 200:11-200:79 | LogicalOperator | value.before.status === "running" || value.after.status === "paused" | ? value.before.status === "running" && value.after.status === "paused"
1160 | Survived | 200:11-200:44 | ConditionalExpression | true | ? value.before.status === "running" && value.after.status === "paused"
1163 | Survived | 200:48-200:79 | ConditionalExpression | true | ? value.before.status === "running" && value.after.status === "paused"
1170 | Survived | 202:13-204:44 | ConditionalExpression | true | ? (value.before.status === "running" ||
1172 | Survived | 202:13-204:44 | LogicalOperator | value.before.status === "running" || value.before.status === "paused" || value.after.status === "closed" | ? (value.before.status === "running" ||
1173 | Survived | 202:14-203:47 | ConditionalExpression | true | ? (value.before.status === "running" ||
1181 | Survived | 204:13-204:44 | ConditionalExpression | true | value.after.status === "closed"
1184 | Survived | 205:13-207:45 | ConditionalExpression | true | : value.action === "RESUME" &&
1186 | Survived | 205:13-207:45 | LogicalOperator | value.action === "RESUME" && value.before.status === "paused" || value.after.status === "running" | : value.action === "RESUME" &&
1187 | Survived | 205:13-206:45 | ConditionalExpression | true | : value.action === "RESUME" &&
1188 | Survived | 205:13-206:45 | LogicalOperator | value.action === "RESUME" || value.before.status === "paused" | : value.action === "RESUME" &&
1189 | Survived | 205:13-205:38 | ConditionalExpression | true | : value.action === "RESUME" &&
1192 | Survived | 206:13-206:45 | ConditionalExpression | true | value.before.status === "paused" &&
1195 | Survived | 207:13-207:45 | ConditionalExpression | true | value.after.status === "running") &&
1228 | Survived | 230:5-231:48 | LogicalOperator | !exact(value, "additionalMinutes previousEndAt effectiveEndAt") && typeof value.additionalMinutes !== "number" | !exact(value, "additionalMinutes previousEndAt effectiveEndAt") ||
1227 | Survived | 230:5-231:48 | ConditionalExpression | false | !exact(value, "additionalMinutes previousEndAt effectiveEndAt") ||
1231 | Survived | 231:5-231:48 | ConditionalExpression | false | typeof value.additionalMinutes !== "number" ||
1251 | Survived | 241:5-241:22 | ConditionalExpression | true | previous !== null &&
1256 | Survived | 243:5-243:22 | ConditionalExpression | true | occurred !== null &&
1258 | Survived | 244:5-244:17 | ConditionalExpression | true | end !== null &&
1265 | Survived | 246:8-246:27 | EqualityOperator | previous >= occurred | (previous > occurred ? previous : occurred) +
1348 | Survived | 313:5-313:29 | ConditionalExpression | false | value.action !== "CLOSE" ||
1362 | Survived | 323:7-323:32 | ConditionalExpression | true | value.action === "EXTEND" &&
1376 | Survived | 328:6-329:57 | LogicalOperator | value.action === "CLOSE" || value.closure.progressNote === intent.progressNote | (value.action === "CLOSE" &&
1375 | Survived | 328:6-329:57 | ConditionalExpression | true | (value.action === "CLOSE" &&
1377 | Survived | 328:6-328:30 | ConditionalExpression | true | (value.action === "CLOSE" &&
1380 | Survived | 329:7-329:57 | ConditionalExpression | true | value.closure.progressNote === intent.progressNote &&
1388 | RuntimeError | 343:7-343:53 | ConditionalExpression | false | if (!(error instanceof Response) || error.bodyUsed) return null;
1389 | RuntimeError | 343:7-343:53 | LogicalOperator | !(error instanceof Response) && error.bodyUsed | if (!(error instanceof Response) || error.bodyUsed) return null;
1391 | Survived | 347:12-347:22 | ArrowFunction | () => undefined | .catch(() => null);
1409 | Survived | 350:5-350:35 | ConditionalExpression | false | typeof value.code !== "string" ||
1413 | Survived | 352:5-352:36 | ConditionalExpression | false | typeof value.title !== "string" ||
1417 | Survived | 353:6-353:24 | MethodExpression | value.title | !value.title.trim() ||
1418 | Survived | 354:5-354:74 | ConditionalExpression | false | value.type !== "urn:organization:problem:" + value.code.toLowerCase() ||
1424 | Survived | 356:5-356:73 | ConditionalExpression | false | stateErrors[value.code as keyof typeof stateErrors] !== error.status
```
