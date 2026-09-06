# Dictamen final de mutación backend13

**APPROVED con limitaciones explícitas de cobertura.** La campaña final supera la puerta80 sin exclusiones:758 mutantes =750 KILLED +3 SURVIVED +5 NO_COVERAGE,0 errores/timeouts;750/758 =98,9446%. No se requiere perseguir100 con pruebas reflexivas ni alterar el denominador. La lectura independiente fb3955 confirma exactamente los ocho residuales descritos abajo. XML SHA256 EE62629B1C1F1D58E1CCF3FEA68D60600C49A54BAF9F38530E1D7C58E244EBD6 (4fff67).

Revisión sólo lectura de XML final, fuentes y tests; sin ejecutar pruebas, mutación o código defectuoso. La evidencia de init,98E2E,9smoke y CI verdes procede del coordinador, no de una repetición realizada aquí. Este dictamen aprueba la puerta de mutación backend; la decisión de cierre/merge es del coordinador.

## Ocho residuales conservados

Los mutadores llevan el prefijo org.pitest.mutationtest.engine.gregor.mutators. No se usan ordinales de aparición como identidad.

| Clase / método / línea / índice | Mutador y estado | Juicio |
| --- | --- | --- |
| adapter.http.RescheduleController / blockStateConflict /213 /20 | NegateConditionalsMutator, SURVIVED | Hueco real de oráculo: status/code se verifican, pero no la explicación pública elegida para412 frente409. El mutante intercambia títulos; no es equivalente. Producción actual contiene la selección correcta. |
| adapter.http.RescheduleController / move /42 /37 | returns.NullReturnValsMutator, NO_COVERAGE | Hueco real de conexión: falta If-Match ausente en movimiento con el resto válido. El caso con query inválida sale antes; preview ausente cubre otro handler. No equivalente ni fallo presente: la rama actual devuelve428 correcto. |
| adapter.http.RescheduleController / moveRequest /120 /60 | VoidMethodCallMutator, SURVIVED | Hueco real de oráculo de JSON cerrado: eliminar forEachRemaining admite extras en movimiento/preview. El test de extra actual ejecuta cancel y no ese bucle. No equivalente; producción sí rechaza UNKNOWN_FIELD. |
| domain.BlockBudget / calculate /34 /187 | ConditionalsBoundaryMutator, SURVIVED | Equivalencia contextual aceptable bajo el dominio/catálogo vigentes; argumento abajo. Se conserva SURVIVED en bruto. |
| domain.BlockMoveRequest / endLocal /6 /5 | returns.NullReturnValsMutator, NO_COVERAGE | Accessor generado sin consumidor operativo actual; no añadir prueba de getter sólo para cambiar score. No se proclama equivalencia de la API Java ante cualquier futuro consumidor. |
| domain.BlockMoveRequest / endOffset /6 /5 | returns.NullReturnValsMutator, NO_COVERAGE | Mismo límite, componente endOffset. Conserva NO_COVERAGE. |
| domain.BlockMoveRequest / startLocal /6 /5 | returns.NullReturnValsMutator, NO_COVERAGE | Mismo límite, componente startLocal. Conserva NO_COVERAGE. |
| domain.BlockMoveRequest / startOffset /6 /5 | returns.NullReturnValsMutator, NO_COVERAGE | Mismo límite, componente startOffset. Conserva NO_COVERAGE. |

La propuesta previa de tres refuerzos HTTP en review_reschedule_pit_partial.md permanece válida como mejora concreta. No se oculta por superar el umbral ni se afirma que haya sido ejecutada. Ninguna de estas tres entradas demuestra un bug actual: identifica que la suite tolera introducirlo. No se amplía ahora una matriz ni se promete un kill futuro.

## Presupuesto: por qué el intervalo cero no distingue este límite

El índice187 cambia seconds>0 por seconds>=0 únicamente en la guarda que rechaza un año de presupuesto fuera de1–9999. Con año válido ambas versiones continúan incluso si seconds=0. El año inicial se valida antes del bucle y las fechas sólo avanzan: la primera fecha posterior fuera de rango sólo puede ser10000-01-01.

La única llamada productiva sigue en PlanBlock.evaluate (lectura cd416d); MoveBlock reutiliza esa evaluación después de resolver el destino y excluir la propia identidad. ResolvedBlockTime exige duración positiva1–1440min, segundos enteros e instantes UTC de años1–9999. En la fecha posterior fuera de rango, from=dayStart y el bucle exige dayStart<end. Si dayEnd>dayStart, to=min(end,dayEnd)>dayStart; con extremos enteros resulta seconds>0 y ambas guardas rechazan antes de avanzar. Un intervalo cero en un día ordinario dentro del rango no cambia nada.

La única salvedad serían anclas iguales/invertidas en ese primer día fuera de rango. La evidencia independiente previa e99b42, conservada en review_schedule_block_mutation_candidates.md, comprobó604zonas del runtimeJava25.0.1 sin ese caso. Se reutiliza esa evidencia histórica, no se afirma haber repetido el barrido. No hay nuevo proveedor de zonas ni una llamada productiva que amplíe esas precondiciones. La equivalencia es contextual a ese catálogo y contrato; reexaminar si se admiten zonas personalizadas, otros extremos o nuevos llamadores. No se excluye el mutante del informe.

## Accessors del record

BlockMoveRequest.withObjective construye BlockRequest desde los campos del propio record; no invoca los cuatro accessors mutados. MoveBlock usa withObjective para evaluar, y PostgresBlockStore lo usa para comparar la intención de replay. Las lecturas temporales del store sobre request en persistencia corresponden a BlockRequest, no al accessor de BlockMoveRequest. Ese flujo explica la ausencia de cobertura de los cuatro getters sin implicar ausencia de validación del destino o de sus offsets. La revisión no convierte un método público no usado en equivalente universal ni elimina sus cuatro entradas.

No aparece un bloqueante funcional nuevo sustentado en estos ocho residuales. Se aprueba el gate medido, manteniendo las tres mejoras HTTP y las cinco limitaciones anteriores visibles, con informes originales intactos. Único archivo escrito: progress/review_reschedule_backend_mutation.md.
