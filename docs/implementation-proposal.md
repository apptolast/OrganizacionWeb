# OrganizationWeb — Propuesta de implementación

Fecha: 2026-09-05. Estado: diseño propuesto; sin código de producto ni despliegue.

## Decisiones confirmadas por el usuario

- React, pnpm y SCSS. Sin Tailwind. SCSS se compila a CSS para el navegador; toda la autoría de estilos se mantiene en SCSS.
- Java, Spring Boot y Gradle Kotlin DSL; Kotlin se utiliza en la configuración de Gradle, no como lenguaje de la API.
- Arquitectura hexagonal orientada a eventos, siguiendo la referencia visual del usuario.
- Web adaptable a móvil, tablet y escritorio, base de datos, API y personalización amplia.
- Desarrollo SDD con features pequeñas, tests, revisión y mutación según la plantilla existente.
- Alojamiento en el servidor del usuario, integrado con su infraestructura versionada.

## Arquitectura propuesta

React/TypeScript → REST /api/v1 → puerto de entrada → caso de uso → dominio.
Los puertos de salida acceden a PostgreSQL y registran eventos mediante adaptadores.
Una outbox transaccional entrega eventos a RabbitMQ; listeners entran por puertos de aplicación.

Un único backend modular inicialmente. Módulos previstos: identidad, proyectos, tareas,
planificación, sesiones, historial, preferencias e integraciones. Cada módulo incorporará
domain, application e infrastructure cuando la feature lo necesite. Ninguna dependencia
Spring, JPA, Jackson o AMQP entra en el dominio. Los puertos pertenecen al interior;
los DTO HTTP y las entidades JPA pertenecen a los adaptadores.

La referencia de Claude se leyó mediante Chrome: «Planos del VPS Monitor», revisión
21 de agosto de 2026. Usa RabbitMQ y PostgreSQL. Su acuerdo de que el usuario escribe
el backend era propio de ese otro proyecto; aquí el usuario ha pedido implementación agéntica.
No copiamos el dominio de monitorización: aplicamos sus fronteras a organización personal.

### Fiabilidad de eventos

- Guardar cambio y evento pendiente en la misma transacción PostgreSQL.
- Publicar después del commit, con confirmación del broker. Si hay fallo, mantener pendiente.
- Entrega al menos una vez; consumidor idempotente con clave única (consumer,eventId).
- Confirmar recepción tras confirmar la transacción del consumidor, nunca antes.
- Reintentos acotados con espera creciente y cola de errores; la recuperación será explícita y trazable.
- Sobre versionado: eventId, eventType, schemaVersion, occurredAt, aggregateId,
  aggregateVersion, correlationId y payload mínimo. No introducir secretos en eventos.
- No prometer entrega exactamente una vez ni usar el indicador de posible duplicado como única defensa.
- Historial visible y bitácora técnica son conceptos diferentes. La retención de la outbox
  no debe eliminar el historial del usuario.
- Lectura de un proyecto recién creado desde el modelo transaccional; no esperar a una proyección asíncrona.

Referencia técnica: [RabbitMQ Reliability Guide](https://www.rabbitmq.com/docs/reliability).
Spring Modulith se evaluó como alternativa para eventos internos; no sustituye al broker
de la referencia en esta propuesta.

## Personalización por entregas

| Área | Campos/configuración | Invariantes |
| --- | --- | --- |
| Apariencia | Tema claro/oscuro/sistema, acento, densidad, tamaño de texto, orden de paneles | Foco visible, contraste validado, restaurar valores iniciales |
| Tiempo | Zona IANA, días laborables, franjas disponibles, descansos, fin de jornada, duración por defecto | Fin posterior a inicio; fechas locales separadas de instantes |
| Proyectos | Nombre, descripción, color, icono, prioridad, fechas objetivo, estado, presupuesto de tiempo | Identidad estable; archivar conserva historial |
| Tareas | Título, resultado esperado, criterios de aceptación, estimación, subtareas, dependencias, etiquetas | Dependencias sin ciclos; completar no inventa sesiones |
| Flujo | Estados visibles, orden y nombres personalizados | Semántica interna estable de pendiente/en curso/completado/cancelado |
| Datos | Campos personalizados de texto, número, fecha, booleano, selección | Esquema versionado y validado; sin ejecutar JavaScript arbitrario |
| Objetivos | Límites diarios, proyectos activos, objetivo semanal, descansos | Avisos configurables; no penalizar días de descanso |
| Avisos | Canales, antelación, horas silenciosas, eventos elegidos | Consentimiento por canal; errores de envío visibles |

Los ajustes se guardarán en servidor para mantenerlos entre dispositivos. Se ofrecerán
vista previa, restablecer y exportar/importar configuración con validación de versión.
No se promete personalización ilimitada sin restricciones: las reglas protegen los datos
y permiten migrar configuraciones sin romper el historial.

## Conectores del producto

Esta lista es un backlog, no un catálogo de integraciones ya implementadas.

| Orden | Conector | Primer contrato útil | Dependencia externa |
| --- | --- | --- | --- |
| 1 | JSON / CSV | Exportación completa y tareas importables con vista previa | Ninguna cuenta externa |
| 2 | iCalendar / ICS | Exportar bloques de planificación | Cliente que acepte ICS |
| 3 | GitHub | Importar issues de repos seleccionados y enlazar PR | GitHub App con permisos por repositorio |
| 4 | Google Calendar | Consultar disponibilidad y publicar bloques en calendario dedicado | Aplicación OAuth y consentimiento |
| 5 | Webhooks / n8n | Enviar eventos firmados configurados por el usuario | Endpoint y secreto configurados |
| 6 | Outlook | Disponibilidad y eventos mediante Microsoft Graph | Registro de aplicación y OAuth |
| 7 | CalDAV | Sincronización con servidor compatible | URL y credenciales del proveedor |
| 8 | Notion, Todoist, Trello, Jira | Importación acotada por proyecto | Evaluar API, scopes y límites antes de cada feature |

Cada adaptador declarará capacidades reales (leer, importar, publicar, sincronizar),
estado de conexión, último éxito, error recuperable y desconexión. No habrá botones
de conectar que finjan éxito. La sincronización bidireccional será una feature distinta,
con sourceId, externalId, versión, cursor, conflictos y política de borrado definidos.
GitHub no completará una tarea por cualquier commit: será una regla explícita configurable.
Las integraciones almacenarán secretos cifrados en backend y usarán permisos mínimos;
las llamadas salientes validarán destinos para evitar SSRF, incluidos redirects.

Fuentes: [GitHub Apps](https://docs.github.com/en/apps/creating-github-apps/about-creating-github-apps/about-creating-github-apps),
[Google Calendar](https://developers.google.com/workspace/calendar/api/guides/overview),
[Microsoft Graph calendar](https://learn.microsoft.com/en-us/graph/api/resources/calendar?view=graph-rest-1.0).

## Skills y plugins investigados

- Plantilla local: spec_partner, gherkin_author, tdd_craftsman, judge, mutation_tester,
  security_reviewer y a11y_seo_auditor. El workflow exige fases separadas y una feature a la vez.
- [React Best Practices de Vercel](https://github.com/vercel-labs/agent-skills/tree/main/skills/react-best-practices):
  localizada y leída; útil para renderizado y carga del cliente. Excluir reglas específicas de Next.js/RSC.
- [Web Design Guidelines de Vercel](https://github.com/vercel-labs/agent-skills/tree/main/skills/web-design-guidelines):
  localizada y leída; aplicable en la revisión de formularios, teclado y estados. No instalada globalmente.
- Las dos skills de Vercel se instalaron a nivel de proyecto en `.agents/skills/`,
  usando skill-installer y la revisión `063bee94c3f4df8453406c830b0a7df0f2860278`.
  Estarán disponibles para descubrimiento en el siguiente turno; no se modificó la instalación global.
- Catálogo de skills de OpenAI consultado. No se instaló una colección indiscriminada.
- Plugin Management: skill disponible y aplicada. En esta sesión hay herramientas de permisos
  y dependencias, pero no aparecen search_plugins ni suggest_plugins. No se afirma haber consultado
  un catálogo dinámico inaccesible. El catálogo recomendado suministrado incluye GitHub, Figma y Google Calendar.
- GitHub sería la integración de desarrollo más directa; Git CLI y Chrome autenticado ya
  permiten avanzar en el repositorio. Figma será útil si se incorpora un archivo de diseño.
  Ninguno se ha instalado ni conectado en esta sesión.
- Chrome conectado y verificado con el perfil indicado: permitió leer el artefacto inaccesible por búsqueda web.
- Sites evaluado: su despliegue Workers y su starter no corresponden al Spring Boot autoalojado
  y SCSS sin Tailwind pedidos. Se conserva el repositorio y stack del usuario.

Las herramientas del agente y los conectores de la aplicación se implementan por separado:
instalar un plugin en Codex no proporciona OAuth ni integración a la web desplegada.

## Verificación requerida por entrega

1. Escenarios Gherkin aprobados y correspondencia escenario → test.
2. TDD: un fallo observable, implementación mínima, refactor en verde.
3. Java: JUnit, pruebas de puertos con dobles, PostgreSQL/RabbitMQ reales con Testcontainers,
   restricciones de dependencias con ArchUnit, PIT para mutación del dominio/casos de uso.
4. React: Vitest y Testing Library; Stryker para lógica propia; verificación de flujos en navegador
   y accesibilidad en móvil, tablet y escritorio. Estas herramientas son propuestas, aún no instaladas.
5. Pruebas de interrupción: broker caído, entrega duplicada, fallo transaccional, reintento de solicitud.
6. Revisión independiente y umbral del arnés (0,8); no declarar done con tests o mutación pendientes.
7. Build reproducible, lockfiles, imágenes versionadas, migraciones verificadas y documentación de operación.

## Encaje con el servidor

Lectura realizada en DockerSwarmInfrastrcture, commit c8bd39f0da15de0364372c79d56ba47fab354e46.
Plantilla de aplicación: commit 61da099e71b16088fe0f65390623101b9736f9c6.

La aplicación conservará código, pruebas, Dockerfiles y documentación de su contrato de ejecución.
El repositorio de infraestructura gobierna DNS, Traefik, redes, secrets, stack productivo,
catálogo de imágenes, backups y presupuesto de recursos. Preparar una imagen no acredita despliegue.

Hallazgo concreto: docs/CAPACITY.md deja 45 MiB dentro del presupuesto declarado de stacks;
no es memoria libre medida del servidor. Antes de añadir backend, base de datos y broker
habrá que actualizar el contrato de capacidad con evidencia real y validar el catálogo.
El README describe estado observado el 26 de julio, no el estado en vivo de septiembre.

Pendiente antes de producción: hostname, estado real del Swarm, recursos aprobados,
registro de imágenes, proveedor de identidad, secrets, backups/restauración y ruta de despliegue.
No se han ejecutado conexiones SSH, escrituras remotas, cambios DNS ni despliegues.
