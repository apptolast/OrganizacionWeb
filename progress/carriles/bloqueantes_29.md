# Motivos bloqueantes del panel — feature 29-conectores-adicionales (features/additional_connectors.feature)

**Cerrable: NO** — 11 bloqueantes, ya deduplicados y con los que no se sostenian descartados por el sintetizador.

Panel de tres jueces independientes (cobertura del contrato, seguridad y datos,
lo que se toco deprisa) mas sintesis, 10 de septiembre de 2026.

## Resumen del sintetizador

Tres jueces, 30 motivos en crudo; tras comprobarlos uno a uno contra el código quedan 11 bloqueantes reales y 5 hallazgos que no llegan a bloquear. La feature NO es cerrable, y no por matices: las dos puertas de mutación están una por debajo del umbral y la otra sin medir siquiera.

Lo medido por mí, no citado: frontend/reports/mutation-additional-connectors/mutation.json da 494 Killed + 13 Timeout frente a 216 Survived + 17 NoCoverage = 68,51 % (gitlab-connector.tsx 64,58 %, gitlab-connector-client.ts 65,98 %, connectors-catalog.tsx 76,62 %, connectors-catalog-client.ts 83,33 %), contra el 0,80 de harness.config.json:22 y el break=80 de stryker.additional-connectors.config.json. backend/build/reports/pitest/ —el directorio por defecto donde cae el ámbito additional_connectors, porque backend/build.gradle.kts:715-743 no le asigna reportDir— está VACÍO. En los dos XML que sí tocaron clases de la 29 confirmé los supervivientes concretos: 10 sobre GitlabConnectorController$ImportResponse:304, 4 sobre HttpGitlabIssueSource (:97 ×2, :180, :185) y 3 sobre BoundedResponse (:82, :90, :98).

SE CAEN O SE DEGRADAN (no los cuento como bloqueantes):
(1) «@s28 no tiene oráculo, la transición a failed/INTERRUPTED no la afirma nadie en el carril de GitLab» (juez de contrato). Se cae como bloqueante: el mecanismo SÍ está medido, sobre la clase de producción compartida PostgresIssueImportReceiptStore, cuyo UPDATE (:81-88) es agnóstico del gestor —WHERE owner_id AND status='running' AND started_at < ?— y lo verifica GithubConnectorPersistenceTest:471-481 afirmando status failed, errorCode INTERRUPTED, finishedAt no nulo, created y skipped. Lo que falta es el recorrido extremo a extremo de la fila de GitLab, que es hueco de escenario, no de mecanismo. Queda como serio.
(2) «La frontera 14/16 minutos se mide contra lógica del propio test» (juez de historial). Se cae: GitlabConnectorPersistenceTest:285-297 conduce el SELECT real started_at >= ? contra Postgres a 16 y a 17 minutos con las dos respuestas opuestas, y GitlabConnectionUseCasesTest:243 sí fija ImportIssues.ABANDONED_AFTER=15min (encoger la ventana pone roja la prueba de 14 minutos, porque el startedAt pasaría a ser anterior al staleBefore). La afirmación de que sólo la mide el doble es falsa.
(3) «La url del enlace no se afirma en el carril de GitLab» (@s15, juez de contrato). Se estrecha: HttpGitlabIssueSourceTest:167 SÍ afirma issue.url() == web_url. Lo que no se afirma es que esa url llegue a task_external_links. Deja de ser un motivo propio.
(4) «El único ámbito que alcanzaría a ConnectorError/ConnectorRateLimitedException/ConnectorsDisabledException es core, que no lo corre nadie» (juez de historial). Medio falso: core lleva el comodín application.* y SÍ entra en el ámbito por defecto (build.gradle.kts:681), así que en la campaña completa esas clases sí reciben mutantes. Lo cierto —y lo conservo dentro del bloqueante de mutación de backend— es que ningún ámbito nombrado, el de la 29 incluido, las alcanza.
(5) «11 mutantes sobre ImportResponse». Son 10 (id, projectId, status, skipped, failed, truncated a true y a false, errorCode, startedAt, finishedAt). La sustancia se mantiene entera; corrijo sólo la cifra.

SOBREVIVEN SIN BLOQUEAR, para el registro: @s9 dice «token_nonce de 12 bytes» y esa columna no existe (V29__additional_connectors.sql:37 sólo declara token_ciphertext, con el nonce embebido) — hay que enmendar el .feature con contrafirma, como se hizo con @s31; ConnectGitlab.java:51 persiste el path_with_namespace que devuelve GitLab sin revalidarlo, y si excede el CHECK de 255 el fallo del proveedor sale como 503 STORAGE_UNAVAILABLE, que miente sobre la causa; HttpGitlabIssueSourceTest:232 (s25_aprovidearThatNeverFinishesAnsweringIsUnavailable) está documentada como no discriminante y sigue en la suite inflando el recuento de oráculos; ImportIssues.java:196 fija «TaskCreated.v1» sin oráculo que lo lea, y cambiarlo dejaría de disparar en silencio las automatizaciones de la 30; y la contrafirma de C7 la escribió el mismo carril que hizo la enmienda, sin artefacto firmado por el propietario.

Un apunte a favor del carril, verificado: los seis cierres declarados (C1, C3, C4, C5, C6, C7) son reales y están bien ejecutados. Los defectos que quedan son anteriores a las condiciones y ninguna de las siete los tocaba.

---

### B1

**Que:** La puerta de mutación de frontend está once puntos por debajo del umbral, y el informe que la publica ya no describe el árbol actual. Calculado por mí sobre frontend/reports/mutation-additional-connectors/mutation.json: 494 Killed + 13 Timeout contra 216 Survived + 17 NoCoverage = 68,51 %. Por fichero: gitlab-connector.tsx 64,58 % (97 supervivientes, 5 sin cobertura), gitlab-connector-client.ts 65,98 % (88 y 11), connectors-catalog.tsx 76,62 %, connectors-catalog-client.ts 83,33 %. El informe es de las 11:47 y frontend/src/connectors-catalog.tsx cambió a las 12:42 en el commit 99b4d028.

**Por que bloquea:** harness.config.json:22 fija 0,80, frontend/stryker.additional-connectors.config.json declara break=80, y CLAUDE.md prohíbe marcar done sin mutación por encima del umbral. No es un ámbito discutible ni una interpretación: es la puerta declarada, medida y suspendida. Faltan unas 85 muertes. Además, la cifra publicada en progress/mutacion_cinco_features.md:45 está caducada por un cambio de producción posterior, así que cualquier cierre que la cite estaría citando una medición que ya no corresponde al código.

**Como se cierra:** Escribir pruebas hasta matar los supervivientes de los dos ficheros que hunden la campaña (gitlab-connector.tsx y gitlab-connector-client.ts concentran 185 de los 216) y volver a correr Stryker sobre el árbol posterior a 99b4d028, publicando la cifra nueva con su marca de tiempo en progress/.

### B2

**Que:** No existe ninguna medición de mutación de backend para esta feature, y el ámbito cambió después de arrancar las campañas. backend/build.gradle.kts:715-743 asigna reportDir a 28 ámbitos y a ninguno le toca additionalConnectorsOnly, que sólo aparece en :47, :656 y :689: su informe cae en el directorio por defecto build/reports/pitest, que hoy está VACÍO (creado a las 12:49). Los dos únicos XML que llegaron a tocar clases de la 29 (pitest-noche-cinco y pitest-github-connector) abren ambos con <mutations partial="true">. Encima, build.gradle.kts avanzó a las 12:58 con el commit 6ec8e69c, posterior a las dos campañas. Y tres clases de producción de la 29 —application/ConnectorError.java (la columna lastError de @s2 y @s5), ConnectorRateLimitedException (el retryAfterSeconds que @s24 fija en 30/60/5) y ConnectorsDisabledException (@s3, @s29)— no aparecen en ningún ámbito nombrado: grep sobre build.gradle.kts devuelve cero para los tres.

**Por que bloquea:** No hay número que aprobar, y sin reportDir propio la evidencia no es trazable ni sobrevive a la siguiente campaña: la sobrescribe en silencio quien corra el ámbito completo. Cualquier porcentaje de backend que se cite hoy para la 29 estaría inventado o calculado sobre un XML truncado y con el ámbito equivocado. CP7 del dictamen exige exactamente esto —la campaña con los ámbitos corregidos— y sigue sin marcar.

**Como se cierra:** Añadir en backend/build.gradle.kts la línea reportDir para additionalConnectorsOnly (reports/pitest-additional-connectors), meter ConnectorError*, ConnectorRateLimitedException* y ConnectorsDisabledException* en additionalConnectorsClasses, y correr la campaña completa sobre el árbol posterior a 6ec8e69c hasta obtener un mutations.xml no parcial que supere 0,80.

### B3

**Que:** @s38 (features/additional_connectors.feature:479-486) tiene seis cláusulas en el Then y una sola medida. En e2e/ hay 64 specs y ni uno navega a /conectores ni a /conectores/gitlab: los dos de conectores son github-connector.spec.mjs y github-connector-native-zoom.spec.mjs. El único oráculo son tres pruebas de foco en frontend/src/gitlab-connector.test.tsx:715, :731 y :743. Sin medir: 320/768/1280 px, texto al 200 %, zoom nativo 200 %, 44×44 px, recorrido de teclado con foco visible, axe en los tres anchos, y la última línea sobre los límites humanos, que no tiene ni fichero donde registrarse.

**Por que bloquea:** Es la condición C2 del propio dictamen y deja CP2 en 35 de 38, siendo CP2 checkpoint de cierre. El motivo que lo justificaba —no poder navegar a las rutas— desapareció al cerrar C1 con frontend/src/connectors-catalog-routing.test.tsx. Hoy es medible y sigue sin medirse: eso es deuda, no prudencia, y es exactamente el fallo que se le imputó a las features 27 y 30.

**Como se cierra:** Escribir e2e/additional-connectors-ux.spec.mjs y e2e/additional-connectors-native-zoom.spec.mjs siguiendo el patrón de los dos de github, cubriendo los tres anchos, texto al 200 %, zoom nativo, tamaño mínimo de diana, recorrido de teclado con foco visible y axe en ambas pantallas, y anotar los límites de la evidencia en progress/tdd_additional_connectors.md.

### B4

**Que:** SecretUndecipherableException no tiene manejador en GitlabConnectorController y encima deja un recibo huérfano. Los nueve @ExceptionHandler del controlador (backend/src/main/java/com/apptolast/organization/adapter/http/GitlabConnectorController.java:170-219) cubren MalformedBody, ConnectorsDisabled, ConnectionNotFound, ConnectionInvalid, GitlabUnavailable, ImportInProgress, ImportNotFound, RateLimited e ImportFailed; su gemelo GithubConnectorController.java:183-190 sí devuelve 503 CONNECTOR_KEY_MISMATCH. La excepción la lanza el caso de uso COMPARTIDO por los dos, ImportIssues.java:148, cuyo propio comentario en :143-144 declara que «el adaptador HTTP la traduce a 503 CONNECTOR_KEY_MISMATCH, así que una clave rotada da un fallo honesto y no un 500». En la ruta de GitLab cae en el catch-all de ApiErrors y sale como 500 INTERNAL_ERROR. Y como ImportIssues.execute inserta el recibo con receipts.begin (:75-82) ANTES de descifrar, mientras el try de run (:93-114) sólo captura IssueSourceException, ProjectCompletedException y StorageUnavailableException, la excepción escapa sin pasar por receipts.finish. Cero oráculo: existe GithubConnectorApiTest:709-720 y no hay equivalente en GitlabConnectorApiTest.

**Por que bloquea:** La línea 10 del .feature exige que TODOS los errores usen application/problem+json con código estable, y las líneas 5-6 declaran que los códigos y estados son los compartidos con la 27. Una rotación de APP_CONNECTOR_KEY —el evento operativo exacto para el que se inventó CONNECTOR_KEY_MISMATCH— da 503 con código en github y 500 sin código en gitlab, con el mismo caso de uso detrás. Y el recibo queda running con finished_at NULL, de modo que ImportGuard e importing() responden 409 IMPORT_IN_PROGRESS a toda importación, PUT y DELETE del propietario durante quince minutos: el sistema se autoinflige por un fallo de configuración el bloqueo que @s27 y @s28 gobiernan.

**Como se cierra:** Añadir a GitlabConnectorController el @ExceptionHandler de SecretUndecipherableException que devuelva 503 CONNECTOR_KEY_MISMATCH (copiando GithubConnectorController:183-190), capturarla también en ImportIssues.run para cerrar el recibo vía receipts.finish antes de propagarla, y escribir en GitlabConnectorApiTest el espejo de GithubConnectorApiTest:709-720 más una prueba que afirme que el recibo queda cerrado.

### B5

**Que:** El contrato fija una instancia autoalojada que producción rechaza, y la prueba que lleva la etiqueta afirma lo contrario del escenario. features/additional_connectors.feature:132 y :136 (@s11) fijan app.gitlab.api-base = https://gitlab.example.com/api/v4 y exigen «HTTP 200 con apiBase "https://gitlab.example.com/api/v4"», pero GitlabApiBase.of (backend/src/main/java/com/apptolast/organization/adapter/connectors/GitlabApiBase.java:36) sólo admite gitlab.com oficial o loopback, y GitlabApiBaseTest.java:31-49, método s11_refusesAnythingElseWhenTheBeanIsBuilt, incluye ese valor exacto entre los REFUSADOS bajo la etiqueta @s11. Se encadena con @s15: la línea :187 exige enlaces con externalId "gitlab.example.com:9001", y el único sitio donde producción compone ese valor es HttpGitlabIssueSource.java:105-107, URI.create(base.value()).getHost() + ":" + id, que con la única configuración aceptada en pruebas devuelve "127.0.0.1:9001" — y así lo afirma el único oráculo sobre producción, HttpGitlabIssueSourceTest.java:164. Todas las pruebas que usan el literal del contrato lo inyectan ellas mismas en un doble (ImportGitlabIssuesTest:66 y :262; GitlabConnectorPersistenceTest:179, :194, :225-235).

**Por que bloquea:** @s11 está declarado cerrado y su Then es inalcanzable en producción. Y el identificador de @s15 es un oráculo que no puede fallar: la prueba afirma el valor que ella misma acaba de fijar, mientras la función que lo calcula devuelve otra cosa. Nada ata el literal del contrato a la función. Es la premisa de la que cuelga toda la sección S1/C3 del dictamen y de progress/proposal_additional_connectors.md:15 y :81, que venden GitLab por «cubrir autoalojado». El git log del .feature (6be97616, 3a0d084b, f25e84c7) no contiene ninguna enmienda sobre este punto.

**Como se cierra:** Decidir con el propietario si producción admite instancias autoalojadas por lista blanca configurable o si no las admite, y en el segundo caso enmendar @s11 y @s15 en el .feature con contrafirma —el mismo procedimiento que se siguió con @s31— para que la base y los externalId del contrato sean los que la implementación produce de verdad.

### B6

**Que:** La frontera del recibo de importación no afirma seis de los siete valores que el contrato fija, y hay diez mutantes vivos que lo demuestran. En backend/build/reports/pitest-noche-cinco/mutations.xml, GitlabConnectorController$ImportResponse línea 304 tiene diez SURVIVED: los accesores id, projectId, status, skipped, failed, errorCode, startedAt y finishedAt se pueden sustituir por null/0/"" y truncated por true y por false, y la suite entera sigue verde. El oráculo de frontera, backend/src/test/java/com/apptolast/organization/adapter/GitlabConnectorApiTest.java:404-412, sólo afirma $.source, $.projectPath, $.created y el conjunto de claves con keysOf(body).containsExactlyInAnyOrder(RECEIPT_FIELDS) — y una clave con valor null sigue estando presente en el conjunto. El mismo XML deja además tres NO_COVERAGE en el controlador: :172 malformed, :215 importFailed y :228 rateLimitedProblem.

**Por que bloquea:** features/additional_connectors.feature:185 fija literalmente «source es gitlab, status completed, created 2, skipped 0, failed 0, truncated false y errorCode null». Seis de esos siete valores no los afirma nadie en el cuerpo que recibe el cliente. Lo mismo alcanza a @s16 (:197, la columna truncated) y a @s26 (:323, errorCode GITLAB_UNAVAILABLE). Son cláusulas del Then declaradas cerradas en el inventario y no medidas en la frontera, con la prueba de mutación aportando la demostración.

**Como se cierra:** Ampliar la aserción de GitlabConnectorApiTest:404-412 para afirmar el VALOR de cada campo del recibo ($.status, $.skipped, $.failed, $.truncated, $.errorCode, $.id, $.projectId, $.startedAt, $.finishedAt) además del conjunto de claves, y hacer lo propio en las pruebas de @s16 y @s26; cubrir de paso los tres manejadores sin cobertura del controlador.

### B7

**Que:** La guarda entera de decodeFailure en frontend/src/gitlab-connector-client.ts:109-117 tiene cero cobertura: la condición (!exact(value, ERROR_FIELDS) || !nonEmpty(value.code) || !instant(value.at)) y el throw new Error(INCOMPATIBLE) no los ejerce ninguna prueba. El motivo está a la vista: TODOS los fixtures del carril ponen lastError: null — gitlab-connector.test.tsx:18, :29 y :593, gitlab-connector-client.test.ts:28 y :39. Ninguna prueba pasa jamás un lastError no nulo por el decodificador. El único lastError que la pantalla llega a pintar lo fabrica el propio componente en gitlab-connector.tsx:232 a partir de un 409, no viene del servidor. En la campaña medida son 11 de los 17 mutantes NoCoverage.

**Por que bloquea:** Es el hueco que C4 decía cerrar y quedó cerrado sólo por un lado: la prueba nueva de gitlab-connector-client.test.ts:319-326 compara el TEXTO FUENTE del record Java ErrorResponse contra ERROR_FIELDS, pero el decodificador que rompería en ejecución nunca se ejecuta. Resultado: las cláusulas de @s8 («el cuerpo contiene exactamente ... lastError»), @s23 («devuelve status error y lastError { code, at }») y @s5 no tienen ningún oráculo en el cliente que las consume.

**Como se cierra:** Añadir en gitlab-connector-client.test.ts casos con lastError poblado —uno válido que decodifique, y los tres inválidos: campos de más o de menos, code vacío y at no instante— afirmando que los tres lanzan INCOMPATIBLE, y un fixture con lastError no nulo en gitlab-connector.test.tsx que llegue a la pantalla desde el servidor.

### B8

**Que:** @s31 (features/additional_connectors.feature:397-406) declara «recibe HTTP <http> con código <codigo>» y tres de sus ocho filas no afirman el código. backend/src/test/java/com/apptolast/organization/adapter/GitlabConnectorApiTest.java:488-504 (s31_withoutACsrfTokenNothingIsWritten) cubre las tres filas de CSRF_INVALID con sólo .andExpect(status().isForbidden()). El literal CSRF_INVALID lo afirman veinte ficheros de prueba del repositorio y ninguno de conectores. En el mismo fichero, :507-518 SÍ afirma $.code para UNTRUSTED_ORIGIN, que devuelve el MISMO 403: el estado por sí solo no discrimina entre las dos filas. Y :480-486 afirma isUnauthorized() sin el código UNAUTHENTICATED.

**Por que bloquea:** Es el defecto H2 que el juez marcó como bloqueante y que C6 cerró UNA CLASE MÁS ALLÁ: ConnectorCatalogApiTest.java:261 ya afirma $.code = UNAUTHENTICATED, pero la misma asimetría sigue viva en el fichero hermano. Y es irrecuperable por otra vía: los códigos son literales de cadena y PIT no muta literales, así que ninguna campaña de mutación lo delatará nunca. C6 se declara cerrada habiendo arreglado la mitad del hallazgo.

**Como se cierra:** Añadir .andExpect(jsonPath("$.code").value("CSRF_INVALID")) a las tres peticiones de s31_withoutACsrfTokenNothingIsWritten y .value("UNAUTHENTICATED") a las dos de s31_withoutASessionNoGitlabRouteAnswersAnything, en GitlabConnectorApiTest.

### B9

**Que:** El borrado de la conexión depende de un literal SQL que ninguna prueba ejercita con dos propietarios en la base. backend/src/main/java/com/apptolast/organization/adapter/persistence/PostgresGitlabConnectionStore.java:86-89 hace DELETE FROM gitlab_connections WHERE owner_id=?. En GitlabConnectorPersistenceTest nunca coexisten dos filas de conexión: s4_theConnectionOfOneOwnerIsInvisibleToAnother (:167-172) guarda OWNER y consulta OTHER —eso sí muerde el SELECT—, pero s14_deletingIsIdempotentAndLeavesTasksLinksAndReceiptsUntouched (:191-204) borra con una única fila presente. Lo mismo vale para el ON CONFLICT (owner_id) del upsert (:64-72).

**Por que bloquea:** Quitar el WHERE owner_id=? del DELETE borra la conexión cifrada de TODOS los propietarios y las dos pruebas siguen verdes: la primera devuelve 1 fila afectada y da true, la segunda 0 y da false. Es destrucción permanente de credenciales cruzando inquilinos, en la tabla que guarda los PAT, sin oráculo y fuera del alcance de la mutación porque PIT no muta literales de cadena. @s14 declara «no existe fila de conexión GitLab DEL PROPIETARIO» y esa mitad —la del propietario— es justo la que no se mide.

**Como se cierra:** Reescribir s14 para que guarde conexión de OWNER y de OTHER antes de borrar, y afirmar que tras connections.delete(OWNER) la de OTHER sigue presente y con su ciphertext intacto; añadir el caso simétrico para el upsert, comprobando que guardar de nuevo con OWNER no toca la fila de OTHER.

### B10

**Que:** @s32 (features/additional_connectors.feature:413) exige que «los logs del fallo contienen el código CONNECTION_INVALID y el identificador de correlación de la petición», y ese identificador no existe. El puerto backend/src/main/java/com/apptolast/organization/application/ConnectorAudit.java no admite ningún parámetro de correlación en ninguno de sus cuatro métodos (connected, connectionRefused, importFinished, importFailed); el único correlationId del repositorio lo acuña adapter/http/ApiErrors.java:127 dentro del manejador de 500, que es otro camino. El oráculo que se cita como cierre, GitlabTokenConfinementTest.java:131-135, afirma contains("CONNECTION_INVALID") y contains("owner=owner-1") — el propietario, que es el mismo en todas las peticiones de esa persona, no la petición.

**Por que bloquea:** La cláusula se declara cubierta sustituyendo el valor que el contrato pide por otro distinto que casualmente está en la misma línea de log. Ni el mecanismo existe ni el oráculo lo mide, así que no es un hueco de prueba sino de implementación. En la lente de seguridad no es cosmético: la correlación es lo que permite diagnosticar un fallo de credencial sin volcar el token, que es la razón de ser de @s32. progress/proposal_additional_connectors.md:63 también lo prometía.

**Como se cierra:** O añadir un parámetro de correlación a las firmas de ConnectorAudit, propagarlo desde el filtro HTTP hasta Slf4jConnectorAudit y afirmarlo en GitlabTokenConfinementTest, o enmendar @s32 con contrafirma del propietario para que la cláusula pida lo que el sistema registra de verdad.

### B11

**Que:** Supervivientes medidos sobre backend/src/main/java/com/apptolast/organization/adapter/connectors/HttpGitlabIssueSource.java y BoundedResponse.java que sostienen cláusulas del contrato, ninguno explicado. En pitest-github-connector/mutations.xml: HttpGitlabIssueSource :97, dos «negated conditional» SURVIVED sobre el mapeo description → completionCriterion de @s21 y @s15; :180, «replaced int return with 0» SURVIVED sobre return DEFAULT_RETRY_SECONDS, que es la fila «429 sin Retry-After → 60 segundos» de @s24 (feature:297); :185 boundary SURVIVED en atLeastOneSecond; y :181 dos NO_COVERAGE sobre la rama ratelimit-reset, que ninguna fila de @s24 pide y ninguna prueba ejerce. En BoundedResponse: :82 boundary SURVIVED sobre while ((read = body.read(chunk)) >= 0), que convertido en > 0 trunca el cuerpo en silencio ante una lectura de cero bytes; :90 negated conditional SURVIVED; :98 boundary SURVIVED; y :34 headers() en NO_COVERAGE. La bitácora de C3 (progress/cierre_condiciones_29.md:187-200) había previsto sólo tres supervivientes y ninguno coincide con :82 ni con :90.

**Por que bloquea:** El dictamen exige que cualquier superviviente no previsto se explique uno a uno y que un porcentaje agregado no valga como evidencia donde vive la seguridad. Aquí hay una previsión escrita, un resultado medido que no coincide y nadie los ha puesto uno al lado del otro. En concreto: el valor por defecto de reintento puede pasar a 0 en vez de 60 sin que nada se entere, el criterio de finalización puede invertirse sin que nada se entere, y el techo de 5 MiB puede truncar el cuerpo en silencio. Son cláusulas del Then declaradas cerradas sin oráculo que discrimine.

**Como se cierra:** Añadir en HttpGitlabIssueSourceTest los casos que discriminan —description ausente, nula y con texto; 429 sin Retry-After afirmando exactamente 60; el borde de atLeastOneSecond— y en el carril de BoundedResponse una lectura que devuelva cero bytes sin cerrar, y luego contrastar el XML resultante superviviente a superviviente contra la previsión de progress/cierre_condiciones_29.md, corrigiendo la previsión donde no coincida.

---

## Nota del orquestador: la campaña de PIT de esta feature murió

Primera campaña de mutación de backend de la 29, lanzada a las 12:47 del 10 de
septiembre. **Falló a los 34 min 41 s**, sin producir `mutations.xml`:

```
PIT >> SEVERE : Tests failing without mutation:
  ImportSocketTest.s31_exact32MiBPreviewReachesTheRealApiThroughNginx()
  ImportSocketTest.s31_chunkedBodiesKeepBothInclusiveBoundaries(...)  [#1]
PitHelpError: 2 tests did not pass without mutation when calculating line
coverage. Mutation testing requires a green suite.
```

**No es una regresión de esta feature.** `ImportSocketTest` es de la 24 y entra
porque el ámbito declara `targetTests = com.apptolast.organization.*`, o sea la
suite entera. Su prueba más lenta tardó **68,2 s** (32 MiB reales a través de
nginx) y en ese momento había **ocho carriles y 31 contenedores** compitiendo por
la máquina. Es la tercera campaña que se cae hoy: dos por memoria y ésta por
carga.

**Lo que enseña, y hay que respetarlo:** las campañas y los carriles no caben a
la vez en esta máquina. Las campañas se relanzan al final, con los carriles
drenados, y hasta entonces **ninguna cifra de esta feature es medida**.
