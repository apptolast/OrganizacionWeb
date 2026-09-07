Feature: Consultar hechos propios sin convertir el historial en estado o progreso
  Como propietario quiero recorrer hechos de planificación, tareas y sesiones
  con filtros y páginas explícitas, conservando su contenido histórico y privacidad.

  # Normativa18: project-spec.md SHA D8A4C5B274832563B67EDF988B6733760107EDF70563191A73195B6582FE7245.
  # H = GET /api/v1/history; identidad global = (type,id).
  # Rangos: BLOCK_PLANNED0, BLOCK_CHANGED1, TASK_STATUS_CHANGED2,
  # SESSION_STARTED3, SESSION_CHANGED4; orden DESC por occurredAt,rango,id.
  # Se reutilizan DTO cerrados9/11/13/14–17 y sus validaciones de aritmética/Unicode.
  # Los ejemplos de privacidad/forma siguientes conectan esos contratos a H,
  # sin sustituir ni duplicar íntegramente sus matrices.

  Background:
    Given una identidad autenticada con seguridad válida salvo el defecto indicado
    And los hechos y contextos de partida cumplen sus contratos vigentes salvo el defecto indicado

  @s1
  Scenario Outline: Cada fuente aporta su hecho y detalle original
    Given existe un hecho propio de <fuente> con ID F y tiempo de origen T
    When consulto H sin filtros
    Then aparece una entrada de type <type> con id F y occurredAt T
    And details conserva exactamente <detalle>
    And la entrada tiene sólo id,type,occurredAt,projectId,projectName,taskId,taskTitle,details
    Examples:
      | fuente                          | type                | detalle                              |
      | reserva creada                  | BLOCK_PLANNED       | BlockResponse9 original              |
      | bloque reprogramado             | BLOCK_CHANGED       | ReceiptResponse7 RESCHEDULED          |
      | bloque cancelado                | BLOCK_CHANGED       | ReceiptResponse7 CANCELLED            |
      | tarea finalizada                | TASK_STATUS_CHANGED | HistoryEntry4 pending a completed     |
      | tarea reabierta                  | TASK_STATUS_CHANGED | HistoryEntry4 completed a pending     |
      | inicio de sesión                | SESSION_STARTED     | SessionStart7                        |
      | pausa de sesión                 | SESSION_CHANGED     | PAUSE6                               |
      | reanudación de sesión           | SESSION_CHANGED     | RESUME6                              |
      | cierre de sesión                | SESSION_CHANGED     | CLOSE7 con closure4                  |
      | ampliación de sesión            | SESSION_CHANGED     | EXTEND7 con extension3               |

  @s2
  Scenario: Las proyecciones y el transporte no multiplican los hechos
    Given una reserva original fue reprogramada y cancelada con dos recibos
    And una sesión iniciada tiene recibos PAUSE,RESUME,EXTEND,CLOSE y sus intervalos
    And una tarea fue finalizada y reabierta y su proyecto está completed
    And outbox conserva eventos correspondientes a esos hechos
    When consulto H
    Then obtengo exactamente diez entradas correspondientes a las diez filas históricas
    And no se añaden hechos por proyección actual, intervalos, outbox o finalización del proyecto
    And reserva e inicio conservan sus datos originales aunque su estado actual haya cambiado

  @s3
  Scenario Outline: El aislamiento se aplica a cada familia y a su contexto
    Given A y B tienen hechos de <type> con proyectos y tareas distintos
    When A consulta H
    Then sólo recibe sus hechos y etiquetas de sus propios proyectos y tareas
    And ninguna entrada ni detalle contiene IDs, nombres, notas o claves de B
    Examples:
      | type                |
      | BLOCK_PLANNED       |
      | BLOCK_CHANGED       |
      | TASK_STATUS_CHANGED |
      | SESSION_STARTED     |
      | SESSION_CHANGED     |

  @s4
  Scenario: Los UUID coincidentes entre familias siguen siendo dos hechos
    Given una reserva y un inicio propios comparten el UUID "12345678-1234-1234-1234-123456789abc"
    And ambos ocurrieron en "2026-09-07T10:00:00.123456Z"
    When consulto H
    Then recibo dos entradas distintas, SESSION_STARTED seguida de BLOCK_PLANNED
    And ninguna se elimina por deduplicar sólo id

  @s5
  Scenario: Las etiquetas actuales no reescriben tiempo ni detalle
    Given un hecho propio guardó su detalle cuando la tarea se titulaba "Borrador"
    And la tarea ahora se titula "Publicar" y el proyecto se llama "Entrega"
    When consulto H
    Then taskTitle es "Publicar" y projectName es "Entrega" como etiquetas actuales
    And id,occurredAt y details son los originales y el orden no depende de esas etiquetas

  @s6
  Scenario Outline: Paginar veinte hechos sin exponer agregados
    Given existen <cantidad> hechos propios que cumplen los filtros
    When consulto la primera página H
    Then recibo 200 application/json con exactamente items y nextCursor
    And items contiene <entregados> entradas y nextCursor es <cursor>
    And Cache-Control es no-store y no hay total, resumen, ETag, Location, owner, requestKey ni payload de outbox
    Examples:
      | cantidad | entregados | cursor      |
      | 0        | 0          | null        |
      | 1        | 1          | null        |
      | 20       | 20         | null        |
      | 21       | 20         | una cadena  |

  @s7
  Scenario Outline: Las categorías seleccionan familias y no estados actuales
    Given existen hechos propios de las cinco familias, incluidas tarea reabierta y sesión cerrada
    When consulto H con category <category>
    Then sólo aparecen <familias>
    And se conservan los hechos anteriores aunque la tarea o sesión ya haya cambiado de estado
    Examples:
      | category    | familias                              |
      | sessions    | SESSION_STARTED y SESSION_CHANGED     |
      | task-status | TASK_STATUS_CHANGED                   |
      | planning    | BLOCK_PLANNED y BLOCK_CHANGED         |

  @s8
  Scenario Outline: Filtrar contexto propio incluso sin hechos o completed
    Given el contexto <contexto> pertenece a la identidad autenticada
    And hay <hechos> hechos en ese contexto y otros fuera de él
    When consulto H con sus IDs de proyecto y de tarea cuando corresponde
    Then recibo 200 con exactamente <hechos> entradas del contexto seleccionado
    Examples:
      | contexto                     | hechos |
      | proyecto en estado idea      | 2      |
      | tarea de proyecto completed  | 1      |
      | tarea completed              | 1      |
      | proyecto sin hechos          | 0      |
      | tarea sin hechos             | 0      |

  @s9
  Scenario Outline: Contextos ajenos y ausentes son indistinguibles
    Given el filtro señala <contexto> con sintaxis válida
    When consulto H sin cursor
    Then recibo 404 RESOURCE_NOT_FOUND con el mismo cuerpo público de ausencia
    And no recibo página vacía ni etiquetas o detalles del contexto
    Examples:
      | contexto                          |
      | proyecto de otro propietario      |
      | proyecto inexistente              |
      | tarea de otro proyecto propio     |
      | tarea inexistente en proyecto propio |

  @s10
  Scenario: Las fechas inclusivas usan el instante UTC del hecho
    Given from y to son "2026-09-07"
    And hay hechos a las 00:00:00Z y 23:59:59.999999Z de ese día y uno a cada lado
    And uno de los incluidos reserva un bloque para octubre y otro es CLOSE con workDate "2026-09-08"
    When consulto H con esas fechas
    Then recibo sólo los dos hechos de la fecha UTC seleccionada
    And el inicio planificado del bloque y workDate del cierre no cambian el filtro

  @s11
  Scenario Outline: Fechas extremas y límites opcionales se representan sin desbordamiento
    Given existe un hecho propio en <instante>
    When consulto H con <filtros>
    Then recibo ese hecho con su instante exacto y sin error de rango
    Examples:
      | instante                    | filtros                          |
      | 0001-01-01T00:00:00Z         | from=0001-01-01                  |
      | 9999-12-31T23:59:59.999999Z   | to=9999-12-31                    |
      | 2024-02-29T12:00:00.123456Z   | from=2024-02-29&to=2024-02-29     |
      | 2026-09-07T10:00:00Z         | to=2026-09-07                    |

  @s12
  Scenario Outline: Rechazar filtros mal formados antes de leer hechos
    Given H recibe <defecto> y el almacenamiento no está disponible
    When envío la consulta
    Then recibo 400 VALIDATION_ERROR con campo <campo> y código <codigo>
    And no recibo STORAGE_UNAVAILABLE
    Examples:
      | defecto                         | campo     | codigo         |
      | parámetro limit=20              | query     | INVALID_VALUE  |
      | category=Sessions               | category  | INVALID_VALUE  |
      | category vacío                  | category  | INVALID_VALUE  |
      | category repetido               | category  | INVALID_VALUE  |
      | projectId=abc                   | projectId | INVALID_FORMAT |
      | taskId UUID sin projectId       | taskId    | INVALID_VALUE  |
      | from=2026-02-30                 | from      | INVALID_VALUE  |
      | to=0000-01-01                   | to        | INVALID_VALUE  |
      | to=10000-01-01                  | to        | INVALID_VALUE  |
      | from=2026-09-08&to=2026-09-07    | to        | INVALID_VALUE  |
      | from vacío                      | from      | INVALID_VALUE  |
      | cursor repetido                 | cursor    | INVALID_VALUE  |

  @s13
  Scenario: Desempatar por familia y UUID sin atribuir causalidad a revisiones
    Given todos los hechos tienen occurredAt "2026-09-07T10:00:00.123456Z"
    And hay dos SESSION_CHANGED de la misma sesión y una entrada de cada una de las otras cuatro familias, seis hechos en total
    And las decisiones tienen revisiones after "2" y "3" con IDs terminados en "0002" y "0001" respectivamente
    When consulto H
    Then primero aparecen SESSION_CHANGED revisión2 y revisión3, seguidos de SESSION_STARTED,TASK_STATUS_CHANGED,BLOCK_CHANGED,BLOCK_PLANNED
    And el orden de IDs coincide con UUID canónico descendente, no con la revisión local

  @s14
  Scenario: Continuar una página conserva frontera y filtros
    Given hay 21 hechos propios con filtros category=sessions y un projectId escrito en mayúsculas
    And la primera página ya entregó veinte hechos en orden total
    When sigo su nextCursor con los mismos filtros de contexto
    Then recibo sólo el hecho restante y nextCursor null
    And el cursor inicial contiene version1,owner,filters,upper,after y ninguna otra clave
    And filters tiene exactamente category,projectId,taskId,from,to normalizados o null
    And upper corresponde a la mayor clave inicial y after al último hecho entregado, no al candidato21
    And cada frontera contiene sólo occurredAt,type,id y UUID canónico en minúsculas

  @s15
  Scenario Outline: Rechazar sintaxis de cursor sin consultar propiedad del contexto
    Given el filtro projectId es ajeno y el cursor tiene <defecto>
    When consulto H
    Then recibo 400 VALIDATION_ERROR campo cursor INVALID_VALUE antes de404
    Examples:
      | defecto                                  |
      | Base64URL con padding                     |
      | codificación Base64URL no canónica         |
      | JSON con clave duplicada                  |
      | JSON con basura final                     |
      | campo superior adicional                  |
      | version2                                  |
      | filters sin to                            |
      | frontera con tipo desconocido              |
      | frontera con UUID inválido                 |
      | frontera con instante de año10000          |
      | frontera con precisión de nanosegundos     |
      | cursor vacío                              |

  @s16
  Scenario Outline: La propiedad precede al vínculo de un cursor sintácticamente válido
    Given el filtro projectId señala un contexto ajeno
    And el cursor tiene sintaxis válida pero <incompatibilidad>
    When consulto H
    Then recibo 404 RESOURCE_NOT_FOUND antes de400 cursor
    Examples:
      | incompatibilidad                          |
      | owner corresponde a otra identidad        |
      | filters no coincide con category actual   |
      | upper es menor que after                  |

  @s17
  Scenario Outline: Vincular cursor y consulta propia antes de obtener una página
    Given el contexto explícito es propio y el cursor tiene sintaxis válida
    And el cursor tiene <incompatibilidad>
    When consulto H
    Then recibo 400 VALIDATION_ERROR campo cursor INVALID_VALUE sin página parcial
    Examples:
      | incompatibilidad                         |
      | owner corresponde a otra identidad       |
      | category difiere                         |
      | projectId difiere                        |
      | taskId difiere                           |
      | from difiere                             |
      | to difiere                               |
      | upper es menor que after                 |

  @s18
  Scenario Outline: Seguridad y estructura preceden al cursor
    Given la petición presenta <defectos>
    When consulto H
    Then recibo <resultado> sin consultar hechos ajenos
    Examples:
      | defectos                                   | resultado                              |
      | anónimo y cursor inválido                  | 401 del contrato de sesión             |
      | seguridad403 y query inválida              | 403 del contrato de sesión             |
      | taskId sin projectId y cursor inválido     | 400 taskId INVALID_VALUE               |
      | from posterior a to y cursor inválido      | 400 to INVALID_VALUE                   |

  @s19
  Scenario: Un cursor no autoriza ni exige la presencia de su fila de frontera
    Given un cursor sintácticamente válido ligado al propietario y filtros propios
    And upper y after son claves coherentes que no identifican filas existentes
    When consulto H con ese cursor
    Then recibo únicamente hechos propios con key menor que after y no mayor que upper
    And no se consulta a otro propietario ni se devuelve404 por la frontera inexistente

  @s20
  Scenario Outline: Un commit tardío respeta el límite sin fingir un snapshot global
    Given ya recorrí veinte hechos con upper a las11:00Z y after a las09:00Z del 2026-09-07
    And sólo quedaba un hecho a las08:00Z y después confirma otro propio a <instante>
    When continúo con el cursor emitido
    Then la continuación <resultado>
    And no se repiten hechos ya entregados
    Examples:
      | instante                   | resultado                                      |
      | 2026-09-07T12:00:00Z        | contiene sólo el hecho de las08:00Z             |
      | 2026-09-07T10:00:00Z        | contiene sólo el hecho de las08:00Z             |
      | 2026-09-07T08:30:00Z        | contiene el nuevo seguido del hecho de las08:00Z |

  @s21
  Scenario: Volver a recientes descubre novedades y etiquetas actuales
    Given un recorrido anterior excluyó un commit tardío situado delante de after
    And ese hecho queda entre los veinte más recientes y un proyecto propio cambió su nombre después de aquella página
    When consulto H sin cursor con los mismos filtros
    Then el nuevo recorrido incluye ese hecho entre sus veinte primeros
    And muestra el nombre actual y conserva los detalles históricos
    And no presenta el recorrido anterior como snapshot congelado entre peticiones

  @s22
  Scenario: Una página lee hechos y contexto en un único snapshot sin escribir
    Given una lectura H está en curso y otra transacción confirma un hecho y renombra su proyecto después de iniciarse su snapshot
    When termina esa lectura H
    Then su página contiene sólo hechos y etiquetas del snapshot anterior
    And la transacción es de sólo lectura REPEATABLE_READ y no consulta el reloj para decidir inclusión
    And ninguna tabla de dominio, preferencias, intervalos, recibos o outbox cambia por leer

  @s23
  Scenario Outline: Un fallo de almacenamiento o representación no produce una página parcial
    Given existen hechos propios y se produce <fallo> durante H
    When termina la consulta
    Then recibo 503 STORAGE_UNAVAILABLE y ningún items parcial ni falso vacío
    And el problema no contiene SQL, stack o notas privadas
    Examples:
      | fallo                                             |
      | fallo SQL de lectura                              |
      | fallo al finalizar la transacción read-only        |
      | recibo seleccionado incoherente con su contexto    |
      | JSON durable seleccionado imposible de representar |

  @s24
  Scenario: El historial sobrevive al reinicio y a la retirada del transporte publicado
    Given hay hechos confirmados de las cinco fuentes y se conservó su página
    And sus eventos fueron publicados y se retiró sólo outbox publicado
    And la API se reinició y el broker está caído
    When consulto H de nuevo con el mismo propietario
    Then recupero los mismos IDs, tiempos y detalles de los hechos
    And no se crean eventos ni se requiere publicación para responder

  @s25
  Scenario: El cliente conserva precisión sin recalcular la atribución histórica con TZDB
    Given H devuelve un CLOSE válido de una sesión iniciada en año1000 con workedMicroseconds "9007199254740992" y occurredAt en año1600 con fracción .123457
    And closure.workDate y closeZoneId persistidos difieren de la fechaUTC o del catálogo disponible en el navegador
    When el cliente presenta la página
    Then conserva exactamente los decimales, instante, workDate y closeZoneId
    And un fallback visual a UTC se etiqueta sin cambiar filtrosUTC ni el recibo

  @s26
  Scenario Outline: Rechazar una página completa por forma o identidad incompatible
    Given una respuesta H contiene entradas y presenta únicamente <defecto>
    When el cliente recibe la página
    Then rechaza la página completa como error y no muestra un subconjunto como resultado parcial ni lista vacía
    Examples:
      | defecto                                               |
      | campo adicional en envelope                           |
      | entrada sin taskTitle                                 |
      | projectName vacío                                     |
      | type desconocido                                      |
      | más de20 entradas                                     |
      | SESSION_STARTED exterior id distinto a details.id     |
      | SESSION_STARTED exterior tiempo distinto a startedAt  |
      | BLOCK_PLANNED tiempo distinto a createdAt              |
      | cambio de sesión con contexto exterior ajeno a before  |
      | recibo con aritmética inválida según su contrato       |
      | details con campo adicional para su variante           |

  @s27
  Scenario Outline: El cliente contrasta filtros y orden total de la página
    Given una página H tiene DTO individuales válidos pero <defecto>
    When el cliente recibe la respuesta para los filtros aplicados
    Then rechaza toda la página sin etiquetarla como vacío
    Examples:
      | defecto                                 |
      | entrada de projectId diferente           |
      | entrada de taskId diferente              |
      | familia planning con category=sessions   |
      | fechaUTC anterior a from                 |
      | fechaUTC posterior a to                  |
      | dos entradas con igual type e id         |
      | instantes en orden ascendente            |
      | empate con rangos de familia invertidos  |
      | empate de familia con UUID ascendente    |

  @s28
  Scenario Outline: Abrir historial desde navegación o contexto sin necesitar hechos previos
    Given estoy en <origen> y el contexto propio no tiene hechos
    When activo <enlace>
    Then navego a <destino> y veo el encabezado "Historial"
    And los filtros contextuales vacíos muestran Este proyecto o Esta tarea cuando corresponda, sin nombre histórico inventado
    Examples:
      | origen             | enlace                         | destino                                  |
      | Workspace          | Historial                      | /historial                               |
      | detalle de proyecto| Ver historial de este proyecto | /historial con projectId                 |
      | detalle de tarea   | Ver historial de esta tarea    | /historial con projectId y taskId         |

  @s29
  Scenario: Aplicar y limpiar filtros son navegación explícita sin escrituras
    Given veo una página antigua y edito categoría y Fecha del hecho (UTC) sin aplicar
    When aplico los filtros
    Then la URL contiene los valores aplicados y no el cursor anterior
    And sólo se consulta GET y la lista anterior no se presenta bajo los filtros nuevos como vigente
    And están disponibles controles etiquetados para limpiar filtros

  @s30
  Scenario: Limpiar filtros reinicia el recorrido
    Given veo H filtrado por categoría, fechas y contexto con cursor
    When activo limpiar filtros
    Then la URL de historial no contiene esos filtros ni cursor
    And sólo se consulta la primera página global propia sin escrituras

  @s31
  Scenario Outline: Una página por URL permite navegar y recargar
    Given existen una URL reciente y otra más antigua con sus filtros y cursor
    When realizo <accion>
    Then la consulta corresponde a <consulta>
    And se presenta una sola página, sin append infinito ni cursores privados en almacenamiento web
    Examples:
      | accion                  | consulta                         |
      | activar Más antiguos    | URL con nextCursor y mismos filtros |
      | activar Volver a recientes | mismos filtros sin cursor    |
      | volver con Back         | URL anterior                     |
      | recargar URL antigua    | filtros y cursor de esa URL      |

  @s32
  Scenario Outline: Los enlaces de fila reutilizan rutas existentes
    Given veo un hecho de <tipo> con contexto propio
    When activo su enlace <enlace>
    Then navego a <destino> sin reenviar la acción histórica
    Examples:
      | tipo                | enlace          | destino                              |
      | SESSION_STARTED     | sesión          | URL16 con proyecto,tarea,sesión       |
      | SESSION_CHANGED     | sesión          | URL16 con proyecto,tarea,sesión       |
      | BLOCK_PLANNED       | tarea           | detalle existente de tarea           |
      | BLOCK_CHANGED       | historial local | tarea y cambios de bloques existentes |
      | TASK_STATUS_CHANGED | proyecto        | detalle existente de proyecto        |
      | SESSION_CHANGED     | filtrar tarea   | /historial con projectId y taskId     |
      | BLOCK_CHANGED       | filtrar proyecto| /historial con projectId              |

  @s33
  Scenario: Detalles históricos se expanden sin consultas ni ejecución de notas
    Given una página incluye reserva, reapertura, CLOSE y EXTEND propios
    And CLOSE conserva una nota con espacios, salto de línea y texto "<script>alert(1)</script>"
    When expando sus details nativos con nombre accesible
    Then veo los datos ya recibidos sin otra solicitud por fila
    And la nota se muestra como texto preservado sin ejecutar HTML
    And reserva, reapertura, trabajo neto del cierre y fin ampliado tienen etiquetas distintas
    And se muestran las revisiones locales y la advertencia de que empate temporal no significa orden causal
    And no aparecen totales, progreso calculado ni controles para reenviar acciones históricas

  @s34
  Scenario Outline: Distinguir carga, vacíos y error conservando los filtros
    Given la consulta actual de historial está en <estado>
    When se presenta su resultado o espera
    Then se muestra <salida> y no se confunde con otro estado
    Examples:
      | estado                         | salida                                   |
      | pendiente                      | anuncio de carga antes de400ms           |
      | 200 vacío sin filtros          | vacío sin hechos                         |
      | 200 vacío con filtros          | vacío por filtros con opción de quitarlos |
      | 503                            | error con filtros conservados y reintento |

  @s35
  Scenario: Reintentar un error sólo repite la lectura vigente
    Given H falló con503 y se conservan filtros y cursor de la URL
    When activo reintentar
    Then se anuncia carga y se envía sólo GET con esos filtros y cursor
    And no se crean decisiones, recibos ni eventos

  @s36
  Scenario Outline: Respuestas obsoletas no sustituyen una nueva página o identidad
    Given una consulta antigua quedó pendiente en <etapa>
    And ya navegué a otro filtro o página y su respuesta válida se muestra
    When termina la respuesta antigua con <resultado>
    Then no reemplaza ni borra la página nueva y no revoca otra identidad autenticada
    Examples:
      | etapa                         | resultado                 |
      | antes de entregar ResponseHTTP| HTTP401                   |
      | decodificación JSON diferida  | HTTP200 con página antigua |
      | clasificación de problema    | HTTP503 desconocido       |

  @s37
  Scenario Outline: Pérdida actual de acceso retira datos privados
    Given veo hechos propios con notas y contexto
    When la consulta vigente devuelve <respuesta>
    Then se retiran la lista, detalles y notas privados
    And se ofrece <salida> sin presentar éxito ni falso vacío
    Examples:
      | respuesta                 | salida                                  |
      | 401                       | recuperación de sesión heredada          |
      | 404 RESOURCE_NOT_FOUND    | quitar filtro de contexto                |

  @s38
  Scenario Outline: Navegación y carga conservan un foco coherente
    Given activé <control> y la consulta sigue pendiente
    When la nueva página sustituye el resultado anterior
    Then conservo foco en iniciador si permanece o en encabezado del resultado cuando procede tras navegar
    And no se roba el foco que moví deliberadamente a otro control
    Examples:
      | control              |
      | aplicar filtros      |
      | Más antiguos         |
      | reintentar           |

  @s39
  Scenario: Recorrer historial accesible sin ampliar a estadísticas
    Given la matriz de treinta principios UX y los tamaños320–2560, bordes y altura400 acordados
    When recorro con teclado historial, filtros, vacíos, error, paginación y detalles largos en los motores y modos pactados
    Then se verifican controles44px, foco visible, orden semántico, etiquetas y ausencia de solapes o scroll horizontal de acciones
    And se conserva evidencia de axe, texto200%, zoom nativo200% y tres motores con sus límites de dispositivos y lectores físicos
    And no aparecen exportación, edición histórica, estadísticas, consumidores o rutas de detalle nuevas
