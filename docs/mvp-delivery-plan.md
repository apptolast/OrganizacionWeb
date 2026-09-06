# Plan de entrega del MVP

Estimación provisional del 6 de septiembre de 2026, basada en el estado del repositorio. Se revisará al cerrar Replanificar y después de la primera prueba de despliegue. Las horas representan trabajo efectivo pendiente con ejecución disponible; no constituyen una fecha garantizada ni una predicción sobre cuota de la cuenta.

## Qué se entregará primero

Un MVP que permita iniciar sesión, organizar proyectos y subtareas, configurar disponibilidad, planificar y replanificar bloques, consultar Hoy, trabajar con temporizador y pausas, cerrar sesiones y consultar lo realizado con sus fechas y duración real. Frontend React con pnpm y SCSS, backend Java/Spring Boot hexagonal con eventos, PostgreSQL y RabbitMQ, dentro del mismo monorepo y desplegado en el servidor del usuario.

El corte de entrega corresponde a las funcionalidades 1–18 del roadmap. Las funcionalidades 19–30 siguen autorizadas: revisión semanal, personalización, vistas y campos, importación/exportación, API de integración, webhooks, calendarios, GitHub, conectores y automatizaciones. Su entrega posterior no cancela ni reduce el proyecto aprobado.

## Estado comprobado

- Funcionalidades 1–12 cerradas localmente conforme a sus dictámenes y límites registrados. Esto no equivale a despliegue productivo.
- Funcionalidad 13, Replanificar, en desarrollo. Revisión de composición del frontend corregida y aprobada; mutación en ejecución, todavía sin resultado. El movimiento básico ya pasa pruebas HTTP con PostgreSQL. Siguen pendientes reintentos y concurrencia, lecturas vigentes, consulta de recibos, integración de eventos y verificación completa del flujo.
- CI completo de `main` verde sobre `ae364e5`, ejecución `34047746896`, con 91 pruebas E2E aprobadas. Es regresión del conjunto integrado; no acredita los escenarios nuevos de Replanificar aún en desarrollo.
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

Dependencia de despliegue ya identificada en docs/implementation-proposal.md: la lectura histórica del contrato de infraestructura dejaba 45 MiB dentro de su presupuesto de stacks. No representa RAM libre medida ni el estado actual del servidor. Hay que comprobar capacidad real y actualizar ese contrato antes de incorporar API, base de datos y broker. Si hace falta ampliar recursos o cambiar infraestructura, la partida de despliegue deberá reestimarse con ese trabajo concreto.

Para el conjunto del roadmap, la orientación anterior de 1–3 semanas es de baja confianza, no un compromiso: «otros conectores» y «personalizable al extremo» requieren un inventario acotado por proveedor y comportamiento antes de poder estimar el total con rigor. Cada integración depende también de permisos y contratos externos. No se puede asegurar que el proyecto completo ni el MVP terminen antes de un reinicio de cuota dentro de cuatro días.

## Resolución de las PR

- PR1, PR2 y PR4: fusionadas por el usuario desde GitHub. Incluían checkpoints parciales de Replanificar.
- PR3: cerrada como referencia alternativa incompleta; rama y commit `d0e83bb` conservados.
- PR5: frontend completado y fusionado por el usuario en `53ed311`.
- Correcciones posteriores de formato y fixtures E2E integradas en `ae364e5`; CI completo verde sobre ese commit.

El usuario confirmó que él realiza squash and merge desde GitHub y que Claude está detenido. Las cancelaciones de CI anteriores coincidían con nuevas fusiones; ya no hay una duda de coordinación pendiente. Los agentes internos mantienen propiedad por archivo y revisión independiente. Las fusiones no sustituyen el cierre funcional: se sigue desarrollando y verificando el backend de Replanificar antes de dar por terminada la funcionalidad 13 o desplegar.

## Qué significa funcional para esta entrega

Todos los recorridos incluidos en el MVP deben funcionar contra el backend y la base de datos reales: persistencia tras reinicio, acceso privado, recuperación de errores, planificación separada del tiempo trabajado y ausencia de duplicados al reintentar. Deben pasar los contratos aprobados, revisión, pruebas y umbral de mutación, además de los criterios responsive y de accesibilidad documentados. El despliegue debe comprobarse en el servidor con configuración de secretos, HTTPS y procedimiento de respaldo/restauración verificado.

Esto define criterios comprobables; no promete ausencia absoluta de errores ni declara cumplimiento humano de las 30 leyes UX sólo por pasar herramientas automáticas. Las limitaciones reales se registran en el acta de entrega.
