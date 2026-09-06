# Cliente de inicio de trabajo14

Rol tdd_craftsman; Ponytail full y Caveman lite. Contrato aprobado por root
en c54aee6. Baseline común91757f reutilizado por coordinación; no nuevo init,
mutación ni suite global. Propiedad limitada a work-session-api.ts, su test y
esta bitácora; sin UI, shared api-client, backend ni metadata.

## Ciclo1 — POST nominal @s1

Un único test nuevo: `sends an explicit start with its key and current CSRF token`.
RED efbe11 EXIT1 por importación del módulo aún inexistente, sin tests ejecutados.
Mínimo: POST mediante apiRequest, contexto de ruta, cuerpo plannedMinutes,
key, credentials/cache/signal y reutilización del token CSRF vigente.
GREEN a32511 EXIT0,1/1 en3,43s. Comando en frontend:
`pnpm exec vitest run src/work-session-api.test.ts`.

Primer corte revisable deliberadamente parcial: sólo transporte nominal.
Todavía faltan estado HTTP, DTO cerrado/relación exacta de microsegundos,
Location/contexto, replay y las tres lecturas. No se presenta como cliente
seguro completo ni como cobertura de toda @s1. Se añadirán casos de uno en uno.

## Corte2 — POST validado y activa, congelado para revisión

Los ciclos siguientes añadieron un test por vez. Cada GREEN ejecutó únicamente
el archivo propio completo; no se añadieron matrices anticipadas.

| Ciclo | Caso y tag | RED | GREEN | Cambio mínimo |
| --- | --- | --- | --- | --- |
| 2 | HTTP503 preservado, @s32 | f7e6bc | 7f307d,2 | Rechazar estado distinto de201 |
| 3 | Replay200, @s14 | b195d9 | 53e3ae,3 | Admitir200 además de201 |
| 4 | Campo extra, @s30 | 9c0d01 | 889e28,4 | Reutilizar exact con los siete campos |
| 5 | Duración string, @s30 | 3851f9 | 8633fa,5 | Comparar duración con intención |
| 6 | Fin desviado un microsegundo, @s30 | 221c2d | 12caf0,6 | Relación exacta BigInt y léxico instant heredado |
| 7 | Proyecto distinto, @s30 | a55fa7 | f34f29,7 | Reutilizar sameId para proyecto |
| 8 | Tarea distinta, @s30 | 447bf4 | 51a2e1,8 | Reutilizar sameId para tarea |
| 9 | Location de otro recibo, @s30 | 0f46ca | eac528,9 | Comparar ruta Location con id |
| 10 | UUID inválida con Location coincidente, @s30 | 8e8a53 | 68c351,10 | Validar identidad con uuid heredado |
| 11 | Zona en blanco, @s30 | a03ded | 4c6fee,11 | Validar texto histórico, sin catálogo |
| 12 | Cero coherente con intención/instantes, @s30 | 69b2b9 | ba850d,12 | Reutilizar integer1–1440 antes de BigInt |
| 13 | Activa de otra tarea propia, @s23 | 274d0d | e2dcc6,13 | GET global con señal/cache/credentials |
| 14 | Ausencia confirmada null, @s23 | inicialmente GREEN | 08e6a7,14 | Sin cambio de producción |
| 15 | Error503 de activa, @s24 | 4fa8b1 | 555a2e,15 | Exigir200 antes del cuerpo |
| 16 | Envoltorio activo con extra, @s42 | 34e75d | 2e77a5,16 | Forma cerrada con session |
| 17 | Activa con fin incompatible, @s42 | 293a5a | 4fa28d,17 | Aplicar validador compartido a session no null |

Refactor GREEN entre16 y17: extraer el validador SessionStart ya exigido por
POST, separar contexto conocido/Location y publicar el tipo de retorno de siete
campos. Formato1cae92 y regresión16/16 GREEN56e7c9. El cliente active reutiliza
ese validador sin imponer el proyecto/tarea de la pantalla, ni consultar reloj
actual o catálogo. Las lecturas porID/key todavía no se han implementado.

Precisión temporal: instant heredado valida forma/calendario/rango y hasta seis
decimales. Date.parse se aplica a la parte de segundos enteros, luego BigInt
convierte a microsegundos y suma la fracción original rellenada a seis dígitos.
La comparación no pierde decimales submilisegundo. No se ha cambiado ningún
validador compartido. Pruebas de todas las variantes léxicas, límites altos,
duraciones distintas, IDs de ruta con mayúsculas, abortos y recuperación ID/key
quedan para el siguiente tramo; no se atribuye su ejecución a estos17 casos.

Validación final del corte: formato1336b5, ESLint focal f84e71 EXIT0,
TypeScript42b264 EXIT0 y17/17 Vitest2cefd8 EXIT0 en2,75s. Renombrado local de
la variable de milisegundos en refactor para que indique su unidad real.
Fuentes y test congelados después de estos resultados. Sin UI, suites globales,
mutación, backend, metadata ni commits por este autor.

SHA256 del freeze2ef490:
- work-session-api.ts: BD7AE7AA7D266A43DAD2D16B955AACF89DB6A0036135862B0E0162A4F6ED8F05
- work-session-api.test.ts: 29773D38B9AB3B43DCE876A23C02A9204C1C7F0DFD7054477DFDA25507D04CE6
