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

**Estado: completada tras contrato aprobado explícitamente el 5 de septiembre de 2026, implementación TDD, verificación local y juez APPROVED. Application CI 33993262637 terminó SUCCESS para el commit 1a3737758c655462fc3814f6af8d0f87138eb1a8.** Alcance: publicador y recuperación según los 23 escenarios aprobados.

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
## Feature 3 — Consultar proyectos propios (completada localmente)

Contrato: `features/read_projects.feature`. Estado **done** tras TDD, revisión independiente y verificación local. Autorizado mediante «Si las apruebo todas». Backend 190 tests y frontend 73 en suite global; cinco casos frontend adicionales verificados focalmente. PIT 103/103 y Stryker global 276/297 (92,93 %), replay selectivo 17/17. CI 33995196185 en curso para 24b1e50ad000fe6fbc96fef5809c12f82d552854 al cerrar localmente; no se atribuye éxito remoto aún.

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

Aprobación explícita recibida. Recorrer escenarios con TDD estricto, probar PostgreSQL real/autorización/paginación, UI y E2E, mutación y juez independiente. Las decisiones propuestas a revisar son páginas de 20 proyectos por cursor, recientes primero y detalle de solo lectura.

## Autorización global del usuario — 5 de septiembre de 2026

El usuario indica «Si las apruebo todas» después de discutir el MVP y el roadmap. Se aprueba read_projects y se autoriza avanzar por las funcionalidades propuestas sin repetir una puerta humana por cada contrato. Esta instrucción del usuario prevalece sobre la puerta por feature de la plantilla. Se mantienen contratos antes de producción, una feature a la vez, TDD, revisión independiente y verificación relevante. Las ampliaciones ajenas al alcance acordado no quedan autorizadas por esta anotación.

Prioridad: recorrido útil de proyectos/tareas, planificación diaria con inicio y fin, registro de completados, historial, acceso privado y preparación de despliegue. Personalización avanzada y conectores después. Esta aprobación no consume créditos de restablecimiento de uso ni autoriza compras.
## Feature 4: editar proyecto — completada localmente

Contrato [features/edit_project.feature](features/edit_project.feature), aprobado dentro de la autorización global del usuario «Si las apruebo todas». Estado **done** tras TDD y juez conjunto APPROVED. Verificación raíz: 240 pruebas backend y 122 frontend, lint correcto; PIT 125/125, Stryker global 209/255 (81,96 %) y replays documentados por separado. Integración: 18/18 E2E, Firefox/WebKit y smoke de edición con RabbitMQ detenido y recuperación. CI 33997062229 completada correctamente sobre f8c1963; no se declara despliegue. Ponytail full y Caveman lite activos durante el corte. Decisiones técnicas: [propuesta edit_project](progress/proposal_edit_project.md).

PUT `/api/v1/projects/{id}` sustituye exclusivamente name y description, ambos obligatorios como propiedades JSON, con validación reutilizada de creación. Preserva id, ownerId, createdAt y status. GET detalle y PUT publican ETag fuerte basado en una versión interna BIGINT; el cuerpo de detalle conserva sus siete campos. If-Match vigente es obligatorio: ausencia 428 PRECONDITION_REQUIRED, formato/repetición/débil/comodín 400 VALIDATION_ERROR, versión obsoleta propia 412 PROJECT_CONFLICT. Proyectos ajenos o inexistentes siguen siendo 404 indistinguible. UPDATE condicionado por propietario/id/versión e inserción outbox se confirman atómicamente. Cambio equivalente con precondición vigente no escribe, no cambia updatedAt/ETag y no crea evento.

Cada cambio real produce ProjectUpdated.v1 con siete campos: eventId nuevo, aggregateId, ownerId, occurredAt=updatedAt, schemaVersion 1, name y type; description excluida. El publicador amplía su allowlist cerrada a Created/Updated: exchange organization.events, rutas project.created.v1/project.updated.v1 y colas quorum durables organization.project-created.v1/organization.project-updated.v1. Mantiene el JSON original, metadata, mandatory/confirms, códigos de fallo y reintentos existentes. No incorpora consumidor, orden global ni historial de versiones.

El formulario `/proyectos/{id}/editar` precarga detalle/ETag, permite guardar o cancelar, conserva borrador ante errores recuperables y trata el conflicto con recarga deliberada de la versión guardada. No reintenta una edición con una versión nueva automáticamente. Pérdida de acceso retira los datos; no hay caché persistente de proyecto/borrador/credenciales. La matriz UX y responsive se aplica al nuevo recorrido con evidencia explícita y sin ampliar funciones a estados, tareas ni borrado.

## Feature 5: estados de proyecto — contrato aprobado

Contrato `features/project_states.feature` aprobado mediante la autorización global. Estado **done** tras revisión conjunta APPROVED: 328 pruebas backend, 176 frontend, PIT 163/163, Stryker 284/312 (91,03 %) y replay separado 14/14. Integración 22 E2E, dos recorridos Firefox/WebKit, zoom real y smoke con broker detenido. CI 33998753845 SUCCESS sobre 171be09; no se declara despliegue. La propuesta acotada está en `progress/proposal_project_states.md`.

Estados cerrados Idea, Activo, Pausado y Terminado. Crear conserva Idea. Transiciones: Idea a Activo/Terminado, Activo a Pausado/Terminado, Pausado a Activo/Terminado y Terminado a Pausado. Igual estado es no-op con precondición vigente; otras transiciones son 409 INVALID_PROJECT_TRANSITION. Reabrir no activa trabajo automáticamente. El estado no inventa tareas, sesiones ni tiempo trabajado.

PUT `/api/v1/projects/{id}/status`, JSON exacto `{status}`, comparte la versión/ETag de edición y conserva los siete campos de respuesta. Mantiene validación estricta, autenticación, privacidad, precondiciones, no-store y errores seguros existentes. Un cambio real modifica sólo estado, updatedAt y versión interna; la edición de texto conserva el estado actual. Lista, detalle y formulario de edición aceptan los cuatro estados.

Máximo inicial de tres proyectos activos propios, configurable para el despliegue con APP_MAX_ACTIVE_PROJECTS entero 1–10. Configuración inválida impide arrancar. Bajar el límite nunca pausa proyectos automáticamente. Entrar en Activo sin plaza devuelve 409 ACTIVE_PROJECT_LIMIT con activeCount y limit propios. No se añade todavía configuración individual. La transacción adquiere primero un bloqueo asesor PostgreSQL común y después la fila del proyecto; cuenta y cambia bajo ese bloqueo. Esta simplificación serializa cambios de estado entre propietarios, admite escalado posterior por propietario si hay contención medida y no espera al broker.

Cada cambio real produce atómicamente ProjectStatusChanged.v1, payload exacto de ocho campos eventId, aggregateId, ownerId, occurredAt, schemaVersion, type, fromStatus y toStatus. Ruta cerrada project.status-changed.v1 y cola quorum durable organization.project-status-changed.v1 en organization.events. Conserva Created/Updated y las garantías del publicador; no incorpora consumidor ni orden global. Fallos revierten estado, versión, fechas y outbox. UI con acciones explícitas por transición, feedback temprano, conflictos sin reintento automático y matriz UX aplicada al control nuevo.

## Feature 6: inicio y cierre de sesión — completada localmente

Contrato `features/authentication.feature`, aprobado dentro de la autorización global y cerrado tras revisión independiente, pruebas y mutación. Publicado en 0913d75; CI 34001003734 completada correctamente, incluidos verify, compilación, E2E y smoke del publicador. La evidencia y los límites están en `progress/judge_authentication.md`; no se ha desplegado en el servidor. La propuesta `progress/proposal_authentication.md` incorpora decisiones de seguridad y contraste de las versiones resueltas: Spring Security 6.5.8 y Spring Session JDBC 3.5.5 sobre PostgreSQL existente, sin actualizar el stack ni introducir Redis o JWT propios.

El único usuario configurado conserva su nombre como propietario. Formulario nativo username/password, POST `/api/session` con codificación de formulario y CSRF, 204 confirmado o 401 UNAUTHENTICATED genérico. GET `/api/session` devuelve exactamente authenticated, username (null anónimo), csrfToken opaco y csrfHeaderName `X-CSRF-TOKEN`, siempre sin caché. POST `/api/session/logout` invalida en servidor y expira la cookie; GET no cierra sesión. Retirar Basic de la aplicación, conservando el acceso separado a administración de RabbitMQ.

Sesión JDBC con treinta minutos de inactividad, rotación al autenticar, persistencia tras reinicio y esquema Flyway compatible con la dependencia. Cookie SESSION HttpOnly, SameSite Lax, Path /api y Secure según el origen HTTPS configurado; desarrollo HTTP limitado a loopback. Ningún token, contraseña o identificador de sesión se persiste en almacenamiento web ni en URLs. El esquema conserva los nombres configurados usando TEXT en PRINCIPAL_NAME; no añade una restricción incidental de longitud.

Protección CSRF estándar HttpSession/XOR en login, logout y escrituras. Un usuario autenticado con token inválido recibe 403 CSRF_INVALID; acceso privado anónimo o caducado recibe 401 incluso con token inválido. Se conserva OriginGuard. Fallo JDBC de sesión produce 503 SESSION_UNAVAILABLE sin SQL ni éxito ficticio. Login y logout no comprometen una respuesta 204 antes de confirmar persistencia; logout fallido conserva la cookie anterior para reintentar. La solución se verificará mediante fallos reales en guardado y eliminación, sin atribuir éxito a la revisión documental.

La web comprueba sesión antes de montar datos privados, anuncia espera, bloquea doble envío, permite gestores de contraseñas y borra la contraseña tras respuesta. El cierre o pérdida de acceso desmonta datos y borradores; una señal sin secretos coordina pestañas y recuperar visibilidad comprueba validez. Sólo se recuperan rutas locales propias. Renovar CSRF no reenvía escrituras automáticamente. Un cierre no confirmado retira la vista privada, explica incertidumbre y permite reintentar.

Nginx sirve documentos/assets públicos y protege datos mediante la API; retirar auth_request de la SPA. Migrar scripts y E2E al login real usando APIRequestContext ya instalado para cookies. Conservar pruebas de propietarios preparadas mediante soporte de Spring y probar seguridad real por separado. Regresión de proyectos/eventos y matriz UX completa, con zoom nativo y límites explícitos de dispositivos, antes de cerrar esta feature. Sin registro público, recuperación por correo, SSO ni cuentas adicionales en este corte.

## Feature 7: crear y recuperar tareas de proyecto — completada localmente

Contrato en features/create_task.feature, aprobado bajo la autorización global del usuario. Implementación cerrada tras revisión independiente, regresión de 486 pruebas backend, suite frontend final de 371 pruebas, integración real y mutación por encima del umbral. progress/judge_create_task.md conserva los resultados de cada ejecución, los replays separados y las limitaciones de dispositivos y limpieza. No se ha desplegado en el servidor.

Una tarea es una unidad pequeña y verificable dentro de un proyecto propio. Campos de entrada: title obligatorio de 1 a 160 puntos de código tras recortar Unicode White_Space; completionCriterion opcional, null o ausente se normaliza a cadena vacía, máximo 2000 puntos de código conservando el contenido; estimatedMinutes opcional, null o ausente se conserva como null y cualquier valor presente debe ser entero entre 1 y 1440. La estimación no representa tiempo trabajado. El JSON es estricto: no permite campos de sistema o desconocidos, duplicados ni documentos concatenados.

POST /api/v1/projects/{projectId}/tasks crea y devuelve HTTP 201 únicamente después de confirmar tarea y evento. Location apunta a GET /api/v1/projects/{projectId}/tasks/{taskId}. Ambos y los elementos de la lista contienen exactamente id, projectId, title, completionCriterion, estimatedMinutes, status, createdAt y updatedAt; status inicial pending y fechas iniciales iguales, fijadas por servidor. GET de colección devuelve exactamente items y nextCursor, veinte tareas por página en orden descendente de createdAt e id. El cursor estricto está vinculado al proyecto y no acepta campos ni parámetros adicionales. Una creación posterior a la primera página no se intercala en su continuación.

La identidad deriva de la sesión. En estos recursos, proyecto o tarea inexistentes, ajenos o con relación incorrecta devuelven HTTP 404 RESOURCE_NOT_FOUND con el mismo mensaje. Los identificadores inválidos y campos inválidos producen VALIDATION_ERROR; almacenamiento fallido produce STORAGE_UNAVAILABLE. No se modifica el contrato PROJECT_NOT_FOUND de las rutas anteriores. Se conservan CSRF, OriginGuard, no-store y las respuestas de sesión establecidas por feature 6.

La tarea se trata como entidad hija del agregado proyecto para este corte. La tabla mantiene referencia a projects y el evento conserva aggregateId del proyecto, con taskId separado; no se elimina la clave externa existente de outbox. La transacción bloquea la fila de proyecto propio y comprueba que no está completed antes de insertar tarea y evento, con confirmación de una fila para cada inserción. No requiere el bloqueo asesor de capacidad. Si terminar confirma primero, crear devuelve HTTP 409 PROJECT_COMPLETED sin escrituras. Si crear confirma primero, terminar puede confirmar después y la tarea sigue pending. Crear no cambia el ETag de la representación actual del proyecto ni su estado, capacidad o fechas. El usuario debe reabrir explícitamente un proyecto terminado en pausa para añadir trabajo.

TaskCreated.v1 tiene exactamente eventId, aggregateId, ownerId, occurredAt, schemaVersion, type, taskId y title. No publica criterio ni estimación. Reutiliza el publicador, exchange y garantías existentes con ruta task.created.v1 y cola durable quorum organization.task-created.v1; valida esquema cerrado y conserva identidad entre reintentos. No se promete orden global o por proyecto en el publicador actual. Un fallo de broker no revierte una tarea confirmada ni pierde el evento pendiente.

La web incorpora una sección Tareas al detalle existente: formulario, lista persistente y paginación, con estados independientes de carga, vacío y error. Las acciones del proyecto permanecen disponibles si falla esa lista. En proyectos terminados se conservan las tareas visibles y se explica la reapertura necesaria; terminar no completa tareas automáticamente. Errores conservan borrador salvo pérdida de sesión, no repiten escrituras automáticamente y no presentan éxito incierto. Navegación y cancelación impiden respuestas antiguas sobre un proyecto nuevo. Formulario y lista cumplen la matriz UX, teclado, controles de 44 por 44, bordes responsive y zoom nativo establecidos. Completar tareas, subdividirlas y planificar sesiones siguen siendo features posteriores.

## Feature 8: dividir tareas — contrato aprobado

Create_task está cerrada y publicada en db4d20b; su CI remota 34004667683 terminó correctamente, incluidos verify, compilación, E2E y smoke del publicador. El contrato `features/split_task.feature` contiene 38 escenarios, revisados desde las perspectivas de integración y backend y aprobados dentro de la autorización global. El parser confirmó 82 casos de tablas locales; ese recuento no incluye las entradas heredadas por referencia, que deberán ejecutarse también sobre el endpoint nuevo. Split_task está cerrada localmente con dictamen APPROVED; su CI de publicación se registra por separado.

Una tarea propia puede dividirse creando hijos nuevos, con los mismos campos y límites de contenido. Un hijo puede tener a su vez hijos: se permite división progresiva sin mover, reasignar o borrar entidades existentes. Crear un paso no completa ninguna tarea, no registra tiempo trabajado ni suma estimaciones. Padre y proyecto conservan sus valores, fechas y versiones. Añadir trabajo a un proyecto completed exige reapertura deliberada en pausa.

POST y GET `/api/v1/projects/{projectId}/tasks/{parentId}/subtasks` crean y consultan hijos directos. POST recibe sólo title, completionCriterion y estimatedMinutes y confirma HTTP 201 después de tarea, relación y evento atómicos. Conserva el DTO de ocho campos y Location `/api/v1/projects/{projectId}/tasks/{id}`. Las reglas cerradas de JSON y las tablas de contenido de create_task se ejecutan íntegramente sobre esta nueva ruta. El cuerpo no admite parentId suministrado por el cliente.

La colección de hijos devuelve items y nextCursor, veinte por página, ordenados por createdAt e id descendentes. El cursor canónico contiene exactamente projectId, parentTaskId, createdAt e id y pertenece a esa colección. Rechaza cursores del proyecto plano, de otro padre o proyecto, campos desconocidos, claves duplicadas y fechas/UUID inválidos. Los identificadores de ruta se validan antes del cursor. La colección anterior del proyecto sigue incluyendo raíces e hijos, con su contrato intacto; su POST sigue creando raíces y TaskCreated.v1.

GET `/api/v1/projects/{projectId}/tasks/{id}/parent` devuelve exactamente `{parent:null}` para una raíz o `{parent:<DTO8>}` para una subtarea. No añade campos al DTO anterior. Proyecto o tarea inexistentes, ajenos o con relación incorrecta conservan el mismo 404 RESOURCE_NOT_FOUND. Se mantienen sesión, CSRF, origen, no-store y errores de almacenamiento, sin convertir un fallo en una relación vacía.

La persistencia añade una relación opcional al padre, limitada al mismo proyecto mediante integridad compuesta. La creación bloquea primero el proyecto propio y verifica el padre dentro de esa transacción; no toma el bloqueo global de capacidad. Reutiliza las consecuencias de la carrera con cierre del proyecto y exige una fila de tarea y una de evento. No necesita leer ancestros ni recorrer todo el árbol. No se introduce borrado en cascada de jerarquías.

SubtaskCreated.v1 tiene exactamente eventId, aggregateId, ownerId, occurredAt, schemaVersion, type, taskId, parentTaskId y title. AggregateId sigue identificando el proyecto; hijo y padre deben ser distintos. Criterio y estimación permanecen privados. Se publica por subtask.created.v1 a la cola quorum durable organization.subtask-created.v1, conservando identidad, confirmaciones y entrega al menos una vez. No se emite además TaskCreated por la misma creación ni se altera ninguno de los cuatro contratos anteriores.

La web permite abrir `/proyectos/{projectId}/tareas/{id}`, mostrar contenido y contexto del proyecto, navegar al padre confirmado y consultar o crear hijos directos con los controles existentes donde encajen. El fallo del recurso parent se muestra como error recuperable; sólo parent null confirmado significa raíz. Se conserva el borrador ante errores, salvo pérdida de sesión, y una confirmación 201 aunque falle la lectura posterior. Navegación, doble envío, recuperación de conflicto y respuestas tardías mantienen las garantías anteriores, incluida la corrección de reintento durante guardado. El recorrido exige teclado, matriz responsive y treinta principios UX con evidencia y limitaciones explícitas.


## Feature 9: completar y reabrir tareas — contrato aprobado

Split_task está cerrada y publicada en 3675c36; su CI 34007601179 sigue ejecutándose al aprobar este contrato. El usuario aprobó globalmente las funcionalidades. El coordinador revisó las propuestas backend/integración y aprueba features/complete_reopen_task.feature: 36 escenarios, 137 casos locales y todas las variantes referenciadas. Esta es la siguiente única implementación autorizada. El historial duradero responde a la necesidad de recordar qué se completó y cuándo, aunque se reabra después.

## Estado, API y compatibilidad

Completar y reabrir son decisiones explícitas entre pending y completed, independientes del estado del proyecto y de padres/hijos. No propagan estados ni registran tiempo trabajado. Un padre completed admite hijos nuevos mientras el proyecto esté abierto; el hijo nace pending y no reabre al padre. La restricción existente de no crear trabajo bajo proyectos completed se conserva.

La lista plana, detalle, padre y lista de hijos mantienen exactamente DTO8: id, projectId, title, completionCriterion, estimatedMinutes, status, createdAt y updatedAt. Sus validadores admiten ahora pending/completed; la creación sigue produciendo pending. No se añade completedAt a esas representaciones.

GET y PUT `/api/v1/projects/{projectId}/tasks/{id}/status` devuelven HTTP 200 con exactamente `{status,completedAt,updatedAt}` y ETag fuerte del mismo snapshot confirmado. PUT acepta sólo `{status}` con el literal exacto pending o completed, sin trim ni cambio de caja. El ETag tiene forma `"task:<UUID canónico minúsculo>:<versión decimal canónica>"`; versión BIGINT no negativa, representada como `0` o entero sin signo ni ceros iniciales. El prefijo diferencia tarea de proyecto. El cliente conserva el ETag opaco, sin convertir la versión en Number.

PUT exige If-Match. Ausencia: 428 PRECONDITION_REQUIRED; formato, identidad de otra tarea o tag no canónico: 400 VALIDATION_ERROR con campo If-Match y código INVALID_VALUE; versión antigua de tarea propia: 412 TASK_CONFLICT. Se compara versión antes del no-op. Una intención ya satisfecha con revisión vigente devuelve cuerpo/ETag anteriores sin escribir, incrementar versión ni tocar fechas, historia o outbox.

## Errores y acceso

Sesión JDBC, origen y CSRF conservan las reglas vigentes. Recursos ajenos, inexistentes o vinculados a otro proyecto devuelven el mismo cuerpo completo 404 RESOURCE_NOT_FOUND, sin revelar conflicto ni estado. Respuesta privada 401 no incluye desafío Basic. Cada nueva respuesta lleva Cache-Control no-store, incluidas las combinaciones de s31.

Después de filtros y con media type admitido: validar UUID de path antes de cursor/cuerpo/precondición; en PUT validar precondición antes de JSON. Sintaxis precede forma/tipos; propiedades extra preceden status y se selecciona la primera por orden lexical. Una vez válida la petición, comprobar propiedad/existencia antes de versión y versión antes de no-op. La selección HTTP del endpoint puede devolver 415 UNSUPPORTED_MEDIA_TYPE antes del handler; no se promete otra precedencia para ese caso. Cada rechazo de las tablas mantiene válido el resto de la petición.

s7 fija REQUIRED para status ausente/null, INVALID_TYPE para escalares o estructuras incorrectos, INVALID_VALUE para literales no admitidos y UNKNOWN_FIELD para extras. JSON truncado, vacío, duplicado o concatenado usa 400 MALFORMED_JSON. Indisponibilidad de almacenamiento usa 503 STORAGE_UNAVAILABLE; nunca fabrica pending, relación ni historial vacío. Las tablas s6/s7/s13/s14/s15/s30 completan las variantes obligatorias.

## Persistencia, tiempo y concurrencia

Añadir versión interna BIGINT con default 0 y completed_at nullable. Las tareas existentes permanecen pending, versión 0, sin historia inventada. Restricciones: versión no negativa; pending implica completed_at null; completed implica completed_at presente e igual a updated_at. Una transición real incrementa la versión una vez y usa el máximo entre reloj UTC truncado a microsegundos y updatedAt anterior. Historia, evento y updatedAt comparten ese instante. Completar fija completedAt; reabrir lo limpia sin borrar cierres previos. La versión, no la fecha, ordena las transiciones.

La consulta transaccional autorizada bloquea sólo la fila de tarea mediante `FOR NO KEY UPDATE OF t`, bajo READ_COMMITTED, antes de comparar revisión. No toma bloqueo global de capacidad ni bloquea todas las tablas del JOIN. Así se mantiene compatibilidad con KEY SHARE de las FK al crear hijos; s28 verifica ambos órdenes reales de creación de hijo y cambio de estado sin bloqueo circular.

UPDATE conserva identidad/proyecto/versión y exige una fila. Actualizar tarea, insertar historia e insertar outbox es una sola transacción; cada inserción exige una fila. Excepción o cero filas en cualquiera revierte las tres operaciones y devuelve 503 sin ETag de éxito. Respuesta sólo después del commit. Dos completed concurrentes desde pending y el mismo ETag producen un ganador 200 y un 412, una transición y un evento. No hay llamada RabbitMQ dentro de la transacción.

## Historial duradero

Tabla propia task_status_history: id UUID, project_id, task_id, task_version positiva, from_status, to_status y occurred_at. FK compuesta a la tarea del mismo proyecto, unicidad `(task_id,task_version)` y estados distintos del conjunto permitido. No se enlaza su retención al outbox. Conserva transiciones indefinidamente; no hay edición, borrado ni limpieza en cascada de historia en este corte. Puede compartir UUID con eventId para correlación sin compartir responsabilidad.

GET `/api/v1/projects/{projectId}/tasks/{id}/history` devuelve exactamente `{items,nextCursor}`. Cada entrada tiene exactamente id, fromStatus, toStatus y occurredAt; son identidades y fechas confirmadas, no derivadas de la cola. Veinte entradas por página, orden task_version DESC. Consultar 21 permite determinar continuación; versiones no necesitan ser consecutivas.

Cursor opaco: base64url canónica sin padding de JSON estricto con exactamente projectId, taskId y taskVersion entero positivo dentro de BIGINT. Vinculado a proyecto/tarea; continúa por versión menor que la última devuelta. No requiere fecha ni UUID de desempate. Rechaza claves ausentes/extra/duplicadas, tipos y límites inválidos, query repetida/desconocida o cursor de otra colección según s13; ejecuta cada variante allí indicada. Historia sin transiciones devuelve vacío confirmado; 503 nunca se convierte en vacío.

## Evento y publicación

Cada transición real produce TaskStatusChanged.v1 con exactamente nueve campos: eventId, aggregateId, ownerId, occurredAt, schemaVersion, type, taskId, fromStatus y toStatus. aggregateId identifica el proyecto; taskId la tarea; schemaVersion es 1 y type TaskStatusChanged.v1. No incluye título, criterio, estimación ni tiempo trabajado. occurredAt coincide con historia y updatedAt.

Publicar en exchange organization.events, ruta task.status-changed.v1, cola quorum durable organization.task-status-changed.v1. Conservar JSON original, identidad, persistencia, mandatory, confirmación y entrega al menos una vez. Tipo/versión desconocidos se bloquean como UNSUPPORTED_EVENT; esquema/identidad/estados incompatibles como INVALID_EVENT, sin envío. s32 ejecuta ausencia de cada uno de los nueve campos. Los cinco contratos anteriores permanecen intactos. Broker detenido no impide HTTP 200 ni consultar historia; la fila pendiente conserva identidad al publicar tras recuperar RabbitMQ.

## Interfaz y límites

El detalle carga estado y revisión antes de habilitar Completar tarea o Reabrir tarea. Muestra fecha de finalización actual y permite consultar el historial incluso después de reabrir. El estado del detalle y su control permanecen alineados. Anuncia espera, impide doble envío y sólo aplica una confirmación válida con ETag correspondiente.

412, 503, fallo de red o HTTP 200 inválido no repiten PUT automáticamente: ofrecen consulta deliberada del estado vigente. Un fallo posterior de historial conserva la transición confirmada y presenta recuperación independiente; reintentar historia no reenvía la escritura. GET anterior no sustituye el PUT confirmado. Navegación/cierre de sesión descarta respuestas tardías de status, history y escritura; 401/404 retira detalle, estado e historial privados, también durante el reintento.

Verificación: teclado/foco visible, objetivos de al menos 44 × 44 CSS, matriz y breakpoints de docs/ux-requirements.md, zoom nativo al 200 % con ancho interior de 320, feedback medido inferior a 400 ms antes de liberar respuesta retenida y treinta principios UX con evidencia o limitaciones explícitas. No atribuir bloques con inicio/fin, sesiones de concentración o progreso medido a esta feature. No incluye edición de contenido, borrado, propagación de estados, suma automática de estimaciones ni planificación. La futura edición de tarea podrá compartir esta versión interna mediante contrato propio.

# Feature 10 — Disponibilidad personal

Contrato aprobado dentro de la autorización global del usuario: `features/availability.feature`, 47 escenarios y 237 casos expandidos. Complete_reopen_task está cerrada y publicada en e1afc11; no se mantienen dos implementaciones activas. Las revisiones documentales backend/frontend y la resolución del coordinador preceden al contrato.

## Presupuesto y zona

El usuario elige una zona y siete presupuestos diarios, sin ventanas horarias. MONDAY a SUNDAY son las claves exactas; cada valor es un entero JSON de 0 a 1440 minutos. Todos los días a cero son válidos y permiten descanso. El total semanal se deriva, no se persiste ni acredita trabajo. Los días locales de 23/25 horas y las horas ambiguas o inexistentes pertenecen al posterior contrato de bloques con inicio y fin.

El backend publica el conjunto ordenado y sin duplicados de IDs disponibles en TZDB mediante java.time, más UTC. Se valida pertenencia exacta al escribir; no se filtran aliases ni se amplía SHORT_IDS ni se aceptan offsets libres. El cliente no usa Intl como autoridad. Una sugerencia del navegador sólo rellena el borrador si está admitida. GET conserva una zona histórica que ya no esté en el catálogo, sin resolverla ni sustituirla: la UI la muestra no disponible y exige escoger otra antes de guardar, incluso para una intención equivalente.

## Representación y persistencia

GET `/api/v1/me/availability` devuelve exactamente configured, zoneId, dailyMinutes y updatedAt, con ETag del mismo snapshot. Ausencia confirmada: false y tres null, HTTP 200 y tag fuerte literal `"availability:unconfigured"`; leer no inserta. Configurada: true, zona, mapa completo y fecha UTC, con tag `"availability:<UUID canónico minúsculo>:<versión decimal canónica BIGINT no negativa>"`. GET `/api/v1/me/availability/zones` devuelve exactamente items. Endpoints privados, sin query params y con no-store.

PUT reemplaza exactamente zoneId y dailyMinutes y exige If-Match. Una fila propia por owner_id único, UUID, siete columnas de minutos con restricciones SQL, zona, versión y fechas. Primer guardado usa versión 0; cambios reales incrementan una vez. La fecha inicial se trunca a microsegundos y una actualización usa el máximo entre reloj truncado y fecha previa. Revisar identidad/versión antes del no-op dentro de transacción; el no-op conserva cuerpo, tag y fechas. Identidad siempre desde sesión, sin consultar al propietario mediante el UUID enviado en el tag.

Dos inserciones desde ausencia producen un 200 y un 412, nunca sobrescriben al ganador. Cero filas por colisión con una preferencia propia existente es conflicto; cero sin fila resultante por supresión es 503. Edición propia usa bloqueo de fila o actualización condicionada equivalente, sin bloqueo global entre usuarios. Cada escritura confirma una fila. La prueba de fallo de COMMIT usa rechazo PostgreSQL previo a confirmar mediante constraint trigger diferido; pérdida de respuesta después de confirmar es incierta y no implica rollback garantizado.

No se emite evento de disponibilidad en este corte: no hay consumidor ni requisito causal para él. No se inventa un proyecto ni se debilita la FK del outbox. Las seis rutas existentes, proyectos, tareas e historia permanecen intactos. Un futuro evento personal requiere contrato y persistencia propios. Cambiar preferencias no desplaza reservas ni reescribe hechos históricos.

## Validación y recuperación

Se conservan filtros de sesión, origen y CSRF; negociación puede devolver 415 antes del handler. Query desconocida precede a If-Match; éste precede al cuerpo. Falta de precondición: 428 PRECONDITION_REQUIRED. Tag débil, lista, UUID no canónico o versión inválida: 400 VALIDATION_ERROR. Tag válido que no coincide con la fila propia: 412 AVAILABILITY_CONFLICT, también ante intención ya satisfecha. Fallo de almacenamiento: 503 STORAGE_UNAVAILABLE, nunca ausencia inventada.

JSON estricto rechaza documento vacío, truncado, concatenado o claves duplicadas como MALFORMED_JSON. Forma, extras raíz, zoneId, objeto dailyMinutes, extras diarios y días de lunes a domingo se validan en ese orden. Extras usan orden léxico. Campo ausente/null: REQUIRED; tipo incorrecto: INVALID_TYPE; zona ajena: INVALID_VALUE; minutos fuera de rango: OUT_OF_RANGE; extra: UNKNOWN_FIELD. `1.0` no es token entero JSON para este contrato. El navegador puede serializar una entrada numérica válida como entero, pero nunca interpretar vacío o `1e` incompleto como cero. Las variantes concretas viven en Gherkin y se ejecutan todas.

## Formulario

Ruta exacta `/disponibilidad`, incluida en retorno tras login. Selector nativo y siete campos etiquetados en español, lunes a domingo. Snapshot, borrador textual y catálogo son estados separados. Ausencia confirmada muestra Sin configurar, sugerencia opcional y siete ceros; puede guardarse sin editar previamente. Total sólo con siete enteros válidos; en otro caso se pide completar los presupuestos. Un reintento de catálogo no sobrescribe lo editado.

Aviso permanente: Los cambios sin guardar se pierden al salir. Cancelar y volver a Proyectos descarta y navega a `/proyectos`. La garantía de descarte explícito cubre acciones del formulario; no se añade guardia global, router, beforeunload ni autosave. La pérdida de sesión retira datos inmediatamente.

Campos y Guardar se bloquean durante PUT o recuperación. Sólo PUT configurado válido, ETag válido y zona/minutos iguales a la intención enviada confirma Disponibilidad guardada. GET inicial incoherente no habilita guardar ni inventa ausencia. Errores de campo 400 conservan el borrador y permiten corregir. Tras 412, 503, red o confirmación inválida se requiere Recargar versión guardada antes de otra escritura. Esa acción explica el descarte y sólo GET válido reemplaza juntos formulario y ETag; un fallo conserva borrador. No hay comparación, fusión ni reenvío automático.

Privacidad y cancelación incluyen GET de preferencia, GET de zonas y PUT: respuestas viejas no restauran datos ni revocan acceso vigente. Se reutiliza el cliente de sesión/CSRF y recuperación existentes. Verificar 44 × 44 CSS, teclado/foco, 22 anchos, zoom nativo 200 % a 320 CSS, feedback antes de 400 ms y matriz de treinta principios con límites explícitos. Este corte no incluye calendario, temporizador, conectores ni trabajo acreditado.
## Feature 11 — planificar bloque: contrato aprobado

La feature 10 está cerrada en cb162b7. Contrato de 62 escenarios y 325 casos aprobado tras revisión independiente bajo la autorización global y la petición de continuar. No reconstituye el documento auxiliar cuya escritura fue bloqueada. La revisión y destilación Gherkin preceden a producción.

### Recorrido y límites

Dentro del detalle de una tarea, consultar «Bloques planificados» y abrir «Planificar bloque». Un bloque tiene objetivo, inicio y fin explícitos y una zona. No acredita trabajo ni completa tareas/proyectos. Se permite planificar tareas pending de proyectos idea, active o paused; completed impide nuevas reservas. Completar después una tarea/proyecto conserva las reservas existentes, que siguen ocupando tiempo hasta una futura replanificación/cancelación explícita. Hoy, sesiones, recurrencia, avisos y mover/cancelar bloques tienen features posteriores.

El objetivo se normaliza con strip y admite 1–500 puntos de código Unicode. Inicio y fin son fechas/horas locales estrictas de precisión de minutos, formato YYYY-MM-DDTHH:mm, años 0001–9999. Pueden cruzar medianoche. La duración real debe ser positiva, de 1 a 1440 minutos enteros; no se redondea una duración fraccionaria. El inicio no puede preceder al instante actual del servidor; igualdad admitida. La comprobación usa un reloj inyectable y se repite al crear, porque una revisión anterior puede quedar en el pasado. La recuperación de una creación confirmada no vuelve a aplicar este requisito temporal.

La zona del bloque debe pertenecer al catálogo Java existente. La disponibilidad guardada propone la zona inicial; puede elegirse otra con el mismo selector nativo. Cada extremo se resuelve con los offsets válidos de ZoneRules. Un gap se rechaza sin desplazarlo. Un overlap requiere elegir explícitamente una ocurrencia; el error devuelve los offsets válidos para ese extremo. Un offset proporcionado debe ser canónico y pertenecer al conjunto válido; Z representa cero y se admiten los segundos cuando formen parte del ID canónico. El orden y la duración se comparan entre instantes, aunque las etiquetas del reloj parezcan invertidas. Persistir UTC y la zona de planificación; cambios de preferencias no mueven esos instantes.

### Disponibilidad, solapes y decisión de exceso

La disponibilidad debe estar configurada. Su zona vigente determina los días del presupuesto, aunque la zona del bloque sea distinta. La revisión muestra ambas zonas con claridad. Para cada fecha afectada se intersecta el intervalo UTC con los límites reales de ese día local; no se añaden siempre 24 horas. Se suman segundos reales de todas las reservas propias que intersectan ese día, también de otros proyectos. La preferencia nueva modifica esta proyección del presupuesto, no los datos históricos del bloque.

Los intervalos son semiabiertos: inicio incluido, fin excluido. Dos bloques contiguos se permiten; cualquier solape de instantes del mismo propietario se rechaza, incluso con proyectos/zonas distintos. Usuarios distintos son independientes. No se libera capacidad por completar una tarea o proyecto.

Decisión de producto adoptada como supuesto explícito para este corte: mostrar exceso y exigir aceptación específica, sin bloquear permanentemente a la persona. El permiso se limita al bloque exacto revisado; modificar su objetivo, fechas, zona u ocurrencias invalida la revisión y el permiso. Cero significa descanso y exige la misma aceptación para reservar. Un permiso de exceso nunca permite solapes. La creación recalcula la capacidad bajo bloqueo: si creció desde la revisión y no existe permiso, responde conflicto y exige revisar de nuevo. Cuando sí existe permiso, autoriza ese bloque concreto frente a la capacidad vigente; no añade minutos ni otras reservas. La UI explica este alcance y no preselecciona la aceptación.

### API de revisión y creación

Base privada: /api/v1/projects/{projectId}/tasks/{taskId}/blocks. Mantener sesión, origen, CSRF en POST, JSON estricto y Cache-Control no-store también en errores. No aceptar parámetros adicionales. Validar identificadores completos y propiedad de proyecto/tarea sin diferenciar ajeno de inexistente.

POST /preview recibe exactamente objective, startLocal, endLocal, zoneId, startOffset y endOffset. Los offsets pueden ser null para solicitar resolución inequívoca; nunca elegir por el usuario entre varias ocurrencias. No escribe entidades, preferencias ni outbox. Devuelve HTTP 200 con exactamente objective normalizado, zoneId, startAt, endAt, startOffset y endOffset resueltos, durationMinutes, availabilityEtag, budgetZoneId y days. Cada elemento de days contiene exactamente date, budgetMinutes, plannedSeconds, requestedSeconds y excessSeconds; orden ascendente, sólo fechas con intersección positiva. excessSeconds es max(0, plannedSeconds + requestedSeconds - budgetMinutes*60). La suma de requestedSeconds coincide con durationMinutes*60. La revisión comprueba elegibilidad y solapes y puede devolver errores de campo o conflictos; superar presupuesto por sí solo devuelve revisión válida con exceso visible.

POST sobre la base crea el bloque. Recibe los mismos seis campos, con ambos offsets explícitos ya resueltos, más allowOverBudget booleano obligatorio. Exige Idempotency-Key UUID canónico y Availability-Revision con el ETag configurado recibido de preview. El segundo header protege contra cambios de zona/presupuestos; no pretende ser una revisión de todas las reservas. Una revisión ausente produce 428 PRECONDITION_REQUIRED, mal formada 400 VALIDATION_ERROR y distinta 412 AVAILABILITY_CONFLICT. La key ausente o mal formada produce 400 VALIDATION_ERROR con campo Idempotency-Key. Las claves no son identificadores de entidad: el servidor genera blockId y eventId.

Una primera creación confirma con 201 y Location del detalle; replay confirmado con 200 y la misma Location/cuerpo. El DTO de bloque tiene exactamente nueve campos: id, projectId, taskId, objective, startAt, endAt, zoneId, durationMinutes y createdAt. Instantes UTC, createdAt con precisión de microsegundos. No incluye una versión futura, campos de sesión, progreso ni métricas ficticias. No cambia las revisiones o fechas de proyecto, tarea o disponibilidad.

### Recuperación de resultado incierto

La key se conserva por intención y se vincula al endpoint de esa tarea. Unicidad (taskId, requestKey). Se persisten junto al bloque los campos normalizados de la petición para comparar exactamente el objetivo, fechas locales, zona, offsets y permiso; la precondición de disponibilidad no forma parte de la intención. Misma key e intención devuelve la creación previa sin otro evento; misma key con intención distinta produce 409 IDEMPOTENCY_CONFLICT. La comparación del replay precede a elegibilidad, instante actual, catálogo y revisión de disponibilidad, tras autenticar, validar estructura y comprobar propiedad. Así se recupera un éxito anterior aunque la tarea se haya completado o cambien preferencias/reglas.

GET /by-request/{requestKey} recupera el DTO propio o 404 BLOCK_NOT_FOUND. Un 404 no prueba rollback de una petición que aún podría llegar: la UI permite volver a enviar manualmente la MISMA intención/key, nunca crear otra automáticamente. Mientras el resultado sea incierto conserva y bloquea la intención; ofrece «Comprobar guardado». Una comprobación fallida conserva ese estado. Un rechazo definitivo permite corregir y generar otra intención. CSRF conocido permite renovar acceso y repetir sólo mediante decisión manual, manteniendo intención/key. Cerrar el editor no anula una petición enviada; se explica y la lista permite consultar reservas después.

### Lectura y errores

GET de la base devuelve exactamente items y nextCursor, veinte DTO como máximo, createdAt DESC e id DESC. Cursor opaco estricto vinculado al proyecto/tarea y a la colección de bloques; rechazar cursores de tareas/subtareas u otro contexto. GET /{blockId} devuelve el DTO o 404 BLOCK_NOT_FOUND; primero debe existir el contexto propio. Lecturas conservan reservas de tareas/proyectos completados y no modifican estado. Paginación no usa estimaciones, fechas locales ni el reloj del navegador.

Campos desconocidos, tipos incorrectos, null obligatorio y valores fuera de límites producen 400 VALIDATION_ERROR con errores asociados. JSON duplicado o sintácticamente mal formado produce 400 MALFORMED_JSON, conservando la forma compartida existente. Fechas inexistentes y offsets inválidos/ambiguos identifican el extremo afectado; la ambigüedad aporta opciones cerradas. Conflictos 409 diferenciados: AVAILABILITY_REQUIRED, PROJECT_COMPLETED, TASK_COMPLETED, BLOCK_OVERLAP, BUDGET_EXCEEDED e IDEMPOTENCY_CONFLICT. Un solape identifica un bloque propio conflictivo para poder consultarlo, sin listar datos ajenos. Un exceso devuelve los días recalculados y zona del presupuesto. Fallos de almacenamiento anteriores al commit revierten todo y producen 503; una confirmación perdida posterior conserva la recuperación por key. No exponer SQL, stack traces ni secretos.

### Atomicidad y evento

Transacción READ_COMMITTED con SELECT separados: proyecto propio FOR SHARE, tarea propia FOR SHARE, disponibilidad propia FOR UPDATE. La revisión puede usar FOR SHARE en disponibilidad para obtener un snapshot coherente de presupuesto/reservas; no escribe datos. Todas las escrituras de bloques conservan el orden. SHARE del proyecto permite KEY SHARE de outbox y evita el ciclo con completar una tarea. Propiedad se confirma antes de divulgar falta de disponibilidad; replay se resuelve antes de rechazar por estado/capacidad.

Dentro de la misma transacción se resuelven deduplicación, revisión, elegibilidad, tiempo, solape y capacidad; insertar exactamente un bloque y una outbox o revertir. Guardar relación compuesta proyecto/tarea, instantes crecientes, duración acotada y key única. La fila de disponibilidad coordina reservas entre proyectos del mismo usuario, sin bloqueo global. Las carreras deben demostrar ausencia de duplicados, exceso no autorizado y ciclos con estados de tarea/proyecto o cambios de disponibilidad. Un fallo/supresión de escritura o rechazo de commit anterior a confirmación no produce éxito parcial.

Evento BlockPlanned.v1: eventId, aggregateId (proyecto), ownerId, occurredAt, schemaVersion=1, type=BlockPlanned.v1, blockId, taskId, startAt, endAt, zoneId y durationMinutes. No difundir el objetivo ni la key de recuperación. Publicación por block.planned.v1 hacia cola quorum durable organization.block-planned.v1, preservando las seis rutas y garantías previas. El validador de outbox exige esquema cerrado, IDs, instantes y duración coherentes; conserva payload/identidad en reintentos. No se promete exactamente una vez.

### Interfaz y pruebas requeridas

Editor inline en detalle de tarea, con objetivo, dos datetime-local de precisión de minutos y selector de zona. «Revisar bloque» consulta el servidor; muestra instantes/offsets comprensibles, duración y presupuesto antes de «Guardar bloque». Ocurrencias ambiguas se eligen mediante controles etiquetados. El exceso usa aceptación explícita sin valor inicial marcado. Cambiar un campo invalida preview y aceptación. No hay autosave, POST automático, almacenamiento local ni nueva guardia global de navegación. Una disponibilidad ausente ofrece configurarla con aviso de pérdida del borrador al salir.

Validar DTO, intención y offsets antes de mostrar revisión/éxito; no confiar sólo en un 200. No convertir etiquetas locales con la zona implícita del navegador. Separar lista, revisión, guardado y recuperación; conservar borrador ante fallos y descartar respuestas obsoletas después de cada await. Mantener foco del control de origen sin robar otro destino elegido. Confirmar bloque visible y lectura persistida, incluido reinicio de backend y resultado perdido recuperado con la misma key.

TDD, revisión independiente y mutación del alcance nuevo/compartido; PostgreSQL real para concurrencia/rollback e idempotencia y RabbitMQ real para la séptima ruta. Convertir en escenarios ejemplos DST de Madrid, media hora de Lord Howe, cruce de medianoche y proyección a una zona distinta. Matriz de treinta principios UX, breakpoints de cualquier SCSS nuevo, 320–2560 CSS, zoom nativo, teclado y motores representativos; conservar límites de evidencia física/humana.

### Precisiones de revisión del contrato

Los instantes UTC y las fechas que se exponen en la proyección del presupuesto permanecen en años 0001–9999. Una entrada cuya conversión sale de ese rango produce 400 VALIDATION_ERROR asociado a startLocal o endLocal; nunca año ampliado ni 500. Los límites internos del día siguiente pueden usar año 10000 si no se expone ni persiste como extremo del bloque. Si la zona guardada de disponibilidad ya no puede resolverse, preview y creación nueva devuelven 409 AVAILABILITY_ZONE_UNAVAILABLE, que solicita actualizar disponibilidad. No se sustituye por UTC. Esto no impide leer ni recuperar bloques guardados.

Para replay se validan autenticación, protección de escritura, estructura y sintaxis de cuerpo/headers e identificadores, normalización del objetivo y propiedad. Los headers obligatorios siguen presentes y bien formados. Después se compara la intención persistida sin resolver fechas/offsets contra el catálogo actual ni exigir que Availability-Revision siga vigente. Para una creación nueva, revisar la key otra vez después de adquirir el bloqueo de disponibilidad y antes de solapes/capacidad: una petición que esperó a otra con la misma key recupera su bloque, no rechaza un solape consigo misma. Dos envíos iguales concurrentes producen 201 y 200 con un bloque y un evento.

La validación del preview en cliente compara objetivo normalizado y zona con la intención, respeta offsets explícitos y verifica cada instante mediante fecha local menos offset, duración e igualdad de la suma de requestedSeconds. Un offset solicitado como null se acepta resuelto sólo con esa correspondencia válida. No se replica TZDB del servidor. Cambiar startLocal retira startOffset; cambiar endLocal retira endOffset; cambiar zona retira ambos. Cada edición invalida revisión y consentimiento. Un preview fallido conserva campos, retira una revisión anterior y mantiene Guardar deshabilitado. Una respuesta obsoleta no reactiva una revisión invalidada.

La key se genera en el primer envío de creación. Se conservan juntos key, petición exacta con offsets resueltos y Availability-Revision. Renovar CSRF o comprobar guardado no los modifica. Mientras se crea o recupera un resultado incierto se bloquean campos; Cancelar sigue disponible con aviso sobre la petición transmitida. BLOCK_NOT_FOUND al comprobar conserva la intención y ofrece «Reenviar el mismo bloque» o volver a comprobar. RESOURCE_NOT_FOUND del contexto retira datos por pérdida de acceso. Un fallo de almacenamiento no se convierte en lista vacía.

Son rechazos definitivos para preparar una nueva revisión los errores reconocidos de validación, disponibilidad/revisión, elegibilidad, solape o presupuesto; se conserva el borrador y se retira preview/consentimiento. Un 412 tras reenviar una intención incierta exige revisar de nuevo antes de preparar otra intención. IDEMPOTENCY_CONFLICT es una excepción: conserva el bloqueo y la key, permite consultar su resultado y nunca genera automáticamente otra key. Red, cuerpo inválido, error desconocido y 503 se tratan como inciertos. 401 retira datos; CSRF reconocido ofrece recuperación manual con la misma intención. La UI no interpreta códigos desconocidos como rechazo definitivo seguro.

Confirmar creación/replay/recuperación requiere DTO de nueve campos válido y coincidencia de contexto, objetivo, zona, instantes y duración con la intención/revisión retenidas. Un 200/201 de contenido distinto mantiene incertidumbre. Un fallo posterior del listado no retira una creación ya confirmada. El consentimiento explica que autoriza ese bloque aunque otras reservas aumenten el exceso mostrado antes del guardado; no sólo la cifra de la revisión. BUDGET_EXCEEDED sin permiso requiere revisar de nuevo antes de ofrecer consentimiento.

Los errores conservan application/problem+json con type, title, status y code, siguiendo ApiErrors. VALIDATION_ERROR añade errors, lista de objetos cerrados field/code/message. Fecha sintácticamente inválida usa INVALID_FORMAT; fecha local inexistente usa NONEXISTENT_LOCAL_TIME en startLocal/endLocal; inicio pasado usa IN_PAST en startLocal; duración no positiva, fraccionaria o superior al límite usa OUT_OF_RANGE en endLocal. Offset ambiguo null usa AMBIGUOUS_OFFSET y offset canónico no válido para esa fecha usa INVALID_OFFSET en startOffset/endOffset. Sólo estos dos últimos errores añaden validOffsets, objeto con el extremo afectado y su lista de offsets canónicos válidos, ordenada por el instante resultante ascendente. Resolver primero inicio y después fin permite corregir un extremo a la vez, sin opciones incompletas inventadas.

BLOCK_OVERLAP añade exactamente conflict con id, projectId y taskId del bloque propio elegido; si hay varios, elegir startAt ascendente e id ascendente. BUDGET_EXCEEDED añade exactamente budgetZoneId y days con los cinco campos del preview. Los demás conflictos no añaden datos de reservas. Un contexto ajeno o inexistente devuelve RESOURCE_NOT_FOUND; un bloque/key ausente dentro de contexto propio devuelve BLOCK_NOT_FOUND. No convertir los errores compartidos de rutas anteriores en otra forma para este corte.

Precedencia: filtros de sesión/origen/CSRF existentes, parámetros de consulta, IDs de ruta, headers de creación (Availability-Revision antes de Idempotency-Key), estructura/sintaxis del JSON y campos, propiedad, replay confirmado. Para nueva creación: disponibilidad configurada, igualdad de su revisión, elegibilidad del proyecto antes de la tarea, resolución de la zona de presupuesto y de los extremos, solape, presupuesto. Preview omite headers/replay/comparación de revisión y conserva ese orden de negocio. No se exige un orden arbitrario entre varios errores puramente estructurales del cuerpo; todos son de validación sin escrituras. El replay no exige elegibilidad ni disponibilidad vigente.

La sección recibe el estado confirmado compartido de la tarea; completar/reabrir o una consulta deliberada actualiza ese contexto sin otra lectura silenciosa. Cambiar elegibilidad impide iniciar creación nueva, pero no desmonta un editor con resultado incierto ni bloquea su recuperación. Para mostrar una zona que Intl no reconoce, presentar el instante UTC explícitamente etiquetado junto al ID original de zona; nunca formatear silenciosamente con la zona del navegador.

La normalización «strip» del objetivo significa retirar Unicode White_Space de los extremos, como los títulos de tarea existentes, conservando el interior y contando puntos de código. No se usa una interpretación distinta entre Java y JavaScript; NBSP en los extremos se retira y un objetivo de sólo espacios Unicode se rechaza. La comparación de replay y de respuestas usa esa misma normalización.

Publicar un evento histórico no vuelve a resolver su zona contra el catálogo vigente: exige zoneId textual no vacío y el esquema/instantes/duración coherentes, preservando el ID almacenado. La validación de pertenencia y reglas de zona ya ocurrió al crear el bloque. Un cambio posterior de TZDB no inutiliza una outbox pendiente válida.

Si el control que inició una acción desapareció o permanece deshabilitado, restaurar foco al encabezado de Bloques planificados, sólo cuando el usuario no eligió otro destino. La recuperación conservada ante cambio de elegibilidad cubre tanto tarea completed como proyecto completed. Un preview válido requiere ETag de disponibilidad configurada, days no vacío y valores enteros no negativos, con requestedSeconds positivo y budgetMinutes entre 0 y 1440 para cada fecha.

Conservar el protocolo existente del publicador: un payload inválido de evento soportado queda blocked con INVALID_EVENT, sin publicar; tipo/versión de envoltorio no soportado mantiene UNSUPPORTED_EVENT. La ausencia de cualquiera de los doce campos obligatorios del payload es inválida. Los fallos reconocidos de almacenamiento usan 503 STORAGE_UNAVAILABLE. Las pruebas de concurrencia con completar tarea/proyecto y cambiar disponibilidad deben ejercitar ambos órdenes posibles de adquisición, además de comprobar ausencia de deadlocks y éxitos parciales.

## Feature 12 — today (Hoy)

### Propósito y alcance

Mostrar al usuario su agenda personal del día, el bloque en horario planificado, el próximo inicio y la hora de cierre prevista. Es una lectura de reservas existentes: transcurrir una hora nunca acredita trabajo ni completa una tarea. Conserva bloques de tareas y proyectos completed. No introduce sesiones, temporizadores de trabajo, replanificación, recurrencia, calendario semanal ni creación de bloques desde Hoy.

Especificación revisada por el coordinador (86dc6c) bajo la autorización global vigente, pendiente de destilación Gherkin; no activa implementación.

### Día y snapshot

`GET /api/v1/today` requiere la sesión existente y no admite parámetros de consulta, propietario, fecha ni zona suministrados por el cliente. Captura una sola vez el reloj de servidor: `serverNow`. Un puerto de lectura devuelve un snapshot coherente de disponibilidad y bloques del propietario autenticado, con los nombres de sus proyectos y títulos de sus tareas. El adaptador PostgreSQL usa una transacción local read-only REPEATABLE_READ para que preferencia, ausencia de preferencia, nombres y reservas no mezclen versiones entre consultas. No reutiliza PlanBlock, no bloquea filas para escribir, no crea disponibilidad, no emite eventos ni modifica outbox. Reutiliza los modelos de bloques y el reloj existentes; filtra en SQL las intersecciones del día, sin cargar todo el historial ni realizar lecturas HTTP por cada tarea.

La zona efectiva es la guardada en disponibilidad si el servidor puede resolverla. Sin disponibilidad usa UTC y `zoneSource=UNCONFIGURED`; con una zona guardada que ya no puede resolver usa UTC y `zoneSource=UNAVAILABLE`. En ambos casos explica la causa y devuelve presupuesto desconocido (null), nunca cero ni una capacidad trasladada de otra zona. No modifica la preferencia ni los instantes o zonas históricos de los bloques. Con una zona válida usa `zoneSource=AVAILABILITY`.

`date` es la fecha local de serverNow en la zona efectiva. `dayStartAt` es el inicio de esa fecha y `dayEndAt` el inicio de la siguiente fecha en esa zona: no se suman 24 horas fijas. El día y los bloques son intervalos semiabiertos. Incluye un bloque si `startAt < dayEndAt && endAt > dayStartAt`; el contacto exclusivo con un extremo no basta. Los días de cambio horario tienen su duración real. Una reserva que cruza medianoche aparece en ambos días que intersecta, sin recortar sus instantes almacenados.

### Respuesta cerrada

200 JSON, `Cache-Control: no-store`, sin cursor ni truncamiento de la agenda del día. El objeto tiene exactamente estos quince campos, todos obligatorios; los valores ausentes se representan mediante null donde se indica:

| Campo | Contrato |
| --- | --- |
| serverNow | Instante UTC capturado una vez y truncado a microsegundos, conforme al formato heredado de feature11; todos los cálculos usan esa misma captura normalizada. |
| date | Fecha local ISO YYYY-MM-DD del día efectivo. |
| zoneId | ID resoluble de la zona efectiva; UTC en los fallback. |
| zoneSource | AVAILABILITY, UNCONFIGURED o UNAVAILABLE. |
| availabilityZoneId | Zona guardada, incluso si no es resoluble; null si no hay preferencia. |
| dayStartAt, dayEndAt | Instantes UTC que delimitan el día efectivo y contienen serverNow. |
| budgetMinutes | Entero 0–1440 del día de semana de date con zona válida; null en fallback. |
| plannedSeconds | Suma entera no negativa de las intersecciones de los bloques con el día. |
| remainingSeconds | max(budgetMinutes × 60 − plannedSeconds, 0); null si no hay presupuesto conocido. |
| excessSeconds | max(plannedSeconds − budgetMinutes × 60, 0); null si no hay presupuesto conocido. |
| currentBlockId | ID del bloque cuyo startAt ≤ serverNow < endAt; null si no existe. |
| nextBlockId | ID del primer bloque de la agenda con startAt > serverNow; null si no existe. |
| closingAt | Mayor endAt real entre los bloques de la agenda; null si está vacía. |
| items | Lista completa, sin IDs repetidos, ordenada por startAt y después ID UUID canónico ascendente. |

Cada item tiene exactamente `block`, `projectName` y `taskTitle`. `block` reutiliza el DTO cerrado de nueve campos de feature11: id, projectId, taskId, objective, startAt, endAt, zoneId, durationMinutes y createdAt. Los nombres/títulos son los valores persistidos actuales, obtenidos dentro del mismo snapshot y bajo el mismo filtro de propietario. No se añaden estados derivados de haber pasado el horario. Los IDs current/next, cuando existen, referencian items distintos de esta respuesta. Un inicio exactamente en serverNow pertenece a current, no a next; un final exactamente en serverNow ya no pertenece a current. Las reservas existentes mantienen la prohibición de solape, también entre proyectos y aunque estén completed.

El cálculo de plannedSeconds recorta cada intervalo sólo para el resumen de capacidad. No añade una petición ficticia a BlockBudget. ClosingAt conserva el fin real aunque caiga mañana; no se sustituye por dayEndAt. Una agenda vacía devuelve items vacíos, plannedSeconds cero y current/next/closing null; conserva el presupuesto conocido, si lo hay.

El cliente valida el esquema cerrado, tipos, DTOs, unicidad/orden, referencias current/next y coherencia de límites, sumas y nulls antes de presentar la respuesta. Valida que date sea una fecha ISO de calendario válida, pero no recalcula su correspondencia local con los límites del día mediante TZDB: esa correspondencia se garantiza en el servidor. No exige equivalencia entre los catálogos del cliente y del servidor, ni resolver la zona efectiva o las zonas históricas para aceptar un DTO válido. Una respuesta inválida se trata como fallo de lectura, nunca como agenda vacía ni como dato confirmado. La identidad del propietario procede exclusivamente de la sesión del servidor, no de campos confiados al navegador.

### Errores y privacidad

Se mantienen los filtros de seguridad y el formato de errores existentes. Sin sesión: 401. Cualquier parámetro de consulta: 400 VALIDATION_ERROR con el campo correspondiente, después de los filtros de seguridad. Fallo reconocido de almacenamiento, incluido el cierre de la transacción: 503 STORAGE_UNAVAILABLE, sin detalles SQL ni datos de otros usuarios. Una lectura fallida de disponibilidad no equivale a disponibilidad no configurada. Este GET no requiere headers de idempotencia o revisión y no escribe aunque falle. Las respuestas exitosas y los errores no se guardan en almacenamiento persistente del navegador.

Cada petición pertenece a la sesión, ruta y generación de actualización que la inició. Salir de Hoy, cerrar/cambiar sesión o sustituir la petición invalida su resultado y cancela la solicitud cuando sea posible. Ninguna respuesta antigua, incluso un error de autenticación, altera una sesión o pantalla posterior. El logout borra inmediatamente la agenda privada; una sesión nueva no muestra datos de la anterior mientras carga.

### Pantalla y actualización

La ruta `/` pasa a ser Hoy, entrada autenticada. Muestra fecha y zona efectiva, nombres de proyecto/tarea, objetivo e intervalo de cada reserva, y enlaces a la ruta existente `/proyectos/{projectId}/tareas/{taskId}`. Mantiene los intervalos reales con fecha cuando cruzan día. Presenta «En horario planificado», «Próximo inicio planificado» y «Cierre previsto», sin afirmar que se está trabajando ni que lo pasado se ha completado. El cierre del día siguiente se etiqueta con su fecha o «mañana». Los resúmenes son «Tiempo planificado», «Presupuesto del día», «Presupuesto sin reservar» y exceso, nunca productividad ni trabajo realizado.

La zona de visualización es explícita. Si Intl no reconoce la zona efectiva o una zona histórica mostrada, presenta el instante UTC etiquetado y conserva el ID de zona original; nunca usa silenciosamente la zona del dispositivo. En fallback de preferencia muestra el motivo, capacidad desconocida y enlace a `/disponibilidad`. Una agenda vacía explica que no hay bloques planificados y ofrece `/proyectos`; no equivale a error. Los enlaces de tareas completed siguen disponibles mientras sus bloques existan.

La pantalla carga al entrar y permite «Actualizar». Refresca al volver a estar visible/con foco, agrupando los eventos de una misma recuperación para no lanzar peticiones duplicadas y compartiendo la petición si ya hay una vigente. Mientras está visible mantiene un único timeout para la siguiente frontera futura del snapshot: el mínimo entre los inicios/finales de sus bloques y dayEndAt. Al vencer invalida la generación y consulta de nuevo; no hay polling periódico, segundero ni WebSocket. La espera se calcula con la diferencia respecto a serverNow y se ancla al tiempo transcurrido desde recibir la respuesta, no a la fecha o zona configurada en el dispositivo. Al ocultarse cancela ese timeout; al regresar obtiene un snapshot nuevo y lo programa otra vez. Sólo programa fronteras estrictamente futuras para evitar bucles.

Cada snapshot muestra «Según actualización de …» con la hora del servidor. Durante actualización conserva los datos con indicación de actualización pendiente, salvo al vencer dayEndAt: retira la agenda anterior de la presentación de Hoy y carga el nuevo día. Un error inicial o tras rollover muestra fallo y reintento, no un vacío de negocio. Un fallo al refrescar dentro del mismo día conserva el último snapshot claramente señalado como «Sin actualizar», con su fecha/hora, y un reintento manual; no vuelve a programar automáticamente una frontera ya vencida. Tras fallo, la recuperación visible/con foco también puede reintentar. No promete observar cambios externos mientras permanece visible antes de una frontera o actualización manual.

### Migración de navegación y accesibilidad

La captura de proyecto se mueve a `/proyectos/nuevo`, que debe resolverse antes del matcher genérico de proyectos. Los dos enlaces de creación de la lista/vacío y las pruebas que usaban `/` para capturar pasan a esa ruta. Se conservan `/proyectos`, su paginación, detalle y edición de proyecto, detalle de tarea y `/disponibilidad`, incluida la entrada directa y retorno de autenticación existentes. Con una sesión ya autenticada en el sondeo inicial, las rutas locales no reconocidas se conservan y muestran página no encontrada con enlaces Hoy/Proyectos, sin consulta inventada ni formulario de creación. Después de un login desde estado anónimo se mantiene el guard existente: un retorno desconocido o inseguro se descarta hacia Hoy; esta migración no amplía los destinos admitidos de retorno. El reinicio de ruta a `/` al cerrar sesión sigue siendo válido y lleva a Hoy tras el acceso posterior.

Workspace distingue Hoy, Proyectos y Disponibilidad: enlaces, breadcrumb y aria-current corresponden a la ruta efectiva, con una sola entrada activa. El skip link sigue llegando al contenido principal enfocable; se permite conservar el ID legacy `proyectos` en ese contenido para evitar un renombrado transversal. Carga, error y actualización se anuncian accesiblemente sin robar foco; «Actualizar»/«Reintentar» y enlaces son operables con teclado. Agenda vertical legible en móvil y sin scroll horizontal accidental, con la matriz de 30 principios y evidencia responsive de docs/ux-requirements.md aplicadas a este flujo; no se afirma cumplimiento global sólo por axe.

### Decisiones y límites deliberados

| Decisión | Alternativa descartada y razón |
| --- | --- |
| UTC explícito y presupuesto null si falta zona utilizable. | Usar la zona del dispositivo, cero o bloquear toda lectura ocultaría el contexto o impediría consultar reservas válidas. |
| Item con DTO de bloque reutilizado y nombres mediante join. | UUIDs solos o consultas por tarea hacen la agenda ilegible o añaden peticiones y snapshots inconsistentes. |
| Snapshot read-only único y reloj capturado una vez. | Lecturas independientes pueden mezclar disponibilidad y reservas de revisiones distintas. |
| Intersección para capacidad y fin real para cierre. | Recortar también el cierre fingiría que una reserva termina a medianoche. |
| Agenda completa de un único día. | Paginación o historial arbitrario complica current/next/resumen y pertenece a vistas posteriores. |
| Hoy en raíz y captura en ruta explícita. | Añadir sólo /hoy dejaría incumplida la entrada Hoy ya aprobada; cambiar deep links existentes no es necesario. |
| Un timeout de frontera, foco/visibilidad y actualización manual. | Polling continuo o reloj de trabajo añade carga y confunde planificación con ejecución. El snapshot fechado hace explícito el límite de frescura. |

La destilación Gherkin cubrirá familias observables acotadas: aislamiento y errores; snapshot y ausencia/cambio de preferencia; día/DST/intersecciones; capacidad conocida/desconocida y completed; current/next/cierre; lectura y fallback; refresco/obsolescencia/logout; migración de rutas y accesibilidad. La validación del esquema se agrupa por contrato, sin convertir variantes de mutadores en escenarios de producto. No se requiere nueva migración o índice sin evidencia de necesidad, ni introducir un framework temporal o de cache para este MVP.

## Feature 13 — reschedule

Especificación revisada y contrato features/reschedule.feature aprobado bajo autorización global:41 escenarios/156 casos, dictámenes en progress/review_reschedule_spec.md y progress/review_reschedule_contract_backend.md. Las menciones a propuesta/revisión siguientes conservan el razonamiento previo y quedan resueltas por esta aprobación; feature13 pasa a in_progress, sin declarar implementación terminada.

### Propósito y límites

Mover o cancelar deliberadamente un bloque planificado, conservando quién es su propietario, su identidad y los hechos anteriores. La planificación no acredita trabajo. Se propone el editor inline y el listado del detalle de tarea ya existentes; Hoy refleja el resultado en su siguiente lectura normal. No añade calendario, arrastre, sesiones, recurrencia, deshacer automático, traslado entre tareas/proyectos, edición del objetivo ni replanificación masiva. El coordinador debe revisar esta propuesta antes de Gherkin; feature13 permanece pending.

Mover permite cambiar inicio, fin y zona, incluida la duración dentro de los límites de feature11. Mantiene id, projectId, taskId, objective y createdAt. El destino cumple los mismos límites de fechas/instantes, duración real1–1440 minutos enteros, inicio no anterior al reloj del servidor, catálogo y resolución explícita DST de feature11. Puede mover una reserva cuyo horario original ya pasó: no existe sesión realizada que deba corregirse. Para un movimiento nuevo se exige tarea pending y proyecto distinto de completed. Cancelar se permite aunque el horario haya pasado, la tarea/proyecto estén completed o la disponibilidad falte o su zona ya no sea resoluble. Es retirar una reserva, no borrar trabajo. Cancelled es terminal en este corte; volver a reservar requiere una creación explícita nueva.

### Compatibilidad y hechos duraderos

El DTO Block de nueve campos y los quince campos de Today no se amplían. La revisión y el estado del bloque se exponen fuera de Block. Un bloque parte de estado planned y revisión1; cada movimiento/cancelación efectivo aumenta exactamente una revisión. No cambia versiones/fechas de proyecto, tarea ni disponibilidad.

Se distingue el recibo de creación original de la proyección vigente. POST de creación con su misma key/intención y GET /by-request de feature11 siguen devolviendo exactamente el DTO originalmente confirmado, incluso tras mover/cancelar, sin otro evento ni reinterpretación temporal. planned_blocks y su petición normalizada permanecen como recibo original inmutable; una proyección opcional guarda sólo el estado vigente posterior. Ausencia de proyección significa planned/revisión1 y snapshot de creación, sin escritura desde GET ni backfill de copias. No se sobrescribe el objetivo, tiempo ni intención originales para resolver ese replay contra la proyección nueva.

Cambio explícito de lectura para13: el listado existente de bloques contiene sólo reservas planned con su intervalo vigente, conservando items/nextCursor,20elementos y orden createdAt/id descendente. GET /{blockId} devuelve el DTO vigente si planned; cancelled devuelve BLOCK_NOT_FOUND. La lectura nueva de estado e historial conserva acceso al snapshot cancelado. Today y los cálculos de solape/capacidad usan únicamente proyecciones planned; dejan de contar el intervalo anterior al mover y cualquier reserva cancelada. Today conserva snapshot read-only REPEATABLE_READ, nombres actuales y reglas de refresco12. No se notifican cambios entre pestañas mediante polling ni se atribuye frescura anterior al próximo refresh.

Una confirmación recuperada de creación o cambio prueba ese hecho histórico, no que siga siendo la reserva vigente. La UI conserva el artículo de confirmación original de11, etiquetado como hecho histórico; ese artículo no inyecta el DTO en el listado, que se recarga por separado. Consulta el estado actual antes de presentar el recibo como vigente o habilitar otra acción. No sustituye los checks de intención contra preview ni by-request por el GET de detalle: readBlock de11 se usa en BlockConflict, no para recuperar ACK. Si esa lectura falla, conserva «Operación confirmada; estado actual sin comprobar», no repone una reserva antigua al listado activo.

### Estado y revisión antes de actuar

Base heredada B=/api/v1/projects/{projectId}/tasks/{taskId}/blocks. GET B/{blockId}/state devuelve200 con exactamente block (DTO9), status (planned|cancelled) y updatedAt (UTC microsegundos), más ETag fuerte `"block:{blockId}:{version}"`. Version es entero positivo decimal canónico dentro de BIGINT y viaja en el ETag, no en Block. El cliente conserva esa revisión como texto y usa BigInt si debe comparar o incrementar: nunca Number para versiones que pueden exceder MAX_SAFE_INTEGER. Estado inicial updatedAt=createdAt. Cancelled conserva el último Block previo a cancelar. El contexto propio inexistente/ajeno usa RESOURCE_NOT_FOUND; blockId desconocido dentro de ese contexto usa BLOCK_NOT_FOUND. No hay lecturas por cada elemento del listado: se consulta estado al abrir sus acciones.

Mover y cancelar exigen If-Match de ese bloque. Ausente:428 PRECONDITION_REQUIRED; sintaxis inválida, débil, comodín, lista, repetición, otro blockId o versión fuera de rango:400 VALIDATION_ERROR en If-Match. Revisión actual distinta:412 BLOCK_CONFLICT, también si otra operación ya canceló el bloque. Sólo una revisión actual sobre estado cancelled recibe409 BLOCK_CANCELLED; un replay confirmado conserva prioridad sobre ambos. No reintentar contra una revisión nueva silenciosamente. Agotar la versión no permite wrap ni éxito parcial:409 BLOCK_VERSION_EXHAUSTED, sin escritura. Se comprueba después de revisión y estado planned, antes de disponibilidad/otros negocios, también en preview para no ofrecer una acción imposible. El replay confirmado conserva prioridad.

### Revisión y confirmación de movimiento

POST B/{blockId}/reschedule/preview recibe exactamente startLocal,endLocal,zoneId,startOffset,endOffset, con offsets null sólo para resolución inequívoca. Exige If-Match pero no key ni Availability-Revision. Comprueba propiedad, revisión y estado planned, disponibilidad, elegibilidad y tiempo. No escribe. Devuelve el mismo cuerpo cerrado de preview11 (objective procede del bloque vigente), con ETag de bloque revisado como header y availabilityEtag de preferencia dentro del cuerpo.

Solape y presupuesto se recalculan excluyendo por id la reserva que se mueve, nunca restando a ciegas su duración total. plannedSeconds suma las otras reservas planned sobre cada día afectado del destino; requestedSeconds representa sólo el destino propuesto. El intervalo original no puede solapar consigo mismo. Los bloques contiguos siguen admitidos; el permiso de exceso no habilita solape. Se muestra antes/después y la zona de presupuesto. El consentimiento de exceso se invalida con cualquier cambio y no se hereda de la creación ni del movimiento anterior.

POST B/{blockId}/reschedule recibe esos cinco campos con offsets explícitos y allowOverBudget booleano obligatorio. Headers: If-Match, Availability-Revision e Idempotency-Key. Repite bajo transacción la revisión de bloque, revisión de disponibilidad y reglas vigentes, incluyendo inicio frente al reloj actual. El cuerpo normalizado, blockId y tipo de operación definen la intención; ninguno de los dos headers de revisión forma parte de ella. Las revisiones se retienen juntas con la intención al recuperar.

Primero se resuelve y valida el tiempo, incluido inicio no pasado; después se comprueba si hay cambio, antes de solape/presupuesto. Por ello repetir el intervalo original ya pasado da el error temporal y no BLOCK_UNCHANGED. Una propuesta válida que tras resolución deja los mismos instantes y zoneId se rechaza con409 BLOCK_UNCHANGED, sin cambio, recibo ni outbox. No se usa un permiso distinto para inventar un cambio. Un movimiento con diferente zoneId sí es un cambio aunque conserve instantes, porque cambia la zona de planificación mostrada. Preview también informa BLOCK_UNCHANGED en esa situación. Las reglas de fecha/offset, validOffsets, presupuesto, solape y su representación son las heredadas de11, sin duplicar sus325 casos en la nueva destilación.

### Cancelación, recibos y recuperación

POST B/{blockId}/cancel recibe exactamente {} y exige If-Match e Idempotency-Key, pero no Availability-Revision. Muestra previamente una confirmación inline: retirar ese objetivo/intervalo de planificación y liberar capacidad, conservando el historial. Primera cancelación de planned cambia a cancelled; una intención nueva sobre cancelled devuelve409 BLOCK_CANCELLED. Mover cancelled también devuelve ese error. El replay de una cancelación ya confirmada sí devuelve su éxito original.

Cada operación efectiva tiene un recibo cerrado con exactamente id,blockId,kind,revision,occurredAt,before,after. id lo genera el servidor como UUID canónico, igual que blockId y los IDs heredados; kind es RESCHEDULED o CANCELLED; revision es el ETag fuerte de la revisión resultante, como texto; before es Block9 previo y after es Block9 nuevo o null exclusivamente al cancelar. occurredAt es un único instante del reloj del servidor truncado a microsegundos, igual al updatedAt del cambio. El reloj puede retroceder: no se exige que ese instante sea posterior a createdAt ni al cambio previo, y los validadores no añaden esa comparación. La revisión representa la secuencia efectiva; el historial ordenado por occurredAt/id representa cronología de fechas registradas, no necesariamente causalidad. No incluir propietario ni key en el recibo HTTP.

Primera operación confirmada devuelve201 con ese recibo y Location B/changes/{id}; replay devuelve200 con mismo cuerpo/Location. GET B/changes/{id} y GET B/changes/by-request/{requestKey} devuelven ese recibo200, o404 BLOCK_CHANGE_NOT_FOUND dentro de contexto propio. Las rutas literales se resuelven antes del matcher UUID. El recurso original blockId debe existir dentro del contexto propio antes de buscar cualquier recibo por key de una acción sobre ese bloque. Un blockId inexistente devuelve BLOCK_NOT_FOUND aunque la key pertenezca a otro bloque existente; un blockId existente con esa key de otro bloque devuelve IDEMPOTENCY_CONFLICT, sin divulgar su recibo. La key UUID canónica es única por tarea en el espacio de cambios, compartido entre mover/cancelar; el espacio de creación11 sigue separado. Reutilizarla para otro bloque/tipo/cuerpo produce409 IDEMPOTENCY_CONFLICT.

Tras autenticación, validación de headers/cuerpo y propiedad se busca un recibo confirmado antes de rechazar estado, If-Match/Availability-Revision obsoletos, elegibilidad, tiempo o catálogo. La validación anterior a replay es sólo estructura, tipos, formatos y normalización sintáctica estable (incluidos offset canónico y fechas locales estrictas); no consulta ZoneCatalog ni resuelve reglas DST, presupuesto o instante actual. El texto de zona se compara tal como se recibió, sin aliasado dependiente del catálogo. El replay no deshace cambios posteriores. Confirmación/replay requieren comprobar esquema, contexto, tipo y correspondencia del snapshot con la intención retenida; un200/201 incompatible sigue siendo incierto.

Red,503, cuerpo inválido y códigos desconocidos mantienen intención/key bloqueadas. «Comprobar cambio» consulta por key. Un404 de recibo no prueba rollback de una petición aún en vuelo: permite otra comprobación o reenviar manualmente exactamente la misma intención/key/headers. No genera key nueva ni hace POST automático. Un409 IDEMPOTENCY_CONFLICT conserva recuperación; CSRF reconocido permite renovación manual y posterior reenvío de la misma intención. Rechazos definitivos conocidos (validación, revisiones, estado, elegibilidad, solape, presupuesto o sin cambio) conservan borrador y requieren nueva revisión antes de otra intención. Salir del editor no cancela una operación transmitida, se explica igual que11.

### Historial, transacción y eventos

GET B/changes devuelve exactamente items y nextCursor: recibos propios,20por página, occurredAt DESC e id DESC. Cursor opaco base64url canónico de JSON cerrado collection=blockChanges,projectId,taskId,occurredAt,id, vinculado a esa colección; se rechazan cursores de otras colecciones y parámetros desconocidos/repetidos. Los IDs del cursor son UUID canónicos con las reglas11; occurredAt cumple UTC/microsegundos y rango heredados. Se usa lookahead21: nextCursor sólo existe si hay más elementos, y es null en una página terminal incluso si contiene exactamente20. Historial inicial vacío; creación se recupera por11, no se fabrica retrospectivamente un cambio. Los recibos no se alteran al mover otra vez ni cancelar. Esta lista permite volver a consultar cancelados después de recargar, sin nueva pantalla de historial global.

Adaptador PostgreSQL con transacción READ_COMMITTED y orden de bloqueo heredado: proyecto propio FOR SHARE, tarea propia FOR SHARE, disponibilidad propia FOR UPDATE, bloque FOR UPDATE. Preview usa disponibilidad FOR SHARE y bloque FOR SHARE. Se confirma propiedad y se busca replay antes de negocio; después de esperar el bloqueo se reconsulta replay antes de aplicar una intención nueva. La fila de disponibilidad continúa serializando reservas entre proyectos del mismo propietario. Cancelar bloquea esa fila si existe, sin interpretar su zona/presupuesto; si no existe puede retirar bajo bloqueo del bloque, pues no añade una reserva. No se introduce lock global ni se depende del estado de RabbitMQ para confirmar.

Proyección, incremento de revisión, recibo durable con petición normalizada y outbox se escriben atómicamente; falta/supresión de cualquiera o fallo previo al commit revierte todo. Una migración aditiva conserva planned_blocks sin UPDATE/DELETE de sus hechos de creación; no reescribe migraciones previas ni infiere hechos de eventos publicados. Diseño físico propuesto: una tabla de proyección opcional por block_id y una tabla de cambios que sirve tanto historial como deduplicación; planned_blocks ya es el recibo original, por lo que no se añade un tercer recibo ni backfill copiado. El FOR UPDATE del bloque toma su fila original existente antes de insertar/actualizar una proyección, evitando confiar en un lock sobre una fila de estado todavía ausente. No event sourcing ni framework nuevo; dominio puro y casos de uso/puertos para coordinar reloj, invariantes y almacenamiento.

Se propone un solo evento nuevo BlockChanged.v1, para los dos tipos de cambio, con exactamente eventId,aggregateId (proyecto),ownerId,occurredAt,schemaVersion=1,type=BlockChanged.v1,changeId,blockId,taskId,kind,revision,before,after. revision es el número BIGINT positivo resultante en el evento backend; before/after contienen sólo startAt,endAt,zoneId,durationMinutes, y after=null al cancelar. No publicar objective, key, fechas locales del formulario ni nombres. Ruta block.changed.v1, cola quorum durable organization.block-changed.v1; conserva siete rutas previas y el protocolo at-least-once, confirmación, retries y blocked. Payload cerrado e instantes/duración coherentes; zoneId histórico textual no vacío, sin resolverlo contra TZDB al publicar. Evento y recibo corresponden al mismo cambio pero eventId y changeId son identidades distintas. No exigir orden de entrega; revisión permite reconocer el orden del agregado bloque sin afirmar un consumidor nuevo.

### UI, errores y verificación prevista

Desde cada elemento del listado, «Mover bloque» y «Cancelar bloque» abren un único panel inline tras consultar su estado. Reutilizar controles nativos datetime-local/zona y feedback de11; los valores locales para editar deben corresponder al instante y zona confirmados, sin conversión implícita de navegador. Si Intl no reconoce la zona histórica, mostrar UTC etiquetado e ID original; antes de revisar un movimiento la persona debe escoger una zona resoluble y horas explícitas, sin inventar la conversión. La resolución final siempre es del servidor. Objective permanece visible y de sólo lectura.

«Cancelar edición» cierra el formulario; «Confirmar cancelación del bloque» identifica la acción de negocio. Carga/errores/confirmación/recuperación son distintos. Tras412 ofrecer consultar estado nuevo, conservar borrador pero retirar preview/consentimiento; nunca sobreescribir automáticamente el cambio ajeno. Cancelación confirmada retira el elemento y muestra su recibo; un fallo de listado posterior no revoca la operación confirmada. «Cambios de bloques» muestra historial paginado inline con antes/después y fecha, accesible incluso cuando no quedan reservas activas. No se requiere un GET por fila ni un modal.

Se mantienen formatos problem+json/no-store, privacidad y precedencia de seguridad de11. Para acciones nuevas: query, IDs, If-Match, Availability-Revision si corresponde, key, sintaxis/estructura, propiedad y existencia del bloque, replay, comparación de revisión de bloque, estado, agotamiento de versión, disponibilidad/revisión, elegibilidad, tiempo, ausencia de cambio, solape y presupuesto. Validación no filtra propiedad; ausencia de contexto nunca se disfraza de ausencia de recibo. Errores conocidos de almacenamiento son503 STORAGE_UNAVAILABLE. Los nuevos códigos estables son BLOCK_CONFLICT, BLOCK_CANCELLED, BLOCK_UNCHANGED, BLOCK_VERSION_EXHAUSTED y BLOCK_CHANGE_NOT_FOUND; se reutilizan los demás sin cambiar contratos antiguos.

Guardas de sesión/ruta/generación tras cada await, incluido clasificar errores y renovar CSRF; cancelar peticiones obsoletas antes de que un401 tardío alcance el observer compartido. Mantener recuperación aunque la tarea/proyecto dejen de ser elegibles;401 actual retira datos inmediatamente. Foco visible conservado si la persona no lo movió; al desaparecer el control/elemento, llevarlo al encabezado de Bloques planificados, sin intentar enfocar un nodo retirado. Aplicar las30filas UX y matriz responsive/zoom/motores de docs/ux-requirements.md, con alternativas completas por teclado/táctil y límites humanos/físicos explícitos.

La futura destilación agrupará familias observables: compatibilidad de creación11; estado/revisión/historial; mover excluyendo propia reserva y liberar días anteriores; cancelar incluso completed/sin zona; DST/medianoche heredados; idempotencia y ACK perdido/reinicio; carreras move/move,move/cancel,cancel/cancel y reservas/estados/disponibilidad en ambos órdenes; rollback/commit/outbox/publicación; snapshots Today previos o posteriores coherentes; UI/privacidad/foco/recuperación. No se escribe Gherkin ni TDD durante esta propuesta.

### Decisiones pendientes de revisión del coordinador

Las alternativas y motivos están en progress/proposal_reschedule.md. El coordinador aceptó preliminarmente duración/zona editables, cancelación histórica terminal, historial por tarea, evento común y recibos201/200. Se adopta para revisión final identidad estable con planned_blocks inmutable y proyección opcional; el documento completo aún no constituye contrato Gherkin aprobado. La revisión debe confirmar esas decisiones antes de destilar el contrato, sin pedir otra autorización global al usuario.

## 14. Iniciar sesión de trabajo — especificación normativa

Feature 13 está cerrada y fusionada por el usuario en `9623990`; main publicado `d997421`. Esta sección incorpora las decisiones aceptadas en `progress/proposal_start_work.md` y `progress/review_proposal_start_work.md`. Define el alcance de 14 para su destilación Gherkin; todavía no declara ese contrato aprobado ni inicia implementación. Se conserva la autorización global del usuario y el proceso de una feature a la vez.

### Hecho de inicio y fin previsto

Una sesión de trabajo comienza únicamente al confirmar «Empezar a trabajar» sobre una tarea propia. Un inicio nuevo requiere tarea `pending` y proyecto distinto de `completed`; los estados `idea`, `active` y `paused` no se restringen adicionalmente. No requiere bloque planificado, disponibilidad configurada, presupuesto positivo ni vínculo a una reserva. No recibe `blockId`, objetivo nuevo o nombres: usa el contexto existente de la tarea.

El inicio no modifica bloques, capacidad, estimaciones, Today, estado de tarea/proyecto ni historial de finalizaciones. Acredita un instante de inicio real; no acredita minutos terminados, tiempo neto, avance, racha o tarea completada. Una reserva sigue siendo planificación y una estimación sigue siendo estimación.

La única entrada de negocio es `plannedMinutes`, entero JSON obligatorio entre 1 y 1440 inclusive, elegido explícitamente. No se acepta string, booleano, fracción ni un valor ausente/null. La UI no precarga una duración como supuesta preferencia del usuario. Este límite se aplica al objetivo previsto, no impone un máximo retrospectivo al trabajo real.

El servidor captura `Clock.instant()` una vez para un inicio nuevo y lo trunca a microsegundos. Ese valor es `startedAt` y también `occurredAt` del evento. `plannedEndAt` se calcula sumando exactamente `plannedMinutes × 60` segundos a ese instante. Ambos instantes deben poder representarse en UTC dentro de los años 0001–9999; no se recorta ni redondea una suma fuera de rango. La duración relativa evita otro formulario de hora civil y otra resolución de ocurrencias DST. Cruzar medianoche o un cambio horario no altera la duración real elegida.

La zona de presentación se captura de la disponibilidad actual del propietario cuando pertenece al catálogo vigente y es resoluble; si no existe preferencia o su zona no es resoluble, se registra `UTC`. No se guarda una preferencia para producir ese fallback ni se exige corregirla antes de trabajar. Si el almacenamiento falla al consultar disponibilidad, se devuelve 503: no se disfraza como ausencia. La zona capturada es texto histórico; cambiar preferencias o catálogo después no cambia el recibo, sus instantes o su zona.

La fecha, hora y zona del fin previsto se muestran explícitamente. Ese fin permanece fijo aunque la respuesta tarde o se recupere más tarde. Alcanzarlo no amplía, pausa ni cierra la sesión. En 14 no se programa un aviso ni se presenta un contador como tiempo neto acreditado. La atribución histórica del día trabajado al cerrar sigue perteneciendo a 16.

### Una sesión activa y recuperación histórica

Cada propietario puede tener como máximo una sesión activa, incluso entre proyectos, tareas, pestañas y dispositivos. En 14 el único estado operativo es `running`; no hay transición de pausa o cierre. Completar después la tarea o el proyecto no elimina, cancela ni cierra el inicio registrado y no impide su consulta o replay. Las transiciones existentes de tarea/proyecto mantienen sus propios contratos.

La confirmación de inicio es inmutable y recuperable, separada conceptualmente de la consulta de actividad actual. En 14 la activa puede representarse con el mismo DTO de inicio porque no existen transiciones posteriores. Feature 15 deberá evolucionar explícitamente la representación de estado sin convertir el recibo histórico en estado mutable ni permitir dos sesiones no cerradas al introducir `paused`.

No se habilitará 14 sola para uso habitual como temporizador sin salida. La habilitación se revisará con el ciclo de inicio, pausa y cierre de 14–16. No se añade un cierre oculto, reset administrativo, eliminación de datos o cierre automático para suplir 16. Esta frontera no introduce infraestructura de flags ni autoriza despliegue.

### API y DTO cerrado

Las rutas son:

- `POST /api/v1/projects/{projectId}/tasks/{taskId}/work-sessions`: cuerpo cerrado con exactamente `plannedMinutes` y header `Idempotency-Key` obligatorio.
- `GET /api/v1/work-sessions/active`: respuesta200 cerrada con exactamente `session`, cuyo valor es `SessionStart` o `null` si se comprobó que no hay activa propia.
- `GET /api/v1/work-sessions/{id}`: recupera el inicio propio por ID, independientemente del estado de tarea/proyecto o del outbox.
- `GET /api/v1/work-sessions/by-request/{requestKey}`: recupera el mismo inicio por key propia. Las rutas literales no se tratan como IDs.

`SessionStart` contiene exactamente siete campos: `id`, `projectId`, `taskId`, `startedAt`, `plannedMinutes`, `plannedEndAt`, `zoneId`. IDs UUID canónicos generados por servidor, instantes UTC con precisión máxima de microsegundos y años 0001–9999, entero previsto 1–1440 y zona histórica no vacía. Se exige la relación exacta entre ambos instantes y la duración. No incluye propietario, key, nombres, revisión, estado mutable, `elapsedSeconds`, `netMinutes`, `endedAt` o `completed`.

El primer POST confirmado devuelve 201 sólo después del commit, `Location: /api/v1/work-sessions/{id}` y `SessionStart`. Un replay devuelve 200 con el mismo cuerpo y Location. GET por ID/key devuelve 200 con ese DTO; no añade Location de una operación nueva. GET active devuelve 200 con su envoltorio, nunca204 ni ausencia inventada ante un error. Ninguna lectura crea sesiones, preferencias, recibos o eventos.

Los validadores cliente comprueban forma cerrada, tipos, contexto, identidad y la relación de duración sin comparar los instantes históricos con el reloj actual ni resolver el catálogo de nuevo. Un201/200 incompatible no confirma el inicio. Para presentación con una zona histórica no soportada por `Intl`, mostrar los instantes en UTC etiquetado y conservar visible el ID de zona original; no usar silenciosamente la zona del navegador.

### Intención, precedencia y errores

`Idempotency-Key` es una UUID canónica única por propietario en el espacio de inicios. Este espacio es independiente de creación11 y cambios13. La intención normalizada contiene `projectId`, `taskId` y el entero `plannedMinutes`; no incluye reloj, zona elegida por servidor, cookies o CSRF. Reutilizar la key con otra tarea, proyecto o duración devuelve 409 `IDEMPOTENCY_CONFLICT`, sin mostrar otro inicio como confirmación de esa intención.

Se heredan filtros de autenticación, origen y CSRF, negociación de contenido, `application/problem+json`, `no-store`, validación UUID y JSON ilegible de 11/13. La negociación puede devolver415 antes del handler. Después de los filtros, el POST valida en este orden: query; IDs de ruta; key; sintaxis JSON; forma/campos extra; tipo/rango de `plannedMinutes`; propiedad y existencia de proyecto/tarea; replay; sesión activa; elegibilidad de proyecto y luego tarea; captura de reloj y zona; escritura. No exige `If-Match` ni `Availability-Revision` sobre una sesión que aún no existe.

La validación de query se aplica a las cuatro rutas. En los GET, seguridad precede a query y query precede a la validación del ID o key; GET active no admite parámetros. Query desconocida o repetida se rechaza con 400 `VALIDATION_ERROR`, campo `query`, código `INVALID_VALUE`. Key ausente usa campo `Idempotency-Key`/`REQUIRED`; repetida o mal formada conserva las reglas UUID comunes. JSON vacío, truncado, duplicado o concatenado usa400 `MALFORMED_JSON`. Raíz no objeto: `body`/`INVALID_TYPE`. Campo extra: nombre del campo/`UNKNOWN_FIELD`, seleccionando el primero por orden lexical. `plannedMinutes` ausente/null usa `REQUIRED`; tipo no entero usa `INVALID_TYPE`; entero fuera de 1–1440 usa `OUT_OF_RANGE`. No se reinterpreta una representación inválida para aceptar un replay.

Propiedad/existencia incorrecta del contexto del POST devuelve 404 `RESOURCE_NOT_FOUND`. GET de ID/key ajeno e inexistente devuelve el mismo404 `WORK_SESSION_NOT_FOUND`, sin divulgar propiedad. La consulta activa sólo devuelve información del propietario autenticado. Proyecto completed usa409 `PROJECT_COMPLETED`; tarea completed usa409 `TASK_COMPLETED`, con explicación apropiada para iniciar trabajo y sin cambiar los textos de rutas anteriores.

Una activa propia impide una intención nueva con 409 `WORK_SESSION_ALREADY_ACTIVE`: problema cerrado con los cuatro campos comunes `type`, `title`, `status`, `code`, más `sessionId`, ID de la activa propia recuperable. No devuelve otro recibo como éxito. El título explica que ya existe una sesión de trabajo activa. ID/key ausente usa problema común de cuatro campos con título «No se ha encontrado la sesión de trabajo.». Los fallos de almacenamiento, incluido el final de transacción, son503 `STORAGE_UNAVAILABLE`.

Si el reloj capturado o el fin calculado queda fuera del rango temporal admitido, se rechaza sin escribir con 409 `WORK_SESSION_TIME_OUT_OF_RANGE`, problema común de cuatro campos y título «No se puede representar el inicio y el fin previsto de la sesión.». No se atribuye una lectura de reloj inválida a un campo editable ni se clasifica como fallo de almacenamiento. No se utiliza este error en replay, que devuelve los instantes ya confirmados.

El replay confirmado precede a activa, completed, disponibilidad, catálogo y reloj actuales. Tras esperar una operación concurrente se vuelve a comprobar la key antes de decidir un conflicto de activa. Para la misma key con intención distinta prevalece `IDEMPOTENCY_CONFLICT`, aunque la ganadora haya creado ya una activa. No se sustituye el inicio previo ni se mueve su fin.

### Persistencia y orden concurrente

Dominio puro para inicio y duración; aplicación mediante puertos de iniciar/consultar, reloj y catálogo; adaptadores PostgreSQL, HTTP y outbox existentes. No se añade event sourcing, servicio de temporizadores, consumidor, broker o framework. No es necesario duplicar el recibo en otra tabla.

Una migración aditiva crea la persistencia de sesiones con los datos inmutables de inicio, propietario, key e intención, y el indicador operativo `running`. La misma fila conserva el recibo original. Debe existir unicidad de `(owner_id, request_key)` y una restricción única parcial por `owner_id` para `running`, además de la integridad de relación proyecto/tarea. Las columnas del hecho de inicio no se actualizan. No se copian bloques ni se fabrican inicios a partir de planes anteriores.

El comando usa READ_COMMITTED. Toma proyecto propio `FOR SHARE`, después tarea propia `FOR SHARE`, en ese orden compatible con las transiciones existentes. Para un inicio nuevo que alcance captura de zona, consulta la preferencia con `FOR SHARE` si existe. Una consulta vacía fija fallback UTC para esa operación, sin afirmar que bloquea una inserción posterior. No se usa disponibilidad como mutex de sesión ni se crea una fila por ausencia.

La unicidad de PostgreSQL decide el ganador de inicios simultáneos del propietario aun cuando no exista availability. No hay mutex global entre propietarios. Si dos peticiones alcanzan INSERT, una colisión esperada se resuelve después de terminar la transacción fallida: reconsultar por key propia y comparar intención antes de consultar activa. No reutilizar una transacción PostgreSQL abortada. No hace falta otro lock o tabla de propietario si estas restricciones y reconsultas garantizan las respuestas establecidas.

Misma key e intención:201 y200, un inicio y un evento. Keys distintas del mismo propietario: un201 y un409 `WORK_SESSION_ALREADY_ACTIVE`. Misma key con intenciones distintas: un201 y un409 `IDEMPOTENCY_CONFLICT`. La existencia/propiedad del contexto de cada petición se comprueba antes de divulgar resultados. La terminación concurrente de tarea/proyecto se ordena con sus locks: si confirma primero completed, se rechaza el inicio nuevo; si el inicio confirma primero, el hecho permanece recuperable y no se cierra por esa transición.

Inicio/intención/key y registro outbox se insertan en la misma transacción, con comprobación de una fila por escritura. Supresión de INSERT sin un ganador durable, fallo de outbox o error previo al commit revierte todo y devuelve 503. Una colisión de unicidad esperada no se presenta como503 genérico; tampoco se presenta una supresión sin ganador como conflicto. Un error de respuesta posterior a commit deja el hecho recuperable por key.

Los GET son transacciones read-only con consultas por propietario, sin `FOR UPDATE`/`FOR SHARE` ni materialización. No modifican flags de una plantilla compartida entre peticiones. El DTO de inicio se obtiene de la fila durable; active consulta la fila `running` propia. READ_COMMITTED basta para estas lecturas de una sola representación; no se impone el RR de Today a rutas independientes. Fallo de lectura o de finalización devuelve 503, nunca `session:null` ni404 inventado. El recibo sobrevive a reiniciar API y a retirar el outbox publicado.

### Evento y publicación

`WorkSessionStarted.v1` contiene exactamente `eventId`, `aggregateId`, `ownerId`, `occurredAt`, `schemaVersion`, `type`, `projectId`, `taskId`, `plannedMinutes`, `plannedEndAt`, `zoneId`. `aggregateId` es el ID de sesión; `occurredAt` es el mismo `startedAt` capturado; `schemaVersion` es 1 y `type` es `WorkSessionStarted.v1`. EventId se genera independientemente de la identidad de sesión. No publica key, nombres ni criterio de finalización.

La ruta es `work-session.started.v1` y la cola quorum durable `organization.work-session-started.v1`. Reutiliza confirms, entrega al menos una vez, reintentos y clasificación blocked existentes, conservando las ocho rutas anteriores. La validación exige forma cerrada, identidad, duración prevista y relación de instantes coherentes; acepta zona histórica textual sin consultar el catálogo al publicar. Broker caído no impide confirmar el commit local. Un resultado incierto puede producir redelivery con la misma identidad, sin prometer exactamente una vez.

### Interfaz, incertidumbre y límites de 14

El formulario de tarea muestra contexto, «Duración prevista (minutos)» y «Empezar a trabajar». No empieza por montar una pantalla, llegar la hora de una reserva, recuperar login o reenviar automáticamente tras un error. La sección «Sesión de trabajo» consulta activa al montarse o al volver a ella y ofrece actualización manual, sin polling por segundo. Distingue consulta pendiente, ausencia, error y activa propia. Con activa muestra inicio, duración, fin previsto, zona y enlace a la tarea; no muestra controles de pausa/cierre todavía inexistentes.

Mientras un POST está pendiente, la intención queda retenida y bloqueada con feedback anunciado. Red,503, cuerpo incompatible, códigos desconocidos e `IDEMPOTENCY_CONFLICT` conservan incertidumbre. «Comprobar inicio» usa GET by-request. Un404 de recibo no acredita rollback de una petición en vuelo; permite comprobar de nuevo o reenviar manualmente la misma intención/key. No genera key nueva ni cambia `plannedMinutes` mientras se resuelve. Los rechazos definitivos reconocidos permiten corregir una nueva intención; el conflicto de activa ofrece consultar la activa propia, sin borrar otra intención todavía incierta.

CSRF reconocido reutiliza la renovación manual de SessionGate; el reenvío de la intención es otra acción manual y usa el token vigente. Cerrar el formulario no revoca un POST enviado y se explica antes de salir. Tras recarga sin key en memoria, consultar activa descubre el inicio confirmado de 14; no promete recuperar una petición que nunca llegó ni reemplaza el futuro historial18. El recibo por ID/key sigue disponible aunque el contexto haya pasado a completed.

Antes de mostrar éxito se comprueba el DTO completo y su correspondencia con la intención. «Sesión iniciada» anuncia el hecho confirmado; un error posterior al consultar activa no lo revoca. Guardas de sesión/ruta/generación después de cada await, también al clasificar error; abortar antes de propagar401 obsoleto. Un401 actual retira datos inmediatamente. El foco no se roba si la persona eligió otro control; si desaparece el iniciador y el foco queda sin destino, se lleva al encabezado de sesión. Espera y error conservan borrador y controles accesibles.

Se aplica la matriz de 30 principios de `docs/ux-requirements.md`, con evidencia por alcance, responsive, ampliación, motores, teclado y foco. El fin previsto fijo, distinción plan/trabajo y lenguaje neutral favorecen constancia sin culpa. No se afirma usabilidad humana o dispositivos físicos sin evidencia ni se añaden rachas, presión para continuar o métricas ficticias.

Quedan fuera de 14: pausa/reanudación e intervalos netos de 15; cierre, avance, siguiente paso y atribución del día de 16; aviso y ampliación deliberada de 17; historial global/filtros de 18; métricas semanales, corrección de hechos y vínculo retrospectivo con bloques. Las transiciones futuras deberán mantener la unicidad de toda sesión no cerrada y la inmutabilidad del inicio. La persistencia y recuperación de 14 no equivalen a haber implementado esas funciones.

## 15. Pausar y reanudar — propuesta normativa para revisión

### Alcance y continuidad

La persona puede pausar una sesión propia `running` y reanudar esa misma sesión `paused`. Ambas ocupan la única plaza de sesión no cerrada del propietario, entre tareas, pestañas y dispositivos. No hay pausa automática, cierre, borrado, corrección retrospectiva ni inicio de otra sesión mientras está pausada. Cierre, avance y atribución del día siguen perteneciendo a16; avisos/ampliación a17 e historial global a18. El fin previsto, duración prevista, inicio y zona de14 son inmutables. Ni pausar ni reanudar altera reservas, presupuesto, Today, estimaciones o estados de tarea/proyecto.

Se permiten ambas transiciones aunque el proyecto o tarea se hayan completado después del inicio: gestionar la sesión ya abierta no exige reabrir el contexto. Propiedad y existencia de la sesión sí se comprueban siempre. No se consulta disponibilidad ni catálogo para pausar, reanudar o recuperar. Una zona histórica desconocida conserva la presentación UTC etiquetada de14.

### Intervalos y estado actual

El inicio14 abre un intervalo de trabajo en `startedAt`. Pausar lo termina y reanudar abre el siguiente. Los intervalos son semicerrados `[inicio, fin)`, ordenados y sin solapamiento; una transición en el mismo microsegundo admite duración cero. Pausas no suman trabajo. La suma exacta de intervalos terminados se conserva en microsegundos, sin redondear cada intervalo a minutos. El intervalo abierto aporta sólo tiempo transcurrido hasta el snapshot consultado. Ese neto es provisional, no una sesión finalizada, productividad, progreso ni crédito diario.

Cada comando nuevo captura el reloj del servidor una vez, después de obtener la fila de sesión para actualización, y trunca a microsegundos. El instante debe estar en años UTC0001–9999 y no preceder a la última transición confirmada; un retroceso rechaza el comando con409 `WORK_SESSION_TIME_OUT_OF_RANGE`, sin recortar el reloj ni escribir. La hora del dispositivo no decide transiciones. Cruzar medianoche, DST o el fin previsto no modifica esta aritmética ni causa acciones automáticas.

La proyección `WorkSessionState` tiene exactamente seis campos: `session` (el DTO7 `SessionStart` original), `status` (`running` o `paused`), `revision` (cadena decimal canónica positiva hasta9223372036854775807), `changedAt` (instante de última transición), `workedMicroseconds` (cadena decimal canónica no negativa de intervalos terminados) y `runningSince` (instante cuando running, null cuando paused). changedAt no precede a startedAt; runningSince es exactamente changedAt cuando running y null cuando paused. workedMicroseconds no supera la duración exacta entre startedAt y changedAt en microsegundos. Todos estos instantes son UTC, con precisión máxima de microsegundos y años0001–9999. Una sesión14 existente se lee inicialmente con revisión `"1"`, changedAt y runningSince iguales a startedAt, workedMicroseconds `"0"`. Cada transición aumenta revisión en uno. No se materializa estado mediante GET ni se reescribe el recibo de inicio para migrar.

`GET /api/v1/work-sessions/{id}/state` devuelve200 con exactamente `state`, `serverNow` y `netMicroseconds`. El último es una cadena decimal canónica no negativa, exactamente workedMicroseconds más `max(0, serverNow-runningSince)` en microsegundos cuando running; estando paused es workedMicroseconds. El máximo evita neto negativo si retrocede el reloj durante una lectura; no habilita una transición con reloj atrasado. En transacción read-only REPEATABLE_READ se lee primero la sesión propia y su proyección coherente; después se captura una sola vez el reloj, truncado a microsegundos, para serverNow y el cálculo. Una transición concurrente posterior al snapshot no se incorpora parcialmente. serverNow se serializa en UTC y exige años0001–9999; fuera de rango devuelve409 `WORK_SESSION_TIME_OUT_OF_RANGE` con el título temporal15, sin escrituras. La ausencia de sesión se resuelve antes de capturar el reloj. La respuesta incluye `Work-Session-Revision: work-session-{id}-{revision}`, token canónico sin comillas cuyo id y revisión coinciden con state.session.id y state.revision. No se devuelve ETag ni Location. El cliente valida tipos, invariantes del estado y relación neta exacta con enteros, sin conversión imprecisa a Number ni comparación con su reloj.

Los GET por ID y por key de inicio14 conservan exactamente el DTO7 histórico. `GET /work-sessions/active` conserva su envoltorio `session` y DTO7; desde15 encuentra también paused, porque significa sesión no cerrada. Nunca deducir running de ese recibo: la UI consulta `/state` para conocerlo. Ausencia comprobada sigue siendo200 con null; un error no equivale a ausencia. Este ajuste mantiene la recuperación de14 y la unicidad futura de16 sin mezclar representación histórica y mutable.

### Comandos, recibos y precedencia

`POST /api/v1/work-sessions/{id}/pause` y `/resume` requieren cuerpo JSON cerrado `{}`, `Idempotency-Key` UUID canónica y `Work-Session-Revision` con el token consultado. No reciben hora, duración, zona, estado objetivo ni campos de cierre. Primer commit devuelve201 y replay200, ambos con el recibo original y `Location: /api/v1/work-session-changes/{changeId}`. Una nueva key para un estado ya pausado/reanudado no se considera replay ni éxito silencioso.

El recibo inmutable `WorkSessionChange` tiene exactamente seis campos: `id` (UUID), `sessionId`, `action` (`PAUSE` o `RESUME`), `occurredAt`, `before` y `after` (proyecciones WorkSessionState). Identidad y SessionStart coinciden en ambas; revisión aumenta uno, after.changedAt es occurredAt y la transición/aritmética corresponde a action. No contiene serverNow ni un neto que cambie al recuperarlo. `GET /api/v1/work-session-changes/{id}` y `/by-request/{key}` recuperan este recibo por propietario con200, sin Location ni escrituras. No se añade lista global de cambios.

La key es única por propietario en un espacio de cambios de sesión distinto de los inicios14 y cambios13. Intención exacta: sessionId, acción de la ruta y revisión esperada. Misma key/intención devuelve el mismo recibo aunque haya transiciones posteriores; otra intención devuelve409 `IDEMPOTENCY_CONFLICT`. Reinicio del backend o eliminación del outbox publicado no afecta la recuperación.

Se heredan seguridad, origen/CSRF, no-store, negociación de contenido y errores cerrados de14. Tras filtros: query prohibida; UUID de ruta; key; parseo sintáctico de Work-Session-Revision; sintaxis JSON; objeto vacío; propiedad/existencia; identidad del token Work-Session-Revision; replay; revisión actual; estado permitido; reloj; escrituras. Query, UUID, JSON duplicado/concatenado, raíz incorrecta y campos extra usan exactamente las convenciones14. Work-Session-Revision ausente devuelve428 `PRECONDITION_REQUIRED`; sintaxis incorrecta400 `VALIDATION_ERROR` con campo Work-Session-Revision/INVALID_VALUE. Tras comprobar propiedad, un id del token distinto del id de ruta devuelve412 `PRECONDITION_FAILED` antes del replay: no se normaliza el token conservando sólo su revisión. Con identidad correcta, el replay precede a comparar la revisión actual; sin replay, revisión no actual devuelve412. Es sintaxis válida una revisión decimal positiva dentro de BIGINT. El token completo es `work-session-{uuid-canónica}-{revision}`, sin comillas; no se aceptan valores repetidos, listas, espacios, basura final, cero, signo, ceros iniciales ni overflow.

Los tres GET nuevos (state, cambio por id y cambio por key) prohíben toda query, incluidas repeticiones, con la misma validación de14: seguridad, query y después UUID. Todos son read-only, sin locks de escritura, materialización ni eventos. Las consultas de recibos recuperan un hecho inmutable por propietario y no capturan reloj; la consulta de estado sigue el orden snapshot/reloj definido arriba.

ID/key de sesión o recibo ajeno e inexistente son indistinguibles:404 `WORK_SESSION_NOT_FOUND` para sesión y404 `WORK_SESSION_CHANGE_NOT_FOUND` para recibo. A revisión coincidente, una transición incompatible devuelve409 `WORK_SESSION_STATE_CONFLICT`. Revisión máxima devuelve409 `WORK_SESSION_REVISION_EXHAUSTED`, antes del reloj. Son problemas de cuatro campos comunes, sin proyección ajena ni recibos en errores. Sus títulos son, respectivamente, «No se ha encontrado la sesión de trabajo.», «No se ha encontrado el cambio de sesión.», «El estado de la sesión no permite esta acción.» y «La sesión no admite más revisiones.». El error temporal15 usa «No se puede registrar la transición en ese instante.», sin cambiar el título de14. Almacén/commit fallido usa503 `STORAGE_UNAVAILABLE`.

### Atomicidad y publicación

El comando READ_COMMITTED bloquea únicamente la sesión propia `FOR UPDATE`; no modifica ni necesita bloquear proyecto, tarea o preferencia. Comprueba replay después del lock y antes de validar revisión/estado. La unicidad `(owner,key)` resuelve comandos simultáneos sobre sesiones distintas: después de una colisión esperada se termina la transacción fallida y se compara con el recibo ganador; no se reutiliza una transacción abortada. Propietarios distintos no comparten mutex global.

Con la misma key e intención hay201/200 y un único cambio/evento. Con keys distintas y la misma revisión, una transición confirma y la otra devuelve412; no se convierten dos clics concurrentes en pausar y reanudar automáticamente. Estado/intervalos, revisión, recibo y outbox se confirman juntos, comprobando una fila por escritura; fallo o supresión de escritura sin ganador durable revierte todo. Una pausa no libera la plaza única: la restricción por propietario debe abarcar running y paused, conservando todos los inicios14 y sus keys. No se exige reconstruir el agregado desde eventos ni introducir un servicio de timers.

Cada transición emite `WorkSessionStateChanged.v1`, esquema cerrado de doce campos: eventId, aggregateId, ownerId, occurredAt, schemaVersion, type, action, revision, fromStatus, toStatus, workedMicroseconds y runningSince. aggregateId es sessionId; schemaVersion1, type literal; los seis últimos reflejan el cambio confirmado, revisión y acumulado con los mismos tipos de cadena y nulabilidad del estado. OcurredAt coincide con el recibo y after.changedAt; UUID del evento independiente del cambio. Ruta `work-session.state-changed.v1`, cola quorum durable `organization.work-session-state-changed.v1`. Se conserva el publicador existente, nueve rutas previas, confirms, reintentos y blocked; entrega al menos una vez, sin promesa de exactly-once. Broker no participa en el commit HTTP.

### Interfaz y recuperación

La sección existente muestra «En curso» o «En pausa» sólo tras una consulta de estado válida; conserva inicio y fin previsto con fecha/zona y explica que pausar no mueve ese fin. Ofrece exclusivamente «Pausar» o «Reanudar» según estado, y actualización manual. Presenta «Tiempo de trabajo hasta la actualización» y hora de actualización; no requiere segundero, polling, aviso ni cuenta atrás. Actualizar tras una transición confirmada es lectura de estado, nunca otra acción de negocio. Si esa lectura falla, el recibo sigue confirmado y se avisa que el estado actual no se pudo actualizar.

Durante envío se conserva intención/key/revisión, se bloquean nuevas decisiones y se anuncia espera. Red,503, respuesta incompatible o error desconocido dejan resultado incierto: «Comprobar cambio» consulta el recibo por key. Su ausencia404 no demuestra rollback de un POST en vuelo; permite consultar otra vez o reenviar manualmente la misma intención. Un recibo recuperado acredita esa transición histórica aunque el estado actual sea posterior. Un412 permite consultar el estado actual y decidir de nuevo con nueva key/revisión; no reintenta automáticamente. Cerrar la sección o salir no revoca una petición enviada, y se explica; tras recarga sin key se puede consultar estado, sin prometer identificar el comando perdido.

Se preservan renovación CSRF manual, privacidad al salir/cambiar sesión, guardas de generación después de cada await y supresión de401 obsoletos de14. Foco estable en espera/error; si desaparece el control accionado y queda BODY, foco al encabezado, sin robar otro control elegido. Anuncios diferencian cambio confirmado de estado actual. Se aplica la matriz UX30 con evidencia real de áreas44px, teclado, error/espera, responsive, texto ampliado y motores; sin atribuir estudios humanos ni dispositivos no medidos.

### Decisiones adoptadas para revisión

Se conserva el recibo14 y se añade lectura de estado, frente a ampliar sus siete campos: evita convertir recuperación histórica en estado mutable. Paused conserva plaza, frente a liberar capacidad: impide sesiones paralelas ambiguas para16. Se usan key y revisión, frente a repetir acciones según estado actual: permite distinguir reenvío de una intención nueva tras otra pestaña. Se guarda suma exacta e intervalos, frente a restar pausas de un reloj de pantalla: conserva evidencia durable sin redondeos ni dependencia del navegador. Se mantiene fin previsto fijo, frente a desplazarlo al reanudar: respeta la decisión14 y reserva la ampliación deliberada a17. No se inicia Gherkin, implementación ni cambio de estado de15 antes de la revisión de esta propuesta.

La revisión15 usa cabecera propia Work-Session-Revision, frente a ETag/If-Match: serverNow y netMicroseconds pueden cambiar sin revisión de negocio, por lo que un ETag fuerte fijo no identifica la representación completa ([RFC9110, sección8.8.1](https://www.rfc-editor.org/rfc/rfc9110.html#section-8.8.1)). La cabecera representa exclusivamente la revisión de la sesión y conserva la precedencia y recuperación definidas, sin modificar14.
## Feature 16: cerrar una sesión de trabajo

Normativa promovida desde la propuesta final77D864520E4A9CD48EBAE10D5F9E49F2319013D1A6EE564E1026535B129CA5C6, aprobada en progress/review_close_work_proposal.md dentro de la autorización global del usuario. La aprobación documental no acredita implementación, pruebas ni cierre de16.
### Resultado y alcance

Cerrar deliberadamente una sesión propia running o paused registra su fin y tiempo neto definitivo, conserva un avance declarado y un siguiente paso opcionales, y libera la única plaza del propietario. No completa ni reabre la tarea/proyecto, no modifica bloques, disponibilidad, estimaciones, Today ni fin previsto. No calcula porcentajes, logros o progreso desde minutos; el trabajo puede contar aunque la tarea necesite otra sesión.

El cierre es terminal e inmutable. No hay reapertura, edición de notas después de confirmar, borrado, corrección retrospectiva ni cierre automático por fin previsto, medianoche, logout o inactividad. Las correcciones necesitan otra propuesta; no se añaden para facilitar fixtures. Completar proyecto/tarea antes del cierre no lo impide.

### Estado, tiempo y atribución

Se conserva WorkSessionState con sus seis campos de15. Se añade status=closed; revision aumenta una vez, changedAt es el instante de cierre y runningSince es null. SessionStart conserva exactamente sus siete campos, incluyendo inicio, zona y fin previsto originales. En running se inserta sólo el último intervalo [runningSince,closedAt) y se suma su duración exacta a workedMicroseconds; en paused se conserva el acumulado y no se inserta un intervalo por el descanso. Cierre en el mismo microsegundo es válido, incluido neto final cero.

Clock del servidor se captura una sola vez después de propiedad, replay, revisión y estado, truncado a microsegundos. Se mantiene la aritmética entera de15, capaz de cubrir años UTC0001–9999 sin Duration.toNanos ni Number. Revisión máxima se rechaza antes del reloj. Un reloj anterior a changedAt o no representable produce409 WORK_SESSION_TIME_OUT_OF_RANGE con el título temporal15, sin escrituras. No se limita el tiempo trabajado a1440 minutos ni se redondea cada intervalo.

Para la atribución histórica del día, workDate es la fecha local **del cierre**, calculada una sola vez desde closedAt y la zona histórica del inicio, y closeZoneId es la zona efectivamente usada. No se consulta la preferencia ni se reasigna el día al cambiarla. Si la zona histórica ya no es resoluble por las reglas de zona instaladas, se usa UTC explícito para el cierre y se persiste esa decisión; SessionStart.zoneId no cambia. La fecha local debe pertenecer también a0001–9999: si la conversión con una zona válida sale de ese rango, se rechaza con el mismo error temporal, sin fallback que oculte el desbordamiento.

workDate permite atribuir este cierre a un día y recuperarlo estable; no afirma que todos los intervalos ocurrieran en ese día ni reparte minutos entre días. El reparto y las comparaciones por día/semana se concretarán en18/19. El día atribuido queda fijado al cerrar; no se recalcula desde preferencias actuales ni desde el navegador.

GET state mantiene el envelope de tres campos de15. Para closed, netMicroseconds es siempre workedMicroseconds aunque serverNow avance o retroceda; serverNow sigue validándose según15. La lectura continúa read-only REPEATABLE_READ, sin escrituras ni materialización. Work-Session-Revision identifica la revisión de negocio, no la representación dependiente de serverNow; no se introduce ETag.

### Intención, API y recibos

POST /api/v1/work-sessions/{id}/close recibe un objeto JSON cerrado con progressNote y nextStep. Ambos son opcionales: ausencia o null se normalizan a cadena vacía; si están presentes deben ser strings de hasta2000 puntos de código Unicode, contando cada par UTF-16 válido (incluidos emoji) como un punto. Se rechazan U+0000 decodificado y surrogates UTF-16 aislados con400 VALIDATION_ERROR y errors del campo correspondiente con code INVALID_VALUE, antes de propiedad y replay. Los pares válidos, saltos de línea y espacios se conservan; no se inventa texto para representar un avance. La sintaxis JSON se valida antes de esos datos: un documento JSON sintácticamente inválido sigue produciendo MALFORMED_JSON. Esta precisión corresponde sólo a las notas16 y no cambia retrospectivamente14/15. La UI muestra «Sin avance anotado» y «Sin siguiente paso anotado» para valores vacíos, sin afirmar que no hubo trabajo. No hay campo completed, porcentaje, hora, duración, fecha, zona ni owner aportado por el cliente.

Se reutilizan Idempotency-Key UUID y Work-Session-Revision completos de15. Intención normalizada = sessionId + action CLOSE + expectedRevision + progressNote + nextStep. Se conserva la unicidad owner/key del espacio de cambios15, distinto de inicios14 y cambios de bloques13. Una key no puede cambiar de acción, sesión, revisión ni notas. La equivalencia ausencia/null/cadena vacía queda resuelta antes de consultar replay; no se recortan contenidos de manera oculta.

La primera confirmación es201 después del commit; replay idéntico es200, con el mismo recibo y Location del cambio. Se reutilizan GET /api/v1/work-session-changes/{id} y /by-request/{key}, con propiedad y read-only de15, sin Location en GET. Se añade únicamente GET /api/v1/work-sessions/{id}/closure para recuperar el cierre por la identidad de sesión, sin conocer changeId o key; no hay lista global.

El recibo es una unión discriminada por action. PAUSE y RESUME conservan exactamente sus seis campos anteriores y sus valores inmutables. CLOSE tiene esos seis campos (id,sessionId,action,occurredAt,before,after) más **closure**, objeto cerrado de cuatro campos: progressNote,nextStep,workDate,closeZoneId. Así se registra texto y atribución sin añadir campos al estado que varía ni al inicio14. Las notas son strings normalizados; workDate es YYYY-MM-DD; closeZoneId es la zona persistida. before admite running o paused; after es closed con revisión siguiente, occurredAt=after.changedAt y suma exacta. Identidades y SessionStart coinciden en before/after. No se duplica un segundo recibo de cierre en otra tabla ni se extiende retroactivamente un recibo15.

La lectura por sesión devuelve el mismo recibo CLOSE completo, incluidas notas y atribución, nunca una reconstrucción desde el estado. Consulta sesión propia y su único recibo CLOSE en un snapshot read-only REPEATABLE_READ, sin reloj ni writes. Sesión ajena/ausente produce404 WORK_SESSION_NOT_FOUND; sesión propia todavía abierta produce404 WORK_SESSION_CHANGE_NOT_FOUND. Se heredan query prohibida antes de UUID, seguridad antes de ambas, no-store y503 incluso al finalizar. La terminalidad y el lock de sesión aseguran un solo cierre; se reutiliza work_session_changes consultando owner/session/action, sin copiar recibos ni guardar otro identificador en SessionStart. Esta ruta resuelve recuperación después de recarga cuando active=null, sin historial18.
El cliente actualizado discrimina CLOSE y valida ese shape, las notas y las relaciones del estado/tiempo con enteros exactos. Valida workDate como fecha persistida válida, pero no la recalcula ni rechaza por discrepancias del catálogo Intl/TZDB del cliente. Un fallback visual a UTC se etiqueta y no altera workDate ni closeZoneId persistidos. Estado closed por sí solo acredita que la sesión está cerrada; no demuestra que una intención incierta concreta, su key y sus notas fueran confirmadas.

### Precedencia, concurrencia y persistencia

Se heredan filtros de seguridad, origen/CSRF, negociación, no-store, UUIDs, JSON estricto y prohibición de query de15. En close: query, UUID de ruta, key, sintaxis de Work-Session-Revision, JSON/forma/notas; propiedad/existencia; identidad del token; replay e intención; revisión vigente; estado compatible; agotamiento de revisión; reloj y atribución; escrituras. Mantener428 de cabecera ausente,400 de sintaxis/campos,404 indistinguible para ajeno/ausente,412 de identidad o revisión,409 IDEMPOTENCY_CONFLICT/WORK_SESSION_STATE_CONFLICT/WORK_SESSION_REVISION_EXHAUSTED y503 de almacenamiento. Reutilizar títulos y cuerpos cerrados de15; no cambiar los de14.

Cerrar closed con key nueva y revisión vigente devuelve409 WORK_SESSION_STATE_CONFLICT; revisión obsoleta devuelve412 primero. PAUSE/RESUME nuevos sobre closed tienen la misma regla. Los replays idénticos de PAUSE, RESUME o CLOSE preceden al estado actual: siguen devolviendo su hecho original después del cierre e incluso después de iniciar otra sesión propia.

El comando usa READ_COMMITTED y el mismo lock de la fila propia FOR UPDATE de15. No bloquea proyecto, tarea, disponibilidad ni un mutex global. Dos cierres con misma key/intención producen201/200 y un hecho; keys distintas con la misma revisión producen201/412. Cierre y pausa/reanudación con igual revisión se ordenan por ese lock: una transición confirma y la otra obtiene412; no se aplica automáticamente sobre una revisión nueva.

La restricción parcial existente abarca running y paused y excluye closed. El cierre libera la plaza sólo al commit. Inicio14 concurrente con cierre o rollback respeta la restricción: puede rechazar porque la sesión aún está abierta o iniciar cuando el cierre ha confirmado; nunca confirma dos abiertas. Tras rollback de cierre la sesión sigue ocupando plaza. No se exige un resultado preferente a un scheduler concreto.

Ahora es alcanzable comprobar key owner-global entre sesiones distintas sin fixtures imposibles: cerrar A, iniciar B y usar en B una key de cambio consumida por A debe409, sin devolver el recibo de A como éxito de B. Concurrencia entre un comando sobre la sesión cerrada A y otro sobre B conserva propiedad, replay y comparación de intención. No confundir esto con dos sesiones abiertas del mismo propietario.

Migración aditiva posterior aV16, sin modificar migraciones publicadas ni reescribir inicios/recibos anteriores. Reutilizar la fila de estado, intervalos cerrados, work_session_changes y outbox: closure se conserva dentro del recibo durable que ya guarda before/after, con los datos mínimos de intención necesarios para compararlo. La aplicación obtiene las notas normalizadas y el adaptador inserta el mismo hecho; no event sourcing, nuevo agregado de notas ni servicio de timers.

Estado/revisión, último intervalo cuando procede, recibo y outbox confirman juntos y comprueban una fila por escritura. Fallo de SQL, supresión sin ganador o fallo al commit revierte todo y no libera plaza. La colisión esperada owner/key se resuelve después de rollback en una transacción nueva, con intención completa; otras restricciones no se convierten en replay. GET de estado, inicio y recibo siguen sin escribir y sus fallos al terminar no significan ausencia.

### Evento y compatibilidad

Nuevo WorkSessionClosed.v1, sin ampliar el schema de WorkSessionStateChanged.v1. El evento tiene exactamente once campos: eventId,aggregateId,ownerId,occurredAt,schemaVersion,type,revision,fromStatus,workedMicroseconds,workDate,closeZoneId. aggregateId=sessionId, type literal, schemaVersion1, occurredAt coincide con el cierre, revision y trabajado son cadenas decimales canónicas como15, fromStatus es running o paused. Las notas quedan en el recibo consultable; no se copian al broker sin un requisito de consumo. El tipo ya expresa el estado terminal, por lo que no necesita toStatus ni runningSince redundantes.

Ruta work-session.closed.v1 y cola quorum durable organization.work-session-closed.v1, junto a las rutas existentes. Reutilizar publicador, confirms, reintentos y blocked. Broker caído no impide cierre local; entrega al menos una vez, no exactly-once. La recuperación por id/key sobrevive al reinicio y a publicar/retirar outbox; demostrar ambas cosas en smoke real, sin atribuir publicación a un DELETE de fixture.

GET active14 sigue devolviendo exactamente session/null con SessionStart7: excluye closed y puede devolver una sesión nueva. Los GET de inicio14 y sus replays conservan siempre el inicio original. Los recibos15 conservan seis campos y los endpoints15 aceptan el nuevo estado terminal de forma explícita. No modificar DTO de bloques ni Today. La actualización del cliente incluye los lectores14/15 que validaban sólo running/paused, conservando la recuperación histórica y rechazando estados desconocidos.

### Recorrido y recuperación UX

Desde un snapshot confirmado running/paused, «Cerrar sesión de trabajo» abre un formulario inline con contexto, inicio y fin previsto visibles, y campos etiquetados «Avance anotado (opcional)» y «Siguiente paso (opcional)». «Confirmar cierre» es la única escritura; «Volver» cierra el formulario sin alterar la sesión. No modal adicional, porcentaje, checkbox de tarea completada ni obligación de justificar una pausa o un neto cero. El borrador explica que cerrar no completa la tarea y que las notas confirmadas no se editan en este corte.

Mientras se envía se retienen key, revisión y cuerpo normalizado; se anuncia espera y se impiden decisiones duplicadas. Red,503, respuesta incompatible, error desconocido o IDEMPOTENCY_CONFLICT conservan resultado incierto. «Comprobar cierre» usa el recibo por key. Un404 reconocido permite volver a consultar o reenviar manualmente la misma intención; no demuestra rollback de un POST en vuelo. Un412 conserva los textos del borrador para corregir o decidir después. Se consulta el estado y sólo si un GET válido confirma que sigue abierta se permite una nueva confirmación manual con key y revisión nuevas; no se reenvía ni se actualiza silenciosamente la intención. CSRF se renueva con el flujo global existente y el reenvío posterior es separado.

La confirmación muestra «Sesión cerrada», fecha/hora/zona del cierre, día atribuido explícito y «Tiempo de trabajo total», además de las notas declaradas. Se conserva el hecho si falla actualizar active o state. Tras confirmar se consulta active con una generación nueva: null confirmado permite volver al inicio14; otra activa puede ser una sesión nueva propia y se muestra/enlaza aparte, sin sustituir el recibo ni cerrarla.

Para recuperar el cierre después de recargar, se usa la ruta UI estable /proyectos/{projectId}/tareas/{taskId}/sesiones/{sessionId}. Al abrir el formulario de cierre se establece esa URL con sessionId antes de cualquier POST, no sólo después de un éxito; así una recarga tras perder la respuesta conserva la identidad. La ruta no depende de que active siga devolviéndola. Consulta state por sessionId y, si closed, GET /work-sessions/{id}/closure para obtener el recibo con notas, incluso sin conocer changeId o key. Verifica coincidencia de proyecto/tarea/sesión antes de presentar el recurso. Sólo los IDs de recursos propios figuran en la URL; nunca key, notas ni recibos privados en almacenamiento web. Si se perdió la respuesta y la key, ese GET recupera el cierre real con sus notas, pero no prueba que perteneciera a aquella intención incierta del navegador. No añadir descubrimiento «último cierre» ni historial global para resolverlo.

Mantener guardas tras cada await, abort antes de propagar401 obsoleto, retirada de datos/borradores privados al perder sesión o cambiar contexto, y descarte de GET antiguo después de cierre o nueva activa. Conservar foco en el control usado durante carga; si desaparece, llevarlo al encabezado correspondiente sólo cuando el foco seguía dentro. No robarlo a otro control. Aplicar las30 filas UX, teclado,44×44, responsive, zoom/motores y feedback temprano, con evidencia real y límites de dispositivos explícitos en su fase; esta sección no acredita pruebas ejecutadas.

### Frontera con las siguientes features

17 conserva aviso de fin y ampliación deliberada, sin alarmas ni cambios de duración aquí. 18 conserva listado global, filtros/orden y acceso histórico por colección, sin impedir ahora recuperar recursos conocidos por id/key. 19 conserva agregación real frente a plan y análisis semanal. Ningún texto opcional se presenta como avance verificado ni el día atribuido como desglose de todos los intervalos.

## 17. Aviso de fin y ampliación deliberada

### Alcance y fin efectivo

Una sesión propia abierta puede mostrar un aviso al alcanzar su fin acordado y ampliarse mediante decisión explícita. Alcanzar ese instante no pausa, reanuda, cierra ni completa nada. Se conservan SessionStart7, plannedMinutes y plannedEndAt originales de14. La proyección añade un fin efectivo durable `effectiveEndAt`, inicialmente plannedEndAt, que sólo cambia por ampliación confirmada. No cambia reservas, disponibilidad, presupuesto, Today, estimaciones, tarea o proyecto. Gestionar la sesión sigue permitido aunque su contexto se haya completado después del inicio. Sin alarmas externas, sonido, permisos de notificación, conectores, worker de timers ni historial18.

Se amplía por `additionalMinutes`, entero entre1 y1440. El nuevo fin es exactamente `max(previousEndAt, occurredAt) + additionalMinutes minutos`, con duración real y sin conversión local/DST. Una decisión tardía obtiene el tiempo elegido a partir de su confirmación; una anterior al vencimiento también es válida. No se limita la ampliación acumulada a1440. Todos los instantes expuestos o persistidos de17 son UTC, precisión máxima de microsegundos y años0001–9999. Desbordamiento del nuevo fin produce409 WORK_SESSION_TIME_OUT_OF_RANGE con el título temporal15 y sin escrituras; no se recorta ni cambia a otra zona.

### Estado, revisión y última decisión temporal

EXTEND comparte la revisión de sesión con PAUSE/RESUME/CLOSE. En su before/after State6 sólo cambia revision, que aumenta exactamente uno; session, status, changedAt, workedMicroseconds y runningSince permanecen iguales. La sesión debe estar running o paused. Ampliar no termina ni abre intervalos, no suma trabajo ni reanuda una pausa. Se conserva sin relajación la invariante runningSince===changedAt cuando running, y runningSince null en paused/closed.

El instante de la ampliación pertenece a su recibo, no a changedAt. Exclusivamente para EXTEND no se exige after.changedAt=occurredAt; esa igualdad sigue vigente para PAUSE/RESUME/CLOSE. El cliente discrimina cada acción y sus invariantes, sin permitir campos o estados desconocidos ni aceptar como ampliación un recibo de otra intención.

Cada sesión mantiene internamente el instante de su última decisión confirmada, incluidas las ampliaciones. Para datos anteriores a17 su valor inicial es changedAt (o startedAt por el fallback14 existente). EXTEND y nuevos PAUSE/RESUME/CLOSE capturan el reloj una sola vez tras bloquear, comprobar replay y validar revisión/estado/máximo; truncan a microsegundos y exigen que no preceda al máximo de changedAt y esa última decisión. Actualizan la marca en la misma transacción. El mismo microsegundo es válido. Reloj fuera de años0001–9999 o anterior devuelve409 temporal15, sin modificar estado, fin, marca, intervalos, recibos ni outbox. Un replay no consulta ni actualiza reloj/marca. Esta marca no añade campos a State6 ni reescribe recibos históricos; puede persistirse junto a la proyección, sin reconstruir toda la sesión desde eventos.

### API, recibos y precedencia

`GET /api/v1/work-sessions/{id}/end-time` devuelve200 con exactamente `state`, `serverNow` y `effectiveEndAt`. State es State6 y puede ser closed; el fin se conserva después del cierre. En una única transacción read-only REPEATABLE_READ se leen primero propiedad, estado, fin y última decisión coherentes; después se captura una vez el reloj UTC truncado a microsegundos. serverNow debe estar en años0001–9999 y no preceder al máximo de changedAt y última decisión confirmada; en caso contrario409 temporal15. La ausencia de sesión se resuelve antes del reloj. No se materializan valores mediante GET. La respuesta entrega Work-Session-Revision canónica, sin comillas, acorde con session.id/revision; sin ETag ni Location. No admite queries. El snapshot3 de GETstate15 y sus reglas de neto/reloj permanecen sin cambios.

`POST /api/v1/work-sessions/{id}/extend` recibe objeto cerrado `{additionalMinutes}` y exige Idempotency-Key y Work-Session-Revision completos. Hereda de15/16 seguridad, origen, negociación, no-store, JSON estricto, query, UUID/key y parseo de cabeceras anterior al cuerpo. Ausencia/null de additionalMinutes:400 VALIDATION_ERROR, campo additionalMinutes/REQUIRED; tipo distinto de número:INVALID_TYPE; número no entero o fuera1–1440:OUT_OF_RANGE. Ningún reloj/fin/zona/estado enviado por el cliente se acepta como campo adicional.

Tras validar forma, se comprueba propiedad, identidad del token, replay/intención, revisión vigente, compatibilidad running/paused, máximo BIGINT y reloj, en ese orden. Se conservan404 WORK_SESSION_NOT_FOUND,428 PRECONDITION_REQUIRED,400 de token malformado,412 PRECONDITION_FAILED,409 WORK_SESSION_STATE_CONFLICT o WORK_SESSION_REVISION_EXHAUSTED y503 STORAGE_UNAVAILABLE de15. Sobre closed, key nueva/revisión vigente produce conflicto de estado; revisión obsoleta produce412. No se reabre ni libera de nuevo la plaza.

Primer commit201 y replay200, ambos con Location del cambio propio. EXTEND usa el namespace owner/key de cambios15/16; intención exacta es sessionId, action EXTEND, revisión esperada y additionalMinutes. Misma key/intención devuelve el recibo original después de cualquier cambio posterior, incluso cierre e inicio de otra sesión. Otra sesión, acción, revisión o cantidad devuelve409 IDEMPOTENCY_CONFLICT. El namespace de inicio14 sigue separado.

Recibo discriminado EXTEND7: exactamente `id`, `sessionId`, `action`, `occurredAt`, `before`, `after`, `extension`. El último contiene exactamente `additionalMinutes`, `previousEndAt`, `effectiveEndAt`; cantidad numérica entera y fechas con las reglas anteriores. Before/after son State6 con identidad e inicio originales, misma condición running/paused y diferencia exclusiva de revisión. occurredAt no precede a before.changedAt; el nuevo fin cumple la fórmula exacta con ese occurredAt y previousEndAt. GET de cambios C/K existentes recuperan EXTEND con200, sin Location, reloj ni escrituras. P/R siguen recibo6, CLOSE recibo7 con closure y el inicio14 DTO7; ningún campo extension/closure null se añade a otros tipos. La migración aditiva conserva filas/keys/JSONB anteriores y la recuperación no depende de retención del outbox ni TZDB.

### Concurrencia y publicación

Se bloquea sólo la sesión propia para actualizar. Fin efectivo, revisión, marca temporal, recibo y outbox se confirman juntos con comprobación de escrituras; cualquier fallo/supresión sin ganador durable o fallo al commit revierte el conjunto. No se bloquea tarea/proyecto/preferencia ni se introduce mutex entre propietarios. Se conserva la única plaza running/paused y cierre terminal16. Misma key/intención concurrente obtiene201/200 y un único evento; decisiones distintas sobre una revisión, incluidas EXTEND frente a PAUSE/RESUME/CLOSE, producen un201 y un412 sin preferencia de ganador. Una colisión owner/key se resuelve tras rollback en una lectura nueva, comparando la intención completa; otra intención409, ausencia de ganador503, sin clasificar otras restricciones como replay.

Nuevo WorkSessionExtended.v1, exactamente once campos: eventId,aggregateId,ownerId,occurredAt,schemaVersion,type,revision,additionalMinutes,previousEndAt,effectiveEndAt,status. aggregateId=sessionId, eventId independiente, schemaVersion1, type literal; revision cadena decimal canónica positiva hasta BIGINT, additionalMinutes entero1–1440, status running/paused. Fechas y fórmula coinciden con el recibo. No incluye notas ni key. Ruta work-session.extended.v1, cola quorum durable organization.work-session-extended.v1. Reutiliza el publicador y todas sus rutas anteriores, payload original, confirms/reintentos/blocked y entrega al menos una vez; el broker no participa en el commit HTTP. Mostrar un aviso no escribe ni emite evento.

### Aviso, recuperación y privacidad

El aviso, ambos fines y el acceso a ampliación se ofrecen en las dos superficies existentes: junto al estado de la sesión activa en el detalle de tarea, y en la URL conocida de la sesión16. GETactive es global del propietario: si devuelve una sesión de otra tarea, end-time y las decisiones se validan contra la identidad de esa sesión, no contra la tarea que aloja el panel. En la URL16 sí se exige coincidencia estricta proyecto/tarea/sesión. «Cerrar sesión de trabajo» conserva la navegación a esa URL antes del POST; no se exige abandonar el detalle para recibir el aviso. Cierre y ampliación tienen confirmaciones diferenciadas, sin formularios o botones submit de una acción anidados en la otra.

La proyección es sólo aditiva: GETend-time exige effectiveEndAt>=state.session.plannedEndAt; EXTEND exige previousEndAt>=before.session.plannedEndAt además de la fórmula exacta del nuevo fin. El cliente verifica esas relaciones y las invariantes completas expuestas con enteros de microsegundos, sin redondear a Number. No inventa una validación de la marca interna, ausente del DTO: su relación adicional con serverNow/occurredAt corresponde al servidor. Paused sigue siendo válido; no se exige running para aceptar la proyección o el recibo.

La interfaz muestra «Fin previsto original» y, si difiere, «Fin acordado actual», con fecha/zona explícitas. La zona histórica sólo sirve para presentar; si no se puede representar localmente se usa UTC etiquetado sin alterar los instantes. El aviso «Ha llegado el fin acordado» se basa en un GETend-time válido de sesión abierta con serverNow>=effectiveEndAt. Ofrece «Cerrar sesión de trabajo», conservando el recorrido16, y «Ampliar tiempo». Este último pide cantidad sin valor preaceptado y «Confirmar ampliación». Paused recibe el aviso por reloj, sin reanudarse; closed retira aviso y controles conservando su recibo histórico. No se presenta el tiempo añadido como trabajo acreditado.

Desde un snapshot válido todavía no vencido, el navegador programa una comprobación usando serverNow y tiempo monotónico transcurrido, sin Date.now como autoridad. El plazo puede superar2^31−1ms: se fragmenta en esperas positivas de como máximo ese límite, sin convertir el plazo completo en un delay nativo que desborde. Un fragmento que termina antes del vencimiento sólo rearma el restante monotónico, sin GET repetido; un resto positivo inferior a1ms espera al menos1ms y nunca genera un bucle de delay0. Sólo el delay acotado se convierte a milisegundos; los instantes y fórmulas siguen siendo exactos en microsegundos. Al vencer consulta de nuevo antes de afirmar que el fin sigue vigente; mientras tanto anuncia comprobación, y ante fallo ofrece reintento sin inferir ausencia/cierre. Un callback anticipado se rearma sin bucle inmediato. Al volver a visible después de ocultación/suspensión se consulta de nuevo; se coalesce una lectura ya pendiente y se rearma el fin vigente cuando termina. Al cerrar/navegar/perder acceso se retiran timers y generaciones anteriores. No se promete puntualidad con navegador cerrado, suspensión, limitación de timers o red pendiente; no hay polling continuo, segundero obligatorio ni alarma externa.

Un aviso confirmado no desaparece por retroceso del reloj local o por un fallo de consulta; sólo por comprobación de ampliación/cierre u otro contexto. Se diferencia el último hecho confirmado de una actualización pendiente/fallida. Otra pestaña se conoce al actualizar o volver a visible, sin sincronización instantánea; revisión y propiedad protegen siempre las acciones. El aviso se anuncia una vez por fin confirmado en el contexto montado, sin mover foco ni abrir modal automáticamente.

Durante envío se conservan cantidad/key/token y se impiden decisiones duplicadas. Red/503/respuesta incompatible/error desconocido/IDEMPOTENCY_CONFLICT mantienen resultado incierto y «Comprobar ampliación» consulta K. Sólo404 de recibo reconocido o CSRF reconocido permiten reenviar manualmente la misma intención;404 no prueba rollback de un POST en vuelo. Un412 conserva cantidad como borrador, exige GET válido abierto y nueva confirmación con key/revisión nuevas. Un recibo recuperado confirma aquella ampliación histórica; se consulta por separado el fin/estado actual y no se sustituye el hecho si esa lectura falla.

En cada superficie se coordinan las decisiones hermanas de la misma sesión: mientras PAUSE/RESUME/CLOSE/EXTEND está enviándose, comprobándose o incierta, no se permite otra decisión local de negocio desde otro componente. Consultar/recuperar sigue disponible según el flujo correspondiente; navegar también, con aviso de que salir no revoca un POST transmitido. Entre pestañas decide la revisión del servidor. Tras confirmar una decisión se invalidan las lecturas anteriores y se actualizan los snapshots dependientes de estado/neto/fin; una respuesta iniciada antes no restaura una revisión anterior ni el fin previo como actuales. Los recibos históricos se conservan separados. Una ampliación actual comprobada rearma el nuevo fin y permite su propio aviso, no uno por render; un cierre comprobado retira controles y timers de esa sesión aunque active descubra otra nueva.

La URL conocida de sesión16 permite recargar y consultar end-time aunque GETactive ya devuelva otra sesión. Sin key retenida se muestra el fin durable, pero no se identifica como propia una intención perdida ni se reenvía automáticamente. No hay almacenamiento web de keys/borradores/recibos ni descubrimiento de cambios mediante historial18. Se verifica coincidencia de proyecto/tarea/sesión en la ruta;401/404 de propiedad retiran datos y borradores. Guardas tras cada await y aborto antes de propagar401 impiden que una lectura obsoleta retire o restaure otro contexto.403 no se convierte en éxito ni habilita reenvío salvo CSRF reconocido.

Conservar foco en el control iniciador si permanece; si desaparece, mover al encabezado sólo cuando el foco seguía dentro, sin robar otro control elegido. Feedback de espera antes de400ms, controles44×44, teclado y matriz30 UX/responsive heredados, con evidencia real de texto/zoom/motores y límites de dispositivos explícitos. Esta sección no acredita pruebas realizadas ni inicia implementación antes de destilar y revisar el contrato.


## 18. Historial propio — especificación normativa

### Alcance y hechos

Una vista global permite consultar qué ocurrió en los proyectos y tareas propios: planificación, replanificación/cancelación, finalización/reapertura de tareas e inicio/decisiones de sesiones. Los hechos son inmutables; mostrar una reserva no acredita trabajo y cerrar una sesión no completa una tarea. No estadísticas19, sumas semanales, exportación, conectores, edición histórica ni consumidores nuevos.

Cinco familias, con rango interno fijo para desempatar (no prioridad visual):

| type / sourceRank      | Fuente durable / instante            | details cerrado reutilizado                                                                                                      |
| ---------------------- | ------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------- |
| BLOCK_PLANNED /0       | planned_blocks.id /created_at        | BlockResponse de11 (nueve campos) original inmutable; nunca la proyección actual.                                                |
| BLOCK_CHANGED /1       | block_changes.id /occurred_at        | ReceiptResponse de13 de siete campos, kind CANCELLED o RESCHEDULED, before/after históricos.                                     |
| TASK_STATUS_CHANGED /2 | task_status_history.id /occurred_at  | HistoryEntryResponse de9, cuatro campos: id,fromStatus,toStatus,occurredAt; completed y reapertura pending son hechos separados. |
| SESSION_STARTED /3     | work_sessions.id /started_at         | SessionStart7 original14.                                                                                                        |
| SESSION_CHANGED /4     | work_session_changes.id /occurred_at | Recibo15–17 discriminado: PAUSE/RESUME6, CLOSE7 con closure, EXTEND7 con extension; formatos existentes sin alterar.             |

Cada fila de origen aparece una vez. El inicio y el cierre son hechos distintos; un CLOSE no se vuelve a emitir desde status=closed, ni EXTEND desde effective_end_at. No se crean hechos por consultar proyecciones, intervals ni outbox. Las reaperturas/correcciones posteriores no eliminan hechos anteriores. El catálogo no agrega finalizaciones de proyecto: no existe una fuente histórica independiente equivalente y no se reconstruyen desde updated_at/outbox ni se inventa backfill. La planificación inicial se etiqueta como reserva, no como cambio ni logro.

### API y representación

Única ruta nueva: GET /api/v1/history. Respuesta200 application/json exacta {items,nextCursor}; items de cero a20 entradas, nextCursor string opaco o null. Sin total, resumen agregado, ETag, Location ni consultas de estado por fila. Cache-Control:no-store y seguridad de sesión heredados. H no exige token CSRF: un GET autenticado sin ese token conserva la validación normal de query, sin añadir políticas de seguridad nuevas. No POST de historial.

Entrada exacta de ocho campos: id,type,occurredAt,projectId,projectName,taskId,taskTitle,details. IDs UUID canónicos; id es el de la fuente, identidad global del hecho es (type,id), porque UUID iguales entre tablas no deben mezclarse. projectName/taskTitle son strings no vacíos conforme a las validaciones vigentes de nombres/títulos y etiquetas **actuales**, obtenidas del contexto propio; nunca se presentan como nombres históricos ni intervienen en cursor/orden. Pueden cambiar entre páginas. No se devuelven owner, requestKey ni payload de outbox.

Details reutiliza íntegramente los DTO indicados; sus nombres/tamaños y validaciones cerradas vigentes se conservan, sin copiar campos nuevos a14–17. Se comparan identidad, instante y contexto exteriores sólo con los campos que existan en cada DTO. SESSION_STARTED exige id=details.id, occurredAt=details.startedAt y contexto de SessionStart. BLOCK_PLANNED exige id=details.id y occurredAt=details.createdAt; taskId y projectId se comparan donde el DTO los incluya. Los cambios comparan id/occurredAt del recibo y los identificadores presentes en before/after; en sesiones, también before.session y after.session. TaskHistory4 no contiene IDs de contexto: su propiedad y projectId/taskId exteriores proceden de joins autorizados, no de campos inventados. El servidor confirma siempre contexto mediante joins propios, incluso cuando el detalle también contiene IDs.

El cliente valida además cada entrada frente a los filtros aplicados projectId/taskId/category/from/to, el orden total estrictamente descendente y la ausencia de duplicados(type,id) dentro de la página. projectName/taskTitle deben ser strings no vacíos. Cualquier incompatibilidad de forma, tipo, identidad, aritmética, filtro u orden rechaza la página completa: no muestra un subconjunto ni la interpreta como lista vacía. No recalcula la atribución histórica del cierre al validar fechasUTC.
Instantes UTC años0001–9999 con precisión máximaµs; revisiones/tiempos acumulados grandes mantienen strings existentes. No comparar hechos con reloj actual ni recalcularlos usando TZDB. El día y zona de atribución del CLOSE permanecen exactamente closure.workDate/closeZoneId, distintos de la fecha UTC del hecho. Notas se muestran como texto, preservando espacios y saltos; no HTML. Una zona histórica no disponible se presenta en UTC etiquetado, sin cambiar el dato.

### Filtros y fechas

Parámetros únicos permitidos: category,projectId,taskId,from,to,cursor. Se rechazan desconocidos, repetidos o valores vacíos. Sin limit configurable:20 fijo, como paginación local existente. Category omitida incluye todo; valores exactos sessions (dos familias de sesión), task-status (finalizaciones y reaperturas) o planning (planificación y cambios). No filtro por estado actual que haga desaparecer hechos previos.

ProjectId opcional; taskId sin projectId devuelve400 VALIDATION_ERROR, campo taskId, código INVALID_VALUE. UUID con formato estricto heredado, normalizados al emitir cursores. Los filtros de contexto ajenos/inexistentes devuelven404 RESOURCE_NOT_FOUND indistinguible, incluso si no hay resultados; contexto propio completed sigue consultable. No catálogo de proyectos/tareas nuevo: la UI accede a estos filtros mediante enlaces contextuales de las filas y de los detalles existentes de proyecto/tarea, y permite quitarlos.

From/to son fechas Gregorianas reales YYYY-MM-DD,0001–9999, ambos límites inclusivos sobre la fecha **UTC de occurredAt**. Cualquier extremo puede omitirse; from>to devuelve400 VALIDATION_ERROR, campo to, código INVALID_VALUE. Normalización equivalente a desde00:00:00Z hasta23:59:59.999999Z, sin calcular un día10000 para el máximo9999-12-31. No se usa start_at de un bloque futuro ni workDate del cierre, ni zona navegador/preferencia. La UI etiqueta explícitamente «Fecha del hecho (UTC)»; el cierre puede mostrar además su día atribuido persistido, sin confundir filtros.

### Orden y cursor

Orden total descendente por (occurredAt,sourceRank,id). UUID se compara en orden PostgreSQL UUID (equivalente a representación canónica minúscula ordenada lexicográficamente); no ordenar por títulos, versión global ficticia ni estado mutable. Empatesµs, también entre decisiones de una misma sesión, no afirman causalidad: UUID no ordena revisiones. La UI muestra after.revision de los recibos de sesión y la revisión del cambio de bloque cuando proceda, y explica que empate de instante no significa orden causal. No añade desempate global por revisión ni secuencia nueva; las revisiones conservan su significado local.

Primera página captura un snapshot PostgreSQL REPEATABLE READ/readOnly de esa solicitud y obtiene hasta21 candidatos propios ya filtrados. Devuelve20 y emite cursor sólo si existe candidato21. Upper es la mayor clave del recorrido inicial; after es la última clave entregada. Continuación exige key<=upper y key<after con el mismo orden/filtros. Lista vacía o sin más datos devuelve nextCursor:null. No se necesita Clock para leer.

Cursor opaco usa Base64URL sin padding, codificación canónica y JSON cerrado con version:1,owner,filters,upper,after. Filters contiene exactamente category,projectId,taskId,from,to con valores normalizados o null; cada frontera contiene occurredAt,type,id y deriva sourceRank del type normado. Vincula identidad autenticada, filtros y versión; upper>=after. Syntax/campos duplicados/basura final/tipos/rangos/UUID/canonización incorrectos, propietario o filtros incompatibles:400 campo cursor INVALID_VALUE. Cursor válido no exige que su fila siga presente; no hay eliminación de hechos en este alcance. No se usa el cursor como autorización: cada rama SQL vuelve a restringir propietario. Base64 no es firma ni secreto; no se añade secret/HMAC ni persistencia de cursores.

Garantía entre páginas: keyset de hechos inmutables, **no snapshot congelado entre requests**. No se repiten hechos ya recorridos siguiendo cursores emitidos sin modificarlos; novedades por delante de after no se insertan en páginas posteriores. Un commit tardío con occurredAt anterior y clave detrás de after puede aparecer después; si queda delante, se verá al volver a recientes. Upper no impide todos los commits tardíos. Una página repetida puede cambiar por esos commits o por nombres actuales. No se añade secuencia global, tabla auxiliar ni transacción persistente para fingir estabilidad absoluta. «Volver a recientes» reinicia sin cursor y descubre el estado de lectura actual.

### Persistencia, errores y privacidad

Un caso de uso de lectura y un adaptador PostgreSQL reúnen las cinco fuentes con UNION ALL; no servicio de indexación, event sourcing ni nueva copia histórica. Owner se filtra en cada rama y joins de proyecto/tarea confirman contexto, también para sesiones con owner_id. Índices sólo si la consulta real los justifica, mediante migración aditiva; no modificar hechos ni migraciones históricas. Lectura, filtros, cursor y reintento no crean preferencias, recibos, intervalos ni eventos, ni reclaman/retiran outbox. El historial sigue disponible con broker caído o outbox publicado retirado.

Precedencia: autenticación/seguridad heredada; validación estructural de query y filtros (incluidas las dos relaciones anteriores); decodificación sintáctica del cursor; propiedad del contexto explícito; vínculo del cursor a owner/filtros y coherencia upper>=after; lectura. La sintaxis de cursor incluye Base64URL canónico, JSON cerrado/sin duplicados ni basura final, tipos, versión, fechas y UUID: su invalidez devuelve400 cursor INVALID_VALUE antes de un404 de contexto. Con cursor sintácticamente válido, contexto ajeno/inexistente devuelve404 antes de comprobar owner/filtros o relación de fronteras del cursor. Errores400 reutilizan ValidationException/ApiErrors y campos query/category/projectId/taskId/from/to/cursor; errores por campo INVALID_VALUE, UUID INVALID_FORMAT conforme helpers.401 y403 conservan contrato de sesión;503 STORAGE_UNAVAILABLE ante fallo de almacenamiento o hecho seleccionado incoherente que impida representar la página. No devolver200 parcial omitiendo filas dañadas, ni convertir fallo en ausencia. No exponer SQL/stack/notas en diagnósticos. No inspeccionar datos de otro propietario para completar una fila.

### Interfaz y navegación

Ruta /historial y un enlace en navegación Workspace existente, sin shell/router nuevos. Encabezado «Historial», filtros categoría/fechasUTC etiquetados, aplicar explícitamente y limpiar; projectId/taskId contextuales en URL. Los detalles existentes de proyecto y tarea incorporan sendos enlaces nativos «Ver historial de este proyecto» y «Ver historial de esta tarea», hacia /historial con projectId y, en tarea, también taskId. Las filas ofrecen los mismos filtros contextuales y las rutas actuales de proyecto/tarea. No hace falta encontrar un hecho previo ni escribir UUID para elegir ese contexto; no se añade catálogo ni ruta de detalle. Sesiones enlazan siempre a URL16 p/t/s; bloques enlazan a la tarea y su historial local existente, sin inventar nueva URL de bloque. Si el filtro de contexto tiene página vacía, etiqueta genérica «Este proyecto/Esta tarea» y enlace existente; no inventar un nombre histórico.

Una página por URL, «Más antiguos» y «Volver a recientes»; Back/recarga reproducen la consulta, con límites de inserciones tardías explícitos. No append infinito ni cadena de cursores almacenada. Formularios cambian filtros sin cursor al aplicar; ninguna mutación de dominio desde la lista. No reenviar acciones históricas ni afirmar que un hecho ajeno a la intención en memoria confirma un POST perdido.

Lista semántica con texto de acción, fechaUTC, etiquetas actuales de contexto y enlace. Details nativo permite expandir datos del recibo/notas sin otra solicitud, con nombre accesible. Reservas, trabajo y finalizaciones se diferencian por palabras; cierre muestra tiempo neto propio/fecha atribuida, ampliación muestra fin anterior/nuevo sin sumarlo al trabajo. No totales de página ni progreso inventado. No ocultar reaperturas.

Carga inicial, vacío sin hechos, vacío por filtros, error y reintento diferenciados; feedback anunciado<400ms. Al cambiar filtros/página se retira la lista anterior o queda explícitamente anterior/no vigente, nunca bajo nuevas etiquetas como resultado actual. Una petición obsoleta se aborta y sus respuestas/401 tardíos no reemplazan contexto ni eliminan otro login.401 actual retira datos privados;404 de contexto los retira y permite quitar filtro.503 no borra filtros ni anuncia ausencia; reintentar repite sólo GET. Foco permanece en iniciador si sigue; al sustituir página después de navegación deliberada pasa a encabezado del resultado cuando proceda, sin robar foco si la persona ya eligió otro control.

Aplican las30filas de docs/ux-requirements.md con evidencia posterior, no certificación automática: matriz320–2560 y bordes/altura400,44px, teclado, orden y foco, textos/notas largos, estados de carga/error/vacío/detalle, axe, texto200, zoom nativo200 y tres motores. Dispositivos/lectores reales y evaluación humana conservan límites declarados. Reutilizar patrones TaskHistory/RescheduleHistory y controles existentes, no montarlos por fila si causarían consultas N+1.

## 19. Revisión semanal — weekly_review

Normativa promovida desde progress/proposal_weekly_review.md tras revisión root. Define el alcance autorizado de 19; no acredita implementación ni pruebas.

### Resultado y alcance

Una pantalla privada «Revisión semanal» compara el plan vigente con el trabajo
real de siete días y muestra capacidad actual y días sin presupuesto. Cuenta
trabajo de sesiones abiertas y cerradas: esperar a CLOSE ocultaría trabajo ya
realizado. La consulta no completa tareas, no modifica sesiones ni crea una
revisión persistida. Sin puntuación, racha, porcentaje de productividad,
recomendación automática, listado de hechos, exportación o personalización20.
No duplicar el historial18 ni su paginación.

### Semana y zona

GET `/api/v1/weekly-review` admite únicamente `date` y `zoneId`, opcionales y
no repetidos. date es fecha gregoriana exacta YYYY-MM-DD, año0001–9999; sirve
como día ancla y se normaliza al lunes de esa semana. Sin date, se usa el día
del único serverNow en la zona resuelta. zoneId explícita debe pertenecer al
ZoneCatalog existente; no offset arbitrario, alias inventado ni trim tácito.
Sin zoneId se reutiliza la preferencia vigente de disponibilidad; ausente o
ya no disponible en catálogo implica UTC con causa explícita.

Siete fechas lunes–domingo; ocho fronteras `date.atStartOfDay(zone)` calculadas
por Java Time del servidor, una por fecha, no sumar24h. Ventanas semiabiertas
[startAt,endAt). Día saltado por cambio de zona puede tener longitud0: suma0,
no error ni día inventado. Semana DST puede durar167/169h. Dos offsets de una
hora repetida cuentan como instantes distintos. En cambios históricos del
catálogo una nueva consulta puede redistribuir días; no se promete snapshot
permanente ni se modifican datos históricos.

Con date explícita, si su lunes o domingo quedan fuera de 0001–9999, se
rechaza 400 VALIDATION_ERROR, field date/code INVALID_VALUE, antes de consultar
datos: la semana civil debe ser completa. Por ejemplo 9999-12-31 es inválida,
pues su domingo pertenece al año 10000. Si una frontera UTC, serverNow o su
fecha local no cabe en el rango público UTC [0001-01-01,+10000-01-01), se
responde 409 WEEKLY_REVIEW_TIME_OUT_OF_RANGE, sin fallback que oculte el
límite. Sin date, una semana civil incompleta derivada del reloj también es
409. El límite superior exclusivo UTC debe ser representable públicamente.
No limitar a semanas pasadas: futuro presenta plan y trabajo cero hasta
serverNow.

### Fuentes y sumas

Plan: `planned_blocks` con proyección vigente de `block_projections`, patrón
COALESCE de Hoy, sólo status planned. RESCHEDULED mueve el plan y CANCELLED lo
retira, incluso al revisar una semana pasada. Etiqueta «Plan vigente», nunca
«lo que habías planificado entonces». Proyecto/tarea completed no elimina
reservas ni trabajo; propiedad/contexto coherentes en todas las consultas.
No sumar originales más proyección, ni considerar planned_minutes de una
sesión o effective_end_at de EXTEND como reservas adicionales.

Trabajo: intervalos cerrados `work_session_intervals` y, sólo para running,
cola `[running_since,serverNow)`. Para running legado con running_since null
se usa started_at como hace el mapper15. paused/closed no tienen cola de
trabajo. Se interseca cada tramo con cada día y con el horizonte serverNow;
fin exacto en lunes00:00 no pertenece a la semana que empieza. Se cuenta una
sola vez: no sumar worked_microseconds a los intervalos. Este acumulado sirve
para comprobar integridad, no para distribuir fechas. workDate de CLOSE no
reparte trabajo: conserva su significado de atribución histórica16.

Descanso contemplado mediante días con presupuesto actual cero, sin castigo ni rachas. No calcular una métrica de pausas observadas ni inferir descanso real: los huecos entre intervalos simplemente no suman trabajo. Tiempo anterior al inicio y posterior al cierre tampoco cuenta.

Para cada día, duración es la suma de intersecciones reales en microsegundos.
Usar aritmética exacta (seconds*1000000+nanos/1000 o equivalente), sin redondeo
por tramo ni Duration.toNanos sobre el rango completo. Valores agregados
representables en long; overflow o datos seleccionados incoherentes devuelven
503 STORAGE_UNAVAILABLE, nunca wrap, saturación o página parcial. No fusionar
silenciosamente intervalos corruptos/solapados. Para sesión cuantificable,
intervalos ordenados no solapados, dentro de su vida y suma cerrada igual a
worked_microseconds; before extremo y tramo abierto deben ser coherentes.

Compatibilidad14: closed con changed_at null y sin intervalos/recibos carece
de final observable. No inferir que trabajó plannedMinutes ni hasta ahora.
Excluir esa sesión de duraciones y contarla en unquantifiedSessionCount sólo
si empezó dentro de la semana. workedMicroseconds representa sólo trabajo cuantificable, no total completo si el contador es positivo. Aviso «Hay sesiones antiguas iniciadas esta
semana sin duración registrada». El contador no asegura que ninguna sesión
antigua iniciada fuera pudiera haber cruzado la semana. Preservar V14 y filas
sin backfill inventado. Otras inconsistencias no se disfrazan de este caso
legacy y producen503 cuando afectan datos seleccionados.

Capacidad: presupuestos diarios de la preferencia ACTUAL, no histórico.
Sólo mostrar si su zona sigue disponible y coincide con la zona del informe;
si se eligió otra zona o no hay preferencia válida, capacityMicroseconds=null.
No convertir presupuesto entre zonas ni ajustar por DST: es duración elegida,
no longitud civil. Cero significa «Sin tiempo presupuestado actualmente»,
no descanso efectivamente disfrutado. Sin presupuesto conocido no calcular
exceso ni deuda. No bloquea mostrar plan/trabajo.

### Snapshot, seguridad y errores

Una transacción read-only REPEATABLE_READ establece primero el snapshot con
una consulta de datos, por ejemplo la preferencia aunque esté ausente. Sólo
después captura un único Clock truncado a microsegundos, dentro de la lectura
coordinada por el caso de uso. Abrir la transacción sin consultar todavía no
satisface este orden. Nunca capturar Clock antes del snapshot y permitir un
writer entre ambos. Preferencia, proyecciones, sesiones e intervalos se leen
en ese mismo snapshot y todas las sumas comparten serverNow.

Los datos seleccionados incluyen las sesiones con intervalos que intersectan
la semana, aunque esos intervalos sean antiguos, y las sesiones abiertas cuya
cola pueda intersectarla. Para cada sesión seleccionada se conserva su marca
vigente last_decision_at (con fallback heredado de la proyección17), además
de los extremos durables usados. Si serverNow precede una decisión o un
extremo confirmado seleccionado, responder 409 WEEKLY_REVIEW_TIME_OUT_OF_RANGE.
También aplica cuando EXTEND cambia last_decision_at sin cambiar changed_at:
una sesión viva con intervalo pasado dentro de la semana y marca posterior
al reloj hace fallar el informe completo. Es consistencia global del snapshot,
no sólo control del tramo de trabajo visible; no devolver resumen parcial,
recortar datos durables ni ocultar el reloj atrasado con cero.

Escritor confirmado antes del snapshot se ve entero; después no mezcla sus
nuevos intervalos con la proyección anterior. Siguiente GET puede ver nueva
revisión y otra captura de serverNow.

Principal determina owner. No parámetro owner/project/task ni filtros nuevos.
Sin autenticación401 antes de query; después query desconocida/repetida o
forma inválida400 VALIDATION_ERROR, códigos heredados por campo (date/zoneId o
query). Fechas y zona se validan antes de consultar datos; preferencia inválida
por catálogo usa fallback descrito, preferencia SQL ilegible no. GET no exige
CSRF y no acepta revisiones/keys como mecanismos de escritura. Almacenamiento,
deserialización o COMMIT read-only fallidos:503 sin datos parciales ni SQL.
No recursos explícitos que produzcan404. Error inesperado conserva500 común.
Sin caché privada persistente; no ETag, snapshot entre requests ni outbox.
No consumer nuevo, nuevas tablas o agregados materializados.

### DTO cerrado

200 JSON de exactamente11 campos:
`weekStart`, `weekEnd`, `zoneId`, `zoneSource`, `availabilityZoneId`,
`serverNow`, `startAt`, `endAt`, `days`, `totals`, `unquantifiedSessionCount`.
weekEnd es domingo inclusivo, endAt es lunes siguiente exclusivo. zoneSource:
EXPLICIT, AVAILABILITY, UNCONFIGURED o UNAVAILABLE; availabilityZoneId conserva
zona guardada o null aunque no se use, siguiendo Hoy.

Cada uno de los siete days tiene exactamente6 campos:
`date`, `startAt`, `endAt`, `plannedMicroseconds`, `workedMicroseconds`,
`capacityMicroseconds`.
Totales exactamente3: plannedMicroseconds, workedMicroseconds y capacityMicroseconds. Totales suman
los siete días; capacity total null si capacidad desconocida. Duraciones
como strings decimales canónicos no negativos, sin signo/ceros iniciales salvo
"0"; capacidad puede ser null. Contador como string decimal canónico también.
Fechas públicas YYYY-MM-DD; instantes UTC canónicos terminados en Z con resolución de microsegundos, siguiendo 14–18 e Instant.toString: se omite la fracción si es cero y no se exigen siempre seis decimales; no admitir precisión submicrosegundo ni offsets alternativos. No notas privadas, títulos, owner o recibos en este DTO.

Cliente valida forma cerrada, rangos, siete fechas consecutivas/lunes,
continuidad de fronteras, sumas con BigInt, duración no negativa, capacidad
null consistente y coherencia de la zona solicitada explícita. Antes de convertir a BigInt, valida longitud y rango decimal long (máximo 9223372036854775807), también para el contador; rechaza cadenas sobredimensionadas. Con date solicitado, weekStart/weekEnd corresponden a su lunes/domingo civiles mediante aritmética de calendario, sin TZDB. Sin date, serverNow pertenece a [startAt,endAt); con date explícito se admiten semanas pasadas o futuras sin exigir esa pertenencia. No recalcula
fronteras con TZDB/Intl ni con Date para validar aritméticaµs. Formato visual
puede usar Intl y fallbackUTC, sin alterar fecha/frontera/valores del servidor.
Dato incompatible es error de respuesta, nunca semana vacía o cero fabricado.

### UI y recuperación

Ruta privada `/revision-semanal`, enlace desde navegación existente. Selector
de fecha nativo, selector de zona con catálogo reutilizado y opción «Zona de
disponibilidad»; controles «Semana anterior», «Semana siguiente», «Esta
semana» y «Actualizar». No guarda preferencia20 ni fuerza elegir zona para
primer uso. Fecha y zona se editan como borrador sin GET; el formulario «Mostrar semana» aplica ambos e inicia la lectura. La selección aplicada se conserva en los parámetros date/zoneId de URL y se reproduce con Back y recarga. «Esta semana» omite date conservando zoneId; «Zona de disponibilidad» omite zoneId al aplicar. Selección permanece visible.
Resumen y siete filas/días accesibles, sin gráfico obligatorio ni paginación.
Textos permanentes explican plan vigente y capacidad actual. Días con presupuesto cero se rotulan descanso planificado actual, sin afirmar descanso real.

Mostrar «Datos consultados a…» con serverNow; no sumar tiempo localmente ni
polling/timer. Durante recarga misma selección puede conservar datos marcados
anteriores y loading; al cambiar semana/zona retirar el resumen anterior para
no atribuirlo a nuevos filtros. Error conserva selección y ofrece reintento
manual GET cuando procede: 503 permite Reintentar; 400 de campo anuncia el campo para corregir y aplicar con «Mostrar semana», sin reenviar automáticamente la query inválida; 409 WEEKLY_REVIEW_TIME_OUT_OF_RANGE anuncia el límite temporal y permite corregir selección o reintentar tras recuperar el reloj del servidor. Vacío válido muestra siete ceros, capacidad si conocida y enlace
a `/proyectos` para planificar; no POST/replay ni idempotencia nuevos.

Abortar cada solicitud sustituida antes de activar la siguiente generación;
tras cada await comprobar generación/acceso, incluida entregaHTTP401 antes
del observer. Logout o401 vigente elimina datos;401 obsoleto no revoca acceso
nuevo. JSON tardío/error clasificado tarde no restaura otra semana, identidad
ni datos privados. AbortController y generación reutilizados, sin localStorage
de datos. Un reintento pendiente no dispara duplicados. Foco en encabezado al
entrar; errores y carga anunciados sin mover foco en cada actualización;
controles44px, reflow y matriz30 de docs/ux-requirements.md se verifican para19.

### Arquitectura mínima y ejemplos contractuales

Reutilizar puertos/lector de transacción de Hoy e historial como patrón, con
un caso de uso y adaptador de consulta propios sólo cuando el test lo exija.
Dominio puro, Clock en aplicación, DTO HTTP separado. No devolver hechos para
que navegador calcule sumas ni cambiar comandos14–18. Para extremos0001 usar JDBC OffsetDateTime, como el lector de History, sin
getTimestamp/calendario híbrido. Índice sólo con necesidad
justificada; no nuevas capas de reporting o calendario.

Ejemplos medibles: tramo domingo23:30→lunes00:30 aporta30min a cada semana;
pausa intermedia resta sólo su intersección; CLOSE running incorpora último
tramo, paused no añade trabajo; EXTEND no suma plan ni neto; cancelación elimina
plan pero conserva trabajo. Madrid2026-03-29 01:30→03:30 son60min reales;
Madrid2026-10-25 01:30→03:30 son180min, ambos cruzan DST. Semana vacía y capacidad0
no afirman descanso; zona inválida explícita400, preferencia desaparecidaUTC;
clock atrasado409; datos ajenos excluidos; writer entre lecturas no mezcla
snapshot; reinicio/outbox retirado conservan resultado salvo serverNow live;
respuesta antigua tras cambio de semana o logout no repuebla la pantalla.

## Feature 20 — apariencia: contrato propuesto para revisión

### Propósito y límite

Personalizar la presentación propia sin cambiar proyectos, tareas, planificación, trabajo, presupuesto ni hechos históricos. Se conservan los contratos 1–19. Este incremento admite exactamente tres preferencias editables: `theme`, `accentLight` y `accentDark`. Tema LIGHT/DARK/SYSTEM; dos colores de acento libres validados. La personalización amplia sigue incremental: densidad, tipografía, disposición, vistas/campos, plantillas y automatizaciones no se implementan ni se declaran cubiertas en 20. Sin CSS/HTML, nombres de color, imágenes, transparencias o código aportados por el usuario.

### Colores, roles y contraste

Entradas de color: cadena exacta `#[0-9a-fA-F]{6}`, sin recorte; se normaliza a mayúsculas al confirmar. No se aceptan #RGB, alfa, rgb(), var(), URL ni cadenas vacías. Defaults: `SYSTEM`, `#244C3C` para claro y `#B7E4C7` para oscuro. No se aclara/oscurece silenciosamente un color inválido.

El acento efectivo cambia sólo: texto de enlaces de acción (incluida navegación), relleno de la acción primaria y su indicador, borde/indicador de selección y contorno de foco. El estado seleccionado sigue identificado mediante texto/semántica y forma/borde, nunca sólo color. Enlaces mantienen distinción no cromática. Error, advertencia, éxito, texto normal y texto secundario usan roles semánticos seguros del tema, no el acento del usuario. No modifica dimensiones, orden, visibilidad ni contenido.

Superficies opacas sobre las que puede aparecer el acento:

| Rol | Claro | Oscuro |
| --- | --- | --- |
| Lienzo | #F8F9F5 | #111827 |
| Panel | #FFFFFF | #1F2937 |
| Control editable | #FDFEFB | #182232 |
| Navegación secundaria | #EEF1E9 | #0B1220 |
| Selección | #DFE8D9 | #28394A |
| Hover | #D0DFC9 | #33485C |

Cada acento debe alcanzar contraste **≥4,5:1 contra las seis superficies de su tema**, incluso si ese tema no está activo. Esto protege su uso como texto y supera el mínimo interno de 3:1 para indicadores/foco. Los acentos no se dibujan encima de otros fondos sin una de estas superficies; en particular avisos semánticos y tarjetas decorativas conservan sus colores seguros y colocan acciones en una superficie definida. La acción primaria utiliza el acento como fondo; su texto es #000000 o #FFFFFF, el que dé mayor contraste (empate: negro), y debe alcanzar 4,5:1. Hover no mezcla/transluce el acento: cambia la superficie neutral o añade una señal de forma, preservando los contrastes.

Se usa luminancia relativa sRGB: canal c=byte/255; si c≤0,04045, cLineal=c/12,92, en otro caso ((c+0,055)/1,055)^2,4. L=0,2126R+0,7152G+0,0722B. Contraste=(max(L1,L2)+0,05)/(min(L1,L2)+0,05). Comparar el valor completo con 4,5, sin redondeo ni tolerancia para aceptar. Referencia primaria: [WCAG 2.2, contraste mínimo](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum). La elección uniforme de roles/superficies es del producto, no una certificación automática de accesibilidad.

Claro/oscuro se aplican a todas las superficies existentes, incluidas login, formularios, errores, listas, sesión, historial y revisión semanal. Se reutilizan tokens SCSS; no rediseño de layouts ni filtros CSS de inversión. Texto normal/secundario y errores conservan contraste al menos 4,5:1; bordes indispensables, controles y foco al menos 3:1 sobre sus fondos reales. No se colorean arbitrariamente los valores históricos. Controles nativos usan `color-scheme` coherente. Preferencia `prefers-reduced-motion` y forced-colors se respetan; sin animación obligatoria ni desactivar colores forzados del usuario.

El anillo de foco se dibuja fuera del botón acentuado, separado por superficie neutral/offset; nunca se valida acento contra su propio relleno. Con colores forzados se permiten los colores del sistema operativo, preservando visibilidad y semántica, sin imponer el color guardado.

### Recurso privado y concurrencia

GET y PUT `/api/v1/me/appearance`, sin parámetros de consulta. Propietario únicamente desde Principal; no owner en DTO, query, ruta o cuerpo. Sesión, origen, CSRF de PUT, JSON estricto, problem+json y no-store reutilizan los contratos comunes. GET no necesita CSRF. No hay ruta por identificador ajeno.

GET 200 devuelve exactamente `{configured,theme,accentLight,accentDark,updatedAt}`. Sin fila: false, SYSTEM, #244C3C, #B7E4C7 y null, con ETag fuerte literal `"appearance:unconfigured"`; no inserta. Configurada: true, los tres valores canónicos y updatedAt UTC en años 0001–9999 con precisión máxima de microsegundos; ETag `"appearance:<uuid canónico minúsculo>:<versión decimal canónica BIGINT no negativa>"`. No se serializa la versión como número JSON. UUID/versión son internos, no texto de interfaz. El tag y cuerpo salen del mismo snapshot.

PUT reemplaza exactamente los tres campos, exige un único If-Match fuerte con el formato anterior. Devuelve 200 con la misma forma de GET y el nuevo ETag después del commit; sin Location ni clave de idempotencia. No hay PATCH/DELETE/reset separado. Restaurar defaults usa este mismo PUT con los tres valores por defecto: conserva fila y control de revisión, no simula que jamás existió configuración.

Una fila por owner_id UNIQUE, UUID propio, versión BIGINT, tres valores y updatedAt. Primer guardado versión 0; cambio real incrementa una vez. Primero comprobar identidad/revisión propia en la transacción, después comparar valores canónicos. No-op con revisión vigente conserva exactamente cuerpo/tag/fecha; incluso distinta capitalización de entrada es no-op tras normalizar. Revisión antigua produce 412 aunque los valores ya coincidan. Dos altas desde ausencia producen un 200 y un 412. Actualización usa bloqueo/condición por propietario, no bloqueo global. Guardado real con versión máxima o almacenamiento incapaz de confirmar produce 503 y ninguna modificación; no-op máximo válido no incrementa. updatedAt es máximo entre instante previo y reloj truncado a microsegundos. El límite temporal no se desborda al emitir DTO.

Fallo PostgreSQL antes del commit revierte; pérdida de respuesta después puede haber confirmado. Reinicio del proceso y nuevo navegador recuperan la preferencia propia. No hay localStorage como autoridad, ni persistencia privada anterior al login. No se crea evento/outbox en 20, conforme al precedente de disponibilidad: no existe consumidor de preferencia personal; no se inventa un proyecto/agregado ni se altera la historia. No se anuncia un cambio EDA inexistente. Migración aditiva, sin reescribir migraciones anteriores ni hechos 1–19.

El no-op válido no consulta el reloj, también en versión máxima. En alta o cambio real, reloj que falla o cuyo instante queda fuera de años 0001–9999 produce 503 STORAGE_UNAVAILABLE sin escritura; no se recorta al límite ni se serializa año ampliado. Una fila existente corrupta o incompatible al leer produce 503 STORAGE_UNAVAILABLE, nunca defaults. Validación de los tres valores precede a acceso transaccional; identidad/revisión se comprueban bajo la exclusión propia antes del no-op y de capturar tiempo nuevo.

### Validación y errores

Precedencia: seguridad y negociación HTTP heredadas (incluido 415) antes del handler; query desconocida/repetida, If-Match, JSON/campos, revisión en almacenamiento. Cualquier query: 400 VALIDATION_ERROR de query, incluso con falta de If-Match. If-Match ausente: 428 PRECONDITION_REQUIRED; débil, lista, repetido, otro recurso, UUID no canónico o versión fuera de BIGINT: 400 VALIDATION_ERROR campo If-Match. Tag bien formado que no coincide con fila propia: 412 APPEARANCE_CONFLICT, sin datos ajenos. Fallo de lectura/guardado: 503 STORAGE_UNAVAILABLE, nunca defaults presentados como ausencia confirmada. Campos/códigos usan la forma problem existente, sin SQL/stack/secretos.

JSON ausente/vacío/malformado/duplicado/concatenado: 400 MALFORMED_JSON. Objeto obligatorio. Orden de validación: forma body; extras raíz en orden léxico; theme; accentLight; accentDark. Ausente/null: REQUIRED; tipo distinto de cadena: INVALID_TYPE; extra: UNKNOWN_FIELD; enum/formato inválidos: INVALID_VALUE; color bien formado con contraste insuficiente: INSUFFICIENT_CONTRAST. Cada fallo se asocia a su campo. Theme distingue mayúsculas exactas. Revisión se comprueba antes de no-op pero después de validar la entrada.

Decoder cliente comprueba JSON cerrado, booleano configured, tres valores/contrastes, updatedAt y ETag coherentes con configured. false requiere exactamente defaults/null y tag unconfigured; true requiere timestamp/tag configurado válidos. No reinterpreta una respuesta configurada inválida como defaults. PUT válido exige además que valores canónicos coincidan con la intención enviada. Problemas se clasifican sólo si HTTP/status/code/type son coherentes; errores desconocidos no se convierten en éxito ni ausencia. Mantener guards después de cada await y antes del observador de acceso, también HTTP 401 tardío.

### Presentación y recuperación

Ruta exacta `/apariencia`, accesible desde Principal como Apariencia y conservada en retorno tras login/Back/recarga. Una carga propia por sesión activa, compartida entre shell/página para no crear lecturas incompatibles al navegar. Al iniciar sin snapshot, se usa provisionalmente SYSTEM/defaults seguros; fallo de GET se anuncia como no cargado con Reintentar, no como preferencia guardada. Tras 401/logout se retiran snapshot y borrador propios y se vuelve a defaults; un login distinto no hereda preferencias del anterior. No bloquear el acceso al resto del producto por un fallo de apariencia.

Con SYSTEM, la apariencia efectiva sigue `prefers-color-scheme: dark`; si no se puede consultar, claro. Cambio del sistema mientras montado actualiza sólo presentación, no PUT ni preferencia guardada. LIGHT/DARK ignoran ese cambio. Navegación interna conserva preferencia confirmada sin recargarla por cada ruta. Al volver de suspensión/visibilidad, se resuelve el valor actual del medio sin temporizadores de sondeo.

Formulario con grupo Tema (Claro, Oscuro, Sistema), dos controles de color nativos acompañados de entradas hexadecimales etiquetadas, Guardar apariencia y Restaurar valores predeterminados. Los dos controles de cada color representan un solo valor. Vista previa local rotulada muestra ambos temas, enlace, botón y foco; no aplica borrador al shell ni guarda al editar. Un color inválido conserva texto, muestra error y no se aplica ni a la muestra; muestra conserva último color seguro con explicación. Ayuda en español normal, sin diagnósticos de ETag/owner/SQL. La muestra no ejecuta operaciones de negocio.

Restaurar prepara SYSTEM y los dos defaults, anuncia cambios pendientes y requiere Guardar; es reversible antes de guardar. Aviso de que salir pierde cambios sin guardar; Cancelar cambios vuelve al snapshot confirmado sin HTTP. Guardar espera valores válidos y versión leída, anuncia carga antes de 400 ms, bloquea duplicados y conserva foco cuando el control sigue. Confirmación válida actualiza snapshot, formulario y shell juntos. No éxito optimista, ni cambiar tareas, fechas o trabajo.

400 de campo conserva borrador y permite corregir. 412, red, 503 o confirmación inválida dejan estado sin confirmar y prohíben otro PUT hasta Recargar versión guardada; texto explica que esta consulta reemplazará el borrador. Sólo GET válido reemplaza juntos valores/ETag/borrador y libera una nueva intención manual. Fallo de esa consulta conserva borrador y mantiene incertidumbre. No GET/PUT de reenvío automático, no comparación que convierta igualdad en recibo de una intención perdida; no se afirma quién guardó los valores recuperados.

Lecturas viejas quedan invalidadas al comenzar escritura y al confirmar; una respuesta anterior no restaura el tema/ETag ni borra el borrador actual. Lecturas/PUT cancelados al salir de la sesión no aplican datos ni disparan observador 401 sobre otra sesión. Nueva ruta no descarta la apariencia global confirmada, pero sí el borrador local abandonado. Foco del encabezado al entrar; tras desaparecer un iniciador se devuelve al encabezado sólo si el usuario no movió el foco deliberadamente. Errores de campo enlazan descripción y aria-invalid; estados de espera y recuperación son anunciados.

### Evidencia exigida antes del cierre

TDD individual, HTTP/PG reales con aislamiento de propietarios, no-op/revisión/carreras/rollback y recuperación tras reinicio. E2E guardar, recargar, SYSTEM y reset real; contraste de ambas variantes y colores arbitrarios aceptados en roles efectivos de pantallas existentes (también error/disabled/foco). Revisión de 30 principios completa en informe UX, con todas las filas y límites explícitos. Matriz responsive de docs/ux-requirements: 320–2560 y lados de breakpoints, alturas reducidas, texto/zoom nativo 200 %, teclado y 44×44, Chromium/Firefox/WebKit, movimiento reducido/forced-colors. No atribuir dispositivos físicos o facilidad psicológica universal a emulación o axe. No implementación ni spec_ready hasta revisión root del contrato y Gherkin.

## Feature 21: custom_views_fields — vistas y campos personales

### Objetivo y límites

Personalizar dos superficies existentes: lista de proyectos y listas de tareas
(incluidas subtareas). La configuración es propia por cuenta y ámbito PROJECT
o TASK, no por proyecto: TASK se aplica a todas las tareas de esa cuenta.
El nombre/título enlazado, estado, acciones, contexto y paginación nunca se
ocultan ni reordenan. Sólo se personalizan metadatos de presentación; no el
orden de entidades ni los filtros del servidor.

Los campos personales tienen definición por ámbito y valores independientes
en cada proyecto/tarea propios. Se consultan y editan en los detalles, no en
cada fila de las listas: no se añade lectura N+1 ni se modifica la paginación.
Una subtarea no hereda valores del padre ni del proyecto. Un campo llamado
«Avance» u «Horas» es información declarada, no progreso, estimación, tiempo
real o hecho histórico. Los contratos y DTO 1–20 permanecen intactos.

Sin tableros, vistas guardadas múltiples, fórmulas, filtros nuevos, opciones
personalizadas, relaciones, adjuntos, HTML/CSS ejecutable, obligatoriedad de
campos ni automatizaciones. No exportación/importación anticipadas de 22/23.

### Presentación y definiciones restaurables

Cada ámbito tiene una lista ordenada visibleFields, sin duplicados. PROJECT
admite createdAt y updatedAt; default [createdAt]. TASK admite
completionCriterion, estimatedMinutes, createdAt y updatedAt; default
[completionCriterion,estimatedMinutes]. Lista vacía es válida. El orden del
array es el orden de lectura visual/DOM de esos metadatos, después de la
identidad y el estado esenciales. Fechas nativas se presentan en UTC como
hasta ahora; estimación null sigue siendo «Sin estimación». El criterio se
oculta sólo en la lista, nunca se elimina del detalle o formulario de tarea.

Hasta 12 definiciones por ámbito, contando activas e inactivas, en este corte.
Cada una tiene UUID generado por servidor, label, type y active. Orden estable
de creación, con UUID como desempate; sin orden manual de definiciones.
Label se recorta por Unicode White_Space exterior, conserva interior y exige
1–60 puntos de código. Es único por ámbito tras ese recorte, sensible a
mayúsculas y sin normalización Unicode, también entre inactivas. El tipo es
inmutable: TEXT, NUMBER, DATE o BOOLEAN. NUMBER representa números enteros
exactos entre -1000000000 y 1000000000; la UI lo rotula «Número entero».
No se admiten decimales en 21. La elección evita redondear datos privados.

Crear activa la definición, sin rellenar ninguna entidad. Renombrar sólo
cambia su etiqueta actual. Desactivar oculta el campo en lectura/edición
ordinarias, pero conserva definición y valores; reactivar recupera esos
valores sin conversión. No hay DELETE ni cambio de tipo: la UI explica el
límite antes de crear y permite gestionar/reactivar las definiciones ocultas.
No se borran datos por restaurar. «Restaurar vista» prepara sólo visibleFields
por defecto y requiere Guardar vista; no altera definiciones ni valores.
«Vaciar» un valor es una decisión explícita sobre ese campo, guardada como
null, no un reset global de datos.

Valores opcionales: null significa sin valor; NUMBER usa un número JSON cuyo
valor matemático sea entero exacto dentro del rango. Se admiten 1.0 y 1e3
como 1 y 1000; se canoniza a entero, sin redondear fracciones ni convertir
strings. BOOLEAN true/false, DATE string YYYY-MM-DD válida
en calendario gregoriano 0001–9999 sin hora/zona, TEXT string de hasta 1000
puntos de código conservando espacios/saltos. TEXT vacío se normaliza a null;
no recortar texto no vacío. Cero y false nunca se convierten en ausencia.
No coerción de strings a número/booleano ni de fecha a instante. Labels y
textos rechazan U+0000 y surrogates UTF-16 aislados; pares válidos cuentan un
punto. Todo se renderiza como texto seguro.

### API de configuración propia

Rutas bajo /api/v1/me/customization/{scope}, scope exacto PROJECT o TASK,
sin query ni owner proporcionado por cliente. GET 200 devuelve exactamente
{configured,visibleFields,customFields,updatedAt}. Cada customFields contiene
exactamente {id,label,type,active}, incluyendo inactivos en el orden fijado.
Sin fila: false, defaults, [], null; no inserta. Con fila: true y updatedAt
UTC canónico con precisión máxima de microsegundos y años 0001–9999.

ETag fuerte: "customization:PROJECT:unconfigured" (o TASK) sin fila;
"customization:PROJECT:<uuid>:<version>" configurado. UUID minúsculo canónico,
versión decimal canónica BIGINT no negativa, conforme al patrón de 20.
GET body/ETag salen del mismo snapshot. No UUID/versión internos en JSON.

Tres comandos, todos con If-Match de esa configuración:

- PUT /api/v1/me/customization/{scope}, body cerrado {visibleFields},
  reemplaza sólo presentación, devuelve 200 con configuración completa.
- POST /api/v1/me/customization/{scope}/fields, body {label,type}, crea una
  definición y devuelve 200 con configuración completa tras commit. Es un
  comando sobre el agregado; no hay Location ni recibo/idempotency-key.
- PUT /api/v1/me/customization/{scope}/fields/{fieldId}, body {label,active},
  actualiza sólo esa definición y devuelve 200 con configuración completa.
  FieldId inexistente o de otro ámbito/owner devuelve 404 sin revelar datos.

Una revisión compartida por ámbito serializa presentación y definiciones.
Primera escritura crea configuración versión 0; cada cambio real posterior
incrementa una vez. Comparar revisión antes de detectar no-op. No-op vigente
conserva body/tag/fecha y no consulta Clock. Revisión antigua da 412 incluso
si los valores coinciden. Dos altas desde unconfigured sólo permiten un
commit; el otro recibe 412. Alta con label ya usado o límite alcanzado da
400 VALIDATION_ERROR del campo label o customFields, código INVALID_VALUE,
comprobado bajo el mismo bloqueo después de la revisión vigente.

### Valores por proyecto o tarea

GET/PUT /api/v1/projects/{projectId}/custom-fields y
/api/v1/projects/{projectId}/tasks/{taskId}/custom-fields. Autorización real
por joins propios: tarea debe pertenecer a ese proyecto; ajeno/inexistente
es 404. Se permite editar metadatos también en proyectos y tareas terminados;
no reabre ni cambia su revisión, estado o updatedAt de negocio.

GET 200 devuelve exactamente {configured,values,updatedAt}. values contiene
una entrada por definición activa, en orden de definición, exactamente
{fieldId,label,type,value}; ausente en almacenamiento se representa null.
No devuelve valores de definiciones inactivas. La ausencia de fila propia
produce configured:false/updatedAt:null sin insertar; la lista puede contener
campos activos todos null. La lista vacía no significa que no haya valores
preservados de campos desactivados.

ETag fuerte de valores compuesto, con un único formato:
"custom-values:<scope>:<entityId>:schema:<schemaRevision>:values:<valuesRevision>".
Scope es PROJECT o TASK; entityId es projectId o taskId respectivamente.
Cada revisión es el literal unconfigured o <uuid>:<version>, con UUID y
versión canónicos como configuración. Por ejemplo, sin ninguna fila:
"custom-values:PROJECT:11111111-1111-1111-1111-111111111111:schema:unconfigured:values:unconfigured".
La revisión schema cambia al renombrar/desactivar/crear/reactivar, por lo que
una representación con etiquetas o campos distintos nunca conserva el mismo
ETag. GET lee propiedad, configuración y valores en una sola transacción
REPEATABLE READ/readOnly; DTO y ETag compuesto salen del mismo snapshot, sin
Clock ni lecturas por cada definición. No hay otra cabecera de revisión.

PUT exige sólo If-Match con ese ETag compuesto;
body cerrado {values}, cada entrada exactamente {fieldId,value}. Debe
contener todos y sólo los IDs activos, una vez cada uno; orden de entrada
irrelevante. Se valida el valor según su tipo guardado. Omisión no borra un
campo ni se interpreta como null. Valores inactivos se conservan sin tocar.
Respuesta 200 tiene la forma y ETag compuesto de GET tras commit. Primera
escritura, no-op, revisión y límites siguen las reglas anteriores; comparar
valores canónicos, no orden del array. Guardar todos null puede crear una
fila configurada; no simula ausencia histórica.

Bloqueo transaccional en orden fijo: configuración propia del ámbito y luego
valores de la entidad. Calcular y comparar el ETag compuesto vigente antes
de validar el conjunto activo y los tipos contra esa configuración. Si otra pestaña renombra,
crea/desactiva/reactiva un campo entre GET y PUT, devuelve 412 y no escribe
ningún valor. Renombrar/desactivar no cambia las revisiones de cada entidad;
el componente schema del ETag protege esa concurrencia sin actualizar todas
las filas. Dos cambios reales competitivos con la misma revisión tienen un
único ganador; un no-op vigente no consume revisión ni convierte otro no-op
vigente en conflicto.

### Seguridad, persistencia y errores

Se reutilizan autenticación, CSRF/origen en escrituras, negociación HTTP,
JSON estricto, problem+json, no-store y precisión temporal de 20. Toda lectura
restringe owner; no eventos/outbox nuevos: son configuración y metadatos
privados sin consumidor, no hechos de trabajo. PG y broker anteriores,
historial, métricas, recibos y datos 1–20 no cambian por esta feature.
Migración aditiva posterior a V19; dominio puro, puertos/casos de uso y
adaptadores conforme a la arquitectura vigente. No motor genérico de schemas.

Orden: seguridad; query/ruta; cabeceras requeridas; sintaxis JSON/shape/campos
independientes del estado; propiedad del recurso/definición; revisión; reglas
que necesitan configuración vigente; escritura. GET no exige CSRF. Cualquier
query se rechaza 400 query INVALID_VALUE. Scope inválido: 400 scope
INVALID_VALUE; UUID de ruta inválido: 400 INVALID_FORMAT. Falta de cabecera
exigida: 428 PRECONDITION_REQUIRED; tag débil/repetido/lista/no canónico o de
familia/ámbito distinto: 400 de esa cabecera. Tag válido pero no vigente:
412 CUSTOMIZATION_CONFLICT (configuración o valores), sin estado ajeno.

JSON malformado/duplicado/concatenado: MALFORMED_JSON. Objeto/campos cerrados;
extras UNKNOWN_FIELD; ausentes/null donde sean obligatorios REQUIRED; tipo
incorrecto INVALID_TYPE; enum/rango/duplicado/fecha/formato inválido
INVALID_VALUE. Se recorre primero raíz, extras léxicos y luego orden de
campos declarado en cada body; arrays por índice. null es válido sólo para
valor, no para arrays, definición o flags. Errores de entrada independientes
del estado no se posponen para devolver 412. Un valor de tipo desconocido
se valida sólo después de propiedad y revisiones, no por datos del cliente.

Precisión de implementación: repetir un fieldId en el array es detectable
sin consultar configuración y devuelve 400 values[i].fieldId INVALID_VALUE
en el índice repetido, incluso con una revisión antigua bien formada.
No se confunde con un ID inactivo/desconocido o un conjunto incompleto,
que requieren configuración vigente y se validan después de la revisión.

Fallo de almacenamiento, fila seleccionada corrupta, versión máxima del recurso
que debe cambiar o Clock fallido/fuera del rango: 503 STORAGE_UNAVAILABLE, sin parcial.
updatedAt de cambio real usa max(previo,Clock truncado a microsegundos);
el no-op no usa Clock. Confirmación sólo después del commit. Defaults sólo
por ausencia confirmada, nunca por fallo/corrupción. Configuración, definición
y valores confirmados sobreviven a reinicio y sesión nueva. No se borra
ninguna fila de valores al desactivar/restaurar presentación.

### Interfaz y recuperación

Gestión contextual «Personalizar vista» en Proyectos y en listas Tareas/
Subtareas, con ámbito explicado. Controles nativos de mostrar/ocultar y
Subir/Bajar sin arrastre obligatorio; Guardar vista/Cancelar/Restaurar vista.
Gestión de campos separada del formulario de presentación, desde esos mismos
contextos: etiqueta, tipo al crear, y renombrar/desactivar/reactivar después.
Sin crear rutas globales nuevas ni añadir una GET a todas las pantallas.

En detalles, sección «Campos personales» separada de hechos y acciones de
negocio. Texto/entero/fecha nativos; booleano permite «Sin valor», «Sí», «No».
Editar no escribe; Guardar campos confirma el conjunto, Cancelar vuelve al
snapshot. Desactivar explica que oculta y conserva; reactivar recupera datos.
La personalización no remonta ni borra formularios de creación de tareas,
sesiones u otros borradores ajenos. Nombre/estado/acciones siguen presentes
en todas las configuraciones, incluso sin metadatos opcionales.

Carga fallida no bloquea el resto de negocio. Listas usan provisionalmente
presentación base, claramente no confirmada, con Reintentar; detalles no
muestran campos vacíos como si fueran una consulta válida. No valores
privados en localStorage/URLs ni etiquetas en telemetría. Estado compartido
sólo entre consumidores de ese scope/recurso y sesión autenticada; no polling.
Logout/cambio de cuenta o 401/404 de contexto retira datos/errores/borradores
propios. Invalidar lecturas al comenzar y confirmar escrituras; aborto y
chequeo de identidad tras cada await y antes del observador401. Respuestas
viejas de proyecto/tarea/configuración no restauran un recurso retirado.

400 de campo conserva borrador para corregir; 412/red/503/confirmación
incompatible impiden otra escritura de ese recurso hasta «Recargar guardado»
manual. Se explica que reemplaza borrador, no que confirma la misma intención.
El bloqueo de resultado incierto sobrevive a navegación interna mientras la
sesión siga, también para crear definición; volver no reenvía POST/PUT ni
consulta automáticamente para liberar ese bloqueo. GET válido reemplaza
snapshot/ETag y habilita una decisión manual nueva; no reenviar el alta
sólo porque apareció otra definición de nombre igual. La etiqueta única y la
revisión impiden una segunda alta inadvertida, sin afirmar recibo idempotente.

Decoder valida objetos cerrados, límites, tipos, IDs únicos y ETag coherente,
configured/fecha y valores contra tipos recibidos; conserva el orden recibido
sin deducir una cronología desde UUID sin timestamps. Defaults sólo en ausencia
coherente. Confirmaciones de escritura deben corresponder a scope/recurso e
intención normalizada, incluyendo UUID nuevo en alta y conservación del resto
confirmado; una respuesta incompatible sigue siendo incierta.

Reutilizar tokens de Apariencia20 y docs/ux-requirements.md, con controles
agrupados por decisión. Foco y anuncios de carga antes de 400 ms, errores asociados, retorno sólo si el iniciador desaparece y el
usuario no movió foco, teclado/44px/reflow320/texto y zoom200, ambos temas,
forced-colors y movimiento reducido. La evidencia nueva cubrirá nominal,
restauración sin pérdida, tipos/null, conflicto y privacidad; las 30 filas UX
se revisan con límites. No se atribuye esa evidencia antes de implementar.

## Feature 22: export_data — exportación privada y portable

### Propósito y frontera

Descargar todos los datos de organización propios que las funcionalidades 1–21
conservan de forma durable, incluidos los hechos históricos y datos ocultos por
personalización. El archivo permite inspección y transporte fuera del servicio;
no es un backup operativo ni acredita restauración/importación, que pertenece a
23. Contrato técnico revisado bajo la autorización global vigente. La destilación
Gherkin y su revisión preceden a la implementación.

Un único formato: JSON UTF-8 sin BOM, identificado y versionado. El alcance
original no exige CSV, ZIP, PDF ni hojas de cálculo; no se presentan como formatos
ya aprobados. ICS pertenece a 26. No incluye conectores, correos, programación
periódica, enlaces públicos ni almacenamiento de archivos en el servidor.

### Recurso HTTP y snapshot

GET `/api/v1/me/export`, sin parámetros de consulta ni cuerpo, sólo para la sesión
autenticada. El propietario procede exclusivamente del Principal. Cualquier
parámetro, incluso ownerId, format, cursor o uno vacío, produce 400
`INVALID_EXPORT_QUERY`; no se permite un filtro implícito que omita datos.
Se conserva la política GET de seguridad existente: no exige CSRF, no crea una
sesión de trabajo, no produce una mutación de negocio ni registra una outbox.

Accept ausente o que admita application/json con calidad positiva permite el
formato; se respetan listas, comodines y exclusiones específicas de negociación
HTTP. Si no admite JSON, responde 406 `EXPORT_FORMAT_NOT_ACCEPTABLE`, sin preparar
el snapshot. Una cabecera Accept mal formada o un cuerpo GET no vacío produce
400 `INVALID_EXPORT_REQUEST`; un cuerpo de longitud cero equivale a ausencia.
HEAD autenticado responde 405 con Allow: GET y sin cuerpo ni lectura de negocio;
no se deja que el soporte HEAD automático construya una exportación para
descartarla. Los métodos no admitidos conservan primero los filtros de seguridad
existentes y después 405. No se añaden métodos de escritura.

La respuesta 200 contiene el archivo íntegro. Todas las colecciones, sus conteos
y validaciones pertenecen a una única transacción PostgreSQL read-only con
snapshot repeatable-read; no se compone exportando páginas HTTP independientes.
Una escritura concurrente queda enteramente dentro o fuera del snapshot, sin
mezclar definición nueva y valor anterior ni una tarea sin su proyecto visible
en ese mismo snapshot. Las relaciones se filtran por propietario en todas las
consultas, también tablas sin owner_id mediante su proyecto/sesión padre propio.
Una relación o recibo incoherente dentro del conjunto propio causa 503; no se
oculta una fila ni se incluyen datos ajenos para completar una relación corrupta.

`exportedAt` es un único instante obtenido del reloj inyectado al preparar el
snapshot y truncado a microsegundos, sin redondear. Ese mismo instante normalizado
se reutiliza en el envelope y el nombre del archivo; no modifica instantes
persistidos. Identifica esta preparación, no es una marca causal de commit ni un
avance de sesiones abiertas. Fallo del reloj o instante fuera de 0001–9999 produce
503 sin archivo. No se requiere RabbitMQ ni se consulta el estado de publicación.

La serialización debe completarse dentro del límite antes de enviar estado 200
o bytes de descarga. No se sirve un archivo parcial como éxito si falla una
consulta, la validación o la serialización. Una interrupción posterior de la red
puede impedir recibir el archivo completo; el cliente no la anuncia como éxito.
No se introducen tablas, recibos de exportación ni eventos por esta lectura.

La lectura, validación y serialización deben tener memoria acotada: cursor/lotes
de registros dentro del mismo snapshot, o un presupuesto equivalente comprobable.
No basta contar 100000 filas y materializarlas completas antes de comprobar sus
bytes. El presupuesto incluye datos leídos, relaciones de validación y salida;
no puede crecer con todo el volumen de texto de la cuenta. Campos persistidos
anormalmente grandes también se rechazan antes de materializarlos sin límite.
413 exige exceso demostrado de bytes exportables; el tamaño bruto de JSONB
desconocido o corrupto no lo demuestra. Corrupción sin exceso demostrado conserva
503, respetando la precedencia ya definida para fallos de preparación.
La salida se acumula en un buffer privado limitado a 32 MiB; la comprobación se
hace antes de cada ampliación, sin duplicar primero el documento entero para
medirlo. Al fallar o cancelar se descartan buffers, se cierra el cursor y termina
la transacción. Esto no exige streaming de bytes al navegador antes de validar:
el envío empieza sólo cuando el documento completo y su tamaño están confirmados.

### Documento JSON v1 cerrado

El objeto exterior tiene exactamente `format`, `schemaVersion`, `exportedAt`,
`owner`, `data` y `counts`. `format` es `organizationweb-export`, `schemaVersion`
es el número entero 1, `owner` es el nombre de la identidad autenticada y no una
credencial. `data` y `counts` contienen exactamente las siguientes catorce claves.
Cada clave de data es siempre un array; cada clave de counts es su longitud
entera no negativa. Ausencia de una preferencia persistida se representa como
array vacío, sin insertar ni materializar defaults ficticios.

| Colección | Campos exactos de cada registro |
| --- | --- |
| projects | id, name, description, status, version, createdAt, updatedAt |
| tasks | id, projectId, parentId, title, completionCriterion, estimatedMinutes, status, version, completedAt, createdAt, updatedAt |
| taskStatusHistory | id, projectId, taskId, taskVersion, fromStatus, toStatus, occurredAt |
| availability | id, zoneId, mondayMinutes, tuesdayMinutes, wednesdayMinutes, thursdayMinutes, fridayMinutes, saturdayMinutes, sundayMinutes, version, createdAt, updatedAt |
| plannedBlocks | id, projectId, taskId, requestKey, objective, startLocal, endLocal, zoneId, startOffset, endOffset, allowOverBudget, startAt, endAt, durationMinutes, createdAt |
| blockProjections | blockId, version, status, updatedAt, startLocal, endLocal, zoneId, startOffset, endOffset, startAt, endAt, durationMinutes |
| blockChanges | id, projectId, taskId, blockId, requestKey, kind, version, occurredAt, receipt |
| workSessions | id, projectId, taskId, requestKey, startedAt, plannedMinutes, plannedEndAt, zoneId, status, revision, changedAt, workedMicroseconds, runningSince, effectiveEndAt, lastDecisionAt |
| workSessionIntervals | sessionId, revision, startAt, endAt |
| workSessionChanges | id, sessionId, requestKey, action, expectedRevision, occurredAt, receipt |
| appearance | id, theme, accentLight, accentDark, version, updatedAt |
| customization | id, scope, visibleFields, customFields, version, updatedAt |
| projectCustomFieldValues | id, projectId, values, version, updatedAt |
| taskCustomFieldValues | id, projectId, taskId, values, version, updatedAt |

Los registros son cerrados: no se serializan filas SQL ni JSONB completos mediante
un mecanismo genérico. `customFields` contiene los cuatro campos de definición
de 21 (`id`, `label`, `type`, `active`), incluidas las desactivadas, en su orden
guardado. `visibleFields` conserva su orden. `values` contiene los pares cerrados
`fieldId`, `value` que están realmente persistidos, también si la definición está
inactiva; se ordenan por fieldId. Un valor ausente sigue ausente y un null
persistido sigue null. No se inventan valores para definiciones recién creadas.
Cada fieldId debe resolver una definición del mismo propietario y ámbito.

`receipt` de blockChanges tiene exactamente los siete campos durables de
BlockChangeReceipt: `id`, `blockId`, `kind`, `version`, `occurredAt`, `before`
y `after`. Se conserva el nombre `version`, como string decimal canónico de long;
no se sustituye por el `revision` del DTO HTTP. `kind` es RESCHEDULED o CANCELLED;
before siempre es un snapshot PlannedBlock completo, after es otro snapshot
completo para RESCHEDULED y null para CANCELLED.

Cada PlannedBlock anidado contiene exactamente `id`, `projectId`, `taskId`,
`request`, `time` y `createdAt`. `request` contiene exactamente los siete campos
`objective`, `startLocal`, `endLocal`, `zoneId`, `startOffset`, `endOffset` y
`allowOverBudget`. `time` contiene exactamente los cinco campos `startAt`,
`endAt`, `startOffset`, `endOffset` y `durationMinutes`.

Los offsets de request expresan la intención persistida y pueden ser null;
los de time son offsets resueltos no nulos. Se conservan ambos pares y sus
valores propios: no se completan los null de intención con el resultado, no se
fusionan ni se recalculan usando la zona actual. Cuando un offset de intención
está presente debe coincidir con el correspondiente resuelto. Los offsets se
representan como strings ISO de ZoneOffset, incluido Z; las fechas locales,
instantes, boolean y duración mantienen las reglas de fidelidad de este archivo.
id/projectId/taskId y los snapshots deben corresponder al bloque y sus entidades
propias. Esta estructura conserva la intención y resolución históricas, incluso
allowOverBudget; no utiliza el BlockResponse público de nueve campos, que omite
parte de esa información. No se aplana ni se exporta JSONB abierto.

`receipt` de workSessionChanges conserva los contratos de 15–17: id, sessionId,
action, occurredAt, before y after; sólo CLOSE añade closure y sólo EXTEND añade
extension. Los snapshots State6 y los campos de cierre/expansión mantienen sus
formas originales. Los campos de la fila y del recibo duplicados deben coincidir.
Los enteros largos de esas representaciones también usan el formato decimal
textual definido a continuación. No se exportan campos extra de JSONB desconocidos.

Las colecciones se ordenan por UUID canónico ascendente del id, excepto:
blockProjections por blockId; workSessionIntervals por sessionId y luego revision
numérica ascendente; customization por scope (PROJECT, TASK). Arrays de
availability y appearance tienen cero o un registro; customization de cero a dos.
El orden técnico no pretende ser cronológico ni imponer causalidad entre UUID.
No hay duplicados por identidad ni referencias a entidades fuera del archivo.

### Fidelidad y precisión

Se conservan identificadores, vínculos de subtareas, estados, revisiones,
objetivos, notas, zona/fecha histórica de cierre y todas las preferencias
persistidas. Las claves de petición son identificadores históricos de las
operaciones, no credenciales ni una orden de repetirlas al abrir el archivo.
Los nombres/descripciones/notas conservan Unicode y espacios; el exportador no
vuelve a normalizarlos con reglas de edición. Los textos se escapan como JSON,
sin interpretarlos como HTML, fórmulas, CSS ni contenido ejecutable.

Todos los BIGINT —version, revision, taskVersion, expectedRevision y
workedMicroseconds, incluso en recibos anidados— son strings decimales canónicos
sin signo positivo, espacios, exponente ni ceros iniciales, dentro del rango
no negativo de long. Los campos enteros acotados, schemaVersion y counts siguen
siendo números JSON; NUMBER personal sigue siendo un entero entre -1000000000
y 1000000000. BOOLEAN false, NUMBER 0, texto y null mantienen tipos distintos.

Instantes se escriben en UTC con Z y seis decimales de microsegundo, sin pasar por
Date de JavaScript ni redondear. Fechas civiles mantienen YYYY-MM-DD; horas
locales de planificación mantienen YYYY-MM-DDTHH:mm:ss con segundos cero.
Zonas IANA y offsets persistidos se conservan, sin consultar la TZDB actual para
recalcular instantes o atribución de cierres. Null históricos admitidos por las
migraciones permanecen null, especialmente changedAt, runningSince,
effectiveEndAt y lastDecisionAt de sesiones legadas. No se fabrican recibos para
sesiones antiguas cerradas ni se afirma que ausencia de recibo signifique cero
trabajo conocido.

La reserva original y su proyección se exportan por separado. Ausencia de
proyección significa que sigue vigente la reserva original según 13, no una
fila de proyección nueva. Los intervalos cerrados y runningSince se conservan;
no se añade trabajo hasta exportedAt, no se cierra ni pausa una sesión y no se
altera una tarea. Los informes Hoy/Historial/Revisión semanal no se exportan
como copias derivadas ni se agregan totales de productividad.

Se excluyen tablas Spring Session, contraseñas/hashes, cookies, CSRF, secretos,
variables de entorno, credenciales de integraciones, outbox/publicación/DLQ,
backups operativos y datos de otras identidades. No se inventa historia de
ediciones que no está conservada como dato de negocio fuera de infraestructura.

### Límites, errores y cabeceras

Límites inclusivos propuestos: 100000 registros sumando las catorce colecciones
exteriores y 33554432 bytes (32 MiB) del JSON UTF-8 sin comprimir. Los elementos
anidados cuentan para bytes; los límites existentes de definiciones/valores y
notas siguen vigentes. Se comprueba el límite de registros y se limita la salida
durante serialización; exceder cualquiera produce 413 `EXPORT_TOO_LARGE` sin
JSON parcial. No se recortan registros ni se entrega silenciosamente una página.

Precedencia: autenticación 401 `UNAUTHENTICATED` antes de validar consulta;
los fallos de persistencia de sesión anteriores al caso de uso conservan
503 `SESSION_UNAVAILABLE`, conforme al contrato de acceso existente.
para GET, consulta inválida 400 `INVALID_EXPORT_QUERY`, cuerpo no vacío o Accept
mal formado 400 `INVALID_EXPORT_REQUEST`, negociación no aceptable 406
`EXPORT_FORMAT_NOT_ACCEPTABLE`; después tamaño 413 cuando se acredita su exceso,
o 503 `STORAGE_UNAVAILABLE` ante fallo de lectura, coherencia, reloj o
serialización. Los errores son application/problem+json con códigos y mensajes
españoles, sin nombres/datos privados de filas ni trazas. No se promete precedencia
entre un fallo de almacenamiento y un exceso que todavía no se pudo comprobar.
No hay 204, 206, 304, ETag ni exportación vacía sin envelope: una cuenta sin datos
recibe 200 con catorce arrays vacíos y sus counts cero.

200 usa Content-Type `application/json; charset=utf-8`, Content-Disposition
`attachment` y un nombre fijo seguro generado por servidor:
`organizationweb-export-v1-YYYYMMDDTHHmmssffffffZ.json`, derivado de exportedAt.
Content-Disposition tiene exactamente la disposición attachment y un único
parámetro filename entre comillas con ese nombre ASCII; no hay filename* ni
nombres alternativos. No incorpora usuario, texto libre ni separadores de ruta.
200 incluye Content-Length decimal exacto de los bytes UTF-8 del documento,
mayor que cero y como máximo 33554432. Se entrega sin compresión ni otra
transformación (Content-Encoding ausente o identity); si el cliente excluye
identity mediante Accept-Encoding, responde 406 sin preparar archivo. El camino
API/proxy debe conservar esta representación y su longitud, sin gzip automático
para esta respuesta. Todas las respuestas conservan Cache-Control
`no-store, private, no-transform` y X-Content-Type-Options `nosniff`;
no se crea una URL pública ni se permiten orígenes cruzados nuevos.
Las respuestas de error no llevan Content-Disposition de descarga.

### Recorrido de descarga y privacidad del cliente

Ruta propia `/exportacion`, alcanzable mediante «Exportación» al final de
navegación Principal, sin desplazar Hoy. Encabezado h1 «Exportar mis datos» y ayuda
breve sobre contenidos, JSON versionado, archivo personal e importación futura
no implementada. Abrir la vista no inicia GET. Una acción primaria «Preparar
exportación» obtiene el archivo; mientras tanto anuncia «Preparando exportación…»
antes de 400 ms, sin porcentaje ficticio, e impide solicitudes duplicadas incluso
por doble clic/Enter. «Cancelar preparación» aborta y vuelve al estado inicial.

El cliente sólo ofrece «Descargar archivo JSON» después de recibir íntegramente
el cuerpo, respetar el límite y validar Content-Type, envelope/version/owner,
colecciones y counts coherentes. Antes de consumirlo valida Content-Length,
codificación y nombre seguros del contrato. Lee los bytes con un contador
acotado; si exceden la longitud declarada o el límite, aborta. Al acabar exige
igualdad exacta entre bytes recibidos y Content-Length, UTF-8 válido y JSON
completo. Falta de longitud, discrepancia, stream interrumpido o vacío se trata
como fallo: no crea Blob descargable ni enlace, aunque el estado HTTP fuera 200.
No reconstruye ni redondea el contenido para
crear el Blob. El servidor valida todos los registros; el cliente valida el
formato de transporte y no reinterpreta cada dato de negocio para renderizarlo.
Una respuesta incompatible no produce enlace ni descarga.

El enlace nativo con download requiere el gesto deliberado final del usuario,
con nombre seguro del contrato. Se anuncia «Archivo preparado»; no se afirma que
el navegador lo guardó en disco. Puede volver a pulsar el enlace mientras siga
en la vista sin hacer otro GET. «Preparar de nuevo» retira el archivo anterior y
consulta un snapshot nuevo. Error de red/503 ofrece reintento manual; 413 explica
el límite sin recomendar pulsar indefinidamente ni ofrecer una exportación parcial.

El estado de preparación/archivo es local a esta sesión y vista, sin persistir
bytes en localStorage, IndexedDB, caché de aplicación ni logs. Cancelar, abandonar
la ruta, logout o cambio de identidad aborta la petición e invalida su generación;
revoca ObjectURL y referencias al archivo. Una respuesta HTTP/JSON/Blob tardía se
descarta antes de notificar un 401 a otra sesión o de crear un enlace/descarga.
Un 401 vigente conserva el flujo de retirada de acceso existente. El archivo
que el navegador ya descargó queda bajo control del usuario, no se promete borrarlo.

Se reutilizan RouteLink, apiRequest, identidad y estilos/tokens actuales. No hay
Provider global nuevo ni alteración de borradores de proyectos, tareas o campos.
No se añaden mutaciones ni resets de estado compartido a los efectos propios del
ciclo de navegación existente; no se exige persistir globalmente borradores
locales de rutas desmontadas.
Controles nativos, foco visible, nombres accesibles y anuncios de estado/error;
restaurar foco lógico sólo cuando desaparece el iniciador y el usuario no lo movió.
Mantener 44px, reflow a320, texto/zoom200, temas, forced-colors y movimiento
reducido. La matriz de30 principios UX se revisará con evidencias y límites,
sin atribuir ahora pruebas, rendimiento o conformidad universal.

## 23. Importación privada de datos propios (`import_data`)

Contrato aprobado por root bajo la autorización global, con revisión técnica independiente ratificada, para destilación Gherkin. Incorpora el JSON v1 de exportación 22 mediante fusión conservadora integral: insertar ausentes, conservar registros durables idénticos y rechazar toda la operación si existe algún conflicto. No hay sobrescritura, reemplazo, selección parcial ni remapeo. Owner coincide exactamente con el principal autenticado; datos actuales no contenidos en la copia permanecen intactos.

Sirve en una cuenta ya usada y también vacía. Una copia antigua no revierte cambios actuales. Reemplazo destructivo, fusión por campos y remapeo entre cuentas quedan fuera de 23. La vista previa no escribe ni reserva la confirmación. La aprobación del contrato no acredita implementación ni aceptación y no inicia TDD antes de revisar Gherkin.
### Archivo y validación

Único formato: el JSON UTF-8 `organizationweb-export`, `schemaVersion: 1`, definido por 22. Se conservan sus catorce colecciones, campos cerrados, recibos anidados completos, valores inactivos, nulos legados, precisión de microsegundos y BIGINT decimales textuales. No CSV, ZIP, importador genérico, conectores ni reinterpretación de proyecciones HTTP incompletas.

Límites inclusivos: 33.554.432 bytes y 100.000 registros exteriores, iguales a exportación. El cliente comprueba `File.size` antes de enviar; el servidor cuenta bytes realmente recibidos y corta al superar el límite, aunque no haya Content-Length. No se confía en el nombre, extensión o tipo declarado por el archivo. Se rechazan UTF-8 inválido, BOM, contenido posterior al único JSON, claves duplicadas, campos desconocidos, formato/versión distintos y counts discordantes.

Profundidad máxima: **16 contenedores JSON**, contando el objeto raíz como 1. El v1 es cerrado: el recorrido de recibo de bloque `data → blockChanges[] → receipt → before → request/time` y los recibos de sesión son de profundidad fija; no existen valores personales objeto/array arbitrarios. El margen hasta 16 admite ese esquema sin permitir anidamiento no acotado. Se contrastaron WorkSessionTransitionReceipt, WorkSessionClosure y WorkSessionExtension del checkout limpio: el máximo estructural validado es siete: raíz/data/array/registro/receipt/before o after/session para sesiones, o request/time para bloques; closure/extension quedan en seis. No existe recursión. El límite16 queda por encima de esos caminos válidos y no abre campos nuevos.

No materializar un árbol completo de 100.000 registros antes de validar el tamaño. Lectura incremental con contador y parser acotado; validación por registro y preparación privada en almacenamiento temporal de PostgreSQL o presupuesto equivalente demostrado. La preparación admite comprobaciones de identidad y relaciones sin mantener todos los registros completos en el heap. Se descarta al terminar o fallar. No se almacena el archivo como respaldo, no se publica y no se registra su contenido en logs.

La validación de importación es más fuerte que reconocer el envelope de exportación: comprueba referencias, identidades, claves alternativas, enums, límites tipados, padres sin ciclos y del mismo proyecto, coherencia entre recibos y sus filas, proyecciones y revisiones, intervalos y acumulados según las reglas durables existentes. Las catorce colecciones deben ser autocontenidas conforme a 22; no completar referencias ausentes usando casualmente datos del destino.

La rehidratación conserva hechos históricos. No vuelve a ejecutar comandos de creación, no exige que un hecho antiguo ocurra hoy, no recalcula offsets históricos con la TZDB actual y no inventa cierres ni tiempo conocido para datos legados. A la vez, el conjunto resultante debe cumplir las restricciones vigentes de integridad: cuota de proyectos activos, como máximo una sesión abierta por propietario, unicidad y referencias. No se introduce una prohibición nueva de solapamientos históricos que el dominio actual no imponga.

### Igualdad, conflictos e identidad

La igualdad es por **registro durable completo y tipado**, no por bytes JSON: el orden de las propiedades o los espacios del documento no hacen diferentes dos registros. Se admite cualquier orden de los registros exteriores y de los valores indexados por identidad; el orden de serialización de exportación no es una precondición de importación. Los duplicados siguen rechazándose. Sí importan las cadenas de negocio exactas, nulos, versiones, fechas, todos los campos de recibos y el orden de las listas cuya semántica es ordenada (`visibleFields` y definiciones). No se normalizan nombres ni notas. Los valores personales se comparan por fieldId y valor tipado, preservando ausencia frente a null, 0 y false.

Se comprueban tanto UUID como claves únicas alternativas del esquema: propietario/ámbito en preferencias, propietario/requestKey, tarea/requestKey y bloque/versión, además de las restantes restricciones publicadas. Dos registros con UUID distintos y la misma clave alternativa no son una oportunidad de remapeo: son conflicto. Una colisión con datos de otro propietario sólo produce el error genérico; no expone su existencia detallada, nombres, IDs adicionales ni valores.

Availability, appearance y cada ámbito de customization son recursos completos. Si existe el recurso, debe coincidir también su identidad y revisión; no se mezclan arrays de definiciones ni se sustituyen preferencias actuales por las del archivo. Las definiciones inactivas y sus valores se conservan. Los datos actuales no mencionados por la copia permanecen intactos.

Una sesión importada conserva status, runningSince, intervalos y demás tiempos originales. Si está RUNNING, la vista previa advierte que continuará figurando en curso desde el instante histórico y que el cómputo actual puede incluir el tiempo transcurrido desde entonces. No se pausa silenciosamente ni se transforma ese periodo en intervalos inventados. Si el destino ya tiene otra sesión abierta, el conjunto es incompatible y no se escribe nada.

### Protocolo

Todas las rutas son privadas, autenticadas, sin caché. Los POST siguen CSRF y comprobación de origen ya existentes. No se modifica el flujo global de autenticación. Los dos POST reciben el archivo directamente como JSON, sin multipart ni un envelope que obligue a volver a serializarlo. No admiten query. Se acepta Content-Type application/json con charset ausente o UTF-8 sin distinguir mayúsculas; otra codificación o Content-Encoding distinto de identity se rechaza con 415 según el flujo HTTP existente. No hay negociación nueva de Accept. Sólo los métodos indicados; HEAD no sustituye POST ni devuelve una vista previa. Métodos no admitidos siguen 405 del framework. El GET de recibo no admite query ni cuerpo (400 IMPORT_INVALID_REQUEST).

El proxy requiere dos locations exactas, `location = /api/v1/me/import/preview` y `location = /api/v1/me/import`, para los POST de vista previa y aplicación: `client_max_body_size 0`, `proxy_request_buffering off` y `proxy_http_version 1.1` explícitos. Las rutas exactas siguen delegando autenticación y métodos a la API. Son locations hermanas de `/api/`, no hijas: deben conservar explícitamente en ambas las directivas actuales `proxy_set_header`, `proxy_read_timeout`, `proxy_next_upstream`, `proxy_pass`, resolución DNS y demás parámetros del proxy; no heredan esos valores de la location hermana. No se introduce un refactor global. No se aplica la excepción a prefijos, al GET de recibos ni a otras rutas. El límite efectivo de 33.554.432 bytes lo impone la API autenticada contando el cuerpo antes de materializarlo, también con transferencia chunked sin Content-Length. El cero de Nginx elimina su límite anticipado sólo allí; no elimina el límite de la aplicación.

La configuración actual hereda 1 MiB y buffering de petición, que no permiten prometer el archivo de 32 MiB sobre el cache tmpfs de 16 MiB. Desactivar buffering evita que el proxy necesite almacenar el cuerpo completo antes de pasarlo al backend; no se promete ausencia de todo buffer de transporte. Se preservan timeout de 15 s, `proxy_next_upstream` desactivado, cabeceras, DNS y resto de parámetros existentes. Las otras rutas mantienen su límite actual de 1 MiB. Referencias: [client_max_body_size](https://nginx.org/en/docs/http/ngx_http_core_module.html#client_max_body_size) y [proxy_request_buffering](https://nginx.org/en/docs/http/ngx_http_proxy_module.html#proxy_request_buffering).

La aceptación local debe atravesar el Nginx real: archivo válido de 32 MiB aceptado, 32 MiB + 1 byte rechazado por API con 413, y ambas fronteras con cuerpo chunked sin Content-Length; verificar además que otra ruta conserva el límite previo. Seguridad se decide antes de consumir el archivo mediante la aplicación, sin un 413 anticipado de tamaño del proxy en esas dos rutas. No se elevan timeouts ni se modifica proxy fuera de ese alcance para obtener verde.
#### Vista previa

`POST /api/v1/me/import/preview` valida y compara una instantánea consistente del destino. No escribe datos de negocio ni reserva recursos para la futura confirmación.

200 cerrado:

```text
{ format: "organizationweb-import-preview", schemaVersion: 1,
  fileSha256, byteLength, owner, exportedAt,
  counts, insertCounts, identicalCounts,
  runningSessions: [{ sessionId, runningSince }] }
```

Los tres objetos de counts tienen exactamente las catorce claves de 22, con enteros no negativos, y por colección `counts = insertCounts + identicalCounts`. `fileSha256` son 64 caracteres hexadecimales minúsculos sobre los bytes originales; `byteLength` es el tamaño exacto. `runningSessions` contiene sólo las sesiones RUNNING que se van a insertar, nunca las idénticas ya presentes. Tiene cero o un elemento, pues antes se valida que el conjunto resultante tenga como máximo una sesión abierta; cada elemento tiene exactamente sessionId y runningSince. `runningSince` conserva el nullable legado permitido, sin inventar una fecha. Un conflicto produce 409, no una vista previa confirmable. Esta primera versión anuncia el conflicto integral sin ofrecer resolución parcial ni un catálogo de valores actuales.

#### Confirmación

`POST /api/v1/me/import` vuelve a recibir **los mismos bytes** y requiere `Idempotency-Key` UUID canónico y `X-Import-Content-SHA256` igual al hash mostrado. El cliente conserva el File y el hash de su propia vista previa; cambiar de archivo retira esa vista y su confirmación. El servidor comprueba el hash, revalida el formato y vuelve a comparar el estado vigente. El hash vincula archivo e intención; no convierte la vista previa en autorización ni evita la validación completa.

200 cerrado, tanto primera confirmación como repetición resuelta:

```text
{ requestKey, fileSha256, byteLength, recordedAt,
  outcome: "IMPORTED" | "NO_CHANGE", insertedCounts, identicalCounts }
```

`recordedAt` es el instante capturado del reloj inyectado al construir el recibo dentro de la transacción, antes del commit; no afirma medir el instante del commit. Se representa en UTC con seis cifras de microsegundos; los counts tienen las catorce claves. IMPORTED exige al menos una fila insertada; NO_CHANGE significa todas las filas idénticas y ninguna inserción. Los counts de confirmación son los reales: otra operación pudo añadir filas idénticas después de la vista previa. Cualquier diferencia incompatible nueva causa 409 y rollback completo.

`GET /api/v1/me/imports/by-key/{requestKey}` obtiene ese mismo recibo para el propietario autenticado, o 404 genérico. Permite resolver una respuesta perdida sin reenviar automáticamente la importación. Un 404 mientras una petición anterior sigue en curso no demuestra que nunca vaya a confirmar; la UI conserva la incertidumbre y sólo permite una recuperación deliberada con la misma clave y los mismos bytes.

El recibo de importación es metadato operativo de idempotencia, no una decimoquinta colección de JSON v1. Restricción única `(owner, requestKey)`: misma clave y mismos bytes devuelve el recibo original aunque después cambie el workspace; misma clave con archivo distinto devuelve 409. Su persistencia es atómica con las inserciones. Nunca se genera una clave nueva para ocultar una confirmación incierta.

#### Errores y prioridad

Se reutiliza `application/problem+json`; título/detalle seguros en español y código estable, sin contenido del archivo ni información ajena. Códigos propios:

| HTTP | Código | Significado |
| --- | --- | --- |
| 400 | IMPORT_INVALID_FILE | Formato, estructura, tipos, integridad interna o propietario incompatible |
| 400 | IMPORT_INVALID_REQUEST | Cabeceras de confirmación o requestKey mal formados |
| 413 | IMPORT_TOO_LARGE | Bytes o número de registros supera el límite |
| 409 | IMPORT_CONFLICT | El conjunto completo no se puede incorporar al destino |
| 409 | IMPORT_KEY_REUSED | Clave propia ya confirmada con otros bytes |
| 412 | IMPORT_FILE_CHANGED | Hash declarado distinto de los bytes recibidos |
| 404 | IMPORT_NOT_FOUND | No hay recibo accesible por esa clave |
| 503 | STORAGE_UNAVAILABLE | Persistencia, bloqueo agotado, deadlock o fallo recuperable de preparación/aplicación |

La seguridad conserva sus respuestas actuales: 401 UNAUTHENTICATED, CSRF/origen y métodos/tipos no admitidos según los filtros existentes; fallo previo de persistencia de sesión 503 SESSION_UNAVAILABLE. No se homogeneizan esas respuestas globales.

Prioridad de la aplicación: seguridad existente; método y tipo de contenido; query/cuerpo no admitidos y sintaxis de ruta/cabeceras; lectura acotada y JSON; hash de confirmación; validación completa interna/owner; recibo idempotente; comparación y restricciones del destino; escritura. Si la lectura alcanza un límite antes de poder terminar la validación, devuelve 413 sin afirmar que el resto era válido. Un recibo confirmado no se devuelve ante archivo inválido o hash discordante. Los fallos de almacenamiento se mantienen 503, no se presentan como conflictos de usuario.

### Atomicidad y concurrencia real

La vista previa es una lectura consistente, no vinculante. La aplicación prepara y valida el archivo **antes** de retener bloqueos de negocio. En una transacción READ COMMITTED activa primero `lock_timeout=2s` y `statement_timeout=10s`. Antes de cualquier lock de tabla adquiere los mismos advisory locks transaccionales que usan `PostgresCustomizationStore.change` y `changeValues`: `pg_advisory_xact_lock(hashtextextended('customization:' + owner + ':PROJECT', 0))` y después el equivalente `:TASK`, siempre PROJECT antes de TASK. La concatenación descrita se pasa como parámetro SQL, no mediante SQL interpolado. No es un lock privado entre importadores: coordina con los escritores existentes de configuración y valores del mismo propietario. Después adquiere bloqueos de tabla `EXCLUSIVE`, en una única lista fija de las catorce tablas durables y el recibo operativo, y sólo entonces lee el destino para el plan definitivo. Orden fijo de adquisición: appearance_preferences, availability_preferences, block_changes, block_projections, customization_preferences, import_receipts (nueva tabla operativa), planned_blocks, project_custom_field_values, projects, task_custom_field_values, task_status_history, tasks, work_session_changes, work_session_intervals, work_sessions. Este orden de locks no es el orden de inserción por dependencias. Las inserciones se ordenan por dependencias y el recibo confirma en esa misma transacción.

EXCLUSIVE bloquea las escrituras ordinarias y los SELECT FOR UPDATE de writers existentes; permite SELECT simple. Por eso los locks de tabla solos son insuficientes para customization: un escritor puede haber leído ausencia o versión, esperar en su UPSERT y sobrescribir después con una validación anterior a la importación. Los dos advisory locks compartidos cierran esa ventana sin modificar a todos los escritores. La elección se apoya en los modos de bloqueo documentados por [PostgreSQL](https://www.postgresql.org/docs/current/explicit-locking.html). La lista usa las tablas físicas del esquema publicado; no se bloquean sesiones de autenticación ni outbox para leer el archivo.

La transacción de aplicación configura `lock_timeout=2s` y `statement_timeout=10s`, sin reintento automático. lock_timeout se aplica a cada adquisición y statement_timeout a cada sentencia, no a toda la transacción. La lista se adquiere tabla por tabla en ese orden, no de forma atómica; los locks parciales se liberan al abortar. No son un SLA global de reloj: varias sentencias y la transmisión tienen su propio tiempo. Un aborto debe hacer rollback y liberar recursos. Nginx sólo recibe las excepciones de tamaño y buffering descritas para las dos rutas; no se eleva su timeout de 15 segundos; una respuesta de red perdida o agotada puede ser incierta y se resuelve mediante el recibo.

El coste explícito es detener brevemente escrituras de otros propietarios en esas tablas. No se oculta esa limitación ni se afirma aislamiento por owner. Las transacciones existentes pueden tener un orden distinto: un deadlock sigue siendo posible, pero debe acabar en aborto recuperable sin datos parciales. Las pruebas futuras deben enfrentar importación con writers reales de proyectos/tareas, reservas, sesiones y preferencias, no sólo con otro importador. No basta con demostrar un advisory lock entre imports. Se exigen dos carreras concretas de personalización, con barreras observables: (1) un escritor de configuración o valores ya leyó ausencia/versión y mantiene su advisory lock cuando empieza importación; ésta espera, o aborta 503 íntegramente al agotar el bloqueo, y tras adquirirlo compara el estado ya confirmado, devolviendo 409 integral si es incompatible; (2) importación mantiene los locks compartidos y se inicia un escritor, que no puede leer/validar para luego hacer UPSERT hasta su liberación. Tras la espera el escritor revalida el estado vigente conforme a su contrato. Ningún resultado puede perder un cambio confirmado, sobrescribir con una lectura obsoleta ni dejar recibo o filas parciales.

Un rechazo comprobado de la transacción o de COMMIT, por ejemplo por una constraint diferida, exige rollback completo: no quedan filas parciales, recibos falsos ni valores personales huérfanos. Perder la respuesta de COMMIT deja su resultado indeterminado; no permite afirmar rollback ni ausencia de escrituras. En ese caso se devuelve 503 si todavía es posible responder, o queda una respuesta de red incierta, y se recupera el resultado mediante el recibo de la misma intención. La desconexión del navegador después de enviar tampoco equivale a rollback: puede haber commit y respuesta perdida.

### Eventos y publicación

No se borran, reescriben ni republican eventos de outbox existentes. Los registros importados conservan sus recibos históricos, pero no se ejecutan de nuevo ProjectCreated, BlockPlanned o cambios de sesión por cada fila. La justificación es semántica, **no una FK vigente**: la migración V14 publicada retiró aquella FK de outbox hacia projects.

Decisión de 23: la restauración duradera se acredita por el recibo de importación y **no produce eventos de negocio históricos ni un nuevo tipo de evento de publicación**. No existen consumidores actuales que deban reconstruirse mediante esa republicación; las vistas leen persistencia. No se añade un evento nuevo durante implementación.

### Recorrido y privacidad de la interfaz

Ruta `/importacion`, encabezado «Importar mis datos», navegación después de Exportación sin desplazar Hoy. Control nativo de archivo y botón «Validar archivo»; seleccionar no envía nada. Ayuda: JSON v1 propio, copia privada, incorporación sin sobrescribir, incompatibilidades impiden toda la operación.

La vista previa muestra propietario, fecha original de exportación, tamaño, cantidades por colección que se añadirán y que ya coinciden, y aviso explícito de sesiones en curso. No presenta reservas como trabajo realizado ni suma logros históricos. «Confirmar importación» es un segundo gesto explícito, con consecuencia visible; cancelar antes de confirmar descarta la preparación local sin escribir.

Durante confirmación no se edita la intención ni se dispara otra petición. Ante respuesta incierta, conservar clave/hash e indicar «Comprobar resultado»; no asegurar que Cancelar o cerrar la pestaña deshace una petición enviada. No guardar bytes, contenido, nombres del archivo o datos privados en localStorage, URLs, logs o analítica. Para recuperar después de recargar la pestaña se guarda únicamente `{ owner, requestKey, fileSha256 }` en una entrada propia de sessionStorage `organizationweb.import.pending.v1`, justo antes de enviar la confirmación. No contiene archivo, nombre, bytes ni vista previa. Se valida su forma al leer y se elimina si es inválida, no coincide con la sesión, cambia la identidad, hay logout o se obtiene un recibo confirmado. Se conserva ante incertidumbre y 404; no permite reenviar sin volver a seleccionar bytes con el mismo hash. No hay localStorage, GET global ni proveedor nuevo. Si sessionStorage no está disponible, no se envía la confirmación: se explica el requisito de recuperación sin degradarlo silenciosamente.

Cambio de sesión o salida retira datos visibles y aborta lectura, con guardas de generación antes y después de awaits: un 401 o JSON tardío de otra identidad no altera la sesión nueva. Después de commit se refrescan deliberadamente snapshots afectados para que los datos importados aparezcan; no se reinician borradores ajenos ni se aplican preferencias en una vista previa.

Controles de al menos 44 px, teclado, foco visible y anuncios de progreso/error/resultado conforme a UX existente. Retorno de foco cuando desaparezca el iniciador, sin robarlo si el usuario lo movió; comprobar el blur real de controles disabled en navegador. Estados preparados y errores no dependen sólo del color. Reutilizar SCSS, controles nativos y apiRequest; ninguna dependencia nueva.

### Aceptación y límites

Toda confirmación de aceptación se ejecutará exclusivamente en PostgreSQL y cuentas locales efímeras. En live sólo se autoriza vista previa sin escrituras; no se borran datos productivos para preparar una importación. La aceptación debe acreditar copia propia útil, no-op, conflicto integral, colisiones privadas, recibos e intervalos completos, sesión RUNNING preservada con aviso, límites de recursos, carrera con writer existente y commit con respuesta perdida.

Root ha aprobado la lista física y el orden de locks, respuestas y recuperación, cero evento nuevo, recordedAt previo al commit y límites por bloqueo/sentencia sin SLA global nuevo. C ha ratificado coordinación con los writers existentes y el límite del proxy. La destilación deberá conservar estas decisiones; todavía no hay implementación validada de 23.

## 24. API para integraciones

Contrato aprobado por root bajo la autorización global del usuario, preparado aisladamente desde `99cd366` mientras 23 completa sus gates. No implica implementación ni cierre de 23. La [propuesta aprobada](progress/proposal_integration_api.md), SHA256 `B948AB34566EA6CE1EED299B5CD7DF1DF42B3E22C56E13EA38F680A25CAE349B`, se incorpora como detalle normativo de este contrato; sus expresiones «propuesta»/«para revisión» identifican su origen histórico, no decisiones pendientes. [Revisión independiente](progress/review_integration_api_proposal.md), SHA256 `ACC931B5` abreviado, APPROVED. Destilación Gherkin antes de TDD.

### Superficie y seguridad

Credenciales personales creadas por sesión humana; consumidor Bearer stateless, sin OAuth ni nuevos proveedores. Se reutilizan las rutas y casos de uso existentes, con allowlist exacta por método/ruta:

| Scope | Operaciones |
| --- | --- |
| `projects:read` | GET `/api/v1/projects` y `/api/v1/projects/{id}` |
| `projects:write` | POST `/api/v1/projects`; PUT `/api/v1/projects/{id}` |
| `tasks:read` | GET `/api/v1/projects/{projectId}/tasks`, `.../tasks/{taskId}`, `.../tasks/{id}/status`, `.../tasks/{id}/parent`, `.../tasks/{parentId}/subtasks` |
| `tasks:write` | POST `/api/v1/projects/{projectId}/tasks` |
| `agenda:read` | GET `/api/v1/today`; GET `/api/v1/projects/{projectId}/tasks/{taskId}/blocks`, `.../blocks/{blockId}`, `.../blocks/{blockId}/state`, `.../blocks/by-request/{requestKey}` |
| `history:read` | GET `/api/v1/history`, `/api/v1/weekly-review`, `/api/v1/projects/{projectId}/tasks/{id}/history` |

Los segmentos `...` de la tabla conservan el prefijo explícito de su fila; no son matchers ni permisos comodín. Escritura no concede lectura. Se preservan cuerpos, paginación, errores, ETags, precondiciones, ownership y outbox existentes. El token no permite otras operaciones, gestión de sesión/credenciales, preferencias, export/import ni transiciones de estado. HEAD/OPTIONS no heredan GET.

Cualquier Authorization selecciona el canal Bearer: no fallback a cookie. Header único, esquema Bearer case-insensitive, un espacio y token canónico; sin query alternativa, comas ni valores duplicados. Precedencia: formato/autenticación 401 `API_UNAUTHENTICATED` con `WWW-Authenticate: Bearer`; después Origin ajeno explícito en método distinto de GET/HEAD/OPTIONS 403 `UNTRUSTED_ORIGIN`; después ruta/scope 403 `API_SCOPE_DENIED`; después cuota 429; después negocio. Credencial inválida/caducada/revocada o de owner distinto del bootstrap actualmente habilitado produce el mismo 401 genérico.

Principal conserva owner de la credencial, nunca su id. El canal Bearer no consulta/crea sesión JDBC ni emite Set-Cookie, ignora cookie inválida y no depende de disponibilidad del almacenamiento de sesión. No se prescribe refactor de filtros globales si configuración estándar cumple esos observables. Authorization ausente conserva cadena cookie, CSRF, OriginGuard y precedencia actuales. No habilitar CORS.

### Credenciales, gestión e incertidumbre

Token `owp_<uuid>.<secret>`: UUID canónico minúsculo, secret de 32 bytes SecureRandom base64url sin padding; persistir sólo SHA-256 de esos bytes y comparación constante. Sin secretos/verificadores en logs, listados, export v1/import23 o almacenamiento cliente. Gestión y errores de autenticación llevan no-store.

Metadatos cerrados `{id,name,scopes,createdAt,expiresAt,revokedAt}`. Nombre tras strip Unicode 1–80 puntos de código sin controles; duplicados de nombre permitidos. Scopes 1–6 conocidos sin repetición, orden canónico de tabla. Caducidad elegida 7/30/90 días UTC desde creación, default UI 30; reloj inyectado, microsegundos, años 0001–9999. La representación UTC admite fracción omitida o hasta seis decimales; precisión de microsegundos no exige seis dígitos impresos. La primera revocación conserva el instante válido del reloj incluso si éste retrocedió respecto a createdAt; el replay no modifica esa fecha. Válida sólo si now < expiresAt. No editar permisos ni renovar: sustitución explícita. Máximo 10 válidas por owner, conteo e inserción serializados; caducadas/revocadas liberan cupo. Historial paginado conservado.

Gestión sólo cookie en `/api/v1/me/api-credentials`:

- PUT `/{id}` con JSON cerrado `{name,scopes,expiresInDays}`: primera creación 201 y Location, `{credential,secret}` sólo tras commit. Id elegida antes del envío identifica intento. Replay misma intención/id/owner 200 `{credential,secret:null}`, sin regenerar ni extender fechas ni exigir otro cupo. Intención diferente o colisión ajena 409 `API_CREDENTIAL_CONFLICT`, sin revelar owner.
- GET `/{id}`: credential o 404 `API_CREDENTIAL_NOT_FOUND`, ajena igual a ausente. GET colección: `{items,nextCursor}`, 50 por página, createdAt DESC/id DESC, sin N+1 ni escrituras al leer.
- PUT `/{id}/revocation`, sin cuerpo/query: 200 credential; primera revocación fija fecha y repeticiones la conservan, irreversible. Ajena/ausente 404; no ETag necesario para esta transición única.

JSON cerrado, duplicados/trailing tokens inválidos, límite 4 KiB antes de materializar. 400 `API_CREDENTIAL_INVALID`, 409 `API_CREDENTIAL_LIMIT`, 413 `API_CREDENTIAL_TOO_LARGE`, 503 `STORAGE_UNAVAILABLE`; sesión conserva sus errores actuales. Prioridad de gestión: seguridad, método/ruta, tamaño/estructura/valores, idempotencia, cupo, escritura. Reloj inválido 503 sin inserción. COMMIT incierto no garantiza rollback: recuperación por id.

UI `/integraciones/api`, nav al final y ruta privada reconocida, H1 «Credenciales para integraciones». Nombre/scopes/caducidad, lista y revocación explícita con consecuencia. Antes de crear guarda sólo `{owner,id}` en sessionStorage propio; si falla, no envía. Formulario y secreto sólo en memoria. Secreto visible una vez, selección manual y Copiar sin clipboard automático; ocultar al abandonar/desmontar/cambiar owner/logout. Ninguna llegada tardía lo publica a otra identidad.

Ante respuesta perdida, bloquear otra creación y «Comprobar creación» manual de misma id. Encontrada sin secreto: informar pérdida irrecuperable y permitir revocar/crear otra explícitamente. 404 no libera id: tras reload verificar/revocar o reintroducir misma intención y reenviar manualmente con la misma id; replay siempre secret:null. No id nueva antes de resolver. Intención se retira al confirmar resolución; fallos al limpiar almacenamiento no invalidan éxito ni impiden logout. Revocación incierta se comprueba con GET manual y sólo se reenvía deliberadamente. Reutilizar guardas identidad/abort/foco, conservar otros borradores y evitar Provider/GET global.

### Cuotas, documentación y aceptación

Cuota compartida PostgreSQL, ventana fija UTC de un minuto: 60 por credencial y 120 por owner. Contadores compactos de ventana actual, comprobación/incremento atómicos en orden owner antes de credencial, sin gasto parcial al rechazar. Sólo tokens válidos y operaciones admitidas consumen; errores posteriores de negocio sí. 429 `API_RATE_LIMITED`, Retry-After entero hasta próxima ventana mínimo 1; DB inaccesible 503 sin bypass. No limita sesiones humanas ni promete defensa volumétrica anónima.

Revocación serializada con autorización/cuota: una solicitud autorizada antes de su commit puede terminar después; toda autenticación posterior rechaza, sin caché permisiva. Caducidad se verifica al autorizar. No modificar idempotencia de POST de negocio ni reintentar automáticamente altas ante respuesta perdida.

GET exacto `/api/v1/integration-openapi.json`: OpenAPI 3.1 versionado/validado, disponible a sesión propia o cualquier Bearer válido, sin scope adicional ni cuota; Bearer sigue sin sesión/Set-Cookie. Sólo allowlist documentada, seguridad y contratos existentes; ejemplos ficticios, sin Swagger UI/dependencia nueva. Excepción exacta, no apertura de prefijos.

Pruebas se centran en fronteras de seguridad, concurrencia, secreto único, recuperación/privacidad y cuotas, reutilizando oráculos de negocio existentes sin replicarlos. UI conserva tokens/temas, 44px, teclado/foco, contraste, reduced motion y matriz UX30 con evidencia en tres motores. Escrituras de aceptación en entorno efímero; LIVE sólo autorización explícita. Nuevas tablas fuera de export/import, migración y rollback aditivo comprobados antes de despliegue.

## Feature 25: webhooks — Webhooks salientes firmados

Propuesta normativa preparada el 8 de septiembre de 2026 bajo la autorización global del 5 de septiembre, sin el humano disponible. Sección lista para integrarse en `project-spec.md`; no acredita implementación ni aceptación. La destilación Gherkin y su revisión preceden a TDD. Depende de la feature 24 sólo en convenciones (secreto de un solo uso, JSON cerrado, no-store); no requiere sus credenciales Bearer.

### Propósito y frontera

Entregar a URLs https elegidas por el propietario, con firma verificable y reintentos acotados, los eventos de negocio que la outbox ya conserva, y dejar un registro consultable de cada entrega. Fuente única: `outbox_events`; no se crean eventos nuevos ni se modifican los adaptadores de commit ni el publicador RabbitMQ. Tipos suscribibles: exactamente los doce que acepta `OutboxMessage.validationCode()` con su nombre literal (`ProjectCreated.v1`, `ProjectUpdated.v1`, `ProjectStatusChanged.v1`, `TaskCreated.v1`, `SubtaskCreated.v1`, `TaskStatusChanged.v1`, `BlockPlanned.v1`, `BlockChanged.v1`, `WorkSessionStarted.v1`, `WorkSessionStateChanged.v1`, `WorkSessionExtended.v1`, `WorkSessionClosed.v1`). `webhook.ping.v1` es un evento sintético de prueba que sólo se emite por acción explícita y no es suscribible. Fuera de alcance: transformación de cuerpos, cabeceras personalizadas, mTLS, filtros por proyecto, edición de URL, rotación de secreto, entrega desde el canal Bearer, inclusión en export/import (22/23) y cualquier conector de terceros (26–29).

### Modelo y persistencia (migración reservada V23__webhooks.sql)

`webhook_endpoints`: `id UUID PK`, `owner_id TEXT NOT NULL`, `url TEXT NOT NULL`, `description TEXT NOT NULL` (0–80 puntos de código), `event_types TEXT[] NOT NULL CHECK (cardinality(event_types) BETWEEN 1 AND 12)`, `secret_ciphertext BYTEA NOT NULL` (nonce de 12 bytes || AES-256-GCM || tag), `status TEXT NOT NULL CHECK (status IN ('active','disabled'))`, `disabled_reason TEXT CHECK (disabled_reason IN ('MANUAL','DELIVERY_EXHAUSTED'))`, `disabled_at TIMESTAMPTZ`, `cursor_occurred_at TIMESTAMPTZ NOT NULL`, `cursor_event_id UUID NOT NULL`, `created_at`, `updated_at TIMESTAMPTZ NOT NULL`. Índice `(owner_id, created_at DESC, id DESC)`.

`webhook_deliveries`: `id UUID PK`, `endpoint_id UUID NOT NULL REFERENCES webhook_endpoints(id) ON DELETE CASCADE`, `owner_id TEXT NOT NULL`, `event_id UUID NOT NULL`, `event_type TEXT NOT NULL`, `body TEXT NOT NULL` (bytes exactos que se envían en todos los intentos), `status TEXT NOT NULL CHECK (status IN ('pending','succeeded','exhausted'))`, `attempt INTEGER NOT NULL CHECK (attempt BETWEEN 0 AND 6)`, `http_status INTEGER`, `latency_ms INTEGER CHECK (latency_ms >= 0)`, `error_class TEXT`, `next_attempt_at TIMESTAMPTZ`, `created_at`, `updated_at TIMESTAMPTZ NOT NULL`. Índices `(endpoint_id, updated_at DESC, id DESC)` y parcial `(next_attempt_at) WHERE status = 'pending'`. Ninguna columna guarda secreto, firma ni cuerpo de respuesta.

Sobre la tabla existente sólo se añade `CREATE INDEX outbox_events_owner_cursor ON outbox_events (owner_id, occurred_at, event_id)`; no se alteran columnas ni filas de la outbox. Migración aditiva; rollback documentado como `DROP` de las dos tablas y del índice.

### API (`/api/v1/me/webhooks`, sólo sesión cookie)

Todas las rutas son privadas, con CSRF, OriginGuard y `Cache-Control: no-store` como en 22–24. El canal Bearer de 24 no las incluye en su allowlist. JSON cerrado, propiedades desconocidas o duplicadas y tokens finales inválidos; cuerpo máximo 4096 bytes comprobado antes de materializar. El propietario procede del Principal. Cinco endpoints como máximo por propietario, contando activos y desactivados; eliminar libera plaza.

- `POST /` con `{url, description, eventTypes}` → 201, `Location`, `{endpoint, secret}` sólo tras commit. `url`: string de 1–2048 puntos de código, esquema `https` en minúsculas, host presente, sin userinfo ni fragmento, puerto explícito opcional 1–65535. `description`: opcional o null → cadena vacía; tras strip Unicode 0–80 puntos de código sin caracteres de control. `eventTypes`: 1–12 nombres del catálogo, sin repeticiones; se guardan en el orden canónico del catálogo. El cursor inicial es `(now, 00000000-0000-0000-0000-000000000000)`: sólo se entregan eventos posteriores a la creación. `secret` tiene formato `whsec_<base64url sin padding de 32 bytes SecureRandom>` y no vuelve a mostrarse.
- `GET /` → `{items}` ordenado `createdAt DESC, id DESC`, sin paginación (máximo 5). `GET /{id}` → `endpoint`; ajeno o inexistente 404.
- `PUT /{id}/status` con `{status: "active" | "disabled"}` → 200 `endpoint`. Idempotente: repetir el mismo estado conserva `disabledAt` y `disabledReason`. Desactivar manualmente fija `disabledReason: "MANUAL"`; activar borra motivo y fecha y reanuda desde el cursor conservado. Sin ETag: la transición es binaria y la última orden humana prevalece.
- `DELETE /{id}` → 204; borra en cascada sus entregas; repetir → 404.
- `POST /{id}/ping` sin cuerpo → 202 `{delivery}`: encola una entrega `webhook.ping.v1` procesada por el worker, nunca síncrona en el hilo HTTP. Exige endpoint activo (si no, 409 `WEBHOOK_DISABLED`) y como máximo un ping pendiente por endpoint (si no, 409 `WEBHOOK_DELIVERY_PENDING`).
- `GET /{id}/deliveries` → `{items}`: hasta 50 entregas ordenadas `updatedAt DESC, id DESC`. `POST /{id}/deliveries/{deliveryId}/redeliver` sin cuerpo → 202 `{delivery}`: sólo para entregas `succeeded` o `exhausted` de un endpoint activo; pone `status: pending`, `attempt: 0`, `nextAttemptAt: now` y conserva `body` y `eventId`; pendiente → 409 `WEBHOOK_DELIVERY_PENDING`.

DTO `endpoint` cerrado: `{id, url, description, eventTypes, status, disabledReason, disabledAt, createdAt, updatedAt}`. DTO `delivery` cerrado: `{id, eventId, eventType, status, attempt, httpStatus, latencyMs, errorClass, nextAttemptAt, createdAt, updatedAt}`; los cuatro últimos campos de resultado describen el último intento y son null antes del primero. Instantes UTC con precisión de microsegundos y reloj inyectado, como en 24.

Errores `application/problem+json` con código estable: 400 `WEBHOOK_INVALID` (campos, con `errors[]` por campo), 400 `WEBHOOK_URL_BLOCKED` (destino resuelto a dirección prohibida), 400 `WEBHOOK_URL_UNRESOLVABLE` (DNS sin respuesta), 413 `WEBHOOK_TOO_LARGE`, 404 `WEBHOOK_NOT_FOUND`, 409 `WEBHOOK_LIMIT`, 409 `WEBHOOK_DISABLED`, 409 `WEBHOOK_DELIVERY_PENDING`, 503 `CONNECTORS_DISABLED` (sin clave de cifrado válida; afecta a POST, ping y redeliver, no a lecturas, estado ni borrado), 503 `STORAGE_UNAVAILABLE`. Prioridad: seguridad existente; método y tipo de contenido; tamaño y estructura; valores; disponibilidad de clave; SSRF; cupo; escritura. Un endpoint ajeno responde igual que uno inexistente.

### Seguridad y privacidad

Secreto generado por el servidor, mostrado una única vez y guardado cifrado con AES-256-GCM, nonce aleatorio de 12 bytes y AAD igual al `id` del endpoint, bajo la clave de entorno `APP_CONNECTOR_KEY` (propiedad `app.connectors.key`, base64 de exactamente 32 bytes). Un hash no sirve porque la firma HMAC exige el secreto en claro en cada envío. Clave ausente o mal formada: la aplicación arranca, `audit.workerError("CONFIGURATION_ERROR")`, el worker es no-op y las operaciones que necesitan clave responden 503 `CONNECTORS_DISABLED`. El secreto en claro sólo existe en memoria durante la respuesta de creación y durante la firma; nunca en logs, listados, export, sessionStorage/localStorage ni mensajes de error.

Guardia SSRF en creación y en cada intento de entrega: esquema https obligatorio; se resuelve el host y se rechaza si alguna dirección es loopback, no especificada, privada (10/8, 172.16/12, 192.168/16, 100.64/10, fc00::/7), enlace local (169.254/16, fe80::/10), multicast, IPv4 mapeada a esas clases o literal IP de esas clases. El cliente HTTP no sigue redirecciones (3xx cuenta como fallo `REDIRECT`), usa timeout de conexión 5 s y de lectura 5 s, descarta el cuerpo de respuesta sin almacenarlo y no envía cookies ni credenciales. Ventana TOCTOU entre comprobación DNS y conexión: se documenta como límite; la política de egreso del despliegue es la segunda barrera. El cuerpo entregado es el payload ya publicado por la outbox, que no contiene descripciones ni notas privadas; sí contiene `ownerId`, lo que el propietario acepta al registrar su propia URL.

### Concurrencia, idempotencia y recuperación

Cupo de 5 y creación serializados con `pg_advisory_xact_lock(hashtextextended('webhooks:' || owner, 0))` pasado como parámetro, mismo patrón que 23/24: dos creaciones concurrentes por la última plaza producen exactamente un 201 y un 409 `WEBHOOK_LIMIT`. Respuesta de creación perdida: no se reintenta automáticamente; la UI refresca la lista y, si aparece un endpoint cuyo secreto no se llegó a ver, indica eliminarlo y crear otro (la creación es barata y sin efectos externos, a diferencia de 24). Las transiciones `PUT /status`, ping y redeliver son idempotentes o rechazadas con 409, nunca duplican trabajo. El worker reclama filas con `SELECT ... FOR UPDATE SKIP LOCKED` y registra el resultado en la misma transacción: un fallo del proceso durante el envío revierte la reclamación y el intento se repite, de ahí «al menos una vez». Reintentar desde el propio worker un intento de red incierto es correcto; el receptor deduplica por `X-OrganizationWeb-Event-Id`.

### Entrega (worker, firma, reintentos, registro)

`WebhookSchedule` replica `PublisherSchedule`: configuración `@ConditionalOnProperty("app.webhooks.enabled")` (env `APP_WEBHOOKS_ENABLED`, por defecto `false`), `@Scheduled(fixedDelay = 1000, initialDelay = 1000)` llamando a `DeliverWebhooksUseCase.runCycle()`, que procesa como máximo 20 entregas por ciclo. Cada ciclo hace dos pasos. Encolar: para cada endpoint activo sin entrega pendiente derivada de la outbox, toma el primer evento con `owner_id` igual, `event_type = ANY(event_types)`, `status <> 'blocked'`, `(occurred_at, event_id) > (cursor_occurred_at, cursor_event_id)` y `occurred_at <= now - 5 s` (ventana de gracia para commits tardíos), ordenado por `(occurred_at, event_id)`; inserta la entrega con el `payload` JSON de la outbox como `body` y avanza el cursor al evento en la misma transacción. Un evento con `validationCode()` no nulo avanza el cursor sin entrega y se audita. Entregar: reclama entregas `pending` con `next_attempt_at <= now` de endpoints activos, ordenadas por `next_attempt_at`, y ejecuta el envío dentro de la reclamación.

Petición: `POST url`, `Content-Type: application/json; charset=utf-8`, `User-Agent: OrganizationWeb-Webhooks/1`, `X-OrganizationWeb-Event-Id: <eventId>`, `X-OrganizationWeb-Signature: t=<segundos unix del envío>,v1=<hex minúsculo de HMAC-SHA256(secreto, t + "." + body)>`. El `body` son los bytes UTF-8 almacenados, idénticos en cada intento; sólo `t` cambia. Para `webhook.ping.v1` el cuerpo es `{eventId, aggregateId: <endpointId>, ownerId, occurredAt, schemaVersion: 1, type: "webhook.ping.v1"}` con `eventId` igual al `id` de la entrega. Éxito: cualquier 2xx recibido dentro del timeout, `status: succeeded`. Cualquier otro resultado es un intento fallido con `error_class` en {`HTTP_ERROR` (4xx/5xx), `REDIRECT`, `TIMEOUT`, `CONNECTION`, `TLS`, `DNS`, `BLOCKED_ADDRESS`, `SECRET_UNREADABLE`} —la octava, ratificada por el propietario el 10 de septiembre de 2026, no clasifica una respuesta del receptor sino una entrega que **no se pudo enviar** porque ninguna clave del llavero abre el secreto guardado—; `http_status` sólo cuando hubo respuesta; `latency_ms` medido con el **cronómetro monótono inyectado**, no con el reloj de pared: medir tiempo transcurrido con un reloj que puede saltar da latencias negativas o absurdas cuando el sistema ajusta la hora. Enmienda ratificada por el propietario el 10 de septiembre de 2026. No se distingue 4xx de 5xx para reintentar: uniformidad frente a heurísticas.

Reintentos tras fallo: 1 min, 5 min, 30 min, 2 h y 24 h, es decir, seis intentos en total y unas 26,6 h de ventana. Al fallar el sexto, la entrega pasa a `exhausted` y, en la misma transacción, el endpoint pasa a `disabled` con `disabledReason: "DELIVERY_EXHAUSTED"`; sus demás entregas pendientes se congelan hasta reactivación y el cursor no retrocede, de modo que reactivar continúa con el siguiente evento y la entrega agotada sólo se reenvía mediante redeliver. Tras cada resultado terminal, el worker borra las entregas terminales del endpoint más allá de las 50 más recientes por `updated_at`; las pendientes nunca se podan. El registro guarda código, latencia, clase de error e intento; nunca cuerpos de respuesta, cabeceras recibidas ni secretos. Auditoría `Slf4j` por ciclo con `eventId`, `endpointId` y clase de error, sin URL completa ni firma.

### Interfaz (`/webhooks`)

Ruta privada `/webhooks`, H1 «Webhooks», entrada de navegación tras «API para integraciones» sin desplazar Hoy. Estado vacío orientador: explica qué es un webhook, la firma y el límite de cinco. Formulario: URL (`type="url"`), descripción, casillas por tipo de evento con «Seleccionar todos» y ayuda que enlaza `docs/webhooks.md`; el botón «Crear webhook» se deshabilita durante la petición. Tras 201 el secreto se muestra una vez, sólo en memoria, con selección manual y botón «Copiar» sin escritura automática; se oculta al desmontar, cambiar de propietario o cerrar sesión, y ninguna respuesta tardía lo publica a otra identidad. Respuesta de red incierta: mensaje explícito, sin reintento automático, botón «Actualizar lista».

Lista: URL, descripción, tipos, estado como texto y atributo ARIA («Activo», «Desactivado manualmente», «Desactivado por entregas agotadas» con fecha), acciones «Enviar ping», «Activar»/«Desactivar», «Eliminar» con confirmación explícita y «Ver entregas». Panel de entregas: tabla con tipo, intento, código HTTP, latencia, clase de error, estado y fecha, botón «Reenviar» por fila terminal y «Actualizar» manual; sin sondeo automático. 503 `CONNECTORS_DISABLED` se explica como configuración del servidor pendiente, sin ocultar el formulario. Controles de 44 px, teclado, foco visible y devuelto, anuncios de estado, contraste en ambos temas, matriz UX30 y responsive de [docs/ux-requirements.md](docs/ux-requirements.md) con evidencia en tres motores. Sin dependencias nuevas, sin sessionStorage ni localStorage, reutilizando `apiRequest`, tokens SCSS y guardas de identidad/abort existentes.

### Límites explícitos

Orden garantizado sólo por endpoint y sólo para eventos de la outbox; ping y redeliver se intercalan. Un evento cuyo commit llegue más de 5 s después de su `occurredAt` y ya rebasado por el cursor no se entrega; se documenta el techo y su alternativa (columna de secuencia en la outbox). No hay entrega a destinos http, loopback ni redes privadas, tampoco en desarrollo: las pruebas sustituyen la política por un puerto. DNS rebinding entre comprobación y conexión **sí queda cerrado**: la petición viaja a la dirección ya validada y no vuelve a resolver el nombre (`AnchoredConnection`), conservando el nombre para la verificación del certificado; hay prueba de TLS en las dos direcciones. El campo del secreto usa `field-sizing: content`, que **sólo implementa Chromium**: en Firefox y Safari el secreto se recortaría con el texto al 200 %, porque el campo no crece con su contenido. Se acepta como límite conocido y no como defecto porque el secreto se muestra una sola vez y se puede copiar con el botón, que no depende del ancho. Rotación de `APP_CONNECTOR_KEY` invalida los secretos guardados; se requiere recrear endpoints. Sin cuota de envíos ni límites por minuto: el volumen lo acota la actividad del propio propietario. Ejecución con varias instancias se apoya en `SKIP LOCKED` pero no se prueba con varias JVM. No se prometen tiempos de entrega ni exactamente una vez.

### Decisiones y alternativas descartadas

1. Cifrado AES-GCM del secreto frente a hash (imposible firmar) y frente a derivación HKDF por endpoint sin almacenamiento (menos columnas, pero misma exposición ante fuga de la clave maestra y menos legible en revisión). 2. Cursor propio `(owner_id, occurred_at, event_id)` sobre la outbox frente a consumir RabbitMQ (obliga a broker en producción y a consumidores nuevos) y frente a marcar filas de la outbox (modifica el publicador). 3. Cuerpo igual al payload de la outbox frente a un sobre nuevo: cero mapeo, esquema ya validado. 4. `POST /` con id de servidor frente a `PUT /{id}` con id de cliente como en 24: la recuperación aquí es eliminar y recrear, sin cupo escaso ni efectos externos. 5. Reintentos uniformes ante cualquier no-2xx frente a fallar rápido en 4xx: menos reglas; el coste es esperar 26 h antes de desactivar una URL errónea, mitigado por el ping inmediato. 6. Una entrega de outbox en vuelo por endpoint frente a paralelismo: conserva orden y acota la tabla sin colas adicionales. 7. Envío dentro de la reclamación transaccional, como `PublishOutbox`, frente a un arrendamiento con `lease_until`: el timeout de 10 s acota el bloqueo y evita una máquina de estados extra. 8. Sin edición de URL ni rotación de secreto: recrear cubre ambos casos con cinco plazas. 9. Reactivar reanuda desde el cursor frente a saltar al presente: respeta «al menos una vez»; saltar se consigue eliminando y recreando. 10. Registro con un único registro por entrega y último intento frente a fila por intento: cumple «últimas 50 entregas» con poda trivial. 11. Ping asíncrono por el worker frente a envío síncrono desde el controlador: ningún egreso de red en hilos HTTP.

### Verificación prevista

Unitarias de dominio y aplicación: validación de URL y campos, catálogo de tipos, `RetrySchedule` (1, 5, 30, 120, 1440 min y agotamiento al sexto), firma HMAC con vector conocido y `t` fijo, política de direcciones con literales IPv4/IPv6 sin red, cifrado/descifrado con AAD y rechazo de clave de longitud distinta de 32 bytes. Persistencia con PostgreSQL 17.9 en Testcontainers: carrera por la quinta plaza, avance de cursor con comparación de tupla y ventana de gracia, exclusión de `blocked`, filtro por tipos, `SKIP LOCKED` con dos hilos, rollback de reclamación al fallar el callback, desactivación transaccional al agotar, poda a 50, cascada al eliminar, aislamiento por propietario. MockMvc `WebhooksApiTest`: los cinco recursos, JSON cerrado, 4096 bytes, códigos de error y prioridad, no-store, 503 sin clave, ausencia del secreto en toda lectura. Worker offline `WebhookDeliveryTest` con `com.sun.net.httpserver.HttpServer` en 127.0.0.1 y política permisiva inyectada: 2xx, 4xx/5xx, 3xx sin seguir, lectura lenta hasta timeout, cierre de conexión, verificación de cabeceras y firma desde el servidor de prueba, cuerpo idéntico entre intentos, ping y redeliver; `WebhooksWiringTest` comprueba que la configuración de producción usa la política estricta y el no-op sin clave. Vitest: `webhooks.tsx`, `webhooks-api.ts`, estados vacío/lista/secreto/entregas/errores y ocultación del secreto. E2E `e2e/webhooks.spec.mjs` con `APP_CONNECTOR_KEY` en la pila efímera: creación, secreto visible una vez, desactivar/activar, ping encolado en estado pendiente, eliminación, teclado, axe y anchos 320/768/1440; la entrega real a un receptor https no se prueba en e2e y queda cubierta por el test de worker. Mutación: PIT scope `webhooks` (dominio, casos de uso, controlador, `PostgresWebhookStore`, `WebhookDeliveryClient`, `ApplicationConfiguration`) y `frontend/stryker.webhooks.config.json`, umbral 80 % sin rebajar. Documento para receptores `docs/webhooks.md` con ejemplo de verificación de firma y tolerancia recomendada de 5 min para `t`.

## Feature 26: ics_calendar — Calendario ICS de bloques propios

### Propósito y frontera

Permitir que una aplicación de calendario (Google Calendar, Apple Calendar, Thunderbird, Outlook) muestre los bloques planificados propios mediante una suscripción iCalendar de solo lectura, y descargar el mismo archivo desde la sesión web. Un bloque sigue siendo una reserva de tiempo: el calendario no acredita trabajo ni sesiones. No incluye sincronización bidireccional ni proveedores externos (feature 28), ni tareas como VTODO, ni alarmas, recurrencias, filtros por proyecto o feeds múltiples. Las credenciales Bearer de la feature 24 no dan acceso al feed: las aplicaciones de calendario solo aceptan una URL sin cabeceras, por lo que el feed usa un token dedicado de capacidad en la ruta.

### Modelo y persistencia (migración reservada V24__calendar_feed_tokens.sql)

Tabla nueva `calendar_feed_tokens(owner_id TEXT PRIMARY KEY, token_hash BYTEA NOT NULL UNIQUE CHECK (octet_length(token_hash) = 32), created_at TIMESTAMPTZ NOT NULL)`. Una fila por propietario implica un único token activo; generar sobre una fila existente la sustituye con `INSERT … ON CONFLICT (owner_id) DO UPDATE` y revocar la borra. No se guarda historial de tokens ni fecha de último uso: la lectura pública no escribe nada. El token es de 32 bytes de `SecureRandom`, codificado base64url sin relleno (exactamente 43 caracteres `[A-Za-z0-9_-]`); solo se persiste su SHA-256. No se altera ninguna tabla anterior.

La consulta del feed parte de `projects.owner_id`, une `tasks(project_id)` y `planned_blocks(project_id, task_id)` y hace `LEFT JOIN block_projections`. Un bloque es vigente cuando `COALESCE(projection.status, 'planned') = 'planned'`; su intervalo, zona y versión son los de la proyección si existe y los de `planned_blocks` con versión 1 en caso contrario, según la feature 13. `planned_blocks` no tiene `owner_id`: la propiedad se resuelve siempre por el proyecto, como en la exportación.

### API y recurso público

`GET /calendar/{token}.ics` es público: `permitAll` para GET bajo `/calendar/**`, sin sesión, sin CSRF, sin Bearer y sin `Set-Cookie`. Éxito: 200, `Content-Type: text/calendar; charset=utf-8`, `Cache-Control: private, no-store`, `X-Content-Type-Options: nosniff`, `Content-Length` exacto, sin `Content-Disposition`. Token con formato distinto de los 43 caracteres, desconocido o revocado, y cualquier otra ruta bajo `/calendar/`, responden el mismo 404 `CALENDAR_NOT_FOUND` en `application/problem+json`, con el mismo cuerpo, cabeceras y sin diferencia de tiempo observable por diseño (búsqueda por hash indexado, sin comparación parcial). HEAD devuelve las cabeceras de GET sin cuerpo. Solo se admiten GET y HEAD; otros métodos responden 405 con `Allow: GET, HEAD`. No se aceptan parámetros de consulta: cualquiera produce el mismo 404.

`GET /api/v1/me/calendar.ics` exige sesión y devuelve el mismo cuerpo que el feed del propietario, con `Content-Disposition: attachment; filename="organizationweb-bloques.ics"` y las mismas cabeceras; funciona aunque no exista token activo. `GET /api/v1/me/calendar-feed` devuelve 200 con exactamente `active` (booleano) y `createdAt` (instante UTC con microsegundos o null); nunca devuelve el token ni su hash. `POST /api/v1/me/calendar-feed` con cuerpo vacío genera o regenera el token y responde 201 con exactamente `url` y `createdAt`; `url` es `app.public-origin` + `/calendar/` + token + `.ics` y es la única vez que el token se transmite. Regenerar invalida el anterior desde el commit. `DELETE /api/v1/me/calendar-feed` responde 204 tanto si había token como si no. POST y DELETE conservan CSRF, OriginGuard y JSON estricto; un cuerpo no vacío en POST produce 400 `VALIDATION_ERROR`. Sin sesión: 401 `UNAUTHENTICATED`. Fallos de almacenamiento reconocidos, incluida una colisión de `token_hash`: 503 `STORAGE_UNAVAILABLE` sin reintento automático.

Contenido: bloques vigentes cuyo intervalo interseca la ventana semiabierta `[now − 30 días, now + 365 días)`, es decir `end_at > now − 30d` y `start_at < now + 365d`, con `now` del reloj inyectado. Más de 2000 eventos en la ventana producen 413 `CALENDAR_TOO_LARGE` sin cuerpo parcial; se comprueba con `LIMIT 2001` antes de serializar. Con el tamaño máximo de título y objetivo, 2000 eventos suponen unos 6 MB; 5000 permitirían unos 15 MB en cada sondeo del cliente de calendario, por eso se descarta.

### Formato iCalendar (RFC 5545 exacto)

Documento UTF-8 sin BOM, líneas terminadas en CRLF, propiedades en este orden fijo: `BEGIN:VCALENDAR`, `VERSION:2.0`, `PRODID:-//apptolast//OrganizationWeb//ES`, `CALSCALE:GREGORIAN`, `METHOD:PUBLISH`, `X-WR-CALNAME:Bloques planificados`, `X-WR-TIMEZONE:<zoneId>` solo si el propietario tiene disponibilidad configurada (informativa: no altera ningún instante), los VEVENT y `END:VCALENDAR`. Los eventos se ordenan por `DTSTART` ascendente y `UID` ascendente. Sin bloques en la ventana se emite el VCALENDAR sin componentes; se documenta como desviación deliberada de RFC 5545 §3.6 porque los clientes aceptan un calendario vacío y un 404 o 204 harían que conservaran eventos obsoletos o marcaran la suscripción como rota.

Cada VEVENT contiene exactamente, en este orden: `UID:<blockId>@organizacion.apptolast.com` (UUID canónico en minúsculas), `DTSTAMP`, `DTSTART`, `DTEND`, `SUMMARY`, `DESCRIPTION`, `SEQUENCE` y `URL`. `DTSTART` y `DTEND` son los instantes vigentes en UTC con formato `YYYYMMDDTHHmmssZ`, sin `VTIMEZONE` ni `TZID`; `DTSTAMP` es `updated_at` de la proyección o `created_at` original, truncado a segundos y en el mismo formato, de modo que un evento sin cambios se serializa byte a byte igual en cada petición. `SUMMARY` es el título de la tarea del bloque; `DESCRIPTION` es el objetivo del bloque tal como se persistió (1–500 puntos de código, sin renormalizar); `SEQUENCE` es la versión vigente del bloque en decimal; `URL` es `app.public-origin` + `/proyectos/<projectId>/tareas/<taskId>`. No se emiten STATUS, TRANSP, CATEGORIES, ORGANIZER, ATTENDEE, VALARM, RRULE, RDATE, EXDATE, LAST-MODIFIED, CREATED ni la zona de planificación.

Los valores TEXT escapan `\` como `\\`, `;` como `\;`, `,` como `\,` y salto de línea como `\n`; los CR aislados se eliminan. Las líneas de más de 75 octetos se pliegan con CRLF seguido de un espacio, cortando siempre en frontera de carácter UTF-8, nunca dentro de una secuencia multibyte ni de un escape. Un lector conforme obtiene, tras desplegar y desescapar, exactamente el título y el objetivo persistidos. El escritor es una clase pura del dominio (`IcsCalendar`/`IcsWriter`) sin dependencias de Spring, verificable por unidad.

### Seguridad y privacidad

El token es una capacidad de solo lectura sobre los bloques propios: quien conoce la URL ve títulos de tareas y objetivos. La interfaz lo explica antes de generar y al mostrar la URL. El token nunca se registra en logs de aplicación, mensajes de error, outbox ni eventos; el adaptador HTTP no debe escribir la ruta de `/calendar/` en logs. El SHA-256 almacenado no permite reconstruirlo; un 256 bits aleatorios hacen irrelevantes la fuerza bruta y la comparación en tiempo constante. La revocación y la regeneración son efectivas en la siguiente petición, sin caché intermedia (`no-store`). No hay cuota propia: un token válido devuelve como máximo 2000 eventos acotados y un token inválido cuesta una búsqueda indexada; una cuota por IP se deja al proxy. El feed no incluye datos de otras identidades, sesiones de trabajo, historial, preferencias, claves de idempotencia ni credenciales de la feature 24, y no forma parte del archivo de exportación de la feature 22. Los textos se escapan como TEXT de iCalendar, nunca se interpretan como HTML.

### Concurrencia, idempotencia y recuperación

La lectura pública y la descarga con sesión ejecutan una única transacción read-only `REPEATABLE_READ`: resolución del token, zona de disponibilidad y bloques salen del mismo snapshot; un movimiento o cancelación concurrente queda enteramente dentro o fuera. Generar y revocar son una sola sentencia sobre la fila del propietario. Dos POST concurrentes devuelven cada uno 201 con su URL, pero solo la última confirmada sigue siendo válida; la interfaz advierte que regenerar invalida cualquier enlace anterior. POST no usa `Idempotency-Key`: una respuesta perdida se recupera consultando el estado y, si el token no llegó a verse, regenerando de forma deliberada, igual que en la feature 24; el coste es solo volver a pegar la URL en el cliente de calendario. DELETE es idempotente por definición. Ningún endpoint escribe outbox ni publica eventos: suscribirse a un calendario no es un hecho de organización. Reiniciar el backend conserva el token; el cliente de calendario sigue funcionando sin intervención.

### Interfaz (ruta `/calendario`, navegación, estados)

Ruta `/calendario`, entrada «Calendario» en la navegación Principal tras «Exportación», sección homónima en `App.tsx` y `Workspace`. Encabezado h1 «Calendario ICS» y ayuda breve: qué contiene el feed (bloques vigentes, de 30 días atrás a un año), que la URL es secreta y de solo lectura, y que no sincroniza cambios desde el calendario externo. Abrir la vista ejecuta solo `GET /api/v1/me/calendar-feed`. Estados distintos y anunciados: cargando; sin enlace (acción primaria «Crear enlace de suscripción»); enlace activo (fecha de creación, «Regenerar enlace» y «Revocar enlace», ambos con confirmación inline que explica que el enlace anterior deja de funcionar); enlace recién creado (URL completa en un campo de solo lectura seleccionable, botón «Copiar enlace» con `navigator.clipboard.writeText` y, si no está disponible, indicación para seleccionar y copiar; aviso de que no volverá a mostrarse); fallo (mensaje y reintento manual, sin falso éxito). Abandonar la ruta, cerrar sesión o cambiar de identidad descarta la URL mostrada y aborta peticiones en vuelo; la URL nunca se guarda en localStorage, sessionStorage ni logs del cliente.

«Descargar archivo .ics» reutiliza el recorrido de la feature 22: `apiRequest` a `/api/v1/me/calendar.ics`, validación de `Content-Type`, `Content-Length` y de que el cuerpo empieza por `BEGIN:VCALENDAR` y termina por `END:VCALENDAR` + CRLF, y solo entonces un enlace nativo con `download` y el nombre del contrato; 413 explica el límite de 2000 eventos; red y 503 ofrecen reintento manual. Se reutilizan `RouteLink`, `apiRequest`, tokens de estilo y patrones de foco existentes: controles nativos, 44 px, reflow a 320, zoom 200 %, temas y movimiento reducido; foco al h1 solo si el control iniciador desaparece y la persona no lo movió. La matriz de 30 principios UX se revisa con evidencias en la entrega.

### Límites explícitos

No hay feeds por proyecto o tarea, varios tokens simultáneos, caducidad automática del token, fecha de último acceso, cuota propia, VTIMEZONE, alarmas, recurrencias, VTODO para tareas, sesiones de trabajo, importación de calendarios ni sincronización hacia OrganizationWeb (feature 28). El feed no se expone por Bearer ni se añade un scope a la feature 24. La zona de planificación de cada bloque no viaja en el evento; el cliente muestra los instantes en su propia zona. Ventana y límite de eventos son fijos, sin parámetros. Los webhooks de la feature 25 no notifican cambios del feed.

### Decisiones y alternativas descartadas

- Token dedicado en la ruta frente a reutilizar credenciales Bearer de 24: los clientes de calendario no envían cabeceras. Frente a usuario y contraseña en la URL (HTTP Basic): expondría una credencial de sesión y Basic está deshabilitado.
- Un único token por propietario con sustitución frente a varios tokens con nombre: menos superficie y una sola pregunta en la interfaz; varios feeds no tienen uso identificado.
- SHA-256 sin sal frente a bcrypt: la entrada tiene 256 bits de entropía y hace falta búsqueda por índice; bcrypt obligaría a recorrer todas las filas.
- 404 indistinguible frente a 401/410 para tokens revocados: no confirmar a terceros que un enlace existió.
- `SUMMARY` = título de la tarea y `DESCRIPTION` = objetivo frente a `SUMMARY` = objetivo: el modelo real (feature 11) siempre vincula el bloque a una tarea y el objetivo puede medir 500 puntos de código, tamaño de descripción. El encargo mencionaba «tarea o proyecto según el objetivo»; el esquema no admite bloques sin tarea, así que no existe el caso «proyecto».
- `DTSTAMP` = `updated_at` del bloque frente a instante de la petición: salida determinista por evento, verificable byte a byte y sin cambios ficticios en cada sondeo.
- UTC con `Z` sin VTIMEZONE frente a `TZID` por bloque: evita generar definiciones VTIMEZONE correctas por cada zona y DST; los clientes convierten UTC de forma fiable. `X-WR-TIMEZONE` solo orienta el calendario recién suscrito.
- Límite 2000 eventos frente a 5000: acota el peso por sondeo a unos 6 MB en el peor caso y sigue lejos del uso real (unos 5 bloques diarios durante 395 días).
- VCALENDAR vacío frente a 404/204 sin bloques: los clientes conservan la suscripción y limpian eventos pasados de ventana.
- Sin `Idempotency-Key` en POST frente a intención recuperable como en 11/13: regenerar no destruye datos de organización y el estado se consulta con GET.
- Migración propia V24 frente a columna en `availability_preferences`: la disponibilidad es opcional y su versión/ETag no debe cambiar por gestionar un token.

### Verificación prevista

Unitarias de dominio: escritor ICS (escape de `\ ; , \n`, plegado a 75 octetos con caracteres multibyte y fuera del BMP en la frontera, CRLF, orden de propiedades y eventos, calendario vacío, formato de instantes, truncado de `DTSTAMP`), token (43 caracteres base64url, hash de 32 bytes, rechazo de formatos incorrectos) y caso de uso (ventana semiabierta con reloj inyectado, exclusión de cancelados, versión y `SEQUENCE`, umbral 2000/2001, precedencia de zona informativa). PostgreSQL Testcontainers: aislamiento por propietario a través de proyectos, precedencia de proyección sobre recibo original, bloque cancelado excluido, límites de ventana en ambos extremos, `ON CONFLICT` de regeneración y borrado, solo hash persistido, snapshot único. MockMvc: 200 público con cabeceras exactas y sin `Set-Cookie`, 404 idéntico para token malformado, desconocido, revocado y ruta ajena, 405 con `Allow`, 401 en `/api/v1/me/*` sin sesión, CSRF exigido en POST y DELETE y no en GET público, `Content-Disposition` de descarga, 413 y 503. ArchUnit conserva el dominio libre de frameworks. Vitest: módulo `calendar-feed-api` y vista `/calendario` en todos sus estados, portapapeles con y sin `navigator.clipboard`, descarga validada y descarte de respuestas obsoletas. E2E con Playwright: crear enlace, pedir la URL sin cookies con `request.newContext()` y comprobar `text/calendar` y el `UID` del bloque planificado, mover el bloque y ver `SEQUENCE:2`, revocar y recibir 404, axe y barrido 320/768/1440 con zoom 200 %. Mutación: PIT scope `ics_calendar` sobre dominio, casos de uso, controlador, adaptador de persistencia y `ApplicationConfiguration`; Stryker `frontend/stryker.ics-calendar.config.json` con destino `ics-calendar-frontend` en `scripts/project.mjs`; umbral 80 % sin rebajas.

### Pregunta abierta

Registro de accesos del proxy: la ruta contiene el token, por lo que el despliegue debe excluir `/calendar/` de los access logs o enmascararla. Pertenece a la infraestructura y queda pendiente de confirmar antes de exponer el feed en producción.

## Feature 27: github_connector — Importar issues de GitHub con trazabilidad

Propuesta normativa preparada el 8 de septiembre de 2026 bajo la autorización global del 5 de septiembre. Pendiente de revisión independiente antes de incorporarse a `project-spec.md`; no acredita implementación. Depende de la fusión de la feature 24 (`/integraciones`, canal Bearer) porque reutiliza su página y su exclusión de rutas.

### Propósito y frontera

Traer issues abiertas de un repositorio de GitHub como tareas de un proyecto propio, conservando de forma duradera qué tarea procede de qué issue. Es una importación bajo demanda y de un solo sentido: nada se escribe en GitHub, nada se sincroniza después y una tarea importada es una tarea normal del producto. Una conexión por propietario, un repositorio por conexión, sin GitHub App, OAuth ni webhooks.

### Modelo y persistencia

Migración reservada **V25__github_connector.sql** con tres tablas:

- `github_connections(owner_id TEXT PRIMARY KEY, repository TEXT NOT NULL, login TEXT NOT NULL, token_ciphertext BYTEA NOT NULL, status TEXT NOT NULL CHECK (status IN ('valid','invalid')), connected_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL)`. `repository` cumple `^[A-Za-z0-9](?:[A-Za-z0-9-]{0,37}[A-Za-z0-9])?/[A-Za-z0-9._-]{1,100}$` mediante CHECK. `token_ciphertext` contiene nonce de 12 bytes, texto cifrado y etiqueta de 16 bytes; CHECK `octet_length BETWEEN 29 AND 283`.
- `task_external_links(owner_id TEXT NOT NULL, task_id UUID NOT NULL UNIQUE REFERENCES tasks(id), source TEXT NOT NULL CHECK (source IN ('github')), external_id TEXT NOT NULL CHECK (char_length(external_id) BETWEEN 1 AND 200), url TEXT NOT NULL CHECK (url ~ '^https://' AND char_length(url) <= 2048), imported_at TIMESTAMPTZ NOT NULL, PRIMARY KEY (owner_id, source, external_id))`. Tabla genérica en forma; el enum de `source` es cerrado y cada conector futuro lo amplía por migración. Una tarea tiene como máximo un origen externo.
- `github_imports(id UUID PRIMARY KEY, owner_id TEXT NOT NULL, project_id UUID NOT NULL REFERENCES projects(id), repository TEXT NOT NULL, status TEXT NOT NULL CHECK (status IN ('running','completed','failed')), created INTEGER NOT NULL DEFAULT 0, skipped INTEGER NOT NULL DEFAULT 0, failed INTEGER NOT NULL DEFAULT 0, truncated BOOLEAN NOT NULL DEFAULT false, error_code TEXT, started_at TIMESTAMPTZ NOT NULL, finished_at TIMESTAMPTZ, CHECK ((status = 'running') = (finished_at IS NULL)))`, contadores con CHECK `>= 0`, índice único parcial `github_imports_one_running ON github_imports(owner_id) WHERE status = 'running'` e índice `(owner_id, started_at DESC, id DESC)`.

`external_id` es el `id` numérico global de la issue en GitHub, en decimal; `url` es su `html_url`. La conexión y los enlaces no forman parte del JSON v1 de exportación (22) ni de importación (23): añadir colecciones cambiaría esos contratos cerrados.

### API

Rutas privadas, sólo con sesión cookie (CSRF y OriginGuard vigentes), `Cache-Control: no-store`, JSON estricto sin campos desconocidos y sin query. El canal Bearer de la feature 24 no las incluye en su allowlist.

- `GET /api/v1/me/connectors/github` devuelve 200 con exactamente `{repository, login, status, connectedAt, lastImport}`; `status` es `valid` o `invalid`; `lastImport` es `null` o el DTO de recibo. Sin conexión, 404 `CONNECTION_NOT_FOUND`.
- `PUT /api/v1/me/connectors/github` recibe exactamente `{repository, token}`. `repository` se recorta con Unicode White_Space y debe cumplir la expresión anterior; `token` es una cadena de 1 a 255 caracteres ASCII imprimibles sin espacios. El servidor llama a `GET /repos/{owner}/{repo}` y a `GET /user` con el token; si ambos responden 200 guarda `repository` con el `full_name` devuelto, `login` de `/user`, el token cifrado y `status: valid`, y responde 200 con el DTO de conexión. Repetir el PUT sustituye repositorio y token; los enlaces existentes se conservan. Nunca devuelve el token.
- `DELETE /api/v1/me/connectors/github` borra la fila completa, incluido el texto cifrado, y responde 204 aunque no exista conexión. Tareas, recibos y `task_external_links` permanecen.
- `POST /api/v1/me/connectors/github/imports` recibe exactamente `{projectId}` (UUID canónico) y ejecuta la importación de forma síncrona. Éxito: 201, `Location: /api/v1/me/connectors/github/imports/{id}` y el recibo con `status: completed`.
- `GET /api/v1/me/connectors/github/imports/{id}` devuelve el recibo propio o 404 `IMPORT_NOT_FOUND`; el ajeno es igual al ausente.

Recibo cerrado: `{id, projectId, repository, status, created, skipped, failed, truncated, errorCode, startedAt, finishedAt}`; `errorCode` y `finishedAt` son `null` mientras corre. Errores en `application/problem+json` con el formato de `ApiErrors`:

| HTTP | Código | Cuándo |
| --- | --- | --- |
| 400 | VALIDATION_ERROR | Cuerpo, campos o `projectId` inválidos; errores por campo REQUIRED, INVALID_TYPE, INVALID_FORMAT, TOO_LONG, UNKNOWN_FIELD |
| 404 | CONNECTION_NOT_FOUND / IMPORT_NOT_FOUND / RESOURCE_NOT_FOUND | Sin conexión; recibo ausente; proyecto inexistente o ajeno |
| 409 | GITHUB_TOKEN_REJECTED | GitHub responde 401 al conectar; no se guarda nada |
| 409 | GITHUB_REPOSITORY_UNAVAILABLE | GitHub responde 404 o 403 sin cabeceras de cuota al conectar o al importar; la conexión no cambia |
| 409 | CONNECTION_INVALID | Conexión con `status: invalid`, o GitHub respondió 401 durante la importación (se marca `invalid` en ese momento) |
| 409 | IMPORT_IN_PROGRESS | Ya existe un recibo `running` del propietario |
| 409 | PROJECT_COMPLETED | El proyecto destino está `completed` al empezar o durante la importación |
| 503 | CONNECTORS_DISABLED | `APP_CONNECTOR_KEY` ausente; ninguna ruta del conector escribe ni lee GitHub |
| 503 | RATE_LIMITED | GitHub 429, o 403 con `x-ratelimit-remaining: 0` o `Retry-After`; incluye `retryAfterSeconds` y cabecera `Retry-After` |
| 503 | GITHUB_UNAVAILABLE | Timeout, red, TLS, 5xx o JSON inesperado de GitHub |
| 503 | STORAGE_UNAVAILABLE | Persistencia fallida, según el contrato existente |

`retryAfterSeconds` toma `Retry-After` si existe, si no `x-ratelimit-reset` menos el reloj inyectado, mínimo 1 y por defecto 60. Cuando un POST de importación falla después de crear el recibo, el problema añade el miembro `importId` para consultar los contadores parciales.

### Seguridad y secretos

El token es un PAT de grano fino que el usuario pega; necesita permiso de lectura de issues y metadatos del repositorio. Se cifra con AES-256-GCM, nonce aleatorio de 12 bytes por escritura, etiqueta de 128 bits y `owner_id` como dato adicional autenticado, usando la clave de `app.connectors.key` (`APP_CONNECTOR_KEY`, base64 de 32 bytes). Clave ausente: el conector responde 503 `CONNECTORS_DISABLED` y el resto de la aplicación arranca. Clave presente pero no decodificable a 32 bytes: la aplicación no arranca. Rotar la clave exige volver a conectar; no hay identificador de clave.

El token no aparece en respuestas, logs, auditoría, mensajes de error, exportación, sesión ni almacenamiento del navegador; el campo del formulario se vacía tras enviar. Sólo viaja a `app.github.api-base` (por defecto `https://api.github.com`; `http` sólo para loopback, como `app.public-origin`), con `Authorization: Bearer`, `Accept: application/vnd.github+json`, `X-GitHub-Api-Version: 2022-11-28` y `User-Agent: OrganizationWeb`, sin seguir redirecciones. Timeouts: 2 s de conexión y 4 s de lectura por llamada. Los logs registran propietario, repositorio, código HTTP de GitHub y contadores; nunca cuerpos de issues.

### Importación

Precondiciones en orden: clave configurada, cuerpo válido, conexión existente y `valid`, proyecto propio y no `completed`, inserción del recibo `running` (la violación del índice parcial produce 409 `IMPORT_IN_PROGRESS`). Después se piden como máximo dos páginas de `GET /repos/{owner}/{repo}/issues?state=open&per_page=100&sort=created&direction=desc&page=n`; la segunda sólo si la primera trae 100 elementos. `truncated` es verdadero si tras la última página leída la cabecera `Link` contiene `rel="next"`. Los elementos con campo `pull_request` se descartan sin contar en ningún contador.

Mapeo por issue: el título se recorta con Unicode White_Space; si supera 160 puntos de código se conservan 159 y se añade `…`; si queda vacío la issue cuenta como `failed`. `completionCriterion` es `html_url`, una línea en blanco y las primeras 20 líneas del `body` (`\r\n` normalizado a `\n`; `body` nulo deja sólo la URL), recortado a 2000 puntos de código con `…` final. `estimatedMinutes` es `null`. `external_id` es el `id` de GitHub; existe enlace `(owner_id, 'github', external_id)` implica `skipped`, sin llamadas ni escrituras.

Cada issue restante se crea con el caso de uso `CreateTaskUseCase` existente dentro de una transacción abierta por un puerto nuevo `TaskLinkCommit`: el adaptador ejecuta el callback de creación (que a su vez usa `TaskCommit`, cuya transacción se une a la exterior) e inserta el enlace; tarea, `TaskCreated.v1` y enlace confirman o revierten juntos. No se define un evento nuevo. Una `ValidationException` de una issue suma `failed` y continúa. `StorageUnavailableException`, `ProjectCompletedException`, 401, 403/404 o cuota de GitHub abortan la ejecución: el recibo pasa a `failed` con `errorCode`, los contadores reflejan lo confirmado y la respuesta es el error correspondiente. Las tareas ya creadas no se revierten; repetir la importación las salta.

La atomicidad es por tarea y no por lote porque el lote exigiría mantener una transacción abierta durante las llamadas de red y el bloqueo del proyecto, y porque un único título inválido descartaría las 199 restantes. La idempotencia por enlace hace que la repetición sea segura y los contadores describen exactamente lo persistido. `created + skipped + failed` es el número de issues consideradas; el máximo de candidatas por ejecución es 200.

### Concurrencia y recuperación

El índice parcial es el bloqueo por propietario: dos POST simultáneos obtienen un 201 y un 409. Si el proceso muere con un recibo `running`, la siguiente petición de importación del mismo propietario marca `failed` con `errorCode: INTERRUPTED` cualquier recibo `running` con `started_at` anterior a 15 minutos según el reloj inyectado, y continúa; antes de ese plazo responde 409. Un enlace duplicado por carrera residual produce `skipped`, no error. Si el cliente pierde la respuesta del POST, no reintenta automáticamente: consulta `GET .../connectors/github` y muestra `lastImport`, que puede seguir `running`.

### Interfaz

Ruta `/integraciones/github`, sección de navegación `Integraciones` (sin entrada nueva en el menú); la página `/integraciones` de la feature 24 enlaza «Conector de GitHub». Estados excluyentes: conectores deshabilitados (explica que falta configuración del servidor, sin formulario); sin conexión (formulario con `repository` y `token` de tipo password, `autocomplete="off"`, ayuda sobre el permiso necesario, errores junto al campo); conectada (repositorio, login, estado, última importación con contadores y enlace al proyecto, selector de proyecto propio no terminado cargado con la API de proyectos existente, botón «Importar issues abiertas» y botón «Desconectar» con confirmación); importando (botón deshabilitado, aviso `aria-live` de progreso honesto sin porcentaje); resultado (contadores, aviso de truncado y de `failed`, enlace al proyecto); errores recuperables (cuota con segundos, conexión inválida que ofrece reconectar, importación en curso con opción de consultar estado). Se conservan borradores salvo pérdida de sesión, foco en `h1` al cambiar de estado, controles de 44 por 44, 320/768/1440 px, zoom 200 % y matriz UX de 30 filas con evidencia.

### Límites explícitos

No se importan pull requests, issues cerradas, etiquetas, asignados, hitos, comentarios ni adjuntos. No se actualizan tareas ya importadas cuando la issue cambia ni se cierran al cerrarse la issue. No hay varios repositorios, organizaciones, filtros ni programación periódica. Repositorios con más de 200 issues abiertas importan las 200 más recientes por ejecución. Sin acceso Bearer, sin webhooks, sin escritura en GitHub. PREGUNTA ABIERTA: si una versión 2 de exportación incluirá `task_external_links`; hasta entonces restaurar una copia y reimportar puede duplicar tareas, y la interfaz no lo avisa.

### Decisiones y alternativas descartadas

- PAT pegado frente a OAuth o GitHub App: cero registro de aplicación y cero redirecciones; OAuth queda para cuando exista más de un usuario.
- Cifrado AES-GCM con clave de entorno frente a guardar el token en claro o sólo un hash: el hash no permite reutilizarlo y el claro expone secretos de terceros ante una fuga de la base.
- `id` global de GitHub como `external_id` frente a `owner/repo#number`: sobrevive a renombrados y transferencias del repositorio.
- Importación síncrona frente a trabajo en segundo plano con sondeo: dos llamadas acotadas y 200 inserciones caben en el timeout del proxy; el recibo persistido ya permite recuperar una respuesta perdida. Si repositorios reales superan ese tiempo, la ampliación es 202 con el mismo recibo.
- Índice único parcial como bloqueo frente a bloqueo asesor de PostgreSQL: el bloqueo no puede abarcar las llamadas de red y el índice sobrevive a reinicios con recuperación explícita.
- Reutilizar `TaskCreated.v1` frente a un evento `GithubIssuesImported.v1`: no hay consumidor y la lista blanca de `OutboxMessage` no cambia.
- Dos páginas fijas frente a paginar hasta reunir 200 issues no enlazadas: acota tiempo y cuota; el usuario ve `truncated`.
- DELETE idempotente 204 frente a 404 sin conexión: desconectar dos veces no es un error para quien quiere que el token desaparezca.

### Verificación prevista

Unitarias: mapeo de título y descripción en 159/160/161 puntos de código y con caracteres fuera del BMP, exclusión de `pull_request`, cálculo de `retryAfterSeconds`, ida y vuelta AES-GCM y rechazo de claves de longitud incorrecta. PostgreSQL con Testcontainers: unicidad de enlaces, atomicidad tarea + evento + enlace con fallo inyectado en el enlace, índice parcial de `running`, recuperación por antigüedad, desconexión que conserva enlaces y recibos. MockMvc: DTO cerrados, cada código de error, ausencia del token en toda respuesta, 503 sin clave, CSRF y origen. Servidor GitHub falso con `com.sun.net.httpserver` en tests: paginación con `Link`, elementos PR, 401, 403 con y sin cuota, 429, 404, 5xx, respuesta lenta y ausencia de redirecciones seguidas. Vitest: los siete estados, vaciado del campo token, foco y `aria-live`. E2E Playwright con un servicio falso de GitHub en la pila de compose de e2e (`APP_GITHUB_API_BASE` apuntando a él): conectar, importar, reimportar con `skipped`, desconectar y estado deshabilitado, con axe y barrido responsive. Mutación: PIT scope `github_connector` sobre dominio, casos de uso, cliente HTTP, controlador, adaptador de persistencia y wiring; `frontend/stryker.github-connector.config.json` y destino `github-connector-frontend` en `scripts/project.mjs`; umbral 80 %, sin timeouts enmascarando supervivientes.

## Feature 28: external_calendar — Suscripción de solo lectura a un calendario iCalendar externo

### Propósito, proveedor y dirección

Permitir que el propietario vea en Hoy los compromisos de su calendario externo junto a sus bloques planificados, para no planificar encima de una reunión que la aplicación no conoce. Proveedor de referencia: la «dirección secreta en formato iCal» de Google Calendar. Cualquier feed ICS servido por `https` que cumpla el subconjunto RFC 5545 descrito funciona; Outlook y Apple Calendar se documentan como compatibles pero no verificados en este corte. La dirección es una entrada de solo lectura: la aplicación descarga y analiza el feed, nunca escribe, publica ni responde hacia el calendario externo. Existe como máximo una suscripción por propietario, con etiqueta de 1 a 40 puntos de código tras strip Unicode y una URL `https` de hasta 2048 caracteres, sin userinfo, sin fragmento y con host de nombre, no IP literal.

### Modelo y persistencia

Migración reservada `V26__external_calendar.sql` con dos tablas. `external_calendar_subscriptions`: `owner_id TEXT PRIMARY KEY`, `id UUID UNIQUE`, `label TEXT`, `url_ciphertext BYTEA` (nonce de 12 bytes seguido del cifrado con etiqueta GCM), `url_host TEXT`, `url_tail TEXT` (últimos 4 caracteres), `version BIGINT CHECK (version >= 0)`, `created_at`, `updated_at`, `last_attempt_at TIMESTAMPTZ NULL`, `last_sync_at TIMESTAMPTZ NULL`, `last_status TEXT NULL CHECK IN ('OK','FAILED')`, `last_error TEXT NULL`, `snapshot_zone_id TEXT NULL`, y los contadores enteros no negativos `imported`, `skipped_recurring`, `skipped_cancelled`, `skipped_invalid` más `truncated BOOLEAN`. `external_calendar_events`: `owner_id TEXT REFERENCES external_calendar_subscriptions ON DELETE CASCADE`, `uid TEXT`, `summary TEXT`, `start_at TIMESTAMPTZ`, `end_at TIMESTAMPTZ CHECK (end_at > start_at)`, `all_day BOOLEAN`, clave primaria `(owner_id, uid)` e índice `(owner_id, start_at)`. La instantánea vive solo en esa tabla; ninguna otra tabla, evento de outbox ni historial se modifica. Sin JSONB: el DTO se compone desde columnas.

### Obtención y análisis del feed

La descarga usa `java.net.http.HttpClient` de la JDK con redirecciones deshabilitadas, timeout de conexión y de petición de 5 s, cabecera `Accept: text/calendar`, y lectura en streaming que aborta al superar 1 MiB. Solo se acepta HTTP 200 con `Content-Type` `text/calendar` o cualquier `text/*`; una redirección 3xx cuenta como fallo, no se sigue. Códigos de fallo cerrados: `FEED_REJECTED` (guardia de direcciones), `FEED_UNREACHABLE` (DNS, conexión, timeout), `FEED_HTTP_ERROR` (estado distinto de 200, incluidas redirecciones), `FEED_TOO_LARGE`, `FEED_UNSUPPORTED_TYPE`, `FEED_MALFORMED` (sin `BEGIN:VCALENDAR` o desplegado imposible) y `SECRET_UNREADABLE` (la URL guardada no se descifra con la clave vigente).

Parser propio mínimo, en `domain`, sin dependencias: desplegado de líneas (CRLF o LF seguido de espacio o tabulador), separación `nombre;parámetros:valor`, parámetros `TZID` y `VALUE=DATE`, y desescape de `\n`, `\,`, `\;` y `\\` en texto. Solo se interpretan componentes `VEVENT`; `VTIMEZONE`, `VTODO`, `VALARM` y desconocidos se ignoran. Propiedades: `UID` y `DTSTART` obligatorias; `DTEND` o `DURATION` opcionales; `SUMMARY` opcional, normalizado con strip y recortado a 500 puntos de código, cadena vacía si falta; `STATUS`; `RRULE`, `RDATE` y `RECURRENCE-ID`. Reglas de fecha: sufijo `Z` es UTC; `TZID` debe pertenecer al catálogo `ZoneCatalog` existente, en otro caso el evento es inválido (los nombres de zona de Windows que emite Outlook caen aquí, de ahí «no verificado»); una fecha-hora flotante sin `Z` ni `TZID` y una fecha `VALUE=DATE` se resuelven en la zona de instantánea: la zona de disponibilidad del propietario si es resoluble, UTC en caso contrario, guardada en `snapshot_zone_id`. Un evento de todo el día cubre desde la medianoche de su fecha de inicio hasta la medianoche de su `DTEND` exclusivo (o del día siguiente si falta) en esa zona, con `allDay=true`. `DURATION` se suma al instante de inicio. Un evento con hora sin `DTEND` ni `DURATION`, con fin no posterior al inicio, sin `UID`, sin `DTSTART` o con fecha imposible cuenta en `skippedInvalid`. `STATUS:CANCELLED` cuenta en `skippedCancelled`. Cualquier `VEVENT` con `RRULE`, `RDATE` o `RECURRENCE-ID` cuenta en `skippedRecurring` y no se expande ni se almacena: la expansión de recurrencias queda fuera de alcance. Un `UID` repetido entre eventos válidos conserva el primero y cuenta los demás en `skippedInvalid`.

Ventana almacenada: eventos cuyo intervalo semiabierto intersecta `[syncAt − 24 h, syncAt + 336 h)`, siendo `syncAt` el instante del servidor capturado al iniciar la sincronización. Se ordenan por `startAt` y `uid` ascendentes y se conservan como máximo 500; si hay más, se descartan los últimos y `truncated=true`. Los contadores describen todo el feed, no solo la ventana.

### API

Cinco rutas privadas bajo `/api/v1/me/external-calendar`, con sesión, `OriginGuard`, CSRF en escrituras, JSON estricto, `Cache-Control: no-store` y errores `application/problem+json` mediante `ApiErrors`. Ningún endpoint admite parámetros de consulta salvo el de eventos.

`GET /api/v1/me/external-calendar` devuelve 200 con exactamente `configured` y `subscription`. Sin suscripción: `false` y `null`; leer no inserta. Con suscripción, el objeto cerrado tiene quince campos: `id`, `label`, `urlHost`, `urlTail`, `lastAttemptAt`, `lastSyncAt`, `lastStatus`, `lastError`, `snapshotZoneId`, `imported`, `skippedRecurring`, `skippedCancelled`, `skippedInvalid`, `truncated` y `updatedAt`. Los instantes usan el formato UTC heredado de feature 11; los campos de estado son null antes del primer intento. La URL completa no aparece en ninguna respuesta, log ni error.

`PUT /api/v1/me/external-calendar` recibe exactamente `label` y `url` y reemplaza la suscripción del propietario. Valida sintaxis de la URL y ejecuta la guardia de direcciones resolviendo DNS antes de guardar; no descarga el feed. Crear o cambiar la URL borra la instantánea y reinicia estado y contadores; cambiar solo la etiqueta conserva ambos. Responde 200 con el mismo DTO de lectura; `version` incrementa en cada cambio real. Errores por campo: `label` REQUIRED, INVALID_TYPE, TOO_LONG; `url` REQUIRED, INVALID_TYPE, INVALID_FORMAT (no es URL absoluta), TOO_LONG, INVALID_VALUE (esquema distinto de https, userinfo, fragmento o IP literal), UNRESOLVABLE_HOST y BLOCKED_ADDRESS. No exige `If-Match`.

`DELETE /api/v1/me/external-calendar` responde 204 siempre, exista o no la suscripción, y elimina en la misma transacción la instantánea por la cascada declarada.

`POST /api/v1/me/external-calendar/sync` recibe exactamente `onlyIfStale` booleano obligatorio. Con `false` sincroniza siempre; con `true` solo si `lastAttemptAt` es null o anterior a `now − 15 min`; el umbral lo aplica el backend, nunca el cliente. Responde 200 con exactamente `performed` y `subscription`. Un fallo del feed no es un error HTTP: la sincronización ocurrió y termina con `lastStatus=FAILED`, `lastError` con el código cerrado y la instantánea anterior intacta. Sin suscripción: 404 `EXTERNAL_CALENDAR_NOT_CONFIGURED`.

`GET /api/v1/me/external-calendar/events?from=&to=` exige ambos instantes UTC ISO con `from < to` y una separación máxima de 16 días; ausencia REQUIRED, formato inválido INVALID_FORMAT, orden o separación fuera de límites OUT_OF_RANGE en `to`. Responde 200 con exactamente `configured`, `lastSyncAt`, `lastStatus` e `items`; sin suscripción, `false`, null, null y lista vacía. Cada item tiene exactamente `uid`, `summary`, `startAt`, `endAt` y `allDay`; se incluyen los eventos con `startAt < to && endAt > from`, ordenados por `startAt` y `uid` ascendentes, sin paginación (el máximo es 500).

Errores comunes: 401 sin sesión; 400 VALIDATION_ERROR para parámetros de consulta no admitidos y JSON con campos extra; 400 MALFORMED_JSON; 415 por negociación; 503 STORAGE_UNAVAILABLE; 503 `CONNECTORS_DISABLED` en las cinco rutas cuando falta `APP_CONNECTOR_KEY`, comprobado antes de leer cuerpo o base de datos.

### Seguridad y privacidad

La URL contiene un secreto equivalente a una contraseña de lectura. Se cifra en reposo con AES-256-GCM de `javax.crypto`, clave de 32 bytes en base64 tomada de `APP_CONNECTOR_KEY` (propiedad `app.connectors.key`), nonce aleatorio de 12 bytes por escritura y `owner_id` como dato adicional autenticado. Clave ausente: los conectores quedan deshabilitados con 503 `CONNECTORS_DISABLED`; clave presente pero mal formada: la aplicación no arranca, con mensaje sin el valor. La clave se comparte con los demás conectores del roadmap; una rotación vuelve ilegible la URL guardada y se comunica como `SECRET_UNREADABLE` pidiendo introducirla de nuevo, sin intentar descifrado con claves antiguas.

Guardia SSRF ejecutada al guardar y en cada sincronización: se resuelven todas las direcciones A y AAAA del host y se rechaza si alguna es loopback, privada (10/8, 172.16/12, 192.168/16), enlace local (169.254/16, fe80::/10), local única (fc00::/7), multicast, no especificada o IPv4 mapeada a cualquiera de ellas; también se rechaza un host sin direcciones. La petición se emite después contra el nombre validado; el margen residual de rebinding DNS entre comprobación y conexión se acepta como límite explícito para una aplicación personal y se registra aquí, no se oculta. La guardia es un puerto de aplicación (`AddressPolicy`) para que las pruebas de descarga la sustituyan de forma explícita; la propiedad `app.connectors.allow-private-addresses` (por defecto `false`) solo se activa en el perfil e2e de Compose y nunca en producción. Los logs registran host, código de fallo y duración, nunca la URL ni el cuerpo del feed. La instantánea contiene únicamente `uid`, resumen, instantes y marca de todo el día: no se guardan descripciones, ubicaciones, asistentes ni adjuntos, aunque el feed los incluya.

### Concurrencia, frescura y recuperación

La descarga ocurre fuera de cualquier transacción, sin bloquear la fila del propietario. Antes de descargar se lee `version`; al terminar, una única transacción `DELETE` de los eventos, `INSERT` de la nueva lista y `UPDATE ... WHERE owner_id = ? AND version = ?` del estado. Cero filas actualizadas significa que otra sincronización o un `PUT` ganó: el resultado se descarta y se responde `performed=false` con el estado vigente. Así dos sincronizaciones concurrentes nunca mezclan instantáneas ni dejan una lista parcial. Un fallo de descarga o análisis actualiza solo `last_attempt_at`, `last_status`, `last_error` bajo la misma condición de versión. `lastSyncAt` es el último éxito; `lastAttemptAt`, el último intento; el umbral de 15 minutos usa `lastAttemptAt` para no insistir cada carga de Hoy contra un feed caído. No hay planificador en segundo plano: la frescura la disparan Hoy y el botón manual. Reiniciar el backend conserva suscripción e instantánea; una respuesta perdida tras el commit se resuelve con el siguiente `GET`.

### Interfaz

Ruta exacta `/calendario-externo`, con entrada «Calendario externo» en la navegación principal y `aria-current` coherente. Formulario etiquetado con «Etiqueta» y «Dirección secreta iCal» (`type="url"`), texto de ayuda que explica dónde obtenerla en Google Calendar y que solo se lee. Tras guardar, el campo de dirección se vacía y la pantalla muestra «calendar.google.com … xxxx» con host y cola; nunca se rellena con la URL completa. Acciones: «Guardar», «Sincronizar ahora», «Eliminar suscripción» con confirmación explícita. Estado visible: «Última sincronización correcta», «Último intento», contadores en frases («12 eventos, 3 recurrentes no incluidos, 1 cancelado, 0 inválidos») y, ante `truncated`, el aviso de que solo se conservan 500. Cada código de fallo tiene un mensaje en español accionable; `SECRET_UNREADABLE` pide volver a pegar la dirección. Errores 400 conservan el borrador; red, 503 y códigos desconocidos se muestran como inciertos con reintento manual; 401 retira datos. Estados guardando, guardado y fallo se anuncian sin robar foco.

Hoy añade una sección de solo lectura «Calendario externo» compuesta en el frontend con dos llamadas adicionales tras recibir el snapshot de Hoy: `POST .../sync` con `onlyIfStale=true` y luego `GET .../events?from=dayStartAt&to=dayEndAt` usando los límites del propio DTO de Hoy. El DTO de Hoy, sus quince campos y `TodayController` no cambian. La sección lista resumen e intervalo en la zona efectiva `zoneId` de Hoy, marca «Todo el día» y «Según sincronización de …» con `lastSyncAt`; muestra «Sincronización fallida, se muestran datos de …» cuando `lastStatus=FAILED` con instantánea previa. Con `configured=false` la sección no se muestra. Un fallo de estas dos llamadas nunca degrada la agenda de bloques: se muestra un aviso en la sección con enlace a `/calendario-externo` y Hoy sigue completa. Las peticiones pertenecen a la generación de actualización de Hoy y se cancelan con ella; una respuesta obsoleta no repinta. Verificar 44 × 44 CSS, teclado, 320–2560 CSS, zoom nativo 200 %, texto ampliado y la matriz de 30 principios con límites explícitos.

### Límites explícitos

Los eventos externos no consumen presupuesto, no alteran `plannedSeconds`, `remainingSeconds`, `excessSeconds`, `currentBlockId`, `nextBlockId` ni `closingAt`, y no participan en la prohibición de solapes al planificar o replanificar; son contexto visual. No hay expansión de recurrencias, varias suscripciones, otros esquemas que `https`, autenticación HTTP, escritura hacia el proveedor, sincronización periódica en segundo plano, notificaciones, historial de cambios del feed, eventos de outbox ni exportación de la instantánea (la exportación de feature 22 no la incluye en este corte). La compatibilidad con Outlook/Apple no se acredita hasta tener fixtures verificados.

### Decisiones y alternativas descartadas

| Decisión | Alternativa descartada y razón |
| --- | --- |
| Feed ICS por URL secreta como único proveedor. | OAuth con la API de Google Calendar: requiere registro de aplicación, consentimiento, refresco de tokens y revocación; el feed cubre el caso de lectura con una sola credencial que el usuario ya controla. |
| Cifrado AES-GCM con clave de entorno y 503 sin clave. | Guardar la URL en claro o hashearla: en claro expone un secreto ante un volcado; el hash impide descargar. Un KMS externo excede la infraestructura actual. |
| Parser propio mínimo en dominio. | Biblioteca ical4j: dependencia amplia con su propio registro de zonas y recurrencias; el subconjunto necesario cabe en unas decenas de líneas verificables por mutación. |
| Recurrencias contadas, no expandidas. | Expandir RRULE con excepciones y `RECURRENCE-ID`: complejidad de calendario completa; el contador hace visible la carencia y una feature posterior puede ampliarla. |
| Todo el día y flotantes resueltos en la zona de instantánea al sincronizar. | Guardar fechas y resolver al leer: duplica la lógica de filtrado y obliga al GET de eventos a conocer la zona; los instantes se alinean con el filtro semiabierto ya usado en Hoy. |
| Sincronización optimista por versión sin bloquear durante la descarga. | `SELECT ... FOR UPDATE` durante los 5 s de HTTP: bloquearía cargas concurrentes de Hoy; el descarte del perdedor es más barato y no deja datos parciales. |
| `PUT` valida y guarda sin descargar; la sincronización es una llamada aparte. | `PUT` que también descarga: mezcla errores de validación con fallos remotos en una sola respuesta y alarga la escritura; dos llamadas dan atribución clara. |
| `PUT` sin `If-Match`. | Precondición como en disponibilidad: no hay fusión posible de un único secreto de un único propietario; último escritor gana y la versión sigue protegiendo la sincronización. |
| Hoy compone desde el frontend con dos llamadas. | Incrustar eventos externos en `GET /api/v1/today`: rompe el DTO cerrado de quince campos y sus 3 escenarios de esquema; la composición mantiene ambos contratos independientes. |
| Umbral de 15 min sobre `lastAttemptAt` aplicado por el backend. | Umbral en el cliente o sobre `lastSyncAt`: el cliente no es autoridad y un feed caído provocaría una descarga por cada carga de Hoy. |
| Fallo de feed como 200 con `lastStatus=FAILED`. | 502/504: la operación del usuario sí se completó y el estado persiste; un error HTTP haría creer que no se registró el intento. |

### Verificación prevista

Backend: tests unitarios del parser con fixtures reales anonimizados en `backend/src/test/resources/ics/` (exportación de Google Calendar con eventos UTC, TZID Europe/Madrid, todo el día, cancelado, recurrente con RRULE y `RECURRENCE-ID`; un feed de Outlook para documentar el rechazo de zonas Windows), incluyendo desplegado, desescape, `DURATION`, duplicados de `UID`, ventana y truncamiento a 500. Tests de `AddressPolicy` con direcciones literales de cada familia rechazada. Tests de descarga contra un servidor falso `com.sun.net.httpserver.HttpServer` en loopback con política permisiva inyectada: 200 válido, 3xx, 404, tipo incorrecto, cuerpo de 1 MiB + 1 byte, respuesta lenta que agota 5 s. Tests del cifrado: ida y vuelta, nonce distinto en cada escritura, clave distinta produce `SECRET_UNREADABLE`. PostgreSQL con Testcontainers para cascada del borrado, reemplazo atómico, versión perdida con `performed=false` y dos sincronizaciones concurrentes. MockMvc para las cinco rutas, DTO cerrados, precedencia de errores y `CONNECTORS_DISABLED`. PIT scope `external_calendar` (dominio, casos de uso, controlador, adaptadores de persistencia y descarga, wiring) con umbral 80 %.

Frontend: Vitest para el módulo `external-calendar-api.ts` (validación de esquemas cerrados y mapeo de errores), la pantalla `/calendario-externo` y la sección de Hoy (compuesta, cancelable, sin alterar el resumen de bloques, oculta sin suscripción, aviso ante fallo). Stryker `frontend/stryker.external-calendar.config.json` con rangos línea:columna para las ramas nuevas de `App.tsx` y `workspace.tsx`, y target `external-calendar-frontend` en `scripts/project.mjs`. E2E Playwright con `authenticated-test`: alta con URL de loopback rechazada por BLOCKED_ADDRESS, alta válida contra un servidor de fixtures dentro de la red de Compose con `app.connectors.allow-private-addresses=true` solo en ese perfil, sincronización manual, sección en Hoy con hora local, borrado con cascada, axe y matriz responsive. Ninguna prueba consulta un feed real de Internet.

PREGUNTA ABIERTA: si `APP_CONNECTOR_KEY` debe compartirse con la feature 24 (credenciales de integración en la rama `codex/integration-api`) o si cada conector tendrá su clave; esta propuesta asume una clave común y un único código `CONNECTORS_DISABLED`.

## Feature 29: additional_connectors — Catálogo de conectores y segundo gestor de issues (GitLab)

Propuesta pendiente de ratificación humana. No acredita implementación ni inicia TDD antes de revisar Gherkin.

### Propósito y priorización por uso

Entrega dos cosas verificables: un **catálogo** que muestra el estado real de cada conector del producto y un **segundo gestor de issues, GitLab**, construido sobre el mismo puerto que GitHub. El roadmap fija «adaptadores priorizados por uso» y advierte que un botón no constituye un conector funcional; por eso este corte entrega un solo proveedor completo con permisos, secretos, desconexión, mapeo, conflictos, idempotencia, límites y pruebas, en lugar de varios a medias.

GitLab se elige como segundo gestor porque comparte con GitHub el modelo exacto que 27 ya prueba: repositorio con issues abiertas, PAT personal, API REST paginada. Es el proveedor más usado como alternativa autoalojada a GitHub entre desarrolladores, perfil del propietario de esta web, y su instancia privada (`gitlab.example.com`) queda cubierta por la base de API configurable. Jira, Trello y Todoist se descartan en este corte: Jira exige autenticación distinta (correo + API token en Basic o OAuth 2.0 3LO), un modelo de proyectos/JQL sin equivalente directo y cuota por sitio; Trello usa clave + token y tarjetas en listas, no issues; Todoist es un gestor de tareas personal cuyo solape con esta web es la propia web. Ninguno valida el puerto compartido con una segunda implementación isomorfa; GitLab sí. No se atribuyen porcentajes de uso no medidos (matriz UX, fila Pareto): la prioridad se contrastará con uso real y cada proveedor descartado tendrá su feature propia si se pide.

El catálogo es un requisito de operabilidad: con seis integraciones (24–29) el usuario necesita un lugar que diga qué está conectado, cuándo actuó por última vez y qué falló, sin abrir cada pantalla. Sin catálogo, un conector con error silencioso no se distingue de uno sano.

### Modelo y persistencia

Migración reservada **`V27__additional_connectors.sql`**, sin cambios en tablas ajenas:

- `gitlab_connections(owner_id TEXT PRIMARY KEY, api_base TEXT NOT NULL, project_path TEXT NOT NULL, project_id BIGINT NOT NULL, token_ciphertext BYTEA NOT NULL, token_nonce BYTEA NOT NULL, token_hint TEXT NOT NULL, status TEXT NOT NULL CHECK (status IN ('connected','error')), last_activity_at TIMESTAMPTZ, last_error_code TEXT, last_error_at TIMESTAMPTZ, version BIGINT NOT NULL CHECK (version >= 0), created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL)`. Una conexión por propietario, simétrica a la de GitHub de 27. `token_hint` son los últimos cuatro caracteres del PAT; nunca se guarda el token en claro ni se registra en logs.
- Se **reutiliza** `task_external_links` de 27 con `source = 'gitlab'`. `external_id = "<host de api_base>:<id global de la issue>"`; el id global es único por instancia y el prefijo de host evita colisiones si el propietario cambia de instancia. `url` es `web_url` de la issue. UNIQUE `(owner_id, source, external_id)` es la única garantía de idempotencia de importación: no se añade otra tabla de deduplicación.
- El guardián `IMPORT_IN_PROGRESS` de 27 se reutiliza tal cual y se aplica **por propietario**, no por proveedor: una importación de GitHub y otra de GitLab no corren a la vez. Si 27 lo definió por proveedor, se conserva esa decisión y se anota aquí.

Mapeo issue → tarea, aplicado por el caso de uso compartido y no por el adaptador: título recortado de espacios exteriores y truncado al límite vigente de nombre de tarea de la feature 7 por puntos de código; descripción = cuerpo de la issue truncado a 4000 puntos de código, o cadena vacía; la tarea nace en el estado inicial de la feature 7 y no hereda etiquetas, asignados, hitos ni fechas. Título vacío tras recorte cuenta como `failed` con motivo `EMPTY_TITLE`. La tarea queda enlazada en la misma transacción que la crea; sin enlace no hay tarea.

### Puerto compartido `IssueSource`

En `application/` se define el puerto de salida `IssueSource` con dos operaciones: `source()` devuelve la clave estable (`github`, `gitlab`) y `openIssues(connection, page)` devuelve `IssuePage(List<ExternalIssue> issues, boolean hasMore)` para páginas de hasta 100 elementos, donde `ExternalIssue(externalId, title, body, url)` es un valor sin dependencias de framework. `connection` transporta base de API, referencia de proyecto y el token ya descifrado, sólo en memoria y sólo durante la ejecución. El adaptador traduce respuestas del proveedor a `ExternalIssue` o a las excepciones de aplicación `RateLimitedException`, `ConnectionInvalidException` y `SourceUnavailableException`; no conoce tareas ni proyectos.

El caso de uso de importación de 27 pasa a ser `ImportIssues(source)`: resuelve el `IssueSource` por clave en un mapa cableado en `ApplicationConfiguration`, lee hasta dos páginas (200 issues), marca `truncated = true` si la segunda página indica más resultados, y por cada issue llama al caso de uso de crear tarea de la feature 7 en su propia transacción junto con el enlace. El adaptador GitHub de 27 pasa a implementar este puerto; ninguna llamada HTTP ocurre dentro de una transacción de base de datos. Es una interfaz con dos implementaciones reales exigida por la arquitectura hexagonal, no una abstracción especulativa.

Adaptador GitLab: `GET {api_base}/projects/{path codificado}` al conectar, para validar token y obtener `id` y `path_with_namespace`; `GET {api_base}/projects/{id}/issues?state=opened&per_page=100&page={n}` al importar, con cabecera `PRIVATE-TOKEN`. `hasMore` se deriva de la cabecera `X-Next-Page` no vacía. Se **excluyen** las issues cuyo `issue_type` sea distinto de `issue` (incidentes, test cases, tasks de GitLab) y las que `moved_to_id` no nulo; ambas cuentan como `skipped`. Las issues confidenciales accesibles con el token se importan como cualquier otra: el propietario ya tiene acceso y la tarea es privada. Respuestas HTTP: 401/403/404 → `ConnectionInvalidException`; 429 o `RateLimit-Remaining: 0` → `RateLimitedException` conservando `Retry-After`; 5xx, tiempo agotado, redirección o cuerpo no JSON → `SourceUnavailableException`. Propiedad `app.gitlab.api-base` con valor por defecto `https://gitlab.com/api/v4`, entorno `APP_GITLAB_API_BASE`; es configuración de servidor, no entrada del usuario.

### API

Todas las rutas son privadas, autenticadas por sesión, sin caché, con CSRF y comprobación de origen existentes; errores en `application/problem+json` con código estable. Los recursos nunca devuelven el token.

- `GET /api/v1/me/connectors` → 200 `{ connectors: [ { id, status, lastActivityAt, lastError } ] }` con exactamente seis filas en orden fijo: `api_credentials` (24), `webhooks` (25), `ics_calendar` (26), `github` (27), `external_calendar` (28), `gitlab` (29). `status` ∈ `connected | not_connected | disabled | error`; `lastActivityAt` es instante UTC o `null`; `lastError` es `null` o `{ code, at }` sin mensaje libre, sin URL ni secreto. Cada feature aporta su estado mediante el puerto `ConnectorStatusProvider(ownerId)`; si una feature aún no está desplegada o su clave de cifrado falta, su fila es `disabled`. El catálogo falla cerrado: un dato no confirmado se muestra `disabled` o `null`, nunca `connected` por defecto. `api_credentials` está `connected` si existe al menos una credencial válida de 24; `lastActivityAt` sólo si 24 registra último uso, si no `null`.
- `GET /api/v1/me/connectors/gitlab` → 200 `{ status, apiBase, projectPath, projectId, tokenHint, lastActivityAt, lastError, version }`; sin conexión devuelve `status: not_connected` y el resto `null`, no 404.
- `PUT /api/v1/me/connectors/gitlab` con `{ token, projectPath }` → valida (`token` string no vacío ≤ 200 caracteres sin espacios; `projectPath` cumple `^[\w.+-]+(/[\w.+-]+){1,4}$`, sin `..`, ≤ 255), consulta el proyecto en GitLab, cifra y hace upsert; 200 con la representación anterior. Campos desconocidos → 400 `VALIDATION_ERROR`. Reemplazar un token válido por otro no borra enlaces ni tareas.
- `DELETE /api/v1/me/connectors/gitlab` → 204 idempotente. Borra la fila de conexión y la clave en memoria; conserva tareas y `task_external_links` como trazabilidad histórica.
- `POST /api/v1/me/connectors/gitlab/imports` con `{ projectId }` (UUID de un proyecto propio destino) → 200 `{ source: "gitlab", projectId, startedAt, finishedAt, created, skipped, failed, truncated }`. Antes de contactar GitLab aplica las precondiciones del proyecto destino de la feature 7 (existencia, propiedad, estado que admite tareas) y devuelve sus mismos errores. Sin cuerpo idempotente: repetir la importación crea sólo las issues nuevas y cuenta las ya enlazadas como `skipped`.

| HTTP | Código | Cuándo |
| --- | --- | --- |
| 400 | VALIDATION_ERROR | Cuerpo inválido, campos desconocidos, `projectPath` o `token` fuera de forma |
| 404 | PROJECT_NOT_FOUND | Proyecto destino inexistente o ajeno (respuesta genérica) |
| 409 | IMPORT_IN_PROGRESS | Otra importación del propietario en curso; también bloquea PUT y DELETE |
| 409 | CONNECTION_MISSING | Importar sin conexión GitLab |
| 422 | CONNECTION_INVALID | GitLab rechaza el token o no encuentra el proyecto; la conexión pasa a `error` |
| 429 | RATE_LIMITED | GitLab limita; se reenvía `Retry-After` si existe |
| 502 | SOURCE_UNAVAILABLE | GitLab no responde, 5xx, redirección o cuerpo inesperado |
| 503 | CONNECTORS_DISABLED | `APP_CONNECTOR_KEY` ausente o inválida |
| 503 | STORAGE_UNAVAILABLE | Fallo de persistencia reconocido |

**PREGUNTA ABIERTA**: si 27 nombra de otro modo el fallo de red del proveedor, 29 adopta ese código en lugar de `SOURCE_UNAVAILABLE`.

### Seguridad

Cifrado AES-256-GCM con la clave `APP_CONNECTOR_KEY` compartida con 27, nonce aleatorio de 12 bytes por escritura y datos autenticados adicionales `owner_id + ":gitlab"`, para que un cifrado no sea reutilizable en otra fila. Sin clave configurada, los conectores 27 y 29 responden `CONNECTORS_DISABLED` y su fila del catálogo es `disabled`; el catálogo sigue disponible. El token viaja sólo en la cabecera `PRIVATE-TOKEN`, nunca en la URL, en logs ni en el problema JSON; los logs registran código de error y correlación. Salida HTTPS obligatoria salvo hosts de loopback, permitidos únicamente para pruebas; sin seguir redirecciones; tiempo de conexión 5 s y lectura 10 s; cuerpo por página limitado a 5 MiB. La base de API no es editable por el usuario, lo que cierra la superficie SSRF; `projectPath` se codifica con `URLEncoder` y se valida antes de componer la URL. Se recomienda en la interfaz un PAT con alcance `read_api`; el servidor no puede verificar el alcance sin un endpoint adicional y no lo afirma.

### Concurrencia y recuperación

Una importación por propietario a la vez mediante el guardián de 27; una entrada `RUNNING` sin finalizar de más de 10 minutos se considera abandonada y no bloquea. Cada issue se confirma en su propia transacción; una caída a mitad deja tareas completas ya enlazadas y ninguna parcial, y la siguiente ejecución las cuenta como `skipped`. Un fallo de GitLab en la página 2 devuelve el recibo con lo creado hasta ese punto y `lastError` actualizado; no hay rollback de las tareas ya confirmadas y el recibo lo dice. Reemplazar o borrar la conexión durante una importación devuelve `IMPORT_IN_PROGRESS`. Respuesta de red incierta en el cliente: no se reintenta automáticamente; la pantalla ofrece «Actualizar estado», que relee `GET .../gitlab` y el catálogo. `version` de la conexión se incrementa en cada PUT y se expone para diagnóstico; el PUT no exige `If-Match` porque hay un único operador por cuenta.

### Interfaz

Ruta `/conectores`, encabezado «Conectores», entrada de navegación después de Importación sin desplazar Hoy. Lista de seis filas con nombre, estado en texto y marcador no dependiente del color, última actividad en la zona del usuario, último error como código traducido y un enlace por fila: `/integraciones` (24), y las rutas propias de 25–28 cuando existan; mientras no existan, la fila muestra «No disponible» sin enlace. Ruta `/conectores/gitlab`: formulario con `token` (`type="password"`, `autocomplete="off"`, nunca precargado), `projectPath` con ayuda «grupo/proyecto», botón «Conectar»; con conexión existente muestra `tokenHint`, proyecto y botones «Importar issues» con selector nativo de proyecto destino, «Actualizar token» y «Desconectar» con confirmación explícita que aclara que las tareas importadas se conservan. El recibo se presenta como cuatro cifras etiquetadas y aviso claro si `truncated`. Estados guardando, guardado y fallo diferenciados; ningún éxito antes de confirmación; controles de al menos 44 px, etiquetas asociadas, foco visible y anuncios `aria-live`. Reutiliza `apiRequest`, SCSS existente y controles nativos; sin dependencias nuevas ni almacenamiento del token en el navegador.

### Límites explícitos

Sin OAuth: el PAT basta para un único operador y OAuth exigiría registrar una aplicación por instancia GitLab. Sin sincronización bidireccional ni cierre de tareas al cerrar issues: la web no escribe en GitLab y no vigila cambios; repetir la importación sólo añade issues nuevas. Sin merge requests, comentarios, etiquetas, asignados ni hitos. Sin webhooks entrantes de GitLab: 25 es saliente. Sin Jira, Trello ni Todoist por las razones de la primera subsección; cada uno requerirá su feature con autenticación y mapeo propios. Sin edición de la base de API por el usuario. La exportación de 22 no incluye conexiones ni tokens; `task_external_links` sigue la decisión que 27 tome sobre exportación.

### Decisiones y alternativas descartadas

| Decisión | Alternativa descartada | Motivo |
| --- | --- | --- |
| GitLab como segundo gestor | Jira, Trello o Todoist | Único proveedor isomorfo a GitHub; valida el puerto con dos implementaciones reales y cubre autoalojado |
| Puerto `IssueSource` y `ImportIssues(source)` compartido | Caso de uso GitLab copiado del de GitHub | Un solo lugar para mapeo, límites y recibo; la mutación muerde una lógica, no dos |
| Reutilizar `task_external_links` con `source = gitlab` | Tabla `gitlab_links` propia | Misma forma, misma unicidad; una tabla menos que migrar y exportar |
| `external_id = host:id global` | `iid` del proyecto | El `iid` colisiona entre proyectos e instancias; el id global con host no |
| Base de API en propiedad de servidor | Campo por conexión | Un operador, una instancia; evita validar URLs arbitrarias (SSRF) |
| Guardián de importación por propietario | Por proveedor | Evita dos importaciones concurrentes contra el mismo proyecto destino |
| Transacción por issue | Una transacción por importación | Sin HTTP dentro de transacciones; caída deja estado consistente y reanudable |
| Catálogo con seis filas fijas y `disabled` para lo no desplegado | Sólo conectores existentes | El usuario ve el mapa completo; falla cerrado sin inventar `connected` |
| GET GitLab devuelve `not_connected` | 404 sin conexión | Una sola forma de respuesta simplifica interfaz y pruebas |
| Excluir `issue_type != issue` y movidas | Importar todo `state=opened` | Incidentes y test cases no son trabajo planificable; las movidas duplican |

### Verificación prevista

Unitarias de `ImportIssues` con `IssueSource` falso: mapeo, truncado por puntos de código, `EMPTY_TITLE`, 200 issues y `truncated`, `skipped` por enlace existente, fallo en página 2 con recibo parcial. Persistencia con PostgreSQL Testcontainers: cifrado/descifrado con AAD, UNIQUE del enlace, enlace y tarea en la misma transacción, guardián con entrada abandonada. Adaptador GitLab contra servidor falso `com.sun.net.httpserver`: codificación del `projectPath`, cabecera `PRIVATE-TOKEN`, `X-Next-Page`, filtros de tipo, 401/404/429/5xx, redirección rechazada, cuerpo sobredimensionado, tiempo agotado. MockMvc para las cinco rutas, errores de la tabla, `CONNECTORS_DISABLED` y ausencia del token en toda respuesta. Vitest para catálogo y pantalla GitLab: estados, recibo, confirmación de desconexión, foco y anuncios. E2E con pila real y servidor GitLab falso publicado en la red de Compose: conectar, importar dos veces, desconectar, catálogo coherente; axe y anchos 320/768/1440 con zoom 200 %. Mutación: PIT `-PmutationScope=additional_connectors` sobre dominio, `ImportIssues`, adaptador GitLab, controladores y `ApplicationConfiguration`; Stryker `frontend/stryker.additional-connectors.config.json` sobre `connectors.tsx`, `gitlab-connector.tsx`, sus módulos `*-api.ts` y los rangos de `App.tsx`/`workspace.tsx` de la feature. Umbral 80 %, sin rebajarlo para cerrar.

## Feature 30: automations — Reglas auditables con simulación

### Propósito y frontera

Permitir que el propietario declare reglas «cuando ocurra este evento, haz esta acción» sobre sus propios datos, con vista previa sin efectos y registro de cada ejecución. Cumple la línea «plantillas y automatizaciones auditables» de personalización. No es un motor de flujos: no hay condiciones compuestas, temporizadores, acciones destructivas ni edición de eventos. Una automatización nunca sustituye el gesto humano de completar, cerrar o replanificar.

### Modelo de regla

Una regla tiene `id` UUID de servidor, `name` (1–80 puntos de código tras recortar Unicode White_Space), `enabled`, `trigger`, `condition`, `action`, `version` y fechas. El propietario procede del principal; ningún campo del cuerpo lo fija.

- `trigger.eventType`: exactamente uno de los doce tipos publicados por `OutboxMessage`: `ProjectCreated.v1`, `ProjectUpdated.v1`, `ProjectStatusChanged.v1`, `TaskCreated.v1`, `SubtaskCreated.v1`, `TaskStatusChanged.v1`, `BlockPlanned.v1`, `BlockChanged.v1`, `WorkSessionStarted.v1`, `WorkSessionStateChanged.v1`, `WorkSessionExtended.v1`, `WorkSessionClosed.v1`. Otro valor: 400 `UNKNOWN_EVENT_TYPE`. Añadir un tipo a `OutboxMessage` no lo habilita solo: la lista de 30 es un catálogo cerrado propio que se amplía por contrato.
- `condition`: `null` o `{ projectId }`. Coincide cuando el proyecto resuelto del evento es ese proyecto propio. Resolución por tipo: `Project*`, `TaskCreated`, `SubtaskCreated` y `TaskStatusChanged` usan `aggregateId`; `WorkSessionStarted` usa `payload.projectId`; `BlockPlanned` y `BlockChanged` resuelven `payload.taskId → tasks.project_id`; `WorkSessionStateChanged`, `WorkSessionExtended` y `WorkSessionClosed` resuelven `aggregateId → work_sessions → tasks.project_id`. Un `projectId` ajeno o inexistente al guardar: 422 `TARGET_NOT_FOUND`, mismo mensaje en ambos casos.
- `action` es una de dos formas cerradas:
  - `{ type: "CREATE_TASK", projectId, titleTemplate, criterionTemplate, estimatedMinutes }`. Crea una tarea raíz en un proyecto propio mediante el caso de uso existente `CreateTask`, con sus mismas reglas (proyecto propio y no `completed`, título 1–160, criterio ≤ 2000, minutos 1–1440 o `null`). La tarea no tiene campo descripción; la «descripción» de la regla es `criterionTemplate`, que alimenta `completionCriterion` (vacío si `null`). `estimatedMinutes` es un valor fijo opcional, nunca calculado.
  - `{ type: "NOTIFY_WEBHOOK", endpointId }`. Encola en el mecanismo de entregas de 25 el evento original, sin transformar, hacia un endpoint propio de 25. Endpoint ajeno, inexistente o desactivado al guardar: 422 `ENDPOINT_NOT_FOUND`.
- Plantillas: texto plano con marcadores `{{event.type}}`, `{{task.title}}`, `{{project.name}}` y `{{occurredAt}}` (ISO-8601 UTC del evento). Cualquier otro marcador, un `{{` sin cierre o `{{task.title}}` en un trigger `Project*` (no hay tarea) produce 400 `INVALID_TEMPLATE` con `errors[]` que señala el campo. Los valores se insertan sin escapar: son texto que la interfaz muestra como texto. `task.title` y `project.name` se resuelven con el estado vigente en el instante de ejecución, no con el histórico. Longitud de plantilla cruda: título ≤ 160, criterio ≤ 2000 puntos de código; el resultado resuelto que supere esos límites no se trunca, falla la ejecución con `TITLE_TOO_LONG` o `CRITERION_TOO_LONG`.
- Máximo 20 reglas por propietario contando desactivadas; la 21.ª devuelve 409 `RULE_LIMIT` sin escritura. Borrar libera cupo.
- `version` BIGINT desde 1; `ETag: "<version>"` en cada representación. `PUT` y `DELETE` exigen `If-Match`: ausente 428 `PRECONDITION_REQUIRED`, mal formado `VALIDATION_ERROR` sobre `If-Match`, distinto de la versión vigente 412 `AUTOMATION_CONFLICT`. Activar/desactivar se hace con `PUT` completo cambiando `enabled`; no hay endpoint aparte.

### Persistencia (migración reservada V28__automations.sql)

- `automation_rules(id UUID PK, owner_id TEXT NOT NULL, name, enabled BOOLEAN, event_type TEXT, condition_project_id UUID NULL, action JSONB CHECK jsonb_typeof='object', version BIGINT CHECK ≥ 1, created_at, updated_at)`, índice `(owner_id, created_at, id)`. `action` guarda la forma cerrada validada en aplicación; la migración no interpreta plantillas.
- `automation_runs(id UUID PK, rule_id UUID NULL REFERENCES automation_rules ON DELETE SET NULL, owner_id, event_id UUID, event_type, occurred_at, attempt INTEGER CHECK 1..3, status TEXT CHECK IN ('succeeded','retry','failed'), created_task_id UUID NULL, delivery_id UUID NULL, error_code TEXT NULL, executed_at TIMESTAMPTZ, UNIQUE (rule_id, event_id))`, índices `(rule_id, executed_at DESC, id DESC)` y `(created_task_id)`. La restricción única es la idempotencia: una regla ejecuta como máximo una vez por evento aunque el cursor relea.
- Cursor de ejecución: una fila por propietario `(owner_id PK, occurred_at, event_id)` bajo el nombre de consumidor `automations`. PREGUNTA ABIERTA (depende de 25): si V23 crea una tabla de cursores por consumidor, V28 no crea otra y usa `consumer = 'automations'`; si no, V28 crea `automation_cursors`. La destilación fija la opción leyendo V23 antes del primer escenario de persistencia.

### Ejecución

Worker programado propio (`app.automations.enabled`, por defecto `false`, `fixedDelay` 1 s) que consume el puerto genérico de cola de eventos de 25 (`EventTail`/`OwnerEventCursor`): «dame hasta N eventos de este propietario posteriores al cursor, ordenados por `occurred_at, event_id`». No modifica adaptadores de commit ni `OutboxMessage`. Lee `outbox_events` con independencia del estado de publicación en RabbitMQ; omite filas `blocked`. La misma semántica de cursor que 25 aplica: un evento que confirma tarde con `occurred_at` anterior al cursor no se procesa; entrega de mejor esfuerzo, sin promesa de exhaustividad ante relojes atrasados.

Por ciclo y propietario con al menos una regla activa: primero reintentos pendientes (`status = 'retry'`, `attempt < 3`), después hasta 100 eventos nuevos. Por evento, una transacción PostgreSQL: evaluar las reglas activas en orden `created_at, id`; para cada coincidencia insertar la fila de `automation_runs` con `ON CONFLICT (rule_id, event_id) DO NOTHING` (si ya existe, no se ejecuta), ejecutar la acción y avanzar el cursor. `CREATE_TASK` invoca `CreateTask` dentro de esa transacción (propagación REQUIRED, el adaptador se une): fila de `automation_runs`, tarea y `TaskCreated.v1` confirman juntos o ninguno. `NOTIFY_WEBHOOK` inserta la entrega de 25 en la misma transacción y guarda `delivery_id`.

Fallos deterministas (`PROJECT_COMPLETED`, `TARGET_NOT_FOUND`, `ENDPOINT_NOT_FOUND`, `TITLE_TOO_LONG`, `CRITERION_TOO_LONG`) registran `failed` en el primer intento con `error_code`, sin tarea ni entrega, y no detienen las demás reglas ni el cursor. Fallos de almacenamiento o excepción inesperada revierten toda la transacción del evento y, en una transacción corta aparte, registran o incrementan la fila `retry` con `attempt + 1` y `error_code` (`STORAGE_UNAVAILABLE`, `INTERNAL_ERROR`); el cursor no avanza. El tercer intento fallido pasa a `failed`. Si ni siquiera puede escribirse el registro de reintento, el evento se vuelve a leer en el ciclo siguiente sin contar intento: no hay bucle infinito porque cada intento registrado consume cupo y un almacenamiento caído no ejecuta nada. No se emite ningún evento nuevo de tipo automatización; el único evento producido es el `TaskCreated.v1` ordinario de la tarea creada.

Una regla solo ve eventos que el cursor lee mientras está activa. Al crear la primera regla del propietario, el cursor se inicializa en el último evento existente de ese propietario: el historial anterior no se ejecuta. Reactivar una regla no procesa lo ocurrido mientras estuvo desactivada.

### Protección contra bucles

Los eventos `TaskCreated.v1` y `SubtaskCreated.v1` cuyo `payload.taskId` aparezca en `automation_runs.created_task_id` no evalúan ninguna regla; el evento se salta y el cursor avanza. Profundidad 1: la tarea creada por una automatización no dispara otra creación, pero acciones humanas posteriores sobre esa tarea (completar, planificar bloque) sí disparan reglas. `NOTIFY_WEBHOOK` no genera eventos y no necesita guarda.

Mecanismo elegido: la columna `created_task_id` de `automation_runs` con índice, consultada solo para esos dos tipos. Justificación: el dato ya existe para auditoría, no exige tabla nueva ni tocar `PostgresTaskCommit`, y `ON DELETE SET NULL` conserva la guarda cuando se borra la regla. Descartadas: marcar el origen en el payload del evento (rompe los esquemas cerrados validados por `OutboxMessage`), y una tabla `task_external_links` con `source = 'automation'` (no existe todavía, obligaría a un segundo insert en el commit de tareas y 27 podría definirla con otra semántica).

### Simulación

`POST /api/v1/me/automations/simulate` recibe el mismo cuerpo que `POST /api/v1/me/automations` (regla sin `id`), lo valida íntegramente con los mismos errores, y evalúa el trigger y la condición contra los últimos 100 eventos del propietario en `outbox_events` (orden `occurred_at, event_id` descendente, excluidas filas `blocked`). Respuesta 200 cerrada: `{ evaluatedEvents, matches: [{ eventId, eventType, occurredAt, preview }] }`, donde `preview` es `{ type: "CREATE_TASK", projectId, title, completionCriterion, estimatedMinutes, wouldFail: null | código }` o `{ type: "NOTIFY_WEBHOOK", endpointId, eventId }`. Las plantillas se resuelven con los valores vigentes; `wouldFail` anticipa `PROJECT_COMPLETED`, `TITLE_TOO_LONG`, etc. La simulación no escribe, no encola, no mueve el cursor, no aplica la guarda de bucles (informa `loopGuarded: true` en la coincidencia) y no queda en `automation_runs`. Cero coincidencias es una respuesta válida con `matches: []`. La simulación no promete que una regla guardada después coincida con los mismos eventos: el cursor arranca en el presente.

Elegida la variante con la regla en el cuerpo y descartada `POST .../{id}/simulate`: la primera sirve antes y después de guardar con un solo evaluador; la segunda obligaría a dos endpoints o a guardar para probar.

### Auditoría

`GET /api/v1/me/automations/{id}/runs?cursor=` devuelve exactamente `{ items, nextCursor }`, 20 por página, orden `executed_at, id` descendente. Cada item: `{ id, eventId, eventType, occurredAt, attempt, status, createdTaskId, deliveryId, errorCode, executedAt }` con `null` donde no aplique. Cursor opaco `(executedAt, id)` vinculado a la regla; cursor de otra regla o mal formado: `VALIDATION_ERROR`. Regla inexistente o ajena: 404 `RESOURCE_NOT_FOUND`. Los runs de reglas borradas dejan de ser consultables; las tareas creadas siguen en sus proyectos.

### API completa

Rutas privadas, autenticadas por sesión o credencial de 24 con alcance de escritura, `Cache-Control: no-store`, CSRF y `OriginGuard` vigentes, JSON estricto sin campos desconocidos. Representación de regla: `{ id, name, enabled, trigger, condition, action, version, createdAt, updatedAt }`.

| Método y ruta | Éxito | Errores propios |
| --- | --- | --- |
| `GET /api/v1/me/automations` | 200 `{ items }` (máx. 20, orden `createdAt, id`) | — |
| `POST /api/v1/me/automations` | 201, `Location`, `ETag` | 400 `VALIDATION_ERROR`/`UNKNOWN_EVENT_TYPE`/`INVALID_TEMPLATE`, 409 `RULE_LIMIT`, 422 `TARGET_NOT_FOUND`/`ENDPOINT_NOT_FOUND` |
| `GET /api/v1/me/automations/{id}` | 200 + `ETag` | 404 `RESOURCE_NOT_FOUND` |
| `PUT /api/v1/me/automations/{id}` | 200 + nuevo `ETag`, `version + 1` | los de POST salvo `RULE_LIMIT`, 428, 412 `AUTOMATION_CONFLICT`, 404 |
| `DELETE /api/v1/me/automations/{id}` | 204 | 428, 412, 404 |
| `POST /api/v1/me/automations/simulate` | 200 | los de validación de POST |
| `GET /api/v1/me/automations/{id}/runs` | 200 | 400, 404 |

401, 403 CSRF/origen, 415, 405 y 503 `STORAGE_UNAVAILABLE` siguen los filtros y códigos globales. `PUT` que no cambia nada sigue incrementando `version` (misma regla que edición de proyecto). Un `PUT` cambiando `trigger` o `action` no borra runs anteriores.

### Seguridad y privacidad

Toda lectura y escritura filtra por `owner_id`; proyectos y endpoints referenciados deben pertenecer al mismo propietario y las respuestas para ajeno e inexistente son idénticas. Las plantillas son texto: la interfaz nunca las interpreta como HTML y el servidor no evalúa expresiones. Los logs del worker registran `ruleId`, `eventId`, `outcome`, `attempt` y `code`; nunca plantillas resueltas, títulos, nombres ni payloads. `NOTIFY_WEBHOOK` no expone datos nuevos: envía el mismo evento que 25 ya puede entregar, con las mismas firma y redacción de 25. No hay acciones sobre datos de terceros ni llamadas salientes fuera del mecanismo de 25.

### Concurrencia y recuperación

Dos réplicas del worker no ejecutan la misma regla dos veces para un evento: la fila de `automation_runs` se inserta antes de actuar y `UNIQUE (rule_id, event_id)` falla la segunda dentro de su transacción. El avance del cursor comparte transacción con las ejecuciones del evento; una caída deja el cursor en el evento anterior y la relectura es idempotente. Borrar o desactivar una regla mientras el worker evalúa: la transacción del evento bloquea la fila de la regla (`FOR SHARE`) y respeta el estado que lea; un `PUT` concurrente espera o el evento se ejecuta con la versión anterior, nunca con una mezcla. Reinicio del backend no pierde reglas, runs ni cursor. Con `app.automations.enabled=false` no se lee ni escribe nada y los eventos no se acumulan como deuda: al habilitar, el cursor sigue desde donde quedó, salvo la inicialización descrita para propietarios sin cursor.

### Interfaz

Ruta `/automatizaciones`, encabezado «Automatizaciones», entrada de navegación tras Integraciones. Lista de reglas con nombre, disparador legible, destino y estado activa/inactiva, con estados vacío, cargando y error independientes. Editor con controles nativos: nombre, `<select>` de tipo de evento con etiqueta en español, proyecto opcional de la condición, tipo de acción y sus campos; ayuda inline de los cuatro marcadores; validaciones del servidor asociadas a su campo. Botón «Simular» que muestra en la misma página el número de eventos evaluados y una lista de coincidencias con la vista previa resuelta y el posible fallo anticipado; no guarda. Botón «Guardar» separado, deshabilitado durante la petición, con conflicto 412 explicado y recarga deliberada de la versión vigente conservando el borrador. Historial de ejecuciones por regla con paginación «Cargar más», estado no solo por color y enlace a la tarea creada. Activar/desactivar mediante interruptor accesible que envía el `PUT` y refleja solo la respuesta confirmada. Sin `localStorage`; el borrador vive en memoria del componente. Matriz de `docs/ux-requirements.md`: 320/768/1440, zoom 200 %, teclado, foco, 44 px, axe en Playwright; principios Tesler (la resolución del proyecto del evento la hace el sistema), Modelo mental (la tarea creada es pendiente, no trabajo hecho) y Fin de pico (simular antes de guardar) revisados con evidencia.

### Límites explícitos

Sin condiciones compuestas, comparadores distintos de «proyecto igual a», ni filtros por texto. Sin programación temporal, cron, retrasos ni recurrencia. Sin acciones destructivas ni de estado: no completa, reabre, borra, replanifica ni cierra sesiones. Sin edición, supresión o reemisión de eventos. Sin encadenar automatizaciones (profundidad 1). Sin plantillas con lógica, formato de fechas ni expresiones. Sin acciones hacia terceros fuera de los endpoints de 25. Sin reintento manual de runs `failed` en este corte. Sin importación/exportación de reglas en 22/23.

### Decisiones y alternativas descartadas

| Decisión | Alternativa descartada | Motivo |
| --- | --- | --- |
| Consumir el puerto de cola de eventos de 25 | Cursor y lector propios de 30 | Un solo mecanismo de cola por consumidor; menos código y una sola semántica de orden |
| Cursor por propietario con nombre de consumidor `automations` | Cursor por regla | 20 cursores por propietario multiplican estados; la unicidad `(rule_id, event_id)` ya da idempotencia por regla |
| Guarda de bucles vía `created_task_id` | Marcar el payload o tabla `task_external_links` | No toca esquemas cerrados ni commits existentes; dato ya necesario para auditoría |
| Fallos deterministas sin reintento; 3 intentos solo para fallos transitorios | 3 intentos uniformes | Reintentar `PROJECT_COMPLETED` no cambia el resultado y ensucia el historial |
| Resultado resuelto demasiado largo falla | Truncar en silencio | Falla cerrado ante dato no confirmado; la simulación lo anticipa |
| Simulación con la regla en el cuerpo | `POST .../{id}/simulate` | Un evaluador y un endpoint; permite probar antes de guardar |
| `PUT` completo para activar/desactivar | `PATCH` o endpoint `/enable` | Reutiliza ETag y validación sin superficie extra |
| Borrado físico con `ON DELETE SET NULL` en runs | Borrado lógico de reglas | Libera cupo real y conserva la guarda de bucles sin estado «borrada» en la API |
| Cursor inicial en el presente | Procesar backlog al crear la primera regla | Evita crear cientos de tareas por historial antiguo; coherente con «una regla ve el futuro» |
| Entrega doble posible si el endpoint ya está suscrito al tipo en 25 | Prohibir la combinación | El receptor deduplica por `eventId` según el contrato de 25; prohibirlo acopla validaciones de dos features |

### Verificación prevista

Unit: motor de plantillas (marcadores válidos, desconocidos, sin cierre, por tipo de trigger, límites de longitud por puntos de código) y evaluador (trigger, condición, resolución de proyecto por tipo, guarda de bucles) con reloj inyectado y sin Spring. Integración PostgreSQL Testcontainers: V28, unicidad `(rule_id, event_id)` con dos ejecutores concurrentes, atomicidad run+tarea+evento con fallo inducido, cursor tras caída, reintentos hasta `failed`, `ON DELETE SET NULL`, límite de 20. MockMvc `AutomationsApiTest`: tabla completa de errores, ETag/If-Match, JSON estricto, simulación sin escrituras. Frontend Vitest: editor, simulación, historial, conflicto 412, estados vacío/error. E2E Playwright con worker habilitado: crear regla, provocar el evento, ver la tarea y su run; matriz responsive y axe. PIT scope `automations` (dominio, casos de uso, controlador, adaptador y worker) y `frontend/stryker.automations.config.json`, ambos con umbral 80 % y sin timeouts contados como muertos. Los escenarios que dependan del puerto de 25 usan un doble del puerto hasta que 25 esté implementada; ningún escenario de 30 se declara verde por vacuidad si 25 no existe.

## Enmiendas de seguridad a las features 24–28 — 9 de septiembre de 2026

Una revisión de seguridad de solo lectura aplicó las listas `springboot-security` y `security-review` de ECC sobre el código de la feature 24 ya integrado y sobre el diseño de 25 a 28 mientras se implementaban. Encontró 18 hallazgos, uno de severidad alta. La evidencia completa, con archivo y línea, está en `progress/security_review_connectors.md`. El coordinador aprueba las siguientes enmiendas dentro de la autorización global; prevalecen sobre el texto de las secciones 25 a 28 escrito antes.

**B1, alta.** La feature 25 mandaba enviar el webhook dentro de la transacción de reclamación, con cinco segundos de conexión y cinco de lectura y sin tope de cuerpo de respuesta. Un receptor que entregue un byte cada cuatro segundos nunca dispara el plazo de lectura, fija el hilo del trabajador y mantiene abierta una transacción PostgreSQL sin fin; con el tiempo de espera de conexión de Hikari en tres segundos eso agota el pool y deja la aplicación entera devolviendo 503. El envío pasa a ocurrir fuera de la transacción, mediante una columna de arrendamiento sobre la entrega, con plazo total del intercambio además del de lectura y con un suscriptor de cuerpo que aborta pasados 64 KiB. Se fija cinco segundos de conexión y diez de plazo total, resolviendo la contradicción del texto anterior.

**B2 y B3.** La lista de direcciones prohibidas de la guardia contra peticiones a la red interna se amplía con `0.0.0.0/8`, `192.0.0.0/24`, `192.88.99.0/24`, `198.18.0.0/15`, `240.0.0.0/4`, `255.255.255.255`, `64:ff9b::/96` y `2002::/16`, normalizando antes las formas mapeada, compatible, 6to4 y NAT64; la feature 28 incorpora además `100.64.0.0/10`, que solo la 25 bloqueaba. La política vive en una sola clase compartida por 25, 27 y 28. Se resuelve el nombre una vez, se validan todas las direcciones devueltas y se abandona la petición antes de abrir ninguna conexión si una sola está bloqueada. La petición viaja después contra la dirección literal ya validada, conservando el nombre original en la cabecera `Host` y en la indicación de servidor de TLS: **el reenlace de nombres entre la comprobación y el uso queda cerrado**, no aceptado. Decisión del propietario del 10 de septiembre de 2026, que zanja una contradicción entre carriles: la 25 revocó el anclaje el día 9 —hallazgo 5 de su dictamen— por temer que anclar rompiera la verificación del nombre del certificado, y la 28 lo implementó media hora después sin verlo. **Se ancla en las dos, y se prueba el TLS**, que era la intención original de esta enmienda. El temor está medido y descartado: `AnchoredConnection` conserva el nombre en SNI y la verificación por nombre de HTTPS, y hay oráculo en las dos features de que un certificado válido para el nombre se acepta aunque la conexión vaya a la dirección (`JdkWebhookSenderTest`, `HttpCalendarFeedTest`) y de que se rechaza si el nombre no coincide o nadie lo avala. El precio, escrito y no escondido: `jdk.httpclient.allowRestrictedHeaders=host`, HTTP/1.1 en las salidas —sobre HTTP/2 la autoridad la fija la URI y el receptor vería la dirección— y, si el nombre resuelve a varias direcciones, se usa la primera sin reintentar las demás. La política de egreso de `deploy/EGRESS.md` deja de ser la contención principal y pasa a ser defensa en profundidad.

**B4.** La firma de webhooks cubre el instante y el cuerpo, pero el contrato pedía al receptor deduplicar por una cabecera que viaja sin firmar, de modo que quien capture una entrega puede reproducirla cambiándola. La documentación pública pasa a exigir que se verifique la firma primero, que se deduplique por el identificador de evento que va dentro del cuerpo firmado, que se rechacen instantes con más de trescientos segundos de diferencia y que la comparación de la firma sea en tiempo constante.

**B5 y B6.** La clave de cifrado de secretos era única y sin identificador de versión, así que rotarla inutilizaba en silencio todo lo guardado. El texto cifrado se prefija con un byte de versión y se leen la clave vigente y la anterior, de modo que una rotación descifre lo antiguo y vuelva a cifrar en la siguiente escritura. Los datos autenticados asociados atan el texto cifrado al propietario además del identificador del recurso. Política unificada de las tres features: fallo rápido al arrancar si la clave está mal formada, modo degradado con `CONNECTORS_DISABLED` solo si está ausente.

**B11.** La base de la interfaz de GitHub se valida al arrancar contra una lista fija, la oficial más el bucle local para pruebas, y el arranque falla si no encaja. Ese valor nunca se toma de nada con alcance de petición, porque a él se envía el token personal del usuario.

**B7, B8 y B9.** La ruta pública del calendario de la feature 26 exige, en el mismo cambio que la introduce, un bloque propio en la configuración del proxy que la reenvíe al backend. Sin él, todo lo que no cuelga de la interfaz de programación cae en la entrega de la aplicación de una sola página y la ruta devolvería la página con código 200, sin las cabeceras prometidas y sin llegar siquiera al servicio. Ese bloque incorpora limitación de tasa y la supresión del registro de accesos, porque el token de capacidad viaja en la ruta.

**Pendiente del coordinador, no de los carriles.** La entrada de sesión por formulario no tiene hoy ninguna protección contra adivinación de contraseña, ni en la aplicación ni en el proxy, y cada intento cuesta una verificación de contraseña deliberadamente costosa. La corrección es limitación de tasa por dirección en el proxy. No se aplica mientras haya carriles ejecutando pruebas de extremo a extremo, porque cambiar esa configuración reconstruye la imagen web de todas sus pilas y puede volver inestables sus suites. Se entrega tras integrar los carriles y antes de cualquier despliegue.

## Enmienda normativa: orden canónico de la navegación principal

Ratificada por el propietario el 9 de septiembre de 2026. Resuelve una
contradicción a tres bandas: las features 25 y 30 reclamaban ambas la entrada
«tras API para integraciones», y la feature 24 tiene pruebas que exigen que
«API para integraciones» sea el **último** enlace del menú. Los tres enunciados
no eran satisfacibles a la vez.

**Ampliación ratificada por el propietario el 10 de septiembre de 2026.** La
redacción anterior fijaba doce entradas y omitía «Calendario externo», la de la
feature 28, que se integró después de redactarse: la enmienda original sólo
arbitraba el choque entre la 25 y la 30. Además ordenaba «Calendario» antes que
«Exportación», al revés de lo que la aplicación sirve. Puesta la contradicción
delante del propietario, resolvió **extender la enmienda al orden servido**, y
no mover la aplicación. Trece entradas, por tanto.

El orden canónico y completo de la navegación principal es:

1. Hoy
2. Proyectos
3. Disponibilidad
4. Historial
5. Revisión semanal
6. Calendario externo
7. Apariencia
8. Exportación
9. Calendario
10. Importación
11. Conectores
12. API para integraciones
13. Webhooks
14. Automatizaciones

**Segunda ampliación, 10 de septiembre de 2026.** La feature 29 añade
«Conectores», y su propio contrato (`features/additional_connectors.feature`,
@s33) sitúa la entrada **después de «Importación»**, no al final. Prevalece el
escenario ratificado de la feature sobre la costumbre de añadir al final, de
modo que «API para integraciones», «Webhooks» y «Automatizaciones» bajan una
posición. Catorce entradas.

«Webhooks» (feature 25) y «Automatizaciones» (feature 30) van detrás de «API
para integraciones» en orden de número de feature, que es lo que ambas
implementaciones ya hacen. El conector GitHub (feature 27) no añade entrada, por
contrato propio.

Que esta contradicción viviera días sin que nadie la viera tiene una causa
concreta, anotada aquí porque volverá a pasar: **las pruebas que vigilaban la
navegación lo hacían por posición** —`nth(-3)`, `last()`— y esas aserciones
siguen pasando aunque el orden entero cambie, mientras lo que miran siga donde
estaba. Sólo al sustituirlas por la lista completa salió a la luz. De ahí la
consecuencia obligatoria que viene a continuación.

**Consecuencia obligatoria para las pruebas.** Toda aserción que fije la
posición de un enlace por índice —`at(-1)`, `last()`, `length - 2`, un recuento
fijo de enlaces— queda prohibida a partir de esta enmienda: es exactamente lo
que ha roto tres carriles esta noche. Las pruebas de la feature 24 y su E2E, que
hoy afirman que «API para integraciones» es el último enlace, deben reescribirse
para afirmar el **orden relativo por nombre**, que es la propiedad que de verdad
les importa y la única que sobrevive a que otra feature añada su entrada.
