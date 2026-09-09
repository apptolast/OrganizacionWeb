# Puerta de mutacion de la feature 25 (webhooks)

Trabajo de arnes, no de producto. Cierra los bloqueantes 6, 7 y 8 del dictamen
final (`progress/dictamen_final_25_28_30.md`): la feature 25 esta integrada en
main pero **su puerta de mutacion no existia**, ni la de backend (PIT) ni la de
frontend (Stryker), y `scripts/project.mjs` rechazaba la invocacion.

Punto de partida: `836628c`, `node --test scripts/project.test.mjs` 91/91 verde.

## Las tres piezas

1. Alcance PIT `webhooks` en `backend/build.gradle.kts`: `webhooksClasses`,
   `webhooksOnly`, entrada en los dos `when` (clases y tests), `reportDir`
   propio y **union en el perfil por defecto**.
2. Destinos `webhooks-backend` y `webhooks-frontend` en `scripts/project.mjs`.
3. `frontend/stryker.webhooks.config.json`, umbral 80, `tempDirName` y
   `jsonReporter.fileName` propios.

## Como se derivo el alcance

`git diff --name-status af6f455 d03e4ba -- backend/src/main` (el merge del
carril de webhooks sobre main, `d03e4ba`, contra su padre de main `af6f455`):
44 ficheros de produccion anadidos mas la migracion `V23__webhooks.sql`.
Todos siguen existiendo en `836628c` con el mismo nombre.

### Clases DENTRO del alcance (38 patrones)

- **Dominio** (`domain`): `RetrySchedule`, `WebhookAttempt`, `WebhookCursor`,
  `WebhookDelivery`, `WebhookEndpoint`, `WebhookIntent`,
  `WebhookInvalidException`, `WebhookPingPayload`. Es donde vive el backoff,
  la clasificacion de intentos y la validacion de la URL: el corazon del
  contrato.
- **Casos de uso** (`application`): `CreateWebhook*`, `ManageWebhook*`,
  `EnqueueWebhookDeliveries*`, `DispatchWebhooks*` (el despachador), mas los
  puertos y los tipos de transporte `ClaimedDelivery`, `OutboxCandidate`,
  `ReadyEndpoint`, `WebhookCreation`, `WebhookOperationException`,
  `WebhookAudit`, `WebhookDeliveries`, `WebhookEndpoints`, `WebhookOutbox`,
  `WebhookSecrets`, `WebhookSender`, `WebhookWork`.
- **Guardia de destino**: `application.WebhookDestinationGuard`.
- **Emisor HTTP y criptografia del carril** (`adapter.webhook`):
  `JdkWebhookSender`, `AesGcmWebhookSecrets`, `WebhookSignature`.
- **Frontera HTTP** (`adapter.http`): `WebhookController`,
  `WebhookDeliveryView`, `WebhookEndpointView`.
- **Persistencia** (`adapter.persistence`): `PostgresWebhookStore`,
  `PostgresWebhookOutbox`, `PostgresWebhookWork`.
- **Auditoria**: `adapter.logging.Slf4jWebhookAudit`.
- **Arranque y planificador del worker** (`adapter.config`):
  `WebhookConfiguration`, `WebhookConnectorStartup` (el arranque degradado sin
  clave de conector, enmienda B5) y `WebhookSchedule`.

Los puertos son interfaces y no generan mutantes; se nombran igualmente para
que el alcance sea legible como inventario del corte y para que anadir logica
a un puerto (metodos `default`) no caiga fuera de la puerta sin que nadie lo
note.

### Clases DELIBERADAMENTE FUERA

- `application.AddressPolicy` y `application.PublicAddressPolicy`. Son
  compartidas y **ya estan en `externalCalendarClasses`**
  (`backend/build.gradle.kts:394-395`). Nombrarlas en dos ambitos duplicaria la
  campana y repartiria la responsabilidad de la misma puntuacion entre dos
  informes. Decision explicita del lead.
- `application.WebhookEndpointLookup` y
  `application.WebhookEndpointNotFoundException`. **No son de la feature 25**:
  no aparecen en el diff del carril, las trae la feature 30
  (automatizaciones), y `automationsClasses` ya las cubre con el comodin
  `application.WebhookEndpoint*`.
- `domain.NotifyWebhookAction`. Misma razon: es la accion de una regla de
  automatizacion, ya declarada en `automationsClasses`.
- `adapter.config.ApplicationConfiguration`. El carril la MODIFICA (ahi viven
  los `@Bean` de webhooks), pero es cableado compartido y el perfil por defecto
  ya la muta a traves de cinco ambitos (`integration_api`, `ics_calendar`,
  `external_calendar`, `export_data_persistence`, `automations`). Meterla una
  sexta vez no anade ni un mutante nuevo y hace que la puntuacion de webhooks
  dependa del cableado de todo el producto. La logica propia del arranque de
  webhooks que si es de la feature —el degradado sin clave— vive en
  `WebhookConnectorStartup`, y esa si entra.
- `adapter.config.SecurityConfiguration` y `adapter.http.ApiErrors`. Tambien
  modificadas por el carril, tambien compartidas; `SecurityConfiguration` ya
  esta en `integrationApiHttpClasses`.
- `adapter.connectors.AesGcmSecretCipher` y `ConnectorKeyRing`. Son de las
  features 27/28 y ya estan en `githubConnectorClasses` y
  `externalCalendarClasses`.

Comprobado que hoy el corte no recibe **ni un mutante**: ningun ambito
existente nombra `domain.Webhook*`, `application.*Webhook*` (salvo el comodin
de automatizaciones sobre `WebhookEndpoint*`, que solo casa con un interfaz sin
mutantes), `adapter.webhook.*`, `adapter.http.Webhook*`,
`adapter.persistence.PostgresWebhook*` ni `adapter.logging.Slf4jWebhookAudit`.

## Alcance del frontend

`git diff --name-status af6f455 d03e4ba -- frontend/src` anade `webhooks.tsx`,
`webhooks-client.ts` y `webhooks.scss` (mas sus tests). `App.tsx` y
`workspace.tsx` quedan MODIFICADOS por la ruta y la entrada de navegacion,
pero **no se acotan por rango**: un rango `linea:columna` desfasado no falla,
muta el codigo equivocado en silencio (el dictamen ya documenta un caso). Se
mutan los dos ficheros propios enteros y se deja la ruta cubierta por
`webhooks-route.test.tsx` sin puerta de mutacion, en vez de fingir una puerta
sobre coordenadas que caducan al primer refactor de `App.tsx`.

## Ciclos rojo-verde-refactor

| # | Guarda (ROJO) | Minimo (VERDE) |
|---|---|---|
| 1 | 3 pruebas de despacho de destinos en `scripts/project.test.mjs` | los dos destinos en `scripts/project.mjs` |
| 2 | `webhooks PIT scope covers the whole slice and extends the default` | `webhooksClasses`, `webhooksOnly`, los dos `when`, `reportDir`, union por defecto |
| 3 | `webhooks Stryker configuration mutates only the feature files` | `frontend/stryker.webhooks.config.json` |

## Trazabilidad guarda -> pieza

| Guarda | Pieza que sujeta |
|---|---|
| `webhooks targets reject other tasks and injected options before execution` | la lista blanca de `project.mjs` no acepta tarea distinta ni opciones inyectadas |
| `webhooks backend mutation runs only its PIT scope` | `webhooks-backend` -> `gradlew pitest -PmutationScope=webhooks` |
| `webhooks frontend mutation invokes only its fixed Stryker configuration` | `webhooks-frontend` -> `stryker run stryker.webhooks.config.json` |
| `webhooks PIT scope covers the whole slice and extends the default` | lista literal de clases, `webhooksOnly`, los dos `when` y la union del perfil por defecto |
| `webhooks Stryker configuration mutates only the feature files` | `mutate`, umbral 80, `tempDirName` y `jsonReporter.fileName` propios |

## Oraculo de contenido sobre el alcance

No basta con que el fichero compile: hay que ver **que selecciona de verdad cada
patron**. Se expandieron los 38 patrones contra el arbol de fuentes:

- **42 clases seleccionadas**, todas del carril.
- **De la feature y fuera del alcance: solo `application.AddressPolicy`**, que es
  exactamente la exclusion pedida.
- **En el alcance y no de la feature: ninguna.**
- Los cuatro patrones que casan con dos clases lo hacen a proposito: cada caso de
  uso mas su puerto (`CreateWebhook*`, `DispatchWebhooks*`,
  `EnqueueWebhookDeliveries*`, `ManageWebhook*`).
- Ningun patron queda muerto: cada uno resuelve a un `.java` existente. Eso lo
  congela ademas una guarda permanente, para que no vuelva a pasar lo del ambito
  del calendario externo con `adapter.crypto.*`.

Comprobado tambien, sobre el fichero completo, que los parentesis y las llaves
quedan balanceados, que `webhooks` figura entre los `mutationScope` invocables y
que **los 20 ambitos `*Classes` declarados estan en la union del perfil por
defecto**, webhooks incluido.

## Verificacion adversarial de las guardas

Las guardas se probaron rompiendo cada pieza a proposito. Siete mutantes, siete
muertos, y despues restaurado y verde:

| Mutante | Guarda que lo mata |
|---|---|
| patron a un paquete inexistente (`adapter.crypto.JdkWebhookSender*`) | alcance + resolucion (2 fallos) |
| quitar `+ webhooksClasses` del perfil por defecto | alcance |
| `reportDir` apuntando al informe de otra feature | alcance |
| `webhooks-backend` despachando `-PmutationScope=automations` | despacho de backend |
| `webhooks-frontend` despachando `stryker.automations.config.json` | despacho de frontend |
| borrar `webhooks-frontend` de la lista blanca | despacho de frontend |
| `tempDirName` compartido con otra feature | configuracion de Stryker |

Invocacion en seco de los destinos (con el runner inyectado, sin lanzar nada):

```
SECO: gradlew.bat ["pitest","--no-daemon","-PmutationScope=webhooks"]
SECO: pnpm ["--dir","frontend","exec","stryker","run","stryker.webhooks.config.json"]
RECHAZA webhooks: Invalid target: webhooks
```

## Estado

- `node --test scripts/project.test.mjs`: **97/97 verde** (91 antes, 6 nuevas).
- `node --check` sobre `scripts/project.mjs` y `scripts/project.test.mjs`: OK.
- **No se ha lanzado ninguna campana de mutacion**, ni PIT ni Stryker, ni la
  suite completa: habia otra corriendo en la maquina y dos Gradle sobre el mismo
  proyecto se pisan `build/test-results`. La puerta queda **creada e invocable**;
  ejecutarla es del lead:
  `bin/harness mutate webhooks-backend` y `bin/harness mutate webhooks-frontend`.
- La feature 25 **sigue sin poder marcarse `done`**: esto abre la puerta, no la
  supera, y los otros bloqueantes del dictamen (el 1, el 2 y el 3, entre ellos
  las filas TIMEOUT y TLS de @s25) siguen abiertos.

### Aviso para quien ejecute la campana

Nadie ha medido nunca este corte. Es la primera vez que recibe mutantes, asi que
lo esperable no es un 95 %: el dictamen ya senala ramas de produccion sin ningun
oraculo (`JdkWebhookSender` :68-71 y :83-90, la clasificacion TIMEOUT/TLS). Si la
campana baja del 80 %, no es un fallo de la puerta, es lo que la puerta venia a
descubrir.

### Nota al margen, no tocada

`scripts/project.test.mjs` tiene el bloque de guardas de la feature 30
duplicado literalmente (lineas 2128-2170 y 2172-2213, mismos nombres de prueba,
mismo cuerpo), residuo de la reinjercion al fusionar. Node lo ejecuta dos veces
y no falla. No se toca aqui para no mover el numero de pruebas mientras se cierra
una puerta ajena, pero conviene desduplicarlo.
