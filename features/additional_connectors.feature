@additional_connectors
Feature: Catálogo de conectores y segundo gestor de issues (GitLab) sobre el puerto compartido de 27
  Como propietario quiero ver en un solo lugar el estado real de mis seis integraciones
  y traer issues abiertas de un proyecto GitLab como tareas propias, con la misma trazabilidad que GitHub.
  Fuente: progress/proposal_additional_connectors.md; donde 27 fija algo distinto, 27 prevalece
  (códigos y estados HTTP compartidos, recibo persistido, guardián por propietario, mapeo issue → tarea).
  Depende de 27 fusionada: puerto IssueSource, caso de uso de importación por origen, tabla task_external_links
  ampliada a source gitlab, guardián IMPORT_IN_PROGRESS por propietario y APP_CONNECTOR_KEY compartida.
  Toda llamada saliente de las pruebas va a un servidor GitLab falso configurado en app.gitlab.api-base;
  ninguna prueba contacta gitlab.com. Los errores usan application/problem+json con código estable.
  Las longitudes son puntos de código Unicode. «Sin secretos» significa: ni token, ni tokenHint, ni URL con token,
  ni mensaje libre del proveedor. Los seis conectores son, en este orden: api_credentials (24), webhooks (25),
  ics_calendar (26), github (27), external_calendar (28), gitlab (29).

  # ---------- Catálogo GET /api/v1/me/connectors ----------

  @s1
  Scenario: Catálogo con seis filas cerradas en orden fijo aunque no haya nada conectado
    Given una sesión válida con APP_CONNECTOR_KEY configurada y sin ninguna integración creada
    When consulta GET /api/v1/me/connectors
    Then recibe HTTP 200 con Cache-Control no-store y un objeto con exactamente la clave connectors
    And connectors tiene exactamente seis elementos con id en este orden: api_credentials, webhooks, ics_calendar, github, external_calendar, gitlab
    And cada elemento contiene exactamente id, status, lastActivityAt y lastError
    And los seis tienen status not_connected, lastActivityAt null y lastError null

  @s2
  Scenario Outline: Cada fila deriva su estado de la fuente de su feature sin inventar connected
    Given una sesión válida con APP_CONNECTOR_KEY configurada
    And el propietario tiene <situacion>
    When consulta GET /api/v1/me/connectors
    Then la fila <conector> tiene status <status>, lastActivityAt <ultimaActividad> y lastError <ultimoError>
    And las otras cinco filas conservan el estado que les corresponde por sus propias fuentes
    Examples:
      | conector          | situacion                                                                        | status        | ultimaActividad                                     | ultimoError                                |
      | api_credentials   | una credencial de 24 vigente y ninguna revocada                                  | connected     | el último uso registrado por 24 o null si no lo hay | null                                       |
      | api_credentials   | únicamente credenciales de 24 revocadas                                          | not_connected | null                                                | null                                       |
      | webhooks          | un endpoint de 25 activo con una entrega registrada                              | connected     | el instante de la última entrega                    | null                                       |
      | webhooks          | todos sus endpoints de 25 desactivados por DELIVERY_EXHAUSTED                    | error         | el instante de la última entrega                    | { code: DELIVERY_EXHAUSTED, at: instante } |
      | ics_calendar      | un token de feed de 26 activo                                                    | connected     | null                                                | null                                       |
      | ics_calendar      | el token de feed de 26 revocado                                                  | not_connected | null                                                | null                                       |
      | github            | una conexión de 27 con status valid y una importación terminada                  | connected     | finishedAt de esa importación                       | null                                       |
      | github            | una conexión de 27 con status invalid                                            | error         | connectedAt o la última importación                 | { code: CONNECTION_INVALID, at: instante } |
      | external_calendar | una suscripción de 28 con lastStatus OK                                          | connected     | lastAttemptAt de la suscripción                     | null                                       |
      | external_calendar | una suscripción de 28 con lastStatus FAILED y lastError FEED_REJECTED            | error         | lastAttemptAt de la suscripción                     | { code: FEED_REJECTED, at: lastAttemptAt } |
      | gitlab            | una conexión GitLab con status connected sin importaciones                       | connected     | el instante de la conexión                          | null                                       |
      | gitlab            | una conexión GitLab con status error por token rechazado                         | error         | el instante de la última importación                | { code: CONNECTION_INVALID, at: instante } |

  @s3
  Scenario: Sin APP_CONNECTOR_KEY los conectores que cifran aparecen disabled y el catálogo sigue disponible
    Given una sesión válida sin APP_CONNECTOR_KEY configurada
    And el propietario tiene una credencial vigente de 24, un token de feed de 26 activo y filas cifradas de 25, 27, 28 y 29
    When consulta GET /api/v1/me/connectors
    Then recibe HTTP 200 con seis filas en el orden fijo
    And webhooks, github, external_calendar y gitlab tienen status disabled, lastActivityAt null y lastError null
    And api_credentials e ics_calendar tienen status connected
    And la respuesta no intenta descifrar ninguna fila ni contiene un texto de error del cifrado

  @s4
  Scenario: El catálogo sólo refleja integraciones del propietario autenticado
    Given el propietario A no tiene integraciones
    And el propietario B tiene conexiones GitHub y GitLab connected, un endpoint de 25 activo y una suscripción de 28
    When A consulta GET /api/v1/me/connectors
    Then las seis filas de A tienen status not_connected
    And la respuesta no contiene identificadores, rutas de proyecto, hosts ni instantes de B

  @s5
  Scenario: lastError no transporta secretos ni texto libre del proveedor
    Given una conexión GitLab del propietario en status error tras un 401 cuyo cuerpo decía "invalid_token: glpat-abcdef1234"
    When consulta GET /api/v1/me/connectors
    Then la fila gitlab tiene lastError con exactamente code CONNECTION_INVALID y at como instante UTC
    And el cuerpo completo de la respuesta no contiene "glpat", los cuatro caracteres del tokenHint, "invalid_token", ninguna URL ni la ruta del proyecto

  @s6
  Scenario: Consultar el catálogo no sincroniza, no llama a terceros y no escribe
    Given una sesión válida con conexiones GitHub y GitLab connected, una suscripción de 28 y un endpoint de 25 activo
    And servidores falsos de GitHub y GitLab con contador de peticiones a cero
    When consulta GET /api/v1/me/connectors tres veces seguidas
    Then ambos servidores falsos siguen con cero peticiones recibidas
    And ninguna fila de conexiones, suscripciones, endpoints, entregas, tareas, enlaces, recibos ni outbox cambia
    And las tres respuestas son idénticas

  @s7
  Scenario: Almacenamiento indisponible no produce un catálogo optimista
    Given una sesión válida y PostgreSQL no permite completar la lectura de las fuentes
    When consulta GET /api/v1/me/connectors
    Then recibe HTTP 503 con código STORAGE_UNAVAILABLE
    And la respuesta no contiene connectors ni ninguna fila con status connected

  # ---------- Conexión GitLab ----------

  @s8
  Scenario: Consultar el conector GitLab sin conexión devuelve not_connected, no 404
    Given una sesión válida con APP_CONNECTOR_KEY configurada y sin conexión GitLab
    When consulta GET /api/v1/me/connectors/gitlab
    Then recibe HTTP 200 con Cache-Control no-store
    And el cuerpo contiene exactamente status, apiBase, projectPath, projectId, tokenHint, lastActivityAt, lastError y version
    And status es not_connected y los otros siete campos son null

  @s9
  Scenario: Conectar valida el proyecto en GitLab y guarda el token cifrado
    Given una sesión válida con CSRF, APP_CONNECTOR_KEY configurada y sin conexión GitLab
    And app.gitlab.api-base apunta al servidor falso, que responde 200 a GET /projects/grupo%2Fproyecto con id 4821 y path_with_namespace "grupo/proyecto"
    When envía PUT /api/v1/me/connectors/gitlab con exactamente { token: "glpat-xxxxxxxxxxxxxxxxWXYZ", projectPath: "grupo/proyecto" }
    Then el servidor falso recibió exactamente una petición, GET /projects/grupo%2Fproyecto, con cabecera PRIVATE-TOKEN igual al token y sin el token en la URL
    And recibe HTTP 200 con status connected, apiBase igual al valor configurado, projectPath "grupo/proyecto", projectId 4821, tokenHint "WXYZ", lastActivityAt igual al instante de la conexión, lastError null y version 1
    And la respuesta no contiene el token
    And la fila persistida guarda token_ciphertext y token_nonce de 12 bytes, y ninguna columna ni log contiene el token en claro
    And la fila gitlab del catálogo pasa a connected

  @s10
  Scenario Outline: Rechazar cuerpos inválidos al conectar sin contactar GitLab
    Given una sesión válida con CSRF y APP_CONNECTOR_KEY configurada
    When envía PUT /api/v1/me/connectors/gitlab con <cuerpo>
    Then recibe HTTP 400 con código VALIDATION_ERROR y error en <campo> con código <detalle>
    And el servidor falso recibe cero peticiones y no se crea ni modifica ninguna conexión
    Examples:
      | cuerpo                                                     | campo       | detalle       |
      | token ausente                                              | token       | REQUIRED      |
      | token ""                                                   | token       | REQUIRED      |
      | token de 201 caracteres                                    | token       | TOO_LONG      |
      | token con un espacio interior                              | token       | INVALID_FORMAT|
      | projectPath "soloproyecto" sin barra                       | projectPath | INVALID_FORMAT|
      | projectPath "grupo/../otro"                                | projectPath | INVALID_FORMAT|
      | projectPath con seis segmentos "a/b/c/d/e/f"               | projectPath | INVALID_FORMAT|
      | projectPath de 256 caracteres                              | projectPath | TOO_LONG      |
      | projectPath con espacio "grupo/mi proyecto"                | projectPath | INVALID_FORMAT|
      | campo adicional apiBase "https://gitlab.com/api/v4"        | apiBase     | UNKNOWN_FIELD |
      | campo adicional desconocido "extra"                        | extra       | UNKNOWN_FIELD |

  @s11
  Scenario: La ruta del proyecto se codifica y la base de API sólo sale de la configuración del servidor
    Given una sesión válida con CSRF y app.gitlab.api-base configurada como https://gitlab.example.com/api/v4 hacia el servidor falso
    And el servidor falso responde 200 a GET /projects/grupo%2Fsub%2Fproyecto con id 77 y path_with_namespace "grupo/sub/proyecto"
    When envía PUT /api/v1/me/connectors/gitlab con projectPath "grupo/sub/proyecto" y un token válido
    Then la única petición recibida tiene la ruta exacta /api/v4/projects/grupo%2Fsub%2Fproyecto en el host configurado
    And recibe HTTP 200 con apiBase "https://gitlab.example.com/api/v4", projectPath "grupo/sub/proyecto" y projectId 77

  @s12
  Scenario Outline: GitLab rechaza la conexión y no se guarda nada
    Given una sesión válida con CSRF, APP_CONNECTOR_KEY configurada y <conexionPrevia>
    And el servidor falso responde <respuesta> a la consulta del proyecto
    When envía PUT /api/v1/me/connectors/gitlab con un cuerpo válido
    Then recibe HTTP <http> con código <codigo>
    And la conexión previa queda exactamente como estaba y no existe ninguna fila con el token nuevo
    And el problema no contiene el token ni el cuerpo de la respuesta de GitLab
    Examples:
      | conexionPrevia                       | respuesta                              | http | codigo             |
      | sin conexión previa                  | 401                                    | 409  | CONNECTION_INVALID |
      | sin conexión previa                  | 404                                    | 409  | CONNECTION_INVALID |
      | una conexión connected a otro token  | 403                                    | 409  | CONNECTION_INVALID |
      | sin conexión previa                  | 429 con Retry-After 20                 | 503  | RATE_LIMITED       |
      | sin conexión previa                  | 500                                    | 503  | GITLAB_UNAVAILABLE |
      | sin conexión previa                  | 302 hacia otra ruta                    | 503  | GITLAB_UNAVAILABLE |
      | sin conexión previa                  | 200 con cuerpo que no es JSON          | 503  | GITLAB_UNAVAILABLE |
      | sin conexión previa                  | sin respuesta dentro del tiempo límite | 503  | GITLAB_UNAVAILABLE |

  @s13
  Scenario: Reemplazar el token conserva tareas y enlaces e incrementa version
    Given una conexión GitLab connected con version 1 y 3 tareas importadas con sus enlaces de origen gitlab
    And el servidor falso acepta el proyecto con el token nuevo
    When envía PUT /api/v1/me/connectors/gitlab con otro token que termina en "9Q2p" y el mismo projectPath
    Then recibe HTTP 200 con tokenHint "9Q2p", status connected, lastError null y version 2
    And las 3 tareas y sus 3 enlaces permanecen con los mismos valores
    And descifrar la fila con la clave devuelve el token nuevo y no el anterior

  @s14
  Scenario: Desconectar es idempotente y conserva tareas y enlaces
    Given una conexión GitLab connected con 2 tareas importadas y sus enlaces
    When envía DELETE /api/v1/me/connectors/gitlab dos veces seguidas con CSRF válido
    Then ambas respuestas son HTTP 204 sin cuerpo
    And no existe fila de conexión GitLab del propietario ni texto cifrado del token
    And las 2 tareas y los 2 enlaces (owner_id, gitlab, external_id) permanecen intactos
    And GET /api/v1/me/connectors/gitlab devuelve status not_connected y la fila gitlab del catálogo es not_connected

  # ---------- Importación de issues ----------

  @s15
  Scenario: Importar issues abiertas crea tareas enlazadas con recibo persistido
    Given una conexión GitLab connected con projectId 4821 y un proyecto destino propio en idea
    And el servidor falso responde a GET /projects/4821/issues?state=opened&per_page=100&page=1 con 2 issues de issue_type issue, ids 9001 y 9002, sin X-Next-Page
    When envía POST /api/v1/me/connectors/gitlab/imports con exactamente { projectId: <proyecto destino> }
    Then la única petición de issues lleva cabecera PRIVATE-TOKEN igual al token y el token no aparece en la URL
    And recibe HTTP 201 con Location /api/v1/me/connectors/gitlab/imports/{id}
    And el recibo contiene exactamente id, source, projectId, projectPath, status, created, skipped, failed, truncated, errorCode, startedAt y finishedAt
    And source es gitlab, status completed, created 2, skipped 0, failed 0, truncated false y errorCode null
    And el proyecto destino tiene exactamente 2 tareas nuevas en status pending con estimatedMinutes null y un evento TaskCreated.v1 por tarea
    And existen exactamente 2 enlaces (owner_id, gitlab, "gitlab.example.com:9001") y (owner_id, gitlab, "gitlab.example.com:9002") con url igual a web_url de cada issue
    And GET /api/v1/me/connectors/gitlab devuelve lastActivityAt igual a finishedAt del recibo

  @s16
  Scenario Outline: Paginar por X-Next-Page hasta dos páginas y marcar truncated
    Given una conexión GitLab connected y un proyecto destino propio abierto
    And la página 1 devuelve <pagina1> issues con X-Next-Page "<siguiente1>"
    And la página 2 devuelve <pagina2> issues con X-Next-Page "<siguiente2>"
    When importa issues
    Then el servidor falso recibió exactamente <peticiones> peticiones de issues, todas con per_page=100 y state=opened
    And el recibo tiene created <created> y truncated <truncated>
    Examples:
      | pagina1 | siguiente1 | pagina2 | siguiente2 | peticiones | created | truncated |
      | 0       |            | 0       |            | 1          | 0       | false     |
      | 37      |            | 0       |            | 1          | 37      | false     |
      | 100     | 2          | 40      |            | 2          | 140     | false     |
      | 100     | 2          | 100     | 3          | 2          | 200     | true      |

  @s17
  Scenario Outline: Excluir incidentes, test cases, tasks de GitLab e issues movidas como skipped
    Given una conexión GitLab connected y un proyecto destino propio abierto
    And la página 1 devuelve 2 issues de issue_type issue y una issue con <caracteristica>
    When importa issues
    Then el recibo tiene created 2, skipped 1 y failed 0
    And no existe tarea ni enlace para la issue excluida
    Examples:
      | caracteristica              |
      | issue_type incident         |
      | issue_type test_case        |
      | issue_type task             |
      | moved_to_id 555 no nulo     |

  @s18
  Scenario: Repetir la importación es idempotente por enlace
    Given una importación previa que creó 5 tareas desde las issues 1..5 del proyecto GitLab
    And ahora el proyecto GitLab devuelve las issues 1..5 y una nueva issue 6
    When importa issues de nuevo sobre el mismo proyecto destino
    Then el recibo tiene created 1, skipped 5 y failed 0
    And el proyecto destino tiene exactamente 6 tareas importadas y 6 enlaces de origen gitlab
    And ninguna de las 5 tareas previas cambia title, completionCriterion, status ni updatedAt

  @s19
  Scenario: La unicidad de enlaces es por origen y admite el mismo external_id en github y gitlab
    Given un enlace (owner_id, github, "gitlab.example.com:42") que apunta a una tarea propia
    When se confirma un enlace (owner_id, gitlab, "gitlab.example.com:42") hacia otra tarea propia
    Then ambos enlaces persisten y cada uno conserva su tarea
    And confirmar un segundo enlace (owner_id, gitlab, "gitlab.example.com:42") viola la unicidad y no crea tarea
    And un enlace de otro propietario con el mismo origen y external_id sí persiste

  @s20
  Scenario: Un mismo caso de uso de importación sirve a GitHub y a GitLab con el mismo resultado
    Given conexiones GitHub y GitLab connected del mismo propietario y dos proyectos destino propios abiertos
    And ambos servidores falsos sirven una issue con el mismo title "Revisar despliegue", el mismo body de 3 líneas y web_url/html_url propias
    When importa desde github al primer proyecto y, terminada esa importación, desde gitlab al segundo
    Then las dos tareas creadas coinciden en title, completionCriterion salvo la URL de la primera línea, status pending y estimatedMinutes null
    And los dos recibos tienen las mismas claves y difieren únicamente en id, source, projectId, projectPath e instantes
    And el enlace de la primera tiene source github y el de la segunda source gitlab

  @s21
  Scenario Outline: El mapeo issue → tarea de GitLab es el de 27
    Given una conexión GitLab connected y un proyecto destino propio abierto
    And la página 1 devuelve una issue con <titulo> y <cuerpo>
    When importa issues
    Then <resultado>
    Examples:
      | titulo                                             | cuerpo                           | resultado                                                                                                       |
      | "  Preparar demo  " con espacios Unicode exteriores | body null                        | crea una tarea con title "Preparar demo" y completionCriterion igual a web_url                                  |
      | 160 puntos de código "🚀"                          | body de 3 líneas                 | crea una tarea con title de 160 puntos de código y completionCriterion web_url, línea en blanco y las 3 líneas |
      | 161 puntos de código "a"                           | body con 25 líneas               | title conserva 159 puntos de código y termina en "…"; completionCriterion contiene sólo las 20 primeras líneas |
      | "   " sólo espacios                                | body "x"                         | el recibo tiene created 0 y failed 1 y no existe tarea ni enlace                                                |
      | "Cuerpo largo"                                     | body de 2500 puntos de código    | completionCriterion tiene exactamente 2000 puntos de código y termina en "…"                                    |

  @s22
  Scenario Outline: Las precondiciones del proyecto destino y de la conexión se aplican antes de contactar GitLab
    Given una sesión válida con CSRF y <estado>
    When envía POST /api/v1/me/connectors/gitlab/imports con <cuerpo>
    Then recibe HTTP <http> con código <codigo>
    And el servidor falso recibe cero peticiones y no se crea tarea, enlace ni recibo
    Examples:
      | estado                                                    | cuerpo                                | http | codigo               |
      | una conexión GitLab connected                             | projectId de un proyecto inexistente  | 404  | RESOURCE_NOT_FOUND   |
      | una conexión GitLab connected                             | projectId de un proyecto ajeno        | 404  | RESOURCE_NOT_FOUND   |
      | una conexión GitLab connected                             | projectId de un proyecto completed    | 409  | PROJECT_COMPLETED    |
      | una conexión GitLab connected                             | projectId "no-uuid"                   | 400  | VALIDATION_ERROR     |
      | una conexión GitLab connected                             | campo adicional projectPath           | 400  | VALIDATION_ERROR     |
      | sin conexión GitLab                                       | projectId de un proyecto propio       | 404  | CONNECTION_NOT_FOUND |
      | una conexión GitLab en status error                       | projectId de un proyecto propio       | 409  | CONNECTION_INVALID   |

  @s23
  Scenario: Un token rechazado durante la importación marca la conexión en error y deja recibo failed
    Given una conexión GitLab connected y un proyecto destino propio abierto
    And el servidor falso responde 401 a la primera página de issues
    When importa issues
    Then recibe HTTP 409 con código CONNECTION_INVALID y el miembro importId
    And el recibo importId tiene status failed, errorCode CONNECTION_INVALID, created 0 y finishedAt no nulo
    And GET /api/v1/me/connectors/gitlab devuelve status error y lastError { code: CONNECTION_INVALID, at }
    And la fila gitlab del catálogo pasa a error con el mismo lastError
    And el token cifrado no se borra y la respuesta no lo contiene

  @s24
  Scenario Outline: La cuota de GitLab produce RATE_LIMITED con Retry-After
    Given una conexión GitLab connected y un proyecto destino propio abierto
    And el servidor falso responde a la primera página con <respuesta>
    When importa issues
    Then recibe HTTP 503 con código RATE_LIMITED, retryAfterSeconds <segundos> y cabecera Retry-After <segundos>
    And la conexión sigue connected y no se crea tarea ni enlace
    And el recibo queda failed con errorCode RATE_LIMITED
    Examples:
      | respuesta                                            | segundos |
      | 429 con Retry-After 30                               | 30       |
      | 429 sin Retry-After                                  | 60       |
      | 200 vacío con RateLimit-Remaining 0 y Retry-After 5  | 5        |

  @s25
  Scenario Outline: GitLab indisponible no altera la conexión ni sigue redirecciones
    Given una conexión GitLab connected y un proyecto destino propio abierto
    And el servidor falso responde a la primera página con <respuesta>
    When importa issues
    Then recibe HTTP 503 con código GITLAB_UNAVAILABLE
    And el servidor falso no recibió ninguna petición al destino de la redirección
    And la conexión sigue connected con lastError { code: GITLAB_UNAVAILABLE, at }
    And no se crea tarea ni enlace y el recibo queda failed con errorCode GITLAB_UNAVAILABLE
    Examples:
      | respuesta                                           |
      | 503                                                 |
      | 301 hacia otra URL de la misma instancia            |
      | 200 con cuerpo HTML                                 |
      | 200 con cuerpo JSON de más de 5 MiB                 |
      | sin respuesta dentro del tiempo de lectura          |

  @s26
  Scenario: Un fallo en la página 2 conserva lo confirmado y lo declara en el recibo
    Given una conexión GitLab connected y un proyecto destino propio abierto
    And la página 1 devuelve 100 issues válidas con X-Next-Page 2 y la página 2 responde 500
    When importa issues
    Then recibe HTTP 503 con código GITLAB_UNAVAILABLE y el miembro importId
    And el recibo importId tiene status failed, created 100, skipped 0, failed 0, truncated false y errorCode GITLAB_UNAVAILABLE
    And existen exactamente 100 tareas y 100 enlaces, cada tarea confirmada junto con su enlace y ninguna sin enlace
    And una segunda importación con las dos páginas sanas devuelve created 100 y skipped 100

  @s27
  Scenario Outline: Una sola importación por propietario, compartida entre GitHub y GitLab
    Given el propietario tiene conexiones GitHub y GitLab connected y <enCurso> con started_at hace 2 minutos
    When <accion>
    Then recibe HTTP 409 con código IMPORT_IN_PROGRESS
    And el servidor falso de GitLab recibe cero peticiones y la conexión GitLab no cambia
    And otro propietario con su propia conexión GitLab puede importar en ese mismo momento con HTTP 201
    Examples:
      | enCurso                             | accion                                             |
      | un recibo de github en status running | envía POST /api/v1/me/connectors/gitlab/imports    |
      | un recibo de gitlab en status running | envía POST /api/v1/me/connectors/gitlab/imports    |
      | un recibo de gitlab en status running | envía POST /api/v1/me/connectors/github/imports    |
      | un recibo de gitlab en status running | envía PUT /api/v1/me/connectors/gitlab con cuerpo válido |
      | un recibo de gitlab en status running | envía DELETE /api/v1/me/connectors/gitlab          |

  @s28
  Scenario Outline: Un recibo running abandonado deja de bloquear pasados 15 minutos
    Given el propietario tiene un recibo de gitlab en status running con started_at hace <antiguedad> según el reloj inyectado
    And una conexión GitLab connected, un proyecto destino abierto y el servidor falso con 1 issue
    When importa issues
    Then recibe HTTP <http> con <resultado>
    And <estadoPrevio>
    Examples:
      | antiguedad | http | resultado                                   | estadoPrevio                                                             |
      | 14 minutos | 409  | código IMPORT_IN_PROGRESS                   | el recibo previo sigue running                                           |
      | 16 minutos | 201  | recibo completed con created 1              | el recibo previo pasa a failed con errorCode INTERRUPTED y finishedAt no nulo |

  @s29
  Scenario Outline: Sin APP_CONNECTOR_KEY el conector GitLab responde CONNECTORS_DISABLED sin tocar nada
    Given una sesión válida con CSRF, sin APP_CONNECTOR_KEY configurada y una fila cifrada de conexión GitLab
    When envía <peticion>
    Then recibe HTTP 503 con código CONNECTORS_DISABLED
    And la fila de conexión, las tareas, los enlaces y los recibos no cambian y el servidor falso recibe cero peticiones
    And GET /api/v1/me/connectors sigue devolviendo HTTP 200 con la fila gitlab disabled
    Examples:
      | peticion                                                     |
      | GET /api/v1/me/connectors/gitlab                             |
      | PUT /api/v1/me/connectors/gitlab con cuerpo válido           |
      | DELETE /api/v1/me/connectors/gitlab                          |
      | POST /api/v1/me/connectors/gitlab/imports con projectId propio |

  @s30
  Scenario Outline: El recibo se recupera por su id y el ajeno equivale al inexistente
    Given un recibo de importación gitlab completed del propietario A con created 2
    When <quien> consulta GET /api/v1/me/connectors/gitlab/imports/<id>
    Then recibe HTTP <http> con <cuerpo>
    Examples:
      | quien           | id                       | http | cuerpo                                                         |
      | A               | el id de ese recibo      | 200  | el mismo recibo que devolvió el POST, con created 2            |
      | el propietario B| el id de ese recibo      | 404  | código IMPORT_NOT_FOUND y el mismo mensaje público que el inexistente |
      | A               | un UUID inexistente      | 404  | código IMPORT_NOT_FOUND                                        |
      | A               | "no-uuid"                | 400  | código VALIDATION_ERROR                                        |

  # ---------- Seguridad ----------

  @s31
  Scenario Outline: Sesión, CSRF y origen se exigen en todas las rutas del catálogo y de GitLab
    Given <credencial>
    When envía <peticion>
    Then recibe HTTP <http> con código <codigo>
    And no se crea ni modifica conexión, tarea, enlace ni recibo y el servidor falso recibe cero peticiones
    # Enmienda del 10 de septiembre de 2026. Las dos filas de credencial Bearer esperaban
    # 401 UNAUTHENTICATED; la línea 5 de esta feature dice que donde 27 fija algo distinto
    # prevalece 27, y el @s31 de features/github_connector.feature ya se enmendó a
    # 403 API_SCOPE_DENIED con el propietario delante: una credencial Bearer válida SÍ está
    # autenticada, y el filtro de 24 la identifica antes de mirar su lista de rutas, que no
    # incluye ni el catálogo ni el conector. Decir «no sé quién eres» a quien se ha
    # identificado es falso. La propiedad de seguridad no cambia: no abre nada, no escribe
    # nada y no contacta con el servidor falso.
    Examples:
      | credencial                                          | peticion                                          | http | codigo            |
      | ninguna sesión                                      | GET /api/v1/me/connectors                         | 401  | UNAUTHENTICATED   |
      | ninguna sesión                                      | GET /api/v1/me/connectors/gitlab                  | 401  | UNAUTHENTICATED   |
      | una credencial Bearer válida de 24                  | GET /api/v1/me/connectors                         | 403  | API_SCOPE_DENIED  |
      | una credencial Bearer válida de 24                  | POST /api/v1/me/connectors/gitlab/imports         | 403  | API_SCOPE_DENIED  |
      | sesión válida sin token CSRF                        | PUT /api/v1/me/connectors/gitlab                  | 403  | CSRF_INVALID     |
      | sesión válida sin token CSRF                        | DELETE /api/v1/me/connectors/gitlab               | 403  | CSRF_INVALID     |
      | sesión válida sin token CSRF                        | POST /api/v1/me/connectors/gitlab/imports         | 403  | CSRF_INVALID     |
      | sesión válida con CSRF y cabecera Origin ajena      | PUT /api/v1/me/connectors/gitlab                  | 403  | UNTRUSTED_ORIGIN |

  @s32
  Scenario: El token nunca sale del servidor salvo en la cabecera PRIVATE-TOKEN
    Given una conexión GitLab connected creada con el token "glpat-SECRETOSECRETO1234" y una importación fallida por 401
    When se capturan las respuestas de GET catálogo, GET gitlab, PUT gitlab, POST imports, GET recibo, el problema JSON del fallo y los logs de la aplicación
    Then ninguna respuesta, problema JSON ni línea de log contiene "glpat-SECRETOSECRETO1234"
    And los logs del fallo contienen el código CONNECTION_INVALID y el identificador de correlación de la petición
    And las peticiones al servidor falso llevan el token únicamente en PRIVATE-TOKEN y nunca en la URL ni en el cuerpo
    And el navegador no guarda el token en localStorage, sessionStorage ni cookies tras enviar el formulario

  # ---------- Interfaz ----------

  @s33
  Scenario: /conectores muestra las seis filas con estado accesible y enlace a cada conector disponible
    Given una sesión válida con GitLab connected, GitHub en status error y las otras cuatro integraciones sin configurar
    When abre la entrada «Conectores», situada después de «Importación» sin desplazar «Hoy», y llega a /conectores
    Then el encabezado h1 es «Conectores» y la lista tiene exactamente seis filas en el orden del catálogo
    And cada fila muestra el nombre, el estado como texto («Conectado», «No conectado», «Deshabilitado», «Error») y un marcador no dependiente del color con el mismo texto accesible
    And la fila GitHub muestra el último error como código traducido y la última actividad en la zona del usuario
    And la fila GitLab enlaza a /conectores/gitlab, la de API para integraciones a /integraciones y la de GitHub a /integraciones/github
    And cada fila de 25, 26 y 28 enlaza a su ruta propia si está desplegada y, si no, muestra «No disponible» sin enlace
    And abrir la vista ejecuta exactamente una petición, GET /api/v1/me/connectors

  @s34
  Scenario: /conectores/gitlab sin conexión ofrece el formulario y no anuncia éxito antes de la confirmación
    Given una sesión válida sin conexión GitLab y el servidor falso listo para aceptar el proyecto
    When abre /conectores/gitlab, rellena token y projectPath «grupo/proyecto» y pulsa «Conectar»
    Then el campo del token tiene type="password", autocomplete="off", etiqueta asociada y nunca está precargado
    And projectPath tiene ayuda visible «grupo/proyecto» y la ayuda recomienda un PAT con alcance read_api
    And mientras la petición está en curso el botón está deshabilitado y un aviso aria-live indica «Guardando…»
    And sólo tras HTTP 200 muestra «Conectado», tokenHint «••••WXYZ», el proyecto y vacía el campo del token
    And si la petición falla muestra el error junto al campo afectado o un aviso aria-live con el código traducido, conserva projectPath y no muestra «Conectado»

  @s35
  Scenario: Con conexión existente la pantalla permite importar, actualizar token y desconectar con confirmación
    Given una sesión válida con GitLab connected, dos proyectos propios abiertos y uno completed
    When abre /conectores/gitlab
    Then muestra tokenHint, projectPath y projectId, y los botones «Importar issues», «Actualizar token» y «Desconectar»
    And el selector nativo de proyecto destino lista únicamente los dos proyectos abiertos
    And pulsar «Importar issues» deshabilita el botón y anuncia por aria-live un progreso sin porcentaje hasta recibir el recibo
    And el recibo se presenta como cuatro cifras etiquetadas «Creadas», «Omitidas», «Fallidas» y «Truncado», con aviso visible si truncated es true
    And «Desconectar» exige una confirmación explícita cuyo texto dice que las tareas importadas se conservan, y sólo tras HTTP 204 vuelve al estado sin conexión
    And «Actualizar token» muestra el formulario con el campo del token vacío

  @s36
  Scenario Outline: Los errores recuperables se muestran con la acción que corresponde
    Given una sesión válida en /conectores/gitlab y el servidor responde <error> a la acción del usuario
    When el usuario <accion>
    Then la pantalla muestra <presentacion>
    And no muestra ningún estado de éxito ni altera la vista con datos no confirmados
    Examples:
      | error                          | accion                       | presentacion                                                                                   |
      | 503 RATE_LIMITED con 30 s      | pulsa «Importar issues»      | un aviso con «30 segundos» y el botón habilitado de nuevo                                       |
      | 409 CONNECTION_INVALID         | pulsa «Importar issues»      | el estado «Error» y el botón «Actualizar token» enfocado como acción sugerida                  |
      | 409 IMPORT_IN_PROGRESS         | pulsa «Importar issues»      | un aviso de importación en curso y el botón «Actualizar estado», que relee GET gitlab y el catálogo |
      | 503 CONNECTORS_DISABLED        | abre la pantalla             | un texto que explica que falta configuración del servidor, sin formulario ni botones de acción  |
      | 503 GITLAB_UNAVAILABLE         | pulsa «Conectar»             | un aviso de proveedor no disponible con reintento manual y el projectPath conservado           |
      | fallo de red sin respuesta     | pulsa «Importar issues»      | el botón «Actualizar estado» sin reintento automático                                          |

  @s37
  Scenario Outline: Cancelación y cierre de sesión no aplican respuestas tardías ni retienen datos
    Given una sesión válida en /conectores/gitlab con una petición de <peticion> en curso
    When <interrupcion> antes de que llegue la respuesta
    Then la respuesta tardía no modifica la vista ni anuncia nada por aria-live
    And <estadoFinal>
    Examples:
      | peticion    | interrupcion                          | estadoFinal                                                                                  |
      | importación | navega a /conectores                  | la lista del catálogo se carga con su propia petición y no muestra el recibo de la importación |
      | conexión    | cierra sesión                         | el formulario, el token y el projectPath no permanecen en memoria, almacenamiento ni caché    |
      | importación | la sesión expira y responde 401       | la aplicación muestra la pantalla de acceso sin datos del conector visibles                   |

  @s38
  Scenario: Responsive, texto ampliado, teclado y axe en /conectores y /conectores/gitlab
    Given una sesión válida con GitLab connected y un recibo reciente truncated true
    When revisa ambas pantallas a 320, 768 y 1280 px CSS, con texto al 200 % y con zoom nativo 200 %
    Then no hay desbordamiento horizontal ni acciones o cifras del recibo recortadas
    And todos los controles interactivos miden al menos 44 × 44 px CSS, son alcanzables por teclado en orden lógico y muestran foco visible
    And al cambiar de estado (conectado, importando, recibo, desconectado) el foco pasa al h1 o al aviso de resultado
    And axe no reporta violaciones en ninguna de las dos pantallas en ninguno de los tres anchos
    And la evidencia registra los límites humanos y de dispositivos sin inferir cumplimiento universal desde axe
