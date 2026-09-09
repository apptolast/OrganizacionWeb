# Mutación frontend — feature 26 `ics_calendar` (Stryker, reejecución)

**Veredicto: PASS** — Score: **340/384 = 88.54 %**, frente al umbral del **80 %**. Supera el umbral
por 8.54 puntos, es decir por **33 mutantes** de margen (308/384 = 80.21 % sería el mínimo).

El denominador es íntegro: 384 mutantes generados, 384 contabilizados. No he reclasificado ni
excluido ninguno. Los 44 supervivientes —incluidos los 12 que el `tdd_craftsman` declara
equivalentes y los 3 de `Intl` que atribuye al corredor— siguen contando en el denominador, y el
88.54 % los incluye a todos.

## Ejecución

| | |
| --- | --- |
| Comando | `node scripts/project.mjs mutate ics_calendar-frontend` |
| Despacho real | `pnpm --dir frontend exec stryker run stryker.ics-calendar.config.json` |
| Checkout | **`8527823` (`claude/ics-calendar`), worktree `C:/Users/vhurt/ow-worktrees/ics-calendar`, árbol limpio (`git status --short` vacío)** |
| Ventana | 2026-09-09T12:40:50Z entrada, 2026-09-09T12:50:02Z salida (9 min 12 s) |
| Código de salida | **0** (`Final mutation score of 88.54 is greater than or equal to break threshold 80`) |
| Ficheros mutados | 4 de 204; 384 mutantes instrumentados; 26.25 pruebas por mutante de media |
| Dry run | 1011 pruebas del frontend en verde, 2 min 2 s |
| Informes | `C:/Users/vhurt/ow-worktrees/ics-calendar/frontend/reports/mutation-ics-calendar/mutation.json` y `mutation.html` |

### Sobre qué checkout corrí, y por qué

**La rama del artesano NO está integrada en `main`.** `git branch --contains 8527823` devuelve sólo
`claude/ics-calendar`. La campaña corrió por tanto sobre la rama, en su worktree, no sobre `main`.

Segundo dato que conviene dejar escrito: el artesano dice que la rama está «reasentada sobre
`origin/main`», y no lo está del todo. `git merge-base main claude/ics-calendar` da `9c2dbe4`,
mientras que `main` está en `e3c08a3`. La rama va un commit por detrás de `main`; el commit que le
falta (`e3c08a3`, «limpiar las fixtures por el punto único») es de fixtures de backend y no toca
nada del frontend, así que no contamina esta campaña. Pero la afirmación, tal cual, es inexacta.

## Tabla de estados (denominador íntegro)

| Estado | Total | calendar-feed-api.ts | calendar.tsx | App.tsx | workspace.tsx |
| --- | ---: | ---: | ---: | ---: | ---: |
| Killed | 339 | 114 | 213 | 8 | 4 |
| Timeout | 1 | 0 | 0 | 0 | 1 |
| Survived | 44 | 10 | 34 | 0 | 0 |
| NoCoverage | **0** | 0 | 0 | 0 | 0 |
| RuntimeError | 0 | 0 | 0 | 0 | 0 |
| CompileError / NonViable | 0 | 0 | 0 | 0 | 0 |
| Ignored | 0 | 0 | 0 | 0 | 0 |
| **Total** | **384** | **124** | **247** | **8** | **5** |

Score por fichero: `App.tsx` 100 %, `workspace.tsx` 100 % (su único no-Killed es un `Timeout`, que
cuenta como muerte), `calendar-feed-api.ts` **91.94 %**, `calendar.tsx` **86.23 %**.

**Los 16 `NoCoverage` de la campaña anterior han desaparecido: ahora son 0.** Todo el código mutado
se ejecuta al menos una vez. Es el cambio de calidad más difícil de fingir: las doce ramas de
reintento y las cuatro sueltas que antes no se ejecutaban jamás, ahora se ejecutan.

## El denominador: 383 a 384, y por qué sube

Comparé mutante a mutante el `mutation.json` de la campaña anterior (conservado íntegro en
`<repo>/frontend/reports/mutation-ics-calendar/mutation.json`, 383 mutantes) contra el nuevo, por
`(mutador, sustitución)`. La diferencia es exactamente una:

```
old=1  new=2   BooleanLiteral | true
```

Un mutante `BooleanLiteral -> true` de más. Nada se ha ido; nada se ha restado. El denominador sube,
que es la dirección segura.

La causa es que **el artesano sí tocó producción**, y hay que decirlo con precisión porque su informe
afirma lo contrario:

- El commit `8527823` (la respuesta a la mutación) toca sólo `calendar.test.tsx`,
  `calendar-feed-api.test.ts` y `progress/tdd_ics_calendar.md`. Ahí su frase «las pruebas nuevas no
  cambian ni una línea de producción» es **cierta**.
- Pero el commit anterior de la misma rama, `a158cf2` («cerrar @s38»), **sí cambia
  `frontend/src/calendar.tsx` y `frontend/src/styles.scss`**: el campo del enlace pasa de
  `<input type="text">` a `<textarea rows={3} spellCheck={false}>`. Respecto al checkout de la
  campaña anterior (`a6164e4`), el código bajo los mutantes se ha movido.

De ahí el mutante nuevo: `spellCheck={false} -> true` (`calendar.tsx:249:25`). Stryker no muta el
valor de un atributo JSX de texto, así que el `type="text"` que desapareció no restaba nada, y el
`{false}` que apareció suma uno. `calendar-feed-api.ts` no ha cambiado ni un byte entre `a6164e4` y
`8527823` (`git diff --stat` vacío), y sus 124 mutantes son los mismos 124.

Que el fichero mutado se moviera no invalida la comparación —los 383 anteriores siguen siendo 383 de
los 384 de ahora, con el mismo mutador y la misma sustitución—, pero la afirmación «sin cambiar una
sola línea de producción» aplicada a la rama entera es **falsa**, y el mutante 44 de esta campaña es
consecuencia directa de ese cambio.

## Comparación con la campaña anterior

| | Anterior (`a6164e4`) | Ahora (`8527823`) | Delta |
| --- | ---: | ---: | ---: |
| Total | 383 | 384 | +1 |
| Killed (+Timeout) | 268 | 340 | **+72** |
| Survived | 99 | 44 | -55 |
| NoCoverage | 16 | **0** | -16 |
| Score | 69.97 % | **88.54 %** | **+18.57 pt** |
| Pruebas de la feature | 40 | **96** | +56 |

El recuento de 96 pruebas está confirmado por el propio corredor, no por el artesano: el
`mutation.json` registra `src/calendar.test.tsx` con 51 pruebas y `src/calendar-feed-api.test.ts`
con 45. 51 + 45 = 96. Su cifra es exacta.

---

# Los 44 supervivientes, uno a uno

## Grupo 1 — los 5 equivalentes que declaró la campaña anterior: 4 sobreviven, 1 estaba mal

- **A1 `67:9` ConditionalExpression -> `true`.** Sobrevive. Equivalencia sostenida.
- **A2 `168:9` ConditionalExpression -> `false`** (`if (!link) return` de `copyLink`). Sobrevive.
- **A3 `170:9` ConditionalExpression -> `false`** (guarda del portapapeles). Sobrevive.
- **A4 `85:11` ConditionalExpression -> `true`** (limpieza de desmontaje). Sobrevive.
- **A5 `152:13` ConditionalExpression -> `true`** (la guarda previa al Blob). **AHORA MUERE.**

**A5 era una equivalencia mal argumentada de mi campaña anterior, y la retiro.** El razonamiento
—«revocar una url inexistente es un no-op»— es cierto para el DOM pero falso para el observador: en
cuanto una prueba espía `URL.revokeObjectURL` y exige que no se llame en la primera descarga, la
distinción existe. La prueba B9 del artesano la mató. Un punto para él y un error mío corregido.

## Grupo 2 — los 12 que el artesano declara equivalentes: los 12 sobreviven. Reviso cada argumento

### B4, las ocho guardas de carrera de `run` (mutantes 22-29) — 8 ACEPTADAS

Sobreviven las ocho: `98:9->false`, `105:11 Cond->false`, `105:11 Logical(&&)`, `105:40->false`,
`108:11 Cond->false`, `108:11 Logical(&&)`, `108:40->false`, `111:11->true`.

**Invariante 1 (ningún control inicia una operación con otra en vuelo): SUJETA.** Las dos pruebas
que dice haber escrito existen y hacen lo que dice:

- `calendar.test.tsx:916` — `@s32 mientras una operación está en vuelo ningún control puede iniciar
  otra`: retiene el GET de descarga con un `hold(...)` y exige `toBeDisabled()` sobre «Regenerar
  enlace», «Revocar enlace» y «Descargar archivo .ics»; después libera la promesa y exige
  `toBeEnabled()` sobre los tres.
- `calendar.test.tsx:941` — `@s32 sin enlace, crear también queda bloqueado…`: retiene el POST y
  exige `toBeDisabled()` sobre «Crear enlace de suscripción» y «Descargar archivo .ics».

Producción tiene cuatro sitios `disabled={Boolean(busy)}` (líneas 229, 272, 278, 319) y las dos
pruebas nombran los cuatro controles correspondientes. Retirar cualquiera de los cuatro `disabled`
pone roja una de las dos. La correspondencia es 1:1 y verificable leyendo; **no he ejecutado yo la
eliminación porque tengo prohibido editar producción**, y lo digo en vez de fingir que la hice.

Matiz que refuerza el valor de esas dos pruebas: **Stryker no genera ningún mutante sobre
`disabled={Boolean(busy)}`** (no muta el argumento de `Boolean(...)` ni el atributo JSX), así que la
invariante no está sujeta por ningún mutante propio. Lo único que la sujeta son esas dos pruebas
escritas a mano. Sin ellas, las ocho equivalencias serían palabra contra palabra.

**Invariante 2: SUJETA, y por una razón más fuerte que la que él da.** Él la formula como «el
cliente convierte todo aborto en excepción». Lo que verifiqué es más simple y más sólido: en todo
`calendar.tsx` hay **una sola llamada a `abort()`**, la de la limpieza de desmontaje
(`calendar.tsx:83`, `pending.current?.abort()`). Ningún control, ningún camino de error y ninguna
operación abortan nada. Luego `controller.signal.aborted` sólo puede ser cierto después de
desmontar, y ahí React ya ignora las actualizaciones de estado. Y `pending.current !== controller`
sólo puede ser cierto si otro `run` reasignó la referencia, que es justo lo que la invariante 1
prohíbe. Las ocho guardas son inalcanzables con efecto observable.

**Veredicto: acepto las ocho.** Matarlas exigiría simular carreras que la interfaz no permite; sería
relleno, y él tiene razón.

### Mutante 20 — `41:49` `useState<Busy|null>("loading") -> ""` — ACEPTADA CON RESERVA

Sobrevive. El argumento es correcto en lo mecánico: `render()` de Testing Library descarga los
efectos dentro de `act`, el efecto de montaje llama a `run` y fija `busy = "loading"` antes de que
el DOM sea observable, así que ninguna prueba de unidad puede separar `"loading"` de `""`.

Pero **esto no es una equivalencia**, y él mismo lo concede en su caveat: en un navegador real hay
un fotograma pintado sin el `role="status"` «Cargando…», porque es `useEffect` y no
`useLayoutEffect`. Un mutante equivalente es el que no cambia ninguna conducta observable; éste
cambia una que este corredor no puede ver. Lo acepto como **inmatable por esta suite**, no como
equivalente, y dejo escrito que la etiqueta correcta es distinta de la que él usa. El caveat honesto
que escribió es exactamente lo que evita que esto sea un maquillaje; se lo reconozco.

### Mutante 31 — `121:7` `setLink(null)` de `load` -> `;` — ACEPTADA

Sobrevive. Verificado leyendo: `load` se invoca en exactamente dos sitios, el efecto de montaje
(`calendar.tsx:126`, donde `link` ya es `null`) y `retry()` cuando `failure === "status"`
(`calendar.tsx:189`). Y `failure = "status"` sólo lo fija `run("loading","status",…)`, es decir el
propio `load`; un fallo de estado implica que el estado nunca llegó, luego nunca se mostró un
enlace, luego `link` es `null`. La asignación es inalcanzable con efecto. Argumento sólido.

### Mutante 33 — `135:7` `setConfirming(null)` de `generate` -> `;` — ACEPTADA, con asimetría

Sobrevive. El argumento es correcto: el botón de confirmar hace `setConfirming(null)`
(`calendar.tsx:300`) antes de despachar la acción, así que la línea de `generate` es redundante.

Lo que no dice es que ese argumento es **simétrico**, y por tanto cubre otros dos mutantes que él NO
declaró y que afirma haber matado:

- `143:7` `setConfirming(null)` de `revoke` -> `;`. **Sobrevive.** Mismo argumento exacto.
- `300:15` `setConfirming(null)` del propio botón de confirmar -> `;`. **Sobrevive.** Es la otra
  mitad del par: con esa línea muerta, la de `generate`/`revoke` limpia igualmente.

Los tres se enmascaran mutuamente: ninguno muere por separado y los tres morirían juntos. Él declaró
uno de los tres como equivalente y prometió haber cerrado los otros dos con la prueba «al confirmar
desaparece la confirmación» (B10). Esa prueba no los mata, y no podía matarlos. La equivalencia de
`135:7` la acepto; la promesa sobre `143:7` y `300:15` no se cumplió.

### Mutante 36 — `140:17` `setStatus({active:false,createdAt:null}) -> setStatus({})` — ACEPTADA

Sobrevive. Verificado exhaustivamente: `status` se lee en exactamente tres sitios de todo el fichero
(`calendar.tsx:183` `status?.createdAt ?? null`, `:184` `status !== null && !status.active`, `:185`
`status !== null && status.active`). Con `{}`: `undefined ?? null` da `null`; `!undefined` es `true`
igual que `!false`; `undefined` es falsy igual que `false`. Los tres rinden idéntico. El único uso
restante es la lista de dependencias de la línea 79, y ahí ambos son objetos recién creados, así que
el efecto se vuelve a lanzar en los dos casos. Sin diferencia observable en el DOM. Correcto.

### Recuento del grupo 2

**12 de 12 sobreviven, tal como él anunció. Confirmo 11 como equivalencias genuinas y 1 (el mutante
20, `41:49`) sólo como inmatable por esta suite, no como equivalencia. Rechazo 0.** Ninguno de los
doce era una excusa para tapar un hueco: los revisé uno a uno contra el código y todos se sostienen.

## Grupo 3 — los tres de `Intl` (46, 47, 48): siguen vivos, y ahora está demostrado por qué

`16:42 -> ""`, `17:14 -> ""`, `18:14 -> ""`. **Sobreviven los tres.** No los resto del denominador.

La campaña anterior lo dejó como hipótesis; ahora es un hecho medido. Los metadatos del
`mutation.json` lo dicen sin ambigüedad:

```
16:48  ObjectLiteral   Killed     testsCompleted=3   static=true
16:42  StringLiteral   Survived   testsCompleted=0   static=true
17:14  StringLiteral   Survived   testsCompleted=0   static=true
18:14  StringLiteral   Survived   testsCompleted=0   static=true
```

**`testsCompleted=0`.** Stryker no ejecutó ni una sola prueba contra estos tres mutantes y aun así
los anotó `Survived`. El mutante hermano del mismo sitio, que no revienta la evaluación del módulo,
corrió 3 pruebas y murió. Son mutantes estáticos: `new Intl.DateTimeFormat("")` lanza `RangeError`
al evaluar el módulo, el fichero de pruebas no llega a importarse, Stryker se queda con cero pruebas
ejecutadas y aplica el valor por defecto `Survived` en vez de `RuntimeError`.

**No es un hueco de las pruebas y no es una equivalencia: es un defecto de clasificación del
corredor.** El artesano tiene razón, la prueba que debería matarlos (`readable()` comparando la
fecha formateada con la misma configuración) existe y sigue ahí, y esos tres mutantes penalizan el
score por un fallo de Stryker, no del trabajo. Aun así no los toco: el 88.54 % los cuenta como
vivos, y si el score dependiera de ellos el veredicto sería el mismo, porque hay 33 de margen.

## Grupo 4 — los 25 supervivientes que nadie declaró (24 de ellos, huecos que dijo haber cerrado)

Aquí está el fondo del asunto. 44 supervivientes = 4 (grupo 1) + 12 (grupo 2) + 3 (grupo 3) + **25
que no aparecen en ninguna declaración**. De esos 25, uno es nuevo (creado por su propio cambio de
producción) y 24 son huecos de la lista original que afirmó haber cerrado con pruebas.

Su cuadre era «110 = 95 cerrados + 12 equivalentes + 3 corredor». La medición dice: **de los 95 que
declara cerrados, 24 siguen vivos. Cerró 71, no 95.**

### En `src/calendar-feed-api.ts` (10 supervivientes; declaró las 32 entradas cerradas)

El patrón dominante es **enmascaramiento**: las pruebas existen, se ejecutan y pasan, pero afirman
sólo `await expect(...).rejects.toThrow()`, sin fijar qué comprobación rechazó. Otra comprobación
posterior atrapa el mismo caso, así que quitar la primera no cambia el resultado. Es la patología
clásica: la prueba describe «falla», no describe la conducta.

1. **`36:55` ArrowFunction** — `.catch(() => incompatible())` del JSON del estado pasa a
   `() => undefined`. Con `undefined`, la línea siguiente evalúa `typeof status.active` sobre
   `undefined` y lanza `TypeError`: sigue rechazando. **Falta:** afirmar el mensaje o el tipo del
   error, no sólo que hay error.
2. **`54:53` ArrowFunction** — el mismo en la creación. Idéntico.
3. **`79:3` CallExpression** — `signal.throwIfAborted()` tras la respuesta de la descarga -> `;`.
4. **`90:3` CallExpression** — `signal.throwIfAborted()` tras leer los octetos -> `;`.
   Los dos se enmascaran entre sí: la prueba que aborta tras la respuesta sigue muriendo en el otro
   `throwIfAborted`. El de la línea 74 sí murió. **Falta:** abortar durante `arrayBuffer()`, entre
   uno y otro. Su afirmación de haber cubierto «abortar tras la respuesta en las cuatro operaciones»
   es cierta para las cuatro operaciones, pero insuficiente para las cuatro guardas de la descarga.
5. **`83:47` StringLiteral** — `response.headers.get("Content-Type") ?? ""` pasa a
   `?? "Stryker was here!"`. **Es un equivalente genuino y lo declaro yo, corrigiendo también mi
   campaña anterior**, que lo llamó «hueco real». `""` y `"Stryker was here!"` fallan igual contra
   el patrón `^text/calendar\s*;\s*charset=utf-8$`; ninguna cadena que no sea un `text/calendar`
   válido puede distinguirse de otra. La prueba «rechaza una descarga sin Content-Type»
   (`calendar-feed-api.test.ts:291`) existe, es correcta y no puede matarlo. **Equivalente,
   documentado, y sigue contando en el denominador.**
6. **`88:7` ConditionalExpression -> `false`**
7. **`88:7` LogicalOperator** — `!declared || !regex.test(declared)` pasa a `&&`
8. **`88:53` CallExpression** — el `incompatible()` del Content-Length -> `;`
9. **`88:21` Regex** — pierde el ancla `^`
10. **`88:21` Regex** — pierde el ancla `$`

    Los cinco del Content-Length. **Y aquí sí hay una prueba que parece cubrirlos y no los cubre.**
    `calendar-feed-api.test.ts:299` es una tabla `it.each` con seis filas —ausente, vacía, `"x12"`,
    `"12x"`, `"0"`, `"012"`— que él cita como «puntos 22, 23, 24, 25, 26 y 27». La tabla existe, se
    ejecuta y pasa. Pero **pasa por el motivo equivocado**: la comprobación de la línea 91,
    `if (bytes.byteLength !== Number(declared)) incompatible()`, atrapa las seis filas por su cuenta
    (`Number("x12")` y `Number("12x")` son `NaN`, `Number("")` es `0`, `Number("0")` es `0`), así que
    la guarda de la línea 88 se puede borrar entera sin que ninguna fila lo note. Que
    `88:7 -> false` sobreviva lo demuestra: **el `incompatible()` de la línea 88 no se dispara en
    ninguna de las 96 pruebas.** El único mutante de esa línea que murió es el de la forma del
    número, porque el camino positivo sí está sujeto.
    **Falta:** afirmar cuál comprobación rechaza (mensaje distinguible o espía sobre
    `incompatible`), o una fila donde `Number(declared)` coincida con la longitud real y aun así el
    formato sea inválido —`"012"` con un cuerpo de 12 octetos es el ejemplo exacto—, que es la única
    que la línea 91 no puede tapar.

### En `src/calendar.tsx` (15 supervivientes no declarados)

11. **`55:5` OptionalChaining** — `heading.current?.focus` pasa a `heading.current.focus`
12. **`67:21` OptionalChaining** — `confirmation.current?.focus` pasa a `confirmation.current.focus`
13. **`77:12` OptionalChaining** — `heading.current?.focus` pasa a `heading.current.focus`

    **Los tres son equivalentes bajo la invariante de renderizado, y aprovecho para corregir mi
    campaña anterior, que los describió mal.** El mutador `OptionalChaining` de Stryker convierte
    `a?.b` en `a.b`; no «acceso sin llamada», como escribí. La única diferencia observable es un
    `TypeError` cuando la referencia es `null`, y las referencias están siempre montadas en los tres
    puntos de uso (el `h1` siempre existe; el grupo de confirmación existe exactamente cuando
    `confirming` es truthy, que es la guarda de la línea 67). **Equivalentes, documentados, contados
    en el denominador.** La prueba `@s31 al abrir la vista el h1 recibe el foco`
    (`calendar.test.tsx:613`) que él escribió es correcta y sí mató el mutante que importaba de esa
    línea; sólo que este otro no era matable.
14. **`56:6` ArrayDeclaration** — dependencias `[]` del efecto de foco de montaje
15. **`64:6` ArrayDeclaration** — dependencias `[]` del efecto `focusin`
16. **`88:5` ArrayDeclaration** — dependencias `[]` de la limpieza de object URL
17. **`128:6` ArrayDeclaration** — dependencias `[]` del efecto de carga

    Los cuatro convierten `[]` en un array literal nuevo en cada render, de modo que el efecto se
    vuelve a montar en cada render en vez de una sola vez. **Hueco real en los cuatro.** El caso 17
    es el más notable porque él sí escribió la prueba que debía matarlo —`@s31 la vista lee el
    estado una sola vez aunque vuelva a renderizar` (`calendar.test.tsx:701`), que exige
    `countOf("GET","/api/v1/me/calendar-feed") === 1`— y no lo mata: el mutante `98:9`
    (`if (pending.current) return`) tapa la relectura mientras la primera petición sigue en vuelo, y
    el remontaje del efecto no llega a producir un segundo GET observable. Es un enmascaramiento
    entre dos mutantes que él declaró en bandos distintos: uno como equivalente (`98:9`) y otro como
    cerrado (`128:6`). **Falta:** forzar un render después de que la carga haya terminado y volver a
    contar; o comprobar el número de altas del efecto con un espía.
18. **`75:20` ConditionalExpression -> `true`** — la condición de la restauración del foco. **Hueco
    real.** Con `true`, el foco se devuelve al control iniciador aunque la persona lo haya movido.
    Escribió cuatro pruebas de foco y mataron la mayoría del bloque B1/B2 —de los 17 mutantes de
    aquellos bloques sólo quedan tres, y dos son equivalentes—, pero esta rama concreta no queda
    sujeta. **Falta:** el caso en que `document.activeElement` no es ni el `body` ni el iniciador.
19. **`142:7` CallExpression** — `setCopy(null)` de `revoke` -> `;`. **Hueco real, enmascarado por
    el renderizado:** al revocar, `link` pasa a `null` y el aviso «Enlace copiado» se desmonta con
    la sección entera, así que da igual que el estado no se limpie. **Falta:** copiar, revocar y
    volver a crear un enlace, exigiendo que el aviso de copia no reaparezca.
20. **`143:7` CallExpression** — `setConfirming(null)` de `revoke` -> `;`. Ver grupo 2, mutante 33:
    es equivalente por el mismo argumento que él usó para `135:7`, pero él lo declaró cerrado, no
    equivalente. Lo dejo como equivalente por simetría, documentado.
21. **`300:15` CallExpression** — `setConfirming(null)` del botón de confirmar -> `;`. Ídem: la otra
    mitad del par de enmascaramiento. Prometido como cerrado por la prueba B10; no lo está.
22. **`149:7` StringLiteral** — `run("downloading", "download", …)` pasa a `""`. **Equivalente
    genuino, y lo declaro yo.** `download` pasa siempre un `onFailure` (`calendar.tsx:161-164`), y
    `run` hace `setFailure(onFailure ? onFailure(error) : failureKind)`: el `failureKind` de la
    descarga es **código muerto**. Ninguna prueba puede matarlo sin borrar el `onFailure`.
    Documentado y contado.
23. **`187:21` ConditionalExpression -> `true`** (la variante que fuerza el operando izquierdo; la
    que fuerza la conjunción entera sí murió). Deja `retriable = failure !== "limit"`, de modo que
    con `failure === null` **el botón «Reintentar» se muestra en el camino feliz**. El
    `{retriable && …}` de la línea 220 no está anidado bajo ningún `{failure && …}`, así que el
    botón aparece de verdad en pantalla. **Hueco real, y con conducta visible para la persona: un
    botón de reintentar permanente sin que nada haya fallado.** De las 96 pruebas, la única que
    afirma la ausencia de «Reintentar» lo hace con `failure === "limit"` (el 413,
    `calendar.test.tsx:608`), donde el mutante y el original coinciden. **Falta:** una línea
    —`expect(screen.queryByRole("button", { name: "Reintentar" })).toBeNull()`— en cualquier prueba
    de camino feliz. Es el superviviente más importante de esta campaña.
24. **`192:9` ConditionalExpression -> `true`** — `if (failure === "download")` del `retry()`.
    **Equivalente bajo la invariante de renderizado:** es la última de cuatro guardas y las tres
    anteriores ya han retornado, y `retry()` sólo es alcanzable desde el botón que `retriable`
    monta, es decir con `failure` en `{status, generate, revoke, download}`. Cuando la ejecución
    llega a la línea 192, `failure` ya es `"download"`. Documentado y contado.
25. **`249:25` BooleanLiteral** — `spellCheck={false}` pasa a `true`. **Mutante nuevo, hueco real, y
    es consecuencia directa de su propio cambio de producción en `a158cf2`.** Ninguna prueba afirma
    que el campo de la url no lleve corrección ortográfica. Es menor, pero es suyo y nadie lo
    declaró.

### Cuadre

4 (grupo 1) + 12 (grupo 2) + 3 (grupo 3) + 25 (grupo 4) = **44**. Ninguno queda sin enumerar.

De los 25 del grupo 4, **ocho entradas son equivalentes genuinos que declaro yo con argumento**
(`api 83:47`; `calendar 55:5`, `67:21`, `77:12`, `143:7`, `300:15`, `149:7`, `192:9`), y las otras
**17 son huecos reales**.

---

# Discrepancias: lo que declaró el artesano frente a lo que mide la campaña

| Afirmación suya | Medición | Veredicto |
| --- | --- | --- |
| «96 pruebas verdes» | `mutation.json`: 51 + 45 = 96; dry run de 1011 pruebas en verde | **CIERTA** |
| «sin cambiar una sola línea de producción» | `8527823` sólo toca pruebas y docs; `a158cf2`, en la misma rama, cambia `calendar.tsx` y `styles.scss` | **CIERTA para el commit, FALSA para la rama** |
| «95 huecos cerrados con pruebas nuevas» | 71 cerrados; **24 de los 95 siguen vivos** | **INEXACTA** |
| «12 equivalentes con argumento individual» | los 12 sobreviven; 11 son equivalencias sólidas, 1 (`41:49`) es inmatable-por-la-suite pero no equivalencia | **SOSTENIDA**, con una etiqueta mal puesta |
| «dos pruebas que pinchan la invariante del `disabled`» | existen (`calendar.test.tsx:916` y `:941`) y cubren los cuatro sitios `disabled={Boolean(busy)}` | **CIERTA** |
| «los 3 de `Intl` son punto ciego del corredor» | `testsCompleted=0` frente a `testsCompleted=3` del hermano; probado, ya no conjeturado | **CIERTA y ahora demostrada** |
| «las tres ramas de reintento repiten el paso fallido y sólo ése» | 12 de los 13 mutantes de B8 mueren; los `NoCoverage` pasan de 16 a 0; las tres pruebas cuentan el verbo repetido y exigen 0 en los otros dos | **CIERTA** |
| «24 de 25 mutantes reproducidos a mano mueren» | no reproducible por mí sin editar producción; la campaña lo contradice en parte: `fatal:false` e `ignoreBOM:false` murieron, pero varios que dio por cerrados no | **PARCIAL** |
| «la rama está reasentada sobre `origin/main`» | `merge-base` = `9c2dbe4`; `main` = `e3c08a3`; va un commit por detrás | **INEXACTA**, sin efecto aquí |

**Lo importante, y va a su favor.** El escenario que había que descartar era el del artesano que
sube el score con pruebas que no describen ninguna conducta. No es lo que ha pasado. Las pruebas que
revisé —las tres de reintento (`calendar.test.tsx:520`, `:542`, `:562`), las dos del 413 (`:583`,
`:598`) y las dos de la invariante del `disabled` (`:916`, `:941`)— afirman exactamente lo que hay
que afirmar: repiten el paso fallido y cuentan cero en los otros dos verbos; exigen el botón
«Reintentar» presente con un 503 y ausente con un 413; y sujetan el `disabled` de los cuatro
controles. Los 16 `NoCoverage` en cero y las 72 muertes nuevas son consistentes con trabajo real.

Donde falla es en el **cuadre**: dio por cerrados 24 huecos que no lo están, casi todos por la misma
causa técnica —pruebas que afirman `rejects.toThrow()` o «desapareció del DOM» cuando otra
comprobación del propio código ya produce ese mismo efecto—. Es un error de medición honesto, no un
maquillaje: si hubiera querido inflar la cifra, esas 24 habrían sido el sitio donde mentir, y no
mintió; dijo que no podía dar el score y que lo relanzara el `mutation_tester`. Aun así, la frase
«95 cerrados» no debe pasar al `judge` sin corregir a **71 cerrados, 24 pendientes**.

---

# Veredicto

**PASS.** 340/384 = **88.54 %**, umbral 80 %. Código de salida 0. La feature 26 supera la puerta de
mutación del frontend con 33 mutantes de margen, sobre el checkout `8527823` de
`claude/ics-calendar`, que **no está integrado en `main`**.

Condición que debe quedar registrada para el `craftsman_lead`: **el PASS acredita la rama, no
`main`.** Al integrar hay que comprobar que `calendar.tsx` y `calendar-feed-api.ts` llegan
idénticos; si el merge los toca, esta campaña deja de acreditar nada.

Ninguno de los supervivientes es trabajo mío. Los 17 huecos reales que quedan (los 25 del grupo 4
menos los 8 que declaro equivalentes) le corresponden al `tdd_craftsman`, en rojo primero, si el
`craftsman_lead` decide seguir apretando; el umbral ya está superado y **no propongo relajarlo ni
recortar el alcance**. Si hubiera que elegir uno solo, sería el 23: el botón «Reintentar» permanente
es el único superviviente con una conducta rota que una persona vería.

## Qué NO he verificado

- **No he ejecutado PIT ni nada de backend.** La campaña de backend de esta feature sigue sin
  producir score; la calidad de sus 97 pruebas sigue sin medir
  (`progress/mutation_ics_calendar_backend.md`).
- No he ejecutado la suite completa del proyecto, ni Playwright, ni axe. Los 569 renglones nuevos de
  `e2e/ics-calendar-ux.spec.mjs` que trae `a158cf2` no están acreditados por esta campaña.
- No he reproducido a mano ninguna mutación: tengo prohibido editar `src/`. Las equivalencias que
  declaro y las que acepto están argumentadas leyendo el código y contrastadas con el
  `mutation.json`, no ejecutando variantes.
- El `Timeout` de `workspace.tsx` lo doy por muerte, que es como lo cuenta Stryker; no he
  investigado por qué expira.
- No he abierto el informe HTML; trabajé sobre `mutation.json` y el resumen de texto.
- **No he tocado código, pruebas, `.feature` ni `feature_list.json`.** Lo único que escribo es este
  informe, en el árbol principal (`<repo>/progress/`), no en el worktree de la rama.
