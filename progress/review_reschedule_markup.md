# Revisión parcial de markup13 — 2026-09-06

Lectura independiente del panel en TDD, sin suites, mediciones de navegador ni cambios de fuentes/tests/CSS. No es dictamen de cierre13. Se conserva la revisión aprobada de API, historial y confirmación; aquí se examina únicamente su composición nueva. Referencias: `features/reschedule.feature` @s29/@s35/@s38/@s40 y `docs/ux-requirements.md` (asociación de errores, contexto, controles44px y reflow).

## Hallazgos accionables antes de E2E

1. **Inputs nuevos sin el estilo de campos compartido.** `reschedule-block.tsx:233–299` coloca zona, inicio y fin en labels sin contenedor `.field`. En `styles.scss:293–324`, ancho completo, padding, borde, colores de readonly/invalid y line-height sólo aplican a inputs dentro de `.field`. El selector general `.task-blocks input` (`:1466`) aporta min-width y font-size, pero no altura ni padding. El panel queda dependiente del tamaño nativo del input y de su ancho intrínseco, especialmente datetime-local en un contenedor móvil con padding. **Mínimo:** reutilizar la estructura `.field` del editor11, con label asociado al control y error adyacente. No basta suponer que heredar `.task-blocks` ya garantiza44px. No se afirma un tamaño medido: debe comprobarse en navegador, a320 y entre breakpoints.

2. **Errores específicos incompletos y persistencia visual del error anterior.** Se almacenan todos los `problem.errors` (`reschedule-block.tsx:217`), pero sólo se deriva `startError` (`:120`) y se presenta/asocia en inicio (`:268–282`). Un rechazo en fin, zona u ocurrencia deja únicamente «No se pudo revisar el movimiento», sin señalar el campo ni mostrar su explicación. Además, `invalidate()` y el camino de preview satisfactoria no retiran `fieldErrors`: tras corregir inicio puede quedar `aria-invalid` y el texto anterior junto a una revisión válida. **Mínimo:** mostrar/asociar los errores de los campos editables y retirar los mensajes ya obsoletos al corregir/revisar según el patrón existente. Probar un rechazo en fin y su corrección con respuesta válida cubre una diferencia observable; no hace falta una batería por cada atributo.

3. **Confirmación inline de cancelación sin intervalo seleccionado.** `BlockActions` monta `ChangeSubmit` en modo cancelación. Éste presenta objetivo y explicación (`change-submit.tsx:94–100`), pero no inicio/fin antes de «Confirmar cancelación del bloque». La lista anterior sí contiene intervalos, pero puede tener varios bloques con el mismo objetivo y no identifica allí cuál está seleccionado. @s38 requiere que la confirmación inline identifique objetivo e intervalo. **Mínimo:** reutilizar `BlockDetails` con el `state.block` leído, junto al objetivo dentro de la confirmación; no inferirlo de la posición de la lista ni añadir otra consulta. Este punto trata la confirmación previa al envío, no reabre el componente aprobado de recibo histórico.

## Reglas existentes que sí alcanzan esta composición

- Selectores DST bajo `.task-blocks select`: ancho completo y min-height48px (`styles.scss:1472`).
- Botones: min-height45px global (`:367`) y min-width44px/max-width100% en `.task-blocks` (`:1486`), con ancho completo bajo600px. No hay evidencia estática de que todos los botones sean pequeños.
- Checkbox: label interactiva con min-height44px, espacio y wrap (`:1511`); la caja22px no se confunde con toda el área de su label.
- Textos largos en párrafos y headings heredan overflow-wrap del contenedor (`:1153–1174`, `:1491`); no se reporta overflow de objetivos sin medición.
- Labels envolventes ya proporcionan nombre accesible a los inputs. La primera observación es de estructura/estilo, no de ausencia de nombre.

Estos puntos se comunicaron al autor y al coordinador para incorporarlos al orden TDD actual. La geometría final, foco, texto ampliado, feedback, axe y motores permanecen sujetos a la validación de @s40; no se exige una certificación adicional ni se bloquea la autoría por la matriz aún en progreso.

Evidencia de lectura: `10be89`, `8c64c3`, `f9e09c`, `bd15e4`. Hashes del corte (autor sigue trabajando): `reschedule-block.tsx` `98CAE04C2EEDF2084D5EA7C57DEE04E8E8C57143B1F764930C6F4392F3A75343`; `task-blocks.tsx` `2A5404FF2B2D93E6D0B13E8E0EBB75EDB983640EFD4B3B30EF3700D49A20A380`; `styles.scss` `F6CDB22BE4DBFA2C2BE01D40C76C86A023262D6DAC66F01C8714966CCCA34122`. Las líneas describen este corte parcial, no un freeze de producción.
