# Juez independiente — frontend read_projects

## Dictamen de fuente

**APPROVED.** Revisión por backend_craftsman, sin autoría ni cambios en producción/tests frontend. No se repiten suites durante esta revisión. No se han encontrado defectos bloqueantes en el código inspeccionado.

Archivos revisados: App.tsx, navigation.tsx, workspace.tsx, project-reader.tsx, use-read-projects.ts, read-projects-api.ts, estilos SCSS y pruebas de lectura. Comparación con features/read_projects.feature y la sección UI de project-spec.md.

- Navegación: rutas de lista, cursor y detalle con enlaces reales; History API conserva URL y popstate, y modificadores de clic conservan comportamiento del navegador. App monta ProjectReader con key de ruta: una página nueva no reutiliza datos de la anterior. El formulario existente queda como recorrido separado.
- Solicitudes: credenciales same-origin, cache no-store, AbortSignal; exige HTTP 200, valida estructura utilizable, fechas y correspondencia de id del detalle. Respuestas incompatibles se presentan como error recuperable, no falso vacío ni excepción de render. El cliente no decodifica ni confía en el cursor.
- Respuestas tardías: limpieza aborta cada efecto; tanto éxito como rechazo comprueban signal antes de cambiar estado. La combinación de key por ruta y guardia cubre navegación y repetición StrictMode. Retry limpia datos y fallo antes de volver a consultar. Un 401 muestra autenticación requerida y retira contenido privado; un 401 obsoleto de un efecto abortado no altera la ruta vigente.
- Texto y estados: nombres/descripciones se interpolan como texto React, sin innerHTML. Vacío solo después de respuesta válida; páginas de continuación vacías ofrecen regreso al inicio. Detalle 404 no reutiliza datos antiguos; errores de red/500/503 permiten Reintentar.
- Semántica: main, encabezado h1, lista ul/li, enlaces, navegación con nombre, estados role=status/alert y fechas time/datetime con UTC visible. Foco al encabezado tras cargar página/detalle. SCSS conserva saltos de línea, rompe palabras largas y establece áreas táctiles principales; esas reglas requieren comprobación real de geometría y teclado.
- Persistencia cliente: no hay almacenamiento local/sessionStorage ni credenciales guardadas por el nuevo recorrido. El estado de la aplicación procede de respuestas verificadas.

## Límites del dictamen

Esta inspección no certifica las 30 heurísticas ni WCAG 2.2 AA por sí sola. Deben incorporarse resultados finales de tests/mutación y E2E, matriz de 12 anchos y breakpoints, zoom real al 200 %, contraste/foco/táctil y los navegadores/dispositivos realmente comprobados. Los pendientes de evaluación humana y dispositivos físicos deben quedar identificados, sin convertir requisitos en resultados. No se atribuye revisión independiente del backend a este archivo: ese código lo escribió su autor y lo revisan el coordinador y otro agente.

## Evidencia final revisada

La suite completa inicial de frontend verificó 73 tests, lint y build. Después se añadieron cinco casos sin cambios de producción: los 40 casos focalizados de lectura y lint final pasaron; junto con los 38 históricos hay 78 declarados. No se afirma una ejecución global nueva de 78.

Inspección independiente de los JSON Stryker: informe global con 276 Killed y 21 Survived de 297 (92,93 %), sin errores ni falta de cobertura. El replay separado tiene 17 Killed de 17. La bitácora identifica seis huecos observables corregidos por las nuevas pruebas y 15 equivalentes justificados. Se revisaron las justificaciones en el contexto del consumidor: los guardas debilitados siguen rechazando JSON incompatible o llegan al mismo error seguro; el contador de retry es opaco; los cambios de título de History API no afectan la ruta. No se combinan denominadores ni se presenta el replay como nueva puntuación global.

Integración revisada: 14 E2E Chromium, dos recorridos Firefox/WebKit, matriz de 12 anchos, teclado/axe y zoom real al 200 % con reflow a 320 px. El coordinador inspeccionó las capturas corregidas. Se conservan los límites declarados de dispositivos físicos, teclado virtual y evaluación humana; no se certifica accesibilidad universal. Fuentes: progress/tdd_read_projects_integration.md, progress/ux_read_projects.md y progress/mutation_read_projects_frontend.md.

Ponytail full y Caveman lite aplicados en esta revisión. No se solicitan capas, dependencias ni pruebas redundantes; se conserva la evidencia exigida por el contrato.
