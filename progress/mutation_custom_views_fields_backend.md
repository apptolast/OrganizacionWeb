# Mutación backend21: original y refuerzo acotado

Original autorizada después de revisión funcional. Comando `node scripts/project.mjs mutate custom_views_fields-backend`, sesión 83039, inicio 2026-09-07T22:39:38Z, EXIT 0 f03136. Gradle 16m43; PIT 16m36. Configuración: 13 patrones, todos los candidatos JUnit, cuatro workers, umbral 80, timeout 15000 ms; exclusiones heredadas intactas.

Resultado original: **378 KILLED / 386 = 97.92746113989638%**, 3 SURVIVED y 5 NO_COVERAGE; cero errores, timeouts u otros estados. 423 candidatos JUnit, 1701 ejecuciones de pruebas, líneas 665/670. El resultado supera el umbral sin quitar residuos del denominador.

XML/HTML completos preservados en `progress/customization_pit_original_report/`. XML SHA256 `57D22D323CCB012971E6E5BB0B608AB15DC9341E104AAF80B779B45D1DD2B6E6`; log original SHA256 `5F058210E51D1AA82E9BDED08F3D4590E3930F2C89627BAB9E05CAD2E3250414`. Inventario original before de 437 inputs SHA256 `772CD298C6AEB05658B836FABA1421FD1067368DAFDA623E7F0486FD578D28AB`; after idéntico. Comprobación adicional de universo sin nuevas fuentes/recursos/wrapper en `customization_pit_original_universe_check.json`. `customization_pit_original_artifacts.json` fija hashes de todos los originales preservados.

Inventario por clase y estados exactos: `customization_pit_original_results.json`. Distribución: ApplicationConfiguration 46K; Controller 100K, CustomizationResponse 5K, ValuesResponse 4K; Store 66K; Create 20K; Time 5K; ReadValues y ReadCustomization 1K cada uno; SaveValues 23K/1S; SaveView 15K; Update 18K; Definition 5K; Input 27K/1S; Label 10K/1S; Value 4K; Values 11K; Collection 4K; ValuesRevision 4NC; Customization 7K; Revision 2K; View 4K/1NC.

## Tres supervivientes observables

Las firmas completas, descriptores e índices originales están en `customization_pit_original_residues.json`.

- `CustomFieldInput.lambda$canonical$0`, línea 26, ConditionalsBoundaryMutator, índice 10/bloque 2: cambiar el extremo inclusivo U+DFFF permite un sustituto aislado prohibido en TEXT. El oráculo anterior cubría U+D800, no el extremo superior.
- `CustomFieldLabel.lambda$new$0`, línea 10, mismo mutador/índice/bloque: idéntica frontera observable en label.
- `SaveCustomFieldValues.lambda$save$5`, línea 83, BooleanTrueReturnValsMutator, índice 7/bloque 2: aceptar siempre el timestamp previo impide avanzar updatedAt cuando el único Clock es posterior. El caso previo sólo exigía no retroceder.

## Cinco NO_COVERAGE conservados

NullReturnValsMutator en `CustomizationView.scope()` (línea 6) y `CustomFieldValuesRevision.entityId/schema/scope/values()` (línea 5); todos índice 5/bloque 0, cero pruebas ejecutadas. Son accesores públicos alcanzables, no código imposible ni equivalencias generales. En el flujo actual View se construye para validar y se consume visibleFields; SaveValues construye la revisión compuesta y la compara mediante equals del record, que usa campos y no estos accesores. Los DTO públicos de salida usan otros records cubiertos. Probar sólo getters no añade aquí un oráculo contractual útil. Se mantienen cinco NC y el denominador 386; no se propone excluirlos ni atribuirles cobertura indirecta.

## Refuerzos autorizados, sin producción

Tres ciclos individuales, inicialmente GREEN; todavía no hay RED bajo mutante ni kill acreditado por estos refuerzos:

1. TEXT U+DFFF, ejemplo adicional en `CustomFieldValuesTest`: EXIT 0 ea9915, `customization_refinement_text_green.log`.
2. Label U+DFFF, ejemplo adicional en `CustomFieldCommandsTest`: EXIT 0 7a1a39, `customization_refinement_label_green.log`.
3. `SaveCustomFieldValuesTest.s22_valueChangeAdvancesToLaterClockWithMicrosecondPrecision`: reloj posterior con nanos, salida truncada a microsegundos, revisión incrementada y una única consulta Clock. EXIT 0 b55990, `customization_refinement_time_green.log`.

Regresión sólo de las tres clases y formateador real: `gradlew.bat spotlessJavaApply test --tests '*CustomFieldValuesTest' --tests '*CustomFieldCommandsTest' --tests '*SaveCustomFieldValuesTest' spotlessJavaCheck`, EXIT 0 554ff8. XML preservados en `customization_pit_refinement_xml/`: 34 + 27 + 16 = **77 pruebas**, cero fallos/errores/skips. Manifiesto de tres tests en `customization_pit_refinement_manifest.json`; resultados en `customization_pit_refinement_results.json`. No cambios productivos ni de build.

## Propuesta de replay, pendiente de revisión y autorización

Un init separado limitaría targetClasses a CustomFieldInput, CustomFieldLabel y SaveCustomFieldValues; mutators CONDITIONALS_BOUNDARY y TRUE_RETURNS. Sin nuevos filtros de métodos ni exclusiones. Conserva todos los candidatos JUnit, 4 workers, umbral 80 y timeouts existentes; reportDir propio `pitest-customization-replay`, originales intocables. Comando propuesto desde backend: `gradlew.bat pitest -PmutationScope=custom_views_fields --init-script ../progress/customization_pit_replay.init.gradle`.

El cruce literal del XML original produce **15 mutantes esperados: tres objetivos SURVIVED y doce extras KILLED originales**, inventariados con clase/método/descriptor/línea/mutador/índice/bloque en `customization_pit_replay_proposed_inventory.json`. Se comprobará el inventario real completo y cada objetivo por firma, nunca por ordinal. Se preservarán todos los estados y EXIT aunque falle el umbral; no se agregará este score al original. No se ha creado ni ejecutado el init: root revisará primero este paquete. No se solicita una nueva campaña completa.
## Replay dirigido autorizado y terminado

Root aprobó los tres refuerzos y bytes del init SHA256 2769EC8CCE65CACA88A08DE44D54B12FA6E670013BA1089322F9471D60D49C18 (c92819/401098). Se ejecutó exactamente una vez el comando propuesto, sesión4635, EXIT0 9c02f6, BUILD SUCCESSFUL en4m28. Conservó423 candidatos JUnit,4workers,80 y los timeouts/exclusiones heredados.

Resultado separado: **15/15 KILLED**, cero SURVIVED/NO_COVERAGE/errores/timeouts. Coinciden las15 firmas previstas completas:3 objetivos y12 extras. Cada objetivo fue eliminado por su nuevo oráculo: TEXT U+DFFF invocación4 de s12_invalidTextReportsValueErrorWithoutTrimming; label U+DFFF invocación4 de s4_invalidLabelIsRejectedBeforeReadingConfiguration; avance temporal por s22_valueChangeAdvancesToLaterClockWithMicrosecondPrecision. Este es el contraste bajo mutante; sus ejecuciones originales fueron inicialmente GREEN.

Los438 inputs del replay (437 anteriores+init) permanecen idénticos antes/después. XML/HTML preservados en progress/customization_pit_replay_report/. XML SHA256 55BB9C3525C7514DD2CF9A6542A81953036EB648E3833A73ADFCB59C75177478; log SHA256 0907D298ADAB988C42A6C8CA4C1083089E71D42F3B7744A5EA2E4AE8E5AD0CEA. Inventario real y killingTests en customization_pit_replay_mapping.json; resumen en customization_pit_replay_results.json; hashes de artefactos en customization_pit_replay_artifacts.json.

El score original permanece378/386=97.92746113989638%; no se sustituye por el100% dirigido ni se recalcula agregando campañas. Los cinco accesores NO_COVERAGE conservan su estado original y límites descritos. No se necesita otra campaña para este paquete. Gradle liberado; producción/tests/config no cambiaron durante replay.