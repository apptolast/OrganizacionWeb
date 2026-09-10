# El punto ciego de la puerta: PIT no muta literales — 10 de septiembre de 2026

Trece agentes: cuatro cazan (una particion por feature) y dos refutan cada caza,
uno buscando falsos positivos y otro buscando lo omitido.

## Por que existe este informe

PIT muta **bytecode** y no muta **literales de cadena**. Toda la conducta que
depende de una cadena -fragmentos de SQL, codigos de error, nombres de columna,
formatos- tiene puntuacion perfecta sin que nadie la haya verificado.

Eso significa que **una campana al 90 % no dice nada** sobre si el ORDER BY es
el correcto, si el LIMIT es el del contrato o si la clausula de backoff existe.
Y un ORDER BY mal puesto pasa todas las pruebas con pocos datos y falla en
produccion cuando hay muchos.

No es una sospecha: el artesano de webhooks ya lo anoto para cuatro literales
suyos y pidio que se juzgaran POR LECTURA. Esto es esa lectura, extendida a las
cinco features.

---

{
  "summary": "Hunt behaviour that depends on string literals, which PIT never mutates, and check whether it has real oracles",
  "agentCount": 13,
  "logs": [
    "34 literales sin oraculo cazados, 7 refutados, 41 anadidos"
  ],
  "result": {
    "vivos": [
      {
        "feature": "25-webhooks",
        "hallazgos": {
          "feature": "25, webhooks",
          "hallazgos": [
            {
              "literal": "`AND d.next_attempt_at <= ?` — C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb/backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresWebhookWork.java:64",
              "queDecide": "La compuerta del backoff: una entrega que fallo y quedo pending con nextAttemptAt futuro no debe reclamarse hasta que venza. Es lo unico que separa los reintentos en el tiempo; el dominio calcula el instante (WebhookDelivery.recorded) pero quien lo hace cumplir es esta linea de SQL.",
              "tieneOraculo": false,
              "prueba": "",
              "consecuenciaSiCambia": "Si se borra la clausula (o se relaja), la entrega recien fallada vuelve a ser reclamable en la misma pasada: DispatchWebhooks.runCycle hace hasta 20 reclamaciones por ciclo, asi que los 6 intentos se queman de golpe contra el receptor caido y el endpoint acaba disabled con DELIVERY_EXHAUSTED en un solo tick, en vez de a lo largo del backoff. NINGUNA prueba de persistencia crea jamas una entrega con next_attempt_at en el futuro: en WebhookWorkPersistenceTest todas nacen con nextAttemptAt=T y se reclaman en T o despues; WebhookRecoveryPersistenceTest solo pone next_attempt_at=NULL (linea 228); DispatchWebhooksTest usa un WebhookWork falso. La suite se queda verde entera.",
              "gravedad": "alta"
            },
            {
              "literal": "`WHERE owner_id=? AND endpoint_id=? AND status='pending' AND event_type=?` — C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb/backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresWebhookStore.java:143-147 (hasPendingPing)",
              "queDecide": "La unica guarda de «un solo ping en vuelo»: ManageWebhook.java:69 devuelve 409 WEBHOOK_DELIVERY_PENDING si esto dice true.",
              "tieneOraculo": false,
              "prueba": "",
              "consecuenciaSiCambia": "Cambiar 'pending' por 'succeeded' invierte la guarda: se aceptan pings ilimitados mientras uno esta en vuelo y se rechaza el primer ping despues de uno entregado. Quitar `event_type=?` haria que cualquier entrega de outbox pendiente bloquease el ping. Nadie lo veria: hasPendingPing NO se invoca en ninguna prueba de persistencia (grep sobre backend/src: solo aparece en el propio adaptador, en el puerto, y en dos dobles — FakeWebhookDeliveries.java:25 y ConnectorStatusSourcesTest.java:561). El SQL real nunca se ejecuta en la suite.",
              "gravedad": "alta"
            },
            {
              "literal": "`ORDER BY occurred_at, event_id`, la parte `event_id` — C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb/backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresWebhookOutbox.java:66",
              "queDecide": "El desempate del recorrido de la outbox cuando dos eventos comparten occurredAt. EnqueueWebhookDeliveries.enqueueFirstEligible toma el PRIMERO de la lista y adelanta el cursor a (occurredAt, eventId) de ese; el orden lo pone entero este SQL.",
              "tieneOraculo": false,
              "prueba": "",
              "consecuenciaSiCambia": "El `occurred_at` ascendente SI tiene oraculo (ver hallazgo siguiente), pero el desempate no. Si se invierte o se cae, con dos eventos del mismo microsegundo se encola primero el de eventId mayor, el cursor salta a el y el hermano de eventId menor queda POR DETRAS del cursor para siempre: WebhookCursor.precedes lo excluye en todos los ciclos futuros. Perdida silenciosa y permanente de un evento, justo lo que la linea 258 de features/webhooks.feature prohibe («se encola primero el de eventId menor A»). La prueba de esa linea, EnqueueWebhookDeliveriesTest.java:169, corre contra FakeOutbox, que se ordena a si mismo (EnqueueWebhookDeliveriesTest.java:56-58): demuestra que el caso de uso respeta el orden que le den, no que el adaptador lo produzca. En WebhookOutboxPersistenceTest no hay ningun caso con dos candidatos del mismo instante.",
              "gravedad": "alta"
            },
            {
              "literal": "`ORDER BY occurred_at, event_id`, la parte `occurred_at` ASC — mismo fichero, PostgresWebhookOutbox.java:66",
              "queDecide": "Que el recorrido de la outbox vaya de mas antiguo a mas reciente.",
              "tieneOraculo": true,
              "prueba": "C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb/backend/src/test/java/com/apptolast/organization/adapter/persistence/WebhookRecoveryPersistenceTest.java:264-267 (s28_reactivatingResumesFromTheKeptCursorAndDeliversD2AndThenTheTwoLaterEventsInOrder), contra la base real: assertEquals(List.of(eventOfD2, firstLater, secondLater), sender.sent...)",
              "consecuenciaSiCambia": "Con DESC se encolaria secondLater (T+3) antes que firstLater (T+2), el cursor saltaria a T+3 y firstLater se perderia: la lista enviada seria [D2, secondLater] y la asercion de la linea 264 se pone roja. Oraculo bueno y ademas ejercitado contra PostgreSQL, no contra un doble.",
              "gravedad": "baja"
            },
            {
              "literal": "`ORDER BY d.next_attempt_at, d.id` — C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb/backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresWebhookWork.java:67",
              "queDecide": "El orden en que el worker reclama entregas vencidas: FIFO por vencimiento.",
              "tieneOraculo": false,
              "prueba": "",
              "consecuenciaSiCambia": "Con DESC el worker sirve siempre lo mas nuevo primero: con cola sostenida, las entregas mas antiguas se quedan sin enviar indefinidamente (inanicion) y el reintento de una entrega vieja se pospone tras cada evento nuevo. Ninguna prueba lo distingue. La unica que menciona ese orden es WebhookWorkPersistenceTest.java:211, y lo usa en SU PROPIA consulta para elegir que fila retener, no para afirmar cual reclama el producto; su asercion es assertNotEquals(held, claimed) (linea 247), que sigue verde con el orden invertido, porque entonces reclama la otra fila igualmente. En el resto de casos solo hay una entrega reclamable a la vez (s29 encola y reclama de una en una; s28 mantiene una sola entrega de outbox en vuelo), asi que el orden nunca se observa.",
              "gravedad": "media"
            },
            {
              "literal": "`FOR UPDATE OF d SKIP LOCKED`, la parte `SKIP LOCKED` — PostgresWebhookWork.java:68",
              "queDecide": "Que una fila retenida por otro worker se salte en vez de esperarla.",
              "tieneOraculo": true,
              "prueba": "C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb/backend/src/test/java/com/apptolast/organization/adapter/persistence/WebhookWorkPersistenceTest.java:204-259 (s23_aRowHeldByAnotherTransactionIsSkippedInsteadOfWaitedFor)",
              "consecuenciaSiCambia": "Con `FOR UPDATE` a secas la reclamacion se bloquea contra el tenedor de la fila y el claim.get(10s) de la linea 246 expira: fail() explicito. El artesano ya documento (lineas 187-201) que el oraculo ingenuo de cardinalidad NO discriminaba y que por eso preguntan por la IDENTIDAD de la fila reclamada. Verificado por lectura: el oraculo es correcto y no depende de un cronometro.",
              "gravedad": "baja"
            },
            {
              "literal": "`FOR UPDATE OF d SKIP LOCKED`, la restriccion `OF d` — PostgresWebhookWork.java:68",
              "queDecide": "Que el bloqueo caiga solo sobre webhook_deliveries y NO sobre la fila de webhook_endpoints unida en el JOIN.",
              "tieneOraculo": false,
              "prueba": "",
              "consecuenciaSiCambia": "Sin `OF d` el FOR UPDATE bloquea tambien e: dos workers sobre el MISMO endpoint dejan de ser independientes, porque SKIP LOCKED hace que el segundo se salte TODAS las entregas de ese endpoint (su fila e esta retenida) y reciba Optional.empty aunque haya trabajo. En DispatchWebhooks.runCycle eso termina el ciclo entero de ese worker. La prueba de concurrencia (WebhookWorkPersistenceTest.java:150) no lo detecta: la transaccion de reclamacion es corta —el sleep del receptor ocurre fuera— y sus aserciones son de cardinalidad total (las 10 se reclaman una vez) mas «ambos avanzaron», que se cumplen igual si el perdedor se queda corto y el ganador drena el resto.",
              "gravedad": "media"
            },
            {
              "literal": "`LIMIT ?` con MAX_LISTED_DELIVERIES = 50 — C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb/backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresWebhookStore.java:132 y 31",
              "queDecide": "El tope de la pagina que sirve GET /deliveries (linea 368 del contrato).",
              "tieneOraculo": true,
              "prueba": "C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb/backend/src/test/java/com/apptolast/organization/adapter/persistence/WebhookWorkPersistenceTest.java:377 (s29_fiftyTwoRowsSurviveThePruneAndTheReadServesAtMostFifty): con 52 filas persistidas, assertEquals(50, served.size())",
              "consecuenciaSiCambia": "Un LIMIT 500 devolveria las 52 y la asercion cae; un LIMIT 5 tambien. El caso esta bien montado porque el escenario separa deliberadamente lo que se guarda (52) de lo que se sirve (50), de modo que el tope no puede quedar tapado por la poda.",
              "gravedad": "baja"
            },
            {
              "literal": "`ORDER BY updated_at DESC, id DESC LIMIT ?` de la poda, y `KEPT_TERMINAL_DELIVERIES = 50` — PostgresWebhookWork.java:131 y 34",
              "queDecide": "Que terminales sobreviven a la poda: las 50 de mayor updatedAt.",
              "tieneOraculo": true,
              "prueba": "WebhookWorkPersistenceTest.java:363-369: assertEquals(recorded.subList(5,55).reversed(), survivors) mas la asercion de que las 5 mas antiguas son las podadas",
              "consecuenciaSiCambia": "Con ASC sobrevivirian las 50 mas antiguas y la comparacion de identidades falla; con LIMIT distinto falla el assertEquals(52, persisted.size()). La asercion compara identidades y orden, no cardinalidad, que es justo lo que hace falta aqui: el comentario de la linea 362 lo dice. Bien cubierto.",
              "gravedad": "baja"
            },
            {
              "literal": "`e.status = 'active'` en la reclamacion (PostgresWebhookWork.java:65) y en readyEndpoints (PostgresWebhookOutbox.java:47)",
              "queDecide": "Que un endpoint deshabilitado ni entregue lo que ya tenia pendiente ni acumule nuevas entregas.",
              "tieneOraculo": true,
              "prueba": "WebhookWorkPersistenceTest.java:123-129 (s22_aDisabledEndpointNeverYieldsItsPendingDeliveries) para el de la reclamacion; WebhookRecoveryPersistenceTest.java:299-331 (s28_theEnqueuerIgnoresADisabledEndpointAndResumesItOnceReactivated) para el del encolador",
              "consecuenciaSiCambia": "Ambos casos ponen el literal en tension en las dos direcciones (deshabilitado no rinde nada, reactivado vuelve a rendir), asi que cambiarlo por 'disabled' o quitarlo pone rojo. Merece nota: el segundo existe precisamente porque en el escenario grande de @s28 esta clausula queda TAPADA por el NOT EXISTS del mismo SQL; el artesano lo documento en el javadoc de la linea 316 y monto el caso aislado. Ese razonamiento es correcto.",
              "gravedad": "baja"
            },
            {
              "literal": "Desempate `, id DESC` en las dos lecturas por updated_at: PostgresWebhookStore.java:131 (pagina servida) y PostgresWebhookWork.java:131 (poda)",
              "queDecide": "Que fila gana cuando dos entregas comparten updatedAt exacto: en la pagina, cual sale antes; en la poda, cual de las empatadas sobrevive.",
              "tieneOraculo": false,
              "prueba": "",
              "consecuenciaSiCambia": "El contrato lo nombra explicitamente («ordenados por updatedAt DESC y despues id DESC», features/webhooks.feature:368) y para los endpoints SI esta probado con dos UUID fijos que empatan en createdAt (WebhookPersistenceTest.java:152-181, s10). Para las entregas no: en s29 las 50 terminales tienen updatedAt distintos (T+5..T+54) y las dos pendientes, que si empatan en T, quedan fuera del subList(0,50) que se compara (WebhookWorkPersistenceTest.java:379). Quitar el desempate deja el orden de las empatadas a merced del plan de PostgreSQL: paginacion inestable y poda no determinista, sin una sola prueba roja.",
              "gravedad": "baja"
            },
            {
              "literal": "`ORDER BY e.created_at, e.id` en readyEndpoints — PostgresWebhookOutbox.java:51",
              "queDecide": "Nada observable: EnqueueWebhookDeliveries.runCycle itera TODOS los endpoints devueltos (linea 39), asi que el orden no altera ningun resultado.",
              "tieneOraculo": false,
              "prueba": "",
              "consecuenciaSiCambia": "Ninguna prueba lo distingue —isReady usa anyMatch (WebhookOutboxPersistenceTest.java:221-224)— pero tampoco hay conducta de producto que dependa de el mientras el bucle recorra la lista entera. Lo anoto para que conste que se miro y se descarto, no como deuda: si alguien introduce un tope de endpoints por ciclo, este literal pasa a decidir a quien se deja sin atender y entonces si necesitara oraculo.",
              "gravedad": "baja"
            },
            {
              "literal": "Codigos emparejados backend/frontend: WebhookController.java:273-287 (WEBHOOK_NOT_FOUND, WEBHOOK_DISABLED, WEBHOOK_DELIVERY_PENDING, CONNECTORS_DISABLED, WEBHOOK_LIMIT, WEBHOOK_URL_BLOCKED, WEBHOOK_URL_UNRESOLVABLE) frente a C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb/frontend/src/webhooks.tsx:167-181, y los estados pending/succeeded/exhausted mas disabledReason 'DELIVERY_EXHAUSTED' en webhooks.tsx:44-46 y 56",
              "queDecide": "Que mensaje ve el usuario ante cada rechazo, y que etiqueta lleva cada entrega en la tabla.",
              "tieneOraculo": false,
              "prueba": "",
              "consecuenciaSiCambia": "Cada lado tiene oraculo del SUYO —el backend en WebhookApiTest.java:160/222/316/336 y el frontend en sus propios tests con fixtures propias— pero NADA ata los dos: son literales independientes. El e2e que deberia atarlos, C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb/e2e/webhooks-ux.spec.mjs, simula el backend entero con route.fulfill (lineas 76-124: el 409 WEBHOOK_LIMIT y los estados succeeded/exhausted/pending son literales escritos en el propio spec), asi que corre contra un doble, no contra el servidor. Ademas el frontend solo ramifica sobre 4 de los 8 codigos: WEBHOOK_DISABLED, WEBHOOK_DELIVERY_PENDING, WEBHOOK_NOT_FOUND y WEBHOOK_URL_UNRESOLVABLE caen al mensaje generico sin que ninguna prueba lo declare intencionado. Renombrar un codigo en el backend pone rojo el backend, pero anadir uno nuevo, o que el frontend deje de reconocer uno existente, no pone rojo nada.",
              "gravedad": "media"
            }
          ]
        },
        "falsos": [
          "PARCIALMENTE INFLADO (unico reparo real): `FOR UPDATE OF d SKIP LOCKED`, la restriccion `OF d` — PostgresWebhookWork.java:68. El cazador dice que la prueba de concurrencia «no lo detecta» porque sus aserciones son «de cardinalidad total mas ambos avanzaron, que se cumplen igual si el perdedor se queda corto y el ganador drena el resto». Ese razonamiento es FALSO. La asercion que no vio bien es WebhookWorkPersistenceTest.java:169-171: `assertTrue(!first.isEmpty() && !second.isEmpty(), \"both workers made progress\")`. Si el perdedor se queda en CERO reclamaciones, esa asercion se pone roja, no verde. Y sin `OF d` el cero es el resultado probable: `deliverAll` (linea 268-278) hace `if (next.isEmpty()) break;` —abandona a la PRIMERA reclamacion vacia—, y los dos hilos entran en `claimNext` a la vez desde `pool.invokeAll` (linea 158), de modo que en la primerisima reclamacion uno de los dos encuentra la fila `e` retenida por el otro, SKIP LOCKED le hace saltarse TODAS las entregas de ese endpoint (las 10 comparten endpoint), recibe Optional.empty y rompe el bucle con `mine` vacio. Es decir: si existe oraculo, es JUSTO esa linea, y el artesano la escribio a proposito para el caso hermano (el javadoc de las lineas 167-168 la llama «la clausula ninguna instancia espera al bloqueo de fila de la otra»). MATIZ QUE SALVA LA CONCLUSION: ese oraculo es PROBABILISTICO, no determinista —depende de que las dos transacciones de reclamacion (de 1-5 ms) se solapen, con el retardo del receptor fuera de ellas—, asi que puede salir verde por suerte de planificacion. La conclusion del cazador («no hay oraculo fiable para `OF d`») se sostiene; su ARGUMENTO no. Gravedad: la que dice, media, pero el hallazgo debe reescribirse como «oraculo flaky, no inexistente», que es una deuda distinta (una prueba intermitente, no una prueba ciega)."
        ],
        "omitidos": [
          "`AND (occurred_at, event_id) > (?, ?)` — la mitad `event_id` de la tupla, PostgresWebhookOutbox.java:64. El cazador auditó el `ORDER BY occurred_at, event_id` de la linea 66 pero NO la clausula WHERE de la linea 64, que es la otra mitad exacta del mismo mecanismo y la que de verdad destierra al hermano empatado. Degradarla a `occurred_at > ?` a secas: ningun evento que comparta instante con el cursor vuelve a ser candidato jamas. Ninguna prueba lo distingue: WebhookOutboxPersistenceTest.java:104 y 117 y 137 y 156 y 169 y 215 usan siempre cursor (T, NIL) con eventos en T+1/T+2/T+600 —instantes distintos—, y WebhookRecoveryPersistenceTest.java:208/219/220 usa T+1, T+2, T+3, tambien distintos. El unico sitio del repositorio donde dos eventos comparten instante es EnqueueWebhookDeliveriesTest.java:167-178, contra FakeOutbox, que reimplementa el predicado con `cursor.precedes` en Java (lineas 46-48 del fake) en vez de ejercitar el SQL. Nota: el `>` frente a `>=` SI tiene oraculo (con `>=` el cursor reencolaria D2 en bucle y la asercion de WebhookRecoveryPersistenceTest.java:265-268 caeria); lo que no lo tiene es la forma de TUPLA.",
          "`LIMIT 1` de la reclamacion — PostgresWebhookWork.java:69. El cazador reviso las lineas 64, 65, 67, 68 y 131 de este mismo fichero y se salto la 69. Sin el `LIMIT 1` la consulta bloquea con FOR UPDATE TODAS las filas vencidas mientras dura la transaccion de reclamacion, aunque `claimed.getFirst()` siga devolviendo una sola: el valor de retorno es identico, asi que ninguna asercion cambia, pero dos workers dejan de poder trabajar en paralelo sobre la misma cola (el segundo se salta todo lo que el primero tiene retenido). Es el mismo defecto de contencion que `OF d`, con el mismo oraculo flaky de WebhookWorkPersistenceTest.java:169-171 como unica red. Ninguna prueba afirma que la reclamacion toca una fila y no N.",
          "`WHERE d.status = 'pending'` de la reclamacion — PostgresWebhookWork.java:63. Borrarlo NO pone roja ninguna prueba, y ademas por una razon que conviene dejar escrita: queda TAPADO por la clausula del hallazgo 1. `record` escribe `next_attempt_at = result.nextAttemptAt()`, que para succeeded y para exhausted es NULL (fijado en WebhookAttemptTest.java:53), y `NULL <= ?` es NULL, o sea falso: los terminales quedan fuera por la comparacion de fecha, no por el estado. Las dos clausulas se cubren la una a la otra, de modo que la puntuacion de mutacion perfecta de esta clase esconde que ninguna de las dos esta sujeta por si sola. Es el complemento exacto del hallazgo 1 del cazador y refuerza su gravedad: si alguien relaja el `next_attempt_at <= ?`, el `status='pending'` sigue ahi por casualidad, pero al reves no.",
          "Verificado y DESCARTADO, para que conste que se miro: JdkWebhookSender —que el encargo nombraba explicitamente y sobre el que el cazador no dice ni una palabra— NO tiene el punto ciego. Todos sus literales estan atados contra un receptor real en JdkWebhookSenderTest: cabeceras y agente en las lineas 258-262 ('application/json; charset=utf-8', 'OrganizationWeb-Webhooks/1', 'X-OrganizationWeb-Event-Id'), la firma en 285-288, y los codigos de transporte uno a uno —'CONNECTION' en 351, 'DNS' en 360, 'TIMEOUT' en 387, 'TLS' en 408/491/507, 'BLOCKED_ADDRESS' en 518, 'REDIRECT' en 336— mas los plazos de produccion en 368-369. El silencio del cazador sobre este fichero es correcto, no una omision. Unico resto no verificado: el recorrido de causas de `classify` (JdkWebhookSender.java:182-189) devuelve los mismos literales que los catch directos, asi que no pude determinar por lectura si alguna prueba entra por esa rama envuelta o todas por la directa; impacto nulo mientras los dos juegos de literales coincidan.",
          "GRAVEDAD ALTA. La mitad `event_id` de la comparacion de tuplas del cursor: `AND (occurred_at, event_id) > (?, ?)` en C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb/backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresWebhookOutbox.java:65. Es el ESPEJO del hallazgo 3 del cazador: el ORDER BY y el WHERE son las dos mitades de la misma garantia y el cazador solo miro una. La direccion estricta sobre occurred_at si tiene oraculo (con `>=` se reencolaria D2 en cada ciclo y la asercion de WebhookRecoveryPersistenceTest.java:265 se pone roja), pero el desempate por eventId no lo tiene: s18 (WebhookOutboxPersistenceTest.java:104) pone el cursor en (T, NIL) con el candidato en T+1, y s28 (WebhookRecoveryPersistenceTest.java:208-220) usa T+1/T+2/T+3, todos distintos. En ninguna prueba existe un evento cuyo occurred_at coincida EXACTAMENTE con el del cursor, que es la unica situacion en que esta clausula decide algo. Si degenerase a `occurred_at > ?` a secas, todo evento del mismo microsegundo que el ultimo entregado queda por detras del cursor para siempre —perdida silenciosa y permanente, exactamente el defecto que la linea 258 de features/webhooks.feature prohibe— y la suite entera sigue verde.",
          "GRAVEDAD ALTA. El contrato de orden de UUID entre el dominio y PostgreSQL, nunca verificado contra PostgreSQL. C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb/backend/src/main/java/com/apptolast/organization/domain/WebhookCursor.java:24-29 existe SOLO porque UUID.compareTo de Java es con signo y PostgreSQL ordena uuid como bytes sin signo; el javadoc de las lineas 19-23 lo dice con todas las letras: si no coinciden, «la mitad de los ids del primer instante de un endpoint nunca se entregarian». Los dos unicos sitios que ejercitan compareUnsigned —EnqueueWebhookDeliveriesTest.java:55 y ExecuteAutomationsTest.java:628— le pasan el comparador DE PRODUCCION a un doble y despues afirman que el doble quedo ordenado: es una tautologia, el doble no puede discrepar de si mismo. No hay ningun WebhookCursorTest en backend/src/test/java/com/apptolast/organization/domain/ y ninguna prueba de persistencia inserta dos filas de outbox_events con el mismo occurred_at y event_ids a ambos lados del bit de signo (uno con el MSB puesto, otro sin el) para comprobar que el `ORDER BY occurred_at, event_id` y el `(occurred_at, event_id) > (?,?)` del adaptador coinciden con precedes(). El defecto que este codigo dice prevenir es indetectable por la suite.",
          "GRAVEDAD ALTA. El catalogo de los doce tipos suscribibles, cuadruplicado y sin nada que ate las copias: backend/src/main/java/com/apptolast/organization/domain/WebhookIntent.java:11-24 (Java), backend/src/main/resources/db/migration/V23__webhooks.sql:9-13 (CHECK de la base) y C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb/frontend/src/webhooks-client.ts:5-17 (frontend). El decodificador del frontend NO es tolerante: webhooks-client.ts:93-99 lanza «Confirmacion incompatible» si algun tipo devuelto es desconocido O si vienen fuera del ORDEN del catalogo, y ese throw (linea 104) tumba la lista entera, no un elemento. La unica prueba del lado frontend es webhooks-client.test.ts:281-296, que afirma la lista del frontend contra una copia literal de si misma escrita en el propio test. Anadir un decimotercer tipo a WebhookIntent.CATALOG y a la migracion deja todo el backend verde y deja la pagina de webhooks en blanco para cualquier propietario suscrito a el.",
          "GRAVEDAD MEDIA-ALTA. Los vocabularios cerrados del decodificador del frontend, que RECHAZAN en vez de degradar: frontend/src/webhooks-client.ts:20 (disabledReasons), :21 (deliveryStatuses) y :22-29 (errorClasses). Cada uno es una copia independiente de un CHECK de la base —V23__webhooks.sql:16, :37 y :41-42— y, para errorClasses, tambien de los literales de JdkWebhookSender.java:164-188. El cazador anoto las ETIQUETAS de webhooks.tsx:44-46, que degradan sin romper; esto es peor: decodeDelivery lanza en la linea 129 (status desconocido) y en la 138 (errorClass desconocida), y el throw de la linea 144 mata toda la respuesta de GET /deliveries. Anadir una clase de transporte nueva en el sender —por ejemplo un 'PROXY'— pasa los tests del backend (JdkWebhookSenderTest.java:351-518 solo cubre las cinco existentes) y hace ilegible la tabla de entregas. El e2e que deberia atarlo esta simulado con literales escritos en el propio spec (e2e/webhooks-ux.spec.mjs:97-121).",
          "GRAVEDAD MEDIA. Los invariantes cruzados reimplementados a mano en el cliente, sin nada que los ate al CHECK que copian: frontend/src/webhooks-client.ts:146 —`if ((value.status === \"pending\") !== (value.nextAttemptAt !== null)) throw incompatible()`— es una reescritura literal de V23__webhooks.sql:49, y webhooks-client.ts:105-115 lo es de V23__webhooks.sql:24. Son dos literales independientes de la misma regla; solo el lado de la base tiene prueba roja si se toca. Si alguien relaja el CHECK de la migracion, el frontend empieza a lanzar «Confirmacion incompatible» sobre datos que el backend considera legales, y ninguna prueba de ninguno de los dos lados lo ve.",
          "GRAVEDAD MEDIA. Los NULL de limpieza del reenvio: `http_status=NULL, latency_ms=NULL, error_class=NULL, leased_until=NULL` en C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb/backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresWebhookStore.java:206-207. Ninguna prueba observa que la limpieza ocurra sobre una fila SUCIA. El unico caso de reenvio contra Postgres es s31 (WebhookRecoveryPersistenceTest.java:340-390) y su primer intento responde 200, de modo que error_class ya es NULL antes del requeue y http_status/latency_ms los reescribe el segundo intento de todos modos: los tres NULL son invisibles. Quitar `error_class=NULL` deja una entrega pending arrastrando un 'HTTP_ERROR' viejo; el decodificador del frontend lo acepta sin chistar —no hay invariante que ate errorClass a status, solo la de nextAttemptAt de la linea 146— y la tabla muestra una clase de error sobre una entrega que aun no ha fallado.",
          "GRAVEDAD MEDIA. `LIMIT 1` en la reclamacion, PostgresWebhookWork.java:69. Sin el, la transaccion de un worker bloquea con FOR UPDATE TODAS las filas vencidas del lote, y el SKIP LOCKED del otro worker las salta todas y recibe Optional.empty aunque haya trabajo —en DispatchWebhooks.runCycle (DispatchWebhooks.java:35-40) eso termina el ciclo entero. La unica asercion que podria verlo, WebhookWorkPersistenceTest.java:169-171 («both workers made progress»), depende de que los dos hilos se solapen dentro de una transaccion de milisegundos, porque el retardo de 120 ms del receptor esta FUERA de ella (linea 274). No es un oraculo: es una loteria que a veces daria rojo y a veces no, que es la peor de las dos opciones. Es el mismo defecto de forma que el `OF d` que el cazador si vio, y por la misma razon.",
          "GRAVEDAD BAJA. `owner_id=?` en las cuatro lecturas y escrituras de entregas: PostgresWebhookStore.java:130 (list), :146 (hasPendingPing), :190 (find) y :208 (requeue). s11 (WebhookPersistenceTest.java:184-198) prueba el aislamiento por propietario de los ENDPOINTS y s30 (WebhookPersistenceTest.java:231-258) el aislamiento por endpoint_id de las ENTREGAS, pero ningun caso pide la entrega de otro PROPIETARIO. Hoy la clausula es redundante porque ManageWebhook.find (ManageWebhook.java:43-45), invocado desde deliveries() en la linea 59 y desde redeliver() en la 78, ya devuelve 404 por el propietario del endpoint. Es defensa en profundidad sin prueba: si alguien anade un camino que llegue a estas lecturas sin pasar por find, el hueco se abre sin que nada se ponga rojo. Lo anoto para que conste que se miro, no como deuda urgente."
        ]
      },
      {
        "feature": "28-calendario",
        "hallazgos": {
          "feature": "28 — calendario externo",
          "hallazgos": [
            {
              "literal": "La lista de siete códigos repetida en el CHECK de SQL: `last_error TEXT CHECK (last_error IN ('FEED_REJECTED', 'FEED_UNREACHABLE', 'FEED_HTTP_ERROR', 'FEED_TOO_LARGE', 'FEED_UNSUPPORTED_TYPE', 'FEED_MALFORMED', 'SECRET_UNREADABLE'))` — backend/src/main/resources/db/migration/V26__external_calendar.sql:17-19. Es una copia independiente del enum de backend/src/main/java/com/apptolast/organization/domain/FeedError.java:4-19, que se escribe como cadena en PostgresExternalCalendarStore.java:164 (`error.name()`).",
              "queDecide": "Si `commitFailure` puede grabar el fallo. Si el nombre del enum no está en la lista del CHECK, el UPDATE viola la restricción, la transacción revienta y POST /sync devuelve 500 en vez de registrar la sincronización fallida y devolver 200 con `lastError`.",
              "tieneOraculo": false,
              "prueba": "",
              "consecuenciaSiCambia": "Solo DOS de los siete códigos llegan alguna vez a un Postgres real: FEED_HTTP_ERROR en backend/src/test/java/com/apptolast/organization/adapter/persistence/ExternalCalendarPersistenceTest.java:445 y FEED_UNREACHABLE en :465. Los otros cinco (FEED_REJECTED, FEED_TOO_LARGE, FEED_UNSUPPORTED_TYPE, FEED_MALFORMED, SECRET_UNREADABLE) solo se ejercitan contra InMemoryExternalCalendarStore, que no tiene CHECK. Añadir un código nuevo al enum —o renombrar uno— compila, pasa la suite entera y pasa la campaña de mutación al 100 %, y luego en producción el primer feed que falle por esa causa devuelve 500. Peor: es el fallo que más se parece a lo que el producto quiere evitar, porque justo la ruta de error es la que no está probada.",
              "gravedad": "alta"
            },
            {
              "literal": "La lista de siete códigos repetida por tercera vez en el frontend: `const ERRORS = [\"FEED_REJECTED\", ... \"SECRET_UNREADABLE\"]` — frontend/src/external-calendar-api.ts:49-57, usada como lista blanca en :109 (`ERRORS.includes(value.lastError as string)`), cuyo incumplimiento llama a `invalid()` (:82-84).",
              "queDecide": "Si la pantalla de calendario externo se pinta o no. `subscriptionOf` no degrada: si `lastError` trae un código que el frontend no conoce, lanza «Respuesta de calendario externo inválida» y se cae la suscripción entera, no solo el mensaje del error.",
              "tieneOraculo": false,
              "prueba": "",
              "consecuenciaSiCambia": "En el backend solo se afirma UN código como cadena de red: `jsonPath(\"$.subscription.lastError\").value(\"FEED_HTTP_ERROR\")` en backend/src/test/java/com/apptolast/organization/adapter/ExternalCalendarApiTest.java:145 y :213. Los otros seis nunca se comparan contra una cadena en ninguna prueba de backend: HttpCalendarFeedTest compara constantes de enum contra constantes de enum, no nombres. Y el frontend solo se prueba contra respuestas que él mismo fabrica con esos mismos literales (external-calendar.test.tsx:158, :173, :361). Nada ata las dos listas. Renombrar o añadir un código deja al usuario con la pantalla de conectores rota, no con un mensaje peor. Además frontend/src/connectors-catalog.tsx:50-59 declara ERROR_TEXT como `Record<string, string>` y no `Record<FeedError, string>`, así que ahí la deriva ni siquiera la detecta el compilador: cae al genérico «Hay un problema con esta integración» (:67).",
              "gravedad": "alta"
            },
            {
              "literal": "`CHECK (char_length(url_host) BETWEEN 1 AND 253)` — backend/src/main/resources/db/migration/V26__external_calendar.sql:9.",
              "queDecide": "Si PUT /api/v1/me/external-calendar responde 400 VALIDATION_ERROR o 500 cuando el nombre de host es absurdamente largo. La validación de dominio, ExternalCalendarInput.of (backend/src/main/java/com/apptolast/organization/domain/ExternalCalendarInput.java:13-41), acota la etiqueta a 40 y la URL entera a 2048, pero NO acota el host: no hay ningún 253 en todo backend/src/main.",
              "tieneOraculo": false,
              "prueba": "",
              "consecuenciaSiCambia": "Una URL https legítima sintácticamente con un host de 300 caracteres (muy por debajo del tope de 2048) pasa la validación, llega al INSERT de PostgresExternalCalendarStore.java:57-66 y viola el CHECK: el usuario recibe un 500 en vez del 400 con `url / INVALID_VALUE` que promete el contrato. Ninguna prueba usa un host de más de 253 caracteres (ExternalCalendarInputTest solo prueba etiqueta y URL largas, :24 y :60). El literal 253 vive solo en SQL y PIT ni lo ve.",
              "gravedad": "media"
            },
            {
              "literal": "`SUMMARY_LIMIT = 500` en backend/src/main/java/com/apptolast/organization/domain/IcsFeed.java:115, emparejado con `CHECK (char_length(summary) <= 500)` en backend/src/main/resources/db/migration/V26__external_calendar.sql:34.",
              "queDecide": "Si una sincronización con un evento de resumen largo se guarda o revienta. Hoy coinciden y la aritmética también (IcsFeed trunca por puntos de código con `offsetByCodePoints`, IcsFeed.java:128-131, y `char_length` de Postgres cuenta puntos de código), así que el emparejamiento es correcto — pero es correcto por casualidad vigilada, no por prueba.",
              "tieneOraculo": false,
              "prueba": "",
              "consecuenciaSiCambia": "Ninguna prueba inserta jamás un resumen de longitud máxima en un Postgres real: ExternalCalendarPersistenceTest fabrica los resúmenes con `\"Evento \" + uid` (:78) y con literales cortos. Subir SUMMARY_LIMIT sin tocar el CHECK —o al revés— deja todas las pruebas verdes y hace que el primer proveedor con una descripción larga rompa la sincronización entera con un 500, no con un evento truncado.",
              "gravedad": "media"
            },
            {
              "literal": "`COLLATE \\\"C\\\"` dentro de `ORDER BY start_at, uid COLLATE \\\"C\\\"` — backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresExternalCalendarStore.java:178.",
              "queDecide": "El desempate de orden entre eventos que empiezan a la misma hora, y que ese desempate coincida con el que usó el dominio al recortar la instantánea a 500 (`thenComparing(ExternalEvent::uid)`, backend/src/main/java/com/apptolast/organization/domain/ExternalCalendarSnapshot.java:20, que es orden natural de Java = orden C).",
              "tieneOraculo": false,
              "prueba": "",
              "consecuenciaSiCambia": "El ORDER BY sí tiene oráculo —ExternalCalendarPersistenceTest.java:470-514 (@s31) mete dos eventos a las 08:00Z con uid «b» y «a» y exige «E», «A», «B»— pero el COLLATE no lo tiene: todos los uid de todas las pruebas son minúsculas ASCII («a».. «e», «u1».. «u499», «v1», «w1», «dup»), que ordenan igual bajo C que bajo cualquier colación por defecto. Quitar `COLLATE \\\"C\\\"` no pone roja ninguna prueba, y en un despliegue con colación en_US los uid con mayúsculas, guiones o puntos —los normales en iCalendar, tipo `A1B2@google.com` frente a `a1b2@google.com`— salen en un orden distinto del que usó el recorte. Además el índice `external_calendar_events_by_start (owner_id, start_at, uid)` (V26:42-43) se crea con la colación por defecto, así que el COLLATE explícito ya impide usarlo para el desempate: la decisión está tomada en dos sitios y medida en ninguno.",
              "gravedad": "media"
            },
            {
              "literal": "`\"El calendario ocupa más de 1 MiB. Reduce el rango que publica tu proveedor.\"` — frontend/src/external-calendar.tsx:26-27, dentro de FEED_MESSAGES.FEED_TOO_LARGE.",
              "queDecide": "La cifra que el usuario lee para saber cuánto tiene que recortar su calendario. Duplica `HttpCalendarFeed.LIMIT = 1024 * 1024` (backend/src/main/java/com/apptolast/organization/adapter/feed/HttpCalendarFeed.java:67).",
              "tieneOraculo": false,
              "prueba": "",
              "consecuenciaSiCambia": "El corte de tamaño en sí SÍ está anclado —ExternalCalendarWiringTest.java:111 afirma `assertEquals(1024 * 1024, HttpCalendarFeed.LIMIT)`, y sin eso las dos pruebas de frontera de HttpCalendarFeedTest (:272-283) serían tautológicas porque se escriben contra la propia constante—, pero nada ata el texto. Si mañana el límite sube a 2 MiB, el backend obliga a tocar el arnés y se entera, y el usuario sigue leyendo «1 MiB» y recorta su calendario a un tamaño que ya no hacía falta.",
              "gravedad": "baja"
            },
            {
              "literal": "`WINDOW_BEFORE_MS = 24 * 3600 * 1000` y `WINDOW_AFTER_MS = 336 * 3600 * 1000` — frontend/src/external-calendar.tsx:36-37, contra `ExternalEventsRange.MAX = Duration.ofDays(16)` — backend/src/main/java/com/apptolast/organization/domain/ExternalEventsRange.java:14.",
              "queDecide": "Si GET /events responde 200 o 400 OUT_OF_RANGE en cada carga de la pantalla. El frontend pide siempre 360 h (15 días exactos) y el backend acepta hasta 16: hay 24 h de holgura, y esa holgura no la afirma nadie.",
              "tieneOraculo": false,
              "prueba": "",
              "consecuenciaSiCambia": "Cada extremo está anclado por separado —el frontend en external-calendar.test.tsx:494-499 (from 2030-01-06T12:00:00Z, to 2030-01-21T12:00:00Z) y el backend en ExternalEventsRangeTest.java:18-22 (16 días es el último rango aceptado) más ExternalCalendarApiTest.java:283-292 (16 días y un segundo es OUT_OF_RANGE)— pero ninguna prueba afirma la RELACIÓN. Estrechar MAX a 14 días, o ensanchar AFTER, deja las dos suites verdes y hace que toda petición de eventos devuelva 400: el calendario aparece vacío para todos los usuarios a la vez, sin error visible en ninguna campaña.",
              "gravedad": "baja"
            },
            {
              "literal": "`last_status = 'OK'` (PostgresExternalCalendarStore.java:130) y `last_status = 'FAILED'` (:160), leídos de vuelta con `SyncStatus::valueOf` (:222).",
              "queDecide": "El estado que ve el usuario y el que el frontend valida contra su propio `STATUSES = [\"OK\", \"FAILED\"]` (frontend/src/external-calendar-api.ts:48).",
              "tieneOraculo": true,
              "prueba": "backend/src/test/java/com/apptolast/organization/adapter/persistence/ExternalCalendarPersistenceTest.java:310 (`assertThat(...lastStatus()).isEqualTo(SyncStatus.OK)` tras commitSuccess), :446 (`SyncStatus.FAILED` tras commitFailure) y :466. Además el CHECK de V26:16 lista los dos valores y el CHECK `external_calendar_error_needs_failure` (V26:26-27) ata 'FAILED' al hecho de haber error.",
              "consecuenciaSiCambia": "Cambiar 'OK' por cualquier otra cosa hace que `SyncStatus.valueOf` lance al leer —o que el CHECK rechace el UPDATE— y las tres pruebas se ponen rojas. Y el nombre de red está afirmado como cadena en backend/src/test/java/com/apptolast/organization/adapter/ExternalCalendarApiTest.java (jsonPath `$.lastStatus` = \"OK\"). Este par SÍ está atado por los dos lados; lo apunto para que no se confunda con el caso de FeedError, que no lo está.",
              "gravedad": "baja"
            },
            {
              "literal": "La plantilla de auditoría `\"host={} status={} code={} durationMs={}\"` y el centinela `\"NONE\"` — backend/src/main/java/com/apptolast/organization/adapter/logging/Slf4jExternalCalendarAudit.java:18 y :21, más el nombre del logger `\"organization.external-calendar\"` (:13).",
              "queDecide": "Qué se puede leer del registro y, sobre todo, qué NO aparece nunca en él: la ruta secreta de la dirección.",
              "tieneOraculo": true,
              "prueba": "backend/src/test/java/com/apptolast/organization/adapter/logging/ExternalCalendarAuditTest.java:44-46 compara el mensaje formateado carácter por carácter (\"host=feed.example.test status=FAILED code=FEED_HTTP_ERROR durationMs=137\"), :60 hace lo mismo con el caso de éxito y su \"code=NONE\", y :70-73 afirma que no aparece ni \"abc123\" ni \"https://\" ni \"/calendar/\". El nombre del logger está atado porque la prueba se engancha a ese mismo nombre en :21.",
              "consecuenciaSiCambia": "Cualquier retoque de la plantilla o del centinela pone roja la prueba. Este es el contraejemplo útil: es un literal de formato, exactamente la familia que PIT no muta, y aquí sí está juzgado por lectura y por igualdad exacta. Es el patrón que le falta a los códigos de FeedError.",
              "gravedad": "baja"
            }
          ]
        },
        "falsos": [
          "FALSO POSITIVO (parcial) - Hallazgo 3, `CHECK (char_length(url_host) BETWEEN 1 AND 253)` en V26__external_calendar.sql:9. Es cierto que ningun test usa un host de mas de 253 caracteres, pero la consecuencia que afirma el cazador -«el usuario recibe un 500 en vez del 400»- esta refutada por una guardia que SI esta probada y que corre ANTES de cualquier INSERT: SaveExternalCalendar.java:33 llama `guard.check(input.urlHost())` en las tres rutas de escritura (create, relabel, rebind) y convierte el veredicto UNRESOLVABLE en un ValidationException `url / UNRESOLVABLE_HOST`. El oraculo que el cazador no cito: backend/src/test/java/com/apptolast/organization/application/SaveExternalCalendarTest.java:203-213 (@CsvSource {\"UNRESOLVABLE, UNRESOLVABLE_HOST\", \"BLOCKED, BLOCKED_ADDRESS\"}, que ademas afirma `store.find(OWNER).isEmpty()`: no se escribe nada) y backend/src/test/java/com/apptolast/organization/application/OutboundHostGuardTest.java:84-89 (un resolutor que falla da UNRESOLVABLE). Y el veredicto para un host de 300 caracteres solo puede ser UNRESOLVABLE: SystemHostResolver.java:11-17 usa `InetAddress.getAllByName`, y ningun nombre de mas de 253 octetos resuelve por DNS (limite del formato de cable), asi que la lista vuelve vacia -> OutboundHostGuard.java:33 -> 400. El CHECK 253 es defensa en profundidad inalcanzable desde la API, no una bomba de 500. Gravedad real: baja, no media.",
          "MATIZ, NO REFUTA - Hallazgos 1 y 2: el cazador no menciono el unico test que atraviesa backend + Postgres real + frontend real, e2e/external-calendar.spec.mjs:52-68 (@s38), que da de alta calendar.google.com, pulsa «Sincronizar ahora» y exige `expect(synced.status()).toBe(200)` con la fila real en la base. Ese test SI ejercita el CHECK de last_error contra un Postgres de verdad... pero solo para el codigo que salga en tiempo de ejecucion, que sera FEED_HTTP_ERROR (404 de Google) o FEED_UNREACHABLE (sin red) - justo los dos que el cazador ya reconoce cubiertos en ExternalCalendarPersistenceTest.java:445 y :465. Para los otros cinco codigos no aporta nada, asi que el hallazgo 1 se mantiene entero. Y para el hallazgo 2 tampoco es oraculo: comprobe la rama de fallo en frontend/src/external-calendar.tsx:210-229 - si `subscriptionOf` lanzara por un lastError desconocido, `synchronise` cae en `failed(error)` -> `setFailure(UNCERTAIN)` (:184) dejando la suscripcion anterior intacta, de modo que el boton «Eliminar suscripcion» sigue ahi y el e2e pasaria igual. La lista ERRORS del frontend sigue sin atar.",
          "MATIZ, NO REFUTA - Hallazgo 5, `COLLATE \"C\"`: la parte central es correcta (ningun uid de ninguna prueba distingue colaciones; s31 en ExternalCalendarPersistenceTest.java:470-514 usa «a».. «e» y el unico otro sitio que siembra eventos en Postgres, ExternalCalendarTodayApiTest.java:159-160, mete un solo evento 'uid-externo'). Pero las dos consecuencias que enuncia estan infladas para ESTE despliegue: docker-compose.yml:2-7 arranca `postgres:17.11-alpine3.23` sin LANG ni POSTGRES_INITDB_ARGS, y la variante alpine (musl, sin locales) inicializa con colacion C, de modo que hoy `COLLATE \"C\"` es un no-op: ni cambia el orden de los uid con mayusculas ni impide usar el indice external_calendar_events_by_start (V26:42-43), que estaria creado tambien en C. El riesgo real es el simetrico y mas silencioso: como es un no-op aqui, quitarlo tampoco rompe nada -ni en CI ni en produccion- hasta que alguien provisione la base con en_US. Gravedad: baja, no media.",
          "HALLAZGO 2 (lista blanca ERRORS del frontend, `tieneOraculo: false`) — PARCIALMENTE FALSO. La afirmacion «el frontend solo se prueba contra respuestas que el mismo fabrica» no es cierta: hay un oraculo de extremo a extremo que el cazador no vio, en e2e/external-calendar-ux-audit.spec.mjs. Ahi `seedSubscription({status: \"FAILED\", error: \"FEED_HTTP_ERROR\"})` (lineas 123-128) hace un INSERT REAL en la tabla real de Postgres a traves de `sql(...)` (lineas 68-87), la pagina real carga esa fila por GET /api/v1/me/external-calendar contra el backend real, y la linea 148-151 exige `expect(page.getByRole(\"alert\")).toContainText(\"El proveedor respondio\")` — que es el principio literal de FEED_MESSAGES.FEED_HTTP_ERROR (frontend/src/external-calendar.tsx:23-24). Esa cadena ata, en una sola prueba y sin fixtures fabricados: (a) el literal 'FEED_HTTP_ERROR' del CHECK de V26:17-19, (b) el nombre del enum de FeedError.java:10, (c) el `error.name()` de PostgresExternalCalendarStore.java:164, (d) la lista blanca ERRORS de external-calendar-api.ts:52, y (e) la clave de FEED_MESSAGES. Renombrar FEED_HTTP_ERROR pone roja esa prueba. Lo que SI sobrevive del hallazgo: los otros SEIS codigos siguen sin oraculo de extremo a extremo, y ese es el hallazgo real. La gravedad «alta» es correcta para seis de siete, no para los siete.",
          "HALLAZGO 1 (CHECK de siete codigos) — el nucleo se sostiene, pero la frase de apoyo «Solo DOS de los siete codigos llegan alguna vez a un Postgres real: FEED_HTTP_ERROR en ExternalCalendarPersistenceTest:445 y FEED_UNREACHABLE en :465» es incorrecta por dos vias que no cito el cazador. Primera: e2e/external-calendar-ux-audit.spec.mjs:126 inserta 'FEED_HTTP_ERROR' contra el CHECK real (ya contado, pero es una segunda ruta). Segunda, y esta si anade un codigo: e2e/external-calendar.spec.mjs:52-69 pulsa «Sincronizar ahora» contra un proveedor de verdad, ejecuta `commitFailure` de verdad contra Postgres de verdad, y exige `expect(synced.status()).toBe(200)` (linea 59) mas `last_attempt_at IS NOT NULL` (linea 64-69). Si el codigo que produce ese intento no estuviera en el CHECK, la transaccion reventaria y la respuesta seria 500: prueba roja. Cual codigo es depende del entorno (FEED_REJECTED si la guardia o el DNS del contenedor no resuelven calendar.google.com; FEED_UNREACHABLE si no hay salida; FEED_HTTP_ERROR si la hay y devuelve 404), y la linea 63 lo tapa a proposito con `expect([\"OK\",\"FAILED\"]).toContain(...)`, que no mira `lastError`. Es decir: hay un tercer codigo cubierto por accidente, no por diseno, y nadie sabe cual. El hallazgo sigue en pie —cinco de los siete codigos jamas tocan el CHECK— pero la contabilidad estaba mal y merece corregirse antes de citarla en un dictamen."
        ],
        "omitidos": [
          "`label TEXT NOT NULL CHECK (char_length(label) BETWEEN 1 AND 40)` - backend/src/main/resources/db/migration/V26__external_calendar.sql:7, emparejado con `LABEL_LIMIT = 40` de backend/src/main/java/com/apptolast/organization/domain/ExternalCalendarInput.java:9 (contado en puntos de codigo, :17). Es exactamente la misma familia que el hallazgo 4 (SUMMARY_LIMIT 500 vs CHECK 500) y el cazador la dejo fuera. NO HAY oraculo: ninguna prueba mete jamas una etiqueta de longitud maxima en un Postgres real - ExternalCalendarPersistenceTest usa siempre 'Trabajo' (:67), ExternalCalendarTodayApiTest.java:154 tambien, y el e2e usa 'Trabajo'/'Interno'. La frontera de 40 solo se mide en memoria, y encima con el caso que mas puede divergir: ExternalCalendarInputTest.java:82 acepta `\"😀\".repeat(40)`, que son 40 puntos de codigo pero 80 chars de Java. Hoy coincide porque `char_length` de Postgres cuenta puntos de codigo, igual que `codePointCount`; si alguien cambiara la validacion a `length()` o subiera LABEL_LIMIT sin tocar el CHECK, toda la suite y la campana seguirian verdes y el primer usuario con una etiqueta larga se comeria un 500 en el PUT.",
          "Los tres codigos de sobre HTTP que el frontend compara como cadena: `\"CONNECTORS_DISABLED\"` (frontend/src/external-calendar-api.ts:160), `\"EXTERNAL_CALENDAR_NOT_CONFIGURED\"` (:164) y `\"VALIDATION_ERROR\"` (:167), copias independientes de ConnectorsGate.java:59, ExternalCalendarController.java:126-129 y del envoltorio de ApiErrors. Es la misma estructura del hallazgo 2 -codigos emparejados sin nada que los ate- y no aparece en la lista. NO HAY oraculo cruzado: el backend afirma su propio literal (ExternalCalendarDisabledApiTest.java:59 `$.code` = \"CONNECTORS_DISABLED\") y el frontend se prueba contra respuestas que el mismo fabrica con esos literales (external-calendar.test.tsx:245, :379; external-calendar-api.test.ts:48, :229). Consecuencia si el backend renombra uno: el 503 deja de mostrar «Los conectores externos no estan disponibles» y cae en el generico UNCERTAIN (external-calendar.tsx:184), y el 404 de sincronizacion deja de llamar a `forget()` (:226), asi que el usuario se queda mirando una suscripcion que ya no existe. Menos grave que el de FeedError, pero mismo agujero.",
          "Los nombres de campo del payload de eventos: `EVENT_FIELDS = \"uid summary startAt endAt allDay\"` (frontend/src/external-calendar-api.ts:47) y `exact(body, \"configured lastSyncAt lastStatus items\")` (:257), frente a los componentes del record backend/src/main/java/com/apptolast/organization/application/ExternalEventsView.java:9-10 y de ExternalEvent. El cazador dio por atados los literales de la suscripcion, y en efecto el e2e los ata (external-calendar.spec.mjs:32-46: tras el PUT real exige 15 claves y que la pagina pinte «calendar.google.com», lo que obliga a que `subscriptionOf` no lance) - pero la lista de eventos NO esta atada por nadie, porque en el e2e la sincronizacion siempre falla contra Google y la lista queda vacia; nunca viaja un evento del backend real al parser real. Renombrar `startAt` a `start` en ExternalEvent compila, deja verdes las dos suites y en produccion `eventOf` llama a `invalid()`, `loadEvents` hace `setInvalidList(true)` (external-calendar.tsx:132) y el calendario aparece vacio con un aviso generico.",
          "`WINDOW_BEFORE_MS`/`WINDOW_AFTER_MS` (frontend/src/external-calendar.tsx:36-37) frente a `ExternalCalendarSnapshot.BEFORE = 24 h` y `AFTER = 336 h` (backend/src/main/java/com/apptolast/organization/domain/ExternalCalendarSnapshot.java:10-11). El hallazgo 7 comparo la ventana del frontend con `ExternalEventsRange.MAX` (:14) -el riesgo de 400- pero se dejo el emparejamiento que de verdad decide QUE ve el usuario: la ventana que se pide tiene que ser la misma que la que se guardo. NO HAY prueba que las relacione: el backend ancla sus fronteras con instantes literales en ExternalCalendarSnapshotTest.java:18-31 (2030-01-06T12:00Z y 2030-01-21T12:00Z sobre syncAt 2030-01-07T12:00Z) y el frontend con los mismos instantes en external-calendar.test.tsx:494-499, pero por coincidencia de fixture, no por una afirmacion compartida; `ReadExternalCalendarEvents.java:14-24` no recorta nada contra la ventana guardada, solo pasa el rango tal cual al store. Si AFTER bajara a 168 h, ninguna suite se pondria roja y la segunda semana del calendario apareceria vacia sin ningun error visible - el fallo mas silencioso de los dos.",
          "`CHECK (char_length(label) BETWEEN 1 AND 40)` — backend/src/main/resources/db/migration/V26__external_calendar.sql:7, emparejado con `ExternalCalendarInput.LABEL_LIMIT = 40` (backend/src/main/java/com/apptolast/organization/domain/ExternalCalendarInput.java:9, comprobado por puntos de codigo en :17). Es EXACTAMENTE la misma familia que los hallazgos 3 (host 253) y 4 (summary 500) que el cazador si vio, y se le escapo estando en la misma migracion. NO HAY prueba que meta una etiqueta de 40 caracteres en un Postgres real: ExternalCalendarPersistenceTest solo usa 'Trabajo', 'Casa', 'Otro' y 'Suya' (:67, :211, :237, :256, :574); e2e/external-calendar-ux-audit.spec.mjs:48 usa una etiqueta de 12 puntos de codigo; e2e/external-calendar.spec.mjs:21 usa 'Trabajo'. ExternalCalendarInputTest:24 prueba 41 caracteres, pero solo contra el dominio, nunca contra la base. Subir LABEL_LIMIT a 80 sin tocar el CHECK compila, pasa la suite y pasa la campana: la etiqueta 41 deja de ser un 400 con `label / TOO_LONG` y pasa a ser un 500 en el INSERT de PostgresExternalCalendarStore.java:57-66. Gravedad: media.",
          "`\"EXTERNAL_CALENDAR_NOT_CONFIGURED\"` — el par de codigos de problema entre backend y frontend, que es literalmente el punto 3 del encargo («si el codigo que devuelve el backend y el que espera el frontend son dos literales independientes, nada los ata») y el cazador solo miro FeedError. El backend lo emite y lo afirma contra su propio literal en backend/src/test/java/com/apptolast/organization/adapter/ExternalCalendarApiTest.java:197 (`jsonPath(\"$.code\").value(\"EXTERNAL_CALENDAR_NOT_CONFIGURED\")`, 404). El frontend lo compara contra su propio literal en frontend/src/external-calendar-api.ts:164, y lo afirma contra su propio fixture en frontend/src/external-calendar-api.test.ts:229 y frontend/src/today-external-calendar.test.tsx:157. NO HAY prueba que los ate: `grep -rn \"EXTERNAL_CALENDAR_NOT_CONFIGURED\" e2e/` no devuelve nada. Si el codigo deriva, `refuse()` (external-calendar-api.ts:158-189) cae al `throw response` final y el usuario deja de ver la rama «no hay ningun calendario externo configurado» para ver el fallo crudo. Comparese con `\"VALIDATION_ERROR\"`, que SI esta atado por e2e/external-calendar.spec.mjs:113, y con `\"CONNECTORS_DISABLED\"`, cuyo lado del backend esta atado de refilon por e2e/github-connector.spec.mjs:148-152 aunque la copia de external-calendar-api.ts:160 no lo este. Gravedad: media-alta.",
          "`ExternalCalendarStatusSource.ID = \"external_calendar\"` — backend/src/main/java/com/apptolast/organization/application/ExternalCalendarStatusSource.java:15, emparejado con la posicion 5 de `CONNECTOR_ORDER` en frontend/src/connectors-catalog-client.ts:19. El comentario de :9-13 de ese fichero declara la conducta: «comprueba que vengan asi y se niega a pintar un catalogo que llegue de otra manera», o sea que la lista blanca es ordenada y su incumplimiento no degrada, tumba la pantalla de conectores entera. El backend afirma la lista contra sus propios literales en ConnectorCatalogApiTest.java:41 y ConnectorCatalogWiringTest.java:29; el frontend contra los suyos en connectors-catalog.test.tsx. NO HAY ninguna prueba que los ate: no existe ningun spec de e2e que abra /conectores (el unico e2e que toca el catalogo es github-connector.spec.mjs, y solo mira su propia pantalla). Renombrar o reordenar el id del carril 28 deja verde toda la suite de backend, toda la de frontend y la campana de mutacion, y deja al usuario sin catalogo de conectores — no sin una fila, sin la pantalla. Gravedad: media-alta por radio de explosion.",
          "`WINDOW_BEFORE_MS = 24 * 3600 * 1000` / `WINDOW_AFTER_MS = 336 * 3600 * 1000` (frontend/src/external-calendar.tsx:36-37) contra `ExternalCalendarSnapshot.BEFORE = Duration.ofHours(24)` y `AFTER = Duration.ofHours(336)` (backend/src/main/java/com/apptolast/organization/domain/ExternalCalendarSnapshot.java:73-74). El cazador emparejo la ventana del frontend con `ExternalEventsRange.MAX = 16 dias`, que es el par flojo (hay 24 h de holgura, romperlo devuelve un 400 ruidoso). El par fuerte es este: son los MISMOS numeros magicos, 24 y 336, escritos dos veces a los dos lados de la red, y describen la misma decision — la ventana que el backend GUARDA y la que el frontend PIDE. Romper este par no da 400 ni error visible: da una cola de dias silenciosamente vacia. Cada extremo esta clavado por separado y, curiosamente, contra los MISMOS instantes literales —ExternalCalendarSnapshotTest.java:20-25 fija los bordes en 2030-01-06T12:00Z y 2030-01-21T12:00Z para SYNC_AT=2030-01-07T12:00Z, y frontend/src/external-calendar.test.tsx:495-499 espera exactamente `from: \"2030-01-06T12:00:00Z\", to: \"2030-01-21T12:00:00Z\"`—, asi que la deriva obliga a tocar una prueba a cada lado y no es del todo invisible; pero la RELACION no la afirma nadie, y ninguna prueba llega a esos dos ficheros a la vez. Gravedad: media."
        ]
      },
      {
        "feature": "30-automatizaciones",
        "hallazgos": {
          "feature": "30, automatizaciones",
          "hallazgos": [
            {
              "literal": "\"ORDER BY occurred_at, event_id LIMIT ?\" en PostgresAutomationWork.window(), backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresAutomationWork.java:274",
              "queDecide": "El orden en que el ejecutor recorre la outbox de cada propietario. El cursor solo avanza hacia delante: procesar en otro orden equivale a saltarse eventos para siempre.",
              "tieneOraculo": false,
              "prueba": "",
              "consecuenciaSiCambia": "Con DESC (o sin ORDER BY) el ciclo procesa primero el evento mas reciente y deja el cursor ahi; todos los eventos anteriores quedan permanentemente por debajo del cursor y no disparan ninguna regla jamas. Silencioso: no hay error, solo automatizaciones que no ocurren. El unico test que toca esta clase contra PostgreSQL, AutomationExecutionTest, nunca tiene mas de una fila candidata por llamada a after(): s19, s23, s25 y s26 insertan un unico evento por propietario. El orden de tupla que exige @s17 (features/automations.feature:232) solo se comprueba contra FakeWork.after(), que ordena en Java dentro del propio test (ExecuteAutomationsTest.java:620-628); esa prueba no puede distinguir el literal SQL.",
              "gravedad": "alta"
            },
            {
              "literal": "\" AND status = 'retry'\" en el UPDATE de reintento, PostgresAutomationWork.claim(), backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresAutomationWork.java:192",
              "queDecide": "Si un segundo o tercer intento puede reclamar la fila abierta. Es la escalera de reintentos completa (attempt 1 -> 2 -> 3 -> failed) que exige @s22.",
              "tieneOraculo": false,
              "prueba": "",
              "consecuenciaSiCambia": "Cambiado a 'failed' o borrado, el UPDATE afecta 0 filas, claim() lanza AutomationClaimedException, ExecuteAutomations lo interpreta como 'otro worker gano' (ExecuteAutomations.java:89) y el paseo continua: el reintento nunca aterriza y la ejecucion se queda clavada en attempt 2 para siempre, sin error visible. Ningun test de integracion deja nunca una fila en 'retry' antes de un ciclo: AutomationExecutionTest solo crea filas de primer intento. La escalera de reintentos solo se prueba con el doble en memoria (ExecuteAutomationsTest.java:350 y :377), que no ejecuta ese UPDATE.",
              "gravedad": "alta"
            },
            {
              "literal": "\"blocked\".equals(row.getString(\"status\")) en PostgresAutomationWork.event(), backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresAutomationWork.java:299",
              "queDecide": "Si una fila de la outbox marcada como bloqueada se salta (avanza el cursor sin producir nada) o dispara reglas. Es la mitad de @s17.",
              "tieneOraculo": false,
              "prueba": "",
              "consecuenciaSiCambia": "Si el literal deja de coincidir, candidate.blocked() es siempre false y las reglas se ejecutan sobre eventos que la outbox retuvo a proposito: se crean tareas y se encolan webhooks a partir de eventos que el sistema decidio no publicar. Ningun test inserta una fila blocked y la hace pasar por este adaptador: la bandera se fabrica a mano en el doble (ExecuteAutomationsTest.java:532, usada en :164). AutomationPersistenceTest.java:315 si inserta una fila blocked, pero prueba PostgresAutomationEvents.recent() y su literal 'status <> blocked' (PostgresAutomationEvents.java:130), que es otra consulta distinta.",
              "gravedad": "alta"
            },
            {
              "literal": "\"SELECT \" + RUN_COLUMNS + \" FROM automation_runs WHERE owner_id = ? AND event_id IN (...)\" en PostgresAutomationWork.withTheirRuns(), backend/.../PostgresAutomationWork.java:250-257 (con RUN_COLUMNS en :40-42)",
              "queDecide": "Si el ejecutor ve las ejecuciones previas de cada evento. De ahi salen la idempotencia (una regla ya resuelta no se reintenta) y el numero de intento.",
              "tieneOraculo": false,
              "prueba": "",
              "consecuenciaSiCambia": "Si esta consulta devolviera vacio siempre (columna mal escrita, IN mal armado, filtro de owner de mas), attemptOf() (ExecuteAutomations.java:143) construiria siempre attempt 1: la escalera de reintentos se colapsa y las filas en 'retry' quedan huerfanas, enmascarado por el ON CONFLICT DO NOTHING. Ningun test de integracion deja de forma determinista una fila en automation_runs antes de que un ciclo la lea: en s25 la caida hace rollback y la fila no llega a existir, y en s19/s26 el ciclo es el primero. La propiedad 'una fila resuelta no se reintenta' solo se prueba con el doble (ExecuteAutomationsTest.java:399).",
              "gravedad": "alta"
            },
            {
              "literal": "\"(occurred_at, event_id) > (?, ?)\" en PostgresAutomationWork.window(), backend/.../PostgresAutomationWork.java:273",
              "queDecide": "Que el evento que esta exactamente en el cursor no se vuelva a leer. Es la frontera del recorrido.",
              "tieneOraculo": false,
              "prueba": "",
              "consecuenciaSiCambia": "Con >= el evento del cursor se relee en cada ciclo; si en ese intervalo el propietario crea una regla nueva, esa regla dispara sobre un evento que ya estaba consumido, creando una tarea o un webhook que no deberia existir. Ningun test lo distingue: el cursor que siembra AutomationExecutionTest.owner() (AutomationExecutionTest.java:253-256) es (now - 1 hora, uuid nulo) y no coincide con ninguna fila de la outbox, asi que > y >= devuelven exactamente lo mismo en los cuatro escenarios. La estrictez si esta probada en el dominio, AutomationCursor.precedes (AutomationCursor.java:18-24), pero ese es otro literal.",
              "gravedad": "media"
            },
            {
              "literal": "\" AND e.status = 'active'\" en el INSERT ... SELECT de PostgresAutomationWork.queue(), backend/.../PostgresAutomationWork.java:167",
              "queDecide": "Que no se encole una entrega hacia un endpoint desactivado en el hueco entre la comprobacion del caso de uso y la escritura.",
              "tieneOraculo": false,
              "prueba": "",
              "consecuenciaSiCambia": "Si se pierde el literal, una regla NOTIFY_WEBHOOK cuyo endpoint se desactivo entre la comprobacion y el commit encola igualmente la entrega: el propietario recibe avisos por un endpoint que dio de baja. Ninguna prueba llega a queue() con un endpoint no activo, porque ExecuteAutomations.java:192 corta antes con ENDPOINT_NOT_FOUND; ese corte si tiene oraculo (AutomationWiringTest.java:148 y ExecuteAutomationsTest.java:264-265, :286), pero prueba el otro literal, no este. El unico escenario que ejecuta queue(), s26 (AutomationExecutionTest.java:133), usa un endpoint activo.",
              "gravedad": "media"
            },
            {
              "literal": "\"ON CONFLICT (owner_id) DO NOTHING\" en PostgresAutomationWork.startCursor(), backend/.../PostgresAutomationWork.java:88-89",
              "queDecide": "Que dos workers que arrancan a la vez el mismo propietario no se pisen el cursor: gana el primero que escribio y el recorrido sigue desde ahi.",
              "tieneOraculo": false,
              "prueba": "",
              "consecuenciaSiCambia": "Con DO UPDATE, el segundo worker rebobina el cursor del primero al instante de la regla mas antigua y se reprocesa toda la ventana: tareas duplicadas hasta donde llegue el ON CONFLICT de automation_runs, y trabajo repetido en cada arranque. Este metodo no se ejecuta nunca contra PostgreSQL: AutomationExecutionTest.owner() (AutomationExecutionTest.java:252-257) siembra siempre la fila de automation_cursors, asi que work.cursor(owner) ya devuelve valor y startCursorOf no se llama. @s16 solo se prueba con el doble (ExecuteAutomationsTest.java:614-618).",
              "gravedad": "media"
            },
            {
              "literal": "\"(executed_at < ? OR (executed_at = ? AND id < ?))\" y \"ORDER BY executed_at DESC, id DESC\" en PostgresAutomationRuns.following()/first(), backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresAutomationRuns.java:52 y :57-58",
              "queDecide": "La paginacion del historial de una regla. El DESC sobre executed_at si esta probado; el desempate por id, no.",
              "tieneOraculo": false,
              "prueba": "backend/src/test/java/com/apptolast/organization/adapter/persistence/AutomationPersistenceTest.java:271-273 prueba el orden por executed_at DESC, pero con tres executed_at distintos (T0, T0+1, T0+2): el desempate por id nunca se ejerce.",
              "consecuenciaSiCambia": "Si \"id < ?\" pasara a \"id > ?\" (o el ORDER BY perdiera el id DESC), la pagina siguiente repetiria o se saltaria ejecuciones en cuanto dos filas de la misma regla compartan executed_at. No es hipotetico: todas las ejecuciones de un mismo ciclo se sellan con la misma captura de reloj (ExecuteAutomations.java:225). El usuario veria el historial con huecos o con filas duplicadas y ninguna prueba se pondria roja.",
              "gravedad": "media"
            },
            {
              "literal": "Los tres estados de ejecucion, escritos tres veces por separado: ExecuteAutomations.java:18-21 (SUCCEEDED/FAILED/RETRY), el CHECK del esquema en backend/src/main/resources/db/migration/V28__automations.sql:24, y RUN_STATUSES en frontend/src/automations-api.ts:70",
              "queDecide": "Que el frontend acepte la pagina de historial: automations-api.ts:225 valida cada fila contra RUN_STATUSES y descarta la respuesta entera como incompatible si el estado no esta en la lista.",
              "tieneOraculo": false,
              "prueba": "",
              "consecuenciaSiCambia": "Si el backend cambiase 'succeeded' por 'success', el backend seguiria verde (sus propias pruebas leen su propia constante: ExecuteAutomationsTest.java:129, :250) y el frontend seguiria verde (sus pruebas usan su propia lista: automations.test.tsx:423), pero el historial en produccion fallaria entero con \"Respuesta de automatizaciones incompatible\". Nada ata los tres literales: no hay prueba de contrato, y tampoco hay red e2e que lo pille, porque app.automations.enabled no esta puesto en ningun sitio de e2e/ ni de docker-compose.yml, asi que el worker no llega a ejecutarse de punta a punta en ninguna prueba.",
              "gravedad": "media"
            },
            {
              "literal": "\"LIMIT ?\" alimentado por BATCH = 200, backend/.../PostgresAutomationWork.java:38 y :274 (argumento en :278)",
              "queDecide": "Cuantas filas de la outbox mira un ciclo por propietario, y por tanto que el recorrido se reanude correctamente entre lotes.",
              "tieneOraculo": false,
              "prueba": "",
              "consecuenciaSiCambia": "Nada prueba que el lote exista ni que el paseo continue en el ciclo siguiente: ningun test hace que after() devuelva mas de una fila. Es exactamente el fallo que solo aparece en produccion, y se multiplica con el hallazgo del ORDER BY: un lote que corta sin un orden garantizado deja eventos por debajo del cursor de forma permanente. Con pocos datos todo pasa.",
              "gravedad": "media"
            },
            {
              "literal": "\"ON CONFLICT (rule_id, event_id) DO NOTHING\" en el INSERT del primer intento, PostgresAutomationWork.claim(), backend/.../PostgresAutomationWork.java:187",
              "queDecide": "Que dos workers concurrentes ejecuten la regla como maximo una vez por evento (@s23).",
              "tieneOraculo": true,
              "prueba": "backend/src/test/java/com/apptolast/organization/adapter/config/AutomationExecutionTest.java:77-99: si el conflicto no se resolviese en DO NOTHING, el segundo worker no veria affected == 0, no lanzaria AutomationClaimedException y la tarea se crearia dos veces, con lo que la asercion de la linea 93 (count de tareas automatizadas == 1) fallaria.",
              "consecuenciaSiCambia": "Se ejecutaria la regla dos veces por evento: tarea duplicada y evento TaskCreated.v1 duplicado. El oraculo existe pero es una carrera real con CyclicBarrier: solo mata al mutante cuando los dos hilos llegan de verdad al INSERT a la vez, asi que es un oraculo probabilistico, no determinista. Merece un test que fuerce el conflicto insertando la fila antes del ciclo.",
              "gravedad": "media"
            },
            {
              "literal": "\"'pending'\" como estado de la entrega en el INSERT de webhook_deliveries, PostgresAutomationWork.queue(), backend/.../PostgresAutomationWork.java:165",
              "queDecide": "Que la entrega encolada por una regla NOTIFY_WEBHOOK quede pendiente de envio para la feature 25.",
              "tieneOraculo": true,
              "prueba": "backend/src/test/java/com/apptolast/organization/adapter/config/AutomationExecutionTest.java:147 afirma containsEntry(\"status\", \"pending\") sobre la fila de webhook_deliveries.",
              "consecuenciaSiCambia": "Con otro literal el INSERT reventaria contra el CHECK de V23__webhooks.sql:37, o la entrega naceria ya resuelta y nunca se enviaria. Aqui si hay lectura que lo distingue.",
              "gravedad": "baja"
            },
            {
              "literal": "\"ORDER BY occurred_at DESC, event_id DESC\" en PostgresAutomationEvents.recent(), backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresAutomationEvents.java:131",
              "queDecide": "El orden de las coincidencias de la simulacion, que @s30 (features/automations.feature:397) exige \"en orden occurredAt, eventId descendente\", y que ventana de 100 eventos se evalua.",
              "tieneOraculo": false,
              "prueba": "backend/src/test/java/com/apptolast/organization/adapter/persistence/AutomationPersistenceTest.java:320-323 prueba occurred_at DESC, pero las cuatro filas tienen instantes distintos (lineas 313-316): el desempate por event_id DESC no lo distingue ninguna prueba.",
              "consecuenciaSiCambia": "Con dos eventos del mismo instante, la simulacion los mostraria en orden arbitrario o invertido respecto al contrato, y la ventana de 100 cortaria por el evento equivocado. Es cosmetico frente al resto, pero el contrato lo escribe explicitamente y nadie lo mide.",
              "gravedad": "baja"
            },
            {
              "literal": "\"SELECT DISTINCT owner_id FROM automation_rules\" en PostgresAutomationWork.ownersWithRules(), backend/.../PostgresAutomationWork.java:66",
              "queDecide": "Que cada propietario se recorra una sola vez por ciclo.",
              "tieneOraculo": false,
              "prueba": "",
              "consecuenciaSiCambia": "Sin DISTINCT, un propietario con N reglas se recorre N veces por ciclo: trabajo repetido y N veces mas consultas contra la outbox. Ningun test de integracion da dos reglas al mismo propietario (AutomationExecutionTest crea siempre una), asi que el mutante sobrevive. Impacto de rendimiento, no de correccion, porque el cursor ya avanzo en la primera pasada.",
              "gravedad": "baja"
            }
          ]
        },
        "falsos": [
          "HALLAZGO 9 (los tres estados 'succeeded'/'failed'/'retry'): el literal del backend SI tiene oraculo, y triple. (a) Las pruebas del caso de uso NO leen la constante como afirma el cazador: escriben el literal a mano. ExecuteAutomationsTest.java:129 dice `containsExactly(tuple(E1, \"succeeded\", 1), tuple(E2, \"succeeded\", 1))`, y lo mismo en :250, :288, :380 y :463 ('succeeded'), :350 ('retry'), :286 y :377 ('failed'). Si ExecuteAutomations.SUCCEEDED pasara a \"success\", esas cinco aserciones se ponen rojas. (b) Las de integracion tambien lo fijan a mano: AutomationExecutionTest.java:69 `.containsEntry(\"status\", \"succeeded\")`, :121 `.containsExactly(\"succeeded\")`, :153. (c) Y aunque alguien tocase todas esas pruebas, el CHECK del esquema (V28__automations.sql:24, `status IN ('succeeded','retry','failed')`) hace reventar el INSERT contra PostgreSQL. Lo mismo por el lado del frontend: automations.test.tsx:422-423 escribe 'succeeded'/'retry'/'failed' a mano, asi que RUN_STATUSES tampoco puede cambiar en silencio. Lo cierto del hallazgo es solo la mitad ultima: no existe prueba de contrato que ate ambos lados, y el e2e no lo cubre (confirmado: `app.automations.enabled` no aparece ni en e2e/ ni en docker-compose.yml, y e2e/automations.spec.mjs nunca lee /runs). Pero el escenario de fallo tal como esta escrito -'el backend seguiria verde'- es falso, y con el se cae la gravedad: el riesgo real exige un renombrado deliberado en cuatro sitios con ocho pruebas en rojo delante, no una deriva silenciosa."
        ],
        "omitidos": [
          "PostgresAutomationWork.record()/upsert() entero, backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresAutomationWork.java:204-212, con su literal `ON CONFLICT (rule_id, event_id) DO UPDATE SET attempt = EXCLUDED.attempt, status = EXCLUDED.status, ...`. Es LA fila que tiene que sobrevivir al rollback (por eso `apart` usa PROPAGATION_REQUIRES_NEW, :60) y NUNCA se ejecuta contra PostgreSQL: `grep -rn '\\.record(' backend/src/test/.../adapter` solo da WebhookWorkPersistenceTest y WebhookRecoveryPersistenceTest, nada de automatizaciones. AutomationExecutionTest s25 llama a work.commit() directamente, no pasa por ExecuteAutomations, asi que la rama de fallo (ExecuteAutomations.java:95-96) jamas se ejerce contra la base. La propiedad entera de @s20/@s22 -la fila 'retry' con STORAGE_UNAVAILABLE sobrevive a la transaccion caida y el reintento renueva SU MISMA fila- solo esta probada contra el doble en memoria (ExecuteAutomationsTest.java:641-644). Si el DO UPDATE se degradara a DO NOTHING, el segundo intento no actualizaria nada y la escalera se congelaria; ninguna prueba se pondria roja. Este es el hueco mas grande de la clase y el cazador no lo menciona.",
          "El emparejamiento de codigos de error entre backend y frontend, que es justo el patron 3 del encargo y no aparece en la lista. FAILURE_LABELS (frontend/src/automations.tsx:49-54) declara PROJECT_COMPLETED, TITLE_TOO_LONG, CRITERION_TOO_LONG, ENDPOINT_NOT_FOUND y TARGET_NOT_FOUND como literales propios; el backend los produce en sitios independientes: AutomationRendering.java:34 ('PROJECT_COMPLETED'), CreateTaskAction.java:36-37 ('TITLE_TOO_LONG', 'CRITERION_TOO_LONG') y ExecuteAutomations.java:20 ('ENDPOINT_NOT_FOUND'). Nada ata las dos listas: si el backend renombrase uno, el frontend seguiria verde (automations-api.test.ts:125 y automations.test.tsx:437 usan sus propios literales) y en produccion la ejecucion fallida se mostraria con el codigo crudo en vez de con su etiqueta. Ademas STORAGE_UNAVAILABLE (ExecuteAutomations.java:22), que es el codigo que MAS se escribe en automation_runs cuando algo va mal, no esta en FAILURE_LABELS en absoluto, y ninguna prueba lo detecta.",
          "RUN_LABELS en frontend/src/automations.tsx:44-46 ({succeeded, retry, failed}) es una CUARTA copia de los tres estados que el hallazgo 9 no cuenta: el cazador enumera tres sitios (ExecuteAutomations, el CHECK de V28 y RUN_STATUSES) y se deja el diccionario que de verdad pinta la etiqueta en pantalla.",
          "El `AND e.owner_id = ?` del INSERT ... SELECT de queue(), PostgresAutomationWork.java:167. El cazador aisla `e.status = 'active'` de esa misma linea pero no el filtro de propietario, que es el que impide encolar una entrega hacia el endpoint de OTRA cuenta si la comprobacion previa fallara. Mismo argumento y mismo hueco: el unico escenario que llega a queue() es s26 (AutomationExecutionTest.java:133) y usa un endpoint propio y activo.",
          "\"ON CONFLICT (rule_id, event_id) DO UPDATE SET attempt = EXCLUDED.attempt, status = EXCLUDED.status, created_task_id = ..., delivery_id = ..., error_code = ..., executed_at = ...\" en PostgresAutomationWork.upsert(), backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresAutomationWork.java:206-211. Es LA fila que sobrevive a un rollback, la pieza central de @s20 y @s22, y no la ejecuta NINGUNA prueba contra PostgreSQL: record() solo se alcanza por el catch de ExecuteAutomations.java:92, y s25 provoca la caida llamando work.commit(...) a pelo (AutomationExecutionTest.java:110), sin pasar por el caso de uso; el runCycle() posterior (:116) no falla. Grep de todo backend/src/test: nadie mas invoca AutomationWork.record contra la base real. Si DO UPDATE fuese DO NOTHING, el primer intento deja su fila y ningun reintento vuelve a escribirla: attemptOf (ExecuteAutomations.java:143) la relee siempre como attempt 1 en 'retry' y el evento se reintenta para siempre sin llegar nunca a 'failed'. El unico oraculo es el doble, cuyo record() se limita a recorded.add(run) (ExecuteAutomationsTest.java:642-644). Gravedad alta.",
          "\" AND o.event_id = ?\" (y sus vecinos \"e.id = ?\" y \"e.owner_id = ?\") en el WHERE del INSERT ... SELECT de PostgresAutomationWork.queue(), PostgresAutomationWork.java:167. Es el join que empareja el endpoint con la fila exacta de la outbox. El unico escenario que ejecuta queue() es s26, y ahi el propietario tiene EXACTAMENTE una fila de outbox (asertado en AutomationExecutionTest.java:157) y exactamente un endpoint (creado en :136-175): si el mutante borra o.event_id = ?, el producto cartesiano sigue devolviendo una sola fila, con el mismo body, y las cuatro aserciones de :143-150 siguen verdes. En produccion, con N eventos en la outbox del propietario, ese mismo mutante encola N entregas por cada regla disparada, cada una con el cuerpo de un evento ajeno. El hallazgo del cazador sobre \"e.status = 'active'\" es el mismo hueco, pero se dejo fuera el literal cuya perdida es catastrofica en vez de meramente indebida. Gravedad alta.",
          "\"'pending', 0\" y los tres argumentos de next_attempt_at, created_at y updated_at en el mismo INSERT, PostgresAutomationWork.java:162-171 con el valor en :159. El cazador acredito 'pending' (oraculo real en AutomationExecutionTest.java:147) pero no miro los vecinos: attempt = 0 no lo afirma nadie, y next_attempt_at se rellena con Timestamp.from(notify.event().occurredAt()), es decir con el instante DEL EVENTO y no con 'ahora'. Ninguna asercion de s26 (:143-156) mira esas tres columnas. La cola de la feature 25 ordena precisamente por next_attempt_at: una entrega que nace con la fecha del evento se despacha con prioridad sobre las demas, o se queda al fondo si el evento es futuro, y nada se pone rojo. Gravedad media.",
          "Los doce literales de AutomationEventType.PUBLISHED, backend/src/main/java/com/apptolast/organization/domain/AutomationEventType.java:10-21, duplicados palabra por palabra en EVENT_TYPES, frontend/src/automations-api.ts:4-17. Es exactamente el pecado del hallazgo de los tres estados, pero doce veces y sobre la pantalla principal en vez de sobre el historial: automation() (automations-api.ts:158-177, la comprobacion en :168) descarta la respuesta ENTERA con \"Respuesta de automatizaciones incompatible\" si el eventType de UNA regla no esta en la lista del frontend. Solo \"TaskCreated.v1\" queda atado de punta a punta, y por casualidad: es el unico tipo que siembra el e2e (e2e/automations.spec.mjs:28) y el unico que la lista de reglas llega a renderizar sobre la pila real. Los otros once no los ata nada: ni prueba de contrato, ni fixture compartido, ni e2e. Gravedad media.",
          "El mapa MESSAGES de frontend/src/automations-api.ts:107-117 frente a los codigos que el backend emite de verdad en esta ruta. AutomationBody.invalid() produce SIEMPRE el codigo \"INVALID_VALUE\" (backend/src/main/java/com/apptolast/organization/adapter/http/AutomationBody.java:121) y ese codigo NO figura en MESSAGES: todo error de forma del editor de reglas cae en el fallback \"?? 'Revisa el valor de este campo.'\" de automations-api.ts:253-254. Hoy el dano es nulo porque el texto del fallback coincide byte a byte con el que manda el backend, pero es coincidencia, no contrato: nada ata las dos listas, y ninguna prueba distingue un codigo mapeado de uno no mapeado (automations-api.test.ts usa sus propios codigos). Gravedad baja.",
          "El mapa FAILURE_LABELS de frontend/src/automations.tsx:49-54 frente a los wouldFail que produce el backend: \"PROJECT_COMPLETED\" (AutomationRendering.java:34) y \"TITLE_TOO_LONG\"/\"CRITERION_TOO_LONG\" (domain/CreateTaskAction.java:36-37). Cinco literales en el frontend, tres emisores en el backend, cero pruebas que los aten; \"TARGET_NOT_FOUND\" esta en el mapa del frontend y ningun wouldFail del backend lo emite (grep: solo aparece en ApiErrors.java:235-240, que es un cuerpo de error HTTP, no una preview). Degrada con gracia (automations.tsx:552 pinta el codigo crudo si no lo conoce), asi que es cosmetico, pero es el mismo par de literales independientes del hallazgo de los estados y el cazador solo miro uno de los dos mapas. Gravedad baja.",
          "Las claves de carga \"taskId\" y \"projectId\" con que las reglas leen la outbox, backend/src/main/java/com/apptolast/organization/domain/AutomationEvent.java:22-23, :36, :43 y el uuid() de :48-50. Ninguna prueba hace pasar por ahi una carga producida DE VERDAD por el emisor: AutomationExecutionTest escribe el payload a mano (:294-299 y :188-194) y el fixture ni siquiera coincide con el evento real (escribe \"status\" donde TaskStatusChanged.v1 lleva fromStatus/toStatus, application/TaskStatusChanged.java), AutomationPersistenceTest.java:334-340 tambien lo escribe a mano, y el e2e no dispara el worker. La consecuencia no es un evento perdido: uuid() hace UUID.fromString(String.valueOf(null)) y lanza IllegalArgumentException, y esa llamada ocurre en matcher.loopGuarded(owner, event), ExecuteAutomations.java:82, FUERA del try que empieza en :85, asi que la excepcion escapa de process(), de walk() y de runCycle() y mata el ciclo entero de TODOS los propietarios, no solo el evento. Probabilidad baja (Jackson serializa los nombres de componente de los records, que hoy coinciden), impacto total, oraculo cero."
        ]
      },
      {
        "feature": "27-29-conectores",
        "hallazgos": {
          "feature": "27 y 29 — conectores GitHub y GitLab (PostgresConnectorConnectionStore, PostgresGitlabConnectionStore, HttpGithubIssueSource, HttpGitlabIssueSource, ConnectorError)",
          "hallazgos": [
            {
              "literal": "El mapa ERROR_TEXT de frontend/src/connectors-catalog.tsx:50-60 (con el respaldo de la linea 67, «Hay un problema con esta integracion») frente a los codigos que el backend sabe escribir en last_error_code: \"GITLAB_UNAVAILABLE\" y RATE_LIMITED en backend/src/main/java/com/apptolast/organization/application/ConnectorFailures.java:64-65, guardados por ImportIssues.java:130 (recordFailure) y publicados tal cual por GitlabStatusSource.java:33.",
              "queDecide": "El texto que la fila del catalogo /conectores ensena como ultimo error de la integracion. El contrato @s33 (features/additional_connectors.feature:424) promete «el ultimo error como codigo traducido», y @s25 (linea 307) obliga a que tras un 503 de GitLab la conexion siga connected con lastError { code: GITLAB_UNAVAILABLE, at }.",
              "tieneOraculo": false,
              "prueba": "",
              "consecuenciaSiCambia": "Hoy mismo, sin cambiar nada: despues de un fallo de GitLab la fila del catalogo dice «Conectado» y debajo «Hay un problema con esta integracion», que no traduce nada. Lo mismo con RATE_LIMITED. Y si el backend renombrase GITLAB_UNAVAILABLE, el frontend seguiria pintando exactamente el mismo texto generico y ninguna prueba se enteraria: son dos listas de literales independientes sin nada que las ate. Las tres pruebas del catalogo que tocan lastError (frontend/src/connectors-catalog.test.tsx:94, :128, :158) usan siempre CONNECTION_INVALID, el unico codigo que si esta en el mapa.",
              "gravedad": "alta"
            },
            {
              "literal": "La ausencia del par \"CONNECTOR_KEY_MISMATCH\" en GitlabConnectorController.java: sus doce @ExceptionHandler (lineas 170-220) no incluyen SecretUndecipherableException. El gemelo si lo tiene: GithubConnectorController.java:183-189 devuelve 503 con codigo CONNECTOR_KEY_MISMATCH.",
              "queDecide": "Que responde una importacion de GitLab cuando APP_CONNECTOR_KEY ya no descifra el token guardado. El caso de uso es compartido y lanza la excepcion para los dos gestores: ImportIssues.java:148 (orElseThrow(SecretUndecipherableException::new)), con un comentario en la linea 143 que dice explicitamente que el adaptador HTTP la traduce a 503 CONNECTOR_KEY_MISMATCH.",
              "tieneOraculo": false,
              "prueba": "",
              "consecuenciaSiCambia": "Con una clave rotada, POST /api/v1/me/connectors/gitlab/imports cae en el @ExceptionHandler(Exception.class) de ApiErrors.java:125 y devuelve un 500 generico con correlationId, en vez del 503 honesto con codigo estable que da GitHub ante el mismo fallo. El frontend de GitLab tampoco tiene entrada para CONNECTOR_KEY_MISMATCH (gitlab-connector.tsx:29-38), asi que aunque se anadiera el handler el aviso saldria generico. No hay ninguna prueba de GitLab que mencione SecretUndecipherableException ni CONNECTOR_KEY_MISMATCH.",
              "gravedad": "media"
            },
            {
              "literal": "\"ratelimit-reset\" en backend/src/main/java/com/apptolast/organization/adapter/connectors/HttpGitlabIssueSource.java:179.",
              "queDecide": "Cuantos segundos se le dice al propietario que espere cuando GitLab agota la cuota y no manda Retry-After. Es la segunda mitad de retryAfter(): sin Retry-After se mira RateLimit-Reset, y solo si tampoco esta se cae al minuto por defecto.",
              "tieneOraculo": false,
              "prueba": "",
              "consecuenciaSiCambia": "Si ese literal fuera otro —por ejemplo \"x-ratelimit-reset\", que es justo el nombre que usa el gemelo de GitHub y lo que saldria de un copiar-pegar— la rama quedaria muerta y todo 429 sin Retry-After daria 60 s fijos en lugar del instante real de reinicio. Ninguna prueba se pondria roja: la tabla del contrato @s24 (features/additional_connectors.feature:295-298) tiene solo tres filas —429 con Retry-After 30, 429 sin Retry-After, y 200 con RateLimit-Remaining 0 + Retry-After 5— y ninguna manda RateLimit-Reset. Grep de «ratelimit-reset» en backend/src/test, e2e, frontend y features: solo aparece el x-ratelimit-reset de GitHub, que si tiene tres pruebas (HttpGithubIssueSourceTest.java:216, :234, :249).",
              "gravedad": "media"
            },
            {
              "literal": "ISSUES_QUERY = \"state=opened&per_page=100\" en GitlabApiBase.java:113, frente a ISSUES_QUERY = \"state=open&per_page=100&sort=created&direction=desc\" en GithubApiBase.java:19.",
              "queDecide": "Cuales son las 200 issues que se convierten en tareas cuando el proyecto tiene mas de 200 abiertas. La importacion corta en dos paginas (MAX_PAGES en ImportIssues.collect) y marca truncated=true; el orden es lo unico que decide que queda dentro. GitHub lo fija en el literal; GitLab no pide ningun order_by ni sort y se queda con el que el servidor tenga por defecto.",
              "tieneOraculo": false,
              "prueba": "",
              "consecuenciaSiCambia": "Este es el caso del «pasa todas las pruebas con pocos datos y falla en produccion»: con 2, 37 o 140 issues —lo unico que ejercita @s16 (features/additional_connectors.feature:191-200)— el orden da igual. Con 250 issues abiertas, que 200 entren y cuales 50 se pierdan depende de una decision que no esta escrita en ninguna parte del codigo ni verificada por nadie, y basta con que una instancia autoalojada de GitLab tenga otro defecto para que el truncado deje fuera issues distintas. Ojo: el literal en si NO es libre —HttpGitlabIssueSourceTest.java:147 fija la query exacta, asi que un mutante que anadiera order_by moriria—; lo que no tiene oraculo es el orden del que depende el recorte.",
              "gravedad": "media"
            },
            {
              "literal": "Las claves del mapa MESSAGES en frontend/src/gitlab-connector.tsx:29-38 (\"GITLAB_UNAVAILABLE\", \"CONNECTION_INVALID\", \"IMPORT_IN_PROGRESS\", \"PROJECT_COMPLETED\", \"STORAGE_UNAVAILABLE\"...) frente a los mismos literales escritos a mano en GitlabConnectorController.java:188, :193, :198, :226, :253-256.",
              "queDecide": "El aviso que la pantalla /conectores/gitlab ensena ante cada fallo, y con el la accion sugerida: gitlab-connector.tsx:225 decide si ofrece «Actualizar token» comparando failure.code === \"CONNECTION_INVALID\", y describeFailure() (linea 53) trata RATE_LIMITED aparte para pintar los segundos.",
              "tieneOraculo": false,
              "prueba": "",
              "consecuenciaSiCambia": "Cada lado tiene sus pruebas —GitlabConnectorApiTest.java:284 y :305 comprueban los codigos del backend, gitlab-connector.test.tsx los del frontend— pero cada una escribe el literal a mano en su propio fichero. Nada ata los dos conjuntos: renombrar un codigo en el controlador deja verde toda la suite del backend y toda la del frontend, y en la pantalla el propietario ve «No se pudo completar. Intentalo mas tarde» y pierde el boton «Actualizar token» que resolvia su problema. No hay e2e de GitLab que cruce la frontera (e2e/ solo tiene github-connector.spec.mjs y su variante de zoom; no existe gitlab-connector.spec.mjs ni un spec del catalogo).",
              "gravedad": "media"
            },
            {
              "literal": "El desempate \", id DESC\" dentro de \"ORDER BY started_at DESC, id DESC LIMIT 1\" en PostgresIssueImportReceiptStore.java:162.",
              "queDecide": "Cual de dos recibos con el mismo started_at se publica como «la ultima importacion», que es lo que alimenta lastActivityAt de la pantalla y de la fila del catalogo (ReadGitlabConnection.java:77-82).",
              "tieneOraculo": false,
              "prueba": "",
              "consecuenciaSiCambia": "El ORDER BY started_at DESC si esta cubierto: GithubConnectorPersistenceTest.java:530-541 (s10) crea dos recibos con instantes distintos y exige el mas nuevo, asi que un ASC moriria ahi. El desempate no: ninguna prueba crea dos recibos del mismo propietario y origen con started_at identico —el s15 de GitlabConnectorPersistenceTest.java:300-317 usa un recibo por origen—, asi que cambiar id DESC por id ASC, o quitarlo, no pone roja ninguna prueba y el orden pasaria a ser el que le apetezca a Postgres. Impacto acotado: solo se ve mal el instante de ultima actividad.",
              "gravedad": "baja"
            },
            {
              "literal": "El reparto de la frontera entre \"... AND status='running' AND started_at < ?\" (interruptAbandoned, PostgresIssueImportReceiptStore.java:84) y \"... AND status='running' AND started_at >= ?\" (importing, linea 178).",
              "queDecide": "Si un recibo que empezo exactamente en el instante limite de caducidad cuenta como importacion en curso —y por tanto bloquea la siguiente con IMPORT_IN_PROGRESS— o se da por abandonado y se cierra como failed/INTERRUPTED.",
              "tieneOraculo": false,
              "prueba": "",
              "consecuenciaSiCambia": "El literal 'running' si tiene oraculo (GitlabConnectorPersistenceTest.java:273-283, s27). La frontera no: s28 (misma clase, linea 286-297) usa un recibo de 16 minutos y lo consulta con ventanas de 15 y 17, nunca con la de 16 exactos. Cambiar >= por > (o < por <=) deja los dos comparadores solapados o con un hueco, y un recibo justo en el limite quedaria a la vez interrumpido y contado como en curso, o ni lo uno ni lo otro. Y como los comparadores viven dentro de una cadena SQL, PIT tampoco los muta: la puntuacion de esta clase no dice nada sobre ellos.",
              "gravedad": "baja"
            },
            {
              "literal": "PROVIDER = \"github\" en PostgresConnectorConnectionStore.java:17, usado en el WHERE de find (:34), delete (:72) e invalidate (:83) y en el INSERT de save (:59).",
              "queDecide": "Que fila de connector_connections se lee, se borra y se invalida.",
              "tieneOraculo": true,
              "prueba": "backend/src/test/java/com/apptolast/organization/adapter/persistence/GithubConnectorPersistenceTest.java:216 (s9, guardar dos veces sustituye la unica fila) apoyado en el CHECK (provider = 'github') del esquema, backend/src/main/resources/db/migration/V25__github_connector.sql:9.",
              "consecuenciaSiCambia": "Aqui la lectura sale limpia y conviene dejarlo dicho: si alguien cambiara la constante, el INSERT chocaria contra el CHECK del esquema y todas las pruebas de guardado se pondrian rojas antes de que el WHERE silencioso pudiera hacer dano. La misma constante gobierna escritura y lectura, y el esquema la ata. No hay punto ciego.",
              "gravedad": "baja"
            },
            {
              "literal": "Los dos ISSUES_QUERY como cadena completa: \"state=opened&per_page=100\" (GitlabApiBase.java:113) y \"state=open&per_page=100&sort=created&direction=desc\" (GithubApiBase.java:19). Ojo al detalle: GitLab dice «opened» y GitHub dice «open».",
              "queDecide": "Que issues pide el adaptador: solo abiertas, de cien en cien.",
              "tieneOraculo": true,
              "prueba": "backend/src/test/java/com/apptolast/organization/adapter/connectors/HttpGitlabIssueSourceTest.java:147 y backend/src/test/java/com/apptolast/organization/adapter/connectors/HttpGithubIssueSourceTest.java:105 afirman la query recibida caracter a caracter; ademas GithubApiBaseTest.java:63 y GitlabApiBaseTest.java:76 fijan la URL montada.",
              "consecuenciaSiCambia": "Este es el literal que mas asustaba —cambiar «opened» por «all» importaria como tareas todas las issues cerradas del proyecto— y resulta que si esta atado, por el lado bueno: la prueba compara la cadena entera, no un contains. Lo mismo con las cabeceras (PRIVATE-TOKEN en HttpGitlabIssueSourceTest.java:79, y las cuatro de GitHub en HttpGithubIssueSourceTest.java:75-79) y con X-Next-Page / rel=\"next\", que tienen prueba positiva y negativa. La familia de literales HTTP de los dos adaptadores esta bien cubierta; el unico hueco es el ratelimit-reset de GitLab.",
              "gravedad": "baja"
            }
          ]
        },
        "falsos": [],
        "omitidos": [
          "FOR UPDATE en backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresImportedTaskCommit.java:56 (\"SELECT status FROM projects WHERE owner_id=? AND id=? FOR UPDATE\"). Es el unico cerrojo que impide que un proyecto completado a mitad de importacion siga recibiendo tareas: se lee el estado, se decide con el (operation.apply(states.getFirst())) y se inserta tarea, evento y enlace en la misma transaccion. NO HAY prueba que lo distinga: el unico test con hilos de la 27/29 es GithubConnectorPersistenceTest.java:401-440 (s25_twoRealSimultaneousImportsLeaveExactlyOneReceiptRunning), que corre ocho begin() de recibo, no el commit; GitlabConnectorPersistenceTest no tiene ni Executor ni Thread ni CountDownLatch (grep vacio). Un grep de \"FOR UPDATE\" en backend/src/test devuelve AvailabilityApiTest, AppearancePersistenceTest, OutboxRecoveryTest, ScheduleBlockPersistenceTest, WebhookWorkPersistenceTest, WorkSession*: ninguna de las clases de conectores. Borrar \" FOR UPDATE\" de esa cadena deja verde toda la suite y PIT no lo muta porque vive dentro del literal.",
          "La ruta \"/api/v1/me/connectors/gitlab\": GitlabConnectorController.java:38 (CONNECTION) y frontend/src/gitlab-connector-client.ts:5 (CONNECTION_URL) son dos literales sueltos, igual que \"/api/v1/me/connectors\" en ConnectorCatalogController.java:27 frente a connectors-catalog-client.ts:4 (CATALOG_URL). NO HAY prueba que cruce la frontera: e2e/ no menciona gitlab en ningun spec (los unicos aciertos de grep son la palabra «conectores» en prosa castellana de github-connector.spec.mjs:132 y support/connector.mjs:87), y no existe spec del catalogo. Renombrar la ruta en un lado deja verde el backend (MockMvc con su propia constante) y el frontend (msw con la suya), y en produccion la pantalla da 404. Es el mismo agujero del hallazgo 5 pero sobre la ruta, no sobre los codigos.",
          "Las ramas por codigo de recibo de GitlabConnectorController.java:246-256 (statusOf: case \"CONNECTION_INVALID\", \"PROJECT_COMPLETED\" -> 409, default 503; titleOf: case \"STORAGE_UNAVAILABLE\" -> «El almacenamiento no esta disponible...»). NO HAY prueba de GitLab para PROJECT_COMPLETED ni para STORAGE_UNAVAILABLE por esta via: el unico test de IssueImportFailedException del conector es GitlabConnectorApiTest.java:360-387 y usa errorCode CONNECTION_INVALID; un grep de PROJECT_COMPLETED en backend/src/test no devuelve ningun fichero de GitLab (si de GitHub, GithubConnectorApiTest y ImportGithubIssuesTest). Ojo al matiz que evita inflar esto: el PROJECT_COMPLETED de precondicion si esta cubierto, pero por otro camino —ImportGitlabIssuesTest.java:328 lanza ProjectCompletedException y la traduce el handler global de ApiErrors.java:272—; lo que no tiene oraculo es el codigo que viaja dentro del recibo cuando el proyecto se completa a mitad de importacion (ImportIssues.java:111-113). Sacar \"PROJECT_COMPLETED\" del case de statusOf convierte ese 409 en 503 sin que nada se ponga rojo.",
          "El par de listas de orden del catalogo: ConnectorCatalogWiringTest.java:29 fija «api_credentials, webhooks, ics_calendar, github, external_calendar, gitlab» en el backend y frontend/src/connectors-catalog-client.ts:14-21 (CONNECTOR_ORDER) lo fija otra vez en el cliente, que ademas RECHAZA el catalogo entero si no llega en ese orden (INCOMPATIBLE, linea 7). Cada lado tiene oraculo propio, pero nada ata los dos: reordenar en el backend junto con su test de wiring deja las dos suites verdes y la pantalla /conectores se queda en blanco —no media lista, ninguna—. Gravedad baja por probabilidad, alta por radio de dano.",
          "GREATEST(?, started_at) dentro del UPDATE de finish(), en backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresIssueImportReceiptStore.java:137. Es el literal SQL mas cargado de la clase y no esta en la lista. Decide que ocurre si el reloj retrocede entre begin() y finish(): con GREATEST el recibo se cierra pegado a su propio inicio; sin el, el UPDATE choca contra el CHECK issue_import_receipts_end_after_start (backend/src/main/resources/db/migration/V25__github_connector.sql:35-36) y el propietario recibe un 503 STORAGE_UNAVAILABLE en vez de su recibo. NO HAY prueba: los unicos finish() con instante retrasado son GithubConnectorPersistenceTest.java:536 (empieza en NOW-100, cierra en NOW-99) y GitlabConnectorPersistenceTest.java:309 y :312, y los tres cierran despues de su propio started_at. Nadie llama a finish() con finishedAt anterior a startedAt, asi que sustituir GREATEST(?, started_at) por un simple ? deja toda la suite verde. Agravante: el propio record del dominio rechaza esa combinacion (backend/src/main/java/com/apptolast/organization/domain/IssueImportReceipt.java:44-45, 'An import cannot finish before it starts'), de modo que el clamp es lo unico que separa un reloj con salto NTP de una excepcion, y PIT no lo muta porque vive dentro de una cadena.",
          "connector={} y los cuatro outcome= de backend/src/main/java/com/apptolast/organization/adapter/logging/Slf4jConnectorAudit.java:19, :30, :49 y :71 (outcome=connected, outcome=connection_refused, outcome=import_finished, outcome=import_failed). Son las dos claves que distinguen una linea de bitacora de otra y, en una feature con DOS gestores, connector= es lo unico que dice si el fallo fue de GitHub o de GitLab. NO HAY prueba de ninguna de las dos: ConnectorAuditTest.java:51-89 afirma owner=, project=, account=, code=, providerStatus=, importId=, created=, skipped=, failed= y truncated=, pero jamas connector= ni outcome=. Grep de 'connector=' y 'outcome=' en backend/src/test y e2e: solo salen en PublicationAuditTest.java:30 y :52, que es de otra feature. Intercambiar outcome=import_finished por outcome=import_failed, o borrar connector= entero, no pone roja ninguna prueba, y el contrato @s34 (features/github_connector.feature:441) solo exige que la linea 'contenga propietario, repositorio, codigo HTTP y contadores' —justo lo unico que si esta afirmado.",
          "CONNECTOR_KEY_MISMATCH ausente del mapa MESSAGES de frontend/src/github-connector.tsx:37-48. El cazador lo detecta para GitLab (dentro de su hallazgo 2) pero no ve que el lado de GitHub, que SI tiene el manejador (GithubConnectorController.java:183-189) y SI tiene prueba de backend (GithubConnectorApiTest.java:709-720, jsonPath $.code = CONNECTOR_KEY_MISMATCH), muere igualmente en el frontend: importMessage() (github-connector.tsx:50-55) no encuentra la clave y devuelve el generico 'No se pudo importar. Intentalo mas tarde'. O sea que la unica pantalla que tiene el 503 honesto lo traduce mal. NO HAY prueba: grep de CONNECTOR_KEY_MISMATCH en frontend/ y e2e/ no devuelve absolutamente nada. Es el ejemplo mas puro de lo que se pedia buscar: el codigo que produce el backend y el que decodifica el frontend son dos literales sueltos, y aqui uno de los dos ni siquiera existe.",
          "El valor con el que se cierra el recibo abandonado en interruptAbandoned (PostgresIssueImportReceiptStore.java:83-88): finished_at recibe el startedAt del recibo NUEVO, mientras que el doble en memoria escribe staleBefore (ConnectorFakes.java:259, row.close(\"failed\", \"INTERRUPTED\", false, staleBefore)). Dos valores distintos para el mismo campo, y nada obliga a que coincidan porque la unica afirmacion es un isNotNull(): GithubConnectorPersistenceTest.java:479 y ImportGithubIssuesTest.java:415 comprueban el errorCode INTERRUPTED pero no el instante, y el contrato se conforma con 'finishedAt no nulo' (features/github_connector.feature:334 y features/additional_connectors.feature:352). Consecuencia: el instante en que consta que murio una importacion abandonada es hoy una decision de la que ninguna prueba se entera, y las pruebas unitarias que corren contra el doble estan midiendo otra cosa que la que hace el SQL.",
          "La rama por defecto de describeError() en frontend/src/connectors-catalog.tsx:67 no tiene prueba propia. El cazador la usa como respaldo de su hallazgo 1, pero no dice que el propio respaldo esta sin oraculo: ninguna prueba del catalogo pinta jamas un codigo desconocido. Grep de 'Hay un problema con esta integracion' en frontend/src, e2e y features: una sola aparicion, la de la propia linea 67. Si alguien borrara el ?? y dejara que el mapa devolviera undefined —que es exactamente lo que pasa hoy con GITLAB_UNAVAILABLE—, la fila se quedaria con un parrafo vacio y la suite seguiria verde.",
          "Asimetria de cuota entre los dos adaptadores en la respuesta 200: HttpGitlabIssueSource.java:67 llama a quotaExhausted(response) sobre una respuesta correcta y convierte un 200 con ratelimit-remaining: 0 en RATE_LIMITED (HttpGitlabIssueSource.java:172-174), mientras que HttpGithubIssueSource.list() (linea 64-75) no hace esa comprobacion en ningun momento: en GitHub la cuota solo se mira dentro de classify(), o sea unicamente cuando el estado ya NO es 200 (HttpGithubIssueSource.java:139-151). NO HAY prueba que fije esa diferencia por ninguno de los dos lados: la unica fila que la ejercita es la de GitLab (HttpGitlabIssueSourceTest.java:208-216, 200 con RateLimit-Remaining 0 y Retry-After 5), y en HttpGithubIssueSourceTest.java el x-ratelimit-remaining solo aparece acompanando a un 403 (linea 214, :232, :261). Nada dice si un 200 de GitHub con la cuota a cero debe cortar la importacion o seguir; hoy sigue, y esa decision no esta escrita ni verificada en ninguna parte."
        ]
      }
    ],
    "sintesis": "# Punto ciego de literales — informe para el orquestador\n\nVerificado por lectura sobre `main` (solo lectura, sin campañas). Cifras de mutación recalculadas por clase desde los `mutations.xml` que ya están en disco (`backend/build/reports/pitest-*/mutations.xml`), no declaradas.\n\n---\n\n## 1. Literales sin oráculo que sobreviven a la refutación\n\n### GRAVEDAD ALTA\n\n**1. `ERROR_TEXT` del catálogo no conoce 4 de los códigos que el backend escribe — DEFECTO VIVO, no hipótesis**\n`frontend/src/connectors-catalog.tsx:50-59` mapea `CONNECTION_INVALID`, `DELIVERY_EXHAUSTED` y los siete `FEED_*`. `backend/src/main/java/com/apptolast/organization/application/ConnectorFailures.java:52-66` puede escribir en `last_error_code` cuatro que **no están en el mapa**: `GITHUB_REPOSITORY_UNAVAILABLE`, `GITHUB_UNAVAILABLE`, `GITLAB_UNAVAILABLE`, `RATE_LIMITED`.\n- *Usuario*: hoy mismo, tras un 503 de GitLab o una cuota agotada, la fila de `/conectores` dice «Conectado» y debajo «Hay un problema con esta integración» (`connectors-catalog.tsx:67`), que no traduce nada.\n- *No hay prueba*: las tres únicas del catálogo que pintan `lastError` usan siempre `CONNECTION_INVALID` (`frontend/src/connectors-catalog.test.tsx:94, :128, :158`). `grep RATE_LIMITED frontend/src/connectors-catalog*` → vacío.\n- *Cierre*: una prueba parametrizada del catálogo sobre los **siete** códigos que `ConnectorFailures` sabe emitir, más una prueba de backend que afirme el conjunto emitido (para que añadir uno rompa el frontend). Esto no es deuda de test: es un arreglo de producto.\n\n**2. `AND d.next_attempt_at <= ?` — `backend/.../adapter/persistence/PostgresWebhookWork.java:64`**\nEs la única compuerta del backoff. **No hay prueba**: ninguna entrega nace con `nextAttemptAt` futuro; `WebhookRecoveryPersistenceTest.java:228` solo pone `next_attempt_at=NULL`, `WebhookWorkPersistenceTest` nace todo en `T`.\n- *Usuario*: los 6 intentos se queman en un solo tick contra el receptor caído y el endpoint acaba `disabled/DELIVERY_EXHAUSTED` en segundos.\n- *Agravante confirmado*: `d.status = 'pending'` (línea 63) queda **tapado** por esta cláusula (los terminales tienen `next_attempt_at` NULL, y `NULL <= ?` es falso). Las dos se cubren mutuamente; ninguna está sujeta por sí sola.\n- *Cierre*: un caso que grabe un fallo, compruebe que `claimNext` devuelve vacío en `T`, y que sí reclama en `T + backoff`.\n\n**3. La mitad `event_id` del cursor de la outbox — `PostgresWebhookOutbox.java:64` (`(occurred_at, event_id) > (?, ?)`) y `:66` (`ORDER BY occurred_at, event_id`)**\nSon las dos mitades de la misma garantía y **ninguna prueba de persistencia mete dos eventos en el mismo instante**: `WebhookOutboxPersistenceTest` usa T+1/T+2/T+600, `WebhookRecoveryPersistenceTest.java:208-220` usa T+1/T+2/T+3. El único caso con empate (`EnqueueWebhookDeliveriesTest.java:167-178`) corre contra `FakeOutbox`, que reimplementa el predicado en Java.\n- *Usuario*: pérdida **silenciosa y permanente** de un evento — justo lo que `features/webhooks.feature:258` prohíbe.\n- *Cierre*: un caso de persistencia con dos eventos de idéntico `occurred_at` y `event_id` A<B, que exija que se encola A primero y B en el ciclo siguiente.\n\n**4. El contrato de orden de UUID entre dominio y PostgreSQL — `backend/.../domain/WebhookCursor.java:24-29`**\n`compareUnsigned` existe **solo** porque `UUID.compareTo` de Java tiene signo y Postgres ordena bytes sin signo. **No existe `WebhookCursorTest`** (confirmado: no hay ningún fichero de cursor en `backend/src/test/java/com/apptolast/organization/domain/`), y los dos usos (`EnqueueWebhookDeliveriesTest.java:55`, `ExecuteAutomationsTest.java:628`) le pasan el comparador de producción a un doble y luego afirman que el doble quedó ordenado: tautología.\n- *Usuario*: el defecto que este código dice prevenir («la mitad de los ids del primer instante nunca se entregarían») es indetectable por la suite.\n- *Cierre*: dos filas de `outbox_events` con el mismo `occurred_at` y `event_id` a ambos lados del bit de signo (uno con MSB puesto), y afirmar que el orden del adaptador coincide con `precedes()`.\n\n**5. `ORDER BY occurred_at, event_id LIMIT ?` — `PostgresAutomationWork.java:270-274` (helper `window`)**\n**No hay prueba**: `AutomationExecutionTest` nunca tiene más de una fila candidata por llamada; @s17 solo se comprueba contra `FakeWork.after()`, que ordena en Java (`ExecuteAutomationsTest.java:620-628`).\n- *Usuario*: con DESC o sin ORDER BY el cursor salta al evento más reciente y **todas las automatizaciones anteriores no disparan jamás**, sin error.\n- *Cierre*: un caso de integración con ≥3 eventos por propietario que exija el orden de disparo y la reanudación en el ciclo siguiente (cierra también el `LIMIT ? / BATCH=200`).\n\n**6. `AND status = 'retry'` en el UPDATE de reintento — `PostgresAutomationWork.java:~192`**\nLa escalera 1→2→3→failed de @s22 solo se prueba contra el doble (`ExecuteAutomationsTest.java:350, :377`); **ninguna prueba de integración deja una fila en `'retry'` antes de un ciclo**.\n- *Usuario*: `affected == 0` → `AutomationClaimedException` → `ExecuteAutomations.java:89` lo lee como «otro worker ganó» y la ejecución se queda clavada en attempt 2 **para siempre**, sin error visible.\n- *Cierre*: sembrar una fila `retry` en Postgres y correr un ciclo, afirmando attempt 2 y luego `failed` en el tercero.\n\n**7. `ON CONFLICT (rule_id, event_id) DO UPDATE SET ...` de `upsert()` — `PostgresAutomationWork.java:~205-211`**\nEs LA fila que sobrevive al rollback (por eso `apart` usa `PROPAGATION_REQUIRES_NEW`). **Confirmado por grep**: `.record(` contra la base solo aparece en `WebhookWorkPersistenceTest` y `WebhookRecoveryPersistenceTest`; **cero llamadas en `AutomationExecutionTest`** — s25 provoca la caída llamando `work.commit()` a pelo, sin pasar por el caso de uso.\n- *Usuario*: degradado a `DO NOTHING`, `attemptOf` relee siempre attempt 1 en `retry` y el evento se reintenta eternamente sin llegar a `failed`.\n- *Cierre*: un ciclo que falle de verdad dentro de `ExecuteAutomations` y afirme la fila `retry` con `STORAGE_UNAVAILABLE` en Postgres, y que el segundo ciclo renueve esa misma fila.\n\n**8. `\"blocked\".equals(row.getString(\"status\"))` — `PostgresAutomationWork.java:299`**\nLa bandera se fabrica a mano en el doble (`ExecuteAutomationsTest.java:532`); ninguna fila `blocked` real pasa por este adaptador (la de `AutomationPersistenceTest.java:315` prueba `PostgresAutomationEvents.recent()`, otra consulta).\n- *Usuario*: se crean tareas y se encolan webhooks a partir de eventos que la outbox retuvo a propósito.\n- *Cierre*: insertar una fila `blocked` en `outbox_events` y afirmar que el ciclo avanza el cursor sin producir nada.\n\n**9. `AND o.event_id = ?` en el `INSERT ... SELECT` de `queue()` — `PostgresAutomationWork.java:167`**\nConfirmado: es un producto cartesiano `FROM webhook_endpoints e, outbox_events o`. El único escenario que llega ahí (s26) tiene **exactamente una** fila de outbox (`AutomationExecutionTest.java:157`), así que sin ese join el resultado es idéntico.\n- *Usuario*: con N eventos en la outbox, cada regla disparada encola **N entregas**, cada una con el cuerpo de un evento ajeno.\n- *Cierre*: s26 con dos eventos en la outbox del propietario y una sola entrega esperada, con el `body` del evento correcto. Mismo caso cierra `e.status='active'` y `e.owner_id=?`.\n\n**10. Cinco de los siete `FeedError` nunca tocan el `CHECK` real — `backend/src/main/resources/db/migration/V26__external_calendar.sql:17-19`**\nConfirmado por grep: `FEED_MALFORMED` y `SECRET_UNREADABLE` aparecen **0 veces** como cadena en `backend/src/test` y `e2e`; `FEED_REJECTED`, `FEED_TOO_LARGE` y `FEED_UNSUPPORTED_TYPE`, una vez cada uno y no contra Postgres. Solo `FEED_HTTP_ERROR` está atado de punta a punta (`e2e/external-calendar-ux-audit.spec.mjs:123-151`, INSERT real + alerta real).\n- *Usuario*: añadir o renombrar un código compila, pasa la suite y pasa la campaña; en producción el primer feed que falle por esa causa hace violar el CHECK → **500** en vez de 200 con `lastError`. Y en el frontend, `external-calendar-api.ts:49-57` lo rechaza y **tumba la suscripción entera**, no el mensaje.\n- *Cierre*: un `@ParameterizedTest` sobre `FeedError.values()` que ejecute `commitFailure` contra Postgres con cada código. Barato y cierra los cinco de golpe.\n\n### GRAVEDAD MEDIA\n\n| # | Literal | Qué decide / qué le pasa al usuario | Prueba que lo cierra |\n|---|---|---|---|\n| 11 | `FOR UPDATE` en `PostgresImportedTaskCommit.java:56` (confirmado) | Único cerrojo que impide que un proyecto completado a mitad de importación siga recibiendo tareas. Ningún test de conectores tiene hilos (`GitlabConnectorPersistenceTest`: grep de Thread/Latch vacío) | Dos transacciones: una completa el proyecto mientras la otra importa; exigir que no se cree la tarea |\n| 12 | `GREATEST(?, started_at)` en `PostgresIssueImportReceiptStore.java:137` (confirmado) | Con un salto NTP hacia atrás, sin él el UPDATE viola `issue_import_receipts_end_after_start` (V25:35-36) y el propietario recibe 503 en vez de su recibo. Nadie llama `finish()` con `finishedAt < startedAt` | `finish()` con instante anterior al inicio, afirmando `finishedAt == startedAt` |\n| 13 | `\"ratelimit-reset\"` en `HttpGitlabIssueSource.java:179` (confirmado: 0 apariciones en tests/e2e/features) | Rama muerta si alguien copia el `x-ratelimit-reset` de GitHub; todo 429 sin `Retry-After` daría 60 s fijos | Fila nueva en la tabla de @s24 (`features/additional_connectors.feature:295-298`): 429 con `RateLimit-Reset` y sin `Retry-After` |\n| 14 | Desempate `(executed_at = ? AND id < ?)` + `ORDER BY executed_at DESC, id DESC` — `PostgresAutomationRuns.java:52, :57-58` | Todas las ejecuciones de un ciclo comparten sello de reloj (`ExecuteAutomations.java:225`), así que el empate es la norma: historial con huecos o filas repetidas al paginar. `AutomationPersistenceTest.java:271-273` usa tres instantes distintos | Dos ejecuciones con `executed_at` idéntico y paginación de 1 en 1 |\n| 15 | `ORDER BY occurred_at DESC, event_id DESC` — `PostgresAutomationEvents.java:131` | El desempate que exige `features/automations.feature:397` y el corte de la ventana de 100. `AutomationPersistenceTest.java:320-323` usa cuatro instantes distintos | Dos eventos del mismo instante en la simulación |\n| 16 | `ORDER BY d.next_attempt_at, d.id` — `PostgresWebhookWork.java:67` | Con DESC: inanición de las entregas antiguas bajo cola sostenida. `WebhookWorkPersistenceTest.java:211` usa ese orden en *su propia* consulta; su aserción (`assertNotEquals`, :247) sigue verde invertido | Tres entregas vencidas con `next_attempt_at` escalonados y afirmar la identidad de la reclamada |\n| 17 | `ON CONFLICT (owner_id) DO NOTHING` — `PostgresAutomationWork.java:88-89` | Nunca se ejecuta contra Postgres (`AutomationExecutionTest.java:252-257` siembra siempre el cursor). Con `DO UPDATE`, dos workers rebobinan el cursor y se reprocesa la ventana entera | Dos `startCursor` concurrentes sobre un propietario sin fila |\n| 18 | `CHECK (char_length(label) BETWEEN 1 AND 40)` (V26:7) y `CHECK (char_length(summary) <= 500)` (V26:34) frente a `ExternalCalendarInput.LABEL_LIMIT` y `IcsFeed.SUMMARY_LIMIT=115` | Ninguna prueba mete jamás el valor de frontera en un Postgres real (`ExternalCalendarPersistenceTest` usa 'Trabajo'/'Casa'). Subir el límite de dominio sin tocar el CHECK → 500 en el PUT / en la sincronización | Un PUT con etiqueta de 40 puntos de código y una sincronización con resumen de 500, contra la base real |\n| 19 | Vocabularios cerrados del frontend que **lanzan** en vez de degradar: `webhooks-client.ts:20-29`, `external-calendar-api.ts:49-57`, `automations-api.ts:4-17` (12 tipos) y `:70`, `connectors-catalog-client.ts:14-21` (orden) | Un valor nuevo del backend no degrada una fila: tumba la respuesta entera (`throw` en `webhooks-client.ts:144`, `automations-api.ts:168`) o la pantalla de conectores completa | Un solo test de contrato por lista: fixture generado desde el enum/CHECK del backend, no escrito a mano en el propio spec |\n| 20 | `CONNECTOR_KEY_MISMATCH`: falta el `@ExceptionHandler(SecretUndecipherableException)` en `GitlabConnectorController.java:170-220` (el gemelo sí lo tiene, `GithubConnectorController.java:183-189`) y falta la clave en `github-connector.tsx:37-48` | Con clave rotada, GitLab da 500 genérico donde GitHub da 503 honesto; y en GitHub, el único 503 honesto se traduce con el mensaje genérico. `grep CONNECTOR_KEY_MISMATCH frontend/ e2e/` → nada | Prueba de API de GitLab con secreto indescifrable + entrada en el mapa del frontend con su test |\n| 21 | Ventana 24 h / 336 h duplicada: `frontend/src/external-calendar.tsx:36-37` vs `ExternalCalendarSnapshot` | Estrecharla en el backend deja **una cola de días silenciosamente vacía**, sin 400 ni error visible. Cada extremo clavado por separado, la relación no la afirma nadie | Que el frontend derive la ventana de un valor servido, o un e2e que siembre un evento en el borde de la ventana |\n\n### Refutados / degradados — **no volver a levantarlos**\n- **`FOR UPDATE OF d` (`PostgresWebhookWork.java:68`)**: el oráculo **existe** (`WebhookWorkPersistenceTest.java:169-171`, «both workers made progress»), pero es **probabilístico**. Deuda real: *prueba intermitente*, no prueba ciega. Igual para el `LIMIT 1` de `:69`.\n- **`CHECK url_host 253` (V26:9)**: inalcanzable desde la API — `SaveExternalCalendar.java:33` corta antes con `UNRESOLVABLE_HOST` (`SaveExternalCalendarTest.java:203-213`). Gravedad baja.\n- **`COLLATE \"C\"` (`PostgresExternalCalendarStore.java:178`)**: hoy es un no-op (`postgres:17.11-alpine`, musl, inicializa en C). Riesgo diferido a quien provisione con `en_US`. Baja.\n- **`succeeded`/`retry`/`failed` de automatizaciones**: las pruebas escriben el literal a mano (`ExecuteAutomationsTest.java:129, :250, :350`; `AutomationExecutionTest.java:69, :121`) **y** el CHECK de V28:24 revienta. Solo falta el contrato cruzado; el escenario «el backend seguiría verde» es falso.\n- **`FEED_HTTP_ERROR`**: sí está atado de punta a punta. Son **seis de siete**, no siete.\n- **`JdkWebhookSender`**: verificado y limpio — todos sus literales atados en `JdkWebhookSenderTest` (cabeceras :258-262, firma :285-288, códigos de transporte :336-518).\n\n---\n\n## 2. Los de ORDEN o LÍMITE en SQL — la familia peor\n\nOcho, y son los que «pasan con pocos datos y fallan en producción». Todos comparten la misma firma de fallo: **ninguna prueba crea el empate o el volumen que hace decidir al literal**.\n\n| Literal | Fichero:línea | Por qué la prueba no lo ve |\n|---|---|---|\n| `ORDER BY occurred_at, event_id` (mitad `event_id`) | `PostgresWebhookOutbox.java:66` | no hay dos eventos del mismo instante |\n| `(occurred_at, event_id) > (?, ?)` (forma de tupla) | `PostgresWebhookOutbox.java:64` | ídem |\n| `ORDER BY occurred_at, event_id LIMIT ?` | `PostgresAutomationWork.java:270-274` | nunca más de un candidato por ciclo |\n| `ORDER BY d.next_attempt_at, d.id` | `PostgresWebhookWork.java:67` | nunca dos reclamables a la vez |\n| `ORDER BY executed_at DESC, id DESC` + `id < ?` | `PostgresAutomationRuns.java:52, :57-58` | tres `executed_at` distintos |\n| `ORDER BY occurred_at DESC, event_id DESC` | `PostgresAutomationEvents.java:131` | cuatro instantes distintos |\n| `, id DESC` de la página y de la poda | `PostgresWebhookStore.java:131`, `PostgresWebhookWork.java:131` | las empatadas caen fuera del `subList(0,50)` comparado |\n| Orden implícito del truncado de GitLab: `ISSUES_QUERY = \"state=opened&per_page=100\"` **sin `order_by`** | `GitlabApiBase.java:113` (el gemelo sí lo fija: `GithubApiBase.java:19`) | @s16 usa 2/37/140 issues; con 250 nadie sabe qué 50 se pierden |\n\nLos tres primeros y el `PostgresAutomationWork` **no son cosméticos**: cortan pérdida permanente de eventos, no orden de presentación.\n\n---\n\n## 3. Cuántos están en clases con campaña por encima del 90 %\n\nRecuento por clase desde los `mutations.xml` en disco:\n\n| Clase | Muertos/total | % | Hallazgos ciegos que contiene |\n|---|---|---|---|\n| `PostgresAutomationEvents` | 18/18 | **100 %** | #15 (desempate `event_id DESC`) |\n| `PostgresAutomationRuns` | 9/9 | **100 %** | #14 (paginación por `id`) |\n| `PostgresAutomationStore` | 27/27 | **100 %** | — |\n| `PostgresExternalCalendarStore` | 37/39 | **94,9 %** | `COLLATE \"C\"` (degradado) |\n| `HttpGithubIssueSource` | 45/48 | **93,8 %** | asimetría del 200 con cuota agotada |\n| `PostgresWebhookOutbox` | 13/14 | **92,9 %** | **#3 (las dos mitades del cursor)** |\n| `PostgresAutomationWork` | 30/35 | 85,7 % | #5, #6, #7, #8, #9, #17 |\n| `PostgresWebhookStore` | 24/27 | 88,9 % | `hasPendingPing`, desempates |\n| `PostgresIssueImportReceiptStore` | 21/24 | 87,5 % | #12 |\n| `PostgresImportedTaskCommit` | 13/16 | 81,2 % | #11 |\n| `PostgresWebhookWork` | 11/14 | 78,6 % | **#2**, #16 |\n| `WebhookCursor` | 8/11 | 72,7 % | #4 |\n\n**Cuenta directa**: 4 de los hallazgos (#3, #14, #15, más el de GitHub) viven en clases con **>90 %**, y dos de ellas están al **100 % con cero supervivientes**. Y los cuatro *features* completos están por encima del umbral: 96,00 / 95,72 medido, 91,71 y 89,88.\n\nPero el dato que más pesa no es el porcentaje sino el **denominador**: `PostgresWebhookWork` recibe **14 mutantes** para siete sentencias SQL; `PostgresAutomationWork`, **35**. `PostgresAutomationRuns` tiene 9 mutantes y su paginación entera es una cadena. Los ficheros `V23__webhooks.sql`, `V26__external_calendar.sql` y `V28__automations.sql` reciben **cero**: no son bytecode. La campaña no está fallando en matar mutantes de estas clases — es que **no los genera**.\n\n---\n\n## 4. La frase honesta\n\nLas puntuaciones **no están infladas ni son falsas: están acotadas**. Miden fielmente lo que PIT muta —control de flujo, condiciones, retornos, aritmética en bytecode Java— y en ese dominio el backend está genuinamente sano; lo que hay que dejar de hacer es leerlas como cobertura de conducta, porque la conducta que vive dentro de una cadena SQL, de un CHECK de migración o de un código emparejado entre dos procesos tiene puntuación perfecta por construcción, no por mérito, y ahí es exactamente donde están los cuatro caminos de pérdida silenciosa y permanente de datos de este informe.\n\n**Acción mínima recomendada**: (a) arreglar `ERROR_TEXT` hoy — es un defecto vivo, no deuda de prueba; (b) tres pruebas de persistencia con **empate** (mismo `occurred_at`, mismo `executed_at`) cierran seis hallazgos de orden a la vez; (c) un `@ParameterizedTest` sobre `FeedError.values()` contra Postgres cierra cinco; (d) añadir a la puerta de cierre un requisito explícito de **lectura de literales** para toda clase `Postgres*` y `Http*`, con la lista de literales acreditados en el `progress/tdd_*` como ya hizo el artesano de webhooks."
  },
  "workflowProgress": [
    {
      "type": "workflow_phase",
      "index": 1,
      "title": "Caza"
    },
    {
      "type": "workflow_phase",
      "index": 2,
      "title": "Refutacion"
    },
    {
      "type": "workflow_phase",
      "index": 3,
      "title": "Sintesis"
    },
    {
      "type": "workflow_agent",
      "index": 1,
      "label": "caza:25-webhooks",
      "phaseIndex": 1,
      "phaseTitle": "Caza",
      "agentId": "aa99400e755adadab",
      "model": "claude-opus-5[1m]",
      "state": "done",
      "startedAt": 1789033000537,
      "queuedAt": 1789033000459,
      "attempt": 1,
      "lastToolName": "StructuredOutput",
      "lastToolSummary": "25, webhooks",
      "promptPreview": "Trabajas en C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb, rama main.\n\nERES DE SOLO LECTURA. No edites nada. NO ejecutes campanas de mutacion ni la\nsuite completa: hay dos campanas corriendo y tres carriles trabajando. Puedes\nleer todo y ejecutar como mucho UNA clase de test suelta.\n\nEL PUNTO CIEGO QUE VAS A CAZAR.\n\nEste proyecto cierra cada feature con una campana de mutacion por e…",
      "promptFramed": false,
      "lastProgressAt": 1789033365357,
      "tokens": 141422,
      "toolCalls": 36,
      "durationMs": 364806,
      "resultPreview": "{\"feature\":\"25, webhooks\",\"hallazgos\":[{\"literal\":\"`AND d.next_attempt_at <= ?` — C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb/backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresWebhookWork.java:64\",\"queDecide\":\"La compuerta del backoff: una entrega que fallo y quedo pending con nextAttemptAt futuro no debe reclamarse hasta que venza. Es lo unico que separ…"
    },
    {
      "type": "workflow_agent",
      "index": 2,
      "label": "caza:28-calendario",
      "phaseIndex": 1,
      "phaseTitle": "Caza",
      "agentId": "a27aa935f5245b81b",
      "model": "claude-opus-5[1m]",
      "state": "done",
      "startedAt": 1789033000547,
      "queuedAt": 1789033000460,
      "attempt": 1,
      "lastToolName": "StructuredOutput",
      "lastToolSummary": "28 — calendario externo",
      "promptPreview": "Trabajas en C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb, rama main.\n\nERES DE SOLO LECTURA. No edites nada. NO ejecutes campanas de mutacion ni la\nsuite completa: hay dos campanas corriendo y tres carriles trabajando. Puedes\nleer todo y ejecutar como mucho UNA clase de test suelta.\n\nEL PUNTO CIEGO QUE VAS A CAZAR.\n\nEste proyecto cierra cada feature con una campana de mutacion por e…",
      "promptFramed": false,
      "lastProgressAt": 1789033351070,
      "tokens": 149058,
      "toolCalls": 36,
      "durationMs": 349318,
      "resultPreview": "{\"feature\":\"28 — calendario externo\",\"hallazgos\":[{\"literal\":\"La lista de siete códigos repetida en el CHECK de SQL: `last_error TEXT CHECK (last_error IN ('FEED_REJECTED', 'FEED_UNREACHABLE', 'FEED_HTTP_ERROR', 'FEED_TOO_LARGE', 'FEED_UNSUPPORTED_TYPE', 'FEED_MALFORMED', 'SECRET_UNREADABLE'))` — backend/src/main/resources/db/migration/V26__external_calendar.sql:17-19. Es una copia independiente d…"
    },
    {
      "type": "workflow_agent",
      "index": 3,
      "label": "caza:30-automatizaciones",
      "phaseIndex": 1,
      "phaseTitle": "Caza",
      "agentId": "a32c82bee7ff31dc9",
      "model": "claude-opus-5[1m]",
      "state": "done",
      "startedAt": 1789033000548,
      "queuedAt": 1789033000461,
      "attempt": 1,
      "lastToolName": "StructuredOutput",
      "lastToolSummary": "30, automatizaciones",
      "promptPreview": "Trabajas en C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb, rama main.\n\nERES DE SOLO LECTURA. No edites nada. NO ejecutes campanas de mutacion ni la\nsuite completa: hay dos campanas corriendo y tres carriles trabajando. Puedes\nleer todo y ejecutar como mucho UNA clase de test suelta.\n\nEL PUNTO CIEGO QUE VAS A CAZAR.\n\nEste proyecto cierra cada feature con una campana de mutacion por e…",
      "promptFramed": false,
      "lastProgressAt": 1789033373147,
      "tokens": 130276,
      "toolCalls": 35,
      "durationMs": 371394,
      "resultPreview": "{\"feature\":\"30, automatizaciones\",\"hallazgos\":[{\"literal\":\"\\\"ORDER BY occurred_at, event_id LIMIT ?\\\" en PostgresAutomationWork.window(), backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresAutomationWork.java:274\",\"queDecide\":\"El orden en que el ejecutor recorre la outbox de cada propietario. El cursor solo avanza hacia delante: procesar en otro orden equivale a saltarse …"
    },
    {
      "type": "workflow_agent",
      "index": 4,
      "label": "caza:27-29-conectores",
      "phaseIndex": 1,
      "phaseTitle": "Caza",
      "agentId": "a92b0b72a758a0b85",
      "model": "claude-opus-5[1m]",
      "state": "done",
      "startedAt": 1789033000550,
      "queuedAt": 1789033000461,
      "attempt": 1,
      "lastToolName": "StructuredOutput",
      "lastToolSummary": "27 y 29 — conectores GitHub y GitLab (PostgresConnectorConn…",
      "promptPreview": "Trabajas en C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb, rama main.\n\nERES DE SOLO LECTURA. No edites nada. NO ejecutes campanas de mutacion ni la\nsuite completa: hay dos campanas corriendo y tres carriles trabajando. Puedes\nleer todo y ejecutar como mucho UNA clase de test suelta.\n\nEL PUNTO CIEGO QUE VAS A CAZAR.\n\nEste proyecto cierra cada feature con una campana de mutacion por e…",
      "promptFramed": false,
      "lastProgressAt": 1789033466302,
      "tokens": 170188,
      "toolCalls": 61,
      "durationMs": 464548,
      "resultPreview": "{\"feature\":\"27 y 29 — conectores GitHub y GitLab (PostgresConnectorConnectionStore, PostgresGitlabConnectionStore, HttpGithubIssueSource, HttpGitlabIssueSource, ConnectorError)\",\"hallazgos\":[{\"literal\":\"El mapa ERROR_TEXT de frontend/src/connectors-catalog.tsx:50-60 (con el respaldo de la linea 67, «Hay un problema con esta integracion») frente a los codigos que el backend sabe escribir en last_er…"
    },
    {
      "type": "workflow_agent",
      "index": 5,
      "label": "refuta:28-calendario:0",
      "phaseIndex": 2,
      "phaseTitle": "Refutacion",
      "agentId": "a220a7ccd91a35b9d",
      "model": "claude-opus-5[1m]",
      "state": "done",
      "startedAt": 1789033351121,
      "queuedAt": 1789033351078,
      "attempt": 1,
      "lastToolName": "StructuredOutput",
      "lastToolSummary": "Lei el codigo y las pruebas de los nueve hallazgos y solo u…",
      "promptPreview": "Trabajas en C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb, rama main.\n\nERES DE SOLO LECTURA. No edites nada. NO ejecutes campanas de mutacion ni la\nsuite completa: hay dos campanas corriendo y tres carriles trabajando. Puedes\nleer todo y ejecutar como mucho UNA clase de test suelta.\n\nEL PUNTO CIEGO QUE VAS A CAZAR.\n\nEste proyecto cierra cada feature con una campana de mutacion por e…",
      "promptFramed": false,
      "lastProgressAt": 1789033811928,
      "tokens": 135587,
      "toolCalls": 41,
      "durationMs": 460804,
      "resultPreview": "{\"falsos\":[\"FALSO POSITIVO (parcial) - Hallazgo 3, `CHECK (char_length(url_host) BETWEEN 1 AND 253)` en V26__external_calendar.sql:9. Es cierto que ningun test usa un host de mas de 253 caracteres, pero la consecuencia que afirma el cazador -«el usuario recibe un 500 en vez del 400»- esta refutada por una guardia que SI esta probada y que corre ANTES de cualquier INSERT: SaveExternalCalendar.java:…"
    },
    {
      "type": "workflow_agent",
      "index": 6,
      "label": "refuta:28-calendario:1",
      "phaseIndex": 2,
      "phaseTitle": "Refutacion",
      "agentId": "a14440d61d66d8307",
      "model": "claude-opus-5[1m]",
      "state": "done",
      "startedAt": 1789033351123,
      "queuedAt": 1789033351079,
      "attempt": 1,
      "lastToolName": "StructuredOutput",
      "lastToolSummary": "Comprobe los nueve hallazgos contra el codigo, linea a line…",
      "promptPreview": "Trabajas en C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb, rama main.\n\nERES DE SOLO LECTURA. No edites nada. NO ejecutes campanas de mutacion ni la\nsuite completa: hay dos campanas corriendo y tres carriles trabajando. Puedes\nleer todo y ejecutar como mucho UNA clase de test suelta.\n\nEL PUNTO CIEGO QUE VAS A CAZAR.\n\nEste proyecto cierra cada feature con una campana de mutacion por e…",
      "promptFramed": false,
      "lastProgressAt": 1789033801799,
      "tokens": 158708,
      "toolCalls": 38,
      "durationMs": 449221,
      "resultPreview": "{\"falsos\":[\"HALLAZGO 2 (lista blanca ERRORS del frontend, `tieneOraculo: false`) — PARCIALMENTE FALSO. La afirmacion «el frontend solo se prueba contra respuestas que el mismo fabrica» no es cierta: hay un oraculo de extremo a extremo que el cazador no vio, en e2e/external-calendar-ux-audit.spec.mjs. Ahi `seedSubscription({status: \\\"FAILED\\\", error: \\\"FEED_HTTP_ERROR\\\"})` (lineas 123-128) hace un …"
    },
    {
      "type": "workflow_agent",
      "index": 7,
      "label": "refuta:25-webhooks:0",
      "phaseIndex": 2,
      "phaseTitle": "Refutacion",
      "agentId": "ac08d75d7b835f174",
      "model": "claude-opus-5[1m]",
      "state": "done",
      "startedAt": 1789033365369,
      "queuedAt": 1789033365364,
      "attempt": 1,
      "lastToolName": "StructuredOutput",
      "lastToolSummary": "No refuto el trabajo del cazador. Verifique los trece halla…",
      "promptPreview": "Trabajas en C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb, rama main.\n\nERES DE SOLO LECTURA. No edites nada. NO ejecutes campanas de mutacion ni la\nsuite completa: hay dos campanas corriendo y tres carriles trabajando. Puedes\nleer todo y ejecutar como mucho UNA clase de test suelta.\n\nEL PUNTO CIEGO QUE VAS A CAZAR.\n\nEste proyecto cierra cada feature con una campana de mutacion por e…",
      "promptFramed": false,
      "lastProgressAt": 1789033664707,
      "tokens": 121767,
      "toolCalls": 21,
      "durationMs": 299332,
      "resultPreview": "{\"falsos\":[\"PARCIALMENTE INFLADO (unico reparo real): `FOR UPDATE OF d SKIP LOCKED`, la restriccion `OF d` — PostgresWebhookWork.java:68. El cazador dice que la prueba de concurrencia «no lo detecta» porque sus aserciones son «de cardinalidad total mas ambos avanzaron, que se cumplen igual si el perdedor se queda corto y el ganador drena el resto». Ese razonamiento es FALSO. La asercion que no vio…"
    },
    {
      "type": "workflow_agent",
      "index": 8,
      "label": "refuta:25-webhooks:1",
      "phaseIndex": 2,
      "phaseTitle": "Refutacion",
      "agentId": "a46ee328cc340bc3b",
      "model": "claude-opus-5[1m]",
      "state": "done",
      "startedAt": 1789033365374,
      "queuedAt": 1789033365364,
      "attempt": 1,
      "lastToolName": "StructuredOutput",
      "lastToolSummary": "Verifique los trece hallazgos uno a uno contra el codigo y …",
      "promptPreview": "Trabajas en C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb, rama main.\n\nERES DE SOLO LECTURA. No edites nada. NO ejecutes campanas de mutacion ni la\nsuite completa: hay dos campanas corriendo y tres carriles trabajando. Puedes\nleer todo y ejecutar como mucho UNA clase de test suelta.\n\nEL PUNTO CIEGO QUE VAS A CAZAR.\n\nEste proyecto cierra cada feature con una campana de mutacion por e…",
      "promptFramed": false,
      "lastProgressAt": 1789033795486,
      "tokens": 147249,
      "toolCalls": 35,
      "durationMs": 430108,
      "resultPreview": "{\"refutado\":false,\"porque\":\"Verifique los trece hallazgos uno a uno contra el codigo y NO encontre ningun falso positivo: los cinco que declara con oraculo lo tienen de verdad (los lei, no me fie de la puntuacion) y los ocho que declara sin oraculo, tampoco lo tienen. Comprobaciones concretas. (1) `AND d.next_attempt_at <= ?` (PostgresWebhookWork.java:64): grep sobre backend/src/test confirma que …"
    },
    {
      "type": "workflow_agent",
      "index": 9,
      "label": "refuta:30-automatizaciones:0",
      "phaseIndex": 2,
      "phaseTitle": "Refutacion",
      "agentId": "ad88e2e91bc135e42",
      "model": "claude-opus-5[1m]",
      "state": "done",
      "startedAt": 1789033373152,
      "queuedAt": 1789033373150,
      "attempt": 1,
      "lastToolName": "StructuredOutput",
      "lastToolSummary": "No queda refutado: doce de los catorce hallazgos resisten l…",
      "promptPreview": "Trabajas en C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb, rama main.\n\nERES DE SOLO LECTURA. No edites nada. NO ejecutes campanas de mutacion ni la\nsuite completa: hay dos campanas corriendo y tres carriles trabajando. Puedes\nleer todo y ejecutar como mucho UNA clase de test suelta.\n\nEL PUNTO CIEGO QUE VAS A CAZAR.\n\nEste proyecto cierra cada feature con una campana de mutacion por e…",
      "promptFramed": false,
      "lastProgressAt": 1789033723191,
      "tokens": 130003,
      "toolCalls": 23,
      "durationMs": 350038,
      "resultPreview": "{\"falsos\":[\"HALLAZGO 9 (los tres estados 'succeeded'/'failed'/'retry'): el literal del backend SI tiene oraculo, y triple. (a) Las pruebas del caso de uso NO leen la constante como afirma el cazador: escriben el literal a mano. ExecuteAutomationsTest.java:129 dice `containsExactly(tuple(E1, \\\"succeeded\\\", 1), tuple(E2, \\\"succeeded\\\", 1))`, y lo mismo en :250, :288, :380 y :463 ('succeeded'), :350 …"
    },
    {
      "type": "workflow_agent",
      "index": 10,
      "label": "refuta:30-automatizaciones:1",
      "phaseIndex": 2,
      "phaseTitle": "Refutacion",
      "agentId": "ac8ec80170d11ed40",
      "model": "claude-opus-5[1m]",
      "state": "done",
      "startedAt": 1789033373153,
      "queuedAt": 1789033373150,
      "attempt": 1,
      "lastToolName": "StructuredOutput",
      "lastToolSummary": "He comprobado los catorce hallazgos contra el codigo, fiche…",
      "promptPreview": "Trabajas en C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb, rama main.\n\nERES DE SOLO LECTURA. No edites nada. NO ejecutes campanas de mutacion ni la\nsuite completa: hay dos campanas corriendo y tres carriles trabajando. Puedes\nleer todo y ejecutar como mucho UNA clase de test suelta.\n\nEL PUNTO CIEGO QUE VAS A CAZAR.\n\nEste proyecto cierra cada feature con una campana de mutacion por e…",
      "promptFramed": false,
      "lastProgressAt": 1789033923927,
      "tokens": 163808,
      "toolCalls": 42,
      "durationMs": 550774,
      "resultPreview": "{\"falsos\":[],\"omitidos\":[\"\\\"ON CONFLICT (rule_id, event_id) DO UPDATE SET attempt = EXCLUDED.attempt, status = EXCLUDED.status, created_task_id = ..., delivery_id = ..., error_code = ..., executed_at = ...\\\" en PostgresAutomationWork.upsert(), backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresAutomationWork.java:206-211. Es LA fila que sobrevive a un rollback, la pieza c…"
    },
    {
      "type": "workflow_agent",
      "index": 11,
      "label": "refuta:27-29-conectores:0",
      "phaseIndex": 2,
      "phaseTitle": "Refutacion",
      "agentId": "a5584a7f30b3f7ede",
      "model": "claude-opus-5[1m]",
      "state": "done",
      "startedAt": 1789033466310,
      "queuedAt": 1789033466306,
      "attempt": 1,
      "lastToolName": "StructuredOutput",
      "lastToolSummary": "No refuto: he ido a buscar la prueba de cada uno de los sie…",
      "promptPreview": "Trabajas en C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb, rama main.\n\nERES DE SOLO LECTURA. No edites nada. NO ejecutes campanas de mutacion ni la\nsuite completa: hay dos campanas corriendo y tres carriles trabajando. Puedes\nleer todo y ejecutar como mucho UNA clase de test suelta.\n\nEL PUNTO CIEGO QUE VAS A CAZAR.\n\nEste proyecto cierra cada feature con una campana de mutacion por e…",
      "promptFramed": false,
      "lastProgressAt": 1789033893158,
      "tokens": 124790,
      "toolCalls": 60,
      "durationMs": 426843,
      "resultPreview": "{\"falsos\":[],\"omitidos\":[\"FOR UPDATE en backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresImportedTaskCommit.java:56 (\\\"SELECT status FROM projects WHERE owner_id=? AND id=? FOR UPDATE\\\"). Es el unico cerrojo que impide que un proyecto completado a mitad de importacion siga recibiendo tareas: se lee el estado, se decide con el (operation.apply(states.getFirst())) y se in…"
    },
    {
      "type": "workflow_agent",
      "index": 12,
      "label": "refuta:27-29-conectores:1",
      "phaseIndex": 2,
      "phaseTitle": "Refutacion",
      "agentId": "a41acf1fe1bdd5ea4",
      "model": "claude-opus-5[1m]",
      "state": "done",
      "startedAt": 1789033466314,
      "queuedAt": 1789033466306,
      "attempt": 1,
      "lastToolName": "StructuredOutput",
      "lastToolSummary": "He leido los nueve hallazgos contra el codigo y NO he podid…",
      "promptPreview": "Trabajas en C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb, rama main.\n\nERES DE SOLO LECTURA. No edites nada. NO ejecutes campanas de mutacion ni la\nsuite completa: hay dos campanas corriendo y tres carriles trabajando. Puedes\nleer todo y ejecutar como mucho UNA clase de test suelta.\n\nEL PUNTO CIEGO QUE VAS A CAZAR.\n\nEste proyecto cierra cada feature con una campana de mutacion por e…",
      "promptFramed": false,
      "lastProgressAt": 1789033999071,
      "tokens": 160323,
      "toolCalls": 58,
      "durationMs": 532755,
      "resultPreview": "{\"refutado\":false,\"porque\":\"He leido los nueve hallazgos contra el codigo y NO he podido refutar ninguno: los siete que declara sin oraculo lo estan de verdad, y los dos que declara con oraculo (PROVIDER=github y las dos ISSUES_QUERY completas) tienen exactamente la prueba que cita. Solo dos imprecisiones de detalle, ninguna cambia el veredicto: (a) el hallazgo 2 dice «doce @ExceptionHandler» en G…"
    },
    {
      "type": "workflow_agent",
      "index": 13,
      "label": "sintesis",
      "phaseIndex": 3,
      "phaseTitle": "Sintesis",
      "agentId": "a2ea1fdf58dc314bd",
      "model": "claude-opus-5[1m]",
      "state": "done",
      "startedAt": 1789033999075,
      "queuedAt": 1789033999073,
      "attempt": 1,
      "lastToolName": "Bash",
      "lastToolSummary": "cd \"C:/Users/vhurt/OneDrive/Escritorio/Proyectos/Organizaci…",
      "promptPreview": "Trabajas en C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb, rama main.\n\nERES DE SOLO LECTURA. No edites nada. NO ejecutes campanas de mutacion ni la\nsuite completa: hay dos campanas corriendo y tres carriles trabajando. Puedes\nleer todo y ejecutar como mucho UNA clase de test suelta.\n\nEL PUNTO CIEGO QUE VAS A CAZAR.\n\nEste proyecto cierra cada feature con una campana de mutacion por e…",
      "promptFramed": false,
      "lastProgressAt": 1789034247597,
      "tokens": 125830,
      "toolCalls": 18,
      "durationMs": 248522,
      "resultPreview": "# Punto ciego de literales — informe para el orquestador\n\nVerificado por lectura sobre `main` (solo lectura, sin campañas). Cifras de mutación recalculadas por clase desde los `mutations.xml` que ya están en disco (`backend/build/reports/pitest-*/mutations.xml`), no declaradas.\n\n---\n\n## 1. Literales sin oráculo que sobreviven a la refutación\n\n### GRAVEDAD ALTA\n\n**1. `ERROR_TEXT` del catálogo no co…"
    }
  ],
  "totalTokens": 1859009,
  "totalToolCalls": 504
}