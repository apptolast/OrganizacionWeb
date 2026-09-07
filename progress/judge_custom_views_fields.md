# Revisión incremental de vistas y campos personales

Estado: **IN_PROGRESS**, sin aprobación de cierre de la feature 21.

## Cortes revisados

- Lectura hexagonal y PostgreSQL: puertos y modelos `9ef8a75`, adaptador,
  migración V20 y bean real `7b58da6`; GET HTTP integrado en `b1ed8e6`.
  La ejecución combinada de HTTP y arranque real quedó verde en `34a4d6`.
  Esto acredita la lectura inicial, no las escrituras o toda la integridad
  de filas seleccionadas.
- Helpers de dominio `CustomizationView` y `CustomFieldLabel`: `36ee10d`.
  Revisados contra los ámbitos, duplicados y Unicode White_Space del
  contrato. Sus dos hashes coinciden con el manifiesto entregado.
- Puertos reales de creación y edición de definiciones: `bdacad6`.
  Sus dos hashes se verificaron antes de transferirlos al agente HTTP
  mediante `9db2289`. No se añadieron beans ficticios.
- Cliente de configuración y 43 pruebas focales: `709021f`.
  Dos hashes coinciden con `customization_frontend_config_freeze.json`.
  Revisados DTO cerrado, identidad de ámbito, BIGINT, ausencia y defaults,
  etiquetas, tipos, conservación de definiciones y de la intención enviada.
  El helper existente `instant` ya limita fechas válidas y microsegundos.
  Se conserva la evidencia focal de formato, ESLint y TypeScript del autor;
  no equivale a una ejecución global del frontend.
- GET y PUT de vista HTTP: corte aislado `692c386`, con 30 pruebas MVC,
  cero fallos, errores u omisiones comprobados en el XML. Tres hashes
  coinciden con `customization_http_view_freeze.json`; formato y regresión
  del autor terminados con EXIT 0 (`6140ab`). Reutiliza el validador de
  dominio, comprueba cabeceras antes del JSON y no publica datos privados
  al informar de un conflicto. Su integración en la aplicación espera el
  bean de escritura real. Los mocks de casos de uso no prueban PostgreSQL.
  Posteriormente integrado en `6e128b3`, tras los beans reales del corte
  de escritura `4e3a8a8`.
- Escrituras de configuración: `4e3a8a8`, once hashes corregidos comprobados
  contra `customization_writing_checkpoint_corrected_manifest.json`.
  Las 46 pruebas del checkpoint original tienen cero fallos, errores y
  omisiones; el fixture corregido tiene dos pruebas verdes adicionales
  verificadas en su XML. No se suman como 48 pruebas distintas.
  Revisados los tres comandos, serialización por cuenta y ámbito antes del
  reloj, carrera inicial con un ganador y un conflicto, no-op físico
  mediante `xmin`/`ctid`, tiempo monótono y ausencia de eventos de negocio.
  La traducción de errores de escritura a 503 y validación integral de filas
  guardadas siguen pendientes; no es aprobación final del adaptador.
- Definiciones HTTP integradas en `55e9c46`: 43 pruebas MVC verdes,
  tres hashes verificados, creación y edición con puertos reales y
  validación por índice corregida. No acredita todavía negociación completa.
- Clientes de configuración y valores integrados en `65d4043`: cuatro
  hashes verificados, 50 pruebas de configuración y 38 de valores verdes.
  Revisados esquema compuesto, orden de definiciones, tipos, límites,
  aborto y confirmación coherente. Formato, ESLint y TypeScript verdes
  en `9e69bc`; ejecución focal de 88 pruebas en `aece31`.
- Valores: cuatro archivos puros revisados e integrados en `1522d39`;
  treinta pruebas de tipos, fechas, Unicode, ausencia y números exactos.
  Lectura PostgreSQL nominal y bean real integrados en `98b53a4`, con cinco
  pruebas de persistencia y tres de wiring. Ocho hashes coinciden con
  `customization_values_checkpoint_manifest.json`. Los seis XML originales
  suman 120 pruebas verdes, pero incluyen quince de Save aún en desarrollo;
  esas quince no acreditan un comando integrado en este corte.
  Siguen pendientes la validación integral de filas y traducción de errores.
- GET de valores `a403f5d`, desde `1267146`: tres hashes y XML de 83 casos
  MVC verificados. Se revisaron ambas rutas, UUID completo, errores privados,
  proyección cerrada y ETag compuesto con BIGINT exacto, cero, false y null.
  Integra el constructor sólo después del bean real. No sustituye las
  pruebas integradas ni acredita todavía PUT de valores.
- Caso de uso de guardado en `8b6f893`: cinco hashes y XML de quince pruebas
  verificados. Conserva identidad y valores inactivos, compara todos los
  componentes de revisión antes del conjunto/tipos y no consume reloj ni
  revisión en no-op. Tiempo monótono y límite BIGINT revisados. La proyección
  usa las definiciones activas ordenadas. El callback de los tests no acredita
  bloqueo, commit ni rollback PostgreSQL; ese adaptador sigue en desarrollo.

La aclaración `b3436fc` distingue fieldId repetido (estructural, antes de
revisión) de conjunto activo incompleto o IDs desconocidos (requiere esquema).
Se añade un ejemplo a @s25 bajo autorización global; siguen 42 escenarios.
Hash actual del Gherkin:
`BAEC71FF20540B69E4DA01846EB85767F3A6D86EAA1D06F65569393B4CBC7BE8`.
El hash anterior documenta el contrato inicial, no esta precisión posterior.

Integración nominal posterior: `ac8b484` (PG/beans, seis hashes y cinco XML
con 125 pruebas verdes) y `e617a09` (PUT HTTP, tres hashes y 134 MVC verdes).
Revisados SQL y oráculos de identidad, no-op físico, carrera inicial,
propiedad previa a revisión, tarea terminada y upgrade 19→20. La negociación,
precondición compuesta, decimal exacto y errores indexados preceden al puerto
según contrato; no hay GET previo en el controlador de escritura. Pendientes
integridad seleccionada y fallos de almacenamiento antes del juicio final.

Soporte de mutación `cfd319f` revisado en aislado: cinco hashes, 61 pruebas de
arnés verdes, y omisión de CustomizationWiringTest en default corregida.
No hay campaña ni score 21. Alcance Stryker de montaje todavía pendiente.

## Hallazgos que deben resolverse antes del cierre

1. El cliente valida el contenido y la gramática del ETag, pero todavía debe
   comprobar que la confirmación respeta la revisión anterior: UUID estable,
   versión cero en primera escritura, incremento único en cambio real y
   revisión/fecha conservadas en no-op. Un cuerpo cambiado con el ETag
   anterior no puede presentarse como éxito. También debe impedir confirmar
   una edición cuyo campo no existía en la instantánea enviada. Corrección
   delegada al autor mediante TDD, sin bloquear su trabajo independiente de
   valores.
   Resuelto en `65d4043`: guardia común de versión y fecha, esquema fijo
   en valores y existencia previa del campo editado. Se revisaron código
   y oráculos de confirmación contradictoria, no-op y microsegundos.
2. En la lectura provisional de `UpdateCustomField`, el reloj podía hacer
   retroceder `updatedAt`. El autor confirmó que su ciclo 29 ya reproduce
   ese caso en rojo (`d869dc`). La implementación no estaba congelada ni
   se había aprobado como completa.
   El corte de escritura posterior ya aplica el máximo entre la fecha
   anterior y el reloj; caso verde `6a17d1` y código comprobado por root.
3. La prueba de arranque `CustomizationWiringTest` comparte PostgreSQL entre
   dos métodos, pero uno contaba todas las filas esperando una. El otro
   método también crea configuración, por lo que el resultado dependía del
   orden de ejecución. Se ha pedido aislar el recuento o la preparación
   antes de integrar el corte de escritura.
   Resuelto antes de integrar: reproducción real con orden inverso
   `a88843` en rojo, limpieza de preferencias en PostgreSQL exclusivo y
   `6433a1` en verde. Se retiraron las anotaciones temporales de orden;
   ejecución final `e02de9` verde, XML de wiring revisado por root.
4. CI `34160728290` sobre `328b505` falló: 1122 de 2398 pruebas Java.
   V20 añadió dos FK y las limpiezas antiguas no incluían las tablas nuevas.
   Reproducción local `e21b09`, SQLSTATE `0A000`. Reparación Java `641ef4f`:
   24 hashes y 24 clases/XML revisados, 1121 pruebas verdes; sólo se añaden
   las dos tablas a los TRUNCATE. HTTP actual 43 y wiring 2 también verdes.
   Reparación E2E `3efa87c`: 42 sentencias en 32 pruebas y limpieza propia
   del fixture autenticado; 36 hashes revisados, sintaxis/formato y AST
   comprobados por el autor. Su ejecución E2E y CI correctiva están pendientes.
   Log original externo: `organizationweb-customization-ci-34160728290.log`,
   SHA256 `606F9D02AD4B161474170A3A7AD889A1A2009F2CE468629CE121AC47C4FA739E`.
5. El oráculo HTTP de Accept descubrió que un PUT podía llegar al caso de uso
   antes de fallar la negociación de respuesta (`15cd96`). Se ha ratificado
   una guardia local a las rutas 21, basada en negociación y especificidad
   de Spring, con rechazo antes de cualquier comando. Corrección y sus
   pruebas de calidad, comodines y ausencia de efectos todavía pendientes.
   Resuelto en `e9e9e54`, desde el corte aislado `0747869`: guardia previa
   a los puertos con especificidad y calidad de Accept. Los 66 casos MVC
   del autor pasaron en `507b84`; integración COMMON también verde en
   `406744`. No hay cambio global de negociación ni aprobación final HTTP.
6. CI correctiva `34162746457` terminó con 2469 de 2470 pruebas Java verdes.
   El único fallo es `AppearancePersistenceTest.s9`: su migración desde V18
   usa latest y compara el esquema excluyendo sólo la tabla de apariencia.
   V20 añade legítimamente tres tablas. Se solicita fijar esa prueba histórica
   en V19 y comprobar V20 en su propio contrato, conservando los oráculos.
   Log externo: `organizationweb-customization-ci-34162746457.log`, SHA256
   `1244733DB8F1768E9FB3FB1FBDF3C25B9EAFD66EBA65BECFA97610C3EA74BCF6`.
   Los pasos posteriores de build y E2E aún no se ejecutaron en esta CI.
   Corregido en `f46200c`: único cambio funcional target("19"), con todas
   las aserciones anteriores conservadas. Hash y XML de veinte pruebas
   verdes verificados por root (`0dd56d`). Nueva CI `34163594795` en curso.
   Esa CI terminó SUCCESS sobre `f46200c`: init, build, DNS Docker,
   143 E2E y publicador verdes. Log externo conservado, SHA256
   `1F2CB731383E27189117F9079B9F9F30E021AE6F57E1D1420B5CA62E35D96E64`.
   No se atribuye el resultado a los commits posteriores de valores.

## Puertas pendientes

Persistencia de comandos y valores, validación de datos guardados,
concurrencia y rollback; HTTP completo con los beans reales; estado y
formularios del navegador, recuperación y privacidad; revisión de código
final, pruebas integradas, mutación, matriz UX y aceptación tras despliegue.

La producción continúa en `ed00ad4`, con las funciones 1–20 aceptadas.
No se ha desplegado la feature 21. V14 heredada permanece fuera del trabajo.

## Revisión final de persistencia, 8 de septiembre

Root ha leído el Store y los seis deltas congelados por A. La lectura exige
JSON y metadatos válidos, UUID canónicos, precisión decimal y calendario
público. Ambos upserts exigen una fila afectada. Las transacciones traducen
fallos de almacenamiento y commit a 503 sin confirmar una escritura perdida.
El bloqueo compartido por propietario/ámbito serializa configuración y
valores; su granularidad conservadora es suficiente para el uso personal.
La lectura RR mantiene propiedad, esquema y valores en el mismo snapshot.

Los oráculos nuevos distinguen corrupción inyectada mediante vistas/triggers
de filas que las constraints admitirían naturalmente. Las carreras observan
espera real, conservan xmin/ctid en no-op y no prefijan ganador salvo el caso
que requiere explícitamente que el esquema confirme primero.

Sin hallazgos productivos abiertos en este corte. La regresión final focal
registra 315 pruebas en once suites, cero fallos y 428 hashes estables.
La auditoría independiente `review_customization_backend_coverage.md` delimita
dos fixtures pendientes de refuerzo para @s18 y @s20; no los presenta como
bugs. El dictamen no acredita todavía mutación ni la UI final.

CI `34165164208` sobre `9835bef` terminó SUCCESS con 143 E2E, build y publicador.
Log externo SHA256 `D7057D90CBE8363587EE52F6395D1840B0D597F083E72522BC3F7F730A9D4185`.
Es evidencia del corte nominal, anterior al freeze final de A y frontend.

Refuerzos revisados y aprobados: @s18 genera sesiones, intervalos, recibos,
historial y eventos reales antes del guardado; compara filas completas y
valores independientes de proyecto/subtarea. @s20 provoca fallo AFTER UPDATE
con dos valores activos y uno inactivo previos, y verifica rollback físico.
Sólo cambia la clase de pruebas. Root verificó seis hashes del manifiesto
refinado y su XML de 62 casos, cero fallos/errores (`880f9f`). La primera
regresión 315/11 queda preservada, no se presenta como reejecutada.
Backend aprobado para iniciar PIT; no es todavía aprobación de mutación.

## Mutación backend aprobada

Root verificó el XML original y los 437 hashes before/after (`db7081`):
378 KILLED de 386, tres SURVIVED y cinco NO_COVERAGE, 97.9274611%, sin
errores ni timeouts. Los tres supervivientes eran límites observables de
Unicode y avance temporal. Se revisaron los refuerzos test-only y sus tres
XML, 77 casos verdes (`c92819`), sin modificación productiva.

Replay dirigido autorizado con init de tres clases y dos mutadores, separado
del original. Root comprobó las quince firmas completas previstas contra
el XML real, todas KILLED, incluidos los tres objetivos (`ac8c59`); 438 inputs
estables. XML SHA256 `55BB9C3525C7514DD2CF9A6542A81953036EB648E3833A73ADFCB59C75177478`.
No se agregan campañas ni se sustituye el score original. Los cinco accesores
sin cobertura conservan su clasificación y el denominador original.

Puerta de mutación backend superada. CI `34167438318` sobre `1cd88e7` también
terminó SUCCESS con 144 E2E y publicador. Frontend, integración final y
aceptación productiva siguen pendientes; no se declara terminada la feature.

## Revisión final de interfaz y trazabilidad

Frontend funcional y foco164 aprobados mediante revisión independiente de
estado, formularios, fixtures y navegador. Root contrastó los catorce hashes
finales (d57287) y los 221 hashes de fuentes/evidencia UX (3c82d8), además de
inspeccionar zoom nativo y recuperación de foco Firefox. CSS revisado y
transferido en 433aced; modalidades finales integradas en b22440b.

Se acepta el alcance técnico documentado por ux_customization.md: Chromium
cinco recorridos focales, Firefox cuatro; WebKit original tres de cuatro y
ensayo separado de recuperación con sólo el enlace abierto por clic en
Windows. No se declara navegación Tab de ese enlace, dispositivos físicos,
lector de pantalla humano ni comprensión universal. El límite observado no
se transforma en un éxito del recorrido original ni en un defecto confirmado
de la aplicación.

La revisión independiente review_customization_acceptance_final.md no señala
huecos adicionales en los 42 escenarios. No sustituye las puertas pendientes:
Stryker frontend, corrección del arnés de rangos históricos, init/CI integrada,
rollback y aceptación productiva. Feature21 conserva in_progress.
