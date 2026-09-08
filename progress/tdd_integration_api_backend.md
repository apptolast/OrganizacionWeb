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

## Corte 4: autenticación y admisión nominal (ciclos 19–26)

| Ciclo | Comportamiento | Resultado original |
| --- | --- | --- |
| 19 | @s13 listado ante conexión PostgreSQL rechazada | RED CannotGetJdbcConnectionException; GREEN traducción StorageUnavailableException. Hallazgo root. |
| 20 | @s20 token válido autentica owner/scopes, sin escritura física | RED puertos ausentes; GREEN contra PG y SHA-256. |
| 21 | @s19 token canónico antes de almacenamiento: prefijo, UUID, base64/padding/bits, tamaño y separador | Ocho RED; GREEN parser acotado a 84 caracteres. |
| 22 | @s19/@s21/@s31 desconocido, secreto incorrecto, revocado, owner deshabilitado/renombrado, instante antes/exacto/después de expiry | Ocho casos inicialmente GREEN. No cambio productivo ni RED inventado. |
| 23 | @s26 última solicitud inclusiva, contadores 59/119 pasan a 60/120 | RED puertos ausentes; GREEN transacción y dos contadores compactos. |
| 24 | @s26 rechazo por cualquiera de los dos límites conserva ambos | RED excepción ausente; GREEN comprobación antes de escribir y Retry-After 40 con fracción de segundo. |
| 25 | @s21/@s30/@s31 admisión revalida revocación/expiry/owner bajo locks | Cuatro RED por ausencia de revalidación; GREEN antes de consultar/incrementar cuotas. |
| 26 | @s20/@s26 beans reales, owner bootstrap habilitado y rechazo de owner huérfano | RED beans ausentes; GREEN contexto Spring/PG. |

Checkpoint: seis suites propias, 67 tests, cero fallos/errores/omisiones, SpotlessApply/Check verdes. Originales `integration24-bearer-checkpoint-xml/` y log/EXIT homónimos; ciclos 19–26 sin sobrescribir. Parser y SQL no reciben el header Authorization completo: HTTP extrae el token y A protege su formato. SHA-256 compara mediante MessageDigest.isEqual, no igualdad textual ni secreto persistido.

Límites pendientes: concurrencia de cuotas/revocación, ventana siguiente, rollback de contadores y conservación operacional V22, validación durable acotada y pérdida incierta de COMMIT. Este corte habilita HTTP aislado; no declara backend final ni ejecución de PIT. V22 agrega ahora dos tablas de contadores compactos y sigue en desarrollo hasta su revisión final.


## Cierre de oráculos y durabilidad (27–39)

Los originales de cada ejecución permanecen fuera del repositorio en `../deployment-preparation/integration24-NN-*.log` y `.exit`. No se reconstruye ningún RED posterior.

| Ciclo | Comportamiento | Evidencia |
|---|---|---|
| 27 | Dos slices heredados de configuración requieren identidad explícita | Fallo original reproducido; dependencia UserDetailsService y app.auth.username añadida únicamente a fixtures. GREEN 31 tests, mismos oráculos. |
| 28 | Ventanas UTC siguientes y Retry-After inclusivo de un segundo | Dos ventanas y contadores compactos, inicialmente GREEN. |
| 29 | Fallo SQL, cero filas o COMMIT diferido en cualquiera de los dos contadores | Dos RED de seis variantes por falsa confirmación de cero filas; ambos UPSERT ahora exigen exactamente una fila. Rollback físico de ambos contadores. |
| 30 | Dos réplicas compiten por última cuota de credencial o propietario | Inicialmente GREEN, bloqueo PostgreSQL observado y exigido antes de liberar. Sólo una admisión y sin contador parcial. |
| 31 | Revocación antes/después de admisión concurrente | Inicialmente GREEN en ambos órdenes, bloqueo real exigido y autenticación posterior rechazada. |
| 32 | Bootstrap realmente deshabilitado mediante UserDetailsService configurado | Inicialmente GREEN: autenticar y consumir rechazan, cuotas intactas. Usuario original restaurado. |
| 33 | Restricciones durables de verificador, scopes y cuotas | Cuatro RED; CHECKs simples de V22, sin trasladar validación exhaustiva de importación. |
| 34 | Migración aditiva 21→22 y exclusión export/import | Inicialmente GREEN: base propia, hechos previos y export iguales, import NO_CHANGE no modifica credenciales/cuotas. No acredita rollback de imágenes desplegadas. |
| 35 | Nombre con surrogate UTF-16 aislado | RED alcanzaba el puerto de commit; GREEN rechazo de name antes del puerto. No se afirma reproducción de sustitución física JDBC. |
| 36 | COMMIT real confirmado pero respuesta de conexión perdida | Dos casos inicialmente GREEN: creación/revocación devuelven almacenamiento incierto, lectura/replay recupera estado durable sin nuevo secreto ni fecha. |
| 37 | Expiración exacta 7/30/90 días y reloj único | Tres casos inicialmente GREEN, truncado a microsegundos. |
| 38 | Replay con capacidad completa en estado vigente/caducado/revocado | Tres casos inicialmente GREEN; fechas caducadas coherentes y lectura propia conserva metadata. PG fijado a postgres:17.9-alpine existente en el proyecto. |
| 39 | Nombre Unicode de 80 puntos suplementarios y nombres duplicados | Inicialmente GREEN contra PG; conserva ambos nombres exactos e IDs diferentes, retira White_Space lateral. |

Precisión Unicode: un surrogate aislado no representa un punto Unicode escalar y puede perder fidelidad al codificar a UTF-8. La guarda comparte el criterio de CustomFieldLabel; acepta pares válidos fuera de BMP. Los espacios laterales Unicode se retiran antes de validar controles, conforme al contrato ratificado.

La fixture PG es singleton por JVM: no arranca contenedor por instancia de test. Cada caso usa identidades propias; los triggers se retiran en finally. El caso de compatibilidad crea y retira una base propia dentro del mismo contenedor. Esto no promete reutilización entre JVM diferentes de PIT.

Los scopes Gradle integration_api e integration_api_http abarcan clases completas, incluidos records con validación y configuración completa. Candidatos: todos los JUnit; controles DEFAULT, -FRECORD, umbral 80 y cuatro workers permanecen iguales. No se ha ejecutado mutación de 24.


## Compatibilidad de negocio integrada y recursos PIT (40–41)

Dos casos nuevos disjuntos en ApiCredentialBusinessCompatibilityTest, sin modificar producto ni los tests de C. Reutilizan su Database.PG singleton y FixedClock; propietario sintético business-owner independiente.

40/@s25: inicialmente GREEN. PUT Bearer sin Origin/CSRF con If-Match vigente conserva normalización, timestamps, ETag versión 1 y exactamente un ProjectUpdated.v1 con sus siete campos. Ambos contadores valen uno.

41/@s25/@s29: inicialmente GREEN. Conflicto de versión 412, precondición ausente 428 y recurso ajeno 404 conservan filas físicas y no añaden eventos; cada solicitud admitida incrementa ambos contadores exactamente una vez. Los contratos heredados no se duplican para las dieciocho rutas.

Originales integration24-40-initial.log/exit e integration24-41-initial.log/exit. La pasada conjunta integration24-business-final.log/exit acredita ambos casos y formato. No se atribuye un RED de producto a casos ya correctos.

Root autorizó ocho workers exclusivamente para los scopes integration_api e integration_api_http. Medición previa aportada por root: 24 cores, 25 % CPU, 24.26 GiB Windows libres; Docker 31.04 GiB disponibles y aproximadamente 0.8 GiB usados. Se conserva el mismo universo, todos los JUnit, mutadores, filtros, timeout y umbral 80; los demás scopes conservan cuatro workers. No se promete aceleración sin medir una campaña completa. No se ejecutó PIT en este corte.
