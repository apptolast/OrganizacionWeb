@read_projects @approved
Feature: Recuperar y consultar los proyectos propios guardados
  Como propietario quiero ver mis proyectos y sus detalles después de volver a la web.
  Contrato aprobado por el usuario: «Si las apruebo todas» (5 de septiembre de 2026).

  @s1
  Scenario: Listar solo proyectos del propietario autenticado
    Given existen proyectos de dos propietarios
    When el primer propietario consulta sus proyectos
    Then recibe HTTP 200 con únicamente sus proyectos
    And cada resumen contiene id, name, status, createdAt y updatedAt
    And no recibe description ni ownerId en el resumen

  @s2
  Scenario: Consultar una colección vacía
    Given el propietario autenticado no tiene proyectos
    When consulta sus proyectos
    Then recibe HTTP 200 con items vacío y nextCursor null

  @s3
  Scenario Outline: Exigir autenticación en cada consulta
    Given la solicitud no tiene credenciales válidas
    When consulta <recurso>
    Then recibe HTTP 401 con código UNAUTHENTICATED
    And no recibe nombres ni datos de proyectos
    Examples:
      | recurso                     |
      | la lista de proyectos       |
      | el detalle de un proyecto   |

  @s4
  Scenario: Ordenar y acotar la primera página
    Given hay 21 proyectos propios y varios comparten createdAt
    When consulta la primera página
    Then recibe los 20 primeros por createdAt descendente y por id descendente en los empates
    And recibe un nextCursor no vacío

  @s5
  Scenario: Continuar desde el cursor sin repetir proyectos
    Given se consultaron los primeros 20 de 21 proyectos propios
    When consulta la página indicada por nextCursor
    Then recibe únicamente el proyecto restante
    And nextCursor es null

  @s6
  Scenario: Mantener estable la continuación ante una nueva creación
    Given el propietario conserva un cursor de la primera página
    And se confirmó después un proyecto más reciente que todos los ya listados
    When consulta la página indicada por ese cursor
    Then no repite los proyectos anteriores ni incluye el proyecto nuevo
    And devuelve los siguientes proyectos propios más antiguos

  @s7
  Scenario Outline: Rechazar parámetros de consulta inválidos
    Given el propietario está autenticado
    When consulta la lista con <defecto>
    Then recibe HTTP 400 con código VALIDATION_ERROR y error en <campo>
    And no modifica proyectos ni eventos
    Examples:
      | defecto                        | campo  |
      | cursor mal codificado          | cursor |
      | cursor sin los campos exigidos | cursor |
      | cursor con fecha inválida      | cursor |
      | cursor con id inválido         | cursor |
      | cursor vacío                   | cursor |
      | cursor repetido                | cursor |
      | parámetro limit no admitido    | query  |

  @s8
  Scenario: Un cursor de otra colección no permite leer proyectos ajenos
    Given el propietario autenticado presenta un cursor válido obtenido por otro propietario
    When consulta la página indicada por ese cursor
    Then solo recibe proyectos del propietario autenticado situados después del cursor en el orden descendente definido (más antiguos)
    And la respuesta no confirma identidades ni nombres del otro propietario

  @s9
  Scenario: Consultar el detalle completo propio
    Given existe un proyecto propio con descripción y nombre Unicode
    When el propietario consulta su detalle por id
    Then recibe HTTP 200 con id, ownerId, name, description, status, createdAt y updatedAt originales
    And conserva exactamente el texto y los instantes almacenados

  @s10
  Scenario Outline: No revelar la existencia de un proyecto ajeno
    Given el propietario está autenticado
    When consulta un id <caso>
    Then recibe HTTP 404 con código PROJECT_NOT_FOUND y el mismo mensaje público
    And no recibe datos del proyecto
    Examples:
      | caso        |
      | inexistente |
      | ajeno       |

  @s11
  Scenario: Rechazar un identificador de detalle mal formado
    Given el propietario está autenticado
    When consulta el detalle con un id que no es UUID
    Then recibe HTTP 400 con código VALIDATION_ERROR y error en id

  @s12
  Scenario Outline: Informar de almacenamiento indisponible sin falso vacío
    Given PostgreSQL no permite completar la lectura
    When el propietario consulta <recurso>
    Then recibe HTTP 503 con código STORAGE_UNAVAILABLE
    And la respuesta no presenta una colección vacía ni un proyecto ficticio
    Examples:
      | recurso                   |
      | la lista de proyectos     |
      | el detalle de un proyecto |

  @s13
  Scenario: Las consultas no escriben datos
    Given hay proyectos y eventos conservados en PostgreSQL
    When el propietario completa consultas de lista y detalle
    Then proyectos y eventos conservan exactamente sus valores y cantidades
    And la lectura no publica mensajes adicionales

  @s14
  Scenario: Recuperar los proyectos al volver a la web
    Given el propietario guardó un proyecto y cerró la página
    When abre de nuevo la lista de proyectos
    Then ve ese proyecto obtenido de la API persistente
    And ve nombre, estado e instante de creación sin necesitar el resultado local del formulario

  @s15
  Scenario: Orientar el primer uso vacío
    Given la API confirmó una colección vacía
    When se presenta la lista
    Then se muestra "Todavía no tienes proyectos"
    And existe una acción "Crear proyecto" que abre el formulario existente
    And no se muestran proyectos de demostración ni métricas inventadas

  @s16
  Scenario: Mostrar espera honesta
    Given la lectura de proyectos todavía no ha terminado
    When se abre la lista
    Then aparece un estado "Cargando proyectos" anunciado de forma accesible antes de 400 ms
    And no se anuncia colección vacía ni éxito antes de responder la API

  @s17
  Scenario: Recuperar una lectura fallida
    Given la lista muestra un error de red o almacenamiento y una acción Reintentar
    And la API vuelve a estar disponible
    When se activa Reintentar
    Then se consulta de nuevo la misma página
    And se muestran sus proyectos y desaparece el error

  @s18
  Scenario: Navegar a proyectos más antiguos
    Given la página visible tiene nextCursor
    When se activa "Más antiguos"
    Then la URL representa la página solicitada y se muestran sus proyectos
    And el foco pasa al título de la lista tras recibir la respuesta
    And existe una acción "Volver al inicio"

  @s19
  Scenario: Reconocer el final de la colección
    Given la API devuelve una página con nextCursor null
    When se presenta la lista
    Then no hay acción activa para solicitar proyectos más antiguos
    And si la página no es la primera sigue disponible "Volver al inicio"

  @s20
  Scenario: Abrir detalles y conservar el texto como texto
    Given la lista contiene un proyecto cuyo nombre y descripción incluyen marcado HTML y Unicode
    When se abre el enlace de su nombre
    Then la URL identifica el proyecto y se obtiene su detalle desde la API
    And nombre y descripción se muestran como texto sin ejecutar código ni interpretar HTML
    And se muestran estado y fechas con una zona horaria indicada
    And existe un enlace "Volver a proyectos" a la primera página

  @s21
  Scenario: Presentar un detalle no disponible
    Given la API respondió PROJECT_NOT_FOUND al detalle solicitado
    When se presenta el resultado
    Then se muestra "Proyecto no encontrado"
    And existe un enlace "Volver a proyectos"
    And no se conserva en pantalla el detalle de otro proyecto consultado anteriormente

  @s22
  Scenario: Evitar que una respuesta antigua sustituya la navegación actual
    Given hay una lectura anterior pendiente y el usuario ya navegó a otra página o detalle
    When llega la respuesta de la lectura anterior
    Then no reemplaza el contenido ni el estado de la ruta actual

  @s23
  Scenario Outline: Conservar lectura y acciones al cambiar de ancho
    Given hay nombres de 120 puntos de código y descripción de 4000 con palabras largas
    When se muestra <vista> con ancho <ancho> píxeles CSS
    Then no hay solapes, recortes ni desplazamiento horizontal de página
    And permanecen legibles los textos y disponibles todos los controles
    And el orden de lectura y teclado conserva su significado
    Examples:
      | vista   | ancho |
      | lista   | 320   |
      | lista   | 768   |
      | lista   | 1440  |
      | detalle | 320   |
      | detalle | 820   |
      | detalle | 2560  |

  @s24
  Scenario: Operar con teclado y ayudas técnicas
    Given la lista y su detalle están disponibles
    When se recorren sus acciones usando solo teclado
    Then cada acción tiene nombre accesible y foco visible no oculto
    And el orden de foco sigue el orden de lectura sin trampas
    And encabezados, lista, enlaces y anuncios de espera o error tienen semántica accesible

  @s25
  Scenario: Reorganizar contenido con zoom real
    Given se muestra la lista o el detalle en un navegador con zoom real de 200 por ciento
    When se recorre el contenido y sus acciones
    Then todo el contenido y acciones siguen accesibles sin solapes ni recortes
    And el reflow a 320 píxeles CSS no introduce desplazamiento horizontal de página

  @s26
  Scenario: Permitir uso táctil sin hover
    Given se muestra la lista o el detalle en pantalla táctil
    When se activan las acciones principales
    Then sus áreas interactivas miden al menos 44 por 44 píxeles CSS
    And ninguna acción requiere hover ni arrastre

  @s27
  Scenario: Presentar una autenticación vencida sin datos antiguos
    Given la web muestra datos propios de una lectura anterior
    And la siguiente lectura responde HTTP 401
    When se presenta esa respuesta
    Then se retiran los datos anteriores y se muestra "Autenticación requerida"
    And no se informa que los proyectos se hayan borrado

  @s28
  Scenario Outline: Informar de un error inesperado de lectura
    Given ocurre un error interno al consultar <recurso>
    When se responde a la consulta autenticada
    Then recibe HTTP 500 con código INTERNAL_ERROR y correlationId
    And no recibe stacktrace, SQL, credenciales ni datos ajenos
    Examples:
      | recurso |
      | lista   |
      | detalle |

  @s29
  Scenario Outline: Evitar caché de consultas privadas
    Given el propietario está autenticado
    When consulta <recurso>
    Then la respuesta incluye Cache-Control no-store
    And la web no copia los datos a almacenamiento persistente del navegador
    Examples:
      | recurso |
      | lista   |
      | detalle |

  @s30
  Scenario: Recuperar un detalle mediante enlace directo
    Given existe un proyecto propio guardado
    When se abre directamente o recarga su URL de detalle
    Then se consulta la API autenticada y aparece su detalle completo
    And no es necesario haber visitado antes la lista

  @s31
  Scenario Outline: Presentar un error de detalle recuperable
    Given la lectura del detalle termina con <fallo>
    When se presenta el resultado
    Then aparece un mensaje comprensible y una acción Reintentar
    And no se muestra un detalle ficticio ni datos de otra ruta
    Examples:
      | fallo          |
      | error de red   |
      | HTTP 503       |
      | HTTP 500       |

  @s32
  Scenario: Anunciar la espera del detalle
    Given la lectura de un detalle todavía no ha terminado
    When se abre su URL
    Then aparece "Cargando proyecto" anunciado de forma accesible antes de 400 ms
    And no se presenta información de otro proyecto mientras espera
