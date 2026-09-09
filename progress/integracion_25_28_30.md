# Bitácora de integración: 27 (rama corregida), 25, 28 y 30 — 9 de septiembre de 2026

Integrador: `tdd_craftsman` en papel de integrador, sobre `main` (arranque en `99d3e64`).
Documento de cabecera: `progress/auditoria_colisiones_25_28_30.md` (55 agentes, 47
candidatas, 25 confirmadas, 8 bloqueantes) más la enmienda del orden canónico de
navegación al final de `project-spec.md`.

Orden de integración (ampliado por el coordinador a mitad de arranque):

1. `claude/github-connector` (feature 27, versión corregida tras el rechazo del juez)
2. `claude/webhooks` (feature 25)
3. `claude/external-calendar` (feature 28)
4. `claude/automations` (feature 30)

## Línea base medida en `main` antes de tocar nada

- `node --test scripts/project.test.mjs`: 79 pruebas, 79 en verde.
- `pnpm --dir frontend exec vitest run`: 74 ficheros, 2622 pruebas en verde.
- Backend: se toma como referencia el dato del encargo, 3824 pruebas en verde.

## Merge 1 — `claude/github-connector` (feature 27, versión que cierra los cinco bloqueantes del juez)

Añadido al encargo por el coordinador. La rama parte de `9c2dbe4`, dos commits por
delante, así que casi todo entra limpio.

**Conflicto único: `backend/.../persistence/GithubConnectorPersistenceTest.java`.**
Ambos lados dejan la misma línea de producción (`TestDatabase.empty(jdbc);`); lo que
choca es que la rama añade encima un comentario de dos líneas explicando por qué se
vacía descubriendo tablas. Resuelto conservando el comentario de la rama sobre la
llamada de `main`. Criterio: no hay diferencia de conducta, solo documentación, y la
rama es la que conoce el motivo (la V25 con `task_external_links`).

**Cruce con la 28 que la auditoría no cubría** (el barrido se hizo sin esta rama):
`docker-compose.yml` y `scripts/e2e.mjs`. Esta rama añade el servicio `github-fake`
bajo el perfil `e2e` y cambia `APP_GITHUB_API_BASE` de `http://127.0.0.1:9` a
`http://127.0.0.1:9000`. La 28 toca los mismos ficheros por `APP_CONNECTOR_KEY` y
`APP_CONNECTORS_ALLOW_PRIVATE_ADDRESSES`, en otras líneas. Anotado para vigilarlo en
el merge 3.

**Verificación tras el merge:**
- backend `gradlew.bat test --no-daemon`: 174 clases, **3824 pruebas, 0 fallos**
  (igual que la línea base: la rama no añade ni pierde pruebas de backend).
  Aviso de entorno: Gradle terminó la tarea `:test` con
  `NoSuchFileException ...in-progress-results-generic*.bin` después de ejecutar y
  escribir los 174 XML en verde. Es la carpeta `build/` dentro de OneDrive, no una
  prueba roja; queda anotado por si se repite.
- `node --test scripts/project.test.mjs`: 79/79.
- `vitest`: 74 ficheros, **2625** pruebas (2622 + 3 del `Escape` real).
## Merge 2 — `claude/webhooks` (feature 25)

**Conflicto único: `backend/.../persistence/WeeklyReviewPersistenceTest.java`.**
La rama parte de `9d81c17`, antes de la limpieza de fixtures, y parchea el
`@BeforeEach` añadiendo ` CASCADE` a la lista de vaciados escrita a mano; `main`
reescribió esa misma línea a `TestDatabase.empty(jdbc)`. Resuelto **con el lado de
`main`**, como manda el hallazgo 17 de la auditoría: `TestDatabase.empty` descubre las
tablas en `information_schema` y emite el vaciado con `RESTART IDENTITY CASCADE`, así
que absorbe entera la intención del parche y además cubre las tablas futuras con clave
ajena. Comprobado después: la búsqueda de listas de tablas escritas a mano en
`backend/src/test` no devuelve nada.

Todo lo demás entró limpio, incluidos los cuatro ficheros de prueba de navegación que
la rama ya había reescrito a orden relativo por nombre (`App.test.tsx`,
`appearance.test.tsx`, `export-data.test.tsx`, `integration-api.test.tsx`). Eso es
justamente lo que exige la enmienda del orden canónico, así que no hubo que tocarlos.

**Nota de trazabilidad.** Mientras el merge estaba resuelto y en el índice, pendiente de
confirmar, el coordinador hizo un commit propio sobre `harness.config.json`
(`one_feature_at_a_time` a `false`). Git tenía `MERGE_HEAD` puesto, así que ese commit
se llevó consigo el merge entero: `1f898c1` es un commit de fusión de verdad (padres
`fa49fd7` y `8389040`, la punta de `claude/webhooks`) con el árbol correcto, pero su
mensaje solo habla de la regla del arnés. No se toca la historia ya escrita; queda
explicado aquí. El `false` de `one_feature_at_a_time` se conserva.

**Verificación tras el merge:**

- backend: 197 clases, **4057 pruebas, 0 fallos** (+23 clases y +233 pruebas sobre las
  3824 de la línea base). BUILD SUCCESSFUL.
- `vitest`: 77 ficheros, **2669** pruebas en verde (+44).
- `node --test scripts/project.test.mjs`: **77 de 79**. Los dos rojos son exactamente
  los que la auditoría predijo (hallazgo 5, punto 2, y hallazgo 7, punto 3): los dos
  únicos guardas *semánticos*, «ics calendar Stryker selects its own nodes of the shared
  files» y «appearance Stryker preserves all candidates and reviewed integration
  nodes», que recortan `App.tsx` por rango `línea:columna` y comprueban el texto
  seleccionado. La rama 25 desplaza `App.tsx` sin recalcular ningún rango. **No se
  tocan ahora**: los rangos se recalculan todos de una vez al final, contra el
  `App.tsx` definitivo, como ordena el encargo.
