@today @approved
Feature: Consultar la agenda propia de Hoy sin acreditar trabajo realizado
  Destilación de project-spec.md sección12 revisada por root en86dc6c, bajo autorización global vigente.
  Contrato revisado por el coordinador y aprobado bajo autorización global vigente antes de TDD.
  GET /api/v1/today devuelve un snapshot del propietario autenticado, sin parámetros ni paginación.
  Se heredan de features/schedule_block.feature el DTO cerrado block9, formatos de UUID/instante,
  privacidad, seguridad HTTP, errores problem+json y prohibición de solapes de reservas existentes.
  No se repiten sus325 casos de creación: Hoy no crea bloques ni invoca planificación.
  El snapshot usa un único serverNow y lectura coherente read-only REPEATABLE_READ según sección12;
  disponibilidad, ausencia, nombres y reservas no mezclan revisiones. No escribe ni bloquea para escribir.
  La consulta filtra intersecciones del día, no carga todo el historial ni hace HTTP por cada tarea.
  Objeto cerrado15: serverNow, date, zoneId, zoneSource, availabilityZoneId, dayStartAt, dayEndAt,
  budgetMinutes, plannedSeconds, remainingSeconds, excessSeconds, currentBlockId, nextBlockId, closingAt, items.
  budgetMinutes es entero de0 a1440 con zona válida; presupuesto, restante y exceso son null en fallback.
  plannedSeconds es entero no negativo; restante y exceso conocidos son max(presupuesto*60-planificado,0)
  y max(planificado-presupuesto*60,0). current/next/cierre son null cuando no existe candidato.
  Cada item contiene exactamente block, projectName y taskTitle; block conserva sus nueve campos de feature11.
  Cada fila de Examples es independiente; lo no mencionado permanece válido. Los relojes son controlados.

  Background:
    Given una sesión válida de persona-a y reservas propias válidas sin solapes
    And serverNow es "2030-01-07T12:00:00Z"
    And disponibilidad propia UTC con120 minutos para el lunes
    And no existen reservas salvo las indicadas

  @s1
  Scenario: Leer un día vacío con esquema cerrado y sin efectos de escritura
    When consulto GET /api/v1/today sin headers de idempotencia o revisión
    Then recibo200 JSON con exactamente los quince campos obligatorios del contrato y Cache-Control no-store
    And date es "2030-01-07", zoneId y availabilityZoneId son "UTC" y zoneSource es "AVAILABILITY"
    And dayStartAt es "2030-01-07T00:00:00Z" y dayEndAt es "2030-01-08T00:00:00Z"
    And items es vacío, plannedSeconds es0 y currentBlockId, nextBlockId y closingAt son null
    And budgetMinutes es120, remainingSeconds es7200 y excessSeconds es0
    And no cambia disponibilidad, bloque, tarea, proyecto ni outbox y no se publica evento

  @s2
  Scenario Outline: Calcular capacidad conocida sin convertir planificación en trabajo
    Given el presupuesto del lunes es <minutos> y las intersecciones propias suman <planificado> segundos
    When consulto Hoy
    Then budgetMinutes es <minutos>, plannedSeconds es <planificado>, remainingSeconds es <restante> y excessSeconds es <exceso>
    And no se completa ninguna tarea ni se crea tiempo trabajado
    Examples:
      | minutos | planificado | restante | exceso |
      | 120     | 3600        | 3600     | 0      |
      | 120     | 7200        | 0        | 0      |
      | 120     | 9000        | 0        | 1800   |
      | 0       | 3600        | 0        | 3600   |

  @s3
  Scenario Outline: Conservar reservas con UTC y presupuesto desconocido si falta zona utilizable
    Given <preferencia>
    And existe una reserva propia de "2030-01-07T10:00:00Z" a "2030-01-07T11:00:00Z"
    When consulto Hoy
    Then zoneId es "UTC", zoneSource es <fuente> y availabilityZoneId es <zonaGuardada>
    And budgetMinutes, remainingSeconds y excessSeconds son null, nunca cero
    And date es "2030-01-07", plannedSeconds es3600 y se conserva el intervalo y la zona originales del bloque
    And no se crea ni corrige la preferencia persistida
    Examples:
      | preferencia                                      | fuente       | zonaGuardada     |
      | no existe disponibilidad                         | UNCONFIGURED | null             |
      | la zona guardada Legacy/Retired ya no es resoluble | UNAVAILABLE  | Legacy/Retired   |

  @s4
  Scenario Outline: Incluir sólo intersecciones positivas del día semiabierto
    Given existe un único bloque propio con inicio <inicio> y fin <fin>
    When consulto Hoy
    Then items contiene <cantidad> bloque y plannedSeconds es <segundos>
    And si está incluido sus instantes siguen siendo <inicio> y <fin>, sin recorte del DTO
    Examples:
      | inicio               | fin                  | cantidad | segundos |
      | 2030-01-06T23:00:00Z  | 2030-01-07T00:00:00Z  | 0        | 0        |
      | 2030-01-06T23:30:00Z  | 2030-01-07T00:30:00Z  | 1        | 1800     |
      | 2030-01-07T10:00:00Z  | 2030-01-07T11:00:00Z  | 1        | 3600     |
      | 2030-01-07T23:30:00Z  | 2030-01-08T00:30:00Z  | 1        | 1800     |
      | 2030-01-08T00:00:00Z  | 2030-01-08T01:00:00Z  | 0        | 0        |

  @s5
  Scenario Outline: Delimitar días DST por sus medianoches reales
    Given disponibilidad en Europe/Madrid y serverNow es <ahora>
    When consulto Hoy
    Then date es <fecha>, dayStartAt es <inicio> y dayEndAt es <fin>
    And la duración del día es <horas> horas y se usa el presupuesto del día de semana de <fecha>
    Examples:
      | ahora                | fecha      | inicio               | fin                  | horas |
      | 2026-03-29T12:00:00Z  | 2026-03-29 | 2026-03-28T23:00:00Z  | 2026-03-29T22:00:00Z  | 23    |
      | 2026-10-25T12:00:00Z  | 2026-10-25 | 2026-10-24T22:00:00Z  | 2026-10-25T23:00:00Z  | 25    |
      | 2026-10-26T12:00:00Z  | 2026-10-26 | 2026-10-25T23:00:00Z  | 2026-10-26T23:00:00Z  | 24    |

  @s6
  Scenario Outline: Mostrar una reserva de medianoche en ambos días y conservar el cierre real
    Given un bloque B de "2030-01-07T23:30:00Z" a "2030-01-08T00:30:00Z"
    And serverNow es <ahora>
    When consulto Hoy
    Then date es <fecha>, items contiene B completo y plannedSeconds es1800
    And closingAt es "2030-01-08T00:30:00Z", aunque esté fuera del día <fecha>
    Examples:
      | ahora                | fecha      |
      | 2030-01-07T23:45:00Z  | 2030-01-07 |
      | 2030-01-08T00:15:00Z  | 2030-01-08 |

  @s7
  Scenario Outline: Elegir bloque actual y próximo inicio con extremos semiabiertos
    Given bloques A de09:00 a10:00, B de10:00 a11:00 y C de12:00 a13:00 del7 de enero de2030 UTC
    And serverNow es <hora> de ese día
    When consulto Hoy
    Then currentBlockId es <actual>, nextBlockId es <siguiente> y closingAt es "2030-01-07T13:00:00Z"
    And cada ID no nulo referencia un item propio y currentBlockId y nextBlockId no son el mismo
    Examples:
      | hora     | actual | siguiente |
      | 08:59:59 | null   | A         |
      | 09:00:00 | A      | B         |
      | 10:00:00 | B      | C         |
      | 11:00:00 | null   | C         |
      | 13:00:00 | null   | null      |

  @s8
  Scenario Outline: Conservar reservas de entidades terminadas con nombres actuales
    Given una reserva propia cuya tarea está <tarea> y proyecto está <proyecto>
    And sus nombres persistidos actuales son "Proyecto actualizado" y "Tarea actualizada"
    When consulto Hoy
    Then la reserva sigue incluida con esos projectName y taskTitle actuales
    And se conservan los nueve campos originales del bloque, sin estado derivado de haber pasado su horario
    Examples:
      | tarea     | proyecto  |
      | pending   | active    |
      | completed | active    |
      | pending   | completed |
      | completed | completed |

  @s9
  Scenario: Devolver toda la agenda ordenada sin cursor ni lecturas HTTP por tarea
    Given veintiuna reservas propias válidas que intersectan el día y fueron insertadas en orden diferente a sus inicios
    When consulto Hoy
    Then items incluye las veintiuna una sola vez ordenadas por startAt y después por UUID canónico ascendente
    And no hay cursor, truncamiento ni petición HTTP adicional por nombre de proyecto o tarea
    And current, next, cierre y capacidad se calculan sobre la agenda completa

  @s10
  Scenario Outline: Leer preferencia, nombres y reservas desde un único snapshot coherente
    Given la lectura ha fijado el snapshot <estadoInicial>
    And otra transacción confirma después <cambio> antes de terminar la consulta
    When se completa GET /api/v1/today
    Then la respuesta contiene sólo los datos coherentes de <estadoInicial>, no mezcla nombres, preferencia o reservas del cambio
    And ninguna entidad ni outbox se modifica por la lectura
    Examples:
      | estadoInicial                          | cambio                                               |
      | preferencia UTC y reserva con nombre A  | nueva zona, nuevo nombre y nueva reserva atómicamente |
      | ausencia de preferencia y agenda vacía  | primera preferencia y primera reserva atómicamente   |

  @s11
  Scenario: Capturar el reloj de servidor una sola vez aunque cambie el día durante la lectura
    Given la captura inicial del reloj es "2030-01-07T23:59:59Z" y el reloj avanza después a "2030-01-08T00:00:01Z"
    When consulto Hoy
    Then serverNow es "2030-01-07T23:59:59Z" y date es "2030-01-07"
    And límites, capacidad, current y next se calculan con esa misma captura inicial
    And dayStartAt es menor o igual a serverNow y serverNow es estrictamente menor que dayEndAt

  @s12
  Scenario: Aislar agenda, nombres y resúmenes por propietario autenticado
    Given persona-b tiene reservas y nombres privados dentro del día de persona-a
    And persona-a no tiene reservas
    When persona-a consulta Hoy
    Then recibe items vacío, plannedSeconds0 y currentBlockId, nextBlockId y closingAt null
    And ninguna respuesta incluye IDs, nombres, títulos o capacidad de persona-b

  @s13
  Scenario: Aplicar autenticación antes de validar parámetros
    Given no existe sesión autenticada
    When envío GET /api/v1/today?ownerId=persona-b
    Then recibo401 conforme al filtro de seguridad existente, sin agenda ni datos privados

  @s14
  Scenario Outline: Rechazar cualquier parámetro cliente sin alterar el día del servidor
    When envío GET /api/v1/today con <consulta>
    Then recibo400 VALIDATION_ERROR con un error del campo <campo>, sin snapshot parcial
    And no se escribe ninguna entidad ni outbox
    Examples:
      | consulta             | campo   |
      | date=2030-01-08       | date    |
      | zoneId=Europe/Madrid  | zoneId  |
      | ownerId=persona-b     | ownerId |
      | cursor=antiguo        | cursor  |
      | extra=               | extra   |

  @s15
  Scenario Outline: Diferenciar fallo de almacenamiento de ausencia de preferencia
    Given falla <fase> de la lectura de Hoy
    When consulto Hoy
    Then recibo503 STORAGE_UNAVAILABLE con el formato de error existente y no-store
    And no recibo un200 de disponibilidad no configurada, agenda vacía ni snapshot parcial
    And no se exponen SQL, trazas, secretos o datos ajenos y no se escribe ninguna entidad ni outbox
    Examples:
      | fase                                         |
      | lectura de disponibilidad                    |
      | lectura de reservas y nombres                |
      | cierre de la transacción de lectura          |

  @s16
  Scenario Outline: Rechazar respuestas de Hoy incompatibles con su esquema cerrado
    Given una respuesta200 cuyo único defecto es <defecto>
    When el cliente intenta presentarla
    Then muestra fallo de lectura y opción de reintento, sin confirmar una agenda vacía o parcial
    Examples:
      | defecto                                                        |
      | falta un campo obligatorio del objeto15                         |
      | campo extra en el objeto o en un item                           |
      | tipo incompatible en un resumen o nombre                        |
      | block incumple el DTO9 heredado de feature11                     |
      | ID de bloque duplicado                                         |
      | items fuera del orden contractual                              |
      | currentBlockId o nextBlockId no pertenece a items                |

  @s17
  Scenario Outline: Rechazar payloads temporalmente incoherentes aunque sus tipos sean válidos
    Given una respuesta200 cuyo único defecto es <defecto>
    When el cliente intenta presentarla
    Then muestra fallo de lectura sin presentar datos incoherentes como confirmados
    Examples:
      | defecto                                                                   |
      | serverNow queda fuera del intervalo semiabierto del día                    |
      | date no es una fecha ISO de calendario válida                         |
      | un item no intersecta el día                                              |
      | plannedSeconds no es la suma de intersecciones                             |
      | remainingSeconds o excessSeconds no corresponde al presupuesto             |
      | fallback declara presupuesto conocido o AVAILABILITY declara null          |
      | currentBlockId referencia un bloque que ya terminó                         |
      | nextBlockId omite el primer inicio estrictamente futuro                    |
      | closingAt no coincide con el mayor fin real o no es null con agenda vacía  |

  @s18
  Scenario: Presentar agenda legible con enlaces existentes sin afirmar trabajo realizado
    Given Hoy devuelve bloques de varios proyectos, incluidos bloques de entidades completed y uno que acaba mañana
    When se confirma la lectura en la pantalla raíz
    Then muestra fecha, zona, projectName, taskTitle, objetivo e intervalo real de cada reserva con enlace a /proyectos/{projectId}/tareas/{taskId}
    And los textos persistidos se presentan como texto, no como HTML ejecutable
    And muestra "En horario planificado", "Próximo inicio planificado", "Cierre previsto" y "Según actualización de" con hora del servidor
    And el cierre de mañana incluye su fecha o la palabra "mañana"
    And los resúmenes distinguen "Tiempo planificado", "Presupuesto del día", "Presupuesto sin reservar" y exceso
    And no presenta sesión de trabajo, progreso realizado, auto-completado ni creación de bloques desde Hoy

  @s19
  Scenario Outline: Explicar fallback de preferencia sin ocultar agenda ni fingir capacidad cero
    Given el snapshot confirmado tiene zoneSource <fuente> y presupuesto null
    When se presenta Hoy
    Then muestra UTC explícito, motivo <motivo>, capacidad desconocida y enlace a /disponibilidad
    And conserva la agenda propia y no muestra presupuesto cero ni la zona del dispositivo
    Examples:
      | fuente       | motivo                           |
      | UNCONFIGURED | disponibilidad no configurada    |
      | UNAVAILABLE  | zona guardada no disponible       |

  @s20
  Scenario Outline: Conservar instantes cuando Intl no reconoce una zona válida del contrato
    Given un snapshot válido cuya zona <zona> no puede formatearse con Intl del navegador
    When se presenta Hoy
    Then el cliente acepta el DTO y muestra los instantes afectados como UTC etiquetado junto al ID original de zona
    And no usa silenciosamente la zona del dispositivo ni consulta otro catálogo para aceptar el bloque histórico
    Examples:
      | zona                                      |
      | efectiva resoluble por el servidor        |
      | histórica de uno de sus bloques           |

  @s21
  Scenario: Diferenciar agenda vacía de carga o fallo inicial
    Given GET de Hoy está pendiente al entrar
    When la lectura confirma un snapshot válido sin items
    Then antes de confirmación se anuncia carga y después se explica que no hay bloques planificados
    And ofrece un enlace a /proyectos y conserva presupuesto conocido si lo hay
    And no muestra alerta de lectura ni inventa current, next o cierre

  @s22
  Scenario: Mostrar fallo inicial recuperable sin persistir datos privados
    Given la primera lectura de Hoy falla por red o respuesta inválida
    When se presenta ese resultado
    Then muestra fallo y "Reintentar", sin estado vacío de negocio
    And no guarda respuestas exitosas ni errores de Hoy en almacenamiento persistente del navegador

  @s23
  Scenario: Actualizar manualmente conservando el snapshot fechado del mismo día
    Given Hoy muestra un snapshot confirmado del día y la siguiente lectura permanece pendiente
    When pulso "Actualizar"
    Then se emite una sola lectura y se conservan datos con indicación accesible de actualización pendiente
    And se mantiene "Según actualización de" con la hora del snapshot mostrado hasta confirmar su reemplazo
    And no se presenta una confirmación anticipada ni se escribe un bloque

  @s24
  Scenario Outline: Agrupar recuperación de foco y visibilidad sin duplicar peticiones
    Given <peticion>
    When llegan los eventos <eventos> de una misma recuperación de Hoy
    Then hay exactamente una petición vigente de Hoy compartida por esos disparadores
    And no se inicia polling periódico, segundero ni WebSocket
    Examples:
      | peticion                          | eventos                          |
      | ninguna petición vigente          | visible y focus                  |
      | actualización manual pendiente    | focus                            |
      | entrada inicial pendiente         | visible y focus                  |

  @s25
  Scenario Outline: Programar una única frontera estrictamente futura
    Given el snapshot tiene serverNow10:00 y <fronteras> en el mismo día
    When se programa su próxima actualización mientras Hoy está visible
    Then existe un único timeout para <siguiente> y al vencer se invalida la generación y se consulta Hoy una vez
    And no se programa una frontera pasada o igual a serverNow ni una repetición periódica
    Examples:
      | fronteras                                                    | siguiente |
      | bloque próximo inicia10:15 y acaba11:00, dayEndAt24:00          | 10:15     |
      | bloque actual acaba10:30 y siguiente inicia11:00               | 10:30     |
      | agenda sin fronteras futuras salvo dayEndAt24:00               | 24:00     |
      | un bloque acaba10:00 y el próximo inicia10:20                  | 10:20     |

  @s26
  Scenario: Anclar la espera al snapshot recibido y no al reloj del dispositivo
    Given se recibió serverNow10:00 con próxima frontera10:10 y han transcurrido120 segundos desde su recepción
    When la fecha o zona del dispositivo cambia sin alterar el tiempo transcurrido
    Then la espera restante es480 segundos y se conserva la fecha/zona explícita del snapshot
    And no se consulta ni se cambia el bloque actual sólo por ese cambio del dispositivo

  @s27
  Scenario: Cancelar la frontera al ocultarse y reconstruirla con lectura nueva al regresar
    Given Hoy está visible con una frontera pendiente
    When pasa a estar oculto
    Then se cancela ese timeout y no dispara una lectura de frontera mientras siga oculto
    And la siguiente recuperación visible/con foco exige un snapshot nuevo antes de volver a programar la frontera

  @s28
  Scenario Outline: Retirar la agenda anterior al vencer el día
    Given Hoy muestra el7 de enero y acaba de vencer su dayEndAt
    When la lectura del nuevo día termina con <resultado>
    Then desde el vencimiento ya no se presenta la agenda del7 de enero como Hoy
    And durante la espera se anuncia carga del nuevo día
    And al terminar aparece <pantalla>
    Examples:
      | resultado                         | pantalla                                  |
      | snapshot válido del8 de enero      | agenda o vacío confirmado del8 de enero    |
      | fallo de lectura                   | fallo y reintento, sin vacío de negocio    |

  @s29
  Scenario: Conservar lectura anterior del mismo día como sin actualizar tras fallo de refresco
    Given existe un snapshot del día y una actualización disparada por su frontera
    When falla esa actualización
    Then conserva el snapshot claramente marcado "Sin actualizar", con su fecha/hora y reintento manual
    And no vuelve a programar automáticamente la frontera ya vencida
    And una recuperación visible/con foco posterior puede reintentar, sin prometer cambios externos antes de una nueva frontera o actualización

  # Para JSON de éxito se difiere su lectura después de recibir headers.
  # Para errores se mantiene fetch pendiente hasta entregar Response, después de retirar el contexto;
  # el observador de 401 actúa síncronamente al recibir Response, sin una ventana posterior inventada.
  @s30
  Scenario Outline: Ignorar resultados obsoletos en ruta, sesión o generación posteriores
    Given una petición de Hoy conserva <resultado> pendiente en la frontera de entrega correspondiente
    And su contexto se invalida por <cambio> y ya existe una pantalla o lectura posterior
    When se libera el resultado de la petición antigua
    Then no cambia agenda, error, foco ni acceso de la pantalla o sesión posterior
    And se canceló la solicitud antigua cuando fue posible
    Examples:
      | resultado               | cambio                                |
      | JSON de éxito           | salir a detalle de tarea              |
      | error401                | salir a Proyectos                     |
      | JSON de éxito           | cerrar sesión y entrar con otra       |
      | error401                | cerrar sesión y entrar con otra       |
      | JSON de éxito           | sustitución por generación de rollover|
      | error de lectura        | sustitución por actualización vigente |

  @s31
  Scenario: Retirar agenda privada inmediatamente al cerrar sesión
    Given Hoy muestra reservas privadas confirmadas
    When cierro sesión
    Then desaparecen agenda y resúmenes privados inmediatamente y la ruta se reinicia a /
    And el acceso posterior lleva a Hoy con una lectura de la sesión nueva, sin mostrar datos anteriores mientras carga

  @s32
  Scenario: Resolver Hoy en raíz y captura en su ruta explícita
    When entro autenticado directamente en /proyectos/nuevo
    Then aparece la captura existente de proyecto y no se solicita un detalle con ID nuevo
    And / sigue siendo Hoy, sin formulario de captura accidental

  @s33
  Scenario Outline: Migrar los enlaces de creación sin romper el recorrido existente
    Given estoy en <origen>
    When sigo su enlace de creación de proyecto
    Then navego a /proyectos/nuevo y puedo usar el mismo formulario de captura
    Examples:
      | origen                       |
      | lista de proyectos con datos |
      | lista de proyectos vacía     |

  @s34
  Scenario Outline: Conservar entradas directas y retorno autenticado de rutas existentes
    Given la ruta solicitada es <ruta>
    When se completa su entrada autenticada o retorno de autenticación
    Then se presenta <destino>, sin redirigirlo indebidamente a captura o a Hoy
    Examples:
      | ruta                              | destino                   |
      | /                                 | Hoy                       |
      | /proyectos                        | lista de proyectos        |
      | /proyectos?cursor=vigente          | página de proyectos       |
      | /proyectos/P                      | detalle del proyecto P    |
      | /proyectos/P/editar               | edición del proyecto P    |
      | /proyectos/P/tareas/T              | detalle de la tarea T     |
      | /disponibilidad                   | disponibilidad personal   |

  @s35
  Scenario Outline: Mostrar página no encontrada para rutas desconocidas
    When entro en <ruta> autenticado
    Then veo un mensaje de página no encontrada con enlaces Hoy y Proyectos
    And no aparece el formulario de creación ni se inventa una consulta de proyecto o tarea a partir del sufijo
    Examples:
      | ruta                              |
      | /desconocida                      |
      | /proyectos/nuevo/extra             |
      | /proyectos/P/tareas/T/extra        |

  @s36
  Scenario Outline: Reflejar una única sección activa en navegación y breadcrumb
    When entro en <ruta> autenticado
    Then Workspace presenta enlaces Hoy, Proyectos y Disponibilidad con sólo <seccion> como aria-current
    And el breadcrumb corresponde a esa ruta y el skip link llega al contenido principal enfocable
    Examples:
      | ruta                         | seccion        |
      | /                            | Hoy            |
      | /proyectos                   | Proyectos      |
      | /proyectos/nuevo             | Proyectos      |
      | /proyectos/P/tareas/T         | Proyectos      |
      | /disponibilidad              | Disponibilidad |

  @s37
  Scenario Outline: Anunciar lectura y recuperación sin robar foco elegido
    Given el usuario ha elegido con teclado un enlace o control que permanece visible
    When Hoy termina <operacion>
    Then se anuncia accesiblemente el estado correspondiente y se conserva ese foco elegido
    And Actualizar, Reintentar y enlaces de tarea son operables con teclado y tienen foco visible cuando existen
    Examples:
      | operacion                             |
      | carga inicial                         |
      | actualización confirmada              |
      | actualización fallida                 |

  @s38
  Scenario: Mantener agenda responsive y evidencia UX del recorrido completo
    Given estados de carga, vacío, agenda con textos largos Unicode, actualización y error
    When se revisa Hoy según la matriz de docs/ux-requirements.md
    Then conserva información y acciones sin solapes, recortes ni scroll horizontal accidental entre320 y2560px CSS y a ambos lados de cada breakpoint
    And conserva orden de lectura/teclado, foco visible y controles principales de al menos44 por44px CSS en alturas reducidas y zoom nativo200%
    And el feedback visual de carga o actualización se mide con objetivo menor de400ms sin prometer respuesta de red en ese plazo
    And se registran resultados de Chromium, Firefox y WebKit y las treinta filas UX con evidencia o límites explícitos
    And axe o emulación por sí solos no se presentan como certificación de dispositivos físicos, lector de pantalla real o facilidad de uso humana
