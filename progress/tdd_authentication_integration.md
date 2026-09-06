# authentication — integración

Contrato aprobado y propuesta revisados; Ponytail full y Caveman lite activos. Ownership de herramientas, proxy y pruebas, sin producción backend/frontend. Las comprobaciones de integración se ejecutan sobre los primeros cortes ya verdes de los autores; no se atribuye un RED ficticio a código correcto existente.

Migración realizada: helper de APIRequestContext con cookiejar nativo, fixture autenticado para los recorridos anteriores y contextos anónimos separados para probar autenticación. Ninguna cabecera CSRF global se inyecta en el navegador. El API de administración Rabbit conserva su autenticación independiente.

## Primer corte ejecutable

Fixture aislado `organizationweb-e2e-36116`, PostgreSQL y aplicación reales, imágenes construidas desde el primer gate y API de sesión funcional. La primera prueba de acceso pasó: documento público, sesión anónima exacta, rechazo de Basic sin challenge, credenciales incorrectas con HTTP 401 y contraseña borrada, login real con HTTP 204, rotación de cookie, acceso a la ruta original y ausencia de almacenamiento web persistente. Comando `node .e2e-work/auth-check.mjs --grep public.access`: 1/1, 3,9 segundos.

Tres comprobaciones de regresión focalizadas también pasaron con la nueva sesión: creación seguida de lista, detalle y recarga; edición persistente y no-op; transiciones de estado con lectura y edición posteriores. Sus tiempos totales fueron 3,2, 3,9 y 9,3 segundos. Una selección inicial `--grep creates` no encontró casos; se corrigió por `--grep real.creation`, sin atribuir el error de selección a la aplicación.

El proxy deja documentos y assets públicos y conserva la API privada. Compose permite configurar `APP_PUBLIC_ORIGIN` por `.env`; el valor vacío conserva HTTP local con `WEB_PORT`. Se validaron ambos casos mediante `docker compose config` con variables sintéticas, sin imprimir credenciales. Los dos runners fijan su propio origen para no heredar el del usuario.

Cierre entre pestañas, CSRF y expiración tienen pruebas preparadas; esperan que los autores terminen esos cortes. La matriz UX, zoom real, regresión conjunta y revisión independiente siguen pendientes.

## Segundo corte

Reconstrucción conjunta sobre el mismo fixture: los cinco casos nuevos pasaron en 17,4 segundos. Incluyen matriz de 22 anchos, axe, teclado, tap, foco, recuperación CSRF sin repetir POST, cierre entre pestañas y rechazo de cookie anterior, expiración real de timestamps PostgreSQL y origen ajeno. El token inválido se modifica en la solicitud real; no se simula la respuesta del servidor. Feedback login: 2 ms.

Zoom nativo al 200 % completado a 713 y 320 CSS; documento estrecho 312/312, inputs 222,33 por 52 y botón 222,33 por 45 CSS. Login fallido y correcto confirmados a ese tamaño. Capturas y JSON guardados en outputs; matriz detallada en `progress/ux_authentication.md`.

El primer smoke Firefox/WebKit encontró una discrepancia del cookiejar WebKit Windows: None ante una cabecera emitida Lax. Diagnóstico aislado repetido sin valores de sesión: Playwright 1.63.0, WebKit 26.6, flags del servidor Path=/api, HttpOnly, SameSite=Lax; metadatos del motor Path=/api, HttpOnly=true, Secure=false, SameSite=None antes y después de APIRequestContext. La prueba comprueba Set-Cookie observado por la página en todos los motores y mantiene Lax en el cookiejar salvo WebKit Windows. Firefox y WebKit completaron después el recorrido, 2/2 en 6,6 segundos; feedback 194 y 2 ms. La excepción está pendiente de revisión independiente; no demuestra enforcement SameSite de ese puerto.

La antigua simulación de HTTP 401 dejaba una sesión real válida: el nuevo gate la recuperaba y hacía fallar el encabezado antiguo. Se cambió el fixture para revocar la sesión SQL real y comprobar Usuario y eliminación del borrador. Edición y lectura recuperable volvieron a verde, 8,8 y 4,5 segundos. No se cambió producción para adaptar una expectativa obsoleta.

El coordinador aceptó la excepción WebKit Windows con encabezado contractual verificado y enforcement no probado. Su investigación primaria en WebKit main (`Source/WebCore/platform/network/curl/CookieUtil.cpp` y `CookieJarDB.cpp`) encuentra una causa compatible en el puerto curl, pero no demuestra identidad con la revisión instalada. No se modifica la cookie de producción.

La primera pasada conjunta terminó con 23/27 verdes. Los cuatro fallos de teclado heredado asumían body como foco inicial; el gate ahora enfoca el encabezado de destino. Se conserva acceso real por teclado al skip-link usando Shift+Tab, geometría oculta/visible y ausencia de trampa. Tras la reconstrucción final, los cuatro casos pasaron en 7,8 segundos. La pasada conjunta congelada está ejecutándose; no se suman resultados de distintas imágenes para declarar un verde conjunto.

## Cierre congelado

La pasada conjunta final `node .e2e-work/auth-all.mjs` terminó con EXIT 0: **27/27 en 2,1 minutos**. Incluye los cinco nuevos casos y toda la regresión existente; reinicio real del backend conserva cookie, sesión, propietario y lectura del proyecto. No se cambiaron fuentes durante esta pasada. El login anunció espera en 1 ms; lectura de lista/detalle en 104/38 ms.

`pnpm test:publisher` terminó con EXIT 0. El cliente usa sesión real obtenida por formulario y token por APIRequestContext. Se verificaron creación y publicación original, creación con broker detenido y recuperación, edición con HTTP 200/evento pendiente y recuperación de Updated, cambio de estado con evento de ocho campos y recuperación, y reinicio de Rabbit con el mismo volumen y topología quorum durable. No se repitió la matriz de crash del proceso, cuyo código no cambió. El runner retiró exclusivamente su stack y volúmenes sintéticos.

La revisión independiente backend está en `progress/judge_authentication_backend.md`, APPROVED. El coordinador ejecutó init 22240 con EXIT 0; este agente verificó XML backend: 384 pruebas y cero fallos, errores u omitidas. Frontend: 241 pruebas según ejecución del coordinador. PIT propio de autenticación: 41/44, tres defaults equivalentes comprobados en la fuente oficial fijada.
El fixture de revisión organizationweb-e2e-36116 terminó con EXIT 0 y retiró sus tres contenedores, red y volumen PostgreSQL. Se eliminaron sus archivos temporales de estado y señal; no se tocaron recursos ajenos.
