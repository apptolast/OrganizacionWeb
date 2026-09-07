# Soporte PIT para historial18

Paquete acotado al dispatcher y DSL en árbol aislado. Propuesta aprobada por root (`7ef958`), 12 patrones completos de módulos nuevos y ApplicationConfiguration, todos los candidatos JUnit, 4 workers y umbral80.

- Test de despacho: RED `25f0a1` por target desconocido, GREEN `b79ac9`. Comando público `node .harness/harness.mjs mutate history-backend` entrega `pitest --no-daemon -PmutationScope=history` al backend, conforme al ejecutor existente.
- Test de alcance: RED `c8c747` por selector inexistente; GREEN `5138a0`, 42/42 tests Node. Verifica lista exacta, selección, JUnit, umbral, workers, inclusión en default y reporte separado.
- Formato focal de scripts y repetición Node: `ba2469`, EXIT0. Validación del DSL mediante `gradlew.bat pitest --dry-run -PmutationScope=history`: BUILD SUCCESSFUL, `:pitest SKIPPED`, sin campaña, sin compilar ni ejecutar suites Java.

Reporte configurado `backend/build/reports/pitest-history`; reportes previos, exclusiones y timeout/mutadores siguen vigentes. No Stryker18 ni rangos provisionales de UI. El scope final se contrastará de nuevo con los archivos de A antes de la campaña autorizada.

Freeze: scripts/project.mjs, scripts/project.test.mjs, backend/build.gradle.kts y esta bitácora. Hashes en history_pit_support_hashes.json. Ningún proceso Gradle activo al entregar.

## Corrección del scope default durante revisión

Root detectó que el default seleccionaba las clases nuevas pero no sus pruebas de adaptador. Nuevo oráculo RED `0624cd`; se añade únicamente HistoryApiTest y adapter.persistence.History*Test a los candidatos default, conservando ReadHistoryTest ya incluido por core. GREEN final `f9ffae`: 43/43 Node, formato focal y DSL dry-run EXIT0 con PIT SKIPPED. No se amplían otros ámbitos históricos ni se ejecuta mutación. Este resultado sustituye el freeze anterior de soporte; los logs originales permanecen.
