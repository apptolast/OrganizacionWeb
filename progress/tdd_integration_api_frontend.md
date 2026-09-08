# TDD frontend 24

Contrato A155A48E aprobado por root. Baseline corregido d987c29, init0 con frontend2424/Node70 ejecutados y Java UP-TO-DATE; no se repite global mientras otros agentes trabajan en rojo. Fuentes frontend propias; no Java, COMMON/V14 ni cambios de producto23.

Cada ciclo añade un único test y ejecuta la suite focal `pnpm --dir frontend exec vitest run src/integration-api-client.test.ts`. Logs originales `progress/integration24_frontend_NNN_red.log` y `_green.log`. Un fallo de Vitest hace que pnpm imprima además un mensaje genérico Command not found; los logs contienen el fallo de test real anterior, no un problema de instalación.

| Ciclo | Contrato y conducta | RED | GREEN |
| --- | --- | --- | --- |
| 001 | s1/s33 PUT estable, JSON y CSRF existentes sin Authorization | módulo aún ausente, EXIT1 | 1/1 EXIT0 |
| 002 | s1 sobre cerrado, rechaza campo inesperado | promesa resuelta, EXIT1 | 2/2 EXIT0 |
| 003 | s1/s10 identidad de confirmación ligada al intento | acepta otra id, EXIT1 | 3/3 EXIT0 |
| 004 | s17 HTTP503 no confirma aunque body parezca éxito | promesa resuelta, EXIT1 | 4/4 EXIT0 |
| 005 | s9 replay200 metadatos y secret:null, rechaza secreto nuevo | acepta secreto en replay, EXIT1 | 5/5 EXIT0 |
| 006 | s1 secreto201 con32bytes canónicos ligado a id | acepta null/formas incompatibles, EXIT1 | 6/6 EXIT0 |
| 007 | s37 señal retirada antes de enviar | envía fetch, EXIT1 | 7/7 EXIT0 |
| 008 | s37 cancelación durante JSON impide entregar secreto | resuelve confirmación, EXIT1 | 8/8 EXIT0 |
| 009 | s37 cancelación al resolver fetch evita decodificar | llama json, EXIT1 | 9/9 EXIT0 |

Corte en elaboración: falta cerrar metadatos/intención, GET/list/revoke y UI. Estos ciclos no acreditan todavía integración de logout, estado global ni UX. No campaña de mutación ni commit propio.

Segundo tramo y primer corte revisable (sin UI todavía):

| Ciclo | Conducta | RED → GREEN |
| --- | --- | --- |
| 010 | s1/s5 metadatos cerrados tipados, scopes canónicos y fechas coherentes | rechazos ausentes → 10/10 |
| 011 | s1/s5 confirmación corresponde al nombre/scopes/plazo enviado y creación no revocada | contradicción aceptada → 11/11 |
| 012 | s33 intención sólo owner/id, recuperable sin secreto/formulario | módulo ausente → 1/1 intención |
| 013 | s35/s37 intención ajena/malformada/id no canónica retirada | parse/retirada ausentes → 2/2 intención |
| 014 | s3 strip Unicode White_Space igual backend, NBSP/NEL | trim JS rechazaba intención válida → 12/12 cliente |
| 015 | s34 fallo silencioso de storage no conserva identidad | no lanzaba error → 3/3 intención |
| 016 | s38 limpieza sólo clave propia | función ausente → conjunta 16/16, 2 suites |

Todos los RED anteriores tienen EXIT1 y GREEN EXIT0 originales bajo el mismo prefijo de logs. Tras ciclo016, Prettier sólo cuatro archivos propios, ESLint focal y tsc --noEmit EXIT0 (salida3aa75d); ninguna fuente existente cambió. La intención pura no acredita aún bloqueo de botón, logout ni consumidor tolerante a fallo de removeItem. Pendientes explícitos del siguiente tramo: validación de id antes de formar URL, Location201, GET/list/revoke, UI/estado y recuperación. El corte es parcial, no cliente completo ni cierre24.
| Ciclo | Conducta y resultado focal |
| --- | --- |
| 017 | s33 UI nominal crea intención antes PUT y muestra secreto tras201; RED módulo ausente → GREEN1/1 UI |
| 018 | s34 UI storage inaccesible informa y no envía; RED sin alerta/rechazo no tratado → GREEN2/2 UI |
| 019 | s5 revisión root: fracción omitida o3dígitos válida; RED rechazo excesivo → GREEN13/13 cliente, instant/microseconds reutilizados |
| 020 | s9 revisión root: revokedAt anterior por reloj retrocedido; RED guarda no contratada → GREEN14/14 cliente |
| 021 | s12/s35 GET por id sin escritura; RED export ausente → GREEN15/15 |
| 022 | s35 preserva404 para recuperación; RED error reemplazado → GREEN16/16 |
| 023 | s12 respuesta de otraid no resuelve intención; RED aceptada → GREEN17/17 |
| 024 | s37 GET cancelado antes/despuésfetch/JSON; RED aceptado → GREEN18/18 |
| 025 | s2 identidad URL canónica antes de request; RED fetch con id inválida → GREEN19/19 |
| 026 | s15/s16 PUT revocación vacío con CSRF; RED export ausente → GREEN20/20 |
| 027 | s15/s16 confirmaciónrevocación conserva campos/fecha repetida; RED contradicción aceptada → GREEN21/21 |
| 028 | s17/s37 revocación conserva errores/abort; RED confirmación503 → GREEN22/22 |
| 029 | s13 listado cursor opaco URLencoded sinowner; RED export ausente → GREEN23/23 |
| 030 | s12/s13 sobre/lista cerrados sin secretos/duplicados/más50; RED acepta sobre → GREEN24/24 |
| 031 | s13/s37 listado errores/abort; RED confirma503 → GREEN25/25 |
| 032 | s37 apiRequest existente no retira siguiente sesión por401tardíoabortado; inicialmente GREEN26/26, sin fabricación de RED |

Corte cliente ampliado:4archivos cliente/intención listos para revisión, vista nominal sigueWIPdisjunta. JSON de body/metadata cerrado, micros exactos y sin orden temporal no contratado en revocación. Location no se usa para navegar ni recuperar: identidad sigue siendo la id propia; comprobaciónHTTPdeLocation pertenece al contrato backend. Tipos señalaron dos opciones `exact` deTestingLibrary inválidas en testUI; retiradas conservando nombre exacto por defecto. Tipos despuésEXIT0. Suite conjunta registrada en integration24_frontend_client_final.log. No API global, Java ni gates generales.
Tramo UI en curso, ciclos 033–048. Cada ciclo conserva RED EXIT1 y GREEN EXIT0 en logs del prefijo; desde042 el comando añade filtro por nombre de test, por lo que no se presenta como suite completa.

- 033/s35: bloqueo durante PUT y resultado incierto, clave conservada.
- 034/s35: reload y404 mantienen id; comprobación sólo manual.
- 035/s37: cambio owner aborta creación y retira secreto tardío.
- 036/s35: GET confirmado sin secreto resuelve intención y explica pérdida.
- 037/s38: fallo removeItem conserva confirmación sin clasificarla incierta.
- 038/s36: reenvío deliberado tras404 con mismaid, replay200secret:null.
- 039/s2/s3: nombre inválido o scopes vacíos no permiten enviar.
- 040/s36: cupo409 coherente permite corregir/reintentar mismaid.
- 041/s36: validación400 coherente conserva borrador para corregir.
- 042/s35: nuevo resultado incierto retira permiso de corrección anterior.
- 043/s13/s41: listado GET perezoso al entrar, metadatos sin secreto.
- 044/s41: fallo de listado recuperable manualmente sin borrar borrador.
- 045/s13: segunda página sólo a petición explícita;50primeros conservados.
- 046/s16: revocación requiere confirmación separada con nombre/consecuencia.
- 047/s40: revocación incierta se consulta antes de repetir PUT deliberado.
- 048/s39: clipboard sólo por gesto; fallo ofrece selección manual.

Al introducir el listado, la suite UI usa un fixture propio exclusivamente para GET exacto de colección sin query; devuelve página vacía y delega métodos/rutas restantes al mock original de cada flujo. Sus conteos anteriores son tráfico de creación/recuperación, no una afirmación de ausencia de GET de listado. Los tests de listado ejercitan fetch directamente, incluida paginación. No setupglobal ni sustitución de Provider/apiRequest. Aún pendientes integraciónApp/sesión, otros bordes públicos, SCSS/UX y freeze final; no UI completa ni aceptación publicada.
Continuación de integración y privacidad, ciclos 049–056. Los comandos focales conservan sus omisiones explícitas; no son regresiones globales.

- 049/s37: lectura tardía retirada al cambiar propietario. Inicialmente GREEN; la limpieza pasiva existente ya satisfacía ese primer oráculo.
- 050/s37: un probe de layout demuestra que la lectura anterior debe estar abortada antes del layout de la identidad nueva. RED y GREEN reales al retirar también el listado en la limpieza de layout.
- 051/s41: ruta privada directa, login real de SessionGate y enlace final de navegación. RED y GREEN al integrar App, Workspace e isPrivateRoute.
- 052/s37: logout real limpia la intención antes de recibir su respuesta. RED y GREEN; se reutiliza la retirada de recuperación existente, con errores de almacenamiento contenidos.
- 053/s37: establecer otro propietario fuera de la ruta de integraciones retira la intención ajena sin consultar credenciales. RED y GREEN.
- 054/s40: revocación incierta de Primera, cambio a Segunda y regreso a Primera. RED porque Segunda heredaba el bloqueo; GREEN con incertidumbre en memoria asociada a cada id. No se descarta al cerrar el panel ni se reenvía automáticamente.
- 055/s38: almacenamiento de recuperación ilegible no derriba la vista; el listado sigue disponible y la creación queda bloqueada. RED y GREEN.
- 056/s37: cerrar el panel retira el secreto de la vista y conserva los metadatos, sin persistirlo ni realizar otra solicitud. RED y GREEN.

Siguen pendientes el cierre de mensajes, foco y estilos, la matriz UX y el freeze funcional. La integración de ruta y sesión ya dispone de oráculos públicos; no se presenta todavía como alcance completo.

- 057/s41: cerrar la acción enfocada del secreto recupera el foco en H1. RED y GREEN; reutiliza captura del iniciador y retirada por focusin voluntario del patrón de importación.
- 058/s41: una respuesta de creación no roba el foco movido voluntariamente a Nombre. Inicialmente GREEN con el mecanismo anterior.
- 059/s13: dos creaciones consecutivas conservan ambas filas y no muestran un listado vacío. RED y GREEN; aceptar una confirmación actualiza el listado y aborta cualquier lectura anterior pendiente.
- 060/s36: un conflicto coherente explica la diferencia de intención y exige consultar la misma id. RED y GREEN. Un intento intermedio no aplicó la edición por diferencia de saltos de línea y siguió fallando; queda conservado en 060_failed_edit.log, sin atribuirlo a un defecto adicional.

El texto aclara que reenviar conserva identidad y compara nombre, permisos y caducidad; un borrador distinto puede provocar 409. No se añade persistencia del borrador. SCSS reutiliza los tokens de temas, objetivos de 44 px y ajuste de palabras; la verificación visual todavía no se ha ejecutado. El refactor de carga usa callbacks de la promesa para no actualizar estado síncrono desde el efecto inicial; los reintentos manuales controlan su indicador y no se añaden consultas automáticas. La caducidad visible se calcula con la hora observada al consultar, sin llamar al reloj durante render.

Validación del corte actual: integration24_frontend_ui_current.log, 60/60 en tres suites; integration24_frontend_ui_lint_final.log EXIT0. Tipos se verifican de nuevo después del refactor en el archivo propio de evidencia. No campaña de mutación ni regresión global.

Revisión independiente del primer freeze y deltas:

- 061/s13: creación antes del GET inicial retenido; RED sin acceso al resto del listado, GREEN con recarga manual siempre disponible. No GET automático ni escritura repetida.
- 062/s13: una página posterior a recuperación por id no duplica filas ni reemplaza metadatos confirmados. RED y GREEN con deduplicación por id al anexar páginas.
- 063/s37: cerrar secreto1 mientras creación2 está en vuelo aborta el contexto, conserva intención2 incierta y no revela su respuesta tardía. RED y GREEN. La finalización de la petición retirada no libera una petición posterior.
- 064/s37: una resolución del cliente seguida por retirada de owner antes de la continuación UI borraba la intención nueva. RED y GREEN reproducidos mediante frontera de promesa controlada; la UI vuelve a comprobar aborto antes de efectos compartidos. Se conserva además la guarda equivalente en lectura y revocación.
- 065/s41: error de Nombre asociado por aria-invalid y aria-describedby, retirado al corregir. RED y GREEN.
- 066/s41: el oráculo de listado pasa de ISO crudo a fecha/hora UTC legible con dateTime original intacto. RED y GREEN; no cambia el instante durable.

Primer navegador original: integration24_browser_initial.log, 3/3 motores, API simulada explícita, dos temas y tres anchos, capturas y geometría bajo .e2e-work/integration-browser-initial. Ese corte precede a 061–066; no se atribuye a los deltas siguientes ni al backend real. El juicio visual y la matriz completa continúan pendientes.

- 067/s13: la recuperación observa caducidad con su propia hora, no con la carga inicial. RED y GREEN, con mensaje explícito de estado observado y sin polling.

Cierre focal tras revisión: integration24_frontend_review_final.log contiene 66/66 en tres suites. El lint señaló que la declaración del hook de hora debía preceder a su uso en el helper; se reordenó sin alterar lógica ni oráculos. integration24_frontend_final_lint.log EXIT0. El build actualizado queda registrado en integration24_frontend_final_build.log.

Navegador de recuperación original sobre Vite de desarrollo: 1/3; Chromium/WebKit observaron dos GET iniciales con StrictMode, aunque todos los pasos de foco y recuperación pasaron. Se conserva integration24_browser_recovery_initial.log. El mismo test sin relajar el conteo, sobre build de producción y preview 5180, pasa en los tres motores. integration24_browser_production.log acredita 6/6 nominal/recuperación, con 18 geometrías, seis ejecuciones axe sin violaciones y foco físico real. No acredita backend real. La matriz ampliada se ejecutará sobre el corte final.

- 068/s2: precisión de texto Unicode acordada con backend: unidades surrogate aisladas altas y bajas bloqueadas; emoji completo permitido. RED y GREEN. Guarda Cs con flag Unicode conserva pares válidos; no altera el límite de 80 puntos de código.

El test histórico del freeze 33D925 está preservado byte a byte en integration24_reviewed_ui_test.snapshot.txt (SHA C111D2910FCB3F771425326654E278B3CF379FD87E626440CBDCE22FF9472C38); el test actual añade el ciclo 068. El delta Unicode tiene manifiesto propio integration24_frontend_unicode_freeze.json y build/lint propios; no se presenta el log 66/66 como 67/67. La matriz de texto 200 %, movimiento reducido y colores forzados pasa 3/3 sobre el build funcional anterior, sin modificación de CSS/DOM por Unicode. Color-contrast de axe sólo se desactiva para colores nativos forzados, limitación declarada; los temas normales conservan la comprobación.

## Gates antes de mutación

Global original: 2488/2491, tres oráculos heredados de posición del menú (App, Apariencia y Exportación), con 192 entradas before/after sin diferencias. Root autorizó únicamente desplazar posiciones conservando Hoy primero y los oráculos de contenido/ruta/tráfico. Las tres suites focales pasan 88/88; sus originales se conservan.

El auditor señaló la variante s40 con revokedAt presente. Ciclo 069 inicialmente GREEN: comprobar manualmente confirma revocación sin segundo PUT, elimina controles de recuperación y devuelve foco al H1. No cambió producción.

Global final: 2492/2492, 65 suites; lint (incluido formato), tipos/build y arnés Node 72/72 verdes. Los 192 inputs frontend/harness coinciden antes/después. Logs global_original/global_final, exits y manifests están bajo progress/integration24_frontend_*. La campaña nueva mantiene tres módulos completos, siete nodos AST, umbral 80, ocho workers y perTest. Los cinco literales históricos de Apariencia se sincronizaron por AST con RED/ GREEN focal Node conservados; no se relaja deepEqual ni startsWith.
