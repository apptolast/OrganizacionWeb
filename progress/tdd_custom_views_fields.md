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

Fixture final sin orden forzado: EXIT0 e02de9; único hash cambiado del checkpoint es WiringTest. Root integró escritura4e3a8a8 y PUT6e128b3; archivos liberados.
36. @s11 NUMBER decimal exacto1.0 canónico Integer1: REDb4187c, GREENefeca6 (logs36).
37. @s12 NUMBER fuera de rango/fracción/magnitud grande devuelve error con índice original: RED3c14ac, GREEN1fb5e3 (logs37).
38. @s11 null de los cuatro tipos conserva ausencia: RED83dce6, GREEN1f2d78 (logs38).
39. @s11 BOOLEAN false/true conserva valor exacto: RED0bf712, GREEN43fcc6 (logs39).
40. @s11 TEXT conserva espacios/saltos/HTML y sólo cadena vacía pasa a null: RED80b312, GREEN08ff4b (logs40).
41. @s11 DATE válida conserva fecha sin zona: RED0b5103, GREENb0562e (logs41).
42. @s12 DATE inválida/reformateada/fuera de calendario devuelve INVALID_VALUE por índice: RED0e748a, GREEN09b574 (logs42).

43. @s12 tipos JSON incompatibles devuelven INVALID_TYPE por índice: RED5c40b3, GREENa175f2 (logs43).
44. @s12 texto largo/NUL/surrogate inválido: REDed0766, GREEN1fc1ca (logs44).

Pausa de nueva producción por fallo de CI JDBC original34160728290. Reparación mecánica de24fixtures autorizada por root; evidencia separada fix_customization_v20_jdbc_cleanup.md. No se descarta trabajo de valores ni se atribuye completitud. Primer foco257/8GREEN07029d; pendiente integrar HTTP C y18suites afectadas restantes.

Reparación JDBC local cerrada ce8023:24 clases afectadas1121tests más HTTP21 final43/Wiring2, todos0fallos. Manifest/SQLaudit y mapaXML en fix_customization_v20_jdbc_cleanup.md. No producción nueva durante ese gate; se reanuda valores tras entrega de los24fixtures congelados.

45. @s10 proyección PG de definición activa a null conserva schema y no crea valores: RED38ecba, GREEN1dd68a (logs45).
46. @s17 lectura de proyecto de otra cuenta responde404: REDda44b1, GREEN349a77 (logs46). Store de valores todavía parcial: TASK, datos guardados, integridad y writes pendientes.

Bundle de lectura de valores: cinco fuentes puras y ReadCustomFieldValuesTest en customization_values_read_ports_manifest.json. get(owner,scope,projectId,entityId) devuelve CustomFieldValues(entityId,scope,schema,revision,values,updatedAt); CustomFieldValue(fieldId,label,type,value) conserva sólo primitivas canónicas en salida. No bean ni Store parcial en entrega. Los valores de entrada NUMBER se transportan BigDecimal exacto; su validador sigue en ciclos separados.

Bundle de lectura puro GREEN8f056d tras formato; root comprometió c47400b y liberó fuentes.
47. @s17 tarea de otro proyecto propio responde404: REDf54f68, GREEN46d17f (logs47).
48. @s10 valores PROJECT guardados conservan0/false/revisión/fecha0001 y ocultan inactivos sin borrar: RED161260, GREEN6f3a45 (logs48).
49. @s10 valores TASK leen su propia colección/esquema: RED6d63ce, GREEN8586a0 (logs49).
50. @s10 bean real de lectura de valores con entidad propia: REDce9213 por bean ausente (logs50). Contexto, esquema y valores sin filas devuelven revisión unconfigured, no se insertan defaults.

50. GREEN47a689 EXIT0: bean real de lectura de valores, PG5 y HTTP de configuración actual en el mismo foco.
51. @s11 comando de primer guardado NUMBER canónico y revisión0: REDb6414a, GREEN03235d (logs51).
52. @s15 seis componentes del ETag compuesto se comprueban antes de validar tipo: RED8127e2, GREEN098322 (logs52).
53. @s13 conjunto incompleto/duplicado/inactivo/desconocido rechazado con índice original: RED41f978, GREENf3a57d (logs53).
54. @s13 cambio de activos preserva valor inactivo, ID y orden de definición: RED3f0065 (logs54). Ventana cedida a root para integrar HTTP Accept0747869 antes del GREEN.

Integridad pendiente señalada por root: validar JSON durable de definiciones antes de deserializar boolean active; valores NUMBER deben leerse como decimal exacto local, nunca ObjectMapper global ni Double con redondeo. Se conserva como pendiente explícito antes del freeze final.

54. GREEN406744 EXIT0 incluye HTTP66 tras integración e9e9e54.
55. @s14 no-op canónico ignora orden,1.0 y null ausente sin reloj: RED5067ec, GREENa8e2e9 (logs55).
56. @s20 valores no desbordan revisión máxima: RED7139d8, GREEN48fe47 (logs56).
57. @s22 valores conservan timestamp monótono sin consumir revisión de esquemaMAX: REDd607e1, GREEN19af51 (logs57).

CI correctiva34162746457:2470Java, único fallo histórico AppearancePersistenceTest.s9 porque migraba18→latest y comparaba esquema excluyendo sólo appearance. Reproducción local6a1ee6 (XML/log customization_appearance_upgrade_red preservados). Corrección exclusiva target19; se mantienen todas aserciones. Prueba propia19→20 pendiente explícita en esta feature, no se sustituye por la histórica18→19.

## Checkpoint GET de valores y puertos PUT

Ocho archivos congelados en customization_values_checkpoint_manifest.json: Store+ApplicationConfiguration+PGValuesTest+WiringTest, más SaveCustomFieldValuesUseCase/CustomFieldValuesRevision/CustomFieldInput y su test puro. Formato real y6focales120/120, EXIT0 7550ea; log customization_values_checkpoint_green.log, XML originales customization_values_checkpoint_xml, conteos customization_values_checkpoint_results.json.

Alcance parcial: GET nominalPROJECT/TASK con propiedad/proyección/bean permite integrar controladorGET de C. PUT sólo puerto/revisión/entrada tipada real; no incluye SaveCustomFieldValues ni colección/helperproject en vuelo. Las firmas del DTO leído no cambian. Persisten integridad durable estricta, traducción503, escrituraPG/composite/carreras/rollback y upgrade propio19→20 pendientes. No se extrapola el callback puro de Save a persistencia.

58. Upgrade propio19→20 con esquema anterior, proyectos/tareas yAppearance guardados: inicialmenteGREEN2bfc30, log customization_58_initial.log. No se inventa RED ni cambio productivo: V20 aditiva existente conserva filas/columnas y crea tres tablas vacías sin eventos. Oráculo independiente de prueba histórica18→19 reparada.

Corrección editorial de trazabilidad: el test de definición ausente corresponde a@s17 (el nombre anterior usaba@s26 por error); se renombra sin cambiar oráculo. Upgrade19→20 respalda la cláusula de migración aditiva y compatibilidad de persistencia, sin atribuirle@s23(seguridad) ni@s42(conjunto vacío). Nombre final additiveUpgradeFrom19PreservesExistingSchemaBusinessFactsAndAppearance; los logs originales conservan el nombre histórico de reproducción.

59. @s11 primer guardado de valoresPROJECT con null explícito durable y confirmación exacta: REDed8329, GREENca8606 (logs59). Adaptador de escritura de valores aún nominal: necesita propiedad previa, colección existente, TASK, no-op, lock/carreras y503.

Aplicación de valores congelada por petición root: SaveCustomFieldValues, puerto CustomFieldValuesEditing, Collection, proyección CustomFieldValues.project y15tests de Save. Manifest customization_values_application_manifest.json; formato y Save15+Input30 GREEN36fdfd EXIT0. XML Save preservado customization_values_application_green.xml. Guardas del caso de uso: comparación de todos componentes, conjunto/tipos/canonicalización, preservación de inactivos, no-op, overflow y max temporal; capturaClock reutiliza CustomizationTime probado. Persistencia no se atribuye a fakes. No bean nuevo de escritura en este bundle.

60. @s11 segundo guardadoPROJECT actualiza misma colección/revisión: RED164c9a, GREENa1c991 (logs60). Se comparte proyección con lectura; no hereda valores de otra entidad.
61. @s17 proyecto ajeno en escritura devuelve404 antes de comparar revisión privada: RED77574f, GREEN0ef420 (logs61).

Aclaración root/C posterior: fieldId repetido se considera estructural independiente y HTTP lo rechaza antes del puerto, incluso con ETag antiguo; root actualiza@s25/prosa. La defensa interna de duplicados se mantiene para llamadores de dominio. Inactivo/desconocido/incompleto y tipos siguen después de revisión.

62. @s16/@s42 dos primeras escrituras[] sin esquema esperan lock compartido antesClock y producen1ganador/1conflicto, sin crear configuración: REDfad73a, GREEN20fe9e (logs62).
63. @s14/@s42 colección con sólo inactivos y versiónMAX acepta[] como no-op, conserva xmin/ctid/valoroculto y noClock: RED1da6b0, GREENe2c59b (logs63).
64. @s18 escrituraTASKterminada preserva todas columnas de negocio y colección separada de subtarea: RED851e02 por escribir equivocadamente en tablaPROJECT, GREENd2cb98 (logs64).
65. @s42 bean real de guardado[] configura sólo valores, no esquema: RED1c03cf por bean ausente. El GREEN conjunto valida también GET HTTP83 integrado y upgrade19→20.

## Checkpoint PG de valores y bean PUT

Seis archivos congelados en customization_values_pg_wiring_manifest.json: Store, ApplicationConfiguration y cuatro tests (PGValues/Wiring/PGCustomization/Commands). Último incluye sólo corrección editorial@s17 de nombre ya explicada; el upgrade19→20 se incorpora desde su GREEN inicial. Focal5suites125/125 EXIT0 86d75a, formato real, log customization_values_pg_wiring_green.log y XML preservados customization_values_pg_wiring_xml.

Permite integrar PUTHTTP completo: ambas colecciones escriben realmente con guardia owner/contexto, revisión compartida, no-op físico y primera carrera observada. Sigue pendiente antes del cierre final: validación estricta JSON y metadata durable, traducción503 de errores de lectura/escritura, rollback/fallos, consistencia RR y carreras entre configuración/valores u otros no-op/cambios existentes; fronteras explícitas restantes del contrato. No es aprobación completa de feature21.

66. @s11 NUMBER acepta los extremos inclusivos −1000000000 y 1000000000 y cero: tres ejemplos inicialmente GREEN bcbc72, log customization_66_initial.log. No se atribuye RED ni cambio productivo.
67. @s20 definición durable exige booleano active explícito: null, ausente y string false reprodujeron tres fallos (customization_67_red.log, BUILD FAILED; código de salida del proceso inicial no recuperado). Guardia compartida anterior a coerción Jackson; GREEN EXIT0 a5df48, customization_67_green.log, con PG configuración, Wiring y los 134 HTTP integrados. Los errores de datos almacenados no se convierten en defaults.

El advisory lock usa una única clave por propietario/ámbito tanto para configuración como para valores. Serializa entidades del mismo ámbito deliberadamente; no se agrega un segundo bloqueo redundante. La lectura usa snapshot RR por solicitud, sin afirmar snapshot entre solicitudes.

68. @s20 forma e invariantes de configuración durable: siete fallos RED de770b entre ocho ejemplos; propiedad extra ya se rechazaba. Validación de tipos JSON y reutilización de Label/View, unicidad de ID/etiqueta; GREEN 377fe6 EXIT0. Logs customization_68_red/green.log.
69. @s20 NUMBER durable conserva precisión decimal antes de validar: seis RED d0b5a7 incluyendo 1.00000000000000000001; GREEN befb11 EXIT0. Reader local USE_BIG_DECIMAL_FOR_FLOATS, validación CustomFieldInput sobre definiciones activas e inactivas; ningún cambio de ObjectMapper global. Logs69.
70. @s20 calendario durable 0000/10000 en configuración y valores: cuatro RED 6b0a40, GREEN 31dcc4 EXIT0. PostgreSQL real acepta ambas fechas del fixture; lectura rechaza fuera del calendario público mediante OffsetDateTime sin getTimestamp. Logs70.
71. @s20 trigger PostgreSQL AFTER INSERT falla en configuración y ambas tablas de valores: tres RED 979145 por excepción JDBC sin traducir; GREEN 2af624 EXIT0. Rollback deja cero filas del agregado y preserva proyectos/tareas/outbox. Se corrigió antes del GREEN el nombre del outbox en el fixture (outbox_events); no cambió el motivo del RED. Sólo errores DataAccess/Transaction se traducen; conflictos/validación/404 se conservan. Logs71.
72. @s20 tabla de valores indisponible en PROJECT/TASK no produce colección vacía: RED c8bb94, GREEN 4026f3 EXIT0. La prueba renombra/restaura únicamente su tabla Testcontainers mediante finally; no toca esquemas ajenos. Logs72.
73. @s10 snapshot de propiedad/esquema/valores y read-only reales: inicialmente GREEN 754b2c EXIT0. Tras SELECT de propiedad, un writer con otra conexión confirma simultáneamente etiqueta/valor/revisiones nuevos; el lector conserva ambos anteriores y otra solicitud ve ambos nuevos. También observa readOnly y aislamiento JDBC RR de la conexión real. No se atribuye RED ni persistencia de snapshot entre solicitudes. Log customization_73_initial.log.
74. @s15 writer de definición retiene lock dentro de Clock; guardado de valores espera de forma observada y, tras commit, recibe conflicto antes de validar un valor de tipo incorrecto. Inicialmente GREEN 4dafc0 EXIT0. El orden de este caso se fuerza explícitamente; no se presenta como ganador aleatorio de una carrera. Log74.
75. @s9/@s16 cuatro casos con revisiones existentes: presentación frente a alta, dos no-op de configuración, dos cambios de valores y dos no-op numéricos canónicos. Espera real de dos contendientes; un cambio/412 o dos confirmaciones sin escritura física, sin ganador prefijado. Inicialmente GREEN 34f5c0 EXIT0. Log75.
76. @s4/@s5 cinco etiquetas válidas: descompuesta frente a é, caso distinto de Dato, espacios interiores, White_Space exterior y 60 pares Unicode. Inicialmente GREEN 939ab0 EXIT0, sin normalización ni pérdida de orden. Log76.
77. @s6 alta duodécima en PROJECT conserva TASK ausente: inicialmente GREEN 63bfde EXIT0. Log77.
78. @s20 hallazgo root: BEFORE INSERT RETURN NULL producía éxito sin fila. Tres RED b4837e, GREEN 927b9a EXIT0 al exigir affected==1 en ambos upserts. Logs78; triggers sólo en base Testcontainers y retirados en finally.
79. @s7/@s21 restaurar vista, desactivar, reactivar y renombrar conserva el valor oculto y su revisión MAX/xmin/ctid. Inicialmente GREEN c63657 EXIT0. Log79.
80. @s11/@s19 proyecto terminado guarda 1000 puntos Unicode pareados y preserva todas columnas de negocio/outbox: primer intento cd1fc3 falló por fixture que asumía completed_at en projects (esa columna sólo existe en tareas). Corregido exclusivamente el INSERT; GREEN ae328d EXIT0 sin cambios de producción. Logs80 inicial/fixture_corrected; no se atribuye RED productivo.
81. @s22 extremos públicos 0001/9999 y truncado microsegundos con una captura Clock: inicialmente GREEN c363d4 EXIT0. Log81.
82. Refuerzo del mismo oráculo71: rama configuración usa alta y constraint trigger DEFERRABLE INITIALLY DEFERRED, por lo que el fallo ocurre en commit real; las dos ramas de valores conservan AFTER INSERT. Inicialmente GREEN 1e51e9 EXIT0, sin cambio productivo. Log82; el nombre del método explicita commit diferido.
83. @s9 el caso de primeras escrituras se amplía con dos altas de definición, conservando el caso original de vista. Una fila, definición y versión0, un conflicto, espera observada y un Clock. Inicialmente GREEN b577ef EXIT0. Log83.
84. @s20 nueve metadatos seleccionados inválidos (id NULL/version NULL/negativa en configuración y ambas tablas de valores): RED f179cc, GREEN 007414 EXIT0. Seam de vista PostgreSQL temporal tras renombrar tabla: no elimina restricciones V20 y restaura en finally. getLong(NULL) ya no se confunde con0; UUID SQL nulo no se confunde con ausencia. Logs84.
85. @s20 raíz JSON y límite de definiciones seleccionadas: diez variantes mediante el mismo tipo de seam; ocho RED 87e374, dos formas de configuración ya rechazadas por Jackson. GREEN 1018f3 EXIT0 al exigir objeto de valores, array de definiciones y máximo12 antes de recorrerlos. Logs85.
86. @s20 UUID textual persistido: abreviatura 1-1-1-1-1 en definición o clave. Una RED 350c90 (clave); la definición ya fallaba al mapearse. GREEN d918a7 EXIT0 con parser canónico compartido. Logs86; no se atribuyen dos fallos iniciales.

## Freeze funcional final de A

Seis deltas de fuente/tests en customization_backend_final_manifest.json; no incluye cambios de B ni HTTP de C. Store incorpora integridad estricta de configuración/valores, decimales exactos locales, metadata válida, traducción503 acotada a almacenamiento/transaction y comprobación affected==1. No se agregan locks: configuración y valores comparten la clave owner/scope existente.

La evidencia de corrupción mediante vistas y triggers es fault injection explícita sobre PostgreSQL Testcontainers. No afirma que NULL/versión negativa o raíz incompatible puedan insertarse naturalmente bajo constraints V20. Se conservan las restricciones originales y se restauran los objetos de cada prueba. Las pruebas de bloqueo sí observan pg_stat_activity y resultados transaccionales reales; el caso74 fuerza el orden schema antes de valores y los casos75/83 no prefijan ganador.

Regresión focal final y spotlessJavaCheck iniciados con once suites21, incluyendo HTTP134 y HTTP+PG8 de C. Manifest antes customization_backend_final_before.json:428 entradas Java/SQL/Gradle. Los resultados, XML y comparación posterior se registrarán al finalizar; este freeze no acredita todavía globales ni mutación ni cierre de feature21.

Resultado final: EXIT0 372d3e, spotlessJavaCheck y once suites con315 pruebas, cero fallos/errores/omisiones. Log customization_backend_final_green.log SHA256 38B85093EA5E7FBC28CEEEF7BD07CD00A24AE1AB8D065229FC2434CE5C60F530. XML originales copiados customization_backend_final_xml; resumen/clases/hashes en customization_backend_final_results.json. Las428 entradas antes/después coinciden y no aparecen inputs nuevos (comparación bdee86). No se repitieron globales ni se ejecutó mutación. Seis fuentes/tests permanecen congelados para revisión root; C mantiene el mapa independiente de cobertura.

## Refuerzos literales solicitados por revisión independiente

87. @s18 amplía el fixture existente con valores PROJECT distintos y hechos poblados generados por StartWorkSession, ChangeWorkSession.close y ChangeTaskStatus reales: sesión cerrada, intervalo, recibo, historial de tarea y eventos. Se comparan todas las filas anteriores, también valores del proyecto y subtarea. Primer intento e996e9 fue fallo de preparación por revisión inicial de sesión0 en lugar de1; corregido el dato del fixture, GREEN13b64e sin modificar producto. No se atribuye RED a personalización. Logs customization_87_initial/fixture_corrected.log. La limpieza amplía sólo las tablas pobladas de la base privada, por orden FK, sin CASCADE.
88. @s20 oráculo adicional único de UPDATE de colección previa: dos valores activos cambian y uno inactivo debe permanecer; un trigger AFTER UPDATE provoca fallo SQL real y el rollback conserva colección/UUID/revisión/fecha/xmin/ctid, esquema y negocio, sin eventos. Inicialmente GREEN8ad999, sin modificación productiva. Log customization_88_initial.log. Se conservan los casos anteriores de fallo INSERT y commit diferido.

Focal de la única clase modificada y formato:62/62, cero fallos/errores/omisiones, EXIT0 35d7e9. Nuevo manifest customization_backend_refined_manifest.json, XML customization_backend_refined_green.xml y resultados customization_backend_refined_results.json. Sólo cambia CustomFieldValuesPersistenceTest respecto al freeze anterior; producción idéntica. Se preservan intactos la regresión anterior315/11 y sus hashes; no se afirma haberla repetido.

### Ciclos89–91: refuerzos puros tras original PIT

Autorización root tras original 378K/3S/5NC. Uno a uno:89 TEXT U+DFFF inicialmente GREEN ea9915;90 label U+DFFF inicialmente GREEN 7a1a39;91 Clock posterior con updatedAt µs/revisión/consulta única inicialmente GREEN b55990. Ningún RED inventado ni producción cambiada. Focal de tres clases + spotlessJavaApply/Check EXIT0 554ff8,77/77 XML preservados. Detalle, hashes y propuesta de replay aún sin ejecutar en progress/mutation_custom_views_fields_backend.md y customization_pit_refinement_manifest.json.