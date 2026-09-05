# OrganizationWeb — especificación de producto

Fecha: 2026-09-05. Estado: contrato create_project aprobado por el usuario tras presentar sus escenarios; roadmap posterior propuesto. El documento no acredita implementación sin las pruebas y revisión correspondientes.

## Propósito y alcance confirmado

Web personal para organizar proyectos, dividirlos en tareas pequeñas, planificar fechas y bloques con tiempo limitado, trabajar con objetivos diarios y consultar qué se completó y cuándo. Debe favorecer la constancia y el cierre deliberado de la jornada. El trabajo realizado cuenta aunque una tarea requiera varias sesiones.

Confirmado por el usuario:

- Monorepo: API y web en el mismo repositorio, separadas en `backend/` y `frontend/`, con builds propios y verificación conjunta desde la raíz.

- React, pnpm y SCSS para la presentación; sin Tailwind. TypeScript es la propuesta para el código de interfaz.
- Java, Spring Boot y Gradle Kotlin DSL; arquitectura hexagonal orientada a eventos (EDA).
- Base de datos, API propia, personalización extensa y conectores ampliables.
- Interfaz funcional y responsive para móvil, tablet y ordenador, con criterios rigurosos de UX.
- Despliegue en su servidor siguiendo la infraestructura existente.
- Desarrollo SDD con features pequeñas, tests y rigor siguiendo la plantilla.
- Áreas aceptadas: Hoy, Planificación, Proyectos, Sesión de trabajo e Historial; capacidad diaria, pocos proyectos activos, captura de ideas, hora de fin, replanificación y progreso semanal.

La plantilla aporta el proceso; no obliga a conservar Angular. No se crean datos de Zenit Digital como proyectos reales sin indicación.

## Requisito transversal UI/UX confirmado

El usuario incorpora Laws of UX en español como referencia obligatoria para todas las interfaces. Revisar sus 30 principios y aplicar los criterios responsive y de accesibilidad de [docs/ux-requirements.md](docs/ux-requirements.md) en cada contrato UI. Registrar evidencia y pendientes por principio; la primera entrega no acredita automáticamente esta matriz ampliada. Móvil, tablet y ordenador deben conservar todos los recorridos funcionales.

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

Estado: completada el 5 de septiembre de 2026 tras aprobación del Gherkin, implementación TDD y revisión APPROVED. Verificación local final: 65 tests backend, 38 frontend y 8 E2E verdes; PIT 36/36 y Stryker 143/148, con equivalencias documentadas. Evidencias en `progress/judge_create_project.md`, `progress/mutation_create_project.md` y `progress/history.md`. No desplegada en servidor; CI remota pendiente de push/ejecución. Propósito: capturar un proyecto propio persistente como idea con confirmación inequívoca.

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
| 2 | Publicar outbox a RabbitMQ | Confirms, recuperación y entrega al menos una vez; contrato propuesto propio, sin consumidores |
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

## Segunda feature: publish_outbox

**Estado: contrato features/publish_outbox.feature aprobado explícitamente el 5 de septiembre de 2026. Implementación en curso.** Alcance: publicador y recuperación según los 23 escenarios aprobados.

### Propósito y límites

Entregar a RabbitMQ los eventos ProjectCreated.v1 ya confirmados en PostgreSQL para que otras funcionalidades puedan consumirlos después. Publicar no significa que un consumidor haya ejecutado trabajo. Este corte no incorpora consumidores, conectores, automatizaciones, DLQ de consumidores, pantalla de eventos, limpieza del historial ni reparación manual de registros.

Se conserva el dominio puro y la separación hexagonal: un caso de uso de publicación mediante puertos de outbox y broker, adaptadores PostgreSQL/RabbitMQ y disparador periódico externo. No se modifica el contrato POST /api/v1/projects ni se espera a RabbitMQ para responder HTTP201.

### Contrato del evento y destino propuestos

Se parte del registro existente outbox_events: event_id, aggregate_id, owner_id, event_type, schema_version, occurred_at, payload y status pending. No se recrea el evento desde el proyecto actual.

- Publicar el objeto JSON guardado con eventId, aggregateId, ownerId, occurredAt, schemaVersion:1, name y type:ProjectCreated.v1. Igualdad semántica JSON, sin imponer orden de propiedades o espacios de serialización.
- Conservar esos valores en cada reenvío, incluido eventId y occurredAt; no generar identidad ni fecha nuevas. No añadir descripción de proyecto, credenciales ni campos del proyecto consultados posteriormente.
- AMQP message-id igual al eventId, content-type application/json, mensaje persistente y publisher confirms habilitados.
- Exchange durable de tipo direct: organization.events. Routing key project.created.v1. Cola durable quorum: organization.project-created.v1, binding exacto a esa clave. Sin exclusive/auto-delete, expiración, TTL ni políticas de descarte. Mantener el volumen del broker. En un solo nodo esto no ofrece alta disponibilidad; no se promete sobrevivir a la pérdida permanente de sus discos.
- Declarar la misma topología al conectar es idempotente. Si existe una topología incompatible, registrar TOPOLOGY_MISMATCH, conservar pendientes y no borrar ni sustituir recursos automáticamente.
- Envío obligatorio (mandatory): una devolución por falta de ruta invalida el intento incluso aunque llegue confirmación positiva. La publicación se considera aceptada únicamente con confirmación positiva del broker y sin devolución para ese intento. Los confirms no son acknowledgements de consumidores.

### Estados y coordinación

Migración aditiva propuesta, sin modificar payloads o estados de eventos existentes: attempts (intentos de publicación terminados y registrados, inicial 0), next_attempt_at (elegibilidad, inicialmente inmediata), published_at (null hasta éxito) y last_error_code (null inicial). Estados: pending, published y blocked. Los registros published se conservan y nunca se vuelven a seleccionar automáticamente.

Cada intento reclama exclusivamente un evento pending elegible; dos réplicas no publican simultáneamente la misma reclamación. Propuesta técnica: transacción por evento con bloqueo de fila y omisión de filas reclamadas, espera de confirmación acotada, registro de resultado y liberación al terminar. Una caída libera la reclamación mediante rollback; no se dejan bloqueos lógicos permanentes ni se necesita lease expirable en este corte. No se promete orden global entre réplicas o ante reintentos.

Éxito: solo después del confirm válido se incrementa attempts una vez, se cambia a published y se registra published_at con el reloj servidor. Si no puede confirmarse ese cambio en PostgreSQL, no se considera finalizado el evento. Puede haber una segunda entrega, con el mismo eventId. No existe transacción distribuida entre PostgreSQL y RabbitMQ y no se promete exactamente una vez.

Fallo observado del broker (indisponible, rechazo negativo, devolución o timeout): conservar pending y payload, incrementar attempts una vez y registrar last_error_code/next_attempt_at en PostgreSQL. Un fallo al registrar el resultado o una caída revierte la transacción: el evento sigue pendiente con sus datos previos. El número attempts no pretende contar envíos cuyo resultado se perdió al caer el proceso.

### Frecuencia y recuperación propuestas

Valores iniciales: ciclo de búsqueda cada 1 segundo, máximo 20 eventos distintos por ciclo, plazo de confirmación 5 segundos. Los pendientes se eligen por occurred_at y event_id dentro de los disponibles; un registro futuro, blocked, published o reclamado por otra réplica no consume el cupo de envíos de este ciclo.

Tras el fallo persistido número n, reintentar como pronto a los min(2^(n-1),60) segundos desde el instante en que se registró el fallo. Primeros intervalos 1, 2, 4, 8, 16, 32, 60, 60… segundos. No reintentar antes del vencimiento y no repetir el mismo evento dentro del mismo ciclo. Al recuperar broker/almacenamiento, los siguientes ciclos vuelven a intentar los eventos elegibles; no se descartan por agotar un contador. Si PostgreSQL impide incluso buscar/reclamar, no se envía nada y no se abre otro ciclo antes del intervalo normal.

El límite es de frecuencia y de trabajo por ciclo. A diferencia de detenerse tras N fallos transitorios, permite recuperarse de caídas prolongadas sin intervención ni pérdida silenciosa. La DLQ mencionada en la arquitectura general corresponde a futuros consumidores: un publicador no depende de otro envío al broker caído para conservar sus fallos; conserva la outbox en PostgreSQL.

Un registro cuyo tipo/versión no es ProjectCreated.v1/1 o cuyo payload contradice sus columnas obligatorias, no contiene exactamente los siete campos descritos o usa tipos incorrectos se conserva como blocked con código UNSUPPORTED_EVENT o INVALID_EVENT. No se envía ni bloquea otros eventos válidos. Su inspección y futura recuperación administrativa requieren otro contrato; no se inventa una API para ello ahora.

### Operación y privacidad

El publicador puede deshabilitarse con configuración explícita; deshabilitado no conecta con RabbitMQ y no altera la outbox. Habilitado exige endpoint, usuario y secreto válidos de configuración, sin credenciales predeterminadas; ausencia de un valor requerido impide iniciar el publicador con CONFIGURATION_ERROR y no imprime secretos. Una caída del broker durante operación no impide crear proyectos y no detiene la API.

Resultado observable por evento en registro estructurado: eventId, outcome (published/retry/blocked), attempt (ordinal del intento terminado; 0 si se bloquea antes de enviar) y code cuando proceda. Incidencias sin evento usan outcome worker_error y code. Códigos: BROKER_UNAVAILABLE, BROKER_NACK, UNROUTABLE, CONFIRM_TIMEOUT, STORAGE_UNAVAILABLE, TOPOLOGY_MISMATCH, UNSUPPORTED_EVENT, INVALID_EVENT y CONFIGURATION_ERROR. No incluir payload, nombre, descripción, ownerId, URL con credenciales ni contraseña. No se añade endpoint público de diagnóstico.

### Alternativas para aprobar

| Decisión propuesta | Otra opción | Motivo de la propuesta |
| --- | --- | --- |
| Confirms más mandatory y estado publicado tras confirm | Marcar enviado al escribir al socket | Comprobar aceptación/ruta antes de dar por terminado el trabajo. |
| Quorum durable con volumen, sin consumidores todavía | Cola clásica durable | Cola orientada a seguridad de datos y evolución a varias réplicas; despliegue inicial de un nodo sin promesa de HA. |
| Reintentos automáticos con intervalo máximo 60 s | Parar tras 10 fallos | Recuperación automática tras una interrupción larga; no se pierde ni se oculta trabajo pendiente. |
| Bloqueo de fila por intento de hasta 5 s | Leases persistentes renovables | Menos estados de recuperación para este volumen; medir contención antes de ampliar el diseño. |
| Conservar published y blocked | Purga automática tras entrega | Mantener trazabilidad; retención/limpieza necesita un contrato y política propios. |

Este paquete de decisiones fue aprobado por el usuario el 5 de septiembre de 2026, después de presentar el contrato y su resumen.

### Validación prevista y fuentes

Tests de aplicación con reloj/control del broker; integración con PostgreSQL y RabbitMQ reales para confirms, devolución obligatoria, persistencia tras reinicio, rollback y reclamación concurrente. Pruebas de caída entre aceptación y commit usando un punto de fallo controlado: primer intento, sin otros envíos ni consumidores; el caso posterior a aceptación verifica que el broker retuvo esa primera copia antes de cortar el proceso. Los conteos de copias de ese experimento no son una garantía de número de duplicados ante fallos arbitrarios de red. No se probará procesamiento por un consumidor que todavía no existe.

Fuentes primarias consultadas el 5 de septiembre de 2026: [RabbitMQ: publisher confirms y mandatory](https://www.rabbitmq.com/docs/confirms) y [RabbitMQ: quorum queues](https://www.rabbitmq.com/docs/quorum-queues). La propuesta usa la separación documentada entre confirmación al publicador y reconocimiento del consumidor; la topología y los intervalos concretos son decisiones propuestas para este producto.
## Feature 3 — Consultar proyectos propios (propuesta pendiente de aprobación)

Contrato: `features/read_projects.feature`. Estado **spec_ready**, sin autorización para implementar. La aprobación de publish_outbox no se extiende a este contrato.

### Comportamiento y decisiones propuestas

Lista persistente y detalle de proyectos propios, usando la autenticación bootstrap existente. React/pnpm/SCSS sin Tailwind; puertos de entrada/salida y dominio/aplicación sin frameworks, conforme a la arquitectura existente. Ningún cambio de proyectos, eventos o publicación es efecto de una consulta. Sin editar, tareas, calendario, búsquedas, filtros, métricas ni conectores nuevos.

API propuesta:

- `GET /api/v1/projects` devuelve 200 `{items: [...], nextCursor: string|null}`. Resumen exacto: id, name, status, createdAt, updatedAt. Página fija de 20 proyectos; nextCursor solo cuando existe otro elemento elegible. Se ordena createdAt DESC, id DESC con la comparación UUID de PostgreSQL. Sin total global ni parámetro limit.
- Cursor opaco para el cliente: base64url sin padding de JSON con dos campos exactos `createdAt` UTC a precisión microsegundo y `id` UUID. Se valida estructura/tipos/formato; no se considera una credencial ni necesita estar firmado. Se consulta siempre con el propietario autenticado y posiciones estrictamente menores que el par del cursor. Un cursor copiado o alterado solo cambia la posición dentro de los proyectos propios, nunca la autorización. Cursor vacío o repetido, formato inválido o campos desconocidos producen 400. Nuevas creaciones más recientes aparecen al regresar al inicio; continuar con el cursor no las intercala ni repite proyectos anteriores. No se promete un snapshot histórico ante futuras ediciones/borrados que este corte no permite.
- `GET /api/v1/projects/{id}` devuelve 200 con los mismos siete campos de la representación creada: id, ownerId, name, description, status, createdAt, updatedAt. Consulta ajena o inexistente: 404 PROJECT_NOT_FOUND, mensaje idéntico "Proyecto no encontrado". UUID inválido: 400 VALIDATION_ERROR con errors.field=id. Lista inválida usa errors.field=cursor o query y mensaje explicativo sin datos privados.
- Ambas lecturas exigen autenticación válida (401 UNAUTHENTICATED), conservan errores problem+json existentes (503 STORAGE_UNAVAILABLE, inesperado 500 INTERNAL_ERROR con correlationId), instantes ISO UTC y texto exacto. Respuestas privadas `Cache-Control: no-store`; no guardar datos en almacenamiento persistente del navegador.

Web: ruta `/proyectos` y `/proyectos?cursor=...` muestran una página; `/proyectos/{id}` permite enlace directo al detalle. Acciones "Más antiguos" y "Volver al inicio" sin menús. El enlace "Volver a proyectos" vuelve expresamente a la primera página; el historial normal del navegador conserva las URLs previas. El formulario Crear proyecto existente sigue disponible mediante enlace; su contrato de creación se mantiene. Fechas visibles localizadas en español con zona indicada, y valor original disponible semánticamente con time/datetime. Detalle muestra descripción completa como texto, incluidos saltos de línea; nombres largos se reorganizan sin ocultar información. La navegación cancela o ignora respuestas obsoletas.

Vacío confirmado orienta a crear; carga tiene feedback antes de 400 ms; error conserva ruta y permite Reintentar sin crear datos. Esperas/errores se anuncian accesiblemente. Un 401 retira contenido anterior y explica autenticación requerida; no añade flujo nuevo de login. Los enlaces directos funcionan tras recargar. Error 500/503 o red en detalle tienen el mismo patrón de reintento que la lista, sin simular éxito.

### Verificación UI propuesta — todas las filas pendientes

Referencia obligatoria: `docs/ux-requirements.md`. Matriz de 30 filas completa; todas las aplicaciones requieren evidencia futura y ninguna se declara cumplida. Las filas no aplicables documentan el recorrido ausente.

| Principio | Aplicación y evidencia futura | Estado |
| --- | --- | --- |
| Atención selectiva | Título Proyectos, nombres y navegación jerarquizados en vacío/error/todos los anchos. | Pendiente |
| Carga cognitiva | Una página de resúmenes; descripción completa solo en detalle. Revisión de uso sin recordar ids. | Pendiente |
| Estética-usabilidad | Componentes y contraste coherentes; capturas de lista/detalle/carga/error y lectura real. | Pendiente |
| Posición en serie | Crear y paginar en posiciones estables; orden de foco al variar ancho. | Pendiente |
| Tendencia a la meta | No hay objetivo ni cálculo de avance en una consulta. | No aplicable: sin progreso |
| Von Restorff | Acción principal y error distinguidos con texto, no solo color. Revisión visual y accesible. | Pendiente |
| Zeigarnik | Proyectos guardados recuperables después de cerrar; sin recordatorios culpabilizadores. Prueba recarga. | Pendiente |
| Fluir | No se inicia ni gestiona una sesión de trabajo en este corte. | No aplicable: sin sesiones |
| Fragmentación | Lista de proyectos separada del detalle; estructura semántica y prueba con textos reales. | Pendiente |
| Memoria de trabajo | Nombre/estado visibles, URLs recuperables y retry misma página. Prueba historial/recarga. | Pendiente |
| Navaja de Occam | Solo crear, abrir detalle, siguiente e inicio; revisión de propósito de cada control. | Pendiente |
| Conectividad uniforme | No se representan dependencias entre proyectos. | No aplicable: sin conexiones visuales |
| Fitts | Medir áreas de 44 × 44 px CSS en las acciones principales y separación; ensayo táctil/teclado. | Pendiente |
| Hick | Elección principal abrir proyecto; paginación directa sin opciones avanzadas. Revisión de uso. | Pendiente |
| Jakob | Enlaces navegables, botón Reintentar, historial y foco esperables. Tests navegador. | Pendiente |
| Semejanza | Misma apariencia para enlaces/estados equivalentes en lista y detalle. Capturas comparadas. | Pendiente |
| Miller | Agrupar cada resumen por proyecto, sin imponer siete elementos; revisión de comprensión. | Pendiente |
| Parkinson | No hay bloques temporales ni ampliación de sesiones. | No aplicable: sin planificación |
| Postel | Texto Unicode conservado; cursor/id estrictos, errores claros sin debilitar autorización. Tests API/UI. | Pendiente |
| Proximidad | Estado/fecha próximos al nombre; error/retry juntos y asociados semánticamente. | Pendiente |
| Prägnanz | Estados con texto y encabezados claros; no iconos como único significado. Revisión accesible. | Pendiente |
| Región común | Cada proyecto forma una unidad de lista y el detalle una región principal. Revisión orden de lectura. | Pendiente |
| Tesler | Cursor oculto al uso cotidiano, fecha con zona y errores comprensibles. Revisión humana. | Pendiente |
| Modelo mental | Lista de proyectos y detalle explícitos; ningún estado representa una sesión o tarea. Revisión etiquetas. | Pendiente |
| Usuario activo | Vacío orientador con Crear proyecto y detalle legible sin manual. Prueba primer uso. | Pendiente |
| Pareto | Priorizar abrir proyectos recientes por hipótesis inicial, sin porcentajes inventados. Validación de uso futura. | Pendiente |
| Fin de pico | Resultado cierto, retorno claro y error recuperable sin falso vacío. Tests éxito/error. | Pendiente |
| Sesgo cognitivo | Orden cronológico explícito, sin puntuar productividad ni penalizar descanso. Revisión de contenido. | Pendiente |
| Sobrecarga de opciones | Página fija de 20 proyectos y dos acciones de navegación; personalización sigue en otra feature. Revisión de uso. | Pendiente |
| Doherty | Medir feedback de carga < 400 ms con red demorada, sin espera artificial ni progreso ficticio. | Pendiente |

Responsive: revisar lista y detalle en 320, 360, 390, 480, 600, 768, 820, 1024, 1280, 1440, 1920 y 2560 px CSS; ambos lados de cada breakpoint implementado, alturas reducidas y orientación horizontal, zoom real 200 %, reflow a 320 px y ampliación de texto. Los ejemplos representativos del Gherkin no sustituyen la matriz completa. Cubrir vacío/carga/error/éxito, nombres de 120 puntos de código Unicode y descripción de 4000 caracteres sin espacios; sin overflow horizontal, recortes ni foco oculto. WCAG 2.2 AA como objetivo, contrastes, semántica, anuncios, movimiento reducido y 44 × 44 mínimo táctil principal. Revisar Chromium/Firefox/WebKit y complementar con móvil/tablet reales (teclado virtual/áreas seguras donde aplique); registrar entorno/evidencia real y dejar pendiente lo no ejecutado. Evaluaciones humanas de carga cognitiva y facilidad de uso no se sustituyen por axe ni screenshots automáticos.

### Puerta de aprobación

Preparar contrato no habilita código ni tests de implementación. Tras aprobación explícita, recorrer escenarios con TDD estricto, probar PostgreSQL real/autorización/paginación, UI y E2E, mutación y juez independiente. Las decisiones propuestas a revisar son páginas de 20 proyectos por cursor, recientes primero y detalle de solo lectura.
