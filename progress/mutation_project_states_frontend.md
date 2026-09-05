# project_states — mutación frontend

La ejecución completa 85794 terminó con EXIT 0: **284 / 312 mutantes detectados (91,03 %)**, 28 supervivientes, 0 sin cobertura, 0 timeouts y 0 errores. Duración: 10 min 8 s. Se conserva esta puntuación; el replay selectivo posterior no se suma como si fuera una segunda ejecución global.

## Alcance y reproducción

Stryker 10, Vitest 4.1.10, `coverageAnalysis: perTest`, dos workers y umbral 80 sin reducción. `pnpm exec stryker run stryker.project-states.config.json` instrumenta exactamente `src/project-status.ts`, `src/read-projects-api.ts`, `src/edit-project-api.ts`, `src/use-read-projects.ts` y `src/use-project-status.ts`. Son los dos módulos nuevos y los tres módulos de lógica compartida modificados. Los cambios de tipos de `projects-api.ts` no añaden ejecución. JSX y SCSS tienen pruebas de comportamiento y navegador; no se les atribuye esta puntuación. No se excluyeron mutantes ni se redujo el umbral.

Informes originales: `frontend/reports/mutation-project-states/mutation.json` y `.html`. Baseline original: 171 pruebas, lint y build verdes. Tras cinco pruebas de regresión adicionales, la ejecución 28182 confirmó **176 pruebas en 7 archivos y lint verdes**. Producción intacta; la compilación e integración anteriores siguen correspondiendo a las mismas fuentes.

## Revisión de los 28 supervivientes originales

Los identificadores corresponden al JSON de la ejecución completa. Las equivalencias se limitan al comportamiento público de esta aplicación y su contrato, no a invocaciones arbitrarias de funciones internas.

| Archivo / IDs | Revisión y evidencia |
| --- | --- |
| `edit-project-api.ts`: 4 | La expresión regular sigue validando el ETag. Todos los valores de producción llegan de `Headers.get`, que devuelve string o null; eliminar `typeof` no permite un ETag distinto en ese flujo. Equivalente. |
| `edit-project-api.ts`: 38, 45, 55 | Cambian sólo mensajes de errores internos. La interfaz los convierte en fallo seguro y nunca muestra esos mensajes. Equivalentes. |
| `project-status.ts`: 61 | Hueco observable: sin `typeof`, un array JSON `["idea"]` se convierte en clave válida. Nueva prueba exige rechazo de la respuesta y ausencia de acciones. Incluido en replay. |
| `read-projects-api.ts`: 91, 93, 94, 97 | Relajan el guard de objeto, pero los campos y validaciones posteriores rechazan primitivas; null lanza un error que recibe el mismo tratamiento privado. No convierten una respuesta JSON inválida en proyecto o página válida. Equivalentes. |
| `read-projects-api.ts`: 168, 169 | Sin el guard de array, `.every(isSummary)` sólo puede ejecutarse correctamente sobre un array recibido por JSON. Objetos o primitivas fallan y conservan el mismo estado de error. Equivalentes. |
| `read-projects-api.ts`: 184 | Cambia texto de un error interno que no se presenta. Equivalente. |
| `use-project-status.ts`: 190 | Sustituye el array vacío de dependencias por una constante estable; el efecto mantiene el mismo montaje y cleanup. Equivalente. |
| `use-project-status.ts`: 195, 196 | Los controles ya están deshabilitados durante envío o sin ETag válido; no hay otro consumidor de `change`. Estos guards son defensa redundante para el flujo público. Equivalentes. |
| `use-project-status.ts`: 201 | Hueco observable: el límite de un intento anterior podía ocultar un error posterior. Nueva prueba reproduce HTTP 409 seguido de reintento explícito HTTP 503 y exige retirar la capacidad anterior. Incluido en replay. |
| `use-project-status.ts`: 208, 214 | Los callbacks tardíos pertenecen al lector desmontado; el cambio de ruta remonta el lector y sus estados. La petición se aborta y las pruebas resuelven también dobles no cooperativos, sin repoblar la ruta nueva. Equivalentes para el árbol actual. |
| `use-project-status.ts`: 229, 230 | Hueco observable: permitían interpretar capacidad en HTTP 500. Nueva prueba usa ese cuerpo incompatible y exige error genérico, sin navegación de límite. Incluidos en replay. |
| `use-project-status.ts`: 233 | El parseo fallido produce undefined en vez de null; ambos paran en el mismo guard y mantienen el error. Equivalente. |
| `use-project-status.ts`: 265, 272 | `Number.isInteger` sigue rechazando strings y otros tipos; guards de tipo redundantes. Equivalentes. |
| `use-project-status.ts`: 269 | Cambia la aceptación de activeCount cero. Un rechazo por límite positivo sólo puede emitirse cuando activeCount alcanza ese límite; cero no pertenece al contrato de ACTIVE_PROJECT_LIMIT. Equivalente dentro del contrato aprobado. |
| `use-read-projects.ts`: 310 | Permitir una página en el snapshot no la convierte en proyecto: el control de estados sólo se monta cuando existe `project`, validado por su discriminante separado. Equivalente. |
| `use-read-projects.ts`: 316 | Hueco observable de privacidad: sin retirar data quedaba el enlace de edición aunque el cuerpo privado estuviera oculto. Nueva prueba exige retirarlo tras HTTP 401. Incluido en replay. |
| `use-read-projects.ts`: 319 | Hueco observable: el GET de recarga debe retirar inmediatamente acciones de la versión antigua. Nueva prueba difiere la respuesta y exige carga sin Activar, después Retomar con la respuesta nueva. Incluido en replay. |
| `use-read-projects.ts`: 322 | El contador sólo dispara un efecto por cambio; decrementar en vez de incrementar conserva ese comportamiento. Equivalente. |

## Replay selectivo

`pnpm exec stryker run stryker.project-states-replay.config.mjs` reproduce sólo las líneas 9 de project-status, 23 y 47 de use-project-status, y 31 y 36 de use-read-projects: 14 mutantes. El primer intento empleó rangos incompletos y terminó sin encontrar archivos ni ejecutar pruebas; se corrigió la sintaxis a línea/columna antes de la ejecución válida. No se presenta ese fallo de configuración como RED funcional.

La ejecución válida 53750 terminó con EXIT 0: **14/14 detectados**, 0 supervivientes y 0 sin cobertura, en 43 segundos. Incluye los seis supervivientes originales 61, 201, 229, 230, 316 y 319, además de otros ocho mutantes de esas líneas. Todos los huecos observables identificados quedaron detectados. Los 22 supervivientes originales restantes tienen justificación de equivalencia en la tabla. Sus informes separados están en `frontend/reports/mutation-project-states-replay/`.

