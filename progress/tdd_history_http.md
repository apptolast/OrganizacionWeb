# Arranque del adaptador HTTP de historial18

Árbol aislado `OrganizacionWeb-history-http`, corte inicial `75a2381`, contrato `features/history.feature` SHA256 `768AA48A5A0495BDC5AA292395F1DC42DADD0BD7702987F50D2FAD4664B72C13`. Propiedad de C: HTTP/cursor y sus pruebas; sin modificar dominio, PostgreSQL, wiring o fuentes comunes. Ponytail full y Caveman lite aplicados.

## Verificación del entorno

- Primer `node .harness/harness.mjs init`: sesión 7246, EXIT1 (`46299e`), log `history_http_init.log`. Java pasó en3 min 2 s y produjo 89 XML; frontend no pudo ejecutar Vitest porque faltaba `node_modules`. Init verifica, pero no ejecuta automáticamente `commands.install`. La atribución inicial de instalación completada fue incorrecta y se corrigió ante root.
- `node scripts/project.mjs install`: sesión 35103, EXIT0 (`7f1dba`), log `history_http_install.log`; ambas instalaciones usan `--frozen-lockfile`. Se conserva el aviso habitual sobre scripts ignorados de `@parcel/watcher`, sin cambiar autorizaciones ni configuración.
- Segundo init: sesión 95060, log separado `history_http_init_verified.log`, EXIT0 (`891b81`). Java: 2.076 tests en 89 XML, cero fallos/errores/skips; frontend: 1.802 tests en 38 suites; arnés: 40 tests; lint verde. Java quedó cacheado después de su ejecución real en el primer init. Sin producción ni pruebas18 escritas durante este arranque.

A entrega un bundle real de aplicación, pendiente de incorporación por root después del init; no se han creado interfaces provisionales.


## Primeros ciclos HTTP

1. Página vacía: RED de compilación `b82824` por controlador inexistente; GREEN `dc1384`. El primer intento de comando estaba situado en la raíz (`6ae67a`) y no ejecutó Gradle. Un intento posterior usó el patrón demasiado amplio `*HistoryApiTest`, que también alcanzó TaskHistoryApiTest sin el wiring18 todavía implementado; no se modificaron fixtures ajenos. El oráculo de Cache-Control se corrigió a contener no-store, preservando las directivas de seguridad heredadas. Los focos siguientes usan el nombre completo de clase.
2. TASK_STATUS_CHANGED: RED `27ac0e`, el detalle exponía taskVersion; GREEN `5b19ec` reutilizando HistoryEntryResponse4.
3. BLOCK_PLANNED: RED `8cf5c3`, dominio sin aplanar; GREEN `9e2353` mediante BlockResponse9.
4. BLOCK_CHANGED: RED `d7537c`, faltaba DTO; primer GREEN intentado `203649` descubrió una expectativa de fixture incorrecta para la revisión heredada. Se conserva el token real entre comillas `block:UUID:revision`; GREEN `73ab4c` sin cambiar el contrato13.
5. SESSION_CHANGED/PAUSE: RED `632e55`, forma interna; GREEN `1c3158`, DTO6 y contadores decimales string mediante el mapper heredado. Cinco tests del adaptador pasan; no acredita PG ni la feature completa.
6. Filtros nominales y extremos inclusivos: RED `6b6e14`, GREEN `4b5907`; se transmiten los valores al puerto, sin consulta PG en este slice.
7. Query desconocida: RED `7b7191`, GREEN `036f10`; 400 antes de aplicación.
8. Query repetida: RED `1af7a9`, GREEN `ee4fd4`; no se elige silenciosamente el primer valor.
9. CLOSE con notas/fecha histórica: inicialmente GREEN `84ccaa` gracias al mapper reutilizado; sin cambios de producción.
10. EXTEND con sus dos fines y estado original: inicialmente GREEN `5da5d2`; no se inventa tiempo neto ni closure.
11. SESSION_STARTED antes de epoch, año0001 y microsegundos: inicialmente GREEN `5fba69`; conserva DTO7.

## Checkpoint nominal para revisión

Foco completo después de Spotless: `9d2332`, EXIT0, 11 tests. XML copiado a `history_http_checkpoint_xml` antes de nuevos ciclos. Formato real por archivo absoluto: el intento de pasar rutas relativas al hook fue ignorado con aviso (`46c4f9`); se corrigió usando una llamada por ruta absoluta, sin formato global.

Congelados `HistoryController.java` y `HistoryApiTest.java`, manifiesto `history_http_checkpoint_hashes.json`. Cinco familias pasan por sus representaciones públicas; no se añaden validadores de almacenamiento al mapper. A valida integridad seleccionada en PG. El puerto ya es real del bundle `99ddbb0`.

Pendientes explícitos: validación completa de category/fechas/relación task-project; decode/encode y sintaxis estricta de cursor; errores/seguridad y regresión final HTTP. `nextCursor` permanece nominal null en este checkpoint. No es aprobación de la feature completa ni de paginación. No wiring nuevo; no mocks añadidos a fixtures históricos.
