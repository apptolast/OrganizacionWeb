# Alcance propuesto PIT: HTTP y recibos de exportación

Propuesta para revisión, sin campaña ni cambios en scripts compartidos.
El código C está cerrado en tres clases nuevas completas, incluidos sus
helpers y clases anidadas, sin selección por líneas ni exclusiones de guardas:

- com.apptolast.organization.adapter.http.ExportDataController*
- com.apptolast.organization.adapter.http.ExportHeadersFilter*
- com.apptolast.organization.adapter.persistence.ExportReceiptWriter*

Todas las pruebas JUnit del checkout deben seguir disponibles como candidatas;
los oráculos directos son ExportDataApiTest, ExportHeadersFilterTest y
ExportReceiptWriterTest. No se limitará la cobertura a nombres de métodos ni
se excluirán los casos de corrupción. No duplicar en otra campaña estas tres
clases cuando A cierre el resto del backend22: coordinar un alcance global22
o conjuntos disjuntos, registrando qué corte y bytes acreditan cada uno.

Conservar configuración PIT actual: umbral80, cuatro workers, configuración
vigente de JVM/mutadores/timeouts y reportes originales separados. La propuesta
no altera build.gradle.kts ni habilita un selector compartido. El comando exacto
se fijará después de revisar el mecanismo de selección y el universo definitivo
con root; no ejecutar contra un scope por defecto que omita las clases22.

Antes de campaña: hashes de entradas, gate backend del corte que se vaya a medir
y comprobación de disponibilidad de los tests. Después: preservar XML original,
todos los estados y hashes finales. No reintentos/replays ni reclasificaciones
de supervivientes sin análisis y revisión independiente.
