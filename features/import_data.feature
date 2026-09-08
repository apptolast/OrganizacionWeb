Feature: Incorporar una copia propia sin sobrescribir datos ni repetir historia
  Como persona autenticada quiero validar y confirmar mi exportación JSON v1
  para recuperar datos ausentes conservando los cambios que ya existen.
  El archivo cerrado y sus catorce colecciones son los de las secciones 22 y 23.
  Toda confirmación de aceptación usa cuentas y PostgreSQL locales efímeros.

  @s1
  Scenario: Archivo vacío confirmado sin fabricar preferencias
    Given una cuenta sin datos y su archivo v1 con catorce arrays vacíos y counts cero
    When confirma el archivo con clave nueva y hash exacto
    Then recibe 200 NO_CHANGE con insertedCounts e identicalCounts cero
    And no crea preferencias ni datos de negocio y persiste sólo su recibo operativo

  @s2
  Scenario: Incorporación integral de las catorce familias a una cuenta usada
    Given un archivo propio válido con registros en las catorce colecciones
    And el destino contiene otros datos propios y datos de otra persona sin colisiones
    When confirma el archivo
    Then inserta todos los registros ausentes con IDs, relaciones y datos durables exactos
    And conserva todas las filas del destino no incluidas en el archivo
    And recibe 200 IMPORTED con conteos exactos y un recibo atómico
    And no crea, modifica ni republica outbox ni eventos históricos
    And exportar después conserva los hechos incorporados y excluye el recibo operativo

  @s3
  Scenario: Igualdad tipada permite otra representación JSON
    Given todas las filas del archivo ya presentes con iguales datos durables
    And el archivo cambia indentación, orden de propiedades, registros exteriores y values por fieldId
    When confirma con una clave nueva y el hash de esos bytes
    Then recibe 200 NO_CHANGE con todas las filas en identicalCounts
    And no actualiza versiones, timestamps ni filas de negocio

  @s4
  Scenario Outline: Una diferencia durable impide la fusión completa
    Given un archivo con filas nuevas válidas y una fila existente cuyo <dato> difiere
    When confirma el archivo
    Then recibe 409 IMPORT_CONFLICT sin insertar ninguna fila ni recibo
    And conserva íntegramente el destino sin sobrescribir ni elegir la mayor versión
    Examples:
      | dato |
      | texto con espacios o Unicode |
      | versión o timestamp |
      | recibo histórico |
      | null frente a valor ausente |
      | orden de visibleFields o definiciones |

  @s5
  Scenario Outline: Preferencias completas no se combinan por campos
    Given una preferencia <recurso> propia persistida
    And el archivo contiene ese recurso con identidad o contenido distinto y otros registros nuevos
    When valida la vista previa
    Then recibe 409 IMPORT_CONFLICT sin vista confirmable ni escrituras
    Examples:
      | recurso |
      | availability |
      | appearance |
      | customization PROJECT |
      | customization TASK |

  @s6
  Scenario Outline: Una clave alternativa ocupada no permite remapear
    Given un archivo internamente válido cuyo UUID nuevo colisiona por <clave> con otro registro del destino
    When confirma el archivo
    Then recibe 409 IMPORT_CONFLICT sin remapear IDs ni insertar parcialmente
    Examples:
      | clave |
      | owner y requestKey de sesión |
      | owner y requestKey de cambio de sesión |
      | taskId y requestKey de reserva o cambio |
      | blockId y version |
      | taskId y taskVersion de historial |
      | sessionId y revision de intervalo |
      | owner y recurso de valores personales |

  @s7
  Scenario Outline: La propiedad nunca se adopta desde el archivo
    Given una persona autenticada y <situacion>
    When confirma el archivo
    Then recibe <estado> con código <codigo> sin escrituras
    And el problema no revela datos, nombres ni identidades adicionales del destino
    Examples:
      | situacion | estado | codigo |
      | owner del archivo distinto del Principal | 400 | IMPORT_INVALID_FILE |
      | UUID del archivo ocupado por otra persona | 409 | IMPORT_CONFLICT |

  @s8
  Scenario Outline: JSON y esquema cerrados se rechazan antes de comparar destino
    Given un cuerpo con <defecto>
    When solicita vista previa autenticada
    Then recibe 400 IMPORT_INVALID_FILE sin plan ni escrituras
    Examples:
      | defecto |
      | UTF-8 inválido o BOM |
      | segundo documento después del JSON |
      | clave JSON duplicada, incluso en receipt |
      | campo desconocido exterior o anidado |
      | format o schemaVersion distinto |
      | colección requerida ausente o no array |
      | counts discordantes |
      | identidad o fieldId duplicado dentro del archivo |
      | objeto personal anidado más allá de 16 contenedores |

  @s9
  Scenario Outline: Los tipos no se corrigen mediante coerción
    Given un archivo que contiene <valor>
    When solicita vista previa
    Then recibe 400 IMPORT_INVALID_FILE sin normalizar ni redondear la entrada
    Examples:
      | valor |
      | BIGINT numérico en vez de string decimal canónico |
      | BIGINT textual con signo positivo, ceros iniciales o mayor que long |
      | NUMBER 1.0000000000000000000001 |
      | NUMBER fuera de -1000000000 a 1000000000 |
      | string false en lugar de BOOLEAN false |
      | fecha civil inválida o instante sin precisión contractual |
      | enum no admitido |

  @s10
  Scenario Outline: El archivo es autocontenido e íntegro
    Given un archivo con <defecto> aunque el destino pueda contener el padre ausente
    When solicita vista previa
    Then recibe 400 IMPORT_INVALID_FILE sin completar referencias desde el destino
    Examples:
      | defecto |
      | tarea sin proyecto incluido |
      | subtarea con padre ajeno al proyecto o ciclo |
      | valor personal sin definición del mismo ámbito |
      | recibo y fila con IDs o versiones discordantes |
      | proyección o intervalo incompatible con sus reglas durables |
      | dos sesiones abiertas en el propio archivo |

  @s11
  Scenario: Recibos completos conservan intención y resolución históricas
    Given un archivo válido con RESCHEDULED, CANCELLED, PAUSE, RESUME, CLOSE y EXTEND
    And sus snapshots contienen longs mayores que 9007199254740991 y offsets de intención nullable
    When confirma el archivo
    Then conserva todos los campos cerrados de los recibos 13 y 15 a 17 sin aplanarlos
    And conserva request y time separados, offsets resueltos, notas y microsegundos exactos
    And no reejecuta comandos ni añade campos desconocidos al JSONB durable

  @s12
  Scenario: Historia legada no se reconstruye con el reloj actual
    Given un archivo con proyectos y tareas terminados, intervalos cerrados y sesiones legadas con nulls admitidos
    And incluye reservas originales sin proyección y fechas cuya zona no debe recalcularse
    When confirma el archivo
    Then conserva estados, completedAt, nulls, ausencia de proyección y tiempos históricos
    And no inventa cierres, intervalos, trabajo hasta hoy ni normaliza textos

  @s13
  Scenario: Valores inactivos y extremos mantienen sus tipos
    Given un archivo con definiciones activas e inactivas, valores ausentes y null persistido
    And contiene TEXT Unicode con espacios, NUMBER 0 y extremos permitidos, BOOLEAN false y DATE válida
    When confirma el archivo
    Then conserva definiciones en su orden y cada valor con su tipo y presencia exactos
    And no materializa defaults ni valores de definiciones sin valor guardado

  @s14
  Scenario Outline: Vista previa advierte sólo de nuevas sesiones running
    Given un archivo válido cuya sesión running con runningSince <instante> está <presencia>
    When solicita vista previa
    Then runningSessions contiene <cantidad> elementos cerrados sessionId y runningSince
    And no inventa un instante ni pausa o cierra la sesión
    Examples:
      | instante | presencia | cantidad |
      | histórico no nulo | ausente en destino | 1 |
      | null legado permitido | ausente en destino | 1 |
      | histórico no nulo | idéntica en destino | 0 |

  @s15
  Scenario Outline: La unión respeta restricciones vigentes del destino
    Given un archivo válido cuya unión con el destino <conflicto>
    When confirma el archivo
    Then recibe 409 IMPORT_CONFLICT sin datos ni recibo parciales
    Examples:
      | conflicto |
      | supera la cuota configurada de proyectos activos |
      | añade otra sesión abierta a una running existente |
      | añade otra sesión abierta a una paused existente |
      | ocupa la clave única de cierre de sesión |

  @s16
  Scenario Outline: Límites inclusivos de bytes y registros
    Given un archivo JSON con <bytes> bytes UTF-8 y <registros> registros exteriores
    And es válido salvo el exceso indicado
    When solicita vista previa autenticada
    Then recibe <estado> y <resultado>
    And no persiste datos ni recibos
    Examples:
      | bytes | registros | estado | resultado |
      | 33554432 | 100000 | 200 | plan íntegro |
      | 33554433 | 100000 | 413 | IMPORT_TOO_LARGE |
      | menor que 33554432 | 100001 | 413 | IMPORT_TOO_LARGE |

  @s17
  Scenario: Una carga excesiva no se materializa completa
    Given un emisor cuyo cuerpo excede 33554432 bytes sin Content-Length
    And el resto del cuerpo aún no se ha recibido y puede ser inválido
    When la API autenticada recibe el byte que excede el límite
    Then rechaza con 413 IMPORT_TOO_LARGE sin esperar ni materializar el resto
    And descarta preparación privada y libera recursos sin datos parciales
    And el consumo de memoria permanece dentro del presupuesto acotado de recepción y preparación

  @s18
  Scenario: Vista previa consistente y no vinculante
    Given un archivo con filas ausentes e idénticas y un writer concurrente de esquema y valores
    When solicita vista previa
    Then recibe 200 con exactamente format, schemaVersion, fileSha256, byteLength, owner, exportedAt, counts, insertCounts, identicalCounts y runningSessions
    And format es organizationweb-import-preview y schemaVersion es 1
    And hash minúsculo de 64 hex y byteLength corresponden a los bytes originales
    And cada uno de los catorce counts es insertCounts más identicalCounts
    And compara un snapshot consistente sin mezclar mitades del writer ni escribir o reservar negocio

  @s19
  Scenario Outline: Confirmar vuelve a comparar tras cambiar el destino
    Given una vista previa válida y después <cambio> en el destino
    When confirma los mismos bytes y hash con una clave nueva
    Then recibe <estado> con <resultado>
    Examples:
      | cambio | estado | resultado |
      | otro importador insertó filas idénticas | 200 | counts reales con esas filas en identicalCounts |
      | otro writer confirmó datos incompatibles | 409 | IMPORT_CONFLICT sin inserciones ni recibo |

  @s20
  Scenario: Confirmación produce un recibo cerrado sólo tras commit
    Given un archivo válido con filas ausentes y clave nueva
    When confirma su importación
    Then recibe 200 con exactamente requestKey, fileSha256, byteLength, recordedAt, outcome, insertedCounts e identicalCounts
    And outcome es IMPORTED y existe al menos una inserción
    And recordedAt es UTC con seis decimales capturado al construir el recibo y no se presenta como instante de commit
    And el recibo y las filas están confirmados atómicamente antes de devolver éxito

  @s21
  Scenario Outline: Una clave confirmada tiene intención inmutable
    Given un recibo propio confirmado y cambios posteriores del workspace
    When confirma con la misma clave <archivo>
    Then recibe <estado> y <resultado>
    Examples:
      | archivo | estado | resultado |
      | los mismos bytes válidos y hash | 200 | el recibo original exacto sin volver a escribir |
      | otros bytes válidos con su hash correcto | 409 | IMPORT_KEY_REUSED sin escritura |
      | bytes cuyo hash declarado no coincide | 412 | IMPORT_FILE_CHANGED sin devolver recibo |
      | archivo JSON inválido | 400 | IMPORT_INVALID_FILE sin devolver recibo |

  @s22
  Scenario Outline: Consulta de recibo no revela otra identidad
    Given una clave canónica cuyo recibo está <situacion>
    When consulta GET /api/v1/me/imports/by-key/{requestKey}
    Then recibe <estado> con <resultado>
    And no ejecuta una importación ni devuelve el archivo
    Examples:
      | situacion | estado | resultado |
      | confirmado para el Principal | 200 | mismo recibo cerrado persistido |
      | ausente | 404 | IMPORT_NOT_FOUND genérico |
      | confirmado para otra persona | 404 | IMPORT_NOT_FOUND genérico |
      | aún sin commit en otra petición | 404 | IMPORT_NOT_FOUND sin afirmar rollback futuro |

  @s23
  Scenario Outline: Todo fallo tardío revierte datos y recibo
    Given una confirmación válida que ya preparó inserciones
    And ocurre <fallo>
    When intenta terminar la transacción
    Then recibe 503 STORAGE_UNAVAILABLE sin éxito parcial
    And todas las filas previas permanecen exactas y no hay inserciones ni recibo confirmado
    And libera recursos y locks adquiridos
    Examples:
      | fallo |
      | fallo de almacenamiento después de insertar varias colecciones |
      | fallo al persistir el recibo |
      | fallo real al confirmar la transacción |

  @s24
  Scenario Outline: Writers existentes no pierden cambios ante importación
    Given un writer real de <familia> y una importación con registros relacionados
    When se intercalan su validación y confirmación
    Then los resultados corresponden a un orden válido o a un aborto íntegro por conflicto o almacenamiento
    And no hay cambio confirmado perdido, combinación parcial ni actualización con lectura obsoleta
    Examples:
      | familia |
      | proyectos y cuota activa |
      | tareas y estados |
      | reservas y proyecciones |
      | sesiones e intervalos |
      | disponibilidad y apariencia |

  @s25
  Scenario Outline: Import espera al writer de personalización que ya leyó
    Given un writer real de <recurso> ya leyó ausencia o versión y retiene su lock compartido
    And su cambio incompatible termina dentro del tiempo de espera permitido
    When confirma concurrentemente el archivo
    Then importa sólo después de observar el cambio confirmado y responde 409 IMPORT_CONFLICT
    And no sobrescribe ese cambio ni deja filas o recibo de importación
    Examples:
      | recurso |
      | configuración PROJECT |
      | configuración TASK |
      | valores PROJECT |
      | valores TASK |

  @s26
  Scenario Outline: Writer iniciado durante import revalida después
    Given importación retiene los locks compartidos de PROJECT y TASK antes de los locks de tablas
    When un writer real de <recurso> intenta leer y confirmar una revisión anterior
    Then no puede validar schema y previous para un UPSERT hasta liberarse la importación
    And después revalida el estado vigente y conserva el resultado confirmado de import
    Examples:
      | recurso |
      | configuración |
      | valores |

  @s27
  Scenario Outline: Contención aborta sin extender el contrato temporal
    Given una importación encuentra <condicion>
    When intenta adquirir locks y aplicar
    Then obtiene 503 STORAGE_UNAVAILABLE y rollback íntegro sin reintento automático
    And libera locks parciales y conserva los writers ajenos confirmados
    And mantiene límites transaccionales de 2 segundos por adquisición y 10 por sentencia sin prometer plazo global
    Examples:
      | condicion |
      | advisory compartido retenido más de 2 segundos |
      | una tabla ocupada tras adquirir otras de la lista |
      | deadlock con un writer de orden distinto |
      | sentencia de aplicación supera 10 segundos |

  @s28
  Scenario Outline: Autenticación y seguridad preceden al negocio
    Given <condicion> y una petición de importación con archivo grande o inválido
    When solicita <ruta>
    Then conserva <resultado> del flujo de seguridad existente sin consumir el archivo en el caso de uso
    Examples:
      | condicion | ruta | resultado |
      | sesión ausente | POST preview | 401 UNAUTHENTICATED |
      | sesión ausente | POST confirmación | 401 UNAUTHENTICATED |
      | sesión ausente | GET recibo | 401 UNAUTHENTICATED |
      | sesión válida y CSRF inválido | POST confirmación | rechazo CSRF existente |
      | sesión válida y origen no permitido | POST preview | rechazo de origen existente |
      | fallo de persistencia de sesión | POST confirmación | 503 SESSION_UNAVAILABLE |

  @s29
  Scenario Outline: Frontera HTTP rechaza antes de aplicar
    Given una sesión autenticada y seguridad válida
    When envía <peticion>
    Then recibe <estado> con <resultado> sin escrituras
    Examples:
      | peticion | estado | resultado |
      | POST JSON UTF-8 con query | 400 | IMPORT_INVALID_REQUEST |
      | GET recibo con query o cuerpo | 400 | IMPORT_INVALID_REQUEST |
      | confirmación sin clave o con UUID no canónico | 400 | IMPORT_INVALID_REQUEST |
      | confirmación con hash de sintaxis inválida | 400 | IMPORT_INVALID_REQUEST |
      | GET recibo con UUID mal formado | 400 | IMPORT_INVALID_REQUEST |
      | POST con charset no UTF-8 o Content-Encoding gzip | 415 | rechazo HTTP existente |
      | HEAD sobre preview o confirmación | 405 | método no admitido sin preparar archivo |

  @s30
  Scenario: Errores y éxito no filtran contenido privado
    Given un archivo que contiene nombres, notas y texto parecido a HTML o credenciales
    When la API responde a la vista previa y su validación
    Then sus respuestas privadas no se almacenan en caché
    And los errores usan problem+json con código y mensaje español sin payload, trazas ni datos ajenos
    And los textos no se ejecutan como HTML ni se copian a logs

  @s31
  Scenario Outline: El proxy admite el límite y delega el exceso a la API
    Given Nginx real con recursos actuales y un archivo válido salvo el exceso de <bytes> bytes
    And la petición autenticada usa <transporte>
    When solicita <ruta> a través del proxy
    Then recibe <resultado> de la API sin 413 HTML anticipado
    And no aumenta tmpfs ni timeout de 15 segundos
    Examples:
      | bytes | transporte | ruta | resultado |
      | 33554432 | Content-Length | POST preview | 200 con plan completo |
      | 33554433 | Content-Length | POST confirmación | 413 IMPORT_TOO_LARGE sin escritura |
      | 33554432 | chunked sin Content-Length | POST confirmación | 200 con recibo completo |
      | 33554433 | chunked sin Content-Length | POST preview | 413 IMPORT_TOO_LARGE |

  @s32
  Scenario: Streaming del proxy queda limitado a dos rutas exactas
    Given Nginx real y una carga chunked a una de las dos rutas exactas de importación
    When el emisor envía sólo el comienzo y demora el resto
    Then el upstream recibe antes de finalizar la carga sin almacenar un archivo completo en el tmpfs de 16 MiB
    And una persona anónima obtiene 401 de la API antes de consumir el archivo de negocio
    And las rutas ajenas y prefijos parecidos conservan su límite de 1 MiB
    And Host, forwarded headers, DNS, 15 segundos y ausencia de reintento al siguiente upstream se conservan

  @s33
  Scenario: Abrir y seleccionar no envía datos
    Given una sesión válida en la navegación principal
    When abre Importación y selecciona un archivo
    Then ve Importar mis datos, control nativo y Validar archivo después de Exportación sin desplazar Hoy
    And no se envía petición hasta Validar archivo
    And la ayuda explica copia propia, privacidad y rechazo integral sin sobrescritura
    And un File.size superior a 33554432 impide el envío

  @s34
  Scenario: Vista previa requiere un segundo gesto informado
    Given un archivo cuya vista previa válida anuncia filas nuevas y una sesión running histórica
    When termina la validación solicitada
    Then muestra owner, exportedAt, tamaño y cantidades a insertar e idénticas por colección
    And advierte que running conserva su instante histórico y puede incluir tiempo transcurrido
    And ofrece Confirmar importación sin enviarla automáticamente ni aplicar preferencias
    And no presenta reservas como trabajo realizado ni agrega logros

  @s35
  Scenario Outline: Retirar una preparación no confirma
    Given una vista previa confirmable con archivo seleccionado
    When <accion>
    Then retira el plan y su confirmación sin escrituras ni envío de importación
    Examples:
      | accion |
      | cambia el archivo |
      | cancela antes de confirmar |
      | sale de la pantalla antes de confirmar |

  @s36
  Scenario: Confirmar guarda identidad mínima y bloquea duplicados
    Given vista previa vigente y sessionStorage disponible
    When activa Confirmar importación dos veces por clic o Enter
    Then envía una sola petición con los mismos bytes, hash y clave canónica
    And antes del envío guarda sólo owner, requestKey y fileSha256 en organizationweb.import.pending.v1
    And no permite editar la intención mientras está pendiente
    And anuncia progreso sin porcentaje ficticio

  @s37
  Scenario Outline: Respuesta incierta conserva la intención
    Given una confirmación enviada con clave y hash guardados
    When <situacion>
    Then no anuncia importación confirmada ni rollback y ofrece Comprobar resultado
    And conserva la clave y hash sin reenviar ni generar clave nueva automáticamente
    Examples:
      | situacion |
      | se pierde la respuesta después del commit |
      | la conexión o el proxy agota su espera |
      | llega un 200 con recibo incompleto o incoherente |
      | consulta deliberada por clave devuelve 404 mientras la petición puede seguir activa |

  @s38
  Scenario: Recargar permite recuperar sin conservar el archivo
    Given una intención pendiente propia válida en sessionStorage tras respuesta perdida
    When recarga y elige Comprobar resultado
    Then consulta sólo su clave y un recibo confirmado muestra el resultado exacto
    And elimina la entrada pendiente al confirmarse sin repetir importación
    And si no hay recibo conserva incertidumbre y exige seleccionar mismos bytes y hash para un reenvío deliberado
    And no persiste archivo, nombre, vista previa, bytes ni payload en storage, URL o analítica

  @s39
  Scenario Outline: Recuperación privada falla cerrada
    Given <situacion>
    When intenta recuperar o confirmar su intención
    Then <resultado>
    Examples:
      | situacion | resultado |
      | sessionStorage no disponible al confirmar | no envía y explica el requisito de recuperación |
      | entrada pendiente de forma inválida | elimina la entrada sin consultar recibo |
      | entrada pendiente de otra identidad | elimina la entrada sin consultar recibo |
      | logout o cambio de identidad | elimina pendiente y contenido visible |
      | reupload distinto del hash pendiente | no reenvía ni cambia silenciosamente la clave |

  @s40
  Scenario: Respuestas tardías no alteran otro contexto
    Given una petición de importación o recuperación iniciada por una identidad anterior
    And la pantalla salió o la sesión cambió mientras esperaba
    When llega su 401 o respuesta JSON tardía
    Then no altera la sesión nueva, sus datos, errores ni foco
    And descarta contenido visible y aborta lecturas del contexto retirado

  @s41
  Scenario: Confirmación refresca lo afectado sin borrar borradores ajenos
    Given una importación confirmada y un borrador ajeno al flujo de importación
    When presenta el recibo de éxito
    Then elimina su intención pendiente y refresca deliberadamente snapshots afectados
    And los datos incorporados aparecen al consultarlos sin reiniciar el borrador ajeno
    And no aplica cambios sólo por una vista previa

  @s42
  Scenario: Recorrido accesible conserva foco y reflow
    Given la pantalla en estados inicial, validando, preparado, error, incierto y confirmado
    When recorre el flujo con teclado y las modalidades de docs/ux-requirements.md
    Then acciones tienen área mínima de 44 por 44 px, nombres y foco visibles
    And si desaparece el iniciador recupera foco lógico salvo movimiento voluntario del usuario
    And el blur real de controles disabled no deja el foco perdido
    And a 320 px, texto y zoom 200 por ciento no se recorta contenido ni aparece desborde horizontal
    And temas, forced-colors y movimiento reducido conservan acciones, contraste y estados no dependientes sólo del color
    And la revisión registra evidencia y límites de los 30 principios UX y motores sin atribuir estudio humano a axe
