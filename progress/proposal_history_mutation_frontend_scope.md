# Propuesta de alcance Stryker18

Corte: frontend final aprobado en COMMON0fa1729. Sólo propuesta; no configuración aplicada, instrumentación ni campaña. Evidencia parser TypeScript instalado6.0.3: `history_mutation_scope_parser.mjs` → `history_mutation_scope_nodes.json`, EXIT0 5c9a25. Seis fuentes sin diagnósticos sintácticos; hashes completos en el JSON. Líneas comienzan en1 y columnas en0, conforme al lector de rangos de Stryker10 instalado.

## Selección propuesta

```json
[
  "src/history-api.ts",
  "src/history.tsx",
  "src/App.tsx:14:8-14:57",
  "src/App.tsx:25:12-31:22",
  "src/App.tsx:38:10-62:7",
  "src/workspace.tsx:55:10-60:22",
  "src/project-reader.tsx:111:10-116:22",
  "src/task-reader.tsx:112:10-117:22"
]
```

- Dos módulos nuevos completos, sin excluir validadores, errores, privacidad, filtros, foco, fechas, orden, detalles ni ramas difíciles.
- App incluye expresión completa de reconocimiento de URL, condición/etiqueta de sección y condición/montaje JSX de History. Los rangos incluyen cada ConditionalExpression completo mínimo y sus alternativas antiguas necesarias; no recortan la nueva decisión. Corrección requerida por root2100a6, ver parser5d668f.
- Workspace incluye el enlace completo con href y comparación de aria-current. Ambos detalles incluyen todo el RouteLink nuevo, className y plantilla de URL con los identificadores conocidos.
- Stryker sólo muta nodos contenidos completos en el rango (`babel-transformer.shouldMutate`); recorre los antecesores que se solapan. No se afirma que cada identificador aislado genere un mutante: el mutador ConditionalExpression instalado no trata el identificador test de un ternario como if. Aun con esa limitación del mutador, ambos ternarios completos quedan dentro del alcance, junto con la expresión regular, llamadas, strings, plantillas y comparación de Workspace. No se cambia el conjunto heredado de mutadores para fabricar conteos.
- Los tres cambios de `export` de `isHistoryEntry`/`isChange` en task-status-api, reschedule-api y work-session-state-api no alteraron cuerpos. No incorporan comportamiento nuevo que requiera mutación íntegra de esas tres familias; sus oráculos siguen disponibles y se ejecutaron en el foco528. Imports, tipos y SCSS no son comportamiento JS que instrumente esta selección.

## Ejecución futura propuesta

Archivo `frontend/stryker.history.config.json`; target `history-frontend`. Heredar runner Vitest/config vite.config.ts, todos los tests disponibles sin testNamePattern/include reducido, coverageAnalysis perTest, concurrency8, umbral high90/low80/break80 y mutadores por defecto intactos. Reporters clear-text/json/html; salidas `reports/mutation-history/mutation.json` y `.html`, temporal `.stryker-tmp-history`. Preservar exactamente ignorePatterns protegido, sin explorar ni limpiar ese directorio. No incremental ni replay en la primera campaña.

Al aplicar soporte, TDD de despacho individual en scripts/project.mjs y su suite, preservando history-backend ya integrado. No editar build.gradle.kts. El default Stryker debería añadir los dos módulos y enlaces de Workspace/proyecto no cubiertos por sus rangos actuales; task-reader ya es completo. Se añaden también ambos rangos completos de App: el montaje llega hasta62 y supera el rango histórico10–49. Esto se propone para no omitir18 del alcance general, conservando su configuración de concurrencia y demás selecciones; requiere la revisión de root antes de editar.

Pruebas candidatas principales: history-api53, History30, ReadProjects36 y composición WorkSession3; TaskBlocks86 acreditó regresión del montaje de tarea. Son candidatas descriptivas, no filtro de campaña. Los528 cliente/herencia y154 montaje se conservan como ejecuciones reales separadas; tras el último caso de rango sólo History30 se repitió, sin inventar155 conjunta. El init/build de root será la línea base global antes de autorizar instrumentación.

Antes y después se capturarán hashes de todas las fuentes, tests y configuración efectiva, con estados raw completos y RuntimeError separados de Killed. No se estima duración ni número de mutantes desde17. La campaña espera revisión de esta propuesta, aplicación/validación del soporte y autorización tras init/build verde.
