# Mutación frontend17: fuentes nuevas

## Dictamen acotado

El gate global superó80%, pero hay huecos diferenciables en los nuevos oráculos. No se ha demostrado un bug de producción por la mera supervivencia. Fuente: `progress/end_time_stryker_original.json`, SHA256 `8E0BF31D8D2CC753700E8946E11516F929ED20A35C7D38D836EC9D8E8FE9EB4A`; cierre root90337 EXIT0 e73c7b,25min42 y129 entradas iguales según root25fae9. No se leyó un resultado parcial ni se ejecutaron pruebas/replay.

Global:1980 generados,1684Killed,289Survived,3NoCoverage,4RuntimeError. Score de Stryker1684/1976=85,22%; fracción estricta1684/1980≈85,05%. Ningún error se cuenta comoKilled. Esta revisión sólo cubre los tres archivos nuevos; los cuatro compartidos los revisa otro autor.

| Archivo | Killed | Survived | Otros | Total |
| --- | ---: | ---: | ---: | ---: |
| use-work-session-decision.ts |21|8|0|29|
| work-session-end-api.ts |40|2|0|42|
| work-session-end.tsx |294|69|0|363|
| Alcance revisado |355|79|0|434|

Recuentos extraídos3cd948/9731fb. Los79residuos siguientes conservan estado **Survived**; etiquetas R son razonamientos contextuales, no reclasificación del reporte.

## Refuerzos prioritarios propuestos, sin ejecutar

1. **Recuperación217/225/226/228 y guardia155:** POST incierto, después K503/problema desconocido; no debe aparecer Reenviar, y Enter en el formulario no debe transmitir. Hay oráculos positivos K404/CSRF, pero no aíslan el negativo K. Puede dividirse en un caso de clasificación y un caso de submit si root autoriza; no matriz de códigos.
2. **Coordinador14:** dos confirmaciones sucesivas en la misma instancia y promesa S/E diferida en la segunda. La primera asignaciónundefined todavía cambia generación0; la segunda dejaundefined y no refresca. Afirmar revisión consultada y ausencia de decisión basada en snapshot previo.
3. **Timer357:** fortalecer el caso25días existente con el retardo solicitado a setTimeout≤2147483647, o emular su límite real. La prueba actual sólo observa una espera pendiente y ausencia de GET al límite, compatible con un timer enorme admitido por el fakeclock. No invocar una espera real de25días.
4. **API52/62:** partir de sesión1600 válida ya aceptada; cambiar sólo serverNow a texto inválido y, en otro caso individual, effectiveEndAt inválido. Con épocas positivas null<fecha posterior actúa como rechazo accidental; con época negativa puede aceptarse. Es un hueco del oráculo ante esas mutaciones, no una relajación del decoder actual.
5. **Privacidad183/198/259/281:** entregar JSON o clasificación de problema después de retirar contexto, no sólo HTTP401. Exigir que no haya callback/settle/refetch ni restauración en el padre vivo. React puede ocultar setters del hijo desmontado y dar falsoGREEN; contar solicitudes nuevas como en ciclo78.
6. **Timers318/336/338/361:** hidden no consulta; closed antes de su fin futuro no rearma; ampliar antes del fin retira timer antiguo y no consulta al vencer ese plazo viejo. Reforzar sólo las firmas autorizadas, no una campaña adicional por cada variante.

## Familias y justificación de todos los residuos

- **P1 privacidad diferida (183, 196, 198, 234, 259, 279, 281, 303):** Guardas tras await/catch/finally: distinguir JSON/problema diferido en la misma instancia además del HTTP antiguo; React puede ocultar setters tras desmontaje. No declaradas equivalentes.
- **P1 recuperación (155, 217, 225, 226, 228):** POST incierto→K503 o problema desconocido: conservar incertidumbre, sin Reenviar; Enter/submit no transmite POST. 228 requiere clasificación ausente durante K para evitar desreferencia.
- **P1 generación (14):** Segundo settle consecutivo: undefined sólo cambia la primera generación; verificar segunda consulta S/E y revisión nueva antes de decidir.
- **P1 precisión negativa (52, 62):** Retirar guardia null puede quedar oculto por comparación con época positiva. Usar sesión válida1600 y serverNow/effectiveEndAt no temporal por separado; no permitir aceptación por coerción null→0.
- **P1 límite timer (357):** El falso omite el límite2^31−1. El test25días comprueba ausencia de GET, pero el reloj simulado admite retardos grandes; exigir retardo máximo programado o reloj que emule overflow de navegador.
- **P2 tiempo/visibilidad (318, 336, 338, 361):** 318: hidden no debe consultar.336/338: closed con fin todavía futuro no rearma timer.361: tras ampliación, vencer antiguo timer no provoca GET anticipada. Casos independientes, sólo si autorizados.
- **P2 feedback/controles (79, 104, 106, 112, 168, 175, 190, 245, 252, 363, 386, 391, 396, 397, 398, 421, 446, 462):** Observables: nivel3 de heading en tarea, formulario inicialmente cerrado y retirado al confirmar, anuncio inicial/K, error retirado al corregir, aria-disabled/readOnly correctos en ambas direcciones.112: dos refresh síncronos antes del efecto; distinguir estado anunciado de guardia real.
- **P2 foco (91, 240):** 91 elimina condición body y podría robar foco en transición de render después de conservar iniciador;240 elimina exclusión de body. Necesitan secuencia pública que alcance restoreFocus, no forzar ref privada.
- **P2 callback (139):** onAccessFailure sustituido con misma identidad de sesión: un404 actual debe llamar al callback vigente; vaciar dependencias retiene callback previo.
- **R generación alternativa (15, 117, 193):** Decrementar sigue cambiando la generación, que sólo se compara por igualdad. Redundancia contextual plausible; no se elimina del raw ni se presenta como mejora funcional necesaria.
- **R doble invalidación (192):** POST exitoso ya llama decision.settle(), que cambia generation y refresca E. La mutación individual de setRefresh queda cubierta por esa segunda causa; analizar conjuntamente no autoriza mutaciones múltiples ni excluir.
- **R dependencias estables (16, 28, 118, 124, 327):** Dependencias constantes/funciones estables: cambiar [] por literal constante o quitar release/refreshEnd estables no cambia reruns en este montaje. Explicación contextual, rawS conservado.
- **R inicialización sobreescrita (107, 108, 126, 128, 177):** Estado oculto o sobreescrito antes de uso: lookupBusy se pone true al montar; checking/mayResend al enviar; lookupFailed oculto durante loading y actualizado al resolver. Revisar orden público si aparece evidencia distinta.
- **R limpieza y recursos (3, 4, 11, 25, 255, 256, 300, 301, 311, 325, 326):** Set/listeners: eliminar limpieza retiene recursos o controladores terminados; abort es idempotente y el callback de visibilidad usa refresh estable. No equivalencia universal: valorar retención y efectos observables, sin tests de campos privados para score.
- **R retirada por padre (131, 133, 135, 137):** Retirada local adicional a callback que desmonta el panel en ambas superficies. Una prueba aislada puede distinguirla, pero priorizar filtración observable real; 137 puede dejar controles de recuperación sin intención si el consumidor no desmonta.
- **R referencia montada (93):** Heading está montado cuando la restauración de foco se ejecuta. Optional chaining defensivo; no se fabrica refnull privada.
- **R input numérico (105):** Texto no numérico inicial en inputnumber se sanitiza visualmente a vacío; Number de ambos estados es inválido para1..1440. Distinguir sólo si existe conducta pública diferente, no contar sanitización como validación del modelo.
- **R presentación (412, 413, 417, 423):** Espacios de texto entre etiqueta y time. Comprobar legibilidad/accesibilidad real si procede; no priorizar snapshots cosméticos.
- **N navegador (445):** preventDefault: jsdom no navega como formulario nativo; comprobar permanencia URL/borrador y un único POST en recorrido real, no afirmar cobertura por fireEvent solamente.
- **R igualdad de límite (358):** Cambiar > por >= compara millis con el mismo valor que ambas ramas devuelven al ser iguales: transformación local equivalente; se conserva Survived en el raw.

## Inventario trazable

Archivo abreviado: H=`src/use-work-session-decision.ts`, A=`src/work-session-end-api.ts`, P=`src/work-session-end.tsx`. Posiciones y mutadores proceden del JSON original, columnas1-based; no usar IDs como identidad estable en futuras campañas. Cada fila incluye contexto y reemplazo; el JSON preservado permite recuperar el rango completo.

|Archivo|ID|Línea:columna|Mutador|Contexto original|Reemplazo|Familia|
|---|---:|---|---|---|---|---|
|H|3|11:18–13:6|BlockStatement|`return () => {`|`{}`|R limpieza y recursos|
|H|4|12:7–12:40|CallExpression|`reads.current.delete(controller);`|`;`|R limpieza y recursos|
|H|11|21:5–21:27|CallExpression|`reads.current.clear();`|`;`|R limpieza y recursos|
|H|14|23:19–23:39|ArrowFunction|`setGeneration((value) => value + 1);`|`() => undefined`|P1 generación|
|H|15|23:30–23:39|ArithmeticOperator|`setGeneration((value) => value + 1);`|`value - 1`|R generación alternativa|
|H|16|24:6–24:15|ArrayDeclaration|`}, [release]);`|`[]`|R dependencias estables|
|H|25|28:5–28:27|CallExpression|`reads.current.clear();`|`;`|R limpieza y recursos|
|H|28|32:6–32:8|ArrayDeclaration|`}, []);`|`["Stryker was here"]`|R dependencias estables|
|A|52|32:7–32:19|ConditionalExpression|`if (now === null \|\| now < microseconds(value.state.changedAt)!)`|`false`|P1 precisión negativa|
|A|62|34:7–34:19|ConditionalExpression|`if (end === null \|\| end < microseconds(value.state.session.plannedEndAt)!)`|`false`|P1 precisión negativa|
|P|79|51:19–51:37|ConditionalExpression|`const Heading = headingLevel === 2 ? "h2" : "h3";`|`true`|P2 feedback/controles|
|P|91|57:33–57:73|ConditionalExpression|`if (restoreFocus.current && document.activeElement === document.body)`|`true`|P2 foco|
|P|93|58:7–58:29|OptionalChaining|`heading.current?.focus();`|`heading.current.focus`|R referencia montada|
|P|104|68:42–68:47|BooleanLiteral|`const [editing, setEditing] = useState(false);`|`true`|P2 feedback/controles|
|P|105|69:42–69:44|StringLiteral|`const [minutes, setMinutes] = useState("");`|`"Stryker was here!"`|R input numérico|
|P|106|73:42–73:46|BooleanLiteral|`const [loading, setLoading] = useState(true);`|`false`|P2 feedback/controles|
|P|107|74:52–74:57|BooleanLiteral|`const [lookupFailed, setLookupFailed] = useState(false);`|`true`|R inicialización sobreescrita|
|P|108|75:29–75:33|BooleanLiteral|`const lookupBusy = useRef(true);`|`false`|R inicialización sobreescrita|
|P|112|78:26–78:30|BooleanLiteral|`lookupBusy.current = true;`|`false`|P2 feedback/controles|
|P|117|80:27–80:36|ArithmeticOperator|`setRefresh((value) => value + 1);`|`value - 1`|R generación alternativa|
|P|118|81:6–81:8|ArrayDeclaration|`}, []);`|`["Stryker was here"]`|R dependencias estables|
|P|124|84:51–84:53|ArrayDeclaration|`useEffect(() => () => command.current?.abort(), []);`|`["Stryker was here"]`|R dependencias estables|
|P|126|86:44–86:49|BooleanLiteral|`const [checking, setChecking] = useState(false);`|`true`|R inicialización sobreescrita|
|P|128|88:46–88:51|BooleanLiteral|`const [mayResend, setMayResend] = useState(false);`|`true`|R inicialización sobreescrita|
|P|131|94:7–94:31|CallExpression|`setConfirmed(undefined);`|`;`|R retirada por padre|
|P|133|95:18–95:20|StringLiteral|`setMinutes("");`|`"Stryker was here!"`|R retirada por padre|
|P|135|96:18–96:23|BooleanLiteral|`setEditing(false);`|`true`|R retirada por padre|
|P|137|98:20–98:25|BooleanLiteral|`setUncertain(false);`|`true`|R retirada por padre|
|P|139|101:5–101:22|ArrayDeclaration|`[onAccessFailure],`|`[]`|P2 callback|
|P|155|106:41–106:74|ConditionalExpression|`if (command.current \|\| conflict \|\| (uncertain && !check && !mayResend))`|`false`|P1 recuperación|
|P|168|112:16–112:21|BooleanLiteral|`setInvalid(false);`|`true`|P2 feedback/controles|
|P|175|118:5–118:24|CallExpression|`setChecking(check);`|`;`|P2 feedback/controles|
|P|177|119:18–119:23|BooleanLiteral|`setMayResend(false);`|`true`|R inicialización sobreescrita|
|P|183|131:11–131:36|ConditionalExpression|`if (controller.signal.aborted) return;`|`false`|P1 privacidad diferida|
|P|190|137:18–137:23|BooleanLiteral|`setEditing(false);`|`true`|P2 feedback/controles|
|P|192|138:18–138:38|ArrowFunction|`setRefresh((value) => value + 1);`|`() => undefined`|R doble invalidación|
|P|193|138:29–138:38|ArithmeticOperator|`setRefresh((value) => value + 1);`|`value - 1`|R generación alternativa|
|P|196|140:11–140:36|ConditionalExpression|`if (controller.signal.aborted) return;`|`false`|P1 privacidad diferida|
|P|198|142:11–142:36|ConditionalExpression|`if (controller.signal.aborted) return;`|`false`|P1 privacidad diferida|
|P|217|156:9–157:71|ConditionalExpression|`problem?.code === "CSRF_INVALID" \|\|`|`true`|P1 recuperación|
|P|225|157:12–157:70|LogicalOperator|`(check && problem?.code === "WORK_SESSION_CHANGE_NOT_FOUND"),`|`check \|\| problem?.code === "WORK_SESSION_CHANGE_NOT_FOUND"`|P1 recuperación|
|P|226|157:21–157:70|ConditionalExpression|`(check && problem?.code === "WORK_SESSION_CHANGE_NOT_FOUND"),`|`true`|P1 recuperación|
|P|228|157:21–157:34|OptionalChaining|`(check && problem?.code === "WORK_SESSION_CHANGE_NOT_FOUND"),`|`problem.code`|P1 recuperación|
|P|234|161:11–161:37|ConditionalExpression|`if (!controller.signal.aborted) {`|`true`|P1 privacidad diferida|
|P|240|163:11–163:38|ConditionalExpression|`initiator !== document.body && initiator === document.activeElement;`|`true`|P2 foco|
|P|245|165:17–165:22|BooleanLiteral|`setBusy(false);`|`true`|P2 feedback/controles|
|P|252|175:18–175:23|BooleanLiteral|`setLoading(false);`|`true`|P2 feedback/controles|
|P|255|177:58–177:72|ObjectLiteral|`controller.signal.addEventListener("abort", aborted, { once: true });`|`{}`|R limpieza y recursos|
|P|256|177:66–177:70|BooleanLiteral|`controller.signal.addEventListener("abort", aborted, { once: true });`|`false`|R limpieza y recursos|
|P|259|180:13–180:38|ConditionalExpression|`if (controller.signal.aborted) return;`|`false`|P1 privacidad diferida|
|P|279|199:13–199:38|ConditionalExpression|`if (controller.signal.aborted) return;`|`false`|P1 privacidad diferida|
|P|281|201:13–201:38|ConditionalExpression|`if (controller.signal.aborted) return;`|`false`|P1 privacidad diferida|
|P|300|210:47–210:54|StringLiteral|`controller.signal.removeEventListener("abort", aborted);`|`""`|R limpieza y recursos|
|P|301|211:9–211:19|CallExpression|`untrack();`|`;`|R limpieza y recursos|
|P|303|212:13–212:39|ConditionalExpression|`if (!controller.signal.aborted) {`|`true`|P1 privacidad diferida|
|P|311|218:45–218:52|StringLiteral|`controller.signal.removeEventListener("abort", aborted);`|`""`|R limpieza y recursos|
|P|318|234:11–234:49|ConditionalExpression|`if (document.visibilityState === "visible") refreshEnd();`|`true`|P2 tiempo/visibilidad|
|P|325|237:12–237:75|ArrowFunction|`return () => document.removeEventListener("visibilitychange", visible);`|`() => undefined`|R limpieza y recursos|
|P|326|237:47–237:65|StringLiteral|`return () => document.removeEventListener("visibilitychange", visible);`|`""`|R limpieza y recursos|
|P|327|238:6–238:18|ArrayDeclaration|`}, [refreshEnd]);`|`[]`|R dependencias estables|
|P|336|240:37–240:71|ConditionalExpression|`if (!snapshot \|\| knownClosed \|\| snapshot.state.status === "closed") return;`|`false`|P2 tiempo/visibilidad|
|P|338|240:63–240:71|StringLiteral|`if (!snapshot \|\| knownClosed \|\| snapshot.state.status === "closed") return;`|`""`|P2 tiempo/visibilidad|
|P|357|257:16–257:36|ConditionalExpression|`Number(millis > 2147483647n ? 2147483647n : millis),`|`false`|P1 límite timer|
|P|358|257:16–257:36|EqualityOperator|`Number(millis > 2147483647n ? 2147483647n : millis),`|`millis >= 2147483647n`|R igualdad de límite|
|P|361|261:12–261:37|ArrowFunction|`return () => clearTimeout(timer);`|`() => undefined`|P2 tiempo/visibilidad|
|P|363|265:40–265:42|UnaryOperator|`<Heading ref={heading} tabIndex={-1}>`|`+1`|P2 feedback/controles|
|P|386|289:23–289:47|StringLiteral|`{checking ? "Comprobando ampliación" : "Ampliando tiempo"}`|`""`|P2 feedback/controles|
|P|391|292:9–292:26|ConditionalExpression|`{(busy \|\| uncertain) && (`|`true`|P2 feedback/controles|
|P|396|303:28–303:43|ConditionalExpression|`aria-disabled={busy \|\| blocked}`|`true`|P2 feedback/controles|
|P|397|303:28–303:43|ConditionalExpression|`aria-disabled={busy \|\| blocked}`|`false`|P2 feedback/controles|
|P|398|303:28–303:43|LogicalOperator|`aria-disabled={busy \|\| blocked}`|`busy && blocked`|P2 feedback/controles|
|P|412|325:29–325:32|StringLiteral|`Fin anterior:{" "}`|`""`|R presentación|
|P|413|332:48–332:51|StringLiteral|`Fin guardado en esta ampliación:{" "}`|`""`|R presentación|
|P|417|344:36–344:39|StringLiteral|`Fin previsto original:{" "}`|`""`|R presentación|
|P|421|350:12–350:60|ConditionalExpression|`{snapshot.effectiveEndAt !== session.plannedEndAt && (`|`true`|P2 feedback/controles|
|P|423|352:36–352:39|StringLiteral|`Fin acordado actual:{" "}`|`""`|R presentación|
|P|445|376:21–376:44|CallExpression|`event.preventDefault();`|`;`|N navegador|
|P|446|386:33–386:50|ConditionalExpression|`readOnly={busy \|\| uncertain}`|`true`|P2 feedback/controles|
|P|462|402:38–402:73|ConditionalExpression|`aria-disabled={busy \|\| blocked \|\| awaitingSnapshot}`|`true`|P2 feedback/controles|

## Reutilización y límites

Los patrones de privacidad tardía, foco, reintentos repetidos y clasificación estricta ya se documentaron en `review_close_work_mutation_frontend.md`. El replay16 `review_close_work_mutation_frontend_replay.md` mató doce firmas objetivo y tres extras, sobre otras posiciones/alcance; no mata retroactivamente ninguna firma17. Los nuevos problemas compartidos con14–16 quedan fuera de este informe.

No se proponen cambios de producción antes de un defecto reproducido. No se exige100%, no se excluyen ramas difíciles, no se suman scores original/replay. La selección final de un refuerzo debe conservar el oráculo público, registrar inicialmenteGREEN honestamente y mantener un eventual replay separado, autorizado y comparado por firma exacta.


## Refuerzos autorizados posteriores, sólo tests

Sin cambios de producción ni ejecución de replay. Cada paso se añadió antes de su focal; todos pasaron inicialmente y no se declara ningún RED de producto:

| Paso | Oráculo | Primera ejecución |
| --- | --- | --- |
|1|POST503→K503 problema desconocido, sin Reenviar; Enter y evento submit no duplicanPOST; conserva5min e intención|GREENa64fb7|
|2|Dos confirmaciones hermanas sucesivas exigen dos nuevas lecturas; segunda S diferida bloquea PAUSE hasta revisión3|GREEN1381f6|
|3|Caso25días existente añade retardo setTimeout exacto2147483647; conserva anteriores asserts|GREENea582a|
|4|Sesión1600 válida, sólo serverNow inválido|GREEN9fe1ae|
|5|Sesión1600 válida, sólo effectiveEndAt inválido|GREENb15bc8|

Foco conjunto90d674:59/59 (End33,API23,hook3). Formato077836 y ESLinte3e6d4 EXIT0; diffchecka37122. Se conserva el resultado original y no se atribuye muerte a ninguna firma antes del replay.

Freeze tests: EndAPI7F7115BEA5881DE9DCF17D35BDAB43ADD60B27E8D7C27D50ACA4A483E3548B9C; EndPanel39690C261D481F439C63A48EFDF2873BCF288AB2362BFDF5EA132BA0BE52D762; hook49BC5F5EAFC41CE3C6D17ADE7F0C6B2B4192F428F485A683CB7206FF0E6E6E26.

`end_time_new_reinforcement_targets.json` contiene las nueve firmas originales14/52/62/155/217/225/226/228/357 con mutador/reemplazo, rangos1-based originales y selectores de columnas0-based. Los rangos se solapan: un único replay conjunto puede generar extras; debe informar todos y casar las nueve firmas, sin usar sólo el conteo ni los IDs nuevos. Root coordina revisión y configuración junto al refuerzo compartido de A. Privacidad tardía y otros timers permanecen límites documentados, sin ampliar este paquete.

### Corrección de formato tras init reforzado

Root53535/c001af detectó sólo Prettier en EndAPItest. La lectura491c1e confirma que todavía tenía el hash7F7115…548B9C entregado tras077836; no se atribuye a una edición de otro autor.077836 fue `prettier --write` exitoso, no una comprobación posterior completa. El siguiente pase con Prettier3.9.6 compacta las dos cadenas `vi.fn().mockResolvedValue(...)` previamente partidas: la primera salida no había quedado estable ante el check posterior. No se cambia configuración ni versión para resolverlo.

Corrección única bee948; comando real `pnpm --dir frontend lint` EXIT0 9f0448 (ESLint completo y Prettier check .). AST sintáctico serializado sin posiciones/trivia antes1e695c y después14867d idéntico SHAa4bac95c4fd0ae14b8a1f84906a0fe97f5d18a7c4f6b0f2d2b4a704520bb8f32. No cambia lógica, valores ni oráculos; no se añaden tests ni producción.

Freeze corregido EndAPItest SHA10DDBF5871F9B263B50F75C1AC7BC5C7EE7442A95119035E433E7C603D969664; reemplaza sólo el hash anterior para el futuro replay. EndPanel/hook tests y firmas de producción permanecen intactos. Conteos de tests y resultado original de mutación no se alteran por este formato.
