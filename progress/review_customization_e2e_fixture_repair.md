# Reparación de fixtures E2E tras V20

Origen: root comunicó CI `34160728290`, commit `328b505`, con 1122 fallos entre 2398 pruebas Java por TRUNCATE que omitía las nuevas FK. A reproduce y corrige Java en COMMON. Este paquete sólo modifica fixtures E2E en el aislado; no reproduce ese fallo ni afirma ejecución E2E verde.

Se leyó la V20 real en COMMON: `project_custom_field_values.project_id` referencia projects y `task_custom_field_values.task_id` referencia tasks. Las preferencias de configuración son independientes de esas FK. No se alteró la migración.

## Delta

- 42 sentencias TRUNCATE en 32 archivos E2E incluyen explícitamente ambas tablas de valores en la misma sentencia que proyectos/tareas. No se añade CASCADE ni se quitan restricciones, tablas anteriores u oráculos.
- `e2e/support/authenticated-test.mjs` limpia sólo filas de las tres tablas21 con `owner_id='e2e-user'`, antes y en finally después de cada fixture autenticado. Reutiliza `sql`, que exige proyecto `organizationweb-e2e-*` y fichero de entorno del runner aislado. Conserva filas de otros owners y datos de negocio; no hay acceso a producción.
- `rg` en e2e/scripts no encontró TRUNCATE SQL en scripts; sus opciones `truncate` son límites de salida y no se modificaron.

## Verificación realizada

Formato focal con Prettier instalado: EXIT0 `5c2fc7`. `node --check` sobre los 33 módulos y `git diff --check` correctos. Siete archivos históricos necesitaban también formato del beforeEach; no cambian sus oráculos.

Auditoría AST `dc925c` EXIT0: los 32 archivos de pruebas coinciden exactamente con el AST de HEAD tras aplicar únicamente la sustitución prevista a las 42 sentencias SQL, ignorando posiciones y formato. El fixture autenticado se revisa separadamente porque incorpora el before/finally. El primer intento de auditoría no encontró el alias directo de Babel en pnpm (`038040`); se usó el parser 7.29.8 ya instalado, sin instalar dependencias ni modificar configuración.

Los hashes previos están en `customization_e2e_fixture_before.json`; la comprobación AST en `customization_e2e_fixture_validation.json`; el inventario final en `customization_e2e_fixture_freeze.json`. Los tres archivos HTTP congelados permanecen intactos.

No se ejecutó Gradle, E2E, SQL ni Docker en esta reparación. Requiere stack integrado con V20 y beans reales antes del runner. El GREEN operativo y CI quedan pendientes de root; esta comprobación sólo acredita el delta estático, formato y sintaxis.
