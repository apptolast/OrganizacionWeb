# Decisiones que esperan al propietario — 10 de septiembre de 2026

Nada de esto bloquea el trabajo: los carriles siguen. Bloquea el **cierre** de la
feature que se nombra en cada punto, porque son cambios de contrato y la
disciplina del repo dice que los firmas tú.

---

## 1. Feature 27 — enmienda del recibo, sin contrafirma


**Qué pasó.** El commit `a1b0d20b` (10 de septiembre, 00:19) cambió dos filas de
`features/github_connector.feature` **sin pasar por ti**. Lo caza el panel de
precierre (motivo M13 de `progress/carriles/bloqueantes_27.md`).

**Qué cambió, exactamente:**

- Línea 146: «con sus **once** campos» → «con sus **doce** campos».
- Líneas 165-166: el recibo pasa de contener `id, projectId, repository, status,
  created, skipped, failed, truncated, errorCode, startedAt, finishedAt` a
  contener `id, **source**, projectId, **projectPath**, status, created, skipped,
  failed, truncated, errorCode, startedAt, finishedAt`. Y la cláusula siguiente
  pasa a exigir `source` es "github" y `projectPath` es "octocat/Hello-World".

**Por qué se hizo.** Al unificar la importación de GitHub y GitLab en la feature
29, el recibo HTTP dejó de llevar `repository` y pasó a llevar `source` +
`projectPath`. El `@s20` de `features/additional_connectors.feature:242` exige
que **los dos recibos tengan las mismas claves**. Las dos filas de la 27 habían
caducado, y mientras tanto la región «Resultado de la importación» **no pintaba
ningún contador** en el producto: `decodeReceipt` rechazaba el recibo entero por
`exact(value, RECEIPT_FIELDS)` y el componente caía al catch. O sea que no es un
contador a cero: la sección no existía.

**Mi lectura.** Es coherencia con un contrato que ya existía, no una ampliación:
sin ella, dos contratos aprobados se contradicen. Es del mismo tipo que las dos
enmiendas que ya ratificaste («alinear el `@s31` de la 29 con el de la 27»).
Pero es un cambio de contrato y no lo firmo yo.

**Lo que hay que decidir:** ratificarla, o revertirla y arreglar el desacuerdo
por el otro lado (que el recibo de GitHub vuelva a llevar `repository`, lo que
rompería el `@s20` de la 29).

---

---

## 2. Feature 29 — el `@s9` nombra una columna que no existe


**Qué pasa.** `@s9` de `features/additional_connectors.feature` dice «token_nonce
de 12 bytes». Esa columna **no existe**:
`V29__additional_connectors.sql:37` declara sólo `token_ciphertext`, con el nonce
embebido dentro del criptograma. Lo caza el panel de precierre de la 29.

**Mi lectura.** El nonce embebido es el diseño correcto y el que usa el resto del
repositorio; lo que caducó es la línea del contrato, que describe una forma de
guardar que nunca se implementó. No es un agujero de seguridad: el nonce está,
sólo que dentro del mismo campo.

**Lo que hay que decidir:** enmendar la línea del `.feature` para que describa el
nonce embebido (y contrafirmarla), o añadir la columna `token_nonce` separada y
migrar, que es trabajo real y sin ganancia de seguridad que yo vea.

---

---

## 3. Feature 30 — `@s43` no describe la carrera entre escrituras


**Qué pasa.** Los `Examples` de `@s43` son «navega a /proyectos», «cierra sesión»
y «cambia a otra regla». El camino donde vivía un defecto **real** que se arregló
hoy —«otra escritura la supera», que dejaba `Guardar` y el interruptor inertes
para siempre con el borrador atrapado— **no está en ese Outline ni en ningún
otro**; sólo lo roza `@s40` fila 1 por el lado del botón.

El carril movió las tres pruebas a `@s40`, porque ahí es donde el contrato dice
algo aplicable («deshabilitado **hasta** la respuesta»), y **no enmendó** nada.

**Lo que hay que decidir:** ¿gana `@s43` una cuarta fila de `Examples` para la
carrera entre escrituras, o basta con la lectura de `@s40` fila 1?

---

---

## 4. Feature 30 — el enlace del historial cuando la regla ya no crea tareas


**Qué pasa.** En `automations.tsx:577-585`, si la regla es `NOTIFY_WEBHOOK` el
segmento de proyecto se resuelve a `""` y el `href` sale `/proyectos//tareas/<id>`.

Hoy es inalcanzable por el camino normal —una regla de webhook no crea tareas—
pero **sí** es alcanzable tras un PUT que cambie una regla `CREATE_TASK` a
`NOTIFY_WEBHOOK`: sus ejecuciones antiguas conservan `createdTaskId` y pintarían
un enlace roto. `@s41` dice «un enlace a la tarea creada» y no dice qué hacer
aquí.

**Lo que hay que decidir:** ¿ocultar el enlace, apuntar a la tarea sin proyecto, o
algo más? El carril **no inventó comportamiento**, que es lo correcto.

---

---

## 5. Feature 30 — `upsert` puede pisar una confirmación buena


**Qué pasa.** `PostgresAutomationWork.upsert` (`:204-212`) hace
`ON CONFLICT (rule_id, event_id) DO UPDATE` **sin guarda de estado**, a diferencia
de `claim` (`:192`), que sí exige `AND status = 'retry'`.

El camino es concreto: el worker A calcula su resultado; el worker B confirma
`succeeded` sobre la misma `(regla, evento)`; la transacción de A revierte y A
llama a `record()`, que **pisa el `succeeded` de B** con un `retry`/`failed`.

El carril **no fijó ese defecto como esperado** en la prueba nueva, y eso está
bien: congelarlo en un oráculo habría sido convertir un fallo en contrato.

**Lo que hay que decidir:** ¿se le añade la guarda de estado a `upsert`, o se
declara que el último que escribe manda? Es el punto H6 que el juez pidió
«arreglar o justificar», y ninguna de las dos salidas la puede elegir un carril.

---

---

## 6. Feature 30 — qué muestra el editor al abrir una regla de webhook


**Qué pasa.** Ni `@s37`, ni `@s38`, ni `@s40` dicen nada sobre qué debe mostrar
el editor al abrir una regla `NOTIFY_WEBHOOK`. Hay dos mutantes vivos ahí y el
oráculo que los mataría está escrito y listo, pero afirmarlo sería **fijar como
esperado un comportamiento que nadie ha aprobado**.

**Texto propuesto** para los `Examples` de `@s37`, si te parece bien:

> `| una regla NOTIFY_WEBHOOK | pulsa «Editar» | los campos de tarea del editor aparecen vacíos y el endpoint se conserva |`

Con esa fila los dos mutantes caen en una sola prueba. **No bloquea**: el fichero
mide 84,60 % y el ámbito 91,18 %.
---

---

## 7. Feature 25 — el `@s9` promete modo degradado y el código muere al arrancar


**Qué pasa.** `features/webhooks.feature:129-142` dice «Sin clave de cifrado
válida la aplicación arranca degradada / Then la aplicación queda disponible» y
lista tres filas de `<clave>`: `ausente`, `base64 de 31 bytes` y `texto no
base64`, las tres con resultado `503 CONNECTORS_DISABLED`.

La producción hace otra cosa en dos de las tres:
`AesGcmWebhookSecrets.from` (`backend/src/main/java/.../adapter/webhook/AesGcmWebhookSecrets.java:54-59`)
devuelve `null` **sólo** si la clave está ausente y **lanza**
`IllegalStateException` en los dos casos malformados; `ApplicationConfiguration:451`
construye el bean con ella, así que el contexto no refresca y **la aplicación no
arranca**. `AesGcmWebhookSecretsTest.s9_b5_aMalformedCurrentKeyFailsFastInsteadOfDegrading`
afirma exactamente lo contrario del contrato.

Y no es un despiste: es la política que **ya ratificaste** en la enmienda B5/B6
de seguridad, escrita en `project-spec.md:2496`: «fallo rápido al arrancar si la
clave está mal formada, modo degradado con `CONNECTORS_DISABLED` solo si está
ausente». El código obedece esa política; el `.feature` nunca se tocó.

Lo mismo, menor, en `@s8:125-126`: el contrato dice que el dato adicional
autenticado es «el id del endpoint» y la producción usa `ownerId + "|" +
endpointId`, que es la enmienda **B6** del mismo `project-spec.md:2496`.

**Mi lectura.** Lo que caducó es el `.feature`, no el código. Pero el efecto
visible para un operador no es cosmético: quien se equivoque copiando
`APP_CONNECTOR_KEY` recibe una aplicación muerta, no el modo degradado que el
contrato describe. Merece que lo decidas mirándolo, no que lo alinee un carril.

**La pregunta, exacta:** ¿enmiendo el `.feature` para que diga lo que fija la
política ya ratificada —las dos filas malformadas de `@s9` pasan a «la aplicación
no arranca y registra `CONFIGURATION_ERROR`», y la línea `@s8:125-126` pasa a
«el propietario y el id del endpoint como dato adicional autenticado»—, con su
nota de enmienda fechada dentro del fichero al modo de la de `@s33`? ¿O prefieres
lo contrario, que la producción degrade también con clave malformada, lo que
revocaría la enmienda B5 de la revisión de seguridad en las tres features que la
comparten?

**No lo he tocado.** Es el bloqueante **B6** de
`progress/carriles/bloqueantes_25.md` y queda declarado abierto en
`progress/mutation_webhooks.md`.

---

---

## 8. Feature 25 — el plazo de `@s32` no cuadra con el del `@Scheduled`


**Qué pasa.** El When de `@s32` dice «transcurren 1500 ms desde el arranque» y su
tercera fila exige 20 entregas `succeeded` tras el primer ciclo y las 5 restantes
tras el segundo. La producción es
`@Scheduled(fixedDelay = 1000, initialDelay = 1000)`
(`backend/src/main/java/.../adapter/config/WebhookSchedule.java:27`), de modo que
a los 1500 ms **sólo ha corrido un tic**: el segundo llega a los 2000. Leído al
pie de la letra, el contrato no se cumple.

Los valores ya están medidos: `WebhookScheduleTest.s32_b9_theTickRunsEverySecondAfterOneSecondOfInitialDelay`
los lee de la anotación y los afirma, con su unidad. Lo que falta es que el texto
del contrato y el código digan el mismo plazo.

**La pregunta, exacta:** ¿enmiendo el When de `@s32` a «transcurren 2500 ms desde
el arranque», que es el primer instante en que los dos ciclos han corrido con el
cableado actual, con su nota fechada dentro del fichero? ¿O prefieres cambiar el
cableado —por ejemplo `initialDelay = 500`— para que el contrato se cumpla tal
como está escrito?

**No lo he tocado.** Es el bloqueante **B9** de
`progress/carriles/bloqueantes_25.md`.

---

---

## 9. Feature 25 — una clase de error nueva: `SECRET_UNREADABLE`


**Qué pasa.** Al cerrar el bloqueante **B10** —un secreto que no se puede abrir
detenía la cola de entrega de **todos** los propietarios, en silencio y para
siempre— la entrega afectada pasa a liquidarse como un intento fallido más. Eso
necesita una clase de error, y el catálogo de `error_class` está fijado en dos
sitios: `project-spec.md:2038` («`error_class` en {`HTTP_ERROR`, `REDIRECT`,
`TIMEOUT`, `CONNECTION`, `TLS`, `DNS`, `BLOCKED_ADDRESS`}») y el `CHECK` de
`V23__webhooks.sql:41-42`, que rebotaba la escritura.

He añadido `SECRET_UNREADABLE` al `CHECK`
(`V31__webhook_secret_unreadable_error_class.sql`) y al dominio. **No** he tocado
el `Examples` de `@s25`, porque ese escenario clasifica **respuestas de un
envío** y aquí no hay envío: sin secreto no hay firma y la petición no llega a
salir. Con seis fallos la entrega se agota y arrastra su endpoint a
`DELIVERY_EXHAUSTED`, que es justo el remedio que `project-spec.md` prescribe
para una clave rotada: «se requiere recrear endpoints».

**La pregunta, exacta:** ¿ratificas ampliar el catálogo de `error_class` con
`SECRET_UNREADABLE` y que se añada esa frase a `project-spec.md:2038`? La
alternativa que veo —dejar la entrega pendiente y sólo arrendarla— hace que el
propietario nunca se entere y que la fila vuelva cada cinco minutos para siempre,
que es una versión más lenta del mismo defecto.
