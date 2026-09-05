@project_states @approved
Feature: Decidir qué proyectos están activos sin exceder la capacidad elegida
  Como propietario quiero activar, pausar, terminar y reabrir proyectos de forma deliberada.
  Contrato dentro de la autorización global del usuario; producción posterior al cierre de edit_project.
  Operación: PUT /api/v1/projects/{id}/status con JSON exacto {status} e If-Match obligatorio.

  @s1
  Scenario Outline: Confirmar una transición permitida
    Given un proyecto propio en <origen> y su ETag vigente
    And hay capacidad disponible si el destino es active
    When solicita el estado <destino>
    Then recibe HTTP 200 con los siete campos del proyecto y un nuevo ETag
    And sólo cambian status, updatedAt y la versión interna
    And la lectura posterior confirma <destino>
    Examples:
      | origen    | destino   |
      | idea      | active    |
      | idea      | completed |
      | active    | paused    |
      | active    | completed |
      | paused    | active    |
      | paused    | completed |
      | completed | paused    |

  @s2
  Scenario Outline: Rechazar una transición no permitida
    Given un proyecto propio en <origen> y su ETag vigente
    When solicita el estado <destino>
    Then recibe HTTP 409 INVALID_PROJECT_TRANSITION sin modificar proyecto ni outbox
    Examples:
      | origen    | destino |
      | idea      | paused  |
      | active    | idea    |
      | paused    | idea    |
      | completed | idea    |
      | completed | active  |

  @s3
  Scenario Outline: Repetir el estado vigente sin efectos adicionales
    Given un proyecto propio en <estado> con ETag vigente
    When solicita ese mismo estado
    Then recibe HTTP 200 con representación y ETag originales
    And no modifica fechas, versión ni outbox
    Examples:
      | estado    |
      | idea      |
      | active    |
      | paused    |
      | completed |

  @s4
  Scenario: Proteger la última plaza frente a activaciones simultáneas
    Given el propietario tiene dos proyectos activos y un límite de tres
    And tiene dos proyectos distintos que puede activar con ETags vigentes
    When solicita ambas activaciones concurrentemente
    Then exactamente una recibe HTTP 200 y la otra HTTP 409 ACTIVE_PROJECT_LIMIT
    And existen tres proyectos activos propios y un único evento adicional
    And el rechazo contiene activeCount 3 y limit 3 sin datos de otros propietarios

  @s5
  Scenario: Liberar capacidad sin cambiar otros proyectos
    Given el propietario ocupa todas sus plazas activas
    When pausa un proyecto propio con su ETag vigente
    Then ese proyecto queda pausado y los demás conservan su estado
    And hay una plaza disponible para una activación posterior

  @s6
  Scenario: Aplicar el límite a cada propietario
    Given otro propietario ocupa todas sus plazas activas
    And el propietario actual tiene capacidad disponible
    When activa un proyecto propio con ETag vigente
    Then recibe HTTP 200 sin cambiar proyectos ni capacidad del otro propietario

  @s7
  Scenario Outline: Validar la configuración de capacidad al arrancar
    Given APP_MAX_ACTIVE_PROJECTS contiene <valor>
    When arranca la API
    Then obtiene <resultado>
    Examples:
      | valor   | resultado                         |
      | ausente | arranque con límite 3             |
      | 1       | arranque con límite 1             |
      | 10      | arranque con límite 10            |
      | 0       | fallo de configuración al iniciar |
      | 11      | fallo de configuración al iniciar |
      | abc     | fallo de configuración al iniciar |
      | 1.5     | fallo de configuración al iniciar |

  @s8
  Scenario: Reducir capacidad sin pausar trabajo automáticamente
    Given existen tres proyectos activos propios y el despliegue adopta límite dos
    When solicita activar otro proyecto propio con ETag vigente
    Then recibe HTTP 409 ACTIVE_PROJECT_LIMIT con activeCount 3 y limit 2
    And los tres proyectos originales siguen activos sin eventos adicionales

  @s9
  Scenario Outline: Compartir concurrencia con la edición de texto
    Given una pestaña guarda <primero> y otra conserva el ETag anterior
    When la segunda intenta <segundo> con ese ETag
    Then recibe HTTP 412 PROJECT_CONFLICT sin sobrescribir datos ni añadir eventos
    Examples:
      | primero           | segundo           |
      | un cambio de texto | un cambio de estado |
      | un cambio de estado | un cambio de texto |
      | un cambio de estado | repetir el estado actual |

  @s10
  Scenario Outline: Mantener las fronteras de acceso y validación
    Given una solicitud válida de cambio de estado, salvo la condición indicada
    When la solicitud tiene <condición>
    Then recibe <respuesta> sin modificar proyectos ni outbox
    And cuando responde 404, ajeno e inexistente comparten el mismo cuerpo público sin revelar estado ni conflicto
    Examples:
      | condición                    | respuesta                   |
      | sin credenciales válidas     | 401 UNAUTHENTICATED          |
      | proyecto ajeno               | 404 PROJECT_NOT_FOUND       |
      | proyecto inexistente         | 404 PROJECT_NOT_FOUND       |
      | id inválido                  | 400 VALIDATION_ERROR        |
      | If-Match ausente             | 428 PRECONDITION_REQUIRED   |
      | If-Match mal formado         | 400 VALIDATION_ERROR        |
      | If-Match repetido            | 400 VALIDATION_ERROR        |
      | If-Match débil               | 400 VALIDATION_ERROR        |
      | If-Match comodín             | 400 VALIDATION_ERROR        |
      | status desconocido           | 400 VALIDATION_ERROR        |
      | status null                  | 400 VALIDATION_ERROR        |
      | status ausente               | 400 VALIDATION_ERROR        |
      | propiedad adicional          | 400 VALIDATION_ERROR        |
      | clave JSON repetida          | 400 VALIDATION_ERROR        |
      | documentos JSON consecutivos | 400 VALIDATION_ERROR        |
      | origen de navegador ajeno    | 403 UNTRUSTED_ORIGIN         |
  @s11
  Scenario: Registrar un único evento del cambio confirmado
    Given un proyecto propio y una transición permitida con ETag vigente
    When confirma el cambio mientras RabbitMQ no está disponible
    Then recibe HTTP 200 y persiste un ProjectStatusChanged.v1 pendiente en la misma transacción
    And su payload contiene exactamente eventId, aggregateId, ownerId, occurredAt, schemaVersion, type, fromStatus y toStatus
    And occurredAt coincide con updatedAt, schemaVersion es 1 y no incluye nombre ni descripción

  @s12
  Scenario Outline: Revertir un fallo de escritura
    Given un proyecto propio y una transición permitida con ETag vigente
    When falla la escritura de <registro>
    Then recibe HTTP 503 STORAGE_UNAVAILABLE sin falso éxito
    And proyecto, versión, fechas y outbox conservan su estado anterior
    Examples:
      | registro |
      | proyecto |
      | outbox   |

  @s13
  Scenario: Publicar el nuevo evento sin alterar las rutas existentes
    Given hay eventos válidos Created, Updated y StatusChanged pendientes
    When el publicador obtiene confirmación del broker
    Then StatusChanged usa project.status-changed.v1 y organization.project-status-changed.v1 en organization.events
    And conserva JSON original, message-id, persistencia, cola quorum, mandatory y confirms
    And Created y Updated conservan sus rutas y los tipos no admitidos siguen bloqueados

  @s14
  Scenario Outline: Mostrar el estado real en todos los recorridos
    Given un proyecto propio con estado <estado>
    When consulta la lista, el detalle o el formulario de edición
    Then muestra <etiqueta> donde se presenta el estado sin rechazar la representación
    And editar nombre y descripción conserva ese estado
    Examples:
      | estado    | etiqueta  |
      | idea      | Idea      |
      | active    | Activo    |
      | paused    | Pausado   |
      | completed | Terminado |

  @s15
  Scenario: Cambiar estado con una acción explícita y recuperable
    Given el detalle muestra acciones válidas para su estado actual
    When activa una de esas acciones
    Then anuncia la espera antes de 400 ms y bloquea envíos duplicados
    And sólo anuncia éxito y modifica la representación tras una respuesta confirmada
    And conflicto o límite activo ofrecen recuperación deliberada sin reintentar ni pausar otros proyectos automáticamente
    And pérdida de acceso retira datos, los demás errores permiten recuperación y las respuestas antiguas no cambian otra ruta

  @s16
  Scenario: Conservar accesibilidad y adaptación del control de estado
    Given el detalle contiene texto largo y controles de estado
    When se recorre la matriz UX con teclado, táctil y zoom real al 200 por ciento
    Then conserva etiquetas, foco visible, anuncios accesibles y acciones principales de 44 por 44 píxeles CSS
    And no hay desplazamiento horizontal ni recortes a 320 píxeles CSS
    And registra los 30 principios, las pruebas ejecutadas y sus límites sin afirmar cobertura de dispositivos no probados

  @s17
  Scenario: Mantener errores internos y respuestas privadas seguros
    Given ocurre un fallo inesperado al cambiar estado
    When responde la API
    Then devuelve HTTP 500 INTERNAL_ERROR con correlationId sin SQL, stacktrace ni credenciales
    And las respuestas privadas de consulta y cambio mantienen Cache-Control no-store
