# Revisión independiente frontend15

**APPROVED del corte de cliente, panel e integración15**, incluido el arreglo de feedback29c10b. No encontré un defecto contractual bloqueante en esta lectura. No se ejecutaron tests, navegador ni campañas; UX física y cierre global siguen fuera del dictamen. Ponytail full y Caveman lite.

## Corte revisado

Fuentes y pruebas leídas en c206f8,910739,b8e98a,5f6888; evidencia de TDD en `tdd_pause_resume_frontend.md`. Los hashes medidos5f6888 coinciden con el freeze comunicado:

- Cliente API: `38743D9B82869708BD53C98850DF8C71FAEFB7CD0BE5348519679D8E32AB2FAA`; prueba `7AC5B1B3CDC1DE502CF6CB801566B4A4B54C8315777CE0A884C29B1A5FF31090`.
- Panel: `4FCD95AC8BCCDE4AAB91137A0F4970378F81DAE078FC0318F4BBB017D26A1599`; prueba `C203284CCE4FF7F5C5664D840C5ABD4946ACE7DA27451AD379254B90908F9046`.
- WorkSession integrado: `E2CDBC83BD8268CA90C19ACA78A4ADA9F912549160FA50628EB381C22275B5DE`.
- Config Stryker: `98C7543A5D3B19CCAB300E79DE3D3A8FA9053B7CB23372C8BCDC3F84FF79DB9A`.

La evidencia del autor distingue API47, composición77 anterior y panel final33 tras REDdee656/c0ddd0 y GREENbb708e. No sumo esas ejecuciones superpuestas como casos diferentes ni afirmo que yo las haya repetido.

## Resultado de la lectura

**Contrato cerrado y precisión.** El cliente exige snapshot3/state6/recibo6 y SessionStart7 heredado. Comprueba identidad solicitada, token propio exacto sin ETag, revisión canónica hasta BIGINT, changedAt y runningSince coherentes, acumulado acotado y neto exacto. Usa BigInt sobre microsegundos; no pierde la fracción ni el rango anterior a1970. Los recibos conservan SessionStart, acción, revisión+1 y acumulación de PAUSE frente a RESUME. POST exige Location; GET histórico no. La recuperación por key verifica la intención conservada. Los tests contienen anomalías coherentes de identidad, revisión, tiempo, acumulado y campos adicionales, no sólo valores triviales mal tipados.

**Estado actual frente a recibo.** WorkSession monta el panel desde active, no infiere running del recibo de inicio. La confirmación de PAUSE/RESUME permanece histórica mientras se solicita un snapshot nuevo; una consulta fallida no la revoca ni ofrece una nueva transición basada en el recibo. El mensaje de fin fijo y la composición permiten gestionar paused aunque el contexto esté completed, sin ampliar14 a otra sesión activa.

**Intención y errores.** Una operación incierta conserva action/key/token/state. Comprobar usa K; sólo ausencia reconocida o CSRF permite reenvío manual de la misma intención. Refresh independiente no sustituye esa intención ni confirma su resultado. Un rechazo conocido412/estado/agotamiento/tiempo/ausencia retira la decisión y requiere consultar antes de elegir con key nueva. Código o cuerpo desconocidos permanecen inciertos. No hay POST automático por renovar CSRF.

**Privacidad asíncrona.** El montaje por session.id retira el contexto anterior. Comando y lookup tienen AbortController; el panel comprueba aborto después de éxito, antes de401, después de clasificar error y en finally. El apiRequest heredado comprueba aborto antes del observer401 y después del await de CSRF403. Los tests distinguen401 vigente/tardío, JSON200 diferido y clasificación no401 diferida con contexto nuevo; @s39 demuestra que el lookup anterior al comando no puede restaurar running tras confirmar paused. Esto no se presenta como prueba de toda intercalación imaginable.

**Feedback y foco.** El delta activa loading y limpia el error anterior al reintentar con snapshot o confirmar una recuperación. El nuevo caso33 exige anuncio durante GET retenido, destino al heading tras desaparecer Reintentar y control refresh temporalmente inactivo. El caso30 observa confirmación histórica y anuncio de consulta simultáneos sin alerta vieja. aria-disabled conserva foco y send tiene guarda busy; useLayoutEffect mueve foco sólo cuando el iniciador desapareció y BODY quedó activo, respetando otro control elegido. Neto lleva hora de actualización y zona; fallback UTC se etiqueta. No hay cronómetro ni supuesto crédito de trabajo terminado.

**Scope Stryker.** Selecciona completos los dos archivos nuevos y WorkSession modificado. Las dos funciones compartidas sólo se exportaron; no cambiaron sus cuerpos. Mantiene break80, perTest,8 workers, Vitest, salida propia y el ignorePatterns protegido intacto. No se afirma mutación ejecutada por leer la configuración.

## Límites

No solicito más casos por duplicación en este paquete. Esta aprobación no acredita44px, reflow, zoom200%, axe, tres motores ni los30 principios por JSDOM; el autor de navegador debe aportar esa evidencia separada. Gate integrado y resultados de mutación permanecen bajo root. Ninguna edición de producto/tests durante esta revisión.

## Corrección de formato tras init

Root comunicó que init25523 sólo falló en Prettier, con las suites verdes. Bajo asignación limitada de formato se ejecutó Prettier únicamente sobre `work-session-state-api.test.ts`. El diff e82b08 cambia sólo la distribución de líneas e indentación de `vi.fn().mockResolvedValue` en el último test de replay200; conserva llamadas, datos y aserciones. No se infiere la causa cronológica de la omisión original.

`node scripts/project.mjs lint` completo terminó GREEN/EXIT0 (835359): Spotless, ESLint y Prettier. Diffcheck411947 también verde; no se repitieron suites. Nuevo hash del test: `88570D507A74955B5B50221BCDE623EC3FEA197600366FC189BD8F2484625B0E`. Log `progress/pause_resume_lint_after_format.log`, SHA256 `D509D0F7C90AC77D16FAFFFBC639EDE312FA503C28FF28516BF332265198C2E4`. Fuente productiva y restantes tests intactos; aprobación de comportamiento conservada.
