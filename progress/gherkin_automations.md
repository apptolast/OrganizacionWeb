# Destilación de automations (feature 30) — borrador revisable

Fuente de verdad: `progress/proposal_automations.md` (8 de septiembre de 2026). Dependencias leídas: `progress/proposal_webhooks.md` (25), `docs/gherkin.md`, `docs/agent-efficiency.md`, patrón de memoria «revisión adversarial del contrato antes de la puerta humana». Modelo de estilo: `features/custom_views_fields.feature` y `features/publish_outbox.feature`. Rol independiente `gherkin_author`: no se ha tocado `project-spec.md`, `feature_list.json`, `src/`, tests ni código. Este documento no aprueba TDD ni marca `spec_ready`; el coordinador decide el cambio de estado.

Archivo: `features/automations.feature`. **43 escenarios @s1–@s43**, 25 de ellos `Scenario Outline`, un solo `When` por escenario, sin `@approved`. Todas las tablas expanden un caso por fila; ninguna fila se presenta como ejecución realizada.

## Mapa @s → sección de la propuesta

| Sección de la propuesta | Escenarios |
| --- | --- |
| Modelo de regla: representación cerrada, propietario del principal, `name` 1–80 | @s1, @s8 |
| Catálogo cerrado de doce disparadores y `UNKNOWN_EVENT_TYPE` | @s2 |
| Condición `null` o `{ projectId }`; `TARGET_NOT_FOUND` ajeno/inexistente indistinguibles | @s3 |
| Acción `CREATE_TASK`: destino propio, `TARGET_NOT_FOUND`, minutos fijos opcionales | @s4, @s1, @s8 |
| Acción `NOTIFY_WEBHOOK`: endpoint propio y activo de 25, `ENDPOINT_NOT_FOUND` | @s5 |
| Plantillas: cuatro marcadores, texto plano, límites crudos 160/2000 | @s6, @s8 |
| `INVALID_TEMPLATE`: marcador desconocido, `{{` sin cierre, `task.title` en `Project*`, `errors[]` por campo | @s7 |
| JSON estricto y `VALIDATION_ERROR` por campo | @s8 |
| Máximo 20 reglas contando desactivadas, `RULE_LIMIT`, borrar libera cupo | @s9, @s10, @s14 |
| `GET` lista (orden `createdAt, id`, máx. 20) y `GET {id}` con ETag, 404 idéntico | @s11 |
| `PUT` completo para activar/desactivar, `version + 1` siempre, runs conservados | @s12 |
| `If-Match`: 428, `VALIDATION_ERROR`, 412 `AUTOMATION_CONFLICT` en PUT y DELETE | @s13 |
| `DELETE` 204, `ON DELETE SET NULL`, tareas conservadas, runs no consultables | @s14 |
| Worker `app.automations.enabled` por defecto `false`; al habilitar continúa | @s15 |
| Cursor inicial en el presente al crear la primera regla | @s16 |
| Orden `(occurred_at, event_id)`, omisión de `blocked`, commit tardío no procesado | @s17 |
| Resolución de plantilla con salida exacta y valores vigentes | @s18 |
| `CREATE_TASK` por el caso de uso existente, `TaskCreated.v1`, atomicidad run+tarea+evento+cursor, logs sin datos | @s19 |
| Fallo de almacenamiento: rollback, fila `retry`, cursor no avanza | @s20 |
| Fallos deterministas `failed` en el primer intento sin detener otras reglas | @s21 |
| Reintentos antes que eventos nuevos, máximo 3 intentos, tercero `failed` | @s22 |
| `UNIQUE (rule_id, event_id)` con dos workers concurrentes | @s23 |
| Reglas desactivadas no ejecutan, cursor avanza, reactivar no recupera, `FOR SHARE` sin mezcla | @s24 |
| Recuperación tras caída/reinicio sin duplicar | @s25 |
| `NOTIFY_WEBHOOK`: entrega de 25 con el payload original, `delivery_id`, misma transacción | @s26 |
| Resolución del proyecto del evento por tipo; aislamiento del cursor por propietario | @s27 |
| Guarda de bucles por `created_task_id`, también con la regla borrada | @s28 |
| Profundidad 1: acciones humanas posteriores sí disparan | @s29 |
| Simulación sin efectos, respuesta cerrada, preview exacta, regla no persistida | @s30 |
| Simulación: últimos 100 eventos, exclusión de `blocked`, `matches: []` | @s31 |
| Simulación: `wouldFail`, `loopGuarded`, preview de `NOTIFY_WEBHOOK` | @s32 |
| Simulación: mismos errores que crear, no consume cupo | @s33 |
| Auditoría: `{ items, nextCursor }`, 20 por página, campos cerrados | @s34 |
| Auditoría: cursor ajeno o mal formado, 404 idéntico, aislamiento | @s35 |
| Seguridad: 401, CSRF, Origin, 415, 405, 503, no-store | @s36 |
| Aislamiento por propietario en lectura, ejecución y auditoría | @s11, @s27, @s35 |
| UI: lista, disparador legible, estados vacío/cargando/error, navegación | @s37 |
| UI: editor con vista previa, ayuda de marcadores, errores por campo, sin `localStorage` | @s38 |
| UI: botón «Simular» con resultados anunciados, sin guardar | @s39 |
| UI: «Guardar» deshabilitado, 412 explicado con recarga deliberada, interruptor confirmado | @s40 |
| UI: historial «Cargar más», estado textual, enlace a la tarea | @s41 |
| UI: responsive 320/768/1280/1440, texto 200 %, teclado, foco, 44 px, axe | @s42 |
| UI: cancelación al navegar, cambiar de regla y cerrar sesión | @s43 |

## Dependencias con la feature 25 (webhooks)

1. **Endpoints.** @s5, @s12, @s21, @s26, @s32 y @s33 requieren `webhook_endpoints` con estados `active`/`disabled` y propietario. Sin 25 implementada, el TDD usa un doble del puerto de consulta de endpoints; el escenario no se declara verde si el doble es el único que existe en producción.
2. **Entregas.** @s26 exige insertar una fila de `webhook_deliveries` (`status pending`, `attempt 0`, `body` igual al payload de la outbox) dentro de la transacción del evento. La propuesta de 25 sólo admite «una entrega de outbox en vuelo por endpoint» para su propio encolado; una entrega insertada por 30 no debe bloquear ni ser bloqueada por ese límite. Queda como duda para 25 (abajo).
3. **Lectura de la outbox por cursor.** La propuesta de 30 asume un puerto genérico `EventTail`/`OwnerEventCursor` de 25. La propuesta de 25 **no define ese puerto**: guarda el cursor dentro de `webhook_endpoints` y consulta `outbox_events` directamente con `(occurred_at, event_id) > cursor` sobre el índice `outbox_events_owner_cursor`. Consecuencia para 30: V28 crea su propia tabla `automation_cursors (owner_id PK, occurred_at, event_id)` y reutiliza sólo el índice y la semántica de tupla. No hay tabla de cursores por consumidor en V23 (las migraciones actuales llegan a V21).
4. **Ventana de gracia.** 25 aplica `occurred_at <= now - 5 s` antes de encolar. La propuesta de 30 no la menciona. @s17 fija «commit tardío con `occurred_at` anterior al cursor no se procesa» sin fijar los 5 s; si 30 adopta la misma ventana, @s17 sigue siendo cierto.
5. **Catálogo de tipos.** Ambas comparten los doce nombres literales de `OutboxMessage.validationCode()`; `webhook.ping.v1` no es disparador (@s2).

## Huecos detectados y cómo se resolvieron

1. **Proyecto `completed` al guardar la regla.** La propuesta define `PROJECT_COMPLETED` sólo como fallo de ejecución y no dice qué ocurre al guardar. Resuelto en @s4: se acepta guardar (201) porque el proyecto puede reabrirse; la ejecución falla `failed` con `PROJECT_COMPLETED` (@s21) y la simulación lo anticipa (@s32). Menos validación y una sola fuente de verdad en el momento de ejecutar.
2. **Cupo concurrente.** La propuesta fija el límite de 20 pero no la serialización; 23/24/25 usan `pg_advisory_xact_lock` por propietario. Añadido @s10: dos creaciones concurrentes por la última plaza producen exactamente un 201 y un 409, sin exigir el mecanismo.
3. **Subcódigos de `INVALID_TEMPLATE`.** La propuesta exige `errors[]` que señale el campo, sin nombrar el motivo. @s7 fija tres motivos (`UNKNOWN_PLACEHOLDER`, `UNCLOSED_PLACEHOLDER`, `PLACEHOLDER_NOT_AVAILABLE`) y decide que `{{ task.title }}` con espacios interiores es desconocido: la comparación es literal, sin recorte, coherente con «sin plantillas con lógica».
4. **Guarda de bucles con la regla borrada.** La propuesta la justifica por `ON DELETE SET NULL` pero no la escenifica. @s28 añade la fila «R1 fue borrada antes de leer el evento de T» y @s14 exige que las filas de ejecución conserven `createdTaskId` con regla nula.
5. **Cursor propio frente a puerto de 25.** Ver dependencia 3. El contrato no nombra la tabla (sin detalles de implementación), pero @s16, @s17, @s24 y @s27 fijan la semántica observable: cursor por propietario, inicializado en el presente, avanza aunque no haya coincidencias y no se mueve por eventos de otro propietario.

## Dudas para el coordinador o el humano

- **Canal Bearer de 24.** La propuesta de 30 admite «sesión o credencial de 24 con alcance de escritura»; la de 25 excluye el canal Bearer. @s36 sólo fija sesión, CSRF y Origin. Si 30 debe aceptar Bearer, hace falta un escenario adicional con los códigos de 24.
- **Límite de «una entrega en vuelo por endpoint» de 25** frente a las entregas insertadas por `NOTIFY_WEBHOOK` (dependencia 2). Propuesta: el límite de 25 aplica sólo a su propio encolado desde la outbox; las entregas de 30 se procesan por el mismo worker de entrega sin ese filtro.
- **Anchos responsive.** La tarea pide 320/768/1280 y la propuesta 320/768/1440. @s42 cubre los cuatro más texto al 200 %; el coordinador puede recortar filas.
- **Entrada de navegación.** La propuesta la sitúa «tras Integraciones». Hoy `App.tsx` sólo tiene «Hoy» y «Proyectos»; 24/25 aún no existen. @s37 exige la entrada «Automatizaciones» sin desplazar «Hoy» y no fija la posición relativa a entradas no implementadas.
- **`errors[]` de `INVALID_TEMPLATE`** cuando un mismo campo tiene dos defectos: @s7 exige exactamente un error por campo (el primero encontrado); si se prefiere listar todos, cambia la última fila.

Pendiente revisión adversarial y del coordinador antes de la puerta humana. No activar `in_progress` ni iniciar TDD desde esta entrega.

## Decisiones del coordinador (8 de septiembre de 2026, 20:55)

- Canal: sólo sesión con CSRF y Origin, como fija @s36; el canal Bearer de 24 no se admite en 30 (coherente con 25).
- El límite «una entrega en vuelo por endpoint» de 25 aplica sólo a su encolado desde la outbox; las entregas insertadas por `NOTIFY_WEBHOOK` las procesa el mismo entregador sin ese filtro (fase 2).
- Anchos: se conservan 320/768/1280/1440 y texto al 200 % en @s42.
- Navegación: entrada «Automatizaciones» sin desplazar «Hoy»; posición relativa a 24/25 se fija al integrar.
- `errors[]` de `INVALID_TEMPLATE`: exactamente un error por campo, el primero encontrado.
- Ejecución por fases: fase 1 sin worker ni `NOTIFY_WEBHOOK` reales (stub de búsqueda de endpoint); fase 2 tras integrar 25.
