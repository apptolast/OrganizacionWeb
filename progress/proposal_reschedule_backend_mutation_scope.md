# Propuesta exacta de PIT13 — pendiente de aplicar y ejecutar

Corte de lectura 6 de septiembre de2026. Se siguió `review_reschedule_backend_mutation_scope.md`; esto es soporte propuesto, no resultado de mutación ni modificación del build activo. Los archivos completos propuestos están en `progress/reschedule-pit-proposal/`.

## Bases y diferencias entre árboles

- Build base: árbol OrganizacionWeb-backend, SHA256 `7819c6fdcc4e4854ecc5fb5d1003eee0ee1fb2d8b703ce005afecbb0ba4e2dd1`.
- Build del árbol principal OrganizacionWeb: `c4e700c780dcfc0ce057b5c4582eee78cf40801e5b60de67e7a3a1cb8b6973f5`. Son iguales tras normalizar CRLF/LF (`2c65ec`); ninguno contiene scope=reschedule.
- Dispatcher base elegido: OrganizacionWeb/scripts/project.mjs, `5c85ea26f8c2c4d1d0aff445ecf2acb2f390e3683114adccfe160d5ab791334f`. Ya conserva reschedule-frontend; el dispatcher backend es más antiguo y no debe reemplazarlo al integrar.
- Tests de dispatcher base: OrganizacionWeb/scripts/project.test.mjs, `8d7907c70c342a7462b885e22eda9f35335c6facd39bd4c1f45fb702ced9f202`.
- Comparación de fuentes entre ambos árboles: lectura `36315c`, sólo directorios src/main/java. No es comparación Git contra el inicio de feature13: algunas clases13 ya están en ambos árboles. Por eso el scope incluye también CancelBlock, BlockState y los recibos/eventos, aunque no aparezcan en ese delta.

## Archivos propuestos

- `reschedule-pit-proposal/build.gradle.kts.proposed`: añade `rescheduleOnly`, `rescheduleClasses`, candidatos globales y reporte separado. SHA256 `5905df8f40be87c144f117fb545d2adf718b3df597da1271267865abdade6735`.
- `reschedule-pit-proposal/project.mjs.proposed`: base principal más whitelist y dispatch cerrado `reschedule-backend` → Gradle `pitest --no-daemon -PmutationScope=reschedule`, sin invocar frontend. SHA256 `34d97dd58359f4e2564bd51e49bc8eb10226992b619740b1439f90f7cfc3959a`.
- `reschedule-pit-proposal/project.test.mjs.proposed`: tests actuales intactos, más propuestas de oráculo del dispatch exacto y rechazo de target alterado/fuera de mutate. SHA256 `63b268102a68fb5a9b11b500e99d6796cc35be3b5147b0229b54a8b01b3155e7`.
- `reschedule-pit-proposal/source-inventory.json`: expansión mecánica de23 patrones a54 fuentes actuales con hashes. Incluye interfaces y records sin comportamiento; NO representa54 clases con mutantes ni incluye un conteo de bytecode anidado. Los comodines abarcan records/clases internas.

## Clases y pruebas candidatas

La lista exacta reutiliza `scheduleBlockClasses` y añade once patrones explícitos. No se introduce filtro por método nuevo ni por mutante difícil.

| Familia de producción incluida | Candidatos concretos, dentro de toda la suite Java |
| --- | --- |
| MoveBlock*, MoveContext, CancelBlock*, BlockState, BlockMoveRequest, BlockRequest, PlanBlock y tipos Block* | MoveBlockTest, BlockMoveRequestTest, BlockStateTest, BlockRequestTest, PlanBlockTest, RescheduleApiTest y RescheduleErrorsApiTest |
| BlockChanged* incluido Interval, BlockMutation, recibos/confirmaciones, OutboxMessage, PublishOutbox y RabbitBrokerPublisher | PublishOutboxTest, RabbitBrokerPublisherTest, RabbitBrokerFailuresTest, OutboxWorkTest y OutboxRecoveryTest |
| ReadBlockChanges*, PostgresBlockChangeQueries, BlockChangesController*, BlockCursor* | ReadBlockChangesTest, BlockChangeQueriesPersistenceTest, BlockChangesApiTest; ScheduleBlockApiTest para decoder compartido |
| RescheduleController*, BlockController*, ApiErrors, PostgresBlockStore, ReadBlocks | RescheduleApiTest, RescheduleErrorsApiTest, RescheduleCoordinationTest, ScheduleBlockApiTest, ScheduleBlockPersistenceTest y APIs anteriores que observan los handlers compartidos |
| PostgresTodayQueries y ApplicationConfiguration | TodayApiTest, RescheduleApiTest para proyección vigente/snapshots, ApplicationWiringTest y ProjectStateConfigurationTest |

`targetTests = setOf("com.apptolast.organization.*")` ofrece todos los tests del proyecto: no elimina oráculos de API heredada, errores, broker o wiring para reducir coste/elevar score. PIT aplica su selección por cobertura. RescheduleMigrationTest y otras pruebas sin cobertura Java relevante pueden no matar mutantes, pero no se filtran por conveniencia. Las pruebas adicionales que core termine antes de freeze entran automáticamente.

`targetClasses` completo sin target añade la unión `rescheduleClasses`; `targetTests` completo añade los candidatos globales. Así CI no omite los nuevos adaptadores. Los scopes anteriores mantienen sus ramas específicas sin cambios. No se sustituye el scope focal por todos los adaptadores del producto ni se elimina lógica compartida heredada de scheduleBlockClasses.

## Política conservada

PIT1.22.0, plugin JUnit5 1.2.3; mutadores predeterminados heredados (no hay `mutators.set` nuevo); `features=-FRECORD`; exclusión existente exclusivamente equals/hashCode/toString; umbral80; threads4; timeoutConst15000 no autenticación; argumentos JVM api.version/outbox.test.classpath; HTML/XML sin timestamp. Reporte13 separado `backend/build/reports/pitest-reschedule`. No se extrapola Stryker8 a PIT12. No filtros nuevos de clases, tests, anotaciones o líneas para hacer pasar el umbral.

Las migraciones SQL V12/V13 no son bytecode PIT. Se mantienen como evidencia independiente de Flyway/PostgreSQL; no hay score SQL ni exclusión de un mutante SQL.

## Orden de aplicación después de aprobación

1. Congelar e integrar fuentes/tests/configuration de todos los autores y contrastar el inventario con el delta final, especialmente nuevas clases o tests posteriores a esta lectura.
2. Aplicar únicamente el primer oráculo propuesto del dispatcher, ejecutar node:test y observar RED por target ausente. Aplicar whitelist/dispatch mínimo y comprobar GREEN. Después incorporar el negativo propuesto como ciclo separado, conservando un caso por vez; no atribuir RED a un test inicialmente GREEN. Las propuestas actuales no se han ejecutado.
3. Integrar la sección Gradle propuesta preservando cualquier cambio independiente posterior. Verificar configuración efectiva y que cada clase13 con bytecode ejecutable esté seleccionada; init integrado debe pasar antes de medir.
4. Sólo con autorización del coordinador tras freeze/init ejecutar `node .harness/harness.mjs mutate reschedule-backend`. Capturar hashes del corte antes/después, baseline real, EXIT y XML/HTML originales. No se ejecutó aquí Gradle/PIT ni se validó aún compilación Kotlin del archivo propuesto.
5. Reportar Killed/Survived/NoCoverage/Timeout/errores separados. El score bruto y las limitaciones permanecen intactos; no contar Timeout/error como detección funcional, ni descontar equivalentes sin dictamen documentado.

No se tocaron fuentes congeladas, tests activos, build activo, dispatcher activo, Git ni rutas protegidas. Esta propuesta no certifica readiness de la campaña: falta freeze/init integrado y aprobación final del scope.
