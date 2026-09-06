@start_work_session @spec_ready
Feature: Iniciar trabajo propio con un fin previsto fijo y recuperar el hecho confirmado
  Fuente normativa: project-spec.md sección14 y progress/review_start_work_spec.md.
  B = /api/v1/projects/{projectId}/tasks/{taskId}/work-sessions.
  A = /api/v1/work-sessions/active; I = /api/v1/work-sessions/{id}.
  K = /api/v1/work-sessions/by-request/{requestKey}; active y by-request son rutas literales.
  SessionStart contiene exactamente id, projectId, taskId, startedAt, plannedMinutes, plannedEndAt, zoneId.
  IDs son UUID canónicos; instantes UTC de años0001–9999 con precisión máxima de microsegundos.
  plannedMinutes es entero1–1440; plannedEndAt = startedAt + plannedMinutes*60 segundos; zoneId es texto histórico no vacío.
  POST recibe exactamente plannedMinutes e Idempotency-Key UUID canónica, sin exigir If-Match ni Availability-Revision.
  La intención es proyecto, tarea y plannedMinutes; su espacio de keys por propietario es independiente de11/13.
  Se heredan seguridad/autenticación/origen/CSRF, negociación415, UUID y headers repetidos,
  problem+json/no-store y JSON ilegible de schedule_block.feature @s19/@s23/@s62 y reschedule.feature.
  No se modifica Block9, Today15 ni las transiciones existentes de proyectos/tareas.
  Tras filtros, POST ordena query, IDs, key, sintaxis, forma/campos extra, tipo/rango, contexto,
  replay, activa, proyecto completed, tarea completed, reloj/zona y escritura.
  GET ordena seguridad, query e ID/key; las cuatro rutas rechazan cualquier query, también repetida.
  Problemas comunes tienen sólo type,title,status,code; VALIDATION_ERROR conserva fieldErrors heredado.
  WORK_SESSION_ALREADY_ACTIVE añade únicamente sessionId de la activa propia a los cuatro campos comunes.
  ID/key ausente usa WORK_SESSION_NOT_FOUND y título "No se ha encontrado la sesión de trabajo.".
  No hay controles ni transiciones de pausa/cierre, avisos, ampliación, historial global o tiempo neto en14.
  Su habilitación habitual queda pendiente del ciclo14–16; no se fabrica una salida oculta ni se autoriza despliegue.
  Cada fila de Examples es un caso independiente para un futuro ciclo TDD; no son pruebas ejecutadas.

  Background:
    Given una persona autenticada con proyecto y tarea propios, tarea pending y sin sesión activa
    And las entradas no mencionadas son válidas y el reloj del servidor está controlado

  @s1
  Scenario: Confirmar un inicio explícito con duración elegida
    Given el reloj devuelve "2026-09-06T10:00:00.123456789Z" y la zona válida es "Europe/Madrid"
    When confirmo POST B con plannedMinutes 25 y una key nueva, sin headers de revisión
    Then recibo 201 después del commit con SessionStart y Location I del mismo id
    And startedAt es "2026-09-06T10:00:00.123456Z" y plannedEndAt es "2026-09-06T10:25:00.123456Z"
    And plannedMinutes es 25 y zoneId es "Europe/Madrid"
    And queda un inicio y un evento, sin modificar bloques, capacidad, estimaciones, Today, estados ni finalizaciones

  @s2
  Scenario Outline: Admitir los límites de duración relativa
    When confirmo un inicio con plannedMinutes <minutes>
    Then recibo 201 y la diferencia exacta entre fin e inicio es <seconds> segundos
    Examples:
      | minutes | seconds |
      | 1       | 60      |
      | 1440    | 86400   |

  @s3
  Scenario Outline: La duración real no cambia por medianoche o DST
    Given el reloj es "<start>" y la zona es "Europe/Madrid"
    When confirmo un inicio de <minutes> minutos
    Then el fin previsto es exactamente "<end>"
    And la presentación identifica fecha, hora y zona del fin, aunque cambie el día o el offset
    Examples:
      | start                | minutes | end                  |
      | 2026-09-06T21:50:00Z | 25      | 2026-09-06T22:15:00Z |
      | 2026-03-29T00:50:00Z | 25      | 2026-03-29T01:15:00Z |
      | 2026-10-25T00:50:00Z | 25      | 2026-10-25T01:15:00Z |

  @s4
  Scenario Outline: Usar UTC sin crear una preferencia
    Given la disponibilidad <condition>
    When confirmo un inicio de 25 minutos
    Then recibo 201 con zoneId "UTC" y no se crea ni cambia disponibilidad
    Examples:
      | condition                                      |
      | no existe                                      |
      | contiene una zona fuera del catálogo vigente   |
      | contiene una zona del catálogo no resoluble    |

  @s5
  Scenario Outline: No exigir planificación ni estado active del proyecto
    Given el proyecto está <state>, no hay bloques y el presupuesto es cero
    When confirmo un inicio de 25 minutos
    Then recibo 201 sin reservar capacidad ni cambiar el estado del proyecto
    Examples:
      | state  |
      | idea   |
      | active |
      | paused |

  @s6
  Scenario Outline: Rechazar duraciones que no son un entero admitido
    When envío POST B con plannedMinutes <value>
    Then recibo 400 VALIDATION_ERROR en plannedMinutes con <reason> y no hay escrituras
    Examples:
      | value    | reason        |
      | ausente  | REQUIRED      |
      | null     | REQUIRED      |
      | "25"     | INVALID_TYPE  |
      | true     | INVALID_TYPE  |
      | 1.5      | INVALID_TYPE  |
      | 0        | OUT_OF_RANGE  |
      | 1441     | OUT_OF_RANGE  |

  @s7
  Scenario Outline: Conservar el cuerpo JSON cerrado antes de resolver replay
    Given la key ya tiene un inicio confirmado
    When envío POST B con el cuerpo <body>
    Then recibo 400 <code> con <detail> y no otro éxito de replay
    Examples:
      | body                                    | code             | detail                       |
      | vacío                                   | MALFORMED_JSON   | problema común               |
      | {"plannedMinutes":                      | MALFORMED_JSON   | problema común               |
      | {"plannedMinutes":25,"plannedMinutes":25} | MALFORMED_JSON | problema común               |
      | {"plannedMinutes":25}{}                 | MALFORMED_JSON   | problema común               |
      | []                                      | VALIDATION_ERROR | body/INVALID_TYPE            |
      | {"plannedMinutes":25,"objective":"x","blockId":"x"} | VALIDATION_ERROR | blockId/UNKNOWN_FIELD |

  @s8
  Scenario Outline: Aplicar query cerrado a cada ruta nueva
    When solicito <route> con <query>
    Then recibo 400 VALIDATION_ERROR con query/INVALID_VALUE sin consultar ni escribir sesiones
    Examples:
      | route  | query     |
      | POST B | ?x=1      |
      | GET A  | ?x=1&x=2  |
      | GET I  | ?x=1      |
      | GET K  | ?x=1      |

  @s9
  Scenario Outline: Conservar la precedencia HTTP propia
    Given <faults>
    When solicito <route>
    Then recibo <result> sin información de otra sesión
    Examples:
      | faults                                         | route  | result                                      |
      | sesión anónima, query e ID inválidos             | GET I  | 401                                         |
      | query e ID inválidos                            | GET I  | 400 VALIDATION_ERROR query/INVALID_VALUE    |
      | query y key inválidas                           | GET K  | 400 VALIDATION_ERROR query/INVALID_VALUE    |
      | query e IDs inválidos y key ausente              | POST B | 400 VALIDATION_ERROR query/INVALID_VALUE    |
      | IDs válidos, key ausente y cuerpo truncado       | POST B | 400 VALIDATION_ERROR Idempotency-Key/REQUIRED |

  @s10
  Scenario Outline: Verificar contexto antes de entregar un replay
    Given una key confirmada y el contexto solicitado <context>
    When envío POST B con esa key
    Then recibo 404 RESOURCE_NOT_FOUND sin divulgar el inicio ni escribir
    Examples:
      | context                         |
      | pertenece a otra persona        |
      | no existe                       |
      | contiene tarea de otro proyecto |

  @s11
  Scenario Outline: Rechazar un inicio nuevo en contexto completado
    Given <context> y no hay replay ni activa
    When confirmo un inicio nuevo
    Then recibo 409 <code> con explicación de inicio de trabajo y no hay escrituras
    Examples:
      | context                            | code              |
      | proyecto completed y tarea pending | PROJECT_COMPLETED |
      | proyecto active y tarea completed  | TASK_COMPLETED    |
      | proyecto y tarea completed         | PROJECT_COMPLETED |

  @s12
  Scenario: La activa prevalece sobre la elegibilidad de una intención nueva
    Given existe una activa propia S y el proyecto solicitado está completed
    When confirmo una key nueva
    Then recibo 409 WORK_SESSION_ALREADY_ACTIVE con sólo los cuatro campos comunes y sessionId S
    And el título explica que ya existe una sesión de trabajo activa y no se confirma la nueva intención

  @s13
  Scenario Outline: Rechazar un reloj o fin que no puede representarse
    Given el reloj y la duración <condition>
    When confirmo el inicio nuevo
    Then recibo 409 WORK_SESSION_TIME_OUT_OF_RANGE sin escribir ni recortar los instantes
    And el título es "No se puede representar el inicio y el fin previsto de la sesión."
    Examples:
      | condition                                                 |
      | sitúan el inicio antes del año0001                         |
      | sitúan el inicio después del año9999                       |
      | sitúan el inicio en9999 y la suma del fin fuera de ese año  |

  @s14
  Scenario: El replay conserva el hecho anterior a los cambios de contexto
    Given una key confirmó 25 minutos, luego el contexto pasó a completed y cambiaron reloj, preferencia y catálogo
    When repito POST B con la misma intención y key
    Then recibo 200 con exactamente el SessionStart y Location originales
    And no se consulta el negocio actual para sustituir el inicio, fin o zona ni se añade otro evento

  @s15
  Scenario Outline: No reutilizar una key para otra intención propia
    Given una key confirmó un inicio en un contexto propio aún existente
    When repito esa key cambiando <part>
    Then recibo 409 IDEMPOTENCY_CONFLICT antes del conflicto de activa y no se muestra otro inicio como éxito
    Examples:
      | part                          |
      | plannedMinutes                |
      | tarea dentro del proyecto     |
      | proyecto y tarea propios      |

  @s16
  Scenario: Los espacios de idempotencia de planificación e inicio son independientes
    Given la misma UUID fue usada para crear y cambiar un bloque propio
    When confirmo un inicio nuevo con esa UUID como key
    Then recibo 201 y los recibos de11 y13 permanecen intactos

  @s17
  Scenario Outline: Resolver inicios concurrentes del mismo propietario sin disponibilidad
    Given no existe disponibilidad y dos peticiones válidas alcanzan el intento de escritura concurrentemente
    When compiten con <intentions>
    Then las respuestas son <results> tras resolver el ganador durable
    And queda exactamente un inicio y un evento, sin crear disponibilidad
    Examples:
      | intentions                                   | results                            |
      | misma key y misma intención                   | 201 y200 con el mismo DTO/Location  |
      | keys distintas entre proyectos propios        | 201 y409 WORK_SESSION_ALREADY_ACTIVE |
      | misma key y distinta duración                 | 201 y409 IDEMPOTENCY_CONFLICT       |

  @s18
  Scenario: Propietarios diferentes pueden iniciar simultáneamente
    Given dos propietarios con tareas propias y sin disponibilidad
    When ambos confirman un inicio concurrente
    Then ambos reciben 201 y cada GET A devuelve sólo la sesión de su propietario

  @s19
  Scenario Outline: Ordenar el inicio frente a completar el contexto
    Given compiten iniciar trabajo y completar <entity>
    When confirma primero <winner>
    Then <result>
    Examples:
      | entity   | winner       | result                                                        |
      | proyecto | completed    | el inicio nuevo recibe409 PROJECT_COMPLETED y no se registra   |
      | tarea    | completed    | el inicio nuevo recibe409 TASK_COMPLETED y no se registra      |
      | proyecto | el inicio    | el inicio queda recuperable después de completed sin cerrarse |
      | tarea    | el inicio    | el inicio queda recuperable después de completed sin cerrarse |

  @s20
  Scenario Outline: No convertir un fallo de persistencia en un éxito o conflicto inventado
    Given <failure>
    When confirmo un inicio nuevo
    Then recibo 503 STORAGE_UNAVAILABLE sin inicio, intención ni evento parciales
    Examples:
      | failure                                                   |
      | falla la consulta de disponibilidad                       |
      | se suprime INSERT de sesión sin ganador durable           |
      | se suprime INSERT de outbox                               |
      | falla la escritura de outbox                              |
      | falla la finalización de la transacción antes del commit  |

  @s21
  Scenario Outline: Recuperar el inicio durable por identidad propia
    Given un inicio confirmado y el contexto ahora completed
    When consulto <route>
    Then recibo 200 con el SessionStart original sin Location de una nueva operación ni escrituras
    Examples:
      | route |
      | GET I |
      | GET K |

  @s22
  Scenario Outline: No distinguir una identidad ajena de una ausente
    Given la identidad solicitada <condition>
    When consulto <route>
    Then recibo 404 WORK_SESSION_NOT_FOUND con el título común y sin datos de propietario
    Examples:
      | condition               | route |
      | pertenece a otra persona | GET I |
      | no existe               | GET I |
      | pertenece a otra persona | GET K |
      | no existe               | GET K |

  @s23
  Scenario Outline: Distinguir activa comprobada y ausencia comprobada
    Given <state>
    When consulto GET A
    Then recibo 200 con exactamente <body>, sin204 ni escrituras
    Examples:
      | state                                                | body                              |
      | no hay activa propia pero sí una ajena                | {session:null}                    |
      | hay activa propia de otra tarea que la pantalla actual | {session:SessionStart de esa tarea} |

  @s24
  Scenario Outline: Las lecturas fallidas no inventan ausencia
    Given falla <stage> de una lectura
    When consulto <route>
    Then recibo 503 STORAGE_UNAVAILABLE sin404 ni session:null ni cambios persistidos
    Examples:
      | stage                      | route |
      | la consulta                | GET A |
      | la consulta                | GET I |
      | la consulta                | GET K |
      | el final de la transacción | GET A |
      | el final de la transacción | GET I |
      | el final de la transacción | GET K |

  @s25
  Scenario: Recuperar tras commit sin respuesta y reinicio real
    Given el inicio hizo commit pero se perdió la respuesta y después se reinició la API conservando PostgreSQL
    And el evento publicado fue retirado del outbox
    When consulto GET K con la key original
    Then recupero el SessionStart original y GET A identifica ese mismo inicio
    And no se necesita el outbox ni se crea una segunda sesión

  @s26
  Scenario: Publicar el hecho de inicio con identidad y duración coherentes
    Given un inicio confirmado con instante truncado a microsegundos
    When el publicador entrega WorkSessionStarted.v1
    Then usa routing key work-session.started.v1 y cola quorum durable organization.work-session-started.v1
    And el evento tiene exactamente eventId,aggregateId,ownerId,occurredAt,schemaVersion,type,projectId,taskId,plannedMinutes,plannedEndAt,zoneId
    And aggregateId es la sesión, eventId tiene identidad independiente, occurredAt es startedAt y schemaVersion es1
    And type es WorkSessionStarted.v1 y la duración/fin coinciden con el recibo sin key ni nombres
    And permanecen las ocho rutas anteriores

  @s27
  Scenario Outline: Conservar la validación y recuperación del publicador
    Given <condition>
    When se intenta publicar el evento
    Then <result>
    Examples:
      | condition                                              | result                                                          |
      | broker caído tras el commit local                      | el inicio sigue confirmado y la entrega se reintenta             |
      | confirm del broker perdido                            | puede repetirse la entrega con la misma identidad                |
      | evento con campo extra                                | queda blocked sin publicar el payload incompatible              |
      | evento con fin incompatible con duración               | queda blocked sin publicar el payload incompatible              |
      | evento válido con zona histórica retirada del catálogo | se publica sin volver a resolver el catálogo                     |

  @s28
  Scenario: Pedir duración explícita antes de empezar
    Given abro la tarea propia
    When se presenta el formulario de inicio
    Then aparecen contexto, Duración prevista (minutos) vacía y Empezar a trabajar
    And no se envía POST por montaje, login o llegada de la hora de un bloque
    And no aparecen pausa, cierre, contador neto ni acciones de features15–18

  @s29
  Scenario Outline: Presentar actividad actual sin suponer que pertenece a la tarea abierta
    Given entro o vuelvo a la sección Sesión de trabajo y GET A <response>
    When se procesa la consulta
    Then se muestra <view>
    And hay actualización manual y no polling por segundo
    Examples:
      | response                                     | view                                                     |
      | sigue pendiente                              | carga anunciada diferenciada de ausencia                  |
      | confirma session:null                        | ausencia comprobada y formulario disponible              |
      | falla503                                     | error con reintento sin afirmar ausencia                  |
      | devuelve activa propia de otra tarea          | inicio,duración,fin,zona y enlace a esa tarea sin nuevo inicio |

  @s30
  Scenario Outline: No confirmar un DTO incompatible con la intención
    Given una intención de 25 minutos está pendiente
    When POST o comprobación devuelve <fault>
    Then no se anuncia Sesión iniciada y se conserva la misma intención incierta
    Examples:
      | fault                                                       |
      | campo adicional elapsedSeconds                              |
      | UUID o instante fuera del formato/rango de SessionStart      |
      | plannedMinutes string o fuera de1–1440                       |
      | plannedEndAt distinto de startedAt más1500segundos            |
      | plannedEndAt desviado exactamente1microsegundo del fin esperado |
      | proyecto o tarea distintos del contexto de la intención      |
      | zoneId vacío                                                |
      | POST201/200 con Location incompatible con el id              |

  @s31
  Scenario: Mostrar un recibo histórico aunque la zona ya no sea soportada
    Given un SessionStart válido de fecha pasada cuya zona histórica Intl no soporta
    When presento el inicio recuperado
    Then muestro inicio y fin en UTC etiquetado, conservando visible la zona original
    And no rechazo el recibo por reloj actual ni uso silenciosamente la zona del navegador

  @s32
  Scenario Outline: Mantener la intención durante envío e incertidumbre
    Given envié 25 minutos con key K
    When el envío <result>
    Then la key K y los25minutos permanecen retenidos sin editar duración ni enviar automáticamente
    And <feedback>
    Examples:
      | result                           | feedback                                                 |
      | sigue pendiente                  | hay feedback anunciado y no se duplica POST al activar otra vez |
      | falla por red                    | se ofrece Comprobar inicio                               |
      | devuelve503                      | se ofrece Comprobar inicio                               |
      | devuelve un código desconocido   | se ofrece Comprobar inicio                               |
      | devuelve IDEMPOTENCY_CONFLICT     | se ofrece Comprobar inicio sin confirmar otra intención   |

  @s33
  Scenario Outline: Resolver una comprobación sin inventar rollback
    Given la intención K de25minutos es incierta
    When Comprobar inicio por GET K <result>
    Then <view>
    And no se genera una nueva key ni un reenvío automático
    Examples:
      | result                        | view                                                        |
      | confirma el recibo compatible | se anuncia Sesión iniciada con el fin original              |
      | responde404                  | se permite comprobar otra vez o reenviar manualmente K/25   |
      | responde503                  | continúa incertidumbre y se permite comprobar otra vez       |

  @s34
  Scenario: Reenviar manualmente después de renovar CSRF
    Given el envío K de25minutos recibió CSRF reconocido y SessionGate renovó acceso por acción manual
    When activo el reenvío de la intención
    Then se envía K con los mismos25minutos y el token vigente
    And renovar acceso por sí solo no envió el POST

  @s35
  Scenario Outline: Distinguir rechazos definitivos y conflicto de activa
    Given una intención nueva recibió <problem>
    When se presenta el rechazo reconocido
    Then <recovery>
    Examples:
      | problem                     | recovery                                                     |
      | VALIDATION_ERROR            | se permite corregir la entrada antes de una nueva intención  |
      | PROJECT_COMPLETED           | se explica la inelegibilidad sin afirmar inicio confirmado   |
      | TASK_COMPLETED              | se explica la inelegibilidad sin afirmar inicio confirmado   |
      | WORK_SESSION_TIME_OUT_OF_RANGE | se explica el límite temporal sin culpar al campo editable |
      | WORK_SESSION_ALREADY_ACTIVE | se ofrece consultar la activa propia sin confirmar esta intención |

  @s36
  Scenario: Salir de un formulario no revoca una intención transmitida
    Given un POST fue transmitido y el formulario explica antes de salir que cerrarlo no lo revoca
    When cierro el formulario y vuelvo después de una recarga sin key en memoria
    Then GET A permite descubrir el inicio si fue confirmado
    And no se promete recuperar una petición que nunca llegó ni se crea otro inicio por volver

  @s37
  Scenario: Una consulta posterior fallida no revoca el inicio confirmado
    Given Sesión iniciada mostró un recibo compatible y su fin original
    When falla una actualización posterior de la activa
    Then el recibo sigue visible como hecho confirmado con error de consulta separado
    And el fin no se desplaza por la latencia ni se amplía o cierra al alcanzarlo

  @s38
  Scenario Outline: Descartar respuestas retiradas antes de publicar datos o clasificar errores
    Given una petición <phase> quedó pendiente y después cambió <context>
    When llega <response>
    Then no reaparecen datos, borradores ni anuncios del contexto retirado y no se invalida la sesión vigente por401 obsoleto
    Examples:
      | phase                      | context          | response                          |
      | lectura de activa          | la sesión        | éxito con inicio privado          |
      | comprobación de intención  | la ruta          | éxito después de leer JSON        |
      | envío                      | la generación    | error503                          |
      | fetch antes de entregar Response | la sesión  | 401                               |
    # apiRequest observa401 al entregar Response; no se inventa una ventana tras headers para ese error.

  @s39
  Scenario: Retirar datos privados al perder acceso actual
    Given hay una sesión de trabajo visible y una petición del contexto vigente
    When llega401 actual o comienza logout
    Then se retiran inmediatamente los datos privados y respuestas anteriores no los restauran
    And recuperar login no inicia trabajo automáticamente

  @s40
  Scenario Outline: Mantener foco y feedback durante las recuperaciones
    Given activé <action> con teclado y la petición sigue pendiente
    When se actualiza la vista de espera o error
    Then hay feedback anunciado, borrador conservado y controles alcanzables con Tab y Shift+Tab
    And si desapareció el iniciador y el foco quedó sin destino se enfoca el encabezado de sesión
    And no se roba el foco cuando elegí otro control conectado
    Examples:
      | action            |
      | Empezar a trabajar |
      | Comprobar inicio  |
      | Actualizar activa |

  @s41
  Scenario: Revisar la interfaz con la matriz de30principios y evidencia delimitada
    Given estados de formulario, espera, error, ausencia, activa de otra tarea y recibo histórico con textos largos
    When se evalúa la interfaz con docs/ux-requirements.md
    Then se registran30criterios con observación, evidencia y límites o hallazgos pendientes
    And se comprueban responsive, teclado/foco, ampliación y motores sin atribuir dispositivos físicos no probados
    And se distingue duración prevista de trabajo acreditado y se usa lenguaje neutral sin culpa ni métricas ficticias

  @s42
  Scenario: Una respuesta activa incompatible no habilita otro inicio
    Given GET A responde200 con session de forma incompatible con SessionStart o un envoltorio con campos adicionales
    When el cliente valida la respuesta
    Then muestra error de consulta sin afirmar ausencia ni habilitar un inicio por esa respuesta
    And un SessionStart válido de otra tarea propia sí es aceptable sin exigir igualdad con la ruta abierta
