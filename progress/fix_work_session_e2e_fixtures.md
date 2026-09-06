# Compatibilidad de fixtures E2E con sesiones de trabajo

Se añade únicamente `work_sessions` a las 18 listas `TRUNCATE` existentes en `e2e/*.spec.mjs`. Se conservan tablas anteriores, aserciones, rutas y formato; no se utiliza `CASCADE`. La clave foránea de V14 impide vaciar `tasks`/`projects` sin incluir la nueva tabla, aunque esté vacía.

## Evidencia PostgreSQL real

- Inventario: 18 sentencias, lectura `080a61`; edición limitada `24f9d3`.
- Contenedor propio `work-session-fixture-20260907`, PostgreSQL 17.9 Alpine, creado `34d879`, sin puertos publicados ni uso de servicios existentes.
- Todas las migraciones SQL actuales V1–V15 se aplicaron en orden con `psql -v ON_ERROR_STOP=1`: EXIT0 `a1f8e7`.
- Sentencia original de `create-task.spec.mjs`: EXIT1 `1670af`, `cannot truncate a table referenced in a foreign key constraint`, tabla `work_sessions` referencia `tasks`.
- Las 18 sentencias corregidas se extrajeron de los archivos y ejecutaron individualmente contra esa base: EXIT0 `03bf6f`. Se retiró exclusivamente el contenedor propio al finalizar.

Esta verificación acredita el arranque de las fixtures SQL sobre el esquema real; no acredita los recorridos de navegador ni la UI14. Root ejecutará el recorrido E2E desde el árbol de coordinación sin UI WIP. No se modificaron fuentes de aplicación, dependencias, configuración ni migraciones para este hotfix.
