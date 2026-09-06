# Revisión del límite temporal de CI

Cambio acotado autorizado por el coordinador: timeout-minutes pasa de 60 a 120 en el único job de Application CI. Se conservan comandos, controles, alcance de mutación, umbral 80 y versiones. Ponytail full y Caveman lite activos. No se añade una prueba que reproduzca el literal del workflow.

La ejecución [34010190766](https://github.com/apptolast/OrganizacionWeb/actions/runs/34010190766), sobre e1afc11359a27464b353db031e692a1d7fd4624d, terminó cancelled. El [job 101424530420](https://github.com/apptolast/OrganizacionWeb/actions/runs/34010190766/job/101424530420) comenzó a las 03:55:13 UTC y terminó a las 04:55:31 UTC del 6 de septiembre de 2026. Su anotación oficial dice: `The job has exceeded the maximum execution time of 1h0m0s`. Esto identifica un timeout; no hay evidencia de cancelación manual externa.

| Fase | Evidencia de la ejecución |
| --- | --- |
| Lint y pruebas normales | Completados correctamente antes de las 03:58:17 UTC. |
| PIT global | Completado a las 04:14:16 UTC; 951 segundos, 547 mutantes generados y 543 KILLED; cero NO_COVERAGE. |
| Stryker global | Iniciado a las 04:14:17 UTC; 2202 mutantes en 23 archivos y dos workers. Dry-run de 647 pruebas verde a las 04:14:56 UTC. |
| Cancelación | A las 04:55:26 UTC, tras unos 41 minutos de Stryker sin resultado final. |
| Build, E2E y publisher | Skipped por cancelación del job; no ejecutados ni aprobados en esta CI. |

Los datos se obtuvieron mediante gh run view --json/--log y la API de anotaciones del check-run 101424530420. El log no permite estimar cuánto faltaba para terminar Stryker. El techo de 120 minutos es provisional y finito: aumenta el margen sin garantizar éxito ni presentar una ejecución incompleta como verde. Debe revisarse el tiempo real de la siguiente ejecución antes de volver a ampliarlo.

Una separación futura de PIT y Stryker en jobs independientes podría reducir el camino crítico, manteniendo build/integración sujetos a ambos resultados. No forma parte de este cambio. Tampoco se cambia concurrencia, se omiten mutantes ni se ajustan timeouts de tests para obtener un score.

Verificación local documental: diff limitado a 60 → 120 en el workflow y este informe; git diff --check sin errores de formato. No se ha ejecutado otra CI ni realizado commit/push desde este agente.
