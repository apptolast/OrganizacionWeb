# TDD frontend17 — aviso y ampliación

Contrato aprobado44 escenarios/132 ejemplos, SHA6BC581725DC0FE4C7B548191A842882CE5A62FD0348CF789D1BCCD843ABE4309. Autorización root42cda11; no equivale a132 pruebas ejecutadas. AGENTS/rol craftsman/TDD y Ponytail full/Caveman lite releídos; init16 vigente por indicación root, sin repetir globales. Root posee current/metadata/Git; este autor sólo frontend y esta bitácora.

## Primer corte cliente

Nuevos work-session-end-api.ts y su test; work-session-state-api.ts compartido exporta el mismo isState sin relajar reglas y admite EXTEND discriminado. Reutiliza apiRequest, exact/sameId, microseconds, transporte POST/C/K, Location y validación histórica14–16. Sin interfaz, hook, SCSS, dependencia ni configuración nueva todavía.

Cada fila siguiente se añadió y ejecutó antes de la siguiente. Un caso por ciclo, no matriz parametrizada anticipada.

| Ciclo | Escenario/oráculo | RED real | GREEN y mínimo |
| --- | --- | --- | --- |
| 1 | @s13 GETend nominal con token/señal | 7db05a import inexistente | fc6aac, transporte y retorno |
| 2 | @s26 campo adicional en E | b48aed | dc7b90, exact3 |
| 3 | @s26 status desconocido | 95224d | 806757, reutilización isState |
| 4 | @s26 cabecera revisión incoherente | 6cd227 | 8133b6, token canónico |
| 5 | @s26 fin un microsegundo anterior al original | 7c3403 | f41650, comparación BigInt |
| 6 | @s26 E de otra identidad | 092200 | e7d0f7, sameId |
| 7 | @s16 HTTP503 conservado | c4b14e | 669966, status antes de JSON |
| 8 | @s26 serverNow anterior al changedAt expuesto | 81630a | 7d2b27, reloj exacto válido |
| 9 | @s1 POSTEXTEND nominal sin alterar intervalo | 6692e4 | a30cf3, unión/body/acción específicos |
| 10 | @s26 EXTEND sin extension por C | 361165 | d38d83, P/R6 discriminados explícitamente |
| 11 | @s26 fórmula desviada un microsegundo | 9eb075 | 3e0303, fórmula exacta max+minutos |
| 12 | @s26 cantidad cadena numérica | 684794 | 73a32f, tipo number |
| 13 | @s26 cantidad0 con fórmula concordante | 6c1be0 | ca4ec9, mínimo1 |
| 14 | @s26 cantidad1441 con fórmula concordante | abc91a | 686666, máximo1440 |
| 15 | @s26 cantidad fraccionaria | 9ed615, RangeError en lugar del error controlado | 59bcfa, Number.isInteger antes de BigInt |
| 16 | @s26 after running internamente válido que altera intervalo/trabajo | bb7933 | 1593d1, campos de State6 conservados |
| 17 | @s26 occurredAt anterior al estado | 58c347 | ef9201, guarda temporal específica |
| 18 | @s26 previousEnd anterior al original con fórmula concordante | 4c38e6 | 1a7e00, proyección aditiva |
| 19 | @s26 K de otra cantidad con misma sesión/revisión | d27da9 | 45d955, comparación de intención en helper compartido |
| 20 | @s27 K paused tardío,1440min, sin Location | inicialmente GREEN75748c | Sin producción para este caso; reutilización nominal K y rama max(occurredAt) |

La ejecución75748c pasó20 tests pero tsc detectó acceso a extension unknown. Refactor verde: validExtension recibe el fin original ya validado dentro de la rama EXTEND; evita acceso sin tipo y conserva las guardas. Formato y regresión111/111 de tres archivos API14/15/16/17 más tsc GREENc958aa. Se renombró sameNotes a sameDetails porque ahora compara cantidad de EXTEND además de notasCLOSE; no cambió su comportamiento.

Freeze final: formato/ESLint/tsc b0e77a, sesión91759 terminó EXIT0 5d7230 con111/111 en tres archivos.20 son nuevos y91 heredados; no se suman ejecuciones de ciclos. Diffcheck focal ebfc1d sin errores. Ninguna suite UI/global/E2E/mutación ejecutada.

| Archivo congelado | SHA256 |
| --- | --- |
| frontend/src/work-session-end-api.ts | A7BD65AA29827DE31BE60286C528E55710ACA53F2A968C4B11F6454D267CFCE6 |
| frontend/src/work-session-end-api.test.ts | 66F7CB55454808D471B8B8CDC439637FCCF732372FCC868C5CD61FA46E78F3FD |
| frontend/src/work-session-state-api.ts | 929352B95B730B05D62F6C578541AC61D4F98C3A84B75CEC8F2C2F0B5B6CFAB8 |

## Alcance y siguiente tramo

Este corte acredita conexión y diferencias cliente E/EXTEND, no toda la feature ni todos los ejemplos de@s26. Reutiliza validación de identidad, revisión BIGINT, SessionStart, problemas y Location ya cubierta por las suites anteriores. No valida una marca temporal interna ausente del DTO. La prueba paused tardía usa fechas2026: no se presenta como nuevo oráculo de época superior a Number seguro; precisión general procede también del microseconds compartido probado14–16.

Pendientes tras revisión de este corte: coordinación local de decisiones/generaciones, panel17/timers y montaje en ambos recorridos, privacidad/recuperación/foco y eventual ampliación acotada de oráculos del cliente demandada por esos flujos. La cobertura propia@s28–44, navegadorUX y gates integrados todavía no está implementada ni certificada. Se congela cliente compilable para primer review por root antes de escribir mucha UI.

## UI y coordinación en curso

Cliente revisado por root y commit22b569a; liberado para siguientes ciclos. Aún sin freeze UI ni pruebas integradas globales.

| Ciclo | Oráculo | RED | GREEN |
| --- | --- | --- | --- |
| 21 | @s36 dos decisiones hermanas en el mismo evento adquieren sólo primera intención | 9ef0ae import ausente | 8d66ae, ref local inmediata |
| 22 | @s28 aviso confirmado y dos decisiones visibles | aa2a6a import ausente | 1f7758, panel/GET nominal |
| 23 | @s29 cantidad inicialmente vacía sin POST | eb1da4 | 5ecbc8, formulario nativo |
| 24 | @s35 POST explícito y consulta separada del nuevo fin | 845863 | a05ccb, recibo/refresh y fin actualizado |
| 25 | @s4 cantidad vacía sin transmisión/error asociado | 27e68d (además rechazo sin captura del envío indebido) | 3d4c83, validator integer reutilizado |
| 26 | @s43 anuncio pendiente/foco/doble submit/readonly | c76756 (además fetch extra indefinido por doble envío) | c83832, guardia ref/busy |
| 27 | @s37 POST503 conserva intención y K confirma sin otro POST | a0241e (rechazo sin capturar) | e9eed4, retención/check/incertidumbre |

Errores de runner en los ciclos25–27 procedían de envíos que el oráculo exigía impedir o capturar y desaparecieron con el mínimo; no se atribuyen como mutantes ni se ocultan como pases. El hook todavía no está integrado y el panel todavía no tiene todos los timers/errores/guardas; no se declara producto completo por estos nominales.

| Ciclo | Oráculo | RED | GREEN |
| --- | --- | --- | --- |
| 28 | @s34 E503 inicial, retry coalescido/carga/foco | 00a1c9, rechazo inicialmente sin capturar | 00e1b2 |
| 29 | @s39 closed conserva fin sin aviso/acciones | 97a5e1 | e53970 |
| 30 | @s30 plazo monotónico inicia GET antes de aviso | c6edad | f0e6f3 |
| 31 | @s31 plazo25d sin overflow ni GET adelantado | 1a04f8 | d2f230 |
| 32 | @s32 resto1µs usa delay1ms y rearma si anticipado | 0ce7a4 | f58125 |
| 33 | @s33 visible coalesce consulta pendiente | 17f535 | f753be |
| 34 | @s41 E HTTP401 antiguo no llega observer ni contexto nuevo | df3c49 | 68baca |
| 35 | @s42 E404 actual retira fin/borrador y propaga acceso | 85c714 | 9699d7 |
| 36 | @s37 K404 reconocido permite reenvío manual idéntico | 4432f3 | 69b703 |
| 37 | @s37 CSRF reconocido y renovación manual no autoenvían | b61aa2 | eea74c |
| 38 | @s41 POST HTTP401 anterior al cambio de sesión no revoca | b5ca73 | 76915b |
| 39 | @s38 412 conserva cantidad, GET válido y otra key/revisión manual | 7e4513 | b4e5d8 |

Antes del RED válido de33, ec2d9c sufrió timeout por el spy del timer falso anterior. Se restauran spies además de timers tras cada prueba y se repitió sólo el caso;17f535 mostró el fallo observable de ausencia de consulta al volver visible. No se atribuye el timeout de fixture como fallo de producto. Formato y tipos del corte parcial18UI+hook GREEN777ab3; no freeze ni aprobación de integración todavía.

### Coordinación entre paneles (ciclos 40–41)

- @s36: pausa pendiente bloquea el POST de ampliación hermano. RED original 8018a0; al recuperar el contexto, f1e1e0 confirmó que faltaba `aria-disabled` en el botón de confirmación. Guardia síncrona compartida y atributo visible: GREEN 32a830, 20/20 en panel y hook.
- @s40: pausa confirmada provoca lectura E nueva y la ampliación posterior conserva el borrador usando revisión 2. RED de2dd5 (una sola lectura E); generación local y liberación tras confirmación. Regresión e2a0ef detectó que el rechazo definitivo 412 retenía el propietario de la decisión y bloqueaba Reanudar; se libera también en esa rama. GREEN 12be34: 57/57 (20 panel17, 1 hook, 36 StatePanel heredados). No se cuentan los heredados como nuevos ciclos.
- Formato y TypeScript GREEN 152ae4. Producción aún WIP: falta invalidación coordinada de lecturas, dirección ampliación→estado y montaje real.

### Coordinación y privacidad (ciclos 42–46)

| Caso añadido individualmente | RED real | GREEN y ajuste mínimo |
| --- | --- | --- |
| @s40 EXTEND confirmado refresca State y permite Pausar con revisión 2 | 1d395d, State sólo tenía una consulta | e908d6, 58/58. `settle` notifica generación; `release` definitivo no provoca consulta automática. La regresión 35ced1 acreditó esa distinción con el 412 heredado. |
| @s41 Pausar aborta E hermano pendiente antes de HTTP401/observador | 75d1a4, observador recibió 401 | 1ecf08, 59/59; registro local de lecturas y aborto síncrono al adquirir intención. |
| @s41 EXTEND aborta State hermano pendiente antes de HTTP401/observador | 8c2f15, observador recibió 401 | c1a8b1, 60/60; State registra y retira su lectura en el mismo coordinador. |
| @s34 reloj posterior atrasado no retira aviso ya confirmado para el mismo fin | a37aa9, aviso desaparecía | 9d4705, 61/61; se recuerda el fin ya notificado, sin cambiar DTO ni reloj del cliente. |
| @s42 HTTP401 vigente retira fin/borrador y comunica pérdida de acceso | c2c87a, callback no recibido | ba8ba4, 62/62; clasificación HTTP independiente del problema JSON, misma retirada privada que 404. |

Las cifras incluyen 36 pruebas heredadas de StatePanel y una del coordinador; hay 25 casos del panel17. No equivalen a los 132 ejemplos Gherkin. Montaje real todavía pendiente; C prepara el único E2E nominal por separado.

### Corte de panel/coordinador para revisión selectiva (ciclos 47–53)

| Ciclo | Caso individual | Evidencia |
| --- | --- | --- |
| 47 | @s40 no enviar con E anterior mientras llega la lectura tras pausa | RED 08e420 → GREEN f865fa (63/63); generación de snapshot bloquea envío y anuncia consulta conservando cantidad. |
| 48 | @s41 confirmación invalida lectura iniciada durante el envío | RED 871d3d → GREEN dfdb47 (64/64); aborto síncrono también al confirmar. Oráculo público del coordinador con apiRequest y HTTP401, no sólo lectura de signal. |
| 49 | @s35 recibo histórico de ampliación sobrevive al error del E posterior | RED afe7a6 → GREEN e51e10 (65/65); artículo con minutos y ambos instantes históricos. |
| 50 | @s35 mismo recorrido: no conservar aviso del fin que el recibo ya amplió | RED 2bb6ce → GREEN 77dd53; se retira el aviso confirmado anterior sin inyectar el recibo en E. No nuevo caso ni conteo adicional. |
| 51 | @s43 foco al encabezado si desaparece el submit enfocado | RED d4b8e3 → GREEN e35ad5 (66/66); destino único tras respuesta. |
| 52 | @s43 no recuperar foco tras apartarse deliberadamente | Inicialmente GREEN 91bc8f; no modificación productiva. |
| 53 | @s27 EXTEND paused y E en 1600 con fracción .123457 | Inicialmente GREEN a8ef74; no modificación productiva. Época en microsegundos excede Number seguro; éste es el oráculo explícito, no el nominal 2026 ni el acumulado >Number heredado15. |

Se corrige etiqueta del ciclo45 a @s34: reloj atrasado, sin cambiar su evidencia original. El primer lint del tramo64890c detectó parámetro de fixture sin uso y dependencias del hook; refactor de tipos/desestructuración, formato/lint/types GREEN1973af. No se atribuye el EXIT final del comando compuesto inicial como prueba de lint correcto.

Alcance de este corte: panel, temporizadores, recuperación propia y coordinador local. La composición de tarea14 y lector16 continúa pendiente, incluidos GET A del padre, CLOSE hermano, bloqueo/refresh entre superficies y conflictos cruzados. No aprobación global ni evidencia de navegador17. C conserva E2E nominal inicialmente RED por montaje ausente. El corte permite revisión independiente de estos archivos sin confundirlo con UI17 terminada.

Freeze selectivo: `progress/end_time_frontend_panel_freeze.json` fija cinco archivos (panel/test, coordinador/test y el único refuerzo del test API). Estado compartido17 sigue disponible para montaje; su regresión36 está incluida. Verificación final: 88/88 en cuatro suites (29 panel, 2 hook, 21 API17, 36 StatePanel), GREEN1dc4ee; ESLint f3d87f, TypeScript35716a, formato fd78f1/5a0566. El lint previo dd9587 detectó lectura de ref en render; la generación visible pasa a estado React y se revalidó, sin desactivar reglas.

Pendiente de composición: Task14/Reader16, registro de GETactive del padre antes de decisiones hijas y al confirmarlas, CLOSE, conflictos cruzados y regresiones de fixtures pertinentes. Estos cinco archivos quedan quietos para revisión selectiva, sin declarar terminada la UI17 ni ejecutar globals.
`work-session-state.tsx` se añade al freeze selectivo para que el corte compile por sí solo: no cambió desde la ejecución88GREEN1dc4ee (último formato5a0566 lo marcó unchanged). Son seis archivos, incluidos los consumidores coordinados usados por esas pruebas; no se repiten suites por añadir el hash. Montaje continúa sólo en WorkSession/Reader.

### Montaje inicial y correcciones del dictamen (ciclos 54–57)

- 54: WorkSession14 muestra aviso/EXTEND y conserva Pausar. RED2edb23 → nominalGREENd34583. Regresión bbeabe: siete fixtures no respondían GET E (seis conteos + alerta activa); se añaden respuestas explícitas por ruta, conteos totales correctos y comprobación de una consulta E, conservando todos los oráculos anteriores. c02c90 reveló otro fixture401 cuyo consumo por orden hacía retirar acceso antes de la acción: pasa también a ruta A/S/E explícita. WorkSession44/44 GREEN051eeb. No se ocultan alertas ni se cambia producto para acomodar fixtures. Montaje aún no comparte decisiones: integración siguiente, no aprobado como completo.
- 55, R1 del juez: el caso @s40 existente ahora retiene la segunda GET S. REDf4d2ae acredita PAUSE enviada con estado anterior. StatePanel exige snapshot de su generación, anuncia espera y mantiene los asserts originales tras resolver S: GREEN14670b (67/67).
- 56, R2: E pendiente, EXTEND412 y consulta manual posterior. Primera ejecución f6bf76 usó un código ficticio de problema y se corrige sólo el fixture a PRECONDITION_FAILED; RED válido7be305 acredita dos lecturas en vez de tres. El aborto libera lookupBusy y carga, con listener retirado antes del cleanup de sustitución: GREENc78f73 (68/68).
- 57, R3: POST404 WORK_SESSION_NOT_FOUND vigente retira privados y llama onAccessFailure404. RED0f0c5b → GREENbcaea5:113/113 (31 panel17,2 hook,36 StatePanel,44 WorkSession). Se reutiliza la misma retirada de datos que GET E, antes del rechazo genérico.

Formato dac422, lint dcf329, tipos2fcd05 GREEN. Nuevo manifiesto de seis archivos `progress/end_time_frontend_panel_freeze_corrected.json`; el manifiesto anterior se conserva como evidencia del corte rechazado. API17 sigue con21 casos ya verdes; no se repite por estas correcciones de UI. Los seis archivos quedan quietos para re-review, mientras WorkSession/Reader continúan fuera de ese freeze. No global ni mutación.

### Composición de tarea (ciclos 58–60)

- 58: coordinación real de PAUSE y EXTEND en WorkSession, RED0933a2 → GREEN7642fe (45/45). Un contenedor local ligado a active.id comparte el coordinador existente; no globalstore ni nueva ruta.
- 59: GET A del padre pendiente antes de PAUSE no propaga401 antiguo: RED6ef293 → GREEN65431a (46/46). La adquisición aceptada aborta el controlador padre síncronamente y retira su espera.
- 60: GET A iniciado durante PAUSE se invalida al confirmar, y se consulta A fresco: RED6e5f68 → GREENc5ecdf (47/47). Conserva el estado paused, sin revocar acceso por la respuesta antigua.
- 61 en curso: montaje del mismo panel17 fuera del formulario CLOSE en Reader. RED0b667b → nominalGREENafe4d4. Regresión17ea7f detecta18 fixtures16 cuyo orden de respuestas no contempla E; no se declara GREEN integral del lector todavía. Se corrigen fixtures por ruta antes de continuar CLOSE/coordinación.

### Reader17 y coordinación (ciclos 61–66, en curso)

- Cierre de fixtures61: 18 fallos iniciales17ea7f eran consumo de respuestas CLOSE/K por la nueva E. `stubClosureRequests` sirve E por ruta en pruebas heredadas16: sus spies/oráculos siguen observando S/F/C/K/A; esto no equivale a comprobar toda la red17. Las nuevas pruebas17 instalan dispatcher completo y verifican E. No se eliminan asserts ni se usa first(). Tras tres fixtures inline pendientes26aaf3, Reader33/33GREEN9c07ee. Formato/types33cea6.
- 62: CLOSE pendiente bloquea EXTEND hermano: RED8975e3 → GREENc8ba5f (34/34), misma instancia local de coordinación.
- 63: EXTEND incierto mantiene CLOSE bloqueado y notas intactas: REDbaaa46 (faltaba aria-disabled, guardia ya impedía POST) → GREEN4f8ee1 (35/35).
- 64: EXTEND confirmado fuerza S nueva y CLOSE espera esa generación conservando notas: RED92113d → GREENaba97b (36/36), revisión2 comprobada en POST posterior.
- 65: recarga cerrada muestra fin durable ampliado sin controles: RED928ee8 → GREEN408f0a (37/37). Fixture coherente EXTEND previo revisión2 y CLOSE revisión3; GET E devuelve closed.
- 66 pendiente: CLOSE confirmado debe retirar controles/timer inmediatamente aunque E posterior siga pendiente y A descubra otra sesión. RED4c4fc1: permanece Ampliar tiempo del snapshot anterior. Reader ya invalida al confirmar y evita releer F propio tras recibo validado; prop mínima knownClosed del panel requiere liberar el freeze de re-review. No se afirma GREEN de este ciclo aún.
