@ics_calendar
Feature: Suscribir un calendario externo a los bloques planificados propios mediante un feed iCalendar de solo lectura
  Como persona autenticada quiero un enlace ICS secreto y una descarga .ics con mis bloques vigentes
  para verlos en Google Calendar, Apple Calendar, Thunderbird u Outlook sin acreditar trabajo ni sincronizar de vuelta.
  Fuente normativa: progress/proposal_ics_calendar.md (sección 26). Un bloque sigue siendo una reserva de tiempo.
  Feed público: GET /calendar/{token}.ics. Descarga: GET /api/v1/me/calendar.ics. Estado y gestión: /api/v1/me/calendar-feed.
  <token> designa los 43 caracteres base64url [A-Za-z0-9_-] devueltos dentro de url por el último POST de persona-a.
  Las cabeceras citadas se comparan exactas. Los documentos iCalendar son UTF-8 sin BOM y cada línea termina en CRLF;
  una línea de continuación plegada empieza por exactamente un espacio. Content-Length cuenta los octetos del cuerpo sin comprimir.
  Errores: application/problem+json con type, title, status y code, sin SQL, trazas, secretos ni el token.
  Cada fila de Examples es independiente; lo no mencionado permanece válido.

  Background:
    Given el reloj del servidor está fijado en 2026-09-08T12:00:00Z
    And app.public-origin es https://organizacion.apptolast.com
    And persona-a tiene sesión válida, protección de origen y CSRF válida para POST y DELETE
    And persona-a tiene disponibilidad configurada en la zona Europe/Madrid
    And persona-a posee el proyecto P con id 0f2b3a1c-9d8e-4f70-a1b2-c3d4e5f60718 en estado active
    And P contiene la tarea T con id 7c1e5d2a-3b4f-4a6c-9d8e-0f1a2b3c4d5e y título "Revisión; plan, fase 2"
    And T tiene el bloque B con id 3a9f1e62-5b7c-4d0e-8f21-6a4b9c0d1e2f, versión 1 y sin proyección
    And B está planificado de 2026-10-25T09:00 a 2026-10-25T10:30 en la zona Europe/Madrid con offsets +01:00 y createdAt 2026-09-01T09:15:30.123456Z
    And el objetivo persistido de B es "Cerrar conclusiones y anotar las dudas pendientes de esa sección", un salto de línea LF, y "Segundo paso: enviar la versión final a revisión\externa; sin adjuntos, sólo texto"
    And persona-b posee su propio proyecto, tarea y bloque planificado dentro de la ventana
    And no existe ningún token de feed salvo los indicados

  @s1
  Scenario: Crear el enlace de suscripción entrega el token una sola vez y persiste solo su hash
    Given persona-a no tiene token de feed
    When envía POST /api/v1/me/calendar-feed con cuerpo vacío
    Then recibe 201 con JSON de exactamente url y createdAt
    And url es https://organizacion.apptolast.com/calendar/<token>.ics y <token> tiene exactamente 43 caracteres del alfabeto [A-Za-z0-9_-]
    And createdAt es un instante UTC con microsegundos igual al reloj del servidor
    And existe exactamente una fila de token para persona-a cuya token_hash mide 32 octetos y es igual al SHA-256 de los 43 caracteres de <token>
    And la fila no contiene el token en claro y no se crea outbox, evento ni fila en otra tabla

  @s2
  Scenario: Consultar el estado sin enlace no crea nada
    Given persona-a no tiene token de feed
    When consulta GET /api/v1/me/calendar-feed
    Then recibe 200 con JSON de exactamente active false y createdAt null
    And no se crea fila de token, outbox ni evento

  @s3
  Scenario: Consultar el estado con enlace nunca revela el token ni su hash
    Given persona-a creó su token con createdAt 2026-09-08T12:00:00.000000Z
    When consulta GET /api/v1/me/calendar-feed
    Then recibe 200 con JSON de exactamente active true y createdAt 2026-09-08T12:00:00.000000Z
    And el cuerpo no contiene <token>, su SHA-256 en ninguna codificación ni ninguna url

  @s4
  Scenario: Regenerar sustituye el único token activo e invalida el anterior desde el commit
    Given persona-a creó el token <token> y el reloj avanza a 2026-09-08T12:05:00Z
    When envía de nuevo POST /api/v1/me/calendar-feed con cuerpo vacío
    Then recibe 201 con una url distinta cuyo token nuevo también tiene 43 caracteres base64url y createdAt 2026-09-08T12:05:00.000000Z
    And sigue existiendo exactamente una fila de token para persona-a, ahora con el SHA-256 del token nuevo
    And GET /calendar/<token>.ics responde 404 CALENDAR_NOT_FOUND y GET /calendar/<token nuevo>.ics responde 200

  @s5
  Scenario Outline: Revocar borra el token y es idempotente
    Given persona-a <situacion>
    When envía DELETE /api/v1/me/calendar-feed
    Then recibe 204 sin cuerpo
    And no existe ninguna fila de token para persona-a y GET /api/v1/me/calendar-feed devuelve active false y createdAt null
    And <comprobacion>
    Examples:
      | situacion                         | comprobacion                                                    |
      | tiene el token <token> activo     | GET /calendar/<token>.ics responde 404 CALENDAR_NOT_FOUND       |
      | no tiene token de feed            | la fila de token de persona-b permanece intacta                 |
      | ya revocó su token en un DELETE previo | no se produce error ni escritura adicional                 |

  @s6
  Scenario Outline: Un cuerpo no vacío en la generación se rechaza sin tocar el token
    Given persona-a tiene el token <token> activo
    When envía POST /api/v1/me/calendar-feed con cuerpo <cuerpo> como application/json
    Then recibe 400 VALIDATION_ERROR
    And la fila de token de persona-a conserva su hash y createdAt y GET /calendar/<token>.ics sigue respondiendo 200
    Examples:
      | cuerpo             |
      | {}                 |
      | {"name":"casa"}    |
      | null               |
      | ""                 |
      | []                 |

  @s7
  Scenario Outline: Sin sesión ningún endpoint de gestión ni la descarga responden datos
    Given una petición sin cookie de sesión, aunque incluya una cabecera Authorization Bearer con cualquier valor
    When envía <metodo> <ruta>
    Then recibe 401 UNAUTHENTICATED como application/problem+json
    And no se crea, modifica ni borra ninguna fila de token y no hay Set-Cookie de sesión nueva
    Examples:
      | metodo | ruta                          |
      | GET    | /api/v1/me/calendar-feed      |
      | POST   | /api/v1/me/calendar-feed      |
      | DELETE | /api/v1/me/calendar-feed      |
      | GET    | /api/v1/me/calendar.ics       |

  @s8
  Scenario Outline: Generar y revocar conservan CSRF y origen; el feed público no los exige
    Given persona-a tiene sesión válida y el token <token> activo
    When envía <metodo> <ruta> con <defecto>
    Then recibe <resultado>
    And la fila de token de persona-a queda exactamente igual que antes
    Examples:
      | metodo | ruta                       | defecto                                | resultado                     |
      | POST   | /api/v1/me/calendar-feed   | token CSRF ausente                     | 403 CSRF_INVALID              |
      | DELETE | /api/v1/me/calendar-feed   | token CSRF ausente                     | 403 CSRF_INVALID              |
      | POST   | /api/v1/me/calendar-feed   | cabecera Origin no confiable           | 403 UNTRUSTED_ORIGIN          |
      | DELETE | /api/v1/me/calendar-feed   | cabecera Origin no confiable           | 403 UNTRUSTED_ORIGIN          |
      | GET    | /calendar/<token>.ics      | sin cookie, sin CSRF y sin Origin      | 200 text/calendar; charset=utf-8 |

  @s9
  Scenario: Dos regeneraciones concurrentes dejan exactamente un token activo
    Given persona-a tiene el token <token> activo
    When dos POST /api/v1/me/calendar-feed con cuerpo vacío se ejecutan a la vez desde la misma sesión
    Then ambos reciben 201 con urls distintas entre sí y distintas de la de <token>
    And existe exactamente una fila de token para persona-a
    And exactamente una de las dos urls responde 200 en GET /calendar/{token}.ics y la otra responde 404 CALENDAR_NOT_FOUND
    And GET /api/v1/me/calendar-feed devuelve active true y el createdAt de la respuesta cuya url responde 200
    And GET /calendar/<token>.ics responde 404 CALENDAR_NOT_FOUND

  @s10
  Scenario: Una colisión de hash u otro fallo de escritura no deja al propietario sin su token anterior
    Given persona-a tiene el token <token> activo
    And el almacenamiento rechaza la siguiente escritura de token por violación de unicidad de token_hash
    When envía POST /api/v1/me/calendar-feed con cuerpo vacío
    Then recibe 503 STORAGE_UNAVAILABLE como application/problem+json sin url
    And se realiza exactamente un intento de escritura, sin reintento automático
    And la fila de token de persona-a conserva su hash y createdAt y GET /calendar/<token>.ics sigue respondiendo 200

  @s11
  Scenario Outline: El feed público responde sin credenciales, con cabeceras exactas y sin escribir ni registrar nada
    Given persona-a tiene el token <token> activo
    When un cliente sin cookies, sin CSRF y sin Authorization envía <metodo> /calendar/<token>.ics
    # Enmienda del 9 de septiembre de 2026, ratificada por el propietario. El espacio
    # tras el punto y coma es OPCIONAL: RFC 9110 lo declara espacio en blanco
    # opcional entre parámetros, y Tomcat entrega la forma sin espacio sin que
    # ninguna capa de la aplicación pueda cambiarlo. Donde este contrato escribe
    # «text/calendar; charset=utf-8» se admite igualmente «text/calendar;charset=utf-8».
    Then recibe 200 con Content-Type text/calendar; charset=utf-8, Cache-Control private, no-store y X-Content-Type-Options nosniff
    And Content-Length es 714 y <cuerpo>
    And no incluye Content-Disposition, Set-Cookie, ETag ni Content-Encoding distinto de identity
    And no cambia ninguna fila de token, bloque ni proyección, y no se escribe outbox ni evento
    And los logs de aplicación capturados durante la petición no contienen <token> ni la ruta /calendar/
    Examples:
      | metodo | cuerpo                                        |
      | GET    | el cuerpo mide exactamente 714 octetos        |
      | HEAD   | el cuerpo está vacío                          |

  @s12
  Scenario: El documento con un bloque se serializa byte a byte según RFC 5545
    Given persona-a tiene el token <token> activo y el único bloque vigente en la ventana es B
    When consulta GET /calendar/<token>.ics
    Then el cuerpo son exactamente estas 21 líneas físicas, cada una terminada en CRLF, 714 octetos en total y sin BOM:
      """
      BEGIN:VCALENDAR
      VERSION:2.0
      PRODID:-//apptolast//OrganizationWeb//ES
      CALSCALE:GREGORIAN
      METHOD:PUBLISH
      X-WR-CALNAME:Bloques planificados
      X-WR-TIMEZONE:Europe/Madrid
      BEGIN:VEVENT
      UID:3a9f1e62-5b7c-4d0e-8f21-6a4b9c0d1e2f@organizacion.apptolast.com
      DTSTAMP:20260901T091530Z
      DTSTART:20261025T080000Z
      DTEND:20261025T093000Z
      SUMMARY:Revisión\; plan\, fase 2
      DESCRIPTION:Cerrar conclusiones y anotar las dudas pendientes de esa secci
       ón\nSegundo paso: enviar la versión final a revisión\\externa\; sin adj
       untos\, sólo texto
      SEQUENCE:1
      URL:https://organizacion.apptolast.com/proyectos/0f2b3a1c-9d8e-4f70-a1b2-c3
       d4e5f60718/tareas/7c1e5d2a-3b4f-4a6c-9d8e-0f1a2b3c4d5e
      END:VEVENT
      END:VCALENDAR
      """
    And la primera línea física de DESCRIPTION mide 74 octetos porque los dos octetos de "ó" no se parten, la segunda mide 75 con su espacio inicial y la tercera 20
    And la primera línea física de URL mide 75 octetos y la segunda 55 con su espacio inicial
    And DTSTAMP procede de createdAt 2026-09-01T09:15:30.123456Z truncado a segundos y una segunda petición devuelve los mismos 714 octetos
    And no aparecen VTIMEZONE, TZID, STATUS, TRANSP, CATEGORIES, ORGANIZER, ATTENDEE, VALARM, RRULE, RDATE, EXDATE, LAST-MODIFIED, CREATED ni la zona Europe/Madrid del bloque

  @s13
  Scenario: Sin bloques en la ventana el calendario se emite vacío, no como 404 ni 204
    Given persona-a tiene el token <token> activo, no tiene disponibilidad configurada y ningún bloque vigente interseca la ventana
    When consulta GET /calendar/<token>.ics
    Then recibe 200 con Content-Length 158 y el cuerpo son exactamente estas 7 líneas terminadas en CRLF:
      """
      BEGIN:VCALENDAR
      VERSION:2.0
      PRODID:-//apptolast//OrganizationWeb//ES
      CALSCALE:GREGORIAN
      METHOD:PUBLISH
      X-WR-CALNAME:Bloques planificados
      END:VCALENDAR
      """
    And no contiene BEGIN:VEVENT ni X-WR-TIMEZONE

  @s14
  Scenario Outline: X-WR-TIMEZONE solo informa de la zona de disponibilidad y nunca altera un instante
    Given persona-a tiene el token <token> activo y <disponibilidad>
    When consulta GET /calendar/<token>.ics
    Then <linea>
    And el VEVENT de B conserva DTSTART:20261025T080000Z y DTEND:20261025T093000Z
    Examples:
      | disponibilidad                                | linea                                                                      |
      | disponibilidad configurada en Europe/Madrid   | la séptima línea es X-WR-TIMEZONE:Europe/Madrid, justo tras X-WR-CALNAME   |
      | disponibilidad configurada en America/Bogota  | la séptima línea es X-WR-TIMEZONE:America/Bogota                           |
      | ninguna disponibilidad configurada            | no existe ninguna línea X-WR-TIMEZONE y BEGIN:VEVENT es la séptima línea    |

  @s15
  Scenario Outline: Cualquier token inválido, revocado, sustituido o ruta ajena recibe el mismo 404
    Given persona-a creó el token <token> y después <historia>
    When un cliente anónimo envía GET <ruta>
    Then recibe 404 CALENDAR_NOT_FOUND como application/problem+json
    And el cuerpo es byte a byte idéntico y el conjunto de cabeceras (incluidas Cache-Control private, no-store y X-Content-Type-Options nosniff) es idéntico en todas las filas
    And no hay Set-Cookie, no se escribe nada y los logs no contienen la ruta solicitada
    And la resolución usa el SHA-256 completo del token como única clave de búsqueda, sin comparación parcial ni por prefijo
    Examples:
      | historia                                  | ruta                                                       |
      | nada                                      | /calendar/<token sin su último carácter>.ics (42)          |
      | nada                                      | /calendar/<token>A.ics (44 caracteres)                     |
      | nada                                      | /calendar/<42 caracteres de token>+.ics                    |
      | nada                                      | /calendar/<43 caracteres base64url nunca emitidos>.ics     |
      | nada                                      | /calendar/<token> (sin sufijo .ics)                        |
      | nada                                      | /calendar/<token>.ics?x=1                                  |
      | nada                                      | /calendar/                                                 |
      | nada                                      | /calendar/otra/<token>.ics                                 |
      | lo revocó con DELETE                      | /calendar/<token>.ics                                      |
      | lo sustituyó con un segundo POST          | /calendar/<token>.ics                                      |

  @s16
  Scenario Outline: El recurso público solo admite GET y HEAD
    Given persona-a tiene el token <token> activo
    When un cliente anónimo envía <metodo> /calendar/<candidato>.ics
    Then recibe 405 con cabecera Allow exactamente GET, HEAD
    And la respuesta es idéntica en estado, cabeceras y cuerpo tanto si <candidato> es <token> como si es un token nunca emitido
    And no se crea, modifica ni borra ninguna fila y no hay Set-Cookie
    Examples:
      | metodo |
      | POST   |
      | PUT    |
      | DELETE |
      | PATCH  |

  @s17
  Scenario Outline: La descarga con sesión entrega el mismo documento como adjunto, con o sin token activo
    Given persona-a <token_estado> y su único bloque vigente en la ventana es B
    When consulta GET /api/v1/me/calendar.ics con su sesión
    Then recibe 200 con los mismos 714 octetos que el feed público de @s12
    And Content-Type es text/calendar; charset=utf-8, Cache-Control private, no-store, X-Content-Type-Options nosniff y Content-Length 714
    And Content-Disposition es attachment; filename="organizationweb-bloques.ics", sin filename*
    And no se crea ni modifica ningún token
    Examples:
      | token_estado                 |
      | tiene el token <token> activo |
      | no tiene token de feed        |

  @s18
  Scenario Outline: La ventana es semiabierta: end_at posterior a now menos 30 días y start_at anterior a now más 365 días
    Given persona-a tiene el token <token> activo y un bloque vigente V con start_at <inicio> y end_at <fin>
    When consulta GET /calendar/<token>.ics
    Then el UID de V <presencia> en el documento
    Examples:
      | inicio               | fin                  | presencia    |
      | 2026-08-09T11:00:00Z | 2026-08-09T12:00:00Z | no aparece   |
      | 2026-08-09T11:00:01Z | 2026-08-09T12:00:01Z | aparece      |
      | 2027-09-08T12:00:00Z | 2027-09-08T13:00:00Z | no aparece   |
      | 2027-09-08T11:59:59Z | 2027-09-08T12:59:59Z | aparece      |
      | 2026-08-01T00:00:00Z | 2027-10-01T00:00:00Z | aparece      |
      | 2026-09-08T11:00:00Z | 2026-09-08T12:00:00Z | aparece      |

  @s19
  Scenario: Un bloque cancelado desaparece del feed
    Given persona-a tiene el token <token> activo y B fue cancelado según la feature 13 con proyección status cancelled
    When consulta GET /calendar/<token>.ics
    Then el documento no contiene UID:3a9f1e62-5b7c-4d0e-8f21-6a4b9c0d1e2f@organizacion.apptolast.com ni ningún BEGIN:VEVENT
    And la reserva original de B sigue existiendo sin cambios en la base de datos

  @s20
  Scenario: Un bloque movido publica el intervalo vigente, SEQUENCE igual a su versión y DTSTAMP de la proyección
    Given persona-a tiene el token <token> activo
    And B fue movido a 2026-10-26T09:00–2026-10-26T10:30 Europe/Madrid, proyección status planned, versión 2 y updated_at 2026-09-07T18:30:45.654321Z
    When consulta GET /calendar/<token>.ics
    Then el único VEVENT contiene exactamente, en este orden, UID:3a9f1e62-5b7c-4d0e-8f21-6a4b9c0d1e2f@organizacion.apptolast.com, DTSTAMP:20260907T183045Z, DTSTART:20261026T080000Z, DTEND:20261026T093000Z, SUMMARY, DESCRIPTION, SEQUENCE:2 y URL
    And no aparece el intervalo original 20261025T080000Z ni un segundo VEVENT para B

  @s21
  Scenario Outline: SUMMARY es siempre el título de la tarea y el estado de tarea o proyecto no excluye el bloque
    Given persona-a tiene el token <token> activo y <estado>
    When consulta GET /calendar/<token>.ics
    Then el VEVENT de B está presente con SUMMARY:Revisión\; plan\, fase 2 y URL con /proyectos/0f2b3a1c-9d8e-4f70-a1b2-c3d4e5f60718/tareas/7c1e5d2a-3b4f-4a6c-9d8e-0f1a2b3c4d5e
    And DESCRIPTION es el objetivo persistido escapado, sin usar el nombre del proyecto como SUMMARY
    Examples:
      | estado                                   |
      | T está completed                         |
      | P está completed                         |
      | P está paused                            |
      | el título de T se cambió a "Cierre final" y SUMMARY pasa a ser Cierre final |

  @s22
  Scenario: Dos bloques con la misma hora local a ambos lados del cambio de hora se publican en UTC distintos
    Given persona-a tiene el token <token> activo
    And un bloque D1 planificado de 2026-10-24T09:00 a 10:00 en Europe/Madrid con offsets +02:00 y un bloque D2 de 2026-10-25T09:00 a 10:00 en Europe/Madrid con offsets +01:00
    When consulta GET /calendar/<token>.ics
    Then D1 tiene DTSTART:20261024T070000Z y DTEND:20261024T080000Z
    And D2 tiene DTSTART:20261025T080000Z y DTEND:20261025T090000Z
    And ningún VEVENT contiene TZID, VTIMEZONE ni un instante sin sufijo Z

  @s23
  Scenario Outline: Los valores TEXT se escapan y un lector conforme recupera el texto persistido
    Given persona-a tiene el token <token> activo y el objetivo persistido de B es <persistido>
    When consulta GET /calendar/<token>.ics
    Then el valor de DESCRIPTION, una vez desplegado, es exactamente <serializado>
    And al desescapar ese valor se obtiene <recuperado>
    Examples:
      | persistido                              | serializado                                           | recuperado                        |
      | a barra-invertida b                     | a barra-invertida barra-invertida b                   | a barra-invertida b               |
      | a;b                                     | a barra-invertida ;b                                  | a;b                               |
      | a,b                                     | a barra-invertida ,b                                  | a,b                               |
      | a LF b                                  | a barra-invertida n b                                 | a LF b                            |
      | a CRLF b                                | a barra-invertida n b                                 | a LF b                            |
      | a CR-aislado b                          | ab                                                    | ab                                |
      | a:b "c" d                               | a:b "c" d                                             | a:b "c" d                         |
      | 500 puntos de código con "ñ" y "😀"     | los mismos 500 puntos de código, plegados             | los 500 puntos de código exactos  |

  @s24
  Scenario Outline: El plegado corta a 75 octetos sin partir caracteres UTF-8 ni secuencias de escape
    Given persona-a tiene el token <token> activo y el objetivo de B produce una línea DESCRIPTION cuyo <contenido>
    When consulta GET /calendar/<token>.ics
    Then <resultado>
    And cada línea física del documento mide como máximo 75 octetos sin contar CRLF y cada continuación empieza por exactamente un espacio
    And al retirar cada CRLF seguido de espacio y desescapar se obtiene exactamente el objetivo persistido
    Examples:
      | contenido                                                                   | resultado                                                                                         |
      | valor son 63 caracteres ASCII (línea de exactamente 75 octetos)              | la línea no se pliega: una única línea física de 75 octetos                                       |
      | valor son 64 caracteres ASCII (línea de 76 octetos)                          | primera línea de 75 octetos y una continuación de 2 octetos: espacio y el carácter 64             |
      | octeto 75 sería el primer octeto de "ó"                                      | primera línea de 74 octetos y la continuación empieza por espacio y "ó" íntegra                   |
      | octetos 73 a 76 serían los cuatro de "😀"                                    | primera línea de 72 octetos y la continuación empieza por espacio y "😀" íntegro                  |
      | octeto 75 sería la barra invertida de un escape de coma                     | primera línea de 74 octetos y la continuación empieza por espacio y el escape de coma completo   |

  @s25
  Scenario: Los eventos se ordenan por DTSTART y después por UID
    Given persona-a tiene el token <token> activo y tres bloques vigentes insertados en orden inverso al esperado
    And E1 empieza 2026-10-20T08:00:00Z con id 00000000-0000-4000-8000-000000000002, E2 empieza 2026-10-20T08:00:00Z con id 00000000-0000-4000-8000-000000000001 y E3 empieza 2026-10-19T08:00:00Z
    When consulta GET /calendar/<token>.ics
    Then los VEVENT aparecen en el orden E3, E2, E1
    And dos peticiones consecutivas devuelven documentos byte a byte idénticos

  @s26
  Scenario Outline: Más de 2000 eventos en la ventana producen 413 sin cuerpo parcial
    Given persona-a tiene el token <token> activo y <cantidad> bloques vigentes dentro de la ventana
    When consulta <ruta>
    Then recibe <resultado>
    Examples:
      | cantidad | ruta                          | resultado                                                                                         |
      | 2000     | GET /calendar/<token>.ics     | 200 con exactamente 2000 BEGIN:VEVENT y Content-Length igual a los octetos del cuerpo              |
      | 2001     | GET /calendar/<token>.ics     | 413 CALENDAR_TOO_LARGE como application/problem+json sin ninguna línea BEGIN:VCALENDAR             |
      | 2001     | GET /api/v1/me/calendar.ics   | 413 CALENDAR_TOO_LARGE sin Content-Disposition ni bytes de calendario                              |
      | 2001 de los que 1 está cancelado | GET /calendar/<token>.ics | 200 con exactamente 2000 BEGIN:VEVENT                                                  |

  @s27
  Scenario: El feed de un propietario nunca contiene bloques ni tokens de otro
    Given persona-a y persona-b tienen cada una su token activo y bloques vigentes en la ventana
    When un cliente anónimo consulta el feed de persona-a y el de persona-b
    Then el documento de persona-a contiene solo UIDs de bloques de proyectos de persona-a y el de persona-b solo los suyos
    And ningún documento contiene títulos, objetivos ni ids de la otra identidad
    And el token de persona-b no resuelve el feed de persona-a ni al revés

  @s28
  Scenario: Una lectura del feed sale entera de un único snapshot
    Given persona-a tiene el token <token> activo y B con versión 1
    And durante la lectura otra transacción confirma el movimiento de B a versión 2 con nuevo intervalo y updated_at
    When termina la generación del documento
    Then el VEVENT de B es íntegramente la versión 1 (DTSTART:20261025T080000Z, DTSTAMP:20260901T091530Z, SEQUENCE:1) o íntegramente la versión 2 (nuevo DTSTART, DTSTAMP del updated_at, SEQUENCE:2)
    And nunca combina el intervalo de una versión con SEQUENCE o DTSTAMP de la otra

  @s29
  Scenario: Reiniciar el backend conserva el token y el cliente de calendario sigue funcionando
    Given persona-a creó el token <token> con createdAt 2026-09-08T12:00:00.000000Z
    And el proceso del backend se reinicia contra la misma base de datos
    When un cliente anónimo consulta GET /calendar/<token>.ics
    Then recibe 200 con los mismos octetos que antes del reinicio
    And GET /api/v1/me/calendar-feed devuelve active true y createdAt 2026-09-08T12:00:00.000000Z

  @s30
  Scenario Outline: Un almacenamiento no disponible produce 503 sin documento parcial
    Given persona-a tiene el token <token> activo y el almacenamiento no está disponible
    When consulta <ruta>
    Then recibe 503 STORAGE_UNAVAILABLE como application/problem+json
    And no hay Content-Disposition, ninguna línea BEGIN:VCALENDAR, trazas ni el token en cuerpo o logs
    Examples:
      | ruta                          |
      | GET /calendar/<token>.ics     |
      | GET /api/v1/me/calendar.ics   |
      | GET /api/v1/me/calendar-feed  |

  @s31
  Scenario Outline: Abrir Calendario solo consulta el estado y lo anuncia
    Given persona-a autenticada en la aplicación y GET /api/v1/me/calendar-feed <respuesta>
    When abre Calendario desde la entrada de navegación Principal situada justo después de Exportación
    Then la ruta es /calendario y muestra el h1 Calendario ICS
    And la ayuda explica que el feed contiene bloques vigentes de 30 días atrás a un año, que la URL es secreta y de solo lectura y que no sincroniza cambios desde el calendario externo
    And la única petición emitida es GET /api/v1/me/calendar-feed, sin POST ni DELETE
    And <estado>
    And ni la URL ni el token se escriben en localStorage, sessionStorage ni consola
    Examples:
      | respuesta                                             | estado                                                                                                                                                  |
      | está retenida                                         | anuncia Cargando… antes de 400 ms y no muestra Crear enlace de suscripción, Regenerar enlace ni Revocar enlace                                          |
      | devuelve active false y createdAt null                | muestra la acción primaria Crear enlace de suscripción con el aviso de que quien conozca el enlace verá títulos de tareas y objetivos, sin Regenerar ni Revocar |
      | devuelve active true y createdAt 2026-09-08T12:00:00Z | muestra la fecha de creación legible, Regenerar enlace y Revocar enlace, y ningún campo de URL ni Copiar enlace                                          |
      | falla por red o 503                                   | muestra un mensaje de fallo y Reintentar, que repite solo el GET, sin mostrar un estado inventado                                                       |

  @s32
  Scenario: Crear el enlace muestra la URL una sola vez con su aviso de secreto
    Given la vista /calendario en estado sin enlace y una respuesta 201 con url https://organizacion.apptolast.com/calendar/<token>.ics
    When activa Crear enlace de suscripción dos veces mediante doble clic o Enter repetido
    Then se envía exactamente un POST /api/v1/me/calendar-feed con cuerpo vacío y se anuncia Creando enlace… antes de 400 ms
    And muestra la url completa de la respuesta en un campo de solo lectura seleccionable con etiqueta accesible, el botón Copiar enlace y la fecha de creación
    And avisa de que el enlace no volverá a mostrarse y de que quien lo conozca verá títulos de tareas y objetivos
    And muestra Regenerar enlace y Revocar enlace y retira Crear enlace de suscripción
    And tras recargar la página el campo de URL y Copiar enlace ya no aparecen, solo la fecha de creación

  @s33
  Scenario Outline: Copiar enlace usa el portapapeles nativo sin fingir éxito
    Given la vista muestra la url recién creada y <portapapeles>
    When activa Copiar enlace
    Then <resultado>
    And no se envía ninguna petición al servidor
    Examples:
      | portapapeles                                         | resultado                                                                                              |
      | navigator.clipboard.writeText está disponible y resuelve | writeText recibe exactamente la url completa y se anuncia Enlace copiado                                |
      | navigator.clipboard.writeText rechaza                | no se anuncia Enlace copiado y se indica seleccionar el campo y copiar manualmente                       |
      | navigator.clipboard no existe                        | el botón indica seleccionar el campo y copiar manualmente, y el campo conserva la url seleccionable      |

  @s34
  Scenario Outline: Regenerar y revocar exigen una confirmación deliberada que explica la consecuencia
    Given la vista /calendario en estado con enlace activo
    When activa <accion>
    Then aparece una confirmación inline que dice que el enlace anterior dejará de funcionar en cualquier calendario donde esté pegado
    And la confirmación recibe el foco, ofrece <confirmar> y Cancelar, y todavía no se ha enviado ninguna petición
    Examples:
      | accion            | confirmar                 |
      | Regenerar enlace  | Confirmar regeneración    |
      | Revocar enlace    | Confirmar revocación      |

  @s35
  Scenario Outline: Decidir la confirmación envía una sola petición o ninguna y gestiona el foco
    Given la confirmación inline de <accion> está abierta
    When activa <decision>
    Then <peticion>
    And <resultado>
    And si el control que tenía el foco desaparece y la persona no movió el foco, el foco pasa al h1 Calendario ICS con foco visible; si lo movió, se respeta su destino
    Examples:
      | accion            | decision                | peticion                                                    | resultado                                                                                              |
      | Regenerar enlace  | Cancelar                | no se envía ninguna petición                                | la vista conserva el estado con enlace activo y su fecha de creación                                   |
      | Revocar enlace    | Cancelar                | no se envía ninguna petición                                | la vista conserva el estado con enlace activo y su fecha de creación                                   |
      | Regenerar enlace  | Confirmar regeneración  | se envía exactamente un POST /api/v1/me/calendar-feed        | muestra la nueva url una sola vez como en @s32 y la nueva fecha de creación                            |
      | Revocar enlace    | Confirmar revocación    | se envía exactamente un DELETE /api/v1/me/calendar-feed      | tras el 204 la vista pasa al estado sin enlace con Crear enlace de suscripción                         |
      | Regenerar enlace  | Confirmar regeneración con respuesta 503 o error de red | se envía exactamente un POST sin reintento automático | muestra el fallo y Reintentar, sin url y sin afirmar que el enlace anterior dejó de funcionar |

  @s36
  Scenario Outline: Descargar archivo .ics valida la respuesta antes de ofrecer el enlace nativo
    Given la vista /calendario y GET /api/v1/me/calendar.ics <respuesta>
    When activa Descargar archivo .ics
    Then <resultado>
    And no se descarga automáticamente ni se afirma que el navegador guardó el archivo en disco
    Examples:
      | respuesta                                                                                                              | resultado                                                                                                                              |
      | devuelve 200 text/calendar; charset=utf-8, Content-Length igual a los bytes recibidos, cuerpo que empieza por BEGIN:VCALENDAR y termina por END:VCALENDAR seguido de CRLF | anuncia Archivo preparado y ofrece un enlace nativo con download organizationweb-bloques.ics cuyos bytes son exactamente los recibidos |
      | devuelve 200 con Content-Type distinto de text/calendar                                                                | muestra un fallo y no crea Blob, enlace ni descarga                                                                                    |
      | devuelve 200 con menos bytes que Content-Length                                                                        | muestra un fallo y no crea Blob, enlace ni descarga                                                                                    |
      | devuelve 200 cuyo cuerpo no termina por END:VCALENDAR seguido de CRLF                                                  | muestra un fallo y no crea Blob, enlace ni descarga                                                                                    |
      | devuelve 413 CALENDAR_TOO_LARGE                                                                                        | explica el límite de 2000 eventos sin archivo parcial ni reintento automático                                                          |
      | falla por red o devuelve 503 STORAGE_UNAVAILABLE                                                                       | muestra un fallo y Reintentar manual, sin enlace                                                                                       |
      | devuelve 401                                                                                                           | aplica la retirada de acceso del flujo de sesión existente, sin enlace                                                                 |

  @s37
  Scenario Outline: Salir de la vista descarta la URL mostrada y las respuestas tardías
    Given la vista muestra la url recién creada o tiene una petición de creación, regeneración o descarga en vuelo
    When <salida>
    Then aborta la petición en vuelo y retira la url del DOM y de la memoria de la vista
    And cualquier respuesta tardía de esa petición se descarta sin mostrar url, estado ni archivo
    And un 401 tardío de esa petición no retira una sesión posterior
    And localStorage, sessionStorage y consola no contienen la url ni el token
    Examples:
      | salida                          |
      | navega a otra ruta              |
      | cierra sesión                   |
      | cambia la identidad de acceso   |

  @s38
  Scenario: La vista es operable con teclado, legible a 320 px con texto al 200 % y sin violaciones de axe en todos sus estados
    Given los estados cargando, sin enlace, con enlace activo, enlace recién creado, confirmación abierta, descarga preparada y fallo de la vista /calendario
    When se recorre la vista solo con teclado en anchos de 320, 768 y 1280 px CSS, con texto al 200 % y con zoom nativo al 200 % a 320 px
    Then todos los controles se alcanzan con Tab en orden lógico, tienen nombre accesible, foco visible y objetivo de al menos 44 × 44 px CSS
    And el campo de url es alcanzable, su contenido completo se selecciona con teclado y ningún ancho recorta la url ni provoca desbordamiento horizontal
    And los cambios de estado, la confirmación y los fallos se anuncian con live region sin exigir ratón
    And axe no reporta violaciones en ninguno de los estados y anchos
    And temas claro y oscuro, forced-colors y movimiento reducido conservan legibilidad y operación
    And la revisión de los treinta principios registra evidencias y límites humanos sin inferir cumplimiento universal desde axe
