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

1. Cerrar la mejora de resolución DNS de Nginx para que el proxy arranque y se recupere aunque la API todavía no esté disponible. El ensayo local pasó; CI detectó una incompatibilidad del fixture de cambio de IP que se está corrigiendo. Esta mejora aún no está desplegada.
2. Conservar la aceptación del MVP y completar las obligaciones operativas de respaldo externo y recuperación que siguen pendientes.
3. Terminar apariencia20: persistencia y concurrencia, contrato HTTP, estado compartido y formulario, SCSS, integración, pruebas de navegador, UX y mutación.
4. Continuar21–30 con contratos acotados: vistas/campos, exportación/importación, API de integración, webhooks, calendarios, GitHub, otros conectores y automatizaciones.

## Estimaciones y límites

El MVP ya se puede usar. Apariencia tiene el backend integrado y una regresión de 2.384 pruebas Java verde; la mutación original está en curso y quedan la integración visual, regresiones del frontend, pruebas de navegador, UX y mutación de la interfaz. Los trabajos avanzados requieren contratos y proveedores concretos, por lo que no hay una estimación total fiable ni una garantía de terminar con una recarga o cuota determinada. Los plazos anteriores de despliegue quedan sustituidos por este estado comprobado.
