# Preparación E2E13 — 2026-09-06

Plan de integración, **sin atribuir pruebas ejecutadas a este documento**. Claude Code está detenido; el núcleo backend13 lo completan autores Codex en su árbol aislado. El panel ya está integrado y el turno E2E se ha cedido en `OrganizacionWeb-reschedule-e2e`, corte `407a534`. Falta integrar el backend completo en ese árbol. No se fabrican respuestas felices para adelantar esa dependencia; la evidencia nueva se registrará en `progress/tdd_reschedule_e2e.md`.

## Reutilización concreta

- `e2e/support/authenticated-test.mjs`: sesión automática y `request` del mismo contexto del navegador. `scripts/session-client.mjs`: `csrfHeaders` y `loginSession`, también tras reinicio. No duplicar login ni CSRF.
- `e2e/support/projects.mjs`: `create` por API y `sql` limitado al proyecto Docker E2E. `support/tasks.mjs`: `saveTask` por API. Crear el bloque por el editor/API reales; SQL sirve para comprobar persistencia, no para fabricar recibos de éxito.
- `support/blocks.mjs`: `configure` obtiene la revisión real de disponibilidad y `openEditor` prepara creación con locales UTC. Se extrajeron las dos funciones durante el turno autorizado, conservando los oráculos11; el recorrido heredado que usa ambas pasó551ece. Evidencia en `tdd_reschedule_e2e.md`. Usar fechas UTC futuras ya empleadas y el fixture Madrid2030 existente para DST; no añadir otro resolvedor TZDB.
- `support/today.mjs:seedAgenda`: reserva real y alineación SQL con `serverNow` para comprobar Hoy. Su UPDATE temporal es preparación explícita del fixture, no prueba de movimiento. No usar ese UPDATE como sustituto del endpoint13 ni asumir que también actualiza proyecciones nuevas.
- `scripts/e2e.mjs`: stack API/web/PostgreSQL aislado, readiness, variables del fixture y retirada propia. Puerto18080 compartido: ejecuciones seriales. No requiere runner nuevo.

## Secuencia mínima de recorridos

| Recorrido propuesto | Base existente y oráculos nuevos |
| --- | --- |
| Crear, mover, cancelar, leer hechos (@s1–4/@s8–13/@s25/@s36/@s38) | Partir de creación real `schedule-block:108`. Leer state/ETag, revisar y confirmar movimiento; capturar headers y recibo real. Mismo blockId, before original, after destino, revisión incrementada. Cancelar con revisión actual: after null, state cancelled y ausencia de lista vigente. Recibo de creación11 sigue original; historial contiene sólo los dos cambios. Consultar estado desde confirmación histórica no restaura la reserva. |
| ACK perdido y reinicio, recuperación del mismo recibo (@s15/@s33–34/@s36–38) | Adaptar `schedule-block:738`: interceptar sólo POST13, `route.fetch()` real, exigir201/leer recibo y después `route.abort`. Comprobar persistencia antes de reiniciar. Mantener key/cuerpo/revisiones, GET `B/changes/by-request/K` recupera exactamente el recibo sin POST adicional. Replay explícito conserva recibo y Location sin otro evento. |
| Conflicto real y corrección deliberada (@s20–22/@s29/@s35) | Patrones de reserva concurrente/budget real `schedule-block:494,614`: otro cambio API entre preview y confirmación. Comprobar rechazo real, borrador conservado, revisión/consentimiento retirados y ninguna escritura parcial. Un caso de revisión obsoleta y otro de presupuesto/solape, según cobertura integrada que entregue backend; no trasladar todas las matrices unitarias al navegador. |
| Historial, privacidad y UX (@s16–18/@s25/@s37–40) | Historial real con cursor y consulta explícita de estado; ninguna petición por fila. Logout con consulta pendiente usa patrón de `today:131` y de sesiones existentes. Recorrido de carga/editor/preview/cancelación/incertidumbre/error/historial con texto largo reutiliza el patrón de inspección de `today:184`, no sus resultados. |

Para ACK/reinicio ya existe control fuerte: fixture `organizationweb-e2e-<pid>`, `compose restart backend`, cambio de `StartedAt` del backend, `StartedAt` y `Mounts` PostgreSQL iguales, readiness y reautenticación sólo si es necesaria. Conservar estas assertions de `schedule-block:738–899`. Acreditar filas/recibos y exactamente un `BlockChanged.v1` por cambio confirmado; no afirmar entrega Rabbit sólo por contar outbox.

## Comandos previstos y dependencias reales

Tras crear el archivo autorizado: `node scripts/e2e.mjs e2e/reschedule.spec.mjs`. Para un ciclo, seleccionar `e2e/reschedule.spec.mjs:<línea-del-test>` una vez escrita, o un `--grep` simple. Evitar alternativas `|` con el runner Windows actual: el shell las interpreta como tuberías. No hay aquí una ejecución ni un archivo nuevo implícitos.

Sólo existe `playwright.config.mjs` en raíz. Para los mismos casos: `--browser firefox` / `--browser webkit` mediante la CLI existente cuando esos navegadores estén instalados. No inventar una configuración cross-browser inexistente. El zoom nativo tiene patrón separado en `today-native-zoom.spec.mjs`, con Chromium headful, extensión y comprobación `getZoom`/DPR; Linux necesita DISPLAY/Xvfb. No reemplazarlo por CSS zoom ni atribuir el zoom de Hoy al panel13.

Las once sentencias TRUNCATE heredadas ya incluyen explícitamente `block_changes, block_projections` en main `ae364e5`; la corrección tiene regresión real 91/91 documentada aparte en `progress/tdd_e2e_schema_fixture.md`. El fixture nuevo conservará esa lista explícita, sin CASCADE. Las queries SQL de comprobación deben distinguir creación inmutable, estado vigente y recibos históricos. Falta integrar el backend13 completo y ejecutar los nuevos oráculos de recibos/eventos/cursor.

@s40 exige las **30 filas** de `docs/ux-requirements.md`, con estructura documental ya usada en `progress/tdd_today_e2e.md`: criterio del nuevo recorrido, evidencia y resultado/límite. Medir320–2560 y bordes, 44px, teclado/foco, texto ampliado, feedback<400ms, axe y cada motor por separado. Los helpers de geometría actuales no están exportados: extraer sólo lo que vaya a compartir el nuevo test y ampliar la selección a input/select y áreas de labels pertinentes. No heredar automáticamente los PASS anteriores ni declarar dispositivos físicos, teclado virtual, lector real o facilidad de uso humana verificados por emulación.

Fuentes leídas: `454b4b`, `c21dff`, `a484ca`, `04b552`. Ninguna suite, nueva infraestructura o modificación de fuentes/configuración realizada.
