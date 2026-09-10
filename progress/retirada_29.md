# Retirada de la feature 29 (conectores adicionales / GitLab)

Decision del propietario, tomada con las consecuencias delante. Este documento
registra que se borro, que se conservo y por que, la migracion y su razon, y los
agujeros que quedan declarados.

Carril: worktree `C:/Users/vhurt/ow-worktrees/retirar-29`, rama `claude/retirar-29`.

## Plan por pasos (suite verde en cada uno)

1. Backend — catalogo de conectores (`ConnectorCatalog`, `ConnectorRow`,
   `ReadConnectorCatalog*`, `ConnectorStatusSource` y sus seis implementaciones,
   `ConnectorCatalogController`) y su cableado.
2. Backend — conector de GitLab (aplicacion, dominio, adaptadores HTTP,
   persistencia y red) y su cableado.
3. Base de datos — migracion V32.
4. Frontend — pantallas, cliente, rutas y navegacion.
5. E2E y contrato.

(el detalle de cada paso se rellena segun se ejecuta)
