Feature: Cerrar una sesión de trabajo y conservar su resultado declarado
  Como propietario quiero finalizar trabajo real y recuperar su cierre
  sin completar automáticamente la tarea ni confundirlo con otra sesión activa.

  # Normativa: project-spec.md, sección16. No aprobación TDD ni estado de feature en este archivo.
  # Se heredan seguridad, negociación, JSON estricto, UUID/key, no-store y errores14/15.
  # P = POST /api/v1/work-sessions/{sessionId}/close.
  # S = GET /api/v1/work-sessions/{sessionId}/state.
  # C = GET /api/v1/work-session-changes/{changeId}.
  # K = GET /api/v1/work-session-changes/by-request/{key}.
  # F = GET /api/v1/work-sessions/{sessionId}/closure.
  # A = GET /api/v1/work-sessions/active.
  # P usa Idempotency-Key y Work-Session-Revision completos de15, sin ETag ni If-Match.
  # SessionStart7, State6 y snapshot3 conservan sus campos15; closed es el estado terminal nuevo.
  # CLOSE7 = id,sessionId,action,occurredAt,before,after,closure.
  # closure4 = progressNote,nextStep,workDate,closeZoneId; PAUSE/RESUME conservan recibo6.
  # Trabajo/revisión se serializan como cadenas decimales canónicas, fechas UTC con precisión µs.

  Background:
    Given una identidad autenticada propia con seguridad y cabeceras válidas salvo el defecto indicado
    And no hay dos sesiones running o paused del mismo propietario
    And los hechos de inicio y cambios anteriores conservan sus contratos14/15

  @s1
  Scenario: Cerrar running suma exactamente el último tramo
    Given una sesión running de revisión3 con workedMicroseconds "59999999"
    And runningSince es "2026-09-07T10:00:00.123456Z" y la zona histórica es "UTC"
    And el reloj único de cierre es "2026-09-07T10:00:01.123457999Z"
    When confirmo P con notas "Avance parcial" y "Continuar la revisión" y revisión3
    Then recibo201 y Location del cambio propio con un recibo CLOSE7 cerrado
    And after es closed, revisión "4", runningSince null y workedMicroseconds "61000000"
    And occurredAt y after.changedAt son "2026-09-07T10:00:01.123457Z"
    And closure conserva ambas notas, workDate "2026-09-07" y closeZoneId "UTC"
    And sólo se añade el intervalo final de1000001 microsegundos, un recibo y un evento
    And SessionStart, tarea, proyecto, planificación y fin previsto permanecen iguales

  @s2
  Scenario: Cerrar paused conserva el acumulado y excluye todo el descanso
    Given una sesión paused de revisión2 con workedMicroseconds "60000001"
    And han pasado dos días desde changedAt
    When confirmo P con revisión2
    Then recibo201 con after closed, revisión "3" y workedMicroseconds "60000001"
    And runningSince es null y no se añade un intervalo por el tiempo pausado
    And se conserva el fin previsto original

  @s3
  Scenario Outline: Admitir cierre sin tiempo adicional ni avance inventado
    Given una sesión <status> con acumulado cero y reloj igual a changedAt
    When confirmo P con revisión vigente y objeto vacío
    Then recibo201 con tiempo final "0" y notas vacías
    And se añaden <intervals> intervalos y no se completa la tarea
    Examples:
      | status  | intervals |
      | running | 1         |
      | paused  | 0         |

  @s4
  Scenario Outline: Normalizar sólo ausencia y null y preservar texto Unicode válido
    Given una sesión abierta y <notes> en ambos campos de P
    When confirmo el cierre
    Then recibo201 con <stored> en ambas notas
    And los textos no se recortan ni se normalizan silenciosamente
    Examples:
      | notes                                  | stored                           |
      | propiedades ausentes                   | cadenas vacías                   |
      | null                                   | cadenas vacías                   |
      | cadena vacía                           | cadenas vacías                   |
      | espacios exteriores y salto de línea   | el contenido exacto recibido     |
      | 2000 emoji como pares UTF-16 válidos    | los mismos2000 puntos de código  |

  @s5
  Scenario Outline: Rechazar notas no almacenables antes de propiedad y replay
    Given <field> contiene <value> en un JSON sintácticamente válido
    And la key ya fue confirmada y el contexto solicitado es ajeno
    When envío P
    Then recibo400 VALIDATION_ERROR con error del campo <field> y code INVALID_VALUE
    And no se consulta propiedad ni se recupera un recibo ni se escribe
    Examples:
      | field        | value                           |
      | progressNote | un número                       |
      | nextStep     | un array                        |
      | progressNote | 2001 puntos de código           |
      | nextStep     | 2001 emoji                      |
      | progressNote | U+0000 decodificado              |
      | nextStep     | un surrogate alto aislado       |
      | progressNote | un surrogate bajo aislado       |

  @s6
  Scenario Outline: Mantener precedencia de forma y seguridad en las rutas nuevas
    Given la petición <route> presenta <defects>
    When completo la petición
    Then obtengo <result> sin escritura ni recibo ajeno
    Examples:
      | route | defects                                                | result                                      |
      | P     | anonimato y nota inválida                               | 401 de sesión heredado                      |
      | P     | CSRF inválido autenticado y nota inválida                | 403 CSRF_INVALID                            |
      | P     | query desconocida e UUID inválido                       | 400 VALIDATION_ERROR de query               |
      | F     | query desconocida e UUID inválido                       | 400 VALIDATION_ERROR de query               |
      | P     | cabecera de revisión ausente y JSON malformado           | 428 PRECONDITION_REQUIRED                   |
      | P     | token sintácticamente inválido y JSON malformado         | 400 VALIDATION_ERROR de la cabecera         |
      | P     | carácter de control literal que invalida JSON            | 400 MALFORMED_JSON                          |
      | P     | documentos JSON concatenados                            | 400 MALFORMED_JSON                          |
      | P     | raíz array o propiedad de sistema desconocida           | 400 VALIDATION_ERROR                        |
    # Tipos de contenido, origen, duplicados, UUID/key y tokens completos restantes remiten a14/15.

  @s7
  Scenario Outline: Rechazar reloj de cierre inválido sin liberar plaza
    Given una sesión abierta compatible y reloj <clock>
    When envío P con revisión vigente
    Then recibo409 WORK_SESSION_TIME_OUT_OF_RANGE con el título temporal15
    And estado, intervalos, recibos y eventos quedan intactos y A conserva la sesión
    Examples:
      | clock                             |
      | un microsegundo antes de changedAt |
      | año UTC0000                       |
      | año UTC10000                      |

  @s8
  Scenario: Mantener precisión más allá de Number y del rango de nanosegundos Long
    Given una sesión running iniciada en "0001-01-01T00:00:00Z" con zona UTC y acumulado cero
    When cierro en "9999-12-31T23:59:59.999999Z"
    Then el tiempo final y el recibo contienen "315537897599999999" microsegundos exactos
    And el cliente conserva ese entero sin redondearlo ni rechazarlo por superar1440 minutos

  @s9
  Scenario Outline: Fijar día local de cierre una sola vez con zona histórica
    Given la zona del inicio es <zone> y closedAt es "2026-09-07T22:30:00Z"
    And la preferencia actual contiene otra zona
    When confirmo P
    Then closure contiene workDate <day> y closeZoneId <usedZone>
    And SessionStart.zoneId no cambia ni se consulta la preferencia para atribuir el cierre
    Examples:
      | zone                                   | day        | usedZone      |
      | Europe/Madrid resoluble                | 2026-09-08 | Europe/Madrid |
      | zona histórica ya no resoluble         | 2026-09-07 | UTC           |

  @s10
  Scenario Outline: No disfrazar desbordamiento local como fallback UTC
    Given zona histórica válida <zone> y un reloj UTC representable <clock>
    When confirmo P desde un estado temporalmente compatible
    Then recibo409 WORK_SESSION_TIME_OUT_OF_RANGE sin escrituras ni fallback UTC
    Examples:
      | zone       | clock                        |
      | Etc/GMT+12 | 0001-01-01T00:01:00Z          |
      | Etc/GMT-14 | 9999-12-31T23:59:59.999999Z   |

  @s11
  Scenario: Preservar atribución y notas frente a preferencias y catálogos posteriores
    Given un cierre confirmado con workDate y closeZoneId persistidos
    And cambiaron preferencias y las reglas de zona disponibles después de confirmar
    When recupero el cierre por F
    Then recibo exactamente el hecho original con sus notas y atribución
    And no se reparte su tiempo entre días ni se recalcula la fecha histórica

  @s12
  Scenario Outline: Conservar precedencia de propiedad, token y replay
    Given una key de cierre confirmada y <setup>
    When envío P
    Then obtengo <result> sin revelar otro recibo ni modificar hechos
    Examples:
      | setup                                                | result                              |
      | sesión ajena y token de otra identidad                | 404 WORK_SESSION_NOT_FOUND          |
      | sesión propia y token válido de otra identidad        | 412 PRECONDITION_FAILED             |
      | identidad correcta y misma intención histórica        | 200 con mismo recibo y Location     |
      | identidad correcta y nota normalizada distinta        | 409 IDEMPOTENCY_CONFLICT            |

  @s13
  Scenario Outline: El cierre es terminal para nuevas decisiones
    Given una sesión closed con revisión vigente y una key nueva
    When envío <action> con <revision>
    Then recibo <result> y no se modifica ni se reabre la sesión
    Examples:
      | action | revision | result                             |
      | CLOSE  | vigente  | 409 WORK_SESSION_STATE_CONFLICT     |
      | PAUSE  | vigente  | 409 WORK_SESSION_STATE_CONFLICT     |
      | RESUME | vigente  | 409 WORK_SESSION_STATE_CONFLICT     |
      | CLOSE  | obsoleta | 412 PRECONDITION_FAILED             |

  @s14
  Scenario: Comprobar agotamiento de revisión antes del reloj
    Given una sesión abierta con revisión máxima BIGINT y un reloj que fallaría
    When envío P con esa revisión
    Then recibo409 WORK_SESSION_REVISION_EXHAUSTED sin capturar reloj ni escribir

  @s15
  Scenario: Recuperar todos los hechos anteriores después de cerrar e iniciar otra sesión
    Given A tuvo inicio14, PAUSE, RESUME y CLOSE confirmados con sus keys originales
    And después se inició una sesión B propia que sigue abierta
    When reenvío cada intención original de A con sus datos normalizados
    Then cada replay devuelve200 con su recibo y Location históricos exactos
    And PAUSE y RESUME conservan recibo6, CLOSE conserva recibo7 e inicio conserva DTO7
    And ninguna operación altera B ni consulta elegibilidad o reloj para reinterpretar A

  @s16
  Scenario Outline: La intención de cierre incluye notas y comparte namespace de cambios15
    Given una key confirmada por un cambio de sesión
    When reutilizo esa key propia cambiando <part>
    Then recibo409 IDEMPOTENCY_CONFLICT sin devolver otro cambio como éxito
    Examples:
      | part                              |
      | PAUSE por CLOSE                   |
      | CLOSE por RESUME                  |
      | revisión esperada de CLOSE        |
      | progressNote de CLOSE             |
      | nextStep de CLOSE                 |
    # Ausencia/null/vacío normalizados son la misma intención; no se recortan espacios.

  @s17
  Scenario: Una key de sesión cerrada no puede consumirse en una sesión nueva
    Given A está cerrada con una key de cambio confirmada y B es la única sesión abierta propia
    When intento cerrar B usando esa key de A y revisión vigente de B
    Then recibo409 IDEMPOTENCY_CONFLICT y B sigue abierta
    And el recibo de A permanece recuperable sin modificarlo

  @s18
  Scenario Outline: Ordenar decisiones simultáneas sobre la misma revisión
    Given dos peticiones alcanzan simultáneamente la misma sesión abierta y revisión
    When se resuelve la pareja <requests>
    Then obtengo <results> con una sola transición y su evento
    And la petición perdedora no se aplica sobre la revisión recién confirmada
    Examples:
      | requests                              | results |
      | CLOSE idéntico con misma key           | 201/200 |
      | CLOSE con keys distintas              | 201/412 |
      | CLOSE y PAUSE sobre running           | 201/412 |
      | CLOSE y RESUME sobre paused           | 201/412 |

  @s19
  Scenario Outline: Liberar plaza sólo al confirmar cierre
    Given la sesión A es la única abierta del propietario y un nuevo inicio B está preparado
    When el cierre de A <outcome> mientras compite el inicio B
    Then <result>
    And nunca hay dos sesiones abiertas confirmadas del propietario
    Examples:
      | outcome                   | result                                                               |
      | confirma                  | B confirma después del cierre o rechaza por A todavía abierta        |
      | revierte antes del commit | A conserva plaza y B no confirma un segundo inicio                    |
    # No se exige ganador preferente; un inicio deliberado posterior al cierre confirmado puede prosperar.

  @s20
  Scenario: Cerrar no depende de otros propietarios ni de la elegibilidad de inicio
    Given proyecto y tarea propios están completed, no hay disponibilidad y la sesión sigue abierta
    And otro propietario mantiene un comando pendiente
    When cierro la sesión propia
    Then recibo201 sin esperar el comando ajeno y sin alterar proyecto ni tarea
    And el fin previsto y la zona original de inicio se conservan

  @s21
  Scenario Outline: Conservar atomicidad del cierre y su plaza ante fallos
    Given una sesión running y <failure> durante P
    When completo la petición
    Then recibo503 STORAGE_UNAVAILABLE sin confirmar cierre
    And estado, revisión, intervalos, recibos y outbox anteriores permanecen iguales
    And la sesión sigue ocupando su plaza
    Examples:
      | failure                             |
      | supresión de escritura de estado     |
      | supresión del último intervalo       |
      | supresión de recibo sin ganador      |
      | supresión de outbox                  |
      | error de SQL                         |
      | fallo al finalizar la transacción    |

  @s22
  Scenario: Resolver colisión esperada con ganador durable después de rollback
    Given el cierre alcanza una colisión owner/key con un recibo ganador durable
    When se resuelve la intención tras revertir la transacción fallida
    Then una nueva lectura compara sesión, acción, revisión y notas normalizadas
    And sólo intención idéntica devuelve200 con el recibo original y Location
    And otra intención devuelve409 y ausencia de ganador503, sin clasificar otras restricciones como replay

  @s23
  Scenario: Migrar sin reescribir inicios, intervalos ni recibos14/15
    Given existen sesiones running y paused de propietarios distintos con sus hechos14/15 durables
    When se aplica la migración aditiva de la feature16, posterior a V16
    Then todos los hechos y keys anteriores conservan sus valores
    And cada sesión sigue consultable y puede cerrarse según su estado
    And ninguna lectura materializa un cierre ni libera una plaza

  @s24
  Scenario Outline: Consultar snapshot terminal con neto fijo y reloj de lectura válido
    Given una sesión closed con workedMicroseconds "60000001"
    When consulto S con reloj <clock>
    Then obtengo <result> sin escrituras
    And no se reabre un intervalo ni cambia la revisión de negocio
    Examples:
      | clock                         | result                                     |
      | posterior a changedAt          | 200 netMicroseconds "60000001"              |
      | anterior a changedAt           | 200 netMicroseconds "60000001"              |
      | año UTC0000                    | 409 WORK_SESSION_TIME_OUT_OF_RANGE          |
      | año UTC10000                   | 409 WORK_SESSION_TIME_OUT_OF_RANGE          |

  @s25
  Scenario: El snapshot anterior al cierre no incorpora parcialmente su resultado
    Given S fija un snapshot running antes de que un cierre concurrente confirme
    When termina esa lectura después de confirmar el cierre
    Then devuelve el snapshot y neto coherentes con su propia captura de reloj
    And una lectura posterior ve closed con neto final fijo, sin escrituras por GET

  @s26
  Scenario Outline: Recuperar por sesión sin conocer key o changeId y sin reloj
    Given <state> para la sesión solicitada y un reloj que fallaría
    When consulto F
    Then recibo <result> sin capturar reloj ni escribir
    Examples:
      | state                         | result                                         |
      | propia closed                 | 200 con el mismo recibo CLOSE7 y sus notas      |
      | propia running                | 404 WORK_SESSION_CHANGE_NOT_FOUND              |
      | propia paused                 | 404 WORK_SESSION_CHANGE_NOT_FOUND              |
      | ajena                         | 404 WORK_SESSION_NOT_FOUND                     |
      | inexistente                   | 404 WORK_SESSION_NOT_FOUND                     |

  @s27
  Scenario Outline: Un fallo de consulta de cierre no significa sesión abierta ni ausencia
    Given ocurre <failure> al consultar F
    When termina la petición
    Then recibo503 STORAGE_UNAVAILABLE sin recibo ficticio ni escritura
    Examples:
      | failure                           |
      | error de almacenamiento           |
      | fallo al finalizar lectura         |

  @s28
  Scenario Outline: Recuperar el cierre durable tras pérdida de respuesta y publicación real
    Given P confirmó, se perdió su respuesta y la API se reinició
    And el evento se publicó realmente en Rabbit y se retiró del outbox
    When recupero el cierre por <route>
    Then obtengo200 con notas, atribución y recibo originales
    And no se añade cierre, intervalo ni evento y A no devuelve la sesión cerrada
    Examples:
      | route |
      | C     |
      | K     |
      | F     |

  @s29
  Scenario Outline: Publicar cierre sin cambiar eventos15 ni depender del broker en HTTP
    Given un cierre durable y <broker>
    When actúa el publicador existente
    Then <result>
    And el evento WorkSessionClosed.v1 tiene exactamente once campos normados y ninguna nota
    And usa aggregateId de sesión, revisión/neto como strings y atribución idéntica al recibo
    And WorkSessionStateChanged.v1 y sus rutas anteriores conservan su contrato
    Examples:
      | broker                        | result                                                        |
      | disponible                    | llega a la cola quorum organization.work-session-closed.v1    |
      | caído y después recuperado    | HTTP ya confirmado persiste y se publica tras retry elegible |

  @s30
  Scenario Outline: Rechazar datos de cierre incompatibles antes de presentar un hecho
    Given el cliente recibe <invalid>
    When valida el recurso de cierre o estado
    Then presenta error recuperable sin anunciar éxito ni habilitar otra decisión desde esos datos
    Examples:
      | invalid                                                     |
      | CLOSE sin closure o con un campo extra                      |
      | PAUSE o RESUME con un séptimo campo closure                  |
      | notas null, demasiado largas, NUL o surrogate aislado         |
      | workDate no válida o closeZoneId no string                   |
      | after no closed, identidad distinta o revisión no siguiente |
      | suma final incompatible con el intervalo running             |
      | before paused y after closed con runningSince null, sumando descanso        |
      | estado closed con snapshot neto distinto del acumulado       |
    # No comparar workDate persistido con Intl/TZDB ni recalcular atribución histórica.

  @s31
  Scenario: Mostrar la atribución persistida aunque el navegador no resuelva la zona
    Given un recibo válido con workDate persistido que difiere de la fecha calculada por el navegador
    And su zona de presentación no es resoluble por Intl
    When muestro el cierre
    Then conservo workDate y closeZoneId del recibo y etiqueto el fallback visual UTC
    And no rechazo el recibo ni presento su día atribuido como reparto de todos los intervalos

  @s32
  Scenario: Abrir formulario establece identidad recuperable antes de enviar
    Given un snapshot running o paused confirmado en su tarea propia
    When abro "Cerrar sesión de trabajo"
    Then la URL ya es /proyectos/{projectId}/tareas/{taskId}/sesiones/{sessionId} antes de cualquier POST
    And veo contexto, fin previsto y los dos campos opcionales etiquetados sin porcentaje ni completar tarea
    And "Volver" permite salir sin escribir y sólo "Confirmar cierre" envía P

  @s33
  Scenario: Recargar después de perder la respuesta recupera cierre y notas por la URL
    Given se estableció la URL de sesión antes de P y se perdió su respuesta confirmada
    And la recarga perdió key y changeId en memoria y A devuelve null
    When abro de nuevo esa URL
    Then consulto S y F por sessionId y muestro el cierre real con sus notas
    And verifico coincidencia de proyecto, tarea y sesión antes de mostrarlo
    And no atribuyo ese hecho a una intención de navegador perdida ni uso almacenamiento privado local

  @s34
  Scenario Outline: Distinguir cierre confirmado de la consulta de otra activa
    Given recibí un cierre confirmado y la actualización de A devuelve <active>
    When presento el resultado
    Then <result> y conservo el recibo de cierre y sus notas
    Examples:
      | active                       | result                                                     |
      | null válido                  | permito volver al flujo de inicio14                        |
      | otra sesión propia válida    | la muestro y enlazo separadamente                           |
      | fallo o respuesta inválida   | ofrezco reintentar lectura sin habilitar inicio por ausencia |

  @s35
  Scenario Outline: Conservar intención incierta y separar comprobar de reenviar
    Given P conserva key, revisión y notas y obtiene <result>
    When recupero el resultado mediante "Comprobar cierre"
    Then sólo consulto K y conservo la intención original sin POST automático
    And sólo un404 reconocido permite consulta o reenvío manual idéntico, sin probar rollback del POST en vuelo
    And un estado closed por sí solo no confirma aquella intención ni sus notas
    Examples:
      | result                         |
      | error de red                   |
      | 503                            |
      | éxito JSON incompatible        |
      | error desconocido              |
      | IDEMPOTENCY_CONFLICT            |

  @s36
  Scenario Outline: Una revisión rechazada conserva textos y exige nueva confirmación
    Given P devuelve412 con notas de borrador conservadas
    When una consulta deliberada de S devuelve <state>
    Then <result> sin reenviar automáticamente
    Examples:
      | state                         | result                                                           |
      | running o paused válido        | permito nueva confirmación manual con key y revisión nuevas       |
      | closed válido                 | recupero cierre por F sin enviar aquel borrador                    |
      | fallo o dato incompatible      | conservo textos sin habilitar nueva confirmación                   |

  @s37
  Scenario: Renovar acceso no reenvía el cierre pendiente
    Given P recibió CSRF_INVALID y conserva su intención
    When recupero acceso mediante el flujo global existente
    Then se renueva la sesión sin POST de cierre automático
    And un reenvío posterior separado conserva cuerpo, key y revisión originales

  @s38
  Scenario Outline: Descartar resultados privados obsoletos en cada espera
    Given cambió ruta o sesión mientras esperaba <stage>
    When llega <response>
    Then no restaura datos ni notas ni activa anterior y no revoca acceso vigente
    Examples:
      | stage                          | response                              |
      | entrega HTTP todavía pendiente | 401 obsoleto                          |
      | JSON de éxito pendiente        | 200 con JSON tardío                   |
      | clasificación de error         | error no401 clasificado tarde         |
    # Abortar antes del observer de401; nunca simular401 pendiente dentro de JSON ya recibido.

  @s39
  Scenario: Un GET anterior no sustituye el cierre ni la nueva sesión activa
    Given S y A anteriores están pendientes cuando P confirma y nuevas lecturas terminan
    When llegan aquellos GET antiguos del mismo contexto
    Then no restauran running de la sesión cerrada ni sustituyen la activa nueva por la anterior
    And el cierre histórico sigue visible con sus notas

  @s40
  Scenario Outline: Conservar foco y anunciar trabajo pendiente sin robar otro control
    Given el usuario activó <control> y la petición sigue pendiente
    When la interfaz muestra el estado de carga
    Then anuncia la espera antes de400ms y conserva el foco en el control si permanece
    And si desaparece tras la respuesta lleva el foco al encabezado sólo si seguía dentro del iniciador
    And no roba foco que el usuario movió a otro control
    Examples:
      | control          |
      | Confirmar cierre |
      | Comprobar cierre |
      | Reintentar lectura |

  @s41
  Scenario: Verificar el recorrido accesible sin ampliar a avisos o historial global
    Given la matriz de30 principios UX y los tamaños, zoom y motores acordados
    When recorro apertura, cierre, incertidumbre, recuperación por URL y nueva sesión con teclado y controles táctiles
    Then se conservan etiquetas, foco visible, áreas44x44, notas largas legibles y acciones sin scroll horizontal
    And la evidencia distingue motores probados de dispositivos físicos no certificados
    And no aparecen cierre automático, ampliación17, lista histórica18, progreso calculado ni tarea completada
