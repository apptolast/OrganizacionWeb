# Revisión de la especificación de inicio de trabajo

Aprobada para destilar Gherkin. Root0da14f revisó la sección normativa14 y
aceptó duración relativa, fin fijo, recibo inmutable, única activa por propietario,
zona histórica con fallback y separación del ciclo15–18. La aprobación global
del usuario sigue vigente.

El conflicto409 WORK_SESSION_TIME_OUT_OF_RANGE describe una imposibilidad de
representar el reloj/fin; no atribuye a almacenamiento ni al campo editable
un error que no corresponde. La precisión14181d aplica query cerrado a las
cuatro rutas y fija seguridad antes de query e ID/key en GET.

Unicidad PostgreSQL y reconsulta fuera de una transacción abortada evitan una
tabla/mutex global especulativo. Los futuros estados deben conservar unicidad
de sesiones no cerradas e inmutabilidad del inicio. No se habilita para uso
habitual un inicio sin el cierre de16, ni se añaden controles ficticios.

No autoriza todavía TDD: falta destilar y revisar el contrato concreto. No
hay código, migraciones ni pruebas14 en esta revisión.
