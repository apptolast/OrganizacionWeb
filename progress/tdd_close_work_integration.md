# Integración HTTP y PostgreSQL16

Un caso nominal independiente, autorizado por root sobre el bundle real3082ccd y el controller HTTP aprobado5bc2e02. No se copiaron deltas activos de A; intención completa de replay, V17 y carreras posteriores no forman parte de esta acreditación. El wiring existente aporta Store, ChangeWorkSession y ReadWorkSessionChanges: no hay beans nuevos de producción ni mocks de aplicación/persistencia.

`CloseWorkSessionIntegrationTest` reutiliza SpringBootTest/MockMvc y PostgreSQL17.9 del patrón integrado14. Sólo reemplaza el reloj por una fuente temporal controlada en configuración de prueba. La base aislada nace vacía y el caso único inserta proyecto/tarea/disponibilidad; el inicio y cierre se realizan por HTTP real con filtros de seguridad y PostgreSQL.

Primer resultado1ec54b EXIT0, inicialmente GREEN: ningún RED funcional ni cambio de producción. POST inicio confirma y luego POST cierre registra60000001µs, cambia a closed, conserva SessionStart y notas, atribuye el cierre a2026-09-08 Europe/Madrid, guarda recibo y evento11. GET F devuelve exactamente el hecho original; GET active devuelve null; ambas lecturas conservan las cuatro tablas de sesiones/recibos/intervalos/outbox.

Se añadieron al mismo caso los oráculos de extremos persistidos del intervalo y correspondencia temporal/neto/día del evento; también inicialmente verdes. Spotless real y foco final c36faf EXIT0,1/1 sin fallos/errores/skip. Logs `close_integration_cycle1_initial.log` y `close_integration_final.log`; XML preservado en `progress/close_integration_final_xml`. Sólo nuevo test y esta bitácora integrables; Java HTTP/publicación/harness permanecen congelados.

Alcance: composición real nominal @s1/@s26 y ausencia de escrituras de esas lecturas. No acredita PAUSE, concurrencia, rollback, reinicio, publicación Rabbit ni recuperación de ACK perdido; no se ejecutaron campañas o E2E. El caso se repetirá con el corte común final según coordinación root, sin añadir infraestructura alternativa.
