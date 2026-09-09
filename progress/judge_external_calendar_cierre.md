# Review de cierre — feature 28 `external_calendar`

**Veredicto: APPROVED CONDICIONADO A** (a) la resolución del conflicto B3 entre
`project-spec.md:2492` / `deploy/EGRESS.md` y lo que la 28 implementó en el ciclo 7,
(b) la ratificación humana de la ampliación de contrato del ciclo 4, y (c) una campaña
de mutación que cumpla las condiciones de la sección «Puerta de mutación». Sin las tres,
la 28 no puede pasar a `done`.

Método: lectura del árbol en `main` (limpio), sin ejecutar la suite completa, ni
`bin/harness init`, ni campañas de mutación (tres carriles y dos Stryker en la misma
máquina). Única ejecución: `node --test --test-name-pattern "external calendar Stryker"
scripts/project.test.mjs` → **2/2 en verde**.

---

## 1. Verificación de los 19 hallazgos: ¿cerrados o declarados cerrados?

Fui a la prueba de cada uno y me pregunté si puede fallar. **Diecisiete cierres son
reales y discriminantes.** Uno (4) está delegado y cumplido por otro carril. Uno (19)
está cerrado en su parte portante y deja un residuo que el artesano declara.

| # | Verificado en | ¿Puede fallar? |
| --- | --- | --- |
| 1, 5, 10, 12 | `e2e/external-calendar-ux-audit.spec.mjs:33-40` (6 estados), `:118-194` (`enter()`), `:219-300` (`geometry()`), `:327-363` (14 anchos x 6 estados = 84 mediciones) | **Sí.** `geometry()` mide recorte por elemento en los **dos** ejes con guarda de `overflow` (`:282-295`), solapes por pares con exclusión de ancestro/descendiente (`:245-259`), objetivos 44 px, escape de viewport y desbordamiento de página. Además `:353-362` afirma que «con suscripción» tiene más controles que «vacío» y que el diálogo aporta exactamente dos: si la siembra por SQL dejara de funcionar, la matriz no volvería a medir seis veces el formulario vacío en silencio. Rojo acreditado con `overflow:hidden; max-height:96px` sobre `li` → 3 de 4 pruebas caídas, nombrando elemento a elemento. |
| 3 | `progress/ux_external_calendar.md`, 176 líneas, **32 filas de tabla** (30 principios + cabecera + separador), sección «Evidencia y límites» **antes** de la tabla | Sí en el sentido exigible: una fila por principio y cifras desglosadas fichero a fichero. |
| 4 | `e2e/external-calendar-native-zoom.spec.mjs` (304 líneas), `:234` `chrome.tabs.setZoom(tab.id, 2)`, `:237` `expect(zoom).toBe(2)`, `:269` `expect(measured.dpr).toBe(baseline.dpr*2)` | **Sí.** Se comprueba que el zoom ocurrió **antes** de medir; no es el placebo de `fontSize`. |
| 6 | `HttpCalendarFeed.java:96-120` y `:122-149`; pruebas `HttpCalendarFeedTest.java:219-239` y `:246-255` | **Sí.** La primera pide `http://nombre.que.no.resuelve.invalid:<puerto>/…` con un resolutor que devuelve loopback: si el cliente volviera a resolver, el fetch acabaría en `FEED_UNREACHABLE`. Afirma además `resolutions == 1` y `Host: nombre.que.no.resuelve.invalid:<puerto>`. La segunda exige `received.isEmpty()`: no basta con fallar después. (Ver §4.2: el diseño choca con la spec.) |
| 8 | `frontend/src/external-calendar.test.tsx:312-351` | **Sí.** Retiene el POST con una promesa, exige «Sincronizando…», `toBeDisabled()` en los tres botones, `readonly` en los dos campos, **segundo clic** y `calls.filter(POST)).toHaveLength(1)`. Ese recuento es lo que mata el mutante de `setBusy("syncing")`. Las filas de FAILED (`:371`) y 404 (`:390`) ganaron el recuento. |
| 2 y 9 | `frontend/src/external-calendar.test.tsx:422-444` | **Sí.** Acumula `options.signal`, exige `every(!aborted)` con la vista montada, desmonta, exige `every(aborted)` y sólo entonces libera la respuesta tardía. Ya no es el `document.body.textContent === ""` que pasaba siempre. |
| 11 | `backend/.../adapter/ExternalCalendarTodayApiTest.java:180-225` | **Sí.** Tres examples reales; captura R0, siembra suscripción y evento por SQL, compara **la cadena entera** (`:201-203`) y planifica exigiendo 201 y ausencia de `BLOCK_OVERLAP`. Reloj a 10:30Z (`:74`) para que `currentBlockId` y `closingAt` lleven valor y no `null`, que es lo que una regresión conservaría por accidente. Cierra el vector de **datos** que ArchUnit no ve. |
| 13 | `e2e/external-calendar-ux-audit.spec.mjs:412-515` | **Sí.** 4 modos x 6 estados = 24 pasadas de axe **y** geometría; cada modo fija las tres preferencias (`:416-433`); `color-contrast` se desactiva sólo bajo `forced-colors` (`:445`); tinta distinta de lienzo (`:483`); y la pasada de movimiento reducido se declara vacua **con una aserción** (`:513`), no con un comentario. |
| 14 | `HttpCalendarFeed.java:137` (instante límite antes de `send`), `:195-218` (`read` con guillotina programada y comprobación por trozo); prueba `HttpCalendarFeedTest.java:180-210` | **Sí.** El servidor gotea un byte cada 50 ms sin cerrar; con plazo de 300 ms se exige `FEED_UNREACHABLE` **y** `elapsed < 5000 ms`. Sin la cota de tiempo el oráculo no distinguiría un corte de una espera larga; con el código anterior la prueba no terminaría (`@Timeout(15)`). |
| 15 | `frontend/src/external-calendar-route.test.tsx:143-187` | **Sí.** Sobre `SessionGate`, no sobre la vista aislada. Lo discriminante es `window.location.pathname === "/"` tras reabrir sesión: añadir `/calendario-externo` a `isPrivateRoute` la rompe. Cierra de paso la tercera entrada de @s37. |
| 16 | `frontend/src/external-calendar-api.test.ts:329-347` | **Sí.** Payload **válido** (`configured:false, subscription:null`) y afirma `rejection === controller.signal.reason`, `name: "AbortError"` y que **no** es el error de forma. Borrar `external-calendar-api.ts:197` ya no queda enmascarado. |
| 17 | `e2e/external-calendar-ux-audit.spec.mjs:541-583` | **Sí.** Mide **dentro** del bucle, parada por parada, con `matches(":focus-visible") && outlineStyle==="solid" && outlineWidth>=3 && !outlineColor.includes("transparent")`. Borrar la regla del producto ya no sobrevive tras el anillo del agente de usuario. Añade `intruders` para focos no esperados dentro del formulario. |
| 7 | `backend/build.gradle.kts`, `externalCalendarClasses` | **Cerrado.** Los globs muertos hacia `adapter.crypto` ya no están; en su lugar `adapter.connectors.AesGcmSecretCipher*`, `ConnectorKeyRing*` y `adapter.feed.HttpCalendarFeed*`. |
| 19 | `ExternalCalendarPersistenceTest.java:329-403` | **Sí en lo portante.** Se cuela dentro de la transacción del escritor subclasificando `JdbcTemplate` y lee desde otro hilo justo tras el `DELETE`: `containsExactly("u1","u2")`. Rojo acreditado sacando DELETE+insert del `writing(...)`. **Residuo declarado**: §5.1. |

**@s30 (el hueco que él mismo encontró): confirmado real y confirmado cerrado.**
`ExternalCalendarPersistenceTest.java:156-199` confirma **dos** sincronizaciones —la de
las 09:00Z con cinco eventos y la de las 11:00Z cuyo valor de retorno se descarta a
propósito, que es lo que significa «la respuesta se perdió»— y sólo entonces abre un
almacén nuevo, afirmando id, `lastSyncAt` 11:00Z, `lastAttemptAt`, estado OK, `version` 2,
`imported` 2 y `events(...)` igual a `["w1","w2"]`. La prueba vieja (`:137-143`, seis
líneas) no tocaba ni una sincronización ni un evento: el hueco era real. El rojo declarado
(`imported = imported + ?` → `expected 2 but was 7`, **una** de veinte pruebas caída) es
coherente con lo que leo: ninguna otra prueba de la clase encadena dos sincronizaciones
sobre la misma suscripción. La segunda mitad del Then («no se dispara ninguna
sincronización al arrancar») la cubren `ExternalCalendarIsolationTest:80` y `:95`.

---

## 2. Cobertura del contrato, escenario a escenario

Recorrí los 40 escenarios cruzando cada `@sN` contra el árbol de pruebas. **Los 40 tienen
al menos un oráculo que puede fallar.** Portadores principales:

- @s1 `ExternalCalendarApiTest`; @s2 `ExternalCalendarApiTest` + `ExternalCalendarPersistenceTest:121-134` + `ExternalCalendarAuditTest` + `e2e/external-calendar.spec.mjs`; @s3 `SaveExternalCalendarTest`; @s4 y @s5 `ExternalCalendarInputTest` + `ExternalCalendarApiTest`; @s6 `ExternalCalendarPersistenceTest:202-260`; @s7 `:263-277`.
- @s8 `ExternalCalendarDisabledApiTest` (un caso por ruta) + `ExternalCalendarWiringTest`; @s9 `ExternalCalendarWiringTest`; @s10 `ExternalCalendarApiTest`.
- @s11 `SyncExternalCalendarTest` + `ExternalCalendarApiTest`; @s12 `HttpCalendarFeedTest` (32 casos) + `SyncExternalCalendarTest` + `ExternalCalendarAuditTest`; @s13 `HttpCalendarFeedTest:117-128, :132-140, :219-255, :263-337`.
- @s14 a @s24 `IcsFeedTest`, `ExternalCalendarSnapshotTest`, `SyncExternalCalendarTest`, `ExternalCalendarPersistenceTest`.
- @s25 `ExternalCalendarPersistenceTest:280-318, :329-403, :622-632` (con el residuo de la sección 5.1); @s26 `:406-433, :462-467` + `SyncExternalCalendarTest`; @s27 `SyncExternalCalendarTest`; @s28 `ExternalCalendarApiTest`; @s29 `SaveExternalCalendarTest` + `SyncExternalCalendarTest`.
- **@s30** `ExternalCalendarPersistenceTest:156-199` + `ExternalCalendarIsolationTest:80, :95` — antes media frase, ahora entero.
- @s31 `ExternalCalendarApiTest` + `ReadExternalCalendarEventsTest` + `external-calendar.test.tsx:494`; @s32 `ExternalEventsRangeTest` + `ExternalCalendarApiTest`; @s33 `ReadExternalCalendarEventsTest` + `ExternalCalendarApiTest` + `ExternalCalendarPersistenceTest`.
- **@s34** `ExternalCalendarTodayApiTest:186-225` (tres examples, R0 byte a byte, 201) + las dos reglas ArchUnit de `ExternalCalendarIsolationTest`.
- @s35 y @s36 `today-external-calendar.test.tsx` (11 pruebas) + `today-external-section.test.tsx`, sólo jsdom y declarado; ningún Then de esos dos escenarios habla de geometría. @s37 `external-calendar.test.tsx` (7 casos) + `external-calendar-route.test.tsx` (5, con las tres entradas). @s38 `external-calendar.test.tsx:181-391`. @s39 `:393-444` + `external-calendar-route.test.tsx:143`.
- **@s40** `e2e/external-calendar-ux-audit.spec.mjs` (5 pruebas: 84 mediciones geométricas, axe, 24 pasadas de modos, teclado parada a parada, texto al 200 % en seis estados) + `e2e/external-calendar-native-zoom.spec.mjs` + `external-calendar.test.tsx:459, :477` + `progress/ux_external_calendar.md`.

Ningún `@s` queda sin oráculo: la regla dura de cobertura se cumple.

---

## 3. Calidad de los oráculos

Lo que hoy delató oráculos incapaces de fallar en otros carriles —esperas negativas,
aserciones ciertas por construcción, títulos que prometen más que el cuerpo— aquí está
corregido, y en dos casos corregido **porque la prueba se ejecutó por primera vez**:

- La espera negativa `not.toHaveText("Sincronizando…")`, que en Playwright pasa de
  inmediato si el anuncio no aparece jamás, se sustituye por dos esperas **positivas**
  (`e2e/external-calendar-ux-audit.spec.mjs:389-393`). Al ejecutarse falló de verdad,
  porque el plazo total del ciclo 4 hace que sincronizar contra un feed inalcanzable tarde
  ahora los cinco segundos completos.
- La prueba «texto al 200 %» **nunca amplió nada** desde que nació (`fontSize` de 32 px
  medía 16). Ahora `doubleText` (`:599-631`) escala el tamaño **calculado** elemento a
  elemento y **afirma** que se duplicó, con prioridad `important` por el
  `.quiet-note { font-size:14px !important }` de la hoja global.
- La excepción de recorte para `INPUT`, `SELECT` y `TEXTAREA` (`:280`) está documentada,
  razonada —una dirección iCal larga daría falso positivo garantizado— y acotada a una
  sola medida: esos nodos siguen entrando en objetivos, solapes y escape de viewport. La
  acepto.

Diseño: `HttpCalendarFeed` mantiene métodos cortos con un solo motivo de cambio
(`fetch`, `download`, `read`, `literal`, `authority`), el javadoc dice lo que el código
hace **y** lo que ninguna prueba ejerce, y las capas se respetan: `HostResolver` y
`AddressPolicy` son puertos de `application` y quien conecta es el adaptador.

---

## 4. Seguridad: los dos controles de carga

### 4.1 Plazo de lectura del cuerpo (ciclo 4) — cerrado, y bien cerrado

`HttpCalendarFeed.java:137` fija el instante límite **antes** de `send`, de modo que lo
que consuman conexión y cabeceras se descuenta del cuerpo; `:205-210` comprueba el plazo
en cada trozo; y `:196-200` programa el cierre del cuerpo en ese instante con un ejecutor
demonio de un solo hilo, porque un proveedor que enmudece del todo dejaría el `read`
bloqueado sin llegar nunca a la comprobación. Es la única forma de cerrarlo con
`BodyHandlers.ofInputStream()`, cuyo temporizador de `HttpRequest.timeout` se cancela al
llegar las cabeceras. El oráculo (`HttpCalendarFeedTest:180-210`) acota **tiempo**, no
sólo código de error. Documentación alineada (`docs/external-calendar.md:52-56`).

### 4.2 Reenlace DNS (enmienda B3) — CONFLICTO NORMATIVO, condición bloqueante

El código hace lo que el dictamen pedía y lo demuestra con dos pruebas que discriminan
(sección 1). El problema no es el código: es que **el texto normativo vigente dice lo
contrario, y lo dice desde 25 minutos antes de ese commit**.

- `project-spec.md:2492`, «Corrección del 9 de septiembre de 2026 (hallazgo 5 del dictamen
  de la feature 25)»: «el texto anterior anunciaba además conectar contra la dirección
  literal ya validada conservando `Host` y SNI, y **eso no se implementó ni se va a
  implementar como estaba escrito**, porque en el cliente HTTP del JDK exige
  `jdk.httpclient.allowRestrictedHeaders=host` y **rompe la verificación del nombre del
  certificado**, empeorando el TLS. **El reenlace de nombres vuelve a ser un límite
  aceptado y declarado**».
- `deploy/EGRESS.md:10-21`, vigente en `main`, nombra explícitamente «calendario externo
  de la 28» y afirma: «La petición viaja después **por nombre**, no por la dirección
  literal ya validada. Se decidió no anclar la conexión».
- `docs/external-calendar.md:40-45`, del mismo commit que el código (`1350034`), afirma lo
  contrario: «cerrado, no aceptado, conecta contra la dirección literal ya validada».

Cronología (`git log`): `0a68774` del 2026-09-09 a las **20:15:36** revoca el anclaje;
`1350034` del 2026-09-09 a las **20:32:43** lo implementa. Es una colisión de carriles
paralelos, no mala fe de nadie. Pero hoy `main` tiene **dos textos normativos que se
contradicen sobre el mismo control de seguridad**, que es exactamente el defecto por el
que el dictamen bloqueó esta feature: documentación que afirma un estado distinto del que
el código tiene.

Dos agravantes concretos que la decisión debe pesar:

1. **La razón alegada para revocar es justo lo único que no está probado.**
   `HttpCalendarFeedTest` habla HTTP en claro contra `127.0.0.1`; el apretón TLS con
   `SSLParameters.setServerNames` y `setEndpointIdentificationAlgorithm("HTTPS")`
   (`HttpCalendarFeed.java:156-166`) **no lo ejerce ninguna prueba**, y el artesano lo
   declara. Mi lectura de SunJSSE es que la identidad se comprueba primero contra el
   `SNIHostName` y sólo después contra el `peerHost`, de modo que el anclaje **no**
   debería romper la verificación del certificado; pero eso es un argumento, no una
   prueba, y si me equivoco **toda** suscripción https real termina en `FEED_UNREACHABLE`
   sin que la suite se entere.
2. **Efecto lateral de ámbito JVM.** El bloque estático de `HttpCalendarFeed.java:64-72`
   fija `jdk.httpclient.allowRestrictedHeaders=host` para **todo el proceso**: habilita el
   override de `Host` también a los clientes de webhooks y del conector de GitHub. Y el
   cliente del JDK lee esa propiedad al cargar su clase de utilidades: si otro componente
   crea un `HttpClient` antes, la autorización llega tarde y la descarga degrada a
   `FEED_UNREACHABLE`, en silencio. Ninguna prueba guarda ninguna de las dos cosas.

**Condición C1 (bloqueante).** El propietario elige una de las dos salidas, y lo elegido
se escribe en la sección de enmiendas de `project-spec.md` y en `progress/current.md`:

- **(a) Ratificar el anclaje para la 28.** Hay que corregir `project-spec.md:2492` y
  `deploy/EGRESS.md:10-21`, que hoy describen la 28 al revés, **y** añadir la prueba TLS
  que falta: servidor HTTPS con certificado propio para un nombre, resolutor que devuelve
  loopback, y afirmar que la descarga llega con el certificado validado contra el
  **nombre**, y que un certificado emitido para otro nombre la rechaza. Sin esa prueba, el
  motivo por el que se revocó B3 sigue sin medirse. Añadir también la guarda de que, sin
  la propiedad restringida, se degrada a `FEED_UNREACHABLE` en vez de conectar sin `Host`.
- **(b) Mantener la revocación.** Revertir el anclaje en `HttpCalendarFeed`, dejar la
  guardia como pre-comprobación y corregir `docs/external-calendar.md:40-45` para que no
  prometa «cerrado, no aceptado».

No decido yo cuál: es una decisión de riesgo del propietario. Lo que no puede quedarse es
el estado actual, con dos documentos normativos incompatibles.

**Condición C2 (bloqueante, puerta humana).** El ciclo 4 **amplía el contrato**
(`features/external_calendar.feature:13-14` y la fila nueva de @s12 en `:199`). El
artesano lo declaró en voz alta, como manda REGLAS.md, pero el `.feature` es puerta de
aprobación humana: hace falta la ratificación explícita del propietario, o la reversión
completa (contrato, documentación, código y prueba).

---

## 5. Accesibilidad, y los dos avisos honestos del artesano

`progress/ux_external_calendar.md` tiene las 30 filas y pone los límites **antes** de la
tabla. La evidencia en navegador es sustancial: 84 mediciones geométricas con cinco
oráculos cada una, 24 pasadas de axe más geometría por modos, teclado medido parada a
parada, texto al 200 % realmente ampliado y zoom nativo con DPR comprobado.

**Los dos avisos NO son bloqueantes:**

- **Revisión manual con lector de pantalla.** No es automatizable; axe lo declara en la
  propia prueba (`e2e/external-calendar-ux-audit.spec.mjs:396-398`) y el documento UX lo
  lista como pendiente. Ninguna feature cerrada de este repositorio ha pasado esa puerta.
  Deuda declarada aceptable; que quede registrada en `progress/current.md` como pendiente
  de producto, no de este carril.
- **Firefox y WebKit.** `playwright.config.mjs` no define proyectos por motor y el zoom
  nativo sólo se sabe hacer con `chrome.tabs.setZoom`. Es limitación **de casa**,
  compartida con las features hermanas ya cerradas. Deuda declarada aceptable.

Bloquear la 28 por cualquiera de las dos sería aplicarle un listón que no se aplicó a
ninguna feature anterior. Que estén escritas, y no descubiertas por el siguiente dictamen,
es exactamente lo que se pide.

### 5.1 Residuo real que corrijo del recuento «19 de 19 sin residuo»

`ExternalCalendarPersistenceTest.java:622-632` (`s25_syncingDoesNotTouchTheOutbox`) sigue
comprobando **sólo** `SELECT count(*) FROM outbox_events = 0`. El tercer Then de @s25
(`features/external_calendar.feature:357`) dice «ninguna otra tabla, outbox ni historial
cambia». El artesano lo declara abierto en el ciclo 6 («Lo que este ciclo NO cierra»),
pero la tabla de estado y el recuento final lo dan por cerrado sin residuo. **No bloquea**
—el escenario tiene tres oráculos que sí muerden—, pero «19 de 19, sin residuo» es medio
punto optimista. Cierre barato: instantánea de recuentos de **todas** las tablas antes y
después de `commitSuccess`, no sólo de `outbox_events`.

---

## 6. Puerta de mutación: qué exijo para que la campaña signifique algo

La campaña la corre el propietario. Fijo el listón teniendo en cuenta
`progress/hallazgo_rangos_stryker.md` (ámbitos con rangos desplazados que acaban mutando
sentencias `import`) y el hallazgo 7 (globs PIT muertos que bendecían una campaña sin
tocar el cifrador).

**Puntuación mínima: 80 % en cada capa.** Es el umbral vivo del proyecto
(`harness.config.json` → `mutation.threshold = 0.8`; `backend/build.gradle.kts:694`
`mutationThreshold = 80`; `stryker.external-calendar.config.json` → `break: 80`). Una
cifra por encima de 80 **no vale por sí sola**: sólo vale si además se cumplen C3 y C4.

**Condición C3 — PIT (`bin/harness mutate external_calendar-backend`).** El informe debe
acreditar **mutantes generados mayores que cero** en cada una de estas clases. Si alguna
sale con cero, la campaña es nula aunque la cifra global pase; es literalmente lo que
ocurrió con `adapter.crypto`:

1. `adapter.feed.HttpCalendarFeed` — **la más importante**: es donde vive todo lo escrito
   esta noche (el instante límite de `:137`, `expired()` `:220-222`, la guillotina
   `:196-200`, `literal()` `:169-177`, `authority()` `:179-181`, `isTextual()` y el bucle
   de `:205-209`).
2. `adapter.persistence.PostgresExternalCalendarStore` — `commitSuccess` (el `imported = ?`
   que **sólo** mata la prueba nueva de @s30), `writing`, `reading` y `events`.
3. `adapter.connectors.AesGcmSecretCipher` y `adapter.connectors.ConnectorKeyRing` — el
   motivo del hallazgo 7; sus verdugos ya se ejecutan en esta puerta.
4. `application.SyncExternalCalendar`, `application.SaveExternalCalendar`,
   `application.ReadExternalCalendarEvents`, `application.OutboundHostGuard`,
   `application.PublicAddressPolicy`.
5. `domain.IcsFeed`, `domain.ExternalCalendarInput`, `domain.ExternalCalendarSnapshot`,
   `domain.ExternalEventsRange`.
6. `adapter.http.ExternalCalendarController` y `adapter.http.ConnectorsGate`.

Dos avisos de lectura del informe: `adapter.config.ApplicationConfiguration*` está en el
ámbito y es una clase compartida por todo el producto, así que sus supervivientes deben
reportarse **aparte** y no computarse como deuda de la 28; y `ExternalCalendarTodayApiTest`
(ciclo 9) **no** moverá el marcador, porque `PostgresTodayQueries` y `PostgresBlockStore`
no están en este ámbito. Su valor es de regresión, y así debe leerse.

**Condición C4 — Stryker (`external_calendar-frontend`).** Antes de lanzar, reejecutar
`node --test --test-name-pattern "external calendar Stryker" scripts/project.test.mjs`.
Hoy está verde: los cuatro rangos apuntan a
`externalCalendar = route === "/calendario-externo"` (`App.tsx:46`), a su rama de
`section` (`:59-81`), a su rama de render (`:92-151`) y al `RouteLink` con el
`aria-current` (`workspace.tsx:81-86`). Si `App.tsx` o `workspace.tsx` se han tocado desde
este dictamen —la feature 29 va a añadir una ruta—, ese test se pone rojo y **la campaña
no debe lanzarse hasta recalcular los rangos**. En el informe debe constar **mutantes
mayores que cero** en `src/external-calendar-api.ts`, `src/external-calendar.tsx`,
`src/today-external-calendar.tsx` y en los cuatro rangos, y hay que verificar que
**ningún** mutante de `App.tsx` o `workspace.tsx` cae sobre una sentencia `import`: ése es
el síntoma exacto del rango desplazado.

**Condición C5.** Dejar la evidencia en `progress/mutation_external_calendar_backend.md` y
`progress/mutation_external_calendar_frontend.md` —hoy no existe ninguno de los dos—, con
la lista de supervivientes y, por cada uno, o la prueba que lo mata o el motivo escrito
por el que es equivalente.

---

## 7. Checkpoints

- C1 Contrato Gherkin destilado y aprobado: **[x]** con salvedad — la ampliación del ciclo
  4 espera ratificación humana (condición C2).
- C2 Cobertura de escenarios, 40 de 40 con oráculo que puede fallar: **[x]**
- C3 Disciplina TDD: rojo acreditado ciclo a ciclo, y ninguna producción sin prueba que la
  pida: **[x]**
- C4 Calidad de oráculos, sin placebos ni títulos que prometan de más: **[x]**
- C5 Accesibilidad, matriz de 30 principios más evidencia en navegador: **[x]** con deuda
  declarada
- C6 Seguridad: **[ ]** — plazo del cuerpo cerrado; B3 en conflicto normativo (C1)
- C7 Mutación: **[ ]** — pendiente de campaña (C3, C4, C5)

## 8. Resumen de condiciones

1. **C1 (bloqueante):** resolver el conflicto B3 entre `project-spec.md:2492` y
   `deploy/EGRESS.md:10-21` por un lado, y `HttpCalendarFeed` más
   `docs/external-calendar.md:40-45` por otro. Si se ratifica el anclaje, añadir la prueba
   TLS/SNI que hoy no existe.
2. **C2 (bloqueante):** ratificación humana de la ampliación de contrato del ciclo 4
   (`features/external_calendar.feature:13-14` y la fila nueva de @s12 en `:199`).
3. **C3, C4 y C5 (bloqueantes):** campaña de mutación con 80 % mínimo por capa **y** con
   las clases y ficheros obligatorios recibiendo mutantes, sin mutantes sobre sentencias
   `import`, con la evidencia escrita.
4. **No bloqueante, a registrar:** el residuo de @s25 (5.1); la revisión con lector de
   pantalla y Firefox/WebKit como deuda declarada; y el efecto de ámbito JVM de
   `jdk.httpclient.allowRestrictedHeaders` (4.2, punto 2).

Cumplidas las tres primeras, la 28 puede pasar a `done`. Mientras tanto,
`feature_list.json` la mantiene correctamente en `in_progress`.

Trabajo revisado con lente de artesano: el carril no sólo cerró sus diecinueve hallazgos,
sino que al ejecutar por primera vez lo que antes sólo se declaraba encontró dos oráculos
que mentían (la espera negativa de «Sincronizando» y el texto al 200 % que nunca amplió) y
un hueco de contrato que nadie le señaló (@s30). Eso es exactamente lo que se le pide a
una bitácora, y por eso el veredicto es condicionado y no rechazado: lo que queda abierto
son dos decisiones del propietario y una campaña que no es suya.
