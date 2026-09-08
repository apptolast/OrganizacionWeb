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



16 @s8: REDba9633 BOM aceptado; GREEN2ce519 al usar InputStreamReader con decoderUTF8 explícito. Fixture del primer proyecto corregido de statuspending a idea (enum real), sin cambiar sus oráculos de texto/UUID/versión. 17 @s8: RED81c374 UTF8 malformado escapaba como error de entrada/salida; GREENa092b1 al traducir CharacterCodingException a ImportInvalidFileException, preservando IO real para 503. Continúan pendientes formato/versión y todos los campos tipados del esquema.



## Ciclos18–27 — PostgreSQL y contexto nominal



18 @s1/@s18: RED309e13 store ausente; GREENff6ee5, preview vacío usa snapshotRR real y no crea defaults/negocio/outbox. 19 @s7: RED99dcc1 owner del archivo adoptado; GREEN223436, comparación exacta contra identidad autenticada.



20 @s1: RED6e1bfe puerto de comandos no implementado; GREEN8c1c13. V21 añade sólo import_receipts, sinFK nuevas ni modificación de migraciones previas. Apply prepara el archivo antes de locks, configura READ COMMITTED/lock_timeout2s/statement_timeout10s y adquiere PROJECT/TASK compartidos antes de las15tablas en orden contractual. El callbackClock comprueba pg_locks, parámetros y ausencia de recibo antes de recordedAt. Commit vacío confirmaNO_CHANGE, sólo recibo operativo, sin outbox.



21 @s22: RED42e65a falta lectura real; GREENc684ee, recupera recibo PG completo sin archivo. 22 @s20: REDff4573 falta excepción412; GREENb25da6, hashdiscordante precede a ownerajeno y no consultaClock/crearecibo. 23 @s21: REDc7a1c1 replay vuelve a consultarClock; GREEN1cc388 conserva recibo y xmin/ctid exactos. 24 @s21: RED02e23c falta error dekeyreutilizada; GREEN512087 rechaza otrosbytes válidos con409 y conserva original.



25 @s1/@s18/@s22: REDc424da faltaban beans; GREENec4fa5 en SpringBoot+PG real: exportvacío,preview,apply yfind usan los tres puertos reales. 26 @s23: RED6f6993 errorSQL de trigger escapaba; GREEN63d084 con traducción local DataAccessException/TransactionException a STORAGE_UNAVAILABLE, sin adviceglobal.27 @s23: REDe0e54b triggerRETURNNULL permitía éxito falso; GREENd0f4ba exige exactly1fila afectada para elrecibo y rollback si no.



Límite explícito del checkpoint: sólo vacío entra al store; una fila no vacía aún se rechaza mientras se implementan staging, validación y fusión de14colecciones. No se afirma integridad completa, carreras, límites inclusivos, rowmetadata corrupta ni rechazoCOMMIT diferido todavía. Los beans reales permiten integración/sockets nominales de C, no despliegue23 ni estado done. Formato focalreal0fe993; foco final de seis suites en import23-reading-context-green.log. No Java global/init/PIT.



## Ciclos 28–29 — staging y comparación nominal de proyectos



28 @s18: RED 05e173 rechazaba el primer proyecto no vacío; GREEN 584e52. Preview prepara cada fila en una tabla TEMP privada, ligada a la conexión de la transacción RR y eliminada al commit. Cuenta el proyecto ausente como inserción propuesta sin crear el proyecto ni un recibo. Conserva nombre con espacios, Unicode y versión superior al entero seguro de JavaScript. No acredita todavía otras colecciones, validación integral ni escritura no vacía.



29 @s3/@s18: el log original import23-cycle29-red.log conserva BUILD FAILED con una prueba fallida por identicalCounts igual a cero; la sesión terminó antes de recuperar su código numérico y no se inventa esa salida. GREEN EXIT 0 ed1390. Una exportación real de un proyecto existente se reconoce por sus siete campos durables y propietario, con versión textual exacta y fechas comparadas como instantes. Preview devuelve cero inserciones y mantiene xmin, ctid y contenido íntegro. Los conteos de fixtures vacíos se acotaron a su propietario para conservar independencia del orden JUnit al introducir la fila sembrada; no se cambiaron oráculos de negocio. Diferencias, colisiones, resto de colecciones y guardas tipadas siguen pendientes explícitos. Java queda libre para integrar el adaptador HTTP revisado antes del siguiente ciclo.



## Ciclos 30–32 — conflicto, inserción y no-op nominal de proyectos



30 @s4: RED EXIT 1 cc9581 por excepción todavía ausente; GREEN EXIT 0 ee74ac. La vista previa rechaza una diferencia durable con ImportConflictException y conserva físicamente el destino. Sólo esa excepción se comprometió en f114af5 para el handler HTTP de C; el store y la prueba permanecían en desarrollo. La preparación del test coincidió con la verificación HTTP de root, pero llegó después de su compilación: root conservó esa limitación del corte, sin atribuir una congelación íntegra.



31 @s2: RED EXIT 1 6a0e58 rechazaba cualquier fila no vacía. La aplicación pasa staging a su misma conexión transaccional, antes de locks de negocio, e inserta el proyecto ausente con sus datos históricos exactos y el recibo en un commit. El primer intento posterior (853aeb) descubrió un error del nuevo fixture: usaba outbox en vez de la tabla real outbox_events. Se conserva el log fallido; la corrección sólo cambia ese nombre. Clase de persistencia completa GREEN EXIT 0 aebd8d, sin inventar un segundo defecto productivo.



32 @s3: RED EXIT 1 b1bc29 intentaba insertar una identidad existente; GREEN EXIT 0 6a186a. La confirmación reutiliza la comparación de preview y sólo inserta los ausentes. Un proyecto idéntico con versión Long.MAX_VALUE mantiene xmin, ctid y todos sus campos, y crea únicamente un recibo NO_CHANGE con identicalCounts exacto. Formato real de los cuatro paths propios EXIT 0 05452b. La comprobación global de Java y la mutación siguen fuera de este checkpoint parcial.



Pendientes del siguiente tramo: validación cerrada por fila y relaciones de las catorce colecciones, inserción de las trece familias restantes, límites/performance y carreras/commit. Reader y su test pasan a C desde a21dc62: no se modifican aquí. C incorporará hash tras EOF y antes de semántica del envelope mediante overload real; apply mantiene provisionalmente la comparación actual hasta integrar esa API. Nada de este corte constituye una importación v1 completa ni autorización de despliegue.



## Ciclos 33–39 — límites de staging y validación de proyectos



33 @s8: RED EXIT 1 66edfe aceptaba un campo desconocido de proyecto; GREEN EXIT 0 f75b9a. Comprueba forma cerrada en el staging antes de comparar el destino. 34 @s8: RED EXIT 1 607f91 aceptaba version como número JSON; GREEN EXIT 0 5c3231, exige el tipo textual durable.



35 @s20: RED EXIT 1 f9b0b8 confirmó el hallazgo de revisión: JSONB rechazaba una cadena con NUL escapado antes de comprobar un hash discordante. GREEN EXIT 0 55e338 al preparar raw TEXT serializado en la misma tabla temporal y convertir a payload JSONB sólo después del hash. 36 @s8: RED EXIT 1 a7be0b clasificaba el mismo NUL con hash correcto como fallo de almacenamiento; GREEN EXIT 0 d95e9b, pg_input_is_valid(raw,'jsonb') detecta esa representación inválida antes del cast y devuelve IMPORT_INVALID_FILE. No se captura globalmente SQL como error de archivo: los fallos reales de almacenamiento conservan 503.



37 @s8: RED EXIT 1 e67eef demostró una confirmación incorrecta con surrogate escapado no emparejado: el archivo contenía literalmente el escape, pero apply no lanzaba error. GREEN EXIT 0 84c664 mediante serialización interna ASCII escapada; evita que el encoder JDBC sustituya un char UTF-16 suelto antes de PostgreSQL. Los bytes originales siguen siendo los usados por SHA y tamaño; no se modifica el archivo ni se normalizan cadenas de negocio.



38 @s8: RED EXIT 1 a0090a aceptaba versión negativa; GREEN EXIT 0 5b4b9e, forma decimal canónica no negativa y representabilidad BIGINT. 39 @s8: RED EXIT 1 11ab7d aceptaba estado no permitido; GREEN EXIT 0 56d110. ImportRecordValidator reutiliza Project y sus reglas existentes, descarta el objeto normalizado y conserva los campos originales en staging; lee una fila cada vez. Resto de identidades/fechas y familias siguen en desarrollo. Los logs originales import23-cycle33–39-* se conservan externamente, incluidos los rechazos reales antes de cada corrección.



## Ciclos 40–42 — tareas nominales y límite raw revisado



40 @s2: RED EXIT 1 4ddaff por tareas ausentes en la exportación posterior; GREEN EXIT 0 993313. Inserta tareas y subtareas con una sentencia ligada a la transacción después de proyectos. El archivo presenta la hija antes que el padre; ambas referencias se conservan, así como campos nulos, texto con espacios/Unicode, versión larga y estado de completado. Exportar de nuevo produce las catorce colecciones exactamente iguales al archivo original; el recibo no aparece en esos arrays y no se crea outbox.



41 @s3: RED EXIT 1 b7cfd2 reinsertaba la tarea existente; GREEN EXIT 0 184298. Comparación tipada de todos sus campos, incluido null y versión Long.MAX_VALUE, produce NO_CHANGE sin tocar xmin/ctid/contenido. 42 @s8: RED EXIT 1 65d751 reproduce el hallazgo de revisión de longitud: 120 letras rodeadas por espacios pasaban el constructor normalizador. GREEN EXIT 0 48193a exige el límite sobre la cadena original antes de SQL, manteniendo el constructor compartido intacto. Formato focal real de cinco paths EXIT 0 466287.



Este corte añade únicamente la incorporación nominal de proyectos/tareas. Siguen pendientes las otras doce familias, integridad interna completa, claves alternativas, restricciones del conjunto, batching acotado y pruebas de concurrencia/commit. La preparación actual por fila no acredita rendimiento de 100.000 registros; esa limitación se cerrará mediante el oráculo inclusivo real sin elevar el timeout del proxy.



## Ciclos 43–50 — relaciones de tareas, historial y disponibilidad



43 @s10: RED EXIT 1 2fa17d → GREEN EXIT 0 48c38f exige que el proyecto de una tarea figure en el archivo; no toma una referencia del destino. 44 @s10: RED e80aa0 → GREEN 06f441 rechaza un ciclo entre padres mediante alcanzabilidad desde raíces del propio staging.



45 @s2: RED 60ee08 → GREEN 96739b conserva el historial de estados. 46 @s3 amplía ese mismo recorrido con otra clave y filas físicas: RED 6f8725 → GREEN 7e94b6 confirma NO_CHANGE y xmin/ctid intactos. 47 @s2: RED 1ff6de → GREEN 3a09f5 conserva disponibilidad. 48 @s3: RED f73c15 → GREEN e027af incorpora su repetición idéntica sin escritura física.



49 @s9: RED 7438d8 → GREEN faba3f acepta estimatedMinutes=1.0 como entero matemático exacto. 50 @s9: RED edca41 → GREEN 8e0752 rechaza 1.25 antes de SQL. El lector local del staging conserva BigDecimal y el validador de tarea reutiliza el tipo durable sin normalizar el texto que se inserta. Estos tags de tipos corresponden a @s9; las referencias históricas a @s8 en ciclos anteriores se conservan como bitácora, no redefinen el contrato.



## Ciclo 51 — recorrido integral de las catorce colecciones (en curso)



Root autorizó un único recorrido integral para las diez familias restantes. El fixture usa tipos durables existentes para ambos recibos y exportación real; conserva una fila propia ajena al archivo y comprueba outbox sin cambios. Dos intentos iniciales fueron errores del fixture: 20e75f no compilaba por una sobrecarga de assertThat; 0b3815 comparaba nodos numéricos Jackson de distinta clase pese a igual valor. Ambos logs permanecen; no se atribuyen a defectos productivos. El oráculo se corrigió a ImportCounts tipado.



RED real EXIT 1 bb8331: faltaban diez arrays en la exportación posterior. Tras añadir ocho escrituras SQL de familias no recibo, EXIT 1 06a3d9 mantiene el ciclo abierto. Comparación independiente del XML fe0a49 confirma doce arrays idénticos y únicamente blockChanges/workSessionChanges con una fila esperada y cero real. Se espera el decoder real de C para esas dos familias, sin stubs ni segunda implementación.



El lector revisado de C (adb2dd3) quedó integrado durante una ventana sin Java. Apply usa ahora read(input, consumer, expectedSha256); preview conserva la sobrecarga sin hash declarado. Se retiró la comparación redundante posterior de apply. Los archivos del lector y sus pruebas pertenecen a C y no se editaron. No hay GREEN integral ni acreditación de repetición completa, límites masivos o integridad restante todavía.



51 cierre: decoder C revisado b50ede1 integrado sin modificar sus fuentes. GREEN EXIT 0 cb2202 conserva las catorce colecciones y los dos recibos completos. El staging mantiene payload v1 y durable_receipt por separado; SessionStart procede de la sesión del propio archivo. Las escrituras de recibos se confirman en la misma transacción.



52 @s3 amplía el recorrido integral con otra clave y compara xmin/ctid/contenido de las catorce tablas antes y después. RED EXIT 1 474a8d intentaba insertar identidades existentes. GREEN EXIT 0 e98a01 añade comparación tipada explícita y omite todas las filas idénticas: NO_CHANGE, cero insertedCounts y identicalCounts completos. No se genera un evento de negocio. Los logs originales son import23-cycle51-receipts.log e import23-cycle52-{red,green}.log externos. La igualdad de recibos con representaciones históricas equivalentes se aborda inmediatamente después; no se presupone a partir de esta repetición de datos recién importados.



53 @s3: la vista previa del archivo recién exportado contra las filas históricas originales dio RED EXIT 1 6a97a5: los recibos durables tenían fechas ISO equivalentes con representación distinta. GREEN EXIT 0 d49aee. La comparación reutiliza ExportReceiptWriter sobre el recibo existente y guarda su representación v1 en staging, preservando el JSONB durable original. Se comparan campos cerrados y números mediante JSONB, sin normalizar datos de negocio ni exportar un documento completo adicional. Formato focal y clase ImportPersistenceTest completa EXIT 0 174b06 (log import23-fourteen-checkpoint.log).



Corte nominal integral: catorce colecciones, repetición física sin escrituras de negocio y representación histórica equivalente acreditadas. Continúan pendientes las guardas agrupadas de forma/tipos/relaciones del conjunto, claves alternativas, cuotas/sesión abierta, corrupción de recibo operativo, concurrencia/commit y batching/límites inclusivos. No se atribuye a este corte la validación completa de importación ni rendimiento masivo.



## Ciclos 54–60 — guardas agrupadas de integridad



54 @s9: ocho variantes de forma cerrada, tipos sin coerción, versión no negativa y precisión representable dieron RED EXIT 1 958499. El primer intento de implementación 3efc35 aún despachaba todas las filas al validador antiguo de tarea; se conserva como intento fallido, no GREEN. Despacho común corregido: ocho variantes e integral GREEN EXIT 0 9f5152. Los metadatos exteriores se validan en A; el contenido de preferencias se delega al validador propio de C cuando se integre.



55 @s10: referencias faltantes en nueve familias, RED EXIT 1 340bb7; GREEN EXIT 0 1a3193 junto al integral. Cada referencia resuelve exclusivamente en staging. Algunas variantes ya eran rechazadas por el contexto de recibos; el grupo conserva esos casos inicialmente cubiertos sin atribuirles un defecto nuevo.



56 @s9: disponibilidad, duración de bloque, minutos de sesión y estimación de tarea con representación decimal integral fueron initial GREEN EXIT 0 26a30d. No se cambió producción por la sospecha de cast: el reader revisado prepara los enteros matemáticos exactos antes de SQL. El log se llama import23-cycle56-red.log por la intención de ejecución, pero el resultado real es GREEN y se registra así.



57 @s10: seis variantes de proyecto discordante dieron RED EXIT 1 607d29; GREEN EXIT 0 bb14a6 junto al integral. El fixture de padre se precisó para seleccionar la tarea original por identidad, evitando que el orden aleatorio de UUID escogiera la segunda tarea y confundiera cruce de proyecto con autociclo. Los joins verifican proyecto compartido y el contexto tarea/bloque.



58 @s11: ocho variantes de hechos históricos (duración, revisión de historia/proyección/intervalo, acumulado, presupuesto, estado y texto) dieron RED EXIT 1 237594. GREEN EXIT 0 a59ac0 junto al integral. Se reutilizan Availability y BlockRequest sin persistir sus normalizaciones; no se consulta TZDB. Los intervalos se contrastan con la sesión y su acumulado exacto, y las revisiones de historia/proyección con sus hechos correspondientes.



59 @s10: identidades durables duplicadas en las catorce colecciones, aun con contenido idéntico, dieron RED EXIT 1 109a05; GREEN EXIT 0 e24c89 junto al integral. Identidad de intervalo es sessionId/revision y de proyección blockId. No se deduplican silenciosamente entradas.



60 @s12: diez variantes de identidad nueva con clave alternativa ya ocupada dieron RED EXIT 1 b06aff; GREEN EXIT 0 fc096e junto al integral. Comparación explícita cubre recursos por propietario/ámbito/entidad, claves de petición, tarea/versión y bloque/versión. El fixture de sesión usa historia cerrada legada para aislar la clave de petición de la restricción de sesión abierta. La variante de bloque omite proyección/cambios para que otra identidad dependiente no oculte el oráculo de clave alternativa.



Los logs originales de estos grupos permanecen en deployment-preparation/import23-cycle54–60-*. No se ejecutó init ni mutación. El freeze nominal anterior describía la bitácora working-tree con EOL mixto (SHA420274…DE70), preservada íntegra en import23-fourteen-tdd-original.md; el commit normalizó su blob LF (SHA1F7FFF…BDD2). Son representaciones distintas documentadas, no una huella atribuida al blob equivocado.



61 @s9/@s10: preferencias inválidas y valores cuyas definiciones faltan sólo en el archivo dieron RED EXIT 1 b35ce2. GREEN EXIT 0 6e20fb junto al integral integra los tres métodos reales de ImportCustomizationValidator de C. El join de definiciones usa exclusivamente customization del staging con el ámbito correspondiente; ausencia equivale a lista vacía, sin defaults ni consulta del esquema del destino.



Trazabilidad precisada: las claves alternativas del ciclo60 corresponden literalmente a @s6 (no @s12); el grupo de integridad histórica del ciclo58 corresponde a @s10, complementando el positivo de recibos @s11. Los nombres de los dos métodos se corrigieron antes de la regresión del corte; los logs originales conservan sus nombres anteriores. No se cambió ningún oráculo por esta corrección documental. Formato focal real y clase completa de persistencia EXIT 0 ea82a1, log import23-integrity-checkpoint.log. Sigue siendo corte parcial: alternativas internas, unión de cuotas/sesiones, recibo durable corrupto, concurrencia y límites/performance se cierran después.



### Ciclos 62–70: unión e integridad durable



Evidencia original en `work/deployment-preparation/import23-cycleNN-*.log`. No se ejecutaron suites globales ni mutación.



- 62: diez claves alternativas duplicadas dentro del archivo, RED dbcf30 → GREEN bfbd54.

- 63 (@s14): avisos sólo para sesiones RUNNING ausentes, incluidos runningSince null legado; RED fcef72 → GREEN 99eec0.

- 64–65 (@s15): cuota configurada de proyectos activos y unión con sesión abierta existente; RED 199fe0/7a6044 → GREEN 9d6a30/fe0bba. Wiring real incluido en64.

- 66 (@s10): cinco restricciones durables de tarea, historial, expansión y proyección parcial; RED8f2919 → GREEN6c0faf. Se conserva el CHECK real completed/non-null/updatedAt y los nulls permitidos de expansión/proyección.

- 67 (@s10): seis discrepancias entre snapshot y fila actual; RED2bd45e → GREENb03bc2. El intento de edición d145a0 no encontró el patrón: no cambió producción y la ejecución siguiente repitió el RED (a9490a). Su archivo llamado green se conserva como intento fallido, no como evidencia GREEN. La confirmación final está en cycle67-green-final.log.

- 68 (@s22): siete corrupciones de recibo operativo (counts incompletos, string, fracción, negativo, suma, outcome y fecha fuera del rango); RED314a6a → GREEN2dbb4d. Lectura devuelve503 sin modificar la fila, el nominal integral sigue verde.

- 69 (@s23): refuerzo inicialmente GREEN28f297. Antes del fallo de INSERT del recibo, el Clock observa las14 colecciones insertadas en la misma transacción. Tras503 se comparan snapshots físicos de todas, outbox y ausencia de recibo. No se atribuye defecto nuevo ni se duplica COMMIT diferido de C.

- 70: coherencia de intervalo completo de proyección con último recibo, manteniendo la cancelación con intervalo y los offsets resueltos históricos; ciclo en curso.



Ciclo70 confirmado: REDd0f138 → GREEN29f9dd,8 casos incluyendo integral y seis discrepancias anteriores. XML original preservado fuera del build en deployment-preparation/import23-cycle70.xml. Comparación sin TZDB ni normalización: request locales/zona y time offsets/instantes/duración; proyección legada null usa los hechos originales.



Foco completo posterior62–70: EXIT0 32c804, Spotless seleccionado y 131 pruebas PG/wiring, sin fallos/errores/omisiones. XML originales externos import23-integrity-final-*.xml. No acredita escala pendiente.


### Ciclos 71–73: escala por proxy y extensión histórica

71: ensayo real de 100000 proyectos idea y 33554432 bytes. Original 52599 EXIT1 (dd35c2): timeout del cliente a 45 segundos durante preview, sin respuesta HTTP 504 y sin alcanzar apply. XML original `deployment-preparation/import23-cycle71-scale-original.xml`. ImportScaleTest pasó a B tras ese EXIT; amplió los oráculos de hash, byteLength, recuentos completos y recibo.

La corrección mantiene una conexión transaccional: staging unnest parametrizado en lotes de 128 filas o 65536 bytes ASCII, salvo una fila individual mayor; verifica filas afectadas. Índices de texto después de validar JSONB, sin cast UUID temprano, y ANALYZE. Fetch de validación acotado según mayor fila más definiciones. Mismo recorrido GREEN d65f1d: preview 200 en 4752 ms y apply 200 en 5136 ms por Nginx con 15 segundos intactos; hash, 32 MiB, 100000 registros y recibo verificados. XML original `import23-cycle71-scale-green.xml`. No acredita todas las distribuciones masivas.

72: EXTEND mediante escritor real. Primer intento 721b3e falló por ObjectMapper del fixture con fechas numéricas, antes del oráculo. Configurado como escritor real (fechas textuales), RED cd00c9 confirma 409 incorrecto en lugar de 400 ante fin o última decisión discordantes. Primer GREEN 90bb3f no compiló por key duplicada en test de escala ajeno al cambio; B lo corrigió. GREEN final ef2f0d, 6 casos, comparando último EXTEND y última decisión no-null; preserva fallback plan y nulls legados. Copias exactas externas `import23-extend-freeze.json`, incluidos Java y XML. C verificó y aprobó la guarda.

73: cadena profunda de 60000 tareas de B, inicialmente GREEN 4c7564. Archivo de 21529756 bytes, 1 proyecto y 60000 tareas; preview 200 en 4506 ms y apply 200 en 5083 ms. Se comprobaron hash, mapas, enlaces y limpieza propia. XML original `import23-cycle73-deep-green.xml`. No fue necesario otro cambio productivo para ese caso.

Se corrigió únicamente un byte CP1252 aislado (guion U+2013) de una anotación nueva para mantener UTF-8 válido. Copia previa exacta: `deployment-preparation/import23-tdd-before-encoding-fix.md`, SHA95635da06cd5d9cfe40b728f635a96a42c0305037ec48cb30afd70eae9d85af2. Los manifiestos anteriores y sus originales externos no se modificaron.

### Ciclos 74–75: últimas fronteras durables

74: cuatro casos de recibo histórico existente corrupto (bloques y sesiones, forma inválida o tamaño superior al presupuesto 22). RED 81816e → GREEN 18d5bd. La consulta previa limita receipt y textos exteriores antes de materializarlos, con el presupuesto de exportación 22; las excepciones de validación durable se traducen a 503, sin confundirlas con archivo inválido 400. El fixture conserva digest, xmin y ctid durante la consulta y restaura después únicamente su recibo para no dejar decenas de MiB en otras pruebas.

75: un identificador inválido de 36 KiB incomprimibles causaba 503 al construir B-tree. RED 8b40ee → GREEN 8e6d3f. Se valida primero cada fila y el contenido acotado de configuración; después se crean los mismos índices textuales y se validan relaciones. Los valores personales se contrastan con definiciones del archivo después de las guardas de identidad. No se cambian límites ni reglas de error.

Foco completo de persistencia y wiring, con Spotless seleccionado, en curso en `deployment-preparation/import23-backend-final-focal.log`; aún no se atribuye resultado.

### Corte final de implementación backend

Formato seleccionado y foco completo: EXIT0 10c0ac, 137 pruebas de persistencia y 1 de wiring, cero fallos/errores/omisiones. Después, la ejecución masiva ya iniciada al llegar la instrucción de evitar una pasada separada terminó EXIT0 faaf32; no se canceló ni se repetirá por cuenta de A. Dos casos sobre fuente final: cadena de 60000 tareas (preview 4701 ms, apply 5582 ms) y 100000 proyectos/32 MiB (preview 4641 ms, apply 5022 ms). Todos HTTP 200 por Nginx con 15 segundos intactos. B formateó el test y confirmó tokens/literales idénticos; hash 8445326b5e7f3bc21beec132f5f0b44b79c0b98ea4dde8e65c560281c9173eea.

Los tres XML originales se conservan en deployment-preparation/import23-backend-final-*.xml. El freeze final relaciona 140 pruebas focales, sin atribuirles la regresión general, CI, PIT ni nuevos casos concurrentes que C todavía prepara. Los originales RED/GREEN y los cortes anteriores siguen preservados. A deja las fuentes quietas para revisión/gates coordinados por root.
