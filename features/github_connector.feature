@github_connector
Feature: Importar issues abiertas de GitHub como tareas propias con trazabilidad duradera
  Como persona autenticada quiero conectar un repositorio con un PAT y traer sus issues abiertas
  a un proyecto propio, sabiendo siempre qué tarea procede de qué issue y sin exponer el token.
  Fuente: progress/proposal_github_connector.md (feature 27). Importación bajo demanda, de un solo
  sentido, una conexión por propietario. Las longitudes son puntos de código Unicode. Toda
  confirmación usa PostgreSQL efímero y un servidor GitHub falso configurado como app.github.api-base;
  ninguna prueba habla con api.github.com. Los errores usan application/problem+json según ApiErrors.

  # ---------------------------------------------------------------- Conexión

  @s1
  Scenario: Conectar con un PAT válido guarda el token cifrado y nunca lo devuelve
    Given una sesión válida con CSRF, APP_CONNECTOR_KEY configurada y sin conexión previa
    And el servidor falso responde 200 a GET /repos/OCTOCAT/hello-world con full_name "octocat/Hello-World" y 200 a GET /user con login "octocat"
    When envío PUT a "/api/v1/me/connectors/github" con repository "OCTOCAT/hello-world" y token "ghp_secreto123"
    Then recibo HTTP 200 con Cache-Control no-store y la respuesta contiene exactamente repository, login, status, connectedAt y lastImport
    And repository es "octocat/Hello-World", login es "octocat", status es "valid" y lastImport es null
    And el cuerpo de la respuesta no contiene la subcadena "ghp_secreto123"
    And existe exactamente una fila de conexión del propietario cuyo token_ciphertext tiene octet_length 12 + 14 + 16 bytes y no contiene los bytes de "ghp_secreto123"
    And descifrar token_ciphertext con APP_CONNECTOR_KEY y owner_id como dato autenticado adicional devuelve exactamente "ghp_secreto123"
    And ambas peticiones al servidor falso llevan Authorization "Bearer ghp_secreto123", Accept "application/vnd.github+json", X-GitHub-Api-Version "2022-11-28" y User-Agent "OrganizationWeb"

  @s2
  Scenario Outline: El cifrado usa nonce nuevo por escritura y ata el texto cifrado al propietario
    Given una conexión guardada con token "ghp_secreto123"
    When <accion>
    Then <resultado>
    Examples:
      | accion                                                                        | resultado                                                                                           |
      | repito el PUT con el mismo repositorio y el mismo token                       | el nuevo token_ciphertext difiere byte a byte del anterior y sus 12 primeros bytes difieren         |
      | intento descifrar token_ciphertext usando otro owner_id como dato autenticado | el descifrado falla por etiqueta inválida y no devuelve ningún byte del token                       |
      | intento descifrar token_ciphertext con una clave distinta de 32 bytes         | el descifrado falla por etiqueta inválida y no devuelve ningún byte del token                       |

  @s3
  Scenario Outline: Sin APP_CONNECTOR_KEY ninguna ruta del conector lee ni escribe
    Given la aplicación arranca sin APP_CONNECTOR_KEY, una sesión válida con CSRF y un proyecto propio en idea
    When solicito <operacion>
    Then recibo HTTP 503 CONNECTORS_DISABLED con Cache-Control no-store
    And el servidor falso recibe cero peticiones y no existe ninguna fila de conexión, enlace ni recibo
    And el resto de rutas de proyectos y tareas responde con normalidad
    Examples:
      | operacion                                            |
      | GET /api/v1/me/connectors/github                     |
      | PUT /api/v1/me/connectors/github con cuerpo válido   |
      | DELETE /api/v1/me/connectors/github                  |
      | POST /api/v1/me/connectors/github/imports            |
      | GET /api/v1/me/connectors/github/imports/{uuid}      |

  @s4
  Scenario Outline: Una clave presente pero inválida impide arrancar sin revelarla
    Given APP_CONNECTOR_KEY con valor <valor>
    When la aplicación arranca
    Then el arranque falla con un mensaje que nombra app.connectors.key y no contiene el valor configurado
    Examples:
      | valor                          |
      | base64 de 16 bytes             |
      | base64 de 33 bytes             |
      | texto que no es base64         |
      | cadena vacía                   |

  @s5
  Scenario Outline: El repositorio se recorta y se valida antes de hablar con GitHub
    Given una sesión válida con CSRF y APP_CONNECTOR_KEY configurada
    When envío PUT de conexión con repository <entrada> y token válido
    Then recibo HTTP <estado> y <resultado>
    And cuando el estado es 400 el servidor falso recibe cero peticiones y no se guarda ninguna fila
    Examples:
      | entrada                                              | estado | resultado                                                        |
      | " octocat/Hello-World " con U+0020 y U+00A0 exteriores | 200    | repository es "octocat/Hello-World"                              |
      | "octocat"                                            | 400    | VALIDATION_ERROR con error en repository y código INVALID_FORMAT |
      | "-octocat/repo"                                      | 400    | VALIDATION_ERROR con error en repository y código INVALID_FORMAT |
      | "octocat-/repo"                                      | 400    | VALIDATION_ERROR con error en repository y código INVALID_FORMAT |
      | 40 letras + "/repo"                                  | 400    | VALIDATION_ERROR con error en repository y código INVALID_FORMAT |
      | "octocat/" + 101 letras                              | 400    | VALIDATION_ERROR con error en repository y código INVALID_FORMAT |
      | "octocat/mi repo"                                    | 400    | VALIDATION_ERROR con error en repository y código INVALID_FORMAT |
      | "https://github.com/octocat/repo"                    | 400    | VALIDATION_ERROR con error en repository y código INVALID_FORMAT |
      | ausente                                              | 400    | VALIDATION_ERROR con error en repository y código REQUIRED       |
      | número                                               | 400    | VALIDATION_ERROR con error en repository y código INVALID_TYPE   |

  @s6
  Scenario Outline: El token y el cuerpo del PUT son estrictos
    Given una sesión válida con CSRF y APP_CONNECTOR_KEY configurada
    When envío PUT de conexión con <defecto>
    Then recibo HTTP <estado> con código <codigo> y error de campo <campo>
    And el servidor falso recibe cero peticiones y no se guarda ninguna fila
    Examples:
      | defecto                                  | estado | codigo           | campo      |
      | token ausente                            | 400    | VALIDATION_ERROR | REQUIRED   |
      | token cadena vacía                       | 400    | VALIDATION_ERROR | REQUIRED   |
      | token de 256 caracteres ASCII            | 400    | VALIDATION_ERROR | TOO_LONG   |
      | token con un espacio interior            | 400    | VALIDATION_ERROR | INVALID_FORMAT |
      | token con U+00E9                         | 400    | VALIDATION_ERROR | INVALID_FORMAT |
      | token con U+0009                         | 400    | VALIDATION_ERROR | INVALID_FORMAT |
      | token número                             | 400    | VALIDATION_ERROR | INVALID_TYPE |
      | propiedad adicional apiBase              | 400    | VALIDATION_ERROR | UNKNOWN_FIELD |
      | query string ?repository=x añadida       | 400    | VALIDATION_ERROR | sin campo  |
      | JSON truncado                            | 400    | MALFORMED_JSON   | sin campo  |
      | contenido distinto de JSON               | 415    | UNSUPPORTED_MEDIA_TYPE | sin campo |

  @s7
  Scenario: GitHub rechaza el token al conectar y no se guarda nada
    Given una sesión válida con CSRF, APP_CONNECTOR_KEY configurada y sin conexión previa
    And el servidor falso responde 401 a GET /repos/octocat/Hello-World
    When envío PUT de conexión con repository "octocat/Hello-World" y token "ghp_malo"
    Then recibo HTTP 409 GITHUB_TOKEN_REJECTED
    And no existe ninguna fila de conexión y el cuerpo del problema y los logs no contienen "ghp_malo"

  @s8
  Scenario Outline: Repositorio no disponible deja la conexión exactamente como estaba
    Given <estado_previo> y el servidor falso responde <respuesta_github> a GET /repos/octocat/Hello-World
    When <accion>
    Then recibo HTTP 409 GITHUB_REPOSITORY_UNAVAILABLE
    And la fila de conexión queda <fila_despues> y no se crea tarea, evento ni enlace
    Examples:
      | estado_previo                                   | respuesta_github                              | accion                                | fila_despues                                  |
      | sin conexión previa                              | 404                                           | envío PUT de conexión                 | inexistente                                   |
      | sin conexión previa                              | 403 sin x-ratelimit-remaining ni Retry-After  | envío PUT de conexión                 | inexistente                                   |
      | una conexión valid a "octocat/Otro"              | 404                                           | envío PUT de conexión a Hello-World   | byte a byte igual, con repository "octocat/Otro" |
      | una conexión valid a "octocat/Hello-World"       | 404                                           | inicio una importación                | byte a byte igual y status valid, con recibo failed errorCode GITHUB_REPOSITORY_UNAVAILABLE |

  @s9
  Scenario: Reconectar sustituye repositorio y token conservando enlaces y recibos
    Given una conexión valid a "octocat/Hello-World" con dos tareas importadas, sus dos enlaces y un recibo completed
    And el servidor falso responde 200 para "octocat/Segundo" con login "octocat"
    When envío PUT de conexión con repository "octocat/Segundo" y un token distinto
    Then recibo HTTP 200 con repository "octocat/Segundo", status "valid" y connectedAt igual al instante de este PUT
    And existe exactamente una fila de conexión del propietario, con token_ciphertext distinto del anterior
    And los dos enlaces, las dos tareas y el recibo previo permanecen sin cambios y lastImport sigue siendo ese recibo

  @s10
  Scenario Outline: Consultar la conexión devuelve el DTO cerrado o 404
    Given <estado_previo>
    When envío GET a "/api/v1/me/connectors/github"
    Then recibo HTTP <estado> con Cache-Control no-store y <cuerpo>
    Examples:
      | estado_previo                                         | estado | cuerpo                                                                                     |
      | sin conexión                                          | 404    | código CONNECTION_NOT_FOUND                                                                |
      | conexión valid sin importaciones                      | 200    | exactamente repository, login, status "valid", connectedAt y lastImport null               |
      | conexión invalid con tres recibos                     | 200    | status "invalid" y lastImport igual al recibo de mayor startedAt, con sus once campos      |
      | conexión valid y un recibo running                    | 200    | lastImport con status "running", errorCode null y finishedAt null                          |

  @s11
  Scenario: Desconectar borra el token y conserva tareas, enlaces y recibos
    Given una conexión valid con tres tareas importadas, tres enlaces y dos recibos
    When envío DELETE a "/api/v1/me/connectors/github" dos veces seguidas
    Then ambas respuestas son HTTP 204 sin cuerpo
    And no existe ninguna fila de conexión del propietario ni copia del texto cifrado
    And las tres tareas, los tres enlaces y los dos recibos permanecen íntegros
    And GET de la conexión responde 404 CONNECTION_NOT_FOUND y GET de cada recibo responde 200

  # ------------------------------------------------------------ Importación

  @s12
  Scenario: Importar crea una tarea, un evento y un enlace por issue abierta y descarta pull requests
    Given una conexión valid y un proyecto propio en idea sin tareas
    And el servidor falso devuelve en page=1 tres issues con id 101, 102 y 103 y un elemento con campo pull_request e id 104, sin Link rel="next"
    When envío POST a "/api/v1/me/connectors/github/imports" con projectId del proyecto
    Then recibo HTTP 201 con Location "/api/v1/me/connectors/github/imports/{id}" y el recibo contiene exactamente id, projectId, repository, status, created, skipped, failed, truncated, errorCode, startedAt y finishedAt
    And status es "completed", created 3, skipped 0, failed 0, truncated false, errorCode null y finishedAt no anterior a startedAt
    And el proyecto tiene exactamente tres tareas pending con estimatedMinutes null y existen exactamente tres eventos TaskCreated.v1 en la outbox, uno por tarea
    And existen exactamente tres enlaces con source "github", external_id "101", "102" y "103" y url igual al html_url de cada issue
    And ninguna tarea, evento ni enlace corresponde al id 104

  @s13
  Scenario: La petición a GitHub usa la consulta fija y no sigue redirecciones
    Given una conexión valid y un proyecto propio en idea
    And el servidor falso responde 302 con Location hacia otra ruta del propio servidor
    When inicio una importación
    Then la única petición recibida es GET /repos/octocat/Hello-World/issues con query exactamente state=open, per_page=100, sort=created, direction=desc y page=1
    And el servidor falso no recibe ninguna petición a la ruta indicada en Location
    And recibo HTTP 503 GITHUB_UNAVAILABLE con importId y el recibo queda failed con errorCode GITHUB_UNAVAILABLE y created 0

  @s14
  Scenario Outline: El título se recorta a 160 puntos de código como en la creación de tareas
    Given una conexión valid, un proyecto propio en idea y una issue cuyo title es <title>
    When inicio una importación
    Then <resultado>
    Examples:
      | title                                         | resultado                                                                                         |
      | 159 repeticiones de "a"                       | created 1 y el título de la tarea tiene 159 puntos de código sin "…"                               |
      | 160 repeticiones de "a"                       | created 1 y el título de la tarea tiene exactamente 160 puntos de código sin "…"                   |
      | 161 repeticiones de "a"                       | created 1 y el título es 159 repeticiones de "a" seguidas de "…", 160 puntos de código en total    |
      | 161 repeticiones de "🚀"                      | created 1 y el título es 159 repeticiones de "🚀" seguidas de "…", sin partir ningún par sustituto |
      | U+0020 U+2003 "Arreglar login" U+00A0         | created 1 y el título es exactamente "Arreglar login"                                              |
      | sólo U+0020 y U+00A0                          | failed 1, created 0 y no existe tarea, evento ni enlace para esa issue                             |
      | 161 caracteres incluidos 6 espacios finales   | el recorte de espacios ocurre antes de contar y el título resultante tiene 155 puntos de código sin "…" |

  @s15
  Scenario Outline: El criterio de completitud contiene la URL y las primeras veinte líneas del cuerpo
    Given una conexión valid, un proyecto propio en idea y una issue con html_url "https://github.com/octocat/Hello-World/issues/7" y body <body>
    When inicio una importación
    Then completionCriterion de la tarea creada es exactamente <criterio>
    Examples:
      | body                                              | criterio                                                                                        |
      | null                                              | "https://github.com/octocat/Hello-World/issues/7"                                              |
      | "línea1\r\nlínea2"                                | la URL, una línea en blanco y "línea1\nlínea2" sin ningún \r                                    |
      | 25 líneas "L1".."L25" separadas por \n            | la URL, una línea en blanco y "L1" a "L20" separadas por \n, sin "L21"                          |
      | una sola línea de 2100 letras                     | 1999 puntos de código seguidos de "…", 2000 en total, empezando por la URL                      |
      | cadena vacía o sólo Unicode White_Space           | "https://github.com/octocat/Hello-World/issues/7", igual que con body null                     |

  @s16
  Scenario Outline: Se leen como máximo dos páginas de cien y truncated refleja lo no leído
    Given una conexión valid, un proyecto propio en idea y un repositorio falso con <abiertas> issues abiertas
    When inicio una importación
    Then el servidor falso recibe exactamente <llamadas> peticiones de issues con per_page=100 y páginas consecutivas desde 1
    And la segunda página sólo se pide cuando la primera trae exactamente 100 elementos, aunque no venga Link rel="next"
    And el recibo queda completed con created <created> y truncated <truncated>
    Examples:
      | abiertas | llamadas | created | truncated |
      | 0        | 1        | 0       | false     |
      | 99       | 1        | 99      | false     |
      | 100      | 2        | 100     | false     |
      | 150      | 2        | 150     | false     |
      | 200      | 2        | 200     | false     |
      | 201      | 2        | 200     | true      |

  @s17
  Scenario Outline: Repetir la importación salta por enlace único sin crear tareas ni eventos
    Given una importación completed con created 5 y <cambio_en_github>
    When inicio una segunda importación al mismo proyecto
    Then recibo HTTP 201 con created <created>, skipped <skipped> y failed 0
    And el número de tareas del proyecto y de eventos TaskCreated.v1 aumenta exactamente en <created>
    And sigue sin poder existir más de un enlace por owner_id, source y external_id
    Examples:
      | cambio_en_github                                       | created | skipped |
      | ninguna issue nueva                                    | 0       | 5       |
      | una issue nueva con id 999                             | 1       | 5       |
      | los títulos de las cinco issues han cambiado           | 0       | 5       |
      | una issue ya importada fue cerrada en GitHub           | 0       | 4       |

  @s18
  Scenario: Una issue fallida no detiene a las demás y el recibo termina completed
    Given una conexión valid, un proyecto propio en idea y cinco issues de las que la tercera tiene título sólo espacios
    When inicio una importación
    Then recibo HTTP 201 con status "completed", created 4, failed 1 y skipped 0
    And existen exactamente cuatro tareas, cuatro eventos TaskCreated.v1 y cuatro enlaces
    And created + skipped + failed es igual al número de issues consideradas, 5

  @s19
  Scenario: Tarea, evento y enlace confirman o revierten juntos y la repetición retoma sin duplicar
    Given una conexión valid, un proyecto propio en idea y cinco issues
    And la inserción del enlace de la tercera issue falla con error de almacenamiento
    When inicio una importación
    Then recibo HTTP 503 STORAGE_UNAVAILABLE con importId igual al id del recibo
    And el recibo queda failed con errorCode STORAGE_UNAVAILABLE, created 2, skipped 0 y failed 0
    And existen exactamente dos tareas, dos eventos TaskCreated.v1 y dos enlaces, ninguno de la tercera issue
    And una importación posterior sin el fallo responde 201 con created 3 y skipped 2

  # ------------------------------------------------------------------ Errores

  @s20
  Scenario Outline: Cuota agotada responde 503 RATE_LIMITED con segundos calculados
    Given una conexión valid, un proyecto propio en idea y el reloj inyectado en T
    And el servidor falso responde <respuesta_github> a la petición de issues
    When inicio una importación
    Then recibo HTTP 503 RATE_LIMITED con retryAfterSeconds <segundos>, cabecera Retry-After "<segundos>" e importId
    And el recibo queda failed con errorCode RATE_LIMITED y la conexión sigue valid
    Examples:
      | respuesta_github                                                       | segundos |
      | 429 con Retry-After: 30                                                | 30       |
      | 429 sin cabeceras                                                      | 60       |
      | 403 con x-ratelimit-remaining: 0 y x-ratelimit-reset = T + 120 s       | 120      |
      | 403 con x-ratelimit-remaining: 0 y x-ratelimit-reset = T - 5 s         | 1        |
      | 403 con Retry-After: 7 y x-ratelimit-reset = T + 900 s                 | 7        |
      | 403 con x-ratelimit-remaining: 0 sin reset ni Retry-After              | 60       |

  @s21
  Scenario: Cuota agotada al conectar no guarda la conexión
    Given una sesión válida con CSRF, sin conexión previa y el servidor falso responde 429 con Retry-After: 45 a GET /repos
    When envío PUT de conexión válido
    Then recibo HTTP 503 RATE_LIMITED con retryAfterSeconds 45 y cabecera Retry-After "45"
    And no existe ninguna fila de conexión y el cuerpo del problema no contiene el token

  @s22
  Scenario: GitHub responde 401 durante la importación y la conexión pasa a invalid
    Given una conexión valid, un proyecto propio en idea y el servidor falso responde 401 en page=2 tras 100 issues en page=1
    When inicio una importación
    Then recibo HTTP 409 CONNECTION_INVALID con importId
    And el recibo queda failed con errorCode CONNECTION_INVALID y created 100
    And GET de la conexión devuelve status "invalid" y una nueva importación responde 409 CONNECTION_INVALID sin ninguna petición al servidor falso
    And un PUT de conexión válido posterior devuelve status "valid" y vuelve a permitir importar

  @s23
  Scenario Outline: Proyecto inexistente, ajeno o mal formado no revela su existencia
    Given una conexión valid y un proyecto en idea que pertenece a otra persona
    When inicio una importación con projectId <projectId>
    Then recibo HTTP <estado> con código <codigo>
    And el cuerpo del problema para proyecto inexistente y para proyecto ajeno es idéntico salvo instance
    And no se crea recibo y el servidor falso recibe cero peticiones
    Examples:
      | projectId                                    | estado | codigo             |
      | UUID que no existe                           | 404    | RESOURCE_NOT_FOUND |
      | UUID del proyecto de la otra persona         | 404    | RESOURCE_NOT_FOUND |
      | "no-es-uuid"                                 | 400    | VALIDATION_ERROR   |
      | UUID en mayúsculas                           | 400    | VALIDATION_ERROR   |
      | ausente                                      | 400    | VALIDATION_ERROR   |
      | UUID válido más propiedad desconocida        | 400    | VALIDATION_ERROR   |

  @s24
  Scenario Outline: Un proyecto completed no recibe tareas ni al empezar ni a mitad
    Given una conexión valid y un proyecto propio <estado_proyecto>
    When inicio una importación
    Then recibo HTTP 409 PROJECT_COMPLETED <detalle>
    And el estado del proyecto y su cuota de activos no cambian
    Examples:
      | estado_proyecto                                                      | detalle                                                                                  |
      | completed antes de empezar                                           | sin importId, sin recibo y con cero peticiones al servidor falso                         |
      | que pasa a completed tras confirmarse la segunda de cinco tareas     | con importId, recibo failed errorCode PROJECT_COMPLETED, created 2 y sólo dos tareas     |

  @s25
  Scenario: Dos importaciones simultáneas reales obtienen un 201 y un 409
    Given una conexión valid, un proyecto propio en idea y el servidor falso retiene la primera respuesta de issues hasta recibir una segunda petición POST
    When envío dos POST de importación desde dos hilos al mismo tiempo
    Then exactamente una respuesta es HTTP 201 completed y la otra es HTTP 409 IMPORT_IN_PROGRESS sin importId
    And en ningún instante existe más de un recibo running del propietario y al final existe exactamente un recibo
    And las tareas, eventos y enlaces creados corresponden sólo a la importación que respondió 201

  @s26
  Scenario Outline: Un recibo running huérfano se recupera por antigüedad
    Given una conexión valid, un proyecto propio en idea y un recibo running con started_at <antiguedad> antes del reloj inyectado
    When inicio una importación
    Then recibo HTTP <estado> con código <codigo>
    And el recibo antiguo queda <recibo_antiguo>
    Examples:
      | antiguedad        | estado | codigo             | recibo_antiguo                                                          |
      | 14 min 59 s       | 409    | IMPORT_IN_PROGRESS | running sin cambios                                                     |
      | 15 min 1 s        | 201    | completed          | failed con errorCode INTERRUPTED, finishedAt no nulo y contadores intactos |

  @s27
  Scenario: Tras un reinicio a mitad de importación queda exactamente lo confirmado
    Given una importación que muere después de confirmar dos de cinco issues
    When la aplicación arranca de nuevo y consulto GET de la conexión
    Then lastImport tiene status "running", created 2, errorCode null y finishedAt null
    And existen exactamente dos tareas, dos eventos TaskCreated.v1 y dos enlaces, y cada tarea tiene enlace y cada enlace tarea
    And no se relanza ninguna importación automáticamente ni se contacta con el servidor falso

  @s28
  Scenario Outline: Fallos de red o respuestas inesperadas de GitHub son 503 GITHUB_UNAVAILABLE
    Given una conexión valid, un proyecto propio en idea y el servidor falso <comportamiento>
    When inicio una importación
    Then recibo HTTP 503 GITHUB_UNAVAILABLE con importId en menos de <plazo>
    And el recibo queda failed con errorCode GITHUB_UNAVAILABLE y la conexión sigue valid
    Examples:
      | comportamiento                                             | plazo  |
      | responde 500                                               | 1 s    |
      | responde 502                                               | 1 s    |
      | no envía cuerpo durante 5 s                                | 4,9 s  |
      | no acepta la conexión TCP                                  | 2,9 s  |
      | responde 200 con un objeto JSON en lugar de un array       | 1 s    |
      | responde 200 con una issue sin id numérico                 | 1 s    |

  @s29
  Scenario Outline: Las precondiciones de importar se evalúan en orden fijo
    Given APP_CONNECTOR_KEY configurada y una sesión válida con CSRF
    And se cumplen a la vez <condiciones>
    When inicio una importación
    Then recibo HTTP <estado> con código <codigo>
    And no se crea ningún recibo nuevo en ninguna fila y el servidor falso recibe cero peticiones
    Examples:
      | condiciones                                                                  | estado | codigo              |
      | cuerpo inválido, sin conexión y proyecto ajeno                               | 400    | VALIDATION_ERROR    |
      | sin conexión y proyecto ajeno                                                | 404    | CONNECTION_NOT_FOUND |
      | conexión invalid y proyecto completed                                        | 409    | CONNECTION_INVALID  |
      | conexión valid, proyecto ajeno y un recibo running propio                    | 404    | RESOURCE_NOT_FOUND  |
      | conexión valid, proyecto completed y un recibo running propio                | 409    | PROJECT_COMPLETED   |
      | conexión valid, proyecto en idea y un recibo running propio reciente         | 409    | IMPORT_IN_PROGRESS  |

  # ---------------------------------------------------------------- Recibos

  @s30
  Scenario Outline: Consultar un recibo devuelve el propio persistido o 404 indistinguible
    Given un recibo completed propio y un recibo completed de otra persona
    When envío GET a "/api/v1/me/connectors/github/imports/<id>"
    Then recibo HTTP <estado> con Cache-Control no-store y <cuerpo>
    Examples:
      | id                              | estado | cuerpo                                                                                             |
      | el recibo propio                | 200    | exactamente los once campos del recibo, con los mismos valores devueltos por el POST original      |
      | el recibo propio tras reiniciar | 200    | el mismo cuerpo que antes del reinicio                                                             |
      | el recibo de la otra persona    | 404    | código IMPORT_NOT_FOUND, idéntico salvo instance al de un UUID inexistente                         |
      | UUID inexistente                | 404    | código IMPORT_NOT_FOUND                                                                            |
      | "no-es-uuid"                    | 404    | código IMPORT_NOT_FOUND                                                                            |

  # -------------------------------------------------------------- Seguridad

  @s31
  Scenario Outline: Sólo la sesión cookie autentica el conector
    Given <credencial>
    When solicito <operacion>
    Then recibo HTTP <estado> sin datos privados y no se escribe ni se contacta con el servidor falso
    # Enmienda del 9 de septiembre de 2026, ratificada por el propietario. La última
    # fila esperaba 401 UNAUTHENTICATED, pero una credencial Bearer válida SÍ está
    # autenticada: el filtro de la feature 24 la identifica y después comprueba su
    # lista de rutas permitidas, que no incluye el conector. Responder «no sé quién
    # eres» a quien sí se ha identificado es falso; 403 API_SCOPE_DENIED dice la
    # verdad. La propiedad de seguridad no cambia: la credencial no abre nada, no
    # escribe nada y no contacta con el servidor falso.
    Examples:
      | credencial                                              | operacion                  | estado               |
      | sin sesión                                              | GET de la conexión         | 401 UNAUTHENTICATED  |
      | sesión vencida                                          | PUT de conexión            | 401 UNAUTHENTICATED  |
      | sin sesión                                              | DELETE de la conexión      | 401 UNAUTHENTICATED  |
      | sesión vencida                                          | POST de importación        | 401 UNAUTHENTICATED  |
      | sin sesión                                              | GET de un recibo existente | 401 UNAUTHENTICATED  |
      | una credencial Bearer válida del canal de integraciones | PUT de conexión            | 403 API_SCOPE_DENIED |
      | una credencial Bearer válida del canal de integraciones | POST de importación        | 403 API_SCOPE_DENIED |

  @s32
  Scenario Outline: CSRF y Origin protegen las operaciones que escriben
    Given una sesión válida y una conexión valid
    When envío <operacion> con <defecto>
    Then recibo HTTP 403 y la conexión, las tareas y los recibos quedan byte a byte iguales
    And el servidor falso recibe cero peticiones
    Examples:
      | operacion             | defecto                          |
      | PUT de conexión       | token CSRF ausente               |
      | PUT de conexión       | Origin de otro sitio             |
      | DELETE de la conexión | token CSRF inválido              |
      | POST de importación   | token CSRF ausente               |
      | POST de importación   | Origin de otro sitio             |

  @s33
  Scenario: Cada propietario sólo ve y usa su propia conexión, enlaces y recibos
    Given la persona A con conexión valid a "octocat/Hello-World", una tarea importada con external_id "101" y un recibo
    And la persona B con sesión propia, conexión valid a "otra/Cosa" cuyo repositorio falso contiene una issue con id 101, y un proyecto propio en idea
    When la persona B consulta su conexión, consulta el recibo de A, importa al proyecto de A e importa a su propio proyecto
    Then la primera responde 200 con repository "otra/Cosa" y lastImport null, la segunda 404 IMPORT_NOT_FOUND y la tercera 404 RESOURCE_NOT_FOUND
    And la cuarta responde 201 con created 1: B tiene su propia tarea y su propio enlace con external_id "101" y los de A quedan intactos

  @s34
  Scenario Outline: El token en claro no sale por ningún canal
    Given una conexión guardada con token "ghp_canal_secreto" y una importación realizada
    When inspecciono <canal>
    Then no contiene la subcadena "ghp_canal_secreto" ni su codificación base64
    Examples:
      | canal                                                          |
      | la respuesta del PUT y del GET de la conexión                  |
      | el problema 409 GITHUB_TOKEN_REJECTED y el 503 RATE_LIMITED    |
      | los logs de conexión e importación, que sí contienen propietario, repositorio, código HTTP de GitHub y contadores |
      | la exportación JSON v1, que conserva catorce colecciones sin conexión ni enlaces |
      | localStorage, sessionStorage y cookies del navegador tras conectar |
      | la respuesta de auditoría de sesión                            |

  @s35
  Scenario Outline: La base de la API de GitHub es configuración del servidor y no del usuario
    Given app.github.api-base apunta al servidor falso
    When <accion>
    Then <resultado>
    Examples:
      | accion                                                                       | resultado                                                                              |
      | envío PUT de conexión con propiedad adicional apiBase "https://atacante.test" | recibo 400 VALIDATION_ERROR UNKNOWN_FIELD y el servidor falso recibe cero peticiones   |
      | envío PUT de conexión con repository "atacante.test/x/y"                     | recibo 400 VALIDATION_ERROR INVALID_FORMAT y no se abre ninguna conexión saliente      |
      | conecto e importo con repository "octocat/Hello-World"                       | todas las peticiones salientes van al servidor falso, con la ruta bajo /repos/octocat/Hello-World |
      | la aplicación arranca con app.github.api-base "http://api.github.com"        | el arranque falla nombrando app.github.api-base; "http://127.0.0.1:{puerto}" sí arranca |

  # --------------------------------------------------------------- Interfaz

  @s36
  Scenario Outline: La página muestra un único estado a la vez
    Given una sesión válida y <situacion>
    When abro "/integraciones/github"
    Then el h1 es "Conector de GitHub" y veo <visible>
    And no veo <oculto>
    Examples:
      | situacion                                       | visible                                                                                              | oculto                                                  |
      | GET de la conexión responde 503 CONNECTORS_DISABLED | un aviso que explica que falta configuración del servidor                                        | formulario, selector de proyecto ni botón de importar   |
      | sin conexión                                    | el formulario con campos repository y token y la ayuda sobre el permiso de lectura de issues         | selector de proyecto ni botón Desconectar               |
      | conexión valid sin importaciones                | repositorio, login, estado "Conectada", selector de proyecto, "Importar issues abiertas" y "Desconectar" | el formulario de token                              |
      | conexión valid con lastImport completed         | los contadores created, skipped, failed del último recibo y un enlace al proyecto destino            | el formulario de token                                  |
      | conexión invalid                                | el estado "Conexión inválida" y un botón "Reconectar" que muestra el formulario                      | el botón "Importar issues abiertas" habilitado          |
      | conexión valid con lastImport running            | el aviso "Hay una importación en curso" y un botón "Consultar estado"                               | el botón "Importar issues abiertas" habilitado          |

  @s37
  Scenario: El formulario de PAT no autocompleta, no persiste y se vacía tras enviar
    Given la página sin conexión
    When relleno repository y token y envío el formulario, una vez con respuesta 200 y otra con 409 GITHUB_TOKEN_REJECTED
    Then el campo token tiene type "password" y autocomplete "off" antes y después de enviar
    And en ambos casos el campo token queda vacío tras la respuesta y repository conserva su valor sólo en el caso 409
    And localStorage y sessionStorage no contienen el token en ningún momento
    And en el caso 409 el mensaje "GitHub rechazó el token" aparece junto al campo token, referenciado por aria-describedby, y el foco va a ese campo

  @s38
  Scenario: Importar desde la interfaz muestra progreso honesto y un resumen medible
    Given una conexión valid, dos proyectos propios no completed, uno completed y un repositorio falso con 201 issues de las que 1 tiene título vacío
    When elijo un proyecto en el selector y activo "Importar issues abiertas"
    Then el selector ofrece exactamente los dos proyectos no completed cargados con la API de proyectos existente
    And durante la petición el botón está disabled y una región aria-live="polite" anuncia "Importando issues…" sin porcentaje
    And al terminar la región anuncia el resumen y veo "Creadas 199", "Omitidas 0", "Fallidas 1", el aviso de que el repositorio tiene más issues que las 200 importadas y un enlace al proyecto destino
    And el foco pasa al h1 al cambiar al estado de resultado

  @s39
  Scenario Outline: Los errores de importación ofrecen la acción que los resuelve
    Given una conexión valid y el servidor responde <error> al importar
    When activo "Importar issues abiertas"
    Then veo <mensaje> y <accion>
    And el botón de importar vuelve a estar habilitado y ninguna petición se reintenta sola
    Examples:
      | error                                          | mensaje                                                        | accion                                                            |
      | 503 RATE_LIMITED con retryAfterSeconds 90      | "GitHub limita las peticiones. Reintenta en 90 segundos"       | ningún reintento automático                                       |
      | 409 CONNECTION_INVALID                         | "La conexión ya no es válida"                                  | un botón "Reconectar" que muestra el formulario                   |
      | 409 IMPORT_IN_PROGRESS                         | "Hay una importación en curso"                                 | un botón "Consultar estado" que recarga la conexión y su lastImport |
      | 409 PROJECT_COMPLETED                          | "El proyecto está terminado"                                   | el selector vuelve a estar activo para elegir otro proyecto       |
      | 503 STORAGE_UNAVAILABLE con importId           | "No se pudo completar" con los contadores parciales del recibo | un enlace al proyecto destino                                     |

  @s40
  Scenario: Desconectar exige confirmación y cancelarla no cambia nada
    Given una conexión valid
    When activo "Desconectar", cancelo el diálogo, vuelvo a activar "Desconectar" y confirmo
    Then tras cancelar no se envía ningún DELETE y la conexión sigue visible con el foco de vuelta en "Desconectar"
    And tras confirmar se envía un único DELETE, la página pasa al estado sin conexión y el foco va al h1

  @s41
  Scenario Outline: Salir, cerrar sesión o respuestas tardías no filtran datos entre contextos
    Given la página con repository "octocat/Hello-World" escrito y token escrito, o una importación en curso
    When <evento>
    Then <resultado>
    Examples:
      | evento                                                       | resultado                                                                                         |
      | navego a otra pantalla y vuelvo con la misma sesión          | repository conserva "octocat/Hello-World" y el campo token está vacío                             |
      | cierro sesión y otra persona inicia sesión en la misma pestaña | no queda repository, token, resumen ni recibo de la primera persona visible                     |
      | llega la respuesta tardía de una importación de la sesión anterior | no altera el estado, los contadores, los errores ni el foco de la sesión nueva              |
      | la sesión vence en mitad de la importación                   | veo la pantalla de acceso sin datos privados y ninguna petición se reintenta                     |

  @s42
  Scenario: El recorrido es accesible en los siete estados y en los tres anchos
    Given la página en los estados deshabilitado, sin conexión, conectada, importando, resultado, error recuperable e inválida
    When recorro cada estado con teclado, con axe y en los anchos 320, 768 y 1440 px CSS con zoom nativo 200 por ciento
    Then todos los controles se alcanzan y activan con Tab, Enter y Escape, con foco visible y área mínima de 44 por 44 px
    And axe no reporta ninguna violación y cada error se anuncia por aria-live o por el foco
    And a 320 px con zoom 200 por ciento no hay desplazamiento horizontal ni contenido recortado
    And "/integraciones" enlaza "Conector de GitHub" y el menú no gana ninguna entrada nueva
    And la revisión registra evidencia de las 30 filas de docs/ux-requirements.md sin atribuir estudio humano a axe
