# OrganizationWeb

Organización personal por proyectos y sesiones de trabajo acotadas: decidir qué hacer, reservar tiempo y conservar un historial del avance. React + TypeScript + SCSS, pnpm, Java + Spring Boot y Gradle Kotlin DSL.

## Estado del producto

La web permite crear proyectos propios, recuperarlos en una lista persistente de 20 elementos por página, abrir su detalle y editar nombre y descripción. También permite activar, pausar, terminar y reabrir proyectos en pausa. La edición y los estados comparten control de versión para proteger cambios concurrentes. PostgreSQL conserva proyectos y eventos de forma atómica; el publicador RabbitMQ añade entrega confirmada, reintentos y recuperación conservando la identidad del evento. El estado de verificación de cada entrega está en el [roadmap](feature_list.json).

Rutas: `/` muestra **Hoy**; `/proyectos/nuevo` contiene el formulario de creación; `/proyectos` muestra la lista privada; `/proyectos/{id}` muestra nombre, descripción, estado, fechas y acciones de estado; `/proyectos/{id}/editar` permite guardar cambios con control de versión. Un conflicto ofrece recargar deliberadamente la versión guardada. La creación y lectura de tareas está implementada y verificada localmente: título, criterio de finalización opcional y estimación en minutos, con lista persistente paginada dentro del detalle del proyecto. Terminar el proyecto conserva sus tareas pendientes; añadir trabajo requiere reabrirlo en pausa. También permite dividir tareas en subtareas, consultar sus hijos directos y navegar al padre o al proyecto desde `/proyectos/{projectId}/tareas/{id}`. Cada estimación es independiente. Calendario y temporizador siguen pendientes; el MVP completo todavía no está terminado.

Completar y reabrir tareas está implementado y verificado localmente. Cada transición conserva su fecha en un historial paginado independiente del outbox. Los cambios simultáneos usan una versión propia de la tarea; ante conflicto, la web permite consultar deliberadamente el estado vigente. No se completa automáticamente el proyecto ni los descendientes. El cierre de calidad se registra en `complete_reopen_task` dentro del roadmap.

La disponibilidad personal está implementada y verificada localmente en `/disponibilidad`. Permite elegir zona horaria y minutos disponibles para cada día, incluido cero para descansar. Conserva la preferencia en PostgreSQL y protege cambios simultáneos. Estos presupuestos no reservan franjas ni acreditan trabajo realizado. El cierre y sus pruebas están en [el dictamen de disponibilidad](progress/judge_availability.md).

Los bloques horarios están implementados en el detalle de una tarea pendiente: configura la disponibilidad, abre **Planificar bloque**, indica objetivo, inicio, fin y zona, y revisa antes de guardar. La revisión muestra el presupuesto diario y exige aceptar explícitamente cualquier exceso. Las reservas propias no pueden solaparse, incluso entre proyectos. Si se pierde la respuesta al guardar, **Comprobar guardado** recupera el resultado sin crear otro bloque. Los bloques conservan sus instantes al cambiar preferencias o completar tareas; todavía no registran trabajo realizado ni ofrecen temporizador. Esta entrega está cerrada en el roadmap; [su dictamen de calidad](progress/judge_schedule_block.md) conserva las mediciones y sus límites.

**Hoy** reúne las reservas del día con su proyecto, tarea, objetivo y horario. Muestra presupuesto, tiempo planificado, exceso y cierre previsto; utiliza la zona configurada o explica el fallback UTC con capacidad desconocida. `GET /api/v1/today` lee una instantánea privada y consistente, sin generar eventos ni modificar las reservas. Esta funcionalidad está implementada y aún en validación: consulta [su dictamen](progress/judge_today.md) para las pruebas realizadas y los defectos pendientes. No acredita trabajo realizado ni completa tareas automáticamente.

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

El broker usa colas quorum durables para ProjectCreated, ProjectUpdated, ProjectStatusChanged, TaskCreated, SubtaskCreated, TaskStatusChanged y BlockPlanned, con un volumen persistente. AMQP y su administración permanecen en la red interna de Docker. La API puede guardar proyectos, tareas y bloques aunque el broker esté caído; el outbox conserva los eventos y reintenta con espera creciente hasta 60 segundos. La entrega es al menos una vez: un consumidor debe deduplicar por `eventId`.

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

Para medir sólo la entrega de bloques, conservando el umbral y el código compartido afectado:

```sh
node .harness/harness.mjs mutate schedule_block-backend
node .harness/harness.mjs mutate schedule_block-frontend
```

Para la agenda Hoy existen los targets `today-backend` y `today-frontend`, con informes separados y el mismo umbral.

Sin target se conserva la mutación completa de ambos componentes. Los targets no reconocidos se rechazan antes de lanzar procesos.

Las pruebas de producto y sus resultados concretos se registran en `progress/`; que exista un comando o un workflow no significa que ya haya pasado. Mutación con umbral 80 % y revisión independiente antes de marcar una feature como done.

## Arquitectura y documentación

- [Arquitectura](docs/architecture.md): monorepo, dominio puro, aplicación, puertos y adaptadores.
- [Publicación y recuperación de eventos](docs/outbox-publishing.md).
- [Especificación](project-spec.md) y [contrato de creación](features/create_project.feature).
- [Campos y recorrido](docs/product-fields.md).
- [Personalización, conectores e infraestructura](docs/implementation-proposal.md).
- [Proceso SDD](docs/workflow.md) y [plantilla original](docs/template-harness.md).

Las skills de React y revisión de interfaces están versionadas en `.agents/skills/`. Todos los agentes aplican también Ponytail full y Caveman lite siguiendo [su integración documentada](docs/agent-efficiency.md): reutilizar soluciones existentes, evitar complejidad innecesaria y comunicar con brevedad, conservando arquitectura, validación, seguridad y accesibilidad. Los plugins del agente no son los conectores del producto.

## Despliegue productivo

Este Compose sirve para ejecutar y verificar el producto localmente. El servidor se gobierna desde `apptolast/DockerSwarmInfrastrcture`: integrar allí rutas Traefik, DNS, secrets, imágenes, recursos y backups. No se ha desplegado OrganizationWeb en el servidor.

Antes de producción se requiere HTTPS, identidad operativa, configuración de secretos, revisión del presupuesto de capacidad, migraciones y prueba de restauración. No trasladar credenciales E2E al servidor. Consultar los hallazgos concretos en `docs/implementation-proposal.md`.
