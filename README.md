# OrganizationWeb

Organización personal por proyectos y sesiones de trabajo acotadas: decidir qué hacer, reservar tiempo y conservar un historial del avance. React + TypeScript + SCSS, pnpm, Java + Spring Boot y Gradle Kotlin DSL.

## Estado del producto

Primera feature implementada: crear un proyecto propio como idea, validar entradas y guardar proyecto y evento pendiente de forma atómica en PostgreSQL. El resto del [roadmap](feature_list.json) sigue pendiente. La tabla outbox prepara la entrega de eventos; el publicador RabbitMQ es el siguiente corte, todavía no implementado.

La interfaz inicial cubre creación y confirmación. Todavía no hay listado persistente, edición, calendario ni temporizador. No se muestran estadísticas de ejemplo como si fueran datos reales.

## Ejecutar localmente

Requisitos: Docker con Compose. Crear `.env` a partir de `.env.example` y completar usuario/contraseña de base de datos y de acceso. No hay credenciales predeterminadas. Los valores permanecen fuera de Git.

```sh
docker compose up --build -d
```

Abrir http://127.0.0.1:8080 (o WEB_PORT configurado). El acceso inicial usa autenticación HTTP Basic. La web y API comparten origen; PostgreSQL no publica puerto al host. Los datos quedan en un volumen persistente.

```sh
docker compose down
```

Ese comando conserva datos. No añadir `--volumes` si se quieren conservar proyectos.

## Desarrollo y verificación

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

- [Arquitectura](docs/architecture.md): dominio puro, aplicación, puertos y adaptadores.
- [Especificación](project-spec.md) y [contrato de creación](features/create_project.feature).
- [Campos y recorrido](docs/product-fields.md).
- [Personalización, conectores e infraestructura](docs/implementation-proposal.md).
- [Proceso SDD](docs/workflow.md) y [plantilla original](docs/template-harness.md).

Las skills de React y revisión de interfaces están versionadas en `.agents/skills/`. Los plugins del agente no son los conectores del producto.

## Despliegue productivo

Este Compose sirve para ejecutar y verificar el producto localmente. El servidor se gobierna desde `apptolast/DockerSwarmInfrastrcture`: integrar allí rutas Traefik, DNS, secrets, imágenes, recursos y backups. No se ha desplegado OrganizationWeb en el servidor.

Antes de producción se requiere HTTPS, identidad operativa, configuración de secretos, revisión del presupuesto de capacidad, migraciones y prueba de restauración. No trasladar credenciales E2E al servidor. Consultar los hallazgos concretos en `docs/implementation-proposal.md`.
