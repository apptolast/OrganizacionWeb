# TDD backend18 — historial propio

## 1. Nominal compilable @s1

ReadHistoryTest.s1_returnsTheOriginalSessionStartWithCurrentContextLabels exige consulta por owner, filtros y cursor completos, con SessionStart original conservado y etiquetas actuales. RED de compilación real ff11af por tipos/puertos ausentes; GREEN1f83d9 con caso de uso y frontera real. No PG/HTTP aún; mock sólo del puerto de lectura. HistoryEntry<D> conserva el tipo del detalle existente al construir cada entrada y la página heterogénea usa HistoryEntry<?>; no DTO HTTP en dominio, ObjectMapper ni dependencia de infraestructura.

Formato: 2e010d ignoró glob relativo;2a400e con rutas absolutas sólo informó IS DIRTY por el hook IDE, no aplicó. SpotlessJavaApply real84183d aplicó formato y foco ReadHistoryTest **1/1 GREEN93bad2** después. No se presenta el hook anterior como formato aplicado. No test global/init repetido.

Primer bundle compilable congelado en history_first_bundle.json: ocho fuentes application y un test. ReadHistoryUseCase.list(owner,HistoryFilters,HistoryCursor) devuelve HistoryPage; Queries.list misma entrada devuelve List<HistoryEntry<?>>. Filters(category,projectId,taskId,from,to); Cursor(owner,filters,upper,after); Position(occurredAt,type,id). El HTTP de C valida sintaxis; propiedad y luego semántica del cursor se completarán en adaptador PG. Primer caso terminal únicamente: paginación21 y cincofuentes/validación/RR/errores pendientes, no feature completa ni stubs PG.

Fuentes del bundle quietas para revisión/commit selectivo root. No Git mutado por autor, migración ni consumer. C avisado de firmas reales; no debe copiar archivos en vuelo.

## 2–5. Nominal PG, owner y lookahead

- @s1 PG HistoryQueriesPersistenceTest.s1_readsTheOriginalStartWithCurrentLabelsFromPostgres: RED853637 adapter ausente; GREEN7e7475 con consulta real PostgreSQL y comparación de fila original/outbox vacío. Primer adapter sólo sesión; no afirma cincofuentes ni filtros ya implementados.
- @s3 PG.s3_anotherOwnerCannotReadASessionStart: REDdc49fc devolvía fila ajena; GREEN4ee979 añade owner tanto en sesión como en proyecto y join task/project coherente.
- @s6 ReadHistoryTest.s6_lookaheadCreatesACursorFromTheLastDeliveredFact: incidente de captura genérica en fixture2a5d96 corregido antes de RED funcional4011bc (devolvía21); GREENaab706 limita20 y crea cursor último entregado/primero inicial.
- @s14 ReadHistoryTest.s14_continuationKeepsTheInitialUpperInsteadOfTheCurrentFirstRow: RED2199d4 reemplazaba upper por primera fila actual; GREEN8598dd conserva upper recibido.

Root aprobó y versionó primer bundle2e60131, liberado. Sin cambios de firmas. V14 mostraba M pero git diff específico ab0c7e no contiene cambios; no fue editada por este autor.

## 6–9. Fuentes durables restantes

- @s1 transición de tarea: RED b924a7 por ausencia de fuente; GREEN 42bc95 (tres PG). Incidente de compilación db25aa por inferencia de Class en el mapper corregido separadamente.
- @s1 pausa durable junto a inicio: incidente genérico f09744 del fixture corregido; RED af5dbd devolvía sólo inicio. GREEN verificado en XML 495baf, un caso sin fallos, tras incorporar receipt durable sin derivarlo de la proyección.
- @s1 planificación con destino futuro: RED 91d67a devolvía vacío; GREEN 6a441e devuelve createdAt y PlannedBlock original, sin usar startAt como fecha del hecho.
- @s1 cancelación: RED 6542b0 faltaba recibo; GREEN 49622d conserva ambas entradas y lee BlockChangeReceipt original. Cinco fuentes nominales disponibles; filtros, orden SQL y transacción aún pendientes. No se atribuye cobertura completa de las variantes de @s1.

## 10–14. Orden, lookahead SQL y categorías

- @s6 PG.s6_databaseReturnsOnlyTwentyOneFactsInDescendingTime: RED 82d533, GREEN bad18f. Veintidós transiciones coherentes; SQL entrega sólo21 en orden temporal inverso.
- @s13 y @s4 PG.s13_equalTimesUseSourceRankThenUnsignedUuidDescending: RED 1a53d5, GREEN aabda4. Empates reales, UUID extremos y misma UUID en familias distintas; orden nativo PostgreSQL, sin comparación UUID firmada en Java.
- Refactor de variable SQL en verde: regresión de los ocho PG y tres application GREEN d6a170.
- @s7 categoría sessions: RED 25f5fc, GREEN 60bfb7; planning: RED 1a8a08, GREEN d61cd0; task-status: RED 48162b, GREEN 4fba3a. Cada caso conserva entrada positiva y excluye otra familia.

## 15–23. Contexto, vínculo del cursor y fechas

- @s9 proyecto desconocido: RED 450811, GREEN e09e1b; tarea de otro proyecto propio: RED a34bbb/60864a, GREEN c30a46. No se oculta 404 como lista vacía.
- @s8 proyecto idea propio vacío frente a hechos de otro: RED 11d813, GREEN 8aff65; tarea completed vacía frente a hermana con hechos: RED c7ec08, GREEN 61a896. SQL filtra contexto después de confirmar propiedad.
- @s17 owner del cursor: RED 79d6fb, GREEN c2d99b, con field cursor / INVALID_VALUE. @s16 propiedad antes de vínculo: inicialmente GREEN 385ffc, sin cambio de producción ni RED inventado.
- @s17 filtros del cursor: RED c53964, GREEN 11701d; upper inferior a after en empate de instante: RED e06ca0, GREEN 11b539. Comparación de posiciones por instante, rank y UUID textual canónica, consistente con orden PostgreSQL.
- @s14 continuación estricta por familia/UUID: RED aae4dd, GREEN 9c5ab4. SQL aplica ambas fronteras; no necesita consultar existencia de la posición.
- @s10 día UTC inclusivo: RED 20cec1, GREEN 343024. Se incluyen medianoche y último microsegundo; se excluyen días adyacentes. Comparación de fecha UTC explícita sin construir año10000.

Este corte todavía no incorpora transacción RR/read-only, traducción de errores/cierre, validación semántica de recibo seleccionado, commits tardíos ni wiring. Tampoco afirma que las variantes heredadas y los cinco owners estén acreditados por un único ejemplo.

Formato real y regresión focal conjunta GREEN 01ce2f: 21 PG + 3 application, cero fallos/errores/omitidos. XML preservados en history_queries_checkpoint y cuatro archivos propios congelados en history_queries_checkpoint.json para revisión selectiva. Fuente/test frontend en WIP pertenecen a B y no forman parte del paquete.

## 24–27. Transacción y conexión real de beans

Root aprobó el checkpoint de consultas en06bd830, liberando las cuatro fuentes/tests.

- @s22 HistoryReadTransactionTest.s22_pageRunsInsideReadOnlyRepeatableRead: RED 0a6047 observó readOnly off, GREEN 0f8bd9 observa on/repeatable read dentro de la consulta PostgreSQL. Constructor del adapter pasa a (JdbcTemplate, PlatformTransactionManager, ObjectMapper); puerto application intacto, consumidores PG ajustados mecánicamente.
- @s23 cierre de transacción: RED d761a9, GREEN c0f448; manager delegado completa doCommit y lanza TransactionSystemException, capturada fuera de execute. No página vacía ante fallo.
- @s23 SQL real: RED 270040, GREEN 4da4d0; tabla de este contenedor renombrada/restaurada en finally y DataAccessException traducida. No test global ni cambio de otra fuente.
- @s1 ApplicationWiringTest.history_s1_readBeanUsesTheRealQueries: RED 1758e2 por bean ausente, GREEN 73e1f2 con ReadHistory y PostgresHistoryQueries reales. JdbcTemplate y manager simulados en este contexto: alcanza el adapter y su rechazo de contexto; no se atribuye PostgreSQL ni HTTP reales a este caso.

Cierre de este checkpoint: formato y regresión GREEN4382ad; los cinco XML preservados en history_wiring_checkpoint suman **56** casos (21 PG consultas +3 PG transacción +3 application +22 wiring +7 configuración), todos sin fallos/errores/omitidos. El mensaje de entrega indicó57 por error de suma; los XML y conteos por suite siempre fueron56. Manifiesto de cinco archivos history_wiring_checkpoint.json, SHA D433DFF3ABDA3091643F763450AEA1BF04950DBCA9C90C6CE3A43302B0920BA5. Se conserva el límite de la prueba wiring: infraestructura simulada, adaptador real.

## 28–33. Corrupción seleccionada y extremos temporales

- @s23 recibo de bloque parseable con ID ajeno al hecho: RED 9c4442, GREEN 98d507. Instantánea before de otra tarea: RED 8d8214, GREEN 9bf0b0. Se valida identidad/contexto del detalle seleccionado antes de entregar página; no se atribuye esta guarda al constructor del record.
- @s11 año0001 real en PostgreSQL: RED 9a0fe8/d0fe17. jsonb_build_object(timestamptz) usaba la zona de conexión e introducía una fecha BC y offset histórico con segundos. Los instantes construidos en JSON se expresan ahora en UTC explícito. Incidente886d5e: reemplazo inicial alcanzó columnas tipadas del UNION; se limitó a JSON antes de continuar. Segundo RED093f8d/f3b8ae reveló getTimestamp con calendario híbrido (0000-12-30 frente0001-01-01); JDBC OffsetDateTime en lectura y parámetros temporales conserva calendario proléptico. GREEN adf7c5. Sólo adapter18, sin modificar adaptadores heredados.
- @s11 último día9999: inicialmente GREEN3b6e50, con final23:59:59.999999 y filtro superior sin construir año10000.
- @s19/@s11 cursor sintético inexistente en año0001: inicialmente GREEN2e73d1; lee el hecho anterior y no exige una fila para la frontera.
- @s22 snapshot con escritor real: fixture e67b85 violó tasks_completion_consistent porque updatedAt usaba now mientras completedAt era fijo posterior; corregidos sólo los instantes de fixture. El oráculo correcto fue inicialmente GREEN067d06. La consulta explícita de propiedad establece snapshot; un escritor en otro hilo confirma reapertura y renombra antes del SELECT de hechos. La página conserva hecho/nombre anteriores y la siguiente lectura observa ambos cambios. No se presenta el fallo de fixture como RED de producción.

## 34–43. Privacidad completa, continuación y metadatos de recibos

- @s3 cinco fuentes simultáneas: inicialmente GREEN f71623. Fixture coherente con planificación/cancelación, transición de tarea e inicio/pausa; propietario obtiene las cinco y otra identidad ninguna. No se extrapola sólo desde inicio.
- @s20 inserción tardía detrás de after: inicialmente GREEN727797; antes hubo incidente de compilación e5e514 por helper temporal no aplicado debido a finales de línea, sin cambio productivo. La primera página se obtiene realmente con ReadHistory y SQL:20 hechos, upper11:00/after09:00. Se confirma un nuevo hecho08:30 y la continuación lo entrega antes del restante08:00.
- @s20 nueva entrada12:00 delante de upper: inicialmente GREEN bcf1a6; entrada10:00 entre upper/after: inicialmente GREEN2f8458. Ninguna entra ni repite los hechos entregados. Inserciones en transacciones posteriores a la primera página, sin afirmar snapshot entre solicitudes.
- @s21/@s5 refresh sin cursor: inicialmente GREEN9fdfc4. Descubre el hecho previamente excluido y las etiquetas actuales; conserva PlannedBlock original completo.
- @s23 contexto de before de sesión: RED4f0a71, GREEN00a104; identidad del recibo de sesión: RED4d404a, GREENa00564.
- @s23 instante de recibo de bloque: RED4d33b8, GREENc5186b. Comparación común sameFact para ambos recibos; instante de recibo de sesión inicialmente GREENd8a2a2.
- @s23 identidad interna de sesión frente a sus snapshots: REDaddc1f, GREENecd72a; identidad interna del bloque: RED0f728c, GREENd91e6e. Estas guardas no sustituyen las FK/join del contexto exterior.

La aclaración contractual @s18 de5e2ad1e sólo afecta al oráculo HTTP de C (GET autenticado sin CSRF y query inválida), sin cambios en estas consultas. SHA de history.feature vigente C9AB1D0486E98FB3F7B868EB5074DFD8C1EEE2EADC463B47C9032749F87B5D0D,39 escenarios/142 ejemplos; no confundir ejemplos con tests.

## Cierre de integridad y compatibilidad de recibos

- Pausa con neto alterado: RED ca4255, GREEN8a44d3. RESUME confirmado por caso de uso y Store reales se lee junto a su PAUSE original: inicialmente GREEN dd78cc/bb16c7. Corrupción del neto de RESUME contando descanso: RED306550, GREEN318670.
- CLOSE real desde paused, con notas y start original, sobrevive a retirada de outbox del fixture: inicialmente GREEN0b1eb0. No prueba publicación Rabbit ni reinicio de proceso: se construye un nuevo lector PG después de retirar la fila de outbox. CLOSE alterado para sumar descanso: REDaa9bef, GREEN0453c7.
- EXTEND real, con extension separada y start original: inicialmente GREEN241120. after.changedAt alterado: REDfda764, GREEN769b05. Se conserva estado/contador y se verifica fórmula exacta del nuevo fin; no se consulta Clock en historial.
- RESCHEDULED con proyección actual distinta: inicialmente GREEN644c41; el historial conserva planificación inicial y before/after del recibo. No deriva un hecho adicional desde la proyección.
- Hallazgo de revisión root: identidad nula en SessionStart de before escapaba como NPE. REDd81660, GREEN405846 con comparación null-safe. Un record PlannedBlock conserva además su constructor de validación heredado.
- Hallazgo de revisión root: recibo coherente del otro bloque del mismo contexto. REDb7d19c, GREEN9b7ce4, nominal de cinco familias revalidado bde925 para descartar un falso verde por SQL roto. SQL incluye metadata durable subject_id,decision_kind,decision_revision y no la devuelve al HTTP.
- Recibo de sesión con tres IDs internos cambiados conjuntamente a otra UUID: REDed8df8, GREENe4c844. Es corrupción controlada del JSON; no se sembraron dos sesiones abiertas ni se afirma existencia de otra sesión real. Comparación contra session_id/action/expected_revision durables; bloques contra block_id/kind/version.
- Suma aritméticamente consistente con before.worked negativo: REDa49ddd, GREENd724db. Incidente d708a6: helper escrito pero llamada no aplicada por finales de línea, corregido antes de GREEN. Se verifican límites y forma básica del snapshot con aritmética de microsegundos, reutilizando guardas de estado vigentes.
- CANCELLED con after: RED5ef958/ecbc8c, GREENfdbeb6. La forma discriminada exige after para RESCHEDULED y ausencia para CANCELLED.
- CLOSE sin workDate persistido: RED9f1544, GREEN3275ca. Forma mínima de closure (notas, fecha y zona), con WorkSessionCloseNotes reutilizado. No resolución de TZDB, conversión de workDate ni atribución recalculada.

Root integró HTTP/cursor4ef385c y soporte222725e entre ejecuciones, sin cambiar los archivos de este autor. El último refactor sólo espacia el SQL para revisión; la regresión focal posterior incluye HTTP integrado y los contextos existentes.

## Freeze final A y mapa contractual

Último hallazgo root: JSON literal null en work_session_changes no viola NOT NULL SQL. El caso PG s23_jsonNullReceiptIsStorageUnavailable reproduce RED 6206b1; guardia de detalle nulo antes del mapeo devuelve StorageUnavailableException, GREEN e97112. No se modifica esquema ni se duplica la restricción object de bloques.

Mapa compacto de evidencia ejecutada (prefijos s corresponden a métodos de HistoryQueriesPersistenceTest salvo indicación):

| Contrato | Evidencia y frontera |
| --- | --- |
| @s1–2 | Nominales de cinco fuentes y recibos PAUSE/RESUME/CLOSE/EXTEND/RESCHEDULED; planificación original se conserva. Evidencia compuesta de ausencia de hechos derivados, no fixture literal de diez entradas. |
| @s3–5 | s3_allFiveFamiliesStayInsideTheirOwnersContext; empate UUID entre familias; s21_refreshIncludesPreviouslyExcludedFactWithCurrentNames. |
| @s6 | PG limita a 21; ReadHistoryTest comprueba entrega de 20 y cursor de última entrada. |
| @s7–9 | Tres categorías, proyecto idea vacío, tarea completed sin hechos, proyecto ausente y tarea de otro proyecto. No se atribuye una ejecución independiente a cada variante de estados. |
| @s10–11 | Día UTC inclusivo con microsegundos, extremos reales PG 0001/9999 y cursor sintético; formas lexicales en HistoryApiTest. |
| @s12 | Validación de query en suite HTTP de C, incluida inversión de fechas y task sin project. |
| @s13–14 | Orden timestamp/rango/UUID nativo, límites estrictos entre familias y UUID; ReadHistoryTest conserva upper inicial. Prueba compuesta, sin atribuir fixture literal de seis hechos al mismo instante. |
| @s15–18 | Sintaxis canónica y precedencia de seguridad/query en HTTP de C; propiedad antes de owner de cursor, filtros y upper/after en PG. Se conserva aclaración contractual de GET sin CSRF con query inválida. |
| @s19 | Cursor en año 0001 sin fila de frontera existente. |
| @s20–21 | Tres inserciones posteriores a página inicial: detrás de after aparece, delante de upper o entre upper/after no aparece; refresh recupera novedades y nombres actuales. No snapshot entre solicitudes. |
| @s22 | HistoryReadTransactionTest observa read-only/RR; writer termina entre consulta de propiedad y SELECT de hechos, primera página conserva snapshot y siguiente ve commit. |
| @s23 | Fallos SQL y cierre transaccional; corrupción seleccionada de identidad, metadata durable, timestamp, forma discriminada, neto, atribución obligatoria y JSON null. HTTP de C verifica 503 sin detalles privados. Oráculos representativos, no certificación exhaustiva de toda corrupción posible. |
| @s24 | Nuevo lector tras retirar outbox conserva start y CLOSE. No equivale a reinicio de proceso ni publicación Rabbit: integración real corresponde al paquete de C. |
| @s25–39 | Cliente/UI de B y E2E/UX de C, fuera de este freeze A. |

No se añadieron tabla, migración, índice, Clock ni consumidor. Los 39 escenarios y 142 ejemplos contractuales no equivalen al número de pruebas. El paquete queda listo para revisión; init/build global, mutación, UX/E2E y CI se acreditan por separado, sin declarar feature terminada.

Formato real y regresión focal final: EXIT 0, 12c2e4 (28 s). Se preservan seis XML, 163/163: 52 PG consultas, 4 PG transacción, 3 aplicación, 22 wiring, 7 configuración y 75 HTTP de C; cero fallos, errores u omitidos. Manifiesto de 14 archivos y XML en progress/history_backend_final_manifest.json. El corte anterior de 162 precedía exclusivamente al oráculo JSON null. Fuentes y tests congelados; no más Gradle por este autor hasta coordinación root.
