# Mutación frontend — feature 26 `ics_calendar` (Stryker, campaña de cierre sobre `main`)

**Veredicto: PASS** — Score: **345/390 = 88.46 %**, umbral **80 %**. Margen: 8.46 puntos, es decir
**33 mutantes** (312/390 = 80.00 % sería el mínimo exacto; 313/390 = 80.26 % el primer valor
estrictamente por encima).

Denominador íntegro: **390 mutantes generados, 390 contabilizados**. No he reclasificado ni excluido
ninguno. Los 45 supervivientes —incluidos los 23 que declaro equivalentes, el inmatable por esta
suite y los 3 del punto ciego de `Intl`— siguen contando, y el 88.46 % los cuenta a todos como
vivos.

Esta campaña cumple la condición que el juez dejó abierta en
`progress/judge_ics_calendar_tercera.md`: el score se ha vuelto a medir **sobre el árbol actual de
`main`**, después del merge `57608a8` y del cambio del campo de la url.

## Ejecución

| | |
| --- | --- |
| Comando | `node scripts/project.mjs mutate ics_calendar-frontend` |
| Despacho real | `pnpm --dir frontend exec stryker run stryker.ics-calendar.config.json` |
| Checkout | **`d418a5d` en `main`**, repositorio principal, árbol limpio (`git status --short` vacío) |
| Ventana | 2026-09-09T15:17:02Z → 2026-09-09T15:27:26Z (10 min 21 s de Stryker) |
| Código de salida | **0** (`Final mutation score of 88.46 is greater than or equal to break threshold 80`) |
| Ficheros mutados | 4 de 227; **390 mutantes** instrumentados; 24.33 pruebas por mutante de media |
| Dry run | **1023 pruebas** del frontend en verde, 3 min 7 s |
| Informes | `frontend/reports/mutation-ics-calendar/mutation.json` y `mutation.html` |

### Comprobación previa del alcance, antes de correr

El juez avisaba de que los rangos `línea:columna` de `stryker.ics-calendar.config.json` se
recalcularon contra el `App.tsx` fusionado. Lo verifiqué por dos vías antes de lanzar nada:

1. `node --test scripts/project.test.mjs` → **91/91 en verde**. La prueba «ics calendar Stryker
   selects its own nodes of the shared files» no compara números: recorta el fichero real por el
   rango y exige que el trozo empiece por `calendar = route` / `calendar` / `calendar && username` /
   `<RouteLink` y contenga `/calendario` / `Calendario` / `<Calendar owner={username} />` /
   `/calendario`. Es una guarda por contenido, no por posición.
2. A mano, leyendo el árbol actual: `App.tsx:40` es
   `const calendar = route === "/calendario";`; `App.tsx:65-66` es la rama `: calendar ? "Calendario"`
   del título; `App.tsx:106-107` es `) : calendar && username ? ( <Calendar owner={username} />`;
   `workspace.tsx:99-104` es el `<RouteLink href="/calendario">`. Los cuatro rangos apuntan a lo que
   deben.

El alcance íntegro (`src/calendar-feed-api.ts` y `src/calendar.tsx` enteros) no depende de rangos y
no puede desalinearse.

## Tabla de estados (denominador íntegro)

| Estado | Total | calendar-feed-api.ts | calendar.tsx | App.tsx | workspace.tsx |
| --- | ---: | ---: | ---: | ---: | ---: |
| Killed | 345 | 114 | 218 | 8 | 5 |
| Timeout | **0** | 0 | 0 | 0 | 0 |
| Survived | 45 | 10 | 35 | 0 | 0 |
| NoCoverage | **0** | 0 | 0 | 0 | 0 |
| RuntimeError | 0 | 0 | 0 | 0 | 0 |
| CompileError / NonViable | 0 | 0 | 0 | 0 | 0 |
| Ignored | 0 | 0 | 0 | 0 | 0 |
| **Total** | **390** | **124** | **253** | **8** | **5** |

Score por fichero: `App.tsx` **100 %**, `workspace.tsx` **100 %**, `calendar-feed-api.ts`
**91.94 %**, `calendar.tsx` **86.17 %**.

`NoCoverage` sigue en 0: todo el código del alcance se ejecuta al menos una vez, incluida la función
`selectAll` nueva.

## Por qué el denominador sube de 384 a 390, mutante a mutante

Comparé el `mutation.json` de la campaña anterior (conservado; 384 mutantes, checkout `8527823` de
`claude/ics-calendar`) contra el de ahora, por fichero y posición. `src/calendar-feed-api.ts`,
`src/App.tsx` y `src/workspace.tsx` **no cambian de censo**: 124, 8 y 5 mutantes, los mismos.
Toda la diferencia está en `src/calendar.tsx`, 247 → 253, y se explica exactamente:

**+7 mutantes nuevos**, todos en la función `selectAll` que introdujo el arreglo del campo de la url
(`calendar.tsx:28-36`):

| Posición | Mutador | Sustitución | Estado |
| --- | --- | --- | --- |
| `29:40` | BlockStatement | `{}` | Killed |
| `31:7` | ConditionalExpression | `true` | Killed |
| `31:7` | BooleanLiteral | `selection` | Killed |
| `31:7` | ConditionalExpression | `false` | **Survived** |
| `33:3` | CallExpression | `;` | Killed |
| `34:3` | CallExpression | `;` | **Survived** |
| `35:3` | CallExpression | `;` | **Survived** |

**−1 mutante**, el que la campaña anterior contaba como superviviente número 25: `249:25`
`BooleanLiteral` `spellCheck={false} -> true`. Desapareció porque **el atributo desapareció con el
`<textarea>`**. No es un mutante «arreglado»: es código que ya no existe.

7 − 1 = **+6**. 384 + 6 = **390**. El denominador sube, que es la dirección segura, y sube por
fuente nueva, no por pruebas nuevas. **Nada se ha restado por vía de exclusión.**

Un dato que conviene registrar porque es contraintuitivo: **el `<div role="textbox">` nuevo no
aporta ni un solo mutante por sus atributos**. Stryker no muta los literales de cadena de los
atributos JSX (`role="textbox"`, `aria-readonly="true"`, `aria-labelledby=…`, `id`, `data-…`) ni el
`tabIndex={0}` (no hay mutador de literales numéricos). Lo único mutable que trajo el cambio es el
cuerpo de `selectAll` y el `onFocus` (`268:22 ArrowFunction`, **Killed**). Es decir: **la
accesibilidad del campo nuevo —el `role`, el `aria-readonly`, el `aria-labelledby`— no está sujeta
por ningún mutante.** Si se rompiera, esta campaña no lo vería. Lo digo para que nadie lea el
88.46 % como si acreditara eso.

## Comparación con la campaña anterior

| | Anterior (`8527823`, rama) | Ahora (`d418a5d`, `main`) | Delta |
| --- | ---: | ---: | ---: |
| Total | 384 | **390** | +6 |
| Killed | 339 | 345 | +6 |
| Timeout (cuenta como muerte) | 1 | 0 | −1 |
| Killed + Timeout | 340 | **345** | +5 |
| Survived | 44 | **45** | +1 |
| NoCoverage | 0 | 0 | = |
| Score | 88.54 % | **88.46 %** | **−0.08 pt** |
| Pruebas de la feature | 96 | **98** | +2 |
| Dry run del frontend | 1011 | 1023 | +12 |

**El score baja ocho centésimas y aun así el trabajo mejoró.** Conviene desglosarlo porque el número
solo, leído a secas, dice lo contrario:

- **Se mató el peor superviviente de la campaña anterior.** El `187:21 ConditionalExpression -> true`
  (ahora `197:21`) —el que dejaba `retriable = failure !== "limit"` y por tanto pintaba un botón
  «Reintentar» **permanente en el camino feliz**, el único superviviente con una conducta rota que
  una persona vería— **ahora muere**, con 52 pruebas ejecutadas. Lo matan las dos pruebas nuevas:
  `@s31 @s35 @s36 no ofrece reintentar mientras nada ha fallado` y
  `@s35 reintentar aparece con el fallo y se retira cuando el paso sale bien`. Son exactamente las
  dos que pedí en el informe anterior. Cerrado.
- **El `Timeout` de `workspace.tsx` era ruido del corredor y se ha ido.** El mismo mutante
  (`101:27 ConditionalExpression -> true`, antes `92:27`) ahora corre 571 pruebas y muere limpio.
  `workspace.tsx` pasa de 4 Killed + 1 Timeout a 5 Killed. Mecánicamente el numerador no cambia
  (Stryker cuenta el Timeout como muerte), pero el resultado ya no depende de un reloj.
- **En contra, el arreglo del campo de la url abrió 3 supervivientes nuevos** y retiró 1 (el
  `spellCheck`). Neto en supervivientes: +2 por el cambio de producción, −1 por las pruebas nuevas
  = **+1**.

La aritmética exacta de la caída: 340/384 = 88.5417 %; 345/390 = 88.4615 %. Los 7 mutantes nuevos
mueren 4 y sobreviven 3 (57.1 % local), muy por debajo del 88.5 % previo, y eso arrastra el conjunto
hacia abajo más de lo que lo empuja hacia arriba la muerte del `197:21`. **La causa de la bajada es
el código nuevo mal sujetado, no una regresión de las pruebas existentes.**

## Supervivientes nuevos en la región del campo de la url: **SÍ, tres**

Es lo que había que mirar y ahí están. Los tres son huecos reales, no ruido, y los tres viven en la
función que el arreglo introdujo.

Código bajo la lupa (`frontend/src/calendar.tsx:28-36`):

```
function selectAll(field: HTMLElement) {
  const selection = window.getSelection();
  if (!selection) return;            // 31:7
  const range = document.createRange();
  range.selectNodeContents(field);   // 33:3  (muere)
  selection.removeAllRanges();       // 34:3  (SOBREVIVE)
  selection.addRange(range);         // 35:3  (SOBREVIVE)
}
```

La única prueba de unidad que toca este camino es `@s38 al enfocar el campo de url su contenido
queda seleccionado entero` (`calendar.test.tsx:884`), y afirma tres cosas: que
`Range.prototype.selectNodeContents` se llamó con el campo, que `window.getSelection()?.rangeCount`
vale 1, y que el campo contiene la url.

**N1. `calendar.tsx:35:3` `CallExpression` → `;`** (se borra `selection.addRange(range)`).
**Hueco real, y el más grave de los tres.** Es la línea que *hace* la selección; borrarla deja el
campo sin seleccionar y ninguna de las 98 pruebas lo nota. El motivo es que la única aserción que
podría verlo, la del `rangeCount`, **pasa por la razón equivocada**: `userEvent.click` ya deja un
rango en la `Selection` de jsdom antes de que el `onFocus` corra, así que `rangeCount` vale 1 con
`addRange` y sin él. El espía sobre `selectNodeContents` sujeta la línea 33, no la 35. Dicho de otro
modo: **la prueba acredita que se preparó el rango, no que se aplicara.**
*Falta:* afirmar el resultado, no el trámite —comparar `window.getSelection().toString()` con la
url—, o, si jsdom no conserva la extensión (el propio comentario de la prueba dice que no), espiar
`Selection.prototype.addRange` y exigir que se llame con el rango que devolvió `createRange`.

**N2. `calendar.tsx:34:3` `CallExpression` → `;`** (se borra `selection.removeAllRanges()`).
**Hueco real.** Con un rango previo vivo, `addRange` es un no-op según la especificación actual
(si el número de rangos no es cero, retorna), de modo que sin `removeAllRanges` la selección del
campo **no llegaría a aplicarse en un navegador de verdad** a partir del segundo foco o con una
selección previa en la página. La prueba solo enfoca una vez y sobre una selección que jsdom no
modela con fidelidad, así que no distingue. *Falta:* una prueba que enfoque el campo con una
selección previa en otro nodo (o dos veces seguidas) y exija que lo seleccionado sea la url.

**N3. `calendar.tsx:31:7` `ConditionalExpression` → `false`** (la guarda `if (!selection) return`
deja de disparar nunca). **Hueco real, menor.** Con el mutante, si `window.getSelection()` devolviera
`null` la línea siguiente reventaría con `TypeError` dentro de un manejador de foco. Es matable sin
esfuerzo y sin tocar producción. *Falta:* una prueba que fuerce `window.getSelection` a devolver
`null`, enfoque el campo y exija que no se lance nada. Nótese que las otras dos variantes del mismo
sitio (`-> true` y `BooleanLiteral -> selection`) **sí mueren**: la guarda está a medio sujetar, no
sin sujetar.

**Atenuante que hay que dejar escrito, sin que rebaje la clasificación.** El E2E
`e2e/ics-calendar-ux.spec.mjs:481-490` sí afirma la conducta de verdad, con un motor real: exige que
el texto seleccionado sea igual al texto del campo. N1 y N2 morirían con esa prueba. Pero **Stryker
no ejecuta Playwright**, así que ese E2E no puede matar mutantes y no lo he ejecutado (prohibido en
esta sesión). El riesgo residual real es menor que el que sugiere el recuento; la puntuación, no.
**No los excluyo ni los reclasifico: cuentan como vivos en el 390.**

---

# Los 45 supervivientes, uno a uno

Las líneas son las del árbol actual. El desplazamiento respecto al informe anterior es de +10 en
`calendar.tsx` desde la línea 28 (las diez que ocupa `selectAll`) y de +19 a partir del bloque JSX
del campo de la url.

## Grupo A — equivalentes con argumento (23)

Los llevo uno a uno del informe anterior, donde cada uno quedó verificado contra el código, más los
que declaré yo mismo. **Todos siguen contando en el denominador.**

### A.1 — Las cuatro guardas defensivas ya sostenidas (4)

- `calendar.tsx:77:9` ConditionalExpression → `true` (`if (confirming) confirmation.current?.focus()`).
- `calendar.tsx:95:11` ConditionalExpression → `true` (`if (objectUrl.current) URL.revokeObjectURL(…)`
  en la limpieza de desmontaje).
- `calendar.tsx:178:9` ConditionalExpression → `false` (`if (!link) return` de `copyLink`).
- `calendar.tsx:180:9` ConditionalExpression → `false` (guarda `!clipboard?.writeText`).

### A.2 — Las ocho guardas de carrera de `run` (8)

`108:9` → `false`; `115:11` Cond → `false`; `115:11` LogicalOperator; `115:40` Cond → `false`;
`118:11` Cond → `false`; `118:11` LogicalOperator; `118:40` Cond → `false`; `121:11` → `true`.

Equivalencia sostenida por dos invariantes verificadas leyendo el código, no por promesa:
(1) ningún control inicia una operación con otra en vuelo —los cuatro `disabled={Boolean(busy)}`
están sujetos por `calendar.test.tsx:916` y `:941`, y Stryker **no genera mutantes** sobre
`disabled={Boolean(busy)}`, así que esas dos pruebas son lo único que las sujeta—; y (2) en todo
`calendar.tsx` hay **una sola llamada a `abort()`** (`calendar.tsx:93`, la limpieza de desmontaje),
luego `signal.aborted` solo puede ser cierto después de desmontar. Las ocho son inalcanzables con
efecto observable.

### A.3 — Encadenamiento opcional bajo la invariante de renderizado (3)

- `calendar.tsx:65:5` OptionalChaining — `heading.current?.focus` → `heading.current.focus`.
- `calendar.tsx:77:21` OptionalChaining — `confirmation.current?.focus` → `confirmation.current.focus`.
- `calendar.tsx:87:12` OptionalChaining — `heading.current?.focus` → `heading.current.focus`.

La única diferencia observable sería un `TypeError` con la referencia a `null`, y las tres
referencias están montadas en sus tres puntos de uso (el `h1` siempre existe; el grupo de
confirmación existe exactamente cuando `confirming` es truthy, que es la guarda de la línea 77).

### A.4 — Asignaciones redundantes y código muerto (6)

- `calendar.tsx:131:7` CallExpression → `;` — `setLink(null)` de `load`. `load` solo se invoca en el
  montaje (donde `link` ya es `null`) y desde `retry()` con `failure === "status"`, que implica que
  el estado nunca llegó y por tanto que nunca hubo enlace.
- `calendar.tsx:145:7` CallExpression → `;` — `setConfirming(null)` de `generate`.
- `calendar.tsx:153:7` CallExpression → `;` — `setConfirming(null)` de `revoke`.
- `calendar.tsx:319:15` CallExpression → `;` — `setConfirming(null)` del botón de confirmar.
  Los tres anteriores forman un trío que se enmascara mutuamente: el botón de confirmar limpia
  (`319:15`) antes de despachar, y cada acción vuelve a limpiar. Ninguno muere por separado; los
  tres morirían juntos. Equivalencia por simetría, ya argumentada.
- `calendar.tsx:150:17` ObjectLiteral → `{}` — `setStatus({ active: false, createdAt: null })` de
  `revoke`. `status` se lee en exactamente tres sitios (`193`, `194`, `195`) y `{}` rinde idéntico en
  los tres: `undefined ?? null` da `null`; `!undefined` es `true` igual que `!false`; `undefined` es
  falsy igual que `false`.
- `calendar.tsx:159:7` StringLiteral → `""` — el `failureKind` `"download"` de `run`. `download`
  pasa **siempre** un `onFailure` (`calendar.tsx:171-174`) y `run` hace
  `setFailure(onFailure ? onFailure(error) : failureKind)`: ese `failureKind` es código muerto.

### A.5 — Últimas guardas de cadenas exhaustivas y comparaciones indistinguibles (2)

- `calendar.tsx:202:9` ConditionalExpression → `true` — `if (failure === "download")` de `retry()`.
  Es la última de cuatro guardas; las tres anteriores ya retornaron y `retry()` solo es alcanzable
  con `failure` en `{status, generate, revoke, download}`.
- `calendar-feed-api.ts:83:47` StringLiteral → `"Stryker was here!"` — el valor por defecto de
  `response.headers.get("Content-Type")`. Ninguna cadena que no sea un `text/calendar; charset=utf-8`
  válido puede distinguirse de otra frente al patrón; `""` y `"Stryker was here!"` fallan igual.

## Grupo B — inmatable por esta suite, pero **no** equivalente (1)

- `calendar.tsx:51:49` StringLiteral → `""` — `useState<Busy | null>("loading")`.
  `render()` descarga los efectos dentro de `act`, así que ninguna prueba de unidad puede separar
  `"loading"` de `""`. Pero en un navegador real hay un fotograma pintado sin el `role="status"`
  «Cargando…», porque el efecto de carga (`calendar.tsx:134`) es `useEffect` y no `useLayoutEffect`.
  Cambia una conducta que **este corredor** no puede ver. **La etiqueta correcta es «inmatable por
  esta suite», no «equivalente».** Cuenta como vivo.

## Grupo C — punto ciego del corredor, ni equivalencia ni hueco (3)

- `calendar.tsx:16:42` StringLiteral → `""` (el locale `"es"` de `new Intl.DateTimeFormat`), `testsCompleted=0`
- `calendar.tsx:17:14` StringLiteral → `""` (`dateStyle: "medium"`), `testsCompleted=0`
- `calendar.tsx:18:14` StringLiteral → `""` (`timeStyle: "short"`), `testsCompleted=0`

**Siguen exactamente igual que en la campaña anterior, y lo digo sin restarlos del denominador.**
Son mutantes estáticos: con un locale vacío `Intl.DateTimeFormat` lanza `RangeError` al evaluar el
módulo, el fichero de pruebas no llega a importarse, Stryker se queda con **cero pruebas ejecutadas**
y aplica el valor por defecto `Survived` en vez de `RuntimeError`. El mutante hermano del mismo sitio
(`16:48 ObjectLiteral`), que no revienta la evaluación del módulo, corre 3 pruebas y muere. La prueba
que debería matarlos existe. **Es un defecto de clasificación del corredor**, y penaliza el score por
un fallo de Stryker, no del trabajo. Aun así cuentan como vivos en el 390.

## Grupo D — huecos reales (18)

Trabajo pendiente del `tdd_craftsman` si el `craftsman_lead` decide seguir apretando. El umbral ya
está superado sin tocarlos, y **no propongo relajarlo ni recortar el alcance**.

### En `src/calendar.tsx` (9)

1. **`31:7` ConditionalExpression → `false`** — ver N3 arriba. *(nuevo, región del campo de la url)*
2. **`34:3` CallExpression → `;`** — ver N2 arriba. *(nuevo, región del campo de la url)*
3. **`35:3` CallExpression → `;`** — ver N1 arriba. *(nuevo, región del campo de la url)*
4. **`66:6` ArrayDeclaration** — dependencias `[]` → `["Stryker was here"]` en el `useLayoutEffect`
   de foco de montaje: el efecto se remonta en cada render en vez de una sola vez.
5. **`74:6` ArrayDeclaration** — ídem en el `useEffect` de `focusin`.
6. **`98:5` ArrayDeclaration** — ídem en el `useLayoutEffect` de limpieza del object URL.
7. **`138:6` ArrayDeclaration** — ídem en el `useEffect` de carga. Es el más notable de los cuatro
   porque **existe la prueba que debería matarlo** —`@s31 la vista lee el estado una sola vez aunque
   vuelva a renderizar`, que exige un único GET a `/api/v1/me/calendar-feed`— y no lo mata: el
   mutante `108:9` (`if (pending.current) return`) tapa la relectura mientras la primera petición
   sigue en vuelo, y el remontaje del efecto no llega a producir un segundo GET observable.
   *Falta:* forzar un render **después** de que la carga haya terminado y volver a contar, o
   comprobar el número de altas del efecto con un espía.
8. **`85:20` ConditionalExpression → `true`** — la condición
   `document.activeElement === document.body` de la restauración de foco. Con `true`, el foco vuelve
   al control iniciador **aunque la persona lo haya movido**. *Falta:* el caso en que
   `document.activeElement` no es ni el `body` ni el iniciador.
9. **`152:7` CallExpression → `;`** — `setCopy(null)` de `revoke`. Enmascarado por el renderizado: al
   revocar, `link` pasa a `null` y el aviso «Enlace copiado» se desmonta con la sección entera.
   *Falta:* copiar, revocar y volver a crear un enlace, exigiendo que el aviso de copia no reaparezca.

### En `src/calendar-feed-api.ts` (9)

El patrón dominante es el **enmascaramiento por aserción débil**: las pruebas afirman que la promesa
rechaza sin fijar *qué* comprobación rechazó, y otra comprobación posterior atrapa el mismo caso.

10. **`36:55` ArrowFunction → `() => undefined`** — el `.catch(() => incompatible())` del JSON del
    estado. Con `undefined`, la línea siguiente evalúa `typeof status.active` sobre `undefined` y
    lanza `TypeError`: sigue rechazando. *Falta:* afirmar el mensaje o el tipo del error.
11. **`54:53` ArrowFunction → `() => undefined`** — el mismo, en la creación. Idéntico.
12. **`79:3` CallExpression → `;`** — `signal.throwIfAborted()` tras la respuesta de la descarga.
13. **`90:3` CallExpression → `;`** — `signal.throwIfAborted()` tras leer los octetos.
    Los dos se enmascaran entre sí: abortar tras la respuesta sigue muriendo en el otro
    `throwIfAborted`. *Falta:* abortar **durante** `arrayBuffer()`, entre uno y otro.
14. **`88:7` ConditionalExpression → `false`**
15. **`88:7` LogicalOperator** — `!declared || !regex.test(declared)` pasa a `&&`
16. **`88:53` CallExpression → `;`** — el `incompatible()` del Content-Length
17. **`88:21` Regex** — pierde el ancla `^`
18. **`88:21` Regex** — pierde el ancla `$`
    Los cinco del Content-Length. La tabla `it.each` de `calendar-feed-api.test.ts:299` (ausente,
    vacía, `"x12"`, `"12x"`, `"0"`, `"012"`) **pasa por el motivo equivocado**: la comprobación de la
    línea 91, `if (bytes.byteLength !== Number(declared)) incompatible()`, atrapa las seis filas por
    su cuenta. Que `88:7 -> false` sobreviva lo demuestra: **el `incompatible()` de la línea 88 no se
    dispara en ninguna de las 98 pruebas.** *Falta:* afirmar cuál comprobación rechaza (mensaje
    distinguible o espía sobre `incompatible`), o una fila donde el número declarado coincida con la
    longitud real y el formato siga siendo inválido —`"012"` con un cuerpo de 12 octetos.

## Cuadre

23 (equivalentes) + 1 (inmatable por la suite) + 3 (punto ciego del corredor) + 18 (huecos reales)
= **45**. Ninguno queda sin enumerar. 345 + 45 = **390**.

---

# Veredicto

**PASS.** **345/390 = 88.46 %**, umbral **80 %**. Código de salida 0. La feature 26 supera la puerta
de mutación del frontend con **33 mutantes de margen**, esta vez **sobre `main` (`d418a5d`)**, con el
campo de la url en su forma actual y con el alcance de `stryker.ics-calendar.config.json` verificado
por contenido contra el `App.tsx` fusionado.

Queda cumplida la condición que el juez dejó abierta: el 88.54 % de la rama ya no acredita nada; lo
que acredita la feature es este 88.46 % medido sobre estas fuentes.

**Lo que este PASS no acredita, y conviene que el `craftsman_lead` lo tenga por escrito:**

- Los atributos de accesibilidad del `<div role="textbox">` nuevo (`role`, `aria-readonly`,
  `aria-labelledby`) **no generan ni un mutante**. Su corrección no está medida aquí.
- Los 3 supervivientes nuevos de `selectAll` son **huecos reales del arreglo**, no ruido. El más
  serio es `35:3`: se puede borrar la línea que aplica la selección y las 98 pruebas siguen verdes.
  El E2E sí lo cubriría, pero Stryker no ejecuta Playwright.
- Si hubiera que elegir un solo test que escribir, es el de N1: afirmar el **resultado** de la
  selección, no el trámite.

## Qué NO he hecho

- **No he ejecutado PIT ni nada de backend**, ni la suite completa, ni Playwright, ni axe. Había una
  suite de backend corriendo en la máquina y esta campaña de Stryker no levanta contenedores. Una
  sola campaña, la pedida.
- No he tocado `src/`, ni pruebas, ni `.feature`, ni `feature_list.json`. Lo único que escribo es
  este informe.
- No he reproducido a mano ninguna mutación: tengo prohibido editar producción. Las equivalencias
  están argumentadas leyendo el código y contrastadas con el `mutation.json`.
- No he abierto el informe HTML; trabajé sobre `mutation.json` y la salida de texto.
- La mutación de backend de esta feature sigue sin producir score
  (`progress/mutation_ics_calendar_backend.md`): la calidad de sus pruebas sigue sin medir, y este
  PASS no dice nada de ella.
