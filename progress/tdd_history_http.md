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

## Aclaración contractual autorizada de @s18

Root comprobó `d1fa57`: la configuración de sesión vigente permite GET autenticado sin CSRF y OriginGuard sólo comprueba escrituras. El ejemplo indeterminado de un 403 de seguridad durante H no tenía premisa realizable con dicha política. Por instrucción de root se sustituye exclusivamente ese ejemplo por GET autenticado sin token CSRF y query inválida: 400 query INVALID_VALUE. Normativa18 explicita la misma reutilización; no se cambia configuración ni se exige un nuevo permiso. Se mantienen 39 escenarios y 142 ejemplos. El oráculo existente `s12_unknownQueryIsRejectedBeforeUnavailableStorage` ya ejecuta exactamente GET autenticado sin CSRF y `limit=20`, con 400 y cero llamadas al puerto (`036f10`); no se atribuye un 403 de lectura ficticio.

## Cursor y validación: ciclos posteriores al checkpoint

| Ciclo | Oráculo | Resultado observado |
| --- | --- | --- |
|12|UUID con letras mayúsculas en query|Inicialmente GREEN 1f576b, normalización heredada; corrige la limitación del primer s8.|
|13|Categoría exacta/no vacía|RED 3b82df, GREEN ec35bd.|
|14|Fecha real, cuatro dígitos y año mínimo|RED 0c3ce8, GREEN 1f6d6f.|
|15|taskId exige projectId antes del cursor|RED 64c823, GREEN b05c21.|
|16|from posterior a to, error en to|RED 0e38fd, GREEN a05849.|
|17|Envelope de salida exacto y fronteras de página de 20|RED 154c6c, GREEN e2d08e.|
|18|Decode conserva ambos límites y filtros|RED d76047, GREEN 842c40.|
|19|Base64URL con padding|RED 759b2f, GREEN 7261d1.|
|20|Alias Base64 de los mismos bytes|RED ab061e, GREEN 87e983.|
|21|Claves JSON duplicadas|RED dce0b0, GREEN 3ebd40.|
|22|JSON posterior al objeto|Inicialmente GREEN 016238: ObjectMapper del proyecto ya activa FAIL_ON_TRAILING_TOKENS; no se atribuye un RED.|
|23|Envelope cerrado, cinco campos|RED dea7ba, GREEN 025004. El nombre inicial decía seis; se corrigió a cinco sin cambiar el oráculo.|
|24|Version entero 1 sin coerción|RED 618f6b, GREEN 9047cb.|
|25|Campos completos de filters/upper/after|RED 1258eb, GREEN f8ae00.|
|26|Textos nunca convierten números|RED fc8be9, GREEN c8da63.|
|27|Fronteras UTC, microsegundos y años|RED d2454c, GREEN d753db.|
|28|Tipo de hecho de catálogo|RED 7f51ae, GREEN 341137.|
|29|UUID canónico en frontera|RED f31bf4, GREEN 30dd1d.|
|30|Categoría válida dentro del cursor|RED d2e5c0, GREEN 4e323f.|
|31|UUID del contexto sin representación Base64 alternativa|RED 20c1a4, GREEN 7e6e1d.|
|32|Fechas del cursor en rango público|RED 79f6e8, GREEN 2075f5; reutiliza el parser de fechas de query.|
|33|Propiedad precede a owner/filtros/orden incompatibles|Inicialmente GREEN 884477; el slice acredita transmisión intacta al puerto y traducción404, no la consulta PG.|
|34|Anónimo antes de query inválida|Primer resultado43ed65 por código esperado incorrecto; el contrato heredado usa UNAUTHENTICATED. Oráculo corregido, GREEN 326f5b; sin producto nuevo.|
|35|503 sin página parcial ni detalles privados|Inicialmente GREEN 0e355d, handler reutilizado.|
|36|24:00/segundo60/nueve cifras terminadas000|RED f3e0b4, GREEN 385e85. Restricción léxica antes de Instant.parse; hallazgo root resuelto.|
|37|400 cursor de aplicación conserva problema|Inicialmente GREEN 5f5d32, handler reutilizado.|
|38|Filtros presentes, categorías restantes y fronteras extremas|Inicialmente GREEN 2323e8; no se exige que las filas de frontera sigan presentes.|

Cada ciclo se ejecutó antes de añadir el siguiente. Los métodos parametrizados prueban variantes del mismo requisito; sus invocaciones no se equiparan a los ejemplos Gherkin. Logs individuales history_http_12…38 preservados.

## Freeze HTTP/cursor

`301db6`, EXIT0 después de Spotless real focal: HistoryApiTest: 75 y SecurityConfigurationTest: 4, total 79, cero fallos/errores/skips. XML copiados a `history_http_final_xml`; resultados y hashes en `history_http_final_results.json` y `history_http_final_hashes.json`. Tres Java propios: HistoryController, HistoryCursorCodec y HistoryApiTest. No se tocaron Store, wiring, configuración, DTO heredados o tests ajenos.

El adaptador cumple forma de las cinco familias, encode/decode cerrado, léxico temporal y UUID, filtros y precedencia estructural. La aplicación recibe el cursor completo: autorización de contexto y vínculo owner/filtros/upper-after son de A y todavía requieren regresión integrada sobre su corte final. Este árbol conserva aplicación nominal99ddbb0; sus mocks no acreditan paginación PG ni detección de JSONB incoherente. Root incorporará PG/wiring/cliente congelados entre ejecuciones, sin copiar WIP. H no requiere CSRF conforme a la aclaración aprobada be25c5c.

El rechazo de JSON posterior usa la configuración estricta real heredada del ObjectMapper, comprobada mediante HTTP. El codec activa localmente el rechazo de claves duplicadas. No nueva política de seguridad ni validación duplicada de almacenamiento. E2E inicial sigue RED por vista ausente; no se ejecutaron campañas globales o de mutación. Gradle detenido al entregar el freeze (daemon disponible, ninguna ejecución activa).
