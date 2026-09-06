# Revisión del corte backend de Replanificar

Veredicto: aprobado para integración local; feature 13 sigue in_progress.

El coordinador revisó la implementación y las entregas independientes de Move,
lecturas, errores HTTP, migración V13, publicación y coordinación. Se conservan
sus revisiones específicas y la bitácora de 59 ciclos, incluidas las desviaciones
de matriz anteriores y los casos inicialmente verdes. No se atribuyen 156 casos
HTTP nuevos a una combinación de pruebas de dominio, adaptadores y regresiones.

La verificación independiente 1fa542 de los XML acredita 447 pruebas en 16
suites, sin fallos, errores ni omisiones. El inventario con hashes está en
reschedule_backend_focal_verification.json. Incluye las 33 pruebas de lecturas
y las seis de coordinación, y sustituye la limitación anterior de no disponer
de los XML originales de lecturas para inspección del coordinador.

La revisión comprueba preservación del hecho original, proyección actual,
recibos durables, precedencia de replay, límites de revisión, exclusión del
intervalo propio, presupuesto compartido, errores cerrados y escritura atómica.
Las carreras observadas usan esperas reales de PostgreSQL. El orden creación
antes de movimiento es secuencial y no se presenta como otra carrera. El fallo
de cierre read-only no se confunde con el rechazo real previo al commit que
demuestra rollback de escritura.

El recorrido E2E nominal y la recuperación tras reinicio se verificaron en un
árbol aislado con snapshot explícito. Sus commits de pruebas se integrarán sin
incorporar los commits completos de snapshots Java. No acreditan todavía la
ejecución del corte final combinado.

Pendientes para cierre: integrar main, pruebas E2E y soporte PIT revisado;
ejecutar init sobre ese corte común; ejecutar mutación backend con umbral 80;
completar la revisión UX y la regresión E2E integrada. No se declara despliegue,
MVP completo ni cumplimiento global de UX por estos resultados parciales.
