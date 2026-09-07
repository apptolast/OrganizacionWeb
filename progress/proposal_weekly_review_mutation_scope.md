# Propuesta de alcance Stryker 19

Preparada sobre el frontend aprobado en `aea633a`. No se ha creado configuración ni ejecutado mutación.

`weekly_review_mutation_scope_nodes.json` conserva hashes, texto y posiciones obtenidos con el parser TypeScript instalado; cero diagnósticos. Las líneas son uno-basadas y las columnas cero-basadas, como los alcances anteriores.

| Fuente | Alcance propuesto | Motivo |
| --- | --- | --- |
| `src/weekly-review-api.ts` | completo | Decoder cerrado, fechas civiles, long/BigInt, sumas y guardas post-await. |
| `src/weekly-review.tsx` | completo | Selección, navegación, carga, errores, privacidad, foco y presentación. |
| `src/App.tsx` | `15:8-15:69` | Reconocimiento de la ruta nueva. |
| `src/App.tsx` | `27:12-35:24` | ConditionalExpression completo de sección, incluida su condición nueva. |
| `src/App.tsx` | `42:10-68:7` | ConditionalExpression completo de montaje, incluida su condición nueva. |
| `src/workspace.tsx` | `67:10-72:22` | Enlace completo y condición de aria-current. |

Los condicionales de App incluyen las alternativas heredadas necesarias para instrumentar el nodo completo: no se recorta el identificador de condición. Imports y tipos no son comportamiento ejecutable. No hay cambios de cuerpos ni exports en helpers heredados; no se atribuye cobertura nueva a esos validadores.

Configuración propuesta: `frontend/stryker.weekly-review.config.json`, derivada de History, con Vitest y todas sus suites, `perTest`, ocho workers, mutadores heredados, umbral 80 (high 90/low 80), JSON/HTML/texto. Salidas `reports/mutation-weekly-review/` y temporal propio `.stryker-tmp-weekly-review`; conservar el ignore protegido sin leer ni limpiar su contenido.

El default contiene una lista explícita y no incluye los dos módulos nuevos: propongo añadir estos mismos seis scopes, preservando todos los anteriores y sus ajustes. Esta propuesta no cambia el significado de configuraciones históricas. El dispatcher y Gradle pertenecen a A; no los edito. Target propuesto `weekly_review-frontend`, pendiente de confirmar con A/root.

Antes de campaña: revisión de config, freeze integrado, actualización del inventario parser si cambia fuente, manifiesto efectivo de código/tests/config antes y después. Registrar todos los estados sin reclasificar errores como muertos; ningún replay automático.

## Configuración aplicada tras aprobación del coordinador

Target confirmado con A: `weekly_review-frontend`. Config propia SHA256 `13A67790F5034A697B699B91438A48FA0371F89E2F7726A9C402716FF6C91602`; default `9CDA0F327F08B7717CCDBEB02BE5D4D2DE923D5E61404E94ACE070C45A780A38`. Verificación estructural `484fed`: ajustes iguales a History, seis scopes añadidos al final del default sin retirar ni modificar entradas o ajustes anteriores. Prettier inicialmente señaló ambos JSON recién serializados; write/check focal dejó formato GREEN `df75c2`. Ninguna campaña ejecutada.

## Preflight tras UX y corrección final de foco

Parser actualizado `42944e`: fuente WeeklyReview `284098F9B523FD289E026C5BFD60C626C9437D9A89354CEC684F4AA2A11FE16C`, mismos cuatro nodos externos; módulos siguen completos. El parser `.mjs` se conserva como herramienta de evidencia reproducible, no test del producto. Configuración no cambia.

Frontend completo y lint posterior al arreglo: `666856`, EXIT0, 1.965 tests/42 archivos (22,92 s), ESLint y Prettier completos verdes. Logs `weekly_review_frontend_final_gate.log` y `weekly_review_frontend_final_lint.log`. Manifiestos `weekly_review_frontend_final_before.json`/`weekly_review_frontend_final_after.json`: 141 entradas idénticas, cero diferencias (`eb64bc`). Incluyen todo `frontend/src`, configuración superior de frontend, paquetes/lock y scripts/project/harness.config; excluyen documentación, E2E y .gitignore. No Gradle, build ni mutación repetidos por este preflight. Se capturará un before efectivo nuevo inmediatamente antes de la campaña autorizada.
