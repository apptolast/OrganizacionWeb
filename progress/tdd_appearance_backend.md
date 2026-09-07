# TDD backend apariencia20

Ponytail full/Caveman lite. Init root25384 EXIT0/14625f heredado para este corte. No HTTP, frontend ni cambios de migraciones previas.

## Primer bundle nominal, no cierre funcional

1. @s1 ReadAppearanceTest.s1_readsConfirmedAbsenceForTheAuthenticatedOwner. Incidente81bf7d: ruta backend/backend errónea, no se creó test y runner no encontró test; NO RED contractual. Corregida ruta. RED real a6e81a (clase ausente), progress/appearance_01_red_actual.log → GREENc70bb7, appearance_01_green.log.
2. @s2 ReadAppearanceTest.s2_readsTheStoredValuesAndRevisionWithoutChangingThem. RED6607ae (record sin campos)→GREEN87925d (2/2); logs appearance_02_red/green.log. Resultado Optional real, sin Clock ni defaults fingidos en dominio; HTTP mapeará ausencia.
3. @s2 SaveAppearanceTest.s2_createsTheFirstPreferenceWithCanonicalColorsAndMicroseconds. RED1b6abe (tipos ausentes)→GREENbd03e9. Campos exigidos por test, UUID propio, versión0, mayúsculas, instante truncadoµs. Puerto callback sin DB simulada como evidencia PG.
4. @s6 SaveAppearanceTest.s6_rejectsARevisionWhoseIdentityDoesNotMatchTheOwnersRow. RED92ab2a (excepción ausente)→GREENf6eda8 conjunto4/4. Excepción pública real para HTTP412.

Formato focal y regresión fc1b25 (logs appearance_bundle_format/green.log); regresión UP-TO-DATE del mismo corte, no cuatro ejecuciones adicionales. Manifiesto progress/appearance_first_bundle.json contiene9fuentes+2tests.

Firmas: ReadAppearanceUseCase.get(String owner)→Optional<Appearance>; SaveAppearanceUseCase.execute(String owner, AppearanceRevision expected, String theme, String accentLight, String accentDark)→Appearance. Appearance(id UUID,owner String,theme String,accentLight String,accentDark String,version long,updatedAt Instant); AppearanceRevision(UUID id,long version). AppearanceConflictException local412; ValidationException/StorageUnavailableException existentes se reutilizarán.

Pendiente explícito: theme/formato/contraste y errores ordenados; comparación versión/ausencia, no-op y cambios existentes, overflow/Clock/rango; Store PG/concurrencia/rollback; V19 y wiring. SaveAppearance actual sólo es nominal de alta y guarda identidad de fila presente; NO usarlo aún como implementación completa20. No stubs HTTP ni fuente de persistencia parcial en este bundle. Root revisará/commiteará checkpoint para C antes de continuar estos archivos.

## Validación independiente mientras root conserva el primer bundle

Archivos nuevos AppearanceValues/AppearanceValuesTest; ninguno de los11archivos congelados fue modificado.
5. @s10 tema inválido antes de ambos acentos: REDcd130a→GREEN7f4d65.
6. @s10 formato claro antes de oscuro: REDbfc635→GREEN9e954e.
7. @s11 contraste claro pese a DARK: RED9b96b1→GREEN1a067f.
8. @s10 formato oscuro tras claro válido: RED87d007→GREENf29230.
9. @s11 contraste oscuro pese a LIGHT: RED1745d0→GREENd5a5c5.
10. @s2 defaults minúsculos canónicos y válidos contra los fondos reales: RED0d53a3→GREENdb62e2 (6/6dominio). Logs appearance_05..10_red/green.log. Fórmula y seis fondos de cada tema implementados sólo en dominio, aún sin conectarlos al caso de uso congelado. No se atribuye validación HTTP/PG todavía.

## Casos de uso tras liberación dc374b0

Corrección de evidencia de formato: el hook focal de fc1b25 terminó sin reformatear las líneas del bundle; no se usa como garantía. Formato real spotlessJavaApply8d791d y, tras ciclos posteriores, spotlessJavaApply+test27e6da completados. No cambió producción ajena20.
11. @s10 dominio integrado antes de almacenamiento: REDbf88ca→GREENe462a9.
12. @s3/@s35 no-op versiónMAX sin interacción Clock: RED0bf101→GREEN423cb3.
13. @s4 stale no se disfraza de no-op: RED96d5b1→GREEN01a1ef.
14. @s6 tag configurado contra ausencia propia: RED60be40→GREEN218057.
15. @s2 cambio conserva ID, incrementa y no retrocede timestamp: RED8ee5a7→GREEN311f57.
16. @s35 cambio real versiónMAX→503 sin wrap: REDf0a3f4→GREEN3181e0.
17. @s35 relojUTC fuera de1..9999 (dos extremos): RED6e0420→GREEN083ca4.
18. @s35 falloClock→503: RED64bbc9→GREENf09d8a.
Logs appearance_11..18_red/green.log. Regresión core y formato real27e6da en appearance_core_green.log. No evidencia PG aún. Segundo corte de fuentes dominio/app mantiene firmas públicas; preparado para que C use validación real, mientras A inicia PG en nuevos archivos.

## Persistencia real en nuevos archivos

19. @s1 ausencia sin INSERT: RED7d13d0→GREENfed6bc; añade V19 nueva y Store consultaowner, JDBC OffsetDateTime.
20. @s2/@s8/@s9 alta real, instancia nueva, aislamientoowner, µs en año0001 y sinoutbox: RED0835ee→GREENade3fb. READ_COMMITTED explícito en plantilla propia; alta nominal únicamente.
21. @s2 actualización mismaID/version+1 y una fila: RED944899→GREEN8f77c6.
22. @s3 no-op no ejecuta UPDATE, trigger de rechazo como oráculo: RED6e11e9→GREENbed9ba.
23. @s16 tabla temporalmente inaccesible→503 y nunca ausencia: REDed9a19→GREENf92243.
24. @s35 fila con contraste inválido pero CHECK SQL compatible→503: RED83d03e→GREEN391edb, reutiliza AppearanceValues al mapear.
Logs appearance_19..24_red/green.log. Store aún WIP: pendientes bloqueo/carreras, todas filasafectadas/commiterror, integridad de metadata y wiring. No freeze ni aprobación funcional atribuida a estos nominales.

25. @s35 timestamp persistido año10000→503: RED40b382→GREEN0967cc.
26. @s7 UPDATE suprimido→503 y fila intacta: RED1891f9→GREEN377f52.
27. @s7 error de commit diferido real PostgreSQL revierte alta: RED198ae3→GREEN270798.
Refactor de campos solicitado por C: validadores static theme/light/dark reutilizados por constructor para que HTTP valide cada campo antes de extraer el siguiente; GREEN17/17f0da1e, fuente versionada8482d10. No se inventó RED de refactor.
28. @s11 vector independiente #645F61, contraste mínimo4.499799974 contra#D0DFC9: inicialmenteGREENff88af; no producción modificada ni RED atribuido.
29. @s5 dos altas desde ausencia mediante barrera de ambas lecturas: REDd4a7f2→GREEN188dc3. Intento intermedio16c864 fallócompilación por sustitución textual que duplicó declaración; preservado appearance_29_green.log; GREENreal appearance_29_green_actual.log. Un ganador y un conflicto, ganador no prefijado.
30. @s7 INSERT suprimido sin ganador durable→503: RED59b58e→GREEN7a3c9a. Distingue la carrera legítima del ciclo29 de un trigger que descarta la escritura.
31. @s5 dos cambios esperan lock propio antes de Clock y releen revisión: RED96cc14→GREEN19b00d. PostgreSQL pg_stat_activity acredita2esperasLock antes de liberar transacción retenedora; Clock0antes/1después, un200aplicación y unconflicto sin ganador prefijado. No conteosHTTP atribuidos al testPG.
32. Wiring nominal Read/Save comparten adaptador real: RED850872→GREENcdc257. JdbcTemplate/transaction manager simulados en ApplicationContextRunner; NO HTTP+PostgreSQL real por este test. La evidencia PG pertenece a AppearancePersistenceTest.
33. @s35 metadata corrupta seleccionada: RED96d619→GREEN2af4c8 (6ejemplos). Tema inválido ya rechazaba; restantes NULL/canonicalización/versionnegativa/idNULL quedan detectados. Vista temporal controlada simula corrupción que constraints normalmente impiden, no se afirma que el DDL permita filasNULL/enum inválido. Tabla original restaurada enfinally. Timestamp extremo/contraste incoherente reales se probaron por separado.
34. @s9 upgrade V18→V19 en esquema aislado: inicialmenteGREENa07dd4. Preserva columnas de todas tablas anteriores y datos de proyecto representativo; tabla nueva vacía y ningún evento. No se alteran ni reescriben migraciones anteriores. No es prueba exhaustiva de restauración de todos hechos1–19.

## Freeze backend funcional para revisión

spotlessJavaApply + test focal/regresión e51b75 EXIT0, progress/appearance_backend_final.log. XML conservados en progress/appearance_backend_final_xml/:70ejecuciones =24ApplicationWiring+7ProjectStateConfiguration+19PG+2Read+11Save+7Values. Cero fallos/errores/omitidos. Son conteos de pruebas, no37escenarios ni112ejemplosdelGherkin. Manifiesto18fuentes/pruebas/SQL en appearance_backend_final_manifest.json, SHA A405AC5D1AB488B7D94910231645FCB524F78E951BED00BE470A8F9A7ED01416.

### Mapa compacto de evidencia A

- @s1: ReadAppearanceTest.s1 + AppearancePersistenceTest.s1 + wiring appearance_s1_s2; defaults/ETag/no-store quedan HTTP.
- @s2: Read.s2,Save.s2_creates/s2_updates,PG.s2_s8_s9/s2_updates; exactitud de GET/PUTHTTP es C.
- @s3: Save.s3_s35 yPG.s3(noUPDATE contrigger); @s4 Save.s4; @s5 PG.s5_twoCreations y s5_updatesWait; @s6 Save.s6 amboscasos+owneraisladoPG.s2_s8_s9.
- @s7: PG.s7_suppressedUpdate/s7_suppressedInsert/s7_deferredCommitFailure; @s8 PG.s2_s8_s9 acredita reconstrucción del Store sobreDBdurable, NO reinicio de proceso/navegador ni respuesta perdida real.
- @s9: PG.s2_s8_s9 y s9_additiveUpgrade; ausenciaoutbox medida, sin refactor de historial.
- @s10: Values.s10 y Save.s10; tiposJSON/ausencia/extra/ordenmezclado entre campos son C con validadores reales. @s11: Values.s11 ambos temas + vector umbral inicialmenteGREEN.
- @s12–15: adaptadorHTTP fueraA; @s16: PG.s16 para503 (seguridadHTTP C).
- @s35: Save.s35/no-opMAX y PG.s35 (contraste/rango/metadata con vista controlada), límites temporales ydeversión explícitos.
- UI/decoder/restantes@s17–34/36–37: B/C, no atribuidos a mocks o dominio A. Sin declaración de aprobación feature ni gatesglobales/mutación/E2E.

Pendientes para integración de equipo: HTTP+C yfrontendB, pruebaHTTPPG si el juez la exige como ruta integrada, gatesglobales y mutación posteriores. Ninguna campaña nueva ejecutada. No hay proceso Gradle activo deA alfreeze.
