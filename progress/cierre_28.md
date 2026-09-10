# Veredicto de cierre — feature 28-external-calendar (features/external_calendar.feature)

**NO APROBADA** — 16 condiciones, 10 bloqueantes. 10 de septiembre de 2026.

Dos verificadores independientes (cierres declarados / lo que sigue bloqueando) más
síntesis. Sin ejecutar nada: la máquina estaba midiendo.

## Resumen

NO se aprueba. Verifiqué a mano las citas de los dos verificadores y descarto cinco motivos que no se sostienen; quedan nueve condiciones bloqueantes reales.

DESCARTADO (con prueba):
1) «backend/build/reports/pitest-external-calendar/ está VACÍO»: falso a las 16:26. El directorio tiene index.html, style.css, los OCHO paquetes del ámbito (incluido com.apptolast.organization.adapter.logging) y un mutations.xml que crecía de 385 KB a 412 KB entre las 16:23 y las 16:26. No es «no hay artefacto», es «la campaña sigue corriendo». No hace falta relanzar nada.
2) «La producción de la 28 dentro de Hoy no entra en NINGÚN ámbito de mutación»: falso. frontend/stryker.today.config.json muta src/today.tsx entero. Y «quitar el montaje entero no lo puntúa nadie» también es falso: frontend/src/today-external-section.test.tsx:65 renderiza <Today /> y exige la región «Calendario externo». Lo que SÍ queda en pie, y lo dejo como trabajo, es que el montaje no está en el ámbito de LA 28 y que ninguna prueba ejerce `revision` (grep de «revision» sobre las pruebas sólo da el prop por defecto de today-external-calendar.test.tsx:66).
3) «:203/:207 de HttpCalendarFeed son código muerto porque PIT los daría NO_COVERAGE»: no demostrado. PIT puntúa por bloque y esos `return` son su propio bloque, así que SURVIVED no descarta «rama no tomada». El argumento no arbitra nada; lo arbitra el XML que se está escribiendo.
4) «adapter.logging es una capa nueva sin nada que la sostenga»: existe backend/src/test/java/com/apptolast/organization/adapter/logging/ExternalCalendarAuditTest.java y el paquete ya aparece en el informe en curso. El riesgo de que baje del 80 % es real pero mucho menor de lo declarado; lo mide la misma campaña.
5) «La nota prestada de App.tsx tumba la puerta de frontend»: no la tumba. Sin esos 64 mutantes (55 muertos + 9 por plazo, 0 vivos) la cifra es 599/662 = 90,48 %, sigue sobre 80. Se arregla por honestidad del ámbito y por coherencia con lo que backend acaba de hacer con ApplicationConfiguration, no porque cambie el veredicto. Por eso lo dejo bloqueante como ARREGLO DE ÁMBITO, no como fallo de nota.

CONFIRMADO POR MÍ, uno a uno:
- C2/B8: grep de «28|calendario externo|intercambio completo|gotea|enmudece|certificado» sobre progress/decisiones_pendientes.md = CERO coincidencias; sus nueve encabezados son de las features 27, 29, 30 y 25. Las cuatro decisiones de la 28 no están donde el propietario mira. Bloqueante y no depende de ninguna campaña.
- Rangos anchos: frontend/src/App.tsx:65 es «: externalCalendar» y :87 es «: null», o sea que src/App.tsx:65:16-87:42 se traga la cadena ternaria entera (Calendario, Exportación, Apariencia, Hoy, Revisión semanal, Historial, Disponibilidad, Proyectos); y :102 es «) : externalCalendar && username ? (» con :161 al final del conmutador, hasta el 404. Las hermanas (automations 61:12-62:32, ics-calendar 71:22-72:36) usan la forma estrecha. Peor: scripts/project.test.mjs:2104-2112 fija esas cadenas con assert.deepEqual y :2151 exige /: null$/, o sea que la guarda del arnés OBLIGA al rango ancho.
- @s35 sin oráculo: frontend/src/today-external-calendar.tsx:59 es «await syncExternalCalendar(true, signal)» y el doble de fetch de today-external-calendar.test.tsx:57 sólo apila `${options.method ?? "GET"} ${url}`, nunca el cuerpo. El acta lo mide: fila «today-external-calendar.tsx | 59 | BooleanLiteral | Survived | false». Cambiar true por false deja la suite verde y convierte cada carga de Hoy en una descarga forzada del feed ajeno, justo lo contrario de la regla de frescura.
- C5 sin cumplir y confesado: progress/mutacion_external_calendar_frontend_medida.md:27-29 escribe «Lo que falta -y es el trabajo siguiente- es el veredicto escrito de cada uno», y no existe ningún acta de backend (ls progress/ | grep external_calendar no da ninguna). Aun así progress/current.md:198 afirma «La 28 tiene ya las dos puertas de mutación» mientras :34 del mismo fichero dice «midiéndose».
- Procedencia rota: `git cat-file -t ec0a3b8` responde «Not a valid object name» (5b019343 y 0bf68914 sí existen). El acta que presume de «calculada, no copiada» cita una base que no se puede recomputar.
- Supervivientes reales verificados en el fuente: external-calendar.tsx:291 (onChange de Etiqueta, ArrowFunction Survived) frente a :311 (el de Dirección, que no está en la lista y por tanto muere) — asimetría real, no limitación del entorno; :236 ConditionalExpression Survived con true y :237 NoCoverage (el 404 EXTERNAL_CALENDAR_NOT_CONFIGURED de feature:530 no se distingue de ningún otro error); :114/:118/:122 de forget() ArrayDeclaration Survived (feature:542); today-external-calendar.tsx:27 hour12 y :105 el texto del aviso de @s36 fila 3.
- Coordenadas: los jvmArgs de pitest están en backend/build.gradle.kts:769 y la systemProperty en :37 — ni el :744 del panel ni el :764 del carril aciertan. La guarda de Stryker empieza en scripts/project.test.mjs:2122, no en 2144.
- Lo que está BIEN cerrado y no vuelvo a abrir: B1 (HttpCalendarFeedTest s12 con @Timeout(20) y el handler que hace flush y no cierra), B2 en los tres tramos, B6, B7, B9, B10, la mitad TLS de C1(a), ApplicationConfiguration fuera del ámbito con el motivo escrito en backend/build.gradle.kts:494-499, los dos arreglos de producto de frontend (external-calendar.tsx:130-137 y :164), y feature_list.json sin marcar la 28 como done. El matiz de B6 es correcto y lo recojo: DeleteExternalCalendar no generará mutantes nunca (su único cuerpo es una llamada no-void descartada), pero NO viola C3 porque esa clase no está en la lista obligatoria del juez (judge:229-247).

Aviso para cuando termine la campaña en curso: si adapter.feed sale por debajo del 80 %, el trabajo que lo sube ya está identificado y presupuestado en la bitácora (los dos mutantes de HttpCalendarFeed:159, interrupción del hilo durante un fetch); lo dejo como condición no bloqueante precisamente porque sólo se activa con ese resultado.

---

## Condiciones

| # | Condición | Bloq. | De quién | Min. |
|---|---|---|---|---|
| 1 | C2 (a) — las cuatro decisiones de la 28 no están encoladas donde el propietario lee | **SÍ** | orquestador | 30 |
| 2 | C2 (b) — contrafirma humana de la ampliación de contrato del ciclo 4 | **SÍ** | propietario | 20 |
| 3 | @s35 «POST /sync con onlyIfStale true» no tiene oráculo que pueda fallar (superviviente medido today-external-calendar.tsx:59) | **SÍ** | carril | 20 |
| 4 | El ámbito de Stryker de la 28 puntúa código de otras diez features y la guarda del arnés lo fija | **SÍ** | orquestador | 40 |
| 5 | Verificar que la guarda del arnés queda verde con los rangos estrechados (precondición C4 antes de lanzar) | **SÍ** | campana | 10 |
| 6 | Remedir el frontend con el ámbito corregido: la cifra publicada no es la de la feature | **SÍ** | campana | 35 |
| 7 | C3 — la campaña de PIT del backend con el ámbito ya corregido, 80 % por capa | **SÍ** | campana | 40 |
| 8 | C5 (frontend) — falta el veredicto escrito de cada superviviente, y el acta lo confiesa | **SÍ** | carril | 150 |
| 9 | C5 (backend) — no existe acta de mutación de backend, ni siquiera el fichero | **SÍ** | carril | 60 |
| 10 | Los seis oráculos nuevos no se han visto en verde en main | **SÍ** | campana | 40 |
| 11 | progress/current.md publica como cerrada la puerta de backend que se está desmontando a propósito | no | orquestador | 10 |
| 12 | El acta de frontend cita como base un commit que no existe | no | orquestador | 10 |
| 13 | Coordenadas publicadas que no recomputan y una etiqueta «MEDIDO» sin artefacto | no | orquestador | 15 |
| 14 | B5 — la mitad literal de C1(a): la rama de la cabecera Host restringida es inalcanzable en los dos JVM de prueba | no | carril | 45 |
| 15 | Registrar la excepción de DeleteExternalCalendar antes de que la lea el próximo juez | no | orquestador | 10 |
| 16 | Reserva por si adapter.feed no llega al 80 % en la campaña que está corriendo | no | carril | 35 |

## El trabajo, una por una

### 1. C2 (a) — las cuatro decisiones de la 28 no están encoladas donde el propietario lee

- **Bloqueante:** sí · **De:** orquestador · **Estimación:** 30 min

Añadir a progress/decisiones_pendientes.md cuatro entradas con la forma de las nueve que ya tiene: la enmienda baf5ab1f (plazo del intercambio completo más la fila del goteo), la enmienda 78dca3a6 (fila del certificado que no es válido para el nombre), la fila propuesta del proveedor que enmudece tras las cabeceras, y el sitio de «Calendario externo» en el orden de navegación de workspace.tsx. Hoy grep de «28|calendario externo|intercambio completo|gotea|enmudece|certificado» sobre ese fichero da cero.

### 2. C2 (b) — contrafirma humana de la ampliación de contrato del ciclo 4

- **Bloqueante:** sí · **De:** propietario · **Estimación:** 20 min

El propietario ratifica o rechaza, una por una, las cuatro entradas anteriores sobre features/external_calendar.feature:13-14 y la fila de @s12. Es puerta humana del juez (judge:174-181) y la única condición del veredicto que no depende de ninguna campaña.

### 3. @s35 «POST /sync con onlyIfStale true» no tiene oráculo que pueda fallar (superviviente medido today-external-calendar.tsx:59)

- **Bloqueante:** sí · **De:** carril · **Estimación:** 20 min

En frontend/src/today-external-calendar.test.tsx, que el doble de fetch guarde también el cuerpo (hoy :57 sólo apila método y url) y que la prueba de orden afirme {onlyIfStale: true} en el POST. Acreditar el rojo cambiando la producción a syncExternalCalendar(false, signal): la prueba tiene que caer.

### 4. El ámbito de Stryker de la 28 puntúa código de otras diez features y la guarda del arnés lo fija

- **Bloqueante:** sí · **De:** orquestador · **Estimación:** 40 min

Estrechar en frontend/stryker.external-calendar.config.json los dos rangos de App.tsx a la forma de las hermanas (src/App.tsx:65:16-66:38 para la rama de section y el tramo de externalCalendar en la de render, en vez de 65:16-87:42 y 102:10-161:7); añadir al mutate el montaje de frontend/src/today.tsx:226-231, que es donde vive la mitad de @s35 y @s36 y ninguna campaña de la 28 lo toca; y reescribir la guarda de scripts/project.test.mjs (el assert.deepEqual de 2104-2112 y el assert.match(section, /: null$/) de :2151, que hoy obliga al rango ancho) para que afirme el FINAL del tramo de externalCalendar y no el de la cadena entera.

### 5. Verificar que la guarda del arnés queda verde con los rangos estrechados (precondición C4 antes de lanzar)

- **Bloqueante:** sí · **De:** campana · **Estimación:** 10 min

node --test --test-name-pattern "external calendar Stryker" scripts/project.test.mjs

### 6. Remedir el frontend con el ámbito corregido: la cifra publicada no es la de la feature

- **Bloqueante:** sí · **De:** campana · **Estimación:** 35 min

cd frontend && npx stryker run stryker.external-calendar.config.json. La medida de hoy (663/726 = 91,32 %) incluye 64 mutantes de App.tsx al 100 % muertos por pruebas de Hoy, Proyectos y Apariencia; sin ellos son 599/662 = 90,48 %. La nueva cifra debe salir de mutation.json, con mutantes mayores que cero en los cuatro ficheros que exige C4 y en cada rango, y sin ningún mutante sobre una sentencia import.

### 7. C3 — la campaña de PIT del backend con el ámbito ya corregido, 80 % por capa

- **Bloqueante:** sí · **De:** campana · **Estimación:** 40 min

Dejar terminar la campaña que corre AHORA (backend/build/reports/pitest-external-calendar/mutations.xml estaba creciendo a las 16:26) y recomputar por paquete sobre mutations.xml: global, adapter.feed (la proyección de 40/47 = 85,1 % es proyección) y la capa adapter.logging, que aparece por primera vez tras el arreglo de avoidCallsTo en backend/build.gradle.kts:779-792. En el mismo XML se arbitra la disputa de HttpCalendarFeed.java:203 y :207 leyendo su <status>, y se comprueba que los mutantes de :195, :196, :210 y :223 no salen TIMED_OUT por culpa de la prueba nueva del proveedor que enmudece. NO hace falta relanzar nada.

### 8. C5 (frontend) — falta el veredicto escrito de cada superviviente, y el acta lo confiesa

- **Bloqueante:** sí · **De:** carril · **Estimación:** 150 min

Sobre la lista de la medida NUEVA, escribir por cada superviviente o la prueba que lo mata o el motivo concreto de equivalencia, en progress/mutacion_external_calendar_frontend.md. Los que ya sé que son hueco real y hay que matar, no justificar: external-calendar.tsx:291 (la etiqueta que teclea el usuario puede no llegar a la petición; su gemelo :311 sí muere), :236 y :237 (ninguna prueba lleva synchronise() a un error distinto del 404, así que el 404 de feature:530 no se distingue de nada), :114/:118/:122 (forget() puede dejar de vaciar la lista, feature:542), today-external-calendar.tsx:105 (el aviso de @s36 fila 3 puede quedarse en blanco; su hermano :104 sí muere) y :27 (hour12, que las dos aserciones de subcadena no fijan). Mirar también por qué los dos mutantes de :76 dan RuntimeError.

### 9. C5 (backend) — no existe acta de mutación de backend, ni siquiera el fichero

- **Bloqueante:** sí · **De:** carril · **Estimación:** 60 min

Escribir progress/mutacion_external_calendar_backend_medida.md con la cifra recomputada del XML (no copiada del HTML), la tabla por capa y la lista NOMINAL de supervivientes con su veredicto uno a uno, al modo de mutacion_webhooks_backend_medida.md. Hoy ls progress/ no devuelve ningún fichero de backend para esta feature.

### 10. Los seis oráculos nuevos no se han visto en verde en main

- **Bloqueante:** sí · **De:** campana · **Estimación:** 40 min

cd backend && gradlew.bat test --tests "*HttpCalendarFeedTest" --tests "*SyncExternalCalendarTest" --tests "*ExternalCalendarApiTest" --tests "*ReadExternalCalendarTest" --tests "*DeleteExternalCalendarTest" --tests "*ExternalCalendarPersistenceTest". El carril sólo los ejecutó clase a clase en su worktree; ExternalCalendarPersistenceTest levanta PostgreSQL con Testcontainers, así que hay que ir por turnos y no a la vez que una campaña.

### 11. progress/current.md publica como cerrada la puerta de backend que se está desmontando a propósito

- **Bloqueante:** no · **De:** orquestador · **Estimación:** 10 min

Fechar o tachar el bloque progress/current.md:190-201: la línea :194 dice «28 calendario externo | 91,71 % OK | 91,32 % OK» y la :198 remata «La 28 tiene ya las dos puertas de mutación», mientras la :34 del mismo fichero dice «midiéndose». El 91,71 % se midió con ApplicationConfiguration dentro del ámbito y ese ámbito ya no existe (backend/build.gradle.kts:494-499).

### 12. El acta de frontend cita como base un commit que no existe

- **Bloqueante:** no · **De:** orquestador · **Estimación:** 10 min

Corregir progress/mutacion_external_calendar_frontend_medida.md:13: «Sobre main en ec0a3b8» no recomputa (git cat-file -t ec0a3b8 = Not a valid object name). Poner el commit real de la medida y decir cómo se comprobó (el mutation.json lleva el fuente embebido y el último commit que toca esos ficheros es 0bf68914, de las 12:49).

### 13. Coordenadas publicadas que no recomputan y una etiqueta «MEDIDO» sin artefacto

- **Bloqueante:** no · **De:** orquestador · **Estimación:** 15 min

En progress/bloqueantes_external_calendar.md y progress/carriles/bloqueantes_28.md: los jvmArgs de pitest están en backend/build.gradle.kts:769 y la systemProperty de test en :37 (no en :744 ni en :764); la guarda de rangos de Stryker empieza en scripts/project.test.mjs:2122 y el deepEqual que fija el rango ancho en :2104-2112 (no en :2144-2154). Y quitar la etiqueta «MEDIDO» de B5 o adjuntar el artefacto: HostProbe.java y HostProbe2.java no están en el árbol, así que ese transcript no lo puede recomputar nadie. La CONCLUSIÓN de B5 sí es buena y está corroborada aparte por AnchoredConnectionTest.java:95-115.

### 14. B5 — la mitad literal de C1(a): la rama de la cabecera Host restringida es inalcanzable en los dos JVM de prueba

- **Bloqueante:** no · **De:** carril · **Estimación:** 45 min

Añadir a backend/build.gradle.kts la tarea forkeada unrestrictedHostTest SIN -Djdk.httpclient.allowRestrictedHeaders (el snippet ya está escrito en progress/bloqueantes_external_calendar.md:454-472; hoy grep de «unrestrictedHostTest» y de «registering(Test::class)» sale vacío) y su clase de prueba, y ver morir el mutante de HttpCalendarFeed.java:148. No bloqueante: la mitad TLS de C1(a) está cumplida (HttpCalendarFeedTest.java:577, :600 y :615) y el otro disparador del mismo catch ya tiene oráculo en :501.

### 15. Registrar la excepción de DeleteExternalCalendar antes de que la lea el próximo juez

- **Bloqueante:** no · **De:** orquestador · **Estimación:** 10 min

Anotar en el acta de backend que DeleteExternalCalendar no generará mutantes nunca: ExternalCalendarStore.java:33 declara «boolean delete(String ownerId)» y el único cuerpo de execute es esa llamada no-void con el valor descartado dentro de un método void, que ningún mutador por defecto de PIT toca. NO viola C3: esa clase no está en la lista obligatoria del juez (judge_external_calendar_cierre.md:229-247).

### 16. Reserva por si adapter.feed no llega al 80 % en la campaña que está corriendo

- **Bloqueante:** no · **De:** carril · **Estimación:** 35 min

Sólo si el XML lo pide: cerrar los dos mutantes de HttpCalendarFeed.java:159 (negated conditional y removed call to Thread::interrupt) con una prueba que interrumpa el hilo durante un fetch y afirme que la marca de interrupción sobrevive. La propia bitácora lo declara cerrable y lo presupuesta en 30-40 min.

