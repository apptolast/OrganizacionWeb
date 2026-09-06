# Revisión independiente de mutantes frontend: schedule-block-api

2026-09-06, resume_review. Ponytail full / Caveman lite. Alcance exclusivo frontend/src/schedule-block-api.ts; frontend revisa los otros módulos. Sólo lectura: sin tests, producción, comandos de suite ni medición nuevos.

Informe final leído: frontend/reports/mutation/mutation.json, LastWriteTimeUtc2026-09-06T10:20:07.9211336Z (12:20 local),28772373 bytes. Contiene85 Survived para src/schedule-block-api.ts. Fuente actual SHA256 BFC93601AC6EFB2C9367604CA4D3AED76D1E6278AB7BA417C979DFA6C8BEBAAB. Se contrastó la fuente embebida y schedule-block-api.test.ts. No se calcula score ni se adjudican kills nuevos.

H = hueco observable propuesto; E = candidata a equivalencia en el contrato/JSON de red, requiere revisión independiente antes de adjudicarla. Las líneas son de la fuente embebida. Cada ID aparece una sola vez en el inventario. No se proponen85 tests: las matrices posteriores agrupan comportamientos.

## Inventario agrupado con IDs

| IDs | Líneas | Clase y razonamiento / vector concreto |
| --- | --- | --- |
| 5 |54| E: catch retorna undefined en vez de null; record rechaza ambos y readBlockError acaba en null. No cambia la interpretación de JSON ilegible. |
| 22 |60| H: quitar igualdad body.status/HTTP permite problema con cuerpo503 y HTTP409 si su code exige409. Variar sólo status del cuerpo manteniendo code/type válidos. |
| 29,30,31,32,33,34,35,38 |64–65| H: puertas BUDGET_EXCEEDED (code,HTTP409,esquema exacto,zona). Partir de days válido con exceso y variar independientemente code+type coherentes pero ajenos, HTTP+cuerpo de otro status, zona vacía, extra y sustitución de campo requerido. readBlockError debe resolver null, no error definitivo. |
| 41 |69| H: some→every rechaza error válido de varios días donde sólo uno tiene exceso. Dos días ordenados: uno exceso0 y otro positivo; aceptar el problema completo. |
| 56,57,58,59,60,63 |73–74| H: puertas BLOCK_OVERLAP. Matriz code/type ajenos, status400 coherente con cuerpo, campo extra y sustitución de campo; conflict UUID válido en cada vector para no ocultar la puerta. Resolver null. |
| 81,82,83,84,85,86,89 |83–84| H: puertas del error de offsets. Mantener error/validOffsets válidos mientras se cambia code/type, HTTP+cuerpo o esquema. Incluir errors array-like objeto para exigir null, no excepción por acceso posterior. |
| 92 |87| H: dos errores con primer AMBIGUOUS_OFFSET válido y segundo cualquiera; no aceptar como la respuesta cerrada de un solo error de ocurrencia. |
| 95 |90| H: code de campo IN_PAST con field=startOffset y validOffsets válido; no debe interpretarse como AMBIGUOUS_OFFSET/INVALID_OFFSET. |
| 103 |92| H: error AMBIGUOUS_OFFSET de field=objective y validOffsets.objective con opciones válidas; field no permitido debe resolver null. |
| 146,147,148,151 |110–111| H: validación ordinaria con code/type ajeno o HTTP+cuerpo409 y lista válida; resolver null. |
| 157 |115| H: every→some acepta errores mixtos [válido, código desconocido], o segundo con campo ausente/extra. Debe rechazar toda la respuesta, sin aceptar parcialmente. |
| 217 |216| H: readBlocks debe aceptar exactamente20 DTO válidos; límite>20→>=20 los rechaza. Contrastar21 rechazados sin replicar cada DTO. |
| 280 |274| H: id=[UUID] en DTO de lista; regex coacciona array unitario a UUID si se retira typeof. Usar readBlocks porque readBlock también compara id y podría enmascararlo. |
| 283,284 |275| H: id con prefijo/sufijo en DTO listado. Anchors son necesarios para UUID del bloque (no confundir con sameId de contexto). |
| 335 |295| H: DTO listado con objetivo rodeado de whitespace se acepta al retirar normalización. Debe rechazarse. |
| 337,340 |296| H: objetivo de501 puntos Unicode en DTO listado; retirar límite o vaciar array de conteo lo admite. |
| 338 |296| H: objetivo de exactamente500 puntos Unicode en DTO válido debe aceptarse. |
| 341 |301| H: DTO listado con instantes60min y durationMinutes59; sigue siendo rango válido pero incoherente. Debe rechazarse. |
| 359,361,363 |310–312| E condicionada a preview retenido válido: isBlock obliga duración=(end-start); preview válido tiene igual relación. Cualesquiera dos igualdades de inicio,fin,duración implican la tercera. Retirar una igualdad no permite confirmar otro intervalo bajo esa precondición. readBlockByRequest recibe preview del estado validado; no inventar un preview interno inválido para reclamar fallo de protocolo. |
| 388 |339| E en JSON: tras quitar typeof object, null y arrays siguen excluidos; primitivas restantes no poseen campos nominales requeridos y exact/text las rechazan. Funciones/prototipos artificiales no llegan mediante Response.json. |
| 403 |345| E candidata por consumo posterior, no por count solamente: todo campo de cada esquema exact se lee/valida después (problema base,conflict,error,DTO,preview,page,day y clave validOffsets). Sustituir una key para mantener cardinalidad deja undefined en guard posterior. Revisar esta justificación si aparece un campo no consumido; no considerar every→some equivalente en general. |
| 408,411 |349| H: strip con cuantificador de un solo whitespace en borde cambia normalización de input de preview. Usar varios espacios Unicode al inicio/fin y preview.objective correctamente recortado; debe aceptarse. Sólo probar DTO sin whitespace no lo distingue. |
| 432 |359| E: Number.isSafeInteger ya exige valor de tipo number; el typeof anterior es redundante, incluidos NaN,Infinity y strings numéricos. |
| 449 |367| E candidata por resultado contractual: ISO en array puede pasar regex/Date.parse al retirar typeof, pero falla value.replace y la operación sigue rechazando el DTO. En JSON de red instant no produce confirmación inválida; no justificar mediante igualdad del texto de excepción interna. |
| 452,453 |368| E candidata: aunque se quite un anchor ISO, la comprobación de fecha real y comparación ISO normalizada con el string original aún rechaza prefijo/sufijo. La igualdad no elimina texto ajeno. |
| 473 |373| E: regex previo exige terminar en Z; quitar anchor del replace de sufijo no encuentra otro Z en el formato aceptado. |
| 484 |378| H: validOffsets contiene array unitario ["+01:00"] como elemento; sin tipo el exec lo convierte a string y acepta. Debe resolver null. |
| 487,488 |379| H: offset con prefijo/sufijo en validOffsets, aislando formato (mantener orden numérico válido). Debe resolver null. |
| 516 |385| E: hours<=18 está implicado por total<=64800 con minutos/segundos extraídos como enteros no negativos. |
| 517,526 |385/388| H: opciones canónicas +18:00 y -18:00 deben aceptarse (INVALID_OFFSET permite una opción); ambas fronteras son inclusivas. |
| 537 |391| E algebraica: dividir por ±1 es idéntico a multiplicar por ±1 para total entero positivo finito del rango validado. |
| 550 |397| H: quitar regex+instant deja Date.parse permisivo. Input local con separador espacio, por ejemplo2030-01-07 10:00, y respuesta UTC correspondiente; preview debe rechazarse por formato local canónico. |
| 551 |397| H: AND→OR deja pasar etiqueta con fecha inexistente pero forma correcta, p.ej2030-02-30T10:00; Date.parse normaliza a marzo. Respuesta/offset/días coherentes con fecha normalizada deben seguir rechazados. No asumir parseo permisivo fuera del motor fijado sin demostrar vector en test. |
| 552,553 |397| E: aunque el regex local pierda anchor, instant(local+":00Z") sigue exigiendo formato completo canónico y fecha real. |
| 573,574 |405| H de robustez: error presupuesto con days={length:1}, o null al retirar ambas guardas, no debe lanzar al invocar every; readBlockError debe resolver null para cuerpo incompatible. Si se evalúa sólo aceptación/rechazo genérico se enmascara esta diferencia. |
| 575,576 |406| E contextual: [] pasa every pero preview exige suma>0 mediante duración1..1440 y error presupuesto exige some(exceso>0); ninguno acepta vacío. |
| 578 |407| H: days=[día válido con exceso, día inválido] en error presupuesto; some en vez de every acepta parcialmente. En preview usar suma de requestedSeconds coherente para no enmascararlo con total. |
| 598 |413| H: day.date=["2030-01-07"] se coacciona al concatenar T00:00:00Z si se elimina typeof. Un único día de error presupuesto evita coerción del orden. Debe resolver null. |
| 602,607 |415| H: error presupuesto con dos días invertidos o repetidos, cada uno aritméticamente válido y al menos un exceso. No usar preview duplicado con suma incorrecta, que oculta el mutante. |
| 620 |430| H: availabilityEtag=[tagVálido] en preview; sin tipo exec coacciona array y permite etiqueta no textual. Debe rechazar preview. |
| 623,624 |431| H: prefijo/sufijo de ETag válido no altera grupo numérico pero lo vuelve no canónico; debe rechazarse. |
| 640,641,642 |431| H: versiones positivas canónicas1,10,100 y BIGINTmax distinguen regex alterados (641 exige exactamente2 dígitos). Añadir negativo -1/01 como defensa coherente:640 puede aceptar -1 por BigInt negativo. |
| 647 |434| H: versión exacta9223372036854775807 debe aceptarse; ya existe max+1 rechazado, no cubre inclusión del máximo. |
| 683 |443| H: input y preview.objective coherentes de exactamente500 puntos Unicode deben aceptarse. Complementa frontera de DTO338, son dos puertas distintas. |
| 696 |450| H: input.endOffset explícito+01:00 frente a preview.endOffsetZ, con at/local coherentes conZ; retirar coincidencia autoriza ocurrencia diferente. Mantener resto válido y exigir rechazo. |
| 702 |452| H: preview instantes60min, duration59 y requestedSeconds3540 con exceso correcto; el total de días coincide con59 y no enmascara incoherencia temporal. Debe rechazarse. |
| 716 |463| E: instant ya restringe sufijo al Z terminal y dígitos decimales; quitar anchor del detector de fracción no cambia el conjunto aceptado. |
| 719 |463| H: wholeInstant con .000Z es segundo entero válido y debe aceptarse; el regex mutado detecta cero y lo rechaza. |
| 721 |463| H: extremos con .100Z ambos y duración coherente; el patrón mutado no detecta fracción no nula terminada en ceros. Debe rechazarse aunque Date.parse conserve duración exacta. |

## Matriz mínima propuesta para autoría TDD

Reutilizar helpers problem/input/preview/block y las matrices negativas existentes. Cada fila es un comportamiento parametrizable, no una prueba por ID; avanzar un caso/ciclo según Ponytail, sin meter toda esta propuesta de golpe.

1. **Fronteras aceptadas:** lista20; objetivos500 Unicode en DTO y preview; versiones1/10/100/MAX_BIGINT; offsets±18:00; instantes.000Z. Positivos necesarios frente a mutantes que endurecen contratos válidos. IDs217,338,683,640–642,647,517,526,719.
2. **Problemas especializados cerrados:** fixture completamente válido por BUDGET/OVERLAP/VALIDATION offsets/VALIDATION ordinaria, cambiar sólo code+type o HTTP+body.status; esquema extra/sustituido y body.status discordante separado. Afirmar resolves.toBeNull, no sólo rechazo genérico. IDs22,29–38 listados,56–63 listados,81–89 listados,146–151 listados. Añadir offsets con dos errores/código o field ajenos para92/95/103.
3. **Colecciones mixtas:** errores [válido,incompatible]→null; días [sin exceso,con exceso]→aceptar; días [válido,incompatible]→null; orden invertido/duplicado con aritmética válida→null. IDs41,157,578,602,607.
4. **Coerción y canonicalidad de primitivas:** arrays unitarios en id/offset/date/etag; prefijos/sufijos UUID/offset/etag; days objeto/null. ID280,283,284,484,487,488,598,620,623,624,573,574. No introducir objetos con getters o prototipos imposibles en JSON.
5. **Coherencia temporal e intención:** DTO duración59 con instantes60; preview duración59+días3540 pero instantes60; endOffset explícito discrepante; local espacio/fecha normalizada; fracción.100Z. IDs341,550,551,696,702,721. Cada vector conserva todas las otras invariantes para alcanzar la guarda objetivo.
6. **Objetivo normalizado:**501 Unicode rechazado en DTO, whitespace de DTO rechazado; input con varios whitespaces Unicode correctamente normalizado aceptado. IDs335,337,340,408,411.

Las candidatas E no autorizan exclusiones automáticas. Falta validación independiente caso a caso, especialmente449 (rechazo contractual frente a excepción),403 (todos los campos consumidos) y359/361/363 (preview retenido válido). Sin modificar código sólo para matar mutantes redundantes. No se atribuye cobertura de task-blocks, reader, state o error, que pertenece al otro revisor.

## Resolución independiente del coordinador

Root contrastó la matriz con el flujo completo de schedule-block-api.ts (47f5ff/5485bb). Acepta las 18 equivalencias contextuales de los IDs5,359,361,363,388,403,432,449,452,453,473,516,537,552,553,575,576,716 con los argumentos individuales anteriores, sin excluirlas de la medición ni recalcular el porcentaje.

Para403 se comprobaron los consumidores de exact: lista exige items y nextCursor; DTO valida sus nueve campos; preview valida sus diez campos; problemas y días consumen cada campo requerido, incluido el nombre dinámico de validOffsets antes de validar sus opciones. Sustituir una clave para mantener cardinalidad deja una propiedad requerida sin valor y sigue impidiendo aceptación. Para449 se acepta equivalencia de rechazo en las operaciones de DTO/preview; no se exige identidad de una excepción interna. Las fechas de days se concatenan en una cadena antes de instant, así que no trasladan esa excepción al protocolo readBlockError. Para359/361/363 la equivalencia exige preview ya validado y retenido en la composición productiva; cambiar esa precondición obliga a revisarlas.

Se devuelven al autor las seis matrices propuestas para los huecos observables, manteniendo asserts de null para problemas incompatibles y de rechazo para DTO inválido. Esta revisión no adjudica nuevos kills ni cierra la feature. Los otros módulos y el error945 conservan su revisión separada.
