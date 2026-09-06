# Entrega de tramo frontend — schedule_block

Pausa de autoría solicitada por el coordinador; disponible para reactivación cuando backend anuncie GETs. Feature sigue in_progress, sin freeze, mutación, commit ni push.

- UI estable: 53/53 focales; regresión completa anterior de 52 UI: 1117/1117 en 21 archivos. TypeScript, ESLint y formato de producción frontend verdes. Los tres P2 de revisión incremental quedaron cerrados por el coordinador.
- Casos adicionales: foco de confirmación/recuperación, privacidad del listado/SessionGate/conflicto, solape definitivo con consulta del bloque propio, recuperación tras proyecto completed y conservación de key ante IDEMPOTENCY_CONFLICT. Cliente API aprobado sin editar; targets API/UI añadidos a Stryker sin ejecutarlo.
- QA con HTTP simulado: Chromium153, Firefox155 y WebKit26.6, 141 combinaciones por motor y axe0 en siete estados; texto duplicado, 768×400, teclado y feedback local por debajo de400ms. Zoom nativo Chromium setZoom2, DPR1,5→3 e inner320/client312/scroll312: siete estados y axe0. Abrir/cancelar conserva secuencia Tab nativa en los tres motores; no se añadió gestión de foco innecesaria.
- Evidencia y treinta filas UX: `progress/ux_schedule_block_frontend.md`. TDD/trazabilidad y límites: `progress/tdd_schedule_block_frontend.md`. Contraste HTTP/cliente: `progress/review_schedule_block_http_client.md`, sin desajustes estables detectados; excepciones extendidas aún en autoría backend.
- Capturas y mediciones: `.e2e-work/schedule-block-ui/{chromium,firefox,webkit,zoom}/`. Script: `.e2e-work/schedule-block-frontend-ui.mjs`. La evidencia simulada no acredita backend/PG, dispositivos físicos, teclado virtual ni evaluación humana.
- Primer E2E real preparado en `e2e/schedule-block.spec.mjs`: creación, recarga, replay, by-request, detalle y SQL bloque/outbox. Registro `--list` y sintaxis verdes; no se ejecutó Docker. Incluido en testMatch de motores alternativos. Ocho fixtures E2E históricos incluyen planned_blocks explícitamente en TRUNCATE, sin CASCADE; runner intacto.
- Próximo paso al reactivar: coordinar build Docker con backend, ejecutar primer E2E y avanzar recuperación de ACK perdido, consentimiento y DST según endpoints disponibles. No atribuir tags de paginación/cambio de preferencias a la prueba smoke actual.

Vite se conserva en `http://127.0.0.1:5174`, sesión de herramienta 7778. No se tocaron ni limpiaron archivos bloqueados, directorios ascendentes o código backend.

## Reactivación E2E completada para este tramo

La pausa histórica anterior quedó superada: seis pruebas reales Chromium pasan en20,8s con API/PG aislados (sesión73584/salida14bc9e, snapshot147HTTP + Store previo al siguiente ciclo storage). Cuatro primeros casos también pasan Firefox/WebKit:8/8 en34,3s (65685/e8ae5b). Se cubren creación/replay/GET/recarga, ACK perdido después de commit y proyecto completado, presupuesto0/consentimiento renovado, DST ambiguo, solape después de preview con lookup y corrección, y capacidad consumida después de preview con nuevo consentimiento. Dos últimos sólo Chromium. No cambios de producción ni runner durante este tramo.

Evidencia real: .e2e-work/schedule-block-real/{chromium,firefox,webkit}/ con review/persisted/recovered320/1440 y JSON de fixture/fecha. Chromium último fixture67288; Firefox/WebKit11224. Inspección real WebKit320 persistido y Firefox1440 revisión sin recorte visible. Matriz simulada sigue diferenciada en UX. Bitácora completa: progress/tdd_schedule_block_integration.md. Config cross-browser amplió grep para incluir todos los casos schedule_block; --list confirma selección.

Pendiente coordinador: revisión independiente del E2E y puertas backend todavía activas; no mutación ni commit. Siguiente snapshot debe coordinar freeze breve de src/main con resume_backend (Controller) y resume_review (Store/V11) hasta COPY/context DONE. Vite5174 conservado.

## Entrega posterior @s35/@s53

Reinicio REAL backend con DB conservada:7/7Chromium y14/14Firefox/WebKit verdes, también cerrados casos5–6 en motores alternativos. Runner intacto; evidencia restart-proof.json y restart-recovered320/1440 por motor. Chromiumfixture66072; Firefox/WebKit42372. Tests@s53 list/check/CSRF tardíos añadidos sin producción;319/319focales ybuild/lint verdes. UI56casos actuales;1118regresión independiente root es anterior a estos3. Mapa de pendientes actualizado; detalle de fallos sólofixture/tipado conservado en bitácora. Producción y E2E estables para revisión independiente; sin mutación/commit. Vite5174 conservado.

## Diagnóstico posterior de E2E disponibilidad

Timeout global original: coste agregado28auditorías; focal original1/1PASS27s con23,9s de matriz, traza propia preservada. División autorizada en funcional+28anchos independientes conserva estadoerror/0, axe/bounds/navigation y30s. Grupo --grep=@s39 **31/31PASS,56,2s,EXIT0**,45703/aebd37; pruebas nuevas1,5–2,5s y funcional1,4s. Informe progress/diagnostic_availability_e2e_timeout.md. Sin cambios productoSCSS. Freeze restaurado.

Stryker config incorpora ignorePatterns del directorio protegido, patrón validado sintéticamente con dependencia instalada antes de recursión; no se enumeró/leyó ese destino y no hubo mutación.
