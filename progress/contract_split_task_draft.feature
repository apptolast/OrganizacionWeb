@split_task @draft
Feature: Dividir una tarea propia en pasos cada vez menores
  Borrador documental: no activa implementación ni cierra create_task.
  La representación pública conserva los ocho campos exactos de create_task s1.
  Cada referencia a una tabla de create_task exige ejecutar todas sus filas en la ruta nueva.
  La trazabilidad indicará escenario heredado, entrada concreta y prueba del nuevo endpoint.
  Los errores conservan application/problem+json, códigos de campo y ausencia de escrituras ante rechazo.
  Cursor revisado por backend: projectId, parentTaskId, createdAt e id.

  @s1
  Scenario: Crear una subtarea propia con relación persistida
    Given una sesión válida con CSRF y una tarea propia en un proyecto idea
    When envío POST a "/api/v1/projects/{projectId}/tasks/{parentId}/subtasks" con application/json y title "Preparar portada"
    Then recibo HTTP 201 después de confirmar la transacción
    And Location es "/api/v1/projects/{projectId}/tasks/{id}" y permite consultar el DTO confirmado
    And el DTO conserva los ocho campos y defaults de create_task s1, con UUID nuevo y fechas iniciales iguales
    And se conserva la relación con el padre dentro del mismo proyecto
    And se guarda exactamente un evento SubtaskCreated.v1 y ningún TaskCreated.v1 adicional

  @s2
  Scenario Outline: Conservar padre y proyecto en todos los estados abiertos
    Given un padre propio con criterio y estimación dentro de un proyecto <estado>
    When creo una subtarea con criterio y estimación distintos
    Then cada tarea conserva sus valores independientes
    And padre y proyecto conservan estado, fechas y versiones, incluido ETag del proyecto
    And no se suman estimaciones, no se registra tiempo trabajado y no se completa ninguna tarea
    Examples:
      | estado |
      | idea   |
      | active |
      | paused |

  @s3
  Scenario: Dividir sucesivamente mediante hijos nuevos
    Given existe una subtarea propia
    When creo una subtarea de esa subtarea
    Then la relación apunta al padre directo dentro del mismo proyecto
    And puedo navegar a cada nivel mediante el mismo recorrido de detalle
    And no se mueve ni reasigna ninguna tarea existente

  @s4
  Scenario Outline: Ejecutar las reglas de contenido heredadas en el nuevo endpoint
    Given una sesión válida con CSRF y un padre propio en proyecto abierto
    When ejecuto todas las entradas de <referencia> sustituyendo sólo la ruta por POST de subtareas
    Then cada entrada conserva estado HTTP, cuerpo, código público, código de campo y normalización de esa referencia
    And los rechazos no guardan subtarea, relación ni evento
    And los éxitos conservan DTO8 y guardan sólo SubtaskCreated.v1
    Examples:
      | referencia                                                |
      | create_task s2: títulos positivos Unicode                  |
      | create_task s3: espacios exteriores e interior conservado  |
      | create_task s4: títulos rechazados                         |
      | create_task s5: criterio ausente, null y texto conservado   |
      | create_task s6: tipos y límites opcionales rechazados      |
      | create_task s7: estimación ausente, null, 1 y 1440          |
      | create_task s8: JSON estricto, campos cerrados y media type |

  @s5
  Scenario: Rechazar relación enviada dentro del cuerpo
    Given una sesión válida con CSRF y un padre propio en proyecto abierto
    When envío un título válido y la propiedad parentId en el JSON de creación
    Then recibo HTTP 400 VALIDATION_ERROR
    And no se guarda subtarea, relación ni evento

  @s6
  Scenario Outline: Exigir sesión en cada recurso nuevo
    Given una sesión <sesion>
    When solicito <operacion>
    Then recibo HTTP 401 UNAUTHENTICATED sin datos privados ni desafío Basic
    And no se modifica ninguna tarea ni evento
    Examples:
      | sesion  | operacion    |
      | ausente | POST hijos   |
      | vencida | POST hijos   |
      | ausente | GET hijos    |
      | vencida | GET hijos    |
      | ausente | GET padre    |
      | vencida | GET padre    |

  @s7
  Scenario Outline: Proteger sólo las escrituras mediante CSRF y origen
    Given una sesión válida y un padre propio
    When envío POST de subtarea con <defecto>
    Then recibo HTTP 403 con código <codigo> y ninguna escritura
    Examples:
      | defecto                                      | codigo           |
      | CSRF ausente con origen permitido            | CSRF_INVALID     |
      | CSRF inválido con origen permitido           | CSRF_INVALID     |
      | origen extranjero con CSRF válido            | UNTRUSTED_ORIGIN |

  @s8
  Scenario Outline: Comparar privacidad en los tres recursos nuevos
    Given una sesión válida con CSRF y <recurso>
    When ejecuto POST hijos, GET hijos y GET padre sobre ese recurso, comprobando cada respuesta por separado
    Then las tres respuestas son HTTP 404 RESOURCE_NOT_FOUND
    And sus cuerpos públicos completos son iguales al de una tarea inexistente de referencia
    And no se exponen datos ni se modifican tareas o eventos
    Examples:
      | recurso                       |
      | proyecto inexistente          |
      | proyecto ajeno                |
      | tarea inexistente             |
      | tarea de otro proyecto propio |
      | tarea de otro propietario     |

  @s9
  Scenario Outline: Consultar el padre directo sin ampliar DTO8
    Given una tarea propia <clase>
    When consulto GET "/api/v1/projects/{projectId}/tasks/{id}/parent"
    Then recibo HTTP 200 con exactamente <cuerpo>
    And cualquier DTO de padre pertenece al mismo proyecto y conserva sus ocho campos confirmados
    Examples:
      | clase    | cuerpo             |
      | raíz     | {"parent":null}    |
      | subtarea | {"parent":<DTO8>}  |

  @s10
  Scenario: Conservar la colección y creación anteriores
    Given un proyecto contiene una raíz y una subtarea
    When consulto su colección plana de tareas y los detalles de ambas
    Then ambas aparecen con DTO8, orden y paginación vigentes sin parentId añadido

  @s11
  Scenario Outline: Consultar sólo hijos directos
    Given un padre propio tiene <estructura>
    When consulto GET "/api/v1/projects/{projectId}/tasks/{parentId}/subtasks"
    Then recibo exactamente items y nextCursor
    And items contiene <contenido> y nextCursor es null
    Examples:
      | estructura                  | contenido                |
      | ningún hijo                 | lista vacía              |
      | dos hijos y un nieto         | sólo los dos hijos directos |

  @s12
  Scenario: Paginar hijos con orden estable tras una creación posterior
    Given un padre propio tiene 21 hijos y varios comparten createdAt
    And he obtenido la primera página de 20 con su cursor original
    When creo un hijo más reciente y consulto la continuación con el cursor original
    Then recibo sólo el hijo original restante y nextCursor null
    And no aparece el hijo nuevo ni se repite ninguna identidad

  @s13
  Scenario: Reutilizar todas las fronteras del cursor y query
    Given una sesión válida y un padre propio
    When ejecuto todas las entradas de create_task s22 sobre GET hijos con cursor de cuatro campos
    Then conservo las reglas de base64url canónica, claves cerradas, fecha y UUID de esa referencia
    And cada rechazo mantiene HTTP 400 VALIDATION_ERROR y su campo cursor o query correspondiente
    And la trazabilidad distingue query repetida, clave JSON duplicada y cada entrada concreta de la tabla

  @s14
  Scenario Outline: Rechazar cursores de otras colecciones
    Given una sesión válida y un padre propio
    When consulto sus hijos con <cursor>
    Then recibo HTTP 400 VALIDATION_ERROR en cursor y ninguna tarea de otra colección
    Examples:
      | cursor                               |
      | cursor de otro proyecto              |
      | cursor de otro padre propio          |
      | cursor de la colección plana de tareas |

  @s15
  Scenario Outline: Validar identificadores antes del cursor
    Given una sesión válida y <identificador> mal formado
    When solicito <operacion>
    Then recibo HTTP 400 VALIDATION_ERROR con INVALID_FORMAT en <identificador>
    Examples:
      | identificador | operacion                  |
      | projectId     | GET hijos con cursor válido |
      | parentId      | GET hijos con cursor válido |
      | parentId      | POST hijos                  |
      | id            | GET padre                   |

  @s16
  Scenario: Rechazar creación en completed y permitir reapertura deliberada
    Given una tarea propia pertenece a un proyecto completed
    When intento crear una subtarea
    Then recibo HTTP 409 PROJECT_COMPLETED sin subtarea ni evento
    And GET hijos y GET padre siguen permitidos

  @s17
  Scenario Outline: Resolver la carrera real entre división y cierre
    Given crear una subtarea y terminar el proyecto compiten concurrentemente en PostgreSQL
    When <primera> obtiene primero el bloqueo del proyecto y confirma mientras la segunda operación espera
    Then <resultado>
    Examples:
      | primera  | resultado                                                          |
      | terminar | la creación devuelve 409 sin subtarea, relación ni evento           |
      | crear    | la subtarea queda confirmada y pending después del cierre posterior |

  @s18
  Scenario Outline: Confirmar relación, contenido y evento de forma atómica
    Given la escritura de <registro> presenta <fallo>
    When intento crear una subtarea válida
    Then recibo HTTP 503 STORAGE_UNAVAILABLE sin Location
    And no quedan subtarea, relación ni evento parciales
    And padre y proyecto conservan datos y versiones
    Examples:
      | registro | fallo         |
      | subtarea | excepción SQL |
      | subtarea | cero filas    |
      | evento   | excepción SQL |
      | evento   | cero filas    |

  @s19
  Scenario: Conservar no-store en las lecturas nuevas
    Given solicito GET hijos y GET padre con las respuestas 200, 400, 401, 404 y 503 de create_task s24
    When verifico cada combinación de operación y estado
    Then cada respuesta contiene Cache-Control no-store

  @s20
  Scenario Outline: No sustituir indisponibilidad por una relación vacía
    Given falla realmente el almacenamiento al ejecutar <operacion>
    When consulto el recurso propio
    Then recibo HTTP 503 STORAGE_UNAVAILABLE
    And no recibo <respuesta_ficticia>
    Examples:
      | operacion | respuesta_ficticia |
      | GET hijos | items vacío        |
      | GET padre | parent null        |

  @s21
  Scenario: Publicar la división tras recuperar el broker
    Given el worker está habilitado y el broker detenido
    And una creación por HTTP ha confirmado 201 con su SubtaskCreated.v1 pendiente
    When el broker vuelve a estar disponible
    Then recibo el JSON original con exactamente eventId, aggregateId, ownerId, occurredAt, schemaVersion, type, taskId, parentTaskId y title
    And aggregateId identifica el proyecto, taskId la tarea nueva y parentTaskId su padre directo
    And no contiene criterio ni estimación
    And usa la ruta subtask.created.v1 y la cola durable quorum organization.subtask-created.v1
    And conserva confirmaciones, persistencia y reintentos vigentes

  @s22
  Scenario Outline: Rechazar eventos de división incompatibles
    Given un SubtaskCreated.v1 presenta <defecto>
    When el publicador lo procesa
    Then se bloquea con <codigo> sin envío
    And los cuatro tipos anteriores conservan sus contratos
    Examples:
      | defecto                    | codigo            |
      | tipo desconocido           | UNSUPPORTED_EVENT |
      | versión desconocida        | UNSUPPORTED_EVENT |
      | campo extra                | INVALID_EVENT     |
      | campo requerido ausente    | INVALID_EVENT     |
      | taskId incompleto          | INVALID_EVENT     |
      | parentTaskId incompleto    | INVALID_EVENT     |
      | taskId igual a parentTaskId | INVALID_EVENT    |
      | título vacío               | INVALID_EVENT     |
      | título de 161 puntos       | INVALID_EVENT     |
    # Los demás límites de contenido/UUID conservan las tablas cerradas del publicador de create_task s18.
    # La trazabilidad ejecuta dichas entradas para ambos identificadores nuevos pertinentes.

  @s23
  Scenario: Abrir detalle de tarea con contexto directo
    Given veo una tarea en la lista del proyecto
    When abro "/proyectos/{projectId}/tareas/{id}" y recargo esa URL directamente
    Then veo título, criterio, estimación, estado y enlace al proyecto
    And si tiene padre confirmado puedo abrirlo mediante enlace accesible
    And veo sólo sus hijos directos, con carga, vacío y error independientes
    And no se descarga todo el árbol para mostrar esa tarea

  @s24
  Scenario: No presentar como raíz una lectura fallida de padre
    Given el detalle de tarea está disponible y GET padre falla
    When veo el contexto de la tarea
    Then se muestra error recuperable de relación, sin afirmar que es raíz

  @s25
  Scenario: Crear un paso y recuperarlo al recargar
    Given veo el detalle de una tarea propia en proyecto abierto
    When guardo un nuevo paso con título válido
    Then veo la subtarea confirmada y puedo abrir su detalle
    And recargar conserva contenido y relación sin repetir POST
    And se explica que las estimaciones no se suman automáticamente

  @s26
  Scenario Outline: Conservar borrador ante un rechazo o incertidumbre
    Given he escrito los tres campos de una subtarea
    When la creación termina con <resultado>
    Then se conservan los valores y se ofrece continuar deliberadamente sin repetir POST
    And PROJECT_COMPLETED sólo ofrece revisar el proyecto mediante GET elegido antes de otra acción explícita
    Examples:
      | resultado                     |
      | 400 VALIDATION_ERROR          |
      | 409 PROJECT_COMPLETED         |
      | 503 STORAGE_UNAVAILABLE       |
      | fallo de red sin confirmación |

  @s27
  Scenario: Eliminar datos y borrador al perder acceso
    Given hay contenido y un borrador de subtarea
    When la sesión se pierde y la API responde 401
    Then se elimina el contenido privado y el borrador, y aparece acceso
    And una respuesta antigua no los restaura

  @s28
  Scenario: Conservar confirmación aunque falle la recarga
    Given el POST de subtarea confirmó 201
    When falla el GET posterior de hijos
    Then la confirmación guardada sigue visible junto a recuperación de lectura
    And no se repite POST ni se presenta el guardado como fallido

  @s29
  Scenario: Bloquear doble envío mientras espera la creación
    Given un POST de subtarea sigue pendiente
    When intento enviar de nuevo desde el mismo formulario
    Then existe un único POST y se anuncia espera con control deshabilitado

  @s30
  Scenario Outline: Ignorar una respuesta de la tarea anterior
    Given hay una <solicitud> retenida de la tarea A
    When navego a la tarea B y después llega la respuesta de A
    Then no cambia el contenido ni el borrador de B
    Examples:
      | solicitud |
      | lectura   |
      | creación  |

  @s31
  Scenario Outline: Ignorar respuestas después de cerrar sesión
    Given hay una <solicitud> retenida
    When cierro sesión y después llega su respuesta
    Then no reaparece contenido privado ni borrador
    Examples:
      | solicitud |
      | lectura   |
      | creación  |

  @s32
  Scenario: Usar detalle, formulario y paginación con teclado
    Given navego sin ratón por una tarea y sus subtareas
    When recorro enlaces, campos, envío inválido, páginas y reintento
    Then cada acción tiene nombre, foco visible y orden lógico
    And el primer campo inválido recibe foco y los cambios de página no lo pierden
    And una entrada numérica incompleta se distingue de vacío opcional y no envía POST

  @s33
  Scenario: Verificar diseño responsive y feedback
    Given uso detalle y formulario de subtareas con contenido largo
    When recorro la matriz de anchos y ambos lados de breakpoints de docs/ux-requirements.md
    Then no hay desplazamiento horizontal ni controles inaccesibles
    And los controles miden al menos 44 por 44 píxeles CSS
    And el feedback inicial se mide inferior a 400 ms antes de liberar una respuesta retenida
    And se prueba zoom nativo al 200 por ciento con ancho interior de 320 píxeles CSS
    And se revisan los 30 principios con evidencia o limitaciones explícitas

  @s34
  Scenario: Crear raíces por la ruta histórica
    Given un proyecto contiene una raíz y una subtarea
    When creo otra tarea mediante POST de la colección anterior del proyecto
    Then esa tarea es raíz y genera su único TaskCreated.v1 histórico

  @s35
  Scenario: Obtener la primera página de hijos
    Given un padre propio tiene 21 hijos y varios comparten createdAt
    When consulto la primera página de hijos
    Then recibo los primeros 20 por createdAt descendente e id descendente
    And recibo cursor con exactamente projectId, parentTaskId, createdAt e id

  @s36
  Scenario: Crear después de reabrir deliberadamente el proyecto
    Given una creación fue rechazada con PROJECT_COMPLETED
    And he reabierto el proyecto en pausa sin reenvío automático de la solicitud rechazada
    When elijo enviar otra creación de subtarea
    Then la subtarea se confirma con un único evento

  @s37
  Scenario: Confirmar la subtarea durante la caída del broker
    Given el worker está habilitado y el broker detenido
    When creo una subtarea por HTTP
    Then recibo 201 y queda un SubtaskCreated.v1 pendiente con identidad conservada

  @s38
  Scenario: Recuperar una lectura de padre fallida
    Given se muestra un error recuperable de relación sin afirmar que la tarea es raíz
    When elijo reintentar y GET padre confirma {"parent":null}
    Then se representa la raíz confirmada y desaparece el error de relación
