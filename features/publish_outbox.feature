@publish_outbox @approval_pending
Feature: Entregar los eventos de proyecto confirmados a RabbitMQ sin perder su identidad
  Como propietario quiero conservar y publicar los hechos de mis proyectos
  para que las futuras integraciones puedan recibirlos sin depender de su disponibilidad al guardar.
  Este contrato es una propuesta pendiente de aprobación humana.

  @s1
  Scenario: Publicar el evento original con metadatos persistentes
    Given existe un evento ProjectCreated.v1 pendiente y confirmado en PostgreSQL con attempts igual a 0
    And la topología acordada está disponible
    When se completa su publicación con confirmación positiva y sin devolución
    Then la cola organization.project-created.v1 contiene el mismo objeto JSON que la outbox
    And conserva eventId, aggregateId, ownerId, occurredAt, schemaVersion, name y type
    And el message-id es el eventId y el content-type es application/json
    And el mensaje es persistente y no incluye descripción ni credenciales
    And la outbox registra published y published_at del reloj servidor
    And attempts es 1
    And el registro original permanece almacenado

  @s2
  Scenario: No publicar una creación cuya transacción aún no se confirmó
    Given una creación tiene proyecto y evento escritos en una transacción sin confirmar
    When el publicador ejecuta un ciclo
    Then no envía ese evento a RabbitMQ
    And no modifica la transacción de creación

  @s3
  Scenario: No producir mensajes si no existen eventos pendientes
    Given la outbox está vacía
    When el publicador ejecuta un ciclo
    Then no se envía ningún mensaje
    And no se crea ningún registro de evento

  @s4
  Scenario: Mantener pendiente el evento mientras espera confirmación
    Given un envío de un evento pendiente todavía no ha recibido confirmación
    When se consulta su estado persistido antes de agotarse el plazo
    Then sigue sin existir una confirmación published de ese evento
    And published_at sigue siendo null

  @s5
  Scenario Outline: Conservar el evento ante un fallo de publicación
    Given existe un evento elegible con attempts igual a 0
    And su publicación encuentra <fallo>
    When termina el intento y PostgreSQL confirma su resultado
    Then el evento conserva estado pending y todos los valores de su payload
    And attempts es 1 y published_at es null
    And last_error_code es <codigo>
    And next_attempt_at es un segundo posterior al instante del fallo registrado
    And no se reenvía ese evento dentro del mismo ciclo

    Examples:
      | fallo                                      | codigo             |
      | broker no disponible                       | BROKER_UNAVAILABLE |
      | confirmación negativa                      | BROKER_NACK        |
      | devolución por falta de ruta               | UNROUTABLE         |
      | ausencia de confirmación durante 5 segundos | CONFIRM_TIMEOUT    |

  @s6
  Scenario: Una devolución invalida también una confirmación positiva
    Given el broker devuelve un mensaje mandatory por falta de ruta
    And a continuación confirma positivamente ese envío
    When el publicador registra el resultado del intento
    Then el evento permanece pending con código UNROUTABLE
    And published_at sigue siendo null

  @s7
  Scenario Outline: Aplicar un intervalo de reintento creciente y acotado
    Given un evento acumula <fallos_previos> fallos terminados y registrados
    When termina otro intento fallido a las 12:00:00 UTC y se confirma su resultado
    Then attempts es <fallos_totales>
    And next_attempt_at es <proximo> UTC
    And el evento permanece pending sin descartar su payload

    Examples:
      | fallos_previos | fallos_totales | proximo  |
      | 0             | 1              | 12:00:01 |
      | 1             | 2              | 12:00:02 |
      | 5             | 6              | 12:00:32 |
      | 6             | 7              | 12:01:00 |
      | 100           | 101            | 12:01:00 |

  @s8
  Scenario Outline: Respetar la fecha de elegibilidad del reintento
    Given un evento pending tiene next_attempt_at a las 12:00:10 UTC
    And el broker acepta los envíos sin devolución
    When se ejecuta un ciclo a las <ahora> UTC
    Then se han iniciado <envios> envíos de ese evento

    Examples:
      | ahora    | envios |
      | 12:00:09 | 0      |
      | 12:00:10 | 1      |

  @s9
  Scenario: Reanudar pendientes tras recuperar el broker
    Given un evento pendiente tuvo fallos durante una interrupción del broker
    And ha llegado su próxima fecha de reintento
    And el broker está recuperado y acepta mensajes sin devolución
    When el publicador completa el siguiente intento y confirma su estado en PostgreSQL
    Then el evento queda published
    And el mensaje conserva el eventId y occurredAt de la primera creación
    And no se crea otro registro de outbox para ese hecho

  @s10
  Scenario: No volver a seleccionar un evento ya publicado
    Given un evento tiene estado published confirmado en PostgreSQL
    When el publicador ejecuta otro ciclo
    Then no vuelve a enviar ese evento
    And el evento y su published_at permanecen almacenados sin cambios

  @s11
  Scenario Outline: Recuperarse de una caída conservando la identidad del evento
    Given el publicador cayó <momento> sin confirmar published en PostgreSQL
    And el evento vuelve a estar disponible tras liberar la reclamación de la conexión caída
    And el broker está disponible
    And la caída se inyectó exactamente en ese punto en el primer intento de ese evento
    And no hubo otros intentos, consumidores ni pérdida de datos del broker
    When el publicador reiniciado completa un intento confirmado para ese evento
    Then la outbox contiene un único registro original en estado published
    And la cola contiene <copias> mensajes de ese hecho
    And todas sus copias conservan el mismo eventId y los mismos valores JSON

    Examples:
      | momento                                             | copias |
      | antes de enviar el mensaje al broker           | 1      |
      | después de que el broker aceptara el mensaje         | 2      |

  @s12
  Scenario: Conservar pendiente cuando falla el registro posterior a la aceptación
    Given el broker aceptó un mensaje con confirmación positiva y sin devolución
    And la transacción que intenta registrar published falla y se revierte
    When se consulta la outbox tras recuperar PostgreSQL
    Then el evento sigue pending con su payload original y published_at null
    And el mensaje aceptado sigue disponible en RabbitMQ
    And el evento es elegible para ser reclamado de nuevo conservando el mismo eventId

  @s13
  Scenario: Dos réplicas no publican simultáneamente la misma reclamación
    Given hay dos eventos pendientes elegibles
    And una réplica ha reclamado el primero y espera su confirmación
    When otra réplica ejecuta un ciclo antes de terminar esa reclamación
    Then la segunda réplica no envía el evento reclamado
    And publica el otro evento con confirmación positiva
    And no altera la reclamación de la primera réplica

  @s14
  Scenario: Conservar mensajes aceptados al reiniciar el broker
    Given un evento publicado tiene un mensaje persistente confirmado en la cola durable quorum
    And no hay consumidores ni políticas de expiración o descarte
    When RabbitMQ se reinicia conservando su volumen de datos
    Then el mensaje y la topología acordada siguen disponibles
    And el payload y el message-id conservan sus valores

  @s15
  Scenario Outline: Aislar un registro no publicable sin frenar los válidos
    Given un registro pending contiene <defecto>
    And después hay otro evento válido elegible
    When el publicador completa un ciclo
    Then el registro defectuoso se conserva blocked con <codigo>
    And no se envía su payload ni se incrementan sus intentos de envío
    And publica el evento válido con confirmación positiva
    And un ciclo posterior no vuelve a seleccionar el registro blocked

    Examples:
      | defecto                                        | codigo            |
      | tipo o versión no admitidos                    | UNSUPPORTED_EVENT |
      | payload incompatible con sus columnas de evento | INVALID_EVENT     |
      | propiedad description adicional en el payload  | INVALID_EVENT     |

  @s16
  Scenario: Guardar proyectos aunque RabbitMQ esté caído
    Given el publicador está habilitado y RabbitMQ no está disponible
    And PostgreSQL está disponible
    When se crea un proyecto válido mediante la API autenticada
    Then la API responde HTTP 201 tras confirmar proyecto y evento en PostgreSQL
    And el evento queda conservado para publicación posterior
    And la API no espera el plazo de confirmación de RabbitMQ para responder

  @s17
  Scenario: Respetar el máximo de trabajo de un ciclo
    Given hay 21 eventos válidos distintos pendientes y elegibles
    And el máximo configurado de este corte es 20 eventos distintos por ciclo
    And el broker confirma cada mensaje sin devolverlo
    When el publicador completa un ciclo
    Then publica 20 eventos distintos y deja un evento pendiente
    And selecciona primero los disponibles por occurred_at y después event_id

  @s18
  Scenario: No enviar si PostgreSQL impide reclamar trabajo
    Given PostgreSQL está indisponible antes de reclamar eventos
    When el publicador ejecuta un ciclo
    Then no envía mensajes a RabbitMQ
    And registra worker_error con código STORAGE_UNAVAILABLE
    And no inicia otro ciclo antes de transcurrir un segundo

  @s19
  Scenario: Mantener la outbox intacta con el publicador deshabilitado
    Given el publicador está deshabilitado explícitamente
    And existen eventos pendientes
    When arranca la aplicación
    Then no abre conexiones de publicación a RabbitMQ
    And no modifica la outbox

  @s20
  Scenario: Declarar de nuevo la topología compatible sin pérdida
    Given existe el exchange durable direct organization.events
    And existe la cola durable quorum organization.project-created.v1 con su binding project.created.v1
    And la cola contiene mensajes previos
    When el publicador conecta de nuevo y prepara su destino
    Then conserva la topología y los mensajes previos
    And puede publicar eventos posteriores

  @s21
  Scenario: No sustituir una topología incompatible automáticamente
    Given organization.project-created.v1 ya existe como una cola clásica incompatible
    And hay eventos pendientes
    When el publicador intenta preparar la topología acordada
    Then registra worker_error con código TOPOLOGY_MISMATCH
    And no elimina ni sustituye la cola existente
    And no publica ni marca publicados los eventos pendientes

  @s22
  Scenario: No iniciar el publicador habilitado con credenciales incompletas
    Given el publicador está habilitado pero falta el secreto de conexión
    When la aplicación evalúa su configuración de publicación
    Then el publicador no inicia conexiones con credenciales predeterminadas
    And registra worker_error con código CONFIGURATION_ERROR
    And no modifica los eventos pendientes

  @s23
  Scenario Outline: Registrar resultados sin datos personales ni secretos
    Given un intento de publicación termina con <resultado>
    And el proyecto y la configuración contienen nombre, descripción, propietario y contraseña identificables
    When se registra el resultado del intento
    Then el registro incluye eventId, outcome <outcome> y attempt
    And incluye un código de error cuando el outcome no es published
    And no incluye payload, nombre, descripción, ownerId, contraseña ni URL con credenciales

    Examples:
      | resultado                | outcome   |
      | aceptación confirmada    | published |
      | fallo transitorio        | retry     |
      | evento inválido          | blocked   |
