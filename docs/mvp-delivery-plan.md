# Plan de entrega del MVP

Estimación provisional del 6 de septiembre de 2026, basada en el estado del repositorio. Se revisará al cerrar Replanificar y después de la primera prueba de despliegue. Las horas representan trabajo efectivo pendiente con ejecución disponible; no constituyen una fecha garantizada ni una predicción sobre cuota de la cuenta.

## Qué se entregará primero

Un MVP que permita iniciar sesión, organizar proyectos y subtareas, configurar disponibilidad, planificar y replanificar bloques, consultar Hoy, trabajar con temporizador y pausas, cerrar sesiones y consultar lo realizado con sus fechas y duración real. Frontend React con pnpm y SCSS, backend Java/Spring Boot hexagonal con eventos, PostgreSQL y RabbitMQ, dentro del mismo monorepo y desplegado en el servidor del usuario.

El corte de entrega corresponde a las funcionalidades 1–18 del roadmap. Las funcionalidades 19–30 siguen autorizadas: revisión semanal, personalización, vistas y campos, importación/exportación, API de integración, webhooks, calendarios, GitHub, conectores y automatizaciones. Su entrega posterior no cancela ni reduce el proyecto aprobado.

## Estado comprobado

- Funcionalidades 1–12 cerradas localmente conforme a sus dictámenes y límites registrados. Esto no equivale a despliegue productivo.
- Funcionalidad 13, Replanificar, en desarrollo. Frontend con componentes y pruebas avanzados; revisión de composición detectó correcciones pendientes. Backend recuperado de un checkpoint: faltan movimiento, concurrencia, lecturas vigentes, recibos e integración de eventos.
- Funcionalidades 14–18 pendientes de contrato detallado e implementación.
- Ningún despliegue productivo acreditado todavía.

## Trabajo restante y estimación

| Hito | Resultado verificable | Horas efectivas estimadas |
| --- | --- | ---: |
| Integrar y cerrar Replanificar (13) | Mover/cancelar, historial del cambio, recuperación y concurrencia con API/BD reales; revisión y mutación | 6–12 |
| Sesiones de trabajo (14–17) | Iniciar, pausar/reanudar, cerrar y avisar al terminar; tiempo real persistido y recuperación tras recarga | 12–24 |
| Historial de trabajo (18) | Consultar tareas y sesiones realizadas, fechas y tiempos reales sin confundirlos con planificación | 4–8 |
| Validación y despliegue del MVP | CI, recorridos completos, responsive/accesibilidad, configuración del servidor, comprobación tras reinicio y recuperación | 4–8 |
| Margen de integración y correcciones | Incidencias descubiertas al unir los flujos o desplegar | 8–16 |
| Total de las partidas | Intervalo aritmético | 34–68 |

La previsión comunicada se redondea prudentemente a **36–72 horas efectivas**. La confianza es limitada hasta cerrar el backend de Replanificar y contrastar acceso/configuración del servidor. El extremo inferior exige reutilización fluida de la infraestructura existente y ausencia de incidencias importantes. Si esos supuestos fallan, se publica una nueva estimación con su causa; no se recortan pruebas para mantener la cifra.

Para el conjunto del roadmap, la orientación anterior de 1–3 semanas es de baja confianza, no un compromiso: «otros conectores» y «personalizable al extremo» requieren un inventario acotado por proveedor y comportamiento antes de poder estimar el total con rigor. Cada integración depende también de permisos y contratos externos. No se puede asegurar que el proyecto completo ni el MVP terminen antes de un reinicio de cuota dentro de cuatro días.

## Orden de las PR actuales

1. PR4: corrección responsive y estabilidad de CI. Fusionar cuando Application CI pase sobre el commit corregido. Mantener todos los oráculos de interfaz y zoom.
2. PR3: cerrada como referencia alternativa incompleta; conservar rama y commit d0e83bb. No integrar dos clientes incompatibles.
3. PR2: continuar el backend recuperado, verificar el contrato completo y revisar migraciones antes de integrar.
4. PR1: completar revisión de frontend, integrar con backend y ejecutar pruebas reales y mutación antes del cierre de la funcionalidad13.

El usuario detuvo Claude Code y autorizó a Codex a resolver y fusionar. Ya no se espera una confirmación externa de reparto. Los agentes internos mantienen propiedad por archivo y revisión independiente.

## Qué significa funcional para esta entrega

Todos los recorridos incluidos en el MVP deben funcionar contra el backend y la base de datos reales: persistencia tras reinicio, acceso privado, recuperación de errores, planificación separada del tiempo trabajado y ausencia de duplicados al reintentar. Deben pasar los contratos aprobados, revisión, pruebas y umbral de mutación, además de los criterios responsive y de accesibilidad documentados. El despliegue debe comprobarse en el servidor con configuración de secretos, HTTPS y procedimiento de respaldo/restauración verificado.

Esto define criterios comprobables; no promete ausencia absoluta de errores ni declara cumplimiento humano de las 30 leyes UX sólo por pasar herramientas automáticas. Las limitaciones reales se registran en el acta de entrega.
