# Propuesta SDD14: iniciar una sesión de trabajo real

Fecha: 2026-09-06. Documento de propuesta para revisión del coordinador; no es contrato aprobado ni autorización de TDD. Feature13 continúa en revisión/integración y no se declara terminada. Feature14 permanece pending. Se aplican Ponytail full y Caveman lite, con prosa documental normal.

Fuentes: project-spec.md, propósito y modelo general, roadmap14–18 y contrato13; feature_list.json; features/reschedule.feature; docs/ux-requirements.md. El roadmap reserva14 para inicio real y una sesión activa;15 para pausas/tiempo neto,16 para cierre/avance,17 para aviso y ampliación deliberada,18 para historial global.

## Resultado propuesto

Desde una tarea propia elegible, la persona elige cuánto pretende trabajar y confirma «Empezar a trabajar». El servidor registra el inicio real una sola vez. La interfaz permite recuperar esa confirmación y consultar la sesión activa después de recargar, navegar, perder una respuesta o reiniciar el backend. No se empieza por abrir una pantalla, alcanzar la hora de un bloque ni recuperar acceso.

Una reserva, una estimación y una sesión son conceptos distintos. Iniciar no consume ni libera capacidad planificada, no modifica bloques/Today, no completa la tarea y no acredita minutos terminados, avance ni una racha. En14 sólo se acredita el hecho «empezó a trabajar a este instante». El cálculo de intervalos/tiempo neto y el cierre corresponden a15–16.

## Decisiones recomendadas para destilar el contrato

| Decisión | Recomendación y motivo |
| --- | --- |
| Contexto | Una tarea propia obligatoria, proyecto distinto de completed y tarea pending para un inicio nuevo, consistente con11/13. No forzar proyecto active si el contrato previo admite idea/paused. |
| Relación con planificación | No exigir bloque ni availability para trabajar. La primera entrada está en la tarea; Hoy ya permite llegar a ella. No añadir blockId opcional, vinculación retrospectiva o cambios en Block9/Today15 en14. |
| Intención mínima | Un entero plannedMinutes entre1 y1440, elegido explícitamente. Es duración prevista, no estimación de tarea ni límite al tiempo real. El máximo es una decisión nueva propuesta de ergonomía, no una regla heredada del trabajo realizado. No introducir un valor por defecto presentado como preferencia del usuario. |
| Hora de fin | Al confirmar, plannedEndAt = startedAt + plannedMinutes ×60s. Mostrar siempre esa hora con fecha/zona, incluido cruce de medianoche. Es un objetivo fijo, no un cierre ejecutado ni permiso para ampliar. El borrador puede explicar la duración; sólo la respuesta confirmada fija la hora exacta. |
| Alternativa descartada para14 | Pedir hora local final, zona y ocurrencia DST exigiría otra resolución temporal/preview. La duración real elegida genera un fin inequívoco y evita replicar11. Si el coordinador prioriza elegir una hora civil exacta, debe reemplazar esta decisión antes del Gherkin, no añadir ambos modos. |
| Reloj | Una lectura Clock del servidor, truncada a microsegundos, para startedAt y occurredAt. Nunca aceptar un inicio aportado por cliente ni interpretar un reenvío como empezar ahora. Validar rango de representación también al sumar plannedMinutes, sin desbordar el año9999. |
| Zona | Conservar como presentación histórica la zona de availability actual si es resoluble; si falta o no es resoluble, UTC explícito, reutilizando el criterio de12. No exigir configurar presupuesto para iniciar. Cambiar preferencias no cambia startedAt/plannedEndAt ni la zona ya registrada. |
| Exclusión | Como máximo una sesión activa por propietario, incluso entre proyectos, tareas, pestañas y dispositivos. Ausencia de availability no elimina esa garantía. Sesiones de propietarios distintos no comparten mutex global. |
| Activa frente a confirmada | La confirmación de inicio es un hecho inmutable. La consulta activa dice qué sesión sigue activa; en14 sólo existe running. No interpretar un recibo antiguo como vigencia perpetua cuando15–16 añadan transiciones. |
| Elegibilidad posterior | Completar tarea/proyecto después no borra ni cierra una sesión iniciada, ni impide recuperarla. No cambiar en14 los contratos de completar/reabrir. Pausa/cierre se revisarán en sus propias features. |

La intención no incluye texto de objetivo nuevo: la tarea y su criterio existentes dan contexto. No congelar nombres como hechos de avance ni publicarlos en el evento. La atribución histórica del día de trabajo al cerrar, definida en el modelo general, sigue pendiente de16; la zona capturada al inicio sólo etiqueta este inicio y su fin previsto.

## API y recuperación propuestas

Rutas tentativas, a fijar en el Gherkin:

- POST /api/v1/projects/{projectId}/tasks/{taskId}/work-sessions con JSON cerrado {plannedMinutes} e Idempotency-Key UUID canónica.
- GET /api/v1/work-sessions/active devuelve exactamente {session: SessionStart|null}; null sólo significa ausencia comprobada, nunca error ni datos de otro propietario.
- GET /api/v1/work-sessions/{id} recupera el inicio confirmado propio, sin depender del estado de tarea/proyecto ni del outbox.
- GET /api/v1/work-sessions/by-request/{requestKey} recupera el mismo hecho por key propia. La ruta literal no se confunde con un UUID.

SessionStart propuesto, cerrado: id, projectId, taskId, startedAt, plannedMinutes, plannedEndAt, zoneId. Los IDs son del servidor; sin owner/key en DTO. No incluir elapsedSeconds, netMinutes, endedAt, completed ni una revisión que14 todavía no necesita modificar. GET active puede reutilizar este hecho porque running es el único estado de14;15 deberá ampliar su representación de estado de forma explícita sin cambiar el recibo de inicio.

Primera confirmación201 después de commit, Location del recurso de inicio; replay200 con el mismo cuerpo y Location. El recibo debe sobrevivir a futuras pausas/cierre y a la retención del outbox. La propia fila de inicio puede ser el recibo: no copiarla en una tercera tabla ni fabricar un historial global.

La key es única por propietario dentro del espacio de inicios, distinto de creación11/cambios13. Intención normalizada = projectId + taskId + plannedMinutes; no incorpora reloj, zona elegida del servidor, cookies o CSRF. Reutilizarla con otra intención devuelve409 IDEMPOTENCY_CONFLICT sin presentar otro inicio como éxito.

Orden propuesto: seguridad y negociación heredadas; query; IDs; key; JSON/tipos/rangos; propiedad/existencia del contexto; replay; sesión ya activa; elegibilidad; captura de reloj/zona; escritura. No If-Match sobre una sesión aún inexistente. Una key confirmada se recupera antes de evaluar sesión activa, completed, preferencias o reloj actuales. La normalización previa al replay no consulta catálogo. El orden exacto de negociación415 y errores de forma debe remitir al contrato común, sin volver a prometer que el handler controla al framework.

Errores nuevos tentativos: WORK_SESSION_ALREADY_ACTIVE (409) y WORK_SESSION_NOT_FOUND (404 de id/key dentro del propietario). Para409 activo basta un sessionId propio recuperable, no datos de otra tarea que no se hayan autorizado. Mantener VALIDATION_ERROR, MALFORMED_JSON, IDEMPOTENCY_CONFLICT, STORAGE_UNAVAILABLE y seguridad comunes. Una lectura de sesión ajena responde el mismo404 que una inexistente. Los rechazos de contexto no se disfrazan de ausencia de recibo.

Red,503, JSON de éxito incompatible, códigos desconocidos e IDEMPOTENCY_CONFLICT conservan la intención y key inciertas. «Comprobar inicio» hace GET by-request. Un404 no prueba rollback de un POST todavía en vuelo: admite nueva comprobación o reenvío manual de exactamente la misma intención/key. No POST automático, key nueva automática ni «cancelar inicio» por cerrar el panel. Reutilizar renovación manual de SessionGate para CSRF y posterior reenvío separado.

Tras recarga sin key en memoria, GET active permite descubrir el inicio propio en14. No prometer que esto recupera una intención que nunca llegó ni un inicio ya cerrado en16. La pantalla global de hechos y sus filtros siguen siendo18; el recurso de inicio por ID/key conserva la recuperación técnica durable.

## Persistencia, concurrencia y EDA

Dominio puro: hecho de inicio y duración prevista; aplicación con puertos de iniciar/consultar, Clock y catálogo existentes; adaptador PostgreSQL y publicación outbox existente. Sin event sourcing, servicio de temporizadores, broker nuevo ni dependencias.

Migración aditiva posterior al cierre13. Guardar inicio/intención/key y outbox atómicamente. La representación física de «activa» debe ofrecer una restricción de unicidad PostgreSQL por owner; no confiar en SELECT vacío, memoria del proceso, bloqueo de botón ni fila availability inexistente. Una restricción parcial para running es una opción mínima; al introducir paused,15 tendrá que mantener la misma unicidad para todas las sesiones no cerradas. No implementar estados futuros o tablas auxiliares por anticipación.

Para inicios nuevos, locks de proyecto/tarea compatibles con las transiciones existentes y orden estable. La unicidad del propietario y la key deben resolver dos inicios concurrentes sin admitir dos sesiones ni convertir una colisión esperada en503. Mismo owner/key/intención:201+200 con un inicio/evento. Keys distintas: un201 y un409 de activa. Misma key/intenciones diferentes: un201 y un409 de idempotencia, aunque la sesión ganadora ya sea activa. Reconsultar el recibo ganador después de esperar o recuperar una colisión; no devolver un recibo de otra intención.

No compartir flags mutables de TransactionTemplate entre peticiones. Las lecturas públicas serán read-only, sin locks de escritura, y fallos SQL o al terminar la transacción devolverán503; GET nunca materializa una sesión ni sustituye fallo por null. La elección de aislamiento exacto se concretará según las consultas necesarias; no imponer RR a toda operación por copiar Today.

Evento propuesto WorkSessionStarted.v1: eventId, aggregateId=sessionId, ownerId, occurredAt=startedAt, schemaVersion=1, type, projectId, taskId, plannedMinutes, plannedEndAt, zoneId. Id del evento generado independientemente. Sin key, nombres ni criterio de finalización. Ruta work-session.started.v1 y cola quorum durable organization.work-session-started.v1, reutilizando confirms/retries/blocked. Publicar la zona histórica textual sin resolverla de nuevo. Fallo de broker no revierte un inicio local confirmado; no prometer entrega única ni consumidor nuevo.

Cada escritura debe afectar exactamente las filas esperadas. Supresión de inserción sin ganador es503 y rollback, no conflicto inventado. Fallo previo al commit revierte inicio/evento; fallo de respuesta posterior se recupera por key. El mismo inicio permanece tras reiniciar API y tras publicar/retirar outbox.

## Recorrido UI acotado

En tarea: contexto visible, campo «Duración prevista (minutos)», una acción «Empezar a trabajar», espera anunciada y confirmación «Sesión iniciada» con inicio/hora de fin prevista y zona. La sección «Sesión de trabajo» permite consultar la activa al montarse/volver, sin polling por segundo. Mostrar la duración elegida, no un contador que parezca tiempo neto acreditado.

Con activa propia: mostrarla y un enlace a su tarea; impedir otro inicio como ayuda UI, manteniendo la garantía del servidor. Mientras se consulta activa, error y ausencia son estados diferentes. Si el estado visible quedó obsoleto, el409 inicia una consulta deliberada o claramente anunciada de la activa, sin perder una intención todavía incierta. No hacer GET por fila de bloque.

Al superar la hora prevista, no cambiarla ni cerrar/pausar automáticamente. En14 permanece visible como «Fin previsto» sin aviso programado;17 añade el aviso y las decisiones explícitas de cierre/ampliación. Nada de rachas, premios por sobrepasar el objetivo ni penalización por descanso.

Guardas tras cada await, también clasificación de errores; abortar antes de propagar401 obsoleto.401 actual retira datos; cambio de ruta/contexto invalida resultados anteriores. Conservar el foco cuando el usuario eligió otro control; si el iniciador desaparece, trasladarlo al encabezado de la sesión. Reutilizar feedback de espera accesible y aria-disabled con guarda, sin spinner obligatorio. No añadir nuevos mecanismos de sesión autenticada o CSRF.

El contrato UI deberá aplicar las30 filas de docs/ux-requirements.md, matriz responsive/zoom/motores, teclado y áreas táctiles. Esta propuesta no afirma ninguna evidencia UX ejecutada. Especial atención a Parkinson (fin fijo), modelo mental (plan frente a trabajo), constancia sin culpa, recuperación, foco y feedback temprano; pausa/cierre de Fluir y siguiente paso de Zeigarnik quedan explícitamente pendientes15–16.

## Familias de escenarios candidatas, aún no Gherkin

1. Inicio nominal: reloj real microsegundos único, duración elegida y fin exacto; sin cambios de tarea, bloques, capacidad o métricas.
2. Inicio sin bloque/availability y con zona de preferencia no resoluble: UTC explícito; preferencia válida conserva su zona.
3. Cruce de medianoche/DST: duración entre instantes y fecha/zona mostradas coherentes, sin reinterpretar inicio histórico.
4. Extremos1/1440 previstos y suma fuera del rango temporal: límites definidos, no overflow ni inicio parcial.
5. JSON cerrado, tipos/rangos, query/IDs/key y seguridad heredada; un defecto por ejemplo salvo precedencia expresamente probada.
6. Contexto propio elegible frente a tarea/proyecto completed; propietario ajeno no descubre inicio o actividad.
7. Una activa bloquea segundo inicio en la misma tarea y en otra; otra cuenta puede iniciar independientemente, incluso sin disponibilidad.
8. Dos pestañas misma key/intención; keys distintas; misma key con duración o tarea distinta. Observar concurrencia real, no sólo requests consecutivos.
9. Completar tarea/proyecto antes y después del inicio: orden serializable y hecho previo recuperable, sin cierre implícito.
10. Replay antes de elegibilidad, actividad, catálogo y reloj actuales; mismas bytes/Location, cero nuevos hechos.
11. Pérdida de ACK y reinicio: comprobar key/activa, mismo inicio/evento;404 mientras POST sigue en vuelo no permite fabricar otra intención.
12. Guardado falla en cada escritura o commit: rollback; lecturas y fallo al finalizar read-only:503, nunca activa/ausencia inventada.
13. Broker caído/nack/confirmación incierta: política común, identidad preservada y recibo independiente de retención.
14. UI vacío/carga/error/inicio confirmado/activa; recuperar tras recarga, rechazar DTO incompatible sin falso éxito.
15. Privacidad por ruta/sesión/generación, renovación CSRF manual, foco durante envío/comprobación y controles externos.
16. Hora de fin visible y fija; alcanzar esa hora no amplía, cierra ni acredita trabajo; no confundirlo con el aviso17.

## Dependencias y frontera de entrega

Depende del cierre integrado de13, autenticación6, tareas7/9 y outbox2. Reutiliza zonas/fallback10–12 y recuperación11/13 sin cambiar sus DTO. No requiere que toda sesión provenga de un bloque.

Hay una limitación material del roadmap: una sesión iniciada en14 no puede terminarse desde el producto hasta16. No solucionarla introduciendo un cierre oculto, reset administrativo de datos o cierre automático fuera de contrato. Se recomienda desarrollar/verificar14 como incremento aislado y revisar la habilitación de uso habitual junto a15–16; no presentar14 por sí sola como ciclo de trabajo completo. El coordinador debe decidir esa frontera de entrega antes de exposición real, sin que este documento añada un feature flag ni modifique despliegue.

Pausa/reanudar e intervalos netos(15), cierre/avance/siguiente paso(16), aviso/ampliación(17), historial global/filtros(18), métricas semanales y edición de hechos quedan fuera. La persistencia de inicio no equivale a tener esas funciones.

Revisión requerida del coordinador: confirmar duración relativa frente a hora civil exacta; zona capturada con fallback; alcance task-only sin blockId; representación activa extensible sin contaminar recibo; frontera de habilitación mientras falta cierre. Son decisiones concretas propuestas, no nuevas preguntas al usuario ni aprobación implícita. Tras resolverlas se podrá destilar contrato propio y entonces TDD, una feature a la vez.

Sólo se crea este documento. No se ejecutaron suites, mutación, migraciones, Docker ni Git de escritura; no se modificaron fuentes, roadmap, estados ni contratos aprobados.
