# Revisión — project_states

Veredicto conjunto final: **APPROVED**. Fuente, integración, UX y mutación revisadas. El historial siguiente conserva las comprobaciones y el orden de cierre.

## Fuente revisada

El caso de uso compara identidad y versión antes del no-op, valida la tabla cerrada de transiciones y aplica capacidad sólo al activar. Preserva identidad, texto y fecha de creación. El adaptador toma el bloqueo transaccional antes de bloquear la fila y contar proyectos propios; exige una fila afectada en ambas escrituras y mantiene proyecto y evento en la misma transacción. El bloqueo global breve tiene su límite documentado mediante Ponytail.

El controlador reutiliza el parser estricto y las precondiciones de edición. La migración amplía el conjunto de estados sin transformar proyectos existentes. El publicador añade un único tipo y ruta cerrados; valida los ocho campos y reutiliza las transiciones del dominio para rechazar eventos imposibles o sin cambio.

La interfaz muestra las acciones válidas y conserva la representación hasta recibir confirmación. Envía el ETag de la misma lectura que muestra, sin una segunda lectura silenciosa. La respuesta debe corresponder al proyecto y destino solicitado. La recuperación de conflicto exige recarga explícita; el límite permite elegir otro proyecto que pausar. Las respuestas 401/404 retiran la representación. Cambiar de ruta desmonta el control y cancela su solicitud. Las acciones tienen semántica nativa y el feedback queda fuera de la región ocupada.

No se añadieron dependencias ni almacenamiento de credenciales en el navegador. Ponytail full y Caveman lite aplicados sin reducir el contrato hexagonal, la validación ni los requisitos de accesibilidad.

## Evidencia pendiente

Contrastar los informes definitivos de TDD y mutación con los resultados generados, ejecutar el init conjunto tras liberar Gradle y revisar las capturas, matriz UX, zoom real y veredicto independiente de backend. No atribuir resultados pendientes ni cobertura universal a la emulación.

## Comprobaciones independientes del coordinador

`node .harness/harness.mjs init`, sesión 51375, terminó con salida 0: lint, 328 pruebas backend y 171 frontend. El recuento de XML del backend confirma cero fallos, errores u omisiones. El XML de PIT contiene exactamente 163 resultados KILLED; el porcentaje corresponde a dominio/aplicación, no a todos los adaptadores.

Se inspeccionaron las capturas de escritorio, móvil y zoom a 320 px de `outputs/project-states-*`, con datos sintéticos. Acciones apiladas y legibles en móvil, estados expresados mediante texto y foco visible. El registro del zoom nativo confirma factor 2, ancho interior 320 px CSS, documento 312/312 sin overflow y controles de altura mínima 45 px. Incluye cambio real confirmado desde esa interfaz. Esto no sustituye pruebas físicas de móvil o evaluación humana de usabilidad.

Se pidió corregir la propagación de APP_MAX_ACTIVE_PROJECTS en Docker Compose y documentar el valor inicial en el ejemplo de entorno. El backend ya interpreta y valida la variable; la entrega de configuración debe permitir transmitirla al contenedor.

Revisión independiente de backend APPROVED en `judge_project_states_backend.md`. El coordinador revisó las cuatro pruebas nuevas de navegador, la extracción del helper SQL existente y las ampliaciones de scripts/Compose: sin hallazgos pendientes. La variable de capacidad ya se propaga y los fixtures fijan su valor para aislarse del entorno. Integración informa 22/22 E2E, 2/2 recorridos Firefox/WebKit y smoke de publicador con salida 0. La matriz UX de treinta filas identifica evidencia y límites; los 22 anchos corresponden a Chromium, no a todos los motores.

Sólo queda contrastar el resultado definitivo de mutación frontend antes del dictamen conjunto.

## Cierre de revisión

El coordinador inspeccionó el JSON completo Stryker: 284 Killed y 28 Survived, sin otros estados, puntuación 91,03 %. Revisó también la justificación individual de los supervivientes. Cinco pruebas adicionales detectan seis huecos observables en los mutantes, manteniendo producción intacta. Replay inspeccionado por separado: 14 Killed, sin otros estados; no se suma a la puntuación completa. Los 22 restantes son redundancias o equivalencias limitadas al flujo público y contrato descritos, sin omitir defensas de producción.

La suite frontend final 28182 confirma 176 pruebas y lint. Se conserva la evidencia raíz de 328 pruebas backend y la integración sobre la misma fuente de producción; no se repiten build ni Gradle por añadir únicamente pruebas. Umbral de mutación 80 mantenido. Backend independiente APPROVED, 22 E2E y dos recorridos adicionales de motores, smoke con recuperación real, matriz UX y configuración revisados. Los fixtures propios están retirados.

No quedan hallazgos bloqueantes de esta feature. El autor TDD puede marcar project_states como done. La publicación y CI del commit final se registrarán aparte; no se declara el MVP completo ni despliegue en el servidor del usuario.

Publicado en `171be090c43aae40a77bcf1cb8bcce236d4c2b5e`. CI 33998753845 completada con **SUCCESS** sobre ese commit, verificada mediante GitHub CLI. Incluye el arnés verify, build, E2E y smoke de publicación. La autenticación comienza después del cierre local, con su contrato previo versionado.
