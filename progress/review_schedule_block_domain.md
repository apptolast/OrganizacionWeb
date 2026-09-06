# Revisión incremental del dominio — schedule_block

Estado: IN_PROGRESS. Revisión independiente del núcleo temporal y presupuesto frente al contrato aprobado a84e42f (62 escenarios, 325 casos). Ponytail full y Caveman lite. No es congelación, aprobación final ni puerta de mutación.

Alcance leído: BlockRequest, ResolvedBlockTime, BlockOffsetException, BlockBudget, BudgetDay, sus tres archivos de pruebas y progress/tdd_schedule_block_backend.md. Se consultaron Availability y el llamador PlanBlock sólo para comprobar precondiciones. No se modificaron producción, pruebas ni contrato. No se ejecutó Gradle ni mutación. Un experimento de fechas con jshell terminó EXIT 0, sin guardar auxiliares.

## Hallazgo real

### [P2] Una regresión de fecha local deja vacío el presupuesto de un intervalo positivo

Ubicación: backend/src/main/java/com/apptolast/organization/domain/BlockBudget.java:9–15.

El bucle supone que la fecha local del inicio nunca es posterior a la del fin. Eso no se cumple en un cambio de zona que atrasa el reloj cruzando medianoche. Ejemplo verificado con ZoneRules del Java instalado:

- Zona del presupuesto: Antarctica/Casey.
- Inicio UTC: 2010-03-04T14:30:00Z; fin UTC: 2010-03-04T15:30:00Z. Duración real: 60 minutos.
- Inicio local: 2010-03-05T01:30+11:00.
- Fin local: 2010-03-04T23:30+08:00.
- Transición informada por Java: `Transition[Overlap at 2010-03-05T02:00+11:00 to +08:00]`.

Con esos valores, date es 2010-03-05 y last es 2010-03-04: el while no entra y calculate devuelve una lista vacía. Incumple days no vacío y suma de requestedSeconds igual a durationMinutes por 60 (@s11 y precisiones del preview). La clasificación local del extremo no sirve como límite del bucle: intercambiar únicamente date y last tampoco corrige todos los intervalos dentro de la fecha repetida.

La reserva puede estar expresada en UTC con reloj controlado anterior, por lo que no depende de elegir una hora local ambigua para el bloque. La fecha histórica es un fixture permitido para comprobar el cálculo con las reglas reales del catálogo admitido. El requisito de usar las fechas reales de presupuesto no restringe zonas a Madrid.

Los anchors atStartOfDay verificados son: 2010-03-04 se inicia en 2010-03-03T13:00Z; 2010-03-05 en 2010-03-04T13:00Z; 2010-03-06 en 2010-03-05T16:00Z. El contrato usa estos intervalos consecutivos, no una reclasificación de cada instante mediante LocalDate. Por tanto el resultado esperado es una única fila de presupuesto 2010-03-05 con requestedSeconds 3600.

Un segundo vector empieza después del retroceso: 2010-03-04T15:10Z a 15:40Z. Ambos extremos tienen fecha local 2010-03-04, pero pertenecen al intervalo entre los anchors del 5 y del 6; resultado esperado: presupuesto 2010-03-05, requestedSeconds 1800. Este caso impide resolver únicamente con máximo/mínimo de las fechas de extremos.

Corrección mínima requerida, acordada con el coordinador: localizar el intervalo entre anchors consecutivos que contiene start, ajustando el candidato start.atZone(zone).toLocalDate cuando no lo contiene. Después avanzar por anchors hasta cubrir end UTC, sin usar end.toLocalDate como condición de parada ni una ventana arbitraria de días. Conservar las intersecciones UTC actuales, fechas ordenadas, suma exacta y omisión de filas de cero segundos. No se requiere un segmentador ni cambiar la semántica de presupuesto. Añadir primero el caso rojo vacío y el caso que comienza en la fecha repetida; preservar Apia. Ambos vectores se comunicaron directamente al autor backend y al coordinador.

## Cobertura pendiente del tramo ya escrito

Son huecos observados en los tres archivos de pruebas, no una afirmación de cierre incumplido: el autor sigue en TDD y la bitácora sólo registra los ciclos alcanzados.

- @s3: aceptación de exactamente 500 puntos de código. El rechazo de 501 no protege contra rechazar por error el límite válido.
- @s6: duraciones positivas límite de 1 y 1440 minutos. Los casos actuales verifican 60 y los rechazos.
- @s7: inicio exactamente igual al reloj. El caso de un microsegundo anterior no protege contra cambiar isBefore por una comparación inclusiva.
- @s8: éxito con ocurrencias explícitas e inversión aparente del reloj de Madrid; éxito de 30 minutos en Lord Howe. Los gaps y ambigüedades sí tienen aserciones de campo, código y opciones.
- @s9: aceptación real de Europe/Paris en 1900 con +00:09:21 y conservación de segundos. Monrovia sólo cubre el rechazo por duración fraccionaria.
- @s10: éxito en 9999-12-31 usando límite interno del día siguiente en año 10000; las pruebas actuales sólo cubren los rechazos fuera de rango.
- @s11: día de 23 horas de Madrid, transición de otoño, fin exactamente en medianoche, y proyección del bloque a una zona de presupuesto distinta. Hoy se cubren UTC, Apia y los límites públicos de año.
- @s15: presupuesto cero y exceso exactamente cero en el borde. El caso existente comprueba exceso positivo sobre presupuesto de 60 minutos.

Los tests de reconstrucción de BlockRequest afirman sólo la clase de excepción. Si se usan como evidencia de errores por campo, faltará afirmar campo y código; la validación HTTP puede proporcionar esa evidencia sin duplicar cada prueba de dominio.

## Diseño y límites

La división actual entre intención, tiempo resuelto y cálculo de presupuesto es pequeña y concreta. No encuentro una abstracción que deba eliminarse. La copia defensiva de validOffsets y el uso de instantes/segundos evitan decisiones implícitas de ocurrencia y redondeos. Las validaciones de año y precisión en los constructores protegen reconstrucciones; no recomiendo eliminarlas para acortar código.

La comprobación de pertenencia al catálogo en ResolvedBlockTime pertenece a creaciones nuevas: el replay debe continuar evitando ese resolver en aplicación/persistencia, trabajo fuera de este dictamen. Propiedad, bloqueo, solapes, idempotencia y publicación requieren su revisión posterior independiente; no están acreditados aquí.

El mojibake de algunos mensajes temporales y de presupuesto ya fue comunicado por el coordinador. Se observa todavía en el corte leído; no se duplica como hallazgo nuevo ni se atribuye su corrección.


## Revisión reanudada del núcleo — 2026-09-06

Veredicto: **PENDINGS por cobertura**. No se observa un defecto nuevo de producción en el núcleo leído. El hallazgo P2 de Casey queda corregido. Este dictamen no aprueba feature 11, adaptadores, publicación ni la puerta PIT.

Alcance: lectura completa de BlockRequest, ResolvedBlockTime, BlockBudget, PlannedBlock, PlanBlock, sus puertos/modelos y los cinco archivos de pruebas correspondientes, contrastados con project-spec.md y features/schedule_block.feature aprobado a84e42f. Se aplicaron Ponytail full y Caveman lite. Se conserva la baseline compartida de init documentada por el coordinador; no se ejecutó init/Gradle ni mutación durante el TDD concurrente de adaptadores. Producción, pruebas y contrato permanecen sin modificaciones de este revisor.

### Hallazgo corregido y límites de evidencia

BlockBudget ya no usa la fecha local del extremo final como condición de parada. Avanza por anclas consecutivas hasta cubrir end UTC y omite intersecciones no positivas. En el segundo vector Casey empieza en la fecha local repetida, descarta la intersección no positiva del día 4 y alcanza correctamente el intervalo del día 5. Ambos vectores ahora tienen aserciones completas de BudgetDay para 2010-03-05, con 3600 y 1800 segundos respectivamente; se mantiene el caso Apia. La implementación es más corta que el ajuste explícito de candidato propuesto anteriormente y resuelve los dos vectores sin cambiar la semántica por anclas.

La bitácora registra 80 pruebas previas del núcleo (18 aplicación y 62 dominio); esta revisión no las reejecutó. Al consultar backend/build/test-results/test sólo había un XML coincidente con Block, ScheduleBlockApiTest de un caso, timestamp 2026-09-06T07:36:14.425Z. Por tanto no se presenta un XML del núcleo ni una ejecución independiente actual como evidencia de esas 80 pruebas.

### Cobertura anterior resuelta

- ResolvedBlockTimeTest añade aceptación de 1 y 1440 minutos, igualdad con Clock, Lord Howe con ocurrencias explícitas, París 1900 con segundos y extremos válidos de años 0001/9999.
- BlockBudgetTest añade el día primaveral de Madrid de 82800 segundos y el límite interno del año 10000. El fixture de otoño contiene 86400 segundos dentro del día de 25 horas; no equivale al cruce del ancla final indicado en @s11.
- PlanBlockTest comprueba presupuesto cero mediante rechazo de creación con una fila completa de exceso.
- Los mensajes revisados se leen correctamente, sin el mojibake señalado en el corte anterior.

### Pendientes concretos para aprobar el núcleo

1. **BlockRequestTest, @s3:** aceptación del límite de 500 puntos de código, preferiblemente caracteres suplementarios. Sólo existe rechazo de 501; una restricción accidental a 499 seguiría verde.
2. **ResolvedBlockTimeTest, @s8:** éxito con reloj local invertido de Madrid y offsets +02:00/+01:00. Lord Howe protege una etiqueta igual, pero no la inversión aparente exigida.
3. **BlockBudgetTest, @s11/@s28:** fin exactamente en medianoche; proyección UTC a Europe/Madrid cruzando dos días; cruce del ancla final del día otoñal. Afirmar fechas, segundos y ausencia de fila cero. Para reservas históricas, falta comprobar plannedSeconds repartido en ambos días de la nueva zona.
4. **PlanBlockTest, @s13/@s14:** aceptación de bloques contiguos y selección de conflicto por startAt antes del UUID. El test actual sólo compara UUID para dos conflictos con inicio idéntico; eliminar el orden primario no lo rompería. Afirmar también el borde inverso de contigüidad para proteger ambas comparaciones semiabiertas.
5. **PlanBlockTest/BlockBudgetTest, @s15:** exceso exactamente cero en el límite y creación permitida con exceso y allowOverBudget=true. El único uso de true comprueba rechazo por solape; convertir la condición de presupuesto en un rechazo incondicional del exceso no rompería los tests actuales.
6. **PlanBlockTest, @s7:** creación exactamente al Clock y rechazo cuando el reloj avanza después de un preview válido. El resolver protege igualdad en aislamiento; falta proteger la repetición temporal por la ruta de creación.

Las aserciones de reconstrucción de BlockRequest siguen comprobando únicamente la clase de excepción. Campo/código deberá quedar acreditado en pruebas HTTP o ampliando estas pruebas; no se exige duplicación si la evidencia HTTP lo cubre. Los casos elegibles idea/paused, la precisión efectiva de microsegundos con reloj fraccionario y replay histórico también necesitan evidencia de aceptación en adaptadores antes de aprobar la feature, sin atribuirla a los tests unitarios actuales.

### Diseño y frontera de responsabilidad

La evaluación compartida conserva disponibilidad/revisión, proyecto antes de tarea, resolución, solape y presupuesto. La captura única del reloj y las copias inmutables son concretas y justificadas. Los puertos permiten que persistencia resuelva propiedad/replay antes de ejecutar la operación; esa garantía aún depende del adaptador y no queda probada por los dobles unitarios. No se recomienda añadir capas ni eliminar validaciones para acortar código.

Se comunicaron los pendientes al coordinador y al autor backend. No se modificaron ni inspeccionaron los temporales bloqueados, ni se intentó su limpieza. La revisión se limita a este append; se preserva íntegra la historia anterior.

## Handoff de cobertura adicional — 2026-09-06

Tras el dictamen PENDINGS, el coordinador y autor backend cedieron a este revisor la autoría de los tests pendientes. Los seis grupos quedaron cubiertos con 13 casos adicionales inicialmente verdes, sin cambios de producción. Evidencia, nombres y ciclos en [tdd_schedule_block_core_coverage.md](tdd_schedule_block_core_coverage.md). Regresión de cinco suites: 93 pruebas, 0 fallos/errores/omitidas, ejecución propia f92fc9 después de Spotless.

No se cambia unilateralmente el veredicto a APPROVED: al haber escrito la cobertura nueva, corresponde revisión independiente del coordinador u otro agente. La evidencia de estados elegibles, precisión de createdAt y replay se encuentra en las pruebas HTTP actuales, identificadas en la nueva bitácora. Feature/PIT siguen sin aprobación desde este informe.

## Revisión independiente de la cobertura adicional

El coordinador revisó los nuevos casos de BlockRequestTest, ResolvedBlockTimeTest, BlockBudgetTest y PlanBlockTest. Los seis grupos pendientes quedan resueltos: límite Unicode de 500, inversión local de Madrid, medianoche y reparto según zona actual, contigüidad en ambos sentidos y orden primario de conflicto, exceso cero/consentido y reloj reevaluado al crear. Las aserciones comprueban valores de dominio, no sólo ausencia de excepción.

El caso de presupuesto histórico comprueba por separado cada fecha afectada con una solicitud diurna sin solape, y conserva los instantes de la reserva. La prueba temporal utiliza el mismo servicio para preview y creación y avanza su reloj controlado; detectaría reutilizar la hora del preview.

Veredicto del núcleo: APPROVED para diseño y cobertura de este tramo. Evidencia de ejecución del autor f92fc9: 93 pruebas verdes; no se presenta como repetición del coordinador. La revisión de la funcionalidad completa, el arnés global y la mutación siguen pendientes. Esta aprobación parcial no acredita locks, atomicidad, idempotencia o recuperación del adaptador PostgreSQL.
