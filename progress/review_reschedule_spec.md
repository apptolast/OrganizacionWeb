# Revisión de especificación13

APPROVED para destilar Gherkin, bajo autorización global vigente. No acredita implementación ni cierre.

El coordinador revisó la propuesta de resume_frontend, la exploración independiente de resume_backend y las cuatro observaciones previas de resume_review. Evidencia de lectura:23a188,51100f,db3fa0,2473b0,d87518,11c865; diffcheck de propuesta68b5fe y precisión final232bff.

Se aceptan identidad estable, creación inmutable, proyección opcional y recibos de cambios compartidos con historial. Evitan duplicar el recibo de creación y conservan sus reintentos. Listado/detalle y Today pasan explícitamente a estado vigente; recuperación original permanece histórica. DTO9 y Today15 se mantienen cerrados.

Movimiento permite duración/zona, exige revisión y reevaluación de disponibilidad excluyendo sólo su reserva. Cancelación terminal permite liberar bloques pasados/completed/sin preferencia. Revisión BIGINT textual evita redondeos del navegador; una operación confirmada se recupera antes de revalidar negocio. Estado, recibo y evento se confirman juntos. No se introduce calendario, sesiones, edición masiva ni motor genérico.

Quedan fijadas precedencias: existencia antes de recibo de acción; replay antes de revisión vigente; revisión antes de cancelled; agotamiento antes de negocio, incluido preview; tiempo antes de unchanged. Historial usa continuación sólo con más de20 elementos. Clock no se supone monótono. La confirmación UI11 se identifica como hecho histórico y no se inyecta en lista vigente.

Ponytail full y Caveman lite aplicados: reutilización de validadores, formularios, reglas temporales y outbox; arquitectura hexagonal, seguridad, TDD y matriz UX conservados. Próxima fase: autor Gherkin independiente. Ningún código/test de13 está autorizado por este dictamen hasta revisar sus escenarios.

## Contrato final

APPROVED para TDD:41 escenarios y156 casos representativos. Revisión completa del coordinador257f72/6917e6/576e69 y correcciones verificadasefd63f/9fb610/ef7169. Dictamen independiente backend06a869 aprobado. Se corrigieron fixture temporal, aserciones sin DTO, salida del editor, cancelación terminal, Location sólo POST y conteo de preview. Se añadieron competencia de presupuesto y carrera de key compartida sin preferencia. No quedan observaciones bloqueantes. Las cifras describen contrato, no tests ejecutados. Autorización global del usuario aplicada sin repetir permiso.
