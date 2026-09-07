# Review de integración HTTP y PostgreSQL17

APPROVED para el recorrido integrado nominal. Root leyó el test completo y bitácora (daed7e). No hay mocks del núcleo ni Store: SpringBoot/MockMvc alcanza PostgreSQL real con reloj de prueba. Se contrastan DTO, fin original/efectivo, marca y evento, cierre y recuperación por POST/C/K después de retirar sólo el outbox de ampliación. La comparación de tablas verifica ausencia de escrituras durante consultas/replay y lecturas401/404.

Un test completo GREENae60e1 y después Spotless/foco1/1 EXIT0 4bffa6. Los dos intentos anteriores fallaron por columnas de orden incorrectas del propio test y no cuentan como RED de producción. Fuente y XML están fijados en end_integration_freeze_hashes.json. No acredita reinicio, Rabbit real, concurrencia, rollback, upgrade ni navegador; tienen evidencia separada pendiente.
