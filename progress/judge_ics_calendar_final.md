REJECTED

# Review — feature 26 `ics_calendar` (segunda lectura)

**Veredicto:** REJECTED

Superficie: `186ff8d..1a12881` de `claude/ics-calendar`, de la que juzgo los tres commits del
carril — `a158cf2` (@s38), `8527823` (huecos de mutación) y `1a12881` (el «Reintentar»)—.
La rama no está integrada; el resto del rango es la feature 27 y no lo vuelvo a juzgar.
Tampoco vuelvo a juzgar @s11 (enmendado en el contrato) ni `deploy/nginx.conf` (carril propio),
por instrucción expresa.

**Un solo bloqueante, y es el mismo de la primera lectura, desplazado de eje:** la cláusula de
@s38 «ningún ancho recorta la url» sigue sin oráculo válido, y el cambio de producción que se
hizo para satisfacerla la incumple a 320 px con el texto al 200 %. Lo he medido, no lo deduzco.

Todo lo demás que se me pidió mirar con lupa **está bien y lo he verificado uno a uno**. El
carril ha mejorado mucho respecto al dictamen anterior y el reconocimiento del error de informe
es genuino y comprobable.

---

## Lo que he ejecutado yo (distinto de lo que cito)

| Qué | Dónde | Resultado |
| --- | --- | --- |
| `pnpm exec vitest run src/calendar.test.tsx src/calendar-feed-api.test.ts` | worktree `ow-worktrees/ics-calendar` @ `1a12881`, árbol limpio | **98/98 verdes** |
| El mismo, con el mutante `187:21` aplicado a mano (`retriable = true && failure !== "limit"`) | copia desechable, detached @ `1a12881`, fuera del repo | **2 fallos / 96 pasan**: fallan **exactamente** las dos nuevas |
| El mismo, con el mutante `249:25` (`spellCheck={true}`) | ídem | **98/98 verdes**: el mutante **sobrevive**, como él declara |
| `gradlew test --tests …CalendarApiTest --rerun --no-daemon` | worktree de la rama | **30/30 verdes** (29 antes + la fila `OPTIONS`) |
| Medición del recorte del campo de url en Chromium con la hoja real compilada (`sass src/styles.scss`) | página estática con el DOM de `calendar.tsx` | **recorte vertical a 320 px con texto al 200 %** (abajo) |
| `defaultBrowserType` / `browserName` de Playwright 1.63 | `node_modules/.pnpm/playwright@1.63.0/…/lib/index.js:187-188` | por defecto **`"chromium"`** |

No he ejecutado: `bin/harness init` (es la suite completa, prohibida en esta sesión), PIT,
Stryker, ni ningún E2E. Todo lo referido a Playwright y a la campaña de mutación lo **cito**.

---

## 1. El bloqueante: @s38 «ningún ancho recorta la url» — SIGUE SIN CUMPLIRSE

### Qué mide el oráculo

`e2e/ics-calendar-ux.spec.mjs:147-149`:

```js
fieldClipped: field ? field.scrollWidth > field.clientWidth + 1 : false,
```

Sólo el eje **horizontal**. Y `e2e/ics-calendar-ux.spec.mjs:171` lo afirma con el mensaje
«`${label} recorta la url`». No existe ninguna comprobación de `scrollHeight` frente a
`clientHeight` en el fichero.

### Por qué eso ya no basta

`a158cf2` sustituyó `<input type="text">` + `text-overflow: ellipsis` por
`<textarea rows={3}>` + `overflow-wrap: anywhere; white-space: pre-wrap`
(`frontend/src/calendar.tsx:245-252`, `frontend/src/styles.scss:413-430`). El recorte no
desapareció: **cambió de eje**. Con `rows={3}` la altura es fija; cuando la url necesita más de
tres líneas, sobra contenido por abajo y el campo scrollea por dentro. El eje al que se mudó el
fallo es justo el único que el oráculo no mira.

### La medida

Página estática con el DOM de la sección «Enlace recién creado» y la **hoja de estilos real
compilada** desde `frontend/src/styles.scss` de `1a12881`, en Chromium headless, url de 79
caracteres (`http://127.0.0.1:18092/calendar/<43>.ics`, la forma que produce la pila E2E),
aplicando el mismo `doubleText` del spec (`main, main *` × 2):

| Ancho / texto | `clientHeight` | `scrollHeight` | `fieldClipped` (lo que mide el test) | Recorte vertical |
| --- | ---: | ---: | --- | --- |
| 320 px / 100 % | 87 | 87 | false | **no** |
| **320 px / 200 %** | **153** | **325** | **false** | **SÍ — se ve el 47 %** |
| 768 px / 200 % | 153 | 153 | false | no |
| 1280 px / 200 % | 153 | 153 | false | no |

A 320 px con el texto al 200 % **más de la mitad de la url queda fuera del campo** y sólo se
alcanza haciendo scroll dentro de él o arrastrando el `resize: vertical`. La combinación
320 px + texto 200 % es literalmente una de las que nombra el `When` de @s38.

### Por qué esto bloquea, y no es formalismo

1. **Es la misma cláusula por la que rechacé la primera vez.** Entonces no tenía oráculo; ahora
   tiene uno que no puede fallar en el eje en el que el producto falla. El rasero es el mismo.
2. **Es la justificación declarada del cambio de producción.** El comentario de
   `frontend/src/calendar.tsx:243-244` dice «a 320 px y con el texto al 200 % la url se lee
   entera, sin recorte»; el mensaje de `a158cf2` dice «el escenario destapó un defecto … pasa a
   ser un campo que envuelve»; `progress/ux_ics_calendar.md:32-35` dice «a 320 px y con el texto
   doblado la url de 43 caracteres se lee entera». **Las tres afirmaciones son falsas en esa
   combinación**, y las tres se apoyan en una medida que no cubre el caso.
3. **Es exactamente el caso que ya rompió antes en este proyecto**: `progress/current.md`
   registra que el único fallo de CI de la feature 24 fue «@s41 texto 200 % a 320 px».
4. La cláusula importa de verdad aquí: la sección dice «Guárdalo ahora: no volverá a mostrarse».
   Una url secreta que no se puede leer entera de una vez no es un detalle cosmético.

**Alcance de mi medida, dicho con precisión:** es una réplica estática del DOM con la hoja real,
no la aplicación servida. Si la vista real quedara aún más estrecha (por algún contenedor
adicional), el resultado sería **peor**, no mejor. Lo accionable no depende del número exacto:
falta el oráculo, y con él la conclusión cambia.

---

## 2. Lo que se me pidió mirar con lupa: verificado, y correcto

### 2.1 El cuadre de la bitácora contra el informe del `mutation_tester` — CUADRA

`progress/tdd_ics_calendar.md:793-870` frente a `progress/mutation_ics_calendar_frontend_final.md`:

| Magnitud | Bitácora corregida | Informe del corredor | ¿Cuadra? |
| --- | --- | --- | --- |
| Cuadre de los 110 huecos | 71 cerrados + 12 equivalentes + 3 corredor + 24 vivos = **110** | «cerró 71, no 95»; 24 de los 95 vivos | **sí** |
| Supervivientes totales | — | 4 + 12 + 3 + 25 = **44** de 384 | **sí** |
| Los 25 del grupo 4 | 24 antiguos enumerados (10 en el cliente + 14 en la vista) + `249:25` nuevo | 25 | **sí** |
| Tras el ciclo 20 | **23** de aquellos 24 vivos | `187:21` muerto por las dos pruebas nuevas | **sí** |
| Huecos reales abiertos | **16** | 17 del grupo 4 menos `187:21` = **16** | **sí** |

Los 24 enumerados en la bitácora coinciden mutante a mutante con los del informe. La aritmética
sale.

**Un desliz menor, no bloqueante:** al contar los equivalentes del final la bitácora lista
**7** (`83:47`, `55:5`, `67:21`, `77:12`, `143:7`, `149:7`, `192:9`) y omite `300:15`, que su
propia tabla dos párrafos antes sí declara equivalente; y a cambio no suma el nuevo `249:25` al
lado de los huecos. Los dos deslices se compensan y el total de 16 sale bien por casualidad
aritmética. La descomposición correcta es: de los 24 antiguos, 8 equivalentes y 16 huecos, uno
de ellos (`187:21`) ya cerrado → 15, más `249:25` → **16**.

### 2.2 El cambio de producción de `a158cf2` — justificado en el motivo, **incompleto en el efecto**

- El motivo declarado es cierto: `text-overflow: ellipsis` recortaba la url y @s38 lo prohíbe.
- El cambio hace lo que dice **en horizontal**: `scrollWidth === clientWidth` en las cuatro
  medidas que hice.
- **No hace lo que dice en vertical** (§1). Ése es el efecto que nadie ha mirado.
- Efectos colaterales revisados, todos limpios: `readOnly`, `htmlFor`/`label` y
  `onFocus → select()` funcionan igual en `<textarea>`; el rol accesible sigue siendo `textbox`,
  que es lo que usan `calendar.test.tsx` y el propio spec; el selector de `styles.scss` pasó de
  `input[type="text"]` a `textarea` dentro de `.calendar-feed`, y no hay ningún otro `textarea`
  en esa vista, así que no arrastra a nadie; `min-height: 44px` conserva el objetivo de Fitts.
- **`249:25` es exactamente lo que él dice.** Aplicado a mano `spellCheck={true}`, las 98 pruebas
  siguen verdes: el mutante sobrevive. Y `grep -ri spellcheck` sobre `frontend/src/` y `e2e/`
  sólo encuentra la línea de producción: ninguna prueba lo afirma. Hueco real, menor, suyo, y
  bien declarado.

### 2.3 Las dos pruebas del «Reintentar» — SON LAS QUE DICE, y el razonamiento se sostiene

- La primera (`calendar.test.tsx:967-991`) recorre **cuatro** estados de camino feliz —recién
  cargada, con el enlace creado, con el archivo preparado y tras revocar— y en los cuatro exige
  `queryByRole("button", { name: "Reintentar" })` igual a `null`.
- La segunda (`:994-1017`) sujeta la cara complementaria: con un 503 exige el botón visible;
  luego devuelve el 201, pulsa «Reintentar», espera el campo de url y exige que el botón haya
  desaparecido. **Su razonamiento es correcto**: sin ella, la primera se satisfaría borrando el
  botón; con ella, borrarlo pone roja la segunda.
- **Su afirmación sobre el rojo la he reproducido yo.** Con
  `const retriable = true && failure !== "limit";` aplicado a mano en una copia desechable:
  `2 failed | 96 passed (98)`, y las dos que fallan son exactamente esas dos. Ninguna otra prueba
  del frontend ejercita `calendar.tsx` (`grep -l "calendario\|CalendarFeed"` sólo devuelve
  `calendar.test.tsx`), así que «ninguna otra» es cierto.

### 2.4 @s38: los siete estados y el zoom nativo — SÍ SE EJECUTAN

- **Siete estados, contados uno a uno** en `STATES` (`spec:117-125`) y provocados de verdad en
  `enter()`: `cargando` con la ruta retenida sin resolver y `role="status"` «Cargando…»;
  `fallo` con un 503 y `role="alert"` + «Reintentar»; `con enlace activo` con recarga y
  `toHaveCount(0)` sobre el campo, que es la distinción con `enlace recién creado` que yo eché
  en falta la vez pasada. 7 × 3 anchos = 21, y el spec lo remacha con
  `expect(evidence).toHaveLength(STATES.length * WIDTHS.length)`.
- **Texto al 200 % verificado elemento a elemento**: `doubleText` (`spec:176-190`) compara
  `after` contra `before * 2` con `toBeCloseTo` para cada nodo, y falla si la lista está vacía.
  No es un `zoom` de CSS disfrazado.
- **Cuatro modos** con `emulateMedia` fijando las tres preferencias a la vez —la nota de por qué
  (`spec:265-266`) es un hallazgo real y bien explicado—, con `color-contrast` desactivado sólo
  en `forced-colors`, comprobando `matchMedia` de verdad y `transitionDuration <= 0.01` en todo
  `main`.
- **Teclado real**: orden comparado con `toEqual` contra el orden del DOM, nombre accesible no
  vacío en cada parada, foco visible, salida hacia delante y `Shift+Tab` hacia atrás, y
  `Ctrl/Cmd+A` con `selectionStart` 0 y `selectionEnd` igual a la longitud. Nada que ver con el
  `Tab` suelto y el `toBeTruthy()` que rechacé.
- **El zoom nativo NO se salta en silencio.** `spec:464` usa
  `test.skip(browserName !== "chromium")`, **no** `project.name`. En Playwright 1.63
  `defaultBrowserType` vale `"chromium"` y `browserName` deriva de él
  (`playwright/lib/index.js:187-188`); `playwright.config.mjs` no declara `projects` ni
  `use.browserName`, luego `browserName === "chromium"` y el caso **corre**. La trampa del otro
  carril (sin `projects`, `project.name` es cadena vacía) no aplica aquí. Además el spec hace
  `chrome.tabs.setZoom` real y **afirma** el efecto: `expect(zoom).toBe(2)`, `devicePixelRatio`
  duplicado y `clientWidth <= 400`; si el zoom no se aplicara, el caso se pondría rojo en vez de
  pasar de largo.
- Y `scripts/e2e.mjs:75-84` invoca `playwright test` sin lista de ficheros, con
  `testDir: "./e2e"`: el spec nuevo entra en la ejecución por defecto, no queda huérfano.

Único matiz del oráculo de teclado, no bloqueante: «foco visible» se acepta con
`outline > 0 || boxShadow !== "none"` (`spec:419-421`). La segunda mitad de la disyunción
aceptaría una sombra permanente que no tuviera nada que ver con el foco. Hoy no ocurre —los
únicos `box-shadow` de la hoja son de tarjetas de otras vistas y el foco lo pinta
`:focus-visible { outline: 3px solid }`—, pero es una puerta que puede abrirse sola con el tiempo.

### 2.5 `progress/ux_ics_calendar.md` — CORRECTO

- **Las treinta filas están, en el mismo orden de `docs/ux-requirements.md:13-42`, sin saltarse
  ninguna.** Las comparé una a una.
- **Distingue lo medido de lo heurístico**, que era el punto: «Verificado por medición» /
  «Verificado geométricamente» frente a «Revisión heurística», «Priorización de diseño, no uso
  observado», «No aplicable» con motivo. Y lo dice en la cabecera de la tabla: «"Verificado" se
  limita al comportamiento o medición descritos en la fila».
- **No infiere cumplimiento desde axe**: lo declara en la línea 7 citando `AGENTS.md:51` y lo
  repite en los límites («axe no certifica accesibilidad … condición necesaria, no suficiente»).
- **Los límites no son una excusa a posteriori.** Van en sección propia antes de la matriz, que
  es donde se emiten los juicios, y siguen el orden del precedente `progress/ux_export_data.md`
  («Resultados y límites» → matriz). Están además nombrados con su coste: sin personas, sin
  lector de pantalla, un solo motor, sin cronometrar Doherty, sin 1440 ni 1920.
- Lo que **no** puedo acreditar: los números que la sección «Medidas concretas» atribuye a la
  ejecución (21/21, 12/12, 373 px, 889,8 px). No he corrido el E2E; los cito. Y su afirmación
  «Recorte de la url: `textarea.scrollWidth > clientWidth` es false en todas» es literalmente
  cierta y **la conclusión que extrae de ella —«la url se lee entera»— no lo es** (§1).

---

## Cobertura de escenarios (@s ↔ test)

@s1–@s37 quedan como en el dictamen anterior: verificados uno a uno leyendo cada prueba, con
@s26 y @s37 marcados `[~]` por composición y así declarados. No los repito. Lo que cambia:

- @s38 [ ] **cubierto salvo una cláusula.** Siete estados [x], 320/768/1280 [x], texto 200 % [x],
  zoom nativo [x], teclado en orden con nombre y foco visible [x], objetivo 44 × 44 medido a mano
  y ya no delegado en axe [x], selección íntegra con teclado [x], axe en los 42 cortes [x], cuatro
  modos [x], matriz de los treinta principios [x] — y **«ningún ancho recorta la url» [ ]**: el
  oráculo no mira el eje en el que el producto falla (§1).
- La observación menor del dictamen anterior sobre `RequestMethod.OPTIONS` está **cerrada**:
  `CalendarApiTest.s16_…` recorre ahora cinco verbos y el comentario explica por qué la quinta
  fila no está en el `Examples`. **30/30 verdes, ejecutado por mí.**

## Disciplina TDD

- **¿Producción sin test que la pida?** Una, menor y reconocida por él mismo: `spellCheck={false}`
  (`frontend/src/calendar.tsx:249`). Ninguna prueba la exige y el mutante `249:25` sobrevive; lo
  he confirmado ejecutando. No bloquea, pero es producción que nadie pidió en rojo.
- **¿Evidencia de Rojo→Verde→Refactor? SÍ**, y el ciclo 20 es el mejor del carril: el rojo sólo
  podía existir contra el mutante, lo aplicó a mano, pegó la salida, y yo la he reproducido con
  el mismo resultado.
- **Honestidad del informe: buena, y ésta es la parte que más pesa a su favor.** Se rectificó
  solo en tres frentes (95 → 71; «no toqué producción» → sí la tocó en `a158cf2`; la incoherencia
  de haber declarado `143:7`/`300:15` cerrados en vez de equivalentes) y nombra la causa
  metodológica exacta: «verifiqué a mano 25 mutantes y extrapolé al resto». Eso es precisamente
  lo que no hay que hacer, y decirlo por escrito vale más que el número corregido. El bloqueante
  de §1 es de la misma familia —una medida que no cubre el caso que dice cubrir—, y por eso
  conviene cerrarlo con la misma vara.

## Calidad (lente de artesano)

- `e2e/ics-calendar-ux.spec.mjs` está bien construido: `enter()` concentra los siete estados en
  un sitio y lanza `Error` ante un estado desconocido; `geometry()` y `audit()` separan medir de
  afirmar; cada `expect` lleva mensaje con la etiqueta del corte, que es lo que hace útil un
  fallo de CI; y `withoutFeedToken()` al principio de cada iteración evita que un estado herede
  el token del anterior. Los comentarios explican fuerzas (por qué `emulateMedia` fija las tres
  preferencias, por qué se desactiva `color-contrast`), no lo que hace el código.
- `frontend/src/calendar.tsx:248` — `rows={3}` es un número mágico sin nombre y es exactamente
  el que produce el defecto de §1. Un campo que debe mostrar entero un valor de longitud conocida
  no debería fijar su altura a ojo.
- `frontend/src/styles.scss:424` — `resize: vertical` en un campo de solo lectura deja el arreglo
  del recorte en manos de la persona. No es una solución, es una salida de emergencia.
- `frontend/src/calendar.tsx:196` — sigue el `<main id="proyectos">` en la vista de Calendario.
  Ya lo dije y sigue igual: no bloquea, pero el nombre miente.
- Arquitectura respetada; el cambio es de una sola capa (vista + hoja de estilos) y no toca
  dominio ni aplicación.

## Checkpoints

- **C1** [~] Ficheros y docs presentes. `bin/harness init` **no ejecutado** (suite completa,
  prohibida en esta sesión). Puerta pendiente para el cierre.
- **C2** [x] `feature_list.json` da la 26 como **`in_progress`** (la incoherencia que señalé está
  corregida) y `progress/current.md` describe la sesión activa.
- **C3** [x] Capas respetadas, sin logs sueltos ni TODOs.
- **C4** [~] Todo módulo tocado tiene prueba; aislamiento real. `bin/harness test` completo no
  ejecutado.
- **C5** [ ] Sin entrada de `progress/history.md` para esta segunda tanda del carril.
- **C6** [ ] **@s38 con una cláusula sin oráculo válido** (§1). El resto del mapa @s → prueba es
  correcto. Además, una línea de producción (`spellCheck={false}`) que ningún test pide.
- **C7** [~] Mutación **frontend PASS**, 340/384 = 88,54 %, salida 0, sobre `8527823` — `1a12881`
  sólo añade pruebas, así que el score sólo puede subir. La mutación de **backend sigue sin
  score** (`progress/mutation_ics_calendar_backend.md`): la calidad de las 97 pruebas de backend
  de este carril no está medida. Puerta abierta para el coordinador, no para mí.

---

## Cambios requeridos

1. **(Bloqueante) Cerrar de verdad «ningún ancho recorta la url».** Tres pasos, en este orden:
   1. **Primero el oráculo, en rojo.** Añadir a `geometry()` de
      `e2e/ics-calendar-ux.spec.mjs:128-153` la medida del eje que falta —
      `fieldClippedVertically: field.scrollHeight > field.clientHeight + 1` — y afirmarla en
      `audit()` junto a la horizontal, y también en el bloque de zoom nativo (`spec:534-538`).
      Debe ponerse **roja** hoy en el corte `enlace recién creado @ 320 px` del barrido de texto
      al 200 %.
   2. **Después el arreglo.** Que el campo muestre la url entera sin scroll interno en las 42
      combinaciones: quitar `rows={3}` y dejar que la altura la fije el contenido (por ejemplo
      `field-sizing: content` con un `min-height` de respaldo, o calcular las filas desde la
      longitud del valor). Lo que se elija, que lo exija la prueba, no el comentario.
   3. Corregir las tres afirmaciones que hoy son falsas: `frontend/src/calendar.tsx:243-244`, la
      descripción de `a158cf2` en `progress/tdd_ics_calendar.md` (sin reescribir historia) y
      `progress/ux_ics_calendar.md:32-35`.
2. **(No bloqueante) Cerrar `249:25` o retirar el atributo.** O una prueba que afirme que el
   campo de la url no lleva corrección ortográfica, o se quita `spellCheck={false}`. Hoy es
   producción que ningún rojo pidió; lo he confirmado ejecutando el mutante.
3. **(No bloqueante) Arreglar el desliz de cuentas** de `progress/tdd_ics_calendar.md:848-850`:
   los equivalentes de los 24 son **8** (falta `300:15`, que su propia tabla ya declara
   equivalente) y el nuevo `249:25` cuenta como hueco. El total de 16 no cambia; la
   descomposición sí.
4. **(No bloqueante) Endurecer «foco visible»** en `e2e/ics-calendar-ux.spec.mjs:419-421`:
   exigir que el indicador aparezca **con** el foco y no antes (comparar contra el estilo del
   mismo elemento sin foco), en vez de aceptar cualquier `box-shadow` presente.

## Para el coordinador

- La rama **sigue sin integrar** y va cuatro commits por detrás de `origin/main`. El PASS de
  mutación acredita `8527823`, no `main`: al integrar hay que comprobar que `calendar.tsx` y
  `calendar-feed-api.ts` llegan idénticos, como avisa el propio informe del corredor.
- **La mutación de backend de esta feature sigue sin producir score.** Es puerta pendiente y no
  la he tocado.
- `bin/harness init` sigue sin ejecutarse por la disciplina de recursos de la sesión.
- Los **16 huecos reales** que quedan tras el ciclo 20 están enumerados con su motivo en
  `progress/tdd_ics_calendar.md:818-850`. El umbral está superado con 33 mutantes de margen; no
  propongo apretar más ahora, pero el más caro de dejar abierto es el grupo del `Content-Length`
  (`88:7` ×2, `88:21` ×2, `88:53`), donde el propio artesano demuestra que la guarda entera se
  puede borrar sin que ninguna de sus seis filas proteste.

## Qué NO he verificado

- **Ningún E2E.** Las 8 pruebas Playwright que el artesano declara verdes las cito; no las he
  corrido. Mi medida de §1 es una réplica estática con la hoja real, no la pila levantada.
- **Ni PIT ni Stryker.** El PASS de 340/384 lo cito del `mutation_tester`.
- `bin/harness init`, la suite completa de backend y la suite completa de frontend.
- No he editado código, pruebas, `.feature` ni `feature_list.json`. Los dos mutantes que
  reproduje a mano vivieron y murieron en una copia desechable fuera del repositorio, ya
  eliminada; el árbol de la rama sigue limpio en `1a12881`.
