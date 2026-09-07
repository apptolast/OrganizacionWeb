# Replay complementario de dos oráculos15

Autorizado por root después de revisión APPROVED de los dos tests (27102e, append en review_pause_resume_frontend.md). No sustituye la campaña861. Ejecutado directamente con el CLI/config local aprobado, sin modificar harness, producción ni config canónica. Ponytail full/Caveman lite.

`pnpm --dir frontend exec stryker run stryker.pause-resume-session.replay.config.json` terminó EXIT0 en1m51s (8bf2b5). Los dos rangos generaron18 mutantes: **17 Killed estrictos,0 Survived,0 NoCoverage,0 Timeout,1 RuntimeError**. Stryker muestra100% al excluir el error; el cociente estricto17/18 es94,44%. No se afirma18 detecciones ni nuevo score global.

Las seis firmas objetivo están presentes una vez y Killed (9b8567):

| ID original | ID replay | Estado |
| --- | --- | --- |
|401|5|Killed|
|409|13|Killed|
|410|14|Killed|
|266|0|Killed|
|267|1|Killed|
|269|3|Killed|

`pause_resume_frontend_replay_mapping.json` conserva esas seis correspondencias por archivo, ubicación completa, mutador y reemplazo. `pause_resume_frontend_replay_inventory.json` conserva los18 y su hash de fuente, incluidos los12 adicionales. Ninguna correspondencia se dedujo del ID regenerado solamente.

## Error adicional conservado

Replay16, original412, OptionalChaining de `problem?.code` a `problem.code` en panel85, termina **RuntimeError**. El runner10.0.0 informa `TypeError: Cannot convert object to primitive value` en `errorToString` de @stryker-mutator/util y VitestTestRunner.run. Stryker intentó reiniciar su worker dos veces sin éxito; no hubo relanzamiento manual ni workaround. Esa entrada no es Killed ni equivalente. No afecta a la presencia/detección de las seis firmas solicitadas y se entrega a root como límite explícito.

## Integridad y artefactos

90 archivos antes/después idénticos durante medición (9b8567), manifests `pause_resume_frontend_replay_before.json`/`after.json`. JSON original861 y su copia raw siguen SHA256 `0F0F92DD7FA76E1548CA024CD4904ED766D91877BC08A79938381E3CF8DBA246`.

Reporte separado `frontend/reports/mutation-pause-resume-session/replay.json`, SHA256 `73D135D7A73D262DD350DB4F615283FC03372B2C81F2E24931EE8F4D79F60F12`; HTML hermano replay.html. Log `progress/pause_resume_frontend_replay.log`, SHA256 `96810A2B73D3A7F5DF0B84BD59E12FBD906FBEFAFDB34B0100354440CB47C723`.

Configuración ejecutada preservada en `progress/pause_resume_frontend_replay_executed_config.json`, hash `CC8C122057636DE169EBF0675348348C9554358E3D647B4B2202DCF4EDB523B8`. Después de guardar snapshotafter/report, se aplicó sólo el formato de configuración solicitado por root: Prettier/check GREENde4a8c; objetos JSON antes/después idénticos. Archivo reproducible final SHA256 `ED1893C46E000E1F405617EA6507FD7D51434C3093DA6DE3025D0BAFCECCC6CF`. No se cambió umbral80,8 workers, perTest, ignorePatterns protegido ni rangos.

No se repitió campaña global ni se persiguieron los demás residuos. Root decide cierre de este complemento y gate global.
