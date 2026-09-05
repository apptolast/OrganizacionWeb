Feature: Capturar un proyecto propio como idea persistente
  Como persona autenticada quiero guardar una idea de proyecto
  para organizarla después sin perderla ni confundir un envío con un guardado.
  Contrato propuesto: pendiente de aprobación humana; no autoriza todavía TDD.
  En los ejemplos, ausente significa propiedad omitida y null es el valor JSON null.
  Las longitudes se cuentan en puntos de código Unicode.

  Background:
    Given no existen proyectos ni eventos de creación para esta interacción
    And la persona autenticada es "persona-a"
    And el almacenamiento está disponible

  @s1
  Scenario: Crear y confirmar una idea con identidad y fechas del servidor
    Given el reloj servidor marca "2026-09-05T12:00:00Z"
    When envío POST a "/api/v1/projects" como application/json con nombre "Zenit" y descripción "Preparar la web"
    Then recibo HTTP 201 después de confirmar la transacción
    And la respuesta contiene un id único y ownerId "persona-a"
    And contiene name "Zenit", description "Preparar la web" y status "idea"
    And createdAt y updatedAt son "2026-09-05T12:00:00Z"
    And Location es "/api/v1/projects/{id}" con el id confirmado
    And queda guardado exactamente un proyecto con esos valores

  @s2
  Scenario Outline: Aceptar límites válidos del nombre sin confundir puntos de código con UTF-16
    When creo un proyecto con un nombre formado por <cantidad> repeticiones de "<caracter>"
    Then recibo HTTP 201
    And el nombre guardado conserva exactamente los <cantidad> puntos de código enviados

    Examples:
      | cantidad | caracter |
      | 1        | a        |
      | 120      | a        |
      | 1        | 🚀       |
      | 120      | 🚀       |

  @s3
  Scenario: Recortar únicamente espacios exteriores Unicode y conservar el interior
    Given el nombre empieza con U+0020, U+00A0 y U+2003 y termina con U+2003, U+00A0 y U+0020
    And entre esos espacios contiene exactamente "Mi  Proyecto"
    When creo el proyecto con ese nombre
    Then recibo HTTP 201
    And el nombre guardado es exactamente "Mi  Proyecto"

  @s4
  Scenario: Conservar mayúsculas y secuencias Unicode sin normalizarlas
    Given el nombre contiene los puntos de código U+0041, U+0065 y U+0301
    When creo el proyecto con ese nombre
    Then recibo HTTP 201
    And el nombre guardado contiene exactamente U+0041, U+0065 y U+0301

  @s5
  Scenario Outline: Rechazar nombres ausentes, vacíos o de tipo incorrecto
    When envío la propiedad name como <entrada>
    Then recibo HTTP 400 con code "VALIDATION_ERROR"
    And errors contiene field "name" y code "<codigo>"
    And no se guarda ningún proyecto ni evento de creación

    Examples:
      | entrada                          | codigo       |
      | ausente                          | REQUIRED     |
      | null                             | REQUIRED     |
      | string vacío                     | REQUIRED     |
      | solo U+0020, U+00A0 y U+2003       | REQUIRED     |
      | número 42                        | INVALID_TYPE |
      | booleano true                    | INVALID_TYPE |
      | array vacío                      | INVALID_TYPE |
      | objeto vacío                     | INVALID_TYPE |

  @s6
  Scenario Outline: Rechazar un nombre por encima del límite después del recorte
    When creo un proyecto con espacios exteriores y un nombre de 121 repeticiones de "<caracter>"
    Then recibo HTTP 400 con code "VALIDATION_ERROR"
    And errors contiene field "name" y code "TOO_LONG"
    And no se guarda ningún proyecto ni evento de creación

    Examples:
      | caracter |
      | a        |
      | 🚀       |

  @s7
  Scenario Outline: Normalizar la descripción opcional vacía
    When creo el proyecto "Idea" con description <entrada>
    Then recibo HTTP 201
    And description en respuesta y proyecto guardado es el string vacío

    Examples:
      | entrada      |
      | ausente      |
      | null         |
      | string vacío |

  @s8
  Scenario Outline: Aceptar el límite de descripción en puntos de código
    When creo el proyecto "Idea" con descripción de 4000 repeticiones de "<caracter>"
    Then recibo HTTP 201
    And la descripción guardada conserva exactamente los 4000 puntos de código enviados

    Examples:
      | caracter |
      | a        |
      | 🚀       |

  @s9
  Scenario: Conservar espacios y saltos de línea en la descripción
    Given la descripción contiene dos espacios, "Primera", un salto LF, "Segunda" y dos espacios finales
    When creo el proyecto "Idea" con esa descripción
    Then recibo HTTP 201
    And la descripción guardada es idéntica a la enviada

  @s10
  Scenario Outline: Rechazar una descripción inválida
    When creo el proyecto "Idea" con description <entrada>
    Then recibo HTTP 400 con code "VALIDATION_ERROR"
    And errors contiene field "description" y code "<codigo>"
    And no se guarda ningún proyecto ni evento de creación

    Examples:
      | entrada                         | codigo       |
      | string de 4001 letras a          | TOO_LONG     |
      | string de 4001 caracteres 🚀     | TOO_LONG     |
      | número 42                       | INVALID_TYPE |
      | booleano true                   | INVALID_TYPE |
      | array vacío                     | INVALID_TYPE |
      | objeto vacío                    | INVALID_TYPE |

  @s11
  Scenario: Permitir proyectos distintos con el mismo nombre
    Given ya existe un proyecto propio llamado "Idea"
    When creo otro proyecto llamado "Idea"
    Then recibo HTTP 201 con un id distinto del proyecto existente
    And existen dos proyectos propios llamados "Idea"
    And el proyecto existente no cambia

  @s12
  Scenario Outline: Rechazar propiedades desconocidas y manipulación de propietario o estado
    When creo el proyecto "Idea" incluyendo la propiedad adicional "<campo>" con valor "<valor>"
    Then recibo HTTP 400 con code "VALIDATION_ERROR"
    And errors contiene field "<campo>" y code "UNKNOWN_FIELD"
    And no se guarda ningún proyecto ni evento de creación

    Examples:
      | campo   | valor     |
      | ownerId | persona-b |
      | status  | activo    |
      | extra   | valor     |

  @s13
  Scenario Outline: Exigir identidad autenticada
    Given la petición tiene <autenticacion>
    When solicito crear el proyecto "Idea"
    Then recibo HTTP 401
    And no se guarda ningún proyecto ni evento de creación

    Examples:
      | autenticacion         |
      | ninguna credencial    |
      | una credencial inválida |

  @s14
  Scenario: Rechazar JSON mal formado
    When envío POST a "/api/v1/projects" como application/json con cuerpo JSON incompleto
    Then recibo HTTP 400 con code "MALFORMED_JSON"
    And no se guarda ningún proyecto ni evento de creación

  @s15
  Scenario: Rechazar un tipo de contenido no soportado
    When envío POST a "/api/v1/projects" como text/plain con cuerpo "Idea"
    Then recibo HTTP 415
    And no se guarda ningún proyecto ni evento de creación

  @s16
  Scenario: Registrar el hecho de creación junto al proyecto confirmado
    When creo el proyecto con nombre "  Idea  " y descripción "Contenido privado"
    Then recibo HTTP 201
    And queda un único evento pendiente "ProjectCreated.v1" con eventId único
    And su aggregateId coincide con el id del proyecto y ownerId es "persona-a"
    And occurredAt coincide con createdAt y schemaVersion es 1
    And el nombre del evento es "Idea"
    And el evento no contiene la descripción del proyecto

  @s17
  Scenario Outline: Revertir ambas escrituras cuando falla el almacenamiento de una de ellas
    Given el almacenamiento devuelve un fallo de disponibilidad al guardar <elemento>
    When creo el proyecto "Idea"
    Then recibo HTTP 503 con code "STORAGE_UNAVAILABLE"
    And no queda guardado ningún proyecto ni evento de esta creación
    And no se comunica una creación confirmada

    Examples:
      | elemento                  |
      | el proyecto               |
      | el evento de creación     |

  @s18
  Scenario: Responder a un fallo interno sin exponer detalles ni dejar estado parcial
    Given la creación provoca un error interno inesperado antes de confirmar la transacción
    When creo el proyecto "Idea"
    Then recibo HTTP 500 con code "INTERNAL_ERROR"
    And la respuesta proporciona una referencia de correlación
    And no contiene stack traces, credenciales ni detalles internos
    And no queda guardado ningún proyecto ni evento de esta creación

  @s19
  Scenario: Confirmar el proyecto aunque el broker no esté disponible
    Given el broker RabbitMQ no está disponible
    When creo el proyecto "Idea"
    Then recibo HTTP 201
    And el proyecto está guardado
    And su evento de creación permanece guardado y pendiente de entrega

  @s20
  Scenario Outline: Conservar los datos confirmados tras reiniciar o recargar
    Given el proyecto "Idea" y su evento se han confirmado con HTTP 201
    When <accion>
    Then el proyecto conserva el mismo id, propietario, nombre, descripción, estado y fechas en el almacenamiento
    And su evento de creación sigue registrado con el mismo eventId

    Examples:
      | accion                      |
      | reinicio el backend         |
      | recargo la página           |

  @s21
  Scenario Outline: Mostrar errores con un contrato estructurado y mensajes comprensibles
    Given se ha solicitado una creación que produce <fallo>
    When recibo la respuesta de error
    Then el Content-Type de respuesta es "application/problem+json"
    And el cuerpo contiene type, title, status y code
    And status es <estado> y code es "<codigo>"
    And el mensaje para la persona está en español y no contiene stack traces

    Examples:
      | fallo                         | estado | codigo              |
      | nombre vacío                  | 400    | VALIDATION_ERROR    |
      | JSON incompleto               | 400    | MALFORMED_JSON      |
      | almacenamiento no disponible  | 503    | STORAGE_UNAVAILABLE |
      | error interno inesperado      | 500    | INTERNAL_ERROR      |

  @s22
  Scenario: Mostrar una confirmación únicamente al recibir éxito del servidor
    Given el formulario enviado contiene nombre "  Idea  " y una descripción
    And todavía no ha llegado respuesta del servidor
    When llega HTTP 201 con la representación confirmada
    Then el formulario deja de indicar guardando
    And se muestra la confirmación de guardado con nombre "Idea" e id confirmado
    And antes de esa respuesta no se había mostrado éxito

  @s23
  Scenario: Evitar envíos repetidos mientras la petición está pendiente
    Given hay una petición de creación del formulario pendiente de respuesta
    When intento enviar de nuevo el mismo formulario
    Then la acción de enviar está deshabilitada
    And esta interacción mantiene una única petición de creación
    And la interfaz indica que está guardando

  @s24
  Scenario: Conservar el formulario al recibir un error de validación
    Given he enviado nombre "Idea" y una descripción de 4001 puntos de código
    When llega HTTP 400 con error de description "TOO_LONG"
    Then ambos campos conservan exactamente los valores escritos
    And el error aparece asociado a la descripción
    And puedo editar los campos y volver a enviar
    And no se muestra una confirmación de guardado

  @s25
  Scenario: Explicar un resultado incierto de red sin duplicar automáticamente el POST
    Given he enviado nombre "Idea" y descripción "Continuar mañana"
    When la conexión se interrumpe antes de recibir una respuesta
    Then ambos campos conservan exactamente los valores escritos
    And se indica que no se puede confirmar si el proyecto se guardó
    And no se muestra éxito ni se asegura que la creación no ocurrió
    And no se reintenta automáticamente el POST
    And los campos vuelven a ser editables

  @s26
  Scenario: Representar el contenido de proyecto como texto plano seguro
    Given el formulario contiene nombre "<b>Idea</b>" y descripción "<script>alert(1)</script>"
    When recibo la confirmación de creación y se muestra el proyecto
    Then el nombre y la descripción se muestran literalmente como texto
    And no se ejecuta ningún script ni se interpretan las etiquetas como elementos HTML

  @s27
  Scenario Outline: Mantener el formulario utilizable en distintos tamaños de pantalla
    Given la ventana tiene <ancho> CSS px de ancho y zoom <zoom>
    When abro el formulario de creación
    Then nombre, descripción y enviar son visibles o alcanzables mediante desplazamiento vertical
    And ninguna acción esencial requiere desplazamiento horizontal
    And los campos tienen etiquetas asociadas y el foco de teclado es visible

    Examples:
      | ancho | zoom |
      | 320   | 100% |
      | 768   | 100% |
      | 1440  | 100% |
      | 1440  | 200% |

  @s28
  Scenario: Crear mediante teclado y anunciar el resultado
    Given el formulario tiene nombre válido y descripción opcional
    And navego usando únicamente el teclado
    When activo enviar desde el teclado y recibo HTTP 201
    Then se crea un único proyecto
    And la confirmación es anunciada a las tecnologías de asistencia
    And el foco permanece visible en un control existente
