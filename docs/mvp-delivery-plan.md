# Plan de entrega

Actualizado el 7 de septiembre de 2026. Las funcionalidades 1–19 están desplegadas y verificadas en https://organizacion.apptolast.com. La apariencia persistente 20 está en desarrollo; 21–30 siguen pendientes. Este recuento de funcionalidades no mide un porcentaje de esfuerzo ni garantiza ausencia de errores.

## Estado comprobado

El MVP incluye acceso privado, proyectos y tareas/subtareas, disponibilidad, planificación y replanificación, Hoy, sesiones con pausa/reanudación/cierre, aviso de fin, ampliación, historial y revisión semanal. Conserva React, TypeScript, pnpm y SCSS; Java/Spring Boot con arquitectura hexagonal y EDA; PostgreSQL y RabbitMQ en un monorepo.

Revisión semanal19: CI34139336203 verde antes de la fusiónPR20; validación local2289Java,1968frontend,51Node,136E2E y13comprobaciones del publicador. PIT original178/190 y Stryker estricto615/764 superan80%; los replays se documentan aparte y no se suman a la campaña original. Los informes de revisión, mutación y UX están en progress. No se atribuye una certificación universal de UX por estas pruebas.

## Entrega en el servidor

SSH/sudo y DNS organizacion.apptolast.com están resueltos. Edge está aplicado y HTTPS funciona. La capacidad dispone de un perfil explícito que conserva los servicios existentes. Las imágenes se publican por digest; las credenciales se conservan protegidas, fuera de Git.

El corte publicado 4d34b9c incluye la revisión semanal y corrige los permisos de los directorios temporales de Nginx. Infraestructura 491e2c2 utiliza montajes largos compatibles con Swarm. Tras corregir el montaje y repetir el despliegue con la API saludable, la aplicación terminó con 38 tareas correctas, 3 cambios y ningún fallo. Una segunda ejecución convergió con 38 tareas correctas, cero cambios y los mismos cuatro contenedores. PR21 de aplicación y PR29 de infraestructura están fusionadas con CI verde. Se conserva la evidencia de los intentos fallidos anteriores.

La aceptación autenticada por HTTPS comprobó proyecto y tarea persistentes, reserva y cancelación, inicio, pausa, reanudación y cierre de trabajo, recuperación idempotente del cierre, historial y revisión semanal. Los datos sintéticos se conservan identificados; no queda una sesión activa ni una reserva futura de esta aceptación. Los nueve eventos aparecen publicados en el outbox y las colas contienen los recuentos esperados; no se consumieron sus mensajes para acreditar sus cuerpos. Los 16 servicios anteriores mantienen 1/1 y las ocho rutas web conservan sus códigos previos.

Se creó una copia PostgreSQL real en el servidor y se restauró en una instancia local aislada: 18 migraciones, proyecto, tarea, sesión y nueve eventos recuperados, con las huellas de sesión e intervalos coincidentes. El entorno de restauración se retiró. Esto no acredita copias externas automáticas, recuperación RabbitMQ ni custodia externa del servidor. Las credenciales se guardan cifradas fuera del repositorio; el usuario dispone de un ayudante local para copiar su contraseña sin mostrarla en el chat.

## Orden de trabajo

1. Terminar apariencia20: completar SCSS, integración, pruebas de navegador, UX y mutación de la interfaz. Backend, HTTP y recuperación real tras reiniciar la API ya están revisados.
2. Conservar la aceptación del MVP y completar las obligaciones operativas de respaldo externo y recuperación que siguen pendientes.
3. Continuar21–30 con contratos acotados: vistas/campos, exportación/importación, API de integración, webhooks, calendarios, GitHub, otros conectores y automatizaciones.

La mejora DNS está desplegada con aplicación4d9469a y catálogo770b736. PR22 y PR30 fusionadas con CI verde; check27/2/0 y apply38/5/0, ambos liberados correctamente. API/web terminaron su actualización, PostgreSQL/Rabbit conservaron sus contenedores, los20servicios están1/1 y las ocho rutas anteriores mantienen sus respuestas. La aceptación HTTPS posterior conserva sesión cerrada, tiempo neto, historial y revisión semanal. La sesión privada utilizada para verificarlo se cerró correctamente.

## Estimaciones y límites

El MVP ya se puede usar. Apariencia tiene el backend integrado, una regresión de 2.384 pruebas Java verde y mutación original153/159 (96,23%). Tres refuerzos posteriores verifican los cuatro cambios observables que faltaban, mediante un replay separado; no se suman campañas. Quedan la integración visual, regresiones finales, UX y mutación de la interfaz. Los trabajos avanzados requieren contratos y proveedores concretos, por lo que no hay una estimación total fiable ni una garantía de terminar con una recarga o cuota determinada. Los plazos anteriores de despliegue quedan sustituidos por este estado comprobado.
