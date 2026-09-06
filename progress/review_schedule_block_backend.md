# Revisión incremental del backend de bloques

Estado: IN_PROGRESS. No habilita mutación ni acredita cierre.

## Intersecciones positivas y días omitidos

El coordinador observó que el primer bucle de BlockBudget añadía una fila por fecha, incluso cuando los límites de un día omitido coincidían. Se pidió contrastar Pacific/Apia en su salto de fecha de 2011.

El autor ejecutó la prueba con java.time: intervalo UTC 2011-12-30 09:30–10:30. Inicialmente devolvía tres fechas, incluida 2011-12-30 con cero segundos (RED). Tras exigir intersección positiva, devuelve sólo 2011-12-29 y 2011-12-31, con 1800 segundos cada una (GREEN, dos tests focales). El caso concreta la regla ya aprobada de no exponer días sin tiempo solicitado; no amplía el alcance del producto.

Falta revisar el backend completo y sus pruebas de persistencia, concurrencia, errores y publicación. El coordinador todavía no ha ejecutado la verificación final.

## Comprobación solicitada: cuerpo ausente y precedencia HTTP

En el corte de BlockController posterior a las primeras pruebas de headers, ambos métodos usan `@RequestBody String raw` obligatorio. Spring puede rechazar el cuerpo ausente antes de entrar en el método, impidiendo que la validación de query, IDs o Availability-Revision aplique la precedencia aprobada. Se solicita al autor probar POST vacío sin Availability-Revision (428 esperado) y query inválida con cuerpo vacío (error de query esperado), antes de decidir la corrección. Es una comprobación pendiente de reproducción, no un fallo de integración ya ejecutado por el coordinador.

El autor reprodujo cuatro RED reales: query de preview, query de creación, ID de creación y revisión ausente con cuerpo vacío. Corregido permitiendo que el handler reciba el cuerpo ausente y verificando su JSON después de query, IDs y headers. Reporta 90 pruebas HTTP verdes tras la corrección y la comparación de los siete campos de intención. El hallazgo queda resuelto en ese corte; no sustituye la revisión final ni las pruebas pendientes de lecturas, concurrencia y rollback.

## Revisión de persistencia y lecturas, 6 de septiembre

El coordinador revisa PostgresBlockStore, V11 y las pruebas reales de concurrencia al cerrar el autor 38 casos verdes (d9ef28). La transacción bloquea proyecto, tarea y disponibilidad en el orden aprobado; vuelve a consultar idempotencia después de esperar la disponibilidad. La ausencia de fila observada por el locking SELECT conserva ausencia aunque otra conexión inserte después. No se usa esa fila nueva sin bloquearla.

Los seis órdenes de cambios de tarea/proyecto/disponibilidad usan los adaptadores reales y verifican espera en pg_stat_activity. Snapshot de preview y usuario independiente tienen sincronización explícita y resultados/fila/eventos comprobados. Los cinco fallos por triggers incluyen supresión de ambas inserciones y fallo diferido de COMMIT; los rowcounts y la captura externa revierten y traducen el error. El helper execute extiende esa traducción a lecturas sin convertir un fallo SQL en lista vacía. Restricciones de identidad compuesta, key por tarea, intervalo, duración y precisión se atacan con SQL directo.

ReadBlocks conserva límite de 20 y posición del último elemento; el adaptador obtiene 21 ordenados por created_at e id. BlockController aplica cursores cerrados al contexto. El target PIT de persistencia coincide con el package real; se retiró OutboxMessageTest inexistente y permanecen las pruebas reales del publicador. ApiErrors no cambió: los handlers nuevos pertenecen a BlockController y están en el scope.

Sin hallazgo nuevo en este corte. Aún pendientes formato final, HTTP 503, init global y mapa consolidado antes de emitir el judge completo. No habilita mutación.
