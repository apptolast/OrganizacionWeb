APPROVED

# Review — feature 26 `ics_calendar` (tercera lectura)

**Veredicto:** APPROVED

Superficie: `1a12881..521840a` de `claude/ics-calendar` («fix(ics_calendar): la url no se recorta en
ningún eje»), 7 ficheros. Mi bloqueante anterior —el `<textarea rows={3})` recortaba la url en
vertical y el oráculo sólo miraba `scrollWidth`— **está cerrado, y lo he verificado ejecutando, no
leyendo**.

Apruebo con **dos condiciones que no son mías** y una lista corta de correcciones menores:

- **La mutación frontend hay que relanzarla.** El PASS de 340/384 se midió sobre `8527823` y
  `521840a` **vuelve a tocar `frontend/src/calendar.tsx`**, que está entero en el alcance de
  `frontend/stryker.ics-calendar.config.json`. Ese PASS ya no acredita el código entregado. Es la
  puerta del `mutation_tester`, que corre después de mi firma; no marcar `done` sin ella.
- **La mutación de backend sigue sin producir score.** Igual que en el dictamen anterior.

---

## Lo que he ejecutado yo (distinto de lo que cito)

| Qué | Resultado |
| --- | --- |
| `pnpm exec vitest run src/calendar.test.tsx src/calendar-feed-api.test.ts` @ `521840a` | **98/98 verdes** |
| `pnpm exec vitest run src/theme-tokens.test.ts` (único otro test que lee `styles.scss`) | **14/14 verdes** |
| `pnpm exec tsc --noEmit` | **limpio** |
| **El oráculo nuevo, copiado literal del spec, contra `1a12881` y contra `521840a`** | rojo en el viejo, verde en el nuevo (tabla abajo) |
| **Sensibilidad del oráculo** frente a cinco formas de recortar | caza cuatro, deja pasar la única que no pierde contenido |
| **Árbol de accesibilidad por CDP** (`Accessibility.getFullAXTree`) del campo viejo y del nuevo | comparados propiedad a propiedad (tabla abajo) |
| **axe** (`wcag2a`, `wcag2aa`, `wcag21aa`, `wcag22aa`, `best-practice`) sobre ambos campos | **0 violaciones en los dos** |
| Recorrido de `Tab`, `:focus-visible`, selección al enfocar y `Ctrl+A` sobre ambos campos | medidos (abajo) |

No he ejecutado: E2E, mutación, `bin/harness init`, ni suite completa de nada. Las mediciones de
navegador son páginas estáticas con el DOM de `calendar.tsx` y la **hoja real compilada** con
`sass` desde `frontend/src/styles.scss` de cada commit; no es la aplicación servida y lo digo cada
vez que apoyo una conclusión en ellas.

---

## 1. ¿El oráculo nuevo se pone rojo contra `1a12881` y verde contra `521840a`? — SÍ

Copié la función `clipped` **literal** de `e2e/ics-calendar-ux.spec.mjs` de `521840a` y la corrí
contra las dos versiones del DOM con sus dos hojas compiladas, aplicando el mismo `doubleText` del
spec:

| | 320 / 100 % | 320 / 200 % | 768 / 200 % | 1280 / 200 % |
| --- | --- | --- | --- | --- |
| **`1a12881` (textarea)** | `[]` | **`["TEXTAREA#calendar-link:http://127.0.0.1:18092/c [alto 325 en 153]"]`** | `[]` | `[]` |
| **`521840a` (campo nuevo)** | `[]` | **`[]`** | `[]` | `[]` |

- **Se pone rojo contra el viejo y señala exactamente el elemento que yo señalé**, con mi propia
  cifra (325 en 153) y el mismo `clientHeight`. El artesano mide 282 en 153 sobre la pila real; la
  diferencia es de métrica de fuente entre su pila y mi réplica, no de conclusión.
- **Se pone verde contra el nuevo en las seis combinaciones.**
- Su hallazgo extra —`[alto 108 en 87]` a 320 px con **texto normal**— no lo reproduzco: en mi
  réplica la url cabe justa en las tres filas al 100 %. Va en su contra, no en la mía: significa
  que el defecto era **peor** de lo que yo medí, y el oráculo lo encontró donde yo no había mirado.
  Lo cito, no lo acredito.

### Sensibilidad: ¿deja fuera algo que sí recorte?

Probé cinco formas de recortar contenido con la misma función:

| Elemento | `overflow` computado | ¿Lo caza? |
| --- | --- | --- |
| `<input type="text">` con `text-overflow: ellipsis` (**el defecto original de la feature**) | `clip/clip` | **sí** |
| `<div>` con altura fija y `overflow: hidden` | `hidden/hidden` | **sí** |
| `<div>` con `overflow-x: hidden` y `white-space: nowrap` | `hidden/auto` | **sí** |
| `<div>` con `overflow: clip` | `clip/clip` | **sí** |
| `<div>` con altura fija y `overflow: visible` | `visible/visible` | **no** (por diseño) |

Dato que vale la pena: **el oráculo nuevo habría cazado también el `ellipsis` original**, no sólo el
`textarea`. Ya no es una prueba que mire donde el autor sabía que estaba el problema.

## 3. ¿Está bien razonada la exclusión de `overflow: visible`? — SÍ, con un límite que conviene nombrar

Su argumento —«con `overflow: visible` el contenido se desborda a la vista pero no se pierde»— es
correcto y lo he comprobado: en ese caso el texto se pinta fuera de la caja y sigue legible; el
desbordamiento de la **página** se comprueba aparte (`root.scrollWidth > root.clientWidth`), y si un
antepasado sí recortase, ese antepasado aparecería en la propia lista `clipped`, porque el barrido
recorre `main, main *` y no sólo el campo.

El límite real, que no bloquea y que hoy no está vivo: con `overflow: visible` el texto desbordado
puede **superponerse** a lo que tiene debajo, y eso ni lo mide este oráculo ni lo ve axe. No es un
riesgo actual: en la hoja compilada **no hay ni una sola declaración de `overflow` dentro de
`.calendar-feed`**, así que en esta vista ningún elemento tiene caja que recorte y ninguno tiene
altura fija. Que el oráculo dispare contra el `textarea` (cuyo `overflow: auto` es del navegador, no
de la hoja) demuestra además que la exclusión no lo deja mudo.

## 4. ¿La asimetría de `escaping` esconde un hueco? — NO la que él declara; sí una menor que no declara

- **La asimetría declarada es correcta.** `escaping` mira `right > clientWidth + 1` y no mira el
  borde inferior porque desplazarse en vertical es navegación normal, no pérdida de contenido. Un
  elemento que quede por debajo del pliegue **dentro** de una caja con altura fija sí se pierde, y
  ése lo caza `clipped`. Los dos ejes están cubiertos por la herramienta adecuada en cada caso.
- **La que no declara**, y es la única que veo: `escaping` tampoco mira el borde **izquierdo**
  (`left < 0`), y el desbordamiento por la izquierda no aumenta `documentElement.scrollWidth`, así
  que se le escaparía a los dos oráculos a la vez. Hoy es teórico —no hay RTL, ni márgenes
  negativos, ni posicionamiento absoluto en esta vista—, pero es un hueco real de la comprobación.
  Lo dejo como mejora, no como condición.

---

## 2. ¿Degrada la accesibilidad cambiar el `<textarea>` por un `role="textbox"`? — CASI NADA, y lo he medido

Lo miré con la desconfianza que se me pidió: cambiar un control nativo por un rol ARIA es donde se
pierden cosas sin querer. Comparé los dos campos con el **árbol de accesibilidad de Chromium**
(`Accessibility.getFullAXTree` por CDP), no con mi opinión:

| Propiedad expuesta | `1a12881` `<textarea readonly>` | `521840a` `<div role="textbox">` |
| --- | --- | --- |
| `role` | `textbox` | `textbox` |
| `name` | «Enlace de suscripción» (por `<label for>`) | «Enlace de suscripción» (por `aria-labelledby`) |
| **`value`** | **la url completa** | **la url completa** |
| `focusable` | `true` | `true` |
| `readonly` | `true` | `true` |
| `editable` | `"plaintext"` | **ausente** |
| **`multiline`** | **`true`** | **`false`** |
| Alcanzable con `Tab` | sí | **sí** (primera parada de `main`) |
| `:focus-visible` tras `Tab` | `true`, `outline 3px solid` | **`true`, `outline 3px solid`** |
| Selección al enfocar | url completa (79/79) | **url completa (79/79)** |
| **axe** (5 conjuntos de reglas) | **0 violaciones** | **0 violaciones** |

**Lo que importa no se pierde:** rol, nombre accesible, **valor** (un lector de pantalla sigue
leyendo la url, que era mi mayor sospecha), estado de solo lectura, alcance por teclado, foco
visible y selección íntegra. Y axe está limpio en los dos.

**Lo que sí cambia, y lo digo por su nombre:**

1. **`multiline` pasa de `true` a `false`** mientras el campo ocupa varias líneas en pantalla. Un
   lector de pantalla lo anunciará como campo de una sola línea. Es defendible —el campo no acepta
   entrada de ninguna clase, y `aria-multiline` describe la entrada— pero la lectura más fiel sería
   `aria-multiline="true"`. **Corrección de una línea, no bloqueante.**
2. **`Ctrl+A` deja de estar acotado al campo.** Medido: con el foco en el campo nuevo, `Ctrl+A`
   selecciona **165 caracteres**, es decir el documento entero, no los 79 de la url; en el
   `<textarea>` seleccionaba sólo el campo. **No incumple @s38**: el `Tab` que lleva al campo ya
   deja la url **entera seleccionada** (verificado: `getSelection().toString() === url`), así que
   «su contenido completo se selecciona con teclado» se cumple, y con una pulsación menos. Que el
   spec haya retirado la pulsación de `Ctrl+A` es coherente con el cambio y está comentado en el
   sitio, no escondido. Pero es una conducta que antes existía y ya no.
3. La `<label>` real pasa a ser un `<span class="field-label">`. El nombre accesible se conserva por
   `aria-labelledby`, pero se pierde el «clic en la etiqueta enfoca el campo» que da `<label for>`.
   Menor.

**Sobre la solidez del arreglo en sí:** su argumento es correcto y lo he comprobado. Un `textarea`
no puede tomar la altura de su contenido sin JavaScript, y el JavaScript no serviría porque el
barrido dobla la letra **después** de montar. El elemento nuevo no declara `overflow`, así que no
hay caja que recortar a ningún ancho ni con ningún tamaño de letra: es una solución por
construcción, no un número ajustado a ojo. De paso mueren tres cosas que yo había señalado —el
`rows={3}` mágico, el `resize: vertical` que dejaba el arreglo en manos de la persona, y el
`spellCheck={false}` que era producción sin oráculo (mutante `249:25`)—.

`copyLink()` copia de `link.url`, no del DOM (`calendar.tsx`), así que el cambio de elemento no
toca el camino del portapapeles ni su respaldo manual.

## 5. ¿Cuadran las cifras corregidas de la bitácora? — SÍ

`progress/tdd_ics_calendar.md:848-856` frente a `progress/mutation_ics_calendar_frontend_final.md`:

| Paso | Bitácora | Informe del corredor | ¿Cuadra? |
| --- | --- | --- | --- |
| De los 24 antiguos, equivalentes | **8** (añade `300:15`, el que yo eché en falta) | los 8 del grupo 4: `83:47`, `55:5`, `67:21`, `77:12`, `143:7`, `300:15`, `149:7`, `192:9` | **sí** |
| De los 24 antiguos, huecos | 16 | 25 − 8 equivalentes − 1 nuevo = 16 | **sí** |
| Menos `187:21`, cerrado en el ciclo 20 | 15 | — | **sí** |
| Más el mutante nuevo `249:25` | 16 | — | **sí** |
| Menos `249:25`, que desaparece con el ciclo 21 | **15 huecos reales abiertos** | — | **sí** |

Y admite por escrito que el total anterior de 16 «coincidía por casualidad aritmética: dos deslices
que se compensaban», que es exactamente lo que yo había encontrado. Cuadra.

---

## Cobertura de escenarios (@s ↔ test)

- **@s38 [x] — cerrado.** Las once cláusulas tienen oráculo, y la que faltaba —«ningún ancho recorta
  la url»— tiene ahora uno **general** (los dos ejes, todos los elementos de `main`) que he
  verificado que discrimina: rojo contra el código defectuoso, verde contra el corregido, y sensible
  a cuatro formas distintas de recortar. La cláusula de selección con teclado sigue cubierta, ahora
  con un oráculo **más fuerte** en E2E (`getSelection().toString()` comparado con el `textContent`)
  y uno más débil en unidad (espía sobre `Range.selectNodeContents`), con la limitación de jsdom
  declarada en el propio comentario. Esa división es honesta y está en el sitio correcto.
- @s1–@s37 sin cambios respecto a mis dos dictámenes anteriores. `521840a` sólo toca la vista del
  enlace y sus pruebas; `getByLabel("Enlace de suscripción")` sigue resolviendo por
  `aria-labelledby` en `e2e/ics-calendar.spec.mjs`, y las adaptaciones de `calendar.test.tsx`
  (`toHaveValue` → `toHaveTextContent`, `readonly` → `aria-readonly`) son traducciones fieles del
  mismo aserto, no relajaciones.

## Disciplina TDD

- **Orden respetado, y es lo que más pesa.** Primero el oráculo, en rojo, con la salida pegada y el
  elemento nombrado; después el arreglo. Yo he reproducido el rojo y el verde por mi cuenta y
  coinciden.
- **¿Producción sin test que la pida?** El único candidato es la guarda `if (!selection) return` de
  `selectAll` (`calendar.tsx`), exigida por el tipo `Selection | null` y no por una prueba. Es
  defensiva y trivial. El `spellCheck={false}` que yo había señalado **ha desaparecido**.
- **Honestidad:** la bitácora escribe «el bloqueante es mío de principio a fin», nombra el
  `rows={3}` como número mágico propio, y repite el diagnóstico de método que ya se había aplicado a
  sí mismo en la mutación: «una prueba que mira donde yo ya sabía que estaba el problema no puede
  encontrarlo en ningún otro sitio». Y corrige las tres afirmaciones falsas que yo había citado, una
  por una, con nota de corrección visible en `progress/ux_ics_calendar.md`.

## Checkpoints

- **C1** [~] `bin/harness init` no ejecutado (suite completa, prohibida en esta sesión).
- **C2** [x] Feature 26 `in_progress`; `progress/current.md` describe la sesión.
- **C3** [x] Capas respetadas; el cambio es de una sola capa (vista + hoja).
- **C4** [~] Pruebas por módulo y aislamiento real; `bin/harness test` completo no ejecutado.
- **C5** [ ] Sigue sin entrada en `progress/history.md` para este tramo del carril.
- **C6** [x] **@s38 cubierto.** Mapa @s → prueba correcto. Sin producción sin oráculo salvo la
  guarda de tipo citada.
- **C7** [ ] **Mutación frontend caducada por el propio arreglo** (ver condición 1) y mutación de
  backend sin score. Puerta del `mutation_tester`, no mía.

## Condiciones y correcciones

**Condiciones antes de marcar `done` (no son mías, son de la puerta siguiente):**

1. **Relanzar Stryker `ics_calendar-frontend` sobre `521840a`.** `frontend/src/calendar.tsx` está
   entero en `mutate` de `frontend/stryker.ics-calendar.config.json` y ha cambiado después de la
   campaña que dio 340/384. Cambia el denominador en los dos sentidos: desaparece `249:25`
   (`spellCheck`) y entran los mutantes de `selectAll`. El PASS actual **no acredita el código
   entregado**.
2. **La mutación de backend de esta feature sigue sin producir score.**

**Correcciones menores, ninguna bloqueante, todas de una o dos líneas:**

3. `progress/ux_ics_calendar.md:64-65` sigue diciendo «con el foco en el campo y `Ctrl/Cmd+A`,
   `selectionStart` es 0 y `selectionEnd` es la longitud completa del valor». **Eso ya no es lo que
   se mide ni lo que ocurre**: el spec retiró la pulsación, un `div` no tiene `selectionStart`, y he
   medido que `Ctrl+A` sobre el campo nuevo selecciona el documento entero. Redactarlo como lo que
   hoy se afirma: al enfocarlo con `Tab`, `getSelection().toString()` es la url completa.
4. `progress/ux_ics_calendar.md:45` sigue diciendo que los objetivos de 44 px se miden sobre
   «`button`, `a` y `textarea`»; el selector es hoy `[data-calendar-link]`.
5. Añadir `aria-multiline="true"` al campo, o dejar escrito por qué `false` es lo correcto para un
   campo que no acepta entrada (§2, punto 1).
6. **Mantengo como no bloqueante** mi punto 4 anterior: endurecer «foco visible» en
   `e2e/ics-calendar-ux.spec.mjs` comparando el estilo del elemento **con** y **sin** foco, en vez
   de aceptar cualquier `box-shadow` presente. Hoy no engaña a nadie —el indicador es el `outline`
   de `:focus-visible`, que he medido en `3px solid` en las tres paradas—, pero la disyunción sigue
   pudiendo abrirse sola con el tiempo.
7. Menor: `escaping` tampoco mira el borde izquierdo (§4). Teórico hoy.

## Qué NO he verificado

- **Ningún E2E.** Las 8 pruebas Playwright verdes que declara las cito. Lo que yo he verificado es
  la **lógica del oráculo**, extraída del spec y ejecutada sobre réplicas estáticas del DOM con la
  hoja real compilada de cada commit; no es la pila levantada.
- **Ni Stryker ni PIT.**
- `bin/harness init`, suite completa de backend y de frontend.
- **Lector de pantalla real**: lo que comparo es el árbol de accesibilidad que expone Chromium, que
  es lo que consume la API de accesibilidad; no he escuchado NVDA ni VoiceOver, y el propio
  `progress/ux_ics_calendar.md` lo declara como límite.
- No he editado código, pruebas, `.feature` ni `feature_list.json`. El worktree del carril sigue
  limpio en `521840a`; mis mediciones vivieron en el scratchpad de la sesión.
