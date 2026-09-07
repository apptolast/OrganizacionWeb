Feature: Avisar del fin acordado y ampliar una sesión por decisión explícita
  Como propietario quiero comprobar el fin vigente y decidir si cierro o amplío
  sin convertir el tiempo añadido en trabajo realizado ni alterar el inicio original.

  # Normativa: project-spec.md, sección17 congelada en SHA2F44F439...B10ACE02.
  # E = GET /api/v1/work-sessions/{sessionId}/end-time.
  # P = POST /api/v1/work-sessions/{sessionId}/extend.
  # S/A y C/K conservan las rutas de estado/activa y recibos14–16.
  # P requiere Idempotency-Key y Work-Session-Revision completos; sin ETag ni If-Match.
  # E3 = state,serverNow,effectiveEndAt; state conserva State6.
  # EXTEND7 = id,sessionId,action,occurredAt,before,after,extension.
  # extension3 = additionalMinutes,previousEndAt,effectiveEndAt.
  # Revisión y acumulado son cadenas decimales canónicas; cantidad es un número entero.
  # Se reutilizan seguridad/negociación/JSON estricto/UUID/key/no-store de14–16.
  # Sus matrices siguen vigentes; aquí se acreditan las conexiones y diferencias17.

  Background:
    Given una identidad propia autenticada con seguridad y cabeceras válidas salvo el defecto indicado
    And existe como máximo una sesión running o paused del propietario
    And inicio, intervalos y recibos anteriores cumplen sus contratos14–16

  @s1
  Scenario: Ampliar antes del fin conserva trabajo e inicio con precisión exacta
    Given una sesión running de revisión3 y fin vigente "2026-09-07T11:00:00.123456Z"
    And changedAt y runningSince son "2026-09-07T10:00:00.123456Z"
    And el reloj único devuelve "2026-09-07T10:30:00.123457999Z"
    When confirmo P con additionalMinutes 1 y revisión3
    Then recibo 201 con Location del cambio propio y EXTEND7 de revisión "4"
    And occurredAt es "2026-09-07T10:30:00.123457Z"
    And extension es exactamente 1, "2026-09-07T11:00:00.123456Z", "2026-09-07T11:01:00.123456Z"
    And before y after difieren únicamente en revision
    And no se añade intervalo ni cambia trabajo, estado, inicio, zona, tarea, proyecto o planificación
    And se confirman un recibo y un evento junto con el fin y la última decisión

  @s2
  Scenario Outline: Una ampliación tardía parte del reloj y no de un fin vencido
    Given una sesión <status> con fin vigente "2026-09-07T11:00:00.000001Z"
    And la tarea y el proyecto de esa sesión están completed
    And el reloj de confirmación es "2026-09-07T11:30:00.000002Z"
    When confirmo P con additionalMinutes 1440 y revisión vigente
    Then recibo 201 y effectiveEndAt "2026-09-08T11:30:00.000002Z"
    And sólo revision cambia en State6 y no se añade trabajo ni intervalo
    And se conserva <status> y la tarea y el proyecto permanecen completed
    Examples:
      | status  |
      | running |
      | paused  |

  @s3
  Scenario: La ampliación acumulada supera un día sin alterar la duración original
    Given una sesión abierta con ampliaciones confirmadas que ya suman 1440 minutos
    When confirmo una nueva ampliación de 1 minuto con revisión vigente
    Then recibo 201 y el nuevo fin cumple max(fin anterior, occurredAt) más 1 minuto
    And plannedEndAt y plannedMinutes originales permanecen idénticos
    And no se limita la duración acumulada a 1440 minutos

  @s4
  Scenario Outline: Validar exclusivamente la cantidad de una intención nueva
    Given un objeto P con additionalMinutes <value>
    And la key existe y el recurso solicitado es ajeno
    When envío P
    Then recibo 400 VALIDATION_ERROR con campo additionalMinutes y code <code>
    And no se recupera recibo ni se consulta propiedad ni se escribe
    Examples:
      | value              | code         |
      | ausente            | REQUIRED     |
      | null               | REQUIRED     |
      | cadena "1"         | INVALID_TYPE |
      | true               | INVALID_TYPE |
      | array vacío        | INVALID_TYPE |
      | objeto vacío       | INVALID_TYPE |
      | 0                  | OUT_OF_RANGE |
      | -1                 | OUT_OF_RANGE |
      | 1441               | OUT_OF_RANGE |
      | 1.5                | OUT_OF_RANGE |

  @s5
  Scenario Outline: Conectar el protocolo estricto heredado a las dos rutas nuevas
    Given <defect> en la ruta <route>
    When envío la petición
    Then recibo <result> según el contrato común 15 sin datos privados ni escrituras
    And no se ejecuta la decisión de ampliación ni se captura un reloj de negocio
    Examples:
      | route | defect                                               | result                       |
      | E     | petición anónima con query e id inválido                | 401                          |
      | E     | query presente con id inválido                         | 400 de query                 |
      | P     | query presente con id inválido                         | 400 de query                 |
      | P     | falta Work-Session-Revision y el JSON es inválido       | 428 PRECONDITION_REQUIRED    |
      | P     | token malformado y JSON inválido                        | 400 de token                 |
      | P     | dos documentos JSON concatenados con cabeceras válidas | 400 MALFORMED_JSON           |
      | P     | campo effectiveEndAt adicional al objeto válido         | 400 de campo no admitido     |

  @s6
  Scenario Outline: Preservar propiedad e identidad antes de recuperar una key
    Given una key EXTEND ya confirmada y <context>
    When envío la misma intención por P
    Then recibo <result> sin entregar el recibo ni escribir
    Examples:
      | context                                                  | result                      |
      | sesión ajena con token de identidad incorrecta            | 404 WORK_SESSION_NOT_FOUND  |
      | sesión propia con UUID del token de otra sesión           | 412 PRECONDITION_FAILED  |

  @s7
  Scenario: Recuperar una ampliación histórica antes de estado y reloj actuales
    Given una ampliación confirmada y su sesión posteriormente cerrada
    And el propietario ha iniciado otra sesión y el reloj ahora falla
    When reenvío P con la key, sesión, acción, revisión y cantidad originales
    Then recibo 200 con el mismo recibo y Location originales
    And no se consulta el reloj ni se modifica fin, marca temporal, intervalos, recibos o outbox
    And la nueva sesión permanece intacta

  @s8
  Scenario Outline: Una key no representa otra intención de cambio
    Given una key confirmada para EXTEND de la sesión A
    And A está cerrada y B es la única sesión abierta propia
    When envío con esa key una intención que cambia <difference>
    Then recibo 409 IDEMPOTENCY_CONFLICT sin escrituras
    Examples:
      | difference                                      |
      | additionalMinutes de la misma sesión A           |
      | revisión esperada de A con token de identidad A  |
      | acción CLOSE de A                               |
      | sesión B con su token propio                     |

  @s9
  Scenario Outline: Revisión y terminalidad preceden al reloj
    Given una sesión <status> de revisión <current> y un reloj que falla
    When envío una key nueva por P con revisión <expected>
    Then recibo <result> y el reloj no se consulta
    And no se escribe ni se libera de nuevo la plaza
    Examples:
      | status  | current             | expected            | result                              |
      | closed  | 4                   | 3                   | 412 PRECONDITION_FAILED             |
      | closed  | 4                   | 4                   | 409 WORK_SESSION_STATE_CONFLICT     |
      | running | 9223372036854775807 | 9223372036854775807 | 409 WORK_SESSION_REVISION_EXHAUSTED  |

  @s10
  Scenario Outline: La última ampliación protege el reloj de todas las decisiones
    Given una sesión <status> cuyo changedAt es anterior a una ampliación confirmada en T
    And el reloj devuelve T menos un microsegundo
    When confirmo <action> con key nueva y revisión vigente
    Then recibo 409 temporal 15
    And estado, fin, marca, intervalos, recibos y outbox permanecen idénticos
    Examples:
      | status  | action |
      | running | EXTEND |
      | running | PAUSE  |
      | paused  | RESUME |
      | paused  | CLOSE  |

  @s11
  Scenario: El mismo microsegundo de la última decisión sigue siendo válido
    Given una sesión paused y una ampliación confirmada en T posterior a changedAt
    When confirmo RESUME con reloj T y revisión vigente
    Then recibo 201 con occurredAt y changedAt T y runningSince T
    And no se atribuye trabajo al descanso anterior ni se modifica el fin ampliado

  @s12
  Scenario Outline: Rechazar reloj o nuevo fin fuera de representación sin recortar
    Given una sesión abierta válida con revisión vigente y <defect>
    When confirmo P con additionalMinutes 1
    Then recibo 409 WORK_SESSION_TIME_OUT_OF_RANGE con el título temporal 15
    And no cambia estado, fin, marca, recibo ni outbox
    Examples:
      | defect                                                 |
      | reloj en año UTC 0000                                   |
      | reloj en año UTC 10000                                  |
      | fin anterior de año 9999 cuyo minuto añadido desborda    |

  @s13
  Scenario Outline: Leer el fin sin materializar proyecciones ni alterar contratos anteriores
    Given una sesión propia <state> creada antes de 17 y sin ampliaciones
    When consulto E
    Then recibo 200 con exactamente state,serverNow,effectiveEndAt
    And effectiveEndAt es el plannedEndAt original y state conserva exactamente State6
    And Work-Session-Revision es canónica acorde al id y revisión, sin comillas
    And no hay ETag ni Location, sí no-store, y no se escribe ninguna fila
    And S y A conservan sus representaciones15 y14
    Examples:
      | state   |
      | running |
      | paused  |
      | closed  |

  @s14
  Scenario Outline: El GET nuevo aplica propiedad y reloj en el orden establecido
    Given <context>
    When consulto E
    Then recibo <result> sin escrituras
    Examples:
      | context                                                       | result                      |
      | sesión ajena y reloj no representable                          | 404 WORK_SESSION_NOT_FOUND  |
      | sesión propia y reloj anterior a última ampliación             | 409 temporal 15              |
      | sesión propia y reloj igual a la última decisión               | 200                         |
      | sesión propia y reloj fuera del rango UTC                      | 409 temporal 15              |

  @s15
  Scenario: El snapshot del fin no mezcla una ampliación concurrente
    Given E ha leído la sesión antes de que otra transacción confirme una ampliación
    And esa ampliación termina antes de que E lea el fin
    When E completa su lectura
    Then devuelve estado, revisión y fin anteriores coherentes en read-only REPEATABLE_READ
    And captura serverNow una sola vez después de sus lecturas
    And una consulta posterior devuelve la revisión y el fin ampliados coherentes
    And ninguna de las consultas escribe ni impide confirmar al escritor

  @s16
  Scenario Outline: Traducir fallos de almacenamiento y del cierre read-only
    Given una sesión propia y <failure> al consultar <route>
    When termina la consulta
    Then recibo 503 STORAGE_UNAVAILABLE sin estado vacío ni recibo fabricado
    Examples:
      | route | failure                         |
      | E     | fallo de lectura de la proyección |
      | E     | fallo al finalizar la transacción |
      | K     | fallo al leer el recibo EXTEND     |

  @s17
  Scenario Outline: Toda ampliación se confirma o revierte como una unidad
    Given una sesión abierta, una key nueva y <failure>
    When intento confirmar P
    Then recibo 503 STORAGE_UNAVAILABLE
    And estado, revisión, fin, marca temporal, intervalos, recibos y outbox son idénticos al antes
    Examples:
      | failure                                                   |
      | supresión de la escritura de proyección                    |
      | supresión de la escritura del recibo sin ganador durable    |
      | supresión de la escritura de outbox                        |
      | fallo al confirmar la transacción                          |
      | violación de una restricción distinta de owner/key          |

  @s18
  Scenario: Dos reenvíos simultáneos recuperan un único cambio
    Given dos peticiones de la misma sesión con idéntica key y toda la intención EXTEND
    When ambas compiten por confirmar P
    Then responden 201 y200 con el mismo recibo y Location
    And sólo se incrementa una revisión y se persisten una ampliación y un evento

  @s19
  Scenario Outline: Dos decisiones distintas de una revisión no pueden confirmar juntas
    Given una sesión <status> y dos intenciones distintas con keys nuevas y la misma revisión
    When EXTEND y <other> compiten por confirmar
    Then hay un 201 y un 412 sin preferencia contractual de ganador
    And sólo el ganador modifica estado, fin, marca, intervalos, recibos y outbox según su acción
    Examples:
      | status  | other  |
      | running | EXTEND |
      | running | PAUSE  |
      | paused  | RESUME |
      | running | CLOSE  |

  @s20
  Scenario Outline: Resolver una colisión de key después del rollback
    Given una inserción de recibo P pierde la unicidad owner/key frente a <winner>
    When se resuelve la colisión en una lectura nueva posterior al rollback
    Then recibo <result> y no persisten efectos del intento perdedor
    Examples:
      | winner                                       | result                    |
      | la misma intención durable                   | 200 y recibo ganador      |
      | otra cantidad o acción durable               | 409 IDEMPOTENCY_CONFLICT   |
      | ningún recibo durable recuperable            | 503 STORAGE_UNAVAILABLE   |

  @s21
  Scenario: Gestionar una sesión no depende de locks de su planificación
    Given otra transacción mantiene bloqueados tarea, proyecto y preferencia del propietario
    And otro propietario tiene una decisión de sesión pendiente
    When confirmo P en mi sesión abierta
    Then puedo confirmar201 sin esperar esos recursos ajenos a la sesión
    And los recursos bloqueados no cambian ni se crea un mutex entre propietarios

  @s22
  Scenario: La migración aditiva conserva hechos y recuperación anteriores
    Given datos válidos de14–16 con sesiones running, paused y closed y recibos P/R/CLOSE
    When aplico la migración de 17 posterior a las migraciones ya publicadas
    Then filas, keys y JSONB históricos permanecen idénticos y recuperables
    And las sesiones anteriores usan plannedEndAt como fin y changedAt o fallback14 como última decisión
    And P/R mantienen recibo6, CLOSE recibo7 y SessionStart7 sin extension ni closure null añadidos

  @s23
  Scenario Outline: Recuperar EXTEND como hecho histórico sin reloj ni retención de outbox
    Given un EXTEND confirmado, una ampliación posterior y su sesión finalmente cerrada
    And el evento original ya se publicó y su fila de outbox se retiró
    When consulto <route> tras reiniciar la API
    Then recibo 200 con el recibo EXTEND7 original idéntico y sin Location
    And no se consulta reloj ni catálogo de zonas ni se escribe
    Examples:
      | route |
      | C     |
      | K     |

  @s24
  Scenario: Confirmar sin broker y publicar el evento exacto tras recuperar la conexión
    Given RabbitMQ no está disponible y el relay perderá la respuesta 201 de P
    When ejecuto el recorrido de ampliación y recuperación con reinicio de API y broker
    Then la base de datos conserva una sola ampliación y K recupera el recibo confirmado
    And WorkSessionExtended.v1 se publica en work-session.extended.v1 y cola quorum durable organization.work-session-extended.v1
    And el payload original tiene exactamente sus once campos y coincide con recibo, sesión, revisión y fórmula
    And eventId es independiente de sessionId y no aparecen notas privadas ni key
    And retirar sólo ese outbox publicado no impide C/K/E después del reinicio

  @s25
  Scenario Outline: Validar el evento nuevo sin alterar rutas históricas
    Given un WorkSessionExtended.v1 con <defect>
    When el publicador intenta entregarlo
    Then queda blocked INVALID_EVENT y no se entrega un mensaje corrupto
    And las rutas y esquemas14–16 siguen aceptando sus eventos originales
    Examples:
      | defect                                      |
      | revisión numérica en lugar de cadena         |
      | status closed                               |
      | additionalMinutes fraccionario              |
      | fin distinto de la fórmula por un microsegundo |
      | campo privado adicional                     |

  @s26
  Scenario Outline: El cliente rechaza snapshots o recibos incompatibles
    Given la respuesta de <route> contiene <defect> y el contexto actual sigue siendo propio
    When el cliente termina de decodificarla
    Then no presenta el dato incompatible como estado o ampliación confirmados
    And si procedía de un envío conserva la intención incierta recuperable
    Examples:
      | route | defect                                                         |
      | E     | campo adicional o ausencia de uno de los tres campos            |
      | E     | token que no coincide con id o revisión                         |
      | E     | effectiveEndAt anterior a plannedEndAt por un microsegundo       |
      | E     | State6 con status desconocido                                  |
      | P     | EXTEND sin extension3 o con closure adicional                   |
      | K     | before paused y after running                                  |
      | K     | acumulado, changedAt o runningSince alterado al ampliar          |
      | K     | previousEndAt anterior a plannedEndAt                            |
      | K     | nuevo fin distinto de la fórmula exacta por un microsegundo      |
      | K     | misma sesión pero additionalMinutes distinto de la intención    |
      | K     | otra identidad o revisión no consecutiva                        |
      | K     | recibo P/R/C que incumple su igualdad temporal heredada          |

  @s27
  Scenario: Admitir datos exactos y separados del reloj interno no expuesto
    Given un EXTEND paused válido con fechas cuya representación en microsegundos supera Number seguro
    And occurredAt es posterior a changedAt y sólo revision cambia entre before y after
    When el cliente recibe ese EXTEND y su E válido
    Then acepta la fórmula exacta sin redondeo ni exigir changedAt igual a occurredAt
    And no inventa una comprobación de la marca interna ausente del DTO
    And representa fecha y zona histórica con fallback UTC etiquetado si hace falta

  @s28
  Scenario Outline: Encontrar el aviso y las decisiones en ambos recorridos existentes
    Given una sesión abierta <session> y un E válido cuyo fin ya se alcanzó
    When abro <surface>
    Then veo el fin original y, si difiere, el fin acordado actual con fecha y zona
    And veo "Ha llegado el fin acordado", "Cerrar sesión de trabajo" y "Ampliar tiempo"
    And la identidad consultada corresponde a la sesión mostrada
    And mostrar el aviso no añade cambios, intervalos, trabajo ni eventos
    Examples:
      | session                                  | surface                     |
      | running en la tarea mostrada              | detalle de tarea            |
      | paused de otra tarea propia devuelta por A | detalle de tarea            |
      | running identificada por p/t/s propios    | URL conocida de la sesión16 |

  @s29
  Scenario: Ampliar exige cantidad y confirmación distinta del cierre
    Given el aviso visible de una sesión abierta
    When abro "Ampliar tiempo"
    Then la cantidad no tiene valor preaceptado y no se transmite P
    And "Confirmar ampliación" es una decisión diferente de "Confirmar cierre"
    And no hay formularios ni submits de ambas acciones anidados
    And acceder al cierre conserva la URL de sesión antes del POST16

  @s30
  Scenario: El vencimiento local solicita comprobar antes de afirmar actualidad
    Given E confirmó una sesión abierta con fin futuro y no hay aviso previo
    When transcurre el plazo monotónico hasta ese fin
    Then se inicia una sola consulta E y se anuncia la comprobación pendiente
    And no se anuncia vencimiento ni se escribe mientras la respuesta siga pendiente
    And una respuesta vigente con serverNow igual al fin permite anunciar el aviso
    And ese aviso no mueve el foco ni abre un modal

  @s31
  Scenario: Fragmentar plazos largos sin adelantar consultas ni desbordar el temporizador
    Given un E válido con fin a 25 días del serverNow recibido
    When termina el primer fragmento de espera de como máximo 2147483647 milisegundos
    Then todavía no se consulta E ni se anuncia vencimiento
    And se rearma una espera positiva acotada por el tiempo monotónico restante
    And el plazo completo nunca se entrega como un delay nativo desbordado

  @s32
  Scenario Outline: Un callback anticipado o resto fraccionario no forma un bucle inmediato
    Given E confirmó un fin futuro y <remaining>
    When se ejecuta el callback de espera
    Then no consulta E ni anuncia vencimiento mientras quede tiempo positivo
    And rearma una espera positiva de al menos 1 milisegundo y como máximo 2147483647
    And no programa un bucle de delay cero ni usa Date.now como autoridad
    Examples:
      | remaining                                                |
      | el callback fue anticipado y quedan dos segundos          |
      | queda exactamente un microsegundo según reloj monotónico |

  @s33
  Scenario Outline: Volver a visible revalida sin duplicar una consulta pendiente
    Given una sesión abierta y la página oculta tras un snapshot confirmado
    And <pending>
    When la página vuelve a visible después de suspensión
    Then hay <requests> consulta E nueva y como máximo una comprobación vigente en vuelo
    And al completarse se rearma el fin que confirme el servidor
    And no se promete una alarma durante la suspensión
    Examples:
      | pending                           | requests |
      | ninguna comprobación en vuelo     | una      |
      | una comprobación E ya está en vuelo | ninguna  |

  @s34
  Scenario Outline: Conservar hechos confirmados frente a fallos o reloj local
    Given <prior>
    When ocurre <change>
    Then se muestra <result>
    And no se pausa, reanuda ni cierra automáticamente la sesión
    Examples:
      | prior                          | change                             | result                                                   |
      | ningún E válido                 | falla la consulta inicial con 503    | error y reintento sin afirmar vencimiento o ausencia       |
      | aviso de fin confirmado         | falla la actualización con 503      | aviso conservado más error de actualización separado       |
      | aviso de fin confirmado         | el reloj local retrocede una hora  | el mismo aviso confirmado                                 |
      | snapshot futuro válido          | E devuelve 409 temporal 15           | error temporal sin interpretar ausencia o cierre           |

  @s35
  Scenario: Una ampliación actual rearmada tiene un aviso propio sin anuncios por render
    Given un aviso confirmado del fin T y una ampliación local ya confirmada
    When E confirma el nuevo fin futuro U con la revisión nueva
    Then se retira el aviso de T y se rearma U sin modificar el inicio original
    And al confirmar posteriormente su vencimiento se anuncia U una sola vez en ese contexto
    And renders repetidos del mismo fin no repiten el anuncio

  @s36
  Scenario Outline: Coordinar decisiones hermanas de la misma sesión
    Given <action> local está <phase> en una superficie con controles de estado y ampliación
    When intento otra decisión de negocio de esa misma sesión desde el control hermano
    Then no se transmite otra intención ni se sustituye la cantidad, key o token retenidos
    And consultar o comprobar según el flujo y navegar siguen disponibles
    And salir no se presenta como revocación del POST transmitido
    Examples:
      | action | phase       |
      | EXTEND | enviándose  |
      | EXTEND | incierta    |
      | PAUSE  | comprobando |
      | RESUME | enviándose  |
      | CLOSE  | incierta    |

  @s37
  Scenario Outline: Conservar una intención incierta y distinguir comprobar de reenviar
    Given una ampliación transmitida con cantidad, key y token retenidos
    When el flujo recibe <response>
    Then <outcome>
    And no se crea otra intención ni se reenvía automáticamente
    Examples:
      | response                                      | outcome                                                       |
      | pérdida de respuesta o503 de P                  | conserva incertidumbre y ofrece Comprobar ampliación            |
      | respuesta P incompatible o error desconocido   | conserva incertidumbre y ofrece Comprobar ampliación            |
      | IDEMPOTENCY_CONFLICT de P                       | conserva incertidumbre sin autorizar otra decisión              |
      | K devuelve 404 de recibo reconocido              | permite reenvío manual idéntico sin afirmar rollback             |
      | K devuelve 503                                  | conserva incertidumbre sin habilitar reenvío                     |
      | 403 distinto de CSRF reconocido                  | conserva incertidumbre sin habilitar reenvío                     |
      | CSRF reconocido y renovación manual completada | permite decidir reenvío idéntico en una acción posterior separada |

  @s38
  Scenario Outline: Una revisión rechazada conserva el borrador pero exige actualidad
    Given P devuelve 412 y la cantidad del borrador se conserva
    When consulto E y obtengo <response>
    Then <outcome>
    And no se reenvía la key rechazada ni se confirma sin otra decisión manual
    Examples:
      | response               | outcome                                                         |
      | sesión abierta válida  | permite nueva confirmación con cantidad conservada y nueva key/revisión |
      | sesión closed válida   | retira ampliación y conserva acceso al cierre histórico          |
      | fallo o dato inválido  | mantiene borrador y error sin habilitar confirmar                 |

  @s39
  Scenario Outline: Separar el recibo recuperado del fin actual y de otra sesión
    Given <context>
    When termina la recuperación del recurso
    Then <outcome>
    And no se usa una lectura de estado como prueba de aquella intención perdida
    Examples:
      | context                                                        | outcome                                                       |
      | K confirma EXTEND y la consulta E posterior falla               | conserva recibo histórico más estado actual sin comprobar      |
      | recarga URL conocida sin key después de perder respuesta        | muestra fin durable por E sin atribuirse ni reenviar la intención |
      | la sesión conocida está closed y A devuelve otra sesión nueva   | conserva cierre y notas propios sin controles o timers de la cerrada |

  @s40
  Scenario Outline: Descartar respuestas anteriores a una decisión local confirmada
    Given <old> empezó antes de que una decisión local confirmara la revisión nueva
    And los snapshots dependientes nuevos ya se mostraron
    When llega la respuesta anterior
    Then no restaura revisión, fin o neto anteriores como actuales
    And se conserva separado el recibo confirmado y sólo queda el timer del fin vigente abierto
    Examples:
      | old                       |
      | E con fin anterior        |
      | S con neto/revisión previa |
      | A de la sesión ya cerrada  |

  @s41
  Scenario Outline: Retirar datos actuales y descartar resultados privados obsoletos
    Given una consulta de sesión está pendiente en <stage>
    And se navega a otra sesión propia antes de que termine
    When llega <response>
    Then no sustituye datos o borradores del contexto nuevo ni revoca su acceso
    And los timers y peticiones de la sesión anterior quedan retirados o abortados
    Examples:
      | stage                       | response                              |
      | entrega HTTP de E            | 401 anterior antes del observador      |
      | JSON de respuesta 200 de E    | snapshot anterior decodificado tarde  |
      | clasificación de error de P  | error distinto de 401 clasificado tarde |

  @s42
  Scenario Outline: Un rechazo actual de propiedad retira información de la sesión
    Given fin y borrador visibles de la sesión actual
    When su consulta recibe <response>
    Then se retiran datos privados, borradores y timers de ese contexto
    And no se confunde con un404 de recibo K que permitiría reenvío manual
    Examples:
      | response                   |
      | 401 actual                  |
      | 404 WORK_SESSION_NOT_FOUND  |
      | identidad p/t/s ajena a la URL conocida |

  @s43
  Scenario Outline: Feedback y foco durante acciones deliberadas
    Given <focus> y una acción de consulta o ampliación iniciada por el usuario
    When queda pendiente una respuesta
    Then hay feedback anunciado antes de 400 milisegundos y se impiden envíos duplicados
    And <result>
    Examples:
      | focus                                      | result                                               |
      | el iniciador permanece                     | conserva el foco en ese control                      |
      | el iniciador desaparece y el foco seguía dentro | el encabezado recibe el foco                     |
      | la persona ya eligió otro control          | no se roba el foco al control elegido                |

  @s44
  Scenario: Verificar ambas superficies accesibles sobre el flujo real
    Given los recorridos de detalle de tarea y URL de sesión con ampliación, pausa y cierre
    When se realiza la validación UX y funcional del alcance 17
    Then ambos permiten teclado, controles de al menos 44 por 44 píxeles y texto legible con zoom
    And se conserva contexto, cantidades y hechos ante carga, error y recuperación
    And se documentan motores, tamaños y evidencia de la matriz 30 heredada sin certificar dispositivos físicos
    And no se añaden alarmas externas, historial18, estadísticas ni tiempo trabajado ficticio
