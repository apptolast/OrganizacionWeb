Pausar una sesión conserva su hora de fin prevista y registra sólo el tiempo trabajado. Reanudar continúa esa misma sesión; una respuesta perdida se puede comprobar sin duplicar la transición. El recibo histórico permanece separado del estado actual.

Incluye persistencia transaccional PostgreSQL con revisión e idempotencia, API de consulta y transición, publicación de eventos, controles React/SCSS y recuperación manual. Mantiene una única sesión abierta por propietario, incluida la pausa.

Validación actual:
- 1.890 pruebas Java, 1.666 frontend y 32 del runner verdes; lint completo verde tras corregir formato.
- Smoke real de respuesta perdida, indisponibilidad de Rabbit y reinicio del backend con recuperación durable.
- Recorridos en Chromium, Firefox y WebKit; 515 medidas responsive y 35 análisis axe sin violaciones, con texto y zoom nativo al 200 %.
- Revisión independiente de backend y frontend; informes de integración, E2E y límites en progress.

Borrador mientras terminan las campañas PIT/Stryker y la regresión completa de CI. Feature15 sigue in_progress. Cierre de sesión, aviso de fin, historial y despliegue pertenecen a los siguientes pasos del MVP. No se acredita validación en dispositivos físicos.
