# Revisión independiente de mutantes — validación y API de tareas

Ámbito exclusivo: supervivientes de `src/task-validation.ts` y `src/tasks-api.ts` en `frontend/reports/mutation-create-task/mutation.json`, corrida original de 402/504. Son 21 IDs: nueve de validación y doce de API. Se revisaron fuente, contrato create_task, cliente HTTP compartido y pruebas existentes. No se modifica producción ni tests. Ponytail full y Caveman lite: cubrir entradas observables, sin ampliar arquitectura ni relajar umbrales.

## Corrección de la primera revisión

El coordinador detectó dos identidades cruzadas en este informe. Mi primera revisión atribuyó a 304 la guarda de objeto y declaró 323 equivalente por la guarda `items`; ambas atribuciones eran incorrectas. No se había alineado correctamente la ubicación con la fuente original del reporte. La corrección se realiza leyendo `files[*].source`, location, replacement y status del JSON original preservado, no usando números de línea de un archivo posteriormente formateado.

En `mutation-original.json`, **304** está en tasks-api.ts línea 80: `response.status !== 200`, sustituido por false. **323** está en línea 83: `typeof data !== "object"`, sustituido por false. Ambos eran Survived. El reporte `mutation-incremental.json` confirma después **323 Killed** y **304 Survived**. Por tanto retiro expresamente la equivalencia de 323. La guarda `items` no corresponde a ninguno de estos dos IDs. Las tablas siguientes están corregidas; el recuento pasa de 18/3 a **19 no equivalentes y dos equivalentes**.

## Huecos que deben cubrirse

| IDs | Diferencia observable | Prueba mínima recomendada |
| --- | --- | --- |
| 125, 128 | Quitar un ancla elimina también espacios interiores al contar título. Un título de 161 puntos formado por 80 A, un espacio y 80 B pasa indebidamente como 160. | Validación rechaza ese título y conserva el espacio interior en otro válido. Incluir White_Space Unicode, conforme s2–s4. |
| 126 | Quitar `+` del extremo inicial deja espacios exteriores adicionales y rechaza un título válido de 160 puntos. | Dos espacios iniciales seguidos de 160 puntos son válidos; no se cambia el contenido interior. |
| 129 | El extremo final elimina sólo un espacio y rechaza el límite válido. | 160 puntos seguidos de dos espacios exteriores son válidos. |
| 136 | Rechaza longitud exactamente uno. | `validateTask` acepta un título de un punto; también puede comprobarse con emoji. |
| 145 | Rechaza criterio exactamente 2000. | Aceptar 2000 puntos, conservando Unicode y sin recorte. |
| 162 | Rechaza estimación exactamente uno. | Estimación textual `1` no produce error. |
| 164 | Elimina el límite superior de estimación. | `1441` produce error y el formulario no envía POST. No confiar en max nativo: el formulario usa noValidate. |
| 165 | Rechaza estimación exactamente 1440. | `1440` no produce error. |
| 210 | Quitar typeof id permite que RegExp convierta un array con un UUID válido en una cadena válida. | Respuesta con `id: [uuid]` se rechaza; una comprobación sólo con `id: "bad"` no cubre esta coerción. |
| 213, 214 | Sin ancla inicial/final se admiten UUID con prefijo/sufijo. | Respuestas con `"x" + uuid` y `uuid + "x"` se rechazan, preservando el UUID válido. |
| 272, 275 | Date.parse admite valores que no son string; `0` produce una fecha finita. La creación puede ocultar el hueco por la igualdad adicional de fechas. | En readTasks, rechazar una fila con createdAt numérico y otra con updatedAt numérico, manteniendo la otra fecha válida. |
| 283, 284 | Se pierde o vacía Content-Type de POST. Los mocks de fetch siguen devolviendo 201, mientras la API real exige application/json. | Inspeccionar Headers del POST y cuerpo JSON exacto; conservar la prueba de API real 415. No hace falta repetir una batería de navegador. |
| 304 | Eliminar el rechazo de status distinto de 200 permite interpretar como página confirmada un cuerpo con forma válida aunque HTTP sea 201 o 503. Los cuerpos habituales de error ocultan el hueco porque las guardas de forma los rechazan después. | readTasks recibe Response.json({items: [], nextCursor: null}, {status: 503}) y rechaza con la misma Response; añadir 201 si se parametrizan estados. No basta un problem JSON ni comprobar sólo que la función rechaza por cualquier motivo. |
| 323 | Sin guarda de objeto, JSON `"ab"` pasa el conteo de dos claves y `"items" in data` lanza TypeError en vez del error controlado de respuesta inválida. No acepta datos privados, pero altera la frontera del módulo. | readTasks con Response.json("ab") rechaza con «Respuesta de tareas inválida». No equivalente; ya figura Killed en el reporte incremental revisado. |
| 349 | Sin typeof string, un array no vacío tiene length positivo y se acepta como nextCursor. | Rechazar `{items: [], nextCursor: ["opaque"]}`; conservar cadena opaca válida y null. |

Los límites positivos de tasks-api.test no matan los límites de validateTask: son funciones distintas. La comprobación puede agruparse en tablas pequeñas, con aserción explícita de los campos de error y de los valores aceptados. No conviene añadir casos que sólo afirmen que algo lanza una excepción.

## Equivalencias justificadas

| ID | Justificación y frontera |
| --- | --- |
| 175 | Eliminar `typeof value !== "object"` de isTask no permite una tarea inválida procedente de JSON: null sigue rechazado; booleanos/números tienen cero claves; cadenas con longitud distinta de ocho fallan el conteo y las de ocho fallan `typeof data.id === "string"`; arrays sin id fallan igual. Objetos siguen pasando exactamente las guardas restantes. undefined no es un resultado JSON válido. Se mantiene la guarda defensiva en producción. |
| 260 | `Number.isInteger` ya devuelve false para cualquier valor que no sea number, sin coerción. Quitar únicamente typeof number no cambia los valores aceptados, incluso fuera del dominio JSON. |

Total corregido: **19 IDs no equivalentes con huecos observables y dos equivalentes, 175 y 260**. La aprobación final requiere nueva ejecución y revisión de supervivientes; esta clasificación no altera el resultado original ni acredita pruebas todavía no escritas.

## Revalidación de los 21 IDs contra fuente original

| Archivo / línea original | IDs | Fragmento original y cambio comprobado |
| --- | --- | --- |
| task-validation.ts:13 | 125, 126, 128, 129 | Regex de White_Space exterior: quitar ancla inicial, quitar cuantificador inicial, quitar ancla final y quitar cuantificador final, respectivamente. |
| task-validation.ts:15 | 136 | `length < 1` pasa a `length <= 1`. |
| task-validation.ts:16 | 145 | Longitud del criterio `> 2000` pasa a `>= 2000`. |
| task-validation.ts:20 | 162 | `Number(estimate) < 1` pasa a `<= 1`. |
| task-validation.ts:21 | 164, 165 | `Number(estimate) > 1440` pasa a false o `>= 1440`. |
| tasks-api.ts:15 | 175 | `typeof value !== "object"` pasa a false. |
| tasks-api.ts:22 | 210 | `typeof data.id === "string"` pasa a true. |
| tasks-api.ts:23 | 213, 214 | Regex UUID pierde ancla inicial o final. |
| tasks-api.ts:33 | 260 | `typeof data.estimatedMinutes === "number"` pasa a true. |
| tasks-api.ts:38 / 40 | 272, 275 | typeof string de createdAt / updatedAt pasa a true. |
| tasks-api.ts:57 | 283, 284 | Headers pasa a objeto vacío o application/json a cadena vacía. |
| tasks-api.ts:80 | 304 | `response.status !== 200` pasa a false. |
| tasks-api.ts:83 | 323 | `typeof data !== "object"` pasa a false. |
| tasks-api.ts:93 | 349 | `typeof data.nextCursor === "string"` pasa a true. |

Los otros 19 IDs conservan la clasificación previa tras esta comprobación de identidad. La corrección afecta a la asociación de 304, al dictamen de 323 y al recuento; queda registrada en vez de presentar la revisión inicial como correcta.

Comprobaciones aisladas de semántica JavaScript, sin modificar archivos: RegExp acepta `[uuid]` por coerción; Date.parse(0) es finito; un array de cursor tiene length positivo; `in` sobre "ab" lanza TypeError. Las longitudes original/mutadas confirman los testigos de 125/126/128/129. No son una ejecución de Stryker ni sustituyen los tests del módulo.
