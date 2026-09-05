# OrganizationWeb — especificación de producto

Fecha: 2026-09-05. Estado: contratos propuestos a partir de la conversación. La aceptación del concepto y del stack no equivale a aprobación de escenarios todavía no presentados. Este documento no declara implementada ninguna funcionalidad.

## Propósito y alcance confirmado

Web personal para organizar proyectos, dividirlos en tareas pequeñas, planificar fechas y bloques con tiempo limitado, trabajar con objetivos diarios y consultar qué se completó y cuándo. Debe favorecer la constancia y el cierre deliberado de la jornada. El trabajo realizado cuenta aunque una tarea requiera varias sesiones.

Confirmado por el usuario:

- React, pnpm y SCSS para la presentación; sin Tailwind. TypeScript es la propuesta para el código de interfaz.
- Java, Spring Boot y Gradle Kotlin DSL; arquitectura hexagonal orientada a eventos (EDA).
- Base de datos, API propia, personalización extensa y conectores ampliables.
- Interfaz funcional y responsive para móvil, tablet y ordenador, con criterios rigurosos de UX.
- Despliegue en su servidor siguiendo la infraestructura existente.
- Desarrollo SDD con features pequeñas, tests y rigor siguiendo la plantilla.
- Áreas aceptadas: Hoy, Planificación, Proyectos, Sesión de trabajo e Historial; capacidad diaria, pocos proyectos activos, captura de ideas, hora de fin, replanificación y progreso semanal.

La plantilla aporta el proceso; no obliga a conservar Angular. No se crean datos de Zenit Digital como proyectos reales sin indicación.

## Decisiones propuestas y alternativas

| Propuesta | Alternativa | Motivo y estado |
| --- | --- | --- |
| PostgreSQL y migraciones versionadas | SQLite o base documental | Relaciones y transacciones de proyecto/evento. Compatible con la referencia; versión y operación pendientes. |
| Monolito modular hexagonal | Microservicios desde el inicio | Mantener límites claros sin multiplicar servicios de negocio prematuramente. Propuesto. |
| RabbitMQ desde el inicio, outbox transaccional y consumidores idempotentes | Publicación directa sin outbox o eventos solo en memoria | Fidelidad al artefacto consultado por el coordinador y tolerancia a caídas del broker. RabbitMQ es propuesta basada en referencia; EDA sí está confirmado. |
| REST versionada y OpenAPI | GraphQL principal | Contratos y conectores fáciles de inspeccionar. Propuesto. |
| Identidad autenticada y propietario en recursos | Propietario fijo implícito | Evitar acoplar dominio a una única cuenta. Proveedor pendiente. |
| Preferencias y campos tipados | Código arbitrario ejecutable | Personalización comprobable manteniendo integridad y accesibilidad. Propuesto. |
| Conectores por capacidades y fases | Muchos proveedores simultáneamente | Cada integración necesita autenticación, errores, límites y sincronización probados. Propuesto. |

El coordinador ha consultado la referencia Claude mediante Chrome: dominio puro sin Spring/JPA/Jackson, aplicación que orquesta, infraestructura con adaptadores, PostgreSQL y RabbitMQ. Se adopta esa dirección arquitectónica como propuesta concreta; no se sustituye RabbitMQ por Modulith. La restricción de otro proyecto de que el usuario escriba el backend no pertenece a esta petición.

Dominio sin dependencias de frameworks, HTTP ni persistencia; casos de uso mediante puertos y adaptadores externos. EDA no implica event sourcing: el estado actual reside en PostgreSQL salvo cambio posterior. Eventos versionados, entrega al menos una vez, consumidores idempotentes, reintentos limitados y DLQ para fallos persistentes. No prometer exactamente una vez.

## Modelo y contratos generales propuestos

- Proyecto: propósito que agrupa tareas. Estados previstos idea, activo, pausado y terminado. Puede existir sin tareas; transiciones en feature propia.
- Tarea: unidad de resultado con criterio de finalización y estimación opcional. Completar registra un instante; trabajar no la completa automáticamente.
- Bloque planificado: inicio, fin y objetivo. Reserva capacidad; no acredita trabajo realizado.
- Sesión: trabajo real con inicio, fin, pausas y avance. Cerrar no cambia por sí solo el estado de una tarea. Tiempo neto excluye pausas y no puede ser negativo.
- Objetivo diario: resultado elegido para un día local; vinculación opcional a tareas por concretar en su feature.
- Historial: hechos identificados con propietario, entidad, tipo e instante. Incluye sesiones, finalizaciones y replanificaciones. No inventa logros desde planes. Correcciones y reaperturas preservan los hechos previos.
- Preferencias: zona, disponibilidad, descansos, límites, tema y vistas. Cambian presentación y defaults, no reescriben hechos pasados.

Identificadores generados por servidor. Lecturas y escrituras autorizadas por propietario. Errores por campo, sin stack traces para usuario. La interfaz diferencia guardando, guardado y fallo; no comunica éxito antes de confirmación.

### Tiempo, calendario y DST

Guardar instantes en UTC y zonas con identificadores IANA. Fechas sin hora permanecen fechas locales. Los bloques conservan su zona de planificación; cambiar preferencias no mueve sus instantes. Recurrencias se definen por hora local y zona, no sumando siempre 24 horas.

Hora local inexistente por cambio horario: impedir guardar y explicar. Hora repetida: elegir ocurrencia antes de confirmar. Calcular duración entre instantes, no etiquetas del reloj. Guardar fecha local y zona de la sesión al cierre para preservar atribución histórica del día; una vista alternativa debe etiquetar su zona. Excepciones de recurrencia y cambios de zona se concretarán antes de implementarse.

## UX y personalización

Hoy como entrada, siguiente bloque y hora de cierre visibles. Calendario semanal amplio y agenda vertical móvil. Arrastrar nunca es el único método de editar. Formularios etiquetados, errores asociados, foco visible, teclado y anuncios accesibles. Objetivo WCAG 2.2 AA, pendiente de pruebas automáticas y revisión manual; no afirmar conformidad anticipadamente.

Validar 320, 768 y 1440 CSS px, zoom 200 %, teclado y textos largos. Acciones esenciales sin desplazamiento horizontal. Temas respetan contraste y movimiento reducido. Límites de proyectos activos configurables no bloquean capturar ideas. Descansos no penalizan constancia.

Personalización incremental: tema claro/oscuro/sistema y colores validados; zona/disponibilidad/duraciones/descansos; orden y visibilidad de secciones y filtros; etiquetas/campos tipados; plantillas y automatizaciones auditables. Cada incremento tendrá restricciones y restauración de defaults. “Completo” requiere límites concretos para ser verificable.

## Primera feature: create_project

Estado: propuesta para Gherkin; no aprobada todavía. Propósito: capturar un proyecto propio persistente como idea con confirmación inequívoca.

### Entrada

Formulario con nombre obligatorio y descripción opcional. API propuesta: POST /api/v1/projects, JSON con name y description. Campos desconocidos, incluido ownerId o status, son errores.

- name: string obligatorio. Recortar solo espacios exteriores Unicode (propiedad Unicode White_Space), conservar interior. Longitud después del recorte 1–120 puntos de código Unicode inclusive, no bytes ni unidades UTF-16. No normalizar mayúsculas ni Unicode. Ausente/null/no string/solo espacios/más de 120: inválido. Nombres duplicados permitidos.
- description: ausente o null se normaliza a string vacío. String de 0–4000 puntos de código inclusive; conservar espacios y saltos. Tipo no string o más de 4000: inválido. Texto plano, nunca HTML ejecutable.
- Propietario procede del contexto autenticado, nunca de valores editables. Estado inicial siempre idea.

### Salida y atomicidad

HTTP 201, Location /api/v1/projects/{id}, representación con id, ownerId, name, description, status: idea, createdAt y updatedAt. Instantes iniciales iguales, del reloj servidor; id único. Location identifica el recurso; su lectura es la siguiente feature.

Una transacción guarda proyecto y registro outbox ProjectCreated.v1. Evento: eventId, aggregateId, ownerId, occurredAt, schemaVersion: 1 y nombre normalizado. aggregateId coincide con id y occurredAt con createdAt. No publicar descripción por defecto. Responder 201 solo tras commit. Fallar cualquiera de las dos escrituras revierte ambas. Broker caído no pierde el evento ni impide confirmar el commit local. La entrega RabbitMQ es una feature posterior del mismo diseño, no una condición para este 201.

Reiniciar backend o recargar no pierde el proyecto confirmado. Tras éxito, la interfaz muestra representación confirmada. Tras error conserva valores y permite corregir. Deshabilitar envío durante petición evita duplicados de una interacción; no garantiza idempotencia entre POST distintos. No reintentar automáticamente un POST con resultado de red incierto; explicar la incertidumbre. Idempotency-Key tendrá contrato propio.

### Errores

| Situación | Respuesta y efecto |
| --- | --- |
| Sin autenticación o credencial inválida | 401, ninguna escritura |
| Valores inválidos o campos desconocidos | 400 VALIDATION_ERROR con errores por campo, ninguna escritura |
| JSON mal formado | 400 MALFORMED_JSON, ninguna escritura |
| Content-Type no soportado | 415, ninguna escritura |
| Almacenamiento indisponible reconocido | 503 STORAGE_UNAVAILABLE, rollback |
| Otro error interno | 500 INTERNAL_ERROR, correlación sin detalles internos, sin estado parcial |

Formato propuesto application/problem+json: type, title, status, code y errors cuando corresponda, lista de field/code/message. Códigos de campo REQUIRED, INVALID_TYPE, TOO_LONG y UNKNOWN_FIELD. Nombre ausente, null o vacío tras recorte usa REQUIRED; otros tipos no string usan INVALID_TYPE. Mensajes iniciales españoles; pruebas sobre códigos estables.

### Casos límite para Gherkin

Nombre 1/120/121 puntos de código, caracteres fuera de BMP, espacios exteriores y solo espacios, ausente/null/no string. Descripción ausente/null/vacía/4000/4001/no string. Duplicados permitidos. Owner/status manipulados. Usuario sin autenticar. Rollback por fallo de cada escritura. Éxito persistente. Broker caído. Etiquetas mostradas como texto seguro. Red fallida conserva formulario. Persistencia puede verificarse mediante adaptador; no inventar listado sin especificar.

### Límites del corte

No incluye listar/editar/activar/eliminar/archivar, tareas, autenticación interactiva completa, entrega de eventos ni sincronización. Identidad es precondición del caso de uso; autenticar realmente antes de exponer en servidor. No desplegar identidad fija ni cabeceras de propietario no verificadas.

## Roadmap acotado

Orden propuesto; una sola feature atraviesa el pipeline a la vez. Cada fila exige contrato propio.

| Orden | Feature | Resultado |
| --- | --- | --- |
| 1 | Crear proyecto | Contrato anterior |
| 2 | Publicar outbox a RabbitMQ | Reintentos, consumidores idempotentes, DLQ y observabilidad |
| 3 | Consultar proyectos propios | Listado paginado, detalle y aislamiento |
| 4 | Editar proyecto | Nombre/descripción y concurrencia |
| 5 | Estados de proyecto | Transiciones y límites activos |
| 6 | Inicio/cierre de sesión | Identidad verificable y expiración; antes de despliegue |
| 7 | Crear tarea | Resultado esperado y estimación |
| 8 | Dividir tarea | Subtareas con progreso definido |
| 9 | Completar/reabrir tarea | Historia conservada |
| 10 | Disponibilidad | Zona y capacidad diaria |
| 11 | Planificar bloque | Inicio/fin/objetivo, DST |
| 12 | Consultar Hoy | Agenda y estados vacíos |
| 13 | Replanificar | Cambios trazables y concurrencia |
| 14 | Iniciar sesión de trabajo | Inicio real, una sesión activa |
| 15 | Pausar/reanudar | Intervalos y tiempo neto |
| 16 | Cerrar sesión | Avance y siguiente paso |
| 17 | Aviso de fin | Cierre o ampliación deliberada |
| 18 | Historial | Filtros y orden de hechos |
| 19 | Revisión semanal | Real frente a plan, descanso contemplado |
| 20 | Apariencia | Preferencias accesibles persistentes |
| 21 | Vistas/campos | Configuración tipada restaurable |
| 22 | Exportación | Descarga versionada de datos propios |
| 23 | Importación | Validación, vista previa y atomicidad definida |
| 24 | API para integraciones | Credenciales con alcance/revocación/límites |
| 25 | Webhooks | Firma, reintentos, registro |
| 26 | Calendario ICS | Exportar bloques propios |
| 27 | GitHub | Importar issues con trazabilidad |
| 28 | Calendario externo | Un proveedor y dirección definidos |
| 29 | Otros conectores | Adaptadores priorizados por uso |
| 30 | Automatizaciones | Reglas, simulación y auditoría |

Un botón no constituye un conector funcional. Cada integración define permisos, secretos, desconexión, mapeo, conflictos, idempotencia, límites y pruebas. Plugins de desarrollo no son conectores del producto. Los candidatos no representan conexiones existentes ni permiso para enviar mensajes a terceros.

## Calidad y operación

Pipeline: spec → Gherkin → puerta humana sobre escenarios → TDD un test cada vez → review → mutación. No done sin tests y umbral del arnés. Adaptar arnés al stack antes de afirmar validación. Tests de dominio, integración PostgreSQL para atomicidad, interfaz por comportamiento y contratos API/eventos. Versiones, herramientas y umbral se fijan explícitamente sin inventar resultados.

Antes de desplegar verificar dominio, proxy/TLS, redes, secretos, volúmenes, backup y restauración, migraciones, healthchecks y rollback en repos de infraestructura. Contenedores reproducibles propuestos; configuración real pendiente. Logs con correlación sin credenciales ni descripciones privadas.

## Pendientes explícitos

1. Proveedor de identidad y necesidad inicial de varios usuarios; propietario desde el primer corte.
2. Repositorio destino entre OrganizationWeb y OrganizacionWeb, ramas y política de despliegue.
3. Versiones compatibles y adaptación del arnés con fuentes primarias y entorno.
4. Ratificar PostgreSQL/RabbitMQ/outbox y límites del primer corte mediante escenarios.
5. Host, dominio, secretos y configuración de infraestructura exacta.
6. Priorización de conectores, extensión de personalización y política de borrado/conservación en contratos posteriores.
