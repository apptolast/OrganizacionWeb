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

**Detalle de la región vacía.** La región de anuncios se monta siempre —un
`role="status"` que aparece y desaparece del DOM no lo anuncia el lector de
pantalla—, así que mientras está vacía debe ocupar cero: `webhooks.scss` gana
`.webhook-announcement:empty { display: none }`. Sin esa regla, el margen del
párrafo vacío desplazaría la geometría que miden los oráculos de recorte y de
objetivo de 44 px del E2E de UX, que este carril no puede ejecutar. Es la opción
conservadora: con ella, el estado vacío deja la maquetación exactamente como
estaba antes del cambio. El mismo patrón `:empty` ya se usa al final de la hoja.

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

---

# Hallazgo 2 — INTENTADO Y NO CERRADO. Trabajo guardado en `git stash`

**No está en el árbol.** Lo dejé en `stash@{0}` («WIP hallazgo 2: oraculo de
recorte por elemento + secreto en textarea (E2E rojo, sin terminar)»), en la
rama `claude/webhooks`. Se recupera con `git stash pop`. El árbol queda en el
último estado verde y commiteado; nada a medias.

## Qué escribí

1. **`e2e/webhooks-ux.spec.mjs`** — dentro del bucle de `WIDTHS`, una medida
   nueva `clipped` y su aserción `expect(measured.clipped, ...).toEqual([])`,
   en los cuatro anchos, los siete estados y los cinco modos que el auditor ya
   recorría. Construida como el verificador exigía:
   - conjunto **NOMBRADO**, nunca `body *` — `main li span` (la URL),
     `main .webhook-secret textarea` (el secreto) y `main tbody td` (las celdas)
     —, porque una lista abierta haría fallar código correcto: el `thead` por
     debajo de 900 px es visually-hidden legítimo (`clip-path: inset(50%)`) y
     ahí `scrollWidth > clientWidth` es lo correcto;
   - **las dos dimensiones**, `scrollWidth > clientWidth + 1` **o**
     `scrollHeight > clientHeight + 1`;
   - aserción sobre la lista de infractores, no sobre un contador, para que el
     fallo diga qué elemento y con qué medidas.
2. **`frontend/src/webhooks.tsx` y `webhooks.scss`** — el cambio de PRODUCTO que
   el cierre exige y que no es una relajación del oráculo: el secreto pasaba de
   `<input readOnly>` a `<textarea readOnly rows={2}>` con
   `field-sizing: content`, y se retiraba la regla muerta
   `overflow-wrap: anywhere` sobre `.webhook-secret input` (un input de una línea
   no envuelve; a 320 px se veían ~34 de 49 caracteres).

## Por qué no lo cierro: la ejecución dio ROJO y no quedaba ventana

`E2E_WEB_PORT=18090 pnpm test:e2e -- e2e/webhooks-ux.spec.mjs` → **5 failed,
1 passed (49,1 s)**. Y el fallo **no es** el oráculo de recorte: es el paso
previo del recorrido de estados,

    e2e/webhooks-ux.spec.mjs:330
    await expect(view.getByLabel("Secreto", { exact: true })).toHaveValue(SECRET);

es decir, **el cambio de producto rompió la localización del campo del secreto**
en los cinco tests que pasan por el estado `secret`. La causa hay que
diagnosticarla con la pila levantada —candidatos: cómo resuelve Playwright
`getByLabel` con un `textarea` envuelto en el `<label>`, o que la imagen web
sirviera un bundle sin reconstruir—, y eso ya no cabía en el plazo.

**Consecuencia honesta:** de este hallazgo no está acreditado ni el rojo del
oráculo ni el verde del conjunto. Lo único demostrado es que el cambio de
producto, tal como lo escribí, rompe cinco tests. Por eso va al stash y no al
árbol: commitear eso sería dejar la rama roja.

## Lo que debe hacer quien lo retome, en este orden

1. `git stash pop` y arreglar primero la localización del campo (línea 330 de la
   spec): decidir entre un `<textarea>` con `id` + `<label htmlFor>` explícito
   —lo más probable que lo resuelva— o mantener el envoltorio y ajustar el
   localizador. Volver a verde los seis tests **antes** de tocar nada más.
2. Con los seis en verde, acreditar el rojo del oráculo con la mutación que el
   dictamen nombra, una línea en `frontend/src/webhooks.scss`:

       li span { white-space: nowrap; overflow: hidden; text-overflow: clip; }

   Debe fallar en `${state}:${width} contenido recortado`. Antes de este trabajo
   la suite quedaba verde con la URL truncada, que es justo el hueco.
3. Restaurar, verde otra vez, y commitear.

**Aviso de solape:** el zoom nativo (hallazgo 3) lo cerró otro carril en
`e2e/webhooks-native-zoom.spec.mjs` con `scrollWidth == clientWidth`. Lo que ahí
no hay es el recorte **por elemento** sobre el conjunto nombrado, que es
exactamente lo que queda pendiente aquí; no lo dupliques al revés.

## Hallazgo 4 — no empezado

No se tocó `e2e/webhooks-ux.spec.mjs` para el recorrido de teclado. El trabajo
sigue descrito arriba, en la sección «Por qué queda abierto cada uno». Sin
cambios respecto a lo ya anotado.

# Recuento final del carril

**Cerrados: 6** — hallazgos 1, 5, 15, 16, 17, 21, todos commiteados y verdes.
**Intentado y devuelto al stash: 1** — hallazgo 2.
**Abiertos: 8** — 2, 4, 9, 10, 11, 12(B), 13, 18. El 3 lo cerró otro carril.

## Hallazgo 2 — el intento, salvado en un parche versionado

El `stash` no sobrevive a nada: no se empuja, no se ve y el próximo que entre en
este worktree no sabría que existe. El intento queda por tanto en
**`progress/parche_webhooks_hallazgo_2.patch`** (99 líneas, generado con
`git stash show -p stash@{0}`). Se aplica con:

    git apply progress/parche_webhooks_hallazgo_2.patch

El `stash@{0}` se deja donde está, por si acaso, pero el parche es la copia que
manda.

### El cambio de producto que probé, exactamente

En `frontend/src/webhooks.tsx`, dentro de `<label>Secreto …</label>`:

    - <input readOnly value={secret} onFocus={(e) => e.currentTarget.select()} />
    + <textarea readOnly rows={2} value={secret}
    +           onFocus={(e) => e.currentTarget.select()} />

Y en `frontend/src/webhooks.scss`, retirar la regla muerta
`overflow-wrap: anywhere` sobre `.webhook-secret input` —un campo de una línea no
envuelve, por eso era muerta— y poner en su lugar:

    .webhook-secret textarea {
      width: 100%;
      resize: none;
      overflow-wrap: anywhere;
      field-sizing: content;
    }

Motivo: a 320 px el `<input>` mostraba ~34 de los 49 caracteres del secreto, es
decir, recorte real. El cierre exigido dice que la respuesta es de producto —un
campo que envuelva—, no relajar el oráculo.

### Por qué rompía `e2e/webhooks-ux.spec.mjs:330`

La línea que falla, en los cinco tests que pasan por el estado `secret`, es:

    await expect(view.getByLabel("Secreto", { exact: true })).toHaveValue(SECRET);

El campo está envuelto por su etiqueta: `<label>Secreto <campo/></label>`, sin
`htmlFor` ni `id`. Con asociación implícita, el nombre accesible sale del
**`textContent` de la etiqueta entera**.

**Hipótesis del diagnóstico (razonada, NO confirmada por ejecución).** React DOM
no trata `<textarea value={…}>` como trata a `<input>`: en el input el valor vive
sólo en la propiedad del nodo y el `textContent` de la etiqueta sigue siendo
`"Secreto"`; en el textarea, React materializa el valor inicial como **texto hijo
del elemento**, de modo que el `textContent` de la etiqueta pasa a ser
`"Secreto" + el secreto entero`. Con `{ exact: true }`, `getByLabel("Secreto")`
deja de casar y el localizador no encuentra nada. Es decir: **el fallo no es del
oráculo de recorte ni del CSS, es del localizador**, y por eso revienta antes,
en el paso previo del recorrido de estados.

Tres hechos que sostienen la hipótesis y descartan las alternativas:

1. **No es un bundle rancio.** Si la imagen web hubiera servido el JavaScript
   viejo, la página tendría todavía el `<input>` y la línea 330 habría pasado.
   Falló, luego el cambio sí llegó al navegador.
2. **No es el `field-sizing` ni el `resize`.** Ninguno afecta al nombre
   accesible, y el fallo es de localización, no de geometría.
3. **Encaja con que los 23 unitarios siguieran verdes**: buscan el campo con
   `getByDisplayValue(secret)`, que lee la propiedad `value` y es indiferente al
   `textContent` de la etiqueta. Sólo el E2E usa `getByLabel(..., exact)`.

### Arreglo que propongo probar primero

Romper la asociación implícita y hacerla explícita, que además es mejor práctica
y deja el nombre accesible aislado del valor:

    <label htmlFor={secretFieldId}>Secreto</label>
    <textarea id={secretFieldId} readOnly … />

con `secretFieldId` de `useId()`, que ya está importado en el fichero. Con la
etiqueta fuera del campo, su `textContent` vuelve a ser exactamente `"Secreto"` y
`{ exact: true }` casa. **No cambiar la spec para acomodar el producto**: el
localizador de la línea 330 es correcto y no debe relajarse.

Orden de trabajo para quien lo retome: (1) aplicar el parche; (2) el arreglo del
`htmlFor`; (3) `E2E_WEB_PORT=18090 pnpm test:e2e -- e2e/webhooks-ux.spec.mjs`
hasta los seis en verde; (4) sólo entonces acreditar el rojo del oráculo con la
mutación que el dictamen nombra —`li span { white-space: nowrap; overflow: hidden;
text-overflow: clip; }`— que debe fallar en `${state}:${width} contenido
recortado`; (5) restaurar, verde y commit.

## Hallazgo 18 — NO empezado, por conflicto de instrucciones que no me toca resolver

El último encargo del coordinador dice dos cosas que no se pueden cumplir a la vez:

- «Con lo que sobre, el **hallazgo 18** (`@s28`: la reactivación no llega a los dos
  eventos posteriores al cursor que el escenario exige), que es de dominio y cabe
  entero.»
- «**No toques** `frontend/src/github-connector*` ni **ningún fichero de backend de
  webhooks**: hay otros dos carriles ahí.»

El hallazgo 18 **es** backend de webhooks. Su cierre, tal como lo redacta el
propio dictamen, exige editar
`backend/src/test/java/com/apptolast/organization/adapter/persistence/WebhookRecoveryPersistenceTest.java`
(montar D2 como entrega real de outbox usando el helper `outbox()` de la línea 92,
insertar dos filas en `outbox_events` posteriores al cursor y afirmar la secuencia
`List.of(D2, E1, E2)`), y probablemente también un caso nuevo sobre la compuerta
`e.status='active'` de `PostgresWebhookOutbox.readyEndpoints()`. No es «de
dominio» en el sentido de dominio puro: es persistencia contra Postgres real.

**Decido no tocarlo.** Entre una asignación de trabajo y una prohibición explícita
de tocar ficheros que otros dos carriles tienen abiertos, la prohibición es la que
protege trabajo ajeno: un conflicto en ese fichero durante la integración puede
costar el trabajo de otro carril, y eso no es reversible desde aquí. Prefiero
devolver el conflicto que resolverlo por mi cuenta.

**Qué necesito para desbloquearlo:** una sola frase del coordinador confirmando
que ningún otro carril tiene abierto `WebhookRecoveryPersistenceTest.java` ni
`PostgresWebhookOutbox`, o bien reasignando el hallazgo 18 al carril que ya esté
dentro de esos ficheros —que además lo cerrará más barato, porque ya tiene el
contexto y los contenedores calientes.

**Lo que sí quedaba en mi ámbito y tampoco empiezo:** el hallazgo 4 (recorrido de
teclado) vive en `e2e/webhooks-ux.spec.mjs`, que no está prohibido. No lo empiezo
porque acreditarlo bien exige **dos** ejecuciones de E2E —una para el verde y otra
para el rojo con la mutación— a ~2,5 min cada una más la escritura, y no caben
antes de la hora de parada. Empezarlo sería repetir exactamente el error del
hallazgo 2: dejar una spec a medias que hay que revertir. El trabajo pendiente
está descrito arriba, con el precedente concreto a copiar
(`e2e/github-connector.spec.mjs:261-291` para el orden y `:302-340` para el anillo
de foco).

---

# SEGUNDA SESIÓN — noche del 9 al 10 de septiembre de 2026

Rama `claude/webhooks` puesta al día sobre `main` (`0c0724f`) antes de empezar.
Pila E2E propia levantada **una sola vez** y dejada viva (`E2E_WEB_PORT=18090`,
proyecto compose `organizationweb-lane25`), para poder iterar con
`pnpm exec playwright test` sin reconstruir en cada ciclo: `.e2e-work/lane-up.sh`.
Cada ciclo con cambio de producto cuesta 31 s de reconstrucción del contenedor
`web`, no los ~2,5 min de levantar y bajar la pila entera.

## Hallazgo 2 — @s42: el recorte por elemento ya se asserta, en los tres sujetos y en las dos dimensiones — CERRADO

**Cubre:** `features/webhooks.feature:521`, «ningún ancho presenta scroll
horizontal **ni recorte de la URL, del secreto ni de la tabla de entregas**».

**La hipótesis de la sesión anterior era correcta.** Aplicado
`progress/parche_webhooks_hallazgo_2.patch` y hecho el arreglo que proponía
—romper la asociación implícita de la etiqueta del secreto y hacerla explícita
con `htmlFor`/`id` sobre un `useId()`—, los **seis** tests de
`e2e/webhooks-ux.spec.mjs` pasan (`6 passed (55,2 s)`). Es decir: el fallo de
`spec:330` (`getByLabel("Secreto", { exact: true })`) era del **nombre
accesible**, no del CSS ni de un bundle rancio, exactamente como decía el
diagnóstico. No se tocó la spec para acomodar el producto.

### El rojo: la mutación que proponía el dictamen NO acredita este oráculo

REPARTO_NOCHE §5 en estado puro. El dictamen mandaba acreditar el rojo con

    li span { white-space: nowrap; overflow: hidden; text-overflow: clip; }

y **eso no sirve**. Ejecutado:

- en el recorrido normal (`four widths in light`) la suite quedó **verde**: a
  320 px con texto normal la URL (194 px) y la descripción (260 px) caben en el
  `li` (294 px de contenido), así que no hay nada que recortar;
- con `text200` sí falló, pero por la **aserción vieja**, no por la nueva:
  `secret:320 horizontal page overflow ... Expected <= 320, Received 560`. El
  `span` no se recortaba, se **ensanchaba**: `scrollWidth == clientWidth == 519`
  y el `li`, que es un contenedor flex, crecía con él y empujaba la página.

La razón es de fondo y queda anotada porque vuelve a aparecer: **un elemento que
recorta no ensancha la página, y uno que ensancha la página no recorta.** Son
sucesos disjuntos. Una mutación que dispara la aserción de scroll nunca puede
acreditar la de recorte.

### Los tres rojos que sí acreditan, uno por sujeto del contrato

| Sujeto | Mutación aplicada a la producción | Rojo obtenido |
|---|---|---|
| **El secreto** | `frontend/src/webhooks.tsx`: devolver el campo a `<input>` de una línea (el producto ANTERIOR a este carril) | `secret:320 contenido recortado` — `INPUT` con `scrollWidth 498` frente a `clientWidth 185`: se veían 185 de 498 px del secreto |
| **La URL** | `webhooks.scss`, `li span { white-space: nowrap; overflow: hidden; max-inline-size: 5rem }` | `secret:320 contenido recortado` — `SPAN` «https://example.com/hooks» con `scrollWidth 194` / `clientWidth 80`, y otro para la descripción (`260` / `80`) |
| **La tabla de entregas** | `webhooks.scss`, `th, td { overflow: hidden; max-block-size: 1rem }` | `deliveries:320 contenido recortado` — 186 celdas con `scrollHeight 27` / `clientHeight 16`: recorte **vertical**, que un oráculo de una sola dimensión no habría visto |

Las tres se restauraron y la suite volvió a verde: **6 passed (1,1 min)**, más
`e2e/webhooks-native-zoom.spec.mjs` **1 passed** (el zoom nativo del hallazgo 3,
de otro carril, sigue verde con el campo nuevo) y los 23 unitarios de
`frontend/src/webhooks.test.tsx`.

El primer rojo es el que más pesa: **es el defecto real de producto** que el
hallazgo denunciaba, y demuestra a la vez que el cambio de `<input>` a
`<textarea>` era necesario y que el oráculo nuevo lo sujeta. Por eso el selector
del oráculo nombra el campo del secreto **como `textarea` y como `input`**: si
alguien lo devuelve a un campo de una línea, la prueba tiene que seguir
mirándolo, no dejar de verlo.

### Cambios de producto, y por qué cada uno

- `webhooks.tsx`: el campo del secreto pasa a `<textarea readOnly rows={2}>` con
  `id`, y su `<label htmlFor>` sale de envolverlo. El comentario del código
  explica el porqué del `htmlFor`: React materializa el valor de un `textarea`
  como texto hijo, así que con asociación implícita el nombre accesible sería
  «Secreto» + el secreto entero.
- `webhooks.scss`: la regla muerta `overflow-wrap: anywhere` sobre
  `.webhook-secret input` desaparece (un campo de una línea no envuelve) y en su
  lugar `.webhook-secret textarea` recibe `width: 100%`, `box-sizing: border-box`
  —sin él el borde suma sobre el 100 % y desborda el panel—, `resize: none`,
  `overflow-wrap: anywhere` y `field-sizing: content`, que hace crecer el alto
  con el contenido y con el texto al 200 %.
- `webhooks.scss`: la regla de tokens `input { … }` pasa a `input, textarea { … }`.
  **Esto no es cosmética**: sin ella el campo nuevo se pintaría con los colores
  del agente de usuario y el contraste se saldría del control de los temas —lo
  habría cazado axe— y perdería el `min-height: 44px` que exige la cláusula de
  objetivo de 44 px del propio @s42.
- `.webhook-secret label { display: block }`, porque al dejar de envolver al
  campo la etiqueta pasó a ser un elemento en línea.

**Ficheros cambiados.** `e2e/webhooks-ux.spec.mjs`, `frontend/src/webhooks.tsx`,
`frontend/src/webhooks.scss`. Ninguno compartido.

# ÍNDICE DE LO ABIERTO — leer sólo esto, no hace falta el dictamen entero

Ocho hallazgos abiertos. Una línea cada uno: qué exige, qué fichero se toca, y si
lo que falta es **oráculo** (la conducta ya es correcta, lo que no hay es prueba
que la sujete) o **producto** (hay que cambiar el comportamiento).

| # | Qué exige | Fichero a tocar | Tipo |
|---|---|---|---|
| **2** | Asertar el recorte POR ELEMENTO sobre el conjunto nombrado —`main li span` (URL), campo del secreto, `main tbody td`— en las dos dimensiones y en los cuatro anchos. Hoy `offenders` se calcula y sólo viaja dentro del mensaje de fallo. | `e2e/webhooks-ux.spec.mjs` + `frontend/src/webhooks.tsx` y `webhooks.scss` | **Oráculo Y producto**: el secreto no cabe a 320 px en un campo de una línea. **Parche listo en `progress/parche_webhooks_hallazgo_2.patch`**, con la hipótesis del fallo y el arreglo propuesto. |
| **4** | Recorrido de teclado real: derivar `expectedOrder` de los enfocables visibles de `main`, sembrar el foco en el `h1`, deduplicar y cerrar con `expect(reached).toEqual(expectedOrder)`. Fuera el umbral `toBeGreaterThan(5)`. | `e2e/webhooks-ux.spec.mjs:372-420` | **Oráculo.** Precedente a copiar: `e2e/github-connector.spec.mjs:261-291`. |
| **12B** | Asertar el **foco visible**, que hoy se mide con `getComputedStyle(active, ":focus-visible")` —pseudo-clase donde la API espera pseudo-elemento, es un no-op— y ni siquiera se asserta: sólo se vuelca a `tab-order.json`. | `e2e/webhooks-ux.spec.mjs:396-406` | **Oráculo.** Precedente: `e2e/github-connector.spec.mjs:302-340`. **Va en el mismo commit que el 4**: es el mismo test. |
| **13** | «Alcanza todos los controles en orden del DOM»: el título lo promete y no se compara ningún orden; 20 controles reales frente a un umbral de 6. | `e2e/webhooks-ux.spec.mjs:372` | **Oráculo. Es el mismo trabajo que el 4**: no son dos commits, es uno. |
| **9** | @s22: recorrer entera la cadena `claim -> lease vencido -> reclaim -> send -> record` y contar las copias que ve el receptor (1 en una fila, 2 en la otra). | `WebhookRecoveryPersistenceTest.java` | **Oráculo.** ⚠️ **EN CURSO EN OTRO CARRIL.** |
| **10** | @s23: el test de concurrencia no discrimina el `SKIP LOCKED`; falta el receptor lento de 500 ms y, sobre todo, la cláusula de **no espera**. | `WebhookWorkPersistenceTest.java:128-144` | **Oráculo.** ⚠️ **EN CURSO EN OTRO CARRIL.** Aviso: la mutación a matar vive en un literal SQL (`FOR UPDATE OF d SKIP LOCKED`, `PostgresWebhookWork.java:68`), la campaña de bytecode no la genera. |
| **11** | @s29: la lectura de `/deliveries` no acota a 50 items, y ni el orden `updatedAt DESC, id DESC` ni la identidad de las 50 supervivientes tienen oráculo. | `PostgresWebhookStore.java:118-128`, `WebhookWorkPersistenceTest.java:180-204` | **Producto Y oráculo, y ADEMÁS necesita decisión del propietario**: `LIMIT 50` contradice la línea 367 del propio contrato (50 terminales + 2 pendientes); la alternativa es enmendar `feature:368` y `project-spec.md:2018`. ⚠️ **EN CURSO EN OTRO CARRIL.** |
| **18** | @s28: la reactivación no llega a los dos eventos posteriores al cursor; y la compuerta `e.status='active'` de `readyEndpoints()` no la ejerce ninguna prueba (suprimirla del SQL no rompe nada hoy). | `WebhookRecoveryPersistenceTest.java:147-216` y `PostgresWebhookOutbox.java:43-55` | **Oráculo.** Bloqueado por el coordinador para no chocar con el carril del backend. |

**Agrupación real para quien reparta trabajo:** los ocho son **cinco** unidades,
no ocho — `4+12B+13` son un solo commit sobre un solo test; `9+10+11` son el
carril de backend ya en marcha; `2` está a medio camino con parche e hipótesis; y
`18` está a la espera de que se libere el backend.

**Corrección documental pendiente, que no debe perderse:** la fila «Posición en
serie» de `progress/ux_webhooks.md` declara «el orden de Tab sigue al DOM —
Verificado en navegador (tab-order.json)». Hoy es **falso**. Quien cierre 4/13
debe corregirla en el mismo cambio.

**Cerrado por otro carril, no lo repitas:** hallazgo 3, zoom nativo al 200 %, en
`e2e/webhooks-native-zoom.spec.mjs`, verde, con los cuatro anchos y
`scrollWidth == clientWidth`. Lo que ese fichero **no** cubre es el recorte por
elemento, que es el hallazgo 2.
