-- Segundo gestor de issues (feature 29). GitLab entra por el mismo puerto que GitHub, así que el
-- recibo y el enlace dejan de ser de un solo proveedor y aparece una tabla propia de conexión.

-- El recibo pasa a nombrar su origen. La columna existente describía un repositorio de GitHub; lo
-- que de verdad guarda es la ruta del proyecto en el gestor, que es lo que GitLab también tiene.
ALTER TABLE issue_import_receipts RENAME COLUMN repository TO project_path;
ALTER TABLE issue_import_receipts ADD COLUMN source TEXT NOT NULL DEFAULT 'github';
ALTER TABLE issue_import_receipts ALTER COLUMN source DROP DEFAULT;
ALTER TABLE issue_import_receipts
    ADD CONSTRAINT issue_import_receipts_source_check CHECK (source IN ('github', 'gitlab'));

-- El índice del catálogo pasa a ser por propietario y origen: la pantalla de cada conector busca
-- su última importación, no la del otro. El índice parcial que arbitra la exclusión mutua sigue
-- siendo sólo por propietario, porque el guardián es por persona y no por proveedor.
DROP INDEX issue_import_receipts_owner_started;
CREATE INDEX issue_import_receipts_owner_source_started
    ON issue_import_receipts (owner_id, source, started_at DESC, id DESC);

-- La unicidad del enlace ya era por (owner_id, source, external_id); lo único que cambia es que
-- 'gitlab' pasa a ser un origen admitido, de modo que la misma issue 42 de dos gestores distintos
-- son dos enlaces hacia dos tareas distintas.
ALTER TABLE task_external_links DROP CONSTRAINT task_external_links_source_check;
ALTER TABLE task_external_links
    ADD CONSTRAINT task_external_links_source_check CHECK (source IN ('github', 'gitlab'));

-- Conexión de GitLab: una por propietario. El token vive aquí y sólo aquí, siempre cifrado con el
-- formato de 27 —1 byte de versión de clave + 12 de nonce + texto cifrado + 16 de etiqueta—, y la
-- comprobación de longitud descarta que alguien guarde texto en claro por error. La base de la API
-- no se guarda a propósito: es configuración del servidor, y guardarla por fila reabriría la
-- superficie SSRF que 27 cerró.
CREATE TABLE gitlab_connections (
    owner_id TEXT PRIMARY KEY,
    project_path TEXT NOT NULL CHECK (char_length(project_path) BETWEEN 3 AND 255),
    project_id BIGINT NOT NULL CHECK (project_id > 0),
    token_hint TEXT NOT NULL CHECK (char_length(token_hint) BETWEEN 1 AND 4),
    status TEXT NOT NULL CHECK (status IN ('connected', 'error')),
    token_ciphertext BYTEA NOT NULL CHECK (octet_length(token_ciphertext) BETWEEN 30 AND 229),
    last_activity_at TIMESTAMPTZ NOT NULL,
    last_error_code TEXT CHECK (last_error_code IS NULL OR char_length(last_error_code) BETWEEN 1 AND 64),
    last_error_at TIMESTAMPTZ,
    version BIGINT NOT NULL CHECK (version >= 1),
    CONSTRAINT gitlab_connections_error_is_complete
        CHECK ((last_error_code IS NULL) = (last_error_at IS NULL))
);
