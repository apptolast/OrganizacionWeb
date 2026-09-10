-- Retirada de la feature 29: el conector de GitLab deja de existir. Esta migración deshace sólo lo
-- que era suyo. Lo que V29 cambió del recibo —el rename de repository a project_path, la columna
-- source y el índice por (owner_id, source, started_at)— NO se revierte: hoy es la forma del recibo
-- de la feature 27, su contrato ya se enmendó para casarla y desandarlo rompería lo que sigue vivo.

-- La tabla de conexiones sólo la escribía el conector de GitLab. Con él fuera nadie la lee, y
-- dejarla en pie mantendría tokens cifrados guardados sin ningún código capaz de rotarlos ni
-- borrarlos: un secreto sin dueño es peor que ningún secreto.
DROP TABLE IF EXISTS gitlab_connections;

-- Los enlaces de GitLab se borran antes de estrechar la comprobación. Es seguro porque el enlace
-- sólo dice «esta tarea vino de aquella issue»: la tarea, su historial y su proyecto siguen
-- intactos, y lo único que se pierde es la trazabilidad hacia un gestor que ya no se puede volver a
-- consultar. Sin borrarlos, el CHECK nuevo no podría validarse contra las filas existentes.
DELETE FROM task_external_links WHERE source = 'gitlab';

-- El origen vuelve a ser sólo github. No es cosmética: la comprobación es lo que impide que una
-- importación futura escriba un origen que ninguna pantalla sabe mostrar ni ninguna ruta sabe leer.
ALTER TABLE task_external_links DROP CONSTRAINT task_external_links_source_check;
ALTER TABLE task_external_links
    ADD CONSTRAINT task_external_links_source_check CHECK (source = 'github');
