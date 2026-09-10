@external_calendar
Feature: Suscribirse en solo lectura a un calendario iCalendar externo y verlo en Hoy
  Destilación de progress/proposal_external_calendar.md (feature 28), fuente normativa, bajo autorización global vigente.
  Una suscripción como máximo por propietario: etiqueta de 1 a 40 puntos de código tras strip Unicode y URL https de hasta 2048
  caracteres, sin userinfo, sin fragmento y con host de nombre, nunca IP literal. La aplicación descarga y analiza; nunca escribe al proveedor.
  La URL se cifra en reposo con AES-256-GCM, clave base64 de 32 bytes en APP_CONNECTOR_KEY, nonce aleatorio de 12 bytes por escritura
  y owner_id como dato adicional autenticado. Ninguna respuesta, log ni error contiene la URL completa: solo urlHost y urlTail (últimos 4 caracteres).
  Rutas privadas bajo /api/v1/me/external-calendar: GET, PUT, DELETE, POST /sync y GET /events?from&to. Sesión, OriginGuard, CSRF en
  escrituras, JSON estricto, Cache-Control no-store y errores problem+json. Sin APP_CONNECTOR_KEY las cinco responden 503 CONNECTORS_DISABLED.
  DTO subscription cerrado de quince campos: id, label, urlHost, urlTail, lastAttemptAt, lastSyncAt, lastStatus, lastError, snapshotZoneId,
  imported, skippedRecurring, skippedCancelled, skippedInvalid, truncated y updatedAt. Instantes en el formato UTC de feature 11.
  Descarga con redirecciones deshabilitadas, Accept text/calendar, aborto al superar 1 MiB; solo HTTP 200 con Content-Type text/*.
  El plazo de 5 s es del intercambio completo: conexión, cabeceras y lectura del cuerpo. Un proveedor que envía las cabeceras y luego
  gotea el cuerpo sin cerrarlo se corta al vencer ese plazo con FEED_UNREACHABLE; ninguna descarga puede retener un hilo más de 5 s.
  # Enmienda del 9 de septiembre de 2026, ratificada por el propietario. El plazo de 5 s
  # era solo del timeout de conexion; pasa a cubrir el INTERCAMBIO COMPLETO. Sin eso, un
  # proveedor que manda las cabeceras y luego gotea el cuerpo sin cerrarlo retiene un hilo
  # indefinidamente: es un agujero de recursos, no una lentitud. La ratificacion es la
  # respuesta "Ratifico las dos", cuya opcion describia esta enmienda como "la de la 28
  # amplia su contrato con el plazo de lectura del cuerpo del feed, que cierra un agujero
  # de recursos". Ver progress/ratificaciones.md, entrada R12.
  Códigos cerrados de fallo: FEED_REJECTED, FEED_UNREACHABLE, FEED_HTTP_ERROR, FEED_TOO_LARGE, FEED_UNSUPPORTED_TYPE, FEED_MALFORMED, SECRET_UNREADABLE.
  Un fallo de sincronización es 200 con lastStatus FAILED y la instantánea anterior intacta; nunca una lista parcial.
  Parser propio: solo VEVENT; UID y DTSTART obligatorios; Z es UTC; TZID debe pertenecer al catálogo de zonas; flotantes y VALUE=DATE se
  resuelven en la zona de instantánea (zona de disponibilidad resoluble o UTC), guardada en snapshotZoneId. RRULE, RDATE y RECURRENCE-ID
  se cuentan en skippedRecurring y no se expanden. UID repetido: el primero se conserva y los demás cuentan en skippedInvalid.
  Los contadores describen todo el feed; imported cuenta los VEVENT válidos y no omitidos aunque queden fuera de ventana.
  Ventana almacenada: intersección semiabierta con [syncAt - 24 h, syncAt + 336 h); orden startAt, uid; máximo 500 y truncated.
  Frescura: onlyIfStale true sincroniza solo si lastAttemptAt es null o anterior a now - 15 min; lo decide el backend.
  Concurrencia optimista por version: la sincronización perdedora se descarta con performed false; no hay planificador en segundo plano.
  Hoy no cambia: su DTO de quince campos es idéntico con y sin suscripción; la sección "Calendario externo" se compone en el frontend.
  Cada fila de Examples es un caso independiente; lo no mencionado permanece válido. Los relojes son controlados.

  Background:
    Given la sesión válida pertenece a "persona-a" y APP_CONNECTOR_KEY contiene una clave válida
    And la disponibilidad de persona-a usa la zona "Europe/Madrid"
    And el host "feed.example.test" resuelve solo a direcciones públicas y sirve un feed ICS válido por https
    And el reloj del servidor marca "2030-01-07T12:00:00Z"

  @s1
  Scenario: Leer ausencia de suscripción sin insertar
    Given persona-a no tiene suscripción
    When consulto GET "/api/v1/me/external-calendar"
    Then recibo 200 con exactamente configured false y subscription null y Cache-Control no-store
    And no se inserta ninguna fila de suscripción ni de eventos

  @s2
  Scenario: Crear la suscripción cifrando la URL y devolviendo solo host y cola
    Given persona-a no tiene suscripción
    When envío PUT con label "Trabajo" y url "https://feed.example.test/calendar/ical/abc123/basic.ics"
    Then recibo 200 con exactamente los quince campos de subscription y configured true
    And label es "Trabajo", urlHost es "feed.example.test", urlTail es ".ics" y snapshotZoneId es null
    And lastAttemptAt, lastSyncAt, lastStatus y lastError son null y los cuatro contadores son 0 y truncated es false
    And la fila guardada tiene version 0, url_ciphertext de 12 bytes de nonce más cifrado y no contiene la URL en claro
    And ni el cuerpo de la respuesta ni los logs contienen "abc123"

  @s3
  Scenario: Guardar dos veces la misma URL produce cifrados distintos
    Given persona-a guardó la URL "https://feed.example.test/a/basic.ics" con url_ciphertext C1
    When envío PUT con la misma URL y label "Otro"
    Then recibo 200 con version 1 y el url_ciphertext guardado C2 es distinto de C1
    And descifrar C1 y C2 con la clave vigente y owner_id "persona-a" devuelve la misma URL

  @s4
  Scenario Outline: Rechazar etiqueta o URL inválidas por campo sin escribir
    Given persona-a no tiene suscripción
    When envío PUT con label <label> y url <url>
    Then recibo 400 VALIDATION_ERROR en <campo> con <codigo> y no se inserta ninguna fila
    Examples:
      | label          | url                                                | campo | codigo            |
      | ausente        | "https://feed.example.test/a.ics"                  | label | REQUIRED          |
      | null           | "https://feed.example.test/a.ics"                  | label | REQUIRED          |
      | "   "          | "https://feed.example.test/a.ics"                  | label | REQUIRED          |
      | 7              | "https://feed.example.test/a.ics"                  | label | INVALID_TYPE      |
      | 41 letras "a"  | "https://feed.example.test/a.ics"                  | label | TOO_LONG          |
      | "Trabajo"      | ausente                                            | url   | REQUIRED          |
      | "Trabajo"      | true                                               | url   | INVALID_TYPE      |
      | "Trabajo"      | "feed.example.test/a.ics"                          | url   | INVALID_FORMAT    |
      | "Trabajo"      | "no es una url"                                    | url   | INVALID_FORMAT    |
      | "Trabajo"      | https con ruta de 2049 caracteres en total         | url   | TOO_LONG          |
      | "Trabajo"      | "http://feed.example.test/a.ics"                   | url   | INVALID_VALUE     |
      | "Trabajo"      | "https://user:pass@feed.example.test/a.ics"        | url   | INVALID_VALUE     |
      | "Trabajo"      | "https://feed.example.test/a.ics#frag"             | url   | INVALID_VALUE     |
      | "Trabajo"      | "https://127.0.0.1/a.ics"                          | url   | INVALID_VALUE     |
      | "Trabajo"      | "https://[::1]/a.ics"                              | url   | INVALID_VALUE     |
      | "Trabajo"      | "https://169.254.169.254/latest/meta-data"         | url   | INVALID_VALUE     |
      | "Trabajo"      | "https://10.0.0.5/a.ics"                           | url   | INVALID_VALUE     |
      | "Trabajo"      | "https://no-existe.invalid/a.ics"                  | url   | UNRESOLVABLE_HOST |
      | "Trabajo"      | "https://interno.example.test/a.ics" que resuelve a 10.0.0.5     | url   | BLOCKED_ADDRESS |
      | "Trabajo"      | "https://metadata.example.test/a.ics" que resuelve a 169.254.169.254 | url | BLOCKED_ADDRESS |
      | "Trabajo"      | "https://dual.example.test/a.ics" que resuelve a 93.184.216.34 y ::1 | url | BLOCKED_ADDRESS |

  @s5
  Scenario Outline: Admitir fronteras de etiqueta y URL
    Given persona-a no tiene suscripción
    When envío PUT con label <label> y url <url>
    Then recibo 200 y label guardada es <guardada> y urlTail es <cola>
    Examples:
      | label                          | url                                            | guardada                   | cola   |
      | 40 puntos de código con emoji  | "https://feed.example.test/a.ics"              | los mismos 40 puntos       | ".ics" |
      | "  Casa  "                     | "https://feed.example.test/a.ics"              | "Casa"                     | ".ics" |
      | "Trabajo"                      | https con ruta de 2048 caracteres en total     | "Trabajo"                  | los 4 últimos de esa URL |
      | "Trabajo"                      | "https://feed.example.test/x?k=v&t=WXYZ"       | "Trabajo"                  | "WXYZ" |

  @s6
  Scenario Outline: Reemplazar la suscripción borra la instantánea solo si cambia la URL
    Given persona-a tiene suscripción en version 3 con lastStatus OK, imported 12 y 12 eventos almacenados
    When envío PUT con <cambio>
    Then recibo 200 con version <version>, imported <imported> y quedan <eventos> eventos almacenados
    And lastSyncAt y lastAttemptAt son <estado>
    Examples:
      | cambio                              | version | imported | eventos | estado                   |
      | otra URL y la misma etiqueta        | 4       | 0        | 0       | null                     |
      | la misma URL y otra etiqueta        | 4       | 12       | 12      | los previos sin cambio   |
      | la misma URL y la misma etiqueta    | 3       | 12       | 12      | los previos sin cambio   |

  @s7
  Scenario Outline: Eliminar la suscripción responde 204 y arrastra la instantánea
    Given persona-a <estado>
    When envío DELETE "/api/v1/me/external-calendar"
    Then recibo 204 sin cuerpo
    And no queda fila de suscripción ni de eventos de persona-a y el GET posterior devuelve configured false
    Examples:
      | estado                                              |
      | tiene suscripción con 12 eventos almacenados        |
      | no tiene suscripción                                |

  @s8
  Scenario Outline: Deshabilitar los conectores cuando falta la clave
    Given APP_CONNECTOR_KEY no está definida y persona-a tiene una suscripción guardada
    When envío <ruta> con cuerpo <cuerpo>
    Then recibo 503 problem+json con código CONNECTORS_DISABLED
    And no se lee el cuerpo ni se consulta la base de datos
    Examples:
      | ruta                                                             | cuerpo                             |
      | GET /api/v1/me/external-calendar                                 | ninguno                            |
      | PUT /api/v1/me/external-calendar                                 | JSON malformado                    |
      | DELETE /api/v1/me/external-calendar                              | ninguno                            |
      | POST /api/v1/me/external-calendar/sync                           | JSON malformado                    |
      | GET /api/v1/me/external-calendar/events?from=x&to=y              | ninguno                            |

  @s9
  Scenario: No arrancar con clave mal formada sin revelar su valor
    Given APP_CONNECTOR_KEY contiene "no-es-base64-de-32-bytes"
    When arranca el backend
    Then el arranque falla con un mensaje que menciona APP_CONNECTOR_KEY
    And el mensaje y los logs no contienen "no-es-base64-de-32-bytes"

  @s10
  Scenario Outline: Aplicar seguridad HTTP común en las cinco rutas
    When envío <peticion>
    Then recibo <estado> con código <codigo> y no se escribe ninguna fila ni se descarga ningún feed
    Examples:
      | peticion                                                          | estado | codigo           |
      | GET sin sesión                                                    | 401    | el del filtro    |
      | PUT válido sin token CSRF                                         | 403    | el del filtro    |
      | POST /sync válido con Origin ajeno                                | 403    | el del filtro    |
      | GET /api/v1/me/external-calendar?x=1                              | 400    | VALIDATION_ERROR |
      | PUT con {"label":"a","url":"https://feed.example.test/a.ics","extra":1} | 400 | VALIDATION_ERROR |
      | PUT con cuerpo "{"                                                | 400    | MALFORMED_JSON   |
      | PUT con Content-Type text/plain                                   | 415    | el de negociación |
      | POST /sync con {"onlyIfStale":"no"}                               | 400    | VALIDATION_ERROR |
      | POST /sync con {}                                                 | 400    | VALIDATION_ERROR |

  @s11
  Scenario Outline: Rechazar en la sincronización un host que resuelve a una dirección prohibida
    Given persona-a tiene suscripción con host "feed.example.test" y 5 eventos almacenados con lastSyncAt "2030-01-07T09:00:00Z"
    And ahora "feed.example.test" resuelve a <direccion>
    When envío POST /sync con onlyIfStale false
    Then recibo 200 con performed true, lastStatus "FAILED" y lastError "FEED_REJECTED"
    And no se emite ninguna petición HTTP y lastAttemptAt es "2030-01-07T12:00:00Z"
    And lastSyncAt sigue siendo "2030-01-07T09:00:00Z" y los 5 eventos siguen almacenados
    Examples:
      | direccion                       |
      | 127.0.0.1                       |
      | 10.0.0.5                        |
      | 172.16.0.1                      |
      | 172.31.255.254                  |
      | 192.168.1.1                     |
      | 169.254.169.254                 |
      | 0.0.0.0                         |
      | 224.0.0.1                       |
      | ::1                             |
      | fe80::1                         |
      | fc00::1                         |
      | ::ffff:10.0.0.1                 |
      | 93.184.216.34 y 192.168.1.1     |
      | ninguna dirección               |

  @s12
  Scenario Outline: Registrar fallos de descarga sin perder la instantánea anterior
    Given persona-a tiene suscripción con 5 eventos almacenados, lastSyncAt "2030-01-07T09:00:00Z" e imported 5
    And el servidor del feed responde <respuesta>
    When envío POST /sync con onlyIfStale false
    Then recibo 200 con performed true, lastStatus "FAILED", lastError <codigo> y lastAttemptAt "2030-01-07T12:00:00Z"
    And lastSyncAt sigue siendo "2030-01-07T09:00:00Z", imported sigue siendo 5 y los 5 eventos siguen almacenados
    And el log registra el host, el código <codigo> y la duración, y no contiene la ruta de la URL
    Examples:
      | respuesta                                                                  | codigo                |
      | 302 con Location a otra URL válida                                         | FEED_HTTP_ERROR       |
      | 301 con Location a otra URL válida                                         | FEED_HTTP_ERROR       |
      | 404                                                                        | FEED_HTTP_ERROR       |
      | 500                                                                        | FEED_HTTP_ERROR       |
      | conexión rechazada                                                         | FEED_UNREACHABLE      |
      | presenta un certificado que no es válido para su nombre                    | FEED_UNREACHABLE      |
      # Fila del 9 de septiembre de 2026, ratificada por el propietario. Es literalmente
      # la prueba de TLS que pidio al resolver la contradiccion del reenlace DNS entre dos
      # carriles: eligio "Anclar, y probar el TLS", opcion descrita como "se conecta a la
      # direccion ya validada en las DOS features y se escribe la prueba de TLS/SNI que hoy
      # no existe". Si se ancla la direccion hay que comprobar que el certificado
      # corresponde al NOMBRE, o el anclaje no sirve de nada: sin esta fila, anclar
      # convertiria una defensa en un agujero. Ver progress/ratificaciones.md, entrada R13.
      | 200 tras 6 s sin enviar cabeceras                                          | FEED_UNREACHABLE      |
      | 200 text/calendar que envía las cabeceras y luego gotea el cuerpo sin cerrar | FEED_UNREACHABLE      |
      | 200 text/calendar con cuerpo de 1 MiB más 1 byte                           | FEED_TOO_LARGE        |
      | 200 application/json con un ICS válido                                     | FEED_UNSUPPORTED_TYPE |
      | 200 text/html con "<html>"                                                 | FEED_MALFORMED        |
      | 200 text/calendar con "BEGIN:VEVENT" sin "BEGIN:VCALENDAR"                 | FEED_MALFORMED        |

  @s13
  Scenario Outline: Descargar con Accept text/calendar, sin seguir redirecciones ni leer más de 1 MiB
    Given persona-a tiene suscripción hacia "https://feed.example.test/cal.ics?tok=WXYZ" y el servidor responde <respuesta>
    When envío POST /sync con onlyIfStale false
    Then <efecto>
    And el servidor recibe exactamente una petición GET a "/cal.ics?tok=WXYZ" con Accept "text/calendar", sin cookie de sesión ni Authorization
    Examples:
      | respuesta                                                     | efecto                                                                 |
      | 302 hacia "https://destino.example.test/otro.ics"             | "destino.example.test" no recibe ninguna petición                      |
      | 200 text/calendar que emite 4 MiB                             | la lectura se aborta antes de recibir 2 MiB y lastError es FEED_TOO_LARGE |
      | 200 text/calendar de exactamente 1 MiB con ICS válido         | lastStatus es "OK"                                                     |
      | 200 "text/calendar; charset=utf-8" con ICS válido             | lastStatus es "OK"                                                     |
      | 200 text/plain con ICS válido                                 | lastStatus es "OK"                                                     |

  @s14
  Scenario: Importar un evento UTC con instantes exactos
    Given el feed contiene un VEVENT con UID "u1@example", DTSTART "20300108T090000Z", DTEND "20300108T100000Z" y SUMMARY "Reunión"
    When envío POST /sync con onlyIfStale false
    Then recibo 200 con lastStatus "OK", lastSyncAt "2030-01-07T12:00:00Z", imported 1 y los tres skipped en 0
    And el evento almacenado tiene uid "u1@example", summary "Reunión", startAt "2030-01-08T09:00:00Z", endAt "2030-01-08T10:00:00Z" y allDay false
    And el reloj de sincronización se captura una sola vez: lastAttemptAt es igual a lastSyncAt

  @s15
  Scenario Outline: Convertir TZID Europe/Madrid con el desfase real de cada fecha DST
    Given el feed contiene un VEVENT con DTSTART;TZID=Europe/Madrid <inicio> y DTEND;TZID=Europe/Madrid <fin>
    And el reloj del servidor marca 12:00Z del mismo día que <inicio>
    When envío POST /sync con onlyIfStale false
    Then el evento almacenado tiene startAt <startAt> y endAt <endAt>
    Examples:
      | inicio          | fin             | startAt              | endAt                |
      | 20260115T100000 | 20260115T110000 | 2026-01-15T09:00:00Z | 2026-01-15T10:00:00Z |
      | 20260715T100000 | 20260715T110000 | 2026-07-15T08:00:00Z | 2026-07-15T09:00:00Z |
      | 20260329T013000 | 20260329T033000 | 2026-03-29T00:30:00Z | 2026-03-29T01:30:00Z |
      | 20260329T100000 | 20260329T110000 | 2026-03-29T08:00:00Z | 2026-03-29T09:00:00Z |
      | 20261025T013000 | 20261025T040000 | 2026-10-24T23:30:00Z | 2026-10-25T03:00:00Z |
      | 20261025T100000 | 20261025T110000 | 2026-10-25T09:00:00Z | 2026-10-25T10:00:00Z |

  @s16
  Scenario Outline: Resolver todo el día y flotantes en la zona de instantánea
    Given persona-a <disponibilidad>
    And el feed contiene un VEVENT con <dtstart> y <dtend>
    And el reloj del servidor marca 12:00Z del día de inicio
    When envío POST /sync con onlyIfStale false
    Then snapshotZoneId es <zona> y el evento almacenado tiene startAt <startAt>, endAt <endAt> y allDay <allDay>
    Examples:
      | disponibilidad                                | dtstart                        | dtend                        | zona          | startAt              | endAt                | allDay |
      | usa Europe/Madrid                             | DTSTART;VALUE=DATE:20260329    | ausente                      | Europe/Madrid | 2026-03-28T23:00:00Z | 2026-03-29T22:00:00Z | true   |
      | usa Europe/Madrid                             | DTSTART;VALUE=DATE:20260105    | DTEND;VALUE=DATE:20260107    | Europe/Madrid | 2026-01-04T23:00:00Z | 2026-01-06T23:00:00Z | true   |
      | no tiene disponibilidad                       | DTSTART;VALUE=DATE:20260105    | ausente                      | UTC           | 2026-01-05T00:00:00Z | 2026-01-06T00:00:00Z | true   |
      | usa la zona no resoluble Legacy/Retired       | DTSTART;VALUE=DATE:20260105    | ausente                      | UTC           | 2026-01-05T00:00:00Z | 2026-01-06T00:00:00Z | true   |
      | usa Europe/Madrid                             | DTSTART:20260115T100000        | DTEND:20260115T110000        | Europe/Madrid | 2026-01-15T09:00:00Z | 2026-01-15T10:00:00Z | false  |
      | no tiene disponibilidad                       | DTSTART:20260115T100000        | DTEND:20260115T110000        | UTC           | 2026-01-15T10:00:00Z | 2026-01-15T11:00:00Z | false  |

  @s17
  Scenario Outline: Calcular el fin con DURATION y rechazar fines no posteriores
    Given el feed contiene un VEVENT con UID "d1", DTSTART "20300108T090000Z" y <resto>
    When envío POST /sync con onlyIfStale false
    Then imported es <imported>, skippedInvalid es <invalid> y el evento almacenado, si existe, tiene endAt <endAt>
    Examples:
      | resto                                  | imported | invalid | endAt                |
      | DURATION "PT1H30M"                     | 1        | 0       | 2030-01-08T10:30:00Z |
      | DURATION "P1D"                         | 1        | 0       | 2030-01-09T09:00:00Z |
      | ni DTEND ni DURATION                   | 0        | 1       | ninguno              |
      | DTEND "20300108T090000Z"               | 0        | 1       | ninguno              |
      | DTEND "20300108T085900Z"               | 0        | 1       | ninguno              |
      | DURATION "PT0S"                        | 0        | 1       | ninguno              |

  @s18
  Scenario Outline: Omitir y contar eventos cancelados y recurrentes sin expandirlos
    Given el feed contiene 2 VEVENT válidos y uno más con UID "x1", DTSTART dentro de la ventana y <propiedad>
    When envío POST /sync con onlyIfStale false
    Then imported es 2, <contador> es 1, los otros dos skipped son 0 y no se almacena ningún evento con uid "x1"
    And el total de eventos almacenados es 2
    Examples:
      | propiedad                                  | contador          |
      | STATUS "CANCELLED"                         | skippedCancelled  |
      | RRULE "FREQ=WEEKLY;COUNT=10"               | skippedRecurring  |
      | RDATE "20300109T090000Z"                   | skippedRecurring  |
      | RECURRENCE-ID "20300108T090000Z"           | skippedRecurring  |
      | STATUS "CANCELLED" y RRULE "FREQ=DAILY"    | skippedRecurring  |

  @s19
  Scenario Outline: Desplegar líneas y desescapar SUMMARY
    Given el feed contiene un VEVENT válido cuyo SUMMARY llega como <crudo>
    When envío POST /sync con onlyIfStale false
    Then el summary almacenado es <summary>
    Examples:
      | crudo                                                                  | summary                            |
      | "SUMMARY:Reunión con\, el equipo\; sala 2\\n" en una sola línea        | "Reunión con, el equipo; sala 2\n"  |
      | "SUMMARY:Reu" CRLF espacio "nión larga" (línea plegada)                | "Reunión larga"                     |
      | "SUMMARY:Reu" LF tabulador "nión" (plegado con LF y tabulador)          | "Reunión"                           |
      | "SUMMARY:  con espacios  "                                             | "con espacios"                      |
      | SUMMARY ausente                                                        | ""                                  |
      | SUMMARY de 501 puntos de código                                        | sus primeros 500 puntos de código   |
      | "SUMMARY:a\\\\b" (barra escapada)                                      | "a\\b"                              |

  @s20
  Scenario: Conservar el primer UID repetido y contar los demás
    Given el feed contiene en este orden tres VEVENT válidos con UID "dup", SUMMARY "Primero", "Segundo" y "Tercero"
    When envío POST /sync con onlyIfStale false
    Then imported es 1, skippedInvalid es 2 y el único evento con uid "dup" tiene summary "Primero"

  @s21
  Scenario Outline: Contar como inválidos los eventos que no se pueden interpretar
    Given el feed contiene 1 VEVENT válido y otro con <defecto>
    When envío POST /sync con onlyIfStale false
    Then lastStatus es "OK", imported es 1 y skippedInvalid es 1
    Examples:
      | defecto                                                     |
      | UID ausente                                                 |
      | DTSTART ausente                                             |
      | DTSTART "20260230T100000Z" (fecha imposible)                |
      | DTSTART "20260115T256000Z" (hora imposible)                 |
      | DTSTART;TZID=Romance Standard Time:20260115T100000          |
      | DTSTART;TZID=Marte/Base:20260115T100000                     |
      | DTSTART "20260115" sin VALUE=DATE                           |

  @s22
  Scenario: Ignorar componentes distintos de VEVENT
    Given el feed contiene un VTIMEZONE, un VTODO con DTSTART en ventana, un VEVENT válido con un VALARM anidado y un componente X-DESCONOCIDO
    When envío POST /sync con onlyIfStale false
    Then lastStatus es "OK", imported es 1, skippedInvalid es 0 y el total de eventos almacenados es 1

  @s23
  Scenario Outline: Almacenar solo los eventos que intersectan la ventana semiabierta
    Given el feed contiene un único VEVENT válido de <inicio> a <fin>
    When envío POST /sync con onlyIfStale false
    Then el total de eventos almacenados es <almacenados> e imported es 1
    Examples:
      | inicio                | fin                   | almacenados |
      | 2030-01-06T11:00:00Z  | 2030-01-06T12:00:00Z  | 0           |
      | 2030-01-06T11:30:00Z  | 2030-01-06T12:30:00Z  | 1           |
      | 2030-01-06T12:00:00Z  | 2030-01-06T13:00:00Z  | 1           |
      | 2030-01-21T11:30:00Z  | 2030-01-21T12:30:00Z  | 1           |
      | 2030-01-21T12:00:00Z  | 2030-01-21T13:00:00Z  | 0           |
      | 2029-12-01T00:00:00Z  | 2030-02-01T00:00:00Z  | 1           |

  @s24
  Scenario: Truncar a 500 conservando los primeros por startAt y uid
    Given el feed contiene 600 VEVENT válidos dentro de la ventana con inicios crecientes y 50 más fuera de ventana
    And dos de los 600 comparten startAt con UID "b" y "a"
    When envío POST /sync con onlyIfStale false
    Then imported es 650, truncated es true y el total de eventos almacenados es 500
    And los almacenados son los 500 primeros por startAt y uid ascendentes y "a" precede a "b"
    And con 500 eventos en ventana truncated es false y se almacenan los 500

  @s25
  Scenario: Reemplazar la instantánea de forma atómica
    Given persona-a tiene almacenados los eventos "u1" y "u2" y el feed ahora contiene "u2" con otro summary y "u3"
    When envío POST /sync con onlyIfStale false
    Then los eventos almacenados son exactamente "u2" con el summary nuevo y "u3", y no queda rastro de "u1"
    And una lectura concurrente durante la sincronización ve la lista anterior completa o la nueva completa, nunca una vacía ni mezclada
    And ninguna otra tabla, outbox ni historial cambia

  @s26
  Scenario Outline: Descartar la sincronización perdedora por versión sin mezclar instantáneas
    Given persona-a tiene suscripción en version 2 y una sincronización S1 ya leyó version 2 y está descargando
    When <competidor> se confirma antes de que S1 termine
    Then S1 responde 200 con performed false y el subscription vigente tras <competidor>
    And los eventos almacenados son exactamente los de <competidor> y version es <version>
    And solo se realizó una descarga por cada sincronización, sin reintento automático
    Examples:
      | competidor                                   | version |
      | otra sincronización S2 con éxito             | 3       |
      | un PUT que cambia la URL                     | 3       |

  @s27
  Scenario Outline: Decidir la frescura en el backend a partir de lastAttemptAt
    Given persona-a tiene suscripción con lastAttemptAt <attempt>, lastSyncAt <sync> y lastStatus <status>
    When envío POST /sync con onlyIfStale <onlyIfStale>
    Then recibo 200 con performed <performed> y el servidor del feed recibió <descargas> petición
    Examples:
      | attempt               | sync                  | status | onlyIfStale | performed | descargas |
      | null                  | null                  | null   | true        | true      | 1         |
      | 2030-01-07T11:44:59Z  | 2030-01-07T11:44:59Z  | OK     | true        | true      | 1         |
      | 2030-01-07T11:45:00Z  | 2030-01-07T11:45:00Z  | OK     | true        | false     | 0         |
      | 2030-01-07T11:59:00Z  | 2030-01-07T11:59:00Z  | OK     | true        | false     | 0         |
      | 2030-01-07T11:50:00Z  | 2030-01-07T10:00:00Z  | FAILED | true        | false     | 0         |
      | 2030-01-07T11:59:00Z  | 2030-01-07T11:59:00Z  | OK     | false       | true      | 1         |

  @s28
  Scenario: Rechazar la sincronización sin suscripción
    Given persona-a no tiene suscripción
    When envío POST /sync con onlyIfStale false
    Then recibo 404 problem+json con código EXTERNAL_CALENDAR_NOT_CONFIGURED y no se descarga nada

  @s29
  Scenario Outline: Volver a pedir la dirección cuando la clave ya no descifra
    Given persona-a guardó su URL con la clave K1 y 5 eventos almacenados
    And el backend arranca con la clave K2
    When envío <accion>
    Then <resultado>
    Examples:
      | accion                                         | resultado                                                                                      |
      | POST /sync con onlyIfStale false               | 200 con performed true, lastStatus "FAILED", lastError "SECRET_UNREADABLE", los 5 eventos intactos y ninguna petición HTTP |
      | GET /api/v1/me/external-calendar               | 200 con urlHost y urlTail de la suscripción, sin intentar descifrar                            |
      | PUT con una URL válida                         | 200 con version incrementada y el POST /sync siguiente tiene lastStatus "OK"                   |

  @s30
  Scenario: Conservar suscripción e instantánea tras reiniciar el backend
    Given persona-a tiene suscripción con 5 eventos y lastSyncAt "2030-01-07T09:00:00Z"
    And una sincronización confirmó en base de datos lastSyncAt "2030-01-07T11:00:00Z" pero su respuesta se perdió
    When el backend se reinicia y consulto GET "/api/v1/me/external-calendar"
    Then recibo 200 con la misma id, lastSyncAt "2030-01-07T11:00:00Z" y GET /events devuelve los eventos de esa sincronización
    And no se dispara ninguna sincronización al arrancar

  @s31
  Scenario: Leer eventos de un rango en UTC con esquema cerrado y orden estable
    Given persona-a tiene almacenados estos eventos
      | nombre | uid | startAt              | endAt                |
      | A      | b   | 2030-01-07T08:00:00Z | 2030-01-07T09:00:00Z |
      | E      | a   | 2030-01-07T08:00:00Z | 2030-01-07T09:00:00Z |
      | B      | c   | 2030-01-07T23:30:00Z | 2030-01-08T00:30:00Z |
      | C      | d   | 2030-01-08T00:00:00Z | 2030-01-08T01:00:00Z |
      | D      | e   | 2030-01-06T23:00:00Z | 2030-01-07T00:00:00Z |
    When consulto GET "/api/v1/me/external-calendar/events?from=2030-01-07T00:00:00Z&to=2030-01-08T00:00:00Z"
    Then recibo 200 con exactamente configured true, lastSyncAt, lastStatus e items, y Cache-Control no-store
    And items contiene en este orden E, A y B, cada uno con exactamente uid, summary, startAt, endAt y allDay, sin C ni D

  @s32
  Scenario Outline: Validar el rango de eventos
    Given persona-a tiene suscripción
    When consulto GET /events con <consulta>
    Then recibo <estado> <detalle>
    Examples:
      | consulta                                                        | estado | detalle                                        |
      | from=2030-01-07T00:00:00Z&to=2030-01-23T00:00:00Z               | 200    | con items                                      |
      | from=2030-01-07T00:00:00Z&to=2030-01-23T00:00:01Z               | 400    | VALIDATION_ERROR en to con OUT_OF_RANGE        |
      | from=2030-01-07T00:00:00Z&to=2030-01-07T00:00:00Z               | 400    | VALIDATION_ERROR en to con OUT_OF_RANGE        |
      | from=2030-01-08T00:00:00Z&to=2030-01-07T00:00:00Z               | 400    | VALIDATION_ERROR en to con OUT_OF_RANGE        |
      | to=2030-01-08T00:00:00Z                                         | 400    | VALIDATION_ERROR en from con REQUIRED          |
      | from=2030-01-07T00:00:00Z                                       | 400    | VALIDATION_ERROR en to con REQUIRED            |
      | from=2030-01-07&to=2030-01-08T00:00:00Z                         | 400    | VALIDATION_ERROR en from con INVALID_FORMAT    |
      | from=2030-01-07T00:00:00%2B01:00&to=2030-01-08T00:00:00Z        | 400    | VALIDATION_ERROR en from con INVALID_FORMAT    |
      | from=2030-01-07T00:00:00Z&to=2030-01-08T00:00:00Z&zone=UTC      | 400    | VALIDATION_ERROR en zone                       |

  @s33
  Scenario Outline: Aislar suscripción y eventos por propietario
    Given persona-a tiene suscripción con 5 eventos y persona-b <estadoB>
    When persona-b <accion>
    Then <resultado>
    And persona-a conserva su suscripción y sus 5 eventos
    Examples:
      | estadoB                     | accion                                                  | resultado                                                    |
      | no tiene suscripción        | consulta GET /api/v1/me/external-calendar               | recibe configured false y subscription null                  |
      | no tiene suscripción        | consulta GET /events del día de los 5 eventos           | recibe configured false, lastSyncAt null, lastStatus null e items vacío |
      | no tiene suscripción        | envía DELETE                                            | recibe 204                                                   |
      | tiene suscripción con 2 eventos | consulta GET /events del día de los 5 eventos       | recibe solo sus 2 eventos                                    |
      | tiene suscripción           | envía POST /sync con onlyIfStale false                  | solo cambia el estado de persona-b                           |

  @s34
  Scenario Outline: Mantener idéntica la respuesta de Hoy con y sin suscripción
    Given un bloque propio de "2030-01-07T10:00:00Z" a "2030-01-07T11:00:00Z" y presupuesto de 120 minutos
    And la respuesta R0 de GET /api/v1/today sin suscripción
    And persona-a suscribe un feed con <evento> ya sincronizado
    When consulto GET /api/v1/today y <planificacion>
    Then el cuerpo de Hoy es byte a byte igual a R0: mismos quince campos, plannedSeconds 3600, remainingSeconds 3600, currentBlockId, nextBlockId y closingAt
    And la planificación responde 201 sin error de solape con el evento externo
    Examples:
      | evento                                                            | planificacion                                                    |
      | un evento de "2030-01-07T10:00:00Z" a "2030-01-07T11:00:00Z"      | planifico un bloque de "2030-01-07T14:00:00Z" a "2030-01-07T15:00:00Z" |
      | un evento de "2030-01-07T14:00:00Z" a "2030-01-07T15:00:00Z"      | planifico un bloque de "2030-01-07T14:00:00Z" a "2030-01-07T15:00:00Z" |
      | un evento de todo el día del 7 de enero                           | planifico un bloque de "2030-01-07T16:00:00Z" a "2030-01-07T17:00:00Z" |

  @s35
  Scenario Outline: Componer la sección Calendario externo de Hoy en hora local del propietario
    Given Hoy responde zoneId "Europe/Madrid", dayStartAt "2030-01-06T23:00:00Z" y dayEndAt "2030-01-07T23:00:00Z"
    And la suscripción tiene <estado>
    When se carga Hoy
    Then tras el snapshot de Hoy se envían en este orden POST /sync con onlyIfStale true y GET /events con from "2030-01-06T23:00:00Z" y to "2030-01-07T23:00:00Z"
    And la sección Calendario externo muestra <seccion>
    And la agenda de bloques y sus resúmenes se muestran completos antes de que respondan esas dos llamadas
    Examples:
      | estado                                                                                  | seccion                                                                                     |
      | lastStatus OK, lastSyncAt "2030-01-07T11:00:00Z" y un evento de 08:00Z a 09:00Z         | "Reunión 09:00–10:00" y "Según sincronización de 12:00" sin botones de edición             |
      | lastStatus OK y un evento allDay del 7 de enero                                         | "Todo el día" sin horas                                                                     |
      | lastStatus FAILED, lastSyncAt "2030-01-07T09:00:00Z" y un evento previo                 | el evento previo y "Sincronización fallida, se muestran datos de 10:00"                    |
      | lastStatus OK e items vacío                                                             | la sección con el texto de ausencia de eventos y "Según sincronización de"                 |

  @s36
  Scenario Outline: No degradar Hoy cuando la sección externa falla, no existe o queda obsoleta
    Given Hoy carga con agenda de bloques válida
    When <situacion>
    Then <resultado>
    And la agenda de bloques, presupuesto, actual y próximo permanecen visibles y sin cambios
    Examples:
      | situacion                                                                          | resultado                                                                                       |
      | GET /api/v1/me/external-calendar/events responde configured false                  | la sección Calendario externo no se muestra y no aparece ningún aviso                            |
      | POST /sync responde 503 y GET /events responde con items                           | la sección muestra los items con el aviso de sincronización pendiente                            |
      | GET /events falla por red                                                          | la sección muestra un aviso con enlace a /calendario-externo, sin error global de Hoy            |
      | GET /events responde 200 con un item sin allDay                                    | la sección muestra el aviso de lectura inválida y no pinta items                                 |
      | Hoy inicia una actualización nueva antes de que respondan las dos llamadas          | las llamadas anteriores se cancelan y su respuesta tardía no repinta la sección                   |
      | el usuario sale de Hoy antes de que respondan                                      | las llamadas se cancelan y no se actualiza estado de una vista desmontada                        |

  @s37
  Scenario Outline: Abrir /calendario-externo en cada estado sin revelar la URL
    Given persona-a <estado>
    When llego a /calendario-externo mediante <entrada>
    Then la entrada de navegación "Calendario externo" tiene aria-current "page" y veo <vista>
    And ningún nodo del DOM contiene la URL completa guardada
    Examples:
      | estado                                                                         | entrada                | vista                                                                                             |
      | no tiene suscripción                                                           | enlace de navegación   | el formulario con campos "Etiqueta" y "Dirección secreta iCal" de type url, ayuda de Google Calendar y Guardar |
      | tiene suscripción sin sincronizar                                              | URL directa autenticada | "calendar.google.com … .ics", "Última sincronización correcta" sin fecha y Sincronizar ahora      |
      | tiene suscripción con OK, imported 12, recurring 3, cancelled 1, invalid 0     | retorno tras iniciar sesión | "12 eventos, 3 recurrentes no incluidos, 1 cancelado, 0 inválidos" y la lista de eventos en hora local |
      | tiene suscripción con truncated true                                            | enlace de navegación   | el aviso de que solo se conservan 500 eventos                                                      |
      | tiene suscripción con FAILED y lastError FEED_HTTP_ERROR                        | enlace de navegación   | "Último intento" con fecha y el mensaje accionable de FEED_HTTP_ERROR                             |
      | tiene suscripción con FAILED y lastError SECRET_UNREADABLE                      | enlace de navegación   | el mensaje que pide volver a pegar la dirección y el campo de dirección vacío y habilitado         |

  @s38
  Scenario Outline: Guardar y sincronizar con feedback inmediato y una sola petición
    Given el formulario tiene una etiqueta válida y una dirección válida pegada
    When activo <accion> y la respuesta es <respuesta>
    Then aparece <feedback> antes de 400 ms, se envía exactamente una petición y los controles quedan bloqueados hasta la respuesta
    And al resolverse <resultado>
    Examples:
      | accion            | respuesta                              | feedback                | resultado                                                                                     |
      | Guardar           | 200                                    | "Guardando"             | el campo de dirección queda vacío, se muestra host y cola y se anuncia "Guardado" sin robar foco |
      | Guardar           | 400 url BLOCKED_ADDRESS                | "Guardando"             | el borrador se conserva, el error se asocia al campo con foco visible y se puede corregir      |
      | Guardar           | 503 CONNECTORS_DISABLED                | "Guardando"             | se muestra estado incierto con reintento manual y el borrador se conserva                     |
      | Guardar           | fallo de red                           | "Guardando"             | se muestra estado incierto con reintento manual y el borrador se conserva                     |
      | Guardar           | 401                                    | "Guardando"             | se retiran datos privados y la ruta vuelve a /                                                |
      | Sincronizar ahora | 200 performed true lastStatus OK       | "Sincronizando"         | se actualizan contadores, "Última sincronización correcta" y la lista de eventos              |
      | Sincronizar ahora | 200 performed true lastStatus FAILED   | "Sincronizando"         | se muestra el mensaje del código y la lista anterior permanece                                |
      | Sincronizar ahora | 404 EXTERNAL_CALENDAR_NOT_CONFIGURED   | "Sincronizando"         | se muestra el formulario vacío de alta                                                        |

  @s39
  Scenario Outline: Eliminar con confirmación explícita y salir sin fugas
    Given persona-a tiene suscripción con eventos listados en /calendario-externo
    When <accion>
    Then <resultado>
    Examples:
      | accion                                                | resultado                                                                                              |
      | activo Eliminar suscripción y cancelo la confirmación | no se envía DELETE y la suscripción sigue visible                                                       |
      | activo Eliminar suscripción y confirmo                | se envía un DELETE, desaparecen host, cola, estado y lista, y se muestra el formulario vacío             |
      | navego a /hoy con una petición en curso               | la petición se cancela y su respuesta tardía no modifica la vista destino                                |
      | cierro sesión                                         | desaparecen host, cola, contadores y lista inmediatamente, la ruta se reinicia a / y no se conserva borrador |

  @s40
  Scenario: Mantener la pantalla accesible, operable con teclado y responsive
    Given /calendario-externo en estados vacío, con suscripción, con error, con lista larga de resúmenes Unicode y guardando
    When se revisa según la matriz de docs/ux-requirements.md a 320, 768, 1280 y 2560 px CSS, zoom nativo 200 % y texto ampliado 200 %
    Then no hay solapes, recortes ni scroll horizontal accidental en ningún ancho ni a ambos lados de cada breakpoint
    And el recorrido con teclado sigue el orden Etiqueta, Dirección secreta iCal, Guardar, Sincronizar ahora, Eliminar suscripción con foco visible en cada control
    And los controles miden al menos 44 por 44 px CSS y los estados guardando, guardado y fallo se anuncian sin mover el foco elegido
    And axe no encuentra violaciones en las reglas ejecutadas y se registra que axe no certifica lector de pantalla real
    And el feedback visual se mide con objetivo menor de 400 ms sin prometer respuesta de red en ese plazo
