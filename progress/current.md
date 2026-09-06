# Sesión actual — feature14 en implementación

Feature13 Replanificar está cerrada e integrada. El usuario fusionó PR6 mediante squash en `9623990`; la evidencia de cierre se publicó en main `d997421`, con CI `34060054467` SUCCESS. No quedan PR abiertas en la última consulta registrada. El despliegue productivo sigue pendiente.

Feature14 start_work_session está **in_progress**. Especificación normativa `f4a87c5`, contrato `c54aee6`: 42 escenarios y 123 casos declarados. El juez independiente aprobó el contrato en [review_start_work_contract.md](review_start_work_contract.md), y root lo aceptó bajo la autorización global vigente. Estos recuentos no representan pruebas ejecutadas.

Primer avance revisado y versionado: backend `bf8f834` contiene núcleo y persistencia nominal con upgrade V13 a V14; dos pruebas verdes verificadas por root en XML8d682a. Cliente `642a5af` contiene POST y GET active con validación cerrada y precisión exacta; 17 pruebas verdes, tipos, lint y formato según su bitácora. Los dictámenes independientes de núcleo, persistencia y cliente aprueban únicamente esos cortes. Root revisó código y límites en d1418f, 1a2a75 y d21f16. Autores detenidos en el checkpoint, sin procesos de prueba activos. Root conserva Git, documentación e integración. No se implementan todavía pausa, cierre, aviso ni historial global (15–18), y el uso habitual requiere el ciclo de inicio/cierre completo.

Baseline validado de código: init91757f (1617 backend, 1498 frontend y 22 scripts), 98 E2E, 9 smoke; PIT 750/758 y frontend Stryker 86,70 % con sus errores de herramienta documentados. El cambio de rama desde main conservó idénticos backend, frontend, E2E y scripts. No repetir suites completas por cambios exclusivamente documentales; los cambios de producción nuevos requieren sus pruebas y gates finales.

Plan pendiente: [mvp-delivery-plan.md](../docs/mvp-delivery-plan.md), estimación provisional de 30–60 horas efectivas. Dominio solicitado al usuario y todavía pendiente; no bloquea la implementación local. Capacidad, acceso, HTTPS y restauración del servidor aún deben verificarse.

Ponytail full y Caveman lite. No leer ni limpiar `.e2e-work/read-review*`, `frontend/.stryker-tmp-availability-replay` ni `progress/proposal_schedule_block_time.md`, ni borrar/mover sus ascendientes. No force-push ni limpieza global. Conservar evidencia histórica y atribución por corte.

## Continuación concreta

Backend: siguientes ciclos individuales de rango temporal y zona fallback; después idempotencia por propietario, unicidad activa, integridad contextual y errores transaccionales antes de HTTP y consultas. Duración y elegibilidad del núcleo ya están implementadas en 53b37de; tipos/ausencia JSON siguen pendientes del adaptador HTTP. V14 sigue siendo una migración de desarrollo: por ahora sólo crea la tabla y retira la FK legacy del agregado outbox hacia proyectos; sus constraints de negocio todavía no están completos. El evento14 aún no tiene ruta de publicador implementada.

Frontend: consultas por ID/key implementadas en 9e67299. Siguientes ciclos: variantes contractuales restantes y componente de tarea con recuperación deliberada, privacidad y foco. Ninguna UI usa todavía este cliente y ningún endpoint/bean expone el nuevo backend. No se ha ejecutado una suite global, E2E ni mutación sobre feature14; los gates de feature13 no se atribuyen al código nuevo. Esta rama no está lista para merge ni despliegue.

## Avance posterior y estimación revisada

Plan actualizado en 170ca76: 30–60 horas sigue siendo hipótesis de baja confianza, con secuencia y definición de horas explícitas. No equivale a calendario ni a cuota garantizados; 19–30 siguen fuera del MVP sin cancelarse.

Cliente 9e67299: GET por ID/key añade diez casos, 27/27 verdes en 23584f, con tipos, lint y formato. Dictamen review_start_work_recovery_client.md APPROVED parcial; root leyó el diff completo en 2b53db. Backend 53b37de: rango 1–1440 y completed proyecto/tarea, ocho pruebas de núcleo verdes e55cd8; dictamen review_start_work_guards.md APPROVED parcial, root386a6a. Regresión PG existente d4b176 1/1 sobre el núcleo actualizado, XML confirmado por root888dd5. Los nueve casos backend se ejecutaron en dos corridas focales, no en una suite global nueva.

Ambos autores terminaron y dejaron el corte congelado, sin procesos activos. Conservados incidentes e inicialmente GREEN en bitácoras. Feature14 continúa in_progress; no nuevos endpoints, UI ni publicación, no merge de esta rama ni despliegue. No trasladar la aprobación de checkpoints a una aprobación final de feature14.

## Ejecución paralela solicitada por el usuario — 7 de septiembre

Tres autores activos para completar feature14 sin detenerse por cada microcheckpoint: resume_backend termina núcleo/persistencia/HTTP en este árbol; resume_frontend termina cliente/UI en este árbol con archivos separados; resume_review actúa ahora como autor del publicador en el worktree aislado OrganizacionWeb-session-publisher, branch codex/work-session-publisher desde020f7ff. Root coordina y revisa; habrá revisión cruzada independiente después del freeze. No hay autoaprobación del autor del publicador.

Publicación posee únicamente OutboxMessage, PublishOutbox, RabbitBrokerPublisher y tests asociados; backend no toca esos archivos. Frontend no toca backend. Gradle corre con salidas separadas entre los dos worktrees. El equipo dispone de24procesadores lógicos y63,4GiB RAM; no se observó presión de memoria en la comprobación3677e8. Se evita repetir suites globales por cada test y se conservan ciclos individuales y regresiones pertinentes.

Acceso al servidor intentado sin escritura: autenticación SSH rechazada por clave (468624). Solicitud de alias/clave configurada pendiente del usuario; dominio también pendiente. No detiene implementación local ni constituye medición de recursos del servidor. Detalles en docs/deployment-readiness.md.

## Publicador integrado y relevo HTTP

Publicador terminado por autor aislado, revisado independientemente por root en review_start_work_publisher.md: commit b4f425a integrado como6a8534e. XML62c0ab confirma185/185 (152 publicación,13 Rabbit,9 fallos Rabbit,11 configuración), sin fallos/errores/omitidos; hashes coinciden con freeze. Nuevo evento/ruta/cola implementados, sin modificar worker/config. Mutación e integración global14 todavía pendientes.

resume_review pasa a autor HTTP14 en el mismo worktree aislado OrganizacionWeb-session-publisher, ahora branch codex/work-session-http desdeb4f425a. Rootcopió desde el árbol común cinco dependencias de compilación reales y no versionadas (79c786): ReadWorkSessionsUseCase y WorkSessionAlreadyActiveException, WorkSessionIdempotencyConflictException, WorkSessionTimeOutOfRangeException, WorkSessionNotFoundException. **No fusionar esta rama entera ni incluir esas copias en el paquete HTTP.** El autor no las edita; root integrará sólo controller/tests/bitácora revisados. Esto evita que los ciclos RED de compilación del backend y HTTP interfieran.

Puertos: StartWorkSessionUseCase.start(owner,project,task,key,minutes) devuelve WorkSessionConfirmation; ReadWorkSessionsUseCase.active devuelve Optional<SessionStart>, detail y byRequest devuelven SessionStart. El backend real implementa sus puertos y PostgreSQL en el árbol común. ApplicationConfiguration/wiring queda reservado al autor HTTP al integrar, cuando constructor y almacén reales estén presentes. La copia aislada mantiene el constructor core anterior y no debe alterar esa configuración por anticipado.

## Estado vigente tras PR7 y corrección de CI

Esta sección sustituye las referencias anteriores a autores detenidos, V14 editable y main verde. El usuario fusionó PR7 desde 54ea132 mediante squash en 25f7447. V14 ya está publicada y se conserva; la unicidad nueva se implementa en V15. La CI de PR7 y de main falló porque veinte fixtures heredados truncaban tasks sin incluir la nueva tabla work_sessions. El fallo no acredita un defecto de los endpoints ni permite eliminar la integridad referencial.

El backend corrigió las veinte listas explícitamente, sin CASCADE. Reproducción SQLSTATE 0A000, veinte casos existentes verdes después de formato (7c5461 y XML0a729b), uno por fixture. Root verificó en44ce25 que los veinte archivos sólo cambian la inclusión de work_sessions y formato; los XML locales ya habían sido sustituidos por el siguiente ciclo TDD y no se presentan como segunda validación independiente. Commit8f2cd75, paquete sobre main a0ac0a3, PR9 abierta con CI completa en ejecución. El mismo paquete está aplicado al árbol HTTP como24b3bf3. No se han incluido los cambios incompletos de14 en esta corrección.

Los tres autores continúan: backend termina reglas y persistencia; frontend termina interfaz, cliente e integración TaskReader; HTTP termina adaptadores y pruebas. Hito frontend56/56 focales (9f8645):28 API,27 UI y1 integración. Hito HTTP11 casos con seguridad real y puertos simulados (6f0adf). Son avances parciales, no cierre ni cobertura global. Root revisa en paralelo; ha pedido comprobar la respuesta active=null tardía después de confirmar un POST.

Pendiente del cierre14: completar los tres paquetes, integrar wiring, revisión independiente, recorrido real, matriz UX y gates globales/mutación con objetivos específicos14. El arnés todavía no dispone del objetivo de mutación14; debe añadirse mediante autor y revisión antes de atribuir un resultado. No habilitar uso habitual sin cierre de sesiones16. La estimación30–60 horas sigue siendo una hipótesis pendiente de recalcular con el cierre14 y acceso al servidor; no se divide por el número de agentes.
