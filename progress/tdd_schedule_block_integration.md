# Integración real de schedule_block

Feature in_progress. Ponytail full/Caveman lite. Coordinador autorizó E2E; no mutación ni commits. Se conserva el runner existente `scripts/e2e.mjs`, PostgreSQL y stack Docker aislados.

## Preparación y límites

Ocho fixtures históricos incluyen planned_blocks explícitamente en TRUNCATE por la FK de V11, sin CASCADE global. `e2e/schedule-block.spec.mjs` contiene la nueva suite y `cross-browser.config.mjs` incluye su smoke. `.dockerignore` añade sólo `frontend/.stryker-tmp-availability-replay/` y `**/proposal_schedule_block_time.md`: exclusiones necesarias para no leer esos destinos bloqueados al empaquetar el contexto. No se leyeron/modificaron contenidos ni se limpiaron esos destinos o sus ascendientes. El coordinador confirmó esta precaución.

## Primer smoke real

Autor backend confirmó snapshot de 105 HTTP verdes y congeló src/main durante envío del contexto. Runner: `node scripts/e2e.mjs e2e/schedule-block.spec.mjs`, sesión23168, proyectoaislado organizationweb-e2e-45580. Freeze liberado tras COPY src/main DONE y transferencia del contexto. Build Docker backend y frontend verde; bundle index-ZfDbvplH.js e index-CfOy6FmQ.css.

Resultado **1/1 PASS, EXIT0**; test3,9s y suite7,3s. La prueba `real creation survives reload and exact replay preserves one block and event` verifica preview sin filas, creación201 con DTO cerrado, Location, key/revisión retenidas, replay200 igual, GETdetalle yby-request, una fila planned_blocks y una outbox BlockPlanned.v1 sin objetivo/key; tarea conserva pending/estimación30. Recarga muestra el bloque persistido. También verifica lista inicial vacía. No se presenta recarga como reinicio de backend ni como cambio de preferencias, ni se atribuye paginación21filas a este smoke.

El runner terminó eliminando sólo su stack/volumen/scratch aislados. No se ejecutó limpieza adicional. El resultado no acredita todavía errores de negocio ampliados, solapes/presupuesto con reservas históricas, carreras ni rollback, que siguen en autoría backend.

## Siguiente caso

Preparada recuperación de respuesta perdida después de POSTreal201: route.fetch permite commit y route.abort simula pérdida del ACK, completar proyecto no desmonta intención, GETby-request confirma el mismo bloque y no se repite POST. Pendiente de ejecutar tras la siguiente coordinación breve de snapshot con backend. No se afirma RED o GREEN antes de esa ejecución.

## Recuperación real después de perder el ACK

Segundo snapshot coordinado sobre las mismas rutas105HTTP estables, con formato revisado. Runner sesión21999, proyectoaislado organizationweb-e2e-10972, salidaefd10d: **2/2 PASS, EXIT0, suite10,1s**. El segundo caso obtuvo201real con route.fetch, abortó sólo la entrega al navegador después del commit y comprobó una fila existente antes de recuperar. La UI mantuvo objetivo bloqueado y consulta disponible al completar el proyecto; GETby-request devolvió el mismo DTO, hubo una sola creación y un solo BlockPlanned.v1. Recarga conservó el bloque sin ofrecer planificación nueva para proyecto terminado. El runner cerró su entorno aislado. No fue un503inventado antes de persistir.

Tercer caso: presupuesto0 con exceso3600segundos, aceptación explícita invalidada al editar y nuevo consentimiento antes de POSTallowOverBudget:true. Capturas de revisión/persistido/recuperado añadidas sólo después de aserciones estables, sin escrituras extra ni perfiles del usuario; directorio propio `.e2e-work/schedule-block-real/<motor>/`,320/1440px. Tercer snapshot126HTTP coordinado, runner61176/proyecto61432, salida0f8f42: **3/3 PASS, EXIT0, suite12,7s**. Consentimiento real confirma una fila con allow_over_budget=true y un evento, sin POST previo al consentimiento renovado. Buildbackendverde y stackaislado cerrado por el runner.

## Selección DST contra API y PostgreSQL reales

Cuarto snapshot coordinado con los dos autores de src/main: 138 HTTP verdes. BlockController SHA256 74CD7E318A83EA29C36FDA8305516DD4289A4E441B50EB14E7EA277BE24093DD. Runner sesión81584, proyecto organizationweb-e2e-69344, salida92404f: **4/4 PASS, EXIT0, suite14,9s**. El caso Madrid 2030-10-27 exige elegir explícitamente ambas ocurrencias; cambiar inicio de +01:00 a +02:00 invalida la revisión y cambia 30 a 90 minutos manteniendo fin +01:00. Creación real conserva Europe/Madrid, duración90 y UTC00:15–01:45; preview no escribe filas. Capturas de los tres estados anteriores regeneradas con metadatos del fixture69344.

Siguiente ejecución: mismos cuatro casos en Firefox y WebKit, con config existente y --grep=schedule_block. Snapshot capturado con freeze de ambos autores; COPY src/main #30 DONE, contexto backend36,60kB, sesión49168/proyecto13460. Este corte ya incorpora carga de reservas del Store; handlers de BUDGET/OVERLAP todavía pendientes según autor. Resultado pendiente: no se atribuye cobertura de esas ramas al humo feliz.

La primera ejecución cruzada seleccionó sólo el smoke por grep heredado del config: **2/2 PASS, EXIT0, 15,3s**, salida8815d4. No fueron ocho casos. Se amplió el grep de cross-browser.config.mjs a schedule_block: y `playwright test ... --list` verificó ocho casos (cuatro por motor) antes de volver a construir. Capturas reales Firefox/WebKit de review y persisted generadas para el fixture13460.

Ejecución cruzada completa sobre snapshot142HTTP + Store ownerBlocks: sesión65685, fixture organizationweb-e2e-11224, COPY #31 DONE, contexto33,99kB, salidae8ae5b. **8/8 PASS, EXIT0, 34,3s** (cuatro recorridos por Firefox/WebKit). Capturas review/persisted/recovered320/1440 regeneradas con metadata11224 en ambos motores; runner cerró únicamente su entorno. Esto acredita creación/replay, ACK perdido después de commit, consentimiento renovado y DST en tres motores; no acredita todavía carreras/rollback de Store.

Preparado quinto caso de integración: otra tarea propia reserva después del preview; el POST debe rechazar BLOCK_OVERLAP sin escribir, conservar borrador editable, permitir consulta explícita del conflicto y exigir revisar un horario adyacente antes de nueva key/creación. Pendiente primera ejecución, sin afirmar RED/GREEN.

Quinto caso y regresión real: **5/5 PASS, EXIT0, 17,1s**, sesión76522/salidasbc4fe1+f553b9, fixture51176; snapshot142HTTP + Store7PG confirmado por autores, COPY#20 y contexto26,14kB. BLOCK_OVERLAP posterior a preview conserva borrador y retira revisión; consulta explícita lee detalle propio de otra tarea/proyecto. Intervalo adyacente revisado permite201 con key nueva; SQL exactamente2bloques/2eventos. No se cambió producción para este test de integración que pasó en primera ejecución.

Sexto caso preparado por separado después del quinto GREEN: consumo real90min entre preview y creación produce BUDGET_EXCEEDED1800s; revisión nueva refleja plannedSeconds5400 y exige consentimiento explícito antes de permitir201. Pendiente primera ejecución.

Sexto caso y regresión: **6/6 PASS, EXIT0, 20,8s**, sesión73584/salida14bc9e, fixture67288. Snapshot147HTTP y Store idéntico al51176 según autores; COPY#31 DONE, contexto34,07kB. La reserva real90min deja revisión antigua sin exceso, pero creación devuelve409 con plannedSeconds5400 y excessSeconds1800. UI retira revisión, preserva edición y exige revisión nueva más consentimiento; creación201 con allowOverBudget:true deja exactamente2bloques y2eventos, sólo uno con permiso. Primera ejecución verde sin cambios de producción. Runner cerró únicamente su stack aislado.

Alcance final de este tramo: seis casos reales Chromium; los cuatro primeros también Firefox/WebKit (8/8). Los dos últimos todavía no se ejecutaron en motores alternativos. No se acredita toda la feature: carreras, fallos de storage, rollback, seguridad HTTP exhaustiva y paginación pertenecen a pruebas backend todavía en curso. No se ejecutó mutación ni se hicieron commits. Vite5174 conservado.

## Reinicio real solicitado por coordinador: @s35

Séptimo test preparado de forma separada: POST201 real con respuesta abortada después de commit; `docker compose restart backend` sólo del proyecto organizationweb-e2e-<pid> validado y con env-file del runner. Compara StartedAt del backend antes/después y conserva StartedAt/montajes de PostgreSQL; readiness acotada a45s. Si la sesión en memoria se pierde, el fixture vuelve a autenticarse explícitamente. La intención sigue abierta en la página y consulta la misma key; verifica DTO cerrado, detalle, lista, una fila y una outbox. Guardará restart-proof.json y capturas sólo después de pasar aserciones. Primera ejecución pendiente, sin afirmar RED/GREEN. Runner scripts/e2e.mjs no requiere cambios.
Snapshot del séptimo caso coordinado con ambos autores: Controller147HTTP más17heredados GREEN reportados; Store/V11 GREEN tras validación de ausencia, nuevas pruebas de estados aún en ejecución sin cambiar producción. Runner12033/fixture66072, COPY#32 DONE y contexto38,47kB; ambos freezes liberados inmediatamente. Resultado pendiente.

@s35 primera ejecución real **7/7 PASS, EXIT0,35,3s**, sesión12033/salidad0ec44. Backend StartedAt08:44:59.815→08:45:33.663 UTC; PostgreSQL08:44:56.091 conservado con volumen66072. La sesión sobrevivió (sin reautenticar), misma key devolvió200/DTO9, detalle/lista consistentes y exactamente una fila/outbox. restart-proof.json y restart-recovered320/1440 guardados en directorio chromium.

Ejecución cruzada completa posterior: **14/14 PASS, EXIT0,1,2min**, sesión7039/salidaa68c90, fixture42372, mismo src/main66072 confirmado por cacheCOPY#15 y contexto16,19kB. Los siete casos pasan en Firefox y WebKit, incluidos solape/presupuesto antes pendientes y reinicio real por cada motor. Backend08:47:13.628→08:47:46.468→08:48:24.014 UTC; PostgreSQL mantuvo08:47:09.913 y volumen42372. JSON restart-proof por motor registra IDs/arranques/montaje, writes1 y recovered200. Runner intacto, sólo restart del servicio backend del stack E2E validado; limpieza propia habitual del runner al finalizar. Ningún servicio ajeno reiniciado.

Estado vigente: siete E2E reales en cada uno de los tres motores. Los pendientes de motores/reinicio descritos arriba son historia superada; permanecen las puertas de revisión/mutación y la autoría backend fuera del alcance de estos recorridos. Capturas reales por motor disponibles; no prueban dispositivos físicos ni evaluación humana.
