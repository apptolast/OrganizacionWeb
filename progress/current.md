# Sesión actual

## Feature activa

Feature12 `today` in_progress. Contrato de38 escenarios y105 casos revisado (e6781d y ajustes1a8e28) bajo autorización global, antes de TDD. Especificación86dc6c. Se inicia implementación acotada de API y pantalla, con fuentes separadas por autor y revisión posterior. Feature11 conserva cierre done APPROVED6b937b y evidencia histórica.

Backend implementado, revisado independientemente y subido en5cc80a9 (push4ea98f). Init35422 EXIT0/292711:1415 backend,1317 frontend,13 pruebas del arnés y lint verdes. Este corte precede a la última corrección UI de retorno visible con GET pendiente. Frontend permanece en revisión; E2E reales vacío y agenda pasan por separado. Capturas320/1440 revisadas por root; matriz UX completa y mutación pendientes. Ver progress/judge_today.md para alcance y evidencias, sin atribuir cierre a estas mediciones parciales.

Checkpoint posterior f568f6e publicado (43e507): frontend final de fronteras temporales, regresión1319/1319 y lint9698e6. PIT backend80/80 Killed confirmado por root1b93c3. Stryker frontend sesión83950 mide521mutantes sobre este corte; baseline657GREEN, todavía sin resultado. No modificar fuentes/tests/config durante esa campaña.

E2E posteriores: históricos19/19c437d1; tres smokes de Hoy pasan en Chromium/Firefox/WebKit; zoom nativo200%1/1c492dc. UXFirefox/WebKit completó155medidas y cinco axe por motor, pero mantiene EXIT1 por defecto real de foco: disabled de Actualizar envía el foco a BODY al pulsar Enter (cc49ad). Autor resume_review tiene diseñado cambiar a aria-disabled preservando guard de petición; espera terminar Stryker antes de editar. Resume_frontend espera ese fix para ejecutar los cuatro tests Today en tres motores, sin repetir los históricos/zoom si sólo cambia esa semántica. Root revisó PNG de zoom44224 y capturas9324. Matriz30 y límites físicos/humanos en tdd_today_e2e.md. Feature12 no está cerrada.

## Último cierre verificado

- Init94736 EXIT0/8d8c38:1209 frontend,10 pruebas del arnés y lint verdes. Backend UP-TO-DATE sobre1365 pruebas previamente verificadas.
- PIT453/454; frontend original85,38 %, primer replay89,83 % y campaña final55/55 Killed con29/29 objetivos exactos47a669. Son mediciones separadas. RuntimeError945 histórico aceptado explícitamente como límite del adaptador, nunca contado como Killed ni presentado como reparado.
- Build391add y E2E72ed46 (7/7, incluido reinicio real) conservan la misma producción vigente56ced31. Evidencia multi-navegador y UX con límites en las bitácoras.

## Continuidad

Feature12 `today`: in_progress, TDD autorizado tras contrato aprobado. Features1–11 done;13–30 pending. Autorización global del usuario vigente: no se repite permiso por feature, se conservan contrato previo, TDD y revisión. El avance de esta feature no declara MVP completo ni despliegue.

## Pendiente remoto y límites

CI2133120/run34030806009 sigue en curso al registrar el cierre. El job anterior terminó por timeout120min; la ampliación240min no se certifica como success remoto. Commits/push de estos metadatos a cargo del coordinador.

Consulta remota e35040: CI56ced31/run34028599117 terminó success;2133120,540381e y a127747 seguían in_progress. Este éxito pertenece al commit indicado, no al backend nuevo ni a la feature12 completa.

Ponytail full y Caveman lite vigentes. Se conservan rutas/acciones protegidas y temporales cuya limpieza fue rechazada: no leer ni limpiar proposal_schedule_block_time.md, .e2e-work/read-review* o frontend/.stryker-tmp-availability-replay, ni eliminar sus ascendientes. No se afirma limpieza completa ni se repiten acciones rechazadas. Sin resets, automaciones o despliegue productivo.

Aclaración técnica de integración30942d/d60c5a: Clock.systemUTC puede dar nanosegundos y el formato heredado admite hasta microsegundos. serverNow se normaliza una vez a microsegundos; el contrato no amplía DTO11. Backend incorpora test de reloj de9 decimales. Continúa TDD; no es prueba E2E ni cierre.
