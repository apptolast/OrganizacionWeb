Permite consultar desde Historial la planificación, los cambios de tarea y las sesiones de trabajo mediante una API de lectura autenticada. Incluye filtros por categoría, contexto y fechas UTC, detalles de los hechos y paginación estable por cursor; distingue planificación de trabajo realizado.

Este es el corte de integración de la funcionalidad 18. El adaptador de la PR 18 ya está incorporado aquí junto con su validación final; no es necesario fusionar ambas ramas de forma independiente.

Estado: borrador. Hay checkpoints revisados de núcleo, PostgreSQL, transacción de lectura, cliente, HTTP y página nominal. Han pasado 79 pruebas focales HTTP/seguridad y dos recorridos E2E de historial. Faltan los últimos cambios de integridad e interacción, el resto de E2E, revisión UX, mutación y validación conjunta sobre un corte final. No hay despliegue productivo acreditado.

Contrato y alcance: `project-spec.md`, `features/history.feature` y `progress/plan_history_validation.md`. La estimación y las dependencias del servidor se mantienen en `docs/mvp-delivery-plan.md`.
