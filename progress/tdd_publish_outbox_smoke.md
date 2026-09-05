# publish_outbox — smoke de stack

## Frontera y arranque

Autor: agente frontend_craftsman, sin cambios de producción backend/broker. El coordinador revisa este tooling de forma independiente; su autor no emite veredicto sobre su propio smoke.

Init propio 67999 detectó formato Java temporalmente rojo mientras los responsables implementaban TDD. Coordinador autorizó continuar este subtrabajo aislado sobre el init raíz 55414 previamente verde de la misma feature aprobada; no se reformatearon archivos ajenos. La verificación final completa sigue siendo obligatoria.

## Ciclo 1 — @s1

Un único test inicial en scripts/publisher-smoke.mjs: stack Compose separado y real, POST autenticado, eventual estado published, mismo JSON en RabbitMQ, message-id/content-type/persistencia y registro original con published_at/attempts. Rojo real en ejecución 7274, exit1: `Timed out: @s1 background publisher marks original event published`. API respondió201; consulta SQL independiente mostró pending|0 mientras no existía aún disparador en background. Stack completo estaba healthy. Cleanup terminó sin error. Verde pendiente de conexión del worker por su responsable.

Revisión temprana del coordinador incorporada: env temporal únicamente con claves sintéticas explícitas (sin copiar secretos homónimos del host); imagen probe node:22.23.2-alpine3.23; espera acotada para disponibilidad management separada de las aserciones de contenido. Comando `pnpm test:publisher`; CI lo ejecuta después de los ocho E2E existentes.

Fixture: nombre de proyecto aleatorio, volúmenes exclusivos, credenciales sintéticas aleatorias, puerto web loopback efímero. RabbitMQ y probe Node internos sin puertos públicos. El probe usa management únicamente como recolector del test con ack_requeue_true; no consumidor de producto. Cleanup Compose por proyecto propio y eliminación de carpeta temporal tras validar ruta absoluta dentro de .e2e-work. La salida de fallos Docker se omite para no divulgar credenciales del fixture.

Verde real del ciclo1: ejecución52325 exit0 tras incorporar el worker su responsable; POST, PostgreSQL y RabbitMQ reales cumplen todas las aserciones @s1.

## Ciclo 2 — @s9/@s16

Añadido solo después del primer verde: parar Rabbit, crear proyecto con plazo HTTP4.5s (inferior a la espera confirm5s), observar pending con fallo transitorio persistido, arrancar mismo broker y comprobar publicación automática conservando eventId y payload. La producción correspondiente ya cuenta con TDD del responsable backend; esta es evidencia adicional de stack, no se atribuye un rojo no observado.

Verde real ciclo2: ejecución17977 exit0, ambos PASS (@s1 y @s16/@s9). No se cambiaron fuentes de producción para este refuerzo del stack ya implementado.

## Ciclo 3 — @s14/@s20

Añadido tras verde anterior: detener backend para impedir recreación/publicación que falsee persistencia; reiniciar Rabbit manteniendo volumen. Comprobar ambos mensajes originales y sus metadatos, cola quorum durable sin autodelete/exclusive/consumidores, exchange direct durable, binding acordado y filas PostgreSQL idénticas. Ejecución78743: mensajes y metadatos retenidos pasan; fallo del fixture al exigir queue.consumers=0 antes de que Rabbit publique estadísticas (campo undefined). Se corrige usando endpoint consumers del vhost y array vacío; no se cambia producción. Revisión del coordinador: esperar de forma acotada presencia de eventId, dejando aserciones JSON/metadatos fuera del catch. Nueva ejecución pendiente. Los ocho E2E base permanecen intactos.

Verde completo: ejecución8008 exit0, tres etapas PASS. Publicación real, API independiente durante broker parado, recuperación automática con identidad estable y reinicio de Rabbit conservando mensajes/topología con backend detenido. Syntax check correcto; cleanup sin errores. No se simularon confirmaciones en este smoke. Los escenarios de caída del proceso antes/después del envío pertenecen a tests backend separados, no se atribuyen a este runner.

## Trazabilidad de esta frontera

- @s1 → etapa POST / published / assertMessage.
- @s9 y @s16 → etapa stop/start rabbitmq / pendiente con fallo / recuperación misma identidad.
- @s14 → restart rabbitmq con backend detenido / mensajes y topología originales.
- @s20 → reconexión del worker al recuperar Rabbit conserva también el mensaje previo, comprobado en etapa final.
- Otros escenarios → responsables backend/broker; no declarar cobertura a partir de este smoke.

Repetición final por el coordinador (sesión49506): pnpm test:e2e y pnpm test:publisher, exit0; ocho pruebas de navegador y tres bloques de publicación/caída/reinicio verdes sobre árbol congelado.
