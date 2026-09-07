# Plan de entrega del MVP

Actualizado el 7 de septiembre de 2026, a las 15:50. **Software del MVP1–18 fusionado y con CI posterior verde en main a5d1586.** Las imágenes están publicadas y su configuración montada funciona en una comprobación local con PostgreSQL y RabbitMQ. El despliegue mantiene una estimación orientativa de **4–8 horas de trabajo**, sujeta a la integración de capacidad y los controles de infraestructura. SSH/sudo y dominio ya están resueltos. No es una predicción de cuota ni garantía de ausencia de errores.

## Estado comprobado

El MVP incluye acceso privado, proyectos y tareas/subtareas, disponibilidad, planificación y replanificación, Hoy, sesiones con pausa/reanudación/cierre, aviso de fin, ampliación e historial. Conserva React, TypeScript, pnpm y SCSS; Java/Spring Boot con arquitectura hexagonal y EDA; PostgreSQL y RabbitMQ en un monorepo.

Las funcionalidades1–18 están fusionadas. PR17 incorporó Historial; PR19 corrigió un fixture de sesión. La CI posterior34120565608 terminó SUCCESS sobre a5d158621bc1eb1ab4f57cbb3161289525d6a851. El dictamen de Historial está en progress/judge_history_final.md.

Validación final local18:2.217 pruebas Java/93suites,1.899frontend/40suites y47Node; lint y build;129E2E completos y13comprobaciones del publicador. Los39escenarios y142ejemplos del contrato no son un recuento de pruebas. PIT original263/284 y Stryker original621/738 superan80; Stryker conserva tres RuntimeError fuera de su denominador oficial. Replays separados15/17 y32/33 detectan los seis y diecisiete objetivos respectivos. No se suman campañas ni se ocultan supervivientes.

UX conserva495mediciones y27axe sin incidencias en evidencia compuesta; teclado Chromium/Firefox y zoom nativo Chromium. WebKit Windows acredita geometría por clic y texto200, no teclado de enlaces. No hay pruebas de dispositivos/lectores físicos ni estudios humanos que certifiquen universalmente las30leyes.

## Trabajo pendiente

| Hito | Resultado verificable | Estimación |
| --- | --- | --- |
| Integración remota del MVP | CI previa, fusión y CI posterior verificadas | Terminada |
| Despliegue condicionado | Imágenes inmutables, Swarm, HTTPS, persistencia, respaldo/restauración y reversión | 4–8 horas adicionales |
| Acceso y dominio | SSH/sudo y DNS organizacion.apptolast.com comprobados | Resueltos |

Este rango sustituye las2–4horas anteriores por los hitos ya acreditados: refuerzos, replays, UX,129E2E y nuevo init final. No se descuenta automáticamente tiempo ni se divide entre agentes. No quedan cambios funcionales pendientes del contrato18.

## Dependencias del despliegue

Todavía no hay despliegue productivo acreditado. SSH/sudo funciona y el DNS apunta a159.195.156.57. El host tiene15981MiB de RAM; se observaron5788MiB disponibles y16servicios1/1. El presupuesto completo de infraestructura sólo deja45MiB, aunque observability no está desplegado: se revisa un perfil explícito de capacidad para la aplicación, conservando reservas y controles.

Las imágenes inmutables del MVP están publicadas. La comprobación local con archivos configtree verificó sesión, escritura, publicación y conservación tras reiniciar API/PG/Rabbit. Falta integrar el nuevo stack en Swarm y verificar HTTPS, permisos de secretos y recuperación en producción. La ruta/healthz deNginx no acredita por sí sola API, PostgreSQL o broker. Véanse docs/deployment-readiness.md y progress/review_mvp_deployment_handoff.md.

## Alcance posterior

La funcionalidad19 está en desarrollo y20–30 siguen autorizadas después del MVP: revisión semanal, personalización, vistas/campos, importación/exportación, API de integración, webhooks, calendarios, GitHub, conectores y automatizaciones. Necesitan contratos propios. El inventario de proveedores debe acotarse antes de estimar el proyecto completo. No se incluyen silenciosamente en la entrega1–18 ni se promete completarlas con una recarga concreta.
