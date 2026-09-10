# C5 (backend) — veredicto de los 30 supervivientes de la feature 28

Cierra la **condición 9** de `progress/cierre_28.md` («no existe acta de mutación de
backend, ni siquiera el fichero»). El acta con la cifra y la lista nominal es
`progress/mutacion_external_calendar_backend_medida.md`; lo que faltaba, y es lo que
está aquí, es **qué se hace con cada superviviente**: o la prueba que lo mata, o el
motivo concreto por el que no se puede matar.

Carril `cal-bloqueantes`, 10 de septiembre de 2026.

## La cifra, recomputada por mí antes de razonar sobre ella

No la copio del acta. La saco otra vez del `mutations.xml` de la campaña
(`backend/build/reports/pitest-external-calendar/mutations.xml`, 467 mutaciones):

| Estado | Mutantes |
|---|---|
| `KILLED` | 436 |
| `TIMED_OUT` | 1 |
| `SURVIVED` | 19 |
| `NO_COVERAGE` | 11 |

437 / 467 = **93,58 %**. Coincide al decimal con el acta, y `ApplicationConfiguration`
no aparece ni una vez en el XML: el ámbito corregido es el que se midió. Ninguna capa
baja del 80 % (la peor, `adapter.feed`, va al 85,1 %), así que la **condición 16** —la
reserva de los dos mutantes de `HttpCalendarFeed:159`— **no se activa**.

## Resumen del veredicto

| Veredicto | Mutantes |
|---|---|
| **Muertos** por oráculo nuevo de este carril | **8** |
| **Equivalentes o inalcanzables**, con la razón medida | 13 |
| **Código muerto medido** (el `unmap` de `PublicAddressPolicy`) | 3 |
| **Abiertos**, con el trabajo concreto escrito | 6 |

Suman 30. Los 8 muertos no se declaran de memoria: por cada uno **apliqué el mutante al
fuente de producción, vi el rojo y lo pegué**, y después restauré. Es la única
acreditación posible sin relanzar PIT, y es más fuerte que volver a lanzarlo porque
señala la prueba exacta que cae.

## Los 8 que se matan, con su rojo acreditado

| Clase | Línea | Mutador | Prueba que lo mata | Rojo observado con el mutante puesto |
|---|---|---|---|---|
| `IcsFeed.summary` | 121 | Math (`i + 1` → `i - 1`) | `IcsFeedTest.summaryEndingInBackslashKeepsTheBackslashInsteadOfReadingPastTheEnd` | `java.lang.StringIndexOutOfBoundsException at IcsFeedTest.java:287` |
| `IcsFeed$Property.of` | 148 | Frontera (`i < line.length()`) | `IcsFeedTest.aPropertyLineWithoutAColonIsMalformedAndNotAnIndexOverflow` | `Caused by: java.lang.StringIndexOutOfBoundsException at IcsFeedTest.java:312` |
| `IcsFeed$Property.of` | 150 | Negación de `!quoted` | `IcsFeedTest.aColonInsideAQuotedParameterDoesNotSeparateNameFromValue` | `IcsFeedTest > aColonInsideAQuotedParameter... FAILED` |
| `SyncSummary.<init>` | 17 | Frontera (`contador < 0`) ×4 | `SyncSummaryTest.everyCounterAtExactlyZeroIsAValidSummary` | `FAILED` con los cuatro contadores, uno a uno |
| `SaveExternalCalendar.reject` | 56 | Negación del ternario del mensaje | `SaveExternalCalendarTest.s4_aHostThatDoesNotPassTheGuardIsRejectedOnTheUrlFieldWithoutWriting` | las dos filas `FAILED` (UNRESOLVABLE y BLOCKED) |
| `ConnectorKeyRing.malformed` | 55 | Negación de `CURRENT.equals` | `ExternalCalendarWiringTest.s9_aMalformedKeyStops…` y `…s9_aMalformedPreviousKeyNamesThePreviousVariable…` | `AssertionFailedError at ExternalCalendarWiringTest.java:43` y `:60` |
| `ConnectorsGate.doFilterInternal` | 54 | `removed call to setHeader` | las cinco rutas de `ExternalCalendarDisabledApiTest` vía el ayudante `disabled(...)` | 7 pruebas `FAILED` |
| `SyncExternalCalendar.execute` | 64 | Math (`nanoTime() - started` → `+`) | `SyncExternalCalendarTest.s12_theAuditRecordsHostStatusAndDuration…` y `s14_theAuditAlsoRecordsASuccess` | las dos `FAILED` |

Son 8 filas y 8 supervivientes de la lista. Dos matices que conviene no confundir:
de los cuatro mutantes de frontera de `SyncSummary:17` —uno por contador— PIT ya
había matado tres y sólo quedaba vivo el cuarto; el oráculo nuevo fija los cuatro a
la vez, y lo acredité aplicando **cada uno** de los cuatro por separado. Y
`ConnectorKeyRing:55` es un solo mutante, pero se cierra por los dos lados del
ternario para que no vuelva a valer una subcadena.

Detalle de lo que cada uno protege, que es lo que importa:

- **`IcsFeed:121`** era un defecto de verdad, no un hueco cosmético. Un `SUMMARY` que
  termina en barra invertida hacía que el parser leyese un carácter más allá del final:
  `StringIndexOutOfBoundsException`, que **no** cae en el `catch` de `IcsFeed:53`
  (`InvalidEvent | DateTimeException | ArithmeticException`) ni en el de
  `SyncExternalCalendar:94` (`IcsFeedMalformedException`). Salía un 500, que viola
  `features/external_calendar.feature:15`.
- **`IcsFeed$Property:148`** es el mismo tipo de agujero un piso más abajo: una línea
  sin dos puntos tiene que ser un feed mal formado, no un desbordamiento de índice.
- **`IcsFeed$Property:150`** salía `NO_COVERAGE` porque **ninguna prueba traía una
  comilla**. El mutante real de PIT no es negar `c == '"'` sino la negación de dentro
  (`quoted = !quoted` → `quoted = quoted`), que es el bloque no cubierto; con él, los
  dos puntos de dentro de un parámetro entrecomillado parten la propiedad por el sitio
  equivocado. Lo comprobé aplicando primero el mutante equivocado (negar el `==`), que
  tumba media suite, y luego el bueno, que sólo tumba la prueba nueva.
- **`ConnectorKeyRing:55`**: el oráculo anterior era `contains("APP_CONNECTOR_KEY")`,
  y `"APP_CONNECTOR_KEY_PREVIOUS"` **contiene esa subcadena**. El mensaje de una caída
  de arranque podía mandar a corregir la clave sana. Ahora se afirma el paréntesis
  (`(APP_CONNECTOR_KEY)` / `(APP_CONNECTOR_KEY_PREVIOUS)`) y la propiedad, por los dos
  lados del ternario.
- **`SyncExternalCalendar:64`**: el doble de bitácora sólo guardaba `millis >= 0`, y
  una suma también es no negativa. Ahora guarda `millis >= 0 && millis < 60_000`: una
  sincronización que no toca la red no tarda un minuto, y `nanoTime() + started` da
  del orden del doble del tiempo de arranque de la máquina.

## Los 13 que no se pueden matar, y por qué

| Clase | Línea | Mutador | Razón medida |
|---|---|---|---|
| `HttpCalendarFeed.read` | 202 | Frontera `read(chunk) >= 0` | **Equivalente por el contrato de `InputStream`**: `read(byte[])` sólo devuelve 0 si el array tiene longitud 0, y `CHUNK` es una constante mayor que cero. `>= 0` y `> 0` no se distinguen desde ninguna prueba. |
| `HttpCalendarFeed.read` | 210 | Negación del `expired` del `catch` | **Equivalente a través de la costura pública.** Con el mutante, una `IOException` no vencida devuelve `FEED_UNREACHABLE` en vez de relanzarse; pero al relanzarse la recoge el `catch (IOException \| InterruptedException)` de `:158`, **que devuelve exactamente `FEED_UNREACHABLE`**. Los dos caminos son indistinguibles desde `fetch()`. |
| `HttpCalendarFeed.expired` | 218 | Frontera `nanoTime() - deadline >= 0` | Los dos lados sólo difieren en el nanosegundo exacto del plazo. No hay forma de posarse ahí desde una prueba. |
| `IcsFeed$Property.of` | 148 (2.º) | Frontera `separator < 0` del bucle | **Equivalente.** Con `separator <= 0` el bucle sigue tras encontrar los dos puntos en la posición 0 y puede quedarse con unos posteriores; pero el nombre resultante empieza entonces por `:`, y ni `""` (original) ni `":ALGO"` (mutante) son nunca una propiedad reconocida. La salida del parser es la misma. |
| `IcsFeed$Property.of` | 158 | Frontera `equals < 0` | **Equivalente.** Sólo difieren para un parámetro cuyo `=` está en la posición 0, o sea de nombre vacío. Ni `VALUE` ni `TZID` —los dos únicos que el parser lee— pueden tener nombre vacío. |
| `IcsFeed$Property.parameters` | 144 | `EmptyObjectReturnVals` | **Accesor generado que nadie invoca.** Verificado con grep: `parameters` se lee en `:172` y `:180` desde *dentro* del propio record, o sea por campo, no por accesor. |
| `PublicAddressPolicy$Cidr.network` | 67 | `NullReturnVals` | Lo mismo: accesor generado del record; `contains` usa el campo. |
| `PublicAddressPolicy$Cidr.prefixLength` | 67 | `PrimitiveReturns` | Lo mismo. |
| `PublicAddressPolicy$Cidr.contains` | 78 | `BooleanTrueReturnVals` | **Rama defensiva inalcanzable**: `allows()` ya elige `BLOCKED_IPV4` o `BLOCKED_IPV6` por la longitud de la dirección, así que nunca se comparan longitudes distintas. |
| `PublicAddressPolicy$Cidr.of` | 70 | `NullReturnVals` | **Artefacto de medición, no hueco de oráculo.** Los rangos se construyen con `List.of(Cidr.of(...))` (`PublicAddressPolicy:22-43`), y `List.of` **prohíbe nulos**: con el mutante puesto, `AddressPolicyTest` caería entero por `ExceptionInInitializerError`. Sobrevive porque el valor se consume en el inicializador estático de la clase, que corre una vez por JVM y que PIT no vuelve a ejecutar. El oráculo existe y es falsable; lo que no puede es matarlo esta herramienta. |
| `AnchoredConnection.literal` | 48 | Frontera `getPort() < 0` | Sólo se distingue con una URL de puerto **0**, que no es un destino conectable ni aparece en ninguna fila del contrato. |
| `AnchoredConnection.authority` | 57 | Frontera `getPort() < 0` | Igual que la anterior. |
| `AnchoredConnection.isAddressLiteral` | 76 | Frontera `indexOf(':') >= 0` | Sólo se distingue con un host que **empiece** por dos puntos, que no es un nombre legal. |

## Los 3 de `unmap`: código muerto ya medido, que se borra, no se prueba

`PublicAddressPolicy.unmap` (`:55` negación, `:60` y `:63` `NullReturnVals`, los dos
últimos `NO_COVERAGE`) es **código inalcanzable**, y no lo digo yo: el panel de
`progress/carriles/bloqueantes_28.md` ejecutó el JDK y comprobó que tanto
`InetAddress.getByName("::ffff:10.0.0.1")` como `getByAddress(byte[16] mapeado)`
devuelven un `Inet4Address`, de modo que la guarda `address instanceof Inet6Address`
de `:55` nunca es cierta con una dirección mapeada.

No hay oráculo que escribir: **la fila del contrato ya lo tiene** —`AddressPolicyTest`
afirma `allows() == false` y cae si se quita `10.0.0.0/8` de la lista—, así que lo que
procede es **borrar el método** y corregir la frase del javadoc y la de
`project-spec.md` que prometen una normalización que el JDK ya hace. Es trabajo de
producción sin rojo que lo pida, o sea refactor sobre barra verde de otro carril:
lo dejo escrito y **no lo toco aquí**, porque no es ninguna de las cinco condiciones
de este encargo y borrarlo de paso mezclaría dos cosas en el mismo commit.

**Aviso que hay que subir al juez:** la condición B1 pedía «matar los mutantes de
`:195`, `:196`, `:210` y `:223`». Tres murieron con el oráculo del proveedor que
enmudece. **`:210` no se puede matar**, y no por falta de prueba: es equivalente a
través de `fetch()` porque el `catch` de `:158` mapea la `IOException` al mismo
`FEED_UNREACHABLE`. La exigencia, tal como está redactada, es inalcanzable; lo que
queda de ella —que la guillotina se mida— está cumplido por los otros tres.

## Los 6 que quedan abiertos, con lo que falta

| Clase | Línea | Mutador | Qué falta, concretamente |
|---|---|---|---|
| `HttpCalendarFeed.download` | 159 | Negación de `instanceof InterruptedException` | Una prueba que interrumpa el hilo durante un `fetch()` y afirme que `Thread.currentThread().isInterrupted()` sigue puesto al volver. **No exigido**: es la reserva de la condición 16, que no se activa porque `adapter.feed` va al 85,1 %. |
| `HttpCalendarFeed.download` | 159 | `removed call to Thread::interrupt` | El mismo par; la misma prueba los mata a los dos. |
| `HttpCalendarFeed.read` | 203 | `NullReturnVals` (`NO_COVERAGE`) | La comprobación de plazo **por trozo** nunca dispara: la guillotina de `:194-197` cierra el cuerpo justo en el instante del plazo, así que el `read` siguiente lanza `IOException` y se sale por `:210` antes. Es defensa en profundidad detrás de la guillotina. Cerrarlo pide un proveedor cuyo goteo gane la carrera al cierre, que es una prueba con carrera y por eso no la escribo a ciegas. |
| `HttpCalendarFeed.read` | 207 | `NullReturnVals` (`NO_COVERAGE`) | Lo mismo, para el plazo vencido con el flujo ya terminado. |
| `ExternalCalendarController.single` | 163 | `EmptyObjectReturnVals` (`NO_COVERAGE`) | **Hueco real**: ninguna prueba llama a `GET /events` **sin** `from` o sin `to`, así que la rama que devuelve `null` (y con ella el rechazo que arma `ExternalEventsRange.of`) no se ejerce nunca. Se cierra con dos casos en `ExternalCalendarApiTest` calcados de los de parámetros desconocidos. |
| `IcsFeed$Property.of` | 153 | Frontera `separator < 0` | Observable: con el mutante, una línea que **empieza** por dos puntos hace que el feed entero se rechace como mal formado, mientras hoy esa propiedad se ignora. **No lo cierro porque el contrato no lo dice**: `features/external_calendar.feature` no tiene ninguna fila sobre una línea sin nombre de propiedad. Pedir la fila al propietario y luego escribir el oráculo; inventar la respuesta sería fijar comportamiento no acordado. |

## Qué cambió en el árbol

Sólo pruebas. **Ni una línea de producción**, que es lo que corresponde: los diez
mutantes se matan poniendo oráculo donde no lo había, no cambiando el comportamiento.

- `backend/.../domain/IcsFeedTest.java` — 3 casos nuevos.
- `backend/.../domain/SyncSummaryTest.java` — clase nueva, 1 caso y 1 parametrizado de 4 filas.
- `backend/.../application/SaveExternalCalendarTest.java` — el mensaje entra en el `@CsvSource`.
- `backend/.../application/SyncExternalCalendarTest.java` — el doble de bitácora exige una duración plausible.
- `backend/.../adapter/config/ExternalCalendarWiringTest.java` — el paréntesis de la variable, por los dos lados.
- `backend/.../adapter/ExternalCalendarDisabledApiTest.java` — `Cache-Control: no-store` en el ayudante de las cinco rutas.

`SyncSummaryTest` entra en la campaña sin tocar nada: `targetTests` de la 28 es
`com.apptolast.organization.*` (`backend/build.gradle.kts:715`), y `SyncSummary*` ya
estaba en `externalCalendarClasses` (`:468`).

## Efecto esperado en la próxima campaña

8 mutantes pasan de vivos a muertos sobre los mismos 467: **445/467 = 95,29 %**,
desde 93,58 %. Por capas: `domain` sube de 95,6 % (174/182) a **97,8 %** (178/182) con
cuatro; `application` de 89,7 % (78/87) a **92,0 %** (80/87) con dos;
`adapter.connectors` de 96,7 % (29/30) a **100 %**; `adapter.http` de 96,6 % (56/58) a
**98,3 %** (57/58). `adapter.feed` **no se mueve**: sus siete
supervivientes son los dos de `:159` (reserva no activada), los dos `NO_COVERAGE` de
`:203`/`:207` y los tres equivalentes de `:202`, `:210` y `:218`. Es una previsión
aritmética, no una medida: **la medida la da la campaña**.
