# Backend 24: ciclos TDD

## Corte 1: emisión nominal (@s1, @s5)

`CreateApiCredentialTest.s1_s5_issuesOneSecretAndOnlyVerifierAcrossCommitBoundary`: RED real por los tipos ausentes; GREEN 1/1. Reloj truncado a microsegundos, caducidad de 30 días, una llamada SecureRandom de 32 bytes, formato de token y SHA-256. El puerto de salida recibe el generador diferido; el test comprueba su resultado, no una transacción PostgreSQL. No se acredita todavía persistencia, idempotencia, límites ni autenticación.

Durante el mínimo verde se movió la preparación MessageDigest fuera de la lambda del fixture para evitar una excepción checked ajena al comportamiento. Los logs originales RED/GREEN/formato y XML se preservan externamente en deployment-preparation/integration24-01-*.

Formato SpotlessApply/Check EXIT 0. Sin campaña global ni mutación nueva. Siguientes ciclos: valores y reloj, PostgreSQL real con fixture compartida por JVM y limpieza por propietario; después lectura/revocación/autenticación/cuotas.

## Corte 2: creación durable (ciclos 2–12)

Todos los logs originales de cada ciclo se conservan en `deployment-preparation/integration24-NN-{red,green}.*`; ningún RED se reconstruye. Los ciclos focales desde el 3 usan `--daemon` autorizado por root; no cambia el arnés oficial ni sus flags.

| Ciclo | Escenario y oráculo | RED y mínimo GREEN |
| --- | --- | --- |
| 2 | @s2 intención inválida antes de persistencia, siete ejemplos | Falta excepción propia; nombre, scopes y días validados. |
| 3 | @s3 80 puntos Unicode, White_Space y scopes con orden estable | Orden no canónico; canonicalización y copia de scopes. |
| 4 | @s6 años fuera de rango o caducidad desbordada | Llegaba a generar secreto; reutiliza CustomizationTime y valida caducidad antes de entropía. |
| 5 | @s1 fila PostgreSQL contiene sólo verificador y metadata/intención | Store ausente; V22 y escritura transaccional mínima. |
| 6 | @s9 replay vigente/caducado/revocado sin supplier ni escritura física | Volvía a emitir; retorna metadata durable y secret null. |
| 7 | @s10 colisión owner/nombre/scopes/días conserva fila | Falta excepción de conflicto; comparación antes de generar. |
| 8 | @s7 cupo inclusivo excluye revocadas/caducadas | Falta excepción de límite; cuenta con instante único de creación. |
| 9 | @s8/@s11 dos conexiones, último cupo o misma id | Dos creaciones competían; locks owner y luego id serializan comparación/alta. |
| 10 | @s17 rechazo INSERT, cero filas y fallo deferred COMMIT | Excepción SQL cruda o falsa confirmación; confirma una fila y traduce fallo de TX/JDBC a 503. |
| 11 | @s1 bean real con Spring y PG | Falta bean; wiring mínimo de creación. |
| 12 | @s1 propiedad de metadata/verificador cruzando puerto | Referencias mutables; copias defensivas de lista y byte[]. |

El primer intento GREEN del ciclo 4 falló por recodificación accidental del emoji del fixture al leer texto sin encoding explícito en Windows. Se conserva ese log: no es un defecto productivo ni un nuevo RED contractual. Se restableció el mismo emoji mediante escapes Java y lectura UTF-8 explícita; `integration24-04-green-corrected-fixture` pasó. El GREEN original del ciclo 3 permanece intacto.

Checkpoint conjunto: SpotlessApply/Check y las tres clases propias, 29 tests, cero fallos/errores/omisiones. XML originales copiados externamente antes de sobrescribir build. El fixture PG usa holder estático sin extensión que cierre/reabra el contenedor por instancia; cada caso usa owner/id propios y elimina sus triggers. Esto acredita reutilización por JVM, no evita un nuevo contenedor por proceso minion de PIT.

Límites del corte: emisión y replay/cupo/escritura probados; no acredita aún lectura paginada, revocación, autenticación Bearer, cuotas, corrupción durable, pérdida de respuesta de COMMIT ni migración/rollback/export-import completos. V22 sigue en desarrollo. El proveedor de emisión se invoca después de idempotencia y antes del conteo: permite usar el único createdAt para decidir cupo; ningún secreto se entrega con rechazo. El adaptador nunca persiste el secreto y sólo devuelve éxito tras TransactionTemplate.execute.

## Corte 3: lectura y revocación (ciclos 13–18)

| Ciclo | Escenario | Evidencia focal original |
| --- | --- | --- |
| 13 | @s12 metadata propia, ajena/ausente indistinguibles y sin UPDATE | RED puerto ausente; GREEN lectura propia. |
| 14 | @s13 51 credenciales históricas, empates, dos páginas y exclusión ajena | RED list ausente; GREEN consulta única de hasta 51 filas/página. |
| 15 | @s14 cursor opaco inválido, UUID abreviado y fracción fuera de micros | RED excepciones crudas/aceptación; GREEN error propio por cursor. |
| 16 | @s15/@s16 revocación propia durable, primera fecha con reloj regresivo y replay físico | RED puerto ausente; GREEN con locks compartidos con creación. |
| 17 | @s17 fallos de UPDATE, cero filas y COMMIT | Sólo cero filas produjo RED; los otros dos fueron inicialmente verdes. Se exige confirmación de una fila. |
| 18 | @s12/@s13/@s15 beans reales de lectura/lista/revocación | RED bean ausente; GREEN contexto PostgreSQL real. |

Ajustes de oráculo aprobados por root sobre creación: replay caducado ahora nace con fecha antigua y siete días coherentes, sin UPDATE artificial de expiresAt. La carrera exige haber observado transacciones PostgreSQL bloqueadas antes de liberar la primera. Son refuerzos de fixture inicialmente verdes en el checkpoint conjunto, no RED de producto.

Checkpoint de gestión: SpotlessApply/Check y tres suites propias, 41 tests, cero fallos/errores/omisiones. XML originales externos `integration24-management-checkpoint-xml/`, log/EXIT `integration24-management-checkpoint.*`; ciclos originales 13–18 preservados. Pendientes autenticación, cuota, fronteras de seguridad durables y pérdida incierta de respuesta COMMIT; ninguna campaña general o PIT todavía.
