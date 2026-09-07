# TDD backend19 — revisión semanal

Contrato34 escenarios/100 ejemplos, freeze root4fbc370. Autorización TDD previa
@s1–24 conservada; init62482 EXIT0 antes del ciclo. Sin suites globales nuevas.
Ponytail full/Caveman lite, docs/tdd leídos77edb8. A posee núcleo/PG/wiring; C
HTTP aislado y B frontend. No cambios a V14 ni rutas protegidas.

## Ciclo1: nominal vacío y frontera de captura temporal

ReadWeeklyReviewTest.s1_emptyWeekUsesOneClockOnlyAfterTheSnapshotCallback.
RED compilación real9361c1 por ausencia de tres tipos de aplicación, antes de
escribir producción. GREEN f3dff5; formato real y focal completo47cc69 EXIT0,
1/1, cero fallos. No se atribuye RED funcional adicional: falta de compilación
cuenta como primer RED según docs/tdd.

El doble de WeeklyReviewQueries establece una marca de snapshot y sólo después
invoca el callback; el Clock verifica esa marca y se llama exactamente una vez.
Se obtiene semanaUTC07–13septiembre, siete días continuos, tres duraciones
correctas con capacidadnull, contador0 e instante truncadoµs. Oráculo compara
las fronteras/fechas/valores públicos, no serialización del mismo record.

Límite: prueba de frontera de puerto y núcleo, no PostgreSQL, aislamiento real,
HTTP ni snapshotdurable. Primer bundle implementa sólo omisión de date/zone y
preferencia vacía; date explícita, catálogo/preferencia, datos no vacíos,
integridad, errores y persistencia son siguientes ciclos. Constructor conserva
la firma convenida con ZoneCatalog, todavía no consumido por este nominal;
no se afirma soportar los otros valores por compilar la firma. Ningún puerto
abstracto tiene stub/default; el doble sólo vive en la prueba.

WeeklyReviewWindow es detalle interno A, inicialmente sólo serverNow y
emptyReview; se ampliará por próximos tests. C consume ReadWeeklyReviewUseCase
y WeeklyReview con records Day/Totals, nunca la ventana interna.

## Freeze nominal

Cinco fuentes y un test en weekly_review_first_bundle.json, con hashes y XML
preservado. No wiring ni adaptadorPG en este corte; root puede revisar/versionar
este bundle para que C implemente el nominal HTTP contra tipos reales. No
feature terminada ni pruebas de todas las familias.
## Ciclos 2–5: selección temporal y capacidad

Cada caso se añadió individualmente y se ejecutó con `gradlew.bat test --daemon --tests '*ReadWeeklyReviewTest.<método>' --max-workers=4` antes de cambiar producción.

- `s2_explicitDateAndZoneSelectTheirCivilWeek`: RED bb6d46 (semana actual en vez de marzo); un reemplazo parcial mantuvo RED 6d8e9d, corregido sin cambiar oráculo. GREEN d1e74c, 2/2. Fecha explícita y semana DST de 167 horas.
- `s2_storedZoneUsesItsLocalWeekAndCurrentCapacity`: RED ce068c; GREEN 425c55, 3/3. También acredita @s3 y capacidad nominal @s15: lunes local cuando UTC sigue domingo, 120 minutos por día.
- `s2_unavailableStoredZoneFallsBackWithoutMovingItsBudget`: RED ff1ffe (ZoneRulesException); GREEN e0b211, 1/1. Fallback UTC, zona guardada visible y capacidad desconocida.
- `s15_explicitDifferentZoneKeepsCapacityUnknown`: RED 3d648f (presupuesto trasladado); GREEN 8354af, 1/1. EXPLICIT conserva la zona guardada, sin trasladar capacidad.

Las firmas públicas de WeeklyReview/get permanecen iguales. Sólo la ventana interna evoluciona. No se atribuye aún evidencia PostgreSQL.
## Ciclos 6–13: validación y nominal PostgreSQL/wiring

Comando focal idéntico al anterior con el método indicado, sin suite global.

- `s4_unknownExplicitZoneFailsBeforeQueriesOrClock`: RED ff926c; GREEN 751175. Error zoneId/INVALID_VALUE antes de consultar y capturar reloj.
- `s6_explicitWeekBeyondCivilRangeFailsBeforeSnapshot`: RED 9ca663; GREEN c56d89. Semana civil incompleta produce date/INVALID_VALUE.
- `s6_utcBoundaryBeforeYearOneIsATemporalConflict`: RED de compilación 9b5839; GREEN 251114. Exige WeeklyReviewTimeOutOfRangeException real.
- `s6_unrepresentableClockFailsEvenForAnExplicitSafeWeek`: RED ed8c79; GREEN 45cc5a.
- `s6_localClockBeyondYearRangeFailsEvenWithExplicitDate`: RED 1b2281; GREEN 6b8cd7.
- `s6_defaultWeekWhoseSundayExceedsYearRangeIsATemporalConflict`: RED 09ff9f; GREEN 02389a, regresión de los 11 casos.
- `WeeklyReviewPersistenceTest.s1_emptyWeekReadsPostgresInReadOnlyRepeatableRead`: RED de compilación f5f52a; GREEN 2fea66, PostgreSQL17.9 real migrado. El callback del Clock comprueba SHOW transaction_isolation=repeatable read y transaction_read_only=on. Availability.find se ejecuta antes del callback; el caso prueba nominal vacío, todavía no carrera con escritor ni agregación de hechos.
- `ApplicationWiringTest.weekly_s1_readBeanUsesTheRealQueries`: RED 07061d (bean ausente); GREEN c5a75d. Usa casos de uso y adaptador concretos con JdbcTemplate/transaction manager simulados; no se atribuye PostgreSQL ni HTTP a este oráculo.

ReadWeeklyReviewUseCase y WeeklyReview no cambian. La excepción temporal vive en domain, como las demás reglas puras. No se ha añadido migración ni endpoint. El adaptador nominal aún devuelve la semana vacía tras disponibilidad; agregación de hechos y errores se incorporan mediante siguientes casos.

Formato real y regresión de las tres clases: 5281e8 EXIT0, 35/35 (11 aplicación, 1 PostgreSQL y 23 wiring), cero fallos/errores. Manifiesto weekly_review_nominal_bundle.json y XML preservados en weekly_review_nominal_xml. Este corte permite integrar HTTP nominal; aún faltan agregaciones, corrupción y carreras reales.
