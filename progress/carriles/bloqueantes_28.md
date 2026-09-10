# Motivos bloqueantes del panel — feature 28-calendario-externo (features/external_calendar.feature)

**Cerrable: NO** — 10 bloqueantes, ya deduplicados y con los que no se sostenian descartados por el sintetizador.

Panel de tres jueces independientes (cobertura del contrato, seguridad y datos,
lo que se toco deprisa) mas sintesis, 10 de septiembre de 2026.

## Resumen del sintetizador

Los tres jueces coinciden en lo esencial y la comprobación directa lo confirma: la campaña PIT del backend existe en disco (531/579 = 91,71 %, medido por mí sobre backend/build/reports/pitest-external-calendar/mutations.xml) pero ninguna bitácora la cita; la de frontend NO se ha ejecutado nunca (frontend/reports/mutation-external-calendar/ no existe, mientras sí existen los seis informes de las features hermanas); y progress/current.md:91 y feature_list.json siguen diciendo "pendiente | pendiente" e "in_progress". Sobre eso, siete agujeros de oráculo confirmados uno a uno en el XML y en los tests. Quedan 10 bloqueantes tras fundir duplicados.

SE CAYERON CUATRO MOTIVOS al comprobarlos:
(1) El @s2 de persistencia ("la fila guardada no contiene abc123 y esa cadena nunca entra en el test", juez 1). FALSO: ExternalCalendarPersistenceTest.java:66-67 define work() como input("Trabajo", "https://feed.example.test/calendar/ical/abc123/basic.ics") y la línea 122 lo pasa a store().create(A, ..., work(), ...). La URL real SÍ entra por el puerto de entrada, así que doesNotContain("abc123") caería el día que una columna guardase la URL en claro. El oráculo es válido.
(2) "El conflicto normativo B3 sigue abierto en main" (juez 3, motivo 13). FALSO a día de hoy: project-spec.md:2492 ya dice "Se ancla en las dos, y se prueba el TLS" y deploy/EGRESS.md:6-23 ya dice "la aplicación la cierra" y se degrada a sí mismo a defensa en profundidad. La mitad de ratificación de C1 está cumplida; sólo sobrevive la mitad de la guarda (bloqueante 5).
(3) "Las nueve cláusulas de 400 ms no se miden en ninguna parte" (juez 1). PARCIALMENTE FALSO: @s40 (features/external_calendar.feature:556) dice literalmente "objetivo menor de 400 ms SIN PROMETER respuesta de red en ese plazo" —el contrato mismo renuncia a la cota dura—, y external-calendar.test.tsx:642-650 retiene la respuesta del POST y afirma que el anuncio aparece antes de que resuelva, que es la sustancia falsable (feedback optimista, no esperando a la red). Baja a menor.
(4) "El literal de error del frontend está descuadrado por el punto final" (juez 2). FALSO en efecto: external-calendar-api.ts:83 lanza "Respuesta de calendario externo inválida." y today-external-calendar.tsx:82 usa startsWith sin el punto, así que casa. Queda como fragilidad (dos literales sin constante compartida), no como defecto.

Además BAJO de bloqueante a serio la rama ::ffff: (juez 1): ejecuté el JDK y tanto InetAddress.getByName("::ffff:10.0.0.1") como getByAddress(byte[16] mapeado) devuelven Inet4Address, así que unmap() (PublicAddressPolicy.java:53-65) es código muerto —lo que explica el SURVIVED de :55 y los NO_COVERAGE de :60 y :63—, PERO la fila del contrato sí tiene oráculo falsable: AddressPolicyTest afirma allows()==false y cae si se quita 10.0.0.0/8 de la lista. Es código muerto que borrar (y una frase del javadoc y de project-spec.md que corregir), no una cláusula sin medir.

Serios/menores confirmados que no bloquean pero deben quedar registrados: ConnectorKeyRing.java:55 (negated conditional SURVIVED; el único oráculo, ExternalCalendarWiringTest.java:43, usa contains("APP_CONNECTOR_KEY") y "APP_CONNECTOR_KEY_PREVIOUS" contiene esa subcadena, así que el mensaje puede nombrar la variable equivocada durante una caída de arranque); ConnectorsGate.java:54 (setHeader del Cache-Control no-store SURVIVED y grep confirma que ExternalCalendarDisabledApiTest no lo afirma en ninguna línea); AesGcmSecretCipher.java:60 (frontera SECRET_UNREADABLE SURVIVED); IcsFeed.java:121 (la guarda i+1==raw.length() contra una SUMMARY terminada en barra invertida SURVIVED, y verifiqué que StringIndexOutOfBoundsException no cae en el catch de :53 —InvalidEvent|DateTimeException|ArithmeticException— ni en el de SyncExternalCalendar.java:94 —IcsFeedMalformedException—, así que saldría un 500 que viola feature:15); @s35 onlyIfStale (today-external-calendar.test.tsx:77-84 sólo compara método y URL, nunca el cuerpo, así que cambiar syncExternalCalendar(true) por false no rompe nada); el 503 CONNECTORS_DISABLED en GET /events cae en la rama "unreachable" de Hoy y pinta un aviso permanente que ninguna fila de @s36 cubre, más el catch pelado de today-external-calendar.tsx:57-62 que se traga el 401 del POST /sync; y el residuo de @s25 (ExternalCalendarPersistenceTest.java:622-632 sólo comprueba SELECT count(*) FROM outbox_events = 0 frente a "ninguna otra tabla, outbox ni historial cambia").

---

### B1

**Que:** La guillotina del plazo de 5 s no tiene ningún oráculo. Confirmado en el XML: HttpCalendarFeed.java:195 ('removed call to closeQuietly'), :196 ('Replaced long subtraction with addition', que programa el cierre a ~2x nanoTime, o sea nunca), :210 (negated conditional) y :223 ('removed call to InputStream::close') salen los cuatro SURVIVED. Leí la única prueba, HttpCalendarFeedTest.java:190-212: el proveedor escribe un byte cada 50 ms contra un presupuesto de 300 ms, así que body.read() siempre retorna y quien corta es el expired() por trozo de :202, no la guillotina. No existe ninguna prueba de un proveedor que envíe cabeceras y luego enmudezca, que es el caso que el javadoc :61-63 declara indispensable.

**Por que bloquea:** features/external_calendar.feature:13-14 promete 'ninguna descarga puede retener un hilo más de 5 s'. Se puede desmontar el ejecutor demonio entero y la suite sigue verde: media defensa contra el agotamiento del pool de Tomcat (la familia de la enmienda B1 de webhooks) está sin medir, y el dictamen la dio por cerrada en su §4.1.

**Como se cierra:** Añadir a HttpCalendarFeedTest un @Test con @Timeout cuyo handler sirva las cabeceras 200 text/calendar y después no escriba ni un byte ni cierre, afirmando FEED_UNREACHABLE con cota temporal medida (elapsed < 3x timeout), y añadir la fila correspondiente a los Examples de @s12. Debe matar los mutantes de :195, :196, :210 y :223.

### B2

**Que:** Los contadores de omisión no tienen oráculo desde el parser hasta el DTO. SURVIVED en el XML: SyncSummary.java:7 'replaced int return with 0' para skippedRecurring, skippedCancelled y skippedInvalid, más ExternalCalendarSubscription.java:10 para skippedInvalid. Grepé todas las aserciones fuera de IcsFeedTest y son CERO sin excepción: SyncExternalCalendarTest.java:173-175 (assertEquals(0,...) x3), ExternalCalendarPersistenceTest.java:113-115 (isZero() x3) y ExternalCalendarApiTest.java:150 (skippedInvalid = 0 sobre un mock de Mockito). Los dos únicos valores no nulos del árbol (:148 recurring=3, :149 cancelled=1) son justo los que matan los mutantes hermanos de ExternalCalendarSubscription, lo que confirma el mecanismo.

**Por que bloquea:** Los Then de @s17 (:263), @s18 (:277), @s20 (:306) y @s21 (:312) hablan de los contadores que publica la sincronización, no de los del parser. Hoy los cuatro accesores pueden devolver 0 siempre y la suite entera queda verde: el propietario vería '0 recurrentes, 0 cancelados, 0 inválidos' con un feed lleno de ellos y ninguna puerta se enteraría. Es un tramo completo de la cadena (parser → SyncSummary → fila → DTO) sin oráculo.

**Como se cierra:** Un caso en SyncExternalCalendarTest con un feed que traiga RRULE, un CANCELLED y un UID repetido, afirmando los tres contadores en valores distintos entre sí y distintos de cero; más la ida y vuelta en ExternalCalendarPersistenceTest y un skippedInvalid no nulo en el fixture synced() de ExternalCalendarApiTest.java:132-152. Deben morir los cuatro mutantes.

### B3

**Que:** No existe la evidencia de mutación que exigen C3, C4 y C5, y la cifra publicada de frontend es una previsión. Comprobado: frontend/reports/mutation-external-calendar/ NO existe (sí mutation-webhooks, -automations, -github-connector, -additional-connectors, -appearance, -ics-calendar); no existen progress/mutation_external_calendar_backend.md ni _frontend.md; progress/current.md:91 dice '28 calendario externo | APPROVED condicionado | pendiente | pendiente' y feature_list.json mantiene la 28 en in_progress. El único documento, progress/mutacion_external_calendar_frontend.md, publica '111 de 119 mueren' mientras el único artefacto máquina, progress/verificacion_mutantes_external_calendar.json, contiene 15 entradas, todas del racimo I y todas MUERE: cero supervivientes, el 87 % de lo publicado no es recomputable. Agravante confirmado: frontend/stryker.external-calendar.config.json declara los rangos src/App.tsx:65:16-87:42 y src/App.tsx:102:10-161:7, que abarcan la cadena ternaria entera hasta ': null' y todo el conmutador de rutas hasta el 404 —Calendario, Exportación, Apariencia, Hoy, Revisión semanal, Historial, Disponibilidad, Proyectos, IntegrationApi, ImportData y el resto—, frente a la forma estrecha de las hermanas (stryker.automations.config.json usa 61:12-62:32 y 94:10-95:40; ics-calendar usa 71:22-72:36 y 116:10-117:37). Y la guarda scripts/project.test.mjs:2144-2154 sólo comprueba el principio de cada rango y un ': null$' final, o sea que bendice el rango ancho en vez de detectarlo.

**Por que bloquea:** C3, C4 y C5 son bloqueantes en el propio veredicto y CLAUDE.md prohíbe marcar done sin mutación sobre el umbral. Una previsión de 88-94 % no es una medida: es el patrón que ha hundido a las features 27 y 30. Y si se lanza Stryker con los rangos actuales, la cifra la sostendrán en parte pruebas de otras diez features contra el break: 80.

**Como se cierra:** Estrechar los dos rangos de App.tsx a las dos o tres líneas de externalCalendar (forma de automations/ics-calendar), endurecer scripts/project.test.mjs para que afirme también el FINAL de cada rango, ejecutar la campaña de Stryker de la 28, y escribir progress/mutation_external_calendar_frontend.md y _backend.md con la puntuación medida y la lista nominal de supervivientes; regenerar verificacion_mutantes_external_calendar.json acumulando las tandas en vez de sobrescribir.

### B4

**Que:** La capa adapter.feed puntúa 70,2 % (33/47), diez puntos bajo el listón de '80 % en cada capa' que fijó el juez en su §6, y el global de 91,71 % lo esconde porque el ámbito incluye código ajeno. Medido por mí sobre el XML: backend/build.gradle.kts:479 mete adapter.config.ApplicationConfiguration* en externalCalendarClasses y aporta 113 de los 579 mutantes (19,5 %) con 112 muertos, casi todos por AutomationWiringTest y ApplicationWiringTest. Sin esa clase la cifra real de la 28 es 419/466 = 89,9 %. Por capas: adapter.feed 70,2 %, adapter.net 86,4 %, application 88,5 %, adapter.http 93,1 %, adapter.connectors 93,3 %, domain 93,4 %, adapter.persistence 94,9 %.

**Por que bloquea:** La capa que incumple es justo donde vive el control de seguridad nuevo del ciclo 4, y el juez la llamó 'la más importante'. La cifra que se presentaría como puerta está sostenida en un quinto por pruebas de otras features: apartar sólo el superviviente de ApplicationConfiguration, como pedía el veredicto, es exactamente al revés de lo que corrige el sesgo.

**Como se cierra:** Sacar ApplicationConfiguration* de externalCalendarClasses (o reportar sus 113 mutantes aparte, muertos incluidos), relanzar la campaña con los oráculos de los bloqueantes 1, 5 y 6 ya escritos, y exigir adapter.feed >= 80 % antes de firmar.

### B5

**Que:** La segunda mitad literal de la condición C1(a) sigue sin escribir y la propia compilación la hace inescribible. HttpCalendarFeed.java:148 —el return FeedFetch.failed(FEED_UNREACHABLE) del catch (IllegalArgumentException) que el comentario :146-147 promete para cuando el despliegue no admita la cabecera Host restringida— sale NO_COVERAGE en el XML, igual que los retornos de :119, :125 y :128. Causa verificada: backend/build.gradle.kts:37 (systemProperty en test) y :744 (jvmArgs en pitest) fijan -Djdk.httpclient.allowRestrictedHeaders=host en TODOS los JVM de prueba, de modo que la rama no se puede alcanzar en proceso. Ninguna prueba de HttpCalendarFeedTest menciona esa propiedad.

**Por que bloquea:** El juez condicionó el paso a done a 'añadir también la guarda de que, sin la propiedad restringida, se degrada a FEED_UNREACHABLE en vez de conectar sin Host'. Con la ratificación normativa ya hecha, esa exigencia queda activa y sin cumplir, y convive con su propio aviso (§4.2 punto 2) de que la propiedad se fija en un bloque estático de alcance JVM y llega tarde si otro componente crea antes un HttpClient: esa degradación silenciosa es literalmente la línea sin cobertura.

**Como se cierra:** Extraer la construcción de la petición a un método con costura (o a AnchoredConnection) y probar directamente que el rechazo de la cabecera Host devuelve FEED_UNREACHABLE, o añadir una tarea de test forkeada SIN jdk.httpclient.allowRestrictedHeaders que ejerza fetch() end to end. El mutante de :148 debe morir.

### B6

**Que:** Dos clases nombradas en el ámbito no reciben presión ninguna. backend/build.gradle.kts:451 nombra application.DeleteExternalCalendar* y la cadena no aparece ni una vez en mutations.xml (cero mutantes generados, sin página HTML propia en el informe); backend/build.gradle.kts:449 nombra application.ReadExternalCalendar* y su único mutante (ReadExternalCalendar.java:16, 'replaced return value with Optional.empty') sale NO_COVERAGE. Causa verificada: ExternalCalendarApiTest y ExternalCalendarDisabledApiTest son @WebMvcTest con @MockitoBean sobre los cinco casos de uso, y el listado de backend/src/test confirma que no existen ni ReadExternalCalendarTest ni DeleteExternalCalendarTest.

**Por que bloquea:** La condición C3 anula la campaña cuando una clase nombrada sale a cero, y ni Delete ni Read figuran en su lista nominal, así que la puerta tal como está redactada no lo habría detectado. Es el eco exacto del hallazgo 7 (adapter.crypto sin un mutante) que el juez declaró cerrado. Los Then de @s1 (:35), @s7 (:117-118), @s29 fila 2 (:390), @s30 (:400) y la fila 3 de @s33 dependen de ese camino de lectura y borrado, y el mutante que hace que GET devuelva siempre 'sin suscripción' no lo mata nadie.

**Como se cierra:** Escribir ReadExternalCalendarTest y DeleteExternalCalendarTest contra InMemoryExternalCalendarStore (lectura presente y ausente; borrado idempotente que arrastra la instantánea) y ampliar la lista nominal de la condición C3 con las dos clases.

### B7

**Que:** El filtro por propietario de la lectura de eventos no tiene ningún oráculo que pueda fallar. El WHERE vive en un literal SQL que PIT no muta (PostgresExternalCalendarStore.java:176-178) y los tres niveles son ciegos. Verifiqué el único sitio con dos propietarios en base real, ExternalCalendarPersistenceTest.java:561-590 (s33_ownersAreIsolated): el evento de B (2030-01-08T20:00Z) cae DENTRO de la ventana consultada [08T00:00, 09T00:00), la línea 586 afirma events(A,...).getFirst().startAt() SIN afirmar el tamaño —y si hubiese fuga, getFirst() seguiría siendo el de A porque 09:00Z ordena antes que 20:00Z—, y el único hasSize(1) es la línea 590, DESPUÉS de store().delete(B), cuando las filas de B ya no existen. Arriba, ExternalCalendarApiTest.java:266-280 afirma sobre lo que el propio mock devuelve, y ReadExternalCalendarEventsTest usa un fake indexado por propietario (cierto por construcción).

**Por que bloquea:** Es la fila 4 de @s33 ('persona-b recibe solo sus 2 eventos') declarada cubierta sobre una fuga de datos entre propietarios que ninguna prueba podría ver, y que la puerta de mutación no puede detectar porque el filtro es una cadena.

**Como se cierra:** Mover el hasSize(1) de ExternalCalendarPersistenceTest.java:590 a ANTES de store().delete(B) y añadir la aserción simétrica sobre B (events(B, ventana) devuelve exactamente su evento de 20:00Z y ninguno de A), de modo que quitar el WHERE owner_id rompa la prueba.

### B8

**Que:** El contrato se ha enmendado dos veces desde el ciclo 4 y ninguna tiene contrafirma. Verificado con git: baf5ab1f (09-09 20:16) introdujo features/external_calendar.feature:13-14 ('El plazo de 5 s es del intercambio completo… ninguna descarga puede retener un hilo más de 5 s') y la fila del goteo en @s12; 78dca3a6 (10-09 01:57) añadió la fila 'presenta un certificado que no es válido para su nombre | FEED_UNREACHABLE', y el fichero del juez se cerró en 9f3fe5c1 a las 01:14, o sea que el juez ni siquiera vio la segunda. El grep de 'intercambio completo' y 'gotea' sobre project-spec.md y progress/current.md no devuelve nada. Se suma el choque abierto que registra progress/current.md:465-472: 'Calendario externo' (feature 28) no aparece en la enmienda del orden de navegación ratificado, y ahí mismo se escribe 'Decisión del propietario' como pendiente.

**Por que bloquea:** CLAUDE.md hace de features/<name>.feature una puerta de aprobación humana y el juez lo elevó a condición C2 bloqueante. Agrava que la enmienda sin firmar es justamente la cláusula que el bloqueante 1 demuestra sin medir: contrato ampliado, documentación normativa muda y propietario sin firmar.

**Como se cierra:** Llevar al propietario en un solo bloque las dos enmiendas (plazo total del intercambio más fila del goteo; fila del certificado no válido para el nombre) y la ubicación de 'Calendario externo' en el orden de navegación; recoger la contrafirma en project-spec.md y anotarla en progress/current.md.

### B9

**Que:** El rechazo de parámetros de consulta desconocidos no está medido en dos de las cinco rutas: sobreviven ExternalCalendarController.java:82 (put) y :92 (delete), ambos 'removed call to ExternalCalendarController::rejectAnyParameter'. Las únicas pruebas que lo ejercen son ExternalCalendarApiTest.java:312 (GET) y :467 (POST /sync).

**Por que bloquea:** El título de @s10 es 'Aplicar seguridad HTTP común en las CINCO rutas' y el veredicto lo dio por cubierto citando ExternalCalendarApiTest. Dos quintas partes de una guarda HTTP pueden perderse sin que caiga una prueba: cláusula del Then declarada cubierta y no medida.

**Como se cierra:** Dos casos en ExternalCalendarApiTest que envíen PUT y DELETE con un parámetro de consulta desconocido y afirmen el rechazo, calcados del de :312. Deben matar los mutantes de :82 y :92.

### B10

**Que:** El aislamiento de la lectura concurrente no está medido: sobreviven PostgresExternalCalendarStore.java:263 ('removed call to TransactionTemplate::setReadOnly') y :264 ('removed call to setIsolationLevel'). El test con el que el juez cerró el hallazgo 19 (ExternalCalendarPersistenceTest.java:329-403) mide la atomicidad del ESCRITOR, que ya la garantiza la transacción externa; con READ COMMITTED por defecto el containsExactly("u1","u2") pasa igual.

**Por que bloquea:** La transacción de lectura hace varias consultas (find y events): sin REPEATABLE_READ pueden caer a ambos lados de un commit y devolver la lista parcial que features/external_calendar.feature:15 prohíbe ('nunca una lista parcial'). El veredicto §1 declaró el hallazgo 19 'sí en lo portante' apoyándose en un oráculo que la mutación demuestra ciego al nivel de aislamiento que el código fija a propósito.

**Como se cierra:** Una prueba de lectura concurrente que abra la transacción de lectura, deje que un escritor haga commit entre sus dos consultas y afirme que la vista devuelta es coherente (o todo lo viejo o todo lo nuevo); debe caer al quitar setIsolationLevel de :264.
