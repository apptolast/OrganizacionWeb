Feature: Revisar el plan vigente y el trabajo real de una semana propia
  La lectura W es GET /api/v1/weekly-review bajo la sesión autenticada existente.
  Conserva los contratos14–18, sin escribir decisiones, interpretar notas como progreso ni crear métricas de pausas.
  Los ejemplos locales no multiplican las matrices heredadas de autenticación, DTO y zonas.

  @s1
  Scenario: Una semana vacía tiene siete días y un DTO cerrado
    Given soy propietario autenticado sin disponibilidad ni hechos
    And serverNow es 2026-09-09T12:00:00.123456Z
    When consulto W sin parámetros
    Then recibo200 no-store con exactamente weekStart,weekEnd,zoneId,zoneSource,availabilityZoneId,serverNow,startAt,endAt,days,totals,unquantifiedSessionCount
    And weekStart es2026-09-07, weekEnd es2026-09-13 y zonaUTC UNCONFIGURED con availabilityZoneId null
    And startAt es2026-09-07T00:00:00Z y endAt es2026-09-14T00:00:00Z
    And days contiene las siete fechas consecutivas con exactamente date,startAt,endAt,plannedMicroseconds,workedMicroseconds,capacityMicroseconds
    And cada plan y trabajo es"0", cada capacidad esnull y unquantifiedSessionCount es"0"
    And totals tiene exactamente plannedMicroseconds,workedMicroseconds,capacityMicroseconds con"0","0",null
    And no recibo ETag, notas, nombres, owner, recibos ni paginación

  @s2
  Scenario Outline: Resolver la zona y normalizar el día ancla al lunes
    Given serverNow es2026-09-09T12:00:00Z y la disponibilidad guarda <preferencia>
    When consulto W con date2026-09-09 y <seleccion>
    Then weekStart es2026-09-07 y weekEnd es2026-09-13
    And la zona efectiva es<zona> con zoneSource<origen> y availabilityZoneId<guardada>
    Examples:
      | preferencia            | seleccion             | zona          | origen        | guardada      |
      | Europe/Madrid válida   | sin zoneId            | Europe/Madrid | AVAILABILITY  | Europe/Madrid |
      | ausente                | sin zoneId            | UTC           | UNCONFIGURED  | null          |
      | zona ya no disponible  | sin zoneId            | UTC           | UNAVAILABLE   | zona guardada |
      | Europe/Madrid válida   | zoneId=UTC            | UTC           | EXPLICIT      | Europe/Madrid |
      | ausente                | zoneId=Europe/Madrid  | Europe/Madrid | EXPLICIT      | null          |

  @s3
  Scenario: La semana por defecto usa el día local del único instante de servidor
    Given serverNow es2026-09-06T23:30:00Z y la zona efectiva esEurope/Madrid
    When consulto W sin date
    Then weekStart es2026-09-07 y weekEnd es2026-09-13
    And la zona del navegador no modifica la semana ni las fronteras del servidor

  @s4
  Scenario Outline: Rechazar estructura y filtros inválidos antes de leer datos
    Given estoy autenticado
    When consulto W con <query>
    Then recibo400 VALIDATION_ERROR con campo<campo> y códigoINVALID_VALUE
    And no se consultan hechos ni se devuelve una semana vacía
    Examples:
      | query                              | campo  |
      | owner=otra-persona                 | query  |
      | date repetida                      | query  |
      | zoneId repetida                    | query  |
      | date vacía                         | date   |
      | date=2026-02-30                    | date   |
      | date=2026-9-7                      | date   |
      | date=0000-01-01                    | date   |
      | date=10000-01-01                   | date   |
      | zoneId vacía                       | zoneId |
      | zoneId fuera del catálogo          | zoneId |
      | zoneId con espacios alrededor      | zoneId |

  @s5
  Scenario Outline: Conservar la precedencia y seguridad de lectura existente
    Given la petición tiene <condicion>
    When consulto W con query desconocida
    Then recibo<resultado>
    And no se obtienen hechos privados
    Examples:
      | condicion                        | resultado                         |
      | sesión ausente                   | 401 UNAUTHENTICATED                |
      | sesión autenticada sin tokenCSRF | 400 query INVALID_VALUE            |
      | sesión autenticada y storage caído| 400 query INVALID_VALUE            |

  @s6
  Scenario Outline: Distinguir semana no representable y error temporal
    Given se cumple <condicion>
    When consulto W
    Then recibo<resultado> sin recortar fechas ni sustituir zona para ocultarlo
    Examples:
      | condicion                                                        | resultado                           |
      | date=9999-12-31 normaliza a una semana cuyo domingo sale de9999    | 400 date INVALID_VALUE              |
      | date=0001-01-01 y zona explícitaEtc/GMT-14 del catálogo            | 409 WEEKLY_REVIEW_TIME_OUT_OF_RANGE  |
      | serverNow no cabe en UTC0001–9999                                 | 409 WEEKLY_REVIEW_TIME_OUT_OF_RANGE  |
      | serverNow cabe en UTC pero su fecha local sale del rango público  | 409 WEEKLY_REVIEW_TIME_OUT_OF_RANGE  |
      | sin date, serverNow=9999-12-31T12:00:00Z y zonaUTC produce una semana cuyo domingo sale de9999 | 409 WEEKLY_REVIEW_TIME_OUT_OF_RANGE  |

  @s7
  Scenario Outline: Las fronteras civiles respetan DST y días saltados
    Given la zona y semana son<seleccion>
    When consulto W
    Then las ocho fronteras corresponden al inicio civil de cada fecha según el servidor
    And <duracion>
    And no se construyen días sumando24horas ni se recalculan fronteras con el navegador
    Examples:
      | seleccion                                | duracion                                                     |
      | Europe/Madrid con date2026-03-29          | la semana dura167horas                                       |
      | Europe/Madrid con date2026-10-25          | la semana dura169horas                                       |
      | Pacific/Apia con date2011-12-30           | el día2011-12-30 tiene inicio igual a fin y plan/trabajo cero  |

  @s8
  Scenario: Recortar reservas vigentes por día y conservar completed
    Given hay una reserva propia domingo2026-09-06T23:30Z a lunes2026-09-07T00:30Z
    And otra reserva propia lunes23:30Z a martes00:30Z
    And su proyecto y tarea están completed
    When consulto W de2026-09-07 enUTC
    Then el plan del lunes es"3600000000" y el del martes"1800000000"
    And el resto del plan es"0" y el total"5400000000"
    And esas reservas no suman trabajo real

  @s9
  Scenario Outline: Replanificación y cancelación modifican sólo el plan vigente
    Given una reserva original propia está el lunes y tiene <estado>
    And una sesión ya produjo trabajo real el lunes
    When consulto W de esa semana
    Then <plan>
    And el trabajo real sigue en sus intervalos efectivos
    And no sumo reserva original y proyección como dos planes ni plannedMinutes o EXTEND como reservas
    Examples:
      | estado                              | plan                                      |
      | movimiento confirmado al martes    | el plan se atribuye al martes             |
      | cancelación confirmada             | esa reserva no aporta plan                |
      | movimiento a otra semana confirmado| esa reserva no aporta plan en esta semana |

  @s10
  Scenario Outline: El trabajo observado distingue running, paused y closed
    Given una sesión propia empezó a09:00 y trabajó hasta09:20 cuando se pausó
    And después tiene <estado> y serverNow es10:00
    When consulto W del día enUTC
    Then su trabajo es<micros> y no se suma además el acumulado de sesión
    And los huecos de pausa no se presentan como métrica de descanso
    Examples:
      | estado                                   | micros       |
      | paused sin reanudación                   | 1200000000   |
      | running tras reanudar09:40                | 2400000000   |
      | closed desde paused a09:50               | 1200000000   |
      | closed a09:50 tras reanudar09:40          | 1800000000   |

  @s11
  Scenario: El trabajo se reparte por intervalos y no por workDate del cierre
    Given una sesión trabajó de2026-09-06T23:30Z a2026-09-07T00:30Z y quedó cerrada
    And su cierre conserva workDate2026-09-06 en su zona histórica
    And otra sesión propia terminó exactamente al inicio de esa semana
    When consulto W de2026-09-07 enUTC
    Then el lunes recibe"1800000000" de trabajo y el resto de días"0"
    And no se atribuye toda la hora al día del cierre ni se altera workDate
    And el tramo que termina exactamente al inicio de semana aporta cero a esa semana

  @s12
  Scenario Outline: Medir duración efectiva al cruzar un cambio horario
    Given existe un intervalo propio cerrado de<inicio> a<fin> enEurope/Madrid
    When consulto W de esa fecha enEurope/Madrid
    Then ese día recibe<micros> de trabajo efectivo
    Examples:
      | inicio                  | fin                     | micros       |
      | 2026-03-29T01:30+01:00  | 2026-03-29T03:30+02:00  | 3600000000   |
      | 2026-10-25T01:30+02:00  | 2026-10-25T03:30+01:00  | 10800000000  |

  @s13
  Scenario: Conservar microsegundos y admitir transiciones de duración cero
    Given una sesión tiene dos tramos cerrados no solapados de1microsegundo cada uno
    And una pausa y reanudación confirmadas en el mismo instante no añaden trabajo
    When consulto W de ese día
    Then el trabajo del día y total son"2"
    And no redondeo cada tramo a segundo/minuto ni uso Duration.toNanos sobre una vida completa

  @s14
  Scenario: El fin previsto no recorta el trabajo y una ampliación no lo inventa
    Given una sesión running prevista hasta09:30 comenzó a09:00 y serverNow es10:00
    And una ampliación confirmó effectiveEndAt11:00 sin alterar intervalos ni runningSince
    When consulto W de ese día
    Then su trabajo hasta serverNow es"3600000000"
    And la ampliación no añade plan ni trabajo por adelantado
    When consulto además una semana futura con reservas y sin trabajo registrado
    Then esa semana tiene trabajo cero

  @s15
  Scenario Outline: Capacidad significa presupuesto actual en la misma zona
    Given la zona efectiva del informe es<zona> y la disponibilidad es<preferencia>
    When consulto W
    Then capacityMicroseconds es<capacidad>
    And capacidad cero no prueba descanso real ni genera deuda o penalización
    Examples:
      | zona          | preferencia                               | capacidad                                 |
      | Europe/Madrid | misma zona válida y120min por día         | "7200000000" por día y"50400000000" total |
      | Europe/Madrid | misma zona válida y0min por día           | "0" por día y total                       |
      | UTC           | Europe/Madrid válida                      | null en cada día y total                  |
      | UTC           | ausente                                   | null en cada día y total                  |
      | UTC           | zona guardada no disponible                | null en cada día y total                  |

  @s16
  Scenario: La capacidad elegida no cambia por la longitud civil del día
    Given la disponibilidad válida enEurope/Madrid elige120min todos los días
    When consulto W de2026-03-29 enEurope/Madrid
    Then la semana dura167horas y capacityMicroseconds total es"50400000000"
    And la capacidad es una referencia actual, no un presupuesto histórico reconstruido

  @s17
  Scenario: La compatibilidad antigua distingue duración desconocida de trabajo cero
    Given una sesión antigua closed sin final observable ni intervalos/recibos empezó dentro de la semana
    And otra sesión así empezó fuera y una sesión cuantificable aporta10min
    When consulto W
    Then unquantifiedSessionCount es"1" y workedMicroseconds total es"600000000"
    And se avisa de sesiones antiguas iniciadas esa semana sin duración registrada y total cuantificable incompleto
    And no se infiere duración desde plannedMinutes ni se asegura que ninguna antigua externa cruzara la semana
    And las filas antiguas y migraciones permanecen intactas

  @s18
  Scenario: Una sesión running heredada usa el inicio cuando no hay runningSince
    Given una sesión running heredada sin runningSince empezó09:00 sin intervalos previos y serverNow es09:10
    When consulto W del día
    Then su trabajo es"600000000" y no se escribe backfill al consultar

  @s19
  Scenario Outline: Rechazar datos seleccionados incoherentes sin una suma parcial
    Given los datos seleccionados contienen<fallo>
    When consulto W
    Then recibo503 STORAGE_UNAVAILABLE sin días/totales parciales ni datos privados del error
    And no se fusiona, satura, redondea o reclasifica esa corrupción como compatibilidad antigua
    Examples:
      | fallo                                           |
      | intervalos solapados de una sesión               |
      | intervalo fuera de la vida de su sesión          |
      | extremo final anterior al inicial               |
      | suma cerrada distinta de workedMicroseconds      |
      | tramo abierto incoherente                       |
      | overflow de suma long                           |
      | dato durable seleccionado ilegible               |

  @s20
  Scenario: Excluir datos ajenos en todas las fuentes de la semana
    Given dos propietarios tienen reservas, trabajo, sesiones antiguas y disponibilidad en la misma semana
    When consulto W como uno de ellos
    Then sólo sus reservas, sesiones, intervalos, capacidad y contador contribuyen a la respuesta
    And no se aceptan parámetros para elegir otro owner, proyecto o tarea

  @s21
  Scenario: Una lectura usa un snapshot y un único instante después de establecerlo
    Given una lectura W ha establecido su snapshot y un escritor confirma pausa/cierre y cambios de reservas después
    When termina W
    Then todas sus fuentes corresponden al snapshot anterior en una transacción read-only REPEATABLE_READ
    And Clock se consulta exactamente una vez después de establecer el snapshot y se trunca a microsegundos
    And no se suma simultáneamente el tramo cerrado nuevo y la cola running anterior
    And una lectura posterior puede ver la decisión completa sin modificar la primera respuesta

  @s22
  Scenario Outline: Un reloj atrasado no se oculta recortando hechos confirmados
    Given el único serverNow capturado precede<dato> seleccionado
    When consulto W
    Then recibo409 WEEKLY_REVIEW_TIME_OUT_OF_RANGE sin resumen parcial
    Examples:
      | dato                                                    |
      | changedAt confirmado de la sesión                       |
      | extremo confirmado del intervalo                        |
      | lastDecisionAt de EXTEND posterior a changedAt           |

  @s23
  Scenario Outline: Traducir fallos de lectura y conservar ausencia de escrituras
    Given se produce<fallo> durante W
    When termina la consulta
    Then recibo503 STORAGE_UNAVAILABLE sin SQL, stack ni resultados parciales
    And no hay cambios en reservas, sesiones, intervalos, preferencias, recibos ni outbox
    Examples:
      | fallo                         |
      | lectura SQL                   |
      | preferencia SQL ilegible      |
      | finalización transacción      |

  @s24
  Scenario: Reinicio y retirada de transporte no alteran un resumen durable
    Given guardé W de una semana pasada con sesiones cerradas y reservas vigentes
    And se retiró sólo su outbox y se reinició la API conservando la base de datos
    When consulto W con la misma selección y propietario
    Then días y totales durables son iguales aunque serverNow sea posterior
    And no se crean revisión persistida, eventos ni peticiones al broker

  @s25
  Scenario Outline: Rechazar el DTO semanal incompatible de forma integral
    Given una respuesta semanal contiene <defecto>
    When el cliente la recibe
    Then muestra error de respuesta y no semana vacía ni valores cero fabricados
    And comprueba longitud y rango decimal long antes de convertir a BigInt
    Examples:
      | defecto                                         |
      | campo adicional en envelope                     |
      | falta un campo de día                           |
      | sólo seis días                                  |
      | weekStart no es lunes                           |
      | fechas no consecutivas                          |
      | fronteras diarias discontinuas                   |
      | total distinto a suma diaria                    |
      | duración negativa o decimal no canónico          |
      | capacidad null inconsistente con su total       |
      | zona diferente de la explícitamente solicitada  |
      | instanteUTC con más de seis cifras fraccionarias |
      | duración "9223372036854775808" fuera del rango long |
      | contador decimal de 100000 cifras |
      | semana distinta al lunes civil de date solicitado |
      | sin date solicitado serverNow queda fuera de startAt inclusivo y endAt exclusivo |

  @s26
  Scenario: Conservar precisión y fronteras del servidor sin depender de TZDB cliente
    Given W válido tiene un tramo de"1000001"microsegundos y un instante terminado.123457Z
    And el catálogo del navegador difiere para una fecha histórica
    When el cliente valida y presenta la respuesta
    Then conserva exactamente suma y fracción usando aritmética entera
    And acepta instantesUTC válidos con cero a seis cifras fraccionarias, sin exigir seis siempre
    And no recalcula fronteras, fechas ni aritmética con Intl o Date
    And un fallback visualUTC se etiqueta sin cambiar los valores del servidor

  @s27
  Scenario: Entrar a revisión semanal no exige personalizar ni escribir
    Given estoy autenticado y no tengo datos ni disponibilidad
    When activo Revisión semanal desde navegación
    Then navego a /revision-semanal y el foco llega a su encabezado
    And veo siete días con ceros/capacidad desconocida y enlace a /proyectos para planificar
    And se explican Plan vigente, capacidad actual y ausencia de descanso real inferido
    And sólo se realizan consultas, sin crear preferencias ni revisión persistida

  @s28
  Scenario Outline: Cambiar semana o zona mantiene una selección explícita
    Given la pantalla muestra una semana y zona conocidas y su selección aplicada en URL
    And editar los borradores de fecha o zona no envía GET ni cambia la selección aplicada
    When activo <control>
    Then <seleccion>
    And la consulta nueva retira el resumen anterior si cambió la selección
    And Back y recarga conservan date y zoneId de la URL para reproducir la selección
    And no cambia disponibilidad ni introduce paginación de hechos
    Examples:
      | control                     | seleccion                                  |
      | Semana anterior             | se consulta la semana anterior              |
      | Semana siguiente            | se consulta la semana siguiente             |
      | Esta semana                 | se omite date en URL y GET conservando zoneId para resolver la semana del servidor |
      | Mostrar semana tras editar fecha nativa | se aplica date en URL y se consulta su lunes normalizado |
      | Mostrar semana tras elegir zona explícita | se aplica zoneId en URL y se consulta esa zona |
      | Mostrar semana tras elegir Zona de disponibilidad | se omite zoneId en URL y GET usando la preferencia vigente |

  @s29
  Scenario: Actualizar la misma selección conserva datos claramente anteriores
    Given veo una respuesta y la nueva consulta manual queda pendiente
    When activo Actualizar para la misma selección
    Then se anuncia carga antes de400ms y los datos conservados se marcan anteriores
    And se conserva Datos consultados a con el instante de la respuesta correspondiente
    And no se suma trabajo con reloj local ni se activa polling

  @s30
  Scenario Outline: El error conserva selección y permite una recuperación adecuada
    Given una consulta semanal con selección conocida termina con <error>
    Then se anuncia <feedback> y se conserva la selección para corregirla o recuperarla
    When <accion>
    Then se envía un único GET de <seleccion> y se anuncia espera
    And mientras espera otro intento no dispara duplicados
    And no se fabrica éxito ni se envían comandos históricos
    And un 400 de campo no reenvía automáticamente la query inválida
    Examples:
      | error | feedback | accion | seleccion |
      | 503 | error de lectura recuperable | activo Reintentar | la misma selección |
      | 400 VALIDATION_ERROR date INVALID_VALUE | error asociado a fecha | corrijo la fecha y activo Mostrar semana | la selección corregida |
      | 409 WEEKLY_REVIEW_TIME_OUT_OF_RANGE | límite temporal recuperable | activo Reintentar tras corregirse el reloj del servidor | la misma selección |

  @s31
  Scenario Outline: Una respuesta antigua no sustituye selección o identidad vigente
    Given una consulta anterior quedó pendiente en<etapa> y ya se muestra otra semana válida
    When termina la anterior con<resultado>
    Then no reemplaza datos ni revoca la identidad vigente
    And la solicitud sustituida fue abortada antes de activar la siguiente generación
    Examples:
      | etapa                        | resultado                 |
      | entrega de ResponseHTTP       | 401                       |
      | lectura JSON                  | 200 de la semana anterior |
      | clasificación de error        | error503 tardío           |

  @s32
  Scenario Outline: La pérdida actual de acceso retira datos semanales
    Given veo un resumen privado
    When ocurre<evento>
    Then desaparecen resumen y datos privados conservados
    And no reaparecen por una respuesta tardía ni se guardan en localStorage
    Examples:
      | evento                       |
      | logout                        |
      | 401 de la consulta vigente    |

  @s33
  Scenario: Actualizaciones y errores no roban el foco
    Given he movido deliberadamente el foco a un control mientras la consulta espera
    When llega una respuesta válida o error de esa actualización
    Then el anuncio accesible comunica su estado y el foco permanece en el control elegido
    And no se enfoca el encabezado en cada actualización

  @s34
  Scenario: Revisar siete días es accesible sin convertirlo en una puntuación
    Given se prepara la matriz30 de docs/ux-requirements.md para revisión semanal
    When recorro selección, vacío, carga, error y resumen con texto largo en tamaños320–2560 y bordes/altura400
    Then se comprueban áreas44px, foco/teclado, reflow, nombres, contraste y anuncios con evidencia por criterio
    And texto200,zoom nativo200 y motores pactados se registran con límites físicos explícitos
    And los días de presupuesto cero no se presentan como descanso real ni como penalización
    And no aparecen rachas, porcentaje de productividad, recomendaciones, notas o exportación20–30
