# Feature 25 — webhooks — bitácora de mutación y cierre de bloqueantes

Carril `claude/wh-bloqueantes`, worktree `C:/Users/vhurt/ow-worktrees/wh-bloqueantes`,
10 de septiembre de 2026. Cierra **B3, B4, B5, B8, B9 y B10** de
`progress/carriles/bloqueantes_25.md`. **B6 queda abierto a propósito** (es una
pregunta al propietario, no un arreglo). **B1, B2 y B7 no son de este carril.**

Este fichero es el que el dictamen exige por nombre y que no existía (B4). Lo que
no está cerrado está declarado abierto **con su razón y con lo que falta**. Nada
maquillado.

---

## 0. Lo primero, porque cambia la lectura de todo lo demás

**No he lanzado ninguna campaña de PIT ni de Stryker.** Instrucción expresa del
coordinador: hoy se han caído tres por carga y memoria, y se relanzan al final
con la máquina drenada. Por tanto:

- La cifra del backend que se publique al cerrar **no la produce este fichero**.
- Para saber si un mutante muere he usado el método que el coordinador indicó:
  **aplicarlo a mano al fuente, correr la clase concreta, y deshacerlo**. Cada
  veredicto de la sección 2 lleva escrito el mutante exacto que apliqué y las
  pruebas que se pusieron rojas. Es reproducible por cualquiera en un minuto.
- Los recuentos de la sección 2 salen del `mutations.xml` **que B2 impugna**
  (`backend/build/reports/pitest-webhooks/mutations.xml`, 10:53). Los uso como
  **inventario nominal**, que es para lo que sirven aunque su numeración de línea
  no case; **no** como puntuación. Cuando la campaña se relance, los recuentos
  cambiarán y varias de estas filas desaparecerán solas.

**Comandos usados** (siempre por clase, nunca la suite entera, porque cada clase
levanta un PostgreSQL con Testcontainers y hay ocho carriles más):

```
backend/gradlew test --tests "com.apptolast.organization.<Clase>"
backend/gradlew spotlessApply
```

**SHA de partida:** `09c694b5` (`docs(25): contrafirma de la enmienda de
latencyMs (B7 del panel)`).

---

## 1. Dos defectos de producción arreglados

### 1.1 La cola de entrega se detenía para todos los propietarios (B10)

`PostgresWebhookWork.claim` llamaba a `secrets.open(...)` **dentro del
`RowMapper`**, o sea antes del `UPDATE ... SET leased_until`. Cuando la fila
estaba sellada con una clave ya rotada —escenario que `project-spec.md` declara
**esperado**: «Rotación de `APP_CONNECTOR_KEY` invalida los secretos guardados»—
`AesGcmWebhookSecrets.decrypt` lanzaba, la transacción entera se deshacía y la
fila quedaba **sin arrendar**. Como el orden de reclamación es
`next_attempt_at, d.id`, esa misma fila volvía a encabezar la cola en el tic
siguiente, un segundo después, para siempre. `WebhookSchedule.guarded` se tragaba
la excepción y registraba sólo el nombre de la clase, sin pasar por
`WebhookAudit`. Ninguna entrega volvía a salir, ninguna quedaba marcada como
fallida, `GET /deliveries` no mostraba nada anómalo.

**El rojo, acreditado y con su texto:**

```
WebhookWorkPersistenceTest > b10_anUnreadableSecretNeitherStopsTheQueueNorKeepsItsTurnForever() FAILED
java.lang.IllegalStateException: Webhook secret cannot be opened
  at ...WebhookWorkPersistenceTest.lambda$workRefusing$0(WebhookWorkPersistenceTest.java:50)
  at ...PostgresWebhookWork.claim(PostgresWebhookWork.java:169)
  at org.springframework.jdbc.core.RowMapperResultSetExtractor.extractData(...)
  at ...PostgresWebhookWork.lambda$claimNext$0(PostgresWebhookWork.java:55)
  at org.springframework.transaction.support.TransactionTemplate.execute(...)
```

La traza es el defecto entero: el descifrado ocurría dentro del extractor de
filas, dentro de la transacción de reclamación.

**El verde, en tres piezas:**

1. **Adaptador** (`PostgresWebhookWork`). El `RowMapper` devuelve ahora un
   `LeasedRow` con el secreto **todavía sellado**: mapear una fila ya no puede
   fallar por material de clave. El descifrado ocurre **después** del
   `UPDATE ... SET leased_until`, en `handOver`, y un fallo se traduce en
   secreto **ausente**. La excepción se descarta a propósito: lleva un mensaje
   sobre material de clave y esta clase no puede dejar que llegue a ningún
   registro.
2. **Aplicación** (`DispatchWebhooks`). Una reclamación sin secreto legible no se
   firma ni se envía —firmar con `null` sería un NPE dentro del ciclo, es decir
   el mismo silencio en otro sitio—: se liquida como **un intento fallido más**,
   con `errorClass` `SECRET_UNREADABLE`, y el `audit.attempt` que ya existía lo
   deja en el rastro. Al sexto fallo la entrega se agota y arrastra su endpoint a
   `DELIVERY_EXHAUSTED`, que es exactamente el remedio que `project-spec.md`
   prescribe para una clave rotada: «se requiere recrear endpoints».
3. **Esquema** (`V31__webhook_secret_unreadable_error_class.sql`). **Esto casi se
   me escapa y es la parte que más importa del arreglo.** `webhook_deliveries`
   tiene un `CHECK` que enumera las clases de error, y la nueva rebotaba:

   ```
   org.springframework.dao.DataIntegrityViolationException: ... ERROR: new row for
   relation "webhook_deliveries" violates check constraint
   "webhook_deliveries_error_class_check"
   ```

   Sin la prueba `b10_aDeliveryFailedForAnUnreadableSecretCanActuallyBeWritten`,
   el arreglo de la cola se habría cambiado por una excepción en `record`: el
   mismo silencio, un método más allá.

**Ampliación de catálogo declarada, no colada.** `SECRET_UNREADABLE` amplía el
conjunto de `error_class` que `project-spec.md:2038` fija. **No** he tocado el
`Examples` de `@s25`, porque ese escenario clasifica **respuestas de un envío** y
aquí no hay envío. La pregunta al propietario está escrita entera en
`progress/decisiones_pendientes.md`, punto 6. Hasta que la ratifique, esa
ampliación es **una decisión de ingeniería tomada por este carril y declarada**,
no un cambio de contrato firmado.

**Pruebas que lo sujetan:**

| Prueba | Qué mide |
| --- | --- |
| `WebhookWorkPersistenceTest.b10_anUnreadableSecretNeitherStopsTheQueueNorKeepsItsTurnForever` | La entrega del vecino **sale** detrás de la ilegible, y la ilegible **queda arrendada** (no vuelve a encabezar cada tic). Los dos oráculos son necesarios: con cualquiera por separado el defecto se colaba. |
| `WebhookWorkPersistenceTest.b10_aDeliveryFailedForAnUnreadableSecretCanActuallyBeWritten` | Que la fila **se pueda escribir**: status, intento, clase de error, sin `httpStatus`, sin latencia y con su reintento a T+1 min. |
| `DispatchWebhooksTest.b10_aDeliveryWhoseSecretCannotBeOpenedFailsWithAnAuditedCodeAndIsNeverSent` | No se envía nada, la siguiente entrega **sí** sale, y el rastro de `WebhookAudit` nombra el fallo. |

### 1.2 `WebhookCursor.precedes`: producción cuyo único cliente era un doble (B8.f)

Borrado. Quien decide qué queda más allá del cursor es el
`(occurred_at, event_id) > (?, ?)` de `PostgresWebhookOutbox`; `precedes` era una
reimplementación gemela de esa decisión cuyo único llamador era el `FakeOutbox`
de `EnqueueWebhookDeliveriesTest`, y dejaba tres supervivientes que nada podía
matar (incluido `replaced boolean return with true`). El doble compara ahora por
su cuenta, en un método privado suyo, que es donde debe estar. `compareUnsigned`
se conserva: **sí** tiene cliente de producción (`AutomationCursor:23`).

Esto no es un ciclo rojo-verde y no finjo que lo sea: es la eliminación de
producción muerta. El rojo que la justificaría no puede existir porque no había
cliente. El oráculo del desempate real ya vivía en
`WebhookOutboxPersistenceTest.s19_ofTwoEventsOfTheSameInstantTheSmallerEventIdGoesFirstAndTheOtherIsNotLost`,
contra el SQL del adaptador.

---

## 2. Supervivientes, uno a uno, con veredicto escrito

Inventario del `mutations.xml` de las 10:53: **425 mutantes, 43 no muertos** (38
`SURVIVED` + 5 `NO_COVERAGE`... el desglose real es 30 `SURVIVED` y 13
`NO_COVERAGE`, contados abajo fila a fila). «Muerto ahora» significa que apliqué
el mutante a mano al fuente actual, corrí la clase y la vi roja.

### 2.1 Muertos por este carril — verificado a mano (12)

| Superviviente | Mutante aplicado | Pruebas que se pusieron rojas |
| --- | --- | --- |
| `WebhookController.find:107` `NullReturnVals` **[NO_COVERAGE]** | `find` devuelve `null` | 1 (`s8_s11_readingOnesOwnWebhookServesTheNineFieldEndpointWithoutTheSecret`) |
| `WebhookController$Failure.message:270` `EmptyObjectReturnVals` | `message()` devuelve `""` | **8** (las 7 filas de `s11_s34_s35_...SpanishMessage` + `s11_readingAWebhookOfAnotherAccount...`) |
| `WebhookDeliveryView.attempt:9` `PrimitiveReturns` (`int` → 0) | accesor devuelve 0 | `s29_everyOneOfTheElevenDeliveryFieldsIsServedByNameWithItsOwnValue` |
| `WebhookDeliveryView.createdAt:9` `EmptyObjectReturnVals` | accesor devuelve `""` | 2 (`s29_every...` y `s29_aDeliveryWithoutOutcomeYet...`) |
| `WebhookDeliveryView.updatedAt:9` `EmptyObjectReturnVals` | ídem | ídem |
| `WebhookDeliveryView.nextAttemptAt:9` `EmptyObjectReturnVals` | ídem | ídem |
| `PostgresWebhookOutbox.deliverable:144` `BooleanTrueReturnVals` | `return true;` | `s21_aPayloadThatParsesButBreaksTheContractIsRejectedByTheCheckAndNotByTheCatch` |
| `WebhookCursor.precedes:15` `ConditionalsBoundary` | — | **el método ya no existe** (§1.2) |
| `WebhookCursor.precedes:16` `ConditionalsBoundary` | — | ídem |
| `WebhookCursor.precedes:16` `BooleanTrueReturnVals` | — | ídem |

Y dos que el informe no podía ni listar, porque la clase entera recibe **cero**
mutantes (B3): la eliminación de la llamada `LOG.info` de `discarded` y el
intercambio de argumentos de `attempt`. Los dos mueren ahora; ver §3.

### 2.2 Vivos y **declarados vivos**, con su razón (31)

**Los que este carril tocó y decidió no perseguir:**

| Superviviente | Veredicto |
| --- | --- |
| `PostgresWebhookWork.number:183` `NegateConditionals` | **Sigue vivo, comprobado a mano** (apliqué el mutante, la clase quedó verde). El `null` de `http_status`/`latency_ms` en una **reclamación** no lo mira ningún oráculo: los tests toman esos campos del resultado del envío, no de la fila reclamada. Para matarlo hace falta una prueba que reclame una entrega **ya fallada** y afirme que el `httpStatus` del intento anterior llega intacto. Trabajo pequeño, ~20 min. |
| `PostgresWebhookWork.number:183` `EmptyObjectReturnVals` | Ídem, mismo oráculo lo mataría. |
| `PostgresWebhookWork.instant:178` `NullReturnVals` | Ídem: sigue vivo, comprobado a mano. `created_at`/`updated_at` de la fila reclamada no se afirman en ninguna parte. |
| `AesGcmWebhookSecrets.decrypt:110` `ConditionalsBoundary` | Vivo. Es el `<=` del tamaño mínimo del criptograma; para matarlo hace falta un criptograma de **exactamente** `1 + 12` bytes. ~15 min, en `AesGcmWebhookSecretsTest`. Fuera del encargo de este carril. |
| `WebhookAttempt.classify:35` `ConditionalsBoundary` | Vivo. Frontera 299/300 entre 2xx y redirección. `@s26` ya prueba 299 en `JdkWebhookSenderTest`, pero no la frontera en el dominio. ~10 min. |

**Los que el sintetizador declaró MENORES y no son de este carril** (los repito
para que nadie los dé por decididos): `WebhookConfiguration.webhookSchedule:18`
`NullReturnVals` (1/1); `AnchoredConnection.authority:57`, `.isAddressLiteral:76`
y `.literal:48`, los tres `ConditionalsBoundary`, en la clase que implementa la
decisión B3 del propietario; `PostgresWebhookStore.enqueuePing:158`
`NullReturnVals`; `WebhookIntent.validUrl:53` ×2 `ConditionalsBoundary`;
`WebhookPingPayload.escape:49` `ConditionalsBoundary`;
`WebhookController.allowedOn:235` y `:236` `NegateConditionals` y `:236`
`EmptyObjectReturnVals`.

**Los `NO_COVERAGE` que son sombra de B2, no agujeros reales:**
`PostgresWebhookStore.hasPendingPing:142` ×2 y los 12 de `JdkWebhookSender`
(`classify:181-186`, `send:172-175`, `elapsedMillis:237`). El propio B2 demuestra
que ese XML describe un árbol que no está en `main`:
`WebhookPersistenceTest:313-331` invoca `hasPendingPing` ocho veces y
`JdkWebhookSenderTest:525-533` mide `elapsedMillis` con 1250 ms exactos. **No los
he perseguido a propósito**: perseguir supervivientes de un informe irreproducible
es exactamente el error que B2 denuncia. Se vuelven a leer cuando la campaña se
relance sobre HEAD limpio.

---

## 3. B3 — por qué `Slf4jWebhookAudit` recibe cero mutantes. **La causa, no una explicación**

**CERRADO**, con causa medida.

`progress/mutacion_cinco_features.md:26-29` respondía con una generalidad
(«ningún adaptador de bitácora recibe mutantes en ninguna campaña: es una
propiedad de los mutadores»). Es falso como explicación y no era comprobable. La
causa real es concreta y configurable:

**PIT filtra, por defecto, toda mutación situada en una línea que llama a un
marco de registro.** Es la característica `FLOGCALL` (el interceptor de llamadas
de registro), activa por defecto, cuya lista `avoidCallsTo` incluye de fábrica
`org.slf4j.Logger`. `backend/build.gradle.kts:757` sólo desactiva `-FRECORD`; no
toca `FLOGCALL` ni `avoidCallsTo`. Los tres métodos de `Slf4jWebhookAudit` no
contienen **nada más** que una llamada `LOG.info`/`LOG.warn`, así que el único
mutante candidato —`VoidMethodCallMutator` sobre esa llamada— se filtra entero,
la clase produce cero mutantes y **por eso ni ella ni el directorio
`adapter.logging` aparecen en el informe**: PIT no lista clases sin mutantes.

**La prueba empírica está dentro del propio informe impugnado, en una clase que
mezcla las dos cosas.** `WebhookSchedule` tiene tres mutantes, todos
`VoidMethodCallMutator`:

```
WebhookSchedule.tick    línea 29   VoidMethodCallMutator
WebhookSchedule.tick    línea 30   VoidMethodCallMutator
WebhookSchedule.guarded línea 35   VoidMethodCallMutator   <- cycle.run()
```

La línea **38** de esa misma clase es `LOG.warn(...)` y **no tiene ni un
mutante**, siendo el mismo mutador sobre la misma forma sintáctica —una llamada a
método `void`— en el mismo fichero. No es una propiedad de los mutadores: es un
filtro, y se puede apagar.

**Lo que propongo para `backend/build.gradle.kts`, que NO he tocado porque es
tuyo.** Una línea, y decide el coordinador si la quiere:

```kotlin
features.set(setOf("-FRECORD", "-FLOGCALL"))
```

Aviso honesto de su precio: `-FLOGCALL` afecta a **todas** las clases del ámbito,
no sólo a esta. Cualquier clase con un `LOG.*` de traza recibirá mutantes nuevos
sobre esas llamadas, y buena parte serán supervivientes legítimos (nadie afirma
sobre una traza de depuración). Puede bajar la puntuación global de varios
ámbitos. Por eso propongo la alternativa de abajo como la buena.

**Lo que sí he hecho: sustituir la puerta por un oráculo equivalente y dejarlo
escrito.** `Slf4jWebhookAuditTest` afirma ahora el **formato exacto** de las tres
líneas —claves, orden, valores—, el logger, el nivel, que cada llamada escribe
**exactamente una** línea y que no lleva `Throwable` adjunto. Cuatro mutantes
aplicados a mano, los cuatro muertos:

| Mutante | Pruebas rojas |
| --- | --- |
| M1: eliminar la llamada `LOG.info` de `discarded` (justo el que `FLOGCALL` filtra) | `s21_s35_b3_aDiscardedRowLogsExactlyItsFourFieldsInOrder`, `s35_evenAPoisonedCodeCannotPutAUrlOrASecretInTheTrail` |
| M2: intercambiar los argumentos `status` y `errorClass` de `attempt` | `s35_b3_anAttemptLogsExactlyItsFourFieldsInOrderAndNothingElse`, `s35_b3_aSuccessfulAttemptCarriesTheNullErrorClassAndNoOtherField` |
| M3: renombrar la clave `errorClass=` a `error=` | las dos mismas |
| M4: bajar `workerError` de `warn` a `info` | `s9_s35_b3_aWorkerErrorLogsItsCodeAloneAtWarnLevel` |

**Lo que de B3 queda abierto:** la verificación clase a clase de las 20 clases
obligatorias del §7 del dictamen **sobre el informe nuevo**, con el recuento de
mutantes de cada una. No se puede hacer sin relanzar la campaña, que es B2 y no
es de este carril. El recuento por clase **del informe viejo** está en la §2 de
este fichero y en la §6 de abajo, para que el cotejo sea inmediato cuando el
nuevo exista.

---

## 4. B5 — los oráculos que no podían fallar. **CERRADO**

### 4.1 El `assertFalse` vacío

`Slf4jWebhookAuditTest:80-93` invocaba al sujeto con dos UUID y códigos cortos y
después afirmaba `assertFalse` sobre `?token=abc`, `whsec_`, `v1=`, `SECRET`,
`SIGNATURE` y `example.com`: **valores que la prueba jamás le entregó**. No podía
fallar por ningún cambio del sujeto. Sustituido por la igualdad exacta descrita
en §3, que sí falla. La aserción de ausencia se conserva pero ahora se acompaña
de «tres llamadas, tres líneas y ninguna más», que es lo que la hace mordible.

### 4.2 Nadie enganchaba un appender al logger **ROOT** durante un ciclo del worker

Ahí es donde el Given de `@s35` sitúa el riesgo: el planificador, JDBC, el
cliente HTTP. Nueva prueba
`WebhookScheduleTest.s35_neitherASuccessfulNorAFailedWorkerTickPutsAUrlSecretSignatureOrBodyInTheRootLog`:

- un tic **con éxito** que hace pasar de verdad por el sujeto la URL envenenada
  `https://example.com/hooks?token=abc`, un secreto de pega y un cuerpo con datos
  —lo afirma: `assertEquals(List.of(POISONED_URL, FAKE_SECRET, BODY), seen)`—;
- un tic **con fallo** cuyas dos mitades revientan con una excepción cuyo mensaje
  lleva dentro la URL, la firma, el secreto y el cuerpo, que es justo lo que
  traería una excepción de JDBC o del cliente HTTP;
- y mira **todo** lo registrado en ROOT: el texto, el mensaje del `Throwable`
  adjunto y su traza, recursivamente por causas.

Dos mutantes a mano, los dos muertos:

| Mutante | Resultado |
| --- | --- |
| `guarded` registra `failure.getMessage()` en vez de `failure.getClass().getSimpleName()` | rojo |
| `guarded` adjunta el `Throwable` al `LOG.warn` | rojo |

### 4.3 «Los problem+json contienen código y **mensaje en español**»

No tenía oráculo en ningún sitio: el único test de cuerpos problem+json afirmaba
status, tipo de contenido, `Cache-Control` y `$.code`.

**Nota sobre el nombre del campo.** El bloqueante pide aserciones sobre
`$.message`. El campo del mensaje se llama **`title`**: es el nombre que le da
RFC 7807, que es el formato que el contrato nombra por su tipo de contenido
(`application/problem+json`). No hay enmienda que hacer; la cláusula se comprueba
sobre `title`.

`WebhookApiTest.s11_s34_s35_everyOperationCodeMapsToItsStatusCodeAndSpanishMessage`
afirma ahora, para los **siete** códigos (400, 404, 409 y 503), la **igualdad
exacta** del texto en español, el `type`, el `status`, el `code` y que el cuerpo
tiene **exactamente cuatro claves**; y pasa el cuerpo entero por un colador que
prohíbe `https://`, `whsec_`, `?token=`, `v1=`, `example.com`, `com.apptolast` y
`Exception`.

Y `s35_anUnexpectedFailureAnswersWithoutTheUrlTheSecretOrAnyTrace` cubre la mitad
que la igualdad de textos constantes no puede cubrir: el caso de uso revienta con
un mensaje **envenenado a propósito** con la URL completa, su cadena de consulta
y algo con forma de secreto, y se afirma que nada de eso sale por la respuesta.

Dos mutantes a mano, los dos muertos:

| Mutante | Resultado |
| --- | --- |
| el manejador genérico adjunta `body.put("detail", error.getMessage())` | rojo — es **el mutante exacto que B5 describe** |
| el mensaje del 404 se traduce al inglés | rojo (fila `NOT_FOUND` de la parametrizada) |

---

## 5. B8 — las siete cláusulas sin oráculo. **CERRADAS LAS SIETE**

Cada una con el mutante que la acredita, aplicado a mano.

| # | Cláusula | Prueba nueva | Mutante que la acredita | Resultado |
| --- | --- | --- | --- | --- |
| a | `@s27:352` «si la transacción de desactivación falla, D1 no queda exhausted ni el webhook disabled» | `WebhookWorkPersistenceTest.s27_whenTheDisablingWriteFailsNeitherTheDeliveryNorTheEndpointChanges` | `record` deja de ser una sola transacción | rojo |
| b | `@s20:265,:267` orden real y ping intercalado | `WebhookWorkPersistenceTest.s20_theThreeEventsReachTheReceiverInOrderAndAnInterleavedPingDoesNotDisturbThem` | la outbox se recorre al revés (`ORDER BY occurred_at DESC`) | rojo |
| c | `@s13:191` «la outbox no pierde ni cambia ninguna fila» | `WebhookPersistenceTest.s13_deletingAWebhookLeavesEveryOutboxRowExactlyAsItWas` | el borrado toca `outbox_events` | rojo |
| d | `@s11` fila 1 y `@s8:122`, `GET /{uuid}` nunca ejercido | `WebhookApiTest.s8_s11_readingOnesOwnWebhookServesTheNineFieldEndpointWithoutTheSecret` y `s11_readingAWebhookOfAnotherAccountOrOfNoAccountIsTheSameNotFound` | `find` ignora al propietario | rojo (2 pruebas) |
| e | `@s21` filas 2 y 3, `deliverable:144` | `WebhookOutboxPersistenceTest.s21_aPayloadThatParsesButBreaksTheContractIsRejectedByTheCheckAndNotByTheCatch` | `return true;` en `deliverable` | rojo |
| f | `@s19`, `precedes` sin cliente de producción | §1.2: **borrada**; el desempate ya tenía oráculo contra el SQL | — | — |
| g | `@s29:369`, `@s30:379`, los once campos del DTO | `WebhookApiTest.s29_everyOneOfTheElevenDeliveryFieldsIsServedByNameWithItsOwnValue` y `s29_aDeliveryWithoutOutcomeYetKeepsItsFourNullFields` | `attempt()` devuelve 0 y `nextAttemptAt()` `null` | rojo |

Detalles que importan de un par de ellas:

- **(a)** el fallo de escritura se provoca con una razón de desactivación fuera
  del catálogo del esquema: lo rechaza **el mismo `CHECK` que protegería la
  columna en producción**, no un doble.
- **(b)** corren los adaptadores reales contra la base, ciclo a ciclo: caminar la
  outbox, encolar de una en una y reclamar por `next_attempt_at, id`. La versión
  anterior medía un `FakeSender` con dos entregas **ya reclamadas** y un solo
  ciclo. No se afirma la posición absoluta del ping —comparte instante de
  vencimiento con E2 y el desempate es por identificador aleatorio—, sino lo que
  dice el contrato: que llega, y que E1, E2 y E3 conservan su orden relativo.
- **(c)** se compara la **fila entera** antes y después, no su presencia: el daño
  que importa —cambiar `status`, `attempts` o `published_at`— no cambia el
  recuento.
- **(e)** el payload nuevo **parsea entero** y sólo falla la comprobación de
  contrato, que es lo que ejercita la línea 144 en vez del `catch`; y su hermano
  válido va en la misma prueba, porque sin él un `return false` constante también
  pasaría.

---

## 6. B9 — el plazo del `@Scheduled`. **MEDIDO; la cláusula del contrato, ABIERTA**

**Lo cerrado.** Los valores del `@Scheduled` eran constantes de anotación: PIT no
genera mutante alguno sobre ellas y `grep -rn fixedDelay backend/src/test` no
devolvía nada, así que se podían cambiar a cualquier cosa y la suite seguía
verde. Ahora
`WebhookScheduleTest.s32_b9_theTickRunsEverySecondAfterOneSecondOfInitialDelay`
lee la anotación y afirma `initialDelay = 1000`, `fixedDelay = 1000` **y la
unidad** (`TimeUnit.MILLISECONDS`, porque `SECONDS` con los mismos números daría
un worker mil veces más lento sin tocar una cifra). Mutante a mano: `fixedDelay`
a 5000 → rojo.

**Lo que queda ABIERTO, y por qué no lo cierro yo.** El When de `@s32` dice
«transcurren 1500 ms desde el arranque» y su tercera fila exige 20 entregas tras
el primer ciclo y las 5 restantes tras el segundo. Con 1000 + 1000, a los 1500 ms
**sólo ha corrido un tic**: el segundo llega a los 2000. **El contrato leído al
pie de la letra no se cumple.** Reconciliarlo es cambiar el contrato, y eso pasa
por el propietario.

- **Falta:** una respuesta a la pregunta escrita en
  `progress/decisiones_pendientes.md`, punto 5 (¿enmendar el When a 2500 ms, o
  cambiar el cableado a `initialDelay = 500`?).
- **Coste una vez respondida:** ~15 minutos, sea cual sea la rama elegida.

---

## 7. B6 — el contrato contradice a la producción en `@s9`. **ABIERTO A PROPÓSITO**

**No lo he tocado, por instrucción expresa y porque es lo correcto.**
`features/webhooks.feature:129-142` promete modo degradado con clave malformada;
`AesGcmWebhookSecrets.from` **lanza** y la aplicación no arranca, que es la
política que el propietario **ya ratificó** en la enmienda B5/B6 de seguridad
(`project-spec.md:2496`). Mismo patrón, menor, en `@s8:125-126` con el dato
adicional autenticado.

La pregunta exacta al propietario, con las dos ramas y sus consecuencias, está
escrita entera en `progress/decisiones_pendientes.md`, punto 4.

- **Falta:** su respuesta.
- **Coste una vez respondida:** ~20 minutos si es enmendar el `.feature` (dos
  filas de `@s9`, una línea de `@s8` y su nota fechada dentro del fichero al modo
  de la de `@s33`). Si la respuesta fuera la contraria —degradar también con
  clave malformada— es trabajo real en tres features que comparten la política, y
  no lo estimo desde aquí.

---

## 8. Lo que este carril NO tocó

- **B1** (mutación de frontend, 432/605 = 71,40 % contra un umbral de 80): de
  otro carril. Ni una línea de `frontend/` en este árbol.
- **B2** (el `mutations.xml` irreproducible): lo relanza el coordinador con la
  máquina drenada. **No he lanzado ninguna campaña.**
- **B7** (contrafirma de `latencyMs`): ya estaba cerrado antes de empezar. La
  nota fechada está en `features/webhooks.feature:321-328` y la ratificación en
  `progress/ratificaciones.md`, entrada **R4**. Verificado leyéndolo, no
  asumido.
- **`backend/build.gradle.kts`**: es del coordinador. Mi propuesta concreta está
  en §3 y **no la he aplicado**.
- **`features/webhooks.feature`**: ni una línea. Los tres cambios de contrato que
  este trabajo hace necesarios están como preguntas en
  `progress/decisiones_pendientes.md`, puntos 4, 5 y 6.

---

## 9. Estado de la suite y de la máquina

Todas las clases tocadas, verdes, corridas **por clase** (nunca la suite entera):

```
ArchitectureTest                        BUILD SUCCESSFUL
WebhookApiTest                          BUILD SUCCESSFUL
WebhookScheduleTest                     BUILD SUCCESSFUL
WebhookConnectorStartupTest             BUILD SUCCESSFUL
Slf4jWebhookAuditTest                   BUILD SUCCESSFUL
DispatchWebhooksTest                    BUILD SUCCESSFUL
EnqueueWebhookDeliveriesTest            BUILD SUCCESSFUL
CreateWebhookTest / ManageWebhookTest   BUILD SUCCESSFUL
AesGcmWebhookSecretsTest / JdkWebhookSenderTest / WebhookSignatureTest  BUILD SUCCESSFUL
dominio: WebhookAttempt/Delivery/Endpoint/Intent/PingPayload            BUILD SUCCESSFUL
WebhookPersistenceTest                  BUILD SUCCESSFUL   (Testcontainers)
WebhookWorkPersistenceTest              BUILD SUCCESSFUL   (Testcontainers)
WebhookOutboxPersistenceTest            BUILD SUCCESSFUL   (Testcontainers)
WebhookRecoveryPersistenceTest          BUILD SUCCESSFUL   (Testcontainers)
WebhookWiringTest                       BUILD SUCCESSFUL   (Testcontainers)
ExecuteAutomationsTest                  BUILD SUCCESSFUL   (por el borrado de precedes)
```

`backend/gradlew spotlessApply` aplicado y las clases vueltas a correr después.

**No he ejecutado `bin/harness init`**, que es el punto 6 del dictamen y parte de
B4. Razón: levanta el arnés completo y el coordinador ha pedido explícitamente
liberar la máquina para la campaña de mutación. Queda **abierto**, es cosa del
coordinador («init en verde ejecutado por el coordinador», dice el propio
dictamen) y cuesta lo que cueste el arnés.

**Sin pilas de E2E levantadas por este carril.** No he arrancado ninguna:
`E2E_WEB_PORT=18107` quedó sin usar. Los contenedores de Testcontainers mueren
con la JVM de cada clase; comprobado al terminar.

---

## 10. Resumen para el coordinador

| Bloqueante | Estado | Qué falta, si falta |
| --- | --- | --- |
| **B3** | **Cerrado** con causa medida (`FLOGCALL` + `avoidCallsTo`) y oráculo sustituto de 4 mutantes | Verificar las 20 clases del §7 sobre el informe **nuevo**: depende de B2 |
| **B4** | **Cerrado**: este fichero | `bin/harness init` en verde, del coordinador |
| **B5** | **Cerrado**: 3 oráculos nuevos, 4 mutantes muertos | — |
| **B6** | **Abierto a propósito**: pregunta escrita, contrato sin tocar | Respuesta del propietario. Después, ~20 min |
| **B8** | **Cerradas las siete cláusulas**, cada una con su mutante acreditado | — |
| **B9** | **Medido** el `@Scheduled`; la cláusula de 1500 ms, **abierta** | Respuesta del propietario. Después, ~15 min |
| **B10** | **Cerrado**, y con él dos defectos de producción y un `CHECK` que habría reventado | Ratificar `SECRET_UNREADABLE` en el catálogo (punto 6 de decisiones pendientes) |

**Los tres abiertos son la misma cosa: tres preguntas al propietario.** Ninguno
necesita más investigación; están escritos para que se respondan de una sentada.
