# Smoke real de inicio de trabajo

Alcance autorizado: sólo scripts/publisher-smoke.mjs, reutilizando stack aislado PostgreSQL/RabbitMQ y recorrido histórico existente. Backend core/persistencia está congelado con63pruebas verdes; HTTP7186fc3 se integra y su wiring se valida por otro autor. No se ejecuta Docker sobre un corte RED.

Nuevo caso @s25/@s26 preparado: un relay HTTP local reenvía una única intención autenticada y destruye la respuesta después de observar201 del servidor. El cliente debe recibir error de transporte; no se usa el body descartado para reconstruir el recibo. GET by-request recupera los siete campos. Con broker detenido, outbox conserva pending/BROKER_UNAVAILABLE; al volver Rabbit debe publicarse el mismo eventId/payload en la novena ruta y cola durable quorum. Sólo después de verificar el mensaje real se retira el evento publicado de la base efímera y se reinicia backend; GET by-request y active deben conservar el mismo recibo sin recrear outbox.

Preparación: node --check2bc4ba/a13192 y formato focal41685d. Todavía no hay resultado de ejecución ni RED/GREEN atribuido. No cambios backend, configuración, dependencias ni pruebas E2E ajenas. El recorrido precedente del mismo script proporciona regresión pertinente de los seis eventos históricos que ya cubría.

Review previo root incorporado antes de ejecutar: payload projectId/taskId/plannedMinutes/ownerId/schema/type concordantes con recibo; fechas UTC con máximo seis decimales y duración25minutos; conteos SQL de la única sesión del propietario y su evento antes de recuperación y después de reinicio. Formato focal y sintaxis3cfecd. El GET que sigue al reinicio debe dejar outbox ausente, sin afirmar entrega por el mero DELETE. Wiring nominal real del autor2f10db disponible, captura espera formato y selector Gradle estables.

## Resultado final

Wiring freeze104b16 (75/75 del autor) y selector Gradle estable antes de Docker. Campaña única iniciada d2f0c2, sesión24155. Primer evento histórico PASS85dc83; seis recuperaciones históricas PASS e4d277; reinicio Rabbit con mensajes/topología persistentes y recuperación de historial después de reinicio backend PASS48334b. Caso14 y proceso completo PASS3e2f05, EXIT0. El stack y su volumen efímero se retiraron mediante el finally existente; no hay proceso smoke activo.

El nuevo caso resultó inicialmente GREEN sobre backend/publicador ya implementados; no se fabricó RED ni se modificó producción para este refuerzo. Se observó el error de transporte del cliente y201 upstream antes de cortar la respuesta. GET key recuperó el recibo, una sesión y un evento. El evento pasó de pending/BROKER_UNAVAILABLE a published conservando identidad y payload, y se verificó realmente en RabbitMQ con delivery_mode2, once campos, cola durable quorum y binding work-session.started.v1. Sólo después se eliminó el outbox publicado propio. Tras reinicio backend, GET key y active devolvieron el recibo idéntico, una sesión y cero eventos, sin segundo inicio ni reescritura por GET.

Límites: este script acredita los seis tipos históricos que ya probaba y WorkSessionStarted.v1; no vuelve a medir por este flujo los eventos11/13. La pérdida de respuesta es una interrupción controlada del relay local, no una caída física de red. El reinicio usa contenedor backend real y misma base; no certifica recuperación de una máquina física. No hubo una nueva campaña de mutación ni suites globales.

SHA256 scripts/publisher-smoke.mjs: 4CC55B6F31984ACB67F72233F7EC76AF7FB2D83035F3C29A8E2106A073A18C74. Fuentes/test/config backend permanecieron congelados durante ejecución. Entrega para revisión independiente, no declaración de feature terminada.
