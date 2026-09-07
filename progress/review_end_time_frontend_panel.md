# Revisión independiente del panel de fin17 — freeze selectivo

**CHANGES_REQUESTED**, limitado a tres flujos diferenciables del corte. Revisión de lectura, sin ejecutar pruebas ni modificar producto/tests. Se verificaron los seis hashes de `end_time_frontend_panel_freeze.json` (`d625de`), incluidos `work-session-state.tsx` 2CE8D5…27F51 y panel 2E9E42…9DD41. La evidencia del autor es88/88 GREEN1dc4ee:29 panel,2 hook,21 API17 y36 StatePanel; lint/types/formato según su bitácora. No se presenta esa ejecución como propia.

## Hallazgos accionables

### P1 — La decisión hermana puede usar el snapshot anterior mientras espera GETstate

`frontend/src/work-session-state.tsx:53–66`, efecto116–141 y botón246–253. Tras confirmar EXTEND, `decision.settle()` libera la acción e incrementa generation. StatePanel inicia una lectura nueva, pero conserva snapshot/revisión anteriores y `send` no comprueba la generación de ese snapshot. Si GETstate queda pendiente, Pausar sigue operativo y construye una intención con la revisión anterior. El nuevo acquire puede abortar precisamente la lectura que debía actualizarlo. Esto contradice la coordinación de snapshots @s40, aunque el servidor impida la segunda escritura mediante412.

Oráculo mínimo: ajustar el recorrido existente «a confirmed extension refreshes sibling state before pausing with the new revision» para diferir la segunda respuesta GETstate. Antes de resolverla, intentar Pausar y comprobar que no se transmite; después, resolver revisión2 y confirmar un único POST con esa revisión. El test actual devuelve GETstate con Promise.resolve y sólo comprueba el número de llamadas, por lo que no cubre la ventana pendiente. No requiere montaje padre ni otra matriz.

### P1 — Abortar una lectura E puede dejar la consulta bloqueada permanentemente

`frontend/src/work-session-end.tsx:66–71,145–180` y `use-work-session-decision.ts:25–29`. Con un snapshot ya disponible, iniciar una actualización E y mantenerla pendiente; después confirmar una ampliación. acquire aborta el controller E, pero el finally de E sólo libera lookupBusy/loading si no está abortado. Si el POST devuelve412 (o queda incierto), no hay settle/generation que cree una nueva lectura y resetee ese estado. «Consultar fin actual» llama refreshEnd y sale porque lookupBusy sigue true: no se puede completar la recuperación requerida por @s38 sin desmontar el panel.

Oráculo mínimo: E inicial válido → actualización E diferida → POST412 → intentar consulta manual. Debe emitirse una nueva E y anunciar su espera; una finalización tardía del E abortado no debe desbloquear ni sobrescribir otra consulta vigente. La corrección debe conservar coalescing @s33 y no liberar indiscriminadamente el indicador de una lectura posterior.

### P1 — Un404 vigente del POST conserva información privada

`frontend/src/work-session-end.tsx:117–127`. Un POST EXTEND con problema reconocido WORK_SESSION_NOT_FOUND entra en rejectionMessage: borra la intención retenida pero conserva snapshot, cantidad/formulario y hechos confirmados, y no comunica onAccessFailure(404). La retirada privada sólo está implementada en el catch de GET E (líneas160–173). @s42 exige retirar esos datos también ante pérdida vigente de propiedad; el montaje padre no recibe la señal necesaria para hacerlo.

Oráculo mínimo: snapshot y borrador visibles → POST404 WORK_SESSION_NOT_FOUND vigente → verificar retirada de fin/borrador/hecho privado y callback404. Conservar las guardas tras clasificación asíncrona para que un404 antiguo no afecte a otro contexto. El caso existente de404 cubre GET E, no este POST. No se pide repetir la matriz completa de errores.

## Lo revisado sin hallazgo adicional

La fórmula del temporizador usa diferencias BigInt de microsegundos y performance.now; sólo convierte un delay acotado y positivo, fragmenta2^31−1 y redondea un resto positivo a al menos1ms. El vencimiento solicita E antes de afirmar el aviso. La visibilidad reutiliza la barrera de lectura; la incidencia de aborto anterior es la excepción señalada.

La recuperación conserva intención/key/revisión y usa K. Sólo CSRF reconocido o ausencia de recibo reconocida habilitan reenvío manual; respuestas desconocidas e IDEMPOTENCY_CONFLICT siguen inciertas. Los recibos confirmados se presentan separados del fin actual. El panel comprueba signal.aborted después de resultados y de clasificación de problemas; los tests de401 atrasado cubren la entrega HTTP, coherentes con el observer síncrono del cliente. El foco se intenta restaurar únicamente cuando el iniciador seguía enfocado y desaparece; no se exige foco automático por aviso.

El coordinador tiene adquisición síncrona mediante ref, invalida lecturas registradas al adquirir/confirmar y mantiene la decisión ante incertidumbre. La brecha StatePanel de generación es concreta, no un rechazo de esta arquitectura.

## Límite del dictamen

No se revisó como terminado el montaje en WorkSession/Reader, GETactive padre, cierre compartido, ambas superficies reales ni la nueva sesión tras un cierre. B reconoce esos pendientes y no son un hallazgo nuevo. Tampoco se acredita navegador, controles44px, geometría, anuncios reales con lector de pantalla ni campañas17 desde estos88 tests. API@s27 y sus campos exactos se leen dentro del freeze; la época grande y precisión son evidencia del caso del autor, no una ejecución independiente.

Los tres hallazgos se comunicaron a root y B antes de cualquier cambio o campaña frontend. El dictamen podrá actualizarse con el delta y oráculos acotados; no se solicita ampliar la matriz heredada14–16.
