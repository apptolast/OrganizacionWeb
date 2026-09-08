# Revisión de import_data

## Estado: APPROVED; funcionalidad desplegada y aceptada

Root aprueba el cierre de los 42 escenarios bajo la autorización global del usuario. Feature 23 queda done. Producto 4c74e183e49afa6d280115b399dbaffedc7bfe7f, aplicación PR27 fusionada en 3f4c3ef0d1b0cacf7c175ddaca27b86dbcc91be2 e infraestructura PR38 en 183293b7d779fc7576387eaa23ecff345b646776. Los árboles de 99cd366 y la fusión de aplicación son idénticos; los cambios posteriores al producto corresponden a pruebas y evidencia.

## Contrato y validación

Importa copias propias JSON v1 de catorce colecciones mediante vista previa y confirmación explícita. Conserva identidades e historia, compara datos existentes y rechaza conflictos completos sin sobrescribir. Los recibos permiten recuperar un resultado incierto; importación no sintetiza ni publica eventos históricos. Los límites inclusivos son 32 MiB y 100000 registros. La trazabilidad, los ciclos TDD y los dictámenes parciales permanecen en los informes import de progress.

Init/build oficiales del candidato: EXIT 0, 3215 pruebas Java en 135 suites, 2424 frontend y 70 del arnés. Root verificó XML, originales y 843 entradas antes/después sin diferencias. Persistencia final: 137 casos, uno de wiring, dos de escala y 22 de concurrencia con escritores reales, ambas direcciones de bloqueo y snapshot. Los casos inicialmente verdes no se presentan como nuevos fallos de producción corregidos.

CI 34217454004, 34219923300 y 34223544959 terminaron SUCCESS: 161 E2E correctos y dos omisiones de zoom documentadas, además de recuperación DNS y publicador. El global local original con dos timeouts se conserva. Su reparación cambia únicamente instrumentación geométrica, manteniendo comparaciones, 31 anchos, cuatro estados y límite de 120 segundos; foco 2/2 y globales remotos posteriores verdes. El log de la última CI tiene SHA256 af46d946b4e59c6c74da07d360c79775afcc2be3536133e4b1546e74056ee25a.

## Mutación conservadora

Las campañas completas son independientes; no se suman sus universos:

| Frontera | KILLED / total | Resultado |
| --- | --- | --- |
| Lector y recibos | 135 / 161 | 83,8509 % |
| HTTP y aplicación | 56 / 56 | 100 % |
| Frontend | 651 / 801 | 81,2734 % |
| Persistencia | 316 / 330 | 95,7576 % |

Persistencia original terminó con EXIT 0 en 2 h 21 min 10 s: 316 KILLED, diez SURVIVED, uno NO_COVERAGE y tres TIMED_OUT. Root y revisión independiente comprobaron 330 firmas únicas, cuatro clases completas, cinco candidatos y 512 entradas antes/después idénticas. Root contrastó los 17 artefactos del freeze y 511 entradas actuales sin leer V14 protegido; no hubo diferencias. Backend bb0064c, 4c74e18 y 99cd366 es idéntico.

El XML original tiene SHA256 bbd5c8d0f0f13e2478ecc23199b796d297b53beba7aa13910742095ae8c9d8d0. Su atributo partial se escribe al comenzar desde una opción de cobertura; no es un marcador de terminación. La conclusión completa se apoya en XML cerrado, 330 resultados coincidentes con la estadística final y EXIT 0. Fuentes: [XMLReportListener 1.22.0](https://raw.githubusercontent.com/hcoles/pitest/1.22.0/pitest-entry/src/main/java/org/pitest/mutationtest/report/xml/XMLReportListener.java) y [XMLReportFactory 1.22.0](https://raw.githubusercontent.com/hcoles/pitest/1.22.0/pitest-entry/src/main/java/org/pitest/mutationtest/report/xml/XMLReportFactory.java).

Se aceptan los residuos dentro del umbral acordado, sin reclasificarlos como equivalentes ni afirmar detección total: dos omisiones de validación temporal, siete límites condicionales, un índice de errores sin efecto observable identificado, un vaciado de fila sin cobertura y tres timeouts. La fuente conserva las guardas; no se demostró un defecto productivo en esa lectura. El análisis está en [mutation_import_persistence.md](mutation_import_persistence.md). Frontend conserva 145 supervivientes, cuatro sin cobertura y un RuntimeError en su denominador. El umbral superado no elimina estos límites de sensibilidad.

El paquete [import_persistence_original_evidence.zip](import_persistence_original_evidence.zip), SHA256 5e5f19ee06ac3d23b602b7a7c989626d1412f9722ee87c405da7a7c85356af40, conserva originales y manifiesto verificados. Los otros paquetes import ya versionados mantienen los originales de frontend, lector/HTTP, concurrencia y validación global. La campaña alternativa de partición32/workers8 se retiró como ABORTED_INCOMPLETE sólo después de aprobar la original; 511 entradas intactas, sin score final ni combinación de resultados. Se conservaron sus logs/configuración y Ryuk retiró sus recursos propios.

## Despliegue y aceptación

La fuente de infraestructura 7712fc87c531f120396a8c8cb80bb0495d78ef5a pasó CI 34220016597, gates y check oficial. Apply terminó con EXIT 0: 38 correctas, cuatro cambios, cero fallos y cero omisiones. Operación bea426b2bf0cfe1b96ef5b95f9c7962449dcb5e251d6075b4fcaa0f3db5157d0 liberada. Imágenes por digest y procedencia, V21 aditiva y rollback aislado API23→22→23 verificados antes del apply.

La [aceptación HTTPS](import_live_acceptance.json), SHA256 1f91bac7248a7f1a7d10003fb2b32b58bdfbefe31f9db146e97f561fe1177142, usó una copia propia de 8568 bytes sólo en memoria. Abrir/seleccionar no envió importación; una única vista previa autenticada devolvió 200, catorce colecciones, hash/tamaño originales y todas las filas idénticas. No se pulsó Confirmar importación ni se creó intención o recibo. La preparación se canceló, logout respondió 204 y las comprobaciones anónimas de export/preview respondieron 401.

Chromium comprobó 320, 768 y 1280 px sin desbordamiento ni incidencias axe y con foco por teclado. Root inspeccionó capturas móvil/escritorio con texto privado enmascarado. Las 17 tablas posteriores a V21 conservaron recuentos y hashes de contenido; no se atribuye identidad física xmin/ctid a esa comparación. Los 20 servicios permanecen 1/1, se conservan 18 contenedores y sólo cambian API/web. Ocho rutas anteriores mantienen sus códigos, incluido el 404 previo conocido de generadorcodigosqr.

Playwright no expuso postDataBuffer del File nativo: la integridad se verificó mediante SHA-256 y longitud calculados por el servidor, sin reenviar preview. Los ajustes del lector de JSON SQL y de comparación entre contextos fueron de la comprobación manual, sin cambios en producto o datos. Se retiraron el buffer privado y el contexto del navegador.

El paquete [import_live_original_evidence.zip](import_live_original_evidence.zip), SHA256 c8020454168e1f3f936325c18ff0abf5d579dd9d0800cc90add56802f4603ecb, conserva doce originales de aceptación, apply, snapshots, servicios, capturas enmascaradas y clasificación de la campaña alternativa retirada. Root verificó todas las entradas byte a byte.

Durante el baseline posterior de24 apareció una carrera en el test heredado de apariencia: esperaba el radio seleccionado antes de comprobar un efecto de pintura todavía pendiente. El cierre incorpora la reparación 16d60f8: espera explícita del mismo colorScheme, con timeout predeterminado y todos los oráculos conservados. La suite focal pasa 50/50; no cambia producto, scope de mutación ni imágenes. Original y revisión en review_integration24_appearance_sync.md. La CI del cierre documental y de esta prueba se registra por separado; no se atribuye a las CI anteriores.

La copia fresca del servidor tiene SHA256 df8c876d9c2684ce8bd62160cd104502402ae25b8c46c22f05f65595b3f63b15 y catálogo válido. La restauración completa corresponde a la copia anterior de esquema20, no a esa copia fresca. Rollback aislado y restauración local no acreditan custodia externa automática ni recuperación del broker. La aceptación live sólo hizo preview; aplicación masiva y escrituras se acreditaron en entornos efímeros. No se certifican dispositivos físicos, evaluación humana universal o ausencia total de errores.
