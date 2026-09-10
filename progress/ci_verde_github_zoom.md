# CI verde: `@s42 github-connector-native-zoom`

Carril `claude/ci-verde`, worktree `C:/Users/vhurt/ow-worktrees/ci-verde`, `E2E_WEB_PORT=18106`.
Encargo: la única prueba que separaba a `main` de una CI verde (run `34466099879`: 223 pasan,
1 falla, 3 se saltan).

---

## 1. Veredicto

**Son dos defectos, los dos de la prueba / del andamiaje de E2E. El producto no tiene culpa
de ninguno.** El segundo estaba tapado por el primero: la prueba moría antes de llegar a él.

| # | Defecto | Dónde | Estado |
|---|---------|-------|--------|
| 1 | `restartBackend` deja al servicio falso de GitHub fuera de la red del backend | `e2e/support/backend.mjs` | reproducido y arreglado |
| 2 | El barrido de anchos no filtra los que no caben en la pantalla | `e2e/github-connector-native-zoom.spec.mjs` | reproducido y arreglado |

---

## 2. Defecto 1 — la causa real del fallo de CI

### Qué pasa

`docker-compose.yml` monta el servicio falso de GitHub **dentro** del espacio de red del backend:

```yaml
  github-fake:
    build: ./e2e/fake-github
    profiles: [e2e]
    network_mode: "service:backend"
```

Eso es deliberado y es carga útil: la lista blanca de `GithubApiBase` sólo admite api.github.com
o *loopback*, así que el falso tiene que verse desde el backend como `http://127.0.0.1:9000`.

`e2e/support/backend.mjs` reiniciaba **sólo** el backend:

```js
  compose("restart", "backend");
```

Al reiniciarse el backend, Docker le crea un espacio de red nuevo. El contenedor del falso, que se
unió al viejo, se queda hablando con un espacio muerto: **desde ese instante el backend ya no
alcanza `127.0.0.1:9000`**. Conectar el conector de GitHub verifica repositorio y cuenta contra
GitHub *antes* de guardar nada (`HttpGithubIssueSource.verify`), así que el `PUT` pasa a responder
`503 GITHUB_UNAVAILABLE` para siempre.

`restartBackend` lo usan **siete** specs. Dos de ellas —`appearance-persistence` y
`customization-persistence`— corren alfabéticamente **antes** que el conector. La avería viajaba
desde ahí hasta la primera prueba del conector que corriera después, sin que nadie supiera de dónde
venía.

### Por qué falla exactamente esa prueba y ninguna otra

Orden alfabético de Playwright: `-` (0x2D) ordena antes que `.` (0x2E), así que
`github-connector-native-zoom.spec.mjs` corre **antes** que `github-connector.spec.mjs`.
Y la primera prueba de `github-connector.spec.mjs` es la del estado deshabilitado, que llama a
`withConnectorDisabled` → `up -d --force-recreate backend github-fake`, es decir, **repara** la
avería sin saberlo (su propio comentario ya declara el acoplamiento: «El servicio falso comparte el
espacio de red del backend, así que se recrea con él en los dos sentidos»).

Resultado: la prueba de zoom nativo era la **única** que se comía el falso roto. Determinista, no
flake.

### Evidencia — reinicio del backend en el log de CI

```
10:45:07.83  Container organizationweb-e2e-36004-backend-1  Restarting
10:49:14.18  Container organizationweb-e2e-36004-backend-1  Restarting
10:49:50.21  Container organizationweb-e2e-36004-backend-1  Restarting
...
10:53:09.15  ✘  121 e2e/github-connector-native-zoom.spec.mjs:37:1 › @s42 ... (6.7s)
10:53:21.61  Container organizationweb-e2e-36004-backend-1  Recreated      <- withConnectorDisabled
10:53:21.61  Container organizationweb-e2e-36004-github-fake-1  Recreated  <- repara el falso
10:53:43.48  ✓  122 e2e/github-connector.spec.mjs:132:1 › @s42 estado deshabilitado ...
10:53:45.10  ✓  123 e2e/github-connector.spec.mjs:162:1 › @s42 estado sin conexión ...
```

Tres reinicios antes del fallo; la reparación llega **después** del fallo, y a partir de ahí las
16 pruebas del conector pasan. Encaja al milímetro.

### Evidencia — reproducción directa con la pila levantada

Sonda desechable contra la pila real (`E2E_WEB_PORT=18106`), conectando por API antes y después de
`restartBackend`:

```
[ANTES]   [200, {"repository":"octocat/hello-world","login":"octocat","status":"valid", ...}]
[DESPUES] [503, {"type":"urn:organization:problem:github_unavailable",
                 "title":"GitHub no responde. Inténtalo más tarde.",
                 "status":503,"code":"GITHUB_UNAVAILABLE"}]
```

La respuesta del POST/PUT de conectar que se pedía: **503 `GITHUB_UNAVAILABLE`**.
`frontend/src/github-connector.tsx` traduce ese código a «GitHub no responde. Inténtalo más tarde»,
deja el formulario en pantalla y no pinta nunca el `<dd>` con «Conectada». De ahí el
`element(s) not found` de CI.

### Hipótesis descartadas por medición, no por intuición

- **No es lentitud del arranque en frío del conector.** Medida de la latencia del `PUT` de conectar
  en una pila recién levantada, seis veces seguidas:
  `[[200,143],[200,26],[200,30],[200,23],[200,24],[200,25]]` ms.
  La primera llamada de la vida del backend tarda **143 ms**, no 5 s. El presupuesto de 5 s del
  `expect` no tenía nada que ver.
- **No es el contexto persistente ni el login.** La prueba pasa en aislamiento contra una pila
  limpia, con el `PUT` devolviendo 200 y la importación 201. Cookies y CSRF del
  `launchPersistentContext` funcionan.
- **No es CSRF, ni sesión, ni carrera de React.** El registro de red de la ejecución en aislamiento
  muestra la secuencia completa correcta.

### El arreglo

`e2e/support/backend.mjs`: tras reiniciar el backend, se reinicia también lo que vive dentro de su
espacio de red. Se añade `--profile e2e` para que el falso sea direccionable, y se salta el paso
si la pila se levantó sin ese perfil (no hay nada que reiniciar).

```js
  compose("restart", "backend");
  const fakeId = compose("ps", "-q", "github-fake");
  if (fakeId) compose("restart", "github-fake");
```

De paso, el plazo de espera deja de ser un `45_000` a mano y pasa a ser
`RESTART_READY_TIMEOUT_MS`, exportado para que quien lo use derive su presupuesto en vez de
inventarlo (mismo criterio que `COMPOSE_TIMEOUT_MS` en `support/connector.mjs`).

### El rojo acreditado

Prueba nueva y **permanente** en `e2e/github-connector.spec.mjs`, que fija la invariante donde se
ve —la pantalla— y mira además el estado del `PUT`:

```
@s42 conectar sobrevive al reinicio del backend que hacen otras pruebas
```

Antes del arreglo:

```
✘  1 e2e\github-connector.spec.mjs:211:1 › @s42 conectar sobrevive al reinicio del backend ... (8.6s)
   Error: expect(received).toBe(expected)
   Expected: 200
   Received: 503
   > 230 |   expect((await connecting).status()).toBe(200);
```

Después del arreglo:

```
✓  1 e2e\github-connector.spec.mjs:211:1 › @s42 conectar sobrevive al reinicio del backend ... (8.8s)
```

### Qué pasa a cubrir

La prueba nueva mira `expect((await connecting).status()).toBe(200)` **antes** de mirar la pantalla.
Eso es deliberado: cuando esto se rompió en CI, la única pista fue «no encuentro el texto
Conectada», que no dice nada de quién falló. Ahora un fallo de la misma familia dirá «esperaba 200,
recibí 503» y señalará al servidor. Es el mismo idioma que ya usa
`e2e/external-calendar.spec.mjs` con `page.waitForResponse`.

**La prueba de zoom no se ha tocado en este punto**: era un verdadero positivo, informaba con razón
de que la pantalla nunca llegaba a «Conectada». No se ha ablandado ni un `waitFor`.

---

## 3. Defecto 2 — anchos que no caben en la pantalla de CI

Tapado por el defecto 1: CI nunca pasó de la línea 96, así que este nunca se vio.

### Qué pasa

La prueba barría `WIDTHS = [320, 768, 1440]` **sin filtrar**. Al 200 % cada píxel CSS ocupa dos de
ventana, así que ver 1440 px CSS exige una ventana de 2880 px más el cromo del navegador.
`xvfb-run -a` (así se invoca en `.github/workflows/harness-ci.yml:53`, sin `-s`) usa la pantalla por
omisión de 1280 px de ancho. Ni 1440 ni 768 caben ahí.

Las **otras tres** specs de zoom nativo del repositorio ya resuelven esto, con el mismo idioma y el
motivo escrito:

- `e2e/external-calendar-native-zoom.spec.mjs:247`
- `e2e/automations-native-zoom.spec.mjs:239`
- `e2e/webhooks-native-zoom.spec.mjs:331`

```js
const available = await page.evaluate(() => screen.availWidth);
const fits = WIDTHS.filter((width) => width * 2 + chrome_ <= available);
const skipped = WIDTHS.filter((width) => !fits.includes(width));
expect(fits, `ningún ancho del contrato cabe ...`).not.toHaveLength(0);
```

`github-connector-native-zoom.spec.mjs` era la **única** que no lo tenía. Confirmación indirecta
desde el propio log de CI: `automations-native-zoom` anuncia «los cuatro anchos» y termina en
**2,0 s**; `external-calendar-native-zoom` en **2,4 s**. Con axe en cada ancho eso sólo es posible
si el filtro está descartando casi todos.

### El rojo acreditado

Se añadió temporalmente un ancho que no cabe (`4000`) y se corrió la prueba tal cual estaba:

```
✘  1 e2e\github-connector-native-zoom.spec.mjs:37:1 › @s42 los tres anchos ... (6.6s)
   Error: worker.evaluate: Error: Invalid value for bounds.
          Bounds must be at least 50% within visible screen space.
   > 135 |  await chrome.windows.update(tab.windowId, { width: outer });
```

Chromium **rechaza** la ventana: un ancho que no cabe hace fallar la prueba, no la salta. Es
exactamente lo que le habría pasado en CI con 768 y 1440 sobre una pantalla de 1280 px.

### El verde

Se adopta el idioma de las tres hermanas: se calcula `fits`/`skipped`, se exige que **al menos uno**
quepa (`expect(fits).not.toHaveLength(0)`, así la prueba nunca queda vacía y verde), se recorre
`fits` y `evidence.json` pasa a registrar `{ pantalla, medidos, omitidos }`. El título deja de
prometer «los tres anchos» y dice «los anchos que caben», como el de calendario externo.

Evidencia de la ejecución verde en esta máquina (pantalla de 1707 px lógicos):

```json
{ "pantalla": 1707,
  "medidos":  [ { "width": 320, "dpr": 3, "scroll": 312, "client": 312, "controls": 5, "violations": 0 },
                { "width": 768, "dpr": 3, "scroll": 760, "client": 760, "controls": 5, "violations": 0 } ],
  "omitidos": [ 1440 ] }
```

### Qué deja de cubrir, dicho sin adornos

En una pantalla pequeña el zoom nativo **no** mide todos los anchos del contrato: en el xvfb de CI
medirá 320, y aquí 320 y 768. Lo que se pierde no queda descubierto: los tres anchos se recorren
enteros, con axe y con la geometría de todos los controles, en
`e2e/github-connector.spec.mjs` (`assertLayout` + `auditAxe` en los siete estados). El precio queda
escrito en la cabecera del fichero y en `evidence.json`, no disimulado con un `skip`.

Sigue mordiendo lo que tenía que morder: si el zoom no se aplica, la prueba **falla**, no se salta
(`expect(zoom).toBe(2)` y `expect.poll(devicePixelRatio)`).

---

## 4. Las 3 pruebas que se saltan

Son estas, y se saltan **siempre**, no por casualidad:

| # | Prueba |
|---|--------|
| 107 | `export-data-browser.spec.mjs:149` › native Chromium zoom 200 at 320 CSS pixels @s32 |
| 153 | `import-data-browser.spec.mjs:8` › native Chromium zoom 200 at 320 CSS pixels @s42 |
| 162 | `integration-api-browser.spec.mjs:437` › native Chromium zoom 200 @s41 |

Las tres empiezan igual:

```js
test.skip(testInfo.project.name !== "chromium", "Native extension zoom is measured in Chromium; ...");
```

`playwright.config.mjs` **no declara `projects`**. Sin `projects`, Playwright crea un único proyecto
cuyo `name` es la cadena vacía `""`, que nunca es `"chromium"`. Es decir: **la guarda no distingue
motores, siempre se cumple**, y estas tres pruebas no se ejecutan jamás bajo `pnpm test:e2e`.

Los `projects` con nombre `"chromium"` viven en `export-data-browser.config.mjs`,
`import-data-browser.config.mjs`, `integration-api-browser.config.mjs` y `cross-browser.config.mjs`
— y `.github/workflows/harness-ci.yml` **no ejecuta ninguna de esas configuraciones**: sólo
`xvfb-run -a pnpm test:e2e`, `pnpm test:web-dns`, `pnpm test:publisher` y `harness init`.

**Conclusión: sí, se saltan por algo que ya no aplica.** El motivo declarado («otros motores tienen
evidencia de texto/reflujo») presupone una ejecución multi-motor que en CI no existe; el efecto real
es que la evidencia de zoom nativo de @s32, @s41 y @s42 no se mide **en ningún sitio** de la
integración continua. No lo he tocado: cae fuera del encargo de esta noche y toca tres ficheros y la
configuración. Queda apuntado como el siguiente hilo del que tirar.

---

## 5. Hallazgo lateral (no tocado)

`e2e/appearance-persistence.spec.mjs:131` **fija el puerto a mano**:

```js
const fresh = await browser.newContext({ baseURL: "http://127.0.0.1:18080" });
```

Eso anula el `E2E_WEB_PORT` que `scripts/e2e.mjs` existe para honrar y que los worktrees en
paralelo necesitan. En CI no se nota (allí el puerto por omisión *es* 18080), pero en este carril
falla con `connect ECONNREFUSED 127.0.0.1:18080`. Es un defecto real y ajeno a esta avería; no lo
arreglo porque no tengo un rojo que lo pida dentro de mi encargo. Por eso esa spec queda fuera de
las dos ejecuciones verdes de abajo.

---

## 6. Ciclos rojo-verde-refactor

| Ciclo | Rojo | Verde | Refactor |
|-------|------|-------|----------|
| 1 | `@s42 conectar sobrevive al reinicio del backend` → `Expected 200, Received 503` | `restartBackend` reinicia también `github-fake` (`--profile e2e`) | `45_000` → `RESTART_READY_TIMEOUT_MS` exportado; el presupuesto de la prueba se deriva de `testInfo.timeout + RESTART_READY_TIMEOUT_MS` |
| 2 | ancho `4000` en `WIDTHS` → `Invalid value for bounds` | `fits`/`skipped` por `screen.availWidth`, con `expect(fits).not.toHaveLength(0)` | título y cabecera dejan de prometer tres anchos; `evidence.json` pasa a `{ pantalla, medidos, omitidos }` |

### Mapa `@s` → prueba

| Escenario | Prueba |
|-----------|--------|
| @s42 (estado conectada, tras reinicio del backend) | `e2e/github-connector.spec.mjs` › `@s42 conectar sobrevive al reinicio del backend que hacen otras pruebas` |
| @s42 (zoom nativo 200 %, anchos que caben) | `e2e/github-connector-native-zoom.spec.mjs` › `@s42 los anchos que caben al 200 % de zoom nativo, sin recorte ni desplazamiento` |

---

## 7. Ficheros tocados

```
 e2e/github-connector-native-zoom.spec.mjs | 31 ++++++++++++++++++++---
 e2e/github-connector.spec.mjs             | 41 +++++++++++++++++++++++++++++++
 e2e/support/backend.mjs                   | 17 ++++++++++-
```

Nada de `src/`, nada de `frontend/src/`, nada del backend Java: **el producto no se ha tocado**,
porque el producto no era el culpable.

---

## 8. Verificación

Rodaja de regresión en orden de CI: las specs que reinician el backend **antes** de las del
conector, más el resto de usuarias de `restartBackend`. Reproduce la situación original de CI
(reinicio → conector) y prueba que el arreglo aguanta.

```
e2e/customization-persistence.spec.mjs
e2e/github-connector-native-zoom.spec.mjs
e2e/github-connector.spec.mjs
e2e/history.spec.mjs
e2e/ics-calendar.spec.mjs
e2e/reschedule-recovery.spec.mjs
e2e/schedule-block.spec.mjs
e2e/weekly-review.spec.mjs
```

**Dos ejecuciones verdes seguidas**, pila levantada y bajada en cada una:

```
36 passed (2.8m)
36 passed (3.1m)
```

Antes de ellas, la rodaja completa (incluyendo `appearance-persistence`) dio `36 passed, 1 failed`,
siendo ese 1 el `ECONNREFUSED 127.0.0.1:18080` del §5, ajeno a este trabajo.

Formato y lint: `pnpm lint` verde (spotless + eslint + prettier de `frontend`); los tres ficheros
tocados pasan `prettier --check`.

Pila de E2E bajada al terminar; no queda ningún contenedor de este carril en pie.

### Lo que esta bitácora **no** puede afirmar

No he corrido las 227 pruebas de una tirada: esta máquina tenía ocho carriles más y ~27
contenedores vivos, y el encargo pedía una sola pila. Lo verificado es la rodaja de arriba, que es
la que contiene la avería y sus vecinas. Queda pendiente la confirmación en la propia CI.

## 9. Nota para el centro (no bloqueante)

El filtro `width * 2 + chrome_ <= available` que usan las cuatro specs de zoom es más estricto que
la regla real de Chromium, que es «al menos el 50 % de la ventana dentro de la pantalla visible».
Medido aquí: una ventana de 2880 px sobre una pantalla de 1707 px (59 % visible) **se acepta**; una
de 8000 px (21 %) se rechaza. Es decir, el filtro omite anchos que sí se podrían medir. He
mantenido el idioma existente por consistencia con las tres hermanas y por ser el lado conservador
del error, pero afinar la regla recuperaría 768 en CI y 1440 en local **en las cuatro specs a la
vez**. Eso es un cambio transversal y no lo he metido en este carril.
