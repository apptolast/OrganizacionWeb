# Mutación final — schedule_block frontend

**Estado:** CERRADO. PASS,55/55 =100 %;29/29 IDs objetivo eliminados.

Checkpoint2133120, publicado118c8d. Puerta aprobada por coordinador: init94736 EXIT0/8d8c38, lint,10 pruebas del arnés y1209frontend verdes; backend UP-TO-DATE sobre1365 previos, producción idéntica56ced31. Fuentes, pruebas y configuración congeladas.

Una campaña exacta por arnés: `node .harness/harness.mjs mutate schedule_block-frontend-replay`. Inicio2026-09-06 13:38:18 Europe/Madrid, sesión23912, PID54256, salidab363fc. Configuración final:26 rangos,29 IDs seleccionados; Stryker instrumentó dos fuentes y55 mutantes. Umbral80, perTest, concurrencia2. Los mutantes adicionales de los rangos tendrán denominador propio, sin mezclar campañas.

Informes destino `frontend/reports/mutation-schedule-block/final.json` y `final.html`; original y replay anterior preservados. No retoques, retries manuales, cambios de runtime ni workaround.945 no está en el objetivo y mantiene su limitación histórica separada; cualquier RuntimeError nuevo se registrará con texto exacto, sin asignarle Killed.

Baseline GREEN56ba77: a13:39:17 termina ejecución inicial con547 pruebas en49s (net31,593s, overhead17,558s). Es el baseline instrumentado de esta selección, no la suite global1209. Score pendiente.


## Resultado final

**Veredicto: PASS. Score bruto y Stryker:55/55 =100 % (umbral80).** Cierre2026-09-06 13:41:28 Europe/Madrid, salida79a726 EXIT0, duración3m10s. Dos fuentes: TaskBlocks50 Killed y TaskReader5 Killed. **0 Survived,0 NoCoverage,0 Timeout,0 RuntimeError y0 errores restantes** en esta campaña.

Cotejo4c00f7: las29 identidades del manifest final coinciden unívocamente con29 IDs locales finales, por archivo, ubicación actual exacta, operador, expresión fuente y replacement sensibles a mayúsculas. **29/29 objetivos Killed**, incluido750;26 mutantes adicionales generados por rangos también Killed. Los55 constituyen el denominador propio de esta campaña; no son una nueva campaña global ni se suman directamente a otras puntuaciones.

945 no fue objetivo y su RuntimeError histórico sigue preservado en el informe anterior, con el límite explícito ya revisado. La ausencia de errores en esta campaña no demuestra un kill945 ni repara el adaptador.

Informes finales: `frontend/reports/mutation-schedule-block/final.json` y `final.html`. Mapa íntegro: `progress/mutation_schedule_block_frontend_final_mapping.json`, con29 coincidencias y26 adicionales. SHA-256 final `cd13201a1cf2f52f1127c3869482a1b35879eddcabcd795fe5865c8f4907a3c5`; original `4c95f22fef3d0040e52303c96427351a11fefaa4358f9d8f070944b1ec003aec`; replay anterior `7c868917ac645a03be78893cdf575ea7ff3abaa8f3bd021fa2a3fbbd62b21059`. Los dos anteriores coinciden con sus hashes preservados. Las dos fuentes medidas mantienen los hashes del manifest y coinciden exactamente con las fuentes embebidas en el informe final.

## Identidades objetivo

| ID original | ID final | Resultado |
| --- | --- | --- |
| 746 | 1 | Killed |
| 931 | 18 | Killed |
| 1070 | 22 | Killed |
| 1303 | 47 | Killed |
| 1304 | 48 | Killed |
| 1401 | 50 | Killed |
| 1403 | 52 | Killed |
| 750 | 2 | Killed |
| 757 | 3 | Killed |
| 814 | 4 | Killed |
| 816 | 6 | Killed |
| 829 | 7 | Killed |
| 887 | 9 | Killed |
| 894 | 10 | Killed |
| 897 | 13 | Killed |
| 898 | 14 | Killed |
| 996 | 20 | Killed |
| 1124 | 24 | Killed |
| 1143 | 25 | Killed |
| 1154 | 26 | Killed |
| 1157 | 27 | Killed |
| 1186 | 28 | Killed |
| 1241 | 29 | Killed |
| 1248 | 30 | Killed |
| 1250 | 32 | Killed |
| 1267 | 34 | Killed |
| 1268 | 35 | Killed |
| 1269 | 36 | Killed |
| 1273 | 37 | Killed |

No hay supervivientes nuevos que clasificar. No se editaron fuentes, tests o configuración ni se hicieron retries manuales o workarounds durante o después de medir. El coordinador decide el cierre global con la evidencia de todas las puertas y las limitaciones históricas; este informe sólo acredita la campaña final descrita.
