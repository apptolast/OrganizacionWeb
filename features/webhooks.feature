Feature: Entregar los eventos propios ya confirmados a URLs https elegidas con firma verificable
  Como propietario quiero registrar hasta cinco webhooks salientes y consultar sus entregas
  para integrar mis hechos de trabajo sin exponer secretos ni depender de un tercero.
  La fuente única es la outbox existente; no se crean eventos ni se altera el publicador RabbitMQ.
  Rutas, DTO cerrados, códigos de error, firma, plazos y límites son los de la sección 25 de project-spec.md.
  Se reutilizan sesión, CSRF/origen, JSON estricto, problem+json y no-store de 20–24.
  La evidencia UX aplica docs/ux-requirements.md sin atribuir resultados antes de implementarlos.

  @s1
  Scenario: Crear un webhook devuelve el secreto una sola vez y un endpoint con cursor en el presente
    Given una cuenta autenticada sin webhooks y un reloj fijo en 2026-09-08T10:00:00.000000Z
    When envía POST /api/v1/me/webhooks con url "https://example.com/hooks", description "  Mi hook  " y eventTypes [TaskStatusChanged.v1, TaskCreated.v1]
    Then recibe 201 con Location /api/v1/me/webhooks/<id> y un cuerpo con exactamente endpoint y secret
    And endpoint contiene exactamente id, url, description, eventTypes, status, disabledReason, disabledAt, createdAt y updatedAt
    And description es "Mi hook", eventTypes es exactamente [TaskCreated.v1, TaskStatusChanged.v1] en orden de catálogo y status es active
    And disabledReason y disabledAt son null y createdAt y updatedAt son 2026-09-08T10:00:00.000000Z
    And secret empieza por whsec_ seguido de exactamente 43 caracteres base64url sin relleno
    And la respuesta es application/json con Cache-Control no-store
    And el cursor persistido es (2026-09-08T10:00:00.000000Z, 00000000-0000-0000-0000-000000000000)

  @s2
  Scenario Outline: La URL sólo admite https absoluto con host y sin credenciales ni fragmento
    Given una cuenta autenticada con plaza disponible
    When envía POST /api/v1/me/webhooks con url <url> y datos válidos en el resto
    Then recibe 400 WEBHOOK_INVALID con una entrada de errors para el campo url
    And no se inserta ningún endpoint ni se resuelve DNS
    Examples:
      | url                                   |
      | "http://example.com/hooks"            |
      | "HTTPS://example.com/hooks"           |
      | "https://user:pw@example.com/hooks"   |
      | "https://example.com/hooks#frag"      |
      | "https:///hooks"                      |
      | "https://example.com:0/hooks"         |
      | "https://example.com:65536/hooks"     |
      | "example.com/hooks"                   |
      | ""                                    |
      | una cadena https de 2049 puntos de código |

  @s3
  Scenario Outline: Descripción y tipos de evento se validan campo a campo
    Given una cuenta autenticada con plaza disponible
    When envía POST /api/v1/me/webhooks con url válida, description <description> y eventTypes <eventTypes>
    Then recibe <resultado>
    Examples:
      | description                              | eventTypes                                   | resultado |
      | null                                     | [ProjectCreated.v1]                          | 201 con description "" |
      | ausente                                  | [ProjectCreated.v1]                          | 201 con description "" |
      | 80 puntos de código sin control          | [ProjectCreated.v1]                          | 201 con los 80 puntos íntegros |
      | 81 puntos de código                      | [ProjectCreated.v1]                          | 400 WEBHOOK_INVALID con errors para description |
      | "a" seguido de U+0001 y "b"              | [ProjectCreated.v1]                          | 400 WEBHOOK_INVALID con errors para description |
      | ""                                       | []                                           | 400 WEBHOOK_INVALID con errors para eventTypes |
      | ""                                       | [webhook.ping.v1]                            | 400 WEBHOOK_INVALID con errors para eventTypes |
      | ""                                       | [TaskCreated.v1, TaskCreated.v1]             | 400 WEBHOOK_INVALID con errors para eventTypes |
      | ""                                       | [taskcreated.v1]                             | 400 WEBHOOK_INVALID con errors para eventTypes |
      | ""                                       | los doce tipos del catálogo                  | 201 con los doce tipos en orden de catálogo |
      | ""                                       | los doce tipos más ProjectCreated.v2         | 400 WEBHOOK_INVALID con errors para eventTypes |
      | 81 puntos de código                      | []                                           | 400 WEBHOOK_INVALID con errors para description y eventTypes |

  @s4
  Scenario Outline: La frontera HTTP rechaza estructura y tamaño antes de validar valores
    Given una cuenta autenticada con plaza disponible
    When envía POST /api/v1/me/webhooks con <peticion>
    Then recibe <resultado> sin insertar endpoint
    Examples:
      | peticion                                                        | resultado |
      | un cuerpo JSON válido de exactamente 4096 bytes UTF-8            | 201 |
      | un cuerpo de 4097 bytes UTF-8 con Content-Length declarado       | 413 WEBHOOK_TOO_LARGE antes de leer el JSON |
      | una propiedad desconocida extra                                  | 400 MALFORMED_JSON |
      | la propiedad url repetida dos veces                               | 400 MALFORMED_JSON |
      | un objeto válido seguido de tokens finales                        | 400 MALFORMED_JSON |
      | Content-Type text/plain                                          | 415 |
      | una query ?owner=otro                                            | 400 WEBHOOK_INVALID |

  @s5
  Scenario Outline: La creación rechaza destinos prohibidos o irresolubles sin guardar nada
    Given una cuenta autenticada con plaza disponible y una resolución DNS controlada
    When envía POST /api/v1/me/webhooks con url <url>
    Then recibe 400 <codigo> como application/problem+json
    And no se inserta ningún endpoint ni se abre conexión saliente
    Examples:
      | url                                            | codigo |
      | "https://127.0.0.1/h"                          | WEBHOOK_URL_BLOCKED |
      | "https://0.0.0.0/h"                            | WEBHOOK_URL_BLOCKED |
      | "https://10.1.2.3/h"                           | WEBHOOK_URL_BLOCKED |
      | "https://172.16.0.9/h"                         | WEBHOOK_URL_BLOCKED |
      | "https://192.168.1.1/h"                        | WEBHOOK_URL_BLOCKED |
      | "https://100.64.0.1/h"                         | WEBHOOK_URL_BLOCKED |
      | "https://169.254.169.254/h"                    | WEBHOOK_URL_BLOCKED |
      | "https://224.0.0.1/h"                          | WEBHOOK_URL_BLOCKED |
      | "https://[::1]/h"                              | WEBHOOK_URL_BLOCKED |
      | "https://[fe80::1]/h"                          | WEBHOOK_URL_BLOCKED |
      | "https://[fc00::1]/h"                          | WEBHOOK_URL_BLOCKED |
      | "https://[::ffff:10.0.0.1]/h"                  | WEBHOOK_URL_BLOCKED |
      | un host que resuelve a 203.0.113.5 y 10.0.0.5  | WEBHOOK_URL_BLOCKED |
      | un host sin registros A ni AAAA                | WEBHOOK_URL_UNRESOLVABLE |

  @s6
  Scenario Outline: El cupo de cinco cuenta también los desactivados
    Given una cuenta con <activos> webhooks activos y <desactivados> desactivados
    When envía POST /api/v1/me/webhooks válido
    Then recibe <resultado>
    And el total de webhooks del propietario queda <total>
    Examples:
      | activos | desactivados | resultado           | total |
      | 4       | 0            | 201                 | 5     |
      | 3       | 2            | 409 WEBHOOK_LIMIT   | 5     |
      | 0       | 5            | 409 WEBHOOK_LIMIT   | 5     |
      | 5       | 0            | 409 WEBHOOK_LIMIT   | 5     |

  @s7
  Scenario: Dos creaciones concurrentes por la última plaza producen un único alta
    Given una cuenta con 4 webhooks y otra cuenta con 0
    When ambas cuentas envían dos POST /api/v1/me/webhooks simultáneos cada una
    Then la primera cuenta recibe exactamente un 201 y un 409 WEBHOOK_LIMIT y queda con 5
    And la segunda cuenta recibe dos 201 y queda con 2
    And ningún endpoint queda parcialmente escrito

  @s8
  Scenario: El secreto sólo existe en claro en la respuesta de creación
    Given un webhook creado cuya respuesta 201 devolvió secret whsec_x
    When consulta GET /api/v1/me/webhooks, GET /api/v1/me/webhooks/{id}, GET /api/v1/me/webhooks/{id}/deliveries y los logs de aplicación
    Then ninguna respuesta contiene la propiedad secret ni la cadena whsec_x
    And la fila persistida guarda un cifrado de al menos 60 bytes cuyo contenido no contiene los bytes de whsec_x
    And descifrar ese valor con la clave y el id del propietario junto al id del endpoint como dato adicional autenticado recupera whsec_x
    And descifrarlo con el id de otro endpoint como dato adicional falla
    And descifrarlo con el id de otro propietario como dato adicional falla
    # Enmienda del 10 de septiembre de 2026, ratificada por el propietario. Decia
    # solo "el id del endpoint". La produccion ata el secreto a propietario Y
    # recurso -ownerId + "|" + endpointId-, que es MAS seguro que lo que pedia el
    # contrato: un endpoint reasignado a otro propietario no puede descifrar su
    # secreto anterior. La politica ya estaba ratificada en project-spec.md (los
    # datos asociados atan propietario ademas de recurso) y esta linea nunca
    # recibio su nota fechada. Se enmienda el contrato, NO la produccion: cambiar
    # el codigo para cumplir la letra seria perder seguridad de verdad. La fila
    # nueva del otro propietario es el oraculo que faltaba.
    # Ver progress/ratificaciones.md, entrada R10.

  @s9
  Scenario Outline: Sin clave de cifrado válida la aplicación arranca degradada
    Given la propiedad app.connectors.key <clave> y un webhook ya persistido
    When la aplicación arranca y el propietario ejecuta <operacion>
    Then la aplicación queda disponible y registra audit workerError CONFIGURATION_ERROR una vez al arrancar
    And <operacion> recibe <resultado>
    And el worker de webhooks no reclama ni envía ninguna entrega
    # Enmienda del 10 de septiembre de 2026, ratificada por el propietario. Dos filas
    # -"base64 de 31 bytes" y "texto no base64"- prometian que la aplicacion queda
    # DISPONIBLE con una clave malformada, y el codigo lanza en los dos casos: el
    # contexto ni siquiera arranca. Describian algo que no ocurre.
    #
    # Manda el codigo, y a proposito: una clave AUSENTE significa "webhooks
    # deshabilitados" y arranca degradado, que es lo que este escenario describe; una
    # clave PRESENTE pero invalida es un error del operador y debe verse al instante,
    # no seis horas despues cuando falle el primer envio. Las dos filas pasan a
    # "ausente", que es el unico caso que el escenario cubre de verdad, y el arranque
    # que falla con clave malformada esta cubierto por el @s6 de la feature 27... que
    # se retiro hoy, asi que queda como hueco declarado en el veredicto de cierre.
    # Ver progress/ratificaciones.md, entrada R11.
    Examples:
      | clave                     | operacion                                  | resultado |
      | ausente                   | POST /api/v1/me/webhooks                   | 503 CONNECTORS_DISABLED |
      | ausente                   | POST /api/v1/me/webhooks/{id}/ping         | 503 CONNECTORS_DISABLED |
      | ausente                   | POST redeliver de una entrega succeeded    | 503 CONNECTORS_DISABLED |
      | ausente                   | GET /api/v1/me/webhooks                    | 200 con items |
      | ausente                   | PUT /api/v1/me/webhooks/{id}/status disabled | 200 con status disabled |
      | ausente                   | DELETE /api/v1/me/webhooks/{id}            | 204 |

  @s10
  Scenario: Listar devuelve sólo los webhooks propios en orden de creación descendente
    Given tres webhooks propios creados en instantes distintos, dos con el mismo createdAt, y dos webhooks de otra cuenta
    When consulta GET /api/v1/me/webhooks sin token CSRF
    Then recibe 200 con exactamente items y tres elementos ordenados por createdAt DESC y después id DESC
    And cada elemento tiene exactamente los nueve campos del DTO endpoint sin secret
    And ningún elemento pertenece a la otra cuenta y la respuesta es no-store
    And una cuenta sin webhooks recibe 200 con items []

  @s11
  Scenario Outline: Un webhook ajeno responde igual que uno inexistente
    Given un webhook de otra cuenta con id X y ningún webhook propio
    When ejecuta <peticion> como la cuenta propia
    Then recibe 404 WEBHOOK_NOT_FOUND con el mismo cuerpo problem+json que para <peticion> con un UUID nunca creado
    And el webhook X conserva status, entregas y cursor sin cambios
    Examples:
      | peticion                                            |
      | GET /api/v1/me/webhooks/X                           |
      | PUT /api/v1/me/webhooks/X/status disabled           |
      | DELETE /api/v1/me/webhooks/X                        |
      | POST /api/v1/me/webhooks/X/ping                     |
      | GET /api/v1/me/webhooks/X/deliveries                |
      | POST /api/v1/me/webhooks/X/deliveries/{d}/redeliver |
      | GET /api/v1/me/webhooks/no-es-uuid                  |

  @s12
  Scenario Outline: Cambiar el estado es binario, idempotente y sin ETag
    Given un webhook propio en estado <antes> y un reloj fijo en 2026-09-08T11:00:00.000000Z
    When envía PUT /api/v1/me/webhooks/{id}/status con <cuerpo> y sin If-Match
    Then recibe <resultado>
    And la respuesta no incluye ETag y es no-store
    Examples:
      | antes                                              | cuerpo                 | resultado |
      | active                                             | {"status":"disabled"}  | 200 con status disabled, disabledReason MANUAL y disabledAt 2026-09-08T11:00:00.000000Z |
      | disabled MANUAL desde 2026-09-01T00:00:00.000000Z  | {"status":"disabled"}  | 200 conservando disabledReason MANUAL y disabledAt 2026-09-01T00:00:00.000000Z |
      | disabled DELIVERY_EXHAUSTED                        | {"status":"active"}    | 200 con status active, disabledReason null y disabledAt null |
      | active                                             | {"status":"active"}    | 200 con status active y updatedAt sin cambios |
      | active                                             | {"status":"paused"}    | 400 WEBHOOK_INVALID con errors para status |
      | active                                             | {}                     | 400 WEBHOOK_INVALID con errors para status |

  @s13
  Scenario: Eliminar borra en cascada sus entregas, libera plaza y no admite repetición
    Given una cuenta con 5 webhooks, uno de ellos con 3 entregas registradas
    When envía DELETE /api/v1/me/webhooks/{id} de ese webhook dos veces
    Then la primera respuesta es 204 sin cuerpo y la segunda 404 WEBHOOK_NOT_FOUND
    And no queda ninguna entrega con ese endpoint y las entregas de otros webhooks siguen intactas
    And el id eliminado no aparece en GET /api/v1/me/webhooks y un POST válido posterior recibe 201
    And la outbox no pierde ni cambia ninguna fila

  @s14
  Scenario Outline: El ping se encola para el worker y nunca sale del hilo HTTP
    Given un webhook propio en estado <estado> con <pendientes> y un receptor de prueba
    When envía POST /api/v1/me/webhooks/{id}/ping sin cuerpo
    Then recibe <resultado>
    And el receptor no ha recibido ninguna petición al terminar la respuesta HTTP
    Examples:
      | estado   | pendientes                                  | resultado |
      | active   | ninguna entrega pendiente                   | 202 con delivery de status pending, attempt 0, eventType webhook.ping.v1, eventId igual a delivery.id y httpStatus, latencyMs, errorClass null |
      | active   | una entrega de outbox pendiente             | 202 con delivery pending |
      | active   | un ping pendiente                           | 409 WEBHOOK_DELIVERY_PENDING |
      | disabled | ninguna entrega pendiente                   | 409 WEBHOOK_DISABLED |

  @s15
  Scenario: La firma se calcula sobre t y el cuerpo exacto y es verificable con el secreto
    Given un webhook cuyo secreto es whsec_AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8
    And una entrega pendiente cuyo body son exactamente estos 209 bytes UTF-8:
      """
      {"eventId":"11111111-1111-4111-8111-111111111111","aggregateId":"22222222-2222-4222-8222-222222222222","ownerId":"owner-a","occurredAt":"2026-09-08T10:00:00.000000Z","schemaVersion":1,"type":"webhook.ping.v1"}
      """
    And el reloj del envío marca 2026-09-08T10:00:00Z
    When el worker ejecuta el intento
    Then el receptor recibe la cabecera X-OrganizationWeb-Signature exactamente igual a t=1788861600,v1=47db42f51507bea71512fc26bef335a304b9382b45b590a774cfc977d6cd708f
    And el cuerpo recibido es byte a byte el body almacenado
    And si el receptor falla y el segundo intento se envía en 2026-09-08T10:01:00Z, el cuerpo es idéntico y la cabecera es t=1788861660,v1=fc161fb2f63428f0680cae6871216284bb420af9917ee794c8159d0400abe460

  @s16
  Scenario: Cada petición saliente lleva las cabeceras acordadas y ninguna credencial
    Given un webhook activo y una entrega pendiente con eventId E
    When el worker ejecuta el intento
    Then el receptor recibe un POST a la url registrada con Content-Type application/json; charset=utf-8
    And User-Agent es exactamente OrganizationWeb-Webhooks/1 y X-OrganizationWeb-Event-Id es E
    And la petición no incluye Cookie, Authorization ni ninguna cabecera de sesión
    And el cuerpo de respuesta del receptor se descarta sin almacenarse

  @s17
  Scenario: El ping entrega un cuerpo sintético con el id de la entrega como eventId
    Given un webhook activo con id W del propietario owner-a y un ping encolado con delivery.id D en 2026-09-08T10:00:00.000000Z
    When el worker ejecuta el intento y el receptor responde 200
    Then el cuerpo recibido es un objeto con exactamente eventId D, aggregateId W, ownerId owner-a, occurredAt 2026-09-08T10:00:00.000000Z, schemaVersion 1 y type webhook.ping.v1
    And la entrega queda succeeded con attempt 1, httpStatus 200 y latencyMs medido con el cronómetro monótono inyectado
    And no se inserta ninguna fila en la outbox ni avanza el cursor del webhook

  @s18
  Scenario: Encolar toma sólo eventos propios suscritos y no toca la outbox
    Given un webhook activo suscrito a [TaskCreated.v1] con cursor en 2026-09-08T10:00:00.000000Z
    And la outbox contiene, posteriores al cursor y anteriores en más de 5 s al reloj, un ProjectCreated.v1 propio, un TaskCreated.v1 propio y un TaskCreated.v1 de otro propietario
    When el worker completa un ciclo de encolado
    Then existe exactamente una entrega pendiente, con eventId y eventType del TaskCreated.v1 propio
    And su body es byte a byte el payload JSON de esa fila de la outbox
    And el cursor queda en (occurredAt, eventId) de ese evento
    And ninguna fila de la outbox cambia status, attempts, published_at ni payload

  @s19
  Scenario Outline: El cursor respeta la creación, la ventana de gracia y el orden por tupla
    Given un webhook creado en 2026-09-08T10:00:00.000000Z suscrito a [ProjectCreated.v1]
    And un ProjectCreated.v1 propio con occurredAt <occurredAt>
    When el worker completa un ciclo con el reloj en <reloj>
    Then <resultado>
    Examples:
      | occurredAt                    | reloj                         | resultado |
      | 2026-09-08T09:59:59.999999Z   | 2026-09-08T10:00:10.000000Z   | no se encola ninguna entrega y el cursor no cambia |
      | 2026-09-08T10:00:00.000000Z   | 2026-09-08T10:00:10.000000Z   | se encola una entrega porque el eventId supera el UUID nulo |
      | 2026-09-08T10:00:06.000000Z   | 2026-09-08T10:00:10.000000Z   | no se encola aún y el cursor no cambia |
      | 2026-09-08T10:00:05.000000Z   | 2026-09-08T10:00:10.000000Z   | se encola una entrega |
      | dos eventos con el mismo occurredAt y eventIds B y A | 2026-09-08T10:01:00.000000Z | se encola primero el de eventId menor A |

  @s20
  Scenario: Un webhook mantiene una sola entrega de outbox en vuelo y conserva el orden
    Given un webhook activo suscrito a [TaskCreated.v1] y tres TaskCreated.v1 propios elegibles E1, E2, E3 en ese orden
    And un receptor que responde 200 a todo
    When el worker completa ciclos hasta que no quedan entregas pendientes
    Then el receptor recibe exactamente tres POST con X-OrganizationWeb-Event-Id E1, E2 y E3 en ese orden
    And en ningún momento existen dos entregas pendientes derivadas de la outbox para ese webhook
    And un ping encolado entre E1 y E2 se entrega sin alterar el orden relativo de E1, E2 y E3

  @s21
  Scenario Outline: Eventos bloqueados o inválidos no generan entrega ni detienen el cursor
    Given un webhook activo suscrito a [ProjectCreated.v1] y en la outbox, tras el cursor, <fila> seguida de un ProjectCreated.v1 válido V
    When el worker completa un ciclo de encolado
    Then <resultado>
    And la fila de la outbox conserva su status y no se modifica
    Examples:
      | fila                                                      | resultado |
      | un ProjectCreated.v1 propio con status blocked            | se encola sólo V y el cursor queda en V |
      | un ProjectCreated.v1 propio cuyo payload carece de name   | no se encola esa fila, se audita su eventId con INVALID_EVENT y el cursor avanza hasta esa fila |
      | un ProjectCreated.v1 con schemaVersion 2                  | no se encola esa fila, se audita UNSUPPORTED_EVENT y el cursor avanza hasta esa fila |

  @s22
  Scenario Outline: Un reinicio durante el envío repite el intento con el mismo evento
    Given una entrega pendiente reclamada por el worker
    And el proceso muere <momento>
    When el worker reiniciado completa el siguiente ciclo
    Then la entrega vuelve a enviarse con el mismo body y el mismo X-OrganizationWeb-Event-Id
    And el receptor cuenta <copias> peticiones con ese eventId y un único registro de entrega existe en la base de datos
    And el registro final es succeeded con attempt 1
    Examples:
      | momento                                        | copias |
      | antes de abrir la conexión                     | 1      |
      | después de que el receptor respondiera 200 y antes de confirmar la transacción | 2 |

  @s23
  Scenario: Dos instancias del worker no envían la misma entrega a la vez
    Given 10 entregas pendientes y elegibles y un receptor que tarda 500 ms en responder 200
    When dos instancias del worker ejecutan runCycle simultáneamente
    Then el receptor recibe exactamente 10 peticiones, una por eventId
    And ninguna instancia espera al bloqueo de fila de la otra y las 10 entregas quedan succeeded con attempt 1

  @s24
  Scenario Outline: Los reintentos siguen la tabla fija y el sexto fallo agota la entrega
    Given una entrega con attempt <previos> fallidos y un receptor que responde 500
    When el worker ejecuta el intento en el instante T
    Then la entrega queda con attempt <attempt>, errorClass HTTP_ERROR, httpStatus 500 y <resultado>
    Examples:
      | previos | attempt | resultado |
      | 0       | 1       | status pending y nextAttemptAt T + 1 min |
      | 1       | 2       | status pending y nextAttemptAt T + 5 min |
      | 2       | 3       | status pending y nextAttemptAt T + 30 min |
      | 3       | 4       | status pending y nextAttemptAt T + 2 h |
      | 4       | 5       | status pending y nextAttemptAt T + 24 h |
      | 5       | 6       | status exhausted y nextAttemptAt null |

  @s25
  Scenario Outline: Cada resultado no 2xx se clasifica sin seguir redirecciones ni guardar la respuesta
    Given una entrega pendiente hacia un receptor de prueba que <comportamiento>
    When el worker ejecuta el intento
    Then la entrega queda pending con attempt 1, errorClass <clase> y httpStatus <http>
    And latencyMs es un entero no negativo medido con el cronómetro monótono inyectado
    # Enmienda del 10 de septiembre de 2026, ratificada por el propietario. Las dos
    # líneas de latencyMs -ésta y la del @s17- decían «medido con el reloj inyectado».
    # Medir tiempo transcurrido con un reloj de pared es un defecto conocido: un ajuste
    # de hora da latencias negativas. El código usa un cronómetro monótono inyectado, y
    # el contrato pasa a decir lo que el código hace y lo que la ingeniería pide. La
    # cláusula sigue siendo comprobable y su rojo está acreditado. Ver
    # progress/ratificaciones.md, entrada R4.
    And el receptor de la redirección, si existe, no recibe ninguna petición
    Examples:
      | comportamiento                                           | clase           | http |
      | responde 404                                             | HTTP_ERROR      | 404  |
      | responde 500                                             | HTTP_ERROR      | 500  |
      | responde 302 con Location a otra URL                     | REDIRECT        | 302  |
      | acepta la conexión y no responde dentro del plazo de intercambio | TIMEOUT   | null |
      | tiene el puerto cerrado                                  | CONNECTION      | null |
      | presenta un certificado no confiable                     | TLS             | null |
      | tiene un host que ya no resuelve                         | DNS             | null |
      | tiene un host que ahora resuelve a 10.0.0.7              | BLOCKED_ADDRESS | null |
    # Enmienda del 10 de septiembre de 2026, ratificada por el propietario. El catalogo
    # de error_class pasa de siete clases a OCHO: se anade SECRET_UNREADABLE, que no es
    # un resultado de un envio -por eso no tiene fila en este Outline, donde se clasifica
    # lo que devuelve el receptor- sino el desenlace de una entrega que NO se pudo
    # enviar porque ninguna clave del llavero abre el secreto guardado, escenario que
    # project-spec.md declara ESPERADO tras una rotacion.
    #
    # Sin esa clase, el arreglo del 10 de septiembre -sacar el descifrado del RowMapper
    # para que una fila envenenada deje de detener la cola de TODOS los propietarios-
    # habria cambiado el silencio de sitio en vez de quitarlo: la entrega quedaria sin
    # explicacion. El propietario eligio ampliar el catalogo para que el operador vea
    # POR QUE fallo. Ver progress/ratificaciones.md, entrada R6.

  @s26
  Scenario Outline: Cualquier 2xx dentro del plazo cierra la entrega como succeeded
    Given una entrega pendiente y un receptor que responde <codigo> con un cuerpo de 1 KiB
    When el worker ejecuta el intento
    Then la entrega queda succeeded con attempt 1, httpStatus <codigo>, errorClass null y nextAttemptAt null
    And ninguna columna ni log contiene el cuerpo de respuesta
    Examples:
      | codigo |
      | 200    |
      | 204    |
      | 299    |

  @s27
  Scenario: Agotar una entrega desactiva el webhook en la misma transacción sin retroceder el cursor
    Given un webhook activo con cursor C, una entrega D1 con 5 fallos y otra entrega pendiente D2 y un receptor que responde 500
    When el worker ejecuta el sexto intento de D1 en 2026-09-08T12:00:00.000000Z
    Then D1 queda exhausted y el webhook queda disabled con disabledReason DELIVERY_EXHAUSTED y disabledAt 2026-09-08T12:00:00.000000Z
    And D2 sigue pending y no se envía en los ciclos siguientes mientras el webhook esté disabled
    And el cursor sigue siendo C y la lista muestra el webhook como desactivado por entregas agotadas
    And si la transacción de desactivación falla, D1 no queda exhausted ni el webhook disabled

  @s28
  Scenario: Reactivar reanuda desde el cursor conservado y no repite la entrega agotada
    Given un webhook disabled por DELIVERY_EXHAUSTED con D1 exhausted, D2 pending y dos eventos suscritos posteriores al cursor
    And el receptor ahora responde 200
    When envía PUT /api/v1/me/webhooks/{id}/status con {"status":"active"} y el worker completa ciclos
    Then D2 se entrega y después los dos eventos posteriores, en orden
    And D1 sigue exhausted y su eventId no se reenvía
    And el webhook queda active con disabledReason y disabledAt null

  @s29
  Scenario: El registro conserva las últimas cincuenta entregas sin cuerpos ni secretos
    Given un webhook con 55 entregas terminales con updatedAt distintos y 2 pendientes
    When el worker registra el siguiente resultado terminal y el propietario consulta GET /api/v1/me/webhooks/{id}/deliveries
    Then quedan persistidas exactamente 50 terminales, las de mayor updatedAt, y las 2 pendientes
    And recibe 200 con items de como máximo 50 elementos ordenados por updatedAt DESC y después id DESC
    And cada elemento tiene exactamente id, eventId, eventType, status, attempt, httpStatus, latencyMs, errorClass, nextAttemptAt, createdAt y updatedAt
    And ningún elemento contiene body, url, secret, firma ni cabeceras del receptor

  @s30
  Scenario Outline: Reenviar sólo aplica a entregas terminales de un webhook activo
    Given un webhook en estado <estado> con una entrega D en estado <entrega> con attempt 6 y body B
    When envía POST /api/v1/me/webhooks/{id}/deliveries/D/redeliver sin cuerpo con el reloj en 2026-09-08T13:00:00.000000Z
    Then recibe <resultado>
    Examples:
      | estado   | entrega                          | resultado |
      | active   | succeeded                        | 202 con delivery pending, attempt 0, nextAttemptAt 2026-09-08T13:00:00.000000Z y el mismo id y eventId |
      | active   | exhausted                        | 202 con delivery pending, attempt 0 y el mismo eventId |
      | active   | pending                          | 409 WEBHOOK_DELIVERY_PENDING |
      | disabled | exhausted                        | 409 WEBHOOK_DISABLED |
      | active   | succeeded pero de otro webhook   | 404 WEBHOOK_NOT_FOUND |

  @s31
  Scenario: Una entrega reenviada sale con el cuerpo original y una firma nueva
    Given una entrega reenviada cuyo body original es B y cuyo primer intento se firmó con t=1788861600
    When el worker ejecuta el intento en 2026-09-08T13:00:00Z y el receptor responde 200
    Then el receptor recibe byte a byte B con el mismo X-OrganizationWeb-Event-Id y una cabecera de firma con t=1788872400
    And la entrega queda succeeded con attempt 1 y sin fila adicional en el registro

  @s32
  Scenario Outline: El worker sólo existe con app.webhooks.enabled y acota cada ciclo
    Given <configuracion> y 25 entregas pendientes elegibles hacia un receptor que responde 200
    When transcurren 2500 ms desde el arranque
    # Enmienda del 10 de septiembre de 2026, ratificada por el propietario. Decia
    # 1500 ms, y la fila 3 exige DOS ciclos del worker. Con
    # @Scheduled(initialDelay=1000, fixedDelay=1000) a los 1500 ms solo ha corrido
    # UNO: la clausula era imposible de cumplir, no dificil. 2500 ms si cubre dos
    # tics. Se enmienda el plazo y no el cableado porque el cableado es correcto y
    # el que estaba mal era el numero del contrato. WebhookScheduleTest sujeta
    # initialDelay, fixedDelay y unidad por reflexion, asi que cualquier desvio
    # futuro se pone rojo al instante. Ver progress/ratificaciones.md, entrada R5.
    Then <resultado>
    Examples:
      | configuracion                                 | resultado |
      | app.webhooks.enabled ausente                  | ninguna entrega cambia y el receptor no recibe peticiones |
      | app.webhooks.enabled false                    | ninguna entrega cambia y el receptor no recibe peticiones |
      | app.webhooks.enabled true                     | exactamente 20 entregas quedan succeeded tras el primer ciclo y las 5 restantes tras el segundo |

  # Enmienda del 9 de septiembre de 2026, con la misma lectura ya ratificada por el propietario
  # para la feature 26 (github_connector.feature:398): una credencial Bearer válida SÍ está
  # autenticada, así que el canal de máquina recibe 403 API_SCOPE_DENIED —«sé quién eres y esto
  # no es para ti»— y no 401. Las dos filas Bearer se corrigen en consecuencia.
  @s33
  Scenario Outline: Seguridad de sesión precede a cualquier lectura o escritura de webhooks
    Given <condicion>
    When solicita <peticion>
    Then recibe <resultado> sin consultar ni escribir webhooks ni entregas
    And la respuesta es no-store
    Examples:
      | condicion                                            | peticion                                     | resultado |
      | ninguna sesión                                       | GET /api/v1/me/webhooks                      | 401 UNAUTHENTICATED |
      | ninguna sesión                                       | POST /api/v1/me/webhooks                     | 401 UNAUTHENTICATED |
      | una credencial Bearer válida de la feature 24        | GET /api/v1/me/webhooks                      | 403 API_SCOPE_DENIED |
      | una credencial Bearer válida de la feature 24        | POST /api/v1/me/webhooks/{id}/ping           | 403 API_SCOPE_DENIED |
      | sesión válida y token CSRF inválido                  | POST /api/v1/me/webhooks                     | 403 CSRF_INVALID |
      | sesión válida y token CSRF inválido                  | DELETE /api/v1/me/webhooks/{id}              | 403 CSRF_INVALID |
      | sesión válida y Origin no permitido                  | PUT /api/v1/me/webhooks/{id}/status          | 403 UNTRUSTED_ORIGIN |
      | sesión válida                                        | PATCH /api/v1/me/webhooks/{id}               | 405 |

  @s34
  Scenario Outline: Los errores se resuelven en el orden fijado
    Given una cuenta con 5 webhooks, la propiedad app.connectors.key <clave> y una URL <url>
    When envía POST /api/v1/me/webhooks con <cuerpo>
    Then recibe <resultado>
    Examples:
      | clave    | url                       | cuerpo                                          | resultado |
      | ausente  | "https://10.0.0.1/h"      | 4097 bytes                                      | 413 WEBHOOK_TOO_LARGE |
      | ausente  | "https://10.0.0.1/h"      | con propiedad desconocida                       | 400 MALFORMED_JSON |
      | ausente  | "http://10.0.0.1/h"       | válido en estructura                            | 400 WEBHOOK_INVALID |
      | ausente  | "https://10.0.0.1/h"      | válido                                          | 503 CONNECTORS_DISABLED |
      | válida   | "https://10.0.0.1/h"      | válido                                          | 400 WEBHOOK_URL_BLOCKED |
      | válida   | "https://example.com/h"   | válido                                          | 409 WEBHOOK_LIMIT |

  @s35
  Scenario: Auditoría y errores no revelan URL completa, firma, secreto ni cuerpos
    Given un webhook con url https://example.com/hooks?token=abc y entregas con éxito y con fallo
    When el worker completa ciclos y la API responde a un 400, un 404 y un 409
    Then los logs contienen eventId, endpointId y clase de error y no contienen ?token=abc, whsec_, v1= ni cuerpos de respuesta
    And los problem+json contienen código y mensaje en español sin URL, secreto, trazas ni datos de otra cuenta

  @s36
  Scenario Outline: Entrar en Webhooks muestra un estado claro sin escrituras
    Given una persona autenticada y el backend que responde a GET /api/v1/me/webhooks con <respuesta>
    When abre Webhooks desde la entrada de navegación situada tras API para integraciones
    Then la ruta es /webhooks, el h1 es Webhooks y Hoy conserva su posición en la navegación
    And <vista>
    And no se envía ningún POST, PUT ni DELETE
    Examples:
      | respuesta                    | vista |
      | una respuesta retenida       | un estado Cargando webhooks… anunciado por aria-live antes de 400 ms |
      | 200 con items []             | un estado vacío que explica qué es un webhook, la firma X-OrganizationWeb-Signature y el límite de cinco, y el formulario visible |
      | 503 STORAGE_UNAVAILABLE      | un mensaje de error con role alert y un botón Reintentar que repite sólo el GET |
      | 200 con dos webhooks         | una lista con URL, descripción, tipos y estado de cada uno |

  @s37
  Scenario: El formulario valida en cliente, evita duplicados y muestra el secreto una sola vez
    Given la vista con el formulario y una respuesta de creación retenida
    When completa URL con type="url", marca Seleccionar todos, activa Crear webhook dos veces y el backend responde 201 con secret whsec_x
    Then sólo se envía un POST y el botón Crear webhook permanece deshabilitado hasta la respuesta
    And el POST lleva los doce eventTypes y se muestra whsec_x en un campo de sólo lectura seleccionable con un botón Copiar
    And no se escribe whsec_x en localStorage, sessionStorage ni en la URL y no se copia sin activar Copiar
    And un texto visible avisa de que el secreto no volverá a mostrarse y la lista incorpora el nuevo webhook
    And el secreto sólo desaparece al activar el botón Cerrar del panel o al salir de la vista

  @s38
  Scenario Outline: El secreto y las peticiones en curso no sobreviven a la salida de la vista ni a otra identidad
    Given un secreto visible en memoria y una respuesta 201 tardía todavía posible
    When <salida>
    Then el secreto deja de estar en el DOM y en el estado de la vista
    And la petición pendiente se aborta y la respuesta tardía no muestra su secreto ni actualiza la lista de otra identidad
    And un 401 tardío de esa petición no retira una sesión posterior
    Examples:
      | salida |
      | navega a otra ruta |
      | cierra sesión |
      | cambia la identidad de acceso |

  @s39
  Scenario Outline: Las acciones de la lista reflejan el estado y confirman lo destructivo
    Given una lista con un webhook en estado <estado>
    When activa <accion>
    Then <resultado>
    And el estado se muestra como texto <texto> y con el mismo valor en un atributo ARIA
    Examples:
      | estado                                              | accion          | resultado | texto |
      | active                                              | Enviar ping     | se envía un POST ping y la fila de entregas muestra webhook.ping.v1 en estado Pendiente | Activo |
      | active                                              | Desactivar      | se envía PUT status disabled y el texto pasa a Desactivado manualmente | Activo |
      | disabled MANUAL                                     | Activar         | se envía PUT status active y el texto pasa a Activo | Desactivado manualmente |
      | disabled DELIVERY_EXHAUSTED en 2026-09-08           | Activar         | se envía PUT status active | Desactivado por entregas agotadas el 2026-09-08 |
      | active                                              | Eliminar        | aparece una confirmación explícita y no se envía DELETE hasta confirmar | Activo |
      | active                                              | Eliminar y confirmar | se envía un DELETE, la fila desaparece y el foco vuelve al encabezado de la lista | Activo |

  @s40
  Scenario: El panel de entregas se actualiza a mano y permite reenviar filas terminales
    Given un webhook con entregas succeeded, exhausted y pending
    When activa Ver entregas y después Reenviar en la fila succeeded
    Then la tabla muestra columnas tipo, intento, código HTTP, latencia, clase de error, estado y fecha con una fila por entrega
    And sólo las filas succeeded y exhausted tienen botón Reenviar y la fila pending no
    And se envía un POST redeliver y la fila pasa a Pendiente con intento 0
    And no se emite ningún GET periódico y el botón Actualizar repite un único GET deliveries

  @s41
  Scenario Outline: Los errores del servidor se explican sin ocultar el formulario ni reintentar solos
    Given la vista con el formulario completo
    When el POST de creación termina con <fallo>
    Then <recuperacion>
    And el formulario sigue visible con los valores introducidos y no se envía otro POST automáticamente
    Examples:
      | fallo                       | recuperacion |
      | 503 CONNECTORS_DISABLED     | un mensaje con role alert que explica que falta configuración del servidor |
      | 409 WEBHOOK_LIMIT           | un mensaje que indica el límite de cinco y sugiere eliminar uno |
      | 400 WEBHOOK_URL_BLOCKED     | un mensaje asociado al campo URL mediante aria-describedby |
      | error de red sin respuesta  | un mensaje explícito de resultado incierto y un botón Actualizar lista que repite sólo el GET |

  @s42
  Scenario: La vista es operable con teclado, a 320, 768, 1280 y 1440 px y sin violaciones axe
    Given los estados vacío, formulario, secreto visible, lista y panel de entregas de /webhooks
    When se recorre la vista con teclado en 320, 768, 1280 y 1440 px CSS, con texto al 200 % y zoom al 200 %, en ambos temas
    Then todos los controles se alcanzan con Tab en orden lógico, tienen nombre accesible, foco visible y objetivo de al menos 44 px
    And ningún ancho presenta scroll horizontal ni recorte de la URL, del secreto ni de la tabla de entregas
    And tras cerrar el panel del secreto o la confirmación de eliminación el foco vuelve al control que los abrió
    And axe no informa violaciones en ninguno de los estados y los cambios de estado se anuncian por aria-live

  @s43
  Scenario: La ayuda del formulario ofrece la guía pública de verificación de firma
    Given la vista de Webhooks con el formulario de creación visible
    When recorre el formulario con el teclado
    Then la ayuda del formulario contiene un enlace a la guía pública de verificación de firma
    And ese enlace se alcanza con Tab y cumple el foco visible y los 44 px que el @s42 exige a todo control
    And su nombre accesible dice que explica cómo verificar la firma, no «aquí» ni «más información»
    And el enlace apunta a un destino no vacío; el contrato no fija cuál
    # Ampliación del 10 de septiembre de 2026. PENDIENTE DE CONTRAFIRMA del
    # propietario: hasta que la haya, este escenario no acredita cierre.
    #
    # project-spec.md:2044 promete desde el principio que la ayuda del formulario
    # ENLAZA docs/webhooks.md, y el producto no enlazaba. Hoy se corrigió el
    # documento -declarando falsa esa línea- porque ningún @s pedía el enlace. El
    # propietario pide cerrarlo por el otro lado: que el formulario enlace de
    # verdad, y que sea el contrato quien lo pida.
    #
    # La cláusula dice ENLACE, no RUTA, y es deliberado. El juez de cierre
    # desaconsejó «fijar una ruta de documentación en la interfaz: frágil, y no
    # mata mutantes», y en eso tenía razón: un oráculo que clave la cadena
    # docs/webhooks.md caería el día que la guía se publique en otro sitio, sin
    # que ningún usuario hubiera perdido nada. Lo que el usuario necesita -y lo
    # único que aquí se promete- es llegar desde el formulario, con el teclado y
    # sabiendo a qué llega, a la guía de verificación de firma. Un enlace ausente,
    # mudo, sin destino o fuera del orden de tabulación incumple; mudar la guía de
    # sitio, no.
    # Ver progress/enlace_docs_webhooks.md y progress/decisiones_pendientes.md.
