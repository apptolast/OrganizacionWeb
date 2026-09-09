# Destilación de external_calendar — borrador revisable

Fuente: `progress/proposal_external_calendar.md` (feature 28), redactada por el `spec_partner` el 8 de septiembre de 2026 bajo autorización global. Rol independiente `gherkin_author`: sin producción, tests, ejecución de suites ni cambios en `project-spec.md` o `feature_list.json`. Ponytail full / Caveman lite. Este documento no aprueba TDD.

Archivo: `features/external_calendar.feature`. **40 escenarios estables @s1–@s40, 27 Scenario Outline y 185 casos expandidos** (una fila de Examples = un caso). Sin `@approved`. Validación estructural propia por script: numeración contigua, un `When` por escenario, ancho de tablas consistente y placeholders declarados en cada cabecera; no es un parser Gherkin ni una prueba de producto.

## Mapa de cobertura

| Sección de la propuesta / comportamiento | Escenarios |
| --- | --- |
| Lectura sin suscripción, DTO cerrado de quince campos, no-store, leer no inserta | @s1, @s2 |
| Cifrado AES-GCM: nonce por escritura, ida y vuelta, ciphertext sin URL en claro, host y cola, ausencia en cuerpo y logs | @s2, @s3, @s12 |
| Validación de `label` y `url` por campo, fronteras 40/2048, strip, userinfo, fragmento, IP literal, esquema, DNS | @s4, @s5 |
| Una suscripción por propietario: reemplazo, borrado de instantánea solo al cambiar URL, `version` | @s6 |
| DELETE 204 idempotente con cascada | @s7 |
| `CONNECTORS_DISABLED` en las cinco rutas antes de cuerpo y BD; clave mal formada impide arrancar sin revelar valor | @s8, @s9 |
| Seguridad HTTP común: 401, CSRF, Origin, query no admitida, campos extra, MALFORMED_JSON, 415, `onlyIfStale` obligatorio booleano | @s10 |
| Guardia SSRF en sincronización por familia de dirección, sin petición HTTP, instantánea intacta | @s11 |
| Fallos de descarga con códigos cerrados, `lastAttemptAt` sin tocar `lastSyncAt`, log sin URL | @s12 |
| Redirección no seguida, aborto a 1 MiB, tipos aceptados, Accept, sin credenciales de la aplicación | @s13 |
| Parser: UTC `Z`, TZID con DST exacto, todo el día y flotantes en zona de instantánea, `DURATION`, fin no posterior | @s14–@s17 |
| Parser: cancelados, recurrentes no expandidos, plegado, escapes, SUMMARY 500, UID repetido, inválidos, componentes ajenos | @s18–@s22 |
| Ventana [syncAt − 24 h, syncAt + 336 h), orden y truncamiento a 500 con contadores de todo el feed | @s23, @s24 |
| Instantánea atómica y concurrencia optimista por `version` con `performed=false` | @s25, @s26 |
| Frescura de 15 min sobre `lastAttemptAt` decidida por el backend con reloj fijo | @s27 |
| 404 sin suscripción, `SECRET_UNREADABLE` y reparación con PUT | @s28, @s29 |
| Recuperación tras reinicio y respuesta perdida tras commit | @s30 |
| GET `/events`: esquema cerrado, filtro semiabierto, orden, validación de rango, aislamiento por propietario | @s31–@s33 |
| Hoy: DTO idéntico con y sin suscripción, sin efecto en presupuesto ni solapes | @s34 |
| Hoy: sección compuesta en el frontend, hora local, todo el día, FAILED con datos previos, sin degradar la agenda, cancelación por generación | @s35, @s36 |
| UI `/calendario-externo`: estados, formulario, host y cola, contadores en frases, truncated, mensajes por código | @s37 |
| UI: guardar y sincronizar con feedback < 400 ms, una petición, bloqueo, errores 400/503/red/401 | @s38 |
| UI: eliminar con confirmación, cancelación al navegar, logout | @s39 |
| UI: teclado, foco, anuncios, 44 × 44, 320/768/1280/2560, zoom y texto 200 %, axe con límites explícitos | @s40 |

## Huecos detectados en la propuesta y cómo se resolvieron

1. **Guardia SSRF con dos momentos y dos códigos.** La propuesta describe una única guardia pero el PUT responde por campo (`INVALID_VALUE` para IP literal, `BLOCKED_ADDRESS` para nombre que resuelve a privada) y la sincronización responde `FEED_REJECTED`. La tabla pedida de URLs rechazadas se reparte en @s4 (momento PUT, por código) y @s11 (momento sync, por familia de dirección, incluida la resolución mixta pública + privada y el host sin direcciones).
2. **Significado de `imported` con ventana y truncamiento.** «Los contadores describen todo el feed» no fijaba si `imported` cuenta lo almacenado o lo válido. Se fijó en la cabecera y en @s24: `imported` cuenta los VEVENT válidos y no omitidos aunque queden fuera de ventana o se trunquen; el número almacenado se verifica aparte. Si el humano prefiere `imported` = almacenados, cambiar @s24 y @s23.
3. **Falta de `DTEND`/`DURATION` en eventos con hora.** La propuesta lo lista como inválido en una frase; @s17 fija también `DURATION` cero y `DTEND` igual o anterior al inicio como `skippedInvalid`, y @s16 fija `DTEND;VALUE=DATE` igual al inicio como inválido por la restricción `end_at > start_at`.
4. **Frescura en el instante exacto de 15 minutos.** «Anterior a now − 15 min» excluye la igualdad; @s27 lo fija con reloj controlado (11:44:59 sincroniza, 11:45:00 no) y añade el caso FAILED reciente con `lastSyncAt` antiguo para que el umbral no pueda medirse sobre `lastSyncAt` sin fallar.
5. **Sección de Hoy cuando `/sync` falla pero `/events` responde.** La propuesta no decía si la sección se pinta con la instantánea existente. @s36 fija que se muestran los items con aviso; el DTO de Hoy nunca espera a esas llamadas (@s35).
6. **`SECRET_UNREADABLE` en GET.** La propuesta solo lo describe en la sincronización; @s29 fija que el GET no intenta descifrar y sigue devolviendo host y cola, para que la pantalla pueda pedir la dirección de nuevo.

## Dudas para el humano

- **Horas locales inexistentes o ambiguas en TZID** (02:30 del 29 de marzo de 2026 y 02:30 del 25 de octubre de 2026 en Europe/Madrid). La propuesta no las fija; @s15 no las incluye. Si se quiere contrato, la opción coherente con `java.time` es desplazar la hora inexistente hacia delante y elegir el primer desfase en la ambigua.
- **Concurrencia**: la propuesta resuelve dos sincronizaciones simultáneas con `performed=false` para la perdedora, no con 409. @s26 sigue la propuesta.
- **Texto exacto de la sección de Hoy** («Según sincronización de 12:00» en hora local) y del texto de ausencia de eventos en @s35: los literales quedan como referencia; el `tdd_craftsman` puede ajustar redacción sin cambiar la semántica si el humano lo aprueba.
- La pregunta abierta de la propuesta sobre compartir `APP_CONNECTOR_KEY` con la feature 24 no afecta a estos escenarios: solo se asume un único código `CONNECTORS_DISABLED`.

Pendiente: revisión adversarial por lentes (satisfacibilidad, mensurabilidad, mutante por `Then`, colisión con `today.feature` @s1 y `availability.feature`) y aprobación humana. No activar `in_progress` ni iniciar TDD desde esta entrega.

## Decisiones del coordinador (8 de septiembre de 2026, 21:00)

- Horas locales inexistentes/ambiguas con TZID: semántica de `java.time` (hora inexistente desplazada hacia delante; ambigua con el primer desfase), documentada en `docs/`, sin escenario adicional.
- Concurrencia de sincronizaciones: `performed=false` para la perdedora (@s26), no 409.
- Literales de la sección de Hoy: referencia; ajustables sin cambiar semántica, anotándolo en `progress/tdd_external_calendar.md`.
- `APP_CONNECTOR_KEY` compartida por 25, 27 y 28 con un único código `CONNECTORS_DISABLED`.
