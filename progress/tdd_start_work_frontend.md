# Cliente de inicio de trabajo14

Rol tdd_craftsman; Ponytail full y Caveman lite. Contrato aprobado por root
en c54aee6. Baseline común91757f reutilizado por coordinación; no nuevo init,
mutación ni suite global. Propiedad limitada a work-session-api.ts, su test y
esta bitácora; sin UI, shared api-client, backend ni metadata.

## Ciclo1 — POST nominal @s1

Un único test nuevo: `sends an explicit start with its key and current CSRF token`.
RED efbe11 EXIT1 por importación del módulo aún inexistente, sin tests ejecutados.
Mínimo: POST mediante apiRequest, contexto de ruta, cuerpo plannedMinutes,
key, credentials/cache/signal y reutilización del token CSRF vigente.
GREEN a32511 EXIT0,1/1 en3,43s. Comando en frontend:
`pnpm exec vitest run src/work-session-api.test.ts`.

Primer corte revisable deliberadamente parcial: sólo transporte nominal.
Todavía faltan estado HTTP, DTO cerrado/relación exacta de microsegundos,
Location/contexto, replay y las tres lecturas. No se presenta como cliente
seguro completo ni como cobertura de toda @s1. Se añadirán casos de uno en uno.

## Corte2 — POST validado y activa, congelado para revisión

Los ciclos siguientes añadieron un test por vez. Cada GREEN ejecutó únicamente
el archivo propio completo; no se añadieron matrices anticipadas.

| Ciclo | Caso y tag | RED | GREEN | Cambio mínimo |
| --- | --- | --- | --- | --- |
| 2 | HTTP503 preservado, @s32 | f7e6bc | 7f307d,2 | Rechazar estado distinto de201 |
| 3 | Replay200, @s14 | b195d9 | 53e3ae,3 | Admitir200 además de201 |
| 4 | Campo extra, @s30 | 9c0d01 | 889e28,4 | Reutilizar exact con los siete campos |
| 5 | Duración string, @s30 | 3851f9 | 8633fa,5 | Comparar duración con intención |
| 6 | Fin desviado un microsegundo, @s30 | 221c2d | 12caf0,6 | Relación exacta BigInt y léxico instant heredado |
| 7 | Proyecto distinto, @s30 | a55fa7 | f34f29,7 | Reutilizar sameId para proyecto |
| 8 | Tarea distinta, @s30 | 447bf4 | 51a2e1,8 | Reutilizar sameId para tarea |
| 9 | Location de otro recibo, @s30 | 0f46ca | eac528,9 | Comparar ruta Location con id |
| 10 | UUID inválida con Location coincidente, @s30 | 8e8a53 | 68c351,10 | Validar identidad con uuid heredado |
| 11 | Zona en blanco, @s30 | a03ded | 4c6fee,11 | Validar texto histórico, sin catálogo |
| 12 | Cero coherente con intención/instantes, @s30 | 69b2b9 | ba850d,12 | Reutilizar integer1–1440 antes de BigInt |
| 13 | Activa de otra tarea propia, @s23 | 274d0d | e2dcc6,13 | GET global con señal/cache/credentials |
| 14 | Ausencia confirmada null, @s23 | inicialmente GREEN | 08e6a7,14 | Sin cambio de producción |
| 15 | Error503 de activa, @s24 | 4fa8b1 | 555a2e,15 | Exigir200 antes del cuerpo |
| 16 | Envoltorio activo con extra, @s42 | 34e75d | 2e77a5,16 | Forma cerrada con session |
| 17 | Activa con fin incompatible, @s42 | 293a5a | 4fa28d,17 | Aplicar validador compartido a session no null |

Refactor GREEN entre16 y17: extraer el validador SessionStart ya exigido por
POST, separar contexto conocido/Location y publicar el tipo de retorno de siete
campos. Formato1cae92 y regresión16/16 GREEN56e7c9. El cliente active reutiliza
ese validador sin imponer el proyecto/tarea de la pantalla, ni consultar reloj
actual o catálogo. Las lecturas porID/key todavía no se han implementado.

Precisión temporal: instant heredado valida forma/calendario/rango y hasta seis
decimales. Date.parse se aplica a la parte de segundos enteros, luego BigInt
convierte a microsegundos y suma la fracción original rellenada a seis dígitos.
La comparación no pierde decimales submilisegundo. No se ha cambiado ningún
validador compartido. Pruebas de todas las variantes léxicas, límites altos,
duraciones distintas, IDs de ruta con mayúsculas, abortos y recuperación ID/key
quedan para el siguiente tramo; no se atribuye su ejecución a estos17 casos.

Validación final del corte: formato1336b5, ESLint focal f84e71 EXIT0,
TypeScript42b264 EXIT0 y17/17 Vitest2cefd8 EXIT0 en2,75s. Renombrado local de
la variable de milisegundos en refactor para que indique su unidad real.
Fuentes y test congelados después de estos resultados. Sin UI, suites globales,
mutación, backend, metadata ni commits por este autor.

SHA256 del freeze2ef490:
- work-session-api.ts: BD7AE7AA7D266A43DAD2D16B955AACF89DB6A0036135862B0E0162A4F6ED8F05
- work-session-api.test.ts: 29773D38B9AB3B43DCE876A23C02A9204C1C7F0DFD7054477DFDA25507D04CE6

## Corte3 — recuperación por ID y key

Reanudación autorizada sobre6578c9e, sólo los dos archivos propios y este
append. Se añaden10 tests de uno en uno; no hay matriz nueva ni trabajo UI.
GET por ID valida la identidad solicitada; GET by-request valida proyecto,
tarea y duración de la intención conocida. Ambos reutilizan isSessionStart,
mantienen AbortSignal/cache/credentials y no exigen Location de POST.

| Ciclo | Caso | RED | GREEN |
| --- | --- | --- | --- |
| 18 | @s21 GET ID nominal sin Location | c3dc43, función ausente | ca2eaf,18 |
| 19 | @s21 ID distinto | 87c6e1, acepta otro recibo | 584e6e,19 |
| 20 | @s22 conservar Response404 de ID | 1fbfff, sustituía por Error de DTO | 1ad554,20 |
| 21 | @s21 GET key nominal sin Location | 091925, función ausente | 4e6ca5,21 |
| 22 | @s30 proyecto distinto por key | 5be79b, acepta otro contexto | 201305,22 |
| 23 | @s30 tarea distinta por key | ba69e8, acepta otra tarea | d0e1ae,23 |
| 24 | @s30 duración distinta pero DTO coherente | 8b8ff4, acepta otra intención | c81e91,24 |
| 25 | @s24 conservar Response503 de key | d6a32c, sustituía por Error de DTO | f8caaa,25 |
| 26 | @s22 conservar Response404 de key | inicialmente GREEN | a861bd,26 |
| 27 | @s24 conservar Response503 de ID | inicialmente GREEN | d3e988,27 |

Los dos últimos casos reutilizan la guarda de estado previamente exigida,
sin modificar producción para fabricar un RED. Los rechazos de DTO reutilizan
el validador completo, sin cambios a su lógica ni al cliente HTTP compartido.

Cierre: formato focal e28238,27/27 Vitest GREEN23584f en3,97s, ESLint focal
c072b1 EXIT0 y TypeScript72b938 EXIT0. Comandos `pnpm exec vitest run
src/work-session-api.test.ts`, `pnpm exec eslint src/work-session-api.ts
src/work-session-api.test.ts`, `pnpm exec tsc --noEmit`, desde frontend.
Retornos explícitos Promise<SessionStart> añadidos en refactor GREEN.
Freeze de fuente/test al terminar; no suites globales, Stryker, backend,
metadata ni Git. Este corte no acredita UI, reinicio real ni todas las
variantes de validación/abortos, que siguen pendientes de fases posteriores.

SHA256 del freeze08f499:
- work-session-api.ts: 84A8F87112F2297986BA0A694F134FDF4E6EB65BA9C2EDB4828B08D61678CCB4
- work-session-api.test.ts: 1CE83520286C3956F98F5FD5B3D02560FCC08F21EB0BE42D16A0F3A008A2B2C7

## Lote UI completo autorizado — en curso

Base020f7ff. El coordinador amplía ownership a frontend necesario y pide
continuar hasta cierre funcional, sin microfreezes, globales, E2E ni mutación.
Se releen UX30, TaskReader, SessionGate y ChangeSubmit; se conserva su arquitectura.

| Ciclo UI | Comportamiento | RED | GREEN |
| --- | --- | --- | --- |
| 1 | @s29 consulta inicial anunciada sin POST | 511e7d, import ausente | 43ca4a,1 |
| 2 | @s28 ausencia y duración vacía con contexto | b5b2c0 | 477b53,2 |
| 3 | @s29 activa de otra tarea, intervalo/enlace | 7336b0 | 3c0e84,3 |
| 4 | @s29 error de consulta y reintento | dd91b9, incluye rechazo no manejado | 1bbc42,4 |
| 5 | @s32 envío explícito retenido, no duplicado | 9d1362 | ec664e,5 |
| 6 | @s37 confirmación persiste ante fallo activo | d492d4 | ed8765,6 |
| 7 | @s32–33 incertidumbre y comprobación por key | 3344ac, incluye rechazo no manejado | a1a870,7 |
| 8 | @s33 missing permite reenvío manual idéntico | 688c76 | 1b1195,8 |

Ciclo cliente28: @s33 reconocer problema cerrado WORK_SESSION_NOT_FOUND
sin consumir Response, RED0e0986 → GREEN9e9897,28 casos API. Sin cambiar
el cliente HTTP compartido. Estos cortes aún no tienen privacidad/foco,
rechazos definitivos, CSRF ni integración completa; no se presentan como UI cerrada.

Continuación del mismo lote, un test por ciclo:

| Ciclo UI | Comportamiento | RED | GREEN |
| --- | --- | --- | --- |
| 9 | @s34 useSession real, renovar y reenviar separados | bacb12 | b7325a,9 |
| 10 | @s28 duración explícita y error asociado | a92185 | 36c140,10 |
| 11 | @s35 corregir validación con key nueva | 794d7d | 0213d6,11 |
| 12 | @s35 proyecto completed definitivo | ba8706 | ccf752,12 |
| 13 | @s35 tarea completed definitiva | 7087c7 | 16f62d,13 |
| 14 | @s35 tiempo fuera de rango sin error de campo | b7ec69 | b9c9e6,14 |
| 15 | @s35 conflicto activo y consulta propia | 057ad9 | c2c107,15 |
| 16 | @s38 cambio de tarea aborta lectura anterior | 54761a | 90f5f4,16 |
| 17 | @s39 consulta401 retira datos mediante padre | 2f96ff | 55820a,17 |
| 18 | @s38 desmontaje aborta POST antes de401 obsoleto | e94157 | e91132,18 |

El parser de errores reutiliza readBlockError para problemas heredados y valida
los problemas nuevos cerrados de14. Se añadieron tiempo fuera de rango y
activa conflictiva por los ciclos UI14/15, sin ampliar DTO de sesión. Las guardas
del comando comprueban aborto tras respuesta y tras clasificar error; la lectura
usa señal propia y el componente se reinicia por identidad proyecto/tarea.
Pendientes al corte18: foco, avisos al cerrar, refresh durante incertidumbre,
elegibilidad, fallback Intl, integración TaskReader y comprobaciones finales.

| Ciclo UI | Comportamiento | RED | GREEN |
| --- | --- | --- | --- |
| 19 | @s35 activa descubierta no oculta comprobación incierta | ee29ad | 3cee9f,19 |
| 20 | @s29 ausencia anterior no habilita envío durante refresh/error | 6eadae | 69c6a5,20 |
| 21 | @s28 espera contexto elegible confirmado | 1238ce | c9a740,21 |
| 22 | @s40 foco al desaparecer iniciador tras éxito | 07fe2e | cc0b17,22 |
| 23 | @s36 cerrar avisa y conserva recuperación | 5a9ad6 | ff0e28,23 |
| 24 | @s31 fallback Intl histórico explícito UTC | a5c18e, RangeError de render | e7628a,24 |
| 25 | @s40 anuncio check, foco y no duplicación | 49f513 | f98b85,25 |
| 26 | @s39 POST401 propaga retirada | 9cd9a6 | 1e62cb,26 |
| 27 | @s35 POST RESOURCE_NOT_FOUND retira contexto | 1aecc6 | 0ea164,27 |

Integración TaskReader @s28: REDa36a0f, sección inexistente. Se integra con
estado de tarea y proyecto ya confirmados por el lector. La primera ejecución
2ca288 descubrió una omisión de fixture nueva: GET /history heredado; se añadió
esa respuesta vacía y se conservó el oráculo de ausencia de peticiones desconocidas.
GREEN4ab0f6,1 caso de integración, sin cambiar comportamiento heredado.

Refactor de presentación GREEN: reutilización de .task-blocks/.task-form/.field,
SCSS propio mínimo con grid y límites de ancho, IDs de useId y región/formulario
nombrados. Mensaje explícito distingue duración prevista de tiempo acreditado.
Formato f2ad86 y56/56 GREEN9f8645 (28API+27UI+1integración). No hay mediciones
de layout por JSDOM: responsive, axe, zoom y motores requieren evidencia posterior.

## Regresión pertinente y recuperación tardía

Regresión de task-state/task-blocks/split-task/Today: ejecución inicial37010 registró fallos de fixtures por GET active nuevo sin respuesta específica (salida parcial613369). Se añadió sólo respuesta explícita `{session:null}` a tres helpers; no se relajaron aserciones ni se modificó Today. Las cuatro suites completas pasaron250/250, EXIT0 ff75d9,28,50s.

| Ciclo UI | Comportamiento | RED | GREEN |
| --- | --- | --- | --- |
| 28 | @s36 lookup anterior no restaura ausencia tras confirmar POST | 61230d, ausencia reaparecía | 022727,28; abortar lookup al confirmar |
| 29 | @s42 envelope activo incompatible no habilita inicio | inicialmente verde | 65580e,1 focal |
| 30 | @s32 conflicto idempotente conserva intención y consulta key | inicialmente verde | 60d244,1 focal |
| 31 | @s33 check404 seguido de503 conserva incertidumbre | inicialmente verde | 3d61e6,1 focal |
| 32 | @s38 JSON de recuperación después de cambiar tarea | inicialmente verde | 82c641,1 focal |
| 33 | @s40 Enter/Tab/ShiftTab y no robar foco conectado | inicialmente verde | 98bd6b,1 focal |
| 34 | @s38 clasificación de error después de retirar contexto | inicialmente verde | b13f23,1 focal |
| 35 | @s32 fallo de red sin POST automático | inicialmente verde | 729104,1 focal |

Cada fila se añadió y ejecutó antes de añadir la siguiente. Los refuerzos inicialmente verdes reutilizan guardas ya implementadas; no se presentan como ciclos RED ficticios. Pausa breve del lote para hotfix autorizado de18fixtures E2E: evidencia independiente en fix_work_session_e2e_fixtures.md; no acredita recorridos de navegador14.

## Cierre del lote API/UI14

Refuerzos individuales restantes: UI36 código desconocido GREEN inicial39bf97; UI37 éxito incoherente por1µs y recuperación de intención original GREEN inicial768a91; UI38 regreso sin key en memoria GREEN inicial6dfdd5; UI39 recuperación después de completed GREEN inicial678cdd. API29 duración1441 GREEN inicial18824b; API30 calendario inexistente GREEN inicialb098b8; API31 exactitudµs antes de1970 y contexto UUID mayúsculas GREEN inicial68829a; API32 problema activo con campo privado extra GREEN inicialf01446. No se modificó producción para esos oráculos.

Integración SessionGate: extracción de fixture compartida GREENd028b7. Primera ejecución cf7117 falló por ruta de logout equivocada en la fixture nueva; lectura f691c9 confirmó /api/session/logout y se corrigió sólo esa URL. Primera ejecución válida GREENc9fba7: datos retirados antes de respuesta logout y lookup tardío ignorado. No se presenta el fallo de fixture como bug de producto.

Formato focal282992. ESLint2203e2 detectó tres infracciones: setState síncrono del efecto y lectura de ref en render. Refactor mínimo: carga/error se inicializan en la acción manual (estado inicial ya pendiente) y aviso de salida deriva de busy/uncertain. Ningún disable de lint ni ajuste de tests. Regresión propia completa73/73 GREEN4a7565 (32API+39UI+2integración),9,04s. Formato final977c1d; ESLint focal EXIT0 4a12d6; tsc -b EXIT0 692cff. Regresión pertinente heredada250/250 ff75d9 permanece vigente: no hubo más cambios TaskReader/helpers después de ella.

Mapa final: @s28 integración/entrada explícita; @s29 carga/ausencia/error/otra tarea/actualización; @s30 validación API y éxito incierto; @s31 fallback histórico; @s32 red/503/desconocido/conflicto y bloqueo; @s33 check nominal/404/503/repetido/completed; @s34 renovación manual separada; @s35 definitivos/acceso/contexto; @s36 aviso/regreso/carrera lookup; @s37 recibo conservado; @s38 guardas tras JSON y clasificación/aborto antes401; @s39 SessionGate logout/401; @s40 teclado/foco/anuncios; @s42 ausencia sólo con DTO válido. @s41 tiene30filas en ux_start_work_session.md y límites de navegador explícitos. No quedan variantes funcionales pendientes de este lote; revisión cruzada, globales, E2E, UX navegador y mutación siguen siendo gates posteriores, no ejecutados aquí.
