Feature: Autorizar integraciones personales sin entregar la sesión humana
  Como persona autenticada quiero credenciales limitadas, caducables y revocables
  para usar las operaciones existentes sin compartir mi contraseña ni mis cookies.
  Los contratos de negocio y errores de las features 1–23 siguen vigentes.
  Las pruebas de escritura, cuota y concurrencia usan infraestructura efímera.

  @s1
  Scenario: Crear una credencial muestra el secreto únicamente tras confirmar
    Given una sesión humana válida, cupo disponible e id canónica nueva
    When crea una credencial con nombre, scopes y expiresInDays válidos
    Then recibe 201 con Location y exactamente credential y secret
    And credential contiene sólo id, name, scopes, createdAt, expiresAt y revokedAt
    And secret tiene formato canónico owp_UUID.SECRETO con 32 bytes aleatorios
    And sólo persiste el verificador SHA-256, nunca el secreto recuperable
    And no modifica datos de negocio ni publica eventos

  @s2
  Scenario Outline: Los valores de gestión tienen límites cerrados
    Given una petición de creación autenticada con <entrada>
    When solicita crear la credencial
    Then recibe 400 API_CREDENTIAL_INVALID con fields sin datos privados
    And no crea credencial ni consume cupo
    Examples:
      | entrada |
      | nombre vacío después de strip Unicode |
      | nombre de 81 puntos de código |
      | nombre con controles |
      | scopes vacío o desconocido |
      | scope duplicado |
      | expiresInDays distinto de 7, 30 o 90 |
      | UUID no canónico minúsculo |

  @s3
  Scenario: Nombre Unicode y scopes conservan su intención canónica
    Given una sesión válida y un nombre con 80 puntos de código rodeado de espacio Unicode
    And scopes válidos y otra credencial con el mismo nombre
    When crea una nueva credencial
    Then recibe el nombre tras strip y scopes en el orden canónico de la sección 24
    And admite el nombre repetido con identidad distinta

  @s4
  Scenario Outline: El cuerpo de gestión se limita antes de materializar
    Given una petición de creación autenticada con <cuerpo>
    When la envía a su id
    Then recibe <resultado> y <efecto>
    Examples:
      | cuerpo | resultado | efecto |
      | JSON válido de exactamente 4096 bytes con padding | 201 | crea una credencial |
      | JSON estructuralmente válido que rebasa 4096 bytes | 413 API_CREDENTIAL_TOO_LARGE | no crea credencial |
      | propiedad desconocida | 400 API_CREDENTIAL_INVALID | no crea credencial |
      | propiedad JSON duplicada | 400 API_CREDENTIAL_INVALID | no crea credencial |
      | segundo valor después del objeto | 400 API_CREDENTIAL_INVALID | no crea credencial |

  @s5
  Scenario Outline: Caducidad calculada una vez con reloj UTC
    Given reloj válido con fracción 123456789 nanosegundos y cupo disponible
    When crea la credencial por <dias> días
    Then createdAt usa precisión de microsegundos y expiresAt está a <horas> horas UTC
    And revokedAt es null y no se amplía la caducidad al consultar
    Examples:
      | dias | horas |
      | 7 | 168 |
      | 30 | 720 |
      | 90 | 2160 |

  @s6
  Scenario: Un reloj no representable no deja una creación parcial
    Given una creación válida cuyo reloj o caducidad calculada sale de los años 0001–9999
    When solicita crear la credencial
    Then recibe 503 STORAGE_UNAVAILABLE sin insertar ni entregar secreto

  @s7
  Scenario Outline: Sólo las credenciales válidas ocupan el cupo de diez
    Given <validas> credenciales válidas propias y otras caducadas o revocadas
    When crea una credencial nueva
    Then recibe <resultado> y quedan <finales> credenciales válidas
    And conserva el historial anterior
    Examples:
      | validas | resultado | finales |
      | 9 | 201 | 10 |
      | 10 | 409 API_CREDENTIAL_LIMIT | 10 |

  @s8
  Scenario: Dos creaciones compiten por la última plaza
    Given nueve credenciales válidas propias y dos conexiones con ids nuevas distintas
    When ambas solicitan crear concurrentemente
    Then exactamente una recibe 201 y la otra 409 API_CREDENTIAL_LIMIT
    And quedan diez credenciales válidas y sólo la ganadora recibe secreto

  @s9
  Scenario Outline: El replay confirmado nunca regenera el secreto
    Given una creación confirmada cuya credencial está <estado>
    And el cupo actual está completo
    When repite PUT con la misma id y la misma intención canónica
    Then recibe 200 con la misma credential y secret null
    And conserva verificador, fechas y revocación sin consumir otra plaza
    Examples:
      | estado |
      | vigente |
      | caducada |
      | revocada |

  @s10
  Scenario Outline: La identidad del intento no permite sustituir intención o propietario
    Given una id ya ocupada por <existente>
    When solicita crear con esa id
    Then recibe 409 API_CREDENTIAL_CONFLICT sin secreto ni datos de otro owner
    And conserva la credencial existente
    Examples:
      | existente |
      | intención propia con nombre distinto |
      | intención propia con scopes distintos |
      | intención propia con expiresInDays distinto |
      | otro owner |

  @s11
  Scenario: Dos reintentos concurrentes entregan un único secreto
    Given una id nueva y dos conexiones con la misma intención válida
    When ambas crean concurrentemente
    Then una recibe 201 con secreto y la otra 200 con secret null
    And sólo existe una credencial con sus fechas originales

  @s12
  Scenario Outline: Consultar una identidad mantiene propiedad y privacidad
    Given una sesión propia y una id <estado>
    When consulta GET de esa credencial
    Then recibe <resultado> sin secreto, verificador ni intención original
    Examples:
      | estado | resultado |
      | propia vigente, caducada o revocada | 200 credential |
      | ajena | 404 API_CREDENTIAL_NOT_FOUND |
      | inexistente | 404 API_CREDENTIAL_NOT_FOUND |

  @s13
  Scenario: Listar historial propio paginado no escribe contadores
    Given 51 credenciales propias con empates de createdAt y credenciales ajenas
    When recorre el listado de 50 elementos con su cursor válido
    Then recibe exactamente las propias en orden createdAt DESC e id DESC sin duplicarlas
    And cada página contiene sólo items y nextCursor
    And no escribe lastUsed, contadores ni credenciales y no realiza consultas por cada elemento

  @s14
  Scenario Outline: La gestión rechaza entradas ajenas a su contrato
    Given una sesión válida y <peticion>
    When solicita la operación de gestión
    Then recibe <resultado>
    And no escribe datos ni devuelve secretos
    Examples:
      | peticion | resultado |
      | listado con cursor inválido | 400 API_CREDENTIAL_INVALID |
      | query owner o query no admitida | 400 API_CREDENTIAL_INVALID |
      | GET con cuerpo no vacío | 400 API_CREDENTIAL_INVALID |
      | revocación con cuerpo o query | 400 API_CREDENTIAL_INVALID |
      | método no permitido | 405 según contrato HTTP existente |

  @s15
  Scenario: Revocar es irreversible e idempotente
    Given una credencial propia ya revocada en un instante conocido
    When repite PUT de revocación sin cuerpo ni query
    Then recibe 200 credential con exactamente el mismo revokedAt
    And conserva scopes, caducidad y datos de negocio sin eventos

  @s16
  Scenario Outline: Revocar mantiene propiedad y sólo confirma después de commit
    Given una credencial <estado>
    When solicita revocarla
    Then recibe <resultado>
    And no cambia otras credenciales ni cierra la sesión humana
    Examples:
      | estado | resultado |
      | propia vigente | 200 credential con revokedAt persistido |
      | ajena | 404 API_CREDENTIAL_NOT_FOUND sin datos ajenos |
      | inexistente | 404 API_CREDENTIAL_NOT_FOUND |

  @s17
  Scenario Outline: Fallos de persistencia no fabrican confirmación
    Given una creación o revocación válida con <fallo>
    When la confirma
    Then recibe 503 si todavía puede responder y no una confirmación inventada
    And <efecto>
    Examples:
      | fallo | efecto |
      | rechazo comprobable antes de commit | no deja cambios parciales |
      | pérdida de respuesta de COMMIT | mantiene resultado incierto y permite recuperar por id |

  @s18
  Scenario: Authorization ausente conserva la seguridad humana
    Given una petición sin Authorization a una ruta de gestión o de negocio
    When la envía con sesión, CSRF y Origin del caso bajo prueba
    Then conserva los resultados y precedencia de los contratos de sesión existentes
    And incluye sus 401 UNAUTHENTICATED, 403 CSRF_INVALID o UNTRUSTED_ORIGIN y 503 SESSION_UNAVAILABLE

  @s19
  Scenario Outline: Authorization inválido nunca cae a una cookie válida
    Given una cookie humana válida y Authorization <entrada>
    When solicita una ruta de la API
    Then recibe 401 API_UNAUTHENTICATED con WWW-Authenticate Bearer
    And no ejecuta negocio, consume cuota ni revela existencia u owner
    Examples:
      | entrada |
      | dos headers Authorization |
      | esquema distinto de Bearer |
      | token con coma o espacios añadidos |
      | UUID o base64url no canónicos |
      | token desconocido o secreto incorrecto |
      | token caducado |
      | token revocado |

  @s20
  Scenario Outline: El canal Bearer no depende del almacén de sesiones
    Given un Bearer válido del owner bootstrap habilitado y <cookie>
    When solicita una lectura permitida con esquema bearer sin distinguir mayúsculas
    Then obtiene la respuesta propia sin consultar ni crear sesión JDBC
    And no emite Set-Cookie ni utiliza la identidad de la cookie
    Examples:
      | cookie |
      | ninguna cookie |
      | cookie válida de sesión |
      | cookie inválida con almacén de sesión inaccesible |

  @s21
  Scenario Outline: Una credencial no habilita una identidad huérfana
    Given una credencial cuyo owner bootstrap está <estado>
    When intenta autenticar una petición Bearer
    Then recibe el mismo 401 API_UNAUTHENTICATED sin negocio ni datos ajenos
    Examples:
      | estado |
      | renombrado |
      | deshabilitado |
      | diferente del usuario actualmente configurado |

  @s22
  Scenario Outline: La allowlist concede únicamente las rutas y métodos explícitos
    Given un Bearer válido con únicamente <scope> y recursos propios válidos
    When solicita <operacion>
    Then usa los mismos contratos de respuesta, propiedad, paginación y negocio existentes
    And no concede otro scope implícitamente
    Examples:
      | scope | operacion |
      | projects:read | GET /api/v1/projects |
      | projects:read | GET /api/v1/projects/{id} |
      | projects:write | POST /api/v1/projects |
      | projects:write | PUT /api/v1/projects/{id} |
      | tasks:read | GET /api/v1/projects/{projectId}/tasks |
      | tasks:read | GET /api/v1/projects/{projectId}/tasks/{taskId} |
      | tasks:read | GET /api/v1/projects/{projectId}/tasks/{id}/status |
      | tasks:read | GET /api/v1/projects/{projectId}/tasks/{id}/parent |
      | tasks:read | GET /api/v1/projects/{projectId}/tasks/{parentId}/subtasks |
      | tasks:write | POST /api/v1/projects/{projectId}/tasks |
      | agenda:read | GET /api/v1/today |
      | agenda:read | GET /api/v1/projects/{projectId}/tasks/{taskId}/blocks |
      | agenda:read | GET /api/v1/projects/{projectId}/tasks/{taskId}/blocks/{blockId} |
      | agenda:read | GET /api/v1/projects/{projectId}/tasks/{taskId}/blocks/{blockId}/state |
      | agenda:read | GET /api/v1/projects/{projectId}/tasks/{taskId}/blocks/by-request/{requestKey} |
      | history:read | GET /api/v1/history |
      | history:read | GET /api/v1/weekly-review |
      | history:read | GET /api/v1/projects/{projectId}/tasks/{id}/history |

  @s23
  Scenario Outline: Ni un prefijo compartido ni una cookie amplían los permisos
    Given un Bearer válido sin permiso para <operacion> y una cookie humana válida
    When solicita <operacion>
    Then recibe 403 API_SCOPE_DENIED sin CSRF_INVALID, negocio ni consumo de cuota
    Examples:
      | operacion |
      | GET de proyecto con sólo projects:write |
      | PUT de proyecto con sólo projects:read |
      | HEAD u OPTIONS de una ruta permitida para GET |
      | transición de estado de proyecto o tarea |
      | crear o modificar bloque |
      | consultar sesión de trabajo fuera de la allowlist |
      | consultar preferencias, exportación o importación |
      | gestionar credenciales o cerrar sesión |
      | ruta desconocida con prefijo parecido |

  @s24
  Scenario Outline: La precedencia Bearer conserva autenticación antes de permiso y cuota
    Given <estado> y una escritura con Origin <origin>
    When envía la petición
    Then recibe <resultado> sin ejecutar negocio
    Examples:
      | estado | origin | resultado |
      | token inválido, sin scope y cuota agotada | ajeno | 401 API_UNAUTHENTICATED |
      | token válido, sin scope y cuota agotada | ajeno | 403 UNTRUSTED_ORIGIN |
      | token válido sin scope y cuota agotada | ausente | 403 API_SCOPE_DENIED |
      | token válido con scope y cuota agotada | ausente | 429 API_RATE_LIMITED |

  @s25
  Scenario: Escritura servidor permitida conserva ETag y outbox existentes
    Given un Bearer con projects:write y una versión vigente de proyecto propio
    When edita sin Origin ni CSRF usando el If-Match correcto
    Then guarda con las mismas versiones, hechos y eventos del contrato de edición existente
    And los casos de propiedad y precondición incorrectas conservan sus errores existentes
    And no convierte los POST de proyecto o tarea en operaciones idempotentes

  @s26
  Scenario Outline: Los límites inclusivos no gastan parcialmente la otra cuota
    Given <token> solicitudes consumidas por credencial y <owner> por owner en la ventana actual
    When una solicitud autorizada intenta consumir cuota
    Then recibe <resultado> y quedan <tokenFinal> y <ownerFinal> consumidas respectivamente
    Examples:
      | token | owner | resultado | tokenFinal | ownerFinal |
      | 59 | 119 | acceso a negocio | 60 | 120 |
      | 60 | 100 | 429 API_RATE_LIMITED | 60 | 100 |
      | 10 | 120 | 429 API_RATE_LIMITED | 10 | 120 |

  @s27
  Scenario Outline: Dos réplicas compiten por la última solicitud admitida
    Given cuota restante de una solicitud <limite> y dos conexiones con <credenciales>
    When solicitan simultáneamente una operación permitida
    Then sólo una accede a negocio y la otra recibe 429 API_RATE_LIMITED
    And no rebasa ninguno de los dos contadores ni hace incrementos parciales
    Examples:
      | limite | credenciales |
      | por credencial | la misma credencial |
      | por owner | credenciales diferentes del mismo owner |

  @s28
  Scenario: La siguiente ventana UTC libera cuota sin acumular filas por petición
    Given cuota agotada inmediatamente antes del siguiente minuto UTC
    When solicita una operación permitida justo en el nuevo minuto
    Then accede a negocio con ambos contadores en uno para esa ventana
    And los rechazos anteriores indican Retry-After entero hasta la próxima ventana, mínimo uno
    And conserva contadores compactos sin historial ilimitado por solicitud

  @s29
  Scenario Outline: Sólo las peticiones autorizadas consumen cuota
    Given <peticion>
    When solicita la operación
    Then <efecto>
    Examples:
      | peticion | efecto |
      | Bearer inválido o scope denegado | no crea ni incrementa contadores |
      | Bearer autorizado con error de negocio | consume una solicitud |
      | cookie humana sin Authorization | no consume cuota Bearer |
      | Bearer autorizado con almacén de cuota inaccesible | recibe 503 sin fallback que ejecute negocio |

  @s30
  Scenario Outline: Revocación y autorización tienen una frontera observable
    Given una solicitud cuya autorización empieza <orden> de revocación confirmada
    When intenta utilizar la credencial
    Then <resultado>
    Examples:
      | orden | resultado |
      | antes y ya quedó admitida | puede terminar después sin prometer cancelación de su escritura |
      | después del commit | recibe 401 API_UNAUTHENTICATED sin acceder a negocio |

  @s31
  Scenario Outline: Caducidad se decide al autorizar
    Given una credencial válida salvo por su instante de autorización <instante>
    When intenta autenticar una solicitud permitida
    Then recibe <resultado>
    Examples:
      | instante | resultado |
      | inmediatamente anterior a expiresAt | acceso a negocio |
      | exactamente expiresAt | 401 API_UNAUTHENTICATED |
      | posterior a expiresAt | 401 API_UNAUTHENTICATED |

  @s32
  Scenario: OpenAPI es una excepción de lectura exacta y privada
    Given una sesión propia o un Bearer válido sin cuota restante
    When solicita GET /api/v1/integration-openapi.json
    Then recibe el documento OpenAPI 3.1 validado de la allowlist y seis scopes
    And sus cuerpos, ETags y errores corresponden a los contratos vigentes sin ejemplos privados
    And no consume cuota ni ejecuta negocio; Bearer no consulta sesión ni emite Set-Cookie
    And no abre Swagger UI ni otras rutas de documentación

  @s33
  Scenario: La creación humana prepara recuperación antes de enviar
    Given el formulario de integraciones con nombre, scopes explícitos y caducidad por defecto de 30 días
    When confirma Crear
    Then retiene sólo owner e id del intento en sessionStorage antes de enviar el PUT
    And muestra una ayuda de visualización única y no selecciona lectura implícita por escritura
    And una respuesta 201 muestra el secreto sólo en memoria con selección y botón Copiar

  @s34
  Scenario: Un almacenamiento de intención inaccesible impide enviar creación
    Given sessionStorage no disponible y un formulario válido
    When intenta crear una credencial
    Then explica que necesita conservar la recuperación y no envía PUT
    And no almacena secreto ni datos del formulario en almacenamiento persistente

  @s35
  Scenario Outline: La recuperación manual conserva identidad tras respuesta perdida
    Given una creación incierta con id retenida <contexto>
    When pulsa Comprobar creación y recibe <respuesta>
    Then <efecto>
    And no crea automáticamente otra id ni reenvía el PUT
    And mientras el intento siga incierto bloquea Crear otro intento con id distinta, también tras 404 o reload
    And mantiene disponibles la comprobación y el reenvío deliberados con la misma id
    Examples:
      | contexto | respuesta | efecto |
      | en la ruta actual | 404 | mantiene el intento porque el primer PUT podría confirmar después |
      | después de reload | 404 | mantiene id y permite reintroducir manualmente la misma intención |
      | después de reload | 200 credential | explica que el secreto no puede recuperarse y ofrece revocación deliberada |

  @s36
  Scenario Outline: Reintentar deliberadamente conserva la identidad del intento
    Given un intento con <estado> y la misma id retenida
    When lo reenvía explícitamente con <intencion>
    Then obtiene <resultado>
    And no sustituye automáticamente la id ni interpreta un secreto perdido como recuperable
    Examples:
      | estado | intencion | resultado |
      | confirmación incierta ya persistida | intención original | 200 secret null e información para revocar y crear otra tras resolver |
      | rechazo definitivo por validación antes de transacción | borrador corregido | 201 si ya es válido y tiene cupo |
      | rechazo definitivo por cupo y una plaza ahora libre | intención original | 201 con una sola credencial nueva |
      | commit tardío con intención diferente | borrador reintroducido distinto | 409 y consulta de esa id antes de resolver |

  @s37
  Scenario Outline: El secreto desaparece al retirar su contexto
    Given una creación pendiente o un secreto visible del owner actual
    When ocurre <retiro>
    Then oculta el secreto y ninguna respuesta tardía vuelve a mostrarlo
    And retira intentos ajenos y conserva borradores de otras funciones
    Examples:
      | retiro |
      | abandonar la ruta o desmontar la pantalla |
      | cerrar el panel del secreto |
      | cambiar de owner |
      | cerrar sesión |

  @s38
  Scenario: Fallar la limpieza de intención no revierte una confirmación
    Given una operación confirmada y removeItem de sessionStorage que falla
    When procesa la confirmación
    Then mantiene el resultado confirmado y no muestra un fallo de creación ficticio
    And no impide cerrar sesión ni vuelve a divulgar el secreto

  @s39
  Scenario: Copiar requiere una acción explícita y admite selección manual
    Given un secreto visible y Clipboard API no disponible
    When pulsa Copiar
    Then ofrece selección manual del secreto y explica que no podrá recuperarse después
    And no descarga archivos ni copia automáticamente ni persiste el secreto

  @s40
  Scenario Outline: Recuperar una revocación incierta no escribe automáticamente
    Given una revocación incierta de una credencial identificada por nombre
    When consulta manualmente su estado y revokedAt está <estado>
    Then <efecto>
    And no ejecuta PUT automático al navegar ni al recuperar visibilidad
    Examples:
      | estado | efecto |
      | presente | confirma la revocación |
      | null | ofrece repetir explícitamente la revocación con su consecuencia visible |

  @s41
  Scenario: Gestión privada accesible sin carga global
    Given la pantalla de integraciones en los temas existentes y las modalidades de UX30 aplicables
    When recorre por teclado listado, creación, copia y confirmación de revocación
    Then conserva foco visible y recuperación de foco, etiquetas y objetivos táctiles de 44px
    And admite reflow, zoom y movimiento reducido según docs/ux-requirements.md sin recortes ni acciones inaccesibles
    And las lecturas privadas sólo ocurren al abrir su ruta, sin GET global ni N+1
    And la ruta privada /integraciones/api tiene H1 Credenciales para integraciones y enlace al final de navegación
    And se recoge evidencia responsive y de tres motores, sin atribuir juicio humano sólo a axe

  @s42
  Scenario: Las credenciales no se propagan a copias o registros de aplicación
    Given creación, autenticación, revocación y recuperación con secretos ficticios rastreables
    When inspecciona respuestas, logs y una exportación o importación v1 de la cuenta efímera
    Then no encuentra Authorization, secreto ni verificador en logs o copias v1
    And gestión y errores de autenticación usan Cache-Control no-store
    And credenciales y contadores quedan fuera de export/import sin restaurar permisos externos
