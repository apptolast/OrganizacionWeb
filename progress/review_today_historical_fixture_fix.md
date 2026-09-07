# Corrección de evidencia histórica Today tras16

El init global detectó dos fallos reales de arnés (root65f76f): las pruebas de selección/replay de Today comparaban sus rangos y hashes históricos con App/use-session actuales, modificados legítimamente por16. Reproducción focal37c29a EXIT1; no falló una campaña nueva ni se invalida retrospectivamente el informe original.

La lectura0fc1c7 comprobó que los seis hashes de `today_frontend_mutation_scope.json` y los cuatro de `today_frontend_replay_selection.json` coinciden exactamente con los blobs Git de `1f7090eb6a114194622d45184a84be4904f302bb`. Con autorización root se extrajeron sólo esas seis fuentes y `frontend/stryker.config.json` del mismo commit a un único `progress/today_frontend_historical_snapshot.json`. Cada entrada conserva path, SHA256 y bytes base64 para evitar conversiones CRLF de checkout. El default histórico tiene SHA256 `42f5684c14952730c9af830a2aed9c6535ed36bd2bb2c07dc3523e3cde9ac017`.

Las comprobaciones de Today leen esos bytes, verifican procedencia/hash y mantienen las comparaciones originales de expresiones, ubicaciones, IDs, selección, exclusiones y umbrales. El chequeo de su default usa el default histórico; el test final de Today también lee el mismo snapshot para no repetir este defecto al modificar Today en otra feature. No se requiere Git ni red durante los tests, por lo que funciona en checkout superficial de CI.

Manifiestos, configuraciones Today y reportes de medición/replay originales permanecen intactos. La cobertura de código actual16 se comprueba separadamente mediante su configuración de archivos completos; el snapshot no convierte aquellos resultados en una medición del código nuevo. Las configuraciones antiguas basadas en rangos requieren su checkout histórico para reproducir aquella campaña.

GREEN inicial f9bdf9 y final con formato/procedencia eddc77:36/36 Node, cero fallos/skip. Logs `today_historical_fixture_red.log`, `today_historical_fixture_green.log`, `today_historical_fixture_final.log` y formato. No se ejecutaron PIT/Stryker, init global ni E2E para esta corrección. Sólo script de tests, snapshot y este documento; producción sin cambios.

Revisión independiente root: APPROVED, diff55e83d. En18beed se compararon los siete buffers decodificados byte por byte contra `git show` del commit declarado y sus SHA256; todos coinciden. Los manifiestos y configuraciones Today originales no tienen diff. Esta corrección conserva evidencia histórica y elimina una dependencia incorrecta del checkout actual, sin afirmar una nueva campaña de Today.
