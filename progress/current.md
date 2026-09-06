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
