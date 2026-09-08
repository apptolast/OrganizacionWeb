# TDD backend import_data23 — A

Contrato42 escenarios aprobado en5feebd7. Baseline install/init23 verde (2846 Java/123,2295 frontend/56,66Node). A posee aplicación/decoder/PG/V21/wiring y pruebas propias; C HTTP aislado, B frontend. No campañas globales ni PIT por ciclo.

## Ciclo1 — puerto de preview (@s1/@s18, nominal parcial)

RED9dfef0: PreviewImportDataTest no compila por cinco tipos ausentes. GREENa065c9: una prueba pura confirma que la identidad/stream llegan una vez al puerto de consulta y que el resultado preparado vacío vuelve intacto sin consumir el stream en la orquestación. El fixture del puerto no es un archivo JSON ni su hash calculado: no acredita parsing, límites, integridad, snapshot PG ni ausencia de escrituras reales. Durante compilación se corrigió el tamaño literal del fixture de21 a20; no se atribuye un RED de negocio a esa corrección.

Tipos reales: ImportDataUseCase.preview(String, InputStream), ImportDataQueries misma firma, PreviewImportData, ImportPreview (incluido RunningSession), ImportCounts cerrado14campos. No métodos apply/receipt futuros ni bean provisional. El caller HTTP cierra su stream; decoder/adaptador A contará bytes y verificará UTF8/duplicados/hash, sin @RequestBody materializado.

El primer comando Spotless767321 devolvió0 pero avisó que el selector requería path absoluto: no seleccionó fuentes y NO acredita formato. Se conserva log. Selector de seis paths exactos en backend/.gradle/import-format.init.gradle, ejecución realfb6f66 aplica/verifica GoogleJavaFormat. No cambios productivos de22.

Logs originales externos en work/deployment-preparation/import23-cycle01-*.log. XML y manifiesto de este corte preservados en progress/import_reading_checkpoint/. Siguiente ciclo: decoder de archivo vacío real, y después fronteras de archivo e integración PG.

## Mapa de alcance

@s1/@s18: orquestación nominal anterior, pendientes persistencia/apply/snapshot. @s2–@s17 y @s19–@s27: pendientes backend. @s28–@s42: fronteras HTTP/proxy/UI y aceptación compartidas según contrato; no se consideran cubiertas por el primer test.

## Ciclos2–4 — decoder nominal y errores reales

Ciclo2 @s1/@s18: REDcf2afd clase ausente; GREEN6cd4e0, exportación vacía real producida por ExportJsonWriter, SHA/tamaño incluyen whitespace final, propietario/fecha exactos y stream del caller no cerrado (override observable). El parser recorre tokens sin árbol global. data aún se salta: NO acredita registros no vacíos ni esquema íntegro.

Ciclo3 @s16/@s17: REDdc7276 por excepción ausente; GREENcc3376 (dos casos). Stream sintético ilimitado de espacios exige ImportTooLargeException y consumo exactamente33554433bytes, sin almacenar ese contenido. El wrapper limita cada read a presupuesto restante más el primer byte de exceso.

Ciclo4 @s8: RED1c45fb por error ausente; GREEN247447 (tres casos). Campo exterior desconocido exige ImportInvalidFileException con mensaje seguro sin payload. Formato real de cuatro paths e6204d. Esas dos excepciones se entregan para HTTP; validación restante, callbacks de filas, PG y beans siguen pendientes. No se presenta este checkpoint como parser completo. Logs externos import23-cycle02/03/04-* preservados.

## Ciclos5–9 — tokens y conteos

5 @s2/@s18: RED3750c0 callback no invocado, GREENe2b77e4casos: primer proyecto entregado registro a registro con texto/version exactos. 6 @s8: RED266df6 duplicado aceptado, GREEN7bb9925casos: STRICT_DUPLICATE_DETECTION y traducción de error Jackson a400seguro. 7 @s8: RED680525 segundo documento aceptado, GREEN597e4f6casos: exige EOF tras raíz. 8 @s8: RED9bce59 counts discordantes aceptados, GREEN42fb8f7casos: compara recuentos observados con declarados. 9 @s8: RED814ece data[] genera error incorrecto/callback, GREENe677bd8casos: exige objeto antes de preparar filas. Aún faltan otras guardas de esquema/tipos y validación durable; no afirmar decoder completo.

## Ciclos10–11 — confirmación/lectura, puertos nominales

10 @s1: REDd78293 tipos ausentes, GREEN2e5418: ApplyImportData entrega owner/key/hash/stream sin transformación y permite al puerto obtener recordedAt desde Clock, truncado a micros, dentro de su operación. Fixture controlado no acredita commit/idempotencia durable; límites de Clock pendientes. Nombre inicialmente incluía@s23, corregido a@s1 porque aún no prueba fallo transaccional.

11 @s22: REDdd1856 tipos ausentes, GREEN177511: consulta de recibo conserva owner/key y resultado cerrado, sin cuerpo/archivo ni comando de importación. Foco conjunto11casos/4suites, formato realafe6de. API real para C: ApplyImportDataUseCase.apply(owner,UUID,expectedSha256,InputStream) devuelve ImportReceipt(requestKey,fileSha256,byteLength,recordedAt,outcome,insertedCounts,identicalCounts); ReadImportReceiptUseCase.find(owner,UUID) devuelve Optional<ImportReceipt>. No beans provisionales. Los errores de conflicto/hash seguirán tras sus ciclos PG/decoder reales. Logs originales externos import23-cycle05–11-*.

## Ciclos12–17 — cierre de EOF y UTF-8

12 @s8: REDdf33ed cuerpo vacío producía NPE; GREEN243b33 con nextRequired en raíz/data/array, sin posibilidad de repetir EOF en un bucle. 13 raíz truncada217600,14data truncada590057 y15registro incompleto en arrayb23d85 fueron inicialmenteGREEN: Jackson ya rechaza los EOF internos y nunca entrega una fila incompleta. No se fabricaron fallos para esos refuerzos solicitados por revisión.

16 @s8: REDba9633 BOM aceptado; GREEN2ce519 al usar InputStreamReader con decoderUTF8 explícito. Fixture del primer proyecto corregido de statuspending a idea (enum real), sin cambiar sus oráculos de texto/UUID/versión. 17 @s8: RED81c374 UTF8 malformado escapaba como error de entrada/salida; GREENa092b1 al traducir CharacterCodingException a ImportInvalidFileException, preservando IO real para503. Continúan pendientes formato/versión y todos los campos tipados del esquema.

## Ciclos18–27 — PostgreSQL y contexto nominal

18 @s1/@s18: RED309e13 store ausente; GREENff6ee5, preview vacío usa snapshotRR real y no crea defaults/negocio/outbox. 19 @s7: RED99dcc1 owner del archivo adoptado; GREEN223436, comparación exacta contra identidad autenticada.

20 @s1: RED6e1bfe puerto de comandos no implementado; GREEN8c1c13. V21 añade sólo import_receipts, sinFK nuevas ni modificación de migraciones previas. Apply prepara el archivo antes de locks, configura READ COMMITTED/lock_timeout2s/statement_timeout10s y adquiere PROJECT/TASK compartidos antes de las15tablas en orden contractual. El callbackClock comprueba pg_locks, parámetros y ausencia de recibo antes de recordedAt. Commit vacío confirmaNO_CHANGE, sólo recibo operativo, sin outbox.

21 @s22: RED42e65a falta lectura real; GREENc684ee, recupera recibo PG completo sin archivo. 22 @s20: REDff4573 falta excepción412; GREENb25da6, hashdiscordante precede a ownerajeno y no consultaClock/crearecibo. 23 @s21: REDc7a1c1 replay vuelve a consultarClock; GREEN1cc388 conserva recibo y xmin/ctid exactos. 24 @s21: RED02e23c falta error dekeyreutilizada; GREEN512087 rechaza otrosbytes válidos con409 y conserva original.

25 @s1/@s18/@s22: REDc424da faltaban beans; GREENec4fa5 en SpringBoot+PG real: exportvacío,preview,apply yfind usan los tres puertos reales. 26 @s23: RED6f6993 errorSQL de trigger escapaba; GREEN63d084 con traducción local DataAccessException/TransactionException a STORAGE_UNAVAILABLE, sin adviceglobal.27 @s23: REDe0e54b triggerRETURNNULL permitía éxito falso; GREENd0f4ba exige exactly1fila afectada para elrecibo y rollback si no.

Límite explícito del checkpoint: sólo vacío entra al store; una fila no vacía aún se rechaza mientras se implementan staging, validación y fusión de14colecciones. No se afirma integridad completa, carreras, límites inclusivos, rowmetadata corrupta ni rechazoCOMMIT diferido todavía. Los beans reales permiten integración/sockets nominales de C, no despliegue23 ni estado done. Formato focalreal0fe993; foco final de seis suites en import23-reading-context-green.log. No Java global/init/PIT.
