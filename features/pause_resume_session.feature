@pause_resume_session @spec_ready
Feature: Pausar y reanudar la misma sesión sin perder tiempo exacto ni el hecho confirmado
  Fuente normativa: project-spec.md sección 15 y progress/review_pause_resume_proposal.md APPROVED.
  S = /api/v1/work-sessions/{id}/state; P y R terminan en /{id}/pause y /{id}/resume.
  C = /api/v1/work-session-changes/{changeId}; K = /api/v1/work-session-changes/by-request/{key}.
  SessionStart conserva exactamente los siete campos de start_work_session.feature.
  WorkSessionState contiene sólo session, status, revision, changedAt, workedMicroseconds, runningSince.
  revision es texto decimal canónico positivo hasta 9223372036854775807; workedMicroseconds es texto decimal canónico no negativo.
  changedAt >= startedAt; runningSince = changedAt cuando running y null cuando paused.
  workedMicroseconds <= microsegundos exactos entre startedAt y changedAt; instantes UTC de años 0001–9999 y precisión máxima µs.
  WorkSessionChange contiene sólo id, sessionId, action, occurredAt, before, after; action es PAUSE o RESUME.
  Ambas proyecciones conservan identidad y SessionStart; after.revision = before.revision + 1 y after.changedAt = occurredAt.
  GET S contiene sólo state, serverNow, netMicroseconds; cabecera Work-Session-Revision: work-session-{id}-{revision}, sin comillas ni ETag.
  netMicroseconds = workedMicroseconds + max(0, serverNow-runningSince) en µs si running; si paused es workedMicroseconds.
  POST P/R recibe sólo {}, Idempotency-Key UUID canónica y Work-Session-Revision; intención = sessionId, acción y revisión esperada.
  Se heredan seguridad, origen/CSRF, negociación, UUID/headers repetidos, JSON y problemas cerrados/no-store de feature 14.
  Las cinco rutas nuevas rechazan toda query. POST ordena query, UUID, key, sintaxis Work-Session-Revision, sintaxis JSON, objeto vacío,
  propiedad, identidad del token de revisión, replay, revisión actual, estado permitido, reloj y escrituras; GET ordena seguridad, query y UUID.
  Problemas tienen los cuatro campos comunes; VALIDATION_ERROR conserva fieldErrors heredado.
  Los títulos exactos y las reglas comunes de formato son los normados en sección 15, sin cambiar títulos de 14.
  Comandos READ_COMMITTED bloquean sólo la sesión propia; estado se consulta read-only REPEATABLE_READ.
  La unicidad de key es por propietario en el espacio de cambios de sesión, independiente de 13/14.
  Una colisión esperada se compara con el recibo ganador después de terminar la transacción fallida.
  Recibos se consultan read-only sin reloj; ningún GET materializa estado, bloquea para escribir ni emite eventos.
  No hay cierre, avance, crédito diario, ampliación, aviso, historial global ni cambio automático por reloj.
  Cada fila de Examples es un caso independiente para TDD posterior, no una prueba ejecutada.

  Background:
    Given una persona autenticada y una sesión propia iniciada con el contrato de 14
    And toda entrada no mencionada es válida y el reloj del servidor está controlado

  @s1
  Scenario: Migrar un inicio existente sin cambiar su recibo ni escribir al leer
    Given un inicio de 14 anterior a la migración con startedAt "2026-09-07T10:00:00.123456Z"
    When consulto S con reloj "2026-09-07T10:00:01.123456789Z"
    Then recibo 200 con status running, revision "1", workedMicroseconds "0" y netMicroseconds "1000000"
    And changedAt y runningSince son startedAt y serverNow es "2026-09-07T10:00:01.123456Z"
    And el token identifica la misma sesión y revisión 1, sin Location
    And el inicio, su key y eventos previos permanecen iguales y no aparece escritura por el GET

  @s2
  Scenario: Pausar confirma un intervalo exacto y un recibo inmutable
    Given running desde "2026-09-07T10:00:00.123456Z", revisión "1" y acumulado "0"
    When envío P con key nueva y revisión 1 y el reloj devuelve "2026-09-07T10:00:01.123457999Z"
    Then recibo 201 después del commit con WorkSessionChange PAUSE y Location C de su id
    And occurredAt es "2026-09-07T10:00:01.123457Z", after.status paused y after.revision "2"
    And after.workedMicroseconds es "1000001" y after.runningSince es null
    And quedan un intervalo terminado, un cambio y un evento nuevos con el mismo instante
    And SessionStart, fin previsto, planificación, presupuesto, Today y estados de proyecto/tarea no cambian

  @s3
  Scenario: Reanudar abre otro intervalo sin sumar la pausa ni desplazar el fin
    Given paused en revisión "2", acumulado "1000001" y changedAt "2026-09-07T10:00:01.123457Z"
    When envío R con key nueva y revisión 2 a "2026-09-07T11:00:00.000001Z"
    Then recibo 201 con WorkSessionChange RESUME, Location C y after.revision "3"
    And after.status es running, workedMicroseconds "1000001" y runningSince igual a occurredAt
    And se abre un único intervalo y no se suma la hora de pausa ni se modifica SessionStart

  @s4
  Scenario Outline: Admitir transiciones en el mismo microsegundo sin fabricar trabajo
    Given estado <before> con última transición T y revisión vigente
    When envío <route> con key nueva y reloj T
    Then recibo 201 con estado <after> y revisión incrementada exactamente uno
    And el acumulado no aumenta y ningún intervalo se solapa
    Examples:
      | before  | route | after   |
      | running | P     | paused  |
      | paused  | R     | running |

  @s5
  Scenario: Sumar intervalos fraccionarios sin redondear cada uno
    Given dos intervalos terminados de 1 y 59999999 microsegundos y una pausa entre ellos
    When consulto S estando paused
    Then workedMicroseconds y netMicroseconds son exactamente "60000000"
    And no se suma la pausa ni se acredita avance o trabajo cerrado

  @s6
  Scenario Outline: Medianoche, DST y fin previsto no son transiciones
    Given una sesión running con zona <zone> e intervalo abierto en <start>
    And su fin previsto ya queda antes del snapshot
    When consulto S con serverNow <now>
    Then sigue running y el aporte abierto es <net> microsegundos exactos
    And no cambia el fin previsto ni aparece cambio o evento
    Examples:
      | zone          | start                       | now                         | net        |
      | UTC           | 2026-09-07T23:58:59.999999Z  | 2026-09-08T00:00:00.000001Z  | 60000002   |
      | Europe/Madrid | 2026-10-25T00:30:00Z         | 2026-10-25T01:30:00Z         | 3600000000 |

  @s7
  Scenario Outline: Rechazar reloj de comando atrasado o no representable antes de escribir
    Given una transición nueva compatible y reloj <clock>
    When envío el comando con revisión vigente
    Then recibo 409 WORK_SESSION_TIME_OUT_OF_RANGE con título "No se puede registrar la transición en ese instante."
    And estado, intervalos, recibos y outbox permanecen iguales
    Examples:
      | clock                                  |
      | un microsegundo antes de changedAt      |
      | año UTC 0000                           |
      | año UTC 10000                          |

  @s8
  Scenario Outline: Gestionar una sesión abierta no depende de la elegibilidad de inicio
    Given <context> y la zona histórica ya no existe en el catálogo
    When envío <route> desde su estado permitido con revisión vigente
    Then recibo 201 sin consultar disponibilidad ni catálogo
    And proyecto y tarea conservan sus estados y la sesión conserva su zona original
    Examples:
      | context                   | route |
      | proyecto completed        | P     |
      | tarea completed           | R     |
      | disponibilidad inexistente| P     |

  @s9
  Scenario: Una pausa conserva la única plaza del propietario
    Given mi sesión está paused
    When intento iniciar otra sesión en otra tarea propia con una key nueva de 14
    Then recibo 409 WORK_SESSION_ALREADY_ACTIVE con el id de la sesión pausada
    And active devuelve su SessionStart en el envoltorio original y no se crea otro inicio o evento
    And GET de inicio por id/key conserva el recibo original sin añadir status ni revisión

  @s10
  Scenario Outline: Validar el token completo de revisión antes de negocio
    When envío P con Work-Session-Revision <token>
    Then recibo <status> y <code> sin cambios persistidos
    And para VALIDATION_ERROR fieldErrors indica Work-Session-Revision/INVALID_VALUE
    Examples:
      | token                                 | status | code                  |
      | ausente                               | 428    | PRECONDITION_REQUIRED |
      | token entre comillas                  | 400    | VALIDATION_ERROR      |
      | lista de dos tokens                     | 400    | VALIDATION_ERROR      |
      | cabecera repetida                     | 400    | VALIDATION_ERROR      |
      | token con espacios                    | 400    | VALIDATION_ERROR      |
      | revisión 0                            | 400    | VALIDATION_ERROR      |
      | revisión +1                           | 400    | VALIDATION_ERROR      |
      | revisión 01                           | 400    | VALIDATION_ERROR      |
      | revisión 9223372036854775808           | 400    | VALIDATION_ERROR      |
      | token de revisión válido seguido de basura         | 400    | VALIDATION_ERROR      |

  @s11
  Scenario Outline: Conectar las convenciones heredadas a las cinco rutas nuevas
    Given una petición autenticada a <route> con query repetida y UUID inválido
    When envío esa petición
    Then recibo el error de query de 14 antes del error de UUID, sin lectura privada ni escritura
    Examples:
      | route |
      | P     |
      | R     |
      | S     |
      | C     |
      | K     |

  @s12
  Scenario Outline: Conservar el orden de validación del comando
    Given la petición contiene <first> y también <later>
    When envío P
    Then se devuelve exclusivamente <result> sin ejecutar etapas posteriores
    Examples:
      | first                    | later                      | result                         |
      | sesión anónima           | query prohibida            | 401 heredado                   |
      | key inválida             | Work-Session-Revision ausente           | validación de key heredada     |
      | Work-Session-Revision ausente          | JSON concatenado {} {}     | 428 PRECONDITION_REQUIRED      |
      | JSON concatenado {} {}    | sesión inexistente         | MALFORMED_JSON heredado        |
      | objeto con campo extra   | sesión inexistente         | validación de campo heredada   |
      | sesión ajena             | identidad del token de revisión distinta    | 404 WORK_SESSION_NOT_FOUND     |
      | identidad del token de revisión distinta   | key con recibo previo      | 412 PRECONDITION_FAILED        |
      | revisión obsoleta         | estado incompatible        | 412 PRECONDITION_FAILED        |

  @s13
  Scenario Outline: Proteger sesión y recibos sin revelar propiedad
    Given el identificador solicitado es <ownership>
    When consulto <route>
    Then recibo 404 <code> con los cuatro campos comunes y sin datos del propietario
    Examples:
      | ownership   | route | code                          |
      | ajeno       | S     | WORK_SESSION_NOT_FOUND        |
      | inexistente | S     | WORK_SESSION_NOT_FOUND        |
      | ajeno       | C     | WORK_SESSION_CHANGE_NOT_FOUND |
      | inexistente | C     | WORK_SESSION_CHANGE_NOT_FOUND |
      | ajeno       | K     | WORK_SESSION_CHANGE_NOT_FOUND |
      | inexistente | K     | WORK_SESSION_CHANGE_NOT_FOUND |

  @s14
  Scenario Outline: Una intención nueva incompatible no es replay
    Given estado <state> y revisión vigente
    When envío <route> con key nueva y esa revisión
    Then recibo 409 WORK_SESSION_STATE_CONFLICT sin consultar reloj ni escribir
    Examples:
      | state   | route |
      | paused  | P     |
      | running | R     |

  @s15
  Scenario: Agotar revisiones antes de capturar el reloj
    Given una transición compatible en revisión "9223372036854775807" y reloj fuera de rango
    When envío su comando con esa revisión y key nueva
    Then recibo 409 WORK_SESSION_REVISION_EXHAUSTED sin capturar reloj ni escribir

  @s16
  Scenario: Recuperar una intención idéntica antes de evaluar el estado actual
    Given P confirmó un recibo con key K1 y revisión esperada 1 y posteriormente se reanudó la sesión
    And el reloj actual falla
    When repito P con K1, {} y el token de revisión original de esa sesión en revisión 1
    Then recibo 200 con exactamente el recibo original y su Location C
    And no se consulta reloj ni se modifica estado, intervalos o número de cambios/eventos

  @s17
  Scenario Outline: Una key de cambio no cambia de intención
    Given una key propia ya confirmó una pausa
    When reutilizo esa key con <difference> y entradas previas válidas
    Then recibo 409 IDEMPOTENCY_CONFLICT antes de comparar revisión actual o reloj
    And no se devuelve otro recibo ni se escribe
    Examples:
      | difference                       |
      | acción RESUME y mismo id/revisión |
      | otra revisión esperada del mismo id y acción |

  @s18
  Scenario: Las keys de cambios de sesión no colisionan con otros espacios
    Given mi key ya existe como inicio de 14 y como cambio de bloque de 13
    When confirmo P con esa misma key, sesión propia y revisión vigente
    Then recibo 201 con un nuevo WorkSessionChange sin alterar aquellos recibos

  @s19
  Scenario Outline: Ordenar comandos concurrentes sobre la misma sesión
    Given dos peticiones alcanzan la misma sesión con revisión vigente y <keys>
    When ambas intentan pausar concurrentemente
    Then las respuestas son <responses> y queda exactamente una pausa, una revisión nueva, un recibo y un evento
    And el segundo resultado se resuelve contra el ganador durable sin reutilizar una transacción abortada
    Examples:
      | keys                          | responses                         |
      | la misma key e intención       | 201 y 200 con recibo idéntico      |
      | keys diferentes               | 201 y 412 PRECONDITION_FAILED     |

  @s20
  Scenario: Propietarios distintos no comparten bloqueo global
    Given otro propietario mantiene bloqueada su sesión mientras mi sesión es modificable
    When pauso mi sesión
    Then mi 201 se confirma antes de que el otro propietario libere su sesión
    And no se bloquean ni modifican proyecto, tarea o disponibilidad para mi cambio

  @s21
  Scenario Outline: Confirmar todas las escrituras o ninguna
    Given una transición nueva y <failure>
    When envío el comando
    Then recibo 503 STORAGE_UNAVAILABLE y permanecen iguales estado, intervalos, revisión, recibos y outbox
    And una supresión sin recibo ganador no se presenta como conflicto ni como éxito
    Examples:
      | failure                          |
      | supresión de escritura de estado |
      | supresión de escritura de intervalo |
      | supresión de inserción de recibo  |
      | fallo de inserción de outbox      |
      | fallo antes de confirmar commit  |

  @s22
  Scenario: Leer un snapshot coherente antes de capturar una sola hora
    Given una lectura S ha fijado snapshot running y una pausa concurrente confirma antes de que termine esa lectura
    When completo la lectura S
    Then obtengo íntegramente el estado running de ese snapshot y su token de revisión, nunca mezcla con paused
    And serverNow se captura una sola vez después de la lectura y el neto usa ese valor truncado a µs
    And una siguiente lectura independiente refleja la pausa confirmada

  @s23
  Scenario Outline: Distinguir reloj de lectura atrasado, inválido y sesión ausente
    Given <setup>
    When consulto S
    Then obtengo <result> sin escritura ni corrección de hechos
    Examples:
      | setup                                      | result                                  |
      | running y serverNow anterior a runningSince | 200 con neto igual a workedMicroseconds  |
      | paused y serverNow posterior a changedAt    | 200 con neto igual a workedMicroseconds  |
      | sesión propia y serverNow en año UTC 0000   | 409 WORK_SESSION_TIME_OUT_OF_RANGE       |
      | sesión propia y serverNow en año UTC 10000  | 409 WORK_SESSION_TIME_OUT_OF_RANGE       |
      | sesión ausente y reloj que fallaría         | 404 WORK_SESSION_NOT_FOUND sin reloj     |

  @s24
  Scenario Outline: Un fallo de lectura nunca significa ausencia
    Given <failure> al consultar <route>
    When completo la petición
    Then recibo 503 STORAGE_UNAVAILABLE, sin null, 404 ni escrituras inventadas
    Examples:
      | route | failure                 |
      | S     | error de consulta       |
      | S     | fallo al finalizar      |
      | C     | error de consulta       |
      | C     | fallo al finalizar      |
      | K     | error de consulta       |
      | K     | fallo al finalizar      |

  @s25
  Scenario Outline: Recuperar el hecho después de perder la respuesta y retirar el evento publicado
    Given un cambio confirmó pero su respuesta HTTP se perdió y hubo una transición posterior
    And el evento original se publicó en Rabbit, se retiró del outbox y se reinició la API
    When recupero el cambio mediante <route>
    Then recibo 200 con el recibo original completo, sin Location y sin capturar reloj
    And no aumenta el número de sesiones, intervalos, cambios ni eventos y no se restaura el estado histórico
    Examples:
      | route |
      | C     |
      | K     |

  @s26
  Scenario Outline: Publicar el cambio durable sin hacer depender el commit del broker
    Given Rabbit no está disponible al confirmar <action> y luego vuelve a estar disponible
    When el publicador entrega el evento pendiente
    Then el commit HTTP ya confirmado se conserva y llega WorkSessionStateChanged.v1 a organization.work-session-state-changed.v1
    And la ruta es work-session.state-changed.v1, cola quorum durable y mensaje persistente con confirms
    And contiene sólo eventId, aggregateId, ownerId, occurredAt, schemaVersion, type, action, revision, fromStatus, toStatus, workedMicroseconds, runningSince
    And aggregateId es sessionId, schemaVersion 1 y los valores coinciden con el recibo, con eventId independiente
    And reintentos conservan identidad y payload; un resultado incierto admite redelivery, sin exactly-once
    And se conservan las nueve rutas y clasificación blocked existentes
    Examples:
      | action |
      | PAUSE  |
      | RESUME |

  @s27
  Scenario Outline: Rechazar respuestas incompatibles sin mostrar vigencia o éxito
    Given una respuesta 200/201 manipulada contiene <invalid>
    When el cliente procesa la respuesta
    Then no presenta un estado válido ni confirma el cambio a partir de ese cuerpo
    And si corresponde a un POST conserva su intención incierta para comprobarla
    Examples:
      | invalid                                               |
      | campos extra o ausentes en state/snapshot/recibo        |
      | revisión no canónica o superior a BIGINT                |
      | microsegundos no canónicos o negativos                 |
      | changedAt anterior a startedAt                         |
      | runningSince distinto de changedAt estando running     |
      | runningSince no null estando paused                    |
      | acumulado superior al intervalo startedAt/changedAt     |
      | neto distinto de la fórmula exacta del snapshot         |
      | token de revisión que no coincide con id/revisión del estado         |
      | recibo que altera SessionStart o no incrementa revisión |
      | acción, transición o aritmética incompatible del recibo |

  @s28
  Scenario: Conservar precisión superior a Number en la consulta y el comando
    Given un snapshot válido con revisión "9007199254740993" y neto "9007199254740993"
    When la persona decide la transición permitida
    Then el cliente conserva ambos valores exactos y envía el token de revisión con esa revisión sin redondear
    And no compara el reloj del dispositivo para aceptar o corregir serverNow

  @s29
  Scenario Outline: Mostrar la acción sólo desde estado consultado y fechado
    Given active devuelve un SessionStart propio y S devuelve <state>
    When se presenta la sección de sesión
    Then muestra <label>, exclusivamente <action> y actualización manual
    And conserva inicio/fin previsto con fecha/zona y explica que la pausa no mueve el fin
    And muestra "Tiempo de trabajo hasta la actualización" y la hora del snapshot, sin segundero ni polling
    Examples:
      | state   | label    | action   |
      | running | En curso | Pausar   |
      | paused  | En pausa | Reanudar |

  @s30
  Scenario: Una consulta pendiente o fallida no convierte el recibo de inicio en estado running
    Given active devuelve SessionStart y la consulta S sigue pendiente o falla
    When observo la sección
    Then se anuncia carga o error con reintento y no se ofrecen Pausar ni Reanudar desde el recibo
    And una zona histórica no resoluble se presenta en UTC etiquetado sin cambiar el hecho

  @s31
  Scenario: Separar confirmación histórica de actualización actual fallida
    Given un POST confirmó un WorkSessionChange válido y la consulta posterior S falla
    When se presenta el resultado
    Then se anuncia el cambio confirmado y que el estado actual no se pudo actualizar
    And no se revoca el recibo ni se envía otro POST; reintentar actualiza sólo S

  @s32
  Scenario Outline: Conservar la intención durante incertidumbre y comprobación
    Given un POST enviado con key, acción y revisión retenidas termina en <outcome>
    When la persona elige "Comprobar cambio"
    Then se consulta K de esa misma key y no se envía un POST automático
    And nuevas decisiones siguen bloqueadas hasta resolver la intención
    And si K devuelve 404 se permite comprobar otra vez o reenviar manualmente la misma intención, sin afirmar rollback
    Examples:
      | outcome                  |
      | fallo de red             |
      | 503                      |
      | respuesta incompatible   |
      | código desconocido       |

  @s33
  Scenario: Una revisión rechazada requiere una decisión nueva
    Given el POST devuelve 412 PRECONDITION_FAILED por otra transición confirmada
    When la persona consulta el estado actual y decide la acción permitida
    Then usa nueva key y revisión del nuevo snapshot
    And no se reintenta automáticamente la intención rechazada

  @s34
  Scenario: Recuperar acceso manualmente no reenvía un comando
    Given el envío recibe el rechazo CSRF reconocido y conserva su intención
    When la persona renueva acceso mediante SessionGate
    Then no se envía otro POST hasta una acción manual separada
    And ese reenvío conserva cuerpo, key y revisión y usa el token vigente

  @s35
  Scenario: Salir no revoca el comando y recargar recupera estado sin inventar historial
    Given un POST sigue pendiente y la sección explica que salir no revoca el envío
    When la persona sale y vuelve tras recargar sin la key en memoria
    Then puede consultar active y S para descubrir el estado confirmado
    And no se reenvía ni se promete identificar el comando perdido sin key

  @s36
  Scenario Outline: Retirar datos y descartar respuestas obsoletas en cualquier await
    Given una petición de comando, recibo o estado espera <stage> y cambia ruta o sesión
    When termina <outcome> del contexto antiguo
    Then se aborta y descarta antes de propagar fallo de acceso o restaurar datos del contexto anterior
    And un 401 del contexto actual sí retira datos inmediatamente
    Examples:
      | stage                         | outcome                         |
      | respuesta HTTP                | respuesta HTTP 401 atrasada      |
      | lectura JSON de un HTTP 200    | cuerpo JSON diferido            |
      | clasificación de error no 401 | clasificación diferida          |

  @s37
  Scenario Outline: Mantener foco y feedback durante envío, comprobación y error
    Given la persona activó Pausar, Reanudar o Comprobar cambio y <focus>
    When la petición entra en espera y después cambia el control disponible
    Then la espera se anuncia sin falso éxito y el foco <destination>
    And errores y confirmación histórica se anuncian separadamente del estado actual
    Examples:
      | focus                                         | destination                   |
      | permanece en un iniciador que desaparece      | llega al encabezado de sesión |
      | ya eligió otro control que sigue presente     | permanece en ese control      |

  @s38
  Scenario: Verificar el recorrido accesible sin ampliar el alcance a cierre o avisos
    Given la sección presenta carga, running, paused, incertidumbre y error con textos largos
    When se revisa el recorrido según docs/ux-requirements.md
    Then la matriz de 30 principios identifica evidencia o límite por fila
    And teclado, foco visible, controles de 44px, feedback temprano y anuncios son operables sin depender del color
    And no hay recorte u overflow en la matriz responsive, zoom real 200% y texto ampliado
    And se registran Chromium, Firefox y WebKit y los límites de dispositivos físicos no medidos
    And no aparecen controles de cierre, ampliación, aviso ni historial global ni afirmaciones de usabilidad humana no observada
  @s39
  Scenario: Descartar un snapshot antiguo tras confirmar un cambio en el mismo contexto
    Given un GET S iniciado en running y revisión 1 sigue pendiente
    And un POST de pausa confirma revisión 2 y un GET S posterior ya muestra paused en revisión 2
    When llega la respuesta del primer GET S con running y revisión 1
    Then se descarta sin reemplazar el estado paused ni su revisión y hora de actualización
    And permanece disponible sólo Reanudar, sin restaurar Pausar ni revocar el cambio confirmado
