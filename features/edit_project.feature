@edit_project @approved
Feature: Editar el nombre y la descripción de un proyecto propio
  Como propietario quiero corregir un proyecto sin perder ediciones de otra pestaña.
  Autorización global del usuario: «Si las apruebo todas» (5 de septiembre de 2026).
  El contrato se prepara mientras read_projects permanece como única feature activa.

  @s1
  Scenario: Guardar una modificación propia con precondición vigente
    Given existe un proyecto propio y el propietario conserva el ETag de su detalle
    When envía PUT con name y description válidos y ese If-Match
    Then recibe HTTP 200 con los siete campos del proyecto confirmado y un nuevo ETag
    And conserva id, ownerId, createdAt y status
    And name y description reflejan los valores validados y updatedAt procede del reloj servidor UTC

  @s2
  Scenario: Proteger dos ediciones concurrentes
    Given dos pestañas conservan el mismo ETag de un proyecto propio
    When la primera guarda una modificación y la segunda intenta guardar usando el ETag anterior
    Then la primera recibe HTTP 200 y la segunda HTTP 412 con código PROJECT_CONFLICT
    And la segunda no modifica el proyecto ni añade eventos

  @s3
  Scenario: No escribir un cambio equivalente
    Given el propietario conserva el ETag vigente y los mismos valores editables almacenados
    When envía PUT con esos valores
    Then recibe HTTP 200 con representación y ETag originales
    And no cambia updatedAt ni la versión interna ni se añade un evento

  @s4
  Scenario: Exigir precondición incluso para cambios equivalentes
    Given el propietario consulta un proyecto propio
    When envía PUT sin If-Match
    Then recibe HTTP 428 con código PRECONDITION_REQUIRED
    And no escribe proyecto ni evento

  @s5
  Scenario Outline: Validar estrictamente la precondición
    Given el propietario está autenticado y existe el proyecto propio
    When envía PUT con If-Match <defecto>
    Then recibe HTTP 400 con código VALIDATION_ERROR
    And no escribe proyecto ni evento
    Examples:
      | defecto        |
      | mal formado    |
      | repetido       |
      | débil          |
      | comodín        |

  @s6
  Scenario Outline: Reutilizar la validación de campos y JSON
    Given existe un proyecto propio y una precondición vigente
    When envía PUT con <defecto>
    Then recibe HTTP 400 con código VALIDATION_ERROR y error en <campo>
    And no modifica el proyecto ni añade eventos
    Examples:
      | defecto                          | campo       |
      | nombre vacío                     | name        |
      | nombre de 121 puntos de código   | name        |
      | descripción de 4001 puntos       | description |
      | name ausente                     | name        |
      | description ausente              | description |
      | description null                 | description |
      | campo ownerId adicional          | body        |
      | dos documentos JSON consecutivos | body        |
      | clave JSON repetida              | body        |

  @s7
  Scenario: Conservar Unicode y texto literal con normalización acordada
    Given existe un proyecto propio y una precondición vigente
    When envía un nombre Unicode con espacios exteriores y una descripción con marcado HTML
    Then guarda el nombre con el mismo trim y cómputo de puntos de código de creación
    And conserva exactamente el texto de la descripción
    And la representación y la web no interpretan ese texto como HTML

  @s8
  Scenario Outline: No revelar proyectos de otro propietario
    Given el propietario está autenticado
    When intenta editar un proyecto <caso> con una precondición bien formada
    Then recibe HTTP 404 con código PROJECT_NOT_FOUND y mensaje Proyecto no encontrado
    And no recibe datos ni un conflicto que revele la existencia del proyecto
    And no escribe proyecto ni evento
    Examples:
      | caso        |
      | inexistente |
      | ajeno       |

  @s9
  Scenario: Exigir autenticación antes de editar
    Given la solicitud no tiene credenciales válidas
    When intenta editar un proyecto
    Then recibe HTTP 401 con código UNAUTHENTICATED sin datos privados
    And no escribe proyecto ni evento

  @s10
  Scenario: Rechazar un identificador inválido
    Given el propietario está autenticado
    When intenta editar un id que no es UUID
    Then recibe HTTP 400 con código VALIDATION_ERROR y error en id

  @s11
  Scenario Outline: Conservar las protecciones del API de escritura
    Given el propietario está autenticado y existe el proyecto propio
    When intenta PUT con <defecto>
    Then se rechaza la escritura sin modificar proyecto ni evento
    Examples:
      | defecto                     |
      | origen de navegador ajeno   |
      | tipo de contenido no JSON   |

  @s12
  Scenario: Guardar modificación y evento en una sola transacción
    Given existe un proyecto propio y una precondición vigente
    When confirma una modificación
    Then guarda un único outbox pending ProjectUpdated.v1 con eventId nuevo
    And su payload contiene exactamente eventId, aggregateId, ownerId, occurredAt, schemaVersion, name y type
    And aggregateId es el proyecto, occurredAt es updatedAt y schemaVersion es 1
    And no transporta description ni credenciales

  @s13
  Scenario Outline: Revertir la modificación cuando falla una escritura
    Given existe un proyecto propio y una precondición vigente
    When falla la escritura de <registro> durante la modificación
    Then no cambia ningún campo ni versión del proyecto y no persiste el nuevo evento
    And recibe HTTP 503 con código STORAGE_UNAVAILABLE sin falso éxito
    Examples:
      | registro |
      | proyecto |
      | outbox   |

  @s14
  Scenario: Editar sin depender de disponibilidad del broker
    Given RabbitMQ está caído y existe un proyecto propio
    When guarda una modificación válida con precondición vigente
    Then recibe HTTP 200 tras el commit local
    And conserva un evento ProjectUpdated.v1 pendiente para reintento

  @s15
  Scenario: Publicar el evento actualizado mediante ruta cerrada
    Given existe un ProjectUpdated.v1 válido pendiente y el broker está disponible
    When el publicador confirma su entrega
    Then publica el JSON original persistente con message-id eventId y content-type application/json
    And usa exchange organization.events, routing key project.updated.v1 y cola quorum durable organization.project-updated.v1
    And conserva mandatory, precedencia de devolución, confirm de 5 segundos y política de reintento existentes

  @s16
  Scenario: Conservar la ruta de creación y bloquear tipos desconocidos
    Given existen eventos Created, Updated y un tipo no admitido
    When el publicador los evalúa
    Then ProjectCreated.v1 conserva organization.project-created.v1 y project.created.v1
    And ProjectUpdated.v1 utiliza únicamente su ruta acordada
    And el tipo desconocido permanece bloqueado como INVALID_EVENT sin publicar una ruta arbitraria

  @s17
  Scenario: Precargar y guardar el formulario de edición
    Given el propietario abre directamente /proyectos/{id}/editar
    When termina la consulta del detalle
    Then ve name y description actuales y un control Guardar cambios
    And Cancelar vuelve al detalle
    When modifica los campos y guarda
    Then anuncia Guardando cambios antes de 400 ms y evita envíos duplicados
    And tras HTTP 200 anuncia Proyecto actualizado y muestra los datos confirmados

  @s18
  Scenario Outline: Recuperar fallos sin perder el borrador
    Given el propietario ha modificado los campos del formulario
    When el guardado responde <fallo>
    Then conserva lo escrito y presenta una explicación accesible sin falso éxito
    And puede corregir o reintentar de forma deliberada
    Examples:
      | fallo             |
      | VALIDATION_ERROR  |
      | STORAGE_UNAVAILABLE |
      | error de red      |
      | INTERNAL_ERROR    |

  @s19
  Scenario: Resolver conflicto sin sobrescritura silenciosa
    Given el formulario conserva un borrador y el guardado responde PROJECT_CONFLICT
    When se muestra el conflicto
    Then conserva el borrador y explica que existe una versión más reciente
    And ofrece Recargar versión guardada como acción deliberada
    And no reintenta automáticamente con un ETag nuevo
    When activa Recargar versión guardada
    Then consulta de nuevo el detalle y sustituye campos y ETag por los valores confirmados

  @s20
  Scenario Outline: Retirar datos ante pérdida de acceso
    Given el formulario mostraba datos de un proyecto propio
    When la consulta o guardado responde <fallo>
    Then muestra <mensaje> y retira los datos anteriores
    And ofrece navegación de recuperación sin afirmar que se borraron los proyectos
    Examples:
      | fallo             | mensaje                |
      | UNAUTHENTICATED    | Autenticación requerida |
      | PROJECT_NOT_FOUND | Proyecto no encontrado |

  @s21
  Scenario: Evitar caché y almacenamiento persistente del borrador
    Given el propietario consulta o modifica un proyecto
    When recibe el detalle o resultado de modificación
    Then la respuesta privada incluye Cache-Control no-store
    And la web no persiste proyecto, borrador ni credenciales en almacenamiento del navegador

  @s22
  Scenario: Utilizar la edición de forma accesible y adaptable
    Given el formulario contiene textos largos Unicode
    When se recorre con teclado, táctil, anchos de la matriz UX y zoom real del 200 por ciento
    Then conserva etiquetas, errores asociados, foco visible y orden significativo
    And las acciones principales miden al menos 44 por 44 píxeles CSS
    And no hay solapes, recortes ni desplazamiento horizontal a 320 píxeles CSS
    And se registra la matriz de los 30 principios y los límites de dispositivos realmente probados

  @s23
  Scenario: Recuperar errores de carga sin mostrar otro proyecto
    Given la lectura inicial del formulario está pendiente o falla
    When se presenta el estado
    Then anuncia la espera honestamente o presenta Reintentar según corresponda
    And no permite guardar usando datos o ETag de otro proyecto
    And una respuesta antigua no sustituye la ruta actual

  @s24
  Scenario: Sanitizar los errores internos
    Given ocurre un error interno inesperado durante la modificación
    When la API responde
    Then recibe HTTP 500 con código INTERNAL_ERROR y correlationId
    And no recibe stacktrace, SQL, credenciales ni datos ajenos
