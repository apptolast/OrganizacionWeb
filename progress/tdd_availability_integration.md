# Integración de disponibilidad

## Preparación, aún sin ejecución de navegador

El cliente API quedó congelado con 147 pruebas focales verdes y aprobación independiente del coordinador en `review_availability_api.md`.

Se prepararon cuatro recorridos en `e2e/availability.spec.mjs`: ausencia y guardado con persistencia tras recarga; concurrencia real de primera creación y actualización con recuperación deliberada del conflicto; rechazo de confirmación contradictoria conservando el borrador; y siete presupuestos con validación numérica, teclado, feedback medido y matriz de 22 anchos. Se reutilizan sesiones reales del fixture y el CSRF del cliente; el navegador no recibe encabezados CSRF globales.

`node --check e2e/availability.spec.mjs` terminó con código 0. Esto comprueba sintaxis, no funcionamiento. No se ha construido ninguna imagen ni ejecutado estos recorridos: se espera la congelación conjunta autorizada. Las etiquetas siguen el acuerdo con la autora de interfaz.

Pendientes de ejecución: recorridos sobre PostgreSQL real, comprobación adicional de privacidad del catálogo, persistencia tras reiniciar el backend, zoom nativo y revisión visual. No se atribuye todavía ningún resultado UX a esta función. Disponibilidad no genera eventos: se verifica que proyectos y outbox permanezcan intactos, sin repetir el conjunto de fallos del publicador que no cambia.

Revisión del coordinador antes de ejecutar: se corrigieron tags heredados incorrectamente. Concurrencia enlaza s19/s20/s33; UX enlaza s31/s38/s39/s42/s43. El caso s38 ahora pulsa Guardar con `1e` todavía presente, comprueba cero PUT, `aria-invalid`, foco real y contorno visible antes de sustituir el valor. No constituye un resultado de navegador todavía.

## Primera ejecución sobre el corte conjunto

El coordinador autorizó integración tras init 8318: 984 pruebas backend y 841 frontend, más lint. Se construyó la imagen aislada organizationweb-e2e-62876; web inicial eDZZ9Djc y CSS BgJhgGHj. La suite de 48 recorridos sigue en curso al registrar esta nota.

Los cinco recorridos nuevos de disponibilidad ya pasaron: guardado/no-op/recarga y reinicio real del backend conservando sesión y snapshot, dos carreras 200/412, recuperación de confirmación contradictoria, entrada 1e y formulario con 28 anchos/axe/teclado, y catálogo con sesión expirada que retira el borrador. Este resultado no aprueba todavía toda la suite ni toda la UX.

Una inspección adicional de navegación detectó un defecto que la medición del main no cubría: entre 701 y 760 px, nav tenía clientWidth 169 y scrollWidth 232; los textos rebasaban sus enlaces de 80,5 px y se solapaban con el contenido. A 761 px los valores volvían a 169/169. La captura availability-navigation-720.png muestra el solape; no es sólo un error de medición. Se comunicó al autor y coordinador antes de capturas finales. La corrección pendiente se limita a SCSS, sin reconstruir backend ni repetir su reinicio.

## Cierre de la suite original y verificación SCSS separada

La sesión 71715 terminó con **48/48 verdes en 3,7 minutos**, EXIT 0 y limpieza completa de su fixture. No incluye la nueva aserción de navegación añadida después.

Sesión 77936: el nuevo caso focal reprodujo RED sobre CSS BgJhgGHj (`scrollWidth 232 > clientWidth 169`). Se reconstruyó únicamente web tras la corrección SCSS; bundle DRuvzMnj, CSS Codz1mIb. Antes del GREEN, el helper de trabajo falló porque `pnpm.cmd` interpretó la alternancia del grep como un pipe de shell. Fue un fallo del helper, no del producto. Se cambió la invocación temporal a Node directo con el CLI de Playwright; no se modificó el runner de producción ni se repitió la construcción.

Sesión 50590, **EXIT 0**: matriz ampliada y navegación **2/2 en 26,9 segundos** sobre el nuevo CSS. Se revisan tanto las cajas del formulario como el texto dentro de cada enlace de navegación y la ausencia de intersección de enlaces. El caso focal añade 720 y 760 a la comprobación específica, además de la matriz de 28 anchos. Firefox y WebKit ejecutaron el recorrido de guardado/no-op/recarga/cancelación **2/2 en 22,4 segundos**. Ese recorrido conserva su comprobación de reinicio también en ambos motores; no se repitieron las carreras ni la matriz completa por motor.

El helper de zoom usó `chrome.tabs.setZoom(2)` en un perfil Chromium propio: ancho interior 1426→713 y DPR 1,5→3. La ventana de 654 produjo innerWidth 320 y documento 312/312, sin overflow. Se guardó realmente un presupuesto de lunes 135 desde la UI a ese zoom y se confirmó mediante GET. Los diez controles/enlaces medidos cumplen al menos 44 × 44 CSS.

Capturas finales en outputs: availability-desktop.png, availability-mobile.png, availability-real-zoom.png, availability-real-zoom-320.png y availability-real-zoom.json. Las capturas de navegación a 701, 720, 759, 760 y 761 se actualizaron con CSS corregido: nav 169/169 y texto dentro de enlaces. La inspección visual de escritorio, móvil, zoom a 320 y navegación a 720 confirma legibilidad y ausencia del solape anterior. Son valores sintéticos, no preferencias del usuario.

Los contenedores, volúmenes y perfil temporal propios se limpiaron al finalizar. No se tocaron los dos archivos heredados bloqueados ni su directorio padre. No se agregan 48+2 como una suite única de 50: corresponden a cortes y propósitos diferentes. La corrección lógica de recuperación 400 que coordina el autor permanece fuera de esta evidencia hasta su liberación.

## Recuperación de HTTP 400 sin mensaje útil — corte final

El coordinador aprobó el único cambio lógico posterior: exigir contenido tras `entry.message.trim()`. Sesión 27100: reconstrucción **sólo web**, bundle final **CIX_-ttO**, CSS **Codz1mIb** sin cambios. No se reconstruyó backend ni se repitió su reinicio o la matriz visual.

Nuevo recorrido `empty server validation messages retain drafts and require explicit recovery without form navigation`: **1/1 verde en 3,5 segundos**, EXIT 0; internamente comprueba por separado mensaje vacío y mensaje de tres espacios. Para cada caso, el backend real responde 400 por un presupuesto fuera de rango en la petición interceptada; el fixture modifica únicamente el mensaje de esa respuesta. La UI muestra una alerta útil, conserva borrador y preferencias, no anuncia éxito ni repite PUT, exige recarga deliberada y permite luego un guardado válido. Se cuentan exactamente dos escrituras por caso: rechazo inicial y nuevo envío explícito tras recuperar. La navegación del frame principal permanece en cero y la URL no cambia al enviar, reforzando el comportamiento preventDefault en navegador real.

Todos los contenedores y volúmenes de organizationweb-e2e-48504 se retiraron en finally. No se tocaron temporales bloqueados. Las capturas previas siguen acreditando el estilo idéntico Codz1mIb; este resultado focal acredita el nuevo comportamiento de error. No se presenta una suite conjunta ficticia sumando los cortes anteriores.

## Hallazgo de foco al enviar desde presupuesto

Comprobación acotada solicitada por el autor y coordinador, sesión 96423, EXIT 0; se usó el bundle final CIX_-ttO ya construido. No se modificó producción ni se reconstruyó imagen. Pasos: abrir /disponibilidad, seleccionar UTC, escribir 77 en Lunes y pulsar Enter real desde ese input, reteniendo PUT antes de completar. No se dispararon eventos sintéticos ni focus() programático.

Se observaron tres resultados en Chromium:

- Éxito real del backend: activeElement era INPUT#minutes-MONDAY antes, BODY mientras estaba pendiente y BODY tras Disponibilidad guardada.
- Error 503 inyectado: la muestra durante el cambio aún veía INPUT deshabilitado; tras aparecer recuperación, activeElement era BODY. El formulario había perdido su destino de foco visible.
- Error 400 con mensaje válido asociado al lunes: la muestra pendiente veía BODY y el foco final regresó a INPUT#minutes-MONDAY, con aria-invalid.

Evidencia cruda en evidence_availability_enter_focus.json. Los textos de BODY son una muestra DOM, no una afirmación de visibilidad de cada texto; el script verificó por separado las confirmaciones/errores esperados. El resultado revela una diferencia de comportamiento real entre envío desde input y desde botón. Se comunicó al autor para TDD de restauración de foco sin robar el foco que la persona haya movido voluntariamente. Fixture organizationweb-e2e-58584 limpiado. La corrección y replay quedan pendientes del nuevo freeze.

## GREEN de foco desde presupuesto — bundle CpU8JHCd

El coordinador revisó y congeló la corrección que conserva input/select además de botón como destino previo. Sesión 5043, **EXIT 0**: reconstrucción sólo web, bundle **CpU8JHCd**, CSS **Codz1mIb** intacto. La regresión permanente de Enter y la de mensajes 400 vacíos/espacios pasaron **2/2 en 4,2 segundos**.

El caso de Enter recorre éxito real, 503 inyectado, 400 real de campo y éxito mientras la persona mueve el foco mediante Tab hasta Cancelar durante PUT. En los tres primeros vuelve a INPUT#minutes-MONDAY; en el cuarto conserva el enlace externo elegido. Se verifican también los resultados visibles, sin focus() ni eventos sintéticos. La regresión 400 comprueba ambos mensajes y recuperación completa en el mismo corte.

La evidencia RED `evidence_availability_enter_focus.json` se conserva sin reemplazarla. Este GREEN pertenece al test permanente y a la salida de sesión 5043; no se reetiqueta el JSON original. No se repitieron matriz global, zoom, reinicio de backend ni mutación. Fixture organizationweb-e2e-20884 y sus volúmenes propios limpiados. No quedan hallazgos de integración abiertos comunicados por este agente; el coordinador conserva las puertas globales de cierre y publicación.

## Foco nativo: Firefox y WebKit

Verificación adicional solicitada por el coordinador sobre **CpU8JHCd** ya construido, sin reconstrucción, reinicio ni matriz. La regresión Enter pasó finalmente **Firefox 1/1 (2,6 segundos)** y **WebKit 1/1 (2,7 segundos)**; sesión 84087, **2/2 en 7,5 segundos, EXIT 0**. Cada motor recorre éxito, 503, 400 y conservación de foco externo.

Se ajustó únicamente el recorrido de teclado del destino externo: Shift+Tab hacia el botón Cerrar sesión anterior al formulario, sin activarlo. La prueba exige que ese botón reciba foco antes de liberar PUT y que lo conserve después. No usa focus(), click ni eventos sintéticos para conseguir ese resultado. El mismo test mantiene la restauración al presupuesto en los otros tres resultados. Al limpiar rutas, `unrouteAll({behavior:'wait'})` espera a los handlers liberados para evitar errores secundarios en un caso fallido.

Intentos previos separados: la primera selección temporal no encontró tests por combinar el grep histórico con el nuevo; se corrigió usando una configuración temporal focal. Después Firefox pasó con el enlace Cancelar, pero WebKit no lo alcanzó con Tab hacia delante antes de liberar PUT. Al elegir el botón Cerrar sesión, ambos motores tampoco lo alcanzaron con Tab hacia delante. No se atribuyeron esos fallos de preparación del foco a la restauración posterior ni se cambiaron fuentes para ocultarlos. El recorrido final retrocede al control anterior sin depender de ciclar por el navegador; ambos motores alcanzan y conservan el destino.

Todos los fixtures de estos intentos se retiraron con sus volúmenes propios, incluido organizationweb-e2e-66428. Esta evidencia acredita sólo la regresión de foco, no una matriz UX completa en Firefox/WebKit. No se modificó producción ni la evidencia RED original.
