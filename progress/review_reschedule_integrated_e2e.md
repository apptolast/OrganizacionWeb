# Revisión del E2E integrado

APROBADO para el corte de integración; mutación backend pendiente.

El autor ejecutó la suite completa mediante el runner Docker aislado, EXIT0
deb4f0:98 pruebas aprobadas en7,2 minutos. Root4c48a7 leyó el encabezado de
ejecución y el resultado completo del log. Rootb43b59 comparó los manifiestos:
296 hashes antes y después, cero cambios. El commit inicial e7f70b0 incluía
soporte PIT todavía sin commit; los commits posteriores conservan esos mismos
bytes. No se mezclan resultados de fuentes distintas.

La suite incluye nominal, recuperación tras reinicio, geometría, texto200,
zoom nativo, controles DST/consentimiento y feedback de Replanificar, además
de todas las regresiones anteriores. El stack efímero40896 se retiró al acabar.

El smoke existente del publicador terminó con EXIT0 y nueve comprobaciones
PASS, tras retirar el stack de navegador. Conserva resultados de persistencia,
broker detenido y recuperación, reinicio de Rabbit con volumen y reinicio del
backend. No se le atribuye publicación específica de BlockChanged: esa nueva
ruta y su payload se acreditan en la suite independiente de205 casos que está
incluida en el init completo. No se añadió otro test para repetir el protocolo
común sin una discrepancia observable.

Los logs íntegros permanecen como evidencia local; hashes y resultados están
en tdd_reschedule_integrated_e2e.md y sus manifiestos. No hay despliegue real
ni cumplimiento universal de dispositivos por ejecutar estos contenedores.
