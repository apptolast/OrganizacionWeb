# authentication — evidencia UI/UX

Responsable de integración: integration_craftsman. Ponytail full y Caveman lite activos. Esta matriz aplica los treinta principios al acceso y cierre de sesión; no atribuye efectos psicológicos universales a decisiones de diseño.

## Evidencia ejecutada

Cinco casos nuevos pasaron contra PostgreSQL y API reales en 17,4 segundos: entrada fallida/correcta y cookie rotada; recuperación CSRF sin reenviar una escritura; cierre entre dos pestañas y rechazo de cookie anterior; expiración de sesión persistida y origen ajeno; adaptación con teclado y tacto emulado. La demora del login se retuvo en el transporte del fixture; el servidor emitió después su respuesta real. El anuncio apareció en 2 ms y el botón quedó deshabilitado.

La matriz recorrió 22 anchos CSS: 320, 359, 360, 361, 390, 480, 599, 600, 601, 699, 701, 768, 820, 1024, 1099, 1101, 1280, 1440, 1599, 1601, 1920 y 2560. A 768 se utilizó altura 400. El error de credenciales permaneció visible; documento sin desbordamiento horizontal, controles de al menos 44 por 44 CSS y axe sin infracciones de las reglas A/AA seleccionadas. Tab y Enter iniciaron sesión; entrada y salida conservaron un destino de foco distinto del body. Tap emulado inició y cerró sesión.

Zoom nativo con extensión temporal `chrome.tabs.setZoom(2)` y perfil aislado: DPR 1,5 a 3; interior 1426 a 713 con exterior 1440. Ventana exterior 654 produjo interior 320 y documento 312/312. Inputs de 222,33 por 52 CSS y botón de 222,33 por 45 CSS. Se rechazó una contraseña incorrecta y se aceptó la correcta mediante la interfaz a ese zoom y ancho. No se sustituyó zoom real por emulación de viewport.

Capturas `outputs/authentication-desktop.png`, `authentication-mobile.png`, `authentication-real-zoom-320.png` y JSON asociadas. Integración las inspeccionó: campos, error y acción legibles, sin recorte horizontal. Credenciales y usuario de fixture son sintéticos. El coordinador revisó escritorio, móvil y JSON de zoom sin desbordes; el autor frontend también inspeccionó escritorio y móvil como observación de autor, distinguiendo el contorno de foco de una decoración.

Firefox completó el acceso real con feedback de 8 ms. El primer intento WebKit encontró una diferencia de observación: su cookiejar informa SameSite=None aunque la cabecera HTTP emitida contiene SameSite=Lax. El diagnóstico mínimo reproduce la diferencia antes de usar APIRequestContext. No se atribuye verificación efectiva de SameSite a ese motor. La prueba comprueba ahora la cabecera Set-Cookie observada por la página en los tres motores y mantiene la comprobación del cookiejar Lax excepto en WebKit sobre Windows. Firefox y WebKit completaron ambos el recorrido, 2/2 en 6,6 segundos; feedback 194 ms y 2 ms respectivamente. El coordinador aceptó la excepción acotada manteniendo la cabecera contractual y la limitación explícita. Playwright 1.63.0, WebKit 26.6, Windows; no se atribuye enforcement efectivo a ese puerto.

| Principio | Aplicación concreta | Evidencia y límites |
| --- | --- | --- |
| Atención selectiva | Dos campos y una acción principal centran el acceso. | Capturas y DOM; atención humana no medida. |
| Carga cognitiva | No requiere configurar conectores ni proyectos para entrar. | Login real y recuperación de ruta. |
| Estética-usabilidad | Conserva colores, tipografía y controles del espacio privado. | Inspección visual; preferencia humana no medida. |
| Posición en serie | Usuario, contraseña, acción y resultado mantienen su orden. | Tab y estructura visual. |
| Tendencia a la meta | El acceso desbloquea la ruta solicitada sin progreso ficticio. | Ruta /proyectos tras login confirmado. |
| Von Restorff | Iniciar sesión tiene énfasis consistente con la acción principal. | Capturas y texto explícito. |
| Zeigarnik | No añade avisos de tareas pendientes a la pantalla de acceso. | No aplicable a tareas en este corte. |
| Fluir | Acceso y cierre no arrancan un cronómetro de trabajo. | Planificación temporal fuera de alcance. |
| Fragmentación | Campos y acción comparten tarjeta; el pie queda separado. | Capturas y semántica. |
| Memoria de trabajo | Autocomplete estándar admite credenciales del gestor. | Atributos username/current-password verificados; gestor real pendiente. |
| Navaja de Occam | Formulario nativo y sesión del servidor, sin cuentas sociales. | Sin dependencia visual adicional. |
| Conectividad uniforme | No representa relaciones entre proyectos. | No aplicable. |
| Fitts | Inputs y acción miden al menos 44 por 44 CSS. | 22 anchos y zoom real estrecho; hardware táctil pendiente. |
| Hick | Una acción de acceso; recuperación aparece sólo ante el fallo correspondiente. | Login fallido y CSRF recuperable reales. |
| Jakob | Etiquetas convencionales, password nativo, Tab y Enter. | Teclado y tap emulado. |
| Semejanza | Acciones y mensajes usan el estilo común de la aplicación. | Capturas revisadas. |
| Miller | Dos campos agrupados sin aplicar un número mágico de opciones. | Estructura revisada; comprensión humana pendiente. |
| Parkinson | Autenticación no fija duración del trabajo. | No aplicable a horarios. |
| Postel | La interfaz acepta pegado y el servidor valida credenciales y CSRF. | Entrada mediante fill y respuestas reales; pegado manual pendiente. |
| Proximidad | Mensajes aparecen junto al formulario o al control de recuperación. | Error visible en 22 anchos y zoom estrecho. |
| Prägnanz | Etiquetas y errores expresan acciones concretas. | No depende sólo de colores o iconos. |
| Región común | Tarjeta reúne título, campos y resultado de acceso. | Semántica y capturas. |
| Tesler | El servidor gestiona sesión, cookie y revocación; el usuario decide reintentar. | Cookie rotada, cierre y CSRF sin reenvío automático. |
| Modelo mental | Cerrar sesión retira acceso también en otra pestaña. | Dos pestañas y cookie antigua rechazada. |
| Usuario activo | Se recupera la ruta local que motivó el acceso. | Login desde /proyectos y recorrido privado real. |
| Pareto | Credenciales del propietario cubren el acceso personal aprobado. | No se atribuyen porcentajes de uso. |
| Fin de pico | Sólo una respuesta confirmada abre o cierra el espacio. | HTTP 204 real; fallos de almacenamiento cubiertos por backend/autor frontend. |
| Sesgo cognitivo | El error no revela si falló usuario o contraseña. | Fallo de credenciales genérico, sin cuenta inventada en la interfaz. |
| Sobrecarga de opciones | No hay recuperación de contraseña ni métodos de acceso sin implementar. | Alcance cerrado a la cuenta configurada. |
| Doherty | La espera se anuncia al iniciar la solicitud. | 2 ms Chromium y 194 ms Firefox y 2 ms WebKit observados; no garantía universal. |

## Límites

No se probaron dispositivos físicos, teclado virtual, gestores de contraseña reales ni lector de pantalla. Axe no certifica WCAG completa y tap emulado no equivale a hardware táctil. La caducidad se verificó moviendo los timestamps de la sesión real PostgreSQL más allá de treinta minutos, sin esperar ese tiempo en reloj. La cookie Secure bajo HTTPS se verifica en backend; este fixture local usa HTTP. La evidencia de imágenes no representa proyectos ni credenciales reales del usuario. Cierre congelado: 27 E2E conjuntos verdes en 2,1 minutos, smoke del publicador migrado verde y revisión independiente backend APPROVED. La revisión global corresponde al coordinador.



