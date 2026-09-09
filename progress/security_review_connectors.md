# Revisión de seguridad — canal Bearer (feature 24) y diseño de conectores (25–28)

Revisor: `security_reviewer` (solo lectura). Fecha: 9 de septiembre de 2026.
Rama `main`, con los cambios de `reschedule` sin confirmar en el árbol (no
revisados: fuera de alcance). Listas aplicadas: `ecc:springboot-security` y
`ecc:security-review`, adaptadas a Spring Boot hexagonal con `JdbcTemplate`,
sesión JDBC con formulario y canal Bearer propio (sin JPA, sin JWT, sin CORS).

## Resumen ejecutivo

El canal Bearer de la feature 24 está bien construido en lo esencial: comparación
del verificador en tiempo constante, secreto nunca persistido ni serializado,
allowlist con denegación por defecto, revocación y caducidad revalidadas dentro de
la transacción antes de gastar cuota, y cero concatenación de cadenas en SQL; los
huecos reales que encontré son de medición, no de autorización.
Ni la autenticación fallida, ni el rechazo por scope, ni el documento OpenAPI
consumen cuota, y el inicio de sesión por formulario no tiene ninguna protección
contra fuerza bruta ni en la aplicación ni en `deploy/nginx.conf`.
En el diseño de 25 a 28 el riesgo dominante es el envío HTTP saliente dentro de la
transacción de reclamación sin plazo total ni tope de cuerpo de respuesta, seguido
de huecos concretos en la lista de direcciones SSRF, la falta de identificador de
clave para rotar `APP_CONNECTOR_KEY` y una deduplicación de webhooks apoyada en una
cabecera que no va firmada.

No repito el hueco de oráculo de scope que ya dictaminó el juez
(`progress/judge_integration_api.md`, sección 8, punto 1): está en corrección.

## Tabla de hallazgos

Origen: **[C]** = verificado leyendo código en `main`; **[D]** = riesgo de diseño
sobre features aún no implementadas (no hay código que confirmar ni refutar).

| # | Sev | Origen | Ubicación | Qué rompe | Corrección mínima |
| --- | --- | --- | --- | --- | --- |
| B1 | alta | [D] | `project-spec.md` seccion Feature 25, «Concurrencia…» y «Decisiones» punto 7; `features/webhooks.feature:327` | Envío HTTP dentro de la transacción de reclamación, con solo 5 s de conexión y 5 s de lectura y sin tope de cuerpo de respuesta. Un receptor que gotee un byte cada 4 s nunca dispara el timeout de lectura, fija el hilo del worker y mantiene abierta una transacción PostgreSQL de forma indefinida; con `spring.datasource.hikari.connection-timeout=3000` eso agota el pool y deja toda la aplicación en 503. | Plazo total del intercambio, no solo por lectura, con `HttpRequest.timeout` más un corte duro sobre el envío asíncrono; `BodySubscriber` que aborte pasados 64 KiB; y sacar el envío de la transacción con una columna de arrendamiento. Resolver además la contradicción 5 s frente a «timeout de 10 s» del propio texto. |
| A1 | media | [C] | `backend/src/main/java/com/apptolast/organization/adapter/http/ApiCredentialBearerFilter.java:133`, `:148-153` | La cuota solo se consume después de superar la comprobación de scope. Autenticar (una `SELECT` sobre `api_credentials`) y ser rechazado con 401 o 403 no cuesta nada: trabajo de base de datos sin medir y sin límite por parte de un cliente remoto. | Consumir cuota, o un contador barato separado, antes de la comprobación de scope, y añadir `limit_req` por IP sobre `/api/` en `deploy/nginx.conf`. |
| A2 | media | [C] | `ApiCredentialBearerFilter.java:141-153` | La rama `openApi` salta scope y cuota: cualquier credencial válida puede pedir `/api/v1/integration-openapi.json` sin límite. Excepción a la denegación por defecto que no acota ningún contador. | Mantener la exención de scope y mover `quota.getObject().consume(access)` fuera del `if (!openApi)`. |
| A3 | media | [C] | `adapter/config/SecurityConfiguration.java:86-93`; `deploy/nginx.conf` (sin `limit_req_zone`) | `POST /api/session` no tiene retardo, bloqueo ni cuota, y cada intento cuesta una verificación BCrypt de coste 10. Permite adivinación de contraseña sin límite y agotamiento de CPU. Punto explícito de la lista ECC que hoy no cubre nadie. | `limit_req_zone` por IP en nginx aplicado a `location = /api/session`, o un filtro de retardo progresivo ante fallos consecutivos. |
| B2 | media | [D] | `project-spec.md` 25 «Seguridad y privacidad» y 28 «Seguridad y privacidad»; `features/webhooks.feature:83-95`; `features/external_calendar.feature:80-82` | La lista de direcciones prohibidas es incompleta. Faltan `0.0.0.0/8` (solo se cita la dirección no especificada), `192.0.0.0/24`, `192.88.99.0/24`, `198.18.0.0/15`, `240.0.0.0/4`, `255.255.255.255`, `64:ff9b::/96` (NAT64 hacia IPv4 privada), `2002::/16` (6to4 que encapsula IPv4 privada) y la forma IPv4-compatible `::a.b.c.d`. La feature 28 omite además `100.64.0.0/10`, que la 25 sí bloquea. | Un único `AddressPolicy` compartido que combine `isLoopbackAddress`, `isLinkLocalAddress`, `isSiteLocalAddress`, `isAnyLocalAddress` e `isMulticastAddress` con una lista CIDR explícita que incluya esos rangos, normalizando antes las formas mapeada, compatible, 6to4 y NAT64. |
| B3 | media | [D] | `project-spec.md` 25 «Guardia SSRF…» y «Límites explícitos»; 28 «Seguridad y privacidad» | El rebinding DNS entre comprobación y conexión se acepta como límite y se delega en «la política de egreso del despliegue», que no existe: `deploy/` no define ninguna y el contenedor del backend comparte red de Compose con `db` y `rabbitmq`. Un host controlado por el atacante que responda público al comprobar y una dirección interna al conectar alcanza servicios internos. La obligación de `https` reduce mucho la explotabilidad, porque el servicio interno tendría que presentar un certificado válido para ese nombre, pero no la elimina. | Resolver una vez, validar todas las direcciones y conectar contra la IP literal ya validada conservando el nombre original en `Host` y SNI. Si se mantiene el límite, declarar la política de egreso como requisito de despliegue en `deploy/`, no como suposición. |
| B4 | media | [D] | `project-spec.md` 25 «Concurrencia…» («el receptor deduplica por `X-OrganizationWeb-Event-Id`»); `features/webhooks.feature:207-217` (@s15) y `:222-226` (@s16) | La firma cubre `t + "." + body` y nada más. La cabecera `X-OrganizationWeb-Event-Id`, que es justo el material de deduplicación que se pide al receptor, viaja sin firmar: quien capture una entrega puede reproducirla cambiando esa cabecera y saltarse la deduplicación. Tampoco hay ventana temporal exigida; los 5 minutos de tolerancia son solo una recomendación en `docs/webhooks.md`. | En `docs/webhooks.md`: verificar la firma primero, deduplicar por el `eventId` que va dentro del cuerpo firmado (la cabecera queda como pista) y rechazar si la diferencia entre `now` y `t` supera 300 s. Añadir que la comparación de la firma debe ser en tiempo constante. |
| B5 | media | [D] | `project-spec.md` 25 «Seguridad y privacidad», 27 «Seguridad y secretos», 28 «Seguridad y privacidad» | `APP_CONNECTOR_KEY` es una clave única compartida por 25, 27 y 28, sin identificador de versión. Rotarla inutiliza en silencio todos los secretos guardados y la única recuperación es volver a pegar credenciales de terceros. Las tres secciones se contradicen sobre la clave mal formada: la 25 dice que la aplicación arranca degradada, la 27 y la 28 dicen que no arranca. | Prefijar el texto cifrado con un byte de versión de clave y leer `APP_CONNECTOR_KEY` más `APP_CONNECTOR_KEY_PREVIOUS`, de modo que una rotación descifre lo antiguo y vuelva a cifrar en la siguiente escritura. Unificar la política: fallo rápido al arrancar si la clave está mal formada, modo degradado solo si está ausente. |
| B7 | media | [D] | `project-spec.md` Feature 26 «Seguridad» («una cuota por IP se deja al proxy»); `deploy/nginx.conf` | `GET /calendar/{token}.ics` es `permitAll`, sin sesión ni CSRF, y cada petición abre una transacción `REPEATABLE_READ` y serializa hasta 2000 eventos. La cuota que el diseño delega en el proxy no está implementada en `nginx.conf`. Denegación de servicio no autenticada. El riesgo no es adivinar el token, que son 256 bits. | Añadir `limit_req_zone` y `limit_req` para `/calendar/` en `deploy/nginx.conf` en el mismo cambio que introduzca la ruta. |
| A4 | baja | [C] | `adapter/persistence/PostgresApiCredentialStore.java:40-52`, `:56-65` | La búsqueda previa es `SELECT * FROM api_credentials WHERE id=?`, sin filtro de propietario. Un identificador ya usado por otro propietario devuelve 409 `API_CREDENTIAL_CONFLICT` donde uno libre devolvería 201: oráculo de existencia entre inquilinos, e id ajeno que puede quedar bloqueado. Explotabilidad despreciable (UUID v4, despliegue de una sola identidad) pero rompe la misma invariante que el contrato exige en lectura. | Documentarlo en `docs/integration-api.md`, o cambiar el contrato a id generado por el servidor. No hay arreglo trivial conservando el id elegido por el cliente. |
| A6 | baja | [C] | `backend/src/main/resources/db/migration/V22__api_credentials.sql:1-12` | Las credenciales caducadas y revocadas se conservan de forma indefinida con su `verifier`. El límite de 10 solo cuenta las vivas, así que la tabla crece sin techo. Minimización de datos. | Purga periódica de filas revocadas o caducadas con más de N días, o al menos poner el `verifier` a `NULL` al revocar. |
| A8 | baja | [C] | `adapter/config/SecurityConfiguration.java` (sin bloque `headers(...)`); `deploy/nginx.conf:13-15` | `Content-Security-Policy` y `Referrer-Policy` solo existen en nginx. Un despliegue que exponga el backend sin ese proxy sirve la API con los escritores por defecto de Spring Security únicamente. | Añadir `headers(h -> h.contentSecurityPolicy(...).referrerPolicy(...))` en ambas cadenas, para que la postura no dependa del ingress. |
| A9 | baja | [C] | `.env.example` (línea `APP_PUBLIC_ORIGIN=`); `adapter/config/SessionCookiePolicy.java:11-22` | `.env.example` deja `APP_PUBLIC_ORIGIN` vacío con el comentario «Leave empty for local HTTP», pero `SessionCookiePolicy.create("")` lanza `IllegalArgumentException` por host nulo. `docker-compose.yml:23` lo tapa con `http://127.0.0.1:8080`, así que solo afecta a un arranque directo con `java -jar`. Falla cerrado, pero invita a «arreglarlo» con un valor no seguro. | Poner en `.env.example` el valor de loopback real en vez de vacío. No confirmado en ejecución: deducido de la lectura de `URI.create("")`. |
| B6 | baja | [D] | `project-spec.md` 25 «Seguridad y privacidad»; `features/webhooks.feature:125-126` | El AAD de la feature 25 es solo el `id` del endpoint; las features 27 y 28 usan `owner_id`. En la 25 el texto cifrado no queda atado al propietario: una fila movida o restaurada bajo otro propietario sigue descifrando. | Usar `ownerId + "|" + endpointId` como AAD en la 25 y dejar el mismo criterio de atado en las tres. |
| B8 | baja | [D] | `deploy/nginx.conf:47-49` frente a `project-spec.md` 26 «API y recurso público» | Todo lo que no cuelga de `/api/` cae en el `try_files` de la SPA. `/calendar/{token}.ics` respondería `index.html` con 200, sin `no-store`, sin `nosniff` y sin el 404 indistinguible que el diseño promete: la petición ni siquiera llegaría al backend. | Añadir un bloque `location /calendar/` con `proxy_pass` al backend, junto con la migración V24. |
| B9 | baja | [D] | `project-spec.md` 26, último párrafo («Registro de accesos del proxy… pendiente de confirmar») | El token de capacidad viaja en la ruta y hoy `nginx.conf` no tiene ni `access_log off` ni un `log_format` que enmascare `$request_uri` para `/calendar/`. El propio diseño lo marca como pendiente de infraestructura. | Entregar `access_log off`, o un `log_format` sin `$request_uri`, en el bloque `/calendar/` junto con la feature, no después. |
| B10 | baja | [D] | `.env.example`; `project-spec.md` 28 «Seguridad y privacidad» (`app.connectors.allow-private-addresses`) | `.env.example` no declara `APP_CONNECTOR_KEY`, `APP_WEBHOOKS_ENABLED`, `APP_GITHUB_API_BASE` ni `APP_CONNECTORS_ALLOW_PRIVATE_ADDRESSES`. La última es un interruptor que desactiva la guardia SSRF y solo debe valer `true` en el perfil e2e. | Declarar las cuatro en `.env.example`, con comentario de advertencia explícito en la de direcciones privadas, y un test de wiring que afirme que el perfil de producción la deja en `false`. |
| B11 | baja | [D] | `project-spec.md` 27 «Seguridad y secretos» (`app.github.api-base`) | El PAT del usuario se envía como `Authorization: Bearer` a lo que apunte `app.github.api-base`. Una configuración errónea manda un secreto de un tercero a un host arbitrario. | Validar la base al arrancar contra una lista fija (`https://api.github.com` más loopback) y fallar el arranque si no encaja. Nunca tomar ese valor de nada con alcance de petición. |

## Lista de comprobación de ECC con su estado real

Estado sobre el código de la feature 24 en `main`, salvo donde se indique.

| Punto de la lista | Estado | Evidencia |
| --- | --- | --- |
| Denegación por defecto y allowlist del canal Bearer | Pasa | `ApiCredentialBearerFilter.java:27-102` enumera 18 pares de método, patrón y scope; `:148-152` responde 403 `API_SCOPE_DENIED` cuando no hay coincidencia, y `SecurityConfiguration.java:52` cierra con `anyRequest().authenticated()`. Las rutas de gestión de credenciales no están en la allowlist: una credencial no puede crear otra. |
| Comparación del verificador en tiempo constante | Pasa | `PostgresApiCredentialStore.java:244` usa `java.security.MessageDigest.isEqual`. El verificador es el SHA-256 de 32 bytes de `SecureRandom` (`CreateApiCredential.java:47-51`), no el secreto. |
| Secreto fuera de logs, respuestas, `toString` y trazas | Pasa | `ApiCredentialIssuance.toString` y `ApiCredentialCreation.toString` devuelven `secret=REDACTED`. Ningún `Logger` toca las clases de credenciales: el único registro es `ApiErrors.java:126-133`, que emite `correlationId` y `getClass().getSimpleName()`, nunca el mensaje. `server.error.include-message=never` e `include-stacktrace=never`. |
| Revocación y caducidad comprobadas en transacción antes de gastar cuota | Pasa | `PostgresApiCredentialStore.consume` toma los dos bloqueos asesores, relee con `find(owner,id)` y revalida `revokedAt`, `expiresAt`, scopes e identidad habilitada en `:277-284`, antes de escribir los contadores en `:309-324`. La autenticación previa ya filtra en SQL con `revoked_at IS NULL AND expires_at>?` en `:242`. |
| Sin concatenación de cadenas en SQL | Pasa | Todo va por `?`, incluidas las claves de `pg_advisory_xact_lock`. El único `sql +=` (`:161`, `:163`) concatena literales fijos, nunca entrada del usuario. El cursor se valida con ida y vuelta base64url antes de usarse (`:141-155`). |
| Validación de entrada en la frontera | Pasa | `ApiCredentialController.create` limita el cuerpo a 4096 bytes antes de materializar y exige un objeto de exactamente 3 propiedades, con detección de duplicados y de tokens finales. `ApiCredentialIntent` valida nombre, scopes contra lista blanca y `expiresInDays` en 7, 30 o 90, con `CHECK` equivalente en `V22__api_credentials.sql:5`. |
| Cabecera `no-store` | Pasa | Explícita en todas las respuestas del controlador y en `ApiCredentialBearerFilter.problem` (`:176`). Afirmada en test: `ApiCredentialApiTest.java:78` y otras nueve, y `ApiCredentialBearerTest.java:227`. |
| Cabeceras de seguridad | Parcial | `nosniff`, `X-Frame-Options` y HSTS quedan en los escritores por defecto de Spring Security; CSP y `Referrer-Policy` solo en `deploy/nginx.conf:13-15`. Ver A8. |
| Postura de CSRF por canal | Pasa | Sesión: `csrf(Customizer.withDefaults())` más `OriginGuard` antes de `CsrfFilter` (`SecurityConfiguration.java:73-76`). Bearer: `csrf.disable()` solo en la cadena cuyo `securityMatcher` exige cabecera `Authorization` (`:42-44`), que es stateless y donde una credencial inválida termina en 401. |
| Postura de sesión por canal | Pasa | Bearer `STATELESS` (`:46-47`) y `ApiCredentialSessionIdResolver` no resuelve ni escribe cookie cuando hay `Authorization`, así que el canal Bearer no puede fijar ni heredar sesión. Cookie de sesión `HttpOnly`, `SameSite=Lax`, `Secure` cuando el origen es https, path `/api` (`SessionCookiePolicy.java:24-30`). |
| 404 indistinguible entre recurso ajeno e inexistente | Pasa en lectura | `find(owner, id)` filtra por `owner_id` y `id` (`:112`), y el controlador devuelve el mismo 404 `API_CREDENTIAL_NOT_FOUND` en ambos casos (`:167-173`). No se cumple en creación: ver A4. |
| Limitación de tasa con 429 y `Retry-After` | Parcial | Existe y es correcta en el camino admitido: 60 por credencial y 120 por propietario y minuto (`:306`), `Retry-After` igual a los segundos que faltan para el corte de minuto (`:308`), cabecera y 429 emitidos en `ApiCredentialBearerFilter.java:154-157`. No cubre autenticación fallida, rechazo por scope ni el documento OpenAPI (A1, A2), ni el inicio de sesión por formulario (A3). |
| Autorización por operación | En corrección ajena | Hueco de oráculo señalado por el juez, sección 8 punto 1. No lo reevalúo. |
| Contraseñas con hash | Pasa | `BCryptPasswordEncoder` en `SecurityConfiguration.java:24`; la credencial de arranque llega por entorno y se rechaza en blanco. |
| Secretos externalizados, nada comprometido | Pasa | `application.properties` es todo sustitución de entorno. `.gitignore` ignora `.env` y `.env.*` salvo `.env.example`. `git ls-files` no devuelve ningún `.env`, `.pem` ni `.key` seguido. Ver B10 para las variables de 25 a 28, aún sin declarar. |
| XSS e interpretación de entrada | No aplica aquí | La superficie de la feature 24 es JSON; el backend no genera HTML ni plantillas. El frontend no guarda el secreto en `localStorage` ni `sessionStorage`: `frontend/src/integration-api-intent.ts` solo persiste la intención de navegación. |
| Salida externa y SSRF | No aplica en 24, abierto en 25 a 28 | La feature 24 no hace ninguna llamada saliente. Toda la exposición SSRF es de diseño: B1, B2, B3. |
| Dependencias sin vulnerabilidades altas nuevas | No verificado | No ejecuté auditoría de Gradle ni `npm audit`: instrucción explícita de no lanzar suites pesadas con seis worktrees en paralelo. |

## Límites de lo no verificado

1. No ejecuté nada: ni tests, ni PIT, ni Stryker, ni auditoría de dependencias, ni
   arranque de la aplicación. Todo lo marcado «pasa» procede de lectura de código,
   no de una prueba que lo demuestre. A1, A2 y A3 son deducciones de flujo de
   control que un test de integración confirmaría en minutos; conviene escribirlo
   antes de darlos por ciertos.
2. Auditoría de dependencias pendiente. No hay dato sobre CVE nuevas en el grafo de
   Gradle ni de npm. La lista de ECC lo exige antes de publicar.
3. A9 no está confirmado en ejecución. Deduzco el fallo de arranque de
   `URI.create("")` con host nulo; no arranqué la aplicación para verlo.
4. No revisé el árbol sin confirmar de `reschedule` (`BlockChange*`,
   `V12__block_changes.sql`, los tests nuevos) ni ninguno de los worktrees de
   `C:/Users/vhurt/ow-worktrees/`. Quedan fuera de este dictamen.
5. La parte 2 es diseño, no código. Las features 25 a 28 no están implementadas:
   cada punto [D] es un riesgo sobre el texto de `project-spec.md` y sus contratos
   `.feature`, no una vulnerabilidad presente en `main`. Puede desaparecer si la
   implementación se aparta del texto, y también puede empeorar.
6. La feature 26 la revisé solo por encima, en lo tocante al token público del feed
   ICS: B7, B8 y B9. No repasé el escritor ICS, ni el escapado de TEXT, ni la
   ventana de 2000 eventos.
7. No leí ningún `.env` real ni ningún valor de clave. La comprobación de variables
   se hizo contra `.env.example` y `docker-compose.yml`.
8. No busqué secretos en el historial de git, solo en el árbol seguido.
9. No afirmo que el canal Bearer sea seguro. Afirmo que los puntos marcados «pasa»
   los comprobé en el código citado, y nada más.

## Resolución del coordinador sobre A1 y A2 — 9 de septiembre de 2026

Los hallazgos A1 y A2 se cierran como **riesgo aceptado**, no como defecto corregido, porque su corrección tal y como estaba propuesta contradice escenarios ya aprobados del contrato.

El artesano encargado de aplicarlos paró y pidió cambio de contrato en lugar de implementarlos, que es la conducta correcta. Sus citas son exactas. El escenario `@s32` fija un `Given` de credencial válida **sin cuota restante** y un `Then` en el que el documento OpenAPI responde **sin consumir cuota** ni ejecutar negocio; consumirla haría inalcanzable ese documento justo cuando la cuota se agota, que es cuando un integrador más necesita consultarlo. La última fila de `@s24` exige 403 ante token válido sin scope y con cuota agotada, de modo que comprobar la cuota antes que el scope devolvería 429 y rompería esa fila. Tres pruebas vigentes fallarían.

El riesgo que describe la revisión es real: un cliente remoto puede provocar una consulta a `api_credentials` sin que ningún contador la mida. La mitigación correcta no es violar el contrato sino la que la propia revisión ofrece como alternativa en A1 y exige en A3: limitación de tasa por dirección en el proxy inverso. Esa medida queda como **requisito obligatorio antes de cualquier despliegue productivo**, registrada en la sección de enmiendas de seguridad de `project-spec.md` y en `progress/current.md`. No se aplica mientras haya carriles ejecutando pruebas de extremo a extremo, porque cambiar `deploy/nginx.conf` reconstruye su imagen web y desestabiliza sus suites.

Cambiar el contrato para consumir cuota en esos dos caminos sigue siendo una opción legítima, pero exigiría reescribir `@s32` y `@s24` y volver a pasar por la puerta de aprobación. No se hace en esta sesión.
