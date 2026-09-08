# TDD HTTP de integration_api

Checkout aislado codex/integration-api-http desde d987c29. Propiedad:
controladores de gestión y OpenAPI, selección Bearer, autorización HTTP,
cabeceras y pruebas de frontera. A conserva dominio, aplicación, PG,
cuotas y migración; no se inventan puertos ni beans productivos.

Install oficial node scripts/project.mjs install terminó EXIT 0. El intento
previo de usar install como subcomando del harness devolvió su ayuda y
EXIT 2, sin modificar producto. Init oficial está en curso; originales en
integration_http_init.log y su exit al terminar. Sin producto previo al gate.

Primer ciclo previsto: una petición autenticada de creación por cookie
sobre puerto real, con 201 y secreto exclusivo del primer resultado. El
primer test de canal verificará Authorization presente inválido junto a
cookie válida: 401 API_UNAUTHENTICATED, WWW-Authenticate Bearer, sin sesión
JDBC, Set-Cookie ni llamada de negocio. Se registrará RED real; no fixtures
de puertos futuros. OpenAPI y cuotas siguen después del corte compilable.

Frontera acordada con A (propuesta todavía sin bundle): authenticate(token)
retorna acceso; consume(access) revalida vigencia/revocación bajo locks y
consume ambas cuotas. C aplica autenticación, Origin y allowlist/scopes en
ese orden antes de consume. OpenAPI autentica sin cuota. Fallo de token o
cuota conserva STORAGE_UNAVAILABLE, nunca SESSION_UNAVAILABLE.

Candidato mínimo para aislamiento de sesiones: cadena stateless seleccionada
por presencia de Authorization y HttpSessionIdResolver estándar delegado
sólo para cookies del canal humano. Requiere oráculos contra repositorio de
sesiones que falle si se usa; no está acreditado por la configuración sola.
No se cambia política cookie, negocio existente ni rutas ajenas al contrato.

## Primer corte nominal

Init terminó EXIT 0 (3c986d): Node70 y frontend2424/62. Java terminó
BUILD SUCCESSFUL; no se reatribuye un conteo desde XML sobrescrito por
los focos siguientes. Bundle real f485486 integrado completo en5888b9f.

Ciclo1 s1: RED3afd9e, falta de clase Controller comprobada en compilación;
GREEN9efad6, una prueba MVC con puerto real mock y modelo real. Comprueba
owner, args, Location, 201, secreto, forma cerrada y Cache-Control no-store.
Ciclo2 s9: RED686cec, esperaba200 y recibía201; GREEN587633 tras distinguir
resultado sin secreto. Fixture revocada/caducada; no atribuye idempotenciaPG.
Refactor Spotless y regresión2/2 EXIT0 af3d68. Timestamps conservan formato
ISO UTC del modelo, sin exigir seis dígitos si fracción cero.

Este corte sólo acredita creación/replay nominal HTTP. Pendientes explícitos:
JSON cerrado/acotado, validación, seguridad Bearer, gestión restante y OpenAPI.
No bean real añadido ni persistencia simulada acreditada. Los ciclos futuros
sustituirán materialización RequestBody al comprobar límite antes de árbol.

Ciclo3 s4: REDb84626, 4097 bytes sintácticamente válidos alcanzaban puerto;
GREEN188fea con lectura máxima4097 y rechazo413 antes de materializar.
Ciclo4 s4: RED906ca3 en tres variantes de JSON desconocido/duplicado/trailing;
GREEN8a5c21 usando ObjectReader local y opciones Jackson ya instaladas.
Ciclo5 s4: exactamente4096 bytes inicialmente GREEN22639f, sin delta producto.
Los originales de cada comando y EXIT permanecen en progress. Pendiente
resto de validaciones y seguridad; estos casos no cierran contrato completo.

Ciclo6 s2: REDe7343c frente a UUID abreviado/mayúsculas/inválido;
GREENaed7c9 con comparación canónica antes del puerto y error id.
Ciclo7 s2: RED25526b contra null, objeto vacío y coerciones de tipos;
GREENec0094 leyendo árbol acotado local y entero matemático exacto.
No normaliza nombre/scopes ni replica reglas del dominio. Regresión
estructural Spotless+16/16 EXIT0 506e1b, sin fuentes ajenas modificadas.

Ciclo8 s20 componente sesión: RED1353f6 por clase ausente, GREENd26755.
SessionRepositoryFilter real y acceso getSession(false) con Authorization y
cookie no interactúan con repositorio ni emiten Set-Cookie. No está aún
conectado a SecurityConfiguration: no acredita canal Bearer completo.
Ciclo9 s18 cookie-only conserva identidad humana, inicialmente GREENad2c5e.
Ciclo10 s14 query owner: REDa80c3c, GREENe018d5 al rechazar query antes del
puerto. Sin normalización ni selección de propietario desde el cliente.

Bundle c58bad5 incorporado por cherry-pick completo en51f4eee, después de
preservar cinco archivos propios byte-exactos en evidencia externa
integration24-http-pre-context. Ciclo11 s2/s7/s10/s17: RED87d6e7 y
GREENaee1c9, cuatro errores reales del bundle traducidos localmente, sin
secreto ni detalle privado. Regresión con wiring PG real, componente de
sesión y MVC: EXIT0 17535f, Spotless y24 casos en3 suites. El componente
resolver aún no se registra como bean; Bearer y gestión restante pendientes.

Ciclo12 s18/s20: setSessionId/expireSession, cookie y Authorization vacío
presente, cuatro casos inicialmente GREEN83d545. Cookie conserva nombre,
path, HttpOnly, Secure y SameSite; canal Authorization no emite cabecera.
Ciclo13 s4: RED8d2736 y GREEN935bb2, error estructural incluye FieldError
body seguro sin reflejar el nombre privado recibido. No bean resolver aún.

### Ciclos 14–27: gestión y negociación

Ciclo 14: los tres oráculos de autenticación Cookie, CSRF y Origin resultaron inicialmente GREEN (e6c35a), sin cambio productivo. Ciclos 15–17: OpenAPI nominal, allowlist de 18 operaciones y contratos de las tres escrituras tuvieron RED/ GREEN 72463d/9c8ac7, 4f612b/2f9682 y 584045/87cdee. Este recurso sigue parcial: falta completar esquemas de respuestas antes de su aprobación.

Ciclos 18–21: lectura propia, ausencia privada, paginación y revocación con puertos reales de A: RED/GREEN 4d390b/6fb5b3, 52c5f7/dcb8ca, b68be9/511816, ef1b8c/7c61fe. Ciclos 22–23: siete casos de cuerpo/query inesperados y tres identidades no canónicas: RED/GREEN 3e1261/f0f3fb y aded7e/1e2bd3. Refactor focal con formato: 7a3642.

Ciclos 24–25: métodos hermanos no admitidos devuelven 405 y Allow antes del puerto; RED/GREEN 224814/154cde y 9b1ca9/f45d89. Ciclo 26: cuatro errores de lectura/revocación privados resultaron inicialmente GREEN 780e77. No atribuyen persistencia: son slices con puertos reales simulados.

Ciclo 27: Accept text/plain produjo 500 en vez de 406 (3827b2). El primer ajuste no insertó las llamadas por una diferencia EOL; el mismo fallo se preserva en cycle27_adjustment. Guardia local con HeaderContentNegotiationStrategy y selección de especificidad, siguiendo el patrón existente: GREEN cbabcb. Se ejecuta antes del puerto, sin parser nuevo ni cambio global de advice. cefd1c9 se incorpora como 4015ce1 para los puertos Authenticate/Consume revisados; incluye la corrección de listado storage de A.

### Ciclos 28–37: canal Bearer, corte parcial revisable

28: primer recorrido autenticación, cuota y lectura por owner, RED 6460ed (401 en vez de 200), GREEN 550c44. 29: seis variantes inválidas sin fallback Cookie, RED df2f8a, GREEN 2e4c46. 30: 18 operaciones sin scope y nominal previo, RED e33423, GREEN 60fdc1. 31: autenticación antes de Origin y Origin antes de scope, RED b5fa7a, GREEN cc7505. 32: errores propios de autenticación/admisión 401/429/503 antes de negocio, RED 88da90, GREEN f43086. 33: OpenAPI autentica sin scope/cuota, RED 33b57c, GREEN af0a9e.

34: cadena Security con SpringHttpSessionConfiguration y SessionRepositoryFilter reales, cookie SESSION presente. RED b45e41 demuestra consulta al repositorio de sesiones pese a STATELESS. Registrar ApiCredentialSessionIdResolver elimina esa consulta: GREEN ef7528. El repositorio y puertos de aplicación son mocks; no se atribuye JDBC ni PostgreSQL a este slice.

35: las 18 admisiones positivas independientes del filtro resultaron inicialmente GREEN ce1d51. Estos tests llegan a FilterChain y verifican orden/owner, no simulan respuestas de los controladores de negocio. 36: la matriz de exclusiones detectó dos rutas con segmento vacío admitidas por Ant (1aa9cf); 14 variantes ya pasaban. PathPatternRequestMatcher con variables obligatorias corrige ambas, conservando los 18 positivos: GREEN f129fc. 37: dos Authorization simultáneos se rechazan antes de autenticar; inicialmente GREEN fe85db.

Formato y regresión focal final: c72350, EXIT 0; 129 pruebas en cinco XML (52 gestión, 50 Bearer/MVC, 18 admisión del filtro, 6 resolver, 3 OpenAPI), cero fallos/errores/skips. Freeze parcial: cadena/gestión revisables. OpenAPI aún no contiene todos los esquemas de respuestas; falta su cierre, wiring real PostgreSQL y verificaciones finales de frontera. No campaña de mutación ni gate global atribuido. Fuentes de A intactas respecto a 4015ce1.
