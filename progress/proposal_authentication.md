# Propuesta acotada — authentication (feature 6)

Preparación documental dentro del roadmap aprobado; no inicia producción ni cambia la feature activa project_states. Objetivo: entrar y salir desde la web con una sesión revocable, sin el diálogo Basic del navegador. Mantener el propietario actual para conservar los proyectos existentes.

## Reutilización y alcance

Usar Spring Security ya instalado y Spring Session JDBC compatible con el BOM de Spring Boot 3.5.11. PostgreSQL ya existe: no incorporar Redis, proveedor externo, JWT propio ni almacén de tokens en el navegador. Spring Session ofrece almacenamiento JDBC de HttpSession y scripts de esquema por motor; la migración del esquema se integra con Flyway, desactivando inicialización automática destructiva en producción. Fuentes: [integración Boot/JDBC](https://docs.spring.io/spring-session/reference/guides/boot-jdbc.html) y [configuración JDBC](https://docs.spring.io/spring-session/reference/configuration/jdbc.html).

Un único usuario configurado mediante las credenciales actuales de despliegue. Sin registro público, recuperación por correo, roles nuevos, cambio de usuario propietario ni SSO en este corte. Cambiar el nombre configurado cambia la identidad: no se migra ni reasigna silenciosamente ningún proyecto. Conectores y cuentas adicionales conservan sus features propias.

Versiones comprobadas en el POM del BOM descargado por Gradle: Spring Security 6.5.8 y Spring Session 3.5.5. No actualizar el stack como efecto incidental de esta feature; utilizar sus APIs y esquema compatibles. La documentación de la rama 6.5 puede mostrar un parche posterior y debe contrastarse con la dependencia resuelta al implementar.

La autenticación es un adaptador de seguridad; no introducir contraseñas, sesiones HTTP ni clases Spring en el dominio de proyectos. Spring realiza verificación de contraseña y protección frente a fijación de sesión. Una sesión compartida en PostgreSQL permite que varias réplicas atiendan al mismo navegador; un reinicio de la API no requiere sesión en memoria local. Referencia: [persistencia y gestión de sesión de Spring Security 6.5](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/session-management.html).

## Operaciones propuestas

- GET `/api/session`: respuesta pública 200, sin caché, con `authenticated`, `username` (null si es anónimo) y token CSRF/header para el cliente. No expone contraseña ni identificador de sesión. Permite inicializar la página y renovar el token después de autenticar o salir.
- POST `/api/session`: formulario estándar `application/x-www-form-urlencoded` con username/password y cabecera CSRF. Usar el filtro de formulario de Spring, con respuesta 204 tras éxito y error JSON 401 genérico tras credenciales inválidas. La excepción de formato corresponde sólo al login; escrituras de proyectos conservan JSON estricto.
- POST `/api/session/logout`: requiere CSRF; invalida la sesión almacenada y elimina su cookie. Responde 204 cuando la operación se confirma. No existe logout por GET ni redirección HTML del API. El cliente consulta otra vez GET session al volver al acceso.
- Los demás endpoints siguen privados y reciben identidad del Principal. Retirar HTTP Basic de la cadena de producción para que logout no deje credenciales de navegador que reautentiquen silenciosamente. Los clientes de pruebas y scripts se migran al flujo real de sesión.

La cookie de sesión será HttpOnly, SameSite=Lax y con Path limitado al API. Secure obligatorio para el origen HTTPS público; HTTP sólo para desarrollo en loopback. La implementación concretará la configuración coherente con APP_PUBLIC_ORIGIN, sin confiar arbitrariamente en cabeceras de proxy externas. Tiempo de inactividad inicial: 30 minutos mediante configuración estándar de sesión; sin remember-me. La sesión y el token CSRF permanecen fuera de localStorage/sessionStorage.

## CSRF y errores

Activar CSRF también en login/logout. El cliente conserva el token en memoria y lo envía en escrituras; tras autenticación o logout pide uno nuevo. Spring elimina el token previo en ambas transiciones. Usar el mecanismo estándar y un endpoint de token, sin desactivar protecciones para acomodar la SPA. Referencias: [CSRF](https://docs.spring.io/spring-security/reference/6.5/servlet/exploits/csrf.html) y [logout](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/logout.html).

Conservar OriginGuard y ausencia de CORS permisivo. Un endpoint privado sin autenticación responde 401 UNAUTHENTICATED, incluso al expirar una sesión antes de una escritura. Un usuario autenticado con CSRF inválido recibe 403 con código específico y recuperación deliberada; no se reenvía automáticamente una escritura. Login sin CSRF válido también se rechaza. Credenciales erróneas no distinguen usuario inexistente de contraseña incorrecta. No registrar cuerpos de login, cookies ni tokens.

Un fallo de PostgreSQL no presenta login/logout como confirmado. Si no se pudo confirmar logout, retirar la vista privada del navegador y explicar que la invalidación del servidor no está confirmada, ofreciendo reintento; no afirmar que se ha cerrado la sesión. Los errores internos mantienen respuesta segura y correlación conforme al API existente.

## Web y continuidad

Pantalla de acceso con usuario, contraseña, etiquetas/autocomplete estándar, estado de envío y error comprensible. La contraseña se borra tras respuesta y nunca queda en una URL. Bloquear doble envío, permitir pegar y usar gestores de contraseñas. El contenedor de sesión decide si muestra la aplicación privada: no renderiza proyectos de ejemplo mientras comprueba acceso.

Cerrar sesión desde la navegación, retirar proyectos/borradores y volver al acceso. Una señal BroadcastChannel puede coordinar la retirada de vistas en pestañas del mismo origen, sin transmitir secretos ni persistir información. Recuperar una ruta local propia tras login; nunca aceptar redirecciones a orígenes externos. Ante 401 durante uso, mostrar acceso y desmontar vistas privadas. La comprobación al recuperar visibilidad debe detectar sesiones invalidadas por otra pestaña o caducadas.

Conservar la distinción entre tests de vistas autenticadas y tests del contenedor de sesión, sin interruptores de producción para saltarse autenticación. Las pruebas de seguridad verifican el login real; las pruebas de propietarios pueden usar los mecanismos de test de Spring para preparar identidades, con CSRF explícito en escrituras. Los E2E y smoke completos deben usar cookies/tokens obtenidos por el flujo real, sin Basic residual.

## Evidencia necesaria antes de cierre

Login correcto/incorrecto y anonimato; rotación de sesión; persistencia JDBC entre instancias o reinicio; expiración y logout con rechazo posterior de la cookie antigua; CSRF/origen en login, logout y escritura de proyectos; ausencia de Basic y datos privados tras logout. Regresión completa de crear/leer/editar/estados y smoke publisher con el nuevo cliente de sesión. Pruebas de error de almacenamiento sin éxito ficticio, teclado/foco, matriz responsive y zoom del formulario. No prometer certificación universal ni controles de identidad ajenos a este alcance.

Antes de destilar Gherkin, concretar formato de respuesta GET session y códigos de CSRF/fallo de sesión, comprobar APIs y esquema de la versión resuelta por el BOM y revisar la compatibilidad del proxy/healthcheck actual. No hay una nueva puerta humana para este corte del roadmap; sí contrato previo a producción.

## Revisión del repositorio por integración

El nginx actual usa `auth_request /_session` y el endpoint devuelve204 con Basic. Al adoptar GET session público200, retirar esa autenticación de documentos/assets: servir la SPA pública y proteger los datos en `/api` con Spring. La cookie Path=/api no acompaña documentos y no debe originar subpeticiones que creen sesiones anónimas para cada asset. El `/healthz` de nginx puede permanecer como comprobación del proceso web; no se presenta como verificación de base de datos.

Readiness de E2E/smoke pasa de204+Basic a GET200 seguido de login real. El `fetch` de Node no conserva cookies: reutilizar `APIRequestContext` de Playwright, ya instalado, evita escribir un cookiejar. La autenticación Basic del API de administración de RabbitMQ es independiente y no se retira.

El proxy actual sobrescribe `X-Forwarded-Proto` con `$scheme`. Configurar la cookie Secure a partir del origen público HTTPS acordado o de una cadena de proxies explícitamente confiable; no interpretar una cabecera arbitraria del cliente como prueba de TLS. Al invalidar la sesión, expirar la cookie con su mismo nombre/path, sin limpiar indiscriminadamente cookies de otros servicios del origen.
