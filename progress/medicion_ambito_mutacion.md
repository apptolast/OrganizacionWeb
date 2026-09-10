# Medición del ámbito de mutación — verdad de partida

11 de septiembre de 2026. Rama `main`, árbol limpio, commit `c83ef392`.

Este fichero **no propone diseño**. Sólo establece, con números reproducibles,
qué muta hoy el objetivo vacío, qué muta la unión de los ámbitos con nombre, y
cuánto cuesta cada cosa. Nadie debería diseñar el troceado del workflow nocturno
sin estos números delante.

Los dos resolvedores usados están junto a este fichero y se pueden reejecutar:
`progress/medicion_ambito_backend.mjs` y `progress/medicion_ambito_frontend.mjs`.

---

## 0. Método, para que otro lo repita

```bash
# universo de produccion del backend: un FQN por fichero .java
find backend/src/main/java -name '*.java' | wc -l          # 470

# resolucion de patrones PIT contra ese universo
node progress/medicion_ambito_backend.mjs  /tmp/backend.json
node progress/medicion_ambito_frontend.mjs /tmp/frontend.json
```

`medicion_ambito_backend.mjs` hace tres cosas:

1. Enumera los `.java` de `backend/src/main/java` y los convierte a nombre
   completamente cualificado (ruta sin prefijo, sin `.java`, `/` → `.`).
2. Parsea **del propio `backend/build.gradle.kts`** todas las declaraciones
   `val NOMBRE = ...` del bloque `pitest { ... }`, evaluando `setOf(...)`,
   la suma `+` de conjuntos y el único `.filter { !it.contains("broker") }`
   que aparece. No hay lista transcrita a mano: si el fichero cambia, el
   resultado cambia.
3. Resuelve cada patrón como glob de PIT (`*` → `.*`, casa también el punto)
   contra el universo del paso 1.

**Unidad de conteo: una clase = un fichero `.java`.** PIT también muta clases
anidadas y sintéticas, así que estos números son un suelo, no un techo. Lo que
importa aquí es la comparación entre conjuntos, y ésa es exacta.

**Contraste independiente de que el resolvedor es correcto:** un escéptico
anterior midió, por otro camino, «131 de 372 clases de domain+application (35 %)
sin mutar por ningún ámbito con nombre». Mi resolvedor da exactamente
**371 clases en `domain`+`application` dentro del universo** — 372 contando el
fichero raíz — y **131 de ellas huérfanas**. Coincide al entero.

Una nota sobre las citas: los números de línea de este fichero están sacados con
`grep -n` sobre el árbol de `c83ef392`, no de memoria.

---

## 1. El universo: qué muta hoy el objetivo vacío

`.github/workflows/harness-mutation.yml:49` ejecuta `node .harness/harness.mjs verify`
dentro de un job con `timeout-minutes: 240` (línea 30). `verify` llama a
`runMutation(cfg)` sin objetivo; como `harness.config.json` declara
`"mutation": { "threshold": 0.8, "targets": [] }`, `resolveMutationTargets`
devuelve la lista de un solo elemento vacío y se ejecuta
`node scripts/project.mjs mutate` **sin target**.

Sin target, `scripts/project.mjs` cae hasta el final de `createProject`:
`backend(commands[task])` (línea 426) con `commands.mutate = "pitest"` (línea 168),
es decir `gradlew pitest --no-daemon` **sin `-PmutationScope`**; y después
`runner("pnpm", ["--dir", "frontend", task])` (línea 427), es decir
`pnpm --dir frontend mutate` → `stryker run` → `frontend/stryker.config.json`.

Sin `mutationScope`, el `when` de `targetClasses`
(`backend/build.gradle.kts:548`) entra por la rama `else` de la **línea 575**:

```
else -> core + authenticationClasses + taskAdapters + taskStatusAdapters +
        availabilityAdapters + scheduleBlockAdapters + todayAdapters +
        rescheduleClasses + startWorkSessionClasses + pauseResumeSessionClasses +
        closeWorkSessionClasses + endTimeNotificationClasses + historyClasses +
        weeklyReviewClasses + appearanceClasses + customizationClasses +
        exportPersistenceClasses + exportHttpClasses + importReaderClasses +
        importHttpClasses + importPersistenceClasses + integrationApiClasses +
        integrationApiHttpClasses + icsCalendarClasses + externalCalendarClasses +
        webhooksClasses
```

`core` está en la **línea 76** y es la trampa:

```
val core = setOf("com.apptolast.organization.domain.*", "com.apptolast.organization.application.*")
```

Es el **único** sitio del fichero donde hay comodines de paquete. Todos los demás
conjuntos son listas cerradas de clases.

### Resultado

| | |
|---|---|
| Ficheros `.java` de producción | **470** |
| Patrones distintos que produce la rama `else` | **225** |
| **Clases de producción que muta hoy el objetivo vacío** | **456** |
| Clases de producción que ni siquiera el objetivo vacío muta | 14 |

Desglose del universo (456) por paquete:

| paquete | en el universo | total en producción |
|---|---|---|
| `domain` | 93 | 93 |
| `application` | 279 | 279 |
| `adapter.http` | 37 | 41 |
| `adapter.persistence` | 29 | 34 |
| `adapter.config` | 7 | 10 |
| `adapter.webhook` | 3 | 3 |
| `adapter.connectors` | 2 | 2 |
| `adapter.net` | 2 | 2 |
| `adapter.logging` | 2 | 3 |
| `adapter.broker` | 1 | 1 |
| `adapter.feed` | 1 | 1 |
| raíz | 0 | 1 |

**`domain` y `application` entran enteros por el comodín de `core`.** Ése es el
motivo de que el objetivo vacío sea irreemplazable por una unión de listas
cerradas.

### Las 14 que hoy no muta nadie, ni el objetivo vacío

Fuera del universo, y por tanto fuera de cualquier discusión de troceado (ya
están sin medir hoy; ningún diseño las empeora, pero conviene saber que existen):

```
OrganizationApplication
adapter.config.ConnectorConfiguration
adapter.config.PublisherConfiguration
adapter.config.PublisherSchedule
adapter.http.OriginGuard
adapter.http.ProjectController
adapter.http.ProjectEditController
adapter.http.ProjectReadController
adapter.logging.Slf4jPublicationAudit
adapter.persistence.PostgresOutboxWork
adapter.persistence.PostgresProjectCommit
adapter.persistence.PostgresProjectEditing
adapter.persistence.PostgresProjectQueries
adapter.persistence.PostgresProjectStatusEditing
```

---

## 2. La unión de los ámbitos con nombre alcanzables

`scripts/project.mjs` acepta 42 targets (lista blanca en las líneas 37–78). De
ellos, los que despachan PIT lo hacen con `-PmutationScope=<x>`. Los ámbitos así
alcanzables son **22**, pero uno de ellos no existe en Gradle:

- **21 ámbitos con rama propia** en el `when`: `appearance`, `close_work_session`,
  `custom_views_fields`, `end_time_notification`, `export_data_persistence`,
  `external_calendar`, `history`, `ics_calendar`, `import_data_http`,
  `import_data_persistence`, `import_data_reader`, `integration_api`,
  `integration_api_http`, `pause_resume_session`, `reschedule`, `schedule_block`,
  `start_work_session`, `start_work_session_replay`, `today`, `webhooks`,
  `weekly_review`.
- **`noche_cinco` NO tiene rama.** `scripts/project.mjs:78` lo declara en la lista
  blanca y las líneas 191–192 lo despachan como
  `pitest -PmutationScope=noche_cinco`, pero en `backend/build.gradle.kts` la
  cadena `noche_cinco` aparece **una sola vez, en un comentario (línea 663)**. No
  hay `val nocheCincoOnly`. Por tanto `noche_cinco-backend` **cae en el `else`**:
  es un alias del objetivo vacío, con el mismo coste y el mismo universo. Lo que
  `progress/plan_campanas.md` describe como «el atajo combinado» y da por muerto
  cuatro veces por memoria es, literalmente, la campaña completa.

Y hay **5 ámbitos declarados en Gradle que ningún target de `project.mjs` puede
lanzar**: `authentication`, `availability`, `complete_reopen_task`, `create_task`,
`split_task`. Sus ramas existen y resuelven clases, pero son código muerto desde
el arnés.

### Tamaño resuelto de cada rama (clases de producción)

| ámbito | clases | | ámbito | clases |
|---|---|---|---|---|
| `reschedule` | 54 | | `history` | 12 |
| `pause_resume_session` | 50 | | `create_task` ✖ | 11 |
| `webhooks` | 43 | | `export_data_persistence` | 9 |
| `start_work_session` | 40 | | `weekly_review` | 9 |
| `schedule_block` | 40 | | `availability` ✖ | 8 |
| `custom_views_fields` | 33 | | `import_data_http` | 6 |
| `integration_api` | 28 | | `today` | 6 |
| `external_calendar` | 28 | | `integration_api_http` | 5 |
| `end_time_notification` | 19 | | `import_data_persistence` | 4 |
| `complete_reopen_task` ✖ | 19 | | `authentication` ✖ | 4 |
| `ics_calendar` | 18 | | `import_data_reader` | 2 |
| `close_work_session` | 15 | | `start_work_session_replay` | 1 |
| `split_task` ✖ | 14 | | | |
| `appearance` | 13 | | | |

✖ = declarado en Gradle, sin target en `scripts/project.mjs`.

### Los dos números que importan

| unión | clases cubiertas | de 456 |
|---|---|---|
| **21 ámbitos alcanzables hoy desde `scripts/project.mjs`** | **308** | 67,5 % |
| Los 26 ámbitos con nombre (añadiendo los 5 sin target) | **345** | 75,7 % |
| Objetivo vacío (rama `else`) | **456** | 100 % |

---

## 3. Las huérfanas

### 3.1 Con los ámbitos alcanzables hoy: **148 clases**

Ninguna de estas 148 recibiría un solo mutante si el workflow nocturno se
trocease en los 21 targets que hoy existen.

| paquete | huérfanas |
|---|---|
| `application` | 106 |
| `domain` | 25 |
| `adapter.http` | 9 |
| `adapter.persistence` | 6 |
| `adapter.config` | 2 |

Lista completa (nombres cortos, prefijo `com.apptolast.organization.`):

**`adapter.config` (2)**
`JavaTimeZoneCatalog`, `SessionCookiePolicy`

**`adapter.http` (9)**
`AvailabilityController`, `ExportDataController`, `ExportHeadersFilter`,
`SessionAccessDeniedHandler`, `SessionController`, `SessionFailureFilter`,
`TaskController`, `TaskHistoryController`, `TaskStatusController`

**`adapter.persistence` (6)**
`ExportReceiptWriter`, `PostgresAvailabilityStore`, `PostgresTaskCommit`,
`PostgresTaskHistoryQueries`, `PostgresTaskQueries`, `PostgresTaskStatusStore`

**`application` (106)**
`ActionPreview`, `ActiveProjectLimitException`, `AvailabilityConflictException`,
`AvailabilityEditing`, `AvailabilityQueries`, `AvailabilityRequiredException`,
`AvailabilityZoneUnavailableException`, `BrokerPublisher`, `CalendarFeed`,
`CalendarFeedLink`, `CalendarFeedStatus`, `CalendarFeedTokens`,
`CalendarNotFoundException`, `CalendarQueries`, `CalendarTooLargeException`,
`ChangeProjectStatus`, `ChangeProjectStatusUseCase`, `ChangeTaskStatus`,
`ChangeTaskStatusUseCase`, `ConnectorsDisabledException`, `CreateProject`,
`CreateProjectUseCase`, `CreateSubtask`, `CreateSubtaskUseCase`, `CreateTask`,
`CreateTaskUseCase`, `DeliveryOutcome`, `EditProject`, `EditProjectUseCase`,
`ExtendWorkSessionUseCase`, `ExternalCalendarAudit`, `ExternalCalendarStore`,
`FeedFetch`, `HostResolver`, `ImportConflictException`, `ImportDataCommands`,
`ImportDataQueries`, `ImportDataUseCase`, `ImportFileChangedException`,
`ImportInvalidFileException`, `ImportKeyReusedException`, `ImportPreview`,
`ImportReceipt`, `ImportReceiptQueries`, `ImportTooLargeException`,
`InvalidProjectTransitionException`, `OutboundGuard`, `OutboxWork`,
`PlanBlockUseCase`, `ProjectChange`, `ProjectCommit`, `ProjectCompletedException`,
`ProjectConflictException`, `ProjectCreated`, `ProjectEditing`,
`ProjectNotFoundException`, `ProjectQueries`, `ProjectStatusChange`,
`ProjectStatusChanged`, `ProjectStatusEditing`, `ProjectUpdated`,
`PublicationAudit`, `PublishOutboxUseCase`, `ReadAvailability`,
`ReadAvailabilityUseCase`, `ReadBlocksUseCase`, `ReadProjects`,
`ReadProjectsUseCase`, `ReadSubtasks`, `ReadSubtasksUseCase`, `ReadTaskHistory`,
`ReadTaskHistoryUseCase`, `ReadTaskStatus`, `ReadTaskStatusUseCase`, `ReadTasks`,
`ReadTasksUseCase`, `ReadTodayUseCase`, `ReadWorkSessionEndUseCase`,
`RepositoryIdentity`, `ResourceNotFoundException`, `SaveAvailability`,
`SaveAvailabilityUseCase`, `SecretCipher`, `StorageUnavailableException`,
`StoredSubscription`, `SubtaskCommit`, `SubtaskCreated`, `SubtaskQueries`,
`SyncOutcome`, `TaskCommit`, `TaskCompletedException`, `TaskConflictException`,
`TaskCreated`, `TaskCreation`, `TaskCreationEvent`, `TaskHistoryQueries`,
`TaskQueries`, `TaskStatusChange`, `TaskStatusChanged`, `TaskStatusEditing`,
`TaskStatusQueries`, `TodayQueries`, `TopologyMismatchException`,
`WebhookEndpointLookup`, `WebhookEndpointNotFoundException`, `ZoneCatalog`

**`domain` (25)**
`Availability`, `AvailabilityRevision`, `EventProject`, `EventTask`, `FeedError`,
`FieldError`, `Project`, `ProjectPage`, `ProjectPosition`, `ProjectRevision`,
`ProjectSnapshot`, `ProjectStates`, `ProjectSummary`, `PublicationAttempt`,
`SyncStatus`, `Task`, `TaskHistoryEntry`, `TaskHistoryPage`,
`TaskHistoryPosition`, `TaskPage`, `TaskPosition`, `TaskRevision`,
`TaskSnapshot`, `TemplateValues`, `ValidationException`

### 3.2 Aunque se dieran target a los 5 ámbitos muertos: **111 clases siguen huérfanas**

Reactivar `authentication`, `availability`, `complete_reopen_task`, `create_task`
y `split_task` rescata 37 clases (148 → 111). Recupera `Task`, `Availability`,
`CreateTask`, `ChangeTaskStatus`, `SessionController`, `TaskController`,
`PostgresTaskQueries`… pero **no** rescata `Project`, `ValidationException`,
`CreateTaskUseCase`, `ChangeTaskStatusUseCase`, `EditProjectUseCase`,
`ReadTasksUseCase` ni ninguna de las 93 clases de `application` que quedan.

Reparto de las 111: `application` 93, `domain` 15, `adapter.http` 2,
`adapter.persistence` 1.

Entre ellas, todo el vertical de **proyectos** (`Project`, `ProjectPage`,
`ProjectRevision`, `ProjectSnapshot`, `ProjectStates`, `ProjectSummary`,
`CreateProject`, `EditProject`, `ChangeProjectStatus`, sus casos de uso y sus
excepciones) y **todos los puertos `*UseCase`** que separan dominio de adaptador.
No existe ni ha existido nunca un ámbito con nombre para proyectos: sus dos actas
—`progress/mutation_edit_project_backend.md` y
`progress/mutation_project_states_backend.md`— dicen literalmente que se midieron
con `gradlew.bat ... pitest` **sin `-PmutationScope`**, o sea por el `else`, y
escribiendo en el `reports/pitest` genérico.

### 3.3 La consecuencia, dicha sin rodeos

**Cualquier troceado que se limite a los ámbitos con nombre muta menos que hoy.**
Con los targets existentes deja fuera 148 de 456 clases (32,5 %); reactivando los
cinco muertos, 111 de 456 (24,3 %). La regla innegociable prohíbe las dos.

Un troceado admisible tiene que llevar el comodín de `core`
(`domain.*` + `application.*`) repartido entre los jobs, no sustituido por listas
cerradas.

---

## 4. Frontend

### 4.1 Lo que muta el objetivo vacío

`pnpm --dir frontend mutate` → `"mutate": "stryker run"` en
`frontend/package.json` → **`frontend/stryker.config.json`** (Stryker usa esa
configuración por defecto).

| | |
|---|---|
| Entradas del array `mutate` | **54** |
| Ficheros distintos | **53** |
| Entradas que son rango de líneas, no fichero entero | 2: `src/session-gate.tsx:22:0-31:100` y `src/session-gate.tsx:44:10-44:37` |
| `concurrency` | **2** |
| `thresholds` | `high 90 / low 80 / break 80` |
| `coverageAnalysis` | `perTest` |

Ficheros `.ts`/`.tsx` de producción bajo `frontend/src` (excluidos tests,
`.d.ts`): **78**.

### 4.2 Lo que mutan los configs por feature

`scripts/project.mjs` invoca **19** ficheros de configuración distintos, más un
`--mutate` en línea para `schedule_block-frontend`
(`src/schedule-block-api.ts,src/task-blocks.tsx,src/task-reader.tsx,src/task-state.tsx`).
En `frontend/` hay 41 configs por feature más el default; **22 de ellos no los
ejecuta ningún target** (`stryker.authentication*`, `stryker.availability*`,
`stryker.complete-reopen-task*`, `stryker.create-task*`, `stryker.edit-project`,
`stryker.split-task*`, `stryker.project-states`, `stryker.appearance-replay`,
`stryker.history-replay`, `stryker.weekly-review-replay`,
`stryker.end-time-notification-replay`, `stryker.close-work-session.replay`,
`stryker.pause-resume-session.replay`, …).

Unión de los 19 alcanzables + el `--mutate` en línea: **53 ficheros**.

### 4.3 El dato incómodo: no son el mismo conjunto

Los dos conjuntos tienen el mismo tamaño (53) y **no se solapan del todo**:

- **19 ficheros están en `stryker.config.json` y en NINGÚN config por feature:**
  `api-client.ts`, `availability-api.ts`, `availability.tsx`,
  `edit-project-api.ts`, `navigation.tsx`, `project-status.ts`, `projects-api.ts`,
  `read-projects-api.ts`, `session-api.ts`, `task-history.tsx`, `task-parent.tsx`,
  `task-status-api.ts`, `task-validation.ts`, `tasks-api.ts`,
  `use-create-project.ts`, `use-edit-project.ts`, `use-project-status.ts`,
  `use-project-tasks.ts`, `use-read-projects.ts`.
- **19 ficheros están en algún config por feature y NO en `stryker.config.json`:**
  `appearance-api.ts`, `appearance-state.tsx`, `appearance.tsx`,
  `calendar-feed-api.ts`, `calendar.tsx`, `external-calendar-api.ts`,
  `external-calendar.tsx`, `import-data-api.ts`, `import-data-intent.ts`,
  `import-data.tsx`, `today-external-calendar.tsx`,
  `use-work-session-decision.ts`, `webhooks-client.ts`, `webhooks.tsx`,
  `work-session-end-api.ts`, `work-session-end.tsx`, `work-session-reader.tsx`,
  `work-session-state-api.ts`, `work-session-state.tsx`.
- Intersección: 34 ficheros. Unión de todo: **72 de 78**.
- **6 ficheros no los muta nadie, por ninguna vía:**
  `integrations-index.tsx`, `main.tsx`, `project-editor.tsx`,
  `project-status-control.tsx`, `test-setup.ts`, `today-fixture.ts`.

O sea: en el frontend **el objetivo vacío no es un superconjunto**. Sustituirlo
por la unión de los configs por feature perdería 19 ficheros —entre ellos
`api-client.ts`, que es el cliente HTTP común— y ganaría otros 19. Preservar «no
menos que hoy» exige la unión de ambos, 72 ficheros, no cualquiera de los dos
solos.

Aviso de rendimiento: `frontend/stryker.config.json` tiene `concurrency: 2`,
mientras que 14 de los 19 configs por feature usan `concurrency: 8`. Cambiar el
reparto sin tocar la concurrencia mueve el coste de forma no lineal.

---

## 5. Coste medido, con la cita textual

Todos los tiempos siguientes son de campañas **reales** anotadas en `progress/`,
sobre la máquina de desarrollo (Windows, 4 hilos PIT salvo `integration_api*`,
que usa 8: `backend/build.gradle.kts:669`), no sobre un runner de GitHub.

### 5.1 La trampa de lectura que hay que evitar

`progress/mutation_import_persistence.md:5` dice, textualmente:

> «Baseline válida en 142 segundos; Gradle terminó en **2 h 21 min 10 s**.»

Y `progress/judge_import_data.md:26` lo repite:

> «Persistencia original terminó con EXIT 0 en **2 h 21 min 10 s**: 316 KILLED,
> diez SURVIVED, uno NO_COVERAGE y tres TIMED_OUT.»

Son **8.470 s**, no 1.270. Un informe anterior lo leyó como «21 min 10 s» y
subestimó ese ámbito en **1 h 60 min**. Y es el ámbito más caro del repositorio
por un factor de casi dos sobre el siguiente, con **4 clases** de producción en
su lista. El coste de un ámbito no lo predice su tamaño: lo predice la fase de
cobertura, que levanta la suite entera (`targetTests` de casi todos los ámbitos
es `com.apptolast.organization.*`).

### 5.2 Tabla de tiempos reales por ámbito de backend

| # | ámbito | tiempo | s | cita |
|---|---|---|---|---|
| 1 | `import_data_persistence` | **2 h 21 min 10 s** | 8470 | `mutation_import_persistence.md:5` «Gradle terminó en 2 h 21 min 10 s» |
| 2 | `export_data_persistence` | 48 min 13 s | 2893 | `mutation_export_data_persistence.md:7` «BUILD SUCCESSFUL en 48m13s. Cobertura previa 340 segundos… mutación 42m17s» |
| 3 | `schedule_block` | 42 min 42 s | 2562 | `mutation_schedule_block_backend.md:27` «PIT: 42 min 35 s (45 s de cobertura y 41 min 49 s de análisis); Gradle: 42 min 42 s» |
| 4 | `end_time_notification` | 38 min 55 s | 2335 | `mutation_end_time_backend.md:5` «Duración Gradle38m55s; PIT informa38m46s, con35m59s de análisis… y2m46s de cobertura» |
| 5 | `close_work_session` | 35 min 28 s | 2128 | `review_close_work_mutation_backend.md:3` «BUILD SUCCESSFUL en35m28s (PIT35m21s, análisis32m44s, cobertura2m36s)» |
| 6 | `pause_resume_session` | 29 min 50 s | 1790 | `mutation_pause_resume_backend.md:17` «Duración PIT29m42s y Gradle29m50s» |
| 7 | `start_work_session` | 27 min 12 s | 1632 | `mutation_start_work_backend.md:24` «Gradle27m12s; PIT26m56s, de los que23m09s son análisis» |
| 8 | `reschedule` | 21 min 41 s | 1301 | `mutation_reschedule_backend.md:19` «PIT:1294segundos/21m34, incluyendo127s de cobertura… Gradle:21m41» |
| 9 | `history` | 21 min 41 s | 1301 | `mutation_history_backend.md:3` «Gradle 21 min 41 s» |
| 10 | `integration_api` | 21 min 13 s | 1273 | `mutation_integration_api_backend_final.md:16` «Cobertura: 547 s; mutación: 725 s; PIT total: 1273 s» |
| 11 | `webhooks` | 18 min 42 s | 1122 | `mutacion_webhooks_backend_medida.md:5` «árbol limpio, máquina drenada. 18 min 42 s» |
| 12 | `custom_views_fields` | 16 min 43 s | 1003 | `mutation_custom_views_fields_backend.md:3` «Gradle 16m43; PIT 16m36» |
| 13 | `complete_reopen_task` ✖ | 11 min 55 s | 715 | `mutation_complete_reopen_task_backend.md:11` «La campaña tardó 715 segundos, de los que 45 fueron cobertura y 669 análisis» |
| 14 | `weekly_review` | 10 min 51 s | 651 | `mutation_weekly_review_backend.md:5` «10m51s Gradle/10m45s PIT (cobertura2m59s, mutación7m44s)» |
| 15 | `ics_calendar` | 10 min 22 s | 622 | `mutation_ics_calendar_backend.md:17` «Ventana … 11:22:47Z → 11:33:10Z (10 min 22 s)» |
| 16 | `split_task` ✖ | 8 min 50 s | 530 | `mutation_split_task_backend.md:15` «Sesión 76051 EXIT 0 en 8 minutos y 50 segundos» |
| 17 | `start_work_session_replay` | 8 min 38 s | 518 | `mutation_start_work_backend_replay.md:15` «EXIT 0, 8 min 38 s de Gradle y 8 min 31 s de PIT» |
| 18 | `appearance` | 7 min 43 s | 463 | `mutation_appearance_backend.md:18` «BUILD SUCCESSFUL en7m43s (PIT7m34s: cobertura177s y mutación4m37s)» |
| 19 | `create_task` ✖ | 7 min 41 s | 461 | `mutation_create_task_backend.md:25` «El perfil completo terminó con salida 0 en 461 segundos» |
| 20 | `availability` ✖ | 1 min 38 s | 98 | `mutation_availability_backend.md:11` «PIT informó 98 segundos: 32 segundos de cobertura y 65 de análisis» |
| 21 | `today` | 58 s | 58 | `mutation_today_backend.md:17` «PIT finalizó en50s (11s cobertura,38s análisis), Gradle BUILD SUCCESSFUL58s» |

✖ = ámbito sin target en `scripts/project.mjs`.

**Suma de los 21 con tiempo anotado: 31.926 s = 8 h 52 min 6 s.**
**Suma de los 12 más caros: 27.810 s = 7 h 43 min 30 s** — ya por encima del techo
duro de 360 min de un job hospedado, con doce ámbitos y sin haber tocado el
frontend.

(Un informe anterior citó «12 ámbitos = 23.835 s = 6 h 37 min 15 s». Mi suma de
los doce mayores es 27.810 s. La diferencia probable es que aquel conjunto no
incluía `export_data_persistence` ni `close_work_session`. En cualquier caso la
conclusión no cambia de signo: no cabe, y no por poco.)

### 5.3 Ámbitos con nombre SIN tiempo anotado

`external_calendar`, `import_data_reader`, `import_data_http`,
`integration_api_http`, `authentication`. De los tres primeros:
`progress/import_pit_scope_plan.md` dice literalmente «Preparación aislada; **no
se ejecutó ninguna campaña**» y «Reader/HTTP esperan autorización de campaña». Es
decir, dos ámbitos declarados en Gradle y con target en el arnés que **nunca se
han lanzado**. Ninguna estimación de coste total puede darse por completa.

### 5.4 Frontend: tiempos reales

| campaña | tiempo | cita |
|---|---|---|
| `reschedule-frontend` | **79 min 3 s** | `mutation_reschedule_frontend.md:11` «Ejecución18743 finalizada179d24 EXIT0,79min3s… 1416 mutantes» |
| `custom_views_fields-frontend` (original) | 44 min 8 s | `mutation_custom_views_fields_frontend_original.md:5` «Duración 44 min 8 s; ocho workers, perTest» |
| `today-frontend` | 25 min 52 s | `mutation_today_frontend.md:17` «duración25min52s… 418 Killed /521 total» |
| `external_calendar-frontend` | 26 min 35 s | `mutacion_external_calendar_frontend_medida.md:26` «26 min 35 s» |
| `custom_views_fields-frontend` (refinada) | 21 min 10 s | `mutation_custom_views_fields_frontend_refined.md:3` «Duración21 min10 s, del02:31:58 al02:53:08» |
| `close_work_session-frontend` | 18 min 55 s | `review_close_work_mutation_frontend.md:5` «duración 18 min 55 s y dry run de 820 pruebas GREEN» |
| `darkmode/appearance` | 17 min 19 s | `mutation_darkmode_appearance.md:23` «inicio 12:47:32, fin 13:04:58… 17 min 19 s de mutación» |
| `complete_reopen_task-frontend` ✖ | 13 min 45 s | `mutation_complete_reopen_task_frontend.md:5` «EXIT 0 en 13 min 45 s» |
| `weekly_review-frontend` | 11 min 24 s | `mutation_weekly_review_frontend.md:15` «duración reportada 11 min 24 s» |
| `authentication-frontend` ✖ | 11 min 15 s | `mutation_authentication_frontend.md:15` «terminó con EXIT 0 en 11 min 15 s» |
| `ics_calendar-frontend` | 11 min 18 s | `mutation_ics_calendar_frontend.md:17` «11:34:29Z entrada, 11:45:49Z salida (11 min 18 s)» |
| `project_states-frontend` ✖ | 10 min 8 s | `mutation_project_states_frontend.md:3` «Duración: 10 min 8 s» |
| `export_data-frontend` | 7 min 27 s | `mutation_export_data_frontend.md:3` «EXIT 0, 7 min 27 s» |
| `schedule_block-frontend` | 63 min 1 s | `mutation_schedule_block_frontend.md:18` «11:17:07–12:20:08 local: 63 min 1 s» |

**No existe en `progress/` ninguna campaña de `frontend/stryker.config.json`
completa.** Los 53 ficheros del perfil por defecto, con `concurrency: 2`, nunca
se han medido de una pasada: en el run 34322164783 el frontend ni siquiera
arrancó, porque PIT se comió las cuatro horas antes. Cualquier plan que asuma un
coste para esa pasada está **estimando, no midiendo**, y debe decirlo.

Referencia útil para calibrar: `reschedule-frontend` produjo 1416 mutantes sobre
7 ficheros en 79 min con `concurrency: 8`. El perfil por defecto tiene 53
ficheros y `concurrency: 2`.

### 5.5 El coste fijo que domina, ya escrito en el repositorio

`progress/plan_campanas.md:66`:

> «PIT gasta **~17 minutos calculando cobertura** sea cual sea el ámbito, porque
> `targetTests` es la suite entera. Ese coste es el mismo para una feature que
> para cinco.»

Y `progress/plan_campanas.md:57`:

> «Las campañas van **UNA POR FEATURE**. Está probado que caben… Son ~35 min cada
> una y no hay forma de bajar de ahí.»

Las cifras de cobertura medidas confirman la magnitud y su varianza: 547 s
(`integration_api`), 340 s (`export_data_persistence`), 177 s (`appearance`),
142 s (`import_data_persistence`), 127 s (`reschedule`), 45 s (`schedule_block`),
11 s (`today`). En el run de CI 34322164783 fueron 470 s. **Trocear en N jobs
multiplica ese coste fijo por N.** Es el número que decide si el troceado sale a
cuenta, y hay que ponerlo en la mesa antes de elegir N.

---

## 6. Resumen ejecutable

| pregunta | respuesta medida |
|---|---|
| ¿Cuántas clases muta hoy el objetivo vacío? | **456** de 470 ficheros de producción |
| ¿Cuántas la unión de los ámbitos alcanzables? | **308** (67,5 %) |
| ¿Cuántas la unión de los 26 ámbitos con nombre? | **345** (75,7 %) |
| ¿Huérfanas con los targets de hoy? | **148** |
| ¿Huérfanas aun reactivando los 5 ámbitos muertos? | **111** |
| ¿Frontend por defecto? | 53 ficheros, `concurrency: 2` |
| ¿Frontend, unión por feature? | 53 ficheros, **19 distintos en cada dirección** |
| ¿Frontend, unión de todo? | 72 de 78 |
| ¿Coste medido, 21 ámbitos de backend? | **8 h 52 min 6 s** |
| ¿Coste medido, los 12 mayores? | **7 h 43 min 30 s** |
| ¿`noche_cinco`? | No existe en Gradle: es un alias del objetivo vacío |
