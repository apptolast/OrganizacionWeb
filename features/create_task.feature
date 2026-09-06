@create_task @approved
Feature: Guardar y recuperar tareas pequeñas dentro de un proyecto propio
  Contrato aprobado bajo la autorización global del usuario, tras cierre local de authentication.
  Las longitudes son puntos de código Unicode. Ausente significa propiedad omitida.
  Los errores usan application/problem+json y los códigos públicos indicados.

  @s1
  Scenario: Crear una tarea propia con valores confirmados
    Given una sesión válida, un token CSRF válido y un proyecto propio en idea
    When envío POST a "/api/v1/projects/{projectId}/tasks" como application/json con title "Preparar portada"
    Then recibo HTTP 201 después del guardado con Location "/api/v1/projects/{projectId}/tasks/{id}"
    And la respuesta contiene exactamente id, projectId, title, completionCriterion, estimatedMinutes, status, createdAt y updatedAt
    And el id es un UUID generado por el servidor, status es pending, completionCriterion es "" y estimatedMinutes es null
    And createdAt y updatedAt coinciden con el instante confirmado del servidor
    And existe exactamente una tarea y un evento TaskCreated.v1

  @s2
  Scenario Outline: Admitir títulos dentro de los límites Unicode
    Given un proyecto propio abierto y una sesión válida con CSRF
    When creo una tarea con <cantidad> repeticiones de "<caracter>" como título
    Then recibo HTTP 201 y el título conserva exactamente esos puntos de código
    Examples:
      | cantidad | caracter |
      | 1        | a        |
      | 160      | a        |
      | 160      | 🚀       |

  @s3
  Scenario: Recortar espacios exteriores sin alterar el título interior
    Given el título contiene U+0020, U+00A0 y U+2003 alrededor de "Mi  Aé tarea"
    When creo la tarea en un proyecto propio abierto
    Then el título confirmado es exactamente "Mi  Aé tarea" sin normalizar mayúsculas ni secuencias Unicode

  @s4
  Scenario Outline: Rechazar títulos inválidos
    Given una sesión válida con CSRF y un proyecto propio abierto
    When envío title como <entrada>
    Then recibo HTTP 400 VALIDATION_ERROR con error en title y código <codigo>
    And no se guarda tarea ni evento
    Examples:
      | entrada                    | codigo       |
      | ausente                    | REQUIRED     |
      | null                       | REQUIRED     |
      | cadena vacía               | REQUIRED     |
      | sólo Unicode White_Space   | REQUIRED     |
      | número                     | INVALID_TYPE |
      | array                      | INVALID_TYPE |
      | 161 puntos de código       | TOO_LONG     |

  @s5
  Scenario Outline: Normalizar o conservar el criterio opcional
    Given una sesión válida con CSRF y un proyecto propio abierto
    When creo una tarea con completionCriterion <entrada>
    Then recibo HTTP 201 y completionCriterion es <salida>
    Examples:
      | entrada                          | salida                           |
      | ausente                          | cadena vacía                     |
      | null                             | cadena vacía                     |
      | 2000 puntos de código Unicode    | el texto exacto enviado          |
      | texto con espacios y saltos      | el texto exacto enviado          |

  @s6
  Scenario Outline: Validar los campos opcionales
    Given una sesión válida con CSRF y un proyecto propio abierto
    When creo una tarea con <campo> como <entrada>
    Then recibo HTTP 400 VALIDATION_ERROR con error en <campo> y código <codigo>
    And no se guarda tarea ni evento
    Examples:
      | campo               | entrada                 | codigo       |
      | completionCriterion | 2001 puntos de código   | TOO_LONG     |
      | completionCriterion | número                  | INVALID_TYPE |
      | estimatedMinutes    | cadena                  | INVALID_TYPE |
      | estimatedMinutes    | booleano                | INVALID_TYPE |
      | estimatedMinutes    | 1.5                     | INVALID_TYPE |
      | estimatedMinutes    | 0                       | OUT_OF_RANGE |
      | estimatedMinutes    | 1441                    | OUT_OF_RANGE |

  @s7
  Scenario Outline: Guardar una estimación opcional sin contabilizar trabajo
    Given una sesión válida con CSRF y un proyecto propio abierto
    When creo una tarea con estimatedMinutes <entrada>
    Then recibo HTTP 201 con estimatedMinutes <salida>
    And no se registra tiempo realizado ni se programa ningún bloque
    Examples:
      | entrada | salida |
      | ausente | null   |
      | null    | null   |
      | 1       | 1      |
      | 1440    | 1440   |

  @s8
  Scenario Outline: Rechazar cuerpos no permitidos
    Given una sesión válida con CSRF y un proyecto propio abierto
    When envío una creación con <defecto>
    Then recibo HTTP <estado> con código <codigo>
    And no se guarda tarea ni evento
    Examples:
      | defecto                         | estado | codigo                 |
      | JSON truncado                   | 400    | MALFORMED_JSON         |
      | dos documentos JSON concatenados| 400    | MALFORMED_JSON         |
      | JSON raíz array                 | 400    | VALIDATION_ERROR       |
      | JSON raíz null                  | 400    | VALIDATION_ERROR       |
      | claves JSON duplicadas          | 400    | MALFORMED_JSON         |
      | propiedad id enviada            | 400    | VALIDATION_ERROR       |
      | propiedad ownerId enviada       | 400    | VALIDATION_ERROR       |
      | propiedad status enviada        | 400    | VALIDATION_ERROR       |
      | propiedad createdAt enviada     | 400    | VALIDATION_ERROR       |
      | propiedad desconocida           | 400    | VALIDATION_ERROR       |
      | contenido distinto de JSON      | 415    | UNSUPPORTED_MEDIA_TYPE |

  @s9
  Scenario Outline: Exigir sesión en todos los recursos de tareas
    Given la sesión está ausente o vencida
    When solicito <operacion>
    Then recibo HTTP 401 UNAUTHENTICATED sin datos privados ni desafío Basic
    And no se modifica ninguna tarea ni evento
    Examples:
      | operacion          |
      | crear una tarea    |
      | listar tareas      |
      | consultar detalle  |

  @s10
  Scenario Outline: Proteger la creación mediante CSRF y origen
    Given una sesión válida y un proyecto propio abierto
    When envío una creación con <defecto>
    Then recibo HTTP 403 con código <codigo>
    And no se guarda tarea ni evento
    Examples:
      | defecto                            | codigo           |
      | token CSRF ausente                 | CSRF_INVALID     |
      | token CSRF incorrecto              | CSRF_INVALID     |
      | origen extranjero y CSRF válido    | UNTRUSTED_ORIGIN |

  @s11
  Scenario Outline: No revelar recursos inexistentes o ajenos
    Given una sesión válida con CSRF
    When solicito <recurso>
    Then recibo HTTP 404 RESOURCE_NOT_FOUND con el mismo mensaje público "No se ha encontrado el recurso."
    And no recibo datos privados ni modifico tareas o eventos
    Examples:
      | recurso                                  |
      | crear en un proyecto inexistente         |
      | crear en un proyecto ajeno               |
      | listar un proyecto inexistente           |
      | listar un proyecto ajeno                 |
      | detalle de una tarea inexistente         |
      | detalle de una tarea ajena               |
      | tarea propia bajo otro proyecto propio   |

  @s12
  Scenario Outline: Rechazar identificadores mal formados
    Given una sesión válida con CSRF
    When solicito un recurso de tareas con <campo> que no es un UUID completo
    Then recibo HTTP 400 VALIDATION_ERROR con error en <campo> y código INVALID_FORMAT
    Examples:
      | campo     |
      | projectId |
      | taskId    |

  @s13
  Scenario Outline: Crear sólo dentro de proyectos abiertos
    Given un proyecto propio en <estado>
    When creo una tarea válida
    Then recibo <resultado>
    And el estado, capacidad y ETag del proyecto no cambian por la creación
    Examples:
      | estado    | resultado                           |
      | idea      | HTTP 201                            |
      | active    | HTTP 201                            |
      | paused    | HTTP 201                            |
      | completed | HTTP 409 PROJECT_COMPLETED          |

  @s14
  Scenario Outline: Resolver la carrera entre crear y terminar sin resultados parciales
    Given crear una tarea y terminar el mismo proyecto compiten con solicitudes válidas
    When <primera> confirma antes de que la otra operación decida
    Then <resultado>
    Examples:
      | primera          | resultado                                                                 |
      | terminar         | crear recibe 409 PROJECT_COMPLETED sin tarea ni TaskCreated                |
      | crear            | crear recibe 201 y terminar puede confirmar conservando la tarea pending   |

  @s15
  Scenario Outline: Mantener atomicidad ante un fallo de guardado
    Given una sesión válida y un proyecto propio abierto
    And el guardado de <registro> falla o no confirma ninguna fila
    When creo una tarea válida
    Then recibo HTTP 503 STORAGE_UNAVAILABLE sin Location ni éxito
    And no queda tarea ni evento parcial y el proyecto conserva sus valores anteriores
    Examples:
      | registro |
      | tarea    |
      | evento   |

  @s16
  Scenario: Crear mientras el broker está inaccesible
    Given el publicador está habilitado y el broker no está disponible
    When creo una tarea válida en un proyecto propio abierto
    Then recibo HTTP 201 con la tarea confirmada
    And su evento permanece pendiente para publicación posterior sin perder identidad

  @s17
  Scenario: Publicar el evento mínimo confirmado
    Given existe un TaskCreated.v1 pendiente y el broker está disponible
    When el publicador procesa el evento
    Then entrega un mensaje persistente a organization.task-created.v1 mediante task.created.v1
    And el JSON contiene exactamente eventId, aggregateId, ownerId, occurredAt, schemaVersion, type, taskId y title
    And aggregateId identifica el proyecto, taskId identifica la tarea y title es el título confirmado
    And no incluye completionCriterion ni estimatedMinutes
    And aplica confirmación, entrega obligatoria y reintentos con la semántica vigente

  @s18
  Scenario Outline: Validar el nuevo evento sin ampliar tipos admitidos
    Given un evento pendiente con <defecto>
    When el publicador lo examina
    Then lo bloquea con código <codigo> sin enviarlo
    Examples:
      | defecto                             | codigo            |
      | tipo desconocido                    | UNSUPPORTED_EVENT |
      | versión desconocida                 | UNSUPPORTED_EVENT |
      | TaskCreated con taskId inválido     | INVALID_EVENT     |
      | TaskCreated con título demasiado largo | INVALID_EVENT  |
      | TaskCreated con campo adicional     | INVALID_EVENT     |

  @s19
  Scenario: Consultar una colección vacía propia
    Given un proyecto propio sin tareas
    When consulto GET "/api/v1/projects/{projectId}/tasks"
    Then recibo HTTP 200 con exactamente items vacío y nextCursor null
    And no se modifica el proyecto, la capacidad ni eventos

  @s20
  Scenario: Ordenar y acotar la primera página
    Given un proyecto propio con 21 tareas y varias comparten createdAt
    When consulto su lista sin cursor
    Then recibo los primeros 20 elementos por createdAt descendente y por id descendente en los empates
    And cada elemento contiene los ocho campos públicos de tarea y nextCursor no es null

  @s21
  Scenario: Continuar sin repetir ante una creación posterior
    Given conservo el cursor de los primeros 20 de 21 elementos y se crea una tarea más reciente
    When consulto la continuación con ese cursor
    Then recibo sólo la tarea restante más antigua y nextCursor null
    And no recibo la nueva tarea ni repito las anteriores

  @s22
  Scenario Outline: Rechazar cursores y consultas incompatibles
    Given una sesión válida y un proyecto propio
    When consulto su lista con <defecto>
    Then recibo HTTP 400 VALIDATION_ERROR con error en <campo>
    And no recibo tareas de otra colección
    Examples:
      | defecto                                  | campo  |
      | cursor vacío                             | cursor |
      | cursor mal codificado                    | cursor |
      | cursor con campos ausentes o adicionales | cursor |
      | cursor con fecha inválida o fuera de rango| cursor |
      | cursor con UUID inválido                 | cursor |
      | cursor de otro proyecto                  | cursor |
      | cursor repetido                          | cursor |
      | parámetro limit                          | query  |
      | parámetro desconocido                    | query  |

  @s23
  Scenario: Consultar el detalle confirmado mediante su Location
    Given una tarea propia guardada con título Unicode, criterio y estimación
    When consulto por API la Location recibida al crearla
    Then recibo HTTP 200 con exactamente los ocho campos y valores confirmados
    And la estimación sigue separada del trabajo realizado

  @s24
  Scenario Outline: Evitar caché de información privada
    Given una consulta de tareas produce <resultado>
    When recibo la respuesta
    Then Cache-Control incluye no-store
    Examples:
      | resultado |
      | 200       |
      | 400       |
      | 401       |
      | 404       |
      | 503       |

  @s25
  Scenario: Distinguir indisponibilidad de una colección vacía
    Given la lectura de tareas no está disponible
    When consulto una lista propia
    Then recibo HTTP 503 STORAGE_UNAVAILABLE sin items vacío ficticio

  @s26
  Scenario: Crear desde la sección Tareas del proyecto
    Given veo el detalle de un proyecto abierto y el formulario de tareas
    When guardo un título válido con criterio y estimación opcionales
    Then se anuncia la confirmación accesiblemente y aparece la tarea guardada
    And la estimación se presenta como planificación y no como tiempo realizado
    And se explica que terminar el proyecto conservará las tareas pendientes

  @s27
  Scenario Outline: Conservar el borrador sin reenvío automático
    Given he escrito un borrador de tarea
    When el envío obtiene <resultado>
    Then el borrador permanece y se muestra una acción deliberada para continuar
    And no se envía otra creación automáticamente ni se muestra éxito sin confirmación
    Examples:
      | resultado                  |
      | validación rechazada       |
      | almacenamiento inaccesible |
      | respuesta de red incierta  |

  @s28
  Scenario: Evitar envíos simultáneos desde el formulario
    Given hay una creación en curso
    When intento enviar otra vez desde el mismo formulario
    Then sólo permanece una solicitud y se anuncia el estado de guardado

  @s29
  Scenario Outline: Mantener independiente la lista de tareas
    Given el detalle del proyecto está disponible y la lista de tareas está <estado>
    When abro la sección Tareas
    Then veo <salida> y las acciones actuales del proyecto siguen disponibles
    Examples:
      | estado   | salida                                     |
      | cargando | un estado de carga accesible               |
      | vacía    | una explicación y el formulario de creación |
      | fallida  | un error y una acción de reintento          |

  @s30
  Scenario: Impedir nuevas tareas en un proyecto terminado desde la interfaz
    Given veo un proyecto completed con tareas pendientes
    When abro su sección Tareas
    Then puedo consultar las tareas y veo que debo reabrir en pausa para añadir trabajo
    And no se inicia creación ni se completan tareas automáticamente

  @s31
  Scenario: Descartar respuestas tardías de otra navegación
    Given hay una consulta de tareas pendiente del proyecto anterior
    When navego a otro proyecto antes de que termine
    Then la respuesta anterior no reemplaza la lista del nuevo proyecto

  @s32
  Scenario: Retirar datos privados al perder la sesión
    Given veo tareas y tengo un borrador
    When la sesión deja de estar autenticada
    Then se retiran lista y borrador privados y se presenta el acceso
    And ninguna respuesta tardía restaura esos datos

  @s33
  Scenario: Operar el formulario y la lista con teclado
    Given navego sin ratón en la sección Tareas
    When recorro campos, envío inválido, paginación y reintento
    Then cada control tiene nombre accesible, orden lógico y foco visible
    And los errores se asocian al campo y el foco permite corregir el primer error

  @s34
  Scenario Outline: Conservar uso responsive y zoom
    Given uso la sección Tareas a <ancho> píxeles y zoom <zoom>
    When consulto la lista y abro el formulario con un título largo
    Then no hay desplazamiento horizontal del documento ni controles inaccesibles
    And texto, errores y acciones permanecen legibles y utilizables
    And se verifica la matriz completa de 12 anchos de docs/ux-requirements.md y ambos lados de cada breakpoint
    And se comprueban alturas reducidas, orientación horizontal y áreas táctiles principales de al menos 44 × 44 píxeles CSS
    And se prueba zoom nativo al 200 % con ancho interior de 320 píxeles CSS, sin sustituirlo sólo por emulación
    And el informe revisa las 30 filas de principios de docs/ux-requirements.md con evidencia, pendiente o no aplicable justificado
    Examples:
      | ancho | zoom |
      | 320   | 100% |
      | 768   | 100% |
      | 1440  | 200% |
      | 2560  | 100% |

  @s35
  Scenario: Recuperar tareas y navegar por sus páginas después de recargar la web
    Given un proyecto propio tiene 21 tareas confirmadas y su detalle está abierto
    When recargo la web
    Then veo las primeras 20 tareas guardadas y una acción accesible para consultar la página restante
    And el detalle y las acciones del proyecto siguen disponibles
    And no se vuelve a enviar ninguna creación al recargar
