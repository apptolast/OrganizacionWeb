# Plan de entrega del MVP

Actualizado el 7 de septiembre de 2026, tras el cierre técnico local de Historial. **Software del MVP1–18 terminado y validado localmente.** Pendiente: CI, fusión y CI posterior en GitHub; estimación orientativa **1–2 horas**, incluida corrección de incidencias remotas, sin garantía. El despliegue mantiene **4–8 horas adicionales**, condicionado a acceso, hostname y capacidad. No es una predicción de cuota ni garantía de ausencia de errores.

## Estado comprobado

El MVP incluye acceso privado, proyectos y tareas/subtareas, disponibilidad, planificación y replanificación, Hoy, sesiones con pausa/reanudación/cierre, aviso de fin, ampliación e historial. Conserva React, TypeScript, pnpm y SCSS; Java/Spring Boot con arquitectura hexagonal y EDA; PostgreSQL y RabbitMQ en un monorepo.

Las funcionalidades1–17 están fusionadas. La PR16 incorporó17 en56b91bee09d332eda27a016eea34d20f639992ab; CI previa34100084803 y posterior34101887939 verdes. La funcionalidad18 está cerrada técnicamente en la rama de PR17, aún sin atribuir fusión ni CI final. Su dictamen está en progress/judge_history_final.md.

Validación final local18:2.217 pruebas Java/93suites,1.899frontend/40suites y47Node; lint y build;129E2E completos y13comprobaciones del publicador. Los39escenarios y142ejemplos del contrato no son un recuento de pruebas. PIT original263/284 y Stryker original621/738 superan80; Stryker conserva tres RuntimeError fuera de su denominador oficial. Replays separados15/17 y32/33 detectan los seis y diecisiete objetivos respectivos. No se suman campañas ni se ocultan supervivientes.

UX conserva495mediciones y27axe sin incidencias en evidencia compuesta; teclado Chromium/Firefox y zoom nativo Chromium. WebKit Windows acredita geometría por clic y texto200, no teclado de enlaces. No hay pruebas de dispositivos/lectores físicos ni estudios humanos que certifiquen universalmente las30leyes.

## Trabajo pendiente

| Hito | Resultado verificable | Estimación |
| --- | --- | --- |
| Integración remota | CI del corte final, fusión y CI posterior sin incidencias | 1–2 horas orientativas |
| Despliegue condicionado | Imágenes inmutables, Swarm, HTTPS, persistencia, respaldo/restauración y reversión | 4–8 horas adicionales |
| Esperas externas | Acceso, hostname, DNS o capacidad que requieran intervención | Duración desconocida |

Este rango sustituye las2–4horas anteriores por los hitos ya acreditados: refuerzos, replays, UX,129E2E y nuevo init final. No se descuenta automáticamente tiempo ni se divide entre agentes. No quedan cambios funcionales pendientes del contrato18.

## Dependencias del despliegue

Todavía no hay despliegue productivo acreditado. El accesoSSH y hostname definitivo ya están solicitados; el último intento aadmin@159.195.156.57 terminó Permission denied(publickey). No existe medición remota nueva. Los45MiB del documento de infraestructura son margen de límites presupuestados, no memoria libre actual.

Existen Dockerfiles, Compose y overlayRabbitMQ. Falta publicar imágenes, integrarlas en el catálogoSwarm y verificar recursos actuales, DNS, HTTPS, secrets, persistencia y recuperación. La ruta/healthz deNginx no acredita por sí sola API, PostgreSQL o broker. Véanse docs/deployment-readiness.md y progress/review_mvp_deployment_handoff.md.

## Alcance posterior

Las funcionalidades19–30 siguen autorizadas después del MVP: revisión semanal, personalización, vistas/campos, importación/exportación, API de integración, webhooks, calendarios, GitHub, conectores y automatizaciones. Necesitan contratos propios. El inventario de proveedores debe acotarse antes de estimar el proyecto completo. No se incluyen silenciosamente en la entrega1–18 ni se promete completarlas con una recarga concreta.
