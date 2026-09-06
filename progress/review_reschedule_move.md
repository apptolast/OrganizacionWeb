# Revisión del caso de uso MoveBlock

Dictamen: APPROVED para el corte directo de nueve archivos descrito en tdd_reschedule_move.md. No constituye aprobación del adaptador PostgreSQL ni cierre de la funcionalidad 13.

Root revisó MoveBlock, sus puertos/contexto, BlockMoveRequest, las extracciones de PlanBlock/BlockRequest y ambas suites directas (e0eb47, ca90ca, f6ece8). Hashes de implementación y suite principal comprobados en b82b69 coinciden con el manifiesto del autor 9628d3.

El movimiento conserva identidad, objetivo y creación; excluye sólo la reserva propia al evaluar capacidad y solape; reutiliza resolución temporal y presupuesto existentes. La precedencia de revisión, cancelación, agotamiento y disponibilidad concuerda con el contrato. Resuelve el tiempo antes de detectar ausencia de cambio; permite cambiar la zona manteniendo instantes. Usa una lectura del reloj por operación y el mismo instante truncado para estado, recibo y evento. El callback permite que el adaptador recupere un recibo histórico sin consultar catálogo ni reloj.

Evidencia del autor: 61 casos propios y compartidos aprobados en 13e459, dentro de una ejecución ampliada 4a20fc que terminó EXIT1 por 18 fixtures de wiring ajenos a este corte; corregidos por su autor en b9736d. No se presenta esa ejecución completa como verde. El intento posterior de root de consultar sus XML encontró que ya no estaban en el directorio de resultados compartido; no constituye una nueva verificación ni contradice el registro previo. Formato focal comprobado por el autor en 9628d3. Los tres últimos oráculos de replay, reloj y zona fueron inicialmente verdes, no ciclos RED inventados.

Se conserva la desviación de granularidad TDD de los ciclos parametrizados documentada por el autor. La aprobación funcional no certifica que esos ciclos fueran individuales. Atomicidad, deduplicación durable, carreras, lecturas vigentes, constraints, integración HTTP completa y mutación siguen pendientes de su verificación separada.
