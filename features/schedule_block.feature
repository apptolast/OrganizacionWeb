@schedule_block @approved
Feature: Planificar bloques propios con revisión de tiempo y presupuesto
  Contrato aprobado bajo autorización global vigente: 62 escenarios y 325 casos expandidos, revisados antes de producción.
  Base: /api/v1/projects/{projectId}/tasks/{taskId}/blocks; preview es POST /preview.
  Cada fila de Examples es independiente; lo no mencionado permanece válido.
  Petición de preview: exactamente objective, startLocal, endLocal, zoneId, startOffset y endOffset.
  Creación: esos seis campos con offsets explícitos y allowOverBudget booleano obligatorio.
  DTO de bloque: exactamente id, projectId, taskId, objective, startAt, endAt, zoneId, durationMinutes y createdAt.
  Día de presupuesto: exactamente date, budgetMinutes, plannedSeconds, requestedSeconds y excessSeconds.
  El objetivo usa strip de Unicode White_Space en extremos, conserva espacios interiores y cuenta puntos de código Unicode.
  Rechazos no escriben bloques, preferencias ni outbox; lecturas y preview tampoco escriben.
  Errores: application/problem+json con type, title, status y code, sin SQL, trazas ni secretos.
  VALIDATION_ERROR añade errors con objetos cerrados field/code/message; no se fija precedencia entre errores estructurales del cuerpo.
  Los fixtures temporales usan reloj de servidor controlado; las fechas de ejemplos son futuras salvo indicación contraria.

  Background:
    Given una sesión válida de persona-a y protección de origen y CSRF válida para POST
    And un proyecto propio P en estado active con tarea propia T pending
    And disponibilidad propia configurada en UTC con 120 minutos para cada día
    And la petición base tiene objetivo "Preparar borrador", inicio "2030-01-07T10:00", fin "2030-01-07T11:00", zona "UTC" y offsets "Z"
    And no existen reservas salvo las indicadas

  @s1
  Scenario: Revisar sin persistir y con esquema cerrado
    When envío la petición base a preview
    Then recibo 200 con exactamente objective, zoneId, startAt, endAt, startOffset, endOffset, durationMinutes, availabilityEtag, budgetZoneId y days
    And objective es "Preparar borrador", zoneId y budgetZoneId son "UTC", ambos offsets son "Z" y durationMinutes es 60
    And startAt es "2030-01-07T10:00:00Z" y endAt es "2030-01-07T11:00:00Z"
    And availabilityEtag corresponde a la disponibilidad configurada vigente
    And days contiene sólo "2030-01-07" con budgetMinutes 120, plannedSeconds 0, requestedSeconds 3600 y excessSeconds 0
    And no cambia ninguna entidad ni se crea outbox

  @s2
  Scenario Outline: Crear una reserva elegible sin acreditar trabajo
    Given el proyecto P está en estado <estado>
    And una revisión válida de la petición base
    When creo el bloque con key UUID canónica nueva, Availability-Revision de preview y allowOverBudget false
    Then recibo 201 con Location del detalle y DTO cerrado de nueve campos coincidente con la revisión
    And el servidor genera blockId y eventId, sin tomar la key como identificador de entidad
    And createdAt es UTC con precisión de microsegundos
    And se persisten exactamente un bloque y una outbox y se conserva la relación P/T
    And no cambian estado, revisión ni fechas del proyecto, tarea o disponibilidad ni se registra trabajo realizado
    Examples:
      | estado |
      | idea   |
      | active |
      | paused |

  @s3
  Scenario Outline: Normalizar objetivo y contar puntos de código
    Given objective es <entrada>
    When solicito preview
    Then recibo 200 con objective <normalizado>
    Examples:
      | entrada                         | normalizado           |
      | espacios exteriores y "Meta"    | "Meta"                |
      | un punto de código "😀"         | "😀"                  |
      | NBSP exterior y "Una  meta"     | "Una  meta"           |
      | 500 puntos de código no blancos | los mismos 500 puntos |

  @s4
  Scenario Outline: Rechazar entradas estructuralmente inválidas
    Given la petición de <operacion> contiene <defecto>
    When envío esa petición con headers válidos
    Then recibo 400 VALIDATION_ERROR con errors asociados al campo o cuerpo afectado
    And no hay escrituras ni se exige un orden arbitrario entre varios errores estructurales
    Examples:
      | operacion | defecto                           |
      | preview   | campo desconocido                 |
      | preview   | objective ausente                 |
      | preview   | objective null                    |
      | preview   | objective número 7                |
      | preview   | objective array []                |
      | preview   | objective objeto {}               |
      | preview   | objective vacío después de strip  |
      | preview   | objective de 501 puntos de código |
      | preview   | startLocal ausente                |
      | preview   | startLocal null                   |
      | preview   | startLocal número 7               |
      | preview   | startLocal array []               |
      | preview   | startLocal objeto {}              |
      | preview   | endLocal ausente                  |
      | preview   | endLocal null                     |
      | preview   | endLocal número 7                 |
      | preview   | endLocal array []                 |
      | preview   | endLocal objeto {}                |
      | preview   | zoneId ausente                    |
      | preview   | zoneId null                       |
      | preview   | zoneId número 7                   |
      | preview   | zoneId array []                   |
      | preview   | zoneId objeto {}                  |
      | preview   | startOffset ausente               |
      | preview   | startOffset número 7              |
      | preview   | startOffset array []              |
      | preview   | startOffset objeto {}             |
      | preview   | endOffset ausente                 |
      | preview   | endOffset número 7                |
      | preview   | endOffset array []                |
      | preview   | endOffset objeto {}               |
      | preview   | raíz JSON que no es objeto        |
      | creación  | allowOverBudget ausente           |
      | creación  | allowOverBudget null              |
      | creación  | allowOverBudget número 1          |
      | creación  | allowOverBudget texto true        |
      | creación  | allowOverBudget array []          |
      | creación  | allowOverBudget objeto {}         |
      | creación  | startOffset null                  |
      | creación  | endOffset null                    |

  @s5
  Scenario Outline: Exigir formato local estricto
    Given <campo> es <valor>
    When solicito preview
    Then recibo 400 VALIDATION_ERROR con field <campo> y code INVALID_FORMAT
    Examples:
      | campo      | valor               |
      | startLocal | 2030-1-07T10:00     |
      | endLocal   | 2030-01-07 11:00    |
      | startLocal | 2030-02-30T10:00    |
      | endLocal   | 2030-01-07T24:00    |
      | startLocal | 2030-01-07T10:00:00 |
      | endLocal   | 2030-01-07T11:00Z   |
      | startLocal | 0000-01-01T10:00    |
      | endLocal   | 10000-01-01T11:00   |

  @s6
  Scenario Outline: Comparar duración real sin redondear
    Given los extremos resueltos tienen duración real <duracion>
    When solicito preview
    Then recibo <resultado>
    Examples:
      | duracion                                                                                                     | resultado                                  |
      | 1 minuto                                                                                                     | 200 con durationMinutes 1                  |
      | 1440 minutos                                                                                                 | 200 con durationMinutes 1440               |
      | 0 minutos                                                                                                    | 400 VALIDATION_ERROR endLocal OUT_OF_RANGE |
      | -1 minuto                                                                                                    | 400 VALIDATION_ERROR endLocal OUT_OF_RANGE |
      | 1441 minutos                                                                                                 | 400 VALIDATION_ERROR endLocal OUT_OF_RANGE |
      | 60,5 minutos en Africa/Monrovia: 1972-01-06T23:30 -00:44:30 a 1972-01-07T01:15 Z, reloj 1972-01-06T00:00:00Z | 400 VALIDATION_ERROR endLocal OUT_OF_RANGE |

  @s7
  Scenario Outline: Validar inicio frente al servidor en cada creación nueva
    Given el inicio resuelto está <relacion> respecto al reloj del servidor
    And <situacion>
    When envío <operacion>
    Then recibo <resultado>
    Examples:
      | relacion          | situacion                                 | operacion | resultado                               |
      | exactamente igual | todavía no hay revisión                   | preview   | 200                                     |
      | un minuto antes   | todavía no hay revisión                   | preview   | 400 VALIDATION_ERROR startLocal IN_PAST |
      | un minuto antes   | preview fue válido antes de avanzar reloj | creación  | 400 VALIDATION_ERROR startLocal IN_PAST |
      | exactamente igual | preview válido y headers vigentes         | creación  | 201                                     |

  @s8
  Scenario Outline: Resolver zonas y offsets sin elegir ocurrencias ambiguas
    Given zona <zona>, inicio <inicio>, fin <fin> y offsets solicitados <offsets>
    When solicito preview
    Then recibo <resultado>
    Examples:
      | zona                | inicio           | fin              | offsets       | resultado                                                                                      |
      | UTC                 | 2030-01-07T10:00 | 2030-01-07T11:00 | null/null     | 200 con Z/Z y duración 60                                                                      |
      | Europe/Madrid       | 2026-03-29T02:15 | 2026-03-29T03:30 | null/null     | 400 VALIDATION_ERROR startLocal NONEXISTENT_LOCAL_TIME                                         |
      | Europe/Madrid       | 2026-03-29T01:30 | 2026-03-29T02:15 | null/null     | 400 VALIDATION_ERROR endLocal NONEXISTENT_LOCAL_TIME                                           |
      | Europe/Madrid       | 2026-10-25T02:15 | 2026-10-25T02:45 | null/null     | 400 VALIDATION_ERROR startOffset AMBIGUOUS_OFFSET con validOffsets startOffset [+02:00,+01:00] |
      | Europe/Madrid       | 2026-10-25T01:30 | 2026-10-25T02:45 | null/null     | 400 VALIDATION_ERROR endOffset AMBIGUOUS_OFFSET con validOffsets endOffset [+02:00,+01:00]     |
      | Europe/Madrid       | 2026-10-25T02:45 | 2026-10-25T02:15 | +02:00/+01:00 | 200 con startAt 2026-10-25T00:45:00Z, endAt 2026-10-25T01:15:00Z y duración 30                 |
      | Europe/Madrid       | 2030-01-07T10:00 | 2030-01-07T11:00 | +02:00/+01:00 | 400 VALIDATION_ERROR startOffset INVALID_OFFSET con validOffsets startOffset [+01:00]          |
      | Australia/Lord_Howe | 2026-04-05T01:45 | 2026-04-05T01:45 | +11:00/+10:30 | 200 con duración 30                                                                            |
      | Australia/Lord_Howe | 2026-10-04T02:15 | 2026-10-04T03:00 | null/null     | 400 VALIDATION_ERROR startLocal NONEXISTENT_LOCAL_TIME                                         |

  @s9
  Scenario Outline: Conservar catálogo y sintaxis canónica de offsets
    Given <condicion>
    When solicito preview
    Then recibo <resultado>
    Examples:
      | condicion                                                                          | resultado                                                                |
      | zoneId no pertenece al catálogo Java                                               | 400 VALIDATION_ERROR asociado a zoneId                                   |
      | startOffset es +00:00 en UTC, cuyo ID canónico es Z                                | 400 VALIDATION_ERROR asociado a startOffset                              |
      | endOffset es +1:00                                                                 | 400 VALIDATION_ERROR asociado a endOffset                                |
      | Europe/Paris 1900-01-01T10:00 a 10:01, ambos +00:09:21, reloj 1899-12-31T00:00:00Z | 200 con UTC 09:50:39Z a 09:51:39Z, offsets +00:09:21 y durationMinutes 1 |
      | alias exacto del catálogo Java con extremos inequívocos                            | 200 conservando el ID de zona elegido                                    |

  @s10
  Scenario Outline: Mantener extremos UTC y fechas públicas dentro de años admitidos
    Given la conversión de <extremo> produce <situacion>
    When solicito preview con reloj anterior o igual al inicio
    Then recibo <resultado>
    Examples:
      | extremo    | situacion                                             | resultado                                          |
      | startLocal | instante UTC en año 0000                              | 400 VALIDATION_ERROR asociado a startLocal         |
      | endLocal   | instante UTC en año 10000                             | 400 VALIDATION_ERROR asociado a endLocal           |
      | startLocal | fecha de presupuesto en año 0000                      | 400 VALIDATION_ERROR asociado a startLocal         |
      | endLocal   | fecha de presupuesto en año 10000                     | 400 VALIDATION_ERROR asociado a endLocal           |
      | endLocal   | fecha pública 9999-12-31 con límite interno año 10000 | 200 sin exponer ni persistir extremos de año 10000 |

  @s11
  Scenario Outline: Distribuir segundos por días reales de la zona del presupuesto
    Given disponibilidad en <zonaPresupuesto> con 120 minutos diarios
    And bloque en <zonaBloque> desde <inicio> hasta <fin> con offsets válidos explícitos
    When solicito preview
    Then days contiene <dias> en fecha ascendente y sólo intersecciones positivas
    And la suma de requestedSeconds coincide con durationMinutes multiplicado por 60
    Examples:
      | zonaPresupuesto | zonaBloque    | inicio           | fin              | dias                                                 |
      | UTC             | UTC           | 2030-01-07T23:30 | 2030-01-08T00:30 | 2030-01-07:1800 segundos; 2030-01-08:1800 segundos   |
      | UTC             | UTC           | 2030-01-07T23:00 | 2030-01-08T00:00 | sólo 2030-01-07:3600 segundos                        |
      | Europe/Madrid   | UTC           | 2030-01-07T22:30 | 2030-01-07T23:30 | 2030-01-07:1800 segundos; 2030-01-08:1800 segundos   |
      | Europe/Madrid   | Europe/Madrid | 2026-03-29T00:00 | 2026-03-30T00:00 | sólo 2026-03-29:82800 segundos, durationMinutes 1380 |
      | Europe/Madrid   | UTC           | 2026-10-25T22:30 | 2026-10-25T23:30 | 2026-10-25:1800 segundos; 2026-10-26:1800 segundos   |

  @s12
  Scenario: Sumar reservas de otros proyectos y estados sin sumar usuarios ajenos
    Given hay reservas propias no solapadas del día por 1800 segundos en P y 3600 segundos en otro proyecto completado con tarea completada
    And otra persona tiene 7200 segundos reservados ese día
    When reviso la petición base de 3600 segundos
    Then el día devuelve plannedSeconds 5400, requestedSeconds 3600, budgetMinutes 120 y excessSeconds 1800
    And no se libera ni modifica ninguna reserva por su estado

  @s13
  Scenario Outline: Aplicar intervalos semiabiertos entre todos los proyectos propios
    Given existe una reserva de <propietario> en otro proyecto entre 09:00Z y 10:00Z
    And la nueva reserva comienza a <inicio> y termina a 11:00Z usando una zona distinta
    When solicito preview
    Then recibo <resultado>
    Examples:
      | propietario | inicio | resultado         |
      | persona-a   | 10:00Z | 200               |
      | persona-a   | 09:59Z | 409 BLOCK_OVERLAP |
      | persona-b   | 09:59Z | 200               |

  @s14
  Scenario: Elegir un conflicto propio determinista sin divulgar otros datos
    Given varias reservas propias intersectan la petición con distintos startAt e id
    When solicito preview
    Then recibo 409 BLOCK_OVERLAP con exactamente conflict adicional que contiene id, projectId y taskId
    And conflict identifica la primera reserva por startAt ascendente e id ascendente
    And no se enumeran reservas ni datos ajenos

  @s15
  Scenario Outline: Mostrar exceso y exigir consentimiento específico incluso en descanso
    Given el presupuesto del día es <presupuesto> minutos y plannedSeconds es <planificado>
    And la petición dura 3600 segundos sin solapes
    When envío <operacion> con <permiso>
    Then recibo <resultado>
    Examples:
      | presupuesto | planificado | operacion | permiso               | resultado                                                                 |
      | 30          | 0           | preview   | sin allowOverBudget   | 200 con excessSeconds 1800                                                |
      | 0           | 0           | preview   | sin allowOverBudget   | 200 con excessSeconds 3600                                                |
      | 30          | 0           | creación  | allowOverBudget false | 409 BUDGET_EXCEEDED con budgetZoneId UTC y days recalculados, exceso 1800 |
      | 0           | 0           | creación  | allowOverBudget false | 409 BUDGET_EXCEEDED con budgetZoneId UTC y days recalculados, exceso 3600 |
      | 30          | 0           | creación  | allowOverBudget true  | 201 sin cambiar el presupuesto                                            |
      | 0           | 0           | creación  | allowOverBudget true  | 201 sin cambiar el presupuesto                                            |

  @s16
  Scenario Outline: Recalcular capacidad al crear sin extender el permiso a solapes
    Given preview de 60 minutos mostró exceso 0 sobre presupuesto 120
    And después otra reserva propia <cambio> sin cambiar Availability-Revision
    When creo el bloque revisado con allowOverBudget <permiso>
    Then recibo <resultado>
    Examples:
      | cambio                           | permiso | resultado                                   |
      | consume 90 minutos sin solaparse | false   | 409 BUDGET_EXCEEDED con days de exceso 1800 |
      | consume 90 minutos sin solaparse | true    | 201 para exactamente el bloque solicitado   |
      | intersecta el bloque             | true    | 409 BLOCK_OVERLAP                           |

  @s17
  Scenario Outline: Exigir disponibilidad y elegibilidad en orden de negocio
    Given <condicion>
    When envío <operacion> nueva estructuralmente válida
    Then recibo <resultado> sin datos adicionales de reservas
    Examples:
      | condicion                                                     | operacion | resultado                         |
      | disponibilidad ausente y proyecto completed                   | preview   | 409 AVAILABILITY_REQUIRED         |
      | disponibilidad configurada y proyecto y tarea completed       | preview   | 409 PROJECT_COMPLETED             |
      | proyecto active y tarea completed                             | creación  | 409 TASK_COMPLETED                |
      | proyecto completed y zona de disponibilidad irresoluble       | creación  | 409 PROJECT_COMPLETED             |
      | proyecto/tarea elegibles y zona de disponibilidad irresoluble | preview   | 409 AVAILABILITY_ZONE_UNAVAILABLE |
      | proyecto/tarea elegibles y zona de disponibilidad irresoluble | creación  | 409 AVAILABILITY_ZONE_UNAVAILABLE |

  @s18
  Scenario Outline: Validar headers de creación sin confundir revisión con intención
    Given <headers>
    When creo la petición base
    Then recibo <resultado>
    Examples:
      | headers                                                                | resultado                                             |
      | Availability-Revision ausente e Idempotency-Key ausente                | 428 PRECONDITION_REQUIRED                             |
      | Availability-Revision mal formada e Idempotency-Key ausente            | 400 VALIDATION_ERROR asociado a Availability-Revision |
      | Availability-Revision configurada válida e Idempotency-Key ausente     | 400 VALIDATION_ERROR asociado a Idempotency-Key       |
      | Availability-Revision configurada válida e Idempotency-Key no canónica | 400 VALIDATION_ERROR asociado a Idempotency-Key       |
      | Availability-Revision configurada distinta y key válida                | 412 AVAILABILITY_CONFLICT                             |
      | Availability-Revision distinta, key válida y proyecto completed        | 412 AVAILABILITY_CONFLICT                             |

  @s19
  Scenario Outline: Conservar seguridad y no-store también en errores
    Given <condicion>
    When invoco <operacion>
    Then recibo <resultado> con Cache-Control no-store
    And no se divulgan datos privados ni hay escrituras
    Examples:
      | condicion                                       | operacion      | resultado                             |
      | sesión ausente                                  | GET lista      | 401 UNAUTHENTICATED                   |
      | sesión ausente                                  | GET detalle    | 401 UNAUTHENTICATED                   |
      | sesión ausente                                  | GET by-request | 401 UNAUTHENTICATED                   |
      | sesión ausente                                  | POST preview   | 401 UNAUTHENTICATED                   |
      | sesión ausente                                  | POST creación  | 401 UNAUTHENTICATED                   |
      | origen no permitido                             | POST preview   | 403 UNTRUSTED_ORIGIN                  |
      | CSRF inválido                                   | POST creación  | 403 CSRF_INVALID                      |
      | query adicional e ID mal formado                | GET lista      | 400 VALIDATION_ERROR asociado a query |
      | ID de ruta incompleto y headers ausentes        | POST creación  | 400 VALIDATION_ERROR asociado a ID    |
      | headers mal formados y cuerpo inválido          | POST creación  | error de headers anterior al cuerpo   |
      | cuerpo inválido y contexto ajeno                | POST preview   | 400 VALIDATION_ERROR                  |
      | contexto ajeno y disponibilidad ausente         | POST preview   | 404 RESOURCE_NOT_FOUND                |
      | contexto inexistente                            | GET detalle    | 404 RESOURCE_NOT_FOUND                |
      | tarea existente bajo proyecto incorrecto propio | GET lista      | 404 RESOURCE_NOT_FOUND                |

  @s20
  Scenario Outline: Confirmar respuestas privadas correctas sin cachearlas
    When ejecuto <operacion> válida dentro del contexto propio
    Then la respuesta incluye Cache-Control no-store
    And sólo los POST de creación confirmados escriben bloques y outbox
    Examples:
      | operacion      |
      | GET lista      |
      | GET detalle    |
      | GET by-request |
      | POST preview   |
      | POST creación  |

  @s21
  Scenario Outline: Reproducir una intención confirmada antes de reglas que pudieron cambiar
    Given existe bloque confirmado con key K y petición normalizada retenida
    And <cambio>
    When reenvío la misma intención y K con cuerpo y headers sintácticamente válidos
    Then recibo 200 con exactamente el DTO y Location de la creación original
    And se conserva un bloque y un evento y no se vuelven a resolver reglas temporales o de catálogo
    Examples:
      | cambio                                            |
      | el inicio ya está en el pasado                    |
      | tarea y proyecto se completaron                   |
      | cambió zona y presupuesto de disponibilidad       |
      | Availability-Revision retenida ya no está vigente |
      | zona del bloque salió del catálogo actual         |
      | cambiaron reglas de offsets de la zona            |
      | zona de disponibilidad ya no puede resolverse     |

  @s22
  Scenario Outline: Vincular idempotencia a intención normalizada y tarea
    Given existe un bloque confirmado para T con key K
    When envío K con <cambio> y headers válidos
    Then recibo <resultado>
    Examples:
      | cambio                                           | resultado                                      |
      | sólo espacios Unicode White_Space exteriores     | 200 con DTO original sin evento nuevo          |
      | objetivo distinto                                | 409 IDEMPOTENCY_CONFLICT                       |
      | startLocal distinto                              | 409 IDEMPOTENCY_CONFLICT                       |
      | endLocal distinto                                | 409 IDEMPOTENCY_CONFLICT                       |
      | zoneId distinto                                  | 409 IDEMPOTENCY_CONFLICT                       |
      | startOffset diferente, pero canónico             | 409 IDEMPOTENCY_CONFLICT                       |
      | endOffset diferente, pero canónico               | 409 IDEMPOTENCY_CONFLICT                       |
      | allowOverBudget distinto                         | 409 IDEMPOTENCY_CONFLICT                       |
      | Availability-Revision distinta pero bien formada | 200 con DTO original sin evento nuevo          |
      | otra tarea propia y otro intervalo sin solape    | 201 con bloque diferente vinculado a esa tarea |

  @s23
  Scenario Outline: Replay conserva validación de seguridad, estructura y propiedad
    Given una key confirmada K
    And <defecto>
    When reenvío la intención anterior
    Then recibo <resultado> sin crear otro evento
    Examples:
      | defecto                           | resultado                 |
      | Availability-Revision ausente     | 428 PRECONDITION_REQUIRED |
      | Idempotency-Key mal formada       | 400 VALIDATION_ERROR      |
      | cuerpo de estructura inválida     | 400 VALIDATION_ERROR      |
      | pérdida de propiedad del contexto | 404 RESOURCE_NOT_FOUND    |
      | sesión ausente                    | 401                       |
      | CSRF inválido                     | 403 CSRF_INVALID          |

  @s24
  Scenario Outline: Recuperar por key sin confundir ausencia de bloque con contexto
    Given <condicion>
    When consulto GET by-request con key canónica
    Then recibo <resultado> y ninguna escritura
    Examples:
      | condicion                                   | resultado                            |
      | key confirmada propia                       | 200 con DTO original de nueve campos |
      | key no encontrada dentro de contexto propio | 404 BLOCK_NOT_FOUND                  |
      | contexto ajeno                              | 404 RESOURCE_NOT_FOUND               |
      | contexto inexistente                        | 404 RESOURCE_NOT_FOUND               |
      | key pertenece a otra tarea propia           | 404 BLOCK_NOT_FOUND                  |
      | key confirmada y proyecto/tarea completados | 200 con DTO original                 |
      | key confirmada y zona guardada irresoluble  | 200 con DTO original                 |

  @s25
  Scenario: Paginar bloques persistidos con desempate estable
    Given T tiene 21 bloques con createdAt iguales en microsegundos y UUID distintos
    When consulto lista y continuación con su cursor
    Then cada cuerpo contiene exactamente items y nextCursor, con 20 y 1 DTO respectivamente
    And el orden total es createdAt DESC e id DESC, sin duplicados ni omisiones
    And nextCursor es opaco en primera página y null en última
    And ni estimaciones ni hora local ni reloj del navegador alteran el orden

  @s26
  Scenario Outline: Leer lista y detalle sin liberar reservas históricas
    Given <condicion>
    When consulto <lectura>
    Then recibo <resultado> sin modificar ningún estado
    Examples:
      | condicion                                 | lectura | resultado                               |
      | contexto propio sin bloques               | lista   | 200 con items vacío y nextCursor null   |
      | bloque propio y tarea/proyecto completed  | detalle | 200 con DTO guardado                    |
      | bloque propio y cambio de preferencia     | detalle | 200 conservando UTC y zoneId originales |
      | blockId ausente dentro de contexto propio | detalle | 404 BLOCK_NOT_FOUND                     |
      | blockId pertenece a otra tarea            | detalle | 404 BLOCK_NOT_FOUND                     |
      | error de almacenamiento                   | lista   | 503, nunca lista vacía                  |

  @s27
  Scenario Outline: Rechazar cursores ajenos a esta colección
    Given cursor <cursor>
    When consulto la lista propia
    Then recibo 400 VALIDATION_ERROR sin datos de otro contexto
    Examples:
      | cursor                      |
      | mal formado                 |
      | de tareas                   |
      | de subtareas                |
      | de bloques de otro proyecto |
      | de bloques de otra tarea    |

  @s28
  Scenario: Reproyectar presupuesto sin reescribir historia
    Given una reserva UTC entre 2030-01-07T22:30Z y 2030-01-07T23:30Z
    And disponibilidad cambió de UTC a Europe/Madrid
    When reviso otra reserva no solapada que afecta ambos días
    Then plannedSeconds de la reserva histórica se distribuye en 1800 para 2030-01-07 y 1800 para 2030-01-08
    And la reserva histórica conserva sus instantes UTC y zona de planificación originales

  @s29
  Scenario: Deduplicar envíos concurrentes de la misma intención
    Given PostgreSQL real y dos peticiones idénticas con la misma key para T
    When ambas crean concurrentemente y una espera el bloqueo de disponibilidad
    Then las respuestas son 201 y 200 con el mismo DTO y Location
    And sólo existe un bloque y una outbox
    And la petición que esperó recupera el bloque sin rechazar solape consigo misma

  @s30
  Scenario Outline: Serializar reservas propias entre proyectos sin éxito inválido
    Given PostgreSQL real y dos peticiones nuevas de persona-a
    And <condicion>
    When las creaciones compiten concurrentemente
    Then <resultado>
    And cada creación confirmada tiene exactamente una outbox y no hay éxitos parciales
    Examples:
      | condicion                                                                            | resultado                                               |
      | proyectos distintos y sus intervalos se solapan                                      | una confirma 201 y otra recibe 409 BLOCK_OVERLAP        |
      | proyectos distintos, juntas exceden presupuesto pero separadas no, ambas sin permiso | una confirma 201 y otra recibe 409 BUDGET_EXCEEDED      |
      | misma tarea/key con intenciones distintas                                            | una confirma 201 y otra recibe 409 IDEMPOTENCY_CONFLICT |

  @s31
  Scenario Outline: Coordinar creación con estados y preferencias sin ciclos
    Given PostgreSQL real y una creación nueva válida
    And la adquisición coordinada permite confirmar primero <primera>
    When la creación compite con <operacion>
    Then ambas transacciones terminan sin deadlock ni éxito parcial
    And <resultado>
    Examples:
      | primera                | operacion              | resultado                                                                      |
      | la creación            | completar la tarea     | creación 201 y cierre posterior conservan el bloque                            |
      | completar la tarea     | completar la tarea     | creación 409 TASK_COMPLETED sin bloque ni evento nuevo                         |
      | la creación            | completar el proyecto  | creación 201 y cierre posterior conservan el bloque                            |
      | completar el proyecto  | completar el proyecto  | creación 409 PROJECT_COMPLETED sin bloque ni evento nuevo                      |
      | la creación            | cambiar disponibilidad | creación 201 con revisión vigente y cambio posterior conserva bloque histórico |
      | cambiar disponibilidad | cambiar disponibilidad | creación 412 AVAILABILITY_CONFLICT sin bloque ni evento nuevo                  |
  @s32
  Scenario: No bloquear reservas de usuarios independientes
    Given PostgreSQL real y creación de persona-a retenida bajo su coordinación de disponibilidad
    When persona-b crea un bloque propio en el mismo intervalo
    Then persona-b recibe 201 sin esperar la liberación de persona-a
    And no se consulta ni divulga capacidad de persona-a

  @s33
  Scenario: Revisar un snapshot coherente durante escrituras concurrentes
    Given PostgreSQL real y un cambio concurrente de presupuesto o reservas
    When solicito preview
    Then availabilityEtag, budgetZoneId, days y plannedSeconds corresponden a un único estado coherente
    And preview no modifica entidades, preferencias ni outbox

  @s34
  Scenario Outline: Revertir bloque y evento ante cualquier fallo anterior al commit
    Given PostgreSQL real y <fallo>
    When intento crear un bloque válido
    Then recibo 503 STORAGE_UNAVAILABLE sin SQL, trazas ni secretos
    And no queda bloque ni outbox ni intención confirmada recuperable
    And no cambian proyecto, tarea ni disponibilidad
    Examples:
      | fallo                                     |
      | falla la inserción del bloque             |
      | se suprime la inserción del bloque        |
      | falla la inserción de outbox              |
      | se suprime la inserción de outbox         |
      | se rechaza el commit antes de confirmarlo |

  @s35
  Scenario: Recuperar confirmación perdida y persistencia tras reinicio
    Given la creación confirmó bloque y outbox pero se perdió la respuesta
    And el backend se ha reiniciado
    When consulto by-request con la misma key
    Then recibo 200 con el DTO persistido de nueve campos correspondiente a la intención
    And el detalle y listado contienen ese bloque y existe una sola outbox

  @s36
  Scenario: Publicar BlockPlanned sin objetivo ni clave de recuperación
    Given un bloque confirmado y RabbitMQ real
    When se publica su outbox
    Then el evento tiene exactamente eventId, aggregateId, ownerId, occurredAt, schemaVersion, type, blockId, taskId, startAt, endAt, zoneId y durationMinutes
    And aggregateId es P, schemaVersion es 1 y type es "BlockPlanned.v1"
    And los IDs, instantes, zona y duración corresponden al bloque confirmado
    And no contiene objective ni requestKey ni Idempotency-Key
    And usa routing key "block.planned.v1" y cola quorum durable "organization.block-planned.v1"
    And las seis rutas anteriores conservan sus destinos y garantías

  @s37
  Scenario Outline: Validar y reintentar la séptima ruta con las garantías existentes
    Given outbox de BlockPlanned.v1 con <condicion>
    When el publicador intenta procesarla con RabbitMQ real
    Then <resultado>
    Examples:
      | condicion                                                | resultado                                                                       |
      | campo extra                                              | queda blocked con INVALID_EVENT y no se publica                                 |
      | payload sin eventId                                      | queda blocked con INVALID_EVENT y no se publica                                 |
      | payload sin aggregateId                                  | queda blocked con INVALID_EVENT y no se publica                                 |
      | payload sin ownerId                                      | queda blocked con INVALID_EVENT y no se publica                                 |
      | payload sin occurredAt                                   | queda blocked con INVALID_EVENT y no se publica                                 |
      | payload sin schemaVersion                                | queda blocked con INVALID_EVENT y no se publica                                 |
      | payload sin type                                         | queda blocked con INVALID_EVENT y no se publica                                 |
      | payload sin blockId                                      | queda blocked con INVALID_EVENT y no se publica                                 |
      | payload sin taskId                                       | queda blocked con INVALID_EVENT y no se publica                                 |
      | payload sin startAt                                      | queda blocked con INVALID_EVENT y no se publica                                 |
      | payload sin endAt                                        | queda blocked con INVALID_EVENT y no se publica                                 |
      | payload sin zoneId                                       | queda blocked con INVALID_EVENT y no se publica                                 |
      | payload sin durationMinutes                              | queda blocked con INVALID_EVENT y no se publica                                 |
      | tipo de envoltorio no soportado                          | queda blocked con UNSUPPORTED_EVENT y no se publica                             |
      | versión de envoltorio no soportada                       | queda blocked con UNSUPPORTED_EVENT y no se publica                             |
      | zona original retirada del catálogo después de confirmar | publica el evento histórico válido sin revalidar pertenencia al catálogo actual |
      | ID inválido                                              | queda blocked con INVALID_EVENT y no se publica                                 |
      | instantes no crecientes                                  | queda blocked con INVALID_EVENT y no se publica                                 |
      | duración incoherente con instantes                       | queda blocked con INVALID_EVENT y no se publica                                 |
      | duración fuera de 1 a 1440                               | queda blocked con INVALID_EVENT y no se publica                                 |
      | confirmación negativa o perdida                          | conserva evento y payload para reintento conforme a garantías previas           |
      | reintento después de fallo                               | conserva identidad y payload; la entrega puede repetirse                        |

  @s38
  Scenario: Abrir editor inline con controles de planificación explícitos
    Given el detalle confirmado de T pending y disponibilidad guardada Europe/Madrid
    When abro "Planificar bloque"
    Then veo objetivo, dos datetime-local con precisión de minutos y selector nativo de zona con Europe/Madrid inicial
    And veo "Revisar bloque" y "Guardar bloque" deshabilitado hasta una revisión válida
    And no se envía POST ni se guarda borrador en almacenamiento local por abrir o editar
    And la sección "Bloques planificados" distingue reservas de trabajo realizado

  @s39
  Scenario: Mostrar revisión comprensible antes de guardar
    Given el editor contiene una intención válida en zona distinta de la disponibilidad
    When pulso "Revisar bloque" y llega un preview válido correspondiente
    Then veo objetivo normalizado, instantes y offsets identificados, duración, zona del bloque y zona del presupuesto
    And veo las fechas y presupuesto, tiempo reservado, solicitado y exceso de days
    And "Guardar bloque" queda habilitado si no hay exceso
    And todavía no se crea ningún bloque

  @s40
  Scenario Outline: Rechazar preview incompatible aunque tenga HTTP 200
    Given el editor retiene una intención
    And la respuesta de preview tiene <defecto>
    When recibo esa respuesta
    Then no se muestra una revisión válida ni se habilita Guardar
    And se conserva el borrador y se comunica fallo de revisión
    Examples:
      | defecto                                                       |
      | campo requerido ausente                                       |
      | campo desconocido                                             |
      | tipo de campo incorrecto                                      |
      | objective distinto del objetivo normalizado                   |
      | zoneId distinta de la intención                               |
      | startAt distinto de startLocal menos startOffset              |
      | endAt distinto de endLocal menos endOffset                    |
      | offset resuelto distinto del offset explícito pedido          |
      | offset resuelto desde null sin correspondencia local/instante |
      | durationMinutes incoherente con instantes                     |
      | suma de requestedSeconds distinta de durationMinutes por 60   |
      | days desordenados o con fechas duplicadas                     |
      | excessSeconds incoherente con presupuesto y segundos          |
      | availabilityEtag ausente                                      |
      | availabilityEtag mal formado                                  |
      | days vacío para intervalo positivo                            |
      | plannedSeconds negativo                                       |
      | plannedSeconds no entero                                      |
      | requestedSeconds cero                                         |
      | requestedSeconds negativo                                     |
      | requestedSeconds no entero                                    |
      | excessSeconds negativo                                        |
      | excessSeconds no entero                                       |
      | budgetMinutes menor que 0                                     |
      | budgetMinutes mayor que 1440                                  |

  @s41
  Scenario: Elegir ocurrencias mediante opciones cerradas etiquetadas
    Given preview devolvió AMBIGUOUS_OFFSET en startOffset con [+02:00,+01:00]
    When selecciono la ocurrencia etiquetada +01:00
    Then startOffset queda explícitamente +01:00 y ningún control elige por mí el otro extremo
    And los campos locales se conservan y debo revisar antes de guardar

  @s42
  Scenario Outline: Invalidar revisión y consentimiento al editar
    Given una revisión válida y consentimiento de exceso marcado
    When cambio <campo>
    Then retiro revisión y consentimiento y Guardar queda deshabilitado
    And <offsets>
    Examples:
      | campo       | offsets                                   |
      | objective   | conservo offsets de los extremos          |
      | startLocal  | retiro startOffset                        |
      | endLocal    | retiro endOffset                          |
      | zoneId      | retiro ambos offsets                      |
      | startOffset | conservo sólo la nueva elección de inicio |
      | endOffset   | conservo sólo la nueva elección de fin    |

  @s43
  Scenario: Requerir aceptación explícita del bloque y explicar su alcance
    Given preview válido tiene exceso positivo
    When se muestra la revisión
    Then la aceptación está sin marcar y Guardar permanece deshabilitado
    And el texto explica que autoriza ese bloque concreto aunque aumente el exceso por otras reservas antes de guardar
    And no presenta el permiso como limitado sólo a la cifra mostrada ni como permiso de solape

  @s44
  Scenario: Preparar key sólo con el primer envío y conservar la intención exacta
    Given una revisión válida y consentimiento aplicable
    When pulso "Guardar bloque"
    Then se genera una key UUID canónica y se envía una sola creación con la petición exacta, offsets resueltos y Availability-Revision retenida
    And se bloquean campos y acciones duplicadas mientras crea
    And Cancelar sigue disponible y explica que cerrar no anula la petición transmitida

  @s45
  Scenario Outline: Tratar resultados no confirmados como inciertos
    Given una creación enviada con intención, key y Availability-Revision retenidas
    When llega <resultado>
    Then se conserva y bloquea la intención y se ofrece "Comprobar guardado"
    And no se genera otra key ni se reenvía automáticamente
    Examples:
      | resultado                              |
      | fallo de red                           |
      | 503                                    |
      | error con código desconocido           |
      | cuerpo inválido                        |
      | 201 con DTO distinto de la intención   |
      | 200 con DTO distinto de la intención   |
      | 200 con DTO de campos extra o ausentes |

  @s46
  Scenario Outline: Confirmar sólo DTO completo coincidente
    Given una intención retenida con contexto, objetivo, zona, instantes y duración conocidos
    When recibo <respuesta> con DTO válido de nueve campos coincidente
    Then se confirma el bloque visible y termina la incertidumbre
    And la key y el bloque se reconocen sin crear otro evento
    Examples:
      | respuesta                  |
      | 201 de creación            |
      | 200 de replay              |
      | 200 de consulta by-request |

  @s47
  Scenario Outline: Comprobar ausencia o fallo sin inferir rollback
    Given un resultado incierto con intención y key bloqueadas
    When "Comprobar guardado" devuelve <respuesta>
    Then se conserva intención, key, Availability-Revision y bloqueo
    And <accion>
    Examples:
      | respuesta                        | accion                                                   |
      | 404 BLOCK_NOT_FOUND              | ofrece "Reenviar el mismo bloque" o volver a comprobar   |
      | fallo de red                     | permite volver a comprobar sin crear otra key            |
      | 503                              | comunica fallo sin convertirlo en ausencia o lista vacía |
      | 200 con DTO ajeno a la intención | conserva incertidumbre y permite comprobar otra vez      |

  @s48
  Scenario: Reenviar manualmente la misma intención después de un 404
    Given by-request devolvió BLOCK_NOT_FOUND para una creación incierta
    When pulso "Reenviar el mismo bloque"
    Then se envía exactamente el mismo cuerpo, key y Availability-Revision
    And no se prepara una intención nueva ni se repite automáticamente

  @s49
  Scenario Outline: Corregir sólo tras rechazo definitivo reconocido
    Given una creación con borrador, preview y aceptación retenidos
    When recibo <error>
    Then se conserva el borrador, se retira preview y consentimiento y se habilita corregir y revisar
    And no se envía otra creación automáticamente
    And una intención nueva requiere una revisión nueva antes de guardar
    Examples:
      | error                                      |
      | 400 VALIDATION_ERROR reconocido            |
      | 409 AVAILABILITY_REQUIRED                  |
      | 409 AVAILABILITY_ZONE_UNAVAILABLE          |
      | 412 AVAILABILITY_CONFLICT                  |
      | 428 PRECONDITION_REQUIRED                  |
      | 409 PROJECT_COMPLETED                      |
      | 409 TASK_COMPLETED                         |
      | 409 BLOCK_OVERLAP                          |
      | 409 BUDGET_EXCEEDED sin permiso            |
      | 412 después de reenviar intención incierta |

  @s50
  Scenario: No regenerar key ante conflicto de idempotencia
    Given una creación incierta con intención y key retenidas
    When recibo 409 IDEMPOTENCY_CONFLICT
    Then se conservan bloqueo, intención y key y se permite consultar su resultado
    And no se prepara otra key automáticamente ni se trata como rechazo definitivo para corregir

  @s51
  Scenario: Renovar CSRF sólo mediante recuperación manual
    Given una creación devuelve un rechazo CSRF reconocido
    When decido renovar acceso y repetir manualmente
    Then se conserva la petición exacta, key y Availability-Revision
    And sólo tras esa decisión se repite la creación con protección renovada

  @s52
  Scenario Outline: Retirar datos ante pérdida de sesión o acceso
    Given la sección contiene lista, borrador o resultado incierto
    When recibo <respuesta>
    Then retiro los datos privados del contexto inaccesible y no envío otra creación automáticamente
    Examples:
      | respuesta              |
      | 401                    |
      | 404 RESOURCE_NOT_FOUND |

  @s53
  Scenario Outline: Descartar respuestas obsoletas sin reactivar estados
    Given una operación <operacion> está pendiente
    And <cambio>
    When llega su respuesta después de ese cambio
    Then la respuesta no reemplaza datos del contexto actual ni reactiva una revisión invalidada
    Examples:
      | operacion       | cambio                              |
      | revisión        | edité un campo                      |
      | revisión        | cerré y volví a abrir el editor     |
      | listado         | navegué a otra tarea                |
      | creación        | cerré el editor                     |
      | recuperación    | navegué a otra tarea                |
      | renovación CSRF | el contexto dejó de estar accesible |

  @s54
  Scenario Outline: Separar fallos de lista, revisión y confirmación
    Given <estado>
    When falla <operacion>
    Then <resultado>
    Examples:
      | estado                         | operacion | resultado                                                             |
      | borrador con revisión previa   | preview   | conserva campos, retira revisión y deshabilita Guardar                |
      | bloque ya confirmado visible   | listado   | conserva confirmación y muestra error de lista sin fingir lista vacía |
      | lista cargada y editor abierto | preview   | conserva lista y borrador                                             |

  @s55
  Scenario: Ofrecer configuración sin ocultar la pérdida del borrador
    Given preview devuelve AVAILABILITY_REQUIRED
    When se muestra la opción de configurar disponibilidad
    Then existe enlace para configurarla y aviso de que salir pierde el borrador
    And no se guardan preferencias automáticamente ni se añade una guardia global de navegación

  @s56
  Scenario Outline: Usar elegibilidad confirmada compartida sin perder recuperación
    Given <estadoEditor>
    When completar/reabrir o una consulta deliberada confirma <estadoContexto>
    Then <resultado>
    And la sección no hace otra lectura silenciosa para reconstruir ese contexto
    Examples:
      | estadoEditor       | estadoContexto     | resultado                                                      |
      | sin envío incierto | completed          | impide iniciar una creación nueva                              |
      | sin envío incierto | pending            | permite iniciar revisión y creación si el proyecto es elegible |
      | resultado incierto | completed          | conserva el editor, la intención y la recuperación disponibles |
      | resultado incierto | proyecto completed | conserva el editor, la intención y la recuperación disponibles |
      | sin envío incierto | proyecto completed | impide iniciar una creación nueva                              |

  @s57
  Scenario: Mostrar UTC explícito cuando Intl no reconoce una zona guardada
    Given una zona válida del backend no puede formatearse mediante Intl en el navegador
    When se muestra revisión o bloque confirmado de esa zona
    Then se presenta el instante UTC etiquetado y el ID original de zona
    And no se usa silenciosamente la zona del navegador ni se rechaza el catálogo del servidor

  @s58
  Scenario Outline: Mantener foco según la intención del usuario
    Given una acción asíncrona del editor se inició desde un control identificable
    And <situacion>
    When termina la acción
    Then <resultado>
    Examples:
      | situacion                                                               | resultado                                                            |
      | el usuario no eligió otro destino de foco                               | conserva o restaura el foco del control de origen si está disponible |
      | el usuario eligió otro destino disponible                               | conserva ese destino sin robar el foco                               |
      | el origen desapareció o quedó deshabilitado y no se eligió otro destino | lleva foco al encabezado Bloques planificados                        |

  @s59
  Scenario Outline: Conservar interfaz operable en la matriz responsive
    Given la pantalla mide <ancho> píxeles CSS y contiene objetivos largos y Unicode
    When recorro lista, editor, revisión, exceso, guardado y recuperación con teclado en vacío, carga, error y éxito
    Then no hay solapes, recortes ni desplazamiento horizontal de página
    And todos los controles conservan nombre accesible, foco visible, orden lógico y errores asociados
    And las acciones táctiles principales miden al menos 44 por 44 píxeles CSS y no dependen de hover
    Examples:
      | ancho |
      | 320   |
      | 360   |
      | 390   |
      | 480   |
      | 600   |
      | 768   |
      | 820   |
      | 1024  |
      | 1280  |
      | 1440  |
      | 1920  |
      | 2560  |

  @s60
  Scenario Outline: Verificar límites visuales y motores con evidencia explícita
    Given el recorrido de bloques está disponible en <entorno>
    When ejecuto revisión, creación y recuperación con <condicion>
    Then las funciones, información y orden lógico se conservan sin recortes ni acciones inaccesibles
    And la evidencia identifica entorno real probado sin extrapolar a dispositivos físicos o evaluación humana no realizados
    Examples:
      | entorno  | condicion                                               |
      | Chromium | zoom nativo 200 por ciento y reflow a 320 píxeles CSS   |
      | Chromium | texto ampliado y altura reducida/orientación horizontal |
      | Chromium | ambos lados de cada breakpoint de cualquier SCSS nuevo  |
      | Firefox  | recorrido de teclado y éxito/error                      |
      | WebKit   | recorrido de teclado y éxito/error                      |

  @s61
  Scenario: Revisar treinta principios UX con límites de evidencia
    Given los requisitos de docs/ux-requirements.md
    When se revisa la interfaz de bloques para aceptación
    Then la matriz tiene exactamente las treinta filas con aplicación, evidencia y resultado o motivo de no aplicabilidad
    And se mide feedback visible de espera con objetivo inferior a 400 ms sin fingir guardado
    And se comprueban contraste, anuncios, semántica y movimiento reducido del recorrido
    And dispositivos físicos, teclado virtual, lector real y facilidad de uso humana figuran pendientes si no se han probado

  @s62
  Scenario Outline: Mantener el error compartido de JSON ilegible
    Given petición de <operacion> con <defecto> y headers válidos
    When envío la petición
    Then recibo 400 MALFORMED_JSON con la forma compartida de ApiErrors y Cache-Control no-store
    And no hay escrituras
    Examples:
      | operacion | defecto          |
      | preview   | JSON duplicado   |
      | preview   | JSON mal formado |
      | creación  | JSON duplicado   |
      | creación  | JSON mal formado |




