# Revisión independiente de concurrencia s23

**APPROVED parcial** para la nueva suite de seis órdenes. Root no escribió pruebas ni producción. Lecturas `ad08df`/`f57723` y revisión del corte formateado `3d11d6`; hash de test `F8895D6F8B7BF371FF1B766067D5E1A0A3F4EBF63BD9F63A73B16FD0DFDB7082`. XML leído por root:6 tests,0 fallos/errores/omitidos. Registro del autor `tdd_reschedule_coordination.md` conserva inicialmente GREEN y dos incidencias operativas sin llamarlas RED funcional.

Cada operación inicial ejecuta el servicio o Store real dentro de una transacción mantenida hasta liberar el latch. La otra operación se observa esperando un lock de PostgreSQL sobre proyecto, tarea o disponibilidad antes de permitir el commit; no basta con arrancar dos futuros. Se comprueba READ_COMMITTED real. Los finally liberan la barrera antes de cerrar el executor, y las esperas tienen límite.

Cuando gana completar proyecto/tarea o actualizar preferencia, movimiento devuelve la excepción contractual correspondiente y deja creación inmutable, sin proyección/recibo/BlockChanged. Cuando gana movimiento, el otro escritor termina después; la reserva vigente completa coincide con after, conserva hechos originales y tiene una proyección planned revisión2, un recibo y un evento. Cambiar luego la preferencia a Madrid no convierte otra vez el intervalo confirmado UTC.

La actualización de preferencia usa el Store real con callback válido, no el controlador HTTP. Esta suite prueba aplicación/persistencia y exclusión; las traducciones201/409/412 dependen de la conexión HTTP revisada por separado. No elimina el advisory heredado de estado de proyectos ni añade locks productivos.

El primer6/6 corresponde al snapshot92e83e6. Root después copió179 archivos Java de producción GREEN coordinada (`2bd776`) con hashes iguales a origen y sin extras en destino; archivo en `work/reschedule-e2e-snapshot-29ad71-b9478c`. Falta registrar la repetición de6 sobre ese corte y luego integrarla al árbol backend. No fusionar el snapshot completo ni atribuirle cierre13, E2E o despliegue.

Repetición sobre los179 Java actualizados: autor `980de6` GREEN6/6; root verificó XML y copió exclusivamente la nueva suite al árbol backend con SHA idéntica en `597e10`. Se autoriza su incorporación al conjunto integrado, pendiente de ejecución en ese árbol y puertas finales. Ningún código de producción viajó con esa integración de tests.
