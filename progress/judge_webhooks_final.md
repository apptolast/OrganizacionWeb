# Review — feature 25 webhooks (`features/webhooks.feature`)

**Veredicto: CHANGES_REQUESTED** — la condición bloqueante anterior (**B4 sin entregar**) queda
**CERRADA**: `docs/webhooks.md` existe, dice las cuatro cosas y explica el porqué. Abro
**cinco correcciones nuevas**, todas de documentación: ninguna toca `src/` ni pruebas, ninguna
obliga a repetir mutación ni la suite. Tercera pasada, 10 de septiembre de 2026, noche.
Sustituye al veredicto de la segunda pasada de este mismo fichero.

Encargo de esta pasada: comprobar si `docs/webhooks.md` cierra B4, si lo que afirma es cierto
contra producción, y dictaminar sobre el hueco declarado del enlace. No se reaudita nada de lo
ya dado por bueno (las dos puertas de mutación, `harness init`, las cuatro enmiendas
ratificadas, las cuatro bloqueantes de la segunda pasada).

**El árbol no ha movido código desde el verde acreditado.** `git diff --name-only 17b823f9 HEAD`
devuelve `backend/build.gradle.kts`, `docs/webhooks.md`, `progress/current.md`,
`progress/judge_webhooks_final.md` y `progress/mutation_external_calendar_backend.md`. Ni un
fuente, ni una prueba. Las demás puertas siguen firmes: backend 92,81 %, frontend 94,57 %,
`harness init` verde. No ejecuté nada pesado: la campaña de la 28 está corriendo.

---

## La bloqueante anterior: **CERRADA**

`docs/webhooks.md` (commit `354f36dc`) entrega las **cuatro** exigencias de B4, en orden y sin
aguar, y no las enuncia: las razona.

| B4 exige | Dónde | ¿Correcto contra código? |
|---|---|---|
| Verificar la **firma primero** | `:14-15`, `:24-43` | Sí. `WebhookSignature.java:13-19`: `t=<unix>,v1=<hex minúsculo>` sobre `HMAC-SHA256(secreto, t + "." + body)`. El aviso de **no reserializar y guardar los bytes crudos** (`:36-40`) es lo que `project-spec.md:2036` llama «los bytes UTF-8 almacenados, idénticos en cada intento; sólo `t` cambia». |
| Deduplicar por el `eventId` **del cuerpo firmado** | `:16-17`, `:45-57` | Sí, y **corrige al propio spec**: `project-spec.md:2030` todavía dice «el receptor deduplica por `X-OrganizationWeb-Event-Id`», que es justo lo que B4 vino a prohibir. |
| Rechazar más de **300 s** | `:18-19`, `:70-77` | Sí, con el número explícito, no «unos cinco minutos». Y `:76-77` añade un dato cierto que evita el falso positivo obvio: el reintento **refirma con `t` nuevo** (`JdkWebhookSender.java:228`, `clock.instant()` en cada envío), así que un reintento legítimo nunca cae fuera de la ventana por viejo. |
| Comparar en **tiempo constante** | `:20`, `:79-84` | Sí, con las tres primitivas por plataforma y el motivo del canal lateral. |

**El motivo del punto 2 está explicado, que era la prueba de fuego.** `:49-54` no dice «usa el
del cuerpo»: dice **por qué**. La cabecera viaja **fuera de la firma** —cierto: la firma cubre
`t + "." + body` y nada más, `WebhookSignature.java:17-18`, mientras la cabecera se añade
aparte en `JdkWebhookSender.java:225`—, está ahí por comodidad, y quien capture una entrega
puede reenviarla **con esa cabecera cambiada** dejando cuerpo y firma válidos. La consecuencia
está dicha con todas las letras: «verás un evento nuevo donde hay una repetición, y actuarás
dos veces sobre el mismo hecho». Es la razón de ser de la enmienda y está entera.

## Lo que verifiqué contra producción, y sale bien

- **Los doce tipos**: `:104-117` coinciden uno a uno, y **en el mismo orden canónico**, con
  `WebhookIntent.CATALOG:12-24` y con `OutboxMessage.validationCode():22-33`.
- **La segunda columna de esa tabla no está inventada**: «Crear proyecto… Cerrar sesión de
  trabajo» son literalmente `frontend/src/webhooks.tsx:17-30` (`eventLabels`), en ese orden,
  que es el que la casilla pinta en `:420`.
- **Escalera de reintentos** `:92-93`: 1, 5, 30, 120 y 1440 min = `RetrySchedule.DELAYS`
  (1 min, 5 min, 30 min, 2 h, 24 h). Idéntica.
- **El sexto intento** `:93-95`: `RetrySchedule.MAX_ATTEMPTS = 6` → `WebhookDelivery.recorded`
  pasa a `EXHAUSTED` → `DispatchWebhooks:73-79` arrastra el endpoint a
  `disabledByExhaustion` con `DELIVERY_EXHAUSTED` (`WebhookEndpoint:20,42`), y el
  `PUT /{id}/status` permite rehabilitarlo, como promete `:95`.
- **Cabeceras** `:28-34`: `Content-Type: application/json; charset=utf-8`, `User-Agent:
  OrganizationWeb-Webhooks/1`, `X-OrganizationWeb-Event-Id`, `X-OrganizationWeb-Signature` =
  `JdkWebhookSender.java:50-51, 223-228`. Exactas, incluido el `/1`.
- **«Cualquier 2xx» y «no leemos tu cuerpo»** `:90`: `WebhookAttempt.classify` (200–299 →
  éxito) y `HttpResponse.BodyHandlers.discarding()`.
- **Redirecciones** `:132-134`: `followRedirects(NEVER)` y 300–399 → `REDIRECT`, que es fallo.
- **Reverificación del destino al conectar** `:129-131`: cierto y bien matizado —
  `validatedAddress` comprueba **todas** las direcciones en cada intento y ancla la petición a
  la ya validada (`AnchoredConnection`, enmienda B3).
- **El secreto no viaja nunca** `:126-127`: `CreationView.toString` lo redacta y el contrato lo
  afirma en `features/webhooks.feature:120-123`.

---

# Lo que sigue abierto — cinco correcciones, todas de markdown

## 1. BLOQUEANTE — `docs/webhooks.md:42-43` promete una rotación que no existe y que el contrato declara fuera de alcance

> «El secreto es el `whsec_…` … No se puede volver a consultar: **si lo pierdes, hay que
> rotarlo**.»

No hay rotación de secreto **en ninguna parte**:

- `project-spec.md:1999` la lista literalmente entre lo **fuera de alcance**: «…edición de URL,
  **rotación de secreto**, entrega desde el canal Bearer…».
- Decisión 8 en `project-spec.md:2054`: «**Sin edición de URL ni rotación de secreto: recrear
  cubre ambos casos** con cinco plazas».
- `WebhookController` expone ocho rutas y ninguna rota nada: `POST /`, `GET /`, `GET /{id}`,
  `PUT /{id}/status`, `DELETE /{id}`, `POST /{id}/ping`, `GET /{id}/deliveries`,
  `POST /{id}/deliveries/{deliveryId}/redeliver`.

El remedio real está escrito en `project-spec.md:2026`: «si aparece un endpoint cuyo secreto no
se llegó a ver, **indica eliminarlo y crear otro**». La página manda al lector a buscar un botón
que no existe. Es una frase: cambiar «rotarlo» por «eliminar el endpoint y crear otro».

## 2. BLOQUEANTE — `docs/webhooks.md:59-68`: el cuerpo de ejemplo lleva `"type": "TaskCreated.v1"`, y ningún `TaskCreated.v1` real llega así

El ejemplo tiene exactamente seis campos: `eventId, aggregateId, ownerId, occurredAt,
schemaVersion, type`. Pero el cuerpo entregado **es el `payload` de la outbox verbatim** —
`PostgresWebhookOutbox.java:61` lo lee como `payload::text` y `:79-94` lo inserta tal cual en
`webhook_deliveries.body`; es la decisión 3 de `project-spec.md:2054`, «cuerpo igual al payload
de la outbox … cero mapeo»— y `OutboxMessage.validationCode():47-55` exige para
`TaskCreated.v1` **exactamente ocho** claves: las seis más **`taskId`** y **`title`**. Una fila
que no las lleve nunca se entrega: `EnqueueWebhookDeliveries:50-53` la descarta como
`INVALID_EVENT`. Lo confirma también `docs/outbox-publishing.md:72`.

No es cosmético: un receptor que copie el ejemplo y valide «exactamente estas seis propiedades»
—que es precisamente el receptor cuidadoso al que va dirigida la página— **rechazará todas las
entregas reales**. Dos arreglos válidos: etiquetar el ejemplo como `webhook.ping.v1`, que sí
tiene exactamente esos seis campos (`WebhookPingPayload.java:19-32`, y así lo fija el `@s` del
cuerpo sintético, `features/webhooks.feature:253-256`), o decir que cada tipo **añade** los
suyos y remitir a `docs/outbox-publishing.md:72,80,88,96`.

## 3. BLOQUEANTE — `docs/webhooks.md:119-120` ata el ping a la creación, y el ping no está atado a nada

> «Además, **al crear un endpoint** puedes enviarte un ping de prueba.»

El ping es una acción explícita sobre **cualquier endpoint activo, en cualquier momento**:
`WebhookController:124-127` (`POST /{id}/ping`), botón «Enviar ping» por fila en
`frontend/src/webhooks.tsx:461-462`, y `project-spec.md:1999`, que lo define como evento
sintético que «**sólo se emite por acción explícita**», sin mencionar el alta. Para el receptor
la diferencia importa: puede recibir `webhook.ping.v1` un martes cualquiera, no sólo en el
minuto del alta. Otra frase.

## 4. BLOQUEANTE — falta el único dato sin el cual no se puede verificar una firma, y este fichero lo tenía asignado por decisión

La clave HMAC son los **bytes UTF-8 de la cadena `whsec_…` completa, prefijo incluido** — **no**
los 32 bytes que el base64url esconde. `WebhookSignature.java:16` no deja duda:
`secret.getBytes(StandardCharsets.UTF_8)`.

La página no lo dice. `:36` dice `HMAC-SHA256(secreto, t + "." + cuerpo)` y `:42` «el secreto es
el `whsec_…`»: se lee en los dos sentidos, y el secreto **tiene toda la pinta de ser base64**
(`whsec_` + 43 caracteres base64url sin relleno, `features/webhooks.feature:17`), que es justo
la trampa. El `gherkin_author` la vio y la elevó como duda abierta 3
(`progress/gherkin_webhooks.md:66`), y la decisión del coordinador del 8 de septiembre (`:87`)
la cerró con estas palabras: «Clave HMAC = bytes UTF-8 de la cadena completa `whsec_...`
(confirmado; **`docs/webhooks.md` lo documenta**)». Hoy no lo documenta.

De paso, y en la misma edición: `project-spec.md:2058` promete «**ejemplo de verificación de
firma** y tolerancia recomendada de 5 min para `t`». La tolerancia está (`:72`); el ejemplo no.
No hace falta inventarlo: el vector ya está calculado y probado en
`features/webhooks.feature:231-235` —secreto `whsec_AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8`,
cuerpo de 209 bytes y dos instantes—. Copiarlo cierra la promesa y demuestra, de paso, cuál es
la clave.

## 5. BLOQUEANTE, y el más barato — el hueco del enlace: **te doy la razón en el fondo, no en la forma**

**Sobre no añadir el `<a>`: tienes razón, y lo ratifico como juez.** Ningún `@s` de
`features/webhooks.feature` pide ese enlace; añadirlo sería producción que ninguna prueba exige,
que es lo que este repositorio prohíbe y lo que yo mismo he exigido pasada tras pasada.
Comprobé el hecho por mi cuenta: `frontend/src/webhooks.tsx` no contiene ni `href` ni `docs/`.
La decisión es correcta y así queda escrita.

**Sobre declararlo en `docs/webhooks.md:152-162` y darlo por cerrado: no.** Declarar un hueco lo
hace visible, pero no vuelve verdadera la línea del contrato. `project-spec.md:2044` sigue
diciendo «ayuda que **enlaza** `docs/webhooks.md`», y sobre este árbol eso es falso. Es
literalmente el punto 2 de la bloqueante que abrí en la pasada anterior —«la sección 25 afirma
dos veces algo que el árbol desmiente»—: si lo apruebo ahora, me contradigo. Y mi Vía A pedía
las dos mitades: escribir el documento **y** «o bien añadir el enlace…, **o bien enmendar
`:2044` con nota fechada**». Se entregó la primera mitad y se sustituyó la segunda por un tercer
documento que describe el problema. Eso deja el contrato mintiendo con una nota al pie.

**Respondo a tu pregunta directa: lo correcto es enmendar el spec, no añadir fila de contrato.**
Tres razones:

1. `:2044` es prosa de interfaz que el contrato ejecutable **nunca quiso** volver escenario:
   ninguna de las 42 filas roza el enlace.
2. Un `@s` que dijera «la ayuda contiene un enlace a `docs/webhooks.md`» sería un oráculo
   pésimo: clava una **ruta de documentación** en una aserción de UI, se rompe el día que el
   fichero se mueva y no mata mutante alguno. Añadir contrato para justificar un `<a>` es
   inflar alcance por la puerta de atrás.
3. El repositorio **ya tiene el mecanismo y lo usó dos veces hoy** para exactamente esto: una
   línea del spec que el árbol desmiente se cierra con nota fechada y ratificación (R10 sobre el
   AAD, R11 sobre la clave). Esto es un R12 de tres líneas.

Concreto: enmendar `project-spec.md:2044` con nota fechada y ratificación —la ayuda del
formulario **no** enlaza el documento; la página se referencia desde `docs/`— y anotarlo en
`progress/ratificaciones.md`. `:2058` **no** hay que tocarlo: pasó a ser cierto en cuanto el
documento existió. Hecho eso, el bloque `:152-162` de `docs/webhooks.md` sobra o se reduce a un
puntero.

---

## Nota no bloqueante

**La página nunca dice cuánto es «el plazo».** `:90` y `:98` hablan de «dentro del plazo» y «más
que nuestro plazo» sin cifra, y al receptor al que se le pide «responde rápido y luego procesa»
le hace falta el número: **5 s de conexión y 10 s de intercambio completo**
(`JdkWebhookSender.java:48-49`, enmienda B1). No lo hago bloqueante porque la página **no
miente**, sólo calla. Ojo al escribirlo: `project-spec.md:2035` todavía dice «timeout de
conexión 5 s y de **lectura 5 s**», que es el texto que B1 dejó atrás. La cifra buena es la del
código.

Siguen vigentes, sin cambios, las no bloqueantes 1 a 5 de la pasada anterior: el informe de
frontend ignorado por git mientras la bitácora dice que está en el repositorio;
`progress/mutacion_webhooks_frontend_medida.md` sin banner de superado; la cita muerta de
`features/webhooks.feature:455`; el javadoc de `WebhookScheduleTest:224-230`; y el hueco del
`ORDER BY` sin oráculo de `PostgresWebhookWork.java:67`.

## Checkpoints

- **C1** [x] — sin cambios: lint y 89/89 guardas verificados por mí en la pasada anterior sobre
  el mismo árbol de código; desde `17b823f9` no se ha tocado ni un fuente ni una prueba.
- **C2** [x] — 27 features, 25 y 28 en `in_progress`, permitido por `one_feature_at_a_time: false`.
- **C3** [x] — ninguna producción de la 25 sin test que la pida.
- **C4** [x] — misma base que C1.
- **C5** [x] — `git status --porcelain` vacío al abrir esta revisión.
- **C6** [ ] — los 42 `@s` tienen test y B4 ya está entregada, pero la página publica tres
  afirmaciones que el código desmiente (rotación, cuerpo de ejemplo, ping), le falta la clave
  HMAC que una decisión ratificada le asignó, y `project-spec.md:2044` sigue sin enmendar.
- **C7** [x] — backend 400/431 = **92,81 %**, frontend 592/626 = **94,57 %**, recomputadas por
  mí en la pasada anterior sobre informes que coinciden con el árbol. Nada las invalida: desde
  entonces sólo cambió markdown.

## Cobertura de escenarios (@s ↔ test)

- @s1..@s42: **[x]**, sin cambios. `git diff --name-only 3113e9ef HEAD` son tres ficheros de
  documentación; ni `backend/src`, ni `frontend/src`, ni `e2e`.

## Disciplina TDD

- **Rojo→Verde→Refactor:** SÍ, sin cambios. Esta entrega es documentación pura.
- **¿Producción sin test que la pida?** **NO.** Y lo subrayo: la decisión de **no** colar el
  enlace en `webhooks.tsx` es disciplina bien aplicada, no un descuido.

---

## Resumen

La bloqueante grande está cerrada, y bien cerrada: `docs/webhooks.md` dice las cuatro cosas que
B4 exige, con el número 300 y no un «unos minutos», y explica el porqué del punto 2 mejor de lo
que lo explica la propia enmienda. Los doce tipos, sus etiquetas de interfaz, la escalera de
reintentos, el sexto intento, las cabeceras y el contrato de la firma están verificados contra
el código y son ciertos.

Lo que la retiene es la advertencia que se me pidió aplicar: **un documento público que miente
es peor que no tenerlo**, y éste afirma tres cosas que el árbol desmiente —una rotación que el
contrato declara fuera de alcance, un cuerpo de ejemplo que ningún `TaskCreated.v1` real puede
tener, y un ping atado al alta— y omite el único dato sin el cual no se puede verificar una
firma, que una decisión ratificada asignó por escrito a este mismo fichero. Más la enmienda de
`:2044`, que es la mitad pendiente de la vía que abrí en la pasada anterior.

Son cinco ediciones de markdown. Ni una línea de `src/`, ni una prueba, ni una campaña que
repetir: las otras puertas siguen acreditadas y este cambio no las roza. Hecho eso, la 25 pasa a
`done` sin condiciones.
