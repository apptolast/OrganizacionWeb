# TDD frontend19 — revisión semanal

Contrato aprobado: `features/weekly_review.feature` SHA256 `637B6BE613EF6B81623801403A922BF0E8EA26821D51950C43092C9206827375`; normativa `3665DCD1259A70D420D2A3124231E265B42DAC9A37C4B7EBCB87278C9215AC68`. Init común 62482 EXIT0 reutilizado por instrucción del coordinador. Ponytail full/Caveman lite; sin Git, backend ni campañas globales.

## Primer corte nominal — 7 de septiembre de 2026, 15:13–15:15 Europe/Madrid

Sólo `frontend/src/weekly-review-api.ts` y `.test.ts`. El fixture vacío contiene los once campos, siete días y tres totales normativos; no representa cobertura de todas sus invariantes.

| Ciclo | Oráculo individual | RED real | Mínimo y GREEN |
| --- | --- | --- | --- |
| 1 / @s1 | GET sin filtros devuelve la semana vacía, con same-origin, no-store y señal | 0e8fec: módulo ausente | apiRequest y lectura JSON; 3d2977, 1/1 |
| 2 / @s25 | Campo adicional en envelope rechaza toda la respuesta | fa6631: aceptaba owner adicional | exact heredado, sin modificar helper; 818bda, 2/2 |
| 3 / @s23 | HTTP503 se conserva como Response para recuperación manual | 34a254: intentaba leer JSON vacío | comprobar status200 antes de JSON; ea6e6d, 3/3 |

Logs locales `weekly_review_frontend_cycle{1,2,3}_{red,green}.log`. El mensaje secundario de pnpm sobre vitest después de los fallos no fue un fallo de instalación: Vitest ejecutó y emitió el diagnóstico anterior.

Pendientes explícitos antes de considerar completo el cliente: DTO interno y tipos, rango long antes de BigInt, fechas/fronteras/continuidad/sumas, null/cero, zona y date solicitados, serverNow dentro de semana sólo sin date, query y guardas heredadas. UI completa y sus estados siguen sin implementar. No se atribuye cobertura de los 34 escenarios a estos tres tests.

## Decoder en curso — ciclos 4–10, 15:16–15:20 Europe/Madrid

Cada caso se añadió y ejecutó antes del mínimo productivo, con logs `weekly_review_frontend_cycleN_red.log` y `_green.log`:

| Ciclo / @s25 | Rechazo individual | RED | GREEN |
| --- | --- | --- | --- |
| 4 | Día sin workedMicroseconds | f30dd4 | 199e2e, 4/4 |
| 5 | Sólo seis días | 663b4e | 8e832c, 5/5 |
| 6 | Campo adicional en totals | dfc5e6 | 45d46e, 6/6 |
| 7 | Duración de trabajo negativa, con total igual | 34c50f | 60e3ff, 7/7 |
| 8 | Duración superior a long | 74730b | 879b28, 8/8 |
| 9 | Duración planificada no canónica 00 | c10edf | 96e8b5, 9/9 |
| 10 | Capacidad numérica JSON en vez de string | 97b766 | 7a9270, 10/10 |

Freeze temporal tras ciclo10 para corregir dos fixtures heredados asignados por root. Se formatearon únicamente estos dos archivos WIP antes del lint global de ese arreglo; no cambió su lógica. El decoder aún no está completo ni aprobado para UI: faltan totales/rangos restantes, fechas y coherencias, tipos y guardas de señal.

## Decoder completado — ciclos 11–38, 15:24–15:41 Europe/Madrid

Cada fila corresponde a un solo test añadido y ejecutado; logs locales `weekly_review_frontend_cycleN_red.log`/`_green.log`. Las filas inicialmente verdes tienen `_initial.log` y no impulsaron producción nueva.

| Ciclo | Oráculo | Resultado |
| --- | --- | --- |
| 11 / @s25 | Total numérico JSON | RED ab5771, GREEN9b11b0; reutilización amounts para día/total |
| 12 / @s25 | Contador de 200 cifras | RED70e878, GREENc06e44; mismo límite previo a conversión |
| 13 / @s25 | Total de trabajo difiere 1 µs | REDfe7e0a, GREENf475d5; suma entera de plan/trabajo |
| 14 / @s25 | Capacidad total conocida y días null | REDbabe1f, GREEN19e442 |
| 15 / @s25 | Suma de capacidad diaria distinta del total | RED11af28, GREEN7245b0 |
| 16 / @s26 | Suma long máxima, superior a Number seguro y presupuesto cero | Inicialmente GREEN0cd97a; fixture precisado con AVAILABILITY/UTC, revalidado desde ciclo17 |
| 17 / @s25 | serverNow con siete cifras fraccionarias | RED02b240, GREENe1f937; instant heredado |
| 18 / @s25 | weekStart no es lunes | REDb296ce, GREEN0ea924 |
| 19 / @s25 | Día civil repetido | RED8d3f33, GREENefa04b; domingo y siete días consecutivos |
| 20 / @s25 | Discontinuidad de 1 µs entre días | REDcfbae2, GREEN4775de |
| 21 / @s25 | Día con extremos invertidos aunque enlacen | RED0280c5, GREENeb0858 |
| 22 / @s25 | serverNow en extremo exclusivo sin date | RED8fb49f, GREENf70d76 |
| 23 / @s25 | Respuesta válida de otra semana solicitada | RED32cd80, GREENa04129 |
| 24 / @s26 | Semana histórica explícita y query conservada | Inicialmente GREEN1a65d0 |
| 25 / @s25 | Zona distinta de la explícita | REDfe2cf9, GREEN61d781 |
| 26 / @s25 | Discriminador zoneSource desconocido | REDeb9aab, GREENfac9e4 |
| 27 / @s25 | Zona efectiva vacía | RED8b2f9f, GREENbe3c29 |
| 28 / @s25 | Zona guardada con tipo numérico | RED968a87, GREEN5652ea |
| 29 / @s25 | Fallback anunciado para zona explícita | REDc37c29, GREEN641b0e |
| 30 / @s31 | HTTP401 tardío abortado | RED0cb026, GREENf510ee; guarda previa a status/JSON |
| 31 / @s31 | JSON tardío abortado | RED6f6eb2, GREEN124ebe |
| 32 / @s31 | Query del llamador cambia después del envío | RED6adb6c, GREEN5aefa7; copia local de selección |
| 33 / @s25 | Fallback no UTC | RED2b3b63, GREENc531e7 |
| 34 / @s25 | Origen AVAILABILITY sin zona guardada coincidente | REDe7030b, GREEN3dd9ee |
| 35 / @s25 | Zona UNAVAILABLE representada por null | RED190204, GREENc55e70 |
| 36 / @s25 | UNCONFIGURED con zona guardada | REDf1ad52, GREEN9ce2c3 |
| 37 / @s7 | Apia2011, día saltado con fronteras iguales del servidor | Inicialmente GREEN990766 |
| 38 / @s26 | Época1600 y fracción .123457 con trabajo1000001 µs | Inicialmente GREENe5f776 |

Tipos públicos WeeklyReview/WeeklyDay/WeeklyAmounts añadidos al cerrar validación estructural. El primer tsc mostró dos pérdidas de estrechamiento dentro de callbacks (6993dd); referencias locales tipadas resolvieron el problema, tsc y32tests verdes c55c40. No se modificaron los cuerpos ni exports de helpers heredados. civilDay usa microseconds heredado para validar la fecha UTC artificial y aritmética entera de calendario; no usa TZDB, Intl ni Date para repartir trabajo o comparar fracciones. El helper heredado separa segundos y fracción antes de BigInt, conservando µs.

### Freeze de cliente para revisión independiente

38/38 pruebas propias verdes en `weekly_review_frontend_client_final.log` (4076b7 confirma log). La aserción del observador de acceso en el caso30 se reforzó sin nuevo caso; inicialmente GREEN, observer0 y JSON no consumido. ESLint focal y tsc --noEmit terminaron EXIT0 en la secuencia28863f/d9298a. El check final de Prettier detectó normalización pendiente en el test después de su primer write; segundo write y check focal EXIT0 14b36a, sin cambio lógico. Incidente de formato declarado, no presentado como primer pase verde.

Manifiesto `weekly_review_frontend_client_manifest.json`:
- `frontend/src/weekly-review-api.ts`: `9A9C336BD6946099E23C7CC1CF655C229A698365C38308A8EF1DC8BD94631F76`.
- `frontend/src/weekly-review-api.test.ts`: `98C015BCA4EEF9F019BD9B9FEF969CF1D476447FF666EF91CBCCE7016400B672`.

Sólo esos dos archivos de código quedan congelados. Se reutilizan apiRequest, exact/text/instant y microseconds sin modificar sus fuentes; no se repite su matriz histórica. El cliente preserva Response no200 para que UI gestione400/409/503; los códigos por campo y recuperación visible se probarán al montar UI. AbortController en montaje, foco, URL/Back, formularios, estados y treinta criterios UX siguen pendientes de implementación/evidencia. No afirmar cobertura de todos los escenarios backend ni de UI por estas38 pruebas; no se ejecutaron campañas globales o mutación del corte completo.

## Montaje UI — ciclos 39–66, 15:44–16:12 Europe/Madrid

Cliente aprobado como checkpoint por root antes de comenzar; sus dos hashes permanecen idénticos. Se añadieron sólo WeeklyReview/test y la ruta/enlace en App/Workspace. Ningún cambio SCSS aún: se reutilizan estilos existentes y la revisión física decidirá el ajuste mínimo mediante evidencia real.

| Ciclo | Recorrido individual | Evidencia |
| --- | --- | --- |
| 39 / @s27 | Principal, ruta privada, siete días, resumen y foco inicial | RED eb80b3; GREEN a996f2 |
| 40 / @s29 | Carga inicial sin ceros provisionales | RED 1f8b50; GREEN e8ce05 |
| 41 / @s30 | 503 sin vacío falso ni carga infinita | RED a09189; GREEN e12a53 |
| 42 / @s30 | Reintento manual pendiente sin duplicado | RED 3197be; GREEN b3ff92 |
| 43 / @s28 | Fecha borrador no consulta; Mostrar semana actualiza URL y retira anterior | RED 953b71; GREEN 5fd25f |
| 44 / @s28 | Catálogo y zona explícita, lectura sólo al aplicar | RED 5c26b0; GREEN e40c46 |
| 45 / @s28 | Semanas anterior/siguiente y Esta semana conservan zona | RED 873454; GREEN fb5e93 |
| 46 / @s29 | Actualizar conserva snapshot fechado y marcado anterior | RED 993abc; GREEN 369446 |
| 47 / @s32 | 401 vigente retira página y controles privados | RED 916f64; GREEN 87b2d0 |
| 48 / @s31 | HTTP401 antiguo abortado no revoca acceso actual | Inicialmente GREEN 7a9c90 |
| 49 / @s28 | Back restaura fecha aplicada | Inicialmente GREEN 5537ac |
| 50 / @s28 | Anterior no navegable en primera semana0001 | RED d98099; GREEN e82345 |
| 51 / @s28 | Siguiente no navegable cuando domingo sale de9999 | RED 5e9d94; GREEN 7440c7 |
| 52 / @s30 | 400date con descripción asociada y corrección | RED 7d2f34; GREEN 192670 |
| 53 / @s30 | 400zoneId asociado y vuelta a disponibilidad | RED 8519bd; GREEN 255edd |
| 54 / @s30 | 409 temporal explicado y recuperable por GET | RED a3c77c; GREEN 813cd5 |
| 55 / @s30 | 400query corregible mediante formulario, sin repetir query inválida | RED a62809; GREEN c24d37 |
| 56 / @s30 | Catálogo fallido recuperable sin recargar semana | RED b935ba; GREEN 40e70c |
| 57 / @s33 | Foco al encabezado si Apply retira iniciador aún enfocado | RED c9ce6e; GREEN 75a3cb |
| 58 / @s33 | Foco deliberadamente apartado permanece allí | Inicialmente GREEN ab16e1 |
| 59 / @s33 | Navegación de semana retira iniciador y recupera foco | RED dd1dfd; GREEN 83b3f3 |
| 60 / @s34 | Duración legible exacta, presupuesto cero y descanso sin logro falso | RED c0af62; GREEN a9a4ea |
| 61 / @s17 | Sesiones antiguas no cuantificables y disponibilidad ausente | RED 3975fa; GREEN 6e250b; texto ajustado a gramática neutra |
| 62 / @s2 | Zona guardada desaparecida explica fallbackUTC | RED d0f176; GREEN c9c387 |
| 63 / @s31 | Clasificación400 tardía no restaura error de selección anterior | Inicialmente GREEN 465d78 |
| 64 / @s34 | Espera anunciada al consultar catálogo desde selector nativo | RED 91f7b8; GREEN e7f4b8 |
| 65 / @s28 | Back durante otra lectura pendiente no revive snapshot anterior | RED 616313; GREEN 4eec36 |
| 66 / @s32 | Desmontaje aborta consulta pendiente antes de HTTP401 tardío | Inicialmente GREEN f14973 |

Logs por ciclo `weekly_review_frontend_cycleN_red.log` y `_green.log`; casos inicialmente verdes usan `_initial.log`. La pérdida de tipos en mock de Back se detectó en tsc (6dce8f) y corrigió sólo su firma. Foco conjunto previo112 verde1e835e; al descubrir el caso65 se añadió el oráculo antes del mínimo de retirar result/settled/failed durante un cambio de URL. No se rehizo una matriz heredada. Refactor pedido por root elimina el onClickCapture sin consumidor de la rama401; el main navegable conserva su captura local y modificadores.

### Freeze UI para revisión y primer recorrido real

- Suite propia: 28 UI +38 API. Foco con App15 e History33: **114/114 en cuatro archivos, EXIT0 3196df** (`weekly_review_frontend_ui_final_focal.log`).
- ESLint focal, tsc --noEmit y Prettier check de los cuatro archivos UI/integración: EXIT0, secuencia fd0e48/3196df. Logs `weekly_review_frontend_ui_final_{lint,types,format_check}.log`.
- `weekly_review_frontend_ui_manifest.json` (010727) contiene seis hashes reales, incluidos cliente y test ya aprobados sin cambio. UI `81DE439D954E7202F3388D635AA6066A8B8246CE5C0D1951F69BF94EA0B22627`; testUI `8B0293842BEA47F4F6878655BB205F260D3C9F976D7EAFBEF694FD4D56EB927E`; App `D8F64EB9C8305CE334EDFBA269D44BBC249F54EE63F5F5B51F2F6924633A3AAC`; Workspace `3FEF8760F2DB65D5FE09967A1F3AB27D30BDB773BBC933FF2FF9B18EAF2478E3`.

Pendiente de evidencia: HTTP/PG real19 en navegador, adaptación física del quinto enlace/controles/agenda, treinta criterios UX con límites, responsive y motores pertinentes. No afirmar UX19 validada por los tests DOM ni copiar la matriz18 como resultado nuevo. El selector consulta el catálogo existente al recibir foco; permite primer uso sin consulta de configuración ni cambio de preferencia. Fechas de navegación usan Date UTC exclusivamente como calendario civil (0001–9999 y domingo completo), sin recalcular fronteras de zona ni duraciones reales.

Reutilización explícita: RouteLink conserva Back/modificadores; SessionGate mantiene logout/autenticación; apiRequest y abort guardan observer; SnapshotTime aporta Intl/fallbackUTC; seconds conserva fracciones visuales. En UI se ejercitaron401 vigente/tardío y desmontaje, no se repitió toda la matriz de login ni cada variante de decoder. Falta campaña19, diseño de scope/config Stryker y gates integrados, todos sujetos al freeze y coordinación de root. No proceso de B ni runner18080 activo al entregar.
# Ajuste UX posterior al freeze: foco de recuperación

El recorrido real de recuperación encontró que Reintentar desaparecía sin capturar el iniciador, dejando foco en body (UX RED `f8dcab`, log `weekly_review_ux_recovery_focus_red.log`). Se reforzó el caso DOM de retry existente, conservando doble clic/contador/URL/espera: resolver respuesta diferida y exigir foco en h1. RED real `07d860`; mínimo handler captura currentTarget sólo si conserva foco antes de refrescar. El listener de cambio deliberado de foco y condición body permanecen intactos.

Suite WeeklyReview 28/28 GREEN `018263`; UX afectado 1/1 GREEN `df3716`, 12 medidas, cuatro axe sin infracciones, feedback 2,2 ms antes de liberar el 503 controlado; reintento final GET real. ESLint y Prettier focal GREEN `dd38a9`. Fuente SHA256 `284098F9B523FD289E026C5BFD60C626C9437D9A89354CEC684F4AA2A11FE16C`; test `EDD426B8371C1FB7DA0497D3797AB972074F5CEBAAC4C63D5CF9492D7EDED1FC`. No campaña de mutación previa ni cambio de API.
## Refuerzos posteriores a la campaña original

Seis filas autorizadas por root, sólo pruebas: catálogo 401 después de desmontar; segundo reintento tras otro 503; foco voluntario que termina en body; semana no lunes pero internamente coherente; capacidad mezclada null/0; suma positiva válida. Cada fila se ejecutó individualmente y fue inicialmente GREEN, sin atribuir RED inexistente ni cambiar TS productivo. Logs y firmas en `weekly_review_refined_replay.md` y su JSON de propuesta.

La revisión conservó además el destino final del foco original mediante dos filas parametrizadas, control/body (2/2 GREEN d60265). Foco final 69/69 (39 API, 30 UI), lint completo EXIT 0 0a424d. Global 1967/42 EXIT 0 6e2f8c pertenece al instante anterior a restituir esa fila, límite explícito. El replay sigue pendiente de revisión/configuración/autorización, sin alterar la campaña original.
