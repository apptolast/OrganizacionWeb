# Arquitectura de OrganizationWeb

Contrato confirmado el 5 de septiembre de 2026. Referencia: «Planos del VPS Monitor», leída en la sesión de Chrome del usuario. Se aplican sus fronteras al producto de organización, sin copiar entidades de monitorización.

## Monorepo confirmado

El usuario confirma un único repositorio Git para la API y la web, con código separado por carpetas. La estructura existente cumple esa decisión:

```text
OrganizacionWeb/
  backend/       # API Java/Spring Boot; fuentes, tests, migraciones y build Gradle propios
  frontend/      # Web React/TypeScript/SCSS; fuentes, tests y build pnpm propios
  e2e/           # Pruebas de la web contra la API y PostgreSQL reales
  deploy/        # Proxy y contenedor de la web
  scripts/       # Comandos coordinados desde la raíz
  features/      # Contratos SDD
  docs/          # Arquitectura y requisitos compartidos
  docker-compose.yml
```

Un mismo historial, roadmap y CI coordinan ambos componentes. Los comandos raíz de test, lint, build y mutación ejecutan las herramientas de cada carpeta; Gradle gobierna la API y pnpm la web. Cada componente conserva sus dependencias y artefacto de despliegue. Monorepo no implica un único contenedor ni mezclar código Java con React. La comunicación entre ambos se hace por el contrato HTTP de la API, no importando detalles internos del otro componente.

La infraestructura operativa sigue en DockerSwarmInfrastrcture; ello no divide el código del producto en repositorios separados. No se trasladan carpetas ni se introducen gestores adicionales para cumplir esta decisión, ya reflejada en la estructura actual.

## Fronteras

- `backend/`: Java, Spring Boot, Gradle Kotlin DSL.
- Dominio: entidades y valores que protegen invariantes, eventos inmutables y errores propios. Sin dependencias de Spring, JDBC, JPA, Jackson, HTTP o RabbitMQ.
- Aplicación: puertos de entrada/salida y casos de uso. Coordina el dominio con reloj, identificadores, persistencia y eventos mediante interfaces.
- Infraestructura: REST, autenticación, transacciones, PostgreSQL, serialización y configuración Spring. Los DTO HTTP y de persistencia no dictan el modelo de dominio.
- `frontend/`: React con TypeScript, pnpm y SCSS. Sin Tailwind. Muestra datos confirmados y errores recuperables; no guarda credenciales.
- `deploy/` y Compose: ejecución local verificable. El despliegue productivo pertenece a DockerSwarmInfrastrcture y exige integrar catálogo, capacidad, DNS, secrets y backups.

Los subpaquetes concretos quedan a cargo de la implementación. Pruebas de arquitectura comprueban dependencias hacia dentro.

## Creación de proyecto

Un POST autenticado entra por REST y un puerto de aplicación. El dominio valida nombre y descripción. La aplicación solicita guardar proyecto y evento pendiente dentro de una transacción PostgreSQL. Una excepción revierte ambas escrituras. El éxito se envía tras commit. Ni localStorage ni ejemplos sustituyen a la base de datos.

La outbox forma parte del primer corte. El publicador RabbitMQ corresponde a la siguiente feature. Entrega al menos una vez con consumidores idempotentes, confirmaciones, reintentos y cola de errores. Una tabla de eventos pendientes por sí sola no acredita EDA completa.

## Identidad inicial

La identidad verificada es precondición del contrato aprobado. Para ejecutar este corte se permite HTTP Basic de bootstrap configurado por entorno, sin credenciales por defecto ni propietario suministrado por cliente. Producción requiere HTTPS. Inicio/cierre de sesión con experiencia propia es una feature posterior. JSON estricto, CORS restringido y protección contra solicitudes de otro origen pertenecen al adaptador HTTP.

## Dependencias justificadas

- React/Vite/TypeScript y Sass: interfaz con el stack solicitado y build reproducible.
- Spring Web/Security y acceso PostgreSQL: HTTP, identidad y persistencia.
- Migraciones: esquema versionado, sin recreación destructiva al arrancar.
- JUnit/Testcontainers/ArchUnit: contrato, transacciones reales y fronteras.
- Vitest/Testing Library/Playwright/axe: comportamiento cliente, integración y accesibilidad.
- PIT/Stryker: mutación de lógica del producto. Umbral mínimo 80 %, alcance y exclusiones explícitos. No bajar el umbral para declarar la feature terminada.

## Reglas de revisión

1. Un contrato funcional en progreso; escenarios trazados a pruebas ejecutadas.
2. Errores estructurados; no exponer detalles internos ni registrar secretos.
3. Reloj inyectable; Unicode contado por puntos de código.
4. Guardado atómico PostgreSQL, sin estados parciales ni éxito ficticio.
5. Texto de usuario tratado como texto, foco visible, etiquetas y estados anunciados.
6. Resultado de red incierto sin reintento automático de creación.
7. Sin pantallas, conectores o estadísticas ficticias fuera del contrato aprobado.
