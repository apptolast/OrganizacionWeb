# Bloqueantes del panel — feature 28, calendario externo

Carril `claude/cal-bloqueantes`, worktree `C:/Users/vhurt/ow-worktrees/cal-bloqueantes`.
Encargo: cerrar B1, B2, B4, B5, B6, B7, B9 y B10 de
`progress/carriles/bloqueantes_28.md`. B3 estaba caducado y no se toca; B8 es
sólo documentación, sin enmendar ni contrafirmar nada.

**Resultado: 6 cerrados (B1, B2, B6, B7, B9, B10), 1 documentado (B8) y 2 abiertos
(B4 y B5), los dos con la razón exacta y lo que falta.**

Ningún fichero de `backend/src/main` cambia en esta rama: los ocho bloqueantes
que se han trabajado eran huecos de oráculo, no defectos de comportamiento. Los
defectos de producto que sí aparecieron están en la sección final, y ninguno
contradice el `.feature`.

## Cómo se acredita el rojo aquí

Todos estos oráculos miden código que ya existe, así que nacen verdes. La regla
del repo (`progress/carriles/REGLAS.md`, §3) es la que se ha seguido: **romper la
producción a mano, ver fallar la prueba nueva por el motivo correcto, restaurar y
volver a verde**. Cada sección trae el texto literal del fallo. Cuando la rotura
es exactamente un mutante del informe de PIT se dice qué mutante y en qué línea;
cuando no lo es —porque PIT no puede expresarlo— también se dice.

No se ha lanzado ninguna campaña de PIT ni de Stryker: van todas al final, con la
máquina drenada (`progress/plan_campanas.md`).

---

## B1 — La guillotina del plazo de 5 s: CERRADO

**Lo que faltaba.** `features/external_calendar.feature:13-14` promete que «el
plazo de 5 s es del intercambio completo» y que «ninguna descarga puede retener
un hilo más de 5 s». La única prueba que rondaba el caso,
`s12_aBodyThatDripsForeverIsUnreachable`, usa un proveedor que escribe un byte
cada 50 ms: mientras algo llega, `body.read()` retorna y quien corta puede ser la
comprobación del instante límite por trozo. Con ella, desmontar el ejecutor
demonio entero dejaba la suite verde.

**Lo que se ha escrito.** `HttpCalendarFeedTest`,
`s12_aProviderThatGoesSilentAfterTheHeadersIsCutByTheDeadline`: el proveedor
sirve las cabeceras `200 text/calendar`, las vacía al socket y **no escribe ni un
byte más ni cierra**. `read()` se queda bloqueado dentro del socket, así que sólo
puede desbloquearlo el cierre programado. Lleva `@Timeout(20)` —sin él «retener
el hilo para siempre» sería una suite colgada, no un fallo— y afirma
`FEED_UNREACHABLE` con cota medida (`elapsed < 3 × plazo`).

**Rojo acreditado**, mutante a mutante sobre `HttpCalendarFeed.java`:

| mutante | rotura aplicada | fallo |
|---|---|---|
| :195 `removed call to closeQuietly` | `() -> closeQuietly(body)` → `() -> { }` | `el hilo quedó retenido 5208 ms con un plazo de 600 ms` |
| :196 `Replaced long subtraction with addition` | `deadline - System.nanoTime()` → `deadline + System.nanoTime()` | `el hilo quedó retenido 5180 ms con un plazo de 600 ms` |
| :223 `removed call to InputStream::close` | `body.close()` nunca se ejecuta | `el hilo quedó retenido 5147 ms con un plazo de 600 ms` |

Los 5,1 s de los tres fallos son el tope que impone el propio servidor de prueba,
que cuelga a los 5 s: en producción, nadie cuelga. Ese número **es** la
demostración de que quien corta es la guillotina y no el plazo de cabeceras del
cliente.

**El cuarto mutante que pedía el bloqueante, :210, es equivalente.** `catch
(IOException cut) { if (expired(deadline)) return failed(FEED_UNREACHABLE); throw
cut; }`: si se niega la condición, la excepción sube y la recoge el `catch
(IOException | InterruptedException)` de `download` (:158), que devuelve **el
mismo** `FEED_UNREACHABLE`. Las dos ramas son indistinguibles desde fuera, y el
`finally` cancela la guillotina en ambas. No hay oráculo posible: seguirá
saliendo SURVIVED en toda campaña futura y hay que leerlo así, no como un hueco.

**Sobre la fila de Examples.** El bloqueante pedía «añadir la fila
correspondiente a los Examples de @s12». No se ha añadido, a propósito: @s12 ya
tiene la fila del goteo, y el silencio total lo cubre la cláusula general de
`feature:14`, que es la que la prueba cita. Añadir una fila sería una **tercera
enmienda sin contrafirma** justo mientras B8 denuncia las dos que ya hay. Queda
propuesta al propietario en la sección B8.

---

## B2 — Los contadores de omisión, del parser al DTO: CERRADO

**Lo que faltaba.** Los `Then` de @s17, @s18, @s20 y @s21 hablan de los
contadores que publica la sincronización. Fuera de `IcsFeedTest`, **todas** las
aserciones del árbol eran ceros. Los cuatro accesores podían devolver 0 siempre y
la suite entera quedaba verde: el propietario vería «0 recurrentes, 0 cancelados,
0 inválidos» con un feed lleno de ellos.

**Lo que se ha escrito**, los tres tramos que pedía el bloqueante:

1. `SyncExternalCalendarTest.s18_theThreeSkipCountersTravelFromTheParserToTheSubscription`:
   un feed con las tres omisiones a la vez —cuatro válidos, un UID repetido, tres
   con `RRULE`, dos `CANCELLED`— y los cuatro contadores en valores **distintos
   entre sí** y distintos de cero (imported 4, recurrentes 3, cancelados 2,
   inválidos 1). Que sean distintos no es cosmético: delata también cualquier
   cruce entre contadores, que un valor repetido escondería.
2. `ExternalCalendarPersistenceTest.s18_theFourCountersSurviveTheRoundTripEachOneInItsOwnColumn`:
   la ida y vuelta a PostgreSQL, cada contador por su columna.
3. `ExternalCalendarApiTest`: el fixture `synced()` pasa a tener
   `skippedInvalid = 7`, y la prueba de los quince campos lo afirma. Era el único
   de los cuatro que seguía en cero en el DTO.

**Rojo acreditado** con `replaced int return with 0` sobre cada accesor:

| mutante | fallo |
|---|---|
| `SyncSummary.skippedRecurring` | `expected: <3> but was: <0>` |
| `SyncSummary.skippedCancelled` | `expected: <2> but was: <0>` |
| `SyncSummary.skippedInvalid` | `expected: <1> but was: <0>` |
| `ExternalCalendarSubscription.skippedInvalid` (aplicación) | `expected: <1> but was: <0>` |
| `ExternalCalendarSubscription.skippedInvalid` (DTO) | `JSON path "$.subscription.skippedInvalid" expected:<7> but was:<0>` |

---

## B4 — La capa `adapter.feed` bajo el listón: ABIERTO, pero con el trabajo hecho

**Medido por mí sobre el XML** (`backend/build/reports/pitest-external-calendar/mutations.xml`,
la campaña del 10 de septiembre a las 11:23), y coincide con el panel al decimal:

| capa | antes |
|---|---|
| adapter.config (ApplicationConfiguration) | 112/113 · 99,1 % |
| adapter.feed | **33/47 · 70,2 %** |
| adapter.net | 19/22 · 86,4 % |
| application | 77/87 · 88,5 % |
| adapter.http | 54/58 · 93,1 % |
| adapter.connectors | 28/30 · 93,3 % |
| domain | 170/182 · 93,4 % |
| adapter.persistence | 37/39 · 94,9 % |
| global | 531/579 · 91,71 % |

**Por qué sigue abierto.** Sólo lo cierra la campaña, y no puedo lanzarla (hoy
han caído tres por carga; van al final con la máquina drenada). Lo que sí se ha
hecho es todo lo demás: escribir los oráculos que faltaban y **verificar a mano,
mutante a mutante, que matan**.

`adapter.feed` tenía 14 supervivientes. Los oráculos nuevos matan **siete**:

- :195, :196, :223 — la guillotina (B1, arriba).
- :119, :125, :128, :148 — las cuatro degradaciones que ocurren **antes** de
  abrir el socket y que salían `NO_COVERAGE`: URL que parsea pero no tiene host,
  resolutor que revienta, nombre sin direcciones (lista vacía o nula) y petición
  que el propio cliente del JDK se niega a construir. Las cuatro afirman
  `FEED_UNREACHABLE` y que el proveedor **no recibe ni una petición**; la primera
  afirma además que no se resuelve nada. Rojo acreditado con `replaced return
  value with null` en cada línea: `Unexpected null value, expected:
  FeedFetch.Failed but was: null` (las cuatro).

Proyección: **40/47 = 85,1 %**, por encima del 80 % del listón. Los siete
supervivientes que quedarían, uno por uno:

| línea | mutante | por qué |
|---|---|---|
| :210 | negated conditional | equivalente: las dos ramas acaban en FEED_UNREACHABLE (ver B1) |
| :202 | conditional boundary `>= 0` → `> 0` | sólo difiere si `read(byte[16384])` devuelve 0, cosa que no ocurre nunca |
| :218 | conditional boundary `>= 0` → `> 0` | sólo difiere en la igualdad exacta de nanosegundos |
| :203, :207 | null return | esos dos `return` no se toman jamás: cuando el plazo vence, la guillotina ya cerró el cuerpo y la salida es por la excepción de :209 |
| :159 ×2 | negated conditional + `removed call to Thread::interrupt` | **cerrable**: falta una prueba que interrumpa el hilo durante un `fetch` y afirme que la marca de interrupción sobrevive. No estaba en el encargo. Estimo 30-40 min |

**Qué falta, concretamente, para cerrar B4:**

1. Sacar `ApplicationConfiguration*` del ámbito (ver la lista para el propietario,
   abajo). Aporta 113 de los 579 mutantes —el 19,5 %— con 112 muertos casi todos
   por `AutomationWiringTest` y `ApplicationWiringTest`: es cabotaje de otras
   features sosteniendo la nota de ésta.
2. Relanzar la campaña con los oráculos ya escritos.
3. Exigir `adapter.feed >= 80 %` antes de firmar.

**Proyección completa tras la campaña** (suma de todos los mutantes que este
carril ha verificado a mano que mueren: 7 en feed, 4 en domain, 2 en http, 2 en
persistence, 1 en application):

| capa | proyectado |
|---|---|
| adapter.feed | 40/47 · **85,1 %** |
| adapter.net | 19/22 · 86,4 % |
| application | 78/87 · 89,7 % |
| adapter.connectors | 28/30 · 93,3 % |
| domain | 174/182 · 95,6 % |
| adapter.http | 56/58 · 96,6 % |
| adapter.persistence | 39/39 · 100 % |
| global sin ApplicationConfiguration | 435/466 · **93,3 %** |

Todas las capas por encima del 80 %. **Es una proyección, no una medida**: no
vale como evidencia de C3/C4/C5 hasta que la campaña la confirme.

---

## B5 — La segunda mitad de C1(a): ABIERTO, y no se puede escribir dentro del JVM de pruebas

Ésta es la que el encargo pedía razonar en vez de forzar. La conclusión es que
**no se puede escribir en proceso**, y la razón está medida, no supuesta.

**El hecho.** El cliente HTTP del JDK congela la lista de cabeceras restringidas
la primera vez que se inicializa su clase de utilidades. Ejecutado hoy con el JDK
de este equipo:

```
$ java HostProbe.java                     # sin la propiedad
propiedad al arrancar = null
1. sin la propiedad: IllegalArgumentException -> restricted header name: "Host"
propiedad ahora = host
2. despues de fijarla en caliente: IllegalArgumentException -> restricted header name: "Host"

$ java -Djdk.httpclient.allowRestrictedHeaders=host HostProbe2.java   # como el JVM de pruebas
propiedad al arrancar = host
1. con la propiedad, como en el JVM de pruebas: ACEPTADA
propiedad ahora = null
2. tras borrarla en caliente: ACEPTADA
```

Es decir: **ni fijarla ni borrarla en caliente cambia nada**. `backend/build.gradle.kts:37`
(`systemProperty` en `test`) y `:764` (`jvmArgs` en `pitest`) la fijan en todos
los JVM de prueba, así que dentro de ellos la rama de `HttpCalendarFeed.java:148`
es inalcanzable por construcción. Una prueba que la simulara con una costura
inyectada mediría nuestra lógica de degradación, no el rechazo del JDK: eso es
justamente el oráculo que no puede fallar por el motivo que dice medir.

**Lo que sí se ha cerrado.** La línea :148 ya no está sin cobertura ni su mutante
sin matar: `s12_aRequestTheClientRefusesToBuildIsUnreachableWithoutConnecting`
llega a ese `catch` por el otro disparador del mismo `IllegalArgumentException`
—un esquema que `HttpRequest.newBuilder` no admite— y afirma lo que el adaptador
debe hacer cuando ocurre: devolver el código, no conectar y no propagar la
excepción. Rojo acreditado (`return null` en :148): `Unexpected null value,
expected: FeedFetch.Failed but was: null`.

**Lo que queda abierto** es la mitad literal de C1(a): *sin* la propiedad, se
degrada a FEED_UNREACHABLE en vez de conectar sin `Host`. Sólo lo puede medir un
JVM forkeado sin la propiedad, y eso vive en `backend/build.gradle.kts`, que
lleva el propietario. Snippet exacto en la lista de abajo. Estimo 45 min de
trabajo una vez exista la tarea.

**Y un dato para el propietario, que el bloqueante no decía.** La primera sonda
demuestra que, sin la propiedad, el rechazo ocurre al **construir** la petición,
antes de cualquier socket. O sea que un despliegue sin la propiedad no conecta
inseguro: se queda **completamente a oscuras**, todas las sincronizaciones en
FEED_UNREACHABLE, y lo único que lo delata es el registro de auditoría. La
degradación es segura, pero es total y silenciosa desde la interfaz.
`OrganizationApplication.main:17` llama a `AnchoredConnection.allow()` como
primera línea, que es lo que evita el problema en producción.

---

## B6 — Dos clases del ámbito sin presión: CERRADO, con un matiz que hay que leer

Las dos causas resultaron ser **distintas**, y esto importa para la condición C3.

**`ReadExternalCalendar`: sí era falta de prueba.** Su único mutante
(`ReadExternalCalendar.java:16`, `replaced return value with Optional.empty`)
salía `NO_COVERAGE` porque las dos clases que rozaban el caso de uso son
`@WebMvcTest` con los cinco casos de uso simulados: afirmaban sobre lo que el
propio doble devolvía. Escrito `ReadExternalCalendarTest` contra
`InMemoryExternalCalendarStore` (presente, ausente y la de otro propietario).
Rojo acreditado forzando `return Optional.empty()`: `la suscripción guardada
tiene que leerse ==> expected <true> but was <false>` y `NoSuchElementException:
No value present`.

**`DeleteExternalCalendar`: NO era falta de prueba, y ninguna prueba lo arregla.**
La clase genera **cero** mutantes por construcción: su único cuerpo es
`store.delete(ownerId);`, una llamada que devuelve `boolean` y se descarta.
`backend/build.gradle.kts` no fija `mutators`, así que rigen los operadores por
defecto de PIT, y ninguno de ellos toca eso: `VOID_METHOD_CALLS` sólo borra
llamadas a métodos `void`, y los operadores de retorno necesitan un retorno que
mutar. La campaña seguirá diciendo cero por muchos tests que se escriban.

Se ha escrito igualmente `DeleteExternalCalendarTest` (arrastra la instantánea,
es idempotente, no toca al otro propietario), porque la presión que faltaba es
real aunque la mutación no sepa expresarla: vaciar `execute()` dejaba la suite
verde y ahora no. Rojo acreditado comentando `store.delete(ownerId)`: `no queda
fila de suscripción ==> expected <true> but was <false>`.

**Consecuencia para C3, que hay que decidir arriba:** la regla «una clase
nombrada que sale a cero anula la campaña» es correcta para `ReadExternalCalendar`
y **falsa** para `DeleteExternalCalendar`, que estará a cero para siempre. O la
regla distingue «cero por no estar cubierta» de «cero por no tener instrucción
mutable», o `DeleteExternalCalendar` la hará saltar en cada campaña futura. La
alternativa —añadir `NON_VOID_METHOD_CALLS` a los mutadores— afectaría a **todas**
las clases de todos los ámbitos y no la recomiendo desde este carril.

---

## B7 — El filtro por propietario de la lectura de eventos: CERRADO

**Lo que faltaba.** La fila 4 de @s33 («persona-b recibe solo sus 2 eventos») se
daba por cubierta sobre una fuga que ninguna prueba podía ver. En
`s33_ownersAreIsolated`, el evento de B (20:00Z) cae dentro de la ventana
consultada, la aserción era `events(A, ...).getFirst().startAt()` **sin afirmar
el tamaño** —y con fuga, `getFirst()` seguiría siendo el de A, porque 09:00Z
ordena antes que 20:00Z—, y el único `hasSize(1)` estaba **después** de
`store().delete(B)`, cuando las filas de B ya no existían.

**Lo que se ha hecho.** Las aserciones se han movido a antes del borrado y se han
hecho simétricas: con las dos filas vivas, cada propietario ve exactamente su
evento (A el de 09:00Z, B el de 20:00Z) y exactamente su suscripción. Después del
borrado quedan las que ya había, más `events(B, ...)` vacío.

**Rojo acreditado.** El `WHERE` vive en un literal de SQL que PIT no muta, así
que se neutraliza a mano con `OR TRUE`, que conserva el parámetro:

| rotura | fallo |
|---|---|
| `events()`: `WHERE (owner_id = ? OR TRUE) AND start_at < ? …` | `Expected size: 1 but was: 2`, con los eventos de A y de B juntos |
| `find()`: `… external_calendar_subscriptions WHERE (owner_id = ? OR TRUE)` | `expected: "Suya" but was: "Trabajo"` |

La segunda no la pedía el bloqueante: es el mismo punto ciego en la lectura de la
**suscripción**, que estaba igual de descubierto y ahora también cae.

---

## B9 — El rechazo de parámetros desconocidos en las cinco rutas: CERRADO

Dos quintas partes de la guarda de @s10 («aplicar seguridad HTTP común en las
CINCO rutas») no se medían: sólo GET y POST /sync la ejercían. Escritos
`s10_anUnknownQueryParameterOnThePutIsRejected` y
`…OnTheDeleteIsRejected`, calcados del de GET, afirmando 400 `problem+json` con
`VALIDATION_ERROR` / `UNKNOWN_PARAMETER` sobre el campo `x` y que el caso de uso
no llega a invocarse.

**Rojo acreditado** quitando la llamada a `rejectAnyParameter`:

| mutante | fallo |
|---|---|
| `ExternalCalendarController:82` (put) | `Status expected:<400> but was:<200>` |
| `ExternalCalendarController:92` (delete) | `Status expected:<400> but was:<204>` |

---

## B10 — El aislamiento de la lectura concurrente: CERRADO, y el bloqueante estaba invertido

**Lo que faltaba.** `PostgresExternalCalendarStore:263` (`setReadOnly`) y `:264`
(`setIsolationLevel`) sobrevivían. La prueba con la que se cerró el hallazgo 19
mide la atomicidad del **escritor**, que ya garantiza la transacción externa: con
READ COMMITTED el `containsExactly("u1","u2")` pasa igual.

**El bloqueante decía «la transacción de lectura hace varias consultas (find y
events)». Eso es falso, y al revés de lo que pasa.** `reading()` construye un
`TransactionTemplate` nuevo **por llamada**, así que `find` y `events` son dos
transacciones distintas, cada una con **una sola** consulta. La prueba tal como
la pedía el bloqueante no se puede escribir: no existe esa transacción de dos
consultas.

**Lo que se ha hecho.** Meter la segunda consulta por la misma costura que ya usa
`s25_aConcurrentReadNeverSeesAnEmptyOrMixedSnapshot`: subclasificar el
`JdbcTemplate` con el que se construye el adaptador. Como la conexión está ligada
al hilo, esa segunda consulta corre **dentro de la misma transacción** que abre
`reading()`. Dos pruebas nuevas:

- `s25_theReadTransactionSeesTheSameSnapshotInBothOfItsQueries`: entre las dos
  consultas, un escritor confirma desde otro hilo una sincronización nueva. La
  segunda consulta debe seguir viendo `u1, u2`.
- `s25_theReadTransactionRefusesToWrite`: un `DELETE` dentro de la transacción de
  lectura tiene que ser rechazado. (Va **después** de la consulta real: al
  rechazarlo, PostgreSQL aborta la transacción entera y cualquier consulta
  posterior muere con SQL state 25P02. Ese detalle costó un rojo intermedio y
  está anotado en el propio test.)

**Rojo acreditado:**

| mutante | fallo |
|---|---|
| :264 `removed call to setIsolationLevel` | `la sincronización que confirmó en medio no puede cambiar lo que esta lectura ve: Expecting actual ["u3"] to contain exactly ["u1","u2"]` |
| :263 `removed call to setReadOnly` | `escribir dentro de la transacción de lectura tiene que ser rechazado: Expecting actual not to be null` |

**Defecto de diseño que queda anotado, no arreglado** (ver la sección final).

---

## B8 — Las dos enmiendas sin contrafirma: DOCUMENTADO, sin tocar nada

No he enmendado ni contrafirmado nada. Esto es lo que hay que llevarle al
propietario, con fecha y contenido exacto.

### Enmienda 1 — `baf5ab1f`, 9 de septiembre de 2026, 20:16 (+0200)

Commit `fix(external_calendar): hallazgo 14, plazo total de 5 s tambien para el
cuerpo del feed`. Sobre `features/external_calendar.feature`:

- **Quita** de la cabecera: `Descarga con redirecciones deshabilitadas, timeout
  de 5 s, Accept text/calendar, …`
- **Pone** en su lugar dos líneas nuevas (hoy :13-14): «El plazo de 5 s es del
  **intercambio completo**: conexión, cabeceras y lectura del cuerpo. Un
  proveedor que envía las cabeceras y luego gotea el cuerpo sin cerrarlo se corta
  al vencer ese plazo con FEED_UNREACHABLE; **ninguna descarga puede retener un
  hilo más de 5 s**.»
- **Añade** una fila a los Examples de @s12: `| 200 text/calendar que envía las
  cabeceras y luego gotea el cuerpo sin cerrar | FEED_UNREACHABLE |`

Qué preguntarle: **¿ratificas que el plazo de 5 s cubre el intercambio completo
y no sólo la conexión y las cabeceras?** Es un ensanchamiento de la promesa, no
una aclaración: obliga a la guillotina y al hilo demonio que la ejecuta.

### Enmienda 2 — `78dca3a6`, 10 de septiembre de 2026, 01:57 (+0200)

Commit `test(external_calendar): @s12/B3 el TLS del calendario, y una sola copia
del anclaje`. Añade una fila a los Examples de @s12:

`| presenta un certificado que no es válido para su nombre | FEED_UNREACHABLE |`

Qué preguntarle: **¿ratificas que un certificado inválido para el nombre es
FEED_UNREACHABLE** —y no un código propio de TLS—? Y conviene decirle que **el
fichero del juez se cerró en `9f3fe5c1` a las 01:14**, cuarenta y tres minutos
**antes** de esta enmienda: el juez no llegó a verla.

### Tercera cosa, que propongo yo y que también necesita su firma

B1 ha medido un caso que el contrato cubre por la cláusula general de :14 pero
que no tiene fila propia: el proveedor que sirve las cabeceras y **enmudece del
todo** (ni gotea ni cierra). Es el único caso que obliga a la guillotina. Fila
propuesta para @s12, **no escrita**:

`| 200 text/calendar que envía las cabeceras y luego enmudece sin cerrar | FEED_UNREACHABLE |`

### Documentación normativa muda

`grep` de «intercambio completo», «gotea» y «certificado que no es válido» sobre
`project-spec.md` y `progress/current.md`: **cero coincidencias**. Las dos
enmiendas viven sólo en el `.feature`. Si se contrafirman, hay que anotarlas en
`project-spec.md` y en `progress/current.md`.

### El choque de navegación, que sigue abierto

`progress/current.md:472-491`: `project-spec.md:2504` fija doce entradas y la
aplicación sirve trece. **«Calendario externo» (feature 28) no aparece en la
enmienda del orden ratificado**, que sólo resolvió el choque entre la 25 y la 30,
y además «Calendario» y «Exportación» están intercambiados. `e2e/export-data.spec.mjs`
afirma hoy el orden **servido**, con la divergencia escrita en el propio test.
La decisión sigue siendo del propietario: o se arregla la aplicación, o se
enmienda `project-spec.md:2504` para incluir la feature 28.

---

## Lista exacta para el propietario: `backend/build.gradle.kts`

No lo he tocado, como se me pidió. Son tres cambios.

**1 (B4).** En `externalCalendarClasses`, **borrar** la última entrada:

```kotlin
"com.apptolast.organization.adapter.config.ApplicationConfiguration*"
```

Aporta 113 de los 579 mutantes (19,5 %) con 112 muertos, casi todos por
`AutomationWiringTest` y `ApplicationWiringTest`. Con ella dentro, un quinto de
la nota de la 28 lo sostienen pruebas de otras features. Alternativa aceptable si
se prefiere no perder cobertura del arranque: dejarla y **reportar sus 113
mutantes aparte**, muertos incluidos, en el acta de la campaña.

**2 (B5).** Tarea de prueba forkeada **sin** la propiedad restringida, más su
exclusión de `test` (donde fallaría, porque allí la cabecera sí se admite):

```kotlin
// La guarda C1(a): sin la cabecera Host autorizada, la descarga se degrada a
// FEED_UNREACHABLE en vez de conectar sin Host. No puede vivir en `test`: la lista de
// cabeceras restringidas se congela al inicializar la clase de utilidades del cliente
// HTTP, y fijarla o borrarla en caliente no cambia nada (medido; ver B5 de
// progress/bloqueantes_external_calendar.md).
val unrestrictedHostTest by tasks.registering(Test::class) {
    useJUnitPlatform()
    systemProperty("api.version", "1.44")
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    filter {
        includeTestsMatching(
            "com.apptolast.organization.adapter.feed.UnrestrictedHostCalendarFeedTest"
        )
    }
}
tasks.check { dependsOn(unrestrictedHostTest) }
tasks.test {
    filter {
        excludeTestsMatching(
            "com.apptolast.organization.adapter.feed.UnrestrictedHostCalendarFeedTest"
        )
    }
}
```

Con la tarea creada, la clase de prueba es de media hora: un `fetch` contra el
servidor local y afirmar `FEED_UNREACHABLE` y `received.isEmpty()`. Ojo: esa
clase **no** puede llamar a `AnchoredConnection.allow()` ni cargar
`HttpCalendarFeed` antes de tiempo… y precisamente por eso conviene que el
`fetch` sea lo primero que haga.

**3 (B6).** La lista nominal de la condición C3 no vive en Gradle sino en el
veredicto, pero el cambio es solidario: al añadir `ReadExternalCalendar` y
`DeleteExternalCalendar`, dejar escrito que la segunda **estará siempre a cero
mutantes por construcción** y que eso no anula la campaña. Razonado en B6.

---

## Defectos y hallazgos que quedan anotados (§9 de las reglas: no los arreglo)

1. **La lectura compuesta de `GET /events` no es atómica** (el hallazgo real que
   hay detrás de B10). `ReadExternalCalendarEvents` llama a `store.find()` y a
   `store.events()`, y **cada una abre su propia transacción**. El
   REPEATABLE_READ de `reading()` protege cada consulta por separado, que es
   justo lo que no hacía falta: una consulta sola ya es atómica. Entre las dos
   puede colarse una sincronización, y la respuesta mezclaría el `lastSyncAt` de
   una con los `items` de otra. **No contradice el contrato**: @s25 habla de la
   *lista* («ni vacía ni mezclada») y la lista sigue siendo completa. Arreglarlo
   es una consulta compuesta en el puerto (`find` + `events` en una transacción),
   toca `ExternalCalendarStore` y sus dos implementaciones, y el worktree
   `external-calendar` tiene trabajo de otro carril sobre esos mismos ficheros:
   por eso queda anotado y no hecho. Estimo 1 h 30 y una revisión de conflictos.
2. **El mutante :210 de `HttpCalendarFeed` es equivalente** (B1). No se puede
   matar; no debe contarse como hueco en ninguna campaña futura.
3. **`DeleteExternalCalendar` no genera mutantes** (B6). Ni los generará.
4. Los serios/menores que ya registraba el panel siguen igual y **este carril no
   los ha tocado**: `ConnectorKeyRing:55`, `ConnectorsGate:54` (el `no-store` del
   503), `AesGcmSecretCipher:60`, `IcsFeed:121` (SUMMARY terminada en barra
   invertida → 500 que viola `feature:15`), @s35 `onlyIfStale` que no compara el
   cuerpo, el 503 `CONNECTORS_DISABLED` en la rama «unreachable» de Hoy, el
   `catch` pelado de `today-external-calendar.tsx:57-62` y el residuo de @s25.

---

## Estado de la suite y de la máquina

- Clases ejecutadas en verde tras cada ciclo, siempre por clase concreta:
  `HttpCalendarFeedTest`, `SyncExternalCalendarTest`, `ExternalCalendarApiTest`,
  `ReadExternalCalendarTest`, `DeleteExternalCalendarTest`,
  `ExternalCalendarPersistenceTest`.
- **No** se ha lanzado la suite completa, ni PIT, ni Stryker, ni E2E.
- `docker ps` vacío al terminar: no queda ningún contenedor de este carril.
- `backend/src/main` **intacto** en toda la rama: `git diff main...HEAD --
  backend/src/main` no devuelve nada.
- Árbol limpio, todo commiteado en `claude/cal-bloqueantes`. Sin push.

## Lo que queda abierto, con lo que falta

| # | qué falta | tiempo |
|---|---|---|
| **B4** | Sacar `ApplicationConfiguration*` del ámbito y **relanzar la campaña**. Los oráculos ya están escritos y verificados a mano: proyección `adapter.feed` 85,1 %. Sin campaña no hay evidencia. | la campaña + 10 min de acta |
| **B5** | La tarea forkeada en `build.gradle.kts` (snippet arriba, lo lleva el propietario) y luego la clase de prueba. La mitad medible ya está cerrada. | 45 min tras la tarea |
| B4 (extra) | Los dos mutantes de `HttpCalendarFeed:159`, cerrables con una prueba de interrupción. No estaban en el encargo y no hacen falta para el 80 %. | 30-40 min |
