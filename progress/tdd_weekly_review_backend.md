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
