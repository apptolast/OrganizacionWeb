# Sesión actual — feature14 en implementación

Feature13 Replanificar está cerrada e integrada. El usuario fusionó PR6 mediante squash en `9623990`; la evidencia de cierre se publicó en main `d997421`, con CI `34060054467` SUCCESS. No quedan PR abiertas en la última consulta registrada. El despliegue productivo sigue pendiente.

Feature14 start_work_session está **in_progress**. Especificación normativa `f4a87c5`, contrato `c54aee6`: 42 escenarios y 123 casos declarados. El juez independiente aprobó el contrato en [review_start_work_contract.md](review_start_work_contract.md), y root lo aceptó bajo la autorización global vigente. Estos recuentos no representan pruebas ejecutadas.

Primer avance revisado y versionado: backend `bf8f834` contiene núcleo y persistencia nominal con upgrade V13 a V14; dos pruebas verdes verificadas por root en XML8d682a. Cliente `642a5af` contiene POST y GET active con validación cerrada y precisión exacta; 17 pruebas verdes, tipos, lint y formato según su bitácora. Los dictámenes independientes de núcleo, persistencia y cliente aprueban únicamente esos cortes. Root revisó código y límites en d1418f, 1a2a75 y d21f16. Autores detenidos en el checkpoint, sin procesos de prueba activos. Root conserva Git, documentación e integración. No se implementan todavía pausa, cierre, aviso ni historial global (15–18), y el uso habitual requiere el ciclo de inicio/cierre completo.

Baseline validado de código: init91757f (1617 backend, 1498 frontend y 22 scripts), 98 E2E, 9 smoke; PIT 750/758 y frontend Stryker 86,70 % con sus errores de herramienta documentados. El cambio de rama desde main conservó idénticos backend, frontend, E2E y scripts. No repetir suites completas por cambios exclusivamente documentales; los cambios de producción nuevos requieren sus pruebas y gates finales.

Plan pendiente: [mvp-delivery-plan.md](../docs/mvp-delivery-plan.md), estimación provisional de 30–60 horas efectivas. Dominio solicitado al usuario y todavía pendiente; no bloquea la implementación local. Capacidad, acceso, HTTPS y restauración del servidor aún deben verificarse.

Ponytail full y Caveman lite. No leer ni limpiar `.e2e-work/read-review*`, `frontend/.stryker-tmp-availability-replay` ni `progress/proposal_schedule_block_time.md`, ni borrar/mover sus ascendientes. No force-push ni limpieza global. Conservar evidencia histórica y atribución por corte.

## Continuación concreta

Backend: siguientes ciclos individuales de validación, zona fallback y elegibilidad; después idempotencia por propietario, unicidad activa, integridad contextual y errores transaccionales antes de HTTP y consultas. V14 sigue siendo una migración de desarrollo: por ahora sólo crea la tabla y retira la FK legacy del agregado outbox hacia proyectos; sus constraints de negocio todavía no están completos. El evento14 aún no tiene ruta de publicador implementada.

Frontend: siguientes ciclos de consultas por ID/key, variantes contractuales restantes y componente de tarea con recuperación deliberada, privacidad y foco. Ninguna UI usa todavía este cliente y ningún endpoint/bean expone el nuevo backend. No se ha ejecutado una suite global, E2E ni mutación sobre feature14; los gates de feature13 no se atribuyen al código nuevo. Esta rama no está lista para merge ni despliegue.
