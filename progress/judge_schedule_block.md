# Review — feature 11 schedule_block

**Veredicto: APPROVED para diseño, cobertura y verificación previa a mutación.** Fecha: 6 de septiembre de 2026. Juez: coordinador, sin autoría de producción ni pruebas. La feature sigue in_progress; este dictamen habilita PIT/Stryker, no el cierre.

## Cobertura de escenarios

El índice [tdd_schedule_block.md](tdd_schedule_block.md) reúne los ciclos y mapas concretos de autores. Se revisaron fuentes, pruebas y revisiones parciales, incluida la cobertura adicional posterior a los hallazgos iniciales.

| Escenario | Evidencia revisada |
| --- | --- |
| @s1 | ScheduleBlockApiTest.s1_previewReturnsExactSnapshotWithoutWriting |
| @s2 | ScheduleBlockApiTest.s2_createAtomic; SQL y evento; primer E2E real |
| @s3 | BlockRequestTest, normalización y límite de 500 puntos suplementarios |
| @s4 | s4_invalidStructure, s4_creationStructure, s4_rootObject, s4_creationRootMustBeObject |
| @s5 | s5_strictLocal |
| @s6 | ResolvedBlockTimeTest, duración exacta y límites |
| @s7 | PlanBlockTest.s7_rechecksServerClockAfterPreviewAndAcceptsExactEquality |
| @s8 | ResolvedBlockTimeTest, s8_closedOffsetOptions y E2E Madrid |
| @s9 | Resolución con catálogo controlado y s9_canonicalOffset |
| @s10 | Límites UTC/locales de ResolvedBlockTimeTest y restricciones SQL |
| @s11 | BlockBudgetTest: anclas consecutivas, Apia, Casey, medianoche y DST |
| @s12 | ScheduleBlockPersistenceTest.s12_previewCountsOwnReservationsAcrossCompletedProjectsOnly |
| @s13 | PlanBlockTest contigüidad y ScheduleBlockPersistenceTest.s13_s15_creationRechecksAllOwnReservationsBeforeWriting |
| @s14 | PlanBlockTest.s13_s14_rejectsOverlapUsingFirstStartThenUuid |
| @s15 | s15_budgetErrorHasCurrentDays; PlanBlockTest y E2E consentimiento |
| @s16 | Carreras de presupuesto en persistencia y E2E consumo posterior al preview |
| @s17 | s17_s18_businessPrecedence |
| @s18 | s18_headers y s19_emptyBodyPreservesPrecedence |
| @s19 | Seguridad, propiedad, query/IDs/headers antes del cuerpo en ScheduleBlockApiTest |
| @s20 | no-store en respuestas correctas y errores de las cinco rutas HTTP |
| @s21 | s21_s22_replayConfirmedIntention |
| @s22 | s22_keyBindsExactIntention y s22_sameKeyIsScopedToTask |
| @s23 | s23_replayStillRequiresSecurityAndStructure |
| @s24 | s24_s26_readSavedBlock y recuperación E2E por key |
| @s25 | s25_s26_listUsesStableCursor y s25_createdAtPrecedesUuidOrder |
| @s26 | s24_s26_readSavedBlock y s26_readStorageFailureIsSafe503 |
| @s27 | s19_s27_strictReadQueryAndCursor |
| @s28 | BlockBudgetTest.s11_s28_projectsMidnightAndHistoricalReservationsIntoCurrentBudgetZone |
| @s29 | Persistencia s29_sameKeyWaitingForAvailabilityReplaysAfterWinningCommit |
| @s30 | Persistencia s30_serializesConflictingReservationsAcrossProjects |
| @s31 | Persistencia s31_realStateAndPreferenceWritersCoordinateInBothCommitOrders |
| @s32 | Persistencia s32_otherOwnerCommitsWhileFirstOwnerStillHoldsAvailability |
| @s33 | Persistencia: ausencia de preferencia bajo lock y snapshot contra writers reales |
| @s34 | Persistencia: cinco triggers de fallo/supresión/COMMIT y fallos de lectura |
| @s35 | E2E lost acknowledgement recovers persisted block after real backend restart |
| @s36 | s2_createAtomic y PublishOutboxTest: payload cerrado BlockPlanned.v1 |
| @s37 | PublishOutboxTest, RabbitBrokerPublisherTest y OutboxRecoveryTest; rutas y reintentos reales |
| @s38 | UI abre editor nativo con zona guardada y recupera configuración fallida |
| @s39 | UI revisa intención, distingue zonas y confirma presupuesto; E2E real |
| @s40 | UI preview fallido conserva borrador; variantes estrictas del cliente API |
| @s41 | UI ocurrencias por extremo y E2E Madrid |
| @s42 | UI edición invalida revisión/respuesta pendiente; E2E renovación de consentimiento |
| @s43 | UI aceptación explícita no preseleccionada; E2E presupuesto cero y consumo concurrente |
| @s44 | UI una intención retenida y doble envío bloqueado; E2E key/revisión/201 |
| @s45 | Cliente API y UI incertidumbre; E2E abort después del commit real |
| @s46 | UI confirmación coherente y recuperación E2E |
| @s47 | UI 404 por key conserva intención bloqueada |
| @s48 | UI reenvía exactamente el mismo bloque de forma manual |
| @s49 | UI rechazos tipados, errores por campo y consulta explícita de conflicto; E2E solape/presupuesto |
| @s50 | UI IDEMPOTENCY_CONFLICT conserva key e intención y consulta su resultado |
| @s51 | UI recuperación CSRF conserva petición y exige reenvío manual |
| @s52 | UI retiro del contexto privado ante pérdida de acceso, incluida SessionGate |
| @s53 | UI JSON tardío en listado/check tras navegación y CSRF tras revocación; señales abortadas y foco conservado |
| @s54 | UI fallos de configuración/preview/lista sin perder confirmación o borrador |
| @s55 | UI enlace a disponibilidad con descarte explicado |
| @s56 | TaskState compartido y recuperación con proyecto/tarea completados; E2E proyecto completed |
| @s57 | UI muestra UTC explícito y zona original si Intl no puede resolverla |
| @s58 | UI foco origen/encabezado/exterior y teclado nativo en tres motores |
| @s59 | Matriz de controles, errores asociados, reflow y axe; estilos SCSS |
| @s60 | Chromium/Firefox/WebKit y zoom nativo Chromium al 200 %, documentados separadamente de viewport |
| @s61 | Matriz de treinta principios y medición de feedback; límites humanos explícitos |
| @s62 | s62_unreadableJson y s62_creationUnreadableJson |

Los tags agrupan comportamientos: no se identifica el número de tests con los 325 Examples ni se afirma que cada E2E cubra todas las variantes de sus etiquetas. Las pruebas y bitácoras detallan cada parámetro y distinguen los niveles HTTP, dominio, persistencia y UI.

## Disciplina y calidad

Ciclos RED/GREEN documentados para código nuevo. Cobertura adicional inicialmente verde identificada como tal. Hallazgos corregidos y revisados: anclas de días omitidos/invertidos, precisión temporal y límites, precedencia con cuerpo ausente, reservas globales, recheck de idempotencia, preferencia insertada después de observar ausencia, rowcounts y traducción de errores hasta COMMIT, foco y retirada de contexto privado. La regresión de configuración se corrigió añadiendo puertos al fixture, sin debilitar aserciones de capacidad.

Dominio/aplicación conservan fronteras hexagonales; PostgreSQL coordina locks y atomicidad, outbox entrega al menos una vez. No hay reenvío automático de creación ni confirmación ficticia. Frontend usa React/SCSS y controles nativos, sin nuevas dependencias. El límite conocido de cargar historia del propietario tiene comentario explícito; no se añade infraestructura especulativa.

Arnés: targets cerrados y argumentos constantes; default completo conservado. Siete pruebas stdlib comprueban selección, rechazo y conexión CLI/CI sin lanzar mutadores. PIT incluye ApplicationConfiguration y los adaptadores/modelos nuevos, mantiene umbral 80 y recibe classpath de recuperación por provider lazy comprobado sin ejecutar mutación. Stryker incluye API/UI y TaskReader/TaskState completos; mantiene umbral 80. Su exclusión de una carpeta protegida fue validada sintéticamente, sin leerla ni limpiarla.

## Verificación y checkpoints

- C1/C2: init 34832 EXIT 0 (1b3236), configuración válida y una sola feature activa.
- C3: arquitectura y dependencias revisadas; formato/lint verdes. Build frontend b96beb y builds reales Docker verdes.
- C4: 1338 backend sin fallos/errores/omitidos (XML 22cb2f), 1121 frontend y siete tests del arnés. PostgreSQL y RabbitMQ reales en sus suites.
- C5: estado activo documentado. Commit/cierre e historial final se completarán después de mutación; no se afirma limpieza de temporales protegidos.
- C6: contrato aprobado, mapas y ciclos enlazados; no se detecta comportamiento de producto ajeno al contrato.
- C7: pendiente, a cargo de PIT/Stryker después de este dictamen.

E2E: los siete recorridos de bloques pasan en cada motor; revisión independiente del E2E aprobada. Regresión completa del coordinador: 57/58 verdes (9109e0). El único fallo fue timeout histórico de disponibilidad; focal con traza pasó en 27 segundos, con 23,9 en la matriz. División revisada: conserva exactamente los 28 anchos, altura reducida, estado de error, bounds, navegación y axe; no cambia SCSS ni timeout. Grupo afectado 31/31 verde (aebd37), con funcional 1,4 segundos y anchos 1,5–2,5 cada uno. Son ejecuciones separadas y dos casos solapados, no una supuesta ejecución global nueva de 86/86. La incidencia queda resuelta mediante comprobación de la parte modificada y conservación de los otros 57 resultados.

No se atribuye evaluación humana, dispositivos físicos o lector real a las mediciones automáticas. No hay despliegue productivo ni cierre de MVP. No quedan cambios bloqueantes para iniciar mutación de esta feature.

## Seguimiento posterior a primera mutación: backend

El coordinador revisó los límites nuevos de BlockBudget y los cursores/página terminal HTTP, las 14 operaciones de ApplicationWiringTest en contextos frescos y los nueve casos netos del publicador. Estos últimos conservan ahora un JSON coherente con el payload del fixture. No hay diferencias de producción respecto a 3671b94. La configuración sólo añade ApplicationWiringTest y RabbitBrokerFailuresTest al conjunto de pruebas de PIT y corrige líneas vacías finales; targetClasses, runtime de recuperación y umbral siguen intactos.

Regresión afectada del autor 6f086d EXIT 0 tras formato: 323 pruebas. XML comprobados independientemente por root en 107320: HTTP173, presupuesto17, wiring14, configuración7, publicación103 y fallos de Rabbit9; cero fallos, errores u omitidos. git diff --check limpio en 20b53c. Los casos nuevos son inicialmente verdes, sin RED fabricado. Detalle de ciclos75–92 y publicador en sus bitácoras.

**APPROVED para repetir PIT backend** sobre el mismo alcance productivo. El informe inicial 414/454 se conserva. No se adelanta ningún nuevo kill; el candidato equivalente BlockBudget34 índice187 tiene argumento independiente en review_schedule_block_mutation_candidates.md y no modifica el score bruto. Stryker inicial ha terminado con umbral superado, pero su análisis de supervivientes y un error del runner sigue pendiente. La feature permanece in_progress.

## Seguimiento frontend: API

Root revisó el diff completo de schedule-block-api.test.ts en 2cfa2c: 60 casos netos agrupados en las matrices existentes y nuevos recorridos públicos, sin tocar producción. Se comprueban fronteras válidas, errores especializados cerrados, colecciones mixtas, coerción de valores JSON y coherencia de tiempo/intención. Los vectores de duración mantienen correctas las demás invariantes, y el UUID array se prueba por lista para evitar que una segunda validación de detalle oculte el hueco. readBlockError debe resolver null cuando el protocolo es incompatible; los DTO deben rechazarse. Las equivalencias se revisaron por separado.

Entrega del autor: 250/250 API PASS bd27c5, sin omitidos; formato17b6df, ESLint7049b0 y TypeScript3184c6. **APPROVED diseño y cobertura del refuerzo API**. La aprobación no sustituye la regresión frontend integrada después de terminar UI ni autoriza aún Stryker; faltan esa entrega y la revisión de su configuración de replay. Los ciclos inicialmente verdes están registrados con sus límites, sin RED fabricado.

## Seguimiento: resultado backend y soporte de replay frontend

PIT segundo c6bc2c EXIT0: 453/454 KILLED (99,78 %), un SURVIVED contextual BlockBudget34 índice187, sin NoCoverage, errores ni timeouts. Root verificó XML y correspondencia de las454 identidades únicas (cf2ac6/925113); se conservan ambos informes. Se acepta el cierre del seguimiento de mutación backend, sin descontar la equivalencia.

Root revisó scripts/project.mjs y sus tests completos d5264d: el target adicional de replay es cerrado, llama sólo una configuración fija y mantiene default/targets previos. La prueba usa runner inyectado y no lanza mutación. Reejecución independiente node:test589cd5: nueve casos verdes, cero omitidos. Verificación independiente3aa674: hashes de las cuatro fuentes y el informe original coinciden;167 IDs únicos y unión exacta de141 rangos incluyendo la línea nueva de producción. El CLI posicional fue confirmado por lectura de la versión instalada; reporters escriben destinos separados, umbral80/perTest/concurrency2 y patrón protector conservados.

**APPROVED soporte de replay**, pendiente de entregar y revisar UI y pasar la regresión integrada antes de iniciar Stryker. El replay tendrá denominador propio y no se presentará como un nuevo resultado global ni se mezclarán sus identidades locales con las originales sin remapeo.

## Puerta de replay frontend completa

Dictamen independiente UI APPROVED en review_schedule_block_ui_mutation.md: 17 nuevos casos, 180/180 de regresión afectada. La única modificación productiva es retirar taskState anterior al reintentar TaskReader; RED9d5579 y GREEN56ed8e prueban que la planificación espera al nuevo estado. Se conserva la separación entre operación nueva y recuperación de una identidad ya enviada.

Init global del coordinador21625 EXIT0 f79afd: lint verde, nueve tests del arnés,1198 frontend y1365 backend; XML backend contrastados en161c53, cero fallos/errores/omitidos. Build frontend391add EXIT0. E2E focal nuevo48211 EXIT0/72ed46: siete recorridos de bloques verdes en35,6s sobre Docker/API/PostgreSQL reales, incluido reinicio real. La invocación previa69111 falló antes de ejecutar pruebas por usar un nombre de proyecto inexistente en Playwright; se retiró ese argumento, sin modificar pruebas ni configuración. Ambos stacks de prueba se retiraron por su runner propio.

**APPROVED para ejecutar replay frontend por arnés**, con fuentes, tests y configuración congelados. Los resultados de la campaña y del replay se conservarán separados, con remapeo de identidades y error945 explícito. No se marca done antes de analizar esa medición.

## Revisión del primer replay y seguimiento final

Root contrastó el informe final y167 identidades en067ac3:362/404 Killed,41 Survived,1 RuntimeError, puntuación bruta89,60 %. La nueva línea de TaskReader es detectada. Se conserva la campaña original y su denominador.

Diff973b9d APPROVED: cinco pruebas nuevas difieren JSON después de recibir Response, mantienen vivo el padre al reintentar o reabrir, y observan estado/foco/sesión públicos tras resolver la petición retirada. Los78 tests de bloques, ESLint y TypeScript del autor pasan. No cambió producción.

Root ratifica las equivalencias contextuales834/1087/1184 de review_schedule_block_replay_remaining.md: el consentimiento inicial se restablece al editar los campos obligatorios vacíos; el catch de configuración retirado sólo afecta un setter de la instancia desmontada o una promesa que ya rechazó; el fallback de select sin coincidencia conserva la primera opción vacía habilitada y no cambia el estado enviado. No se eliminan estos mutantes del denominador.

Los22 casos observables restantes se asignan a cinco flujos públicos agrupados. El umbral numérico no elimina la obligación de justificar supervivientes de docs/verification.md. RuntimeError945 permanece como límite de medición del adaptador, sin kill ni equivalencia inferidos; no se ordena modificar el ejecutor ni repetirlo indefinidamente. Su aceptación final queda explícita en el dictamen de cierre, una vez resueltos los supervivientes observables. Feature sigue in_progress.

Soporte final APPROVED: root leyó configuración26 rangos/29 identidades y diff de tests86881a, con10/10 pruebas stdlib verdes independientes. Reporters final.json/html separados del replay y original; defaults, umbral80 y patrón protector conservados. El nuevo manifiesto conserva identidades/posiciones/hashes; fuentes e informes históricos verificados. CI: aprobado diff de una línea120→240 por timeout confirmado del job antiguo; no cambia ninguna puerta. La medición final espera entrega y regresión del autor UI.

## Puerta de medición final

APPROVED tras revisión completa de los11 casos netos y ajustes existentes (0ab894/3eca08), dictamen independiente y dos correcciones de oráculos (@s40 última petición y selector actual tras editar fin). No hay cambios productivos respecto a56ced31. La regresión global inicial26470 falló en una prueba histórica por intervalo DOM retirado/cleanup pasivo; diagnóstico y corrección mantienen aborto y no restauración, revisados independientemente. No se debilitó el resultado esperado.

Init final94736 EXIT0/8d8c38: lint verde,10 tests del arnés,1209/1209 frontend en21 archivos; Gradle test UP-TO-DATE con suite backend1365 previamente verificada, sin cambios backend. No se presenta ese resultado cacheado como1365 nuevas ejecuciones. Build/E2E previos conservan validez al no cambiar producción. Fuentes, pruebas y configuración congelados: APPROVED medición final por el target cerrado del arnés. RuntimeError945 histórico sigue separado.

## Dictamen de cierre de schedule_block

**APPROVED para marcar feature11 done.** Root verificó independientemente55/55 Killed y29/29 coincidencias exactas por archivo/operador/replacement/ubicación47a669. Campaña final79a726 EXIT0, sin supervivientes, NoCoverage, timeouts o errores. Los29 cambios observables pendientes son detectados; las equivalencias restantes conservan justificación contextual en las revisiones y no se descuentan de los denominadores históricos. Backend453/454, frontend original85,38 %, replay89,83 % y final55/55 son mediciones separadas, nunca un supuesto100 % global.

Se acepta explícitamente como límite de medición el RuntimeError945 histórico del adaptador Stryker, contando ese caso como no eliminado en el cociente bruto anterior89,60 % >80. No es equivalente, Killed ni error resuelto; no queda un hallazgo de producto demostrado asociado. Las reglas no exigen modificar el ejecutor ni cero RuntimeError en toda campaña histórica. La última campaña no lo incluyó y no se usa para ocultarlo. La documentación conserva su excepción y los dos intentos internos del runner.

Último init94736 verde y dictámenes independientes completos. Producción no cambió desde el build y E2E72ed46; las dos correcciones posteriores son de oráculos de pruebas. Contratos, trazabilidad, API, persistencia, eventos, recuperación, responsive y accesibilidad técnica están documentados con sus límites. CI2133120 run34030806009 sigue en curso; ampliación240min no se presenta como CI completo verde. Este cierre es de planificación de bloques, no del MVP/proyecto ni del despliegue productivo.
