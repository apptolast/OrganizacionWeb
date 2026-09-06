@availability @draft
Feature: Definir zona y presupuesto diario personal sin reservar horarios
  Preparación documental: feature 10 sigue pending y este archivo no aprueba implementación.
  Borrador de 47 escenarios, 237 casos expandidos de Examples, un When por escenario; sujeto a revisión del coordinador.
  GET /api/v1/me/availability devuelve exactamente configured, zoneId, dailyMinutes y updatedAt.
  Ausencia es configured false y los otros tres campos null, con ETag fuerte "availability:unconfigured"; leer no inserta.
  Preferencia guardada es configured true, zona textual, mapa de siete días y updatedAt UTC.
  ETag configurado: "availability:<UUID canónico minúsculo>:<versión decimal canónica BIGINT no negativa>".
  Primer INSERT usa versión 0; cambios reales suman 1 y no-op conserva revisión/fechas.
  PUT /api/v1/me/availability reemplaza exactamente zoneId y dailyMinutes.
  Días exactos: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY; cada presupuesto es entero JSON de 0 a 1440 minutos.
  Zona admitida: pertenencia exacta a catálogo backend getAvailableZoneIds más UTC, sin filtros de aliases, sin SHORT_IDS adicional ni offsets libres.
  GET /api/v1/me/availability/zones devuelve exactamente items, array ordenado de IDs únicos; cliente no usa Intl como autoridad.
  Endpoints nuevos requieren sesión y no aceptan query params. No se recibe propietario del cliente ni se inventa un proyecto.
  Filtros de sesión, Origin y CSRF conservan precedencia existente; negociación puede devolver 415 antes del handler.
  En handler se rechaza query desconocida antes de If-Match; PUT valida precondición antes de JSON.
  Sintaxis JSON precede forma; extras del objeto raíz preceden zoneId; luego se valida dailyMinutes como objeto, sus extras y cada día de lunes a domingo.
  Dentro de cada campo: ausencia/null REQUIRED, tipo INVALID_TYPE, luego pertenencia INVALID_VALUE o rango OUT_OF_RANGE; extra UNKNOWN_FIELD.
  Entre extras se informa primero por orden léxico. Error de campo usa zoneId, dailyMinutes o dailyMinutes.MONDAY, por ejemplo.
  Resuelta petición válida, fila propia desde sesión; comprobar revisión antes del no-op dentro de transacción.
  Fecha inicial es reloj UTC truncado a microsegundos; update usa máximo entre reloj truncado y updatedAt previo.
  Sin evento de disponibilidad, ventanas, reservas, autosave, sincronización externa ni reescritura de hechos históricos.
  Ruta UI exacta /disponibilidad, sin query ni sufijos, también para retorno autenticado; Cancelar y volver a Proyectos lleva a /proyectos.
  Aviso permanente: Los cambios sin guardar se pierden al salir. Descarte explícito en acciones del formulario, sin guardia global ni beforeunload nuevo.
  Campos y Guardar se bloquean durante PUT o recuperación; no se sobrescriben ediciones hechas durante una espera.
  Tras 412, 503, red o confirmación inválida se exige recargar antes de otra escritura; 400 de campo permite corregir.
  GET rehidrata la zona histórica como texto sin consultar el catálogo ni resolver ZoneId; PUT exige pertenencia actual incluso para no-op.
  INSERT que afecta cero por colisión con fila propia existente es 412; supresión sin fila resultante es 503, nunca sobrescribe al ganador.
  Fallo de COMMIT significa rechazo antes de confirmar mediante constraint trigger diferido; pérdida de ACK puede dejar resultado incierto y exige consulta, no promesa de rollback.
  Cada fila de Examples es un caso independiente; en rechazos lo no mencionado permanece válido.

  Background:
    Given la sesión válida pertenece a "persona-a"
    And el presupuesto válido base contiene 60 minutos en cada uno de los siete días
    And "Europe/Madrid" y "UTC" pertenecen al catálogo backend de prueba

  @s1
  Scenario: Leer ausencia sin persistir sugerencias
    Given persona-a no tiene fila de disponibilidad
    When consulto GET "/api/v1/me/availability"
    Then recibo 200 con configured false, zoneId null, dailyMinutes null y updatedAt null
    And el ETag es "availability:unconfigured" y no se inserta ninguna fila

  @s2
  Scenario: Exponer catálogo exacto sin inventar aliases
    Given el proveedor backend tiene un catálogo TZDB con IDs de región y aliases históricos
    When consulto GET "/api/v1/me/availability/zones"
    Then recibo 200 con exactamente items igual al catálogo más UTC, ordenado y sin duplicados
    And no se filtran IDs por longitud ni por contener barra ni se añade SHORT_IDS

  @s3
  Scenario Outline: Guardar por primera vez con identidad propia
    Given persona-a no tiene fila de disponibilidad
    When envío PUT válido con zona <zona> y If-Match "availability:unconfigured"
    Then recibo 200 con configured true, zona exacta y los siete presupuestos confirmados
    And hay una fila de persona-a con UUID propio, versión 0 y createdAt igual a updatedAt UTC en microsegundos
    And cuerpo y ETag configurado corresponden al mismo snapshot
    Examples:
      | zona          |
      | Europe/Madrid |
      | UTC           |

  @s4
  Scenario: Conservar un alias que pertenece al catálogo
    Given el catálogo publicado incluye el alias TZDB "CET"
    When guardo la zona "CET" con presupuestos y revisión válidos
    Then se conserva exactamente "CET" sin sustituirlo por otro ID ni un offset

  @s5
  Scenario Outline: Admitir fronteras en cada presupuesto diario
    Given una revisión vigente y los demás días con el presupuesto válido base
    When guardo <dia> con <minutos> minutos
    Then el presupuesto confirmado de <dia> es exactamente <minutos>
    Examples:
      | dia       | minutos |
      | MONDAY    | 0       |
      | MONDAY    | 1440    |
      | TUESDAY   | 0       |
      | TUESDAY   | 1440    |
      | WEDNESDAY | 0       |
      | WEDNESDAY | 1440    |
      | THURSDAY  | 0       |
      | THURSDAY  | 1440    |
      | FRIDAY    | 0       |
      | FRIDAY    | 1440    |
      | SATURDAY  | 0       |
      | SATURDAY  | 1440    |
      | SUNDAY    | 0       |
      | SUNDAY    | 1440    |

  @s6
  Scenario Outline: Permitir descanso completo y calcular total sin duplicarlo
    Given una revisión vigente
    When guardo los siete días con <minutos> minutos cada uno
    Then el guardado confirma sin penalización y el total derivado es <total>
    And no se persiste un campo separado para el total semanal
    Examples:
      | minutos | total |
      | 0       | 0     |
      | 1440    | 10080 |

  @s7
  Scenario: Actualizar sólo preferencias con versión siguiente
    Given una fila propia en versión 0 con Europe/Madrid y siete presupuestos de 60 minutos
    When guardo UTC y siete presupuestos de 30 minutos con su ETag vigente
    Then recibo 200 con versión 1, mismo UUID y createdAt, zona y presupuestos nuevos
    And updatedAt corresponde al mismo snapshot que el ETag

  @s8
  Scenario: No-op conserva hechos confirmados
    Given una fila propia y su ETag vigente
    When guardo exactamente su zona y presupuestos actuales
    Then recibo 200 con cuerpo y ETag idénticos, sin cambiar createdAt, updatedAt ni versión

  @s9
  Scenario Outline: Mantener fecha no decreciente
    Given una fila propia con updatedAt confirmado y revisión vigente
    And el reloj de prueba <condicion>
    When guardo un presupuesto diferente
    Then versión aumenta una vez y updatedAt es el máximo entre reloj truncado a microsegundos y fecha previa
    Examples:
      | condicion                       |
      | avanza con precisión nanosegundo |
      | coincide en el microsegundo      |
      | retrocede                       |

  @s10
  Scenario Outline: Rechazar precondiciones ambiguas antes del cuerpo
    Given un cuerpo JSON mal formado y precondición <defecto>
    When envío PUT de disponibilidad
    Then recibo <http> con <codigo> sin escrituras
    And si hay error de validación usa If-Match con INVALID_VALUE
    Examples:
      | defecto                   | http | codigo                |
      | encabezado ausente         | 428  | PRECONDITION_REQUIRED |
      | cadena vacía              | 400  | VALIDATION_ERROR      |
      | asterisco                 | 400  | VALIDATION_ERROR      |
      | tag débil                 | 400  | VALIDATION_ERROR      |
      | dos encabezados           | 400  | VALIDATION_ERROR      |
      | lista de dos tags         | 400  | VALIDATION_ERROR      |
      | tag de proyecto           | 400  | VALIDATION_ERROR      |
      | UUID incompleto           | 400  | VALIDATION_ERROR      |
      | UUID mayúsculo            | 400  | VALIDATION_ERROR      |
      | versión -1                | 400  | VALIDATION_ERROR      |
      | versión 01                | 400  | VALIDATION_ERROR      |
      | versión 1.5               | 400  | VALIDATION_ERROR      |
      | versión 9223372036854775808| 400  | VALIDATION_ERROR      |

  @s11
  Scenario Outline: Rechazar revisión válida que ya no representa la preferencia propia
    Given la fila propia <existencia> y <precondicion>
    When guardo con ese If-Match y cuerpo válido <intencion>
    Then recibo 412 AVAILABILITY_CONFLICT y la fila permanece idéntica
    Examples:
      | existencia | precondicion                         | intencion                       |
      | existe     | tag anterior de la misma fila         | diferente del contenido actual |
      | existe     | tag anterior de la misma fila         | igual al contenido actual      |
      | existe     | tag availability:unconfigured         | igual al contenido actual      |
      | existe     | tag válido con UUID de otro usuario   | diferente del contenido actual |
      | existe     | tag válido con UUID inexistente       | diferente del contenido actual |
      | no existe  | tag configurado de UUID inexistente   | presupuesto válido base        |

  @s12
  Scenario Outline: Rechazar sintaxis JSON sin escrituras
    Given una revisión vigente
    When envío PUT con <cuerpo>
    Then recibo 400 MALFORMED_JSON sin cambiar fila ni fechas
    Examples:
      | cuerpo                        |
      | cuerpo vacío                  |
      | sólo espacios                 |
      | documento truncado            |
      | dos documentos concatenados   |
      | clave zoneId duplicada        |
      | clave dailyMinutes duplicada  |
      | clave MONDAY duplicada        |
      | clave TUESDAY duplicada       |
      | clave WEDNESDAY duplicada     |
      | clave THURSDAY duplicada      |
      | clave FRIDAY duplicada        |
      | clave SATURDAY duplicada      |
      | clave SUNDAY duplicada        |

  @s13
  Scenario Outline: Rechazar formas y campos de preferencia inválidos
    Given una revisión vigente
    When el cuerpo contiene <defecto>
    Then recibo 400 VALIDATION_ERROR en <campo> con <codigo> sin escrituras
    Examples:
      | defecto                         | campo        | codigo        |
      | raíz null                       | body         | INVALID_TYPE  |
      | raíz array                      | body         | INVALID_TYPE  |
      | raíz textual                    | body         | INVALID_TYPE  |
      | raíz numérica                   | body         | INVALID_TYPE  |
      | raíz booleana                   | body         | INVALID_TYPE  |
      | zoneId ausente                  | zoneId       | REQUIRED      |
      | zoneId null                     | zoneId       | REQUIRED      |
      | zoneId numérico                 | zoneId       | INVALID_TYPE  |
      | zoneId booleano                 | zoneId       | INVALID_TYPE  |
      | zoneId array                    | zoneId       | INVALID_TYPE  |
      | zoneId objeto                   | zoneId       | INVALID_TYPE  |
      | zoneId vacío                    | zoneId       | INVALID_VALUE |
      | zoneId Europe/Madrid con espacios| zoneId       | INVALID_VALUE |
      | zoneId europe/madrid            | zoneId       | INVALID_VALUE |
      | zoneId +02:00                   | zoneId       | INVALID_VALUE |
      | zoneId UTC+02:00                | zoneId       | INVALID_VALUE |
      | zoneId inexistente              | zoneId       | INVALID_VALUE |
      | dailyMinutes ausente            | dailyMinutes | REQUIRED      |
      | dailyMinutes null               | dailyMinutes | REQUIRED      |
      | dailyMinutes array              | dailyMinutes | INVALID_TYPE  |
      | dailyMinutes texto              | dailyMinutes | INVALID_TYPE  |
      | dailyMinutes número             | dailyMinutes | INVALID_TYPE  |
      | dailyMinutes booleano           | dailyMinutes | INVALID_TYPE  |
      | extra ownerId en raíz           | ownerId      | UNKNOWN_FIELD |
      | extra configured en raíz        | configured   | UNKNOWN_FIELD |
      | extra windows en raíz           | windows      | UNKNOWN_FIELD |

  @s14
  Scenario Outline: Exigir cada día exacto y validar su rango
    Given una revisión vigente
    When dailyMinutes contiene <defecto> en <dia>
    Then recibo 400 VALIDATION_ERROR en dailyMinutes.<dia> con <codigo> sin escrituras
    Examples:
      | dia       | defecto | codigo       |
      | MONDAY    | ausente | REQUIRED     |
      | TUESDAY   | ausente | REQUIRED     |
      | WEDNESDAY | ausente | REQUIRED     |
      | THURSDAY  | ausente | REQUIRED     |
      | FRIDAY    | ausente | REQUIRED     |
      | SATURDAY  | ausente | REQUIRED     |
      | SUNDAY    | ausente | REQUIRED     |
      | MONDAY    | -1      | OUT_OF_RANGE |
      | TUESDAY   | -1      | OUT_OF_RANGE |
      | WEDNESDAY | -1      | OUT_OF_RANGE |
      | THURSDAY  | -1      | OUT_OF_RANGE |
      | FRIDAY    | -1      | OUT_OF_RANGE |
      | SATURDAY  | -1      | OUT_OF_RANGE |
      | SUNDAY    | -1      | OUT_OF_RANGE |

  @s15
  Scenario Outline: No convertir valores diarios incorrectos
    Given una revisión vigente y los otros seis días válidos
    When dailyMinutes.MONDAY contiene <valor>
    Then recibo 400 VALIDATION_ERROR en dailyMinutes.MONDAY con <codigo> sin escrituras
    Examples:
      | valor                 | codigo       |
      | null                  | REQUIRED     |
      | 1441                  | OUT_OF_RANGE |
      | 9223372036854775808    | OUT_OF_RANGE |
      | 1.5                   | INVALID_TYPE |
      | 1.0                   | INVALID_TYPE |
      | "60"                  | INVALID_TYPE |
      | ""                    | INVALID_TYPE |
      | true                  | INVALID_TYPE |
      | []                    | INVALID_TYPE |
      | {}                    | INVALID_TYPE |

  @s16
  Scenario Outline: Rechazar claves diarias adicionales sin traducirlas
    Given una revisión vigente y los siete días válidos
    When añado la clave <clave> a dailyMinutes
    Then recibo 400 VALIDATION_ERROR en dailyMinutes.<clave> con UNKNOWN_FIELD sin escrituras
    Examples:
      | clave       |
      | monday      |
      | LUNES       |
      | HOLIDAY     |
      | weeklyTotal |

  @s17
  Scenario Outline: Resolver precedencias de error explícitas
    Given una petición con <defectos>
    When envío PUT de disponibilidad
    Then recibo <http> con <resultado> sin escrituras
    Examples:
      | defectos                                           | http | resultado                         |
      | sin sesión y sin If-Match                           | 401  | UNAUTHENTICATED                    |
      | CSRF inválido y JSON truncado                       | 403  | CSRF_INVALID                      |
      | media type text/plain y sin If-Match                 | 415  | UNSUPPORTED_MEDIA_TYPE            |
      | query limit=1 y sin If-Match                         | 400  | query INVALID_VALUE               |
      | If-Match inválido y JSON truncado                    | 400  | If-Match INVALID_VALUE            |
      | JSON truncado y zoneId de tipo número                | 400  | MALFORMED_JSON                    |
      | extra root z y extra root a y zoneId ausente         | 400  | a UNKNOWN_FIELD                   |
      | zoneId ausente y dailyMinutes ausente                | 400  | zoneId REQUIRED                   |
      | dailyMinutes extra z y extra a y MONDAY ausente      | 400  | dailyMinutes.a UNKNOWN_FIELD      |
      | MONDAY -1 y TUESDAY ausente                          | 400  | dailyMinutes.MONDAY OUT_OF_RANGE   |
      | revisión anterior y JSON válido igual al actual      | 412  | AVAILABILITY_CONFLICT             |

  @s18
  Scenario Outline: Guardar sin evento ni efectos fuera de preferencias
    Given proyectos, tareas e historia existentes y seis tipos de eventos existentes
    When realizo <operacion> de disponibilidad
    Then las tablas ajenas y el outbox permanecen idénticos, sin proyecto ficticio ni evento personal
    Examples:
      | operacion         |
      | primer guardado   |
      | cambio real       |
      | no-op             |

  @s19
  Scenario: Resolver creación simultánea desde ausencia
    Given dos peticiones reales de la misma sesión parten de availability:unconfigured
    When ambas intentan insertar preferencias distintas concurrentemente en PostgreSQL
    Then una recibe 200 y otra 412 AVAILABILITY_CONFLICT
    And existe una sola fila propia en versión 0 con el contenido completo del ganador

  @s20
  Scenario: Resolver edición simultánea sin perder una escritura
    Given dos peticiones reales de la misma sesión usan el mismo ETag vigente
    When ambas intentan cambiar presupuestos concurrentemente en PostgreSQL
    Then una recibe 200 y otra 412 AVAILABILITY_CONFLICT
    And queda una actualización completa y una sola versión adicional

  @s21
  Scenario: Revisar no-op después de un escritor concurrente
    Given un escritor mantiene un cambio real bajo bloqueo PostgreSQL de la fila propia
    And otra petición espera con la revisión anterior y valores iguales a su snapshot anterior
    When el escritor confirma y se libera la petición que esperaba
    Then la petición que esperaba recibe 412 sin deshacer el cambio confirmado

  @s22
  Scenario Outline: Resolver usuario exclusivamente desde sesión
    Given persona-a y persona-b tienen preferencias distintas
    When persona-a ejecuta <operacion> sin identificador de propietario en la ruta
    Then sólo se consulta o modifica su propia fila y la fila de persona-b queda idéntica
    Examples:
      | operacion |
      | GET       |
      | PUT       |

  @s23
  Scenario Outline: Exigir sesión también con cookie expirada
    Given <sesion>
    When solicito <operacion>
    Then recibo 401 UNAUTHENTICATED sin preferencias ni catálogo y sin WWW-Authenticate
    Examples:
      | sesion                        | operacion |
      | sesión ausente                | GET       |
      | sesión expirada en PostgreSQL | GET       |
      | sesión ausente                | GET zones |
      | sesión expirada en PostgreSQL | GET zones |
      | sesión ausente                | PUT       |
      | sesión expirada en PostgreSQL | PUT       |

  @s24
  Scenario Outline: Mantener protección de escritura
    Given una sesión válida y <defecto>
    When envío PUT válido de disponibilidad
    Then recibo 403 con <codigo> sin escrituras
    Examples:
      | defecto         | codigo            |
      | CSRF ausente    | CSRF_INVALID      |
      | CSRF incorrecto | CSRF_INVALID      |
      | Origin ajeno    | UNTRUSTED_ORIGIN  |

  @s25
  Scenario Outline: No convertir fallo de almacenamiento en ausencia o éxito
    Given <fallo> real inducido en PostgreSQL con fixture aislado
    When ejecuto <operacion> válida
    Then recibo 503 STORAGE_UNAVAILABLE sin ETag de confirmación falso
    And fila previa y demás tablas permanecen idénticas
    Examples:
      | fallo                          | operacion       |
      | consulta de preferencia falla   | GET             |
      | INSERT lanza excepción          | primer PUT      |
      | INSERT suprimido sin fila propia| primer PUT      |
      | UPDATE lanza excepción          | cambio PUT      |
      | UPDATE afecta cero filas        | cambio PUT      |
      | constraint trigger diferido rechaza COMMIT | cambio PUT |

  @s26
  Scenario Outline: No admitir query params en endpoints cerrados
    Given una sesión válida
    When solicito <operacion> con limit=1
    Then recibo 400 VALIDATION_ERROR en query con INVALID_VALUE
    Examples:
      | operacion |
      | GET       |
      | GET zones |
      | PUT       |

  @s27
  Scenario Outline: Conservar no-store en nuevas respuestas
    Given una petición que produce la respuesta indicada
    When solicito <operacion>
    Then recibo HTTP <http> y Cache-Control contiene no-store
    Examples:
      | operacion | http |
      | GET       | 200  |
      | GET       | 400  |
      | GET       | 401  |
      | GET       | 503  |
      | GET zones | 200  |
      | GET zones | 400  |
      | GET zones | 401  |
      | PUT       | 200  |
      | PUT       | 400  |
      | PUT       | 401  |
      | PUT       | 403  |
      | PUT       | 412  |
      | PUT       | 415  |
      | PUT       | 428  |
      | PUT       | 503  |

  @s28
  Scenario: Recuperar preferencias tras reinicio real
    Given una preferencia propia confirmada y una sesión persistida
    When reinicio el backend conservando PostgreSQL y vuelvo a consultar con la misma sesión
    Then cuerpo y ETag son idénticos y no se crean nuevos valores por leer

  @s29
  Scenario: Presentar ausencia y sugerencia como borrador
    Given GET confirma ausencia y la zona del navegador pertenece al catálogo recibido
    When abro la vista Disponibilidad
    Then veo Sin configurar, la sugerencia de zona como no guardada y siete ceros editables
    And cero permite descansar sin advertencia de incumplimiento y no se realiza PUT
    And puedo guardar ese borrador inicial válido sin tener que modificar antes un campo

  @s30
  Scenario Outline: No inventar una zona válida ante problemas del catálogo
    Given <situacion>
    When abro la vista Disponibilidad
    Then ocurre <resultado> sin guardar ni sustituir preferencias por UTC automáticamente
    Examples:
      | situacion                                    | resultado                                  |
      | zona del navegador fuera del catálogo        | selector sin selección                     |
      | navegador no puede resolver su zona          | selector sin selección                     |
      | GET zones falla por red                      | error y Reintentar zonas                    |
      | GET zones devuelve cuerpo inválido           | error y Reintentar zonas                    |
      | zona guardada ya no pertenece al catálogo    | valor guardado visible como no disponible   |

  @s31
  Scenario: Confirmar sólo después del guardado
    Given un formulario válido modificado y un PUT retenido
    When activo Guardar disponibilidad con teclado
    Then aparece Guardando disponibilidad antes de 400 ms y sólo se envía una petición
    And no aparece Disponibilidad guardada antes de HTTP 200 válido con ETag
    And al liberar una respuesta válida el snapshot y formulario muestran los valores confirmados

  @s32
  Scenario Outline: Conservar borrador recuperable sin reenvío automático
    Given una zona y siete presupuestos modificados
    When el PUT responde <fallo>
    Then conservo el borrador con error accesible, sin falso éxito ni PUT automático
    And Guardar permanece bloqueado hasta una recuperación válida, salvo 400 de campo que permite corregir y enviar de nuevo
    Examples:
      | fallo                           |
      | HTTP 400 con error de campo      |
      | HTTP 412 AVAILABILITY_CONFLICT   |
      | HTTP 503                        |
      | pérdida de conexión              |
      | HTTP 200 con cuerpo inválido     |
      | HTTP 200 sin ETag válido         |

  @s33
  Scenario: Descartar borrador tras conflicto sólo mediante acción explícita
    Given un conflicto conserva el borrador y ofrece Recargar versión guardada
    When activo Recargar versión guardada
    Then se consulta GET sin repetir PUT y sólo una respuesta válida sustituye formulario y ETag
    And la acción explica que descarta cambios sin guardar, igual que el patrón de edición existente
    And no se ofrece comparación, fusión ni reenvío automático del borrador anterior

  @s34
  Scenario: Mantener borrador si falla la recuperación explícita
    Given un conflicto conserva cambios sin guardar
    When Recargar versión guardada falla por red
    Then el borrador permanece y puedo reintentar la consulta sin enviar PUT

  @s35
  Scenario: Cancelar no guarda el formulario
    Given cambios sin guardar y la acción Cancelar y volver a Proyectos que comunica descartarlos
    When activo Cancelar y volver a Proyectos
    Then navego a /proyectos sin PUT y el snapshot persistido permanece idéntico

  @s36
  Scenario Outline: Retirar datos privados tras pérdida de sesión
    Given hay preferencias confirmadas y un borrador modificado
    When <respuesta>
    Then la vista privada y el borrador se retiran y no vuelven por una respuesta tardía
    Examples:
      | respuesta                              |
      | GET disponibilidad devuelve 401        |
      | GET zones devuelve 401                  |
      | PUT devuelve 401                       |
      | se confirma logout en otra pestaña     |

  @s37
  Scenario Outline: Ignorar respuestas antiguas de la vista
    Given una petición anterior está retenida
    When <secuencia>
    Then la respuesta antigua no cambia la vista vigente ni repite escrituras
    Examples:
      | secuencia                                                     |
      | navego fuera y llega éxito del GET disponibilidad anterior       |
      | navego fuera y llega rechazo del GET disponibilidad anterior     |
      | navego fuera y llega éxito del GET zones anterior                |
      | navego fuera y llega rechazo del GET zones anterior              |
      | cierro sesión y llega éxito del PUT anterior                     |
      | cierro sesión y llega rechazo del PUT anterior                   |
      | recupero acceso y llega 401 antiguo de GET disponibilidad         |
      | recupero acceso y llega 401 antiguo de GET zones                  |
      | recupero acceso y llega 401 antiguo de PUT                        |
      | confirmo PUT y llega GET previo con contenido antiguo            |

  @s38
  Scenario: No interpretar entrada numérica incompleta como descanso
    Given los otros campos son válidos
    When escribo 1e con teclado real en un presupuesto y activo Guardar disponibilidad
    Then no se envía PUT ni se sustituye la entrada incompleta por cero
    And el error se asocia al campo con foco visible

  @s39
  Scenario: Seleccionar zona y días con controles accesibles
    Given selector nativo con IDs largos y siete presupuestos visibles
    When recorro la vista con teclado, táctil, los 22 anchos UX existentes y zoom nativo 200 por ciento a 320 CSS
    Then etiquetas, errores, orden de lunes a domingo y foco son claros, sin desbordamiento de página
    And controles tienen al menos 44 por 44 CSS y axe no encuentra violaciones en las reglas ejecutadas
    And la matriz de treinta principios distingue presupuesto de ventana, descanso de incumplimiento y planificación de trabajo realizado

  @s40
  Scenario: Calcular total del borrador sin anunciar progreso ganado
    Given tengo siete presupuestos enteros válidos modificados sin guardar
    When observo el resumen semanal
    Then la suma corresponde al borrador y está etiquetada como no guardada
    And no se presenta como tiempo trabajado, bloques reservados ni objetivo obligatorio

  @s41
  Scenario Outline: Rechazar snapshot inicial o ETag incoherente sin inventar ausencia
    Given GET disponibilidad devuelve HTTP 200 con <defecto>
    When abro /disponibilidad
    Then veo error recuperable, no se habilita Guardar y no se inventan valores guardados ni ausencia
    Examples:
      | defecto                                    |
      | campo configured ausente                    |
      | campo zoneId ausente                        |
      | campo dailyMinutes ausente                  |
      | campo updatedAt ausente                     |
      | campo extra                                 |
      | configured no booleano                      |
      | configured false con zona no null           |
      | configured false con dailyMinutes no null   |
      | configured false con updatedAt no null      |
      | configured true con zona no textual         |
      | configured true con mapa incompleto         |
      | configured true con minutos fuera de rango  |
      | configured true con fecha UTC inválida      |
      | ETag ausente                                |
      | ETag débil                                  |
      | ETag mal formado                            |
      | tag de ausencia y cuerpo configurado        |
      | tag configurado y cuerpo de ausencia        |

  @s42
  Scenario Outline: No sumar presupuestos incompletos
    Given seis presupuestos válidos y uno <valor>
    When observo el resumen semanal
    Then no aparece una suma parcial ni un total numérico
    And veo Completa los siete presupuestos para calcular el total
    Examples:
      | valor         |
      | vacío         |
      | incompleto 1e |
      | -1            |
      | 1441          |
      | 1.5           |

  @s43
  Scenario Outline: Bloquear edición mientras se resuelve la intención enviada
    Given un formulario con borrador válido y <peticion> retenida
    When intento editar sus campos o activar Guardar disponibilidad
    Then los siete presupuestos, la zona y Guardar están bloqueados sin escrituras adicionales
    And permanecen visibles los valores enviados hasta resultado o recuperación válida
    Examples:
      | peticion                         |
      | PUT                              |
      | GET de Recargar versión guardada |

  @s44
  Scenario Outline: No confirmar una respuesta válida que contradice la intención
    Given un PUT enviado con zona y siete presupuestos válidos
    When recibo HTTP 200 de estructura válida con <diferencia>
    Then no aparece Disponibilidad guardada y el borrador enviado permanece con error
    And se exige Recargar versión guardada antes de otra escritura, sin reenviar PUT
    Examples:
      | diferencia                            |
      | otra zoneId                           |
      | otro presupuesto MONDAY               |
      | otro presupuesto TUESDAY              |
      | otro presupuesto WEDNESDAY            |
      | otro presupuesto THURSDAY             |
      | otro presupuesto FRIDAY               |
      | otro presupuesto SATURDAY             |
      | otro presupuesto SUNDAY               |
      | ausencia explícita con tag de ausencia |

  @s45
  Scenario: Leer zona histórica aunque ya no esté disponible
    Given una fila propia contiene una zona que el catálogo actual ya no incluye
    When consulto GET disponibilidad
    Then recibo 200 con el texto histórico y ETag confirmado sin invocar resolución de esa zona
    And la lectura no reescribe la fila ni sustituye la zona por UTC

  @s46
  Scenario: Validar catálogo vigente también al intentar no-op
    Given una fila propia conserva una zona fuera del catálogo actual y su ETag vigente
    When envío PUT con exactamente sus valores anteriores
    Then recibo 400 VALIDATION_ERROR en zoneId con INVALID_VALUE sin escrituras

  @s47
  Scenario Outline: Abrir la ruta exacta con aviso de borrador
    Given acceso a la aplicación mediante <entrada>
    When llego a /disponibilidad
    Then veo la vista Disponibilidad y el aviso Los cambios sin guardar se pierden al salir
    And Cancelar y volver a Proyectos tiene destino /proyectos, sin history.back variable ni nueva guardia global
    Examples:
      | entrada                       |
      | enlace de navegación          |
      | URL directa autenticada       |
      | retorno tras iniciar sesión   |
