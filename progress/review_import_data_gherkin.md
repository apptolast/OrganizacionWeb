# Revisión del contrato de importación

APPROVED para TDD bajo la autorización global del usuario, 8 de septiembre de 2026. No acredita implementación ni cierre de la funcionalidad.

Root leyó los 42 escenarios y la sección 23 completa, contrastó el esquema y los escritores existentes y revisó los informes independientes. Contrato SHA256 5678995ED52A89E969D5ED2E36D74DFD4030E6BD8A9D92C43E14A6E64608211E; especificación 959BE37196F6E07FFAF969C1824BB33C86DCAE3E6DBD4872DF15437BEDF49369. Las precisiones finales distinguen identidad compuesta de UUID, prefijo válido al superar el límite y rechazo comprobado de commit frente a resultado incierto.

La fusión conserva las catorce colecciones, sin sobrescritura ni remapeo. Los escenarios cubren validación, propiedad, claves alternativas, historia, vista previa, recibos, concurrencia, recursos y recuperación. Los bloqueos de tabla se preceden de los dos advisory locks que ya emplea la personalización; una lectura simple anterior a un UPSERT no quedaba protegida sólo con EXCLUSIVE. Las pruebas deben demostrar las dos carreras descritas, no limitarse a comprobar la cadena SQL.

Las dos excepciones exactas de Nginx delegan el límite de 32 MiB en la API autenticada y transmiten el cuerpo sin acumularlo completo en el proxy. Se conservan las directivas de las rutas originales explícitamente, pues las locations hermanas no las heredan. Los oráculos pasan por el proxy real, con longitud conocida y chunked; no se aumentan timeouts ni tmpfs para obtener un resultado verde.

Los escenarios de interfaz exigen confirmación informada, recuperación sin duplicación, aislamiento de identidad, conservación de borradores y evidencia UX con sus límites. No se interpreta axe como estudio humano ni se declaran dispositivos físicos probados.

Baseline oficial y hashes verificados en 7b7847: 2846 pruebas Java en 123 suites sin fallos, errores ni omisiones; 2295 frontend y 66 del arnés. Después sólo cambian documentación y las dos esperas del test heredado, cuya suite de 47 casos y frontend completo pasaron; la reparación no altera producción. Ponytail full y Caveman lite se mantienen con SDD, TDD, arquitectura y umbral de mutación intactos.

Propiedad de implementación: A lleva aplicación, decoder y persistencia; B interfaz, cliente y ciclo de sesión; C adaptador HTTP y proxy en checkout aislado tras recibir un puerto real compilable. Root coordina y revisa, sin escribir producto ni pruebas ejecutables. Una sola funcionalidad en curso; cada autor entrega ciclos y evidencias propios antes de la integración.
