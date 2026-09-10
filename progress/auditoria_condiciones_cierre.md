# Auditoria adversarial de las condiciones de cierre — 10 de septiembre de 2026

Trece agentes: cuatro auditan (uno por feature con aprobacion condicionada) y dos
refutan cada auditoria, uno buscando indulgencia y otro buscando severidad
injusta. Auditan el trabajo del ORQUESTADOR, no el de los artesanos.

Motivo: esta madrugada el orquestador se equivoco cuatro veces y las cuatro las
cazo otro. Si quien impone la puerta se autoexime de ella, la puerta no vale.

---

{
  "summary": "Audit, adversarially, whether the non-mutation conditions of the four conditional approvals are really closed",
  "agentCount": 13,
  "logs": [
    "4 features auditadas, 2 con objeciones"
  ],
  "result": {
    "vivos": [
      {
        "feature": "25-webhooks",
        "auditoria": {
          "feature": "25 webhooks",
          "condiciones": [
            {
              "id": "1. AnchoredConnection en el ambito PIT (estaba en ninguno: cero mutantes)",
              "estado": "cerrada",
              "evidencia": "Leido en backend/build.gradle.kts: la clase esta ahora en webhooksClasses (linea 628, con comentario en :624-627) y tambien en externalCalendarClasses (:454). El patron es exacto: com.apptolast.organization.adapter.net.AnchoredConnection*, y existe backend/src/main/java/com/apptolast/organization/adapter/net/AnchoredConnection.java. Las dos guardas del arnes estan sincronizadas y son de igualdad estricta, no de 'contiene': scripts/project.test.mjs:2346 (deepEqual de la lista completa de webhooksClasses, ahora 39 patrones) y :2075 (calendario externo). Ademas la guarda de :2399 ('every webhooks mutation pattern resolves to a file that exists today') resuelve cada patron a un .java existente, asi que un FQN mal escrito rompe. El comentario anadido no lleva comillas, de modo que no falsea el regex de la guarda. La rama else de :655 hereda el ambito porque incluye webhooksClasses, y nightFiveOnly tambien (union de conjuntos, sin doble conteo). No es solo declarativo: YA esta medido. backend/build/reports/pitest-webhooks/mutations.xml (escrito a las 10:53:06, despues del commit f969d608 de las 10:33) contiene 21 mutantes de AnchoredConnection, 18 KILLED y 3 SURVIVED, y existe el directorio de informe com.apptolast.organization.adapter.net/AnchoredConnection.java.html. Cero mutantes se acabo. Nota, no defecto de esta condicion: los 3 supervivientes caen justo en las lineas que el juez predijo -literal:48 negated conditional, authority:57 negated conditional, isAddressLiteral:76 negated conditional-, es decir, anadirla al ambito ha destapado hueco real que nadie ha cerrado todavia; la puntuacion agregada del informe (382 KILLED de 425) sigue por encima de 0,80.",
              "queFalta": ""
            },
            {
              "id": "2. project-spec.md:2050, el reenlace DNS declarado como limite abierto",
              "estado": "cerrada",
              "evidencia": "project-spec.md:2050, dentro de '### Limites explicitos' de la feature 25 (encabezado en :2048), dice ahora 'DNS rebinding entre comprobacion y conexion **si queda cerrado**: la peticion viaja a la direccion ya validada y no vuelve a resolver el nombre (AnchoredConnection), conservando el nombre para la verificacion del certificado; hay prueba de TLS en las dos direcciones'. Antes decia literalmente 'no queda cerrado por la aplicacion' (diff de f969d608 leido). Contrastado con las tres fuentes que el juez citaba y coincide con las tres: project-spec.md:2492 (enmienda B3, 'el reenlace de nombres entre la comprobacion y el uso queda cerrado, no aceptado'), deploy/EGRESS.md:12-20 ('Que cierra la aplicacion': resuelve una vez, valida todas las direcciones, y la peticion viaja contra esa misma direccion ya validada) y el codigo, JdkWebhookSender.java:157 (validatedAddress) y :220-221 (AnchoredConnection.literal + cabecera Host). Residuo cosmetico, no bloqueante: la frase final 'Este limite se declaro abierto hasta el 10 de septiembre de 2026 y la linea sobrevivio a la decision del propietario de anclarlo: lo corrige el juez de cierre de la feature 25' es historia de proceso metida en una seccion normativa; el siguiente lector la leera como parte del limite.",
              "queFalta": ""
            },
            {
              "id": "3. latencyMs desde un cronometro inyectado (features/webhooks.feature:320)",
              "estado": "cerrada_a_medias",
              "evidencia": "El mecanismo es real, no aparente. Campo ticker (java.util.function.LongSupplier) en JdkWebhookSender.java:71; se usa en los DOS extremos de la medida: :152 'var started = ticker.getAsLong()' y :239 dentro de elapsedMillis, que dejo de ser static. Constructor de 7 argumentos package-private en :122-137; produccion entra por :113, que cablea System::nanoTime. Inyectable de verdad desde una prueba: JdkWebhookSenderTest.java esta en el mismo paquete (linea 1) y construye el emisor con su propio cronometro en :99-110 (senderTicking, LongSupplier respaldado por un AtomicInteger que va devolviendo la secuencia). La prueba PUEDE FALLAR: :525-534 inyecta 1.000.000 y 1.251.000.000 nanos y afirma assertEquals(1250, outcome.latencyMs()) mas assertEquals(200, httpStatus) contra un receptor real en 127.0.0.1 (url() en :141-142). Borrado mental del codigo de produccion: si started vuelve a System.nanoTime, la resta da un numero enorme negativo que Math.max clava en 0 y la asercion cae; si el final vuelve a System.nanoTime, da del orden de 10^6 ms y tambien cae; con el codigo viejo entero ni compila, porque el constructor de 7 argumentos no existia. 1250 ms exactos son inalcanzables por casualidad contra un receptor local que responde en microsegundos y con exchange deadline de 300 ms (:56). Ninguna otra prueba del fichero pasa por senderTicking, asi que no hay verde por arrastre.",
              "queFalta": "Tres residuos. (a) DOCUMENTAL, el que el juez pedia resolver: la clausula dice 'medido con el RELOJ inyectado' y lo que hay es un SEGUNDO temporizador inyectado, deliberadamente distinto del Clock (javadoc :64-69); el Clock inyectado sigue alimentando solo la t de la firma (:228). Ni features/webhooks.feature:320 ni project-spec.md:2038 ('latency_ms medido con el reloj inyectado') se han tocado, y no ha pasado por la puerta de aprobacion humana. El juez ofrecia dos puertas -cambiar el codigo con su rojo, o enmendar la linea por la puerta humana-; se ha usado la primera, pero la letra del contrato sigue diciendo algo que el codigo no hace literalmente. Que el juez ratifique la lectura, o que se enmiende la linea a 'cronometro monotono inyectado' con su ratificacion. (b) EL ROJO NO ESTA ACREDITADO EN NINGUNA BITACORA: solo se afirma en el mensaje del commit 1eaa622c ('Rojo acreditado devolviendo el reloj ambiente al fichero de produccion: la prueba cae'). Ningun progress/tdd_*.md se ha tocado desde las 10:31 (progress/tdd_webhooks_cierre_dictamen.md sigue en 01:00), y el checkpoint C3 del propio dictamen elogiaba justo lo contrario: tablas con la mutacion aplicada y el rojo obtenido, no una afirmacion. (c) LA CAMPANA QUE HAY EN DISCO NO LO MIDE: backend/build/reports/pitest-webhooks/mutations.xml (10:53:06) trae el mutante de elapsedMillis 'Replaced long subtraction with addition' como SURVIVED y no menciona s25_latencyMs como killingTest; las clases se recompilaron a las 10:53:43-45, o sea que ese informe es de la corrida anterior a la prueba nueva. Con la prueba, ese mutante muere (1252 != 1250). Si se adjunta ese XML como evidencia de la puerta de mutacion, se estara adjuntando una corrida que no incluye el cambio: hay que esperar a la campana en vuelo. Ademas, cosmetico: el javadoc de JdkWebhookSenderTest.java:92 ('The same sender with the two deadlines shortened...') quedo huerfano encima del javadoc nuevo, describiendo ahora a senderTicking, y senderWithShortDeadlines (:111) se quedo sin comentario."
            },
            {
              "id": "EXTRA, fuera de mis tres: condicion 4 del dictamen (limite de field-sizing: content a Limites explicitos de la 25)",
              "estado": "declarada_no_cerrada",
              "evidencia": "El dictamen la exige explicitamente en 'Cambios requeridos', punto 4, y en 6.2 ('Si exijo que deje de vivir solo en una bitacora de carril: pertenece a ### Limites explicitos de la feature 25 en project-spec.md'). Comprobado: grep 'field-sizing' sobre project-spec.md no devuelve NADA, y grep 'Chromium' solo devuelve :284 y :1302, ambas de otras secciones. El parrafo de limites de la 25 (project-spec.md:2050) no lo menciona. No la incluia el orquestador entre las tres que declara cerradas, pero es una de las seis condiciones del veredicto y sigue abierta.",
              "queFalta": "Anadir a project-spec.md:2050 el limite de que field-sizing: content solo lo implementa Chromium (frontend/src/webhooks.scss:143-149) y que en Firefox y Safari el secreto se recortaria al 200 % de texto, con el analisis del artesano."
            }
          ],
          "bloqueaElCierre": true
        },
        "objeciones": [
          "La condicion 1 esta marcada \\\"cerrada\\\" y no lo esta. El auditor uso backend/build/reports/pitest-webhooks/mutations.xml para declarar \\\"Cero mutantes se acabo\\\" y cito su agregado (382 KILLED de 425, por encima de 0,80) como si el informe acreditara la puerta. En ESE MISMO informe, Slf4jWebhookAudit recibe CERO mutantes: `grep -c \"Slf4jWebhookAudit\" mutations.xml` devuelve 0 y no existe el directorio com.apptolast.organization.adapter.logging/ en el informe. Recuento de las 20 clases que el dictamen declara obligatorias: 19 reciben mutantes, Slf4jWebhookAudit ninguno. Y Slf4jWebhookAudit esta nombrada literalmente en progress/judge_webhooks_cierre.md:271, dentro de \"Clases que tienen que recibir mutantes si o si\", bajo la regla dura de :265-267: \"Si el informe muestra cero mutantes generados para alguna de estas, la puerta no vale aunque la puntuacion salga alta: es el patron de AesGcmSecretCipher\". Es exactamente la patologia que la condicion 1 venia a extinguir, sobreviviendo en el mismo fichero que se adjunta como prueba de su extincion.\n\nAdemas no es un fallo de ambito que se arregle relanzando la campana. La clase esta bien declarada en webhooksClasses y fijada por la guarda de scripts/project.test.mjs:2346, y su fichero existe (backend/src/main/java/com/apptolast/organization/adapter/logging/Slf4jWebhookAudit.java): sus tres metodos contienen solo llamadas org.slf4j, y el bloque pitest de backend/build.gradle.kts:725-745 no fija ni `mutators` ni `avoidCallsTo`, de modo que rige el avoidCallsTo por defecto de PIT —que incluye org.slf4j— y suprime VOID_METHOD_CALLS ahi. Verificado como patron estructural del repo, no accidente de esta corrida: pitest-external-calendar/mutations.xml y pitest-automations/mutations.xml tambien dan `grep -c Slf4j` = 0 pese a declarar sus adapter.logging.Slf4j*Audit*. Ninguna campana en vuelo lo cierra; hace falta acreditacion explicita del juez (como los seis literales SQL que si acredita en su §7) o cambiar mutadores/avoidCallsTo.\n\nSegundo flanco de indulgencia dentro de la misma condicion: los 3 supervivientes de AnchoredConnection se archivan como \"Nota, no defecto de esta condicion\". El §7 del dictamen enumera taxativamente los supervivientes que acepta por lectura (los seis literales SQL y JdkWebhookSender.classify()); AnchoredConnection no esta entre ellos, y el juez escribio expresamente que sus nueve pruebas propias \"no las mide nadie\". El objeto de la condicion era que esa clase quedara medida; medida, devolvio tres huecos que nadie ha matado ni acreditado, en literal:48, authority:57 e isAddressLiteral:76. Correccion factual menor a la auditoria: son ConditionalsBoundaryMutator (\"changed conditional boundary\"), no \"negated conditional\" como afirma.\n\nLo demas de la auditoria lo verifique y se sostiene. Condicion 2: project-spec.md:2050 dice ahora \"**si queda cerrado**\" y concuerda con :2492, deploy/EGRESS.md y el codigo; el residuo historico que señala existe y es cosmetico. Condicion 3: el mecanismo es real y la prueba PUEDE fallar —senderTicking (JdkWebhookSenderTest.java:99-110) se usa en un unico sitio (:530; solo :383 usa senderWithShortDeadlines, confirmado por grep) y elapsedMillis (JdkWebhookSender.java:239) dejo de ser static y llama al ticker—, asi que su \"cerrada_a_medias\" por la letra del contrato y por el rojo no acreditado es correcto. La condicion 4 (field-sizing) sigue abierta como dice, y el veredicto global bloqueaElCierre: true es correcto. Lo que falla es el grado asignado a la condicion 1.",
          "OJO: refuto en la direccion CONTRARIA a la que sugiere el campo. No he encontrado ninguna condicion declarada cerrada que no lo este —las dos que la auditoria da por cerradas lo estan y las he verificado a mano—. Lo que hay es SEVERIDAD INJUSTA en la condicion 3.\n\nLo que confirmo de la auditoria (correcto, sin objecion):\n- Cond. 1: backend/build.gradle.kts:628 y :454 llevan com.apptolast.organization.adapter.net.AnchoredConnection*; el fichero existe en backend/src/main/java/com/apptolast/organization/adapter/net/AnchoredConnection.java; las dos guardas de scripts/project.test.mjs (:2346 y :2075) estan sincronizadas; mutations.xml de pitest-webhooks trae 21 apariciones de AnchoredConnection. Cerrada.\n- Cond. 2: project-spec.md:2050 dice hoy \"si queda cerrado\". Cerrada.\n- EXTRA (field-sizing, punto 4 de \"Cambios requeridos\"): grep de field-sizing sobre project-spec.md no devuelve nada; solo vive en frontend/src/webhooks.scss:143-149. Abierta de verdad, y el juez la pide literalmente en 6.2 y en el punto 4. Severidad JUSTA.\n- bloqueaElCierre: true es correcto igualmente, por esa condicion 4 y por C7 (las dos campanas, punto 1 del juez), que el propio veredicto deja sin marcar.\n\nDONDE LA AUDITORIA SE PASA DE DURA — condicion 3, \"cerrada_a_medias\":\nEl juez (progress/judge_webhooks_cierre.md, 6.3 y punto 5 de \"Cambios requeridos\") abrio DOS puertas y solo dos: \"enmendar features/webhooks.feature:320 por la puerta de aprobacion humana, O cambiar el codigo con su rojo primero\". La puerta humana es requisito de la puerta 1, no de la 2. Se tomo la 2, y el defecto que el juez nombraba —\"lo miden con System.nanoTime()\", es decir, ambiente— ya no existe: JdkWebhookSender.java (esta en adapter/webhook, no en adapter/net como sugiere la auditoria) tiene el campo ticker LongSupplier, lo usa en los dos extremos de la medida, produccion lo cablea con System::nanoTime, y JdkWebhookSenderTest.java:524-534 (s25_latencyMsComesFromTheInjectedTickerAndNotFromTheAmbientClock) afirma assertEquals(1250, latencyMs) con nanos inyectados: devolver el reloj ambiente tumba la prueba. La propia auditoria lo concede entero (\"el mecanismo es real\", \"la prueba PUEDE FALLAR\", \"sin verde por arrastre\") y aun asi la puntua a medias.\n\n(a) Exige una tercera puerta que el contrato no pide: \"que el juez ratifique la lectura, o que se enmiende la linea con su ratificacion\". El juez no condiciono la puerta 2 a ninguna ratificacion ni a la puerta humana. Y la lectura estrecha (\"reloj\" tiene que ser el Clock) es la peor de las dos: medir tiempo transcurrido con un reloj de pared es un defecto conocido, no un cumplimiento; el javadoc de :64-69 reconcilia las dos lecturas citando features/webhooks.feature:320 en el punto de uso. La mitad de la clausula que el juez declaraba falsa hoy es verdadera en sustancia y en una lectura defendible de la letra.\n\n(b) Exige acreditar el rojo en una bitacora. El hecho es cierto —1eaa622c no toca ningun progress/tdd_*.md y el rojo solo se afirma en el mensaje de commit—, pero la condicion 5 dice \"con su rojo primero\", no \"documentado en bitacora\", y C3 ya estaba marcado [x] en el veredicto. Ademas el rojo es verificable por construccion sin creerle a nadie (el borrado mental del ticker rompe la asercion), que es justo como el propio juez trato las acreditaciones del artesano en la seccion 7: verificandolas en el fichero de prueba, no exigiendo mas prosa.\n\n(c) Cuelga de esta condicion un asunto que es de otra: que backend/build/reports/pitest-webhooks/mutations.xml sea anterior a la prueba nueva es materia del punto 1 del juez (las dos campanas, C7 sin marcar), no de la clausula latencyMs. Aviso sensato, pero inflar con el la nota de la condicion 3 la hace parecer a medias cuando esta cumplida.\n\nEfecto practico: nulo sobre el bloqueo (sigue bloqueado por la 4 y por C7), pero la condicion 3 deberia figurar como CERRADA con una nota no bloqueante, no como \"cerrada_a_medias\" con tres deberes; tal como esta, invita a abrir una ronda de juez y una puerta de aprobacion humana que nadie ha pedido. Cosmetico y cierto: el javadoc huerfano de JdkWebhookSenderTest.java:92 describe ahora a senderTicking."
        ],
        "malJuzgadas": [
          "1. AnchoredConnection en el ambito PIT (estaba en ninguno: cero mutantes) — marcada \"cerrada\", en realidad cerrada a medias",
          "3. latencyMs desde un cronometro inyectado (features/webhooks.feature:320) — marcada 'cerrada_a_medias'; esta cerrada por la puerta 2 que el propio juez ofrecio (punto 5 de Cambios requeridos). Los residuos (a) ratificacion del juez / puerta humana y (b) acreditacion del rojo en bitacora son gates que el veredicto no impone, y (c) la frescura de mutations.xml pertenece al punto 1 (C7), no a esta condicion."
        ]
      },
      {
        "feature": "27-github",
        "auditoria": {
          "findings": "[]",
          "feature": "27 — conector GitHub (frontend, campaña de mutación de cierre)",
          "condiciones": [
            {
              "id": "Frontend-1 — puntuación mínima 80",
              "estado": "cerrada",
              "evidencia": "`frontend/reports/mutation-github-connector/mutation.json` recontado entero: 3 ficheros, 589 mutantes, estados SOLO `Killed` (501) y `Survived` (88). Cero `NoCoverage`, cero `Timeout`, cero `CompileError`, cero `Ignored` — es decir, no hay ninguna categoría excluida del denominador que pudiera inflar la cifra. 501/589 = 85,0594 % = **85,06 %**, exactamente lo declarado en `progress/mutacion_github_connector_frontend_cierre.md:10`. Por fichero: `src/github-connector-client.ts` 215/246 = 87,40 %; `src/github-connector.tsx` 284/341 = 83,28 %; `src/integrations-index.tsx` 2/2 = 100 %. Las tres cifras de la tabla del informe (:7-9) cuadran al segundo decimal. `frontend/stryker.github-connector.config.json` fija `thresholds.break: 80`, y el propio `mutation.json` lo lleva embebido. Coherencia interna extra: `progress/mutacion_github_connector_supervivientes.md:3-6` da el punto de partida 409/175/5 = 589, el mismo universo de mutantes; el recorrido 69,44 → 85,06 % es sobre el mismo denominador, no sobre un ámbito reapuntado.",
              "queFalta": ""
            },
            {
              "id": "Frontend-2 — total declarado y ningún módulo de `mutate` a cero",
              "estado": "cerrada",
              "evidencia": "`mutation.json` → `config.mutate` son los tres ficheros que exige el juez, y `files` tiene esas tres claves y solo esas. Recuento: 246 + 341 + 2 = **589**, lo declarado en el informe (:17-18). Ninguno a cero. `src/integrations-index.tsx` es el sospechoso (solo 2 mutantes), así que lo he mirado uno a uno: `BlockStatement -> {}` en :7 y `UnaryOperator +1` sobre el `tabIndex={-1}` de :9, ambos `Killed`. Leído el fichero (32 líneas, JSX casi puro; el texto JSX no genera `StringLiteral`), 2 es el número plausible, no la señal de un glob muerto. Y lo que el juez temía —que `coverageAnalysis: \"perTest\"` no encontrara pruebas— queda descartado por el otro lado: **0 `NoCoverage` en los tres módulos**, cuando en la primera medición había 5 en `github-connector.tsx`.",
              "queFalta": ""
            },
            {
              "id": "Frontend-3 — mutantes del escuchador de Escape (`github-connector.tsx:149-159`) muertos",
              "estado": "cerrada",
              "evidencia": "Leído el fuente: `frontend/src/github-connector.tsx:149-151` son comentario y el bloque real es **152-161**. En el rango literal 149-159 hay 14 mutantes solapantes; quitando el `BlockStatement` del componente entero (57-460, que solapa con todo), quedan **13, los 13 `Killed`** — el número exacto del informe (:21). Ampliado al bloque completo 152-161: **17 mutantes, 17 `Killed`, 0 vivos**. Y los `killedBy` de `mutation.json` nombran a los verdugos: son las unitarias @s42 de `github-connector.test.tsx` («Escape closes the confirmation and returns the focus to Desconectar», «no escucha Escape mientras no hay ninguna confirmación abierta», «sólo Escape cierra la confirmación: otra tecla no la toca», «el Escape que cierra la confirmación queda consumido», «retira el escuchador de Escape al cerrarse la confirmación»). Las dos mitades que el juez exigió por nombre están cubiertas: la guarda `if (!confirming) return` de :153 muere por tres vías (`ConditionalExpression -> true`, `-> false` y `BooleanLiteral`), y el **desregistro** de :160 muere en sus dos mutantes (`ArrowFunction -> () => undefined` y el `StringLiteral` del nombre del evento), a manos de una prueba dedicada. Aviso de método, no de resultado: **el desregistro está en la línea 160, FUERA del rango 149-159 que el orquestador contó**. Su cifra sale bien, pero el rango que usó habría dejado el desregistro fuera; acertó sin cubrir lo que el juez nombró. La verificación se sostiene porque la he ampliado yo.",
              "queFalta": ""
            },
            {
              "id": "Ambas-4 — medida sobre este árbol, con el commit declarado en el informe",
              "estado": "cerrada_a_medias",
              "evidencia": "Esta es la condición que el orquestador no ha visto: está en la sección «### Ambas» del veredicto (`progress/judge_github_connector_cierre.md:299-300`) y aplica también al frontend. Dice literalmente: «El informe debe decir contra qué commit se midió». `progress/mutacion_github_connector_frontend_cierre.md:3` dice solo «sobre `main`» — **ningún hash**. Y no es un descuido inocuo: `mutation.json` lleva embebido el `source` de cada fichero mutado, y lo he comparado byte a byte con el disco. `src/github-connector.tsx` y `src/integrations-index.tsx` coinciden exactamente. `src/github-connector-client.ts` **NO**: el informe midió `const RECEIPT_FIELDS` / `const CONNECTION_FIELDS`, y en disco son `export const` desde el commit `44011086` («test(29): C4 la prueba de contrato apunta por fin al conector que se rompio», 2026-09-10 10:45:01), del carril de la feature 29. Lo mismo con las pruebas: `src/github-connector-client.test.ts` figura en el informe con 554 líneas y 46 tests, y en disco tiene 620 y 48. `mutation.json` se escribió a las 10:55. Es decir: **main se movió por debajo de la campaña mientras corría, y el informe mide un árbol que ya no existe.** He medido el daño real en vez de suponerlo, y es benigno: el cambio de producción son dos `export` más un reformateo de una constante en dos líneas (`git show 44011086` → 5 líneas, sin cambio de comportamiento y sin generar ningún mutante nuevo), el cambio de pruebas es **puramente aditivo** (`--numstat`: 66 insertadas, 0 borradas), y he ejecutado ese único fichero sobre el árbol de hoy: `npx vitest run src/github-connector-client.test.ts` → **48/48 verdes**. Con dos oráculos más y el mismo conjunto de nodos mutables, 85,06 % es un suelo, no una cifra inflada. Sustancia: buena. Declaración: incumplida.",
              "queFalta": "Rehacer dos frases del informe, no la campaña: (1) escribir el commit contra el que se midió, que es el padre de `44011086` y no el `main` de ahora; (2) anotar que `src/github-connector-client.ts` y su fichero de pruebas derivaron durante la ejecución por la entrada del carril 29, y por qué eso no mueve la puntuación (los dos `export` no generan mutantes; las 66 líneas de prueba son aditivas y verdes). Sin eso, el informe afirma una medición sobre un árbol que no es el que dice."
            },
            {
              "id": "C7 — disposición de los supervivientes del frontend",
              "estado": "cerrada_a_medias",
              "evidencia": "El único documento de supervivientes, `progress/mutacion_github_connector_supervivientes.md`, es del **9-sep 20:40** y describe el estado de **175 supervivientes / 69,44 %**, no el de la campaña de cierre. Los **88 supervivientes** que quedan vivos hoy (57 en `github-connector.tsx`, 31 en `github-connector-client.ts`, repartidos en 66 líneas distintas — no hay racimo dominante) no tienen disposición uno por uno en ninguna parte. En letra estricta el juez pide eso en el punto 8 de la sección «### Backend — PIT», con destino `progress/mutation_github_connector.md`, así que **no es una condición del frontend** y no la cuento como incumplida; pero el checkpoint C7 (`:281-282`) está redactado en general y quien lea el informe de frontend no encontrará ni un mutante vivo justificado.",
              "queFalta": "Nada exigible por la letra del veredicto en el lado frontend. Si se quiere honrar C7 tal como está redactado, falta la disposición de los 88 vivos (matado o equivalente), o una frase explícita que acote el punto 8 al backend."
            },
            {
              "id": "Backend — PIT, ámbito `github_connector` (puntos 1-8 del veredicto)",
              "estado": "declarada_no_cerrada",
              "evidencia": "Fuera de mi encargo, pero lo señalo porque es lo que decidirá el cierre: `progress/mutation_github_connector.md` **no existe** (solo hay `mutacion_github_connector_frontend_cierre.md` y `mutacion_github_connector_supervivientes.md`), y `backend/build/reports/pitest` está vacío. La campaña de backend es la que lleva las ocho exigencias duras del juez (`:243-282`): 80 de puntuación, total declarado, desglose por clase, suelo de 12 mutantes en `AesGcmSecretCipher` con cero `NO_COVERAGE`, los mutantes de `:51`, `:52-53`, `:60`, `:61-62` y `:25` `KILLED` uno por uno —«un superviviente en cualquiera de estos puntos es rechazo, aunque la puntuación global pase»—, `ConnectorKeyRing` con al menos 1 mutante, y ninguna clase del ámbito a cero salvo los cuatro portadores de datos que él enumera. Nada de eso está sobre disco. Puede estar corriendo ahora mismo (hay dos campañas vivas), pero hoy no hay evidencia.",
              "queFalta": "La campaña de backend entera y su informe, con el recuento al lado del porcentaje y el veredicto mutante a mutante sobre los cinco puntos que el juez enumera. Sin eso, C7 no se puede marcar."
            },
            {
              "id": "Condiciones no relativas a la mutación (puntos 5, 6 y 7 del veredicto)",
              "estado": "cerrada",
              "evidencia": "Comprobadas porque tocan al mismo cierre. Punto 6 (deuda del `CHECK` de `V25`, con dueño y número de migración, más el comentario mentiroso de `V25:6`): **cerrada de verdad**, no anotada. `backend/src/main/resources/db/migration/V30__github_connector_ciphertext_bounds.sql` existe, hace el `DROP CONSTRAINT` + `ADD CONSTRAINT ... BETWEEN 29 AND 283` que el juez pedía palabra por palabra (`:140`), y su cabecera corrige el formato que `V25:6` describe mal —explica el byte de versión retirado y deriva 12+1+16=29 y 12+255+16=283—, que era la otra mitad del encargo. `V25:13` sigue diciendo `BETWEEN 30 AND 284`, y así debe seguir: Flyway no permite reescribirla. Punto 7 (C5, entrada en `progress/history.md`): **cerrada**, `progress/history.md:731` abre «## 2026-09-10 — feature 27 `github_connector`: carril de cierre» con la bitácora del carril. Punto 5 (`bin/harness init` verde) no lo he tocado: prohibido por instrucción de sesión, y sigue sin acreditar.",
              "queFalta": ""
            }
          ],
          "bloqueaElCierre": true
        },
        "objeciones": [
          "La auditoria es solida en lo sustantivo —recontE mutation.json y las cifras cuadran: 589 mutantes, 501 Killed, 85,06 %, cero NoCoverage; los 13 del rango 149-159 y los del bloque real 152-161 estan todos muertos por unitarias @s42 que existen en disco y pueden fallar; la deriva de github-connector-client.ts es real (const -> export const desde 44011086) y benigna (3/2 en produccion, 66 insertadas / 0 borradas en el test)— pero incurre en una indulgencia de etiquetado. Agrupa los puntos 5, 6 y 7 del veredicto en UNA entrada marcada \"cerrada\" con queFalta vacio, cuando su propia evidencia admite que el punto 5 (bin/harness init verde, C1/C4) \"sigue sin acreditar\". El juez lo puso como condicion dura y escribio \"si sale rojo, este veredicto decae\". El repositorio confirma que sigue abierto: progress/history.md:748 dice \"falta la campana de mutacion medida y bin/harness init verde, los dos en manos del orquestador\", y progress/current.md:493-495 lo lista como \"lo primero que hay que hacer al retomar\". No es formalidad: progress/current.md:94 documenta que ese mismo paso ya tumbo la CI por el lint de Prettier en ocho stryker.*.config.json. Una puerta con precedente de ponerse roja, archivada como cerrada. Ademas, error menor de recuento en Frontend-3: dice \"17 mutantes, 17 Killed\" en el bloque 152-161; los mutantes integramente dentro del bloque son 16 (llega a 17 solo incluyendo el BlockStatement del componente entero, 57-460, que el mismo habia descartado al contar el rango 149-159). Todos Killed, asi que el veredicto no cambia, pero es un desliz de conteo justo donde lo que se auditaba era la fiabilidad del conteo. Su conclusion global (bloqueaElCierre: true, por la campana de backend ausente: progress/mutation_github_connector.md no existe y backend/build/reports/pitest ni siquiera existe como directorio) sigue siendo correcta.",
          "Verifiqué las siete entradas contra disco. El grueso está bien medido: mutation.json da exactamente 246+341+2 = 589 mutantes con sólo dos estados (501 Killed / 88 Survived, cero NoCoverage), 501/589 = 85,06 %, y la deriva que denuncia en Ambas-4 es real (el `source` embebido en el informe dice `const RECEIPT_FIELDS` / `const CONNECTION_FIELDS` en una línea; el disco dice `export const` y la segunda partida en dos, por el commit 44011086 de las 10:45, con mutation.json escrito a las 10:55). Frontend-1, Frontend-2, Frontend-3 y Ambas-4 se sostienen. Pero hay dos entradas mal juzgadas, una en cada dirección.\n\n1) SEVERIDAD INJUSTA — «C7 — disposición de los supervivientes del frontend», marcada `cerrada_a_medias`. El contrato no la pide en el frontend y el propio auditor lo escribe («Nada exigible por la letra del veredicto en el lado frontend»), pero le estampa media condición igualmente, y en un cierre eso se lee como deficiencia abierta. La premisa además es un error de lectura: dice que «el checkpoint C7 (:281-282) está redactado en general». Las líneas 281-282 de progress/judge_github_connector_cierre.md NO son C7; son el punto 8, dentro de «### Backend — PIT», y su destino es progress/mutation_github_connector.md, un artefacto de backend. El texto real de C7 está en :228 y dice sólo «Mutación pendiente. **Es la condición de este veredicto.**» — es la etiqueta de la puerta entera, y el «(C7)» de :282 es una referencia hacia atrás desde el punto 8, no una cláusula general. La sección «### Frontend — Stryker» (:285-297) enumera tres requisitos y ninguno es disposición mutante a mutante. Encima, la frase «quien lea el informe de frontend no encontrará ni un mutante vivo justificado» desmiente el disco: progress/verificacion_mutantes_github_connector.json documenta 43 supervivientes uno por uno, con el mutante y las pruebas que lo matan, y progress/mutacion_github_connector_supervivientes.md cataloga los 175 anteriores. Esta entrada debería ser `cerrada` o «no aplica».\n\n2) LENIENCIA — «Condiciones no relativas a la mutación (puntos 5, 6 y 7)», marcada `cerrada` con queFalta vacío, cuando su propia evidencia dice que el punto 5 (`bin/harness init` verde) «sigue sin acreditar». El veredicto lo pone en la línea de aprobación (:3-4: «APPROVED CONDICIONADO ... y a `bin/harness init` verde en manos del orquestador (C1/C4)») y el punto 5 remata «si sale rojo, este veredicto decae». Una condición abierta archivada bajo `cerrada`. Los puntos 6 y 7 sí los verifiqué cerrados: V30__github_connector_ciphertext_bounds.sql hace el DROP + ADD ... BETWEEN 29 AND 283 palabra por palabra y su cabecera corrige el comentario mentiroso (V25:5-6 sigue diciendo «1 byte de versión de clave» y V25:13 «BETWEEN 30 AND 284», que es lo que Flyway impide reescribir), y progress/history.md:731 abre la entrada del carril.\n\nApunte menor, no mal juicio: la etiqueta `declarada_no_cerrada` del backend atribuye una declaración que nadie hizo — progress/cierre_github_connector.md:301 y :359 entregan PIT explícitamente al orquestador y dicen que el carril no lo ejecutó. La sustancia es correcta: backend/build/reports/pitest-github-connector/ existe pero está vacío (cero ficheros) y progress/mutation_github_connector.md no existe.\n\nLa conclusión bloqueaElCierre: true se mantiene, por la campaña de backend ausente y por `bin/harness init`, no por C7."
        ],
        "malJuzgadas": [
          "Condiciones no relativas a la mutacion (puntos 5, 6 y 7 del veredicto) — marcada 'cerrada' con queFalta vacio, pero el punto 5 (bin/harness init verde, C1/C4) esta declarado y no cerrado: el propio auditor lo admite en su evidencia, progress/history.md:748 y progress/current.md:493-495 lo dan por pendiente, y el juez advirtio que si sale rojo el veredicto decae. Deberia figurar como 'declarada_no_cerrada' o desglosarse: 6 y 7 cerradas, 5 abierta.",
          "Frontend-3 — el estado 'cerrada' es correcto (todos los mutantes del escuchador de Escape estan Killed, verificado), pero la cifra citada es erronea: dice 17 mutantes en el bloque 152-161 y son 16 los que caen integramente dentro; el 17 solo sale contando el BlockStatement del componente entero (57-460) que el mismo excluyo al contar 149-159.",
          "C7 — disposición de los supervivientes del frontend: marcada `cerrada_a_medias` sin que el contrato la pida en el frontend. Los tres requisitos del juez para frontend (judge_github_connector_cierre.md:285-297) no incluyen disposición mutante a mutante; el punto 8 que cita (:281-282) pertenece a «### Backend — PIT» y apunta a progress/mutation_github_connector.md. El C7 real (:228) dice sólo «Mutación pendiente», no exige disposición general. Además 43 supervivientes sí están dispuestos uno por uno en progress/verificacion_mutantes_github_connector.json. Debe ser `cerrada` / no aplica.",
          "Condiciones no relativas a la mutación (puntos 5, 6 y 7): marcada `cerrada` con queFalta vacío, pero el punto 5 (`bin/harness init` verde, condición de aprobación en :3-4 y :304-305) no está acreditado — la propia evidencia del auditor lo admite. Debe ser `cerrada_a_medias`, con el punto 5 pendiente."
        ]
      },
      {
        "feature": "28-calendario",
        "auditoria": {
          "feature": "28 — external_calendar (veredicto: progress/judge_external_calendar_cierre.md)",
          "condiciones": [
            {
              "id": "C1a — el anclaje está implementado en el calendario externo, no sólo en webhooks",
              "estado": "cerrada",
              "evidencia": "C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb/backend/src/main/java/com/apptolast/organization/adapter/feed/HttpCalendarFeed.java:110-133 resuelve el nombre UNA vez (`resolver.resolve(host)`), exige `allMatch(policy::allows)` y pasa `addresses.getFirst()` a `download`; :135-149 construye la petición contra `AnchoredConnection.literal(target, pinned)` con `.header(\"Host\", AnchoredConnection.authority(target, host))`; :169-178 crea el cliente con `HTTP_1_1`, `Redirect.NEVER` y `.sslParameters(AnchoredConnection.sniFor(host))`. No es prestado de webhooks: la clase compartida `adapter/net/AnchoredConnection.java:38-73` la usan los dos (JdkWebhookSender.java:213-221 y este). Producción cablea el constructor de 3 argumentos —confianza de plataforma— en adapter/config/ApplicationConfiguration.java:586-587, y OrganizationApplication.java:9-18 llama a `AnchoredConnection.allow()` como primera línea antes de Spring. Oráculo propio que muerde: HttpCalendarFeedTest.java:227-248 pide `http://nombre.que.no.resuelve.invalid:<puerto>/cal.ics?tok=WXYZ`, afirma `resolutions == 1`, `received.size()==1` y `Host: nombre.que.no.resuelve.invalid:<puerto>`; el mutante D (desanclar, `newBuilder(target)`) está acreditado rojo en progress/tdd_anclaje_dns.md:262-266.",
              "queFalta": ""
            },
            {
              "id": "C1b — prueba de TLS en las dos direcciones (válido aceptado por IP / no confiable rechazado)",
              "estado": "cerrada",
              "evidencia": "backend/src/test/java/com/apptolast/organization/adapter/feed/HttpCalendarFeedTest.java:455-511, tres pruebas contra un `HttpsServer` en 127.0.0.1: (1) `s12_b3_unCertificadoValidoParaElNombreSeAceptaAunqueSeConecteALaDireccion` exige `FeedFetch.Downloaded`, cuerpo con BEGIN:VCALENDAR y `Host: destino.anclado.invalid:<puerto>`; (2) `s12_b3_elMismoCertificadoSeRechazaSiElNombrePedidoEsOtro` exige FEED_UNREACHABLE y `received.isEmpty()`; (3) `s12_b3_unCertificadoQueNadieAvalaSeRechaza` con confianza de plataforma (`feedConfiando(null)`), FEED_UNREACHABLE y `received.isEmpty()`. He verificado el fixture yo mismo con openssl sobre backend/src/test/resources/tls/anchored-receiver.p12: `subject=CN=destino.anclado.invalid`, `X509v3 Subject Alternative Name: DNS:destino.anclado.invalid` — NINGUNA SAN de tipo iPAddress —, válido notBefore 2026-09-09 / notAfter 2126-08-16. Eso es lo que hace que la prueba (1) pueda fallar: sin `setServerNames` (AnchoredConnection.java:70) el certificado se verificaría contra 127.0.0.1 y no hay SAN de dirección que lo salve; el rojo está acreditado y medido en progress/tdd_anclaje_dns.md:255-261 (MUTANTE A: `expected: <FeedFetch.Downloaded> but was: <FeedFetch.Failed>`). La confianza inyectada (HttpCalendarFeed.java:96-107, constructor de paquete) añade un ancla, no apaga la verificación, y la tercera prueba lo demuestra. El contrato ganó su fila: features/external_calendar.feature:198 «presenta un certificado que no es válido para su nombre | FEED_UNREACHABLE» (ver C2). No he ejecutado la clase —hay una campaña PIT viva en backend/build/reports/pitest-external-calendar (mutations.xml de 0 bytes, 11:04)—; la bitácora declara HttpCalendarFeedTest 37/37 verde (progress/tdd_anclaje_dns.md:270-272).",
              "queFalta": ""
            },
            {
              "id": "C1c — los cuatro documentos dicen ahora lo mismo que hace el código",
              "estado": "cerrada_a_medias",
              "evidencia": "Alineados los cuatro que el ciclo 4 se propuso tocar: project-spec.md:2492 (enmienda B2/B3 reescrita: «la petición viaja después contra la dirección literal ya validada… el reenlace de nombres queda cerrado, no aceptado», decisión del propietario del 10-sep, cita `JdkWebhookSenderTest` y `HttpCalendarFeedTest`); deploy/EGRESS.md:6-48 (reescrito: la aplicación cierra la ventana, el egreso pasa a defensa en profundidad, y :37-46 nombra las cuatro pruebas de TLS, dos de ellas las del calendario); docs/external-calendar.md:40-64 (anclaje + «el TLS del camino anclado está probado», con el fixture y el porqué de la ausencia de SAN iPAddress); progress/current.md:17-20 (la decisión del propietario registrada). Javadocs coherentes: HttpCalendarFeed.java:30-51 y OutboundHostGuard.java:10-12. PERO queda un quinto texto normativo, de la propia feature 28, diciendo lo contrario: project-spec.md:2255, sección «Feature 28 → Seguridad y privacidad»: «La petición se emite después contra el nombre validado; el margen residual de rebinding DNS entre comprobación y conexión se acepta como límite explícito para una aplicación personal y se registra aquí, no se oculta». Es la línea gemela exacta de la de la feature 25 (project-spec.md:2050), que sí se corrigió —y por esta misma causa— en el commit f969d608 («la línea sobrevivió a la decisión del propietario de anclarlo: lo corrige el juez de cierre de la feature 25»). A la 28 no se le aplicó el mismo rasero: git log -L2255,2255 muestra que esa línea no se ha tocado desde f29b44cd, el commit original del contrato. Aviso adicional del mismo tipo: project-spec.md:2229 sigue describiendo la descarga como «timeout de conexión y de petición de 5 s», sin rastro del plazo del intercambio completo del ciclo 4 (features/external_calendar.feature:13-14 y docs/external-calendar.md:66-71 sí lo dicen).",
              "queFalta": "Corregir project-spec.md:2255 igual que se corrigió el 2050 de la feature 25: la petición viaja a la dirección ya validada (`AnchoredConnection`), el reenlace queda cerrado y no se acepta como margen residual. Mientras esa línea siga en pie, `main` conserva un texto normativo de la 28 que afirma un estado distinto del que el código tiene, que es literalmente el defecto por el que el juez abrió C1. Conviene además llevar al cuerpo de la spec (2229) el plazo del intercambio completo."
            },
            {
              "id": "C1d — guarda: sin la propiedad restringida se degrada a FEED_UNREACHABLE en vez de conectar sin Host",
              "estado": "cerrada_a_medias",
              "evidencia": "El camino de degradación existe en producción: HttpCalendarFeed.java:145-149 captura `IllegalArgumentException` al construir la petición y devuelve FEED_UNREACHABLE con el comentario «antes de conectar sin ella —y por tanto contra el servidor equivocado— se prefiere no conectar». Pero ninguna prueba lo ejerce: no hay ni una ocurrencia de `restricted` ni de `clearProperty` en HttpCalendarFeedTest.java, así que ese `catch` no tiene oráculo (candidato claro a superviviente en la campaña C3). Lo que sí se añadió es la guarda inversa, y es más fuerte de lo pedido para el agravante 2 del §4.2 del veredicto: AnchoredConnectionTest.java:107-115 `thisJvmAcceptsTheHostHeaderTheAnchoringDependsOn` afirma la CAPACIDAD del JVM («este JVM deja poner la cabecera»), no que la propiedad esté puesta; y la autorización deja de depender del orden de carga porque se declara en OrganizationApplication.java:17 (primera línea del main), en backend/build.gradle.kts:37 (`tasks.test`) y en :724 (jvmArgs de pitest). El defecto latente que esto cura está medido en progress/tdd_anclaje_dns.md:316-345 (20 pruebas caídas en `main` al correr HttpCalendarFeedTest junto a HttpGithubIssueSourceTest).",
              "queFalta": "La guarda literal que pedía el juez —afirmar que sin la propiedad se degrada a FEED_UNREACHABLE y no se conecta sin `Host`— no existe; el `catch` de HttpCalendarFeed.java:145-149 queda sin prueba. No lo considero bloqueante por sí solo (el efecto de ámbito JVM, que era el riesgo real, sí está cerrado), pero el recuento no debe darlo por hecho y la campaña PIT debería vigilar ese bloque."
            },
            {
              "id": "C2 — ratificación humana de la ampliación de contrato del ciclo 4",
              "estado": "declarada_no_cerrada",
              "evidencia": "No hay rastro de ratificación en ninguna parte: `grep -rn ratific` sobre todo el repositorio devuelve ratificaciones explícitas de OTRAS features (p. ej. progress/judge_github_connector_final.md:247 «con la enmienda ratificada por el propietario», progress/judge_additional_connectors.md:305-307 «marcada ratificada por el propietario») y, para la 28, únicamente las tres menciones del propio veredicto que la exigen (progress/judge_external_calendar_cierre.md:5, :177, :294). progress/current.md:17-20 sólo registra la decisión del propietario sobre el ANCLAJE (C1), no sobre el contrato. feature_list.json:321-322 sigue en `in_progress`, que es lo correcto. Y hay algo peor que «sigue pendiente»: el propio arreglo de C1 AMPLIÓ el contrato otra vez. `git show 78dca3a6 -- features/external_calendar.feature` añade la fila «| presenta un certificado que no es válido para su nombre | FEED_UNREACHABLE |» en los ejemplos de @s12 (hoy features/external_calendar.feature:198), razonada en progress/tdd_anclaje_dns.md:274-283. O sea que la puerta humana ya no cubre dos líneas y una fila (:13-14 y la fila del cuerpo que gotea, :200), sino además esta tercera edición, hecha después del veredicto.",
              "queFalta": "La ratificación explícita del propietario sobre TODO el delta del contrato: features/external_calendar.feature:13-14 (plazo de 5 s del intercambio completo), la fila de @s12 del cuerpo que gotea (:200) y —nueva, posterior al dictamen— la fila del certificado no válido para su nombre (:198). O eso, o la reversión completa. Mientras no exista, C2 sigue abierta y su alcance ha crecido durante el cierre de C1."
            }
          ],
          "bloqueaElCierre": true
        },
        "objeciones": [],
        "malJuzgadas": [
          "C2 — ratificación humana de la ampliación de contrato del ciclo 4: el estado 'declarada_no_cerrada' es correcto en su núcleo, pero el alcance que la auditoría le atribuye es injusto. Cuenta como tercera edición no cubierta la fila de @s12 de features/external_calendar.feature:198 ('presenta un certificado que no es válido para su nombre | FEED_UNREACHABLE'), cuando esa fila es la expresión contractual de una decisión del propietario ya registrada: project-spec.md:2492, dentro de la sección de enmiendas encabezada en :2488 ('El coordinador aprueba las siguientes enmiendas dentro de la autorización global'), dice literalmente «Decisión del propietario del 10 de septiembre de 2026 … Se ancla en las dos, y se prueba el TLS … hay oráculo en las dos features de que un certificado válido para el nombre se acepta … y de que se rechaza si el nombre no coincide o nadie lo avala»; y progress/current.md:20 registra «el anclaje del reenlace DNS en las dos features con su prueba de TLS». La propia C1(a) del veredicto exigía esa prueba y mandaba escribir lo elegido justo en esos dos sitios. Cronología verificada: 78dca3a6 (09-10 01:57) añade la fila y b80acdb3 (09-10 02:00) escribe la decisión. Lo que sigue realmente sin ratificar es sólo el delta del ciclo 4 (features/external_calendar.feature:13-14 y la fila del cuerpo que gotea, :200)."
        ]
      },
      {
        "feature": "29-conectores",
        "auditoria": {
          "feature": "29 — additional_connectors",
          "condiciones": [
            {
              "id": "C1",
              "estado": "cerrada",
              "evidencia": "frontend/src/connectors-catalog-routing.test.tsx existe (117 lineas, 5 pruebas) y lo he EJECUTADO: 5 passed / 5. El oraculo no es satisfacible por el chrome: workspace.tsx no tiene ningun h1 (solo <strong>{section}</strong> en workspace.tsx:174), y los h1 que la prueba busca solo existen en connectors-catalog.tsx:123-125 («Conectores») y gitlab-connector.tsx:304-306 («Conector de GitLab»). Las pruebas 3, 4 y 5 muerden la otra mitad del cableado: aria-current sale de section, y section sale de App.tsx:59 leyendo App.tsx:50-51 (connectorsCatalog / gitlabConnector). Borrado mental del bloque de render (App.tsx:90-93): caen las pruebas 1, 2 y 5 y quedan verdes las 3 y 4 —exactamente los dos rojos que el commit 709f0b73 acredita—; borrada la entrada de menu (workspace.tsx:115-123): caen 3, 4 y 5. La prueba compara la posicion por nombre relativo contra la lista entera de la nav, no por indice, que es la leccion del hallazgo de rangos. prettier --check limpio sobre el fichero.",
              "queFalta": ""
            },
            {
              "id": "C2",
              "estado": "declarada_no_cerrada",
              "evidencia": "El orquestador no la declara cerrada, y en efecto no lo esta. `ls e2e/` no tiene ningun spec de gitlab, conectores ni additional_connectors (si hay github-connector.spec.mjs y github-connector-native-zoom.spec.mjs). El unico grep de «conectores» en e2e/ da e2e/github-connector.spec.mjs:132 y e2e/support/connector.mjs:87, los dos sobre «clave de conectores» (APP_CONNECTOR_KEY), no sobre las pantallas. Cero evidencia de 320/768/1280, texto al 200 %, zoom nativo, 44x44, recorrido de teclado con foco visible y axe, y sigue sin existir el fichero donde registrar la nota de limites humanos que pide la ultima linea del @s38.",
              "queFalta": "El spec de Playwright entero para /conectores y /conectores/gitlab, tal cual lo enumera la condicion C2 del veredicto (judge_additional_connectors.md:493-496). No esta empezado."
            },
            {
              "id": "C3",
              "estado": "cerrada",
              "evidencia": "Mecanismo real y compartido: backend/.../adapter/connectors/BoundedResponse.java, LIMIT = 5*1024*1024 en :36, corte en :85 (`buffer.size() > LIMIT`), lectura en trozos dentro de try-with-resources (cerrar el cuerpo es lo que aborta la descarga) y plazo por dos vias —comprobacion por trozo en :83/:87/:90 y una guillotine programada en :73 que cierra el cuerpo del proveedor mudo—. Los dos origenes pasan de ofString() a ofInputStream(): HttpGitlabIssueSource.java:132-138 y HttpGithubIssueSource.java:106-111, con el instante limite calculado ANTES de conectar. Pruebas que pueden fallar: HttpGitlabIssueSourceTest s25_abodyOverFiveMebibytesIsUnavailable..., s25_abodyOfExactlyFiveMebibytesStillImports (frontera que mata el mutante > -> >=) y s25_aproviderThatOpensTheBodyAndGoesSilentIsCutByTheReadDeadline, esta con @Timeout(20s, SEPARATE_THREAD) y un FakeIssueServer.holdBody() nuevo que manda cabeceras y no emite ni cierra: sin plazo la llamada no termina nunca. En GitHub, las dos de tamano. El REQUEST_TIMEOUT es 4 s, asi que el @Timeout de 20 s discrimina.",
              "queFalta": "Dos residuos menores, ninguno bloqueante por si solo: (1) el plazo de lectura solo tiene oraculo en el lado GitLab; HttpGithubIssueSourceTest recibio las dos de tamano pero no la del proveedor mudo (el mecanismo es compartido, asi que es aceptable, pero la condicion decia «aplica igual a HttpGithubIssueSource»). (2) BoundedResponse NO esta en additionalConnectorsClasses (backend/build.gradle.kts:84-110): la logica nueva —la frontera >, expired(), la guillotine— recibe cero mutantes en la campana de la 29, y solo los recibe por el comodin adapter.connectors.* de githubConnectorClasses (:130). Es la misma clase de hueco que denuncia C5, abierta por el arreglo de C3."
            },
            {
              "id": "C4",
              "estado": "cerrada",
              "evidencia": "La guarda que faltaba existe: frontend/src/github-connector-client.test.ts:609 y :615 leen GithubConnectorController.java y comparan ImportResponse contra RECEIPT_FIELDS y ConnectionResponse contra CONNECTION_FIELDS. Contrastado a mano contra la fuente: GithubConnectorController.java:302 ConnectionResponse(repository, login, status, connectedAt, lastImport) = CONNECTION_FIELDS de github-connector-client.ts:14; :318 ImportResponse con los doce componentes = RECEIPT_FIELDS de :11. Las constantes pasan a `export` para comparar contra la misma que usa exact() en produccion, no contra una copia. F2 cerrado: gitlab-connector-client.test.ts:323 anade ErrorResponse contra ERROR_FIELDS («code at») y GitlabConnectorController.java:298 es `record ErrorResponse(String code, Instant at)`. La afirmacion «GitHub no tiene ErrorResponse» la he verificado: grep 'record ' en GithubConnectorController.java devuelve exactamente dos, :302 y :318. F3 cerrado: sorted() en los dos ficheros, comparacion por conjuntos. Puede fallar: renombrar cualquier componente rompe la igualdad, y controller() lanza si no encuentra el fichero en vez de saltarse en silencio.",
              "queFalta": ""
            },
            {
              "id": "C5",
              "estado": "cerrada_a_medias",
              "evidencia": "Dos de los tres patrones estan donde el juez los pidio y el tercero no. (a) El patron muerto se arreglo: backend/build.gradle.kts:120 dice ahora application.ImportIssues* en vez de ImportGithubIssues*, y ImportIssues.java existe. (b) domain.GitlabProjectPath* se anadio al ambito de la 29 en :91, y GitlabProjectPath.java existe. (c) application.ImportGuard* se anadio en :121, pero DENTRO de githubConnectorClasses, no de additionalConnectorsClasses. targetTests es com.apptolast.organization.* para los dos ambitos (:668-669), asi que la cobertura de tests no es el problema; el problema es targetClasses (:636 additionalConnectorsOnly -> additionalConnectorsClasses). He barrido yo los 23 patrones del ambito de la 29 contra src/main: todos resuelven, cero muertos.",
              "queFalta": "ImportGuard* esta en el ambito equivocado. La condicion dice literalmente «anadir domain.GitlabProjectPath* y application.ImportGuard* al ambito de la 29» (judge:508) y la tabla de umbrales del juez mete ImportGuard en la capa de dominio y aplicacion al >= 0,90 de la campana de la 29 (judge:404), porque @s27 (cinco filas) y @s28 (frontera 14/16 minutos) de ESTA feature descansan enteros ahi. Con scope=additional_connectors, ImportGuard sigue recibiendo cero mutantes; solo los recibe en github_connector o en la campana conjunta night_five (:631), mezclado con otras cifras. Es defendible como clase compartida —el mismo argumento que ImportIssues—, pero no es lo que la condicion pedia y el numero de la 29 no podra acreditarlo. Ademas, el commit 72910290 dice «queda una herramienta que barre los DOS arboles... 439 patrones, cero muertos» y solo toco build.gradle.kts: no hay herramienta commiteada, asi que esa cifra no es verificable y no queda ninguna guarda permanente contra el proximo patron muerto. Y falta BoundedResponse en el ambito (ver C3)."
            },
            {
              "id": "C6",
              "estado": "cerrada",
              "evidencia": "H1 cerrado y el razonamiento del carril es CORRECTO, no es quitar una prueba incomoda; lo he trazado en produccion. ConnectorCatalogApiTest.java:152 construye un GitlabConnectionView real con los cinco campos prohibidos (API_BASE, PROJECT_PATH='grupo/proyecto', PROJECT_ID=4821, TOKEN_HINT='WXYZ', version 2), afirma PRIMERO que estan en la entrada (:164-166), deriva la fila con el GitlabStatusSource de verdad (:168, `new GitlabStatusSource(owner -> view).read(\"owner\")`; ReadGitlabConnectionUseCase es interfaz de un solo metodo, compila) y afirma seis ausencias sobre el cuerpo entero mas que lastError.code sigue siendo el codigo estable. Cubre derivacion + serializacion, y una fuga por cualquiera de los cinco campos descartados (GitlabStatusSource.java:31 se queda con tres de ocho) la tumba. Sobre las dos aserciones retiradas: el token entero no tiene hueco en GitlabConnectionView (ocho campos, solo tokenHint), y el texto libre del proveedor no puede llegar a ConnectorError.code —el unico camino es GitlabConnection.java:58 <- withError/withFailure <- GitlabIssueConnections.java:37,43 <- ImportIssues.java:126 ConnectorFailures.importErrorCode(), y ConnectorFailures.java solo devuelve literales de un conjunto cerrado (CONNECTION_INVALID, RATE_LIMITED, GITLAB_UNAVAILABLE, GITHUB_*)—. Es decir: 'glpat' e 'invalid_token' eran inalcanzables AHI y siguen afirmadas donde muerden, HttpGitlabIssueSourceTest:91-105, con el fixture real {\"message\":\"invalid_token: glpat-abcdef1234\"} parametrizado sobre cinco estados. Merece decirse que la receta literal del juez (meter el texto del proveedor como ConnectorError.code y afirmar que no sale) habria sido IRREALIZABLE en verde: GitlabConnectorController.java:298 publica lastError.code tal cual; el carril eligio otra via valida y mas fuerte. H2 cerrado: ConnectorCatalogApiTest.java:256 afirma ahora status 401 Y jsonPath $.code = UNAUTHENTICATED; el literal vive en SecurityConfiguration.java:123, asi que el mutante UNAUTHENTICATED->UNAUTHORIZED la mata.",
              "queFalta": "Residuo cosmetico: la mitad de «esta en la entrada» solo se afirma para TOKEN_HINT, PROJECT_PATH y API_BASE (:164-166). Para PROJECT_ID (4821) y para la version se afirma la ausencia sin afirmar la presencia, asi que esas dos aserciones vuelven a ser decorativas el dia que alguien vacie ese campo del fixture —justo el fallo que C6 venia a corregir—. No bloquea."
            },
            {
              "id": "C7",
              "estado": "declarada_no_cerrada",
              "evidencia": "features/additional_connectors.feature:388-395 lleva el comentario de la enmienda («Enmienda del 10 de septiembre de 2026...») y cita la de 27, pero NO lleva contrafirma. El precedente si la lleva: features/github_connector.feature:393 dice «Enmienda del 9 de septiembre de 2026, ratificada por el propietario». grep de «ratificad|contrafirm» sobre additional_connectors.feature no devuelve nada. Las cuatro filas enmendadas (Bearer valida -> 403 API_SCOPE_DENIED) estan en su sitio en :397-400.",
              "queFalta": "La contrafirma literal del propietario en features/additional_connectors.feature:388, como la de la 27. Es formalidad, pero es bloqueante segun el propio veredicto y el carril no la ha tocado."
            }
          ],
          "bloqueaElCierre": true
        },
        "objeciones": [],
        "malJuzgadas": [
          "C5 — el VEREDICTO (cerrada_a_medias) es correcto, pero dos de los tres reproches del «queFalta» estan fuera del contrato y uno de ellos es exigencia inventada: (a) «no hay herramienta commiteada ... no queda ninguna guarda permanente contra el proximo patron muerto» — C5 (judge_additional_connectors.md:508) pide TRES patrones nombrados (GitlabProjectPath*, ImportGuard*, arreglar el muerto ImportGithubIssues*), no una herramienta de barrido commiteada ni una guarda permanente; ademas la propia auditoria dice haber barrido a mano los 23 patrones del ambito de la 29 con cero muertos, o sea que la sustancia esta verificada y lo unico que falta es un artefacto que nadie pidio. (b) «falta BoundedResponse en el ambito» — BoundedResponse ni existia cuando se escribio C5 y no es ninguno de sus tres patrones; y no es una clase de cero mutantes: el comodin com.apptolast.organization.adapter.connectors.* de githubConnectorClasses (build.gradle.kts:130) la cubre en la campana de github_connector y en night_five, cosa que la propia auditoria admite. Cargarla bajo C5 infla el residuo. En cambio el reproche que SI decide (ImportGuard* en githubConnectorClasses:121 en vez de additionalConnectorsClasses) esta infra-argumentado: la auditoria lo llama «defendible como clase compartida, el mismo argumento que ImportIssues», y no lo es — grep ImportGuard sobre backend/src/main da exactamente tres hits (la clase, ConnectGitlab.java:46 y DisconnectGitlab.java:28), ambos llamantes de la feature 29; ImportIssues nunca la llama. La colocacion es inequivocamente contraria a la condicion Y al comentario del propio ambito («No incluye las clases compartidas con la 27»).",
          "C3 — VEREDICTO correcto (cerrada), pero el residuo (1) exige mas de lo que el contrato pide: C3 pide «techo de tamano ... con su prueba» y anade «Aplica igual a HttpGithubIssueSource». Las dos pruebas de tamano existen en los dos origenes (HttpGitlabIssueSourceTest:255/:264 y HttpGithubIssueSourceTest:353/:361). El plazo de lectura y la prueba del proveedor mudo (HttpGitlabIssueSourceTest:282, con FakeIssueServer.holdBody()) son un extra que el artesano puso de mas; reclamar su gemela en GitHub es exigir sobre un mecanismo que la condicion nunca menciono. La auditoria lo reconoce a medias («es aceptable») pero lo cuenta igual como residuo.",
          "C7 — VEREDICTO correcto (declarada_no_cerrada: la contrafirma del propietario NO esta en features/additional_connectors.feature:388, y el precedente de github_connector.feature:393 si la lleva), pero la cita de la evidencia es erronea: dice «las cuatro filas enmendadas (Bearer valida -> 403 API_SCOPE_DENIED) estan en su sitio en :397-400». Son DOS filas, en :400 y :401; :397 es la cabecera de la tabla y :398-399 son las dos filas de «ninguna sesion» que siguen en 401 UNAUTHENTICATED y no se enmendaron. El propio comentario de la enmienda (:388) dice «Las dos filas de credencial Bearer». No altera el veredicto, pero es el tipo de cita descuidada que la auditoria le reprocha al orquestador."
        ]
      }
    ],
    "checklist": "# Lo que falta para `done`, feature por feature\n\nVerificado a mano lo decisivo (grep sobre `mutations.xml`, `build.gradle.kts`, `project-spec.md`, `.feature`). Donde la auditoría y la refutación discrepan, digo cuál sostiene el disco.\n\n---\n\n## Feature 25 — webhooks\n\n**1. Condiciones no cerradas**\n\n- **Cond. 4 (field-sizing) — abierta.** `grep field-sizing project-spec.md` no devuelve nada. Falta añadir a `### Límites explícitos` de la 25 (`project-spec.md:2050`) que `field-sizing: content` solo lo implementa Chromium (`frontend/src/webhooks.scss:143-149`) y que en Firefox/Safari el secreto se recorta al 200 % de texto. **Minutos.**\n- **Cond. 1 — cerrada a medias, y no la arregla relanzar.** `AnchoredConnection` sí está en el ámbito y ya recibe 21 mutantes: eso está cerrado. Pero en ese mismo informe `grep -c Slf4jWebhookAudit backend/build/reports/pitest-webhooks/mutations.xml` = **0**, y `Slf4jWebhookAudit` está nombrada en la lista dura de `progress/judge_webhooks_cierre.md:271` (\"cero mutantes generados… la puerta no vale aunque la puntuación salga alta\"). Confirmado el porqué: sus tres métodos son solo llamadas `org.slf4j` y el bloque pitest (`backend/build.gradle.kts:715-745`) no fija `mutators` ni `avoidCallsTo`, así que el `avoidCallsTo` por defecto de PIT los suprime. **Dos salidas, no tres: o acreditación explícita del juez (como los seis literales SQL de su §7), o tocar `mutators`/`avoidCallsTo` de ese ámbito y volver a medir.**\n- **Cond. 1, segundo flanco.** Los 3 supervivientes de `AnchoredConnection` (`ConditionalsBoundaryMutator` en `literal:48`, `authority:57`, `isAddressLiteral:76`) no están en la lista de supervivientes que el §7 acepta. Matarlos o acreditarlos por escrito. **Una hora.**\n- **Cond. 3 (latencyMs) — dala por CERRADA con nota.** El juez ofreció dos puertas y se tomó la 2ª: el `ticker` existe, se usa en los dos extremos y `JdkWebhookSenderTest:524-534` cae si vuelve el reloj ambiente. La auditoría la puntúa a medias exigiendo ratificación del juez, bitácora del rojo y frescura del XML: lo primero y lo segundo no están en el contrato, y lo tercero es el punto 1 (campañas). Único deber real: **cosmético**, el javadoc huérfano de `JdkWebhookSenderTest.java:92`.\n- **Punto 6 — `bin/harness init` verde.** Sin él \"ninguna aprobación de este dictamen es firme\" (`judge:349-351`).\n\n**2. Campañas** — Backend medido 89,88 %, pero ese informe (10:53) es **anterior** a la prueba de `latencyMs` y arrastra el cero de `Slf4jWebhookAudit`: hay que rehacerlo tras resolver el punto anterior. **Frontend de la 25: sin medir.** El juez pide las dos y los dos `progress/mutation_webhooks_*.md`.\n\n**3. Decisión del propietario** — `features/webhooks.feature:320` y `project-spec.md:2038` siguen diciendo \"medido con el **reloj** inyectado\" y lo que hay es un cronómetro monótono aparte del `Clock`. **Pregunta, no tarea:** ¿se ratifica la lectura tal cual, o se enmienda la línea a \"cronómetro monótono inyectado\" por la puerta humana? El orquestador no puede decidirlo.\n\n**4. Esfuerzo** — **Varias horas** (frontend + rehacer backend). Solo docs: minutos.\n\n---\n\n## Feature 27 — conector GitHub\n\n**1. Condiciones no cerradas**\n\n- **Punto 5 — `bin/harness init` verde: declarado, no cerrado.** Está en la línea de aprobación del veredicto (`:3-4`, `:304-305`) y remata \"si sale rojo, este veredicto decae\". `progress/history.md:748` y `progress/current.md:493-495` lo dan por pendiente. La auditoría lo archivó bajo una entrada `cerrada`; no lo está. Y tiene precedente de ponerse rojo (`current.md:94`, lint de Prettier en ocho `stryker.*.config.json`).\n- **\"Ambas-4\" — el informe debe decir contra qué commit se midió.** `progress/mutacion_github_connector_frontend_cierre.md:3` dice solo \"sobre `main`\". Escribir el padre de `44011086` y anotar la deriva de `src/github-connector-client.ts` y su test durante la corrida (2 `export`, +66 líneas de test, 0 borradas) y por qué no mueve el 85,06 %. **Minutos, dos frases.**\n- **NO gastes tiempo en C7 frontend.** La sección \"### Frontend — Stryker\" (`judge:285-297`) no pide disposición mutante a mutante; el punto 8 que se citó (`:281-282`) es de \"### Backend — PIT\". Además 43 supervivientes ya están dispuestos uno a uno en `progress/verificacion_mutantes_github_connector.json`.\n\n**2. Campañas** — Frontend cerrado (85,06 %, recuento verificado). **Backend PIT de `github_connector`: no existe** (`backend/build/reports/pitest-github-connector` vacío, `progress/mutation_github_connector.md` no existe). Lleva las ocho exigencias duras de `judge:243-282`: ≥80, total declarado, desglose por clase, `AesGcmSecretCipher` con ≥12 mutantes y cero `NO_COVERAGE`, los mutantes de `:51`, `:52-53`, `:60`, `:61-62` y `:25` KILLED **uno por uno** (\"un superviviente en cualquiera de estos puntos es rechazo\"), `ConnectorKeyRing` ≥1, ninguna clase a cero salvo los cuatro portadores de datos.\n\n**3. Decisión del propietario** — ninguna.\n\n**4. Esfuerzo** — **Varias horas** (la campaña de backend y lo que destapen esos cinco puntos). Lo documental: minutos.\n\n---\n\n## Feature 28 — external_calendar\n\n**1. Condiciones no cerradas**\n\n- **C1c — abierta.** `project-spec.md:2255` (Feature 28 → Seguridad y privacidad) sigue diciendo, verificado literalmente: \"el margen residual de rebinding DNS entre comprobación y conexión **se acepta** como límite explícito\". Es la línea gemela del `:2050` de la 25, que sí se corrigió. Corregirla con el mismo rasero. De paso, `:2229` sigue hablando solo de \"timeout de conexión y de petición de 5 s\", sin el plazo del intercambio completo. **Minutos.**\n- **C1d — cerrada a medias.** La guarda literal que pedía el juez no existe: ni `restricted` ni `clearProperty` aparecen en `HttpCalendarFeedTest.java`, así que el `catch` de `HttpCalendarFeed.java:145-149` no tiene oráculo (candidato claro a superviviente en la campaña). O una prueba que afirme la degradación a `FEED_UNREACHABLE`, o que el juez acepte la guarda de ámbito JVM (`AnchoredConnectionTest:107-115`) como sustituta. **Una hora.**\n- **C2 — declarada, no cerrada.** Sin ratificación en ninguna parte.\n\n**2. Campañas** — Backend en vuelo (`pitest-external-calendar/mutations.xml` de 0 bytes a las 11:04): falta que termine por encima del umbral y su informe. **Frontend de la 28: sin medir.**\n\n**3. Decisión del propietario** — **C2, y es la que bloquea.** Alcance real a ratificar: `features/external_calendar.feature:13-14` (plazo de 5 s del intercambio completo) y la fila del cuerpo que gotea (`:200`). **No pidas la fila `:198`** (certificado no válido para su nombre): ya la cubre la decisión registrada en `project-spec.md:2492` y `progress/current.md:20`, y la cronología lo confirma (78dca3a6 a las 01:57, b80acdb3 a las 02:00). La auditoría infla ahí el alcance.\n\n**4. Esfuerzo** — **Varias horas** (las dos campañas); C1c minutos, C1d una hora.\n\n---\n\n## Feature 29 — additional_connectors\n\n**1. Condiciones no cerradas**\n\n- **C2 — no empezada, y es el grueso.** `ls e2e/` no tiene ningún spec de gitlab ni de conectores. Falta el spec de Playwright entero para `/conectores` y `/conectores/gitlab` tal como lo enumera `judge_additional_connectors.md:493-496`: 320/768/1280, texto al 200 %, zoom nativo, 44×44, recorrido de teclado con foco visible, axe, y el fichero donde registrar la nota de límites humanos del @s38.\n- **C5 — cerrada a medias, y el reproche que decide es real.** Verificado: `ImportGuard*` está en `build.gradle.kts:121`, **dentro de `githubConnectorClasses`**, no de `additionalConnectorsClasses` (`:84-110`). `grep ImportGuard backend/src/main` da la clase más `ConnectGitlab.java:46` y `DisconnectGitlab.java:28` — los dos de la 29; `ImportIssues` nunca la llama, así que el argumento de \"clase compartida\" no se sostiene y el propio comentario del ámbito (`:81-83`) dice lo contrario. Moverla, **y resincronizar en el mismo commit las guardas del arnés que fijan esas listas** (`scripts/project.test.mjs`, `deepEqual` de la lista completa) — es exactamente la trampa que ya cayó esta madrugada. Después hay que remedir la 29. **Minutos de edición.**\n  - **No lo cuentes como deuda de C5:** la \"herramienta de barrido commiteada\" (nadie la pidió; los 23 patrones ya se barrieron a mano, cero muertos) ni `BoundedResponse` en el ámbito (no existía cuando se escribió C5 y la cubre el comodín `adapter.connectors.*` de `githubConnectorClasses`). Si quieres medirla en la 29, es decisión aparte, no condición.\n- **C7 — declarada, no cerrada.** Verificado: `features/additional_connectors.feature:388` lleva el comentario de la enmienda pero **no la contrafirma**; el precedente (`features/github_connector.feature:393`) sí la lleva. Corrección de cita para la bitácora: las filas enmendadas son **dos, `:400` y `:401`**, no cuatro en `:397-400` (`:397` es la cabecera y `:398-399` siguen en 401 UNAUTHENTICATED).\n- **NO gastes tiempo en el residuo (1) de C3.** C3 pide techo de tamaño con su prueba, y las dos de tamaño existen en los dos orígenes (`HttpGithubIssueSourceTest:353/:361`). La prueba del proveedor mudo es un extra del artesano; reclamar su gemela en GitHub es exigir sobre un mecanismo que la condición no menciona.\n\n**2. Campañas** — **Backend 29 y frontend 29: sin medir.** El backend, además, solo después de mover `ImportGuard`; si no, el número de la 29 no acredita `@s27` ni `@s28`.\n\n**3. Decisión del propietario** — **La contrafirma de C7** en `features/additional_connectors.feature:388`. Es formalidad, pero el veredicto la hace bloqueante y no la puede poner el orquestador.\n\n**4. Esfuerzo** — **Varias horas**, dominadas por el spec E2E. C5 y C7 son minutos cada una.\n\n---\n\n**Ninguna: aunque las seis campañas pendientes salieran todas por encima del umbral, hoy no se podría marcar `done` ni una sola —la 30 sigue con el frontend en 52,74 %, y la 25, la 27, la 28 y la 29 arrastran cada una al menos un deber que ninguna campaña cierra (el cero de `Slf4jWebhookAudit`, `bin/harness init` verde, la ratificación del propietario de la 28 y la contrafirma más el spec E2E de la 29).**"
  },
  "workflowProgress": [
    {
      "type": "workflow_phase",
      "index": 1,
      "title": "Auditoria"
    },
    {
      "type": "workflow_phase",
      "index": 2,
      "title": "Refutacion"
    },
    {
      "type": "workflow_phase",
      "index": 3,
      "title": "Checklist"
    },
    {
      "type": "workflow_agent",
      "index": 1,
      "label": "audita:25-webhooks",
      "phaseIndex": 1,
      "phaseTitle": "Auditoria",
      "agentId": "a431c46b0d5afc81a",
      "model": "claude-opus-5[1m]",
      "state": "done",
      "startedAt": 1789031435891,
      "queuedAt": 1789031435859,
      "attempt": 1,
      "lastToolName": "StructuredOutput",
      "lastToolSummary": "25 webhooks",
      "promptPreview": "Trabajas en C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb, rama main.\n\nERES DE SOLO LECTURA. No edites nada. NO ejecutes campanas de mutacion, ni la\nsuite completa, ni bin/harness init: hay dos campanas corriendo y 39 contenedores\nvivos. Puedes leer todo y ejecutar como mucho UNA clase de test o UN fichero de\nvitest si es imprescindible para distinguir dos hipotesis.\n\nQUE SE JUEGA A…",
      "promptFramed": false,
      "lastProgressAt": 1789031968924,
      "tokens": 148102,
      "toolCalls": 41,
      "durationMs": 533026,
      "resultPreview": "{\"feature\":\"25 webhooks\",\"condiciones\":[{\"id\":\"1. AnchoredConnection en el ambito PIT (estaba en ninguno: cero mutantes)\",\"estado\":\"cerrada\",\"evidencia\":\"Leido en backend/build.gradle.kts: la clase esta ahora en webhooksClasses (linea 628, con comentario en :624-627) y tambien en externalCalendarClasses (:454). El patron es exacto: com.apptolast.organization.adapter.net.AnchoredConnection*, y exis…"
    },
    {
      "type": "workflow_agent",
      "index": 2,
      "label": "audita:27-github",
      "phaseIndex": 1,
      "phaseTitle": "Auditoria",
      "agentId": "aa5033df6fb2049ba",
      "model": "claude-opus-5[1m]",
      "state": "done",
      "startedAt": 1789031435894,
      "queuedAt": 1789031435859,
      "attempt": 1,
      "lastToolName": "StructuredOutput",
      "lastToolSummary": "[]",
      "promptPreview": "Trabajas en C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb, rama main.\n\nERES DE SOLO LECTURA. No edites nada. NO ejecutes campanas de mutacion, ni la\nsuite completa, ni bin/harness init: hay dos campanas corriendo y 39 contenedores\nvivos. Puedes leer todo y ejecutar como mucho UNA clase de test o UN fichero de\nvitest si es imprescindible para distinguir dos hipotesis.\n\nQUE SE JUEGA A…",
      "promptFramed": false,
      "lastProgressAt": 1789031778999,
      "tokens": 87218,
      "toolCalls": 22,
      "durationMs": 341924,
      "resultPreview": "{\"findings\":\"[]\",\"feature\":\"27 — conector GitHub (frontend, campaña de mutación de cierre)\",\"condiciones\":[{\"id\":\"Frontend-1 — puntuación mínima 80\",\"estado\":\"cerrada\",\"evidencia\":\"`frontend/reports/mutation-github-connector/mutation.json` recontado entero: 3 ficheros, 589 mutantes, estados SOLO `Killed` (501) y `Survived` (88). Cero `NoCoverage`, cero `Timeout`, cero `CompileError`, cero `Ignored…"
    },
    {
      "type": "workflow_agent",
      "index": 3,
      "label": "audita:28-calendario",
      "phaseIndex": 1,
      "phaseTitle": "Auditoria",
      "agentId": "a3b1c59229accaa4f",
      "model": "claude-opus-5[1m]",
      "state": "done",
      "startedAt": 1789031435896,
      "queuedAt": 1789031435861,
      "attempt": 1,
      "lastToolName": "StructuredOutput",
      "lastToolSummary": "28 — external_calendar (veredicto: progress/judge_external_…",
      "promptPreview": "Trabajas en C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb, rama main.\n\nERES DE SOLO LECTURA. No edites nada. NO ejecutes campanas de mutacion, ni la\nsuite completa, ni bin/harness init: hay dos campanas corriendo y 39 contenedores\nvivos. Puedes leer todo y ejecutar como mucho UNA clase de test o UN fichero de\nvitest si es imprescindible para distinguir dos hipotesis.\n\nQUE SE JUEGA A…",
      "promptFramed": false,
      "lastProgressAt": 1789031832649,
      "tokens": 138786,
      "toolCalls": 31,
      "durationMs": 395573,
      "resultPreview": "{\"feature\":\"28 — external_calendar (veredicto: progress/judge_external_calendar_cierre.md)\",\"condiciones\":[{\"id\":\"C1a — el anclaje está implementado en el calendario externo, no sólo en webhooks\",\"estado\":\"cerrada\",\"evidencia\":\"C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb/backend/src/main/java/com/apptolast/organization/adapter/feed/HttpCalendarFeed.java:110-133 resuelve el nombre …"
    },
    {
      "type": "workflow_agent",
      "index": 4,
      "label": "audita:29-conectores",
      "phaseIndex": 1,
      "phaseTitle": "Auditoria",
      "agentId": "a710bd43af7e0592b",
      "model": "claude-opus-5[1m]",
      "state": "done",
      "startedAt": 1789031435897,
      "queuedAt": 1789031435861,
      "attempt": 1,
      "lastToolName": "StructuredOutput",
      "lastToolSummary": "29 — additional_connectors",
      "promptPreview": "Trabajas en C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb, rama main.\n\nERES DE SOLO LECTURA. No edites nada. NO ejecutes campanas de mutacion, ni la\nsuite completa, ni bin/harness init: hay dos campanas corriendo y 39 contenedores\nvivos. Puedes leer todo y ejecutar como mucho UNA clase de test o UN fichero de\nvitest si es imprescindible para distinguir dos hipotesis.\n\nQUE SE JUEGA A…",
      "promptFramed": false,
      "lastProgressAt": 1789031863095,
      "tokens": 139440,
      "toolCalls": 48,
      "durationMs": 426018,
      "resultPreview": "{\"feature\":\"29 — additional_connectors\",\"condiciones\":[{\"id\":\"C1\",\"estado\":\"cerrada\",\"evidencia\":\"frontend/src/connectors-catalog-routing.test.tsx existe (117 lineas, 5 pruebas) y lo he EJECUTADO: 5 passed / 5. El oraculo no es satisfacible por el chrome: workspace.tsx no tiene ningun h1 (solo <strong>{section}</strong> en workspace.tsx:174), y los h1 que la prueba busca solo existen en connectors…"
    },
    {
      "type": "workflow_agent",
      "index": 5,
      "label": "refuta:27-github:0",
      "phaseIndex": 2,
      "phaseTitle": "Refutacion",
      "agentId": "a25e8bf5781a8335d",
      "model": "claude-opus-5[1m]",
      "state": "done",
      "startedAt": 1789031779043,
      "queuedAt": 1789031779006,
      "attempt": 1,
      "lastToolName": "StructuredOutput",
      "lastToolSummary": "La auditoria es solida en lo sustantivo —recontE mutation.j…",
      "promptPreview": "Trabajas en C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb, rama main.\n\nERES DE SOLO LECTURA. No edites nada. NO ejecutes campanas de mutacion, ni la\nsuite completa, ni bin/harness init: hay dos campanas corriendo y 39 contenedores\nvivos. Puedes leer todo y ejecutar como mucho UNA clase de test o UN fichero de\nvitest si es imprescindible para distinguir dos hipotesis.\n\nQUE SE JUEGA A…",
      "promptFramed": false,
      "lastProgressAt": 1789031939288,
      "tokens": 71650,
      "toolCalls": 18,
      "durationMs": 160242,
      "resultPreview": "{\"refutado\":true,\"porque\":\"La auditoria es solida en lo sustantivo —recontE mutation.json y las cifras cuadran: 589 mutantes, 501 Killed, 85,06 %, cero NoCoverage; los 13 del rango 149-159 y los del bloque real 152-161 estan todos muertos por unitarias @s42 que existen en disco y pueden fallar; la deriva de github-connector-client.ts es real (const -> export const desde 44011086) y benigna (3/2 en…"
    },
    {
      "type": "workflow_agent",
      "index": 6,
      "label": "refuta:27-github:1",
      "phaseIndex": 2,
      "phaseTitle": "Refutacion",
      "agentId": "a10a221466bd84e2c",
      "model": "claude-opus-5[1m]",
      "state": "done",
      "startedAt": 1789031779045,
      "queuedAt": 1789031779006,
      "attempt": 1,
      "lastToolName": "StructuredOutput",
      "lastToolSummary": "Verifiqué las siete entradas contra disco. El grueso está b…",
      "promptPreview": "Trabajas en C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb, rama main.\n\nERES DE SOLO LECTURA. No edites nada. NO ejecutes campanas de mutacion, ni la\nsuite completa, ni bin/harness init: hay dos campanas corriendo y 39 contenedores\nvivos. Puedes leer todo y ejecutar como mucho UNA clase de test o UN fichero de\nvitest si es imprescindible para distinguir dos hipotesis.\n\nQUE SE JUEGA A…",
      "promptFramed": false,
      "lastProgressAt": 1789031991333,
      "tokens": 84042,
      "toolCalls": 20,
      "durationMs": 211099,
      "resultPreview": "{\"refutado\":true,\"porque\":\"Verifiqué las siete entradas contra disco. El grueso está bien medido: mutation.json da exactamente 246+341+2 = 589 mutantes con sólo dos estados (501 Killed / 88 Survived, cero NoCoverage), 501/589 = 85,06 %, y la deriva que denuncia en Ambas-4 es real (el `source` embebido en el informe dice `const RECEIPT_FIELDS` / `const CONNECTION_FIELDS` en una línea; el disco dice…"
    },
    {
      "type": "workflow_agent",
      "index": 7,
      "label": "refuta:28-calendario:0",
      "phaseIndex": 2,
      "phaseTitle": "Refutacion",
      "agentId": "a037cc7eaab37535b",
      "model": "claude-opus-5[1m]",
      "state": "done",
      "startedAt": 1789031832653,
      "queuedAt": 1789031832652,
      "attempt": 1,
      "lastToolName": "StructuredOutput",
      "lastToolSummary": "Fui a cada prueba que cierra una condicion y comprobe si pu…",
      "promptPreview": "Trabajas en C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb, rama main.\n\nERES DE SOLO LECTURA. No edites nada. NO ejecutes campanas de mutacion, ni la\nsuite completa, ni bin/harness init: hay dos campanas corriendo y 39 contenedores\nvivos. Puedes leer todo y ejecutar como mucho UNA clase de test o UN fichero de\nvitest si es imprescindible para distinguir dos hipotesis.\n\nQUE SE JUEGA A…",
      "promptFramed": false,
      "lastProgressAt": 1789032175718,
      "tokens": 132992,
      "toolCalls": 19,
      "durationMs": 343063,
      "resultPreview": "{\"refutado\":false,\"porque\":\"Fui a cada prueba que cierra una condicion y comprobe si puede fallar; ninguna de las dos condiciones marcadas \\\"cerrada\\\" esta a medias.\\n\\nC1b (la que mas sospeche, por ser la que el juez exigio de cero): verifique el fixture yo mismo con keytool sobre backend/src/test/resources/tls/anchored-receiver.p12 y sale Owner CN=destino.anclado.invalid con SubjectAlternativeNa…"
    },
    {
      "type": "workflow_agent",
      "index": 8,
      "label": "refuta:28-calendario:1",
      "phaseIndex": 2,
      "phaseTitle": "Refutacion",
      "agentId": "a0c94bd2c15772a46",
      "model": "claude-opus-5[1m]",
      "state": "done",
      "startedAt": 1789031832654,
      "queuedAt": 1789031832652,
      "attempt": 1,
      "lastToolName": "StructuredOutput",
      "lastToolSummary": "Auditoría en general sólida y verificada contra el código: …",
      "promptPreview": "Trabajas en C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb, rama main.\n\nERES DE SOLO LECTURA. No edites nada. NO ejecutes campanas de mutacion, ni la\nsuite completa, ni bin/harness init: hay dos campanas corriendo y 39 contenedores\nvivos. Puedes leer todo y ejecutar como mucho UNA clase de test o UN fichero de\nvitest si es imprescindible para distinguir dos hipotesis.\n\nQUE SE JUEGA A…",
      "promptFramed": false,
      "lastProgressAt": 1789032040587,
      "tokens": 106606,
      "toolCalls": 16,
      "durationMs": 207932,
      "resultPreview": "{\"condicionesMalJuzgadas\":[\"C2 — ratificación humana de la ampliación de contrato del ciclo 4: el estado 'declarada_no_cerrada' es correcto en su núcleo, pero el alcance que la auditoría le atribuye es injusto. Cuenta como tercera edición no cubierta la fila de @s12 de features/external_calendar.feature:198 ('presenta un certificado que no es válido para su nombre | FEED_UNREACHABLE'), cuando esa …"
    },
    {
      "type": "workflow_agent",
      "index": 9,
      "label": "refuta:29-conectores:0",
      "phaseIndex": 2,
      "phaseTitle": "Refutacion",
      "agentId": "ac0ade24f71aac08e",
      "model": "claude-opus-5[1m]",
      "state": "done",
      "startedAt": 1789031863099,
      "queuedAt": 1789031863097,
      "attempt": 1,
      "lastToolName": "StructuredOutput",
      "lastToolSummary": "He ido a cada fichero y linea que la auditoria cita y no he…",
      "promptPreview": "Trabajas en C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb, rama main.\n\nERES DE SOLO LECTURA. No edites nada. NO ejecutes campanas de mutacion, ni la\nsuite completa, ni bin/harness init: hay dos campanas corriendo y 39 contenedores\nvivos. Puedes leer todo y ejecutar como mucho UNA clase de test o UN fichero de\nvitest si es imprescindible para distinguir dos hipotesis.\n\nQUE SE JUEGA A…",
      "promptFramed": false,
      "lastProgressAt": 1789032248750,
      "tokens": 114529,
      "toolCalls": 45,
      "durationMs": 385650,
      "resultPreview": "{\"refutado\":false,\"porque\":\"He ido a cada fichero y linea que la auditoria cita y no he encontrado indulgencia: las cuatro condiciones que declara \\\"cerrada\\\" (C1, C3, C4, C6) lo estan, y sus pruebas pueden fallar. VERIFICACION INDEPENDIENTE. C1: ejecute yo mismo frontend/src/connectors-catalog-routing.test.tsx -> 5 passed/5, y confirme que el fichero entra en la suite (vitest.config.ts include \\\"…"
    },
    {
      "type": "workflow_agent",
      "index": 10,
      "label": "refuta:29-conectores:1",
      "phaseIndex": 2,
      "phaseTitle": "Refutacion",
      "agentId": "ac2abfde5f16cd749",
      "model": "claude-opus-5[1m]",
      "state": "done",
      "startedAt": 1789031863100,
      "queuedAt": 1789031863097,
      "attempt": 1,
      "lastToolName": "StructuredOutput",
      "lastToolSummary": "He comprobado las siete condiciones contra el codigo y NING…",
      "promptPreview": "Trabajas en C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb, rama main.\n\nERES DE SOLO LECTURA. No edites nada. NO ejecutes campanas de mutacion, ni la\nsuite completa, ni bin/harness init: hay dos campanas corriendo y 39 contenedores\nvivos. Puedes leer todo y ejecutar como mucho UNA clase de test o UN fichero de\nvitest si es imprescindible para distinguir dos hipotesis.\n\nQUE SE JUEGA A…",
      "promptFramed": false,
      "lastProgressAt": 1789032088108,
      "tokens": 83187,
      "toolCalls": 24,
      "durationMs": 225008,
      "resultPreview": "{\"refutado\":false,\"porque\":\"He comprobado las siete condiciones contra el codigo y NINGUNA de las que la auditoria declara cerrada esta en realidad abierta. C1: frontend/src/connectors-catalog-routing.test.tsx existe y su oraculo solo puede satisfacerlo el cableado de App.tsx:50-51/59/90-93 (los unicos h1 «Conectores» y «Conector de GitLab» viven en connectors-catalog.tsx y gitlab-connector.tsx; w…"
    },
    {
      "type": "workflow_agent",
      "index": 11,
      "label": "refuta:25-webhooks:0",
      "phaseIndex": 2,
      "phaseTitle": "Refutacion",
      "agentId": "a73d7fac2ab41ac78",
      "model": "claude-opus-5[1m]",
      "state": "done",
      "startedAt": 1789031968930,
      "queuedAt": 1789031968928,
      "attempt": 1,
      "lastToolName": "StructuredOutput",
      "lastToolSummary": "La condicion 1 esta marcada \\\"cerrada\\\" y no lo esta. El au…",
      "promptPreview": "Trabajas en C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb, rama main.\n\nERES DE SOLO LECTURA. No edites nada. NO ejecutes campanas de mutacion, ni la\nsuite completa, ni bin/harness init: hay dos campanas corriendo y 39 contenedores\nvivos. Puedes leer todo y ejecutar como mucho UNA clase de test o UN fichero de\nvitest si es imprescindible para distinguir dos hipotesis.\n\nQUE SE JUEGA A…",
      "promptFramed": false,
      "lastProgressAt": 1789032220053,
      "tokens": 91089,
      "toolCalls": 19,
      "durationMs": 251121,
      "resultPreview": "{\"condicionesMalJuzgadas\":[\"1. AnchoredConnection en el ambito PIT (estaba en ninguno: cero mutantes) — marcada \\\"cerrada\\\", en realidad cerrada a medias\"],\"porque\":\"La condicion 1 esta marcada \\\\\\\"cerrada\\\\\\\" y no lo esta. El auditor uso backend/build/reports/pitest-webhooks/mutations.xml para declarar \\\\\\\"Cero mutantes se acabo\\\\\\\" y cito su agregado (382 KILLED de 425, por encima de 0,80) como …"
    },
    {
      "type": "workflow_agent",
      "index": 12,
      "label": "refuta:25-webhooks:1",
      "phaseIndex": 2,
      "phaseTitle": "Refutacion",
      "agentId": "af77463779ea3fd13",
      "model": "claude-opus-5[1m]",
      "state": "done",
      "startedAt": 1789031968931,
      "queuedAt": 1789031968928,
      "attempt": 1,
      "lastToolName": "StructuredOutput",
      "lastToolSummary": "OJO: refuto en la direccion CONTRARIA a la que sugiere el c…",
      "promptPreview": "Trabajas en C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb, rama main.\n\nERES DE SOLO LECTURA. No edites nada. NO ejecutes campanas de mutacion, ni la\nsuite completa, ni bin/harness init: hay dos campanas corriendo y 39 contenedores\nvivos. Puedes leer todo y ejecutar como mucho UNA clase de test o UN fichero de\nvitest si es imprescindible para distinguir dos hipotesis.\n\nQUE SE JUEGA A…",
      "promptFramed": false,
      "lastProgressAt": 1789032157903,
      "tokens": 86761,
      "toolCalls": 15,
      "durationMs": 188971,
      "resultPreview": "{\"refutado\":true,\"porque\":\"OJO: refuto en la direccion CONTRARIA a la que sugiere el campo. No he encontrado ninguna condicion declarada cerrada que no lo este —las dos que la auditoria da por cerradas lo estan y las he verificado a mano—. Lo que hay es SEVERIDAD INJUSTA en la condicion 3.\\n\\nLo que confirmo de la auditoria (correcto, sin objecion):\\n- Cond. 1: backend/build.gradle.kts:628 y :454 …"
    },
    {
      "type": "workflow_agent",
      "index": 13,
      "label": "checklist",
      "phaseIndex": 3,
      "phaseTitle": "Checklist",
      "agentId": "aa3a30e39b92fb3eb",
      "model": "claude-opus-5[1m]",
      "state": "done",
      "startedAt": 1789032248756,
      "queuedAt": 1789032248752,
      "attempt": 1,
      "lastToolName": "Bash",
      "lastToolSummary": "sed -n '715,750p' backend/build.gradle.kts; echo \"=== Slf4j…",
      "promptPreview": "Trabajas en C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb, rama main.\n\nERES DE SOLO LECTURA. No edites nada. NO ejecutes campanas de mutacion, ni la\nsuite completa, ni bin/harness init: hay dos campanas corriendo y 39 contenedores\nvivos. Puedes leer todo y ejecutar como mucho UNA clase de test o UN fichero de\nvitest si es imprescindible para distinguir dos hipotesis.\n\nQUE SE JUEGA A…",
      "promptFramed": false,
      "lastProgressAt": 1789032381310,
      "tokens": 80948,
      "toolCalls": 6,
      "durationMs": 132554,
      "resultPreview": "# Lo que falta para `done`, feature por feature\n\nVerificado a mano lo decisivo (grep sobre `mutations.xml`, `build.gradle.kts`, `project-spec.md`, `.feature`). Donde la auditoría y la refutación discrepan, digo cuál sostiene el disco.\n\n---\n\n## Feature 25 — webhooks\n\n**1. Condiciones no cerradas**\n\n- **Cond. 4 (field-sizing) — abierta.** `grep field-sizing project-spec.md` no devuelve nada. Falta a…"
    }
  ],
  "totalTokens": 1365350,
  "totalToolCalls": 324
}