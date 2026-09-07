# Revisión de transacción y wiring del historial

Dictamen: **APPROVED para integrar el controlador nominal**. No cierra los escenarios pendientes del historial.

Root revisó el diff y la nueva suite transaccional (229a38), el contexto de wiring y los cinco hashes (060b8b). Los cinco XML preservados suman **56 pruebas verdes**: 21 de consultas, 3 transaccionales, 3 de aplicación, 22 de wiring y 7 de configuración. La atribución inicial de 57 se señaló al autor; no se utiliza como resultado. Foco del autor EXIT0 4382ad después de formato.

TransactionTemplate envuelve toda la comprobación de propiedad, vínculo del cursor y consulta, con readOnly y REPEATABLE_READ. El oráculo observa ambas propiedades en PostgreSQL. Las excepciones de acceso y de finalización transaccional se traducen fuera de execute, por lo que no se devuelve una página aparentemente válida si el cierre falla. El fallo de cierre está inyectado después del commit real; no se presenta como una carrera natural. La tabla ausente se prueba en un contenedor propio y se restaura en finally.

Los dos beans conectan el puerto de aplicación con el adaptador real. El test de wiring utiliza JDBC y manager simulados; demuestra que alcanza el adaptador, no un recorrido HTTP/PG completo. Los cambios de la suite de consultas sólo adaptan el constructor al nuevo manager y mantienen los oráculos anteriores.

Quedan la concurrencia con escritor dentro del snapshot, integridad de recibos seleccionados, extremos de calendario, commits tardíos, variantes restantes y la integración HTTP completa. El puerto público conserva sus firmas. La incorporación del controlador no autoriza todavía campañas globales sobre el corte en desarrollo.
