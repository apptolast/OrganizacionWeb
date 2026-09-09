-- Conector de GitHub (feature 27). Tres tablas: la conexión con su token cifrado, los recibos de
-- importación y el enlace duradero entre una tarea propia y la issue de la que salió.

-- Una conexión por propietario y proveedor. El token vive aquí y sólo aquí, siempre cifrado: la
-- comprobación de longitud descarta que alguien guarde texto en claro por error, porque el formato
-- es 1 byte de versión de clave + 12 de nonce + texto cifrado + 16 de etiqueta.
CREATE TABLE connector_connections (
    owner_id TEXT NOT NULL,
    provider TEXT NOT NULL CHECK (provider = 'github'),
    repository TEXT NOT NULL CHECK (char_length(repository) BETWEEN 3 AND 140),
    login TEXT NOT NULL CHECK (char_length(login) BETWEEN 1 AND 100),
    status TEXT NOT NULL CHECK (status IN ('valid', 'invalid')),
    token_ciphertext BYTEA NOT NULL CHECK (octet_length(token_ciphertext) BETWEEN 30 AND 284),
    connected_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (owner_id, provider)
);

-- Recibo de cada importación. Los CHECK impiden que exista un recibo incoherente aunque alguien
-- escriba en la tabla por fuera de la aplicación.
CREATE TABLE issue_import_receipts (
    id UUID PRIMARY KEY,
    owner_id TEXT NOT NULL,
    project_id UUID NOT NULL REFERENCES projects(id),
    repository TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('running', 'completed', 'failed')),
    created INTEGER NOT NULL CHECK (created >= 0),
    skipped INTEGER NOT NULL CHECK (skipped >= 0),
    failed INTEGER NOT NULL CHECK (failed >= 0),
    truncated BOOLEAN NOT NULL,
    error_code TEXT CHECK (error_code IS NULL OR char_length(error_code) BETWEEN 1 AND 64),
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
    CONSTRAINT issue_import_receipts_running_has_no_end
        CHECK ((status = 'running') = (finished_at IS NULL)),
    CONSTRAINT issue_import_receipts_end_after_start
        CHECK (finished_at IS NULL OR finished_at >= started_at),
    CONSTRAINT issue_import_receipts_running_has_no_error
        CHECK (status <> 'running' OR error_code IS NULL)
);

-- La exclusión mutua de @s25 es del esquema, no del código: dos importaciones simultáneas del
-- mismo propietario compiten por este índice y sólo una lo gana.
CREATE UNIQUE INDEX issue_import_receipts_one_running_per_owner
    ON issue_import_receipts (owner_id) WHERE status = 'running';

CREATE INDEX issue_import_receipts_owner_started
    ON issue_import_receipts (owner_id, started_at DESC, id DESC);

-- Enlace entre la tarea propia y la issue de la que procede. La clave primaria es la que impide
-- que reimportar duplique: un identificador externo por propietario y origen.
CREATE TABLE task_external_links (
    owner_id TEXT NOT NULL,
    source TEXT NOT NULL CHECK (source = 'github'),
    external_id TEXT NOT NULL CHECK (char_length(external_id) BETWEEN 1 AND 100),
    task_id UUID NOT NULL UNIQUE REFERENCES tasks(id),
    url TEXT NOT NULL CHECK (char_length(url) BETWEEN 1 AND 2000),
    linked_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (owner_id, source, external_id)
);
