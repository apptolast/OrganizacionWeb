# Revisión del borrador — completar y reabrir tareas

Revisión documental de los 35 escenarios actuales, la propuesta y ambas revisiones previas. Ponytail full y Caveman lite. Diseño favorable con ajustes acotados antes de aprobar el contrato; feature 9 continúa sin activar. No se modifica producción ni el borrador.

## Decisiones que conviene conservar

DTO8 mantiene su forma y amplía status explícitamente en las cuatro lecturas. El recurso /status tiene tres campos y ETag de tarea; historial independiente de outbox, paginado por versión. Comparación de versión antes del no-op, fechas no decrecientes y transición/historia/evento atómicos son coherentes. La revisión backend explica el bloqueo NO KEY UPDATE para no invertir la dependencia con la creación de hijos; s28 debe demostrarlo, sin convertir la elección técnica del lock en una nueva API.

## Cambios mínimos de contrato

1. **s7: entradas y errores exactos.** Añadir columnas JSON, campo y código. Recomiendo lo siguiente, conservando el código público VALIDATION_ERROR salvo sintaxis y media type:

| Entrada concreta | Campo | Código de campo / resultado |
| --- | --- | --- |
| `{}` | status | REQUIRED |
| `{"status":null}` | status | REQUIRED |
| `{"status":1}`, `{"status":[]}`, `{"status":{}}`, `{"status":true}` — cuatro filas | status | INVALID_TYPE |
| `{"status":"done"}`, `{"status":""}`, `{"status":" pending "}`, `{"status":"COMPLETED"}` — cuatro filas | status | INVALID_VALUE; no trim ni conversión de caja |
| `{"status":"completed","extra":1}` | extra | UNKNOWN_FIELD |
| `[]`, `null`, `"pending"`, `1` — cuatro filas | body | INVALID_TYPE |
| JSON truncado, duplicado, concatenado o cuerpo vacío — cuatro filas concretas | sin errors de campo exigidos | 400 MALFORMED_JSON |
| JSON válido enviado como text/plain | sin errors de campo exigidos | 415 UNSUPPORTED_MEDIA_TYPE |

MALFORMED_JSON es una decisión explícita del recurso de tarea, distinta del handler histórico de estado de proyecto. No exigir un objeto errors ficticio para respuestas problem que no lo incluyen. La política missing/null REQUIRED y texto vacío INVALID_VALUE corresponde a un enum exacto, no a normalización de títulos.

2. **Precedencia acotada y verificable.** Cada fila s6/s7/s13/s14 debe declarar que el resto de la petición es válido; para privacidad, If-Match sintácticamente válido identifica la tarea de la ruta, aunque su versión sea antigua. No exigir 404 para una petición malformada. Conservar los filtros actuales de sesión/origen/CSRF, sin prometer un orden nuevo cuando coinciden varios defectos de seguridad. Dentro de las nuevas rutas, fijar: UUID de path antes de cursor o cuerpo/precondición; para PUT, precondición ausente/formato antes de analizar JSON; sintaxis JSON antes de forma/tipos; propiedad extra antes de status (si hay varias, primera en orden lexical). Tras validar la petición, resolver propiedad/existencia antes de comparar versión; comparar versión antes del no-op. Añadir sólo tres casos combinados decisivos: path inválido más cursor válido identifica path; recurso ajeno más versión antigua sigue 404; estado ya satisfecho con versión antigua sigue 412. Los dos últimos ya tienen escenarios cercanos que pueden precisarse. No crear una matriz cartesiana de defectos.

3. **s16 debe excluir una primera intención no-op.** Especificar tarea pending y dos PUT completed con el mismo ETag: el primer escritor confirma una transición, el segundo espera realmente y devuelve 412. Con «dos cambios» genéricos, uno podría pedir pending y no incrementar versión, contradiciendo el Then. s28 debe ejecutar ambos órdenes de adquisición, coordinados sobre PostgreSQL real y con esperas acotadas; ambos resultados siguen siendo padre completed e hijo pending. No se necesita otro sistema de locks ni prueba de estrés.

4. **Independencia del proyecto en ambos sentidos.** s8 sólo prueba completar. Convertirlo en tabla con estado inicial/destino o añadir reapertura desde completed bajo proyecto completed. La propuesta permite ambas operaciones; impedir sólo la reapertura sería un hueco observable. s27 sigue permitiendo nuevos hijos de padre completed únicamente con proyecto abierto.

5. **Confirmaciones y privacidad de UI.** Ampliar s23 con PUT HTTP 200 cuyo cuerpo es inválido o cuyo ETag falta/no corresponde: resultado incierto, sin aplicar éxito ni repetir PUT, con consulta deliberada. s34 debe incluir lectura de status 401/404 y diferenciarla de 503: retirar detalle/estado/historia privados, no conservar una tarjeta anterior junto a un error. s24 debe nombrar status e history como lecturas pendientes, sin permitir cubrir sólo una. s35 debe afirmar que el estado visible del detalle y su control quedan alineados tras el PUT; no sólo el snapshot interno. s25 ya separa acertadamente fallo de historial de transición confirmada.

6. **Trazabilidad sin inflar escenarios.** En s13, «clave ausente», «clave duplicada» y «raíz no objeto» deben señalar todas las claves/tipos a ejecutar o enlazar una tabla cerrada; no elegir una variante representativa. El cursor sigue opaco en cliente, evitando convertir BIGINT en Number. s31 debe incluir 415 para PUT, ya exigido por s7. s20 debe precisar aggregateId del proyecto, taskId de la tarea y occurredAt igual al instante confirmado en historia/updatedAt, como ya decide la propuesta.

## Observaciones documentales

La propuesta todavía informa 28 escenarios/61 casos de su primer parser; identificar ese recuento como histórico y añadir el recuento final sólo después del próximo parseo. Los treinta principios UX deben distinguir implementación del control frente a aspectos futuros de planificación, sesiones y progreso medido; su ausencia no prueba cumplimiento. No hace falta añadir edición de contenido, borrado, propagación de estados ni árbol completo a este corte.

No se ejecutaron pruebas ni se activó feature 9. Los cambios anteriores son recomendaciones concretas para que el coordinador cierre el contrato.
## Dictamen del corte final de 36 escenarios

**Favorable para cerrar el contrato documental.** Esta lectura sustituye la lista anterior de huecos como estado vigente; aquella queda conservada como revisión del borrador de 35 escenarios. Se comprobaron textualmente 36 tags únicos. No se ejecutó parser, pruebas ni implementación de feature 9.

Los cambios pedidos están incorporados: campos/códigos de s7; precondición antes de JSON y propiedad antes de versión; ocho combinaciones de completar/reabrir bajo estados del proyecto; dos completed concurrentes desde pending; ambos órdenes de la carrera con creación de hijo; respuestas 200 inválidas en s23; lecturas status/history diferenciadas en s24; identidad/instante del evento; 415 no-store; retirada privada separada en s36. No quedan contradicciones funcionales detectadas entre esos escenarios y las propuestas.

En particular, s29 define 20 resultados de 21 sin exigir versiones consecutivas; s30 preserva error de path antes del cursor; s31 exige cada combinación HTTP, no una respuesta representativa; s32 mantiene esquema cerrado y las cinco rutas anteriores; s33 ordena por versión aunque la fecha se repita o retroceda; s34 impide acciones sin revisión válida; s35 protege el estado confirmado frente al GET anterior; s36 elimina detalle/control/historia y no los resucita durante reintento. Son fronteras distintas y no conviene fusionarlas para reducir artificialmente el número de escenarios.

Quedan tres precisiones pequeñas de trazabilidad, sin cambiar el diseño ni añadir una feature:

- En s32, indicar que «campo requerido ausente» ejecuta cada uno de los nueve campos, o referenciar una tabla cerrada de validación común más taskId/fromStatus/toStatus. Así se evita cubrir sólo una ausencia representativa. La misma regla general ya está explícita para las claves del cursor s13.
- El texto de precedencia pertenece a solicitudes con media type admitido después de los filtros existentes. HTTP 415 se resuelve por selección de endpoint antes de entrar al handler; no prometer que un path mal formado gane también frente a un Content-Type inválido. Basta una salvedad en la cabecera, manteniendo el resto válido en cada rechazo como ya indica el borrador. No es necesario cambiar filtros ni crear una matriz de errores combinados.
- Antes de publicar la especificación final, trasladar la retención indefinida del historial desde la propuesta y actualizar el recuento de la propuesta: sus 28 escenarios/61 casos corresponden a la primera validación histórica, no al corte actual de 36. Una eventual limpieza del outbox no debe convertirse en política de retención del producto.

Las entradas descriptivas de las tablas pueden mapearse a fixtures concretos durante TDD, conservando todas las variantes indicadas y sus códigos; no requieren repetir ahora todas las tablas JSON del corte anterior. La revisión es documental y no demuestra aún locks, recuperación, accesibilidad ni comportamiento del servidor. El coordinador decide la aprobación y activación futura después de cerrar split_task.