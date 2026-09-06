@reschedule @approved
Feature: Mover y cancelar reservas propias conservando sus hechos anteriores
  Contrato aprobado bajo autorización global vigente, tras revisión del coordinador e independiente backend.
  Fuente: project-spec.md sección13, review_reschedule_spec.md y review_reschedule_contract_backend.md.
  B = /api/v1/projects/{projectId}/tasks/{taskId}/blocks.
  Preview = POST B/{blockId}/reschedule/preview; mover = POST B/{blockId}/reschedule; cancelar = POST B/{blockId}/cancel.
  Estado = GET B/{blockId}/state; recibos = GET B/changes/{id} y GET B/changes/by-request/{requestKey}.
  Block9 conserva exactamente id, projectId, taskId, objective, startAt, endAt, zoneId, durationMinutes, createdAt.
  Estado contiene exactamente block, status y updatedAt; status es planned o cancelled.
  Recibo contiene exactamente id, blockId, kind, revision, occurredAt, before, after.
  IDs son UUID canónicos; kind es RESCHEDULED o CANCELLED; revision es ETag fuerte de bloque como texto.
  ETag tiene formato entre comillas block:UUID:versión; versión decimal canónica positiva hasta9223372036854775807.
  before es Block9 y after es Block9 para movimiento o null exclusivamente para cancelación.
  Preview recibe exactamente startLocal, endLocal, zoneId, startOffset, endOffset; mover añade allowOverBudget booleano.
  Preview admite offsets null sólo para resolución inequívoca; confirmar movimiento exige offsets explícitos.
  Cancelar recibe exactamente {}; no acepta cambios de objetivo, identidad, tarea o proyecto.
  Preview exige If-Match; mover además Availability-Revision e Idempotency-Key; cancelar sólo If-Match e Idempotency-Key.
  Preview devuelve los diez campos de schedule_block.feature @s1 y days con sus cinco campos cerrados; ETag de bloque es header.
  Se heredan formatos locales/offset/UUID, años1–9999, segundos enteros, duración real1–1440 minutos enteros,
  catálogo/DST y validOffsets de schedule_block.feature @s4–s10; presupuesto de @s11–s16 y elegibilidad @s17.
  Se heredan seguridad/origen/CSRF, problem+json, no-store y JSON ilegible de @s19, @s23 y @s62.
  Se heredan validación de DTO9 e intención de @s40/@s46; no se amplía DTO9 ni Today15 de today.feature.
  Privacidad UI de11@s52–s53 se conserva:401 actual retira datos inmediatamente y recuperación persiste ante inelegibilidad.
  La octava ruta conserva confirmación, retries y blocked de11@s37, sin exigir orden de entrega.
  Las excepciones explícitas aquí cambian lectura vigente y excluyen cancelados; creación/by-request siguen históricos.
  Precedencia: seguridad; query; IDs; If-Match; Availability-Revision aplicable; key; estructura;
  propiedad y existencia del bloque; replay; revisión; estado; agotamiento; disponibilidad/revisión;
  elegibilidad; tiempo; ausencia de cambio; solape; presupuesto. Sin orden entre errores estructurales del cuerpo.
  Rechazos y GET/preview no escriben entidades, revisión, recibos ni outbox.
  Replay compara normalización sintáctica estable sin consultar catálogo, DST, presupuesto o reloj; zoneId no se aliasa.
  Cursor de cambios: base64url canónico de JSON cerrado collection=blockChanges,projectId,taskId,occurredAt,id.
  Historial ordena occurredAt/id DESC con lookahead21; el cursor continúa sólo si quedan más elementos.
  Escrituras usan READ_COMMITTED y orden proyecto SHARE, tarea SHARE, disponibilidad UPDATE, bloque UPDATE.
  Preview toma disponibilidad y bloque SHARE; Today conserva REPEATABLE_READ read-only de feature12.
  Cancelar bloquea preferencia si existe sin resolver zona; si falta, bloquea el bloque sin crear preferencia.
  La fila original garantiza exclusión aunque aún no haya proyección; tras esperar se reconsulta replay antes del negocio.
  Proyección opcional y recibos de cambios/historial se confirman con outbox; la creación no se copia ni se reescribe.
  Cada fila de Examples es independiente; lo no indicado es válido y los relojes están controlados.

  Background:
    Given sesión válida de persona-a y origen y CSRF válidos para POST
    And proyecto propio P active con tarea propia T pending
    And disponibilidad propia UTC con120 minutos diarios y revisión vigente
    And reloj del servidor "2030-01-07T09:00:00Z"
    And bloque propio A planned revisión1, objetivo "Preparar borrador", intervalo UTC10:00–11:00 del2030-01-07
    And A tiene createdAt "2030-01-06T10:00:00Z" y recibo de creación original durable
    And no hay otras reservas ni cambios salvo los indicados
    And propuesta base mueve A a UTC12:00–13:00 del2030-01-07 con offsets Z y allowOverBudget false
    And If-Match de A y demás headers exigibles son válidos y actuales con key nueva

  @s1
  Scenario: Leer bloques anteriores a la migración sin inventar hechos
    Given A fue creado antes de habilitar replanificación y no tiene cambios
    When consulto el estado de A
    Then recibo200 con Block9 original, status planned y updatedAt igual a createdAt
    And ETag corresponde a revisión1 de A y el historial de cambios permanece vacío
    And GET no crea proyección, copia de recibo, preferencia, historial ni outbox
    And la migración conserva las creaciones existentes y sus peticiones normalizadas sin UPDATE ni DELETE

  @s2
  Scenario Outline: Recuperar la creación original después de cambiar su proyección
    Given A fue <cambio> después de su creación confirmada
    When <recuperacion>
    Then recibo exactamente el Block9 originalmente confirmado con status y Location heredados de feature11
    And no reinterpreto su zona ni comparo su inicio con el reloj actual
    And no se revierte la proyección ni se genera otro evento
    Examples:
      | cambio | recuperacion |
      | movido dos veces | repito POST de creación con su key e intención original |
      | cancelado | consulto GET B/by-request con su key de creación |

  @s3
  Scenario Outline: Leer sólo reservas vigentes sin perder acceso a canceladas
    Given A fue <cambio>
    When consulto <lectura>
    Then obtengo <resultado>
    And cuando hay DTO, Block9 conserva id, projectId, taskId, objective y createdAt sin añadir estado o revisión
    Examples:
      | cambio | lectura | resultado |
      | movido | GET B | A una vez con intervalo nuevo, orden createdAt/id descendente y paginación20 heredada |
      | cancelado | GET B | items vacío y nextCursor null |
      | movido | GET B/A | 200 con intervalo nuevo |
      | cancelado | GET B/A | 404 BLOCK_NOT_FOUND |
      | cancelado | GET B/A/state | 200 cancelled con último Block9 y ETag resultante |

  @s4
  Scenario: Mantener revisión textual exacta y esquema de estado cerrado
    Given A tiene revisión9007199254740993 y updatedAt "2030-01-07T08:59:59.123456Z"
    When consulto su estado desde el cliente
    Then acepto exactamente block, status y updatedAt con Block9 válido y ETag fuerte de A versión9007199254740993
    And conservo la revisión como texto sin redondeo por Number ni campo version en Block9
    And la acción posterior usa exactamente ese If-Match

  @s5
  Scenario Outline: Exigir revisión del bloque antes de los demás headers
    Given If-Match <valor> y faltan los demás headers de mover
    When envío mover con cuerpo válido
    Then recibo <resultado>
    Examples:
      | valor | resultado |
      | ausente | 428 PRECONDITION_REQUIRED |
      | débil | 400 VALIDATION_ERROR en If-Match |
      | comodín | 400 VALIDATION_ERROR en If-Match |
      | lista o repetido | 400 VALIDATION_ERROR en If-Match |
      | pertenece a otro blockId | 400 VALIDATION_ERROR en If-Match |
      | versión0 o decimal con cero inicial | 400 VALIDATION_ERROR en If-Match |
      | versión9223372036854775808 | 400 VALIDATION_ERROR en If-Match |

  @s6
  Scenario Outline: Resolver revisión, cancelación y agotamiento antes del negocio
    Given <condicion>
    When envío <operacion> nueva con estructura válida
    Then recibo <resultado>
    Examples:
      | condicion | operacion | resultado |
      | A cancelled revisión2 e If-Match1 | mover | 412 BLOCK_CONFLICT |
      | A cancelled revisión2 e If-Match2 | cancelar | 409 BLOCK_CANCELLED |
      | A cancelled revisión2 e If-Match2 | preview | 409 BLOCK_CANCELLED |
      | A planned revisión máxima y disponibilidad ausente | preview | 409 BLOCK_VERSION_EXHAUSTED |
      | A planned revisión máxima y disponibilidad ausente | cancelar | 409 BLOCK_VERSION_EXHAUSTED |
      | revisión actual y disponibilidad ausente y proyecto completed | mover | 409 AVAILABILITY_REQUIRED |
      | revisión actual y Availability-Revision obsoleta y proyecto completed | mover | 412 AVAILABILITY_CONFLICT |
      | revisión actual y preferencia válida y proyecto y tarea completed | mover | 409 PROJECT_COMPLETED |
      | revisión actual y preferencia válida y sólo tarea completed | mover | 409 TASK_COMPLETED |

  @s7
  Scenario: Revisar el destino excluyendo únicamente la identidad movida
    Given otra reserva propia de otra tarea completed ocupa UTC14:00–14:30
    And la propuesta mueve A a UTC10:30–11:30
    When solicito preview sin key ni Availability-Revision
    Then recibo200 con objective "Preparar borrador", destino UTC10:30–11:30 y duración60
    And ETag corresponde a la revisión vigente de A y availabilityEtag a la preferencia vigente
    And days muestra lunes con budgetMinutes120, plannedSeconds1800, requestedSeconds3600 y excessSeconds0
    And el intervalo anterior de A no solapa consigo mismo ni se elimina la reserva de otra tarea

  @s8
  Scenario Outline: Reutilizar límites temporales al revisar y confirmar un destino
    Given <destino>
    When envío <operacion>
    Then obtengo <resultado> conforme a los formatos y errores de schedule_block.feature @s4–s10
    Examples:
      | destino | operacion | resultado |
      | duración real1 minuto e inicio igual al reloj | preview | 200 y durationMinutes1 |
      | duración real1440 minutos con dos días presupuestarios | preview | 200 y durationMinutes1440 |
      | duración real1441 minutos con extremos locales UTC válidos | mover | 400 VALIDATION_ERROR endLocal OUT_OF_RANGE |
      | fecha local con segundos no admitidos o año fuera1–9999 | preview | 400 VALIDATION_ERROR temporal |
      | inicio Europe/Madrid 2030-03-31T02:30 y fin03:30, offsets null | preview | 400 VALIDATION_ERROR startLocal NONEXISTENT_LOCAL_TIME |
      | inicio Europe/Madrid 2030-10-27T02:30 y fin03:30, offsets null | preview | 400 VALIDATION_ERROR startOffset AMBIGUOUS_OFFSET con validOffsets startOffset [+02:00,+01:00] |
      | Europe/Madrid 2030-10-27T02:45 +02:00 a02:15 +01:00 | preview | 200 con startAt2030-10-27T00:45:00Z, endAt2030-10-27T01:15:00Z y durationMinutes30 |
      | preview válido pero inicio ya pasó al confirmar | mover | 400 VALIDATION_ERROR startLocal IN_PAST |

  @s9
  Scenario: Mover entre días libera el intervalo anterior y cuenta el destino real
    Given A ocupa UTC23:30 del lunes a00:30 del martes
    And hay otra reserva propia el martes de30 minutos y una reserva ajena de60 minutos
    And el destino es martes UTC12:00–13:00
    When confirmo mover A
    Then recibo201 con una revisión adicional y un solo recibo y evento
    And el presupuesto planned del lunes deja de incluir los1800 segundos originales de A
    And el martes suma5400 segundos propios sin reserva ajena ni media hora original de A
    And Today muestra el nuevo intervalo en su siguiente lectura con los quince campos originales

  @s10
  Scenario Outline: Distinguir ausencia de cambio de error temporal y cambio de zona
    Given <propuesta>
    When envío preview
    Then obtengo <resultado>
    Examples:
      | propuesta | resultado |
      | mismos instantes futuros y misma zona de A | 409 BLOCK_UNCHANGED |
      | mismos instantes de A pero su inicio ya pasó | 400 VALIDATION_ERROR startLocal IN_PAST antes de BLOCK_UNCHANGED |
      | mismos instantes futuros y distinta zona resoluble con horas y offsets correspondientes | 200 con nueva zona |

  @s11
  Scenario Outline: Confirmar un movimiento efectivo sin alterar identidad ni trabajo
    Given el destino <cambio>
    When confirmo mover A
    Then recibo201 con Location B/changes/{id} y recibo RESCHEDULED de revisión2
    And before es Block9 anterior y after cambia sólo instantes, zona y duración sobre la identidad original
    And no cambian versiones, estados o fechas de proyecto, tarea ni disponibilidad
    And no se acredita trabajo ni se completa tarea o proyecto
    Examples:
      | cambio |
      | cambia inicio y fin manteniendo duración |
      | cambia duración a90 minutos |
      | conserva instantes pero cambia zoneId |

  @s12
  Scenario Outline: Cancelar deliberadamente reservas históricas o no elegibles
    Given <condicion>
    When confirmo cancelar A con cuerpo vacío, If-Match actual y key nueva sin Availability-Revision
    Then recibo201 con recibo CANCELLED, before igual al último Block9 y after null
    And A pasa a cancelled con una revisión adicional y deja de ocupar capacidad y solapes
    And historial y creación siguen recuperables y estados de proyecto y tarea no cambian
    Examples:
      | condicion |
      | A empieza en el futuro |
      | A terminó ayer |
      | proyecto y tarea completed |
      | no existe disponibilidad |
      | zona guardada de disponibilidad ya no se resuelve |

  @s13
  Scenario: Registrar un único instante aunque el reloj retroceda
    Given A fue movido con updatedAt "2030-01-07T09:05:00Z"
    And el reloj de cancelación devuelve "2030-01-07T09:04:00.123456789Z"
    When confirmo cancelar A con su revisión actual
    Then recibo revisión3, occurredAt "2030-01-07T09:04:00.123456Z" y estado updatedAt idéntico
    And el recibo tiene exactamente sus siete campos, UUID generado por servidor y ninguna key ni owner
    And el cliente no lo rechaza por occurredAt anterior al updatedAt previo
    And revisión conserva causalidad mientras historial ordena las fechas registradas

  @s14
  Scenario Outline: Vincular key de cambios a intención y tarea sin mezclar creaciones
    Given <antecedente>
    When envío <peticion>
    Then obtengo <resultado>
    Examples:
      | antecedente | peticion | resultado |
      | keyK confirmó mover A | mismo mover A con otro cuerpo normalizado | 409 IDEMPOTENCY_CONFLICT |
      | keyK confirmó mover A | cancelar A con keyK | 409 IDEMPOTENCY_CONFLICT |
      | keyK confirmó mover A | mover otro bloque existente de T con keyK | 409 IDEMPOTENCY_CONFLICT sin recibo ajeno |
      | keyK confirmó mover A | mover bloque inexistente de T con keyK | 404 BLOCK_NOT_FOUND |
      | keyK sólo existe en creación11 | mover A con keyK | 201 con recibo nuevo de cambio |
      | keyK confirmó mover A en T | mover bloque propio de otra tarea con keyK | 201 con recibo independiente |

  @s15
  Scenario Outline: Reproducir cambio confirmado antes de revalidar negocio
    Given <tipo> de A confirmado con keyK y cuerpo normalizado retenido
    And después <condicion>
    When reenvío esa misma intención con keyK y headers sintácticamente válidos aunque obsoletos
    Then recibo200 con mismo recibo y Location originales byte a byte
    And no cambia la proyección vigente ni se añade recibo o outbox
    Examples:
      | tipo | condicion |
      | movimiento | A se movió otra vez y luego se canceló |
      | movimiento | tarea y proyecto pasaron a completed |
      | movimiento | disponibilidad cambió o fue retirada |
      | movimiento | catálogo ya no reconoce la zona textual original |
      | movimiento | inicio original ya pasó |
      | movimiento | revisión vigente alcanzó el máximo |
      | cancelación | A sigue cancelled y la tarea pasó a completed |

  @s16
  Scenario Outline: Paginar hechos de cambio sin fabricar historial de creación
    Given T tiene <cantidad> cambios propios con empates de occurredAt y IDs distintos
    When consulto GET B/changes sin cursor
    Then recibo200 con exactamente items y nextCursor, <items> recibos ordenados occurredAt DESC e id DESC
    And nextCursor es <cursor>
    And no se fabrica cambio de creación ni se exige occurredAt creciente con revisión
    Examples:
      | cantidad | items | cursor |
      | 0 | 0 | null |
      | 20 | 20 | null |
      | 21 | 20 | continuación desde último recibo servido, cuya página siguiente contiene sólo el restante |

  @s17
  Scenario Outline: Rechazar cursores incompatibles de historial
    Given <cursor>
    When consulto GET B/changes
    Then recibo400 VALIDATION_ERROR sin recibos
    Examples:
      | cursor |
      | base64url no canónico o JSON con campo adicional |
      | collection distinta de blockChanges |
      | projectId o taskId de otro contexto |
      | id no UUID canónico o occurredAt fuera de UTC/microsegundos/rango heredado |
      | query desconocida o cursor repetido |

  @s18
  Scenario Outline: Separar ausencia de contexto, bloque y recibo sin divulgar propiedad
    Given <condicion>
    When consulto <ruta>
    Then recibo <resultado> con no-store y sin datos privados de otro contexto
    Examples:
      | condicion | ruta | resultado |
      | proyecto ajeno o tarea ajena/inexistente | estado o historial o recibo por key | 404 RESOURCE_NOT_FOUND |
      | contexto propio y blockId inexistente | estado | 404 BLOCK_NOT_FOUND |
      | contexto propio y changeId inexistente | changes/{id} | 404 BLOCK_CHANGE_NOT_FOUND |
      | contexto propio y key inexistente | changes/by-request/{requestKey} | 404 BLOCK_CHANGE_NOT_FOUND |
      | contexto propio y A cancelado con recibo durable | changes/{id} | 200 con recibo histórico completo |
      | fallo de almacenamiento o fin de transacción read-only | estado o historial | 503 STORAGE_UNAVAILABLE sin detalles internos |

  @s19
  Scenario Outline: Mantener seguridad y estructura antes del replay
    Given existe un recibo confirmado y <defecto>
    When envío <operacion> con su key original si corresponde
    Then recibo <resultado> sin ejecutar cambio ni devolver ese recibo
    Examples:
      | defecto | operacion | resultado |
      | sesión ausente | mover | 401 según seguridad heredada |
      | origen o CSRF inválido | cancelar | 403 según seguridad heredada |
      | query no permitida e If-Match ausente | mover | 400 VALIDATION_ERROR de query |
      | If-Match válido y Availability-Revision ausente | mover | 428 PRECONDITION_REQUIRED |
      | If-Match válido y key ausente | cancelar | 400 VALIDATION_ERROR de Idempotency-Key |
      | cuerpo ilegible | mover | 400 MALFORMED_JSON con forma compartida de11@s62 |
      | objective extra o allowOverBudget no booleano | mover | 400 VALIDATION_ERROR de estructura |
      | cuerpo no vacío o raíz array | cancelar | 400 VALIDATION_ERROR de estructura |
      | fecha local u offset sintácticamente no canónicos | mover | 400 VALIDATION_ERROR antes de replay |

  @s20
  Scenario Outline: Confirmar proyección, revisión, recibo y outbox atómicamente
    Given almacenamiento real y <fallo>
    When intento un cambio nuevo válido
    Then recibo503 STORAGE_UNAVAILABLE sin SQL, trazas ni secretos
    And proyección, revisión, recibo y outbox quedan exactamente como antes
    And no queda key confirmada recuperable y planned_blocks original permanece intacto
    Examples:
      | fallo |
      | falla o se suprime la escritura de proyección |
      | falla o se suprime la escritura de recibo |
      | falla o se suprime la escritura de outbox |
      | se rechaza el commit antes de confirmarlo |

  @s21
  Scenario Outline: Serializar cambios simultáneos del mismo bloque
    Given dos peticiones propias sincronizadas sobre revisión1 de A: <pareja>
    When compiten ambas confirmaciones
    Then <resultado>
    And existe exactamente un cambio efectivo, revisión2 y un evento, sin alterar creación original
    Examples:
      | pareja | resultado |
      | mover idéntico con misma key | una recibe201 y otra200 con recibo idéntico |
      | mover distinto con keys nuevas | una recibe201 y otra412 BLOCK_CONFLICT |
      | mover y cancelar con keys nuevas, mover confirma primero | mover201 y cancelar412 BLOCK_CONFLICT |
      | mover y cancelar con keys nuevas, cancelar confirma primero | cancelar201 y mover412 BLOCK_CONFLICT |
      | cancelar y cancelar con keys nuevas | una recibe201 y otra412 BLOCK_CONFLICT |
      | misma key con mover y cancelar distintos | una recibe201 y otra409 IDEMPOTENCY_CONFLICT |

  @s22
  Scenario Outline: Compartir capacidad y solapes entre reservas propias
    Given bloques distintos de proyectos distintos del mismo propietario con <condicion>
    When <orden>
    Then <resultado>
    And ningún éxito deja reservas planned solapadas y owners distintos pueden confirmar independientemente
    Examples:
      | condicion | orden | resultado |
      | movimiento y creación quieren el mismo destino libre | movimiento confirma primero y creación evalúa después | creación409 BLOCK_OVERLAP |
      | movimiento y creación quieren el mismo destino libre | creación confirma primero y movimiento evalúa después | movimiento409 BLOCK_OVERLAP |
      | cancelación libera un destino | cancelo antes de nueva creación | creación puede confirmar sin contar cancelada |
      | movimiento termina donde empieza otra reserva | confirmo movimiento contiguo | 201 sin solape |
      | destino excede presupuesto pero no solapa | confirmo con allowOverBudget false | 409 BUDGET_EXCEEDED con días actuales |
      | destino excede presupuesto pero no solapa | confirmo con allowOverBudget true | 201 sin contar dos veces intervalo previo |
      | destino solapa aunque permita exceso | confirmo con allowOverBudget true | 409 BLOCK_OVERLAP con conflicto determinista de11@s14 |
      | dos movimientos a destinos distintos no solapados, allowOverBudget false y sólo cabe uno en presupuesto | ambos compiten y uno confirma primero | primero201 y segundo409 BUDGET_EXCEEDED recalculado, sin sumar intervalo previo del bloque evaluado |

  @s23
  Scenario Outline: Coordinar cambios con estados y preferencia en ambos órdenes
    Given movimiento de A y <otro> concurrentes con revisiones válidas al empezar
    When <primero> confirma antes de evaluar la otra operación
    Then <resultado>
    And no hay espera circular ni cambios parciales
    Examples:
      | otro | primero | resultado |
      | completar proyecto | completar proyecto | movimiento409 PROJECT_COMPLETED |
      | completar proyecto | movimiento | movimiento201 y completar conserva reserva |
      | completar tarea | completar tarea | movimiento409 TASK_COMPLETED |
      | completar tarea | movimiento | movimiento201 y completar conserva reserva |
      | actualizar disponibilidad | actualizar disponibilidad | movimiento412 AVAILABILITY_CONFLICT |
      | actualizar disponibilidad | movimiento | movimiento201 y preferencia posterior no reescribe instantes |

  @s24
  Scenario: Mantener snapshots coherentes durante movimiento y cancelación
    Given un escritor mueve o cancela A entre lecturas internas de una consulta
    When ejecuto preview de otra reserva o GET Today
    Then observo íntegramente el estado anterior o posterior, nunca mezcla de intervalo, cancelación o preferencia
    And Today conserva nombres actuales y quince campos, sin cargar todo el historial ni escribir
    And GET Today no bloquea para escribir y preview coordina las reservas de su presupuesto

  @s25
  Scenario: Recuperar cambio confirmado tras perder ACK y reiniciar backend
    Given el cambio confirmó proyección, recibo y evento pero su respuesta se perdió
    And backend se reinició conservando PostgreSQL y después A volvió a cambiar
    When consulto el recibo por su key original
    Then recibo200 con hecho original completo y revisión aunque el estado vigente sea posterior
    And no se escribe ni repite evento y el recibo no depende de retención o publicación del outbox

  @s26
  Scenario Outline: Publicar un cambio sin objetivo ni datos de recuperación
    Given cambio efectivo <kind> confirmado
    When el publicador procesa su outbox
    Then envía BlockChanged.v1 por block.changed.v1 a cola quorum durable organization.block-changed.v1
    And payload contiene exactamente eventId, aggregateId, ownerId, occurredAt, schemaVersion, type, changeId, blockId, taskId, kind, revision, before, after
    And schemaVersion es1, aggregateId es proyecto, revision es BIGINT positivo y eventId distinto de changeId
    And before contiene sólo startAt,endAt,zoneId,durationMinutes y after es <after>
    And occurredAt coincide con recibo y updatedAt, sin objective, key, fechas locales ni nombres
    And las siete rutas anteriores conservan tipo, payload y garantías de entrega
    Examples:
      | kind | after |
      | RESCHEDULED | esos cuatro campos del nuevo intervalo |
      | CANCELLED | null |

  @s27
  Scenario Outline: Validar el evento y conservar recuperación del broker
    Given el publicador recibe <situacion>
    When intenta publicar BlockChanged.v1
    Then <resultado>
    And no consulta catálogo histórico ni cambia identidad/revisión para simular orden de entrega
    Examples:
      | situacion | resultado |
      | evento válido con zona histórica no disponible | publica bytes originales y confirma según protocolo heredado |
      | campo extra, kind desconocido o revision inválida | blocked por payload inválido sin enviar |
      | RESCHEDULED con after null o CANCELLED con after objeto | blocked por payload inválido sin enviar |
      | instante o duración incoherente en before/after | blocked por payload inválido sin enviar |
      | broker caído, nack o resultado incierto | reintenta misma identidad según política heredada sin afirmar entrega única |
      | reinicio tras envío no confirmado | recupera y puede redeliver con mismo eventId |

  @s28
  Scenario: Abrir un solo editor desde estado vigente sin consultas por fila
    Given el listado muestra varias reservas y aún no se abrió ninguna acción
    When abro Mover bloque de A
    Then consulto sólo estado de A y muestro panel inline con objetivo de sólo lectura y controles nativos de inicio, fin y zona
    And los valores locales corresponden a instantes y zona confirmados, no a zona implícita del navegador
    And no hay GET de estado por fila ni calendario, modal, arrastre o cambio de tarea/proyecto

  @s29
  Scenario Outline: Revisar antes y después sin reutilizar consentimiento
    Given editor con preview válido que muestra antes/después, zona de presupuesto y exceso
    When <accion>
    Then <resultado>
    Examples:
      | accion | resultado |
      | modifico hora, duración, zona u ocurrencia | retiro preview y consentimiento y exijo nueva revisión |
      | abro movimiento tras uno confirmado con permiso de exceso | no heredo consentimiento de creación ni cambio anterior |
      | confirmo destino con exceso tras aceptación explícita | envío allowOverBudget true sin atribuir permiso de solape |

  @s30
  Scenario: Mostrar zona histórica desconocida sin inventar conversión local
    Given Intl no reconoce la zona guardada de A
    When abro sus acciones
    Then muestro intervalo UTC etiquetado e ID original
    And para revisar movimiento exijo elegir zona resoluble y horas explícitas sin sustituir silenciosamente horario
    And cancelar sigue disponible aunque esa zona no se resuelva

  @s31
  Scenario Outline: Rechazar respuestas de estado o revisión incompatibles
    Given respuesta200 de <recurso> con <defecto>
    When el cliente la procesa
    Then muestra error de lectura o revisión y no habilita confirmación con ese dato
    And conserva contexto previo sin mezclarlo con respuesta incompatible
    Examples:
      | recurso | defecto |
      | estado | campo faltante/extra, Block9 inválido o status desconocido |
      | estado | ETag débil, de otro bloque, versión no canónica o fuera de BIGINT |
      | estado | updatedAt fuera de UTC/microsegundos heredados |
      | preview | objetivo/contexto distinto del bloque leído |
      | preview | instantes, offsets, duración o días incompatibles con propuesta según11@s40 |
      | preview | ETag de bloque distinto o availabilityEtag inválido |

  @s32
  Scenario Outline: Confirmar sólo recibos cerrados correspondientes a intención
    Given intención retenida transmitida y respuesta200 o201 con <defecto>
    When proceso confirmación o recuperación
    Then la operación sigue incierta con misma key, cuerpo y headers retenidos
    And no inyecto ese recibo en lista vigente
    Examples:
      | defecto |
      | campo extra/faltante, id no UUID o kind incompatible |
      | blockId o identidad de before/after distinta |
      | after null para movimiento o no null para cancelación |
      | destino incompatible con instantes y zona de intención confirmada |
      | revision de otro bloque o versión inválida |
      | occurredAt inválido |
      | Location incompatible en POST201 o replay POST200; GET recibo no exige ese header |

  @s33
  Scenario Outline: Mantener incertidumbre sin permitir intención nueva
    Given intención transmitida con key, cuerpo, If-Match y Availability-Revision aplicable
    When <resultado>
    Then mantengo recuperación bloqueando otra intención y ofrezco Comprobar cambio por la misma key
    And no genero key nueva ni envío POST automático
    Examples:
      | resultado |
      | pérdida de red o503 |
      | cuerpo inválido o código desconocido |
      | 409 IDEMPOTENCY_CONFLICT |

  @s34
  Scenario: Reenviar sólo intención retenida después de ausencia de recibo
    Given Comprobar cambio devolvió404 BLOCK_CHANGE_NOT_FOUND y la petición original podría seguir en vuelo
    When la persona elige reenviar manualmente
    Then envío exactamente misma key, cuerpo y headers de revisión retenidos aplicables
    And no muestro404 como rollback confirmado ni permito sustituir por otra intención

  @s35
  Scenario Outline: Recuperar rechazos definitivos o CSRF sin sobreescribir cambios ajenos
    Given intención transmitida que recibe <error>
    When la persona elige <accion>
    Then <resultado>
    Examples:
      | error | accion | resultado |
      | 412 BLOCK_CONFLICT | consultar estado actual | conservo borrador, retiro preview/consentimiento y no reintento con nueva revisión automáticamente |
      | validación, estado, elegibilidad, solape, presupuesto o BLOCK_UNCHANGED conocidos | corregir borrador | exijo nueva revisión antes de otra intención |
      | CSRF reconocido | renovar CSRF manualmente y reenviar | conservo key, cuerpo y revisiones sin reenvío automático |

  @s36
  Scenario Outline: Mostrar confirmación histórica antes de consultar vigencia
    Given se recuperó <hecho> y el estado actual de A está por comprobar
    When el cliente procesa ese recibo válido
    Then conserva artículo de confirmación etiquetado histórico y consulta estado y listado por separado
    And no inyecta DTO recuperado en lista ni sustituye recuperación por GET detalle
    And si falla estado muestra Operación confirmada; estado actual sin comprobar y no habilita nueva acción
    Examples:
      | hecho |
      | creación original mediante by-request11 y A tiene cambios posteriores |
      | movimiento anterior mediante changes/by-request y A tiene cambios posteriores |
      | cancelación confirmada cuyo refresco de listado falla |

  @s37
  Scenario Outline: Descartar respuestas antiguas tras cada espera
    Given operación, clasificación de error o renovación CSRF pendiente
    And <cambio>
    When llega la respuesta anterior
    Then no restaura datos, recibos, preview, mensajes ni permisos retirados ni provoca otro POST
    And un401 obsoleto no alcanza observer de acceso de sesión vigente
    Examples:
      | cambio |
      | cambio de ruta o tarea |
      | cierro sesión y entra otra persona |
      | otra generación sustituyó esa petición antes de cleanup |

  @s38
  Scenario Outline: Conservar foco y distinguir cerrar editor de cancelar reserva
    Given panel abierto y <contexto>
    When <accion>
    Then <resultado>
    Examples:
      | contexto | accion | resultado |
      | sin intención transmitida | pulso Cancelar edición | cierro formulario sin POST de negocio |
      | intención transmitida todavía en vuelo y aviso de que salir no revoca la operación | elijo salir del editor | descarto estado local y cierro sin POST nuevo ni afirmar rollback o mostrar recuperación del panel retirado |
      | confirmación inline identifica objetivo e intervalo | pulso Confirmar cancelación del bloque | envío cancelación y tras éxito retiro fila, muestro recibo y enfoco Bloques planificados si desapareció control |
      | control visible y no moví foco | recibo actualización o error | conservo foco visible elegido |
      | moví foco a otro control visible mientras esperaba | recibo respuesta | no robo foco |

  @s39
  Scenario: Consultar historial aunque no queden reservas activas
    Given A cancelled y ninguna reserva planned en T
    When abro Cambios de bloques
    Then veo historial paginado inline con before/after, tipo y fecha y puedo recuperar cancelados tras recargar
    And carga, vacío y errores de historial/listado son estados separados con reintento explícito
    And fallo de otra lectura no oculta una operación confirmada

  @s40
  Scenario: Verificar interfaz completa y límites de evidencia
    Given estados de lista, editor, preview, cancelación, incertidumbre, error e historial con texto largo
    When reviso la matriz de docs/ux-requirements.md
    Then controles y contenido son operables entre320 y2560px, a ambos lados de breakpoints y con altura reducida
    And teclado y táctil tienen alternativas completas, foco visible, objetivos44px, feedback menor400ms y ausencia de overflow
    And verifico zoom nativo200%, texto ampliado, axe y Chromium/Firefox/WebKit con resultados separados
    And documento treinta leyes UX con evidencia por estado o límite humano/físico explícito sin certificar dispositivos no probados
    And recorrido real crea, mueve, cancela y recupera recibos mediante API/PostgreSQL sin simular respuestas felices

  @s41
  Scenario: Deduplicar cancelaciones de bloques distintos sin fila de disponibilidad
    Given A y otro bloque C pertenecen a T, ambos planned con revisión actual y sin disponibilidad
    And dos cancelaciones usan la misma key nueva para A y C con sus If-Match respectivos
    When ambas cancelaciones compiten antes de confirmar un recibo
    Then una recibe201 y la otra409 IDEMPOTENCY_CONFLICT, nunca503 por colisión de key
    And sólo el bloque ganador cambia a cancelled y aumenta una revisión
    And el perdedor conserva estado y revisión, sin recibo ni evento parcial
    And existe un solo recibo para esa key y un evento, sin crear disponibilidad
