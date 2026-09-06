# Review — authentication, backend

**Veredicto: APPROVED.** Revisión independiente de las fuentes backend por integration_craftsman, que no implementó el backend de autenticación. La revisión global y del frontend corresponde al coordinador. El coordinador revisa además las pruebas y herramientas de integración escritas por este agente; no se presentan como autorrevisión independiente.

## Alcance y calidad

Leídos contrato, propuesta, bitácora, configuración de seguridad y cookie, controlador, clasificación de CSRF, filtro exterior de persistencia, migración V6 y pruebas HTTP/unitarias pertinentes. Ponytail full y Caveman lite activos. La solución conserva Spring Security y Spring Session JDBC para autenticación, fijación de sesión, CSRF, cookie y almacenamiento; no incorpora un repositorio paralelo ni cambios de dominio o eventos.

No se encontraron bloqueantes. Basic está deshabilitado, la API requiere identidad y GET de sesión entrega sólo los cuatro campos aprobados. El OriginGuard precede a CSRF y protege también login/logout. El token anterior al login queda invalidado, no sólo cambia su representación enmascarada. La identidad conserva el nombre configurado y los recorridos anteriores mantienen propietario, ETag, capacidad y outbox.

El filtro de fallo rodea SessionRepositoryFilter y captura fallos de almacenamiento antes del compromiso de la respuesta. Reinicia cabeceras provisionales y devuelve HTTP 503 SESSION_UNAVAILABLE; no confirma un Set-Cookie de borrado si el DELETE falla. No suprime errores ajenos ni convierte una respuesta ya comprometida en éxito. Los handlers de login/logout no fuerzan flush anticipado.

La cookie usa Path /api, HttpOnly y Lax; Secure deriva de APP_PUBLIC_ORIGIN y no de cabeceras enviadas por el cliente. La política rechaza orígenes ambiguos e HTTP fuera de loopback. El serializador real se prueba con origen HTTPS y cabeceras reenviadas contrarias. El fixture de navegador usa HTTP local: no se afirma que haya probado terminación TLS de producción.

La migración aditiva usa las tablas nativas de Spring Session con principal TEXT, índices de identidad/caducidad y atributos con eliminación en cascada. La inicialización automática está desactivada y el timeout es de treinta minutos. No hay cambio de identidad del propietario ni exposición de sus proyectos al publicar documentos de la SPA.

## Cobertura del contrato

| Escenario | Prueba o evidencia revisada |
| --- | --- |
| s1 | AuthenticationHttpTest.s1_anonymousSessionIsPublicExactAndPersisted y s1_storageFailureCannotMasqueradeAsAnonymous, con servidor HTTP y PostgreSQL reales. |
| s2 | s2_loginRotatesPersistedSessionAndConfirmsOwner y s2_s18_loginRefreshesCsrfBeforeCreatingOwnProject; rechazo del token viejo antes de crear con el nuevo. |
| s3 | s3_credentialsFailWithSamePublicProblem, tres credenciales inválidas y mismo cuerpo público. |
| s4 | s4_basicCannotAuthenticateOrTriggerBrowserChallenge; también navegador sin fixture autenticado. |
| s5 | E2E create-project: project and outbox survive reload and backend restart. Reinicio real del contenedor, misma cookie, GET autenticado y detalle visible sin relogin. |
| s6 | s6_s10_expiredSessionReturns401BeforeCsrfAndCannotWrite; E2E actualiza timestamps persistidos más allá del límite y comprueba retirada de acceso. |
| s7 | s7_logoutDeletesServerSessionAndExpiresSameCookie; E2E dos pestañas y reproducción de cookie anterior en GET/POST rechazados. |
| s8 | s8_getLogoutDoesNotInvalidateSession y recorrido de navegador equivalente. |
| s9 | s9_csrfRejectsEveryUnsafeOperation cubre login/logout/create/edit/state. E2E rechaza token inválido real y no reenvía automáticamente. |
| s10 | s6_s10_expiredSessionReturns401BeforeCsrfAndCannotWrite y E2E sesión caducada con token inválido. |
| s11 | s11_foreignOriginCannotBypassProtectionWithValidCsrf cubre login/logout/proyecto. E2E origen ajeno con token válido confirma HTTP 403. |
| s12 | s12_storageFailureNeverConfirmsSessionAndPreservesRetryCookie, triggers PostgreSQL reales para UPDATE de login y DELETE de logout, HTTP 503, ausencia de Set-Cookie, identidad SQL y reintento. |
| s13 | SessionCookiePolicyTest comprueba serializador real, HTTPS, loopback y cabeceras arbitrarias. E2E verifica atributos emitidos y ausencia de almacenamiento web. |
| s14–s17 | Frontera frontend revisada por el coordinador; integración comprueba feedback, foco, dos pestañas, ruta local y CSRF sin reenvío. Los casos de cancelación/visibilidad están en las pruebas del autor frontend. |
| s18 | Pasada conjunta de 27 E2E sobre el corte congelado, con sesiones reales y sin cabecera CSRF global del navegador. |
| s19 | progress/ux_authentication.md: treinta principios, 22 anchos, axe, teclado, tap y zoom nativo al 200 %. |

## TDD y verificación

La bitácora backend documenta RED reales para acceso anónimo, login, errores genéricos, retirada de Basic, logout, clasificación de CSRF, expiración y fallo PostgreSQL. Las verificaciones añadidas sobre producción ya correcta se distinguen como GREEN inicial, sin fabricar un RED.

El coordinador ejecutó init 22240 con EXIT 0: lint y 241 pruebas frontend. Este revisor leyó los XML backend y sumó independientemente **384 pruebas, cero fallos, errores u omitidas**; AuthenticationHttpTest contiene 22 casos. No se repite Gradle sobre el mismo corte.

PIT de autenticación leído directamente: **41 KILLED y 3 SURVIVED, 93,18 %**. Los tres supervivientes eliminan llamadas redundantes setCookieName(SESSION), setUseHttpOnlyCookie(true) y setSameSite(Lax). Sus valores por defecto se verificaron en la [fuente oficial Spring Session 3.5.5](https://raw.githubusercontent.com/spring-projects/spring-session/3.5.5/spring-session-core/src/main/java/org/springframework/session/web/http/DefaultCookieSerializer.java); son equivalentes para la dependencia fijada. No se usa la puntuación del dominio intacto para ocultar esta cobertura.

La pasada E2E final terminó **27/27 en 2,1 minutos**, con reinicio real y los cuatro recorridos históricos de teclado adaptados al foco inicial del gate. No se suman resultados de imágenes distintas. El smoke del publicador migrado a APIRequestContext con sesión terminó con EXIT 0: creación, edición y estados publican sus eventos originales; broker detenido conserva escrituras y pendientes; recuperación y reinicio de Rabbit conservan mensajes y topología. Evidencia detallada en la bitácora de integración.

## Límites explícitos

Playwright 1.63.0 con WebKit 26.6 en Windows informa SameSite=None aunque la cabecera real contiene Lax. Se verifica la cabecera en todos los motores y el cookiejar Lax en Chromium/Firefox; la excepción Windows WebKit fue revisada por el coordinador. No se afirma enforcement SameSite en ese puerto. No se desplegó TLS, DNS ni infraestructura del servidor del usuario. Los dispositivos físicos y lectores de pantalla quedan fuera de las comprobaciones automáticas documentadas.

