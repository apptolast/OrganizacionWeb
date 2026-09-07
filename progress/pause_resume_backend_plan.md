# Plan mínimo de backend15

Propuesta de reparto tras la aprobación de la sección15; no inicia TDD ni añade requisitos al contrato. Un árbol común, tres propietarios de archivos y ventanas de compilación coordinadas. Root conserva Git, CI y configuración de campañas. No hacen falta snapshots de fuentes ni ramas paralelas mientras cada frontera permanezca compilable.

## Propiedad de los tres paquetes

| Autor | Archivos y responsabilidad exclusiva | Frontera entregable |
|---|---|---|
| A — núcleo | Nuevos valores de estado/intervalo/recibo; casos de uso de cambiar y consultar estado/recibo; puertos mínimos y excepciones; `WorkSessionStateChanged`; tests de aplicación/dominio. `ApplicationConfiguration` sólo al integrar con B/C. | Tipos y firmas reales surgidos del primer ciclo, congelados antes de que B/C dependan de ellos. |
| B — PostgreSQL | `V16__work_session_transitions.sql`; `PostgresWorkSessionStore.java`, sus nuevas operaciones de comando/lectura y tests PG; fixtures de limpieza afectados por las nuevas FK. | Estado, intervalos y recibos durables, replay y locks; consultas de estado coherentes y activas running/paused. Un solo dueño del Store evita separar artificialmente comandos y consultas en dos adaptadores. |
| C — HTTP/publicación | Nuevo `WorkSessionStateController` y suite HTTP; `OutboxMessage`, `RabbitBrokerPublisher` y tests directos. `PublisherConfiguration` sólo si el mecanismo existente lo requiere. | Rutas y problemas15 con puertos simulados; décima ruta y validación cerrada del evento. No modifica Store ni dominio de A. |

No renombrar el `application.WorkSessionChange` existente de14: hoy es el par interno `(SessionStart, WorkSessionStarted)`. El recibo15 puede llamarse `WorkSessionTransitionReceipt` internamente y conservar la forma HTTP normativa `WorkSessionChange`. Evita romper creación14 por una colisión de nombres sin valor funcional. La representación DTO7 y los métodos `WorkSessionQueries.detail/byRequest/active` se conservan; sólo cambia la selección de activa para incluir paused.

## V16 aditiva y compatibilidad

La opción mínima es añadir metadata mutable a `work_sessions` (revisión, última transición, acumulado de microsegundos y comienzo del tramo abierto), una tabla de intervalos y otra de recibos de transición. No crear otra copia del DTO de inicio como estado actual. La metadata inicial de filas14 deriva exclusivamente de su startedAt real; la migración puede inicializar el primer intervalo abierto de esas sesiones. No inventa otra sesión, key ni evento y no modifica las siete columnas del recibo histórico. GET nunca efectúa esa inicialización.

V14/V15 quedan intactas. V16 sustituye el índice parcial `work_sessions_one_running_owner` por la unicidad de owner para running/paused, preserva `(owner,request_key)` de inicios y añade `(owner,key)` de cambios en su propio espacio. FK de intervalos/recibos a sesión; restricciones de estado, revisión positiva, acumulado no negativo y grupos temporales coherentes. Intervalos permiten fin igual al inicio; un único tramo abierto por sesión. Las invariantes entre tablas se mantienen en la transacción existente, sin triggers, parser SQL de DTO ni event sourcing. Las relaciones de reloj/microsegundos siguen las del contrato, no el orden de horas civiles.

B verifica upgrade desde V15 con un inicio/evento real conservado y fallos de escritura/commit que revierten proyección, intervalo, recibo y outbox juntos. Al añadir tablas hijas, revisar las listas explícitas TRUNCATE de las suites afectadas: el antecedente14 mostró fallos de fixture por omitir FK nuevas. No resolverlos debilitando aserciones ni con limpieza global. Esa modificación de fixtures tiene un solo dueño y se comunica a C antes de su regresión HTTP integrada.

## Dependencias y secuencia de trabajo

1. A obtiene el primer comportamiento de transición en verde y entrega tipos/puertos compilables: comando recibe owner, sessionId, acción, key y token de revisión conservando también su id; devuelve recibo y marca replay. El puerto conserva la posibilidad de comprobar propiedad antes de identidad del token Work-Session-Revision y replay. Lectura retorna estado con snapshot temporal, o recibo inmutable. No introducir interfaces vacías adelantadas sólo para repartir trabajo.
2. B implementa esos puertos en el Store único. C puede avanzar contrato HTTP con mocks de casos de uso y publicación con el evento real ya estable, sin compilar un core en RED. Nadie copia clases de otro árbol ni mantiene firmas paralelas. Un cambio de firma se acuerda antes de editar consumidores.
3. En una frontera GREEN común, A añade sólo factories necesarias a `ApplicationConfiguration`; se ajustan los contextos de wiring que realmente lo necesiten y se hace el primer HTTP+PG real. C conserva el título temporal14 en su controller existente y aplica el título15 localmente; no mover handlers a un advice global que altere ambos contratos.
4. Focos TDD individuales por autor; Gradle serial mientras se comparte build y código Java. Spotless común sólo con los tres quietos. Un worktree aislado se justifica únicamente si un RED inevitable de una pista impide avanzar otra; primero intentar una frontera pequeña compilable. No campañas globales durante TDD.

## Regresiones que preservar

Reutilizar las suites14 de inicio, Store, lecturas, HTTP/integración y wiring: creación201/replay200 siguen devolviendo DTO7 y Location original, incluso después de transiciones; GET id/key no devuelve estado mutable. La consulta y el conflicto de activa deben reconocer paused tanto antes del INSERT como en la recuperación de una carrera de unicidad. La pausa no abre una ventana para un segundo inicio. Zona histórica, fin previsto, proyecto/tarea completed y todos los nueve eventos anteriores conservan sus protocolos.

El paquete final combina los nuevos focos con esas regresiones pertinentes, upgrade PG y Rabbit real. E2E, smoke y mutación se coordinan después con root; este documento no atribuye ejecuciones ni verde anticipado. Ponytail full/Caveman lite: reutilizar el Store y el publicador, mantener las fronteras de autoría y añadir únicamente los tipos necesarios por los casos reales.

Precisión normativa previa a TDD: HTTP15 usa Work-Session-Revision en request/response, token canónico work-session-{uuid}-{revision} sin comillas. GETstate no emite ETag: su cuerpo incluye reloj/neto variables. El parser y sus pruebas deben conservar id y revisión del token, con428 ausente,400 sintaxis y412 identidad/revisión según la precedencia del contrato. No reutilizar un parser de If-Match que exija comillas.
