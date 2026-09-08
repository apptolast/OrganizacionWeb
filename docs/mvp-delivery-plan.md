# Plan de entrega

Actualizado el 8 de septiembre de 2026. Las funcionalidades 1–21 están desplegadas y verificadas en https://organizacion.apptolast.com. Exportación22 es la siguiente entrega;22–30 siguen pendientes. Este recuento de funcionalidades no mide un porcentaje de esfuerzo ni garantiza ausencia de errores.

## Estado comprobado

El MVP incluye acceso privado, proyectos y tareas/subtareas, disponibilidad, planificación y replanificación, Hoy, sesiones con pausa/reanudación/cierre, aviso de fin, ampliación, historial y revisión semanal. Conserva React, TypeScript, pnpm y SCSS; Java/Spring Boot con arquitectura hexagonal y EDA; PostgreSQL y RabbitMQ en un monorepo.

Revisión semanal19: CI34139336203 verde antes de la fusiónPR20; validación local2289Java,1968frontend,51Node,136E2E y13comprobaciones del publicador. PIT original178/190 y Stryker estricto615/764 superan80%; los replays se documentan aparte y no se suman a la campaña original. Los informes de revisión, mutación y UX están en progress. No se atribuye una certificación universal de UX por estas pruebas.

## Entrega en el servidor

SSH/sudo y DNS organizacion.apptolast.com están resueltos. Edge está aplicado y HTTPS funciona. La capacidad dispone de un perfil explícito que conserva los servicios existentes. Las imágenes se publican por digest; las credenciales se conservan protegidas, fuera de Git.

El corte publicado 4d34b9c incluye la revisión semanal y corrige los permisos de los directorios temporales de Nginx. Infraestructura 491e2c2 utiliza montajes largos compatibles con Swarm. Tras corregir el montaje y repetir el despliegue con la API saludable, la aplicación terminó con 38 tareas correctas, 3 cambios y ningún fallo. Una segunda ejecución convergió con 38 tareas correctas, cero cambios y los mismos cuatro contenedores. PR21 de aplicación y PR29 de infraestructura están fusionadas con CI verde. Se conserva la evidencia de los intentos fallidos anteriores.

La aceptación autenticada por HTTPS comprobó proyecto y tarea persistentes, reserva y cancelación, inicio, pausa, reanudación y cierre de trabajo, recuperación idempotente del cierre, historial y revisión semanal. Los datos sintéticos se conservan identificados; no queda una sesión activa ni una reserva futura de esta aceptación. Los nueve eventos aparecen publicados en el outbox y las colas contienen los recuentos esperados; no se consumieron sus mensajes para acreditar sus cuerpos. Los 16 servicios anteriores mantienen 1/1 y las ocho rutas web conservan sus códigos previos.

Se creó una copia PostgreSQL real en el servidor y se restauró en una instancia local aislada: 18 migraciones, proyecto, tarea, sesión y nueve eventos recuperados, con las huellas de sesión e intervalos coincidentes. El entorno de restauración se retiró. Esto no acredita copias externas automáticas, recuperación RabbitMQ ni custodia externa del servidor. Las credenciales se guardan cifradas fuera del repositorio; el usuario dispone de un ayudante local para copiar su contraseña sin mostrarla en el chat.

## Orden de trabajo

1. Vistas y campos21 cerrada: mutación incremental completa81,91% conservador,2.241 pruebas frontend, CI34173869406 con151 E2E y publicador verdes. PR24/infra33 fusionadas; apply38/5/0, aceptación HTTPS y Chromium completa dentro del alcance documentado. La campaña original76,64% se conserva y no se reescribe. Siguiente entrega: exportación22 con contrato propio.
2. Conservar la aceptación del MVP y completar las obligaciones operativas de respaldo externo y recuperación que siguen pendientes.
3. Continuar22–30 con contratos acotados: exportación22 antes de importación23; credenciales/API24 antes de webhooks25; calendarioICS26; después GitHub27, calendario externo28, otros conectores29 y automatizaciones30. Definir proveedor, dirección de sincronización, permisos y conflictos antes de cada integración. La autorización global del usuario permite continuar sin repetir la aprobación de cada contrato.

La mejora DNS está desplegada con aplicación4d9469a y catálogo770b736. PR22 y PR30 fusionadas con CI verde; check27/2/0 y apply38/5/0, ambos liberados correctamente. API/web terminaron su actualización, PostgreSQL/Rabbit conservaron sus contenedores, los20servicios están1/1 y las ocho rutas anteriores mantienen sus respuestas. La aceptación HTTPS posterior conserva sesión cerrada, tiempo neto, historial y revisión semanal. La sesión privada utilizada para verificarlo se cerró correctamente.

## Estimaciones y límites

El MVP ya se puede usar, incluida apariencia persistente20 y vistas/campos21 (release dfac90edcabdf04e442b906f0ab6db8894cbc4b2). La aceptación, evidencias y límites están en progress/judge_appearance.md y progress/judge_custom_views_fields.md. Los datos anteriores y los16 servicios ajenos a la aplicación se conservan.

Como referencia de tiempo observado, apariencia20 ocupó unas3h27 desde contrato hasta cierre;21 superaba4h16 al corte de esta revisión y seguía en validación. Incluyen trabajo paralelo y esperas; no son horas de CPU ni una velocidad garantizada. La campaña original de mutación frontend21 duró44min8s. El modo incremental oficial puede reutilizar pruebas intactas, pero su duración y resultado se medirán al ejecutarlo.

Estimación provisional de baja confianza para planificación:22–26 podrían requerir15–30 horas adicionales de ejecución y validación;27,28 y30 otras15–35 horas si proveedores y permisos están disponibles. No incluye29, cuyo inventario sigue abierto, ni esperas externas ni hallazgos nuevos. Es un orden de magnitud de varias jornadas, no un compromiso de completar todo hoy. Con cuota limitada, el siguiente resultado prioritario es exportación22. No hay garantía de terminar el alcance completo con una recarga o cuota concreta.
