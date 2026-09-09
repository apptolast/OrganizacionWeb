Feature: Declarar reglas auditables «cuando ocurra este evento, haz esta acción» con simulación sin efectos
  Como propietario quiero que ciertos hechos de mis proyectos creen tareas o avisen a mis webhooks
  para delegar gestos repetitivos sin perder el control de lo que se ejecutó y por qué.
  Se reutilizan sesión, CSRF/origen, negociación, JSON estricto, problem+json y no-store de 20–24.
  Rutas, DTO cerrados, ETag, códigos y precedencias son los de progress/proposal_automations.md (feature 30).
  La acción NOTIFY_WEBHOOK y la lectura de la outbox por cursor (occurred_at, event_id) dependen de la
  feature 25 (progress/proposal_webhooks.md); hasta que 25 exista, sus escenarios usan un doble del puerto
  y ninguno se declara verde por vacuidad. La evidencia UX aplica docs/ux-requirements.md.

  # ---------------------------------------------------------------
  # Reglas: creación, validación, límite, lectura, edición y borrado
  # ---------------------------------------------------------------

  @s1
  Scenario: Crear una regla CREATE_TASK válida devuelve la representación cerrada sin ejecutar nada
    Given una cuenta autenticada sin reglas y con un proyecto propio activo "Marketing"
    When crea una regla con name "  Seguimiento  ", trigger TaskCreated.v1, condition null y acción CREATE_TASK hacia "Marketing" con titleTemplate "Revisar {{task.title}}", criterionTemplate null y estimatedMinutes 30
    Then recibe 201 con Location de la regla, ETag "1" y Cache-Control no-store
    And el cuerpo contiene exactamente id UUID de servidor, name "Seguimiento", enabled true, trigger, condition null, action, version 1, createdAt y updatedAt UTC con resolución máxima de microsegundos
    And la acción devuelta contiene exactamente type, projectId, titleTemplate, criterionTemplate null y estimatedMinutes 30
    And no crea tareas, entregas, filas de ejecución ni eventos en la outbox

  @s2
  Scenario Outline: El disparador es exactamente uno de los doce tipos publicados
    Given una cuenta autenticada con un proyecto propio activo
    When crea una regla CREATE_TASK con trigger <eventType> y titleTemplate "{{event.type}}"
    Then recibe <status> <detalle>
    Examples:
      | eventType                   | status | detalle                                              |
      | ProjectCreated.v1           | 201    | y la regla queda guardada                            |
      | ProjectUpdated.v1           | 201    | y la regla queda guardada                            |
      | ProjectStatusChanged.v1     | 201    | y la regla queda guardada                            |
      | TaskCreated.v1              | 201    | y la regla queda guardada                            |
      | SubtaskCreated.v1           | 201    | y la regla queda guardada                            |
      | TaskStatusChanged.v1        | 201    | y la regla queda guardada                            |
      | BlockPlanned.v1             | 201    | y la regla queda guardada                            |
      | BlockChanged.v1             | 201    | y la regla queda guardada                            |
      | WorkSessionStarted.v1       | 201    | y la regla queda guardada                            |
      | WorkSessionStateChanged.v1  | 201    | y la regla queda guardada                            |
      | WorkSessionExtended.v1      | 201    | y la regla queda guardada                            |
      | WorkSessionClosed.v1        | 201    | y la regla queda guardada                            |
      | TaskCreated.v2              | 400    | UNKNOWN_EVENT_TYPE sobre trigger.eventType sin escritura |
      | taskcreated.v1              | 400    | UNKNOWN_EVENT_TYPE sobre trigger.eventType sin escritura |
      | webhook.ping.v1             | 400    | UNKNOWN_EVENT_TYPE sobre trigger.eventType sin escritura |
      | cadena vacía                | 400    | UNKNOWN_EVENT_TYPE sobre trigger.eventType sin escritura |

  @s3
  Scenario Outline: La condición es null o un proyecto propio, y ajeno e inexistente responden igual
    Given una cuenta A con proyecto propio P y una cuenta B con proyecto Q
    When A crea una regla TaskCreated.v1 con condition <condition>
    Then recibe <status> <efecto>
    Examples:
      | condition                       | status | efecto                                                       |
      | null                            | 201    | y condition null en la representación                        |
      | { projectId: P }                | 201    | y condition exactamente { projectId: P }                     |
      | { projectId: Q }                | 422    | TARGET_NOT_FOUND sobre condition.projectId sin escritura     |
      | { projectId: UUID inexistente } | 422    | TARGET_NOT_FOUND con cuerpo byte a byte igual al de Q        |
      | { projectId: "no-uuid" }        | 400    | VALIDATION_ERROR sobre condition.projectId sin escritura     |
      | {}                              | 400    | VALIDATION_ERROR sobre condition.projectId sin escritura     |

  @s4
  Scenario Outline: El destino de CREATE_TASK debe ser un proyecto propio existente
    Given una cuenta A con proyectos propios P activo y C completed, y una cuenta B con proyecto Q
    When A crea una regla CREATE_TASK hacia action.projectId <projectId>
    Then recibe <status> <efecto>
    Examples:
      | projectId        | status | efecto                                                                 |
      | P                | 201    | y action.projectId P                                                   |
      | C                | 201    | y la regla se guarda; la ejecución fallará con PROJECT_COMPLETED (@s21) |
      | Q                | 422    | TARGET_NOT_FOUND sobre action.projectId sin escritura                  |
      | UUID inexistente | 422    | TARGET_NOT_FOUND con cuerpo byte a byte igual al de Q                  |

  @s5
  Scenario Outline: NOTIFY_WEBHOOK exige un endpoint propio y activo de la feature 25
    Given una cuenta A con endpoints propios E activo y D desactivado, y una cuenta B con endpoint F activo
    When A crea una regla ProjectStatusChanged.v1 con acción { type: NOTIFY_WEBHOOK, endpointId: <endpointId> }
    Then recibe <status> <efecto>
    Examples:
      | endpointId       | status | efecto                                                      |
      | E                | 201    | y action exactamente { type: NOTIFY_WEBHOOK, endpointId: E } |
      | D                | 422    | ENDPOINT_NOT_FOUND sobre action.endpointId sin escritura    |
      | F                | 422    | ENDPOINT_NOT_FOUND con cuerpo byte a byte igual al de D     |
      | UUID inexistente | 422    | ENDPOINT_NOT_FOUND con cuerpo byte a byte igual al de D     |

  @s6
  Scenario Outline: Las plantillas aceptan exactamente cuatro marcadores y texto plano con llaves sueltas
    Given una cuenta autenticada con un proyecto propio activo
    When crea una regla CREATE_TASK con trigger <trigger>, titleTemplate <titleTemplate> y criterionTemplate <criterionTemplate>
    Then recibe 201 y las plantillas se guardan byte a byte como se enviaron
    Examples:
      | trigger              | titleTemplate                                    | criterionTemplate                                  |
      | TaskCreated.v1       | "Revisar {{task.title}} en {{project.name}}"     | "{{event.type}} a las {{occurredAt}}"              |
      | ProjectCreated.v1    | "Kickoff de {{project.name}}"                    | null                                               |
      | TaskCreated.v1       | "{{task.title}}"                                 | "Llave simple { y cierre } sin marcador"           |
      | BlockPlanned.v1      | "Preparar {{task.title}}"                        | "Texto de exactamente 2000 puntos de código"       |
      | WorkSessionClosed.v1 | "Título de exactamente 160 puntos de código"     | ""                                                 |

  @s7
  Scenario Outline: Una plantilla inválida devuelve INVALID_TEMPLATE señalando el campo
    Given una cuenta autenticada con un proyecto propio activo
    When crea una regla CREATE_TASK con trigger <trigger>, titleTemplate <titleTemplate> y criterionTemplate <criterionTemplate>
    Then recibe 400 INVALID_TEMPLATE con errors[] que contiene exactamente <errores> y no escribe
    Examples:
      | trigger           | titleTemplate                | criterionTemplate            | errores                                            |
      | TaskCreated.v1    | "Revisar {{task.name}}"      | null                         | action.titleTemplate UNKNOWN_PLACEHOLDER           |
      | TaskCreated.v1    | "Revisar {{event.type"       | null                         | action.titleTemplate UNCLOSED_PLACEHOLDER          |
      | TaskCreated.v1    | "{{ task.title }}"           | null                         | action.titleTemplate UNKNOWN_PLACEHOLDER           |
      | TaskCreated.v1    | "Ok"                         | "{{project}}"                | action.criterionTemplate UNKNOWN_PLACEHOLDER       |
      | ProjectCreated.v1 | "Revisar {{task.title}}"     | null                         | action.titleTemplate PLACEHOLDER_NOT_AVAILABLE     |
      | ProjectUpdated.v1 | "Ok"                         | "{{task.title}}"             | action.criterionTemplate PLACEHOLDER_NOT_AVAILABLE |
      | TaskCreated.v1    | "{{task.title"               | "{{nada}}"                   | action.titleTemplate UNCLOSED_PLACEHOLDER, action.criterionTemplate UNKNOWN_PLACEHOLDER |

  @s8
  Scenario Outline: Los campos de la regla se validan con límites por puntos de código y JSON estricto
    Given una cuenta autenticada con un proyecto propio activo
    When crea una regla válida salvo <campo> con valor <valor>
    Then recibe 400 VALIDATION_ERROR con errors[] que señala exactamente <campo> y no escribe
    Examples:
      | campo                    | valor                                   |
      | name                     | sólo Unicode White_Space                |
      | name                     | 81 puntos de código                     |
      | name                     | ausente                                 |
      | action.titleTemplate     | 161 puntos de código                    |
      | action.titleTemplate     | cadena vacía                            |
      | action.criterionTemplate | 2001 puntos de código                   |
      | action.estimatedMinutes  | 0                                       |
      | action.estimatedMinutes  | 1441                                    |
      | action.estimatedMinutes  | 30.5                                    |
      | action.type              | DELETE_TASK                             |
      | action                   | { type: CREATE_TASK } sin projectId     |
      | enabled                  | "sí"                                    |
      | cuerpo                   | propiedad desconocida ownerId           |
      | cuerpo                   | propiedad name duplicada                |

  @s9
  Scenario Outline: El cupo de veinte reglas cuenta las desactivadas y borrar libera plaza
    Given una cuenta con <existentes> reglas, de ellas <inactivas> desactivadas, y <accion_previa>
    When crea otra regla válida
    Then recibe <status> y el total de reglas queda <total>
    And un PUT válido sobre una regla existente sigue respondiendo 200 con el cupo lleno
    Examples:
      | existentes | inactivas | accion_previa                     | status         | total |
      | 19         | 0         | ninguna acción previa             | 201            | 20    |
      | 20         | 0         | ninguna acción previa             | 409 RULE_LIMIT | 20    |
      | 20         | 20        | ninguna acción previa             | 409 RULE_LIMIT | 20    |
      | 20         | 0         | borrado de una regla con If-Match | 201            | 20    |

  @s10
  Scenario: Dos creaciones concurrentes por la última plaza producen exactamente un 201 y un 409
    Given una cuenta con 19 reglas
    When dos peticiones POST válidas se ejecutan concurrentemente
    Then una recibe 201 y la otra 409 RULE_LIMIT
    And el total de reglas de la cuenta es exactamente 20

  @s11
  Scenario Outline: Leer reglas devuelve sólo las propias, en orden estable y con ETag por regla
    Given una cuenta A con <reglas_A> y una cuenta B con 2 reglas
    When A consulta <ruta>
    Then recibe <status> <cuerpo>
    And la respuesta es no-store
    Examples:
      | reglas_A                                    | ruta                                   | status | cuerpo                                                         |
      | 0 reglas                                    | GET /api/v1/me/automations             | 200    | exactamente { items: [] }                                      |
      | 3 reglas creadas en instantes distintos     | GET /api/v1/me/automations             | 200    | { items } con las 3 en orden createdAt, id y ninguna de B      |
      | 2 reglas con el mismo createdAt             | GET /api/v1/me/automations             | 200    | las 2 ordenadas por id ascendente                              |
      | 1 regla versión 3                           | GET /api/v1/me/automations/{id propio} | 200    | la representación completa y ETag "3"                          |
      | 1 regla                                     | GET /api/v1/me/automations/{id de B}   | 404    | RESOURCE_NOT_FOUND                                             |
      | 1 regla                                     | GET /api/v1/me/automations/{UUID inexistente} | 404 | RESOURCE_NOT_FOUND con cuerpo byte a byte igual al de B   |

  @s12
  Scenario Outline: Activar y desactivar es un PUT completo que siempre incrementa la versión
    Given una regla propia versión 1 enabled true con 2 ejecuciones registradas
    When envía PUT con el cuerpo completo <cambio> e If-Match "1"
    Then recibe 200 con version 2, ETag "2" y updatedAt posterior
    And <efecto>
    Examples:
      | cambio                                  | efecto                                                               |
      | enabled false                           | la lectura devuelve enabled false y las 2 ejecuciones siguen consultables |
      | sin ningún cambio                       | el cuerpo es igual salvo version, updatedAt y ETag                   |
      | trigger TaskStatusChanged.v1            | las 2 ejecuciones anteriores siguen consultables sin cambios         |
      | acción NOTIFY_WEBHOOK a endpoint propio | la lectura devuelve la nueva acción y las 2 ejecuciones se conservan |

  @s13
  Scenario Outline: PUT y DELETE exigen If-Match vigente
    Given una regla propia versión 4
    When envía <metodo> con If-Match <ifMatch>
    Then recibe <status> y conserva versión 4, ETag "4" y el cuerpo íntegro
    Examples:
      | metodo | ifMatch    | status                                  |
      | PUT    | ausente    | 428 PRECONDITION_REQUIRED               |
      | PUT    | "4x"       | 400 VALIDATION_ERROR sobre If-Match     |
      | PUT    | 4 sin comillas | 400 VALIDATION_ERROR sobre If-Match |
      | PUT    | "3"        | 412 AUTOMATION_CONFLICT                 |
      | DELETE | ausente    | 428 PRECONDITION_REQUIRED               |
      | DELETE | "*"        | 400 VALIDATION_ERROR sobre If-Match     |
      | DELETE | "5"        | 412 AUTOMATION_CONFLICT                 |

  @s14
  Scenario: Borrar una regla libera cupo, conserva las tareas creadas y oculta sus ejecuciones
    Given una cuenta con 20 reglas, una de ellas R versión 2 con 3 ejecuciones y 3 tareas creadas por ellas
    When envía DELETE /api/v1/me/automations/{R} con If-Match "2"
    Then recibe 204 sin cuerpo
    And GET /api/v1/me/automations/{R} y GET .../{R}/runs responden 404 RESOURCE_NOT_FOUND
    And las 3 tareas siguen en sus proyectos sin cambios
    And las 3 filas de ejecución conservan createdTaskId con regla nula
    And un POST válido posterior recibe 201
    And repetir el DELETE con cualquier If-Match recibe 404

  # ---------------------------------------------------------------
  # Ejecución
  # ---------------------------------------------------------------

  @s15
  Scenario: Con el worker deshabilitado no se lee ni escribe nada y al habilitarlo continúa desde el cursor
    Given app.automations.enabled ausente en configuración y una regla activa TaskCreated.v1 con cursor en el evento E0
    And después ocurren los eventos E1 y E2 del propietario
    When se habilita app.automations.enabled=true y el worker completa un ciclo
    Then antes de habilitarlo no existía ninguna fila de ejecución ni cambio de cursor tras E0
    And tras el ciclo existen exactamente 2 ejecuciones succeeded, para E1 y E2, y 2 tareas
    And el cursor queda en E2

  @s16
  Scenario: La primera regla del propietario inicializa el cursor en el presente y no procesa historial
    Given un propietario sin reglas ni cursor con 40 eventos TaskCreated.v1 anteriores
    And crea su primera regla activa TaskCreated.v1 y después ocurre un evento nuevo E41
    When el worker completa un ciclo
    Then existe exactamente una ejecución, para E41, y exactamente una tarea creada
    And ninguna ejecución referencia los 40 eventos anteriores
    And el cursor queda en E41

  @s17
  Scenario: Los eventos se procesan en orden (occurred_at, event_id), omitiendo blocked y commits tardíos
    Given una regla activa TaskCreated.v1 con cursor en C
    And eventos posteriores E1 y E2 con el mismo occurred_at y event_id de E1 menor, E3 posterior en estado blocked y E4 posterior
    And un evento T con occurred_at anterior a C que confirma después de leer el cursor
    When el worker completa un ciclo
    Then las ejecuciones registradas tienen executedAt no decreciente en el orden E1, E2, E4
    And las 3 tareas creadas tienen createdAt en ese mismo orden
    And no existe ejecución para E3 ni para T
    And el cursor queda en E4

  @s18
  Scenario Outline: La plantilla se resuelve con los valores vigentes en el instante de ejecución
    Given una regla activa TaskCreated.v1 con titleTemplate "Revisar {{task.title}} en {{project.name}}", criterionTemplate "{{event.type}} a las {{occurredAt}}" y estimatedMinutes 30
    And un evento TaskCreated.v1 de la tarea "Redactar informe" del proyecto "Marketing" con occurredAt 2026-09-08T10:15:30.123456Z
    And <cambio_previo>
    When el worker procesa ese evento
    Then la tarea creada tiene título exactamente <titulo>
    And completionCriterion exactamente "TaskCreated.v1 a las 2026-09-08T10:15:30.123456Z"
    And estimatedMinutes exactamente 30
    Examples:
      | cambio_previo                                              | titulo                                     |
      | ningún cambio antes de la ejecución                        | "Revisar Redactar informe en Marketing"    |
      | el proyecto se renombra a "Marketing 2027" antes del ciclo | "Revisar Redactar informe en Marketing 2027" |

  @s19
  Scenario: Ejecutar CREATE_TASK crea la tarea por el caso de uso existente con su TaskCreated.v1 en una sola transacción
    Given una regla activa R con trigger TaskStatusChanged.v1, criterionTemplate null y estimatedMinutes null hacia el proyecto propio P
    And ocurre un evento TaskStatusChanged.v1 de una tarea de P
    When el worker procesa ese evento
    Then existe exactamente una tarea raíz nueva en P con completionCriterion "" y estimatedMinutes null
    And la outbox contiene exactamente un TaskCreated.v1 nuevo con aggregateId P y payload taskId igual al de la tarea
    And existe exactamente una ejecución { ruleId R, eventId del evento, attempt 1, status succeeded, createdTaskId de la tarea, deliveryId null, errorCode null }
    And el cursor queda en ese evento
    And la ejecución, la tarea, el evento y el cursor son visibles juntos en la misma confirmación
    And el log del worker contiene ruleId, eventId, outcome, attempt y code y no contiene el título ni el nombre del proyecto

  @s20
  Scenario: Un fallo de almacenamiento revierte ejecución, tarea y evento y registra un reintento
    Given una regla activa CREATE_TASK y un evento coincidente E
    And la inserción del TaskCreated.v1 falla con error de almacenamiento inducido
    When el worker procesa E
    Then no existe tarea nueva ni evento nuevo en la outbox
    And existe exactamente una ejecución para E con attempt 1, status retry y errorCode STORAGE_UNAVAILABLE
    And el cursor no avanza más allá del evento anterior a E

  @s21
  Scenario Outline: Los fallos deterministas quedan failed en el primer intento sin detener las demás reglas
    Given dos reglas activas R1 y R2 para el mismo trigger, donde R1 <situacion> y R2 es válida
    And ocurre un evento coincidente E
    When el worker procesa E
    Then la ejecución de R1 tiene attempt 1, status failed, errorCode <codigo>, createdTaskId null y deliveryId null
    And la ejecución de R2 es succeeded con su tarea o entrega
    And el cursor queda en E
    And ningún ciclo posterior reintenta R1 para E
    Examples:
      | situacion                                                           | codigo               |
      | apunta a un proyecto que pasó a completed antes del ciclo           | PROJECT_COMPLETED    |
      | resuelve un título de 161 puntos de código                          | TITLE_TOO_LONG       |
      | resuelve un criterio de 2001 puntos de código                       | CRITERION_TOO_LONG   |
      | es NOTIFY_WEBHOOK hacia un endpoint borrado antes del ciclo         | ENDPOINT_NOT_FOUND   |
      | es NOTIFY_WEBHOOK hacia un endpoint desactivado antes del ciclo     | ENDPOINT_NOT_FOUND   |

  @s22
  Scenario: Los fallos transitorios se reintentan antes que los eventos nuevos y el tercero queda failed
    Given una regla activa con una ejecución retry attempt 2 para el evento E1 y un evento nuevo E2 posterior
    And el almacenamiento vuelve a fallar sólo para la acción sobre E1
    When el worker completa un ciclo
    Then la ejecución de E1 tiene attempt 3, status failed y errorCode STORAGE_UNAVAILABLE
    And E1 se intentó antes que E2 dentro del ciclo
    And la ejecución de E2 es succeeded con attempt 1
    And el cursor queda en E2
    And un ciclo posterior no crea un cuarto intento para E1

  @s23
  Scenario: Dos workers concurrentes ejecutan una regla como máximo una vez por evento
    Given una regla activa CREATE_TASK y un evento coincidente E sin ejecución previa
    When dos workers procesan E de forma concurrente
    Then existe exactamente una ejecución para (regla, E)
    And existe exactamente una tarea creada y exactamente un TaskCreated.v1 nuevo
    And ninguno de los dos workers termina con error no controlado

  @s24
  Scenario Outline: Las reglas desactivadas no ejecutan pero el cursor avanza, sin mezclar versiones
    Given una regla R TaskCreated.v1 <estado> y un evento coincidente E
    When el worker procesa E
    Then <resultado>
    And el cursor queda en E
    Examples:
      | estado                                                                 | resultado                                                                                   |
      | desactivada antes de E                                                 | no existe ejecución ni tarea para E                                                         |
      | desactivada antes de E y reactivada después de procesarlo              | un ciclo posterior tampoco crea ejecución ni tarea para E                                    |
      | desactivada por un PUT concurrente con la evaluación de E              | o bien existe una ejecución succeeded con tarea, o bien ninguna; nunca una tarea sin ejecución |
      | con titleTemplate cambiada por un PUT concurrente con la evaluación de E | la tarea creada usa íntegramente la plantilla anterior o íntegramente la nueva               |

  @s25
  Scenario: Una caída antes de confirmar deja el cursor atrás y la relectura no duplica
    Given una regla activa CREATE_TASK y un evento coincidente E
    And el proceso cae después de crear la tarea y antes de confirmar la transacción de E
    When el worker reiniciado completa un ciclo
    Then el cursor previo al reinicio seguía en el evento anterior a E
    And existe exactamente una ejecución succeeded, una tarea y un TaskCreated.v1 para E
    And las reglas y ejecuciones anteriores al reinicio se conservan sin cambios

  @s26
  Scenario: NOTIFY_WEBHOOK encola el evento original en las entregas de la feature 25 sin transformarlo
    Given una regla activa ProjectStatusChanged.v1 con acción NOTIFY_WEBHOOK hacia el endpoint propio E suscrito sólo a TaskCreated.v1
    And ocurre un evento ProjectStatusChanged.v1 del propietario con payload conocido
    When el worker procesa ese evento
    Then existe exactamente una entrega pendiente para E con eventId y eventType del evento y body byte a byte igual al payload de la outbox
    And la ejecución es succeeded con deliveryId igual al id de esa entrega y createdTaskId null
    And no se crea ningún evento nuevo en la outbox
    And la entrega y la ejecución son visibles juntas en la misma confirmación

  @s27
  Scenario Outline: La condición resuelve el proyecto del evento según su tipo y sólo entre datos propios
    Given una cuenta A con proyectos P y P2 y una regla activa <trigger> con condition { projectId: P }
    And ocurre <evento>
    When el worker procesa ese evento
    Then <resultado>
    Examples:
      | trigger                    | evento                                                                 | resultado                          |
      | TaskCreated.v1             | un TaskCreated.v1 con aggregateId P                                    | existe exactamente una ejecución   |
      | TaskCreated.v1             | un TaskCreated.v1 con aggregateId P2                                   | no existe ejecución                |
      | WorkSessionStarted.v1      | un WorkSessionStarted.v1 con payload.projectId P                       | existe exactamente una ejecución   |
      | BlockPlanned.v1            | un BlockPlanned.v1 cuyo payload.taskId pertenece a una tarea de P      | existe exactamente una ejecución   |
      | BlockChanged.v1            | un BlockChanged.v1 cuyo payload.taskId pertenece a una tarea de P2     | no existe ejecución                |
      | WorkSessionClosed.v1       | un WorkSessionClosed.v1 cuya sesión pertenece a una tarea de P         | existe exactamente una ejecución   |
      | WorkSessionExtended.v1     | un WorkSessionExtended.v1 cuya sesión pertenece a una tarea de P2      | no existe ejecución                |
      | ProjectUpdated.v1          | un ProjectUpdated.v1 con aggregateId P                                 | existe exactamente una ejecución   |
      | TaskCreated.v1             | un TaskCreated.v1 de otra cuenta B con aggregateId de un proyecto de B | no existe ejecución ni avanza el cursor de A |

  # ---------------------------------------------------------------
  # Protección contra bucles
  # ---------------------------------------------------------------

  @s28
  Scenario Outline: El TaskCreated.v1 de una tarea creada por automatización no dispara ninguna regla
    Given una regla R1 activa TaskCreated.v1 con CREATE_TASK hacia el mismo proyecto y una regla R2 activa TaskCreated.v1 sin condición
    And una tarea humana H provoca un TaskCreated.v1 que R1 ya ejecutó creando la tarea T, cuyo TaskCreated.v1 está pendiente de leer
    And <estado_regla>
    When el worker procesa el TaskCreated.v1 de T
    Then no existe ejecución de ninguna regla para ese evento
    And el número de tareas del proyecto no cambia
    And el cursor queda en ese evento
    Examples:
      | estado_regla                                        |
      | R1 sigue existiendo                                 |
      | R1 fue borrada antes de leer el evento de T         |

  @s29
  Scenario: La profundidad es 1: una acción humana posterior sobre la tarea automatizada sí dispara reglas
    Given una tarea T creada por automatización y una regla activa TaskStatusChanged.v1 sin condición
    And el propietario completa T y se emite su TaskStatusChanged.v1
    When el worker procesa ese evento
    Then existe exactamente una ejecución succeeded para ese evento
    And la guarda sólo se consulta para TaskCreated.v1 y SubtaskCreated.v1

  # ---------------------------------------------------------------
  # Simulación
  # ---------------------------------------------------------------

  @s30
  Scenario: Simular evalúa la regla del cuerpo sin persistirla ni producir efectos
    Given una cuenta con 3 reglas, 5 eventos recientes de los que 2 son TaskCreated.v1 de "Redactar informe" en "Marketing", cursor en el último y N filas en tareas, outbox y ejecuciones
    When envía POST /api/v1/me/automations/simulate con la regla de @s18 sin id
    Then recibe 200 con exactamente { evaluatedEvents: 5, matches } y 2 coincidencias en orden occurredAt, eventId descendente
    And cada coincidencia contiene exactamente eventId, eventType TaskCreated.v1, occurredAt, preview y loopGuarded
    And la preview de la más reciente es exactamente { type: CREATE_TASK, projectId, title: "Revisar Redactar informe en Marketing", completionCriterion: "TaskCreated.v1 a las <occurredAt del evento>", estimatedMinutes: 30, wouldFail: null }
    And tareas, outbox y ejecuciones conservan exactamente N filas y el cursor no cambia
    And GET /api/v1/me/automations sigue devolviendo 3 reglas
    And la respuesta es no-store

  @s31
  Scenario Outline: Simular considera los últimos 100 eventos no bloqueados
    Given una cuenta con <eventos>
    When simula una regla TaskCreated.v1 sin condición
    Then recibe 200 con evaluatedEvents <evaluados> y matches <coincidencias>
    Examples:
      | eventos                                                                        | evaluados | coincidencias                                     |
      | 0 eventos                                                                      | 0         | []                                                |
      | 5 eventos ProjectCreated.v1                                                    | 5         | []                                                |
      | 130 eventos: los 100 más recientes ProjectUpdated.v1 y los 30 más antiguos TaskCreated.v1 | 100 | []                                          |
      | 101 eventos TaskCreated.v1                                                     | 100       | 100, sin el más antiguo                           |
      | 3 TaskCreated.v1 y 1 TaskCreated.v1 en estado blocked                          | 3         | 3, sin el bloqueado                               |

  @s32
  Scenario Outline: La vista previa anticipa fallos y guardas sin ejecutarlos
    Given una cuenta con un evento reciente <evento>
    When simula <regla>
    Then la coincidencia contiene <campos>
    And no se crean filas en tareas, outbox, entregas ni ejecuciones
    Examples:
      | evento                                                        | regla                                                          | campos                                                         |
      | TaskCreated.v1 de un proyecto ahora completed                 | CREATE_TASK hacia ese proyecto                                 | preview.wouldFail PROJECT_COMPLETED                            |
      | TaskCreated.v1 con título de 150 puntos de código             | CREATE_TASK con titleTemplate "Revisar {{task.title}} ahora"   | preview.wouldFail TITLE_TOO_LONG y title resuelto sin truncar  |
      | TaskCreated.v1 de una tarea creada por automatización         | CREATE_TASK hacia cualquier proyecto propio                    | loopGuarded true y preview resuelta                            |
      | ProjectStatusChanged.v1 propio                                | NOTIFY_WEBHOOK hacia el endpoint propio E                      | preview exactamente { type: NOTIFY_WEBHOOK, endpointId: E, eventId } |
      | TaskCreated.v1 de un proyecto activo                          | CREATE_TASK válida                                             | preview.wouldFail null y loopGuarded false                     |

  @s33
  Scenario Outline: Simular valida el cuerpo con los mismos errores que crear y no consume cupo
    Given una cuenta con <reglas> reglas
    When simula una regla con <defecto>
    Then recibe <status> y no escribe
    Examples:
      | reglas | defecto                                       | status                    |
      | 0      | trigger "TaskCreated.v9"                      | 400 UNKNOWN_EVENT_TYPE    |
      | 0      | titleTemplate "{{task.title"                  | 400 INVALID_TEMPLATE      |
      | 0      | action.projectId de otra cuenta               | 422 TARGET_NOT_FOUND      |
      | 0      | endpointId inexistente                        | 422 ENDPOINT_NOT_FOUND    |
      | 0      | propiedad id en el cuerpo                     | 400 VALIDATION_ERROR      |
      | 20     | ningún defecto                                | 200                       |

  # ---------------------------------------------------------------
  # Auditoría
  # ---------------------------------------------------------------

  @s34
  Scenario: El historial de ejecuciones se pagina de veinte en veinte en orden descendente
    Given una regla propia con 25 ejecuciones de estados succeeded, retry y failed en instantes distintos
    When consulta GET /api/v1/me/automations/{id}/runs y después la página con su nextCursor
    Then la primera respuesta es exactamente { items, nextCursor } con 20 items en orden executedAt, id descendente
    And cada item contiene exactamente id, eventId, eventType, occurredAt, attempt, status, createdTaskId, deliveryId, errorCode y executedAt con null donde no aplica
    And la segunda respuesta contiene los 5 restantes y nextCursor null
    And ambas respuestas son no-store

  @s35
  Scenario Outline: El historial rechaza cursores ajenos y oculta reglas y ejecuciones que no son propias
    Given una cuenta A con reglas R1 y R2 con ejecuciones y una cuenta B con regla S con ejecuciones
    When A consulta <ruta>
    Then recibe <status> <cuerpo>
    Examples:
      | ruta                                            | status | cuerpo                                                   |
      | GET .../{R1}/runs                               | 200    | sólo ejecuciones de R1, ninguna de R2 ni de S            |
      | GET .../{R1}/runs?cursor=<cursor de R2>         | 400    | VALIDATION_ERROR sobre cursor                            |
      | GET .../{R1}/runs?cursor=no-base64              | 400    | VALIDATION_ERROR sobre cursor                            |
      | GET .../{S}/runs                                | 404    | RESOURCE_NOT_FOUND                                       |
      | GET .../{UUID inexistente}/runs                 | 404    | RESOURCE_NOT_FOUND con cuerpo byte a byte igual al de S  |

  # ---------------------------------------------------------------
  # Seguridad
  # ---------------------------------------------------------------

  @s36
  Scenario Outline: Las rutas son privadas y aplican CSRF, origen, tipo de contenido y no-store
    Given una regla propia existente
    When envía <peticion>
    Then recibe <status> y no escribe
    And si hay cuerpo de error es problem+json y la respuesta es no-store
    Examples:
      | peticion                                                   | status                 |
      | POST /api/v1/me/automations sin sesión                     | 401                    |
      | GET /api/v1/me/automations sin sesión                      | 401                    |
      | POST /api/v1/me/automations con sesión y sin token CSRF    | 403                    |
      | PUT /api/v1/me/automations/{id} con Origin ajeno           | 403                    |
      | POST /api/v1/me/automations/simulate con Origin ajeno      | 403                    |
      | POST /api/v1/me/automations con Content-Type text/plain    | 415                    |
      | PATCH /api/v1/me/automations/{id}                          | 405                    |
      | POST /api/v1/me/automations con almacenamiento caído       | 503 STORAGE_UNAVAILABLE |

  # ---------------------------------------------------------------
  # Interfaz /automatizaciones
  # ---------------------------------------------------------------

  @s37
  Scenario Outline: La página lista las reglas propias con disparador legible y estados independientes
    Given una sesión iniciada y el servidor responde a GET /api/v1/me/automations con <respuesta>
    When abre /automatizaciones
    Then el encabezado de nivel 1 es «Automatizaciones» y la navegación contiene la entrada «Automatizaciones» sin desplazar «Hoy»
    And <resultado>
    Examples:
      | respuesta                                         | resultado                                                                                              |
      | retraso sin resolver                              | se muestra un estado de carga anunciado con role status y ningún error                                 |
      | { items: [] }                                     | se muestra un estado vacío que explica qué es una regla y ofrece «Nueva regla»                         |
      | 2 reglas, una TaskCreated.v1 activa y otra inactiva | cada fila muestra nombre, «Tarea creada» como disparador, destino y el texto «Activa» o «Inactiva» además del color |
      | 503 STORAGE_UNAVAILABLE                           | se muestra un error con botón «Reintentar» y sin lista ni estado vacío                                 |

  @s38
  Scenario: El editor muestra la vista previa de la plantilla y asocia cada error del servidor a su campo
    Given el editor de una regla nueva con trigger «Tarea creada» y la ayuda inline de los cuatro marcadores visible
    When escribe titleTemplate "Revisar {{task.title}} en {{project.name}}" y el servidor responde al guardar 400 INVALID_TEMPLATE sobre action.criterionTemplate
    Then la vista previa muestra el texto con los marcadores sustituidos por valores de ejemplo y sin interpretar HTML
    And el campo criterio queda aria-invalid true con el mensaje enlazado por aria-describedby y recibe el foco
    And el borrador del título se conserva y no existe ninguna clave en localStorage ni sessionStorage

  @s39
  Scenario: Simular muestra las coincidencias en la misma página sin guardar
    Given el editor con una regla válida y el servidor responde a simulate con evaluatedEvents 5 y 2 coincidencias, una con wouldFail PROJECT_COMPLETED
    When pulsa «Simular»
    Then se envía exactamente una petición POST a /api/v1/me/automations/simulate y ninguna a /api/v1/me/automations
    And una región con role status anuncia «5 eventos evaluados, 2 coincidencias»
    And cada coincidencia muestra tipo, fecha y título resuelto, y la que falla muestra el texto «Fallaría: proyecto completado»
    And el botón «Guardar» sigue disponible y la lista de reglas no cambia

  @s40
  Scenario Outline: Guardar y el interruptor reflejan sólo la respuesta confirmada del servidor
    Given <estado_inicial>
    When <accion>
    Then <resultado>
    Examples:
      | estado_inicial                                             | accion                                     | resultado                                                                                                     |
      | el editor válido y una respuesta retenida                  | pulsa «Guardar»                            | el botón queda deshabilitado hasta la respuesta y sólo se envía una petición                                 |
      | una regla versión 2 y el servidor responde 412             | pulsa «Guardar»                            | se muestra «Otra pestaña cambió esta regla» con botón «Cargar versión actual» y el borrador se conserva hasta pulsarlo |
      | una regla activa y el servidor responde 200 enabled false  | activa el interruptor accesible            | el interruptor pasa a «Inactiva» sólo tras la respuesta y envía PUT con If-Match del ETag vigente             |
      | una regla activa y el servidor responde 503                | activa el interruptor accesible            | el interruptor vuelve a «Activa», se anuncia el error y no hay segunda petición                                |

  @s41
  Scenario: El historial por regla carga de veinte en veinte con estado textual y enlace a la tarea
    Given una regla con 25 ejecuciones y una de ellas succeeded con createdTaskId
    When abre su historial y pulsa «Cargar más»
    Then se muestran 20 filas y después 25 sin duplicados y sin botón «Cargar más» al agotar nextCursor
    And cada fila muestra el estado como texto («Correcta», «Reintento», «Fallida») además del color, y el código de error cuando existe
    And la fila con createdTaskId contiene un enlace a la tarea creada

  @s42
  Scenario Outline: La página cumple la matriz responsive, de zoom y de accesibilidad
    Given /automatizaciones con lista, editor abierto y resultados de simulación visibles
    When se revisa a <ancho> píxeles CSS con <zoom>
    Then no hay desplazamiento horizontal ni contenido cortado
    And todos los controles interactivos miden al menos 44 por 44 píxeles CSS y son alcanzables por teclado en orden lógico con foco visible
    And axe no reporta violaciones serious ni critical
    Examples:
      | ancho | zoom       |
      | 320   | 100 %      |
      | 768   | 100 %      |
      | 1280  | 100 %      |
      | 1440  | 100 %      |
      | 1440  | texto 200 % |

  @s43
  Scenario Outline: Navegar fuera o cerrar sesión descarta las respuestas tardías
    Given una petición de <operacion> en vuelo en /automatizaciones
    When <salida> antes de que llegue la respuesta
    Then la respuesta tardía no modifica la interfaz visible ni anuncia nada
    And no queda ningún dato de reglas ni simulaciones en memoria de otra identidad ni en localStorage
    Examples:
      | operacion      | salida                              |
      | simulación     | navega a /proyectos                 |
      | guardado       | cierra sesión                       |
      | historial      | cambia a otra regla                 |
