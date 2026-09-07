# TDD del soporte Stryker18

7 de septiembre de2026. Alcance aprobado por root2100a6, corregido a ternarios App completos conforme parser5d668f:25:12–31:22 y38:10–62:7. Propuesta/nodos actualizados antes de aplicar; sin código de producto ni Gradle.

1. Target history-frontend: test de despacho RED76b12f por Invalid target; mínimo whitelist y rama runner exclusivo → GREENdbc382. Logs history_harness_01_red/green.log.
2. Uso de ese target con test en vez de mutate: inicialmente GREEN826394, ninguna llamada al runner. history_harness_02_initial.log. No se inventa cambio productivo.
3. Configuración dedicada cerrada en el test: RED826394 por archivo ausente → configuración heredada con ocho selecciones, todos tests/perTest/8/80, ignore intacto y salidas próprias → GREEN83fe19. history_harness_03_red/green.log.
4. Default no omitirá18: RED83fe19 por módulos/rangos ausentes → adición de dos módulos, ternarios App completos y enlaces Workspace/proyecto. TaskReader ya completo; parámetros generales conservados. Primera regresión47:46GREEN/1FAIL b9eca3, independiente del selector nuevo.

El fallo restante era el oráculo histórico14: contrastaba líneas124/135 de TaskReader actual, desplazadas por el enlace18. Root aprobó preservarlo como snapshot. Extracción de bytes exactos `git show 46913a71c1a582357fa8d50053766a50a5793299:frontend/src/task-reader.tsx`:5993bytes, SHA0CC6ED956F083FE7262F512DA2830F11FB46596191DDD4E6C2DA5945E187402A, idéntico a mutation_start_work_frontend_before.json, final_before y freeze14 (c31fdf). Archivo documental start_work_frontend_historical_snapshot.json, base64 sin reconstruir líneas. Se cambió sólo la lectura de ese oráculo, conservando todas las aserciones previas y añadiendo integridad del snapshot. Config14/informe14/producto intactos. Ese scope es histórico; no acredita integración18. El default actual incluye TaskReader completo.

Regresión final scripts/project.test.mjs:47/47 EXIT0 c2e1b5, history_harness_final_corrected_tests.log. Prettier check final después de formato focal, sin formato global. No instrumentación ni campaña, no mutantes ni score inferidos. Root hará init/build y autorizará campaña después de revisión de cuatro archivos activos (scripts/project.mjs/.test.mjs, frontend/stryker.history.config.json y stryker.config.json) y snapshot documental. Build.gradle.kts no se ha editado.

## Campaña original autorizada

Root autorizó después de init2211Java/1887frontend, repetición frontend1887/40 y lint tras foco, build y CSS físico integrados. Inicio efectivo2026-09-07T10:29:30.9333466Z (12:29 Europe/Madrid), target único history-frontend, sesión56475/PID46068. Before136entradas SHA291E334CF22C2F261A5AC1C92E7A9F3E9EAEFF4A5A4A6C4FB0C596CB342CAE66; incluye src entero, archivos de configuración frontend, lockfiles y dispatcher. Parser actualizado SHA674B4C71ECAE8E8F64DA28FCE859660E0D409291722DD7BDB2AF31D94F14C948: History539B…D8500 y rangos externos intactos.

Log history_stryker_original.log: instrumentadas seis fuentes con741mutantes; ocho runners, dry-run perTest iniciado. No se atribuye aún score ni resultado a esos mutantes. Sin modificaciones de fuente/tests/config durante la campaña; PIT de A y UX aislado de C son independientes. No otro Gradle por B.

## Refuerzos posteriores a la campaña original — sólo tests

Root autorizó un máximo de12oráculos, sin producción. Cada uno se añadió y ejecutó antes del siguiente; todos fueron inicialmente GREEN. Logs history_refinement_01–12_initial.log:

| Ciclo | Oráculo | Evidencia |
|---|---|---|
|1|entryextra con detalles válidos|0bb5e5|
|2|TaskStatus inválido con id/time coherentes|bc7287|
|3|Block original duración incoherente|8f9978|
|4|CANCELLED con after no nulo|04181f|
|5|PAUSE con un microsegundo de trabajo indebido|a9bd0c|
|6|TaskStatus válido rotulado BLOCK_CHANGED|fae6de|
|7|Block válido rotulado SESSION_STARTED|57f2e9|
|8|BlockChange válido rotulado TASK_STATUS_CHANGED|7b6f79|
|9|SessionChange válido rotulado TASK_STATUS_CHANGED|35be58|
|10|error de rango, otra URL y Back sin revivir error|48015d|
|11|segundo503 y segundo reintento GET|74ca16|
|12|CLOSE válido con ambas notas vacías|93f104|

Los casos6–9 conservan DTO interno válido, ID/contexto/tiempo coherentes con ese DTO; sólo la familia exterior es incompatible. No se añadieron matrices de validadores heredados. El ciclo10 conserva la URL aplicada válida y prueba vuelta después de un borrador inválido, sin simular retiro de componente.

Regresión API62+UI33=95/95 en dos suites, EXIT0 7c9055. Primer Prettier check tras write avisó history-api.test.ts (93f104); segunda escritura y check pasaron, log history_refinement_format_final.log. ESLint focal y tsc sin emisión EXIT0 db2e1f. Ninguna fuente productiva ni configuración cambiada por B; informe original no modificado ni estados reclasificados.

Propuesta de replay, no configuración: history_frontend_replay_proposal.json.17firmas originales verificadas contra nodosAST exactos, nueve rangos; fuente textual igual al raw original, herramienta a716b8. Incluye76/77/124/125/136/137/138/150/151/166/167/168/298/302/547/692/696. Puede generar extras de esos nodos: deberán conservarse y mapearse por firma, no por ID nuevo. No errores438/442/443 ni otros diferidos dentro del objetivo declarado. No se ha instrumentado ni ejecutado replay.

## Replay dirigido terminado

Configuración exclusiva autorizada tras revisión de12tests y CSS8092443. Sesión38642, before137entradas662249…EB600; formato inicial de JSON requerido y check finalGREEN antes de iniciar. EXIT0 b57d3d,33mutantes32K/1S/0otros,1min42. Mapping f11ea2 acredita17/17objetivosKilled y15extrasKilled; únicoextra548Survived por contador decreciente equivalente en este uso. After137idénticosb6c3e0. Informe review_history_frontend_replay.md y mapping preservados; original84,15%/3errores no reclasificados. No másrefuerzos/campañas.

## Regresión frontend global final

Sobre paquete cebebeb y CSS8092443, una única ejecución posterior a los12refuerzos. Sesión4154, EXIT0 b45d0c: **1899/1899 pruebas en40suites**,23,64s; ESLint y Prettier globalfrontend EXIT0; tsc/Vite build EXIT0,67módulos,447ms de Vite. Logs history_frontend_final_gate_tests.log, _lint.log y _build.log con sus EXIT_CODE explícitos.

Before/after137entradas de fuentes, tests y configuración idénticas,0diferencias. No arreglos durante el pase, ninguna otra campaña ni Gradle. Manifiestos history_frontend_final_gate_before.json y history_frontend_final_gate_after.json. Esta ejecución reemplaza el conteo1887 anterior para el gate frontend final, sin cambiar el score de las campañas originales ni atribuirse los gates backend/E2E de root/A/C. B queda libre y no amplía trabajo.
