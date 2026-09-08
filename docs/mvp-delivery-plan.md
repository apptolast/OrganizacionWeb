# Plan de entrega

Actualizado el 8 de septiembre de 2026. Las funcionalidades 1–23 están desplegadas y verificadas en https://organizacion.apptolast.com. Importación23 está aceptada en producción; 24–30 siguen pendientes de implementación. Este recuento de funcionalidades no mide un porcentaje de esfuerzo ni garantiza ausencia de errores.

Última entrega: producto 4c74e183, aplicación PR27 fusionada en 3f4c3ef e infraestructura PR38 en 183293b. Fuente 7712fc8 aplicada con 38 tareas correctas, cuatro cambios y cero fallos. La aceptación de importación usa una vista previa propia sin confirmar escrituras: 17 tablas conservadas, 20 servicios 1/1, 18 contenedores intactos y ocho rutas anteriores sin cambios. PIT de persistencia 316/330 KILLED (95,76 % conservador); demás campañas, CI y límites en progress/judge_import_data.md. La propuesta y los escenarios de API24 se prepararon en un checkout aislado durante la espera de mutación; no acreditan todavía implementación.

## Estado comprobado

El MVP incluye acceso privado, proyectos y tareas/subtareas, disponibilidad, planificación y replanificación, Hoy, sesiones con pausa/reanudación/cierre, aviso de fin, ampliación, historial y revisión semanal. Conserva React, TypeScript, pnpm y SCSS; Java/Spring Boot con arquitectura hexagonal y EDA; PostgreSQL y RabbitMQ en un monorepo.

Revisión semanal19: CI34139336203 verde antes de la fusiónPR20; validación local2289Java,1968frontend,51Node,136E2E y13comprobaciones del publicador. PIT original178/190 y Stryker estricto615/764 superan80%; los replays se documentan aparte y no se suman a la campaña original. Los informes de revisión, mutación y UX están en progress. No se atribuye una certificación universal de UX por estas pruebas.

## Entrega en el servidor

SSH/sudo y DNS organizacion.apptolast.com están resueltos. Edge está aplicado y HTTPS funciona. La capacidad dispone de un perfil explícito que conserva los servicios existentes. Las imágenes se publican por digest; las credenciales se conservan protegidas, fuera de Git.

El corte publicado 4d34b9c incluye la revisión semanal y corrige los permisos de los directorios temporales de Nginx. Infraestructura 491e2c2 utiliza montajes largos compatibles con Swarm. Tras corregir el montaje y repetir el despliegue con la API saludable, la aplicación terminó con 38 tareas correctas, 3 cambios y ningún fallo. Una segunda ejecución convergió con 38 tareas correctas, cero cambios y los mismos cuatro contenedores. PR21 de aplicación y PR29 de infraestructura están fusionadas con CI verde. Se conserva la evidencia de los intentos fallidos anteriores.

La aceptación autenticada por HTTPS comprobó proyecto y tarea persistentes, reserva y cancelación, inicio, pausa, reanudación y cierre de trabajo, recuperación idempotente del cierre, historial y revisión semanal. Los datos sintéticos se conservan identificados; no queda una sesión activa ni una reserva futura de esta aceptación. Los nueve eventos aparecen publicados en el outbox y las colas contienen los recuentos esperados; no se consumieron sus mensajes para acreditar sus cuerpos. Los 16 servicios anteriores mantienen 1/1 y las ocho rutas web conservan sus códigos previos.

Se creó una copia PostgreSQL real en el servidor y se restauró en una instancia local aislada: 18 migraciones, proyecto, tarea, sesión y nueve eventos recuperados, con las huellas de sesión e intervalos coincidentes. El entorno de restauración se retiró. Esto no acredita copias externas automáticas, recuperación RabbitMQ ni custodia externa del servidor. Las credenciales se guardan cifradas fuera del repositorio; el usuario dispone de un ayudante local para copiar su contraseña sin mostrarla en el chat.

## Orden de trabajo

1. Vistas y campos21 cerrada: mutación incremental completa81,91% conservador,2.241 pruebas frontend, CI34173869406 con151 E2E y publicador verdes. PR24/infra33 fusionadas; apply38/5/0, aceptación HTTPS y Chromium completa dentro del alcance documentado. La campaña original76,64% se conserva y no se reescribe. Exportación22 cerrada con 33 escenarios, CI final 34190090017 SUCCESS y aceptación live: dos descargas idénticas de 8568 bytes mediante un GET, sin alterar datos ni servicios. PR25/infra35 fusionadas; apply 38/4/0/0. Evidencia y límites en progress/judge_export_data.md y progress/export_live_acceptance.json.
2. Conservar la aceptación del MVP y completar las obligaciones operativas de respaldo externo y recuperación que siguen pendientes.
3. Continuar24–30 con contratos acotados: credenciales/API24 antes de webhooks25; calendarioICS26; después GitHub27, calendario externo28, otros conectores29 y automatizaciones30. Definir proveedor, dirección de sincronización, permisos y conflictos antes de cada integración. La autorización global del usuario permite continuar sin repetir la aprobación de cada contrato.

La mejora DNS está desplegada con aplicación4d9469a y catálogo770b736. PR22 y PR30 fusionadas con CI verde; check27/2/0 y apply38/5/0, ambos liberados correctamente. API/web terminaron su actualización, PostgreSQL/Rabbit conservaron sus contenedores, los20servicios están1/1 y las ocho rutas anteriores mantienen sus respuestas. La aceptación HTTPS posterior conserva sesión cerrada, tiempo neto, historial y revisión semanal. La sesión privada utilizada para verificarlo se cerró correctamente.

## Estimaciones y límites

El MVP ya se puede usar, incluidas exportación22 e importación23 (release 4c74e183e49afa6d280115b399dbaffedc7bfe7f), además de apariencia20 y vistas/campos21. Los datos anteriores y los16 servicios ajenos a la aplicación se conservan.

Como referencia de tiempo observado, apariencia20 ocupó unas3h27 desde contrato hasta cierre;21 superaba4h16 al corte de esta revisión y seguía en validación. Incluyen trabajo paralelo y esperas; no son horas de CPU ni una velocidad garantizada. La campaña original de mutación frontend21 duró44min8s. La campaña incremental oficial posterior terminó en21min10s con1494/1824 mutantes detectados (81,91% conservador), conservando el universo completo y las dos evidencias. CI de main34176109030 terminó también SUCCESS.

La antigua estimación de 22–26 queda obsoleta tras entregar 22 y 23; no se utiliza para predecir el trabajo restante. La campaña original de persistencia23 duró 2 h 21 min 10 s y la alternativa se retiró incompleta después de validar aquélla: no se mezclan sus resultados. La siguiente implementación es API24; siguen sin contrato cerrado algunos proveedores de 28–29 y las reglas de 30. No hay una estimación global fiable ni garantía de terminar el alcance completo hoy o con una recarga concreta. Se informará de implementación, validación y dependencias externas por separado.
