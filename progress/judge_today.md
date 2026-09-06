# Revisión coordinada de Hoy

Contrato a127747, 38 escenarios y 105 casos; autorización global del usuario vigente. Ponytail full y Caveman lite. Feature12 continúa in_progress.

## Corte de integración

Init35422 EXIT0 (292711): 1317 pruebas frontend en 23 archivos, 13 pruebas del arnés y lint completos verdes. Backend ejecutado de nuevo, no UP-TO-DATE: lectura XML cfbe76 confirma 1415 pruebas, cero fallos, errores u omitidas. Este corte precede a la corrección adicional de retorno visible con petición pendiente; no acredita todavía esa corrección.

Revisión independiente backend APPROVED en review_today_backend.md: contrato @s1–15, transacción read-only REPEATABLE_READ, aislamiento, reloj único y traducción de fallos. Revisión frontend en review_today_frontend.md reabierta por un caso real: ocultar y volver con GET pendiente cancela el temporizador de retirada a medianoche. El autor debe entregar RED/GREEN y nueva revisión antes de cerrar o mutar frontend.

## Soporte de mutación

Revisados diff de Gradle adce7b y configuraciones frontend853939/84efaa. El scope backend incluye núcleo nuevo, adaptadores y wiring, con informe separado pitest-today. El scope frontend incluye Today/API completos y las regiones compartidas modificadas de App, Workspace, ProjectReader y use-session. CreateProjectScreen permanece intacta; schedule-block-api sólo exporta validadores existentes. El scope por defecto también añade las regiones nuevas. Se conservan umbral80, perTest, concurrency2 y el ignorePatterns protegido. Trece pruebas del arnés verifican selección y manifest. El hash de Today deberá actualizarse después de su último arreglo.

El backend puede pasar a medición independiente una vez liberados los procesos Gradle. Frontend espera la corrección y su dictamen. No hay resultados de mutación de Today todavía.

## Interfaz real y límites

E2E vacío UTC 1/1 y agenda persistida 1/1 (71817d) ejecutados contra API y PostgreSQL reales. Revisadas por root las capturas de .e2e-work/today-real/9324/today-agenda-320.png y today-agenda-1440.png: navegación de tres secciones, presupuesto y bloque legibles, título largo y texto HTML literal contenidos, sin recorte visible. El foco de entrada está visible. Son dos anchos del recorrido con datos, no una certificación universal ni la matriz completa de treinta principios. Resto E2E/UX y regresiones de rutas pendientes del autor de integración.

Dictamen global: pendiente. No se declara done, MVP completo, despliegue ni éxito de CI remoto.

## Corte final de fuentes para medición

Último defecto resuelto por ciclos33/34 del autor y revisión independiente APPROVED4ffcf5. Root releyó código y oráculos b687e8/8c4dc7: volver visible mantiene el deadline del día mientras espera snapshot, sin duplicar GET ni reactivar fronteras de bloques antiguas. El día vencido se retira y la generación anterior se aborta.

Regresión root posterior al arreglo:1319/1319 frontend,23 archivos, EXIT0/4aecda; lint completo EXIT0/9698e6. Build y tipos del autor7cf936. Backend permanece idéntico al init1415 verde y checkpoint5cc80a9; no se repite Gradle mientras PIT lo utiliza. Configs/scripts congelados,13 pruebas38e58c y hash Today0bcc57577073014f76eee41110121b03413e9731a72544d13d0cf1432a9ef789. Se autoriza medición frontend independiente sobre este corte; conserva pendiente el cierre E2E/UX global y la propia mutación.

## Mutación backend

Campaña independiente sobre5cc80a9: arnés EXIT0/56f8fa,80/80 Killed, score bruto100%, cero supervivientes, sin cobertura, timeouts o errores. Root verificó el XML1b93c3; siete clases incluyendo wiring y Agenda. Informe y hashes en progress/mutation_today_backend.md. Backend supera la puerta. Frontend y UX permanecen pendientes; no se extrapola este100% al proyecto.

## Corrección de foco y corte de integración posterior

Stryker original finalizó EXIT0/e5ccd3:418/521 Killed,102 Survived y1 NoCoverage;80,23%, sin errores ni timeouts. Root verificó cifras y SHA2565cc335b97919aaa1bcd3cf4cf956af54ee55db3a02fdb23a060c77508f5c47a3 en7fa9c0. El inventario original y copia inmutable quedan conservados. El umbral pasa; el seguimiento de103 identidades continúa y no se declara cierre.

El navegador encontró un defecto fuera de los mutantes generados: disabled de Actualizar retiraba el foco. El autor cambió sólo ese atributo a aria-disabled, conservando el guard de petición. RED realcc49ad y test semántico3aac94,111 pruebas focales y build/lint9f4c89; revisión independiente APPROVEDd2979d. Root revisó el diff740211 y la captura final de error18396: el foco permanece visible, el aviso y la agenda fechada se conservan sin recorte.

E2E final sobre ese código: Chromium4/4 (6b8014), Firefox4/4 (862700), WebKit4/4 (d7349f). Cada motor completa155 combinaciones de31 anchos por cinco estados, cinco axe, texto200% y teclado/foco; feedback medido2,9/6/4ms con respuesta retenida. Históricos19/19c437d1 verifican las rutas migradas. Zoom nativo200%1/1c492dc conserva geometría del mismo recorrido; root inspeccionó el PNG44224 con agenda larga. No se extrapola a dispositivos físicos, lector real o facilidad humana. Matriz30 y fallos previos conservados en tdd_today_e2e.md.

Init15059 EXIT0/aeb03d posterior al arreglo:1320 frontend en23 archivos,13 pruebas del arnés y lint verdes; backend UP-TO-DATE sobre1415 pruebas sin cambios. Sólo resta en esta puerta resolver o justificar los103 resultados de mutación y verificar su seguimiento; no inventar un100% global ni marcar done por superar80%.

## Selección de seguimiento

Root revisó los argumentos independientes dbdbf5/6b537e y acepta las37 equivalencias propuestas en review_today_mutation_candidates.md, conservando sus límites: seis redundancias de validación/normalización, dos condiciones bajo reservas válidas sin solape, siete redundancias del ciclo de vida/estado, cuatro variantes de espaciado legibles y18 variantes del punto decorativo cuya sección activa sigue indicada por aria-current, borde y fondo. No son todas equivalencias universales ni visualmente idénticas. El informe bruto original no se modifica ni se recalcula descontándolas.

Se admite además justificar209 y215 como restricciones más fuertes que sólo rechazan empates de inicio, imposibles en reservas positivas válidas sin solape. El informe independiente debe incorporarlas individualmente antes de fijar la selección. Los cambios que pueden aceptar un empate descendente manipulado siguen sujetos a prueba negativa; no se inventa una agenda válida con solapes para matar restricciones más fuertes.399/412 permanecen observables por truncamiento del delay fraccionario y se refuerzan con un caso de plataforma.

Selección prevista:64 identidades originales pendientes tras39 justificaciones, más la región del botón modificada después del informe original. Falta aprobar los refuerzos y configurar una campaña separada con correspondencia exacta de identidades; esta decisión no atribuye kills ni autoriza todavía ejecutar el replay.

Corrección previa a configurar:219 también endurece únicamente el rechazo de empates. Con UUID hex validado, upper(rhs)<=lower(rhs), por lo que previousLower>=rhsUpper nunca acepta un descendente que la expresión original rechazaba. Se acepta su equivalencia contextual bajo ausencia de empates válidos;218 modifica el operando contrario y sigue observable. Selección definitiva prevista:63 originales,40 justificados, más la región nueva del botón. Se conserva la decisión anterior como historia, sin una campaña ejecutada con ella.

## Refuerzos revisados

API:17 casos nuevos en seis comportamientos,88/88 verdes b85355, tipos/lint288a83. Revisión independiente APPROVEDed390b en review_today_mutation_candidates.md; producción API intacta. UI/App/Workspace:16 casos nuevos y una aserción añadida a carga inicial,56/56 verdes98844f, tipos/lintd7d862; revisión independiente APPROVEDf59032 en review_today_ui_mutation.md. El juez detectó y el autor cubrió ausencia de alerta mientras la primera petición sigue pendiente, antes del reset de error por éxito. No se atribuyen kills a pruebas ejecutadas sólo sobre el programa original.

E2E/UX cuenta además con dictamen independiente final2a09f8 en review_today_e2e.md, separado de la autoría de producción. No hay nuevos cambios productivos después del arreglo de foco y no se repiten esos recorridos por añadir únicamente tests. Restan verificación del soporte de replay, regresión final y medición independiente de las63 identidades.

Soporte de replay revisado eca77d/d21b03: target fijo today-frontend-replay, informe separado, umbral80/perTest/concurrency2 e ignore protegido intactos. Root contrastó directamente las63 identidades con el informe original8877d8 (IDs únicos, estados, operadores, reemplazos, ubicaciones y hash);40 justificaciones conservadas. Los15 tests del arnés verifican además fuente exacta, posiciones convertidas y hashes. Init14639 EXIT0/a6e120:1353 frontend en23 archivos,15 pruebas del arnés y lint verdes; backend sin cambios UP-TO-DATE sobre1415. Se autoriza la campaña independiente de seguimiento, conservando fuentes/tests/config congelados hasta su resultado.
