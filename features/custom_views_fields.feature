Feature: Personalizar vistas y campos propios sin alterar los hechos de trabajo
  Como persona usuaria quiero elegir metadatos visibles y guardar información personal
  conservando los datos, las acciones esenciales y una recuperación explícita.
  Se reutilizan sesión, CSRF/origen, negociación, JSON estricto, problem+json y no-store de 20.
  Las rutas, DTO cerrados, ETag y precedencias son los de la sección 21 de project-spec.md.
  La evidencia UX aplica docs/ux-requirements.md sin atribuir resultados antes de implementarlos.

  @s1
  Scenario Outline: Consultar un ámbito ausente devuelve defaults sin escribir
    Given una cuenta autenticada sin configuración del ámbito <scope>
    When consulta GET /api/v1/me/customization/<scope> sin token CSRF
    Then recibe 200 con exactamente configured false, visibleFields <fields>, customFields [] y updatedAt null
    And el ETag fuerte es "customization:<scope>:unconfigured" y la respuesta es no-store
    And no crea configuración, valores ni eventos
    Examples:
      | scope   | fields                                 |
      | PROJECT | [createdAt]                            |
      | TASK    | [completionCriterion,estimatedMinutes] |

  @s2
  Scenario Outline: Reemplazar la presentación conserva orden y configuración restante
    Given una configuración <scope> con definiciones activas e inactivas y valores guardados
    When guarda visibleFields <fields> con If-Match vigente
    Then recibe 200 con la configuración completa y visibleFields exactamente <fields>
    And conserva definiciones, valores y orden de entidades sin alterar los DTO de negocio
    Examples:
      | scope   | fields                                              |
      | PROJECT | []                                                  |
      | PROJECT | [updatedAt,createdAt]                                |
      | TASK    | [updatedAt,createdAt,estimatedMinutes,completionCriterion] |

  @s3
  Scenario Outline: Crear una definición confirma identidad nueva y tipo inmutable
    Given una cuenta sin configuración PROJECT ni valores personales de sus proyectos
    When crea un campo con label "  Dato  " y type <type> usando el tag unconfigured
    Then recibe 200 con configured true y una definición exacta id UUID nuevo, label "Dato", type <type> y active true
    And recibe configuración versión 0 y updatedAt UTC canónico con resolución máxima de microsegundos
    And conserva visibleFields [createdAt], sin rellenar entidades ni emitir Location, recibo o evento
    Examples:
      | type    |
      | TEXT    |
      | NUMBER  |
      | DATE    |
      | BOOLEAN |

  @s4
  Scenario Outline: Las etiquetas respetan puntos de código y unicidad sin normalización
    Given una configuración vigente con una definición inactiva llamada "Dato" y espacio disponible
    When solicita crear una etiqueta <label> de tipo TEXT con revisión vigente
    Then obtiene <result> y <effect>
    Examples:
      | label                                     | result | effect                                      |
      | Unicode White_Space exterior seguido de X  | 200    | guarda X sin ese espacio exterior            |
      | A seguido de dos espacios y B              | 200    | conserva los dos espacios interiores         |
      | 60 puntos de código de pares válidos       | 200    | conserva los 60 puntos                       |
      | 61 puntos de código                       | 400    | label INVALID_VALUE sin escritura           |
      | sólo Unicode White_Space                   | 400    | label INVALID_VALUE sin escritura           |
      | Dato                                      | 400    | label INVALID_VALUE sin escritura           |
      | dato                                      | 200    | crea una definición distinta de Dato         |
      | U+0000                                    | 400    | label INVALID_VALUE sin escritura           |
      | un surrogate UTF-16 aislado                | 400    | label INVALID_VALUE sin escritura           |

  @s5
  Scenario: Las etiquetas canónicamente equivalentes siguen siendo distintas
    Given una definición llamada U+00E9 en PROJECT
    When crea otra con label U+0065 seguido de U+0301 y revisión vigente
    Then confirma ambas etiquetas distintas sin normalizarlas ni cambiar su orden de creación

  @s6
  Scenario Outline: El límite de doce cuenta también las definiciones ocultas
    Given <count> definiciones propias PROJECT contando activas e inactivas y ninguna en TASK
    When crea una definición PROJECT de etiqueta no usada con revisión vigente
    Then recibe <status> y conserva TASK sin configurar
    And el total PROJECT queda <total>
    Examples:
      | count | status                                             | total |
      | 11    | 200                                                | 12    |
      | 12    | 400 VALIDATION_ERROR customFields INVALID_VALUE     | 12    |

  @s7
  Scenario Outline: Cambiar una definición conserva identidad, tipo y valores ocultos
    Given un campo TEXT con valor "anotación" y estado <before>
    When guarda label "Nombre actual" y active <active> con revisión vigente
    Then recibe 200 conservando UUID y tipo TEXT y cambiando sólo etiqueta y estado de esa definición
    And el valor almacenado sigue siendo exactamente "anotación" y su revisión no cambia
    And la lectura ordinaria del valor <visibility>
    Examples:
      | before   | active | visibility                                        |
      | activo   | false  | omite ese campo                                   |
      | inactivo | true   | devuelve anotación con la etiqueta Nombre actual  |
      | activo   | true   | devuelve anotación con la etiqueta Nombre actual  |

  @s8
  Scenario Outline: La revisión precede al no-op de configuración
    Given una configuración versión 4 y un reloj que fallaría al consultarlo
    When guarda la misma presentación con <tag>
    Then recibe <status> y conserva exactamente cuerpo, ETag, fecha y versión 4
    And no consulta el reloj ni escribe
    Examples:
      | tag                  | status                       |
      | ETag vigente         | 200                          |
      | ETag de versión 3    | 412 CUSTOMIZATION_CONFLICT   |

  @s9
  Scenario Outline: La revisión compartida serializa cambios pero no consume no-op
    Given <initial> y dos clientes con el mismo ETag
    When se ejecutan concurrentemente <commands>
    Then el resultado es <result> sin cambios parciales ni definiciones duplicadas
    Examples:
      | initial                      | commands                                      | result                              |
      | configuración ausente        | dos altas de definición                       | un 200 versión 0 y un 412           |
      | configuración versión 4      | cambiar presentación y crear una definición   | un 200 versión 5 y un 412           |
      | configuración versión 4      | dos guardados sin cambio                      | dos 200 y versión 4                 |

  @s10
  Scenario Outline: Leer valores propios proyecta sólo definiciones activas sin crear filas
    Given una entidad propia <entity> sin fila de valores y definiciones activa TEXT e inactiva NUMBER
    When consulta su ruta de campos personales
    Then recibe 200 con exactamente configured false, updatedAt null y values con sólo el campo TEXT a null
    And cada entrada contiene exactamente fieldId, label, type y value en orden de definición
    And el ETag único tiene scope <scope>, id de esa entidad, schema vigente y values unconfigured
    And body y tag corresponden a una sola lectura consistente sin reloj ni escritura
    Examples:
      | entity    | scope   |
      | proyecto  | PROJECT |
      | tarea     | TASK    |
      | subtarea  | TASK    |

  @s11
  Scenario Outline: Los valores válidos se guardan sin coerción ni pérdida de ausencia
    Given una entidad propia con un campo activo <type> y ETag compuesto vigente
    When guarda el conjunto completo con value <input>
    Then recibe 200 y el valor confirmado es exactamente <output>
    And la primera escritura crea una fila configured true con versión 0 incluso si el valor es null
    Examples:
      | type    | input                              | output                             |
      | TEXT    | null                               | null                               |
      | TEXT    | cadena vacía                       | null                               |
      | TEXT    | espacios y salto de línea          | los mismos espacios y salto        |
      | TEXT    | 1000 puntos de código con pares    | los mismos 1000 puntos              |
      | TEXT    | texto con etiquetas HTML          | el mismo texto sin ejecutarlo      |
      | NUMBER  | -1000000000                        | -1000000000                        |
      | NUMBER  | 1000000000                         | 1000000000                         |
      | NUMBER  | 0                                  | 0                                  |
      | NUMBER  | 1.0                                | 1                                  |
      | NUMBER  | 1e3                                | 1000                               |
      | NUMBER  | null                               | null                               |
      | BOOLEAN | false                              | false                              |
      | BOOLEAN | true                               | true                               |
      | BOOLEAN | null                               | null                               |
      | DATE    | "0001-01-01"                       | "0001-01-01"                       |
      | DATE    | "9999-12-31"                       | "9999-12-31"                       |
      | DATE    | "2000-02-29"                       | "2000-02-29"                       |
      | DATE    | null                               | null                               |

  @s12
  Scenario Outline: Rechazar un valor incompatible conserva el conjunto anterior
    Given un campo activo <type>, valores confirmados y revisión compuesta vigente
    When guarda el conjunto completo con el valor <value>
    Then recibe 400 VALIDATION_ERROR con código <code> asociado a ese valor
    And no cambia ningún valor, revisión o fecha
    Examples:
      | type    | value                         | code          |
      | TEXT    | 1001 puntos de código         | INVALID_VALUE |
      | TEXT    | U+0000                        | INVALID_VALUE |
      | TEXT    | surrogate UTF-16 aislado       | INVALID_VALUE |
      | TEXT    | número JSON 1                 | INVALID_TYPE  |
      | NUMBER  | -1000000001                   | INVALID_VALUE |
      | NUMBER  | 1000000001                    | INVALID_VALUE |
      | NUMBER  | 1.5                           | INVALID_VALUE |
      | NUMBER  | string "1"                    | INVALID_TYPE  |
      | BOOLEAN | string "false"                | INVALID_TYPE  |
      | BOOLEAN | número JSON 0                 | INVALID_TYPE  |
      | DATE    | "1900-02-29"                  | INVALID_VALUE |
      | DATE    | "0000-01-01"                  | INVALID_VALUE |
      | DATE    | "10000-01-01"                 | INVALID_VALUE |
      | DATE    | "2026-1-01"                   | INVALID_VALUE |
      | DATE    | "2026-01-01T00:00:00Z"        | INVALID_VALUE |

  @s13
  Scenario Outline: El conjunto de valores es completo y no modifica campos inactivos
    Given dos campos activos A y B y un campo inactivo C con valor preservado
    When guarda <entries> con revisión vigente
    Then obtiene <result> conservando siempre el valor de C
    Examples:
      | entries                       | result                                              |
      | B y A una vez cada uno        | 200 con salida en orden de definición A y B          |
      | sólo A                        | 400 INVALID_VALUE sin modificar A ni B               |
      | A dos veces y B               | 400 INVALID_VALUE sin modificar A ni B               |
      | A, B y C                      | 400 INVALID_VALUE sin modificar A ni B               |
      | A, B y un UUID desconocido    | 400 INVALID_VALUE sin modificar A ni B               |

  @s14
  Scenario: El no-op de valores compara números y texto canónicos sin depender del orden
    Given valores confirmados A NUMBER 1 y B TEXT null con versión 7 y un reloj que fallaría
    When guarda B con cadena vacía y A con 1.0 usando el ETag compuesto vigente
    Then recibe 200 con cuerpo, tag, fecha y versión 7 originales sin consultar el reloj

  @s15
  Scenario Outline: El ETag compuesto impide escribir contra otra definición vigente
    Given un cliente con valores y ETag leído antes de <change> por otra pestaña
    And ese cambio de configuración ya está confirmado sin cambiar la revisión de los valores
    When el cliente guarda sus valores usando el ETag compuesto anterior
    Then recibe 412 CUSTOMIZATION_CONFLICT antes de validar el conjunto activo o su tipo guardado
    And no escribe valores y la nueva representación tiene otro componente schema en el ETag
    Examples:
      | change               |
      | renombrar un campo   |
      | crear un campo       |
      | desactivar un campo  |
      | reactivar un campo   |

  @s16
  Scenario Outline: Dos guardados competitivos conservan una revisión de valores coherente
    Given dos clientes con el mismo ETag compuesto vigente y valores versión 2
    When ejecutan concurrentemente <commands>
    Then reciben <result> y cada respuesta confirmada corresponde a su cuerpo y ETag duraderos
    Examples:
      | commands                      | result                              |
      | dos cambios reales distintos  | un 200 versión 3 y un 412            |
      | dos no-op canónicos            | dos 200 y versión 2                  |

  @s17
  Scenario Outline: La propiedad se comprueba antes de comparar revisiones privadas
    Given una petición autenticada con sintaxis válida y ETag bien formado no vigente
    When intenta <operation>
    Then recibe 404 problem+json sin datos ajenos en el cuerpo, etiquetas, valores o revisiones y sin escritura
    Examples:
      | operation                                                      |
      | leer valores de un proyecto ajeno                               |
      | guardar valores de una tarea de otro proyecto propio            |
      | guardar valores de una entidad inexistente                      |
      | actualizar una definición de otra cuenta                        |
      | actualizar una definición del otro ámbito                       |
      | actualizar una definición inexistente                           |

  @s18
  Scenario: Los metadatos de una tarea terminada no alteran negocio ni se heredan
    Given una tarea terminada con subtarea y proyecto propios con valores personales distintos
    When guarda campos personales de la tarea terminada con revisión vigente
    Then confirma únicamente los valores de esa tarea
    And conserva estado, revisión y updatedAt de negocio, valores del proyecto y valores de la subtarea
    And no modifica estimaciones, progreso, intervalos, historial, recibos ni outbox

  @s19
  Scenario: Editar un proyecto terminado no lo reabre
    Given un proyecto terminado con su estado, revisión y updatedAt de negocio registrados
    When guarda sus campos personales con revisión vigente
    Then recibe 200 y conserva exactamente esos tres atributos de negocio

  @s20
  Scenario Outline: Los fallos persistentes no producen defaults ni confirmaciones parciales
    Given una operación propia válida y <failure>
    When ejecuta <operation>
    Then recibe 503 STORAGE_UNAVAILABLE sin confirmar éxito ni defaults
    And conserva configuración, definiciones, valores y revisiones anteriores sin eventos
    Examples:
      | failure                                | operation                       |
      | almacenamiento no disponible           | leer configuración              |
      | fila seleccionada corrupta             | leer valores                    |
      | fallo de commit                        | crear definición                |
      | fallo tras preparar parte del conjunto | guardar varios valores          |
      | reloj falla                            | cambiar presentación            |
      | reloj anterior a año 0001              | cambiar valores                 |
      | reloj posterior a año 9999             | cambiar presentación            |
      | versión máxima del recurso cambiado   | cambiar valores                 |

  @s21
  Scenario Outline: Sólo consume revisión el recurso que cambia
    Given <initial> con revisión vigente y almacenamiento disponible
    When ejecuta <operation>
    Then recibe 200 y <result>
    Examples:
      | initial                                 | operation                        | result                                          |
      | configuración con versión máxima        | guardar presentación idéntica    | conserva revisión y fecha sin consultar reloj   |
      | schema con versión máxima y valores 2   | cambiar un valor                 | valores pasa a 3 y schema permanece idéntico     |
      | valores con versión máxima y schema 2   | renombrar una definición         | schema pasa a 3 y valores permanece idéntico     |

  @s22
  Scenario Outline: Las fechas de cambios no retroceden y respetan resolución y extremos
    Given una configuración con updatedAt <previous> y reloj <clock>
    When guarda un cambio real con revisión vigente
    Then confirma updatedAt <confirmed> y una sola revisión nueva
    Examples:
      | previous                    | clock                         | confirmed                   |
      | 2026-09-07T10:00:00Z         | 2026-09-07T09:00:00Z           | 2026-09-07T10:00:00Z         |
      | 2026-09-07T10:00:00Z         | 2026-09-07T10:00:01.123456789Z | 2026-09-07T10:00:01.123456Z  |
      | 0001-01-01T00:00:00Z         | 0001-01-01T00:00:00Z           | 0001-01-01T00:00:00Z         |
      | 9999-12-31T23:59:59.999998Z  | 9999-12-31T23:59:59.999999Z     | 9999-12-31T23:59:59.999999Z  |

  @s23
  Scenario Outline: Seguridad y parámetros preceden al manejador privado
    Given <context>
    When envía <request>
    Then recibe <result> sin leer ni cambiar datos privados mediante el manejador
    Examples:
      | context                         | request                                      | result                              |
      | sesión ausente                  | GET con query inválida                       | 401                                 |
      | sesión válida sin CSRF          | PUT con JSON inválido                        | 403                                 |
      | sesión válida y origen prohibido| PUT válido con CSRF                          | 403                                 |
      | sesión válida                   | GET configuración con cualquier query        | 400 query INVALID_VALUE             |
      | sesión válida                   | GET configuración scope project              | 400 scope INVALID_VALUE             |
      | sesión válida                   | GET valores con UUID de ruta inválido         | 400 INVALID_FORMAT                  |

  @s24
  Scenario Outline: Las escrituras sólo admiten la cabecera de revisión fuerte correspondiente
    Given una petición propia autorizada con cuerpo válido
    When guarda usando <header>
    Then recibe <result> antes de escribir
    Examples:
      | header                                         | result                         |
      | If-Match ausente                               | 428 PRECONDITION_REQUIRED      |
      | tag compuesto débil                            | 400 de If-Match                |
      | versión no canónica en el componente schema     | 400 de If-Match                |
      | tag de otra familia o ámbito                   | 400 de If-Match                |
      | tag válido anterior al vigente                 | 412 CUSTOMIZATION_CONFLICT     |

  @s25
  Scenario Outline: El JSON cerrado tiene errores deterministas antes de consultar estado
    Given una escritura autorizada con ETag bien formado antiguo
    When envía <body>
    Then recibe 400 con <error> antes de devolver conflicto
    And no cambia datos
    And la selección de error respeta raíz, extras léxicos, campos declarados y arrays por índice
    Examples:
      | body                                          | error                               |
      | clave JSON duplicada                          | MALFORMED_JSON                      |
      | extras z y a                                  | a UNKNOWN_FIELD antes de z          |
      | visibleFields ausente o null                  | visibleFields REQUIRED              |
      | visibleFields como string                     | visibleFields INVALID_TYPE          |
      | visibleFields duplicados                      | visibleFields INVALID_VALUE         |
      | visibleFields ajenos al ámbito                | visibleFields INVALID_VALUE         |
      | alta con type desconocido                     | type INVALID_VALUE                  |
      | actualización que intenta cambiar type        | type UNKNOWN_FIELD                  |
      | actualización con active null                 | active REQUIRED                     |
      | valores con entrada que incluye type          | type UNKNOWN_FIELD                  |
      | values null                                   | values REQUIRED                     |

  @s26
  Scenario: Consultar tras reinicio conserva configuración, valores y revisiones confirmados
    Given una vista personalizada, una definición desactivada y valores confirmados antes de reiniciar la API
    And una nueva sesión autenticada de la misma cuenta
    When consulta configuración y valores propios
    Then recupera las representaciones y ETag confirmados sin reconstruirlos desde outbox
    And conserva en almacenamiento los valores inactivos aunque la respuesta ordinaria los omita

  @s27
  Scenario Outline: El cliente rechaza una representación incompatible completa
    Given una consulta de configuración o valores con identidad de contexto vigente
    When recibe 200 con <defect>
    Then rechaza toda la respuesta sin publicar datos parciales ni defaults confirmados
    Examples:
      | defect                                                        |
      | campo extra en un objeto cerrado                              |
      | configured false con fecha o revisión configuradas             |
      | configured true con updatedAt inválido                         |
      | defaults distintos para un ámbito sin configurar               |
      | IDs repetidos o definición sin tipo permitido                  |
      | etiqueta vacía o texto fuera de límites                        |
      | valor incompatible con su tipo recibido                        |
      | ETag compuesto con scope o entityId distinto del solicitado     |
      | versión del ETag no canónica                                   |

  @s28
  Scenario Outline: Una confirmación debe corresponder a la intención normalizada
    Given una escritura pendiente propia
    When recibe 200 con <defect>
    Then mantiene resultado incierto y bloquea otra escritura de ese recurso hasta recarga manual válida
    Examples:
      | defect                                                    |
      | presentación diferente de la solicitada                    |
      | alta sin UUID nuevo o con otra etiqueta o tipo              |
      | alta que pierde una definición anterior                    |
      | actualización que cambia otra definición                   |
      | valores distintos de los normalizados enviados             |
      | cuerpo o ETag perteneciente a otro recurso                  |

  @s29
  Scenario Outline: Personalizar metadatos conserva el contenido esencial y el ámbito
    Given la lista <surface> y una configuración confirmada <fields>
    When abre esa lista
    Then muestra los metadatos opcionales en el orden visual y DOM <fields>
    And conserva nombre o título enlazado, estado, acciones, contexto y paginación
    And permite Personalizar vista sin añadir consultas de valores por cada fila
    And una estimación null se muestra como Sin estimación y las fechas nativas siguen en UTC
    Examples:
      | surface                            | fields                                 |
      | Proyectos                          | []                                     |
      | Tareas de un primer proyecto        | [estimatedMinutes,completionCriterion] |
      | Tareas de otro proyecto             | [estimatedMinutes,completionCriterion] |
      | Subtareas                           | [estimatedMinutes,completionCriterion] |

  @s30
  Scenario Outline: Restaurar o cancelar la vista no borra información personal
    Given una presentación modificada y definiciones con valores guardados
    When activa <action>
    Then obtiene <result> sin modificar definiciones ni valores
    Examples:
      | action                                        | result                                            |
      | Restaurar vista                               | prepara defaults en borrador sin enviar escritura  |
      | Cancelar después de preparar Restaurar vista   | vuelve a la presentación confirmada sin escritura  |
      | Guardar vista con defaults ya preparados       | confirma sólo los metadatos visibles por defecto   |

  @s31
  Scenario: Gestionar campos explica conservación y mantiene separados los formularios
    Given una lista propia con borradores abiertos de creación de tarea y sesión
    When abre la gestión de campos desde Personalizar vista
    Then encuentra etiqueta y cuatro tipos al crear, incluido Número entero
    And la edición ofrece renombrar, desactivar y reactivar sin cambiar tipo ni eliminar
    And desactivar explica que oculta y conserva valores
    And conserva íntegros los otros borradores y separa gestión de campos de Guardar vista

  @s32
  Scenario Outline: Los controles personales distinguen ausencia de cero y falso
    Given un detalle propio con campos TEXT, NUMBER, DATE y BOOLEAN confirmados
    When realiza <action>
    Then obtiene <result>
    And Campos personales permanece separado de hechos, progreso y acciones de negocio
    Examples:
      | action                                | result                                                     |
      | editar texto, entero y fecha          | controles nativos sin escribir todavía                     |
      | elegir No en el booleano              | borrador false distinto de Sin valor                       |
      | Vaciar un campo                       | borrador null sólo para ese campo                          |
      | Cancelar un borrador modificado       | snapshot confirmado sin escritura                          |
      | Guardar campos con cero y false       | una escritura del conjunto y confirmación de cero y false   |

  @s33
  Scenario Outline: Un fallo de carga no convierte datos desconocidos en vacíos
    Given una superficie propia <surface> sin respuesta válida de personalización
    When falla su consulta
    Then presenta <result> y mantiene disponibles las acciones de negocio
    Examples:
      | surface | result                                                            |
      | lista   | presentación base marcada como no confirmada y Reintentar          |
      | detalle | error y Reintentar sin mostrar campos vacíos como datos válidos     |

  @s34
  Scenario: El error de campo permite corregir el borrador sin perderlo
    Given un borrador de campos personales propio con varios valores
    When el guardado devuelve 400 con un error de campo válido
    Then conserva el borrador completo y asocia el mensaje al control correspondiente
    And permite corregirlo y conserva los demás formularios de negocio

  @s35
  Scenario Outline: La incertidumbre persiste al volver sin reenviar ni liberar automáticamente
    Given una escritura <command> que terminó con <failure> y permanece incierta
    When navega a otra pantalla y vuelve al recurso dentro de la misma sesión
    Then conserva el bloqueo de otra escritura y ofrece Recargar guardado
    And no reenvía POST o PUT ni consulta automáticamente para liberar el bloqueo
    Examples:
      | command            | failure                       |
      | crear definición   | ACK perdido tras commit       |
      | guardar vista      | 412 CUSTOMIZATION_CONFLICT    |
      | guardar valores    | 503 STORAGE_UNAVAILABLE       |
      | guardar valores    | confirmación incompatible     |

  @s36
  Scenario Outline: Recargar guardado sustituye el borrador sin atribuir un recibo
    Given un alta de definición incierta con una etiqueta que ya aparece en el servidor
    When activa Recargar guardado y recibe <response>
    Then <result>
    And no reenvía el alta ni declara que la igualdad de etiqueta acredita la misma intención
    Examples:
      | response          | result                                                         |
      | GET válido        | sustituye snapshot y ETag y habilita una decisión manual nueva  |
      | GET fallido       | conserva bloqueo y explicación de recuperación                  |
      | GET incompatible  | conserva bloqueo sin publicar una configuración parcial         |

  @s37
  Scenario Outline: La retirada de identidad invalida datos privados y respuestas tardías
    Given datos, errores y borradores privados visibles con una consulta anterior todavía pendiente
    And se ha producido <event>
    When termina la consulta anterior del recurso retirado
    Then no restaura datos, errores ni borradores privados ni ejecuta un observador 401 obsoleto
    And no conserva valores en localStorage o URLs ni etiquetas en telemetría
    Examples:
      | event                     |
      | cierre de sesión          |
      | cambio de cuenta          |
      | 401 del contexto vigente  |
      | 404 del contexto vigente  |
      | cambio de proyecto o tarea|

  @s38
  Scenario: Una lectura anterior no reemplaza una escritura posterior confirmada
    Given una lectura retenida y un guardado posterior ya confirmado del mismo recurso
    When llega la lectura antigua
    Then conserva el cuerpo y ETag de la confirmación sin reemplazarlos por la versión anterior
    And los consumidores de otro ámbito, entidad o sesión permanecen separados

  @s39
  Scenario Outline: La interacción por teclado anuncia espera y restaura foco sólo cuando corresponde
    Given un formulario contextual con iniciador <focus> y respuesta de guardado retenida
    When activa Guardar mediante teclado
    Then muestra espera y anuncio accesible antes de 400 ms sin fingir confirmación
    And los controles de mostrar, Subir y Bajar funcionan sin arrastre obligatorio
    And al desaparecer el iniciador <result>
    Examples:
      | focus                         | result                                             |
      | conserva el foco del usuario | restaura foco al contexto accesible correspondiente |
      | el usuario movió el foco     | no roba el foco elegido por el usuario              |

  @s40
  Scenario: Las superficies personalizadas conservan legibilidad en las modalidades requeridas
    Given listas y detalles con doce campos, etiquetas Unicode largas, vacío, carga, error y éxito
    When recorre personalización con teclado en los anchos y modalidades de docs/ux-requirements.md
    Then conserva controles principales de al menos 44 por 44 píxeles CSS, foco visible y orden de lectura
    And no hay solapes, recortes ni desplazamiento horizontal de página a 320 píxeles CSS o texto y zoom real al 200 por ciento
    And ambos temas, forced-colors y movimiento reducido conservan información y acciones sin depender sólo del color
    And la revisión aporta las 30 filas UX con evidencia y límites por motor, dispositivo y modalidad realmente probados

  @s41
  Scenario: Guardar los defaults ausentes configura la vista por primera vez
    Given una cuenta sin configuración PROJECT y ETag "customization:PROJECT:unconfigured"
    When envía PUT con visibleFields [createdAt] y ese If-Match
    Then recibe 200 con configured true, visibleFields [createdAt], customFields [] y updatedAt válido
    And crea la configuración con UUID propio y versión 0 aunque la presentación coincida con los defaults

  @s42
  Scenario Outline: El conjunto vacío es válido cuando no hay definiciones activas
    Given una entidad propia con <initial>, ninguna definición activa y definiciones inactivas preservadas
    When ejecuta <operation>
    Then recibe 200 con values [] y <result>
    And conserva todas las definiciones inactivas y sus posibles valores sin crearlos ni eliminarlos
    Examples:
      | initial                            | operation                                   | result                                                  |
      | ausencia de fila de valores         | GET de sus campos personales                | configured false y updatedAt null sin insertar          |
      | ausencia de fila de valores         | PUT con values [] y ETag compuesto vigente   | configured true, updatedAt válido y versión de valores 0 |
      | valores inactivos en una fila       | PUT con values [] y ETag compuesto vigente   | configured true y no-op con fecha y revisión conservadas |
