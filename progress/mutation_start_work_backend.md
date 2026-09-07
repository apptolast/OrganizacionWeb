# Mutación backend14

**Veredicto de umbral: PASS.** Score bruto estricto 340/347 = 97,9827%, umbral 80. EXIT0 confirmado por coordinador0b0152. Hay5 SURVIVED,1 NO_COVERAGE y1 TIMED_OUT; no se excluye ni reclasifica ninguno. No declara cierre de feature14.

Producto revisado por paquetes: review_start_work_backend.md, review_start_work_http.md, review_start_work_wiring.md y review_start_work_publisher.md. Selector aprobado en review_start_work_harness.md. Comando node .harness/harness.mjs mutate start_work_session-backend, arranque f0b0b1, sesión2804; log mutation_start_work_backend.log. Snapshot antes de fuentes, pruebas, Gradle y dispatcher en mutation_start_work_backend_before.json. Quince unidades detectadas en pre-scan fe5116; no implica quince casos ni resultado.

Init completo previo terminó EXIT0 (438035): Spotless/lint y1751Java/1571frontend/29scripts verdes. XML Java independiente d0b518:73suites, cero fallos, errores u omitidos. Backend terminó a01:38:55 y frontend aaprox01:39:49; PIT arrancó a01:40:25. Fuentes y pruebas Java quedan congeladas durante campaña. Los refuerzos frontend posteriores son disjuntos de PIT.

Se conserva umbral 80, cuatro workers, todos los candidatos JUnit y clases14 más publicación/configuración compartidas. Se registrarán todos los estados, supervivientes, fallos de ejecución y hashes finales; no se atribuye Killed a errores de herramienta. No hay declaración de cierre de14.

Seguimiento delegado: write_stdin2804 no es accesible desde el agente de seguimiento (Unknown process id); el coordinador conserva el proceso original y comunicará su EXIT. No se lanzó otra campaña. Rol/docs y umbral0.8 leídos c04ced/3d4d20; before contiene288archivos.

Cobertura calculada en227segundos, a01:44:12;15unidades de mutación creadas a01:44:13 (log27d431). Cinco pruebas superan2000ms, mayor3022ms. A01:45:45 aparece «Minion exited abnormally due to TIMED_OUT». Es un aviso parcial de ejecución: no se atribuye aún a una identidad ni se cuenta como Killed. Campaña sin intervención, resultado final pendiente.

Diagnóstico puntual autorizado por coordinador: una muestra jcmd Thread.print del minion32344, evidencia90d11f/6ace36, guardada en mutation_start_work_backend_minion_threads.txt. El ejecutable no estaba en PATH (c86307, sin attach); se usó el jcmd del mismo JDK del proceso. Main esperaba FutureTask en MutationTimeoutDecorator; mutationTestThread#2092 tenía2,95s de vida y esperaba readiness de contenedor mediante TestcontainersExtension.beforeAll → GenericContainer.start → LogMessageWaitStrategy/WaitingConsumer. No había todavía método de aplicación/test en ese stack. Es compatible con arranques repetidos durante ejecución, no evidencia de bloqueo fijo. No se detuvo ni modificó proceso alguno y no se repitió la muestra.

Aprendizaje para campañas futuras, sin modificar ésta: al quedar un único minion con tres slots libres, podría evaluarse particionar unidades grandes mediante mutationUnitSize. Es una hipótesis de reparto de carga, no un speedup medido ni una recomendación de cambiar el freeze actual. No se investigaron dependencias ni se cambió configuración.


## Resultado final e integridad

Después del freeze, root trasladó únicamente el dump de diagnóstico a `backend/build/reports/pitest-start-work-session/minion-threads-diagnostic.txt`, ruta ignorada de artefactos. Su interpretación queda documentada arriba; no se incorpora un volcado de depuración al repositorio. Verificación independiente33e759 confirma los288 hashes antes/después idénticos.

XML final c2ca59/e27e0d:347 mutantes,340 KILLED,5 SURVIVED,1 NO_COVERAGE y1 TIMED_OUT. NON_VIABLE, MEMORY_ERROR, RUN_ERROR, NOT_STARTED y STARTED:0. PIT imprime «Killed341» porque incorpora el timeout en su agregado detectado; ese agregado no se presenta como341 KILLED efectivos ni se usa para ocultar la incidencia. El score estricto 340/347 sigue superando 80 sin ajustes. Gradle27m12s; PIT26m56s, de los que23m09s son análisis de mutación. Cobertura530/536líneas,273 clases de prueba examinadas y2371 ejecuciones de prueba (no2371 tests distintos).

Inventario íntegro de347 identidades: mutation_start_work_backend_inventory.json, con clase/método/descriptor/línea/mutador/índices/bloques/estado y prueba informada. Snapshot posterior mutation_start_work_backend_after.json:288/288 hashes iguales, sin archivos backend/src añadidos ni ausentes; verificación e27e0d. Fuentes/pruebas/config no cambiaron durante la campaña. El diagnóstico jcmd fue una lectura autorizada, no una alteración de ejecución.

Informes originales conservados:
- backend/build/reports/pitest-start-work-session/mutations.xml, SHA256167AEB9D9461E8E5AECABC6D73D56105FCE892B539BB0670397BEF03DACF445F.
- backend/build/reports/pitest-start-work-session/index.html, SHA25627DFF88E6215FD22008DC354600BB6964DC4C12D4F2DBB5AD1F3FD163568CB56.
- progress/mutation_start_work_backend.log, ejecución original por arnés; EXIT0 comunicado0b0152.

## Siete residuales, sin equivalencias ni kills inventados

| Estado | Identidad de origen | Mutación y dictamen |
| --- | --- | --- |
| NO_COVERAGE | OutboxMessage.validationCode:157, EmptyObjectReturnValsMutator | Devuelve cadena vacía en catch DateTimeException. Rama alcanzable por plannedEndAt con forma UTC válida pero calendario imposible (p. ej.2026-02-30T10:25:00Z). Falta un payload incompatible que exija blocked/INVALID_EVENT sin envío. No equivalente. |
| SURVIVED | WorkSessionStarted.ownerId:6, EmptyObjectReturnValsMutator | Retorno vacío. Oráculo persistido debe comparar ownerId literal de la intención, sin construir otro WorkSessionStarted como esperado. |
| SURVIVED | WorkSessionStarted.plannedMinutes:6, PrimitiveReturnsMutator | Retorno0. Mismo refuerzo del JSON durable con25 explícito. |
| SURVIVED | WorkSessionStarted.projectId:6, NullReturnValsMutator | Retorno null. Comparar UUID de proyecto de fixture, independiente de accessor mutado. |
| SURVIVED | WorkSessionStarted.taskId:6, NullReturnValsMutator | Retorno null. Comparar UUID de tarea de fixture, independiente de accessor mutado. |
| SURVIVED | WorkSessionStarted.zoneId:6, EmptyObjectReturnValsMutator | Retorno vacío. Comparar zona esperada explícita de fixture. |
| TIMED_OUT | RabbitBrokerPublisher.<init>:27, VoidMethodCallMutator, índice82/bloque13 | Elimina ConnectionFactory.useNio. XML detected=true pero numberOfTestsRun=0 y killingTest vacío: no identifica qué test agotó tiempo. Mantener TIMED_OUT. |

Los cinco accessors sobrevivientes tienen30 pruebas ejecutadas cada uno según XML. La causa del hueco está contrastada con fuentes: WorkSessionPersistenceTest compara payload contra json.valueToTree(new WorkSessionStarted(...)), así que la mutación puede alterar también el esperado; el test de wiring compara aggregateId/occurredAt/plannedEndAt, no esos cinco campos. El smoke real sí compara sus valores con recibo/contexto, pero no forma parte de PIT. Es un acoplamiento del oráculo, no un defecto observado de producción ni una razón para pruebas reflexivas de getters. Un único refuerzo de JSON durable con once valores independientes puede comprobar este comportamiento.

Sobre el timeout: useNio activa el transporte al que se aplica NioParams.writeEnqueuingTimeoutInMs(1000). Su eliminación cambia esa configuración; es plausible una espera de transporte distinta, pero el XML no permite atribuir causalidad a un test específico. RabbitBrokerFailuresTest.s5_transportAndCleanupHaveFiniteBoundsWithoutRecovery ya verifica useNio; no se afirma que lo haya matado en esta campaña. s5_tcpPeerThatNeverHandshakesIsBounded comprueba límite real de handshake. No segunda ejecución ni reclasificación por inferencia.

Dictamen: puerta cuantitativa superada con siete residuales documentados. El coordinador decidirá refuerzos acotados y revisión posterior; esta medición no equivale al cierre de14. Informe original congelado antes de cualquier TDD de seguimiento.

