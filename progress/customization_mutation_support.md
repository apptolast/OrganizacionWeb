# Soporte de mutación 21: alcance preparado, campañas pendientes

PIT usa el selector custom_views_fields-backend y mutationScope=custom_views_fields. Trece patrones incluyen familias completas domain.Customization*, domain.CustomField*, application.ReadCustomization*, SaveCustomization*, CreateCustomField*, UpdateCustomField*, ReadCustomFieldValues*, SaveCustomFieldValues*, Customization*, CustomFieldValues*, PostgresCustomizationStore*, CustomizationController* y ApplicationConfiguration. A confirmó que incluyen los archivos actuales y previstos, guardas, helpers e internos. Se conservan todos los candidatos JUnit, cuatro workers, heap, mutadores, timeouts y umbral80. El default añade esas clases y los tests de adaptadores Customization/CustomField y wiring; no se altera ningún ámbito histórico.

Stryker usa custom_views_fields-frontend y stryker.custom-views-fields.config.json. Incluye íntegros los cinco módulos confirmados por B: customization-api.ts, custom-fields-api.ts, customization-state.ts, customization.tsx y custom-fields.tsx. Ocho workers, perTest, vite.config.ts con todos sus candidatos, umbral80 y exclusión protegida heredada sin novedades. JSON/HTML y temporal propios. No se modifica el default histórico Stryker.

**No está listo para campaña frontend.** El montaje sigue WIP en B: faltan rangos reales de App.tsx, project-reader.tsx, project-tasks.tsx y task-reader.tsx. Root ratificó completar esos nodos/guardas tras diff final, sin inventar líneas ni incluir toda App1–20. Revalidar inventario definitivo y añadir los rangos antes de ejecutar. Los archivos nuevos UI aún no están copiados a este aislado. Esta configuración parcial prepara soporte; no acredita cobertura completa.

TDD del arnés, sin ejecutar mutadores:

- Dispatcher PIT: RED5f8b4a por target desconocido; GREEN9c586c.
- DSL, familias completas y tests default: RED509cce por scope ausente; GREENda042d.
- Dispatcher frontend: RED5a4f15 por target desconocido; GREEN39f272.
- Configuración heredada y cinco módulos completos: RED4d7c78 por archivo ausente; GREEN2c4a0c.
- Targets desconocidos o fuera de mutate: inicialmente GREEN121de5, sin llamadas al runner.
- Formato focal y scripts/project.test.mjs completo:60/60, cero fallos/skips, EXIT0 9deda6. Log progress/customization_scope_final.log; se conservan los logs RED/GREEN de cada ciclo.

Comandos futuros, sujetos a freeze/revisión y autorización de campaña: `node scripts/project.mjs mutate custom_views_fields-backend` y `node scripts/project.mjs mutate custom_views_fields-frontend`. Reportes backend/build/reports/pitest-custom-views-fields y frontend/reports/mutation-custom-views-fields. No se ejecutó Gradle, PIT, Stryker, Vitest ni E2E. HTTP134 permanece intacto. Freeze separado en customization_mutation_support_freeze.json.

Revisión root: el default omitía CustomizationWiringTest, que verifica los beans reales21. Nuevo oráculo detectó la omisión (RED5af5ee EXIT1); añadida esa clase, GREENb10ce3 EXIT0. Se conserva ApplicationWiringTest porque ApplicationConfiguration compartida está íntegramente en alcance y sus beans históricos necesitan sus oráculos. Formato focal y regresión del arnés:61/61 GREEN, EXIT0; log customization_scope_wiring_final.log. No Gradle ni mutación. El resultado60 anterior permanece preservado con su límite.
