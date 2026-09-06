# Revisión independiente del contrato13 — backend

Corte inicial bb6b4d/5d9f70: lectura completa de los40escenarios y contraste con contratos11/12 y sección13 aprobada para destilación. Rol judge readonly, Ponytail full/Caveman lite; sin suites ni fuentes/tests/contratos editados.

Estado: PENDING corte corregido solicitado por raíz; no autoriza TDD todavía.

Hallazgos concretos ya comunicados al autor/coordinador:
- Cabecera cuenta once campos de preview11@s1; son diez. ETag de bloque es cabecera y no amplía el cuerpo.
- @s8 usa AMBIGUOUS_LOCAL_TIME; la herencia11 exige AMBIGUOUS_OFFSET y validOffsets cerrado por campo.
- @s32 mezcla validación de Location POST con recuperación GET; precisar el ámbito POST conserva GET de recibo válido sin un header no contratado.
- Pendientes del coordinador: precisiones@s3/@s33/@s36, carrera de presupuesto@s22 y@s41 de dos cancelaciones de distintos bloques de misma tarea/misma key sin preferencia. No se duplican aquí como hallazgos nuevos.

Revisión de fondo sin objeción adicional en este corte:
- Creación immutable/by-request/replay11 se separan expresamente de estado vigente y cancelación404 porID; DTO9 y Today15 siguen cerrados.
- Precedencia existencia→replay→revisión→estado evita divulgar recibos ajenos y permite recuperar ACK tras cambios posteriores, reloj pasado o catálogo ausente.
- Disponibilidad no sustituye revisión del bloque; excluir sólo propioID conserva capacidad y solapes de otras reservas/owners. Recalcular después de lock protege cambios desde preview.
- Orden proyecto/tarea/preferencia/bloque y lock de fila original evita proyección ausente sin exclusión. Sin preferencia, el lock de bloque no serializa keys compartidas entre bloques: la nueva@s41 debe exigir conflicto de idempotencia y rollback completo del perdedor, no503/segundoéxito.
- Recibo durable separado del outbox conserva recuperación después de publicación/reinicio; proyección, recibo y evento comparten commit y rowcounts verificables.
- BlockChanged.v1 tiene payload cerrado y rutas explícitas; revision numérica backend, ETag textual HTTP y tiempo potencialmente no monótono están diferenciados.
- GET/preview sin escrituras, Today RR y herencia de formatos/seguridad siguen coherentes. No se exige repetir los325casos antiguos.

## Corte corregido41/156 — APPROVED contrato backend

Relectura0ed932 confirma diez campos y ETag header, AMBIGUOUS_OFFSET con validOffsets por campo, Location sólo POST y recuperación histórica sin sucesor imposible de cancelación. Se mantienen DTO9/Today15 y recibos originales intactos.
@s22 incorpora carrera real de presupuesto entre movimientos no solapados; @s41 exige única key/recibo/evento y rollback del perdedor sin preferencia, con409 en vez de503. Cubre la brecha de mutex entre dos bloques sin prescribir infraestructura nueva.
No quedan bloqueantes en herencia API11, locks, idempotencia, recibos/estado u outbox del alcance revisado. El estado PENDING anterior queda resuelto por este corte; APPROVED para la puerta de contrato del coordinador, no acredita implementación ni autoriza por sí solo TDD. No se ejecutaron suites ni se modificaron fuentes/tests/Gherkin.
