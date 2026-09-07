# Revisión de los oráculos finales de inicio de trabajo

APPROVED. Root revisó los dos cambios de pruebas, sin escribir producción ni tests. El esperado de WorkSessionPersistenceTest compara los once campos del JSON durable con valores independientes del record serializado; elimina la comparación que mutaba simultáneamente resultado y esperado. Lectura7c8604/c92892, prueba PG inicialmente GREENd8ed64 y Spotless real81027f según bitácora. No se atribuye un RED inexistente.

PublishOutboxTest añade una fecha de calendario imposible con forma UTC correcta y conserva el resto del evento válido. El oráculo exige exactamente blocked/INVALID_EVENT y falla ante cualquier invocación del broker. Root70da5c/43cd97 contrastó el diff, la rama DateTimeException de producción y el helper. XML43cd97 confirma153 pruebas, cero fallos, errores u omitidos; hash69A14120302754114B03AFCBA1362B76E4908EB8B1B020A1883EDECD3BA5DB62. No cambia producción.

Los resultados originales de mutación permanecen intactos: el nuevo test no convierte retrospectivamente NO_COVERAGE en KILLED. El replay del record tendrá informe propio y sólo podrá acreditar las identidades que mida. El timeout de Rabbit mantiene su clasificación; las comprobaciones existentes de límites y useNio no permiten inventar una causa o detección en aquel proceso.
