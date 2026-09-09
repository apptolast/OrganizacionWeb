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

Actualizado el **10 de septiembre de 2026**, tras cerrar los hallazgos 1, 5, 10 y 13.
Todo con `E2E_WEB_PORT=18092` sobre la pila real, `5 passed`.

`e2e/external-calendar-ux-audit.spec.mjs` (Chromium, Playwright), 5 pruebas:

1. **Matriz geométrica**: los **14 anchos** (320, 359, 360, 361, 599, 600, 601, 767,
   768, 769, 1279, 1280, 1281, 2560) por los **seis estados** de pantalla = 84
   mediciones. Cada medición comprueba cinco cosas: desbordamiento horizontal de la
   página, controles fuera del viewport, objetivos menores de 44 × 44 px, **recorte
   por elemento en los dos ejes** y **solapes por pares de rectángulos**.
2. **axe** con `wcag2a`, `wcag2aa`, `wcag21aa`, `wcag22aa` y `best-practice` sobre
   vacío, con suscripción y tras una sincronización real contra un proveedor
   inalcanzable.
3. **Cuatro modos × seis estados** = 24 pasadas de axe + geometría: claro, oscuro,
   `forced-colors: active` y `prefers-reduced-motion: reduce`. Cero violaciones.
4. **Recorrido de teclado** completo con medición del anillo `:focus-visible`
   **parada por parada**.
5. **Texto ampliado al 200 %** a 320 px en los **seis estados**, escalando el
   `font-size` calculado elemento a elemento y **afirmando que se ha duplicado**.

`e2e/external-calendar-native-zoom.spec.mjs` (Chromium con extensión efímera y
`chrome.tabs.setZoom`): **zoom nativo al 200 %** a 320, 768 y 1280 px CSS, con
comprobación de que el DPR se duplica de verdad antes de medir.

Los **seis estados de pantalla** son los cinco del Given de @s40 —vacío, con
suscripción, con error, con lista larga de resúmenes Unicode y guardando— más el
diálogo de confirmación abierto, que se añade porque «Sí, eliminar» y «Cancelar» son
controles que la línea 546 del contrato obliga a medir a 44 × 44 px y que sólo
existen dentro de `{confirming ? …}`. «Con suscripción», «con error» y «lista larga»
se siembran por SQL contra el postgres de la pila; «guardando» se congela reteniendo
la respuesta del `PUT` con `page.route`.

### Lo que este documento NO afirma

1. **No afirma cumplimiento a partir de axe.** axe automatiza un subconjunto de
   reglas; no certifica un lector de pantalla real. La revisión manual con lector
   de pantalla **sigue pendiente** y así consta en las filas afectadas.
2. **Sí afirma, desde el 10-09-2026, cobertura de los cinco estados en geometría.**
   El Given de @s40 nombra cinco (vacío, con suscripción, con error, con lista larga
   de resúmenes Unicode y guardando) y los cinco se recorren en los 14 anchos, más el
   diálogo de confirmación. Lo que **no** se afirma es que el estado «con lista larga»
   se haya alcanzado por una sincronización real: con la guardia SSRF activa no hay
   ningún feed iCalendar alcanzable desde el contenedor, así que las filas se siembran
   por SQL. Lo medido es la pantalla, no el camino que la llena; el camino lo cubren
   las pruebas de backend del carril.
3. **Sí afirma zoom nativo del navegador al 200 %, y sólo a 320, 768 y 1280 px CSS.**
   `e2e/external-calendar-native-zoom.spec.mjs` amplía de verdad con
   `chrome.tabs.setZoom`. **2560 px queda fuera y con motivo**: al 200 % cada píxel
   CSS ocupa dos de ventana, así que ver 2560 px CSS exigiría una ventana de 5120 px
   más el cromo, que ninguna pantalla de desarrollo o de CI de este proyecto tiene, y
   el gestor de ventanas recortaría la petición en silencio. 2560 sí se recorre en la
   matriz de anchos, sin zoom.
4. **Sí afirma tema oscuro y colores forzados, y declara vacua la pasada de
   movimiento reducido.** Cuatro modos por seis estados, con axe y geometría en cada
   combinación. Bajo `forced-colors` se omite **sólo** la regla `color-contrast`,
   porque Chromium pinta colores del sistema mientras axe lee los declarados; el
   resto de reglas se conserva. La pasada de `prefers-reduced-motion` **no aporta
   evidencia**: esta pantalla no tiene ni una transición ni una animación que reducir,
   ni con la preferencia puesta ni sin ella, y la prueba lo afirma explícitamente para
   que deje de ser vacua en cuanto alguien añada movimiento aquí.
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
| Von Restorff | El fallo se distingue por `role="alert"`, texto propio y borde, no sólo por color; el aviso de truncado por su texto. | axe sin violaciones en los cuatro modos por los seis estados, `forced-colors: active` incluido; el `role="alert"` se comprueba en las pruebas de error. Bajo colores forzados se pierde el matiz rojo, nunca el texto. | Verificado en navegador (Chromium), claro, oscuro y forced-colors |
| Zeigarnik | Un guardado que falla conserva el borrador y devuelve el foco al campo culpable; el trabajo a medias no se pierde al equivocarse. | @s38 «conserva el borrador y enfoca el campo cuando la dirección se rechaza». | Verificado en jsdom |
| Fluir | No aplica: esta pantalla no gobierna sesiones de trabajo ni duración; su unidad es una suscripción, no un bloque de tiempo. | — | No aplica, con motivo |
| Fragmentación | Tres grupos con nombre: el formulario (`form-card` con `aria-labelledby`), el estado de la suscripción (`dl` de metadatos) y la lista de eventos. | axe sin violaciones (`region`, `landmark-unique`); E2E localiza la región por nombre accesible. | Verificado en navegador (Chromium) |
| Memoria de trabajo | La etiqueta guardada se recarga en el campo al abrir; el borrador sobrevive a 503 y a fallo de red. | @s38 filas de 503 y fallo de red: «se conserva el borrador». | Verificado en jsdom |
| Navaja de Occam | Cinco controles y ninguno de adorno: dos campos, Guardar, Sincronizar ahora, Eliminar suscripción, más los dos del diálogo de confirmación. | El recorrido de teclado de @s40 declara la lista cerrada: cualquier control nuevo dentro del formulario aparece como «intruso» y rompe la prueba. | Verificado en navegador (Chromium) |
| Conectividad uniforme | No aplica: la pantalla no dibuja líneas ni conectores entre entidades. | — | No aplica, con motivo |
| Fitts | Objetivo interno de 44×44 px CSS para todo botón y campo. | E2E: los 14 anchos por los **seis estados** = 84 mediciones, con el diálogo de confirmación abierto, de modo que «Sincronizar ahora», «Eliminar suscripción», «Sí, eliminar» y «Cancelar» entran en la medición. Repetido bajo zoom nativo al 200 % a 320, 768 y 1280 px. | Verificado en navegador (Chromium) |
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

Recuento al 10-09-2026: 30 filas, ninguna omitida. Verificadas con prueba que puede
fallar: **24** (Fitts y Von Restorff pasan de parciales a verificadas al cerrar los
hallazgos 1, 5, 10 y 13). Heurísticas declaradas: 3 (Semejanza, Miller, y la parte
estética de Estética-usabilidad). No aplicables con motivo: 3 (Fluir, Conectividad
uniforme, Parkinson).

## Trabajo pendiente que este documento deja escrito, no tapado

1. ~~Geometría en los cinco estados del Given de @s40~~ — **cerrado el 10-09-2026**:
   14 anchos × 6 estados, con «Sincronizar ahora», «Eliminar suscripción», «Sí,
   eliminar» y «Cancelar» dentro de la medición de 44 × 44 px.
2. ~~Oráculo de recorte y de solapes en los dos ejes y por elemento~~ — **cerrado**:
   `geometry()` mide desbordamiento, escape del viewport, objetivos, recorte por
   elemento en los dos ejes y solapes por pares. Acreditado con una mutación de
   control (`li { overflow: hidden; max-height: 96px }`) que tumbó 3 de 4 pruebas y
   que con el oráculo anterior no habría movido una sola aserción.
3. ~~Zoom nativo al 200 %~~ — **cerrado** en
   `e2e/external-calendar-native-zoom.spec.mjs`, con 2560 px excluido y razonado
   arriba.
4. ~~Tema oscuro y forced-colors~~ — **cerrado**: cuatro modos × seis estados.
   Movimiento reducido se ejecuta y se declara **vacuo** en la propia prueba.
5. **Revisión manual con lector de pantalla**, que el propio comentario del spec
   reconoce obligatoria y que **no consta hecha**. Sigue siendo el hueco principal de
   este documento: nada de lo automático la sustituye.
6. **Firefox y WebKit**, y dispositivos táctiles reales. Todo lo medido es Chromium.
7. **La excepción de `INPUT`, `SELECT` y `TEXTAREA` en el oráculo de recorte**, que
   se documenta en el propio spec: su `scrollWidth > clientWidth` con un valor largo
   es su comportamiento correcto —el usuario recorre el valor con el cursor y no
   pierde texto—, y contarlo daría falso positivo garantizado en una pantalla cuyo
   dato principal es una dirección iCal larga. La excepción vale **sólo** para el
   recorte: esos elementos siguen entrando en objetivos, en solapes y en escape del
   viewport. Queda escrita aquí porque una lista blanca silenciosa es exactamente lo
   que este documento existe para impedir.
