# La CI vuelve a verde — 10 de septiembre de 2026, 15:07

**Ejecución `34477162444`, sobre `9d1e7d2e`. Todos los pasos en verde.** Es la
primera desde hace más de un día: las cuarenta anteriores fallaron o se
cancelaron.

| Paso | Resultado |
|---|---|
| `node scripts/project.mjs install` | ✅ |
| `playwright install --with-deps chromium` | ✅ |
| **`node .harness/harness.mjs init`** | ✅ — **95/95 guardas** y **91 ficheros de prueba** de frontend |
| `pnpm build` | ✅ |
| `pnpm test:web-dns` | ✅ |
| `xvfb-run -a pnpm test:e2e` | ✅ — **232 pruebas** en 17,9 min |
| `pnpm test:publisher` | ✅ |

`bin/harness init` en verde es **condición de cierre** en varios veredictos, y
llevaba días sin poder acreditarse.

## Qué la tenía en rojo, y no era el producto

Dos defectos del andamiaje de E2E, los dos encontrados hoy:

1. **El falso de GitHub no sobrevivía al reinicio del backend.** Vive *dentro*
   del espacio de red del backend (`network_mode: service:backend`), y
   `restartBackend` reiniciaba sólo el backend: desde el primer reinicio ya no lo
   alcanzaba en `127.0.0.1:9000` y conectar respondía 503. Reproducido:
   `[ANTES] 200 status:valid` → `[DESPUES] 503 GITHUB_UNAVAILABLE`. El spec
   hermano que conecta de verdad pasaba sólo porque corre **antes** de cualquier
   reinicio.
2. **Los barridos de anchos no filtraban lo que no cabe en el xvfb de 1280 px.**
   `chrome.windows.update` rechaza límites que no quepan al menos al 50 % en la
   pantalla visible, con «Invalid value for bounds». Le pasaba al spec de la
   feature 27 y, en cuanto se arregló, salió a la luz el mismo defecto en el spec
   de accesibilidad de la 29, recién integrado.

En los dos casos el arreglo fue del **andamiaje**, no del producto, y en los dos
se corrigió además la **afirmación**: los títulos prometían «los tres anchos» y
ahora dicen «los que caben», con los omitidos registrados en `evidence.json`.

## La progresión, que es lo que dice que el árbol está sano

| Ejecución | Resultado |
|---|---|
| `34466099879` | 223 pasan, 1 falla (zoom nativo de la 27) |
| `34470737773` | 223 pasan, 1 falla — **la misma**: los merges de la 27 y la 28 no rompieron nada |
| `34473728624` | **231** pasan, 1 falla — la de la 27 ya pasa; falla el spec nuevo de la 29 |
| `34477162444` | **232 pasan, 0 fallan** ✅ |
