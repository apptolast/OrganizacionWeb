# Revisión HTTP y cursor de historial

Dictamen: **APPROVED para integrar HTTP/cursor**, sin acreditar todavía el recorrido real ni cerrar la funcionalidad 18.

Root leyó controlador, codec, bitácora y delta completo de pruebas (3fe84d, e74692). Cinco hashes del manifiesto y los dos XML coinciden (24c59e): 75 pruebas del adaptador y 4 de seguridad, todas verdes. El foco del autor terminó EXIT0 301db6 tras Spotless por archivo.

Query y sus relaciones se validan antes del cursor. El codec exige Base64URL canónico, objetos cerrados, versión entera exacta, tipos sin coerción, UUID y fechas en el formato acordado. Activa localmente el rechazo de claves duplicadas y conserva el rechazo de tokens posteriores del ObjectMapper del proyecto, probado mediante HTTP. La restricción léxica temporal resuelve el hallazgo de normalización de 24:00, segundo 60 y nueve cifras fraccionarias. La prueba de mayúsculas ahora utiliza letras reales.

La propiedad y vinculación semántica se mantienen en aplicación/persistencia, después del parseo. Las pruebas del slice demuestran transmisión intacta y traducción de problemas; no prueban por sí solas propiedad real en PostgreSQL. Los DTO públicos se reutilizan sin exponer versiones internas. El 503 conserva el problema público sin SQL ni página parcial. GET sin CSRF conserva la política heredada y la aclaración contractual aprobada.

Pendientes de integración: backend final con integridad y concurrencia, pantalla, E2E y UX. El árbol aislado todavía debe recibir PG/wiring/cliente antes de ejecutar API real. Las campañas de mutación se iniciarán sobre el conjunto de fuentes congelado, con el alcance del plan aprobado. No se interpreta un resultado de mocks como confirmación del MVP completo.
