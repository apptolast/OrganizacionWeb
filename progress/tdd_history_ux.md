# TDD UX Historial18

Primer caso individual en history-ux.spec.mjs; corte241a2f0 UI final/PG nominal, sin delta final PG. Plan:31anchos, cinco estados y teclado real; ejecución aún parcial por RED temprano.

## RED inicial real

Sesión77683, EXIT1 5d013a. A320, details, nav Hoy mide41,359px de ancho frente a44 requerido. JSON también observa Categoría19px alto, fechas27px, enlaces cortos/summary21px. La página no desborda (scroll320). No se declaran solapes de cajas inline como colisión efectiva: requieren remedir después del ajuste de áreas. Tab alcanzó summary, el foco fue visible y Enter abrió details. No se alcanzaron las otras30anchuras, axe ni estados posteriores; no se acredita la matriz30 por este fallo.

Evidencia copiada sin mover originales a history_ux_initial_artifacts (geometry, captura y error-context); log history_ux_initial.log con RUN_EXIT=1. Before/after323 entradas:322 iguales y PostgresHistoryQueries.java distinto al preservar after (94411f). Se notificó root para atribuir su copia coordinada; no se declara producción idéntica entre esos manifiestos. El fallo medido es CSS de frontend. Se informó root/B antes de cualquier CSS; C no cambia producción. Stack59880 retirado por lifecycle,18080 y Gradle libres. Propuesta mínima para B: áreas44 de controles/links/summary de .history y ancho mínimo del enlace Hoy, verificando reflow de navegación completa.

## Corrección mínima y ejecución intermedia

Root confirmó que el cambio PG del primer after ocurrió después del lifecycle, durante preservación: commits b639336/f03ce2f. Se conserva la comparación322/1; no hubo cambio de frontend durante ese RED.

history.scss incorpora áreas44 en filtros/links/summary y rejilla de etiquetas. Intento939b05 EXIT1 conserva el mismo RED Hoy: sustitución textual inicial no modificó styles.scss por diferencia de finales de línea (comprobación lo detectó durante run; no se editó entonces). Log geometry y copia history_ux_nav_omission_geometry.json preservados. Después se aplicó el único min-width44 de .sidebar nav a con verificación literal antes de ejecutar de nuevo. Ninguna fuente TS/TSX modificada.

## Geometría corregida; foco Limpiar pendiente del ajuste de B

Run7009 EXIT1 beffd9 supera124medidas (4estados ×31), cuatro axe sin violaciones y feedback medido; se detiene en foco de Limpiar. El oráculo exigía enlace enfocado, pero el formulario keyroute desmonta al iniciador: corresponde h1 según contrato. Se corrige sólo esa expectativa antes del siguiente run. Root detectó además ausencia de captura del iniciador en producción y B prepara su corrección puntual; no se atribuye GREEN global ni la quinta matriz aún. Capturas/JSON copiados en history_ux_geometry_fixed_artifacts sin mover originales. Runner22884 retirado, puerto libre para copia del bundle de foco.

## GREEN del primer UX sobre fuente final

Corte4d42253 incorpora foco de B y PG final. Mismo caso con oráculo h1 corregido GREENd934ae, EXIT0 propio, 1/1 (13,8s; Playwright16s).155medidas: cinco estados ×31anchos, incluida altura400 a768;5axe sin violaciones. Tab/Enter reales alcanzan details, quitar contexto, aplicar, reintentar y limpiar; el foco de h1 tras reintentar/limpiar desaparecidos se comprueba en navegador. No se atribuye todavía navegación física de paginación ni respuesta obsoleta a este primer caso.

Copias de capturas/JSON en history_ux_verified_artifacts; originales conservados. Before/after323inputs idénticos. Los dos SCSS y este primer test quedan congelados para integración root; texto200/zoom/motores son ciclos posteriores, todavía no acreditados. No se certifican los30principios por axe ni dispositivos/lectores físicos. Runner16288 retirado por lifecycle.

Segundo caso de teclado: ed1bf8 EXIT1 por tolerancia impropia del oráculo (bottom700,0625 frente viewport700, diferencia subpíxel). Se admite1px de redondeo, como geometría horizontal, sin relajar44px ni ocultación real. No defecto productivo demostrado; contexto inicial preservado. El after de este intento incluye la modificación explícita del test al corregir tolerancia, no se presenta como freeze idéntico.

Segundo caso GREEN con tolerancia subpíxel: RUN_EXIT0 en history_ux_keyboard_verified.log. Tab/Enter activan Más antiguos, enlace de fila a historial de tarea, quitar contexto, recientes, aplicar y limpiar durante espera. La petición antigua retenida recibe401 después de mostrar nueva página y no revoca la sesión ni sustituye20hechos. Es una inyección de respuesta HTTP obsoleta con navegación real; no promete emular JSON decodificado de forma diferida. Focus JSON preservado, capturas propias y before/after.

Texto200 inicialmente GREENac77c3, EXIT0,1/1 (4,6s),9medidas/3axe0 en notas largas/error/vacío a320/768/1440 (altura400). Escalado exacto por elemento before/after×2 guardado, notas reales conservadas como texto. No es zoom nativo. Stack55444 retirado; fuente estable durante ejecución.

Zoom nativo200 inicialmente GREEN22cea4 EXIT0,1/1 (6,1s),3medidas/3axe0. Extensión local con chrome.tabs.setZoom/getZoom=2, DPR duplicado y innerWidth320 CSS reales; evidencia zoom.json y notas/error/vacío preservada en history_ux_chromium_artifacts/nativeZoom200. Browser persistente cerrado en finally, stack29760 retirado. No se sustituye zoom por emulación de viewport.

Motores instalados verificados963d36 mediante @playwright/test: Chromium1243,Firefox1543,WebKit2359. La primera consulta importó paquete no expuesto playwright y falló antes de abrir navegador; corregida al paquete instalado, sin instalación ni cambios de dependencias. Chromium UX/notas/teclado copiados antes de los siguientes runners, sin mover originales.

Firefox inicial13f249: texto200 GREEN, dos recorridos teclado fallaron. Se instrumentó espera de frame y scroll visible para diferenciar scroll asíncrono de ocultación; no se toleran26px fuera de pantalla. Intento a169a1 no ejecutó tests: el runner Windows interpretó pipe del regex grep-invert como shell. Se conserva log; siguiente comando usa grep keyboard sin metacaracteres, exactamente los dos casos pendientes. No se atribuye falso RED de producto a ese intento.

Firefox diagnóstico b18453 conserva dos fallos. Registro history_ux_firefox_unreached.json muestra120Tabs sin cambiar SUMMARY tras axe; implementación instalada @axe-core/playwright/dist/index.js abre blankPage348 y la cierra369. El test ahora restaura página activa con bringToFront después del análisis, sin aplicar focus a controles ni sustituir teclado real. El otro fallo mantiene Quitar contexto parcialmente bajo viewport durante5s (726 frente700), por lo que root autorizó experimentar scroll-margin-block16px sólo en .history a/summary. Se conserva únicamente si el mismo oráculo acredita corrección; no se afloja la tolerancia.

Firefox cerrado por evidencia compuesta: texto20013f249 pasó; teclado/paginación8f36a2 pasó después de scroll-margin16; matriz primer caso a4f1b0 EXIT0 (15s) pasó al recorrer Shift+Tab hacia destino anterior, sin abandonar contenido por el último control.155+9medidas/8axe0; foco real sin focus() artificial. No se atribuye a bringToFront una corrección que por sí sola no resolvió el fallo. Scroll-margin sí corrigió el control parcialmente oculto del segundo caso, conserva mismo oráculo. Artefactos finales Firefox copiados; resultado no es una ejecución monolítica3/3.

Zoom recapturado por CDP:019105 EXIT0,1/1, mismos oráculos de zoom real y3estados. El motivo de repetición fue captura fullPage recortada por Playwright al200%, no fallo geométrico. Copia final history_ux_native_final_artifacts incluye viewport y fullclip explícito; inspección de empty320viewport muestra controles completos y texto legible dentro del ancho. Capturas iniciales conservadas, no se reinterpretan.

WebKit e5513a: texto200 pasó; Tab omite anchors. Intento42e75c con Alt+Tab tampoco alcanza enlaces en el port Windows instalado; no se atribuye comportamiento Safari/Mac a este motor. Referencia primaria consultada: [Apple, atajos Safari](https://help.apple.com/safari/mac/8.0/en.lproj/cpsh003.html), Option-Tab para elementos clicables; la medición local prevalece y evidencia que esa adaptación no resuelve este entorno. Propuesto a root mantener límite de teclado y ejecutar geometría/axe con clicks explícitos sólo WebKit, pendiente de decisión antes de cambiar recorrido. No modificar tabindex ni producto por esta limitación del motor.

Root autorizó completar WebKit con activación por clic y límite explícito de teclado. Se retira AltTab ineficaz; el primer caso anota limitation y sólo desactiva los oráculos de foco de teclado en ese motor, conserva geometría/axe/feedback/estados/reintento. Chromium/Firefox siguen Tab/Shift+Tab/Enter y foco. El caso separado de paginación por teclado mantiene sus oráculos: su RED WebKit no se reclasifica como GREEN.

## Cierre de modalidades y límites

WebKit geometría por clic GREENdc8ab8 EXIT0 (12,4s),155medidas/5axe0, feedback130ms. Texto200 WebKit original e5513a pasó9medidas/3axe0. Su teclado de enlaces permanece NO acreditado; los RED originales se conservan y no se reclasifican. Chromium y Firefox sí tienen navegación real Tab/Shift+Tab/Enter, paginación/contexto y401obsoleto comprobados.

Totales compuestos sin duplicar intentos: Chromium167medidas/11axe (155base+9texto+3zoom), Firefox164/8, WebKit164/8:495medidas y27axe sin violaciones. No es una ejecución global monolítica. Feedback de matriz final8/18/130ms según motor. Matriz30 en ux_history.md distingue evidencia y límites físicos/humanos.

Último before/after WebKit323inputs idénticos155976. El único cambio productivo desde primerfreeze es scroll-margin-block16px en history.scss, demostrado para foco parcial Firefox; ninguna fuente TS/TSX/backend modificada por C. Cambios del test entre ciclos son explícitos: tolerancia subpíxel, selección de dirección Tab, restauración página trasaxe, captura CDP y modalidad clic WebKit. El global root repetirá Chromium sobre paquete integrado; no se ejecutó otro global aquí.

Todos los stacks propios retirados;18080 y Gradle libres. Paquete final congela test, SCSS, bitácora/matriz y manifiestos. Logs/capturas permanecen locales, no se incluyen secretos, perfiles de navegador ni árboles snapshot en el commit.
