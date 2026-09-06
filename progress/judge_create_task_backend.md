# Revisión backend — create_task

**Diseño y cobertura: favorables para continuar con mutación. Cierre de integración pendiente.** Revisión independiente por integration_craftsman, que no escribió producción backend de este corte. La revisión global y la de las herramientas E2E escritas por este agente corresponden al coordinador. Ponytail full y Caveman lite activos.

## Fuentes y contrato

Leídos los 35 escenarios, la sección 7 de project-spec.md, bitácora backend, Task/TaskPage/TaskPosition, puertos y casos de uso, TaskController, adaptadores PostgreSQL, migración V7, extensión OutboxMessage y selección de ruta RabbitBrokerPublisher. No se modifica el contrato de ocho campos públicos ni el estado, fechas o ETag de la representación del proyecto.

La entidad Task valida identidad, estado pending y fechas coherentes; normaliza sólo el título exterior, conserva criterio y cuenta puntos de código. El controlador exige JSON estricto, campos admitidos y tipos sin coerción. La estimación no crea registros de trabajo realizado.

TaskCommit bloquea la fila del proyecto propio dentro de la misma transacción, decide completed bajo ese bloqueo y comprueba que ambos INSERT confirmen exactamente una fila. No incorpora el bloqueo asesor global de capacidad. La carrera se verifica en ambos órdenes con PostgreSQL real, callbacks que mantienen el bloqueo adquirido y una segunda operación concurrente; terminar primero rechaza la creación y crear primero conserva una tarea pending al terminar después. Triggers reales cubren INSERT suprimido y fallo SQL en tasks/outbox con rollback y proyecto intacto.

Las consultas relacionan tasks con projects y restringen propietario y proyecto, además del id de tarea en detalle. La colección vacía exige primero existencia propia; un recurso ajeno, inexistente o asociado a otro proyecto produce el mismo RESOURCE_NOT_FOUND. Errores de lectura no se disfrazan de items vacío. El cursor conserva proyecto, fecha e id, rechaza campos adicionales/duplicados, exige base64url canónica y fecha representable en PostgreSQL; la consulta usa orden y corte de tupla descendentes, con un elemento adicional para detectar continuación.

El publicador incorpora sólo TaskCreated.v1 versión 1. Comprueba ocho campos exactos, taskId completo y título válido, sin criterio ni estimación. aggregateId sigue siendo el proyecto; taskId representa la tarea. La ruta task.created.v1 y cola organization.task-created.v1 conservan entrega obligatoria, persistencia y confirmaciones existentes. No se amplían recuperaciones o capas de transporte fuera del contrato.

## Hallazgo resuelto

TaskController.list decodificaba el cursor antes de validar de forma independiente el identificador de ruta. Una excepción por projectId mal formado quedaba atrapada y se convertía en un error cursor. Se comunicó al autor; `s12_pathErrorPrecedesCursorParsing` reprodujo RED y quedó GREEN tras validar projectId una vez antes de decode y reutilizar su UUID. No quedan otros hallazgos bloqueantes de esta revisión previa. Las invariantes de Task fueron señaladas y revisadas por el coordinador; no se atribuyen como hallazgo nuevo de este agente.

## Evidencia revisada

- TaskApiTest cubre contrato HTTP y PostgreSQL: campos/tipos/JSON, lectura propia, privacidad, 21 tareas con empates e inserción posterior, cursores adversos, identificadores, supresión/fallo de ambas escrituras, carrera, CSRF/origen, no-store, almacenamiento y ETag intacto.
- Pruebas de dominio/aplicación cubren Unicode, estimación, invariantes, creación/evento y paginación. La bitácora del autor registra 23 ciclos y mapa de escenarios; su corte focal informa 106 casos verdes.
- El primer E2E real de este agente pasó, 1/1 en 5,2 segundos: POST desde formulario, Location, ocho campos exactos, fila y recarga, estimación diferenciada, evento mínimo y proyecto/ETag intactos. Es evidencia del primer corte, no de la suite final.
- El autor informa TaskCreated confirmado en Rabbit real. La ejecución independiente de broker detenido/recuperación está preparada en el smoke; todavía no se presenta como ejecutada.

## Pendientes de cierre

El coordinador ejecuta regresión conjunta y autoriza mutación. La primera regresión backend encontró tres fallos de un fixture ApplicationContextRunner histórico; el autor corrigió el fixture y verificó sus siete casos, sin alterar producción. No se declara todavía verde global con resultados incompletos.

La reconstrucción intermedia coincidió con imports backend y una prueba TypeScript en escritura, por lo que abortó antes de reemplazar el fixture. Los resultados finales siguientes sustituyen los pendientes de integración; el perfil completo de mutación sigue pendiente. Este informe no aprueba cierre global ni sustituye esa puerta.

## Integración final verificada

El freeze conjunto permitió reconstruir una vez backend/web. La suite final dio 32/32 en 2,4 minutos; los dos fallos previos eran selectores ambiguos de confirmación y se corrigieron sin modificar producción. Los cinco casos nuevos confirman guardado/Location/recarga, proyecto y ETag intactos, 21 tareas con empates y alta posterior sin repetición, cierre/reapertura, privacidad con cuerpos 404 idénticos, errores recuperables y pérdida real de sesión. Firefox/WebKit repitieron el guardado y persistencia: 2/2 en 10 segundos.

El smoke independiente terminó EXIT 0: API 201 con broker detenido y worker habilitado; TaskCreated pendiente con reintento; recepción real del mensaje original al recuperar Rabbit. Después del mismo reinicio del broker con backend detenido, el eventId/payload siguió disponible en la cola quorum y la fila publicada permaneció idéntica. Created, Updated y StatusChanged históricos conservaron sus comprobaciones. No se repitió la matriz de crash ajena a los cambios de este corte.

El coordinador verificó init 74002: 484 backend y 331 frontend, lint y cero fallos/errores/omisiones en XML. Los fixtures de navegador y smoke se eliminaron exclusivamente dentro de sus proyectos propios. Matriz de 22 anchos, zoom nativo y 30 principios disponibles en ux_create_task.md; el caso reforzado midió 2 ms de feedback en una ejecución adicional verde.

## Mutación final y dictamen backend

Se inspeccionaron directamente ambos XML: el perfil completo contiene 182 KILLED y cuatro SURVIVED sobre 186, sin timeouts ni NO_COVERAGE. El replay contiene 15 KILLED, incluyendo los tres huecos observables: microsegundos de cursor y uso del host/vhost configurados. Los denominadores se mantienen separados; no se afirma un global recalculado.

El cuarto superviviente cambia null por cadena vacía en TaskController.string. Se revisaron sus dos usos y la entidad: title ausente/null y vacío producen el mismo REQUIRED; completionCriterion ausente/null y vacío terminan en la misma cadena. Es equivalente dentro del contrato, sin aceptar un tipo antes rechazado.

**Dictamen backend: APPROVED.** Fuente, comportamiento integrado y mutación revisados sin hallazgos backend abiertos. La revisión global del frontend y del nuevo caso de reintento concurrente corresponde al coordinador; esta aprobación no sustituye su cierre ni convierte resultados parciales de frontend en una suite final.
