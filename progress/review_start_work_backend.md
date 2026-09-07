# Revisión de núcleo y persistencia — iniciar trabajo

Dictamen: aprobado para integración y mutación; feature14 todavía no cerrada.

Se revisaron StartWorkSession, WorkSessionContext, ReadWorkSessions, puertos y excepciones, PostgresWorkSessionStore, V15 y las pruebas focales. El núcleo captura una vez el reloj, conserva microsegundos, valida rango y duración, y comparte la regla de elegibilidad. El almacén prioriza contexto propio, replay y sesión activa antes de elegibilidad y disponibilidad. Usa READ_COMMITTED explícito y una plantilla de lectura separada, sin modificar configuración por petición.

Las restricciones de V15 garantizan owner/key, una sesión activa por propietario e integridad proyecto/tarea. V14 publicada no cambia. La captura de colisión está limitada al INSERT de sesión y la resolución ocurre después del rollback; un error23505 del outbox sigue siendo almacenamiento no disponible. Se revisaron las carreras con dos INSERT alcanzados y los cuatro órdenes de bloqueo observados en PostgreSQL, además de rollback, rowcounts y fallos de cierre.

Evidencia: regresión final del autor1e3110 y Spotless real verdes. Root verificó independientemente XML y cuatro hashes en492b1f: StartWorkSessionTest17, ReadWorkSessionsTest4, WorkSessionPersistenceTest1 y WorkSessionStoreTest41;63 casos, cero fallos, errores u omitidos. Los hashes completos están en tdd_start_work_backend.md. Lectura final de código a0d10b, cdb3d9 y0fc877 confirma las correcciones revisadas.

Pendiente: wiring HTTP con PostgreSQL real, smoke de publicación/recuperación, navegador/UX, mutación e integración global. Las pruebas de este paquete no acreditan esos niveles.
