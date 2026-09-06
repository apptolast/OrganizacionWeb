# authentication — TDD frontend

Ponytail full y Caveman lite leídos y aplicados. Baseline raíz aceptado: 176 pruebas frontend y lint verdes, 328 backend. No se repitió init. Contrato aprobado: `features/authentication.feature`; matriz UX a cargo de integración. Producción limitada a frontend.

## Ciclos observados

Cada ciclo ejecutó primero el ejemplo indicado fallando y después pasó con el cambio mínimo. Los outlines agrupan variantes del mismo comportamiento.

1. @s1: gate inexistente (import RED); GET previo al montaje y formulario etiquetado GREEN.
2. @s2: enviar formulario no abría App (RED); POST form-urlencoded con CSRF, seguido de GET confirmado, GREEN.
3. @s1: respuestas incompatibles montaban App o no ofrecían error (seis RED); validación exacta de cuatro campos y recuperación GREEN. Un error de sustitución de PowerShell dejó la fuente sin cambiar, se corrigió antes del GREEN; no se contó como ciclo funcional.
4. @s3/@s12/@s14: botón activo y contraseña retenida ante login demorado/fallido (tres RED); espera, doble envío y errores seguros GREEN.
5. @s18: POST de creación sin CSRF (RED); cliente compartido y token sólo en memoria GREEN.
6. @s6: HTTP 401 dejaba el borrador montado (RED); notificación al gate, retirada y comprobación nueva GREEN.
7. @s7: acción de cierre ausente (RED); retirada inmediata, POST logout y GET anónimo GREEN.
8. @s12: logout 503/red perdía recuperación o lanzaba rechazo sin manejar (dos RED); aviso honesto y reintento deliberado GREEN.
9. @s6: lectura/lista y precarga de edición no invalidaban el gate (dos RED); ambos pasan por el cliente compartido GREEN.
10. @s14: efecto StrictMode no abortaba GET antiguo (RED); AbortController y guard de respuesta GREEN.
11. @s14: creación abandonada HTTP 401 retiraba la vista nueva (RED); creación aborta al desmontar y cliente ignora notificación de señal cancelada GREEN.
12. @s14: login resuelto tras desmontar enviaba otro GET (RED); cancelación de operación GREEN.
13. Refactor con 21 ejemplos verdes: lógica de sesión en use-session, transporte/validación separado de JSX, estilos SCSS reutilizados y entrada real SessionGate. Build y suite focalizada verdes. Integración confirmó login, creación, lectura, edición y estados usando CSRF real.
14. @s17: faltaba recuperación de token en escritura rechazada (RED); GET deliberado conserva borrador y nunca repite POST, GREEN.
15. @s9/@s17: login/logout 403 no ofrecían recuperación (dos RED); reconocimiento del código cerrado y recuperación explícita GREEN.

Estado parcial: 24 ejemplos de autenticación verdes. Pendientes: coordinación entre pestañas, visibilidad, foco y otros límites de recuperación; después regresión completa y mutación acotada. No se declara cierre.

16. @s15: faltaba señal entre pestañas (RED); BroadcastChannel emite únicamente `logout` tras 204 y cierra su recurso en cleanup, GREEN.
17. @s15: la pestaña receptora conservaba datos (RED); señal cerrada retira vistas inmediatamente y comprueba sesión, sin reenviar la señal, GREEN.
18. @s15: recuperar visibilidad no comprobaba sesión (RED); listener de visibilitychange, retirada de sesión caducada y cleanup GREEN.
19. @s19: al entrar quedaba el foco en body (RED); foco en encabezado del contexto nuevo sólo si no existe otro control elegido, GREEN.
20. @s17: fallo de red durante renovación descartaba borrador (RED); recuperación conserva vista y permite otra comprobación sin POST automático, GREEN.
21. @s1: reintentar tras fallo de comprobación volvía a montar sesión antigua (RED); retirar sesión antes de permitir recuperación, GREEN.
22. @s16: rutas no pertenecientes a la aplicación se conservaban tras login (tres RED); allowlist local y descarte de parámetros ajenos, GREEN. La prueba no depende de un redirect externo real.
23. @s9: Headers nativo con token previo lo combinaba con el nuevo (`old, current`, RED); `Headers.set` sustituye de forma estándar, GREEN.
24. @s14: logout resuelto tras desmontar publicaba y consultaba otra sesión (RED); operación cancelable y guard de respuesta GREEN.
25. @s14: resultado de login cancelado borraba contraseña del flujo nuevo (RED); finally respeta cancelación, GREEN.
26. @s6: logout 401 no comprobaba sesión y atrapaba al usuario en retry (dos RED); GET real muestra formulario cuando anónimo y conserva incertidumbre/reintento si no se confirma, GREEN.
27. @s14: parseo CSRF tardío alteraba login/logout tras revocar (dos RED); guard tras await conserva el flujo vigente, GREEN.
28. @s14: contraseña permanecía hasta terminar GET posterior al login (RED); limpiar al recibir respuesta, GREEN.
29. @s19: error de login dejaba foco perdido cuando el botón se deshabilitaba (RED); restaura contexto y respeta foco movido, GREEN.

Se añadieron regresiones de rutas válidas, tipos/estados HTTP de sesión, almacenamiento, abortos y rechazo de origen. Pasaron sobre la implementación existente; se documentan como regresiones, no como RED inventado. El conjunto nuevo tiene 65 ejemplos.

## Verificación y corte

Primer intento completo: 240 verdes y una expectativa histórica fallida porque ahora creación pasa AbortSignal. Se actualizó la expectativa para exigir esa señal; no se debilitó la comparación restante. Ejecución 28726: **241/241 pruebas en 9 archivos**, lint verde. Build de las mismas fuentes verde. Fuentes congeladas y entregadas a integración para reconstrucción final. Stryker focal 62167 instrumenta 355 mutantes; resultado todavía pendiente.

## Trazabilidad del contrato

| Escenario | Evidencia frontend y frontera restante |
| --- | --- |
| @s1 | Gate previo a datos; validación exacta, tipos/HTTP incompatibles, StrictMode y recuperación sin datos antiguos. |
| @s2 | Formulario CSRF, POST 204 y GET confirmado; E2E real acredita cookie y acceso. |
| @s3 | HTTP 401 genérico, contraseña borrada y campos recuperables. Las tres credenciales concretas son responsabilidad backend/E2E. |
| @s4 | Retirada Basic acreditada por backend e integración; frontend no envía Authorization. |
| @s5 | Persistencia de sesión tras reinicio: backend/integración, no simulada como evidencia frontend. |
| @s6 | 401 en creación, lectura y edición retira vistas; logout 401 recupera acceso. Expiración temporal real: integración. |
| @s7 | Logout POST, retirada inmediata, respuesta confirmada y token/cookie vía transporte real en E2E. |
| @s8 | Frontend usa botón POST; rechazo de GET logout corresponde a backend. |
| @s9 | Token sólo para escrituras, sustitución de Headers sin duplicados y recuperación de login/logout. |
| @s10 | Cliente notifica 401 y gate desmonta; precedencia servidor 401 frente a CSRF: backend. |
| @s11 | Problemas UNTRUSTED_ORIGIN no se confunden con CSRF. Guard de origen real: backend/integración. |
| @s12 | Login/logout 503 y red sin éxito ficticio; logout oculto permite reintento. Fallo JDBC real: backend. |
| @s13 | Prueba de no escritura en Storage; token en memoria, contraseña borrada, ningún secreto en URL. Atributos de cookie: backend/E2E. |
| @s14 | Etiquetas/autocomplete, espera inmediata/doble envío, contraseña borrada, cancelación GET/POST y cuerpos tardíos. |
| @s15 | BroadcastChannel sin secretos, receptor retira antes de GET, visibilidad y cleanup. Dos pestañas reales: integración. |
| @s16 | Rutas locales de lista/cursor/detalle/edición preservadas; rutas ajenas descartadas. |
| @s17 | Renovación deliberada sin POST automático; borrador conservado incluso si el GET de renovación falla. |
| @s18 | 176 pruebas previas conservadas, cliente compartido en todos los transportes. Integración recorre login/crear/leer/editar/estado con CSRF generado por frontend. |
| @s19 | Foco al entrar/salir/error, sin robar otro control; matriz de 30 principios, 22 anchos y zoom real: `progress/ux_authentication.md` de integración. No se atribuye prueba en dispositivos físicos. |

Verificación raíz independiente 22240: lint verde, 384 pruebas backend y 241 frontend verdes. La raíz revisó el alcance de mutación, incluidos foco y guard privado del gate. El autor inspeccionó `outputs/authentication-desktop.png` y `outputs/authentication-mobile.png`: tarjeta, marca, etiquetas y acción legibles, espaciado consistente y sin recortes visibles. El contorno del encabezado corresponde al foco; no se eliminó. Esta revisión visual de autor complementa la evidencia independiente de integración y no sustituye dispositivos físicos.

La raíz confirma el corte integrado congelado: 27/27 E2E, smoke del publicador EXIT 0 y juez backend APPROVED. No se vuelve a ejecutar la suite global durante la mutación; el único pendiente frontend es su resultado y la clasificación de supervivientes.

## Entrega final frontend

Mutación completa 62167: **302/355 (85,07 %)**, EXIT 0, 53 supervivientes y 0 sin cobertura/timeouts/errores. La revisión añadió 19 regresiones y reforzó aserciones existentes, sin modificar producción. Ejecución 79967: **260/260 pruebas completas y lint verdes**. Replay 14627: **79/79**, EXIT 0, que detecta 28 supervivientes originales. Replay adicional 94937: **1/1**, EXIT 0, que detecta el error transitorio de primer commit; su baseline ejecutó los 73 casos de autenticación actuales. Lint final verde tras ese refuerzo.

Quedan 24 supervivientes equivalentes para el árbol actual, clasificados individualmente en `progress/mutation_authentication_frontend.md` y revisados por la raíz. No se presenta una suma de replays como score global. Los archivos de fuentes, tests y configuración quedan liberados para cierre por la raíz; el agente frontend no modifica estados globales ni realiza commits. No quedan procesos propios activos.
