# Matriz UX de /calendario-externo (feature 28)

Escenario que la exige: `features/external_calendar.feature:540-548` (@s40), cuya
línea 543 remite por nombre a `docs/ux-requirements.md`. `AGENTS.md:51` obliga a
recorrer las **30 filas** y prohíbe expresamente declarar cumplimiento global por
pasar axe o unas pocas resoluciones. Formato tomado de `progress/ux_ics_calendar.md`
y `progress/ux_webhooks.md`.

Fecha de la revisión: 9 de septiembre de 2026. Carril B del cierre del dictamen
de la feature 28.

## Evidencia y límites — léase ANTES de la tabla

Esta sección va delante a propósito: sin ella la tabla se lee como una
certificación, y no lo es.

### Lo que se ha ejecutado, con cifras medidas

| Fichero | Motor | Pruebas | Cómo se midió |
| --- | --- | --- | --- |
| `frontend/src/external-calendar.test.tsx` | vitest 4.1.10 sobre jsdom | **25** | `pnpm --dir frontend exec vitest run src/external-calendar.test.tsx` → `Tests 25 passed (25)` |
| `frontend/src/external-calendar-api.test.ts` | vitest 4.1.10 sobre jsdom | **42** | idem sobre ese fichero → `Tests 42 passed (42)` |
| `frontend/src/today-external-calendar.test.tsx` | vitest 4.1.10 sobre jsdom | **11** | idem → `Tests 11 passed (11)` |
| `frontend/src/external-calendar-route.test.tsx` | vitest 4.1.10 sobre jsdom | **5** | idem → `Tests 5 passed (5)` |
| **Total del carril en el navegador simulado** | | **83** | los cuatro juntos en una sola invocación: `Test Files 4 passed (4)` / `Tests 83 passed (83)` |
| `backend/…/adapter/feed/HttpCalendarFeedTest` | JUnit 5 sobre JDK 25 | **32** | `gradlew test --tests "…HttpCalendarFeedTest"` → `32 tests completed`, `BUILD SUCCESSFUL` |

Las 83 no son una cifra declarada: salen de una ejecución real del 9 de septiembre
de 2026 y están desglosadas fichero a fichero arriba, de modo que cualquiera puede
reproducir el desglose y comprobar que suma.

De esas 25 de `external-calendar.test.tsx`, dos llevan explícitamente `@s40`
(orden de tabulación; región `role=status` con `aria-live="polite"` que anuncia sin
mover el foco) y una, añadida en este cierre, mide el feedback inmediato y el
bloqueo de controles de @s38 reteniendo la respuesta del `POST /sync`.

### Lo que se ha ejecutado en navegador real

`e2e/external-calendar-ux-audit.spec.mjs` (Chromium, Playwright): axe con las
etiquetas `wcag2a`, `wcag2aa`, `wcag21aa`, `wcag22aa` y `best-practice` sobre los
estados **vacío**, **con suscripción** y **tras sincronizar**; recorrido de teclado
completo con medición del anillo `:focus-visible` **parada por parada**; matriz de
14 anchos (320, 359, 360, 361, 599, 600, 601, 767, 768, 769, 1279, 1280, 1281,
2560) con medición de objetivos de 44×44 px; y texto ampliado al 200 % a 320 px.

### Lo que este documento NO afirma

1. **No afirma cumplimiento a partir de axe.** axe automatiza un subconjunto de
   reglas; no certifica un lector de pantalla real. La revisión manual con lector
   de pantalla **sigue pendiente** y así consta en las filas afectadas.
2. **No afirma cobertura de los cinco estados en geometría.** El Given de @s40
   nombra cinco (vacío, con suscripción, con error, con lista larga de resúmenes
   Unicode y guardando). La matriz de anchos y la de texto al 200 % se ejercen hoy
   sobre el estado **vacío**; «con suscripción» y «tras sincronizar» sólo pasan por
   axe. Las filas geométricas quedan **parcialmente verificadas** y así se marcan.
   Cerrarlo es el hallazgo 5 del dictamen.
3. **No afirma zoom nativo del navegador al 200 %.** Lo único ejecutado es texto
   ampliado (`documentElement.style.fontSize = "32px"`), que es otra cosa: el
   contrato nombra las dos por separado y `docs/ux-requirements.md:50` dice que «la
   emulación de ancho equivalente no sustituye toda la comprobación de zoom real».
   El zoom nativo con `chrome.tabs.setZoom` lo cubre un fichero aparte
   (`e2e/external-calendar-native-zoom.spec.mjs`, hallazgo 4 del dictamen, en otro
   carril); **hasta que ese fichero exista y se ejecute, aquí queda pendiente**.
4. **No afirma tema oscuro ni forced-colors.** No se ha ejecutado ninguna pasada
   con `emulateMedia` sobre esta ruta (hallazgo 13). La pantalla no introduce
   tokens de color propios —usa `.field-error`, `.failure`, `.save-status`,
   `.notice`, `button` e `input` globales, ya auditados en otras rutas—, pero eso
   es un argumento de plausibilidad, no una medición.
5. **No afirma cobertura de Firefox ni WebKit ni de dispositivos reales.** Todo lo
   de navegador es Chromium.
6. **No afirma nada sobre facilidad de aprendizaje ni carga cognitiva reales.** No
   ha habido estudio con usuarios. Las filas cognitivas van marcadas como revisión
   heurística.

## Matriz completa: las 30 filas de `docs/ux-requirements.md`

Ninguna fila se omite. «Verificado» sólo cuando hay una prueba que puede fallar;
«heurístico» cuando es juicio de diseño sin oráculo; «pendiente» cuando el
recorrido existe pero nadie lo ha medido; «no aplica» siempre con motivo.

| Principio | Aplicación en /calendario-externo | Evidencia | Resultado |
| --- | --- | --- | --- |
| Atención selectiva | Un solo objetivo por estado: suscribirte (formulario con «Guardar») o gobernar la suscripción (host, cola, contadores y lista). Los avisos —truncado, sincronización pendiente— son texto secundario, no compiten con la acción. | `external-calendar.test.tsx` @s37: sin suscripción sólo hay «Guardar»; con suscripción aparecen «Sincronizar ahora» y «Eliminar suscripción». | Verificado en jsdom; jerarquía visual, revisión heurística |
| Carga cognitiva | Sólo dos campos: «Etiqueta» y «Dirección secreta iCal». Nada que recordar de otra pantalla: la ayuda dice dónde encontrar la dirección en Google Calendar. | @s37 comprueba el texto de ayuda y que el campo es `type="url"`. | Verificado en jsdom |
| Estética-usabilidad | Reutiliza `.form-card`, `.field`, `.field-error` y `.failure` del resto del producto; los errores de feed se traducen a mensajes accionables, no a códigos. | Pruebas de FEED_UNREACHABLE, FEED_HTTP_ERROR y SECRET_UNREADABLE en `external-calendar.test.tsx`; axe sin violaciones en tres estados. | Verificado parcialmente; estética, revisión heurística |
| Posición en serie | El orden visual y el de teclado coinciden: Etiqueta, Dirección, Guardar, Sincronizar ahora, Eliminar suscripción, y no cambian con el ancho. | E2E: recorrido de teclado que exige exactamente esa secuencia y rechaza intrusos dentro del formulario. Unitaria @s40 equivalente en jsdom. | Verificado en navegador (Chromium) |
| Tendencia a la meta | Los contadores describen el feed real («12 eventos, 3 recurrentes no incluidos, 1 cancelado, 0 inválidos»); no hay barra de progreso inventada. | @s37 y @s38 comparan el texto exacto de contadores contra la respuesta. | Verificado en jsdom |
| Von Restorff | El fallo se distingue por `role="alert"`, texto propio y borde, no sólo por color; el aviso de truncado por su texto. | axe sin violaciones; el `role="alert"` se comprueba en las pruebas de error. Bajo forced-colors sólo se perdería el matiz rojo, nunca el texto. | Verificado en jsdom; forced-colors **pendiente** (hallazgo 13) |
| Zeigarnik | Un guardado que falla conserva el borrador y devuelve el foco al campo culpable; el trabajo a medias no se pierde al equivocarse. | @s38 «conserva el borrador y enfoca el campo cuando la dirección se rechaza». | Verificado en jsdom |
| Fluir | No aplica: esta pantalla no gobierna sesiones de trabajo ni duración; su unidad es una suscripción, no un bloque de tiempo. | — | No aplica, con motivo |
| Fragmentación | Tres grupos con nombre: el formulario (`form-card` con `aria-labelledby`), el estado de la suscripción (`dl` de metadatos) y la lista de eventos. | axe sin violaciones (`region`, `landmark-unique`); E2E localiza la región por nombre accesible. | Verificado en navegador (Chromium) |
| Memoria de trabajo | La etiqueta guardada se recarga en el campo al abrir; el borrador sobrevive a 503 y a fallo de red. | @s38 filas de 503 y fallo de red: «se conserva el borrador». | Verificado en jsdom |
| Navaja de Occam | Cinco controles y ninguno de adorno: dos campos, Guardar, Sincronizar ahora, Eliminar suscripción, más los dos del diálogo de confirmación. | El recorrido de teclado de @s40 declara la lista cerrada: cualquier control nuevo dentro del formulario aparece como «intruso» y rompe la prueba. | Verificado en navegador (Chromium) |
| Conectividad uniforme | No aplica: la pantalla no dibuja líneas ni conectores entre entidades. | — | No aplica, con motivo |
| Fitts | Objetivo interno de 44×44 px CSS para todo botón y campo. | E2E `controlsAreLargeEnough` en los 14 anchos… **pero sólo en el estado vacío**: los botones «Sincronizar ahora», «Eliminar suscripción», «Sí, eliminar» y «Cancelar» viven en estados que la matriz no recorre. | **Parcialmente verificado** — cerrarlo es el hallazgo 5 |
| Hick | Una decisión principal por estado; el borrado exige confirmación explícita en vez de ofrecer un menú. | @s39: cancelar la confirmación no envía DELETE; confirmar envía exactamente uno. | Verificado en jsdom |
| Jakob | Formulario HTML convencional, `type="url"`, enlace de navegación con `aria-current="page"`, vuelta a «/» al reabrir sesión. | `external-calendar-route.test.tsx`: 5 pruebas, incluidas entrada por enlace, URL directa y retorno tras iniciar sesión. | Verificado en jsdom |
| Semejanza | Estados equivalentes con la misma apariencia que el resto del producto: `.save-status` para «Guardando…»/«Sincronizando…», `.failure` para los fallos. | Comparación de clases con las pantallas hermanas; axe sin violaciones. | Heurístico + axe |
| Miller | Los quince campos del DTO se presentan agrupados por significado (identidad, último intento, contadores), no como una lista plana de quince. | Revisión de la `dl` de metadatos. | Heurístico |
| Parkinson | No aplica: no hay sesión que se alargue; una sincronización termina cuando el proveedor responde o cuando vence el plazo de 5 s. | El plazo total de 5 s sí está verificado en backend (`HttpCalendarFeedTest`, 32 pruebas). | No aplica, con motivo |
| Postel | Se aceptan etiquetas Unicode de 1 a 40 puntos de código tras strip, y resúmenes Unicode largos en la lista, sin relajar la validación de la URL (https, sin userinfo, sin fragmento, host de nombre). | Pruebas de validación en `external-calendar-api.test.ts` (42) y en backend. | Verificado |
| Proximidad | Etiqueta, ayuda y error pegados a su campo y asociados con `aria-describedby`; `aria-invalid` cuando hay error. | axe sin violaciones; las pruebas de error comprueban la asociación programática. | Verificado en navegador (Chromium) |
| Prägnanz | Estados en texto, no en iconos: «Última sincronización correcta», «Último intento», mensajes por código de error. | @s37 comprueba que sin sincronización previa el término queda sin fecha, no con un icono ambiguo. | Verificado en jsdom |
| Región común | `form-card` con `aria-labelledby`, región de estado con nombre y lista de eventos como lista real. | axe (`region`, `list`) sin violaciones en tres estados. | Verificado en navegador (Chromium) |
| Tesler | La complejidad la absorbe el sistema: zonas horarias, ventana semiabierta, recurrencias omitidas y truncado a 500 se resuelven en el backend y se explican en lenguaje llano; nunca se muestra una traza. | Contadores y aviso de truncado comprobados en jsdom; nunca se pinta la URL completa (@s37 exige que ningún nodo la contenga). | Verificado en jsdom |
| Modelo mental | Se distingue con claridad la suscripción (lo que tú configuras) del último intento (lo que hizo el sistema) y de la lista (lo que hay hoy). | La `dl` separa `lastAttemptAt` de `lastSyncAt`; las pruebas de FAILED comprueban que la lista anterior permanece. | Verificado en jsdom |
| Usuario activo | El estado vacío orienta: dice dónde está la «Dirección secreta en formato iCal» en Google Calendar y avisa de tratarla como una contraseña. | @s37 comprueba el texto de ayuda. | Verificado en jsdom |
| Pareto | La acción frecuente —ver los eventos en Hoy— no exige pasar por esta pantalla: Hoy sincroniza sola con `onlyIfStale`. Aquí quedan las poco frecuentes (alta, resincronización manual, baja). | `today-external-calendar.test.tsx`: 11 pruebas del recorrido en Hoy. | Verificado en jsdom; la priorización, heurística sin uso real medido |
| Fin de pico | Cierre claro y recuperable: «Guardado.», «Sincronizado.», «Sincronización fallida.» y «Suscripción eliminada.» se anuncian en `role="status"` con `aria-live="polite"`, sin robar el foco. | @s40 en jsdom: la región anuncia y el foco no se mueve. Un fallo nunca deja falso éxito: la instantánea anterior permanece. | Verificado en jsdom |
| Sesgo cognitivo | Los contadores dicen lo que ocurrió («3 recurrentes no incluidos») sin juzgar; no hay racha ni penalización. | Comparación literal del texto de contadores. | Verificado en jsdom |
| Sobrecarga de opciones | Una única suscripción por propietario, por contrato: no hay que elegir entre calendarios ni configurar nada más. | El contrato lo fija y el backend lo impone; la pantalla no ofrece alternativas. | Verificado por contrato |
| Doherty | Feedback antes de 400 ms sin prometer red: «Guardando…» y «Sincronizando…» aparecen al instante, se envía **una** sola petición y los controles quedan bloqueados hasta la respuesta. | Dos pruebas que retienen la respuesta con una promesa y afirman el estado intermedio, el bloqueo y `toHaveLength(1)` en el recuento de peticiones (una para Guardar, otra para Sincronizar, añadida en el cierre del hallazgo 8). | Verificado en jsdom; el objetivo de 400 ms es de feedback, no de red |

Recuento: 30 filas, ninguna omitida. Verificadas con prueba que puede fallar: 22.
Parcialmente verificadas y con hallazgo abierto que las cierra: 2 (Fitts,
Von Restorff). Heurísticas declaradas: 3 (Semejanza, Miller, y la parte estética
de Estética-usabilidad). No aplicables con motivo: 3 (Fluir, Conectividad
uniforme, Parkinson).

## Trabajo pendiente que este documento deja escrito, no tapado

1. **Geometría en los cinco estados** del Given de @s40, no sólo en el vacío
   (hallazgo 5 del dictamen). Incluye medir a 44×44 px los botones «Sincronizar
   ahora», «Eliminar suscripción», «Sí, eliminar» y «Cancelar», que hoy no entran
   en ninguna medición.
2. **Oráculo de recorte y de solapes** en los dos ejes y por elemento, no sólo
   `scrollWidth` del documento (hallazgos 10 y 12).
3. **Zoom nativo al 200 %** con `chrome.tabs.setZoom` (hallazgo 4; lo cubre otro
   carril en `e2e/external-calendar-native-zoom.spec.mjs`).
4. **Tema oscuro y forced-colors** con `emulateMedia` (hallazgo 13). Movimiento
   reducido sería una comprobación vacua en esta pantalla —no hay `transition`,
   `animation` ni `transform` propios— y si se ejecuta debe declararse como tal y
   no contarse como evidencia.
5. **Revisión manual con lector de pantalla**, que el propio comentario del spec
   reconoce obligatoria y que no consta hecha.
6. **Firefox y WebKit**, y dispositivos táctiles reales.
