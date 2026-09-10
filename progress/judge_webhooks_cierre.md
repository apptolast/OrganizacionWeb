# Review — feature 25 `webhooks` (cierre)

**Veredicto:** APPROVED CONDICIONADO A las dos campañas de mutación y a cinco
correcciones documentales/contractuales enumeradas al final.

Método: sólo lectura. No he ejecutado la suite, ni `bin/harness init`, ni
mutación (hay dos campañas y un carril en la misma máquina). Todo lo que afirmo
aquí sale de abrir los ficheros en `main`; donde razono sobre un rojo que no he
visto, lo digo.

---

## 1. Los 23 hallazgos del dictamen: verificados uno a uno

Refuté por defecto. **Los 23 están cerrados de verdad.** No he encontrado ni un
solo «declarado y no hecho». Evidencia por hallazgo:

| #          | Cerrado por                                              | Comprobado en                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| ---------- | -------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1, 17      | TIMEOUT y TLS contra receptor real                       | `JdkWebhookSenderTest.java:355` (receptor que acepta y no responde; `accepted.await` prueba que la conexión se aceptó, `latencyMs >= plazo-5` prueba que el plazo transcurrió) y `:386` (certificado no confiable). Plazo de producción cableado y afirmado en `:347-351` leyendo el `connectTimeout` del cliente real, no el campo                                                                                                                                  |
| 2          | Recorte por elemento, conjunto nombrado, dos dimensiones | `e2e/webhooks-ux.spec.mjs:208-226` (selector `main li span, main .webhook-secret textarea, main .webhook-secret input, main tbody td`; filtro por `scrollWidth` **y** `scrollHeight`) y la aserción real en `:262-264`. El `input` sigue en el selector aunque el producto sea hoy `textarea`: si alguien lo devuelve, el oráculo no deja de mirar. Producto cambiado a `textarea` en `frontend/src/webhooks.tsx:309-315`                                            |
| 3          | Zoom nativo                                              | `e2e/webhooks-native-zoom.spec.mjs:316-322` (`chrome.tabs.setZoom(tab.id, 2)`, `expect(zoom).toBe(2)`, `expect.poll(devicePixelRatio)`), con recorte `:334`, foco visible `:231`, axe `:376`                                                                                                                                                                                                                                                                         |
| 4, 12B, 13 | Recorrido de teclado real                                | `e2e/webhooks-ux.spec.mjs:438-451` (expectativa derivada del DOM, dos listas para que un tabindex negativo no se autoexcluya), `:524` reachable == visible, `:528` nombres únicos, `:536` orden exacto de ida, `:547` orden exacto de vuelta rotado, `:537` y `:548` foco visible en CADA parada con `matches(":focus-visible")` más el estilo del producto (`:480-487`). El no-op de `getComputedStyle(el, ":focus-visible")` está muerto y explicado en `:466-469` |
| 5          | Anclaje implementado (ver §3)                            | `JdkWebhookSender.java:128-131`, `adapter/net/AnchoredConnection.java`                                                                                                                                                                                                                                                                                                                                                                                               |
| 6, 7, 8    | Puertas de mutación cableadas                            | `backend/build.gradle.kts:44` webhooksOnly, `:583-622` webhooksClasses (38 patrones, incluidas las 13 clases de adapter), `:625`, `:658`, `:689`; `scripts/project.mjs:80-81` y `:246-257`; `frontend/stryker.webhooks.config.json` (ficheros enteros, sin rangos línea:columna — decisión correcta a la luz de `progress/hallazgo_rangos_stryker.md`)                                                                                                               |
| 9          | Las dos filas de @s22, con conteo de copias              | `WebhookRecoveryPersistenceTest.java:405` (1 copia) y `:443` (2 copias, mismo eventId)                                                                                                                                                                                                                                                                                                                                                                               |
| 10         | SKIP LOCKED con oráculo que discrimina                   | `WebhookWorkPersistenceTest.java:204-260`. El artesano hizo bien en NO seguir la propuesta del dictamen: el oráculo temporal (elapsed < 3000) no discriminaba con cola. El suyo pregunta por la identidad de la fila reclamada mientras otra transacción retiene la primera: con SKIP LOCKED devuelve otra, con FOR UPDATE devuelve justo la retenida. Sin cronómetro                                                                                                |
| 11, 19     | @s29 (ver §3)                                            | `WebhookWorkPersistenceTest.java:339-382`                                                                                                                                                                                                                                                                                                                                                                                                                            |
| 14         | @s38 monta la precondición antes de afirmar la ausencia  | `frontend/src/webhooks.test.tsx:317-362`: crea el webhook, afirma presencia del secreto y de la lista de Ana (`:348-350`), y sólo entonces cambia de identidad y afirma las tres ausencias más la lista de Bea. El mutante de key constante muere                                                                                                                                                                                                                    |
| 15         | Región role=status propia y anunciada                    | `frontend/src/webhooks.tsx:279-286`, escrita desde changeStatus `:212`, ping `:227`, remove `:237`, redeliver `:257`; unitario `webhooks.test.tsx:580`; E2E `webhooks-ux.spec.mjs:551-567` con el ciclo vacío→texto. El defecto que el propio E2E destapó (la región no se exponía) está corregido                                                                                                                                                                   |
| 16         | Filas Bearer de @s33                                     | `WebhookApiTest.java:411-433`, con la enmienda a 403 API_SCOPE_DENIED ya reconciliada en `features/webhooks.feature:403-406, 417-418`                                                                                                                                                                                                                                                                                                                                |
| 18         | @s28 completo                                            | `WebhookRecoveryPersistenceTest.java:197` afirma la secuencia exacta D2, E1, E2 (`:260-263`), y la compuerta de estado activo tiene test propio en `:299-312`                                                                                                                                                                                                                                                                                                        |
| 20         | @s40 por fila, no por conteo                             | `webhooks.test.tsx:538-553` (within(fila) para succeeded y exhausted, queryByRole null para pending), URL exacta con el id de la fila `:570-572`, y el efecto visible «Pendiente / intento 0» `:575-579`. El mutante de invertir la guarda muere                                                                                                                                                                                                                     |
| 21         | @s30 aislamiento entre webhooks                          | `WebhookPersistenceTest.java:231`                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| 22         | Portapapeles                                             | `webhooks.test.tsx:205`, `:238` not.toHaveBeenCalled() con el panel visible, `:242` toHaveBeenCalledExactlyOnceWith(secret) tras el gesto                                                                                                                                                                                                                                                                                                                            |
| 23         | Cifra de unitarios                                       | `progress/ux_webhooks.md` corregida a 44 con desglose (commit ae3a56d)                                                                                                                                                                                                                                                                                                                                                                                               |

Los tres patrones que esta noche ha cazado el proyecto —oráculos que no pueden
fallar, verde por suerte de carga, oráculo que no discrimina— los he buscado
expresamente en el trabajo de cierre y **no reaparecen**. El margen de 5 ms de
`JdkWebhookSenderTest:379-381` no es suerte de carga: la comparación sigue
siendo contra el plazo completo y es imposible pasarla sin haberlo esperado.

---

## 2. Cobertura del contrato: los 42 escenarios

**Los 42 tienen al menos un oráculo que puede fallar.** Recorrido completo (rutas
relativas a `backend/src/test/java/com/apptolast/organization/` y a `frontend/src/`):

@s1 `WebhookApiTest:60` + `CreateWebhookTest:38` + `WebhookPersistenceTest:70` ·
@s2 `WebhookIntentTest:29,48,57` · @s3 `WebhookIntentTest:71-122` ·
@s4 `WebhookApiTest:292-353` · @s5 `CreateWebhookTest:86` + `WebhookWiringTest:54` ·
@s6 `WebhookPersistenceTest:103` (más `s7:121`, que parte de 4 y exige un solo 201: cubre en sustancia la fila 4+0) ·
@s7 `WebhookPersistenceTest:121` · @s8 `AesGcmWebhookSecretsTest:24,36,45` + `WebhookPersistenceTest:84` ·
@s9 `WebhookConnectorStartupTest:48,58` + `AesGcmWebhookSecretsTest:81-105` + `ManageWebhookTest:111` + `DispatchWebhooksTest:139` ·
@s10 `WebhookApiTest:98,111` + `WebhookPersistenceTest:147` ·
@s11 `WebhookApiTest:203,219` + `ManageWebhookTest:99,156` ·
@s12 `WebhookApiTest:119,148` + `ManageWebhookTest:130,144` + `WebhookEndpointTest:26,36,43` ·
@s13 `WebhookApiTest:165` + `WebhookPersistenceTest:200` + `ManageWebhookTest:174` ·
@s14 `ManageWebhookTest:45,71,89` + `WebhookApiTest:173` ·
@s15 `WebhookSignatureTest:14` + `JdkWebhookSenderTest:250` (vector exacto del contrato) ·
@s16 `JdkWebhookSenderTest:228` · @s17 `WebhookPingPayloadTest:14-47` + `WebhookDeliveryTest:16` ·
@s18 `WebhookOutboxPersistenceTest:94,112,164` + `EnqueueWebhookDeliveriesTest:132,236` ·
@s19 `EnqueueWebhookDeliveriesTest:155,169` ·
@s20 `DispatchWebhooksTest:154` + `WebhookOutboxPersistenceTest:205` ·
@s21 `EnqueueWebhookDeliveriesTest:106-208` + `Slf4jWebhookAuditTest:64` ·
@s22 `WebhookRecoveryPersistenceTest:405,443` · @s23 `WebhookWorkPersistenceTest:150,204` ·
@s24 `WebhookAttemptTest:29` + `DispatchWebhooksTest:218` ·
@s25 las OCHO filas: `JdkWebhookSenderTest:292,302,326,338,355,386,496` + `WebhookAttemptTest:59-82` ·
@s26 `JdkWebhookSenderTest:276` (200/204/299) ·
@s27 `WebhookWorkPersistenceTest:283` + `DispatchWebhooksTest:200` ·
@s28 `WebhookRecoveryPersistenceTest:197,299` ·
@s29 `WebhookWorkPersistenceTest:339` + `WebhookApiTest:227` ·
@s30 `ManageWebhookTest:183,203,224` + `WebhookPersistenceTest:231` + `WebhookApiTest:254` ·
@s31 `WebhookRecoveryPersistenceTest:340` ·
@s32 `WebhookScheduleTest:27,38` + `DispatchWebhooksTest:174,188` ·
@s33 las OCHO filas: `WebhookApiTest:366,378,391,411,435` ·
@s34 `CreateWebhookTest:113` + `WebhookApiTest:203` ·
@s35 `Slf4jWebhookAuditTest:46-100` + `DispatchWebhooksTest:111,126` ·
@s36 `webhooks.test.tsx:105,125,136,158` · @s37 `webhooks.test.tsx:198,245,269` ·
@s38 `webhooks.test.tsx:293,317` · @s39 `webhooks.test.tsx:181,364,398,424,446` ·
@s40 `webhooks.test.tsx:475,584` · @s41 `webhooks.test.tsx:601,631,653` ·
@s42 `webhooks.test.tsx:92,675,691` + `e2e/webhooks-ux.spec.mjs:374,388,506,551,570` + `e2e/webhooks-native-zoom.spec.mjs:234`.

Ninguno queda sin oráculo. ~~La única cláusula del contrato que hoy NO se cumple
es media línea de @s25 (ver §6.3).~~

> **Corregido el 10 de septiembre de 2026.** Esa frase **ya no es cierta y no lo
> era del todo cuando se escribió**. El panel de precierre y el juez de cierre
> encontraron, entre las dos rondas, al menos **cuatro** cláusulas que el código
> incumple, no una: la de `@s25`, las dos filas de `@s9` —que prometen
> «la aplicación queda disponible» donde `AesGcmWebhookSecrets.versioned` lanza
> y el contexto no arranca—, el plazo de `@s32` —«transcurren 1500 ms» no cabe
> en un `@Scheduled` de `initialDelay=1000, fixedDelay=1000`, que a los
> 1500 ms sólo ha corrido un ciclo cuando la fila 3 exige dos— y la línea
> `@s8:125-126`, que dice «el id del endpoint» cuando el dato adicional
> autenticado es `ownerId + "|" + endpointId`.
>
> Se tacha en vez de borrarse porque **la frase hizo de puerta**: mientras estuvo
> escrita, cualquiera que leyera este veredicto daba por cerrado lo que no lo
> estaba. Las cuatro están hoy encoladas en `progress/decisiones_pendientes.md`.

Observación acotada, no bloqueante: el recorrido de teclado de
`e2e/webhooks-ux.spec.mjs:506` se ejecuta en un solo estado (lista con el
formulario visible, 1280 px). El panel del secreto y el de entregas quedan fuera
de esa comparación de orden, aunque sí entran en las auditorías de geometría,
recorte y axe de los siete estados y en la de zoom nativo. El dictamen pedía
cuatro estados; se ha entregado el que tiene 20 de los controles. Lo dejo como
mejora, no como puerta.

---

## 3. Las dos decisiones del propietario: aplicadas

### @s29 — 52 filas persistidas, 50 servidas

Ambas partes tienen oráculo y **el `.feature` no se enmendó**: `features/webhooks.feature:367`
y `:368` siguen literalmente como estaban. Las dos eran correctas y el código las
honra sin tocarlas.

- Línea 367, lo que se guarda: `WebhookWorkPersistenceTest.java:360` afirma 52
  filas; `:363-366` afirma CUÁLES 50 y EN QUÉ ORDEN
  (`recorded.subList(5, 55).reversed()`); `:367-369` que las cinco podadas son
  las cinco más antiguas; `:370-373` que ninguna pendiente se poda. La mutación
  DESC→ASC del prune muere aquí.
- Línea 368, lo que se sirve: `PostgresWebhookStore.java:126-137` añade `LIMIT ?`;
  `WebhookWorkPersistenceTest.java:377` afirma `served.size() == 50` y `:378-381`
  la secuencia exacta contra el orden real de la tabla. La mutación DESC→ASC del
  list muere aquí.

Consecuencia deliberada y bien anotada: 2 de las 52 filas no son alcanzables por
la API. No es defecto; es la resolución del propietario.

### Anclaje del reenlace DNS — implementado y probado en las dos direcciones

- **Implementado**: `JdkWebhookSender.java:128` resuelve y valida una vez;
  `:129` construye la petición contra `AnchoredConnection.literal(target, pinned)`;
  `:192` conserva el nombre en la cabecera Host; `:184` lo ofrece en SNI con
  verificación HTTPS (`AnchoredConnection.java:68-73`).
- **TLS en las dos direcciones**, que es lo que el propietario pidió:
  - Certificado válido para el NOMBRE, aceptado yendo a la DIRECCIÓN:
    `JdkWebhookSenderTest.java:448-459`.
  - Certificado no confiable, rechazado por la ruta anclada: `:484-492`; y fuera
    del anclaje `:386-394`.
  - Y un tercero que impide que el primero sea un placebo: el MISMO certificado
    se rechaza si el nombre pedido es otro, `:468-476`. Sin él, un
    `endpointIdentificationAlgorithm` apagado pasaría los dos primeros.
  - Prueba de que no hay segunda resolución: `:404-420`; el nombre no existe
    fuera de la zona fabricada, así que llegar al receptor sólo es posible por la
    dirección anclada.
- **Coherencia documental**: `deploy/EGRESS.md:12-70` dice exactamente lo que hace
  el código, con el precio escrito (HTTP/1.1, primera dirección,
  allowRestrictedHeaders); el javadoc de `JdkWebhookSender.java:28-45` afirma sólo
  lo que hace y nombra sus límites residuales; `project-spec.md:2492` recoge la
  decisión con sus oráculos nombrados.

  **Excepción, y es condición del cierre**: `project-spec.md:2050`, dentro de
  «### Límites explícitos» de la propia feature 25, sigue diciendo «DNS rebinding
  entre comprobación y conexión no queda cerrado por la aplicación». Contradice a
  `:2492`, a `deploy/EGRESS.md` y al código. Es la misma familia del hallazgo 5
  —un artefacto que afirma algo falso sobre el código—, sólo que ahora en la
  dirección segura. Es una línea y la corrige el coordinador.

---

## 4. El defecto de `jdk.httpclient.allowRestrictedHeaders`: bien resuelto y bien probado

Bien resuelto:

- `OrganizationApplication.java:17` la declara como PRIMERA línea de `main`, antes
  de `SpringApplication.run`, con el porqué escrito en `:10-16`. Es el único punto
  del que se puede llegar antes que cualquier bean de Spring.
- `AnchoredConnection.allow()` (`:38-42`) AÑADE en vez de pisar lo que declarase
  el despliegue, y eso tiene oráculo propio en `AnchoredConnectionTest.java:132-146`.
- El bloque estático de `JdkWebhookSender.java:57-59` se conserva como red, no como
  garantía, y así lo dice el comentario. Correcto.
- En pruebas la decisión es del JVM y no del orden de carga:
  `backend/build.gradle.kts:37` para `test` y `:717` para `pitest`.

Bien probado, y con el acierto que importa: `AnchoredConnectionTest.java:107-115`
NO afirma «la propiedad está puesta» —eso no discriminaría nada— sino que ESTE
JVM deja construir la petición con la cabecera Host. Es la afirmación al nivel al
que el defecto ocurría. El javadoc `:94-104` deja medida la regresión original
(20 pruebas cayendo con FEED_UNREACHABLE según qué clase cargara primero), que es
exactamente la forma que tendría el fallo en producción con Spring creando beans
en orden arbitrario: todas las entregas como «no alcanzable».

Riesgo residual anotado y no bloqueante: si algún día se ejecutara la suite sin la
`systemProperty` de `build.gradle.kts:37`, la prueba de `:107` podría pasar por
casualidad si otra clase hubiera llamado a `allow()` antes en ese mismo JVM. La
red es la propiedad declarada en Gradle, y está.

---

## 5. Las cuatro dimensiones

- **Cobertura**: 42/42 con oráculo (§2). Las capas están donde deben: dominio y
  casos de uso con dobles, persistencia contra Postgres real, adaptador de red
  contra receptores reales (HTTP y TLS), frontera HTTP con MockMvc, vista en jsdom
  y auditoría en navegador. Respeta `docs/architecture.md`: la política de
  direcciones es puerto de aplicación (`AddressPolicy`) y el anclaje vive en
  `adapter/net`, compartido por las features 25 y 28 en lugar de duplicado —que es
  justo lo que dejó que los dos carriles decidieran lo contrario sin verse.
- **Oráculos**: es la dimensión que más ha mejorado. Los dos que no podían fallar
  están cerrados con aserción real, y los sustitutos eligen bien el discriminante:
  identidad de fila para SKIP LOCKED, tercer certificado para la verificación por
  nombre, presencia antes que ausencia en @s38, `within(fila)` en @s40. Las tablas
  de rojos acreditados de `progress/tdd_webhooks_cierre_dictamen.md:668-672`,
  `:771-775`, `:937-940` y `:996-999` documentan la mutación aplicada y el rojo
  obtenido, no una afirmación.
- **Accesibilidad**: orden de tabulación contra el DOM, ida y vuelta; foco visible
  por parada medido con el anillo del producto; nombre accesible; 44 px; recorte en
  DOS dimensiones sobre los tres sujetos que el contrato nombra; siete estados,
  cuatro anchos, dos temas, text200, forced-colors, reduced-motion, zoom nativo al
  200 % y axe a cero. Es el listón más alto de este repositorio.
- **Seguridad**: secreto cifrado con AES-GCM y el id del endpoint como AAD, con
  oráculo de que descifrar con otro id falla; secreto en claro sólo en el 201, con
  los cuatro canales de fuga afirmados incluido el portapapeles; canal Bearer
  denegado con `verifyNoInteractions`; SSRF con la política compartida, https
  obligatorio y `Redirect.NEVER`; y ahora el anclaje con TLS probado en las dos
  direcciones. Auditoría sin URL completa, sin firma, sin secreto y sin cuerpos.

---

## 6. Lo que el artesano deja abierto: qué bloquea y qué no

**6.1 El foco cae al `body` al pulsar «Desactivar»** (`frontend/src/webhooks.tsx:399-417`:
el fragmento de dos botones se sustituye por «Activar» y React desmonta el botón
enfocado). **NO bloquea.** @s42 nombra el retorno de foco para dos casos —panel del
secreto y confirmación de borrado— y los dos tienen oráculo
(`webhooks.test.tsx:675,691`; `e2e/webhooks-ux.spec.mjs:570-588`). Ninguna cláusula
del contrato cubre este otro caso, y exigirlo sería inventar contrato. Dicho eso:
es lo único de la lista que un usuario notaría, el artesano lo señala como lo
primero que miraría y estoy de acuerdo. Que se abra como trabajo propio, con su
rojo primero; no se cuela en este cierre.

**6.2 `field-sizing: content` sólo lo implementa Chromium** (`webhooks.scss:143-149`),
así que el secreto se recortaría en Firefox y Safari al 200 % de texto. **NO
bloquea**: toda la puerta UX de este repositorio es Chromium y ninguna feature
cerrada afirma otra cosa; el análisis del artesano (49 caracteres, ~33 por línea,
harían falta cuatro filas) es correcto y está declarado en vez de escondido, que
es la conducta que quiero ver. **Sí exijo** que deje de vivir sólo en una bitácora
de carril: pertenece a «### Límites explícitos» de la feature 25 en
`project-spec.md`, junto a los demás límites, porque es ahí donde mira el
siguiente revisor.

**6.3 `latencyMs` no usa el reloj inyectado.** `features/webhooks.feature:320` dice
«medido con el cronómetro monótono inyectado» —enmendado y **ratificado**, ver
`progress/ratificaciones.md` entrada **R4**— y `JdkWebhookSender.java:161-168`
lo mide con el cronómetro;

> **Corregido el 10 de septiembre de 2026.** Este párrafo citaba la línea 320 con
> un texto que **ya no dice** y situaba la medición en `:123` y `:209-211`,
> que **no son las líneas**: `elapsedMillis` se usa en `:161-168` y se
> declara en `:238`. Se corrigen las tres citas. El fondo del hallazgo era
> correcto y se resolvió: el propietario eligió enmendar la línea al cronómetro
> monótono, y la contrafirma está escrita en el propio `.feature`. el `Clock` sólo alimenta el `t` de la firma. La mitad
> verificable de la cláusula («entero no negativo») sí tiene oráculo
> (`JdkWebhookSenderTest:285` y `:379-381`); la otra mitad era falsa. ~~Es la ÚNICA
> cláusula del contrato que el código incumple~~ —ver la corrección de §5, son al
> menos cuatro—, y no podía cerrarse callando: o se
> cambia el código con su rojo primero, o se enmienda la línea 320 pasando por la
> puerta de aprobación humana, como se hizo con las filas Bearer de @s33. Queda como
> condición y no como rechazo porque es decisión del propietario, y porque el
> artesano hizo lo correcto al no tocar producción sin un test que la pidiera.

---

## 7. Condición de mutación

La feature no pasa a `done` hasta que terminen las dos campañas y se adjunten
`progress/mutation_webhooks_backend.md` y `progress/mutation_webhooks_frontend.md`.

### Umbrales exigidos por capa

- **Backend**, `node scripts/project.mjs mutate webhooks-backend` (ámbito
  `webhooks` de `backend/build.gradle.kts:44,625,658,689`): **>= 0,80**, el de
  `harness.config.json:22`. Sin rebaja y sin excluir clases para subir la cifra.
- **Frontend**, `mutate webhooks-frontend` (`frontend/stryker.webhooks.config.json`):
  `break: 80` tal como está declarado. La navegación de @s36 en `App.tsx` y
  `workspace.tsx` la muta la configuración base (`frontend/stryker.config.json:55,57`,
  ficheros enteros), así que no queda fuera de ámbito. Celebro que este ámbito NO
  fije rangos `línea:columna`: es exactamente la clase de fallo de
  `progress/hallazgo_rangos_stryker.md` y aquí se ha evitado por construcción.

### Clases que tienen que recibir mutantes sí o sí

Si el informe muestra **cero mutantes generados** para alguna de éstas, la puerta
no vale aunque la puntuación salga alta: es el patrón de `AesGcmSecretCipher`.

`WebhookSignature`, `AesGcmWebhookSecrets`, `JdkWebhookSender`,
`PostgresWebhookStore`, `PostgresWebhookWork`, `PostgresWebhookOutbox`,
`WebhookController`, `WebhookConnectorStartup`, `WebhookSchedule`,
`Slf4jWebhookAudit`, `RetrySchedule`, `WebhookAttempt`, `WebhookCursor`,
`WebhookDelivery`, `WebhookEndpoint`, `CreateWebhook`, `ManageWebhook`,
`DispatchWebhooks`, `EnqueueWebhookDeliveries`, `WebhookDestinationGuard`.
En el frontend: `src/webhooks.tsx` y `src/webhooks-client.ts`.

### Hallazgo propio del juez, y es condición

`com.apptolast.organization.adapter.net.AnchoredConnection` **no aparece en ningún
ámbito PIT del repositorio**. Comprobado leyendo: `grep -n "adapter.net"
backend/build.gradle.kts` devuelve UNA sola línea, `:452` `SystemHostResolver*`;
`webhooksClasses` (`:583-622`) no la nombra —su comentario `:577-582` enumera lo que
excluye a propósito y ésta no está—; `externalCalendarClasses` (`:430-460`) tampoco;
y la rama `else` de `:655` es la unión de ámbitos más `core` (`domain.*` +
`application.*`), que no alcanza `adapter.net`.

Es decir: **la clase que implementa la decisión B3 del propietario recibe cero
mutantes en todos los modos de ejecución**, tres líneas por debajo del comentario
de `:447-449` que conmemora exactamente ese mismo defecto en `AesGcmSecretCipher`.
Y tiene lógica mutable de sobra: el `contains(HOST)` de `:40`, el
`isAddressLiteral` de `:76`, los valores por defecto de puerto y ruta de `:48-52`,
el `getPort() < 0` de `:57`. Sus nueve pruebas propias (`AnchoredConnectionTest`)
no las mide nadie.

**Añádase `com.apptolast.organization.adapter.net.AnchoredConnection*` a
`webhooksClasses` antes de lanzar la campaña del backend.**

### Supervivientes esperados y aceptados por lectura

El artesano anota que los literales SQL sobrevivirán porque PIT no muta cadenas.
Es cierto, y **acepto su acreditación**, porque la he verificado uno a uno en el
fichero de prueba y no en su bitácora. Son seis, no cuatro:

| Literal                                                                | Oráculo que lo sujeta                                                             |
| ---------------------------------------------------------------------- | --------------------------------------------------------------------------------- |
| `LIMIT ?` de `PostgresWebhookStore.list:130`                           | `WebhookWorkPersistenceTest.java:377`                                             |
| `ORDER BY updated_at DESC, id DESC` de `list:129`                      | `WebhookWorkPersistenceTest.java:378-381`                                         |
| `ORDER BY updated_at DESC, id DESC` del prune de `PostgresWebhookWork` | `WebhookWorkPersistenceTest.java:363-369`                                         |
| `FOR UPDATE OF d SKIP LOCKED` de `PostgresWebhookWork:68`              | `WebhookWorkPersistenceTest.java:204-260`, por identidad de fila y sin cronómetro |
| `e.status = 'active'` de `PostgresWebhookOutbox:46`                    | `WebhookRecoveryPersistenceTest.java:299-312`                                     |
| `ORDER BY occurred_at, event_id` del `after()`                         | `WebhookRecoveryPersistenceTest.java:260-263`                                     |

Supervivientes también aceptables sin discusión: los de
`JdkWebhookSender.classify()` (`:153-159`), que es red por defecto para
`IOException` envueltas mientras la rama viva para TLS es el catch directo de
`:140` —lo demuestran `JdkWebhookSenderTest:386` y `:484`—; y cualquier cosa de
`e2e/`, que Stryker no muta por diseño.

---

## Checkpoints

- C1 contrato Gherkin aprobado y no enmendado a conveniencia: **[x]** (las dos
  líneas de @s29 intactas; la enmienda de @s33 lleva su nota y su ratificación)
- C2 cobertura escenario ↔ test: **[x]** 42/42
- C3 disciplina TDD, sin producción que ningún test exija: **[x]** (la bitácora
  documenta mutación aplicada y rojo obtenido, no afirmaciones)
- C4 calidad y arquitectura: **[x]**
- C5 seguridad: **[x]**, con `project-spec.md:2050` a corregir
- C6 accesibilidad: **[x]**
- C7 puerta de mutación: **[ ]** — en curso; condiciones en §7

---

## Cambios requeridos para levantar la condición

1. Correr las dos campañas con los umbrales y las clases obligatorias de §7 y
   adjuntar los dos `progress/mutation_webhooks_*.md`.
2. Añadir `com.apptolast.organization.adapter.net.AnchoredConnection*` a un ámbito
   PIT **antes** de lanzar la campaña del backend.
3. Corregir `project-spec.md:2050`, que declara como límite explícito de la
   feature 25 un reenlace DNS que el código ya cierra, contra `:2492`, contra
   `deploy/EGRESS.md` y contra `JdkWebhookSender.java:128-131`.
4. Llevar a «### Límites explícitos» de la feature 25 en `project-spec.md` el
   límite de `field-sizing: content` (sólo Chromium), hoy sólo en una bitácora de
   carril.
5. Resolver la discrepancia de `latencyMs`: enmendar `features/webhooks.feature:320`
   por la puerta de aprobación humana, o cambiar el código con su rojo primero.
   No vale dejarlo.
6. `bin/harness init` en verde, ejecutado por el coordinador cuando la máquina esté
   libre. No lo he ejecutado por instrucción expresa; sin él, ninguna aprobación de
   este dictamen es firme.

Recomendado y **no** condición: devolver el foco al botón que sustituye al que se
desmonta en «Desactivar»/«Activar», con su prueba unitaria; ampliar el recorrido de
teclado a los estados del secreto y de entregas; y añadir `e2e/webhooks-ux.spec.mjs`
a la lista de `lint` de `scripts/project.mjs:460-475`, donde hoy sólo está
`e2e/webhooks-native-zoom.spec.mjs`.
