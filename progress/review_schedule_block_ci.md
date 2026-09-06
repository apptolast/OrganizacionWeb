# Revisión readonly de CI — schedule_block

## Causa confirmada

Run [34024330569](https://github.com/apptolast/OrganizacionWeb/actions/runs/34024330569), push de `3671b943532fb6c8d7e6de91ec29fc00477439ac`, finalizó cancelled. La anotación del check101462497562 confirma literalmente: **“The job has exceeded the maximum execution time of 2h0m0s”**. No es una inferencia por duración ni evidencia de cancelación manual. Consulta gh del run8c29d0 y annotations/log4df008.

Job verify: inicio09:19:36 UTC, fin11:19:52 UTC. `.github/workflows/harness-ci.yml:12` fija timeout-minutes120. El paso `node .harness/harness.mjs verify` empezó09:20:10 y fue cancelado11:19:49. Build, E2E y publisher posteriores quedaron skipped; no deben presentarse como fallidos ni como ejecutados.

## Última etapa y evidencia

El arnés completó init/tests a09:23:02 (96d354). PIT completo terminó09:53:05, BUILD SUCCESSFUL en30m3s:995 mutantes,983 Killed,12 Survived,0 NoCoverage,0 TIMED_OUT y0 RUN_ERROR/MEMORY_ERROR. Este resultado pertenece a aquel commit y su scope completo, no sustituye la medición focal posterior454.

Stryker comenzó09:53:08, instrumentó28 fuentes con4167 mutantes. Baseline09:54:05:1121 tests verdes en55s. La siguiente salida relevante es11:19:49, “The operation was canceled.” El runner retiró el proceso Stryker durante la finalización del job. No hay score frontend final en ese run. La campaña tuvo aproximadamente85m45s después del baseline antes del timeout global.

La causa probada del estado cancelled es el presupuesto temporal del job consumido por la verificación secuencial completa. Los logs no muestran un fallo de aplicación que causara la cancelación. Tampoco permiten afirmar que todos los mutantes frontend fueran resolubles, ni que la campaña fuera a pasar una vez terminada; esa evidencia quedó pendiente.

## Propuesta mínima, sin implementar

Cambiar únicamente `jobs.verify.timeout-minutes: 120` a `240`, manteniendo exactamente todos los pasos, el comando verify sin target y los umbrales. Es una ampliación del presupuesto del job, no de los timeouts de tests/mutantes. Permite que la campaña completa disponga de más tiempo antes de build/E2E/publisher;240 es un margen operativo propuesto, no una duración de campaña ya medida ni garantía de éxito.

`git show 3671b94:scripts/project.mjs` y el script actual confirman que mutate sin target ejecuta PIT y luego `pnpm --dir frontend mutate`. Los targets schedule_block-backend/frontend y el replay actual son ramas explícitas; sustituir verify por uno de ellos recortaría la cobertura global del CI. No se propone hacerlo, omitir gates, bajar umbral, añadir continue-on-error, cambiar concurrency ni modificar el default. No hace falta editar scripts/project.mjs para ampliar el presupuesto.

Si una ejecución con margen suficiente demuestra que el coste sigue siendo excesivo, se podría estudiar separar jobs manteniendo todas las puertas; no está justificada esa reestructuración como primer cambio. La advertencia de Node20 de acciones antiguas es independiente del timeout y queda fuera de esta corrección puntual.

No se editaron workflow, scripts, fuentes, pruebas, dependencias o runtime. No se canceló ni relanzó ningún run; sólo consultas gh y documentación. Pendiente decisión del coordinador sobre el cambio propuesto y evidencia del siguiente CI completo.

## Ajuste autorizado y aplicado

El coordinador aprobó la propuesta. Se cambió únicamente timeout-minutes de120 a240 en .github/workflows/harness-ci.yml. Diff724cd4 confirma una sola sustitución de valor escalar, con indentación YAML y demás pasos intactos; diff --check sin errores. No se añadieron tests espejo ni se ejecutaron suites por este cambio de configuración. No se cancelaron ni relanzaron runs. El nuevo margen todavía debe validarse con un CI completo posterior; no convierte el run cancelado en éxito ni garantiza que240 minutos basten. Listo para revisión y commit conjunto del coordinador.
