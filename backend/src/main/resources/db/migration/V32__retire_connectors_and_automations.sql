-- Retirada de tres features: 27 (conector de GitHub), 29 (catálogo de conectores y GitLab) y
-- 30 (automatizaciones). Una sola migración para las tres, que es más fácil de revisar y de
-- revertir que tres seguidas.
--
-- Antes de escribirla se comprobó tabla por tabla, con grep sobre backend, frontend, E2E y scripts,
-- que ninguna feature superviviente las lee. Las que se quedan y NO se tocan: las de webhooks
-- (25), las del calendario externo (28), y `import_receipts`, que pese al nombre parecido es de la
-- importación de datos del propietario y no tiene nada que ver con las issues.

-- ---------------------------------------------------------------- 27 y 29: conectores de issues

-- El enlace va primero porque apunta a `tasks`. Borrarlo no toca ninguna tarea: el enlace sólo
-- decía «esta tarea vino de aquella issue», y lo único que se pierde es la trazabilidad hacia un
-- gestor que ya no se puede consultar.
DROP TABLE IF EXISTS task_external_links;

-- Recibos de importación de issues. No los lee nadie más: la pantalla que los mostraba se va con
-- su conector.
DROP TABLE IF EXISTS issue_import_receipts;

-- Las dos tablas de conexión guardaban tokens cifrados. Dejarlas en pie mantendría secretos
-- almacenados sin ningún código capaz de rotarlos ni de borrarlos, que es peor que no tenerlos:
-- nadie podría responder a un «bórrame el token» porque ya no existe la ruta que lo hacía.
DROP TABLE IF EXISTS connector_connections;
DROP TABLE IF EXISTS gitlab_connections;

-- --------------------------------------------------------------------- 30: automatizaciones

-- Las ejecuciones primero, que referencian a las reglas.
DROP TABLE IF EXISTS automation_runs;
DROP TABLE IF EXISTS automation_rules;
DROP TABLE IF EXISTS automation_cursors;
