# Review de cierre — feature 28, calendario externo

**Veredicto: APPROVED. Sin condiciones.**
10 de septiembre de 2026, sobre `main` en `34b2026c`, árbol limpio.

Las 16 condiciones de `progress/cierre_28.md` están cerradas. La única que
quedaba bloqueante en mi pasada anterior —el ámbito de Stryker que puntuaba diez
features ajenas— está cerrada **por donde había que cerrarla**: no ensanchando la
excusa, sino invirtiendo la guarda que obligaba al rango ancho.

Todo lo que sigue lo he recomputado o ejecutado yo. No hay una sola cifra en este
veredicto que venga de leer un acta.

---

## La condición bloqueante: cerrada, y comprobada hasta la columna

**El ámbito.** `frontend/stryker.external-calendar.config.json` pasa de
`src/App.tsx:53:12-75:38` a **`53:12-54:34`**: empieza en `externalCalendar` y
termina en el cierre de `"Calendario externo"`. La rama de esta feature y nada
más.

**La guarda.** `scripts/project.test.mjs:2144-2153` afirma ahora
`assert.doesNotMatch(section, /: null/)` y
`assert.doesNotMatch(section, /integrationApi|importData|exportData|appearance/)`.
Antes exigía `/: null$/`, o sea que **obligaba** al rango ancho: la guarda no
vigilaba el ámbito, lo ensanchaba. El comentario de `:2147-2150` deja escrito por
qué, que es lo que impide que vuelva. **89/89 guardas verdes**, ejecutadas por mí
(`node --test scripts/project.test.mjs`).

**La cifra.** `progress/mutation_external_calendar_frontend.md` publica
**620/670 = 92,54 %**, recomputada del mismo `mutation.json` sin relanzar la
campaña. Es **exactamente** la que yo calculé por mi cuenta antes de que nadie la
publicara, incluidos los 16 mutantes retirados (líneas 56, 58, 60, 62, 64, 65×4,
66, 68, 70, 72, 73×2 y 74 de `App.tsx`, todos `Killed`) y el `App.tsx` propio en
**8/8**: `:43` la ruta ×4, `:54` la etiqueta ×1, `:82` la rama de render ×3.

**Y una comprobación que no me han pedido pero que era la trampa de este
arreglo.** Un rango que termina en `54:34` sólo sirve si el mutante del literal
`"Calendario externo"` —que acaba **justo** en la columna 34— sigue entrando; si
Stryker excluyera el borde, la etiqueta se quedaría sin mutar en silencio y el
ámbito estrecho habría comprado honestidad a cambio de un agujero. No entra por
suerte:

- `@stryker-mutator/core/.../project-reader.js:196-206` toma la línea en base 1 y
  **la columna tal cual** (base 0, la de Babel), que es la convención del `mutate`.
- La inclusión es `locationIncluded` con `gte` (`incremental-differ.js:395-402`),
  o sea **`>=`**: el mutante que acaba en la columna del final del rango entra.
- Y hay precedente medido en esta misma campaña: el rango de la ruta es
  `43:8-43:58` y el literal `"/calendario-externo"` acaba **en la columna 58
  clavada** — y su mutante existe y sale `Killed`.

`scripts/reapuntar-rangos-stryker.mjs:63-67` también se estrechó, así que la
próxima derivación nace estrecha.

---

## Las dos puertas, recomputadas por mí de los artefactos

| Qué | Fuente | Mi cálculo | Publicado |
|---|---|---|---|
| Mutación backend | `pitest-external-calendar/mutations.xml` (19:34) | 445 KILLED / 467 = **95,29 %**, **cero** `TIMED_OUT` | 95,29 % ok |
| Por capa (8) | el mismo XML | feed 40/47 **85,1**, net 19/22 86,4, application 80/87 92,0, domain 178/182 97,8, http 57/58 98,3, persistence 39/39, connectors 30/30, logging 2/2 | idéntico ok |
| Mutación frontend | `mutation-external-calendar/mutation.json` (19:45), ámbito estrecho | 620 / 670 = **92,54 %** | 92,54 % ok |
| Por fichero | el mismo JSON | external-calendar.tsx 220/250 **88,00**, today 77/81 95,06, api 310/326 95,09, App.tsx 8/8, workspace.tsx 5/5 | idéntico ok |
| Suite de backend | 205 XML de `backend/build/test-results/test/` (19:14) | **4201 pruebas, 0 fallos, 0 errores, 0 saltadas** | verde ok |
| Guardas del arnés | ejecutadas por mí ahora | **89/89** | 89/89 ok |

**Ninguna capa por debajo del 80 %, ningún fichero por debajo del 80 %.** La más
floja de todas, `adapter.feed`, va al 85,1 %: era mi condición C3/B4, la que el
global escondía cuando iba al 70,2 %.

**Procedencia.** El último commit que toca `backend/src` o `frontend/src` con
efecto sobre el bytecode o el bundle es `fe5bc7ca`, de las **18:43**. Lo único que
ha cambiado en producción desde que se midió es el **javadoc** de
`PublicAddressPolicy:53-62` —lo verifiqué con `git diff`, son comentarios—, así que
las dos medidas siguen siendo del árbol que se cierra. Los SHA que citan las actas
existen. La suite de frontend no deja artefacto, pero una campaña de Stryker que
**termina** implica que su corrida inicial fue verde: sin ella aborta.

---

## Cobertura de escenarios (@s ↔ test)

Los 40 `@s` tienen prueba en los ficheros de esta feature (24 clases de backend, 4
de frontend, 3 de E2E). Los que verifiqué uno a uno, por ser los señalados:

- **@s35** (`onlyIfStale`): `today-external-calendar.tsx:59` (`BooleanLiteral ->
  false`) sale **Killed** en la campaña. Era el oráculo que no podía fallar —el
  doble de `fetch` apilaba método y URL pero **nunca el cuerpo**—, y sin él cada
  carga de Hoy forzaba una descarga del feed ajeno con la suite en verde.
- **@s36**: `today:104` y `today:105` (el literal del aviso y el separador `{" "}`)
  salen **Killed**.
- **@s9** (`ConnectorKeyRing:55`): el oráculo usaba `contains("APP_CONNECTOR_KEY")`,
  **subcadena** de `APP_CONNECTOR_KEY_PREVIOUS`, así que no distinguía las dos
  ramas del ternario y el arranque podía mandar a corregir la clave sana. Hoy
  `ExternalCalendarWiringTest.java:43,47,63,65` afirma el paréntesis
  `(APP_CONNECTOR_KEY)` **y** que la otra variable no aparece, por los dos lados.
  Mutante muerto; `adapter.connectors` 30/30.
- **@s12**, las dos filas nuevas: el certificado que no vale para su nombre tiene
  oráculo en `HttpCalendarFeedTest.java:601`; el cuerpo que gotea sin cerrarse, en
  `:191`; el proveedor que enmudece, en `:237`.
- **@s37**: la entrada de navegación puntúa 5/5 en `workspace.tsx:82`.

**Ningún `@s` queda sin oráculo.**

---

## Disciplina TDD

- **¿Producción sin test que la pida?** `git diff b11859ef HEAD` (mi veredicto
  anterior) sobre los ficheros de esta feature: **vacío**, salvo comentarios. Los
  21 mutantes que pasaron de vivos a muertos se mataron **poniendo oráculo**, no
  cambiando comportamiento.
- **¿Rojo -> verde -> refactor?** Sí, y con la acreditación más fuerte que existe:
  las dos campañas se remidieron después y **confirmaron las previsiones al
  mutante**. Backend previó 445/467 = 95,29 % y midió 445/467 = 95,29 %, con las
  cuatro capas que debían subir subiendo lo previsto; frontend previó
  `external-calendar.tsx` 220/250 y `today` 77/81, y eso midió. Una previsión que
  acierta el denominador **y** la identidad de cada superviviente no se escribe de
  memoria.

---

## C5 — veredicto por superviviente: verificado por identidad, no por confianza

No me basta con que un acta diga «uno a uno»; comparé **la lista medida** con la
lista razonada, mutador a mutador.

**Backend, 22 sin matar.** `progress/mutacion_external_calendar_backend.md` da 13
equivalentes + 3 de código muerto + 6 abiertos = 22, y coinciden clase, línea y
mutador con el XML: `AnchoredConnection:48/57/76`,
`HttpCalendarFeed:159x2/202/203/207/210/218`, `IcsFeed$Property:144/148/153/158`,
`PublicAddressPolicy:55/60/63`, `$Cidr:67x2/70/78`,
`ExternalCalendarController:163`.

**Frontend, 50 sin matar** (más 2 `RuntimeError` fuera del denominador, como manda
Stryker). `progress/mutacion_external_calendar_frontend.md` los razona todos y el
conjunto de líneas coincide **exactamente** con el JSON.

Tres correcciones del carril a mi veredicto anterior que **acepto y registro**,
porque las comprobé en el fuente: `today:105` no era el mensaje sino el separador
`{" "}`; de `:114`/`:118`/`:122` sólo `:118` es `setEvents([])` (los otros dos son
arrays de dependencias de `useCallback`); y `HttpCalendarFeed:210` **no se puede
matar** porque el `catch` de `:158` mapea la `IOException` al mismo
`FEED_UNREACHABLE` — la condición B1 estaba mal redactada, no faltaba prueba.

`DeleteExternalCalendar` sale con cero mutantes y ahora está **escrito** por qué
(`mutation_external_calendar_backend.md:63-71`), con la distinción que importa: su
cuerpo es una llamada no-void cuyo valor se descarta y ningún mutador por defecto
toca esa forma. **No es** el caso de los adaptadores de bitácora, donde había
superficie mutable y una opción la suprimía. Y no viola C3: no está en la lista
obligatoria.

---

## C3 — el ámbito de PIT apunta a lo que debe

Las **quince** clases obligatorias de `judge_external_calendar_cierre.md:229-247`
generan mutantes: `HttpCalendarFeed` 47, `PostgresExternalCalendarStore` 39,
`ExternalCalendarController` 39 (+6), `IcsFeed` 54 (+32 en `$Property`),
`ExternalCalendarInput` 28, `SyncExternalCalendar` 23 (+4), `AnchoredConnection`
21, `AesGcmSecretCipher` 17, `ConnectorsGate` 13, `ExternalEventsRange` 13,
`ConnectorKeyRing` 12, `PublicAddressPolicy` 11 (+20 en `$Cidr`),
`SaveExternalCalendar` 11, `ExternalCalendarSnapshot` 10, `OutboundHostGuard` 6,
`ReadExternalCalendarEvents` 2. `ApplicationConfiguration` **no aparece ni una
vez**. Cero `TIMED_OUT`, así que la prueba del proveedor que enmudece no envenenó
`:195/:196/:210/:223`, que era mi duda.

---

## B5 — la declaración de imposibilidad es honesta

Verificado en el árbol, no heredado: `build.gradle.kts:37` (JVM de `test`) y
`:769` (`jvmArgs` de `pitest`) fijan
`jdk.httpclient.allowRestrictedHeaders=host` en los dos JVM;
`HttpCalendarFeed.java:145-148` es el `catch (IllegalArgumentException)` de la
cabecera `Host` y sale `NO_COVERAGE`; la lista de cabeceras restringidas del
cliente del JDK se congela en el inicializador estático de sus utilidades, la
primera vez que alguien construye una petición, y por eso no se puede tocar en
caliente **en ninguna de las dos direcciones** —el carril lo midió con dos
programas de un solo fichero, y el comentario de `build.gradle.kts:31-36` ya lo
explicaba antes de que nadie lo objetara—. Y no hay segundo disparador: la URI
está validada como `https` con host de nombre.

**Es imposible en proceso, y la declaración es honesta.** Queda la tarea forkeada
`unrestrictedHostTest` como deuda escrita y presupuestada, **no como condición**:
la mitad TLS de C1(a) está cumplida (`HttpCalendarFeedTest:578`, `:601`, `:616`) y
el otro disparador del mismo `catch` tiene oráculo en `:501`.

---

## Contrato: dos enmiendas, dos citas, y ninguna colada

El diff del `.feature` **desde la destilación aprobada** (`6be97616`) hasta hoy son
**exactamente dos enmiendas**: la cabecera `:13-14` (`baf5ab1f`) y la fila del
certificado (`78dca3a6`). Las dos llevan nota fechada **dentro del `.feature`**
(`:15-21` y `:206-212`) y entrada en `progress/ratificaciones.md` (R12, R13) con
la pregunta y la opción citadas literalmente.

- **R13 sostiene sin discusión**: el propietario eligió «Anclar, y probar el TLS»,
  opción cuyo texto dice «se escribe la prueba de TLS/SNI que hoy no existe». La
  fila añadida **es** esa prueba.
- **R12 sostiene, aunque la cita diga menos que la enmienda.** La opción hablaba
  «del plazo de lectura del cuerpo del feed» y la enmienda escribe «conexión,
  cabeceras y lectura del cuerpo». La parte de más no es contrato nuevo: la fila
  `| 200 tras 6 s sin enviar cabeceras |` (`:213`) **ya venía de la destilación
  aprobada** —lo confirma `git blame`—, o sea que el plazo sobre las cabeceras ya
  estaba pactado. Lo que la enmienda añade es justo lo que la cita nombra, con su
  mismo motivo.
- Las otras dos «decisiones» de mi lista se disolvieron: la fila del proveedor que
  enmudece no era enmienda (es de `6be97616`), y el orden de navegación lo resolvió
  la retirada de las tres features que discutían ese orden.

**La herencia de los conectores no deja hueco.** `SecretCipher`,
`AesGcmSecretCipher`, `ConnectorKeyRing` y `ConnectorsGate` pasan a ser código de
la 28 y el contrato los cubre: cabecera `:6-7` (AES-256-GCM, nonce de 12 bytes,
`owner_id` como dato adicional autenticado), `@s2`, `@s3`, `@s8` y `@s9`. En
mutación, `connectors` 30/30 y `ConnectorsGate` 13/13. Y `ConnectorsGate.PREFIX`
quedó reducido a `/api/v1/me/external-calendar`: la cirugía **estrechó** la puerta
en vez de dejarla ancha.

---

## Las cinco no bloqueantes

| # | Condición | Estado |
|---|---|---|
| 2 | El parte publicaba «remidiéndose» | **hecha en lo que importa**: `current.md:25` da 95,29 % y 92,54 % con `harness init` verde |
| 3 | Tres actas conviviendo, una con un commit inexistente | **hecha**: `mutacion_..._frontend_medida.md` abre con «⚠️ ACTA CADUCADA», remite a la vigente y tacha el `ec0a3b8` |
| 4 | Coordenadas que no recomputaban | **hecha**: `:769` en los dos ficheros, `2122-2154` para la guarda |
| 5 | `DeleteExternalCalendar` con cero mutantes | **hecha**, con la distinción correcta |
| 6 | `PublicAddressPolicy.unmap` | **hecha por la salida que dejé abierta**: se conserva el código y se corrigen las dos frases (`PublicAddressPolicy:53-62` y `project-spec.md:2492`), sin tocar el denominador ni obligar a remedir |

Sobre la 3, que se me pregunta expresamente: **me vale conservarla**. Un acta
caducada con el aviso arriba del todo y la cita tachada no es una segunda verdad;
es la historia de por qué existió un 91,32 %. Lo que no valía era tenerla sin
marcar. Borrarla habría dejado sin explicar una cifra que estuvo publicada.

---

## Checkpoints

- **C1** — [x] arnés completo. `harness init` no lo relanzo yo, pero sus tres
  piezas están verdes por artefacto: lint (`17b823f9`), **89/89** guardas
  (ejecutadas por mí) y **4201** pruebas de backend sin un fallo.
- **C2** — [x] `feature_list.json` tiene **26 de 27 en `done`** y **una sola**
  feature en `in_progress`, la 28. Mi objeción anterior decae con el cierre de
  la 25.
- **C3** — [x] capas y dependencias respetadas.
- **C4** — [x] 4201 pruebas de backend verdes; 30 ficheros y 1124 pruebas de
  frontend inventariadas por Stryker sobre este ámbito.
- **C5** — [x] árbol limpio, sin mutantes olvidados en producción.
- **C6** — [x] los 40 `@s` con oráculo, y ninguna producción sin test que la pida.
- **C7** — [x] las dos puertas sobre el umbral, recomputadas por mí, con acta
  nominal y veredicto por superviviente en las dos.

---

## Las 16 condiciones, cerradas

| # | Condición | Bloq. | Cierre |
|---|---|---|---|
| 1 | Las cuatro decisiones donde el propietario lee | SÍ | por cita (R12/R13) + dos disueltas |
| 2 | Contrafirma de la ampliación de contrato | SÍ | notas fechadas en el `.feature`, citas literales |
| 3 | `@s35` sin oráculo que pueda fallar | SÍ | `today:59` sale `Killed` |
| 4 | Ámbito de Stryker que puntuaba otras features | SÍ | **rango estrecho + guarda invertida + 620/670 recomputado** |
| 5 | Guarda del arnés verde con el ámbito nuevo | SÍ | 89/89, ejecutado por mí |
| 6 | Remedir el frontend | SÍ | 92,54 % del `mutation.json` |
| 7 | C3, PIT con el ámbito corregido, 80 % por capa | SÍ | 95,29 %, la peor capa 85,1 % |
| 8 | C5 frontend, veredicto por superviviente | SÍ | 50 de 50, verificado por identidad |
| 9 | C5 backend, acta inexistente | SÍ | 22 de 22, verificado por identidad |
| 10 | Los seis oráculos nuevos en verde en `main` | SÍ | suite completa, 0 fallos |
| 11 | El parte publicaba una puerta que se desmontaba | no | corregido |
| 12 | Acta que citaba un commit inexistente | no | caducada y tachada |
| 13 | Coordenadas que no recomputaban | no | corregidas |
| 14 | B5, la cabecera `Host` restringida | no | imposibilidad **verificada**; deuda escrita, no condición |
| 15 | Excepción de `DeleteExternalCalendar` | no | registrada |
| 16 | Reserva de `adapter.feed` | no | no aplica: 85,1 % |

---

## Dos apuntes para el coordinador, que no son condiciones

Los dejo escritos porque los vi, no porque frenen nada. **No condicionan esta
aprobación**; hágase cuando toque.

1. **El mismo defecto de guarda vive en `stryker.appearance.config.json`.**
   `scripts/reapuntar-rangos-stryker.mjs:85` sigue derivando el tramo de Apariencia
   hasta `": null"`. Esa feature está `done` y su puerta ya pasó, así que no es
   asunto de la 28 — pero es la misma trampa y ahora se sabe cómo se arregla.
2. **Prosa caducada alrededor de la tabla del parte.** `current.md:5-6` dice «25 en
   `done`. Faltan la 25 y la 28» cuando ya son 26 y sólo falta la 28; `:24` da la
   25 como «re-juzgando» y `:30-32` dice que las cifras de la 28 «se están
   rehaciendo». La fila de la 28 —lo que juzgo— está correcta. Se arregla en el
   mismo commit que marque la 28 como `done`.

**La feature 28 puede pasar a `done`.**
