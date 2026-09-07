# TDD del soporte Stryker18

7 de septiembre de2026. Alcance aprobado por root2100a6, corregido a ternarios App completos conforme parser5d668f:25:12–31:22 y38:10–62:7. Propuesta/nodos actualizados antes de aplicar; sin código de producto ni Gradle.

1. Target history-frontend: test de despacho RED76b12f por Invalid target; mínimo whitelist y rama runner exclusivo → GREENdbc382. Logs history_harness_01_red/green.log.
2. Uso de ese target con test en vez de mutate: inicialmente GREEN826394, ninguna llamada al runner. history_harness_02_initial.log. No se inventa cambio productivo.
3. Configuración dedicada cerrada en el test: RED826394 por archivo ausente → configuración heredada con ocho selecciones, todos tests/perTest/8/80, ignore intacto y salidas próprias → GREEN83fe19. history_harness_03_red/green.log.
4. Default no omitirá18: RED83fe19 por módulos/rangos ausentes → adición de dos módulos, ternarios App completos y enlaces Workspace/proyecto. TaskReader ya completo; parámetros generales conservados. Primera regresión47:46GREEN/1FAIL b9eca3, independiente del selector nuevo.

El fallo restante era el oráculo histórico14: contrastaba líneas124/135 de TaskReader actual, desplazadas por el enlace18. Root aprobó preservarlo como snapshot. Extracción de bytes exactos `git show 46913a71c1a582357fa8d50053766a50a5793299:frontend/src/task-reader.tsx`:5993bytes, SHA0CC6ED956F083FE7262F512DA2830F11FB46596191DDD4E6C2DA5945E187402A, idéntico a mutation_start_work_frontend_before.json, final_before y freeze14 (c31fdf). Archivo documental start_work_frontend_historical_snapshot.json, base64 sin reconstruir líneas. Se cambió sólo la lectura de ese oráculo, conservando todas las aserciones previas y añadiendo integridad del snapshot. Config14/informe14/producto intactos. Ese scope es histórico; no acredita integración18. El default actual incluye TaskReader completo.

Regresión final scripts/project.test.mjs:47/47 EXIT0 c2e1b5, history_harness_final_corrected_tests.log. Prettier check final después de formato focal, sin formato global. No instrumentación ni campaña, no mutantes ni score inferidos. Root hará init/build y autorizará campaña después de revisión de cuatro archivos activos (scripts/project.mjs/.test.mjs, frontend/stryker.history.config.json y stryker.config.json) y snapshot documental. Build.gradle.kts no se ha editado.
