# publish_outbox — integración

## Alcance y autorización

El coordinador confirmó aprobación humana del contrato publish_outbox e init55414 exit0 antes de editar. Esta frontera añade el stack local opcional y pruebas del publicador real; no toca producción backend/frontend ni despliega al servidor.

## Ciclo1: configuración de ejecución

- Rojo real: `docker compose -f docker-compose.yml -f deploy/compose.publisher.yml config --quiet` falla porque el override de publicación no existe.
- Verde: override local con RabbitMQ4.3.5-management-alpine, hostname estable, volumen persistente y sin puertos AMQP/management públicos. Config validada con credenciales sintéticas; credenciales reales obligatorias desde entorno.
- El Compose base fija OUTBOX_PUBLISHER_ENABLED=false. El override fija true y conexión interna. Así el primer corte conserva su regresión de eventos pendientes y no conecta al broker accidentalmente por variables del host.
- API/backend no depende de RabbitMQ healthy para arrancar: el broker caído no impide crear proyectos.

## Fuentes y decisiones

Imagen4.3.5-management-alpine verificada en página oficial Docker Hub RabbitMQ y acordada con responsable backend. La documentación oficial señala que el hostname estable conserva la ruta del nodo al usar volumen. Cola quorum durable/direct exchange declarados por el adaptador backend, no por scripts que oculten fallos del publicador. Gestión usada únicamente dentro del contenedor durante prueba; no añade consumidor de producto.

## Pendiente

Smoke real de publicación, caída/recuperación, persistencia del broker y regresión8E2E. No se declaran verificados por tener un Compose válido.

## Reparto posterior y documentación

El coordinador delegó después el adaptador RabbitMQ al mismo responsable de este log: su producción y ciclos viven en `progress/tdd_publish_outbox_broker.md`, separados de este registro de tooling. El runner `scripts/publisher-smoke.mjs`, package script y CI se delegaron a frontend_craftsman y su evidencia vive en `progress/tdd_publish_outbox_smoke.md`. README documenta activación por los dos ficheros Compose, credenciales requeridas, ausencia de puertos públicos del broker, volumen persistente y semántica al menos una vez. Ningún despliegue al servidor se ha ejecutado.
