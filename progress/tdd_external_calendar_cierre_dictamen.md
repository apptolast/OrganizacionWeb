# Cierre del dictamen de la feature 28 (calendario externo)

Carril B, worktree `C:/Users/vhurt/ow-worktrees/external-calendar`, rama
`claude/external-calendar`, base `c5f9f93`. `E2E_WEB_PORT=18092`.

Fuente del encargo: `progress/carriles/dictamen_f28.md` (19 hallazgos). Ya
estaban cerrados en la base los hallazgos **2, 7, 16 y 17**; no se tocan.

Reglas de operación aplicadas: `progress/carriles/REGLAS.md`. Backend siempre
por clase concreta, nunca la suite entera.

## Estado

| # | Gravedad | Estado | Ciclo |
| --- | --- | --- | --- |
| 1 | bloqueante | **CERRADO** | ciclo 10 |
| 2 | bloqueante | ya cerrado en la base | — |
| 3 | bloqueante | **CERRADO** | ciclo 5 |
| 4 | bloqueante | **fuera de alcance**: lo hace otro carril en `e2e/external-calendar-native-zoom.spec.mjs` (instrucción del coordinador) | — |
| 5 | bloqueante | **CERRADO** | ciclo 10 |
| 6 | bloqueante | **CERRADO** | ciclo 7 |
| 7 | bloqueante | ya cerrado en la base | — |
| 8 | alta | **CERRADO** | ciclo 2 |
| 9 | alta | ya cerrado en la base (es el hallazgo 2) | — |
| 10 | alta | **CERRADO** | ciclo 10 |
| 11 | alta | **CERRADO** | ciclo 9 |
| 12 | alta | **CERRADO** | ciclo 8 |
| 13 | alta (media segun el verificador) | **CERRADO** | ciclo 11 |
| 14 | alta | **CERRADO** (amplía contrato) | ciclo 4 |
| 15 | media | **CERRADO** | ciclo 3 |
| 16 | media | ya cerrado en la base | — |
| 17 | media | ya cerrado en la base | — |
| 18 | media | **CERRADO** | ciclo 1 |
| 19 | baja (media segun el verificador) | **CERRADO** | ciclo 6 |

---

## Ciclo 1 — hallazgo 18: el gate de mutación no muta la ruta ni la entrada de navegación (@s37)

**Qué decía el dictamen.** `frontend/stryker.external-calendar.config.json`
sólo listaba los tres módulos propios del carril. Los cuatro tramos que la
feature 28 introdujo en ficheros compartidos —la ruta, su rama de `section`, su
rama de render y el `RouteLink` con el `aria-current` que @s37 exige por
nombre— quedaban fuera del alcance, contra la convención viva de
`stryker.appearance.config.json` y `stryker.ics-calendar.config.json`.

**ROJO.** Se escribió primero el oráculo en `scripts/project.test.mjs`:

1. se amplió `external calendar Stryker configuration mutates only its own files`
   con los cuatro rangos esperados;
2. se añadió una prueba nueva,
   `external calendar Stryker ranges still cover the route and the navigation entry (@s37)`,
   que **no se cree los números**: recorta `frontend/src/App.tsx` y
   `frontend/src/workspace.tsx` por cada rango `linea:columna-linea:columna`
   (0-based en columnas, fin exclusivo, como el resto del repositorio) y
   comprueba que lo recortado es de verdad ese tramo.

Ejecución antes de tocar la configuración:

```
node --test --test-name-pattern "external calendar Stryker" scripts/project.test.mjs
# tests 2 / pass 0 / fail 2
# "faltan tramos de App.tsx o workspace.tsx"  0 !== 4
```

**VERDE.** Se añadieron al array `mutate` los cuatro rangos, recalculados sobre
el árbol actual (no copiados de la bitácora vieja, que estaba desfasada):

```
src/App.tsx:46:8-46:58        externalCalendar = route === "/calendario-externo"
src/App.tsx:59:14-81:40       su rama de la cadena de `section` hasta `: null`
src/App.tsx:92:10-151:7       su rama de la cadena de render
src/workspace.tsx:81:10-86:22 el RouteLink completo, con el ternario del aria-current
```

`# tests 2 / pass 2 / fail 0`.

**Prueba de que el oráculo discrimina** (rojo provocado a mano sobre producción,
no sobre el test). Se desplazó un solo rango una línea,
`src/App.tsx:46:8-46:58` → `src/App.tsx:47:8-47:58`, y la prueba nueva falló por
el motivo correcto:

```
+ actual: 'automations = route === "/automatizaciones";'
- expected: 'externalCalendar = route === "/calendario-externo"'
```

Es decir: si App.tsx se reordena y los rangos dejan de apuntar al tramo de la 28,
la puerta se pone roja en vez de mutar código ajeno en silencio. Se restauró el
rango y se volvió a verde.

**Ficheros compartidos tocados** (aviso de REGLAS.md §6): `scripts/project.test.mjs`
—sólo el bloque de esta feature, líneas 2101-2160— y
`frontend/stryker.external-calendar.config.json`, que es propio del carril.
`frontend/src/App.tsx` y `frontend/src/workspace.tsx` **no** se han modificado:
sólo se leen desde la prueba.

**Lo que este ciclo NO cierra.** El dictamen pedía además una prueba de @s37 para
la tercera entrada, «retorno tras iniciar sesión». Va aparte, con el hallazgo 15,
que también vive en la transición de sesión.

---

## Ciclo 2 — hallazgo 8: @s38 filas 6, 7 y 8, «Sincronizando» y el bloqueo de controles

**Qué decía el dictamen.** El Then de @s38
(`features/external_calendar.feature:515`) exige para las tres filas de
«Sincronizar ahora» que «aparece "Sincronizando" antes de 400 ms, se envía
exactamente una petición y los controles quedan bloqueados hasta la respuesta».
Ninguna de las tres pruebas de sincronización retenía la respuesta, así que
ninguna podía observar el estado intermedio: dos mutantes sobrevivían.

**ROJO (dos mutantes, provocados a mano sobre producción).**

1. Comentar `setAnnouncement("Sincronizando…")` en
   `frontend/src/external-calendar.tsx:215`:
   `TestingLibraryElementError: Unable to find an element with the text: Sincronizando…`
   → `Tests 1 failed | 24 passed`.
2. Comentar `setBusy("syncing")` en `:214`:
   `Error: expect(element).toBeDisabled()`
   → `Tests 1 failed | 24 passed`.

Ambos eran exactamente los dos mutantes que el dictamen señalaba como
supervivientes. Restaurada la producción: `Tests 25 passed (25)`.

**VERDE.** Prueba nueva
`@s38 anuncia Sincronizando, envía una sola petición y bloquea los controles`:
retiene la respuesta del `POST /sync` con una promesa liberada a mano y, con la
petición en vuelo, afirma

- `findByText("Sincronizando…")`;
- «Sincronizar ahora», «Eliminar suscripción» y «Guardar» `toBeDisabled()`;
- «Etiqueta» y «Dirección secreta iCal` con atributo `readonly`;
- un **segundo** clic sobre «Sincronizar ahora» y
  `calls.filter(POST)).toHaveLength(1)` — esta es la aserción que mata el mutante
  de `setBusy`, porque sin él ni el botón se bloquea ni el guardián de reentrada
  de `:212` guarda nada;
- tras liberar: «Sincronizado.», botón habilitado y campo sin `readonly`.

Además se añadió el recuento de POST a las otras dos filas del Examples (la de
`lastStatus FAILED` y la de `404 EXTERNAL_CALENDAR_NOT_CONFIGURED`), que
inspeccionaban el cuerpo con `find` sin contar nunca las peticiones.

`pnpm --dir frontend exec vitest run src/external-calendar.test.tsx` →
**25 pruebas, 25 en verde**. Medido, no declarado: el fichero declara 24 bloques
`it(`/`it.each(` (`grep -cE "^\s*it(\.each)?[(\[]"`), uno de ellos un `it.each`
de dos filas, de donde salen las 25 que ejecuta vitest. Eran 24 antes de añadir
la prueba nueva y con los mutantes de arriba el marcador fue
`1 failed | 24 passed`.

**Pendiente asociado, anotado y no silenciado.** La línea 84 de
`e2e/external-calendar-ux-audit.spec.mjs`
(`await expect(page.getByRole("status")).not.toHaveText("Sincronizando…")`) es
una espera negativa que pasa de inmediato si el anuncio no aparece jamás. Se
sustituye al reescribir la auditoría (hallazgos 1/4/5/10/12/13), no aquí.

**Decisión de contrato registrada.** El formulario declara
`aria-busy={busy === "saving"}` y no hay equivalente para `syncing`. El Then de
@s38 exige «los controles quedan bloqueados», que es lo que se ha medido
(`disabled` y `readonly`); `aria-busy` no lo pide el contrato, así que no se ha
tocado producción para añadirlo.

---

## Ciclo 3 — hallazgo 15: @s39 fila 4 (cierre de sesión) y la tercera entrada de @s37

**Qué decía el dictamen.** La fila 4 de @s39
(`features/external_calendar.feature:538`) exige que al cerrar sesión desaparezcan
host, cola, contadores y lista, que la ruta se reinicie a `/` y que no se conserve
borrador. No había ninguna prueba de cierre de sesión para /calendario-externo, ni
unitaria ni E2E. El verificador precisó dos cosas que se han respetado:

- **no** añadir `expect(window.location.pathname).toBe("/")` a la prueba de 401 de
  `external-calendar.test.tsx:266`: ese fichero nunca toca `window.history`, así
  que la aserción sería un placebo;
- escribir la prueba sobre **`SessionGate`**, y sobre la transición real, porque
  «la ruta se reinicia a /» no ocurre al cerrar sino al volver a abrir sesión: el
  único `replaceState(null, "", "/")` del producto (`use-session.ts:49`) se dispara
  en anónimo→autenticado y sólo si la ruta actual NO está en `isPrivateRoute`.

**VERDE.** Prueba nueva en `frontend/src/external-calendar-route.test.tsx`:
`@s39 al cerrar sesión desaparecen los datos y el retorno tras iniciar sesión reinicia la ruta a / (@s37)`.
Parte de `/calendario-externo` con suscripción sincronizada y un evento en lista,
escribe un borrador en «Dirección secreta iCal», pulsa «Cerrar sesión» y exige que
desaparezcan host, cola, contadores, lista y el borrador; después vuelve a iniciar
sesión y exige `window.location.pathname === "/"`; y por último entra otra vez en
la vista para comprobar que el campo de dirección está vacío.

De paso cierra la **tercera entrada de @s37**, «retorno tras iniciar sesión», que
el dictamen señalaba como no cubierta ni siquiera por oráculo (hallazgo 18).

**ROJO demostrado sobre producción.** Se añadió `path === "/calendario-externo"`
a la lista blanca `isPrivateRoute` de `frontend/src/use-session.ts` —exactamente el
cambio que la feature 24 hizo con `/integraciones/api` y que hoy nadie detectaba—:

```
FAIL src/external-calendar-route.test.tsx > @s39 al cerrar sesión ...
AssertionError: expected '/calendario-externo' to be '/'
Tests  1 failed | 4 passed (5)
```

Restaurado `use-session.ts`: `Tests 5 passed (5)`.

**Límite declarado.** La mitad «desaparecen los datos» está garantizada por
construcción (`session-gate.tsx` desmonta `<App/>`), así que esa aserción no
discrimina por sí sola: lo que discrimina, y lo que el ciclo fija de verdad, es la
ruta. Queda escrito para que nadie la lea como más de lo que es.

---

## Ciclo 4 — hallazgo 14: la lectura del cuerpo del feed no tenía ningún plazo

### AMPLIACIÓN DE CONTRATO — léase entero antes de aprobar

Este ciclo **añade una cláusula al contrato** `features/external_calendar.feature`.
Se declara aquí en voz alta para que el propietario pueda revocarla si no la
quiere, tal como pide REGLAS.md §8.

**Qué había.** La cabecera del contrato (línea 12) prometía «timeout de 5 s» sin
decir a qué se aplicaba, y el único ejemplo de plazo de @s12 era «200 tras 6 s sin
enviar cabeceras → FEED_UNREACHABLE». `docs/external-calendar.md:41-44` iba más
lejos y afirmaba **dos veces** un plazo de 5 s sobre lo que se hace con la
respuesta, una de ellas como mitigación explícita del rebinding aceptado como
riesgo residual.

**Qué hacía el código.** `HttpCalendarFeed` usaba `BodyHandlers.ofInputStream()`.
Con ese handler el temporizador de `HttpRequest.timeout` se cancela en cuanto
llegan las cabeceras (`MultiExchange.responseAsyncImpl` llama a `cancelTimer()`
antes de encadenar `readBodyAsync`), y el bucle de lectura sólo cortaba por
tamaño. Es decir: **ni un byte del cuerpo estaba cubierto por plazo alguno**. La
documentación afirmaba una protección inexistente.

**Por qué se amplía el contrato en vez de dejarlo abierto.** El defecto no era
sólo documental: cualquier cuenta autenticada con un host público propio podía
dejar hilos de petición de Tomcat bloqueados para siempre, y @s35 lo amplifica
porque `lastAttemptAt` sólo se escribe en `commit(...)`, después de que
`feed.fetch` retorne — mientras una descarga cuelga la suscripción sigue
eternamente «rancia» y **cada** carga de Hoy dispara otra que también cuelga. Es
la misma familia que la enmienda de seguridad **B1 de los webhooks**: un receptor
lento que agota recursos del emisor. Cerrarlo exige una obligación que el contrato
no tenía escrita, así que se escribe.

**Cambios exactos en el contrato.** En `features/external_calendar.feature`:

1. Cabecera, se parte la línea 12 y se sustituye «timeout de 5 s» por:
   «El plazo de 5 s es del intercambio completo: conexión, cabeceras y lectura del
   cuerpo. Un proveedor que envía las cabeceras y luego gotea el cuerpo sin
   cerrarlo se corta al vencer ese plazo con FEED_UNREACHABLE; ninguna descarga
   puede retener un hilo más de 5 s.»
2. `@s12`, fila nueva en el Examples:
   `| 200 text/calendar que envía las cabeceras y luego gotea el cuerpo sin cerrar | FEED_UNREACHABLE |`

**Si el propietario revoca la ampliación**, hay que revertir además el cambio de
`docs/external-calendar.md` y dejar escrito que el plazo cubre sólo las cabeceras;
lo que **no** puede quedarse es el estado anterior, en el que la documentación
prometía un plazo que el código no daba.

### El ciclo

**ROJO.** Prueba nueva `s12_aBodyThatDripsForeverIsUnreachable` en
`backend/src/test/java/.../feed/HttpCalendarFeedTest.java`: un `HttpServer` que
envía `200 text/calendar` al instante y luego escribe un byte cada 50 ms sin
cerrar nunca, contra un `HttpCalendarFeed` de 300 ms. Lleva `@Timeout(15)` porque
contra el código anterior **no terminaría jamás**, y eso es precisamente la prueba
de que el hueco era real:

```
gradlew test --tests "...HttpCalendarFeedTest" --no-daemon
HttpCalendarFeedTest > s12_aBodyThatDripsForeverIsUnreachable() FAILED
    java.util.concurrent.TimeoutException at ArrayList.java:1604
32 tests completed, 1 failed
```

**VERDE.** En `HttpCalendarFeed`:

- se calcula `deadline = System.nanoTime() + timeout.toNanos()` **antes** de
  `send`, de modo que el plazo es del intercambio completo y lo que consuman las
  cabeceras se descuenta del cuerpo;
- el bucle de lectura comprueba el instante límite en cada trozo (caso del goteo);
- y además se programa el cierre del cuerpo en ese instante con un
  `ScheduledExecutorService` de un solo hilo demonio, porque un proveedor que se
  calla del todo dejaría el `read` bloqueado sin llegar nunca a la comprobación.
  Cerrar el `HttpResponseInputStream` desbloquea al lector con `IOException`, que
  se traduce a `FEED_UNREACHABLE` cuando el plazo ya venció.

`32 tests completed`, `BUILD SUCCESSFUL`. La prueba nueva afirma además que
`fetch` retorna en menos de 5 s con un plazo de 300 ms, no sólo que el código sea
`FEED_UNREACHABLE`: sin la cota de tiempo el oráculo no distinguiría un corte de
una espera larga.

**Documentación alineada.** `docs/external-calendar.md` deja de prometer un plazo
que no existía y explica el mecanismo (por qué `HttpRequest.timeout` no basta con
`ofInputStream`).

**Alcance no invadido.** `JdkWebhookSender` (feature 25) usa
`BodyHandlers.discarding()` con `EXCHANGE_TIMEOUT`, así que no comparte este
defecto y no se ha tocado: es del carril de webhooks.

---

## Ciclo 5 — hallazgo 3: la matriz de los 30 principios no existía

**Qué decía el dictamen.** `AGENTS.md:51` obliga a recorrer las 30 filas de
`docs/ux-requirements.md` y **prohíbe** inferir cumplimiento desde axe. El
verificador comprobó, además, que la prueba no es la ausencia del fichero con ese
nombre exacto sino la búsqueda por contenido: los nombres propios de la matriz
(Zeigarnik, Tesler, Fitts, Hick, Jakob, Miller, Parkinson, Occam, Von Restorff…)
aparecían en 30 ficheros del repositorio y en **ninguno** de external_calendar.

**Hecho.** `progress/ux_external_calendar.md`, con la estructura de
`progress/ux_integration_api.md` y `progress/ux_ics_calendar.md`: sección
«Evidencia y límites» **antes** de los resultados, y después la tabla con
exactamente una fila por principio.

Comprobado por script, no a ojo: se extraen los 30 nombres de principio de
`docs/ux-requirements.md` y los 30 de la tabla nueva, y se cruzan.
`faltan: []`, `sobran: []`.

**Cifras medidas, no declaradas.** Cada número de la sección de evidencia sale de
una ejecución real del día y está desglosado fichero a fichero:

| Fichero | Pruebas |
| --- | --- |
| `frontend/src/external-calendar.test.tsx` | 25 |
| `frontend/src/external-calendar-api.test.ts` | 42 |
| `frontend/src/today-external-calendar.test.tsx` | 11 |
| `frontend/src/external-calendar-route.test.tsx` | 5 |
| **los cuatro en una sola invocación** | **83** (`Test Files 4 passed (4)` / `Tests 83 passed (83)`) |
| `backend …/feed/HttpCalendarFeedTest` | 32 (`32 tests completed`, `BUILD SUCCESSFUL`) |

El desglose suma exactamente el total, de modo que la cifra es reproducible y no
una afirmación de autoridad.

**Lo que el documento NO afirma, y lo dice antes de la tabla.** Que axe no
certifica lector de pantalla; que la geometría sólo se ha ejercido sobre el estado
vacío (hallazgo 5, marcado en las filas Fitts y Von Restorff como «parcialmente
verificado»); que el zoom nativo al 200 % **no** se ha ejecutado y lo cubre otro
carril; que no hay pasada de tema oscuro ni forced-colors (hallazgo 13); que todo
lo de navegador es Chromium; y que no ha habido estudio con usuarios.

Recuento de la tabla: 30 filas — 22 verificadas con prueba que puede fallar, 2
parcialmente verificadas con su hallazgo abierto citado, 3 heurísticas declaradas
como tales y 3 no aplicables con motivo escrito.

**Nota de alcance.** Por instrucción del coordinador, el hallazgo 4 (zoom nativo
al 200 %) lo cierra otro carril en un fichero nuevo,
`e2e/external-calendar-native-zoom.spec.mjs`. Este carril no lo crea, para no
duplicar ni chocar en la integración; la matriz UX lo deja escrito como pendiente
con el nombre del fichero que lo cubrirá.

---

## Ciclo 6 — hallazgo 19: @s25, la lectura concurrente durante la sincronización

**Qué decía el dictamen.** @s25 tiene tres Then y el segundo —«una lectura
concurrente durante la sincronización ve la lista anterior completa o la nueva
completa, nunca una vacía ni mezclada»— no tenía **ningún** oráculo: los doce
ficheros de prueba del carril no contenían un solo `Thread`, `ExecutorService`,
`CountDownLatch` ni conexión secundaria. La garantía existía por construcción
(`PostgresExternalCalendarStore.commitSuccess` envuelve UPDATE + DELETE + insert
en `writing(...)`), pero nada la fijaba, y PIT no la detecta porque sus operadores
no reestructuran transacciones.

**VERDE.** Prueba nueva `s25_aConcurrentReadNeverSeesAnEmptyOrMixedSnapshot` en
`ExternalCalendarPersistenceTest`, con la técnica que el repositorio ya usa en
`HistoryReadTransactionTest`: se subclasifica el `JdbcTemplate` del escritor para
colarse **dentro** de su transacción, justo después del
`DELETE FROM external_calendar_events` y antes de insertar la lista nueva —el
único instante en que la instantánea está vacía—, y desde **otro hilo**, y por
tanto desde otra conexión, se lee con `store().events(...)`. Se afirma que esa
lectura devuelve exactamente `["u1","u2"]` (la lista anterior completa, ni vacía
ni mezclada) y que al terminar la escritura queda exactamente `["u3","u4"]`.

`gradlew test --tests "…ExternalCalendarPersistenceTest" --no-daemon` →
`BUILD SUCCESSFUL`, 19 pruebas.

**ROJO demostrado sobre producción.** Se sacaron el DELETE y el `insert` fuera del
lambda de `writing(...)` en `commitSuccess` —exactamente el cambio que el dictamen
describe como indetectable hoy— y quedaron en auto-commit. La prueba cayó por el
motivo correcto:

```
ExternalCalendarPersistenceTest > s25_aConcurrentReadNeverSeesAnEmptyOrMixedSnapshot() FAILED
org.opentest4j.AssertionFailedError:
Expecting actual:
  []
to contain exactly (and in same order):
  ["u1", "u2"]
19 tests completed, 1 failed
```

La lectura concurrente vio **la lista vacía**: justo el estado que el contrato
prohíbe. Producción restaurada al estado de HEAD (`git status` sólo marcaba el
fichero de prueba) y verde recuperado.

**Lo que este ciclo NO cierra.** El verificador señalaba además que
`s25_syncingDoesNotTouchTheOutbox` sólo comprueba `SELECT count(*) FROM
outbox_events = 0`, y no «ninguna otra tabla ni historial» como promete su título
y el contrato. Queda abierto y anotado, no silenciado.

---

> **NOTA DEL 10-09-2026.** Esta sección se escribió al cortar la sesión del 9 de
> septiembre y **ha quedado obsoleta**: los seis que declaraba abiertos (1, 5, 10, 11,
> 12 y 13) están cerrados en los ciclos 8, 9, 10 y 11, escritos más abajo. Se conserva
> sin retocar porque su valor es histórico: dice qué se sabía y qué se temía entonces.
> El estado vigente es la tabla del principio del fichero.

## Estado al cortar la sesión

**Cerrados por este carril, con rojo demostrado y commit propio:** 18, 8, 15, 14,
3, 19 y **6** (este ultimo en el ciclo 7, escrito mas abajo). **Ya venían cerrados en la base:** 2, 7, 16 y 17 (el 9 es el mismo defecto
que el 2 y quedó cubierto por la prueba de señales que ya existía en
`external-calendar.test.tsx`).

**Abiertos al cortar: 1, 5, 10, 11, 12 y 13.** Lo que hace falta para cada uno:

- **1, 5, 10, 12** (bloqueantes de la auditoría UX): parametrizar la matriz de 14
  anchos y la de texto al 200 % por los cinco estados del Given de @s40 —vacío;
  con suscripción; con error tras sync FAILED; con lista larga de resúmenes
  Unicode, sembrada por `sql()` con INSERT en `external_calendar_events`; y
  guardando, congelado con `page.route` sobre el PUT—, incluyendo el diálogo de
  confirmación abierto para que «Sí, eliminar» y «Cancelar» entren en la medición
  de 44×44 px; y sustituir `noHorizontalScroll` por el `geometry()` de
  `e2e/ics-calendar-ux.spec.mjs` (solapes por pares y recorte por elemento en los
  dos ejes). Requiere levantar la pila con `E2E_WEB_PORT=18092` y ejecutar sólo
  `e2e/external-calendar-ux-audit.spec.mjs`.
- **4** (zoom nativo 200 %): **fuera del alcance de este carril** por instrucción
  del coordinador; lo cubre otro carril en
  `e2e/external-calendar-native-zoom.spec.mjs`.
- ~~**6** (enmienda B3, guardia SSRF)~~: **CERRADO en el ciclo 7**. Lo que sigue es el analisis previo, conservado solo como registro de por que se dudo en empezarlo. El diseño que exige el
  dictamen es que `OutboundGuard` devuelva las direcciones validadas junto al
  veredicto y que `HttpCalendarFeed` conecte contra la dirección literal ya
  validada conservando el nombre en `Host` y en SNI. Se decidió **no empezarlo**
  con el plazo restante: a medias vale cero, y dejar `OutboundGuard` con la
  interfaz cambiada y la conexión sin fijar habría roto el árbol sin cerrar nada.
  Aviso para quien lo retome: `OutboundGuard` sólo lo usan `SaveExternalCalendar`
  y `SyncExternalCalendar` (los webhooks tienen su propio
  `WebhookDestinationGuard`), así que el cambio de interfaz no sale del carril; el
  punto caro es el pinning en el `HttpClient` del JDK, que no admite un resolutor
  por cliente (haría falta `java.net.spi.InetAddressResolverProvider`, que es de
  ámbito JVM, o reescribir la autoridad de la URI con `Host` restringido y
  `SSLParameters.setServerNames`). Y sigue en pie la alternativa (b) del dictamen:
  que el coordinador escriba la resolución sobre B3 con registro en
  `project-spec.md` y en `progress/current.md` más la política de egreso en
  `deploy/`.
- **11** (@s34, cuerpo de Hoy con suscripción): falta la prueba estilo
  `TodayApiTest` con los tres examples, sembrando suscripción e instantánea por
  SQL y comparando el cuerpo de `GET /api/v1/today` byte a byte con R0.
- **13** (tema oscuro y forced-colors): falta la pasada con `emulateMedia`
  siguiendo `e2e/ics-calendar-ux.spec.mjs:304-327`.
- Menor, anotado arriba: `s25_syncingDoesNotTouchTheOutbox` promete más de lo que
  comprueba; y la línea 84 de `e2e/external-calendar-ux-audit.spec.mjs` es una
  espera negativa placebo que debe morir al reescribir la auditoría.

**Puerta de mutación:** no se ha ejecutado ninguna campaña, por instrucción
expresa del coordinador (tardan demasiado para el plazo). Antes de correrla,
comprobar que los patrones PIT de `externalCalendarClasses` resuelven a clases
existentes —el hallazgo 7 ya corrigió los dos globs muertos hacia
`adapter.crypto`— y que los cuatro rangos nuevos de
`stryker.external-calendar.config.json` siguen alineados: la prueba
`external calendar Stryker ranges still cover the route and the navigation entry (@s37)`
lo verifica sin necesidad de ejecutar Stryker.

---

## Ciclo 7 — hallazgo 6: la enmienda B3, entera

**Qué decía el dictamen.** `project-spec.md:2488` aprueba las enmiendas «con
prevalencia sobre el texto de las secciones 25 a 28 escrito antes», y B3 dice: «se
resuelve el nombre una vez, se validan todas las direcciones devueltas y **se
conecta contra la dirección literal ya validada**, conservando el nombre original
en la cabecera Host y en la indicación de servidor de TLS». El código cumplía la
primera mitad y no la segunda: `OutboundHostGuard` descartaba la lista resuelta
(su veredicto era un enum sin datos) y `HttpCalendarFeed` construía
`HttpRequest.newBuilder(URI.create(url))`, con lo que el JDK volvía a resolver el
nombre **por segunda vez e independientemente**. Encima el javadoc y
`docs/external-calendar.md` publicaban como «riesgo residual aceptado» justo lo que
la enmienda había dejado de aceptar, sin resolución del coordinador que lo
autorizara.

**Diseño elegido, y por qué éste.** El dictamen proponía que el veredicto de la
guardia arrastrase las direcciones hasta el feed. Se ha hecho algo equivalente y
más barato de verificar: **quien conecta es quien resuelve**. `HttpCalendarFeed`
recibe ahora el `HostResolver` y la `AddressPolicy` —los mismos beans que usa la
guardia— y en `fetch` resuelve una vez, exige `allMatch(policy::allows)` y
construye la petición contra la dirección literal, con el nombre en `Host` y en
`SNIHostName`. La ventana entre comprobación y uso no se estrecha: **desaparece**,
porque ya no hay dos resoluciones. La guardia de `SaveExternalCalendar` y
`SyncExternalCalendar` se conserva intacta: sigue siendo la que decide si una
suscripción puede guardarse o sincronizarse, y su interfaz no cambia (radio de
cambio pequeño, que con el plazo que quedaba era decisivo).

**VERDE.** Dos pruebas nuevas en `HttpCalendarFeedTest`:

1. `s13_connectsToTheValidatedAddressAndKeepsTheNameInHost`. Pide
   `http://nombre.que.no.resuelve.invalid:<puerto>/cal.ics?tok=WXYZ` con un
   resolutor que devuelve el loopback. **Ese nombre no existe en ningún DNS**: si
   el cliente volviera a resolverlo, la descarga acabaría en `FEED_UNREACHABLE`.
   Se afirma que la descarga llega, que el resolutor se llamó **exactamente una
   vez** y que el servidor recibió `Host: nombre.que.no.resuelve.invalid:<puerto>`.
   Ése es el reenlace cerrado, medido.
2. `s13_aNameThatResolvesToAForbiddenAddressNeverConnects`. Con una política que
   rechaza todo: `FEED_REJECTED` y `received.isEmpty()` — no se abrió ninguna
   conexión, no basta con fallar después.

`gradlew test --tests "…HttpCalendarFeedTest" --tests "…ExternalCalendarWiringTest"`
→ `BUILD SUCCESSFUL`, 34 pruebas en la clase del feed.

**ROJO demostrado sobre producción.** Se volvió a poner
`HttpRequest.newBuilder(target)` —conectar por nombre, el código anterior—:

```
HttpCalendarFeedTest > s13_connectsToTheValidatedAddressAndKeepsTheNameInHost() FAILED
org.opentest4j.AssertionFailedError: Unexpected type,
expected: <FeedFetch.Downloaded> but was: <FeedFetch.Failed>
34 tests completed, 1 failed
```

El fallo es el correcto: sin anclaje, el JDK intenta resolver el nombre inventado,
no puede, y devuelve `FEED_UNREACHABLE`. Producción restaurada y verde recuperado.

**La documentación deja de mentir.** El javadoc de `OutboundHostGuard` ya no habla
de riesgo residual aceptado —dice dónde vive la otra mitad de B3— y
`docs/external-calendar.md` pasa de «*Riesgo residual aceptado*» a «**cerrado, no
aceptado**», con el mecanismo explicado.

**Límites que se dejan escritos, no disfrazados.**

- Si el nombre resuelve a varias direcciones se usa la primera y no se reintenta
  con las demás. Todas estaban validadas: es pérdida de tolerancia a fallos, no de
  seguridad.
- La parte de **SNI y verificación de certificado no tiene prueba propia**: el
  arnés de esta clase habla HTTP en claro contra `127.0.0.1`. Lo verificado es el
  anclaje de dirección y la cabecera `Host`; el apretón de manos TLS con SNI
  fijado está implementado (`SSLParameters.setServerNames` +
  `setEndpointIdentificationAlgorithm("HTTPS")`) pero **no ejercido por ninguna
  prueba**. Quien quiera cerrarlo del todo necesita un servidor TLS con
  certificado propio en el arnés.
- Enviar `Host` a mano exige `jdk.httpclient.allowRestrictedHeaders=host`. La
  clase lo añade en un bloque estático **sin pisar** lo que declare el despliegue,
  pero el cliente del JDK lee esa propiedad al cargar su clase de utilidades: si
  otro componente creara un `HttpClient` antes, la autorización llegaría tarde. En
  ese caso `HttpRequest.Builder` lanza `IllegalArgumentException` y la descarga
  devuelve `FEED_UNREACHABLE` — **no se conecta sin `Host`**, que sería conectar
  contra el servidor equivocado. Recomendación para el despliegue: declarar la
  propiedad también en los argumentos de la JVM.
- **Alcance no invadido:** la feature 25 tiene el mismo defecto en
  `JdkWebhookSender.guardDestination`, y el dictamen pedía cubrir 25 y 28 a la vez.
  No se ha tocado: es de otro carril y REGLAS.md §9 lo prohíbe. Queda anotado aquí
  para que el coordinador lo enrute.

---

## Ciclo 8 — hallazgo 12: el oráculo medía el ancho de la página y el título prometía solapes

**Qué decía el dictamen.** El título de la prueba decía «sin solapes ni scroll
horizontal en toda la matriz», pero el cuerpo llamaba a `noHorizontalScroll()`, que
entero era comparar `document.documentElement.scrollWidth` con `clientWidth`. Ni
`scrollHeight`, ni recorte por elemento, ni una sola comparación de rectángulos.
Agravante: la línea que fuerza `height: 400` a 768 px pone justo el caso donde el
recorte vertical es más probable, y no se medía nada vertical.

**VERDE.** `noHorizontalScroll` y `controlsAreLargeEnough` se sustituyen por un
`geometry(page)` calcado del de `e2e/ics-calendar-ux.spec.mjs`, que devuelve
desbordamiento de página, objetivos menores de 44 px, controles fuera del viewport,
**recorte por elemento en los dos ejes** y **solapes por pares de rectángulos**; y
un `nothingBreaksAt(page, label)` que afirma las cinco cosas con un mensaje propio
por medida. El título pasa a «sin solapes, recortes ni scroll horizontal», que es lo
que el cuerpo ahora sí asegura.

**Excepción documentada, no lista blanca silenciosa.** `INPUT`, `SELECT` y
`TEXTAREA` se excluyen **del recorte** porque su `scrollWidth > clientWidth` con un
valor largo es su comportamiento correcto: el usuario recorre el valor con el cursor
y no pierde texto. Contarlo sería falso positivo garantizado en una pantalla cuyo
dato principal es una dirección iCal larga. Siguen entrando en objetivos, en solapes
y en escape del viewport: la excepción es de una medida, no de todas.

**Ejecutado de verdad**, con `E2E_WEB_PORT=18092` y sólo mi spec:

```
E2E_WEB_PORT=18092 pnpm test:e2e -- e2e/external-calendar-ux-audit.spec.mjs
4 passed (15.8s)
```

**ROJO acreditado con mutación de control** (la técnica que el carril de
automatizaciones usó hoy). Se añadió a `frontend/src/styles.scss` la regla
`.external-calendar .field { overflow: hidden; max-height: 40px; }` y cayeron **2 de
las 4** pruebas, nombrando el recorte vertical elemento a elemento:

```
+   "DIV:Etiqueta [alto 81 en 40]",
+   "DIV:Dirección secreta iCalEn Goo [alto 145 en 40]",
2 failed / 2 passed
```

Con el oráculo anterior ese mismo CSS no habría movido una sola aserción: el
documento no desborda a lo ancho y los botones seguían midiendo 44 px. CSS
restaurado y verde recuperado.

**De paso, la espera placebo que anoté en el ciclo 2.** La línea
`expect(page.getByRole("status")).not.toHaveText("Sincronizando…")` era una espera
negativa que en Playwright pasa de inmediato si el anuncio no aparece jamás. Se
sustituye por dos esperas positivas: primero que el anuncio **aparezca**, después
que ceda al resultado, con plazo de 20 s porque el contrato admite hasta 5 s de
descarga. No es cosmética: al ejecutar por primera vez la auditoría, esa línea
**falló de verdad** con `Received: "Sincronizando…"`, porque el plazo total del
cuerpo que introdujo el ciclo 4 hace que sincronizar contra un feed inalcanzable
tarde ahora los cinco segundos completos. Es decir: la auditoría, ejecutada por fin,
encontró algo.

**Nota de higiene.** Una invocación de shell mal citada dejó un fichero vacío
llamado `clientWidth` en la raíz del worktree y relanzó una vez la pila de E2E. El
fichero se ha borrado antes de commitear y la pila se bajó sola; ninguna de las dos
cosas tocó código.

---

## Ciclo 9 — hallazgo 11: @s34 nunca ejecutaba su Given

**Qué decía el dictamen.** El Outline @s34 tiene tres *examples* y dos Then, y ni el
Given («persona-a suscribe un feed con &lt;evento&gt; ya sincronizado») ni ninguno de
los dos Then se ejecutaban en ninguna parte. `ExternalCalendarIsolationTest` cierra
el vector de **código** con dos reglas ArchUnit, y `e2e/external-calendar.spec.mjs`
mira Hoy **sin** suscripción. El vector de **datos** —unir `external_calendar_events`
en una consulta SQL de Hoy o de la planificación— no añade ninguna dependencia de
clase, así que ArchUnit no lo ve, y ningún test siembra esas filas, así que tampoco
lo ve nadie más. Los tres *examples* colapsaban en el mismo caso nulo.

**VERDE.** `backend/src/test/java/com/apptolast/organization/adapter/ExternalCalendarTodayApiTest.java`,
un `@SpringBootTest` con Testcontainers y MockMvc, parametrizado con los tres
*examples* del contrato. Por cada uno: se siembra el bloque propio de 10:00Z a 11:00Z
con presupuesto de 120 minutos, se captura **R0** de `GET /api/v1/today`, se inserta
por SQL la suscripción y el evento del *example* en `external_calendar_subscriptions`
y `external_calendar_events`, se repite `GET /api/v1/today` y se compara **la cadena
entera** con R0, y se planifica el hueco del *example* exigiendo **201** y ausencia
de `BLOCK_OVERLAP`.

El reloj se para a las **10:30Z**, dentro del bloque propio, no a las 12:00Z: así
`currentBlockId` y `closingAt` —los dos campos que el Then nombra— llevan valor
(`ownBlock` y `2030-01-07T11:00:00Z`) en vez de `null`, que es el valor que cualquier
regresión conservaría por accidente. Se afirman además los quince campos,
`plannedSeconds` 3600 y `remainingSeconds` 3600 que el contrato escribe con número.

**ROJO acreditado con dos mutaciones de control simultáneas**, las mismas que el
dictamen señalaba como invisibles:

1. `PostgresTodayQueries`: `AND NOT EXISTS (SELECT 1 FROM external_calendar_events e
   WHERE e.owner_id=owner_project.owner_id AND e.start_at<…end_at AND e.end_at>…start_at)`
   añadido al `WHERE` de los ítems de Hoy.
2. `PostgresBlockStore.ownerBlocks`: `UNION ALL` que sintetiza cada fila de
   `external_calendar_events` como si fuera un bloque planificado.

Resultado: **3 de 3 en rojo**, y cada mitad del Then delató su mutación.

```
Hoy cambió al haber una suscripción con un evento ya sincronizado
expected: "{…,"plannedSeconds":3600,…,"items":[{…}]}"
 but was: "{…,"items":[]}"

la planificación tropezó con el evento externo:
{"code":"BLOCK_OVERLAP","conflict":{"id":"bb8fa9c7-060f-bb56-ff3f-761cb8e94cb0",…}}
expected: 201
 but was: 409
```

**Y la prueba de que el guardarraíl viejo no bastaba**: con las dos mutaciones
puestas, `ExternalCalendarIsolationTest` (las dos reglas ArchUnit de @s34) siguió en
`BUILD SUCCESSFUL`. Ese era exactamente el hueco: el vector de datos no toca ninguna
clase del carril.

Producción restaurada byte a byte (`git status` limpio en los dos ficheros) y verde
recuperado: `3 tests completed, 0 failed`.

**Corregida la bitácora que mentía.** `progress/tdd_external_calendar.md:16` afirmaba
«HTTP MockMvc … Hoy intacto — @s34» cuando no existía ningún MockMvc de @s34; se
anota la corrección fechada y se añade la clase nueva al mapa de trazabilidad (:337).

**Coste declarado.** Una clase de test nueva es un contenedor PostgreSQL más en la
suite completa (49 → 50). Se asume: la alternativa era escribir en `TodayApiTest`,
que no es fichero de este carril y habría chocado en la integración.

**Previsión de mutación (PIT).** Esta prueba no entra en `externalCalendarClasses`
—`PostgresTodayQueries` y `PostgresBlockStore` son del carril de Hoy y de bloques—,
así que **no espero que mueva el marcador de la campaña de la 28**. Su valor es de
regresión: es la red que ArchUnit no puede tender.

---

## Ciclo 10 — hallazgos 1, 5 y 10: la pantalla se mide en todos sus estados

Son el mismo problema visto tres veces, y se cierran de una vez: la auditoría medía
el formulario **vacío** y nada más.

**Qué decía el dictamen.** El Given de @s40 nombra cinco estados —vacío, con
suscripción, con error, con lista larga de resúmenes Unicode y guardando—. La matriz
de catorce anchos y la de texto al 200 % hacían `goto` + `expect(getByLabel("Etiqueta")).toBeVisible()`
y medían el formulario de alta vacío. Consecuencia concreta: «Sincronizar ahora» y
«Eliminar suscripción» viven dentro de `{subscription ? …}` y «Sí, eliminar»/«Cancelar»
dentro de `{confirming ? …}` (`frontend/src/external-calendar.tsx:327` y `:374`), así
que los **cuatro** nunca entraban en la medición de 44 × 44 px, en ningún ancho ni en
ninguna prueba.

**VERDE.** Un helper `enter(page, state)` deja la pantalla en cada estado y devuelve
la función que lo deshace:

| Estado | Cómo se construye |
| --- | --- |
| vacío | `goto`, y se afirma el texto del estado vacío |
| con suscripción | `INSERT` por SQL de la suscripción y de 3 eventos; se afirman los 3 `listitem` |
| con error | `INSERT` con `last_status='FAILED'` y `last_error='FEED_HTTP_ERROR'`; se afirma el `role="alert"` |
| lista larga de resúmenes Unicode | `INSERT` de **60** eventos con 400 puntos de código cada uno: 120 de emoji y acentos más una palabra sin espacios de 280 caracteres |
| guardando | `page.route` retiene la respuesta del `PUT` hasta que se sale del estado; se afirman «Guardando…» y el botón deshabilitado |
| confirmando la eliminación | se pulsa «Eliminar suscripción» y se afirma el `alertdialog` |

El sexto estado **no está en el contrato** y se añade a propósito: la línea 546 exige
44 × 44 px para «los controles», y dos de ellos sólo existen con el diálogo abierto.
Se declara en el `.mjs` como ampliación del estado «con suscripción», no como
sustitución de ninguna fila.

Además, la propia prueba de la matriz se defiende de que la preparación de estados
deje de funcionar: afirma que «con suscripción» tiene más controles que «vacío» y que
el diálogo aporta exactamente dos más. Si mañana la siembra fallara en silencio, la
matriz volvería a medir seis veces el formulario vacío… y esa aserción caería.

**La bitácora vieja se equivocaba y así queda dicho.**
`progress/tdd_external_calendar.md:440-442` daba el estado «con lista larga» por
estructuralmente inalcanzable «porque no hay ningún feed ICS alcanzable desde el
contenedor con la guardia SSRF activada». La premisa es cierta; la conclusión, no.
`e2e/support/projects.mjs` ejecuta SQL contra el postgres de la pila y
`PostgresExternalCalendarStore` lee los eventos de esa misma tabla: sembrar la lista
no exige ningún feed ni relajar la guardia. Lo que sí sigue siendo cierto es que
**ninguna E2E llega a `lastStatus OK` por sincronización real**, y eso se declara en
el documento UX: lo medido es la pantalla, no el camino que la llena.

**Detalle de higiene con el texto Unicode.** Los resúmenes y la etiqueta se
construyen en SQL con `chr(128512)`, `chr(233)` y `chr(241)`: el `statement` viaja a
`psql` como argumento de proceso y en Windows los caracteres fuera de ASCII se pueden
estropear por el camino. Así lo que llega a la base es exactamente lo que se pretende.

### El defecto que la auditoría destapó al ejecutarse de verdad

El carril de automatizaciones avisó de que al sembrar y medir suele salir algo
escondido. Aquí salió, y no es de contraste: **la prueba de texto al 200 % nunca
amplió nada**.

Hacía `addInitScript(() => document.documentElement.style.fontSize = "32px")` y no
comprobaba el resultado. Al añadirle la comprobación, el navegador devolvió:

```
Error: el texto no llegó al 200 %
Expected: 32
Received: 16
```

Es decir: desde que nació, esa prueba medía la pantalla a **tamaño normal** con el
título «texto al 200 %». Y aunque el init script hubiera funcionado tampoco habría
bastado, porque la hoja declara casi todos sus tamaños en píxeles
(`styles.scss:658`, `:671`, `:692`…), que no dependen del `font-size` de la raíz.

Se sustituye por el `doubleText` del precedente de `ics-calendar-ux`: se duplica el
`font-size` **calculado** elemento a elemento y se afirma que cada uno llegó al doble.
Al hacerlo apareció un segundo detalle, y también en rojo: un elemento se quedaba en
14 px porque la hoja global declara `.quiet-note { font-size: 14px !important }`
(`styles.scss:1095`), y una declaración en línea sin prioridad pierde contra ella.
**No se toca esa regla**: es una hoja compartida, y el `!important` no es un fallo de
accesibilidad —el zoom real del navegador sí escala esos píxeles, y eso lo mide
`e2e/external-calendar-native-zoom.spec.mjs`—, sólo impide emular la ampliación desde
la prueba. Se resuelve con `setProperty(…, "important")` y se explica en el comentario.

### ROJO acreditado con la mutación de control del carril de automatizaciones

Se añadió a `frontend/src/styles.scss`:

```scss
.external-calendar .external-calendar-events li { overflow: hidden; max-height: 96px; }
```

Cayeron **3 de 4** pruebas —matriz, modos y texto al 200 %—, nombrando cada elemento
recortado:

```
lista larga de resúmenes Unicode a 320 px: contenido recortado (ancho o alto)
+   "LI:...emoji y acentos... [alto 167 en 95]",   (x 60)
texto al 200 % a 320 px, con suscripción: contenido recortado (ancho o alto)
+   "LI:Reunión de equipo 1 01:35-02 [alto 149 en 95]",
```

Con el oráculo anterior esa misma regla **no habría movido una sola aserción**: el
documento no desborda a lo ancho, los botones seguían midiendo 44 px, y sobre todo la
lista **no se renderizaba nunca** en las dos pruebas responsivas. CSS restaurado
(`git status` limpio) y verde recuperado.

### Ejecución

```
E2E_WEB_PORT=18092 pnpm test:e2e -- e2e/external-calendar-ux-audit.spec.mjs
5 passed (1.6m)
```

**Previsión de mutación.** Stryker de esta feature no cubre las E2E, así que **no
espero movimiento en el marcador** por este ciclo. Lo que sí cambia es la red de
regresión: 84 mediciones geométricas donde antes había 14, y con cinco oráculos por
medición en vez de uno.

---

## Ciclo 11 — hallazgo 13: los modos que la aplicación implementa y nadie ejecutaba

**Qué decía el dictamen.** `docs/ux-requirements.md:54`, que @s40 incorpora por
referencia, exige que toda variante personalizable pase sus verificaciones. La
aplicación implementa `prefers-color-scheme: dark` (`styles.scss:53`) y
`prefers-reduced-motion` (`:1293`), y otras nueve specs del repositorio ya usan
`emulateMedia`; sobre `/calendario-externo` no había ni una ocurrencia.

**VERDE.** Una prueba de **cuatro modos × seis estados = 24 pasadas**: claro, oscuro,
`forced-colors: active` y `prefers-reduced-motion: reduce`. En cada combinación se
ejecuta axe **y** la geometría completa, y se mide tinta contra lienzo para que dos
tokens no colapsen en el mismo color. Cada modo fija **las tres** preferencias, porque
`emulateMedia` conserva las que no se nombran —lección ya escrita en
`ics-calendar-ux.spec.mjs:304`—. Bajo colores forzados se omite **sólo**
`color-contrast`, porque Chromium pinta colores del sistema mientras axe lee los
declarados; el resto de reglas se conserva.

**La pasada de movimiento reducido se declara vacua, en la propia prueba.** El
verificador pedía que si se ejecutaba no se contase como evidencia. En vez de un
comentario, hay una aserción: se cuenta cuántos elementos de la pantalla tienen
`transitionDuration` o `animationDuration` por encima de 0,01 s **sin** la preferencia
puesta, y se exige que sean cero. Hoy no hay nada que reducir; el día que alguien
añada movimiento aquí, esa aserción caerá y habrá que medirlo de verdad.

**ROJO acreditado con una mutación de control que sólo existe en un estado sembrado y
sólo en oscuro:**

```scss
@media (prefers-color-scheme: dark) {
  .external-calendar .external-calendar-counters { color: #2b2b2b; }
}
```

axe la delató, con impacto `serious`:

```
Element has insufficient color contrast of 1.03
(foreground color: #2b2b2b, background color: #1f2937, font size: 9.0pt)
html: <p class="external-calendar-counters">3 eventos, 1 recurrente no incluido, …</p>
```

Es exactamente la forma del defecto que el carril de automatizaciones encontró (1,01
sobre 1 en un interruptor que jamás se había renderizado en una corrida medida): un
color que sólo se pinta en un estado que la auditoría no construía y en un modo que no
ejecutaba. Con la auditoría anterior —tema por defecto, estado vacío— habría pasado la
puerta sin tocar una aserción. CSS restaurado y verde recuperado.

**Resultado honesto: no había defecto de producto.** Las 24 combinaciones dan cero
violaciones y la geometría aguanta en todas. La pantalla no introduce paleta propia:
usa `.field-error`, `.failure`, `.save-status`, `.notice`, `button` e `input`
globales, ya auditados en otras rutas. Se buscó el equivalente del contraste de 1,01
y **no lo hay aquí**; lo que había escondido era el placebo del texto al 200 %, que
está en el ciclo 10.

### Ejecución

```
E2E_WEB_PORT=18092 pnpm test:e2e -- e2e/external-calendar-ux-audit.spec.mjs
5 passed (1.6m)
```

`progress/ux_external_calendar.md` queda actualizado: las 30 filas siguen ahí, Fitts
y Von Restorff pasan de «parcialmente verificado» a «verificado en navegador», la
sección «Lo que este documento NO afirma» se reescribe para decir lo que ahora sí se
afirma, y el trabajo pendiente se reduce a la revisión manual con lector de pantalla,
Firefox/WebKit y la excepción de `INPUT`/`SELECT`/`TEXTAREA`, que queda escrita en la
lista en vez de silenciada.

---

## Ciclo 12 — repaso del contrato: @s30 sólo cumplía media frase

Terminada la lista del dictamen, se repasó `features/external_calendar.feature`
escenario a escenario contra el mapa `@s → test`. Aparecieron tres cosas; ésta se
cierra aquí y las otras dos quedan escritas en el apartado siguiente.

**Lo que decía el contrato.** @s30 (`features/external_calendar.feature:404-409`):

> Given persona-a tiene suscripción con 5 eventos y lastSyncAt "2030-01-07T09:00:00Z"
> And una sincronización confirmó en base de datos lastSyncAt "2030-01-07T11:00:00Z"
> pero **su respuesta se perdió**
> When el backend se reinicia y consulto GET "/api/v1/me/external-calendar"
> Then recibo 200 con la misma id, lastSyncAt "2030-01-07T11:00:00Z" y **GET /events
> devuelve los eventos de esa sincronización**

**Lo que había.** `ExternalCalendarPersistenceTest.s30_aCommittedSubscriptionSurvivesANewStore`,
seis líneas: crear y volver a leer con un almacén nuevo, comprobando la id y que la
versión es cero. Ni una sincronización, ni la respuesta perdida, ni un solo evento. La
segunda mitad del Then —la instantánea sobrevive y es la de la **última**
sincronización— no la ejercía nadie.

**VERDE.** `s30_theSnapshotOfASyncWhoseResponseWasLostSurvivesTheRestart`: se confirman
**dos** sincronizaciones sobre la misma suscripción —la de las 09:00Z con cinco eventos
y la de las 11:00Z con dos, cuyo valor de retorno se **descarta a propósito**, que es
exactamente lo que le ocurre al llamante cuando la respuesta se pierde por el camino— y
sólo entonces se abre un almacén nuevo sobre la misma base, que es como esta clase
modela el reinicio. Se afirman la id, `lastSyncAt` 11:00Z, `lastAttemptAt`, el estado
OK, la versión 2, `imported` 2 y que `events(...)` devuelve exactamente `w1` y `w2`.

**ROJO acreditado, y con un mutante que no mataba nadie más.** En
`PostgresExternalCalendarStore.commitSuccess` se cambió `imported = ?` por
`imported = imported + ?`, es decir: los contadores pasan a acumular en vez de describir
la última sincronización.

```
ExternalCalendarPersistenceTest > s30_theSnapshotOfASyncWhoseResponseWasLostSurvivesTheRestart() FAILED
expected: 2
 but was: 7
20 tests completed, 1 failed
```

**Una sola** de las veinte pruebas de la clase cayó, y es la nueva. Las demás no
encadenan dos sincronizaciones sobre la misma suscripción, así que ese mutante sobrevivía
a la clase entera; los tests de aplicación usan un doble del almacén y los de HTTP un
mock, de modo que tampoco lo veían. Producción restaurada y `BUILD SUCCESSFUL`.

**Previsión de mutación (PIT).** `PostgresExternalCalendarStore` sí está en
`externalCalendarClasses`, así que **este ciclo sí debería mover el marcador**: espero
matar los mutantes que tocan la asignación de `imported` en `commitSuccess` y, en general,
los que hacen que la instantánea o los contadores de la última sincronización no
sobrevivan a una segunda pasada.

---

## Repaso del contrato, escenario a escenario — qué le falta a la 28 para el 100 %

Hecho el 10 de septiembre de 2026, después de cerrar los diecinueve hallazgos del
dictamen. Se recorrieron los **40 escenarios** de `features/external_calendar.feature`
contra el mapa `@s → test` de `progress/tdd_external_calendar.md`, comprobando que
cada clase citada **existe** y que cubre **todas las filas** del Outline, no sólo la
primera.

### Lo que se arregló en el propio repaso

1. **@s30 sólo cumplía media frase.** Cerrado en el ciclo 12, arriba.
2. **El mapa `@s → test` citaba una clase borrada.** `ConnectorCipherTest` aparecía en
   las filas de @s8 y @s9 y **no existe**: desapareció con la deduplicación que unificó
   el cifrado de las features 27 y 28. Quien leyera el mapa creía que había una prueba
   que no hay. Las que sí hay —`ExternalCalendarDisabledApiTest.s8_*` (siete casos, uno
   por ruta) y `ExternalCalendarWiringTest.s8_*`/`.s9_*`— quedan escritas en su lugar.
   Cobertura real: intacta. Lo que estaba roto era el índice.
3. **La fila de @s40 decía «sin ejecutar».** Era cierto cuando se escribió y ya no lo
   es: se sustituye por lo que hoy se ejecuta, con cifras.

### Lo que NO se ha podido cerrar, y por qué

Ordenado por lo que más pesa en una puerta:

1. **Las dos campañas de mutación no se han corrido.** No existe
   `progress/mutation_external_calendar*.md`: ninguna campaña de PIT ni de Stryker ha
   medido nunca esta feature. Es **la** puerta que falta, y no es de este carril: las
   corre el orquestador en serie (`REPARTO_NOCHE.md`, regla 4). Previsión escrita por
   ciclo, arriba; el ciclo 12 es el único que debería mover el marcador de PIT.
2. **La revisión manual con lector de pantalla.** El propio spec de la auditoría la
   declara obligatoria y `progress/ux_external_calendar.md` la lista como pendiente.
   axe automatiza un subconjunto de reglas y no certifica NVDA ni VoiceOver. No es
   automatizable: hace falta una persona.
3. **Firefox y WebKit.** Todo lo medido en navegador es Chromium.
   `external-calendar-native-zoom.spec.mjs` lo declara por escrito (el zoom nativo sólo
   se sabe hacer con `chrome.tabs.setZoom`), y `playwright.config.mjs` no define
   proyectos por motor. Es una limitación de casa, compartida con las features hermanas,
   no un descuido de esta.

### Limitaciones declaradas que NO considero deuda

Están medidas, escritas y razonadas; las nombro para que nadie las descubra como
sorpresa en el dictamen siguiente:

- **2560 px CSS no se mide con zoom nativo al 200 %.** Exigiría una ventana de 5120 px
  más el cromo. Sí se recorre en la matriz de 14 anchos, sin zoom.
- **El estado «con lista larga de resúmenes Unicode» se siembra por SQL**, no por una
  sincronización real: con la guardia SSRF activa no hay ningún feed iCalendar
  alcanzable desde el contenedor. Lo medido es la pantalla, no el camino que la llena;
  el camino lo cubren `IcsFeedTest`, `HttpCalendarFeedTest` (32 pruebas) y
  `SyncExternalCalendarTest`.
- **La pasada de `prefers-reduced-motion` es vacua** y la prueba lo afirma en vez de
  esconderlo.
- **`INPUT`, `SELECT` y `TEXTAREA` quedan fuera del oráculo de recorte** —y sólo de
  ése—, documentado en el spec y en la matriz UX.
- **@s35 y @s36 sólo se ejercen en jsdom.** Las once pruebas de
  `today-external-calendar.test.tsx` y las dos de `today-external-section.test.tsx`
  cubren **fila por fila** los cuatro *examples* de @s35 y los seis de @s36, incluidas
  la cancelación por actualización nueva y la de salir de Hoy. La sección externa de
  Hoy nunca se ha pintado en un navegador real, pero ningún Then de esos dos escenarios
  habla de geometría ni de accesibilidad, así que no es un hueco de contrato: es una
  oportunidad barata ahora que sembrar por SQL está resuelto, no un requisito.

### Estado del recuento

- 19 de 19 hallazgos del dictamen, cerrados.
- 40 de 40 escenarios con al menos una prueba que puede fallar, y con las filas de los
  Outline cubiertas una a una donde el contrato las enumera.
- `feature_list.json`: la 28 sigue **`in_progress`**, y así debe seguir hasta que el
  juez y el `mutation_tester` se pronuncien. Este carril no la marca `done`.

### Verde de esta sesión

```
gradlew test --tests "…ExternalCalendarTodayApiTest"                 3 tests, BUILD SUCCESSFUL
gradlew test --tests "…persistence.ExternalCalendarPersistenceTest"  20 tests, BUILD SUCCESSFUL
gradlew test --tests "…ExternalCalendarIsolationTest"                BUILD SUCCESSFUL
vitest run (los 5 ficheros del carril)                               85 passed (85)
E2E_WEB_PORT=18092 … e2e/external-calendar-ux-audit.spec.mjs         5 passed
```

No se ha ejecutado la suite completa de backend ni PIT ni Stryker: cinco carriles
comparten la máquina y las reglas lo prohíben.
