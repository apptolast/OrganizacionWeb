# Motivos bloqueantes del panel — feature 27-github

Extraidos integros de progress/panel_precierre_27_30.md. Cada motivo lleva
QUE (el hallazgo) y POR QUE BLOQUEA (por que un juez rechazaria el cierre).

## Lente: Cobertura del contrato: recorrido escenario a escenario del .feature buscando cláusulas del Then sin oráculo que pueda fallar, más verificación profunda de tres escenarios elegidos (s16 paginación, s24 proyecto terminado, s37/s38 formulario y resumen).

### M1

**Que:** @s41, primera fila: el contrato exige que al navegar a otra pantalla y volver con la misma sesión «repository conserva "octocat/Hello-World" y el campo token está vacío». El producto NO conserva el repositorio, y la única prueba de esa fila afirma sólo la mitad del token. El nombre de la prueba —«forgets the token but keeps the repository when the screen is remounted»— declara la mitad que ningún expect() comprueba: tras view.unmount() + render() sólo hay dos aserciones, toHaveValue("") sobre el token y un innerHTML sin el token. Añadir hoy expect(getByLabelText(/repositorio/i)).toHaveValue("octocat/Hello-World") falla: App.tsx:98-99 renderiza <GithubConnector> dentro de un ternario por ruta, así que salir de /integraciones/github lo desmonta, y github-connector.tsx:61 vuelve a nacer con useState(""). El juez dio la fila por buena (progress/judge_github_connector.md:307, «@s41 [x] las cuatro filas»).

**Por que bloquea:** Es una cláusula del Then declarada cerrada cuya conducta no existe y cuyo oráculo no puede fallar por construcción. No es un oráculo débil: es un Then que hoy sería rojo si alguien lo escribiera. Marcar done con esto dentro repite exactamente el patrón que la noche ya ha cazado siete veces: ramas que ninguna prueba ejecuta y filas de contrato que nadie mide.

### M2

**Que:** @s14 (dos filas) y @s15 (una fila) nombran U+00A0 y «sólo Unicode White_Space», y ninguna prueba mete jamás un U+00A0 —ni un U+2003— en ExternalIssue. ExternalIssueTest.java:16-23 usa sólo espacios ASCII ("  Arreglar login ", "   ", "b"*155+"      "); ExternalIssueCriterionTest.java:23 usa "", "   " y "  \n\r\n\t". El recorte de producción es un literal de expresión regular, ExternalIssue.java:28: replaceAll("(?U)^\\s+|\\s+$", ""). Sustituirlo por String.strip() deja verdes las 14 aserciones existentes y rompe las tres filas del contrato, porque Character.isWhitespace(' ') es false y strip() no quita el espacio duro. PIT no muta literales de cadena, así que la campaña tampoco puede delatarlo. Y el proyecto ya sabe que este caso importa: el gemelo del mismo regex en GithubRepository.parse SÍ tiene su oráculo con   y   (GithubRepositoryTest.java:12-14, fila @s5:76). Falta justo donde el gemelo lo tiene.

**Por que bloquea:** Tres filas del contrato cuyo oráculo no distingue la implementación correcta de una que las incumple, en un punto que ninguna puerta puede cubrir (PIT no muta cadenas). Es el mismo hueco que produjo los diez literales de SQL y de códigos de error de esta misma noche.

### M3

**Que:** El ámbito de mutación github_connector no muta una clase de la 27 y sí muta una de la 28. GithubIssueConnections —la clase que fija source()="github" (que acaba en cada recibo y en cada fila de task_external_links), que traduce la fila almacenada a IssueConnection decidiendo projectPath, reference y valid, y cuyo invalidate(ownerId, errorCode, at) tira a propósito errorCode y at— no aparece en NINGÚN targetClasses del repositorio: el único glob que la alcanzaría es el «else ->» de la corrida completa, y bin/harness mutate github_connector pasa -PmutationScope=github_connector (scripts/project.mjs:212). Su gemela GitlabIssueConnections sí está declarada, en el ámbito de la 29 (build.gradle.kts:89). En sentido contrario, ImportGuard está DENTRO del ámbito de la 27 (:121) pero sólo lo usan ConnectGitlab.java:46 y DisconnectGitlab.java:28. La cifra 467/502 no mide, por tanto, lo que dice medir.

**Por que bloquea:** El veredicto vigente está condicionado a la puerta de mutación, y su punto 7 exige que ninguna clase del ámbito quede a cero mutantes por «glob muerto». Una clase de la feature que ni siquiera está nombrada es peor que una a cero: no aparece en el informe, así que nadie la echa de menos. Es el cuarto ámbito mal apuntado de la noche, y el precedente de ImportGithubIssues —anotado en el propio build.gradle.kts:117-119— dice que aquí se rechaza por esto. (No he podido contrastarlo contra los XML: tanto pitest-noche-cinco/mutations.xml como pitest-github-connector/mutations.xml están truncados a media escritura, campañas en curso; el hallazgo es estático y se lee en el build.)

### M4

**Que:** Cláusulas de Then declaradas y no medidas en ninguna parte: (a) @s3 «el resto de rutas de proyectos y tareas responde con normalidad» — GithubConnectorWiringTest y GithubConnectorApiTest sólo afirman las cinco rutas del conector; (b) @s29 «no se crea ningún recibo nuevo en ninguna fila y el servidor falso recibe cero peticiones» — de las seis filas, sólo dos lo comprueban; las cuatro pruebas s29_* de ImportGithubIssuesTest.java:432-462 afirman únicamente la excepción lanzada, sin mirar fakes.receipts.size() ni fakes.source.calls(); (c) @s27 «no se relanza ninguna importación automáticamente ni se contacta con el servidor falso» — sin oráculo (hoy es cierto porque no hay ningún @Scheduled del conector, pero nada lo sujeta); (d) @s41 fila 4, «la sesión vence en mitad de la importación» — ninguna prueba del conector la ejerce; los tres 401 de github-connector.test.tsx llevan código GITHUB_TOKEN_REJECTED, que es otra cosa. Descansa en el mecanismo global de api-client.ts:32.

**Por que bloquea:** Cada una es una frase del contrato que hoy no puede ponerse roja. Ninguna sola justifica el rechazo, pero juntas dicen que el recuento de filas del juez se hizo por escenario y no por cláusula.

### M5

**Que:** El Then de @s12 «finishedAt no anterior a startedAt» lo sostiene un literal de SQL sin oráculo: PostgresIssueImportReceiptStore.java:135 escribe finished_at=GREATEST(?, started_at), y la única prueba que cierra un recibo (GithubConnectorPersistenceTest.java:500-516) usa NOW.plusSeconds(4), muy por encima de started_at. Quitar el GREATEST deja verde toda la suite. PIT no muta la cadena SQL.

**Por que bloquea:** Defensa contra desajuste de reloj que ninguna prueba distingue de su ausencia, con la restricción de la V25 esperando al otro lado. Es el mismo molde que el evento perdido en el mismo microsegundo, aunque el daño aquí sea menor.

## Lente: seguridad y datos

### M6

**Que:** La fila 1 de @s41 («navego a otra pantalla y vuelvo con la misma sesión → repository conserva "octocat/Hello-World" y el campo token está vacío») NO la cumple el producto y NO la mide ningún oráculo. `App.tsx:98-99` renderiza `<GithubConnector>` por render condicional, así que salir de /integraciones/github lo DESMONTA; `repository` es `useState("")` local (`github-connector.tsx:61`), y al volver el campo está vacío. La prueba que dice cubrirlo, `frontend/src/github-connector.test.tsx:540` — «forgets the token but keeps the repository when the screen is remounted» — sólo afirma las DOS cosas del token (`:555` token vacío, `:556` el token no está en el HTML) y no afirma nunca el repositorio. Comprobado: `grep toHaveValue` en la unitaria y en el E2E sólo da la fila 409 de @s37 (`github-connector.test.tsx:262`, `e2e/github-connector.spec.mjs:473`), que es otro caso y ocurre sin desmontar. El veredicto vigente da por buenos «los 41 escenarios ya cubiertos» sin volver a mirarlos: esa afirmación es falsa para esta fila.

**Por que bloquea:** Marcar `done` afirma que los 42 escenarios están cubiertos. Aquí hay una fila del contrato que el código contradice y que ningún assert toca, disfrazada por un nombre de test que promete lo que no comprueba: exactamente la clase de «condición declarada cerrada» que esta noche ha estado destapando. El daño de producto es pequeño (comodidad), pero la puerta de cobertura no está pasada.

### M7

**Que:** `ImportIssues.collect()` descifra el token en `:145-148` y lanza `SecretUndecipherableException` cuando ninguna clave del llavero abre el texto guardado. Esa excepción es `RuntimeException` pura (`SecretUndecipherableException.java`) y `run()` sólo captura `IssueSourceException` (`:108`), `ProjectCompletedException` (`:110`) y `StorageUnavailableException` (`:112`). Se escapa por encima de `receipts.begin()` (`:74`), que YA ha insertado un recibo `running`. Resultado: el 503 CONNECTOR_KEY_MISMATCH sale, pero el recibo queda `running` con `finished_at IS NULL` hasta que otro intento lo barra a los 15 minutos. Mientras tanto: cualquier nuevo POST choca con el índice único parcial y responde 409 IMPORT_IN_PROGRESS (mentira: no hay nada importando), y la pantalla entra en el estado «Hay una importación en curso» (`github-connector.tsx: running → canImport false`), que oculta el botón de importar y NO enseña la causa real. La rama no la ejerce ninguna prueba: `grep -rn "Undecipherable"` en `backend/src/test/.../application/` no da un solo acierto; el único test es `GithubConnectorApiTest:709`, que la simula con un mock EN la frontera HTTP y por tanto nunca ve el recibo colgado. El disparador es real y operativo: rotar `APP_CONNECTOR_KEY` sin conservar `key-previous`, o restaurar una copia con otra clave.

**Por que bloquea:** Es una rama que nadie ejecuta, alrededor del manejo de la clave de cifrado, y deja estado persistente incoherente: un recibo `running` que no corresponde a ninguna importación viva. @s25, @s26 y @s27 construyen sus garantías sobre el significado de `running` («hay una importación en curso» o «murió a mitad»), y este camino lo rompe sin que nada lo mida. El contrato del recibo dice, en todos los fallos que sí nombra, que queda `failed` con su errorCode.

### M8

**Que:** `GithubIssueConnections` NO está en ningún ámbito de mutación de los que se han medido. `githubConnectorClasses` (build.gradle.kts:113-136) enumera `ConnectGithub*`, `ImportIssues*`, `ReadGithubConnection*`, `DisconnectGithub*`, `ReadIssueImport*`, `ConnectorFailures*`… y no incluye `application.GithubIssueConnections*`; `additionalConnectorsClasses` (`:82-110`) sí lista su gemela `GitlabIssueConnections*` pero, por su propio comentario, excluye a propósito lo compartido con la 27. `noche_cinco` es la unión de esos dos conjuntos, así que tampoco. Sólo la aparece el ámbito por defecto (`core` = `application.*`, `:661`), que nadie corre. Y esa clase no es un portador de datos: mapea la fila a `IssueConnection` (repositorio, referencia, texto cifrado, validez) y, sobre todo, implementa `invalidate()`, que es lo que marca la conexión como inválida tras el 401 de @s22. Un mutante VOID_METHOD_CALLS sobre `connections.invalidate(ownerId)` significa «el token rechazado sigue considerándose válido y se reenvía en cada importación» y no se genera en ninguna campaña.

**Por que bloquea:** El juez exigió «ninguna clase nombrada en el ámbito con cero mutantes» y desglose por clase; una clase que ni siquiera está en el ámbito no aparece en el desglose, así que el informe puede salir impecable con esta clase sin un solo mutante. Es el patrón que la noche ya cazó cuatro veces. El 93,03 % no cubre esta clase.

### M9

**Que:** El ámbito `github_connector` incluye el glob de paquete `com.apptolast.organization.adapter.connectors.*` (`:130`), que arrastra `GitlabApiBase` y `HttpGitlabIssueSource` — ambas listadas también, una por una, en `additionalConnectorsClasses` (`:106-107`). O sea: las dos campañas se solapan por construcción, justo lo contrario de lo que declara el comentario de `:41-43` («No incluye las clases compartidas con la 27 … un mutante contado dos veces no informa»). El adaptador HTTP de GitLab no es pequeño, y sus mutantes los matan las pruebas de la 29.

**Por que bloquea:** La cifra que se presenta como la de la feature 27 —93,03 %, 467/502— tiene en el denominador clases que no son de la 27 y en el numerador muertes que no las provocan las pruebas de la 27. El juez pidió explícitamente «no acepto un porcentaje sin el recuento al lado» porque un ámbito mal apuntado da buena puntuación; aquí el ámbito está contaminado en la dirección que infla.

### M10

**Que:** `PersonalAccessToken.hint()` devuelve el token ENTERO cuando mide 4 caracteres o menos (`value.length() <= HINT_LENGTH ? value : substring(...)`), y el dominio no impone longitud mínima distinta de 1 (sólo nulo/vacío, >255 y no-ASCII). `PostgresGitlabConnectionStore` persiste ese `token_hint` en claro (columna `TEXT CHECK char_length BETWEEN 1 AND 4`, V29) y lo devuelve en su DTO. La clase del dominio la posee la 27 —está en su ámbito de mutación y su @s6 fija sus reglas—, y su propia V30 razona por escrito sobre «un token de un solo carácter, que el dominio admite».

**Por que bloquea:** Un secreto corto acaba en claro en la base de datos y en una respuesta de API, por un camino que la 27 abre al no fijar longitud mínima. No lo ejerce ninguna ruta de la 27, así que no bloquea su cierre, pero la corrección (mínimo de dominio) es de la 27 y debe salir con dueño, no como nota suelta.

### M11

**Que:** `ReadIssueImport.execute` busca el recibo sólo por `(owner_id, id)` — `PostgresIssueImportReceiptStore.read()` no filtra por `source`, a diferencia de `latest(ownerId, source)`, que sí lo hace desde la V29. Así, `GET /api/v1/me/connectors/github/imports/{id}` devuelve 200 con un recibo de GitLab del mismo propietario. No cruza propietarios (@s30 se mantiene) y el DTO declara `source`, y el cliente lo rechaza (`github-connector-client.ts` exige `source === "github"`), así que sale como «Confirmación incompatible».

**Por que bloquea:** No hay fuga entre propietarios, sólo entre conectores del mismo dueño, y el contrato de @s30 no la prohíbe expresamente. Queda como incoherencia entre las dos lecturas del mismo almacén (una filtra por origen y la otra no) que nadie mide.

### M12

**Que:** El `full_name` que devuelve GitHub se guarda tal cual como `repository` (`ConnectGithub:44`, `identity.fullName()`) sin volver a pasarlo por `GithubRepository` —la validación sólo se aplica a lo que escribe la persona— y la única cota es el `CHECK char_length BETWEEN 3 AND 140` de V25. Después se concatena a pelo en la URL saliente (`GithubApiBase.repository()` → `value + "/repos/" + repository`) y se pasa a `URI.create` en `HttpGithubIssueSource.get()`, que sólo captura `InterruptedException` e `IOException`. Un `full_name` con espacio, `?` o `#` produce `IllegalArgumentException`, que sube por `collect()` y sale por el mismo agujero del hallazgo 2: recibo `running` colgado y 500.

**Por que bloquea:** Requiere que el proveedor al otro lado devuelva un `full_name` no canónico, y la lista blanca de `GithubApiBase` limita ese lado a api.github.com o loopback, así que el riesgo real es bajo. Lo anoto porque el valor es de fuera y viaja sin validar hasta una URL y hasta una columna, y porque su fallo cae en la misma rama sin capturar del hallazgo 2.

## Lente: Lo que se rompió y lo que se tocó deprisa: historial reciente de los ficheros de la feature, cambios de producción sin prueba, aserciones que no pueden fallar, y arreglos que silencian el síntoma. Contrastado contra los artefactos de mutación reales en disco, no contra las cifras declaradas.

### M13

**Que:** `Slf4jConnectorAudit` está nombrada en el ámbito PIT de la feature y generó CERO mutantes en la campaña que sostiene el 93,03 %. El juez lo declaró causal de rechazo con estas palabras: «Ninguna clase nombrada en el ámbito con cero mutantes, salvo los cuatro portadores de datos… Cualquier otra clase a cero es un glob muerto y hay que arreglarlo antes de dar la campaña por buena». Los cuatro portadores exentos (IssuePage 7, StoredConnection 8, ConnectionView 6, IssueSourceException 10) SÍ recibieron mutantes; la única clase a cero es el auditor. Verificado: `grep -c Slf4jConnectorAudit backend/build/reports/pitest-github-connector/mutations.xml` = 0, y el informe HTML no tiene paquete `adapter.logging` (27 clases: 1+7+1+3+11+4). Causa raíz confirmada en el propio fichero de build: el bloque pitest fija `excludedMethods` pero NO fija `mutators` ni `avoidCallsTo` (`backend/build.gradle.kts:733`), así que el `avoidCallsTo` por defecto de PIT suprime las llamadas a `org.slf4j`, y esta clase es puro logging. Es el MISMO defecto, en la clase gemela, que `progress/auditoria_condiciones_cierre.md` ya declara bloqueante para la feature 25 (`Slf4jWebhookAudit`, «la puerta no vale aunque la puntuación salga alta»). Aquí ni siquiera está declarado: la bitácora de cierre no lo menciona.

**Por que bloquea:** Es una condición dura del veredicto vigente, literal y verificable, y está incumplida sin declarar. Además deja sin oráculo de mutación justamente la clase que implementa «bitácora sin token» (@s?, commit 11a0c80b): la redacción de la bitácora del conector no tiene un solo mutante en ninguna campaña del repositorio.

### M14

**Que:** El informe de backend que produce el 93,03 % (467/502) sigue mostrando VIVO el mutante de frontera que el juez exigió KILLED uno por uno. En `AesGcmSecretCipher.java.html`, línea 60, mutante 3: «changed conditional boundary → SURVIVED / Killed by: none». El juez escribió: «`:60` `ciphertext.length <= SHORTEST` (NEGATE_CONDITIONALS y CONDITIONALS_BOUNDARY)… Un superviviente en cualquiera de estos puntos ES RECHAZO, aunque la puntuación global pase». Cronología, medida en disco: el HTML del cifrador se escribió a las 11:49:00 y el `index.html` a las 11:52:09; el commit que añade el oráculo (`e5e0d563`, «matar el mutante de frontera del cifrador que el juez exigia») es de las 11:54:17. El oráculo se escribió DESPUÉS de la campaña y se verificó a mano aplicando el mutante; la campaña no se ha vuelto a lanzar. Segundo problema en el mismo sitio: ese commit afirma «el cifrador genero 26 mutantes»; el informe dice 17 (16 KILLED, 1 SURVIVED, 0 NO_COVERAGE). El suelo de 12 se cumple igual, pero la cifra que se presenta como prueba de que el ámbito apunta bien está mal por nueve, y el suelo era precisamente el disparador que el juez puso contra ámbitos mal apuntados.

**Por que bloquea:** La cifra que se ofrece a la puerta (93,03 %) y el artefacto que la respalda contienen el superviviente que el juez declaró causal de rechazo. El arreglo es creíble y probablemente lo mata, pero eso hay que MEDIRLO, no razonarlo: es exactamente la sustitución de medición por argumento que la noche ya ha castigado cuatro veces. Y la cifra de 26 mutantes no la sostiene ningún artefacto.

### M15

**Que:** `progress/mutation_github_connector.md` no existe. El juez lo exige como condición 8: «Todo superviviente, documentado en `progress/mutation_github_connector.md`: matado con test nuevo o justificado como equivalente, uno por uno (C7)». La campaña de backend deja 35 mutantes no muertos: 25 SURVIVED y **10 NO_COVERAGE** (deducidos del informe: cobertura 467/502 frente a fuerza de prueba 467/492). Ninguno está documentado en ninguna parte. Los sin cobertura se reparten en HttpGithubIssueSource (1), HttpGitlabIssueSource (3), GithubConnectorController (1), PostgresIssueImportReceiptStore (2), IssueImportReceipt (2) y BoundedResponse (1). Los únicos ficheros de mutación de la feature son `mutacion_github_connector_frontend_cierre.md` y `mutacion_github_connector_supervivientes.md`, ambos de frontend.

**Por que bloquea:** Diez mutantes SIN COBERTURA en una campaña de cierre significan diez ramas de producto que ninguna prueba ejecuta. Es literalmente el patrón que esta noche ha producido siete defectos reales de producto. Y es una condición explícita del veredicto, no una formalidad: el juez la ató a C7.

### M16

**Que:** El contrato de la feature se enmendó a las 00:19 de hoy SIN contrafirma del propietario. `a1b0d20b` reescribe `features/github_connector.feature:146` («once campos» → «doce campos») y `:165-166` (el recibo pasa de `repository` a `source` + `projectPath`, y se añaden dos aserciones nuevas). El propio fichero lleva el formato correcto en las dos enmiendas anteriores: `:55` y `:397` dicen «Enmienda del 9 de septiembre de 2026, ratificada por el propietario». Las líneas nuevas no llevan ni comentario de enmienda ni contrafirma; `grep ratificad` sobre el fichero sólo devuelve esas dos. Es el mismo rasero que `progress/auditoria_condiciones_cierre.md` aplica como C7 BLOQUEANTE a la feature 29, citando precisamente `github_connector.feature:393` como el precedente que sí la lleva.

**Por que bloquea:** CLAUDE.md, regla dura: «No saltes la puerta de aprobación humana sobre los `features/<name>.feature`». Y el proyecto acaba de bloquear la 29 por esto mismo con una desviación menor (allí al menos hay comentario de enmienda; aquí no hay nada). Marcar `done` la 27 con el contrato enmendado por el propio carril sería incoherente con el dictamen que sigue abierto sobre la 29.

### M17

**Que:** El veredicto vigente se emitió sobre `HEAD = cc76ec5` y su disciplina TDD se sostiene en «el diff NO toca `src/` en absoluto… Cero producción nueva». Después del veredicto sí se tocó producción: `a1b0d20b` (00:19) cambia `frontend/src/github-connector-client.ts` (RECEIPT_FIELDS a doce claves, el tipo `GithubImportReceipt`, y `decodeReceipt` pasa de `typeof value.repository !== "string"` a `value.source !== GITHUB || !nonEmpty(value.projectPath)`), y `44011086` (10:45) lo vuelve a tocar exportando dos constantes. La superficie que el juez leyó ya no es la que se va a cerrar, y la afirmación de disciplina que sostiene su apartado TDD ha caducado.

**Por que bloquea:** No es que el cambio sea malo —tiene rojo acreditado y una prueba de contrato que ata el record de Java al cliente—, es que el veredicto que se invoca para cerrar no lo ha visto. Cerrar con un APPROVED emitido sobre otro árbol es exactamente el «una condición declarada cerrada tampoco es evidencia» del encargo.

### M18

**Que:** El ámbito PIT `github_connector` mide producción de la feature 29. El comodín `com.apptolast.organization.adapter.connectors.*` arrastra `GitlabApiBase` (27 mutantes) y `HttpGitlabIssueSource` (54 mutantes): 81 de los 502 (el 16 %) son código GitLab, y aportan 4 de los 25 supervivientes y 3 de los 10 sin cobertura. Además `ImportGuard*` está en `githubConnectorClasses` (`:121`) aunque sólo lo llaman `ConnectGitlab` y `DisconnectGitlab` —hallazgo ya escrito en `progress/auditoria_condiciones_cierre.md` y todavía sin corregir—. Descontado GitLab, la feature 27 mide 393/421 = 93,3 %: sigue pasando, pero la puntuación declarada no es la de la feature que se cierra.

**Por que bloquea:** El juez pidió el desglose por clase precisamente porque «un ámbito mal apuntado da buena puntuación sin generar un solo mutante de la clase que importa». Aquí el desglose existe pero nadie lo ha leído: nadie ha notado ni las clases de otra feature dentro ni la clase propia a cero. No tumba el umbral, pero invalida la lectura de la puerta.

### M19

**Que:** Las dos pruebas de persistencia que justifican la migración V30 llevan una aserción que no puede fallar y no tocan el cifrador real. `s1_theShortestTokenTheDomainAcceptsAlsoFitsInTheColumn` construye `var sealed = new byte[NONCE_BYTES + shortest.value().length() + TAG_BYTES]` y a continuación afirma `assertThat(sealed).hasSize(29)`: es cierta por construcción, con cualquier valor de las constantes locales del test. Lo mismo en la del token largo con 283. Ninguna de las dos llama a `AesGcmSecretCipher`, así que nada ata la cota 29..283 de la V30 al formato en reposo que el cifrador produce de verdad. Si el formato vuelve a cambiar (por ejemplo si regresa el byte de versión), las dos pruebas siguen verdes y la restricción vuelve a rechazar filas legítimas con un 500 — que es literalmente el defecto que estas pruebas se escribieron para impedir que se repitiera.

**Por que bloquea:** No bloquea por sí solo: la mitad útil (que PostgreSQL acepte la fila) sí muerde, y el juez ya dio la deuda del CHECK por cerrada. Pero es una aserción tautológica presentada como el oráculo que «fija el rango entero para que una cota calculada para otro formato no vuelva a pasar inadvertida», y no lo hace.

### M20

**Que:** La campaña de frontend no dice contra qué commit se midió y hay deriva medible dentro de la ventana. `progress/mutacion_github_connector_frontend_cierre.md:3` dice sólo «sobre `main`». El informe se escribió a las 10:55:31; `44011086` toca `frontend/src/github-connector-client.ts` (dos `export` nuevos) y su test (+66 líneas) a las 10:45:01, diez minutos antes. Una corrida de 589 mutantes con `concurrency: 8` empieza bastante antes de las 10:45, así que lo más probable es que el 85,06 % se midiera sobre el árbol anterior. El juez lo pidió expresamente: «Ambas — 4. Medidas sobre este árbol… El informe debe decir contra qué commit se midió». Las cifras en sí las he verificado y son exactas (501/589 = 85,06 %, 0 sin cobertura, los tres módulos con mutantes).

**Por que bloquea:** No cambia el veredicto numérico —los dos exports no generan mutantes y el total sigue siendo 589—, pero deja la trazabilidad rota justo en la condición que el juez escribió para impedirlo, y ya no se puede reconstruir a posteriori.
