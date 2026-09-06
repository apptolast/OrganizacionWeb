# Coordinación PostgreSQL de movimiento, s23

Árbol aislado OrganizacionWeb-reschedule-e2e, snapshot92e83e6 autorizado. No se copia WIP de core: rowcounts/recheck posteriores no están aquí; suite se repetirá al integrar. Baseline del árbol y regresión handlers224GREEN vigentes; no init global adicional. Ponytail full/Caveman lite. Sólo nueva RescheduleCoordinationTest y bitácora; E2E13 congelado/RED previo.

Reutiliza patrón ScheduleBlockPersistenceTest: Flyway/PostgreSQL real, servicios de estado reales, TransactionTemplate READ_COMMITTED, latch de commit y pg_stat_activity que demuestra espera por lock. Poll10ms sólo observa espera real. Un caso por ciclo.

Incidencia51d6cc: cwd backend duplicó prefijo backend en generación del archivo, que falló antes de escribir; Gradle se lanzó sin test creado. Es fallo operativo, no RED funcional. Corregido cwd raíz.

1. Completar proyecto primero: movimiento espera lock projects, luego PROJECT_COMPLETED sin proyección/recibo/evento ni modificación original. Primera ejecución pendiente.

1. Inicialmente GREEN6c0e0b (1 caso), sin producción.2. Orden inverso movimiento primero, completar proyecto después; comprueba recibo2/intervalo nuevo y original inmutable; ejecución pendiente.

2. Compilación4b103b falló por nombre de método de test byId inexistente; corrección detail(c97002), inicialmente GREEN61d05a con producción intacta.3. Completar tarea primero; mismo patrón, excepción TaskCompleted. Extraído helper de rechazo para reutilizar el caso anterior; foco ambos pendiente.

3. Inicialmente GREEN94f286 (2 casos, proyecto anterior y nueva tarea).4. Movimiento primero y completar tarea después; helper preserva recibo/proyección/instantes originales y actuales. Foco junto al proyecto anterior pendiente.

4. Inicialmente GREENb2774b (2 casos).5. Disponibilidad actualiza zona/version primero; movimiento espera mutex de preferencia y luego AvailabilityConflict sin escritura de cambio. Foco rechazos pendiente por refactor helper.

5. Inicialmente GREENf56d9c (3 rechazos).6. Movimiento antes de actualizar disponibilidad; preferencia posterior Madrid/version1 conserva igualdad del bloque actual al after confirmado en UTC. Foco tres órdenes inversos pendiente.

6. Inicialmente GREEN0fa588 (tres órdenes de movimiento primero). Producción intacta en los seis ciclos; sólo hubo errores operativos/corrección del propio test descritos arriba, no RED funcional. El primer lanzamiento sin archivo terminó50415c con No tests found, sin atribuirlo al producto.

## Entrega congelada

GJF1.31.0 focal replace+dry-run452b95 EXIT0, usando dependencia instalada. Regresión final8c651c: **6/6**, cero fallos/errores/omitidos; XML47ce26. No Spotless global, init, E2E, PIT, Git ni cambios de fuente. Sólo dos archivos nuevos en status.

| Fila s23 | Prueba |
| --- | --- |
| Proyecto primero | s23_projectCompletionFirstRejectsWaitingMove |
| Movimiento antes de proyecto | s23_moveFirstThenProjectCompletionPreservesReservation |
| Tarea primero | s23_taskCompletionFirstRejectsWaitingMove |
| Movimiento antes de tarea | s23_moveFirstThenTaskCompletionPreservesReservation |
| Disponibilidad primero | s23_availabilityFirstRejectsWaitingMoveWithOldRevision |
| Movimiento antes de disponibilidad | s23_moveFirstThenAvailabilityPreservesInstants |

Los servicios de aplicación y adaptadores PostgreSQL son reales; se observa wait_event_type Lock sobre tabla correspondiente antes de liberar commit. El segundo futuro sigue pendiente entonces; ambos terminan dentro de10s. READ_COMMITTED se comprueba dentro de la transacción retenida; finally libera latch. No se elimina advisory heredado de proyectos. La actualización de disponibilidad usa su Store real, no SQL directo. Los fixtures se crean mediante PlanBlock y sus eventos reales.

Rechazos: tipo exacto de excepción contractual (ProjectCompleted/TaskCompleted/AvailabilityConflict), original inmutable y cero proyecciones/recibos/BlockChanged. Éxitos: confirmación no replay con revisión2, before original, inicio nuevo12Z, estado posterior del otro escritor, igualdad completa de reserva actual al after, original inmutable y exactamente una proyección/recibo/evento. Así preferencia Madrid posterior no reescribe la zona ni los instantes confirmados UTC.

Límite: suite de aplicación/persistencia, no reclama haber ejecutado HTTP201/409/412 en estas carreras; traducciones HTTP corresponden a los handlers y suites independientes. Snapshot92e83e6, no producción más reciente del autor core. Debe repetirse tras integración; no acredita s24, E2E ni cierre13. Core confirmó que sus cambios posteriores no alteran locks s23.

SHA256 test: F8895D6F8B7BF371FF1B766067D5E1A0A3F4EBF63BD9F63A73B16FD0DFDB7082.
SHA256 XML: ED3F383593CCCC9EB666477EA28FD5F5163D339F49F0DE67849A1B53B5A7087C.
Diffcheck47ce26 EXIT0; archivos nuevos pendientes de revisión y commit por root.

Snapshot actualizado COPYDONE2bd776:179 Java de core29ad71/lecturasb9478c, hashes origen/destino iguales según manifest de root. Repetición autorizada980de6 EXIT0, seis casos GREEN sobre nuevo corte; sin cambios de suite ni fuente por este agente. E2E nominal se ejecuta después por separado.
