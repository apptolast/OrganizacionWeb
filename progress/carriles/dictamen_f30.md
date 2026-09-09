## Feature 30-automations

11 hallazgos confirmados, de los que **4 son bloqueantes**. 1 se cierran en minutos.

### 1. [BLOQUEANTE · varias horas] Nueve escenarios completos del contrato no tienen ningún oráculo: no existe el ejecutor de reglas

- **Dimension:** contrato
- **Rutas:** features/automations.feature:213-343 (@s15–@s17, @s19, @s20, @s22, @s23, @s25, @s26); backend/src/main/resources/db/migration/V28__automations.sql:35; progress/tdd_automations.md:472-474

**Evidencia.** Listé backend/src/main/java/com/apptolast/organization/**: hay CreateAutomation, ReadAutomations, ReplaceAutomation, DeleteAutomation, SimulateAutomation, ReadAutomationRuns y AutomationMatcher, y NINGUNA clase que ejecute una regla ni avance un cursor. `grep -rn automation_cursors` sobre backend/src devuelve exactamente una línea: la CREATE TABLE de V28__automations.sql:35. `grep -rniE 'AutomationWorker|executeAutomation|automation_cursors' backend/src/test` no devuelve nada. La propia bitácora lo admite en progress/tdd_automations.md:472-474: «Diferidos a la fase 2 …, sin cobertura y sin declarar verdes: @s15, @s16, @s17, @s19, @s20, @s22, @s23, @s25, @s26».

**Arreglo.** Ejecutar la fase 2: construir el caso de uso de ejecución y su adaptador de cursor sobre automation_cursors, y escribir por TDD los oráculos de @s15, @s16, @s17, @s19, @s20, @s22, @s23, @s25 y @s26. Si el propietario decide en su lugar partir la feature, hay que enmendar features/automations.feature y feature_list.json para que 30 sólo declare los escenarios entregados, y abrir una feature nueva con los nueve restantes; cerrar 30 con el contrato actual intacto no es una opción.

**Correccion del verificador.** El bloqueante se sostiene tal cual; solo conviene precisar el alcance, que es mayor que el declarado, y afinar dos matices:

TÍTULO CORREGIDO: «El ejecutor de reglas no existe: nueve escenarios completos sin ningún oráculo, más la mitad de ejecución de otros tres y las filas NOTIFY_WEBHOOK reales de cinco».

Alcance real (comprobado leyendo, no citado de bitácora):
- Sin oráculo alguno, escenarios enteros: @s15 (213), @s16 (222), @s17 (231), @s19 (256), @s20 (268), @s22 (294), @s23 (305), @s25 (326), @s26 (335). Son 9 de 43 = 20,9 %, la cifra del 21 % es correcta.
- Adicionalmente, la matriz de progress/tdd_automations.md:452-454 declara @s18, @s21 y @s24 con una etiqueta que confiesa el recorte: «@s18 (render)», «@s21 (anticipación)», «@s24 (regla desactivada)». Lo cubierto es la plantilla, la simulación y el matcher; el Then de esos tres escenarios («la tarea creada tiene título…», «la ejecución de R1 tiene attempt 1, status failed, errorCode…», «el cursor queda en E») sigue sin oráculo porque nada ejecuta. @s24 se apoya en una sola prueba (AutomationMatcherTest.s24_aDisabledRuleNeverMatches) para un Scenario Outline de cuatro filas, tres de ellas sobre concurrencia y avance de cursor.
- Y las filas NOTIFY_WEBHOOK reales de @s5, @s12, @s21, @s32 y @s33, que la propia bitácora (472-474) también difiere.

Evidencia estructural que refuerza el bloqueante y que conviene añadir al dictamen:
- backend/src/main/java/com/apptolast/organization/application/AutomationRunStore.java es un @FunctionalInterface con un único método de lectura, `List<AutomationRun> page(String owner, UUID ruleId, AutomationRunCursor after, int limit)`. No existe puerto de escritura de ejecuciones: automation_runs solo puede poblarse desde las pruebas, nunca desde producción.
- El único @Scheduled de backend/src/main/java está en adapter/config/WebhookSchedule.java:27. No hay planificador ni bean de worker de automatizaciones.
- AutomationRunCursor es el cursor opaco de paginación del historial (record ruleId/executedAt/id), no el cursor de eventos: no confundirlo con automation_cursors.

Matiz sobre la defensa esperable del artesano («fase 2 depende de 25»): el encabezado del propio contrato (features/automations.feature:6-8) no autoriza omitir, autoriza un doble del puerto — «hasta que 25 exista, sus escenarios usan un doble del puerto y ninguno se declara verde por vacuidad». Además la feature 25 ya está integrada en main, de modo que la condición suspensiva se ha cumplido. La descripción de feature_list.json (línea 339) menciona la fase 2, pero la aceptación vinculante (línea 341) sigue siendo @s1–@s43.

Gravedad: bloqueante, confirmada. No procede done sin que exista el ejecutor con sus oráculos, o sin una enmienda formal del .feature y de feature_list.json aprobada por la puerta humana que reduzca explícitamente el contrato.

### 2. [BLOQUEANTE · una hora] La prueba del interruptor no comprueba ni el ETag vigente ni que el cambio ocurra sólo tras la respuesta

- **Dimension:** oraculos
- **Rutas:** frontend/src/automations.test.tsx:341-354 (y el doble de fetch en :74-76); producción en frontend/src/automations.tsx:310-335

**Evidencia.** Título: `it("@s40 flips the switch only after the confirmed answer and sends the live ETag", ...)`. Cuerpo completo tras el render: `const toggle = await screen.findByRole("switch", ...); expect(toggle).toBeChecked(); await userEvent.click(toggle); await waitFor(() => expect(toggle).not.toBeChecked()); expect(screen.getByText("Inactiva")).toBeInTheDocument();`. El doble de fetch sólo registra `calls.push({ url, method })` (línea 75): las cabeceras de la petición no se guardan en ninguna parte, y la ruta PUT se declara sin `delay` (línea 343), así que no hay ningún instante entre el clic y la respuesta en el que se afirme nada.

**Arreglo.** Registrar las cabeceras en el doble (`calls.push({ url, method, headers: options.headers })`) y afirmar `If-Match: "2"` sobre la petición PUT; declarar la ruta PUT con una promesa retenida, afirmar `expect(toggle).toBeChecked()` y `screen.getByText("Activa")` ANTES de liberarla, y sólo después esperar el cambio a «Inactiva».

**Correccion del verificador.** TÍTULO: La prueba del interruptor no ata el If-Match a la versión viva de la regla ni fija el instante del cambio; dos mutantes del producto sobreviven

RUTAS: frontend/src/automations.test.tsx:341-354 (prueba del 200) y :356-368 (prueba hermana del 503); doble de fetch en :50 y :74-75; producción en frontend/src/automations.tsx:310-326 (`rule.version` en :320). Contrato: features/automations.feature:534.

GRAVEDAD: bloqueante, pero acotado a un hueco de oráculo, no a una ausencia total de cobertura del If-Match.

QUÉ ESTÁ CUBIERTO (que el bloqueante original negaba): frontend/src/automations-api.test.ts:149-162 sí captura las cabeceras del mismo PUT y afirma `If-Match === '"2"'` cuando se le pasa la versión 2 a `replaceAutomation`. La capa de API está probada. El bloqueante NO debe redactarse como «nadie comprueba el If-Match».

QUÉ NO ESTÁ CUBIERTO EN NINGÚN FICHERO (comprobado en automations.test.tsx, automations-api.test.ts, automations-route.test.tsx y e2e/automations.spec.mjs):
1. Que el componente pase la versión VIVA. Mutante superviviente: cambiar `rule.version` por el literal `1` en automations.tsx:320. Todo sigue verde: la prueba del componente no guarda cabeceras (el array `calls` de :50 sólo tiene url y method) y la prueba de API llama a `replaceAutomation` directamente, sin pasar por el componente. Tampoco hay nada que fije que tras un primer toggle exitoso la siguiente escritura use la versión 3 devuelta por el servidor, aunque :325-326 la guarde.
2. Que el cambio ocurra SÓLO tras la respuesta. Mutante superviviente: interruptor optimista (marcar en el clic, revertir en el error). La ruta PUT de :343 se declara sin `delay`, así que no existe ningún instante entre el clic y la respuesta en el que se afirme nada; y la prueba del 503 (:356-368) sólo observa el estado final, luego el revert la deja verde. E2E no lo tapa: e2e/automations.spec.mjs sólo comprueba que el switch es visible tras guardar (:50) y lo mide dentro del barrido 44x44 de @s42 (:77); nunca lo acciona.

MATIZ DE JUSTICIA (no es un placebo): la prueba sí mata un mutante real — si `setRules` conservara `item` en lugar de `saved` (:325-326), el interruptor seguiría marcado y fallaría. El defecto es que el título promete dos conductas más que el cuerpo no ejercita.

CIERRE MÍNIMO EXIGIDO (tres cambios, todos en pruebas):
a) Ampliar el doble de fetch: en :50 declarar `calls: {url: string; method: string; headers: Headers}[]` y en :75 `calls.push({ url, method, headers: new Headers(options.headers) })`.
b) En la prueba del 200, tras el waitFor, afirmar que el PUT salió con `If-Match === '"2"'` (la versión que sirve `listed({...rule, version: 2})`), y accionar el interruptor una segunda vez con una segunda respuesta 200 para afirmar que el segundo PUT lleva `'"3"'`. Esto mata el mutante del literal.
c) Dar a la ruta PUT un `delay` retenido —el helper `route()` de :56-66 ya lo admite y la prueba de «Guardar» de :299-314 ya usa ese patrón— y, antes de liberarlo, afirmar `expect(toggle).toBeChecked()` y `screen.getByText("Activa")`; sólo después de `release()` afirmar el paso a «Inactiva». Hacer lo mismo en la prueba del 503 para que el optimismo con revert también muera.
Con (b) y (c) el título vuelve a decir la verdad y las dos conductas de la fila 3 de @s40 quedan con oráculo discriminante.

### 3. [BLOQUEANTE · varias horas] La auditoría de accesibilidad sólo alcanza 2 de los 7 estados de pantalla

- **Dimension:** accesibilidad
- **Rutas:** e2e/automations-ux.spec.mjs:18-31 (clearRules + openEditor); frontend/src/automations.tsx:384-594 (los siete estados); progress/ux_automations.md:13

**Evidencia.** El componente tiene siete estados excluyentes: «Cargando automatizaciones…» con role=status (automations.tsx:384-386), error con role=alert y «Reintentar» (:387-394), vacío (:395-408), lista con reglas en `<ul aria-label="Reglas">` con switch «Activa/Inactiva», «Editar X» e «Historial de X» (:409-454), editor (:455-561), resultados de simulación con `<ul aria-label="Coincidencias">` (:534-559) e historial con «Cargar más» y el enlace «Ver la tarea creada» (:562-594). Pero e2e/automations-ux.spec.mjs:24-25 hace `test.beforeEach(() => clearRules())` con `DELETE FROM automation_rules WHERE owner_id='e2e-user'`, y openEditor (:27-31) sólo hace goto + clic en «Nueva regla». Con cero reglas la rama que se pinta es siempre la del estado vacío (automations.tsx:395-408). Los cuatro tests de la auditoría (:61, :91, :140, :201) llaman todos a openEditor. No hay ni un `getByRole("switch")`, ni «Simular», ni «Historial», ni «Reintentar» en todo el fichero. En automations.spec.mjs:56-95 el bucle de @s42 por anchos hace exactamente lo mismo: clearRules + «Nueva regla». Grep confirma que sólo dos specs E2E tocan /automatizaciones y ninguno audita los otros cinco estados.

**Arreglo.** Sembrar reglas reales (por API, como hace automations.spec.mjs:49-53 al guardar) y recorrer con axe + geometría los cinco estados que faltan: carga retenida, error 503 con «Reintentar», lista con al menos dos reglas (una activa y una inactiva), resultados de simulación visibles tras pulsar «Simular», e historial abierto con una fila que tenga createdTaskId. Medir en esos estados los mismos 44 × 44 sobre `main a, main button, main input, main select`. Actualizar progress/ux_automations.md para que la sección de estados enumere estados de pantalla y no modalidades.

**Correccion del verificador.** TÍTULO: El @s42 de automatizaciones se audita con un Given que nunca se cumple: ni la lista ni los resultados de simulación llegan a estar en pantalla

RUTAS: features/automations.feature:546-559 (Given del @s42); e2e/automations-ux.spec.mjs:18-31 y 61/91/140/201; e2e/automations.spec.mjs:9-15 y 56-95; frontend/src/automations.tsx:395-408 y 409-454 y 534-559 y 562-594; progress/ux_automations.md:20

EVIDENCIA (comprobada leyendo, no citada de bitácora):
El contrato exige en features/automations.feature:548 «Given /automatizaciones con lista, editor abierto y resultados de simulación visibles», y ese Given rige las cinco filas de Examples (320/768/1280/1440 al 100 % y 1440 al texto 200 %). Los dos únicos specs que tocan la ruta borran todas las reglas antes de cada test — e2e/automations-ux.spec.mjs:18-25 y e2e/automations.spec.mjs:9-15, ambos `DELETE FROM automation_runs/automation_rules/automation_cursors WHERE owner_id='e2e-user'` — y entran siempre por goto + «Nueva regla». Con rules.length===0 el bloque de lista de automations.tsx:409-454 no puede renderizarse, y como «Simular» no se pulsa en ninguno de los cuatro tests de la auditoría, `simulation` sigue nulo y el bloque 534-559 tampoco existe. Lo que sí se audita son dos estados a la vez, porque los condicionales de automations.tsx no son excluyentes: el estado vacío (395-408) y el editor (455-561) se pintan juntos. Total: 2 de 7 estados, y ninguna de las cinco filas del Examples se ejecuta en las condiciones que su propio Given fija.

Consecuencias medibles que quedan sin oráculo: axe nunca ve `<ul aria-label="Reglas">`, `<ul aria-label="Coincidencias">` ni la `<section aria-label="Historial">`; el contraste del borde del `role="switch"` (styles.scss:536-546, `--success` frente a `--muted` sobre `--editable`) no se evalúa en tema claro, oscuro ni forced-colors; y el reflujo a 320 px de un `li` con cinco hijos en `flex-wrap` (styles.scss:512-523) —el riesgo real de recorte de esta pantalla— nunca se ejerce, porque no hay ni un `li`. Los estados de carga y de error con «Reintentar» sólo tienen prueba funcional en jsdom (frontend/src/automations.test.tsx:96 y :138), sin axe, sin anchos y sin temas: grep de «axe» en frontend/src/*.test.tsx da cero.

Añadido de bitácora inexacta: progress/ux_automations.md:20 afirma que la medida se tomó con «lista y editor abiertos». Es falso: el clearBefore garantiza cero reglas. La fila 1 de esa tabla declara una condición que el código de prueba impide. El título de la sección (:13, «los siete que exige la revisión» sobre una lista de modalidades) es confuso, pero es un problema menor comparado con esa fila.

NO SOSTENGO dos de los sub-argumentos del bloqueante original: styles.scss:471-484 da `min-width/min-height: 44px` con `display:inline-flex` a todo `button` y `a` de `.automations`, de modo que el enlace «Ver la tarea creada» no puede reproducir la regresión de 21 px de github_connector aunque no se mida; y el switch imprime «Activa»/«Inactiva» como texto (automations.tsx:430), así que no depende del color solo.

ADYACENTE (no es el bloqueante, pero conviene arreglarlo en la misma pasada): assertUsable (e2e/automations-ux.spec.mjs:53-59) sólo compara `documentElement.scrollWidth` con `clientWidth`. El Then del contrato dice «ni contenido cortado» (features/automations.feature:550) y no hay ningún oráculo de recorte —vertical ni por `overflow` de contenedor— para esa mitad de la aserción.

MÍNIMO PARA CERRAR: sembrar por API (o por la interfaz, como hizo github_connector con su servicio falso) al menos dos reglas, abrir el editor y pulsar «Simular» ANTES de medir, de forma que las cinco filas del Examples corran con lista + editor + resultados visibles; añadir a la auditoría los estados de historial (con «Cargar más» y el enlace a la tarea), de error recuperable con «Reintentar» y de carga; y corregir progress/ux_automations.md:20 para que describa la pantalla realmente medida.

GRAVEDAD: bloqueante (Given del contrato incumplido, no preferencia de estilo).

### 4. [BLOQUEANTE · varias horas] El Given de @s42 («lista, editor y resultados de simulación visibles») no se cumple en ningún test

- **Dimension:** accesibilidad
- **Rutas:** features/automations.feature:547-552; e2e/automations-ux.spec.mjs:61-89, 91-138, 140-199, 201-305; e2e/automations.spec.mjs:56-95

**Evidencia.** features/automations.feature:548 dice «Given /automatizaciones con lista, editor abierto y resultados de simulación visibles». De las tres condiciones, la auditoría cumple una: el editor. La lista está vacía por clearRules (e2e/automations-ux.spec.mjs:20) y los resultados de simulación no se muestran nunca: el bloque `{simulation && ...}` de automations.tsx:534 sólo se pinta tras `simulate()`, y no hay ninguna llamada a «Simular» en e2e/automations-ux.spec.mjs (el único clic en «Simular» del repositorio está en automations.spec.mjs:41, en un test funcional que no fija viewport, no ejecuta axe y no mide ninguna caja).

**Arreglo.** Añadir a e2e/automations-ux.spec.mjs un paso previo que cree al menos dos reglas, abra el editor y ejecute «Simular» hasta ver `role="status"` con nombre «Resultado de la simulación», y ejecutar desde ahí las cinco filas de anchos/zoom del Examples de @s42 (320, 768, 1280, 1440 y 1440 con texto 200 %), incluyendo una coincidencia con wouldFail para que el texto largo esté presente.

**Correccion del verificador.** BLOQUEANTE (reformulado): la auditoría @s42 mide la pantalla en su estado más vacío y progress/ux_automations.md declara medido un estado que nunca se renderizó.

Hechos verificados por lectura:
1. features/automations.feature:548 fija el estado de partida de @s42 en tres condiciones: lista, editor abierto y resultados de simulación visibles.
2. frontend/src/automations.tsx:409 sólo pinta el <ul aria-label="Reglas"> (411) si rules.length > 0, y :534 sólo pinta el role="status" de la simulación tras simulate().
3. e2e/automations-ux.spec.mjs:18-25 y e2e/automations.spec.mjs:9-16 ejecutan el mismo clearRules() (DELETE de automation_runs, automation_rules y automation_cursors) en beforeEach y afterEach. create(request, "Marketing") crea un PROYECTO, no una regla (e2e/support/projects.mjs). Los nueve tests @s42 (automations-ux.spec.mjs:61, 91, 140, 201; automations.spec.mjs:57 x4 anchos y :97) miden por tanto el estado vacío más el editor.
4. Grep de «Simular» en e2e/: única ocurrencia en automations.spec.mjs:41, dentro del test @s39 @s40 (32-54), que no fija viewport, no ejecuta axe y no toma ninguna boundingBox. Ningún test que mida geometría o pase axe ha visto jamás una fila de regla ni el bloque de coincidencias.

Lo que NO sostengo (corrección al bloqueante original): no hay indicio de que el estado denso desborde. .automations declara overflow-wrap: anywhere (styles.scss:458-460); button, a llevan min-width y min-height de 44px con max-width: 100% (471-485); li lleva flex-wrap: wrap y min-width: 0 (513-523); li > span lleva min-width: 0 y overflow-wrap: anywhere (524-527). Los fallos concretos que el bloqueante predecía están prevenidos por construcción. El defecto no es un bug latente, es evidencia inexistente sobre un estado que el contrato nombra por escrito.

Lo que agrava y hace que esto bloquee de verdad, no visto por el bloqueante original: progress/ux_automations.md afirma cosas que el código desmiente.
- :20 dice «e2e/automations-ux.spec.mjs, lista y editor abiertos». Falso: la lista está vacía por clearRules en las dos suites.
- :81 dice que el <ul aria-label="Reglas"> está «Verificado estructuralmente y con axe». Falso: axe sólo corre en automations-ux.spec.mjs (líneas 80-82, 129-131, 178-185, 299-301) y en automations.spec.mjs:87-94, siempre sobre la página sin reglas; no hay axe en los unitarios (grep sin resultados en automations.test.tsx y automations-route.test.tsx).
- :65 (Von Restorff) dice que el «Activa»/«Inactiva» de cada regla está «Verificado en claro, oscuro y forced-colors». Falso por lo mismo: ese role="switch" (automations.tsx:421-431) nunca se renderizó en ninguna corrida medida.
Esto cae exactamente en la prohibición de AGENTS.md:51 de declarar cumplimiento sin medirlo, y repite el patrón de bitácora inexacta ya visto hoy en esta feature.

Remedio mínimo para levantar el bloqueante: en e2e/automations-ux.spec.mjs, sembrar al menos dos reglas por API (una activa y una inactiva, una con nombre largo y acción CREATE_TASK y otra NOTIFY_WEBHOOK) y pulsar «Simular» antes de geometry()/assertUsable()/axe, de modo que el <ul aria-label="Reglas"> y el bloque {simulation} estén en pantalla en los cuatro anchos, los dos temas, texto al 200 %, zoom nativo, forced-colors y reduced-motion. Y corregir las filas 20, 65 y 81 de progress/ux_automations.md para que digan lo que realmente se midió.

### 5. [ALTA · una hora] @s12: el «And <efecto>» de las cuatro filas (las 2 ejecuciones registradas siguen consultables) no tiene oráculo en ningún test

- **Dimension:** contrato
- **Rutas:** features/automations.feature:170-181; backend/src/test/java/com/apptolast/organization/application/AutomationRulesTest.java:53-80; backend/src/test/java/com/apptolast/organization/adapter/persistence/AutomationPersistenceTest.java:174-190

**Evidencia.** El Given de @s12 es «una regla propia versión 1 enabled true con 2 ejecuciones registradas» y las cuatro filas comprueban que tras el PUT «las 2 ejecuciones siguen consultables» / «se conservan». Los tres tests que la bitácora cita en :449 no crean ninguna ejecución: AutomationRulesTest.java:53-65 y :67-80 trabajan sobre InMemoryAutomations sin runs, y AutomationPersistenceTest.java:174-190 hace store.replace sin insertar ninguna fila en automation_runs (el helper run(...) de :225 sólo se usa en s34 y s14). No hay ningún test en el repositorio que reemplace una regla que tenga historial y compruebe después su historial.

**Arreglo.** Añadir a AutomationPersistenceTest un caso que cree una regla, le inserte 2 filas en automation_runs, ejecute store.replace y compruebe que runs.page devuelve las 2 sin cambios, repitiéndolo para el cambio de trigger y para el cambio de acción a NOTIFY_WEBHOOK una vez el bean real de 25 esté enchufado.

**Correccion del verificador.** @s12: la conservación del historial tras el PUT («las 2 ejecuciones siguen consultables» / «se conservan») no tiene oráculo en ningún test; el único test que lee el historial tras un replace real lo hace sobre una regla vacía.

Rutas: features/automations.feature:170-181; backend/src/test/java/com/apptolast/organization/adapter/config/AutomationWiringTest.java:66-77 (en concreto :73-74); backend/src/test/java/com/apptolast/organization/adapter/persistence/AutomationPersistenceTest.java:174-190; backend/src/test/java/com/apptolast/organization/application/AutomationRulesTest.java:53-80; backend/src/test/java/com/apptolast/organization/adapter/AutomationsApiTest.java:459, :849.

Evidencia comprobada leyendo:
- El Given de @s12 es «una regla propia versión 1 enabled true con 2 ejecuciones registradas». Tres de las cuatro filas del Examples (enabled false, trigger TaskStatusChanged.v1, acción NOTIFY_WEBHOOK) añaden que las 2 ejecuciones siguen consultables o se conservan.
- Los cinco tests que progress/tdd_automations.md:449 asigna a @s12 no crean ninguna ejecución: AutomationRulesTest.s12_replacingAlwaysBumpsTheVersionAndKeepsCreatedAt (:53) y s12_s3_replacingValidatesReferencesLikeCreating (:67) usan InMemoryAutomations sin runs; AutomationPersistenceTest.s12_… (:174) hace store.replace sobre una regla recién creada sin filas en automation_runs; los dos de AutomationsApiTest (:459, :849) son MockMvc contra ReplaceAutomationUseCase mockeado y no pueden observar persistencia.
- AutomationWiringTest.s1_s11_s12_s14 (:66) sí ejercita replace contra Postgres real y luego lee runs.read(...).items(), pero asserta isEmpty() sobre una regla que nunca tuvo ejecuciones (:73-74). Es un anti-oráculo: pasa idénticamente si el replace conserva, borra u orfana el historial.
- Barrido completo del repositorio: los únicos puntos que crean ejecuciones son el helper run(...) de AutomationPersistenceTest:225 (usado sólo en :267, :268 y :292) e InMemoryAutomationRuns (usado sólo por ReadAutomationRunsTest). Ninguno se combina con un replace. Los tests de frontend (automations.test.tsx:387-452, automations-api.test.ts:207-234) sirven /runs con rutas stub y no observan el backend.

Por qué bloquea: es una cláusula Then repetida en tres filas de un Scenario Outline sin ninguna aserción que la verifique, el mismo motivo por el que el juez ha rechazado features hoy. El modo de fallo es concreto y hoy indetectable: V28__automations.sql:18 declara automation_runs.rule_id ON DELETE SET NULL, y AutomationPersistenceTest.s14 (:287-295) ya demuestra que con rule_id a NULL las ejecuciones desaparecen de runs.page. Si el replace pasara a borrar y reinsertar la regla (o si una migración futura cambiara la cascada), el historial quedaría huérfano y la suite entera seguiría verde.

Gravedad: media-alta. El producto hoy es correcto — PostgresAutomationStore.java:119 implementa replace como un UPDATE automation_rules SET ... que no puede tocar automation_runs — así que no hay bug latente; lo que falta es la red de seguridad que impide que se convierta en uno.

Arreglo mínimo (tres líneas, sin suite nueva): en AutomationWiringTest.s1_s11_s12_s14, insertar 2 ejecuciones para la regla antes del replace y sustituir el assert isEmpty() de :74 por una comprobación de que las mismas 2 ejecuciones (por id) siguen devolviéndose después. Alternativa equivalente en AutomationPersistenceTest.s12_… reutilizando el helper run(...) de :225 y comprobando runs.page(owner, rule.id(), null, 20) antes y después del store.replace.

Fuera de alcance de este bloqueante: la fila 4 (PUT a acción NOTIFY_WEBHOOK hacia endpoint propio) es hoy inalcanzable porque ApplicationConfiguration.java:683-685 sigue devolviendo el stub (owner, endpointId) -> false pese a que la feature 25 ya está en main. Es cierto y verificado, pero es cableado entre features 25 y 30 y corresponde al barrido de colisiones, no al oráculo de @s12; debe levantarse como asunto aparte.

### 6. [ALTA · una hora] @s43 fila 2 (guardado en vuelo / cierra sesión) no tiene ningún test

- **Dimension:** contrato
- **Rutas:** features/automations.feature:561-571; frontend/src/automations.test.tsx:418-440 y :442-468; frontend/src/automations-api.test.ts:263

**Evidencia.** El outline tiene tres filas: (simulación, navega a /proyectos), (guardado, cierra sesión) y (historial, cambia a otra regla). Sólo hay dos tests: automations.test.tsx:418 «@s43 ignores a late answer after the component is gone…» usa una simulación y view.unmount() (proxy razonable de la fila 1), y :442 «@s43 drops the history of the rule the owner just left» cubre la fila 3. El tercer test citado en el mapa, automations-api.test.ts:263 «@s43 stops before touching the network when the signal is already aborted», es sobre un AbortSignal ya abortado, no sobre un cierre de sesión. Ningún test somete un POST/PUT de guardado en vuelo a un cierre de sesión.

**Arreglo.** Añadir a frontend/src/automations.test.tsx un caso que deje un guardado retenido, dispare el cierre de sesión, libere la respuesta tardía y compruebe que no se pinta ni se anuncia nada, que no queda estado de reglas en memoria bajo la nueva identidad y que localStorage y sessionStorage siguen vacíos.

**Correccion del verificador.** Titulo: el aislamiento por identidad de @s43 (key={owner}) no tiene ningun oraculo: borrarlo no rompe ningun test

Rutas: features/automations.feature:561-571 (fila «guardado / cierra sesion» y el Then «...ni en memoria de otra identidad ni en localStorage»); frontend/src/automations.tsx:147-149; frontend/src/automations.test.tsx:418-440.

Que se comprobo leyendo:
1. automations.tsx:147-149 es `export function Automations({ owner }: { owner: string }) { return <AutomationsWorkspace key={owner} />; }`. El prop `owner` no se usa para nada mas (grep de "owner" en automations.tsx da solo esa linea y un comentario en :337): existe unicamente para forzar el remontaje cuando cambia la identidad, que es literalmente la clausula «no queda ningun dato de reglas ni simulaciones en memoria de otra identidad».
2. Ningun test cambia nunca de identidad. Los 17 `it(` de automations.test.tsx renderizan `<Automations owner="owner" />` y ninguno hace `rerender` con otro owner; automations-route.test.tsx renderiza `<App username="owner" />` fijo; no hay @s43 en e2e. Consecuencia concreta: si se borra `key={owner}` (o el wrapper entero y se exporta AutomationsWorkspace como Automations), la suite sigue verde al 100 %. Es un mutante vivo por construccion, y Stryker no mutila atributos JSX, asi que la campana tampoco lo va a delatar.
3. Es la unica feature del proyecto sin esa prueba: el mismo patron `view.rerender(<X owner="otro" />)` existe en github-connector.test.tsx:562, integration-api.test.tsx:173/740/1359, webhooks.test.tsx:304, import-data.test.tsx:216 y calendar.test.tsx:703. La convencion esta establecida y automations es la excepcion.
4. La otra mitad del Then tampoco discrimina: `grep -c "localStorage\|sessionStorage"` devuelve 0 tanto en automations.tsx como en automations-api.ts, luego `expect(localStorage.length).toBe(0)` y `expect(sessionStorage.length).toBe(0)` (automations.test.tsx:437-438) son ciertos hagas lo que hagas; y `expect(screen.queryByRole("status")).not.toBeInTheDocument()` se evalua DESPUES de `view.unmount()`, cuando el contenedor ya no esta en el documento, asi que tambien es cierto pase lo que pase. El test de :418 solo discrimina, en el mejor de los casos, que la respuesta tardia no lance.

Lo que NO es el hueco (correccion al bloqueante original): no hace falta un test que someta un POST/PUT en vuelo a un cierre de sesion. save(), simulate() y toggle() comparten el mismo ref `writeRequest` y la misma guarda `if (!mounted.current || writeRequest.current !== controller) return;`, y el cleanup de useLayoutEffect (automations.tsx:178-185) aborta live, runsRequest y writeRequest; cerrar sesion desmonta el arbol entero (session-gate.tsx devuelve la pantalla de acceso cuando session.authenticated deja de ser true), que es el mismo evento que `view.unmount()`. Esa mitad ya esta cubierta por la misma via de codigo.

Que se pide para levantarlo (barato, una prueba):
- En automations.test.tsx: montar con `owner="ana"`, dejar cargada la lista de reglas y una simulacion visible, luego `view.rerender(<Automations owner="bruno" />)` con la ruta de /api/v1/me/automations devolviendo una lista distinta (o vacia), y afirmar que ninguna regla ni resultado de simulacion de «ana» sigue en pantalla y que se ha vuelto a pedir la lista. Esa prueba muere si se borra `key={owner}`.
- Opcionalmente, reforzar el oraculo de :418 para que discrimine algo: capturar `console.error`/el aviso de React de setState tras desmontaje, o comprobar que el fetch se aborto, en vez de aserciones que son ciertas por construccion.

Gravedad corregida: media-alta, no alta. El producto SI trae la guarda (`key={owner}`), asi que no hay fuga demostrada entre identidades; lo que falta es el oraculo que la protege. Bloquea igual segun el liston de hoy (mecanismo de seguridad sin prueba que lo sostenga, mas dos aserciones placebo en el unico test que lo roza), pero no debe redactarse como si hubiera una fuga en produccion.

### 7. [ALTA · una hora] El oráculo de @s42 mide desplazamiento de página pero nunca recorte de contenido

- **Dimension:** oraculos
- **Rutas:** e2e/automations.spec.mjs:67-73 y :122-128; e2e/automations-ux.spec.mjs:33-59 (campo `scroll` y `assertUsable`)

**Evidencia.** Único oráculo geométrico en los dos ficheros: `document.documentElement.scrollWidth <= document.documentElement.clientWidth` y, en la auditoría UX, `expect(observed.scroll).toBeLessThanOrEqual(observed.client);`. El contrato @s42 dice «Then no hay desplazamiento horizontal ni contenido cortado» (features/automations.feature:550).

**Arreglo.** Reutilizar el oráculo ya escrito y validado en e2e/ics-calendar-ux.spec.mjs:139-189: añadir a `geometry()` de automations-ux.spec.mjs las listas `clipped` (ambos ejes, filtrando `overflow` visible) y `escaping` (controles cuyo `right` supera `clientWidth`), y afirmarlas contra `[]` en los siete estados; replicar el `clipped` en el caso de texto al 200 % de automations.spec.mjs.

**Correccion del verificador.** BLOQUEANTE (gravedad alta, confirmado y ampliado)

Titulo: El unico oraculo geometrico de @s42 mide desbordamiento de pagina y nunca recorte de contenido, en los cuatro tests UX y en los dos de la suite funcional

Rutas verificadas:
- e2e/automations.spec.mjs:68-73 (matriz de 4 anchos) y :122-128 (texto al 200 % a 1440 px)
- e2e/automations-ux.spec.mjs:33-51 (`geometry`) y :53-59 (`assertUsable`)
- Consumidores de `assertUsable`: :74 (4 anchos x 2 temas), :123 (texto 200 % x 4 anchos x 2 temas), :151 (forced-colors + reduced-motion), :290 (zoom NATIVO de Chromium al 200 % sobre 320 px CSS)
- Contrato: features/automations.feature:550; docs/ux-requirements.md:52
- Precedente en el repositorio: e2e/ics-calendar-ux.spec.mjs:131-133 (comentario) y :171-185 (campo `clipped`), consumido en :208 y :577

Hecho comprobado leyendo el codigo (no citado de bitacora):
1. `grep -n "scrollWidth"` en e2e/automations.spec.mjs devuelve exactamente dos apariciones (70-71, 125-126), ambas `document.documentElement.scrollWidth <= document.documentElement.clientWidth`.
2. `geometry` en automations-ux.spec.mjs:34-50 devuelve `{width, scroll, client, forced, reduced, dark, theme, controls}`. No hay `scrollHeight`, `clientHeight`, `overflowX` ni `overflowY` en el fichero.
3. `grep -rn "clipped\|overflowY" e2e/*.mjs` devuelve unicamente e2e/ics-calendar-ux.spec.mjs. Ningun otro fichero de la feature 30 cubre el recorte. frontend/src/automations.test.tsx y automations-route.test.tsx corren en jsdom y no miden geometria.
4. `grep -n "overflow *:"` sobre todo frontend/src/styles.scss devuelve una sola linea: `text-overflow: ellipsis` en :502. El bloque `.automations` va de :458 a :560 y no declara ningun `overflow`.
5. progress/ux_automations.md:20 declara como evidencia del principio responsive solo «Sin desplazamiento horizontal (scrollWidth <= clientWidth) y todo control >= 44 x 44 px». La propia matriz de principios reconoce, sin decirlo, que el recorte no esta medido.

Por que bloquea:
El contrato de @s42 (features/automations.feature:550) nombra dos obligaciones, «no hay desplazamiento horizontal ni contenido cortado», y docs/ux-requirements.md:52 exige «sin solapes, recortes ni desplazamiento horizontal de pagina». Solo la primera tiene oraculo. Un contenedor con `overflow` distinto de `visible` y altura o anchura acotada recorta el texto sin que `document.documentElement.scrollWidth` crezca un pixel: el fallo cae exactamente en el punto ciego. axe no tiene regla para contenido recortado, y AGENTS.md («Referencia obligatoria de interfaces») prohibe declarar cumplimiento por pasar axe, asi que no hay red de seguridad indirecta. Este repositorio ya pago esta leccion en la feature 26 y la dejo escrita en e2e/ics-calendar-ux.spec.mjs:131-133: «La version anterior solo miraba scrollWidth, y por eso no vio que al arreglar el recorte horizontal el contenido paso a recortarse por abajo».

Alcance real (aqui es donde la formulacion original se queda corta): el hueco no son dos escenarios sino SIETE ESTADOS. `assertUsable` es el oraculo compartido de los cuatro tests UX. Incluye el zoom nativo de Chromium al 200 % sobre 320 px CSS (:289-290), que es precisamente el estado donde el recorte vertical es mas probable y el que el juez ya senalo hoy en otra feature. Es decir: el estado mas caro de montar de toda la auditoria (extension, contexto persistente, chrome.tabs.setZoom) se ejecuta de verdad, pero luego mide con un oraculo ciego a la mitad del criterio.

Mutacion que deberia romper la suite y no la rompe (verificada estaticamente contra el codigo, no ejecutada):
Anadir a frontend/src/styles.scss, dentro del bloque .automations, `li { overflow: hidden; max-height: 96px; }` (o lo equivalente en `section`), y guardar una regla con nombre y plantilla largos. A 320 px el texto se corta visiblemente. Ninguno de los seis tests falla: `documentElement.scrollWidth` no crece porque el contenido queda recortado, no desbordado; los `getBoundingClientRect()` de botones e interruptores siguen devolviendo >= 44 x 44 px aunque el ancestro los recorte; axe no reporta nada; las capturas de :76-79, :125-128 y :291-294 se guardan pero no se asertan.

Correccion exigida:
1. Anadir a `geometry` de e2e/automations-ux.spec.mjs un campo `clipped` equivalente al de e2e/ics-calendar-ux.spec.mjs:171-185: recorrer `main, main *`, y marcar el elemento cuando `getComputedStyle(el).overflowX !== "visible" && el.scrollWidth > el.clientWidth + 1` o `overflowY !== "visible" && el.scrollHeight > el.clientHeight + 1`, devolviendo una descripcion legible (etiqueta, id, primeros caracteres de texto y las dos medidas) para que el fallo sea diagnosticable.
2. Anadir a `assertUsable` `expect(observed.clipped, "<estado> recorta contenido (ancho o alto)").toEqual([])`, con lo que los siete estados quedan cubiertos de golpe, zoom nativo incluido.
3. Replicar la comprobacion en los dos tests de e2e/automations.spec.mjs:68-73 y :122-128, o extraer el helper a e2e/support/ y consumirlo desde ambos ficheros para que no vuelvan a divergir.
4. CAVEAT DE IMPLEMENTACION que el bloqueante original no contempla y que hara fallar la adopcion literal del oraculo de ics: `.automations input, select` lleva `text-overflow: ellipsis` (frontend/src/styles.scss:502) y son editores de una sola linea. Si en Chromium el `overflow` computado de esos controles no es `visible`, un valor largo los marcara como recortados de forma legitima. Al portar el oraculo hay que decidir y DOCUMENTAR en el propio comentario del helper si se exceptuan `input`/`select`/`textarea` (contenido alcanzable con el cursor y el desplazamiento interno) o si se les exige `title`/valor completo accesible; no basta con silenciar el caso.
5. Actualizar progress/ux_automations.md:20 para que la columna de evidencia nombre el recorte medido en los dos ejes, no solo `scrollWidth <= clientWidth`.
6. Antes de cerrar, ejecutar la mutacion de control descrita arriba (`overflow: hidden` + `max-height` en `.automations li`) y comprobar que ahora la suite falla, y registrar esa comprobacion en progress/ux_automations.md. Un oraculo de recorte que nunca se ha visto fallar es otra prueba placebo.

### 8. [ALTA · una hora] El oráculo de recorte sólo mide el eje horizontal y sólo a nivel de documento

- **Dimension:** accesibilidad
- **Rutas:** e2e/automations-ux.spec.mjs:33-59 (geometry/assertUsable); e2e/automations.spec.mjs:67-73 y 122-128; comparar con e2e/ics-calendar-ux.spec.mjs:127-189

**Evidencia.** assertUsable (e2e/automations-ux.spec.mjs:53-59) contiene una sola comprobación de recorte: `expect(observed.scroll).toBeLessThanOrEqual(observed.client)`, donde scroll/client son `document.documentElement.scrollWidth` y `clientWidth` (:36-37). No hay ninguna lectura de scrollHeight, clientHeight, overflowX ni overflowY en todo el fichero. Lo mismo en e2e/automations.spec.mjs:70-72 y :125-127. El repositorio ya tiene el oráculo correcto y documentado en e2e/ics-calendar-ux.spec.mjs:171-185, que recorre `main, main *` y marca recorte cuando `style.overflowX !== "visible" && scrollWidth > clientWidth + 1` o `style.overflowY !== "visible" && scrollHeight > clientHeight + 1`, con el comentario de :130-133 explicando que «la versión anterior sólo miraba scrollWidth, y por eso no vio que al arreglar el recorte horizontal el contenido pasó a recortarse por abajo».

**Arreglo.** Reemplazar la comprobación de assertUsable por el oráculo de dos ejes por elemento de ics-calendar-ux.spec.mjs:171-185, aplicado a `main, main *`, y asertar la lista de recortados vacía en las cuatro pruebas de la auditoría (anchos, texto 200 %, forced-colors y zoom nativo).

**Correccion del verificador.** TÍTULO CORREGIDO: La cláusula «ni contenido cortado» de features/automations.feature:550 no tiene oráculo en ninguno de los siete estados, y una prueba se titula como si lo tuviera

GRAVEDAD CORREGIDA: media (no alta). Lo probado es un hueco de oráculo sobre la mitad de una cláusula del contrato, no un defecto demostrado. Bloquea igualmente porque el juez ya rechazó hoy una feature por este mismo hueco, y el arreglo es barato.

EVIDENCIA (verificada leyendo):
- `e2e/automations-ux.spec.mjs:53-59` — `assertUsable` es la única puerta geométrica de las cuatro pruebas (:74, :123, :151, :290), es decir de los siete estados, y su comprobación de recorte es una sola línea: `expect(observed.scroll).toBeLessThanOrEqual(observed.client)` sobre `documentElement.scrollWidth/clientWidth` (:36-37). Cero apariciones de `scrollHeight`, `clientHeight`, `overflowX` u `overflowY` en el fichero.
- `e2e/automations.spec.mjs:67-73` y `:122-128` — idénticas, a nivel de documento y sólo horizontal.
- Barrido completo de `e2e/`: la única lectura de recorte a nivel de ELEMENTO en todo el directorio es `e2e/ics-calendar-ux.spec.mjs:171-185`, y allí sí se aserta (`:207-210` y `:577`). Ningún otro fichero visita `/automatizaciones`.
- `progress/ux_automations.md:20-26` sólo reclama «sin desplazamiento horizontal» y «sin desbordamiento»; nunca reclama «sin contenido cortado». El hueco está admitido en la propia evidencia.

MECANISMO CORREGIDO (el del bloqueante original es erróneo):
No culpar a `text-overflow: ellipsis`. Chromium ya aplica `overflow: clip !important` a los `<input>` de texto en su hoja de agente de usuario, así que la declaración de `frontend/src/styles.scss:502` (no :504) sólo cambia el corte seco por puntos suspensivos; borrarla no arregla nada. El portador real está en `progress/ux_automations.md:48-51`: el bloque `.automations` se añadió *para* eliminar el desbordamiento horizontal al 200 % de texto, y lo hizo encerrando `input, select` en `width: 100%; max-width: 100%` (`styles.scss:490-495`). Eso convierte desbordamiento de página en recorte dentro de la caja — la misma regresión que documenta `e2e/ics-calendar-ux.spec.mjs:130-133` — y el oráculo mide sólo el eje que se arregló. Señal de refuerzo: `.automations` es el ÚNICO bloque de las ~1000 líneas de `styles.scss` con `text-overflow`, mientras el bloque hermano de ics (`styles.scss:417-434`) lleva el comentario explícito «Sin `overflow` propio no hay caja que pueda cortarla» y sustituyó el `<input>` por un `<div data-calendar-link>` justo para no recortar. Automations se desvía del patrón que el juez ya aprobó.

HALLAZGO ADICIONAL, DE LA MISMA CLASE QUE EL JUEZ YA RECHAZÓ HOY:
`e2e/automations.spec.mjs:97` se titula «automatizaciones: el texto al 200 % **no corta contenido** a 1440 px» y su único aserto (:122-128) es `documentElement.scrollWidth <= clientWidth`. El título nombra el recorte; el oráculo mide desbordamiento. Es el patrón «se cancela con Escape» pulsando Enter. O se renombra a «no desborda en horizontal», o se le pone el oráculo que el título promete.

QUÉ NO SE AFIRMA:
No afirmo que exista un defecto visible. El caso concreto (el `<input>` con el valor por defecto `"Revisar {{task.title}}"` de `frontend/src/automations.tsx:97`, a 320 px con texto al 200 %) recorta visualmente, pero el valor sigue siendo alcanzable con el cursor y está íntegro en el árbol de accesibilidad, así que no es un fallo cierto de WCAG 1.4.4. Lo que falta es la medición, no necesariamente el arreglo.

REMEDIO MÍNIMO (sin ejecutar nada aquí):
1. Portar el array `clipped` de `e2e/ics-calendar-ux.spec.mjs:171-185` a `geometry` de `automations-ux.spec.mjs` y asertar `toEqual([])` dentro de `assertUsable`, de modo que cubra los cuatro anchos × dos temas, el texto al 200 %, forced-colors/reduced-motion y el zoom nativo.
2. Prever que la primera ejecución marque `INPUT` y `SELECT` nativos. Decidir explícitamente y dejarlo escrito en `progress/ux_automations.md`: o se excluyen los controles de formulario nativos con justificación (su valor es alcanzable y está en el árbol de accesibilidad), o se acepta el hallazgo y se arregla como en ics. Excluirlos en silencio volvería a vaciar el oráculo.
3. Renombrar o reforzar `e2e/automations.spec.mjs:97`.
4. Añadir a `progress/ux_automations.md` la fila de la matriz que hoy falta: recorte en ambos ejes, con su límite explícito.

### 9. [ALTA · una hora] No existe recorrido de teclado ni medición de foco visible, y la matriz afirma lo contrario

- **Dimension:** accesibilidad
- **Rutas:** e2e/automations-ux.spec.mjs:153-156; progress/ux_automations.md:63 y :72

**Evidencia.** Lo único relacionado con teclado en toda la feature son tres líneas dentro de la prueba de forced-colors: `const save = page.getByRole("button", { name: "Guardar" }); await save.focus(); await expect(save).toBeFocused();` (e2e/automations-ux.spec.mjs:154-156). Es un `.focus()` programático sobre un solo control. Grep de `Tab` y `keyboard` sobre e2e/automations.spec.mjs y e2e/automations-ux.spec.mjs: cero coincidencias. Grep de `.tab(` sobre frontend/src/automations*.tsx y automations*.ts: cero. En frontend/src/automations.test.tsx la única mención de foco es el @s38 de :185. Tampoco hay ninguna lectura de `outlineWidth`, pese a que es la práctica establecida del repositorio (e2e/appearance-ux-audit.spec.mjs:496 y :516 con `expect(focus.outlineWidth).toBeGreaterThanOrEqual(2)`, y lo mismo en authentication.spec.mjs:125, availability.spec.mjs:263, create-project.spec.mjs:256, create-task.spec.mjs:521). Frente a eso, progress/ux_automations.md:63 declara «en el editor, Guardar antes que Simular en el DOM y en el orden de teclado. | Orden verificado en los 4 anchos» y :72 declara Fitts «Medido … alcanzables por teclado».

**Arreglo.** Añadir en la auditoría un recorrido real con `page.keyboard.press("Tab")` desde el inicio del `main` hasta salir de él, recogiendo en cada parada el rol y el nombre accesible del elemento enfocado; asertar la secuencia esperada (incluido Guardar antes que Simular), que el conjunto enfocado coincide con los controles interactivos visibles, que `outlineWidth >= 2` en cada parada, y repetir con Shift+Tab para la salida en sentido inverso. Hacerlo también en forced-colors. Corregir o retirar las afirmaciones de ux_automations.md:63 y :72 según lo que quede realmente medido.

**Correccion del verificador.** TÍTULO: El conjunto «alcanzables por teclado en orden lógico con foco visible» de @s42 no tiene oráculo, y dos filas de la matriz UX lo dan por verificado.

RUTAS: features/automations.feature:551 (contrato); e2e/automations-ux.spec.mjs:33-59 (geometry/assertUsable, el único oráculo de @s42) y :154-156 (el único foco de la feature); progress/ux_automations.md:25 y :63 (afirmaciones no respaldadas). [Corregido respecto al bloqueante original: :72 NO es una cita válida — la fila Fitts habla solo de 44x44 px y de zoom nativo, y esa parte sí está medida.]

EVIDENCIA VERIFICADA LEYENDO:
- features/automations.feature:551 exige tres propiedades acopladas en @s42: «todos los controles interactivos miden al menos 44 por 44 píxeles CSS y son alcanzables por teclado en orden lógico con foco visible». El oráculo de los cuatro tests @s42 es assertUsable(observed) (e2e/automations-ux.spec.mjs:53-59), que comprueba scroll<=client y width/height>=44 sobre los controles de geometry() (:33-51). Nada mide tabulación ni indicador de foco. axe se ejecuta en los cuatro, pero AGENTS.md:51 prohíbe declarar cumplimiento por axe.
- Único foco de toda la feature: e2e/automations-ux.spec.mjs:154-156, dentro del test de forced-colors, con el comentario «El foco sigue siendo alcanzable y visible con colores del sistema» — pero solo hace save.focus() y toBeFocused() sobre un <button> nativo. Prueba que ese control es enfocable programáticamente; no prueba que Tab lo alcance, ni el orden, ni que se pinte indicador alguno: :focus-visible (styles.scss:621) no se activa igual con foco programático que con teclado, así que un foco invisible pasaría verde. El propio comentario del test afirma «y visible» sin medir nada.
- Cobertura ausente en todo el repositorio para esta ruta: grep de Tab/keyboard/press(/outlineWidth sobre e2e/automations-ux.spec.mjs y e2e/automations.spec.mjs da cero; esos dos son los únicos ficheros de e2e/ que mencionan «automatizaciones»; e2e/support/*.mjs no tiene helpers de foco; frontend/src/automations.test.tsx solo tiene el @s38 de :185 (foco de error de campo) y automations-route.test.tsx nada.
- Afirmaciones sin respaldo en la matriz: progress/ux_automations.md:63 (Posición en serie) declara «en el editor, Guardar antes que Simular en el DOM y en el orden de teclado. | Orden verificado en los 4 anchos»; el orden del DOM es cierto por lectura (automations.tsx:528 Guardar, :531 Simular) pero el orden de teclado y el «verificado en los 4 anchos» no tienen prueba. Y :25 declara para el estado 6 «foco alcanzable y visible», que es justamente lo que save.focus() no demuestra.
- Práctica establecida que aquí falta: outlineWidth se mide en al menos diez specs del repo (appearance-ux-audit.spec.mjs:496 y :516 con toBeGreaterThanOrEqual(2), authentication.spec.mjs:125, availability.spec.mjs:263 y :282, create-project.spec.mjs:256 y :275, create-task.spec.mjs:521, complete-reopen-task.spec.mjs:455, edit-project.spec.mjs:420, export-data-browser.spec.mjs:127).

GRAVEDAD: alta, sin cambios. Es un conjunto del contrato sin oráculo más dos filas de matriz que lo declaran verificado, el patrón exacto de dos de los tres rechazos de hoy.

NO SE LE IMPUTA: el zoom nativo al 200 % sí se ejecuta de verdad (e2e/automations-ux.spec.mjs:201-303, extensión MV3 + chrome.tabs.setZoom, expect(zoom).toBe(2), poll de devicePixelRatio y de innerWidth==320), y la fila Fitts de :72 está correctamente sustentada.

CIERRE MÍNIMO: (a) un test que recorra con page.keyboard.press("Tab") desde el inicio del <main> del editor, recoja el nombre accesible de cada elemento enfocado y afirme la secuencia esperada, incluido Guardar antes que Simular, y que vuelva con Shift+Tab comprobando que se sale por ambos extremos sin trampa de foco; (b) en cada parada, leer getComputedStyle del elemento activo y exigir outlineWidth>=2 (o box-shadow equivalente) como en el resto del repo, ejecutándolo también en forced-colors; (c) reescribir progress/ux_automations.md:25 y :63 para que la columna «Resultado / límite» cite el test nuevo o rebaje la afirmación a «orden del DOM verificado por lectura; orden de teclado no medido».

### 10. [ALTA · minutos] ApiErrors.java queda fuera del ambito PIT de automations: los seis manejadores de error de la feature no reciben ningun mutante

- **Dimension:** mutacion
- **Rutas:** backend/build.gradle.kts:492-511 (automationsClasses); backend/src/main/java/com/apptolast/organization/adapter/http/ApiErrors.java:210-270

**Evidencia.** El ambito declara "com.apptolast.organization.adapter.http.Automation*", que NO casa con la clase ApiErrors. Sin embargo `git blame -L 205,272` sobre ApiErrors.java devuelve 62 lineas con summary "feat(automations): exponer la API privada de reglas, simulacion e historial" (comit d4cb3d2) y solo 6 de otra feature. Esas 62 lineas son los @ExceptionHandler de UnknownEventTypeException (linea 213, `problem(400, "UNKNOWN_EVENT_TYPE", ...)` con FieldError sobre "trigger.eventType"), AutomationTemplateException (227, INVALID_TEMPLATE), AutomationTargetNotFoundException (235-241, `ResponseEntity.status(422)` + FieldError TARGET_NOT_FOUND), WebhookEndpointNotFoundException (247-253, 422 ENDPOINT_NOT_FOUND), AutomationLimitException (259-261, `problem(409, "RULE_LIMIT", ...)` y `body.put("limit", AutomationRuleStore.RULE_LIMIT)`) y AutomationConflictException (267-269, `ResponseEntity.status(412)` AUTOMATION_CONFLICT). Contraste leido en el mismo build.gradle.kts: las lineas 201, 235 y 307 declaran literalmente "com.apptolast.organization.adapter.http.ApiErrors" dentro de taskStatusAdapters y availabilityAdapters, es decir, la convencion del repositorio es incluir ApiErrors en el ambito de quien le anade manejadores.

**Arreglo.** Anadir "com.apptolast.organization.adapter.http.ApiErrors" al conjunto automationsClasses de backend/build.gradle.kts (junto a las entradas adapter.http.Automation*), igual que hacen taskStatusAdapters (linea 201) y availabilityAdapters (lineas 235 y 307). Es una linea. Despues volver a lanzar la campana automations-backend, que ya no se puede reutilizar la anterior si la hubiera.

**Correccion del verificador.** Titulo corregido: "El ambito PIT de automations omite ApiErrors, rompiendo la convencion 3/3 del repositorio; la puerta de mutacion no mide los seis manejadores de error de la feature (gravedad media, no alta)".

Hecho verificado: backend/build.gradle.kts:492-511 declara automationsClasses con "com.apptolast.organization.adapter.http.Automation*", patron que no casa con la clase ApiErrors. Los seis manejadores de la feature 30 estan en backend/src/main/java/com/apptolast/organization/adapter/http/ApiErrors.java lineas 210-271: unknownEventType (210, 400 UNKNOWN_EVENT_TYPE con FieldError sobre trigger.eventType), template (224, 400 INVALID_TEMPLATE), automationTarget (232, 422 TARGET_NOT_FOUND), automationEndpoint (244, 422 ENDPOINT_NOT_FOUND), automationLimit (256, 409 RULE_LIMIT y body.put("limit", AutomationRuleStore.RULE_LIMIT) en 260) y automationConflict (264, 412 AUTOMATION_CONFLICT). git log sobre ApiErrors.java confirma que las unicas features previas que le anadieron manejadores fueron complete_reopen, availability y reschedule, y las tres declaran "com.apptolast.organization.adapter.http.ApiErrors" en su ambito (build.gradle.kts lineas 201, 235 y 307). Automations es la unica excepcion.

Riesgo real, ya acotado: el bloqueante alegaba que AutomationsApiTest no ejerce esas ramas. Es falso. AutomationsApiTest.java las ejerce con aserciones que discriminan: 159-161, 214-230, 247-249, 300-304, 321-327, 425-426, 523-524, 749-750 y 862-863. Con los mutadores por defecto de PIT (el build no fija mutators), NULL_RETURNS y VOID_METHOD_CALLS sobre cinco de los seis manejadores morirían con lo que ya hay. El unico mutante que previsiblemente sobrevive es el VOID_METHOD_CALLS sobre ApiErrors.java:260 (body.put("limit", ...)), porque ninguna prueba asserta $.limit (grep de RULE_LIMIT en todo el repo: solo aparece en produccion, en la asercion de $.code de AutomationsApiTest:426 y en recuentos hasSize de persistencia/aplicacion). Ese campo limit NO esta en el contrato: features/automations.feature:135-148 usa la columna "total" para "el total de reglas queda 20", el recuento de reglas de la cuenta, no un campo del cuerpo de error. Es decir, es produccion por encima del .feature.

Tambien hay que corregir la premisa temporal: no existe progress/mutation_automations*.md ni progress/judge_automations.md. Ninguna campana ha terminado en verde ignorando el fichero; la puerta de mutacion de automations aun no se ha ejecutado ni documentado. El defecto es prospectivo, sobre la configuracion que usaria esa campana.

Accion pedida antes de cerrar la puerta de mutacion (no antes del juicio funcional):
1. Anadir "com.apptolast.organization.adapter.http.ApiErrors" a automationsClasses, igual que en taskStatusAdapters, availabilityAdapters y rescheduleClasses. targetTests de automations ya es "com.apptolast.organization.*" (build.gradle.kts:543), asi que PIT dispone de toda la bateria para matar tambien los manejadores de otras features que entren de rebote con el fichero completo.
2. Ejecutar la campana con ese ambito y registrar en progress/mutation_automations_backend.md la lista de supervivientes en ApiErrors, no solo el porcentaje.
3. Decidir sobre ApiErrors.java:260: o se asserta $.limit en el escenario s9 de AutomationsApiTest, o se elimina el campo por no estar en el contrato. Cualquiera de las dos cierra el unico hueco real.

Gravedad: media. Es higiene de la puerta con precedente unanime en el repositorio, no un agujero de comportamiento: el contrato HTTP de los seis manejadores ya tiene oraculos que discriminan status, code y field.

### 11. [MEDIA · una hora] La configuracion de Stryker no muta la ruta ni la entrada de navegacion que anade la feature, al contrario que las seis configuraciones hermanas

- **Dimension:** mutacion
- **Rutas:** frontend/stryker.automations.config.json:10-13; frontend/src/App.tsx:47,55-56,84-85; frontend/src/workspace.tsx:128-133

**Evidencia.** stryker.automations.config.json declara `"mutate": ["src/automations-api.ts", "src/automations.tsx"]` y nada mas. El codigo de produccion que la feature anadio fuera de esos dos ficheros es, leido en el arbol actual: App.tsx:47 `  const automations = route === "/automatizaciones";`, App.tsx:55-56 `        automations` / `          ? "Automatizaciones"`, App.tsx:84-85 `      {automations && username ? (` / `        <Automations owner={username} />` y workspace.tsx:128-133, el bloque `<RouteLink href="/automatizaciones" aria-current={section === "Automatizaciones" ? "page" : undefined}>Automatizaciones</RouteLink>`. Repase todas las configuraciones Stryker del repositorio: stryker.ics-calendar.config.json (la feature 26, el caso analogo mas reciente, que tambien anade ruta y entrada de menu) fija `src/App.tsx:40:8-40:42`, `src/App.tsx:65:20-66:34`, `src/App.tsx:106:10-107:37` y `src/workspace.tsx:99:10-104:22`, y verifique leyendo esas lineas que seleccionan justo el predicado de ruta, la rama del ternario `section` y la rama de render de calendario, mas su RouteLink. Lo mismo hacen appearance, export-data, history, import-data e integration-api: seis configuraciones con el mismo cuarteto de rangos. La de automations tiene cero.

**Arreglo.** Anadir a `mutate` en frontend/stryker.automations.config.json los cuatro rangos que corresponden, siguiendo la convencion de stryker.ics-calendar.config.json: el predicado de ruta (App.tsx linea 47, de la columna del identificador al final de la expresion, longitud de linea 52), la rama del ternario `section` (App.tsx 55-56), la rama de render (App.tsx 84-85) y el RouteLink completo (workspace.tsx 128-133, longitud de la ultima linea 22). Validar cada rango con el mismo oraculo de contenido que ya usa scripts/project.test.mjs antes de escribirlo. OJO: el guardarrail `automations Stryker configuration mutates only the feature files` hace `assert.deepEqual(config.mutate, [...])` y esta DUPLICADO en scripts/project.test.mjs:2161-2170 y :2204-2213; hay que actualizar las dos copias o el test rompera.

**Correccion del verificador.** Titulo corregido: El ambito de mutacion de automations deja fuera las lineas de integracion con el armazon (App.tsx y workspace.tsx), omision compartida con las features 25, 27 y 28 del mismo lote.

Rutas: frontend/stryker.automations.config.json:10-13; frontend/src/App.tsx:47,55-56,84-85; frontend/src/workspace.tsx:128-133; frontend/src/automations-route.test.tsx; scripts/project.mjs (targets automations-frontend / external_calendar-frontend / github_connector-frontend, y ausencia de cualquier target de webhooks).

Evidencia verificada leyendo: el mutate de stryker.automations.config.json es ["src/automations-api.ts","src/automations.tsx"]. El codigo de produccion que la feature anadio fuera de esos dos ficheros (confirmado por git log -S automatizaciones -> be87ac6) es App.tsx:47 (predicado de ruta), App.tsx:55-56 (rama del ternario section) y App.tsx:84-85 (rama de render), mas workspace.tsx:128-133 (el RouteLink con su aria-current). Ninguna configuracion invocable las muta: stryker.config.json si lista App.tsx y workspace.tsx completos, pero no aparece en scripts/project.mjs, luego no hay target que lo ejecute. docs/mutation-testing.md exige el umbral sobre las lineas nuevas o tocadas por la feature, asi que el ambito declarado es incompleto respecto a esa regla.

Contexto que corrige la version original: no es una desviacion de la feature 30 frente a seis hermanas. stryker.external-calendar.config.json y stryker.github-connector.config.json tienen exactamente la misma omision (cero rangos de App.tsx pese a anadir predicado de ruta, rama de section y rama de render), y la feature 25 (webhooks) no tiene configuracion Stryker ni target de mutacion frontend alguno pese a anadir App.tsx:43,57-58,86-87 y workspace.tsx:122-127. Solo ics_calendar, dentro del lote, mantiene el cuarteto de rangos. Es deriva de convencion de todo el lote 25-30, no un fallo aislado de automations.

Riesgo real: bajo. Las pruebas ya muerden esas lineas: automations-route.test.tsx afirma href="/automatizaciones" y aria-current="page" en la ruta, que links[0] sigue siendo "Hoy" y que la entrada no lleva aria-current cuando se navega a /exportacion. Un mutante del literal de cadena, la inversion de && username o la rotura del ternario de aria-current moririan. Lo que falta es la evidencia por campana, no la deteccion.

Gravedad corregida: baja.

Remedio propuesto: anadir a stryker.automations.config.json los rangos "src/App.tsx:47:8-47:44", "src/App.tsx:55:10-56:36", "src/App.tsx:84:10-85:37" y "src/workspace.tsx:128:10-133:22" (verificar columnas contra el arbol en el momento de editar, porque App.tsx se desplaza con cada feature) y relanzar solo la campana automations-frontend cuando la maquina este libre. Y registrar como deuda de lote, fuera de la puerta de la feature 30, la misma enmienda para external_calendar y github_connector y la creacion del ambito y el target de mutacion frontend inexistentes de webhooks. Si el juez decide bloquear por esto, debe bloquear las cuatro por igual; bloquear solo automations seria incoherente con lo que ya paso el mismo lote.

