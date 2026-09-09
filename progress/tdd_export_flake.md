# Carril: flake de exportación en CI (`export_flake`)

Worktree: `C:/Users/vhurt/ow-worktrees/ci-export-flake` — rama `claude/ci-export-flake`.
Alcance: **sólo pruebas E2E**. No se toca `src/` ni el comportamiento del producto.

## 1. Síntoma

Ejecución de CI `34303295662` («Application CI», commit `5c84a87`), paso
`xvfb-run -a pnpm test:e2e`:

```
✘ 98 e2e/export-data.spec.mjs:13:1 › export: real owner snapshot downloads original bytes twice
   without another GET and releases its URL @s22 @s24 @s26 @s29 (1.1s)
Error: response.body: Protocol error (Network.getResponseBody): No data found for resource with given identifier
   79 |     expect(response.status()).toBe(200);
1 failed, 166 passed (10.4m)
```

Intermitente: la misma prueba pasa en local y ha pasado en otras ejecuciones de CI.

## 2. Diagnóstico

La línea que falla era `const original = await response.body();`.

`Response.body()` **no lee del transporte**: le pide a Chromium, por CDP
(`Network.getResponseBody`), una copia del cuerpo guardada en la *caché del
inspector*. Esa caché tiene un presupuesto de memoria y se vacía cuando:

- el recurso ya fue consumido por la página (aquí la página hace `fetch` →
  `blob` → `URL.createObjectURL`, y el `blob:` se libera al navegar), o
- entra tráfico nuevo que desaloja las entradas viejas (presión de memoria:
  justo lo que pasa en CI, con la suite entera compartiendo un navegador y una
  máquina cargada).

Es decir: la prueba pedía los bytes **tarde**, cuando Chromium ya podía haberlos
tirado. Nada del producto falla; falla la instrumentación de la prueba. Los dos
mensajes observados (`No data found for resource with given identifier` en CI y
`Request content was evicted from inspector cache` en la reproducción local) son
la misma familia de error.

## 3. Reproducción determinista (ROJO)

Con la pila E2E aislada levantada (proyecto compose
`organizationweb-e2e-flake18095`, puerto 18095), dos specs de diagnóstico
temporales — no versionados, conservados fuera del repo — forzaron el fallo
**siempre**, sobre el flujo real de exportación:

1. `export-flake-repro.spec.mjs`
   - *repro A*: tras preparar la exportación, 30 descargas de 8 MiB desde un
     servidor local (240 MiB de presión de red) y luego `response.body()`
     → `Protocol error (Network.getResponseBody): Request content was evicted
     from inspector cache`.
   - *repro B*: tras preparar la exportación, navegar a otra ruta y luego
     `response.body()` → `Protocol error (Network.getResponseBody): No resource
     with given identifier found`.
   - Resultado: `2 failed`.

2. `export-flake-guard.spec.mjs` — el contraste rojo/verde sobre el flujo real,
   con la **misma** presión de red en ambos casos:

```
✘  1 old reading: response.body() over the real export under cache pressure (3.8s)
    Error: response.body: Protocol error (Network.getResponseBody): Request content was evicted from inspector cache
✓  2 transport capture: route.fetch() bytes survive the same cache pressure (5.2s)
1 failed
1 passed (11.6s)
```

Queda probado que la causa es la lectura tardía y que la captura en el
transporte la elimina.

## 4. Arreglo (VERDE)

### `e2e/export-data.spec.mjs`

La prueba se queda con los bytes **en el momento en que la respuesta cruza el
transporte**, antes de que la página los consuma y antes de que Chromium pueda
desalojarlos:

```js
await page.route(
  (url) => url.pathname === "/api/v1/me/export",
  async (route) => {
    transportFetches++;
    const served = await route.fetch();     // un único GET real al backend
    original = await served.body();         // bytes retenidos por Playwright
    transport = { status: served.status(), headers: served.headers() };
    await route.fulfill({ response: served, body: original });
  },
);
```

`route.fetch()` usa las cookies del contexto (la sesión `e2e-user` real), hace
**una** petición real al backend y devuelve un `APIResponse` cuyo cuerpo
pertenece a Playwright, no a la caché del inspector: no caduca nunca.

No se ha debilitado ninguna aserción; se han **añadido**:

- `expect(transportFetches).toBe(1)` — un único GET real al servidor.
- `expect(transport.status).toBe(200)` y `expect(original).toBeInstanceOf(Buffer)`
  — si alguien retira la captura, `original` sería `null` y la prueba cae.
- `content-type`, `content-length` y `content-disposition` verificados **dos
  veces**: en la respuesta del servidor (`transport.headers`) y en la que recibe
  el navegador (`response.headers()`).
- `expect(transportFetches).toBe(1)` de nuevo tras las dos descargas.

Sigue intacto todo lo demás: comparación byte a byte de cada descarga contra
`original` (`readFile(await download.path())`), `reads.length === 1`,
`downloads.length === 2`, `writes === []`, snapshot de BD sin cambios, revocación
de la `blob:` URL y exclusión del proyecto ajeno.

Ni `flaky`, ni `skip`, ni `retry`, ni tolerancias: el arreglo es determinista.

### `e2e/export-data-browser.spec.mjs`

Único otro uso de `await response.body()` del repo (línea 495), dentro de un
`page.on("response", async …)`: mismo riesgo de desalojo y, además, un rechazo
ahí dentro es una promesa no gestionada. Era evidencia (`transport.json`), no
aserción.

Se sustituye por: cabeceras desde el evento `response` (`response.headers()`, un
getter síncrono, sin ida y vuelta por CDP) y bytes desde el archivo de la
descarga, que Chromium sí persiste en disco y la prueba ya leía para su
comparación byte a byte contra `bytes`.

De paso se corrige un fallo silencioso: el filtro
`response.url().endsWith("/api/v1/me/export")` **nunca** casaba, porque
`fulfillArchive` reencamina la petición al servidor local y la respuesta
observable termina en `/export`. La evidencia `transport.json` no llegaba a
escribirse. Ahora sí:

```json
{
  "headers": {
    "content-length": "886",
    "content-type": "application/json; charset=utf-8",
    "content-disposition": "attachment; filename=\"organizationweb-export-v1-20260908T102030123456Z.json\""
  },
  "bytes": 886,
  "equal": true
}
```

## 5. Trazabilidad de escenarios (`features/export_data.feature`)

| Escenario | Qué lo verifica tras el arreglo |
| --- | --- |
| `@s22` | Navegación a `/exportacion`, h1 enfocado y `expect(reads).toHaveLength(0)` antes de preparar: sin cambios. |
| `@s24` | Bytes del transporte capturados en `route.fetch()`; `content-length` del servidor y del navegador iguales a `original.length`; cada descarga comparada byte a byte con `original`; envelope, owner, versión, colecciones y counts validados sobre esos bytes. Más estricto que antes. |
| `@s26` | `reads.length === 1` (una sola petición del navegador) **y** `transportFetches === 1` (un solo GET real al backend) tras los dos gestos de descarga; `downloads.length === 2` con el mismo nombre de archivo. |
| `@s29` | Navegar a Hoy retira el enlace y la `blob:` URL deja de resolver (`fetch(objectUrl)` lanza): sin cambios. |
| `@s23 @s24 @s26 @s31` (browser) | La descarga nativa sigue comparándose byte a byte contra los bytes simulados; la evidencia de transporte ya no depende de la caché del inspector. |

## 6. Evidencia de ejecución

Pila E2E aislada: `docker compose -p organizationweb-e2e-flake18095`,
`E2E_WEB_PORT=18095`. Una sola pila, levantada y derribada una vez.

Comando exigido, sobre el árbol final:

```
$ pnpm exec playwright test e2e/export-data.spec.mjs --repeat-each=5 --workers=1
Running 5 tests using 1 worker
  ✓  1 … @s22 @s24 @s26 @s29 (2.7s)
  ✓  2 … @s22 @s24 @s26 @s29 (3.9s)
  ✓  3 … @s22 @s24 @s26 @s29 (2.7s)
  ✓  4 … @s22 @s24 @s26 @s29 (3.3s)
  ✓  5 … @s22 @s24 @s26 @s29 (3.0s)
  5 passed (22.0s)
```

(Una primera tanda idéntica, antes de tocar el spec de navegador, dio también
`5 passed (32.1s)`. Total: 10/10 verdes.)

Spec de navegador tocado:

```
$ pnpm exec playwright test e2e/export-data-browser.spec.mjs --workers=1
  1 skipped
  4 passed (19.2s)
```

Formato: `prettier --check` sobre los dos ficheros → *All matched files use
Prettier code style!*

## 7. Pendiente / avisos

- **No** se ha ejecutado la suite E2E completa (`pnpm test:e2e`): requiere aviso
  previo por disciplina de recursos (5 carriles más en la máquina). Los dos
  únicos ficheros modificados se han ejecutado en verde por separado. Queda a la
  orden del lead lanzar la suite completa una vez.
- Los specs de diagnóstico (`export-flake-repro`, `export-flake-guard`) no se
  versionan: son temporales y lentos (~3 min por la presión de red). Su código
  está resumido arriba y conservado en el scratchpad de la sesión.
- Regla que conviene recordar en revisiones: **no usar `Response.body()` en E2E**
  para recursos que la página consume o descarga. Capturar en `route`/`route.fetch`
  o leer el archivo del `download`.
