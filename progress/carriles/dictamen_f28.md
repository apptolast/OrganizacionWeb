## Feature 28-external_calendar

19 hallazgos confirmados, de los que **7 son bloqueantes**. 4 se cierran en minutos.

### 1. [BLOQUEANTE · varias horas] @s40: la auditoría solo mira el estado vacío, el zoom nativo 200 % no se ejecuta y no existe la matriz UX

- **Dimension:** contrato
- **Rutas:** e2e/external-calendar-ux-audit.spec.mjs:45-55, :57-87, :123-134, :16-22; progress/ (ausencia de ux_external_calendar.md)

**Evidencia.** El Given de @s40 (features/external_calendar.feature:542) exige cinco estados: «vacío, con suscripción, con error, con lista larga de resúmenes Unicode y guardando». La prueba de la matriz de anchos (línea 45) hace `await page.goto("/calendario-externo"); await expect(page.getByLabel("Etiqueta")).toBeVisible();` y recorre los 14 anchos SOLO en ese estado vacío; la de texto al 200 % (línea 123) hace exactamente lo mismo. axe (línea 57) cubre vacío, con suscripción y tras sincronizar: nunca «con lista larga de resúmenes Unicode» (imposible en E2E, la propia bitácora admite en tdd_external_calendar.md:440 que ninguna E2E llega a lastStatus OK) ni «guardando». El contrato nombra «zoom nativo 200 %» (línea 543) y en todo el fichero no hay ni `setZoom` ni `deviceScaleFactor`: la línea 128 solo hace `document.documentElement.style.fontSize = "32px"`, que es texto ampliado, no zoom del navegador; e2e/appearance-ux-audit.spec.mjs:94 y e2e/automations-ux.spec.mjs:271 sí lo hacen con `chrome.tabs.setZoom`. El oráculo de recorte (líneas 16-22) solo compara `document.documentElement.scrollWidth` con `clientWidth`: no detecta solapes ni recorte vertical, y el Then los exige («no hay solapes, recortes ni scroll horizontal»). Por último, `ls progress/` no devuelve ningún ux_external_calendar.md, mientras que progress/ux_ics_calendar.md documenta 7 estados × 3 anchos y dice explícitamente «Este documento no infiere cumplimiento a partir de axe (AGENTS.md:51)».

**Arreglo.** Parametrizar la prueba de anchos y la de texto al 200 % por los cinco estados del Given (vacío, con suscripción, con error —forzando lastStatus FAILED por interceptación de GET /api/v1/me/external-calendar—, con lista larga de resúmenes Unicode —interceptando GET /events con muchos items— y guardando —reteniendo la respuesta del PUT—); añadir la prueba de zoom nativo al 200 % con contexto persistente y chrome.tabs.setZoom, copiando e2e/automations-ux.spec.mjs:201-297; ampliar el oráculo geométrico para detectar solapes entre cajas y recorte vertical (scrollHeight/clientHeight y comparación de rectángulos), no solo scrollWidth; y escribir progress/ux_external_calendar.md con la matriz de los 30 principios de docs/ux-requirements.md diciendo qué se midió con qué oráculo.

**Correccion del verificador.** Título corregido: @s40 tiene la mitad semántica cubierta, pero la geométrica solo en el estado vacío, sin zoom nativo y sin progress/ux_external_calendar.md.

Lo que SÍ está cubierto (y el bloqueante original omitió): frontend/src/external-calendar.test.tsx aporta 23 pruebas, dos explícitamente @s40 — :397 verifica el orden de tabulación Etiqueta → Dirección secreta iCal → Guardar → Sincronizar ahora → Eliminar suscripción, y :415 verifica que la región `role=status` con `aria-live="polite"` anuncia sin mover el foco. Los estados «guardando» (:181), «con error» (:153, :168, :309, :384) y la lista de eventos (:132) tienen oráculo de comportamiento y semántica en jsdom. La E2E :89-121 repite el orden de teclado y el foco visible en navegador real sobre el estado «con suscripción». Nada de esto hay que rehacer.

Los tres huecos que quedan, todos verificados leyendo:

(a) Geometría en un solo estado. Las dos pruebas que miden anchos son :45-55 (14 anchos, ambos lados de cada breakpoint) y :123-134 (texto al 200 % a 320 px), y ambas arrancan con `goto` + `expect(getByLabel("Etiqueta")).toBeVisible()`: miden el formulario de alta vacío y nada más. La de axe (:57-87) sí recorre vacío → con suscripción → tras sincronizar (este último es de hecho el estado de fallo, porque el feed de calendar.google.com no es alcanzable desde el contenedor), pero lo hace en el viewport por defecto, sin recorrer ni un ancho. Falta la combinación estado × ancho para «con suscripción» y «con error», que sí son alcanzables en E2E. El estado «con lista larga de resúmenes Unicode» es inalcanzable con la guardia SSRF activa (progress/tdd_external_calendar.md lo admite): o se cubre interceptando `GET …/external-calendar/events` con una respuesta larga —como ics-calendar-ux.spec.mjs provoca `cargando` y `fallo` interceptando su GET—, o se declara como limitación aceptada y firmada en el documento UX, pero no se deja en silencio.

(b) Oráculo de recorte insuficiente. `noHorizontalScroll` (:16-22) solo compara `documentElement.scrollWidth` con `clientWidth`. El Then exige además «no hay solapes, recortes». Este mismo oráculo, de ancho solamente, es el que en la feature hermana ocultó un recorte vertical real (progress/ux_ics_calendar.md: textarea de 282 px de contenido en 153 visibles). Debe medir los dos ejes sobre los descendientes de `main` con `overflow` distinto de `visible`, y comprobar que ningún control queda con `right > clientWidth + 1`, como ya hace ics-calendar-ux.spec.mjs.

(c) Zoom nativo y matriz. `grep -rn "setZoom\|deviceScaleFactor" e2e/` da 19 ficheros y ninguno es este; la :128 hace `fontSize = "32px"`, que cubre «texto ampliado 200 %» pero no «zoom nativo 200 %», y el When nombra los dos por separado. El patrón está resuelto en el repo (appearance-ux-audit.spec.mjs:98, automations-ux.spec.mjs:273, ics-calendar-ux.spec.mjs:562: `chrome.tabs.setZoom` desde el service worker de la extensión efímera). Y falta progress/ux_external_calendar.md con las 30 filas de docs/ux-requirements.md diciendo qué se midió, con qué oráculo y dónde acaba lo automático: AGENTS.md:51 lo exige y prohíbe expresamente inferirlo de axe. Es el único de los cinco carriles integrados hoy sin ese documento (existen ux_integration_api.md, ux_webhooks.md, ux_ics_calendar.md, ux_github_connector.md y ux_automations.md).

Gravedad: bloqueante, pero solo (a)+(b)+(c); el orden de teclado, el foco visible, los 44×44 px y los anuncios sin robo de foco ya están demostrados y no deben rehacerse.

### 2. [BLOQUEANTE · minutos] La prueba de cancelación al desmontar no comprueba ninguna cancelación

- **Dimension:** oraculos
- **Rutas:** frontend/src/external-calendar.test.tsx:376-382 (producción: frontend/src/external-calendar.tsx:160)

**Evidencia.** it("@s39 cancela la petición en curso al desmontar la vista", async () => { withoutSubscription(); const view = render(<ExternalCalendar />); view.unmount(); await new Promise((resolve) => setTimeout(resolve, 0)); expect(document.body.textContent).toBe(""); });

**Arreglo.** Copiar el patrón que ya existe en frontend/src/today-external-calendar.test.tsx:248-276: acumular las `options.signal` entregadas a `fetch`, comprobar que ninguna está abortada antes del `unmount()` y que todas lo están después, y que la respuesta tardía no repinta.

**Correccion del verificador.** Bloqueante REAL, con el alcance corregido.

Título correcto: «La prueba @s39 de desmontaje no tiene oráculo de cancelación: la línea 160 de producción sobrevive a su borrado».

Rutas: frontend/src/external-calendar.test.tsx:376-382 (oráculo ausente); frontend/src/external-calendar.tsx:160 (`return () => controller.abort();`, sin cobertura); contrato features/external_calendar.feature:534 (@s39, fila 3).

Hecho comprobado leyendo: la única aserción del test es `expect(document.body.textContent).toBe("")` después de `view.unmount()`. El doble de fetch del propio fichero (líneas 75-88) no mira `options.signal`, de modo que la petición resuelve exista o no el abort; con React 19 el setState posterior sobre la vista desmontada se descarta sin aviso; y frontend/src/test-setup.ts no eleva ningún aviso de consola a fallo. Conclusión: borrar la línea 160 deja la prueba verde. Ninguna otra prueba del proyecto desmonta ExternalCalendar con una petición en vuelo.

Alcance corregido (esto es lo que el hallazgo original exagera): solo queda sin oráculo la FILA 3 de @s39. La fila 4 («cierro sesión → desaparecen host, cola, contadores y lista, la ruta se reinicia a /») sí está cubierta por external-calendar.test.tsx:266-282 («@s38 retira los datos privados cuando la sesión ha caducado»), que responde 401 al PUT y comprueba que desaparecen host y lista y que el campo de dirección queda vacío. No la incluyas en el bloqueante.

Contexto de bitácora (citado, no comprobado ejecutando): progress/tdd_external_calendar.md ciclo 16 (líneas 213-215) afirma que quitar el abort del desmontaje hace caer 3 pruebas de esta pantalla y habla de «24 pruebas»; el fichero tiene 22 `it(` contados por mí, y el ciclo 18 (líneas 244-247) documenta lo contrario sobre el mismo mutante. Trata el ciclo 16 como no fiable.

Remedio mínimo (barato, no es rediseño): reescribir 376-382 con la misma técnica ya usada en today-external-calendar.test.tsx:251-272 — acumular en un array las `options.signal` que recibe el fetch simulado, esperar a que haya al menos una sin abortar, desmontar y exigir `signals.every((s) => s.aborted)`; y, para cerrar la segunda mitad de la fila («su respuesta tardía no modifica la vista destino»), resolver la respuesta después del desmontaje y comprobar que no se produce ninguna llamada ni escritura sobre la vista destino. Sin ese cambio, la fila 3 de @s39 no está demostrada y el título del test miente sobre lo que verifica, que es exactamente el motivo por el que el juez rechazó hoy la prueba «se cancela con Escape» y la de `expect(states).toHaveLength(7)`.

### 3. [BLOQUEANTE · varias horas] No existe progress/ux_external_calendar.md: la matriz de los 30 principios no se ha hecho

- **Dimension:** accesibilidad
- **Rutas:** progress/ux_external_calendar.md (ausente); AGENTS.md:51; docs/ux-requirements.md:7

**Evidencia.** `ls progress/ux_external_calendar.md` -> «No such file or directory», y `git log --oneline --all -- progress/ux_external_calendar.md` no devuelve NINGUN commit: el fichero no ha existido jamas. Hay 21 ficheros progress/ux_*.md para otras features y ninguno para esta. AGENTS.md:51 dice literalmente: «Para disenar, implementar o revisar cualquier interfaz, leer docs/ux-requirements.md: matriz completa de los 30 principios Laws of UX... No declarar cumplimiento global por pasar unicamente axe o unas pocas resoluciones». docs/ux-requirements.md:7: «Toda feature con interfaz revisara las 30 filas... Ninguna fila se omite». El contrato remata en features/external_calendar.feature:543: «se revisa segun la matriz de docs/ux-requirements.md».

**Arreglo.** Escribir progress/ux_external_calendar.md siguiendo el formato de progress/ux_integration_api.md: primero una seccion «Evidencia y limites» ANTES de los resultados (que specs se ejecutaron, en que motores, que capturas se inspeccionaron y que NO se afirma), y despues una tabla con exactamente una fila por cada uno de los 30 principios de docs/ux-requirements.md:13-42, sin saltarse ninguna, distinguiendo en la columna de resultado lo medido («Verificado geometricamente», «Verificado en navegador») de lo heuristico («revision heuristica, sin estudio con usuarios») y de lo no aplicable con motivo. Los precedentes tienen 32 lineas `^| ` (cabecera + separador + 30 filas); este debe tener las mismas 32.

**Correccion del verificador.** TÍTULO CORREGIDO: La matriz de los 30 principios de docs/ux-requirements.md no existe para external_calendar en ningún fichero; la única evidencia UX es la auditoría axe/geometría, que AGENTS.md:51 declara insuficiente.

GRAVEDAD: bloqueante (se mantiene).

RUTAS: features/external_calendar.feature:540-548 (@s40; la línea 543 invoca la matriz por nombre); AGENTS.md:51; docs/ux-requirements.md:7 y su tabla de 30 filas; e2e/external-calendar-ux-audit.spec.mjs (única evidencia aportada, 134 líneas, 4 tests); progress/tdd_external_calendar.md (546 líneas, sin ninguna fila de la matriz).

EVIDENCIA CORRECTA (sustituye a la del bloqueante original): no basta con que falte progress/ux_external_calendar.md, porque ese nombre no es la puerta: nueve features ya en 'done' carecen de ese fichero exacto (create_project, publish_outbox, schedule_block, today, reschedule, end_time_notification, appearance, custom_views_fields, import_data) y varias llevan la matriz bajo otro nombre (progress/ux_schedule_block_frontend.md, progress/tdd_today_e2e.md, progress/review_reschedule_ux_principles.md, progress/tdd_end_time_ux.md, progress/tdd_appearance_ux_audit.md, progress/review_import_data_browser.md, progress/explore_custom_views_fields_ui.md). La prueba real es la búsqueda por contenido: los nombres propios de la matriz (Zeigarnik, Tesler, Pareto, Fitts, Hick, Jakob, Miller, Parkinson, Occam, Von Restorff, Atención selectiva, Carga cognitiva, Estética) aparecen en 30 ficheros del repo y en ninguno de external_calendar; docs/external-calendar.md da cero coincidencias para «principio», «ux-requirements», «Laws» y «matriz»; y `git log --all --diff-filter=AD --name-only -- progress/*external*` muestra que solo han existido tres ficheros (tdd, gherkin, proposal), así que no hay matriz borrada ni renombrada.

QUÉ FALTA PARA LEVANTARLO: un fichero (nombre sugerido progress/ux_external_calendar.md, por coherencia con los 21 existentes) con las 30 filas de docs/ux-requirements.md, cada una con aplicación concreta en /calendario-externo, evidencia y resultado verificado/pendiente/no-aplicable-con-motivo, sin omitir ninguna fila, más la revisión manual con lector de pantalla que el propio comentario del spec (≈línea 84 de e2e/external-calendar-ux-audit.spec.mjs) reconoce como obligatoria y que aún no consta hecha.

DOS HUECOS ADYACENTES DEL MISMO @s40 que encontré leyendo y que conviene levantar en el mismo ciclo (son motivos por los que el juez rechazó features hoy):
- Estados: el contrato (features/external_calendar.feature:541) nombra cinco estados —vacío, con suscripción, con error, con lista larga de resúmenes Unicode y guardando—. El test de axe solo analiza tres (vacío, con suscripción, tras sincronizar). «Con error» y «guardando» no se auditan nunca.
- Zoom nativo: el contrato (línea 543) exige «zoom nativo 200 %» además de texto ampliado 200 %. En e2e/external-calendar-ux-audit.spec.mjs no hay ninguna aparición de «zoom», «deviceScaleFactor» ni setDeviceMetrics; el único test al 200 % (línea 123) solo hace `document.documentElement.style.fontSize = "32px"`, que es texto ampliado, no zoom del navegador. El zoom nativo del contrato no se ejecuta jamás.

### 4. [BLOQUEANTE · varias horas] El zoom NATIVO del navegador al 200 % no se ejecuta nunca, aunque el contrato lo nombra

- **Dimension:** accesibilidad
- **Rutas:** features/external_calendar.feature:543; e2e/external-calendar-ux-audit.spec.mjs (fichero completo, 135 lineas)

**Evidencia.** El contrato dice en la linea 543: «a 320, 768, 1280 y 2560 px CSS, **zoom nativo 200 %** y texto ampliado 200 %». `grep -n -i -E "deviceScaleFactor|forced-colors|forcedColors|colorScheme|prefers-reduced|reducedMotion|emulateMedia|zoom"` sobre e2e/external-calendar-ux-audit.spec.mjs y e2e/external-calendar.spec.mjs devuelve CERO coincidencias. `ls e2e | grep -i external` devuelve solo external-calendar.spec.mjs y external-calendar-ux-audit.spec.mjs: no hay ningun external-calendar-native-zoom.spec.mjs, cuando el repo si tiene github-connector-native-zoom.spec.mjs, reschedule-native-zoom.spec.mjs, today-native-zoom.spec.mjs y start-work-session-native-zoom.spec.mjs. La unica prueba de ampliacion es la de la linea 123-134, que hace `document.documentElement.style.fontSize = "32px"`: eso es texto ampliado, no zoom. El propio repo lo deja por escrito en e2e/github-connector-native-zoom.spec.mjs:13-16: «El zoom nativo de Chromium se aplica con chrome.tabs.setZoom desde una extension efimera, que es la unica forma de ampliar de verdad: deviceScaleFactor cambia la densidad, no el zoom, y no reproduce el reflujo que este escenario quiere medir».

**Arreglo.** Anadir e2e/external-calendar-native-zoom.spec.mjs calcado del mecanismo ya validado en e2e/github-connector-native-zoom.spec.mjs (extension efimera de Chromium + chrome.tabs.setZoom al 200 %, comprobando que el DPR se duplica de verdad antes de medir), cubriendo al menos 320, 768 y 1280 px CSS y comprobando ausencia de desplazamiento horizontal y de contenido recortado. Declarar en el fichero, como hace el precedente, que solo se afirma zoom nativo para Chromium.

**Correccion del verificador.** BLOQUEANTE (confirmado, con dos precisiones y un agravante).

Título: El zoom nativo del navegador al 200 % que exige el contrato no se ejecuta nunca en /calendario-externo, y la auditoría UX completa nunca se ha ejecutado.

Rutas exactas:
- `features/external_calendar.feature:543` (exigencia)
- `e2e/external-calendar-ux-audit.spec.mjs` (fichero completo, **134** líneas — no 135)
- `e2e/external-calendar.spec.mjs` (sin zoom)
- `docs/ux-requirements.md:50` (norma que lo hace innegociable)
- `progress/tdd_external_calendar.md:287-291` y `:343` (confesión del artesano)

Evidencia verificada:
1. Contrato: la 543 exige «zoom nativo 200 % **y** texto ampliado 200 %» — dos condiciones, no una. Es la única mención de zoom en las 5xx líneas del .feature.
2. Sólo la segunda condición tiene prueba: el test de líneas 123-134 pone `document.documentElement.style.fontSize = "32px"` a 320 px de ancho. Zoom nativo: cero coincidencias en ambos specs para deviceScaleFactor|emulateMedia|zoom|setZoom (grep con EXIT 1).
3. Ninguna otra prueba cubre el hueco: `grep -rln "calendario-externo"` en todo el repo da sólo esos dos specs; los cuatro `*-native-zoom.spec.mjs` existentes navegan cada uno a una ruta fija distinta y no están parametrizados; `e2e/support/` no tiene helper de zoom.
4. La norma del proyecto lo prohíbe explícitamente sustituirlo: `docs/ux-requirements.md:50` — «La emulación de ancho equivalente no sustituye toda la comprobación de zoom real». Y `e2e/github-connector-native-zoom.spec.mjs:13-16` documenta que `chrome.tabs.setZoom` desde extensión efímera «es la única forma de ampliar de verdad».
5. Riesgo concreto: `.external-calendar` colapsa a una columna por debajo de 520 px CSS (`progress/tdd_external_calendar.md:286`). El zoom nativo al 200 % sobre 1024 px deja un viewport CSS efectivo de 512 px, justo al otro lado de ese breakpoint, y arrastra paddings y media queries que el `font-size` del root no toca. Es exactamente el reflujo que nadie ha mirado.

AGRAVANTE que el hallazgo original no recoge (y que eleva el alcance del arreglo): `progress/tdd_external_calendar.md:343` marca la fila @s40 como «`e2e/external-calendar-ux-audit.spec.mjs` (sin ejecutar)», y las líneas 287-291 confirman que las dos specs E2E «no [han sido] ejecutadas… Solo se han validado con `node --check` y prettier». Por tanto NO existe evidencia de ejecución de NINGUNA de las cuatro pruebas de @s40, no sólo del zoom ausente. Cerrar esto no es «añadir un test»: es añadir el test de zoom nativo Y ejecutar la auditoría entera con evidencia registrada.

Qué hace falta para levantarlo:
- Crear `e2e/external-calendar-native-zoom.spec.mjs` con el mismo mecanismo de extensión efímera + `chrome.tabs.setZoom(tab.id, 2)` que usan reschedule/today/github-connector, cubriendo al menos 320, 768 y 1280 px CSS al 200 % y los estados del Given de la 542 (vacío, con suscripción, con error, lista larga Unicode, guardando).
- El oráculo no puede ser el actual `noHorizontalScroll` (líneas 16-22), que sólo compara `scrollWidth` con `clientWidth`: bajo zoom el recorte típico es vertical y por contenedor. Hay que medir también recorte por elemento (rect del hijo contra rect del contenedor con overflow oculto), o repetimos el sexto motivo de rechazo de hoy.
- Ejecutar la auditoría y dejar la evidencia; y por separado sigue faltando `progress/ux_external_calendar.md` con la matriz de los 30 principios (no existe en `progress/`; AGENTS.md:51 prohíbe inferir cumplimiento desde axe) — hallazgo distinto pero de la misma escena.

### 5. [BLOQUEANTE · varias horas] La matriz geometrica solo audita 1 de los 5 estados de pantalla que exige el contrato

- **Dimension:** accesibilidad
- **Rutas:** features/external_calendar.feature:542; e2e/external-calendar-ux-audit.spec.mjs:45-55 y :123-134

**Evidencia.** El contrato enumera cinco estados en la linea 542: «/calendario-externo en estados vacio, con suscripcion, con error, con lista larga de resumenes Unicode y guardando». La prueba de la matriz de catorce anchos (linea 45-55) hace exactamente esto y nada mas: `await page.goto("/calendario-externo"); await expect(page.getByLabel("Etiqueta")).toBeVisible();` y a continuacion el bucle `for (const width of widths)`. Es decir, recorre los catorce anchos SOLO sobre el estado vacio. La prueba de texto al 200 % (linea 123-134) hace lo mismo: goto, espera «Etiqueta», mide. Nunca guarda, nunca sincroniza, nunca hay lista. Los estados «con error», «con lista larga de resumenes Unicode» y «guardando» no se construyen en NINGUNA de las cuatro pruebas del fichero: no hay page.route ni ninguna retencion de peticion que permita capturar «guardando». Y el estado con lista es estructuralmente inalcanzable: la propia bitacora lo admite en progress/tdd_external_calendar.md:440-442: «ninguna prueba de extremo a extremo llega a una sincronizacion con lastStatus OK, porque no hay ningun feed ICS alcanzable desde el contenedor con la guardia SSRF activada».

**Arreglo.** Parametrizar la prueba de los catorce anchos por los cinco estados del contrato, preparando cada uno de forma determinista sin depender de un feed real: sembrar la suscripcion y la instantanea por SQL (el fichero ya importa `sql` de ./support/projects.mjs y lo usa en la linea 13) para los estados «con suscripcion», «con error» (lastStatus FAILED con lastError FEED_HTTP_ERROR) y «lista larga de resumenes Unicode» (por ejemplo 60 eventos con resumenes de 500 puntos de codigo, emoji y palabras sin espacios), y usar page.route con una promesa retenida sobre PUT /api/v1/me/external-calendar para capturar «guardando». Ejecutar en los cinco estados la geometria, el 44x44 y el texto al 200 %, y ejecutar axe tambien en el estado de error y en el de lista larga.

**Correccion del verificador.** TITULO: La matriz geometrica del @s40 solo se ejerce sobre el estado vacio, y los dos controles que el contrato nombra nunca se miden a 44x44 px

RUTAS: features/external_calendar.feature:542, 544 y 546; e2e/external-calendar-ux-audit.spec.mjs:45-55, :57-87 y :123-134; frontend/src/external-calendar.tsx:327-373.

EVIDENCIA COMPROBADA (leida, no citada de bitacora):
1. El contrato (linea 542) fija cinco estados: vacio, con suscripcion, con error, con lista larga de resumenes Unicode y guardando. Las lineas 544 y 546 exigen sobre esos estados, en los catorce anchos, ausencia de solapes/recortes/scroll horizontal y controles de >= 44x44 px.
2. La prueba de la matriz (45-55) hace `goto("/calendario-externo")`, espera "Etiqueta" y entra directamente en `for (const width of widths)`. Solo estado vacio. La prueba de texto al 200 % (123-134) hace lo mismo a 320 px. Ninguna de las cuatro pruebas del fichero usa `page.route` ni retiene peticion alguna, luego "guardando" no se congela nunca.
3. Consecuencia mas grave que la alegada: en frontend/src/external-calendar.tsx:327-373 los botones "Sincronizar ahora" y "Eliminar suscripcion" viven dentro de `{subscription ? ...}`, y "Si, eliminar"/"Cancelar" dentro de `{confirming ? ...}`. Como `controlsAreLargeEnough` solo consulta `.external-calendar button, .external-calendar input` en el estado vacio, los unicos nodos medidos son los dos inputs y "Guardar". Los tres controles que el propio contrato enumera en las lineas 545 y 546 (Sincronizar ahora, Eliminar suscripcion, y los del dialogo de confirmacion) no se miden a 44x44 px en ningun ancho ni en ninguna prueba. La comprobacion de 44 px del contrato esta, en la practica, sin ejercer para la mayoria de los controles.
4. La lista larga de resumenes Unicode -el estado que el contrato senala precisamente porque es el que puede desbordar- no se mide en ningun ancho.

CORRECCIONES AL BLOQUEANTE ORIGINAL (ambas amplian el alcance, no lo reducen):
a) Es falso que el estado "con error" no se construya en ninguna prueba. La prueba de axe (78-83) pulsa "Sincronizar ahora" contra un feed inalcanzable; segun progress/tdd_external_calendar.md:440-442 eso termina siempre en lastStatus FAILED, y external-calendar.tsx:352-356 pinta entonces el `role="alert"` del codigo de error. El estado SI se alcanza: lo que falta es medirlo geometricamente. Lo mismo vale para "con suscripcion", que se construye en las pruebas 2 y 3 pero solo se audita con axe y con teclado, nunca con la matriz de anchos.
b) Es falso que el estado con lista sea "estructuralmente inalcanzable". e2e/support/projects.mjs expone `sql()`, que ejecuta SQL arbitrario contra el postgres del stack E2E (el propio fichero ya lo usa en su `afterEach`), y PostgresExternalCalendarStore.java:176 lee los eventos de la tabla `external_calendar_events`. Sembrar por INSERT una lista larga de resumenes Unicode no exige ningun feed ICS ni relajar la guardia SSRF. Y "guardando" se congela con `page.route` reteniendo el PUT a /api/v1/me/external-calendar. La limitacion que la bitacora declara (no llegar a lastStatus OK por sincronizacion real) es cierta y aceptable, pero NO implica que el estado de pantalla con lista sea inalcanzable: es la conclusion la que esta mal, no el hecho.

QUE HACE FALTA PARA CERRAR: extraer el cuerpo de la matriz (noHorizontalScroll + controlsAreLargeEnough) a un helper y recorrer los catorce anchos sobre los cinco estados del contrato -vacio; con suscripcion; con error tras sync FAILED; con lista larga de resumenes Unicode sembrada por `sql()` con INSERT; y guardando, congelado con `page.route` sobre el PUT-, incluyendo el dialogo de confirmacion abierto para que "Si, eliminar" y "Cancelar" entren en la medicion de 44x44 px. Aplicar lo mismo a la prueba de texto al 200 %.

NOTA APARTE (no forma parte de este bloqueante, pero conviene registrarla): `noHorizontalScroll` solo compara scrollWidth con clientWidth, asi que es ciego al recorte vertical y a los solapes, que la linea 544 tambien exige. Y no existe progress/ux_external_calendar.md con la matriz de los 30 principios de docs/ux-requirements.md. Son hallazgos independientes que deberian ir en sus propios bloqueantes.

GRAVEDAD: bloqueante (confirmada). Es el mismo motivo por el que el juez rechazo tres features hoy, y aqui la cobertura geometrica es de 1 de 5 estados.

### 6. [BLOQUEANTE · varias horas] La guardia SSRF descarta las direcciones validadas y vuelve a conectar por nombre: la enmienda B3, aprobada y con prevalencia, exige lo contrario

- **Dimension:** seguridad
- **Rutas:** backend/src/main/java/com/apptolast/organization/application/OutboundHostGuard.java:8-11 y :22-31; backend/src/main/java/com/apptolast/organization/application/SyncExternalCalendar.java:86-88; backend/src/main/java/com/apptolast/organization/adapter/feed/HttpCalendarFeed.java:41-49; project-spec.md:2488 y :2492; docs/external-calendar.md:40-42

**Evidencia.** project-spec.md:2488: «El coordinador aprueba las siguientes enmiendas dentro de la autorización global; prevalecen sobre el texto de las secciones 25 a 28 escrito antes.» project-spec.md:2492 (B2 y B3): «Deja de aceptarse como límite el reenlace de nombres entre la comprobación y el uso: se resuelve el nombre una vez, se validan todas las direcciones devueltas y se conecta contra la dirección literal ya validada, conservando el nombre original en la cabecera Host y en la indicación de servidor de TLS.» El código hace lo opuesto y lo declara aceptado: OutboundHostGuard.java:8-11 «Riesgo residual conocido: entre esta comprobación y la conexión el DNS puede cambiar (rebinding DNS). Se acepta y se mitiga repitiendo la comprobación en cada sincronización, no solo al guardar.» El veredicto es un enum sin direcciones (OutboundGuard.java:6-10) y OutboundHostGuard.java:30 evalúa «addresses.stream().allMatch(policy::allows) ? Verdict.ALLOWED : Verdict.BLOCKED», descartando la lista resuelta. SyncExternalCalendar.java:86-88 llama a guard.check(urlHost) y acto seguido a feed.fetch(url.get()), y HttpCalendarFeed.java:45 construye «HttpRequest.newBuilder(URI.create(url))», que resuelve el nombre por segunda vez, de forma independiente. docs/external-calendar.md:40 lo publica como «*Riesgo residual aceptado*». El texto que sí lo aceptaba, progress/proposal_external_calendar.md:45, es anterior a la enmienda y es exactamente el que la enmienda deroga.

**Arreglo.** Que OutboundGuard devuelva las direcciones validadas junto al veredicto (por ejemplo un record Verdict + List<InetAddress>), y que HttpCalendarFeed conecte contra una de esas direcciones literales conservando el nombre original en Host y en SNI: en el cliente JDK se consigue con un java.net.spi.InetAddressResolver o un HttpClient con un resolutor fijado a las direcciones ya aprobadas, sin volver a preguntar al DNS. Cubrirlo con un test que devuelva una dirección pública en la comprobación y una privada en la segunda resolución y exija FEED_REJECTED o ninguna conexión. Si el coordinador decide reaceptar B3 en su lugar, debe escribirlo como resolución fechada en progress/security_review_connectors.md y en project-spec.md, y entonces entregar la política de egreso en deploy/, no dejarla como suposición.

**Correccion del verificador.** Bloqueante confirmado, con tres correcciones al enunciado.

TÍTULO CORRECTO: «La guardia SSRF de la 28 cumple la mitad de la enmienda B3 (resuelve y valida todas las direcciones) pero no la otra mitad (conectar contra la dirección literal validada), y el javadoc y docs/external-calendar.md publican como riesgo aceptado justo lo que la enmienda dejó de aceptar, sin resolución del coordinador que lo autorice. Mismo defecto en la feature 25 ya integrada.»

CORRECCIÓN 1 — el alcance no es solo la 28. COMPROBADO LEYENDO backend/src/main/java/com/apptolast/organization/adapter/webhook/JdkWebhookSender.java: su método `guardDestination` (:92-97) resuelve y comprueba, y luego `request(...)` (:100) construye `HttpRequest.newBuilder(URI.create(url))` — exactamente la misma forma que la 28. Peor: su javadoc :25-27 afirma «Amendment B3: the host is resolved once and every returned address is checked before connecting, so no name can be re-pointed between the check and the use», que es falso, porque el JDK vuelve a resolver en el connect. Y progress/auditoria_colisiones_25_28_30.md:252 fija la misma lectura reducida de la enmienda: «Asi la enmienda B3 (resolver una vez y comprobar todas las direcciones) queda en un unico sitio», omitiendo la cláusula de la dirección literal. Es decir: hay una interpretación truncada de B3 compartida por 25, por 28 y por la propia auditoría de integración. La 27 no está afectada: su base de API es fija y validada al arrancar (GithubApiBase, enmienda B11), no la elige el usuario. Por tanto el arreglo y la puerta deben cubrir 25 y 28 a la vez; bloquear solo la 28 dejaría el mismo agujero abierto en una feature ya integrada.

CORRECCIÓN 2 — la desviación SÍ está declarada, lo que falta es la autorización. El enunciado dice «desviación no declarada». No es exacto: está declarada dos veces y a la vista (OutboundHostGuard.java:8-11 y docs/external-calendar.md:40-42), y también en progress/tdd_external_calendar.md línea 90 («Riesgo residual anotado en el javadoc: rebinding DNS»). Lo que no existe es la resolución del coordinador que convierta esa declaración en aceptación válida. Y hay doctrina propia del proyecto sobre cómo se hace eso: security_review_connectors.md:106-114 cierra A1 y A2 como riesgo aceptado sólo porque su corrección «contradice escenarios ya aprobados del contrato», y exige registrar la mitigación obligatoria «en la sección de enmiendas de project-spec.md y en progress/current.md». Aquí no se cumple ninguna de las dos condiciones: features/external_calendar.feature (aprobado el 09-08 20:52, ANTERIOR a la enmienda) no dice nada sobre el pinning, así que la corrección no contradice ningún escenario aprobado —no hay conflicto que excuse la aceptación—, y no hay registro alguno en project-spec.md ni en current.md. Nota: el propio carril tiene precedente de subordinar una enmienda al Gherkin (Ciclo 6, enmienda B5, tdd_external_calendar.md:70-80), pero allí había contradicción literal con @s2; aquí el contrato es silencioso, no contradictorio, así que ese precedente no cubre B3.

CORRECCIÓN 3 — el impacto está sobredimensionado. El enunciado afirma que el atacante «alcanza servicios de la red del despliegue (db y rabbitmq)». COMPROBADO: ExternalCalendarInput.java:27 exige esquema `https`, HttpCalendarFeed desactiva redirecciones y sólo acepta 200 con Content-Type text/*, y el HttpClient usa el truststore por defecto sin desactivar la verificación. Para que el rebinding llegue a algo, el servicio interno tendría que completar un handshake TLS presentando un certificado válido y confiable para el nombre que controla el atacante; PostgreSQL y RabbitMQ en la red de Compose no lo hacen, y un fallo TLS termina en SSLException → FEED_UNREACHABLE. La propia revisión lo dice (security_review_connectors.md:40: «La obligación de https reduce mucho la explotabilidad… pero no la elimina») y por eso clasifica B3 como severidad **media**, no alta. La descripción debe decir eso: la explotabilidad práctica está muy acotada por https, y lo que bloquea no es un SSRF demostrable hoy sino el incumplimiento de una enmienda de seguridad aprobada con prevalencia explícita, más una documentación pública que afirma un estado que el contrato normativo ya no admite. Sigue siendo bloqueante para marcar `done` —AGENTS.md/CLAUDE.md no permiten cerrar con una desviación no resuelta del contrato normativo—, pero por incumplimiento de contrato, no por «agujero SSRF explotable».

CIERRE ACEPTABLE (cualquiera de los dos, no ambos):
(a) Implementar B3 completa en una sola clase compartida por 25 y 28: que el veredicto de la guardia devuelva la `InetAddress` elegida (no un enum sin datos), y que la conexión se abra contra esa dirección literal conservando el nombre en Host y en SNI. En el HttpClient del JDK esto exige construir el request contra la IP, fijar SNI con `SSLParameters.setServerNames` en el builder del cliente y habilitar la cabecera Host restringida (`jdk.httpclient.allowRestrictedHeaders=host`); es la parte cara de la corrección y hay que dimensionarla, pero es la que pide la enmienda.
(b) Que el coordinador escriba una resolución explícita sobre B3 con la misma forma que la de A1/A2: aceptación razonada, registro en la sección de enmiendas de project-spec.md y en progress/current.md, y la mitigación alternativa que la propia revisión exige —política de egreso declarada como requisito en `deploy/`— que hoy no existe. Sin una de las dos, la 28 (y la 25) no pueden cerrarse.

DEFECTO MENOR DE CITA en el enunciado original: «la única resolución escrita en ese fichero, líneas 106-114» se lee como si fuera progress/proposal_external_calendar.md, que sólo tiene 83 líneas. Esas líneas son de progress/security_review_connectors.md. El contenido que afirma es correcto.

### 7. [BLOQUEANTE · minutos] El alcance PIT apunta a un paquete borrado: el cifrado AES-256-GCM no recibiria ni un mutante

- **Dimension:** mutacion
- **Rutas:** backend/build.gradle.kts:398-399 (entradas muertas); backend/src/main/java/com/apptolast/organization/adapter/connectors/AesGcmSecretCipher.java y ConnectorKeyRing.java (el codigo real, ausente del alcance)

**Evidencia.** En `val externalCalendarClasses = setOf(` (build.gradle.kts:381) las lineas 398-399 dicen literalmente:
    "com.apptolast.organization.adapter.crypto.AesGcmSecretCipher*",
    "com.apptolast.organization.adapter.crypto.ConnectorCipher*"
El paquete no existe: `ls backend/src/main/java/com/apptolast/organization/adapter/` devuelve broker, config, connectors, feed, http, logging, net, persistence, webhook — no hay `crypto`. Tampoco existen ya sus pruebas: `find backend/src/test -iname "*ConnectorCipher*" -o -iname "*SecretUrlCipher*"` no devuelve nada. La propia bitacora lo dice en progress/tdd_external_calendar.md:489-492: «Se borran mi adapter/crypto/AesGcmSecretCipher y ConnectorCipher con sus dos tests». El cifrador vigente es adapter/connectors/AesGcmSecretCipher.java, cableado en adapter/config/ConnectorConfiguration.java:32-36, y NO figura en externalCalendarClasses (solo aparece en githubConnectorClasses, build.gradle.kts:81, que es la puerta de la feature 27).

**Arreglo.** Sustituir las dos entradas muertas de backend/build.gradle.kts:398-399 por "com.apptolast.organization.adapter.connectors.AesGcmSecretCipher*" y "com.apptolast.organization.adapter.connectors.ConnectorKeyRing*", y actualizar su espejo en scripts/project.test.mjs:2071-2072. No hace falta escribir pruebas: targetTests para este alcance es setOf("com.apptolast.organization.*") (build.gradle.kts:551), asi que adapter/connectors/AesGcmSecretCipherTest.java ya entra en la campana y sus casos s1_ciphertextIsNonceThenSealedAndHidesTheToken, s2_everyWriteUsesAFreshNonce, s2_anotherOwnerCannotReadTheCiphertext..., s2_anotherKeyCannotReadTheCiphertext y s4_aMalformedKeyStopsTheStartupWithoutRevealingItsValue mataran esos mutantes.

**Correccion del verificador.** BLOQUEANTE (confirmado, con el alcance del daño acotado)

Título corregido: el alcance PIT de external_calendar arrastra dos globs muertos hacia `adapter.crypto`, de modo que la primitiva AES-256-GCM y el llavero de claves no reciben ni un mutante en la puerta de la feature 28.

Rutas
- backend/build.gradle.kts:398-399 — entradas muertas dentro de `externalCalendarClasses` (abre en :381).
- backend/src/main/java/com/apptolast/organization/adapter/connectors/AesGcmSecretCipher.java y ConnectorKeyRing.java — el código vigente, ausente del conjunto.
- backend/build.gradle.kts:81 (`adapter.connectors.*`, único sitio que los cubre, y es la puerta de la 27) y :522 (`externalCalendarOnly -> externalCalendarClasses`, excluyente).
- scripts/project.mjs:222-223 (`external_calendar-backend` → `-PmutationScope=external_calendar`).

Hecho verificado
Las líneas 398-399 apuntan a `com.apptolast.organization.adapter.crypto.*`, paquete inexistente (el listado de `adapter/` es broker, config, connectors, feed, http, logging, net, persistence, webhook). De las 25 entradas del conjunto, 23 resuelven, así que PIT no aborta: descarta los dos globs en silencio y la campaña `bin/harness mutate external_calendar-backend` puede cerrar en verde sobre el umbral 0.8 sin haber tocado el cifrador. Las entradas nacieron muertas en el commit d77125d, de la propia feature, tras la deduplicación que borró `adapter/crypto/`.

Qué queda realmente sin medir (acotación frente a la redacción original)
NO es cierto que la puerta no mida nada del cifrado: `SaveExternalCalendar:38,42,45` y `SyncExternalCalendar:84-85` (traducción de `Optional` vacío a `SECRET_UNREADABLE`, @s9/@s29) están en alcance y sí reciben mutantes. Lo que recibe CERO es:
- AesGcmSecretCipher: `NONCE_BYTES = 12`, `TAG_BITS = 128`, `SHORTEST = NONCE + 16`, la guarda `ciphertext.length <= SHORTEST`, `updateAAD(bytes(ownerId))` y las dos `System.arraycopy` que fabrican el formato nonce‖sellado. Es exactamente lo que fijan @s2 («url_ciphertext de 12 bytes de nonce más cifrado», features/external_calendar.feature:45) y @s3 (:52-53, dos cifrados distintos que descifran a la misma URL con el owner_id como AAD).
- ConnectorKeyRing: validación de 32 bytes base64, orden de candidatas en la rotación, y el mensaje que @s9 exige nombrando `APP_CONNECTOR_KEY`.
Añadir también, que el revisor omite: `adapter.config.ConnectorConfiguration` (:32-36), que construye el bean y determina el `enabled()` del que cuelga el 503 CONNECTORS_DISABLED, tampoco está en `externalCalendarClasses` — solo en `githubConnectorClasses` (build.gradle.kts:84).

Atenuante que debe constar
La clase no está huérfana en el repositorio: `githubConnectorClasses` la cubre vía `adapter.connectors.*` y el alcance completo (línea 540) también. Pero `github_connector` sigue `in_progress` y no existe ningún `progress/mutation_github_connector*.md`, así que ninguna campaña ha mutado aún esa primitiva. La puerta de la 28 no puede delegar en una puerta que tampoco se ha ejecutado.

Corrección propuesta (una línea, coste cero en pruebas)
Sustituir las dos entradas muertas por las clases reales, con nombre completo y NO con `adapter.connectors.*` (ese comodín metería `GithubApiBase` y `HttpGithubIssueSource`, código de la 27, dentro del score de la 28):
    "com.apptolast.organization.adapter.connectors.AesGcmSecretCipher*",
    "com.apptolast.organization.adapter.connectors.ConnectorKeyRing*",
y valorar añadir "com.apptolast.organization.adapter.config.ConnectorConfiguration". Los verdugos ya existen y ya se ejecutan en esta puerta: `backend/src/test/java/com/apptolast/organization/adapter/connectors/AesGcmSecretCipherTest.java` tiene 8 @Test, con `assertEquals(12 + TOKEN.length() + 16, sealed.length)`, comparación de nonces entre dos cifrados y un `@ValueSource(ints = {0, 1, 12, 13, 27, 28})` sobre los límites de longitud; `targetTests` de este alcance es `com.apptolast.organization.*` (:551), luego ya corren. Solo falta que PIT genere los mutantes.

Cierre: no se puede marcar `done` la feature 28 con este alcance. Corregir las dos líneas y reejecutar `bin/harness mutate external_calendar-backend`, dejando la evidencia en `progress/mutation_external_calendar.md` (hoy inexistente).

### 8. [ALTA · una hora] @s38 filas 6, 7 y 8: el feedback «Sincronizando» y el bloqueo de controles no los verifica ninguna prueba

- **Dimension:** contrato
- **Rutas:** frontend/src/external-calendar.tsx:215; frontend/src/external-calendar.test.tsx:284, :309, :329, :415

**Evidencia.** El Then de @s38 (features/external_calendar.feature:515) exige, para las tres filas de «Sincronizar ahora», que «aparece "Sincronizando" antes de 400 ms, se envía exactamente una petición y los controles quedan bloqueados hasta la respuesta». `grep -rn "Sincronizando" frontend/src` devuelve una sola línea: external-calendar.tsx:215 `setAnnouncement("Sincronizando…")`; ningún fichero de prueba la menciona. Las tres pruebas de sincronización (líneas 284, 309, 329) resuelven la respuesta inmediatamente y solo afirman el resultado final; la de la región viva (línea 415) espera `toHaveTextContent("Sincronizado.")`, es decir el estado posterior. Ninguna cuenta los POST (`calls.filter(...).toHaveLength(1)` solo se hace para PUT en la línea 205 y para DELETE en la 371) ni comprueba `toBeDisabled()` durante la sincronización, al contrario que la fila de Guardar, que sí lo hace en las líneas 203-205 reteniendo la respuesta con una promesa.

**Arreglo.** Añadir una prueba que retenga la respuesta del POST /sync con una promesa (mismo patrón que la línea 181-210), y que afirme: que aparece «Sincronizando…» antes de resolverla, que `calls.filter(c => c.method === "POST")` tiene longitud 1 tras pulsar dos veces, y que el botón «Sincronizar ahora» está `toBeDisabled()` hasta que se resuelve.

**Correccion del verificador.** @s38 filas 6, 7 y 8 (Sincronizar ahora): sin oráculo para feedback inmediato, petición única y bloqueo de controles.

Contrato: features/external_calendar.feature:515 (Then de @s38) exige para las tres filas de «Sincronizar ahora» que «aparece "Sincronizando" antes de 400 ms, se envía exactamente una petición y los controles quedan bloqueados hasta la respuesta».

Lo verificado (leído, no citado):
- Producto: frontend/src/external-calendar.tsx:212 `if (busy) return`, :214 `setBusy("syncing")`, :215 `setAnnouncement("Sincronizando…")`, :255 `const locked = busy !== ""`, aplicado en :281 y :301 (`readOnly`) y :321, :361, :369 (`disabled`).
- Pruebas de sincronización: frontend/src/external-calendar.test.tsx:284, :309, :329. Las tres resuelven la respuesta al instante y solo afirman el resultado final. La de :284 usa `calls.find((c) => c.method === "POST")` para inspeccionar el cuerpo — inspecciona, no cuenta: con dos POST devolvería el primero y pasaría igual. La de la región viva (:415) espera `toHaveTextContent("Sincronizado.")`, el estado posterior.
- Contraste: la fila de Guardar sí tiene oráculo (:203 `findByText("Guardando…")`, :204 `toBeDisabled()`, :205 `calls.filter(PUT).toHaveLength(1)`) reteniendo la respuesta con una promesa; el DELETE se cuenta en :371. Las filas de sincronizar no tienen ninguna de las tres cosas.
- El arnés no aporta un oráculo implícito: el fetch falso de external-calendar.test.tsx:80 hace `list.length > 1 ? list.shift() : list[0]`, o sea reutiliza la última respuesta encolada; un segundo POST se respondería en silencio.
- Fuera de frontend/src no hay cobertura: e2e/external-calendar.spec.mjs solo espera la respuesta del POST con `waitForResponse` y verifica el efecto en base de datos; external-calendar-api.test.ts cubre la capa HTTP, no la vista.

CORRECCIÓN al bloqueante original — «ningún fichero de prueba la menciona» es falso: e2e/external-calendar-ux-audit.spec.mjs:79 escribe `await expect(page.getByRole("status")).not.toHaveText("Sincronizando…")`. Pero es una aserción NEGATIVA usada como espera de reposo: en Playwright pasa de inmediato si el anuncio no aparece nunca, así que no solo no cubre la fila, sino que es el mismo patrón placebo que el juez ya rechazó hoy en otra feature. Si se arregla el hueco, esa línea debe sustituirse por una espera positiva («aparece y luego desaparece») o por una espera del texto final.

Mutantes que sobreviven hoy (esto es lo que bloquea, no el estilo):
- Borrar external-calendar.tsx:215: el anuncio pasa de "" a "Sincronizado." y las cinco pruebas siguen verdes.
- Borrar :214 (`setBusy("syncing")`): `locked` queda false durante toda la sincronización, el guardián de reentrada de :212 deja de guardar (dos clics seguidos envían dos POST) y los cuatro controles quedan operables; ninguna prueba falla. El `toBeDisabled()` de :204 solo pincha el camino `busy === "saving"`.

Nota adicional para el artesano (no es parte del bloqueo): el formulario declara `aria-busy={busy === "saving"}` en :272 y no hay equivalente para el estado «syncing»; conviene decidirlo al escribir la prueba.

Reparación mínima: en las pruebas de :284 y :309, retener la respuesta del POST con una promesa como se hace en :182-:191, y antes de liberarla afirmar `findByText("Sincronizando…")`, `getByRole("button", { name: "Sincronizar ahora" })` y `getByRole("button", { name: "Eliminar suscripción" })` con `toBeDisabled()`, los dos campos con `toHaveAttribute("readonly")`, un segundo clic sobre el botón y `calls.filter((c) => c.method === "POST")).toHaveLength(1)`; después liberar y comprobar el resultado ya cubierto. La fila de 404 (:329) necesita al menos el feedback y el recuento.

Bitácora que no coincide con el código: progress/tdd_external_calendar.md:219 afirma «"Guardando"/"Sincronizando" antes de esperar nada, con una sola petición»; la mitad de sincronizar no está respaldada por ninguna prueba. Conviene corregir esa línea al cerrar el hueco.

Gravedad: alta (tres de las ocho filas del escenario sin oráculo en la columna que el propio escenario destaca).

### 9. [ALTA · una hora] @s39 fila 3: la prueba de cancelación al salir no puede fallar aunque se quite el abort

- **Dimension:** contrato
- **Rutas:** frontend/src/external-calendar.test.tsx:376-382

**Evidencia.** La fila 3 de @s39 (features/external_calendar.feature:537) dice «navego a /hoy con una petición en curso → la petición se cancela y su respuesta tardía no modifica la vista destino». La única prueba es: `withoutSubscription(); const view = render(<ExternalCalendar />); view.unmount(); await new Promise(r => setTimeout(r, 0)); expect(document.body.textContent).toBe("");`. No hay petición en curso retenida, no se navega a /hoy, no se captura ninguna AbortSignal y no se entrega ninguna respuesta tardía: `document.body.textContent` es "" tras cualquier unmount, con abort o sin él. La propia bitácora (tdd_external_calendar.md:243-247) reconoce este modo de fallo en la sección de Hoy y allí lo corrigió capturando las señales entregadas a fetch; aquí no se hizo.

**Arreglo.** Reescribir la prueba como en today-external-calendar.test.tsx: capturar las `signal` entregadas a `fetch`, retener la respuesta, desmontar (o navegar a /hoy con el enrutador de App), y afirmar que todas las señales quedan `aborted` y que resolver la respuesta tardía no cambia el DOM de la vista destino.

**Correccion del verificador.** El bloqueante es real y está bien dirigido. Solo ajusto tres cosas para que sea preciso y no rebase su alcance:

TÍTULO: @s39 fila 3 (cancelación al salir) queda sin oráculo: la prueba de `external-calendar.test.tsx:376-382` pasa igual con y sin el `abort` del desmontaje.

RUTAS: `frontend/src/external-calendar.test.tsx:376-382` (la prueba); `frontend/src/external-calendar.tsx:160` (el `return () => controller.abort()` que nadie observa); `features/external_calendar.feature:537` (la fila); referencia de la receta correcta: `frontend/src/today-external-calendar.test.tsx:248-275`.

MATIZ 1 — el producto SÍ está implementado. `external-calendar.tsx:139-160` crea un `AbortController` por efecto y lo aborta en el cleanup; `start()` (líneas 109-114) aborta además la petición anterior en cada acción. El defecto es de cobertura de prueba, no de comportamiento. No pedir cambios en producción.

MATIZ 2 — hay cobertura parcial que el bloqueante no acredita. `external-calendar-api.test.ts:319-339` cubre la mitad «la petición se cancela» en la capa de API (señal ya abortada no llega a fetch; respuesta de lectura cancelada no se valida). Lo que queda huérfano es (a) el cableado componente→cleanup y (b) la mitad «su respuesta tardía no modifica la vista destino», que ninguna prueba ejerce en ningún fichero.

MATIZ 3 — añadir la contradicción de la bitácora como evidencia, porque cambia la lectura del caso. `tdd_external_calendar.md:211-215` afirma que quitar el `abort` del desmontaje hace caer una prueba de esta pantalla; `tdd_external_calendar.md:243-249` demuestra lo contrario para la sección de Hoy («sobrevivía porque React 19 ya no avisa de un `setState` sobre una vista desmontada»). El artesano corrigió Hoy y no volvió sobre /calendario-externo. Es decir: el modo de fallo estaba diagnosticado por escrito y se dejó abierto aquí.

GRAVEDAD: alta se mantiene (fila del contrato sin oráculo, mismo patrón placebo ya rechazado hoy), pero el arreglo es mecánico y no toca producción.

REMEDIO EXIGIBLE: reescribir `external-calendar.test.tsx:376-382` calcando `today-external-calendar.test.tsx:248-275`: retener la respuesta del GET con una promesa que se libera a mano, acumular los `options.signal` que recibe el `fetch` doblado, comprobar `signals.length >= 1` y `every(s => !s.aborted)` con la vista montada, desmontar, exigir `every(s => s.aborted)`, y solo entonces liberar la respuesta tardía y verificar que la vista destino no cambia. Para cubrir de verdad «navego a /hoy» y «la vista destino», hacerlo a través de `App` con `window.history` en `/calendario-externo` y navegación al enlace de Hoy (como hace `external-calendar-route.test.tsx`), o dejar constancia explícita de que se cubre por desmontaje y que la ruta destino la cubre @s37; lo que no vale es la aserción actual sobre `document.body.textContent`, que es cierta tras cualquier desmontaje.

### 10. [ALTA · varias horas] La auditoría UX promete «sin solapes» y «no recorta» pero solo mide el scroll horizontal del documento

- **Dimension:** oraculos
- **Rutas:** e2e/external-calendar-ux-audit.spec.mjs:16-22, 45-55 y 123-134

**Evidencia.** async function noHorizontalScroll(page) { const overflow = await page.evaluate(() => ({ scrollWidth: document.documentElement.scrollWidth, clientWidth: document.documentElement.clientWidth })); expect(overflow.scrollWidth).toBeLessThanOrEqual(overflow.clientWidth + 1); } … test("calendario externo audit: sin solapes ni scroll horizontal en toda la matriz @s40" …) … test("calendario externo audit: texto al 200 % no recorta la pantalla a 320 px @s40" …)

**Arreglo.** Añadir en el mismo bucle de anchos un oráculo de recorte por elemento (recorrer los descendientes de `.external-calendar` y exigir `scrollWidth <= clientWidth + 1` y `scrollHeight <= clientHeight + 1` en los que tengan overflow no visible) y uno de solape (comparar los `getBoundingClientRect()` de los controles y textos del formulario dos a dos). Renombrar el título si al final no se mide alguna de las dos cosas.

**Correccion del verificador.** BLOQUEANTE (gravedad alta, reformulado y ampliado): la auditoria UX de la feature 28 no tiene oraculo para dos de las tres obligaciones de @s40 y ademas audita un solo estado de cinco.

Rutas: e2e/external-calendar-ux-audit.spec.mjs:16-22 (noHorizontalScroll), 24-43 (controlsAreLargeEnough), 45-55 (test de matriz), 123-134 (test de texto 200 %); contrato features/external_calendar.feature:540-548.

Hechos comprobados leyendo:
1. features/external_calendar.feature:544 exige «no hay solapes, recortes ni scroll horizontal accidental en ningun ancho ni a ambos lados de cada breakpoint». De las tres, solo la tercera tiene oraculo, y unicamente a nivel de document.documentElement (linea 21). En las 134 lineas del spec no aparece scrollHeight, clientHeight, ni ninguna comparacion de la caja de un elemento con la de otro: solapes y recortes no se miden en absoluto.
2. El estandar correcto ya existe en el repositorio y el juez lo acepto en la feature hermana: e2e/ics-calendar-ux.spec.mjs:170-185 calcula una lista `clipped` sobre `main, main *` comparando scrollWidth>clientWidth+1 y scrollHeight>clientHeight+1 con la guarda getComputedStyle overflowX/overflowY !== "visible", mas una lista `escaping` con getBoundingClientRect().right > root.clientWidth+1. El solape por pares esta resuelto en history-ux.spec.mjs:121 y customization-ux.spec.mjs:757. La feature 28 no reutiliza ninguno de los tres.
3. Agravante no visto por el revisor original: el test de la matriz (48-54) y el de texto al 200 % (126-133) hacen goto + expect(getByLabel("Etiqueta")).toBeVisible() y nunca guardan una suscripcion; el afterEach (12-14) borra las filas y el fixture no siembra ninguna. Por tanto los 14 anchos y el texto ampliado se auditan SOLO sobre el formulario vacio. Los otros cuatro estados que exige la linea 542 del contrato (con suscripcion, con error, con lista larga de resumenes Unicode, y guardando) jamas se renderizan en la auditoria responsive, y con ellos quedan sin auditar los nodos que de verdad pueden recortar o solapar: .external-calendar-address (styles.scss:2371, pintado en external-calendar.tsx:333), .external-calendar-summary (styles.scss:2402, external-calendar.tsx:396), el dl de dos columnas (styles.scss:2377-2386) y el [role="alertdialog"] (styles.scss:2408-2416). El unico estado con suscripcion vive en el test de axe (57-87), que no mide geometria.
4. Consecuencia demostrable: cualquier regresion de recorte en esos nodos pasa la puerta. El ejemplo del revisor (white-space:nowrap; overflow:hidden en .external-calendar-address) sobrevive por partida doble — el oraculo no mira scrollWidth de elementos, y ademas el elemento no llega a existir en los dos tests responsivos. Igual con overflow:hidden y una altura fija en .external-calendar-summary, que recortaria verticalmente los resumenes Unicode largos sin tocar el scroll del documento.

Correccion exigida: (a) extraer a e2e/support un helper de geometria equivalente al de ics-calendar-ux.spec.mjs:150-190 (clipped horizontal Y vertical con guarda de overflow, escaping, small) y un comparador de solapes por pares como el de history-ux.spec.mjs; (b) parametrizar el bucle de los 14 anchos y el de texto al 200 % por los cinco estados del contrato (vacio, con suscripcion, con error, lista larga Unicode, guardando), sembrando cada estado antes de medir; (c) aplicar el mismo helper en el escenario de zoom nativo al 200 %, que hoy no existe para esta feature (no hay external-calendar-native-zoom.spec.mjs pese a que la linea 543 del contrato lo nombra y a que existen los equivalentes de today, reschedule, start-work-session y github-connector).

Nota aparte, no cubierta por este bloqueante pero verificada de paso: no existe progress/ux_external_calendar.md con la matriz de los 30 principios de docs/ux-requirements.md, que AGENTS.md:51 exige y no permite inferir desde axe.

### 11. [ALTA · varias horas] @s34 no tiene ningún oráculo sobre la respuesta de Hoy con suscripción

- **Dimension:** oraculos
- **Rutas:** backend/src/test/java/com/apptolast/organization/ExternalCalendarIsolationTest.java:54-77; e2e/external-calendar.spec.mjs:125-134

**Evidencia.** test("calendario externo: Hoy sigue mostrando su agenda sin suscripción @s34 @s36", …) { await page.goto("/"); … await expect(page.getByRole("region", { name: "Calendario externo" })).toHaveCount(0); }

**Arreglo.** Una prueba de integración que capture el cuerpo de GET /api/v1/today sin suscripción, cree suscripción y una instantánea con un evento solapado, repita la llamada y exija igualdad literal del cuerpo, y que a continuación planifique el bloque solapado esperando 201. Comparto este hallazgo con el revisor de cobertura: aquí lo señalo porque el test que se presenta como oráculo de @s34 no puede fallar por lo que @s34 protege.

**Correccion del verificador.** TITULO: @s34 nunca observa el cuerpo de Hoy ni el 201 de planificacion con una suscripcion presente; la garantia solo esta cerrada a nivel de dependencias de compilacion.

RUTAS: backend/src/test/java/com/apptolast/organization/ExternalCalendarIsolationTest.java:46-77 (las dos reglas @s34); e2e/external-calendar.spec.mjs:125-134 (unico @s34 del carril, sin suscripcion); features/external_calendar.feature:452-464 (el escenario); ausencia: backend/src/test/java/com/apptolast/organization/adapter/TodayApiTest.java (no siembra external_calendar_*).

QUE FALTA EXACTAMENTE. El Outline @s34 tiene tres examples y dos Then: (1) el cuerpo de Hoy byte a byte igual a R0 con plannedSeconds 3600, remainingSeconds 3600, currentBlockId, nextBlockId y closingAt; (2) planificar 14:00Z-15:00Z responde 201 sin error de solape aunque el segundo example ponga un evento externo exactamente en ese hueco. Ninguna prueba del repositorio ejecuta el Given "persona-a suscribe un feed con <evento> ya sincronizado": ni la R0, ni la comparacion, ni el 201. Los tres examples solo se distinguen por el evento externo sembrado, y ese evento no se siembra en ninguna parte, asi que los tres colapsan en el mismo caso nulo.

COBERTURA PARCIAL QUE SI EXISTE (y que el alegato original omitia, por honestidad):
- Las dos reglas ArchUnit cierran de verdad el vector de codigo Java: ninguna ruta desde ReadToday, PlanBlock, TodayController o BlockController hacia el carril compila sin poner rojo la regla 1, y ninguna clase ajena al carril puede tomar ExternalCalendarStore/SyncExternalCalendar/ReadExternalCalendarEvents sin poner roja la regla 2. Es un guardarrail real, no decorativo.
- TodayApiTest.java:432 afirma result.size() == 15 sobre GET /api/v1/today, luego anadir un decimosexto campo (p. ej. una seccion externa) al cuerpo de Hoy si se pone rojo.
Lo que queda descubierto es el vector de datos/SQL y todo el contrato numerico del Then: cambiar contenido de items o del calculo de solapes sin anadir campos ni dependencias de clase pasa invisible.

CAMBIO DE PRODUCTO QUE DEBERIA ROMPERLO Y NO LO ROMPE (verificado): anadir external_calendar_events al SQL que alimenta el contexto de planificacion en PostgresBlockStore (los solapes de PlanBlock salen de consultas sobre planned_blocks en ese adaptador; PlanBlock:111 lanza BlockOverlapException sobre context.blocks()). PostgresBlockStore no referencia ninguna clase ExternalCalendar*, asi que las dos reglas ArchUnit siguen verdes; y ningun test de bloques ni de planificacion siembra filas en external_calendar_events, asi que tampoco hay rojo por datos. El example 2 de @s34 (evento externo de 14:00Z a 15:00Z, planificar 14:00Z-15:00Z) devolveria 409 y la suite entera seguiria en verde. Simetricamente, unir external_calendar_events en PostgresTodayQueries anadiria items al cuerpo de Hoy sin cambiar el numero de campos, y TodayApiTest:432 seguiria verde porque sus escenarios no tienen suscripcion.

REMEDIO MINIMO (barato, la infraestructura ya existe): un test estilo TodayApiTest (Testcontainers, ya siembra bloques con seedBlock y ya tiene jdbc) parametrizado con los tres examples: sembrar el bloque propio 10:00Z-11:00Z y presupuesto 120, capturar el cuerpo de GET /api/v1/today como R0, insertar suscripcion + snapshot en external_calendar_subscriptions/external_calendar_events (ExternalCalendarPersistenceTest.java ya demuestra como), repetir GET /api/v1/today y comparar el JSON completo con R0, y planificar el bloque del example esperando 201. Ese test se pone rojo con cualquiera de los dos cambios de arriba; el ArchUnit no.

GRAVEDAD: alta se sostiene por el criterio que el juez ya aplico hoy (el contrato nombra una observacion que nunca se realiza, igual que el zoom nativo al 200 %), pero el alegato debe dejar de decir que solo hay "una regla ArchUnit" y reconocer las dos coberturas parciales de arriba; sin ese matiz el bloqueante sobrevende el hueco. Si el juez prefiere ponderar que el vector dominante (codigo Java) si esta cerrado estructuralmente, media-alta es defendible; lo que no es defendible es cerrar la feature 28 con los tres examples de @s34 sin ejecutar jamas su Given.

NOTA DE BITACORA: progress/tdd_external_calendar.md:16 afirma "HTTP MockMvc - @s1, @s2, @s4, @s8, @s10, @s28, @s31, @s32; Hoy intacto - @s34", lo cual es falso: no existe ningun MockMvc de @s34. El mapa de trazabilidad del mismo fichero (linea 337) si es honesto y solo lista ExternalCalendarIsolationTest y el e2e. Conviene corregir la linea 16 al cerrar el hallazgo.

### 12. [ALTA · una hora] El oraculo de recorte solo mide el ancho y la prueba titulada «sin solapes» no comprueba ningun solape

- **Dimension:** accesibilidad
- **Rutas:** e2e/external-calendar-ux-audit.spec.mjs:16-22 y :45

**Evidencia.** El titulo de la prueba dice «calendario externo audit: sin solapes ni scroll horizontal en toda la matriz @s40» (linea 45), pero su cuerpo solo llama a noHorizontalScroll() y controlsAreLargeEnough(): no hay ni una sola comparacion de rectangulos entre elementos. Y noHorizontalScroll (lineas 16-22) es, entero: `const overflow = await page.evaluate(() => ({ scrollWidth: document.documentElement.scrollWidth, clientWidth: document.documentElement.clientWidth })); expect(overflow.scrollWidth).toBeLessThanOrEqual(overflow.clientWidth + 1);`. Solo ancho de documento: ni scrollHeight/clientHeight, ni recorte por elemento. Agrava el detalle de la linea 51, `height: width === 768 ? 400 : 900`: se fuerza una altura corta de 400 px justo en 768 -el caso donde el recorte vertical es mas probable- y no se mide nada vertical. El repo ya tiene el liston mas alto en al menos ocho specs: solapes por pares de cajas en e2e/history-ux.spec.mjs:121, e2e/close-work-session-ux.spec.mjs:121, e2e/reschedule-ux.spec.mjs:130, e2e/pause-resume-session.spec.mjs:187; scrollHeight en e2e/close-work-session-ux.spec.mjs:159 y e2e/history-ux.spec.mjs:575; y recorte por elemento en ambas dimensiones en e2e/ics-calendar-ux.spec.mjs:179-183 (`element.scrollHeight > element.clientHeight + 1`).

**Arreglo.** En noHorizontalScroll, medir tambien scrollHeight frente a clientHeight cuando la altura del viewport es la limitante, y anadir un oraculo de recorte por elemento en AMBAS dimensiones sobre los nodos de .external-calendar, copiando el de e2e/ics-calendar-ux.spec.mjs:179-183. Anadir una comprobacion de solapes por pares de rectangulos de los controles y regiones nombradas, como en e2e/history-ux.spec.mjs:121, o bien corregir el titulo de la linea 45 para que no afirme lo que no mide.

**Correccion del verificador.** TÍTULO: El barrido responsive @s40 cubre 1 de los 5 estados del contrato y no tiene oráculo ni de solapes ni de recortes, pese a que su título afirma cubrir los solapes

RUTAS: e2e/external-calendar-ux-audit.spec.mjs:16-22 (helper), :45-55 (prueba), contra features/external_calendar.feature:542 y :544

EVIDENCIA (verificada leyendo los ficheros, no bitácoras):

(a) Estados: el contrato exige en la línea 542 la matriz sobre «vacío, con suscripción, con error, con lista larga de resúmenes Unicode y guardando». El cuerpo de la prueba (48-55) hace `goto("/calendario-externo")`, espera a que se vea el campo «Etiqueta» y recorre 14 anchos. Nunca rellena el formulario, nunca guarda, nunca provoca error, nunca carga la lista. Cubre el estado vacío y ninguno más: 1 de 5. Los cuatro estados ausentes son los únicos con contenido variable (la `li` de `.external-calendar-events` con `display:flex; flex-wrap:wrap`, la `dl` de metadatos, el `[role="alertdialog"]` de borrado) — es decir, la prueba mide la única pantalla donde no hay nada que pueda solaparse ni recortarse.

(b) Solapes: el título de la línea 45 dice «sin solapes ni scroll horizontal en toda la matriz», pero no hay ni una comparación de rectángulos entre elementos en las 134 líneas del fichero. El listón del repositorio es un bucle por pares con intersección en ambos ejes, presente en e2e/history-ux.spec.mjs:121, e2e/close-work-session-ux.spec.mjs:121, e2e/reschedule-ux.spec.mjs:130 y e2e/pause-resume-session.spec.mjs:187.

(c) Recortes: `noHorizontalScroll` (16-22) es un oráculo de desbordamiento de página, no de recorte. El oráculo de recorte del repositorio está en e2e/ics-calendar-ux.spec.mjs:174-181 y mide, por elemento y en ambos ejes, `overflow` computado distinto de `visible` junto a `scroll* > client* + 1`. Ese mismo fichero lleva en las líneas 129-137 un comentario que documenta que un oráculo de sólo `scrollWidth` ya falló una vez en este proyecto: «La versión anterior sólo miraba scrollWidth, y por eso no vio que al arreglar el recorte horizontal el contenido pasó a recortarse por abajo». La feature 28 regresa a ese oráculo pre-corrección.

QUÉ NO ES: no es una prueba placebo. Discrimina de verdad en dos frentes — si `controlsAreLargeEnough` viera un control por debajo de 44 px, o si el documento desbordara a lo ancho en cualquiera de los 14 anchos, la prueba rompe. Tampoco hay mala fe en el `height: width === 768 ? 400 : 900` de la línea 51: un viewport corto hace aparecer barra vertical y reduce `clientWidth`, con lo que endurece la comprobación horizontal. El defecto es de cobertura, no de simulación.

ATENUANTE REGISTRADO: el bloque `.external-calendar` de frontend/src/styles.scss está escrito defensivamente (`min-width: 0`, `overflow-wrap: anywhere`, `flex-wrap: wrap`, `minmax(0, ...)`, ningún `overflow: hidden`), así que la probabilidad de un recorte real es baja. Eso rebaja el riesgo de producto, no el hueco de verificación: el contrato pide prueba, no plausibilidad, y sin oráculo cualquier regresión futura de CSS pasa la puerta en verde.

CAMBIO CONCRETO QUE HOY NO ROMPERÍA LA SUITE (demuestra que el hueco es real): poner `overflow: hidden` y una altura fija a `.external-calendar-events li`, o quitar `flex-wrap: wrap` de `.form-footer` para que «Guardar», «Sincronizar ahora» y «Eliminar suscripción» se pisen a 320 px. Los tres botones seguirían midiendo 44×44 por el `min-height`/`min-width` del SCSS y el documento no desbordaría a lo ancho, así que la prueba de la línea 45 seguiría verde con solape visible y contenido recortado.

CIERRE MÍNIMO: reutilizar el `geometry()` de e2e/ics-calendar-ux.spec.mjs (bucle de solape por pares + recorte por elemento en ambos ejes con guarda de `overflow`), aplicarlo a los cinco estados de la línea 542 en los 14 anchos ya listados, y corregir el título para que nombre lo que el cuerpo asegura.

GRAVEDAD CORREGIDA: alta. Baja de «alta por recorte invisible» a «alta por cobertura de estados»: el hueco de recorte por sí solo sería media dado el CSS defensivo, pero la combinación de 1-de-5 estados, cero oráculo de solapes y un título que afirma cubrirlos es el mismo patrón que el juez rechazó hoy dos veces (auditoría parcial de estados y título que promete más que el cuerpo).

### 13. [ALTA · varias horas] Tema oscuro, forced-colors y prefers-reduced-motion no se ejecutan nunca, aunque la app los implementa

- **Dimension:** accesibilidad
- **Rutas:** e2e/external-calendar-ux-audit.spec.mjs (135 lineas, sin emulateMedia); frontend/src/styles.scss:53 y :1293; docs/ux-requirements.md:54

**Evidencia.** `grep -n -i -E "emulateMedia|colorScheme|forced-colors|prefers-reduced"` sobre los dos unicos ficheros que mencionan /calendario-externo (e2e/external-calendar-ux-audit.spec.mjs y e2e/external-calendar.spec.mjs) devuelve CERO coincidencias, asi que solo se audita el tema por defecto. Pero la app si tiene esas variantes: frontend/src/styles.scss:53 `@media (prefers-color-scheme: dark) {` y frontend/src/styles.scss:1293 `@media (prefers-reduced-motion: reduce) {`. Y el repo ya usa emulateMedia en nueve specs (e2e/appearance-ux-audit.spec.mjs, automations-ux, customization-ux, export-data-browser, ics-calendar-ux, import-data-browser, integration-api-browser, today-dark, webhooks-ux). docs/ux-requirements.md:54 lo exige: «movimiento reducido y semantica. Toda variante personalizable (tema, densidad, tamano de texto, paneles) debe pasar sus verificaciones». El precedente progress/ux_integration_api.md lo acredita explicitamente: «LIGHT/DARK mediante SYSTEM y preferencias del navegador, movimiento reducido y colores forzados».

**Arreglo.** Anadir al audit pasadas con page.emulateMedia({ colorScheme: "dark" }), { forcedColors: "active" } y { reducedMotion: "reduce" }, ejecutando axe y la geometria en cada una, siguiendo el patron ya establecido en e2e/integration-api-browser.spec.mjs o e2e/ics-calendar-ux.spec.mjs. Documentar en la matriz UX que bajo forced-colors la regla color-contrast se omite por usar colores nativos, como ya se declara en progress/ux_integration_api.md, y no omitirla en claro/oscuro.

**Correccion del verificador.** TITULO CORREGIDO: La auditoria UX de /calendario-externo no ejecuta tema oscuro ni forced-colors en ninguno de sus estados, rompiendo el patron que el contrato incorpora por referencia y que la feature hermana ya cumple.

GRAVEDAD CORREGIDA: media (no alta). No hay indicio de defecto visual: la pantalla no introduce paleta propia. Es un hueco de evidencia frente a un estandar de casa consolidado, no un riesgo de contraste identificado.

HECHOS (comprobados leyendo, no citados de bitacora):
- e2e/external-calendar-ux-audit.spec.mjs tiene 134 lineas (el bloqueante decia 135) y cero ocurrencias de emulateMedia/colorScheme/forcedColors/reducedMotion; idem e2e/external-calendar.spec.mjs.
- Son los dos unicos ficheros de e2e/ que referencian la ruta. today-dark.spec.mjs no la toca; appearance-ux-audit.spec.mjs solo recorre /apariencia y /historial. playwright.config.mjs no define proyectos por esquema de color y e2e/support/ no emula medios: no hay cobertura implicita.
- Base contractual: features/external_calendar.feature:543 (@s40) remite a la matriz de docs/ux-requirements.md; docs/ux-requirements.md:54 exige que toda variante personalizable, incluido el tema, pase sus verificaciones.
- Precedente vinculante: e2e/ics-calendar-ux.spec.mjs:304-327 (4 modos x 3 estados, axe por modo, fijando las tres preferencias en cada pasada porque emulateMedia conserva las no nombradas).
- progress/ux_external_calendar.md no existe; progress/tdd_external_calendar.md no menciona oscuro ni forced-colors.

CORRECCION DEL MECANISMO (lo que el bloqueante decia mal):
- Es FALSO que la pantalla anada tokens de color propios. styles.scss:2334-2430 declara solo color: var(--ink) y background: var(--editable) en el input; today.scss:67-92 no declara ningun color. Errores y estados usan clases globales preexistentes (.field-error, .failure, .save-status, .notice, .quiet-note, button, input) ya auditadas en oscuro y forced-colors por otras cinco rutas. El riesgo no es "paleta nueva sin medir" sino "pares de token nunca medidos en ESTA ruta y en ESTOS estados".
- prefers-reduced-motion debe SALIR del bloqueante: no hay transition, animation ni transform en los bloques nuevos, y el bloque global styles.scss:1292 solo resetea scroll-behavior. Ejecutarlo aqui no afirmaria nada.
- Bajo forced-colors el texto de error no "desaparece": .field-error es texto con aria-describedby y .failure lleva role="alert" mas borde var(--line); lo que se pierde es solo el matiz rojo. La comprobacion sigue valiendo la pena, pero por conformidad con el estandar, no por un fallo previsto.

LO QUE DE VERDAD PESA (y agrava el hueco): el unico test con axe cubre 3 pasadas (vacio, con suscripcion, tras sincronizar), mientras @s40:542 nombra cinco estados: vacio, con suscripcion, con error, con lista larga de resumenes Unicode y guardando. Los estados de error y guardando no pasan por axe ni siquiera en tema claro. Es el mismo patron de "3 de 7 estados" por el que el juez ya ha rechazado hoy.

REMEDIO MINIMO PARA LEVANTARLO: en e2e/external-calendar-ux-audit.spec.mjs, envolver la auditoria axe en un bucle de modos siguiendo el patron de ics-calendar-ux.spec.mjs:304-327 con base {colorScheme:"light", forcedColors:"none", reducedMotion:"no-preference"} y los modos claro / oscuro / forced-colors, recorriendo los cinco estados de @s40:542 (incluidos error y guardando, hoy sin oraculo axe), y registrar la matriz resultante en el progress/ux_external_calendar.md que aun no existe. No es necesario anadir pasada de movimiento reducido para esta pantalla; si se anade, debe declararse como comprobacion vacua y no contarse como evidencia.

### 14. [ALTA · varias horas] La lectura del cuerpo del feed no tiene ningún plazo: el timeout de 5 s solo cubre las cabeceras

- **Dimension:** seguridad
- **Rutas:** backend/src/main/java/com/apptolast/organization/adapter/feed/HttpCalendarFeed.java:74-84 y :44-49; backend/src/test/java/com/apptolast/organization/adapter/feed/HttpCalendarFeedTest.java:141-161; docs/external-calendar.md:41-42; project-spec.md:2490

**Evidencia.** HttpCalendarFeed.java:75-84: «while ((read = body.read(chunk)) >= 0) { buffer.write(chunk, 0, read); if (buffer.size() > LIMIT) return FeedFetch.failed(FeedError.FEED_TOO_LARGE); }» — el bucle no consulta ningún reloj ni instante límite; la única condición de corte es el tamaño. El plazo se declara en :48 sobre la petición («.timeout(timeout)») y en :36 como connectTimeout, y se consume con «BodyHandlers.ofInputStream()» (:54), de modo que send() retorna en cuanto llegan las cabeceras y las lecturas posteriores quedan fuera de él. La única prueba de plazo, HttpCalendarFeedTest.java:141-156, retiene deliberadamente las CABECERAS («aServerThatDoesNotSendHeadersInTimeIsUnreachable»); no hay ninguna con un cuerpo que gotee. HttpCalendarFeedTest.java:159-161 solo afirma que la constante vale 5 s. La documentación afirma una protección que el código no da: docs/external-calendar.md:41-42 dice que el rebinding «se mitiga [...] limitando lo que se puede hacer con la respuesta: solo se lee, con 200, tipo textual, 1 MiB y 5 s».

**Arreglo.** Fijar un instante límite antes de send() y comprobarlo dentro del bucle de lectura, abortando el cuerpo y devolviendo FEED_UNREACHABLE al superarlo (o usar sendAsync con orTimeout y un plazo total del intercambio, que es lo que B1 exige para la 25). Añadir un test con un HttpServer que envíe cabeceras y luego gotee el cuerpo, y comprobar que se corta dentro del plazo. Corregir docs/external-calendar.md:41-42 para que no prometa un plazo sobre la respuesta que hoy no existe.

**Correccion del verificador.** **Bloqueante mantenido, con estas tres correcciones al enunciado.**

**Título corregido:** «La lectura del cuerpo del feed no tiene plazo: con `BodyHandlers.ofInputStream()` el temporizador de `HttpRequest.timeout` se cancela al llegar las cabeceras, y ni el contrato ni las pruebas cubren un cuerpo que gotea.»

**Evidencia (verificada leyendo, incluido el fuente del JDK):**
- `backend/src/main/java/com/apptolast/organization/adapter/feed/HttpCalendarFeed.java:54` usa `ofInputStream()`; `:48` fija `.timeout(timeout)`; `:36` `connectTimeout(timeout)`; `:79-82` el bucle de lectura solo corta por tamaño.
- JDK (leído en `lib/src.zip` de jdk-21 y jdk-25; el toolchain del proyecto es 25, `backend/build.gradle:10`): `MultiExchange.responseAsyncImpl()` llama a `cancelTimer()` en el `handle(...)` que corre al obtener el `Response` (jdk-25 `MultiExchange.java:472-474`), y `responseAsync0` encadena `exch.readBodyAsync(...)` **después** (`:370`). `ResponseSubscribers.HttpResponseInputStream.getBody()` documenta «Returns the stream immediately, before the response body is received» y `current()` bloquea en `buffers.take()` (`:369`) sin plazo. Luego el plazo de 5 s no cubre ni un byte del cuerpo.
- Precedente interno que cierra la discusión sobre si es aceptable: `JdkWebhookSender` (feature 25) usa `EXCHANGE_TIMEOUT = 10 s` con `BodyHandlers.discarding()`; con un handler que consume el cuerpo, `send()` no retorna hasta que el suscriptor termina y el plazo sí cubre todo el intercambio. La 28 pierde esa propiedad únicamente por usar `ofInputStream`.

**Corrección 1 — encuadre contractual.** No es cierto que quede un renglón del contrato sin oráculo. La única fila de plazo del contrato es `features/external_calendar.feature:196`: «200 tras 6 s sin enviar cabeceras → FEED_UNREACHABLE», y esa fila SÍ está cubierta por `HttpCalendarFeedTest.java:141-156` (retiene cabeceras con un latch y usa un cliente de 300 ms). Lo que falla es (a) la cabecera del contrato, `features/external_calendar.feature:12`, que promete «timeout de 5 s» para la descarga sin acotarlo a las cabeceras, y (b) `docs/external-calendar.md:41-42` y `:43-44`, que afirman dos veces un plazo de 5 s sobre lo que se hace con la respuesta —una de ellas como mitigación explícita del rebinding aceptado como riesgo residual—. Es decir: documentación que afirma una protección inexistente, no una fila de ejemplos sin verificar.

**Corrección 2 — la amplificación por @s35, peor pero por otra vía.** El bloqueante dice que Hoy lo dispara sola en cada carga; el matiz importa. `features/external_calendar.feature:471` manda `POST /sync` con `onlyIfStale` **true**, y `SyncExternalCalendar.FRESH` son 15 minutos medidos sobre `lastAttemptAt` (`isStale`), lo que a primera vista parecería frenarlo. No lo frena: `lastAttemptAt` solo se escribe en `commit(...)`, después de que `feed.fetch` retorne. Mientras una descarga está colgada la suscripción sigue eternamente «rancia», así que **cada** carga de Hoy dispara otra sincronización que también se cuelga. Y el botón manual manda `onlyIfStale false`, sin ningún freno. Añádase `deploy/nginx.conf` con `proxy_read_timeout 15s`: el navegador recibe 504 en 15 s y el usuario reintenta, pero el hilo de Tomcat sigue bloqueado en `buffers.take()` —nginx no lo libera—. La acumulación es más rápida de lo que describe el bloqueante.

**Corrección 3 — radio de daño, y por tanto gravedad.** No transfieras entera la consecuencia de B1: allí el envío iba dentro de la transacción y lo que reventaba era el pool de Hikari (503 en toda la aplicación). Aquí la descarga ocurre fuera de toda transacción (`SyncExternalCalendar.attempt` corre antes de `commit`), así que **no** hay conexión de PostgreSQL retenida. El daño es agotamiento del pool de hilos de petición de Tomcat (200 por defecto), suficiente para dejar la aplicación entera sin atender, pero el mecanismo hay que enunciarlo bien. **Gravedad: alta**, sostenida: denegación de servicio de toda la aplicación provocable por cualquier cuenta autenticada con un host público propio, sin necesidad de rebinding ni de nada exótico.

**Qué haría falta para cerrarlo (mínimo):**
1. Un plazo real sobre el intercambio completo en `HttpCalendarFeed`: o bien un `BodySubscriber` propio que corte a 1 MiB con `sendAsync(...).orTimeout(TIMEOUT)`, o bien conservar `ofInputStream` pero llevar la lectura a un `CompletableFuture` con corte duro que cierre el cuerpo y devuelva `FEED_UNREACHABLE` al vencer. Cualquiera de las dos, no un cambio de constante.
2. Una prueba que discrimine, análoga a `s13_abortsAnEndlessBodyLongBeforeTwoMebibytes` pero en el eje del tiempo: handler que envíe cabeceras 200 `text/calendar` al instante y luego un byte cada N ms sin cerrar nunca; con un `HttpCalendarFeed` de plazo corto (p. ej. 300 ms, como ya hace la prueba de cabeceras) se afirma `FEED_UNREACHABLE` y que `fetch` retorna en menos de un par de segundos. Hoy esa prueba, escrita contra el código actual, no terminaría nunca: ese es el argumento de que discrimina.
3. Alinear `docs/external-calendar.md:41-44` y `features/external_calendar.feature:12` con lo que el código garantice de verdad, y añadir la fila correspondiente a la tabla de @s12 (cuerpo que gotea → `FEED_UNREACHABLE`) para que la dimensión quede verificada y no inferida.

### 15. [MEDIA · una hora] @s39 fila 4 (cierre de sesión) y la coletilla «la ruta vuelve a /» de @s38 fila 5 no se comprueban

- **Dimension:** contrato
- **Rutas:** frontend/src/external-calendar.test.tsx:266-282; frontend/src/external-calendar-route.test.tsx:58-64

**Evidencia.** @s39 fila 4 (features/external_calendar.feature:538) pide que al cerrar sesión «desaparecen host, cola, contadores y lista inmediatamente, la ruta se reinicia a / y no se conserva borrador». No hay ninguna prueba de cierre de sesión en el carril: `grep -rln "Cerrar sesión" frontend/src/*.test.tsx` lista appearance, authentication, availability, create-task, export-data, integration-api, split-task, task-state, today y work-session-end, pero no external-calendar. Lo más cercano es la prueba de 401 (línea 266), que es la fila 5 de @s38 y solo afirma que desaparecen «calendar.google.com» y «Reunión» y que el campo queda vacío: nunca comprueba `window.location.pathname === "/"`, que el Then de esa fila también exige. La prueba de ruta más cercana (external-calendar-route.test.tsx:58) monta App sin sesión desde el principio; no es una transición de cierre de sesión.

**Arreglo.** Añadir una prueba a nivel de App: con la vista /calendario-externo cargada y suscripción visible, disparar el cierre de sesión y afirmar que desaparecen host, cola, contadores y lista, que `window.location.pathname` vuelve a "/" y que al volver a entrar el campo de dirección está vacío; y añadir la afirmación de la ruta a la prueba de 401 existente.

**Correccion del verificador.** Título: @s39 fila 4 (cierre de sesión) no tiene oráculo en ningún nivel del carril

Rutas: features/external_calendar.feature:538 (fila sin cubrir); frontend/src/external-calendar.test.tsx (tres pruebas @s39: 347, 359, 376 — ninguna de cierre de sesión); e2e/external-calendar.spec.mjs (sin cierre de sesión); frontend/src/use-session.ts:208-228 (lista blanca isPrivateRoute) y use-session.ts:47-50 (único reinicio de ruta del producto).

Evidencia verificada: la fila 4 de @s39 exige que al cerrar sesión desaparezcan host, cola, contadores y lista, que la ruta se reinicie a / y que no se conserve borrador. No existe prueba de cierre de sesión para /calendario-externo ni en unitarias ni en e2e. La parte de «desaparecen los datos» está garantizada por construcción (session-gate.tsx desmonta <App/> y ExternalCalendar no persiste nada fuera del estado de React), de modo que el valor discriminante de la fila está en la ruta: el único replaceState(null,"","/") del producto (use-session.ts:49) se dispara en la transición anónimo→autenticado y sólo cuando la ruta actual NO está en isPrivateRoute; /calendario-externo vuelve a «/» exclusivamente porque no figura en esa lista blanca. Ninguna prueba fija esa ausencia: añadir "/calendario-externo" a isPrivateRoute (como la feature 24 añadió "/integraciones/api") dejaría la ruta privada viva tras el ciclo de sesión con la suite en verde.

Qué se pide: una prueba sobre SessionGate (no sobre ExternalCalendar aislado) que parta de /calendario-externo con suscripción y eventos visibles, pulse «Cerrar sesión», compruebe que host, contadores y lista desaparecen y que el borrador de «Dirección secreta iCal» no reaparece, y que tras volver a iniciar sesión window.location.pathname sea "/". El e2e existente sirve igual de bien como sede.

Qué NO se pide (rectificación del hallazgo original): no debe añadirse expect(window.location.pathname).toBe("/") a la prueba de 401 de external-calendar.test.tsx:266. Ese fichero nunca manipula window.history, así que la aserción pasaría con el producto vacío — sería un placebo. Además ExternalCalendar no toca la ruta en el 401 (external-calendar.tsx:181-183 delega en forget()); el corte de sesión ante 401 es responsabilidad de api-client.observeAccess → use-session.revoke() y ya está cubierto en authentication.test.tsx:307 (@s6) y :322 (@s7).

Punto a resolver con el contrato: literalmente, «la ruta se reinicia a /» al cerrar sesión no ocurre en ninguna feature de este producto — al cerrar, la URL permanece y el reinicio se produce en el siguiente inicio de sesión. O la prueba se escribe sobre esa transición (recomendado, es la conducta real y heredada), o hay que corregir la redacción de la fila; escribir la aserción tal cual al cerrar sesión fallaría.

Gravedad: media. Bloquea porque una fila del contrato se cierra sin oráculo y la conducta de ruta específica de esta feature no está fijada por nada, pero no hay fuga de datos observable hoy.

### 16. [MEDIA · minutos] La prueba de «no validar una lectura ya cancelada» no distingue el aborto del error de forma

- **Dimension:** oraculos
- **Rutas:** frontend/src/external-calendar-api.test.ts:329-339 (producción: frontend/src/external-calendar-api.ts:197)

**Evidencia.** it("no valida la respuesta de una lectura ya cancelada", async () => { … vi.fn().mockImplementation(async () => { controller.abort(); return Response.json({ configured: true, subscription: null }); }) … await expect(readExternalCalendar(controller.signal)).rejects.toThrow(); });

**Arreglo.** Devolver un cuerpo válido y exigir el error concreto del aborto (`rejects.toThrowError(expect.objectContaining({ name: "AbortError" }))` o comparar con `controller.signal.reason`), y afirmar además que no se lanzó el error de validación.

**Correccion del verificador.** Título corregido: "El único guardián de cancelación posterior a la respuesta (external-calendar-api.ts:197) no tiene oráculo que mate su borrado".

Rutas: frontend/src/external-calendar-api.test.ts:329-339 (prueba); frontend/src/external-calendar-api.ts:197 (producción); consumidores sin guarda propia: frontend/src/external-calendar.tsx:143-148 y :127-129.

Qué comprobé (por lectura, sin ejecutar):
- `json()` en external-calendar-api.ts llama `signal?.throwIfAborted()` dos veces: 191 (antes de `apiRequest`) y 197 (después). `apiRequest` en api-client.ts no lanza ante señal abortada.
- El test de 329-339 aborta dentro del mock y devuelve `{configured:true, subscription:null}`, payload que `snapshotOf` (137-150) ya declara inválido y que el test de la línea 105 cubre con mensaje exacto. La aserción es `rejects.toThrow()` pelado.
- Mutante concreto: borrar la línea 197. El flujo llega a `snapshotOf`, `invalid()` lanza `new Error("Respuesta de calendario externo inválida.")` y el test pasa igual. Revisé los demás candidatos y tampoco lo matan: today-external-calendar.test.tsx:212 y :248 son oráculos reales de las filas @s36 del contrato, pero today-external-calendar.tsx repite la guarda `if (signal.aborted) return;` tras cada await, así que sobreviven al mutante; external-calendar.test.tsx:376 solo comprueba `document.body.textContent === ""` tras unmount, cierto en cualquier caso.
- Consecuencia de producto, no solo de test: en external-calendar.tsx la vía de ÉXITO no comprueba `signal.aborted` (`setSubscription(snapshot.subscription); setLoaded(true);` en 144-145 y `setEvents(view.items); setInvalidList(false);` en 128-129). Con la 197 fuera, una lectura superada por `start()` (que aborta la anterior en :110) repinta suscripción y eventos rancios sobre estado más fresco en /calendario-externo, y ninguna prueba de la suite lo ve.

Arreglo mínimo (de test, no de producto; producción es correcta hoy):
1. En 329-339, usar un payload VÁLIDO (`{configured:false, subscription:null}` o `{configured:true, subscription}`) para que la validación de forma no pueda enmascarar el abort, y afirmar el error concreto, p. ej. `.rejects.toThrow(DOMException)` o comprobando `name === "AbortError"` sobre el rechazo. Con eso, borrar la 197 rompe la prueba y borrar la validación de forma no la enmascara.
2. Añadir una prueba de componente en external-calendar.test.tsx para la fila de producto que hoy queda sin oráculo: con una lectura inicial retenida, disparar Guardar/Sincronizar (que llama `start()` y aborta la anterior), liberar después la respuesta vieja con una suscripción distinta y afirmar que la pantalla sigue mostrando la nueva. Alternativa aceptable si se prefiere no ampliar la suite: replicar en external-calendar.tsx la guarda `if (signal.aborted) return;` en las vías de éxito de 144-145 y 128-129, dejando la 197 como defensa en profundidad.

Nota de alcance no verificada: existen dos `throwIfAborted` más en 223 y 225 (`deleteExternalCalendar`) cuyos consumidores no revisé; puede aplicarles la misma clase de hueco, pero no lo afirmo.

Gravedad: media (correcta como estaba). Es un arreglo de una línea de prueba más, opcionalmente, una prueba de componente; no bloquea por comportamiento incorrecto en producción sino por ausencia de oráculo sobre un guardián portante.

### 17. [MEDIA · minutos] El foco visible se comprueba en un solo control, no «en cada control»

- **Dimension:** oraculos
- **Rutas:** e2e/external-calendar-ux-audit.spec.mjs:105-121

**Evidencia.** for (let step = 0; step < 40 && seen.length < expected.length; step++) { await page.keyboard.press("Tab"); … if (expected.includes(name) && seen.at(-1) !== name) seen.push(name); } expect(seen).toEqual(expected); const outline = await page.evaluate(() => getComputedStyle(document.activeElement).outlineWidth); expect(outline).not.toBe("0px");

**Arreglo.** Medir `outlineWidth` (o el indicador que se use) dentro del bucle cada vez que se reconoce uno de los cinco controles, y acumular también los nombres no esperados para poder afirmar que no se cuela ningún foco intermedio dentro del formulario.

**Correccion del verificador.** BLOQUEANTE (media) — @s40 exige foco visible «en cada control» y el oráculo lo mide una vez, con un predicado que ni siquiera discrimina.

Ruta: e2e/external-calendar-ux-audit.spec.mjs:105-121 (test «el recorrido con teclado sigue el orden del contrato @s40»).
Contrato: features/external_calendar.feature:545 — «...con foco visible en cada control».

Hecho comprobado por lectura:
- El bucle (106-115) sale en cuanto `seen` alcanza los cinco nombres, de modo que la única medición (117-120) se hace sobre el elemento activo en ese instante: el botón «Eliminar suscripción». Etiqueta, Dirección secreta iCal, Guardar y Sincronizar ahora nunca se miden.
- El predicado es `expect(outline).not.toBe("0px")` sobre `outlineWidth` solamente: sin outlineStyle, sin color no transparente, sin `matches(":focus-visible")`.

Cambio de producto que DEBERÍA romperlo y no lo rompe (el plausible, no uno inventado): borrar la regla global `:focus-visible { outline: 3px solid var(--accent); outline-offset: 4px; }` de frontend/src/styles.scss:621-624 — que es la ÚNICA fuente del anillo de foco de esta pantalla, porque el bloque `.external-calendar` (styles.scss:2334-2415) no declara ningún outline y los cinco controles son input/button nativos (frontend/src/external-calendar.tsx:275, 294, 321, 358, 365). Con la regla borrada, Chromium sigue pintando su anillo de agente de usuario para `:focus-visible` y `outlineWidth` computa distinto de "0px": la prueba sigue verde con los cinco controles sin el indicador del producto. Y, secundariamente, cualquier override que apague el anillo en los dos inputs (o en «Guardar»/«Sincronizar ahora») pasaría inadvertido, porque solo se mira el último botón.

Sin cobertura alternativa: ningún otro spec visita /calendario-externo; e2e/external-calendar.spec.mjs no contiene focus/Tab/outline; frontend/src/external-calendar.test.tsx solo hace `button.focus()` (línea 424) sin medir; axe-core no tiene regla de visibilidad de foco, así que las tres pasadas de axe del mismo fichero no lo suplen.

Estándar ya vigente en este repo (y escrito tras un rechazo previo del juez por esto mismo): e2e/github-connector.spec.mjs:293-338 tabula por todos los controles de `main` y acumula `invisible[]` exigiendo `matches(":focus-visible")` && outlineStyle!=="none" && outlineWidth>=1 && color no transparente; e2e/ics-calendar-ux.spec.mjs:425-467 valida «foco visible» parada por parada y además comprueba que no hay trampa de foco hacia delante ni hacia atrás.

Remedio: medir el anillo DENTRO del bucle, en cada una de las cinco paradas esperadas, con el predicado del precedente (`matches(":focus-visible")`, outlineStyle !== "none", outlineWidth >= 1 y outlineColor sin "transparent", o box-shadow equivalente), acumulando los fallos y afirmando `expect(invisible).toEqual([])` con el nombre del control en el mensaje.

NO forma parte de este bloqueante (retirar de la versión original): la queja de que «el bucle ignora elementos intermedios, luego no verifica que los cinco controles sean consecutivos». El contrato pide que el recorrido «siga el orden», no contigüidad, y el producto interpone focusables legítimos —la prueba arranca desde «Saltar al contenido» (frontend/src/workspace.tsx:28-30) y después va la barra lateral—, así que filtrar los intermedios es correcto. Si se quiere endurecer ahí, el hueco real y distinto es la ausencia de comprobación de trampa de foco (salir de main hacia delante y hacia atrás), como sí hace ics-calendar-ux.spec.mjs; eso sería a lo sumo un aviso, no un bloqueante de @s40.

### 18. [MEDIA · una hora] El Stryker de la feature deja fuera la ruta y la entrada de navegacion que exige @s37

- **Dimension:** mutacion
- **Rutas:** frontend/stryker.external-calendar.config.json:6-10; frontend/src/App.tsx:46,59,92-93; frontend/src/workspace.tsx:81-86

**Evidencia.** El array `mutate` del config solo lista src/external-calendar-api.ts, src/external-calendar.tsx y src/today-external-calendar.tsx. Sin embargo el carril tambien vive en App.tsx:46 (`const externalCalendar = route === "/calendario-externo";`), App.tsx:59 (rama de la cadena de `section`), App.tsx:92-93 (`) : externalCalendar && username ? ( <ExternalCalendar />`) y workspace.tsx:81-86 (el RouteLink con `aria-current={section === "Calendario externo" ? "page" : undefined}` en la linea 83). La convencion del repositorio es incluir esos tramos: stryker.ics-calendar.config.json:12-15 lista "src/App.tsx:40:8-40:42", "src/App.tsx:65:20-66:34", "src/App.tsx:106:10-107:37" y "src/workspace.tsx:99:10-104:22"; stryker.appearance.config.json:14-17 hace lo mismo con "src/App.tsx:38:8-38:44", "src/App.tsx:69:24-81:40", "src/App.tsx:110:10-151:7" y "src/workspace.tsx:87:10-92:22".

**Arreglo.** Anadir a `mutate` de frontend/stryker.external-calendar.config.json los rangos linea:columna (0-based, fin exclusivo, como los usan los otros dos configs) de App.tsx:46 (la declaracion `externalCalendar = route === "/calendario-externo"`), de la rama de `section` en App.tsx:59 y de la rama de render App.tsx:92-93, mas el bloque workspace.tsx:81-86; y anadir la afirmacion correspondiente al test «external calendar Stryker configuration mutates only its own files» de scripts/project.test.mjs:2094 en adelante. Ojo: hay que recalcular los rangos sobre el arbol actual, no copiar los que anoto la bitacora (progress/tdd_external_calendar.md:526-529), que ya estan desfasados.

**Correccion del verificador.** Titulo: El gate de mutacion de external_calendar no genera mutantes sobre la ruta ni la entrada de navegacion que @s37 exige.

Gravedad: media (confirmada). No hay defecto de comportamiento ni prueba placebo; hay un tramo de produccion introducido por la propia feature, con contrato Gherkin explicito, que queda fuera del alcance del gate de cierre.

Hechos verificados por lectura:
- frontend/stryker.external-calendar.config.json:6-10 declara solo "src/external-calendar-api.ts", "src/external-calendar.tsx", "src/today-external-calendar.tsx".
- El carril tambien vive en frontend/src/App.tsx:46 (const externalCalendar = route === "/calendario-externo";), App.tsx:59 (": externalCalendar" en la cadena de section), App.tsx:92-93 (") : externalCalendar && username ? ( <ExternalCalendar />") y frontend/src/workspace.tsx:81-86 (el RouteLink a /calendario-externo, con aria-current={section === "Calendario externo" ? "page" : undefined} en la linea 83).
- git log -L sobre esas lineas devuelve el commit 8b951cc "feat(external_calendar): pantalla de gestion y ruta /calendario-externo": es "lo tocado" por la feature 28 en el sentido de AGENTS.md:86.
- features/external_calendar.feature:496-509 (@s37) exige literalmente que «la entrada de navegacion "Calendario externo" tiene aria-current "page"» y llegar por enlace de navegacion, URL directa y retorno tras iniciar sesion.
- La convencion del repositorio esta viva y es line-accurate contra el arbol actual: stryker.appearance.config.json:14-17 lista "src/App.tsx:38:8-38:44", "src/App.tsx:69:24-81:40", "src/App.tsx:110:10-151:7", "src/workspace.tsx:87:10-92:22", que hoy son exactamente el literal de /apariencia, su rama de section, su rama de render y su RouteLink. stryker.ics-calendar.config.json:11-14 hace lo mismo para /calendario. De los tres .feature que nombran aria-current (appearance:228, today:416, external_calendar:500), solo external_calendar omite estos tramos.
- No hay coartada de cobertura en otro config: stryker.config.json mutaria App.tsx y workspace.tsx enteros, pero su ultimo commit es 0277c50 (feature 24), no incluye ningun modulo de external-calendar, y scripts/project.mjs:226-235 fija que el target external_calendar-frontend ejecuta stryker.external-calendar.config.json.

Lo que el enunciado original decia de mas o de menos:
- Las entradas de stryker.ics-calendar.config.json estan en sus lineas 11-14, no 12-15.
- El hueco es algo MAYOR de lo descrito: external-calendar-route.test.tsx contiene 4 pruebas (contadas) que si discriminan — entrada por enlace con asercion de aria-current="page", URL directa con aria-current y nav "Principal" visible, miga de pan del banner, y no montar la vista sin sesion — pero NO cubre la tercera entrada de @s37, "retorno tras iniciar sesion". Es decir: para enlace y URL directa falta el mutante; para el retorno tras login falta tambien el oraculo.

Remedio (verificable sin ejecutar nada): anadir al array mutate de stryker.external-calendar.config.json los cuatro tramos, con los mismos offsets 0-based que usa el resto del repositorio:
  "src/App.tsx:46:8-46:58"      (el declarador externalCalendar = route === "/calendario-externo")
  "src/App.tsx:59:14-81:40"     (su rama de la cadena de section hasta el cierre en ": null")
  "src/App.tsx:92:10-151:7"     (su rama de la cadena de render)
  "src/workspace.tsx:81:10-86:22" (el RouteLink completo con el ternario del aria-current)
y anadir una prueba de @s37 para la entrada "retorno tras iniciar sesion". Reejecutar despues el target external_calendar-frontend y dejar la evidencia en progress/mutation_external_calendar.md, que hoy no existe en el repositorio (solo hay gherkin_, proposal_ y tdd_ de esta feature).

### 19. [BAJA · una hora] @s25: la cláusula de la lectura concurrente durante la sincronización no tiene oráculo

- **Dimension:** contrato
- **Rutas:** features/external_calendar.feature:353; backend/src/test/java/com/apptolast/organization/adapter/persistence/ExternalCalendarPersistenceTest.java:220-258, :477-487

**Evidencia.** @s25 tiene tres Then: la sustitución completa de la instantánea (cubierto por s25_aSuccessfulSyncReplacesTheWholeSnapshot, línea 220: afirma exactamente u2 con el summary nuevo y u3), que ninguna otra tabla ni outbox cambia (cubierto por s25_syncingDoesNotTouchTheOutbox, línea 477) y «una lectura concurrente durante la sincronización ve la lista anterior completa o la nueva completa, nunca una vacía ni mezclada». Este tercero no lo comprueba ninguna prueba: no hay ningún hilo ni conexión secundaria en el fichero, que es secuencial de principio a fin. La atomicidad existe por construcción (PostgresExternalCalendarStore.commitSuccess envuelve el UPDATE y el reemplazo en `writing(...)` sobre un TransactionTemplate, línea 125), pero nada lo fija.

**Arreglo.** En el test de Testcontainers, lanzar la lectura desde una segunda conexión mientras un hilo ejecuta commitSuccess con una lista grande, y afirmar que el resultado es exactamente la lista anterior completa o la nueva completa; alternativamente, fijar por prueba que commitSuccess se ejecuta dentro de una única transacción.

**Correccion del verificador.** BLOQUEANTE (gravedad: media, no baja).

Título: @s25 — la cláusula de la lectura concurrente durante la sincronización no tiene oráculo, y tampoco lo tiene el modo de lectura que la sostiene.

Rutas verificadas:
- features/external_calendar.feature:348-355 (@s25; la cláusula es la línea 353).
- backend/src/test/java/com/apptolast/organization/adapter/persistence/ExternalCalendarPersistenceTest.java:220-258 y :477-487 (los dos únicos tests s25_ del carril).
- backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresExternalCalendarStore.java:125 (`return writing(` en commitSuccess) y :261-265 (`reading()`: TransactionTemplate con setReadOnly(true) e ISOLATION_REPEATABLE_READ).

Hechos comprobados leyendo:
1. @s25 tiene tres Then. El primero ("los eventos almacenados son exactamente u2 con el summary nuevo y u3") lo cubre s25_aSuccessfulSyncReplacesTheWholeSnapshot (:220) con containsExactly("u2","u3") y summary "Nuevo".
2. El tercero ("ninguna otra tabla, outbox ni historial cambia") está cubierto SOLO A MEDIAS: s25_syncingDoesNotTouchTheOutbox (:477) comprueba únicamente `SELECT count(*) FROM outbox_events = 0`. No comprueba "ninguna otra tabla" ni "historial". El revisor original lo dio por cubierto entero; no lo está.
3. El segundo Then (lectura concurrente) no tiene oráculo alguno. Los 12 ficheros de test que mencionan ExternalCalendar no contienen ni un solo Thread, ExecutorService, CountDownLatch, CompletableFuture ni conexión secundaria (única coincidencia del grep: appender.start() de logback en ExternalCalendarAuditTest:23, irrelevante). El carril es secuencial de principio a fin.
4. La garantía existe por construcción en DOS piezas, ninguna fijada por una prueba: el lado escritor (commitSuccess:125 envuelve UPDATE + DELETE + insert en `writing`) y el lado lector (`reading()`:261-265, read-only REPEATABLE READ), usado por events() (:173).

Por qué bloquea:
- Un Then de tres queda sin verificar y un segundo está verificado solo en su tercio de outbox.
- Escenario de fallo concreto: si alguien saca `jdbc.update("DELETE FROM external_calendar_events ...")` e `insert(...)` fuera del lambda de `writing` en commitSuccess (dejando el guard `if (updated == 0) return empty` intacto), TODA la suite sigue verde — s25_aSuccessfulSyncReplacesTheWholeSnapshot y s26_aSyncThatLostTheVersionRaceChangesNothing solo miran el estado final secuencial — y una lectura concurrente de Hoy vería la lista vacía. PIT tampoco lo detecta: sus operadores no reestructuran transacciones.
- Segundo escenario: quitar setReadOnly/setIsolationLevel de `reading()` no rompe ninguna prueba del repositorio.

Por qué no es "riesgo aceptado" ni cláusula intestable:
- progress/proposal_external_calendar.md:79 comprometió explícitamente "PostgreSQL con Testcontainers para ... reemplazo atómico ... y dos sincronizaciones concurrentes". Ese test no existe.
- El repositorio ya resuelve exactamente este oráculo en otra feature: HistoryReadTransactionTest.s22_committedWriterCannotChangeFactsOrLabelsInsideThePageSnapshot subclasifica JdbcTemplate para lanzar, desde dentro de la consulta, un escritor real que confirma en otro hilo, y afirma que la lectura no ve el cambio. La técnica y el listón ya están en casa.
- progress/tdd_external_calendar.md:328 mapea "s25 → ExternalCalendarPersistenceTest.s25_*", es decir, la propia bitácora del artesano declara cubierto todo @s25 con esos dos tests. No lo está: es una traza inexacta más.

Remedio mínimo aceptable: un test en ExternalCalendarPersistenceTest, con la técnica de HistoryReadTransactionTest, que (a) precargue u1+u2, (b) interrumpa commitSuccess entre el DELETE y el insert (o entre el UPDATE y el DELETE) y desde otro hilo/conexión invoque store().events(A, ...), y (c) afirme que el resultado es containsExactly("u1","u2") o containsExactly("u2","u3"), nunca vacío ni mezclado. Y ampliar s25_syncingDoesNotTouchTheOutbox para cubrir el "ninguna otra tabla ni historial" que su título y el contrato prometen.

