# TDD publicación17

Árbol aislado OrganizacionWeb-end-http, contrato aprobado SHA6BC581725DC0FE4C7B548191A842882CE5A62FD0348CF789D1BCCD843ABE4309. Baseline testClasses1168c9, mismo producto que init16 verde, sin repetir init ni tocar COMMON. Ponytail full/Caveman lite. Cada caso se añade después del GREEN anterior; los logs end_pub_NN_* conservan resultados reales.

1. Evento EXTEND11 nominal @s24: RED62c6f5, unsupported; mínimo allowlist/shape y GREEN90d4c2. Fixture JSON explícito, UUID independiente y publicador real por sus puertos; todavía no se acredita Rabbit ni record de A.
2. Revisión numérica @s25: RED481219, GREEN4011e9.
3. Status closed @s25: RED0b654c, GREEN0255e5.
4. Minutos fraccionarios @s25: RED362ede, GREEN896da7.
5. Fin distinto por1µs @s25: RED3ff2ed, fórmula max(previous,occurred)+minutos y GREENc998f6.
6. Campo privado adicional @s25: inicialmente GREEN por esquema exacto del primer ciclo, sin cambio productivo.

7. Cero minutos con fin coherente: REDa4b386, GREENf50bd8.
8.1441 minutos con fin coherente: RED264a05, GREENe04a0d.
9. Revisión no canónica: REDd127e4, GREENec7cb7.
10. Revisión superior a BIGINT: RED095d32, GREENd8b181.
11. PreviousEnd pasado con nanosegundos, manteniendo fórmula por occurredAt: RED461d4b, GREENeccffe. Se reutiliza el patrón UTC/µs de14 para los tres instantes.
12. PreviousEnd año cero, manteniendo fórmula: REDc39d59, GREEN013232.
13. Identidad de evento igual a sesión: RED3ad9e1, GREENca662d.
14. Positivo paused tardío1440 con revisión máxima y fin en último microsegundo9999: inicialmente GREENf646e5.
15. Positivo un minuto desde primer instante0001: inicialmente GREEN081f63.

Primer checkpoint de validación congelado: SpotlessApply real y regresión PublishOutboxTest **EXIT0 32cc40,207/207**, cero fallos/errores/omitidos. XML preservado en `progress/end_pub_checkpoint_xml/PublishOutboxTest.xml`, recuento2ba5b4. Sólo dos Java modificados; esquema/validadores anteriores conservados. OutboxMessage SHA2E53C8431C1DE70C4641089085D44059931DCF9EC5AD38BFCF29B7E40FECAB91; test SHA7F3A15F1573745DF0463F304CE688D3EA6A3343B0AA4C4BCC0782CF647A82832. Campo privado adicional ciclo6 fue inicialmente GREEN9eb064.

Rabbit todavía espera el record real de A mediante bundle aprobado; no se declara publicación17 completa ni entrega durable desde el broker de prueba de estos helpers. No código core/Store/HTTP provisional, no stubs ni copia de WIP. El evento no contiene plannedEndAt; su validador comprueba sólo las relaciones expuestas y no inventa una consulta para verificar el fin original. No campañas ni gates globales.

## Paquete de publicación completo para revisión

Root incorporó el bundle real909b0ca como6948904, sin tocar estos adaptadores. Ciclo16 Rabbit usa WorkSessionExtended real de A: REDb60409 por Unsupported event type, nueva ruta y GREEN710a42. Rabbit real devuelve el JSON original byte por byte, once campos, routing work-session.extended.v1, messageId independiente y entrega persistente2; redeclaración de cola durable/quorum compatible. No se añadió otro worker ni configuración. Las once rutas anteriores permanecen.

Ciclo17 broker indisponible: inicialmente GREEN8a64fa con evento original y reintento. Ciclo18 confirmación perdida: inicialmente GREEN667242 con el mismo evento. Son conexiones del protocolo existente, no una simulación de reinicio/ACK HTTP.

Freeze final: **EXIT0 7ff7c5**, Spotless real y regresión209 PublishOutbox +17 Rabbit =226 casos, cero fallos/errores/omitidos, log `end_pub_final.log`. XML preservados en `end_pub_final_xml`; cuatro hashes exactos en `end_pub_freeze_hashes.json`. Sólo cuatro Java de propiedad C y esta bitácora; sin núcleo/PG/wiring. El cambio de indentación del ternario Rabbit procede de Spotless al añadir la duodécima ruta. Paquete congelado para revisión independiente; siguiente trabajo HTTP separado con puertos reales. Smoke con reinicio y gates globales/mutación quedan para integración, no se atribuyen aquí.
