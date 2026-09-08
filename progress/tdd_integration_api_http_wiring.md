# Wiring HTTP/Bearer real y corrección de logout (24)

Contexto final de A: 8d7d8b2 incorporado como 29f8025. La clase nueva ApiCredentialHttpPersistenceTest usa SpringBoot, MockMvc, PostgreSQL real y todos los puertos productivos. El spy de JdbcIndexedSessionRepository observa el adaptador real; no reemplaza su comportamiento. Contenedor privado singleton por JVM, limpieza limitada a propietarios de esta fixture. No navegador ni socket acreditado.

38 inicialmente GREEN 0a425f: login Cookie/CSRF, creación HTTP real, admisión Bearer con cookie presente, cuota durable y cero interacciones con sesiones; snapshot de spring_session/xmin/ctid idéntico. 39 inicialmente GREEN 0ba446: POST de negocio sin CSRF con Bearer, GET/ETag, OpenAPI sin cuota y revocación real con posterior 401. Refactor de aislamiento de fixture b779fb.

40 inicialmente GREEN aaa606: indisponibilidad real de tabla api_credentials dentro del PG privado produce STORAGE_UNAVAILABLE, no SESSION_UNAVAILABLE, sin cookie ni cuota. Tabla restaurada en finally. 41 inicialmente GREEN 1e01d1: firma incorrecta, revocación, bootstrap deshabilitado y propietario huérfano, siempre 401 uniforme, sin sesiones ni cuotas. Usuario habilitado restaurado en finally.

42 RED f7a35d: POST /logout con Authorization inválido respondió 302 por LogoutFilter predeterminado antes de Bearer. Cambio productivo único: logout.disable() en cadena Bearer; GREEN fde39d. 43 inicialmente GREEN 2dfa62: token válido en esa ruta devuelve 403, sesión Cookie conserva filas/xmin/ctid y cero interacciones; después GET /api/session sigue autenticado y logout Cookie legítimo funciona. Ningún cambio en la cadena Cookie.

Fixture nominal del slice corregida de prefijo ficticio owk1_ a owp_; el test real utiliza el secreto emitido por el puerto. Regresión focal final 38fe40: formato y seis suites, 138 pruebas, cero F/E/S, EXIT 0. OAS aún parcial; no gate global ni PIT en este corte.

Revisión independiente: se fija Clock del contexto de test en 2026-09-08T12:00:10Z con @Primary, para que dos solicitudes no crucen una ventana de cuota. No RED intermitente inventado. Imagen de fixture fijada postgres:17.9-alpine. Foco posterior de la clase: nueve pruebas, cero F/E/S, EXIT 0 fb0d68 y formato. Las 138 originales siguen preservadas; no se sustituyen por este foco de nueve.
