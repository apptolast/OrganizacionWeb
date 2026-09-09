# Revisión UX de automatizaciones (30, fase 1)

Ruta revisada: `/automatizaciones`. Corte: rama `claude/automations` sobre `origin/main` 9d81c17.
Componentes `frontend/src/automations.tsx` y `frontend/src/automations-api.ts`, estilos bajo
`.automations` en `frontend/src/styles.scss`.

Esta matriz cubre la **fase 1**: declarar, editar, simular y auditar reglas. La fase 2 (ejecución
por el worker y `NOTIFY_WEBHOOK` real) no existe todavía y **no se valora aquí**.

Regla aplicada de `AGENTS.md`: axe por sí solo no declara cumplimiento. Lo que sigue distingue
siempre lo **medido** de lo **revisado heurísticamente**, y nombra lo que no está demostrado.

## Estados verificados (los siete que exige la revisión)

Todo lo de abajo se ejecutó sobre la pila real (`docker compose`, PostgreSQL y backend de verdad),
puerto 18093, pila retirada al terminar.

| Estado | Cómo se midió | Resultado |
| --- | --- | --- |
| 1. Anchos 320, 768, 1280 y 1440 px CSS | `e2e/automations-ux.spec.mjs`, lista y editor abiertos | Sin desplazamiento horizontal (`scrollWidth <= clientWidth`) y todo control ≥ 44 × 44 px |
| 2. Tema claro | `page.emulateMedia({ colorScheme: "light" })` en los cuatro anchos | Verificado; capturas `light-<ancho>.png` |
| 3. Tema oscuro | `page.emulateMedia({ colorScheme: "dark" })` en los cuatro anchos | Verificado; capturas `dark-<ancho>.png` |
| 4. Texto al 200 % | Se dobla el tamaño **calculado** de cada elemento de `main` por su atributo `style`, y se comprueba elemento a elemento que el tamaño final es el doble | Verificado en los cuatro anchos y los dos temas, sin desbordamiento ni controles por debajo de 44 px |
| 5. Zoom nativo al 200 % | Chromium real con extensión y `chrome.tabs.setZoom(tab, 2)`; se comprueba `devicePixelRatio` duplicado y se estrecha la ventana hasta `innerWidth === 320` | Verificado; captura `zoom200-compositor.png` y medidas en `native-zoom.json` |
| 6. `forced-colors: active` | `page.emulateMedia({ forcedColors: "active" })` a 320 px | `matchMedia` confirma el modo; foco alcanzable y visible; sin desbordamiento |
| 7. `prefers-reduced-motion: reduce` | `page.emulateMedia({ reducedMotion: "reduce" })` | `matchMedia` confirma el modo y **cero** elementos con animación o transición activa en `main` |

axe (`wcag2a`, `wcag2aa`, `wcag21aa`, `wcag22aa`, `best-practice`) devuelve **cero violaciones de
cualquier impacto** en los estados 1–5 y 7. En `forced-colors` se desactiva sólo `color-contrast`,
porque los colores los impone el sistema operativo y no la hoja de estilos; el resto de reglas sí
se evalúa.

### Límites explícitos de esta evidencia

- El zoom nativo se mide **sólo en Chromium**. Para Firefox y WebKit se afirma texto al 200 % y
  reflujo, no zoom nativo.
- El texto al 200 % es zoom **de texto**, no de disposición: se dobla `font-size`, no se escala el
  lienzo. Es la misma técnica que el resto de auditorías del repositorio.
- Nada de esto es un estudio con usuarios. Las valoraciones de comprensión y jerarquía son revisión
  heurística.
- No hay medición de `Doherty` (< 400 ms) contra un servidor real: lo que está verificado es que la
  interfaz muestra estado de trabajo **antes** de la respuesta y que nunca anticipa éxito.

## Dos defectos reales que encontró esta auditoría

Ninguno se detectó por inspección; los destapó la ejecución.

1. **La página no tenía estilos.** El componente traía `className="automations"` pero
   `styles.scss` no definía ese bloque, así que los `input` y `select` medían 27 px de alto y el
   texto al 200 % desbordaba en horizontal. Se añadió el bloque `.automations` **usando sólo
   tokens del tema**, verificado por `theme-tokens.test.ts` (la guarda global contra colores fijos).
2. **El enlace «Saltar al contenido» estaba roto en esta ruta.** El armazón apunta a `#proyectos` y
   mi `<main>` no llevaba ese `id`, de modo que axe reportaba `skip-link` y `region`. Corregido con
   `<main id="proyectos" className="automations">`, como el resto de páginas.

## Matriz de los 30 principios

| Principio | Aplicación y evidencia | Resultado / límite |
| --- | --- | --- |
| Atención selectiva | Un solo objetivo por pantalla: la lista, y dentro del editor «Simular» y «Guardar». Los avisos van en `role="alert"` discreto. | Revisión heurística sobre capturas de los 4 anchos y 2 temas. |
| Carga cognitiva | El editor pide nombre, disparador, proyecto opcional y plantillas; nada exige recordar datos de otra pantalla, y los proyectos se ofrecen en un `<select>`. | Recorrido verificado en E2E; comprensión pendiente de uso real. |
| Estética-usabilidad | Reutiliza tokens y patrones de `.export-data` y `.calendar-feed`; errores del servidor se anclan a su campo. | Verificado visual y funcionalmente; no se infiere facilidad de la estética. |
| Posición en serie | «Nueva regla» al final de la lista y al pie del estado vacío; en el editor, Guardar antes que Simular en el DOM y en el orden de teclado. | Orden verificado en los 4 anchos. |
| Tendencia a la meta | No hay progreso ni objetivo cuantificado en automatizaciones. | No aplicable; no se inventa progreso. |
| Von Restorff | El estado de cada regla se distingue por **texto** («Activa»/«Inactiva») además del borde; nunca sólo por color. | Verificado en claro, oscuro y `forced-colors`. |
| Zeigarnik | Ante un 412 el borrador se conserva hasta que se pulsa «Cargar versión actual»; ante errores de campo, también. | Verificado en unitario y E2E. |
| Fluir | Automatizar no inicia ni cierra sesiones de trabajo. | No aplicable; la regla no altera el recorrido de sesiones. |
| Fragmentación | Lista, editor e historial son secciones separadas con su propio encabezado y región. | Verificado estructuralmente. |
| Memoria de trabajo | El borrador vive en memoria del componente; los errores del servidor no lo borran. No hay `localStorage` ni `sessionStorage`. | Verificado en unitario (se comprueba que ambos almacenes quedan vacíos). |
| Navaja de Occam | Cada control crea, edita, activa, simula, guarda, borra o pagina. No hay pasos redundantes. | Revisión heurística del recorrido. |
| Conectividad uniforme | No hay diagramas ni líneas decorativas. | No aplicable. |
| Fitts | Todo `a`, `button`, `input` y `select` de `main` mide ≥ 44 × 44 px. | **Medido** en 4 anchos × 2 temas, con texto al 200 % y con zoom nativo. |
| Hick | Doce disparadores en un `<select>` con etiqueta en español, y un solo proyecto opcional; sin menús anidados. | Revisión heurística; no se afirman tiempos de decisión medidos. |
| Jakob | `<select>`, `<input>`, botones y un interruptor con `role="switch"` y `aria-checked`. | Verificado por rol accesible en unitario y E2E. |
| Semejanza | Estados y controles reutilizan los estilos existentes de formularios y tarjetas. | Verificado en ambos temas. |
| Miller | Agrupación por significado (regla, acción, historial); no se impone un número de opciones. | Revisión heurística. |
| Parkinson | `estimatedMinutes` es un valor fijo y opcional, nunca calculado ni ampliado en silencio. | Verificado en el contrato y en el dominio. |
| Postel | El nombre recorta `White_Space` Unicode y admite 1–80 puntos de código, emoji incluido; las plantillas se guardan byte a byte, con llaves sueltas permitidas. | Verificado por tests de dominio y de API. |
| Proximidad | Cada error va en un `<p>` inmediatamente después de su campo, enlazado por `aria-describedby`, con `aria-invalid` y foco automático. | Verificado en unitario; también a 320 px. |
| Prägnanz | Los estados de ejecución se leen como texto («Correcta», «Reintento», «Fallida») además del color, y el código de error se muestra cuando existe. | Verificado en unitario. |
| Región común | La lista es un `<ul aria-label="Reglas">`, el editor y el historial son `<section>` con etiqueta. | Verificado estructuralmente y con axe. |
| Tesler | El sistema resuelve de qué proyecto habla cada evento (por agregado, por tarea o por sesión); el usuario sólo elige «Sólo en el proyecto». Los errores internos nunca se muestran. | Verificado en dominio, aplicación y persistencia. |
| Modelo mental | La regla **crea una tarea pendiente**; no completa, no cierra ni replanifica nada. La simulación no escribe. | Verificado: la simulación deja el almacén intacto, comprobado también en SQL sobre la pila real. |
| Usuario activo | El estado vacío explica qué es una regla y ofrece «Nueva regla»; la ayuda de los cuatro marcadores está siempre visible en el editor, con vista previa. | Verificado en unitario y E2E; primera experiencia no medida con usuarios. |
| Pareto | Crear, simular y activar están en una pantalla, sin esconder el historial ni el borrado. | Priorización de diseño, no porcentaje de uso observado. |
| Fin de pico | «Simular» permite comprobar antes de guardar; el guardado sólo confirma tras la respuesta y el 412 se explica con recarga deliberada. | Verificado en unitario y E2E. |
| Sesgo cognitivo | No hay métricas de productividad ni presión; `wouldFail` anticipa el fallo en vez de ocultarlo. | Verificado en la simulación. |
| Sobrecarga de opciones | Dos tipos de acción, doce disparadores y una condición opcional; valores iniciales útiles al abrir el editor. | Verificado estructuralmente. |
| Doherty | El botón «Guardar» se deshabilita durante la petición y sólo se envía una; el interruptor no cambia hasta la respuesta confirmada y vuelve atrás si falla. | Verificado en unitario con respuesta retenida. **No** se mide latencia contra servidor real. |

## Cómo reproducir

```
E2E_WEB_PORT=18093 pnpm test:e2e e2e/automations-ux.spec.mjs
E2E_WEB_PORT=18093 pnpm test:e2e e2e/automations.spec.mjs
```

4/4 y 7/7 respectivamente, con la pila retirada al final. Evidencia (capturas y JSON de medidas)
bajo `test-results/` de cada ejecución.

## Lo que esta revisión NO declara

- No declara la feature 30 lista para producción: falta la fase 2 y la puerta de mutación.
- No sustituye un estudio con usuarios.
- No afirma zoom nativo en Firefox ni WebKit.
- No cubre ningún escenario de ejecución de reglas: en fase 1 ninguna regla se ejecuta.
