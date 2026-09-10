# Retirada de las features 27, 29 y 30

Decisión del propietario, tomada en tres tandas dentro de la misma sesión y con
las consecuencias delante. Este documento registra qué se borró, qué se
conservó y **por qué**, la migración con su razón, dónde han quedado tocadas
las features supervivientes, y los agujeros que dejo declarados.

- Carril: worktree `C:/Users/vhurt/ow-worktrees/retirar-29`, rama `claude/retirar-29`.
- Doce commits, `+1100 / −40658` líneas sobre `main`.
- Sobreviven la **25** (webhooks) y la **28** (calendario externo), más las 1–24.

## Qué se ha ido

### Feature 29 — catálogo de conectores y GitLab
Catálogo entero (`ConnectorCatalog`, `ConnectorRow`, `ReadConnectorCatalog*`,
el puerto `ConnectorStatusSource` y sus **seis** implementaciones,
`ConnectorCatalogController`); el conector de GitLab completo (casos de uso de
conectar, leer y desconectar; `Gitlab*` de aplicación y dominio;
`GitlabConnectorController`; `PostgresGitlabConnectionStore`; `GitlabApiBase` y
`HttpGitlabIssueSource`); `ImportGuard`; pantallas `gitlab-connector.tsx` y
`connectors-catalog.tsx` con sus clientes, sus pruebas, sus estilos y las rutas
`/conectores` y `/conectores/gitlab`; los dos recorridos E2E con su fixture; y
`features/additional_connectors.feature`.

Las seis fuentes de estado se fueron con el catálogo porque su **único**
consumidor era él: cada una traducía lo que ya sabía su propia feature a una
fila del catálogo. Sin catálogo no traducen para nadie.

### Feature 27 — conector de GitHub
La infraestructura de conectores de issues entera: `ImportIssues` y su puerto
`IssueSource`, las conexiones, el recibo con su almacén, `ConnectGithub` /
`ReadGithubConnection` / `DisconnectGithub`, `ReadIssueImport`,
`ConnectorFailures`, `ConnectorAudit` con su adaptador, `ImportedTaskCommit`,
el dominio (`ExternalIssue`, `GithubRepository`, `PersonalAccessToken`,
`IssueImportReceipt`), `HttpGithubIssueSource`, `GithubApiBase`,
`BoundedResponse`, `GithubConnectorController`, los tres almacenes Postgres, la
pantalla y su cliente, la ruta `/integraciones/github`, los dos recorridos E2E,
el servidor falso de GitHub con su servicio de compose, y
`features/github_connector.feature`.

`BoundedResponse` se fue **en la segunda tanda, no en la primera**: mientras la
27 vivía era el techo de 5 MiB que impedía que un proveedor agotara la memoria
del proceso, así que se conservó; cuando la 27 también se retiró dejó de tener
a quién proteger.

### Feature 30 — automatizaciones
Todo `Automation*` de dominio, aplicación y adaptadores; `ExecuteAutomations`;
`AutomationSchedule` y `AutomationConfiguration`; `PostgresAutomation*`;
`Slf4jAutomationAudit`; `domain/CreateTaskAction`, `domain/NotifyWebhookAction`
y `domain/UnknownEventTypeException`; la pantalla `automations.tsx` con su
cliente, sus pruebas y sus estilos; la ruta `/automatizaciones` y su entrada de
menú; los tres recorridos E2E; y `features/automations.feature`.

`ApplicationConfiguration` perdió **catorce** beans y `ApiErrors` **cinco**
traducciones de error: sólo las suyas, ni una de otra feature.

## Qué se ha quedado, y por qué

| Pieza | Por qué se queda |
|---|---|
| `SecretCipher`, `AesGcmSecretCipher`, `ConnectorKeyRing` | Los usa la **28**: cifra la URL del feed, que lleva el token del proveedor. Comprobado con grep en `SaveExternalCalendar` y `SyncExternalCalendar`. |
| `ConnectorsGate` | Responde 503 en las rutas de la **28** cuando no hay clave configurada. |
| `ConnectorsDisabledException` | La lanza el propio cifrado (`AesGcmSecretCipher`) cuando no hay clave. Es parte del contrato del cifrado, no de los conectores. |
| `ConnectorConfiguration` | Reducida al bean del cifrado. **No se renombra**: eso es otra cirugía, igual que `APP_CONNECTOR_KEY`. |
| `AddressPolicy`, `AnchoredConnection` | Guardia SSRF y anclaje de dirección de la **25** y la **28**. |
| `IntegrationsIndex` | Sigue sirviendo a la API para integraciones de la **24**. Sólo pierde su primera entrada. |
| `import_receipts` (tabla) | Pese al nombre parecido, es de la importación de datos del propietario. Nada que ver con las issues. |

### Dos piezas conservadas que ahora no tienen consumidor — decisión tuya

Las dejo en pie **a propósito**, porque llevan el nombre de la feature 25 y
quien decide si sobran no soy yo:

1. **`WebhookEndpointLookup`** y su `@Bean` en `ApplicationConfiguration`
   (consulta `webhook_endpoints` activos del propietario). Su único cliente era
   la 30.
2. **`WebhookEndpointNotFoundException`** y su traducción en `ApiErrors`
   (422 `ENDPOINT_NOT_FOUND`). Sólo la lanzaba `AutomationReferences`.

Ambas compilan y no molestan; si la 25 no las necesita para su propia API, son
dos borrados de un minuto.

## La migración

**`V32__retire_connectors_and_automations.sql`** — una sola para las tres, como
pediste. `ls` delante: la más alta era `V31`, así que `V32` era la libre.

Retira siete tablas, en orden de claves ajenas:

1. `task_external_links` — apunta a `tasks`, así que va primero. Borrarla no
   toca ninguna tarea: el enlace sólo decía «esta tarea vino de aquella issue».
2. `issue_import_receipts`.
3. `connector_connections` y `gitlab_connections` — guardaban **tokens
   cifrados**. Dejarlas sería peor que no tenerlas: secretos almacenados sin
   ningún código capaz de rotarlos ni de borrarlos, y sin ruta que responda a un
   «bórrame el token».
4. `automation_runs` (referencia a las reglas), `automation_rules`,
   `automation_cursors`.

Antes de escribirla se comprobó **tabla por tabla con grep** sobre backend,
frontend, E2E y scripts que ninguna feature superviviente las lee.

`RetirementMigrationTest` fija las dos mitades —las siete que se van y las de
las supervivientes, que no puede rozar— y **se vio caer** con la migración
apartada antes de darla por buena.

### Nota de historia sobre la migración
En la primera tanda llegué a commitear un `V32__drop_gitlab_connector.sql` que
sólo retiraba lo de la 29 y **conservaba** la forma del recibo de la 27. Al
ampliarse el encargo lo sustituí por la migración única. Nunca se desplegó ni
salió de esta rama, así que reemplazarla es seguro; queda en el historial de
commits por si quieres verla.

## Dónde han quedado tocadas las supervivientes

Nada se ha roto, pero estos ficheros de features vivas cambiaron:

- **`e2e/export-data.spec.mjs` y `e2e/import-data.spec.mjs`**: la lista canónica
  de navegación pierde «Conectores» y «Automatizaciones». Esas listas se afirman
  enteras justamente para que una feature que añade o quita una ruta tenga que
  pasar por aquí: actualizarlas es el mantenimiento previsto.
- **`e2e/support/backend.mjs`**: reiniciaba el servicio falso de GitHub y pedía
  el perfil `e2e` de compose. Era código vivo, no un comentario, y ninguna de
  las dos cosas existe ya.
- **`docker-compose.yml` y `scripts/e2e.mjs`**: fuera el servicio `github-fake`
  y `APP_GITHUB_API_BASE`. `APP_CONNECTOR_KEY` se queda: la necesita la 28.
- **`e2e/webhooks-native-zoom.spec.mjs`** y
  **`e2e/external-calendar-native-zoom.spec.mjs`**: sólo un comentario, que
  citaba el spec de zoom del conector como hermano.
- **`deploy/EGRESS.md`**: fuera el párrafo del conector de GitHub. Los
  conectores que anclan dirección son ahora los dos que quedan.
- **`ApiErrors`**: el manejador conservado se llamaba `automationEndpoint`; pasa
  a `webhookEndpointNotFound`.

Pruebas de la 25 y la 28 ejecutadas tras cada paso, todas verdes:
`WebhookApiTest` 33, `WebhookPersistenceTest` 10, `ExternalCalendarApiTest` 39,
`ExternalCalendarDisabledApiTest` 10, `ExternalCalendarIsolationTest` 7,
`ExternalCalendarPersistenceTest` 23, más `ArchitectureTest`,
`ApplicationWiringTest` 24, `ConnectorSettingsTest` 8, `AddressPolicyTest` 68,
`AesGcmSecretCipherTest` 19, `AnchoredConnectionTest` 10, `ExportPersistenceTest`
39, `ImportPersistenceTest` 137 y `TestDatabaseTest` 2.
Frontend: **2955** pruebas verdes, `tsc`, `prettier` y `eslint` limpios.

## Agujeros declarados

1. **`node --test scripts/project.test.mjs` va 91/95.** Cuatro guardas caen y
   **las cuatro viven en ficheros tuyos**, así que no las he tocado:
   - `#89 automations Stryker configuration mutates only the feature files` —
     se va con la 30.
   - `#8 ics calendar`, `#79 appearance`, `#85 external calendar` — **no son de
     las features retiradas**: son rangos `linea:columna` fijados en
     `frontend/stryker.{ics-calendar,appearance,external-calendar}.config.json`
     que apuntan a `App.tsx` y `workspace.tsx`, y mis borrados desplazaron esas
     líneas. Hay que re-apuntarlos. Las anclas están hoy en:
     `App.tsx:36` `appearance = route === "/apariencia"`,
     `App.tsx:38` `calendar = route === "/calendario"`,
     `App.tsx:43` `externalCalendar = route === "/calendario-externo"`;
     el ternario de `section` va de `App.tsx:50` a `App.tsx:75` y el cuerpo
     empieza en `App.tsx:78`; en `workspace.tsx` los enlaces de
     `/calendario-externo`, `/apariencia` y `/calendario` tienen su `href` en
     las líneas 81, 87 y 99.
2. **Tuyos y sin tocar, como pediste**: `backend/build.gradle.kts` (el
   `mutationScope` de las tres features retiradas sigue declarado),
   `scripts/project.mjs` (targets `github_connector-*`, `automations-*`,
   `additional_connectors-*`), `scripts/project.test.mjs`, `harness.config.json`,
   `feature_list.json`, `project-spec.md` y los `frontend/stryker.*.config.json`
   de las tres features.
3. **`features/webhooks.feature:431`** cita `github_connector.feature:398` en un
   comentario. Es un contrato vivo y no lo edito por mi cuenta.
4. **Se fue `ConnectorExportExposureTest`**, que era de la 27 por nombre y por
   escenario (`@s34`). Con él se pierde la única prueba que afirmaba que la
   exportación lleva **exactamente catorce** colecciones. Si la feature de
   exportación quiere esa guarda, hay que reinjertarla en su carril.
5. **Comentarios históricos** que nombran clases ya borradas, dejados a
   propósito porque explican por qué existe algo que sigue vivo:
   `AnchoredConnectionTest` (cita `HttpGithubIssueSourceTest` como causa de un
   fallo intermitente que motivó la prueba), `scripts/patrones-muertos.mjs`,
   `external-calendar-api.test.ts` y `docs/mvp-delivery-plan.md`.
6. **La `V29` no se revierte entera** y nunca hizo falta: el rename de
   `repository` a `project_path`, la columna `source` y los índices del recibo
   desaparecen con la tabla, no con un `ALTER` de vuelta.
