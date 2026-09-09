# UX del conector de GitHub (feature 27)

Pantalla revisada: `/integraciones/github` (`frontend/src/github-connector.tsx`) y el índice
`/integraciones` (`integrations-index.tsx`). Contrato: @s36–@s42 de
`features/github_connector.feature`.

## Qué se ha medido y con qué

- **E2E ejecutado**, puerto 18094, pila de compose con PostgreSQL real y **servicio falso de GitHub**
  (`e2e/fake-github/`, perfil `e2e`). Los estados se alcanzan **por la interfaz**, no fabricando
  filas: se conecta, se importa, se reimporta, se desconecta.
  - `e2e/github-connector.spec.mjs` — **15 pruebas, 15 en verde**.
  - `e2e/github-connector-native-zoom.spec.mjs` — **1 prueba, en verde**, con Chromium real y
    `chrome.tabs.setZoom` al 200 %.
- **axe-core** (`wcag2a`, `wcag2aa`, `wcag21aa`, `wcag22aa`, `best-practice`) en **los siete estados**
  que nombra @s42: deshabilitado, sin conexión, conectada, importando, resultado, error recuperable
  e inválida. **Cero violaciones.**
- **Zoom nativo al 200 %** en 320, 768 y 1440 px CSS. Evidencia en
  `.e2e-work/github-connector-zoom/<pila>/evidence.json` y tres capturas `zoom200-<ancho>.png`:

  | Ancho CSS | devicePixelRatio | scrollWidth | clientWidth | Controles medidos | Violaciones axe |
  | --- | --- | --- | --- | --- | --- |
  | 320 | 3 | 312 | 312 | 5 | 0 |
  | 768 | 3 | 760 | 760 | 5 | 0 |
  | 1440 | 3 | 1432 | 1432 | 5 | 0 |

  El `devicePixelRatio` triplicado respecto al 1 del arranque (1,5 × 2) confirma que el zoom se
  aplicó de verdad; `scrollWidth == clientWidth` es la ausencia de desplazamiento horizontal.
- **Unitarias** (Vitest): 28 en `github-connector.test.tsx`, 5 en `github-connector-routing.test.tsx`
  y 23 en `github-connector-client.test.ts`.

## Límites explícitos de esta revisión

Se dicen antes que los resultados, para que nadie lea la matriz como más de lo que es:

1. **No hay evaluación con personas.** Ninguna fila de abajo acredita facilidad de uso real. Donde
   pone «heurístico» es **juicio de quien implementa**, no observación de nadie usando esto.
2. **axe no demuestra accesibilidad.** Cubre una parte comprobable por máquina. Cero violaciones no
   es lo mismo que «accesible», y `AGENTS.md:51` prohíbe expresamente esa inferencia.
3. **Un solo motor.** Chromium. No se ha probado Firefox ni WebKit en esta pantalla.
4. **Sin lector de pantalla real** (NVDA, JAWS, VoiceOver), sin teclado virtual y sin dispositivo
   físico. Lo que se afirma del anuncio por `aria-live` es que el atributo y el texto están, no que
   un lector concreto lo pronuncie como se espera.
5. **Sin `forced-colors`, sin `prefers-reduced-motion` y sin texto al 200 %** por separado del zoom.
   El zoom nativo al 200 % sí se midió; el ajuste de sólo texto no.
6. **El servicio falso no es GitHub.** Reproduce los códigos y las formas que el contrato nombra;
   no reproduce la latencia, ni la paginación real, ni los rarezas del servicio de verdad.
7. **Doherty no se ha medido con cronómetro.** No hay una medición de «feedback < 400 ms» en esta
   pantalla; lo que hay es que el estado «importando» aparece en el mismo ciclo del clic.

## Matriz de los 30 principios de `docs/ux-requirements.md`

Ninguna fila se omite. La columna «Cómo se sabe» distingue **medido** (una prueba lo comprueba y
falla si deja de cumplirse) de **heurístico** (juicio sin oráculo).

| Principio | Aplicación en esta pantalla | Cómo se sabe |
| --- | --- | --- |
| Atención selectiva | Un estado visible a la vez; la acción principal de cada estado es única («Conectar», «Importar issues abiertas», «Confirmar desconexión»). | **Medido**: @s36 en `github-connector.test.tsx` comprueba los seis estados y que lo que no toca **no** está |
| Carga cognitiva | El formulario pide dos datos, repositorio y token, y nada más. La ayuda del permiso necesario está junto al campo. | **Medido**: @s36 exige el texto de ayuda sobre lectura de issues; la ausencia de más campos es estructural |
| Estética-usabilidad | Sólo tokens del tema (`--ink`, `--editable`, `--control-border`, `--muted`); ningún color fijo, así que modo claro y oscuro se comportan igual. | **Medido**: guardas de `theme-tokens.test.ts` sobre todas las hojas SCSS; axe de contraste en los siete estados |
| Posición en serie | El orden de lectura y el de tabulación coinciden: datos de conexión, selector, importar, desconectar. | **Medido**: la prueba de Tab compara el recorrido con el orden del DOM, elemento a elemento |
| Tendencia a la meta | El resumen da los tres contadores reales del recibo y avisa cuando el repositorio tenía más issues que las 200 traídas. | **Medido**: @s38 y el E2E de importación real (created 1, skipped 0, failed 1 con el falso `con-fallo`) |
| Von Restorff | El error se distingue por posición, texto y `role`, no sólo por color. | **Medido** (que no depende del color): axe sin violaciones de contraste. **Heurístico**: que destaque lo justo |
| Zeigarnik | Una importación a medias deja recibo `running` y la pantalla ofrece «Consultar estado» en vez de fingir que terminó. | **Medido**: @s36 fila `running` y @s39 fila `IMPORT_IN_PROGRESS` |
| Fluir | Importar no bloquea la pantalla: el botón se inhabilita, se anuncia el progreso y al terminar el foco va al encabezado. | **Medido**: @s38 y el E2E, que comprueba el foco en el h1 tras el resultado |
| Fragmentación | Conexión, destino de la importación y resultado son tres bloques con su propio encabezado o `aria-label`. | **Medido**: las regiones `Conexión` y `Resultado de la importación` se consultan por rol en las pruebas |
| Memoria de trabajo | Tras un token rechazado el repositorio se conserva y sólo se vacía el token. | **Medido**: @s37, en unitaria y en E2E contra el repositorio falso `rechazado` |
| Navaja de Occam | Cada control existe para un paso del recorrido; no hay ajustes decorativos. | **Heurístico**, apoyado en que las pruebas de estado fallan si aparece un control que no toca |
| Conectividad uniforme | No se dibujan líneas ni conexiones: la relación tarea↔issue se cuenta con palabras y con el enlace al proyecto. | **Heurístico** (no aplica material gráfico) |
| Fitts | Botones, selector, campos y **enlaces** miden al menos 44 × 44 px. | **Medido**: en los siete estados y también a 320/768/1440 px con zoom nativo al 200 %. Fue esta comprobación la que encontró que los enlaces medían 21 px |
| Hick | Una decisión principal por estado; el selector sólo ofrece proyectos no terminados. | **Medido**: @s38 comprueba que el terminado no aparece |
| Jakob | Formulario, `select` nativo y botones estándar; el token es `type="password"`. | **Medido**: @s37 fija `type` y `autocomplete` |
| Semejanza | Los contadores se presentan igual tras una importación correcta y tras una interrumpida. | **Medido**: @s39 fila `STORAGE_UNAVAILABLE` reutiliza el mismo resumen |
| Miller | Tres datos de conexión y tres contadores; no se imponen listas largas. | **Heurístico** |
| Parkinson | Importar termina solo, sin ampliarse: como mucho dos páginas de cien y `truncated` lo dice. | **Medido** en backend (@s16, seis filas) y visible en la interfaz mediante el aviso de truncado |
| Postel | El repositorio se recorta de espacios Unicode y se valida antes de salir a la red; los límites se explican. | **Medido**: `GithubRepositoryTest` (12) y @s5 en la frontera HTTP |
| Proximidad | Etiqueta, ayuda y error del token están junto al campo y asociados por `aria-describedby`. | **Medido**: @s37 lee el `aria-describedby` y comprueba que contiene el mensaje |
| Prägnanz | Los estados se nombran con palabras («Conectada», «Conexión inválida»), no con iconos. | **Medido**: @s36 los localiza por texto |
| Región común | `Conexión`, `Confirmar desconexión` y `Resultado de la importación` son regiones con nombre accesible. | **Medido**: las pruebas las obtienen por rol y nombre |
| Tesler | La complejidad de paginar, descartar pull requests, cifrar y no duplicar vive en el servidor; a la persona se le piden dos datos. | **Medido** en backend (@s12–@s19); **heurístico** en cuanto a que el reparto sea el adecuado |
| Modelo mental | Se distingue la conexión (credencial) de las tareas importadas (trabajo propio): desconectar borra la primera y conserva las segundas, y el diálogo lo dice. | **Medido**: el E2E de desconexión comprueba que quedan 0 conexiones y 3 enlaces |
| Usuario activo | Sin conexión, la pantalla explica qué permiso hace falta antes de pedir nada. | **Medido**: @s36 exige la ayuda; **heurístico** que baste para no leer un manual |
| Pareto | Lo frecuente —importar al proyecto habitual— está a un clic, con el primer proyecto ya seleccionado. | **Medido**: el selector arranca con un valor; **heurístico** que sea el que la persona quiere |
| Fin de pico | El recorrido termina con contadores y un enlace al proyecto destino; un fallo del almacén también muestra lo que sí se hizo. | **Medido**: @s38 y @s39 fila `STORAGE_UNAVAILABLE` |
| Sesgo cognitivo | Los contadores son hechos del recibo, no estimaciones; «Fallidas» se muestra aunque sea 0. | **Medido**: el E2E compara los tres contadores contra lo que el falso sirvió |
| Sobrecarga de opciones | No hay preferencias que configurar: la base de la API es del servidor y no del usuario. | **Medido**: @s35, `apiBase` en el cuerpo ⇒ `UNKNOWN_FIELD` |
| Doherty | Al pulsar importar, el botón se inhabilita y `aria-live` anuncia «Importando issues…» **sin porcentaje**, porque no hay progreso real que contar. | **Medido** que el aviso aparece y que no contiene «%». **No medido**: el umbral de 400 ms |

## Defectos que esta revisión encontró y se corrigieron

1. **Los enlaces medían 21 px de alto**, por debajo del mínimo de 44. La primera versión del E2E los
   excluía invocando la excepción *Inline* de WCAG 2.2 §2.5.8, que **no** aplica: los dos enlaces son
   el único contenido de su bloque, no van dentro de una frase. Corregido en `styles.scss` y ahora se
   miden **todos** los controles, enlaces y campos incluidos.
2. **`Escape` no cerraba la confirmación de desconexión**: no existía en el componente, y la prueba
   que decía comprobarlo hacía clic en «Cancelar». Implementado con un escuchador de documento
   mientras la confirmación está abierta, con el foco de vuelta a «Desconectar», y probado pulsando
   `Escape` de verdad en unitaria y en E2E.
3. **Cuatro de los siete estados eran inalcanzables** porque la pila de E2E no tenía servicio falso
   de GitHub. Ahora existe y los estados se recorren de verdad.
4. **Una prueba que no podía fallar** (`expect(states).toHaveLength(7)` sobre un literal de siete
   cadenas) contaba como cobertura de los siete estados. Borrada.
