@complete_reopen_task @draft
Feature: Completar y reabrir tareas conservando cada transición
  Preparación documental: split_task sigue siendo la única implementación activa.
  Este borrador no autoriza código ni sustituye la sección final de project-spec.
  Se conserva DTO8 ampliando explícitamente status a pending o completed.
  El recurso /status expone exactamente status, completedAt y updatedAt con ETag propio.
  ETag propuesto: fuerte, con contenido task:<UUID canónico minúsculo>:<versión decimal canónica BIGINT no negativa>.
  Cursor de historia: base64url canónica de JSON con exactamente projectId, taskId y taskVersion BIGINT positiva.
  Cada referencia a variantes exige ejecutar todas las filas, nunca seleccionar sólo una.
  En cada rechazo el resto de la petición es válido; las reglas de sesión, origen y CSRF existentes no cambian.
  Con media type admitido y después de los filtros existentes, validar path antes de cursor/cuerpo/precondición; PUT valida precondición antes de JSON.
  La selección del endpoint puede rechazar media type con 415 antes del handler.
  El historial conserva sus transiciones indefinidamente, separado de la retención operativa del outbox.
  Sintaxis JSON precede forma y tipos; propiedades extra preceden status, seleccionando la primera por orden lexical.
  Tras validar la petición, comprobar propiedad/existencia antes de versión y versión antes de no-op.

  @s1
  Scenario: Consultar estado de una tarea pendiente existente
    Given una sesión válida y una tarea propia creada antes de esta funcionalidad
    When consulto GET "/api/v1/projects/{projectId}/tasks/{id}/status"
    Then recibo 200 con status pending, completedAt null y updatedAt confirmado
    And el cuerpo tiene exactamente esos tres campos y un ETag fuerte de tarea
    And no se inventan transiciones anteriores

  @s2
  Scenario: Completar una tarea explícitamente
    Given una tarea propia pending y su ETag vigente
    When envío PUT a su recurso status con JSON {"status":"completed"} y If-Match vigente
    Then recibo 200 con completedAt UTC de microsegundos igual al updatedAt confirmado
    And recibo un ETag nuevo de la misma versión confirmada que el cuerpo
    And se guarda una sola transición pending a completed y un solo TaskStatusChanged.v1
    And se conservan título, criterio, estimación, relación, createdAt y datos del proyecto

  @s3
  Scenario: Reabrir sin borrar la finalización anterior
    Given una tarea propia completed con una finalización registrada y ETag vigente
    When solicito explícitamente status pending con esa precondición
    Then recibo 200 con completedAt null, updatedAt confirmado y ETag nuevo
    And la finalización anterior permanece y se añade una transición completed a pending
    And no se inventa tiempo trabajado ni cambia ningún ancestro o descendiente

  @s4
  Scenario Outline: Repetir una intención satisfecha con versión vigente
    Given una tarea propia en estado <estado> y su ETag vigente
    When solicito ese mismo estado con If-Match vigente
    Then recibo 200 con cuerpo y ETag anteriores
    And no cambian fechas, versión, historial ni outbox
    Examples:
      | estado    |
      | pending   |
      | completed |

  @s5
  Scenario: Detectar una versión antigua aunque coincida el estado solicitado
    Given otra operación completó y reabrió la tarea después de mi lectura pending
    When solicito pending con el ETag anterior
    Then recibo 412 TASK_CONFLICT sin escrituras
    And sólo una consulta deliberada recupera la versión actual

  @s6
  Scenario Outline: Exigir una precondición propia inequívoca
    Given una sesión válida, CSRF y una tarea propia
    When envío un cambio con If-Match <defecto>
    Then recibo <resultado> sin cambiar tarea, historial ni evento
    Examples:
      | defecto                        | resultado                    |
      | ausente                        | 428 PRECONDITION_REQUIRED    |
      | mal formado                    | 400 VALIDATION_ERROR         |
      | débil                          | 400 VALIDATION_ERROR         |
      | comodín                        | 400 VALIDATION_ERROR         |
      | lista de valores               | 400 VALIDATION_ERROR         |
      | cabecera repetida              | 400 VALIDATION_ERROR         |
      | ETag del proyecto              | 400 VALIDATION_ERROR         |
      | ETag de otra tarea             | 400 VALIDATION_ERROR         |
      | UUID en mayúsculas             | 400 VALIDATION_ERROR         |
      | versión con ceros iniciales    | 400 VALIDATION_ERROR         |
      | versión con signo             | 400 VALIDATION_ERROR         |
      | versión desbordada             | 400 VALIDATION_ERROR         |
    # Los errores de formato usan campo If-Match y código INVALID_VALUE.

  @s7
  Scenario Outline: Validar un cuerpo cerrado de estado
    Given una sesión válida, CSRF y precondición vigente
    When envío <cuerpo> al recurso de estado
    Then recibo <resultado> sin escrituras
    And cuando el resultado es VALIDATION_ERROR contiene campo <campo> y código <codigo>
    Examples:
      | cuerpo                      | resultado                  | campo  | codigo        |
      | objeto sin status           | 400 VALIDATION_ERROR       | status | REQUIRED      |
      | status null                 | 400 VALIDATION_ERROR       | status | REQUIRED      |
      | status número               | 400 VALIDATION_ERROR       | status | INVALID_TYPE  |
      | status array                | 400 VALIDATION_ERROR       | status | INVALID_TYPE  |
      | status objeto               | 400 VALIDATION_ERROR       | status | INVALID_TYPE  |
      | status booleano             | 400 VALIDATION_ERROR       | status | INVALID_TYPE  |
      | status desconocido          | 400 VALIDATION_ERROR       | status | INVALID_VALUE |
      | status vacío                | 400 VALIDATION_ERROR       | status | INVALID_VALUE |
      | status con espacios         | 400 VALIDATION_ERROR       | status | INVALID_VALUE |
      | status COMPLETED            | 400 VALIDATION_ERROR       | status | INVALID_VALUE |
      | propiedad extra             | 400 VALIDATION_ERROR       | extra  | UNKNOWN_FIELD |
      | JSON truncado               | 400 MALFORMED_JSON         | —      | —             |
      | cuerpo vacío                | 400 MALFORMED_JSON         | —      | —             |
      | dos documentos concatenados | 400 MALFORMED_JSON         | —      | —             |
      | clave status duplicada      | 400 MALFORMED_JSON         | —      | —             |
      | raíz array                  | 400 VALIDATION_ERROR       | body   | INVALID_TYPE  |
      | raíz null                   | 400 VALIDATION_ERROR       | body   | INVALID_TYPE  |
      | raíz textual                | 400 VALIDATION_ERROR       | body   | INVALID_TYPE  |
      | raíz numérica               | 400 VALIDATION_ERROR       | body   | INVALID_TYPE  |
      | Content-Type text/plain     | 415 UNSUPPORTED_MEDIA_TYPE | —      | —             |

  @s8
  Scenario Outline: Mantener estados de tarea independientes del proyecto
    Given una tarea propia <inicial> dentro de un proyecto <estado>
    When solicito <destino> mediante una precondición vigente
    Then la tarea queda <destino> y el proyecto conserva datos y ETag
    And no se completa ninguna otra tarea ni se suman estimaciones
    Examples:
      | estado    | inicial   | destino   |
      | idea      | pending   | completed |
      | active    | pending   | completed |
      | paused    | pending   | completed |
      | completed | pending   | completed |
      | idea      | completed | pending   |
      | active    | completed | pending   |
      | paused    | completed | pending   |
      | completed | completed | pending   |

  @s9
  Scenario: Conservar contratos de lectura al ampliar estados
    Given una tarea completed con padre y un hijo completed en el mismo proyecto
    When consulto colección plana, detalle, padre y lista de hijos
    Then las representaciones conservan exactamente DTO8 con el estado confirmado
    And clientes y orden de paginación aceptan pending y completed

  @s10
  Scenario: Consultar historial vacío confirmado
    Given una tarea propia sin transiciones
    When consulto GET "/api/v1/projects/{projectId}/tasks/{id}/history"
    Then recibo 200 con exactamente {"items":[],"nextCursor":null}

  @s11
  Scenario: Conservar todas las finalizaciones al volver a completar
    Given una tarea propia fue completada, reabierta y completada de nuevo
    When consulto su historial
    Then veo las tres transiciones confirmadas ordenadas por versión descendente
    And cada entrada tiene exactamente id, fromStatus, toStatus y occurredAt
    And una limpieza operativa del outbox no elimina esas transiciones

  @s12
  Scenario: Paginar historia sin repetir entradas ante cambios posteriores
    Given una tarea tiene 21 transiciones y he obtenido las primeras 20 con su cursor
    And se confirmó una transición posterior
    When solicito la continuación original
    Then recibo sólo la transición restante de la lectura original y nextCursor null
    And no aparecen duplicados ni la transición posterior

  @s13
  Scenario Outline: Vincular el cursor al historial solicitado
    Given una sesión válida y una tarea propia
    When consulto historia con <defecto>
    Then recibo 400 VALIDATION_ERROR sin datos ajenos
    Examples:
      | defecto                                      |
      | cursor de otra tarea                        |
      | cursor de otro proyecto                     |
      | cursor de lista plana                       |
      | cursor de lista de subtareas                |
      | cursor vacío                               |
      | base64url mal codificada                    |
      | base64url con padding                       |
      | JSON truncado                              |
      | dos documentos JSON                        |
      | raíz JSON que no es objeto                 |
      | clave ausente                              |
      | clave extra                                |
      | clave duplicada                            |
      | projectId no textual                       |
      | taskId no textual                          |
      | versión cero                               |
      | versión negativa                           |
      | versión fraccionaria                       |
      | versión desbordada                         |
      | versión textual                            |
      | versión booleana                           |
      | query repetida                             |
      | parámetro desconocido                      |
    # Campo cursor salvo parámetros desconocidos (query); código INVALID_VALUE.
    # Ausentes/duplicadas ejecutan cada una de projectId, taskId y taskVersion.
    # Raíz no objeto ejecuta null, array, texto, número y booleano por separado.

  @s14
  Scenario Outline: Proteger todos los recursos de estado e historial
    Given <sesion> y un recurso <recurso>
    And el PUT tiene If-Match sintácticamente válido de la tarea de la ruta, con versión antigua cuando el recurso existe
    When ejecuto GET status, PUT status y GET history comprobando cada respuesta
    Then recibo <resultado> en cada operación sin escrituras ni información privada
    And cada cuerpo 404 completo es igual al de una tarea inexistente de referencia
    And las respuestas 401 no incluyen desafío Basic
    Examples:
      | sesion          | recurso                    | resultado                    |
      | ausente         | propio                     | 401 UNAUTHENTICATED           |
      | JDBC vencida    | propio                     | 401 UNAUTHENTICATED           |
      | válida con CSRF | inexistente                | 404 RESOURCE_NOT_FOUND       |
      | válida con CSRF | de otro propietario        | 404 RESOURCE_NOT_FOUND       |
      | válida con CSRF | de otro proyecto propio    | 404 RESOURCE_NOT_FOUND       |

  @s15
  Scenario Outline: Mantener protección de escrituras
    Given una sesión válida y una tarea propia con precondición vigente
    When envío PUT con <defecto>
    Then recibo <resultado> y no cambia ningún registro
    Examples:
      | defecto                         | resultado              |
      | CSRF ausente                    | 403 CSRF_INVALID       |
      | CSRF inválido                   | 403 CSRF_INVALID       |
      | origen extranjero y CSRF válido | 403 UNTRUSTED_ORIGIN   |

  @s16
  Scenario: Resolver dos cambios concurrentes con una única versión ganadora
    Given una tarea pending recibe dos solicitudes completed con el mismo ETag que compiten realmente
    When el primer cambio confirma mientras el segundo espera
    Then sólo el primero recibe 200 y el segundo recibe 412 TASK_CONFLICT
    And se incrementa una sola versión con una transición y un evento

  @s17
  Scenario Outline: Evitar confirmaciones parciales
    Given la escritura de <registro> falla por <fallo>
    When solicito una transición válida
    Then recibo 503 STORAGE_UNAVAILABLE sin ETag de éxito
    And tarea, historial y outbox conservan íntegramente su estado anterior
    Examples:
      | registro | fallo         |
      | tarea    | excepción SQL |
      | tarea    | cero filas    |
      | historia | excepción SQL |
      | historia | cero filas    |
      | evento   | excepción SQL |
      | evento   | cero filas    |

  @s18
  Scenario Outline: No inventar estado o historia ante indisponibilidad
    Given el almacenamiento de <operacion> no está disponible
    When consulto el recurso propio
    Then recibo 503 STORAGE_UNAVAILABLE y nunca <respuesta> como éxito
    Examples:
      | operacion | respuesta                 |
      | status    | estado pending ficticio  |
      | history   | items vacío              |

  @s19
  Scenario: Conservar transición e historia durante una caída del broker
    Given el worker está habilitado y el broker detenido
    When completo una tarea propia mediante HTTP
    Then recibo 200 y su historia es consultable
    And queda un TaskStatusChanged.v1 pendiente con identidad conservada

  @s20
  Scenario: Recuperar la publicación de un cambio confirmado
    Given hay un TaskStatusChanged.v1 pendiente tras una caída del broker
    When el broker vuelve
    Then recibo el JSON original con identidad, persistencia y confirmación vigentes
    And el payload tiene exactamente eventId, aggregateId, ownerId, occurredAt, schemaVersion, type, taskId, fromStatus y toStatus
    And utiliza task.status-changed.v1 y la cola quorum durable organization.task-status-changed.v1
    And aggregateId es el proyecto, taskId la tarea y occurredAt coincide con historia y updatedAt confirmados
    And no se modifican los cinco contratos anteriores de eventos

  @s21
  Scenario: Completar desde el detalle con confirmación cierta
    Given veo una tarea pending y su estado confirmado
    When elijo Completar tarea
    Then se anuncia la espera y se bloquea el doble envío
    And sólo una confirmación válida muestra completed y su fecha real
    And el historial permite recuperar la transición después de recargar

  @s22
  Scenario: Reabrir deliberadamente desde el detalle
    Given veo una tarea completed y su finalización confirmada
    When elijo Reabrir tarea
    Then se muestra pending después de confirmar y permanece la finalización en el historial
    And no se inicia trabajo ni cambia el proyecto automáticamente

  @s23
  Scenario Outline: Recuperar un cambio incierto sin repetirlo automáticamente
    Given solicité una transición desde el detalle
    When recibo <resultado>
    Then no se presenta éxito ficticio ni se repite PUT automáticamente
    And puedo consultar deliberadamente el estado vigente antes de decidir otra acción
    Examples:
      | resultado         |
      | 412 TASK_CONFLICT |
      | 503               |
      | fallo de red      |
      | 200 con cuerpo inválido |
      | 200 sin ETag |
      | 200 con ETag de otra tarea |

  @s24
  Scenario Outline: Retirar datos privados y descartar respuestas antiguas
    Given hay una <solicitud> pendiente del detalle anterior
    When <accion> antes de recibirla
    Then su respuesta no cambia el contenido, estado ni historial de la vista vigente
    And perder sesión retira todos los datos privados
    Examples:
      | solicitud | accion                |
      | lectura status | navego a otra tarea   |
      | lectura history | navego a otra tarea  |
      | escritura | navego a otra tarea   |
      | lectura status | cierro sesión         |
      | lectura history | cierro sesión        |
      | escritura | cierro sesión         |

  @s25
  Scenario: Mantener el resultado confirmado si falla la lectura de historia
    Given una transición confirmó 200
    When falla la recarga del historial
    Then conservo el estado confirmado y un error independiente de historia
    And reintentar historia no repite la transición

  @s26
  Scenario: Verificar teclado, reflow y feedback del control de estado
    Given uso detalle e historial con contenido largo
    When recorro la matriz y breakpoints de docs/ux-requirements.md
    Then las acciones tienen foco visible, nombres accesibles y área de al menos 44 por 44 píxeles CSS
    And no hay desplazamiento horizontal, incluido zoom nativo al 200 por ciento con ancho interior 320
    And el feedback inicial medido es inferior a 400 ms antes de liberar una respuesta retenida
    And se documentan los treinta principios con evidencia o limitaciones explícitas

  @s27
  Scenario: Añadir un hijo no reabre automáticamente un padre completado
    Given una tarea completed pertenece a un proyecto abierto
    When creo una subtarea propia bajo esa tarea
    Then la nueva subtarea queda pending y el padre conserva completed, fecha, versión e historia
    And se conserva la restricción histórica de no añadir trabajo a proyectos completed

  @s28
  Scenario Outline: Cambiar estado y crear un hijo sin bloqueo circular
    Given crear un hijo y completar su padre compiten en PostgreSQL sobre un proyecto abierto
    And <primera> obtiene primero su bloqueo y se coordina con la otra operación
    When ambas operaciones válidas llegan a sus confirmaciones con esperas acotadas
    Then ambas confirman sin bloqueo circular ni escrituras parciales
    And el padre queda completed y el hijo pending con un evento propio por operación
    Examples:
      | primera          |
      | crear hijo       |
      | completar padre  |

  @s29
  Scenario: Obtener primera página de historia con continuidad
    Given una tarea propia tiene 21 transiciones confirmadas
    When consulto la primera página
    Then recibo las veinte versiones más recientes en orden descendente y un cursor de tres claves
    And la continuación corresponde a la última versión devuelta, sin exigir versiones consecutivas

  @s30
  Scenario Outline: Rechazar identificadores mal formados antes del cursor
    Given una sesión válida y <campo> mal formado
    When solicito <operacion>
    Then recibo 400 VALIDATION_ERROR con INVALID_FORMAT en <campo>
    Examples:
      | campo     | operacion                  |
      | projectId | GET status                 |
      | id        | GET status                 |
      | projectId | PUT status                 |
      | id        | PUT status                 |
      | projectId | GET history con cursor     |
      | id        | GET history con cursor     |

  @s31
  Scenario: Conservar no-store en cada respuesta nueva
    Given solicito GET status y GET history con respuestas 200, 400, 401, 404 y 503
    And solicito PUT status con respuestas 200, 400, 401, 403, 404, 412, 415, 428 y 503
    When compruebo cada combinación de operación y respuesta
    Then todas contienen Cache-Control no-store

  @s32
  Scenario Outline: Bloquear eventos incompatibles antes de publicar
    Given un TaskStatusChanged.v1 contiene <defecto>
    When el publicador lo procesa
    Then se bloquea con <codigo> sin enviar al broker
    Examples:
      | defecto                   | codigo            |
      | tipo desconocido          | UNSUPPORTED_EVENT |
      | versión desconocida       | UNSUPPORTED_EVENT |
      | campo requerido ausente   | INVALID_EVENT     |
      | campo extra               | INVALID_EVENT     |
      | taskId incompleto         | INVALID_EVENT     |
      | taskId no textual         | INVALID_EVENT     |
      | fromStatus desconocido    | INVALID_EVENT     |
      | toStatus desconocido      | INVALID_EVENT     |
      | ambos estados iguales     | INVALID_EVENT     |
    # Campo requerido ausente ejecuta cada uno de los nueve campos del evento por separado.

  @s33
  Scenario Outline: Ordenar transiciones aunque el reloj no avance
    Given una tarea propia tiene updatedAt confirmado y versión vigente
    And el reloj del servidor <condicion>
    When solicito una transición real
    Then se incrementa la versión una vez y se conserva updatedAt no decreciente
    And historia, evento y updatedAt usan el máximo entre reloj truncado a microsegundos y updatedAt anterior
    And completedAt coincide con ese instante en completed y es null en pending
    Examples:
      | condicion                     |
      | coincide en el microsegundo   |
      | retrocede                     |

  @s34
  Scenario Outline: No activar cambios sin una revisión válida
    Given abro un detalle de tarea y su lectura de status devuelve <respuesta>
    When se representa el control
    Then se muestra error recuperable y no puedo enviar una transición sin ETag confirmado
    Examples:
      | respuesta                  |
      | HTTP 503                   |
      | cuerpo mal formado         |
      | ETag ausente               |
      | ETag de otra tarea          |

  @s35
  Scenario: No sustituir una confirmación por una lectura anterior
    Given hay una lectura anterior de status retenida y un PUT acaba de confirmar completed
    When llega aquella lectura con pending
    Then permanece completed con su ETag y fecha confirmados
    And el estado visible del detalle coincide con el del control
    And no se repite PUT ni se pierde el historial confirmado

  @s36
  Scenario Outline: Retirar el detalle cuando la lectura de estado deniega acceso
    Given hay contenido privado en el detalle y GET status responde <respuesta>
    When se procesa esa respuesta
    Then desaparecen detalle, estado e historial privados
    And reintentar no restaura contenido anterior mientras espera la nueva consulta
    Examples:
      | respuesta |
      | 401       |
      | 404       |
