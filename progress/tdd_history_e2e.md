# Historial18: primer recorrido real

Corte aislado `99ddbb0`, contrato aprobado de39 escenarios/142 ejemplos. Único caso `e2e/history.spec.mjs`, @s28 desde navegación principal, con PostgreSQL aislado vacío y autenticación real. Comprueba enlace/URL/encabezado y, cuando exista implementación, respuesta H200 exacta vacía/no-store y ausencia de outbox.

## RED inicial

`node scripts/e2e.mjs history.spec.mjs`, sesión35245, EXIT1 `7e3f17`. Error real: el enlace Historial no existe en navegación Principal (`toBeVisible`,5s). El test no alcanzó los oráculos de URL, encabezado ni respuesta H; no se atribuyen todavía. B confirmó montaje pendiente. Ninguna respuesta de éxito simulada.

Log `history_e2e_initial.log`; copia `history_e2e_initial_error.md`, SHA256 `497D93EEEF69F25AF8F6900BB1DA6C57E24137E5DE74DE6F67396959AEADD02A`. Manifiestos `history_e2e_initial_before.json`/`after.json` idénticos (`65aea7`). Test SHA256 `A8B1D46913FD9A11309810EAA0343FCFFF0260C9A2970B1361B43B8B6829D999`.

Runner `organizationweb-e2e-49532` retiró sus propios contenedores, red y volumen;18080 libre. No se limpiaron otras rutas ni servicios. Caso congelado hasta bundle frontend coherente y backend integrado; no más E2E antes de su GREEN.

## Primer GREEN integrado

Snapshot `e3cf6d2` con PG/wiring/cliente/UI nominales incorporados por root. El mismo test congelado, sin cambiar fuentes, pasa en `0ba87d`: EXIT0, 1/1, 1,4s de caso y4,1s de Playwright. Log `history_e2e_nominal.log`; manifiestos `history_e2e_nominal_before.json`/`after.json` idénticos. Se alcanzaron enlace, URL, encabezado, H200 real `{items:[],nextCursor:null}`, no-store y outbox0. No se acredita todavía lista no vacía, paginación, UX completa o las guardas PG/UI que A/B siguen cerrando.

Runner `organizationweb-e2e-41832` retirado por su lifecycle,18080 libre. El RED inicial y su contexto se conservan. Root autorizó implementar ahora selector PIT18 antes del siguiente recorrido funcional; ninguna campaña se ha lanzado.

## Cinco fuentes y vacío visible

Segundo ciclo inicialmente GREEN `2e7881`, EXIT0: dos casos en5,6s. El primero incorpora, por review root, el texto vacío visible tras H; el GREEN anterior sólo acreditaba su alcance original. El caso nuevo crea diez hechos reales: plan/movimiento/cancelación, inicio/pausa/reanudación/ampliación/cierre, finalización/reapertura de tarea; completa después el proyecto. Compara todos los details con respuestas públicas originales, conserva reserva/inicio y notas literales sin script ejecutable, comprueba SQL10, lista10 y ausencia de lecturas por fila. Activa enlace real al lector de sesión cerrada. No afirma todavía todas las variantes de enlaces de @s32 ni UX completa.

Logs/manifiestos `history_e2e_sources*`; before/after idénticos. Runner organizationweb-e2e-55012 retirado por lifecycle. No cambios de producción. Próximo caso: paginación, filtros, recarga e independencia de outbox del fixture.

## Paginación, filtros y recarga después de reinicio

Tercer ciclo inicialmente GREEN: `history_e2e_pagination.log` registra 1/1 pasado en 13,3s y retirada completa del stack propio `organizationweb-e2e-61384`. La respuesta de la herramienta que inició el proceso quedó truncada al recuperar contexto; no se conserva aquí su código EXIT original, por lo que el dictamen se limita al resultado Playwright y cleanup registrados, sin inventar una salida de proceso.

El recorrido genera 21 cambios de tarea mediante HTTP real; verifica páginas 20+1, unicidad de los 21 hechos, cursor en URL, Back/Forward, recarga y vuelta a recientes. Retira únicamente los 21 outbox del fixture, reinicia el proceso API y conserva PostgreSQL (StartedAt de API cambia; el de PG permanece idéntico), recuperando la misma página. Aplica categoría y fechas UTC, comprueba vacío filtrado y limpia filtros con reinicio del cursor. Los 21 hechos durables siguen presentes en SQL. La eliminación del outbox prueba independencia de consulta; no acredita entrega Rabbit ni publicación previa de esos eventos. Tampoco convierte keyset en snapshot entre peticiones.

Evidencia copiada, sin mover el original, a `history_e2e_pagination_evidence.json` (SHA256 `693357DAD5909E00CBE7D15AD9949F2F78FEDDFFCE5F3D7A9CD1C62EAD0565DC`). Log SHA256 `354F98789568F1BB33E7513C78D5449F63AD3ECDE46B8A77A0FB9E21F98BDDD6`. Manifiestos pagination before/after: 322 entradas idénticas (`bd0c14`). Test congelado SHA256 `B4857C4AF136E353EFFC39E4EDBF7591512DF054E78453D58DC0432F5D1149BF`.

Los tres casos funcionales tienen resultado GREEN compuesto de dos ejecuciones (2/2 y 1/1), no una campaña global nueva. Quedan UI final, privacidad y matriz UX para el corte posterior revisado. Puerto 18080 y Gradle libres; ningún cambio de producción.
