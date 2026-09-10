# @s38 en navegador — las dos pantallas de la feature 29

Cierre de la condición **C2** del dictamen (`progress/judge_additional_connectors.md:493`),
la única de las siete que seguía abierta.

Rama `claude/conectores-a11y`, worktree `C:/Users/vhurt/ow-worktrees/conectores-a11y`.
Pantallas revisadas: el catálogo en `/conectores` (`frontend/src/connectors-catalog.tsx`) y
el conector de GitLab en `/conectores/gitlab` (`frontend/src/gitlab-connector.tsx`).

Antes de esto **no existía ningún spec de Playwright para estas dos pantallas**: el
escenario `@s38` estaba declarado y no verificado. Los dos ficheros nuevos son:

| Fichero | Qué mide |
| --- | --- |
| `e2e/additional-connectors-ux.spec.mjs` | anchos, texto al 200 %, 44 × 44, recorte, teclado, foco por cambio de estado, axe |
| `e2e/additional-connectors-native-zoom.spec.mjs` | zoom **nativo** al 200 % en los tres anchos |
| `e2e/support/connectors-fixture.mjs` | cotas del contrato y API simulada, compartidas por los dos |

Playwright los recoge solo: `playwright.config.mjs` declara `testDir: "./e2e"` y el patrón
`*.spec.mjs`, y `scripts/e2e.mjs` invoca `playwright test` sin lista de ficheros. **No hace
falta registrarlos en ningún sitio.** Tampoco se ha tocado `scripts/project.mjs`: su lista
de `lint` es curada —`webhooks-ux.spec.mjs` y `external-calendar-ux-audit.spec.mjs`, las dos
revisiones hermanas, tampoco están en ella—, así que añadir estos dos habría sido cambiar el
criterio de esa lista, no seguirlo. Comprobado igualmente:
`node --test scripts/project.test.mjs` → **95/95**.

Corrida final contra el arnés de verdad, no contra un servidor de desarrollo:

```
E2E_WEB_PORT=18105 node scripts/e2e.mjs \
  e2e/additional-connectors-ux.spec.mjs e2e/additional-connectors-native-zoom.spec.mjs
  ...
  7 passed (47.5s)
```

Pila completa (PostgreSQL, backend y `web` de producción), puerto 18105, retirada al
terminar. `pnpm --dir frontend test` → 3130/3130. `tsc --noEmit` y `pnpm --dir frontend lint`
limpios.

---

## 1. Las siete cosas que pedía la condición

| # | Lo que pedía C2 | Dónde se afirma | Estado |
| --- | --- | --- | --- |
| 1 | Matriz de anchos 320 / 768 / 1280 px | `additional-connectors-ux`, bucle `WIDTHS` | **cubierto** |
| 2 | Texto al 200 % | ídem, modo `text200` | **cubierto** |
| 3 | Zoom nativo al 200 % | `additional-connectors-native-zoom` | **cubierto** |
| 4 | Objetivos táctiles de 44 × 44 | los dos specs, bucle `controls` | **cubierto** |
| 5 | Recorrido de teclado con foco visible **en cada parada** | `additional-connectors-ux`, dos pruebas de teclado | **cubierto** |
| 6 | axe en los tres anchos, sin `serious` ni `critical` | los dos specs, dentro del bucle de anchos | **cubierto, y más estricto** |
| 7 | Nota de límites (última línea del escenario) | sección 4 de este documento | **cubierto** |

Sobre el 6: el escenario dice «axe no reporta violaciones», no «ninguna `serious` ni
`critical`». Lo que se afirma es lo del escenario, que es lo más duro:
`expect(axe.violations).toEqual([])` con las etiquetas `wcag2a`, `wcag2aa`, `wcag21aa` y
`wcag22aa`, **de cualquier impacto**, en cada uno de los tres anchos y en cada estado. Hoy
salen cero.

Y **axe corre en los tres anchos, no en uno**. Es una diferencia real con
`e2e/webhooks-ux.spec.mjs`, que sólo lo pasa a 320 px: una pasada única no dice nada del
reflujo de 768 ni del de 1280.

### Los estados que se recorren

`/conectores` — dos: **catálogo** (las seis filas, con las cuatro variantes de estado,
una fila con `lastError`, otra sin actividad) y **catálogo ilegible** (el `role="alert"`
de `STORAGE_UNAVAILABLE`).

`/conectores/gitlab` — cinco: **conectado**, **recibo truncado** (el Given literal:
`truncated: true`, con sus cuatro cifras y el aviso de «quedaron issues sin traer»),
**confirmando la desconexión**, **desconectado** y **error al conectar**.

Cada estado × tres anchos × tres modalidades (claro, oscuro, texto al 200 %) = 63
mediciones de geometría y 63 pasadas de axe por corrida, más 6 bajo zoom nativo.

---

## 2. El rojo acreditado de cada aserción

Regla: una aserción que nunca ha fallado no demuestra nada. De las nueve de abajo, **tres
salieron rojas solas** —son los defectos reales de la sección 3— y las otras seis se
rompieron a propósito, se vio el rojo y se deshizo el cambio. Salvo la última, todas las
roturas son en **producción** (`styles.scss`, `connectors-catalog.tsx`,
`gitlab-connector.tsx`), no en la prueba. Cada corrida reconstruyó la imagen `web` de
verdad, así que el rojo es el del producto compilado.

Los volcados completos están en `.e2e-work/a11y-log/` (`rojo-01.txt`, `B1`…`B8`).

### R1 — Anchos: desbordamiento horizontal · **rojo provocado**

Rotura: `.connectors-catalog { min-width: 420px }` en lugar de `0`.

```
✘ conectores: catálogo y GitLab en los tres anchos con light @s38
  Error: catálogo @ 320px (light): desbordamiento horizontal; culpables=[
    {"tag":"MAIN","className":"connectors-catalog","right":420,...},
    {"tag":"H1",...},{"tag":"UL",...},{"tag":"LI",...}]
  Expected: <= 320
  Received:    420
```

El mensaje nombra a los culpables; no dice sólo «por cuánto», dice «quién».

### R2 — Texto al 200 % · **rojo real primero, luego provocado**

Rojo real de la primera corrida, antes de tocar nada
(`.e2e-work/a11y-log/rojo-01.txt:58`):

```
✘ ... con text200 @s38
  Error: catálogo @ 320px (text200): desbordamiento horizontal
  Expected: <= 320
  Received:    452
```

Rotura posterior para acreditar la aserción una vez arreglada:
`grid-template-columns: fit-content(40%)` → `auto`.

```
✓ ... con light @s38        (9.4s)
✓ ... con dark @s38         (9.8s)
✘ ... con text200 @s38      (2.5s)
  Error: conectado @ 320px (text200): desbordamiento horizontal; culpables=[
    ...,{"tag":"DL","right":305,"scrollWidth":318,"clientWidth":290,"overflowing":true,
        "text":"EstadoConectadoProyectoplataforma-de-int"}]
  Expected: <= 320
  Received:    333
```

Fíjese en que **claro y oscuro pasan y sólo `text200` falla**: la modalidad de texto
ampliado no es decorado, mide algo que los tres anchos por sí solos no ven.

### R3 — Objetivos de 44 × 44 · **rojo real primero, luego provocado**

Rojo real de la primera corrida: los enlaces del catálogo median 21 px de alto.

```
Error: catálogo @ 320px (light): «API para integraciones» alto del objetivo
Expected: >= 44
Received:    21
```

Rotura posterior: `.connectors-catalog a { min-height: 20px }`. Los **dos** specs se
ponen rojos, incluido el de zoom:

```
✘ @s38 las dos pantallas en los tres anchos al 200 % de zoom nativo
  Error: catalogo @ 320 px CSS al 200 %: «API para integraciones» alto del objetivo
  Expected: >= 44
  Received:    21.33333396911621
✘ ... con light @s38     Received: 21
✘ ... con dark @s38      Received: 21
✘ ... con text200 @s38   Received: 43
```

El `21.33` del primero es la medida bajo zoom real; el `43` del último es el caso
interesante: con el texto al doble el enlace crece por contenido y se queda *justo* por
debajo del umbral. Una aserción de `>= 40` habría dejado pasar eso.

### R4 — Recorte de las acciones y de las cifras del recibo · **rojo provocado**

Rotura: `.gitlab-connector dd { overflow: hidden; max-height: 12px }`. Esto es exactamente
lo que el escenario prohíbe: «cifras del recibo recortadas».

```
✘ @s38 ... zoom nativo | ✘ light | ✘ dark | ✘ text200
  Error: gitlab @ 320 px CSS al 200 %: contenido recortado
  - Array []
  + Array [
  +   { "what":"DD","text":"Conectado","clientHeight":12,"scrollHeight":21 },
  +   { "what":"DD","text":"plataforma-de-integraciones/servicios-co",
  +     "clientHeight":12,"scrollHeight":107 },
  +   { "what":"DD","text":"90210","clientHeight":12,"scrollHeight":21 },
  +   { "what":"DD","text":"••••9f4c","clientHeight":12,"scrollHeight":21 }, ... ]
```

Esta aserción **no la cubre** la del desbordamiento de página: un elemento que recorta no
ensancha la página, precisamente porque se recorta. Se miden los dos ejes: un oráculo que
sólo mirase la horizontal dejaría pasar el recorte vertical de las celdas apiladas.

### R5 — axe · **rojo provocado**

Rotura: quitar `aria-label={status}` del `<span role="img">` que pinta el glifo de estado
en `connectors-catalog.tsx`.

```
✘ @s38 ... zoom nativo | ✘ light | ✘ dark | ✘ text200
  Error: catálogo @ 320px (light): axe
  + "id": "role-img-alt", "impact": "serious"
```

`serious`, en los tres anchos y también bajo zoom.

### R6 — Teclado: ningún control fuera de la secuencia · **rojo provocado**

Rotura: `tabIndex={-1}` en el botón «Desconectar».

```
✘ conectores: el teclado recorre gitlab ... con foco visible en cada parada @s38
  Error: expect(received).toEqual(expected)
    Array [ "Conectores","Proyecto de destino","Importar issues","Actualizar token",
  -   "Desconectar" ]
  427 | expect(controls.reachable).toEqual(controls.visible);
```

Por esto la prueba deriva **dos** listas del documento y no una: si sólo derivase la de los
tabulables, un `tabindex="-1"` sacaría al botón a la vez de la expectativa y del recorrido y
la prueba seguiría verde con un control inalcanzable.

### R7 — Teclado: el ORDEN, no sólo el conjunto · **rojo provocado**

Rotura: `tabIndex={1}` en «Actualizar token», que lo saca de su sitio en la secuencia.

```
✘ ... el teclado recorre gitlab ... @s38
    Array [ "Conectores","Proyecto de destino","Importar issues",
  -   "Actualizar token",
      "Desconectar",
  +   "Actualizar token" ]
  439 | expect(forward.seen).toEqual(expected);
```

Nota honesta: el **primer** intento de esta rotura puso `tabIndex={1}` en «Desconectar» y
**la prueba siguió verde**. No es un agujero del oráculo: «Desconectar» es el último control
del DOM, y adelantarlo al grupo de tabindex positivos hace que se alcance igualmente el
último cuando se arranca desde `main`. La rotura se rehízo sobre un control intermedio, que
es donde la reordenación sí cambia la secuencia observable. Queda anotado porque una rotura
que no enrojece hay que explicarla, no esconderla.

### R8 — Teclado: foco visible en CADA parada · **rojo provocado**

Rotura: `.gitlab-connector button:focus-visible, .connectors-catalog a:focus-visible {
outline: none }`.

```
✘ el teclado recorre catálogo ... @s38
✘ el teclado recorre gitlab ... @s38
  - Array []
  + Array [
  +   { "name":"API para integraciones","matchesFocusVisible":true,
  +     "outlineStyle":"none","outlineWidth":3,"outlineColor":"rgb(36, 76, 60)" },
  +   { "name":"Webhooks", ... }, { "name":"Calendario", ... }, ... 50 líneas ]
```

Se mide **una vez por parada**, no una al final: un anillo apagado en el primer control
tiene que doler. Y se exige el anillo del **producto** (`outline-style: solid`, `>= 3px`, no
transparente), no el del agente de usuario, que Chromium computa con `outline-style: auto` y
que un `outline: none` del producto no apagaría. La rotura de arriba lo demuestra: el
`outlineWidth` sigue diciendo `3` y el color sigue siendo el verde de la marca; lo único que
cambia es `outlineStyle: none`. Un oráculo que sólo mirase el grosor habría pasado.

### R9 — Zoom nativo: que de verdad amplía · **rojo provocado en la prueba**

La única rotura que **no** es de producción, y va marcada como tal. Se cambió
`chrome.tabs.setZoom(tab.id, 2)` por `setZoom(tab.id, 1)`:

```
✘ @s38 las dos pantallas en los tres anchos al 200 % de zoom nativo
  Error: catalogo: el zoom nativo no llegó al 200 %
  Expected: 2
  Received: 1
```

Sirve para responder a la pregunta que importa: *¿esta prueba está midiendo de verdad al
200 %, o dice que sí y mide al 100 %?* La evidencia de la corrida verde
(`.e2e-work/additional-connectors-zoom/*/evidence.json`) lo confirma por otro lado:
`devicePixelRatio` pasa de 1.5 —esta máquina escala la pantalla al 150 %— a **3.0** en las
seis mediciones, y `innerWidth` llega exactamente a 320, 768 y 1280 px **CSS**, lo que
obliga a abrir ventanas de 640, 1536 y 2560 px de ancho más el cromo.

---

## 3. Los tres defectos de accesibilidad reales

Ninguno se encontró leyendo el código. Los tres los destapó la ejecución, y los tres tienen
su rojo delante (secciones R2, R3 y el de abajo).

### D1 — Las dos pantallas no tenían estilos propios

`frontend/src/styles.scss` tenía bloque para `.github-connector`, `.external-calendar`,
`.automations`, `.webhooks`… y **ninguno** para `.connectors-catalog` ni para
`.gitlab-connector`. Los componentes ponían la clase y nadie la escuchaba, así que a esas dos
pantallas no les alcanzaba ninguna regla de control del área de trabajo:

- los seis enlaces del catálogo medían **21 px de alto** contra los 44 que exige el
  escenario (y `docs/ux-requirements.md`);
- nada tenía `overflow-wrap: anywhere`, así que a 320 px con el texto al 200 % el `h1`
  «Conectores» —una sola palabra a 64 px— no podía partirse y desbordaba la página hasta
  **452 px**.

Arreglo: dos bloques nuevos, hermanos del `.github-connector` que ya existía y copiados de
él salvo donde hacía falta más (abajo). El comentario del código dice por qué existen.

Es literalmente el mismo defecto que la auditoría de la feature 30 encontró en
`/automatizaciones` (`progress/ux_automations.md`, «La página no tenía estilos»). Tercera
vez que este repositorio publica una pantalla nueva sin bloque de estilos y la revisión de
UX es lo único que lo ve.

### D2 — La lista de definición de la conexión daba 0 px al valor

Ya con estilos, a 320 px y texto al 200 % la página seguía desbordando 13 px. El culpable
era la propia rejilla:

```
getComputedStyle(dl).gridTemplateColumns === "274px 0px"
```

Con `grid-template-columns: auto minmax(0, 1fr)` —que es lo que usa `.github-connector`— la
primera columna se lleva todo su `max-content` antes de que el `1fr` reparta nada. La
etiqueta «Identificador del proyecto» pedía 274 px de los 290 disponibles, la segunda pista
se quedaba en **0 px** y el valor —la ruta del proyecto, el identificador, la pista del
token— se salía por la derecha.

Arreglo: `grid-template-columns: fit-content(40%) minmax(0, 1fr)`. La etiqueta no puede
pasar del 40 % del ancho, pase lo que pase con el tamaño del texto.

Aviso para quien mantenga esto: **`.github-connector` sigue con `auto 1fr`**
(`styles.scss:2287`) y su pantalla tiene la misma lista de definición. No se ha tocado
porque pertenece a la feature 27 y no a este carril, y porque su `@s42` no está en este
contrato; pero es muy probablemente el mismo defecto sin descubrir. Queda anotado como
sospecha medida en la pantalla hermana, no como hallazgo verificado en la suya.

### D3 — Al importar (y al guardar), el foco se caía al `body`

Éste es el hallazgo más serio de los tres, y el que el escenario nombra de frente: «al
cambiar de estado … el foco pasa al h1 o al aviso de resultado».

`gitlab-connector.tsx` marca `disabled={importing}` en el botón «Importar issues» y
`disabled={connecting}` en el de «Conectar». Un elemento que se deshabilita **pierde el
foco**, y el navegador lo devuelve a `document.body`. Resultado: quien pulsa con teclado o
con lector de pantalla se queda sin punto de foco justo en el instante en que empieza la
operación, y para enterarse de qué pasa tiene que volver a recorrer la página.

Rojo, con la respuesta retrasada a propósito para poder mirar el estado intermedio:

```
✘ conectores: en cada cambio de estado el foco pasa al h1 o al aviso de resultado @s38
  Object {
    "conectado": "h1",
    "desconectado": "h1",
  -  "importando": "aviso",
  +  "importando": "body",
    "recibo": "aviso",
  }
```

y, en el ciclo siguiente, el mismo defecto en el otro botón:

```
  -  "guardando": "aviso",
  +  "guardando": "body",
```

Arreglo: el `<p role="status" aria-live="polite">` —que ya existía y ya decía «Importando
issues. Esto puede tardar un poco…» y «Guardando…»— recibe `tabIndex={-1}` y el foco cuando
la operación arranca. Es el «aviso de resultado» que el escenario permite como destino.

Contrapartida asumida, escrita también en el código: un lector de pantalla anunciará ese
texto **dos veces**, una por la región viva y otra por el foco. Es mucho menos malo que no
anunciar nada y dejar el foco en el `body`. La alternativa —cambiar `disabled` por
`aria-disabled`— habría sido un cambio de comportamiento mayor y habría roto los oráculos
unitarios que hoy afirman `toBeDisabled`; no procede desde este carril.

Nota de alcance sobre «guardando»: los cuatro estados que el paréntesis del escenario
enumera son *conectado, importando, recibo, desconectado*. «Guardando» **no** está entre
ellos. Se ha medido y arreglado igualmente porque es un cambio de estado de la misma
pantalla con el mismo defecto y el mismo arreglo de una línea. Va declarado aquí como
ampliación, no como cumplimiento de una fila del contrato.

---

## 4. La nota de límites que pide la última línea del escenario

> «la evidencia registra los límites humanos y de dispositivos sin inferir cumplimiento
> universal desde axe» — `features/additional_connectors.feature:486`

Esto es lo que **no** está demostrado, dicho antes de que nadie lo pregunte.

1. **axe no declara cumplimiento.** Cero violaciones automáticas no es WCAG 2.2 AA. Las
   herramientas automáticas cubren, según su propia documentación, en torno a un tercio de
   los criterios; el resto —orden de lectura con sentido, calidad de los nombres, si el
   texto de un error dice de verdad qué hacer— es juicio humano. Lo de aquí es **una
   revisión heurística más unas medidas**, no una auditoría de conformidad.

2. **Un solo motor y un solo navegador.** Todo se ha medido en **Chromium** sobre Windows.
   No hay una sola medida en Firefox ni en WebKit. El zoom nativo, en particular, sólo se
   sabe hacer en Chromium con el mecanismo de la extensión: para los otros dos motores no
   se afirma nada, ni siquiera reflujo.

3. **Ningún dispositivo real.** Ni un móvil, ni una tableta, ni un lector de pantalla de
   verdad. Los 44 × 44 son **píxeles CSS medidos en un navegador de escritorio**, no un
   dedo sobre un cristal: no dicen nada de la precisión real del toque, del alcance del
   pulgar ni del teclado virtual tapando medio formulario. Los anuncios `aria-live` se
   afirman por el DOM (`role="status"` presente, con el texto correcto): **nadie ha
   escuchado a NVDA, JAWS ni VoiceOver leerlos**, y la doble lectura de D3 es una
   predicción razonada, no una observación.

4. **El texto al 200 % es zoom de texto, no de disposición.** Se dobla el `font-size`
   calculado de cada elemento y se comprueba elemento a elemento que el resultado es
   exactamente el doble. No es lo mismo que la ampliación de texto de un sistema operativo,
   que además cambia métricas de fuente y espaciados. Es la misma técnica que el resto de
   auditorías de este repositorio, con sus mismas limitaciones.

5. **`forced-colors` y `prefers-reduced-motion` no se miden aquí.** El escenario `@s38` no
   los pide y no se han añadido. Ninguna de las dos pantallas tiene animación propia, pero
   eso **no está afirmado por ninguna prueba**.

6. **Alturas y orientaciones: sólo una.** Todo se mide a 900 px de alto en la revisión de
   anchos. Ni pantallas cortas, ni apaisado de móvil, ni ventanas de 400 px de alto.

7. **La API está simulada.** Las dos pantallas son las de verdad, servidas por la imagen
   `web` de producción y pintadas por un navegador de verdad; lo que se finge son las
   respuestas HTTP. No es una comodidad: el Given exige «GitLab connected y un recibo
   reciente `truncated: true`», y la pila de E2E **no levanta ningún GitLab falso** —
   `docker-compose.yml` sólo declara `github-fake`, y `APP_GITLAB_API_BASE` ni siquiera
   existe en `scripts/e2e.mjs`—, así que por la interfaz real ese estado es inalcanzable.
   Consecuencia honesta: esta revisión **no** verifica el comportamiento del backend, ni
   latencias, ni qué pinta la pantalla ante una respuesta que el servidor real dé y esta
   simulación no contemple. La aceptación del backend la cubre la suite JVM.

8. **Sin medición de tiempos.** No hay nada aquí sobre los 400 ms de Doherty. Lo único
   verificado en esa dirección es que la pantalla muestra estado de trabajo **antes** de la
   respuesta y no anticipa éxito.

9. **Los estados auditados son siete, no todos.** Dos del catálogo y cinco de GitLab. Se
   quedan fuera, por ejemplo, el estado «conectores deshabilitados» (que exige recrear el
   backend sin `APP_CONNECTOR_KEY`, como hace `e2e/support/connector.mjs`) y el formulario
   de «Actualizar token» con la conexión en `error`. No están medidos y no se afirma nada
   de ellos.

10. **Nada de esto es un estudio con usuarios.** Las valoraciones de jerarquía, claridad y
    carga cognitiva de estas dos pantallas siguen sin hacerse.

---

## 5. Mapa `@s` → prueba

| Cláusula de `@s38` | Prueba |
| --- | --- |
| «a 320, 768 y 1280 px CSS … no hay desbordamiento horizontal» | `additional-connectors-ux`, `expect(measured.scroll).toBeLessThanOrEqual(width)` |
| «… con texto al 200 %» | ídem, modalidad `text200`, con verificación previa de que el navegador aplicó el doble |
| «… y con zoom nativo 200 %» | `additional-connectors-native-zoom`, `chrome.tabs.setZoom` + `devicePixelRatio` doblado |
| «ni acciones o cifras del recibo recortadas» | los dos specs, `expect(measured.clipped).toEqual([])` sobre conjunto nombrado y en los dos ejes |
| «todos los controles … miden al menos 44 × 44 px CSS» | los dos specs, bucle `controls` |
| «… son alcanzables por teclado en orden lógico» | `additional-connectors-ux`, dos pruebas de teclado: `reachable == visible`, ida `== expected`, vuelta `== inverso rotado` |
| «… y muestran foco visible» | ídem, `forward.invisible` y `backwards.invisible` vacíos, medido parada a parada |
| «al cambiar de estado … el foco pasa al h1 o al aviso de resultado» | `additional-connectors-ux`, prueba de foco, cuatro estados del contrato más «guardando» |
| «axe no reporta violaciones en ninguna de las dos pantallas en ninguno de los tres anchos» | los dos specs, `expect(axe.violations).toEqual([])` dentro del bucle de anchos |
| «la evidencia registra los límites … sin inferir cumplimiento universal desde axe» | sección 4 de este documento |

---

## 6. Lo que este carril NO ha hecho

- **No** se ha marcado `@s38` como cerrado en `features/additional_connectors.feature` ni se
  ha tocado `feature_list.json`: eso lo decide el `craftsman_lead` con el `judge`.
- **No** se han lanzado campañas de PIT ni de Stryker.
- **No** se ha hecho `push`, ni `fetch`, ni `rebase`, ni ningún `reset`. La rama
  `claude/conectores-a11y` sólo tiene commits añadidos sobre el punto del que partía.
- **No** se ha entrado en el worktree `additional-connectors`.
- **No** se ha tocado `scripts/project.mjs`, por el motivo de la cabecera.
- Las otras seis condiciones del dictamen (C1, C3…C7) siguen como estaban: este carril sólo
  cierra C2.
