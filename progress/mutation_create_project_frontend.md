# Mutación de create_project — frontend

Fecha: 2026-09-05. Estado: medición final verde, pendiente de juicio independiente del coordinador. No declara feature done.

## Resultado reproducible

- Comando en frontend/: `pnpm mutate` (StrykerJS 10.0.0, runner Vitest 4.1.10).
- Source scope exacto: `frontend/src/projects-api.ts` completo y `frontend/src/use-create-project.ts` completo.
- 38 tests en 3 archivos; initial dry run correcto.
- 148 mutantes: **143 killed, 5 survived, 0 timeout, 0 no coverage, 0 errors**.
- Score sin ajustes: **143 / 148 = 96.62%**, superior al umbral 80.
- API: 92 / 94 = 97.87%. Hook: 51 / 54 = 94.44%.
- Duración: 5 minutos 23 segundos. Exit code 0.
- Informes completos reproducibles: frontend/reports/mutation/mutation.json y mutation.html (generados, no se suben al repositorio).
- No exclusiones de mutadores, líneas, comentarios Stryker ni cambios de umbral.

JSX declarativo de App.tsx, entrada main.tsx y SCSS no entran en este score de lógica: se verifican con tests de comportamiento de App y Playwright/axe real. El score no se presenta como mutación de todo el código de presentación.

## Los cinco sobrevivientes

No quedan huecos observables conocidos dentro del contrato y formulario de este corte. Cada sobreviviente conserva el comportamiento observable bajo esas condiciones; se mantienen en el denominador, sin falsear el resultado.

| ID | Archivo/línea | Mutación | Clasificación y justificación |
| --- | --- | --- | --- |
| 6 | projects-api.ts:19 | condición typeof object → true, conserva value !== null | Equivalente en el dominio JSON de este adaptador. Para objetos/arrays el resultado no cambia. Para strings, booleanos o números JSON, leer los campos requeridos devuelve undefined y los guards siguientes de tipo/estado rechazan la representación o usan fallback. null sigue rechazado. Funciones, símbolos y objetos con getters no proceden de response.json(). Los tests incluyen valores escalares, null, tipos incorrectos y errores de campo mixtos. |
| 59 | projects-api.ts:55 | catch devuelve undefined en lugar de null | Equivalente: ambos valores son rechazados por isRecord, generan serviceFailure y errors vacío. No se expone ni almacena el valor intermediario. |
| 98 | use-create-project.ts:8 | errors inicial [] → ["Stryker was here"] | Equivalente observable: el array es interno; sus únicos consumidores buscan field === name/description y leen message. La cadena no tiene esos campos, por lo que ambos errores derivados siguen undefined. La siguiente petición reemplaza ese estado. Se mantienen los tests de no alerta inicial y aria-invalid false. |
| 130 | use-create-project.ts:29 | limpiar errors con [] → ["Stryker was here"] | Equivalente por el mismo mapeo interno. Durante espera desaparecen todos los errores derivados y la respuesta reemplaza la lista; ningún consumidor renderiza ni devuelve el array. |
| 146 | use-create-project.ts:38 | fallback field vacío → "Stryker was here!" | Equivalente en el formulario existente, cuyos únicos campos se llaman name y description. Ninguno de los dos nombres de fallback identifica un elemento; namedItem devuelve null y no se cambia el foco. No hay campos personalizables en este corte. |

La equivalencia de los tres mutantes del hook debe reconsiderarse si se expone el array de errores, se añaden controles dinámicos o se permite personalizar sus nombres.

## Huecos reales cerrados durante la validación

Tests añadidos o fortalecidos uno a uno: evitar alerta inicial falsa; impedir errores cruzados entre nombre y descripción; cancelar navegación por submit nativo; clasificar exactamente fallo de transporte; fallback ante títulos no textuales o vacíos; quitar confirmación anterior; tolerar respuesta tras desmontaje; restaurar foco perdido al deshabilitar sin robar foco al usuario. La restauración de foco se implementó tras rojo en Chromium y test local equivalente.

## Incidencia de herramientas resuelta

El descubrimiento implícito de plugins bajo pnpm no encontraba el runner; se declaró explícitamente @stryker-mutator/vitest-runner. La primera medición con Vitest5 produjo 53.38% y clasificó como sobrevivientes defectos que tests existentes detectaban: los tests anidados no se seleccionaban correctamente. Se fijó Vitest4.1.10, versión usada por el propio paquete Stryker10 para desarrollar su runner. La medición posterior mató esos defectos y obtuvo 90.23%; después de cerrar huecos y corregir foco se obtuvo el resultado final de este informe. No se parchearon node_modules ni se ignoraron errores.
