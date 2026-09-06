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
