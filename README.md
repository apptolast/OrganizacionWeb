# OrganizationWeb

Organización personal por proyectos y sesiones de trabajo acotadas: decidir qué hacer, reservar tiempo y conservar un historial del avance. React + TypeScript + SCSS, pnpm, Java + Spring Boot y Gradle Kotlin DSL.

## Estado del producto

La web permite crear proyectos propios, recuperarlos en una lista persistente de 20 elementos por página, abrir su detalle y editar nombre y descripción. También permite activar, pausar, terminar y reabrir proyectos en pausa. La edición y los estados comparten control de versión para proteger cambios concurrentes. PostgreSQL conserva proyectos y eventos de forma atómica; el publicador RabbitMQ añade entrega confirmada, reintentos y recuperación conservando la identidad del evento. El estado de verificación de cada entrega está en el [roadmap](feature_list.json).

Rutas: `/` conserva el formulario de creación; `/proyectos` muestra la lista privada; `/proyectos/{id}` muestra nombre, descripción, estado, fechas y acciones de estado; `/proyectos/{id}/editar` permite guardar cambios con control de versión. Un conflicto ofrece recargar deliberadamente la versión guardada. Tareas, calendario y temporizador siguen en desarrollo; el MVP completo todavía no está terminado.

El límite inicial es de tres proyectos activos por propietario. `APP_MAX_ACTIVE_PROJECTS` permite elegir de 1 a 10 en `.env`; todas las réplicas deben usar el mismo valor. Al alcanzar el límite, el propietario decide qué pausar. Reducirlo no pausa proyectos automáticamente y dos activaciones simultáneas no pueden ocupar la misma última plaza.

## Ejecutar localmente

Requisitos: Docker con Compose. Crear `.env` a partir de `.env.example` y completar usuario/contraseña de base de datos y de acceso. No hay credenciales predeterminadas. Los valores permanecen fuera de Git.

```sh
docker compose up --build -d
```

Abrir http://127.0.0.1:8080 (o WEB_PORT configurado). El acceso utiliza el formulario de usuario y contraseña configurados. La sesión se guarda en PostgreSQL, caduca tras treinta minutos de inactividad y puede cerrarse desde la web. La web y API comparten origen; PostgreSQL no publica puerto al host. Los datos quedan en un volumen persistente.

```sh
docker compose down
```

Para HTTPS, configura `APP_PUBLIC_ORIGIN` con el origen público exacto, sin ruta ni barra final. La cookie utiliza Secure en HTTPS; HTTP sólo se admite para desarrollo en loopback. No se aceptan cabeceras del cliente para decidir esa política.

Ese comando conserva datos. No añadir `--volumes` si se quieren conservar proyectos.

## Desarrollo y verificación

### Publicador de eventos opcional

El Compose base mantiene el publicador desactivado. Para habilitar RabbitMQ, completa `RABBITMQ_USERNAME` y `RABBITMQ_PASSWORD` en `.env` y ejecuta:

```sh
docker compose -f docker-compose.yml -f deploy/compose.publisher.yml up --build -d --wait
```

El broker usa colas quorum durables para ProjectCreated, ProjectUpdated y ProjectStatusChanged y un volumen persistente. AMQP y su administración permanecen en la red interna de Docker. La API puede crear, editar y cambiar el estado de proyectos aunque el broker esté caído; el outbox conserva los eventos y reintenta con espera creciente hasta 60 segundos. La entrega es al menos una vez: un consumidor debe deduplicar por `eventId`.

Para detener esta instancia conservando sus datos, utiliza los mismos dos argumentos `-f` con `down`, sin `--volumes`. Las credenciales de RabbitMQ inicializan el volumen la primera vez; modificar `.env` no rota un usuario existente.

La prueba de integración `pnpm test:publisher` utiliza otra instancia aislada, credenciales sintéticas y un puerto local efímero automático. El script elimina únicamente sus propios contenedores y volúmenes. El recorrido existente `pnpm test:e2e` sigue verificando el modo sin broker en el puerto 18080.

Requisitos adicionales: JDK 25, Node.js 22.12 o posterior y pnpm 10.21.0. Gradle se descarga mediante el wrapper versionado. Docker debe estar disponible para pruebas de PostgreSQL real.

```sh
node scripts/project.mjs install
pnpm exec playwright install chromium
pnpm test
pnpm lint
pnpm build
pnpm mutate
pnpm test:e2e
```

`pnpm test:e2e` crea su propia instancia aislada y elimina solamente sus contenedores y datos al finalizar. Usa credenciales exclusivas de prueba. No ejecutar pruebas contra una base productiva.

El arnés SDD conserva sus comandos:

```sh
node .harness/harness.mjs init
node .harness/harness.mjs verify
```

Las pruebas de producto y sus resultados concretos se registran en `progress/`; que exista un comando o un workflow no significa que ya haya pasado. Mutación con umbral 80 % y revisión independiente antes de marcar una feature como done.

## Arquitectura y documentación

- [Arquitectura](docs/architecture.md): monorepo, dominio puro, aplicación, puertos y adaptadores.
- [Publicación y recuperación de eventos](docs/outbox-publishing.md).
- [Especificación](project-spec.md) y [contrato de creación](features/create_project.feature).
- [Campos y recorrido](docs/product-fields.md).
- [Personalización, conectores e infraestructura](docs/implementation-proposal.md).
- [Proceso SDD](docs/workflow.md) y [plantilla original](docs/template-harness.md).

Las skills de React y revisión de interfaces están versionadas en `.agents/skills/`. Los plugins del agente no son los conectores del producto.

## Despliegue productivo

Este Compose sirve para ejecutar y verificar el producto localmente. El servidor se gobierna desde `apptolast/DockerSwarmInfrastrcture`: integrar allí rutas Traefik, DNS, secrets, imágenes, recursos y backups. No se ha desplegado OrganizationWeb en el servidor.

Antes de producción se requiere HTTPS, identidad operativa, configuración de secretos, revisión del presupuesto de capacidad, migraciones y prueba de restauración. No trasladar credenciales E2E al servidor. Consultar los hallazgos concretos en `docs/implementation-proposal.md`.
