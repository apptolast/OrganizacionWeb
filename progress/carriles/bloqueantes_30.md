# Motivos bloqueantes del panel — feature 30-automatizaciones

Extraidos integros de progress/panel_precierre_27_30.md. Cada motivo lleva
QUE (el hallazgo) y POR QUE BLOQUEA (por que un juez rechazaria el cierre).

## Lente: Cobertura del contrato: recorrido escenario a escenario de features/automations.feature (43 escenarios, ~120 filas de Examples) comprobando que cada cláusula tiene un oráculo que puede fallar

### M1

**Que:** @s4 fila 2 («C completed → 201 y la regla se guarda») no tiene oráculo en ningún sitio del árbol. La única producción que decide esa fila es el literal SQL del bean automationTargets: "SELECT 1 FROM projects WHERE id = ? AND owner_id = ?", cuyo Javadoc dice explícitamente «whatever its status». El helper del único test de integración que ejerce ese bean, AutomationWiringTest.project(String owner, String status), acepta un estado — y jamás se le pasa "completed": las tres llamadas usan "active". Grep de "completed" en AutomationWiringTest: cero resultados; el único UPDATE ... status='completed' del árbol está en AutomationPersistenceTest:382 y ataca AutomationFacts.projectCompleted, que es otro puerto.

**Por que bloquea:** Añadir " AND status = 'active'" a esa cadena rompe @s4 fila 2 (la regla dejaría de poder crearse), y con ella la premisa entera de @s21 fila 1 y @s32 fila 1 —PROJECT_COMPLETED en ejecución y en simulación sólo existe si la regla se pudo guardar antes—, y toda la suite sigue verde. Es exactamente el modo de fallo que esta noche ya ha cobrado diez veces: literal de SQL que PIT no muta y ningún test discrimina. Además es la fila que separa «error de guardado» de «fallo determinista de ejecución», que es la distinción sobre la que se apoya la mitad del bloque de ejecución del contrato.

### M2

**Que:** Tres pruebas etiquetadas @s43 sancionan como correcto un estado del que la interfaz no se recupera, y su oráculo no lo puede detectar. save(), simulate() y toggle() comparten el mismo writeRequest.current (automations.tsx:254, :296, :319). Cuando una supera a otra, el perdedor sale por `if (!mounted.current || writeRequest.current !== controller) return;` y su `finally` —`if (mounted.current && writeRequest.current === controller) setSaving(false)`— no se ejecuta nunca. `saving` (o `busyToggle`) queda en true de por vida: `disabled={saving}` en el botón Guardar (:534) y la guarda de entrada `if (!editing || saving) return;` (:253) dejan Guardar inerte para siempre, con el borrador atrapado dentro del editor; igual con `disabled={busyToggle === rule.id}` (:433). Las tres pruebas ejecutan literalmente ese camino (Guardar → Simular; Simular → Guardar; interruptor → Simular) y sólo afirman queryByRole("alert") ausente y que el borrador/el texto siguen ahí. Ninguna comprueba que Guardar o el interruptor vuelvan a ser usables.

**Por que bloquea:** Dos cosas a la vez. Primera: los Examples de @s43 son «navega a /proyectos», «cierra sesión» y «cambia a otra regla» — «otra escritura la supera» no está en el contrato, así que estas tres pruebas inflan la cobertura aparente de un escenario con una situación que no le pertenece, y de paso dan por bueno el defecto. Segunda: la cláusula que sí está en el contrato, @s40 fila 1 («el botón queda deshabilitado HASTA la respuesta»), se incumple: aquí queda deshabilitado para siempre. La prueba de @s40 (:305) sólo mide el caso sin interferencia, y las de @s43 miden el caso con interferencia sin mirar el botón. Entre las dos no hay ninguna aserción que pueda fallar por esto. Está anotado como conocido, pero un juez no acepta que además esté congelado en las pruebas como comportamiento esperado.

### M3

**Que:** @s41 «la fila con createdTaskId contiene un enlace a la tarea creada»: el fixture pone createdTaskId igual al id del proyecto (`createdTaskId: status === "succeeded" ? PROJECT : null`, con PROJECT = "11111111-1111-4111-8111-111111111111", que es también rule.action.projectId), y la aserción espera `/proyectos/${PROJECT}/tareas/${PROJECT}`. Los dos segmentos del href son el mismo UUID.

**Por que bloquea:** El oráculo no discrimina: si la producción intercambiara los dos identificadores, o usara projectId en los dos segmentos, o createdTaskId en los dos, la prueba pasa igual. La cláusula «enlace a la tarea creada» queda sin comprobar. Y hay una rama de producción adyacente que nadie ejecuta: cuando la regla es NOTIFY_WEBHOOK el segmento de proyecto se resuelve a "" y el href sale como /proyectos//tareas/<id>. Basta cambiar el fixture a dos UUID distintos para que el oráculo empiece a medir.

### M4

**Que:** @s31 filas 3 y 4 —la ventana de «los últimos 100 eventos»— no tienen oráculo. El único sitio donde se afirma el 100 es `assertThat(requestedLimit).isEqualTo(SimulateAutomation.WINDOW)`, que compara el valor pedido contra la propia constante de producción; y ningún fixture de SimulateAutomationTest pasa nunca de 5 eventos, así que ni «evaluados 100 de 130» ni «100 coincidencias, sin la más antigua» se recorren jamás. Contrasta con @s34, donde el mismo patrón sí discrimina: ReadAutomationRunsTest:56 afirma `hasSize(5)` sobre un fixture de 25 ejecuciones, y eso clava PAGE_SIZE=20 aunque la primera aserción use la constante.

**Por que bloquea:** Es una aserción autorreferencial sobre la única cifra que esas dos filas del Outline afirman. Atenuante que le baja la gravedad: AutomationPersistenceTest:308-326 sí ejerce el ORDER BY ... LIMIT real con límite 2 sobre 4 filas, así que el mecanismo «los más recientes primero, hasta el límite» está probado; lo que no está probado es que el límite que pide el caso de uso sea 100.

### M5

**Que:** @s36 fila 5 («POST /api/v1/me/automations/simulate con Origin ajeno → 403») no tiene prueba propia. El único test de origen ajeno usa PUT /api/v1/me/automations/{id}, y el único test de CSRF ausente usa POST /api/v1/me/automations. La ruta /simulate —la única POST del contrato que no escribe, y por tanto la candidata natural a que alguien la excluya del guardián— nunca se prueba contra Origin ni contra CSRF.

**Por que bloquea:** Riesgo real bajo, porque SecurityConfiguration:127-130 aplica csrf().withDefaults() y el OriginGuard a toda la cadena sin ignoringRequestMatchers, así que hoy la protección es estructural. Pero la fila del contrato existe precisamente para fijar que /simulate no se exceptúa, y hoy no hay nada que lo impida sin que la suite lo note.

### M6

**Que:** Toda la evidencia de @s42 (matriz responsive de 320/768/1280/1440, texto al 200 %, zoom nativo, objetivos de 44x44 y axe sin serious/critical) vive sólo en Playwright, y `bin/harness test` no lo ejecuta: scripts/project.mjs:478-481 corre `node --test scripts/project.test.mjs`, la tarea de Gradle y `pnpm --dir frontend test` (vitest). Los ficheros e2e sólo aparecen en la tarea `lint`, y ahí únicamente como `node --check` (comprobación de sintaxis). No hay ninguna mención a e2e en init.sh, init.ps1, bin/harness ni harness.config.json.

**Por que bloquea:** Las pruebas de @s42 están bien escritas —recorrido con Tab real contra el orden del DOM leído, anillo medido en cada parada, 44x44 en los cuatro anchos, axe en claro y oscuro—, pero el escenario no está cubierto por la puerta que cierra la feature: nada de eso corre en `bin/harness test` ni en `bin/harness verify`. Es una convención del repositorio que afecta también a webhooks y external_calendar, no un fallo de esta noche; por eso no lo marco bloqueante. Debe quedar escrito antes de poner `done`, junto a la condición 4 del juez.

### M7

**Que:** @s6 filas 4 y 5 y @s17 tercera cláusula quedan sin oráculo. En @s6, «criterionTemplate de exactamente 2000 puntos de código» y `criterionTemplate ""` nunca se envían por la API para comprobar el «se guardan byte a byte como se enviaron»: el único test de @s6 usa la fila de las llaves sueltas, y AutomationDraftTest sólo comprueba que 2000 se acepta en el constructor (el criterio vacío no se prueba en ninguna parte, aunque CreateTaskAction sí lo permita: la guarda de isEmpty() está sólo sobre titleTemplate). En @s17, «las 3 tareas creadas tienen createdAt en ese mismo orden» no tiene nada: AutomationEffect.CreateTask no lleva instante y el unitario sólo afirma hasSize(3).

**Por que bloquea:** Cláusulas del contrato sin ninguna aserción. La de @s17 ya está registrada por el juez como H3; la de @s6 no está en su dictamen porque su revisión sólo entró en los nueve escenarios del ejecutor. Ninguna de las dos justifica por sí sola parar el cierre, pero las dos deben quedar anotadas como deuda con destinatario, no desaparecer.

## Lente: seguridad y datos

### M8

**Que:** `queue()` confunde «el endpoint ya no es un endpoint activo de este propietario» con «otro worker se me adelantó», y el ejecutor descarta el evento entero en silencio, sin fila de ejecución. El INSERT de la entrega no tiene ON CONFLICT y el id de entrega es un UUID recién sorteado: `affected == 0` NUNCA puede significar un reclamo perdido. Sólo puede significar que el SELECT no casó, es decir que `e.owner_id`/`e.status='active'` fallaron (o que la fila de outbox no está). Aun así se lanza `AutomationClaimedException`, que `ExecuteAutomations:89` interpreta como «ya lo hizo otro» y responde `return true` SIN llamar a `record()`. La transacción `confirming` ya ha revertido: se pierde la ejecución de la regla de webhook, la de CUALQUIER otra regla que casara el mismo evento (p. ej. un CREATE_TASK ya aplicado en el mismo `apply()`), su tarea, su TaskCreated.v1 y el avance del cursor. Pero el paseo continúa, y el commit del evento siguiente hace UPSERT del cursor a su posición absoluta (`advance`, :212): el evento saltado no se vuelve a leer jamás. El contrato pide `attempt 1, status failed, errorCode ENDPOINT_NOT_FOUND` (features/automations.feature:282 y filas 4-5 de :291-292) y una fila por (regla, evento) (:263); aquí no queda ni una línea de bitácora, porque `log()` sólo se llama tras un commit bueno. No hace falta un atacante: el worker de la feature 25 desactiva endpoints solo (`PostgresWebhookWork.disable`, :108-121) en el mismo proceso, así que la ventana entre `endpoints.isActiveEndpointOf` (ExecuteAutomations:178, fuera de transacción) y el INSERT es una condición de producto normal, no exótica.

**Por que bloquea:** Es pérdida silenciosa y permanente de ejecuciones, justo la promesa del encabezado del contrato («sin perder el control de lo que se ejecutó y por qué»), y viola dos cláusulas explícitas (@s21 filas de webhook y el invariante de :263). Además NO tiene ninguna prueba: `AutomationClaimedException` no aparece en ningún test del árbol; la única prueba que llega a este SQL es el camino feliz de @s26 (AutomationExecutionTest.java:133-161), con un endpoint activo del propietario. Y por ser una rama guardada por literales SQL, PIT no le generará mutante ni con la campaña que exige la condición 2 del juez.

### M9

**Que:** Los cuatro predicados de aislamiento/estado del `AutomationWork` son literales de SQL sin ningún oráculo: `window()` `WHERE owner_id = ?` (:271-274), `withTheirRuns` `WHERE owner_id = ?` (:252-256), `queue()` `e.owner_id = ? AND e.status = 'active'` (:167) y `claim()` `AND status = 'retry'` (:192). `PostgresAutomationWork` sólo se ejercita a través de `execute.runCycle()` en `AutomationExecutionTest`, y CADA test de esa clase crea un propietario nuevo y único (`owner()`, :250-258): no hay un solo test en el que el ciclo de A corra mientras B tiene eventos, reglas o endpoints que casen. `AutomationPersistenceTest` sí hace ese trabajo, y bien, pero sólo para `PostgresAutomationStore`, `PostgresAutomationRuns` y `PostgresAutomationEvents` (`recent(owner(),100)` vacío, `projectOfTask(owner(),…)` vacío, `createdByAutomation(owner(),…)` falso). El `AutomationWork` se quedó fuera. La rama de reintento de `claim()` (attempt > 1) no se ejecuta NUNCA contra Postgres: @s20/@s22 viven enteros en `ExecuteAutomationsTest` con `FakeWork`.

**Por que bloquea:** Es exactamente el punto ciego que esta noche ha destapado diez casos: PIT no muta cadenas, así que la campaña que la condición 2 exige sobre esta misma clase informará cobertura alta y no generará un solo mutante para ninguno de los cuatro. Borrar `AND e.owner_id = ?` de :167 dejaría la suite verde y entregaría el payload de A a la cola de entregas de B, firmado con el secreto de B y enviado a la URL de B — el `INSERT … SELECT` toma `e.owner_id` del endpoint y `o.payload` del evento de A. Hoy hay cinturón (la comprobación en Java) y tirantes (el SQL), pero los tirantes no los sujeta ninguna prueba, y `AND status = 'retry'` es justo la guarda cuya ausencia el juez señaló como H6 en `upsert()`.

### M10

**Que:** `runCycle()` no aísla el fallo de un propietario del resto. `for (var owner : work.ownersWithRules()) walk(owner);` no envuelve nada, `walk()` deja escapar lo que lancen `work.cursor`, `startCursor` y `work.after`, y `process()` calcula `outcomes` (con `matcher.loopGuarded`, que llama a `AutomationEvent.uuid("taskId")` → `IllegalArgumentException` si el payload no lo trae) FUERA del try; además `rows.forEach(work::record)` (:96) va dentro del catch, sin protección. Cualquier fallo atribuible a un propietario aborta el ciclo para todos los que `SELECT DISTINCT owner_id` devuelva después de él. `AutomationSchedule.tick` lo traga y registra sólo `failure.getClass().getSimpleName()`: ni propietario, ni evento, ni causa.

**Por que bloquea:** Es aislamiento entre propietarios en su forma de disponibilidad: un dato envenenado de un propietario deja sin automatizaciones a todos los demás, indefinidamente y sin diagnóstico posible (una línea de warn por segundo con el nombre de una clase). Ningún test corre un ciclo con más de un propietario; la cláusula «sin detener las demás reglas» de @s21 se afirma sólo dentro de un evento de un propietario, así que el ámbito mayor no tiene oráculo.

### M11

**Que:** `queue()` sella `next_attempt_at`, `created_at` y `updated_at` de la entrega con `notify.event().occurredAt()`, el instante del EVENTO, no el instante en que se encola. En el propio escenario @s15 (worker apagado, se enciende y camina el atraso) toda entrega de automatización nace con las tres marcas en el pasado. El `created_at` que la feature 25 muestra al propietario deja de ser cuando se creó la entrega, y el índice `webhook_deliveries_due (next_attempt_at, id) WHERE status='pending'` (V23__webhooks.sql:53-54) las coloca por delante de todas las entregas legítimas.

**Por que bloquea:** El oráculo de @s26 (AutomationExecutionTest.java:143-150) afirma endpoint_id, event_id, event_type, status y body — ninguna de las tres marcas de tiempo. Nada notaría que están mal, ni en un sentido ni en el otro, así que es una tercera conducta de este adaptador que depende de un literal sin ningún testigo.

### M12

**Que:** El editor no tiene control para `action.projectId` ni para `action.estimatedMinutes`: los únicos controles son `automation-name`, `automation-trigger`, `automation-condition`, `automation-title` y `automation-criterion`. Una regla nueva apunta siempre a `projects[0]?.id ?? ""` (`blank`, :91-102). Si la lista de proyectos aún no ha llegado o está vacía, Guardar envía `projectId: ""`, el servidor responde 400 sobre `action.projectId`, y `controlIdOf` no tiene entrada para ese campo: cae en el `?? "automation-name"` y cuelga el mensaje del servidor — y mueve el foco — al campo Nombre. Lo mismo con `action.estimatedMinutes` y `action.type`.

**Por que bloquea:** @s38 pide que el editor «asocie cada error del servidor a su campo», y aquí un error de destino se atribuye visiblemente a otro campo, con el foco incluido; la prueba de @s38 sólo ejercita `action.criterionTemplate`, que sí está en el mapa, así que la rama por defecto de `controlIdOf` no tiene oráculo. Es menor porque no filtra nada y sólo desorienta.

## Lente: Lo que se rompió y lo que se tocó deprisa: historial reciente de los ficheros de la feature, cambios de producción sin prueba que los cubra, pruebas que no pueden fallar, aserciones relajadas y arreglos que silencian el síntoma. Contrastado contra los informes de mutación medidos (backend/build/reports/pitest-automations/mutations.xml y frontend/reports/mutation-automations/mutation.json), no contra las bitácoras.

### M13

**Que:** La condición 2 del juez —BLOQUEANTE— está incumplida: no existe la lista nominal de supervivientes con veredicto escrito. Para el backend no hay ningún progress/mutation_automations_*.md; progress/mutacion_cinco_features.md sólo da conteos por clase. Los 26 mutantes no muertos del informe (14 SURVIVED + 12 NO_COVERAGE) no tienen veredicto en ninguna parte del repositorio. No es burocracia: leer esa lista es exactamente lo que destapa los cuatro hallazgos siguientes, y ninguno estaba visto.

**Por que bloquea:** El juez condicionó la aprobación a registrar cada superviviente con un veredicto escrito, precisamente porque el modo de fallo de este proyecto no es la puntuación baja. La puntuación se publicó (96 %) y la lista no se leyó.

### M14

**Que:** Prueba placebo con nombre que promete lo contrario. `s3_acceptsAConditionOverAnOwnProject` monta `when(create.create(eq("owner"), any())).thenReturn(rule(1, conditioned))` y luego afirma `$.condition.projectId`: está comprobando el eco de su propio stub, no lo que el parser produjo. PIT lo acredita: `AutomationBody::condition L48 replaced return value with null` SOBREVIVE. Con ese mutante, una regla que el propietario acota a un proyecto se guarda como «cualquier proyecto» —se dispara en todos sus proyectos— y toda la suite sigue verde. El único test que sí verifica el draft entregado al caso de uso (`verify(create).create(eq("owner"), eq(draft()))`) usa un draft con condition null, así que la rama no nula no la mira nadie.

**Por que bloquea:** @s3 es una cláusula de alcance: una regla acotada que pasa a global es ampliación silenciosa de efectos sobre datos del propietario. La prueba que la cubre no puede fallar, y el informe de mutación lo dice desde las 04:26 sin que nadie lo leyera.

### M15

**Que:** `ExecuteAutomations` L143, `lambda$attemptOf$0 -> true`, SOBREVIVE. Es el filtro `run -> rule.id().equals(run.ruleId())` que escoge la ejecución previa DE ESTA regla entre las del evento. Ningún test del árbol le da a un evento ejecuciones previas de más de una regla: `withRuns(...)` recibe un único run y todos los montajes usan `givenARuleThatCreatesTasks()` con una sola regla. Con el mutante, si R1 quedó `failed` sobre E y R2 sigue en `retry`, R2 se salta para siempre (o hereda el runId y el número de intento de R1). Es justo el cruce de @s21 («sin detener las demás reglas») con @s22 (el reintento), y no tiene oráculo.

**Por que bloquea:** Es el motor, probado con un doble en memoria y en milisegundos: ahí el juez dijo que un superviviente no tiene coartada. El fallo que deja pasar es pérdida permanente y silenciosa de una automatización del propietario.

### M16

**Que:** Cinco mutantes NO_COVERAGE en `PostgresAutomationWork`: `record L126` (dos: el `executeWithoutResult` y el `upsert` de dentro), `startCursor L85`, `write L341` y `runOf L316`. O sea: el `upsert` con `ON CONFLICT (rule_id,event_id) DO UPDATE` —exactamente el H6 del juez, la escritura que puede pisar el `succeeded` de otro worker—, la creación del cursor inicial de un propietario nuevo (@s16) y el RowMapper que relee ejecuciones no los ejecuta ninguna prueba, ni unitaria ni con contenedor. @s16, @s20 y @s22 se sostienen sólo sobre el doble `FakeWork`.

**Por que bloquea:** El juez pidió por escrito una decisión sobre H6 (arreglar o justificar) y exigió que esta clase recibiera mutantes. Recibe 38 y cinco caen en código que nadie ejecuta, incluida la línea del hallazgo. La mitad de recuperación del adaptador real —reintento, cursor inicial, relectura— está sin probar.

### M17

**Que:** `AutomationView$Rule::enabled -> true` SOBREVIVE y no hay una sola aserción de `enabled:false` en toda `AutomationsApiTest`. La fila 1 de @s12 («enabled false → la lectura devuelve enabled false») no tiene oráculo en la capa HTTP; sólo en la de aplicación (`AutomationWiringTest.java:117`, sobre el draft, no sobre el JSON). Es una familia: los accesores de `AutomationView` sólo se afirman en el caso feliz CREATE_TASK — también sobreviven `Run::deliveryId`, `Match::loopGuarded`, `WebhookPreview::eventId`, `TaskPreview::completionCriterion` y `AutomationRun::deliveryId`.

**Por que bloquea:** El interruptor del frontend se pinta con ese campo del JSON. Una regla pausada mostrada como activa es la clase de fallo que el contrato nombra explícitamente en @s12, y el juez dio @s12 por cerrada salvo la fila 4.

### M18

**Que:** La cifra publicada «96,00 %» no está medida. El `mutations.xml` da 581 muertos de 607 = **95,72 %**; el 96 sale del entero redondeado del index.html de PIT. Dos decimales que nadie calculó, en un documento cuyo encabezado dice «todas las cifras están medidas, no declaradas». Además la campaña es de las 04:26 y el superviviente de `AutomationConfiguration` se mató a las 04:28 (5309e409): la cifra publicada ya no describe el árbol.

**Por que bloquea:** Es exactamente la infracción de AGENTS.md:51 que el juez persiguió en H5, en pequeño y en el documento que hace de puerta.

### M19

**Que:** `Slf4jAutomationAudit` está ahora en el ámbito de PIT pero recibe **cero** mutantes: no aparece ni una vez en el XML. La condición 2 decía «clases que tienen que recibir mutantes sí o sí». La explicación del documento («ningún adaptador de bitácora los recibe: es una propiedad de los mutadores») acierta en el fondo pero no nombra la causa —el filtro `FLOGCALL` de PIT, activo por defecto, que descarta las mutaciones dentro de llamadas a logging— ni cierra el hueco: a diferencia de la feature 25, que escribió `Slf4jWebhookAuditTest`, aquí no hay test unitario del adaptador. La línea que afirma @s19 sigue medida por una campaña que no le genera mutantes.

**Por que bloquea:** Meter el patrón satisface la letra de H7 y no su motivo. La condición se declaró cerrada; la clase sigue sin un solo mutante.

### M20

**Que:** `ExecuteAutomations` L97 sobrevive dos veces (`removed call to List::forEach` y, dentro del lambda, `removed call to ::log`): la bitácora del camino de fallo se puede borrar entera sin que caiga ninguna prueba. El contrato sólo exige el log en @s19 (camino de éxito), así que no es incumplimiento; pero es la misma línea que la feature presenta como su evidencia de auditoría, y la que el juez validó con lupa en la sección 6.

**Por que bloquea:** No bloquea por contrato. Pide un veredicto escrito, que es justo lo que falta (motivo 1).

### M21

**Que:** La evidencia del frontend no es reproducible tal como está commiteada. `progress/verificacion_mutantes_automations.json`, que la bitácora declara como «el veredicto de cada uno», contiene **11** de los 198 —el script lo sobrescribe entero en cada pasada, y el resto sólo sobrevive en el historial de git de ese fichero—; y uno de esos 11 está grabado como SOBREVIVE. El propio `scripts/verificar-mutantes-automations.mjs` conserva hoy 178 anclas: las 23 del racimo 3 ya no están en ninguna parte. Detalle operativo: el script escribe sobre `frontend/src/automations.tsx` y `automations-api.ts` reales y, si se interrumpe, los deja mutados.

**Por que bloquea:** El método de acreditación es bueno; el rastro que deja no permite comprobarlo sin reconstruirlo desde git. Y la medida real (91,18 %) ya cubre el umbral, así que esto es higiene, no puerta.

### M22

**Que:** El arreglo de `actionOf` es correcto y mínimo, pero dejó sin oráculo la rama que hizo inofensiva: los ternarios de `editingOf` que vacían los campos de tarea para una regla de webhook acumulan **diez supervivientes** medidos (L110:16, L110:77, L112:72, L114:7, L116:11, L118:7 ×2 + LogicalOperator, L119:7, L121:11). Al devolver intacta la acción, esos valores dejaron de viajar en el cuerpo del PUT y sólo se ven en la pantalla; y ninguna prueba afirma qué muestra el editor al abrir una regla de webhook. El defecto anotado nº 3 no tiene oráculo alguno.

**Por que bloquea:** No bloquea —el fichero mide 84,60 % y el ámbito 91,18 %—, pero es el patrón que la lente busca: el arreglo movió el síntoma fuera del alcance de las pruebas en vez de cerrar la rama.
