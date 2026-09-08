Feature: Descargar un archivo privado y completo de los datos de organización
  Como persona autenticada quiero obtener mis datos durables en JSON versionado
  para conservarlos fuera del servicio sin modificar mi trabajo.
  El esquema cerrado y las catorce colecciones son los de la sección 22.
  Descargar no acredita importación, restauración ni guardado en disco por el navegador.

  @s1
  Scenario: Una cuenta vacía obtiene un documento completo sin crear defaults
    Given una persona autenticada sin datos ni preferencias persistidas
    When consulta GET /api/v1/me/export
    Then recibe 200 con JSON UTF-8 sin BOM y exactamente format, schemaVersion, exportedAt, owner, data y counts
    And format es organizationweb-export, schemaVersion es el número 1 y owner es su identidad
    And data contiene exactamente las catorce colecciones de la sección 22 como arrays vacíos y counts contiene sus catorce ceros
    And no se crea ninguna preferencia, fila de negocio ni evento

  @s2
  Scenario: Exportar todas las familias conserva campos e identidades propias
    Given datos propios válidos en las catorce colecciones y datos diferentes de otra identidad
    And hay proyectos terminados, tareas terminadas y subtareas con su parentId
    When prepara su exportación
    Then cada fila propia aparece una sola vez con exactamente los campos de su colección definidos en la sección 22
    And los valores, IDs y relaciones corresponden al dato persistido, incluido projectId de valores TASK derivado de su tarea
    And cada count coincide con la longitud de su array y todas las referencias resuelven dentro del archivo
    And no aparecen filas, nombres ni valores de la otra identidad

  @s3
  Scenario: La exportación excluye infraestructura aunque exista actividad publicada
    Given una cuenta con datos de negocio, sesiones de acceso, secretos y eventos de outbox publicados y pendientes
    And RabbitMQ no está disponible
    When prepara su exportación
    Then recibe sus datos de negocio con 200 sin depender del publicador
    And no aparecen sesiones de acceso, cookies, CSRF, credenciales, secretos, outbox, DLQ ni backups
    And no aparecen copias derivadas de Hoy, Historial o Revisión semanal
    And datos de negocio, preferencias y eventos conservan su contenido e identidad

  @s4
  Scenario: Una escritura concurrente no mezcla versiones de un mismo snapshot
    Given una definición y sus valores propios coherentes antes de comenzar la exportación
    And durante la lectura otra transacción confirma conjuntamente un cambio de definición y valores
    When termina la preparación del archivo
    Then el documento y sus counts corresponden íntegramente a un único snapshot
    And no combina el esquema posterior con los valores anteriores ni incorpora sólo una parte de la transacción concurrente

  @s5
  Scenario Outline: Una incoherencia propia impide entregar un archivo aparentemente completo
    Given datos propios con <defecto>
    When prepara su exportación
    Then recibe 503 STORAGE_UNAVAILABLE sin bytes de descarga ni Content-Disposition
    And no omite la fila defectuosa ni incluye datos ajenos para completar relaciones
    And el problema no revela datos privados ni trazas
    Examples:
      | defecto |
      | una relación de tarea con proyecto ajeno |
      | un valor cuyo fieldId no pertenece al esquema del mismo ámbito |
      | un recibo cuyo ID o instante contradice su fila |
      | un JSONB de recibo con estructura inválida |

  @s6
  Scenario: Personalización conserva valores ocultos, inactivos, ausentes y null
    Given configuraciones PROJECT y TASK con orden de columnas propio y definiciones activas e inactivas
    And sus valores incluyen NUMBER 0, BOOLEAN false, DATE 0001-01-01, texto con espacios y un null explícito
    And una definición recién creada no tiene aún valor persistido
    When prepara su exportación
    Then conserva visibleFields y customFields en su orden guardado, incluidos los cuatro campos de cada definición inactiva
    And exporta los pares fieldId y value persistidos con sus tipos y espacios exactos, también los inactivos
    And conserva el null y no añade un par para el valor ausente

  @s7
  Scenario: El orden del archivo es determinista sin confundir UUID con cronología
    Given filas introducidas en distinto orden con instantes que no siguen el orden de sus UUID
    And intervalos de una sesión con revisiones 2 y 10 y ambas configuraciones TASK y PROJECT
    When prepara su exportación
    Then cada colección sigue UUID canónico ascendente de id salvo las excepciones de la sección 22
    And blockProjections sigue blockId, los intervalos siguen sessionId y revisión numérica 2 antes de 10 y customization sigue PROJECT antes de TASK
    And los pares de valores siguen fieldId y no se reordenan las definiciones ni las columnas visibles

  @s8
  Scenario: Reserva original, proyección y ausencia de proyección permanecen separadas
    Given una reserva reprogramada con su proyección y otra reserva nunca modificada sin proyección
    When prepara su exportación
    Then plannedBlocks conserva ambas reservas originales y blockProjections contiene sólo la proyección persistida
    And no sustituye el horario original por el actual ni fabrica una proyección para la segunda reserva

  @s9
  Scenario Outline: Los recibos de bloque conservan intención y resolución históricas completas
    Given un recibo durable <kind> con before completo y <after>
    And el snapshot histórico tiene offsets de request null y offsets de time resueltos, locales propios y allowOverBudget true
    When prepara su exportación
    Then receipt tiene exactamente id, blockId, kind, version textual, occurredAt, before y after
    And conserva kind <kind> y <after> sin convertir version en un ETag revision
    And cada snapshot conserva PlannedBlock6, request7 y time5 con los campos exactos de la sección 22
    And no completa los offsets null de intención ni elimina los offsets resueltos, locales o allowOverBudget
    Examples:
      | kind        | after                         |
      | RESCHEDULED | after como snapshot completo  |
      | CANCELLED   | after null                    |

  @s10
  Scenario Outline: Los recibos de sesión conservan su forma histórica discriminada
    Given un recibo válido de sesión con acción <accion> y <detalle>
    When prepara su exportación
    Then conserva id, sessionId, action, occurredAt, before y after con sus State6 y SessionStart7 originales
    And conserva <salida> sin añadir campos de otra acción
    And coinciden IDs, revisiones e instantes duplicados entre recibo y fila
    Examples:
      | accion | detalle                                                       | salida                                                   |
      | PAUSE  | un intervalo de trabajo cerrado                               | sólo los seis campos comunes                             |
      | RESUME | un nuevo runningSince                                         | sólo los seis campos comunes                             |
      | CLOSE  | notas con espacios y workDate distinto del día UTC del cierre  | closure con progressNote, nextStep, workDate y closeZoneId |
      | EXTEND | un fin original distinto del acordado                          | extension con additionalMinutes, previousEndAt y effectiveEndAt |

  @s11
  Scenario: Sesiones legadas y abiertas no se completan con historia inventada
    Given una sesión antigua closed sin recibo de cierre y con metadatos históricos null admitidos
    And una sesión running con intervalos cerrados, workedMicroseconds persistido y runningSince anterior a exportedAt
    When prepara su exportación
    Then conserva los null históricos y no fabrica recibo, notas ni fecha de cierre para la sesión antigua
    And conserva exactamente los intervalos y contadores persistidos sin sumar tiempo hasta exportedAt
    And ninguna sesión cambia estado, revisión ni fin acordado

  @s12
  Scenario Outline: Los enteros largos no pierden precisión ni se convierten en números JSON
    Given campos BIGINT válidos exteriores y anidados con valor <valor>
    When prepara su exportación
    Then esos campos contienen exactamente el string decimal <texto>
    And counts y duraciones acotadas siguen siendo números JSON
    Examples:
      | valor               | texto                 |
      | 0                   | "0"                   |
      | 9007199254740993    | "9007199254740993"    |
      | 9223372036854775807 | "9223372036854775807" |

  @s13
  Scenario: Textos y tiempos se conservan sin reinterpretar su contenido
    Given textos persistidos con espacios extremos, Unicode, saltos de línea y caracteres de HTML o fórmulas
    And un instante 2026-09-08T01:02:03.123456Z y fechas civiles y horarios locales históricos válidos
    And offsets y zonas históricos cuya resolución no debe recalcularse con la TZDB actual
    When prepara su exportación
    Then el JSON decodificado conserva exactamente los textos sin normalización ni ejecución
    And representa instantes en UTC Z con seis decimales, fechas YYYY-MM-DD y locales YYYY-MM-DDTHH:mm:ss con segundos cero
    And conserva zonas y offsets históricos incluidos Z y los offsets de intención presentes coincidentes con su resolución

  @s14
  Scenario Outline: El reloj identifica una preparación sin convertirse en una marca de commit
    Given que el único instante del reloj de preparación es <reloj>
    When prepara su exportación
    Then obtiene <resultado>
    Examples:
      | reloj                             | resultado |
      | 0001-01-01T00:00:00Z              | 200 con exportedAt 0001-01-01T00:00:00.000000Z |
      | 9999-12-31T23:59:59.999999Z       | 200 con exportedAt 9999-12-31T23:59:59.999999Z |
      | 2026-09-08T01:02:03.123456789Z    | 200 con exportedAt 2026-09-08T01:02:03.123456Z y el mismo instante truncado en filename |
      | un fallo al consultar el reloj    | 503 STORAGE_UNAVAILABLE sin archivo |
      | un instante anterior al año 0001  | 503 STORAGE_UNAVAILABLE sin archivo |
      | un instante posterior al año 9999 | 503 STORAGE_UNAVAILABLE sin archivo |

  @s15
  Scenario Outline: Los límites de tamaño son inclusivos y no producen páginas silenciosas
    Given un conjunto válido cuya representación completa tiene <registros> registros exteriores y <bytes> bytes UTF-8
    When prepara su exportación
    Then recibe <resultado>
    And nunca recibe un archivo recortado ni un éxito parcial
    Examples:
      | registros | bytes    | resultado |
      | 100000    | 33554432 | 200 con todas las filas y bytes exactos |
      | 100001    | 33554432 | 413 EXPORT_TOO_LARGE sin archivo |
      | 100000    | 33554433 | 413 EXPORT_TOO_LARGE sin archivo |

  @s16
  Scenario: Un exceso exportable demostrado no obliga a materializar toda la cuenta
    Given registros propios válidos cuyo contenido exportable conocido supera 33554432 bytes UTF-8
    And el exceso puede acreditarse sin acumular todos sus textos en memoria
    When intenta preparar la exportación
    Then recibe 413 EXPORT_TOO_LARGE antes de acumular el contenido completo de la cuenta
    And la lectura y salida respetan un presupuesto de memoria acotado y liberan sus recursos al terminar
    And no envía estado 200 ni bytes de descarga
    And el exceso se acredita sobre contenido exportable, no sólo por el tamaño bruto de JSONB desconocido o corrupto

  @s17
  Scenario Outline: Un fallo tardío de preparación no deja un archivo parcial exitoso
    Given una preparación que ya ha leído parte de los datos propios
    And ocurre <fallo> antes de terminar la representación
    When termina la solicitud
    Then recibe 503 STORAGE_UNAVAILABLE sin Content-Disposition ni bytes de archivo
    And se liberan los recursos de lectura y la representación parcial
    Examples:
      | fallo |
      | un fallo de lectura de una colección posterior |
      | un fallo de serialización |

  @s18
  Scenario Outline: Seguridad y consulta se resuelven antes de preparar datos
    Given <acceso>
    When solicita <peticion>
    Then recibe <resultado> sin preparar un snapshot de negocio
    Examples:
      | acceso                    | peticion                               | resultado |
      | ninguna sesión válida     | GET /api/v1/me/export?ownerId=otro      | 401 UNAUTHENTICATED |
      | una sesión válida         | GET /api/v1/me/export?ownerId=otro      | 400 INVALID_EXPORT_QUERY |
      | una sesión válida         | GET /api/v1/me/export?format=json       | 400 INVALID_EXPORT_QUERY |
      | una sesión válida         | GET /api/v1/me/export?cursor=           | 400 INVALID_EXPORT_QUERY |
      | una sesión válida         | GET /api/v1/me/export con cuerpo no vacío | 400 INVALID_EXPORT_REQUEST |
      | una sesión válida         | HEAD /api/v1/me/export                 | 405 con Allow GET y sin cuerpo |

  @s19
  Scenario Outline: La negociación sólo permite la representación JSON sin transformación
    Given una sesión válida sin token CSRF y una solicitud sin consulta ni cuerpo
    When consulta GET /api/v1/me/export con Accept <accept> y Accept-Encoding <encoding>
    Then recibe <resultado>
    And cualquier rechazo ocurre antes de preparar datos
    Examples:
      | accept                             | encoding   | resultado |
      | ausente                            | ausente    | 200 con archivo JSON íntegro |
      | application/*                      | identity   | 200 con archivo JSON íntegro |
      | application/json;q=0.5, */*;q=0     | gzip       | 200 con archivo JSON sin compresión |
      | application/json;q=0, */*;q=1       | ausente    | 406 EXPORT_FORMAT_NOT_ACCEPTABLE |
      | text/csv                           | ausente    | 406 EXPORT_FORMAT_NOT_ACCEPTABLE |
      | application/json                   | identity;q=0 | 406 EXPORT_FORMAT_NOT_ACCEPTABLE |
      | una sintaxis de cabecera inválida   | ausente    | 400 INVALID_EXPORT_REQUEST |

  @s20
  Scenario: La respuesta permite verificar nombre, longitud y privacidad del archivo
    Given una preparación válida con exportedAt 2026-09-08T01:02:03.123456Z
    When recibe la respuesta de exportación
    Then Content-Type es application/json; charset=utf-8 y Content-Length expresa exactamente sus bytes UTF-8 no comprimidos
    And Content-Disposition es attachment con un único filename entre comillas igual a organizationweb-export-v1-20260908T010203123456Z.json
    And no incluye filename*, nombre del usuario, ETag ni codificación distinta de identity
    And Cache-Control es no-store, private, no-transform y X-Content-Type-Options es nosniff

  @s21
  Scenario: Los errores conservan privacidad sin convertirse en descargas
    Given una solicitud autenticada con query inválida y Accept text/csv
    When consulta la exportación
    Then recibe primero 400 INVALID_EXPORT_QUERY como application/problem+json con mensaje español
    And conserva no-store, private, no-transform y nosniff sin Content-Disposition
    And no contiene datos privados ni trazas y no prepara datos

  @s22
  Scenario: Entrar a Exportación no inicia una descarga ni añade efectos sobre otros formularios
    Given una persona autenticada y el ciclo de navegación existente de proyectos, tareas y campos personales
    When abre Exportación desde el final de navegación Principal
    Then la ruta es /exportacion y muestra h1 Exportar mis datos sin desplazar Hoy
    And explica JSON versionado, carácter personal del archivo e importación aún no disponible
    And ofrece Preparar exportación sin consultar GET /api/v1/me/export
    And no añade escrituras de negocio ni resets de estado compartido a los efectos propios de ese ciclo de navegación
    And no exige persistir globalmente los borradores locales de rutas desmontadas

  @s23
  Scenario: Una preparación pendiente da respuesta inmediata y evita duplicados
    Given la vista inicial y una respuesta de exportación retenida
    When activa Preparar exportación dos veces mediante doble clic o Enter repetido
    Then sólo se envía una solicitud y se anuncia Preparando exportación… antes de 400 ms
    And no muestra porcentaje inventado ni enlace descargable y ofrece Cancelar preparación

  @s24
  Scenario: El archivo sólo se ofrece después de validar su recepción íntegra
    Given una preparación con transporte, envelope, owner, versión, colecciones y counts válidos
    When termina de recibir exactamente los bytes declarados
    Then anuncia Archivo preparado y ofrece el enlace nativo Descargar archivo JSON con download y nombre seguro
    And los bytes descargables son exactamente los recibidos sin reconstruir ni redondear el JSON
    And no descarga automáticamente ni afirma que el navegador lo guardó en disco

  @s25
  Scenario Outline: Una respuesta incompatible nunca crea un archivo descargable
    Given una preparación que devuelve HTTP 200 con <defecto>
    When el cliente procesa la respuesta
    Then muestra un fallo y no crea Blob descargable, enlace ni descarga
    And no conserva una representación parcial como archivo preparado
    Examples:
      | defecto |
      | Content-Type distinto de JSON |
      | Content-Length ausente, cero o superior a 33554432 |
      | más bytes recibidos que la longitud declarada |
      | menos bytes recibidos que la longitud declarada |
      | stream interrumpido |
      | Content-Encoding gzip |
      | filename con ruta o filename* adicional |
      | UTF-8 inválido o JSON incompleto |
      | format o schemaVersion no admitido |
      | owner distinto de la sesión solicitante |
      | una colección ausente o un count distinto de su longitud |
      | una colección que no es array o un count fraccionario |

  @s26
  Scenario: Reutilizar el enlace no prepara otro snapshot
    Given un archivo válido ya preparado en la vista
    When activa Descargar archivo JSON de nuevo
    Then el gesto utiliza el mismo archivo y nombre sin otro GET
    And la interfaz sigue anunciando disponibilidad sin afirmar guardado en disco

  @s27
  Scenario: Preparar de nuevo retira inmediatamente la descarga anterior
    Given un archivo preparado y datos propios que han cambiado desde su snapshot
    When activa Preparar de nuevo
    Then retira el enlace anterior y libera su archivo local antes de recibir el nuevo
    And envía una sola nueva solicitud con estado de preparación
    And sólo una respuesta nueva válida permite ofrecer otro archivo

  @s28
  Scenario Outline: Fallos de preparación no inducen descargas parciales ni reintentos automáticos
    Given una preparación pendiente
    When termina con <fallo>
    Then presenta <recuperacion> sin enlace descargable ni nueva solicitud automática
    Examples:
      | fallo                       | recuperacion |
      | error de red                | error y reintento manual |
      | 503 STORAGE_UNAVAILABLE      | error y reintento manual |
      | 413 EXPORT_TOO_LARGE         | límite explicado sin sugerir reintentos indefinidos ni archivo parcial |
      | 401 vigente                 | retirada de acceso conforme al flujo de sesión existente |

  @s29
  Scenario Outline: Salir de la preparación descarta incluso respuestas tardías
    Given una preparación o archivo propio en la vista y una respuesta tardía todavía posible
    When <salida>
    Then aborta la solicitud pendiente, revoca la URL local y retira bytes y referencias del archivo
    And cualquier respuesta HTTP, JSON o Blob tardía se descarta sin enlace ni descarga
    And un 401 tardío de esa solicitud no retira una sesión posterior
    And no promete borrar un archivo que el navegador ya hubiese descargado
    Examples:
      | salida |
      | activa Cancelar preparación mientras está pendiente |
      | navega a otra ruta |
      | cierra sesión |
      | cambia la identidad de acceso |

  @s30
  Scenario: La exportación no persiste los bytes fuera de su vista
    Given una preparación válida y almacenamiento web vacío para la exportación
    When recibe el archivo completo
    Then el archivo queda disponible sólo en memoria de esta sesión y vista
    And no se escriben sus bytes en localStorage, IndexedDB, caché de aplicación ni logs
    And no se crea enlace público ni un archivo persistente en el servidor

  @s31
  Scenario Outline: El foco respeta tanto la recuperación lógica como el movimiento voluntario
    Given una preparación iniciada por teclado y <foco>
    When desaparece el control iniciador al concluir o cancelar la preparación
    Then <resultado>
    And los estados y errores se anuncian sin obligar a usar el ratón
    Examples:
      | foco                                                | resultado |
      | la persona no ha movido el foco a otro destino       | queda en el control o encabezado lógico disponible con foco visible |
      | la persona ha movido voluntariamente el foco         | conserva ese destino y no roba el foco |

  @s32
  Scenario: La nueva vista conserva uso accesible en los estados de descarga
    Given los estados inicial, preparando, preparado y error de Exportación
    When se recorre la vista con teclado en los anchos y modalidades de docs/ux-requirements.md
    Then todos los controles conservan nombre accesible, foco visible y objetivo táctil de al menos 44px
    And a 320px, texto 200% y zoom 200% no pierde contenido ni acciones por recorte o desbordamiento horizontal
    And temas, forced-colors y movimiento reducido conservan legibilidad y operación
    And la revisión de los treinta principios registra evidencia y límites humanos y de dispositivos sin inferir cumplimiento universal desde axe

  @s33
  Scenario: Una lectura condicional no sustituye el archivo por una respuesta sin documento
    Given una sesión válida y una exportación anterior con sus datos ya descargados
    When consulta GET /api/v1/me/export con If-None-Match de una lectura anterior
    Then recibe 200 con un nuevo documento íntegro y sus catorce colecciones
    And no recibe 204, 206, 304 ni ETag y mantiene la política no-store
