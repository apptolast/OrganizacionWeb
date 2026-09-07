# TDD custom_views_fields — backend21

Ponytail full/Caveman lite. Init21 root94704 EXIT0 ce0f1d heredado del corte
aprobado, sin nuevo init local ficticio. Contrato c91bb8b, 42 escenarios y
174 ejemplos léxicos, no equiparados al conteo de tests. A posee dominio,
aplicación, persistencia, V20 y wiring; C HTTP, B frontend. V14 no se toca.

## Primer bundle de lectura, no cierre funcional

1. @s1 ReadCustomizationTest.s1_readsAbsenceForOnlyTheAuthenticatedOwnerAndRequestedScope:
   RED real 3bb0ff por tipos ausentes (customization_01_red.log), GREEN c9a6ac
   (customization_01_green.log). Puerto devuelve Optional real, sin Clock.
2. @s2 ReadCustomizationTest.s2_readsTheStoredScopeAndDefinitionOrderWithoutChangingItsRevision:
   RED 5d3e0f por modelos ausentes/incompletos, GREEN bc1efe, logs
   customization_02_red/green.log. Campos exigidos por el test; preserva el
   orden de inserción incluso cuando UUID lexicográfico sería distinto.

Formato real spotlessJavaApply y regresión focal 2/2 GREEN62c3f4, log
customization_first_bundle_green.log. Manifest customization_first_bundle.json
contiene siete fuentes y un test. Sólo nominal lectura por puerto; no acredita
PostgreSQL, defaults HTTP, validación completa, comandos, concurrencia ni beans.
Se entrega temprano para que C use tipos reales sin duplicarlos.

Firmas estables: ReadCustomizationUseCase.get(String owner,CustomizationScope
scope) devuelve Optional<Customization>; CustomizationQueries.find igual.
Customization(id,owner,scope,visibleFields,customFields,version,updatedAt);
CustomFieldDefinition(id,label,type,active); Scope PROJECT/TASK y Type
TEXT/NUMBER/DATE/BOOLEAN. Enums y records puros sin HTTP/Jackson/Spring.

Siguiente ciclo: defaults y validación de dominio; lectura PG nominal y bean
real tras adaptador, sin registrar un bean con dependencia ficticia. Guardas,
escrituras y lectura compuesta de valores todavía pendientes explícitamente.

## PG y guardado de presentación en ciclos siguientes

3. @s1 CustomizationPersistenceTest.s1_readsBothAbsentScopesWithoutInsertingConfigurationValuesOrEvents:
   RED867154 (adaptador ausente), GREEN1a4447 con PostgreSQL real/FlywayV20.
   Tres tablas aditivas con FK de valores a proyectos/tareas. Read RR/readOnly;
   no se atribuye aún validación de fila ni guardado PG. Logs customization_03_*.
4. @s2 SaveCustomizationTest.s2_firstViewSaveCreatesOnlyItsScopeAtRevisionZero:
   RED9eae41→GREENe53b43, puerto callback real y microsegundos. Logs04.
5. @s8 stale antes de no-op/reloj: REDa52747→GREENdcd1f8. Logs05.
6. @s8/@s21 no-op versiónMAX conserva objeto y no consultaClock:
   RED6dc63c→GREENeecc1a. Logs06.
7. @s2/@s22 cambio vista conserva defs/id y timestamp monótono:
   REDcaa234→GREEN298581. Logs07.

Primer bundle versionado por root9ef8a75, liberado e integrado C72ff667.
Firmas de comando nominal: SaveCustomizationViewUseCase.save(owner,scope,
CustomizationRevision,List<String>) devuelve Customization. Exception real
CustomizationConflictException para412. Aún no se entrega como comando
completo: validación, reloj/rangos y PGcommands pendientes.

Convención coordinada con C/B: values[i].fieldId / values[i].value siguen
índice ENVIADO; conjunto incompleto usa values INVALID_VALUE. Puerto conservará
List para no perder orden/duplicados. NUMBER se transportará exacto, sin double.

8. @s25 lista duplicada antes de almacenamiento: RED7938af, GREEN85131f (logs08).
9. @s25 campo exclusivo de TASK rechazado para PROJECT: RED3c9522, GREEN0f161d (logs09).
10. @s20 cambio con versión máxima falla sin reloj: RED7cafdd, GREEN80eda0 (logs10).
11. @s20 reloj fuera de años públicos 0001–9999: RED04b70f, GREEN2a347c (logs11, dos ejemplos).
12. @s3 alta nominal de definición con UUID propio, etiqueta normalizada y defaults PROJECT: RED801d01, GREEN7961e4 (logs12). CreateCustomField sigue parcial: no se atribuyen todavía guardas ni persistencia.
13. @s1/@s2 bean real de lectura con Spring y PostgreSQL: RED3db607 por bean ausente (XML a6670f), GREEN41e7ea (logs13). El contexto usa migración real, aislamiento owner/scope y timestamp del año0001. No acredita HTTP ni escrituras.

Checkpoint de lectura: spotlessJavaApply y cinco suites focales (97c022). Log customization_reading_checkpoint_green.log: BUILD SUCCESSFUL en14s; XML preservados en customization_reading_checkpoint_xml suman14/14 sin fallos/errores/skips. La sesión35877 ya estaba consumida al recuperar el contexto; se conserva evidencia del log y XML, sin atribuir un nuevo EXIT de proceso. Manifest customization_reading_checkpoint_manifest.json congela Store/V20/configuración y dos tests PG; las tres firmas de escritura ya fueron integradas por root2797039. Persisten pendientes comandos completos, validación durable y lectura/escritura de valores.

14. @s3 alta posterior conserva identidad/vista/definiciones y añade al final: RED1d5fed (UUID reemplazado), GREEN516c56. Logs14.
15. @s8 alta con revisión obsoleta rechaza antes del reloj: RED8c47b0, GREEN35cb1d. Logs15.
16. @s3 primera definición TASK usa defaults propios: RED68c564. Logs16. El GREEN incluye controlador GET integrado por root y wiring real; no se atribuye PUT HTTP aún.

16. GREEN final34a4d6 EXIT0 para defaults TASK y arranque GET HTTP+Spring/PG.
17. @s4 etiqueta vacía normalizada, NUL, surrogate aislado y61 puntos de código: REDb678c5, GREEN03c03e (logs17). Validador puro CustomFieldLabel reutilizable por HTTP; no validación duplicada.
18. @s4 etiqueta duplicada de definición inactiva se rechaza sin reloj: REDb4acee, GREENdfabf5 (logs18).

Helpers de dominio congelados: CustomizationView y CustomFieldLabel. spotlessJavaApply + SaveCustomizationTest9 y CustomFieldCommandsTest9: EXIT0 04d433, log customization_domain_validators_green.log. Manifiesto de dos hashes customization_domain_validators_manifest.json. Root recibe sólo helpers, no comandos parciales.

19. @s6 doce definiciones inactivas también agotan capacidad: RED743ea8, GREENcc78bb (logs19).
20. @s20 alta no desborda versión máxima y no consulta reloj: RED517a1a, GREEN148009 (logs20).
21. @s22 alta conserva timestamp ante reloj anterior: REDff4824, GREEN53a643 (logs21).
22. @s20 alta rechaza reloj fuera0001–9999: REDc038e5, GREEN4a13f9 (logs22). Tras GREEN se reutiliza captura temporal entre guardar vista y crear campo; no reloj adicional.

Refactor temporal compartido: GREEN3d71d9, logs22_refactor, sin cambio de reglas.
23. @s7 renombrar/desactivar preserva ID/tipo/orden/vista y avanza revisión: RED420980 por tipo ausente, GREEN45d28a (logs23).
24. @s26 definición ausente precede revisión obsoleta y reloj: RED007e87 (logs24). Se reutiliza ResourceNotFoundException existente.

24. GREENe9e766 EXIT0, formato real y16 casos comandos.
25. @s8 update obsoleto no pasa aunque contenido idéntico: RED54964d, GREEN49a3a0 (logs25).
26. @s21 update no-op vigente con versión máxima no consulta reloj: RED8f28c4, GREEN46403a (logs26).
27. @s4 renombrar no reutiliza etiqueta de otra definición inactiva: REDa07c17, GREENe24915 (logs27).
28. @s20 update real en versión máxima falla sin reloj: RED7319d7, GREEN46a977 (logs28).
29. @s22 update con reloj anterior conserva timestamp: REDd869dc, GREEN6a17d1 (logs29).
30. @s2 guardado PG nominal durable desde una instancia nueva y sin evento: REDb1232a por puerto aún no implementado (logs30). Escritura todavía parcial: siguientes ciclos exigirán update/no-op/lock.

30. GREEN527ec6 EXIT0 para guardado durable nominal.
31. @s3/@s7 alta y desactivación actualizan la misma fila durable: REDb68f95 por DuplicateKeyException, GREEN09d905 (logs31).
32. @s21 no-op físico conserva xmin/ctid: REDf0f8eb, GREENcf992c (logs32).
33. @s9 dos primeras escrituras bloqueadas por owner/scope antes del Clock, luego un éxito y un conflicto sin ganador prefijado: RED3dd750, GREENc1c333 (logs33). pg_stat_activity observa dos esperas advisory, no se infiere bloqueo por sleep.
34. @s2/@s3/@s7 tres beans de comandos reales sobre PostgreSQL: RED09562f por bean ausente, GREENefa663 (logs34). Conserva ID/revisión compartida; no MockMvc en este test.

## Checkpoint de escritura y wiring

Congelados once archivos en customization_writing_checkpoint_manifest.json. spotlessJavaApply y seis suites focales: EXIT0 297d1e, log customization_writing_checkpoint_green.log. XML preservados en customization_writing_checkpoint_xml: Read2 + Save9 + Fields21 + PG5 + Wiring2 + HTTP GET7 =46; cero fallos/errores/skips. HTTP GET pertenece a C y se acredita sólo regresión conjunta. Los puertos/helpers ya comprometidos por root no se duplican en este manifiesto.

Alcance honesto: guardado de vista, creación/actualización de definiciones y beans funcionan con PG real; bloqueo inicial/no-op físico tienen evidencia. Pendientes: validación integral de filas corruptas y traducción503 de fallos de escritura, más fronteras de etiquetas/precedencia que aún no tengan oráculo, configuración existente concurrente y todo el subsistema de valores/composite ETag/propiedad. No se atribuye finalización21 ni HTTP PUT antes de integrar paquete C. No se ejecutaron campañas globales ni mutación.

### Corrección de fixture tras revisión root

El checkpoint de wiring tenía un conteo global dependiente del orden de sus dos métodos. Reproducción con orden inverso temporal: REDa88843 (comandos primero, lectura después, count2 contra1). Limpieza de customization_preferences en BeforeEach del PostgreSQL exclusivo: GREEN6433a1 en ese mismo orden. Luego se retiran las anotaciones temporales, se conserva la limpieza y se verifica el corte final. No es RED de producto; no se modifica ningún assert de negocio ni producción. Manifest corregido separado para preservar hashes originales.

35. @s10 lectura de valores nominal mediante puerto real, con owner/proyecto/entidad y ambas revisiones: RED60d6c3, GREEN3930cd. Seis archivos de dominio/aplicación nuevos; sin persistencia o bean de valores todavía.
