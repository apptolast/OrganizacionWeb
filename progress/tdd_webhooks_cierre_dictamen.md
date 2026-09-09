# Cierre del dictamen de la feature 25 (webhooks) — carril A

Worktree `C:/Users/vhurt/ow-worktrees/webhooks`, rama `claude/webhooks`, base `c5f9f93`.
`E2E_WEB_PORT=18090`.

Hallazgos que YA venían cerrados en la base y que no se tocan: 6, 7, 8, 14, 19, 20, 22, 23.

Backend siempre por clase concreta (REGLAS.md §1). Ninguna ejecución de la suite completa.

---

## Hallazgo 1 y 17 — @s25: la fila TIMEOUT no tenía oráculo (parte a)

**Cubre:** `features/webhooks.feature:327` (fila «acepta la conexión y no responde»).

**Prueba:** `JdkWebhookSenderTest.s25_aReceiverThatAcceptsTheConnectionAndNeverAnswersIsATimeout`
más el test de cableado `b1_theProductionDeadlinesAreFiveSecondsToConnectAndTenForTheWholeExchange`.

**Ciclo.**

1. ROJO 1 (no compila, Ley 2). Escribo el test contra un constructor con plazos
   inyectables que no existe. `gradlew test --tests JdkWebhookSenderTest`:
   `error: constructor JdkWebhookSender in class JdkWebhookSender cannot be applied
   to given types; required: Clock,AddressPolicy,HostResolver`.
2. VERDE. Constructor package-private con `connectDeadline`/`exchangeDeadline`; el
   público delega en las constantes de producción (5 s / 10 s). `request()` usa
   `exchangeDeadline` en vez de la constante. Accesores package-private
   `connectDeadline()` (leído del propio `HttpClient`, no de un campo) y
   `exchangeDeadline()`.
3. ROJO ACREDITADO del oráculo de comportamiento. Rompo la producción: cambio
   `.timeout(exchangeDeadline)` por `.timeout(Duration.ofSeconds(30))`. Resultado:
   `s25_aReceiverThatAcceptsTheConnectionAndNeverAnswersIsATimeout() FAILED —
   org.opentest4j.AssertionFailedError at JdkWebhookSenderTest.java:248`
   (el receptor acaba respondiendo 200 y `errorClass` es null, no "TIMEOUT").
   Restaurado.
4. ROJO ACREDITADO del oráculo de cableado. Rompo la producción: `EXCHANGE_TIMEOUT`
   de 10 s a 9 s. Resultado:
   `b1_theProductionDeadlinesAreFiveSecondsToConnectAndTenForTheWholeExchange() FAILED —
   AssertionFailedError at JdkWebhookSenderTest.java:230`. Restaurado.
5. VERDE final: `BUILD SUCCESSFUL`.

**Ficheros cambiados.**
- `backend/src/main/java/com/apptolast/organization/adapter/webhook/JdkWebhookSender.java`
- `backend/src/test/java/com/apptolast/organization/adapter/webhook/JdkWebhookSenderTest.java`
- `features/webhooks.feature:327`

**Enmienda del contrato (REGLAS.md §8).** La fila 327 decía «acepta la conexión y no
responde **en 5 s**». Los 5 s son `CONNECT_TIMEOUT`, que sobre una conexión YA
ACEPTADA no aplica: el corte real es el plazo de intercambio de la enmienda B1
(10 s, `JdkWebhookSender:31`). Ejecutar la fila tal como estaba escrita habría
fallado. La fila pasa a «acepta la conexión y no responde **dentro del plazo de
intercambio**», y el valor concreto (10 s) queda fijado por el test de cableado
`b1_theProductionDeadlines...`, que es donde debe vivir un número, no en el
Gherkin. Se alinea contrato con código sin silenciar nada.

**Coste en la suite.** El test de plazo tarda ~0,3 s: los plazos se inyectan
(`TEST_EXCHANGE_DEADLINE = 300 ms`), no se esperan los 10 s de producción.

**Queda abierto de este hallazgo:** la fila TLS (certificado no confiable). Ver abajo.

## Hallazgo 1 y 17 — @s25: la fila TLS no tenía oráculo (parte b) — CERRADO

**Cubre:** `features/webhooks.feature:329` (fila «presenta un certificado no confiable»).

**Prueba:** `JdkWebhookSenderTest.s25_aReceiverWithAnUntrustedCertificateIsATlsFailure`.

**Ciclo.**

1. Fixture nuevo: `backend/src/test/resources/webhooks/untrusted-receiver.p12`,
   PKCS12 autofirmado generado con keytool (CN=127.0.0.1, SAN ip:127.0.0.1,
   validez 36500 días, clave RSA 2048, contraseña `changeit`). No es una
   credencial: sólo lo usa el receptor de prueba y ninguna cadena de confianza
   del JDK lo avala, que es justamente la condición del contrato.
2. El helper `start` se parte en `listen(scheme, server, handler)` y aparece
   `startUntrustedTls`, que monta un `HttpsServer` con ese keystore. El record
   `Receiver` gana el esquema para poder emitir `https://…`.
3. El test PASÓ A LA PRIMERA: la rama de producción ya existía, lo que faltaba
   era el oráculo. Por eso el rojo se acredita rompiendo la producción.
4. ROJO ACREDITADO. Cambio `catch (SSLException tls) -> transport("TLS", …)` por
   `transport("MUTANT_DIRECT_CATCH", …)`. Resultado:
   `s25_aReceiverWithAnUntrustedCertificateIsATlsFailure() FAILED`. Restaurado y
   verde otra vez (`BUILD SUCCESSFUL`).

**Dato que el dictamen pedía averiguar (hallazgo 1, punto 3).** Con el catch
directo mutado la prueba se pone roja, luego **la rama viva es el `catch
(SSLException)` de `JdkWebhookSender:70-71`**, no la rama `"TLS"` de
`classify()` (:86). El fallo TLS llega desnudo, no envuelto en `IOException`.
La rama de `classify()` para TLS es, por tanto, código no alcanzado por este
camino; queda anotado como observación fuera de ámbito (REGLAS.md §9) para que
la campaña de mutación no sorprenda a nadie: `classify()` sigue siendo la red
por defecto de cualquier `IOException` que no case con los catch previos.

**Ficheros cambiados.**
- `backend/src/test/java/com/apptolast/organization/adapter/webhook/JdkWebhookSenderTest.java`
- `backend/src/test/resources/webhooks/untrusted-receiver.p12` (nuevo)

Con esto las **ocho filas** del outline @s25 quedan ejercidas contra el adaptador
real. Hallazgos 1 y 17 CERRADOS.

## Hallazgo 16 — @s33: las dos filas Bearer no tenían oráculo — CERRADO

**Cubre:** `features/webhooks.feature:413-414`.

**Prueba:** `WebhookApiTest.s33_aBearerCredentialOfTheIntegrationChannelReachesNoWebhookRoute`.

**Ciclo.**

1. Test nuevo con `@MockitoBean AuthenticateApiCredentialUseCase`: credencial
   Bearer válida con scopes de projects/tasks contra `GET /api/v1/me/webhooks` y
   `POST /api/v1/me/webhooks/{id}/ping`; exige 403, `code=API_SCOPE_DENIED`,
   `Cache-Control` con `no-store` (la segunda cláusula del Then de @s33) y
   `verifyNoInteractions(create, manage)`.
2. Pasó a la primera: la frontera aguantaba hoy por omisión (denegar por defecto).
   Por eso el rojo se acredita rompiendo la producción.
3. ROJO ACREDITADO. Añado a `ApiCredentialBearerFilter.PERMISSIONS` una entrada
   `GET /api/v1/me/webhooks -> projects:read`, que es exactamente la regresión que
   el hallazgo teme («si alguien amplía PERMISSIONS o el securityMatcher, ninguna
   prueba se pondrá roja»). Resultado:
   `s33_aBearerCredentialOfTheIntegrationChannelReachesNoWebhookRoute() FAILED`.
   Restaurado con `git checkout` y verde otra vez.

**Enmienda del contrato (REGLAS.md §8).** Las filas 413-414 esperaban
`401 UNAUTHENTICATED` y el producto responde `403 API_SCOPE_DENIED`. Se corrigen
a 403 con una nota de enmienda encima del `@s33`, calcada de la que el propietario
ya ratificó en `features/github_connector.feature:398`: una credencial Bearer
válida **sí** está autenticada, luego la respuesta correcta es «sé quién eres y
esto no es para ti». Se alinea el contrato con el producto, no al revés, porque
401 sería aquí una respuesta falsa.

**Corrección documental.** `progress/tdd_webhooks.md:9` afirmaba que esas dos filas
«se prueban con 403 API_SCOPE_DENIED». Era falso. La línea queda corregida y
fechada, apuntando a la prueba que sí lo hace.

**Ficheros cambiados.**
- `backend/src/test/java/com/apptolast/organization/adapter/WebhookApiTest.java`
- `features/webhooks.feature:409-418`
- `progress/tdd_webhooks.md:9`

## Hallazgo 5 — el rebinding DNS que el spec exigía no estaba implementado y el código decía que sí — CERRADO

**Sin ciclo rojo-verde, y con razón.** Este hallazgo no pide comportamiento nuevo:
el verificador lo calibra explícitamente como «bloqueante **por integridad de las
afirmaciones**, no por el riesgo», y añade que implementarlo tal como lo redacta
`project-spec.md:2492` **empeoraría el TLS**, porque anclar la conexión a la
dirección literal en el cliente HTTP del JDK exige
`jdk.httpclient.allowRestrictedHeaders=host` y rompe la verificación del nombre
del certificado. Su cierre mínimo aceptable son tres actos documentales y de
despliegue, y dice literalmente «cualquiera de esas tres formas cierra el
bloqueante; dejar el javadoc como está, no». Se hacen **las tres**.

1. **Javadoc de `JdkWebhookSender:25-27` reescrito.** Afirmaba «so no name can be
   re-pointed between the check and the use», que es falso. Ahora afirma sólo lo
   que el código hace —resolución única, validación de todas las direcciones,
   abandono antes de conectar— y nombra el reenlace posterior como límite
   residual, con el porqué de no anclarlo.
2. **`project-spec.md` (enmienda B2/B3) reconciliado.** Donde decía «Deja de
   aceptarse como límite el reenlace de nombres» ahora dice que vuelve a ser un
   límite aceptado y declarado, con las tres cosas que lo acotan y la fecha de la
   corrección. También `progress/tdd_webhooks.md:36-39` queda revocado en su
   segunda mitad.
3. **`deploy/EGRESS.md` (nuevo).** Es la salida de emergencia que el propio B3
   preveía (`progress/security_review_connectors.md:40`): la política de egreso
   declarada como requisito de despliegue, con la lista de rangos a bloquear en
   la red y qué pasa si no se cumple.

**Lo que sí tiene oráculo ahora.** La barrera que de verdad sostiene el residuo
—que un certificado no confiable se rechaza— pasó de no tener ninguna prueba a
tenerla: `JdkWebhookSenderTest.s25_aReceiverWithAnUntrustedCertificateIsATlsFailure`
(hallazgo 1/17, más arriba). Antes de este carril, un `SSLContext` permisivo de
depuración habría pasado la suite entera en verde. Ése era el riesgo real que el
verificador señalaba, y está cubierto por una prueba con rojo acreditado.

**Ficheros cambiados.**
- `backend/src/main/java/com/apptolast/organization/adapter/webhook/JdkWebhookSender.java` (javadoc)
- `project-spec.md` (COMPARTIDO, REGLAS.md §6 — un solo párrafo, la enmienda B2/B3)
- `progress/tdd_webhooks.md`
- `deploy/EGRESS.md` (nuevo)

## Hallazgo 15 — @s42:522: los cambios de estado no se anunciaban por aria-live — CERRADO en unitarios

**Cubre:** `features/webhooks.feature:522`, cláusula «los cambios de estado se
anuncian por aria-live», sobre las acciones de @s39 y @s40.

**Pruebas (cinco aserciones, una por acción):** en `frontend/src/webhooks.test.tsx`,
sobre los tests existentes de cada acción, con el helper nuevo `announcement()`
—`screen.getByRole("status").textContent`—:
- `@s39 pings an active webhook…` → «Ping enviado. La entrega queda pendiente.»
- `@s39 disables an active webhook…` → «Webhook desactivado.»
- `@s39 reactivates a manually disabled webhook` → «Webhook activado.»
- `@s39 asks for confirmation before deleting…` → «Webhook eliminado.»
- `@s40 opens the deliveries panel…` → «Entrega reenviada. Vuelve a estar pendiente.»

`getByRole("status")` no puede confundirse con el div de carga: ése lleva
`aria-live` pero no rol, que era justamente la trampa que el hallazgo describía
(`webhooks.test.tsx:110` afirmaba `closest("[aria-live]")` sobre un texto
estático y no discriminaba nada).

**Ciclo.**

1. ROJO ACREDITADO (el bueno: la región no existía). Añado las cuatro primeras
   aserciones y ejecuto `pnpm --dir frontend exec vitest run src/webhooks.test.tsx`:
   `Tests 4 failed | 19 passed` con
   `TestingLibraryElementError: Unable to find an accessible element with the role "status"`
   en las cuatro.
2. VERDE. En `webhooks.tsx`: estado `announcement`, región
   `<p role="status" aria-live="polite" aria-atomic="true">` justo bajo la de
   carga, y `setAnnouncement(...)` en `changeStatus` (con el texto según el
   destino), `ping`, `remove` y `redeliver`. `Tests 23 passed`.
3. ROJO ACREDITADO de la quinta. Añado la aserción de @s40 con
   `setAnnouncement` de `redeliver` retirado a mano:
   `AssertionError: expected '' to be 'Entrega reenviada. Vuelve a estar pen…'`.
   Restaurado: `Tests 23 passed`.
4. REFACTOR en verde: `prettier --write` sobre los dos ficheros.

**Ficheros cambiados.**
- `frontend/src/webhooks.tsx`
- `frontend/src/webhooks.test.tsx`
- `progress/ux_webhooks.md` (fila nueva «Anuncio de resultado», con el hueco declarado)

**Lo que queda de este hallazgo (declarado, no escondido).** El punto (c) del
cierre mínimo pedía además una comprobación en `e2e/webhooks-ux.spec.mjs` de que
la región cambia tras «Desactivar». No se entrega en este carril: el E2E exige
levantar la pila y el plazo de la sesión no da. La fila de `ux_webhooks.md` lo
dice con esas palabras. La cláusula del contrato sí queda implementada y con
oráculo en la capa de unitarios, que es donde discrimina.

**Fuera de ámbito, anotado (REGLAS.md §9).** El «aviso adicional» del hallazgo
—al pulsar «Desactivar», React desmonta el botón enfocado y el foco cae al
`body`— no se toca: no forma parte del bloqueante y arreglarlo aquí, sin prueba
de foco en E2E, sería producción sin test rojo que la pida.

## Hallazgo 21 — @s30: la última fila (entrega de OTRO webhook → 404) no tenía oráculo — CERRADO

**Cubre:** `features/webhooks.feature:372-385`, última fila del outline @s30.

**Prueba:** `WebhookPersistenceTest.s30_aTerminalDeliveryOfAnotherWebhookOfTheSameOwnerIsNeitherFoundNorRedelivered`
(contra Postgres real, que es donde vive la regla: el aislamiento por endpoint no
está en el dominio —`WebhookDelivery` no lleva `endpointId`— sino en el predicado
`endpoint_id=?` de la consulta).

**Ciclo.**

1. El test monta un único propietario con **dos** endpoints, encola una entrega en
   el segundo y la lleva a terminal (`succeeded`, attempt 1, http 200). Luego
   exige, preguntando por el **primero**: `find` vacío, `list` vacío, la entrega
   sigue listada bajo su endpoint de verdad, `ManageWebhook.redeliver` lanza
   `NOT_FOUND`, y la entrega ajena sigue `succeeded` (no se reencoló).
2. Dos rojos intermedios, útiles y anotados:
   `DataIntegrityViolationException ... violates check constraint
   "webhook_deliveries_check"` (una fila `succeeded` exige attempt y httpStatus:
   el esquema no admite terminales de mentira), y
   `expected: <NOT_FOUND> but was: <CONNECTORS_DISABLED>` (con
   `WebhookSecrets.DISABLED` el caso de uso corta antes; el test pasa a un doble
   con clave disponible, porque lo que se juzga es el aislamiento, no la clave).
3. VERDE: `BUILD SUCCESSFUL`.
4. ROJO ACREDITADO. Rompo la producción invirtiendo el predicado de
   `PostgresWebhookStore.find` (:180), de `AND endpoint_id=?` a
   `AND endpoint_id<>?` —mismo número de parámetros, predicado invertido—.
   Resultado:
   `s30_aTerminalDeliveryOfAnotherWebhookOfTheSameOwnerIsNeitherFoundNorRedelivered() FAILED`.
   Restaurado con `git checkout`.

**Renombrado exigido por el hallazgo.** `ManageWebhookTest.s30_aDeliveryOfAnotherWebhookIsNotFound`
pasa a `s30_anUnknownDeliveryIdIsNotFound`: su cuerpo pasa `UNKNOWN` como id de
ENTREGA, así que fija «id de entrega desconocido → NOT_FOUND», no la fila del
contrato. No se borra —es la única prueba que ejercita el `orElseThrow` del
lookup de entrega—, sólo deja de prometer lo que no hace.

**Ficheros cambiados.**
- `backend/src/test/java/com/apptolast/organization/adapter/persistence/WebhookPersistenceTest.java`
- `backend/src/test/java/com/apptolast/organization/application/ManageWebhookTest.java`

---

# Estado al cierre de la sesión

**Cerrados en este carril: 6** — hallazgos **1, 5, 15, 16, 17, 21**.
(Ya venían cerrados de la base y siguen intactos: 6, 7, 8, 14, 19, 20, 22, 23.)

Total del dictamen de la feature 25: 23 hallazgos. **14 cerrados, 9 abiertos.**

| # | Gravedad | Estado | Commit |
|---|---|---|---|
| 1 | BLOQUEANTE | **CERRADO** — filas TIMEOUT y TLS de @s25 contra receptor real | `c4fc097`, `0ed7735` |
| 2 | BLOQUEANTE | abierto (E2E) | — |
| 3 | BLOQUEANTE | **no me corresponde**: lo hace otro carril en `e2e/webhooks-native-zoom.spec.mjs` | — |
| 4 | BLOQUEANTE | abierto (E2E) | — |
| 5 | BLOQUEANTE | **CERRADO** — reenlace DNS devuelto a límite declarado + `deploy/EGRESS.md` | `0a68774` |
| 9 | ALTA | abierto (persistencia) | — |
| 10 | ALTA | abierto (persistencia) | — |
| 11 | ALTA | abierto, **necesita decisión del propietario** | — |
| 12 | ALTA | su parte (A) es el hallazgo 3; su parte (B) va con 4 y 13 | — |
| 13 | ALTA | abierto (E2E) | — |
| 15 | ALTA | **CERRADO** en unitarios; falta la comprobación en E2E | `2aa5258` |
| 16 | ALTA | **CERRADO** — @s33 filas Bearer | `68e533a` |
| 17 | ALTA | **CERRADO** — mismo trabajo que el 1 | `c4fc097`, `0ed7735` |
| 18 | MEDIA | abierto (persistencia) | — |
| 21 | MEDIA | **CERRADO** — @s30 aislamiento entre webhooks | `b8dcf5d` |

## Por qué queda abierto cada uno, exactamente

**2, 4, 12(B), 13 — todos E2E.** Los cuatro exigen tocar
`e2e/webhooks-ux.spec.mjs` y ejecutarlo con la pila levantada
(`E2E_WEB_PORT=18090`). No se entregan porque un cambio de oráculo en E2E que no
se ha ejecutado es peor que no hacerlo: quedaría una aserción sin acreditar. El
plazo de la sesión no daba para levantar la pila y recorrer los estados.
**Nada de esto está a medias en el árbol**: no he tocado ese fichero, así que
quien lo retome parte de main limpio.

- **2**: convertir `offenders` (calculado en :174-190 y usado sólo dentro del
  mensaje de fallo de :220) en aserción por elemento sobre un conjunto
  NOMBRADO —el `span` de la URL, el `input` del secreto, las celdas de la
  tabla—, en horizontal **y** vertical, en los cuatro anchos, excluyendo con
  lista blanca justificada el `thead` visually-hidden por `clip-path`. Nunca
  sobre `body *`: haría fallar código correcto.
- **4 y 13** (son el mismo trabajo): derivar `expectedOrder` de los enfocables
  visibles de `main`, sembrar el foco en el `h1`, deduplicar, y cerrar con
  `expect(reached).toEqual(expectedOrder)`; y asertar el foco visible medido
  como en `e2e/github-connector.spec.mjs:302-340`, **no** con
  `getComputedStyle(active, ":focus-visible")`, que es un no-op —pseudo-clase
  donde la API espera pseudo-elemento—. El umbral `toBeGreaterThan(5)` debe
  desaparecer. Precedente exacto: `github-connector.spec.mjs:261-291`.
- Corrección documental pendiente y separada: la fila «Posición en serie» de
  `progress/ux_webhooks.md` declara «el orden de Tab sigue al DOM — Verificado
  en navegador». Hoy sigue siendo falso. **No la he corregido** a propósito:
  quien cierre 4/13 debe corregirla en el mismo cambio, o quedaría descuadrada.

**9, 10, 18 — persistencia con Testcontainers.** Los tres piden componer cadenas
completas: `claim -> lease vencido -> reclaim -> send -> record` con conteo de
copias en el receptor (9); concurrencia con receptor lento y oráculo de no
espera (10); reactivación más recorrido de la outbox con dos eventos posteriores
al cursor (18). Cada iteración de esas clases cuesta ~1 min de contenedor y
ninguno se escribe de una pasada. No se empiezan a medias: medio test de
concurrencia sin rojo acreditado no vale nada.

Aviso para quien tome el **10**: la mutación que hay que matar vive en un
**literal SQL** (`FOR UPDATE OF d SKIP LOCKED` → `FOR UPDATE`,
`PostgresWebhookWork.java:68`), así que la campaña de mutación de bytecode no la
generará nunca. El oráculo de no-espera hay que escribirlo a mano.

**11 — necesita decisión del propietario, y por eso no lo toco.** El hueco 1 es
un incumplimiento de contrato real: `features/webhooks.feature:368` y
`project-spec.md:2018` exigen «items de como máximo 50 elementos» y la lectura
no acota (`PostgresWebhookStore.list`, :118-128, sin `LIMIT`). Las dos salidas
son excluyentes y ninguna es mía:

- (a) poner `LIMIT 50` en la lectura — pero entonces hay que cambiar
  `WebhookWorkPersistenceTest:198`, `assertEquals(52, log.size(), "fifty
  terminals plus the two pending ones")`, que hoy codifica la **línea 367 del
  contrato** (50 terminales + 2 pendientes). Es decir: (a) contradice otra línea
  del mismo escenario.
- (b) enmendar contrato y spec para que la 368 diga «como máximo 50 terminales
  más las pendientes vivas», tocando también `project-spec.md:2018`.

Elegir por mi cuenta sería inventar comportamiento sobre un contrato aprobado
por la puerta humana. Los huecos 2 y 3 del mismo hallazgo —orden
`updatedAt DESC, id DESC` sin oráculo, e identidad de las 50 supervivientes— sí
son cerrables sin decisión, pero van en el mismo test y el mismo commit que el
hueco 1.

## Mutación

**No ejecutada, por instrucción expresa del coordinador** («no ejecutes campañas
de mutación: tardan demasiado; las corre el orquestador después»). La puerta
existe y está cableada desde la base de este carril (hallazgos 6, 7 y 8 ya
cerrados): `node scripts/project.mjs mutate webhooks-backend` y
`webhooks-frontend`, umbral 0,80.

Dos avisos para quien la lance, salidos de este carril:

1. `JdkWebhookSender.classify()` (:83-90) es hoy una red por defecto. Para TLS
   está **demostrado** que la rama viva es el `catch (SSLException)` directo de
   :70-71, no la de `classify`. Es probable que sobrevivan mutantes ahí.
2. Los `ORDER BY` y el `SKIP LOCKED` viven en literales SQL: la campaña de
   bytecode no los muta. Lo que cubren los hallazgos 10 y 11 hay que juzgarlo
   por lectura, no por la puntuación.

## Ficheros compartidos tocados (REGLAS.md §6)

- `project-spec.md` — **un solo párrafo**, la enmienda B2/B3 (hallazgo 5).
- `features/webhooks.feature` — mío, con dos enmiendas de contrato razonadas
  (filas 327 y 413-414), explicadas arriba.
- `progress/tdd_webhooks.md` y `progress/ux_webhooks.md` — correcciones de
  afirmaciones falsas que el dictamen exigía nominalmente.
- **No** he tocado `frontend/src/App.tsx`, `navigation*`, `scripts/project.mjs`,
  `harness.config.json`, `feature_list.json`, `docker-compose.yml`,
  `scripts/e2e.mjs` ni `.github/workflows/*`.

## Fuera de ámbito, anotado y no tocado (REGLAS.md §9)

- Al pulsar «Desactivar», React desmonta el botón enfocado y el foco cae al
  `body` (el fragmento de dos botones se sustituye por «Activar»). Lo detectó el
  verificador del hallazgo 15 y lo declaró fuera del bloqueante.
- `@s25` pide «latencyMs medido con el reloj inyectado» y `JdkWebhookSender` lo
  mide con `System.nanoTime()` (:58, :117-119); el `Clock` sólo alimenta el `t`
  de la firma. Observación del verificador del hallazgo 17, hallazgo distinto.
- `PostgresWebhookOutbox.readyEndpoints()` filtra por `e.status='active'` y ese
  filtro no lo ejerce ninguna prueba; suprimirlo del SQL no rompe nada hoy
  (parte del hallazgo 18).
