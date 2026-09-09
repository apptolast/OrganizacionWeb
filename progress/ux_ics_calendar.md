# Revisión UX del calendario ICS (feature 26)

Vista revisada: `/calendario` (`frontend/src/calendar.tsx`), corte `e2e/ics-calendar-ux.spec.mjs`
sobre la pila real (API + PostgreSQL + nginx) levantada con `E2E_WEB_PORT=18092`. Contrato:
`features/ics_calendar.feature` @s31–@s38.

Este documento **no infiere cumplimiento a partir de axe** (`AGENTS.md:51`). Cada fila de la matriz
dice qué se midió, con qué oráculo, y dónde termina la evidencia automática y empieza la revisión
heurística.

## Qué se ejecutó

Cinco pruebas, todas verdes en una sola pila, bajada al terminar:

| Prueba | Alcance | Resultado |
| --- | --- | --- |
| `los siete estados … a 320, 768 y 1280 px` | 7 estados × 3 anchos = **21 medidas** | 21/21, axe sin violaciones |
| `los siete estados … con el texto al 200 %` | 7 estados × 3 anchos con la letra calculada duplicada y verificada elemento a elemento | 21/21, axe sin violaciones, capturas por estado |
| `tema claro, tema oscuro, forced-colors y movimiento reducido` | 4 modos × 3 estados = **12 medidas** a 320 px | 12/12 |
| `el recorrido de teclado …` | 3 estados: orden, nombre accesible, foco visible, salida por delante y por detrás, selección del campo | 3/3 |
| `zoom nativo de Chromium al 200 % con 320 px CSS` | `chrome.tabs.setZoom` real sobre contexto persistente con extensión | 1/1 |

Los siete estados son los que nombra @s38: `cargando`, `sin enlace`, `con enlace activo`,
`enlace recién creado`, `confirmación abierta`, `descarga preparada` y `fallo`. `cargando` y
`fallo` se provocan interceptando `GET /api/v1/me/calendar-feed`, que es la única petición que la
vista emite al abrirse.

## Medidas concretas

- **Desbordamiento horizontal**: `documentElement.scrollWidth > clientWidth` es `false` en las 21
  medidas normales y en las 21 con el texto al 200 %.
- **Recorte**: medido en **los dos ejes** (`scrollWidth`/`clientWidth` y `scrollHeight`/
  `clientHeight`) sobre **todos los elementos de `main`** que tengan `overflow` distinto de
  `visible`; ninguno recorta en las 21 medidas normales, las 21 con el texto al 200 % ni bajo zoom
  nativo.

  > **Corrección (segundo dictamen del juez).** La primera versión de este documento afirmaba que
  > con un `textarea` de solo lectura «a 320 px y con el texto doblado la url se lee entera».
  > **Era falso**: el `rows={3}` fijaba la altura y la url se recortaba por abajo (282 px de
  > contenido en 153 visibles, y 108 en 87 incluso con el texto normal). El oráculo de entonces
  > sólo miraba el ancho y por eso no lo veía. El campo es ahora un elemento de solo lectura cuya
  > altura la fija su contenido, sin `overflow` propio, y la afirmación ya se sostiene sobre una
  > medida que cubre los dos ejes.
- **Objetivos de 44 × 44 px**: medidos con `getBoundingClientRect` sobre cada `button`, `a` y
  `textarea` de `main`; ninguno baja de 44 en ninguna combinación. No se delega en la regla
  `target-size` de axe.
- **Controles dentro del viewport**: ninguno tiene `right > clientWidth + 1` en ninguna medida.
  Ancho máximo de control con texto al 200 %: **889,8 px** a 1280.
- **Temas**: claro `rgb(35, 57, 47)` sobre `rgb(248, 249, 245)`; oscuro `rgb(232, 238, 233)` sobre
  `rgb(17, 24, 39)`. Tinta y lienzo se leen del primer ancestro que pinta de verdad, no de un fondo
  transparente.
- **Movimiento reducido**: `prefers-reduced-motion` activo y **todas** las `transition-duration` de
  `main` y sus descendientes ≤ 0,01 s.
- **`forced-colors`**: `matchMedia("(forced-colors: active)")` activo; los tres estados siguen
  operables y el control principal recibe foco.
- **Zoom nativo**: `chrome.tabs.setZoom(tab, 2)` devuelve 2, `devicePixelRatio` se duplica y el
  ancho CSS útil queda en **373 px**; sin desbordamiento, sin recorte de la url, control más ancho
  321,3 px y axe sin violaciones.
- **Teclado**: en los tres estados el orden de tabulación coincide **exactamente** con el orden del
  DOM (`sin enlace` → Crear, Descargar; `enlace recién creado` → campo, Copiar, Regenerar, Revocar,
  Descargar; `confirmación abierta` → campo, Copiar, Confirmar regeneración, Cancelar, Descargar),
  cada parada tiene nombre accesible y foco visible (`outline` o `box-shadow`), se sale de `main`
  tabulando hacia delante y también con `Shift+Tab` desde el primer control: **no hay trampa**.
- **Selección del campo**: con el foco en el campo y `Ctrl/Cmd+A`, `selectionStart` es 0 y
  `selectionEnd` es la longitud completa del valor, que casa con la dirección pública del feed.

Artefactos: `.e2e-work/ics-calendar-ux/{normal,text200,modes}/*-axe.json` (42 informes de axe),
`.e2e-work/ics-calendar-ux/text200/*.png` (21 capturas),
`.e2e-work/ics-calendar-native-zoom/<proyecto>/zoom200.png`, y los JSON de medidas en
`test-results/…/ics-calendar-ux-{normal,text200,modes,keyboard}.json` y
`ics-calendar-native-zoom.json`.

## Límites explícitos de esta revisión

- **No se ha probado con personas.** Todo lo que abajo dice «heurístico» es juicio de diseño, no
  investigación con usuarios ni medición de comprensión.
- **axe no certifica accesibilidad.** Cubre un subconjunto automatizable. Las 42 ejecuciones sin
  violaciones son condición necesaria, no suficiente.
- **`forced-colors`**: los colores los impone el sistema y no son comparables por máquina de forma
  útil (Chromium informa un lienzo transparente). Se desactiva `color-contrast` en ese modo, como
  hace `e2e/appearance-ux-audit.spec.mjs:254`, y el contraste forzado queda como **revisión visual**,
  no medida.
- **Zoom nativo sólo en Chromium.** Firefox y WebKit no exponen una API equivalente; para ellos hay
  texto ampliado y reflow, y **no se afirma** zoom nativo.
- **Un solo motor.** Este corte corre en Chromium. No hay barrido cross-browser propio de la vista,
  a diferencia de `integration_api`.
- **Sin medición de Doherty.** No se ha cronometrado el feedback bajo respuesta retenida; lo que
  hay es el oráculo unitario de que el anuncio aparece antes de la respuesta
  (`calendar.test.tsx`, «@s32 announces the creation while it is in flight»).
- **Lector de pantalla real**: no probado. Los nombres accesibles y las live regions se verifican
  por DOM y por axe, no escuchando NVDA o VoiceOver.
- **Anchos**: 320/768/1280. No se han medido 1440 ni 1920, que `integration_api` sí añadió.

## Matriz de los treinta principios

«Verificado» se limita al comportamiento o medición descritos en la fila.

| Principio | Aplicación y evidencia | Resultado / límite |
| --- | --- | --- |
| Atención selectiva | En `sin enlace` la única acción primaria es «Crear enlace de suscripción»; con enlace, «Regenerar» y «Revocar»; la descarga vive en su propia sección con `h2`. Revisado en las 21 capturas de texto al 200 %. | Verificado visualmente; jerarquía, heurística. |
| Carga cognitiva | La vista pide **cero datos**: crear el enlace es un POST con cuerpo vacío. No hay formulario ni nada que recordar de otra pantalla. | Verificado por recorrido. |
| Estética-usabilidad | Tokens del tema existentes en claro y oscuro; el fallo se explica con texto y ofrece «Reintentar». Éxito y error ejecutados los dos. | Verificado funcional y visualmente; no se infiere facilidad de la estética. |
| Posición en serie | El orden del DOM y el del teclado coinciden exactamente en los tres estados medidos, y no cambia con el ancho. | Verificado por medición. |
| Tendencia a la meta | No hay progreso ni objetivo cuantificado en esta vista. | No aplicable; no se inventa avance. |
| Von Restorff | La acción primaria se distingue por posición y superficie, no sólo por color; la confirmación se separa en un `role="group"` con borde propio. Legible también en `forced-colors`. | Verificado; no depende del color. |
| Zeigarnik | La url se muestra una sola vez y se avisa antes de generarla; si se pierde, el estado se consulta y se regenera de forma deliberada. Salir descarta la url a propósito (@s37). | Verificado unitario y E2E; no persistirla es requisito de privacidad. |
| Fluir | La vista no inicia, pausa ni cierra ninguna sesión de trabajo. | No aplicable. |
| Fragmentación | Tres regiones: ayuda, gestión del enlace y descarga, cada una con su encabezado. | Verificado estructuralmente. |
| Memoria de trabajo | No hay borrador que perder: no hay campos de entrada. Tras recargar, la fecha de creación sigue ahí; la url no, y se explica por qué. | Verificado (`@s32`, estado `con enlace activo`). |
| Navaja de Occam | Seis controles como mucho, cada uno con un propósito: crear, copiar, regenerar, revocar, confirmar/cancelar y descargar. | Revisión heurística del recorrido. |
| Conectividad uniforme | No hay líneas ni conexiones decorativas. | No aplicable. |
| Fitts | Todos los objetivos ≥ 44 × 44 px CSS medidos con `getBoundingClientRect` en las 21 medidas normales, las 21 con texto al 200 % y bajo zoom nativo. | Verificado geométricamente. |
| Hick | Una decisión principal por estado; regenerar y revocar sólo aparecen cuando hay enlace. Sin menús anidados. | Revisión heurística; no se miden tiempos de decisión. |
| Jakob | Botones, campo de solo lectura, enlace nativo con `download` y live regions convencionales. | Verificado por teclado y por axe. |
| Semejanza | Reutiliza `RouteLink`, los tokens de estilo y el patrón de foco al `h1` del resto del producto. | Verificado visualmente en ambos temas. |
| Miller | Agrupación por significado (ayuda / enlace / descarga); no se impone un recuento de opciones. | Revisión heurística. |
| Parkinson | No hay duración ni extensión: el token no caduca por sí solo y la ventana publicada es fija. | No aplicable; el límite es explícito en la ayuda. |
| Postel | El cliente acepta `http` y `https` y tolera el OWS de `Content-Type`, pero exige la forma exacta de la dirección y el documento completo antes de ofrecer descarga. | Verificado unitario; sin debilitar validación. |
| Proximidad | `label` asociada al campo por `htmlFor`; el aviso de secreto va dentro de la misma sección; el fallo, junto a «Reintentar». | Verificado por DOM y por axe. |
| Prägnanz | Todos los estados se anuncian con **texto** («Cargando…», «Creando enlace…», «Enlace copiado», «Archivo preparado»), nunca con un icono solo. | Verificado. |
| Región común | El enlace recién creado y la confirmación viven en contenedores con borde y fondo propios. | Verificado visualmente en los cuatro modos. |
| Tesler | La fecha de creación se muestra legible con `Intl`; los fallos no exponen el error interno ni el token. | Verificado unitario y E2E. |
| Modelo mental | El feed publica **bloques planificados**, no sesiones ni tareas completadas; la ayuda lo dice y el documento no incluye VTODO. | Verificado por contenido del `.ics`. |
| Usuario activo | El estado vacío explica qué contiene el feed, su ventana, que la url es secreta y que no sincroniza de vuelta, antes de ofrecer crearla. | Recorrido verificado; primera experiencia no medida con personas. |
| Pareto | Suscribirse y descargar están en la misma pantalla, sin quitar regenerar ni revocar. | Priorización de diseño, no uso observado. |
| Fin de pico | Confirmación cierta al crear y al revocar; los fallos no dan falso éxito ni afirman que el enlace anterior dejó de funcionar. | Verificado (`@s35`, fila del 503). |
| Sesgo cognitivo | No hay métricas, ni rachas, ni presión: sólo la fecha de creación. | Verificado por contenido. |
| Sobrecarga de opciones | Sin ajustes: la ventana y el límite de eventos son fijos y están explicados. | Verificado estructuralmente. |
| Doherty | «Cargando…» y «Creando enlace…» se anuncian con `role="status"` mientras la petición está en vuelo, sin porcentaje inventado ni url anticipada. | Verificado unitario y en el estado `cargando` del E2E; **latencia no cronometrada**. |

## Estado de las puertas

Cubierto por esta revisión: @s38 completo (siete estados, tres anchos, texto al 200 %, zoom nativo
al 200 %, cuatro modos de presentación, recorrido de teclado y selección del campo). Pendientes y
fuera de este documento: la mutación (PIT `ics_calendar` y Stryker `ics_calendar-frontend`) y
`bin/harness init`, que es la suite completa y está prohibida por la disciplina de recursos de esta
sesión.
