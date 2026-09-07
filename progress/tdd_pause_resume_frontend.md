# TDD frontend15 — pausa y reanudación

Gate coordinador aprobado sobre contrato a5c556f, feature in_progress5367b35. Una prueba/caso por ciclo, sólo foco Vitest; no globals ni backend/metadata/Git. Módulos iniciales son un corte incompleto hasta cerrar validaciones y composición. Ponytail full/Caveman lite, validadores14 reutilizados cuando lo exija un caso.

| Ciclo | Contrato y oráculo | RED | GREEN | Mínimo |
| --- | --- | --- | --- | --- |
| 1 | @s1 lectura state de inicio existente, token propio y señal/no-store | 4a5598 import ausente | f7f463 1/1 | Módulo y lectura nominal |
| 2 | @s27 token de otra revisión se rechaza | ba3a8a resolvía | 2080d9 2/2 | Correspondencia header/cuerpo |
| 3 | @s24 HTTP503 conserva Response para clasificación | 14986a TypeError/cuerpo consumido | 39204c 3/3 | Exigir200 antes de JSON |

Ningún test nuevo se declara prueba de backend ni toda la matriz contractual. Siguientes ciclos: validación cerrada/identidad/µs del estado, luego recibos y acciones antes de UI.

## Continuación de validación de lectura

| Ciclo | Oráculo @s27 | RED | GREEN | Cambio |
| --- | --- | --- | --- | --- |
| 4 | Campo extra snapshot | a4c856 | 06ebd6 4/4 | Forma cerrada |
| 5 | Campo extra state | 178a37 | c66b79 5/5 | Forma exact6 |
| 6 | SessionStart interno con fin desviado1µs | faead3 | 0ad39d 6/6 | Export/reuso del validador14 intacto |
| 7 | ID solicitado distinto del estado válido | 5399b1 | 6386ec 7/7 | sameId contextual heredado |
| 8 | Revisión BIGINT+1 con header coincidente | dbc51c | 5d0d21 8/8 | Decimal canónico positivo/rango exacto |
| 9 | changedAt unµs anterior al inicio | f9f3f3 | d0fca7 9/9 | Export/reuso conversiónµs14 y orden exacto |
| 10 | Status fuera de running/paused | 4f1639 | 244745 10/10 | Estados cerrados |
| 11 | runningSince distinto1µs de changedAt | 2268d9 | 02949b 11/11 | Igualdad temporal exacta |
| 12 | paused conserva intervalo abierto | bd9650 | efbda1 12/12 | runningSince null al pausar |

Los exports14 no alteran sus validadores, rutas ni comportamiento. Cada fila se añadió después del GREEN anterior; no se introdujo una matriz completa antes de implementación.

| Ciclo | Oráculo | RED | GREEN | Cambio |
| --- | --- | --- | --- | --- |
| 13 | @s27 acumulado supera tiempo disponible1µs | 1948fa | 0c3232 13/13 | Decimal no negativo y cota exacta |
| 14 | @s27 neto snapshot desviado1µs | 415d6a | 80f831 14/14 | Fórmulaµs reutilizada |
| 15 | @s23 reloj de lectura anterior | 032aa2 | 64ec85 15/15 | Aporte abierto mínimo0 |
| 16 | @s28 paused con revisión/neto superiores aNumber | Inicialmente GREEN | 337bba 16/16 +types/formato | Refuerzo sin producción |
| 17 | @s2 POSTpause nominal, cuerpo/key/header/señal | a42881 función ausente | 30b2f2 17/17 | Envío nominal |
| 18 | @s3 POSTresume ruta/revisión paused | 6f88e0 | 419f76 18/18 | Ruta según acción |
| 19 | @s33 POST412 conserva Response | 36095f | 763d04 19/19 | Aceptar sólo201/200 |
| 20 | @s27 recibo con campo extra | eaba35 | 0734c3 20/20 | Forma exact6 |
| 21 | @s27 before inválido | c860d3 | feae04 21/21 | Reuso validador state |
| 22 | @s27 after imposible | 5446f2 | b0d0ad 22/22 | Reuso validador state |

Corte sigue incompleto: faltan coherencia entre before/after/intención, Location y recuperación antes de comenzar UI. Los GREEN iniciales se conservan como refuerzo, sin fabricar RED.

## Recibos, intención y recuperación

| Ciclo | Oráculo | RED | GREEN |
| --- | --- | --- | --- |
| 23 | @s27 revisión avanza exactamente1 | 08a265 | 096107 23/23 |
| 24 | @s27 SessionStart inmutable (zona) | 543a30 | f03687 24/24 |
| 25 | @s27 occurredAt coincide con after | d11fb3 | 8c58cb 25/25 |
| 26 | @s27 acción/transición compatibles | ad1fe5 | 235a97 26/26 |
| 27 | @s27 suma exacta pausa (no omitir1µs) | 0fb4ad | 3a0fa0 27/27+formato |
| 28 | @s27 sessionId del recibo coherente | d26d73 | 1fdd0b 28/28 |
| 29 | @s27 revisión de intención retenida | b71ecd | 92cb5b 29/29 |
| 30 | @s27 sesión capturada de intención | 8b2e43 | 12f11c 30/30 |
| 31 | @s27 acción retenida coincide | 05f4f7 | 1a0524 31/31 |
| 32 | @s27 Location POST del mismo recibo | a16e0b | 89c9cd 32/32 |
| 33 | @s27 id recibo UUID válido | 2349b5 | cbbdbb 33/33 |
| 34 | @s27 reloj de transición no retrocede | cad9b9 | 4cda6c 34/34 |
| 35 | @s25 GETid sin Location | 69ba22 función ausente | 48247d 35/35 |
| 36 | @s27 GETid identidad solicitada | d3e39f | a91c79 36/36 |
| 37 | @s24 GETid503 conserva Response | 8e4f55 | fc0451 37/37 |
| 38 | @s25 GETkey sin POST y con señal | f45bb9 | 6a84a7 38/38 |
| 39 | @s32 GETkey corresponde a intención | 4ce5ac | 6f8ba2 39/39 |
| 40 | @s32 GETkey404 conserva Response | 2d61bc | e774bc 40/40+formato |
| 41 | @s32 problema CHANGE_NOT_FOUND | 0e0bf9 | 26e59f 41/41 |
| 42 | @s33 problema PRECONDITION_FAILED | a3cd4b | 684101 42/42 |
| 43 | @s34 CSRF heredado | ba8e03 | c3729d 43/43 |
| 44 | @s14 STATE_CONFLICT | 297f2f | da2e7f 44/44 |
| 45 | @s15 REVISION_EXHAUSTED | 9d2aeb | 53021e 45/45 |
| 46 | @s32 problema contradice status | Inicialmente GREEN | 7d5a5c 46/46 |
| 47 | @s16 POSTreplay200 y Location | Inicialmente GREEN | f3e812 47/47 |

Incidente de test en ciclo35:90db45 falló por import omitido al añadir el test; corregido sólo import, RED válido69ba22 por función ausente antes de producción. No se confunde el incidente con fallo funcional. No se contabilizan todas las variantes heredadas como casos nuevos. Problemas comunes de14 se delegan en su lector existente; nuevos15 mantienen forma/code/type/status coherentes. UI aún pendiente al cerrar este corte de cliente.

## Panel: primeros ciclos públicos

El corte API se verificó con 90/90 (47 nuevos y 43 API14) en c06d50; formato, ESLint y tipos en 18ef8a. Salida conservada en herramientas, sin archivo log independiente. Root revisó los hashes y aprobó parcialmente el cliente; no constituye gate UI15.

| Ciclo UI | Comportamiento | RED | GREEN |
| --- | --- | --- | --- |
| 1 | @s29 estado running y neto del snapshot, sin token visible | 7ec727 módulo ausente | 7b9d12 1/1 |
| 2 | @s29 paused ofrece sólo reanudar | b128d1 | 212c43 2/2 |
| 3 | @s29 consulta pendiente no infiere running del recibo | b320cc | f14545 3/3 |
| 4 | @s30 pausa explícita y lectura del estado posterior | e13d01 | 133240 4/4 |

Panel aún en construcción; faltan recuperación, errores, privacidad, concurrencia, formato legible e integración14. Ninguno se declara terminado por este nominal.

| Ciclo UI | Comportamiento | RED | GREEN |
| --- | --- | --- | --- |
| 5 | @s38 foco y coalescing durante POST | ae1e8f | 6049ff 5/5 |
| 6 | @s29 fallo GET y reintento manual | 26df7f, también rechazo sin capturar | b128cf 6/6 |
| 7 | @s31 recibo confirmado permanece ante fallo GET posterior | e9597f | 4f3227 7/7 |
| 8 | @s32 recuperación por key tras POST503 | 462fc8 | bab9d4 8/8 |
| 9 | @s32 GETkey404 y reenvío manual misma intención | 8cb7be | 0aabbd 9/9 |
| 10 | @s36 abortar comando al desmontar | 892202 | fe2754 10/10 |
| 11 | @s36 401 vigente propaga retirada | 37239f | 32f55d 11/11 |
| 12 | @s36 401 tardío retirado no afecta padre vivo | b47129 | 0e1fb7 12/12 |
| 13 | @s33 412 exige consulta manual y nueva decisión/key | 3f334e | a85222 13/13 |

Incidente del ciclo UI9: fixture inicial usó urn incompleto y por ello 4a0647/ac8ab0 no acreditaban el problema contractual. Se corrigió el fixture, se retiró la condición productiva pendiente y se observó RED8cb7be con el problema válido antes de restaurar el mínimo. No se cuenta como dos ciclos ni como regresión del cliente.

| Ciclo UI | Comportamiento | RED | GREEN |
| --- | --- | --- | --- |
| 14 | @s36 GETstate401 vigente retira padre | 9f7f0b | cfb1e5 14/14 |
| 15 | @s36 cambio de identidad retira comando/estado | a56dec | 08bba0 15/15 |
| 16 | @s29 refresco deliberado con snapshot fechado | 6b395f | c3a906 16/16 |
| 17 | @s39 GET anterior retirado al transmitir y no restaura running | 43ed46 | 4e6d80 17/17 |
| 18 | @s34 CSRF conserva intención; renovación y reenvío separados | 03339c | e935f7 18/18 |
| 19 | @s28 presentación conserva un microsegundo | 709f42 | ef2c7c 19/19 |
| 20 | @s35 salir no revoca envío | d74350 | 3869fa 20/20 |
| 21 | @s37 iniciador desaparece: foco al encabezado | 83ead7 | 3603b2 21/21 |
| 22 | @s37 no robar foco trasladado a otro control | Inicialmente GREEN | e8596d 22/22 |
| 23 | @s30 fecha legible con fallback UTC etiquetado | f13393 | 38de40 23/23 |
| Composición 1 | @s29 active paused permite reanudar con tarea/proyecto completed | 233e78 | b458e6 66/66 entre panel y WorkSession |
| 24 | @s14 STATE_CONFLICT es rechazo explicado | 5d6299 | c36d57 24/24 |
| 25 | @s15 revisión agotada es rechazo explicado | b23052 | d9fa05 25/25 |
| 26 | @s6 rechazo temporal es definitivo | 3ec128 | fc2fb4 26/26 |
| 27 | @s12 sesión no encontrada retira decisión | 69d804 | 2cba16 27/27 |
| 28 | @s4 pausa seguida de reanudación reemplaza recibo visible | Inicialmente GREEN | d5076d 28/28 |
| 29 | @s37 foco durante reintento inicial de consulta | 2f066a | bde14c 29/29 |

Incidente de edición UI16: 176b3e registró SyntaxError del script de edición, que no modificó la fuente; corregido el script y alcanzado c3a906. No se atribuye al producto.

La integración añadió el GETstate requerido por15. Regresión12b21e detectó seis conteos14 que asumían que la recuperación/confirmación no consultaba estado: se mantienen sus oráculos y se añade una llamada exacta `/work-sessions/{id}/state`. 72f54c detectó un fixture401 que ocupaba esa llamada nueva; se insertó un snapshot válido antes del 401 de refresco active, manteniendo la retirada de datos exigida. b458e6 confirmó ambos grupos. Los errores de GETstate no eliminan el recibo14, cuya semántica sigue histórica.

Cierre de composición adicional: el refresco fallido durante incertidumbre podía sobrevivir al GETstate válido posterior. Oráculo UI30 RED990e3e → GREEN66ef03: la respuesta vigente elimina el error de consulta anterior sin alterar el recibo ni la key.

Refactor en frontera verde: completadas las guardas de aborto después del await de éxito y de clasificación del comando, además de la guarda antes del callback401 ya reproducida en UI12. La composición por identidad desmonta el contexto antiguo; no se presentan esas guardas adicionales como nuevos RED independientes.

Verificación56f4f2/f82d73: formato y ESLint focales, TypeScript y 73/73 (30 panel +43 WorkSession) verdes. Los patrones task-reader.test.tsx/session-gate.test.tsx no correspondían a archivos existentes; no se les atribuyen pruebas. La suite real work-session-integration.test.tsx produjo RED5ae21b por dos rutas GETstate no previstas en su fixture. Se añadió el snapshot cerrado de esa ruta, conservando ambos oráculos de TaskReader/SessionGate: c36e64 2/2 GREEN. Ningún cambio en TaskReader ni SessionGate.

Incidente de tipos fb1155: `exact` no es una opción de ByRole aunque las búsquedas por nombre string ya son exactas. Se retiró la opción redundante de las pruebas nuevas; formato y TypeScript3e2a91 verdes. No se relajó ningún nombre ni expectativa.

## Corte de producto para revisión

Build, tipos y ESLint finales 4ca699 GREEN. No campaña de mutación ni navegador en este corte.

| Archivo | SHA256 |
| --- | --- |
| frontend/src/work-session-state-api.ts | 38743D9B82869708BD53C98850DF8C71FAEFB7CD0BE5348519679D8E32AB2FAA |
| frontend/src/work-session-state-api.test.ts | 7AC5B1B3CDC1DE502CF6CB801566B4A4B54C8315777CE0A884C29B1A5FF31090 |
| frontend/src/work-session-api.ts | 8B37B3C856FC208EB8A873A676D757E3C8945A60470C50A2739A7602540520C3 |
| frontend/src/work-session-api.test.ts | 8E6C39B5A0594188F44A19F1B8B8AD052676F0DF7066B0F71C0069D65C850485 |
| frontend/src/work-session-state.tsx | 81A0A604357A91195B70C592061B1D56D8BF0587573DF06BF7166D46668EA9A8 |
| frontend/src/work-session-state.test.tsx | 153C7FFF3FAFF35C0B81998ED99846407BD4A85A9D0A0ED143DCDCDDE6DCABBB |
| frontend/src/work-session.tsx | E2CDBC83BD8268CA90C19ACA78A4ADA9F912549160FA50628EB381C22275B5DE |
| frontend/src/work-session.test.tsx | E354D8A330D14E9041AA053E1418A17A527DD0D2DB7EDCACF273E77BD2FFA351 |
| frontend/src/work-session-integration.test.tsx | 4C5D8CFC34D02059ED83A03CFCDCEA5E8898F062B6A6A6502DAC396F39F9D84A |


Cierre final de oráculos de retiro: UI31 JSON de éxito diferido y UI32 clasificación no401 diferida, añadidos individualmente e inicialmente GREEN (6a5e62, d78cb5). El caso JSON usa replay HTTP200 en el corte final; la reejecución d78cb5 confirma esa variante. No se atribuye RED nuevo a las guardas simétricas ni se confunde montaje por identidad con validación del protocolo.

**Freeze producto:** f04253/240bef formato, ESLint, tipos y 77/77 (32 panel,43 WorkSession,2 integración real de componentes) verdes. API47 y regresión14 de43 ya acreditadas en c06d50; no se repitieron sin cambios. Build4ca699 sigue vigente: las dos últimas adiciones son sólo tests. Tabla SHA anterior actualizada al corte final. Quedan revisión independiente, init coordinado, navegador y mutación; no se declara feature15 terminada.

### Trazabilidad y alcance UX

- @s25–28: cliente cerrado, cabecera propia exacta, precisión BigInt/µs, recepción histórica porID/key y correspondencia de intención. Se reutilizan SessionStart y problemas comunes14; no se atribuyen sus matrices como nuevas.
- @s29–31: descubrimiento active14 integrado con GETstate, running/paused, fin histórico intacto, hora/neto fechados, UTC histórico y separación de confirmación/consulta fallida.
- @s32–35: intención retenida, incertidumbre y comprobación, reenvío manual exacto tras404/CSRF, rechazo412 con decisión nueva y advertencia de salida. SessionGate14 mantiene renovación; el test de panel verifica token renovado e intención sin POST automático. No hay cierre/aviso/historial de16–18.
- @s36 y39: abort al desmontar/cambiar identidad, 401 vigente frente tardío, JSON y clasificación diferidos, snapshot viejo descartado tras comando confirmado.
- @s37–38: anuncios separados, aria-disabled con guarda real, foco al encabezado cuando desaparece el iniciador y respeto por otro control. Reutiliza botones, tipografía, wrapping y espaciado SCSS nativos de task-blocks/work-session; sin nueva dependencia ni breakpoint.
- La matriz UX30 de docs/ux-requirements.md sigue siendo requisito de validación posterior. Estos tests acreditan lenguaje, estados, decisiones, precisión y foco en JSDOM. No miden 44px, reflow, zoom nativo, velocidad física de feedback, axe ni motores; esas filas quedan pendientes de navegador real, sin afirmaciones de usabilidad humana.

### Selector frontend15 preparado, sin ejecutar

`frontend/stryker.pause-resume-session.config.json` reutiliza el runner14 y selecciona completos `work-session-state-api.ts`, `work-session-state.tsx` y `work-session.tsx` (composición14 modificada). `work-session-api.ts` sólo añade export a dos funciones preexistentes, sin cambio de cuerpo ni nueva rama; no se vuelve a medir su lógica inalterada como cambio15. TaskReader/SessionGate/SCSS no fueron modificados por15.

Reportes propios `reports/mutation-pause-resume-session/mutation.json`/`mutation.html`, temporal propio `.stryker-tmp-pause-resume-session`. Se conserva `ignorePatterns` protegido exacto, break80/high90/low80, concurrency8, perTest y Vitest. bb029d verifica JSON, existencia de tres fuentes y parámetros heredados; formato GREEN. No se ejecutó Stryker, no se modificó dispatch ni default. Root revisará configuración y coordinará gate integrado antes de campaña.

## Delta de revisión root antes de navegador completo

Hallazgo de espera533026 reproducido con snapshot existente → refresh503 → reintentoGET diferido: nuevo UI33 REDdee656 → GREEN9e23ec. El reintento ahora activa loading. Variante de la misma espera tras recuperar un recibo: ampliación del caso30 con GETstate diferido produjo REDc0ddd0 porque conservaba error del refresco anterior; mínimo limpia ese error y activa loading al confirmar antes de consultar. GREENbb708e33/33. Formato, ESLint y tipos29c10b verdes. Nuevo freeze: panel SHA4FCD95AC8BCCDE4AAB91137A0F4970378F81DAE078FC0318F4BBB017D26A1599; test C203284CCE4FF7F5C5664D840C5ABD4946ACE7DA27451AD379254B90908F9046. El resto de hashes de producto permanece igual. La próxima imagen E2E incorpora este delta; no se repite smoke backend por un cambio exclusivamente frontend.

## Refuerzo acotado tras mutación original

Dos oráculos adicionales, introducidos y ejecutados uno a uno; no se modificó producción ni se ejecutó mutación. El resultado original de861 mutantes permanece independiente de este refuerzo.

1. Panel @s32: POST incierto seguido de GET por key con503 y problema desconocido. La incertidumbre permanece, Comprobar sigue disponible y no aparece Reenviar ni Pausar; se verifica key retenida y exactamente un POST. Primera ejecución válida inicialmente GREEN1b3364 (1 nuevo/33 filtrados). Incidente previo04905e: ruta de escritura duplicaba frontend, no se añadió el test y Vitest omitió33 por filtro; no se cuenta como ejecución del nuevo oráculo ni RED.
2. Cliente @s32: recuperación por key devuelve RESUME internamente coherente frente a intención PAUSE, conservando SessionStart y revisión esperada1. El recibo pasa las relaciones internas de RESUME (paused→running, revisión2, acumulado0, mismo occurredAt/runningSince) y se rechaza por intención opuesta. Primera ejecución inicialmente GREEN69dbcb (1 nuevo/47 filtrados). No se alteró el recibo para provocar un rechazo trivial de esquema.

Después: Prettier focal c661be EXIT0; ambos archivos completos82/82 GREEN34f600; ESLint focal f6c143 EXIT0; diffcheck c23294 EXIT0. Diff limitado a57 líneas añadidas en los dos tests. No campaña global ni backend Gradle.

Freeze de pruebas: work-session-state.test.tsx SHA25692ABFCB589938D32CAD198AF605BC7B42E9A39BB75BC489C8D87046A36086FC5; work-session-state-api.test.ts SHA256F7B72E82E722114EA02237B344CD82EE5BB0F9C3CB831AA96B766361EBF191C7. Fuentes intactas: panel4FCD95AC8BCCDE4AAB91137A0F4970378F81DAE078FC0318F4BBB017D26A1599; API38743D9B82869708BD53C98850DF8C71FAEFB7CD0BE5348519679D8E32AB2FAA. Los IDs candidatos401/409/410 y266/267/269 no se declaran Killed hasta medición independiente.

## Corrección de fixture CI15, sin cambio de producto

CI34076959702 conservado en progress/ci_pause_resume_final_failed.log:1667/1668, falla el caso14 @s37 «confirms the original end and preserves the receipt if active refresh fails» por dos role=alert. Causa: sus tres respuestas secuenciales eran active=null, POST201 y503; desde15, GET state consume el503 y el posterior GET active recibe undefined del mock agotado. Quedan dos errores de consultas distintas. La aserción global podía pasar prematuramente mirando el error de state o fallar al observar ambos; no es fallo de los anuncios del producto.

Reproducción: el foco sin cambios pasó fd591c, confirmando sensibilidad al orden. Instrumentación temporal exclusiva del test esperó ambos textos de error antes de la aserción original: RED04acda reproduce las dos alertas exactas de CI. Se retiró esa instrumentación al corregir la fixture; no se presentó la pasada inicial como un RED.

Arreglo sólo en ese caso de work-session.test.tsx: respuestas por URL, GET state15 válido con token/snapshot exactos, primera consulta active=null y refresco active503, POST201 independiente. Una URL no prevista falla explícitamente. Conserva fin original, confirmación histórica y ausencia de nuevo inicio; exige texto preciso de alerta de active, estado En curso válido, dos consultas active y un único POST. No first(), ocultación de alertas ni relajación del producto.

Se revisaron las otras ocho consultas get/find/queryByRole(alert) del archivo (1362f8/c3f03a). Las restantes aserciones de alerta corresponden a ausencia/inicio rechazado antes de montar state, o contextos retirados; no se encontró otra aserción hermana dependiente del GET añadido. No se extendió el parche a43 fixtures.

Verificación: formato focal5073f0,43/43 GREENab95e7; frontend completo1668/1668 en33 archivos GREEN8c78b0 (sesión9407,20,80s), ESLint focal c6c67f EXIT0. No backend/Gradle ni mutación ni código16. Este cambio de fixture no altera fuentes productivas ni el resultado original de mutación.
