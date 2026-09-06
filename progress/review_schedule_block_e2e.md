# Revisión independiente E2E schedule_block

Fecha: 2026-09-06. Revisor: resume_review, autor previo de persistencia/publicador, sin autoría de estos E2E. Aplicadas Ponytail full y Caveman lite del repositorio. Contrato: features/schedule_block.feature aprobado a84e42f y especificación feature11.

**Veredicto: APPROVED para los siete recorridos E2E revisados.** Sin hallazgos bloqueantes en este alcance. No constituye aprobación del backend, de la feature completa, de mutación ni de la matriz UX. Revisión estática: no ejecuté suites, Docker ni PIT y no modifiqué fuentes o tests.

## Evidencia comprobada en código

| Inicio en e2e/schedule-block.spec.mjs | Recorrido y aserciones reales |
| --- | --- |
| 108 | Creación UI 201, preview sin escrituras, DTO cerrado de nueve campos, Location, replay HTTP 200 idéntico, detalle/by-request, recarga visible y una fila/evento. Comprueba estado/estimación de tarea conservados. |
| 230 | route.fetch obtiene primero un 201 real y su DTO; sólo entonces aborta la respuesta al navegador. La UI conserva intención bloqueada, completa el proyecto y recupera por la misma key, sin otro POST; una fila/evento y recarga coherente. |
| 314 | Presupuesto cero exige consentimiento; editar objetivo retira revisión y aceptación sin crear; revisar y consentir otra vez permite una creación real con objetivo actualizado. |
| 388 | Ambigüedad de ambos extremos Madrid exige opciones explícitas. Cambiar inicio +01 a +02 conserva fin +01 e invalida revisión; preview y creación representan 90 minutos UTC y zona guardada Madrid. |
| 494 | Una reserva real de otra tarea/proyecto propio se confirma después del preview. POST devuelve 409 BLOCK_OVERLAP, conserva borrador editable y retira revisión. Consulta deliberada muestra detalle; intervalo adyacente revisado crea con key diferente. Dos filas y dos eventos finales. |
| 614 | Una reserva real de 90 minutos posterior al preview provoca 409 BUDGET_EXCEEDED con exceso de 1800 segundos. Nueva revisión muestra consumo actual y exige aceptación sin marcar; después crea con permiso. Dos filas/eventos, sólo un bloque con permiso. |
| 738 | ACK perdido después de 201 confirmado, reinicio real del backend y recuperación 200 de DTO exacto por la misma key, detalle y listado, una fila/outbox y un solo POST. Cumple el recorrido concreto @s35. |

Los dos aborts afectan a la entrega al navegador, no sustituyen el servidor ni simulan una inserción. Las aserciones SQL se ejecutan contra PostgreSQL del fixture. En el reinicio se valida el nombre `organizationweb-e2e-<número>` y env-file antes de actuar; se obtiene cada contenedor desde ese proyecto Compose y se reinicia exclusivamente `backend`. Se compara StartedAt del backend, Running y la conservación de StartedAt/montajes de PostgreSQL. Readiness tiene límite de 45 segundos. La autenticación de recuperación usa el mismo context.request compartido con la página; si la sesión se pierde, el fixture la repone explícitamente. Esto es preparación del fixture, no evidencia de una recuperación de sesión realizada por la UI del producto. restart-proof.json se escribe tras las aserciones.

## Selección, aislamiento y límites

cross-browser.config.mjs conserva los recorridos previos y añade el archivo y `schedule_block:` al grep; los siete nombres coinciden y los proyectos Firefox/WebKit seleccionan ambos motores. El config base usa un worker, sin paralelismo completo ni reintentos: los TRUNCATE y el reinicio no compiten entre estos casos.

El runner crea proyecto Compose por PID y credenciales sintéticas, pasa el env-file propio y cierra ese proyecto. El helper SQL requiere prefijo de fixture y env-file, ejecuta psql con ON_ERROR_STOP y límite temporal. La suite nueva vacía explícitamente planned_blocks y sus dependencias; los cambios de fixtures anteriores añaden planned_blocks al TRUNCATE existente, sin CASCADE ni ampliar a servicios ajenos. El helper SQL heredado acepta un prefijo más amplio que la guarda de reinicio, pero el runner suministra el nombre exacto: no impide esta evidencia del recorrido autorizado.

Los conteos globales son significativos porque cada caso empieza con las tablas vacías y sólo un worker. El filtro del evento BlockPlanned evita confundirlo con eventos de preparación del proyecto/tarea. Estos E2E no prueban entrega RabbitMQ ni validación cerrada de los doce campos del evento; tampoco prueban concurrencia/rollback del almacenamiento.

Las etiquetas @s son trazabilidad parcial, no acreditación de todos sus ejemplos: aquí @s45 cubre pérdida de red, @s49 cubre solape y presupuesto, @s56 cubre proyecto completado con resultado incierto. @s44 no queda completo por comprobar una key y una petición; los otros detalles de interacción pertenecen a cobertura UI. Las capturas 320/1440 y controles accesibles del caso DST no acreditan la matriz entera @s59/@s60, dispositivos físicos ni evaluación humana. No se atribuye @s53 a estos siete recorridos.

La bitácora progress/tdd_schedule_block_integration.md documenta siete casos Chromium (salida d0ec44) y catorce Firefox/WebKit (a68c90). También conserva y corrige explícitamente la selección inicial de sólo dos casos. Son ejecuciones reportadas por el autor, no una ejecución nueva del revisor; el código actual respalda los siete recorridos declarados.

## Corte revisado

- e2e/schedule-block.spec.mjs SHA256: 20A68E78A8801EE628BAA9F15DD78D65E48A81AEE67075EA1B1C87C7A9E389A4.
- e2e/cross-browser.config.mjs SHA256: 1A10A8B4D25C79EF8A918E0B7B6C9CDF3D0641C9362E63B2B662128DD8C1A628.
- Leídos también playwright.config.mjs, scripts/e2e.mjs, e2e/support/authenticated-test.mjs, e2e/support/projects.mjs y diffs de fixtures afectados.
