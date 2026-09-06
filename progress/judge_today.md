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
