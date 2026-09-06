# Revisión — authentication

**Veredicto: APPROVED.** El coordinador revisó fuentes, contrato, casos límite, pruebas, integración y mutación. La revisión backend independiente está en judge_authentication_backend.md. Ponytail full y Caveman lite se aplican conservando arquitectura, seguridad, TDD y accesibilidad.

## Comportamiento y decisiones

Spring Security y Spring Session JDBC gestionan formulario, identidad, rotación, CSRF, almacenamiento y caducidad. GET de sesión devuelve exclusivamente cuatro campos; el gate espera su confirmación antes de montar datos privados. Basic está retirado. La identidad del propietario, ETag, capacidad y los tres eventos existentes conservan sus contratos.

La política de cookie valida el origen y sólo admite HTTP en loopback; Secure deriva de configuración, nunca de Forwarded. OriginGuard precede a CSRF. El filtro exterior maneja el fallo de almacenamiento de sesión antes de comprometer la respuesta. Las pruebas HTTP con PostgreSQL fuerzan fallos reales de UPDATE y DELETE: reciben 503 sin Set-Cookie provisional y conservan el reintento cuando el cierre no se confirma. Se contrastó el orden de commit/invalidate del SessionRepositoryFilter oficial 3.5.5.

El cliente compartido conserva Headers nativo y cancela operaciones al desmontar. Se verificaron respuestas tardías, cuerpo 403 demorado, expiración durante logout, rechazo tardío de GET, dos cierres entrelazados y GET postlogout reemplazado por visibilidad. La recuperación CSRF conserva borrador y no repite escrituras. El formulario cancela el envío nativo; contraseña, token y cookie no se guardan en almacenamiento web persistente. BroadcastChannel sólo transmite el cierre.

## Evidencia ejecutada

- Init independiente 22240: EXIT 0, lint y 384 pruebas backend; XML con cero fallos, errores u omitidas. Frontend inicial: 241 pruebas. Tras refuerzos sólo de pruebas, el autor ejecutó 260/260 y lint verdes, además de comprobar la aserción final del primer commit de React.
- PIT propio de sesión: 41/44, 93,18 %, cero sin cobertura. Tres supervivientes equivalentes eliminan asignaciones idénticas a defaults SESSION, HttpOnly=true y Lax. Contrastados con [DefaultCookieSerializer 3.5.5](https://raw.githubusercontent.com/spring-projects/spring-session/3.5.5/spring-session-core/src/main/java/org/springframework/session/web/http/DefaultCookieSerializer.java). No se atribuye cobertura del dominio anterior a autenticación.
- Stryker focal completo: 302/355, 85,07 %, cero sin cobertura, timeouts o errores. Se revisaron los 53 supervivientes. Replay separado: 79/79 Killed; comprobación adicional del primer commit: 1/1 Killed. El coordinador cotejó JSON por archivo, ubicación, mutador y sustitución: 29 supervivientes originales quedan detectados; los otros 24 son equivalentes en el flujo actual, justificados individualmente en mutation_authentication_frontend.md. No se suman replays a una puntuación global inventada.
- E2E final sobre imagen congelada: 27/27 en 2,1 minutos. Incluye recarga, persistencia de sesión tras reinicio real, acceso, cierre en dos pestañas y regresión completa de proyectos. La adaptación de teclado conserva el skip-link y su destino desde el nuevo foco inicial.
- Publicador con sesión real: EXIT 0; creación, edición y estados conservan eventos, recuperación del broker y persistencia Rabbit. Los fixtures y volúmenes sintéticos quedaron retirados.

## UX y límites

Matriz de treinta principios documentada; 22 anchos, teclado, tap emulado, axe, foco y anuncios. Se inspeccionaron escritorio/móvil y JSON de zoom nativo: 200 %, interior 320 CSS, documento 312/312 y controles de 222 por 52 o 45 CSS. No se declara validación física, de lectores de pantalla ni cumplimiento psicológico universal.

Chromium, Firefox y WebKit completan el acceso. Playwright 1.63.0/WebKit 26.6 revisión 2359 en Windows informa SameSite=None aunque el encabezado real contiene Lax. La excepción acotada comprueba la cabecera en todos los motores y el atributo almacenado fuera de ese puerto; no demuestra enforcement SameSite en WebKit Windows. El parser y esquema actuales del puerto curl de WebKit ofrecen una causa compatible, no identidad demostrada con el binario probado. La política de producción permanece intacta.

No se ha desplegado en el servidor del usuario. La preparación documental de create_task no representa implementación ni altera el cierre de este contrato. Queda autorizado el cierre de authentication por su autor y su commit/push; la CI se registrará después de ejecutarse.
