Feature: Personalizar la apariencia propia con colores legibles y guardado confirmado
  Como persona usuaria quiero elegir tema y acentos accesibles
  para mantener una presentación cómoda sin alterar mis datos de trabajo.
  El contrato usa los defaults, superficies y contraste exactos de la sección 20.
  Las garantías comunes de sesión, JSON, CSRF y problemas se reutilizan explícitamente.

  @s1
  Scenario: Leer defaults seguros no crea una preferencia
    Given una persona autenticada sin preferencia de apariencia
    When consulta GET /api/v1/me/appearance
    Then recibe 200 con exactamente configured false, theme SYSTEM, accentLight #244C3C, accentDark #B7E4C7 y updatedAt null
    And recibe ETag fuerte "appearance:unconfigured" y Cache-Control no-store
    And no se inserta ninguna fila ni evento

  @s2
  Scenario Outline: Guardar los tres valores propios confirma una versión durable
    Given una persona autenticada sin preferencia y su ETag unconfigured
    When envía PUT /api/v1/me/appearance con theme <theme>, accentLight #0000ff y accentDark #00ffff usando If-Match vigente
    Then recibe 200 con configured true y los colores canónicos #0000FF y #00FFFF
    And el tema confirmado es <theme> y updatedAt es UTC con máximo seis decimales
    And el ETag contiene el UUID propio generado y versión 0
    And queda exactamente una fila propia con los tres valores confirmados
    Examples:
      | theme  |
      | LIGHT  |
      | DARK   |
      | SYSTEM |

  @s3
  Scenario: Cambiar sólo mayúsculas de un color es no-op con revisión vigente
    Given apariencia configurada SYSTEM, #244C3C, #B7E4C7 y versión 4
    When guarda SYSTEM, #244c3c y #b7e4c7 con su ETag vigente
    Then conserva cuerpo, ETag, updatedAt y versión 4 exactamente

  @s4
  Scenario: Una intención ya satisfecha no elude revisión obsoleta
    Given apariencia propia versión 4 con DARK y ambos acentos por defecto
    When guarda esos mismos valores con un ETag propio válido de versión 3
    Then recibe 412 APPEARANCE_CONFLICT
    And la fila y su versión no cambian

  @s5
  Scenario Outline: Dos escrituras compiten por la misma revisión propia
    Given <estado> y dos peticiones válidas distintas con el mismo If-Match
    When ambas escrituras concurren antes de confirmar ninguna
    Then exactamente una recibe 200 y otra 412 APPEARANCE_CONFLICT
    And queda una sola fila con todos los campos del ganador sin mezcla
    And la versión final es <version>
    Examples:
      | estado                   | version |
      | ausencia confirmada      | 0       |
      | preferencia en versión 3 | 4       |

  @s6
  Scenario: Un tag ajeno no selecciona la preferencia de otro propietario
    Given Ana tiene LIGHT y Bruno tiene DARK con otro UUID y ambos autenticados por separado
    When Ana intenta guardar usando el ETag configurado de Bruno
    Then recibe 412 APPEARANCE_CONFLICT sin datos de Bruno
    And ninguna de las dos preferencias cambia

  @s7
  Scenario: Un fallo antes del commit no confirma cambios parciales
    Given una preferencia propia vigente y PostgreSQL rechaza el commit de su actualización
    When guarda DARK con colores válidos y If-Match vigente
    Then recibe 503 STORAGE_UNAVAILABLE
    And valores, versión y updatedAt anteriores permanecen sin cambios

  @s8
  Scenario: Una confirmación perdida se recupera mediante una lectura después de reiniciar
    Given un PUT de DARK confirmó en PostgreSQL pero su respuesta no llegó al navegador
    And el proceso API se reinició conservando la base de datos
    When la persona vuelve a consultar su apariencia autenticada desde otro navegador
    Then recibe los valores DARK confirmados y su revisión persistida
    And no necesita localStorage ni genera otra escritura

  @s9
  Scenario: Preferencias no alteran hechos ni inventan eventos
    Given existen proyectos, tareas, reservas, sesiones, historial y outbox propios
    When guarda un cambio válido de apariencia
    Then sólo cambia su preferencia de apariencia
    And los datos de trabajo, disponibilidad, historial y outbox conservan sus valores

  @s10
  Scenario Outline: Validar campos mantiene orden y errores corregibles
    Given una petición PUT autenticada con CSRF e If-Match vigentes y los demás campos válidos
    When envía <entrada>
    Then recibe 400 VALIDATION_ERROR asociado a <campo> con código <codigo>
    And no escribe preferencias
    Examples:
      | entrada                              | campo       | codigo                |
      | raíz array                           | body        | INVALID_TYPE          |
      | extra ownerId                        | ownerId     | UNKNOWN_FIELD         |
      | extras zeta y alpha                  | alpha       | UNKNOWN_FIELD         |
      | theme ausente                        | theme       | REQUIRED              |
      | theme null                           | theme       | REQUIRED              |
      | theme numérico                       | theme       | INVALID_TYPE          |
      | theme light                          | theme       | INVALID_VALUE         |
      | accentLight ausente                  | accentLight | REQUIRED              |
      | accentLight null                     | accentLight | REQUIRED              |
      | accentLight array                    | accentLight | INVALID_TYPE          |
      | accentLight #123                     | accentLight | INVALID_VALUE         |
      | accentLight #11223344                | accentLight | INVALID_VALUE         |
      | accentLight con espacios exteriores | accentLight | INVALID_VALUE         |
      | accentLight var(--x)                  | accentLight | INVALID_VALUE         |
      | accentDark ausente                   | accentDark  | REQUIRED              |
      | accentDark null                      | accentDark  | REQUIRED              |
      | accentDark booleano                  | accentDark  | INVALID_TYPE          |
      | accentDark rgb(1,2,3)                | accentDark  | INVALID_VALUE         |

  @s11
  Scenario Outline: Un acento debe servir también para su tema inactivo
    Given un PUT válido salvo <campo> con theme <tema>
    When envía el color <color> para ese campo
    Then recibe 400 VALIDATION_ERROR de <campo> con INSUFFICIENT_CONTRAST
    And no redondea contraste ni sustituye silenciosamente el color
    Examples:
      | campo       | tema  | color   |
      | accentLight | DARK  | #FFFFFF |
      | accentLight | LIGHT | #777777 |
      | accentDark  | LIGHT | #000000 |

  @s12
  Scenario Outline: Precondición de escritura es obligatoria y cerrada
    Given una petición PUT autenticada con cuerpo válido
    When presenta If-Match <header>
    Then recibe <status> y <codigo> sin escribir
    Examples:
      | header                                      | status | codigo                |
      | ausente                                     | 428    | PRECONDITION_REQUIRED |
      | débil                                       | 400    | VALIDATION_ERROR      |
      | dos valores                                 | 400    | VALIDATION_ERROR      |
      | lista separada por coma                     | 400    | VALIDATION_ERROR      |
      | appearance con UUID en mayúsculas           | 400    | VALIDATION_ERROR      |
      | appearance con versión 9223372036854775808  | 400    | VALIDATION_ERROR      |
      | appearance con versión 01                  | 400    | VALIDATION_ERROR      |
      | availability:unconfigured                   | 400    | VALIDATION_ERROR      |

  @s13
  Scenario Outline: Query no permitida precede a precondición y cuerpo
    Given una persona autenticada y <metodo> sin If-Match y con cuerpo malformado si corresponde
    When solicita /api/v1/me/appearance con <query>
    Then recibe 400 VALIDATION_ERROR de query
    And no escribe ni devuelve defaults por error
    Examples:
      | metodo | query           |
      | GET    | ownerId=otro    |
      | GET    | theme=x&theme=y |
      | PUT    | extra=1         |

  @s14
  Scenario Outline: JSON cerrado reutiliza el contrato común
    Given un PUT autenticado con If-Match válido y Content-Type application/json
    When envía <cuerpo>
    Then recibe 400 MALFORMED_JSON sin escritura
    Examples:
      | cuerpo                          |
      | cuerpo ausente                  |
      | texto vacío                     |
      | objeto truncado                 |
      | dos documentos concatenados     |
      | clave theme duplicada           |
      | clave accentLight duplicada     |

  @s15
  Scenario Outline: Seguridad y negociación reutilizadas protegen el recurso
    Given <contexto>
    When solicita <peticion>
    Then recibe <status> sin lectura privada ni escritura confirmada
    Examples:
      | contexto                              | peticion                           | status |
      | sesión ausente                        | GET /api/v1/me/appearance           | 401    |
      | sesión ausente                        | PUT /api/v1/me/appearance           | 401    |
      | sesión válida sin CSRF                | PUT /api/v1/me/appearance           | 403    |
      | sesión válida con CSRF y origen ajeno | PUT /api/v1/me/appearance           | 403    |
      | sesión válida con CSRF                | PUT appearance con tipo text/plain | 415    |

  @s16
  Scenario: Un fallo de lectura no es ausencia de configuración
    Given una preferencia DARK persistida y almacenamiento temporalmente inaccesible
    When consulta su apariencia
    Then recibe 503 STORAGE_UNAVAILABLE sin cuerpo de defaults
    And la preferencia sigue persistida

  @s17
  Scenario Outline: Versión máxima no desborda y no-op sigue disponible
    Given preferencia propia en versión 9223372036854775807
    When guarda <intencion> con If-Match vigente
    Then recibe <status>
    And la versión y datos anteriores permanecen intactos
    Examples:
      | intencion             | status |
      | los mismos valores    | 200    |
      | un tema diferente     | 503    |

  @s18
  Scenario Outline: El decoder rechaza recursos incompatibles antes de aplicarlos
    Given el navegador conserva una apariencia válida y recibe HTTP 200
    When decodifica <respuesta>
    Then rechaza la respuesta sin aplicar colores ni sustituir su snapshot por defaults
    Examples:
      | respuesta                                                   |
      | objeto con campo extra                                      |
      | configured como cadena                                      |
      | false con un color distinto del default                      |
      | false con updatedAt no null                                  |
      | false con ETag configurado                                   |
      | true con updatedAt null                                      |
      | true con timestamp de siete decimales                        |
      | true con ETag débil                                          |
      | true con ETag de disponibilidad                              |
      | true con versión fuera de BIGINT                             |
      | theme desconocido                                           |
      | color de formato correcto sin contraste                      |
      | color no canónico en minúsculas                              |

  @s19
  Scenario: Confirmación de PUT debe corresponder a la intención enviada
    Given se envió DARK con #0000FF y #00FFFF
    When recibe un 200 estructuralmente válido que confirma LIGHT con esos mismos colores
    Then no anuncia guardado ni aplica LIGHT
    And exige Recargar versión guardada antes de otra escritura

  @s20
  Scenario: La vista está accesible en navegación y retorno tras acceso
    Given la persona entra a /apariencia sin sesión
    When completa el acceso válido
    Then vuelve a /apariencia y enfoca el encabezado Apariencia
    And Principal contiene Apariencia con aria-current page
    And muestra los valores obtenidos del recurso privado

  @s21
  Scenario: Editar sólo cambia muestra y borrador
    Given apariencia guardada LIGHT y su formulario cargado
    When selecciona DARK y escribe #0000FF y #00FFFF en los campos de acento
    Then la muestra local representa ambos temas con esos colores
    And el resto de la aplicación mantiene LIGHT guardado
    And no envía PUT y anuncia que hay cambios sin guardar

  @s22
  Scenario: Restaurar defaults prepara una intención revisable
    Given apariencia DARK personalizada y formulario cargado
    When pulsa Restaurar valores predeterminados
    Then el borrador contiene SYSTEM, #244C3C y #B7E4C7
    And la muestra se actualiza sin PUT ni cambio global confirmado
    And Guardar apariencia permite persistir esos valores con la revisión vigente

  @s23
  Scenario: Guardar confirma junto el formulario y la apariencia global
    Given borrador válido distinto del guardado y PUT retenido por el servidor
    When pulsa Guardar apariencia dos veces antes de recibir respuesta
    Then envía un solo PUT con los tres valores y la revisión leída
    And anuncia guardado pendiente antes de 400 ms sin éxito anticipado
    And al recibir 200 válido aplica el tema confirmado en todas las rutas sin recargar la página
    And mantiene el foco en Guardar si ese control sigue presente y enfocado

  @s24
  Scenario Outline: Tema sistema sigue el medio sin guardar otra preferencia
    Given preferencia <preferencia> y sistema inicialmente claro
    When el sistema cambia a oscuro <momento>
    Then la apariencia efectiva es <efectiva>
    And no envía PUT ni cambia la preferencia persistida
    Examples:
      | preferencia | momento                         | efectiva |
      | SYSTEM      | con la página visible           | oscuro   |
      | SYSTEM      | durante suspensión y al regresar | oscuro   |
      | LIGHT       | con la página visible           | claro    |
      | DARK        | con la página visible           | oscuro   |

  @s25
  Scenario: Navegar conserva preferencia sin conservar borrador abandonado
    Given apariencia DARK confirmada y borrador LIGHT sin guardar en Apariencia
    When navega a Historial y vuelve mediante Back
    Then ambas pantallas mantienen DARK confirmado
    And el formulario vuelve a los valores confirmados sin recuperar LIGHT abandonado
    And no consulta apariencia de nuevo por cada ruta ni escribe la preferencia

  @s26
  Scenario: Fallo inicial permite seguir trabajando y reintentar honestamente
    Given no hay snapshot local y GET appearance devuelve 503
    When se muestra la aplicación
    Then usa provisionalmente SYSTEM y acentos seguros con aviso de carga fallida
    And permite navegar a las demás funciones y Reintentar la lectura
    And no presenta defaults como guardados ni permite PUT sin versión válida

  @s27
  Scenario: Color inválido conserva texto y error accesible sin contaminar la muestra
    Given formulario válido con accentLight #244C3C
    When escribe #FFFFFF como acento claro
    Then conserva #FFFFFF en el campo y marca aria-invalid true
    And la descripción accesible enlaza un error de contraste corregible
    And la muestra conserva el último color seguro explicado y Guardar no envía PUT

  @s28
  Scenario Outline: Incertidumbre obliga a recuperar antes de nueva escritura
    Given un PUT transmitido con borrador válido
    When termina con <resultado>
    Then conserva borrador y apariencia previamente confirmada sin anunciar éxito
    And no permite otro PUT hasta Recargar versión guardada válido
    And no reenvía automáticamente la intención
    Examples:
      | resultado                         |
      | HTTP 412 APPEARANCE_CONFLICT       |
      | HTTP 503 STORAGE_UNAVAILABLE       |
      | error de red                      |
      | HTTP 200 con cuerpo inválido       |
      | problema de code/type contradictorio |

  @s29
  Scenario Outline: Recuperación manual reemplaza el borrador sólo con lectura válida
    Given PUT incierto y la advertencia de que Recargar versión guardada reemplazará el borrador
    When ejecuta esa consulta y recibe <respuesta>
    Then ocurre <resultado>
    And no envía PUT ni afirma haber recuperado un recibo de la intención perdida
    Examples:
      | respuesta                     | resultado                                                   |
      | 200 válido con valores nuevos | reemplaza juntos borrador, apariencia y revisión             |
      | 503                           | conserva borrador, anuncia error y mantiene bloqueo de PUT    |
      | 200 inválido                   | conserva borrador, anuncia error y mantiene bloqueo de PUT    |

  @s30
  Scenario: Una lectura anterior a guardar no restaura un tema obsoleto
    Given GET antiguo LIGHT pendiente y PUT DARK confirmado con revisión nueva
    When llega la respuesta válida del GET antiguo
    Then conserva DARK y la revisión nueva sin borrar el formulario confirmado

  @s31
  Scenario Outline: Respuesta privada antigua no afecta una sesión posterior
    Given <peticion> pendiente de Ana y después logout seguido de login de Bruno
    When llega <respuesta> de Ana antes del observador de acceso
    Then no restaura valores de Ana ni revoca acceso de Bruno
    Examples:
      | peticion       | respuesta       |
      | GET appearance | HTTP 401        |
      | PUT appearance | HTTP 401        |
      | GET appearance | HTTP 200 válido |
      | PUT appearance | HTTP 200 válido |

  @s32
  Scenario: Salir de la sesión retira la personalización privada
    Given una persona usa DARK y acentos propios con borrador abierto
    When cierra su sesión de acceso
    Then retira preferencia y borrador y aplica SYSTEM con defaults seguros
    And no conserva datos privados como autoridad en localStorage

  @s33
  Scenario Outline: La recuperación respeta el destino deliberado del foco
    Given una consulta de recuperación pendiente iniciada desde un control que desaparecerá
    And <foco>
    When llega una lectura válida actual
    Then <destino>
    Examples:
      | foco                                      | destino                                      |
      | el foco seguía en el iniciador             | el encabezado recibe foco                    |
      | el usuario movió foco a otro control       | ese control conserva foco                    |
      | el usuario movió foco y después dejó body | el encabezado no roba foco                   |

  @s34
  Scenario Outline: Variantes conservan legibilidad y acceso al producto existente
    Given tema <tema> con los colores válidos libres #0000FF y #00FFFF
    When recorre navegación, formulario, error, sesión, historial y revisión semanal a <entorno>
    Then textos y acentos cumplen los contrastes definidos sobre los fondos reales
    And no hay solapes, recortes ni overflow horizontal y los controles alcanzan 44 por 44 píxeles CSS
    And teclado, foco visible separado de etiquetas y estados anunciados se conservan
    And los estados siguen identificados sin depender sólo del color
    Examples:
      | tema   | entorno                                                 |
      | LIGHT  | anchos 320 a 2560 y ambos lados de breakpoints            |
      | DARK   | anchos 320 a 2560 y altura reducida                       |
      | SYSTEM | texto ampliado 200 por ciento                            |
      | DARK   | zoom nativo 200 por ciento con viewport 320 CSS           |
      | DARK   | movimiento reducido y colores forzados del navegador     |

  @s35
  Scenario Outline: Fallos temporales o persistencia corrupta no fabrican preferencias
    Given <contexto>
    When ejecuta <operacion>
    Then recibe <status> sin modificar la preferencia
    And no sustituye datos corruptos por defaults ni emite año ampliado
    Examples:
      | contexto                                      | operacion                      | status |
      | ausencia y reloj en año 10000                  | PUT válido desde unconfigured  | 503    |
      | preferencia válida y reloj en año 0000         | PUT de cambio real             | 503    |
      | preferencia válida y reloj que falla           | PUT de cambio real             | 503    |
      | preferencia válida y reloj que falla           | PUT no-op vigente              | 200    |
      | fila con un acento persistido inválido         | GET appearance                 | 503    |

  @s36
  Scenario: Sistema sin consulta de medio usa claro sin cambiar la preferencia
    Given preferencia SYSTEM confirmada y consulta de esquema del sistema no disponible
    When muestra una ruta del producto
    Then usa tema claro con accentLight guardado
    And no guarda LIGHT ni escribe otra preferencia

  @s37
  Scenario: Cancelar devuelve el borrador al snapshot sin petición
    Given apariencia DARK confirmada y borrador LIGHT con otro color válido
    When pulsa Cancelar cambios
    Then formulario y muestra vuelven a DARK con colores confirmados
    And no envía GET ni PUT y conserva apariencia global confirmada
