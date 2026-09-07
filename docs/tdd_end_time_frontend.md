# TDD frontend17 — aviso y ampliación

Contrato aprobado44 escenarios/132 ejemplos, SHA6BC581725DC0FE4C7B548191A842882CE5A62FD0348CF789D1BCCD843ABE4309. Autorización root42cda11; no equivale a132 pruebas ejecutadas. AGENTS/rol craftsman/TDD y Ponytail full/Caveman lite releídos; init16 vigente por indicación root, sin repetir globales. Root posee current/metadata/Git; este autor sólo frontend y esta bitácora.

## Primer corte cliente

Nuevos work-session-end-api.ts y su test; work-session-state-api.ts compartido exporta el mismo isState sin relajar reglas y admite EXTEND discriminado. Reutiliza apiRequest, exact/sameId, microseconds, transporte POST/C/K, Location y validación histórica14–16. Sin interfaz, hook, SCSS, dependencia ni configuración nueva todavía.

Cada fila siguiente se añadió y ejecutó antes de la siguiente. Un caso por ciclo, no matriz parametrizada anticipada.

| Ciclo | Escenario/oráculo | RED real | GREEN y mínimo |
| --- | --- | --- | --- |
| 1 | @s13 GETend nominal con token/señal | 7db05a import inexistente | fc6aac, transporte y retorno |
| 2 | @s26 campo adicional en E | b48aed | dc7b90, exact3 |
| 3 | @s26 status desconocido | 95224d | 806757, reutilización isState |
| 4 | @s26 cabecera revisión incoherente | 6cd227 | 8133b6, token canónico |
| 5 | @s26 fin un microsegundo anterior al original | 7c3403 | f41650, comparación BigInt |
| 6 | @s26 E de otra identidad | 092200 | e7d0f7, sameId |
| 7 | @s16 HTTP503 conservado | c4b14e | 669966, status antes de JSON |
| 8 | @s26 serverNow anterior al changedAt expuesto | 81630a | 7d2b27, reloj exacto válido |
| 9 | @s1 POSTEXTEND nominal sin alterar intervalo | 6692e4 | a30cf3, unión/body/acción específicos |
| 10 | @s26 EXTEND sin extension por C | 361165 | d38d83, P/R6 discriminados explícitamente |
| 11 | @s26 fórmula desviada un microsegundo | 9eb075 | 3e0303, fórmula exacta max+minutos |
| 12 | @s26 cantidad cadena numérica | 684794 | 73a32f, tipo number |
| 13 | @s26 cantidad0 con fórmula concordante | 6c1be0 | ca4ec9, mínimo1 |
| 14 | @s26 cantidad1441 con fórmula concordante | abc91a | 686666, máximo1440 |
| 15 | @s26 cantidad fraccionaria | 9ed615, RangeError en lugar del error controlado | 59bcfa, Number.isInteger antes de BigInt |
| 16 | @s26 after running internamente válido que altera intervalo/trabajo | bb7933 | 1593d1, campos de State6 conservados |
| 17 | @s26 occurredAt anterior al estado | 58c347 | ef9201, guarda temporal específica |
| 18 | @s26 previousEnd anterior al original con fórmula concordante | 4c38e6 | 1a7e00, proyección aditiva |
| 19 | @s26 K de otra cantidad con misma sesión/revisión | d27da9 | 45d955, comparación de intención en helper compartido |
| 20 | @s27 K paused tardío,1440min, sin Location | inicialmente GREEN75748c | Sin producción para este caso; reutilización nominal K y rama max(occurredAt) |

La ejecución75748c pasó20 tests pero tsc detectó acceso a extension unknown. Refactor verde: validExtension recibe el fin original ya validado dentro de la rama EXTEND; evita acceso sin tipo y conserva las guardas. Formato y regresión111/111 de tres archivos API14/15/16/17 más tsc GREENc958aa. Se renombró sameNotes a sameDetails porque ahora compara cantidad de EXTEND además de notasCLOSE; no cambió su comportamiento.

Freeze final: formato/ESLint/tsc b0e77a, sesión91759 terminó EXIT0 5d7230 con111/111 en tres archivos.20 son nuevos y91 heredados; no se suman ejecuciones de ciclos. Diffcheck focal ebfc1d sin errores. Ninguna suite UI/global/E2E/mutación ejecutada.

| Archivo congelado | SHA256 |
| --- | --- |
| frontend/src/work-session-end-api.ts | A7BD65AA29827DE31BE60286C528E55710ACA53F2A968C4B11F6454D267CFCE6 |
| frontend/src/work-session-end-api.test.ts | 66F7CB55454808D471B8B8CDC439637FCCF732372FCC868C5CD61FA46E78F3FD |
| frontend/src/work-session-state-api.ts | 929352B95B730B05D62F6C578541AC61D4F98C3A84B75CEC8F2C2F0B5B6CFAB8 |

## Alcance y siguiente tramo

Este corte acredita conexión y diferencias cliente E/EXTEND, no toda la feature ni todos los ejemplos de@s26. Reutiliza validación de identidad, revisión BIGINT, SessionStart, problemas y Location ya cubierta por las suites anteriores. No valida una marca temporal interna ausente del DTO. La prueba paused tardía usa fechas2026: no se presenta como nuevo oráculo de época superior a Number seguro; precisión general procede también del microseconds compartido probado14–16.

Pendientes tras revisión de este corte: coordinación local de decisiones/generaciones, panel17/timers y montaje en ambos recorridos, privacidad/recuperación/foco y eventual ampliación acotada de oráculos del cliente demandada por esos flujos. La cobertura propia@s28–44, navegadorUX y gates integrados todavía no está implementada ni certificada. Se congela cliente compilable para primer review por root antes de escribir mucha UI.
