# TDD MoveBlock — feature13

Autor limitado a MoveBlock/MoveBlockUseCase, BlockMoving, MoveContext, BlockMoveRequest, pruebas directas y refactor mínimo PlanBlock/BlockRequest. Árbol OrganizacionWeb-backend; no Store/controller/BlockState/migraciones/publicador/build ni Git. Contrato aprobado41escenarios; alcance directo @s6–s11, @s13–s15, @s22–s23 según responsabilidades del caso de uso. Atomicidad/propiedad/replay concurrente se integrarán por el autor del adaptador, no se atribuyen a mocks.

Ponytail full/Caveman lite y TDD individual. Baseline backendGREENd72c00 comunicado por coordinador; no repetir init global ni competir con ventanaGradle. Lecturas335bd0/de68d3/45aa68/bc18b7/60d82d/397f0e. Las partes truncadas del contrato se releyeron por rango. Respeto rutas protegidas y fuentes frontend congeladas.

Interfaz acordada con resume_backend: BlockMoving.preview(owner,project,task,block,Function<MoveContext,BlockPreview>); commit(owner,project,task,block,key,BlockMoveRequest,Function<MoveContext,BlockMutation>) devuelve BlockChangeConfirmation. MoveContext reúne BlockState y BlockPlanningContext. BlockMoveRequest conserva intención sin objective y construye BlockRequest con objetivo vigente. MoveBlockUseCase.preview añade expectedRevision; move añade key y expectedAvailability. Reuse de evaluate package-private estático con bloque anterior opcional, sin doble resolución; unchanged después de tiempo y antes de solape/presupuesto. Creación11 conserva su conducta.

Ventanas de compilación coordinadas: publicador tiene ventana actual; no escribir Java que deje compilación incompleta mientras otro autor prueba.
## Ciclo1 — @s7 preview con identidad excluida

Test s7_previewExcludesOnlyMovedIdentityAndKeepsOtherTaskReservations: preview10:30–11:30 no cuenta su reserva anterior10–11, mantiene1800s de otra tarea, objetivo original y revisión de disponibilidad. RED real d7a6ec por clases faltantes. Primer comando376b74/da82d9 tuvo ruta relativa equivocada y no escribió test; filtro sin tests no cuenta como RED funcional. Tras mínimo,17db5b señaló verifyNoMoreInteractions sin verificar previamente la llamada esperada; se corrige el oráculo de Mockito conservando comprobación de única consulta.

GREENf26fbe incluye MoveBlockTest y PlanBlockTest después de extraer evaluate package-private estático con catálogo explícito. El caso de uso filtra únicamente id movido y resuelve tiempo una vez. Todavía no se implementan revisión/estado/unchanged ni commit; puerto inicial sólo preview, ampliación seguirá TDD. Ventana Gradle cedida al autor Store/controller tras este corte.
Conteo comprobado en XML58b1f9: MoveBlockTest1/1 y PlanBlockTest24/24, sin fallos ni errores. No atribuir25casos nuevos ni pruebas de PostgreSQL a esta ejecución.
## Ciclo2 — @s11/@s13 recibo y evento nominales

RED65e973 por move/commit ausentes; GREEN8771f6 (2tests directos). Move conserva identidad/objetivo/createdAt, cambia duración a90, produce revisión2/estado planned, recibo y evento con IDs distintos, before/after y un único Clock truncado a microsegundos. No afirma atomicidad real: el puerto fake entrega BlockMutation al adaptador.

## Ciclo3 — @s6 precedencia de revisión/estado/agotamiento

Una matriz de seis ejecuciones cubre preview/commit con revisión obsoleta sobre cancelled, actual cancelled y máximo planned antes de disponibilidad ausente. RED284db6, GREEN9cc613 (8tests). Guardas locales compartidas; no modificar BlockState del otro autor.

## Ciclo4 — @s6 revisión de disponibilidad

Identidad incorrecta o versión obsoleta preceden a proyecto completed: REDf3216f, GREENf59257 (10tests). Commit compara ambas partes antes de evaluación del negocio.

## Ciclo5 — @s10 ausencia de cambio antes de solape

Preview y commit con mismos instantes/zona rechazan BLOCK_UNCHANGED aunque otra reserva solape. REDa20f33. Evaluate recibe anterior opcional; creación pasa null, movimiento comprueba justo tras resolver tiempo una vez y antes de solape/presupuesto. Focal conjunto Move+Plan en curso al registrar.
Ciclo5 GREEN6f9303: MoveBlockTest12 + PlanBlockTest24,36/36, XML comprobado. Se libera Gradle para dos ciclos HTTP y después publicador. Producción propia quieta mientras compilan otros. Pendientes reales: presupuesto de commit, coherencia de cambio de zona, temporal/replay y validación estructural propia; no se declara caso de uso completo.
## Ciclo6 — @s19 estructura antes de negocio/replay

BlockMoveRequestTest.s19_rejectsMalformedDestinationBeforeAnyBusinessOrReplay cubre seis variantes de fecha/zonablank inválidas. RED2381206/6; GREEN3999dc junto a BlockRequestTest heredado. BlockRequest.validateDestination se reutiliza desde ambos records; ningún objetivo ficticio ni consulta de catálogo para construir intención. Fechas/catálogo de negocio siguen dentro del callback para permitir replay histórico.

## Ciclo7 — @s22 presupuesto y permiso específico

Preview conserva días/exceso y commit exige permiso: REDed4037 sólo variante false (true ya pasaba), GREEN98e2cf. El caso calcula0min de presupuesto,1800s de otra tarea,3600s destino,5400s exceso sin contar intervalo propio anterior. Excepción conserva zona/días; permitir exceso confirma sin alterar reglas de solape compartidas.

Corte98e2cf: MoveBlockTest14 y BlockMoveRequestTest6,20/20, sin fallos; Gradle liberado a adaptador y después publicador. Firmas y validación estructural disponibles para integración; quedan oráculos temporal/zona/replay, refactor y formato final. No PostgreSQL ni atomicidad atribuida a estas pruebas directas.
## Desviación de granularidad TDD detectada en revisión

El coordinador observó76f7bf que los ciclos3 y6 incorporaron seis casos parametrizados antes de implementar. No fueron seis ciclos individuales RED/GREEN. Lo mismo aplica a las dos variantes agrupadas de los ciclos4,5 y7: la unidad editada fue el método parametrizado, no una sola fila. Se conserva la evidencia real de cada ejecución y no se rehacen pruebas verdes para fabricar una historia diferente. La cabecera «TDD individual» expresa la regla exigida, pero estos ciclos no cumplieron su granularidad por caso. Desde esta corrección se añade un único caso/fila, se ejecuta y se cierra antes del siguiente. Las variantes inicialmente verdes (p.ej. permiso true del ciclo7) siguen identificadas como tales, no como RED demostrado.
## Reutilización comprobada para la revisión

Las reglas DST/duración/años de @s8 se ejecutan por ResolvedBlockTime.resolve sin modificación; sus oráculos existentes son s8_rejectsGapsWithoutShifting, s8_requiresExplicitOccurrenceWithOrderedOffsets, s6_rejectsNonPositiveLongAndFractionalRealDuration y s6_s7_s8_s10_acceptsExactBoundariesAndHistoricalOffsets. La división de capacidad de @s9 reutiliza BlockBudget.calculate y sus pruebas s11_splitsPositiveIntersectionsAtBudgetMidnight/s12_s15_countsExistingIntersectionsAndExcessInSeconds. La selección determinista de solape y elegibilidad siguen en PlanBlock.evaluate y sus pruebas24 ya reejecutadas. Esta lectura2d1ef2 documenta correspondencia de flujo; no convierte pruebas de dominio no incluidas en los focales en ejecuciones nuevas. El adaptador debe verificar escrituras y reservas vigentes reales.
## Ciclos8–10 — replay, reloj y cambio efectivo de zona

Cada uno se añadió y ejecutó por separado. Fueron refuerzos inicialmente GREEN, sin cambios de producción para forzar historia RED:

- Ciclo8, @s15: s15_confirmedReplayReturnsHistoricalReceiptWithoutClockOrCatalog, GREENec9330. El puerto devuelve recibo histórico incluso con zona ya fuera del catálogo, sin consultar Clock ni catálogo. Prueba la frontera del caso de uso; la búsqueda durable/atomicidad corresponde al adaptador.
- Ciclo9, @s8: s8_confirmationRechecksClockAfterAValidPreview, GREEN2c2304. Preview válido a las09:00; confirmación a las12:00:01 rechaza IN_PAST para inicio12:00. Dos lecturas de Clock entre las dos operaciones, sin reutilizar la hora de preview.
- Ciclo10, @s10: s10_changingOnlyZoneIsEffectiveEvenWhenInstantsStayEqual, GREEN072d4e. UTC10–11 pasa a Madrid11–12+01 conservando instantes; la zona distinta constituye cambio efectivo y produce revisión2.

Refactor GREEN final: helper privado evaluate reúne la exclusión de la identidad propia y la llamada única a PlanBlock.evaluate tanto en preview como en commit. No cambia las reglas ni duplica resolución temporal. GJF focal escribió únicamente los nueve archivos propios en7e2bec.

## Corte final para revisión

Comando final desde backend/: `./gradlew.bat test --tests '*MoveBlockTest' --tests '*BlockMoveRequestTest' --tests '*BlockRequestTest' --tests '*PlanBlockTest' --tests '*ApplicationWiringTest' --tests '*ProjectStateConfigurationTest' --no-daemon`.

4a20fc terminó EXIT1:83ejecuciones,65PASS y18FAIL. XML13e459 confirma Move17 + MoveRequest6 + Plan24 + BlockRequest14 = **61/61 PASS**. ApplicationWiring15/15 falló y ProjectStateConfiguration3/7 falló por el nuevo bean MoveBlock sin mock BlockMoving (causa47f474). El autor de wiring pidió incluir estos focos y recibió la evidencia; no se editaron sus fixtures ni se presenta este comando como global GREEN. Posteriormente comunica corrección propia GREENb9736d; no es una ejecución de este autor. Sin procesos Gradle propios activos y ventana cedida antes del publicador.

Comprobación GJF de sólo lectura y diffcheck9628d3 EXIT0. Nueve fuentes/pruebas congeladas para revisión; sin Git, mutación, PostgreSQL propio ni cambios de configuración.

Comando focal GJF validado (PowerShell, cwd backend/); usar `--dry-run --set-exit-if-changed` para comprobar o `--replace` sólo en archivos propios autorizados:

```powershell
$formatJar = Get-ChildItem -LiteralPath "$env:USERPROFILE/.gradle/caches/modules-2/files-2.1/com.google.googlejavaformat/google-java-format/1.31.0" -Filter '*.jar' -Recurse
$guavaJar = Get-ChildItem -LiteralPath "$env:USERPROFILE/.gradle/caches/modules-2/files-2.1/com.google.guava/guava/32.1.3-jre" -Filter '*.jar' -Recurse
$formatClassPath = $formatJar.FullName + ';' + $guavaJar.FullName
$moveFiles = @(
 'src/main/java/com/apptolast/organization/application/MoveBlock.java',
 'src/main/java/com/apptolast/organization/application/MoveBlockUseCase.java',
 'src/main/java/com/apptolast/organization/application/BlockMoving.java',
 'src/main/java/com/apptolast/organization/application/MoveContext.java',
 'src/main/java/com/apptolast/organization/application/PlanBlock.java',
 'src/main/java/com/apptolast/organization/domain/BlockRequest.java',
 'src/main/java/com/apptolast/organization/domain/BlockMoveRequest.java',
 'src/test/java/com/apptolast/organization/application/MoveBlockTest.java',
 'src/test/java/com/apptolast/organization/domain/BlockMoveRequestTest.java')
& 'C:/Program Files/Java/jdk-25/bin/java.exe' --add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED -cp $formatClassPath com.google.googlejavaformat.java.Main --dry-run --set-exit-if-changed $moveFiles
```

SHA256 de9628d3 (rutas relativas a backend/):

```text
52AC62D72E843144F33F129004042AF0C2C61D4A37BFE6B9D5B673AF1FBB0A3F application/MoveBlock.java
CAAE7E80B06303689F7D813F2DF046D7676CC083ECEC03A8EBA42A7E98551037 application/MoveBlockUseCase.java
0EE02493B765D645D0E696B4D81E04DAA2C74038933F9430948BE18105EC9F9C application/BlockMoving.java
FD9C2ED7D1E5AFE123B2A11D156C7130A63F44520E5ED0738E33AB06C5657A0F application/MoveContext.java
40411E7BB7B5B99A1C076F370315C69BA2D0343BC9B176AA4D4C53A71A70A548 application/PlanBlock.java
864EF82CC23A6FCB2A132DEFFE7282A84A1F04D222E014437A502F12AE097F14 domain/BlockRequest.java
F6050FB9CF0EC61AA32D2596B88B995F2A5BDD0FAD231F96F99983BD9217985B domain/BlockMoveRequest.java
83D079DF4AD05F0D37C4D78F6D28BFBB2D895367659B509EE2B2767433689526 test/application/MoveBlockTest.java
CF80D815EFF5CB718155A44FF93A3DEAB264976F2C286EE43442FE526C243078 test/domain/BlockMoveRequestTest.java
```

En la tabla abreviada application/domain pertenecen a src/main/java/com/apptolast/organization y test/application/domain a src/test/java/com/apptolast/organization. Las rutas completas de la lista del comando son las autorizadas. Se mantienen explícitas las desviaciones de granularidad previas; no se atribuyen diez ciclos RED individuales. Las rutas protegidas permanecen sin acceso ni limpieza.
