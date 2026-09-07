# Plan de entrega del MVP

Estado actualizado el 7 de septiembre de 2026 tras aprobar el inicio de sesiones. Las30–60 horas son la hipótesis de planificación anterior, de baja confianza: no se reducen por proporción de funcionalidades ni por número de agentes. La solicitud de terminar hoy es el objetivo de trabajo, no una fecha garantizada. Las horas representan tiempo transcurrido de ejecución efectiva coordinada, incluidas pruebas, CI, mutación y revisión; excluyen pausas por cuota, acceso o información pendiente. No predicen la cuota de la cuenta.

## Qué se entregará primero

Un MVP que permita iniciar sesión, organizar proyectos y subtareas, configurar disponibilidad, planificar y replanificar bloques, consultar Hoy, trabajar con temporizador y pausas, cerrar sesiones y consultar lo realizado con sus fechas y duración real. Frontend React con pnpm y SCSS, backend Java/Spring Boot hexagonal con eventos, PostgreSQL y RabbitMQ, dentro del mismo monorepo y desplegado en el servidor del usuario.

El corte de entrega corresponde a las funcionalidades 1–18 del roadmap. Las funcionalidades 19–30 siguen autorizadas: revisión semanal, personalización, vistas y campos, importación/exportación, API de integración, webhooks, calendarios, GitHub, conectores y automatizaciones. Su entrega posterior no cancela ni reduce el proyecto aprobado.

## Estado comprobado

- Funcionalidades 1–13 cerradas conforme a sus dictámenes y límites registrados. Esto no equivale a despliegue productivo.
- Replanificar: init integrado con1617 pruebas backend,1498 frontend y22 scripts verdes;98 E2E y9 comprobaciones del publicador aprobadas. Mutación backend98,9446 % (750/758), cero errores/timeouts; frontend86,70 % global con dos errores de herramienta registrados. Recuperación tras reinicio, concurrencia, privacidad, presupuesto y UX cuentan con dictámenes finales; los residuales y límites físicos se conservan explícitos.
- PR6 fusionada por el usuario en `9623990`; evidencia de cierre publicada en main `d997421`, con CI `34060054467` SUCCESS. Dictamen final en `progress/judge_reschedule_final.md`.
- Funcionalidad 14 cerrada y fusionada mediante PR13 en main353c9d4, con CI34070995379 SUCCESS. Inicio durable y recuperación real; 104 E2E, mutación backend97,9827% y frontend89,6104%, con residuales explícitos. Replay adicional terminado:11/11 KILLED, separado de las campañas originales. Funcionalidad15 en implementación: PostgreSQL, cliente y publicación avanzan en paralelo;16–18 siguen pendientes.
- Ningún despliegue productivo acreditado todavía.

## Trabajo restante y estimación

Camino crítico vigente: contrato y ejecución de15 pausa/reanudación →16 cierre con duración neta y atribución histórica →17 aviso de fin y decisión explícita →18 historial de hechos → validación integrada y despliegue real. Backend y frontend de cada funcionalidad se reparten entre agentes; cada una conserva contrato previo, TDD y validación. La tabla siguiente conserva las partidas de la hipótesis anterior, **no constituye una nueva medición** después de14. No hay evidencia suficiente para prometer el MVP hoy ni para actualizar el total con una cifra menor.

| Hito | Resultado verificable | Horas efectivas estimadas |
| --- | --- | ---: |
| Replanificar (13), completado | Dictamen final, integración funcional y mutación superadas | 0 |
| Sesiones de trabajo (14–17) | Iniciar, pausar/reanudar, cerrar y avisar al terminar; tiempo real persistido y recuperación tras recarga | 12–24 |
| Historial de trabajo (18) | Consultar tareas y sesiones realizadas, fechas y tiempos reales sin confundirlos con planificación | 4–8 |
| Validación y despliegue del MVP | CI, recorridos completos, responsive/accesibilidad, configuración del servidor, comprobación tras reinicio y recuperación | 4–8 |
| Margen de integración y correcciones | Incidencias descubiertas al unir los flujos o desplegar | 8–16 |
| Total de las partidas pendientes | Intervalo aritmético | 28–56 |

La previsión pendiente se redondea prudentemente a **30–60 horas efectivas**, frente a las36–72 estimadas antes del cierre13. La confianza sigue siendo limitada hasta contrastar acceso/configuración del servidor y completar el primer ciclo de sesiones. El extremo inferior exige reutilización fluida de la infraestructura existente y ausencia de incidencias importantes. Si esos supuestos fallan, se publica una nueva estimación con su causa; no se recortan pruebas para mantener la cifra.

Dependencia de despliegue ya identificada en docs/implementation-proposal.md: la lectura histórica del contrato de infraestructura dejaba 45 MiB dentro de su presupuesto de stacks. No representa RAM libre medida ni el estado actual del servidor. Hay que comprobar capacidad real y actualizar ese contrato antes de incorporar API, base de datos y broker. Si hace falta ampliar recursos o cambiar infraestructura, la partida de despliegue deberá reestimarse con ese trabajo concreto.

Para el conjunto del roadmap, la orientación anterior de 1–3 semanas es de baja confianza, no un compromiso: «otros conectores» y «personalizable al extremo» requieren un inventario acotado por proveedor y comportamiento antes de poder estimar el total con rigor. Cada integración depende también de permisos y contratos externos. No se puede asegurar que el proyecto completo ni el MVP terminen antes de un reinicio de cuota dentro de cuatro días.

## Secuencia de entrega y puntos de revisión

1. Inicio de sesiones (14) completado: contrato trazado, revisión, gates propios y CI posterior al merge verdes. Conservar esa regresión al integrar pausa/reanudación.
2. Implementar pausa/reanudación (15) y después cierre (16), cada una con su contrato y validación. Comprobar duración real, recuperación y posibilidad de terminar una sesión antes de habilitar el ciclo para uso habitual.
3. Añadir aviso de fin (17), conservando el fin previsto y la decisión explícita del usuario sobre continuar o terminar.
4. Incorporar historial (18): consultar lo realizado con fechas y tiempo real, separado de los bloques planificados.
5. Validar el recorrido integrado y desplegar: acceso privado, persistencia tras reinicio, responsive, HTTPS, respaldo y restauración verificados en el servidor.

En paralelo con el desarrollo puede prepararse la configuración documental del despliegue; la puesta en servicio depende del ciclo funcional y de comprobar acceso, capacidad y dominio. Se mantiene una feature en implementación cada vez, aunque backend y frontend de esa feature se repartan entre agentes.

La revisión de estimación en `progress/review_mvp_estimate.md` confirma que 30–60 horas es una **hipótesis de planificación de baja confianza**, no una previsión validada por medición. No se convierte automáticamente en días ni se divide por el número de agentes. Se recalculará tras cerrar 14 con sus gates, tras completar el ciclo 14–16 y tras comprobar el servidor. Los recuentos de funcionalidades o pruebas no equivalen a un porcentaje de esfuerzo terminado.

## Resolución de las PR

- PR1, PR2 y PR4: fusionadas por el usuario desde GitHub. Incluían checkpoints parciales de Replanificar.
- PR3: cerrada como referencia alternativa incompleta; rama y commit `d0e83bb` conservados.
- PR5: frontend completado y fusionado por el usuario en `53ed311`.
- Correcciones posteriores de formato y fixtures E2E integradas en `ae364e5`; CI completo verde sobre ese commit.
- Consulta remota posterior `27bee7`: cero PR abiertas y main `fc31969`. La integración local `1332eb7` conserva backend revisado y frontend de main; incorpora únicamente los paquetes E2E aprobados, sin fusionar snapshots Java provisionales.
- PR6 integrada mediante squash por el usuario en `9623990`. El cierre documental se publicó en `d997421`; CI34060054467 SUCCESS. La última consulta posterior confirmó cero PR abiertas. Feature14 continúa en una rama nueva desde ese main validado.

El usuario confirmó que él realiza squash and merge desde GitHub y que Claude está detenido. Las cancelaciones de CI anteriores coincidían con nuevas fusiones; ya no hay una duda de coordinación pendiente. Los agentes internos mantienen propiedad por archivo y revisión independiente. Replanificar queda cerrada por sus pruebas y dictámenes, no por el mero hecho de fusionar una rama. El despliegue del MVP sigue pendiente del ciclo de trabajo real y la preparación del servidor.

## Qué significa funcional para esta entrega

Todos los recorridos incluidos en el MVP deben funcionar contra el backend y la base de datos reales: persistencia tras reinicio, acceso privado, recuperación de errores, planificación separada del tiempo trabajado y ausencia de duplicados al reintentar. Deben pasar los contratos aprobados, revisión, pruebas y umbral de mutación, además de los criterios responsive y de accesibilidad documentados. El despliegue debe comprobarse en el servidor con configuración de secretos, HTTPS y procedimiento de respaldo/restauración verificado.

Esto define criterios comprobables; no promete ausencia absoluta de errores ni declara cumplimiento humano de las 30 leyes UX sólo por pasar herramientas automáticas. Las limitaciones reales se registran en el acta de entrega.
