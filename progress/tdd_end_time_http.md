# TDD HTTP17

Árbol aislado end-http con bundle real6948904, sin copiar WIP. Ownership WorkSessionStateController, nuevo EndTimeApiTest y mocks de dependencias nuevas en los dos slices HTTP15/16. No cambiar sus oráculos ni wiring real: éste pertenece a A. Paquete publisher congelado en informe propio.

Primer ciclo POST nominal EXTEND7: RED4dc661 antes de implementar ruta. Después apareció error de fixture7a6cca: csrf() agregó parámetro _csrf y el contrato rechaza queries; c6f8bf confirmó400query. Se corrigió exclusivamente fixture a csrf().asHeader(), siguiendo el helper heredado. GREENaa7243,74 casos (1nuevo+73HTTP15/16). No se presenta el fallo de fixture como defecto productivo. El oráculo contrasta EXTEND7/extension3 completo, estado original salvo revisión, µs antes/después1970, Location, no-store y llamada exacta al puerto sin lecturas auxiliares. Cantidad todavía nominal; validaciones/GET/replay siguen próximos ciclos individuales.

Ciclos 2–9, individuales y contra MockMvc real: ausencia REQUIRED RED1a034f→GREENa0b175; texto INVALID_TYPE REDd08613→GREENd9f0ed; fracción OUT_OF_RANGE REDd85f6d→GREEN verificado en end_http_04_green.log; cero RED5cbe99→GREEN38c5ff; 1441 REDc6720c→GREENfaf210; entero4294967311 que desbordaba a15 RED849d4c→GREEN3f2fc3. Cada rechazo verifica que ningún puerto se invoca. GET E exacto3/State6/cabecera revisión/no-store/sin ETag ni Location RED238041→GREENf48df0: 81 casos contando73 heredados. Se añadió el segundo @MockitoBean de lectura a los dos slices históricos sin cambiar assertions. Query E precede id inválido RED2d4122; resultado GREEN en end_http_09_green.log. Se mantienen logs separados por ciclo; no se confunde esta validación HTTP con persistencia, relojes ni concurrencia de A.

## Cierre del paquete HTTP

Ciclos 10–27 inicialmente GREEN, uno añadido y ejecutado antes del siguiente: replay200 (2e4e18); recuperación K (ef7174) y C (dd9e9c); anónimo E (b9be4b); revisión ausente antes de JSON (f45cb7); revisión malformada (6cb22e); JSON concatenado (6beaed); effectiveEndAt no admitido (471e26); identidad de token ajena preservada hasta la decisión (ba401b); E404 (cc9792), E409 temporal (866a66), E503 sin filtrar SQL (bfb2f9); frontera1 (bd1711), frontera1440 pausada (3fb808); CSRF (3e99ca); conflicto de key (bb2877); POST503 (9fd257); query P antes de ID (1882f7). Son adaptaciones de contratos existentes, no RED ficticios ni prueba de sus decisiones internas.

Root incorporó el checkpoint real 0d48abb durante este tramo. El resultado final estable se obtuvo después de esa incorporación: Spotless real y regresión conjunta EXIT0 **3b771b**, 100 casos (27 EndTimeApiTest,48 WorkSessionStateApiTest,25 CloseWorkSessionApiTest), cero fallos, errores o saltos. XML preservado en `progress/end_http_final_xml`; conteo y diffcheck **6f6f2f**. Manifiesto de los cuatro Java propios en `progress/end_http_freeze_hashes.json`. Spotless no modificó otros archivos versionados fuera de este paquete. Los dos tests históricos sólo añaden los mocks de los puertos nuevos.

### Mapa y límites

- @s1–2: representación EXTEND7, límites1/1440, estado running/paused, SessionStart intacta, µs y respuesta exacta. La fórmula de negocio pertenece a core/PG; aquí se verifica serialización y delegación.
- @s4: clases de validación REQUIRED/INVALID_TYPE/OUT_OF_RANGE, fracción y overflow sin coerción ni llamada a ningún puerto.
- @s5: E y P conectan queries, autenticación, CSRF, precedencia de revisión, JSON estricto y campos cerrados. Variantes restantes usan el mismo método y helpers de15, cuya regresión completa de48 casos se repitió.
- @s6–8: se conserva identidad completa del token y se traducen412/conflicto; replay200 y Location. El orden transaccional propiedad→identidad→replay no se acredita con mocks.
- @s13–14: E exacto3 con State6, revisión/no-store/sinETag/sinLocation;404 y409 temporal. Rango y lectura única del reloj pertenecen a A.
- @s16–17:503 de lectura y escritura conserva el problema y no fabrica resultado ni filtra excepción. Rollback y RR requieren las suites PostgreSQL.
- @s22–23: C/K seleccionan EXTEND7; se repiten73 casos HTTP15/16 para P/R6/CLOSE7. Reinicio/outbox retirado y durabilidad no se atribuyen a este slice.

No se ejecutaron integración SpringBoot, E2E, smoke, PIT, Stryker ni una suite global. No se cambió wiring, Store, core, configuración ni frontend. Paquete congelado para revisión independiente; publicación está integrada por root como2089cbd.
