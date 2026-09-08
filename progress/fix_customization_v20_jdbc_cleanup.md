# Reparación de limpiezas JDBC tras V20

CI original 34160728290 sobre328b505: root observó1122 fallos de2398 en fixtures JDBC. La reproducción local ejecutó sólo AuthenticationHttpTest.s1_anonymousSessionIsPublicExactAndPersisted: EXIT1 e21b09, SQLSTATE0A000 porque task_custom_field_values referencia tasks. Log/XML originales customization_v20_fixture_auth_red conservados. Es un defecto del montaje de pruebas ante la nueva FK, no se elimina la restricción.

Inventario:24 archivos de backend/src/test con TRUNCATE de proyectos/tareas sin CASCADE. Se añaden explícitamente project_custom_field_values y task_custom_field_values a cada misma sentencia. WorkSessionStoreTest conserva su CASCADE histórico y no necesita este arreglo; limpiezas independientes de outbox, appearance y spring_session permanecen intactas. No se añade CASCADE, se suprimen asserts ni se cambia producción.

Auditoría estructural326597: al retirar exclusivamente el prefijo de las dos tablas de cada archivo corregido, los24 coinciden con HEAD (normalización de finales de línea únicamente). Lista y hashes en customization_v20_fixture_audit.json; before en customization_v20_fixture_before.json.

Primer foco representativo y formato real: EXIT0 07029d,8 suites257 tests, cero fallos/errores/skips. Clases y XML en customization_v20_fixture_first_results.json y customization_v20_fixture_first_xml. Incluye6 fixtures afectados más HTTP21 y Wiring. Los18 afectados restantes se derivan por nombre de clase exacto, no restando el número total de suites; lista customization_v20_fixture_remaining.json. Root integrará el controlador de definiciones de C antes del segundo foco. Aún no se afirma cierre de la reparación hasta ese resultado.

## Cierre local

Segundo foco tras integrar55e9c46: EXIT0 ce8023,20 suites941 tests en1m43s, cero fallos/errores/skips. Incluye los18 fixtures restantes y CustomizationApiTest actualizado43 más Wiring2. XML originales separados en customization_v20_fixture_remaining_xml y conteos en customization_v20_fixture_remaining_results.json.

Mapa exhaustivo clase→XML: customization_v20_fixture_class_xml_map.json verifica los24 archivos modificados y1121 tests, todos verdes. Añadiendo HTTP21 final43 y Wiring2 hay1166 pruebas distintas acreditadas; no se suman como distintas las repeticiones de HTTP/Wiring entre ambos focos. Manifest final24 hashes: customization_v20_fixture_final_manifest.json. No fue una campaña global; se ejecutaron todas las clases cuyo montaje cambió más la integración21 actual. Fuente y asserts históricos permanecen iguales salvo los24 prefijos SQL ya auditados.

Se entrega freeze a root para revisión/commit/CI. CI remota correctiva y producción21 completa siguen pendientes; este informe acredita únicamente reparación local de los fixtures JDBC afectados.
