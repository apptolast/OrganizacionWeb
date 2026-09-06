# Evidencia consolidada — schedule_block

Feature 11, contrato aprobado a84e42f: 62 escenarios y 325 casos expandidos. Este índice del coordinador enlaza la evidencia de sus autores; no sustituye sus ciclos ni convierte comprobaciones inicialmente verdes en RED.

## Trazabilidad y ciclos

- [Backend HTTP y núcleo inicial](tdd_schedule_block_backend.md): ciclos 1–65 y mapa de @s1–@s37/@s62; después, corrección del fixture de configuración y soporte cerrado de targets del arnés en ciclos 66–73.
- [Cobertura adicional del núcleo](tdd_schedule_block_core_coverage.md): límite Unicode, DST, anclas de presupuesto, contigüidad, selección de conflicto y reloj reevaluado. Trece casos inicialmente verdes, sin producción nueva.
- [Persistencia real](tdd_schedule_block_persistence.md): @s12–@s16 y @s29–@s34, cargas propias, idempotencia concurrente, seis órdenes de cambios de estado/preferencias, usuarios independientes, snapshots y rollback de inserciones/COMMIT. El mapa identifica métodos concretos.
- [Publicador](tdd_schedule_block_publisher.md): @s36–@s37, payload cerrado, séptima ruta y confirmación/reintento en PostgreSQL/RabbitMQ reales. La prueba de recuperación siembra outbox directamente; la creación del bloque/outbox se verifica en HTTP.
- [Cliente API](tdd_schedule_block_api.md): contratos estrictos, intención retenida, respuestas reconocidas y coherencia temporal sin depender del catálogo horario del navegador.
- [Interfaz](tdd_schedule_block_frontend.md): mapa concreto de @s38–@s61 y cobertura de presentación/recuperación; @s53 incluye respuestas JSON tardías durante navegación y revocación de sesión.
- [Integración de navegador](tdd_schedule_block_integration.md): siete recorridos reales, ACK perdido después de 201 y reinicio real del backend, con PostgreSQL conservado. No se confunde recarga con reinicio.
- [Matriz UX](ux_schedule_block_frontend.md): treinta principios, evidencia técnica por motores/anchos/zoom nativo y límites de comprobación humana y dispositivos físicos.

## Revisión y verificación

Las revisiones parciales están en review_schedule_block_api.md, review_schedule_block_domain.md, review_schedule_block_backend.md, review_schedule_block_ui_integration.md, review_schedule_block_publisher.md y review_schedule_block_e2e.md. Ninguna aprobación parcial habilita por sí sola mutación o cierre.

El segundo init global del coordinador terminó EXIT 0: sesión 34832, salida 1b3236, 1338 pruebas backend (XML verificados, cero fallos/errores/omitidas), 1121 frontend y siete del arnés; lint verde. El primer init rojo y las correcciones permanecen documentados en current.md y las bitácoras de autores.

Regresión E2E global del coordinador: 57/58 verdes, incluidos los siete recorridos de bloques. La matriz histórica de disponibilidad agotó 30 segundos; el focal con traza pasó en 27 segundos y mostró 23,9 segundos invertidos en sus 28 anchos. Se está separando esa matriz sin retirar anchos/auditorías ni aumentar el límite. Su cierre y la configuración de runtime de PIT siguen pendientes del judge global. No se ejecutó mutación ni se marca done desde este índice.
