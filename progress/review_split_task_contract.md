# Revisión ejecutable del borrador split_task

Revisión documental de `progress/contract_split_task_draft.feature`, todavía `@draft`; no activa feature 8. Ponytail full y Caveman lite. La dirección es compatible con create_task: DTO de ocho campos, lista plana intacta, recurso de padre separado, hijos nuevos y navegación por un nivel cada vez. No necesita árbol completo, movimientos ni otra capa arquitectónica.

**Dictamen:** alcance coherente; antes de aprobar conviene separar fronteras actualmente unidas por «o» y fijar una referencia ejecutable a las tablas existentes. No hace falta copiar todas las tablas ni multiplicar casos de navegador.

## Referencia mínima reutilizable

La frase introductoria sobre reglas heredadas no garantiza que se ejecuten en la ruta nueva. Proponer una nota contractual explícita:

> Las tablas y aserciones de create_task s2–s8 se aplican íntegramente al POST de subtareas, sustituyendo su ruta y preparando un padre propio. La trazabilidad identificará cada fila heredada con escenario y entrada, y la prueba que ejecuta esa fila en el nuevo endpoint. Se mantienen estado HTTP, código público, código de campo, normalización y ausencia de escrituras ante rechazo. Se añade parentId como propiedad no admitida del cuerpo.

Esto cubre también positivos de 1/160/2000/1440, opcionales ausentes/null, Unicode y tipos, que s4 resume sólo como negativos. Permite reutilizar datos/helpers, pero no atribuir a una ruta nueva únicamente pruebas que siguen llamando a la antigua. El smoke de navegador puede seguir siendo uno por recorrido, sin reproducir todas las combinaciones HTTP.

## Ajustes concretos por escenario

| Escenario | Ambigüedad o mezcla | Mejora acotada |
| --- | --- | --- |
| s1 | «Ocho campos» y «Location de detalle» remiten implícitamente al corte previo. | Referenciar create_task s1 con Location `/api/v1/projects/{projectId}/tasks/{id}`, fechas iniciales iguales y defaults. Sustituir sólo el evento por SubtaskCreated; comprobar un único evento, sin TaskCreated adicional. |
| s1–s2 | Padre intacto está claro; proyecto/ETag intactos sólo aparecen explícitos ante fallo en s15. | Heredar la conservación del proyecto de create_task s13 para éxito y los estados abiertos idea/active/paused. No añadir versiones nuevas. |
| s4 | Filas «vacío o espacios», «fuera de rango», «id, status u ownerId» y «duplicadas o truncado» admiten ejecutar sólo una alternativa. | Sustituir esas filas resumidas por la referencia ejecutable s2–s8 anterior y una fila parentId. Si se conservan tablas locales, cada entrada concreta ocupa su propia fila. |
| s5 | «Creación o consulta» mezclada con CSRF permite un GET al que no corresponde ese requisito; ausente y vencida tampoco están separados. | Dos grupos: sesión ausente/vencida para POST hijos, GET hijos y GET padre; CSRF ausente/inválido y origen extranjero sólo para POST, con sesión válida. El caso de origen conserva CSRF válido para aislar la guarda. |
| s6–s7 | «Creo o consulto» no obliga a comprobar los tres recursos nuevos; «mismo mensaje» no fija un oráculo. | Enumerar operaciones nuevas y aplicarles cada recurso ajeno/inexistente pertinente. Comparar el cuerpo público 404 completo con un recurso inexistente de referencia; GET padre usa id de tarea, los otros usan parentId. No añadir una batería por cada combinación en E2E. |
| s8 | La conservación de la colección anterior puede quedar probada con una sola fila. | Afirmar que aparecen raíz y subtarea, con DTO8; que POST antiguo crea raíz y su TaskCreated histórico. Reutilizar la paginación histórica, sin repetir todos los límites. |
| s9 | La rama sin hijos está en el mismo escenario cuya precondición tiene dos hijos. | Separar dos datasets o Examples: sin hijos y dos hijos directos con nieto. El segundo excluye explícitamente al nieto. |
| s10 | La inserción más reciente aparece como resultado sin precisar cuándo ocurre. | Secuencia inequívoca: primera página, crear hijo más reciente, consultar cursor original; segunda página contiene el elemento original restante y nextCursor null. Comparar identidad/orden exactos. |
| s11 | Se mezclan vacío/duplicado/mal formado, extra/ausente, fecha/UUID; «duplicado» puede ser query repetida o clave JSON repetida. | Referenciar la tabla de cursor create_task s22 aplicada al formato de cuatro campos, distinguiendo query repetida y clave duplicada. Añadir por separado cursor de otro padre y de colección plana. Definir cuáles nombres son los cuatro campos y error cursor frente a query. |
| s12 | Proyecto, padre y tarea mal formados aparecen juntos; el recurso «correspondiente» no es preciso. | Tabla corta identificador/operación: projectId en recurso nuevo, parentId en POST/GET hijos, id en GET padre. Al menos un caso combina path inválido con cursor válido para comprobar precedencia. |
| s13 | Lectura completed y reapertura comparten el rechazo de creación. | Conservar un recorrido deliberado completed: POST 409, lecturas permitidas, reapertura confirmada y nuevo POST elegido por usuario. No inferir reenvío automático. |
| s14 | «Confirma primero» puede probarse secuencialmente y no demostrar carrera. | Especificar dos operaciones concurrentes con PostgreSQL real y orden controlado del bloqueo del proyecto; el segundo intento espera. Reutilizar fixture de carrera create_task, sin temporizadores arbitrarios. |
| s15 | «Falla o no confirma una fila» no exige ambos modos. | Examples con registro y modo: subtarea/excepción, subtarea/cero filas, evento/excepción, evento/cero filas. Relación persistida en la misma escritura no exige una tabla extra ni un tercer modo ficticio. |
| s16 | «Éxito o error» es abierto y mezcla GET hijos/padre. | Heredar no-store de create_task s24 para estados pertinentes de ambas lecturas; dos pruebas SQL de lectura fallida distinguen colección vacía de parent null. No es necesario repetir fallos globales de sesión ya compartidos. |
| s17 | «Se confirma con broker detenido» podría ser una precondición sembrada directamente, sin demostrar independencia de HTTP. | Explicitar worker habilitado, broker detenido, POST 201 y evento pendiente antes de recuperación. Después verificar identidad, esquema9, ruta/cola y recepción persistente. Reutilizar el smoke existente y su mismo reinicio, sin repetir crash completo. |
| s18 | Tipo/versión/campo/identificador/título agrupa cinco fronteras; hay dos identidades hijas. | Reutilizar tablas cerradas del publicador y enumerar tipo desconocido, versión desconocida, campo extra/ausente, taskId inválido, parentTaskId inválido y título inválido. Mantener UNSUPPORTED_EVENT frente a INVALID_EVENT y cero envío. |
| s19 | Los hijos tienen carga/error independientes, pero el fallo del recurso parent podría confundirse con una raíz. | Añadir resultado explícito: sólo `{parent:null}` confirmado significa raíz; fallo de lectura muestra error recuperable y no inventa ausencia de padre. El detalle propio sigue accesible por URL directa y recarga. |
| s21 | Validación, conflicto, SQL, red, pérdida de sesión y 201 seguido de GET fallido se mezclan. | Parametrizar 400, 409 PROJECT_COMPLETED, 503 y fallo de red para conservar borrador. Separar sesión 401 que lo elimina y 201 confirmado + GET fallido que conserva confirmación. El 409 sólo permite GET deliberado y una acción posterior elegida. |
| s22 | Doble envío, navegación y logout aparecen como un único When alternativo. | Tres casos identificables: doble envío retenido; respuesta antigua tras cambiar de tarea; respuesta antigua tras logout. Reutilizar guards existentes y verificar que nunca reaparece contenido del contexto anterior. |
| s23–s24 | Ya expresan criterios verificables, pero no son una prueba humana universal. | Mantener matriz de anchos/bordes, keyboard real, badInput y medición de feedback antes de liberar respuesta. La matriz de 30 principios debe seguir distinguiendo automatización, inspección visual y dispositivos físicos pendientes. |

## Trazabilidad sin ampliar el producto

El mapa final puede usar nombres como `s4 / create_task.s6 / estimatedMinutes=1441` y `s5 / GET padre / sesión vencida`. Cada referencia debe señalar una prueba que llama al recurso nuevo. Para escenarios de flujo, dividir pasos o datasets con nombres claros basta; no es necesario crear un archivo por fila ni repetir la matriz visual para cada error.

Quedan fuera de esta revisión producción, tests, activación y modificación del contrato. El coordinador conserva el borrador y decide la redacción final tras cerrar create_task.
