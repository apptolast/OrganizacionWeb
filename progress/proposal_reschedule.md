# Propuesta13: replanificar sin perder hechos ni recuperación

Corte leído: feature12done e9269db, init78050 vigente (1354frontend/17scripts/lint;1415backend sin cambios). Rol spec_partner; Ponytail full y Caveman lite. Autorización global del usuario vigente. Esta tarea sólo amplía documentación y no repite suites, Gherkin, TDD o autorización humana. Propuesta normativa en la sección13 de project-spec.md, pendiente de revisión final del coordinador.

## Evidencia de implementación usada

- V11__planned_blocks.sql y PostgresBlockStore: planned_blocks contiene la petición normalizada, key y DTO de creación. Tanto replay como by-request leen esa fila. Un UPDATE del horario rompe recuperación original; DELETE permite perderla y duplicar una creación tardía.
- PlanBlock/ResolvedBlockTime/BlockBudget: reutilizar reglas temporales, ocurrencias DST y presupuesto por intersección. Excluir sólo el blockId movido antes de resolver solape y sumar capacidad; no restar duración sin distinguir días.
- BlockPlanning/BlockCommit: callbacks dentro de transacción ya mantienen lectura/evaluación/escritura. El nuevo caso de uso puede seguir ese patrón; no encadenar create y cancel en transacciones distintas ni añadir un motor de eventos.
- PostgresAvailabilityStore: FOR UPDATE del propietario coordina reservas y cambios de preferencia. Availability-Revision sólo representa preferencia, nunca versión del bloque.
- PostgresTodayQueries: SQL actual consulta directamente planned_blocks. Debe consultar proyección vigente y filtrar cancelados dentro del mismo snapshot RR; adaptar también ownerBlocks/list/detail para que ninguna ruta siga contando un tiempo liberado.
- TaskStatusController: precedente de estado separado y ETag fuerte ligado a entidad. La nueva revisión mantiene BIGINT y representación textual en cliente, sin Number inseguro.
- RabbitBrokerPublisher y OutboxMessage: siete tipos/rutas cerrados actuales. BlockChanged.v1 requiere incorporarse al validador y ruta/cola con pruebas; un INSERT de un tipo no reconocido quedaría blocked.
- TaskBlocks y schedule-block-api: DTO9 cerrado, recuperación key/petición y guardas tras awaits. Reutilizar sus validadores sin añadir campos a Block ni al DTO15 de Hoy. El nuevo envelope distingue recibo histórico y estado actual.

La exploración independiente progress/research_reschedule_backend.md confirma estas fronteras. No se leyó el documento protegido proposal_schedule_block_time.md ni otros destinos protegidos.

## Alternativas y elección propuesta

|Decisión|Opciones consideradas|Elección y motivo|
|---|---|---|
|Identidad|Crear otro bloque y cancelar el anterior; conservar ID y revisar su proyección.|ID estable: mantiene referencias y trazabilidad sin fingir una creación. Crear otro ID queda como acción explícita futura si se quiere otra reserva.|
|Persistencia|UPDATE de planned_blocks y copiar recibo original; planned_blocks inmutable más proyección opcional y registro de operaciones.|Segunda: la creación ya es un recibo inmutable, evita tercer duplicado/backfill y mantiene replay11. Historial y deduplicación usan la misma tabla de cambios.|
|Estado/versión|Añadir campos a DTO9; recurso state con Block9+status+updatedAt y ETag.|Recurso separado preserva validadores11/12. Los GET no materializan proyección ausente.|
|Horario editable|Sólo trasladar inicio conservando duración/zona; editar comienzo/fin/zona dentro del formulario existente.|Segunda aceptada preliminarmente por root: permite ajustar un bloque manual sin introducir resize/arrastre ni editar objetivo/tarea.|
|Pasado y completed|Prohibir cualquier cambio; cancelar libremente pero mover sólo a futuro y en tarea/proyecto elegibles.|Segunda aceptada preliminarmente: liberar una reserva no acredita ni borra trabajo. Mover no evade política de nuevas reservas.|
|Cancelación|Borrar/restaurar; estado terminal trazable.|Terminal aceptado preliminarmente: conserva hechos y recibos, sin feature extra de undo. Otra key sobre cancelled produce conflicto, misma key recupera éxito.|
|No-op|Guardar revisión/evento aunque horario idéntico; rechazar BLOCK_UNCHANGED.|Rechazo: no fabrica hecho ni permiso nuevo. Cambiar zoneId sí cambia planificación aunque instantes coincidan.|
|Recuperación|Devolver proyección actual con cada replay; devolver recibo histórico e inspeccionar estado aparte.|Histórico: una operación confirmada debe seguir siendo comprobable tras otras operaciones. UI no presenta ese snapshot como vigente sin leer state.|
|Historial|Pantalla global; sección de cambios paginada por tarea.|Segunda aceptada preliminarmente: reutiliza detalle, permite descubrir cancelados tras reload, sin calendario/historial global.|
|Eventos|Dos eventos/tipos casi iguales; un BlockChanged.v1 con kind y antes/después temporales.|Evento común aceptado preliminarmente: una familia trazable, tipo cerrado, revisión y payload mínimo sin objetivo/key. No implica consumidores nuevos.|
|Respuesta operación|200 siempre o204;201 al crear recibo y200 en replay.|201/200 aceptado preliminarmente, con Location recuperable y mismo recibo durable. No semántica de recurso borrado invisible.|
|Conflicto concurrente|Última escritura gana; If-Match y conflicto explícito.|If-Match: evita sobrescribir un movimiento/cancelación ajeno. Revisión diferente precede a cancelled; replay confirmado precede a ambos.|
|Ausencia de preferencia al cancelar|Exigir configurarla; permitir retirada sin reservar capacidad.|Permitir: la cancelación no añade ocupación. Se bloquea disponibilidad si existe y siempre la fila original del bloque; no se afirma que SELECT vacío bloquee al propietario.|

## Precisiones críticas para la revisión

1. **Compatibilidad explícita:** list/detail representan reservas vigentes; list omite cancelled y detail usa404 para cancelled. By-request de creación representa creación original. El nuevo state/historial recupera cancelados. Es un cambio semántico deliberado de13 sin ampliar DTO9, no un comportamiento histórico atribuido a11.
2. **Headers frente a replay:** comprobar estructura/canonicalidad de If-Match, Availability-Revision e intención antes de ownership/replay; no comparar su vigencia ni resolver catálogo/DST/reloj antes de buscar recibo. Una key con intención distinta sigue conflicto incluso si ambos planes acabaron con los mismos instantes. IDs de recurso/tipo también integran intención.
3. **BIGINT:** revisión textual en ETag y recibo HTTP. No introducir Number para validación/comparación; BigInt si hay aritmética. El evento backend puede conservar entero BIGINT porque no es un DTO consumido por el navegador.
4. **Mutex ausente:** cancelar sin disponibilidad no puede añadir solape ni exceso. Un create concurrente sólo puede empezar tras configurar preferencia y conserva su propia evaluación; si vio el bloque antes de cancelarse puede rechazar conservadoramente, equivalente al orden create-antes-cancel. No prometer aceptación de una reserva basándose en la cancelación aún no confirmada. Carreras de misma key en bloques distintos sin fila de disponibilidad requieren deduplicación durable y rollback de cualquier proyección perdedora, no convertir un UNIQUE conflict en503 genérico ni dejar cambios parciales.
5. **Snapshot en lecturas:** todos los queries de ocupación deben considerar la misma proyección planned. Cancelar no cambia task/project completed; mover no cambia createdAt ni orden de paginación. Today no necesita eventos locales, polling o lógica de replanificación propia; al volver de tarea se remonta/lee según12.
6. **Presentación histórica:** si se recupera éxito antiguo y falla state, la operación está confirmada pero la vigencia es desconocida. No volver a meter el before/after antiguo en la lista activa. Las rutas de cambios literales deben registrarse antes del matcher blockId.
7. **Formulario y zonas:** Block9 no ofrece campos locales históricos. Formatear desde instantes con zona explícita sólo cuando Intl la soporte; si no, mostrar UTC y pedir zona/horas deliberadas para mover. Nunca convertir silenciosamente con zona del navegador. La revisión del servidor decide ocurrencias y muestra antes/después, incluso si los catálogos difieren.

## Huecos que no deben quedar implícitos en Gherkin

No se pide permiso adicional al usuario. El coordinador debe revisar el contrato completo y confirmar especialmente los cambios list/detail tras cancelación, preview con If-Match, nombres/forma de códigos nuevos, recuperación task-scoped y ausencia de preferencia durante cancelación. Los endpoints y schemas propuestos ya están concretos en project-spec.md; estos puntos no autorizan producir una implementación por interpretación libre.

La destilación debe fijar ejemplos mínimos para revisión máxima BIGINT, misma key en bloques/tipos diferentes, replay tras cambio posterior, cancelación de bloque pasado/completed, estado inicialmente ausente, fechas/zonas distintas y propia reserva excluida. Reusar restricciones temporales/JSON/seguridad11 y refresco12 por referencia; no duplicar325 ejemplos ni variantes de mutación. Decisiones físicas de índices deben seguir queries/integridad reales, sin optimización especulativa.

No se modificaron sources/tests/config ni estados de feature. No se ejecutaron suites, broker, Docker, CI ni despliegue. La evidencia de12 sigue siendo histórica y no acredita13. Este documento es entrega para revisión, no cierre de feature.

## Cierre de observaciones del coordinador y prereview

Se incorpora el orden confirmado: existencia del bloque antes de buscar recibo de acción; replay antes de vigencia; revisión antes de cancelled; agotamiento después de revisión/estado y antes de negocio, también en preview; tiempo inválido antes de BLOCK_UNCHANGED, y ausencia de cambio antes de solape/presupuesto. El mismo intervalo original ya pasado se rechaza como tiempo pasado. Historial usa lookahead21 y nextCursor=null en terminal de20, con UUIDs canónicos y fechas heredadas11.

Precisada la UI existente: la confirmación de creación muestra su DTO histórico y recarga la lista; no lo inyecta en ella. readBlock sólo pertenece a BlockConflict. Se conserva by-request/replay y comparación con preview original, etiquetando el artículo como hecho confirmado y consultando state antes de afirmar vigencia/habilitar acción. No se cambia el protocolo de recuperación11 para añadir este comportamiento.

Propuesta lista para revisión final del coordinador; no se ejecutó ni redactó Gherkin. Los puntos anteriores sustituyen sus menciones como huecos abiertos: no queda una pregunta operativa bloqueante de esas familias.

Precisión final del reloj: occurredAt/updatedAt capturan Clock sin prometer monotonía respecto a createdAt o cambios anteriores. Revisión ordena los cambios efectivos; historial occurredAt/id ordena fechas registradas, sin afirmar secuencia causal. No añadir guardas temporales monotónicas en validadores.
