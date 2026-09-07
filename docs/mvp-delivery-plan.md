# Plan de entrega

Actualizado el 7 de septiembre de 2026, a las18:43. Las funcionalidades1–19 están validadas y fusionadas en mainc20105a. La apariencia persistente20 está en desarrollo;21–30 siguen pendientes. Este recuento de funcionalidades no mide un porcentaje de esfuerzo ni garantiza ausencia de errores.

## Estado comprobado

El MVP incluye acceso privado, proyectos y tareas/subtareas, disponibilidad, planificación y replanificación, Hoy, sesiones con pausa/reanudación/cierre, aviso de fin, ampliación, historial y revisión semanal. Conserva React, TypeScript, pnpm y SCSS; Java/Spring Boot con arquitectura hexagonal y EDA; PostgreSQL y RabbitMQ en un monorepo.

Revisión semanal19: CI34139336203 verde antes de la fusiónPR20; validación local2289Java,1968frontend,51Node,136E2E y13comprobaciones del publicador. PIT original178/190 y Stryker estricto615/764 superan80%; los replays se documentan aparte y no se suman a la campaña original. Los informes de revisión, mutación y UX están en progress. No se atribuye una certificación universal de UX por estas pruebas.

## Entrega en el servidor

SSH/sudo y DNS organizacion.apptolast.com están resueltos. Edge está aplicado y HTTPS funciona. La capacidad dispone de un perfil explícito que conserva los servicios existentes. Las imágenes se publican por digest; las credenciales se conservan protegidas, fuera de Git.

El primer apply creó PostgreSQL y RabbitMQ, pero API/web no convergieron: Swarm descartó los montajes temporales abreviados. Una prueba real detectó además permisos insuficientes de los directorios de Nginx. El corte4d34b9c incluye la revisión semanal y la corrección mínima de la imagen. Infra491e2c2 usa montajes largos;15pruebas focales y lint pasaron. El check real terminó27ok/2changed/0failed. Falta aplicar este corte tras sus verificaciones remotas y ejecutar la aceptación autenticada.

Los16servicios anteriores mantienen1/1 y las ocho rutas web conservan sus códigos previos. El ensayo local de restauración PostgreSQL pasó; no acredita por sí mismo copias externas, recuperación RabbitMQ ni escrow del servidor. Estas obligaciones operativas se mantienen visibles.

## Orden de trabajo

1. Completar el despliegue corregido y verificar HTTPS, login, proyectos, tareas, planificación, pausa/reanudación/cierre, historial y revisión semanal con datos sintéticos identificados.
2. Comprobar publicación de eventos, persistencia y convergencia; documentar acceso, respaldo y reversión sin eliminar datos.
3. Terminar apariencia20: persistencia y concurrencia, contrato HTTP, estado compartido y formulario, SCSS, integración, pruebas de navegador, UX y mutación.
4. Continuar21–30 con contratos acotados: vistas/campos, exportación/importación, API de integración, webhooks, calendarios, GitHub, otros conectores y automatizaciones.

## Estimaciones y límites

El rango anterior de4–8horas era una previsión de despliegue, no una cuenta atrás ni una promesa del proyecto completo. Ahora quedan la convergencia del corte corregido y su aceptación real; cualquier fallo observado se diagnostica antes de publicar un nuevo plazo. Los trabajos avanzados todavía requieren contratos y proveedores concretos, por lo que no hay una estimación total fiable ni una garantía de terminar con una recarga o cuota determinada.
