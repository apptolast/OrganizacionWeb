## Feature 25-webhooks

23 hallazgos confirmados, de los que **8 son bloqueantes**. 6 se cierran en minutos.

### 1. [BLOQUEANTE · varias horas] @s25: las filas TIMEOUT y TLS no tienen oráculo; el único test que las nombra es un eco de sí mismo

- **Dimension:** contrato
- **Rutas:** features/webhooks.feature:327,329; backend/src/test/java/com/apptolast/organization/domain/WebhookAttemptTest.java:66-79; backend/src/test/java/com/apptolast/organization/adapter/webhook/JdkWebhookSenderTest.java:143-208; backend/src/main/java/com/apptolast/organization/adapter/webhook/JdkWebhookSender.java:68-70,86-87

**Evidencia.** El outline tiene ocho filas. JdkWebhookSenderTest ejerce contra un receptor real cinco: 404, 500, 302, puerto cerrado (CONNECTION), host que no resuelve (DNS) y dirección bloqueada. No hay ninguna prueba de «acepta la conexión y no responde» ni de «presenta un certificado no confiable». Lo único que menciona esas dos clases es WebhookAttemptTest:66-79, `@ValueSource(strings = {"TIMEOUT", "CONNECTION", "TLS", "DNS", "BLOCKED_ADDRESS"})` sobre `WebhookAttempt.transport(errorClass, 5)`: pasa la cadena y comprueba que la recupera. Mientras tanto la producción sí tiene esas ramas: JdkWebhookSender.java:68-70 `catch (HttpTimeoutException) -> transport("TIMEOUT", ...)` y :70,:86-87 `SSLException -> "TLS"`. La bitácora (línea 226) declara «11 tests verdes contra un receptor real. Cubre @s15 @s16 @s25 @s26».

**Arreglo.** En JdkWebhookSenderTest añadir dos casos con receptor real: (a) un handler que duerme más que EXCHANGE_TIMEOUT (o inyectar un plazo corto por constructor para no tardar 10 s en la suite) y afirmar errorClass TIMEOUT con httpStatus null; (b) un HttpsServer con certificado autofirmado y un cliente sin ese trust anchor, afirmando errorClass TLS. Con eso las ocho filas del outline quedan ejercidas contra el adaptador.

**Correccion del verificador.** TÍTULO CORREGIDO: @s25: dos de las ocho filas del outline (TIMEOUT y TLS) no tienen oráculo en ninguna capa, y con ellas queda sin ejercer la enmienda de seguridad B1.

RUTAS: features/webhooks.feature:322-331 (filas 327 y 329); backend/src/test/java/com/apptolast/organization/adapter/webhook/JdkWebhookSenderTest.java:143-208; backend/src/main/java/com/apptolast/organization/adapter/webhook/JdkWebhookSender.java:68-71 y :83-90; progress/security_review_connectors.md:35; progress/proposal_webhooks.md:66; progress/tdd_webhooks.md:226.

EVIDENCIA (comprobada leyendo, no citada de bitácora):
1. El outline @s25 declara ocho filas. JdkWebhookSenderTest ejercita contra un receptor real SEIS: 404 y 500 (:143-152), 302 sin seguir y sin llamar al destino (:154-176), puerto cerrado -> CONNECTION (:178-188), host que no resuelve -> DNS (:190-197), dirección bloqueada -> BLOCKED_ADDRESS (:199-208). El fichero tiene 209 líneas, 8 métodos y 11 casos contando parametrizaciones: la cifra «11 tests» de la bitácora sí es correcta.
2. Las dos filas que faltan son 327 («acepta la conexión y no responde») y 329 («presenta un certificado no confiable»). No están cubiertas en ninguna otra capa: `grep -rn "TIMEOUT|SSLException|HttpTimeoutException|TLS" backend/src/test` devuelve, para webhooks, una única línea (WebhookAttemptTest.java:67); `transport(` solo se invoca en tests en WebhookAttemptTest.java:69; DispatchWebhooksTest no menciona timeout; e2e/webhooks-ux.spec.mjs (420 líneas) tampoco.
3. Producción sí tiene esas ramas y nadie las ejecuta: catch directo de HttpTimeoutException -> "TIMEOUT" (:68-69), catch directo de SSLException -> "TLS" (:70-71), y además classify() (:83-90) recorre la cadena de causas devolviendo "DNS"/"TLS"/"TIMEOUT" y "CONNECTION" por defecto. Sin ninguna prueba, nada decide si el fallo llega envuelto en IOException (camino classify) o desnudo (catch directo): una de las dos rutas es muy probablemente código muerto, y eso lo pagará la campaña de mutación.
4. Agravante que el barrido no recogió: la fila 327 es exactamente la que cita el hallazgo de seguridad B1 (alta) en progress/security_review_connectors.md:35 («un receptor que gotee un byte cada 4 s ... agota el pool y deja la aplicación en 503»). La remediación está en el código (HttpRequest.timeout de 10 s, JdkWebhookSender:31 y :101, documentada en el javadoc :22-23) y no tiene ni una prueba. Peor: el contrato quedó desincronizado —la fila dice «no responde en 5 s» sobre una conexión YA ACEPTADA, donde los 5 s de CONNECT_TIMEOUT no aplican y el corte real son 10 s—, así que ejecutar la fila tal como está escrita fallaría. Hay que arreglar la fila o el código, y solo una prueba lo revela.
5. progress/proposal_webhooks.md:66 planificaba explícitamente «lectura lenta hasta timeout» en el plan de pruebas del worker. No se entregó, y progress/tdd_webhooks.md:226 declara «11 tests verdes contra un receptor real. Cubre @s15 @s16 @s25 @s26». La declaración de cobertura de @s25 es inexacta: cubre 6/8 filas.

CORRECCIÓN IMPORTANTE AL BLOQUEANTE ORIGINAL, que estaba mal en dos puntos:
- NO es cierto que WebhookAttemptTest:66-79 sea un placebo ni «un eco de sí mismo». Además de recuperar la cadena, afirma que httpStatus es null, que succeeded() es false, y —esto es lo que el revisor omitió— registra el intento sobre la entrega y afirma que queda status "pending", httpStatus null y nextAttemptAt = T+1min. Discrimina contra cambios concretos del producto: que WebhookAttempt.transport() ponga 0 en lugar de null en httpStatus; que succeeded() pase a mirar httpStatus en vez de errorClass; que WebhookDelivery.recorded() trate un fallo sin httpStatus como succeeded o exhausted; que el primer escalón del backoff deje de ser 1 min. Es un test de dominio legítimo. Su límite es otro: no comprueba la CLASIFICACIÓN del adaptador, es decir, que una condición real de red produzca esa cadena. Ese es el hueco, no el placebo.
- El barrido dice «cinco» filas ejercitadas y a continuación enumera seis. Son seis de ocho.

GRAVEDAD: bloqueante, sostenido. Dos filas de un Scenario Outline aprobado por la puerta humana sin ningún oráculo es el mismo patrón que este juez rechazó hoy en el zoom nativo al 200 %; y la fila 327 arrastra además una contradicción viva entre contrato y código, y la única evidencia de que la enmienda B1 funciona.

QUÉ LO CIERRA (dos tests en JdkWebhookSenderTest, ninguno necesita infraestructura nueva):
- TIMEOUT: un com.sun.net.httpserver.HttpServer que acepte la conexión y no llame a sendResponseHeaders; afirmar errorClass "TIMEOUT", httpStatus null y latencyMs >= 0. Para que no cueste 10 s de suite, extraer CONNECT_TIMEOUT/EXCHANGE_TIMEOUT a parámetros del constructor (o a constantes package-private) e inyectar un plazo corto en la prueba, dejando un test de cableado que afirme 5 s y 10 s en la configuración de producción —el mismo patrón que ya usa HttpCalendarFeed.TIMEOUT con ExternalCalendarWiringTest:104.
- TLS: un com.sun.net.httpserver.HttpsServer sobre 127.0.0.1 con un keystore autofirmado generado en el propio test (o cargado de src/test/resources) y el HttpClient por defecto, que no lo confía; afirmar errorClass "TLS" y no "CONNECTION". Este es justamente el test que distingue el catch directo de :70-71 de la rama de classify() en :86.
- Y de paso: alinear features/webhooks.feature:327 con el plazo real de la enmienda B1 (intercambio de 10 s), o justificar por qué se conserva «5 s».

### 2. [BLOQUEANTE · varias horas] El oráculo de recorte de @s42 mide el recorte, lo escribe en disco y nunca lo asserta

- **Dimension:** oraculos
- **Rutas:** e2e/webhooks-ux.spec.mjs:174-191 y :218-221; frontend/src/webhooks.scss:20-23; frontend/src/webhooks.tsx:285-289

**Evidencia.** El bloque calcula por elemento `overflowing: element.scrollWidth > element.clientWidth + 1` y arma `offenders`, pero la única aserción de anchura es `expect(measured.scroll, `${state}:${width} horizontal page overflow; offenders=${JSON.stringify(measured.offenders)}`).toBeLessThanOrEqual(width);` — `offenders` aparece SOLO dentro del mensaje de fallo. En la hoja: `li span, .webhook-secret input { overflow-wrap: anywhere; }` y en la vista `<input readOnly value={secret} …>`.

**Arreglo.** Asertar el recorte, no solo escribirlo: para los elementos que el contrato nombra (el span de la URL, el input del secreto y las celdas `td[role=cell]` del panel de entregas) exigir `scrollWidth <= clientWidth + 1` en los cuatro anchos, dejando fuera explícitamente el `thead` oculto a propósito (por clip-path) con una lista blanca justificada. Si el secreto no cabe a 320 px, la respuesta es de producto (textarea que envuelve, o campo multilínea de solo lectura), no relajar el oráculo.

**Correccion del verificador.** TÍTULO CORREGIDO: @s42 exige «ni recorte de la URL, del secreto ni de la tabla»; el recorte de la URL y del secreto no tiene ninguna aserción (el de la tabla sí).

RUTAS: e2e/webhooks-ux.spec.mjs:174-190 (construcción de `offenders`), :218-221 (única aserción de anchura, con `offenders` sólo en el mensaje); frontend/src/webhooks.scss:19-22; frontend/src/webhooks.tsx:285-289.

EVIDENCIA VERIFICADA: la cadena `offenders` aparece 3 veces en el fichero (174, 195, 220) y ninguna es un `expect`. No existe aserción por elemento de `scrollWidth`/`clientWidth`/`scrollHeight` en ningún test de la feature 25 (los unitarios son jsdom, sin layout, y `webhooks-ux.spec.mjs` es el único E2E de webhooks). `overflow-wrap: anywhere` aplicado a `.webhook-secret input` es una regla sin efecto: un `<input>` de una línea no envuelve.

LO QUE SÍ ESTÁ CUBIERTO (no repetir como hallazgo): el recorte/desbordamiento de la tabla de entregas está cubierto por las aserciones de geometría por control de :226-236 sobre los botones «Reenviar» de cada fila (`main button`); volver a un contenedor con `overflow-x` rompe la prueba, y así se detectó el defecto original a 768 px.

MUTACIÓN QUE DEMUESTRA EL HUECO: añadir `white-space:nowrap; overflow:hidden; text-overflow:clip` a `li span` o a `.webhook-secret input`. `documentElement.scrollWidth` no cambia, las cajas de los controles siguen dentro del viewport y con >=44 px, axe no informa nada: la suite queda verde con la URL y el secreto truncados.

CIERRE EXIGIDO (una de estas dos, no la que proponía el bloqueante original):
(1) Aserción por elemento sobre un conjunto NOMBRADO, no sobre `body *`: para el `<span>` de la URL de cada `li` y para el `<input>` del secreto, comprobar en los cuatro anchos y en `text200` que `scrollWidth <= clientWidth + 1` Y `scrollHeight <= clientHeight + 1` (ambas dimensiones: el juez ya rechazó hoy un oráculo que sólo miraba la horizontal). Nunca `expect(offenders).toEqual([])` sobre `body *`: el `thead` a <900 px es visually-hidden legítimo (`width:1px; overflow:hidden; clip-path: inset(50%)`) y haría fallar código correcto; si se quiere usar la lista, hay que excluir explícitamente los elementos sr-only y justificar el filtro.
(2) O bien cambiar el producto para que el secreto no dependa de un campo de una línea (p. ej. mostrarlo en un elemento que envuelve, o un `<textarea readOnly>` de varias líneas) y entonces asertar el reflow del texto completo; y retirar de la hoja la regla muerta `overflow-wrap: anywhere` sobre `.webhook-secret input`.

NOTAS DE CALIBRACIÓN PARA EL JUEZ: no se ha comprobado que el producto esté hoy roto. El secreto en el campo al 100 % de 320 px muestra ~34 de 49 glifos, pero el valor es seleccionable (`onFocus` hace `select()`), desplazable y copiable con el botón «Copiar»: eso es contenido alcanzable, no pérdida. El bloqueante es de COBERTURA DE ORÁCULO, no de defecto observado.

### 3. [BLOQUEANTE · varias horas] El zoom NATIVO del navegador al 200 % no se ejecuta nunca, aunque el contrato lo nombra

- **Dimension:** accesibilidad
- **Rutas:** features/webhooks.feature:518; progress/ux_webhooks.md:33-35; e2e/webhooks-ux.spec.mjs (fichero completo, 6 tests); e2e/github-connector-native-zoom.spec.mjs:37; e2e/automations-ux.spec.mjs:201

**Evidencia.** El contrato dice: «se recorre la vista con teclado en 320, 768, 1280 y 1440 px CSS, con texto al 200 % y zoom al 200 %, en ambos temas» (webhooks.feature:518) — texto y zoom son dos cosas distintas y se piden las dos. La matriz lo admite: «**No se ejecutó zoom nativo del navegador**: la comprobación de 200 % es por tamaño de fuente computado y reflow» (ux_webhooks.md:33-34). He leído los 6 tests de e2e/webhooks-ux.spec.mjs: los modos son ['text200','forced-colors','reduced-motion'] más light/dark; no hay `chromium.launchPersistentContext`, ni extensión, ni `chrome.tabs.setZoom`, ni `devicePixelRatio` en todo el fichero. El repositorio ya tiene el mecanismo resuelto cuatro veces (github-connector-native-zoom, reschedule-native-zoom, start-work-session-native-zoom, today-native-zoom) e incluso uno con API simulada como esta (automations-ux.spec.mjs:201, «zoom nativo de Chromium al 200 % sobre 320 px CSS»).

**Arreglo.** Añadir `e2e/webhooks-native-zoom.spec.mjs` calcado de e2e/automations-ux.spec.mjs:201-299 (extensión efímera MV3 con permiso `tabs`, `launchPersistentContext` con `viewport: null`, `chrome.tabs.setZoom(tab.id, 2)`, `expect(zoom).toBe(2)` y `expect.poll(devicePixelRatio).toBe(base*2)`, y `chrome.windows.update` con `width*2 + cromo` para llegar a 320/768/1440 px CSS reales), recorriendo al menos los estados secreto, lista y entregas con `page.route` y reutilizando las mismas aserciones de geometría y axe. Corregir la guarda del precedente: `test.skip(info.project.name !== "" && info.project.name !== "chromium")`, nunca `=== "chromium"` a secas, porque playwright.config.mjs no declara `projects` y `name` es cadena vacía.

**Correccion del verificador.** BLOQUEANTE (confirmado, con la justificación corregida): @s42 exige «texto al 200 % **y** zoom al 200 %» (features/webhooks.feature:518) y sólo se ejecuta el primero.

Comprobado leyendo, no citado de bitácora: e2e/webhooks-ux.spec.mjs es el único fichero E2E que toca /webhooks (grep -rln en e2e/); sus 6 tests salen de 2 temas (línea 331) + 3 modos ['text200','forced-colors','reduced-motion'] (344) + 1 de teclado (372); no contiene setZoom, Emulation., newCDPSession, launchPersistentContext, deviceScaleFactor ni devicePixelRatio; playwright.config no aporta zoom por configuración; los unitarios corren en jsdom. progress/ux_webhooks.md:32-35 lo admite por escrito.

Por qué bloquea, con la física bien dicha (la versión del hallazgo original está equivocada aquí y no debe repetirse): bajo zoom nativo los paddings, bordes y `min-height: 44px` NO cambian de valor en píxeles CSS. Lo que cambia es que el viewport CSS se reduce a la mitad en **ambos** ejes y que cambian el redondeo subpíxel, el ancho de la barra de desplazamiento y la rasterización de imágenes. De ahí salen dos huecos concretos que la auditoría actual no puede ver:
1. El eje vertical no se prueba a escala reducida: todos los anchos se miden con `height: 900` fijo (bucle de WIDTHS, ~línea 160), luego el reflujo de la tabla de entregas y del panel del secreto con ~400 px CSS de alto —justo lo que da un zoom del 200 % en una ventana normal— nunca se ejerce.
2. El modo `text200` duplica `node.style.fontSize` elemento a elemento (127-149): es una comprobación legítima de WCAG 1.4.4 (redimensionado de texto), pero no es 1.4.10 (reflujo). No reduce el viewport CSS, no pasa por la ruta del compositor y no interactúa con la barra de desplazamiento.

Remedio, ya resuelto cuatro veces en este repositorio: añadir e2e/webhooks-native-zoom.spec.mjs calcado de e2e/github-connector-native-zoom.spec.mjs (extensión efímera con `permissions: ["tabs"]` + `chrome.tabs.setZoom` bajo `chromium.launchPersistentContext`), con el `test.skip` de motor de e2e/automations-ux.spec.mjs:201, cubriendo al menos 320 px CSS al 200 % sobre los estados lista y entregas, con el oráculo de recorte midiendo scroll y desbordamiento en horizontal **y** vertical. Mientras no exista, @s42 está incumplido en su literal y la feature 25 no puede pasar a `done`.

### 4. [BLOQUEANTE · varias horas] El oráculo de teclado es el umbral «hubo más de cinco»: ni orden, ni foco visible aserto, ni Shift+Tab, ni más de un estado y un ancho

- **Dimension:** accesibilidad
- **Rutas:** e2e/webhooks-ux.spec.mjs:372-420 (asserts en 405-406); .e2e-work/webhooks-ux/keyboard/tab-order.json; features/webhooks.feature:517-519; e2e/github-connector.spec.mjs:261 y 294

**Evidencia.** Las únicas aserciones del recorrido son `expect(order.length).toBeGreaterThan(5)` (línea 405) y que cada nombre no sea cadena vacía (línea 406). El campo `visibleFocus` se calcula en las líneas 396-400 y solo se vuelca a `tab-order.json`: no hay ningún `expect` sobre él, de modo que la prueba pasaría igual con el foco invisible en los veinte controles. Y su cálculo tampoco serviría: `getComputedStyle(active, ":focus-visible")` pasa una pseudo-CLASE donde la API espera un pseudo-ELEMENTO, así que devuelve el estilo normal. El comentario de la línea 384 dice «in the order it appears in the DOM», pero no existe ninguna comparación contra el orden del DOM ni contra el conjunto de controles enfocables. No aparece `Shift+Tab` en ningún punto del fichero. Además el test corre con `items: [endpoint()]` (línea 378), es decir solo el estado `lista`, en el viewport por defecto 1280x720 (playwright.config.mjs no fija `viewport`), sin texto al 200 % ni temas. La propia evidencia delata el umbral: `tab-order.json` tiene 40 entradas que son los mismos 20 controles repetidos dos veces, porque el bucle de 60 tabulaciones da la vuelta y nadie lo comprueba.

**Arreglo.** Reescribir el test: (1) construir en la página la lista de controles enfocables de `main` y exigir igualdad exacta —conjunto y orden— con la secuencia recorrida con Tab, deteniendo el bucle al volver al primer control en lugar de tabular 60 veces a ciegas; (2) aseverar el foco visible con una medida real, como en e2e/github-connector.spec.mjs:294 (comparar `outline`/`box-shadow` computados con el elemento sin foco, o capturar la caja antes y después), nunca con un segundo argumento `":focus-visible"`; (3) recorrer con Shift+Tab en sentido inverso y comprobar la secuencia espejo; (4) ejecutar el recorrido en los cinco estados del contrato (llamarlo desde `walkStates`) y al menos a 320 y 1440 px, y en modo `text200`.

**Correccion del verificador.** BLOQUEANTE (redacción corregida y acotada)

Título: El recorrido de teclado de @s42 no tiene oráculo ni para «orden lógico» ni para «foco visible»; ambas cláusulas están en el contrato y no se comprueban en ningún fichero.

Rutas: e2e/webhooks-ux.spec.mjs:383-406 (aserciones en 405-406; visibleFocus muerto en 397-399); features/webhooks.feature:519; progress/ux_webhooks.md:79; frontend/src/webhooks.scss:57; precedente correcto en e2e/github-connector.spec.mjs:261-291 y 294 y ss.

Hechos:
1. features/webhooks.feature:519 exige que los controles «se alcanzan con Tab en orden lógico, tienen nombre accesible, foco visible y objetivo de al menos 44 px».
2. Nombre accesible, 44 px y ausencia de recorte/scroll SÍ están cubiertos, y bien: los cinco tests de auditoría del mismo fichero recorren los siete estados en 320/768/1280/1440 px, en claro, oscuro, texto al 200 %, forced-colors y reduced-motion, con expects de geometría y axe a cero. Eso no se discute.
3. «Orden lógico»: el bucle de 60 tabulaciones (385-403) recoge el orden pero no lo compara con nada. La única aserción es `order.length > 5`. La evidencia lo delata: tab-order.json tiene 40 entradas que son los mismos 20 controles dos veces, porque el bucle da la vuelta y nadie lo comprueba. Reordenar el DOM de la vista, o dar la vuelta al formulario, deja la prueba en verde.
4. «Foco visible»: `visibleFocus` se calcula (397-399) y solo se escribe a disco; no hay ni un `expect` sobre él en todo el repositorio. Borrar `&:focus-visible { outline: 2px solid var(--accent) }` de webhooks.scss:57 —o añadir `outline: none`— deja verdes los 6 E2E, los unitarios de la vista y axe (que no tiene regla de anillo de foco; webhooks.test.tsx corre en jsdom y no computa :focus-visible). La cláusula no tiene oráculo alguno.
5. Agravante documental: progress/ux_webhooks.md:79 declara «el orden de Tab sigue al DOM — Verificado en navegador (tab-order.json)». El test no verifica eso. Es una fila de la matriz UX que afirma más de lo que el código comprueba.

Arreglo mínimo, ya resuelto en este repositorio para otra feature: replicar github-connector.spec.mjs:261-291 (construir `expectedOrder` con los enfocables visibles de main y exigir `expect(reached).toEqual(expectedOrder)`) y :294 y ss. (medir el anillo con el foco llegado POR TECLADO, no con `element.focus()`, y asertarlo control a control). Basta hacerlo sobre los estados con controles propios —formulario, secreto, lista y panel de entregas— para cerrar la cláusula; no hace falta multiplicarlo por los cuatro anchos ni por las modalidades, que ya están cubiertos por los otros cinco tests.

Fuera de este bloqueante (no exigir): Shift+Tab no aparece en el contrato; el `getComputedStyle(active, ":focus-visible")` de la línea 399 es un no-op —pseudo-clase donde se espera pseudo-elemento— pero el primer disyuntor sí mide bien, así que la fórmula sirve en cuanto se aserte; y la línea 406 y el retorno de foco al cancelar el borrado (409-419) son oráculos reales, no placebo.

### 5. [BLOQUEANTE · varias horas] El cierre del rebinding DNS que el spec de record exige no está implementado, y el código afirma que sí

- **Dimension:** seguridad
- **Rutas:** backend/src/main/java/com/apptolast/organization/adapter/webhook/JdkWebhookSender.java:25-27, :92-97, :99-110 (contra project-spec.md:2492 y progress/tdd_webhooks.md:36-39)

**Evidencia.** project-spec.md:2492 dice literalmente: «Deja de aceptarse como límite el reenlace de nombres entre la comprobación y el uso: se resuelve el nombre una vez, se validan todas las direcciones devueltas y se conecta contra la dirección literal ya validada, conservando el nombre original en la cabecera Host y en la indicación de servidor de TLS.» El código hace solo la mitad: guardDestination (:92-97) resuelve con resolver.resolve(host) y valida, y acto seguido request() (:99-110) construye HttpRequest.newBuilder(URI.create(url)) —el nombre, no la IP— sobre un HttpClient creado en :49-53 sin resolutor propio, que vuelve a resolver por su cuenta. No hay cabecera Host, no hay SNIServerName, no hay conexión contra literal. Y el javadoc de la propia clase, :25-27, asegura «the host is resolved once and every returned address is checked before connecting, so no name can be re-pointed between the check and the use», que es falso. La bitácora tdd_webhooks.md:36-39 repite la afirmación: «se conecta contra la IP literal ya validada, conservando el nombre original en la cabecera Host y en SNI. Deja de ser un límite aceptado.» La salida de emergencia que el propio hallazgo B3 preveía tampoco está: progress/security_review_connectors.md:40 dice «Si se mantiene el límite, declarar la política de egreso como requisito de despliegue en deploy/» y deploy/ solo contiene compose.publisher.yml, nginx.conf y web.Dockerfile, sin ninguna restricción de egreso.

**Arreglo.** Una de dos, sin término medio. (a) Implementarlo: resolver una vez, validar, y construir la petición contra la IP literal validada, fijando el nombre original en la cabecera Host —requiere jdk.httpclient.allowRestrictedHeaders=host— y en SNI vía SSLParameters con SNIHostName en el HttpClient; añadir un test que demuestre que un resolutor que devuelve pública en la comprobación y 10.0.0.7 después no llega a conectar. (b) Si se mantiene como límite aceptado, retirar la afirmación falsa del javadoc JdkWebhookSender:25-27, corregir project-spec.md:2492 y progress/tdd_webhooks.md:36-39 para que digan lo que el código hace, y entregar la barrera de egreso en deploy/ que B3 exige como condición de aceptar el límite.

**Correccion del verificador.** TÍTULO CORREGIDO: La mitad de anclaje del control de rebinding (B3) no está implementada, y dos artefactos —el javadoc de producción y el registro de contrato— la dan por implementada.

RUTAS: backend/src/main/java/com/apptolast/organization/adapter/webhook/JdkWebhookSender.java:25-27 (javadoc), :49-53 (cliente sin resolutor), :92-97 (guardia), :99-110 (petición por nombre); project-spec.md:2492.

QUÉ ESTÁ Y QUÉ NO (verificado leyendo, no citado). Implementado y probado: se resuelve el nombre una vez, se validan TODAS las direcciones devueltas y se corta antes de conectar si alguna está bloqueada (JdkWebhookSenderTest:200, s25_b3_anAddressBlockedAtSendTimeStopsTheRequestBeforeConnecting). Eso cubre la fila SSRF de @s25 en features/webhooks.feature:331. NO implementado: conectar contra la dirección literal ya validada conservando el nombre en la cabecera Host y en SNI. request() (:99-110) pasa la URL con el nombre a un HttpClient (:49-53) que resuelve otra vez por su cuenta. Grep global del repositorio: cero apariciones de SNIHostName, sslParameters, jdk.httpclient.allowRestrictedHeaders o networkaddress.cache.ttl. No hay segundo emisor: ApplicationConfiguration:509-514 cablea este y solo este.

QUÉ IMPIDE CERRAR, exactamente. No es el riesgo SSRF: con https obligatorio y sin seguir redirecciones (Redirect.NEVER), un destino interno reenlazado tendría que presentar un certificado válido para el nombre del atacante, y además la caché DNS del JVM (30 s por defecto) hace que las dos resoluciones coincidan en la práctica —mitigación real pero incidental, no configurada ni documentada en ningún sitio. Lo que impide cerrar son dos afirmaciones falsas que sobreviven al cierre: (a) el javadoc de :25-27 concluye «so no name can be re-pointed between the check and the use», que no se sigue de lo que hace el código y es justo lo que leerá el siguiente revisor para decidir que esta zona ya está mirada; (b) project-spec.md:2492, que es el registro del contrato y manda sobre el .feature por decisión del propio coordinador, declara «Deja de aceptarse como límite el reenlace de nombres entre la comprobación y el uso», cuando se sigue aceptando.

CORRECCIÓN A LA EVIDENCIA DEL HALLAZGO ORIGINAL: retirar la acusación a la bitácora. tdd_webhooks.md:36-39 está bajo «## Enmiendas del contrato» (:13) y transcribe la exigencia de B3, no un cierre; y :202-209 declara expresamente «conexión contra la IP ya validada conservando Host/SNI (B3)» como pendiente y no cubierta por ningún test. La bitácora es honesta sobre el código; su único defecto es no arrastrar esa mitad abierta al «Estado final» (:368-374).

GRAVEDAD: bloqueante, pero por integridad de las afirmaciones, no por el riesgo. Es barato de cerrar y NO exige escribir el anclaje: en el JDK HttpClient, conectar contra literal conservando Host exige -Djdk.httpclient.allowRestrictedHeaders=host y rompe la verificación de nombre del certificado, así que implementarlo tal cual lo redacta 2492 empeoraría el TLS. Cierre mínimo aceptable, sin tocar el emisor: (1) reescribir el javadoc :25-27 para que afirme solo lo que hace —resolución única, validación de todas las direcciones, corte antes de conectar— y nombre el reenlace posterior como límite residual; (2) reconciliar project-spec.md:2492 y docs/webhooks.md devolviendo el reenlace a límite aceptado con su mitigación; y (3) ejecutar la salida de emergencia que el propio B3 preveía (progress/security_review_connectors.md:40): declarar la política de egreso como requisito de despliegue en deploy/, o en su defecto fijar networkaddress.cache.ttl explícitamente y documentar por qué eso acota la ventana. Cualquiera de esas tres formas cierra el bloqueante; dejar el javadoc como está, no.

### 6. [BLOQUEANTE · una hora] No existe ambito PIT `webhooks`: los 38 ficheros de produccion del backend no reciben ni un mutante

- **Dimension:** mutacion
- **Rutas:** backend/build.gradle.kts:34-612 (declaraciones de scope 37-64; `targetClasses.set(when {...})` 512-541; rama `else` 540)

**Evidencia.** El bloque `pitest` declara 28 variables de scope (`val icsCalendarOnly = scope == "ics_calendar"` ... `val startWorkSessionReplayOnly = ...`, lineas 38-64) y NINGUNA es `webhooks`. `grep -rn -i webhook backend/build.gradle.kts` devuelve exactamente dos lineas, ambas de la feature 30: `"com.apptolast.organization.domain.NotifyWebhookAction*"` (linea 495) y `"com.apptolast.organization.application.WebhookEndpoint*"` (linea 506), ambas dentro de `automationsClasses`. La segunda solo alcanza `WebhookEndpointLookup`, `WebhookEndpointNotFoundException` (de la feature 30) y la interfaz `WebhookEndpoints` (sin mutantes por ser interfaz). La rama `else` de la linea 540 suma 27 conjuntos y no incluye ninguna clase Webhook* de la feature 25. Contado con `find backend/src/main/java -iname "*ebhook*.java"`: 41 ficheros, 38 de la feature 25.

**Arreglo.** Anadir en backend/build.gradle.kts `val webhooksOnly = scope == "webhooks"`, un `webhooksClasses` y las ramas correspondientes en `targetClasses`, `targetTests` y `reportDir`. Nombrar las clases explicitamente: un glob ingenuo tipo `*.Webhook*` NO alcanza `CreateWebhook*`, `ManageWebhook*`, `DispatchWebhooks*`, `EnqueueWebhookDeliveries*`, `AesGcmWebhookSecrets`, `JdkWebhookSender`, `PostgresWebhook*` ni `Slf4jWebhookAudit`. Conjunto verificado fichero a fichero contra el arbol actual: `com.apptolast.organization.domain.Webhook*` (WebhookAttempt, WebhookCursor, WebhookDelivery, WebhookEndpoint, WebhookIntent, WebhookInvalidException, WebhookPingPayload), `application.CreateWebhook*`, `application.ManageWebhook*`, `application.DispatchWebhooks*`, `application.EnqueueWebhookDeliveries*`, `application.Webhook*` (Audit, Creation, Deliveries, DestinationGuard, Endpoints, OperationException, Outbox, Secrets, Sender, Work), `adapter.webhook.*` (AesGcmWebhookSecrets, JdkWebhookSender, WebhookSignature), `adapter.http.Webhook*` (Controller, DeliveryView, EndpointView), `adapter.persistence.PostgresWebhook*` (Store, Work, Outbox), `adapter.logging.Slf4jWebhookAudit*`, `adapter.config.Webhook*` (Configuration, ConnectorStartup, Schedule). Comprobar que cada patron casa con un .java existente antes de lanzar la campana. Y anadir `if (webhooksOnly) reportDir.set(layout.buildDirectory.dir("reports/pitest-webhooks"))`.

**Correccion del verificador.** TITULO CORREGIDO: Falta el ambito PIT `webhooks` prometido por el contrato: 13 clases de adapter de la feature 25 -- incluidas AesGcmWebhookSecrets y WebhookSignature -- quedan sin un solo mutante, y no hay forma de producir una medicion acotada a la feature.

RUTAS: backend/build.gradle.kts:34-64 (28 declaraciones de scope, ninguna `webhooks`), :65 (`val core`), :512-541 (`targetClasses.set(when {...})`, rama `else` en :540); scripts/project.mjs (sin target `webhooks`); progress/proposal_webhooks.md:66.

EVIDENCIA VERIFICADA POR LECTURA (nada ejecutado):
1) `grep -rn -i webhook backend/build.gradle.kts` devuelve exactamente 2 lineas, ambas dentro de `automationsClasses` (feature 30): :495 `domain.NotifyWebhookAction*` y :506 `application.WebhookEndpoint*`. Esta parte de la alegacion original es correcta.
2) PERO backend/build.gradle.kts:65 define `val core = setOf("com.apptolast.organization.domain.*", "com.apptolast.organization.application.*")`, y la rama `else` de :540 es `core + ...`. Esos dos globs cubren TODO domain y TODO application. La afirmacion original de que el `else` "no incluye ninguna clase Webhook* de la feature 25" es FALSA.
3) Reparto real de los 41 ficheros (`find backend/src/main/java -iname "*ebhook*"` da 42 entradas, una es el directorio adapter/webhook):
   - domain (8, CUBIERTOS por `core`): NotifyWebhookAction, WebhookAttempt, WebhookCursor, WebhookDelivery, WebhookEndpoint, WebhookIntent, WebhookInvalidException, WebhookPingPayload.
   - application (20, CUBIERTOS por `core`): CreateWebhook, CreateWebhookUseCase, DispatchWebhooks, DispatchWebhooksUseCase, EnqueueWebhookDeliveries, EnqueueWebhookDeliveriesUseCase, ManageWebhook, ManageWebhookUseCase, WebhookAudit, WebhookCreation, WebhookDeliveries, WebhookDestinationGuard, WebhookEndpointLookup, WebhookEndpointNotFoundException, WebhookEndpoints, WebhookOperationException, WebhookOutbox, WebhookSecrets, WebhookSender, WebhookWork.
   - adapter (13, SIN CUBRIR en ningun conjunto): config/WebhookConfiguration, config/WebhookConnectorStartup, config/WebhookSchedule, http/WebhookController, http/WebhookDeliveryView, http/WebhookEndpointView, logging/Slf4jWebhookAudit, persistence/PostgresWebhookOutbox, persistence/PostgresWebhookStore, persistence/PostgresWebhookWork, webhook/AesGcmWebhookSecrets, webhook/JdkWebhookSender, webhook/WebhookSignature.
4) `grep -n -i webhook scripts/project.mjs` no devuelve NINGUNA linea: no existe target `webhooks-backend` ni `webhooks-frontend`. Un `mutate` con target no reconocido cae al mapa generico (:191-196, `mutate: "pitest"`), es decir a la campana completa por defecto.
5) frontend/stryker.webhooks.config.json NO existe. No hay progress/mutation_webhooks*.md.
6) progress/proposal_webhooks.md:66 promete literalmente: "Mutacion: PIT scope `webhooks` (dominio, casos de uso, controlador, `PostgresWebhookStore`, `WebhookDeliveryClient`, `ApplicationConfiguration`) y `frontend/stryker.webhooks.config.json`, umbral 80 % sin rebajar". Compromiso del contrato aprobado, incumplido.

POR QUE BLOQUEA (version acotada):
(a) Cobertura cero real. Las 13 clases de adapter no reciben mutante en ningun modo de ejecucion. Incluyen la logica criptografica delicada del contrato: AesGcmWebhookSecrets (AES-GCM con el id del endpoint como AAD, @s8) y WebhookSignature (HMAC-SHA256 sobre t y cuerpo, @s15/@s31), mas JdkWebhookSender (3xx sin seguir, timeouts), PostgresWebhookStore/Work/Outbox (cursor por tupla, SKIP LOCKED, poda a 50, cascada, aislamiento por propietario) y WebhookController. Es exactamente el mismo defecto que el AES-GCM bendecido sin mutantes detectado hoy.
(b) Imposibilidad de medir la puerta. Aunque los 28 ficheros de domain/application si reciben mutantes, solo lo hacen dentro de la campana por defecto, cuyo `mutationThreshold.set(80)` (:610) es un agregado de TODO el backend. No se puede producir hoy una cifra >= 0.8 (harness.config.json:22) atribuible a la feature 25, que es lo que exige `require_mutation_to_close`.

CIERRE MINIMO ACEPTABLE: anadir `val webhooksOnly = scope == "webhooks"`, un `webhooksClasses` que incluya los tipos de dominio, los casos de uso Y las 13 clases de adapter (o al menos AesGcmWebhookSecrets, WebhookSignature, JdkWebhookSender, PostgresWebhookStore/Work/Outbox, WebhookController, WebhookConfiguration), su rama en `targetClasses`/`targetTests`, su `reportDir`, el target `webhooks-backend` en scripts/project.mjs, y frontend/stryker.webhooks.config.json. Luego ejecutar y adjuntar progress/mutation_webhooks_backend.md.

QUE NO ALEGAR: no decir "38 ficheros sin un solo mutante" ni listar DispatchWebhooksUseCase, EnqueueWebhookDeliveriesUseCase, CreateWebhookUseCase, ManageWebhookUseCase, WebhookDestinationGuard ni los tipos de dominio como fuera de cobertura -- todos entran por `core` (:65). La gravedad correcta sigue siendo bloqueante, pero por la capa adapter y por la imposibilidad de medicion acotada, no por una ausencia total de mutantes.

### 7. [BLOQUEANTE · minutos] scripts/project.mjs no admite `webhooks-backend` ni `webhooks-frontend`: el arnes rechaza la invocacion

- **Dimension:** mutacion
- **Rutas:** scripts/project.mjs:32-80 (allowlist de targets) y :90-240 (despacho de `mutate`)

**Evidencia.** La allowlist termina en `"automations-backend", "automations-frontend",` (lineas 78-79) seguida de `].includes(target)) { throw new Error(`Invalid target: ${target}`); }` (lineas 80-83). `grep -n -i webhook scripts/project.mjs` devuelve `No matches found`. Tampoco hay ninguna rama `if (task === "mutate" && target === "webhooks-backend")` equivalente a la de automations (lineas 101-103: `backend("pitest", ["-PmutationScope=automations"])`).

**Arreglo.** Anadir `"webhooks-backend"` y `"webhooks-frontend"` a la allowlist de scripts/project.mjs y las dos ramas de despacho, copiando la forma de automations (lineas 90-103): `backend("pitest", ["-PmutationScope=webhooks"])` y `runner("pnpm", ["--dir","frontend","exec","stryker","run","stryker.webhooks.config.json"])`. Anadir ademas la guarda por contenido correspondiente en scripts/project.test.mjs, como tienen los demas carriles.

**Correccion del verificador.** Título corregido: «La puerta de mutación de la feature 25 no está cableada en ninguna de las tres capas del arnés, y no existe evidencia de mutación»

Gravedad: bloqueante (calibración correcta), pero el alcance del arreglo es mayor que el descrito: parchear solo la allowlist de scripts/project.mjs NO haría ejecutable la puerta.

Comprobado leyendo (HEAD de main, sin ejecutar nada):
1. scripts/project.mjs — allowlist en las líneas 34-80, 43 targets, ninguno con «webhooks»; `throw new Error(\`Invalid target: ${target}\`)` en la 82. No hay rama `if (task === "mutate" && target === "webhooks-backend"/"webhooks-frontend")` análoga a las de automations (90-104). `git log --all -S"webhooks" -- scripts/project.mjs`: cero commits. Corrección menor a la evidencia alegada: el despacho de `mutate` se extiende hasta la línea ~418, no «:90-240».
2. backend/build.gradle.kts — no existe `val webhooksOnly = scope == "webhooks"` (los alcances viven en 38-64) ni un `webhooksClasses`; el `when` de targetClasses (512-540) caería al `else`, que es la unión de la cuarentena de alcances. Hay que crear el conjunto de clases (domain WebhookEndpoint/Delivery/Attempt/Cursor/Intent/PingPayload/InvalidException, application CreateWebhook*, ManageWebhook*, DispatchWebhooks*, EnqueueWebhookDeliveries*, WebhookSender, WebhookOutbox, WebhookSecrets, WebhookDestinationGuard, WebhookAudit, WebhookDeliveries, WebhookWork, adapter.http.WebhookController + vistas, y los adaptadores Postgres/firma), más sus ramas en targetTests (541-570) y en reportDir.
3. frontend — no existe stryker.webhooks.config.json (los 43 configs no mencionan webhooks) y el `mutate` de stryker.config.json no lista src/webhooks.tsx, src/webhooks-client.ts ni src/webhooks-route. Es decir, hoy ningún comando del repositorio muta una sola línea del frontend de webhooks, ni siquiera la ejecución completa.

Consecuencia real (no solo «el comando aborta»): no hay progress/mutation_webhooks.md y progress/tdd_webhooks.md no menciona la mutación; la puerta que harness.config.json exige para cerrar (`require_mutation_to_close`, umbral 0.8) no se ha ejecutado nunca sobre la feature 25. El cierre a `done` es imposible hasta que se añadan las tres piezas y se corra la puerta.

Matiz que hay que dejar escrito para no exagerar: `bin/harness mutate` sin target no aborta y su rama `else` de pitest incluye `core` (domain.* + application.*), de modo que el dominio y los casos de uso de webhooks sí entrarían en la campaña completa; pero esa campaña está documentada en progress/current.md:127-134 como ~13 h dentro de una ventana de 4, cancelada por timeout las tres noches, y no cubre ningún adaptador de webhooks ni el frontend. Y `mutate automations-backend` muta de refilón `application.WebhookEndpoint*` (build.gradle.kts:506). Ninguna de las dos cosas es la puerta de la feature 25.

Hallazgo adicional advisory (no bloqueante) del mismo descuido de registro: la lista de `lint` de scripts/project.mjs (420-432) incluye e2e/automations-ux.spec.mjs y los specs del conector GitHub, pero no e2e/webhooks-ux.spec.mjs, que por tanto nunca pasa por `node --check`.

### 8. [BLOQUEANTE · una hora] No existe frontend/stryker.webhooks.config.json: la vista y el cliente HTTP quedan sin puerta, y la navegacion de @s36 fuera de cualquier ambito

- **Dimension:** mutacion
- **Rutas:** frontend/ (44 ficheros stryker.*.json, ninguno de webhooks); frontend/src/webhooks.tsx, frontend/src/webhooks-client.ts; frontend/src/App.tsx:43,57-58,86-87; frontend/src/workspace.tsx:21-22,122-127

**Evidencia.** El listado de frontend/ contiene stryker.automations.config.json, stryker.external-calendar.config.json, stryker.integration-api.config.json, etc., pero no stryker.webhooks.config.json. `grep -l -i webhook stryker.*.json stryker.config.json` no devuelve ningun fichero, es decir ni siquiera de rebote otro carril mutaria `src/webhooks.tsx`. Existen sin embargo `src/webhooks.tsx`, `src/webhooks-client.ts` y tres suites (`webhooks.test.tsx`, `webhooks-client.test.ts`, `webhooks-route.test.tsx`). La ruta y la entrada de navegacion que exige @s36 ("la ruta es /webhooks, el h1 es Webhooks", "la entrada de navegacion situada tras API para integraciones") viven en ficheros compartidos: App.tsx:43 `const webhooks = route === "/webhooks";`, App.tsx:57-58 y App.tsx:86-87 `) : webhooks && username ? (<Webhooks owner={username} />`, y workspace.tsx:122-127 con `href="/webhooks"` y `aria-current={section === "Webhooks" ? "page" : undefined}`.

**Arreglo.** Crear frontend/stryker.webhooks.config.json calcado de stryker.automations.config.json (testRunner vitest, thresholds break 80, tempDirName .stryker-tmp-webhooks, reports/mutation-webhooks/) con `mutate`: `src/webhooks.tsx`, `src/webhooks-client.ts` y los rangos de los ficheros compartidos, calculados sobre el arbol actual: la asignacion de ruta de App.tsx:43, el brazo de seccion App.tsx:57-58, el brazo de render App.tsx:86-87 y el enlace de navegacion workspace.tsx:122-127. Verificar los rangos abriendo los ficheros tras escribirlos (la union de las features 24-30 ya ha desplazado estas lineas hoy) y anadir la guarda por contenido en scripts/project.test.mjs. Nota: `src/webhooks.scss` no se mutila, es correcto dejarlo fuera.

**Correccion del verificador.** Título correcto: «La feature 25 no tiene puerta de mutación en ninguno de los dos lados: ni configuración Stryker para la vista y el cliente HTTP, ni scope pitest para el backend, ni destino en el arnés».

Lo comprobado leyendo (no citado de bitácora):

1) FRONTEND, sin puerta — CIERTO. `ls frontend/stryker*.json` da 43 ficheros (42 por feature + `stryker.config.json`), ninguno de webhooks. El `mutate` de `frontend/stryker.config.json` tiene 54 entradas `src/...` y `grep -c webhook` sobre él devuelve 0: ni `src/webhooks.tsx` ni `src/webhooks-client.ts` reciben mutantes de rebote. Existen los tres suites (`webhooks.test.tsx`, `webhooks-client.test.ts`, `webhooks-route.test.tsx`) sin gate que las mida.

2) LA PARTE DE NAVEGACIÓN DEL BLOQUEANTE ES FALSA. El revisor no miró `frontend/stryker.config.json`: su array `mutate` incluye `"src/App.tsx"` y `"src/workspace.tsx"` como FICHEROS ENTEROS (no rangos), y `frontend/package.json` declara `"mutate": "stryker run"`, que usa esa configuración base por defecto. Por tanto App.tsx:43, 57-58, 86-87 y workspace.tsx:122-127 SÍ reciben mutantes bajo la configuración base. Los rangos `linea:columna` de `stryker.ics-calendar.config.json` y `stryker.integration-api.config.json` son una optimización de tiempo de ejecución para acotar el carril, no la única vía de ámbito. Eliminar de la redacción «la navegación de @s36 fuera de cualquier ámbito» y «sin esos rangos... no recibe mutantes»; sustituir por: conviene añadir los rangos de App.tsx y workspace.tsx al nuevo config por convención y por velocidad, pero su ausencia no deja la navegación sin mutar.

3) EL BLOQUEANTE SE QUEDA CORTO: EL BACKEND TAMPOCO TIENE PUERTA. `grep -i webhook backend/build.gradle.kts` sólo da dos líneas, 495 (`domain.NotifyWebhookAction*`) y 506 (`application.WebhookEndpoint*`), y ambas están DENTRO de `automationsClasses`, es decir el scope de la feature 30, no de la 25. No existe ningún `webhooksOnly`/`-PmutationScope=webhooks`. Quedan sin scope pitest, entre otras: `adapter.http.WebhookController`, `adapter.persistence.PostgresWebhookStore`/`PostgresWebhookOutbox`/`PostgresWebhookWork`, `adapter.webhook.JdkWebhookSender`, `adapter.webhook.AesGcmWebhookSecrets`, `adapter.webhook.WebhookSignature`, `application.CreateWebhook`, `application.DispatchWebhooks`, `application.EnqueueWebhookDeliveries`, `application.ManageWebhook`. Es decir, los 35 escenarios de backend (@s1–@s35), incluidos firma HMAC, cifrado del secreto y política de direcciones SSRF, están hoy sin mutación.

4) Cifras a corregir en el texto original: son 43 ficheros `stryker*.json`, no 44. Y @s36–@s42 son SIETE etiquetas de escenario (cuatro de ellas Scenario Outline: 4+1+3+6+1+4+1 = 20 filas de ejemplo concretas), no «once escenarios».

Trabajo mínimo para levantar el bloqueante:
 a) crear `frontend/stryker.webhooks.config.json` con `src/webhooks.tsx`, `src/webhooks-client.ts` y, por convención, los rangos de `src/App.tsx` (43, 57-58, 86-87) y `src/workspace.tsx` (21-22, 122-127), con `tempDirName`/`jsonReporter`/`htmlReporter` propios como hacen ics-calendar e integration-api;
 b) añadir un scope `webhooks` en `backend/build.gradle.kts` con las clases listadas en el punto 3;
 c) registrar `webhooks-frontend` y `webhooks-backend` en la allowlist y en el dispatch de `scripts/project.mjs`, que hoy los rechaza con `Invalid target`;
 d) ejecutar ambas campañas y dejar `progress/mutation_webhooks.md`, que no existe.

Nota de contexto verificada: `progress/current.md:236` ya registra «Features 25, 28 y 30: sin juez y sin mutación», así que esto no es un descubrimiento nuevo sino una puerta abierta y conocida; sigue impidiendo el `done`.

### 9. [ALTA · una hora] @s22: la fila de las dos copias no existe, y ninguna de las dos filas comprueba el registro final

- **Dimension:** contrato
- **Rutas:** features/webhooks.feature:286-292; backend/src/test/java/com/apptolast/organization/adapter/persistence/WebhookWorkPersistenceTest.java:77-91; progress/tdd_webhooks.md:234

**Evidencia.** El outline tiene dos filas: «antes de abrir la conexión | 1» y «después de que el receptor respondiera 200 y antes de confirmar la transacción | 2». El único test de recuperación es `s22_aLeasedDeliveryIsNotClaimedAgainUntilItsLeaseExpires`, que reclama, comprueba que el arrendamiento la oculta y que a los 10 minutos vuelve a ser reclamable con `attempt() == 0`. No hay receptor en ese test, así que no se cuenta ninguna petición; y no se ejecuta ningún intento, así que tampoco se comprueba el «Then» común a las dos filas: «el registro final es succeeded con attempt 1». La fila de las 2 copias no aparece en ningún fichero. La bitácora del ciclo 12 declara «Cubre `@s22` `@s23`»; en cambio la lista honesta de la sesión 1 (líneas 297-298) sí lo reconoce: «@s22 fila «muere después de que el receptor respondiera 200» (2 copias): cubierta la recuperación por arrendamiento, no el conteo de copias».

**Arreglo.** Extender WebhookWorkPersistenceTest con un caso que use el RecordingSender del test hermano: reclamar, enviar (el receptor cuenta 1), NO llamar a `record` (simula la muerte), dejar vencer el arrendamiento, reclamar de nuevo, enviar (el receptor cuenta 2) y ahora sí `record`; afirmar `count(*) == 1` sobre webhook_deliveries y status succeeded con attempt 1. El mismo test con `record` en el primer paso cubre la fila de 1 copia.

**Correccion del verificador.** TITULO: @s22: ninguna de las dos filas ejecuta el reinicio de extremo a extremo y el conteo de copias no se observa nunca.

GRAVEDAD: media (no alta).

RUTAS: features/webhooks.feature:281-292; backend/src/test/java/com/apptolast/organization/adapter/persistence/WebhookWorkPersistenceTest.java:78-91; progress/tdd_webhooks.md:234 frente a 297-298.

EVIDENCIA (comprobada leyendo): el outline @s22 tiene dos filas que solo se diferencian en lo que ve el receptor (1 o 2 peticiones). El unico test que toca la recuperacion es s22_aLeasedDeliveryIsNotClaimedAgainUntilItsLeaseExpires (:78-91): reclama, comprueba que el arrendamiento la oculta a las 12:00 y que a las 12:10 vuelve a ser reclamable con attempt() == 0. No hay receptor y no se llama a record(), asi que (a) no se cuenta ninguna peticion y (b) la entrega recuperada nunca llega a liquidarse. Los otros dos tests etiquetados s22 en ese fichero (:62 entrega de body y secreto descifrado, :120 endpoint desactivado) no son de reinicio. La fila de las 2 copias no aparece en ningun fichero del repositorio; PublisherCrashProcess/OutboxRecoveryTest son de la outbox del broker, no de webhooks. La bitacora del ciclo 12 (linea 234) declara «Cubre @s22», mientras la lista honesta de la sesion 1 (297-298) reconoce «cubierta la recuperacion por arrendamiento, no el conteo de copias»; ninguna sesion posterior lo cierra.

QUE NO ES CIERTO DEL BLOQUEANTE ORIGINAL (no repetirlo ante el juez): el «Then» comun si tiene oraculo en otros ficheros. «Registro final succeeded con attempt 1» esta afirmado sobre el esquema real en WebhookRecoveryPersistenceTest s28 y s31 y a nivel de caso de uso en DispatchWebhooksTest s20. «Un unico registro de entrega» esta afirmado en WebhookWorkPersistenceTest:168 y :186 y en s31 («a redelivery adds no row to the log»), y ademas PostgresWebhookWork.record es un UPDATE por id: una regresion que duplicase la fila o que reenviase con attempt 2 muere hoy con los tests existentes. La linea 286 (mismo body y mismo X-OrganizationWeb-Event-Id) tambien esta cubierta (:62-74 y s31).

RIESGO RESIDUAL REAL: nadie demuestra que una entrega recuperada tras el reinicio llegue efectivamente a enviarse y a liquidarse (la cadena claim -> lease lapsa -> reclaim -> send -> record no se recorre entera en ningun test), ni que el receptor vea exactamente 1 o 2 copias. Una regresion en la que la entrega recuperada se reclama pero ya no es enviable, o en la que el arrendamiento no se limpia al liquidar, pasaria desapercibida.

CIERRE PROPUESTO (barato, la maquinaria ya existe): en WebhookRecoveryPersistenceTest, que ya tiene RecordingSender y drain(), dos tests: (1) fila 1 — encolar, reclamar y no llamar a record (muerte antes de abrir la conexion), avanzar el reloj mas alla de los 5 min de LEASE, drenar y afirmar sender.sent.size() == 1, un unico registro para ese endpoint y estado succeeded con attempt 1; (2) fila 2 — enviar con el RecordingSender y descartar el resultado sin llamar a record (muerte tras el 200 y antes de confirmar), avanzar el reloj, drenar y afirmar sender.sent.size() == 2 con el mismo eventId en ambas, un unico registro y succeeded con attempt 1.

### 10. [ALTA · varias horas] @s23: sólo se prueba la reclamación; ni el receptor ni el desenlace de las diez entregas tienen oráculo

- **Dimension:** contrato
- **Rutas:** features/webhooks.feature:296-299; backend/src/test/java/com/apptolast/organization/adapter/persistence/WebhookWorkPersistenceTest.java:128-144

**Evidencia.** El contrato exige tres cosas: «el receptor recibe exactamente 10 peticiones, una por eventId», «ninguna instancia espera al bloqueo de fila de la otra» y «las 10 entregas quedan succeeded con attempt 1». El test `s23_twoWorkersNeverClaimTheSameDeliveryAndNeitherWaitsForTheOther` lanza dos hilos que sólo llaman a `claimedFor(...)` y afirma `assertEquals(10, all.size())` y `assertEquals(10, Set.copyOf(all).size())`. No hay receptor, no se envía nada, no se llama a `record` y ninguna aserción mira el status ni el attempt de las filas. Tampoco hay medición ni aserción alguna sobre la espera al bloqueo (el receptor de 500 ms del Given no existe en el test).

**Arreglo.** Reutilizar el patrón `drain` de WebhookRecoveryPersistenceTest: dos hilos que reclamen, envíen a un receptor compartido que tarde y responda 200, y liquiden con `record`. Afirmar 10 peticiones con 10 eventId distintos y que las 10 filas queden succeeded con attempt 1. Para la no-espera, basta con un tiempo total muy por debajo de 10 x 500 ms, o comprobar que ambos hilos reclamaron al menos una entrega.

**Correccion del verificador.** TITULO CORREGIDO: @s23: el test de concurrencia no discrimina el SKIP LOCKED; ni el receptor lento de 500 ms ni el desenlace se ejercitan en el escenario concurrente.

RUTAS: features/webhooks.feature:294-299; backend/src/test/java/com/apptolast/organization/adapter/persistence/WebhookWorkPersistenceTest.java:128-144; backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresWebhookWork.java:50-80 (la clausula FOR UPDATE OF d SKIP LOCKED esta en la linea 68).

EVIDENCIA (comprobada leyendo, no citada de bitacora): s23_twoWorkersNeverClaimTheSameDeliveryAndNeitherWaitsForTheOther lanza dos hilos que solo llaman a claimedFor(...) y afirma unicamente assertEquals(10, all.size()) y assertEquals(10, Set.copyOf(all).size()). No hay sender, no se llama a record y no hay aserción de tiempo. El Given del contrato exige un receptor que tarda 500 ms en responder 200 y ese receptor no existe en el test.

POR QUE BLOQUEA (argumento de mutacion concreto, no ejecutado): si se sustituye «FOR UPDATE OF d SKIP LOCKED» por «FOR UPDATE» en PostgresWebhookWork.java:68, las 10 filas comparten next_attempt_at y se ordenan por d.id, de modo que ambos hilos eligen la misma primera fila. El perdedor bloquea; cuando el ganador confirma el lease, el perdedor reevalua el predicado en READ COMMITTED, la fila ya no cumple (leased_until > now), el LIMIT 1 devuelve cero filas, claimNext devuelve Optional.empty y el helper claimedFor hace break. El hilo perdedor acaba con 0 reclamaciones y el ganador, ya sin contencion, drena las 10: all.size()==10 y Set.size()==10 siguen ciertas y el test queda VERDE. La unica propiedad nueva que este escenario existe para proteger, el no bloqueo mutuo, carece por completo de oraculo, y al vivir la mutacion en un literal SQL la campana de mutacion Java tampoco la detecta.

MATIZ IMPORTANTE, que corrige la version original del bloqueante: NO es cierto que el receptor ni el desenlace carezcan de oraculo en la feature. DispatchWebhooksTest.java:153-171 (s20) ya afirma que un ciclo envia exactamente una peticion por eventId, en orden, y que cada entrega se registra succeeded con attempt 1; DispatchWebhooksTest.java:173-185 (s32) lo repite sobre 25 entregas; y WebhookRecoveryPersistenceTest drena contra Postgres real con un RecordingSender que cuenta los envios. Lo que falta es esas dos clausulas BAJO CONCURRENCIA, y sobre todo la clausula de no espera.

REMEDIO MINIMO SUFICIENTE (una sola prueba, sin tocar produccion): en WebhookWorkPersistenceTest, extender s23 para que cada hilo, ademas de reclamar, (a) simule el receptor lento con un Thread.sleep de unos 300-500 ms por entrega antes de llamar a work.record(claimed, claimed.delivery().recorded(WebhookAttempt.http(200,1), T), null), contando sus envios por eventId en una lista concurrente; (b) al final afirme sobre store().list(owner, endpointId) que las 10 filas del owner estan succeeded con attempt 1 y que hay 10 eventId distintos enviados; y (c) mida el reloj de pared del bloque completo y afirme que es netamente inferior a la suma serie de los diez retardos (por ejemplo, con 10 entregas y 400 ms de retardo, assertTrue(elapsed < 3000) frente a los ~4000 ms de la ejecucion serializada). Ese ultimo oraculo es el que muere si SKIP LOCKED desaparece, porque el hilo perdedor abandona y el ganador serializa las diez. Si se prefiere un oraculo determinista en vez de temporal, vale igualmente afirmar que AMBOS hilos reclamaron al menos una entrega (assertTrue(primero.size() > 0 && segundo.size() > 0) con el retardo puesto), que es justo lo que deja de cumplirse con FOR UPDATE bloqueante.

GRAVEDAD RECALIBRADA: media-alta (bloqueante, pero acotado a una sola prueba a ampliar; no hay hueco de cobertura en el envio ni en el desenlace fuera del escenario concurrente).

### 11. [ALTA · una hora] @s29: el tope de 50 elementos de la lectura no está implementado ni probado, y el orden tampoco se comprueba

- **Dimension:** contrato
- **Rutas:** features/webhooks.feature:367-369; backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresWebhookStore.java:118-128; backend/src/test/java/com/apptolast/organization/adapter/persistence/WebhookWorkPersistenceTest.java:180-204

**Evidencia.** El contrato dice «recibe 200 con items de como máximo 50 elementos ordenados por updatedAt DESC y después id DESC». La consulta de lectura es `SELECT * FROM webhook_deliveries WHERE owner_id=? AND endpoint_id=? ORDER BY updated_at DESC, id DESC` — sin LIMIT. El único test de poda afirma justamente lo contrario del tope: `assertEquals(52, log.size(), "fifty terminals plus the two pending ones")`. Además ninguna aserción, ni ahí ni en WebhookApiTest.s29 (que mockea `manage.deliveries`), comprueba el orden por updatedAt DESC + id DESC, ni que las 50 supervivientes sean «las de mayor updatedAt» (el test sólo cuenta 50 succeeded, no dice cuáles).

**Arreglo.** Decidir con el coordinador la lectura correcta de la línea 368 y dejarla escrita. Después: un test que inserte entregas con updatedAt e id controlados y afirme la secuencia exacta devuelta por `list`, otro que afirme el tamaño máximo acordado, y en el test de poda afirmar que las 50 supervivientes son exactamente las de updatedAt mayor (comparando ids, no contando).

**Correccion del verificador.** @s29: la lectura de /deliveries no acota a 50 items y ni el orden ni la identidad de las 50 supervivientes tienen oráculo (la poda de 50, en cambio, sí está implementada y probada).

Rutas: features/webhooks.feature:367-368; project-spec.md:2018; backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresWebhookStore.java:118-128; backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresWebhookWork.java:30,104,124-136; backend/src/main/java/com/apptolast/organization/application/ManageWebhook.java:59-63; backend/src/main/java/com/apptolast/organization/adapter/http/WebhookController.java:129-136; backend/src/test/java/com/apptolast/organization/adapter/persistence/WebhookWorkPersistenceTest.java:180-204; backend/src/test/java/com/apptolast/organization/adapter/WebhookApiTest.java:224-247.

Lo que SÍ está bien (no tocar): la poda a 50 terminales existe y funciona — PostgresWebhookWork:30 `KEPT_TERMINAL_DELIVERIES = 50`, :124-136 borra los terminales fuera del top-50 por `updated_at DESC, id DESC` y :104 lo hace en la misma transacción de `record`. Y tiene oráculo: WebhookWorkPersistenceTest:199-203 asserta exactamente 50 `succeeded` y las dos pendientes vivas. El `assertEquals(52, ...)` de :198 no es un error: es la línea 367 del contrato (50 terminales + 2 pendientes).

Hueco 1 — incumplimiento de contrato en la lectura. features/webhooks.feature:368 y project-spec.md:2018 exigen «items de como máximo 50 elementos». La cadena de lectura no acota en ningún punto: `PostgresWebhookStore.list` (:118-128) hace `SELECT * ... ORDER BY updated_at DESC, id DESC` sin LIMIT, `ManageWebhook.deliveries` (:59-63) la devuelve tal cual y `WebhookController.deliveries` (:129-136) mapea todas las filas. Con el escenario del propio @s29 (50 terminales + 2 pendientes) el GET devuelve 52 items. Hay que decidir explícitamente entre las dos únicas salidas coherentes y dejarla escrita: (a) añadir `LIMIT 50` a la consulta de lectura (o un `.limit(50)` en el caso de uso), o (b) enmendar contrato y spec para que la línea 368 diga «como máximo 50 terminales más las pendientes vivas». Si se elige (b), la enmienda debe tocar también project-spec.md:2018, que hoy dice «hasta 50 entregas» sin excepción.

Hueco 2 — orden sin oráculo. Ninguna prueba del repositorio comprueba el orden `updatedAt DESC, id DESC` del registro. Comprobado uno a uno: WebhookApiTest.s29 (:224-247) mockea `manage.deliveries` con UNA sola entrega y sólo valida la forma cerrada del DTO (11 campos) y la ausencia de body/url; WebhookWorkPersistenceTest.s29 cuenta pero no compara secuencias; WebhookPersistenceTest:196-197 y WebhookRecoveryPersistenceTest:204-213,259-262 filtran por id en vez de leer posiciones; e2e/webhooks-ux.spec.mjs:90 estubea la respuesta con un array fijo, así que no puede ser oráculo del backend.

Hueco 3 — identidad de las supervivientes sin oráculo. El contrato (:367) exige que las 50 conservadas sean «las de mayor updatedAt». El test de poda usa 55 entregas con `updatedAt` distintos (`T.plusSeconds(index)`), así que el dato para discriminar ya está ahí, pero nunca se mira: invertir el `DESC` del prune conservaría las 50 MÁS ANTIGUAS y el test seguiría verde (50 succeeded + 2 pending). Nota para el juez: por ser el ORDER BY parte de un literal SQL, la campaña de mutación de bytecode no genera este mutante; el defecto no lo detecta la puerta de mutación y hay que cerrarlo con aserciones explícitas.

Qué basta para levantarlo: (1) resolver el hueco 1 con código o con enmienda firmada del contrato y del spec; (2) en WebhookWorkPersistenceTest.s29, capturar los updatedAt/ids esperados y assertar la lista exacta de supervivientes en orden (`assertEquals(esperados, log.stream().map(WebhookDelivery::id).toList())`), lo que cubre de una vez orden e identidad en la capa de persistencia real; (3) un caso HTTP con al menos tres entregas de updatedAt decrecientes y dos empatadas en updatedAt para fijar el desempate por id DESC.

### 12. [ALTA · varias horas] @s42: el zoom nativo al 200 % no se ejecuta, el foco visible se mide pero no se afirma y el recorte se calcula y se descarta

- **Dimension:** contrato
- **Rutas:** features/webhooks.feature:518-522; e2e/webhooks-ux.spec.mjs:174-191,218-239,385-406; progress/tdd_webhooks.md:434-436

**Evidencia.** El contrato pide «con texto al 200 % y zoom al 200 %». La spec recorre WIDTHS = [320, 768, 1280, 1440] sin tocar `deviceScaleFactor`, sin reducir el viewport para emular el zoom y sin CSS zoom; el modo `text200` (líneas 126-158) sólo dobla `fontSize` inline. La bitácora lo admite: «No se ejecutó zoom nativo del navegador: el 200 % se mide por tamaño de fuente computado y reflow. No afirmo zoom nativo». Segundo: el test de teclado calcula `visibleFocus` (líneas 396-399) y lo vuelca a tab-order.json, pero las únicas aserciones son `expect(order.length).toBeGreaterThan(5)` y que los nombres no estén vacíos — «foco visible» no se afirma nunca. Tercero: el auditor calcula por elemento `overflowing: element.scrollWidth > element.clientWidth + 1` (línea 182) y sólo lo usa dentro del mensaje de fallo de la línea 220; la única aserción de desbordamiento es `measured.scroll <= width` sobre documentElement, así que «ni recorte de la URL, del secreto ni de la tabla de entregas» carece de oráculo y el recorte vertical no se mide en absoluto.

**Arreglo.** 1) Emular el zoom al 200 % de verdad: `browser.newContext({ deviceScaleFactor: 2 })` o recorrer los cuatro anchos con el viewport a la mitad de CSS px, y dejar la evidencia por estado. 2) Convertir `visibleFocus` en aserción: `for (const item of order) expect(item.visibleFocus).toBe(true)`. 3) Convertir `offenders` en aserción (`expect(measured.offenders).toEqual([])`) y añadir la comprobación vertical (`scrollHeight > clientHeight + 1` sobre los contenedores de la URL, el secreto y la tabla).

**Correccion del verificador.** Título: @s42: el zoom nativo al 200 % nunca se ejecuta y el foco visible se mide con una API inválida y no se afirma

Rutas: features/webhooks.feature:518-519; e2e/webhooks-ux.spec.mjs:126-158,396-406; e2e/github-connector-native-zoom.spec.mjs:37-126; e2e/github-connector.spec.mjs:302-340; progress/tdd_webhooks.md:434-436; progress/ux_webhooks.md:32-35

Evidencia (comprobada leyendo, no citada):

(A) Zoom nativo al 200 % — cláusula del When de @s42 (línea 518: «con texto al 200 % y zoom al 200 %») sin ejecución alguna. `e2e/webhooks-ux.spec.mjs` no contiene `zoom`, `deviceScaleFactor`, `setZoom` ni `chrome.windows.update` (grep sobre las 420 líneas: cero coincidencias). El modo `text200` (126-158) sólo dobla `fontSize` inline y verifica el doblado; eso cubre «texto al 200 %», que el contrato pide *además*. `grep -rn "webhooks" e2e/ -l` devuelve un único fichero, así que no hay cobertura en otro sitio; y no existe `webhooks-native-zoom.spec.mjs` aunque el repo ya tiene cuatro specs de zoom nativo. Ambas bitácoras lo declaran: «No se ejecutó zoom nativo del navegador […] No afirmo zoom nativo». Precedente aprobado y reutilizable: `github-connector-native-zoom.spec.mjs:37-126` (extensión efímera + `chrome.tabs.setZoom`, con `expect(zoom).toBe(2)` y `expect(measured.dpr).toBe(baseline.dpr*2)`).

(B) Foco visible — cláusula del Then de @s42 (línea 519: «foco visible») medida y descartada, y además medida mal. Líneas 396-400 calculan `visibleFocus` con `getComputedStyle(active, ":focus-visible")`; el segundo parámetro de `getComputedStyle` es un pseudo-ELEMENTO, no una pseudo-clase, de modo que Chromium devuelve el estilo normal del elemento y la comprobación es tautológica. El valor se escribe en `tab-order.json` (404) y no aparece en ningún `expect`: las únicas aserciones del test son `order.length > 5` (405) y nombre accesible no vacío (406). El listón del proyecto ya está fijado: `github-connector.spec.mjs:302-340` recorre con Tab y afirma `expect(invisible).toEqual([])` sobre `matches(":focus-visible") && outlineStyle !== "none" && outlineWidth >= 1 && !outlineColor.includes("transparent")`; equivalente en `appearance-ux-audit.spec.mjs:516-517`, `customization-ux.spec.mjs:467`, `history-ux.spec.mjs:178`. La regla `:focus-visible { outline: 2px solid var(--accent) }` de `frontend/src/webhooks.scss:57-59` es producción, no oráculo: nada falla si alguien la borra.

Por qué bloquearía: dos cláusulas explícitas de @s42 quedan sin verificar. La (A) es exactamente el motivo por el que el juez rechazó hoy otra feature (zoom nativo nombrado en el contrato y nunca ejecutado) y la técnica ya está escrita y aprobada en este mismo repo, así que no hay coste de investigación. La (B) deja el anillo de foco sin ninguna red: hoy sólo lo sostiene una regla SCSS, y el propio dato que lo mediría está calculado con una API que no hace lo que el autor cree.

Gravedad: alta.

NO forma parte del bloqueante (retirado del original por sobredimensionado y por describir mal el código): el «recorte». `webhooks-ux.spec.mjs:222-239` SÍ afirma, por cada control visible de `main` y en los cuatro anchos, `box.x >= 0`, `box.x + box.width <= width + 1`, `width >= 44` y `height >= 44` — el mismo oráculo de «sin contenido recortado» que el juez aceptó en la feature 24 (`github-connector-native-zoom.spec.mjs:174-192`), y que cubre el campo del secreto por ser un `<input>` de `main`. El recorte vertical no se mide en ningún carril del repo, incluidos los aprobados. Queda como ADVISORY, no bloqueante: `overflowing` (182) sólo viaja dentro del mensaje de fallo de la línea 220, así que el texto de la URL y las celdas de la tabla de entregas (que no son controles) no tienen oráculo propio; convertir `offenders` en una aserción (`expect(measured.offenders).toEqual([])`) sería un refuerzo barato y sensato al cerrar (A) y (B).

### 13. [ALTA · una hora] «keyboard reaches every control in DOM order» no compara ningún orden y se conforma con 6 controles de ~21

- **Dimension:** oraculos
- **Rutas:** e2e/webhooks-ux.spec.mjs:372-406

**Evidencia.** Título: `webhooks audit: keyboard reaches every control in DOM order and focus returns @s42`. Cuerpo: `for (let step = 0; step < 60; step++) { await page.keyboard.press("Tab"); … if (focused) order.push(focused); }` seguido de `await writeFile(`${folder}/tab-order.json`, …); expect(order.length).toBeGreaterThan(5);`. No hay ninguna comparación de `order` contra el orden del DOM.

**Arreglo.** Enumerar los controles esperados de cada estado (`document.querySelectorAll` dentro de `main`) y asertar `expect(order.map(o => o.name)).toEqual(nombresEnOrdenDom)`, o como mínimo `expect(order.length).toBe(controles.length)` más la igualdad de la secuencia. El umbral `toBeGreaterThan(5)` debe desaparecer.

**Correccion del verificador.** BLOQUEANTE (gravedad alta, confirmado). Título correcto: «webhooks-ux.spec.mjs:372 promete "alcanza todos los controles en orden del DOM" y no compara ningún orden: su único oráculo de cobertura es un umbral de 6 frente a 20 controles reales».

Rutas: e2e/webhooks-ux.spec.mjs:372 (título), :385-402 (bucle), :404-406 (aserciones). Contraste: e2e/github-connector.spec.mjs:261-291. Declaración inexacta: progress/ux_webhooks.md, fila «Posición en serie». Contrato: features/webhooks.feature:519.

Qué hace de verdad la prueba: pulsa Tab 60 veces a ciegas sin sembrar el foco, empuja a `order` cada elemento enfocado que esté dentro de `main` (sin deduplicar), vuelca `order` a `.e2e-work/webhooks-ux/keyboard/tab-order.json` y afirma solo dos cosas: que se acumularon más de 5 entradas y que ninguna tiene nombre accesible vacío. Después sí comprueba, correctamente, que cancelar la confirmación de borrado devuelve el foco a «Eliminar». El campo `visibleFocus` se calcula dentro del `page.evaluate` y no se afirma nunca: es evidencia muerta, pese a que la cláusula 519 también exige «foco visible».

Alcance real frente al contrato (leído en frontend/src/webhooks.tsx con items:[endpoint()], sin secreto, sin diálogo, sin panel de entregas): 20 controles alcanzables — URL, Descripción, la casilla «Seleccionar todos», las 12 casillas de tipo de evento, «Crear webhook», «Enviar ping», «Desactivar», «Ver entregas» y «Eliminar». El umbral es 6. «Todos los controles» queda sin oráculo y «en orden lógico» queda sin oráculo alguno.

Mutaciones del producto que sobreviven, verificadas contra el código y no contra la bitácora:
  - Intercambiar el orden de «Enviar ping» y «Eliminar» en la fila, o mover el `<fieldset>` de tipos detrás del `<button type="submit">`: `order` cambia de contenido, ninguna aserción lo mira. Es la mutación limpia; el orden no se compara con nada.
  - Poner `tabIndex={-1}` en las 12 casillas de tipo de evento: quedan 8 controles alcanzables, 8 > 5, verde. (La variante del revisor —desactivar también los 4 botones de fila, dejando 4— solo sobrevive si Tab cicla en Chromium headless; con 8 no hace falta esa suposición.)
  - Ninguna de las dos la detecta axe, ni el bloque `auditor` (mide geometría, no alcanzabilidad), ni los unitarios de frontend/src/webhooks.test.tsx (getByRole ignora tabindex).

Qué se exige para levantarlo, sin inventar nada nuevo: portar el patrón que esta misma casa ya tiene verde en e2e/github-connector.spec.mjs:261-291 — derivar `expectedOrder` de `document.querySelectorAll("main button, main a, main select, main input")` filtrando por `getClientRects().length`, sembrar el foco en `main h1`, iterar `expectedOrder.length + 4` pulsaciones deduplicando, y cerrar con `expect(reached).toEqual(expectedOrder)`. Añadir además la aserción del `visibleFocus` que ya se recoge y se tira, para cubrir la parte de «foco visible» de la cláusula 519.

Corrección documental obligatoria y separada: la fila «Posición en serie» de progress/ux_webhooks.md declara «el orden de Tab sigue al DOM | Verificado en navegador (tab-order.json)». Eso es falso hoy; o se corrige la prueba o se corrige la fila, pero no puede cerrarse la feature con la fila como está.

### 14. [ALTA · minutos] «@s38 starts clean for another identity» afirma la ausencia de un secreto que la prueba nunca llegó a mostrar

- **Dimension:** oraculos
- **Rutas:** frontend/src/webhooks.test.tsx:299-307; frontend/src/webhooks.tsx:50-52

**Evidencia.** `it("@s38 starts clean for another identity", async () => { stubApi([endpoint()]); const view = render(<Webhooks owner="Ana" />); await shown(); view.rerender(<Webhooks owner="Bea" />); expect(screen.queryByDisplayValue(secret)).not.toBeInTheDocument(); });` — en ningún momento se crea un webhook ni se recibe un 201, así que `secret` jamás estuvo en el DOM.

**Arreglo.** Reescribirla en dos tiempos: crear el webhook con la identidad A hasta que `getByDisplayValue(secret)` sea visible (presencia), y solo entonces `rerender` con la identidad B y exigir que el secreto y los items de A hayan desaparecido. Sin el paso de presencia la prueba no puede fallar.

**Correccion del verificador.** BLOQUEANTE CONFIRMADO, con la descripción afinada en tres puntos.

Título correcto: «@s38 starts clean for another identity» no monta la precondición del contrato («un secreto visible en memoria») y deja sin oráculo la fila «cambia la identidad de acceso» del Scenario Outline @s38.

Rutas verificadas leyendo: frontend/src/webhooks.test.tsx:299-307 (única aserción en la 306); frontend/src/webhooks.tsx:48-51; features/webhooks.feature:465-476.

Qué falla exactamente:
- La prueba nunca crea el webhook: `stubApi([endpoint()])` sólo contesta el GET `/api/v1/me/webhooks`; `secret` sólo se pinta tras un 201 (véase la prueba de la línea 189, que sí lo hace). La aserción de la 306 es cierta antes del `rerender`.
- Mutación de producto que la deja verde — CORRIJO la que propone el revisor: quitar `key` a secas dejaría `owner` sin usar y podría caer en la puerta de lint/TS, enmascarando el problema. La mutación mínima y limpia es fijar la key a una constante conservando el prop, p. ej. `return <WebhookPanel key="panel" />;` (o `key={owner.slice(0,0)}`). Con eso desaparece el remonte por identidad, el estado de Ana sobrevive a Bea, y las 21 pruebas de webhooks.test.tsx siguen en verde: es un mutante superviviente que la fila @s38 debería matar.
- Cláusulas de la fila que no se comprueban en ningún sitio del repositorio (grep sobre frontend/src y frontend/e2e): (a) que la lista de la identidad anterior no sobreviva ni se actualice con la respuesta tardía; (b) que la petición pendiente se aborte al cambiar de identidad — la prueba de la 275 sí verifica `signal.aborted` pero sólo para `view.unmount()`, que cubre «navega a otra ruta» y «cierra sesión», no el cambio de identidad; (c) «un 401 tardío de esa petición no retira una sesión posterior»: no hay ni una sola aparición de `401` ni de `observeAccess` en webhooks.test.tsx ni en webhooks-client.test.ts, pese a que `frontend/src/api-client.ts:32` dispara `onUnauthorized?.(401)`.

Forma del arreglo (para el artesano): reutilizar el montaje de la prueba de la 189 — stub que devuelva listas DISTINTAS por identidad y un 201 con `secret`; crear el webhook y afirmar `getByDisplayValue(secret)` visible; dejar además un POST en vuelo con su `signal` capturado; `rerender` con `owner="Bea"`; y sólo entonces afirmar (1) que `secret` ya no está en el DOM, (2) que la lista de Ana («Mi hook» / su URL) ya no está y se muestra la de Bea, (3) que el `signal` de la petición de Ana quedó abortado, y (4) que resolver tarde ese POST con 201 o con 401 no repinta el secreto ni retira la sesión de Bea. Con ese oráculo, `key="panel"` muere.

Gravedad: alta, confirmada. No es preferencia de estilo: es una fila del contrato sin oráculo y una prueba cuyo título afirma más de lo que verifica, exactamente el patrón por el que el juez ya rechazó features hoy.

Nota de honestidad: todo lo anterior sale de leer los ficheros y el .feature; no ejecuté la suite, así que el «deja la prueba verde» es razonamiento sobre el código, no una corrida observada.

### 15. [ALTA · varias horas] Solo hay una región aria-live y únicamente anuncia la carga: los cambios de estado que el contrato manda anunciar no se anuncian ni se prueban

- **Dimension:** accesibilidad
- **Rutas:** frontend/src/webhooks.tsx:265; features/webhooks.feature:522 y 483; e2e/webhooks-ux.spec.mjs (sin ninguna aserción sobre aria-live); frontend/src/webhooks.test.tsx:108-110

**Evidencia.** En toda la vista hay un único `aria-live`: `<div aria-live="polite">{loading && <p>Cargando webhooks…</p>}</div>` (webhooks.tsx:265). No existe ninguna otra región live ni `role="status"`. Los cambios de estado de @s39 —Activo → «Desactivado manualmente» tras pulsar Desactivar, la fila que desaparece tras Eliminar, la entrega que pasa a Pendiente tras Enviar ping o Reenviar— se repintan en `<span aria-label={statusLabel}>` (webhooks.tsx:396-398) sin anuncio alguno. En las pruebas: el único assert sobre aria-live del repositorio para esta vista es webhooks.test.tsx:110 (`loading.closest("[aria-live]")`); en e2e/webhooks-ux.spec.mjs la cadena «aria-live» no aparece ni una vez. Nota adicional: `aria-label` sobre un `span` sin rol no expone nombre a las tecnologías asistivas, así que el «mismo valor en un atributo ARIA» de @s42:483 se cumple de forma literal pero inerte, y axe no lo señala.

**Arreglo.** Añadir una región `role="status"`/`aria-live="polite"` que reciba el resultado de cada acción (desactivado, activado, ping encolado, entrega reenviada, webhook eliminado), asertarla en unitario por cada acción de @s39 y @s40, y comprobar en el E2E que tras «Desactivar» el texto anunciado aparece en la región live. Cambiar el `span` del estado por un elemento con rol que soporte nombre accesible (o retirar el `aria-label` redundante y dejar el texto, que ya es el mismo valor).

**Correccion del verificador.** Título: @s42:522 exige que los cambios de estado se anuncien por aria-live y la única región live propia de la vista anuncia sólo la carga; ni el producto lo implementa ni ninguna prueba lo comprueba.

Rutas verificadas: frontend/src/webhooks.tsx:265 (única `aria-live`, envuelve sólo `loading`); 396-397 (`<span aria-label={statusLabel}>` fuera de toda región live); 197-247 (`changeStatus`, `ping`, `remove`, `redeliver`: ninguna escribe en una región live); features/webhooks.feature:522 y 483; frontend/src/webhooks.test.tsx:110 (único assert sobre `[aria-live]`); e2e/webhooks-ux.spec.mjs (0 apariciones de «aria-live» en 420 líneas); progress/ux_webhooks.md:105 y 30-36.

Evidencia, con la corrección de la versión original: la vista SÍ tiene otras regiones live, pero todas son `role="alert"` (268, 321, 369, 371) y cubren únicamente los errores de @s41. Ninguna cubre los cambios de estado de @s39: «Desactivar» → el texto de la fila pasa a «Desactivado manualmente» sin anuncio; «Activar» → ídem; «Enviar ping» y «Reenviar» → la entrega aparece en Pendiente sin anuncio. La única excepción parcial es «Eliminar y confirmar», donde `remove()` devuelve el foco al `<h2>` «Tus webhooks» (webhooks.tsx:228), lo que produce un anuncio de foco —no de resultado— y no satisface la cláusula. Cobertura: cero asserts de anuncio para @s39 en unitarios (los cinco tests @s39 verifican método, ruta, cuerpo y texto) y cero en el E2E de UX. La matriz UX menciona `aria-live` una sola vez (línea 105) y sólo para la carga, y no declara este hueco entre sus límites (30-36), así que el vacío queda invisible al lector del expediente.

Nota técnica que se mantiene: `aria-label` sobre un `<span>` sin rol no expone nombre accesible (ARIA lo prohíbe en `role=generic`), de modo que «el mismo valor en un atributo ARIA» de @s42:483 se cumple de forma literal pero inerte, y axe no lo señala. Esto es una observación de calidad, no la base del bloqueo.

Por qué bloquea: es una cláusula nombrada del contrato sin implementación y sin oráculo, la misma categoría por la que el juez ya rechazó hoy el «zoom nativo al 200 % que el contrato nombra y nunca se ejecuta». No es opinable: `grep` sobre producto y sobre las dos suites lo demuestra.

Gravedad: alta (bloqueante). Cierre mínimo esperable: (a) una región `role="status" aria-live="polite" aria-atomic="true"` propia de la vista a la que `changeStatus`, `ping`, `redeliver` y `remove` escriban un mensaje con el resultado; (b) al menos un unitario por acción de @s39 que lea el texto de esa región tras la acción —no `closest("[aria-live]")` sobre un texto estático, que no discriminaría—; (c) una comprobación en `e2e/webhooks-ux.spec.mjs` de que la región cambia tras «Desactivar»; y (d) actualizar `progress/ux_webhooks.md` para que la fila de anuncios deje de referirse sólo a la carga. Si se decide no implementarlo, no vale con dejarlo callado: hay que declararlo como límite explícito en la matriz y aceptarlo como riesgo, porque hoy contradice @s42:522.

Aviso adicional para el artesano (no forma parte del bloqueante, verificado por lectura del JSX en 399-417): al pulsar «Desactivar», el fragmento de dos botones se sustituye por el botón único «Activar», por lo que React desmonta el botón enfocado y el foco cae al `body`. Conviene resolverlo en el mismo cambio.

### 16. [ALTA · una hora] Ningún test sujeta que una credencial Bearer de la feature 24 no alcance ninguna ruta de webhooks

- **Dimension:** seguridad
- **Rutas:** features/webhooks.feature:413-414; progress/tdd_webhooks.md:9; backend/src/test/java/com/apptolast/organization/adapter/WebhookApiTest.java (fichero completo, sin ninguna cabecera Authorization)

**Evidencia.** El contrato pide dos filas: «una credencial Bearer válida de la feature 24 | GET /api/v1/me/webhooks» y «… | POST /api/v1/me/webhooks/{id}/ping», ambas sin tocar webhooks. La bitácora tdd_webhooks.md:9 se compromete: «esas dos filas se prueban con 403 API_SCOPE_DENIED y sin tocar webhooks». Busqué la cadena 'me/webhooks' en todo backend/src/test: aparece únicamente en WebhookApiTest.java, y ese fichero no monta ni una sola petición con cabecera Authorization (los cuatro tests s33_ de :363-411 cubren sin sesión, CSRF, Origin y PATCH, ninguno el canal Bearer). Por lectura la frontera hoy aguanta: ApiCredentialBearerFilter.java:27-100 no incluye ningún patrón /api/v1/me/webhooks en PERMISSIONS y :146-149 responde 403 API_SCOPE_DENIED cuando no hay coincidencia. Pero nada lo mantiene así.

**Arreglo.** Añadir en WebhookApiTest dos tests con .header("Authorization", "Bearer …") sobre GET /api/v1/me/webhooks y POST /api/v1/me/webhooks/{id}/ping que exijan el rechazo acordado (403 API_SCOPE_DENIED según la decisión 5) más verifyNoInteractions(create, manage). Si el @WebMvcTest no puede levantar la cadena Bearer con sus ObjectProvider, hacerlo como test de la allowlist: afirmar que ninguna Permission de ApiCredentialBearerFilter.PERMISSIONS casa con una petición a /api/v1/me/webhooks en ninguno de los siete verbos del contrato.

**Correccion del verificador.** Título: Las dos filas Bearer de @s33 no tienen oráculo y el contrato quedó sin reconciliar con la enmienda que sí se aplicó al código

Rutas: features/webhooks.feature:413-414; progress/tdd_webhooks.md:9; backend/src/test/java/com/apptolast/organization/adapter/WebhookApiTest.java:363-411; backend/src/main/java/com/apptolast/organization/adapter/http/ApiCredentialBearerFilter.java:27-102 y :146-150; backend/src/main/java/com/apptolast/organization/adapter/config/SecurityConfiguration.java:84-88. Comparación: features/github_connector.feature:398 y :411-412; backend/src/test/java/com/apptolast/organization/adapter/GithubConnectorApiTest.java:638-661.

Evidencia (comprobada leyendo, no citada de bitácora):
- El Scenario Outline @s33 tiene ocho filas. WebhookApiTest cubre seis con cuatro pruebas s33_ (:363 sin sesión GET+POST, :375 CSRF POST+DELETE, :388 Origin, :402 PATCH). Las filas :413 y :414 —canal Bearer— no tienen ninguna prueba. El fichero no monta ni una petición con cabecera Authorization; el grep de "Authorization|Bearer" sobre backend/src/test no lo lista.
- No hay cobertura equivalente en ningún otro fichero. Solo WebhookApiTest ejercita la frontera HTTP de webhooks entre los 32 ficheros de prueba que mencionan webhook. Ninguna prueba fija PERMISSIONS como conjunto cerrado (dos únicas ocurrencias de PERMISSIONS, ambas en el filtro); ApiCredentialBearerTest.s23:70-101 enumera las 18 rutas que están DENTRO de la allowlist, nunca lo que queda fuera.
- Por lectura la frontera aguanta hoy por omisión: PERMISSIONS (:27-102, 18 entradas) no contiene ningún patrón /api/v1/me/webhooks, y :146-150 responde 403 API_SCOPE_DENIED al no haber coincidencia. La cadena Bearer sí alcanza la ruta: su securityMatcher (SecurityConfiguration:84-88) captura toda petición con Authorization que no sea de calendario.
- AGRAVANTE que el bloqueante original no vio: el contrato nunca se reconcilió. features/webhooks.feature:413-414 sigue esperando 401 UNAUTHENTICATED, mientras el producto devuelve 403 API_SCOPE_DENIED. La feature hermana 26 resolvió lo mismo correctamente —github_connector.feature:398 lleva el comentario de la enmienda, :411-412 se reescribieron a 403 API_SCOPE_DENIED, y GithubConnectorApiTest:638-661 implementa la prueba con verifyNoInteractions—. En la feature 25 no se hizo ninguna de las dos cosas: ni enmienda del .feature ni prueba. (features/additional_connectors.feature:392-393 arrastra la misma forma sin tocar.)

Por qué bloquea: dos filas del contrato sin oráculo, con una bitácora que afirma explícitamente que están probadas cuando no lo están, es exactamente el patrón que ya motivó un rechazo hoy (la prueba «se cancela con Escape»). Además el contrato y el producto discrepan por escrito sin que nadie lo haya conciliado, así que ni siquiera está claro qué se considera correcto. La frontera es la que separa el canal de credenciales de máquina de una superficie que crea egreso de red firmado y devuelve un secreto; si alguien amplía PERMISSIONS o el securityMatcher, ninguna prueba se pondrá roja.

Gravedad corregida: media-alta, y bloqueante para cerrar la feature. Rebajo de «alta» a «media-alta» en el eje de riesgo porque no hay exposición viva: el diseño es denegar-por-defecto y el comportamiento de hoy es correcto. Lo que falla es la disciplina de cobertura y la veracidad de la bitácora, y eso sí impide el done.

Cierre mínimo: (a) enmendar features/webhooks.feature:413-414 a 403 API_SCOPE_DENIED con la misma nota que lleva github_connector.feature:398, o justificar por escrito por qué webhooks difiere; (b) añadir a WebhookApiTest la prueba análoga a GithubConnectorApiTest.s31 —credencial Bearer válida con scopes de projects/tasks contra GET /api/v1/me/webhooks y POST /api/v1/me/webhooks/{id}/ping, esperando 403 API_SCOPE_DENIED y verifyNoInteractions sobre los casos de uso de webhooks—; (c) corregir progress/tdd_webhooks.md:9, que hoy afirma algo falso.

### 17. [ALTA · varias horas] La validación TLS y el plazo de intercambio, que son controles de carga, no tienen ninguna prueba

- **Dimension:** seguridad
- **Rutas:** backend/src/test/java/com/apptolast/organization/adapter/webhook/JdkWebhookSenderTest.java:80-208 (contra features/webhooks.feature:327 y :329)

**Evidencia.** El contrato @s25 pide dos filas explícitas: «acepta la conexión y no responde en 5 s → TIMEOUT» y «presenta un certificado no confiable → TLS». JdkWebhookSenderTest tiene once tests y ninguno es esos dos: cubre 404, 500, 302, puerto cerrado, host que no resuelve y dirección bloqueada, todos contra receptores en texto claro (Receiver.url() en :48 devuelve http://127.0.0.1:…). No hay ni un receptor con certificado autofirmado ni un receptor que se cuelgue. Las ramas catch (SSLException) de :70-71 y catch (HttpTimeoutException) de :68-69, y el classify() de :83-90, no los ejecuta ningún test.

**Arreglo.** Añadir a JdkWebhookSenderTest dos casos: un HttpsServer con certificado autofirmado que debe dar errorClass TLS y httpStatus null, y un receptor que acepta la conexión y no responde, que debe dar TIMEOUT dentro del plazo de intercambio de 10 s y no antes de 5 s (para que el test distinga el plazo de conexión del total, que es justo lo que B1 desambiguó).

**Correccion del verificador.** BLOQUEANTE (confirmado, gravedad alta). Versión corregida:

**Título**: Las dos filas de transporte de @s25 que dependen de una condición de red real — plazo agotado y certificado no confiable — no tienen ningún test que las produzca.

**Qué está y qué falta (comprobado leyendo)**
- Contrato: `features/webhooks.feature:327` y `:329`, dos de las ocho filas del Scenario Outline `@s25`, cuyo Given exige «un receptor de prueba que <comportamiento>».
- `backend/src/test/java/com/apptolast/organization/adapter/webhook/JdkWebhookSenderTest.java`: 8 métodos / 11 ejecuciones, todos contra `http://127.0.0.1` (`Receiver.url()`, `:48`). Cubren 404, 500 (`:145`), 302 sin seguir (`:155`), puerto cerrado (`:179`), host que no resuelve (`:191`) y dirección bloqueada (`:200`). No hay receptor que se cuelgue ni receptor con certificado propio.
- **Matiz que el borrador omitía y hay que incluir**: la cobertura no es cero. `backend/src/test/java/com/apptolast/organization/domain/WebhookAttemptTest.java:66-79` (`s25_atransportFailureHasNoHttpStatusAndKeepsTheDeliveryPending`) parametriza sobre TIMEOUT, CONNECTION, TLS, DNS y BLOCKED_ADDRESS y comprueba `httpStatus` nulo, `status` pending, attempt 1 y `nextAttemptAt` T+1 min. Es decir: **el Then de las dos filas sí está probado**; lo que no existe es el mapeo Given→errorClass, porque la cadena la mete el propio test a mano. Y ahí es justo donde está el valor de seguridad. Formularlo así evita que el artesano conteste «ya está en WebhookAttemptTest» y cierre el ciclo sin arreglar nada.
- En todo `backend/src/test` hay un único acierto de «TLS» (ese `@ValueSource`) y ningún `.jks`/`.p12`/`.pem` en el repositorio: no hay infraestructura de certificado de prueba en ninguna parte.

**Por qué bloquea, con la premisa corregida (más fuerte, no más débil)**
La enmienda B3 de `progress/tdd_webhooks.md:36-39` declara que el rebinding «deja de ser un límite aceptado» porque se conectaría «contra la IP literal ya validada conservando el nombre original en Host y SNI», y el javadoc de la clase (`JdkWebhookSender.java:25-27`) lo repite. **El código no hace eso**: `guardDestination` (`:92-97`) resuelve y valida las direcciones, y acto seguido `:62` llama a `client.send(request(url, …))` con la URL y el nombre originales, de modo que el `HttpClient` vuelve a resolver por su cuenta. La ventana TOCTOU sigue abierta. Por tanto la validación de certificado no es «la barrera principal» del riesgo residual del SSRF: hoy es **la única**, tal como la describe `progress/security_review_connectors.md:40` («la obligación de https reduce mucho la explotabilidad, porque el servicio interno tendría que presentar un certificado válido para ese nombre»). Sin un test, un `SSLContext` permisivo o un trust-all de depuración pasa la suite entera en verde. (La no implementación de B3 es un hallazgo independiente y probablemente otro bloqueante; conviene emitirlo aparte, no fundirlo con este.)
En la mitad del plazo, el motivo de B1 era exactamente que un receptor que gotea fija el hilo del worker; nada lo demuestra hoy.

**Dos precisiones más para el dictamen**
- La fila `:327` está desfasada: la enmienda B1 (`progress/tdd_webhooks.md:22-29`) la convierte en el plazo total del intercambio, 10 s, y encarga al coordinador actualizar el `.feature` en main, cosa que no se hizo. El test debe fijar el plazo total de `EXCHANGE_TIMEOUT` (10 s, `JdkWebhookSender.java:31`) y hay que corregir la línea del contrato en el mismo cambio.
- Coste real del arreglo: `EXCHANGE_TIMEOUT` es una constante privada, así que un test de plazo cuesta ~10 s de reloj salvo que se inyecte el plazo. Ese es el trabajo, y el precedente de que es asumible está en el propio repositorio: `HttpGithubIssueSourceTest.java:307-317` con `FakeGithub.delayBody`.

**Nota de contexto, no parte del bloqueante**: `backend/build.gradle.kts` no define ningún scope PIT `webhooks` (el único «Webhook» que aparece, líneas 495 y 506, pertenece a automatizaciones), pese a que `progress/proposal_webhooks.md:66` lo prometía. Cuando se añada e incluya `adapter.webhook.JdkWebhookSender`, los mutantes sobre los `return transport("TIMEOUT"/"TLS", …)` y sobre `classify()` sobrevivirán: la puerta de mutación volverá a tropezar con este mismo hueco.

**Observación adyacente detectada al verificar** (finding distinto, no mezclar): el contrato de `@s25` pide «latencyMs … medido con el reloj inyectado», pero `JdkWebhookSender` la mide con `System.nanoTime()` (`:58`, `:117-119`); el `Clock` solo alimenta el `t` de la firma. Además esa aserción de latencia solo aparece en el test de `@s26` (`:138`), en ninguna fila de `@s25`.

### 18. [MEDIA · una hora] @s28: la reactivación no llega a los dos eventos posteriores al cursor que el escenario exige

- **Dimension:** contrato
- **Rutas:** features/webhooks.feature:356-359; backend/src/test/java/com/apptolast/organization/adapter/persistence/WebhookRecoveryPersistenceTest.java:147-216

**Evidencia.** El Given pide «un webhook disabled por DELIVERY_EXHAUSTED con D1 exhausted, D2 pending y dos eventos suscritos posteriores al cursor», y el Then «D2 se entrega y después los dos eventos posteriores, en orden». El test monta D1 y D2 como dos pings (`store.enqueuePing`, líneas 153-156) y no inserta ninguna fila en outbox_events posterior al cursor; ni siquiera usa el `outbox()` que declara en la línea 92. La aserción final es `assertEquals(List.of(pending.eventId().toString()), sender.sent...)`: exactamente un envío. La conservación del cursor sí está bien afirmada (líneas 187-193).

**Arreglo.** Insertar en el mismo test dos ProjectCreated.v1 propios con occurredAt posterior al cursor (el helper `givenEvent` de WebhookOutboxPersistenceTest ya hace esto), ejecutar EnqueueWebhookDeliveries junto al drenaje tras reactivar, y afirmar la secuencia D2, E1, E2 sobre el RecordingSender.

**Correccion del verificador.** @s28: el Then «D2 se entrega y después los dos eventos posteriores, en orden» no tiene oráculo, y la compuerta que la reactivación abre no está cubierta en ningún sitio.

Rutas:
- features/webhooks.feature:355-359 (escenario @s28)
- backend/src/test/java/com/apptolast/organization/adapter/persistence/WebhookRecoveryPersistenceTest.java:147-216 (test s28), líneas 154 y 156 (los dos pings), 198-201 (aserción final), 92 (helper `outbox()` declarado y nunca usado)
- backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresWebhookOutbox.java:43-55 (`readyEndpoints()`, filtro `e.status = 'active'`)

Qué se comprobó leyendo:
1. El test monta D1 y D2 como pings sintéticos (`store.enqueuePing`) y no inserta ni una fila en `outbox_events` (grep sin resultados sobre el fichero). El helper `outbox()` de la línea 92 no se invoca en ninguna parte del fichero.
2. La aserción final, `assertEquals(List.of(pending.eventId().toString()), sender.sent...)`, exige exactamente un envío. No es que no compruebe los dos eventos posteriores: es que el test se pondría en rojo si el producto los entregase. El Then del contrato y el oráculo del test se contradicen.
3. Sustituir D2 por un ping además vacía la parte de orden del Then: `readyEndpoints()` excluye endpoints con una entrega de outbox pendiente pero NO con un ping pendiente (`AND d.event_type <> ?` con PING, PostgresWebhookOutbox.java:47-50). Con D2 como ping desaparece el bloqueo que hace significativo el «primero D2 y después los dos eventos»; el «en orden» queda sin ejercer por construcción.
4. Lo que el test sí prueba y está bien: nada sale mientras el webhook está disabled, el cursor no se mueve al reactivar (187-193), D1 sigue exhausted y no se reenvía, y el endpoint queda active con disabledReason/disabledAt nulos.
5. La cobertura no está en otro fichero. Búsqueda completa en backend/src/test: el único otro @s28 de webhooks es WebhookEndpointTest:43 (dominio puro, solo cubre el tercer Then); ManageWebhookTest:130 cubre reactivar-limpia-la-razón a nivel de caso de uso sin tocar la outbox; WebhookWorkPersistenceTest:119-125 (s22) cubre que un disabled no entrega. Ninguna compone reactivación con el recorrido de la outbox.
6. Agujero adicional detectado: la única compuerta de producción que la reactivación abre en el lado del encolador es `e.status = 'active'` en `readyEndpoints()`. `grep -rn "readyEndpoints"` en backend/src da 5 aciertos y ninguno la ejerce con un endpoint no activo (WebhookOutboxPersistenceTest:222 solo la consulta sobre endpoints activos; EnqueueWebhookDeliveriesTest:40 es un doble en memoria). Suprimir ese filtro del SQL no rompe ninguna prueba del repositorio: un webhook desactivado seguiría consumiendo su outbox y avanzando su cursor sin que nada lo detecte.

Matiz de calibración respecto a la formulación anterior: el mecanismo de reanudación desde el cursor SÍ está probado de forma aislada (WebhookOutboxPersistenceTest `s18_onlyTheOwnersRowsAfterTheCursorAndBeforeTheHorizonAreCandidates`, `s18_enqueueingWritesTheDeliveryAndMovesTheCursorWithoutTouchingTheOutbox`, `s20_...NotReadyButAPingDoesNotBlockIt`; EnqueueWebhookDeliveriesTest s18/s19). Lo que falta no es el recorrido en abstracto, sino (a) que se reanude para un endpoint que estuvo disabled y (b) el filtro de status del encolador. Eso hace el arreglo barato, no lo hace prescindible.

Arreglo mínimo para levantar el bloqueante:
- En el test s28, montar D2 como entrega real de outbox (usando el `outbox()` ya declarado en la línea 92) e insertar dos filas en `outbox_events` del tipo suscrito, con `occurred_at` posteriores al cursor y fuera de la ventana de gracia de 5 s, con `event_id` que fijen el orden por tupla; drenar alternando ciclos de encolado y de envío, y afirmar la secuencia completa `List.of(D2, E1, E2)` sobre `sender.sent`, más el cursor final en (occurredAt, eventId) de E2.
- Añadir un caso que fije la compuerta: con el endpoint disabled, `readyEndpoints()` no lo incluye y `EnqueueWebhookDeliveries` no encola nada ni mueve el cursor; tras `changeStatus(..., "active")`, vuelve a incluirlo. Sin este caso, el filtro `e.status='active'` de PostgresWebhookOutbox.java:46 sobrevive a cualquier mutante.

Gravedad: media. No hay indicio de defecto en producción (los tres componentes existen y están probados por separado), pero el escenario que el contrato declara cerrado no tiene oráculo de extremo a extremo y una compuerta del encolador queda a cero cobertura, lo que también castigará la campaña de mutación.

### 19. [MEDIA · minutos] La poda de @s29 cuenta 50 supervivientes pero no comprueba que sean las de mayor updatedAt

- **Dimension:** oraculos
- **Rutas:** backend/src/test/java/com/apptolast/organization/adapter/persistence/WebhookWorkPersistenceTest.java:181-204; backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresWebhookWork.java:120-131

**Evidencia.** Prueba: `assertEquals(52, log.size(), "fifty terminals plus the two pending ones"); assertEquals(50, log.stream().filter(delivery -> "succeeded".equals(delivery.status())).count());` — nada más sobre qué 50 son. Producción: `… ORDER BY updated_at DESC, id DESC LIMIT ?`.

**Arreglo.** Guardar los ids de las 55 terminales en orden y asertar que el conjunto superviviente es exactamente el de los 50 `updatedAt` mayores (y que las 5 primeras han desaparecido).

**Correccion del verificador.** BLOQUEANTE CONFIRMADO, con las rutas y el alcance corregidos.

Titulo: El oraculo de @s29 mide cardinalidad y no identidad: ni las 50 terminales supervivientes ni el orden del listado estan verificados.

Rutas exactas:
- backend/src/test/java/com/apptolast/organization/adapter/persistence/WebhookWorkPersistenceTest.java:180-204 (metodo s29_onlyTheFiftyMostRecentTerminalsSurviveAndPendingOnesAreNeverPruned; los asserts estan en 197-203).
- backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresWebhookWork.java:123-136 (metodo prune; el ORDER BY updated_at DESC, id DESC esta en la linea 131; la constante KEPT_TERMINAL_DELIVERIES = 50 en la linea 30). El rango 120-131 del informe original empieza dentro de disable().
- Ampliacion no vista por el revisor: backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresWebhookStore.java:118-127 (list ordena por updated_at DESC, id DESC) y ese orden tampoco tiene oraculo en ninguna prueba.

Contrato incumplido: features/webhooks.feature:367 "quedan persistidas exactamente 50 terminales, las de mayor updatedAt, y las 2 pendientes" y :368 "items ... ordenados por updatedAt DESC y despues id DESC".

Mutaciones que sobreviven (razonadas, no ejecutadas):
1. PostgresWebhookWork:131 DESC -> ASC. Como prune se ejecuta dentro de cada record(), en las iteraciones 50..54 la fila recien grabada seria la borrada y quedarian los indices 0..49. Total: 50 terminales + 2 pendientes = 52, 50 succeeded, ambos pendientes presentes. Los tres asserts (198, 199-200, 202-203) pasan. Se conservarian las 50 MAS ANTIGUAS y se perderian las recientes, que es justo lo contrario de lo que pide el propietario.
2. PostgresWebhookStore:122 DESC -> ASC. Ninguna prueba compara una lista de mas de un elemento (WebhookPersistenceTest:197 compara List.of(survivor.id()) de un solo elemento; WebhookApiTest:224 y ManageWebhookTest:124 trabajan con una unica entrega mockeada), asi que el orden del listado tampoco esta cubierto.

Correccion minima (dentro del test existente, sin tocar produccion):
- Acumular los ids en el bucle 185-193: var recorded = new ArrayList<UUID>(); ... recorded.add(delivery.id());
- Sustituir el assert 199-200 por la comprobacion de identidad y de orden:
  var terminals = log.stream().filter(d -> "succeeded".equals(d.status())).map(WebhookDelivery::id).toList();
  assertEquals(recorded.subList(5, 55).reversed(), terminals, "las 50 de mayor updatedAt, mas nuevas primero");
  Esto mata las dos mutaciones a la vez: fija QUE 50 sobreviven (mata la del prune) y en QUE orden se listan (mata la del store). Los instantes ya son distintos (T.plusSeconds(index)), asi que no hace falta nada mas.

Gravedad: media. No es un fallo de producto observado ni un placebo total (la prueba si discrimina el LIMIT, la poda de pendientes y la ausencia de poda), pero deja sin oraculo la mitad de identidad de un escenario que el contrato enuncia de forma explicita, y el arreglo cabe en dos lineas de test.

### 20. [MEDIA · minutos] @s40: se cuentan dos botones «Reenviar» en vez de comprobar en qué filas están

- **Dimension:** oraculos
- **Rutas:** frontend/src/webhooks.test.tsx:479-491; frontend/src/webhooks.tsx:502-511

**Evidencia.** `expect(screen.getAllByRole("row")).toHaveLength(4); expect(screen.getAllByRole("button", { name: "Reenviar" })).toHaveLength(2);` y después solo `expect(callsOf(other).some(([u]) => String(u).endsWith("/redeliver"))).toBe(true)`. Producción: `{row.status !== "pending" && (<button …>Reenviar</button>)}`.

**Arreglo.** Localizar cada fila por su `row` y asertar `within(fila).getByRole("button", {name:"Reenviar"})` para succeeded y exhausted y `queryByRole(...)` nulo para la pending; comprobar que la URL del POST contiene el id de la entrega succeeded y que tras la respuesta esa fila muestra «Pendiente» e intento 0.

**Correccion del verificador.** @s40: dos de las tres clausulas de la escena no tienen oraculo en la vista.

Rutas: frontend/src/webhooks.test.tsx:479-491 (prueba); frontend/src/webhooks.tsx:502-511 (guarda) y 240-247 (actualizacion de la fila); contrato en features/webhooks.feature:493-500.

Clausula 2 del contrato — «sólo las filas succeeded y exhausted tienen botón Reenviar y la fila pending no» — se verifica con un TOTAL, no con la asociacion fila-boton: `expect(screen.getAllByRole("button", { name: "Reenviar" })).toHaveLength(2)`. Mutante concreto que sobrevive: cambiar la guarda de produccion `row.status !== "pending"` por `row.status !== "succeeded"`. Los botones pasan a las filas EXHAUSTED y PENDING —justo el caso que el contrato prohibe—, siguen siendo 2, y el clic sobre `[0]` (ahora la fila exhausted) sigue disparando una URL que termina en `/redeliver`, con lo que las dos aserciones pasan. Nota de calibracion: la asercion de conteo no es placebo (mata "eliminar la guarda" -> 3 botones, y "=== succeeded" -> 1 boton); lo que no discrimina es la inversion del literal.

Clausula 3 del contrato — «se envía un POST redeliver y la fila pasa a Pendiente con intento 0» — no tiene ninguna asercion en la vista. El stub de `/redeliver` devuelve `{ delivery: { status: "pending", attempt: 0, … } }` con el id por defecto, y produccion si aplica el cambio en webhooks.tsx:240-247, pero la prueba termina en el `waitFor` sobre la llamada; convertir ese `setDeliveries(...)` en un no-op no rompe nada. Es decir, el efecto visible que el contrato exige no esta observado.

Cobertura parcial que el revisor original no cito y que acota el hallazgo: frontend/src/webhooks-client.test.ts:239-262 («@s40 redelivers a terminal delivery») si fija la URL exacta `/api/v1/me/webhooks/${id}/deliveries/${other}/redeliver` y `reopened.attempt === 0`. Por tanto NO hace falta reclamar cobertura del id a nivel de cliente: lo que falta es el cableado fila->id en la vista. Tampoco hay red E2E: e2e/webhooks-ux.spec.mjs no ejerce «Reenviar».

Correccion minima suficiente (dentro de la misma prueba, sin suite nueva): (a) obtener las filas por su celda de Estado y afirmar `within(filaSucceeded).getByRole("button", { name: "Reenviar" })`, `within(filaExhausted).getByRole("button", …)` y `within(filaPending).queryByRole("button", { name: "Reenviar" })` a `null`; (b) hacer clic en el boton de la fila succeeded obtenido con `within` y afirmar que la URL llamada es exactamente `/api/v1/me/webhooks/<id>/deliveries/<idSucceeded>/redeliver`; (c) tras el reenvio, afirmar que esa misma fila muestra «Pendiente» e intento 0. Con (a) el mutante `!== "succeeded"` muere; con (b) muere pasar `row` equivocado a `redeliver`; con (c) muere anular la actualizacion de estado.

Gravedad: media. Bloquea el cierre porque son dos de las tres clausulas de @s40 sin oraculo, no una preferencia de estilo.

### 21. [MEDIA · una hora] Las pruebas de aislamiento entre webhooks ejercitan un doble que ignora el endpointId, no una regla

- **Dimension:** oraculos
- **Rutas:** backend/src/test/java/com/apptolast/organization/application/ManageWebhookTest.java:223-234; backend/src/test/java/com/apptolast/organization/application/FakeWebhookDeliveries.java:20-22 y :40-42; backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresWebhookStore.java:177-181

**Evidencia.** El doble: `public List<WebhookDelivery> list(String owner, UUID endpointId) { return List.copyOf(stored); }` y `public Optional<WebhookDelivery> find(String owner, UUID endpointId, UUID deliveryId) { return stored.stream().filter(delivery -> delivery.id().equals(deliveryId)).findFirst(); }` — ni `owner` ni `endpointId` intervienen. La prueba `s30_aDeliveryOfAnotherWebhookIsNotFound` pasa `UNKNOWN` como id de ENTREGA, no una entrega existente de otro webhook.

**Arreglo.** Añadir en `WebhookPersistenceTest`/`WebhookRecoveryPersistenceTest` un caso con dos endpoints del mismo propietario y una entrega terminal en el primero: `store.find(owner, segundo.id(), entrega.id())` debe estar vacío y `manage.redeliver(owner, segundo.id(), entrega.id())` debe dar NOT_FOUND. Alternativamente hacer que el doble respete `endpointId`, que además desactiva el falso título.

**Correccion del verificador.** TITULO CORREGIDO: La ultima fila de @s30 (entrega terminal de OTRO webhook -> 404) no tiene oraculo en ninguna capa, y el test que dice cubrirla comprueba otra cosa.

HECHOS (comprobados leyendo, no citados de bitacora):
- features/webhooks.feature:372-385, ultima fila del Scenario Outline @s30: «active | succeeded pero de otro webhook | 404 WEBHOOK_NOT_FOUND».
- backend/.../application/ManageWebhook.java:77-83: redeliver hace requireActive(owner,id) y luego deliveries.find(owner,id,deliveryId).orElseThrow(notFound). El aislamiento por endpoint no se decide aqui.
- backend/.../domain/WebhookDelivery.java: el record no lleva endpointId (2º componente es eventId). La pertenencia de una entrega a un endpoint solo existe en la fila SQL.
- backend/.../adapter/persistence/PostgresWebhookStore.java:177-187: la unica implementacion de la regla es «SELECT * FROM webhook_deliveries WHERE owner_id=? AND endpoint_id=? AND id=?». Su gemela list(owner,endpointId) tambien filtra por endpoint_id.
- backend/src/test/.../application/FakeWebhookDeliveries.java:20-22 y :40-42: el doble ignora owner y endpointId en list y en find. Con ese doble la fila del contrato es inconstruible.
- backend/src/test/.../application/ManageWebhookTest.java:223-234 (s30_aDeliveryOfAnotherWebhookIsNotFound): guarda D succeeded y llama redeliver(OWNER, W, UNKNOWN); UNKNOWN es un id de ENTREGA inexistente. Lo que fija es «id de entrega desconocido -> NOT_FOUND», no «entrega de otro webhook -> 404».

BUSQUEDA DE COBERTURA ALTERNATIVA (agotada, negativa): redeliver aparece ademas en WebhookApiTest.java:251-274 (mockea ManageWebhookUseCase, un solo endpoint) y en WebhookRecoveryPersistenceTest.java:220-268 (@s31, un solo endpoint). En frontend, webhooks.test.tsx:488 y webhooks-client.test.ts:239-261 mockean fetch. Ningun test de persistencia crea dos endpoints con entregas vivas a la vez: WebhookPersistenceTest.s13:177-198 es el unico con dos endpoints, pero borra las entregas del primero antes de listar, de modo que un list sin endpoint_id seguiria verde. Ninguna prueba pasa a find/list un endpointId distinto del dueno de la entrega.

CONSECUENCIA (razonada estaticamente; NO he ejecutado nada): eliminar «AND endpoint_id=?» (y su parametro) de PostgresWebhookStore:180 no rompe ninguna prueba del repositorio. Lo mismo vale para el filtro por endpoint_id del list de entregas: tampoco tiene oraculo.

CORRECCIONES AL BLOQUEANTE ORIGINAL:
1) La prueba de :223-234 NO es un placebo. Es la unica que ejercita el orElseThrow(ManageWebhook::notFound) del lookup de ENTREGA: sustituirlo por un valor por defecto o por otro Code la rompe, mientras que s11_s13_readsStatusDeleteAndRedeliverAllReportNotFoundForAnUnknownId (:161-172) no lo notaria porque pasa UNKNOWN como ENDPOINT y falla antes en requireActive. El defecto es el titulo, que promete una conducta que el cuerpo no ejercita.
2) El impacto esta sobredimensionado como «fuga entre webhooks». requeue (PostgresWebhookStore:189-207) conserva su propio «AND owner_id=? AND endpoint_id=? AND id=?», y owner_id sigue en find. Por tanto, con el predicado suprimido en find, el efecto seria: mismo propietario, endpoint A devuelve 202 con la entrega de su endpoint B marcada como pending en la respuesta, mientras el UPDATE afecta a 0 filas y la base no cambia. Es una respuesta mentirosa y una confusion entre endpoints del mismo dueno, no un cruce entre inquilinos ni un reenvio ajeno efectivo.

GRAVEDAD: media (se mantiene). Fila del contrato sin oraculo + predicado SQL sin cobertura; sin cruce entre propietarios.

CIERRE EXIGIBLE (minimo): un test en WebhookPersistenceTest (o WebhookRecoveryPersistenceTest) que, con un unico owner, inserte dos endpoints A y B, encole/persista una entrega terminal en B, y afirme que store.find(owner, A.id(), entregaDeB.id()) esta vacia y que new ManageWebhook(...).redeliver(owner, A.id(), entregaDeB.id()) lanza Code.NOT_FOUND, mas que store.list(owner, A.id()) no contiene esa entrega. Y renombrar ManageWebhookTest:224 a algo veraz, p. ej. s30_anUnknownDeliveryIdIsNotFound.

### 22. [MEDIA · minutos] @s37: «Copiar» solo se comprueba visible; la cláusula «no se copia sin activar Copiar» no tiene oráculo

- **Dimension:** oraculos
- **Rutas:** frontend/src/webhooks.test.tsx:213-225; frontend/src/webhooks.tsx:291-296

**Evidencia.** `expect(screen.getByRole("button", { name: "Copiar" })).toBeVisible();` junto a `expect(JSON.stringify(localStorage)).not.toContain(secret); … expect(window.location.href).not.toContain(secret);`. Producción: `onClick={() => void navigator.clipboard?.writeText(secret)}`.

**Arreglo.** Stub de `navigator.clipboard.writeText` con `vi.fn()`: asertar que NO se ha llamado tras mostrarse el secreto y que se llama exactamente una vez con el secreto tras activar «Copiar».

**Correccion del verificador.** @s37 (feature 25, webhooks): el portapapeles, único canal que el contrato condiciona, no tiene oráculo.

Ruta: frontend/src/webhooks.test.tsx:213-225 (prueba «@s37 sends one POST with the twelve types and shows the secret once»); producción frontend/src/webhooks.tsx:291-296.

Hecho comprobado: features/webhooks.feature:461 exige «no se escribe whsec_x en localStorage, sessionStorage ni en la URL y no se copia sin activar Copiar». De los cuatro canales, tres tienen aserción (localStorage, sessionStorage, window.location.href, líneas 222-224) y el cuarto —el portapapeles, el único con condición— no se observa en ningún punto del carril: `navigator.clipboard` / `writeText` no aparece ni en webhooks.test.tsx, ni en webhooks-route.test.tsx, ni en webhooks-client.test.ts, ni en e2e/webhooks-ux.spec.mjs, ni en ningún setup global. La única aserción sobre el botón es que se ve.

Cambios de producto que deberían romper una prueba y hoy no rompen ninguna:
  (a) añadir `useEffect(() => void navigator.clipboard?.writeText(secret), [secret])` — copia el secreto sin gesto, viola la letra de la cláusula, suite verde;
  (b) borrar el `onClick` de la línea 293 y dejar el botón decorativo — el usuario pierde el secreto irrecuperable, suite verde. Este segundo es además un mutante que la campaña de Stryker dejará vivo sobre webhooks.tsx.

Listón ya fijado por el propio repositorio: la feature 24, mismo tag @s37 y mismo patrón de secreto de un solo uso, cubre esto en frontend/src/integration-api.mutation.test.tsx:86-119 con `expect(write).not.toHaveBeenCalled()` antes del clic y `expect(write).toHaveBeenCalledExactlyOnceWith(secret)` después. La feature 33 hace lo mismo en frontend/src/calendar.test.tsx:233-258. No es criterio nuevo ni preferencia de estilo: es el estándar vigente en dos carriles ya integrados, ausente solo en éste.

Corrección al bloqueante original: se RETIRA la acusación de que las tres aserciones de ausencia son placebo. Sí discriminan — frontend/src/import-data-intent.ts:4-39 y frontend/src/integration-api-intent.ts:4-28 muestran que esta aplicación persiste payloads de formulario en sessionStorage como patrón establecido, así que un `webhooks-intent.ts` calcado de ese precedente sería cazado por la línea 223. Manténganse tal cual.

Remedio mínimo, dentro del contrato y sin ampliar alcance (el contrato de webhooks, a diferencia de features/integration_api.feature:431 y features/ics_calendar.feature:425, NO exige anuncio de éxito ni rama de fallo, así que no se piden): en la prueba de webhooks.test.tsx, espiar `navigator.clipboard.writeText`, aserir `not.toHaveBeenCalled()` con el panel del secreto ya visible, pulsar «Copiar», y aserir `toHaveBeenCalledExactlyOnceWith(secret)`. Cuatro líneas en la prueba que ya existe.

Gravedad: media. Bloqueante para el dictamen del juez, y previsiblemente también para la puerta de mutación por el mutante (b).

### 23. [MEDIA · minutos] La cifra de unitarios de la matriz no es reproducible: declara 58 y he contado 44

- **Dimension:** accesibilidad
- **Rutas:** progress/ux_webhooks.md:4-5; frontend/src/webhooks.test.tsx; frontend/src/webhooks-route.test.tsx; frontend/src/webhooks-client.test.ts

**Evidencia.** La matriz abre con «Unitarios de la vista y del cliente: 58 verdes» (ux_webhooks.md:5). Contando declaraciones en los ficheros: webhooks.test.tsx tiene 21 `it(` de primer nivel más un `it.each([...])` de dos casos (línea 512-515) = 23 casos; webhooks-route.test.tsx, 3; webhooks-client.test.ts, 18. Total 44. No hay `describe` anidados ni `it` indentados en ninguno de los tres (grep de `^\s+(it|test)\(` vacío), y ningún otro fichero de pruebas del frontend menciona «webhook». No he ejecutado vitest, por la regla de esta revisión: cuento declaraciones. La cifra E2E, en cambio, sí cuadra: 2 temas + 3 modos + 1 teclado = 6 tests, y hay seis carpetas de evidencia.

**Arreglo.** Recontar con la salida real de vitest sobre los tres ficheros y corregir la cifra en ux_webhooks.md:5, o indicar exactamente qué comando y qué filtro produjeron los 58 (si incluía ficheros ajenos a webhooks, decirlo).

**Correccion del verificador.** TÍTULO: progress/ux_webhooks.md:5 declara 58 unitarios verdes; los ficheros contienen 44, y nunca contuvieron 58

GRAVEDAD: baja (defecto documental del artefacto de puerta, no hueco de cobertura). No devuelve la feature al artesano: es una corrección de una línea en progress/, editable por el coordinador.

RUTAS: progress/ux_webhooks.md:5; progress/tdd_webhooks.md:371; frontend/src/webhooks.test.tsx; frontend/src/webhooks-route.test.tsx; frontend/src/webhooks-client.test.ts

EVIDENCIA COMPROBADA POR LECTURA (sin ejecutar vitest, conforme a la regla de esta revisión):
- webhooks.test.tsx: 21 `it(` en columna 0 + un `it.each([...])` de dos filas (líneas 512-515: CONNECTORS_DISABLED y WEBHOOK_LIMIT) = 23 casos.
- webhooks-route.test.tsx: 3 casos.
- webhooks-client.test.ts: 18 casos.
- TOTAL 44. Sin describe anidados, sin it/test indentados, sin generador de pruebas.
- Ningún otro fichero de pruebas del frontend contiene tests de webhooks: integration-api.test.tsx:821 lo menciona sólo en un comentario.
- Corroboración independiente: progress/tdd_webhooks.md:371 declara «Frontend: 42 tests propios de webhooks», escrito antes del commit de auditoría 99f1e94, que añadió los dos tests de @s42 (retorno de foco al cancelar y nombres accesibles) → 44.
- `git show 99f1e94:frontend/src/webhooks*.test.*` da 22/3/18, idéntico a HEAD: la cifra 58 no era reproducible ya el día en que se escribió la matriz. No es cobertura perdida en la integración.

LO QUE NO ES ESTE HALLAZGO (para que el siguiente revisor no lo sobredimensione):
- No hay pérdida de cobertura. Los 44 tests cubren @s36–@s42 y el commit de auditoría añadió, no quitó.
- La cifra E2E de la misma línea SÍ es correcta: 6 = 2 temas (e2e/webhooks-ux.spec.mjs:330) + 3 modos (:344) + 1 teclado (:372). ATENCIÓN: esto se verifica leyendo el spec, NO contando carpetas de evidencia. `.e2e-work/` está en .gitignore:56 y en este árbol de trabajo no existe `.e2e-work/webhooks-ux/`; se generó en el worktree C:/Users/vhurt/ow-worktrees/webhooks. Cualquier dictamen que cite «seis carpetas de evidencia» como comprobado en main está afirmando algo que no puede haber visto.
- Ninguna de las 30 filas de la matriz de docs/ux-requirements.md deriva su veredicto de la cifra 58; el preámbulo es contexto, no oráculo.

ARREGLO EXIGIDO (documental, sin tocar src/ ni tests): sustituir en progress/ux_webhooks.md:5 «58 verdes» por la cifra recontable y su desglose, p. ej. «Unitarios de la vista y del cliente: 44 (webhooks.test.tsx 23, incluido un it.each de 2 filas; webhooks-client.test.ts 18; webhooks-route.test.tsx 3)». El desglose por fichero es la parte importante: hace la cifra auditable sin ejecutar la suite, que es justo lo que hoy ha fallado dos veces en este proyecto.

