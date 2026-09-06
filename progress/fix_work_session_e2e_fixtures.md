# Compatibilidad de fixtures E2E con sesiones de trabajo

Se añade únicamente `work_sessions` a las 18 listas `TRUNCATE` existentes en `e2e/*.spec.mjs`. Se conservan tablas anteriores, aserciones, rutas y formato; no se utiliza `CASCADE`. La clave foránea de V14 impide vaciar `tasks`/`projects` sin incluir la nueva tabla, aunque esté vacía.

## Evidencia PostgreSQL real

- Inventario: 18 sentencias, lectura `080a61`; edición limitada `24f9d3`.
- Contenedor propio `work-session-fixture-20260907`, PostgreSQL 17.9 Alpine, creado `34d879`, sin puertos publicados ni uso de servicios existentes.
- Todas las migraciones SQL actuales V1–V15 se aplicaron en orden con `psql -v ON_ERROR_STOP=1`: EXIT0 `a1f8e7`.
- Sentencia original de `create-task.spec.mjs`: EXIT1 `1670af`, `cannot truncate a table referenced in a foreign key constraint`, tabla `work_sessions` referencia `tasks`.
- Las 18 sentencias corregidas se extrajeron de los archivos y ejecutaron individualmente contra esa base: EXIT0 `03bf6f`. Se retiró exclusivamente el contenedor propio al finalizar.

Esta verificación acredita el arranque de las fixtures SQL sobre el esquema real; no acredita los recorridos de navegador ni la UI14. Root ejecutará el recorrido E2E desde el árbol de coordinación sin UI WIP. No se modificaron fuentes de aplicación, dependencias, configuración ni migraciones para este hotfix.

## Revisión y navegador sobre main

Root revisó las dieciocho diferencias completas en1328e0: sólo se añade la tabla al SQL de limpieza. Paquete ef7f423, aplicado sobre main f0f3b50 como788c2f6. PR10 mantiene el frontend anterior, sin incorporar UI14 incompleta.

Validación independiente Docker/PostgreSQL/backend/Chromium: cuatro recorridos existentes verdes en2fe098 (22,7 segundos de ejecución Playwright), con creación persistida tras recarga, reintento confirmado y dos recorridos de completar/reabrir. El fixture aislado organizationweb-e2e-43788 se retiró al terminar; los servicios ajenos se conservaron. No equivale a los98 E2E completos ni valida la UI14 nueva.

Incidente de invocación previo6a89ca: el filtro «real creation» no coincidía con ningún test; no se ejecutó ningún caso y no se contabiliza como RED de producto. Se consultaron los títulos reales en9167fa y se repitió con «confirmed task survives reload»; el comando seleccionó los cuatro casos registrados, no sólo el nominal de creación.

## Comprobación posterior del formato Java

La CI de main f0f3b50 pasó las suites de tests (1525 frontend; backend BUILD SUCCESSFUL), pero falló Spotless en tres fixtures Java. La pasada CLI GJF anterior no equivalía al resultado de Spotless. Tras el freeze del autor, root ejecutó su corrección mediante `gradlew spotlessApply spotlessCheck` en este árbol limpio: GREEN c3942b. Diff a8f10c verifica únicamente seis líneas de indentación en AvailabilityApiTest, ScheduleBlockApiTest y ScheduleBlockPersistenceTest; `git diff -w` vacío. No se modifican lógica ni aserciones. CI completa de PR10 pendiente tras integrar el formato.
