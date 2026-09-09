# Revisión UX de Webhooks (25)

Vista revisada: `frontend/src/webhooks.tsx` + `webhooks.scss`, ruta `/webhooks`.
Auditoría E2E: `e2e/webhooks-ux.spec.mjs`, **6/6 verde** (`E2E_WEB_PORT=18097 pnpm
test:e2e e2e/webhooks-ux.spec.mjs`). Unitarios de la vista y del cliente: **44
verdes**, con este desglose por fichero, medido con la salida real de Vitest
(`pnpm --dir frontend exec vitest run src/webhooks.test.tsx
src/webhooks-route.test.tsx src/webhooks-client.test.ts` → `Tests 44 passed`):
`webhooks.test.tsx` 23 (21 `it` más un `it.each` de 2 filas),
`webhooks-client.test.ts` 18, `webhooks-route.test.tsx` 3.

La cifra «58 verdes» que figuraba aquí no era reproducible y queda corregida:
en el commit que escribió esta matriz (`99f1e94`) los tres ficheros ya
declaraban 22 + 18 + 3 = 43 `it` (44 casos con el `it.each`), y ningún otro
fichero de pruebas del frontend contiene pruebas de webhooks
(`integration-api.test.tsx` sólo lo menciona en un comentario). No hay ningún
comando ni filtro sobre estos tres ficheros que produzca 58, así que el número
no venía de incluir ficheros ajenos: era, simplemente, un recuento erróneo. La
corrección no implica cobertura perdida.

Esta matriz aplica `docs/ux-requirements.md`. Conforme a `AGENTS.md:51`, **no se
declara cumplimiento a partir de axe**: axe es una de las evidencias, no la
conclusión. Las filas de comprensión y jerarquía son revisión heurística, no un
estudio con personas usuarias.

## Alcance real de la evidencia, y sus límites

- **Siete estados auditados**, no cuatro: `empty`, `form`, `secret`, `list`,
  `deliveries`, `confirm` y `error`. Cada uno se mide en **320, 768, 1280 y
  1440 px** y pasa axe (`wcag2a`, `wcag2aa`, `wcag21aa`, `wcag22aa`).
- **Modalidades**: tema claro, tema oscuro, **texto al 200 %**, `forced-colors:
  active` y `prefers-reduced-motion: reduce`. Las dos últimas se comprueban
  además leyendo `matchMedia` en la página, para no dar por hecho que la
  emulación se aplicó. `color-contrast` **solo** se desactiva bajo
  `forced-colors`, donde la paleta la impone el sistema; **nunca** en claro ni
  oscuro.
- **Teclado**: recorrido con Tab dentro de `main`, cada control con nombre
  accesible no vacío, y foco de vuelta al control que abrió la confirmación.
- **Evidencia en disco** bajo `.e2e-work/webhooks-ux/<modo>/`: `geometry.json`
  (ancho, `scrollWidth` y caja de cada control por estado y ancho),
  `<estado>-axe.json`, `<estado>-font-scale.json`, `media.json`,
  `tab-order.json` y capturas a 320 y 1440.

**Límites que no oculto.** La API del navegador se simula con `page.route`, así
que esto audita interfaz real, **no** aceptación de backend (esa la cubre la
suite JVM filtrada: 584 verdes). **No se ejecutó zoom nativo del navegador**: la
comprobación de 200 % es por tamaño de fuente computado y reflow, medida y
verificada elemento a elemento. El precedente de la feature 24 lo cubrió con una
extensión y DPR duplicado en Chromium; aquí **no se afirma zoom nativo**. Tampoco
se ejecutó la suite E2E completa ni mutación: son puertas del coordinador.

## Defectos reales que encontró esta auditoría

Se documentan porque son el motivo de que la auditoría exista, no para adornarla.

1. **Sin landmark `main`.** La vista abría en `<section>`, así que el enlace
   «Saltar al contenido» (que apunta a `#proyectos`) no tenía destino en
   `/webhooks`. Corregido con un test unitario propio.
2. **La tabla de entregas se salía de la pantalla.** El botón «Reenviar» caía en
   x=907 con el viewport en 768. Ocho columnas con objetivos de 44 px no caben
   en 320 px, y `@s42` prohíbe **a la vez** el scroll horizontal y el recorte de
   la tabla, así que el contenedor con `overflow-x` era la respuesta equivocada:
   recorta. Ahora la tabla **refluye**: en pantallas anchas conserva columnas
   reales y en estrechas apila cada entrega en un bloque etiquetado. Se añaden
   roles ARIA explícitos (`table`, `row`, `columnheader`, `cell`) porque
   sobrescribir `display` en elementos de tabla borra la semántica del árbol de
   accesibilidad, y cada celda lleva su propia etiqueta: **no se oculta ningún
   dato en ningún ancho**.
3. **Desbordamiento real al 200 % de texto**, en el primer estado y por tanto a
   escala 2× legítima: un campo de texto tiene ancho intrínseco `size=20` que
   crece con la fuente, y tokens como `X-OrganizationWeb-Signature` no rompen.
   Corregido con campos fluidos y `overflow-wrap: anywhere`.
4. **Foco perdido al cancelar la confirmación de borrado.** Solo el panel del
   secreto devolvía el foco. Corregido y cubierto por unitario y por E2E.

**Un fallo era mío, no del producto.** El ayudante de texto al 200 % guardaba el
tamaño original pero **no lo restauraba**, así que cada estado volvía a doblar
(51 → 102 → 204 → 409 px) y el `h1` llegaba a 368 px. Estuve a punto de
«arreglar» el CSS para satisfacer un oráculo roto. Corregido el ayudante,
**retiré** las dos reglas que solo servían para aquello (`word-break` en
encabezados y `min-inline-size` en `fieldset`) y volví a ejecutar: 6/6 sigue
verde, luego eran innecesarias.

## Matriz de los 30 criterios

«Verificado» se limita a la medición descrita. Ninguna fila se omite.

| Criterio | Aplicación y evidencia | Resultado / límite |
| --- | --- | --- |
| Atención selectiva | «Crear webhook» es la acción principal; el panel del secreto aparece por delante de la lista y concentra Copiar/Cerrar. Capturas 320 y 1440 en los siete estados. | Verificado visualmente; jerarquía por revisión heurística. |
| Carga cognitiva | El alta pide solo URL, descripción opcional y tipos. El resto (cursor, reintentos, firma) lo resuelve el sistema y no se pregunta. | Recorrido verificado; comprensión pendiente de uso real. |
| Estética-usabilidad | Solo tokens del tema (guarda `theme-tokens` en verde); errores explicados en texto; axe limpio en claro y oscuro. | Verificado; no se infiere facilidad a partir de la estética. |
| Posición en serie | Formulario, lista y panel de entregas mantienen su orden en los cuatro anchos; el orden de Tab sigue al DOM. | Verificado en navegador (`tab-order.json`). |
| Tendencia a la meta | No hay objetivo ni progreso cuantificado en esta gestión. | No aplicable; no se inventa progreso. |
| Von Restorff | El secreto se distingue por posición, borde acentuado y texto de aviso, no solo por color; bajo `forced-colors` sigue distinguiéndose por estructura. | Verificado en ambos temas y en colores forzados. |
| Zeigarnik | Ante fallo de red el resultado se declara **incierto** y se ofrece «Actualizar lista»; el formulario conserva lo escrito. | Verificado unitario y E2E (estado `error`). |
| Fluir | No inicia ni extiende ninguna sesión de trabajo. | No aplicable; no altera el recorrido de sesiones. |
| Fragmentación | Tres regiones: crear, lista de webhooks y registro de entregas, cada una con su encabezado. | Verificado estructuralmente. |
| Memoria de trabajo | Tras un 409 o un 400 el formulario sigue visible con sus valores; el secreto nunca se recupera de almacenamiento. | Verificado unitario (`@s41`) y E2E. |
| Navaja de Occam | Cada control crea, copia, cierra, activa, desactiva, hace ping, consulta entregas, reenvía o elimina. Sin pasos redundantes. | Revisión heurística del recorrido. |
| Conectividad uniforme | No hay diagramas ni conexiones decorativas. | No aplicable. |
| Fitts | Botones y campos ≥44×44 px; el **label** es el objetivo real de cada casilla y también mide ≥44 px. Medido en 7 estados × 4 anchos, sin solapes. | Verificado geométricamente (`geometry.json`). |
| Hick | Doce tipos visibles con un «Seleccionar todos»; sin menús anidados. | Revisión heurística; no se miden tiempos de decisión. |
| Jakob | Campo `type="url"`, casillas, botones y diálogo de confirmación convencionales. | Verificado unitario y en navegador. |
| Semejanza | Bordes, foco y superficies reutilizan los tokens y estilos existentes. | Verificado en claro, oscuro y colores forzados. |
| Miller | Agrupación por significado (datos del webhook, tipos, entregas); no se impone un número de opciones. | Revisión heurística. |
| Parkinson | No hay duración de sesión ni ampliación automática. | No aplicable a webhooks. |
| Postel | La descripción admite espacios Unicode y hasta 80 puntos de código; la URL exige https absoluto sin credenciales ni fragmento. El cliente rechaza cualquier DTO con campos de más. | Verificado en dominio y en el cliente; sin debilitar validación. |
| Proximidad | El error de URL se asocia al campo con `aria-describedby`; la ayuda vive junto al formulario. | Verificado unitario (`@s41`) y por DOM. |
| Prägnanz | Estados en texto: Activo, Desactivado manualmente, Desactivado por entregas agotadas el AAAA-MM-DD; nunca solo un icono. | Verificado; el mismo texto va en el atributo ARIA. |
| Región común | Secreto, formulario, cada webhook y el diálogo de borrado están agrupados en contenedores propios. | Verificado visualmente y por estructura. |
| Tesler | La complejidad (cursor, arrendamiento, reintentos, firma) queda en el servidor; los `problem+json` se traducen a mensajes en español sin trazas. | Verificado; la auditoría no puede exponer URL ni secreto por construcción. |
| Modelo mental | Un webhook entrega hechos ya confirmados; gestionarlo no cambia tareas, bloques ni sesiones. | Revisión de alcance; la outbox nunca se escribe (test de persistencia). |
| Usuario activo | El estado vacío explica qué es un webhook, nombra la cabecera de firma y el límite de cinco, con el formulario visible. | Verificado (`@s36`); primera experiencia no medida con personas. |
| Pareto | Crear y revisar entregas caben en una pantalla; nada se esconde tras menús. | Priorización de diseño, no porcentaje de uso observado. |
| Fin de pico | El secreto se muestra una sola vez con aviso explícito y solo desaparece al pulsar Cerrar; el borrado exige confirmación y devuelve el foco. | Verificado unitario y E2E. |
| Sesgo cognitivo | Sin métricas de productividad ni presión; ningún tipo de evento viene marcado por defecto. | Verificado estructuralmente. |
| Sobrecarga de opciones | Doce tipos acotados con selección en bloque; las acciones destructivas piden confirmación. | Verificado; comprensión pendiente de uso real. |
| Doherty | El botón queda deshabilitado mientras el POST está en vuelo y el estado de carga se anuncia por `aria-live`, sin fingir éxito ni porcentaje. **No se midió** el umbral de 400 ms en este carril. | Parcial y declarado: comportamiento verificado, latencia **no** medida. |

## Puertas que siguen abiertas

Regresión E2E completa, mutación y aceptación de despliegue las abre el
coordinador. Esta matriz **no** declara la feature disponible en producción.
