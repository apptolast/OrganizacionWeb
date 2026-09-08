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
