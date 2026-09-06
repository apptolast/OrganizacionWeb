# Sesión actual

## Feature activa

Feature 11 `schedule_block` in_progress. Features 1–10 terminadas;12–30 pendientes bajo autorización global del usuario. Contrato a84e42f, implementación3671b94 y refuerzos334f47b/56ced31 publicados en origin/main. No es cierre de feature, MVP ni despliegue.

## Verificación vigente

- Init21625 EXIT0/f79afd: lint,1365 backend,1198 frontend y9 tests del arnés verdes. Build391add y E2E7/7 Docker/API/PostgreSQL72ed46 verdes, incluido reinicio real. Invocación E2E anterior falló antes de ejecutar tests por nombre de proyecto inexistente; se corrigió el comando.
- Siete recorridos de bloques pasan en Chromium y14 en Firefox/WebKit. Matriz responsive/zoom/axe y30 principios UX documentados con límites humanos explícitos.
- Regresión E2E global previa57/58: timeout histórico de28 anchos diagnosticado con traza y dividido conservando todos los checks. Grupo afectado31/31 verde; son ejecuciones separadas, no una supuesta corrida86/86.
- Judge completo APPROVED para último replay en progress/judge_schedule_block.md. La única corrección productiva del primer seguimiento fue retirar taskState antiguo durante retry: RED9d5579/GREEN56ed8e.
- CI3671b94 terminó cancelled por timeout confirmado de120min durante Stryker completo, después de PIT verde. Cambio puntual120→240 revisado; no reduce ninguna puerta y espera validación en CI. El success de contrato a84e42f no acredita implementación11.

## Mutación

Backend cerrado: PIT inicial414/454 Killed; segundo453/454 (99,78 %), una equivalencia contextual BlockBudget34, cero NoCoverage/errores/timeouts. Root contrastó454 identidades sin diferencias. No se excluyeron mutantes ni cambió producción backend.

Frontend original:1332 Killed/1561 generados,225 Survived,3 NoCoverage,1 RuntimeError,0 Timeout. Stryker85,38 %. Informe original preservado.

Replay56ced31 cerrado ca2c38:404 generados,362 Killed,41 Survived,1 RuntimeError,0 NoCoverage/Timeout. Bruto89,60 % y Stryker89,83 %. Root contrastó167 correspondencias067ac3;133 seleccionados ahora Killed (130 supervivientes y3 NoCoverage anteriores),33 Survived,1 RuntimeError. La nueva línea TaskReader también Killed. No se mezclan denominadores ni se cuenta945 como kill.

## Trabajo en curso

Seguimiento final:11 pruebas netas y ajustes existentes cubren29 identidades observables.84/84 bloques,55/55 creación de tareas, lint/types focales verdes. Dos revisiones corrigieron aserciones de petición anterior y nodo retirado. Init26470 detectó carrera en test antiguo:1208/1209 frontend; diagnóstico confirmó DOM retirado antes del cleanup pasivo. Sólo se sincronizó la espera404/cancelación, sin cambios de producción. Root94736 repite init; fuentes/tests/config congelados. Soporte final29 identidades/26 rangos APPROVED y10/10 scripts86881a. Reporters nuevos final.json/html conservan informes previos. Pendiente medición independiente final después de init verde; no declarar done antes del dictamen.

## Límites

Ponytail full y Caveman lite activos en todos los agentes. Commits/push autorizados. No resets, automaciones ni despliegue productivo. No promesas sobre fecha o cuota.

Permanecen destinos y acciones previamente bloqueados por revisión automática: no reintentar, leer su contenido ni limpiar ascendientes. El incidente documental previo de aplicación parcial tras rechazo ya fue comunicado. No se afirma limpieza completa de temporales protegidos. El patrón protector de Stryker está conservado y fue comprobado sin leer esa carpeta.

Seguimiento tras replay: cinco tests de secuencias746/931/1070/1303-1304/1401-1403, todosGREENinicial.78/78 bloques750008,eslint3a7fd0,tsc94a1ba. Sólo task-blocks.test.tsx +221líneas, producción intacta. Detalle TDD; pendiente juez, sin nueva mutación. Corte congelado.
